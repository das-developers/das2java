/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.autoplot.bufferdataset;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.CacheTag;
import org.das2.datum.EnumerationUnits;
import org.das2.datum.Units;
import org.das2.datum.UnitsConverter;
import org.das2.util.LoggerManager;
import org.virbo.dataset.AbstractDataSet;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dataset.WritableDataSet;
import org.virbo.dsops.Ops;

/**
 * rank 1, 2, 3, and 4 datasets backed by NIO buffers.  These have the 
 * advantage that data can be stored outside of the JVM, and in fact they 
 * can be backed by a huge file on disk.  
 * 
 * This code was copied from BinaryDataSource.
 *
 * @author jbf
 */
public abstract class BufferDataSet extends AbstractDataSet implements WritableDataSet {

    private static final Logger logger= LoggerManager.getLogger( "qdataset.bufferdataset" );
    
    int rank;
    int len0;
    int len1;
    int len2;
    int len3;
    
    
    /**
     * the number of bytes per record
     */
    int reclen;
    
    /**
     * the byte offset into each record
     */
    int recoffset;
    
    /**
     * the number of bytes of the field in each record
     */
    int fieldLen;
    
    /**
     * the field type
     */
    Object type;

    /**
     * the array backing the data
     */
    protected ByteBuffer back;
    
    private static final boolean RANGE_CHECK = true;

    /**
     * the data is in 8 byte doubles.
     */
    public final static Object DOUBLE= "double";
    /**
     * the data is in 4 byte floats.
     */
    public final static Object FLOAT= "float";
    /**
     * the data is in 16 bit real that has exponent like a FLOAT but mantissa precision is reduced.
     */
    public final static Object TRUNCATEDFLOAT= "truncatedfloat"; 
    /**
     * VAX floats.
     */
    public final static Object VAX_FLOAT= "vaxfloat";
    /**
     * 8 byte signed longs
     */
    public final static Object LONG= "long";
    /**
     * 4 byte signed integers
     */
    public final static Object INT= "int";
    /**
     * 4 byte unsigned integers.  Note 4-byte signed ints are used to store the data 
     * which is unpacked in the value() method.
     */
    public final static Object UINT= "uint";
    /**
     * 2 byte short integer.
     */
    public final static Object SHORT= "short";
    /**
     * 2 byte unsigned short.
     */
    public final static Object USHORT= "ushort";
    /**
     * 1 byte signed byte. 
     */
    public final static Object BYTE= "byte";
    /**
     * 1 byte signed byte.
     */
    public final static Object UBYTE= "ubyte";
    
    /**
     * return the number of bytes of each type (double=8, etc).
     * @param type DOUBLE, FLOAT, UBYTE, etc.
     * @return 8, 4, 1, etc.
     */
    public static int byteCount(Object type) {
        if (type.equals(DOUBLE)) {
            return 8;
        } else if (type.equals(FLOAT)) {
            return 4;
        } else if ( type.equals(VAX_FLOAT) ) {
            return 4;
        } else if (type.equals(LONG)) {
            return 8;
        } else if (type.equals(INT)) {
            return 4;
        } else if (type.equals(UINT)) {
            return 4;
        } else if (type.equals(TRUNCATEDFLOAT)) {
            return 2;
        } else if (type.equals(SHORT)) {
            return 2;
        } else if (type.equals(USHORT)) {
            return 2;
        } else if (type.equals(BYTE)) {
            return 1;
        } else if (type.equals(UBYTE)) {
            return 1;
        } else {
            throw new IllegalArgumentException("bad type: " + type);
        }
    }
    
