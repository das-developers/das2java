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
public final class DDataSet extends AbstractDataSet implements WritableDataSet {
    double[] back;
    
    int rank;
    
    int len0;
    int len1;
    int len2;
    
    private static final boolean RANGE_CHECK= false;
    
    public static final String version="20070529";
    
    public static DDataSet createRank1( int len0 ) {
        return new DDataSet( 1, len0, 1, 1 );
    }
    
    public static DDataSet createRank2( int len0, int len1 ) {
        return new DDataSet( 2, len0, len1, 1 );
    }
    
    public static DDataSet createRank3( int len0, int len1, int len2 ) {
        return new DDataSet( 3, len0, len1, len2 );
    }
    
    /**
     * Makes an array from array of dimension sizes.  The result will have
     * rank qube.length(). 
     * @param qube array specifying the rank and size of each dimension
     * @return DDataSet
     */
    public static DDataSet create( int[] qube ) {
        if (qube.length == 1) {
            return DDataSet.createRank1(qube[0]);
        } else if (qube.length == 2) {
            return DDataSet.createRank2(qube[0], qube[1]);
        } else if (qube.length == 3) {
            return DDataSet.createRank3(qube[0], qube[1], qube[2]);
        } else {
            throw new IllegalArgumentException("bad qube");
        }
    }
    
    /** Creates a new instance of DDataSet */
    private DDataSet( int rank, int len0, int len1, int len2 ) {
        this( rank, len0, len1, len2, new double[ len0 * len1 * len2 ] );
    }

    private DDataSet( int rank, int len0, int len1, int len2, double[] back ) {
       this.back= back;
       this.rank= rank;
       this.len0= len0;
       this.len1= len1;
       this.len2= len2;        
       DataSetUtil.addQube(this);
    }
    
    public int rank() {
        return rank;
    }

    public int length() {
        return len0;
    }

    public int length(int i) {
        return len1;
    }
    
    public int length( int i0, int i1 ) {
        return len2;
    }

    public double value(int i0) {
        if ( RANGE_CHECK ) {
            if ( i0<0 || i0>=len0 ) throw new IndexOutOfBoundsException("i0="+i0+" "+this);
        }
        return back[ i0 ];
    }    

    public double value(int i0, int i1) {
        if ( RANGE_CHECK ) {
            if ( i0<0 || i0>=len0 ) throw new IndexOutOfBoundsException("i0="+i0+" "+this);
            if ( i1<0 || i1>=len1 ) throw new IndexOutOfBoundsException("i1="+i1+" "+this);
        }
        return back[ i0 * len1 + i1 ];
    }    
    
    public double value(int i0, int i1, int i2 ) {
        if ( RANGE_CHECK ) {
            if ( i0<0 || i0>=len0 ) throw new IndexOutOfBoundsException("i0="+i0+" "+this);
            if ( i1<0 || i1>=len1 ) throw new IndexOutOfBoundsException("i1="+i1+" "+this);
            if ( i2<0 || i2>=len2 ) throw new IndexOutOfBoundsException("i2="+i2+" "+this);
        }
        return back[ i0 * len1 * len2 + i1 *len2 + i2 ];
    }    

    public void putValue( int i0, double value ) {
        if ( RANGE_CHECK ) {
            if ( i0<0 || i0>=len0 ) throw new IndexOutOfBoundsException("i0="+i0+" "+this);
        }
        back[ i0 ]= value;
    }

    public void putValue( int i0, int i1, double value ) {
        if ( RANGE_CHECK ) {
            if ( i0<0 || i0>=len0 ) throw new IndexOutOfBoundsException("i0="+i0+" "+this);
            if ( i1<0 || i1>=len1 ) throw new IndexOutOfBoundsException("i1="+i1+" "+this);
        }        
        back[  i0 * len1 + i1 ]= value;
    }

