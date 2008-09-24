/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.qstream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.das2.stream.StreamException;
import org.das2.util.StreamTool;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Description of the Stream, and manages resources for the stream.
 * @author jbf
 */
class StreamDescriptor implements Descriptor {

    Map<Integer, Descriptor> descriptors;
    Map<Descriptor, Integer> invPackets;
    Map<Descriptor, Document> documents;
    DocumentBuilderFactory factory;
    private Element element;

    StreamDescriptor(DocumentBuilderFactory factory) {
        this();
        this.factory = factory;
    }

    StreamDescriptor(Element element) {
        this();
    }

    private StreamDescriptor() {
        descriptors = new HashMap<Integer, Descriptor>();
        invPackets = new HashMap<Descriptor, Integer>();
        documents = new HashMap<Descriptor, Document>();
    }

    public void addDescriptor(Descriptor pd) {
        int count = descriptors.size() + 1;
        descriptors.put(count, pd);
        invPackets.put(pd, count);
    }

    public int descriptorId(Descriptor pd) {
        if ( pd==this ) return 0;
        return invPackets.get(pd);
    }

    public void send(Descriptor pd, WritableByteChannel out) throws StreamException, IOException {
        Document document = documents.get(pd);

        document.appendChild(pd.getDomElement());

        ByteArrayOutputStream pdout = new ByteArrayOutputStream(1000);
        Writer writer = new OutputStreamWriter(pdout);
        StreamTool.formatHeader(document, writer);

        String packetTag = String.format("[%02d]", descriptorId(pd));
        out.write(ByteBuffer.wrap(packetTag.getBytes()));
        out.write(ByteBuffer.wrap(String.format("%06d", pdout.size()).getBytes()));
        out.write(ByteBuffer.wrap(pdout.toByteArray()));

    }

    public Document newDocument(Descriptor descriptor) throws ParserConfigurationException {
        Document document = factory.newDocumentBuilder().newDocument();
        documents.put(descriptor, document);
        return document;
    }

    /**
     * return the compression used on the stream.
     * @return
     */
    String getCompression() {
        return "none";
    }

    public void setDomElement( Element element ) {
        this.element= element;
    }
    
    public Element getDomElement() {
        return element;
    }
    
    int sizeBytes;
    
    int sizeBytes() {
        return sizeBytes;
    }
    
    void setSizeBytes( int size ) {
        this.sizeBytes= size;
    }
    
    protected ByteOrder byteOrder = ByteOrder.nativeOrder();

    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    public void setByteOrder(ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
    }

    /**
     * stream uses just ascii format types. 
     */
    protected boolean asciiTypes = false;

    public boolean isAsciiTypes() {
        return asciiTypes;
    }

    public void setAsciiTypes(boolean asciiTypes) {
        this.asciiTypes = asciiTypes;
    }

}
