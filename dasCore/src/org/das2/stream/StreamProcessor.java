/*
 * StreamProcessor.java
 *
 * Created on December 16, 2003, 10:09 AM
 */

package org.das2.stream;

import org.das2.DasProperties;
import java.io.*;

/**
 *
 * @author  Jeremy
 */
public abstract class StreamProcessor {    
    
    public abstract void process( InputStream in, OutputStream out ) throws IOException ;    

    public InputStream process(final InputStream in) throws IOException {        
        
        final PipedOutputStream out= new PipedOutputStream();
        final PipedInputStream pin= new PipedInputStream(out);
        Runnable r= new Runnable() {
            public void run() {
                try {
                    process( in, out );
                    out.close();
                } catch ( IOException e ) {
                    try {
                        out.write( getDasExceptionStream(e) );                    
                    } catch ( IOException e2 ) {
                        DasProperties.getLogger().severe(e2.toString());    
                        throw new RuntimeException(e2);
                    }
                }
            }
        };
        Thread t= new Thread(r);
        t.start();
        return pin;
        
    }

    public byte[] getDasExceptionStream( Throwable t ) {
        String exceptionString= "[xx]<exception rootCause=\""+t.toString()+"\"/>";
        return exceptionString.getBytes();
    }
}
