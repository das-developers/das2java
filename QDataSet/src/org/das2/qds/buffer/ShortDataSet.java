package org.das2.qds.buffer;

import java.nio.ByteBuffer;

public class ShortDataSet extends BufferDataSet {

    public ShortDataSet(int rank, int reclen, int recoffs, int len0, int len1, int len2, int len3, ByteBuffer back ) {
        super(rank, reclen, recoffs, len0, len1, len2, len3, SHORT, back );
    }

    public double value() {
        return back.getShort(offset());
    }

    public double value(int i0) {
        return back.getShort(offset(i0));
    }

    public double value(int i0, int i1) {
        return back.getShort(offset(i0, i1));
    }

    public double value(int i0, int i1, int i2) {
        return back.getShort(offset(i0, i1, i2));
    }

    public double value(int i0, int i1, int i2, int i3) {
        return back.getShort(offset(i0, i1, i2, i3) );
    }
    
    public void putValue(double d) {
        ensureWritable();
        back.putShort( offset(), (short)d );
    }
    
    public void putValue(int i0, double d) {
        ensureWritable();
        back.putShort( offset(i0), (short)d );
    }

    public void putValue(int i0, int i1, double d) {
        ensureWritable();
        back.putShort( offset(i0, i1), (short)d );
    }

    public void putValue(int i0, int i1, int i2, double d) {
        ensureWritable();
        back.putShort( offset(i0, i1, i2), (short)d );
    }

    public void putValue(int i0, int i1, int i2, int i3, double d) {
        ensureWritable();
        back.putShort( offset(i0, i1, i2, i3), (short)d );
    }
}
