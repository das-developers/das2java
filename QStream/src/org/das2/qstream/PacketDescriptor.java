/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.qstream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.w3c.dom.Element;

/**
 *
 * @author jbf
 */
public class PacketDescriptor implements Descriptor,Cloneable {

    PacketDescriptor() {
        planes = new ArrayList<PlaneDescriptor>();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        PacketDescriptor result= (PacketDescriptor) super.clone();
        result.planes= new ArrayList<PlaneDescriptor>();
        for ( int i=0; i<planes.size(); i++ ) {
            result.planes.add( (PlaneDescriptor)planes.get(i).clone() );
        }
        return result;
    }
    
    
    /**
     * this is used when reading streams.
     * @param element
     */
    PacketDescriptor( Element element ) {
        this();
        this.domElement= element;
    }

    boolean stream;

    /**
     * If true, then slices of the dataset fill each packet.
     */
    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream ) {
        this.stream = stream;
    }

    int streamRank;

    public int streamRank() {
        return streamRank;
    }
    
    /**
     * number of dimensions being streamed.  Zero means all the data is
     * found in the first packet (no streaming).  One means a qube is being
     * streamed.  Two means a this packet is one qube of a higher rank dataset.
     */
    public void setStreamRank( int streamRank ) {
        this.streamRank= streamRank;
    }

    List<PlaneDescriptor> planes;

    public void addPlane( PlaneDescriptor planeDescriptor ) {
        planes.add(planeDescriptor);
    }

    /**
     * return the list of planes in an unmodifiable list.
     * @return
     */
    public List<PlaneDescriptor> getPlanes() {
        return Collections.unmodifiableList(planes);
    }
    
    private Element domElement;

    boolean valuesInDescriptor= false;
    
    void setValuesInDescriptor(boolean b) {
        valuesInDescriptor= b;
    }

    boolean isValuesInDescriptor() {
        return valuesInDescriptor;
    }

    public void setDomElement(Element packetElement) {
        domElement = packetElement;
    }

    @Override
    public Element getDomElement() {
        return domElement;
    }
    
    /**
     * calculate the number of bytes in each packet.
     * @return
     */
    public int sizeBytes() {
        int sizeBytes = 0;
        for (int iplane = 0; iplane < planes.size(); iplane++) {
            PlaneDescriptor pd = planes.get(iplane);
            sizeBytes+= pd.sizeBytes();
        }
        return sizeBytes;
    }
  
    private int packetId=-1;
    
    /**
     * return the packet ID, which is a number from 1-99.
     * @return 
     */
    public int getPacketId() {
        if ( packetId<1 || packetId>99 ) {
            throw new IllegalStateException("packetId is invalid: "+packetId );
        }
        return packetId;
    }
    
    /**
     * keep track of the packet ID, which is a number from 1-99.
     * @param packetId 
     */
    public void setPacketId( int packetId ) {
        if ( packetId<1 || packetId>99 ) {
            throw new IllegalArgumentException("packetId is invalid: "+packetId );
        }
        this.packetId= packetId;
    }

    @Override
    public String toString() {
        return planes.toString();
    }
}
