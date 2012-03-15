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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Description of the Stream, and manages resources for the stream.
 * @author jbf
 */
public class StreamDescriptor implements Descriptor {

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

    public synchronized void addDescriptor(Descriptor pd) {
        int found= -1;
        if ( invPackets.get(pd)!=null ) {
            return; // it's already registered.
        }
        for ( int i=0; i<100; i++ ) {
            if ( descriptors.get(i)==null ) {
                found=i;
                break;
            } else {

            }
        }
        if ( found==-1 ) throw new IllegalArgumentException("ran out of numbers, use retire to free");
        addDescriptor( pd, found );
    }

    public synchronized void addDescriptor( Descriptor pd, int found ) {
        descriptors.put(found, pd);
        invPackets.put(pd, found );
    }


    public synchronized int descriptorId(Descriptor pd) {
        if ( pd==this ) return 0;
        return invPackets.get(pd);
    }

    /**
     * indicate that no more packets will be sent with this descriptor.
     * This will free up the number so it can be reused.
     * @param pd
     */
    public synchronized void retireDescriptor(Descriptor pd) {
        int i= invPackets.get(pd);
        invPackets.remove(pd);
        descriptors.remove(i);
    }

    public void send(Descriptor pd, WritableByteChannel out) throws StreamException, IOException {
        Document document = documents.get(pd);

        document.appendChild( document.importNode( pd.getDomElement(), true ) );

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
    
    protected ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;

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

    //added to support filters
    void setFactory(DocumentBuilderFactory newInstance) {
        this.factory= newInstance;
    }

}
