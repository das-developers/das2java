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
import edu.uiowa.physics.pw.das.util.*;
import edu.uiowa.physics.pw.das.stream.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.regex.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.w3c.dom.*;

public class StreamDataSetDescriptor extends DataSetDescriptor {
    
    protected StandardDataStreamSource standardDataStreamSource;
    private boolean serverSideReduction = true;
    private StreamDescriptor defaultStreamDescriptor;

    public Units getXUnits() {
        return defaultStreamDescriptor.getXDescriptor().getUnits();
    }
    
    /**
     * Creates a new instance of <code>StreamDataSetDescriptor</code>
     * from the specified file
     */
    protected StreamDataSetDescriptor(Map properties) {
        setProperties(properties);
    }
    
    public StreamDataSetDescriptor(StreamDescriptor sd, StandardDataStreamSource sdss) {
        this(sd.getProperties());
        this.standardDataStreamSource = sdss;
    }
    
    public void setStandardDataStreamSource(StandardDataStreamSource sdss) {
        this.standardDataStreamSource= sdss;
    }
    
    public StandardDataStreamSource getStandardDataStreamSource() {
        return this.standardDataStreamSource;
    }
    
    protected void setProperties( Map properties ) {
        super.setProperties(properties);
        defaultStreamDescriptor = dsdfToStreamDescriptor(properties);
    }

    public StreamDescriptor getDefaultStreamDescriptor() {
        return defaultStreamDescriptor;
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
        if ( ! resolution.isFinite() ) throw new IllegalArgumentException( "resolution is not finite" );
        InputStream in;
        DataSet result;
        if ( isServerSideReduction() ) {
            in= standardDataStreamSource.getReducedInputStream( this, start, end, resolution);
        } else {
            in= standardDataStreamSource.getInputStream( this, start, end );
        }
        in = new DasProgressMonitorInputStream(in, monitor);
        result = getDataSet( in, start, end, resolution );
        return result;
    }
    
    protected DataSet getDataSet(InputStream in, Datum start, Datum end, Datum resolution) throws DasException {
        if (getProperty("form").equals("x_tagged_y_scan")) {
            return getTableDataSet(in, start);
        }
        else if (getProperty("form").equals("x_multi_y")) {
            return getVectorDataSet(in, start);
        }
        else {
            throw new IllegalStateException("Unrecognized data set type: " + getProperty("form"));
        }
    }
    
