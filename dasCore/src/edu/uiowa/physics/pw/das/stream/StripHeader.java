/*
 * StripHeader.java
 *
 * Created on December 11, 2003, 12:35 PM
 */

package edu.uiowa.physics.pw.das.stream;

import java.io.*;

/**
 *
 * @author  Jeremy
 */
public class StripHeader {
        
    public static void stripHeader( InputStream in, OutputStream out ) throws IOException {
        byte[] header= StreamDescriptor.readHeader(in);
        out.write(header);        
    }
        
    public static void main(String[] args) throws IOException {
        stripHeader( System.in, System.out );
    }
    
}
