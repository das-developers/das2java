/*
 * DDataSet.java
 *
 * Created on April 24, 2007, 11:08 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.virbo.dataset;

import java.util.Map;
import org.das2.datum.Units;

/**
 * rank 1,2,or 3 dataset backed by double array. 
 * Mutable datasets warning: No dataset should be mutable once it is accessible to the
 * rest of the system.  This would require clients make defensive copies which would 
 * seriously degrade performance.  
 *
 * @author jbf
 */
public final class DDataSet extends ArrayDataSet {

    double[] back;

    private static final boolean RANGE_CHECK = false;
    public static final String version = "20110217";

    public static DDataSet createRank1(int len0) {
        return new DDataSet(1, len0, 1, 1, 1);
    }

    public static DDataSet createRank2(int len0, int len1) {
        return new DDataSet(2, len0, len1, 1, 1);
    }

    public static DDataSet createRank3(int len0, int len1, int len2) {
        return new DDataSet(3, len0, len1, len2, 1);
    }

    public static DDataSet createRank4(int len0, int len1, int len2, int len3) {
        return new DDataSet(4, len0, len1, len2, len3);
    }

    /**
     * Makes an array from array of dimension sizes.  The result will have
     * rank qube.length(). 
     * @param qube array specifying the rank and size of each dimension
     * @return DDataSet
     */
    public static DDataSet create(int[] qube) {
        if ( qube.length==0 ) {
            return new DDataSet( 0, 1, 1, 1, 1 );
        } else if (qube.length == 1) {
            return DDataSet.createRank1(qube[0]);
        } else if (qube.length == 2) {
            return DDataSet.createRank2(qube[0], qube[1]);
        } else if (qube.length == 3) {
            return DDataSet.createRank3(qube[0], qube[1], qube[2]);
        } else if (qube.length == 4) {
            return DDataSet.createRank4(qube[0], qube[1], qube[2], qube[3]);
        } else if (qube.length == 0 ) {
            return new DDataSet( 0, 1, 1, 1, 1 );
        } else {
            throw new IllegalArgumentException("bad qube");
        }
    }

    /**
     * Wraps an array from array of dimension sizes.  The result will have
     * rank qube.length(). 
     * @param data array containing the data, with the last dimension contiguous in memory.
     *    for rank 0, data is 1-element array.
     * @param qube array specifying the rank and size of each dimension
     * @return DDataSet
     */
    public static DDataSet wrap( double[] data, int[] qube ) {
        if (qube.length == 1) {
            return new DDataSet( 1, qube[0], 1, 1, 1, data );
        } else if (qube.length == 2) {
            return new DDataSet( 2, qube[0], qube[1], 1, 1, data );
        } else if (qube.length == 3) {
            return new DDataSet( 3, qube[0], qube[1], qube[2], 1, data );
        } else if (qube.length == 4) {
            return new DDataSet( 4, qube[0], qube[1], qube[2], qube[3], data);
        } else if (qube.length == 0 ) {
            // we support rank 0 with a 1-element array.
            return new DDataSet( 0, 1, 1, 1, 1, data );
        } else {
            throw new IllegalArgumentException("bad qube");
        }
    }

        
    protected DDataSet(int rank, int len0, int len1, int len2, int len3) {
        this(rank, len0, len1, len2, len3, new double[len0 * len1 * len2 * len3]);
    }

    protected DDataSet(int rank, int len0, int len1, int len2, int len3, double[] back) {
        if ( back==null ) throw new NullPointerException("back was null");
        this.back = back;
        this.rank = rank;
        this.len0 = len0;
        this.len1 = len1;
        this.len2 = len2;
        this.len3 = len3;
        if ( rank>1 ) putProperty(QDataSet.QUBE, Boolean.TRUE);
    }

    protected Object getBack() {
        return this.back;
    }

    protected void setBack(Object back) {
        this.back= (double[])back;
    }

    @Override
    public double value() {
        if ( RANGE_CHECK ) {
            if ( this.rank!=0 ) {
                throw new IllegalArgumentException("rank 0 access on rank "+this.rank+" dataset");
            }
        }
        return back[0];
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
        return back[i0];
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
        return back[i0 * len1 + i1];
    }

    @Override
    public double value(int i0, int i1, int i2) {
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
        return back[i0 * len1 * len2 + i1 * len2 + i2];
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
        return back[i0*len1*len2*len3 + i1*len2*len3 + i2*len3 +i3];
    }

    public void putValue(double value) {
        back[0]= value;
    }

    public void putValue(int i0, double value) {
        if (RANGE_CHECK) {
            if (i0 < 0 || i0 >= len0) {
                throw new IndexOutOfBoundsException("i0=" + i0 + " " + this);
            }
        }
        back[i0] = value;
    }

    public void putValue(int i0, int i1, double value) {
        if (RANGE_CHECK) {
            if (i0 < 0 || i0 >= len0) {
                throw new IndexOutOfBoundsException("i0=" + i0 + " " + this);
            }
            if (i1 < 0 || i1 >= len1) {
                throw new IndexOutOfBoundsException("i1=" + i1 + " " + this);
            }
        }
        back[i0 * len1 + i1] = value;
    }

