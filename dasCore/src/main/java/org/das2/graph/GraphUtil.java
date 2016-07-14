/*
 * Util.java
 *
 * Created on September 22, 2004, 1:47 PM
 */
package org.das2.graph;

import org.das2.dataset.ConstantDataSetDescriptor;
import org.das2.dataset.XTagsVectorDataSet;
import org.das2.dataset.TableDataSet;
import org.das2.dataset.DataSet;
import org.das2.dataset.TableUtil;
import org.das2.dataset.VectorDataSet;
import org.das2.dataset.DataSetUtil;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.datum.Datum;
import org.das2.datum.UnitsUtil;
import org.das2.dasml.FormBase;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import javax.swing.*;
import javax.xml.parsers.*;
//import org.apache.xml.serialize.*;
import org.w3c.dom.*;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

/**
 *
 * @author  Jeremy
 */
public class GraphUtil {

    public static DasPlot newDasPlot(DasCanvas canvas, DatumRange x, DatumRange y) {
        DasAxis xaxis = new DasAxis(x.min(), x.max(), DasAxis.HORIZONTAL);
        DasAxis yaxis = new DasAxis(y.min(), y.max(), DasAxis.VERTICAL);
        DasRow row = new DasRow(canvas, null, 0, 1, 2, -3, 0, 0);
        DasColumn col = new DasColumn(canvas, null, 0, 1, 5, -3, 0, 0);
        DasPlot result = new DasPlot(xaxis, yaxis);
        canvas.add(result, row, col);
        return result;
    }

