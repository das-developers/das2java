/* File: XMultiYDataSet.java
 * Copyright (C) 2002-2003 University of Iowa
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

package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.graph.*;
import edu.uiowa.physics.pw.das.util.DasDate;
import edu.uiowa.physics.pw.das.datum.LocationUnits;
import edu.uiowa.physics.pw.das.datum.TimeLocationUnits;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.dataset.DataSet;
import edu.uiowa.physics.pw.das.dataset.XMultiY;

import javax.swing.*;
import java.awt.*;

/**
 *
 * @author  eew
 */
public class XMultiYDataSet extends DataSet implements java.io.Serializable {
    
    //static final long serialVersionUID = -1170391053428276284L;
    
    public int ny;
    
    public double x_sample_width = Double.MAX_VALUE;
    
    public double y_fill;
    
    public String description = "";
    
    public String x_parameter="";
    
    public String x_unit="";
    
    public String y_parameter="";
    
    public String y_unit="";
    
    private Units yUnits=null;
    
    public XMultiY[] data;
    
    public double xSampleWidth;
    
    /** Creates a new instance of XMultiY */
    public XMultiYDataSet(XMultiYDataSetDescriptor dataSetDescriptor) {
        super( dataSetDescriptor );
        yUnits= dataSetDescriptor.getYUnits();
    }
    
    public XMultiYDataSet(XMultiYDataSetDescriptor dataSetDescriptor, DasDate start, DasDate end ) {
        super(dataSetDescriptor,start, end);
        if (dataSetDescriptor!=null) {
            yUnits= dataSetDescriptor.getYUnits();
        } else {
            yUnits= Units.dimensionless;
        }
    }
    
    public XMultiYDataSet( Units xUnits, Units yUnits ) {
        super(xUnits);
        this.yUnits= yUnits;       
    }
    
    public static XMultiYDataSet create(double[] x, Units xUnits, double[] y, Units yUnits ) {
        XMultiYDataSet result= new XMultiYDataSet(xUnits,yUnits);
        
        result.data= new XMultiY[x.length];
        double [] yy= new double[1];
        for (int i=0; i<x.length; i++) {
            yy[0]= y[i];
            result.data[i]= new XMultiY(x[i],yy);
        }
        result.ny=1;
        return result;
    }
    
    public static XMultiYDataSet createEmptyDataSet() {
        return create(new double[0], Units.dimensionless, new double[0], Units.dimensionless);
    }
    
    public static XMultiYDataSet create(DasDate[] x, double[] y) {
      	DasDate startTime= x[0];
	DasDate endTime= x[x.length-1];

	XMultiYDataSet result= new XMultiYDataSet(null, startTime,endTime);
	result.data= new XMultiY[x.length];

        for (int i=0; i<x.length; i++) {
            result.data[i]= new XMultiY();
            result.data[i].x= x[i].subtract(startTime);
            result.data[i].y= new double[1];
            result.data[i].y[0]= y[i];
        }
        result.ny=1;	

	return result;
    }

    public static XMultiYDataSet create(double[] y) {
        double [] x= new double[y.length];
        for (int i=0; i<x.length; i++)
            x[i]= i;
        return create(x,Units.dimensionless,y,Units.dimensionless);
    }
    
    public String toString() {
        return getClass().getName() + "[Description = \"" + description + "\" Start time = " + startTime + " End time = " + endTime + "]";
        
    }
    
    public Units getYUnits() {
        return yUnits;
    }

    
    public void visualize() {
        DasCanvas canvas= new DasCanvas(640,480);
        DasRow row= DasRow.create(canvas);
        DasColumn col= DasColumn.create(canvas);
        DasAxis xAxis= createXAxis(row,col);
        DasAxis yAxis= createYAxis(row,col);
        SymbolLineRenderer rend= new SymbolLineRenderer(this);
        DasPlot plot= new DasPlot(xAxis,yAxis,row,col);
        plot.addRenderer(rend);
        canvas.addCanvasComponent(plot);
        JFrame jFrame= new JFrame("Visualize()");
        JPanel panel= new JPanel(new BorderLayout());
        panel.add(canvas,BorderLayout.CENTER);
        jFrame.setContentPane(panel);
        jFrame.pack();
        jFrame.setVisible(true);
        jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
    
    public DasAxis createXAxis(DasRow row, DasColumn column) {
        double [] x;
        
        int nx= this.data.length;
        
        x= new double[nx];
        for (int i=0; i<nx; i++) {
            x[i]= this.data[i].x;
        }
        
        boolean isLog;
        Units units= getXUnits();
        if ( ( x[x.length-1] / x[0] ) > 1e3 && x[0] > 0. && ! ( units instanceof LocationUnits ) ) {
            isLog=true;
        } else {
            isLog=false;
        }
        
        if ( ! (getXUnits() instanceof TimeLocationUnits ) ) {
            return DasAxis.create(x,getXUnits(),row,column,DasAxis.HORIZONTAL,false);
        } else {
            return DasTimeAxis.create( x, getXUnits(),row, column, DasAxis.HORIZONTAL );
        }
    }
    
    public DasAxis createYAxis(DasRow row, DasColumn column) {
        
        double [] y;
        y= new double[this.data.length];
        for (int i=0; i<this.data.length; i++) {
            y[i]= this.data[i].y[0];
        }
        
        boolean isLog;
        Units units= getYUnits();
        if ( ( y[y.length-1] / y[0] ) > 1e3 && y[0] > 0.  && ! ( units instanceof LocationUnits ) ) {
            isLog=true;
        } else {
            isLog=false;
        }
        return DasAxis.create(y,yUnits,row,column,DasAxis.VERTICAL,isLog);
        
    }
    
}
