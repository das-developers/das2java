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

package edu.uiowa.physics.pw.das.stream;

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.util.*;
import edu.uiowa.physics.pw.das.client.*;
import org.apache.xml.serialize.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/** Represents the global properties of the stream, that are accessible to
 * datasets within.
 * @author jbf
 */
public class PacketDescriptor implements SkeletonDescriptor {
    
    private StreamXDescriptor xDescriptor;
    private SkeletonDescriptor[] yDescriptors = new SkeletonDescriptor[6];
    private int yCount = 0;
    
    /** Creates a new instance of StreamProperties */
    public PacketDescriptor(Element element) {
        if (element.getTagName().equals("packet")) {
            processElement(element);
        }
        else {
            processLegacyElement(element);
        }
    }
    
    private void processElement(Element element) {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if ( node instanceof Element ) {
                Element child = (Element)node;
                String name = child.getTagName();
                if ( name.equals("x") ) {
                    xDescriptor = new StreamXDescriptor(child);
                }
                else if ( name.equals("y") ) {
                    StreamMultiYDescriptor d = new StreamMultiYDescriptor(child);
                    addYDescriptor(d);
                }
                else if ( name.equals("yscan") ) {
                    StreamYScanDescriptor d = new StreamYScanDescriptor(child);
                    addYDescriptor(d);
                }
            }
        }
    }
    
    private void processLegacyElement(Element element) {
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
                    StreamMultiYDescriptor d= new StreamMultiYDescriptor(child);
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
    }
    
    public int getYCount() {
        return yCount;
    }
    
    /** Returns a List of SkeletonDescriptor instances that represent the y
     * planes in a packet.  The List is unmodifiable and will throw an acception
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

    public static Document parseHeader(String header) throws DasIOException, DasStreamFormatException {
        try {
            DocumentBuilder builder= DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource source = new InputSource(new StringReader(header));
            Document document= builder.parse(source);
            return document;
        }
        catch ( ParserConfigurationException ex ) {
            throw new IllegalStateException(ex.getMessage());
        }
        catch ( SAXException ex ) {
            throw new DasIOException(ex.getMessage());
        }
        catch ( IOException ex) {
            throw new DasIOException(ex.getMessage());
        }
    }
    
    public int getSizeBytes() {
        int sizeBytes = xDescriptor.getSizeBytes();
        for (int i = 0; i < yCount; i++) {
            sizeBytes += yDescriptors[i].getSizeBytes();
        }
        return sizeBytes;
    }
    
    public void read(java.nio.ByteBuffer input, double[] output, int offset) {
    }
    
    public void write(double[] output, int offset, java.nio.ByteBuffer input) {
    }
    
    private static String trimComment(String line) {
        int index = line.indexOf(';');
        if (index == 0) {
            return "";
        }
        else if (index != -1) {
            return line.substring(0, index);
        }
        else {
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
        }
        else if (dsdf.get("form").equals("x_multi_y") && dsdf.get("ny") != null) {
            StreamMultiYDescriptor y = new StreamMultiYDescriptor();
            packetDescriptor.addYDescriptor(y);
        }
        else if (dsdf.get("form").equals("x_multi_y") && dsdf.get("items") != null) {
            List planeList = (List)dsdf.get("plane-list");
            packetDescriptor.addYDescriptor(new StreamMultiYDescriptor());
            for (int index = 0; index < planeList.size(); index++) {
                StreamMultiYDescriptor y = new StreamMultiYDescriptor();
                y.setName((String)planeList.get(index));
                packetDescriptor.addYDescriptor(y);
            }
        }
        return packetDescriptor;
    }
    
    public static String createHeader(Document document) throws DasIOException {
        StringWriter writer= new StringWriter();
        OutputFormat format= new OutputFormat();
        format.setOmitXMLDeclaration(true);
        format.setEncoding("UTF-8");
        XMLSerializer serializer= new XMLSerializer(writer, format);
        try {
            serializer.serialize(document);
        } catch ( IOException ex) {
            throw new DasIOException(ex.getMessage());
        }
        String result= writer.toString();
        return result;
    }
    
    /*
    // read off the bytes that are the xml header of the stream.  
    protected static byte[] readHeader( InputStream in ) throws IOException {              
        byte[] buffer= new byte[10000];
        int b;
        b= in.read();
        if ( b==(int)'[' ) {  // [00]
            for ( int i=0; i<3; i++ ) b= in.read(); 
        }        
        return StreamTool.readXML( in );
    }
     */
    
    private static String[] ensureCapacity(String[] array, int capacity) {
        if (array == null) {
            return new String[capacity];
        }
        else if (array.length >= capacity) {
            return array;
        }
        else {
            String[] temp = new String[capacity];
            System.arraycopy(array, 0, temp, 0, array.length);
            return temp;
        }
    }

}
