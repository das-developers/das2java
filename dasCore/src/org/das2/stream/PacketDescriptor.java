/* File: PacketDescriptor.java
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
import java.nio.ByteBuffer;
import java.util.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.das2.datum.LoggerManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Represents the global properties of the stream, that are accessible to
 * datasets within.
 * @author jbf
 */
public class PacketDescriptor implements Cloneable {
    
    private static Logger logger = LoggerManager.getLogger("das2.d2s");
    
    private StreamXDescriptor xDescriptor;
    private SkeletonDescriptor[] yDescriptors = new SkeletonDescriptor[6];
    private int yCount = 0;    
    private Map properties;
    private int id= -99;
    
    /** 
     * creates a new PacketDescriptor
     * @param element
     * @throws org.das2.stream.StreamException
     */
    public PacketDescriptor( Element element ) throws StreamException {
        this( -99, element );
    }
    
    /** 
     * creates a new PacketDescriptor
     * @param id
     * @param element
     * @throws org.das2.stream.StreamException
     */
    public PacketDescriptor( int id, Element element ) throws StreamException {
        properties= new HashMap();        
        this.id= id;
        if (element.getTagName().equals("packet")) {
            processElement(element);
        } else {
            processLegacyElement(element);
        }
    }   

    /**
     * return the ID associated with this packet type, or -99 if no id
     * has been assigned.
     * @return the id or -99
     */
    public int getId() {
        return id;
    }
    
