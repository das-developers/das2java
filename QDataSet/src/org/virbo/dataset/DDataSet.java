/*
 * DDataSet.java
 *
 * Created on April 24, 2007, 11:08 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.virbo.dataset;

import java.lang.reflect.Array;
import java.util.Map;
import org.das2.datum.Units;

/**
 * rank 0,1,2,3 or 4 dataset backed by double array (8 byte real numbers).
 *
 * @author jbf
 */
public final class DDataSet extends ArrayDataSet {

    static long t0= System.currentTimeMillis();
    
    public long t= System.currentTimeMillis() - t0;
    
    double[] back;

    private static final boolean RANGE_CHECK = "true".equals( System.getProperty("rangeChecking","true") );
    
    public static final String version="20150219";

    /**
     * create a rank 1 dataset backed by array of doubles.
     * @return rank 0 dataset backed by double.
     */
    public static DDataSet createRank0() {
        return new DDataSet(0, 1, 1, 1, 1);
    }
    
    /**
     * create a rank 1 dataset backed by array of doubles.
     * @param len0 length of the dimension
     * @return rank 1 qube dataset of backed by array of doubles.
     */
    public static DDataSet createRank1(int len0) {
        return new DDataSet(1, len0, 1, 1, 1);
    }

    /**
     * create a rank 2 qube dataset backed by array of doubles.
     * @param len0 length of the dimension
     * @param len1 length of the dimension
     * @return rank 2 qube dataset of backed by array of doubles.
     */    
    public static DDataSet createRank2(int len0, int len1) {
        return new DDataSet(2, len0, len1, 1, 1);
    }

    /**
     * create a rank 3 qube dataset backed by array of doubles.
     * @param len0 length of the dimension
     * @param len1 length of the dimension
     * @param len2 length of the dimension
     * @return rank 3 qube dataset of backed by array of doubles.
     */    
    public static DDataSet createRank3(int len0, int len1, int len2) {
        return new DDataSet(3, len0, len1, len2, 1);
    }

    /**
     * create a rank 4 qube dataset backed by array of doubles.
     * @param len0 length of the dimension
     * @param len1 length of the dimension
     * @param len2 length of the dimension
     * @param len3 length of the dimension
     * @return rank 4 qube dataset of backed by array of doubles.
     */    
    public static DDataSet createRank4(int len0, int len1, int len2, int len3) {
        return new DDataSet(4, len0, len1, len2, len3);
    }

    /**
     * Makes an array from array of dimension sizes.  The result will have
     * rank qube.length(). 
     * @param qube array specifying the rank and size of each dimension
     * @return the array as a QDataSet
     */
    public static DDataSet create(int[] qube) {
        if ( qube.length==0 ) {
            return new DDataSet( 0, 1, 1, 1, 1 );
        } else if ( qube.length == 1 ) {
            return DDataSet.createRank1(qube[0]);
        } else if ( qube.length == 2 ) {
            return DDataSet.createRank2(qube[0], qube[1]);
        } else if ( qube.length == 3 ) {
            return DDataSet.createRank3(qube[0], qube[1], qube[2]);
        } else if ( qube.length == 4 ) {
            return DDataSet.createRank4(qube[0], qube[1], qube[2], qube[3]);
        } else {
            throw new IllegalArgumentException("bad qube");
        }
    }

    /**
     * convenient method for creating DatumRanges bins datasets.
     * @param min the minumum value
     * @param max the maximum value
     * @param u the ratiometric or time location units
     * @return the rank1 bins dataset
     */
    public static DDataSet createRank1Bins( double min, double max, Units u ) {
        DDataSet result= new DDataSet(1, 2, 1, 1, 1);
        if ( u!=null ) result.putProperty( QDataSet.UNITS, u );
        result.putValue(0,min);
        result.putValue(1,max);
        result.putProperty( QDataSet.BINS_0, QDataSet.VALUE_BINS_MIN_MAX );
        return result;
    }
    /**
     * Wraps an array from array of dimension sizes.  The result will have
     * rank qube.length().  For rank 0, data is 1-element array.
     * @param data array containing the data, with the last dimension contiguous in memory.
     * @param qube array specifying the rank and size of each dimension
     * @return the array as a QDataSet
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

    /**
     * wrap the array to make it into a dataset.  This is the same as
     * wrap( back, qube ).  The data is not copied, so be sure that no outside 
     * codes modify the data unintentionally.
     * @param back the array
     * @param rank the rank of the dataset
     * @param len0 the length, or 1 if the index is not needed.
     * @param len1 the length, or 1 if the index is not needed.
     * @param len2 the length, or 1 if the index is not needed.
     * @return the dataset wrapping the array.
     */
    public static DDataSet wrap( double[] back, int rank, int len0, int len1, int len2 ) {
        return new DDataSet( rank, len0, len1, len2, 1, back );
    }
        
    protected DDataSet( int rank, int len0, int len1, int len2, int len3 ) {
        this( rank, len0, len1, len2, len3, new double[ len0 * len1 * len2 * len3] );
    }