    /**
     * Make a BufferDataSet of the given type.
     * @param rank the rank (number of indeces) of the data.
     * @param reclen  length in bytes of each record.  This may be longer than len1*len2*len3*byteCount(type)
     * @param recoffs  byte offset of each record
     * @param len0   number of elements in the first index
     * @param len1   number of elements in the second index
     * @param len2   number of elements in the third index
     * @param len3 number of elements in the fourth index
     * @param buf   ByteBuffer containing the data, which should be at least recoffs + reclen * len0 bytes long.
     * @param type   BufferDataSet.INT, BufferDataSet.DOUBLE, etc...
     * @return BufferDataSet of the given type.
     */
    public static BufferDataSet makeDataSet( int rank, int reclen, int recoffs, int len0, int len1, int len2, int len3, ByteBuffer buf, Object type ) {
        BufferDataSet result;
        if ( rank==1 && len1>1 ) throw new IllegalArgumentException("rank is 1, but len1 is not 1");
        int nperRec=  len1 * len2 * len3; // assumes unused params are "1"
        if ( reclen < byteCount(type) ) {
            throw new IllegalArgumentException("reclen " + reclen + " is smaller than length of type "+type);
        } 
        if ( reclen < nperRec * byteCount(type) ) {
            throw new IllegalArgumentException("reclen " + reclen + " is smaller than length of " + nperRec +" type "+type);
        } 
        if ( reclen * len0 > buf.limit() ) {
            throw new IllegalArgumentException( String.format( "buffer length (%d bytes) is too small to contain data (%d %d-byte records)", buf.limit(), len0, reclen ) );
        }
        if ( type.equals(DOUBLE) ) {
            result=new DoubleDataSet( rank, reclen, recoffs, len0, len1, len2, len3, buf );
        } else if ( type.equals(FLOAT) ) {
            result=new  FloatDataSet( rank, reclen, recoffs, len0, len1, len2, len3, buf );
        } else if ( type.equals(VAX_FLOAT) ) {
            result= new VaxFloatDataSet( rank, reclen, recoffs, len0, len1, len2, len3, buf );
        } else if ( type.equals(LONG) ) {
            result=new  LongDataSet( rank, reclen, recoffs, len0, len1, len2, len3, buf );
        } else if ( type.equals(INT) ) {
            result=new  IntDataSet( rank, reclen, recoffs, len0, len1, len2, len3, buf );
        } else if ( type.equals(UINT) ) {
            result=new  UIntDataSet( rank, reclen, recoffs, len0, len1, len2, len3, buf );
        } else if ( type.equals(SHORT) ) {
            result=new  ShortDataSet( rank, reclen, recoffs, len0, len1, len2, len3, buf );
        } else if ( type.equals(USHORT) ) {
            result=new  UShortDataSet( rank, reclen, recoffs, len0, len1, len2, len3, buf );
        } else if ( type.equals(TRUNCATEDFLOAT) ) {
            result=new  TruncatedFloatDataSet( rank, reclen, recoffs, len0, len1, len2, len3, buf );
        } else if ( type.equals(BYTE) ) {
            result=new  ByteDataSet( rank, reclen, recoffs, len0, len1, len2, len3, buf );
        } else if (type.equals(UBYTE) ) {
            result=new UByteDataSet( rank, reclen, recoffs, len0, len1, len2, len3, buf );
        } else {
            throw new IllegalArgumentException("bad data type: "+type);
        }
        return result;
    }

    /**
     * Make a BufferDataSet of the given type.
     * @param rank the rank (number of indeces) of the data.
     * @param reclen  length in bytes of each record
     * @param recoffs  byte offset of each record
     * @param qube integer array of the number of elements in each index.  If rank is less than the number of elements, then ignore extra trailing elements.
     * @param buf  ByteBuffer containing the data, which should be at least recoffs + reclen * len0 bytes long.
     * @param type BufferDataSet.INT, BufferDataSet.DOUBLE, etc...
     * @return BufferDataSet of the given type.
     */
    public static BufferDataSet makeDataSet(  int rank, int reclen, int recoffs, int[] qube, ByteBuffer buf, Object type ) {
        int len0=1;
        int len1=1;
        int len2=1;
        int len3=1;
        if ( rank>0 ) len0= qube[0];
        if ( rank>1 ) len1= qube[1];
        if ( rank>2 ) len2= qube[2];
        if ( rank>3 ) len3= qube[3];
        return makeDataSet(rank, reclen, recoffs, len0, len1, len2, len3, buf, type );
    }
    
    /**
     * Create a new BufferDataSet of the given type.  Simple sanity checks are made, including:<ul>
     *   <li>rank 1 dataset may not have len1&gt;1.
     *   <li>reclen cannot be shorter than the byte length of the field type. 
     *   <li>buffer must have room for the dataset
     * </ul>
     * @param rank dataset rank
     * @param reclen  length in bytes of each record
     * @param recoffs  byte offet of each record
     * @param len0   number of elements in the first index
     * @param len1   number of elements in the second index
     * @param len2   number of elements in the third index
     * @param back   ByteBuffer containing the data, which should be at least reclen * len0 bytes long.
     * @param type   BufferDataSet.INT, BufferDataSet.DOUBLE, etc...
     */
    public BufferDataSet( int rank, int reclen, int recoffs, int len0, int len1, int len2, Object type, ByteBuffer back  ) {
        this( rank, reclen, recoffs, len0, len1, len2, 11, type, back );
    }

    /**
     * Create a new BufferDataSet of the given type.  Simple sanity checks are made, including:<ul>
     *   <li>rank 1 dataset may not have len1&gt;1.
     *   <li>reclen cannot be shorter than the byte length of the field type. 
     *   <li>buffer must have room for the dataset
     * </ul>
     * @param rank   dataset rank
     * @param reclen  length in bytes of each record
     * @param recoffs  byte offet of each record
     * @param len0   number of elements in the first index
     * @param len1   number of elements in the second index
     * @param len2   number of elements in the third index
     * @param len3   number of elements in the fourth index
     * @param back   ByteBuffer containing the data, which should be at least reclen * len0 bytes long.
     * @param type   BufferDataSet.INT, BufferDataSet.DOUBLE, etc...
     */
    public BufferDataSet( int rank, int reclen, int recoffs, int len0, int len1, int len2, int len3, Object type, ByteBuffer back  ) {
        if ( rank<0 ) {
            throw new IllegalArgumentException("rank cannot be negative");
        }
        if ( rank==1 && len1>1 ) throw new IllegalArgumentException("rank is 1, but len1 is not 1");
        if ( reclen < byteCount(type) ) throw new IllegalArgumentException("reclen " + reclen + " is smaller that length of type "+type);
        if ( reclen*len0 > back.limit() ) throw new IllegalArgumentException("buffer is too short (len="+back.limit()+") to contain data ("+len0+" "+reclen+" byte records)");
        this.back= back;
        this.rank = rank;
        this.reclen= reclen;
        this.recoffset= recoffs;
        this.len0 = len0;
        this.len1 = len1;
        this.len2 = len2;
        this.len3 = len3;
        this.type= type;
        this.fieldLen= byteCount(type);
        if ( rank>1 ) {
            putProperty( QDataSet.QUBE, Boolean.TRUE );
        }
        if ( reclen>0 && fieldLen>reclen ) { // negative reclen supported 9-bit floats.
            logger.warning( String.format( "field length (%d) is greater than record length (%d) for len0=%d.", (int)fieldLen, (int)reclen, (int)len0 ) );
        }
        if ( reclen>0 && ( back.remaining()< ( reclen*len0 ) ) ) {
            logger.warning( String.format( "back buffer is too short (len=%d) for %d records each reclen=%d.", (int)back.remaining(), (int)len0, (int)reclen ) );
        }
    }

