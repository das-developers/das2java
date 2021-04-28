
package org.das2.qds.buffer;

import java.nio.ByteBuffer;
import org.das2.qds.DataSetOps;
import org.das2.qds.QDataSet;

/**
 * Introduced to support looking at data from LWA, which are 12 bit ints.
 * @author jbf
 */
public final class NybbleDataSet extends BufferDataSet {

    public NybbleDataSet(int rank, int reclenbits, int recoffsbits, int len0, int len1, int len2, int len3, ByteBuffer back ) {
        super( rank, reclenbits, recoffsbits, BufferDataSet.BITS, len0, len1, len2, len3, BufferDataSet.NYBBLE, back );
        int recLenNeededBits= (int)Math.ceil( 4 * ( len1 * len2 * len3 ) );
        if ( reclenbits < recLenNeededBits ) {
            throw new IllegalArgumentException("reclenbits too small to hold each record");
        }
        reclen= reclenbits/8;
        this.makeImmutable();
    }

    @Override
    protected int offset() {
        return recoffset;
    }
    
    @Override
    protected int offset( int i0 ) {
        return recoffset + i0/2;
    }

    @Override
    protected int offset( int i0, int i1 ) {
        return recoffset + reclen * i0 + i1 / 2;
    }

    @Override
    public double value() {
        return back.get(offset()) & 0x0F;
    }

    @Override
    public double value(int i0) {
        if ( i0 % 2 == 0 ) {
            return back.get(offset(i0)) & 0x0F;
        } else {
            return back.get(offset(i0)) >> 4;
        }
    }

    @Override
    public double value(int i0, int i1) {
        byte vv= back.get(offset(i0,i1));
        try {
            if ( i1 % 2 == 0 ) {
                return vv & 0x0F;
            } else {
                return ( vv & 0xF0 ) >> 4;
            }
        } catch ( IndexOutOfBoundsException ex ) {
            throw ex;
        }
    }

    @Override
    public double value(int i0, int i1, int i2) {
        throw new IllegalArgumentException("not supported");
    }

    @Override
    public double value(int i0, int i1, int i2, int i3) {
        throw new IllegalArgumentException("not supported");
    }

    @Override
    public int rank() {
        return super.rank();
    }

    @Override
    public int length(int i) {
        return super.length(i); 
    }

    @Override
    public int length() {
        return super.length(); 
    }

    
    @Override
    public void putValue(double d) {
        int vv= back.get( offset() ) & 0x0F;
        vv= vv & ( ((int)d) & 0xF );
        back.put( offset(), (byte)vv );
    }

    @Override
    public void putValue(int i0, double d) {
        int off= offset(i0);
        if ( i0 % 2 == 0 ) {
            int vv= back.get( off );
            vv= ( vv & 0xF0 ) | ( ((int)d) & 0xF );
            back.put( off, (byte)vv );
        } else {
            int vv= back.get( off );
            vv= ( vv & 0x0F ) | ( ((int)d)<<4 & 0xF0 );
            back.put( off, (byte)vv );
        }
    }

    @Override
    public void putValue(int i0, int i1, double d) {
        int off= offset(i0,i1);
        if ( i1 % 2 == 0 ) {
            int vv= back.get( off );
            vv= ( vv & 0xF0 ) | ( ((int)d) & 0xF );
            back.put( off, (byte)vv );
        } else {
            int vv= back.get( off );
            vv= ( vv & 0x0F ) | ( ((int)d)<<4 & 0xF0 );
            back.put( off, (byte)vv );
        }
    }

    @Override
    public void putValue(int i0, int i1, int i2, double d) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void putValue(int i0, int i1, int i2, int i3, double d) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }
    
    @Override
    public QDataSet slice(int i) {
        return DataSetOps.slice0( this, i );
    }

    @Override
    public QDataSet trim(int ist, int ien) {
        return DataSetOps.trim( this, ist, ien );
    }

}