    protected DDataSet(int rank, int len0, int len1, int len2, int len3, double[] back) {
        super( double.class );
        if ( back==null ) throw new NullPointerException("back was null");
        this.back = back;
        if ( rank<0 ) throw new IllegalArgumentException("rank was -1");
        this.rank = rank;
        this.len0 = len0;
        this.len1 = len1;
        this.len2 = len2;
        this.len3 = len3;
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
        return this.back.length * 8;
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
        this.back= (double[])back;
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

    @Override
    public void putValue( double value ) {
        checkImmutable();
        if ( this.rank!=0 ) {
            throw new IllegalArgumentException("rank 0 putValue called on dataset that is rank "+this.rank+".");
        }
        back[0]= value;
    }

    @Override
    public void putValue( int i0, double value ) {
        checkImmutable();
        if (RANGE_CHECK) {
            if (i0 < 0 || i0 >= len0) {
                throw new IndexOutOfBoundsException("i0=" + i0 + " " + this);
            }
        }
        back[i0] = value;
    }

    @Override
    public void putValue( int i0, int i1, double value ) {
        //checkImmutable();
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
        back[i0 * len1 * len2 + i1 * len2 + i2] = value;
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
        back[ i0 * len1 * len2 * len3 + i1 * len2 * len3 + i2 * len3 + i3 ] = value;
    }
    
    /**
     * add this value to the current value. 
     * @param i0 the index
     * @param value the value, which is cast to this internal type.
     */
    public void addValue( int i0, double value ) {
        checkImmutable();
        back[ i0 ]+= value;
    }

    /**
     * add this value to the current value. 
     * @param i0 the index
     * @param i1 the index
     * @param value the value, which is cast to this internal type.
     */
    public void addValue( int i0, int i1, double value ) {
        checkImmutable();
        back[  i0 * len1 + i1 ]+= value;
    }
    
    /**
     * add the value to the position.  This is done all over the place with code like:
     *   dd.putValue( i0,i1, dd.getValue( i0,i1 ) + z )
     * which is inefficient.
     * @param i0
     * @param value
     * @deprecated use addValue
     */
    public void accumValue( int i0, double value ) {
        addValue(i0,value);
    }    
    
    /**
     * add the value to the position.  This is done all over the place with code like:
     *   dd.putValue( i0,i1, dd.getValue( i0,i1 ) + z )
     * which is inefficient.
     * @param i0
     * @param i1
     * @param value
     * @deprecated use addValue
     */
    public void accumValue( int i0, int i1, double value ) {
        addValue( i0,i1,value );
    }
    
    /**
     * creates a rank1 DDataSet by wrapping an existing array.
     * @param back the new backing array
     * @return the dataset
     */
    public static DDataSet wrap( double[] back ) {
        return new DDataSet( 1, back.length, 1, 1, 1, back );
    }

    /**
     * creates a DDataSet by wrapping an existing array, aliasing it to rank 2.
     * @param back the new backing array
     * @param nx number of elements in the zeroth index
     * @param ny number of elements in the first index
     * @return the dataset
     */
    public static DDataSet wrap( double[] back, int nx, int ny ) {
        return new DDataSet( 2, nx, ny, 1, 1, back );
    }

    public static DDataSet wrap( double[] back, int rank, int len0, int len1, int len2, int len3) {
        return new DDataSet( rank, len0, len1, len2, len3, back);
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
     * @param back 
     * @param n1 the size of the second dimension.
     * @return 
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
     * copy elements of src DDataSet into dest DDataSet, with System.arraycopy.
     * src and dst must have the same geometry, except for dim 0.  Allows for
     * aliasing when higher dimension element count matches.
     * @param src source dataset
     * @param srcpos source dataset first dimension index.
     * @param dest destination dataset
     * @param nrec number of records to copy.  Note this is different than the other copyElements!
     * @param destpos destination dataset first dimension index.
     * @throws IllegalArgumentException if the higher rank geometry doesn't match
     * @throws IndexOutOfBoundsException
     * @see #copyElements(org.virbo.dataset.DDataSet, int, org.virbo.dataset.DDataSet, int, int, boolean) 
     */
    public static void copyElements(DDataSet src, int srcpos, DDataSet dest, int destpos, int nrec ) {
        if ( src.len1 != dest.len1 || src.len2 != dest.len2 ) {
            throw new IllegalArgumentException("src and dest geometry don't match");
        }
        copyElements( src, srcpos, dest, destpos, nrec * src.len1 * src.len2 * src.len3, false); 
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
        if ( checkAlias && ( src.len1*src.len2*src.len3 != dest.len1*dest.len2*dest.len3 ) ) {
            throw new IllegalArgumentException("src and dest geometry don't match");
        }
        int srcpos1 = srcpos * src.len1 * src.len2 * src.len3;
        int destpos1 = destpos * dest.len1 * dest.len2 * dest.len3;
        int len1 = len;
        if ( dest.rank==4 ) {
            System.err.println("here16 "+destpos1+" "+len1);
        }
        System.arraycopy( src.back, srcpos1, dest.back, destpos1, len1 );
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
        double[] newback = new double[noff2-noff1];
        System.arraycopy( this.back, noff1, newback, 0, noff2-noff1 );
        Map<String,Object> props= DataSetOps.sliceProperties0(i,DataSetUtil.getProperties(this));
        props= DataSetUtil.sliceProperties( this, i, props );
        DDataSet result= new DDataSet( nrank, len1, len2, len3, 1, newback );
        DataSetUtil.putProperties( props, result );
        return result;
    }

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
        double[] newback = new double[noff2-noff1];

        if ( noff2-noff1>0 ) {
            System.arraycopy( this.back, noff1, newback, 0, noff2-noff1 );
        }
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