    /**
     * create a rank 0 dataset backed by the given type.
     * @param type DOUBLE, FLOAT, UINT, etc
     * @return BufferDataSet of the given type.
     */
    public static BufferDataSet createRank0( Object type ) {
        int typeLen= byteCount(type);
        ByteBuffer buf= ByteBuffer.allocateDirect( typeLen );
        int recLen= typeLen;
        return makeDataSet( 0, recLen, 0, 1, 1, 1, 1, buf, type );
    }

    /**
     * create a rank 1 dataset backed by the given type.
     * @param type DOUBLE, FLOAT, UINT, etc
     * @param len0 length of the zeroth index
     * @return BufferDataSet of the given type.
     */
    public static BufferDataSet createRank1( Object type, int len0 ) {
        int typeLen= byteCount(type);
        ByteBuffer buf= ByteBuffer.allocateDirect( typeLen * len0 );
        int recLen= typeLen;
        return makeDataSet( 1, recLen, 0, len0, 1, 1, 1, buf, type );
    }    

    /**
     * create a rank 2 dataset backed by the given type.
     * @param type DOUBLE, FLOAT, UINT, etc
     * @param len0 length of the zeroth index
     * @param len1 length of the first index
     * @return BufferDataSet of the given type.
     */    
    public static BufferDataSet createRank2( Object type, int len0, int len1 ) {
        int typeLen= byteCount(type);
        ByteBuffer buf= ByteBuffer.allocateDirect( typeLen * len0 * len1 );
        int recLen= typeLen * len1;
        return makeDataSet( 2, recLen, 0, len0, len1, 1, 1, buf, type );
    }
    
    /**
     * create a rank 3 dataset backed by the given type.
     * @param type DOUBLE, FLOAT, UINT, etc
     * @param len0 length of the zeroth index
     * @param len1 length of the first index
     * @param len2 length of the second index
     * @return BufferDataSet of the given type.
     */
    public static BufferDataSet createRank3( Object type, int len0, int len1, int len2 ) {
        int typeLen= byteCount(type);
        ByteBuffer buf= ByteBuffer.allocateDirect( typeLen * len0 * len1 * len2 );
        int recLen= typeLen * len1 * len2;
        return makeDataSet( 3, recLen, 0, len0, len1, len2, 1, buf, type );
    }

    /**
     * create a rank 4 dataset backed by the given type.
     * @param type DOUBLE, FLOAT, UINT, etc
     * @param len0 length of the zeroth index
     * @param len1 length of the first index
     * @param len2 length of the second index
     * @param len3 length of the third index
     * @return BufferDataSet of the given type.
     */
    public static BufferDataSet createRank4( Object type, int len0, int len1, int len2, int len3 ) {
        int typeLen= byteCount(type);
        ByteBuffer buf= ByteBuffer.allocateDirect( typeLen * len0 * len1 * len2 * len3 );
        int recLen= typeLen * len1 * len2 * len3;
        return makeDataSet( 4, recLen, 0, len0, len1, len2, len3, buf, type );
    }

    private static BufferDataSet ddcopy(BufferDataSet ds) {
        
        ByteBuffer newback= ByteBuffer.allocateDirect(ds.back.limit());
        newback.order(ds.back.order());
        ds.copyTo(newback);
        
        newback.flip();
        
        BufferDataSet result = BufferDataSet.makeDataSet( ds.rank, ds.reclen, ds.recoffset, ds.len0, ds.len1, ds.len2, ds.len3, newback, ds.type );
        result.properties.putAll( Ops.copyProperties(ds) );

        return result;
    }
    
    /**
     * return a copy of the data.  If the data is a BufferDataSet, then a BufferDataSet
     * is used for the copy.  
     * 
     * Note this does not consider isMutable.  If the dataset is not mutable, then the
     * original data could be returned (probably).
     * 
     * @param ds any qube dataset.
     * @return a BufferDataSet copy of the dataset.
     */
    public static BufferDataSet copy( QDataSet ds ) {
        //TODO: this should check that the data is a qube.
        if ( ds instanceof BufferDataSet ) {
            return ddcopy( (BufferDataSet)ds );
        } else {
            return copy( DOUBLE, ds ); // strange type does legacy behavior.
        }
    }
    
