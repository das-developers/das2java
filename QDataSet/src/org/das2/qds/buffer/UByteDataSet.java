package org.das2.qds.buffer;

import java.nio.ByteBuffer;

/**
 * Unsigned Byte.
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
}
