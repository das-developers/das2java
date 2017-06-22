/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.qstream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.List;
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

    public StreamDescriptor(DocumentBuilderFactory factory) {
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

    /**
     * add the descriptor to the stream, manually assigning it an id
     * @param pd
     */
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
        if ( found==-1 ) {
            throw new IllegalArgumentException("ran out of numbers, use retire to free");
        }
        addDescriptor( pd, found );
    }

    /**
     * If a second PacketDescriptor contains the same descriptor information, then the PacketDescriptor can be
     * dropped.  This was introduced when two daily streams appended did not create a valid stream.
     * 
     * It has the descriptor if:
     * * the number is the same
     * * the planes within are the same ids.
     * @param pd0
     * @param descriptorId
     * @return
     */
    public synchronized boolean hasDescriptor( Descriptor pd0, int descriptorId ) {
        if ( pd0 instanceof PacketDescriptor ) {
            PacketDescriptor ppd0= (PacketDescriptor)pd0;
            Descriptor o= descriptors.get(descriptorId);
            if ( o instanceof PacketDescriptor ) {
                PacketDescriptor ppd1= (PacketDescriptor)o;
                List<PlaneDescriptor> planed0= ppd0.getPlanes();
                List<PlaneDescriptor> planed1= ppd1.getPlanes();
                if ( planed0.size()==planed1.size() ) {
                    boolean same= true;
                    for ( int i=0; i<planed1.size(); i++ ) {
                        if ( !planed0.get(i).getName().equals(planed1.get(i).getName()) ) {
                            same= false;
                        }
                    }
                    if ( same ) return true;
                }
            }
        }
        return false;

    }

    /**
     * add the descriptor to the stream, with the given ID.
     * @param pd
     * @param descriptorId
     */
    public synchronized void addDescriptor( Descriptor pd, int descriptorId ) {
        descriptors.put(descriptorId, pd);
        invPackets.put(pd, descriptorId );
    }


    public synchronized int descriptorId(Descriptor pd) {
        if ( pd==this ) return 0;
        Integer i= invPackets.get(pd);
        if ( i==null ) {
            throw new IllegalArgumentException("no descriptor ID found for descriptor: "+pd) ;
        } else {
            return i;
        }
    }

    /**
     * indicate that no more packets will be sent with this descriptor.
     * This will free up the number so it can be reused.
     * @param pd the descriptor.
     */
    public synchronized void retireDescriptor(Descriptor pd) {
        int i= invPackets.get(pd);
        invPackets.remove(pd);
        descriptors.remove(i);
    }

    public void send(Descriptor pd, WritableByteChannel out) throws StreamException, IOException {
        Document document = documents.get(pd);

        Element ele= pd.getDomElement();
        if ( ele==null ) {
            throw new IllegalArgumentException("Descriptor contains no domElement, cannot be sent");
        }

        document.appendChild( document.importNode( ele, true ) );

        ByteArrayOutputStream pdout = new ByteArrayOutputStream(1000);
        Writer writer = new OutputStreamWriter(pdout);
        StreamTool.formatHeader(document, writer);

        String packetTag = String.format("[%02d]", descriptorId(pd));
        out.write(ByteBuffer.wrap(packetTag.getBytes()));
        
        if ( pdout.size()>999999 ) {
            throw new IllegalArgumentException("packet header is longer than can be formatted to a packet header (longer than 999999 bytes).");
        }
        
        out.write(ByteBuffer.wrap(String.format("%06d", pdout.size()).getBytes()));
        out.write(ByteBuffer.wrap(pdout.toByteArray()));

    }

    /**
     * get the XML document that will contain the descriptor.  Note that
     * a QStream will have many XML documents, one for each descriptor.  This
     * keeps track of the documents for each descriptor.
     * @param descriptor the descriptor
     * @return the Document, which will have elements added to it.
     * @throws ParserConfigurationException 
     */
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
    
    @Override
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
