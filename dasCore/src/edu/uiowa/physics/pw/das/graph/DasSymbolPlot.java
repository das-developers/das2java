/* File: DasSymbolPlot.java
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
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.TimeLocationUnits;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.dataset.ConstantDataSetDescriptor;
import edu.uiowa.physics.pw.das.dataset.DataSetDescriptor;
import edu.uiowa.physics.pw.das.datum.*;

public class DasSymbolPlot extends DasPlot {
       
    private SymbolLineRenderer renderer;
    
    public DasSymbolPlot(VectorDataSet data, DasAxis xAxis, DasAxis yAxis, DasRow row, DasColumn column) {
        this((data==null ? null : new ConstantDataSetDescriptor(data)),xAxis,yAxis,row,column);
    }
    
    public DasSymbolPlot(DataSetDescriptor dsd, DasAxis xAxis, DasAxis yAxis, DasRow row, DasColumn column) {
        super(xAxis,yAxis,row,column);
        renderer= new SymbolLineRenderer(dsd);
        addRenderer(renderer);
    }

    
    /**
     * Accessor method for <code>psym</code> property
     *
     * TODO: change psym property to type-save enumeration class
     *
     * @return the value of the <code>psym</code>property
     */
    public Psym getPsym() {
        return renderer.getPsym();
    }
    
    
    /**
     * Mutator method for <code>psym</code> property
     *
     * TODO: change psym property to type-safe enumeration class
     *
     * @param psym the new value for the psym property
     */
    public void setPsym(Psym psym){
        renderer.setPsym(psym);
    }
    
    public void addData(double [] x, double [] y) {
        Units units = Units.dimensionless;
        addData(new DefaultVectorDataSet(x, units, y, units, java.util.Collections.EMPTY_MAP));
    }
    
    public void addData(double[] y) {
        double[] x= new double[y.length];
        for (int i=0; i<x.length; i++) x[i]=i;
        addData(x,y);
    }
    
    public void addData(VectorDataSet Data) {
        renderer.setDataSet(Data);
    }
    
    public static DasSymbolPlot create(DasCanvas parent, DataSetDescriptor dsd) {
        
        DasRow row= new DasRow(parent,.05,.85);
        DasColumn column= new DasColumn(parent,0.15,0.90);
        
        DasAxis xAxis;
        DasAxis yAxis;
        
        edu.uiowa.physics.pw.das.util.DasDie.println("  dsd: "+dsd+" units: "+dsd.getXUnits() );
        
        if (dsd.getXUnits() instanceof TimeLocationUnits ) {
            xAxis= new DasAxis( TimeUtil.createValid("2000/1/1"), TimeUtil.createValid("2000/1/2"), row, column, DasAxis.HORIZONTAL );
        } else {
            xAxis= new DasAxis( Datum.create(0,dsd.getXUnits()), Datum.create(10,dsd.getXUnits()), row, column, DasAxis.HORIZONTAL );
        }
        
        yAxis= new DasAxis( Datum.create(0, Units.dimensionless), Datum.create(10, Units.dimensionless), row, column, DasAxis.VERTICAL );
        
        DasSymbolPlot result= new DasSymbolPlot(dsd,
        xAxis, yAxis,
        row, column );
        
        parent.addCanvasComponent(result);
        parent.addCanvasComponent(result.getXAxis());
        parent.addCanvasComponent(result.getYAxis());
        return result;
    }
    
    public static DasSymbolPlot create(DasCanvas parent, VectorDataSet Data) {
        DasRow row= new DasRow(parent,.05,.85);
        DasColumn column= new DasColumn(parent,0.15,0.90);
        
        double [] x;
        
        int nx= Data.getXLength();
        int ny= nx;
        
        x= new double[nx];
        for (int i=0; i<nx; i++) {
            x[i]= Data.getXTagDouble(i, Data.getXUnits());
        }
        
        double y_fill= Double.NaN;
        
        double [] y= new double[ny];
        int iy= 0;
        for (int i=0; i < ny; i++) {
            double yValue = Data.getDouble(i, Data.getYUnits());
            if (!Double.isNaN(yValue)) {
                y[iy++]= yValue;
            }
        }
        
        DasSymbolPlot result= new DasSymbolPlot(Data,
        DasAxis.create(x,Data.getXUnits(),row,column,DasAxis.HORIZONTAL,false),
        DasAxis.create(y,Data.getYUnits(),row,column,DasAxis.VERTICAL,false),
        row, column );
        
        parent.addCanvasComponent(result);
        return result;
    }
    
    
    
    
    
    
    
    /** Getter for property symsize.
     * @return Value of property symsize.
     */
    public double getSymSize() {
        return renderer.getSymSize();
    }
    
    /** Setter for property symsize.
     * @param symsize New value of property symsize.
     */
    public void setSymSize(double symSize) {
        renderer.setSymSize(symSize);
    }
    
}
