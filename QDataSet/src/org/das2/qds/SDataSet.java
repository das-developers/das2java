/*
 * SDataSet.java
 *
 * Created on April 24, 2007, 11:08 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.qds;

import java.lang.reflect.Array;
import java.util.Map;

/**
 * rank 0,1,2,3 or 4 dataset backed by short array (2 byte signed numbers).
 * Note access to the array is still done via doubles.
 *
 * @author jbf
 */
public final class SDataSet extends ArrayDataSet {

    short[] back;
      
    private static final boolean RANGE_CHECK = "true".equals( System.getProperty("rangeChecking","true") );
    
    public static final String version="20150219";
    
    /**
     * create a rank 1 dataset backed by array of shorts.
     * @param len0 length of the dimension
     * @return rank 1 qube dataset of backed by array of shorts.
     */
    public static SDataSet createRank1( int len0 ) {
        return new SDataSet( 1, len0, 1, 1, 1 );
    }
    
    /**
     * create a rank 2 qube dataset backed by array of shorts.
     * @param len0 length of the dimension
     * @param len1 length of the dimension
     * @return rank 2 qube dataset of backed by array of shorts.
     */
    public static SDataSet createRank2( int len0, int len1 ) {
        return new SDataSet( 2, len0, len1, 1, 1 );
    }
    
    /**
     * create a rank 3 qube dataset backed by array of shorts.
     * @param len0 length of the dimension
     * @param len1 length of the dimension
     * @param len2 length of the dimension
     * @return rank 3 qube dataset of backed by array of shorts.
     */
    public static SDataSet createRank3( int len0, int len1, int len2 ) {
        return new SDataSet( 3, len0, len1, len2, 1 );
    }

    /**
     * create a rank 4 qube dataset backed by array of shorts.
     * @param len0 length of the dimension
     * @param len1 length of the dimension
     * @param len2 length of the dimension
     * @param len3 length of the dimension
     * @return rank 4 qube dataset of backed by array of shorts.
     */
    public static SDataSet createRank4( int len0, int len1, int len2, int len3) {
        return new SDataSet( 4, len0, len1, len2, len3 );
    }
    
    /**
     * Makes an array from array of dimension sizes.  The result will have
     * rank qube.length().
     * @param qube array specifying the rank and size of each dimension
     * @return the array as a QDataSet
     */
    public static SDataSet create(int[] qube) {
        switch (qube.length) {
            case 0:
                return new SDataSet( 0, 1, 1, 1, 1 );
            case 1:
                return SDataSet.createRank1(qube[0]);
            case 2:
                return SDataSet.createRank2(qube[0], qube[1]);
            case 3:
                return SDataSet.createRank3(qube[0], qube[1], qube[2]);
            case 4:
                return SDataSet.createRank4(qube[0], qube[1], qube[2], qube[3]);
            default:
                throw new IllegalArgumentException("bad qube");
        }
    }
    
    /**
     * Wraps an array from array of dimension sizes.  The result will have
     * rank qube.length().  For rank 0, data is 1-element array.
     * @param data array containing the data, with the last dimension contiguous in memory.
     * @param qube array specifying the rank and size of each dimension
     * @return the array as a QDataSet
     */
    public static SDataSet wrap( short[] data, int[] qube ) {
        switch (qube.length) {
            case 1:
                return new SDataSet( 1, qube[0], 1, 1, 1, data );
            case 2:
                return new SDataSet( 2, qube[0], qube[1], 1, 1, data );
            case 3:
                return new SDataSet( 3, qube[0], qube[1], qube[2], 1, data );
            case 4:
                return new SDataSet( 4, qube[0], qube[1], qube[2], qube[3], data );
            case 0:
                return new SDataSet( 0, 1, 1, 1, 1, data );
            default:
                throw new IllegalArgumentException("bad qube");
        }
    }
    
    /**
     * Wraps an array from array of dimension sizes.  The result will have
     * rank qube.length(). 
     * @param back the backing array
     * @param rank the rank
     * @param len0 length of the dimension
     * @param len1 length of the dimension
     * @param len2 length of the dimension
     * @return the array as a QDataSet
     */
    public static SDataSet wrap( short[] back, int rank, int len0, int len1, int len2 ) {
        return new SDataSet( rank, len0, len1, len2, 1, back );
    }
    
    /**
     * Wraps an array from array of dimension sizes.  The result will have
     * rank qube.length(). 
     * @param back the backing array
     * @param rank the rank
     * @param len0 length of the dimension
     * @param len1 length of the dimension
     * @param len2 length of the dimension
     * @param len3 length of the dimension
     * @return the array as a QDataSet
     */
    public static SDataSet wrap( short[] back, int rank, int len0, int len1, int len2, int len3 ) {
        return new SDataSet( rank, len0, len1, len2, len3, back );
    }    
    
    protected SDataSet( int rank, int len0, int len1, int len2, int len3 ) {
        this( rank, len0, len1, len2, len3, new short[ len0 * len1 * len2 * len3] );
    }

