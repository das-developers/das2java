/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.qstream;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.das2.util.FixedWidthFormatter;
import org.das2.util.NumberFormatUtil;

/**
 *
 * @author jbf
 */
public class AsciiTransferType extends TransferType {

    final int sizeBytes;
    private DecimalFormat formatter;
    
    AsciiTransferType( int sizeBytes, boolean scientificNotation ) {
        this.sizeBytes = sizeBytes;
        formatter = NumberFormatUtil.getDecimalFormat( getFormat(sizeBytes-1,scientificNotation) );
    }

    private static String getFormat( int length, boolean sci ) {
        if (length < 9 || sci==false ) {
            return "0.#";
        } else {
            StringBuffer buffer = new StringBuffer(length);
            buffer.append("+0.");
            for (int i = 0; i < length - 7; i++) {
                buffer.append('0');
            }
            buffer.append("E00;-#");
            return buffer.toString();
        }
    }
    
    public void write( double d, ByteBuffer buffer) {
        String s = formatter.format(d);
        if ( s.length() < sizeBytes ) s+=" ";
        if (s.length() < sizeBytes ) {
            s = FixedWidthFormatter.format(s, sizeBytes ) ;
        }
        byte[] bytes=null;
        try {
            bytes = s.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(AsciiTransferType.class.getName()).log(Level.SEVERE, null, ex);
        }
        if ( bytes.length!=sizeBytes ) {
            throw new IllegalAccessError();
        }
        buffer.put(bytes);
        
    }

    public double read(ByteBuffer buffer) {
        byte[] bytes = new byte[sizeBytes];
        buffer.get(bytes);
        String str;
        try {
            str = new String(bytes, "US-ASCII").trim();
            return Double.parseDouble(str);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(AsciiTransferType.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }

    public int sizeBytes() {
        return sizeBytes;
    }

    public boolean isAscii() {
        return true;
    }

    public String name() {
        return "ascii"+sizeBytes;
    }

    public static TransferType getByName( String name, Map<String, Object> properties) {
        Pattern p= Pattern.compile("ascii(\\d+)");
        Matcher m= p.matcher(name);
        if ( m.matches() ) {
            return new AsciiTransferType( Integer.parseInt(m.group(1)), true );
        } else {
            return null;
        }
    }
    
}
