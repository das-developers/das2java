/* File: DasStackedSymbolPlot.java
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

/**
 *
 * @author  jessica
 */
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.util.DasDate;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.TimeLocationUnits;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.dataset.XMultiYDataSet;
import edu.uiowa.physics.pw.das.dataset.XMultiYDataSetDescriptor;

import java.awt.*;
import java.awt.geom.Line2D;

public class DasStackedSymbolPlot extends DasPlot {
    
    protected double symSize= 1.0;
    
    protected Psym psym = Psym.LINES;
    
     /*
    public static DasStackedSymbolPlot create(DasCanvas parent, double[] x, double[] y) {
        return DasStackedSymbolPlot.create(parent, XMultiYDataSet.create(x,y));
    }
      
    public static DasStackedSymbolPlot create(DasCanvas parent, XMultiYDataSet Data) {
      
        DasRow row= new DasRow(parent,.05,.85);
        DasColumn column= new DasColumn(parent,0.15,0.95);
      
        double [] x;
        double [] y;
      
        int nx= Data.data.length;
        int ny= Data.data[0].y.length;
      
        x= new double[nx];
      
        for (int i=0; i<nx; i++) {
            x[i]= Data.data[i].x;
        }
        y= new double[nx*ny];
        int iy= 0;
        for (int i=0; i<nx; i++) {
            for (int j=0; j<ny; j++) {
                if (Data.data[i].y[j]!=Data.y_fill) {
                    y[iy++]= Data.data[i].y[j];
                }
            }
       }
      
        DasAxis xAxis;
        if (Data.getTimeBase()==null) {
            xAxis= DasAxis.create(x,row,column,DasAxis.HORIZONTAL,false);
        } else {
            xAxis= new DasTimeAxis(Data.getStartTime(),Data.getEndTime(),
                              row,column,DasAxis.HORIZONTAL);
        }
      
        return new DasStackedSymbolPlot(Data,xAxis,
           DasAxis.create(y,row,column,DasAxis.VERTICAL,false),row,column);
    }
      */
    
    public DasStackedSymbolPlot(XMultiYDataSet Data, DasAxis xAxis, DasAxis yAxis, DasRow row, DasColumn column ) {
        super(xAxis,yAxis,row,column);
        this.Data = Data;
    }
    
    public DasStackedSymbolPlot( XMultiYDataSetDescriptor dsd, DasAxis xAxis, DasAxis yAxis, DasRow row, DasColumn column ) {
        super(xAxis,yAxis,row,column);
    }
    
    /**
     * Accessor method for <code>psym</code> property
     *
     * TODO: change psym property to type-save enumeration class
     *
     * @return the value of the <code>psym</code>property
     */
    public Psym getPsym() {
        return psym;
    }
    
    
    /**
     * Mutator method for <code>psym</code> property
     *
     * TODO: change psym property to type-safe enumeration class
     *
     * @param psym the new value for the psym property
     */
    public void setPsym(Psym psym){
        if (psym == null) throw new NullPointerException("psym cannot be null");
        Object oldValue = this.psym;
        this.psym = psym;
        repaint();
        if (oldValue != psym) firePropertyChange("psym", oldValue, psym);
    }
    
    public void addData(double [] x, double [] y) {
        this.Data= XMultiYDataSet.create(x,Units.dimensionless,y,Units.dimensionless);
    }
    
    public void addData(double[] y) {
        double[] x= new double[y.length];
        for (int i=0; i<x.length; i++) x[i]=i;
        addData(x,y);
    }
    
    public void addData(XMultiYDataSet Data) {
        this.Data = Data;
    }
    
