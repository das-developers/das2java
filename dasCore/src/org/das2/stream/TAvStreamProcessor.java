/*
 * gzip.java
 *
 * Created on July 11, 2003, 9:39 AM
 */

package org.das2.stream;


import java.io.*;


/**
 *
 * @author  jbf
 */
public class TAvStreamProcessor extends StreamProcessor {

    public void process(InputStream in, OutputStream out) throws IOException {
        
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
            new TAvStreamProcessor().process(in,out);
        } catch ( IOException ex) {
            System.err.println(ex.getMessage());
        } 
    }
    
}
