/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.qstream;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * A transfer type is an encoding of a double on to the stream.  It must have a fixed length in bytes.
 * Some are pure ascii, and isAscii can be used to check this.
 * @author jbf
 */
public abstract class TransferType {

    /**
     * returns a TranferType for the given name, or null if none is found.
     * @param ttype
     * @param properties map of dataset properties.  Introduced to pass in time Unit to AsciiTimeTransferType without unduly coupling codes.
     * @return returns a TranferType for the given name, or null if none is found.
     */
    public static TransferType getForName(String ttype, Map<String,Object> properties ) {
        TransferType tt;
        
        tt= DoubleTransferType.getByName(ttype, properties);
        if ( tt!=null ) return tt;
        
        tt= FloatTransferType.getByName(ttype, properties);
        if ( tt!=null ) return tt;
        
        tt= IntegerTransferType.getByName(ttype, properties);
        if ( tt!=null ) return tt;
        
        tt= AsciiTransferType.getByName(ttype, properties);
        if ( tt!=null ) return tt;
            
        tt= AsciiIntegerTransferType.getByName(ttype, properties);
        if ( tt!=null ) return tt;
        
        tt= AsciiTimeTransferType.getByName(ttype, properties);
        if ( tt!=null ) return tt;
        
        return null;
    }
    
    /**
     * write the data to the buffer.  The buffer's position should be incremented by sizeBytes.
     * @param d
     * @param buffer
     * @param endOfPacket.  hint to AsciiType that this might be a good place for a new line.
     */
    public abstract void write( double d, ByteBuffer buffer  );
    
    /**
     * read the data from the buffer.  The buffer's position should be incremented by sizeBytes.
     * @param d
     * @param buffer
     */
    public abstract double read( ByteBuffer buffer );
    
    /**
     * return the number of bytes used by the transfer type.
     * @return
     */
    public abstract int sizeBytes();
    
    /**
     * return true if the transfer type uses Ascii-encodings to respresent data.
     * It's assumed in this case that trailing whitespace can be modified for
     * readability.
     * @return true if the type is ascii-based.
     */
    public abstract boolean isAscii( );
    
    /**
     * return a string identifying the TransferType.
     * @return
     */
    abstract String name();
    
    public String toString() {
        return name();
    }

    
}
