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

import edu.uiowa.physics.pw.das.dasml.FormBase;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.util.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.*;
import java.awt.geom.Line2D;

/**
 *
 * @author  jbf
 */
public class SymbolLineRenderer extends Renderer {
    
    private Psym psym = Psym.DOTS;
    private double symSize = 1.0; // radius in pixels
    private float lineWidth = 1.5f; // width in pixels
    
    /** Holds value of property color. */
    private SymColor color= SymColor.black;
    
    /** Holds value of property psymConnector. */
    private PsymConnector psymConnector= PsymConnector.SOLID;
    
    private long lastUpdateMillis;
    
    /** Holds value of property antiAliased. */
    private boolean antiAliased;
    
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
                
        double xSampleWidth= 1e31;
        
        if ( psymConnector != PsymConnector.NONE ) {
            int x0 = xAxis.transform(dataSet.getXTagDouble(ixmin,xUnits),xUnits);
            int y0 = yAxis.transform(dataSet.getDouble(ixmin,yUnits),yUnits);
            for (int i = ixmin+1; i <= ixmax; i++) {
                int x = xAxis.transform(dataSet.getXTagDouble(i,xUnits),xUnits);
                int y = yAxis.transform(dataSet.getDouble(i,yUnits),yUnits);
                if ( !yUnits.isFill(dataSet.getDouble(i,yUnits)) ) {
                    if ( !yUnits.isFill(dataSet.getDouble(i-1,yUnits)) ) {
                        psymConnector.drawLine(graphics,x0,y0,x,y,lineWidth);
                    }
                    x0= x;
                    y0= y;
                }
            }
            graphics.setStroke(new BasicStroke(1.0f));
        }
        for (int i = ixmin; i <= ixmax; i++) {
            if ( ! yUnits.isFill(dataSet.getDouble(i,yUnits)) ) {
                int x = xAxis.transform(dataSet.getXTagDouble(i,xUnits),xUnits);
                int y = yAxis.transform(dataSet.getDouble(i,yUnits),yUnits);
                psym.draw( graphics, x, y, (float)symSize );
            }
        }
        
        long milli= System.currentTimeMillis();
        edu.uiowa.physics.pw.das.DasProperties.getLogger().finer( "render: "+ ( milli - timer0 ) + " total:" + ( milli - lastUpdateMillis )+ " fps:"+ (1000./( milli - lastUpdateMillis )) );
        lastUpdateMillis= milli;
        
        graphics.setRenderingHints(hints0);
    }
    
    public void updatePlotImage(DasAxis xAxis, DasAxis yAxis, DasProgressMonitor monitor) {
    }
    
    private void refreshImage() {
        if ( getParent()!=null ) {
            getParent().markDirty();
            getParent().repaint();
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
    
    /** Getter for property psymConnector.
     * @return Value of property psymConnector.
     *
     */
    public PsymConnector getPsymConnector() {
        return this.psymConnector;
    }
    
    /** Setter for property psymConnector.
     * @param psymConnector New value of property psymConnector.
     *
     */
    public void setPsymConnector(PsymConnector psymConnector) {
        this.psymConnector = psymConnector;
        refreshImage();
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
    }
    
}
