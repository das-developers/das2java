/* File: SymbolLineRenderer.java
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
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 *
 * @author  jbf
 */
public class SeriesRenderer extends Renderer implements Displayable {
    
    private DefaultPlotSymbol psym = DefaultPlotSymbol.STAR;
    private double symSize = 3.0; // radius in pixels
    private float lineWidth = 1.0f; // width in pixels
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
    
    /** The 'image' of the data */
    private GeneralPath path;
    private GeneralPath fillToRefPath;
    
    /* the index of the first point drawn, nonzero when X is monotonic and we can clip. */
    private int firstIndex;
    
    /* the non-inclusive index of the last point drawn. */
    private int lastIndex;
    
    private Logger log = DasLogger.getLogger(DasLogger.GRAPHICS_LOG);
    
    public SeriesRenderer() {
        super();
    }
    
    
    private void reportCount() {
        //if ( renderCount % 100 ==0 ) {
        //System.err.println("  updates: "+updateImageCount+"   renders: "+renderCount );
        //new Throwable("").printStackTrace();
        //}
    }
    
    private AffineTransform getMyAffineTransform( DasAxis xAxis, double xBase, DasAxis yAxis ) {
        edu.uiowa.physics.pw.das.datum.Units xUnits = xAxis.getUnits();
        edu.uiowa.physics.pw.das.datum.Units yUnits = yAxis.getUnits();
        final boolean xlog = xAxis.isLog();
        final boolean ylog = yAxis.isLog();
        
        double dstMin;
        double dstMax;
        double srcMin;
        double srcMax;
        
        srcMin = xAxis.getDataMinimum(xAxis.getUnits());
        srcMax = xAxis.getDataMaximum(xAxis.getUnits());
        dstMax = xAxis.transform(srcMax, xUnits);
        dstMin = xAxis.transform(srcMin, xUnits);
        if (xlog) {
            srcMin = Math.log(srcMin);
            srcMax = Math.log(srcMax);
        }
        // TODO: floats in TimeLocationUnits will need an offset applied to avoid roundoff errors, which will be picked up in the AT.
        AffineTransform at = getAffineTransform(dstMin, dstMax, srcMin, srcMax, 0, null);
        at.translate( xBase * at.getScaleX(), 0 );
        srcMin = yAxis.getDataMinimum(yAxis.getUnits());
        srcMax = yAxis.getDataMaximum(yAxis.getUnits());
        dstMax = yAxis.transform(srcMax, yUnits);
        dstMin = yAxis.transform(srcMin, yUnits);
        if (ylog) {
            srcMin = Math.log(srcMin);
            srcMax = Math.log(srcMax);
        }
        at = getAffineTransform(dstMin, dstMax, srcMin, srcMax, 1, at);
        return at;
    }
    
    public void render(Graphics g, DasAxis xAxis, DasAxis yAxis, DasProgressMonitor mon) {
        
        renderCount++;
        reportCount();
        
        long timer0 = System.currentTimeMillis();
        
        VectorDataSet dataSet = (VectorDataSet) getDataSet();
        
        //if ( this.renderException!=null ) {
        //    renderException( g, xAxis, yAxis, renderException );
        //    return;
        //}
        if (this.ds == null && lastException != null) {
            renderException(g, xAxis, yAxis, lastException);
            return;
        }
        
        if (dataSet == null || dataSet.getXLength() == 0) {
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("null data set");
            renderException(g, xAxis, yAxis, new Exception("null data set"));
            return;
        }
        
        // sometimes this happens, TODO: investigate
        if (path == null) {
            return;
        }
        
        
        DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("render data set " + dataSet);
        
        Graphics2D graphics = (Graphics2D) g.create();
        
        RenderingHints hints0 = graphics.getRenderingHints();
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
        
        psymConnector.draw(graphics, path, lineWidth);
        
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
            xmax = xAxis.invTransform((int) (r.getX()+r.getWidth())).doubleValue(xUnits);
            ymin = yAxis.invTransform((int) r.getY()).doubleValue(yUnits);
            ymax = yAxis.invTransform((int) (r.getY()+r.getHeight())).doubleValue(yUnits);
        }
        
        graphics.setColor(color);
        
        Units zunits = null;
        