    /**
     * Copy the dataset to an BufferDataSet only if the dataset is not already an BufferDataSet.
     * @param ds
     * @return a BufferDataSet.
     */
    public static BufferDataSet maybeCopy( QDataSet ds ) {
        if ( ds instanceof BufferDataSet ) {
            return (BufferDataSet)ds;
        } else {
            return copy(ds);
        }
    }    
    
    /**
     * return true if the dataset can be appended.  Note this assumes that the
     * same length, etc.  This just checks that we have the number of spare records
     * in the backing store.
     * @param ds
     * @return
     */
    public boolean canAppend( BufferDataSet ds ) {
        if ( ds.rank()!=this.rank ) throw new IllegalArgumentException("rank mismatch");
        if ( ds.len1!=this.len1 ) throw new IllegalArgumentException("len1 mismatch");
        if ( ds.len2!=this.len2 ) throw new IllegalArgumentException("len2 mismatch");
        if ( ds.len3!=this.len3 ) throw new IllegalArgumentException("len3 mismatch");
        if ( this.getType()!=ds.getType() ) {
            String s1,s2;
            s1= "" + this.getType();
            s2= "" + ds.getType();
            throw new IllegalArgumentException("backing type mismatch: "+ s2 + "["+ds.length()+",*] can't be appended to "+ s1 + "["+this.length()+",*]" );
        }
        int trec=  this.back.capacity() / ( byteCount(type) * this.len1 * this.len2 * this.len3 );
        
        return trec > ds.length() + this.len0;
        
    }
    
    /**
     * append the dataset with the same geometry but different number of records (zeroth dim)
     * to this.  An IllegalArgumentException is thrown when there is not enough room.  
     * See grow(newRecCount).
     * Not thread safe--we need to go through and make it so...
     * @param ds
     */
    public synchronized void append( BufferDataSet ds ) {
        if ( ds.rank()!=this.rank ) throw new IllegalArgumentException("rank mismatch");
        if ( ds.len1!=this.len1 ) throw new IllegalArgumentException("len1 mismatch");
        if ( ds.len2!=this.len2 ) throw new IllegalArgumentException("len2 mismatch");
        if ( ds.len3!=this.len3 ) throw new IllegalArgumentException("len3 mismatch");
        if ( this.type!=ds.type ) throw new IllegalArgumentException("backing type mismatch");

        int elementSizeBytes= byteCount(this.type);
        
        int myLength= elementSizeBytes * this.len0 * this.len1 * this.len2 * this.len3;
        int dsLength= elementSizeBytes * ds.len0 * ds.len1 * ds.len2 * ds.len3;

        if ( this.back.capacity()< ( recoffset + myLength + dsLength ) ) {
            throw new IllegalArgumentException("unable to append dataset, not enough room");
        } else {
            this.back.limit( recoffset + myLength + dsLength );
        }

        ByteBuffer dsBuffer= ds.back.duplicate(); // TODO: verify thread safety
        
        this.back.position( recoffset + myLength );
        this.back.limit( recoffset + myLength + dsLength );
        this.back.put( dsBuffer );
        this.back.flip();
        
        Units u1= SemanticOps.getUnits(this);
        Units u2= SemanticOps.getUnits(ds);
        if ( u1!=u2 ) {
            if ( u1 instanceof EnumerationUnits && u2 instanceof EnumerationUnits ) { // convert so the enumeration units are the same.
                for ( int i=myLength; i<myLength+dsLength; i++ ) {
                    double d= this.back.getDouble( i*elementSizeBytes );
                    d= ((EnumerationUnits)u1).createDatum( u2.createDatum(d).toString() ).doubleValue(u1);
                    this.back.putDouble( i*elementSizeBytes, d );
                }
            } else {
                UnitsConverter uc= UnitsConverter.getConverter(u2,u1); // convert so the time location units are the same (for example).
                for ( int i=myLength; i<myLength+dsLength; i++ ) {
                    Number nv=  uc.convert( ds.value(i) ) ;
                    this.putValue( i, nv.doubleValue() );
                }
            }
        }
        
        this.len0= this.len0 + ds.len0;

        properties.putAll( joinProperties( this, ds ) ); //TODO: verify

    }
    
