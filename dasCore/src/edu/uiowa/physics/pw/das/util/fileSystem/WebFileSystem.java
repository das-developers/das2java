/*
 * WebFileSystem.java
 *
 * Created on May 13, 2004, 1:22 PM
 *
 * A WebFileSystem allows web files to be opened just as if they were
 * local files, since it manages the transfer of the file to a local
 * file system.
 */

package edu.uiowa.physics.pw.das.util.fileSystem;

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.util.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import javax.swing.*;

/**
 *
 * @author  Jeremy
 */
public abstract class WebFileSystem extends FileSystem {
    
    final File localRoot;
    
    /** Creates a new instance of WebFileSystem */
    protected WebFileSystem(URL root, File localRoot) {
        super( root );        
        this.localRoot= localRoot;
    }
    
    static protected File localRoot( URL root ) {
        File local;
        if ( System.getProperty("user.name").equals("Web") ) {
            local= new File("/tmp");
        } else {
            local= new File( System.getProperty("user.home") );
        }
        local= new File( local, ".das2/fileSystemCache/WebFileSystem/" );
              
        local= new File( local, root.getProtocol() );
        local= new File( local, root.getHost() );
        local= new File( local, root.getFile() );
        local.mkdirs();
        return local;
    }
    
    /**
     * Transfers the file from the remote store to a local copy f.  This should only be
     * used within the class and subclasses, clients should use getFileObject( String ).getFile().
     */
    abstract void transferFile( String filename, File f, DasProgressMonitor monitor ) throws IOException;
    
    protected File getLocalRoot() {
        return this.localRoot;
    }
    
    abstract public boolean isDirectory( String filename );
    
    abstract public String[] listDirectory( String directory );
    
    public String[] listDirectory( String directory, String regex ) {
        String[] names= listDirectory( directory );
        Pattern pattern= Pattern.compile(regex);
        ArrayList result= new ArrayList();
        for ( int i=0; i<names.length; i++ ) {                        
            if ( pattern.matcher(names[i]).matches() ) {
                result.add(names[i]);
            }
        }
        return (String[])result.toArray(new String[result.size()]);
    }
    
    public URL getURL( String filename ) {
        try {
            filename= FileSystem.toCanonicalFilename(filename);
            return new URL( root+filename.substring(1) );
        } catch ( MalformedURLException e ) {
            throw new RuntimeException(e);
        }
    }
    
    public String getLocalName( File file ) {
        if ( !file.toString().startsWith(localRoot.toString() ) ) {
            throw new IllegalArgumentException( "file \""+file+"\"is not of this web file system" );
        }
        String filename= file.toString().substring(localRoot.toString().length() );
        filename= filename.replaceAll( "\\\\", "/" );
        return filename;
    }
    
    public String getLocalName( URL url ) {
        if ( !url.toString().startsWith(root.toString() ) ) {
            throw new IllegalArgumentException( "url \""+url+"\"is not of this web file system" );
        }
        String filename= FileSystem.toCanonicalFilename( url.toString().substring(root.toString().length() ) );
        return filename;
    }
    
    
    public FileObject getFileObject( String filename ) {        
        WebFileObject f= new WebFileObject( this, filename, new Date(System.currentTimeMillis()) );
        return f;
    }
    
    /**
     * copies data from in to out, sending the number of bytesTransferred to the monitor.
     */
    protected void copyStream( InputStream is, OutputStream out, DasProgressMonitor monitor ) throws IOException {        
        byte[] buffer= new byte[2048];
        int bytesRead= is.read( buffer, 0, 2048 );
        long totalBytesRead= bytesRead;
        while ( bytesRead>-1 ) {
            if ( monitor.isCancelled() ) throw new IOException( "operation cancelled" );
            monitor.setTaskProgress( totalBytesRead );            
            out.write( buffer, 0, bytesRead );            
            bytesRead= is.read( buffer, 0, 2048 );         
            totalBytesRead+= bytesRead;
        }
    }
    
    public String toString() {
        return "wfs "+root;
    }
    
}
