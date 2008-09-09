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

import org.das2.DasApplication;
import org.das2.DasProperties;
import org.das2.DasException;
import org.das2.util.monitor.ProgressMonitor;
import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.components.propertyeditor.*;
import edu.uiowa.physics.pw.das.dasml.FormBase;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.event.BoxZoomMouseModule;
import edu.uiowa.physics.pw.das.event.DasMouseInputAdapter;
import edu.uiowa.physics.pw.das.event.LengthDragRenderer;
import edu.uiowa.physics.pw.das.event.MouseModule;
import edu.uiowa.physics.pw.das.system.*;
import java.awt.image.*;
import javax.swing.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.*;
import java.awt.geom.*;
import java.util.logging.Logger;


/**
 *
 * @author  jbf
 */
public class SymbolLineRenderer extends Renderer implements Displayable {
    
    private Psym psym = Psym.NONE;
    private double symSize = 3.0; // radius in pixels
    private float lineWidth = 1.0f; // width in pixels
    private boolean histogram = false;
    //private Stroke stroke;
    private PsymConnector psymConnector = PsymConnector.SOLID;
    
    int renderCount=0, updateImageCount=0;
    
    /** Holds value of property color. */
    private Color color= Color.BLACK;
    
    private long lastUpdateMillis;
    
    /** Holds value of property antiAliased. */
    private boolean antiAliased= ("on".equals(DasProperties.getInstance().get("antiAlias")));
    
    /** The 'image' of the data */
    private GeneralPath path;
    
    private Logger log= DasLogger.getLogger(DasLogger.GRAPHICS_LOG);
    
    public SymbolLineRenderer() {
        super();
    }
    
    /**
     * @deprecated use SymbolLineRenderer() and setDataSet() instead.  Note that
     * behavior may be slightly different since a DataLoader is created.
     */
    public SymbolLineRenderer(DataSet ds) {
        super(ds);
    }
    
    /**
     * @deprecated use SymbolLineRenderer() and setDataSetDescriptor() instead.
     */
    public SymbolLineRenderer(DataSetDescriptor dsd) {
        super(dsd);
    }
    
