/* File: DasLabelAxis.java
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
package edu.uiowa.physics.pw.das.graph;

import edu.uiowa.physics.pw.das.datum.DasFormatter;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.EnumerationDatum;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.graph.event.DasUpdateEvent;
import edu.uiowa.physics.pw.das.graph.event.DasUpdateListener;

import javax.swing.*;
import java.text.DecimalFormat;


public class DasLabelAxis extends DasAxis implements DasUpdateListener {
    DecimalFormat nfy= null;
    edu.uiowa.physics.pw.das.datum.Datum[] labels= null;
    double[] labelValues= null;
    edu.uiowa.physics.pw.das.datum.Units labelUnits=null;
    double[] labelPositions= null;
    edu.uiowa.physics.pw.das.datum.DasFormatter df= null;
    int indexMinimum;  // first label to be displayed
    int indexMaximum;  // last label to be displayed

    private void setLabels( edu.uiowa.physics.pw.das.datum.Datum[] labels ) {
        if (labels.length==0) {
            throw new IllegalArgumentException( "labels can not be a zero-length array!" );
        }
        this.labels= labels;
        this.labelPositions= new double[labels.length];
        indexMinimum= 0;
        indexMaximum= labels.length-1;
        //this.df= new DecimalFormat();
        labelValues= new double[ labels.length ];
        labelUnits= labels[0].getUnits();
        for ( int i=0; i<labels.length; i++ ) {
            labelValues[i]= labels[i].getValue();
            if ( labels[i].getUnits() != labelUnits ) {
                throw new IllegalArgumentException( "Datums must all have same units!" );
            }
        }
        this.df= labels[0].getFormatter(labels[1],1);
    }
    
    protected DasLabelAxis(edu.uiowa.physics.pw.das.datum.Datum[] labels, DataRange dataRange, DasRow row, DasColumn column, int orientation) {
        super( dataRange, row, column, orientation );                
        setLabels(labels);
        getDataRange().addpwUpdateListener(this);
    }


    public DasLabelAxis(edu.uiowa.physics.pw.das.datum.Datum[] labels, DasRow row, DasColumn column, int orientation) {
        super( labels[0], labels[labels.length-1], row, column, orientation, false );
        setLabels(labels);
        getDataRange().addpwUpdateListener(this);
    }
    
    
    private void updateTickPositions() {        
        int nlabel= indexMaximum - indexMinimum + 1;

        double size;
        double min;
        
        double interItemSpacing;
        
        if ( this.getOrientation()==DasAxis.HORIZONTAL ) {
            size= getColumn().getWidth();            
            min= getColumn().getDMinimum();
            interItemSpacing= size / nlabel;
        } else {
            size= getRow().getHeight();
            min= getRow().getDMaximum();
            interItemSpacing= -1 * size / nlabel;
        }
                                      
        for ( int i=0; i<labelPositions.length; i++ ) {
            labelPositions[i]= min + interItemSpacing * ( (i-indexMinimum)+0.0 );
        }        
       
    }
    
    public edu.uiowa.physics.pw.das.datum.Datum findTick(edu.uiowa.physics.pw.das.datum.Datum xDatum, double direction, boolean minor) {
        // somehow tickv.minor is set to non-zero, and Axis.findTick gets messed up.
        // This is a work-around...
        return xDatum;
    }
    
    public void updateTickV() {
        super.updateTickV();
        updateTickPositions();        
    }
    
    public tickVDescriptor getTickV() {
        tickVDescriptor result= new tickVDescriptor();
        result.units= getUnits();
        int ny= indexMaximum - indexMinimum + 1;
        result.tickV= new double[ny];
        result.minor= 0.;  // no minor ticks
        for (int i=0; i<ny; i++) result.tickV[i]= labels[i+indexMinimum].getValue();
        result.nf= df;
        return result;
    }
    
    public double transform( double value, edu.uiowa.physics.pw.das.datum.Units units ) {
        if ( units!=this.labelUnits ) {
            throw new IllegalArgumentException("units don't match");
        }        
        int iclose= findClosestIndex( labelValues, value );        
        return labelPositions[iclose];
    }
    
    private int findClosestIndex( double[] data, double searchFor ) {
        int iclose=0;
        double closest= Math.abs(data[iclose]-searchFor);
        for ( int i=0; i<labelPositions.length; i++ ) {
            double c1= Math.abs(data[i]-searchFor);
            if ( c1<closest ) {
                iclose= i;
                closest= c1;
            }            
        }                
        return iclose;
    }    
    
    public edu.uiowa.physics.pw.das.datum.Datum invTransform( double idata ) {
        int iclose= findClosestIndex( labelPositions, idata );
        return labels[iclose];        
    }

    public void setDataRange(edu.uiowa.physics.pw.das.datum.Datum minimum, edu.uiowa.physics.pw.das.datum.Datum maximum) {
        super.setDataRange(minimum, maximum);                
    }
         
    protected String tickFormatter(double tickv) {                
        return df.format(tickv,labels[0].getUnits());
    }
    
    public String getLabel(double tickv) {
        return tickFormatter(tickv);
    }
    
    public double getInterItemSpace() { 
        return Math.abs(transform(labels[1])-transform(labels[0]));
    }
    
    
    public DasAxis createAttachedAxis(DasRow row, DasColumn column) {
        DasLabelAxis result= new DasLabelAxis(labels, getDataRange(), row, column, this.getOrientation());
        return result;
    }
    
    public DasAxis createAttachedAxis(DasRow row, DasColumn column, int orientation) {
        return new DasLabelAxis(labels, getDataRange(), row, column, orientation);
    }
    
    public static void main( String[] args ) throws Exception {
        edu.uiowa.physics.pw.das.datum.Datum[] labels= new edu.uiowa.physics.pw.das.datum.Datum[] { edu.uiowa.physics.pw.das.datum.EnumerationDatum.create("cat"), edu.uiowa.physics.pw.das.datum.EnumerationDatum.create("dog"), edu.uiowa.physics.pw.das.datum.EnumerationDatum.create("fish") };
        DasCanvas canvas= new DasCanvas(400,400);
        DasRow row= DasRow.create(canvas);
        DasColumn column= DasColumn.create(canvas);
        
        edu.uiowa.physics.pw.das.datum.Datum[] labels2= edu.uiowa.physics.pw.das.datum.EnumerationDatum.create(new String[] {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"});
        DasPlot p= new DasPlot( new DasLabelAxis( labels, row, column, DasAxis.HORIZONTAL ),
            new DasLabelAxis( labels2, row, column, DasAxis.VERTICAL ), row, column );
        canvas.add(p);
        
        JFrame jframe= new JFrame();
        jframe.setContentPane(canvas);
        jframe.pack();
        jframe.setVisible(true);
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    
    public void update(DasUpdateEvent e) {
        double minimum=  getDataRange().getMinimum();
        double maximum=  getDataRange().getMaximum();
        if ( getDataRange().getUnits()!=this.labelUnits ) { 
            throw new IllegalArgumentException("units don't match");
        }
        
        this.indexMinimum=  findClosestIndex(labelValues,minimum);        
        this.indexMaximum= findClosestIndex(labelValues,maximum);

        if ( this.indexMinimum > this.indexMaximum ) {
            int t= this.indexMinimum;
            this.indexMaximum= this.indexMinimum;
            this.indexMinimum= t; 
        }
        

    }
    
}
