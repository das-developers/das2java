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

import edu.uiowa.physics.pw.das.DasException;
import edu.uiowa.physics.pw.das.DasIOException;
import edu.uiowa.physics.pw.das.client.CachedXTaggedYScanDataSetDescriptor;
import edu.uiowa.physics.pw.das.dataset.DataRequestor;
import edu.uiowa.physics.pw.das.dataset.DataSet;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.client.*;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.event.DasEventMulticaster;
import edu.uiowa.physics.pw.das.util.*;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class StreamDataSetDescriptor extends DataSetDescriptor implements Serializable {
    
    private DataSet cacheDataSet;
    private class CacheTag {
        Datum start;
        Datum end;
        Datum resolution;
        CacheTag( Datum start, Datum end, Datum resolution ) {
            this.start= start;
            this.end= end;
            this.resolution= resolution;
        }
    }
    private CacheTag cacheTag;
    
    private static Map EMPTY_MAP = Collections.EMPTY_MAP;
    
    private Map properties = EMPTY_MAP;
    
    DataSet cachedDS;
    
    public String description = "";
    public String form = "";
    public String reader = "";
    public String dataSetID= "No dsdfFile set";
    
    public String x_parameter = "";
    public String x_unit = "";
    
    public double x_sample_width = Double.NaN;  // from dsdf -- obsolete
    public double xSampleWidth= Double.NaN;  // this is used now, and is in xUnits.
    
    private Units xUnits;
    
    protected StandardDataStreamSource standardDataStreamSource;
    
    /**
     * Creates a new instance of <code>DataSetDescription</code>
     * from the specified file
     *
     * @param filename the name of the file containing the data set description
     * @throws java.io.IOException if there is an error reading from the file.
     */
    
    protected StreamDataSetDescriptor( Units xUnits ) {       
        this.xUnits= xUnits;
        // yuck--I trust you will set the sdss.
    }
    
    
    private boolean serverSideReduction = true;
    
    public void setStandardDataStreamSource(StandardDataStreamSource sdss) {
        this.standardDataStreamSource= sdss;
    }
    
    public StandardDataStreamSource getStandardDataStreamSource() {
        return this.standardDataStreamSource;
    }
    
    public boolean isDas2Stream() {
        String xxx= (String)properties.get("stream");
        return ( xxx!=null) && xxx.equals("1");
    }
        
    /**
     * Returns the value of the property with the specified name
     *
     * @param name The name of the property requested
     * @return The value of the requested property as an Object
     */
    public Object getProperty(String name) {
        return properties.get(name);
    }
    
    /**
     * Returns a list of valid property names in this DataSetDescription
     *
     * @return Property names for this DataSetDescription as a String array
     */
    public String[] getPropertyNames() {
        return (String[])properties.keySet().toArray(new String[properties.size()]);
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
    protected float[] readFloats(InputStream in, Datum start, Datum end) throws DasException {
        float[] f;
        byte[] data = readBytes(in, start, end);
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
    protected double[] readDoubles(InputStream in, Datum start, Datum end) throws DasException {
        double[] d;
        byte[] data = readBytes(in, start, end);
        d = new double[data.length/4];
        ByteBuffer buff = ByteBuffer.wrap(data);
        FloatBuffer fbuff = buff.asFloatBuffer();
        for (int i = 0; i < d.length; i++) {
            d[i] = fbuff.get();
        }
        
        return d;
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
    protected byte[] readBytes(InputStream uin, Datum start, Datum end) throws edu.uiowa.physics.pw.das.DasException {
        
        LinkedList list = new LinkedList();
        byte[] data;
        int bytesRead=0;
        int totalBytesRead=0;
        
        //BufferedInputStream in= new BufferedInputStream(uin,4096*2);
        InputStream in= uin;
        
        long time = System.currentTimeMillis();
        //FileOutputStream out= new FileOutputStream("x."+time+".dat");
        
        data = new byte[4096];
        
        int lastBytesRead = -1;
        
        String s;
        
        int offset=0;
        
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
            throw new DasIOException("Error reading data for '"+description+"', no data available");
        }
        
        int dataLength = (list.size()-1)*4096 + lastBytesRead;
        
        data = new byte[dataLength];
        
        Iterator iterator = list.iterator();
        int i;
        for (i = 0; i < list.size()-1; i++) {
            System.arraycopy(iterator.next(), 0, data, i*4096, 4096);
        }
        System.arraycopy(iterator.next(), 0, data, i*4096, lastBytesRead);
        return data;
    }
    
    public String toString() {
        return dataSetID;
    }
    
    public DataSet getDataSetImpl( Datum start, Datum end, Datum resolution, DasProgressMonitor monitor ) throws DasException {
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
        cacheTag= new CacheTag(start,end,resolution);
        cacheDataSet= result;
        return result;
    }
    
    protected abstract DataSet getDataSet(InputStream in, Datum start, Datum end, Datum resolution) throws DasException;
    
    public String getDataSetID() {
        return dataSetID;
    }
    
    protected void setProperties(Map properties) {
        
        this.properties = new HashMap(properties);
        
        if (properties.containsKey("description")) {
            description= (String)properties.get("description");
        }
        if (properties.containsKey("form")) {
            form = (String)properties.get("form");
        }
        if (properties.containsKey("reader")) {
            reader = (String)properties.get("reader");
        }
        if (properties.containsKey("x_parameter")) {
            x_parameter = (String)properties.get("x_parameter");
        }
        if (properties.containsKey("x_unit")) {
            x_unit= (String)properties.get("x_unit");
        }
        if (properties.containsKey("x_sample_width")) {
            x_sample_width = ((Double)properties.get("x_sample_width")).doubleValue();
            UnitsConverter uc= UnitsConverter.getConverter(Units.seconds,((LocationUnits)getXUnits()).getOffsetUnits());
            xSampleWidth= uc.convert(x_sample_width);
        }
        
    }
    
    public Units getXUnits() {
        return this.xUnits;
    }
    
    public boolean isRestrictedAccess() {
        boolean result;
        if (properties.containsKey("groupAccess")) {
            result= !(properties.get("groupAccess").equals(""));
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
    
    
    public static void main( String[] args ) {
        try {
            String url= "http://www-pw.physics.uiowa.edu/das/dasServer?galileo/pws/best-e";
            DataSetDescriptor dsd= DataSetDescriptorUtil.create(url);
            System.out.println(dsd);        
        } catch ( DasException e ) {
            System.out.println(e);
        }
    }
    
    public Datum getXSampleWidth() {
        Units xUnits= getXUnits();
        if ( xUnits instanceof LocationUnits ) {
            xUnits= ((LocationUnits)xUnits).getOffsetUnits();
        }
        return Datum.create(xSampleWidth,xUnits);
    }
    
    public void setXSampleWidth( Datum datum ) {
        if ( getXUnits() instanceof LocationUnits ) {
            xSampleWidth= datum.doubleValue( ((LocationUnits)getXUnits()).getOffsetUnits() );
        } else {
            xSampleWidth= datum.doubleValue(getXUnits());
        }
    }
    
    public void setServerSideReduction(boolean x) {
        serverSideReduction= x;
    }
    
    public boolean isServerSideReduction() {
        return serverSideReduction;
    }
        
}
