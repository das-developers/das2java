/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qds.buffer;

import java.nio.ByteBuffer;
import org.das2.qds.WritableDataSet;

/**
 * Truncated floats are two-byte floats with low precision used by the plasma wave group.  They
 * have an exponent the same size as a 4-byte float, but the mantissa has just 7(?) bits.
 * @author jbf
 */
public class TruncatedFloatDataSet extends BufferDataSet implements WritableDataSet {
    public TruncatedFloatDataSet( int rank, int reclen, int recoffs, int len0, int len1, int len2, int len3, ByteBuffer back ) {
        super( rank, reclen, recoffs, len0, len1, len2, len3, TRUNCATEDFLOAT, back );
    }

    public double value() {
        return java.lang.Float.intBitsToFloat( back.getShort(offset()) << 16 );
    }

    public double value(int i0) {
        return java.lang.Float.intBitsToFloat( back.getShort(offset(i0)) << 16 );
    }

    public double value(int i0, int i1) {
        return java.lang.Float.intBitsToFloat( back.getShort(offset(i0, i1)) << 16 );
    }

    public double value(int i0, int i1, int i2) {
        return java.lang.Float.intBitsToFloat( back.getShort(offset(i0, i1, i2)) << 16 );
    }

    public double value(int i0, int i1, int i2, int i3) {
        return java.lang.Float.intBitsToFloat( back.getShort(offset(i0, i1, i2, i3 )) << 16 );
    }

    private final short truncate( double d ) {
        return (short)((java.lang.Float.floatToIntBits((float)d)>>16) & 0xffff );
    }

    public void putValue(double d) {
        ensureWritable();
        back.putShort( offset(), truncate(d) );
    }

    public void putValue(int i0, double d) {
        ensureWritable();
        back.putShort( offset(i0), truncate(d) );
    }

    public void putValue(int i0, int i1, double d) {
        ensureWritable();
        back.putShort( offset(i0, i1), truncate(d) );
    }

    public void putValue(int i0, int i1, int i2, double d) {
        ensureWritable();
        back.putShort( offset(i0, i1, i2), truncate(d) );
    }

    public void putValue(int i0, int i1, int i2, int i3, double d) {
        ensureWritable();
        back.putShort( offset(i0, i1, i2, i3), truncate(d) );
    }
}
