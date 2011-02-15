/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.qstream;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 *
 * @author jbf
 */
public class IntegerTransferType extends TransferType {

    @Override
    public void write(double d, ByteBuffer buffer) {
        buffer.putInt((int)d);
    }

    @Override
    public double read(ByteBuffer buffer) {
        return buffer.getInt();
    }

    @Override
    int sizeBytes() {
        return 4;
    }

    @Override
    boolean isAscii() {
        return false;
    }

    @Override
    String name() {
        return "int4";
    }

    public static TransferType getByName( String ttype, Map<String,Object> properties ) {
        if ( ttype.equals("int4") ) {
            return new IntegerTransferType();
        } else {
            return null;
        }
    }
}
