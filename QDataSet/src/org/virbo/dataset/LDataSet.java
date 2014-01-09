/*
 * LDataSet.java
 *
 * Created on April 24, 2007, 11:08 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.virbo.dataset;

import java.util.Map;

/**
 * rank 1,2,or 3 dataset backed by Long (8 byte) array.
 * Mutable datasets warning: No dataset should be mutable once it is accessible to the
 * rest of the system.  This would require clients make defensive copies which would 
 * seriously degrade performance.  
 *
 * @author jbf
 */
public final class LDataSet extends ArrayDataSet {

    long[] back;
    
    public static final String version="20070529";
    
    public static LDataSet createRank1( int len0 ) {
        return new LDataSet( 1, len0, 1, 1, 1 );
    }
    
    public static LDataSet createRank2( int len0, int len1 ) {
        return new LDataSet( 2, len0, len1, 1, 1 );
    }
    
    public static LDataSet createRank3( int len0, int len1, int len2 ) {
        return new LDataSet( 3, len0, len1, len2, 1 );
    }

    public static LDataSet createRank4( int len0, int len1, int len2, int len3 ) {
        return new LDataSet( 4, len0, len1, len2, len3);
    }
    
    /**
     * Wraps an array from array of dimension sizes.  The result will have
     * rank qube.length(). 
     * @param data array containing the data, with the last dimension contiguous in memory.
     * @param qube array specifying the rank and size of each dimension
     * @return IDataSet
     */
    public static LDataSet wrap( long[] data, int[] qube ) {
        if (qube.length == 1) {
            return new LDataSet( 1, qube[0], 1, 1, 1, data );
        } else if (qube.length == 2) {
            return new LDataSet( 2, qube[0], qube[1], 1, 1, data );
        } else if (qube.length == 3) {
            return new LDataSet( 3, qube[0], qube[1], qube[2], 1, data );
        } else if (qube.length == 4) {
            return new LDataSet( 4, qube[0], qube[1], qube[2], qube[3], data);
        } else if (qube.length == 0 ) {
            return new LDataSet( 0, 1, 1, 1, 1, data );
        } else {
            throw new IllegalArgumentException("bad qube");
        }
    }
    
    public static LDataSet wrap( long[] back, int rank, int len0, int len1, int len2, int len3 ) {
        return new LDataSet( rank, len0, len1, len2, len3, back );
    }
    
    protected LDataSet( int rank, int len0, int len1, int len2, int len3 ) {
        this( rank, len0, len1, len2, len3, new long[ len0 * len1 * len2 * len3] );
    }

    protected LDataSet( int rank, int len0, int len1, int len2, int len3, long[] back ) {
       this.back= back;
       this.rank= rank;
       this.len0= len0;
       this.len1= len1;
       this.len2= len2;
       this.len3= len3;
       if ( this.back.length < len0 * len1 * len2 * len3 ) {
           logger.warning("backing array appears to be too short");
       }
       if ( rank>1 ) putProperty(QDataSet.QUBE, Boolean.TRUE);
    }

    protected Object getBack() {
        checkImmutable();
        return this.back;
    }

    protected void setBack(Object back) {
        checkImmutable();
        this.back= (long[])back;
    }

    @Override
    public double value() {
        return back[0];
    }

    @Override
    public double value(int i0) {
        return back[ i0 ];
    }    

    @Override
    public double value(int i0, int i1) {
        return back[ i0 * len1 + i1 ];
    }    
    
    @Override
    public double value(int i0, int i1, int i2 ) {
        return back[ i0 * len1 * len2 + i1 *len2 + i2 ];
    }

    @Override
    public double value(int i0, int i1, int i2, int i3) {
        return back[ i0*len1*len2*len3 + i1*len2*len3 + i2*len3 + i3];
    }

    public void putValue( double value ) {
        checkImmutable();        
        back[0]= (long) value;
    }

    public void putValue( int i0, double value ) {
        checkImmutable();        
        back[ i0 ]= (long)value;
    }

    public void putValue( int i0, int i1, double value ) {
        checkImmutable();        
        back[  i0 * len1 + i1 ]= (long)value;
    }

    public void putValue( int i0, int i1, int i2, double value ) {
        checkImmutable();        
        back[ i0 * len1 * len2 + i1 *len2 + i2  ]= (long)value;
    }

    public void putValue( int i0, int i1, int i2, int i3, double value ) {
        checkImmutable();        
        back[ i0*len1*len2*len3 + i1*len2*len3 + i2*len3 +i3 ] = (long)value;
    }


    /**
     * Shorten the dataset by changing it's dim 0 length parameter.  The same backing array is used, 
     * so the element that remain ill be the same.
     * can only shorten!
     */
    public void putLength( int len ) {
        checkImmutable();        
        if ( len>len0 ) throw new IllegalArgumentException("dataset cannot be lengthened");
        len0= len;
    }

    /**
     * creates a rank1 IDataSet by wrapping an existing array.
     */
    public static LDataSet wrap( long[] back ) {
        return new LDataSet( 1, back.length, 1, 1, 1, back );
    }
    
    /**
     * creates a IDataSet by wrapping an existing array, aliasing it to rank 2.
     */
    public static LDataSet wrap( long[] back, int nx, int ny ) {
        return new LDataSet( 2, nx, ny, 1, 1, back );
    }

    /**
     * creates a DataSet by wrapping an existing array, aliasing it to rank 3.
     */
    public static LDataSet wrap( long[] back, int nx, int ny, int nz ) {
        return new LDataSet( 3, nx, ny, nz, 1, back );
    }


    /**
     * the slice operator is better implemented here.  Presently, we
     * use System.arraycopy to copy out the data, but this should be
     * reimplemented along with an offset parameter so the original data
     * can be used to back the data.
     * @param i
     * @return
     */
    @Override
    public QDataSet slice(int i) {
        int nrank = this.rank-1;
        int noff1= i * len1 * len2 * len3;
        int noff2= (i+1) * len1 * len2 * len3;
        long[] newback = new long[noff2-noff1];
        System.arraycopy( this.back, noff1, newback, 0, noff2-noff1 );
        Map<String,Object> props= DataSetOps.sliceProperties0(i,DataSetUtil.getProperties(this));
        props= DataSetUtil.sliceProperties( this, i, props );
        LDataSet result= new LDataSet( nrank, len1, len2, len3, 1, newback );
        DataSetUtil.putProperties( props, result );
        return result;
    }

    /**
     * trim operator copies the data into a new dataset.
     * @param start
     * @param end
     * @return
     */
    @Override
    public QDataSet trim(int start, int end) {
        int nrank = this.rank;
        int noff1= start * len1 * len2 * len3;
        int noff2= end * len1 * len2 * len3;
        long[] newback = new long[noff2-noff1];
        System.arraycopy( this.back, noff1, newback, 0, noff2-noff1 );
        LDataSet result= new LDataSet( nrank, end-start, len1, len2, len3, newback );
        Map<String,Object> props= DataSetUtil.getProperties(this);
        Map<String,Object> depProps= DataSetUtil.trimProperties( this, start, end );
        props.putAll(depProps);
        DataSetUtil.putProperties( props, result );

        return result;
    }

    /**
     * TODO: this is untested, but is left in to demonstrate how the capability
     * method should be implemented.  Clients should use this instead of
     * casting the class to the capability class.
     * @param <T>
     * @param clazz
     * @return
     */
    @Override
    public <T> T capability(Class<T> clazz) {
        if ( clazz==WritableDataSet.class ) {
            if ( isImmutable() ) {
                return null;
            } else {
                return (T) this;
            }
        } else {
            return super.capability(clazz);
        }
    }

}
