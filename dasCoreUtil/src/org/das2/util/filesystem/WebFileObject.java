/* Copyright (C) 2003-2015 The University of Iowa 
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

//import java.awt.EventQueue;
import java.util.logging.Level;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.util.monitor.NullProgressMonitor;
import java.io.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.util.*;
import java.util.logging.Logger;
import org.das2.util.Base64;
import org.das2.util.filesystem.FileSystem.DirectoryEntry;
import org.das2.util.filesystem.FileSystem.FileSystemOfflineException;
import static org.das2.util.filesystem.FileSystem.loggerUrl;
import org.das2.util.monitor.CancelledOperationException;

/**
 *
 * @author  Jeremy
 *
 * This is a general-purpose FileObject representing items where latency requires a little
 * caching of metadata.  This is used for both HTTP and FTP implementations.
 *
 */
public class WebFileObject extends FileObject {

    private static final Logger logger= org.das2.util.LoggerManager.getLogger("das2.filesystem.wfs");
    
    final WebFileSystem wfs;
    String pathname;
    File localFile;
    boolean isRoot;
    boolean isFolder;
    Map<String,String> metadata;
    long metaFresh= 0;  // freshness of the metadata.
    Date modifiedDate;  // more accessible version of the metadata
    long size=-1;       // more accessible version of the metadata
    
    /**
     * true if we know if it's a folder or not.
     */
    boolean isFolderResolved = false;
    public int METADATA_FRESH_TIMEOUT_MS = 10000;

