/*
 * WebFile.java
 *
 * Created on May 14, 2004, 10:06 AM
 */

package edu.uiowa.physics.pw.das.util.fileSystem;

/* this is modelled after the NetBeans fileSystem FileObject, with the
 * mind that we might use it later. */

import edu.uiowa.physics.pw.das.util.DasProgressMonitor;
import java.io.*;
import java.nio.channels.*;

/**
 *
 * @author  Jeremy
 */
public abstract class FileObject {    
    
    /**
     * returns true if the file can be read (see getInputStream)
     * @return
     */    
    public abstract boolean canRead();
    
    /**
     * returns objects within a folder.
     * @return
     */    
    public abstract FileObject[] getChildren();
    
    /**
     * opens an inputStream, perhaps transferring the file to a
     *  cache first.
     * @throws FileNotFoundException
     * @return
     */        
    public abstract InputStream getInputStream( DasProgressMonitor monitor ) throws FileNotFoundException;
    public InputStream getInputStream() throws FileNotFoundException {
        return getInputStream( DasProgressMonitor.NULL );
    }
    
   /**
     * opens a Channel, perhaps transferring the file to a
     *  cache first.
     * @throws FileNotFoundException
     * @return
     */    
    public abstract Channel getChannel( DasProgressMonitor monitor ) throws FileNotFoundException;
    
    public Channel getChannel() throws FileNotFoundException {
        return getChannel( DasProgressMonitor.NULL );
    }

    /**
     * returns a reference to a File that can be opened.
     */
    public abstract File getFile( DasProgressMonitor monitor ) throws FileNotFoundException ;
    public File getFile() throws FileNotFoundException {
        return getFile( DasProgressMonitor.NULL );
    }   
    
    
    /**
     * returns the parent folder of this object.
     * @return
     */    
    public abstract FileObject getParent();
    
    /**
     * returns the size in bytes of the file, and -1 if the size is unknown.
     * @return
     */    
    public abstract long getSize();
    
    /**
     * returns true if the file is a data file that to be used
     *  reading or writing data.
     * @return
     */    
    public abstract boolean isData();
    
    /**
     * returns true if the object is a folder (directory).
     * @return
     */    
    public abstract boolean isFolder();
    
    /**
     * returns true if the file is read-only (redundant)
     * @return
     */    
    public abstract boolean isReadOnly();
    
    /**
     * return true if this is the root of the filesystem it came from.
     * @return
     */    
    public abstract boolean isRoot();        
    
    /**
     * return true if the file exists
     * @return
     */    
    public abstract boolean exists();
        
    /**
     * returns the canonical name of the file within the filesystem.
     * For example /a/b/c.dat.
     * @return
     */    
    public abstract String getNameExt();
    
    /**
     * returns the lastModified date, or new Date(0L) if the date is
     * not available.
     * @return
     */    
    public abstract java.util.Date lastModified();        
        
}
