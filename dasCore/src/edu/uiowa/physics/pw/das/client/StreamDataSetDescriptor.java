/* File: DataSetDescriptor.java
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
package edu.uiowa.physics.pw.das.client;

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.client.*;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.DatumVector;
import edu.uiowa.physics.pw.das.util.*;
import edu.uiowa.physics.pw.das.stream.*;
import edu.uiowa.physics.pw.das.util.ByteBufferInputStream;
import edu.uiowa.physics.pw.das.util.StreamTool;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.w3c.dom.*;
import org.w3c.dom.Document;

public class StreamDataSetDescriptor extends DataSetDescriptor {
    
    protected StandardDataStreamSource standardDataStreamSource;
    private boolean serverSideReduction = true;
    private PacketDescriptor defaultPacketDescriptor;
    
    
    public Units getXUnits() {
        return defaultPacketDescriptor.getXDescriptor().getUnits();
    }
    
    /**
     * Creates a new instance of <code>StreamDataSetDescriptor</code>
     * from the specified file
     */
    protected StreamDataSetDescriptor(Map properties) {
        setProperties(properties);
    }
    
    protected StreamDataSetDescriptor(Map properties, boolean legacy) {
        setProperties(properties, legacy);
    }
    
    public StreamDataSetDescriptor(StreamDescriptor sd, StandardDataStreamSource sdss) {
        this(sd.getProperties(), "true".equals(sd.getProperty("legacy")));
        this.standardDataStreamSource = sdss;
    }
    
    public void setStandardDataStreamSource(StandardDataStreamSource sdss) {
        this.standardDataStreamSource= sdss;
    }
    
    public StandardDataStreamSource getStandardDataStreamSource() {
        return this.standardDataStreamSource;
    }
    
    protected void setProperties(Map properties, boolean legacy) {
        super.setProperties(properties);
        if (legacy) {
            defaultPacketDescriptor = PacketDescriptor.createLegacyPacketDescriptor(properties);
        }
    }
    
    protected void setProperties( Map properties ) {
        setProperties(properties, false);
    }
    
    /**
     * Reads data for the given start and end dates and returns an array of floats
     *
     * @author eew
     * @param start A Datum object representing the start time for the interval requested
     * @param end A Datum object representing the end time for the interval requested
     * @return array of floats containing the data returned by the reader
     * @throws java.io.IOException If there is an error getting data from the reader, and IOException is thrown
     */
    protected float[] readFloats(InputStream in) throws DasException {
        float[] f;
        byte[] data = readBytes(in);
        f = new float[data.length/4];
        ByteBuffer buff = ByteBuffer.wrap(data);
        FloatBuffer fbuff = buff.asFloatBuffer();
        fbuff.get(f);
        return f;
    }
    
    /**
     * Reads data for the given start and end dates and returns an array of doubles
     *
     * @author eew
     * @param start A Datum object representing the start time for the interval requested
     * @param end A Datum object representing the end time for the interval requested
     * @return array of doubles containing the data returned by the reader
     * @throws java.io.IOException If there is an error getting data from the reader, and IOException is thrown
     */
    protected double[] readDoubles(InputStream in) throws DasException {
        double[] d;
        byte[] data = readBytes(in);
        d = new double[data.length/4];
        ByteBuffer buff = ByteBuffer.wrap(data);
        FloatBuffer fbuff = buff.asFloatBuffer();
        for (int i = 0; i < d.length; i++) {
            d[i] = fbuff.get();
        }
        return d;
    }
    
    private ByteBuffer getByteBuffer(InputStream in) throws DasException {
        byte[] data = readBytes(in);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        return buffer;
    }
    
    /**
     * Auxiliary method used by readDoubles(InputStream, Object, Datum, Datum);
     *
     *
     * Read data for the given start and end dates and returns an array of bytes
     *
     * @author eew
     * @param start A Datum object representing the start time for the interval requested
     * @param end A Datum object representing the end time for the interval requested
     * @throws java.io.IOException If there is an error getting data from the reader, and IOException is thrown
     */
    protected byte[] readBytes(InputStream in) throws DasException {
        LinkedList list = new LinkedList();
        byte[] data = new byte[4096];
        int bytesRead=0;
        int totalBytesRead=0;
        int lastBytesRead = -1;
        int offset=0;
        long time = System.currentTimeMillis();
        
        try {
            bytesRead= in.read(data,offset,4096-offset);
            while (bytesRead != -1) {
                int bytesSoFar = totalBytesRead;
                offset+=bytesRead;
                lastBytesRead= offset;
                if (offset==4096) {
                    list.addLast(data);
                    data = new byte[4096];
                    offset=0;
                }
                totalBytesRead+= bytesRead;
                bytesRead= in.read(data,offset,4096-offset);
            }
        } catch ( IOException e ) {
            throw new DasIOException(e);
        }
        
        if (lastBytesRead>=0 && lastBytesRead<4096) {
            list.addLast(data);
        }
        
        if (list.size()== 0) {
            throw new DasIOException("Error reading data: no data available");
        }
        int dataLength = (list.size()-1)*4096 + lastBytesRead;
        data = new byte[dataLength];
        Iterator iterator = list.iterator();
        for (int i = 0; i < list.size()-1; i++) {
            System.arraycopy(list.get(i), 0, data, i*4096, 4096);
        }
        System.arraycopy(list.get(list.size()-1), 0, data, (list.size() - 1)*4096, lastBytesRead);
        return data;
    }
    
    public String toString() {
        return getDataSetID();
    }
    
    protected DataSet getDataSetImpl( Datum start, Datum end, Datum resolution, DasProgressMonitor monitor ) throws DasException {
        if ( resolution != null && !resolution.isFinite() ) throw new IllegalArgumentException( "resolution is not finite" );
        InputStream in;
        DataSet result;
        if ( isServerSideReduction() ) {
            in= standardDataStreamSource.getReducedInputStream( this, start, end, resolution);
        } else {
            in= standardDataStreamSource.getInputStream( this, start, end );
        }
        
        result = getDataSetFromStream( in, start, end, monitor );
        return result;
    }
    
    protected DataSet getDataSetFromStream(InputStream in, Datum start, Datum end, DasProgressMonitor monitor ) throws DasException {
        PushbackInputStream pin = new PushbackInputStream(in, 4096);
        try {
            byte[] four = new byte[4];
            int bytesRead= pin.read(four);
            if ( bytesRead!=4 ) {
                throw new DasIOException( "No data returned from server" );
            }
            if (new String(four).equals("[00]")) {
                pin.unread(four);
                
                if (monitor != null) monitor.started();
                InputStream mpin = new DasProgressMonitorInputStream(pin, monitor);
                
                ReadableByteChannel channel = Channels.newChannel(mpin);
                
                DataSetStreamHandler handler = new DataSetStreamHandler( properties, monitor, start, end );
                StreamTool.readStream(channel, handler);
                return handler.getDataSet();
            }
            else {
                pin.unread(four);
                
                if (monitor != null) monitor.started();
                
                InputStream mpin = new DasProgressMonitorInputStream(pin, monitor);
                
                if (getProperty("form").equals("x_tagged_y_scan")) {
                    return getLegacyTableDataSet(mpin, start);
                }
                else if (getProperty("form").equals("x_multi_y")) {
                    return getLegacyVectorDataSet(mpin, start);
                }
                else {
                    throw new IllegalStateException("Unrecognized data set type: " + getProperty("form"));
                }
            }
        }
        catch (UnsupportedEncodingException uee) {
            //UTF-8 should be supported by all JVM's
            throw new RuntimeException(uee);
        }
        catch (IOException ioe) {
            throw new DasIOException(ioe);
        }
        finally {
            try { pin.close(); } catch (IOException ioe) {}
        }
    }
    
    private static String getPacketID(byte[] four) throws DasException {
        if ((four[0] == (byte)'[' && four[3] == (byte)']') || (four[0] == (byte)':' && four[3] == (byte)':')) {
            return new String(new char[]{(char)four[1], (char)four[2]});
        }
        else {
            throw new DasException("Invalid stream, expecting 4 byte header, encountered '" + new String(four) + "'");
        }
    }
    
    private DataSet getLegacyVectorDataSet(InputStream in0, Datum start) throws DasException {
        try {
            PushbackInputStream in = new PushbackInputStream(in0, 50);
            PacketDescriptor sd = getPacketDescriptor(in);
            VectorDataSetBuilder builder = new VectorDataSetBuilder(start.getUnits(),Units.dimensionless);   // Units will be set when "" is encountered
            for (Iterator i = sd.getYDescriptors().iterator(); i.hasNext();) {
                Object o = i.next();
                if (o instanceof StreamMultiYDescriptor) {
                    StreamMultiYDescriptor y = (StreamMultiYDescriptor)o;
                    String name =  y.getName();
                    if (name != null && !name.equals("")) {
                        builder.addPlane(name,y.getUnits());
                    } else if ( "".equals(name) ) {
                        builder.setYUnits(y.getUnits());
                    }
                }
                else {
                    throw new DasIOException("Invalid Stream Header: Non-Y-descriptor encountered");
                }
            }
            StreamMultiYDescriptor[] yDescriptors = (StreamMultiYDescriptor[])sd.getYDescriptors().toArray(new StreamMultiYDescriptor[0]);
            int planeCount = yDescriptors.length - 1;
            String[] planeIDs = new String[planeCount];
            int recordSize = sd.getXDescriptor().getSizeBytes() + yDescriptors[0].getSizeBytes();
            for (int planeIndex = 0; planeIndex < planeCount; planeIndex++) {
                planeIDs[planeIndex] = yDescriptors[planeIndex+1].getName();
                recordSize += yDescriptors[planeIndex+1].getSizeBytes();
            }
            ByteBuffer data = getByteBuffer(in);
            double timeBaseValue = start.doubleValue(start.getUnits());
            Units offsetUnits = start.getUnits().getOffsetUnits();
            UnitsConverter uc = sd.getXDescriptor().getUnits().getConverter(offsetUnits);
            while (data.remaining() > recordSize) {
                DatumVector vector = sd.getXDescriptor().read(data);
                double xTag = timeBaseValue + vector.doubleValue(0, offsetUnits);
                vector = yDescriptors[0].read(data);
                double yValue = vector.doubleValue(0, yDescriptors[0].getUnits());
                builder.insertY(xTag, yValue);
                for (int planeIndex = 0; planeIndex < planeCount; planeIndex++) {
                    vector = yDescriptors[planeIndex + 1].read(data);
                    yValue = vector.doubleValue(0, yDescriptors[planeIndex + 1].getUnits());
                    builder.insertY(xTag, yValue, yDescriptors[planeIndex + 1].getName());
                }
            }
            builder.addProperties(properties);
            VectorDataSet result = builder.toVectorDataSet();
            return result;
        }
        catch (DasException de) {
            de.printStackTrace();
            throw de;
        }
    }
    
    private DataSet getLegacyTableDataSet(InputStream in0, Datum start) throws DasException {
        PushbackInputStream in= new PushbackInputStream(in0,50);
        PacketDescriptor sd = getPacketDescriptor(in);
        TableDataSetBuilder builder = new TableDataSetBuilder(start.getUnits(),Units.dimensionless,Units.dimensionless);
        Units yUnits = Units.dimensionless;
        Units zUnits= Units.dimensionless;
        
        for (Iterator i = sd.getYDescriptors().iterator(); i.hasNext();) {
            Object o = i.next();
            if (o instanceof StreamYScanDescriptor) {
                StreamYScanDescriptor scan = (StreamYScanDescriptor)o;
                String name = scan.getName();
                if (name != null && !name.equals("")) {
                    builder.addPlane(name,scan.getZUnits());
                }
            }
            else {
                throw new DasIOException("Invalid Stream Header: Non-yScan descriptor encountered");
            }
        }
        StreamYScanDescriptor[] yScans = (StreamYScanDescriptor[])sd.getYDescriptors().toArray(new StreamYScanDescriptor[0]);
        final int planeCount = yScans.length;
        String[] planeIDs = new String[planeCount];
        int recordSize = sd.getXDescriptor().getSizeBytes();
        for (int planeIndex = 0; planeIndex < planeCount; planeIndex++) {
            planeIDs[planeIndex] = yScans[planeIndex].getName();
            recordSize += yScans[planeIndex].getSizeBytes();
        }
        ByteBuffer data = getByteBuffer(in);
        double timeBaseValue= start.doubleValue(start.getUnits());
        Units offsetUnits = start.getUnits().getOffsetUnits();
        UnitsConverter uc = sd.getXDescriptor().getUnits().getConverter(offsetUnits);
        double[] yCoordinates = yScans[0].getYTags();
        DatumVector y = DatumVector.newDatumVector(yCoordinates, yUnits);
        
        while (data.remaining() > recordSize) {
            DatumVector vector = sd.getXDescriptor().read(data);
            Datum xTag = start.add(vector.get(0));
            DatumVector[] z = new DatumVector[planeCount];
            for (int planeIndex = 0; planeIndex < planeCount; planeIndex++) {
                z[planeIndex]= yScans[planeIndex].read(data);
            }
            builder.insertYScan(xTag, y, z, planeIDs);
        }
        
        if ( properties.containsKey("x_sample_width") ) {
            properties.put( "xTagWidth", Datum.create( ((Double)properties.get("x_sample_width")).doubleValue(),
            Units.seconds ) );
        }
        
        builder.addProperties(properties);
        TableDataSet result = builder.toTableDataSet();
        return result;
    }
    
    private static final byte[] HEADER = { (byte)'d', (byte)'a', (byte)'s', (byte)'2', (byte)0177, (byte)0177 };
    
    private PacketDescriptor getPacketDescriptor(PushbackInputStream in) throws DasIOException {
        try {
            byte[] four = new byte[HEADER.length];
            
            int bytesRead = 0;
            int totalBytesRead = 0;
            do {
                bytesRead = in.read(four, totalBytesRead, HEADER.length - totalBytesRead);
                if (bytesRead != -1) totalBytesRead += bytesRead;
            } while (totalBytesRead < HEADER.length && bytesRead != -1);
            if (Arrays.equals(four, HEADER)) {
                byte[] header = StreamTool.advanceTo(in, "\177\177".getBytes());
                ByteArrayInputStream source = new ByteArrayInputStream(header);
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document document = builder.parse(source);
                Element docNode= document.getDocumentElement();
                PacketDescriptor packetDescriptor= new PacketDescriptor(docNode);
                return packetDescriptor;
            }
            else {
                in.unread(four, 0, totalBytesRead);
                return defaultPacketDescriptor;
            }
        }
        catch ( ParserConfigurationException ex ) {
            throw new IllegalStateException(ex.getMessage());
        }
        catch ( StreamTool.DelimeterNotFoundException dnfe) {
            DasIOException dioe = new DasIOException(dnfe.getMessage());
            dioe.initCause(dioe);
            throw dioe;
        }
        catch ( SAXException ex ) {
            DasIOException e= new DasIOException(ex.getMessage());
            e.initCause(ex);
            throw e;
        }
        catch ( IOException ex) {
            throw new DasIOException(ex);
        }
    }
    
    public boolean isRestrictedAccess() {
        boolean result;
        if (getProperty("groupAccess") != null) {
            result= !("".equals(getProperty("groupAccess")));
        } else {
            result= false;
        }
        return result;
    }
    
    public edu.uiowa.physics.pw.das.graph.DasAxis getDefaultXAxis(edu.uiowa.physics.pw.das.graph.DasRow row, edu.uiowa.physics.pw.das.graph.DasColumn column) {
        return null;
    }
    
    public edu.uiowa.physics.pw.das.graph.Renderer getRenderer(edu.uiowa.physics.pw.das.graph.DasPlot plot) {
        return null;
    }
    
    public void setServerSideReduction(boolean x) {
        serverSideReduction= x;
    }
    
    public boolean isServerSideReduction() {
        return serverSideReduction;
    }
    
    public PacketDescriptor getDefaultPacketDescriptor() {
        return defaultPacketDescriptor;
    }
    
}
