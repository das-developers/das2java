/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.qstream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.QDataSet;
import org.w3c.dom.Element;

/**
 *
 * @author jbf
 */
public class PacketDescriptor implements Descriptor {

    List<PlaneDescriptor> planes;
    /**
     * If true, then slices of the dataset fill each packet.
     */
    boolean stream;
    
    /**
     * number of dimensions being streamed.  Zero means all the data is 
     * found in the first packet (no streaming).  One means a qube is being
     * streamed.  Two means a this packet is one qube of a higher rank dataset.
     */
    int streamRank;

    PacketDescriptor() {
        planes = new ArrayList<PlaneDescriptor>();
    }
    /**
     * this is used when reading streams.
     * @param element
     */
    PacketDescriptor( Element element ) {
        this();
        this.domElement= element;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream ) {
        this.stream = stream;
    }

    public int streamRank() {
        return streamRank;
    }
    
    public void setStreamRank( int streamRank ) {
        this.streamRank= streamRank;
    }
    
    public void addPlane( PlaneDescriptor planeDescriptor ) {
        planes.add(planeDescriptor);
    }
    
    private Element domElement;

    /**
     * return the list of planes in an unmodifiable list.
     * @return
     */
    public List<PlaneDescriptor> getPlanes() {
        return Collections.unmodifiableList(planes);
    }

    boolean valuesInDescriptor= false;
    
    void setValuesInDescriptor(boolean b) {
        valuesInDescriptor= b;
    }

    boolean isValuesInDescriptor() {
        return valuesInDescriptor;
    }


    void setDomElement(Element packetElement) {
        domElement = packetElement;
    }

    public Element getDomElement() {
        return domElement;
    }
    
    /**
     * calculate the number of bytes in each packet.
     * @return
     */
    int sizeBytes() {
        int sizeBytes = 0;
        for (int iplane = 0; iplane < planes.size(); iplane++) {
            PlaneDescriptor pd = planes.get(iplane);
            sizeBytes+= pd.sizeBytes();
        }
        return sizeBytes;
    }

    public String toString() {
        return planes.toString();
    }
}
