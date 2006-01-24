/*
 * WebFile.java
 *
 * Created on May 14, 2004, 10:06 AM
 */

package edu.uiowa.physics.pw.das.util.fileSystem;

import edu.uiowa.physics.pw.das.system.DasLogger;
import edu.uiowa.physics.pw.das.util.DasProgressMonitor;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
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
        return modifiedDate;
    }
    
    /**
     * returns the File that corresponds to the remote file.  This may or may
     * not exist, depending on whether it's been downloaded yet.
     */
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
                // I'm going to assume that it's because the file was not found. 404's from pw's server end up here
                return false;
            }
        }
    }
    
    protected WebFileObject( WebFileSystem wfs, String pathname, Date modifiedDate ) {
        this.localFile= new File( wfs.getLocalRoot(), pathname );
        
        this.localFile.deleteOnExit();
        this.modifiedDate= modifiedDate;
        
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
        try {
            boolean download= false;
            
            Date remoteDate;
            if ( wfs instanceof HttpFileSystem ) {
                URL url= wfs.getURL( this.getNameExt() );
                HttpURLConnection connection= (HttpURLConnection)url.openConnection();
                connection.setRequestMethod("HEAD");
                connection.connect();
                remoteDate= new Date( connection.getLastModified() );
            } else {
                // this is the old logic
                remoteDate= new Date( localFile.lastModified() );
            }
            
            if ( localFile.exists() ) {
                Date localFileLastModified= new Date( localFile.lastModified() );
                if ( remoteDate.after( localFileLastModified ) ) {
                    FileSystem.logger.info("remote file is newer than local copy of "+this.getNameExt()+", download.");
                    download= true;
                }
            } else {
                download= true;
            }
            
            if ( download ) {
                try {                    
                    wfs.transferFile( pathname,localFile, monitor );
                } catch ( FileNotFoundException e ) {
                    throw e;
                }
            }
            
            return localFile;
        } catch ( IOException e ) {
            wfs.handleException( e ) ;
            throw (FileNotFoundException)new FileNotFoundException( e.getMessage() ).initCause(e);
        }
    }
    
}