    /**
     * append the two datasets.  The two datasets need only have convertible units, 
     * so for example two time arrays may be appended even if their units don't 
     * have the same base.  Only properties of the two datasets that do not change 
     * are preserved.
     * @param ths rank N dataset
     * @param ds rank N dataset of the same type and geometry as ths.
     * @return the dataset 
     */
    public static BufferDataSet append( BufferDataSet ths, BufferDataSet ds ) {
        if ( ths==null ) return ds;
        if ( ds==null ) throw new NullPointerException("ds is null");
        
        if ( ths.rank()==ds.rank()-1 ) {
            Units u= SemanticOps.getUnits(ths);
            ths= BufferDataSet.makeDataSet( ths.rank()+1, 1, 0, ths.len0, ths.len1, ths.len2, 1, ths.back, ths.type );
            ths.putProperty( QDataSet.UNITS,u);
        }
        if ( ths.rank()-1==ds.rank() ) {
            Units u= SemanticOps.getUnits(ds);
            ds= BufferDataSet.makeDataSet( ds.rank()+1, 1, 0, ds.len0, ds.len1, ds.len2, 1, ds.back, ds.type );
            ds.putProperty( QDataSet.UNITS,u);
        }
        if ( ds.rank()!=ths.rank ) throw new IllegalArgumentException("rank mismatch");
        if ( ds.len1!=ths.len1 ) throw new IllegalArgumentException("len1 mismatch");
        if ( ds.len2!=ths.len2 ) throw new IllegalArgumentException("len2 mismatch");
        if ( ds.len3!=ths.len3 ) throw new IllegalArgumentException("len3 mismatch");
        if ( ths.getType()!=ds.getType() ) throw new IllegalArgumentException("backing type mismatch");
        if ( ths.back.order()!=ds.back.order() ) throw new IllegalArgumentException("byte order (endianness) must be the same");

        int myLength= byteCount(ths.type) * ths.len0 * ths.len1 * ths.len2 * ths.len3;
        int dsLength= byteCount(ds.type) * ds.len0 * ds.len1 * ds.len2 * ds.len3;

        ByteBuffer newback= ByteBuffer.allocateDirect( myLength + dsLength );
        newback.order( ths.back.order() );
        
        newback.put( ths.back );
        newback.put( ds.back );
        newback.rewind(); // why not flip()?
                
        BufferDataSet result= BufferDataSet.makeDataSet( ths.rank, ths.reclen, 0, 
                ths.len0 + ds.len0, ths.len1, ths.len2, ths.len3, 
                newback, ths.type );
                
        Units u1= SemanticOps.getUnits(ths);
        Units u2= SemanticOps.getUnits(ds);
        if ( u1!=u2 ) {
            if ( u1 instanceof EnumerationUnits && u2 instanceof EnumerationUnits ) { // convert so the enumeration units are the same.
                for ( int i=myLength; i<myLength+dsLength; i++ ) {
                    double d= ths.value( i );
                    d= ((EnumerationUnits)u1).createDatum( u2.createDatum(d).toString() ).doubleValue(u1);
                    ths.putValue( i, d );
                }
            } else {
                UnitsConverter uc= UnitsConverter.getConverter(u2,u1); // convert so the time location units are the same (for example).
                for ( int i=myLength; i<myLength+dsLength; i++ ) {
                    Number nv=  uc.convert( ds.value(i) ) ;
                    ths.putValue( i, nv.doubleValue() );
                }
            }
        }
        
        result.properties.putAll( joinProperties( ths, ds ) );
        result.properties.put( QDataSet.UNITS, u1 ); // since we resolve units when they change (bug 3469219)

        return result;
    }


