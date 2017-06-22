/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qstream;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Long transfer type, to support cdfTT2000. 
 * NOTE THERE ARE MANY NUMBERS THAT CANNOT BE PRECISELY REPRESENTED BECAUSE
 * DOUBLES ARE USED TO COMMUNICATE DATA.
 * @author jbf
 */
public class LongTransferType extends TransferType {

    @Override
    public void write(double d, ByteBuffer buffer) {
        buffer.putLong((long)d);
    }

    @Override
    public double read(ByteBuffer buffer) {
        return buffer.getLong(); // caution!
    }

    @Override
    public int sizeBytes() {
        return 8;
    }

    @Override
    public boolean isAscii() {
        return false;
    }

    @Override
    public String name() {
        return "int8";
    }

    public static TransferType getByName( String ttype, Map<String,Object> properties ) {
        if ( ttype.equals("int8") ) {
            return new LongTransferType();
        } else {
            return null;
        }
    }
}
