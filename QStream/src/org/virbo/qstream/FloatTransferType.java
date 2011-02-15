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
public class FloatTransferType extends TransferType {

    @Override
    public void write(double d, ByteBuffer buffer) {
        buffer.putFloat((float)d);
    }

    @Override
    public double read(ByteBuffer buffer) {
        return buffer.getFloat();
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
        return "float";
    }

    public static TransferType getByName( String ttype, Map<String,Object> properties ) {
        if ( ttype.equals("float" ) || ttype.equals("real4") ) {
            return new FloatTransferType();
        } else {
            return null;
        }
    }
}
