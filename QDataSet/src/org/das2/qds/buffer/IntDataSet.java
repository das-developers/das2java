package org.das2.qds.buffer;

import java.nio.ByteBuffer;
import org.das2.qds.WritableDataSet;

public class IntDataSet extends BufferDataSet implements WritableDataSet {

    public IntDataSet(int rank, int reclen, int recoffs, int len0, int len1, int len2, int len3, ByteBuffer back) {
        super(rank, reclen, recoffs, len0, len1, len2, len3, INT, back);
    }

    public double value() {
        return back.getInt(offset());
    }

    public double value(int i0) {
        return back.getInt(offset(i0));
    }

    public double value(int i0, int i1) {
        return back.getInt(offset(i0, i1));
    }

    public double value(int i0, int i1, int i2) {
        return back.getInt(offset(i0, i1, i2));
    }

    public double value(int i0, int i1, int i2, int i3) {
        return back.getInt(offset(i0, i1, i2, i3));
    }
    
    public void putValue(double d) {
        ensureWritable();
        back.putInt( offset(), (int)d );
    }
    
    public void putValue(int i0, double d) {
        ensureWritable();
        back.putInt( offset(i0), (int)d );
    }

    public void putValue(int i0, int i1, double d) {
        ensureWritable();
        back.putInt( offset(i0, i1), (int)d );
    }

    public void putValue(int i0, int i1, int i2, double d) {
        ensureWritable();
        back.putInt( offset(i0, i1, i2), (int)d );
    }

    public void putValue(int i0, int i1, int i2, int i3, double d) {
        ensureWritable();
        back.putInt( offset(i0, i1, i2, i3), (int)d );
    }
}
