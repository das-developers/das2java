/* File: StreamProducer.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on February 12, 2004, 2:59 PM
 *      by Edward E. West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.das2.stream;

import org.das2.datum.Datum;
import org.das2.datum.DatumVector;
import org.das2.util.DeflaterChannel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.*;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 *
 * @author  eew
 */
public class StreamProducer implements StreamHandler {
    
    private Map descriptors = new IdentityHashMap();
    private Map idMap = new HashMap();
    private WritableByteChannel stream;
    private ByteBuffer bigBuffer = ByteBuffer.allocate(4096);
    private byte[] six = new byte[6];
    private int nextAvail = 1;
    private DocumentBuilder builder;
    
    private static class IdentitySet extends AbstractCollection implements Set {
        
        private IdentityHashMap map = new IdentityHashMap();
        private static final Object VALUE = new Object();
        
        public Iterator iterator() {
            return map.keySet().iterator();
        }
        
        public int size() {
            return map.size();
        }
        
    }
    
    /** Creates a new instance of StreamProducer */
    public StreamProducer(WritableByteChannel stream) {
        this.stream = stream;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        }
        catch (ParserConfigurationException pce) {
            throw new RuntimeException(pce);
        }
    }
    
    public void packet(PacketDescriptor pd, Datum xTag, DatumVector[] vectors) throws StreamException {
        try {
            if (!descriptors.containsKey(pd)) {
                packetDescriptor(pd);
            }
            String header = (String)descriptors.get(pd);
            if (pd.getSizeBytes() > bigBuffer.capacity()) {
                resizeBuffer(pd.getSizeBytes() + pd.getSizeBytes() >> 1);
            }
            if ((pd.getSizeBytes() + 4) > bigBuffer.remaining()) {
                flush();
            }
            six[0] = six[3] = (byte)':';
            six[1] = (byte)header.charAt(0);
            six[2] = (byte)header.charAt(1);
            bigBuffer.put(six, 0, 4);
            pd.getXDescriptor().writeDatum(xTag, bigBuffer);
            for (int i = 0; i < pd.getYCount(); i++) {
                pd.getYDescriptor(i).write(vectors[i], bigBuffer);
            }
            //Ascii format hack
            int lastChar = bigBuffer.position() - 1;
            SkeletonDescriptor lastY = pd.getYDescriptor(pd.getYCount() - 1);
            if (((lastY instanceof StreamYScanDescriptor
                  && ((StreamYScanDescriptor)lastY).getDataTransferType().isAscii())
                 || (lastY instanceof StreamScalarDescriptor
                     && ((StreamScalarDescriptor)lastY).getDataTransferType().isAscii()))
                && Character.isWhitespace((char)bigBuffer.get(lastChar))) {
                bigBuffer.put(lastChar, (byte)'\n');
            }
            //End of Ascii format hack
            bigBuffer.flip();
            stream.write(bigBuffer);
            bigBuffer.compact();
        }
        catch (IOException ioe) {
            throw new StreamException(ioe);
        }
    }
    
    private int nextAvailable() {
        int result = nextAvail;
        if (nextAvail == 99) {
            nextAvail = 1;
        }
        else {
            nextAvail++;
        }
        return result;
    }
    
    public void packetDescriptor(PacketDescriptor pd) throws StreamException {
        try {
            String id = toString2(nextAvailable());
            if (idMap.containsKey(id)) {
                Object d = idMap.get(id);
                descriptors.remove(d);
            }
            descriptors.put(pd, id);
            idMap.put(id, pd);
            Document document = builder.newDocument();
            Element root = pd.getDOMElement(document);
            document.appendChild(root);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(out, "US-ASCII");
            StreamTool.formatHeader(document, writer);
            writer.append("\n");
            writer.flush();
            byte[] header = out.toByteArray();
            int length = header.length;
            if ( length>999999 ) {
                throw new IllegalArgumentException("packet header is longer than can be formatted to a packet header (longer than 999999 bytes).");
            }
            if (bigBuffer.remaining() < (length + 10)) {
                flush();
            }
            if (bigBuffer.capacity() < (length + 10)) {
                resizeBuffer(length + (length / 2) + 15);
            }
            six[0] = '[';
            six[1] = (byte)id.charAt(0);
            six[2] = (byte)id.charAt(1);
            six[3] = ']';
            bigBuffer.put(six, 0, 4);
            byte[] ssix= String.format( "%06d",length).getBytes("US-ASCII");
            bigBuffer.put(ssix);
            bigBuffer.put(header);
            bigBuffer.flip();
            stream.write(bigBuffer);
            bigBuffer.compact();
        }
        catch (IOException ioe) {
            throw new StreamException(ioe);
        }
    }
    
    public void resizeBuffer(int size) throws StreamException {
        flush();
        bigBuffer = ByteBuffer.allocate(size);
    }
    
    public void streamClosed(StreamDescriptor sd) throws StreamException {
        flush();
        try {
            stream.close();
        }
        catch (IOException ioe) {
            throw new StreamException(ioe);
        }
    }
    
    public void streamDescriptor(StreamDescriptor sd) throws StreamException {
        try {
            Document document = builder.newDocument();
            Element root = sd.getDOMElement(document);
            document.appendChild(root);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
            StreamTool.formatHeader(document, writer);
            writer.append("\n");
            writer.flush();
            byte[] header = out.toByteArray();
            int length = header.length;
            if ( length>999999 ) {
                throw new IllegalArgumentException("packet header is longer than can be formatted to a packet header (longer than 999999 bytes).");
            }
            if (bigBuffer.remaining() < (length + 10)) {
                flush();
            }
            if (bigBuffer.capacity() < (length + 10)) {
                resizeBuffer(length + (length / 2) + 15);
            }
            
            six[0] = '[';
            six[1] = six[2] = '0';
            six[3] = ']';
            bigBuffer.put(six, 0, 4);
            six[0] = (byte)Character.forDigit((length / 100000) % 10, 10);
            six[1] = (byte)Character.forDigit((length / 10000) % 10, 10);
            six[2] = (byte)Character.forDigit((length / 1000) % 10, 10);
            six[3] = (byte)Character.forDigit((length / 100) % 10, 10);
            six[4] = (byte)Character.forDigit((length / 10) % 10, 10);
            six[5] = (byte)Character.forDigit(length % 10, 10);
            bigBuffer.put(six);
            bigBuffer.put(header);
            flush();
            if ("deflate".equals(sd.getCompression())) {
                stream = getDeflaterChannel(stream);
            }
        }
        catch (IOException ioe) {
            throw new StreamException(ioe);
        }
    }
    
    public void streamException(StreamException se) throws StreamException {
    }
    
    
    
    public void flush() throws StreamException {
        try {
            bigBuffer.flip();
            while (bigBuffer.hasRemaining()) {
                stream.write(bigBuffer);
            }
            bigBuffer.clear();
        }
        catch (IOException ioe) {
            throw new StreamException(ioe);
        }
    }
    
    private static String toString4(int i) {
        if (i > 9999) {
            throw new IllegalArgumentException("header is too big");
        }
        else if (i < 10) {
            return "000" + i;
        }
        else if (i < 100) {
            return "00" + i;
        }
        else if (i < 1000) {
            return "0" + i;
        }
        else {
            return String.valueOf(i);
        }
    }
    
    private static String toString2(int i) {
        if (i > 99) {
            throw new IllegalArgumentException("header number cannot be > 99");
        }
        else if (i < 10) {
            return "0" + i;
        }
        else {
            return String.valueOf(i);
        }
    }
    
    private static WritableByteChannel getDeflaterChannel(WritableByteChannel channel) throws IOException {
        return new DeflaterChannel(channel);
        //return Channels.newChannel(new DeflaterOutputStream(Channels.newOutputStream(channel)));
    }
    
    public void streamComment(StreamComment sc) throws StreamException {
    }
    
}

