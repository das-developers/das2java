package org.das2.qds.buffer;

import java.nio.ByteBuffer;

public class LongDataSet extends BufferDataSet {

    public LongDataSet(int rank, int reclen, int recoffs, int len0, int len1, int len2, int len3, ByteBuffer back ) {
        super(rank, reclen, recoffs, len0, len1, len2, len3, LONG, back );
    }

    public double value() {
        return back.getLong(offset());
    }

    public double value(int i0) {
        return back.getLong(offset(i0));
    }

    public double value(int i0, int i1) {
        return back.getLong(offset(i0, i1));
    }

    public double value(int i0, int i1, int i2) {
        return back.getLong(offset(i0, i1, i2));
    }

    public double value(int i0, int i1, int i2, int i3) {
        return back.getLong(offset(i0, i1, i2, i3));
    }

    public void putValue(double d) {
        ensureWritable();
        back.putLong( offset(), (long)d );
    }
    
    public void putValue(int i0, double d) {
        ensureWritable();
        back.putLong( offset(i0), (long)d );
    }

    public void putValue(int i0, int i1, double d) {
        ensureWritable();
        back.putLong( offset(i0, i1), (long)d );
    }

    public void putValue(int i0, int i1, int i2, double d) {
        ensureWritable();
        back.putLong( offset(i0, i1, i2), (long)d );
    }

    public void putValue(int i0, int i1, int i2, int i3, double d) {
        ensureWritable();
        back.putLong( offset(i0, i1, i2, i3), (long)d );
    }
}
