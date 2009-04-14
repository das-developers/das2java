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
 * WebFile.java
 *
 * Created on May 14, 2004, 10:06 AM
 */

package org.das2.util.filesystem;

import org.das2.util.monitor.ProgressMonitor;
import org.das2.util.monitor.NullProgressMonitor;
import java.io.*;
import java.nio.channels.*;

/**
 * <p>Class for describing and accessing files in file systems.  This 
 * is similar to java.io.File, except that it can describe files on remote
 * file systems like an ftp site.</p>
 *
 * <p>Note: this is modelled after the NetBeans fileSystem FileObject, with the
 * thought that we might use it later. </p>
 * 
 * <p>TODO: investigate using javax.tools.FileObject.
 *
 * @author Jeremy
 */
public abstract class FileObject {
    
    /**
     * returns true if the file can be read by the client.
     * @return true if the file can be read (see getInputStream)
     */
    public abstract boolean canRead();
    
    /**
     * returns objects within a folder.
     * @return an array of all FileObjects with the folder.
     */
    public abstract FileObject[] getChildren() throws IOException;
    
    /**
     * opens an inputStream, perhaps transferring the file to a
     *  cache first.
     * @param monitor for monitoring the download.  The monitor won't be used when the access
     *   is immediate, for example with local FileObjects.
     * @throws FileNotFoundException if the file doesn't exist.
     * @throws IOException
     * @return an InputStream
     */
    public abstract InputStream getInputStream( ProgressMonitor monitor ) throws FileNotFoundException, IOException;
    
    /**
     * opens an inputStream, perhaps transferring the file to a
     *  cache first.  Note no monitor is used but this may block at sub-interactive
     *  time until the file is downloaded.
     *
     * @throws FileNotFoundException if the file doesn't exist.
     * @throws IOException
     * @return an InputStream
     */    
    public InputStream getInputStream() throws FileNotFoundException, IOException {
        return getInputStream( new NullProgressMonitor() );
    }
    
    /**
     * opens a Channel, perhaps transferring the file to a local cache first.  monitor
     * is used to monitor the download.
     * @param monitor for monitoring the download.  The monitor won't be used when the access
     *   is immediate, for example with local FileObjects.
     * @throws FileNotFoundException if the file doesn't exist.
     * @throws IOException
     * @return a java.nio.channels.Channel for fast IO reads.
     */
    public abstract ReadableByteChannel getChannel( ProgressMonitor monitor ) throws FileNotFoundException, IOException;
    
    /**
     * opens a Channel, but without a monitor.  Note this may block at sub-interactive time
     * if the FileObject needs to be downloaded before access.
     * @throws FileNotFoundException if the file doesn't exist.
     * @throws IOException
     * @return a java.nio.channels.Channel for fast IO reads.
     */
    public ReadableByteChannel getChannel() throws FileNotFoundException, IOException {
        return getChannel( new NullProgressMonitor() );
    }
    
    /**
     * gets a File object that can be opened by the client.  This may download a remote file, so
     * a progress monitor can be used to monitor the download.
     * @return a reference to a File that can be opened.
     * @param monitor for monitoring the download.  The monitor won't be used when the access
     *   is immediate, for example with local FileObjects.
     * @throws java.io.FileNotFoundException if the file doesn't exist.
     * @throws IOException if the file cannot be made local
     * @throws NullPointerException if the monitor is null.
     */
    public abstract File getFile( ProgressMonitor monitor ) throws FileNotFoundException, IOException;
    
    /**
     * gets a File object that can be opened by the client.  Note this may block at sub-interactive time
     * if the remote file needs to be downloaded before access.
     * @return a reference to a File that can be opened.
     * @throws java.io.FileNotFoundException if the file doesn't exist.
     */
    public File getFile() throws FileNotFoundException, IOException {
        return getFile( new NullProgressMonitor() );
    }
    
    /**
     * returns the parent FileObject (a folder).
     * If the fileObject is root, then null should be returned.
     * @return the parent folder of this object.
     */
    public abstract FileObject getParent();
    
    /**
     * returns the size of the file.
     * @return the size in bytes of the file, and -1 if the size is unknown.
     */
    public abstract long getSize();
    
    /**
     * returns true if the file is a data file that to be used
     *  reading or writing data. (And not a folder.)
     * @return true if the file is a data file
     */
    public abstract boolean isData();
    
    /**
     * indicates the type of FileObject
     * @return true if the object is a folder (directory).
     */
    public abstract boolean isFolder();
    
    /**
     * true is the file is read-only.
     * @return true if the file is read-only
     */
    public abstract boolean isReadOnly();
    
    /**
     * returns true if this is the root of the filesystem it came from.
     * @return true if this is the root of the filesystem it came from.
     */
    public abstract boolean isRoot();
    
    /**
     * returns true if the file is locally available, meaning clients can 
     * call getFile() and the readble File reference will be available in
     * interactive time.  Note that isLocal does not imply exists().  Also,
     * This may result in side effects such as a website hit.
     */
    public abstract boolean isLocal();
    
    /**
     * returns true if the file exists.  This may have the side effect of 
     * downloading the file.
     * @return true if the file exists
     */
    public abstract boolean exists();
    
    /**
     * returns the canonical name of the file within the filesystem.  For example,
     * in the local filesystem /mnt/data/steven/, /mnt/data/steven/jan/01.dat
     * would be /jan/01.dat.
     *
     * For example, /a/b/c.dat.
     * @return the name of the file within the FileSystem.
     */
    public abstract String getNameExt();
    
    /**
     * returns the Date when the file was last modified.  or new Date(0L) if the date is
     * not available.
     *
     * @return the last modified Date, or new Date(0) if it is not available.
     */
    public abstract java.util.Date lastModified();
    
}
