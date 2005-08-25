/*
 * Leveler.java
 *
 * Created on April 22, 2003, 4:58 PM
 */


/**
 *
 * @author  jbf
 */
// Manages a set of rows or columns, making sure they fill a space without
// overlapping.
package edu.uiowa.physics.pw.das.graph;

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.datum.*;
import java.util.ArrayList;
import javax.swing.*;

public class Leveler {
    
    ArrayList rows;  // DasDevicePositions
    ArrayList weights;  // doubles
    double topMargin;
    double bottomMargin;
    double interMargin;
    
    DasCanvas parent;
    
    private static class LevelRow extends DasRow {
        
        Leveler lev;
        
        /** Creates a new instance of DasLevelRow */
        public LevelRow( DasCanvas parent, Leveler lev, double nposition, double weight ) {
            super(parent,0,0);
            this.lev= lev;
            lev.insertAt(nposition,this,weight);
        }
        
        public LevelRow(DasCanvas parent, Leveler lev, double nposition) {
            this( parent, lev, nposition, 1.0 );
        }
        
        public double getMaximum() {
            if (lev==null) {
                return 0.;
            } else {
                return lev.getMaximum(this);
            }
        }
        
        public double getMinimum() {
            if (lev==null) {
                return 0.;
            } else {
                return lev.getMinimum(this);
            }
        }
        
        public int getDMinimum() {
            return (int)( getMinimum()*getDeviceSize() );
        }
        
        public int getDMaximum() {
            return (int)( getMaximum()*getDeviceSize() );
        }
        
        public void setDPosition( int minimum, int maximum) {
            lev.setMaximum(this,maximum/(float)getDeviceSize());
            lev.setMinimum(this,minimum/(float)getDeviceSize());
        }

        public void setDMinimum(int minimum) {

            super.setDMinimum(minimum);
        }

        public void setDMaximum(int maximum) {

            super.setDMaximum(maximum);
        }
    }
    
    /** Creates a new instance of Leveler */
    public Leveler( DasCanvas parent ) {
        this.parent= parent;
        rows= new ArrayList();
        weights= new ArrayList();
        topMargin= 0.05;
        bottomMargin= 0.10;
        interMargin= 0.03;
    }
    
    public DasRow getRow( double nposition, double weight ) {
        LevelRow r= new LevelRow( parent, this, nposition, weight );
        return r;
    }
        
    public DasRow getRow( double nposition ) {
        LevelRow r= new LevelRow( parent, this, nposition );
        return r;
    }

    public DasRow getRow( ) {
        LevelRow r= new LevelRow( parent, this, 1.0 );
        return r;
    }
    
    void insertAt( double nposition, DasDevicePosition row ) {
        insertAt( nposition, row, 1.0 );
    }
    
    void insertAt( double nposition, DasDevicePosition row, double weight ) {
        int i;
        if ( nposition==0 ) {
            i=0;
        } else if ( nposition==1.0 ) {
            i=rows.size();
        } else {
            throw new IllegalArgumentException( "nposition must be 0.0 or 1.0" );
        }
        rows.add(i,row);
        weights.add(i,new Double(weight));
    }
    
    void remove( DasDevicePosition row ) {
        int i= rows.indexOf(row);
        rows.remove(i);
        weights.remove(i);
    }
    
    /*
     * return the index of the row that contains nPosition 
     */
    private int objectIndexAt(double nposition) {
        int i=0;
        for ( i=0; i<rows.size(); i++ ) {
            if ( getMinimum(i)<=nposition && nposition < getMaximum(i) ) break;
        }  
        return i;
    }
    
    private double integrateWeight(int nRows) {
        if (nRows==0) {
            return 0.;
        } else {
            double totalWeight= ((Double)weights.get(0)).doubleValue();
            for (int i=1; i<nRows; i++) {
                totalWeight+= ((Double)weights.get(i)).doubleValue();
            }
            return totalWeight;
        }
    }
    
    public void setInsideMargin( double n ) {        
        this.interMargin= n;
    }
    
    private double getMinimum(int i) {
        
        double totalWeight= integrateWeight(rows.size());
        double partialWeight= integrateWeight(i);
        
        double alphaFt= 1.0 - bottomMargin - topMargin - ((rows.size()-1) * interMargin);
        double nWeight= ( partialWeight / totalWeight );
        double plotsFt= alphaFt * nWeight;
        return topMargin + plotsFt + ( i * interMargin );
    }
    
