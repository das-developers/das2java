/* File: SeriesRenderer.java
 * Copyright (C) 2002-2003 The University of Iowa
 * Created by: Jeremy Faden <jbf@space.physics.uiowa.edu>
 *             Jessica Swanner <jessica@space.physics.uiowa.edu>
 *             Edward E. West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package edu.uiowa.physics.pw.das.graph;

import edu.uiowa.physics.pw.das.DasApplication;
import edu.uiowa.physics.pw.das.DasException;
import edu.uiowa.physics.pw.das.DasProperties;
import edu.uiowa.physics.pw.das.components.propertyeditor.Displayable;
import edu.uiowa.physics.pw.das.dataset.DataSet;
import edu.uiowa.physics.pw.das.dataset.DataSetUtil;
import edu.uiowa.physics.pw.das.dataset.VectorDataSet;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.DatumRange;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.event.DasMouseInputAdapter;
import edu.uiowa.physics.pw.das.event.LengthDragRenderer;
import edu.uiowa.physics.pw.das.event.MouseModule;
import edu.uiowa.physics.pw.das.system.DasLogger;
import edu.uiowa.physics.pw.das.util.DasProgressMonitor;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * SeriesRender is a high-performance replacement for the SymbolLineRenderer.
 * The SymbolLineRenderer is limited to about 30,000 points, beyond which 
 * contracts for speed start breaking degrading usability.  The goal of the
 * SeriesRenderer is to plot 1,000,000 points without breaking the contracts.
 * 
 * @author  jbf
 */
public class SeriesRenderer extends Renderer implements Displayable {

    private DefaultPlotSymbol psym = DefaultPlotSymbol.CIRCLES;
    private double symSize = 3.0; // radius in pixels
    private double lineWidth = 1.0; // width in pixels
    private boolean histogram = false;
    //private Stroke stroke;
    private PsymConnector psymConnector = PsymConnector.SOLID;
    private FillStyle fillStyle = FillStyle.STYLE_FILL;
    private int renderCount = 0;
    private int updateImageCount = 0;
    /** Holds value of property color. */
    private Color color = Color.BLACK;
    private long lastUpdateMillis;
    /** Holds value of property antiAliased. */
    private boolean antiAliased = "on".equals(DasProperties.getInstance().get("antiAlias"));

    /* the cached data to speed up render step */
    private GeneralPath path;
    private GeneralPath fillToRefPath;
    private GeneralPath psymsPath; // store the location of the psyms here.
    private int firstIndex;/* the index of the first point drawn, nonzero when X is monotonic and we can clip. */

    private int lastIndex;/* the non-inclusive index of the last point drawn. */

    private Logger log = DasLogger.getLogger(DasLogger.GRAPHICS_LOG);
    /**
     * indicates the dataset was clipped by dataSetSizeLimit 
     */
    private boolean dataSetClipped;

    public SeriesRenderer() {
        super();
        updatePsym();
    }
    Image psymImage;
    Image[] coloredPsyms;
    int cmx, cmy;

    /**
     * updates the image of a psym that is stamped
     */
    private void updatePsym() {
        int sx = (int) Math.ceil(symSize + 2 * lineWidth);
        int sy = (int) Math.ceil(symSize + 2 * lineWidth);
        double dcmx = (int) (lineWidth + symSize / 2);
        double dcmy = (int) (lineWidth + symSize / 2);

        BufferedImage image = new BufferedImage(sx, sy, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) image.getGraphics();

        Object rendering = antiAliased ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, rendering);
        g.setColor(color);
        if (parent != null) {
            g.setBackground(parent.getBackground());
        }

        g.setStroke(new BasicStroke((float) lineWidth));

        psym.draw(g, dcmx, dcmy, (float) symSize, fillStyle);
        psymImage = image;