    private DataSet getVectorDataSet(InputStream in0, Datum start) throws DasException {
        try {
            PushbackInputStream in = new PushbackInputStream(in0, 50);
            StreamDescriptor sd = getStreamDescriptor(in);
            VectorDataSetBuilder builder = new VectorDataSetBuilder();
            builder.setXUnits(start.getUnits());
            builder.setYUnits(Units.dimensionless);
            for (Iterator i = sd.getYDescriptors().iterator(); i.hasNext();) {
                Object o = i.next();
                if (o instanceof StreamMultiYDescriptor) {
                    StreamMultiYDescriptor y = (StreamMultiYDescriptor)o;
                    String name =  y.getName();
                    if (name != null && !name.equals("")) {
                        builder.addPlane(name);
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
            int recordCount = data.remaining() / recordSize;
            double timeBaseValue = start.doubleValue(start.getUnits());
            Units offsetUnits = start.getUnits().getOffsetUnits();
            UnitsConverter uc = sd.getXDescriptor().getUnits().getConverter(offsetUnits);
            double[] xTag = new double[1];
            double[] yValue = new double[1];
            while (data.remaining() > recordSize) {
                sd.getXDescriptor().read(data, xTag, 0);
                xTag[0] = timeBaseValue + uc.convert(xTag[0]);
                yDescriptors[0].read(data, yValue, 0);
                builder.insertY(xTag[0], yValue[0]);
                for (int planeIndex = 0; planeIndex < planeCount; planeIndex++) {
                    yDescriptors[planeIndex + 1].read(data, yValue, 0);
                    builder.insertY(xTag[0], yValue[0], yDescriptors[planeIndex + 1].getName());
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
    
    private DataSet getTableDataSet(InputStream in0, Datum start) throws DasException {
        PushbackInputStream in= new PushbackInputStream(in0,50);
        StreamDescriptor sd = getStreamDescriptor(in);
        TableDataSetBuilder builder = new TableDataSetBuilder();
        builder.setXUnits(start.getUnits());
        builder.setYUnits(Units.dimensionless);
        builder.setZUnits(Units.dimensionless);
        for (Iterator i = sd.getYDescriptors().iterator(); i.hasNext();) {
            Object o = i.next();
            if (o instanceof StreamYScanDescriptor) {
                StreamYScanDescriptor scan = (StreamYScanDescriptor)o;
                String name = scan.getName();
                if (name != null && !name.equals("")) {
                    builder.addPlane(name);
                }
            }
            else {
                throw new DasIOException("Invalid Stream Header: Non-yScan descriptor encountered");
            }
        }
        StreamYScanDescriptor[] yScans = (StreamYScanDescriptor[])sd.getYDescriptors().toArray(new StreamYScanDescriptor[0]);
        int planeCount = yScans.length - 1;
        String[] planeIDs = new String[planeCount];
        int recordSize = sd.getXDescriptor().getSizeBytes() + yScans[0].getSizeBytes();
        for (int planeIndex = 0; planeIndex < planeCount; planeIndex++) {
            planeIDs[planeIndex] = yScans[planeIndex+1].getName();
            recordSize += yScans[planeIndex+1].getSizeBytes();
        }
        ByteBuffer data = getByteBuffer(in);
        int recordCount = data.remaining() / recordSize;
        double timeBaseValue= start.doubleValue(start.getUnits());
        Units offsetUnits = start.getUnits().getOffsetUnits();
        UnitsConverter uc = sd.getXDescriptor().getUnits().getConverter(offsetUnits);
        double[] xTag = new double[1];
        double[] yCoordinates = yScans[0].getYCoordinates();
        double[] scan = new double[yScans[0].getNItems()];
        while (data.remaining() > recordSize) {
            sd.getXDescriptor().read(data, xTag, 0);
            xTag[0] = timeBaseValue + uc.convert(xTag[0]);
            yScans[0].read(data, scan, 0);
            builder.insertYScan(xTag[0], yCoordinates, scan);
            for (int planeIndex = 0; planeIndex < planeCount; planeIndex++) {
                yScans[planeIndex + 1].read(data, scan, 0);
                builder.insertYScan(xTag[0], yCoordinates, scan, yScans[planeIndex + 1].getName());
            }
        }
        builder.addProperties(properties);
        TableDataSet result = builder.toTableDataSet();
        return result;
    }
    
    private static final byte[] HEADER = { (byte)'d', (byte)'a', (byte)'s', (byte)'2', (byte)0177, (byte)0177 };
    
    private StreamDescriptor getStreamDescriptor(PushbackInputStream in) throws DasIOException {
        try {
            byte[] tip = new byte[HEADER.length];
            
            int bytesRead = 0;
            int totalBytesRead = 0;
            do {
                bytesRead = in.read(tip, totalBytesRead, HEADER.length - totalBytesRead);
                if (bytesRead != -1) totalBytesRead += bytesRead;
            } while (totalBytesRead < HEADER.length && bytesRead != -1);
            if (Arrays.equals(tip, HEADER)) {
                byte[] header = StreamTool.advanceTo(in, "\177\177".getBytes());
                ByteArrayInputStream source = new ByteArrayInputStream(header);
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document document = builder.parse(source);
                Element docNode= document.getDocumentElement();
                StreamDescriptor streamDescriptor= new StreamDescriptor(docNode);
                return streamDescriptor;
            }
            else {
                in.unread(tip, 0, totalBytesRead);
                return defaultStreamDescriptor;
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
    
    private StreamDescriptor dsdfToStreamDescriptor(Map dsdf) {
        StreamDescriptor streamDescriptor = new StreamDescriptor();
        streamDescriptor.setXDescriptor(new StreamXDescriptor());
        if (dsdf.get("form").equals("x_tagged_y_scan")) {
            StreamYScanDescriptor yscan = new StreamYScanDescriptor();
            yscan.setYCoordinates((double[])dsdf.get("y_coordinate"));
            streamDescriptor.addYScan(yscan);
        }
        else if (dsdf.get("form").equals("x_multi_y") && dsdf.get("ny") != null) {
            StreamMultiYDescriptor y = new StreamMultiYDescriptor();
            streamDescriptor.addYMulti(y);
        }
        else if (dsdf.get("form").equals("x_multi_y") && dsdf.get("items") != null) {
            List planeList = (List)dsdf.get("plane-list");
            streamDescriptor.addYMulti(new StreamMultiYDescriptor());
            for (int index = 0; index < planeList.size(); index++) {
                StreamMultiYDescriptor y = new StreamMultiYDescriptor();
                y.setName((String)planeList.get(index));
                streamDescriptor.addYMulti(y);
            }
        }
    
        return streamDescriptor;
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
        
}
