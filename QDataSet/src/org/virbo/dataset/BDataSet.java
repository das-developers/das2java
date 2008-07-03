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
 * rank 1,2,or 3 dataset backed by Integer (4 byte) array. 
 * Mutable datasets warning: No dataset should be mutable once it is accessible to the
 * rest of the system.  This would require clients make defensive copies which would 
 * seriously degrade performance.  
 *
 * @author jbf
 */
public final class BDataSet extends AbstractDataSet implements WritableDataSet {
    byte[] back;
    
    int rank;
    
    int len0;
    int len1;
    int len2;
    
    public static final String version="20070529";
    
    public static BDataSet createRank1( int len0 ) {
        return new BDataSet( 1, len0, 1, 1 );
    }
    
    public static BDataSet createRank2( int len0, int len1 ) {
        return new BDataSet( 2, len0, len1, 1 );
    }
    
    public static BDataSet createRank3( int len0, int len1, int len2 ) {
        return new BDataSet( 3, len0, len1, len2 );
    }
    
    /** Creates a new instance of DDataSet */
    private BDataSet( int rank, int len0, int len1, int len2 ) {
        this( rank, len0, len1, len2, new byte[ len0 * len1 * len2 ] );
    }

    private BDataSet( int rank, int len0, int len1, int len2, byte[] back ) {
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
        return back[ i0 ];
    }    

    public double value(int i0, int i1) {
        return back[ i0 * len1 + i1 ];
    }    
    
    public double value(int i0, int i1, int i2 ) {
        return back[ i0 * len1 * len2 + i1 *len2 + i2 ];
    }    

    public void putValue( int i0, double value ) {
        back[ i0 ]= (byte)value;
    }

    public void putValue( int i0, int i1, double value ) {
        back[  i0 * len1 + i1 ]= (byte)value;
    }

    public void putValue( int i0, int i1, int i2, double value ) {
        back[ i0 * len1 * len2 + i1 *len2 + i2  ]= (byte)value;
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

    public String toString( ) {
        return DataSetUtil.toString( this );
    }
    
    /**
     * copies the properties, copying depend datasets as well.
     */
    private static Map copyProperties( QDataSet ds ) {
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
            }
        }

        return result;
    }
     
    private static BDataSet ddcopy( BDataSet ds ) {
        int dsLength= ds.len0 * ds.len1 * ds.len2;
        
        byte[] newback= new byte[ dsLength ];
        
        System.arraycopy( ds.back, 0, newback, 0, dsLength );
        
        BDataSet result= new BDataSet( ds.rank, ds.len0, ds.len1, ds.len2, newback );
        result.properties.putAll( copyProperties(ds) ); // TODO: problems... 
        
        return result;
    }
    
    /**
     * copies the dataset into a writeable dataset, and all of it's depend datasets as well.
     * //TODO: check for DDataSet, do System.arraycopy.
     */
    public static BDataSet copy( QDataSet ds ) {
        if ( ds instanceof BDataSet ) return ddcopy( (BDataSet)ds );
        int rank= ds.rank();
        BDataSet result;
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
     * creates a rank1 BDataSet by wrapping an existing array.
     */
    public static BDataSet wrap( byte[] back ) {
        return new BDataSet( 1, back.length, 1, 1, back );
    }
    
    /**
     * creates a BDataSet by wrapping an existing array, aliasing it to rank 2.
     */
    public static BDataSet wrap( byte[] back, int nx, int ny ) {
        return new BDataSet( 2, nx, ny, 1, back );
    }
        
    private void joinProperties( BDataSet ds ) {
        Map result= new HashMap();
        for ( int i=0; i<ds.rank(); i++ ) {
            QDataSet dep1= (QDataSet) ds.property( "DEPEND_"+i );
            if ( dep1!=null ) {
                QDataSet dep0= (QDataSet) this.property( "DEPEND_"+i );
                BDataSet djoin= BDataSet.copy( dep0 );
                BDataSet ddep1= dep1 instanceof BDataSet ? (BDataSet) dep1 : BDataSet.copy( dep1 );
                djoin.join( ddep1 );
                result.put( "DEPEND_"+i, djoin );
            }
        }
        QDataSet dep1= (QDataSet) ds.property( QDataSet.PLANE_0 );
        if ( dep1!=null ) {
            QDataSet dep0= (QDataSet) this.property( QDataSet.PLANE_0 );
            BDataSet djoin= BDataSet.copy( dep0 );
            BDataSet dd1= dep1 instanceof BDataSet ? (BDataSet) dep1 : BDataSet.copy( dep1 );
            djoin.join( dd1 );
            result.put( QDataSet.PLANE_0, djoin );
        }
        //TODO: correlated PLANEs
        this.properties.putAll( result );
    }
    
    /**
     * append the second dataset onto this dataset.  Not thread safe!!!
     * TODO: this really should return a new dataset.  Presumably this is to avoid copies, but currently it copies anyway!
     */
    public void join( BDataSet ds ) {
        if ( ds.rank()!=rank ) throw new IllegalArgumentException("rank mismatch");
        if ( ds.len1!=len1 ) throw new IllegalArgumentException("len1 mismatch");
        if ( ds.len2!=len2 ) throw new IllegalArgumentException("len2 mismatch");
        
        int myLength= len0 * len1 * len2;
        int dsLength= ds.len0 * ds.len1 * ds.len2;
        
        byte[] newback= new byte[ myLength + dsLength ];
        
        System.arraycopy( this.back, 0, newback, 0, myLength );
        System.arraycopy( ds.back, 0, newback, myLength, dsLength );
        
        len0= this.len0 + ds.len0;
        this.back= newback;
        
        joinProperties( ds );
    }

    
}
