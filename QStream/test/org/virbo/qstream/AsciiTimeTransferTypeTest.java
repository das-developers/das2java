/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.qstream;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.Units;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.das2.qds.QDataSet;
import static org.junit.Assert.*;

/**
 *
 * @author jbf
 */
public class AsciiTimeTransferTypeTest {

    private static final Logger logger= Logger.getLogger("qstream");

    public AsciiTimeTransferTypeTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    /**
     * Test of write method, of class AsciiTimeTransferType.
     */
    @Test
    public void testWrite() {
        System.out.println("write");
        double d = 0.0;
        byte[] buf= new byte[30];
        ByteBuffer buffer = ByteBuffer.wrap(buf);
        AsciiTimeTransferType instance =  new AsciiTimeTransferType(30,Units.us2000);
        instance.write(d, buffer);
        assertEquals( new String(buf), "2000-01-01T00:00:00.000000000 " );

        buffer.rewind();
        instance.write( Units.us2000.getFillDouble(), buffer );
        assertEquals( new String(buf), "fill                          " );
    }

    /**
     * Test of read method, of class AsciiTimeTransferType.
     */
    @Test
    public void testRead() {
        System.out.println("read");
        ByteBuffer buffer=null;
        try {
            buffer = ByteBuffer.wrap("2000-01-01T00:00:00.000 ".getBytes("US-ASCII"));
        } catch (UnsupportedEncodingException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        AsciiTimeTransferType instance = new AsciiTimeTransferType(24,Units.us2000);
        double expResult = 0.0;
        double result = instance.read(buffer);
        assertEquals( expResult, result, 0.0 );
    }

    /**
     * Test of getByName method, of class AsciiTimeTransferType.
     */
    @Test
    public void testGetByName() {
        System.out.println("getByName");
        String name = "time17";
        Map<String, Object> properties = Collections.singletonMap( QDataSet.UNITS, (Object)Units.us2000 );
        TransferType result = AsciiTimeTransferType.getByName(name, properties);
        assertEquals(result.sizeBytes(),17);
    }

    /**
     * Test of name method, of class AsciiTimeTransferType.
     */
    @Test
    public void testName() {
        System.out.println("name");
        AsciiTimeTransferType instance = new AsciiTimeTransferType(24,Units.us2000);
        String expResult = "time24";
        String result = instance.name();
        assertEquals(expResult, result);
    }

}