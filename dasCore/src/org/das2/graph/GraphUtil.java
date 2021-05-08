/*
 * Util.java
 *
 * Created on September 22, 2004, 1:47 PM
 */
package org.das2.graph;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.datum.Datum;
import org.das2.datum.UnitsUtil;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.LoggerManager;
import org.das2.qds.DataSetOps;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.ops.Ops;
import org.das2.util.DasMath;
import org.jdesktop.beansbinding.Converter;
//import org.apache.xml.serialize.*;

/**
 * Utilities for drawing graphics and establishing standard behavior.
 * This provides functions to get a 16x16 icon for a color, getting a Path
 * from a series of data points, and a "visualize" method for simply looking at
 * some data.
 * @author  Jeremy
 */
public class GraphUtil {
	
	/**
	 * Classes that implement this interface provide their instances with
	 * the ability to copy themselves in a manner similar to Cloneable objects,
	 * but without the drawbacks of Cloneable.
	 * 
	 * Implementers need not return an object of their exact type, but the object
	 * they return should have all the same supertypes as the original object.
	 */
	public interface Copyable<T> {
		T copy();
	}

    private static final Logger logger= LoggerManager.getLogger("das2.graphics.util");
    
    /**
     * create a plot for the canvas, along with the row and column for layout.
     * @param canvas the canvas parent for the plot.
     * @param x the x range
     * @param y the y range
     * @return the plot.
     */
    public static DasPlot newDasPlot(DasCanvas canvas, DatumRange x, DatumRange y) {
        DasAxis xaxis = new DasAxis(x.min(), x.max(), DasAxis.HORIZONTAL);
        DasAxis yaxis = new DasAxis(y.min(), y.max(), DasAxis.VERTICAL);
        DasRow row = new DasRow(canvas, null, 0, 1, 2, -3, 0, 0);
        DasColumn col = new DasColumn(canvas, null, 0, 1, 5, -3, 0, 0);
        DasPlot result = new DasPlot(xaxis, yaxis);
        canvas.add(result, row, col);
        return result;
    }


    /**
     * get the path for the points, checking for breaks in the data from fill values.
     * @param xAxis the x axis.
     * @param yAxis the y axis.
     * @param ds the y values.  SemanticOps.xtagsDataSet is used to extract the x values.
     * @param histogram if true, use histogram (stair-step) mode
     * @param clip limit path to what's visible for each axis.
     * @return the GeneralPath.
     */
    public static GeneralPath getPath(DasAxis xAxis, DasAxis yAxis, QDataSet ds, boolean histogram, boolean clip ) {
        return getPath(xAxis, yAxis, SemanticOps.xtagsDataSet(ds), ds, histogram, clip );
    }

    /**
     * get the path for the points, checking for breaks in the data from fill values.
     * @param xAxis the x axis.
     * @param yAxis the y axis.
     * @param xds the x values.
     * @param yds the y values.
     * @param histogram if true, use histogram (stair-step) mode
     * @param clip limit path to what's visible for each axis.
     * @return the GeneralPath.
     */
    public static GeneralPath getPath( DasAxis xAxis, DasAxis yAxis, QDataSet xds, QDataSet yds, boolean histogram, boolean clip ) {
        return getPath( xAxis, yAxis, xds, yds, histogram ? CONNECT_MODE_HISTOGRAM : CONNECT_MODE_SERIES, clip );
    }
    
    /**
     * draw the lines in histogram mode, horizontal to the half-way point, then vertical, then horizontal the rest of the way.
     */
    public static final String CONNECT_MODE_HISTOGRAM= "histogram";
    
    /**
     * don't draw connecting lines.
     */
    public static final String CONNECT_MODE_SCATTER= "scatter";
    
    /**
     * the normal connecting mode from point-to-point in a series.
     */
    public static final String CONNECT_MODE_SERIES= "series";
    
