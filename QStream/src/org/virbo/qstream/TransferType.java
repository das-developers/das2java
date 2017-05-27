
package org.virbo.qstream;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A transfer type is an encoding of a double on to the stream.  It must have a fixed length in bytes.
 * Some are pure ASCII, and isAscii can be used to check this.  
 * @author jbf
 */
public abstract class TransferType {

    protected static final Logger logger= Logger.getLogger("qstream");

    /**
     * returns a TranferType for the given name, or null if none is found.
     * @param ttype
     * @param properties map of dataset properties.  Introduced to pass in time Unit to AsciiTimeTransferType without unduly coupling codes.
     * @return returns a TranferType for the given name, or null if none is found.
     */
    public static TransferType getForName(String ttype, Map<String,Object> properties ) {

        logger.log(Level.FINEST, "getForName({0})", ttype);

        TransferType tt;
        
        tt= DoubleTransferType.getByName(ttype, properties);
        if ( tt!=null ) return tt;
        
        tt= FloatTransferType.getByName(ttype, properties);
        if ( tt!=null ) return tt;
        
        tt= IntegerTransferType.getByName(ttype, properties);
        if ( tt!=null ) return tt;

        tt= LongTransferType.getByName(ttype, properties);
        if ( tt!=null ) return tt;
        
        tt= ShortTransferType.getByName(ttype, properties);
        if ( tt!=null ) return tt;

        tt= AsciiTransferType.getByName(ttype, properties);
        if ( tt!=null ) return tt;
            
        tt= AsciiIntegerTransferType.getByName(ttype, properties);
        if ( tt!=null ) return tt;
        
        tt= AsciiTimeTransferType.getByName(ttype, properties);
        if ( tt!=null ) return tt;
        
        tt= AsciiHexIntegerTransferType.getByName(ttype, properties);
        if ( tt!=null ) return tt;

        return null;
    }
    
    /**
     * write the data to the buffer.  The buffer's position should be incremented by sizeBytes.
     * @param d the value to write.
     * @param buffer the byte buffer positioned to receive sizeByte bytes.
     */
    public abstract void write( double d, ByteBuffer buffer  );
    
    /**
     * read the data from the buffer.  The implementations will increment the 
     * buffer's position by sizeBytes.
     * @param buffer the container for the transfer type.
     * @return the value read
     */
    public abstract double read( ByteBuffer buffer );
    
    /**
     * return the number of bytes used by the transfer type.
     * @return the size of the type in bytes.
     */
    public abstract int sizeBytes();
    
    /**
     * return true if the transfer type uses ASCII-encodings to represent data.
     * It's assumed in this case that trailing whitespace can be modified for
     * readability.
     * @return true if the type is ASCII-based.
     */
    public abstract boolean isAscii( );
    
    /**
     * return a string identifying the TransferType.
     * @return the name
     */
    public abstract String name();
    
    @Override
    public String toString() {
        return name();
    }

    /**
     * convenient method which gently reminds that the ByteOrder needs to be
     * specified with ByteBuffers.  ByteBuffers are created with big endian 
     * turned on, but more often little endian is used.
     * @param recordLengthBytes
     * @param byteOrder
     * @return the byte buffer with the order set.
     */
    public static ByteBuffer allocate( int recordLengthBytes, ByteOrder byteOrder ) {
        ByteBuffer buf = ByteBuffer.allocate(recordLengthBytes);
        buf.order( ByteOrder.LITTLE_ENDIAN );
        return buf;
    }
    
}