    /**
     * join the properties of the two datasets.  (for append, really...)
     * Note MONOTONIC assumes the ds will be added after ths.
     * 
     * @param ths 
     * @param ds
     * @return the two sets combined.
     */
    protected static Map joinProperties( BufferDataSet ths, BufferDataSet ds ) {
        Map result= new HashMap();
        for ( int i=0; i<ds.rank(); i++ ) {
            QDataSet thatDep= (QDataSet) ds.property( "DEPEND_"+i );
            if ( thatDep!=null && ( i==0 || thatDep.rank()>1 ) ) {
                QDataSet thisDep= (QDataSet) ths.property( "DEPEND_"+i );
                BufferDataSet djoin= copy( thisDep ); //TODO: reconcile types
                //if ( thatDep instanceof BufferDataSet ) {
                //    System.err.println("== DEPEND_0 ==");
                //    ((BufferDataSet)thatDep).about();
                //}
                BufferDataSet ddep1= thatDep instanceof BufferDataSet ? (BufferDataSet) thatDep : maybeCopy( thatDep );
                //((BufferDataSet)thatDep).about();
                djoin= append( djoin, ddep1 );
                //((BufferDataSet)djoin).about();
                result.put( "DEPEND_"+i, djoin );

            } else if ( thatDep!=null && thatDep.rank()==1 ) {
                //TODO: check properties equal.
                result.put( "DEPEND_"+i, thatDep );
            }
            QDataSet thatBundle= (QDataSet) ds.property( "BUNDLE_"+i );
            QDataSet thisBundle= (QDataSet) ths.property("BUNDLE_"+i );
            if ( i>0 && thatBundle!=null && thisBundle!=null ) {
                if ( thisBundle.length()!=thatBundle.length() ) {
                    throw new IllegalArgumentException("BUNDLE_"+i+" should be the same length to append, but they are not");
                }
                for ( int j=0; j<thatBundle.length(); j++ ) {
                    Units thatu= (Units)thatBundle.property( QDataSet.UNITS, j );
                    Units thisu= (Units)thisBundle.property( QDataSet.UNITS, j );
                    if ( thisu!=thatu ) {
                        throw new IllegalArgumentException("units in BUNDLE_"+i+" change...");
                    }
                }
                //TODO: other safety checks...
                result.put( "BUNDLE_"+i, thatBundle );
            }
        }
        String[] props;
        props= DataSetUtil.correlativeProperties();
        for ( int iprop= -1; iprop<props.length; iprop++ ) {
            String prop= iprop==-1 ? QDataSet.PLANE_0 : props[iprop];
            QDataSet w1= (QDataSet) ds.property( prop );
            if ( w1!=null ) {
                QDataSet dep0= (QDataSet) ths.property( prop );
                if ( dep0!=null ) {
                    BufferDataSet djoin=  copy( dep0 );
                    BufferDataSet dd1=  maybeCopy(w1);
                    djoin= append( djoin, dd1 );
                    result.put( prop, djoin );
                } else {
                    logger.log(Level.INFO, "dataset doesn''t have property \"{0}\" but other dataset does: {1}", new Object[]{prop, ths});
                }
            }
        }

        props= DataSetUtil.dimensionProperties();
        for (String prop : props) {
            Object value= ths.property(prop);
            if ( value!=null && value.equals(ds.property(prop) ) ) {
                result.put( prop, ths.property(prop) );
            }
        }
        // special handling for QDataSet.CADENCE, and QDataSet.MONOTONIC
        props= new String[] { QDataSet.CADENCE, QDataSet.BINS_1 };
        for (String prop : props) {
            Object o = ths.property(prop);
            if (o!=null && o.equals(ds.property(prop))) {
                result.put(prop, o);
            }
        }

        // special handling for monotonic property.
        Boolean m= (Boolean) ths.property( QDataSet.MONOTONIC );
        if ( m!=null && m.equals(Boolean.TRUE) && m.equals( ds.property( QDataSet.MONOTONIC ) ) ) {
            // check to see that result would be monotonic
            try {
                int[] fl1= DataSetUtil.rangeOfMonotonic( ths );
                int[] fl2= DataSetUtil.rangeOfMonotonic( ds );
                Units u1= SemanticOps.getUnits(ds);
                Units u2= SemanticOps.getUnits(ths);
                UnitsConverter uc= u2.getConverter(u1);
                if ( ds.value(fl2[0]) -  uc.convert( ths.value(fl1[1]) ) >= 0 ) { 
                    result.put( QDataSet.MONOTONIC, Boolean.TRUE );
                }
            } catch ( IllegalArgumentException ex ) {
                logger.fine("rte_1282463981: can't show that result has monotonic timetags because each dataset is not monotonic.");
            }
        }

        // special handling for cacheTag property.
        org.das2.datum.CacheTag ct0= (CacheTag) ths.property( QDataSet.CACHE_TAG );
        org.das2.datum.CacheTag ct1= (CacheTag) ds.property( QDataSet.CACHE_TAG );
        if ( ct0!=null && ct1!=null ) {
            // If cache tags are not adjacent, the range between them is included in the new tag.
            CacheTag newTag= null;
            try {
                newTag= CacheTag.append(ct0, ct1);
            } catch ( IllegalArgumentException ex ) {
                logger.fine( "append of two datasets that have CACHE_TAGs and are not adjacent, dropping CACHE_TAG" );
            }
            if ( newTag!=null ) {
                result.put( QDataSet.CACHE_TAG, newTag );
            }
        }

        // special handling of TYPICAL_MIN _MAX properties
        Number dmin0= (Number) ths.property(QDataSet.TYPICAL_MIN );
        Number dmax0= (Number) ths.property(QDataSet.TYPICAL_MAX );
        Number dmin1= (Number) ds.property(QDataSet.TYPICAL_MIN );
        Number dmax1= (Number) ds.property(QDataSet.TYPICAL_MAX );
        if ( dmin0!=null && dmin1!=null ) result.put( QDataSet.TYPICAL_MIN, Math.min( dmin0.doubleValue(), dmin1.doubleValue() ) );
        if ( dmax0!=null && dmax1!=null ) result.put( QDataSet.TYPICAL_MAX, Math.max( dmax0.doubleValue(), dmax1.doubleValue() ) );

        return result;
    }
    
    /**
     * Return the type for the given class.  Note that there is a type for
     * each native type (Byte,Short,Float,etc), but not a class for each type. 
     * (E.g. UBYTE is unsigned byte.)
     * @param c java class
     * @return DOUBLE,FLOAT,etc.
     */
    public static Object typeFor( Class c ) {
        Object result;
        if ( c==byte.class ) {
            result=BufferDataSet.BYTE;
        } else if ( c==short.class ) {
            result=BufferDataSet.SHORT;
        } else if ( c==int.class ) {
            result=BufferDataSet.INT;
        } else if ( c==long.class ) {
            result=BufferDataSet.LONG;
        } else if ( c==float.class ) {
            result=BufferDataSet.FLOAT;
        } else if ( c==double.class ) {
            result=BufferDataSet.DOUBLE;
        } else {
            throw new IllegalArgumentException("bad class type: "+c);
        }      
        return result;
    }
    
