/*
 * StripHeader.java
 *
 * Created on December 11, 2003, 12:35 PM
 */

package org.das2.stream;

import org.das2.util.StreamTool;
import java.io.*;

/**
 *
 * @author  Jeremy
 */
public class StripHeader {
        
    public static void stripHeader( InputStream in, OutputStream out ) throws IOException {
        byte[] header= StreamTool.readXML(new PushbackInputStream(in));
        out.write(header);        
    }
        
    public static void main(String[] args) throws IOException {
        stripHeader( System.in, System.out );
    }
    
}
