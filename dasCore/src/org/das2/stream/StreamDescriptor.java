/* File: StreamDescriptor.java
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

import org.das2.DasIOException;
import org.das2.datum.DatumVector;
import org.das2.util.IDLParser;
import java.io.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

//import org.apache.xml.serialize.OutputFormat;
//import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXParseException;

/** Represents the global properties of the stream, that are accessible to
 * datasets within.
 * @author jbf
 */
public class StreamDescriptor implements SkeletonDescriptor, Cloneable {
    
    private Map properties = new HashMap();
    
    private StreamXDescriptor xDescriptor;
    private ArrayList yDescriptors = new ArrayList();
    private String compression;
    
    /** Creates a new instance of StreamProperties */
    public StreamDescriptor(Element element) throws StreamException {
        if (element.getTagName().equals("stream")) {
            processElement(element);
        }
        else {
            processLegacyElement(element);
        }
    }
    
    private void processElement(Element element) throws StreamException {
        compression = element.getAttribute("compression");
        NodeList list = element.getElementsByTagName("properties");
        if (list.getLength() != 0) {
            Element propertiesElement = (Element)list.item(0);
            Map m = StreamTool.processPropertiesElement(propertiesElement);
            properties.putAll(m);
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
                    yDescriptors.add(d);
                } else if ( name.equals("MultiY")) {
                    StreamScalarDescriptor d= new StreamScalarDescriptor(child);
                    yDescriptors.add(d);
                }
            }
        }
    }
    
    public StreamDescriptor() {
    }
    
    public StreamXDescriptor getXDescriptor() {
        return xDescriptor;
    }
    
    public void setXDescriptor(StreamXDescriptor x) {
        xDescriptor = x;
    }
    
    public void addYScan(StreamYScanDescriptor y) {
        yDescriptors.add(y);
    }
    
    public void addYMulti(StreamScalarDescriptor y) {
        yDescriptors.add(y);
    }
    
    public List getYDescriptors() {
        return Collections.unmodifiableList(yDescriptors);
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

    public static Document parseHeader(Reader header) throws DasIOException, DasStreamFormatException {
        try {
            /*
            header = new FilterReader(header) {
                public int read() throws IOException {
                    int result = super.read();
                    if (result != -1) {
                        System.out.print((char)result);
                    }
                    return result;
                }
                public int read(char[] c, int offset, int length) throws IOException {
                    int result = super.read(c, offset, length);
                    if (result != -1) {
                        System.out.print(new String(c, offset, result));
                    }
                    return result;
                }
            };
             */
            DocumentBuilder builder= DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource source = new InputSource(header);
            Document document= builder.parse(source);
            return document;
        }
        catch ( ParserConfigurationException ex ) {
            throw new IllegalStateException(ex.getMessage());
        }
        catch ( SAXException ex ) {
            String msg;
            if ( ex instanceof SAXParseException ) {
                SAXParseException spe= (SAXParseException) ex;
                msg= spe.getMessage() + "at line="+spe.getLineNumber()+" col="+spe.getColumnNumber();
            } else {
                msg= ex.getMessage();
            }
            DasIOException thr= new DasIOException(msg);
            throw thr;
        }
        catch ( IOException ex) {
            throw new DasIOException(ex.getMessage());
        }
    }
    
    public int getSizeBytes() {
        return -1;
    }
    
    public DatumVector read(ByteBuffer input) {
        return null;
    }
    
    public void write(DatumVector input, ByteBuffer output) {
    }

    public static StreamDescriptor createLegacyDescriptor(BufferedReader in) throws IOException {
        IDLParser parser = new IDLParser();
        double[] array;
        String key;
        String value;
        int index, lineNumber;
        lineNumber = 1;
        Pattern labelPattern = Pattern.compile("\\s*label\\((\\d+)\\)\\s*");
        Matcher matcher;
        StreamDescriptor result = new StreamDescriptor();
        
        result.properties.put("legacy", "true");
        
        for (String line = in.readLine(); line != null; line = in.readLine()) {
            //Get rid of any comments
            index = line.trim().indexOf(';');
            if (index == 0) {
                lineNumber++;
                continue;
            }
            else if (index != -1) {
                line = line.substring(0, index);
            }
            
            //Break line into key-value pairs
            index = line.indexOf('=');
            key = line.substring(0,index).trim();
            value = line.substring(index+1).trim();
            
            //deterimine type of value
            
            if (key.equals("description")) {
                String description = value.substring(1, value.length()-1);
                result.properties.put(key, description);
            }
            else if (key.equals("groupAccess")) {
                result.properties.put(key, value.substring(1, value.length()-1));
            }
            else if (key.equals("form")) {
                result.properties.put(key, value);
            }
            else if (key.equals("reader")) {
                String reader = value.substring(1, value.length()-1);
                result.properties.put(key, reader);
            }
            else  if (key.equals("x_parameter")) {
                String x_parameter = value.substring(1, value.length()-1);
                result.properties.put(key, x_parameter);
            }
            else if (key.equals("x_unit")) {
                String x_unit = value.substring(1, value.length()-1);
                result.properties.put(key, x_unit);
            }
            else if (key.equals("y_parameter")) {
                String y_parameter = value.substring(1, value.length()-1);
                result.properties.put(key, y_parameter);
            }
            else if (key.equals("y_unit")) {
                String y_unit = value.substring(1, value.length()-1);
                result.properties.put(key, y_unit);
            }
            else if (key.equals("z_parameter")) {
                String z_parameter = value.substring(1, value.length()-1);
                result.properties.put(key, z_parameter);
            }
            else if (key.equals("z_unit")) {
                String z_unit = value.substring(1, value.length()-1);
                result.properties.put(key, z_unit);
            }
            else if (key.equals("x_sample_width")) {
                double x_sample_width = parser.parseIDLScalar(value);
                if ( Double.isNaN(x_sample_width) )
                    throw new IOException("Could not parse \"" + value + "\" at line " + lineNumber);
                result.properties.put(key, new Double(x_sample_width));
            }
            else if (key.equals("y_fill")) {
                double y_fill = parser.parseIDLScalar(value);
                if (Double.isNaN( Double.NaN ))
                    throw new IOException("Could not parse \"" + value + "\" at line " + lineNumber);
                result.properties.put(key, new Double(y_fill));
            }
            else if (key.equals("z_fill")) {
                double z_fill = (float)parser.parseIDLScalar(value);
                if (Double.isNaN(Float.NaN))
                    throw new IOException("Could not parse \"" + value + "\" at line " + lineNumber);
                result.properties.put(key, new Float(z_fill));
            }
            else if (key.equals("y_coordinate")) {
                array = parser.parseIDLArray(value);
                if (array == null) {
                    throw new IOException("Could not parse \"" + value + "\" at line " + lineNumber);
                }
                result.properties.put(key, array);
            }
            else if (key.equals("ny")) {
                int ny;
                try {
                    ny = Integer.parseInt(value);
                }
                catch (NumberFormatException nfe) {
                    throw new IOException("Could not parse \"" + value + "\" at line " + lineNumber);
                }
                result.properties.put(key, Integer.valueOf(ny));
            }
            else if (key.equals("items")) {
                int items;
                try {
                    items = Integer.parseInt(value);
                }
                catch (NumberFormatException nfe) {
                    throw new IOException("Could not parse \"" + value + "\" at line " + lineNumber);
                }
                result.properties.put(key, Integer.valueOf(items));
            }
            else if ((matcher = labelPattern.matcher(key)).matches()) {
                int i = Integer.parseInt(matcher.group(1));
                value = value.substring(1, value.length() - 1);
                if (i == 0) {
                    result.properties.put("label", value);
                }
                else {
                    String[] labels = ensureCapacity((String[])result.properties.get("plane-list"), i);
                    labels[i - 1] = value;
                    result.properties.put("plane-list", labels);
                    result.properties.put(value + ".label", value);
                }
            }
            else if (value.charAt(0)=='\'' && value.charAt(value.length()-1)=='\'') {
                result.properties.put(key, value.substring(1, value.length()-1));
            }
            else if (value.charAt(0)=='"' && value.charAt(value.length()-1)=='"') {
                result.properties.put(key, value.substring(1, value.length()-1));
            }
            else {
                result.properties.put(key, value);
            }
            lineNumber++;
        }
        String[] planeList = (String[])result.properties.get("plane-list");
        if (planeList != null) {
            result.properties.put("plane-list", Collections.unmodifiableList(Arrays.asList(planeList)));
        }
        result.properties.put("legacy", "true");
        return result;
    }
    
    public static String createHeader(Document document) throws DasIOException {
        StringWriter writer= new StringWriter();

		DOMImplementationLS ls = (DOMImplementationLS)
				document.getImplementation().getFeature("LS", "3.0");
		LSOutput output = ls.createLSOutput();
		output.setCharacterStream(writer);
		LSSerializer serializer = ls.createLSSerializer();
		serializer.write(document, output);

		/*
        OutputFormat format= new OutputFormat();
        format.setOmitXMLDeclaration(true);
        format.setEncoding("UTF-8");
        XMLSerializer serializer= new XMLSerializer(writer, format);
        try {
            serializer.serialize(document);
        } catch ( IOException ex) {
            throw new DasIOException(ex.getMessage());
        }
		 */

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

    /** Getter for property compression.
     * @return Value of property compression.
     *
     */
    public String getCompression() {
        return compression;
    }
    
    /** Setter for property compression.
     * @param compression New value of property compression.
     *
     */
    public void setCompression(String compression) {
        this.compression = compression;
    }
    
    public Element getDOMElement(Document document) {
        Element element = document.createElement("stream");
        if (compression != null && !compression.equals("")) {
            element.setAttribute("compression", compression);
        }
        if (!properties.isEmpty()) {            
            Element propertiesElement = StreamTool.processPropertiesMap( document, properties );
            element.appendChild(propertiesElement);
        }
        return element;
    }
    
    public Object clone() {
        try {
            StreamDescriptor clone = (StreamDescriptor)super.clone();
            clone.properties = new HashMap(this.properties);
            return clone;
        }
        catch (CloneNotSupportedException cnse) {
            throw new RuntimeException(cnse);
        }
    }
    
    @Override
    public String toString() {
        return "StreamDescriptor " + this.properties.size() + " properties";
    }
}
