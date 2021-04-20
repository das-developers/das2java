package org.das2.qds.buffer;

import java.nio.ByteBuffer;

/**
 * Unsigned Integer.
 * @author jbf
 */
public class UIntDataSet extends BufferDataSet {

    public UIntDataSet(int rank, int reclen, int recoffs, int len0, int len1, int len2, int len3, ByteBuffer back ) {
        super(rank, reclen, recoffs, len0, len1, len2, len3, UINT, back );
    }

    public double value() {
        double b= back.getInt(offset());
        return b < 0 ? b + 4294967296. : b;
    }

    public double value(int i0) {
        double b= back.getInt(offset(i0));
        return b < 0 ? b + 4294967296. : b;
    }

    public double value(int i0, int i1) {
        double b= back.getInt(offset(i0, i1));
        return b < 0 ? b + 4294967296. : b;
    }

    public double value(int i0, int i1, int i2) {
        double b= back.getInt(offset(i0, i1, i2));
        return b < 0 ? b + 4294967296. : b;
    }
    
    public double value(int i0, int i1, int i2, int i3) {
        double b= back.getInt(offset(i0, i1, i2, i3));
        return b < 0 ? b + 4294967296. : b;
    }

    public void putValue(double d) {
        ensureWritable();
        back.putInt( offset(), (int)( d > 2147483648. ? d - 4294967296. : d ) );
    }

    public void putValue(int i0, double d) {
        ensureWritable();
        back.putInt( offset(i0), (int)( d > 2147483648. ? d - 4294967296. : d ) );
    }

    public void putValue(int i0, int i1, double d) {
        ensureWritable();
        back.putInt( offset(i0, i1), (int)( d > 2147483648. ? d - 4294967296. : d ) );
    }

    public void putValue(int i0, int i1, int i2, double d) {
        ensureWritable();
        back.putInt( offset(i0, i1, i2), (int)( d > 2147483648. ? d - 4294967296. : d ) );
    }

    public void putValue(int i0, int i1, int i2, int i3, double d) {
        ensureWritable();
        back.putInt( offset(i0, i1, i2, i3), (int)( d > 2147483648. ? d - 4294967296. : d ) );
    }
}