    @Override
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
                if ( FileSystem.settings().isOffline() ) { //bug https://sourceforge.net/tracker/?func=detail&aid=3578171&group_id=199733&atid=970682
                    metadata= new HashMap();
                    metadata.put( WebProtocol.META_EXIST, isLocal() ? "true" : "false" );
                } else {
                    if ( wfs.protocol!=null ) {
                       metadata= wfs.protocol.getMetadata( this );
                    }
                }
            } else {
                if ( wfs.protocol!=null ) {
                    metadata= wfs.protocol.getMetadata( this );
                }
            }
            metaFresh= System.currentTimeMillis();
        }
    }
    
    @Override
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

    @Override
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
        if ( !localFile.exists() || ( this.modifiedDate.getTime()-localFile.lastModified() > 10 ) && !this.wfs.isOffline() ) { //TODO: test me!
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
    @Override
    public FileObject getParent() {
        return new WebFileObject(wfs, wfs.getLocalName(localFile.getParentFile()), new Date(System.currentTimeMillis()));
    }
    
    @Override
    public boolean isData() {
        return !this.isFolder;
    }

    @Override
    public boolean isFolder() {
        if ( this.isFolderResolved ) {        
            return this.isFolder;
        } else {    
            //TODO: make HttpFileObject that does HEAD requests to properly answer these questions.  See HttpFileSystem.getHeadMeta()
            throw new RuntimeException("IOException in constructor prevented us from resolving");
        }
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public boolean isRoot() {
        return this.isRoot;
    }

    @Override
    public java.util.Date lastModified() {
        if ( System.currentTimeMillis() - metaFresh > METADATA_FRESH_TIMEOUT_MS ) {
            metadata= null;
            modifiedDate= new Date( Long.MAX_VALUE );
        }
        if ( modifiedDate.getTime()==Long.MAX_VALUE ) {
            try {
                maybeLoadMetadata();
            } catch ( IOException ex ) {
                logger.log(Level.FINE, "unable to load metadata: {0}", ex);
                modifiedDate= new Date( localFile.lastModified() );
            }
            if ( metadata!=null && metadata.containsKey("Last-Modified") ) {
                long date= Date.parse( metadata.get("Last-Modified") );
                modifiedDate= new Date( date );
            } else {
                logger.fine("metadata doesn't contain Last-Modified, using localFile" );
                modifiedDate= new Date( localFile.lastModified() );
            }
        }
        return new Date(modifiedDate.getTime());
    }

    /**
     * return the fileObject size in bytes.  This may contact the server to get the size, and this
     * caches the size.
     * @return 
     */
    @Override
    public long getSize() {
        if (isFolder) {
            throw new IllegalArgumentException("is a folder");
        }
        if ( this.size==-1 ) {
            try {
                maybeLoadMetadata();
            } catch ( IOException ex ) {
                logger.log(Level.FINE, "unable to load metadata: {0}", ex);
                size= localFile.length();
            }
            if ( metadata.containsKey("Content-Length") ) {
                size= Long.parseLong(metadata.get("Content-Length") );
            } else {
                logger.fine("remote length is not known");
                size= localFile.length();
            }
        }
        return size;
    }
    
    /**
     * allow subclasses, such as FtpBeanFileSystem, to delay loading of the date.
     * @param d
     */
    protected void setLastModified( Date d ) {
        if ( this.modifiedDate.getTime()==0 || this.modifiedDate.getTime()==Long.MAX_VALUE ) {
            this.modifiedDate= d;
        } else {
            if ( !d.equals(modifiedDate) ) {
                throw new IllegalArgumentException("valid date cannot be modified");
            }
        }
    }

    /**
     * allow classes to delay loading of the size.
     * @param size the size in bytes of the file.
     * @throws IllegalArgumentException if the size is reset to a different value from a valid value.
     */
    protected void setSize( long size ) {
        if ( this.size==-1 ) {
            this.size= size;
        } else {
            if ( size!=this.size ) {
                throw new IllegalArgumentException("valid size cannot be modified");     
            } 
        }
    }

    /**
     * returns the File that corresponds to the remote file.  This may or may
     * not exist, depending on whether it's been downloaded yet.
     * @return the file reference within the cache.
     */
    protected File getLocalFile() {
        return this.localFile;
    }

    @Override
    public boolean removeLocalFile() {
        if ( this.localFile==null ) {
            logger.fine("failed to removeLocalFile, it is null.  Applet mode" );
            return true;
        }
        if ( !this.localFile.exists() ) {
            logger.fine("localfile does not exist." );
            return true;
        }
        if ( this.localFile.canWrite() ) {
            if ( !this.localFile.delete() ) {
                logger.log(Level.FINE, "failed to removeLocalFile: {0}", this.localFile);
                return false;
            } else {
                logger.log(Level.FINER, "local file was removed: {0}", localFile);
                return true;
            }
        } else {
            logger.log(Level.FINE, "user does not have access to delete the local file: {0}", this.localFile);
            return true;
        }
    }

    @Override
    public boolean exists() {
        if ( wfs.getReadOnlyCache()!=null ) {
            File f= wfs.getReadOnlyCache();
            File ff= new File( f, this.pathname );
            if ( ff.exists() ) return true;
        }
        if ( localFile!=null && localFile.exists() &&  wfs.isAppletMode() ) { // applet support
            return true;
        } else {
            try {
                if ( wfs.protocol!=null ) {
                    maybeLoadMetadata();
                    return "true".equals( metadata.get( WebProtocol.META_EXIST ) );
                } else {
                    // TODO: use HTTP HEAD, etc
                    logger.fine("This implementation of WebFileObject.exists() is not optimal");
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
            this.localFile = new File(wfs.getLocalRoot(), pathname); // TODO: perhaps this should be ro_cache.txt...

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
            } catch ( ConnectException ex ) {
                ex.printStackTrace();
                logger.log(Level.SEVERE,ex.getMessage(), ex);
            } catch (IOException ex) {
                logger.log(Level.SEVERE,"unable to construct web file object",ex);
                this.isFolderResolved = false;
            }
        }
    }

    @Override
    public String toString() {
        return "[" + wfs + "]" + getNameExt();
    }

    @Override
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
    @Override
    public java.nio.channels.ReadableByteChannel getChannel(ProgressMonitor monitor) throws FileNotFoundException, IOException {
        InputStream in= getInputStream(monitor);
        return Channels.newChannel(in);
    }

    /**
     * return the file for the WebFileObject.
     * @param monitor
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    @Override
    public File getFile(ProgressMonitor monitor) throws FileNotFoundException, IOException {
        
        if ( wfs.isAppletMode() ) throw new SecurityException("getFile cannot be used with applets.");

        //bugfix http://sourceforge.net/tracker/?func=detail&aid=3155917&group_id=199733&atid=970682:
        // calling EventQueue.isDispatchThread() starts the event thread, causing problems when called from RSI's IDL.
//        if ( false ) {
//            if ( EventQueue.isDispatchThread() ) {
//                logger.log(Level.SEVERE, "download on event thread! {0}", this.getNameExt());
//            }
//        }

        boolean download = false;

        if ( monitor==null ) throw new NullPointerException("monitor may not be null");
        Date remoteDate;
        long remoteLength=0;

        //check readonly cache for file.
        if ( this.wfs.getReadOnlyCache()!=null ) {
            File cacheFile= new File( this.wfs.getReadOnlyCache(), this.getNameExt() );
            if ( cacheFile.exists() ) {
                logger.log(Level.FINE, "using file from ro_cache: {0}", this.getNameExt());
                return cacheFile;
            }
        }

        if ( isLocal() ) { // isLocal does a careful check of timestamps, and minds the limits on access.
            remoteDate = new Date(localFile.lastModified());
            remoteLength= localFile.length();
            
        } else if (wfs instanceof HttpFileSystem && !wfs.isOffline() ) {
            URL url = wfs.getURL(this.getNameExt());

            String userInfo= null;

            try {
                userInfo = KeyChain.getDefault().getUserInfo( url );
            } catch (CancelledOperationException ex) {
                throw new FileSystemOfflineException("user cancelled credentials");
            }
            
            Map<String,String> requestProperties= new HashMap<>();
            
            if ( userInfo != null) {
                String encode = Base64.encodeBytes( userInfo.getBytes());
                requestProperties.put( "Authorization", "Basic " + encode );
            }

            String cookie= ((HttpFileSystem)wfs).getCookie();
            if ( cookie!=null ) {
                requestProperties.put( "Cookie", cookie  );
            }

            Map<String,String> meta= HttpUtil.getMetadata( url, requestProperties );
            
            long lastModified= Long.parseLong( meta.get( WebProtocol.META_LAST_MODIFIED ) );
            remoteDate = new Date(lastModified); // here bug 1393 w/webstart https://sourceforge.net/p/autoplot/bugs/1393/
            logger.log(Level.FINE, "HEAD request reports connection.getLastModified()={0}", remoteDate);
            long contentLength= Long.parseLong( meta.get( WebProtocol.META_CONTENT_LENGTH ) );
            if ( contentLength>-1 ) remoteLength= contentLength;
                
        } else {
            if ( this.lastModified().getTime()==0 || this.lastModified().getTime()==Long.MAX_VALUE ) {
                DirectoryEntry result= wfs.maybeUpdateDirectoryEntry( this.getNameExt(), true ); // trigger load of the modifiedDate
                if ( result==null ) {
                    logger.fine("file does not exist on remote filesystem"); 
                } else {
                    result= wfs.maybeUpdateDirectoryEntry( this.getNameExt(), true );
                    remoteDate= new Date( result.modified );
                    remoteLength= result.size;
                    this.setLastModified( remoteDate );
                    this.setSize( remoteLength );
                }
                if ( !( wfs instanceof HttpFileSystem ) ) {
                    download= true;
                } //FTP filesystem timetags are very course.
            }
            remoteDate = this.lastModified();
            remoteLength= this.getSize();
        }

        if (localFile.exists()) {
            Date localFileLastModified = new Date(localFile.lastModified()); 
            if (remoteDate.after(localFileLastModified) || remoteLength!=localFile.length() ) {
                logger.log(Level.FINE, "remote file length is different or is newer than local copy of {0}, download.", this.getNameExt());
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
                File partFile = wfs.getPartFile( localFile );
                wfs.downloadFile(pathname, localFile, partFile, monitor.getSubtaskMonitor("download file"));

                if ( !localFile.setLastModified(remoteDate.getTime()) ) {
                    logger.log(Level.FINE, "unable to modify date of {0}", localFile);
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
     * @return true if the file is local and can be used without web access.
     */
    @Override
    public boolean isLocal() {
        if ( wfs.isAppletMode() ) return false;

        boolean download;

        if ( this.wfs.getReadOnlyCache()!=null ) {
            File cacheFile= new File( this.wfs.getReadOnlyCache(), this.getNameExt() );
            if ( cacheFile.exists() ) {
                logger.log(Level.FINE, "file exists in ro_cache, so trivially local: {0}", this.getNameExt());
                return true;
            }
        }

        if (localFile.exists()) {
            if ( !wfs.isOffline() ) {
                try {
                    synchronized ( wfs ) {
                        DirectoryEntry remoteDate= (DirectoryEntry) wfs.accessCache.doOp( this.getNameExt() );
                        long localFileLastModified = localFile.lastModified();
                        setLastModified( new Date(remoteDate.modified) );
                        setSize( remoteDate.size );
                        if ( remoteDate.modified > localFileLastModified ) {
                            logger.log(Level.FINE, "remote file is newer than local copy of {0}, download.", this.getNameExt());
                            download = true;
                        } else download = remoteDate.size!= localFile.length();
                    }
                } catch ( Exception ex ) {
                    logger.log( Level.WARNING, ex.getMessage(), ex );
                    return false;
                }
            } else {
                logger.log(Level.FINE, "wfs is offline, and local file exists: {0}", this.getNameExt());
                return true;
            }

        } else {
            download = true;
        }

        return !download;

    }
}
