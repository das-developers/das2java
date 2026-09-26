package org.das2.qds.buffer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Utility class for reading CCSDS telemetry streams
 *
 * @author jbf
 */
public class CcsdsReader {

    /**
     * extend this class to handle a particular packet type.
     */
    public static interface PacketHandler {

        /**
         * process the packet return a 0 if the data was processed correctly.
         * @param packetId
         * @param buf
         * @return 
         */
        int packet(int packetId, ByteBuffer buf);
    }

    //private Map<Integer,PacketHandler> handlers= new HashMap<>();
    PacketHandler[] handlers = new PacketHandler[65535];

    public void addPacketHandler(int packetId, PacketHandler h) {
        if (packetId > (65535)) {
            throw new IllegalArgumentException("packetId must be between 0 and 2^16.");
        }
        handlers[packetId] = h;
    }

    public void parse(File file) throws FileNotFoundException, IOException {
        RandomAccessFile f = new RandomAccessFile(file, "r");
        //FileChannel channel = f.getChannel();
        try ( FileChannel channel= f.getChannel() ) {
            ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            parse(buf);
        }

    }

    private SortedSet<Integer> appIds= new TreeSet<>();
    
    public void parse(ByteBuffer buf) throws IOException {
        int packetNumber = 0;
        
        while (buf.remaining() >= 6) {
            int offset = buf.position();
            int b0 = buf.get(offset) & 255;
            int b1 = buf.get(offset + 1) & 255;
            int b2 = buf.get(offset + 2) & 255;
            int b3 = buf.get(offset + 3) & 255;
            int b4 = buf.get(offset + 4) & 255;
            int b5 = buf.get(offset + 5) & 255;

            int apid = ( (b0 & 0x07 ) << 8) | b1;
            int sequenceCount = ( (b2 & 0x3f ) << 8) | b3;
            int dataLength = ( (b4 << 8) | b5) + 1;
            int packetLength = 6 + dataLength;

            if (packetLength > buf.remaining()) {
                throw new IOException(String.format("Incomplete packet at offset %d", offset));
            }

            int oldLimit = buf.limit();
            buf.limit(offset + packetLength);
            buf.position(offset);
            ByteBuffer packet = buf.slice();
            buf.limit(oldLimit);
            buf.position(offset);
            
            appIds.add(apid);
            
            PacketHandler handler = handlers[apid];
            if (handler != null) {
                handler.packet(apid, packet);
            }

            buf.position(offset + packetLength);
            packetNumber = packetNumber + 1;
        }

    }
    
    /**
     * get the set of appIds.
     * @return 
     */
    public Set<Integer> getAppIds() {
        return Collections.unmodifiableSortedSet(appIds);
    }
}