        if (psym != DefaultPlotSymbol.NONE) {
            // optimize for common case
            float fsymSize = (float) symSize;
            mon.setTaskSize(lastIndex - firstIndex);
            mon.started();
            
            if (colorByDataSetId != null) {
                colorByDataSet = (VectorDataSet) dataSet.getPlanarView( colorByDataSetId );
                zunits = colorBar.getUnits();
            }
            
            PathIterator it = path.getPathIterator(null);
            
            double[] coords = new double[6];
            
            double h0x = 0;
            double h1x = 0;
            double h2x = 0; // for detecting actual point in histogram mode
            double h0y = 0;
            double h1y = 0;
            double h2y = 0; // for detecting actual point in histogram mode
            int type0 = 99;
            int type1 = 99;
            int type2 = 99;
            
            double lastX = Double.NaN;
            
            graphics.setStroke(new BasicStroke(lineWidth));
            
            int i = firstIndex;
            
            if (colorByDataSet != null) {
                while (!it.isDone()) {
                    int type = it.currentSegment(coords);
                    if (i == colorByDataSet.getXLength()) {
                        //System.err.println("here");
                    }
                    graphics.setColor(new Color(colorBar.rgbTransform(colorByDataSet.getDouble(i, zunits), zunits)));
                    if (type == PathIterator.SEG_LINETO) {
                        psym.draw( graphics, coords[0], coords[1], fsymSize, fillStyle);
                        i++;
                    }
                    it.next();
                    if (mon.isCancelled()) {
                        break;
                    }
                    mon.setTaskProgress(Math.min(i - firstIndex, lastIndex - firstIndex));
                }
            } else {
                while (!it.isDone()) {
                    int type = it.currentSegment(coords);
                    boolean drawIt;
                    if (histogram) {
                        h0x = h1x;
                        h1x = h2x;
                        h2x = coords[0];
                        h0y = h1y;
                        h1y = h2y;
                        h2y = coords[1];
                        type0 = type1;
                        type1 = type2;
                        type2 = type;
                        //System.err.print("type: " + type + "  " + String.format("[%f %f]", coords[0], coords[1]));
                        if (h0x != h1x && h1x != h2x && type1 == PathIterator.SEG_LINETO && type2 == PathIterator.SEG_LINETO) {
                            drawIt = true;
                            coords[0] = h1x;
                            coords[1] = h1y;
                            System.err.println("  ***");
                        } else {
                            drawIt = false;
                            System.err.println(" ");
                        }
                    } else {
                        drawIt = type == PathIterator.SEG_LINETO;
                    }
                    
                    if (drawIt) {
                        psym.draw( graphics, coords[0], coords[1], fsymSize, fillStyle);
                    }
                    it.next();
                    i++;
                    if (mon.isCancelled()) {
                        break;
                    }
                    mon.setTaskProgress(Math.min(i - firstIndex, lastIndex - firstIndex));
                }
            }
            
            double simplifyFactor = (double) (  i - firstIndex ) / (lastIndex - firstIndex);
            System.err.println("simplify factor=" + simplifyFactor);
            
            mon.finished();
        }
        