        if (colorBar != null) {
            IndexColorModel model = colorBar.getIndexColorModel();
            coloredPsyms = new Image[model.getMapSize()];
            for (int i = 0; i < model.getMapSize(); i++) {
                Color c = new Color(model.getRGB(i));
                image = new BufferedImage(sx, sy, BufferedImage.TYPE_INT_ARGB);
                g = (Graphics2D) image.getGraphics();
                if (parent != null) {
                    g.setBackground(parent.getBackground());
                }
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, rendering);
                g.setColor(c);
                g.setStroke(new BasicStroke((float) lineWidth));

                psym.draw(g, dcmx, dcmy, (float) symSize, this.fillStyle);
                coloredPsyms[i] = image;
            }
        }
        
        cmx= (int)dcmx;
        cmy= (int)dcmy;
        
        refresh();
    }

    /**
     * render the psyms by stamping an image at the psym location.  The intent is to
     * provide fast rendering by reducing fidelity.
     * On 20080206, this was measured to run at 320pts/millisecond for FillStyle.FILL
     * On 20080206, this was measured to run at 300pts/millisecond in FillStyle.OUTLINE
     * @param g
     * @param xAxis
     * @param yAxis
     * @param vds
     * @param mon
     * @return
     */
    private int renderPsymsStamp(Graphics g, DasAxis xAxis, DasAxis yAxis, VectorDataSet vds, DasProgressMonitor mon) {

        Units yunits = vds.getYUnits();
        Units xunits = vds.getXUnits();

        VectorDataSet colorByDataSet = null;
        if (colorByDataSetId != null) {
            colorByDataSet = (VectorDataSet) vds.getPlanarView(colorByDataSetId);
        }

        //System.err.println("psymsPath= " + GraphUtil.describe(psymsPath, false));
        double[] coords = new double[6];
        PathIterator it = psymsPath.getPathIterator(null);

        int i = firstIndex - 1; // first point is always a SEG_MOVETO

        if (colorByDataSet != null) {
            Units cunits = colorBar.getUnits();
            while (!it.isDone()) {

                int type = it.currentSegment(coords);
                Datum d = vds.getDatum(Math.max(i, 0));
                //System.err.println("i:" + i + "  " + type + " " + coords[0] + "," + coords[1] + "  " + d);

                boolean drawIt = type == PathIterator.SEG_LINETO;

                if (drawIt) {
                    int icolor = colorBar.indexColorTransform(colorByDataSet.getDouble(i, cunits), cunits);
                    g.drawImage(coloredPsyms[icolor], (int) coords[0] - cmx, (int) coords[1] - cmy, parent);
                }

                i++;
                it.next();

                if (mon.isCancelled()) {
                    break;
                }
                mon.setTaskProgress(Math.min(i - firstIndex, lastIndex - firstIndex));
            }
        } else {
            while (!it.isDone()) {

                boolean drawIt = it.currentSegment(coords) == PathIterator.SEG_LINETO;

                if (drawIt) {
                    g.drawImage(psymImage, (int) coords[0] - cmx, (int) coords[1] - cmy, parent);
                    i++;
                }

                it.next();
                if (mon.isCancelled()) {
                    break;
                }
                mon.setTaskProgress(Math.min(i - firstIndex, lastIndex - firstIndex));
            }
        }

        if (dataSetClipped) {
            parent.postMessage(this, "rendering stopped!cat " + dataSetSizeLimit + " points", DasPlot.WARNING, null, null);
        }

        return vds.getXLength();

    }

    /**
     * Render the psyms individually.  This is the highest fidelity rendering, and
     * should be used in printing.
     * On 20080206, this was measured to run at 45pts/millisecond in FillStyle.FILL
     * On 20080206, this was measured to run at 9pts/millisecond in FillStyle.OUTLINE
     * @param graphics
     * @param xAxis
     * @param yAxis
     * @param dataSet
     * @param mon
     * @return
     */
    private int renderPsyms(Graphics2D graphics, DasAxis xAxis, DasAxis yAxis, VectorDataSet dataSet, DasProgressMonitor mon) {

        Units zunits = null;

        float fsymSize = (float) symSize;
        mon.setTaskSize(lastIndex - firstIndex);
        mon.started();

        if (colorByDataSetId != null) {
            colorByDataSet = (VectorDataSet) dataSet.getPlanarView(colorByDataSetId);
            zunits = colorBar.getUnits();
        }

        //System.err.println("psymsPath= " + GraphUtil.describe(psymsPath, false));

        PathIterator it = psymsPath.getPathIterator(null);
        //PathIterator it = path.getPathIterator(null);

        double[] coords = new double[6];

        double lastX = Double.NaN;

        graphics.setStroke(new BasicStroke((float) lineWidth));

        int i = firstIndex - 1;

        if (colorByDataSet != null) {
            while (!it.isDone()) {

                boolean drawIt = it.currentSegment(coords) == PathIterator.SEG_LINETO;

                if (drawIt) {
                    graphics.setColor(new Color(colorBar.rgbTransform(colorByDataSet.getDouble(i, zunits), zunits)));
                    psym.draw(graphics, coords[0], coords[1], fsymSize, fillStyle);
                }

                i++;
                it.next();

                if (mon.isCancelled()) {
                    break;
                }
                mon.setTaskProgress(Math.min(i - firstIndex, lastIndex - firstIndex));
            }
        } else {
            while (!it.isDone()) {
                int type = it.currentSegment(coords);
                boolean drawIt = it.currentSegment(coords) == PathIterator.SEG_LINETO;

                if (drawIt) {
                    psym.draw(graphics, coords[0], coords[1], fsymSize, fillStyle);
                }

                i++;
                it.next();

                if (mon.isCancelled()) {
                    break;
                }
                mon.setTaskProgress(Math.min(i - firstIndex, lastIndex - firstIndex));
            }
        }

        if (dataSetClipped) {
            parent.postMessage(this, "rendering stopped!cat " + dataSetSizeLimit + " points", DasPlot.WARNING, null, null);
        }

        return i;
    }

    private void reportCount() {
    //if ( renderCount % 100 ==0 ) {
    //System.err.println("  updates: "+updateImageCount+"   renders: "+renderCount );
    //new Throwable("").printStackTrace();
    //}
    }

    public void render(Graphics g, DasAxis xAxis, DasAxis yAxis, DasProgressMonitor mon) {

        renderCount++;
        reportCount();

        long timer0 = System.currentTimeMillis();

        VectorDataSet dataSet = (VectorDataSet) getDataSet();

        if (this.ds == null && lastException != null) {
            renderException(g, xAxis, yAxis, lastException);
            return;
        }

        if (dataSet == null) {
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("null data set");
            parent.postMessage(this, "no data set", DasPlot.INFO, null, null);
            return;
        }

        if (dataSet.getXLength() == 0) {
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("empty data set");
            parent.postMessage(this, "empty data set", DasPlot.INFO, null, null);
            return;
        }

        if (!(dataSet.getXUnits().isConvertableTo(xAxis.getUnits()) && dataSet.getYUnits().isConvertableTo(yAxis.getUnits()))) {
            return;
        }

        // sometimes this happens, TODO: investigate
        if (path == null) {
            return;
        }


        DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("render data set " + dataSet);

        Graphics2D graphics = (Graphics2D) g.create();

        if (antiAliased) {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        } else {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        }

        edu.uiowa.physics.pw.das.datum.Units xUnits = xAxis.getUnits();
        edu.uiowa.physics.pw.das.datum.Units yUnits = yAxis.getUnits();

        if (this.fillToReference && fillToRefPath != null) {
            graphics.setColor(fillColor);
            graphics.fill(fillToRefPath);
        }

        graphics.setColor(color);
        log.finest("drawing psymConnector in " + color);

        //String s = GraphUtil.describe(path, false);
        //s = GraphUtil.describe(path, false);
        //System.err.println(s);

        psymConnector.draw(graphics, path, (float) lineWidth);

        double xmin;
        double xmax;
        double ymin;
        double ymax;

        Rectangle r = graphics.getClipBounds();

        if (r == null) {
            xmax = xAxis.getDataMaximum().doubleValue(xUnits);
            xmin = xAxis.getDataMinimum().doubleValue(xUnits);
            ymax = yAxis.getDataMaximum().doubleValue(yUnits);
            ymin = yAxis.getDataMinimum().doubleValue(yUnits);
        } else {
            xmin = xAxis.invTransform((int) r.getX()).doubleValue(xUnits);
            xmax = xAxis.invTransform((int) (r.getX() + r.getWidth())).doubleValue(xUnits);
            ymin = yAxis.invTransform((int) r.getY()).doubleValue(yUnits);
            ymax = yAxis.invTransform((int) (r.getY() + r.getHeight())).doubleValue(yUnits);
        }

        graphics.setColor(color);

        if (psym != DefaultPlotSymbol.NONE) {
            int i;

            if ( stampPsyms && ! parent.getCanvas().isPrintingThread() ) {
                i = renderPsymsStamp(graphics, xAxis, yAxis, dataSet, mon);
            } else {
                i = renderPsyms(graphics, xAxis, yAxis, dataSet, mon);
            }

            //double simplifyFactor = (double) (  i - firstIndex ) / (lastIndex - firstIndex);

            if (i - firstIndex == 0) {
                parent.postMessage(this, "dataset contains no valid data", DasPlot.INFO, null, null);
            }
            mon.finished();
        }

        //g.drawString( "renderCount="+renderCount+" updateCount="+updateImageCount,xAxis.getColumn().getDMinimum()+5, yAxis.getRow().getDMinimum()+20 );
        long milli = System.currentTimeMillis();
        long renderTime = (milli - timer0);
        double dppms = (lastIndex - firstIndex) / (double) renderTime;
        
        setRenderPointsPerMillisecond( dppms );
        
        logger.finer("render: " + renderTime + " total:" + (milli - lastUpdateMillis) + " fps:" + (1000. / (milli - lastUpdateMillis)) + " pts/ms:" + dppms);
        lastUpdateMillis = milli;
    }
    boolean updating = false;

    /**
     * do the same as updatePlotImage, but use AffineTransform to implement axis transform.
     */
    @Override
    public void updatePlotImage(DasAxis xAxis, DasAxis yAxis, DasProgressMonitor monitor) {

        updating = true;

        updateImageCount++;
        reportCount();

        try {
            super.updatePlotImage(xAxis, yAxis, monitor);
        } catch (DasException e) {
            // it doesn't throw DasException, but interface requires exception, jbf 5/26/2005
            throw new RuntimeException(e);
        }

        logger.fine("entering updatePlotImage");
        long t0 = System.currentTimeMillis();

        boolean histogram = this.histogram;
        VectorDataSet dataSet = (VectorDataSet) getDataSet();
        if (dataSet == null || dataSet.getXLength() == 0) {
            return;
        }

        if (!(dataSet.getXUnits().isConvertableTo(xAxis.getUnits()) && dataSet.getYUnits().isConvertableTo(yAxis.getUnits()))) {
            return;
        }

        Dimension d;

        double xmin;
        double xmax;
        double ymin;
        double ymax;
        int ixmax;
        int ixmin;

        Units xUnits = xAxis.getUnits();
        Units yUnits = yAxis.getUnits();

        DatumRange visibleRange = xAxis.getDatumRange();

        xmax = visibleRange.min().doubleValue(xUnits);
        xmin = visibleRange.max().doubleValue(xUnits);
        ymax = yAxis.getDataMaximum().doubleValue(yUnits);
        ymin = yAxis.getDataMinimum().doubleValue(yUnits);

        Boolean xMono = (Boolean) dataSet.getProperty(DataSet.PROPERTY_X_MONOTONIC);
        if (xMono != null && xMono.booleanValue()) {
            ixmin = DataSetUtil.getPreviousColumn(dataSet, visibleRange.min());
            ixmax = DataSetUtil.getNextColumn(dataSet, visibleRange.max()) + 1;
        } else {
            ixmin = 0;
            ixmax = dataSet.getXLength();
        }

        GeneralPath newPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, 110 * (ixmax - ixmin) / 100);
        GeneralPath fillPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, 110 * (ixmax - ixmin) / 100);
        GeneralPath newPymsPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, 110 * (ixmax - ixmin) / 100);

        Datum sw = DataSetUtil.guessXTagWidth(dataSet);
        double xSampleWidth = sw.doubleValue(xUnits.getOffsetUnits());

        /* fuzz the xSampleWidth */
        xSampleWidth = xSampleWidth * 1.10;

        if (reference != null && reference.getUnits() != yAxis.getUnits()) {
            // switch the units to the axis units.
            reference = yAxis.getUnits().createDatum(reference.doubleValue(reference.getUnits()));
        }

        if (reference == null) {
            reference = yUnits.createDatum(yAxis.isLog() ? 1.0 : 0.0);
        }

        double yref = (double) reference.doubleValue(yUnits);

        double x = Double.NaN;
        double y = Double.NaN;

        double x0 = Double.NaN; /* the last plottable point */
        double y0 = Double.NaN; /* the last plottable point */

        float fyref = (float) yAxis.transform(yref, yUnits);
        float fx = Float.NaN;
        float fy = Float.NaN;
        float fx0 = Float.NaN;
        float fy0 = Float.NaN;

        int index;

        // find the first valid point, set x0, y0 //
        for (index = ixmin; index < ixmax; index++) {
            x = (double) dataSet.getXTagDouble(index, xUnits);
            y = (double) dataSet.getDouble(index, yUnits);

            final boolean isValid = yUnits.isValid(y) && xUnits.isValid(x);
            if (isValid) {
                x0 = x;
                y0 = y;
                firstIndex = index;  // TODO: what if no valid points?
                index++;
                break;
            }
        }

        // first point //
        fx = (float) xAxis.transform(x, xUnits);
        fy = (float) yAxis.transform(y, yUnits);

        if (histogram) {
            float fx1 = (float) xAxis.transform(x - xSampleWidth / 2, xUnits);
            newPath.moveTo(fx1, fy);
            newPath.lineTo(fx, fy);
            fillPath.moveTo(fx1, fyref);
            fillPath.lineTo(fx1, fy);
            fillPath.lineTo(fx, fy);
            newPymsPath.moveTo(fx, fy);// lineTo will be done later
        } else {
            newPath.moveTo(fx, fy);
            newPath.lineTo(fx, fy);
            fillPath.moveTo(fx, fyref);
            fillPath.lineTo(fx, fy);
            newPymsPath.moveTo(fx, fy); // lineTo will be done later
        }
        fx0 = fx;
        fy0 = fy;

        int pointsPlotted = 1;

        if (psymConnector != PsymConnector.NONE || fillToReference) {
            // now loop through all of them. //
            for (; index < ixmax && pointsPlotted < dataSetSizeLimit; index++) {

                x = dataSet.getXTagDouble(index, xUnits);
                y = dataSet.getDouble(index, yUnits);

                final boolean isValid = yUnits.isValid(y) && xUnits.isValid(x);

                fx = (float) xAxis.transform(x, xUnits);
                fy = (float) yAxis.transform(y, yUnits);

                //double tx= xAxis.transformFast( x, xUnits );

                //System.err.println( ""+(float)tx+ "   " + fx );

                if (isValid) {
                    if ((x - x0) < xSampleWidth) {
                        // draw connect-a-dot between last valid and here
                        if (histogram) {
                            float fx1 = (fx0 + fx) / 2;
                            newPath.lineTo(fx1, fy0);
                            newPath.lineTo(fx1, fy);
                            newPath.lineTo(fx, fy);
                            fillPath.lineTo(fx1, fy0);
                            fillPath.lineTo(fx1, fy);
                            fillPath.lineTo(fx, fy);
                        } else {
                            newPath.lineTo(fx, fy); // this is the typical path
                            fillPath.lineTo(fx, fy);
                        }
                    } else {
                        // introduce break in line
                        if (histogram) {
                            float fx1 = (float) xAxis.transform(x0 + xSampleWidth / 2, xUnits);
                            newPath.lineTo(fx1, fy0);
                            fillPath.lineTo(fx1, fy0);
                            fillPath.lineTo(fx1, fyref);

                            fx1 = (float) xAxis.transform(x - xSampleWidth / 2, xUnits);
                            newPath.moveTo(fx1, fy);
                            newPath.lineTo(fx, fy);
                            fillPath.moveTo(fx1, fyref);
                            fillPath.lineTo(fx1, fy);
                            fillPath.lineTo(fx, fy);

                        } else {
                            newPath.moveTo(fx, fy);
                            newPath.lineTo(fx, fy);
                            fillPath.lineTo(fx0, fyref);
                            fillPath.moveTo(fx, fyref);
                            fillPath.lineTo(fx, fy);
                        }
                    } // else introduce break in line
                    x0 = x;
                    y0 = y;
                    fx0 = fx;
                    fy0 = fy;
                    pointsPlotted++;
                } else {
                    newPath.moveTo(fx0, fy0); // place holder
                }

            } // for ( ; index < ixmax && pointsPlotted < dataSetSizeLimit; index++ )
        }

        lastIndex = index;

        if (psym != DefaultPlotSymbol.NONE) {
            pointsPlotted = 0;
            for ( index = firstIndex; index < ixmax && pointsPlotted < dataSetSizeLimit; index++) {
                x = dataSet.getXTagDouble(index, xUnits);
                y = dataSet.getDouble(index, yUnits);

                final boolean isValid = yUnits.isValid(y) && xUnits.isValid(x);

                fx = (float) xAxis.transform(x, xUnits);
                fy = (float) yAxis.transform(y, yUnits);

                if (isValid) {
                    newPymsPath.lineTo(fx, fy);
                    pointsPlotted++;
                } else {
                    newPymsPath.moveTo(fx, fy);
                }
            }
            lastIndex = index;
        }

        if (index < ixmax) {
            dataSetClipped = true;
        } else {
            dataSetClipped = false;
        }

        fillPath.lineTo(fx0, fyref);

        this.fillToRefPath = fillPath;

        if (this.fillToReference && fillToRefPath != null) {
            if (simplifyPaths) {
                fillToRefPath = GraphUtil.reducePath(fillToRefPath.getPathIterator(null), new GeneralPath(GeneralPath.WIND_NON_ZERO, lastIndex - firstIndex));
            }
        }

        //System.err.println(GraphUtil.describe(newPath, true));
        this.path = newPath;
        this.psymsPath = newPymsPath;

        // draw the stored path that we calculated in updatePlotImage
        if (simplifyPaths && colorByDataSet == null) {
            if (histogram) {
                this.path = newPath;
                this.psymsPath = GraphUtil.reducePath(newPymsPath.getPathIterator(null), new GeneralPath(GeneralPath.WIND_NON_ZERO, lastIndex - firstIndex));
            } else {
                this.path = GraphUtil.reducePath(newPath.getPathIterator(null), new GeneralPath(GeneralPath.WIND_NON_ZERO, lastIndex - firstIndex));
                this.psymsPath = GraphUtil.reducePath( newPymsPath.getPathIterator(null), new GeneralPath(GeneralPath.WIND_NON_ZERO, lastIndex - firstIndex) );
            }
        }

        if (getParent() != null) {
            getParent().repaint();
        }
        
        
        logger.fine("done updatePlotImage in " + (System.currentTimeMillis() - t0) + " ms");
        updating = false;
                 
        long milli = System.currentTimeMillis();
        long renderTime = (milli - t0);
        double dppms = (lastIndex - firstIndex) / (double) renderTime;
        
        setUpdatesPointsPerMillisecond( dppms );
    }

    
    public PsymConnector getPsymConnector() {
        return psymConnector;
    }

    public void setPsymConnector(PsymConnector p) {
        PsymConnector old = this.psymConnector;
        if (!p.equals(psymConnector)) {
            psymConnector = p;
            refreshImage();
            propertyChangeSupport.firePropertyChange("psymConnector", old, p);
        }
    }

    /** Getter for property psym.
     * @return Value of property psym.
     */
    public PlotSymbol getPsym() {
        return this.psym;
    }

    /** Setter for property psym.
     * @param psym New value of property psym.
     */
    public void setPsym(PlotSymbol psym) {
        if (psym == null) {
            throw new NullPointerException("psym cannot be null");
        }
        if (psym != this.psym) {
            Object oldValue = this.psym;
            this.psym = (DefaultPlotSymbol) psym;
            updatePsym();
            refreshImage();
            propertyChangeSupport.firePropertyChange("psym", oldValue, psym);
        }
    }

    /** Getter for property symsize.
     * @return Value of property symsize.
     */
    public double getSymSize() {
        return this.symSize;
    }

    /** Setter for property symsize.
     * @param symSize New value of property symsize.
     */
    public void setSymSize(double symSize) {
        double old = this.symSize;
        if (this.symSize != symSize) {
            this.symSize = symSize;
            setPsym(this.psym);
            updatePsym();
            refreshImage();
            propertyChangeSupport.firePropertyChange("symSize", new Double(old), new Double(symSize));
        }
    }

    /** Getter for property color.
     * @return Value of property color.
     */
    public Color getColor() {
        return color;
    }

    /** Setter for property color.
     * @param color New value of property color.
     */
    public void setColor(Color color) {
        Color old = this.color;
        if (!this.color.equals(color)) {
            this.color = color;
            updatePsym();
            refreshImage();
            propertyChangeSupport.firePropertyChange("color", old, color);
            updatePsym();
        }
    }

    public double getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(double f) {
        double old = this.lineWidth;
        if (this.lineWidth != f) {
            lineWidth = f;
            updatePsym();
            refreshImage();
            propertyChangeSupport.firePropertyChange("lineWidth", new Double(old), new Double(f));
        }
    }

    protected void installRenderer() {
        if (!DasApplication.getDefaultApplication().isHeadless()) {
            DasMouseInputAdapter mouseAdapter = parent.mouseAdapter;
            DasPlot p = parent;
            mouseAdapter.addMouseModule(new MouseModule(p, new LengthDragRenderer(p, p.getXAxis(), p.getYAxis()), "Length"));
        }
        updatePsym();
    }

    protected void uninstallRenderer() {
    }

    public Element getDOMElement(Document document) {
        return null;
    }

    /** Getter for property antiAliased.
     * @return Value of property antiAliased.
     *
     */
    public boolean isAntiAliased() {
        return this.antiAliased;
    }

    /** Setter for property antiAliased.
     * @param antiAliased New value of property antiAliased.
     *
     */
    public void setAntiAliased(boolean antiAliased) {
        boolean old = this.antiAliased;
        this.antiAliased = antiAliased;
        refreshImage();
        propertyChangeSupport.firePropertyChange("antiAliased", old, antiAliased);
    }

    public boolean isHistogram() {
        return histogram;
    }

    public void setHistogram(final boolean b) {
        boolean old = b;
        if (b != histogram) {
            histogram = b;
            refreshImage();
            propertyChangeSupport.firePropertyChange("histogram", old, antiAliased);
        }
    }

    public String getListLabel() {
        return String.valueOf(this.getDataSetDescriptor());
    }

    public javax.swing.Icon getListIcon() {
        Image i = new BufferedImage(15, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) i.getGraphics();
        g.setRenderingHints(DasProperties.getRenderingHints());
        g.setBackground(parent.getBackground());

        // leave transparent if not white
        if (color.equals(Color.white)) {
            g.setColor(Color.GRAY);
        } else {
            g.setColor(new Color(0, 0, 0, 0));
        }
        g.fillRect(0, 0, 15, 10);

        if (fillToReference) {
            g.setColor(fillColor);
            Polygon p = new Polygon(new int[]{2, 13, 13, 2}, new int[]{3, 7, 10, 10}, 4);
            g.fillPolygon(p);
        }

        g.setColor(color);
        Stroke stroke0 = g.getStroke();
        getPsymConnector().drawLine(g, 2, 3, 13, 7, 1.5f);
        g.setStroke(stroke0);
        psym.draw(g, 7, 5, 3.f, fillStyle);
        return new ImageIcon(i);
    }
    /**
     * Holds value of property selected.
     */
    private boolean selected;

    /**
     * Getter for property selected.
     * @return Value of property selected.
     */
    public boolean isSelected() {
        return this.selected;
    }

    /**
     * Setter for property selected.
     * @param selected New value of property selected.
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    /**
     * Holds value of property fillColor.
     */
    private Color fillColor = Color.lightGray;

    /**
     * Getter for property fillReference.
     * @return Value of property fillReference.
     */
    public Color getFillColor() {
        return this.fillColor;
    }

    /**
     * Setter for property fillReference.
     * @param fillReference New value of property fillReference.
     */
    public void setFillColor(Color color) {
        Color old = this.fillColor;
        if (!this.fillColor.equals(color)) {
            this.fillColor = color;
            refresh();
            propertyChangeSupport.firePropertyChange("fillColor", old, color);
        }
    }
    /**
     * Holds value of property colorByDataSetId.
     */
    private String colorByDataSetId = null;

    /**
     * Getter for property colorByDataSetId.
     * @return Value of property colorByDataSetId.
     */
    public String getColorByDataSetId() {
        return this.colorByDataSetId;
    }

    /**
     * Setter for property colorByDataSetId.
     * @param colorByDataSetId New value of property colorByDataSetId.
     */
    public void setColorByDataSetId(String colorByDataSetId) {
        String oldVal= this.colorByDataSetId;
        this.colorByDataSetId = colorByDataSetId;
        refresh();
        propertyChangeSupport.firePropertyChange("colorByDataSetId", oldVal, colorByDataSetId );
    }
    /**
     * Holds value of property colorBar.
     */
    private DasColorBar colorBar;

    /**
     * Getter for property colorBar.
     * @return Value of property colorBar.
     */
    public DasColorBar getColorBar() {
        return this.colorBar;
    }

    /**
     * Setter for property colorBar.
     * @param colorBar New value of property colorBar.
     */
    public void setColorBar(DasColorBar colorBar) {
        this.colorBar = colorBar;
        colorBar.addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                if (colorByDataSetId != null) {
                    refresh();
                }
            }
        });
        refreshImage();
        updatePsym();
    }
    /**
     * Holds value of property fillToReference.
     */
    private boolean fillToReference;

    /**
     * Getter for property fillToReference.
     * @return Value of property fillToReference.
     */
    public boolean isFillToReference() {
        return this.fillToReference;
    }

    /**
     * Setter for property fillToReference.
     * @param fillToReference New value of property fillToReference.
     */
    public void setFillToReference(boolean fillToReference) {
        boolean old = this.fillToReference;
        if (this.fillToReference != fillToReference) {
            this.fillToReference = fillToReference;
            refresh();
            propertyChangeSupport.firePropertyChange("fillToReference", old, fillToReference);
        }
    }
    /**
     * Holds value of property reference.
     */
    private Datum reference = Units.dimensionless.createDatum(0);

    /**
     * Getter for property reference.
     * @return Value of property reference.
     */
    public Datum getReference() {
        return this.reference;
    }

    /**
     * Setter for property reference.
     * @param reference New value of property reference.
     */
    public void setReference(Datum reference) {
        Datum old = this.reference;
        if (!this.reference.equals(reference)) {
            this.reference = reference;
            refreshImage();
            propertyChangeSupport.firePropertyChange("reference", old, reference);
        }
    }
    /**
     * Holds value of property colorByDataSet.
     */
    private edu.uiowa.physics.pw.das.dataset.VectorDataSet colorByDataSet;

    /**
     * Getter for property colorByDataSet.
     * @return Value of property colorByDataSet.
     */
    public edu.uiowa.physics.pw.das.dataset.VectorDataSet getColorByDataSet() {
        return this.colorByDataSet;
    }

    /**
     * Setter for property colorByDataSet.
     * @param colorByDataSet New value of property colorByDataSet.
     */
    public void setColorByDataSet(edu.uiowa.physics.pw.das.dataset.VectorDataSet colorByDataSet) {
        this.colorByDataSet = colorByDataSet;
        refreshImage();
    }
    /**
     * Holds value of property resetDebugCounters.
     */
    private boolean resetDebugCounters;

    /**
     * Getter for property resetDebugCounters.
     * @return Value of property resetDebugCounters.
     */
    public boolean isResetDebugCounters() {
        return this.resetDebugCounters;
    }

    /**
     * Setter for property resetDebugCounters.
     * @param resetDebugCounters New value of property resetDebugCounters.
     */
    public void setResetDebugCounters(boolean resetDebugCounters) {
        if (resetDebugCounters) {
            renderCount = 0;
            updateImageCount = 0;
            refresh();
        }
    }
    /**
     * Holds value of property simplifyPaths.
     */
    private boolean simplifyPaths = true;

    /**
     * Getter for property simplifyPaths.
     * @return Value of property simplifyPaths.
     */
    public boolean isSimplifyPaths() {
        return this.simplifyPaths;
    }

    /**
     * Setter for property simplifyPaths.
     * @param simplifyPaths New value of property simplifyPaths.
     */
    public void setSimplifyPaths(boolean simplifyPaths) {
        this.simplifyPaths = simplifyPaths;
        refreshImage();
    }
    private boolean stampPsyms = true;
    public static final String PROP_STAMPPSYMS = "stampPsyms";

    public boolean isStampPsyms() {
        return this.stampPsyms;
    }

    public void setStampPsyms(boolean newstampPsyms) {
        boolean oldstampPsyms = stampPsyms;
        this.stampPsyms = newstampPsyms;
        propertyChangeSupport.firePropertyChange(PROP_STAMPPSYMS, oldstampPsyms, newstampPsyms);
        refreshImage();
    }

    public FillStyle getFillStyle() {
        return fillStyle;
    }

    public void setFillStyle(FillStyle fillStyle) {
        this.fillStyle = fillStyle;
        updatePsym();
        refreshImage();
    }

    public boolean acceptContext(int x, int y) {
        boolean accept = false;

        Point2D.Double dp = new Point2D.Double(x, y);

        if (fillToReference && fillToRefPath != null && fillToRefPath.contains(dp)) {
            accept = true;
        }

        if ((!accept) && path != null && path.intersects(dp.x - 5, dp.y - 5, 10, 10)) {
            accept = true;
        }
        
        if ((!accept) && psymsPath != null && psymsPath.intersects(dp.x - 5, dp.y - 5, 10, 10)) {
            accept= true;
        }
        
        return accept;
    }
    /**
     * property dataSetSizeLimit is the maximum number of points that will be rendered.
     * This is introduced because large datasets cause java2D plotting to fail.
     * When the size limit is reached, the data is clipped and a message is displayed.
     */
    private int dataSetSizeLimit = 200000;

    /**
     * Getter for property dataSetSizeLimit.
     * @return Value of property dataSetSizeLimit.
     */
    public int getDataSetSizeLimit() {
        return this.dataSetSizeLimit;
    }

    /**
     * Setter for property dataSetSizeLimit.
     * @param dataSetSizeLimit New value of property dataSetSizeLimit.
     */
    public void setDataSetSizeLimit(int dataSetSizeLimit) {
        int oldDataSetSizeLimit = this.dataSetSizeLimit;
        this.dataSetSizeLimit = dataSetSizeLimit;
        refreshImage();
        propertyChangeSupport.firePropertyChange("dataSetSizeLimit", new Integer(oldDataSetSizeLimit), new Integer(dataSetSizeLimit));
    }

    private double updatesPointsPerMillisecond;

    public static final String PROP_UPDATESPOINTSPERMILLISECOND = "updatesPointsPerMillisecond";

    public double getUpdatesPointsPerMillisecond() {
        return this.updatesPointsPerMillisecond;
    }

    public void setUpdatesPointsPerMillisecond(double newupdatesPointsPerMillisecond) {
        double oldupdatesPointsPerMillisecond = updatesPointsPerMillisecond;
        this.updatesPointsPerMillisecond = newupdatesPointsPerMillisecond;
        propertyChangeSupport.firePropertyChange(PROP_UPDATESPOINTSPERMILLISECOND, oldupdatesPointsPerMillisecond, newupdatesPointsPerMillisecond);
    }

    
    private double renderPointsPerMillisecond;

    public static final String PROP_RENDERPOINTSPERMILLISECOND = "renderPointsPerMillisecond";

    public double getRenderPointsPerMillisecond() {
        return this.renderPointsPerMillisecond;
    }

    public void setRenderPointsPerMillisecond(double newrenderPointsPerMillisecond) {
        double oldrenderPointsPerMillisecond = renderPointsPerMillisecond;
        this.renderPointsPerMillisecond = newrenderPointsPerMillisecond;
        propertyChangeSupport.firePropertyChange(PROP_RENDERPOINTSPERMILLISECOND, oldrenderPointsPerMillisecond, newrenderPointsPerMillisecond);
    }

}
