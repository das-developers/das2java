/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.qstream;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.das2.util.FixedWidthFormatter;

/**
 * Transfer type that represents data as formatted ASCII strings.  See
 * also AsciiTimeTransferType, which represents UT times.
 * @author jbf
 */
public class AsciiTransferType extends TransferType {

    final int sizeBytes;
    private String formatterStr;
    boolean useFormatterStr;
    
    private static String STARS= "**************************************************";
    
    /**
     * create a new transfer type with the given number of spaces, which
     * will format numbers using optionally scientific notation.
     * @param sizeBytes the number of bytes alloted to the transfer type, at least 6 and no more than 50.
     * @param scientificNotation if true, use scientific notation (e.g. +3.14e00)
     */
    public AsciiTransferType( int sizeBytes, boolean scientificNotation ) {
        if ( sizeBytes<6 ) {
            throw new IllegalArgumentException("sizeBytes cannot be less than 6");
        } else if ( sizeBytes>50 ) {
            throw new IllegalArgumentException("sizeBytes cannot be greater than 18");
        }
        this.sizeBytes = sizeBytes;
        if ( scientificNotation ) {
            int decimals= sizeBytes - 6;
            if ( decimals>0 ) decimals--; // in case there is room for whitespace, use it.
            if ( decimals<0 ) {
                throw new IllegalArgumentException("sizeBytes cannot be less than 6.");
            }
            if ( scientificNotation ) {
                formatterStr= String.format( "%%%d.%de", sizeBytes, Math.min( decimals, 18 ) );
            } else {
                formatterStr= String.format( "%%%d.%df", sizeBytes, Math.min( decimals, 18 ) );
            }
            this.useFormatterStr= true;
        } else {
            this.formatterStr= String.format( "%%%df", sizeBytes-1 ); // -1 is for whitespace.
            this.useFormatterStr= false;
        }
        //formatter = NumberFormatUtil.getDecimalFormat( formatStr );   
    }

    //TODO: see Chris' qstream where ascii5 didn't work...  

    /**
     * return a transfer type with the given number of decimal places.
     * @param sizeBytes the number of bytes alloted to the transfer type, at least 6 and no more than 18.
     * @param scientificNotation if true, use scientific notation (e.g. +3.14e00)
     * @param decimals 0 for integers, number of decimal places otherwise.
     */
    AsciiTransferType( int sizeBytes, boolean scientificNotation, int decimals ) {
        this.sizeBytes = sizeBytes;
        if ( sizeBytes<6 ) {
            throw new IllegalArgumentException("sizeBytes cannot be less than 6");
        } else if ( sizeBytes>50 ) {
            throw new IllegalArgumentException("sizeBytes cannot be greater than 50");
        }
        if ( decimals<0 ) {
            throw new IllegalArgumentException("decimals cannot be negative");
        }
        if ( scientificNotation ) {
            formatterStr= String.format( "%%%d.%de", sizeBytes, Math.min( decimals, 18 ) );
        } else {
            formatterStr= String.format( "%%%d.%df", sizeBytes, Math.min( decimals, 18 ) );
        }
        this.useFormatterStr= true;
    }

    @Override
    public void write( double d, ByteBuffer buffer) {
        String s;
        if ( useFormatterStr ) {
            s= String.format( formatterStr, d );
        } else {
            s= String.valueOf(d);
            if ( s.length()>sizeBytes ) {
                s= String.format( formatterStr, d );
            }
        }
        
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
            if ( bytes.length!=sizeBytes ) {
                bytes = STARS.substring(0,sizeBytes).getBytes("US-ASCII");
            }
        } catch (UnsupportedEncodingException ex) {
            logger.log(Level.SEVERE, "write", ex);
        }
        
        buffer.put(bytes);
        
    }

    @Override
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
        } catch ( NumberFormatException ex ) {
            return Double.NaN;
        }
    }

    @Override
    public int sizeBytes() {
        return sizeBytes;
    }

    @Override
    public boolean isAscii() {
        return true;
    }

    @Override
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
