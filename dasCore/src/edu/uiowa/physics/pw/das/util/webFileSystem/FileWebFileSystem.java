/*
 * FileWebFileSystem.java
 *
 * Created on May 14, 2004, 1:02 PM
 */

package edu.uiowa.physics.pw.das.util.webFileSystem;

import java.io.*;
import java.net.*;
import java.util.regex.*;

/**
 *
 * @author  Jeremy
 */
public class FileWebFileSystem extends WebFileSystem {
    
    File localRoot;
    
    /** Creates a new instance of FileWebFileSystem */
    protected FileWebFileSystem( URL root ) {
        if ( !("file".equals(root.getProtocol()) ) ) {
            throw new IllegalArgumentException("protocol not file: "+root);
        }
        localRoot= new File( root.getFile() );
        if ( !localRoot.exists() ) {
            throw new IllegalArgumentException( "root does not exist: "+root );
        }
    }
    
    public java.io.File getFile(String filename) {
        return new File( localRoot, filename );
    }
    
    public boolean isDirectory(String filename) {
        return new File( localRoot, filename ).isDirectory();
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
    
}
