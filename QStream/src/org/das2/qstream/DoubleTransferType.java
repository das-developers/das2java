/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qstream;

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
    public int sizeBytes() {
        return 8;
    }

    @Override
    public boolean isAscii() {
        return false;
    }

    @Override
    public String name() {
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
