/* File: DasLabelAxis.java
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
 
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.datum.format.DatumFormatter;
import edu.uiowa.physics.pw.das.graph.DasAxis.TickVDescriptor;
import edu.uiowa.physics.pw.das.graph.event.DasUpdateEvent;
import edu.uiowa.physics.pw.das.graph.event.DasUpdateListener;
import edu.uiowa.physics.pw.das.util.*;
import java.awt.*;
import java.text.DecimalFormat;
import javax.swing.JFrame;



public class DasLabelAxis extends DasAxis implements DasUpdateListener {
    DecimalFormat nfy= null;
    Datum[] labels= null;
    double[] labelValues= null;
    Units labelUnits=null;
    int[] labelPositions= null;
    DatumFormatter df= null;
    int indexMinimum;  // first label to be displayed
    int indexMaximum;  // last label to be displayed
    
    private void setLabels(Datum[] labels) {
        if (labels.length==0) {
            throw new IllegalArgumentException( "labels can not be a zero-length array!" );
        }
        this.labels= labels;
        this.labelPositions= new int[labels.length];
        indexMinimum= 0;
        indexMaximum= labels.length-1;
        //this.df= new DecimalFormat();
        labelValues= new double[ labels.length ];
        labelUnits= labels[0].getUnits();
        for ( int i=0; i<labels.length; i++ ) {
            labelValues[i]= labels[i].doubleValue(labelUnits);
            if ( labels[i].getUnits() != labelUnits ) {
                throw new IllegalArgumentException( "Datums must all have same units!" );
            }
        }
        this.df = DatumUtil.bestFormatter(labels[0], labels[1],1);
    }
    
    protected DasLabelAxis(Datum[] labels, DataRange dataRange, DasRow row, DasColumn column, int orientation) {
        super( dataRange, row, column, orientation );
        setLabels(labels);
        getDataRange().addpwUpdateListener(this);
    }
    
    
    public DasLabelAxis(Datum[] labels, DasRow row, DasColumn column, int orientation) {
        super( labels[0], labels[labels.length-1], row, column, orientation, false );
        setLabels(labels);
        getDataRange().addpwUpdateListener(this);
    }
    
    
    public int[] getLabelPositions() {
        return this.labelPositions;
    }
    
    private void updateTickPositions() {
        int nlabel= indexMaximum - indexMinimum + 1;
        
        int size;
        int min;
        
        int interItemSpacing;
        
        if ( this.getOrientation()==DasAxis.HORIZONTAL ) {
            size= getColumn().getWidth()-10;            
            interItemSpacing= size / nlabel;
            min= getColumn().getDMinimum()+5+interItemSpacing/2;
        } else {
            size= getRow().getHeight()-10;            
            interItemSpacing= -1 * size / nlabel ;
            min= getRow().getDMaximum()-5+interItemSpacing/2;
        }
        
        for ( int i=0; i<labelPositions.length; i++ ) {
            labelPositions[i]= min + interItemSpacing * ( (i-indexMinimum)+0 );                        
        }
        
    }
    
    public Datum findTick(Datum xDatum, double direction, boolean minor) {
        // somehow tickv.minor is set to non-zero, and Axis.findTick gets messed up.
        // This is a work-around...
        return xDatum;
    }
    
    public void updateTickV() {
        super.updateTickV();
        updateTickPositions();
    }
    
    public TickVDescriptor getTickV() {
        TickVDescriptor result= new TickVDescriptor();
        result.units= getUnits();
        int ny= indexMaximum - indexMinimum + 1;
        result.tickV= new double[ny];
        result.minorTickV= new double[0];  // no minor ticks
        for (int i=0; i<ny; i++) result.tickV[i]= labels[i+indexMinimum].doubleValue(result.units);
        return result;
    }
    
    public int transform(double value, Units units) {
        if ( units!=this.labelUnits ) {
            throw new IllegalArgumentException("units don't match");
        }
        int iclose= findClosestIndex( labelValues, value );
        return labelPositions[iclose];
    }
    
    private int findClosestIndex( int[] data, int searchFor ) {
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
    
    public Datum invTransform(int idata) {
        int iclose= findClosestIndex( labelPositions, idata );
        return labels[iclose];
    }
    
    public void setDataRange(Datum minimum, Datum maximum) {
        super.setDataRange(minimum, maximum);
    }
    
    protected String tickFormatter(double tickv) {
        return df.format(Datum.create(tickv,labels[0].getUnits()));
    }
    
    public String getLabel(double tickv) {
        return tickFormatter(tickv);
    }
    
    public int getInterItemSpace() {
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
        EnumerationUnits units= EnumerationUnits.create("");
        Datum x= units.createDatum(new Object());
        
        Datum[] labels= new Datum[] { units.createDatum("cat"), units.createDatum("dog"), units.createDatum("fish") };
        DasCanvas canvas= new DasCanvas(400,400);
        DasRow row= DasRow.create(canvas);
        DasColumn column= DasColumn.create(canvas);
        
        Datum[] labels2= units.createDatum(new String[] {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"});
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
    
    protected void paintHorizontalAxis(java.awt.Graphics2D g) {
        boolean bottomTicks = (getOrientation() == BOTTOM || isOppositeAxisVisible());
        boolean bottomTickLabels = (getOrientation() == BOTTOM && areTickLabelsVisible());
        boolean bottomLabel = (getOrientation() == BOTTOM && !axisLabel.equals(""));
        boolean topTicks = (getOrientation() == TOP || isOppositeAxisVisible());
        boolean topTickLabels = (getOrientation() == TOP && areTickLabelsVisible());
        boolean topLabel = (getOrientation() == TOP && !axisLabel.equals(""));
        
        int topPosition = getRow().getDMinimum() - 1;
        int bottomPosition = getRow().getDMaximum();
        int DMax= getColumn().getDMaximum();
        int DMin= getColumn().getDMinimum();
        
        Font labelFont = getTickLabelFont();
        
        double dataMax= dataRange.getMaximum();
        double dataMin= dataRange.getMinimum();
        
        TickVDescriptor ticks= getTickV();
        double[] tickv= ticks.tickV;
        
        if (bottomTicks) {
            g.drawLine(DMin,bottomPosition,DMax,bottomPosition);
        }
        if (topTicks) {
            g.drawLine(DMin,topPosition,DMax,topPosition);
        }
        
        int tickLengthMajor = labelFont.getSize() * 2 / 3;
        int tickLengthMinor = tickLengthMajor / 2;
        int tickLength;
        
        for ( int i=0; i<ticks.tickV.length; i++ ) {
            double tick1= ticks.tickV[i];
            int w= getInterItemSpace();
            int tickPosition= (int)Math.floor(transform(tick1,ticks.units) + 0.5)-w/2;
            tickLength= tickLengthMajor;
            if (bottomTicks) {
                g.drawLine( tickPosition, bottomPosition, tickPosition, bottomPosition + tickLength);
                g.drawLine( tickPosition+w, bottomPosition, tickPosition+w, bottomPosition + tickLength);
                if (bottomTickLabels) {
                    drawLabel(g, tick1, i, tickPosition+w/2 , bottomPosition + tickLength);
                }
            }
            if (topTicks) {
                g.drawLine( tickPosition, topPosition, tickPosition, topPosition - tickLength);
                g.drawLine( tickPosition+w, topPosition, tickPosition+w, topPosition - tickLength);
                if (topTickLabels) {
                    drawLabel(g, tick1, i, tickPosition+w/2, topPosition - tickLength);
                }
            }
        }
        
        
        if (!axisLabel.equals("")) {
            Graphics2D g2 = (Graphics2D)g.create();
            int titlePositionOffset = getTitlePositionOffset();
            GrannyTextRenderer gtr = new GrannyTextRenderer();
            gtr.setString(this, axisLabel);
            int titleWidth = (int)gtr.getWidth();
            int baseline;
            int leftEdge;
            g2.setFont(getLabelFont());
            if (bottomLabel) {
                leftEdge = DMin + (DMax-DMin - titleWidth)/2;
                baseline = bottomPosition + titlePositionOffset;
                gtr.draw(g2, (float)leftEdge, (float)baseline);
            }
            if (topLabel) {
                leftEdge = DMin + (DMax-DMin - titleWidth)/2;
                baseline = topPosition - titlePositionOffset;
                gtr.draw(g2, (float)leftEdge, (float)baseline);
            }
            g2.dispose();
        }
    }
    
    
    protected void paintVerticalAxis(java.awt.Graphics2D g) {
        boolean leftTicks = (getOrientation() == LEFT || isOppositeAxisVisible());
        boolean leftTickLabels = (getOrientation() == LEFT && areTickLabelsVisible());
        boolean leftLabel = (getOrientation() == LEFT && !axisLabel.equals(""));
        boolean rightTicks = (getOrientation() == RIGHT || isOppositeAxisVisible());
        boolean rightTickLabels = (getOrientation() == RIGHT && areTickLabelsVisible());
        boolean rightLabel = (getOrientation() == RIGHT && !axisLabel.equals(""));
        
        int leftPosition = getColumn().getDMinimum() - 1;
        int rightPosition = getColumn().getDMaximum();
        int DMax= getRow().getDMaximum();
        int DMin= getRow().getDMinimum();
        
        Font labelFont = getTickLabelFont();
        
        double dataMax= dataRange.getMaximum();
        double dataMin= dataRange.getMinimum();
        
        TickVDescriptor ticks= getTickV();
        double[] tickv= ticks.tickV;
        
        if (leftTicks) {
            g.drawLine(leftPosition,DMin,leftPosition,DMax);
        }
        if (rightTicks) {
            g.drawLine(rightPosition,DMin,rightPosition,DMax);
        }
        
        int tickLengthMajor= labelFont.getSize()*2/3;
        int tickLengthMinor = tickLengthMajor / 2;
        int tickLength;
        
        for ( int i=0; i<ticks.tickV.length; i++ ) {
            double tick1= ticks.tickV[i];
            if ( tick1>=(dataMin*0.999) && tick1<=(dataMax*1.001) ) {
                int w= getInterItemSpace();
                int tickPosition= (int)Math.floor(transform(tick1,ticks.units) + 0.5) - w/2;
                tickLength= tickLengthMajor;
                if (leftTicks) {
                    g.drawLine( leftPosition, tickPosition, leftPosition - tickLength, tickPosition );
                    g.drawLine( leftPosition, tickPosition+w, leftPosition - tickLength, tickPosition+w );
                    if (leftTickLabels) {
                        drawLabel(g, tick1, i, leftPosition - tickLength, tickPosition+w/2);  
                    }
                }
                if (rightTicks) {
                    g.drawLine( rightPosition, tickPosition, rightPosition + tickLength, tickPosition );
                    g.drawLine( rightPosition, tickPosition+w, rightPosition + tickLength, tickPosition+w );
                    if (rightTickLabels) {
                        drawLabel(g, tick1, i, rightPosition + tickLength, tickPosition+w/2);
                    }
                }
            }
        }
                
        if (!axisLabel.equals("")) {
            Graphics2D g2 = (Graphics2D)g.create();
            int titlePositionOffset = getTitlePositionOffset();
            GrannyTextRenderer gtr = new GrannyTextRenderer();
            gtr.setString(this, axisLabel);
            int titleWidth = (int)gtr.getWidth();
            int baseline;
            int leftEdge;
            g2.setFont(getLabelFont());
            if (leftLabel) {
                g2.rotate(-Math.PI/2.0);
                leftEdge = -DMax + (DMax-DMin - titleWidth)/2;
                baseline = leftPosition - titlePositionOffset;
                gtr.draw(g2, (float)leftEdge, (float)baseline);
            }
            if (rightLabel) {
                g2.rotate(Math.PI/2.0);
                leftEdge = DMin + (DMax-DMin - titleWidth)/2;
                baseline = - rightPosition - titlePositionOffset;
                gtr.draw(g2, (float)leftEdge, (float)baseline);
            }
            g2.dispose();
        }
        
    }
    
}