    protected void drawContent(Graphics2D graphics) {
        
        XMultiYDataSet Data= ( XMultiYDataSet ) this.Data;
        if (Data == null) return;
        
        if (getXAxis().getUnits()!=Data.getXUnits()) throw new IllegalArgumentException("Data x units and xAxis units differ");
        if (getYAxis().getUnits()!=Data.getYUnits()) throw new IllegalArgumentException("Data y units and yAxis units differ");
        
        Dimension d;
        
        double xmax= getXAxis().getDataMaximum().getValue();
        double xmin= getXAxis().getDataMinimum().getValue();
        double ymax= getYAxis().getDataMaximum().getValue();
        double ymin= getYAxis().getDataMinimum().getValue();
        int ixmax, ixmin;
        
        ixmin=0;
        while (ixmin<Data.data.length-1 && Data.data[ixmin].x<xmin) ixmin++;
        if (ixmin>Data.data.length-1) ixmin--;
        
        ixmax=Data.data.length-1;
        while (ixmax>0 && Data.data[ixmin].x>xmax) ixmax--;
        if (ixmax<0) ixmax++;
        
        graphics.setColor(Color.black);        
        
        for (int iy = 0; iy < Data.data[0].y.length; iy++) {            
            
            if ( psym.drawsLines() ) {
                int x0 = (int)Math.floor(getXAxis().transform(Data.data[ixmin].x,Data.getXUnits()) + 0.5);
                int y0 = (int)Math.floor(getYAxis().transform(Data.data[ixmin].y[iy],Data.getYUnits()) + 0.5);
                Line2D.Double line= new Line2D.Double();
                for (int i = ixmin+1; i <= ixmax; i++) {
                    int x = (int)Math.floor(getXAxis().transform(Data.data[i].x,Data.getXUnits()) + 0.5);
                    int y = (int)Math.floor(getYAxis().transform(Data.data[i].y[iy],Data.getYUnits()) + 0.5);
                    if ( Data.data[i].y[iy] != Data.y_fill ) {
                        if ( Data.data[i].y[iy] != Data.y_fill && Data.data[i-1].y[iy] != Data.y_fill ) {
                            line.setLine(x0,y0,x,y);
                            graphics.draw(line);
                        }
                        x0= x;
                        y0= y;
                    }
                }
            }
            for (int i = ixmin; i <= ixmax; i++) {
                if ( Data.data[i].y[iy] != Data.y_fill ) {
                    int x = (int)Math.floor(getXAxis().transform(Data.data[i].x,Data.getXUnits()) + 0.5);
                    int y = (int)Math.floor(getYAxis().transform(Data.data[i].y[iy],Data.getYUnits()) + 0.5);                    
                    psym.draw(graphics, x, y);
                }
            }
        }        
    }
    
    
    public static DasStackedSymbolPlot create(DasCanvas parent, XMultiYDataSetDescriptor dsd) {
        
        DasRow row= new DasRow(parent,.05,.85);
        DasColumn column= new DasColumn(parent,0.15,0.90);
        
        DasAxis xAxis;
        DasAxis yAxis;
        
        edu.uiowa.physics.pw.das.util.DasDie.println("  dsd: "+dsd+" units: "+dsd.getXUnits() );
        
        if (dsd.getXUnits() instanceof TimeLocationUnits ) {
            xAxis= new DasTimeAxis( new DasDate("2000/1/1"), new DasDate("2000/1/2"), row, column, DasAxis.HORIZONTAL );
        } else {
            xAxis= new DasAxis( new Datum(0,dsd.getXUnits()), new Datum(10,dsd.getXUnits()), row, column, DasAxis.HORIZONTAL );
        }
        
        if (dsd.getYUnits() instanceof TimeLocationUnits ) {
            yAxis= new DasTimeAxis( new DasDate("2000/1/1"), new DasDate("2000/1/2"), row, column, DasAxis.VERTICAL );
        } else {
            yAxis= new DasAxis( new Datum(0,dsd.getYUnits()), new Datum(10,dsd.getYUnits()), row, column, DasAxis.VERTICAL );
        }
        
        DasStackedSymbolPlot result= new DasStackedSymbolPlot(dsd,
        xAxis, yAxis,
        row, column );
        
        parent.addCanvasComponent(result);
        parent.addCanvasComponent(result.getXAxis());
        parent.addCanvasComponent(result.getYAxis());
        return result;
    }
    
    public static DasStackedSymbolPlot create(DasCanvas parent, XMultiYDataSet Data) {
        DasRow row= new DasRow(parent,.05,.85);
        DasColumn column= new DasColumn(parent,0.15,0.90);
        
        double [] x;
        
        int nx= Data.data.length;
        int ny= Data.ny*nx;
        
        x= new double[nx];
        for (int i=0; i<nx; i++) {
            x[i]= Data.data[i].x;
        }
        
        double y_fill= Data.y_fill;
        
        double [] y= new double[ny];
        int iy= 0;
        for (int i=0; i<nx; i++) {
            for (int j=0; j<Data.ny; j++) {
                if (Data.data[i].y[j] != y_fill)
                    y[iy++]= Data.data[i].y[j];
            }
        }
        
        DasStackedSymbolPlot result= new DasStackedSymbolPlot(Data,
        DasAxis.create(x,Data.getXUnits(),row,column,DasAxis.HORIZONTAL,false),
        DasAxis.create(y,Data.getYUnits(),row,column,DasAxis.VERTICAL,false),
        row, column );
        
        parent.addCanvasComponent(result);
        parent.addCanvasComponent(result.getXAxis());
        parent.addCanvasComponent(result.getYAxis());
        return result;
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
    
}