    private void reportCount() {
        //if ( renderCount % 100 ==0 ) {
        //    System.err.println("  updates: "+updateImageCount+"   renders: "+renderCount );
        //}
    }
    
    
    public void render(Graphics g, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {
        renderCount++;
       // reportCount();
        
        long timer0= System.currentTimeMillis();
        
        VectorDataSet dataSet= (VectorDataSet)getDataSet();
        
        if ( this.ds==null && lastException!=null ) {
            renderException(g,xAxis,yAxis,lastException);
            return;
        }
        
        if (dataSet == null || dataSet.getXLength() == 0) {
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("null data set");
            parent.postMessage(this, "null data set", DasPlot.INFO, null, null );
            return;
        }
        
        DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("render data set "+dataSet);
        
        g.setColor(color);
        
        Graphics2D graphics= (Graphics2D) g.create();
        
        RenderingHints hints0= graphics.getRenderingHints();
        if ( antiAliased ) {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        } else {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        }
        
        log.finest("drawing psym in "+color);
        
        // draw the stored path that we calculated in updatePlotImage
        if (path != null) {
            psymConnector.draw(graphics, path, lineWidth);
        }
        
        Dimension d;
        
        double xmin, xmax, ymin, ymax;
        
        edu.uiowa.physics.pw.das.datum.Units xUnits= xAxis.getUnits();
        edu.uiowa.physics.pw.das.datum.Units yUnits= yAxis.getUnits();
        
        Rectangle r= g.getClipBounds();
        
        if ( r==null ) {
            xmax= xAxis.getDataMaximum().doubleValue(xUnits);
            xmin= xAxis.getDataMinimum().doubleValue(xUnits);
            ymax= yAxis.getDataMaximum().doubleValue(yUnits);
            ymin= yAxis.getDataMinimum().doubleValue(yUnits);
        } else {
            xmin= xAxis.invTransform((int)r.getX()).doubleValue(xUnits);
            xmax= xAxis.invTransform((int)(r.getX()+r.getWidth())).doubleValue(xUnits);
            ymin= yAxis.invTransform((int)r.getY()).doubleValue(yUnits);
            ymax= yAxis.invTransform((int)(r.getY()+r.getHeight())).doubleValue(yUnits);
        }
        
        //Support flipped axes
        if (xmax < xmin) {
            double tmp = xmax;
            xmax = xmin;
            xmin = tmp;
        }
        if (ymax < ymin) {
            double tmp = ymax;
            ymax = ymin;
            ymin = tmp;
        }
        
        if ( psym!=Psym.NONE ) { // optimize for common case
            int ixmax, ixmin;
            
            ixmin= VectorUtil.closestXTag(dataSet,xmin,xUnits);
            if ( ixmin>0 ) ixmin--;
            ixmax= VectorUtil.closestXTag(dataSet,xmax,xUnits);
            if ( ixmax<dataSet.getXLength()-1 ) ixmax++;
            
            for (int index = ixmin; index <= ixmax; index++) {
                if ( !dataSet.getDatum(index).isFill() ) {
                    double i = xAxis.transform(dataSet.getXTagDouble(index,xUnits),xUnits);
                    double j = yAxis.transform(dataSet.getDouble(index,yUnits),yUnits);
                    if ( Double.isNaN(j) ) {
                        //DasApplication.getDefaultApplication().getDebugLogger().warning("got NaN");
                    } else {
                        // note g is in the original space, not the AT space.
                        ((Graphics2D)g).setStroke(new BasicStroke(lineWidth));
                        psym.draw( g, i, j, (float)symSize );
                    }
                }
            }
        }
        
        long milli= System.currentTimeMillis();
        logger.finer( "render: "+ ( milli - timer0 ) + " total:" + ( milli - lastUpdateMillis )+ " fps:"+ (1000./( milli - lastUpdateMillis )) );
        lastUpdateMillis= milli;
        
    }
    
    boolean updating=false;
    
    public synchronized void updatePlotImage(DasAxis xAxis, DasAxis yAxis, ProgressMonitor monitor) {
        /*
         *This was an experiment to see if updates were being performed on multiple threads.
            if ( updating ) {
            System.err.println("hello, "+Thread.currentThread().getName() );
            return;
        }*/
        updating= true;
        
        //updateImageCount++;
        //reportCount();
        
        try {
            super.updatePlotImage( xAxis, yAxis, monitor );
        } catch ( DasException e ) {
            // it doesn't throw DasException, but interface requires exception, jbf 5/26/2005
            throw new RuntimeException(e);
        }
        
        DasLogger.getLogger( DasLogger.GRAPHICS_LOG ).fine( "entering updatePlotImage" );
        boolean histogram = this.histogram;
        VectorDataSet dataSet= (VectorDataSet)getDataSet();
        if (dataSet == null || dataSet.getXLength() == 0) {
            return;
        }
        Dimension d;
        
        double xmin, xmax, ymin, ymax;
        int ixmax, ixmin;
        
        Units xUnits= xAxis.getUnits();
        Units yUnits= yAxis.getUnits();
        
        DatumRange visibleRange= xAxis.getDatumRange();
        // if ( isOverloading() ) {
        //     visibleRange= visibleRange.rescale(-1,2);
        // }
        xmax= visibleRange.min().doubleValue(xUnits);
        xmin= visibleRange.max().doubleValue(xUnits);
        ymax= yAxis.getDataMaximum().doubleValue(yUnits);
        ymin= yAxis.getDataMinimum().doubleValue(yUnits);
        
        if ( psymConnector!=PsymConnector.NONE ) {
            ixmin= DataSetUtil.getPreviousColumn( dataSet, visibleRange.min() );
            ixmax= DataSetUtil.getNextColumn( dataSet, visibleRange.max() );
            
            GeneralPath newPath = new GeneralPath( GeneralPath.WIND_NON_ZERO, 110 * ( ixmax - ixmin ) / 100 );
            
            double xSampleWidth;
            if (dataSet.getProperty("xTagWidth") != null) {
                Datum xSampleWidthDatum = (Datum)dataSet.getProperty("xTagWidth");
                xSampleWidth = xSampleWidthDatum.doubleValue(xUnits.getOffsetUnits());
            } else {
                //Try to load the legacy sample-width property.
                String xSampleWidthString = (String)dataSet.getProperty("x_sample_width");
                if (xSampleWidthString != null) {
                    double xSampleWidthSeconds = Double.parseDouble(xSampleWidthString);
                    xSampleWidth = Units.seconds.convertDoubleTo(xUnits.getOffsetUnits(), xSampleWidthSeconds);
                } else {
                    xSampleWidth = 1e31;
                }
            }
            
            /* fuzz the xSampleWidth */
            xSampleWidth = xSampleWidth * 1.5;
            
            double x0 = Double.NaN;
            double y0 = Double.NaN;
            double i0 = Double.NaN;
            double j0 = Double.NaN;
            boolean skippedLast = true;
            
            for (int index = ixmin; index <= ixmax; index++) {
                double x = dataSet.getXTagDouble(index, xUnits);
                double y = dataSet.getDouble(index, yUnits);
                double i = xAxis.transform(x, xUnits);
                double j = yAxis.transform(y, yUnits);
                if ( yUnits.isFill( dataSet.getDouble(index,yUnits) ) || Double.isNaN(y) ) {
                    skippedLast = true;
                } else if (skippedLast) {
                    newPath.moveTo((float)i, (float)j);
                    skippedLast = false;
                } else if (Math.abs(x - x0) > xSampleWidth) {
                    //This should put a point on an isolated data value
                    newPath.lineTo((float)i0, (float)j0);
                    newPath.moveTo((float)i, (float)j);
                    skippedLast = false;
                } else {
                    if (histogram) {
                        double i1 = (i0 + i)/2;
                        newPath.lineTo((float)i1, (float)j0);
                        newPath.lineTo((float)i1, (float)j);
                        newPath.lineTo((float)i, (float)j);
                    } else {
                        newPath.lineTo((float)i, (float)j);
                    }
                    skippedLast = false;
                }
                x0= x;
                y0= y;
                i0= i;
                j0= j;
            }
            path = newPath;
            
        } else {
            path= null;
        }
        if (getParent() != null) {
            getParent().repaint();
        }
        DasLogger.getLogger( DasLogger.GRAPHICS_LOG ).fine( "done updatePlotImage" );
        updating=false;
    }
    
    
    public PsymConnector getPsymConnector() {
        return psymConnector;
    }
    
    public void setPsymConnector(PsymConnector p) {
        psymConnector = p;
        refreshImage();
    }
    
    /** Getter for property psym.
     * @return Value of property psym.
     */
    public Psym getPsym() {
        return this.psym;
    }
    
    
    /** Setter for property psym.
     * @param psym New value of property psym.
     */
    public void setPsym(Psym psym) {
        if (psym == null) throw new NullPointerException("psym cannot be null");
        Object oldValue = this.psym;
        this.psym = psym;
        refreshImage();
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
        this.symSize= symSize;
        setPsym(this.psym);
        refreshImage();
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
        this.color= color;
        refreshImage();
    }
    
    public float getLineWidth() {
        return lineWidth;
    }
    
    public void setLineWidth(float f) {
        lineWidth = f;
        refreshImage();
    }
    
    protected void installRenderer() {
        if ( ! DasApplication.getDefaultApplication().isHeadless() ) {
            DasMouseInputAdapter mouseAdapter = parent.mouseAdapter;
            DasPlot p= parent;
            mouseAdapter.addMouseModule( new MouseModule( p, new LengthDragRenderer( p,p.getXAxis(),p.getYAxis()), "Length" ) );
        }
    }
    
    protected void uninstallRenderer() {
    }
    
    public static SymbolLineRenderer processLinePlotElement(Element element, DasPlot parent, FormBase form) {
        String dataSetID = element.getAttribute("dataSetID");
        Psym psym = Psym.parsePsym(element.getAttribute("psym"));
        SymColor color = SymColor.parseSymColor(element.getAttribute("color"));
        SymbolLineRenderer renderer = new SymbolLineRenderer( (VectorDataSet)null );
        parent.addRenderer(renderer);
        float lineWidth = Float.parseFloat(element.getAttribute("lineWidth"));
        try {
            renderer.setDataSetID(dataSetID);
        } catch (org.das2.DasException de) {
            org.das2.util.DasExceptionHandler.handle(de);
        }
        renderer.setPsym(psym);
        renderer.setColor(color);
        renderer.setLineWidth(lineWidth);
        return renderer;
    }
    
    public Element getDOMElement(Document document) {
        
        Element element = document.createElement("lineplot");
        element.setAttribute("dataSetID", getDataSetID());
        element.setAttribute("psym", getPsym().toString());
        element.setAttribute("color", getColor().toString());
        
        return element;
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
            refreshImage();
        }
    }
    
    public String getListLabel() {
        return String.valueOf( this.getDataSetDescriptor() );
    }
    
    public javax.swing.Icon getListIcon() {
        Image i= new BufferedImage(15,10,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g= (Graphics2D)i.getGraphics();
        g.setRenderingHints(DasProperties.getRenderingHints());
        
        // leave transparent if not white
        if ( color.equals( Color.white ) ) {
            g.setColor( Color.GRAY );
        } else {
            g.setColor( new Color( 0,0,0,0 ) );
        }
        g.fillRect(0,0,15,10);
        g.setColor(color);
        Stroke stroke0= g.getStroke();
        getPsymConnector().drawLine( g, 2, 3, 13, 7, 1.5f );
        g.setStroke(stroke0);
        psym.draw( g, 7, 5, 3.f );
        return new ImageIcon(i);
    }

    public boolean acceptContext(int x, int y) {
        return path!=null && path.intersects( x-5, y-5, 10, 10 );
    }
}
