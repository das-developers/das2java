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

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.dasml.FormBase;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.system.*;
import edu.uiowa.physics.pw.das.util.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.*;
import java.awt.geom.*;
import java.awt.geom.Line2D;

/**
 *
 * @author  jbf
 */
public class SymbolLineRenderer extends Renderer {
    
    private Psym psym = Psym.NONE;
    private double symSize = 1.0; // radius in pixels
    private float lineWidth = 1.5f; // width in pixels
    private boolean histogram = false;
    //private Stroke stroke;
    private PsymConnector psymConnector = PsymConnector.SOLID;
    
    /** Holds value of property color. */
    private SymColor color= SymColor.black;
    
    private long lastUpdateMillis;
    
    /** Holds value of property antiAliased. */
    private boolean antiAliased= ("on".equals(DasProperties.getInstance().get("antiAlias")));
    
    /** The 'image' of the data */
    private GeneralPath path;
    
    public SymbolLineRenderer(DataSet ds) {
        super(ds);
    }
    
    public SymbolLineRenderer(DataSetDescriptor dsd) {
        super(dsd);
    }
    
    public void render(Graphics g, DasAxis xAxis, DasAxis yAxis) {
        long timer0= System.currentTimeMillis();
        
        VectorDataSet dataSet= (VectorDataSet)getDataSet();
        if (dataSet == null || dataSet.getXLength() == 0) {            
            return;
        }
        
        Graphics2D graphics= (Graphics2D) g;
        
        RenderingHints hints0= graphics.getRenderingHints();
        if ( antiAliased ) {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        } else {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
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
        
        int ixmax, ixmin;
        
        ixmin= VectorUtil.closestXTag(dataSet,xmin,xUnits);
        if ( ixmin>0 ) ixmin--;
        ixmax= VectorUtil.closestXTag(dataSet,xmax,xUnits);
        if ( ixmax<dataSet.getXLength()-1 ) ixmax++;
        
        graphics.setColor(color.toColor());                        
        
        if (path != null) {
            psymConnector.draw(graphics, path, lineWidth);
        }

        for (int index = ixmin; index <= ixmax; index++) {
            if ( ! yUnits.isFill(dataSet.getDouble(index,yUnits)) ) {
                double i = xAxis.transform(dataSet.getXTagDouble(index,xUnits),xUnits);
                double j = yAxis.transform(dataSet.getDouble(index,yUnits),yUnits);
                if ( Double.isNaN(j) ) {
                    //DasApplication.getDefaultApplication().getDebugLogger().warning("got NaN");
                } else {
                    psym.draw( graphics, i, j, (float)symSize );
                }
            }
        }
        
        long milli= System.currentTimeMillis();
        edu.uiowa.physics.pw.das.DasProperties.getLogger().finer( "render: "+ ( milli - timer0 ) + " total:" + ( milli - lastUpdateMillis )+ " fps:"+ (1000./( milli - lastUpdateMillis )) );
        lastUpdateMillis= milli;
        
        graphics.setRenderingHints(hints0);
    }
    
    public void updatePlotImage(DasAxis xAxis, DasAxis yAxis, DasProgressMonitor monitor) {
        boolean histogram = this.histogram;
        GeneralPath newPath = new GeneralPath();
        
        VectorDataSet dataSet= (VectorDataSet)getDataSet();
        if (dataSet == null || dataSet.getXLength() == 0) {            
            return;
        }
        Dimension d;
        
        double xmin, xmax, ymin, ymax;
        int ixmax, ixmin;
        
        Units xUnits= xAxis.getUnits();
        Units yUnits= yAxis.getUnits();
        
        xmax= xAxis.getDataMaximum().doubleValue(xUnits);
        xmin= xAxis.getDataMinimum().doubleValue(xUnits);
        ymax= yAxis.getDataMaximum().doubleValue(yUnits);
        ymin= yAxis.getDataMinimum().doubleValue(yUnits);
        
        ixmin= VectorUtil.closestXTag(dataSet,xmin,xUnits);
        if ( ixmin>0 ) ixmin--;
        ixmax= VectorUtil.closestXTag(dataSet,xmax,xUnits);
        if ( ixmax<dataSet.getXLength()-1 ) ixmax++;
        
        double xSampleWidth;
        if (dataSet.getProperty("xTagWidth") != null) {
            Datum xSampleWidthDatum = (Datum)dataSet.getProperty("xTagWidth");
            xSampleWidth = xSampleWidthDatum.doubleValue(xUnits.getOffsetUnits());
        }
        else {
            //Try to load the legacy sample-width property.
            String xSampleWidthString = (String)dataSet.getProperty("x_sample_width");
            if (xSampleWidthString != null) {
                double xSampleWidthSeconds = Double.parseDouble(xSampleWidthString);
                xSampleWidth = Units.seconds.convertDoubleTo(xUnits.getOffsetUnits(), xSampleWidthSeconds);
            }
            else {
                xSampleWidth = 1e31;
            }
        }
        
        /* fuzz the xSampleWidth */
        xSampleWidth = xSampleWidth * 1.5;
        
        double x0 = -Double.MAX_VALUE;
        double y0 = -Double.MAX_VALUE;
        double i0 = -Double.MAX_VALUE;
        double j0 = -Double.MAX_VALUE;
        boolean skippedLast = true;
        for (int index = ixmin; index <= ixmax; index++) {
            double x = dataSet.getXTagDouble(index, xUnits);
            double y = dataSet.getDouble(index, yUnits);
            double i = xAxis.transform(x, xUnits);
            double j = yAxis.transform(y, yUnits);
            if ( yUnits.isFill(y) || Double.isNaN(y)) {
                skippedLast = true;
            }
            else if (skippedLast || Math.abs(x - x0) > xSampleWidth) {
                newPath.moveTo((float)i, (float)j);
                skippedLast = false;
            }
            else {
                if (histogram) {
                    double i1 = (i0 + i)/2;
                    newPath.lineTo((float)i1, (float)j0);
                    newPath.lineTo((float)i1, (float)j);
                    newPath.lineTo((float)i, (float)j);
                }
                else {
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
        if (getParent() != null) {
            getParent().repaint();
        }
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
     * @param symsize New value of property symsize.
     */
    public void setSymSize(double symSize) {
        this.symSize= symSize;
        setPsym(this.psym);
        refreshImage();
    }
    
    /** Getter for property color.
     * @return Value of property color.
     */
    public SymColor getColor() {
        return color;
    }
    
    /** Setter for property color.
     * @param color New value of property color.
     */
    public void setColor(SymColor color) {
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
        }
        catch (edu.uiowa.physics.pw.das.DasException de) {
            edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(de);
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
            if (getParent() != null && getParent().getCanvas() != null) {
                new Runnable() {
                    {RequestProcessor.invokeLater(this, getParent().getCanvas());}
                    public void run() {
                        updatePlotImage(getParent().getXAxis(), getParent().getYAxis(), null);
                    }
                };
            }
        }
    }
}
