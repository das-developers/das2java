/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dataset;

import java.util.HashMap;
import java.util.Map;

/**
 * DataSet for storing sparse data.  This is used initially to describe bundles.
 * This returns 0 where no data has been set.
 * @author jbf
 */
public class SparseDataSet extends AbstractDataSet implements WritableDataSet {

    int rank;
    Map<String,Double> data;
    int length0;
    int[] qube;
    Map<String,Integer> length;
    
    public SparseDataSet( int rank ) {
        this.rank= rank;
        data= new HashMap();
        length= new HashMap();
    }
    
    /**
     * set the length of the zeroth dimension.  Other dimensions have length set implicitly by the highest value set.
     * If this is not set explicitly, then it will be implicit as well.
     * @param length 
     */
    public void setLength( int length ) {
        this.length0= length0;
    }
    
    /**
     * make this a qube dataset, where all the lengths are the same.
     * @param qube 
     */
    public void setQube( int[] qube ) {
        this.qube= qube;
        this.length0= qube[0];
    }
    
    @Override
    public int rank() {
        return rank;
    }

    @Override
    public int length() {
        return this.length0; 
    }

    @Override
    public int length(int i) {
        if ( this.qube!=null ) {
            return this.qube[1];
        } else {
            Integer len= length.get( String.valueOf(i) );
            return len==null? 0 : len;
        }
    }

    @Override
    public int length(int i0, int i1) {
        if ( this.qube!=null ) {
            return this.qube[2];
        } else {
            Integer len= length.get( String.valueOf(i0)+"_"+String.valueOf(i1) );
            return len==null? 0 : len;
        }
    }

    @Override
    public int length(int i0, int i1, int i2 ) {
        if ( this.qube!=null ) {
            return this.qube[3];
        } else {
            Integer len= length.get( String.valueOf(i0)+"_"+String.valueOf(i1)+"_"+String.valueOf(i2) );
            return len==null? 0 : len;
        }
    }

    
    public void putValue(double d) {
        data.put( "", d );
    }

    public void putValue(int i0, double d) {
        length0= Math.max( i0+1, length0 );
        data.put( String.valueOf(i0), d );
    }

    public void putValue(int i0, int i1, double d) {
        length0= Math.max( i0+1, length0 );
        data.put( String.valueOf(i0)+"_"+String.valueOf(i1), d );
    }

    public void putValue(int i0, int i1, int i2, double d) {
        length0= Math.max( i0+1, length0 );
        data.put( String.valueOf(i0)+"_"+String.valueOf(i1)+"_"+String.valueOf(i2), d );
    }

    public void putValue(int i0, int i1, int i2, int i3, double d) {
        length0= Math.max( i0+1, length0 );
        data.put( String.valueOf(i0)+"_"+String.valueOf(i1)+"_"+String.valueOf(i2)+"_"+String.valueOf(i3), d );
    }

    @Override
    public double value() { // not that this would be useful, but...
        if ( rank!=0 ) throw new IllegalArgumentException( "rank mismatch, data is rank "+rank );
        Double v= data.get("");
        return v==null ? 0 : v;
    }

    @Override
    public double value(int i0) {
        if ( rank!=1 ) throw new IllegalArgumentException( "rank mismatch, data is rank "+rank );
        Double v= data.get( String.valueOf(i0) );
        return v==null ? 0 : v;
    }

    @Override
    public double value(int i0, int i1) {
        if ( rank!=2 ) throw new IllegalArgumentException( "rank mismatch, data is rank "+rank );
        Double v= data.get( String.valueOf(i0)+"_"+String.valueOf(i1) );
        return v==null ? 0 : v;
    }

    @Override
    public double value(int i0, int i1, int i2) {
        if ( rank!=3 ) throw new IllegalArgumentException( "rank mismatch, data is rank "+rank );
        Double v= data.get( String.valueOf(i0)+"_"+String.valueOf(i1)+"_"+String.valueOf(i2) );
        return v==null ? 0 : v;
    }

    @Override
    public double value(int i0, int i1, int i2, int i3) {
        if ( rank!=4 ) throw new IllegalArgumentException( "rank mismatch, data is rank "+rank );
        Double v= data.get( String.valueOf(i0)+"_"+String.valueOf(i1)+"_"+String.valueOf(i2)+"_"+String.valueOf(i3) ); 
        return v==null ? 0 : v;
    }
    
}