    public void putValue( int i0, int i1, int i2, double value ) {
        if ( RANGE_CHECK ) {
            if ( i0<0 || i0>=len0 ) throw new IndexOutOfBoundsException("i0="+i0+" "+this);
            if ( i1<0 || i1>=len1 ) throw new IndexOutOfBoundsException("i1="+i1+" "+this);
            if ( i2<0 || i2>=len2 ) throw new IndexOutOfBoundsException("i2="+i2+" "+this);
        }
        back[ i0 * len1 * len2 + i1 *len2 + i2  ]= value;
    }


    /**
     * Shorten the dataset by changing it's dim 0 length parameter.  The same backing array is used, 
     * so the element that remain ill be the same.
     * can only shorten!
     */
    public void putLength( int len ) {
        if ( len>len0 ) throw new IllegalArgumentException("dataset cannot be lengthened");
        len0= len;
    }

    @Override
    public String toString( ) {
        return DataSetUtil.toString( this );
    }
    
    /**
     * copies the properties, copying depend datasets as well.  See also DataSetUtil.copyProperties
     */
    private static Map copyProperties( QDataSet ds ) {
        Map result= new HashMap();
        String[] names= new String[] { QDataSet.VALID_RANGE, QDataSet.CADENCE, QDataSet.FILL, QDataSet.MONOTONIC, QDataSet.SCALE_TYPE, 
            QDataSet.TYPICAL_RANGE, QDataSet.UNITS, QDataSet.VALID_RANGE,
            QDataSet.CACHE_TAG, };
        
        Map srcProps= DataSetUtil.getProperties(ds);
        
        result.putAll(srcProps);
                
        for ( int i=0; i<ds.rank(); i++ ) {
            QDataSet dep= (QDataSet) ds.property( "DEPEND_"+i );
            if ( dep==ds ) throw new IllegalArgumentException("dataset is dependent on itsself!");
            if ( dep!=null ) {
                result.put( "DEPEND_"+i, copy( dep ) );
            }
        }
        
        QDataSet plane0= (QDataSet) ds.property( QDataSet.PLANE_0 );
        if ( plane0!=null ) result.put( QDataSet.PLANE_0, copy( plane0 ) );
        //TODO: correlated PLANEs
        return result;
    }
     
    private static DDataSet ddcopy( DDataSet ds ) {
        int dsLength= ds.len0 * ds.len1 * ds.len2;
        
        double[] newback= new double[ dsLength ];
        
        System.arraycopy( ds.back, 0, newback, 0, dsLength );
        
        DDataSet result= new DDataSet( ds.rank, ds.len0, ds.len1, ds.len2, newback );
        result.properties.putAll( copyProperties(ds) ); // TODO: problems... 
        
        return result;
    }
    
    /**
     * copies the dataset into a writeable dataset, and all of its depend datasets as well.
     * //TODO: check for DDataSet, do System.arraycopy.
     */
    public static DDataSet copy( QDataSet ds ) {
        if ( ds instanceof DDataSet ) return ddcopy( (DDataSet)ds );
        int rank= ds.rank();
        DDataSet result;
        switch (rank) {
            case 1: 
                result= createRank1( ds.length() ); 
                for ( int i=0; i<ds.length(); i++ ) {
                    result.putValue( i, ds.value(i) );
                }
                break;
            case 2: 
                result= createRank2( ds.length(), ds.length(0) ); 
                for ( int i=0; i<ds.length(); i++ ) {
                    for ( int j=0; j<ds.length(i); j++ ) {
                        result.putValue( i, j, ds.value(i,j) );
                    }
                }
                break;
            case 3: 
                result= createRank3( ds.length(), ds.length(0), ds.length(0,0) ); 
                for ( int i=0; i<ds.length(); i++ ) {
                    for ( int j=0; j<ds.length(i); j++ ) {
                        for ( int k=0; k<ds.length(i,j); k++ ) {
                            result.putValue( i, j, k, ds.value(i,j,k) );
                        }
                    }
                }
                break;
            default: throw new IllegalArgumentException("bad rank");
        }
        result.properties.putAll( copyProperties(ds) ); // TODO: problems...
        
        return result;
    }
    
    /**
     * creates a DDataSet by wrapping an existing double array.
     */
    public static DDataSet wrap( double[] back ) {
        return new DDataSet( 1, back.length, 1, 1, back );
    }
    
