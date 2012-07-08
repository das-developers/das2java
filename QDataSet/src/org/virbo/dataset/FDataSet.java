/*
 * FDataSet.java
 *
 * Created on April 24, 2007, 11:08 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.virbo.dataset;

import java.util.Map;

/**
 * rank 1,2,or 3 dataset backed by double array. 
 * Mutable datasets warning: No dataset should be mutable once it is accessible to the
 * rest of the system.  This would require clients make defensive copies which would 
 * seriously degrade performance.  
 *
 * @author jbf
 */
public final class FDataSet extends ArrayDataSet {
    float[] back;

    private static final boolean RANGE_CHECK = false;
    public static final String version="20120416";
    
    public static FDataSet createRank1( int len0 ) {
        return new FDataSet( 1, len0, 1, 1, 1 );
    }
    
    public static FDataSet createRank2( int len0, int len1 ) {
        return new FDataSet( 2, len0, len1, 1, 1 );
    }
    
    public static FDataSet createRank3( int len0, int len1, int len2 ) {
        return new FDataSet( 3, len0, len1, len2, 1 );
    }

    public static FDataSet createRank4( int len0, int len1, int len2, int len3 ) {
        return new FDataSet( 4, len0, len1, len2, len3);
    }
    /**
     * Makes an array from array of dimension sizes.  The result will have
     * rank qube.length(). 
     * @param qube array specifying the rank and size of each dimension
     * @return FDataSet
     */
    public static FDataSet create(int[] qube) {
        if (qube.length == 0) {
            return new FDataSet( 0, 1, 1, 1, 1 );
        } else if ( qube.length == 1 ) {
            return FDataSet.createRank1(qube[0]);
        } else if ( qube.length == 2 ) {
            return FDataSet.createRank2(qube[0], qube[1]);
        } else if ( qube.length == 3 ) {
            return FDataSet.createRank3(qube[0], qube[1], qube[2]);
        } else if ( qube.length == 4 ) {
            return FDataSet.createRank4(qube[0], qube[1], qube[2], qube[3]);
        } else {
            throw new IllegalArgumentException("bad qube");
        }
    }
    
    /**
     * Wraps an array from array of dimension sizes.  The result will have
     * rank qube.length(). 
     * @param data array containing the data, with the last dimension contiguous in memory.
     * @param qube array specifying the rank and size of each dimension
     * @return FDataSet
     */
    public static FDataSet wrap( float[] data, int[] qube ) {
        if (qube.length == 1) {
            return new FDataSet( 1, qube[0], 1, 1, 1, data );
        } else if (qube.length == 2) {
            return new FDataSet( 2, qube[0], qube[1], 1, 1, data );
        } else if (qube.length == 3) {
            return new FDataSet( 3, qube[0], qube[1], qube[2], 1, data );
        } else if (qube.length == 4) {
            return new FDataSet( 4, qube[0], qube[1], qube[2], qube[3], data);
        } else {
            throw new IllegalArgumentException("bad qube");
        }
    }
    
    public static FDataSet wrap( float[] back, int rank, int len0, int len1, int len2 ) {
        return new FDataSet( rank, len0, len1, len2, 1, back );
    }
    
    protected FDataSet( int rank, int len0, int len1, int len2, int len3 ) {
        this( rank, len0, len1, len2, len3, new float[ len0 * len1 * len2 * len3] );
    }

    protected FDataSet( int rank, int len0, int len1, int len2, int len3, float[] back ) {
       this.back= back;
       this.rank= rank;
       this.len0= len0;
       this.len1= len1;
       this.len2= len2;
       this.len3= len3;
       if ( rank>1 ) putProperty(QDataSet.QUBE, Boolean.TRUE);
    }

    protected Object getBack() {
        return this.back;
    }

    protected void setBack(Object back) {
        this.back= (float[])back;
    }

    @Override
    public double value() {
        if ( RANGE_CHECK ) {
            if ( this.rank!=0 ) {
                throw new IllegalArgumentException("rank 0 access on rank "+this.rank+" dataset");
            }
        }
        float v= back[0];
        return v==fill ? dfill : v;
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
        float v= back[i0];
        return v==fill ? dfill : v;
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
        float v= back[ i0 * len1 + i1 ];
        return v==fill ? dfill : v;
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

        float v= back[ i0 * len1 * len2 + i1 *len2 + i2 ];
        return v==fill ? dfill : v;
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
        float v=  back[ i0*len1*len2*len3 + i1*len2*len3 + i2*len3 +i3 ];
        return v==fill ? dfill : v;
    }

    public void putValue( double value ) {
        back[0]= (float)value;
    }
    
    public void putValue( int i0, double value ) {
        back[ i0 ]= (float)value;
    }

    public void putValue( int i0, int i1, double value ) {
        back[  i0 * len1 + i1 ]= (float)value;
    }

    public void putValue( int i0, int i1, int i2, double value ) {
        back[ i0 * len1 * len2 + i1 *len2 + i2  ]= (float)value;
    }

    public void putValue( int i0, int i1, int i2, int i3, double value ) {
        back[ i0*len1*len2*len3 + i1*len2*len3 +i2*len3 +i3] = (float)value;
    }
    
    /**
     * creates a rank1 FDataSet by wrapping an existing array.
     */
    public static FDataSet wrap( float[] back ) {
        return new FDataSet( 1, back.length, 1, 1, 1, back );
    }
    
    /**
     * creates a FDataSet by wrapping an existing array, aliasing it to rank 2.
     */
    public static FDataSet wrap( float[] back, int nx, int ny ) {
        return new FDataSet( 2, nx, ny, 1, 1, back );
    }
        
    /**
     * creates a FDataSet by wrapping an existing array, aliasing it to rank 3.
     */
    public static FDataSet wrap( float[] back, int nx, int ny, int nz ) {
        return new FDataSet( 3, nx, ny, nz, 1, back );
    }

    @Override
    public void putProperty(String name, Object value) {
        super.putProperty(name, value);
        if ( name.equals(QDataSet.FILL_VALUE) ) checkFill(); // because of rounding errors
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
        float[] newback = new float[noff2-noff1];
        System.arraycopy( this.back, noff1, newback, 0, noff2-noff1 );
        Map<String,Object> props= DataSetOps.sliceProperties0(i,DataSetUtil.getProperties(this));
        props= DataSetUtil.sliceProperties( this, i, props );
        FDataSet result= new FDataSet( nrank, len1, len2, len3, 1, newback );
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
        float[] newback = new float[noff2-noff1];
        System.arraycopy( this.back, noff1, newback, 0, noff2-noff1 );
        FDataSet result= new FDataSet( nrank, end-start, len1, len2, len3, newback );
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
            return (T) this;
        } else {
            return super.capability(clazz);
        }
    }

}
