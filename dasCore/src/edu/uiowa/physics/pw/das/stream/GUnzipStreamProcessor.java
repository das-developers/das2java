/*
 * gzip.java
 *
 * Created on July 11, 2003, 9:39 AM
 */

package edu.uiowa.physics.pw.das.stream;

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.util.*;
import org.w3c.dom.*;

import java.io.*;
import java.util.*;

/**
 *
 * @author  jbf
 */
public class GUnzipStreamProcessor extends StreamProcessor {
    
    public void process( InputStream in, OutputStream out) throws IOException {
        
        byte[] header;
        byte[] tag=new byte[4];
        boolean isCompressed=false;  // true if input stream is already compressed
        
        int bytesRead= in.read( tag, 0, 4 );
        int offset= bytesRead;
        while ( bytesRead!=-1 && offset<4 ) {
            bytesRead= in.read( tag, offset, 4-offset );
            offset+= bytesRead;
        }
        if ( ! Arrays.equals(tag,"[00]".getBytes()) ) {
            throw new IOException( "Expected [00], got "+new String(tag) );
        }
        
        header= StreamTool.readXML(in);
        
        try {
            Document document= StreamDescriptor.parseHeader(new String(header));
            Element docNode= document.getDocumentElement();
            if ( ! docNode.getAttribute("compression").equals("") ) {
                String compression= docNode.getAttribute("compression");
                isCompressed= compression.equals("gzip");
                if ( !isCompressed ) {
                    throw new IOException( "unsupported compression used: "+compression );
                }
            }
            docNode.setAttribute("compression","none");
            header= StreamDescriptor.createHeader(document).getBytes();
            
            out.write("[00]".getBytes());
            out.write(header);
        } catch ( DasException e ) {
            out.write( getDasExceptionStream(e) );
        }
        
        if ( isCompressed ) {
            in= new java.util.zip.GZIPInputStream(in);            
        }
        
        int ib=0;
        byte[] buf= new byte[2048];
        while (ib!=-1) {
            ib= in.read(buf);
            if (ib>-1) out.write(buf,0, ib);
        }
        
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        InputStream in=null;
        OutputStream out=null;
        if ( args.length>0 ) {
            try {
                in= new FileInputStream(args[0]);
            } catch ( FileNotFoundException ex) {
                System.err.println("Input file not found");
                System.err.println("  file="+args[0]);
                System.exit(-1);
            }
        } else {
            in= System.in;            
        }
        
        if ( args.length>1 ) {
            try {
                out= new FileOutputStream(args[1]);
            } catch ( FileNotFoundException ex) {
            }
            
        } else {
            out= System.out;
        }
        
        try {
            new GUnzipStreamProcessor().process(in,out);            
        } catch ( IOException ex) {
            System.err.println(ex.getMessage());
        }
    }
    
}
