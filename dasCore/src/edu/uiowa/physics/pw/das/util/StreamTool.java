/* File: StreamTool.java
 * Copyright (C) 2002-2003 The University of Iowa
 * Created by: Jeremy Faden <jbf@space.physics.uiowa.edu>
 *             Jessica Swanner <jessica@space.physics.uiowa.edu>
 *             Edward E. West <eew@space.physics.uiowa.edu>
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

package edu.uiowa.physics.pw.das.util;

import edu.uiowa.physics.pw.das.stream.PacketDescriptor;
import edu.uiowa.physics.pw.das.stream.StreamDescriptor;
import edu.uiowa.physics.pw.das.stream.StreamException;
import edu.uiowa.physics.pw.das.stream.StreamHandler;
/**
 *
 * @author  jbf
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.io.StringBufferInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class StreamTool {
    
    public static final int MAX_DESCRIPTOR_LENGTH;
    static {
        MAX_DESCRIPTOR_LENGTH = 4096;
    }
    
    /** Creates a new instance of StreamTool */
    public StreamTool() {
    }
    
    public static class DelimeterNotFoundException extends Exception {
    }
    
    public static byte[] advanceTo( InputStream in, byte[] delim ) throws IOException, DelimeterNotFoundException {
        
        // Read from stream to delimeter, leaving the InputStream immediately after
        // and returning bytes from stream.
        
        byte[] data = new byte[4096];
        
        ArrayList list= new ArrayList();
        
        int bytesMatched=0;
        int matchIndex=0;
        
        int streamIndex=0;  // offset in bytes from the beginning of the stream
        
        int index=0;
        boolean notDone=true;
        
        int unreadOffset=-99999;
        int unreadLength=-99999;               
        
        int totalBytesRead=0;
        int offset=0;  // offset within byte[4096]
        
        while ( notDone ) {
            
            int byteRead= in.read();
            totalBytesRead++;
            
            if ( byteRead==-1 ) {
                notDone=false;
                
            } else {
            
                data[offset]= (byte)byteRead;
            
                if ( delim[bytesMatched]==byteRead ) {
                    bytesMatched++;
                } else {
                    bytesMatched=0;
                }
                if ( bytesMatched==delim.length ) {
                    notDone= false;
                    index= totalBytesRead - delim.length;
                }
            }
            
            if ( notDone ) {
                offset++;
                if ( offset==4096 ) {
                    list.add(data);
                    offset=0;
                    data= new byte[4096];
                }
            }
        }
        
        
        if ( bytesMatched!=delim.length ) {
            throw new StreamTool.DelimeterNotFoundException();
        }
        
        byte[] result= new byte[index];
        for ( int i=0; i<list.size(); i++ ) {
            System.arraycopy( list.get(i), 0, result, i*4096, 4096);
        }
        System.arraycopy( data, 0, result, list.size()*4096, index-(list.size()*4096) );
        return result;
        
    }
    
    public static void main( String[] args ) {
        PushbackInputStream in= new PushbackInputStream( new StringBufferInputStream("hello there silly man") );
        
        try {
            byte[] firstPart= advanceTo(in,"er".getBytes());
            System.out.println( "before part: "+ new String(firstPart));
            
            int buf;
            byte[] arrayBuf= new byte[1];
            
            System.out.print("after part: ");
            while ( (buf=in.read())!=-1 ) {
                arrayBuf[0]= (byte)buf;
                System.out.print( new String(arrayBuf) );
            }
            
            System.out.println();
            
        } catch ( IOException ex ) {
            System.out.println(ex);
        } catch ( StreamTool.DelimeterNotFoundException ex ) {
            System.out.println(ex);
        }
    }
    
    /** Read off XML data from the InputStream up to the termination of the XML.
     * XML data is returned in a byte array.  The InputStream is left just
     * following the XML terminator.  Processing is done with as little interpretation
     * as possible, so invalid XML will cause problems with little feedback about
     * what's wrong.
     */    
    public static byte[] readXML(PushbackInputStream in) throws IOException {
        ReadableByteChannel channel = Channels.newChannel(in);
        byte[] back = new byte[MAX_DESCRIPTOR_LENGTH];
        ByteBuffer buffer = ByteBuffer.wrap(back);
        channel.read(buffer);
        buffer.flip();
        ByteBuffer xml = readXML(buffer);
        byte[] bytes = new byte[xml.remaining()];
        xml.get(bytes);
        return bytes;
    }
    
    private static void eatWhiteSpace( ByteBuffer buffer ) {
        while (buffer.hasRemaining()) {
            char c = (char)(0xFF & buffer.get());
            if (!Character.isWhitespace(c)) {
                buffer.position(buffer.position() - 1);
                return;
            }
        }
    }

    public static ByteBuffer readXML(ByteBuffer input) throws IOException {
        int gtCount= 0;
        int tagCount= 0;
        int bufidx= 0;
        char lastChar;
        boolean inQuotes = false;
        boolean inTag = false;
        boolean tagContainsSlash = false;
        int b;
        ByteBuffer buffer = input.duplicate();
        buffer.mark();

        eatWhiteSpace(buffer);
        
        b = 0xFF & buffer.get();

        if ( ((char)b) != '<' ) {
            throw new IOException("found '" + ((char)b) + "', expected '<' at offset=" + buffer.position() + ".\n");
        } else {
            gtCount++;
            tagContainsSlash = false;
        }

        while ( buffer.hasRemaining() && ( gtCount > 0 || tagCount > 0 ) ) {
            lastChar = (char)b;
            b = 0xFF & buffer.get();

            if ( inQuotes && ((char)b) == '"' && lastChar != '\\') {
                inQuotes = false;
            }
            else if ( ((char)b) == '<' ) {
                gtCount++;
                inTag = true;
                tagContainsSlash = false; /* until proven otherwise */
            }
            else if ( b==(int)'>' ) {
                gtCount--;
                inTag = false;
                if ( lastChar != '/' ) {
                    if ( tagContainsSlash ) {
                        tagCount--;
                    } else {
                        tagCount++;
                    }
                }
            } else if ( b==(int)'/' ) {
                if ( lastChar==(int)'<' ) {
                    tagContainsSlash = true;
                }
            } else if ( ((char)b) == '"' && inTag) {
                inQuotes = true;
            }
        }

        if ( b==-1 ) {
            throw new IOException("unexpected end of file before xml termination\n");
        }

        eatWhiteSpace(buffer);

        int limit = buffer.limit();
        buffer.limit(buffer.position());
        buffer.reset();
        
        ByteBuffer result = buffer.slice();
        
        buffer.position(buffer.limit());
        buffer.limit(limit);
        
        return result;
    }
    
    public static void readStream(ReadableByteChannel stream, StreamHandler handler) throws StreamException {
        Map descriptors = new HashMap();
        ByteBuffer bigBuffer = ByteBuffer.allocate(4096);
        byte[] four = new byte[4];
        try {
            StreamDescriptor sd = getStreamDescriptor(stream, bigBuffer, four);
            if ("gzip".equals(sd.getCompression())) {
                stream = getGZIPChannel(stream);
            }
            while (stream.read(bigBuffer) != -1) {
                bigBuffer.flip();
                while (getChunk(bigBuffer, four, handler, descriptors));
                bigBuffer.compact();
            }
            handler.streamClosed(sd);
        }
        catch (StreamException se) {
            handler.streamException(se);
            throw se;
        }
        catch (IOException ioe) {
            StreamException se = new StreamException(ioe);
            handler.streamException(se);
            throw se;
        }
    }
    
    private static StreamDescriptor getStreamDescriptor(ReadableByteChannel stream, ByteBuffer bigBuffer, byte[] four) throws StreamException, IOException {
        bigBuffer.clear().limit(8);
        while (bigBuffer.hasRemaining() && stream.read(bigBuffer) != -1);
        if (bigBuffer.hasRemaining()) {
            throw new StreamException("Reached end of stream before encountering stream descriptor");
        }
        bigBuffer.flip();
        bigBuffer.get(four);
        if (isStreamDescriptorHeader(four)) {
            int contentLength = getContentLength(bigBuffer);
            bigBuffer.clear().limit(contentLength);
            while (bigBuffer.hasRemaining() && stream.read(bigBuffer) != -1);
            if (bigBuffer.hasRemaining()) {
                throw new StreamException("Reached end of stream before encountering stream descriptor");
            }
            bigBuffer.flip();
            Document doc = getXMLDocument(bigBuffer, contentLength);
            StreamDescriptor sd = new StreamDescriptor(doc.getDocumentElement());
            bigBuffer.clear();
            return sd;
        }
        else {
            throw new StreamException("Expecting stream descriptor header, found: '" + asciiBytesToString(four, 0, 4) + "'");
        }
    }
    
    private static boolean getChunk(ByteBuffer bigBuffer, byte[] four, StreamHandler handler, Map descriptors) throws StreamException, IOException {
        bigBuffer.mark();
        if (bigBuffer.remaining() < 4) {
            return false;
        }
        bigBuffer.get(four);
        if (isPacketDescriptorHeader(four)) {
            if (bigBuffer.remaining() < 4) {
                bigBuffer.reset();
                return false;
            }
            int contentLength = getContentLength(bigBuffer);
            if (bigBuffer.remaining() < contentLength) {
                bigBuffer.reset();
                return false;
            }
            Document doc = getXMLDocument(bigBuffer, contentLength);
            PacketDescriptor pd = new PacketDescriptor(doc.getDocumentElement());
            handler.packetDescriptor(pd);
            descriptors.put(asciiBytesToString(four, 1, 2), pd);
        }
        else if (isPacketHeader(four)) {
            String key = asciiBytesToString(four, 1, 2);
            PacketDescriptor pd = (PacketDescriptor)descriptors.get(key);
            int contentLength = pd.getSizeBytes();
            if (bigBuffer.remaining() < contentLength) {
                bigBuffer.reset();
                return false;
            }
            ByteBuffer slice = sliceBuffer(bigBuffer, contentLength);
            bigBuffer.position(bigBuffer.position() + contentLength);
            handler.packet(pd, slice);
        }
        else {
            throw new StreamException("Expected four byte header, found '" + new String(four) + "'");
        }
        return true;
    }
    
    private static ByteBuffer sliceBuffer(ByteBuffer buffer, int length) {
        ByteBuffer dup = buffer.duplicate();
        dup.limit(dup.position() + length);
        return dup.slice();
    }
    
    private static String asciiBytesToString(byte[] bytes, int offset, int length) {
        try {
            return new String(bytes, offset, length, "US-ASCII");
        }
        catch (java.io.UnsupportedEncodingException uee) {
            //All JVM implementations are required to support US-ASCII
            throw new RuntimeException(uee);
        }
    }
    
    private static boolean isStreamDescriptorHeader(byte[] four) {
        return four[0] == (byte)'[' && four[1] == (byte)'0'
            && four[2] == (byte)'0' && four[3] == (byte)']';
    }
    
    private static boolean isPacketDescriptorHeader(byte[] four) {
        return four[0] == (byte)'[' && four[3] == (byte)']'
            && Character.isDigit((char)four[1])
            && Character.isDigit((char)four[2]);
    }
    
    private static boolean isPacketHeader(byte[] four) {
        return four[0] == (byte)':' && four[3] == (byte)':'
            && Character.isDigit((char)four[1])
            && Character.isDigit((char)four[2]);
    }

    private static Document getXMLDocument(ByteBuffer buffer, int contentLength) throws StreamException, IOException {
        ByteBuffer xml = buffer.duplicate();
        xml.limit(xml.position() + contentLength);
        buffer.position(buffer.position() + contentLength);
        ByteBufferInputStream bbin = new ByteBufferInputStream(xml);
        InputStreamReader isr = new InputStreamReader(bbin);
        return parseHeader(isr);
    }
    
    private static int getContentLength(ByteBuffer buffer) throws StreamException {
        int contentLength = 0;
        for (int i = 0; i < 4; i++) {
            char c = (char)(0xFF & buffer.get());
            if (c == ' ') {
                continue;
            }
            if (!Character.isDigit(c)) {
                throw new StreamException("Invalid character in contentLength: '" + c + "'");
            }
            int digit = Character.digit(c, 10);
            contentLength = contentLength * 10 + digit;
        }
        return contentLength;
    }

    public static Document parseHeader(Reader header) throws StreamException {
        try {
            DocumentBuilder builder= DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource source = new InputSource(header);
            Document document= builder.parse(source);
            return document;
        }
        catch ( ParserConfigurationException ex ) {
            throw new RuntimeException(ex.getMessage());
        }
        catch ( SAXException ex ) {
            throw new StreamException(ex);
        }
        catch ( IOException ex) {
            throw new StreamException(ex);
        }
    }
    
    private static ReadableByteChannel getGZIPChannel(ReadableByteChannel channel) throws IOException {
        return Channels.newChannel(new GZIPInputStream(Channels.newInputStream(channel)));
    }
}