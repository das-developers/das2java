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

import java.awt.EventQueue;
import java.util.logging.Level;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.util.monitor.NullProgressMonitor;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.util.*;
import java.util.logging.Logger;
import org.das2.util.Base64;
import org.das2.util.filesystem.FileSystem.DirectoryEntry;
import org.das2.util.filesystem.FileSystem.FileSystemOfflineException;
import org.das2.util.monitor.CancelledOperationException;

/**
 *
 * @author  Jeremy
 *
 * This is a refactoring of the HttpFileObject, generalized for use with FTP and HTTP file objects.  Note that
 * the HttpFileObject has not been refactored to use this.
 *
 */
public class WebFileObject extends FileObject {

    private static final Logger logger= org.das2.util.LoggerManager.getLogger("das2.filesystem");
    
    WebFileSystem wfs;
    String pathname;
    File localFile;
    Date modifiedDate;
    boolean isRoot;
    boolean isFolder;
    Map<String,String> metadata;
    
    /**
     * true if we know if it's a folder or not.
     */
    boolean isFolderResolved = false;

    public boolean canRead() {
        return true;
    }

    /**
     * load the metadata for the file object, presently only for HTTP doing a HEAD request.
     * This is nasty and needs to be rewritten...
     * For thread safety, metadata should only be read by other threads, and only if it exists.
     * @throws IOException
     */
    protected synchronized void maybeLoadMetadata() throws IOException {
        if ( metadata==null ) {
            if ( this.wfs.offline ) {
                metadata= new HashMap();
                metadata.put( WebProtocol.META_EXIST, isLocal() ? "true" : "false" );
            } else {
                if ( wfs.protocol!=null ) {
                    metadata= wfs.protocol.getMetadata( this );
                }
            }
        }
    }
    
    public FileObject[] getChildren() throws IOException {
        if (!isFolder) {
            throw new IllegalArgumentException(toString() + "is not a folder");
        }
        String[] list = wfs.listDirectory(pathname);
        FileObject[] result = new FileObject[list.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = new WebFileObject(wfs, list[i], new Date(System.currentTimeMillis()));
        }
        return result;
    }

    public InputStream getInputStream(ProgressMonitor monitor) throws FileNotFoundException, IOException {
        if ( wfs.protocol !=null && !this.wfs.offline ) {
            logger.log(Level.FINE, "get inputstream from {0}", wfs.protocol);
            return wfs.protocol.getInputStream(this, monitor);
        }
        if (isFolder) {
            throw new IllegalArgumentException("is a folder");
        }

        if ( this.modifiedDate.getTime()==0 ) {
            lastModified(); // trigger load of the modifiedDate
        }
        if ( !localFile.exists() || ( this.modifiedDate.getTime()-localFile.lastModified() > 10 ) ) { //TODO: test me!
            File partFile = new File(localFile.toString() + ".part");
            wfs.downloadFile(pathname, localFile, partFile, monitor);
        }
        logger.log( Level.FINE, "read local file {0}", localFile);
        return new FileInputStream(localFile);
    }

    /**
     * 
     * @return a WebFileObject referencing the parent directory.
     */
    public FileObject getParent() {
        return new WebFileObject(wfs, wfs.getLocalName(localFile.getParentFile()), new Date(System.currentTimeMillis()));
    }

    public long getSize() {
        if (isFolder) {
            throw new IllegalArgumentException("is a folder");
        }
        try {
            maybeLoadMetadata();
        } catch ( IOException ex ) {
            logger.log(Level.INFO, "unable to load metadata: {0}", ex);
            return localFile.length();
        }
        if ( metadata.containsKey("Content-Length") ) {
            return Long.parseLong(metadata.get("Content-Length") );
        } else {
            return localFile.length();
        }
    }

    public boolean isData() {
        return !this.isFolder;
    }

    public boolean isFolder() {
        if ( this.isFolderResolved ) {        
            return this.isFolder;
        } else {    
            //TODO: make HttpFileObject that does HEAD requests to properly answer these questions.  See HttpFileSystem.getHeadMeta()
            throw new RuntimeException("IOException in constructor prevented us from resolving");
        }
    }

    public boolean isReadOnly() {
        return true;
    }

    public boolean isRoot() {
        return this.isRoot;
    }

    public java.util.Date lastModified() {
        if ( modifiedDate.getTime()==Long.MAX_VALUE ) {
            try {
                maybeLoadMetadata();
            } catch ( IOException ex ) {
                logger.log(Level.INFO, "unable to load metadata: {0}", ex);
                modifiedDate= new Date( localFile.lastModified() );
            }
            if ( metadata.containsKey("Last-Modified") ) {
                long date= Date.parse( metadata.get("Last-Modified") );
                modifiedDate= new Date( date );
            } else {
                logger.info("metadata doesn't contain Last-Modified, using localFile" );
                modifiedDate= new Date( localFile.lastModified() );
            }
        }
        return modifiedDate;
    }

