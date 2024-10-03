
package org.das2.qds.buffer;

import java.nio.ByteBuffer;
import java.text.ParseException;
import org.das2.datum.EnumerationUnits;
import org.das2.datum.Units;
import org.das2.qds.QDataSet;

/**
 * Sometimes you might just have numbers encoded in ASCII within a binary stream, 
 * or fixed-length fields allow such parsing.  If the unit has been set to an enumeration (or nominal)
 * unit, then strings can be extracted as well, but this must be done using the setUnits
 * method.
 * @author jbf
 */
public class AsciiDataSet extends BufferDataSet {
    
    int lenBytes;
    double fill= -1e38;
    
    private Units units= Units.dimensionless;
    
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
     * @param type string "asciiXX" where XX is number of bytes, between 1 and 24.
     */
    public AsciiDataSet(int rank, int reclen, int recoffs, int len0, int len1, int len2, int len3, ByteBuffer back, Object type ) {
        super(rank, reclen, recoffs, len0, len1, len2, len3, type, back );
        this.lenBytes= Integer.parseInt( type.toString().substring(5) );
        if ( this.lenBytes<1 ) throw new IllegalArgumentException("ascii1 is the shortest field supported.");
        if ( this.lenBytes>32 ) throw new IllegalArgumentException("ascii24 is the longest field supported.");
        putProperty( QDataSet.FILL_VALUE, fill );
    }
    
    public void setLengthBytes( int length ) {
        this.lenBytes= length;
    }
    
    public void setUnits( Units units ) {
        this.units= units;
    }
    
    private double parseDouble( ByteBuffer back, int offset ) {
        byte[] buff= new byte[lenBytes];
        for ( int i=0; i<lenBytes; i++ ) buff[i]= back.get(i+offset);
        if ( buff[lenBytes-1]==',' ) buff[lenBytes-1]=' '; // allow the field to contain delimiter.
        String s= new String(buff);
        if ( units instanceof EnumerationUnits ) {
            return ((EnumerationUnits)units).createDatum(s.trim()).doubleValue(units);
        } else {
            try {
                return units.parse(s).doubleValue( units );
            } catch (ParseException ex) {
                return fill;
            }
        }
    }
    
    @Override
    public double value() {
        return parseDouble(back, offset());
    }

    @Override
    public double value(int i0) {
        return parseDouble(back, offset(i0));
    }

    @Override
    public double value(int i0, int i1) {
        return parseDouble(back, offset(i0, i1));
    }

    @Override
    public double value(int i0, int i1, int i2) {
        return parseDouble(back, offset(i0, i1, i2));
    }

    @Override
    public double value(int i0, int i1, int i2, int i3) {
        return parseDouble(back, offset(i0, i1, i2, i3));
    }

    private byte[] getBytes( double d ) {
        String s= units.createDatum(d).toString(); //TODO: 24 byte limit is here.
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
