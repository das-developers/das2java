/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.autoplot.bufferdataset;

import java.nio.ByteBuffer;
import java.text.ParseException;
import static org.autoplot.bufferdataset.BufferDataSet.BYTE;
import org.das2.datum.TimeUtil;
import org.das2.datum.Units;
import org.virbo.dataset.QDataSet;

/**
 *
 * @author jbf
 */
public class TimeDataSet extends BufferDataSet {

    int lenBytes;
    double fill= -1e38;
    
    public TimeDataSet(int rank, int reclen, int recoffs, int len0, int len1, int len2, int len3, ByteBuffer back ) {
        super(rank, reclen, recoffs, len0, len1, len2, len3, BYTE, back );
        this.lenBytes= 24;
        putProperty( QDataSet.UNITS, Units.us2000 );
        putProperty( QDataSet.FILL_VALUE, fill );
    }
    
    public void setLengthBytes( int length ) {
        this.lenBytes= length;
    }
    
    private double parseTime( ByteBuffer back, int offset ) {
        byte[] buff= new byte[lenBytes];
        for ( int i=0; i<lenBytes; i++ ) buff[i]= back.get(i+offset);
        try {
            return TimeUtil.create( new String(buff) ).doubleValue( Units.us2000 );
        } catch (ParseException ex) {
            return fill;
        }
    }
    
    @Override
    public double value() {
        return parseTime(back, offset());
    }

    @Override
    public double value(int i0) {
        return parseTime(back, offset(i0));
    }

    @Override
    public double value(int i0, int i1) {
        return parseTime(back, offset(i0, i1));
    }

    @Override
    public double value(int i0, int i1, int i2) {
        return parseTime(back, offset(i0, i1, i2));
    }

    @Override
    public double value(int i0, int i1, int i2, int i3) {
        return parseTime(back, offset(i0, i1, i2, i3));
    }

    @Override
    public void putValue(double d) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void putValue(int i0, double d) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void putValue(int i0, int i1, double d) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void putValue(int i0, int i1, int i2, double d) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void putValue(int i0, int i1, int i2, int i3, double d) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