    /**
     * allow subclasses, such as FtpBeanFileSystem, to delay loading of the date.
     * @param d
     */
    protected void setLastModified( Date d ) {
        if ( this.modifiedDate.getTime()==0 || this.modifiedDate.getTime()==Long.MAX_VALUE ) {
            this.modifiedDate= d;
        } else {
            throw new IllegalArgumentException("valid date cannot be modified");
        }
    }

    /**
     * returns the File that corresponds to the remote file.  This may or may
     * not exist, depending on whether it's been downloaded yet.
     */
    protected File getLocalFile() {
        return this.localFile;
    }

    public boolean exists() {
        if ( localFile!=null && localFile.exists()) { // applet support
            return true;
        } else {
            try {
                if ( wfs.protocol!=null ) {
                    maybeLoadMetadata();
                    return "true".equals( metadata.get( WebProtocol.META_EXIST ) );
                } else {
                    // TODO: use HTTP HEAD, etc
                    logger.info("This implementation of WebFileObject.exists() is not optimal");
                    File partFile = new File(localFile.toString() + ".part");
                    wfs.downloadFile(pathname, localFile, partFile, new NullProgressMonitor());
                    return localFile.exists();
                }
            } catch (FileNotFoundException e) {
                return false;
            } catch (IOException e) {
                // I'm going to assume that it's because the file was not found. 404's from pw's server end up here
                return false;
            }
        }
    }

    protected WebFileObject( WebFileSystem wfs, String pathname, Date modifiedDate ) {

        this.modifiedDate = modifiedDate;

        this.wfs = wfs;
        this.pathname = pathname;
        this.isFolderResolved = false;
        
        if ( ! wfs.isAppletMode() ) {
            this.localFile = new File(wfs.getLocalRoot(), pathname);

            if ( FileSystem.settings().getPersistence()==FileSystemSettings.Persistence.SESSION ) this.localFile.deleteOnExit();

            try {
                if (!localFile.canRead()) {
                    if ( !( pathname.endsWith(".zip") || pathname.endsWith(".ZIP") ) && wfs.isDirectory(pathname) ) {  // klugde, see https://sourceforge.net/tracker/index.php?func=detail&aid=3049303&group_id=199733&atid=970682
                        FileSystemUtil.maybeMkdirs(localFile);
                        this.isFolder = true;
                        if ("".equals(pathname)) {
                            this.isRoot = true;
                        }
                    } else {
                        this.isFolder = false;
                    }
                } else {
                    this.isFolder = localFile.isDirectory();
                }
                this.isFolderResolved= true;
            } catch (IOException ex) {
                logger.log(Level.SEVERE,"unable construct web file object",ex);
                this.isFolderResolved = false;
            }
        }
    }

    public String toString() {
        return "[" + wfs + "]" + getNameExt();
    }

    public String getNameExt() {
        return pathname;
    }

