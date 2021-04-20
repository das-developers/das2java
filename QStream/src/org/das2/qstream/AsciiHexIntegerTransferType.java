
package org.das2.qstream;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * format as hex, useful for RGB colors, where the color is represented as
 * 0xRRGGBB.
 * @author jbf
 */
public class AsciiHexIntegerTransferType extends AsciiIntegerTransferType {
    
    public AsciiHexIntegerTransferType(int sizeBytes) {
        super(sizeBytes);
        format= "0x%0"+(sizeBytes-3)+"x";
    }

    @Override
    public double read(ByteBuffer buffer) {
        try {
            byte[] bytes = new byte[sizeBytes];
            buffer.get(bytes);
            String str;
            str = new String(bytes, "US-ASCII");
            if ( str.startsWith("0x") ) {
                str= str.substring(2);
            } 
            return Integer.valueOf( str.trim(),16 );

        } catch (UnsupportedEncodingException ex) {
            return Double.NaN;
        }
    }

    @Override
    public void write(double d, ByteBuffer buffer) {
        super.write(d, buffer); //To change body of generated methods, choose Tools | Templates.
    }
    
    

    public static TransferType getByName( String ttype, Map<String,Object> properties ) {
        if ( ttype.startsWith("hex") ) {
            return new AsciiHexIntegerTransferType(Integer.parseInt(ttype.substring(3)));
        } else {
            return null;
        }
    }

    @Override
    public String name() {
        return "hex"+sizeBytes;
    }
    
    
    
}