        //g.drawString( "renderCount="+renderCount+" updateCount="+updateImageCount,xAxis.getColumn().getDMinimum()+5, yAxis.getRow().getDMinimum()+20 );
        long milli = System.currentTimeMillis();
        logger.finer("render: " + (milli - timer0) + " total:" + (milli - lastUpdateMillis) + " fps:" + (1000. / (milli - lastUpdateMillis)));
        lastUpdateMillis = milli;
    }
    
    boolean updating = false;
    
    /**
     * return the AffineTransform that goes from [dmin0, dmax0] to [dmin1, dmax1], where both spaces are linear.
     * at= getAffineTransform( 0, 100, 32, 212, 0, null );
     * at.getScaleX() --> 9/5
     * at.getTranslateX() --> 32
     *@param axis 0=xaxis, 1=yaxis.
     *@param at if non-null, then append the AffineTransform onto at.
     */
    private static AffineTransform getAffineTransform(double dstMin, double dstMax, double srcMin, double srcMax, int axis, AffineTransform at) {
        
        if (at == null) {
            at = new AffineTransform();
        }
        double scalex = (dstMin - dstMax) / (srcMin - srcMax);
        double transx = -1 * srcMin * scalex + dstMin;
        if (axis == 0) {
            at.translate(transx, 0);
            at.scale(scalex, 1.);
        } else {
            at.translate(0, transx);
            at.scale(1., scalex);
        }
        
        return at;
    }
    
    
    /**
     * do the same as updatePlotImage, but use AffineTransform to implement axis transform.
     */
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
        
        Boolean xMono = (Boolean) dataSet.getProperty( DataSet.PROPERTY_X_MONOTONIC );
        if (xMono != null && xMono.booleanValue()) {
            ixmin = DataSetUtil.getPreviousColumn(dataSet, visibleRange.min());
            ixmax = DataSetUtil.getNextColumn(dataSet, visibleRange.max()) + 1;
        } else {
            ixmin = 0;
            ixmax = dataSet.getXLength();
        }
        
        GeneralPath newPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, 110 * (ixmax - ixmin) / 100);
        GeneralPath fillPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, 110 * (ixmax - ixmin) / 100);
        firstIndex = ixmin;
        lastIndex = ixmax;
        
        Datum sw = DataSetUtil.guessXTagWidth(dataSet);
        double xSampleWidth = sw.doubleValue(xUnits.getOffsetUnits());
        
        /* fuzz the xSampleWidth */
        xSampleWidth = xSampleWidth * 1.10;
        
        boolean skippedLast = true; // true if the last point was skipped
        boolean penUp = true; // pen is up because data gap needs to be rendered.
        if (reference != null && reference.getUnits() != yAxis.getUnits()) {
            reference = null;
        }
        
        if (reference == null) {
            reference = yUnits.createDatum(yAxis.isLog() ? 1.0 : 0.0);
        }
        
        double yref = (double) reference.doubleValue( yUnits );
        
        double x =Double.NaN;
        double y = Double.NaN;
        
        double x0 = Double.NaN; /* the last plottable point */
        double y0 = Double.NaN; /* the last plottable point */
        
        float fyref= (float) yAxis.transform( yref, yUnits );
        float fx= Float.NaN;
        float fy= Float.NaN;
        float fx0= Float.NaN;
        float fy0= Float.NaN;
        
        int index;
        
        // find the first valid point, set x0, y0 //
        for (index = ixmin; index < ixmax; index++) {
            x = (double) dataSet.getXTagDouble(index, xUnits);
            y = (double) dataSet.getDouble(index, yUnits);
            
            final boolean isValid = yUnits.isValid(y) && xUnits.isValid(x);
            if (isValid) {
                x0 = x;
                y0 = y;
                index++;
                break;
            }
        }
        
        // first point //
        fx= (float)xAxis.transform( x, xUnits );
        fy= (float)yAxis.transform( y, yUnits );
        
        if (histogram) {
            float fx1 = ( float ) xAxis.transform( x - xSampleWidth / 2, xUnits );
            newPath.moveTo(fx1,fy);
            newPath.lineTo(fx, fy);
            fillPath.moveTo(fx1, fyref);
            fillPath.lineTo(fx1, fy);
            fillPath.lineTo(fx, fy);
        } else {
            newPath.moveTo(fx,fy);
            newPath.lineTo(fx, fy);
            fillPath.moveTo(fx, fyref);
            fillPath.lineTo(fx, fy);
        }
        fx0= fx;
        fy0= fy;
        
        // now loop through all of them. //
        for (; index < ixmax; index++) {
            
            x = dataSet.getXTagDouble(index, xUnits) ;
            y = dataSet.getDouble(index, yUnits);
            
            final boolean isValid = yUnits.isValid(y) && xUnits.isValid(x);
            
            fx= (float) xAxis.transform( x, xUnits );
            fy= (float) yAxis.transform( y, yUnits );
            
            //double tx= xAxis.transformFast( x, xUnits );
            
            //System.err.println( ""+(float)tx+ "   " + fx );
            
            if (isValid) {
                
                if ((x - x0) < xSampleWidth) {
                    // draw connect-a-dot between last valid and here
                    if (histogram) {
                        float fx1 = (fx0 +fx) / 2;
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
                        float fx1 = (float)  xAxis.transform( x0 + xSampleWidth / 2, xUnits );
                        newPath.lineTo(fx1, fy0);
                        fillPath.lineTo(fx1, fy0);
                        fillPath.lineTo(fx1, fyref);
                        
                        fx1 = (float)  xAxis.transform( x - xSampleWidth / 2, xUnits );
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
                fx0= fx;
                fy0= fy;
                
            }
        } // for (int index = ixmin; index <= ixmax; index++)
        fillPath.lineTo(fx, fyref);
        
        path = newPath;
        this.fillToRefPath = fillPath;
                
        if (this.fillToReference && fillToRefPath != null) {
            GeneralPath pixelFillPath;
            if (simplifyPaths) {
                pixelFillPath = GraphUtil.reducePath(fillToRefPath.getPathIterator(null), new GeneralPath(GeneralPath.WIND_NON_ZERO, lastIndex - firstIndex));
            } else {
                pixelFillPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, 200 * (lastIndex - firstIndex) / 100);
                pixelFillPath.append(fillToRefPath, false);
            }
            
            fillToRefPath= pixelFillPath;
        }
        
        GeneralPath pixelPath;
        // draw the stored path that we calculated in updatePlotImage
        if (simplifyPaths && colorByDataSet == null) {
            pixelPath = GraphUtil.reducePath(path.getPathIterator(null), new GeneralPath(GeneralPath.WIND_NON_ZERO, lastIndex - firstIndex));
        } else {
            pixelPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, 110 * (lastIndex - firstIndex) / 100); // DANGER--should be exactly the same length as path to avoid copies.
            pixelPath.append(path, false);
        }
        
        path= pixelPath;
        
        if (getParent() != null) {
            getParent().repaint();
        }
        logger.fine("done updatePlotImage in " + (System.currentTimeMillis() - t0) + " ms");
        updating = false;
    }
    
    
    public PsymConnector getPsymConnector() {
        return psymConnector;
    }
    
    public void setPsymConnector(PsymConnector p) {
        if (!p.equals(psymConnector)) {
            psymConnector = p;
            refreshImage();
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
            refreshImage();
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
        if (this.symSize != symSize) {
            this.symSize = symSize;
            setPsym(this.psym);
            refreshImage();
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
        if (!this.color.equals(color)) {
            this.color = color;
            refreshImage();
        }
    }
    
    public float getLineWidth() {
        return lineWidth;
    }
    
    public void setLineWidth(float f) {
        if (this.lineWidth != f) {
            lineWidth = f;
            refreshImage();
        }
    }
    
    protected void installRenderer() {
        if (!DasApplication.getDefaultApplication().isHeadless()) {
            DasMouseInputAdapter mouseAdapter = parent.mouseAdapter;
            DasPlot p = parent;
            mouseAdapter.addMouseModule(new MouseModule(p, new LengthDragRenderer(p, p.getXAxis(), p.getYAxis()), "Length"));
        }
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
        this.antiAliased = antiAliased;
        refreshImage();
    }
    
    public boolean isHistogram() {
        return histogram;
    }
    
    public void setHistogram(final boolean b) {
        if (b != histogram) {
            histogram = b;
            refresh();
        }
    }
    
    public String getListLabel() {
        return String.valueOf(this.getDataSetDescriptor());
    }
    
    public javax.swing.Icon getListIcon() {
        Image i = new BufferedImage(15, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) i.getGraphics();
        g.setRenderingHints(DasProperties.getRenderingHints());
        g.setBackground( parent.getBackground( ) );
        
        // leave transparent if not white
        if (color.equals(Color.white)) {
            g.setColor(Color.GRAY);
        } else {
            g.setColor(new Color(0, 0, 0, 0));
        }
        g.fillRect(0, 0, 15, 10);
        
        if ( fillToReference ) {
            g.setColor( fillColor );
            Polygon p= new Polygon( new int[] { 2, 13, 13, 2 }, new int[] { 3, 7, 10, 10 }, 4 );
            g.fillPolygon(p);
        }
        
        g.setColor(color);
        Stroke stroke0 = g.getStroke();
        getPsymConnector().drawLine(g, 2, 3, 13, 7, 1.5f);
        g.setStroke(stroke0);
        psym.draw(g, 7, 5, 3.f, fillStyle );
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
        if (!this.fillColor.equals(color)) {
            this.fillColor = color;
            refresh();
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
        this.colorByDataSetId = colorByDataSetId;
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
        if (this.fillToReference != fillToReference) {
            this.fillToReference = fillToReference;
            refresh();
        }
    }
    
    /**
     * Holds value of property reference.
     */
    private Datum reference = null;
    
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
        if (!this.reference.equals(reference)) {
            this.reference = reference;
            refresh();
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
        refresh();
    }
    
    public FillStyle getFillStyle() {
        return fillStyle;
    }
    
    
    public void setFillStyle(FillStyle fillStyle) {
        this.fillStyle = fillStyle;
        refresh();
    }
    
    public boolean acceptContext(int x, int y) {
        boolean accept= false;
        //AffineTransform at= getMyAffineTransform( parent.getXAxis(), parent.getYAxis() );
        //Point2D.Double p= new Point2D.Double( x, y );
        // Point2D.Double dp= new Point2D.Double();
        // try {
        //     at.inverseTransform( p, dp );
        // } catch ( NoninvertibleTransformException e ) {
        //     throw new RuntimeException(e);
        // }
        
        Point2D.Double dp= new Point2D.Double( x, y );
        
        if ( fillToReference && fillToRefPath!=null && fillToRefPath.contains( dp ) ) {
            accept= true;
        }
        //double sx= Math.abs( at.getScaleX() );
        // double sy= Math.abs( at.getScaleY() );
        //if ( (!accept) && path.intersects( dp.x-5/sx, dp.y-5/sy, 10/sx, 10/sy ) ) {
        //    accept= true;
        //}
        if ( (!accept) && path!=null && path.intersects( dp.x-5, dp.y-5, 10, 10 ) ) {
            accept= true;
        }
        return accept;
    }
}
