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

package edu.uiowa.physics.pw.das.stream;

import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.DatumVector;
import edu.uiowa.physics.pw.das.util.StreamTool;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
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
    private WritableByteChannel stream;
    private ByteBuffer bigBuffer = ByteBuffer.allocate(4096);
    private byte[] four = new byte[4];
    private int nextAvailable = 1;
    private DocumentBuilder builder;
    
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
            String header = (String)descriptors.get(pd);
            if ((pd.getSizeBytes() + 4) > bigBuffer.remaining()) {
                flush();
            }
            four[0] = four[3] = (byte)':';
            four[1] = (byte)header.charAt(0);
            four[2] = (byte)header.charAt(1);
            bigBuffer.put(four);
            pd.getXDescriptor().writeDatum(xTag, bigBuffer);
            for (int i = 0; i < pd.getYCount(); i++) {
                pd.getYDescriptor(i).write(vectors[i], bigBuffer);
            }
            bigBuffer.flip();
            stream.write(bigBuffer);
            bigBuffer.compact();
        }
        catch (IOException ioe) {
            throw new StreamException(ioe);
        }
    }
    
    public void packetDescriptor(PacketDescriptor pd) throws StreamException {
        try {
            String id = toString2(nextAvailable++);
            descriptors.put(pd, id);
            Document document = builder.newDocument();
            Element root = pd.getDOMElement(document);
            document.appendChild(root);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(out, "US-ASCII");
            StreamTool.formatHeader(document, writer);
            writer.flush();
            byte[] header = out.toByteArray();
            int length = header.length;
            four[0] = '[';
            four[1] = (byte)id.charAt(0);
            four[2] = (byte)id.charAt(1);
            four[3] = ']';
            bigBuffer.put(four);
            four[0] = (byte)Character.forDigit((length / 1000) % 10, 10);
            four[1] = (byte)Character.forDigit((length / 100) % 10, 10);
            four[2] = (byte)Character.forDigit((length / 10) % 10, 10);
            four[3] = (byte)Character.forDigit(length % 10, 10);
            bigBuffer.put(four);
            bigBuffer.put(header);
            bigBuffer.flip();
            stream.write(bigBuffer);
            bigBuffer.compact();
        }
        catch (IOException ioe) {
            throw new StreamException(ioe);
        }
    }
    
    public void streamClosed(StreamDescriptor sd) throws StreamException {
        flush();
    }
    
    public void streamDescriptor(StreamDescriptor sd) throws StreamException {
        try {
            Document document = builder.newDocument();
            Element root = sd.getDOMElement(document);
            document.appendChild(root);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(out, "US-ASCII");
            StreamTool.formatHeader(document, writer);
            writer.flush();
            byte[] header = out.toByteArray();
            int length = header.length;
            four[0] = '[';
            four[1] = four[2] = '0';
            four[3] = ']';
            bigBuffer.put(four);
            four[0] = (byte)Character.forDigit((length / 1000) % 10, 10);
            four[1] = (byte)Character.forDigit((length / 100) % 10, 10);
            four[2] = (byte)Character.forDigit((length / 10) % 10, 10);
            four[3] = (byte)Character.forDigit(length % 10, 10);
            bigBuffer.put(four);
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
        return Channels.newChannel(new DeflaterOutputStream(Channels.newOutputStream(channel)));
    }
}

