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
import edu.uiowa.physics.pw.das.dataset.DataSetDescriptor;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.client.XMultiYDataSet;
import edu.uiowa.physics.pw.das.client.XMultiYDataSetDescriptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.*;
import java.awt.geom.Line2D;

/**
 *
 * @author  jbf
 */
public class SymbolLineRenderer extends Renderer {
    
    private Psym psym = Psym.LINES;
    private double symSize = 1.0;
    private float lineWidth = 1.0f;
    
    /** Holds value of property color. */
    private SymColor color= SymColor.black;
    
    public SymbolLineRenderer(edu.uiowa.physics.pw.das.client.XMultiYDataSetDescriptor dsd) {
        this((edu.uiowa.physics.pw.das.dataset.DataSetDescriptor)dsd);
    }
    
    public SymbolLineRenderer(edu.uiowa.physics.pw.das.client.XMultiYDataSet ds) {
        super(ds);
    }
    
    protected SymbolLineRenderer(edu.uiowa.physics.pw.das.dataset.DataSetDescriptor dsd) {
        super(dsd);
    }
    
    /**
     * @deprecated use {@link
     * #SymbolLineRenderer(das_prot.XMultiYDataSetDescriptor)}
     */
    protected SymbolLineRenderer(DasPlot parent, edu.uiowa.physics.pw.das.dataset.DataSetDescriptor dsd) {
        super(dsd);
        this.parent = parent;
    }
    
    /** Creates a new instance of SymbolLineRenderer
     * @deprecated use {@link
     * #SymbolLineRenderer(das_proto.XMultiYDataSetDescriptor)}
     */
    public SymbolLineRenderer(DasPlot parent, edu.uiowa.physics.pw.das.client.XMultiYDataSetDescriptor dsd) {
        this(dsd);
        this.parent = parent;
    }
    
    /**
     * @deprecated use {@link
     * #SymbolLineRenderer(das_proto.XMultiYDataSet)}
     */
    public SymbolLineRenderer(DasPlot parent, edu.uiowa.physics.pw.das.client.XMultiYDataSet ds) {
        this(ds);
        this.parent = parent;
    }
    
    public void render(Graphics g, DasAxis xAxis, DasAxis yAxis) {
        edu.uiowa.physics.pw.das.client.XMultiYDataSet Data= ( edu.uiowa.physics.pw.das.client.XMultiYDataSet ) getDataSet();
        if (Data == null) return;
        
        double xSampleWidth = Data.xSampleWidth;
        if (xSampleWidth == 0.0) {
            xSampleWidth = Double.POSITIVE_INFINITY;
        }
        
        Graphics2D graphics= (Graphics2D) g;
        
        if (xAxis.getUnits()!=Data.getXUnits()) throw new IllegalArgumentException("Data x units and xAxis units differ");
        if (yAxis.getUnits()!=Data.getYUnits()) throw new IllegalArgumentException("Data y units and yAxis units differ");
        
        Dimension d;
        
        edu.uiowa.physics.pw.das.datum.Units xUnits= xAxis.getUnits();
        edu.uiowa.physics.pw.das.datum.Units yUnits= yAxis.getUnits();
        double xmax= xAxis.getDataMaximum(xUnits);
        double xmin= xAxis.getDataMinimum(xUnits);
        double ymax= yAxis.getDataMaximum(yUnits);
        double ymin= yAxis.getDataMinimum(yUnits);
        int ixmax, ixmin;
        
        ixmin=0;
        while (ixmin<Data.data.length-1 && Data.data[ixmin].x<xmin) ixmin++;
        if (ixmin>Data.data.length-1) ixmin--;
        
        ixmax=Data.data.length-1;
        while (ixmax>0 && Data.data[ixmin].x>xmax) ixmax--;
        if (ixmax<0) ixmax++;
        
        graphics.setColor(color.toColor());
        
        for (int iy = 0; iy < Data.data[0].y.length; iy++) {
            
            if ( psym.drawsLines() ) {
                graphics.setStroke(new BasicStroke(lineWidth));
                int x0 = xAxis.transform(Data.data[ixmin].x,Data.getXUnits());
                int y0 = yAxis.transform(Data.data[ixmin].y[iy],Data.getYUnits());
                Line2D.Double line= new Line2D.Double();
                for (int i = ixmin+1; i <= ixmax; i++) {
                    int x = xAxis.transform(Data.data[i].x,Data.getXUnits());
                    int y = yAxis.transform(Data.data[i].y[iy],Data.getYUnits());
                    if ( Data.data[i].y[iy] != Data.y_fill ) {
                        if ( Data.data[i].y[iy] != Data.y_fill && Data.data[i-1].y[iy] != Data.y_fill
                        && Math.abs(Data.data[i].x - Data.data[i-1].x) < xSampleWidth) {
                            psym.drawLine(graphics,x0,y0,x,y);
                        }
                        x0= x;
                        y0= y;
                    }
                }
                graphics.setStroke(new BasicStroke(1.0f));
            }
            for (int i = ixmin; i <= ixmax; i++) {
                if ( Data.data[i].y[iy] != Data.y_fill ) {
                    int x = xAxis.transform(Data.data[i].x,Data.getXUnits());
                    int y = yAxis.transform(Data.data[i].y[iy],Data.getYUnits());
                    psym.draw(graphics, x, y);
                }
            }
        }
    }
    
    public void updatePlotImage(DasAxis xAxis, DasAxis yAxis) {
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
        if ( getParent()!=null ) getParent().repaint();
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
        getParent().repaint();
    }
    
    public float getLineWidth() {
        return lineWidth;
    }
    
    public void setLineWidth(float f) {
        lineWidth = f;
        getParent().repaint();
    }
    
    protected void installRenderer() {
    }
    
    protected void uninstallRenderer() {
    }
    
    public static SymbolLineRenderer processLinePlotElement(Element element, DasPlot parent, FormBase form) {
        String dataSetID = element.getAttribute("dataSetID");
        Psym psym = Psym.parsePsym(element.getAttribute("psym"));
        SymColor color = SymColor.parseSymColor(element.getAttribute("color"));
        SymbolLineRenderer renderer = new SymbolLineRenderer(parent, (edu.uiowa.physics.pw.das.client.XMultiYDataSet)null);
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
    
}
