/*
 * WebFile.java
 *
 * Created on May 14, 2004, 10:06 AM
 */

package edu.uiowa.physics.pw.das.util.fileSystem;

import edu.uiowa.physics.pw.das.system.DasLogger;
import edu.uiowa.physics.pw.das.util.DasProgressMonitor;
import java.io.*;
import java.util.*;

/**
 *
 * @author  Jeremy
 *
 * This is a refactoring of the HttpFileObject, generalized for use with FTP and HTTP file objects.  Note that
 * the HttpFileObject has not been refactored to use this.
 *
 */
public class WebFileObject extends FileObject {
    
    WebFileSystem wfs;
    String pathname;
    File localFile;
    Date modifiedDate;
    
    boolean isRoot;
    boolean isFolder;
    
    public boolean canRead() {
        return true;
    }
    
    public FileObject[] getChildren() {
        if ( !isFolder ) {
            throw new IllegalArgumentException(toString()+"is not a folder");
        }
        String[] list= wfs.listDirectory( pathname );
        FileObject[] result= new FileObject[list.length];
        for ( int i=0; i<result.length; i++ ) {
            result[i]= new WebFileObject( wfs, list[i], new Date(System.currentTimeMillis() ) );
        }
        return result;
    }
    
    public InputStream getInputStream( DasProgressMonitor monitor ) throws FileNotFoundException {
        if ( isFolder ) {
            throw new IllegalArgumentException( "is a folder" );
        }
        if ( !localFile.exists() ) {
            try {
                wfs.transferFile( pathname,localFile,monitor );
            } catch ( FileNotFoundException e ) {
                throw e;
            } catch ( IOException e ) {
                wfs.handleException(e);
            }
        }
        return new FileInputStream( localFile );
    }
    
    public FileObject getParent() {
        return new WebFileObject( wfs, wfs.getLocalName( localFile.getParentFile() ), new Date(System.currentTimeMillis()) );
    }
    
    public long getSize() {
        if ( isFolder ) {
            throw new IllegalArgumentException("is a folder");
        }
        return localFile.length();
    }
    
    public boolean isData() {
        return !this.isFolder;
    }
    
    public boolean isFolder() {
        return this.isFolder;
    }
    
    public boolean isReadOnly() {
        return true;
    }
    
    public boolean isRoot() {
        return this.isRoot;
    }
    
    public java.util.Date lastModified() {
        return new java.util.Date(System.currentTimeMillis());
    }
    
    protected File getLocalFile() {
        return this.localFile;
    }
    
    public boolean exists() {
        if ( localFile.exists() ) {
            return true;
        } else {
            try {
                DasLogger.getLogger( DasLogger.DATA_TRANSFER_LOG ).info("This implementation of WebFileObject.exists() is not optimal");
                wfs.transferFile( pathname,localFile, DasProgressMonitor.NULL );
                return localFile.exists();
            } catch ( FileNotFoundException e ) {
                return false;
            } catch ( IOException e ) {
                wfs.handleException(e);
                return false;
            }
        }
    }
    
    protected WebFileObject( WebFileSystem wfs, String pathname, Date modifiedDate ) {
        this.localFile= new File( wfs.getLocalRoot(), pathname );
        
        // until we can check for file dates on the server, don't keep cached copies around
        this.localFile.deleteOnExit();
        
        this.wfs= wfs;
        this.pathname= pathname;
        if ( !localFile.canRead() ) {
            if ( wfs.isDirectory( pathname ) ) {
                localFile.mkdirs();
                this.isFolder= true;
                if ( "".equals(pathname) ) {
                    this.isRoot= true;
                }
            } else {
                this.isFolder= false;
            }
        } else {
            this.isFolder= localFile.isDirectory();
        }
        
    }
    
    public String toString() {
        return "["+wfs+"]"+getNameExt();
    }
    
    public String getNameExt() {
        return pathname;
    }
    
    public java.nio.channels.Channel getChannel( DasProgressMonitor monitor ) throws FileNotFoundException {
        return ((FileInputStream)getInputStream( monitor )).getChannel();
    }
    
    public File getFile( DasProgressMonitor monitor ) throws FileNotFoundException {
        if ( !localFile.exists() ) {
            try {
                wfs.transferFile( pathname,localFile, monitor );
            } catch ( FileNotFoundException e ) {
                throw e;
            } catch ( IOException e ) {
                wfs.handleException(e);
            }
        }
        return localFile;
    }
        
}