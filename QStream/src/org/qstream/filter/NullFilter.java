/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.qstream.filter;

import java.nio.ByteBuffer;
import org.virbo.qstream.PacketDescriptor;
import org.virbo.qstream.StreamDescriptor;
import org.virbo.qstream.StreamException;
import org.virbo.qstream.StreamHandler;


/**
 * Trivial filter just passes stream on to the next guy.  This is
 * useful for starting new filters or debugging.
 * 
 * @author jbf
 */
public class NullFilter implements StreamHandler {

    StreamHandler sink;

    double length=60;

    public void streamDescriptor(StreamDescriptor sd) throws StreamException {
        sink.streamDescriptor(sd);
    }

    public void packetDescriptor(PacketDescriptor pd) throws StreamException {
        sink.packetDescriptor(pd);
    }

    public void streamClosed(StreamDescriptor sd) throws StreamException {
        sink.streamClosed(sd);
    }

    public void streamException(StreamException se) throws StreamException {
        sink.streamException(se);
    }

    public void packet(PacketDescriptor pd, ByteBuffer data) throws StreamException {
        sink.packet(pd, data);
    }

}
