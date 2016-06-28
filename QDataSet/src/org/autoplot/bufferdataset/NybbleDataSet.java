/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.autoplot.bufferdataset;

import java.nio.ByteBuffer;
import static org.autoplot.bufferdataset.BufferDataSet.BYTE;
import static org.autoplot.bufferdataset.BufferDataSet.byteCount;
import org.virbo.dataset.QDataSet;

/**
 * Introduced to support looking at data from LWA, which are 12 bit ints.
 * @author jbf
 */
public class NybbleDataSet extends BufferDataSet {

    public NybbleDataSet(int rank, int reclenbits, int recoffsbits, int len0, int len1, int len2, int len3, ByteBuffer back ) {
        super( rank, reclenbits, recoffsbits, BufferDataSet.BITS, len0, len1, len2, len3, BufferDataSet.NYBBLE, back );        
    }

    protected int offset() {
        return recoffset;
    }
    
    protected int offset( int i0 ) {
        return recoffset + i0/2;
    }

    protected int offset( int i0, int i1 ) {
        return recoffset + reclen * i0 + i1 / 2;
    }

    public double value() {
        return back.get(offset()) & 0x0F;
    }

    
    public double value(int i0) {
        if ( i0 % 2 == 0 ) {
            return back.get(offset(i0)) & 0x0F;
        } else {
            return back.get(offset(i0)) >> 4;
        }
    }

    public double value(int i0, int i1) {
        if ( i0 % 2 == 0 ) {
            return back.get(offset(i0,i1)) & 0x0F;
        } else {
            return back.get(offset(i0,i1)) >> 4;
        }
    }

    public double value(int i0, int i1, int i2) {
        throw new IllegalArgumentException("not supported");
    }

    public double value(int i0, int i1, int i2, int i3) {
        throw new IllegalArgumentException("not supported");
    }

    @Override
    public int rank() {
        return super.rank();
    }

    @Override
    public int length(int i) {
        return super.length(i); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int length() {
        return super.length(); //To change body of generated methods, choose Tools | Templates.
    }

    
    @Override
    public void putValue(double d) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void putValue(int i0, double d) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void putValue(int i0, int i1, double d) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void putValue(int i0, int i1, int i2, double d) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void putValue(int i0, int i1, int i2, int i3, double d) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    

}
