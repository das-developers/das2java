/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qstream;

import java.nio.ByteBuffer;
import java.util.Locale;

/**
 * optimized for sending integers
 * @author jbf
 */
public class AsciiIntegerTransferType extends AsciiTransferType {

    String format;
    
    public AsciiIntegerTransferType( int sizeBytes ) {
        super(sizeBytes,false);
        format= "%"+(sizeBytes-1)+"d";
    }
    
    public void write(double d, ByteBuffer buffer) {
        String s= String.format( Locale.US, format, (int)d );
        buffer.put( s.getBytes() );
        buffer.put( (byte)' ' );
    }

}
