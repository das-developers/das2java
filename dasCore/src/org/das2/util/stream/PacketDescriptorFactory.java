/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.util.stream;

import java.nio.ByteBuffer;

/**
 *
 * @author jbf
 */
public interface PacketDescriptorFactory {
    PacketDescriptor newPacketDescriptor(ByteBuffer data);
    PacketDescriptor mutatePacketDescriptor(PacketDescriptor orig, ByteBuffer data);
}
