
package org.das2.qds.buffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 3-byte unsigned integers, introduced for an application where we needed
 * to get at 3-nybble numbers, but Int24 was confusing because of the sign
 * bit.
 * @author jbf
 */
public class UInt24DataSet extends BufferDataSet {

    public UInt24DataSet(int rank, int reclen, int recoffs, int len0, int len1, int len2, int len3, ByteBuffer back) {
        super(rank, reclen, recoffs, len0, len1, len2, len3, UINT24, back);
    }
    
    private int intValue( ByteBuffer buf, int offset ) {
        ByteBuffer int4= ByteBuffer.allocate(4);
        int4.order(ByteOrder.BIG_ENDIAN);
        int4.put((byte)0);
        if ( back.order()==ByteOrder.LITTLE_ENDIAN ) { // TODO: this seems backwards, but this works.  Why?
            int4.put(buf.get(offset));
            int4.put(buf.get(offset+1));
            int4.put(buf.get(offset+2));
        } else {
            int4.put(buf.get(offset+2));
            int4.put(buf.get(offset+1));
            int4.put(buf.get(offset));
        }
        int4.flip();
        return int4.getInt(0);
    }
    
    @Override
    public double value() {
        return intValue(back, offset());
    }

    @Override
    public double value(int i0) {
        return intValue(back, offset(i0));
    }

    @Override
    public double value(int i0, int i1) {
        return intValue(back, offset(i0,i1));
    }

    @Override
    public double value(int i0, int i1, int i2) {
        return intValue(back, offset(i0,i1,i2));
    }

    @Override
    public double value(int i0, int i1, int i2, int i3) {
        return intValue(back, offset(i0,i1,i2,i3));
    }

    private byte[] bytes3( int d ) {
        ByteBuffer buf= ByteBuffer.allocate(4);
        buf.order(ByteOrder.BIG_ENDIAN);
        byte[] b= buf.putInt(d).array();
        if ( back.order()==ByteOrder.BIG_ENDIAN ) {
            return new byte[] { b[1], b[2], b[3] };            
        } else {
           return new byte[] { b[3], b[2], b[1] };             
        }
        
    }
    
    @Override
    public void putValue(double d) {
        byte[] b3= bytes3((int)d);
        int offset= offset();
        back.put(offset,b3[0]);
        back.put(offset+1,b3[1]);
        back.put(offset+2,b3[2]);
    }

    @Override
    public void putValue(int i0, double d) {
        byte[] b3= bytes3((int)d);
        int offset= offset(i0);
        back.put(offset,b3[0]);
        back.put(offset+1,b3[1]);
        back.put(offset+2,b3[2]);
    }

    @Override
    public void putValue(int i0, int i1, double d) {
        byte[] b3= bytes3((int)d);
        int offset= offset(i0,i1);
        back.put(offset,b3[0]);
        back.put(offset+1,b3[1]);
        back.put(offset+2,b3[2]);
    }

    @Override
    public void putValue(int i0, int i1, int i2, double d) {
        byte[] b3= bytes3((int)d);
        int offset= offset(i0,i1,i2);
        back.put(offset,b3[0]);
        back.put(offset+1,b3[1]);
        back.put(offset+2,b3[2]);
    }

    @Override
    public void putValue(int i0, int i1, int i2, int i3, double d) {
        byte[] b3= bytes3((int)d);
        int offset= offset(i0,i1,i2,i3);
        back.put(offset,b3[0]);
        back.put(offset+1,b3[1]);
        back.put(offset+2,b3[2]);
    }
    
}
