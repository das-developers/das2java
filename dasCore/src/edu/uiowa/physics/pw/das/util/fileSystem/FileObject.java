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
    
    public boolean canRead();
    
    public FileObject[] getChildren();
    
    public InputStream getInputStream() throws FileNotFoundException ;        
    
    public FileObject getParent();
    
    public long getSize();
    
    public boolean isData();
    
    public boolean isFolder();
    
    public boolean isReadOnly();
    
    public boolean isRoot();        
    
    public java.util.Date lastModified();        
        
}
