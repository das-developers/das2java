/*
 * SubFileSystem.java
 *
 * Created on January 16, 2007, 2:19 PM
 *
 *
 */

package edu.uiowa.physics.pw.das.util.fileSystem;

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
    
    public boolean isDirectory(String filename) {
        return parent.isDirectory( dir + filename );
    }
    
    public String[] listDirectory(String directory) {
        return parent.listDirectory( dir + directory );
    }
    
    public String[] listDirectory(String directory, String regex) {
        return parent.listDirectory( dir + directory, regex );
    }
    
    
}