    /**
     * Copy to array of specific type.  For example, copy( DOUBLE, ds ) would return a copy
     * in a DoubleDataSet.
     * @param type the primitive type to use (e.g. double.class).
     * @param ds the data to copy.
     * @return BufferDataSet of specific type.
     */
    public static BufferDataSet copy( Object type, QDataSet ds ) {
        
        if ( ds instanceof BufferDataSet && ((BufferDataSet)ds).getType()==type ) return ddcopy( (BufferDataSet)ds );
        
        int rank= ds.rank();
        BufferDataSet result;

        switch (rank) {
            case 0:
                result= createRank0( type );
                result.putValue( ds.value() );
                break;
            case 1:
                result= createRank1( type, ds.length() );
                for ( int i=0; i<ds.length(); i++ ) {
                    result.putValue( i, ds.value(i) );
                }
                break;
            case 2:
                result= createRank2( type, ds.length(), ds.length(0) );
                int i0= ds.length()>0 ? ds.length(0) : -1;
                for ( int i=0; i<ds.length(); i++ ) {
                    if ( ds.length(i)!=i0 ) throw new IllegalArgumentException("Attempt to copy non-qube into ArrayDataSet which must be qube: "+ds );
                    for ( int j=0; j<ds.length(i); j++ ) {
                        result.putValue( i, j, ds.value(i,j) );
                    }
                }
                break;
            case 3:
                result= createRank3( type, ds.length(), ds.length(0), ds.length(0,0) );
                int i0_= ds.length()>0 ? ds.length(0) : -1;
                for ( int i=0; i<ds.length(); i++ ) {
                    if ( ds.length(i)!=i0_ ) throw new IllegalArgumentException("Attempt to copy non-qube into ArrayDataSet which must be qube: "+ds );
                    for ( int j=0; j<ds.length(i); j++ ) {
                        for ( int k=0; k<ds.length(i,j); k++ ) {
                            result.putValue( i, j, k, ds.value(i,j,k) );
                        }
                    }
                }
                break;
            case 4:
                result = createRank4( type, ds.length(), ds.length(0), ds.length(0,0), ds.length(0,0,0));
                for ( int i=0; i<ds.length(); i++ )
                    for ( int j=0; j<ds.length(i); j++ )
                        for ( int k=0; k<ds.length(i,j); k++ )
                            for ( int l=0; l<ds.length(i,j,k); l++ )
                                result.putValue( i, j, k, l, ds.value(i,j,k,l));
                break;

            default: 
                throw new IllegalArgumentException("bad rank");
        }
        result.properties.putAll( Ops.copyProperties(ds) );
        //result.checkFill();

        return result;

    }
    
    /**
     * grow the internal store so that append may be used to resize the 
     * dataset.  This simply grows the internal buffer, so for example length()
     * will return the same value after.
     * @param newRecCount the new record count, generally larger than the old rec count.
     */
    public void grow( int newRecCount ) {
        
        if ( newRecCount < len0 ) throw new IllegalArgumentException("new recsize for grow smaller than old");
        
        int newSize= newRecCount * len1 * len2 * len3 * byteCount(type);
        
        ByteBuffer lback= this.back.duplicate();
        lback.order( this.back.order() );
        int oldSize= len0 *  len1 * len2 * len3 * byteCount(type);

        if ( newSize<oldSize ) { // it's possible that the dataset already has a backing that can support this.  Check for this.
            return;
        }

        ByteBuffer newBack= ByteBuffer.allocateDirect( newSize );
        newBack.order( lback.order() );
        newBack.put(lback);
        
        lback.flip();
        newBack.flip();
                
        this.back= newBack;
    }

    /**
     * return the type of this dataset, for example BufferDataSet.INT, BufferDataSet.DOUBLE, etc... 
     * @return the type of this dataset.
     */
    public Object getType() {
        return this.type;
    }

    @Override
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
    
    /**
     * for internal use, verify that the indeces are all within bounds.
     * @param i0 the zeroth index.
     * @param i1 the first index
     * @param i2 the second index
     * @param i3 the third index
     * @see #RANGE_CHECK which is used to turn on range checking.
     */
    protected void rangeCheck(int i0, int i1, int i2, int i3) {
        if (i0 < 0 || i0 >= len0) {
            throw new IndexOutOfBoundsException("i0=" + i0 + " " + this.toString());
        }
        if (i1 < 0 || i1 >= len1) {
            throw new IndexOutOfBoundsException("i1=" + i1 + " " + this.toString());
        }
        if (i2 < 0 || i2 >= len2) {
            throw new IndexOutOfBoundsException("i2=" + i2 + " " + this.toString());
        }
        if (i3 < 0 || i3 >= len3) {
            throw new IndexOutOfBoundsException("i3=" + i3 + " " + this.toString());
        }
    }

    /**
     * return the offset, in bytes, of the element.
     * @return the offset, in bytes, of the element.
     */
    protected int offset( ) {
        if ( this.rank!=0 ) throw new IllegalArgumentException("rank error");
        return recoffset;
    }

    /**
     * return the offset, in bytes, of the element.  We do not check
     * the dataset rank, so that trim and slice may find the location of any record.
     * @param i0
     * @return the offset, in bytes, of the element.
     */
    protected int offset(int i0 ) {
        if (RANGE_CHECK) {
            rangeCheck(i0, 0, 0, 0 );
        }
        return recoffset + reclen * i0;
    }
        
