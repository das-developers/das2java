/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package test;

import java.nio.ByteBuffer;
import org.das2.qstream.AsciiTransferType;

/**
 *
 * @author jbf
 */
public class MiscTest {
    
    public static void testAsciiTransferType() {
        AsciiTransferType at= new AsciiTransferType(3,false);
        ByteBuffer buf;
        
        byte[] bytes= new byte[10];
        
        buf= ByteBuffer.wrap(bytes);
        at.write(1,buf);
        System.err.println( new String( bytes, 0, 3 ) );
        
        buf= ByteBuffer.wrap(bytes);
        at.write(999,buf);
        System.err.println( new String( bytes, 0, 3 ) );

        buf= ByteBuffer.wrap(bytes);
        at.write(-99,buf);
        System.err.println( new String( bytes, 0, 3 ) );
        
        buf= ByteBuffer.wrap(bytes);
        at.write(-999,buf);
        System.err.println( new String( bytes, 0, 3 ) );

        buf= ByteBuffer.wrap(bytes);        
        at.write(9999,buf);
        System.err.println( new String( bytes, 0, 3 ) );
        
        buf= ByteBuffer.wrap(bytes);
        at.write(1.5,buf);
        System.err.println( new String( bytes, 0, 3 ) );
        
    }
    
    public static void main( String[] args ) {
        testAsciiTransferType();
    }
}
