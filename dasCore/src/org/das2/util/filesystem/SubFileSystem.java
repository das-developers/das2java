/*
 * SubFileSystem.java
 *
 * Created on January 16, 2007, 2:19 PM
 *
 *
 */

package org.das2.util.filesystem;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author Jeremy
 */
public class SubFileSystem extends FileSystem {
    FileSystem parent;
    String dir;
    
    protected SubFileSystem( FileSystem parent, String dir ) throws MalformedURLException {
        super( new URL( parent.getRootURL(), dir ) );
        this.parent= parent;
        this.dir= dir;
        
    }
    
    public FileObject getFileObject(String filename) {
        return parent.getFileObject( dir + filename );
    }
    
    public boolean isDirectory(String filename) throws IOException {
        return parent.isDirectory( dir + filename );
    }
    
    public String[] listDirectory(String directory) throws IOException {
        return parent.listDirectory( dir + directory );
    }
    
    public String[] listDirectory(String directory, String regex) throws IOException {
        return parent.listDirectory( dir + directory, regex );
    }
    
    
}