    /**
     * return a Channel for the resource.  If the resource can be made locally available, a FileChannel is returned.
     * @param monitor
     * @return
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    public java.nio.channels.ReadableByteChannel getChannel(ProgressMonitor monitor) throws FileNotFoundException, IOException {
        InputStream in= getInputStream(monitor);
        return Channels.newChannel(in);
    }

    public File getFile(ProgressMonitor monitor) throws FileNotFoundException, IOException {
        
        if ( wfs.isAppletMode() ) throw new SecurityException("getFile cannot be used with applets.");

        //bugfix http://sourceforge.net/tracker/?func=detail&aid=3155917&group_id=199733&atid=970682:
        // calling EventQueue.isDispatchThread() starts the event thread, causing problems when called from RSI's IDL.
        if ( false ) {
            if ( EventQueue.isDispatchThread() ) {
                logger.log(Level.SEVERE, "download on event thread! {0}", this.getNameExt());
            }
        }

        boolean download = false;

        if ( monitor==null ) throw new NullPointerException("monitor may not be null");
        Date remoteDate;
        if (wfs instanceof HttpFileSystem && !wfs.isOffline() ) {
            URL url = wfs.getURL(this.getNameExt());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");

            String userInfo= null;

            try {
                userInfo = KeyChain.getDefault().getUserInfo( url );
            } catch (CancelledOperationException ex) {
                throw new FileSystemOfflineException("user cancelled credentials");
            }
            
            if ( userInfo != null) {
                String encode = Base64.encodeBytes( userInfo.getBytes());
                connection.setRequestProperty("Authorization", "Basic " + encode);
            }


            try {
                connection.connect();
                remoteDate = new Date(connection.getLastModified());
            } catch ( IOException ex ) {
                if ( !((HttpFileSystem)wfs).isOffline() ) {
                    throw ex;
                } else {
                    remoteDate= new Date(0);
                }
            }
        } else {
            if ( this.lastModified().getTime()==0 || this.lastModified().getTime()==Long.MAX_VALUE ) {
                DirectoryEntry result= wfs.maybeUpdateDirectoryEntry( this.getNameExt(), true ); // trigger load of the modifiedDate
                if ( result==null ) {
                    logger.fine("file does not exist on remote filesystem"); 
                } else {
                    result= wfs.maybeUpdateDirectoryEntry( this.getNameExt(), true );
                    this.setLastModified( new Date( result.modified ) );
                }
            }

            remoteDate = this.lastModified();
        }

        if (localFile.exists()) {
            Date localFileLastModified = new Date(localFile.lastModified()); // TODO: I think this is a bug...
            if (remoteDate.after(localFileLastModified)) {
                logger.log(Level.INFO, "remote file is newer than local copy of {0}, download.", this.getNameExt());
                download = true;
            }
        } else {
            download = true;
        }

        //check readonly cache for file.
        if ( download && this.wfs.getReadOnlyCache()!=null ) {
            File cacheFile= new File( this.wfs.getReadOnlyCache(), this.getNameExt() );
            if ( cacheFile.exists() ) {
                logger.log(Level.FINE, "using file from ro_cache: {0}", this.getNameExt());
                return cacheFile;
            }
        }

        if (download) {
            try {
                logger.log(Level.FINE, "downloading file {0}", getNameExt());
                if (!localFile.getParentFile().exists()) {
                    FileSystemUtil.maybeMkdirs( localFile.getParentFile() );
                }
                File partFile = new File(localFile.toString() + ".part");
                wfs.downloadFile(pathname, localFile, partFile, monitor);

                if ( !localFile.setLastModified(remoteDate.getTime()) ) {
                    logger.log(Level.INFO, "unable to modify date of {0}", localFile);
                }

                logger.log(Level.FINE, "downloaded local file has date {0}", new Date(localFile.lastModified()));

            } catch (FileNotFoundException e) {
                // TODO: do something with part file.
                throw e;
            } catch (IOException ex ) {
                if ( ex.getMessage()!=null && ex.getMessage().contains("Forbidden") ) {
                    throw ex;
                }
                if ( this.wfs instanceof HttpFileSystem && !(ex instanceof InterruptedIOException )  ) { //TODO: when would we use this--it needs to be more precise.
                    if ( this.wfs.isOffline() ) {
                        logger.log(Level.SEVERE,"unable getFile",ex);
                        throw new FileSystem.FileSystemOfflineException("not found in local cache: "+getNameExt() );
                    }
                }
                throw ex;
            } finally {
                monitor.finished();
            }
        }

        return localFile;

    }

    /**
     * returns true is the file is locally available, meaning clients can 
     * call getFile() and the readable File reference will be available in
     * interactive time.  For FileObjects from HttpFileSystem, a HEAD request
     * is made to ensure that the local file is as new as the website one (when offline=false).
     */
    public boolean isLocal() {
        if ( wfs.isAppletMode() ) return false;

        boolean download = false;

        if (localFile.exists()) {
            if ( !wfs.isOffline() ) {
                synchronized ( wfs ) {
                    Date remoteDate;
                    long localFileLastAccessed = wfs.getLastAccessed( this.getNameExt() );
                    if ( System.currentTimeMillis() - localFileLastAccessed > 60000 ) {
                        try {
                            if ( wfs instanceof HttpFileSystem ) {
                                    URL url = wfs.getURL(this.getNameExt());
                                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                                    connection.setRequestMethod("HEAD");
                                    connection.connect();
                                    remoteDate = new Date(connection.getLastModified());
                            } else {
                                // this is the old logic
                                remoteDate = new Date(localFile.lastModified());
                            }

                            Date localFileLastModified = new Date(localFile.lastModified());
                            if (remoteDate.after(localFileLastModified)) {
                                logger.log(Level.INFO, "remote file is newer than local copy of {0}, download.", this.getNameExt());
                                download = true;
                            }
                            wfs.markAccess(this.getNameExt());
                        } catch ( IOException ex ) {
                            return false;
                        }

                    }

                }
            } else {
                return true;
            }

        } else {
            download = true;
        }

        return !download;

    }
}
