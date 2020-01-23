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
package org.das2.stream;

import org.das2.datum.Datum;
import org.das2.datum.DatumVector;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
/**
 *
 * @author  jbf
 */
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.das2.dataset.NoDataInIntervalException;
//import org.apache.xml.serialize.Method;
//import org.apache.xml.serialize.OutputFormat;
//import org.apache.xml.serialize.XMLSerializer;
import org.das2.util.ByteBufferInputStream;
import org.das2.util.InflaterChannel;
import org.das2.util.LoggerManager;
import org.w3c.dom.*;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class StreamTool {
    private static final Logger logger= LoggerManager.getLogger("das2.stream.d2s");

    private StreamTool() {
        // utility class 
    }

    public static class DelimeterNotFoundException extends Exception {
    }

    public static byte[] advanceTo(InputStream in, byte[] delim) throws IOException, DelimeterNotFoundException {

        // Read from stream to delimeter, leaving the InputStream immediately after
        // and returning bytes from stream.

        byte[] data = new byte[4096];

        ArrayList list = new ArrayList();

        int bytesMatched = 0;
        int matchIndex = 0;

        int streamIndex = 0;  // offset in bytes from the beginning of the stream

        int index = 0;
        boolean notDone = true;

        int unreadOffset = -99999;
        int unreadLength = -99999;

        int totalBytesRead = 0;
        int offset = 0;  // offset within byte[4096]

        while (notDone) {

            int byteRead = in.read();
            totalBytesRead++;

            if (byteRead == -1) {
                notDone = false;

            } else {

                data[offset] = (byte) byteRead;

                if (delim[bytesMatched] == byteRead) {
                    bytesMatched++;
                } else {
                    bytesMatched = 0;
                }
                if (bytesMatched == delim.length) {
                    notDone = false;
                    index = totalBytesRead - delim.length;
                }
            }

            if (notDone) {
                offset++;
                if (offset == 4096) {
                    list.add(data);
                    offset = 0;
                    data = new byte[4096];
                }
            }
        }


        if (bytesMatched != delim.length) {
            throw new StreamTool.DelimeterNotFoundException();
        }

        byte[] result = new byte[index];
        for (int i = 0; i < list.size(); i++) {
            System.arraycopy(list.get(i), 0, result, i * 4096, 4096);
        }
        System.arraycopy(data, 0, result, list.size() * 4096, index - (list.size() * 4096));
        return result;

    }

    /** Read off XML data from the InputStream up to the termination of the XML.
     * XML data is returned in a byte array.  The InputStream is left just
     * following the XML terminator.  Processing is done with as little interpretation
     * as possible, so invalid XML will cause problems with little feedback about
     * what's wrong.
     */
    public static byte[] readXML(PushbackInputStream in) throws IOException {
        ReadableByteChannel channel = Channels.newChannel(in);
        byte[] back = new byte[4096];
        ByteBuffer buffer = ByteBuffer.wrap(back);
        channel.read(buffer);
        buffer.flip();
        ByteBuffer xml = readXML(buffer);
        byte[] bytes = new byte[xml.remaining()];
        xml.get(bytes);
        return bytes;
    }

    private static void eatWhiteSpace(ByteBuffer buffer) {
        while (buffer.hasRemaining()) {
            char c = (char) (0xFF & buffer.get());
            if (!Character.isWhitespace(c)) {
                buffer.position(buffer.position() - 1);
                return;
            }
        }
    }

    /**
     * return a ByteBuffer containing just the next xml portion of the stream.
     * @param input
     * @return
     * @throws IOException 
     */
    public static ByteBuffer readXML(ByteBuffer input) throws IOException {
        int gtCount = 0;
        int tagCount = 0;
        int bufidx = 0;
        char lastChar;
        boolean inQuotes = false;
        boolean inTag = false;
        boolean tagContainsSlash = false;
        int b;
        ByteBuffer buffer = input.duplicate();
        buffer.mark();

        eatWhiteSpace(buffer);

        b = 0xFF & buffer.get();

        if (((char) b) != '<') {
            throw new IOException("found '" + ((char) b) + "', expected '<' at offset=" + buffer.position() + ".\n");
        } else {
            gtCount++;
            tagContainsSlash = false;
        }

        while (buffer.hasRemaining() && (gtCount > 0 || tagCount > 0)) {
            lastChar = (char) b;
            b = 0xFF & buffer.get();

            if (inQuotes && ((char) b) == '"' && lastChar != '\\') {
                inQuotes = false;
            } else if (((char) b) == '<') {
                gtCount++;
                inTag = true;
                tagContainsSlash = false; /* until proven otherwise */
            } else if (b == (int) '>') {
                gtCount--;
                inTag = false;
                if (lastChar != '/') {
                    if (tagContainsSlash) {
                        tagCount--;
                    } else {
                        tagCount++;
                    }
                }
            } else if (b == (int) '/') {
                if (lastChar == (int) '<') {
                    tagContainsSlash = true;
                }
            } else if (((char) b) == '"' && inTag) {
                inQuotes = true;
            }
        }

        //findbugs INT_BAD_COMPARISON_WITH_NONNEGATIVE_VALUE.  b must be positive, this was probably leftover from an earlier implementation.
        //if (b == -1) {
        //    throw new IOException("unexpected end of file before xml termination\n");
        //}

        eatWhiteSpace(buffer);

        int limit = buffer.limit();
        buffer.limit(buffer.position());
        buffer.reset();

        ByteBuffer result = buffer.slice();

        buffer.position(buffer.limit());
        buffer.limit(limit);

        return result;
    }

    private static class ReadStreamStructure {

        private ReadableByteChannel stream;
        private ByteBuffer bigBuffer = ByteBuffer.allocate(4096);
        private byte[] four = new byte[4];
        private StreamHandler handler;
        private Map descriptors = new HashMap();
        private int byteOffset = 0;  // byte offset into file of the end of the buffer.
        private int descriptorCount = 0; // successfully read descriptors
        private int packetCount = 0; // successfully read packets

        private ReadStreamStructure(ReadableByteChannel stream, StreamHandler handler) {
            this.stream = stream;
            this.handler = handler;
        }
        @Override
        public String toString() {
            return "\ndescriptorCount="+descriptorCount+
                    "\npacketCount="+packetCount+
                    "\nbyteOffset="+byteOffset+
                    "\ncarotPos="+(byteOffset-bigBuffer.limit()+bigBuffer.position())+
                    "\nbuffer="+String.valueOf(bigBuffer);
        }
    }

    public static void readStream(ReadableByteChannel stream, StreamHandler handler) throws StreamException {
        ReadStreamStructure struct = new ReadStreamStructure(stream, handler);
        try {
			   // Stream properties are loaded in the next line, but not placed in a dataset
			   // yet.
            StreamDescriptor sd = getStreamDescriptor(struct);
            if ("deflate".equals(sd.getCompression())) {
                stream = getInflaterChannel(stream);
            }
            handler.streamDescriptor(sd);
            struct.descriptorCount++;
            int bytesRead;
            //int totalBytesRead = 0;

            while ((bytesRead = stream.read(struct.bigBuffer)) != -1) {
                struct.byteOffset += struct.bigBuffer.position();
                struct.bigBuffer.flip();
                
                if ( bytesRead==0 && stream instanceof InflaterChannel ) break; // https://bugs-pw.physics.uiowa.edu/mantis/view.php?id=441 
                
                //totalBytesRead += bytesRead;
                //System.err.println("d2s bytesRead="+bytesRead+" total="+totalBytesRead );
                //if ( totalBytesRead>318260 ) {
                //  System.err.println("here");
                //}
                while (getChunk(struct)) {
                // this block is empty
                }
                struct.bigBuffer.compact();
            }
            if ( struct.bigBuffer.position()!=0 ) {
                throw new StreamException("Stream ends with partial packet");
            }
            
            handler.streamClosed(sd);
        } catch (StreamException se) {
            handler.streamException(se);
            throw se;
        } catch (InterruptedIOException ex) {
            throw new StreamException(ex);
        } catch (IOException ioe) {
            StreamException se = new StreamException(ioe);
            handler.streamException(se);
            throw se;
        }
    }

    private static StreamDescriptor getStreamDescriptor(ReadStreamStructure struct) throws StreamException, IOException {
        struct.bigBuffer.clear().limit(10);
        while (struct.bigBuffer.hasRemaining() && struct.stream.read(struct.bigBuffer) != -1) {
            //do nothing
            ;
        }
        if ( struct.byteOffset==0 && struct.bigBuffer.position()==0 ) {
            throw new StreamException("Stream is empty");
        }
        if (struct.bigBuffer.hasRemaining()) {
            throw new StreamException("Reached end of stream before encountering stream descriptor");
        }
        struct.byteOffset += struct.bigBuffer.position();
        struct.bigBuffer.flip();
        struct.bigBuffer.get(struct.four);
        if (isStreamDescriptorHeader(struct.four)) {
            int contentLength = getContentLength(struct.bigBuffer);
            if (contentLength == 0) {
                throw new StreamException("streamDescriptor content length is 0.");
            }
            struct.bigBuffer.clear().limit(contentLength);
            while (struct.bigBuffer.hasRemaining() && struct.stream.read(struct.bigBuffer) != -1) {
                // do nothing
                ;
            }
            if (struct.bigBuffer.hasRemaining()) {
                throw new StreamException("Reached end of stream before encountering stream descriptor");
            }
            struct.byteOffset += struct.bigBuffer.position();
            struct.bigBuffer.flip();

            try {
                Document doc = getXMLDocument(struct.bigBuffer, contentLength);
                Element root = doc.getDocumentElement();
                if (root.getTagName().equals("stream")) {
                    StreamDescriptor sd = new StreamDescriptor(doc.getDocumentElement());
                    struct.bigBuffer.clear();
                    return sd;
                } else if (root.getTagName().equals("exception")) {
                    throw exception(root);
                } else {
                    throw new StreamException("Unexpected xml header, expecting stream or exception, received: " + root.getTagName());
                }
            } catch (SAXException ex) {
                String msg = getSAXParseExceptionMessage(ex, struct, contentLength );
                throw new StreamException(msg);
            }
        } else {
            String s = readMore(struct);
            throw new StreamException("Expecting stream descriptor header, found: '" + asciiBytesToString(struct.four, 0, 4) + "' beginning \n'" + s + "'");
        }
    }

    /**
     * call this after error, to get another 100 bytes off the stream
     */
    private static String readMore(ReadStreamStructure struct) throws IOException {
        struct.bigBuffer.position(0);
        struct.bigBuffer.limit(10);
        byte[] bytes10 = new byte[10];
        struct.bigBuffer.get(bytes10);
        String s = new String(bytes10);
        struct.bigBuffer.limit(1000);
        struct.bigBuffer.position(0);
        while (struct.bigBuffer.hasRemaining() && struct.stream.read(struct.bigBuffer) != -1) {
            // do nothing
            ;
        }
        int p = struct.bigBuffer.position();
        byte[] bytes = new byte[p];
        struct.bigBuffer.flip();
        struct.bigBuffer.get(bytes);
        s = s + new String(bytes);
        return s;
    }

    private static String getSAXParseExceptionMessage(final SAXException ex, final ReadStreamStructure struct,int contentLength) {
        String loc = null;
        if (ex instanceof SAXParseException) {
            SAXParseException spe = (SAXParseException) ex;
            loc = "Relative to packet start, line number is " + spe.getLineNumber() + ", column is " + spe.getColumnNumber();
        }
        
        int bufOffset= struct.byteOffset - struct.bigBuffer.limit();
        
        String msg = "xml parser fails with the message: \"" + ex.getMessage() +
                "\" within the packet ending at byte offset " + ( bufOffset + struct.bigBuffer.position() ) + ".";
        if (ex.getMessage().contains("trailing")) {
            msg += "\nNon-whitespace data found after xml closing tag, probably caused by content length error.";
            int i;
            // find the end of the closing xml tag.
            for (i = bufOffset + struct.bigBuffer.position() - 1; i > 0; i--) {
                int bpos= i - bufOffset ;
                if (struct.bigBuffer.get( bpos ) == '>' ) {
                    break;
                } else {
                    System.err.println( (char)struct.bigBuffer.get(bpos) );
                }
            }
            for ( ; i < bufOffset + struct.bigBuffer.position() ; i++) {
                int bpos= i - bufOffset;
                if (struct.bigBuffer.get(bpos) == '[' || struct.bigBuffer.get(bpos)==':' ) {
                    break;
                }
            }
            if (i > 0) {
                int error= i - ( struct.bigBuffer.position() + bufOffset );
                //int error= ( i + bufOffset ) - ( struct.byteOffset - 10 );
                NumberFormat nf = new DecimalFormat("000000");
                msg += "\nContent length was " + nf.format(contentLength) 
                        + ", maybe it should have been " 
                        + nf.format(contentLength+error) + ".";
            }
        }

        if (loc != null) {
            msg += "  " + loc;
        }
        return msg;
    }
	
	// Copies over contents of current buffer and generates a new one that can hold
	// a little more than the content length.  Old buffer should be discarded.
	private static ByteBuffer biggerBuffer(ByteBuffer buf, int nContentLen)
	{
		ByteBuffer temp = ByteBuffer.allocate(8 + nContentLen + nContentLen / 10);
		buf.reset();   // set read position to the mark
		temp.put(buf); // read in data from the position to the limit
		temp.flip();   // set limit to the current position and the read position back to zero
		
      return temp;   // this buffer should have all the old buffers data and be bigger
	 }

    private static final StreamException exception(Element exception) {
        String type = exception.getAttribute("type");
        String message= exception.getAttribute("message");
        if ( type.equals( StreamException.NO_DATA_IN_INTERVAL ) ) {
            NoDataInIntervalException ex = new NoDataInIntervalException(message);
            StreamException se= new StreamException(ex);
            return se;
        } else if ( type.equals(StreamException.EMPTY_RESPONSE_FROM_READER ) ) {
            StreamException se = new StreamException( "Empty response from reader\n"+message );
            return se;
        } else {
            return new StreamException(message);
        }        
    }

    private static boolean getChunk(ReadStreamStructure struct) throws StreamException, IOException {
        struct.bigBuffer.mark();
        if (struct.bigBuffer.remaining() < 4) {
            return false;
        }
        struct.bigBuffer.get(struct.four);
        if ( struct.four[0]== 0xa ) {
            // Jeremy's temperature sensors had the wrong content length for about a year.  Short of fixing this data, check for this for now.
            logger.log(Level.WARNING, "off-by-one error in the content length preceding byte offset {0}", (struct.byteOffset + struct.bigBuffer.position()  - struct.bigBuffer.limit() - 10 -4));
            struct.four[0]= struct.four[1];
            struct.four[1]= struct.four[2];
            struct.four[2]= struct.four[3];
            struct.four[3]= struct.bigBuffer.get();
        }
        if (isPacketDescriptorHeader(struct.four)) {
            if (struct.bigBuffer.remaining() < 6) {
                struct.bigBuffer.reset();
                return false;
            }
            int contentLength = getContentLength(struct.bigBuffer);
            if (contentLength == 0) {
                throw new StreamException("packetDescriptor content length is 0.");
            }
				
				// Have to be able to hold the entire packet header in the buffer
            if (struct.bigBuffer.capacity() < contentLength) {
					struct.bigBuffer = biggerBuffer(struct.bigBuffer, contentLength);
               return false;
            } 
				
				if (struct.bigBuffer.remaining() < contentLength) {
               struct.bigBuffer.reset();
               return false;
            }

            logger.log(Level.FINE, "packetDescriptor len={0}", contentLength);
            try {
                Document doc = getXMLDocument(struct.bigBuffer, contentLength);
                Element root = doc.getDocumentElement();
                if (root.getTagName().equals("packet")) {
                    int id= 10*(struct.four[1]-48)+(struct.four[2]-48);
                    if ( id<0 || id>99 ) logger.warning("unable to parse id for packetDescriptor");
                    PacketDescriptor pd = new PacketDescriptor(id,doc.getDocumentElement());
                    struct.handler.packetDescriptor(pd);
                    struct.descriptors.put(asciiBytesToString(struct.four, 1, 2), pd);
                } else if (root.getTagName().equals("exception")) {
                    throw exception(root);
                } else if (root.getTagName().equals("comment")) {
                    struct.handler.streamComment(new StreamComment(doc.getDocumentElement()));
                } else {
                    throw new StreamException("Unexpected xml header, expecting stream or exception, received: " + root.getTagName());
                }
                struct.descriptorCount++;
            } catch (SAXException ex) {
                String msg = getSAXParseExceptionMessage(ex, struct, contentLength );
                throw new StreamException(msg);
            }
        } else if (isPacketHeader(struct.four)) {
            String key = asciiBytesToString(struct.four, 1, 2);
            PacketDescriptor pd = (PacketDescriptor) struct.descriptors.get(key);
            int contentLength = pd.getSizeBytes();
				
				// Have to be able to hold at least one packet in the buffer
				if (struct.bigBuffer.capacity() < contentLength) {
					struct.bigBuffer = biggerBuffer(struct.bigBuffer, contentLength);
               return false;
            } 
				
            if (struct.bigBuffer.remaining() < contentLength) {
					struct.bigBuffer.reset();
               return false;
            }
            logger.log(Level.FINE, "packetHeader len={0}", contentLength);
            int yCount = pd.getYCount();
            Datum xTag = pd.getXDescriptor().readDatum(struct.bigBuffer);
            DatumVector[] vectors = new DatumVector[yCount];
            for (int i = 0; i < yCount; i++) {
                vectors[i] = pd.getYDescriptor(i).read(struct.bigBuffer);
            }
            struct.handler.packet(pd, xTag, vectors);
            struct.packetCount++;
        } else {
            String msg = "Expected four byte header, found '";
            String s = new String(struct.four);
            s = s.replaceAll("\n", "\\\\n"); // TODO: what's the right way to say this?
            msg += s;
            msg += "' at byte offset " + (struct.byteOffset + struct.bigBuffer.position()  - struct.bigBuffer.limit() - 10 -4 );
            msg += " after reading " + struct.descriptorCount + " descriptors and " + struct.packetCount + " packets.";

            if ( s.contains("\\n==") ) {
                byte[] buf= new byte[struct.bigBuffer.limit()-struct.bigBuffer.position()];
                struct.bigBuffer.get(buf);
                String ss= new String( buf );
                if ( ss.contains("SPICELIB") ) {
                    msg+= "\nThis appears to be a message from SPICE:\n";
                    msg+= ss;
                }
                
            }

            throw new StreamException(msg);
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
        } catch (UnsupportedEncodingException uee) {
            //All JVM implementations are required to support US-ASCII
            throw new RuntimeException(uee);
        }
    }

    private static boolean isStreamDescriptorHeader(byte[] four) {
        return four[0] == (byte) '[' && four[1] == (byte) '0' && four[2] == (byte) '0' && four[3] == (byte) ']';
    }

    private static boolean isPacketDescriptorHeader(byte[] four) {
        return four[0] == (byte) '[' && four[3] == (byte) ']' && (Character.isDigit((char) four[1]) && Character.isDigit((char) four[2]) || (char) four[1] == 'x' && (char) four[2] == 'x');
    }

    private static boolean isPacketHeader(byte[] four) {
        return four[0] == (byte) ':' && four[3] == (byte) ':' && Character.isDigit((char) four[1]) && Character.isDigit((char) four[2]);
    }

    private static Document getXMLDocument(ByteBuffer buffer, int contentLength) throws StreamException, IOException, SAXException {
        ByteBuffer xml = buffer.duplicate();

        ByteBuffer xml2=null;
        if ( logger.isLoggable(Level.FINEST) ) xml2= buffer.duplicate(); // for debugging.
        
        xml.limit(xml.position() + contentLength);
        if ( logger.isLoggable(Level.FINEST) ) xml2.limit(xml.position() + contentLength);
        
        buffer.position(buffer.position() + contentLength);
        final boolean DEBUG = false;
        if (DEBUG) {
            int pos = xml.position();
            byte[] bytes = new byte[xml.limit() - xml.position()];
            xml.get(bytes);
            xml.position(pos);
            System.err.println(new String(bytes));
        }
        
        ByteBufferInputStream bbin;
        InputStreamReader isr;
        
        if ( logger.isLoggable(Level.FINEST) ) {
            bbin = new ByteBufferInputStream(xml2);
            isr = new InputStreamReader(bbin,"UTF-8");

            System.err.println("the encoding is "+isr.getEncoding());
            int c= isr.read();
            int count= 0;
            while ( c!=-1 && count<1000 ) {
                System.err.println( String.format( "%05d %04d %X %c", count, c, c, c ) );
                count++;
                c= isr.read();
            }
        }
        
        bbin = new ByteBufferInputStream(xml);
        isr = new InputStreamReader(bbin,"UTF-8");
        
        try {
            DocumentBuilder builder;
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource source = new InputSource(isr);
            logger.fine("setting UTF-8 to input source");
            source.setEncoding("UTF-8");
            Document document = builder.parse(source);
            return document;
        } catch (ParserConfigurationException ex) {
            throw new RuntimeException(ex);
        }

    }

    private static int getContentLength(ByteBuffer buffer) throws StreamException {
        int contentLength = 0;
        for (int i = 0; i < 6; i++) {
            char c = (char) (0xFF & buffer.get());
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

    public static void formatHeader(Document document, Writer writer) throws StreamException {
		DOMImplementationLS ls = (DOMImplementationLS)
				document.getImplementation().getFeature("LS", "3.0");
		LSOutput output = ls.createLSOutput();
      
		output.setCharacterStream(writer);
		LSSerializer serializer = ls.createLSSerializer();
      try {
        if ( serializer.getDomConfig().canSetParameter( "format-pretty-print", Boolean.TRUE ) ) {
            serializer.getDomConfig().setParameter( "format-pretty-print", Boolean.TRUE );
          }
      } catch ( Error e ) {
          e.printStackTrace();
      }
		serializer.write(document, output);
		/*
        try {
            OutputFormat format = new OutputFormat(Method.XML, "US-ASCII", true);
            XMLSerializer serializer = new XMLSerializer(writer, format);
            serializer.serialize(document);
        } catch (IOException ioe) {
            throw new StreamException(ioe);
        }
		 */
    }

    public static Map<String,Object> processPropertiesElement(Element element) throws StreamException {
        try {
            if (!element.getTagName().equals("properties")) {
                // TODO maybe this should be a RuntimeException
                throw new StreamException("expecting 'properties' element, encountered '" + element.getTagName() + "'");
            }
            HashMap<String, Object> map = new HashMap();
            NamedNodeMap attributes = element.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                Attr attr = (Attr) attributes.item(i);
                String name = attr.getName();
                String[] split = name.split(":");
                if (split.length == 1) {
                    map.put(name, attr.getValue());
                } else if (split.length == 2) {
                    PropertyType type = PropertyType.getByName(split[0]);
                    Object value = type.parse(attr.getValue());
                    map.put(split[1], value);
                } else {
                    throw new IllegalArgumentException("Invalid typed name: " + name);
                }
            }
            return map;
        } catch (ParseException pe) {
            String msg= pe.getMessage();
            if (msg==null ) msg=pe.toString();
            StreamException se = new StreamException(msg);
            se.initCause(pe);
            throw se;
        }
    }
    private final static HashMap typesMap;

    static {
        typesMap = new HashMap();
        typesMap.put(Datum.class, "Datum");
        typesMap.put(Datum.Double.class, "Datum");
        typesMap.put(Integer.class, "int");
    }

    public static Element processPropertiesMap(Document document, Map properties) {
        Element propertiesElement = document.createElement("properties");
        for (Iterator i = properties.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            String key = (String) entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (typesMap.containsKey(value.getClass())) {
                key = (String) typesMap.get(value.getClass()) + ":" + key;
            }
            propertiesElement.setAttribute(key, value.toString());
        }
        return propertiesElement;
    }

    private static ReadableByteChannel getInflaterChannel(ReadableByteChannel channel) throws IOException {
        return new InflaterChannel(channel);
    //return Channels.newChannel(new InflaterInputStream(Channels.newInputStream(channel)));
    }
}
