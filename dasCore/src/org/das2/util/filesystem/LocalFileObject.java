/*
 * LocalFileObject.java
 *
 * Created on May 25, 2004, 6:01 PM
 */

package org.das2.util.filesystem;

import org.das2.util.monitor.ProgressMonitor;
import java.io.*;
import org.das2.util.monitor.NullProgressMonitor;

/**
 *
 * @author  Jeremy
 */
public class LocalFileObject extends FileObject {
    
    File localFile;
    File localRoot;
    LocalFileSystem lfs;

    protected LocalFileObject( LocalFileSystem lfs, File localRoot, String filename ) {
        this.lfs= lfs;
        this.localFile= new File( localRoot, filename );
        this.localRoot= localRoot;
    }
        
    public boolean canRead() {
        return localFile.canRead();
    }
    
    public FileObject[] getChildren() {
        File[] files= localFile.listFiles();
        LocalFileObject[] result= new LocalFileObject[files.length];
        for ( int i=0; i<files.length; i++ ) {
            result[i]= new LocalFileObject( lfs, localRoot, lfs.getLocalName(files[i]) );
        }
        return result;
    }
    
    public java.io.InputStream getInputStream( ProgressMonitor monitor ) throws java.io.FileNotFoundException {
        return new FileInputStream( localFile );
    }
    
    public FileObject getParent() {        
        if ( ! localFile.equals(localRoot) ) {
            return new LocalFileObject( lfs, localRoot, lfs.getLocalName( localFile.getParentFile() ) );
        } else {
            return null;
        }
    }
    
    public long getSize() {
        return localFile.length();
    }
    
    public boolean isData() {
        return localFile.isFile();
    }
    
    public boolean isFolder() {
        return localFile.isDirectory();
    }
    
    public boolean isReadOnly() {
        return !localFile.canWrite();
    }
    
    public boolean isRoot() {
        return localFile.getParentFile()==null;
    }
    
    public java.util.Date lastModified() {
        return new java.util.Date(localFile.lastModified());
    }
    
    public boolean exists() {
        return localFile.exists();
    }
    
    public String getNameExt() {
        return FileSystem.toCanonicalFilename( localFile.toString().substring( localRoot.toString().length() ) );
    }
    
    public String toString() {
        return "["+lfs+"]"+getNameExt();
    }
    
    public java.nio.channels.ReadableByteChannel getChannel( ProgressMonitor monitor ) throws FileNotFoundException {
        return ((FileInputStream)getInputStream( monitor )).getChannel();
    }

    public File getFile() throws FileNotFoundException {
        return getFile( new NullProgressMonitor() );
    }

    public File getFile(org.das2.util.monitor.ProgressMonitor monitor) throws FileNotFoundException {
        if ( !localFile.exists() ) throw new FileNotFoundException("file not found: "+localFile);
        return localFile;
    }

    public boolean isLocal() {
        return true;
    }
    
    
}
