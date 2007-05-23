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
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
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
    
    private Psym psym = Psym.NONE;
    private double symSize = 3.0; // radius in pixels
    private float lineWidth = 1.0f; // width in pixels
    private boolean histogram = false;
    //private Stroke stroke;
    private PsymConnector psymConnector = PsymConnector.SOLID;
    
    private int renderCount=0, updateImageCount=0;
    
    /** Holds value of property color. */
    private Color color= Color.BLACK;
    
    private long lastUpdateMillis;
    
    /** Holds value of property antiAliased. */
    private boolean antiAliased= ("on".equals(DasProperties.getInstance().get("antiAlias")));
    
    /** The 'image' of the data */
    private GeneralPath path;
    private GeneralPath fillToRefPath;
    
    private Logger log= DasLogger.getLogger(DasLogger.GRAPHICS_LOG);
    
    public SeriesRenderer() {
        super();
    }
    
    
    private void reportCount() {
        //if ( renderCount % 100 ==0 ) {
        //System.err.println("  updates: "+updateImageCount+"   renders: "+renderCount );
        //new Throwable("").printStackTrace();
        //}
    }
    
    public void render(Graphics g, DasAxis xAxis, DasAxis yAxis) {
        
        renderCount++;
        reportCount();
        
        long timer0= System.currentTimeMillis();
        
        VectorDataSet dataSet= (VectorDataSet)getDataSet();
        
        if ( this.renderException!=null ) {
            renderException( g, xAxis, yAxis, renderException );
            return;
        }
        
        if ( this.ds==null && lastException!=null ) {
            renderException(g,xAxis,yAxis,lastException);
            return;
        }
        
        if (dataSet == null || dataSet.getXLength() == 0) {
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("null data set");
            renderException( g, xAxis, yAxis, new Exception("null data set") );
            return;
        }
        
        DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("render data set "+dataSet);
        
        Graphics2D graphics= (Graphics2D) g.create();
        
        RenderingHints hints0= graphics.getRenderingHints();
        if ( antiAliased ) {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        } else {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        }
        
        edu.uiowa.physics.pw.das.datum.Units xUnits= xAxis.getUnits();
        edu.uiowa.physics.pw.das.datum.Units yUnits= yAxis.getUnits();
        
        double dstMin, dstMax, srcMin, srcMax;
        
        srcMin= xAxis.getDataMinimum( xAxis.getUnits() );
        srcMax= xAxis.getDataMaximum( xAxis.getUnits() );
        dstMax= xAxis.transform(  srcMax, xUnits );
        dstMin= xAxis.transform(  srcMin, xUnits );
        AffineTransform at= getAffineTransform( dstMin, dstMax, srcMin, srcMax, 0, null );
        
        srcMin= yAxis.getDataMinimum( yAxis.getUnits() );
        srcMax= yAxis.getDataMaximum( yAxis.getUnits() );
        dstMax= yAxis.transform(  srcMax, yUnits );
        dstMin= yAxis.transform(  srcMin, yUnits );
        
        at= getAffineTransform( dstMin, dstMax, srcMin, srcMax, 1, at );
        
        System.err.println( "X: " +
                at.getScaleX() + " " +at.getTranslateX() +" \nY:" +
                at.getScaleY() + " " + at.getTranslateY() );
        
        if ( this.fillToReference && fillToRefPath!=null ) {
            GeneralPath pixelFillPath= new GeneralPath();
            pixelFillPath.append( fillToRefPath, false );
            pixelFillPath.transform( at );
            graphics.setColor( fillColor );
            graphics.fill( pixelFillPath ) ;
        }
        
        graphics.setColor(color);
        log.finest("drawing psymConnector in "+color);
        
        
        GeneralPath pixelPath;
        // draw the stored path that we calculated in updatePlotImage
        
        pixelPath= new GeneralPath();
        pixelPath.append( path, false );
        pixelPath.transform(at);
        
        psymConnector.draw(graphics, pixelPath, lineWidth);
        
        double xmin, xmax, ymin, ymax;
        
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
        
        DatumRange visibleRange= new DatumRange( xmin, xmax, xUnits );
        
        g.setColor( color );
        
        if ( psym!=Psym.NONE ) { // optimize for common case
            
            if ( colorByDataSetId!=null ) {
                colorByDataSet= (VectorDataSet) dataSet.getPlanarView( colorByDataSetId );
            }
            
            PathIterator it= pixelPath.getPathIterator( null );
            
            double[] coords= new double[6];
            
            ((Graphics2D)g).setStroke(new BasicStroke(lineWidth));
            
            while ( ! it.isDone() ) {
                it.currentSegment( coords );
                psym.draw( g, coords[0], coords[1], (float)symSize );
                it.next();
            }
        }
        
        g.drawString( "renderCount="+renderCount+" updateCount="+updateImageCount,xAxis.getColumn().getDMinimum()+5, yAxis.getRow().getDMinimum()+20 );
        long milli= System.currentTimeMillis();
        logger.finer( "render: "+ ( milli - timer0 ) + " total:" + ( milli - lastUpdateMillis )+ " fps:"+ (1000./( milli - lastUpdateMillis )) );
        lastUpdateMillis= milli;
        
    }
    
    boolean updating=false;
    
    /**
     * return the AffineTransform that goes from dmin0, dmax0 to dmin1, dmax1.
     * at= getAffineTransform( 0, 100, 32, 212, 0, null );
     * at.getScaleX() --> 9/5
     * at.getTranslateX() --> 32
     *@param axis 0=xaxis, 1=yaxis.
     *@param at if non-null, then append the AffineTransform onto at.
     */
    public static AffineTransform getAffineTransform( double dstMin, double dstMax,  double srcMin, double srcMax, int axis,  AffineTransform at ) {
        
        if ( at==null ) at= new AffineTransform();
        
        double scalex= ( dstMin - dstMax ) / ( srcMin - srcMax );
        double transx= -1* srcMin * scalex + dstMin;
        if ( axis==0 ) {
            at.translate( transx, 0 );
            at.scale( scalex, 1. );
        } else {
            at.translate( 0, transx );
            at.scale( 1., scalex );
        }
        
        return at;
    }
    
    /*public static void main( String[] args ) {
        AffineTransform at= getAffineTransform( 32, 212, 0, 100, 0, null );
        System.err.println( at.getScaleX()+ "==9./5" );
         System.err.println( at.getTranslateX() + "==32 ");
         System.err.println( at.transform( new Point2D.Double( 100, 0 ), new Point2D.Double(0,0) ) );
    }*/
    
    /**
     * do the same as updatePlotImage, but use AffineTransform to implement axis transform.
     */
    public void updatePlotImage( DasAxis xAxis, DasAxis yAxis, DasProgressMonitor monitor) {
        updating= true;
        
        updateImageCount++;
        reportCount();
        
        try {
            super.updatePlotImage( xAxis, yAxis, monitor );
        } catch ( DasException e ) {
            // it doesn't throw DasException, but interface requires exception, jbf 5/26/2005
            throw new RuntimeException(e);
        }
        
        logger.fine( "entering updatePlotImage" );
        long t0= System.currentTimeMillis();
        
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
        
        xmax= visibleRange.min().doubleValue(xUnits);
        xmin= visibleRange.max().doubleValue(xUnits);
        ymax= yAxis.getDataMaximum().doubleValue(yUnits);
        ymin= yAxis.getDataMinimum().doubleValue(yUnits);
        
        ixmin= DataSetUtil.getPreviousColumn( dataSet, visibleRange.min() );
        ixmax= DataSetUtil.getNextColumn( dataSet, visibleRange.max() );
        
        GeneralPath newPath = new GeneralPath( GeneralPath.WIND_NON_ZERO, 110 * ( ixmax - ixmin ) / 100 );
        GeneralPath fillPath=  new GeneralPath( GeneralPath.WIND_NON_ZERO, 110 * ( ixmax - ixmin ) / 100 );
        
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
        
        float x0 = Float.NaN;
        float y0 = Float.NaN;
        boolean skippedLast = true;
        
        if ( reference!=null && reference.getUnits()!=yAxis.getUnits() ) {
            reference=null;
        }
        
        if ( reference==null ) {
            reference= yUnits.createDatum( yAxis.isLog() ? 1.0 : 0.0 );
        }
        
        float ref= (float) reference.doubleValue( yUnits );
        
        float x=Float.NaN, y=Float.NaN;
        for (int index = ixmin; index <= ixmax; index++) {
            x = (float)dataSet.getXTagDouble(index, xUnits);
            y = (float)dataSet.getDouble(index, yUnits);
            
            if ( yUnits.isFill( y ) || Double.isNaN(y) ) {
                skippedLast = true;
                if ( Float.isNaN(x0)) {
                    fillPath.moveTo( x,ref );
                } else {
                    fillPath.lineTo( x0, ref );
                }
            } else if (skippedLast) {
                newPath.moveTo(x, y);
                fillPath.moveTo( x, ref);
                fillPath.lineTo( x, y);
                skippedLast = false;
            } else if (Math.abs(x - x0) > xSampleWidth) { // put a point on an isolated data value  // TODO: this should be the same as one point adjacent to two fill's--it's not.
                newPath.lineTo(x0, y0);  // TODO: histogram mode should draw the two wings.
                newPath.moveTo(x, y);
                fillPath.lineTo(x0, y0);
                fillPath.lineTo( x0, ref ); // return to the baseline
                fillPath.moveTo(x, ref);
                fillPath.lineTo( x, y );
                skippedLast = false;
            } else {
                if (histogram) {
                    float x1 = (x0 + x)/2;
                    newPath.lineTo(x1, y0);
                    newPath.lineTo(x1, y);
                    newPath.lineTo(x, y);
                    fillPath.lineTo(x1, y0);
                    fillPath.lineTo(x1, y);
                    fillPath.lineTo(x, y);
                } else {
                    newPath.lineTo(x, y);
                    fillPath.lineTo(x, y);
                }
                skippedLast = false;
            }
            x0= x;
            y0= y;
        } // for (int index = ixmin; index <= ixmax; index++)
        fillPath.lineTo( x, ref );
        path = newPath;
        this.fillToRefPath= fillPath;
        
        if (getParent() != null) {
            getParent().repaint();
        }
        logger.fine( "done updatePlotImage in "+(System.currentTimeMillis()-t0)+" ms" );
        updating=false;
        
    }
    
    public void updatePlotImageOld(DasAxis xAxis, DasAxis yAxis, DasProgressMonitor monitor) {
        
        updating= true;
        
        updateImageCount++;
        reportCount();
        
        try {
            super.updatePlotImage( xAxis, yAxis, monitor );
        } catch ( DasException e ) {
            // it doesn't throw DasException, but interface requires exception, jbf 5/26/2005
            throw new RuntimeException(e);
        }
        
        logger.fine( "entering updatePlotImage" );
        long t0= System.currentTimeMillis();
        
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
        
        xmax= visibleRange.min().doubleValue(xUnits);
        xmin= visibleRange.max().doubleValue(xUnits);
        ymax= yAxis.getDataMaximum().doubleValue(yUnits);
        ymin= yAxis.getDataMinimum().doubleValue(yUnits);
        
        if ( psymConnector!=PsymConnector.NONE ) {
            ixmin= DataSetUtil.getPreviousColumn( dataSet, visibleRange.min() );
            ixmax= DataSetUtil.getNextColumn( dataSet, visibleRange.max() );
            
            GeneralPath newPath = new GeneralPath( GeneralPath.WIND_NON_ZERO, 110 * ( ixmax - ixmin ) / 100 );
            GeneralPath fillPath=  new GeneralPath( GeneralPath.WIND_NON_ZERO, 110 * ( ixmax - ixmin ) / 100 );
            
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
            float i0 = Float.NaN;
            float j0 = Float.NaN;
            boolean skippedLast = true;
            
            if ( reference!=null && reference.getUnits()!=yAxis.getUnits() ) {
                reference=null;
            }
            
            if ( reference==null ) {
                reference= yUnits.createDatum( yAxis.isLog() ? 1.0 : 0.0 );
            }
            
            float jref;
            if ( yAxis.isLog() && reference.doubleValue( yUnits ) <= 0.0 ) {
                jref= yAxis.getRow().getDMaximum();
            } else {
                jref= (float) yAxis.transform( reference ); // reference line
            }
            
            float i=Float.NaN;
            float j;
            for (int index = ixmin; index <= ixmax; index++) {
                double x = dataSet.getXTagDouble(index, xUnits);
                double y = dataSet.getDouble(index, yUnits);
                
                i = (float) xAxis.transform(x, xUnits);
                j = (float) yAxis.transform(y, yUnits);
                
                if ( yUnits.isFill( y ) || Double.isNaN(y) ) {
                    skippedLast = true;
                    if ( Float.isNaN(i0)) {
                        fillPath.moveTo( i,jref );
                    } else {
                        fillPath.lineTo( i0, jref );
                    }
                } else if (skippedLast) {
                    newPath.moveTo(i, j);
                    fillPath.moveTo( i, jref);
                    fillPath.lineTo( i, j);
                    skippedLast = false;
                } else if (Math.abs(x - x0) > xSampleWidth) { // put a point on an isolated data value  // TODO: this should be the same as one point adjacent to two fill's--it's not.
                    newPath.lineTo(i0, j0);  // TODO: histogram mode should draw the two wings.
                    newPath.moveTo(i, j);
                    fillPath.lineTo(i0, j0);
                    fillPath.lineTo( i0, jref ); // return to the baseline
                    fillPath.moveTo(i, jref);
                    fillPath.lineTo(i, j);
                    skippedLast = false;
                } else {
                    if (histogram) {
                        float i1 = (i0 + i)/2;
                        newPath.lineTo(i1, j0);
                        newPath.lineTo(i1, j);
                        newPath.lineTo(i, j);
                        fillPath.lineTo(i1, j0);
                        fillPath.lineTo(i1, j);
                        fillPath.lineTo(i, j);
                    } else {
                        newPath.lineTo(i, j);
                        fillPath.lineTo(i, j);
                    }
                    skippedLast = false;
                }
                x0= x;
                y0= y;
                i0= i;
                j0= j;
            } // for (int index = ixmin; index <= ixmax; index++)
            fillPath.lineTo( i, jref );
            path = newPath;
            this.fillToRefPath= fillPath;
            
        } else {
            path= null;
            fillToRefPath= null;
        }
        if (getParent() != null) {
            getParent().repaint();
        }
        logger.fine( "done updatePlotImage in "+(System.currentTimeMillis()-t0)+" ms" );
        updating=false;
    }
    
    
    public PsymConnector getPsymConnector() {
        return psymConnector;
    }
    
    public void setPsymConnector(PsymConnector p) {
        if ( !p.equals(psymConnector) ) {
            psymConnector = p;
            refreshImage();
        }
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
        if ( psym!=this.psym ) {
            Object oldValue = this.psym;
            this.psym = psym;
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
        if ( this.symSize!=symSize ) {
            this.symSize= symSize;
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
        if ( !this.color.equals(color) ) {
            this.color= color;
            refreshImage();
        }
    }
    
    public float getLineWidth() {
        return lineWidth;
    }
    
    public void setLineWidth(float f) {
        if ( this.lineWidth != f ) {
            lineWidth = f;
            refreshImage();
        }
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
    private Color fillColor=Color.lightGray;
    
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
        if ( !this.fillColor.equals(color) ) {
            this.fillColor= color;
            refresh();
        }
    }
    
    /**
     * Holds value of property colorByDataSetId.
     */
    private String colorByDataSetId=null;
    
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
        colorBar.addPropertyChangeListener( new PropertyChangeListener( ) {
            public void propertyChange(PropertyChangeEvent evt) {
                if ( colorByDataSetId!=null ) {
                    refresh();
                }
            }
        } );
        
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
        if ( this.fillToReference != fillToReference ) {
            this.fillToReference = fillToReference;
            refresh();
        }
    }
    
    /**
     * Holds value of property reference.
     */
    private Datum reference= null;
    
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
        if ( !this.reference.equals( reference ) ) {
            this.reference= reference;
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
        if ( resetDebugCounters ) {
            renderCount= 0;
            updateImageCount= 0;
            refresh();
        }
    }
    
    
}