    /**
     * get the path for the points, checking for breaks in the data from fill values.
     * @param xAxis the x axis.
     * @param yAxis the y axis.
     * @param xds the x values.
     * @param yds the y values.
     * @param mode one of CONNECT_MODE_SERIES, CONNECT_MODE_SCATTER, or CONNECT_MODE_HISTOGRAM
     * @param clip limit path to what's visible for each axis.
     * @see #CONNECT_MODE_SERIES
     * @see #CONNECT_MODE_SCATTER
     * @see #CONNECT_MODE_HISTOGRAM
     * @return the GeneralPath.
     */
    public static GeneralPath getPath( DasAxis xAxis, DasAxis yAxis, QDataSet xds, QDataSet yds, String mode, boolean clip ) {

        GeneralPath newPath = new GeneralPath();

        //Dimension d;

        Units xUnits = SemanticOps.getUnits(xds);
        Units yUnits = SemanticOps.getUnits(yds);

        //QDataSet tagds= SemanticOps.xtagsDataSet(xds); // yes, it's true, I think because of orbit plots

        //double xSampleWidth= Double.MAX_VALUE; // old code had 1e31.  MAX_VALUE is better.
        //if ( tagds.property( QDataSet.CADENCE ) != null ) { //e.g. Orbit(T);
            //Datum xSampleWidthDatum = (Datum) org.virbo.dataset.DataSetUtil.asDatum( (RankZeroDataSet)tagds.property( QDataSet.CADENCE ) );
            //xSampleWidth = xSampleWidthDatum.doubleValue(xUnits.getOffsetUnits());
        //}

        //double t0 = -Double.MAX_VALUE;
        double i0 = -Double.MAX_VALUE;
        double j0 = -Double.MAX_VALUE;
        boolean v0= false;  // last point was visible
        boolean skippedLast = true;
        int n = xds.length();

        QDataSet wds= SemanticOps.weightsDataSet(yds);
        
        Rectangle rclip= clip ? DasDevicePosition.toRectangle( yAxis.getRow(), xAxis.getColumn() ) : null;

        boolean histogram= mode.equals(CONNECT_MODE_HISTOGRAM);
        boolean scatter= mode.equals(CONNECT_MODE_SCATTER);
        
        for (int index = 0; index < n; index++) {
            //double t = index;
            double x = xds.value(index);
            double y = yds.value(index);
            double i = xAxis.transform(x, xUnits);
            double j = yAxis.transform(y, yUnits);
            boolean v= rclip==null || rclip.contains( i,j);
            if ( wds.value(index)==0 || Double.isNaN(y)) {
                skippedLast = true;
            } else if ( skippedLast ) { //|| (t - t0) > xSampleWidth ) { // remove use of t until it is compared with physical data, not dimensionless
                newPath.moveTo((float) i, (float) j);
                if ( scatter ) {
                    newPath.lineTo((float) i, (float) j);
                }
                skippedLast = !v;
            } else {
                if ( v||v0 ) {
                    if (histogram) {
                        double i1 = (i0 + i) / 2;
                        newPath.lineTo((float) i1, (float) j0);
                        newPath.lineTo((float) i1, (float) j);
                        newPath.lineTo((float) i, (float) j);
                    } else if (scatter) {
                        newPath.moveTo((float) i, (float) j); 
                        newPath.lineTo((float) i, (float) j);                        
                    } else {
                        newPath.lineTo((float) i, (float) j);
                    }
                }
                skippedLast = false;
            }
            //t0 = t;
            i0 = i;
            j0 = j;
            v0= v;
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

    public static DasAxis guessYAxis(QDataSet dsz) {
        boolean log = false;

        if (dsz.property(QDataSet.SCALE_TYPE) != null) {
            if (dsz.property(QDataSet.SCALE_TYPE).equals("log")) {
                log = true;
            }
        }

        DasAxis result;

        if ( SemanticOps.isSimpleTableDataSet(dsz) ) {
            QDataSet ds = (QDataSet) dsz;
            QDataSet yds= SemanticOps.ytagsDataSet(ds);

            DatumRange yrange = org.das2.qds.DataSetUtil.asDatumRange( Ops.extent(yds), true );
            yrange= DatumRangeUtil.rescale( yrange, -0.1, 1.1 );
            Datum dy = org.das2.qds.DataSetUtil.asDatum(org.das2.qds.DataSetUtil.guessCadenceNew( yds, null ) );

            if (UnitsUtil.isRatiometric(dy.getUnits())) {
                log = true;
            }
            result = new DasAxis(yrange.min(), yrange.max(), DasAxis.LEFT, log);

        } else if ( !SemanticOps.isTableDataSet(dsz) ) {
            QDataSet yds= dsz;
            if ( SemanticOps.isBundle(dsz) ) {
                yds= DataSetOps.unbundleDefaultDataSet(dsz);
                dsz= yds;
            }

            DatumRange yrange = org.das2.qds.DataSetUtil.asDatumRange( Ops.extent(yds), true );
            yrange= DatumRangeUtil.rescale( yrange, -0.1, 1.1 );
            result = new DasAxis(yrange.min(), yrange.max(), DasAxis.LEFT, log);

        } else {
            throw new IllegalArgumentException("not supported: " + dsz);
        }

        if (dsz.property(QDataSet.LABEL) != null) {
            result.setLabel( (String) dsz.property(QDataSet.LABEL));
        }
        return result;
    }

    public static DasAxis guessXAxis(QDataSet ds) {
        QDataSet xds= SemanticOps.xtagsDataSet(ds);
        DatumRange range= org.das2.qds.DataSetUtil.asDatumRange( Ops.extent(xds), true );
        range= DatumRangeUtil.rescale( range, -0.1, 1.1 );
        return new DasAxis( range.min(), range.max(), DasAxis.BOTTOM);
    }

    public static DasAxis guessZAxis(QDataSet dsz) {
        if (!( SemanticOps.isTableDataSet(dsz) ) ) {
            throw new IllegalArgumentException("only TableDataSet supported");
        }
        QDataSet ds = (QDataSet) dsz;

        DatumRange range = org.das2.qds.DataSetUtil.asDatumRange( Ops.extent(ds), true );

        boolean log = false;
        if ( "log".equals( dsz.property( QDataSet.SCALE_TYPE ) ) ) {
            log = true;
            if (range.min().doubleValue(range.getUnits()) <= 0) { // kludge for VALIDMIN

                double max = range.max().doubleValue(range.getUnits());
                range = new DatumRange(max / 1000, max, range.getUnits());
            }
        }

        DasAxis result = new DasAxis(range.min(), range.max(), DasAxis.LEFT, log);
        if (dsz.property( QDataSet.LABEL ) != null) {
            result.setLabel((String) dsz.property( QDataSet.LABEL ) );
        }
        return result;
    }

    /**
     * legacy guess that is used who-knows-where.  Autoplot has much better code
     * for guessing, refer to it.
     * @param ds
     * @return
     */
    public static Renderer guessRenderer(QDataSet ds) {
        Renderer rend = null;
        if ( !SemanticOps.isTableDataSet(ds) ) {// TODO use SeriesRenderer
            if (ds.length() > 10000) {
                rend = new HugeScatterRenderer( null );
                rend.setDataSet( ds );
            } else {
                rend = new SeriesRenderer();
                rend.setDataSet(ds);
                ((SeriesRenderer) rend).setPsym( DefaultPlotSymbol.CIRCLES );
                ((SeriesRenderer) rend).setSymSize(2.0);
            }

        } else if ( SemanticOps.isTableDataSet(ds) ) {
            DasAxis zaxis = guessZAxis(ds);
            DasColorBar colorbar = new DasColorBar(zaxis.getDataMinimum(), zaxis.getDataMaximum(), zaxis.isLog());
            colorbar.setLabel(zaxis.getLabel());
            rend = new SpectrogramRenderer( null, colorbar);
            rend.setDataSet(ds);
        }
        return rend;
    }

    /**
     * get a plot and renderer for the dataset.
     * @param ds the dataset
     * @return a plot with a renderer for the dataset.
     */
    public static DasPlot guessPlot(QDataSet ds) {
        DasAxis xaxis = guessXAxis(ds);
        DasAxis yaxis = guessYAxis(ds);
        DasPlot plot = new DasPlot(xaxis, yaxis);
        plot.addRenderer(guessRenderer(ds));
        return plot;
    }

    /**
     * return a copy of the plot.  It does not have the
     * row and column set to its own row and column.
     * @param a
     * @return 
     */
    public static DasAxis copyAxis( DasAxis a ) {
        DasAxis c= new DasAxis( a.getDatumRange(), a.getOrientation() );
        c.setDataMinimum( a.getDataMinimum() );
        c.setDataMaximum( a.getDataMaximum() );
        c.setLog(a.isLog());
        c.setLabel(a.getLabel());
        c.setFlipLabel(a.isFlipLabel());
        c.setFlipped(a.isFlipped());
        c.setEnabled( a.isEnabled() );
        c.setEnableHistory( a.isEnableHistory() );
        c.setLog( a.isLog() );
        c.setOpaque( a.isOpaque() );
        c.setOppositeAxisVisible( a.isOppositeAxisVisible() );
        c.setTickLabelsVisible( a.isTickLabelsVisible() );
        c.setUseDomainDivider( a.isUseDomainDivider() );
        c.setUserDatumFormatter( a.getUserDatumFormatter() );
        return c;
    }
    
    /**
     * return a copy of the plot.  It does not have the
     * row and column set to its own row and column.
     * @param a
     * @return 
     */
    public static DasColorBar copyColorBar( DasColorBar a ) {
        DasColorBar c= new DasColorBar( a.getDataMinimum(), a.getDataMaximum(), a.getOrientation(), a.isLog() );
        c.setLabel(a.getLabel());
        c.setFlipLabel(a.isFlipLabel());
        c.setType(a.getType());
        return c;
    }
    
    /**
     * return a copy of the plot.  This will include the Renderers and the 
     * data they contain.  The plot is not attached to a canvas or row 
     * and column.
     * <pre>
     * {@code
     *   cnvsNew= new DasCanvas(500,500);
     *   row= new DasRow(cnvsNew,0.2,0.8);
     *   column= new DasColumn(cnvsNew,0.2,0.8);
     *   p= GraphUtil.copyPlot(dp); 
     *   cnvsNew.add(p,row,column); 
     * }
     * </pre>
     * @param p
     * @return 
     */
    public static DasPlot copyPlot( DasPlot p ) {
        DasAxis xaxis= copyAxis(p.getXAxis());
        DasAxis yaxis= copyAxis(p.getYAxis());
        DasPlot c= new DasPlot(xaxis, yaxis);
        c.setTitle( p.getTitle() );
        c.setDisplayTitle( p.isDisplayTitle() );
        c.setDrawGrid( p.isDrawGrid() );
        c.setPreviewEnabled( p.isPreviewEnabled() );
        c.setLegendPosition( p.getLegendPosition() );
        c.setDisplayLegend( p.isDisplayLegend() );
        for ( Renderer r: p.getRenderers() ) {
            Renderer cr;
            if ( r instanceof Copyable ) {
            	@SuppressWarnings("unchecked")
		Copyable<Renderer> copyable = (Copyable<Renderer>) r;
            	cr = copyable.copy();
            } else if ( r instanceof SpectrogramRenderer ) {
                DasColorBar cb= copyColorBar(((SpectrogramRenderer)r).getColorBar());
                cr= new SpectrogramRenderer(null,cb);
                SpectrogramRenderer sr= (SpectrogramRenderer)cr;
                sr.setRebinner(((SpectrogramRenderer)r).getRebinner());
            } else if ( r instanceof SeriesRenderer ) {
                cr= new SeriesRenderer();
                SeriesRenderer sr= (SeriesRenderer)cr;
                ((SeriesRenderer)cr).setAntiAliased(((SeriesRenderer) r).isAntiAliased());
                sr.setColor( ((SeriesRenderer) r).getColor() );
                sr.setFillColor(((SeriesRenderer) r).getFillColor() );
                sr.setFillStyle(((SeriesRenderer) r).getFillStyle() );
                sr.setLineWidth(((SeriesRenderer) r).getLineWidth() );
                sr.setFillToReference(((SeriesRenderer) r).isFillToReference() );
                sr.setReference(((SeriesRenderer) r).getReference() );
                sr.setSymSize(((SeriesRenderer) r).getSymSize() );
                sr.setPsym(((SeriesRenderer) r).getPsym() );
                sr.setPsymConnector(((SeriesRenderer) r).getPsymConnector() );
                sr.setLegendLabel(((SeriesRenderer) r).getLegendLabel());
                sr.setDrawLegendLabel(((SeriesRenderer) r).isDrawLegendLabel());
            } else if ( r instanceof HugeScatterRenderer ) {
                cr= new HugeScatterRenderer(null);
                HugeScatterRenderer sr= (HugeScatterRenderer)cr;
                sr.setColor( ((HugeScatterRenderer) r).getColor() );
            } else if ( r instanceof ContoursRenderer ) { // NOTE this could probably work for all Renderers.
                cr= new ContoursRenderer();
                ContoursRenderer sr= (ContoursRenderer)r;
                cr.setControl( sr.getControl() );
            } else {
                logger.log(Level.WARNING, "source renderer {0} cannot be copied. Skipping.", r.getLegendLabel());
            	continue;
            }
            cr.setControl(r.getControl());
            cr.setDataSet(r.getDataSet());
            c.addRenderer( cr );
        }
        if (c.getRenderers().length == 0) {
            throw new UnsupportedOperationException("No copyable renderers.");
        }
        return c;
    }
    
    /**
     * get a plot and add it to a JFrame.
     * @param ds
     * @return 
     */
    public static DasPlot visualize(QDataSet ds) {

        JFrame jframe = new JFrame("DataSetUtil.visualize");
        DasCanvas canvas = new DasCanvas(400, 400);
        jframe.getContentPane().add(canvas);
        DasPlot result = guessPlot(ds);
        canvas.add(result, new DasRow(canvas,0.1,0.9), DasColumn.create(canvas,null,"5em","100%-10em") );

        jframe.pack();
        jframe.setVisible(true);
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        return result;
    }

//    public static DasPlot visualize(QDataSet ds, boolean ylog) {
//        DatumRange xRange = org.virbo.dataset.DataSetUtil.asDatumRange( Ops.extent( ds ). true );
//        DatumRange yRange = org.virbo.dataset.DataSetUtil.yRange(ds);
//        JFrame jframe = new JFrame("DataSetUtil.visualize");
//        DasCanvas canvas = new DasCanvas(400, 400);
//        jframe.getContentPane().add(canvas);
//        DasPlot result = guessPlot(ds);
//        canvas.add(result, DasRow.create(canvas), DasColumn.create(canvas,null,"5em","100%-10em"));
//        Units xunits = result.getXAxis().getUnits();
//        result.getXAxis().setDatumRange(xRange.zoomOut(1.1));
//        Units yunits = result.getYAxis().getUnits();
//        if (ylog) {
//            result.getYAxis().setDatumRange(yRange);
//            result.getYAxis().setLog(true);
//        } else {
//            result.getYAxis().setDatumRange(yRange.zoomOut(1.1));
//        }
//        jframe.pack();
//        jframe.setVisible(true);
//        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        return result;
//    }


    /**
     * clip the path to within the clip rectangle.  Note this may introduce 
     * breaks where the path was continuous before.  Note this does not work
     * with quadTo etc.  This was motivated by an old version of Adobe Illustrator
     * which didn't respect the clip set in the PDF, and with the journal 
     * Nature, which apparently uses an old version of Illustrator.
     * @param it
     * @param result
     * @param clip
     * @return 
     */
    public static int clipPath( PathIterator it, GeneralPath result, Rectangle clip ) {
        logger.entering( "GraphUtil", "clipPath" );
        
        float[] p = new float[6];
        
        Point2D lastP= null;
        Point2D thisP;
        
        boolean initialMoveTo= true;
        
        while (!it.isDone()) {

            int type = it.currentSegment(p);
            it.next();
            
            float xx = p[0];
            float yy = p[1];
                        
            if ( type == PathIterator.SEG_MOVETO ) {
                if ( lastP!=null ) { 
                    thisP= new Point2D.Float( xx, yy );
                    Point2D clipP= lineRectangleIntersection( lastP, thisP, clip );
                    if ( clip.contains(lastP) ) {
                        throw new IllegalArgumentException("should not happen 584");
                    } else if ( clip.contains(thisP) ) {
                        result.moveTo( clipP.getX(), clipP.getY() );
                        result.moveTo( thisP.getX(), thisP.getY() );
                        initialMoveTo= false;
                    }
                    lastP= thisP;
                } else {
                    thisP= new Point2D.Float( xx, yy );
                    if ( clip.contains(thisP) ) {
                        result.moveTo( thisP.getX(), thisP.getY() );
                        initialMoveTo= false;
                    } 
                    lastP= thisP;
                }
                    
            } else if ( type == PathIterator.SEG_LINETO ) {
                if ( lastP!=null ) { // we need to clip this line segment
                    thisP= new Point2D.Float( xx, yy );
                    Point2D clipP= lineRectangleIntersection( lastP, thisP, clip );
                    if ( clip.contains(lastP) ) {
                        if ( clip.contains(thisP) ) {
                            if ( initialMoveTo ) {
                                result.moveTo( lastP.getX(), lastP.getY() );
                                initialMoveTo= false;
                                //logger.warning("what to do now!");
                            }
                            result.lineTo( thisP.getX(), thisP.getY() );
                        } else {
                            if ( initialMoveTo ) {
                                result.moveTo( lastP.getX(), lastP.getY() );
                                initialMoveTo= false;
                            }
                            try {
                                result.lineTo( clipP.getX(), clipP.getY() );
                            } catch ( NullPointerException ex ) {
                                result.lineTo( clipP.getX(), clipP.getY() );
                            }
                        }
                    } else if ( clip.contains(thisP) ) {
                        if ( clipP!=null ) {                            
                            result.moveTo( clipP.getX(), clipP.getY() );
                            result.lineTo( thisP.getX(), thisP.getY() );
                        }
                    } else {
                        Line2D clipP2= lineRectangleMask( lastP, thisP, clip );
                        if ( clipP2!=null ) {
                            result.moveTo( clipP2.getX1(), clipP2.getY1() );
                            result.lineTo( clipP2.getX2(), clipP2.getY2() );
                        }
                    }
                    lastP= thisP;
                } else {
                    thisP= new Point2D.Float( xx, yy );
                    if ( clip.contains(thisP) ) {
                        result.lineTo( thisP.getX(), thisP.getY() );
                    } else {
                        logger.info("TODO: what about this branch?");
                    }
                    lastP= thisP;
                }
            }

        }
        logger.exiting( "GraphUtil", "clipPath" );
        return 0;

    }
    
    /**
     * New ReducePath reduces a path by keeping track of vertically collinear points, and reducing them to an entry
     * point, an exit point, min and max.  This can be all four in one point.  We also limit the resolution and 
     * combine points that are basically the same value, using resn and resd (numerator and denominator).  For 
     * example (1/5) would mean that points within x of 1/5 of one another are considered vertically collinear.
     * 
     * @param it input path.
     * @param result output path.
     * @param resn the resolution numerator (e.g. 1)
     * @param resd the resolution denominator (e.g. 5)
     * @return the number of points.
     */
    public static int reducePath20140622( PathIterator it, GeneralPath result, int resn, int resd ) {
        
        logger.entering( "GraphUtil", "reducePath20140622" );
        
        long t0= System.currentTimeMillis();
        
        float[] p = new float[6];

        int x0 = -99999;
        int y0 = -99999;
        int entryy= -99999;
        int exity;
        int miny= 99999;
        int maxy= -99999;

        int type0 = -999; // the previous segment type.

        //String[] types = new String[]{"M", "L", "QUAD", "CUBIC", "CLOSE"};

        int points = 0;
        int inCount = 0;

        boolean atMiny=false;
        boolean atMaxy=false;
            
        while (!it.isDone()) {
            inCount++;

            int type = it.currentSegment(p);
            it.next();
            int xx = (int)( (p[0]*resd) ) / resn;
            int yy = (int)( (p[1]*resd) ) / resn;
            
            if ( type0==-999 ) {
                result.moveTo((float)xx/resd,(float)yy/resd);
                x0= xx;
                entryy= yy;
                miny= yy;
                maxy= yy;
            }
            
            if ( (type == PathIterator.SEG_MOVETO ) && xx==x0 ) {
                // do nothing
            } else if ( (type == PathIterator.SEG_LINETO || type == type0) && xx==x0 ) {
                miny= Math.min( miny, yy );
                maxy= Math.max( maxy, yy );
            }
            
            if ( xx!=x0 ) {
                atMiny=false;
                atMaxy=false;                
                exity= y0;
                if ( miny==maxy ) { // implies entryx==exitx
                    atMaxy=true;
                    atMiny=true;
                } else if ( entryy==miny ) {
                    result.lineTo( ((float)x0)*resn/resd, ((float)miny)*resn/resd );  points++;
                    atMiny= true;
                } else if ( entryy==maxy ) {
                    result.lineTo( ((float)x0)*resn/resd, ((float)maxy)*resn/resd );  points++;
                    atMaxy= true;
                } else {
                    result.lineTo( ((float)x0)*resn/resd, ((float)entryy)*resn/resd );  points++;
                    result.lineTo( ((float)x0)*resn/resd, ((float)miny)*resn/resd );  points++;
                    atMiny= true;
                }
                if ( miny<maxy ) {
                    if ( atMiny ) {
                        result.lineTo(  ((float)x0)*resn/resd, ((float)maxy)*resn/resd );  points++;
                        atMaxy= true;
                    } else if ( !atMiny ) {
                        result.lineTo(  ((float)x0)*resn/resd, ((float)miny)*resn/resd );  points++;
                        atMiny= true;                                         
                    }
                }
                
                if ( miny==maxy ) {
                    // nothing to do
                } else if ( exity==miny ) {
                    if ( atMiny ) {
                        result.lineTo( ((float)x0)*resn/resd, ((float)exity)*resn/resd );  points++;
                    } else if ( atMaxy ) {
                        result.lineTo( ((float)x0)*resn/resd, ((float)exity)*resn/resd );  points++;
                    } else {
                        throw new RuntimeException("shouldn't get here line608");
                    }
                } else if ( exity==maxy ) {
                    if ( atMaxy ) {
                        
                    } else if ( atMiny ) {
                        throw new RuntimeException("shouldn't get here line614");
                    } else {
                        throw new RuntimeException("shouldn't get here line616");
                    }
                } else {
                    result.lineTo( ((float)x0)*resn/resd, ((float)exity)*resn/resd );  points++;
                }
                
                if ( type==PathIterator.SEG_LINETO ) {
                    result.lineTo( ((float)xx)*resn/resd, ((float)yy)*resn/resd );  points++;
                } else if ( type==PathIterator.SEG_MOVETO ) {
                    result.moveTo( ((float)xx)*resn/resd, ((float)yy)*resn/resd );  points++;
                }
                
                entryy= yy;             
                miny= yy;
                maxy= yy;
                
            }
            
            if ( type == PathIterator.SEG_MOVETO ) {
                result.moveTo( ((float)xx)*resn/resd, ((float)yy)*resn/resd );
                points++;
            }

            x0 = xx;
            y0 = yy;

            type0 = type;
        }
        
        if ( miny==maxy ) { // implies entryx==exitx
        } else if ( entryy==miny ) {
            result.lineTo( ((float)x0)*resn/resd, ((float)miny)*resn/resd );  points++;
            atMiny= true;
        } else if ( entryy==maxy ) {
            result.lineTo( ((float)x0)*resn/resd, ((float)maxy)*resn/resd );  points++;
            atMiny= false;
        } else {
            result.lineTo( ((float)x0)*resn/resd, ((float)entryy)*resn/resd );  points++;
            result.lineTo( ((float)x0)*resn/resd, ((float)miny)*resn/resd );  points++;
            atMiny= true;
        }
        if ( miny<maxy ) {
            if ( atMiny ) {
                result.lineTo(  ((float)x0)*resn/resd, ((float)maxy)*resn/resd );  points++;
            } else if ( !atMiny ) {
                result.lineTo(  ((float)x0)*resn/resd, ((float)miny)*resn/resd );  points++;
            }
        }
        
        logger.log(Level.FINE, "reduce {0} to {1} in {2}ms", new Object[]{inCount, points, System.currentTimeMillis()-t0 });
        logger.exiting( "GraphUtil", "reducePath20140622" );
        
        return points;
        
    }
    
    /**
     * Returns the input GeneralPath filled with new points which will be rendered identically to the input path,
     * but contains a minimal number of points.  We bin average the points within a cell, because discretization
     * would mess up the label orientation in contour plotting.
     *
     * a new GeneralPath which will be rendered identically to the input path,
     * but contains a minimal number of points.
     *
     * @return the number of "points" (LINE_TOs) in the result.
     * @param it A path iterator with minute details that will be lost when rendering.
     * @param result A GeneralPath to put the result into.
     */
    public static int reducePath(PathIterator it, GeneralPath result) {
        return reducePath( it, result, 1 );
    }
   
    /**
     * Returns the input GeneralPath filled with new points which will be rendered identically to the input path,
     * but contains a minimal number of points.  We bin average the points within a cell, because discretization
     * would mess up the label orientation in contour plotting.
     *
     * a new GeneralPath which will be rendered identically to the input path,
     * but contains a minimal number of points.
     *
     * @return the number of "points" (LINE_TOs) in the result.
     * @param it A path iterator with minute details that will be lost when rendering.
     * @param result A GeneralPath to put the result into.
     * @param res limit the resolution in pixels 
     */
    public static int reducePath(PathIterator it, GeneralPath result, int res ) {

        logger.fine( "enter reducePath" );
        long t0= System.currentTimeMillis();
        
        float[] p = new float[6];

        float x0 = Float.MAX_VALUE;
        float y0 = Float.MAX_VALUE;
        float sx0 = 0;
        float sy0 = 0;
        int nx0 = 0;
        int ny0 = 0;
        float ax0;
        float ay0;  // last averaged location

        int type0 = -999;

        float xres = res;
        float yres = res;

        //String[] types = new String[]{"M", "L", "QUAD", "CUBIC", "CLOSE"};

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
                case PathIterator.SEG_CUBICTO:
                    result.lineTo(ax0, ay0);
                    break;
                case PathIterator.SEG_CLOSE:
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
            case PathIterator.SEG_CUBICTO:
                result.lineTo(ax0, ay0);
                break;                
            case PathIterator.SEG_CLOSE:
                break;                
            case -999:
                //System.err.println(" ignore");
                break;
            default:
                throw new IllegalArgumentException("not supported");
        }

        logger.log(Level.FINE, "reduce {0} to {1} in {2}ms", new Object[]{inCount, points, System.currentTimeMillis()-t0 });
        return points;
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
     * parse strings like "14em+2pt" into a length in pixels.
     * <ul>
     * <li>"1em",0,8 -> 8
     * <li>"50%",240,0 -> 120
     * <li>"4pt",240,8 -> 4
     * <li>"4px",240,8 -> 4
     * <li>"1em+4pt",240,8 -> 12
     * </ul>
     * @param s the string specifying ems and pxs
     * @param totalWidth the total with for the normalized length.
     * @param em the size of an em in pixels.
     * @return the length in pixels
     * @see DasDevicePosition#parseLayoutStr(java.lang.String) 
     */
    public static double parseLayoutLength( String s, double totalWidth, double em ) {
        try {
            double[] dd= DasDevicePosition.parseLayoutStr((String)s);
            if ( dd[0]==0 && dd[1]==1 && dd[2]==0 ) {
                return em; 
            } else {
                double parentSize= em;
                double newSize= dd[0]*totalWidth + dd[1]*parentSize + dd[2];
                return newSize;
            }
        } catch (ParseException ex) {
            logger.log( Level.WARNING, null, ex.getMessage() );
            return 0.f;
        }
    }

    /**
     * return a string representation of the affine transforms used in DasPlot for
     * debugging.
     * @param at the affine transform
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
     * @param x0 the first point x
     * @param y0 the first point y
     * @param x1 the second point x
     * @param y1 the second point y
     * @return a double array with two elements [ slope, intercept ].
     */
    public static double[] getSlopeIntercept(double x0, double y0, double x1, double y1) {
        double slope = (y1 - y0) / (x1 - x0);
        double intercept = y0 - slope * x0;
        return new double[]{slope, intercept};
    }

    /**
     * return translucent white color for indicating the application is busy.
     * @return translucent white color 
     */
    public static Color getRicePaperColor() {
        return ColorUtil.getRicePaperColor();
    }

    /**
     * return a Gaussian filter for blurring images.
     * @param radius the radius filter in pixels.
     * @param horizontal true if horizontal blur. 
     * @return the ConvolveOp
     */
    public static ConvolveOp getGaussianBlurFilter(int radius,
            boolean horizontal) {
        if (radius < 1) {
            throw new IllegalArgumentException("Radius must be >= 1");
        }
        
        int size = radius * 2 + 1;
        float[] data = new float[size];
        
        float sigma = radius / 3.0f;
        float twoSigmaSquare = 2.0f * sigma * sigma;
        float sigmaRoot = (float) Math.sqrt(twoSigmaSquare * Math.PI);
        float total = 0.0f;
        
        for (int i = -radius; i <= radius; i++) {
            float distance = i * i;
            int index = i + radius;
            data[index] = (float) Math.exp(-distance / twoSigmaSquare) / sigmaRoot;
            total += data[index];
        }
        
        for (int i = 0; i < data.length; i++) {
            data[i] /= total;
        }        
        
        Kernel kernel;
        if (horizontal) {
            kernel = new Kernel(size, 1, data);
        } else {
            kernel = new Kernel(1, size, data);
        }
        return new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
    }
    
    /**
     * blur the image with a Guassian blur. 
     * @param im
     * @param size the size of the blur, roughly in pixels.
     * @return image
     */
    public static BufferedImage blurImage( BufferedImage im, int size ) {
        ConvolveOp op= getGaussianBlurFilter( size, true );
        BufferedImage out= new BufferedImage( im.getWidth(), im.getHeight(), im.getType() );
        op.filter( im, out );
        op= getGaussianBlurFilter( size, false );
        im= out;
        out= new BufferedImage( im.getWidth(), im.getHeight(), im.getType() );
        return op.filter( im, out );
    }
        
    /**
     * describe the path for debugging.
     * @param path the Path to describe
     * @param enumeratePoints if true, print all the points as well.
     * @return String description.
     */
    public static String describe(GeneralPath path, boolean enumeratePoints) {
        PathIterator it = path.getPathIterator(null);
        int count = 0;
        int lineToCount = 0;
        double[] seg = new double[6];
        while (!it.isDone()) {
            int type = it.currentSegment(seg);
            if (type == PathIterator.SEG_LINETO) {
                lineToCount++;
            }
            if (enumeratePoints) {
                if ( type==PathIterator.SEG_MOVETO ) {
                   System.err.println( String.format( Locale.US, "moveTo( %9.2f, %9.2f )\n", seg[0], seg[1] ) );
                } else if ( type==PathIterator.SEG_LINETO ) {
                   System.err.println( String.format( Locale.US, "lineTo( %9.2f, %9.2f )\n", seg[0], seg[1] ) );
                } else {
                    System.err.println( String.format( Locale.US, "%4d( %9.2f, %9.2f )\n", type, seg[0], seg[1] ) );
                }
            }
            count++;
            it.next();
        }
        System.err.println("count: " + count + "  lineToCount: " + lineToCount);
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
        Point2D result;
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
            
            if (noBoundsCheck ) {
                return result;
            } else {
                // calculate small number which can be treated as zero.
                double epsilon= -1 * Math.min( ( line1.getP1().distance(line1.getP2()) ), line2.getP1().distance(line2.getP2() ) ) / 10000.;
                if (((result.getX() - line1.getX1()) * (line1.getX2() - result.getX()) >= epsilon ) 
                    && ((result.getY() - line1.getY1()) * (line1.getY2() - result.getY()) >= epsilon ) 
                    && ((result.getX() - line2.getX1()) * (line2.getX2() - result.getX()) >= epsilon ) 
                    && ((result.getY() - line2.getY1()) * (line2.getY2() - result.getY()) >= epsilon ) ) {
                    return result;
                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
    }
    
    /**
     * return the line segment which is within the rectangle mask.
     * @param p0 the first point
     * @param p1 the second point
     * @param r the rectangle
     * @return null when they do not intersect, or the segment
     */
    public static Line2D lineRectangleMask( Point2D p0, Point2D p1, Rectangle2D r ) {
        Line2D.Double line= new Line2D.Double( p0, p1 );
        Point2D.Double r0= new Point2D.Double( r.getX(), r.getY() );
        Point2D.Double r1= new Point2D.Double( r.getX()+r.getWidth(), r.getY()+r.getHeight() );
        Point2D point1=null; 
        Point2D point2=null;
        Point2D p;
        p= lineIntersection( line, new Line2D.Double( r0.x, r0.y, r1.x, r0.y ), false );
        if ( p!=null ) point1= p;
        p= lineIntersection( line, new Line2D.Double( r1.x, r0.y, r1.x, r1.y ), false );
        if ( p!=null ) if ( point1==null ) point1= p; else point2= p;
        p= lineIntersection( line, new Line2D.Double( r1.x, r1.y, r0.x, r1.y ), false );
        if ( p!=null ) if ( point1==null ) point1= p; else point2= p; 
        p= lineIntersection( line, new Line2D.Double( r0.x, r1.y, r0.x, r0.y ), false );
        if ( p!=null ) if ( point1==null ) point1= p; else point2= p; 
        if ( point1==null ) {
            return null;
        } else if ( point2==null ) {
            if ( r.contains( p1 ) ) {
                return new Line2D.Double( point1, p1 );
            } else {
                return new Line2D.Double( p0, point1 );
            }
        } else if ( Point2D.distance( p0.getX(), p0.getY(), point1.getX(), point1.getY() ) < Point2D.distance( p0.getX(), p0.getY(), point2.getX(), point2.getY() ) ) {
            return new Line2D.Double( point1, point2 );
        } else {
            return new Line2D.Double( point2, point1 );
        }
        
    }
            
    /**
     * return the intersection of a line segment and the edge of a rectangle, 
     * where one point is outside of the rectangle and one is inside.
     * @param p0
     * @param p1
     * @param r0
     * @return null or the point along the rectangle
     */
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
     * return an icon block with the color and size.
     * @param iconColor the color
     * @param w the width in pixels
     * @param h the height in pixels
     * @return an icon.
     */
    public static Icon colorIcon( Color iconColor, int w, int h ) {
        return colorImageIcon(iconColor, w, h);
    }
    
    /**
     * return an ImageIcon with the color and size.
     * @param iconColor
     * @param w
     * @param h
     * @return 
     */
    public static ImageIcon colorImageIcon( Color iconColor, int w, int h ) {
        BufferedImage image= new BufferedImage( w, h, BufferedImage.TYPE_INT_ARGB );
        Graphics g= image.getGraphics();
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

    /** 
     * return rectangle with same center that is percent/100 of the
     * original width and height.
     * @param bounds the original rectangle.
     * @param percent the percent to increase (110% is 10% bigger)
     * @return a rectangle with same center that is percent/100. of the
     * original width and height.
     */
    public static Rectangle shrinkRectangle(Rectangle bounds, int percent ) {
        Rectangle result= new Rectangle( 
            bounds.x + bounds.width*(100-percent)/2/100, 
            bounds.y + bounds.height*(100-percent)/2/100, 
            bounds.width * percent / 100, 
            bounds.height * percent / 100 );
        return result;
    }
    
    /** 
     * return line shorted by so many pixels at each end.
     * @param line the line
     * @param l1 number of units to adjust the first point, towards the center
     * @param l2 number of units to adjust the second point, towards the center
     * @return the new line
     */
    public static Line2D shortenLine( Line2D line, double l1, double l2 ) {
        double len= line.getP1().distance( line.getP2() );
        if ( len==0 ) return line;
        double sx= ( line.getX2() - line.getX1() ) / len;
        double sy= ( line.getY2() - line.getY1() ) / len;
        return new Line2D.Double( line.getX1()+sx*l1, line.getY1()+sy*l1, line.getX2()-sx*l2, line.getY2()-sy*l2 );
    }
    
    /**
     * create a line perpendicular to the line segment <code>line</code>, which
     * would go through <code>p</code>, and have length <code>abs(len)</code>.
     * If len is negative, then line.p1,line.p2,p is counter-clockwise.
     * This is left unimplemented as it's a nice student project.
     * @param line a line segment.
     * @param p a point, whose projection is necessarily within the line segment.
     * @param len the length of the resulting line, or 
     * @return line colinear with p and having length abs(len).
     */
    public static Line2D perpendicularLine( Line2D line, Point p, double len ) {
        throw new IllegalArgumentException("not implemented.");
    }
    
    /**
     * DebuggingGeneralPath can be used for debugging.
     */
    public static class DebuggingGeneralPath {
        GeneralPath delegate;
        int count= 0;
        double lastfx0=0;
        double lastfy0=0;
        double initx=0;
        double inity=0;
        boolean arrows=false;
        boolean printRoute= true;
        
        DebuggingGeneralPath( int rule, int capacity ) {
            delegate= new GeneralPath( rule, capacity );
            System.err.println(String.format("==newPath=="));
            count= 0;
        }
        
        DebuggingGeneralPath( ) {
            delegate= new GeneralPath(GeneralPath.WIND_NON_ZERO, 20 );
            System.err.println(String.format("==newPath=="));
            count= 0;
        }        

        public void setArrows( boolean drawArrows ) {
            this.arrows= drawArrows;
        }
        
        public void lineTo(double fx, double fy) {
            if ( printRoute ) {
                System.err.println(new Formatter().format( Locale.US, "lineTo(%5.1f,%5.1f) %d",fx,fy,count ).toString());
            }
            if ( arrows ) {
                if ( inity==lastfy0 && initx==lastfx0 ) {
                    double perpy= fx-lastfx0;
                    double perpx= -1 * ( fy-lastfy0 );
                    double n= Math.sqrt( perpx*perpx + perpy*perpy );
                    perpx= perpx/n;
                    perpy= perpy/n;
                    int len=4;
                    delegate.lineTo( lastfx0 - perpx*len, lastfy0 - perpy*len );
                    delegate.lineTo( lastfx0 + perpx*len, lastfy0 + perpy*len );
                    delegate.moveTo( lastfx0, lastfy0 );
                }
            }
            delegate.lineTo(fx, fy);
            if ( arrows ) {
                double perpy= fx-lastfx0;
                double perpx= -1 * ( fy-lastfy0 );
                double n= Math.sqrt( perpx*perpx + perpy*perpy );
                perpx= perpx/n;
                perpy= perpy/n;
                int len=4;
                delegate.lineTo( fx+perpx*len - perpy*len, fy+perpy*len + perpx*len );
                delegate.lineTo( fx , fy );
            }
            lastfx0= fx;
            lastfy0= fy;
            count++;
        }

        public void moveTo(double fx, double fy) {
            if ( printRoute ) {
                System.err.println(new Formatter().format( Locale.US, "moveTo(%5.1f,%5.1f) %d",fx,fy,count ).toString());
            }
            //if ( count==3 ) {
            //    System.err.println("here1112");
            //}
            delegate.moveTo(fx,fy);
            lastfx0= fx;
            lastfy0= fy;       
            if ( count==0 ) {
                initx= fx;
                inity= fy;
            }
            count++;
        }

        PathIterator getPathIterator(AffineTransform at) {
            return delegate.getPathIterator(at);
        }

        GeneralPath getGeneralPath() {
            return delegate;
        }
    }
    
    /**
     * converts forward from relative font spec to point size, used by
     * the annotation and axis nodes.
     * @param dcc the canvas component.
     * @param fallbackFont the font to use when a font is not available, like "sans-8"
     * @return the converter that converts between strings like "1em" and the font.
     */
    public static Converter getFontConverter( final DasCanvasComponent dcc, final String fallbackFont ) {
        return new Converter() {
            @Override
            public Object convertForward(Object s) {
                try {
                    double[] dd= DasDevicePosition.parseLayoutStr((String)s);
                    Font f= dcc.getFont();
                    if ( f==null ) {
                        f= Font.decode( fallbackFont );
                    }
                    if ( dd[1]==1 && dd[2]==0 ) {
                        return f.getSize2D();
                    } else {
                        double parentSize= f.getSize2D();
                        double newSize= dd[1]*parentSize + dd[2];
                        return (float)newSize;
                    }
                } catch (ParseException ex) {
                    ex.printStackTrace();
                    return 0.f;
                }
            }

            @Override
            public Object convertReverse(Object t) {
                float size= (float)t;
                Font f= dcc.getFont();
                if ( f==null ) {
                    f= Font.decode( fallbackFont );
                }                
                if ( size==0 ) {
                    return "1em";
                } else {
                    double parentSize= f.getSize2D();
                    double relativeSize= size / parentSize;
                    return String.format( Locale.US, "%.2fem", relativeSize );
                }
            }  
        };
    }

    /**
     * return the number of minor ticks for the spacing.  This should be
     * return by searching for the first two factors.
     * @param dt the step size
     * @return the number of minor ticks
     */
    private static int updateTickVManualTicksMinor( double dt ) {
        int scale= (int)Math.log10(dt);
        dt= dt/Math.pow(10,scale);
        if ( dt==1. ) return 4;
        if ( dt==2. ) return 2;
        if ( dt==4. ) return 2;
        if ( dt==5. ) return 5;
        if ( dt==3. ) return 3;
        if ( dt==9. ) return 3;
        if ( dt==1.5 ) return 3;
        return 1;
    }
    
    /**
     * calculate a TickVDescriptor for the ticks.  
     * Example specifications:<ul>
     * <li>+20 every 20 units, whatever the data units are.
     * <li>+20s  every 20 seconds
     * <li>0,20,40,60,100  explicit locations.
     * <li>+20s/4 every 20 seconds, with four minor divisions.
     * </ul>
     * 
     * @param lticks the specification
     * @param dr the range to cover
     * @param log
     * @return null if the string can't be parsed, or the TickVDescriptor
     */
    public static TickVDescriptor calculateManualTicks( String lticks, DatumRange dr, boolean log ) {
        TickVDescriptor result;
        Units u= dr.getUnits();
        int islash= lticks.indexOf('/');
        int minorMult= 0;
        int[] minorList= null;
        if ( islash>-1 ) {
            String minorTicksSpec= lticks.substring(islash+1);
            lticks= lticks.substring(0,islash);
            if ( minorTicksSpec.contains(",") ) {
                String[] ss= minorTicksSpec.split(",");
                minorList= new int[ss.length];
                for ( int i=0; i<ss.length; i++ ) {
                    minorList[i]= Integer.parseInt(ss[i]);
                }
            } else {
                try {
                    minorMult= Integer.parseInt(minorTicksSpec);
                } catch ( NumberFormatException ex ) {
                    logger.log(Level.INFO, "unable to parse integer after slash: {0}", lticks);
                }
            }
        }
        if ( lticks.startsWith("+") ) {
            try {
                Datum tickM= u.getOffsetUnits().parse(lticks.substring(1));
                double min= dr.min().doubleValue(u);
                double max= dr.max().doubleValue(u);
                double dt= tickM.doubleValue(u.getOffsetUnits());
                if ( dt==0. ) {
                    logger.warning("delta ticks cannot be 0.");
                    return null;
                }
                double firstTick= Math.floor( min/dt )*dt;
                double lastTick= Math.ceil( max/dt )*dt;
                int ntick= (int)( ( lastTick - firstTick ) / dt ) + 1;
                ntick= Math.max( 0, ntick );
                ntick= Math.min( 120, ntick );
                double[] dticks= new double[ ntick ];
                for ( int i=0; i<dticks.length; i++ ) {
                    dticks[i]= firstTick + i*dt; // TODO: rewrite unstable
                }
                double[] dticksMinor;
                if ( minorList!=null ) {
                    dticksMinor= new double[ ntick*minorList.length ];
                    for ( int i=0; i<dticks.length; i++ ) {
                        double dd= dticks[i];
                        for ( int j=0; j<minorList.length; j++ ) {
                            dticksMinor[i*minorList.length+j]= dd + minorList[j]; 
                        }
                    }                    
                } else {
                    int minorTicks= minorMult>0 ? minorMult : updateTickVManualTicksMinor(dt);
                    dt= dt/minorTicks;
                    dticksMinor= new double[ ntick*minorTicks ];
                    for ( int i=0; i<dticksMinor.length; i++ ) {
                        dticksMinor[i]= firstTick + i*dt; // TODO: rewrite unstable
                    }
                }
                TickVDescriptor majorTicks= new TickVDescriptor( dticksMinor, dticks, u );
                result= majorTicks;
            } catch (ParseException ex) {
                logger.warning(ex.getMessage());
                result= null;
            }
        } else if ( lticks.startsWith("*") ) {
            try {
                Datum tickM= u.getOffsetUnits().parse(lticks.substring(1));
                double min= dr.min().doubleValue(u);
                double max= dr.max().doubleValue(u);
                double dt= Math.log10( tickM.doubleValue(u.getOffsetUnits()) );
                if ( dt==0. ) {
                    logger.warning("delta ticks cannot be 0.");
                    return null;
                }
                double firstTick= Math.floor( Math.log10(min)/dt )*dt;
                double lastTick= Math.ceil( Math.log10(max)/dt )*dt;
                int ntick= (int)( ( lastTick - firstTick ) / dt ) + 1;
                ntick= Math.max( 0, ntick );
                ntick= Math.min( 120, ntick );                
                double[] dticks= new double[ ntick ];
                for ( int i=0; i<dticks.length; i++ ) {
                    dticks[i]= Math.pow( 10, firstTick + i * dt );
                }
                List<Double> dticksMinorList= new ArrayList<>();
                //double[] dticksMinor= new double[ ntick*minorTicks ];
                if ( minorList==null ) {
                    if ( minorMult==2 ) {
                        minorList= new int[] { 10 };
                    } else if ( minorMult==3 ) {
                        minorList= new int[] { 10, 100 };
                    } else {
                        minorList= new int[] { 2,3,4,5,6,7,8,9 };
                    }
                }
                for ( int i=0; i<dticks.length-1; i++ ) {
                    double d= dticks[i];
                    dticksMinorList.add( d );
                    for ( int j=0; j<minorList.length; j++ ) {                        
                        dticksMinorList.add( d * minorList[j] );
                    }
                }
                double[] dticksMinor= new double[dticksMinorList.size()];
                for ( int i=0; i<dticksMinor.length; i++ ) {
                    dticksMinor[i]= dticksMinorList.get(i);
                }
                TickVDescriptor majorTicks= new TickVDescriptor( dticksMinor, dticks, u );
                result= majorTicks;
            } catch (ParseException ex) {
                logger.warning(ex.getMessage());
                result= null;
            }            
        } else {
            String[] ss= lticks.split(",");
            double[] dticks= new double[ss.length];
            for ( int i=0; i<dticks.length; i++ ) {
                try {
                    dticks[i]= u.parse(ss[i]).doubleValue(u);
                } catch (ParseException ex) {
                    logger.log(Level.WARNING, "failed to parse tick: {0}", ss[i]);
                    dticks[i]= 0;
                }
            }
            double[] dticksMinor;
            if ( dticks.length>2 ) {
                double dt= DasMath.gcd( dticks, (dticks[1]-dticks[0])/100. );
                int minorTicks= minorMult>0 ? minorMult : updateTickVManualTicksMinor(dt);
                dt= dt/minorTicks;
                double firstTick= DasMath.min(dticks);
                double lastTick= DasMath.max(dticks);
                int ntick= (int)(Math.ceil(lastTick-firstTick)/dt) + 1;
                dticksMinor= new double[ ntick ];
                for ( int i=0; i<dticksMinor.length; i++ ) {
                    dticksMinor[i]= firstTick + i * dt;
                }
            } else {
                dticksMinor= dticks;
            }
            TickVDescriptor majorTicks= new TickVDescriptor( dticksMinor, dticks, u );
            result= majorTicks;
        }
        return result;
    }


}
