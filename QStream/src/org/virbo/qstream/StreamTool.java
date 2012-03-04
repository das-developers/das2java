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
package org.virbo.qstream;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import org.das2.util.InflaterChannel;
import org.das2.util.ByteBufferInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
/**
 *
 * @author  jbf
 */
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathExpressionException;
import org.das2.dataset.NoDataInIntervalException;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.virbo.dataset.SemanticOps;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class StreamTool {

    ByteOrder byteOrder;
    private static final int PACKET_LENGTH_LIMIT=100000;

    /** Creates a new instance of StreamTool */
    public StreamTool() {
    }

    public static class DelimeterNotFoundException extends Exception {
    }

    public static byte[] advanceTo(InputStream in, byte[] delim) throws IOException, DelimeterNotFoundException {

        // Read from stream to delimeter, leaving the InputStream immediately after
        // and returning bytes from stream.

        byte[] data = new byte[4096];

        ArrayList list = new ArrayList();

        int bytesMatched = 0;

        int index = 0;
        boolean notDone = true;

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

        //findbugs INT_BAD_COMPARISON_WITH_NONNEGATIVE_VALUE, see Das2Stream StreamTool.  This was probably leftover code.
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
        private ByteBuffer bigBuffer = ByteBuffer.allocate(PACKET_LENGTH_LIMIT);
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

        public String toString() {
            return "\ndescriptorCount=" + descriptorCount +
                    "\npacketCount=" + packetCount +
                    "\nbyteOffset=" + byteOffset +
                    "\ncarotPos=" + (byteOffset - bigBuffer.limit() + bigBuffer.position()) +
                    "\nbuffer=" + String.valueOf(bigBuffer);
        }
    }

    public static void readStream(ReadableByteChannel stream, StreamHandler handler) throws StreamException {

        ReadStreamStructure struct = new ReadStreamStructure(stream, handler);

        try {
            StreamDescriptor sd = getStreamDescriptor(struct);
            if ("deflate".equals(sd.getCompression())) {
                stream = getInflaterChannel(stream);
            }

            handler.streamDescriptor(sd);
            struct.descriptorCount++;
            int bytesRead;
            int totalBytesRead = 0;

            while ( (bytesRead = stream.read(struct.bigBuffer)) >= 0 || struct.bigBuffer.position()!=0 ) {
                struct.byteOffset += struct.bigBuffer.position();
                struct.bigBuffer.flip();
                
                totalBytesRead += bytesRead;
                while (getChunk(struct)) {
                    // this block is intentionally empty
                }
                struct.bigBuffer.compact();
            }
            handler.streamClosed(sd);
        } catch (StreamException se) {
            handler.streamException(se);
            throw se;
        } catch (IOException ioe) {
            StreamException se = new StreamException(ioe);
            handler.streamException(se);
            throw se;
        }
    }

    private static StreamDescriptor getStreamDescriptor(ReadStreamStructure struct) throws StreamException, IOException {

        struct.bigBuffer.clear().limit(10);
        while (struct.bigBuffer.hasRemaining() && struct.stream.read(struct.bigBuffer) != -1) {
            ;
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
            struct.byteOffset += struct.bigBuffer.position();
            struct.bigBuffer.clear().limit(contentLength);
            while (struct.bigBuffer.hasRemaining() && struct.stream.read(struct.bigBuffer) != -1) {
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
                    sd.setDomElement(doc.getDocumentElement());
                    sd.setSizeBytes(contentLength);
                    String b = root.getAttribute("byte_order");
                    if (b != null && !b.equals("")) {
                        sd.setByteOrder(b.equals("little_endian") ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
                        struct.bigBuffer.order(sd.getByteOrder());
                    }
                    struct.bigBuffer.clear();
                    return sd;
                } else if (root.getTagName().equals("exception")) {
                    throw exception(root);
                } else {
                    throw new StreamException("Unexpected xml header, expecting stream or exception, received: " + root.getTagName());
                }
            } catch (SAXException ex) {
                String msg = getSAXParseExceptionMessage(ex, struct, contentLength);
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
            ;
        }
        int p = struct.bigBuffer.position();
        byte[] bytes = new byte[p];
        struct.bigBuffer.flip();
        struct.bigBuffer.get(bytes);
        s = s + new String(bytes);
        return s;
    }

    private static String getSAXParseExceptionMessage(final SAXException ex, final ReadStreamStructure struct, int contentLength) {
        String loc = null;
        if (ex instanceof SAXParseException) {
            SAXParseException spe = (SAXParseException) ex;
            loc = "Relative to packet start, line number is " + spe.getLineNumber() + ", column is " + spe.getColumnNumber();
        }

        int bufOffset = struct.byteOffset - struct.bigBuffer.limit();

        String msg = "xml parser fails with the message: \"" + ex.getMessage() +
                "\" within the packet ending at byte offset " + (bufOffset + struct.bigBuffer.position()) + ".";
        if (ex.getMessage().contains("trailing")) {
            msg += "\nNon-whitespace data found after xml closing tag, probably caused by content length error.";
            int i;
            // find the end of the closing xml tag.
            for (i = bufOffset + struct.bigBuffer.position() - 1; i > 0; i--) {
                int bpos = i - bufOffset;
                if (struct.bigBuffer.get(bpos) == '>') {
                    break;
                } else {
                    //System.err.println((char) struct.bigBuffer.get(bpos));
                }
            }
            for (; i < bufOffset + struct.bigBuffer.position(); i++) {
                int bpos = i - bufOffset;
                if (struct.bigBuffer.get(bpos) == '[' || struct.bigBuffer.get(bpos) == ':') {
                    break;
                }
            }
            if (i > 0) {
                int error = i - (struct.bigBuffer.position() + bufOffset);
                //int error= ( i + bufOffset ) - ( struct.byteOffset - 10 );
                NumberFormat nf = new DecimalFormat("000000");
                msg += "\nContent length was " + nf.format(contentLength) + ", maybe it should have been " + nf.format(contentLength + error) + ".";
            }
        }

        if (loc != null) {
            msg += "  " + loc;
        }
        return msg;
    }

    private static StreamException exception(Element exception) {
        String type = exception.getAttribute("type");
        String message= exception.getAttribute("message");
        if ( type.equals( StreamException.NO_DATA_IN_INTERVAL ) ) {
            NoDataInIntervalException ex = new NoDataInIntervalException(message);
            StreamException se= new StreamException(ex);
            return se;
        } else {
            return new StreamException(message);
        }
    }

    /**
     * this is code buried down in QDataSetStreamHandler, which was doing a lot of the interpretation that could
     * be done without knowing about QDataSet.
     * @param pd
     * @throws XPathExpressionException
     */
    public static void interpretPlanes( PacketDescriptor pd ) throws StreamException {
        try {
            Element e = pd.getDomElement();

            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();

            XPathExpression expr = xpath.compile("/packet/qdataset");
            Object o = expr.evaluate(e, XPathConstants.NODESET);
            NodeList nodes = (NodeList) o;
            for (int i = 0; i < nodes.getLength(); i++) {
                Element n = (Element) nodes.item(i);
                String name = n.getAttribute("id");
                int rank = Integer.parseInt(n.getAttribute("rank"));

                String sdims=null;
                int[] dims= null;
                String ttype=null; // ttype or
                String joinChildren= null; //  join will be specified.
                //String joinParent= null;
                boolean isInline= false;
                //joinParent= n.getAttribute("joinId");

                NodeList values= (NodeList) xpath.evaluate("values", n, XPathConstants.NODESET );
                if ( values.getLength()==0 ) {
                    throw new IllegalArgumentException("no values node in "+n.getNodeName() + " " +n.getAttribute("id") );
                }

                for ( int iv= 0; iv<values.getLength(); iv++ ) {
                    Element vn= (Element)values.item(iv);

                    if ( vn.hasAttribute("values") ) {  // TODO: consider "inline"
                        isInline= true;
                    }
                    //index stuff--Ed W. thinks index should be implicit.
                    sdims = xpath.evaluate("@length", vn);
                    ttype = xpath.evaluate("@encoding", vn);
                    joinChildren = xpath.evaluate("@join", vn);

                    if (sdims == null) {
                        dims = new int[0];
                    } else {
                        dims = Util.decodeArray(sdims);
                    }

                }

                PlaneDescriptor planeDescriptor = new PlaneDescriptor();
                planeDescriptor.setRank(rank);
                planeDescriptor.setQube(dims); // zero length is okay
                boolean isStream = rank > dims.length;
                pd.setStream(isStream);
                pd.setStreamRank( rank - dims.length );

                // this is nasty.
                String sunits=null;
                NodeList odims= (NodeList) xpath.evaluate("properties[not(@index)]/property", n, XPathConstants.NODESET );
                for ( int ii=0; sunits==null && ii<odims.getLength(); ii++ ) {
                    Node nn= odims.item(ii);
                    if ( nn.getAttributes().getNamedItem("name").getNodeValue().equals("UNITS") ) {
                        sunits= nn.getAttributes().getNamedItem("value").getNodeValue();
                    }
                }
                //String sunits= xpath.evaluate("properties[not(@index)]/property/@name=UNITS", n, XPathConstants.NODE );
                Object units= sunits==null ? null : SemanticOps.lookupUnits(sunits);
                TransferType tt = TransferType.getForName( ttype, Collections.singletonMap( "UNITS", units ) );
                if ( tt==null && isInline ) {
                    tt= new AsciiTransferType( 10, true ); // kludge because we need something
                }
                if ( tt==null && joinChildren!=null && joinChildren.length()>0 ) {
                    tt= new AsciiTransferType( 10, true ); // kludge because we need something
                }
                if (tt == null ) {
                    throw new IllegalArgumentException("unrecognized transfer type: " + ttype);
                }
                planeDescriptor.setType(tt);
                planeDescriptor.setName(name);

                pd.addPlane(planeDescriptor);
            }
        } catch ( XPathExpressionException ex ) {
            throw new StreamException(ex);
        }
    }

    private static boolean getChunk(ReadStreamStructure struct) throws StreamException, IOException {
        struct.bigBuffer.mark();
        if (struct.bigBuffer.remaining() < 4) {
            return false;
        }
        struct.bigBuffer.get(struct.four);
        if (isPacketDescriptorHeader(struct.four)) {
            if (struct.bigBuffer.remaining() < 6) {
                struct.bigBuffer.reset();
                return false;
            }
            int contentLength = getContentLength(struct.bigBuffer);
            if (contentLength == 0) {
                throw new StreamException("packetDescriptor content length is 0.");
            }
            if (struct.bigBuffer.capacity() < contentLength) {
                struct.bigBuffer.reset();
                ByteBuffer temp = ByteBuffer.allocate(8 + contentLength + contentLength / 10);
                temp.put(struct.bigBuffer);
                temp.flip();
                struct.bigBuffer = temp;
                return false;
            } else if (struct.bigBuffer.remaining() < contentLength) {
                struct.bigBuffer.reset();
                return false;
            }

            try {
                Document doc = getXMLDocument(struct.bigBuffer, contentLength);
                Element root = doc.getDocumentElement();

                DescriptorFactory factory = DescriptorRegistry.get(root.getTagName());

                Element ele= doc.getDocumentElement();
                Descriptor pd = factory.create(ele);

                if (pd instanceof PacketDescriptor) {
                    struct.descriptors.put(asciiBytesToString(struct.four, 1, 2), pd);
                    StreamTool.interpretPlanes((PacketDescriptor)pd);
                    struct.handler.packetDescriptor((PacketDescriptor) pd);
                } else if (root.getTagName().equals("exception")) {
                    throw exception(root);
                } else if (pd instanceof StreamComment) {
                    //struct.handler.streamComment( (StreamComment)pd);
                } else {
                    throw new StreamException("Unexpected xml header, expecting stream or exception, received: " + root.getTagName());
                }
                struct.descriptorCount++;
            } catch (SAXException ex) {
                String msg = getSAXParseExceptionMessage(ex, struct, contentLength);
                throw new StreamException(msg);
            }

        } else if (isPacketHeader(struct.four)) {
            String key = asciiBytesToString(struct.four, 1, 2);
            PacketDescriptor pd = (PacketDescriptor) struct.descriptors.get(key);
            int contentLength = pd.sizeBytes();
            if ( contentLength>PACKET_LENGTH_LIMIT ) throw new IllegalStateException("packet length bug would cause stream parser to hang (bug 0000348: streams with long packet lengths)");
            if (struct.bigBuffer.remaining() < contentLength) {
                struct.bigBuffer.reset();
                return false;
            }

            int oldLimit = struct.bigBuffer.limit();
            struct.bigBuffer.limit(struct.bigBuffer.position() + contentLength);
            ByteBuffer packet = struct.bigBuffer.slice();
            packet.order(struct.bigBuffer.order());
            struct.bigBuffer.position(struct.bigBuffer.position() + contentLength);
            struct.bigBuffer.limit(oldLimit);
            struct.handler.packet(pd, packet);
            struct.packetCount++;

        } else {
            String msg = "Expected four byte header, found '";
            String s = new String(struct.four);
            s = s.replaceAll("\n", "\\\\n"); // TODO: what's the right wat to say this?
            msg += s;
            msg += "' at byteOffset=" + (struct.byteOffset - struct.bigBuffer.limit() + struct.bigBuffer.position() - 4 + 10);
            msg += " after reading " + struct.descriptorCount + " descriptors and " + struct.packetCount + " packets.";
            throw new StreamException(msg);
        }
        return true;
    }

    private static final String asciiBytesToString(byte[] bytes, int offset, int length) {
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
        xml.limit(xml.position() + contentLength);
        buffer.position(buffer.position() + contentLength);
        final boolean DEBUG = false;
        if (DEBUG) {
            int pos = xml.position();
            byte[] bytes = new byte[xml.limit() - xml.position()];
            xml.get(bytes);
            xml.position(pos);
            //System.err.println(new String(bytes));
        }
        ByteBufferInputStream bbin = new ByteBufferInputStream(xml);
        InputStreamReader isr = new InputStreamReader(bbin);

        try {
            DocumentBuilder builder;
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource source = new InputSource(isr);
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
        DOMImplementationLS ls = (DOMImplementationLS) document.getImplementation().getFeature("LS", "3.0");
        LSOutput output = ls.createLSOutput();

        output.setCharacterStream(writer);
        LSSerializer serializer = ls.createLSSerializer();
        try {
            if (serializer.getDomConfig().canSetParameter("format-pretty-print", Boolean.TRUE)) {
                serializer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
            }
        } catch (Error e) {
            // Ed's nice trick for finding the implementation
            String name = serializer.getClass().getSimpleName();
            java.net.URL u = serializer.getClass().getResource(name+".class");
            //System.err.println(u);
            e.printStackTrace();
        }
        serializer.write(document, output);

    }

    private static ReadableByteChannel getInflaterChannel(ReadableByteChannel channel) throws IOException {
        return new InflaterChannel(channel);
    //return Channels.newChannel(new InflaterInputStream(Channels.newInputStream(channel)));
    }
}
