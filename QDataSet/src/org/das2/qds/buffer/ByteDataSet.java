package org.das2.qds.buffer;

import java.nio.ByteBuffer;

public class ByteDataSet extends BufferDataSet {

    public ByteDataSet(int rank, int reclen, int recoffs, int len0, int len1, int len2, int len3, ByteBuffer back ) {
        super(rank, reclen, recoffs, len0, len1, len2, len3, BYTE, back );
    }

    public double value() {
        return back.get(offset());
    }

    public double value(int i0) {
        return back.get(offset(i0));
    }

    public double value(int i0, int i1) {
        return back.get(offset(i0, i1));
    }

    public double value(int i0, int i1, int i2) {
        return back.get(offset(i0, i1, i2));
    }

    public double value(int i0, int i1, int i2, int i3) {
        return back.get(offset(i0, i1, i2,i3));
    }

    public void putValue(double d) {
        ensureWritable();
        back.put( offset(), (byte)d );
    }

    public void putValue(int i0, double d) {
        ensureWritable();
        back.put( offset(i0), (byte)d );
    }

    public void putValue(int i0, int i1, double d) {
        ensureWritable();
        back.put( offset(i0, i1), (byte)d );
    }

    public void putValue(int i0, int i1, int i2, double d) {
        ensureWritable();
        back.put( offset(i0, i1, i2), (byte)d );
    }

    public void putValue(int i0, int i1, int i2, int i3, double d) {
        ensureWritable();
        back.put( offset(i0, i1, i2, i3), (byte)d );
    }
}
