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
package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.DasException;
import edu.uiowa.physics.pw.das.DasIOException;
import edu.uiowa.physics.pw.das.dataset.CachedXTaggedYScanDataSetDescriptor;
import edu.uiowa.physics.pw.das.dataset.DataRequestor;
import edu.uiowa.physics.pw.das.dataset.DataSet;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.client.*;
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

public abstract class DataSetDescriptor implements Serializable {
    
    private static Map EMPTY_MAP = Collections.EMPTY_MAP;
    
    private Map properties = EMPTY_MAP;
    
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
    
    protected DataSetDescriptor( Units xUnits ) {
        this.xUnits= xUnits;
    }
    
    private static final Pattern CLASS_ID = Pattern.compile("class:([a-zA-Z\\.]+)(?:\\?(.*))?");
    private static final Pattern NAME_VALUE = Pattern.compile("([_0-9a-zA-Z%+.]+)=([_0-9a-zA-Z%+./]+)");
    
    private boolean serverSideReduction = true;
    
    public static DataSetDescriptor create(String dataSetID) throws DasException {
        java.util.regex.Matcher classMatcher = CLASS_ID.matcher(dataSetID);
        if (classMatcher.matches()) {
            try {
                String className = classMatcher.group(1);
                String argString = classMatcher.group(2);
                String[] argList;
                if (argString != null && argString.length() > 0) {
                    argList = argString.split("&");
                }
                else {
                    argList = new String[0];
                }
                URLDecoder decoder = new URLDecoder();
                Map argMap = new HashMap();
                for (int index = 0; index < argList.length; index++) {
                    Matcher argMatcher = NAME_VALUE.matcher(argList[index]);
                    if (argMatcher.matches()) {
                        argMap.put(decoder.decode(argMatcher.group(1), "UTF-8"),
                                   decoder.decode(argMatcher.group(2), "UTF-8"));
                    }
                    else {
                        throw new NoSuchDataSetException("Invalid argument: " + argList[index]);
                    }
                }
                Class dsdClass = Class.forName(className);
                Method method = dsdClass.getMethod("newDataSetDescriptor", new Class[]{java.util.Map.class});
                if (!Modifier.isStatic(method.getModifiers())) {
                    throw new NoSuchDataSetException("getDataSetDescriptor must be static");
                }
                return (DataSetDescriptor)method.invoke(null, new Object[]{argMap});
            }
            catch (ClassNotFoundException cnfe) {
                DataSetDescriptorNotAvailableException dsdnae =
                   new DataSetDescriptorNotAvailableException(cnfe.getMessage());
                dsdnae.initCause(cnfe);
                throw dsdnae;
            }
            catch (NoSuchMethodException nsme) {
                DataSetDescriptorNotAvailableException dsdnae =
                    new DataSetDescriptorNotAvailableException(nsme.getMessage());
                dsdnae.initCause(nsme);
                throw dsdnae;
            }
            catch (InvocationTargetException ite) {
                DataSetDescriptorNotAvailableException dsdnae =
                    new DataSetDescriptorNotAvailableException(ite.getTargetException().getMessage());
                dsdnae.initCause(ite.getTargetException());
                throw dsdnae;
            }
            catch (IllegalAccessException iae) {
                DataSetDescriptorNotAvailableException dsdnae =
                    new DataSetDescriptorNotAvailableException(iae.getMessage());
                dsdnae.initCause(iae);
                throw dsdnae;
            }
            catch (UnsupportedEncodingException uee) {
                throw new RuntimeException(uee);
            }
        }
        else if (!dataSetID.startsWith("http://")) {
            dataSetID = "http://www-pw.physics.uiowa.edu/das/dasServer?" + dataSetID;
        }
        try {
            DataSetDescriptor result = create(new URL(dataSetID));
            result.dataSetID = dataSetID;
            return result;
        }
        catch (MalformedURLException mue) {
            throw new DasIOException(mue.getMessage());
        }
    }
    
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
     * Creates a new instance of <code>DataSetDescription</code>
     * from the specified <code>Reader</code>
     *
     * @param in the specified <code>Reader</code>
     * @throws java.io.IOException if there is an error reading from the <code>InputStream</code>
     */
    private static DataSetDescriptor create(Reader r) throws IOException {
        BufferedReader in = new BufferedReader(r);
        IDLParser parser = new IDLParser();
        double[] array;
        String key;
        String value;
        String line;
        int index, lineNumber;
        
        line = in.readLine();
        lineNumber = 1;
        
        Hashtable properties = new Hashtable();
        
        while (line != null) {
            //Get rid of any comments
            index = line.trim().indexOf(';');
            if (index == 0) {
                lineNumber++;
                line = in.readLine();
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
                properties.put(key, description);
            }
            else if (key.equals("groupAccess")) {
                properties.put(key, value.substring(1, value.length()-1));
            }
            else if (key.equals("form")) {
                properties.put(key, value);
            }
            else if (key.equals("reader")) {
                String reader = value.substring(1, value.length()-1);
                properties.put(key, reader);
            }
            else  if (key.equals("x_parameter")) {
                String x_parameter = value.substring(1, value.length()-1);
                properties.put(key, x_parameter);
            }
            else if (key.equals("x_unit")) {
                String x_unit = value.substring(1, value.length()-1);
                properties.put(key, x_unit);
            }
            else if (key.equals("y_parameter")) {
                String y_parameter = value.substring(1, value.length()-1);
                properties.put(key, y_parameter);
            }
            else if (key.equals("y_unit")) {
                String y_unit = value.substring(1, value.length()-1);
                properties.put(key, y_unit);
            }
            else if (key.equals("z_parameter")) {
                String z_parameter = value.substring(1, value.length()-1);
                properties.put(key, z_parameter);
            }
            else if (key.equals("z_unit")) {
                String z_unit = value.substring(1, value.length()-1);
                properties.put(key, z_unit);
            }
            else if (key.equals("x_sample_width")) {
                double x_sample_width = parser.parseIDLScalar(value);
                if (x_sample_width == Double.NaN)
                    throw new IOException("Could not parse \"" + value + "\" at line " + lineNumber);
                properties.put(key, new Double(x_sample_width));
            }
            else if (key.equals("y_fill")) {
                double y_fill = parser.parseIDLScalar(value);
                if (y_fill == Double.NaN)
                    throw new IOException("Could not parse \"" + value + "\" at line " + lineNumber);
                properties.put(key, new Double(y_fill));
            }
            else if (key.equals("z_fill")) {
                double z_fill = (float)parser.parseIDLScalar(value);
                if (z_fill == Float.NaN)
                    throw new IOException("Could not parse \"" + value + "\" at line " + lineNumber);
                properties.put(key, new Float(z_fill));
            }
            else if (key.equals("y_coordinate")) {
                array = parser.parseIDLArray(value);
                if (array == null) {
                    throw new IOException("Could not parse \"" + value + "\" at line " + lineNumber);
                }
                properties.put(key, array);
            }
            else if (key.equals("ny")) {
                int ny;
                try {
                    ny = Integer.parseInt(value);
                }
                catch (NumberFormatException nfe) {
                    throw new IOException("Could not parse \"" + value + "\" at line " + lineNumber);
                }
                properties.put(key, new Integer(ny));
            }
            else if (key.equals("items")) {
                int items;
                try {
                    items = Integer.parseInt(value);
                }
                catch (NumberFormatException nfe) {
                    throw new IOException("Could not parse \"" + value + "\" at line " + lineNumber);
                }
                properties.put(key, new Integer(items));
            }
            else if (value.charAt(0)=='\'' && value.charAt(value.length()-1)=='\'') {
                properties.put(key, value.substring(1, value.length()-1));
            }
            else if (value.charAt(0)=='"' && value.charAt(value.length()-1)=='"') {
                properties.put(key, value.substring(1, value.length()-1));
            }
            else {
                properties.put(key, value);
            }
            line = in.readLine();
            lineNumber++;
        }
        
        DataSetDescriptor result;
        
        String form= (String)properties.get("form");
        if (form.equals("x_tagged_y_scan")) {
            result= new CachedXTaggedYScanDataSetDescriptor(properties);
        } else if ( form.equals("x_multi_y") ) {
            result= new XMultiYDataSetDescriptor(properties);
        } else {
            throw new IllegalArgumentException("dsdf file is invalid, 'form' keyword is not valid.");
        }
        return result;
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
    
    public DataSet getDataSet( Datum start, Datum end, Datum resolution, DasProgressMonitor monitor ) throws DasException {
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
    
    public static DataSetDescriptor create( URL url ) throws DasException {
        DasServer dasServer = DasServer.create(url);
        try {
            String dsdfString= dasServer.getDataSetDescriptor(url.getQuery());
            DataSetDescriptor dsd = create(new StringReader(dsdfString));
            dsd.standardDataStreamSource = dasServer.getStandardDataStreamSource();
            dsd.dataSetID = url.toExternalForm();
            return dsd;
        } catch ( IOException e ) {
            throw new DasIOException(e.getMessage());
        }
        
    }
    
    public static void main( String[] args ) {
        try {
            URL url= new URL("http://www-pw.physics.uiowa.edu/das/dasServer?galileo/pws/best-e");
            DataSetDescriptor dsd= DataSetDescriptor.create(url);
            System.out.println(dsd);
        } catch ( MalformedURLException e ) {
            System.out.println(e);
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
