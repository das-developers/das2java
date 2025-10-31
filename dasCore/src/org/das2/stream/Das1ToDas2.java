/*
 * Das1ToDas2.java
 *
 * Created on December 10, 2003, 8:40 PM
 */

package org.das2.stream;

import org.das2.datum.Datum;
import org.das2.datum.TimeUtil;
import org.das2.DasProperties;
import org.das2.DasException;
import org.das2.util.IDLParser;
import java.io.*;
import java.util.*;

/**
 *
 * @author  jbf
 */
public class Das1ToDas2 {
    
    static Das1ToDas2 _instance;
    
    String createStreamDescriptor( Map dsdp, Datum start, Datum end ) {
        String header= "<stream ";
        header+= "start=\""+start+"\" end=\""+end+"\" ";        
        header+= "/>";
        return header;
    }
    
    String createPacketDescriptor( Map dsdp, Datum base ) {
        if ( dsdp.get("form").equals("x_tagged_y_scan")) {            
            double[] yTag= (double[])dsdp.get("y_coordinate");
            String yTagString= ""+yTag[0];
            for ( int i=1; i<yTag.length; i++ ) yTagString+= ", "+yTag[i];
            int nitems= yTag.length;
            
            return "<packet>\n"+
               "    <x type=\"float\" base=\""+base+"\" xUnits=\"seconds\" />\n" +
               "    <yscan nitems=\""+nitems+"\" type=\"float\" yTags=\""+yTagString+"\" yUnits=\"\" zUnits=\"\"/>\n" +
               "    </packet>";
        } else {
            throw new IllegalArgumentException("not implemented yet for anything besides x_tagged_y_scan");
        }
    }
    
    Map getDsdfProperties( String dsdf ) throws IOException {
        FileReader r= new FileReader( dsdf );
        BufferedReader in = new BufferedReader(r);
        IDLParser parser = new IDLParser();
        double[] array;
        String key;
        String value;
        String line;
        int index, lineNumber;
        
        line = in.readLine();
        lineNumber = 1;
        
        HashMap properties = new HashMap();
        
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
                properties.put(key, x_sample_width);
            }
            else if (key.equals("y_fill")) {
                double y_fill = parser.parseIDLScalar(value);
                if (y_fill == Double.NaN)
                    throw new IOException("Could not parse \"" + value + "\" at line " + lineNumber);
                properties.put(key, y_fill);
            }
            else if (key.equals("z_fill")) {
                double z_fill = (float)parser.parseIDLScalar(value);
                if (z_fill == Float.NaN)
                    throw new IOException("Could not parse \"" + value + "\" at line " + lineNumber);
                properties.put(key, z_fill);
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
                properties.put(key, ny);
            }
            else if (key.equals("items")) {
                int items;
                try {
                    items = Integer.parseInt(value);
                }
                catch (NumberFormatException nfe) {
                    throw new IOException("Could not parse \"" + value + "\" at line " + lineNumber);
                }
                properties.put(key, items);
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
        return properties;
    }
    
    public static Das1ToDas2 getInstance() {
        if ( _instance==null ) {
            _instance= new Das1ToDas2();
        } 
        return _instance;
    }
            
    public void das1ToDas2( String dsdf, InputStream in, OutputStream out, Datum start, Datum end ) throws DasException, IOException {        
        Map properties= getDsdfProperties( dsdf );        
        String header= createStreamDescriptor( properties, start, end );
        String packet= createPacketDescriptor( properties, start );
        out.write( "[00]".getBytes() );
        out.write( header.getBytes() );
        out.write( "[01]".getBytes() );
        out.write( packet.getBytes() );
        
        int nItems= ((double[])properties.get("y_coordinate")).length;
        int bytesPerPacket= ( nItems + 1 ) * 4;
        
        byte[] buffer= new byte[bytesPerPacket];
        
        int b= in.read(buffer);
        
        if ( b==-1 ) {
            out.write("[xx]<exception message=\"No data in interval\" source=\"Das1ToDas2\"/>".getBytes());
        }
        
        int offset= b;
        
        while ( b!=-1 ) {
            while ( b!=-1 && offset<bytesPerPacket ) {
                b= in.read( buffer, offset, bytesPerPacket-offset );
            }
            
            if ( b==-1 ) {
                out.close();  // jbf: does this do anything for OutputStream?
            }
            out.write(":01:".getBytes());
            out.write(buffer);
            
            b= in.read(buffer);
            offset= b;
        }
        
    }
    
    public static InputStream das1ToDas2( final InputStream in, final String dsdf, final Datum start, final Datum end ) throws IOException {
        final Das1ToDas2 instance= getInstance();
        final PipedOutputStream out= new PipedOutputStream();
        final PipedInputStream pin= new PipedInputStream(out);
        Runnable r= new Runnable() {
            public void run() {
                try {
                    instance.das1ToDas2( dsdf, in, out, start, end );
                    out.close();
                } catch ( IOException e ) {
                    DasProperties.getLogger().severe(e.toString());
                } catch ( DasException e ) {
                    DasProperties.getLogger().severe(e.toString());
                }
            }
        };
        Thread t= new Thread(r);
        t.start();
        return pin;
        
    }
    
    public static void main(String[] args) throws Exception {
        final Das1ToDas2 instance= getInstance();
        InputStream in=null;
        OutputStream out=null;
        if ( args.length == 3 ) {
            in= System.in;
            out= System.out;
            Datum start= TimeUtil.create(args[1]);
            Datum end= TimeUtil.create(args[2]);
            instance.das1ToDas2( args[0], in, out, start, end );
        } else {
            System.err.println("usage: Das1ToDas2 <dsdf file> <start> <end>");
        }
        
    }
    
    
}