    protected SDataSet( int rank, int len0, int len1, int len2, int len3, short[] back ) {
        super( short.class );
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

    @Override
    protected Object getBack() {
        checkImmutable();
        return this.back;
    }

    @Override
    protected int getBackJvmMemory() {
        return this.back.length * 2;
    }
    

    @Override
    protected Object getBackReadOnly() {
        return this.back;
    }
    
    @Override
    protected Object getBackCopy() {
        Object newback = Array.newInstance( this.back.getClass().getComponentType(), this.back.length  );
        System.arraycopy( this.back, 0, newback, 0, this.back.length );
        return newback;
    }

    @Override
    protected void setBack(Object back) {
        checkImmutable();
        this.back= (short[])back;
    }

    @Override
    public double value() {
        if ( RANGE_CHECK ) {
            if ( this.rank!=0 ) {
                throw new IllegalArgumentException("rank 0 access on rank "+this.rank+" dataset");
            }
        }
        return back[ 0 ];
    }

    @Override
    public double value(int i0) {
        if (RANGE_CHECK) {
            if ( this.rank!=1 ) {
                throw new IllegalArgumentException("rank 1 access on rank "+this.rank+" dataset");
            }
            if (i0 < 0 || i0 >= len0) {
                throw new IndexOutOfBoundsException("i0=" + i0 + " " + this);
            }
        }
        return back[ i0 ];
    }    

    @Override
    public double value(int i0, int i1) {
        if (RANGE_CHECK) {
            if ( this.rank!=2 ) {
                throw new IllegalArgumentException("rank 2 access on rank "+this.rank+" dataset");
            }
            if (i0 < 0 || i0 >= len0) {
                throw new IndexOutOfBoundsException("i0=" + i0 + " " + this);
            }
            if (i1 < 0 || i1 >= len1) {
                throw new IndexOutOfBoundsException("i1=" + i1 + " " + this);
            }
        }
        return back[ i0 * len1 + i1 ];
    }    
    
    @Override
    public double value(int i0, int i1, int i2 ) {
        if (RANGE_CHECK) {
            if ( this.rank!=3 ) {
                throw new IllegalArgumentException("rank 3 access on rank "+this.rank+" dataset");
            }
            if (i0 < 0 || i0 >= len0) {
                throw new IndexOutOfBoundsException("i0=" + i0 + " " + this);
            }
            if (i1 < 0 || i1 >= len1) {
                throw new IndexOutOfBoundsException("i1=" + i1 + " " + this);
            }
            if (i2 < 0 || i2 >= len2) {
                throw new IndexOutOfBoundsException("i2=" + i2 + " " + this);
            }
        }
        return back[ i0 * len1 * len2 + i1 *len2 + i2 ];
    }

    @Override
    public double value(int i0, int i1, int i2, int i3) {
        if (RANGE_CHECK) {
            if ( this.rank!=4 ) {
                throw new IllegalArgumentException("rank 4 access on rank "+this.rank+" dataset");
            }
            if (i0 < 0 || i0 >= len0) {
                throw new IndexOutOfBoundsException("i0=" + i0 + " " + this);
            }
            if (i1 < 0 || i1 >= len1) {
                throw new IndexOutOfBoundsException("i1=" + i1 + " " + this);
            }
            if (i2 < 0 || i2 >= len2) {
                throw new IndexOutOfBoundsException("i2=" + i2 + " " + this);
            }
            if (i3 < 0 || i3 >= len3) {
                throw new IndexOutOfBoundsException("i3=" + i3 + " " + this);
            }
        }
        return back[ i0*len1*len2*len3 + i1*len2*len3 + i2*len3 + i3 ];
    }

    @Override
    public void putValue( double value ) {
        checkImmutable();
        back[0]= (short)value;
    }
    
    @Override
    public void putValue( int i0, double value ) {
        checkImmutable();
        if (RANGE_CHECK) {
            if (i0 < 0 || i0 >= len0) {
                throw new IndexOutOfBoundsException("i0=" + i0 + " " + this);
            }
        }        
        back[ i0 ]= (short)value;
    }

    @Override
    public void putValue( int i0, int i1, double value ) {
        checkImmutable();
        if (RANGE_CHECK) {
            if (i0 < 0 || i0 >= len0) {
                throw new IndexOutOfBoundsException("i0=" + i0 + " " + this);
            }
            if (i1 < 0 || i1 >= len1) {
                throw new IndexOutOfBoundsException("i1=" + i1 + " " + this);
            }
        }        
        back[  i0 * len1 + i1 ]= (short)value;
    }

    @Override
    public void putValue( int i0, int i1, int i2, double value ) {
        checkImmutable();
        if (RANGE_CHECK) {
            if (i0 < 0 || i0 >= len0) {
                throw new IndexOutOfBoundsException("i0=" + i0 + " " + this);
            }
            if (i1 < 0 || i1 >= len1) {
                throw new IndexOutOfBoundsException("i1=" + i1 + " " + this);
            }
            if (i2 < 0 || i2 >= len2) {
                throw new IndexOutOfBoundsException("i2=" + i2 + " " + this);
            }
        }        
        back[ i0 * len1 * len2 + i1 *len2 + i2  ]= (short)value;
    }

    @Override
    public void putValue( int i0, int i1, int i2, int i3, double value ) {
        checkImmutable();
        if (RANGE_CHECK) {
            if (i0 < 0 || i0 >= len0) {
                throw new IndexOutOfBoundsException("i0=" + i0 + " " + this);
            }
            if (i1 < 0 || i1 >= len1) {
                throw new IndexOutOfBoundsException("i1=" + i1 + " " + this);
            }
            if (i2 < 0 || i2 >= len2) {
                throw new IndexOutOfBoundsException("i2=" + i2 + " " + this);
            }
            if (i3 < 0 || i3 >= len3) {
                throw new IndexOutOfBoundsException("i3=" + i3 + " " + this);
            }
        }        
        back[ i0*len1*len2*len3 + i1*len2*len3 + i2*len3 + i3] = (short)value;
    }
    
    /**
     * add this value to the current value. 
     * @param i0 the index
     * @param value the value, which is cast to this internal type.
     */
    public void addValue( int i0, double value ) {
        checkImmutable();
        back[ i0 ]+= (short)value;
    }

    /**
     * add this value to the current value. 
     * @param i0 the index
     * @param i1 the index
     * @param value the value, which is cast to this internal type.
     */
    public void addValue( int i0, int i1, double value ) {
        checkImmutable();
        back[  i0 * len1 + i1 ]+= (short)value;
    }
    
    /**
     * creates a rank1 SDataSet by wrapping an existing array.
     * @param back the new backing array
     * @return the dataset
     */
    public static SDataSet wrap( short[] back ) {
        return new SDataSet( 1, back.length, 1, 1, 1, back );
    }
    
    /**
     * creates a SDataSet by wrapping an existing array, aliasing it to rank 2.
     * @param back the new backing array
     * @param nx number of elements in the zeroth index
     * @param ny number of elements in the first index
     * @return the dataset
     */
    public static SDataSet wrap( short[] back, int nx, int ny ) {
        return new SDataSet( 2, nx, ny, 1, 1, back );
    }
        
    /**
     * creates a DataSet by wrapping an existing array, aliasing it to rank 3.
     * @param back the new backing array
     * @param nx number of elements in the zeroth index
     * @param ny number of elements in the first index
     * @param nz number of elements in the second index
     * @return the dataset
     */
    public static SDataSet wrap( short[] back, int nx, int ny, int nz ) {
        return new SDataSet( 3, nx, ny, nz, 1, back );
    }
    
    /**
     * the slice operator is better implemented here.  Presently, we
     * use System.arraycopy to copy out the data, but this could be
     * re-implemented along with an offset parameter so the original data
     * can be used to back the data.
     * @param i the index
     * @return a rank N-1 slice of the data.
     */
    @Override
    public QDataSet slice(int i) {
        if ( this.rank<1 ) {
            throw new IllegalArgumentException("slice called on rank 0 dataset");
        }        
        int nrank = this.rank-1;
        int noff1= i * len1 * len2 * len3;
        int noff2= (i+1) * len1 * len2 * len3;
        short[] newback = new short[noff2-noff1];
        System.arraycopy( this.back, noff1, newback, 0, noff2-noff1 );
        Map<String,Object> props= DataSetOps.sliceProperties0(i,DataSetUtil.getProperties(this));
        props= DataSetUtil.sliceProperties( this, i, props );
        SDataSet result= new SDataSet( nrank, len1, len2, len3, 1, newback );
        DataSetUtil.putProperties( props, result );
        return result;
    }

    /**
     * trim operator copies the data into a new dataset.
     * @param start the first index
     * @param end the last index, exclusive
     * @return a shorter dataset of the same rank.
     */
    @Override
    public QDataSet trim(int start, int end) {
        if ( rank==0 ) {
            throw new IllegalArgumentException("trim called on rank 0 dataset");
        }
        if ( start==0 && end==len0 ) {
            return this;
        }
        if ( RANGE_CHECK ) {
            if ( start>len0 ) throw new IndexOutOfBoundsException("start="+start+" > "+len0 );
            if ( start<0 ) throw new IndexOutOfBoundsException("start="+start+" < 0");
            if ( end>len0 ) throw new IndexOutOfBoundsException("end="+end+" > "+len0 );
            if ( end<0 ) throw new IndexOutOfBoundsException("end="+end+" < 0");
            if ( start>end ) throw new IllegalArgumentException("trim called with start>end: "+start +">"+end);
        }
        int nrank = this.rank;
        int noff1= start * len1 * len2 * len3;
        int noff2= end * len1 * len2 * len3;
        short[] newback = new short[noff2-noff1];
        if ( noff2-noff1>0 ) {
            System.arraycopy( this.back, noff1, newback, 0, noff2-noff1 );
        }
        SDataSet result= new SDataSet( nrank, end-start, len1, len2, len3, newback );
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
     * @param clazz the class, such as WritableDataSet.class
     * @return null or the capability if exists, such as WritableDataSet
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
