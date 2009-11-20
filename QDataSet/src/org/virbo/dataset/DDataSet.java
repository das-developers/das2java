/*
 * DDataSet.java
 *
 * Created on April 24, 2007, 11:08 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.virbo.dataset;

import java.util.HashMap;
import java.util.Map;

/**
 * rank 1,2,or 3 dataset backed by double array. 
 * Mutable datasets warning: No dataset should be mutable once it is accessible to the
 * rest of the system.  This would require clients make defensive copies which would 
 * seriously degrade performance.  
 *
 * @author jbf
 */
public final class DDataSet extends AbstractDataSet implements WritableDataSet, RankZeroDataSet {

    double[] back;
    int rank;
    int len0;
    int len1;
    int len2;
    int len3;

    private static final boolean RANGE_CHECK = true;
    public static final String version = "20090605";

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
        } else {
            throw new IllegalArgumentException("bad qube");
        }
    }

    /**
     * Wraps an array from array of dimension sizes.  The result will have
     * rank qube.length(). 
     * @param data array containing the data, with the last dimension contiguous in memory.
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
        } else {
            throw new IllegalArgumentException("bad qube");
        }
    }

        
    /** Creates a new instance of DDataSet */
    private DDataSet(int rank, int len0, int len1, int len2, int len3) {
        this(rank, len0, len1, len2, len3, new double[len0 * len1 * len2 * len3]);
    }

    private DDataSet(int rank, int len0, int len1, int len2, int len3, double[] back) {
        this.back = back;
        this.rank = rank;
        this.len0 = len0;
        this.len1 = len1;
        this.len2 = len2;
        this.len3 = len3;
        DataSetUtil.addQube(this);
    }

    public int rank() {
        return rank;
    }

    @Override
    public int length() {
        return len0;
    }

    @Override
    public int length(int i) {
        return len1;
    }

    @Override
    public int length(int i0, int i1) {
        return len2;
    }

    @Override
    public int length(int i0, int i1, int i2) {
        return len3;
    }

    @Override
    public double value() {
        return back[0];
    }

    @Override
    public double value(int i0) {
        if (RANGE_CHECK) {
            if (i0 < 0 || i0 >= len0) {
                throw new IndexOutOfBoundsException("i0=" + i0 + " " + this);
            }
        }
        return back[i0];
    }

    @Override
    public double value(int i0, int i1) {
        if (RANGE_CHECK) {
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
        back[i0 * len1 * len2 + i1 * len2 + i2] = value;
    }

    /**
     * Shorten the dataset by changing it's dim 0 length parameter.  The same backing array is used, 
     * so the element that remain ill be the same.
     * can only shorten!
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
     * copies the properties, copying depend datasets as well.  
     * @see DataSetUtil.copyProperties, which is a shallow copy.
     */
    protected static Map copyProperties(QDataSet ds) {
        Map result = new HashMap();        
        Map srcProps= DataSetUtil.getProperties(ds);
        
        result.putAll(srcProps);
                
        for ( int i=0; i < ds.rank(); i++) {
            QDataSet dep = (QDataSet) ds.property("DEPEND_" + i);
            if (dep == ds) {
                throw new IllegalArgumentException("dataset is dependent on itsself!");
            }
            if (dep != null) {
                result.put("DEPEND_" + i, copy(dep));
            }
        }

        for (int i = 0; i < QDataSet.MAX_PLANE_COUNT; i++) {
            QDataSet plane0 = (QDataSet) ds.property("PLANE_" + i);
            if (plane0 != null) {
                result.put("PLANE_" + i, copy(plane0));
            } else {
                break;
            }
        }

        return result;
    }

    private static DDataSet ddcopy(DDataSet ds) {
        int dsLength = ds.len0 * ds.len1 * ds.len2 * ds.len3;

        double[] newback = new double[dsLength];

        System.arraycopy(ds.back, 0, newback, 0, dsLength);

        DDataSet result = new DDataSet(ds.rank, ds.len0, ds.len1, ds.len2, ds.len3, newback);
        result.properties.putAll(copyProperties(ds)); // TODO: problems... 

        return result;
    }

    /**
     * Copy the dataset to a DDataSet only if the dataset is not already a DDataSet.
     * @param ds
     * @return
     */
    public static DDataSet maybeCopy( QDataSet ds ) {
        if ( ds instanceof DDataSet ) {
            return (DDataSet)ds;
        } else {
            return copy(ds);
        }
    }
    
    /**
     * copies the dataset into a writeable dataset, and all of its depend datasets as well.
     * An optimized copy is used when the argument is a DDataSet.
     */
    public static DDataSet copy(QDataSet ds) {
        if (ds instanceof DDataSet) {
            return ddcopy((DDataSet) ds);
        }
        int rank = ds.rank();
        DDataSet result;
        int len1,len2,len3;
        if ( !DataSetUtil.isQube(ds) ) {
            //throw new IllegalArgumentException("copy non-qube");
            System.err.println("copy of non-qube to DDataSet, which must be qube");
        }
        switch (rank) {
            case 1:
                result = createRank1(ds.length());
                for (int i = 0; i < ds.length(); i++) {
                    result.putValue(i, ds.value(i));
                }
                break;
            case 2:
                len1= ds.length() == 0 ? 0 : ds.length(0);
                result = createRank2(ds.length(), len1 );
                for (int i = 0; i < ds.length(); i++) {
                    for (int j = 0; j < ds.length(i); j++) {
                        result.putValue(i, j, ds.value(i, j));
                    }
                }
                break;
            case 3:
                len1= ds.length() == 0 ? 0 : ds.length(0) ;
                len2= len1 == 0 ? 0 : ds.length(0,0);
                result = createRank3(ds.length(), len1, len2 );
                for (int i = 0; i < ds.length(); i++) {
                    for (int j = 0; j < ds.length(i); j++) {
                        for (int k = 0; k < ds.length(i, j); k++) {
                            result.putValue(i, j, k, ds.value(i, j, k));
                        }
                    }
                }
                break;
            case 4:
                len1 = (ds.length()==0) ? 0 : ds.length(0);
                len2 = (len1==0) ? 0 : ds.length(0,0);
                len3 = (len2==0) ? 0 : ds.length(0,0,0);
                result = createRank4(ds.length(), len1, len2, len3);
                for (int i=0; i< ds.length(); i++)
                    for (int j=0; j<ds.length(i); j++)
                        for (int k=0; k<ds.length(i,j); k++)
                            for (int l=0; l<ds.length(i,j,k); l++)
                                result.putValue(i, j, k, l, ds.value(i, j, k, l));
                break;
            default:
                throw new IllegalArgumentException("bad rank");
        }
        result.properties.putAll(copyProperties(ds)); // TODO: problems...

        return result;
    }

    /**
     * creates a DDataSet by wrapping an existing double array.
     */
    public static DDataSet wrap(double[] back) {
        return new DDataSet(1, back.length, 1, 1, 1, back);
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
     * join dep0 if found, join auxillary planes if found.
     */
    private void joinProperties(DDataSet ds) {
        Map result = new HashMap();
        for (int i = 0; i < 1; i++) {
            QDataSet dep1 = (QDataSet) ds.property("DEPEND_" + i);
            if (dep1 != null) {
                QDataSet dep0 = (QDataSet) this.property("DEPEND_" + i);
                DDataSet djoin = DDataSet.copy(dep0);
                DDataSet ddep1 = dep1 instanceof DDataSet ? (DDataSet) dep1 : DDataSet.copy(dep1);
                djoin.append(ddep1);
                result.put("DEPEND_" + i, djoin);
            }
        }

        for (int i = 0; i < QDataSet.MAX_PLANE_COUNT; i++) {
            QDataSet dep1 = (QDataSet) ds.property("PLANE_" + i);
            if (dep1 != null) {
                QDataSet dep0 = (QDataSet) this.property("PLANE_" + i);
                DDataSet djoin = DDataSet.copy(dep0); 
                DDataSet dd1 = dep1 instanceof DDataSet ? (DDataSet) dep1 : DDataSet.copy(dep1);
                djoin.append(dd1);  
                result.put("PLANE_" + i, djoin);
            } else {
                break;
            }
        }

        this.properties.putAll(result);
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
     * append the second dataset onto this dataset.  Not thread safe!!!
     * TODO: this really should return a new dataset.  Presumably this is to avoid copies, but currently it copies anyway!
     * TODO: this will be renamed "concatenate" or "append" since "join" is the anti-slice.
     * @deprecated use append instead.
     */
    public void join(DDataSet ds) {
        append(ds);
    }
    
    /**
     * append the second dataset onto this dataset.  Not thread safe!!!
     * TODO: this really should return a new dataset.  Presumably this is to avoid copies, but currently it copies anyway!
     */
    public void append( DDataSet ds ) {
        if (ds.rank() != rank) {
            throw new IllegalArgumentException("rank mismatch");
        }
        if (ds.len1 != len1) {
            throw new IllegalArgumentException("len1 mismatch");
        }
        if (ds.len2 != len2) {
            throw new IllegalArgumentException("len2 mismatch");
        }
        if (ds.len3 != len3) {
            throw new IllegalArgumentException("len3 mismatch");
        }

        int myLength = len0 * len1 * len2 * len3;
        int dsLength = ds.len0 * ds.len1 * ds.len2 * ds.len3;

        double[] newback = new double[myLength + dsLength];

        System.arraycopy(this.back, 0, newback, 0, myLength);
        System.arraycopy(ds.back, 0, newback, myLength, dsLength);

        len0 = this.len0 + ds.len0;
        this.back = newback;

        joinProperties(ds);
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
        return new DDataSet( nrank, end-start, len1, len2, len3, newback );
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
