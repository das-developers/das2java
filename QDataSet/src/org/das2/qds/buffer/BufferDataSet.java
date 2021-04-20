/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.qds.buffer;

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
import org.das2.qds.AbstractDataSet;
import org.das2.qds.BDataSet;
import org.das2.qds.DataSetOps;
import org.das2.qds.DataSetUtil;
import org.das2.qds.FDataSet;
import org.das2.qds.IDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.SDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.WritableDataSet;
import org.das2.qds.ops.Ops;

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

    protected static final Logger logger= LoggerManager.getLogger( "qdataset.bufferdataset" ); 
    
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
     * the number of bytes between records.
     */
    int recStride;
    
    /**
     * the byte offset into each record
     */
    int recoffset;
    
    /**
     * the number of bytes of the field in each record
     */
    private int fieldLen;
    
    /**
     * the number of bytes between the fields in each record (for rank 2 and 
     * higher data).
     */
    private int fieldStride;
        
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
     * three-byte ints.
     */
    public final static Object INT24= "int24";
    
    /**
     * three-byte unsigned ints.
     */
    public final static Object UINT24= "uint24";
    
    /**
     * four-bit unsigned ints.
     */
    public final static Object NYBBLE= "nybble";
    
    /**
     * 8 byte signed longs.
     */
    public final static Object LONG= "long";
    /**
     * 4 byte signed integers.
     */
    public final static Object INT= "int";
    /**
     * 4 byte signed integers, INT is canonical but INTEGER should be accepted.
     */
    public final static Object INTEGER= "integer";
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
     * 1 byte unsigned byte.
     */
    public final static Object UBYTE= "ubyte";
    
    public static int bitCount( Object type ) {
        if ( type.equals(NYBBLE) ) {
            return 4; 
        } else {
            return byteCount( type ) * 8;
        }
    }
    
    /**
     * return the number of bytes of each type (double=8, etc).
     * @param type DOUBLE, FLOAT, UBYTE, TIME28, etc.
     * @return 8, 4, 1, etc.
     * @throws IllegalArgumentException for NYBBLE, which can only be used with bitCount.
     */
    public static int byteCount(Object type) {
        if (type.equals(DOUBLE)) {
            return 8;
        } else if (type.equals(FLOAT)) {
            return 4;
        } else if ( type.equals(VAX_FLOAT) ) {
            return 4;
        } else if ( type.equals(NYBBLE) ) {
            throw new IllegalArgumentException("NYBBLE must be used with bitCount and makeDataSetBits");
        } else if ( type.equals(INT24) ) {
            return 3; 
        } else if ( type.equals(UINT24) ) {
            return 3;
        } else if (type.equals(LONG)) {
            return 8;
        } else if (type.equals(INT)) {
            return 4;
        } else if (type.equals(INTEGER) ) {
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
        } else if (type.toString().startsWith("time") ) {
            return Integer.parseInt( type.toString().substring(4) );
        } else if (type.toString().startsWith("ascii") ) {
            return Integer.parseInt( type.toString().substring(5) );
        } else {
            throw new IllegalArgumentException("bad type: " + type);
        }
    }
    
    /**
     * support binary types that are not a multiple of 8 bits.  This was
     * added to support Nybbles, and 12-bit ints.
     * @param rank
     * @param reclenbits number of bits per record
     * @param recoffsbits number of bits offset.  Note this must be a multiple of 8, for now.
     * @param len0   number of elements in the first index
     * @param len1   number of elements in the second index
     * @param len2   number of elements in the third index
     * @param len3 number of elements in the fourth index
     * @param buf ByteBuffer containing the data, which should be at least recoffsbits/8 + reclenbits/8 * len0 bytes long.
     * @param type BufferDataSet.NYBBLE, etc
     * @return BufferDataSet of the given type.
     */
    public static BufferDataSet makeDataSetBits( int rank, int reclenbits, int recoffsbits, int len0, int len1, int len2, int len3, ByteBuffer buf, Object type ) {
        BufferDataSet result;
        if ( rank==1 && len1>1 ) throw new IllegalArgumentException("rank is 1, but len1 is not 1");
        int nperRec=  len1 * len2 * len3; // assumes unused params are "1"
        if ( reclenbits < bitCount(type) ) {
            throw new IllegalArgumentException("reclenbits " + reclenbits + " is smaller than length of type "+type);
        } 
        if ( reclenbits < nperRec * bitCount(type) ) {
            throw new IllegalArgumentException("reclenbits " + reclenbits + " is smaller than length of " + nperRec +" type "+type);
        } 
        if ( ( (long)(reclenbits) * len0 / 8 ) > buf.limit() ) {
            throw new IllegalArgumentException( String.format( "buffer length (%d bytes) is too small to contain data (%d %d-bit records)", buf.limit(), len0, reclenbits ) );
        }
        if ( type.equals( NYBBLE ) ) {
            result= new NybbleDataSet( rank, reclenbits, recoffsbits, len0, len1, len2, len3, buf );
        } else {
            return makeDataSet( rank, reclenbits/8, recoffsbits/8, len0, len1, len2, len3, buf, type);
        }
        return result;
        
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
        if ( reclen*8 < bitCount(type) ) {
            throw new IllegalArgumentException("reclen " + reclen + " is smaller than length of type "+type);
        } 
        if ( reclen*8 < nperRec * bitCount(type) ) {
            throw new IllegalArgumentException("reclen " + reclen + " is smaller than length of " + nperRec +" type "+type);
        } 
        if ( (long)(reclen) * len0 > buf.limit() ) {
            throw new IllegalArgumentException( String.format( "buffer length (%d bytes) is too small to contain data (%d %d-byte records)", buf.limit(), len0, reclen ) );
        }
        if ( type.equals(DOUBLE) ) {
            result=new DoubleDataSet( rank, reclen, recoffs, len0, len1, len2, len3, buf );
        } else if ( type.equals(FLOAT) ) {
            result=new  FloatDataSet( rank, reclen, recoffs, len0, len1, len2, len3, buf );
        } else if ( type.equals(VAX_FLOAT) ) {
            result= new VaxFloatDataSet( rank, reclen, recoffs, len0, len1, len2, len3, buf );
        } else if ( type.equals(INT24) ) {
            result= new Int24DataSet( rank, reclen, recoffs, len0, len1, len2, len3, buf );
        } else if ( type.equals(UINT24) ) {
            result= new UInt24DataSet( rank, reclen, recoffs, len0, len1, len2, len3, buf );
        } else if ( type.equals(NYBBLE) ) {
            result= new NybbleDataSet( rank, reclen, recoffs, len0, len1, len2, len3, buf );
        } else if ( type.equals(LONG) ) {
            result=new  LongDataSet( rank, reclen, recoffs, len0, len1, len2, len3, buf );
        } else if ( type.equals(INT) || type.equals(INTEGER) ) {
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
        } else if (type.toString().startsWith("time") ) {
            result= new TimeDataSet( rank, reclen, recoffs, len0, len1, len2, len3, buf, type );
        } else if (type.toString().startsWith("ascii") ) {
            result= new AsciiDataSet( rank, reclen, recoffs, len0, len1, len2, len3, buf, type );
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
     * @param qube integer array of the number of elements in each index. 
     *   If rank is less than the number of elements, then ignore extra 
     *   trailing elements.
     * @param buf  ByteBuffer containing the data, which should be at least 
     *   recoffs + reclen * len0 bytes long.
     * @param type BufferDataSet.INT, BufferDataSet.DOUBLE, etc...
     * @return BufferDataSet of the given type.
     */
    public static BufferDataSet makeDataSet(  
            int rank, int reclen, int recoffs, 
            int[] qube, ByteBuffer buf, Object type ) {
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
     * @param recoffs  byte offset of each record
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
        if ( reclen>0 && reclen*len0 > back.limit() ) throw new IllegalArgumentException("buffer is too short (len="+back.limit()+") to contain data ("+len0+" "+reclen+" byte records)");
        if ( len0<0 ) throw new IllegalArgumentException("len0 is negative: "+len0);
        this.back= back;
        this.rank = rank;
        this.reclen= reclen;
        this.recStride= this.reclen;
        this.recoffset= recoffs;
        this.len0 = len0;
        this.len1 = len1;
        this.len2 = len2;
        this.len3 = len3;
        this.type= type;
        this.fieldLen= byteCount(type);
        this.fieldStride= this.fieldLen;
        if ( rank>1 ) {
            putProperty( QDataSet.QUBE, Boolean.TRUE );
        }
        if ( reclen>0 && fieldLen>reclen ) { // negative reclen supported 9-bit floats.
            logger.warning( String.format( "field length (%d) is greater than record length (%d) for len0=%d.", (int)fieldLen, (int)reclen, (int)len0 ) );
        }
    }
    
    /**
     * allow clients to override the cadence of data.  By default, this is
     * just the number of bytes in each field.  
     * @param bytes number of bytes between field beginnings.
     */
    public void setFieldStride( int bytes ) {
        if ( this.isImmutable() ) {
            throw new IllegalArgumentException("dataset is immutable");
        }
        this.fieldStride= bytes;
    }
    
    /**
     * return the number of bytes to advance for each field.
     * @return the number of bytes to advance for each field.
     */
    public int getFieldStride() {
        return this.fieldStride;
    }
    
    /**
     * allow clients to override the cadece of the records.  By default, this
     * is the number of bytes in each record.
     * @param bytes number of bytes between record beginnings.
     */
    public void setRecordStride( int bytes ) {
        if ( this.isImmutable() ) {
            throw new IllegalArgumentException("dataset is immutable");
        }
        this.recStride= bytes;
    }
    
    /**
     * return the number of bytes to advance for each record.
     * @return the number of bytes to advance for each record.
     */
    public int getRecordStride() {
        return this.recStride;
    }
    
    /**
     * reset the number of records.
     * @param len0 the new number of records.
     */
    public void setLength(int len0) {
        if ( this.isImmutable() ) {
            throw new IllegalArgumentException("dataset is immutable");
        }
        this.len0= len0;
    }

    /**
     * reset the length (in fields) of each record of the rank 2 dataset.
     * @param len1 the length of each record of the rank 2 dataset.
     * @see Ops#decimateBufferDataSet(org.das2.qds.buffer.BufferDataSet, int, int) 
     */
    public void setLength1(int len1) {
        if ( this.isImmutable() ) {
            throw new IllegalArgumentException("dataset is immutable");
        }
        this.len1= len1;
    }
    
    /**
     * constructor units are in bytes.
     */
    public static final Object BYTES="bytes";
    
    /**
     * constructor units are in bits.
     */
    public static final Object BITS="bits";
        
    /**
     * Create a new BufferDataSet of the given type.  Simple sanity checks are made, including:<ul>
     *   <li>rank 1 dataset may not have len1&gt;1.
     *   <li>reclen cannot be shorter than the byte length of the field type. 
     *   <li>buffer must have room for the dataset
     * </ul>
     * @param rank   dataset rank
     * @param reclen  length in bytes/bits of each record.
     * @param recoffs  byte/bit offset of each record.  For bits this must be multiple of 8.
     * @param bitByte  either BufferDataSet.BYTES or BufferDataSet.BITS
     * @param len0   number of elements in the first index
     * @param len1   number of elements in the second index
     * @param len2   number of elements in the third index
     * @param len3   number of elements in the fourth index
     * @param back   ByteBuffer containing the data, which should be at least reclen * len0 bytes long (when reclen is in bytes).
     * @param type   BufferDataSet.INT, BufferDataSet.DOUBLE, etc...
     */
    public BufferDataSet( int rank, int reclen, int recoffs, Object bitByte, int len0, int len1, int len2, int len3, Object type, ByteBuffer back ) {
        if ( rank<0 ) {
            throw new IllegalArgumentException("rank cannot be negative");
        }
        if ( rank==1 && len1>1 ) throw new IllegalArgumentException("rank is 1, but len1 is not 1");
        if ( bitByte.equals(BufferDataSet.BITS) ) {
            if ( reclen < bitCount(type) ) throw new IllegalArgumentException("reclen " + reclen + " bytes is smaller that length of type "+type);
            if ( reclen*len0/8 > back.limit() ) throw new IllegalArgumentException("buffer is too short (len="+back.limit()+") to contain data ("+len0+" "+reclen+" bit records)");    
        } else {
            if ( reclen < byteCount(type) ) throw new IllegalArgumentException("reclen " + reclen + " is smaller that length of type "+type);
            if ( reclen*len0 > back.limit() ) throw new IllegalArgumentException("buffer is too short (len="+back.limit()+") to contain data ("+len0+" "+reclen+" byte records)");                
        }
        this.back= back;
        this.rank = rank;
        this.reclen= reclen;
        this.recoffset= recoffs;
        this.len0 = len0;
        this.len1 = len1;
        this.len2 = len2;
        this.len3 = len3;
        this.type= type;
        this.fieldLen= bitCount(type)/8;
        if ( reclen>0 && fieldLen>reclen ) { // negative reclen supported 9-bit floats.
            logger.warning( String.format( "field length (%d) is greater than record length (%d) for len0=%d.", (int)fieldLen, (int)reclen, (int)len0 ) );
        }
        int n= bitByte==BITS ? 8 : 1;
        if ( reclen>0 && ( back.remaining()< ( reclen*len0/n ) ) ) {
            logger.warning( String.format( "back buffer is too short (len=%d) for %d records each reclen=%d.", (int)back.remaining(), (int)len0, (int)reclen ) );
        }    
        if ( rank>1 ) {
            putProperty( QDataSet.QUBE, Boolean.TRUE );
        }
    }
    
    /**
     * create a dataset backed by the given type.
     * @param rank the rank of the data
     * @param type DOUBLE, FLOAT, UINT, etc
     * @param len0 number of records (ignored for rank 0).
     * @param size size of each record
     * @return BufferDataSet of the given type.
     */
    public static BufferDataSet create( int rank, Object type, int len0, int[] size ) {
        switch( rank )  {
            case 0: return createRank0(type);
            case 1: return createRank1(type,len0);
            case 2: return createRank2(type,len0,size[0]);
            case 3: return createRank3(type,len0,size[0],size[1]);
            case 4: return createRank4(type,len0,size[0],size[1],size[2]);
            default: throw new IllegalArgumentException("rank error: "+rank);
        }
    }
    
    /**
     * create a rank 0 dataset backed by the given type.
     * @param type DOUBLE, FLOAT, UINT, etc
     * @return BufferDataSet of the given type.
     */
    public static BufferDataSet createRank0( Object type ) {
        int typeLen= byteCount(type);
        ByteBuffer buf= checkedAllocateDirect( typeLen );
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
        if ( (long)typeLen * len0 > Integer.MAX_VALUE ) {
            throw new IllegalArgumentException("request is too large to allocate (>2147483647)");
        }
        ByteBuffer buf= checkedAllocateDirect( typeLen * len0 );
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
        if ( (long)typeLen * len0 * len1 > Integer.MAX_VALUE ) {
            throw new IllegalArgumentException("request is too large to allocate (>2147483647)");
        }
        ByteBuffer buf= checkedAllocateDirect( typeLen * len0 * len1 );
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
        if ( (long)typeLen * len0 * len1 * len2 > Integer.MAX_VALUE ) {
            throw new IllegalArgumentException("request is too large to allocate (>2147483647)");
        }
        ByteBuffer buf= checkedAllocateDirect( typeLen * len0 * len1 * len2 );
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
        if ( (long)typeLen * len0 * len1 * len2 * len3 > Integer.MAX_VALUE ) {
            throw new IllegalArgumentException("request is too large to allocate (>2147483647)");
        }
        ByteBuffer buf= checkedAllocateDirect( typeLen * len0 * len1 * len2 * len3 );
        int recLen= typeLen * len1 * len2 * len3;
        return makeDataSet( 4, recLen, 0, len0, len1, len2, len3, buf, type );
    }

    private static BufferDataSet ddcopy(BufferDataSet ds) {
        
        ds= ds.compact(); //TODO: copy then copy again
        
        ByteBuffer newback= checkedAllocateDirect(ds.back.limit());
        newback.order(ds.back.order());
        ds.copyTo(newback);
        
        newback.flip();
        newback.limit( newback.capacity() );
        
        BufferDataSet result = BufferDataSet.makeDataSet( ds.rank, ds.reclen, ds.recoffset, ds.len0, ds.len1, ds.len2, ds.len3, newback, ds.type );
        result.properties.putAll( Ops.copyProperties(ds) );

        return result;
    }
    
    /**
     * return a copy of the data.  If the data is a BufferDataSet, then a new BufferDataSet
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
            return copy( guessBackingStore(ds), ds ); // strange type does legacy behavior.
        }
    }
    
    /**
     * guess the type of the backing store, returning double.class
     * if it cannot be determined.
     * @param ds the dataset
     * @return the backing store class, one of double.class, float.class, etc.
     */
    public static Object guessBackingStore( QDataSet ds ) {
        if ( ds instanceof BDataSet || ds instanceof ByteDataSet ) {
            return BYTE;
        } else if ( ds instanceof SDataSet || ds instanceof ShortDataSet ) {
            return SHORT;
        } else if ( ds instanceof IDataSet || ds instanceof IntDataSet ) {
            return INT;
        } else if ( ds instanceof FDataSet || ds instanceof FloatDataSet ) {
            return FLOAT;
        } else {
            return DOUBLE;
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
     * @param ds dataset of the same rank and len1, len2, and len3.
     * @return true if the dataset can be appended.
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
    
    private static long gcCounter= 0;
    
    /**
     * -1 means check; 0 means no; 1 means yes, do allocate outside of the JVM memory.
     */
    private static int allocateDirect= -1;
    
    /**
     * return 1 if direct allocate should be used, 0 if not.  
     * Direct allocations are memory allocations outside of the JVM heap memory.
     * (The internal variable has a -1 initial state, which is why this is
     * not boolean.)  This looks for 32bit Javas, and if more than 1/2 Gig is 
     * being used then it will allocate direct.  This is because 32bit Javas
     * cannot access any memory outside of 1Gig.
     * @return 1 or 0 if direct allocations should not be made.
     * @see https://sourceforge.net/p/autoplot/bugs/1395/
     * @see http://stackoverflow.com/questions/807263/how-do-i-detect-which-kind-of-jre-is-installed-32bit-vs-64bit
     * @see http://stackoverflow.com/questions/3651737/why-the-odd-performance-curve-differential-between-bytebuffer-allocate-and-byt "How ByteBuffer works and why Direct (Byte)Buffers are the only truly useful now"
     */
    public static int shouldAllocateDirect() {
        int result;    
        String s= System.getProperty("sun.arch.data.model");
        long maxMemoryBytes= Runtime.getRuntime().maxMemory();
        boolean moreThanHalfOfGig= maxMemoryBytes > 500000000;
        if ( s==null ) { // GNU 1.5? 
            s= System.getProperty("os.arch");
            if ( s.contains("64") ) {
                result= 1;
            } else { // 32bit
                if ( moreThanHalfOfGig ) {
                    result= 0;
                } else {
                    result= 1;
                }
            }
        } else {
            if ( s.equals("32") ) {
                if ( moreThanHalfOfGig ) {
                    result= 0;
                } else {
                    result= 1;
                }
            } else {
                result= 1;
            }
        }       
        return result;
    }
    
    /**
     * There's a known bug with NIO where data outside of the heap is not released
     * until the Java objects are garbage collected, which may not happen 
     * soon enough, because they are small.  This catches the error and calls
     * a System.gc if necessary.  This also keeps track of allocations and calls
     * an explicit GC every 100MB allocated.
     * 
     * This may fall back to allocating data within the heap, for example when
     * a 32 bit JVM is used. 
     * 
     * See https://sourceforge.net/p/autoplot/bugs/1395/, and
     * http://stackoverflow.com/questions/1854398/how-to-garbage-collect-a-direct-buffer-java
     * http://stackoverflow.com/questions/1744533/jna-bytebuffer-not-getting-freed-and-causing-c-heap-to-run-out-of-memory/1775542#1775542
     * 
     * @param capacity
     * @return the ByteBuffer result of ByteBuffer.allocateDirect.
     */
    private static ByteBuffer checkedAllocateDirect( int capacity ) {
        
        if ( allocateDirect==-1 ) { 
            allocateDirect= shouldAllocateDirect();
        }
        
        if ( allocateDirect==0 ) {
            return ByteBuffer.allocate(capacity);
        }
        
        ByteBuffer result;
        gcCounter+= capacity;
        try {
            result= ByteBuffer.allocateDirect( capacity );
            return result;
        } catch ( java.lang.OutOfMemoryError ex ) {
            logger.log(Level.FINE, "out of memory error handled: gcCounter={0}", gcCounter);
            System.gc();
            gcCounter=capacity;
            try {
                result= ByteBuffer.allocate( capacity );
                return result;
            } catch ( java.lang.OutOfMemoryError ex2 ) {
                logger.warning("out of memory fall back to heap allocate");
                result= ByteBuffer.allocate( capacity ); // fall back to allocate from heap
                return result;
            }
        }
    }
    
    /**
     * append the dataset with the same geometry but different number of records (zeroth dim)
     * to this.  An IllegalArgumentException is thrown when there is not enough room.  
     * See grow(newRecCount).
     * Not thread safe--we need to go through and make it so...
     * @param ds
     * @see #grow(int) 
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
        
        if ( this.len1 * this.len2 * this.len3 * byteCount(this.type) < this.reclen ) {
            throw new IllegalArgumentException("dataset must be compact");
        }
        
        if ( ds.len1 * ds.len2 * ds.len3 * byteCount(ds.type) < ds.reclen ) {
            BufferDataSet ds2= ds.compact();
            ds= ds2;
        }
        
        if ( this.back.capacity()< ( recoffset + myLength + dsLength ) ) {
            throw new IllegalArgumentException("unable to append dataset, not enough room");
        } else {
            this.back.limit( recoffset + myLength + dsLength );
        }

        ByteBuffer dsBuffer= ds.back.duplicate(); // TODO: verify thread safety
        
        int recLenBytes= ds.len1 * ds.len2 * ds.len3 * byteCount(type);
        if ( this.reclen < ds.reclen || this.recoffset!=0 || ds.recoffset!=0 ) { // there's a lot of data we aren't reading, we need to compact the data.
            ByteBuffer lback= ds.back.duplicate();
            this.back.position( recoffset + myLength );
            this.back.limit( recoffset + myLength + dsLength );
            for ( int i=0; i<len0; i++ ) {
                int recStartBytes= ds.offset(i);
                lback.limit(recStartBytes+recLenBytes);
                lback.position(recStartBytes);
                this.back.put( lback );
            }
        } else {
            this.back.position( recoffset + myLength );
            this.back.limit( recoffset + myLength + dsLength );
            this.back.put( dsBuffer );
            this.back.flip();
        }
        
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
            ths= BufferDataSet.makeDataSet( ths.rank()+1, ths.reclen*ths.len0, 0, 1, ths.len0, ths.len1, ths.len2, ths.back, ths.type );
            ths.putProperty( QDataSet.UNITS,u);
        }
        if ( ths.rank()-1==ds.rank() ) {
            Units u= SemanticOps.getUnits(ds);
            ds= BufferDataSet.makeDataSet( ds.rank()+1, ds.reclen*ds.len0, 0, 1, ds.len0, ds.len1, ds.len2, ds.back, ds.type );
            ds.putProperty( QDataSet.UNITS,u);
        }
        if ( ds.rank()!=ths.rank ) throw new IllegalArgumentException("rank mismatch");
        if ( ds.len1!=ths.len1 ) throw new IllegalArgumentException("len1 mismatch");
        if ( ds.len2!=ths.len2 ) throw new IllegalArgumentException("len2 mismatch");
        if ( ds.len3!=ths.len3 ) throw new IllegalArgumentException("len3 mismatch");
        if ( !ths.getType().equals(ds.getType()) ) throw new IllegalArgumentException("backing type mismatch"); // time21
        if ( ths.back.order()!=ds.back.order() ) throw new IllegalArgumentException("byte order (endianness) must be the same");

        int myLength= ths.len0 * ths.len1 * ths.len2 * ths.len3 * byteCount(ths.type);
        int dsLength= ds.len0 * ds.len1 * ds.len2 * ds.len3 * byteCount(ds.type);

        if ( ths.len1 * ths.len2 * ths.len3 * byteCount(ths.type) < ths.reclen ) {
            ths= ths.compact();
        }
        
        if ( ds.len1 * ds.len2 * ds.len3 * byteCount(ds.type) < ds.reclen ) {
            ds= ds.compact();
        }
        ByteBuffer newback= checkedAllocateDirect( myLength + dsLength );
        newback.order( ths.back.order() );
        
        ByteBuffer back2= ths.back.duplicate();
        back2.limit( myLength );
        back2.rewind();
        newback.put( back2 );
        ByteBuffer ds2= ds.back.duplicate();
        ds2.limit(dsLength);
        ds2.rewind();
        newback.put( ds2 );
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

        result.fieldStride= ths.fieldStride;
        result.recStride= ths.recStride;
        
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
    protected static Map<String,Object> joinProperties( BufferDataSet ths, BufferDataSet ds ) {
        Map<String,Object> result= new HashMap<>();
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
                if ( result.get( "DEPEND_"+i)!=null ) {
                    //TODO: check properties equal.
                } else {
                    result.put( "DEPEND_"+i, thatDep );
                }
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
     * @see #append(org.das2.qds.buffer.BufferDataSet) 
     */
    public void grow( int newRecCount ) {
        
        if ( newRecCount < len0 ) throw new IllegalArgumentException("new recsize for grow smaller than old");
        
        int newSize= newRecCount * len1 * len2 * len3 * byteCount(type);
        
        ByteBuffer lback= this.back.duplicate(); // note this does not copy the data!
        lback.order( this.back.order() );
        int oldSize= len0 *  len1 * len2 * len3 * byteCount(type);

        if ( newSize<oldSize ) { // it's possible that the dataset already has a backing that can support this.  Check for this.
            return;
        }
        
        ByteBuffer newBack= checkedAllocateDirect( newSize );
        newBack.order( lback.order() );

        int recLenBytes= len1 * len2 * len3 * byteCount(type);
        if ( recLenBytes < reclen || recoffset!=0 ) { // there's a lot of data we aren't reading, we need to compact the data.
            for ( int i=0; i<len0; i++ ) {
                int recStartBytes= offset(i);
                lback.limit(recStartBytes+recLenBytes);
                lback.position(recStartBytes);
                newBack.put(lback);
            }
        } else {
            newBack.put(lback);
        }
        
        newBack.flip();
                
        this.back= newBack;
        this.recoffset= 0;
        
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
        return recoffset + recStride * i0;
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
        return recoffset + recStride * i0 
                + fieldStride * i1;
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
        return recoffset + recStride * i0 
                + fieldStride * len2 * i1 
                + fieldStride * i2;
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
        return recoffset + recStride * i0 
                + fieldStride * len2 * len3 * i1  
                + fieldStride * len3 * i2 
                + fieldStride * i3;
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
        result.fieldStride= this.fieldStride;
        result.recStride= this.recStride;
        return result;
    }

    @Override
    public QDataSet slice(int i) {
        BufferDataSet result= makeDataSet( rank-1, byteCount(type)*len2*len3, offset(i), len1, len2, len3, 1, back, type );
        Map<String,Object> props= DataSetOps.sliceProperties0(i,DataSetUtil.getProperties(this));
        props= DataSetUtil.sliceProperties( this, i, props );
        DataSetUtil.putProperties( props, result );
        result.fieldStride= this.fieldStride;
        return result;
    }
    
    /**
     * dump the contents to this buffer into buf.  The buffer buf is 
     * left with its position at the end of the copied data.
     * @param buf
     */
    private void copyTo( ByteBuffer buf ) {
        if ( isCompact() ) {
            ByteBuffer lback= this.back.duplicate(); // duplicate just the indeces, not the data
            lback.order(back.order());
            lback.position( 0 ); // bugfix should be 0, see only usage
            lback.mark();
            lback.limit( reclen * len0 );
            buf.put( lback );
        } else {
            BufferDataSet c= this.compact();
            c.copyTo(buf);
        }
        
    }

    /**
     * copy the data to a writable buffer if it's not already writable.
     */
    protected synchronized void ensureWritable() {
        if ( this.isImmutable() ) {
            logger.warning("dataset has been marked as immutable, this will soon throw an exception");
        }
        if ( back.isReadOnly() ) {
            ByteBuffer wback= checkedAllocateDirect( back.capacity() );
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

    /**
     * return the Java type that is capable of containing elements of this dataset.
     * For unsigned types, the next Java class is used, for example int.class is
     * used to store unsigned shorts.
     * @return double.class, float.class, long.class, etc.
     */
    public Class getCompatibleComponentType() {
        Object t= getType();
        if ( t==DOUBLE ) {
            return double.class;
        } else if ( t==FLOAT ) {
            return float.class;
        } else if ( t==LONG ) {
            return long.class;
        } else if ( t==UINT ) {
            return long.class;
        } else if ( t==INT ) {
            return int.class;
        } else if ( t==USHORT ) {
            return int.class;
        } else if ( t==SHORT ) {
            return short.class;
        } else if ( t==UBYTE ) {
            return short.class;
        } else if ( t==BYTE ) {
            return byte.class;
        } else {
            return double.class;
        }
        
    }

    /**
     * returns true if the dataset is compact, meaning that there
     * are no gaps between records, and no byte offset.
     * @return true if the dataset is compact
     */
    public boolean isCompact() {
        int recLenBytes= len1 * len2 * len3 * byteCount(type) ;
        return recLenBytes==this.reclen && this.recoffset==0;
    }
    
    /**
     * get ride of extra spaces between records.
     * @return new BufferDataSet without gaps.
     */
    public BufferDataSet compact() {
        ByteBuffer lback= this.back.duplicate();
        lback.order(this.back.order());
        
        int recLenBytes= len1 * len2 * len3 * byteCount(type) ;
        ByteBuffer newBuf= ByteBuffer.allocate( len0 * recLenBytes );
        newBuf.order(this.back.order());
        for ( int i=0; i<len0; i++ ) {
            int recStartBytes= offset(i);
            if ( recStartBytes+recLenBytes > back.capacity() ) {
                logger.info("something is wrong");
            }
            lback.limit(recStartBytes+recLenBytes);
            lback.position(recStartBytes);
            newBuf.put( lback );
        }
        newBuf.flip();
        BufferDataSet result= makeDataSet( this.rank, recLenBytes, 0, len0, len1, len2, len3, newBuf, type );
        result.properties.putAll( Ops.copyProperties(this) );
        result.fieldStride= this.fieldStride;
        result.recStride= this.recStride;
        return result;
    }
}