    private void processElement(Element element) throws StreamException {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if ( node instanceof Element ) {
                Element child = (Element)node;
                String name = child.getTagName();
                logger.log(Level.FINE, "process element type {0}", name);
                if ( name.equals("x") ) {
                    xDescriptor = new StreamXDescriptor(child);
                } else if ( name.equals("y") ) {
                    StreamScalarDescriptor d = new StreamYDescriptor(child);
                    addYDescriptor(d);
                } else if ( name.equals("z") ) {
                    StreamScalarDescriptor d = new StreamZDescriptor(child);
                    addYDescriptor(d);
                } else if ( name.equals("yscan") ) {
                    StreamYScanDescriptor d = new StreamYScanDescriptor(child);
                    addYDescriptor(d);
                } else if ( name.equals("properties") ) {
                    try {
                        NodeList list = element.getElementsByTagName("properties");
                        if (list.getLength() != 0) {
                            Element propertiesElement = (Element)list.item(0);
                            Map m = StreamTool.processPropertiesElement(propertiesElement);
                            properties.putAll(m);
                        }
                    } catch ( StreamException e ) {
                        throw new RuntimeException(e);
                    }
                } else if ( name.equals("qdataset") ) {
                    throw new StreamException("das2stream reader found that content appears to be qstream.");
                }
            }
        }
    }
    
    private void processLegacyElement(Element element) throws StreamException {
        NodeList children= element.getChildNodes();
        for (int i=0; i<children.getLength(); i++) {
            Node node= children.item(i);
            if ( node instanceof Element ) {
                Element child = (Element)node;
                String name= child.getTagName();
                if ( name.equals("X")) {
                    xDescriptor = new StreamXDescriptor(child);
                } else if ( name.equals("YScan")) {
                    StreamYScanDescriptor d= new StreamYScanDescriptor(child);
                    addYDescriptor(d);
                } else if ( name.equals("MultiY")) {
                    StreamScalarDescriptor d= new StreamScalarDescriptor(child);
                    addYDescriptor(d);
                }
            }
        }
    }
    
    public PacketDescriptor() {
    }
    
    public StreamXDescriptor getXDescriptor() {
        return xDescriptor;
    }
    
    public void setXDescriptor(StreamXDescriptor x) {
        xDescriptor = x;
    }
    
    public void addYDescriptor(SkeletonDescriptor y) {
        if (yCount == yDescriptors.length) {
            SkeletonDescriptor[] temp = new SkeletonDescriptor[yCount * 2];
            System.arraycopy(yDescriptors, 0, temp, 0, yCount);
            yDescriptors = temp;
        }
        yDescriptors[yCount] = y;
        yCount++;
        Map<String,String> names= new HashMap();
        for ( int i=0; i<yCount; i++ ) {
            Object pp= yDescriptors[i].getProperty("name");
            if ( pp==null && yDescriptors[i] instanceof StreamYScanDescriptor ) {
                pp= ((StreamYScanDescriptor)yDescriptors[i]).getName();
            }
            if ( pp==null && yDescriptors[i] instanceof StreamScalarDescriptor ) {
                pp= ((StreamScalarDescriptor)yDescriptors[i]).getName();
            }
            if ( names.containsKey(pp) ) {
                throw new IllegalArgumentException("Das2 Stream Format error: Required Attribute '"+"name"+
				                        "' missing in "+y+" plane.");
            }
            names.put( (String)yDescriptors[i].getProperty("name"),(String)yDescriptors[i].getProperty("name") );
        }
    }
    
    public int getYCount() {
        return yCount;
    }
    
    /** Returns a List of SkeletonDescriptor instances that represent the y
     * planes in a packet.  The List is unmodifiable and will throw an exception
     * if any attempt is made to alter the list.  The contents of the list will
     * not be updated if a yDescriptor is added to this packet descriptor.
     * @return a List of y planes
     */
    public List getYDescriptors() {
        return Collections.unmodifiableList(Arrays.asList(yDescriptors).subList(0, yCount));
    }
    
    public SkeletonDescriptor getYDescriptor(int index) {
        if (index < 0 || index >= yCount) {
            throw new IndexOutOfBoundsException("index = " + index + ", yCount = " + yCount);
        }
        return yDescriptors[index];
    }
    
    public int getSizeBytes() {
        int sizeBytes = xDescriptor.getSizeBytes();
        for (int i = 0; i < yCount; i++) {
            sizeBytes += yDescriptors[i].getSizeBytes();
        }
        return sizeBytes;
    }
    
    public DatumVector[] read(ByteBuffer input) {
        DatumVector[] vectors = new DatumVector[yCount + 1];
        vectors[0] = xDescriptor.read(input);
        for (int i = 0; i < yCount; i++) {
            vectors[i + 1] = yDescriptors[i].read(input);
        }
        return vectors;
    }
    
    public void write(Datum xTag, DatumVector[] vectors, ByteBuffer output) {
        xDescriptor.writeDatum(xTag, output);
        for (int i = 0; i < yCount; i++) {
            yDescriptors[i].write(vectors[i], output);
        }
        //ASCII KLUDGE!!!!
        if (yDescriptors[yCount - 1] instanceof StreamYScanDescriptor
                && ((StreamYScanDescriptor)yDescriptors[yCount - 1]).getDataTransferType().isAscii()
                && Character.isWhitespace((char)output.get(output.position() - 1))) {
            output.put(output.position() - 1, (byte)'\n');
        } else if (yDescriptors[yCount - 1] instanceof StreamScalarDescriptor
                && ((StreamScalarDescriptor)yDescriptors[yCount - 1]).getDataTransferType().isAscii()
                && Character.isWhitespace((char)output.get(output.position() - 1))) {
            output.put(output.position() - 1, (byte)'\n');
        }
    }
    
    private static String trimComment(String line) {
        int index = line.indexOf(';');
        if (index == 0) {
            return "";
        } else if (index != -1) {
            return line.substring(0, index);
        } else {
            return line;
        }
    }
    
    private static final Pattern LABEL_PATTERN = Pattern.compile("\\s*label\\((\\d+)\\)\\s*");
    
    public static PacketDescriptor createLegacyPacketDescriptor(Map dsdf) {
        PacketDescriptor packetDescriptor = new PacketDescriptor();
        packetDescriptor.setXDescriptor(new StreamXDescriptor());
        if (dsdf.get("form").equals("x_tagged_y_scan")) {
            StreamYScanDescriptor yscan = new StreamYScanDescriptor();
            yscan.setYCoordinates((double[])dsdf.get("y_coordinate"));
            packetDescriptor.addYDescriptor(yscan);
        } else if (dsdf.get("form").equals("x_multi_y") && dsdf.get("ny") != null) {
            StreamScalarDescriptor y = new StreamScalarDescriptor();
            packetDescriptor.addYDescriptor(y);
        } else if (dsdf.get("form").equals("x_multi_y") && dsdf.get("items") != null) {
            List planeList = (List)dsdf.get("plane-list");
            packetDescriptor.addYDescriptor(new StreamScalarDescriptor());
            for (int index = 0; index < planeList.size(); index++) {
                StreamScalarDescriptor y = new StreamScalarDescriptor();
                y.setName((String)planeList.get(index));
                packetDescriptor.addYDescriptor(y);
            }
        }
        return packetDescriptor;
    }
    
    private static String[] ensureCapacity(String[] array, int capacity) {
        if (array == null) {
            return new String[capacity];
        } else if (array.length >= capacity) {
            return array;
        } else {
            String[] temp = new String[capacity];
            System.arraycopy(array, 0, temp, 0, array.length);
            return temp;
        }
    }
    
    public Element getDOMElement(Document document) {
        Element element = document.createElement("packet");
        element.appendChild(xDescriptor.getDOMElement(document));
        for (int i = 0; i < yCount; i++) {
            element.appendChild(yDescriptors[i].getDOMElement(document));
        }
        return element;
    }
        
    public Object getProperty(String name) {
        return properties.get(name);
    }
    
    public Map getProperties() {
        return Collections.unmodifiableMap(properties);
    }
    
    public void setProperty(String name, Object value) {
        properties.put(name, value);
    }
    
    public Object clone() {
        try {
            PacketDescriptor clone = (PacketDescriptor)super.clone();
            clone.xDescriptor = (StreamXDescriptor)xDescriptor.clone();
            clone.yDescriptors = new SkeletonDescriptor[yCount];
            for (int i = 0; i < yCount; i++) {
                clone.yDescriptors[i] = (SkeletonDescriptor)yDescriptors[i].clone();
            }
            return clone;
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException(cnse);
        }
    }
    
    @Override
    public String toString() {
        return "PacketDescriptor "+this.yCount + " ydescriptors";
    }
    
}
