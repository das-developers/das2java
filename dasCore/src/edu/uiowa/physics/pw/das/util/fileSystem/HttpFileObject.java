/*
 * WebFile.java
 *
 * Created on May 14, 2004, 10:06 AM
 */

package edu.uiowa.physics.pw.das.util.fileSystem;

import java.io.*;
import java.net.*;

/**
 *
 * @author  Jeremy
 */
public class HttpFileObject implements FileObject {
    
    HttpFileSystem wfs;
    String pathname;
    File localFile;
    
    boolean isRoot;
    boolean isFolder;
    
    public boolean canRead() {
        return true;
    }
    
    public FileObject[] getChildren() {
        if ( !isFolder ) {
            throw new IllegalArgumentException("is not a folder");
        }
        try {
            URL[] list= HtmlUtil.getDirectoryListing( new URL( wfs.getRoot().toString()+pathname ) );
            if ( list.length>100 ) {
                throw new IllegalStateException( "URL list is very long, refusing to transfer" );
            }
            FileObject[] result= new FileObject[list.length];
            for ( int i=0; i<list.length; i++ ) {
                URL url= list[i];
                String localName= wfs.getLocalName(url);
                result[i]= new HttpFileObject( wfs, localName );
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
        return new FileInputStream( localFile );
    }
    
    public FileObject getParent() {
        return new HttpFileObject( wfs, wfs.getLocalName( localFile.getParentFile() ) );
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
        return new java.util.Date(localFile.lastModified());
    }
    
    protected File getLocalFile() {
        return this.localFile;
    }
    
    public boolean exists() {
        return localFile.exists();
    }
    
    protected HttpFileObject( HttpFileSystem wfs, String pathname ) {
        this.localFile= new File( wfs.getLocalRoot(), pathname );
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
                try {
                    wfs.transferFile(pathname,localFile);
                } catch ( IOException e ) {
                    wfs.handleException(e);
                }
            }
        } else {
            
        }
        
        
    }
}