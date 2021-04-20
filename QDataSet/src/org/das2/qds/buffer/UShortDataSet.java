package org.das2.qds.buffer;

import java.nio.ByteBuffer;

/**
 * Unsigned Short.
 * @author jbf
 */
public class UShortDataSet extends BufferDataSet {

    public UShortDataSet(int rank, int reclen, int recoffs, int len0, int len1, int len2, int len3, ByteBuffer back ) {
        super(rank, reclen, recoffs, len0, len1, len2, len3, USHORT, back );
    }

    public double value() {
        double b= back.getShort(offset());
        return b < 0 ? b + 65536 : b;
    }

    public double value(int i0) {
        double b= back.getShort(offset(i0));
        return b < 0 ? b + 65536 : b;
    }

    public double value(int i0, int i1) {
        double b= back.getShort(offset(i0, i1));
        return b < 0 ? b + 65536 : b;
    }

    public double value(int i0, int i1, int i2) {
        double b= back.getShort(offset(i0, i1, i2));
        return b < 0 ? b + 65536 : b;
    }

    public double value(int i0, int i1, int i2, int i3) {
        double b= back.getShort(offset(i0, i1, i2, i3));
        return b < 0 ? b + 65536 : b;
    }
    
    public void putValue(double d) {
        ensureWritable();
        back.putShort( offset(), (short)( d > 32768 ? d - 65536 : d ) );
    }

    public void putValue(int i0, double d) {
        ensureWritable();
        back.putShort( offset(i0), (short)( d > 32768 ? d - 65536 : d ) );
    }

    public void putValue(int i0, int i1, double d) {
        ensureWritable();
        back.putShort( offset(i0, i1), (short)( d > 32768 ? d - 65536 : d ) );
    }

    public void putValue(int i0, int i1, int i2, double d) {
        ensureWritable();
        back.putShort( offset(i0, i1, i2), (short)( d > 32768 ? d - 65536 : d ) );
    }

    public void putValue(int i0, int i1, int i2, int i3, double d) {
        ensureWritable();
        back.putInt( offset(i0, i1, i2, i3), (short)( d > 32768 ? d - 65536 : d ) );
    }
}
