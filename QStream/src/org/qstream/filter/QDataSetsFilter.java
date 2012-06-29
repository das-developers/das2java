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
 * Use this to promote the abstraction of the stream to QDataSets.  This was
 * introduced when it became clear that to introduce an FFT filter would be
 * quite difficult with the StreamHandler interface.
 * @author jbf
 */
public class QDataSetsFilter implements StreamHandler {

    /**
     * send packets on to here
     */
    StreamHandler sink;

    public void streamDescriptor(StreamDescriptor sd) throws StreamException {
        sink.streamDescriptor(sd);
    }

    public void packetDescriptor(PacketDescriptor pd) throws StreamException {
        sink.packetDescriptor(pd);
        //TODO: Fire off a QDataSet packet when the values are in-line.
    }

    public void streamClosed(StreamDescriptor sd) throws StreamException {
        sink.streamClosed(sd);
    }

    public void streamException(StreamException se) throws StreamException {
        sink.streamException(se);
    }

    public void packet(PacketDescriptor pd, ByteBuffer data) throws StreamException {
        //TODO: form QDataSet when the values are not in-line and only one packet exists.  Fire off a QDataSet packet.
        sink.packet(pd, data);
    }

}