    public void putValue(int i0, int i1, int i2, double value) {
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
        back[i0 * len1 * len2 + i1 * len2 + i2] = value;
    }

    public void putValue(int i0, int i1, int i2, int i3, double value) {
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
        back[ i0 * len1 * len2 * len3 + i1 * len2 * len3 + i2 * len3 + i3 ] = value;
    }

    /**
     * Shorten the dataset by changing it's dim 0 length parameter.  The same backing array is used, 
     * so the element that remain will be the same.
     * This can only shorten!
     */
    public void putLength(int len) {
        if (len > len0) {
            throw new IllegalArgumentException("dataset cannot be lengthened");
        }
        len0 = len;
    }

    @Override
    public String toString() {
        return DataSetUtil.toString(this);
    }

    /**
     * creates a DDataSet by wrapping an existing double array.
     */
    public static DDataSet wrap(double[] back) {
        return new DDataSet(1, back.length, 1, 1, 1, back);
    }

    /**
     * useful create with units.
     * @param xx
     * @param xunits
     * @return
     */
    public static DDataSet wrap(double[] xx, Units xunits) {
        DDataSet result= wrap( xx );
        result.putProperty( QDataSet.UNITS, xunits );
        return result;
    }


    /**
     * creates a DDataSet by wrapping an existing array, and aliasing it to rank2.
     * Note the last index is packed closest in memory.
     * @param n1 the size of the second dimension.
     */
    public static DDataSet wrapRank2(double[] back, int n1) {
        return new DDataSet(2, back.length / n1, n1, 1, 1, back);
    }

    /**
     * creates a DDataSet by wrapping an existing array, and aliasing it to rank2.
     * Note the last index is packed closest in memory.  The first index length
     * is calculated from the size of the array.
     * @param n1 the size of the second index.
     * @param n2 the size of the third index.
     */
    public static DDataSet wrapRank3(double[] back, int n1, int n2) {
        return new DDataSet(3, back.length / (n1 * n2), n1, n2, 1, back);
    }

    /**
     * creates a DDataSet by wrapping an existing array, aliasing it to rank 2.
     */
    public static DDataSet wrap(double[] back, int nx, int ny) {
        return new DDataSet(2, nx, ny, 1, 1, back);
    }

    public static DDataSet wrap( double[] back, int rank, int len0, int len1, int len2 ) {
        return new DDataSet( rank, len0, len1, len2, 1, back );
    }

    public static DDataSet wrap( double[] back, int rank, int len0, int len1, int len2, int len3) {
        return new DDataSet( rank, len0, len1, len2, len3, back);
    }
    

    /** 
     * convenient method for setting the units.
     */
    public DDataSet setUnits( Units units ) {
        properties.put(QDataSet.UNITS, units);
        return this;
    }

    /**
     * copy elements of src DDataSet into dest DDataSet, with System.arraycopy.
     * src and dst must have the same geometry, except for dim 0.  Allows for
     * aliasing when higher dimension element count matches.
     * @param len number of records to copy.
     * @throws IllegalArgumentException if the higher rank geometry doesn't match
     * @throws IndexOutOfBoundsException
     */
    public static void copyElements(DDataSet src, int srcpos, DDataSet dest, int destpos, int len) {
        if ( src.len1 != dest.len1 || src.len2 != dest.len2 ) {
            throw new IllegalArgumentException("src and dest geometry don't match");
        }
        copyElements( src, srcpos, dest, destpos, len * src.len1 * src.len2, false); 
    }    
    
    /**
     * copy elements of src DDataSet into dest DDataSet, with System.arraycopy.
     * src and dst must have the same geometry, except for dim 0.  Allows for
     * aliasing when higher dimension element count matches.
     * @param src source dataset
     * @param srcpos source dataset first dimension index.
     * @param dest destination dataset
     * @param destpos destination dataset first dimension index.
     * @param len total number of elements to copy
     * @param checkAlias bounds for aliased write (same number of elements, different geometry.)
     * @throws IllegalArgumentException if the higher rank geometry doesn't match
     * @throws IndexOutOfBoundsException
     */
    public static void copyElements( DDataSet src, int srcpos, DDataSet dest, int destpos, int len, boolean checkAlias ) {
        if ( checkAlias && ( src.len1*src.len2 != dest.len1*dest.len2 ) ) {
            throw new IllegalArgumentException("src and dest geometry don't match");
        }
        int srcpos1 = srcpos * src.len1 * src.len2;
        int destpos1 = destpos * dest.len1 * dest.len2;
        int len1 = len;
        System.arraycopy( src.back, srcpos1, dest.back, destpos1, len1 );
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
        double[] newback = new double[noff2-noff1];
        System.arraycopy( this.back, noff1, newback, 0, noff2-noff1 );
        Map<String,Object> props= DataSetOps.sliceProperties0(i,DataSetUtil.getProperties(this));
        props= DataSetUtil.sliceProperties( this, i, props );
        DDataSet result= new DDataSet( nrank, len1, len2, len3, 1, newback );
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
        double[] newback = new double[noff2-noff1];
        System.arraycopy( this.back, noff1, newback, 0, noff2-noff1 );
        DDataSet result= new DDataSet( nrank, end-start, len1, len2, len3, newback );
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
