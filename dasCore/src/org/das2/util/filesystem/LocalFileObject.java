/* Copyright (C) 2003-2008 The University of Iowa 
 *
 * This file is part of the Das2 <www.das2.org> utilities library.
 *
 * Das2 utilities are free software: you can redistribute and/or modify them
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Das2 utilities are distributed in the hope that they will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * as well as the GNU General Public License along with Das2 utilities.  If
 * not, see <http://www.gnu.org/licenses/>.
 *
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
