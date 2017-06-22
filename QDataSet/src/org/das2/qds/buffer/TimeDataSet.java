
package org.das2.qds.buffer;

import java.nio.ByteBuffer;
import java.text.ParseException;
import org.das2.datum.TimeUtil;
import org.das2.datum.Units;
import org.das2.qds.QDataSet;

/**
 * Often we see where time is left decoded in binary streams, occupying ~21 
 * bytes (ASCII characters) instead of 8 bytes to represent them as a double.  This extension
 * allows this sort of data to be read in as well, making the data available 
 * as Units.us2000.
 * 
 * @author jbf
 */
public class TimeDataSet extends BufferDataSet {

    int lenBytes;
    double fill= -1e38;
    private final Units UNITS= Units.us2000;

    /**
     * Like the other constructors, but the type is needed as well to get the 
     * number of bytes.  
     * @param rank
     * @param reclen
     * @param recoffs
     * @param len0
     * @param len1
     * @param len2
     * @param len3
     * @param back
     * @param type string "timeXX" where XX is number of bytes, between 20 and 32.
     */
    public TimeDataSet(int rank, int reclen, int recoffs, int len0, int len1, int len2, int len3, ByteBuffer back, Object type ) {
        super(rank, reclen, recoffs, len0, len1, len2, len3, type, back );
        this.lenBytes= Integer.parseInt( type.toString().substring(4) );
        if ( this.lenBytes<16 ) throw new IllegalArgumentException("time16 is the shortest time supported.");
        if ( this.lenBytes>24 ) throw new IllegalArgumentException("time24 is the shortest time supported.");
        putProperty( QDataSet.UNITS, UNITS );
        putProperty( QDataSet.FILL_VALUE, fill );
    }
    
    public void setLengthBytes( int length ) {
        this.lenBytes= length;
    }
    
    private double parseTime( ByteBuffer back, int offset ) {
        byte[] buff= new byte[lenBytes];
        for ( int i=0; i<lenBytes; i++ ) buff[i]= back.get(i+offset);
        try {
            return TimeUtil.create( new String(buff) ).doubleValue( UNITS );
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

    private byte[] getBytes( double d ) {
        String s= UNITS.createDatum(d).toString(); //TODO: 24 byte limit is here.
        return s.substring(0,lenBytes).getBytes();
    }
    
    @Override
    public void putValue(double d) {
        ensureWritable();
        int offs= offset();
        byte[] bb= getBytes( d );
        for ( int i=0; i<lenBytes; i++ ) back.put( offs+i, bb[i] );
    }

    @Override
    public void putValue(int i0, double d) {
        ensureWritable();
        int offs= offset(i0);
        byte[] bb= getBytes( d );
        for ( int i=0; i<lenBytes; i++ ) back.put( offs+i, bb[i] );
    }

    @Override
    public void putValue(int i0, int i1, double d) {
        ensureWritable();
        int offs= offset(i0,i1);
        byte[] bb= getBytes( d );
        for ( int i=0; i<lenBytes; i++ ) back.put( offs+i, bb[i] );
    }

    @Override
    public void putValue(int i0, int i1, int i2, double d) {
        ensureWritable();
        int offs= offset(i0,i1,i2);
        byte[] bb= getBytes( d );
        for ( int i=0; i<lenBytes; i++ ) back.put( offs+i, bb[i] );
    }

    @Override
    public void putValue(int i0, int i1, int i2, int i3, double d) {
        ensureWritable();
        int offs= offset(i0,i1,i2,i3);
        byte[] bb= getBytes( d );
        for ( int i=0; i<lenBytes; i++ ) back.put( offs+i, bb[i] );
    }
    
}
