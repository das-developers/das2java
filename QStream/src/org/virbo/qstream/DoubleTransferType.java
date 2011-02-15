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
public class DoubleTransferType extends TransferType {

    @Override
    public void write(double d, ByteBuffer buffer) {
        buffer.putDouble(d);
    }

    @Override
    public double read(ByteBuffer buffer) {
        return buffer.getDouble();
    }

    @Override
    int sizeBytes() {
        return 8;
    }

    @Override
    boolean isAscii() {
        return false;
    }

    @Override
    String name() {
        return "double";
    }

    public static TransferType getByName( String ttype, Map<String,Object> properties ) {
        if ( ttype.equals("double" ) || ttype.equals("real8") ) {
            return new DoubleTransferType();
        } else {
            return null;
        }
    }
}
