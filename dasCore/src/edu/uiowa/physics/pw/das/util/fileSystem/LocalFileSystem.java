/*
 * FileWebFileSystem.java
 *
 * Created on May 14, 2004, 1:02 PM
 */

package edu.uiowa.physics.pw.das.util.fileSystem;

import java.io.*;
import java.net.*;
import java.util.regex.*;

/**
 *
 * @author  Jeremy
 */
public class LocalFileSystem extends FileSystem {
    
    File localRoot;
        
    protected LocalFileSystem(URL root) {
        if ( !("file".equals(root.getProtocol()) ) ) {
            throw new IllegalArgumentException("protocol not file: "+root);
        }
        localRoot= new File( root.getFile() );
        if ( !localRoot.exists() ) {
            throw new IllegalArgumentException( "root does not exist: "+root );
        }
    }
    
    public FileObject getFile(String filename) {
        return new LocalFileObject( this, localRoot, filename );
    }
    
    public boolean isDirectory(String filename) {
        return new File( localRoot, filename ).isDirectory();
    }
    
    String getLocalName( File file ) {
        if ( !file.toString().startsWith(localRoot.toString() ) ) {
            throw new IllegalArgumentException( "file \""+file+"\"is not of this web file system" );
        }
        String filename= file.toString().substring(localRoot.toString().length() );
        filename= filename.replaceAll( "\\\\", "/" );
        return filename;
    }
    
    public String[] listDirectory(String directory) {
        File f= new File( localRoot, directory );
        return f.list();
    }
    
    public String[] listDirectory(String directory, String regex ) {
        File f= new File( localRoot, directory );
        final Pattern pattern= Pattern.compile(regex);
        return f.list( new FilenameFilter() {
            public boolean accept( File file, String name ) {
                return pattern.matcher(name).matches();
            }
        });
    }
    
    public String toString() {
        return "lfs "+localRoot;
    }
    
}
