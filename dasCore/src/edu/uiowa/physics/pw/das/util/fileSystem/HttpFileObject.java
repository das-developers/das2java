/*
 * WebFile.java
 *
 * Created on May 14, 2004, 10:06 AM
 */

package edu.uiowa.physics.pw.das.util.fileSystem;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 *
 * @author  Jeremy
 */
public class HttpFileObject implements FileObject {
    
    HttpFileSystem wfs;
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
        try {
            URL[] list= HtmlUtil.getDirectoryListing( new URL( wfs.getRoot().toString()+pathname ) );
            FileObject[] result= new FileObject[list.length];
            for ( int i=0; i<list.length; i++ ) {
                URL url= list[i];
                String localName= wfs.getLocalName(url);
                result[i]= new HttpFileObject( wfs, localName, new Date(System.currentTimeMillis()) );
            }
            return result;
        } catch ( MalformedURLException e ) {
            wfs.handleException(e);
            return new FileObject[0];
        } catch ( IOException e ) {
            wfs.handleException(e);
            return new FileObject[0];
        }
    }
    
    public InputStream getInputStream() throws FileNotFoundException {
        if ( isFolder ) {
            throw new IllegalArgumentException( "is a folder" );
        }
        if ( !localFile.exists() ) {
            try {
                wfs.transferFile(pathname,localFile);
            } catch ( FileNotFoundException e ) {
                throw e;
            } catch ( IOException e ) {
                wfs.handleException(e);
            }
        }
        return new FileInputStream( localFile );
    }
    
    public FileObject getParent() {
        return new HttpFileObject( wfs, wfs.getLocalName( localFile.getParentFile() ), new Date(System.currentTimeMillis()) );
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
                wfs.transferFile(pathname,localFile);
                return localFile.exists();
            } catch ( FileNotFoundException e ) {                
                return false;
            } catch ( IOException e ) {
                wfs.handleException(e);
                return false;
            }
        }
    }
    
    protected HttpFileObject( HttpFileSystem wfs, String pathname, Date modifiedDate ) {
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
    
    public java.nio.channels.Channel getChannel() throws FileNotFoundException {
        return ((FileInputStream)getInputStream()).getChannel();
    }
    
}