    double getMinimum( DasDevicePosition row) {
        int i= rows.indexOf(row);
        return getMinimum(i);
    }
    
    double getMaximum(int i) {
        return getMinimum(i+1)-interMargin;
    }
    
    double getMaximum(DasDevicePosition row) {
        int i= rows.indexOf(row);
        return getMaximum(i);
    }
    
    private void printWeights() {
        for (int i=0; i<weights.size(); i++) {
            System.out.println("  "+i+": "+weights.get(i));
        }
        System.out.println("total: "+integrateWeight(rows.size()));
    }
    
    double [] getMinima() {
        double[] result= new double[weights.size()];
        for (int i=0; i<weights.size(); i++) {
            result[i]= getMinimum(i);
        }
        return result;
    }
    
    double [] getMaxima() {
        double[] result= new double[weights.size()];
        for (int i=0; i<weights.size(); i++) {
            result[i]= getMaximum(i);
        }
        return result;
    }
    
    void setWeights( double[] minima, double[] maxima ) {
        double totalWeight= integrateWeight(rows.size());
        double[] ftWeight= new double[rows.size()]; // wieght in normal units
        double ftTotalWeight= 0.;
        
        for (int j=0; j<rows.size(); j++) {
            ftWeight[j]= maxima[j]-minima[j];
            ftTotalWeight+= ftWeight[j];
        }
        
        for (int j=0; j<rows.size(); j++) {
            weights.set( j,new Double(totalWeight * ftWeight[j]/ftTotalWeight) );
        }
    }
    
    /*
     * sets the top margin in canvas-normal units.
     */
    public void setTopMargin( double nmargin ) {
        topMargin= nmargin;        
    }
    
    /*
     * sets the bottom margin in canvas-normal units.
     */
    public void setBottomMargin( double nmargin ) {
        bottomMargin= nmargin;
    }
    
    void setMaximum(DasDevicePosition row, double nposition ) {
        
        double minima[]= getMinima();
        double maxima[]= getMaxima();
        
        int i= rows.indexOf(row);
        
        maxima[i]= nposition;
        
        double alpha1= nposition;
        double alpha2= 1.-bottomMargin;
        
        if (i==(rows.size()-1)) {
            bottomMargin= 1.-nposition;
        } else {
            int nBelow= rows.size()-1-i;
            double weight3= integrateWeight(rows.size()) - integrateWeight(i+1);
            double fractionalIntegratedWeight= 0.;
            for (int j=1; j<nBelow; j++) {
                fractionalIntegratedWeight+= ((Double)weights.get(j+i)).doubleValue() / weight3;
                maxima[j+i]= alpha2 * fractionalIntegratedWeight + alpha1 * ( 1-fractionalIntegratedWeight );
            }
        }
        
        for ( int j=1; j<rows.size(); j++) {
            minima[j]= maxima[j-1] + interMargin;
        }
        
        printArray("setMax: minima = ", minima );
        printArray("setMax: maxima = ", maxima );
        setWeights( minima, maxima );
        
    }
    
    void setMinimum(DasDevicePosition row, double nposition ) {
        
        double minima[]= getMinima();
        double maxima[]= getMaxima();
        
        
        int i= rows.indexOf(row);
        
        minima[i]= nposition;
        
        double alpha1= topMargin;
        double alpha2= nposition;
        
        if (i==0) {
            topMargin= nposition;
        } else {
            int nAbove= i;
            double weight3= integrateWeight(i);
            double fractionalIntegratedWeight= 0.;
            for (int j=0; j<i; j++) {
                minima[j]= alpha2 * fractionalIntegratedWeight + alpha1 * ( 1-fractionalIntegratedWeight ) ;
                fractionalIntegratedWeight+= ((Double)weights.get(j)).doubleValue() / weight3;
            }
        }
        
        for ( int j=0; j<rows.size()-1; j++) {
            maxima[j]= minima[j+1] - interMargin;
        }
        
        setWeights( minima, maxima );
    }
    
    private void printArray( String label, double [] values ) {
        System.out.print(label);
        for (int i=0; i<values.length-1; i++)
            System.out.print((int)(values[i]*100)+",");
        System.out.println((int)(values[values.length-1]*100));
    }
    
    public String toString() {
        String result= "--- leveler ---";
        for (int i=0; i<rows.size(); i++) {
            result+= "\n"+rows.get(i).toString();
        }
        result+= "-------------\n";
        return result;
    }
    
 
}
