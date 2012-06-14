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
public class ShortTransferType extends TransferType {

    @Override
    public void write(double d, ByteBuffer buffer) {
        buffer.putShort((short)d);
    }

    @Override
    public double read(ByteBuffer buffer) {
        return buffer.getShort();
    }

    @Override
    public int sizeBytes() {
        return 2;
    }

    @Override
    public boolean isAscii() {
        return false;
    }

    @Override
    String name() {
        return "int2";
    }

    public static TransferType getByName( String ttype, Map<String,Object> properties ) {
        if ( ttype.equals("int2") ) {
            return new ShortTransferType();
        } else {
            return null;
        }
    }
}