    public static void serializeCanvas(DasCanvas canvas, OutputStream out) {
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            document.appendChild(canvas.getDOMElement(document));
            StringWriter writer = new StringWriter();

            DOMImplementationLS ls = (DOMImplementationLS) document.getImplementation().getFeature("LS", "3.0");
            LSOutput output = ls.createLSOutput();
            output.setEncoding("UTF-8");
            output.setByteStream(out);
            LSSerializer serializer = ls.createLSSerializer();
            serializer.write(document, output);

            /*
            OutputFormat format = new OutputFormat(Method.XML, "UTF-8", true);
            XMLSerializer serializer = new XMLSerializer(new OutputStreamWriter(out), format);
            serializer.serialize(document);
             */

            out.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static JTabbedPane loadCanvas(InputStream in) throws Exception {
        FormBase form = new FormBase(in, null, true);
        return form;
    }

    public static GeneralPath getPath(DasAxis xAxis, DasAxis yAxis, VectorDataSet xds, boolean histogram) {
        return getPath(xAxis, yAxis, new XTagsVectorDataSet(xds), xds, histogram);
    }

    public static GeneralPath getPath(DasAxis xAxis, DasAxis yAxis, VectorDataSet xds, VectorDataSet yds, boolean histogram) {

        GeneralPath newPath = new GeneralPath();

        Dimension d;

        double xmin, xmax, ymin, ymax;
        int ixmax, ixmin;

        Units xUnits = xAxis.getUnits();
        Units yUnits = yAxis.getUnits();

        xmax = xAxis.getDataMaximum().doubleValue(xUnits);
        xmin = xAxis.getDataMinimum().doubleValue(xUnits);
        ymax = yAxis.getDataMaximum().doubleValue(yUnits);
        ymin = yAxis.getDataMinimum().doubleValue(yUnits);

        /* need consider how to handle clipping.  Presumably java does this efficiently,
         * so there is no longer a need to worry about this.
         */


        double xSampleWidth;
        if (xds.getProperty("xTagWidth") != null) {
            Datum xSampleWidthDatum = (Datum) xds.getProperty("xTagWidth");
            xSampleWidth = xSampleWidthDatum.doubleValue(xUnits.getOffsetUnits());
        } else if (xds.getProperty("xSampleWidth") != null) {
            Datum xSampleWidthDatum = (Datum) xds.getProperty("xSampleWidth");   // this is not the correct property name, but we'll allow it for now

            xSampleWidth = xSampleWidthDatum.doubleValue(xUnits.getOffsetUnits());
        } else {
            //Try to load the legacy sample-width property.
            String xSampleWidthString = (String) xds.getProperty("x_sample_width");
            if (xSampleWidthString != null) {
                double xSampleWidthSeconds = Double.parseDouble(xSampleWidthString);
                xSampleWidth = Units.seconds.convertDoubleTo(xUnits.getOffsetUnits(), xSampleWidthSeconds);
            } else {
                xSampleWidth = 1e31;
            }
        }

        double t0 = -Double.MAX_VALUE;
        double x0 = -Double.MAX_VALUE;
        double y0 = -Double.MAX_VALUE;
        double i0 = -Double.MAX_VALUE;
        double j0 = -Double.MAX_VALUE;
        boolean skippedLast = true;
        int n = xds.getXLength();
        for (int index = 0; index < n; index++) {
            double t = xds.getXTagDouble(index, xUnits);
            double x = xds.getDouble(index, yUnits);
            double y = yds.getDouble(index, yUnits);
            double i = xAxis.transform(x, xUnits);
            double j = yAxis.transform(y, yUnits);
            if (yUnits.isFill(y) || Double.isNaN(y)) {
                skippedLast = true;
            } else if (skippedLast || (t - t0) > xSampleWidth) {
                newPath.moveTo((float) i, (float) j);
                skippedLast = false;
            } else {
                if (histogram) {
                    double i1 = (i0 + i) / 2;
                    newPath.lineTo((float) i1, (float) j0);
                    newPath.lineTo((float) i1, (float) j);
                    newPath.lineTo((float) i, (float) j);
                } else {
                    newPath.lineTo((float) i, (float) j);
                }
                skippedLast = false;
            }
            t0 = t;
            x0 = x;
            y0 = y;
            i0 = i;
            j0 = j;
        }
        return newPath;

    }

    /**
     * calculates the AffineTransform between two sets of x and y axes, if possible.
     * @param xaxis0 the original reference frame x axis
     * @param yaxis0 the original reference frame y axis
     * @param xaxis1 the new reference frame x axis
     * @param yaxis1 the new reference frame y axis
     * @return an AffineTransform that transforms data positioned with xaxis0 and yaxis0 on xaxis1 and yaxis1, or null if no such transform exists.
     */
    public static AffineTransform calculateAT(DasAxis xaxis0, DasAxis yaxis0, DasAxis xaxis1, DasAxis yaxis1) {
        return calculateAT( xaxis0.getDatumRange(), yaxis0.getDatumRange(), xaxis1, yaxis1 );
    }
    
    public static AffineTransform calculateAT( 
            DatumRange xaxis0, DatumRange yaxis0, 
            DasAxis xaxis1, DasAxis yaxis1 ) {
        
        AffineTransform at = new AffineTransform();

        double dmin0 = xaxis1.transform( xaxis0.min() );  // old axis in new axis space
        double dmax0 = xaxis1.transform( xaxis0.max() );
        double dmin1 = xaxis1.transform( xaxis1.getDataMinimum() );
        double dmax1 = xaxis1.transform( xaxis1.getDataMaximum() );

        double scalex = (dmin0 - dmax0) / (dmin1 - dmax1);
        double transx = -1 * dmin1 * scalex + dmin0;

        at.translate(transx, 0);
        at.scale(scalex, 1.);

        if (at.getDeterminant() == 0.000) {
            return null;
        }

        dmin0 = yaxis1.transform( yaxis0.min() );  // old axis in new axis space
        dmax0 = yaxis1.transform( yaxis0.max() );
        dmin1 = yaxis1.transform(yaxis1.getDataMinimum());
        dmax1 = yaxis1.transform(yaxis1.getDataMaximum());

        double scaley = (dmin0 - dmax0) / (dmin1 - dmax1);
        double transy = -1 * dmin1 * scaley + dmin0;

        at.translate(0, transy);
        at.scale(1., scaley);

        return at;
    }

    public static DasAxis guessYAxis(DataSet dsz) {
        boolean log = false;

        if (dsz.getProperty(DataSet.PROPERTY_Y_SCALETYPE) != null) {
            if (dsz.getProperty(DataSet.PROPERTY_Y_SCALETYPE).equals("log")) {
                log = true;
            }
        }

        DasAxis result;

        if (dsz instanceof TableDataSet) {
            TableDataSet ds = (TableDataSet) dsz;
            Units yunits = ds.getYUnits();
            Datum min, max;

            DatumRange yrange = DataSetUtil.yRange(dsz);
            Datum dy = TableUtil.guessYTagWidth(ds);
            if (UnitsUtil.isRatiometric(dy.getUnits())) {
                log = true;
            }
            result = new DasAxis(yrange.min(), yrange.max(), DasAxis.LEFT, log);

        } else if (dsz instanceof VectorDataSet) {
            VectorDataSet ds = (VectorDataSet) dsz;
            Units yunits = ds.getYUnits();

            DatumRange range = DataSetUtil.yRange(dsz);
            if (range.width().doubleValue(yunits.getOffsetUnits()) == 0.) {
                range = range.include(yunits.createDatum(0));
                if (range.width().doubleValue(yunits.getOffsetUnits()) == 0.) {
                    range = new DatumRange(0, 10, yunits);
                }
            }
            result = new DasAxis(range.min(), range.max(), DasAxis.LEFT, log);

        } else {
            throw new IllegalArgumentException("not supported: " + dsz);
        }

        if (dsz.getProperty(DataSet.PROPERTY_Y_LABEL) != null) {
            result.setLabel((String) dsz.getProperty(DataSet.PROPERTY_Y_LABEL));
        }
        return result;
    }

    public static DasAxis guessXAxis(DataSet ds) {
        Datum min = ds.getXTagDatum(0);
        Datum max = ds.getXTagDatum(ds.getXLength() - 1);
        return new DasAxis(min, max, DasAxis.BOTTOM);
    }

    public static DasAxis guessZAxis(DataSet dsz) {
        if (!(dsz instanceof TableDataSet)) {
            throw new IllegalArgumentException("only TableDataSet supported");
        }
        TableDataSet ds = (TableDataSet) dsz;
        Units zunits = ds.getZUnits();

        DatumRange range = DataSetUtil.zRange(ds);

        boolean log = false;
        if (dsz.getProperty(DataSet.PROPERTY_Z_SCALETYPE) != null) {
            if (dsz.getProperty(DataSet.PROPERTY_Z_SCALETYPE).equals("log")) {
                log = true;
                if (range.min().doubleValue(range.getUnits()) <= 0) { // kludge for VALIDMIN

                    double max = range.max().doubleValue(range.getUnits());
                    range = new DatumRange(max / 1000, max, range.getUnits());
                }
            }
        }

        DasAxis result = new DasAxis(range.min(), range.max(), DasAxis.LEFT, log);
        if (dsz.getProperty(DataSet.PROPERTY_Z_LABEL) != null) {
            result.setLabel((String) dsz.getProperty(DataSet.PROPERTY_Z_LABEL));
        }
        return result;
    }

    public static Renderer guessRenderer(DataSet ds) {
        Renderer rend = null;
        if (ds instanceof VectorDataSet) {// TODO use SeriesRenderer

            if (ds.getXLength() > 10000) {
                rend = new ImageVectorDataSetRenderer(new ConstantDataSetDescriptor(ds));
            } else {
                rend = new SymbolLineRenderer();
                rend.setDataSet(ds);
                ((SymbolLineRenderer) rend).setPsym(Psym.DOTS);
                ((SymbolLineRenderer) rend).setSymSize(2.0);
            }

        } else if (ds instanceof TableDataSet) {
            Units zunits = ((TableDataSet) ds).getZUnits();
            DasAxis zaxis = guessZAxis(ds);
            DasColorBar colorbar = new DasColorBar(zaxis.getDataMinimum(), zaxis.getDataMaximum(), zaxis.isLog());
            colorbar.setLabel(zaxis.getLabel());
            rend = new SpectrogramRenderer(new ConstantDataSetDescriptor(ds), colorbar);
        }
        return rend;
    }

    public static DasPlot guessPlot(DataSet ds) {
        DasAxis xaxis = guessXAxis(ds);
        DasAxis yaxis = guessYAxis(ds);
        DasPlot plot = new DasPlot(xaxis, yaxis);
        plot.addRenderer(guessRenderer(ds));
        return plot;
    }


    public static DasPlot visualize( DataSet ds, String title, String xlabel, String ylabel ) {
        DasPlot p= visualize(ds);
        p.setTitle( title );
        p.getXAxis().setLabel(xlabel);
        p.getYAxis().setLabel(ylabel);
        return p;
    }

    public static DasPlot visualize(DataSet ds) {

        JFrame jframe = new JFrame("DataSetUtil.visualize");
        DasCanvas canvas = new DasCanvas(400, 400);
        jframe.getContentPane().add(canvas);
        DasPlot result = guessPlot(ds);
        canvas.add(result, DasRow.create(canvas), DasColumn.create(canvas,null,"5em","100%-10em") );

        jframe.pack();
        jframe.setVisible(true);
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        return result;
    }

    public static DasPlot visualize(DataSet ds, boolean ylog) {
        DatumRange xRange = DataSetUtil.xRange(ds);
        DatumRange yRange = DataSetUtil.yRange(ds);
        JFrame jframe = new JFrame("DataSetUtil.visualize");
        DasCanvas canvas = new DasCanvas(400, 400);
        jframe.getContentPane().add(canvas);
        DasPlot result = guessPlot(ds);
        canvas.add(result, DasRow.create(canvas), DasColumn.create(canvas,null,"5em","100%-10em"));
        Units xunits = result.getXAxis().getUnits();
        result.getXAxis().setDatumRange(xRange.zoomOut(1.1));
        Units yunits = result.getYAxis().getUnits();
        if (ylog) {
            result.getYAxis().setDatumRange(yRange);
            result.getYAxis().setLog(true);
        } else {
            result.getYAxis().setDatumRange(yRange.zoomOut(1.1));
        }
        jframe.pack();
        jframe.setVisible(true);
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        return result;
    }

    /**
     * Returns the input GeneralPath filled with new points which will be rendered identically to the input path,
     * but contains a minimal number of points.  Successive points occupying the same pixel are
     * culled.
     * TODO: bin average the points within a cell.  The descretization messes up the label orientation in contour plotting.
     * @return a new GeneralPath which will be rendered identically to the input path,
     * but contains a minimal number of points.
     * @param it A path iterator with minute details that will be lost when rendering.
     * @param result A GeneralPath to put the result into.
     */
    public static GeneralPath reducePath(PathIterator it, GeneralPath result) {

        float[] p = new float[6];

        float x0 = Float.MAX_VALUE;
        float y0 = Float.MAX_VALUE;
        float sx0 = 0;
        float sy0 = 0;
        int nx0 = 0;
        int ny0 = 0;
        float ax0 = Float.NaN;
        float ay0 = Float.NaN;  // last averaged location

        int type0 = -999;

        float xres = 1;
        float yres = 1;

        String[] types = new String[]{"M", "L", "QUAD", "CUBIC", "CLOSE"};

        int points = 0;
        int inCount = 0;

        while (!it.isDone()) {
            inCount++;

            int type = it.currentSegment(p);
            it.next();
            float dx = p[0] - x0;
            float dy = p[1] - y0;

            //System.err.print("" + inCount + ": type: " + types[type] + "   " + String.format("[ %f %f ] ", p[0], p[1]));
            if ((type == PathIterator.SEG_MOVETO || type == type0) && Math.abs(dx) < xres && Math.abs(dy) < yres) {
                sx0 += p[0];
                sy0 += p[1];
                nx0 += 1;
                ny0 += 1;
                //System.err.println(" accum");
                continue;
            } else {
                x0 = 0.5f + (int) Math.floor(p[0]);
                y0 = 0.5f + (int) Math.floor(p[1]);
                ax0 = nx0 > 0 ? sx0 / nx0 : p[0];
                ay0 = ny0 > 0 ? sy0 / ny0 : p[1];
                sx0 = p[0];
                sy0 = p[1];
                nx0 = 1;
                ny0 = 1;
            //System.err.print(" avg " + nx0 + " points (" + String.format("[ %f %f ]", ax0, ay0));
            }

            switch (type0) {
                case PathIterator.SEG_LINETO:
                    result.lineTo(ax0, ay0);
                    points++;
                    //j        System.err.println( ""+points+": " + GraphUtil.describe(result, false) );
                    //System.err.println(" lineTo"+ String.format("[ %f %f ]", ax0, ay0));
                    break;
                case PathIterator.SEG_MOVETO:
                    result.moveTo(ax0, ay0);
                    //System.err.println(" moveTo"+ String.format("[ %f %f ]", ax0, ay0));
                    break;
                case -999:
                    //System.err.println(" ignore"+ String.format("[ %f %f ]", ax0, ay0));
                    break;
                default:
                    throw new IllegalArgumentException("not supported");
            }

            type0 = type;
        }

        ax0 = nx0 > 0 ? sx0 / nx0 : p[0];
        ay0 = ny0 > 0 ? sy0 / ny0 : p[1];
        //System.err.print(" avg " + nx0 + " points " );

        switch (type0) {
            case PathIterator.SEG_LINETO:
                result.lineTo(ax0, ay0);
                points++;
                //j        System.err.println( ""+points+": " + GraphUtil.describe(result, false) );
                //System.err.println(" lineTo"+ String.format("[ %f %f ]", ax0, ay0) );
                break;
            case PathIterator.SEG_MOVETO:
                result.moveTo(ax0, ay0);
                //System.err.println(" moveTo "+ String.format("[ %f %f ]", ax0, ay0) );
                break;
            case -999:
                //System.err.println(" ignore");
                break;
            default:
                throw new IllegalArgumentException("not supported");
        }

        return result;
    }

    /**
     * return the points along a curve.  Used by ContourRenderer.  The returned
     * result is the remaining path length.  Elements of pathlen that are beyond
     * the total path length are not computed, and the result points will be null.
     * @param pathlen monotonically increasing path lengths at which the position is to be located.  May be null if only the total path length is desired.
     * @param result the resultant points will be put into this array.  This array should have the same number of elements as pathlen
     * @param orientation the local orientation, in radians, of the point at will be put into this array.  This array should have the same number of elements as pathlen
     * @param it PathIterator first point is used to start the length.
     * @param stopAtMoveTo treat SEG_MOVETO as the end of the path.  The pathIterator will be left at this point.
     * @return the remaining length.  Note null may be used for pathlen, result, and orientation and this will simply return the total path length.
     */
    public static double pointsAlongCurve(PathIterator it, double[] pathlen, Point2D.Double[] result, double[] orientation, boolean stopAtMoveTo) {

        float[] point = new float[6];
        float fx0 = Float.NaN, fy0 = Float.NaN;

        double slen = 0;
        int pathlenIndex = 0;
        int type;

        if (pathlen == null) {
            pathlen = new double[0];
        }
        while (!it.isDone()) {
            type = it.currentSegment(point);
            it.next();

            if (!Float.isNaN(fx0) && type == PathIterator.SEG_MOVETO && stopAtMoveTo) {
                break;
            }
            if (PathIterator.SEG_CUBICTO == type) {
                throw new IllegalArgumentException("cubicto not supported");
            } else if (PathIterator.SEG_QUADTO == type) {
                throw new IllegalArgumentException("quadto not supported");
            } else if (PathIterator.SEG_LINETO == type) {
            }

            if (Float.isNaN(fx0)) {
                fx0 = point[0];
                fy0 = point[1];
                continue;
            }

            double thislen = (float) Point.distance(fx0, fy0, point[0], point[1]);

            if (thislen == 0) {
                continue;
            } else {
                slen += thislen;
            }

            while (pathlenIndex < pathlen.length && slen >= pathlen[pathlenIndex]) {
                double alpha = 1 - (slen - pathlen[pathlenIndex]) / thislen;
                double dx = point[0] - fx0;
                double dy = point[1] - fy0;
                if (result != null) {
                    result[pathlenIndex] = new Point2D.Double(fx0 + dx * alpha, fy0 + dy * alpha);
                }
                if (orientation != null) {
                    orientation[pathlenIndex] = Math.atan2(dy, dx);
                }
                pathlenIndex++;
            }

            fx0 = point[0];
            fy0 = point[1];

        }

        double remaining;
        if (pathlenIndex > 0) {
            remaining = slen - pathlen[pathlenIndex - 1];
        } else {
            remaining = slen;
        }

        if (result != null) {
            for (; pathlenIndex < result.length; pathlenIndex++) {
                result[pathlenIndex] = null;
            }
        }

        return remaining;
    }

    /**
     * @return a string representation of the affine transforms used in DasPlot for
     * debugging.
     */
    public static String getATScaleTranslateString(AffineTransform at) {
        String atDesc;
        NumberFormat nf = new DecimalFormat("0.00");

        if (at == null) {
            return "null";
        } else if (!at.isIdentity()) {
            atDesc = "scaleX:" + nf.format(at.getScaleX()) + " translateX:" + nf.format(at.getTranslateX());
            atDesc += "!c" + "scaleY:" + nf.format(at.getScaleY()) + " translateY:" + nf.format(at.getTranslateY());
            return atDesc;
        } else {
            return "identity";
        }
    }

    /**
     * calculates the slope and intercept of a line going through two points.
     * @return a double array with two elements [ slope, intercept ].
     */
    public static double[] getSlopeIntercept(double x0, double y0, double x1, double y1) {
        double slope = (y1 - y0) / (x1 - x0);
        double intercept = y0 - slope * x0;
        return new double[]{slope, intercept};
    }

    public static Color getRicePaperColor() {
        return new Color(255, 255, 255, 128);
    }

    public static String describe(GeneralPath path, boolean enumeratePoints) {
        PathIterator it = path.getPathIterator(null);
        int count = 0;
        int lineToCount = 0;
        double[] coords = new double[6];
        while (!it.isDone()) {
            int type = it.currentSegment(coords);
            if (type == PathIterator.SEG_LINETO) {
                lineToCount++;
            }
            if (enumeratePoints) {
                //j   System.err.println(" " + coords[0] + " " + coords[1]);
            }
            count++;
            it.next();
        }
        return "count: " + count + "  lineToCount: " + lineToCount;
    }

    static String toString(Line2D line) {
        return ""+line.getX1()+","+line.getY1()+" "+line.getX2()+","+line.getY2();
    }
    
    //TODO:  sun.awt.geom.Curve and sun.awt.geom.Crossings are GPL open-source, so
    // these methods will provide reliable methods for getting rectangle, line 
    // intersections.

    /**
     * returns the point where the two line segments intersect, or null.
     * @param line1
     * @param line2
     * @param noBoundsCheck if true, then do not check the segment bounds.
     * @return
     */
    public static Point2D lineIntersection(Line2D line1, Line2D line2, boolean noBoundsCheck) {
        Point2D result = null;
        double a1, b1, c1, a2, b2, c2, denom;
        a1 = line1.getY2() - line1.getY1();
        b1 = line1.getX1() - line1.getX2();
        c1 = line1.getX2() * line1.getY1() - line1.getX1() * line1.getY2();

        a2 = line2.getY2() - line2.getY1();
        b2 = line2.getX1() - line2.getX2();
        c2 = line2.getX2() * line2.getY1() - line2.getX1() * line2.getY2();

        denom = a1 * b2 - a2 * b1;
        if (denom != 0) {
            result = new Point2D.Double((b1 * c2 - b2 * c1) / denom, (a2 * c1 -
                    a1 * c2) / denom);
            if (noBoundsCheck 
                    ||(((result.getX() - line1.getX1()) * (line1.getX2() - result.getX()) >= 0) 
                    && ((result.getY() - line1.getY1()) * (line1.getY2() - result.getY()) >= 0) 
                    && ((result.getX() - line2.getX1()) * (line2.getX2() - result.getX()) >= 0) 
                    && ((result.getY() - line2.getY1()) * (line2.getY2() - result.getY()) >= 0) ) ) {
                return result;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static Point2D lineRectangleIntersection( Point2D p0, Point2D p1, Rectangle2D r0) {

        PathIterator it = r0.getPathIterator(null);

        Line2D line = new Line2D.Double( p0, p1 );

        float[] c0 = new float[6];
        float[] c1 = new float[6];
        it.currentSegment(c0);
        it.next();
        while ( !it.isDone() ) {
            int type= it.currentSegment(c1);
            if ( type==PathIterator.SEG_LINETO ) {
                Line2D seg = new Line2D.Double(c0[0], c0[1], c1[0], c1[1]);
                Point2D result = lineIntersection(line, seg, false);
                if (result != null) {
                    return result;
                }
            }
            it.next();
            c0[0]= c1[0];
            c0[1]= c1[1];
        }
        return null;
    }
    
    /**
     * returns pixel range of the datum range, guarenteeing that the first 
     * element will be less than or equal to the second.
     * @param axis
     * @param range
     * @return
     */
    public static double[] transformRange( DasAxis axis, DatumRange range ) {
        double x1= axis.transform(range.min());
        double x2= axis.transform(range.max());
        if ( x1>x2 ) {
            double t= x2;
            x2= x1;
            x1= t;
        }
        return new double[] { x1, x2 };
    }

    public static DatumRange invTransformRange( DasAxis axis, double x1, double x2 ) {
        Datum d1= axis.invTransform(x1);
        Datum d2= axis.invTransform(x2);
        if ( d1.gt(d2) ) {
            Datum t= d2;
            d2= d1;
            d1= t;
        }
        return new DatumRange( d1, d2 );
    }
    
    /**
     * return a block with the color and size.
     * @param w
     * @param h
     * @return 
     */
    public static Icon colorIcon( Color iconColor, int w, int h ) {
        BufferedImage image= new BufferedImage( w, h, BufferedImage.TYPE_INT_ARGB );
        Graphics g= image.getGraphics();
        Color save = g.getColor();
        if ( iconColor.getAlpha()!=255 ) { // draw checkerboard to indicate transparency
            for ( int j=0; j<16/4; j++ ) {
                for ( int i=0; i<16/4; i++ ) {
                    g.setColor( (i-j)%2 ==0 ? Color.GRAY : Color.WHITE );
                    g.fillRect( 0+i*4,0+j*4,4,4);
                }
            }
        }
        g.setColor(iconColor);
        g.fillRect( 0, 0, w, h );
        return new ImageIcon(image);
    }
}
