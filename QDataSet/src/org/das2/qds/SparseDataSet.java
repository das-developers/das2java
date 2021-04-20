/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.qds;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * DataSet for storing sparse data.  This is used initially to describe bundles.
 * This returns 0 where no data has been set.  For example,
 *<blockquote><pre>
 *sp= SparseDataSet.createQube([2,4])
 *sp[2,2]= 1
 *print sp[0,0]
 *</pre></blockquote>
 * @author jbf
 */
public class SparseDataSet extends AbstractDataSet implements WritableDataSet {

    int rank;
    Map<String,Double> data;
    int length0=-1;
    int[] qube;
    Map<String,Integer> length;
    
    /**
     * this is private because calling the constructor from Jython won't adapt to PyQDataSet.
     * @param rank 
     */
    private SparseDataSet( int rank ) {
        this.rank= rank;
        data= new HashMap<>();
        length= new HashMap<>();
    }
    
    /**
     * create the dataset with the given rank.  The length of any dimension is explicitly set with the setLength
     * method or implicitly by the highest index assigned a value.  Note Jython scripts are unable to call the setLength method.
     * @param rank number of indeces
     * @return a dataset that is empty.
     * @see #createRankLen(int, int) 
     * @see #createQube(int[]) 
     */
    public static SparseDataSet createRank( int rank ) {
        return new SparseDataSet(rank);
    }

    /**
     * create the dataset with the given rank and initial length.  This
     * was introduced because setLength cannot be called from Jython scripts.
     * Each record will have length(i) based on the highest index assigned.
     * @param rank the result's rank.
     * @param len0 the number of records in the result.  
     * @see #createQube(int[]) 
     * @return SparseDataSet with the given rank.
     */
    public static SparseDataSet createRankLen( int rank, int len0 ) {
        SparseDataSet result= new SparseDataSet(rank);
        result.setLength(len0);
        return result;
    }

    /**
     * create the qube dataset with the given dimensions.  This
     * was introduced because setQube cannot be called from Jython scripts.
     * @param qube the index dimensions.
     * @return SparseDataSet with the given rank and dimensions based on qube.
     */
    public static SparseDataSet createQube( int[] qube ) {
        SparseDataSet result= new SparseDataSet(qube.length);
        result.setQube(qube);
        return result;
    }
    
    /**
     * set the length of the zeroth dimension.  Other dimensions have length set implicitly by the highest value set.
     * If this is not set explicitly, then it will be implicit as well.
     * @param length 
     */
    public void setLength( int length ) {
        checkImmutable();
        this.length0= length;
    }
    
    /**
     * make this a qube dataset, where all the lengths are the same.
     * @param qube 
     */
    public void setQube( int[] qube ) {
        if ( qube.length!=this.rank ) {
            throw new IllegalArgumentException("qube length must match rank: "+this.rank );
        }
        this.qube= Arrays.copyOf( qube,qube.length );
        this.length0= qube[0];
    }
    
    @Override
    public int rank() {
        return rank;
    }

    @Override
    public int length() {
        if ( length0<0 ) {
            throw new IllegalArgumentException("length of SparseDataSet was never set");
        }
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
        checkImmutable();
        data.put( "", d );
    }

    public void putValue(int i0, double d) {
        checkImmutable();
        if ( qube==null ) {
            length0= Math.max( i0+1, length0 );
        }
        data.put( String.valueOf(i0), d );
    }

    public void putValue(int i0, int i1, double d) {
        checkImmutable();
        if ( qube==null ) {
            length0= Math.max( i0+1, length0 );
            Integer length1= length.get( String.valueOf(i0) );
            if ( length1==null || length1<=i1 ) {
                length1= i1+1;
                length.put( String.valueOf(i0), length1 );
            }
        }
        data.put( String.valueOf(i0)+"_"+String.valueOf(i1), d );
    }

    public void putValue(int i0, int i1, int i2, double d) {
        checkImmutable();
        if ( qube==null ) {
            length0= Math.max( i0+1, length0 );
            Integer length1= length.get( String.valueOf(i0) );
            if ( length1==null || length1<=i1 ) {
                length1= i1+1;
                length.put( String.valueOf(i0), length1 );
            }
            Integer length2= length.get( String.valueOf(i0)+"_"+String.valueOf(i1) );
            if ( length2==null || length2<=i1 ) {
                length2= i2+1;
                length.put( String.valueOf(i0)+"_"+String.valueOf(i1), length2 );
            }
        }
        data.put( String.valueOf(i0)+"_"+String.valueOf(i1)+"_"+String.valueOf(i2), d );
    }

    public void putValue(int i0, int i1, int i2, int i3, double d) {
        checkImmutable();
        if ( qube==null ) {
            length0= Math.max( i0+1, length0 );
            Integer length1= length.get( String.valueOf(i0) );
            if ( length1==null || length1<=i1 ) {
                length1= i1+1;
                length.put( String.valueOf(i0), length1 );
            }
            Integer length2= length.get( String.valueOf(i0)+"_"+String.valueOf(i1) );
            if ( length2==null || length2<=i1 ) {
                length2= i2+1;
                length.put( String.valueOf(i0)+"_"+String.valueOf(i1), length2 );
            }        
            Integer length3= length.get( String.valueOf(i0)+"_"+String.valueOf(i1)+"_"+String.valueOf(i2) );
            if ( length3==null || length3<=i1 ) {
                length3= i3+1;
                length.put( String.valueOf(i0)+"_"+String.valueOf(i1)+"_"+String.valueOf(i2), length3 );
            }        
        }
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
