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
package org.das2.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.PushbackInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;
import org.das2.dataset.VectorDataSetBuilder;
import org.das2.dataset.DataSetDescriptor;
import org.das2.dataset.TableDataSet;
import org.das2.dataset.DataSet;
import org.das2.dataset.VectorDataSet;
import org.das2.dataset.TableDataSetBuilder;
import org.das2.datum.Units;
import org.das2.datum.Datum;
import org.das2.stream.StreamYScanDescriptor;
import org.das2.stream.StreamScalarDescriptor;
import org.das2.stream.StreamDescriptor;
import org.das2.stream.StreamException;
import org.das2.stream.PacketDescriptor;
import org.das2.DasIOException;
import org.das2.CancelledOperationException;
import org.das2.DasException;
import org.das2.util.DasProgressMonitorInputStream;
import org.das2.util.monitor.NullProgressMonitor;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.datum.DatumVector;
import org.das2.system.DasLogger;
import org.das2.stream.StreamTool;

import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class StreamDataSetDescriptor extends DataSetDescriptor {
    
    protected StandardDataStreamSource standardDataStreamSource;
    private boolean serverSideReduction = true;
    private PacketDescriptor defaultPacketDescriptor;
    
    private static final Logger logger= DasLogger.getLogger(DasLogger.DATA_TRANSFER_LOG);
    
    @Override
    public Units getXUnits() {
        return Units.us2000;
    }
    
    /**
     * Creates a new instance of <code>StreamDataSetDescriptor</code>
     * from the specified file
     * @param properties the properties
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
    
    protected final void setProperties(Map properties, boolean legacy) {
        super.setProperties(properties);
        if (properties.containsKey("form") && properties.get("form").equals("x_multi_y")
            && properties.containsKey("items")) {
                setDefaultCaching(false);
        }
        if (legacy) {
            defaultPacketDescriptor = PacketDescriptor.createLegacyPacketDescriptor(properties);
        }
    }
    
    @Override
    protected final void setProperties( Map properties ) {
        setProperties(properties, false);
    }
    
    private ByteBuffer getByteBuffer(InputStream in) throws DasException {
        byte[] data = readBytes(in);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        return buffer;
    }
    
    /**
     * Auxiliary method used by readDoubles(InputStream, Object, Datum, Datum);
     *
     * Read data for the given start and end dates and returns an array of bytes
     *
     * @author eew
     * @param in the source
     * @return the bytes.
     * @throws DasException If there is an error getting data from the reader
     */
    protected byte[] readBytes(InputStream in) throws DasException {
        LinkedList<byte[]> list = new LinkedList();
        byte[] data = new byte[4096];
        int bytesRead;

        int lastBytesRead = -1;
        int offset=0;
        
        try {
            bytesRead= in.read(data,offset,4096-offset);
            while (bytesRead != -1) {
                offset+=bytesRead;
                lastBytesRead= offset;
                if (offset==4096) {
                    list.addLast(data);
                    data = new byte[4096];
                    offset=0;
                }
                bytesRead= in.read(data,offset,4096-offset);
            }
        } catch ( IOException e ) {
            throw new DasIOException(e);
        }
        
        if (lastBytesRead>=0 && lastBytesRead<4096) {
            list.addLast(data);
        }
        
        if (list.isEmpty()) {
            throw new DasIOException("Error reading data: no data available");
        }
        int dataLength = (list.size()-1)*4096 + lastBytesRead;
        data = new byte[dataLength];
        for (int i = 0; i < list.size()-1; i++) {
            System.arraycopy(list.get(i), 0, data, i*4096, 4096);
        }
        System.arraycopy(list.get(list.size()-1), 0, data, (list.size() - 1)*4096, lastBytesRead);
        return data;
    }
    
    @Override
    public String toString() {
        return "dsd "+getDataSetID();
    }
    
    @Override
    protected DataSet getDataSetImpl( Datum start, Datum end, Datum resolution, ProgressMonitor monitor ) throws DasException {
        if ( resolution != null && !resolution.isFinite() ) throw new IllegalArgumentException( "resolution is not finite" );
        InputStream in;
        DataSet result;
        if ( serverSideReduction ) {
            logger.info("getting stream from standard data stream source");
            in= standardDataStreamSource.getReducedInputStream( this, start, end, resolution);
        } else {
            in= standardDataStreamSource.getInputStream( this, start, end );
        }
        
        logger.info("reading stream");                    
        result = getDataSetFromStream( in, start, end, monitor );
        return result;
    }
    
    protected DataSet getDataSetFromStream(InputStream in, Datum start, Datum end, ProgressMonitor monitor ) throws DasException {
        if ( monitor==null ) monitor= new NullProgressMonitor();
        
        PushbackInputStream pin = new PushbackInputStream(in, 4096);
        try {
            byte[] four = new byte[4];
            int bytesRead= pin.read(four);
            logger.log(Level.FINER, "read first four bytes bytesRead={0}", bytesRead);
            if ( bytesRead!=4 ) {
                logger.info("no data returned from server");
                throw new DasIOException( "No data returned from server" );
            }            
            
            if (new String(four).equals("[00]")) {
                logger.finer("got stream header [00]");
                pin.unread(four);
                
                if ( monitor.isCancelled() ) {
                    pin.close();
                    throw new InterruptedIOException("Operation cancelled");
                }
                
                final DasProgressMonitorInputStream mpin = new DasProgressMonitorInputStream(pin, monitor);
                //InputStream mpin = pin;
                
                logger.finer("creating Channel");
                ReadableByteChannel channel = Channels.newChannel(mpin);
                
                DataSetStreamHandler handler = new DataSetStreamHandler( properties, monitor ) {
                    @Override
                    public void streamDescriptor(StreamDescriptor sd) throws StreamException {
                        super.streamDescriptor( sd );
                        if ( super.taskSize!=-1 ) {
                            mpin.setEnableProgressPosition(false);
                        }
                    }
                };
                
                logger.finer("using StreamTool to read the stream");                
                StreamTool.readStream(channel, handler);
                return handler.getDataSet();
            }
            else {
                pin.unread(four);
                
                if ( monitor.isCancelled() ) {
                    pin.close();
                    throw new InterruptedIOException("Operation cancelled");
                }
                
                monitor.started();
                
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
        catch ( StreamException se ) {
            /* kludge for when an InterruptedIOException is caused by the user's cancel.
             * TODO: This is danger code, because it masks the condition where the 
             * interruption happened for some other reason the user isn't aware of.
             */
            if ( se.getCause() instanceof InterruptedIOException ) {
                DasException e= new CancelledOperationException();
                e.initCause(se);
                throw e;   
            } else {
                throw se;
            }
        }
        finally {
            try { 
                pin.close();
            } catch (IOException ioe) {
                logger.log( Level.WARNING, null, ioe );
            }
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
                if (o instanceof StreamScalarDescriptor) {
                    StreamScalarDescriptor y = (StreamScalarDescriptor)o;
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
            StreamScalarDescriptor[] yDescriptors = (StreamScalarDescriptor[])sd.getYDescriptors().toArray(new StreamScalarDescriptor[0]);
            int planeCount = yDescriptors.length - 1;
            int recordSize = sd.getXDescriptor().getSizeBytes() + yDescriptors[0].getSizeBytes();
            for (int planeIndex = 0; planeIndex < planeCount; planeIndex++) {
                recordSize += yDescriptors[planeIndex+1].getSizeBytes();
            }
            ByteBuffer data = getByteBuffer(in);
            double timeBaseValue = start.doubleValue(start.getUnits());
            Units offsetUnits = start.getUnits().getOffsetUnits();
            //UnitsConverter uc = sd.getXDescriptor().getUnits().getConverter(offsetUnits);
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
            
            if ( properties.containsKey("x_sample_width") ) {
                properties.put( "xTagWidth", Datum.create( ((Double)properties.get("x_sample_width") ),
                Units.seconds ) );
            }
            
            builder.addProperties(properties);
            VectorDataSet result = builder.toVectorDataSet();
            return result;
        }
        catch (DasException de) {
            logger.log( Level.WARNING, de.getMessage(), de );
            throw de;
        }
    }
    
    private DataSet getLegacyTableDataSet(InputStream in0, Datum start) throws DasException {
        PushbackInputStream in= new PushbackInputStream(in0,50);
        PacketDescriptor sd = getPacketDescriptor(in);
        TableDataSetBuilder builder = new TableDataSetBuilder(start.getUnits(),Units.dimensionless,Units.dimensionless);
        Units yUnits = Units.dimensionless;
        //Units zUnits= Units.dimensionless;
        
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
        //double timeBaseValue= start.doubleValue(start.getUnits());
        //Units offsetUnits = start.getUnits().getOffsetUnits();
        //UnitsConverter uc = sd.getXDescriptor().getUnits().getConverter(offsetUnits);
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
            properties.put( "xTagWidth", Datum.create( ((Double)properties.get("x_sample_width") ),
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
            
            int bytesRead;
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
        catch ( StreamTool.DelimeterNotFoundException | StreamException dnfe) {
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
