/*
 * WebFile.java
 *
 * Created on May 14, 2004, 10:06 AM
 */

package edu.uiowa.physics.pw.das.util.fileSystem;

/* this is modelled after the NetBeans fileSystem FileObject, with the
 * mind that we might use it later. */

import java.io.*;
import java.net.*;

/**
 *
 * @author  Jeremy
 */
public interface FileObject {    
    
    /**
     * returns true if the file can be read (see getInputStream)
     * @return
     */    
    public boolean canRead();
    
    /**
     * returns objects within a folder.
     * @return
     */    
    public FileObject[] getChildren();
    
    /**
     * opens an inputStream, perhaps transferring the file to a
     *  cache first.
     * @throws FileNotFoundException
     * @return
     */    
    public InputStream getInputStream() throws FileNotFoundException ;        
    
    /**
     * returns the parent folder of this object.
     * @return
     */    
    public FileObject getParent();
    
    /**
     * returns the size in bytes of the file, and -1 if the size is unknown.
     * @return
     */    
    public long getSize();
    
    /**
     * returns true if the file is a data file that to be used
     *  reading or writing data.
     * @return
     */    
    public boolean isData();
    
    /**
     * returns true if the object is a folder (directory).
     * @return
     */    
    public boolean isFolder();
    
    /**
     * returns true if the file is read-only (redundant)
     * @return
     */    
    public boolean isReadOnly();
    
    /**
     * return true if this is the root of the filesystem it came from.
     * @return
     */    
    public boolean isRoot();        
    
    /**
     * return true if the file exists
     * @return
     */    
    public boolean exists();
        
    /**
     * returns the canonical name of the file within the filesystem.
     * For example /a/b/c.dat.
     * @return
     */    
    public String getNameExt();
    
    /**
     * returns the lastModified date, or new Date(0L) if the date is
     * not available.
     * @return
     */    
    public java.util.Date lastModified();        
        
}
