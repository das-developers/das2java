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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.das2.util.FixedWidthFormatter;
import org.das2.util.NumberFormatUtil;

/**
 * Transfer type that represents data as formatted ASCII strings.  See
 * also AsciiTimeTransferType, which represents UT times.
 * @author jbf
 */
public class AsciiTransferType extends TransferType {

    final int sizeBytes;
    private DecimalFormat formatter;
    private String formatStr; // for debugging
    
    public AsciiTransferType( int sizeBytes, boolean scientificNotation ) {
        this.sizeBytes = sizeBytes;
        this.formatStr= getFormat(sizeBytes-1,scientificNotation);
        formatter = NumberFormatUtil.getDecimalFormat( formatStr );
    }

    //TODO: see Chris' qstream where ascii5 didn't work...  

    /**
     * return a transfer type with the given number of decimal places.
     * TODO: this is not in use and has little testing.
     * @param sizeBytes
     * @param scientificNotation
     * @param decimals
     */
    AsciiTransferType( int sizeBytes, boolean scientificNotation, int decimals ) {
        this.sizeBytes = sizeBytes;
        if ( decimals==0 ) {
            this.formatStr= "0";
        } else {
            String pounds="################";
            this.formatStr= "0."+pounds.substring(0,decimals);
        }
        formatter = NumberFormatUtil.getDecimalFormat( formatStr );
    }

    private static String getFormat( int length, boolean sci ) {
        if ( length<9 || sci==false ) {
            if (length==9  ) {
                return "0.####";
            } else if ( length==8 ) {
                return "0.###";
            } else if ( length==7 ) {
                return "0.##";
            } else if ( length==6 ) {
                return "0.#";
            } else if ( length<6 ) {
                throw new IllegalArgumentException("length is too short: "+length);
            } else {
                return "0.####";
            }
        } else {
            StringBuilder buffer = new StringBuilder(length);
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
        if ( s.charAt(sizeBytes-1)!=' ' && s.charAt(0)=='+' ) {
            s= s.substring(1)+' ';
        }
        byte[] bytes=null;
        try {
            bytes = s.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            logger.log(Level.SEVERE, "write", ex);
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
            logger.log(Level.SEVERE, "read", ex);
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
