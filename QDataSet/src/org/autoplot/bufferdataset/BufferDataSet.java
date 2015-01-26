/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.autoplot.bufferdataset;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.logging.Logger;
import org.das2.util.LoggerManager;
import org.virbo.dataset.AbstractDataSet;
import org.virbo.dataset.ArrayDataSet;
import static org.virbo.dataset.ArrayDataSet.copy;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.JoinDataSet;
import org.virbo.dataset.QDataSet;
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

    public final static Object DOUBLE= "double";
    public final static Object FLOAT= "float";
    public final static Object TRUNCATEDFLOAT= "truncatedfloat"; // 16 bit real that has exponent like a FLOAT but mantissa precision is reduced.
    public final static Object VAX_FLOAT= "vaxfloat";
    public final static Object LONG= "long";
    public final static Object INT= "int";
    public final static Object UINT= "uint";
    public final static Object SHORT= "short";
    public final static Object USHORT= "ushort";
    public final static Object BYTE= "byte";
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
        return makeDataSet( 0, recLen, 0, len0, 1, 1, 1, buf, type );
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
        int recLen= typeLen;
        return makeDataSet( 0, recLen, 0, len0, len1, 1, 1, buf, type );
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
        int recLen= typeLen;
        return makeDataSet( 0, recLen, 0, len0, len1, len2, 1, buf, type );
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
        int recLen= typeLen;
        return makeDataSet( 0, recLen, 0, len0, len1, len2, len3, buf, type );
    }

    private static BufferDataSet ddcopy(BufferDataSet ds) {
        
        ByteBuffer newback= ByteBuffer.allocateDirect(ds.back.limit());
        ds.copyTo(newback);
        
        BufferDataSet result = BufferDataSet.makeDataSet( ds.rank, ds.reclen, ds.recoffset, ds.len0, ds.len1, ds.len2, ds.len3, newback, ds.type );
        result.properties.putAll( Ops.copyProperties(ds) );

        return result;
    }
    
    public static BufferDataSet copy( QDataSet ds ) {
        //TODO: this should check that the data is a qube.
        if ( ds instanceof BufferDataSet ) {
            return ddcopy( (BufferDataSet)ds );
        } else {
            return copy( DOUBLE, ds ); // strange type does legacy behavior.
        }
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
     * dump the contents to this buffer.
     * @param buf
     */
    public void copyTo( ByteBuffer buf ) {
        ByteBuffer lback= this.back.duplicate(); // duplicate just the indeces, not the data
        lback.order(back.order());
        lback.position( recoffset );
        lback.mark();
        lback.limit( recoffset + reclen * len0 );
        buf.put( this.back );
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
}
