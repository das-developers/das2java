/*
 * BDataSet.java
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
    int len3;
    
    public static final String version="20090605";
    
    public static BDataSet createRank1( int len0 ) {
        return new BDataSet( 1, len0, 1, 1, 1 );
    }
    
    public static BDataSet createRank2( int len0, int len1 ) {
        return new BDataSet( 2, len0, len1, 1, 1 );
    }
    
    public static BDataSet createRank3( int len0, int len1, int len2 ) {
        return new BDataSet( 3, len0, len1, len2, 1 );
    }

    public static BDataSet createRank4( int len0, int len1, int len2, int len3 ) {
        return new BDataSet( 4, len0, len1, len2, len3);
    }
    /**
     * Wraps an array from array of dimension sizes.  The result will have
     * rank qube.length(). 
     * @param data array containing the data, with the last dimension contiguous in memory.
     * @param qube array specifying the rank and size of each dimension
     * @return IDataSet
     */
    public static BDataSet wrap( byte[] data, int[] qube ) {
        if (qube.length == 1) {
            return new BDataSet( 1, qube[0], 1, 1, 1, data );
        } else if (qube.length == 2) {
            return new BDataSet( 2, qube[0], qube[1], 1, 1, data );
        } else if (qube.length == 3) {
            return new BDataSet( 3, qube[0], qube[1], qube[2], 1, data );
        } else if (qube.length == 4) {
            return new BDataSet( 4, qube[0], qube[1], qube[2], qube[3], data);
        } else {
            throw new IllegalArgumentException("bad qube");
        }
    }
    
    /** Creates a new instance of BDataSet */
    private BDataSet( int rank, int len0, int len1, int len2, int len3 ) {
        this( rank, len0, len1, len2, len3, new byte[ len0 * len1 * len2 * len3 ] );
    }

    private BDataSet( int rank, int len0, int len1, int len2, int len3, byte[] back ) {
       this.back= back;
       this.rank= rank;
       this.len0= len0;
       this.len1= len1;
       this.len2= len2;
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
    public int length( int i0, int i1 ) {
        return len2;
    }

    @Override
    public int length( int i0, int i1, int i2) {
        return len3;
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
        back[0]= (byte)value;
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

    public void putValue( int i0, int i1, int i2, int i3, double value) {
        back[ i0*len1*len2*len3 + i1*len2*len3 + i2*len3 +i3] = (byte)value;
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
            } else {
                break;
            }
        }

        return result;
    }
     
    private static BDataSet ddcopy( BDataSet ds ) {
        int dsLength= ds.len0 * ds.len1 * ds.len2 * ds.len3;
        
        byte[] newback= new byte[ dsLength ];
        
        System.arraycopy( ds.back, 0, newback, 0, dsLength );
        
        BDataSet result= new BDataSet( ds.rank, ds.len0, ds.len1, ds.len2, ds.len3, newback );
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
            case 4:
                result = createRank4(ds.length(), ds.length(0), ds.length(0,0), ds.length(0,0,0));
                for (int i=0; i< ds.length(); i++)
                    for (int j=0; j < ds.length(i); j++)
                        for (int k=0; k < ds.length(i,j); k++)
                            for (int l=0; l < ds.length(i,j,k); l++)
                                result.putValue(i, j, k, l, ds.value(i,j,k,l));
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
        return new BDataSet( 1, back.length, 1, 1, 1, back );
    }
    
    /**
     * creates a BDataSet by wrapping an existing array, aliasing it to rank 2.
     */
    public static BDataSet wrap( byte[] back, int nx, int ny ) {
        return new BDataSet( 2, nx, ny, 1, 1, back );
    }
        
    /**
     * creates a DataSet by wrapping an existing array, aliasing it to rank 3.
     */
    public static BDataSet wrap( byte[] back, int nx, int ny, int nz ) {
        return new BDataSet( 3, nx, ny, nz, 1, back );
    }

    /**
     * creates a BDataSet by wrapping an existing array, aliasing it to rank 4.
     */
    public static BDataSet wrap( byte[] back, int nx, int ny, int nz, int nw) {
        return new BDataSet (4, nx, ny, nz, nw, back);
    }

    public static BDataSet wrap( byte[] back, int rank, int len0, int len1, int len2, int len3 ) {
        return new BDataSet( rank, len0, len1, len2, len3, back );
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
     * @deprecated use append instead.
     */
public void join(BDataSet ds) {
        append(ds);
    }

    /**
     * append the second dataset onto this dataset.  Not thread safe!!!
     * TODO: this really should return a new dataset.  Presumably this is to avoid copies, but currently it copies anyway!
     */
    public void append( BDataSet ds ) {
        if ( ds.rank()!=rank ) throw new IllegalArgumentException("rank mismatch");
        if ( ds.len1!=len1 ) throw new IllegalArgumentException("len1 mismatch");
        if ( ds.len2!=len2 ) throw new IllegalArgumentException("len2 mismatch");
        if ( ds.len3!=len3 ) throw new IllegalArgumentException("len3 mismatch");
        int myLength= len0 * len1 * len2 * len3;
        int dsLength= ds.len0 * ds.len1 * ds.len2 * ds.len3;
        
        byte[] newback= new byte[ myLength + dsLength ];
        
        System.arraycopy( this.back, 0, newback, 0, myLength );
        System.arraycopy( ds.back, 0, newback, myLength, dsLength );
        
        len0= this.len0 + ds.len0;
        this.back= newback;
        
        joinProperties( ds );
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
        byte[] newback = new byte[noff2-noff1];
        System.arraycopy( this.back, noff1, newback, 0, noff2-noff1 );
        Map<String,Object> props= DataSetOps.sliceProperties0(i,DataSetUtil.getProperties(this));
        BDataSet result= new BDataSet( nrank, len1, len2, len3, 1, newback );
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
        byte[] newback = new byte[noff2-noff1];
        System.arraycopy( this.back, noff1, newback, 0, noff2-noff1 );
        BDataSet result= new BDataSet( nrank, end-start, len1, len2, len3, newback );
        DataSetUtil.putProperties( DataSetUtil.getProperties(this), result );
        QDataSet dep0= (QDataSet) property(QDataSet.DEPEND_0);
        if ( dep0!=null ) result.putProperty( QDataSet.DEPEND_0, dep0.trim(start, end) );
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
