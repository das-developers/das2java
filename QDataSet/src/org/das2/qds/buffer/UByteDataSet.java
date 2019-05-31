package org.das2.qds.buffer;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Unsigned Byte.  This also contains a method
 * for extracting the data at a location as a string.
 * @author jbf
 */
public class UByteDataSet extends BufferDataSet {

    public UByteDataSet(int rank, int reclen, int recoffs, int len0, int len1, int len2, int len3, ByteBuffer back ) {
        super(rank, reclen, recoffs, len0, len1, len2, len3, UBYTE, back );
    }

    public double value() {
        byte b= back.get(offset());
        return b < 0 ? b + 256 : b;
    }

    public double value(int i0) {
        byte b= back.get(offset(i0));
        return b < 0 ? b + 256 : b;
    }

    public double value(int i0, int i1) {
        byte b= back.get(offset(i0, i1)); 
        return b < 0 ? b + 256 : b;
    }

    public double value(int i0, int i1, int i2) {
        byte b= back.get(offset(i0, i1, i2));
        return b < 0 ? b + 256 : b;
    }
    
    public double value(int i0, int i1, int i2, int i3) {
        byte b= back.get(offset(i0, i1, i2, i3));
        return b < 0 ? b + 256 : b;
    }

    public void putValue(double d) {
        ensureWritable();
        back.put( offset(), (byte)( d > 128 ? d - 256 : d ) );
    }

    public void putValue(int i0, double d) {
        ensureWritable();
        back.put( offset(i0), (byte)( d > 128 ? d - 256 : d ) );
    }

    public void putValue(int i0, int i1, double d) {
        ensureWritable();
        back.put( offset(i0, i1), (byte)( d > 128 ? d - 256 : d ) );
    }

    public void putValue(int i0, int i1, int i2, double d) {
        ensureWritable();
        back.put( offset(i0, i1, i2), (byte)( d > 128 ? d - 256 : d ) );
    }

    public void putValue(int i0, int i1, int i2, int i3, double d) {
        ensureWritable();
        back.put( offset(i0, i1, i2, i3), (byte)( d > 128 ? d - 256 : d ) );
    }
    
    /**
     * efficiently return the data contained in the bytes presuming a string
     * encoding.  This is useful when the data is mostly binary but parts
     * are text data.  This is typically used with trim to extract text portions
     * of data.  This code shows how the length of each record in a U.S.G.S.
     * DEM file is extracted:
     * <pre>{@code
     * from java.nio.charset import Charset
     * usascii= Charset.forName( 'US-ASCII' )
     * ds=getDataSet( 'vap+bin:%s?byteOffset=858&byteLength=6' % fil.toURI() )
     * from java.lang import Integer
     * ncol= Integer.parseUnsignedInt( ds.collectString( usascii ).strip() )
     * print ncol
     * }</pre>
     * @see https://github.com/autoplot/dev/blob/master/demos/20190529/readUsgsDem.jyds
     * @param charset
     * @return the string
     */
    public String collectString( Charset charset ) {
        if ( this.rank!=1 ) throw new IllegalArgumentException("data must be rank 1");
        int st= offset(0);
        if ( back.hasArray() ) {
            return new String( back.array(), st, st+this.len0, charset );
        } else {
            byte[] array= new byte[this.length()];
            back.position( st );
            back.limit( st+this.len0 );
            back.get(array);
            return new String( array, charset );
        }
    }
}
