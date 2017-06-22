/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.qstream;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.das2.datum.TimeUtil;
import org.das2.datum.Units;
import org.das2.datum.format.DatumFormatter;
import org.das2.datum.format.TimeDatumFormatter;
import org.das2.qds.QDataSet;

/**
 * ISO-8601 encoded times.
 * @author jbf
 */
public class AsciiTimeTransferType extends AsciiTransferType {

    Units units;
    DatumFormatter formatter;

    /**
     * create the ISO8601 transfer type.
     * @param sizeBytes
     * @param units 
     */
    public AsciiTimeTransferType(int sizeBytes, Units units) {
        super(sizeBytes,false);
        this.units = units;
        try {
            if ( sizeBytes<=24 ) {
                this.formatter = TimeDatumFormatter.DEFAULT;
            } else if ( sizeBytes<=27 ) {
                this.formatter = new TimeDatumFormatter("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z')");
            } else if ( sizeBytes<=30 ) {
                this.formatter = new TimeDatumFormatter("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z')");
            } else if ( sizeBytes<50 ) {
                this.formatter = new TimeDatumFormatter("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSSSS'Z')");
            } else {
                throw new IllegalArgumentException("size of AsciiTimeTransferType must be less than 50");
            }
        } catch ( ParseException ex ) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void write(double d, ByteBuffer buffer) {
        try {
            String s;
            if ( units.isFill(d) ) {
                s= "**************************************************".substring(0,sizeBytes);
            } else {
                s= formatter.format(units.createDatum(d));
            }
            byte[] bak = s.getBytes("US-ASCII");
            int c = Math.min(s.length(), sizeBytes-1 );
            buffer.put(bak, 0, c);
            while ( c < sizeBytes ) {
                c++;
                buffer.put((byte) 32);
            }
        } catch (UnsupportedEncodingException ex) {
            logger.log(Level.SEVERE, "write", ex);
        }
    }

    @Override
    public double read(ByteBuffer buffer) {
        try {
            byte[] bytes = new byte[sizeBytes];
            buffer.get(bytes);
            String str = new String(bytes, "US-ASCII").trim();
            double result = TimeUtil.create(str).doubleValue(units);
            return result;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

    }

    public static TransferType getByName( String name, Map<String, Object> properties) {
        Pattern p= Pattern.compile("time(\\d+)");
        Matcher m= p.matcher(name);
        if ( m.matches() ) {
            Units units= (Units) properties.get( QDataSet.UNITS );
            if ( units==null ) throw new IllegalArgumentException("Units need to be in properties");
            return new AsciiTimeTransferType( Integer.parseInt(m.group(1)),units );
        } else {
            return null;
        }
    }

    @Override
    public String name() {
        return "time"+sizeBytes;
    }
    
    
}