    /**
     * return the offset, in bytes, of the element.
     * @param i0 first index
     * @param i1 second index
     * @return the offset, in bytes, of the element.
     */
    protected int offset(int i0, int i1 ) {
        if ( this.rank!=2 ) throw new IllegalArgumentException("rank error");
        if (RANGE_CHECK) {
            rangeCheck(i0, i1, 0, 0 );
        }        
        return  recoffset + reclen * i0 + i1 * fieldLen;
    }

    /**
     * return the offset, in bytes, of the element.
     * @param i0 first index
     * @param i1 second index
     * @param i2 third index
     * @return the offset, in bytes, of the element.
     */
    protected int offset(int i0, int i1, int i2) {
        if ( this.rank!=3 ) throw new IllegalArgumentException("rank error");
        if (RANGE_CHECK) {
            rangeCheck(i0, i1, i2, 0);
        }
        return recoffset + reclen * i0 + i1 * fieldLen * len2  + i2 * fieldLen ;
    }

    /**
     * return the offset, in bytes, of the element.
     * @param i0 first index
     * @param i1 second index
     * @param i2 third index
     * @param i3 fourth index
     * @return the offset, in bytes, of the element.
     */
    protected int offset(int i0, int i1, int i2, int i3 ) {
        if ( this.rank!=4 ) throw new IllegalArgumentException("rank error");
        if (RANGE_CHECK) {
            rangeCheck(i0, i1, i2, i3);
        }
        return recoffset + reclen * i0 + i1 * fieldLen * len2 * len3  + i2 * fieldLen * len3 + i3 * fieldLen ;
    }

    @Override
    public abstract double value();

    @Override
    public abstract double value(int i0);

    @Override
    public abstract double value(int i0, int i1);

    @Override
    public abstract double value(int i0, int i1, int i2);

    @Override
    public abstract double value(int i0, int i1, int i2, int i3);

    /**
     * provide a subset of the dataset.  Note that writes to the result dataset
     * will affect the original dataset.  TODO: correct this since it's a WriteableDataSet.
     * @param ist the first index 
     * @param ien the last index, exclusive.
     */
    @Override
    public QDataSet trim( int ist, int ien ) {
        int offset;
        if ( ist<len0 ) {
            offset= offset(ist);
        } else if ( ist==len0 && ien==len0 ) {
            offset= recoffset + reclen * ist;  // code duplicated to avoid index out of bounds error.
        } else {
            offset= offset(ist);
        }
        BufferDataSet result= makeDataSet( rank, reclen, offset, ien-ist, len1, len2, len3, back, type );
        DataSetUtil.putProperties( DataSetUtil.trimProperties( this, ist, ien ), result );
        return result;
    }

    /**
     * slice operator based on ArrayDataSet code.
     * @param i
     * @return
     */
    @Override
    public QDataSet slice(int i) {
        BufferDataSet result= makeDataSet( rank-1, byteCount(type)*len2*len3, offset(i), len1, len2, len3, 1, back, type );
        Map<String,Object> props= DataSetOps.sliceProperties0(i,DataSetUtil.getProperties(this));
        props= DataSetUtil.sliceProperties( this, i, props );
        DataSetUtil.putProperties( props, result );
        return result;
    }
    
    /**
     * dump the contents to this buffer into buf.  The buffer buf is 
     * left with its position at the end of the copied data.
     * @param buf
     */
    public void copyTo( ByteBuffer buf ) {
        ByteBuffer lback= this.back.duplicate(); // duplicate just the indeces, not the data
        lback.order(back.order());
        lback.position( recoffset );
        lback.mark();
        lback.limit( recoffset + reclen * len0 );
        buf.put( lback );
    }

    /**
     * copy the data to a writable buffer if it's not already writable.
     */
    protected synchronized void ensureWritable() {
        if ( this.isImmutable() ) {
            logger.warning("dataset has been marked as immutable, this will soon throw an exception");
        }
        if ( back.isReadOnly() ) {
            ByteBuffer wback= ByteBuffer.allocateDirect( back.capacity() );
            wback.order( back.order() );
            wback.put(back);
            back= wback;
        }
    }
    
    /*public abstract double putValue(int i0, double d );

    public abstract double putValue(int i0, int i1, double d );

    public abstract double putValue(int i0, int i1, int i2, double d );     */

    /**
     * estimate the jvmMemory occupied by this dataset, looking at the NIO buffer
     * to see if it is direct as has no JVM memory cost, or if it has been made into
     * an array.
     * @return the estimated number bytes that the dataSet occupies.
     */
    public int jvmMemory() {
        if ( back.isDirect() ) {
            return 0;
        } else if ( back.hasArray() ) {
            return back.array().length;
        } else {
            return 0; // not sure
        }
    }
    
    /**
     * print some info about this BufferDataSet.
     */
    public void about() {
        System.err.println("== "+this.toString() + "==");
        System.err.println("back="+this.back);
        System.err.println("recoffset="+this.recoffset);
        //QDataSet extent= Ops.extent(this);  // this is occasionally very slow. TODO: investigate
        //System.err.println("extent="+extent);
    }
}
