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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.util.monitor.ProgressMonitor;
import java.io.*;
import java.util.zip.GZIPInputStream;
import org.das2.util.monitor.NullProgressMonitor;

/**
 *
 * @author  Jeremy
 */
public class LocalFileObject extends FileObject {
    
    File localFile;
    File localGzFile;
    File localRoot;
    LocalFileSystem lfs;

    protected LocalFileObject( LocalFileSystem lfs, File localRoot, String filename ) {
        this.lfs= lfs;
        this.localFile= new File( localRoot, filename );
        this.localGzFile= new File( localRoot, filename+".gz" );
        this.localRoot= localRoot;
    }
        
    public boolean canRead() {
        return localFile.canRead();
    }
    
    public FileObject[] getChildren() {
        File[] files= localFile.listFiles();
        LocalFileObject[] result= new LocalFileObject[files.length];
        for ( int i=0; i<files.length; i++ ) {
            if ( ! files[i].isHidden() ) {
                result[i]= new LocalFileObject( lfs, localRoot, lfs.getLocalName(files[i]) );
            }
        }
        return result;
    }
    
    public java.io.InputStream getInputStream( ProgressMonitor monitor ) throws IOException {
        if ( !localFile.exists() && localGzFile.exists() ) {
            return new GZIPInputStream( new FileInputStream( localGzFile ) );
        } else {
            return new FileInputStream( localFile );
        }
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
        if ( !localFile.exists() && localGzFile.exists() ) {
            return true;
        } else {
            return localFile.isFile();
        }
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
        return localFile.exists() || localGzFile.exists();
    }
    
    public String getNameExt() {
        return FileSystem.toCanonicalFilename( localFile.toString().substring( localRoot.toString().length() ) );
    }
    
    public String toString() {
        return "["+lfs+"]"+getNameExt();
    }
    
    public java.nio.channels.ReadableByteChannel getChannel( ProgressMonitor monitor ) throws IOException {
        return ((FileInputStream)getInputStream( monitor )).getChannel();
    }

    public File getFile() throws FileNotFoundException {
        return getFile( new NullProgressMonitor() );
    }

    /**
     * This will generally return the local file object directly, but may 
     * return the name of a temporary file where the data was gunzipped.
     * 
     * @param monitor progress monitor
     * @return the local file, or a temporary file where the data was gunzipped.
     * @throws FileNotFoundException 
     */
    @Override
    public File getFile(org.das2.util.monitor.ProgressMonitor monitor) throws FileNotFoundException {
        if ( !localFile.exists() ) {
            if ( localGzFile.exists() ) {
                File tempFile= FileSystemUtil.createTempFile( localFile, FileSystem.settings().getTemporaryFileTimeoutSeconds() );
                try {
                    if ( tempFile.exists() && tempFile.lastModified()>localGzFile.lastModified() ) {
                        return tempFile;
                    } else {
                        synchronized ( LocalFileObject.class ) {
                            if ( !tempFile.getParentFile().exists() && !tempFile.getParentFile().mkdirs() ) {
                                throw new FileNotFoundException("unable to create parent directories: "+tempFile );
                            }
                        }
                        FileSystemUtil.gunzip( localGzFile, tempFile );
                        tempFile.deleteOnExit(); //TODO: verify this on all platforms.
                        return tempFile;
                    }
                } catch (FileNotFoundException ex ) {
                    throw ex; //cheesy
                } catch (IOException ex) {
                    throw new FileNotFoundException("unable to gunzip: "+localGzFile+", "+ex.toString() );
                }
            } else {
                throw new FileNotFoundException("file not found: "+localFile);
            }
        }
        return localFile;
    }

    public boolean isLocal() {
        return true;
    }

    @Override
    public <T> T getCapability(Class<T> clazz) {
        if ( clazz==WriteCapability.class ) {
            return (T) new WriteCapability() {
                public OutputStream getOutputStream() throws FileNotFoundException {
                    return new FileOutputStream(localFile);
                }
                public boolean canWrite() {
                    return localFile.canWrite();
                }
                public boolean delete() {
                    return localFile.delete();
                }
            };
        } else {
            return super.getCapability(clazz);
        }
    }
    
    
}
