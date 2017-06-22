/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qstream;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Four byte float type.
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
    public int sizeBytes() {
        return 4;
    }

    @Override
    public boolean isAscii() {
        return false;
    }

    @Override
    public String name() {
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