    /**
     * creates a DDataSet by wrapping an existing array, and aliasing it to rank2.
     * Note the last index is packed closest in memory.
     * @param n1 the size of the second dimension.
     */
    public static DDataSet wrapRank2( double[] back, int n1 ) {
        return new DDataSet( 2, back.length/n1, n1, 1, back );
    }
    
    /**
     * creates a DDataSet by wrapping an existing array, and aliasing it to rank2.
     * Note the last index is packed closest in memory.  The first index length
     * is calculated from the size of the array.
     * @param n1 the size of the second index.
     * @param n2 the size of the third index.
     */
    public static DDataSet wrapRank3( double[] back, int n1, int n2 ) {
        return new DDataSet( 3, back.length/(n1*n2), n1, n2, back );
    }
    
    /**
     * creates a DDataSet by wrapping an existing array, aliasing it to rank 2.
     */
    public static DDataSet wrap(double[] back, int nx, int ny) {
        return new DDataSet( 1, nx, ny, 1, back );
    }
    
    /**
     * join dep0 if found, join auxillary planes if found.
     */
    private void joinProperties( DDataSet ds ) {
        Map result= new HashMap();
        for ( int i=0; i<1; i++ ) {
            QDataSet dep1= (QDataSet) ds.property( "DEPEND_"+i );
            if ( dep1!=null ) {
                QDataSet dep0= (QDataSet) this.property( "DEPEND_"+i );
                DDataSet djoin= DDataSet.copy( dep0 );
                DDataSet ddep1= dep1 instanceof DDataSet ? (DDataSet) dep1 : DDataSet.copy( dep1 );
                djoin.join( ddep1 );
                result.put( "DEPEND_"+i, djoin );
            }
        }
        
        QDataSet dep1= (QDataSet) ds.property( QDataSet.PLANE_0 );
        if ( dep1!=null ) {
            QDataSet dep0= (QDataSet) this.property( QDataSet.PLANE_0 );
            DDataSet djoin= DDataSet.copy( dep0 );
            DDataSet dd1= dep1 instanceof DDataSet ? (DDataSet) dep1 : DDataSet.copy( dep1 );
            djoin.join( dd1 );
            result.put( QDataSet.PLANE_0, djoin );
        }
        
        //TODO: correlated PLANEs
        this.properties.putAll( result );
    }
    
    /**
     * copy elements of src DDataSet into dest DDataSet, with System.arraycopy.
     * src and dst must have the same geometry, except for dim 0.
     * @throws IllegalArgumentException if the higher rank geometry doesn't match
     * @throws IndexOutOfBoundsException
     */
    public static void copyElements( DDataSet src, int srcpos, DDataSet dest, int destpos, int len ) {
        if ( src.len1 !=  dest.len1 || src.len2 != dest.len2 ) throw new IllegalArgumentException("src and dest geometry don't match");
        int srcpos1= srcpos * src.len1 * src.len2;
        int destpos1=  destpos * src.len1 * src.len2;
        int len1= len * src.len1 * src.len2;
        System.arraycopy( src.back, srcpos1, dest.back, destpos1, len1 );
    }
    
    /**
     * append the second dataset onto this dataset.  Not thread safe!!!
     * TODO: this really should return a new dataset.  Presumably this is to avoid copies, but currently it copies anyway!
     */
    public void join( DDataSet ds ) {
        if ( ds.rank()!=rank ) throw new IllegalArgumentException("rank mismatch");
        if ( ds.len1!=len1 ) throw new IllegalArgumentException("len1 mismatch");
        if ( ds.len2!=len2 ) throw new IllegalArgumentException("len2 mismatch");
        
        int myLength= len0 * len1 * len2;
        int dsLength= ds.len0 * ds.len1 * ds.len2;
        
        double[] newback= new double[ myLength + dsLength ];
        
        System.arraycopy( this.back, 0, newback, 0, myLength );
        System.arraycopy( ds.back, 0, newback, myLength, dsLength );
        
        len0= this.len0 + ds.len0;
        this.back= newback;
        
        joinProperties( ds );
    }
    
}
