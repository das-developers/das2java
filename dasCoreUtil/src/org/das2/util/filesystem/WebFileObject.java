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
 * Created on May 14, 2004, 10:06 AM
 */
package org.das2.util.filesystem;

//import java.awt.EventQueue;
import java.util.logging.Level;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.util.monitor.NullProgressMonitor;
import java.io.*;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
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
 * This is a general-purpose FileObject representing items where latency requires a little
 * caching of metadata.  This is used for both HTTP and FTP implementations.
 *
 */
public class WebFileObject extends FileObject {

    protected static final Logger logger= org.das2.util.LoggerManager.getLogger("das2.filesystem.wfs");
    
    final WebFileSystem wfs;
    String pathname;
    
    /**
     * localFile is a local copy of the remote file, which the FS library will fetch if needed.
     */
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
    protected void maybeLoadMetadata() throws IOException {
        Map<String,String> localMetadata;
        synchronized (this){
            localMetadata= this.metadata;
        }
        if ( localMetadata==null ) {
            logger.log(Level.FINER, "loading new metadata for {0}", this.pathname);
            synchronized (this) {
                if ( this.metadata==null ) { // double-check
                    if ( this.wfs.offline ) {
                        if ( FileSystem.settings().isOffline() ) { //bug https://sourceforge.net/p/autoplot/bugs/932/
                            logger.finer("offline check of metadata based on local file");
                            metadata= new HashMap();
                            metadata.put( WebProtocol.META_EXIST, isLocal() ? "true" : "false" );
                        } else {
                            if ( wfs.protocol!=null ) {
                                logger.finer("offline check of metadata based on wfs.protocol");
                                metadata= wfs.protocol.getMetadata( this );
                            } else {
                                logger.finer("no load of metadata");
                            }
                        }
                    } else {
                        if ( wfs.protocol!=null ) {
                            logger.finer("wfs.protocol used to get metadata");
                            metadata= wfs.protocol.getMetadata( this );
                        } else {
                            logger.finer("no load of metadata");
                        }
                    }
                    metaFresh= System.currentTimeMillis();
                } else {
                    logger.finer("double check says we have metadata now");
                }
            }
        } else {
            logger.finer("using local metadata");
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
            try {
                this.isFolder= wfs.isDirectory(this.pathname);
                this.isFolderResolved= true;
                return this.isFolder;
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
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
        long localMetaFresh;
        synchronized (this) {
            localMetaFresh= metaFresh;
        }
        if ( System.currentTimeMillis() - localMetaFresh > METADATA_FRESH_TIMEOUT_MS ) {
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
            } catch ( javax.net.ssl.SSLException e ) {
                throw new RuntimeException(e);
            } catch ( SocketTimeoutException ex ) {
                wfs.setOffline(true);
                return false;
            } catch (IOException e) {
                logger.log( Level.FINE, e.getMessage(), e );
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
            this.localFile = new File(wfs.getLocalRoot(), pathname); // This will always be the scientist's cache area which we can modify
            
            if ( FileSystem.settings().getPersistence()==FileSystemSettings.Persistence.SESSION ) this.localFile.deleteOnExit();

            try {
                if (!localFile.canRead()) {
                    if ( pathname.equals("") || pathname.endsWith("/") ) {  // this used to check the file listing.
                        FileSystemUtil.maybeMkdirs(localFile);
                        this.isFolder = true;
                        if ("".equals(pathname)) {
                            this.isRoot = true;
                        }
                        this.isFolderResolved= true;
                    } else {
                        this.isFolderResolved= false;
                        // do nothing, we still don't know
                    }
                } else {
                    this.isFolder = localFile.isDirectory();
                    this.isFolderResolved= true;
                }
            } catch ( ConnectException ex ) {
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


        if ( monitor==null ) throw new NullPointerException("monitor may not be null");

        //check readonly cache for file.  For the Github filesystem, this could be used to verify local modifications.
        if ( this.wfs.getReadOnlyCache()!=null ) {
            File cacheFile= new File( this.wfs.getReadOnlyCache(), this.getNameExt() );
            if ( cacheFile.exists() ) {
                logger.log(Level.FINE, "using file from ro_cache: {0}", cacheFile.getPath() );
                return cacheFile;
            }
        }

        Map<String,Object> firstMeta= new HashMap<>();
        
        boolean download;
        Date remoteDate;
        long remoteLength;

        download= doCheckFreshness(firstMeta);
        
        remoteDate= (Date)firstMeta.get("remoteDate");
        remoteLength= (Long)firstMeta.get("remoteLength");
        
        logger.log(Level.FINER, "remoteDate: {0}", remoteDate);
        logger.log(Level.FINER, "remoteLength: {0}", remoteLength);
        
        //check readonly cache for file.
        if ( download && this.wfs.getReadOnlyCache()!=null ) {
            File cacheFile= new File( this.wfs.getReadOnlyCache(), this.getNameExt() );
            if ( cacheFile.exists() ) {
                logger.log(Level.FINE, "using file from ro_cache: {0}", cacheFile.getPath() );
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
                Map<String,String> meta= wfs.downloadFile(pathname, localFile, partFile, monitor.getSubtaskMonitor("download file"));

                if ( meta!=null && meta.size()>0 ) {
                    cacheMeta( getLocalFile(), meta );
                }
                if ( remoteDate.getTime()>0 ) { // 401 might have prevented getting a timestamp for the file
                    if ( !localFile.setLastModified(remoteDate.getTime()) ) {
                        logger.log(Level.FINE, "unable to modify date of {0}", localFile);
                    }
                }

                logger.log(Level.FINE, "downloaded local file has date {0}", new Date(localFile.lastModified()));

            } catch (FileNotFoundException e) {
                // TODO: do something with part file.
                throw e;
            } catch (IOException ex ) {
                if ( ex.getMessage()!=null ) {
                    if ( ex.getMessage().contains("Forbidden") ) {
                        throw ex;
                    } else if ( ex.getMessage().contains("requires authentication")) { //"GitHub/GitLabs which requires authentication is not supported"
                        throw ex;
                    }
                } 
                if ( this.wfs instanceof HttpFileSystem && !(ex instanceof InterruptedIOException )  ) { //TODO: when would we use this--it needs to be more precise.
                    if ( this.wfs.isOffline() ) {
                        logger.log(Level.SEVERE,"unable getFile",ex);
                        throw new FileSystem.FileSystemOfflineException("not found in local cache: "+getNameExt() );
                    }
                }
                throw ex;
            } finally {
                if ( !monitor.isFinished() ) monitor.finished();
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
                        DirectoryEntry remoteMeta= (DirectoryEntry) wfs.accessCache.doOp( this.getNameExt() );
                        long localFileLastModified = localFile.lastModified();
                        if ( remoteMeta.modified>0 ) setLastModified( new Date(remoteMeta.modified) );
                        if ( remoteMeta.size>-1 ) setSize( remoteMeta.size );
                        if ( remoteMeta.modified > localFileLastModified ) {
                            logger.log(Level.FINE, "remote file is newer than local copy of {0}, download.", this.getNameExt());
                            download = true;
                        } else {
                            download = remoteMeta.size>-1 && remoteMeta.size!= localFile.length();
                        }
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

    /**
     * lookup the ETag associated with the file or "" if one is not found.  The
     * etag is found in ".meta/filename.meta"
     * @param localFile
     * @return 
     */
    private String getLocalETag(File localFile) {
        File parentFile= localFile.getParentFile();
        File meta= new File( parentFile, ".meta" );
        File localFileMeta= new File( meta, localFile.getName()+".meta" );
        if ( !localFileMeta.exists() ) {
            return "";
        } else {
            try {
                String etag="";
                try (BufferedReader r = new BufferedReader( new FileReader(localFileMeta) )) {
                    String l= r.readLine();
                    while ( l!=null ) {
                        if ( l.startsWith("ETag: " ) ) {
                            etag= l.substring(6).trim();
                            break;
                        }
                        l= r.readLine();
                    }
                }
                return etag;
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
                return "";
            }
        }
        
    }
    
    private void cacheMeta( File localFile, Map<String,String> metap ) throws IOException {
        File parentFile= localFile.getParentFile();
        File meta= new File( parentFile, ".meta" );
        if ( !meta.exists() ) {
            if ( !meta.mkdirs() ) {
                logger.log(Level.WARNING, "unable to create local directory: {0}", meta);
                return;
            }
        }
        File localFileMeta= new File( meta, localFile.getName()+".meta" );
        File localFileMetaTemp= new File( meta, localFile.getName()+".meta.temp" );
        try (PrintWriter write = new PrintWriter( new FileWriter( localFileMetaTemp ) )) {
            write.println("ETag: "+metap.get("ETag") );
        }
        if ( localFileMeta.exists() ) {
            if ( !localFileMeta.delete() ) {
                logger.log(Level.WARNING, "unable to delete metadata file: {0}", localFileMeta);
            }
        }
        if ( !localFileMetaTemp.renameTo(localFileMeta) ) {
            logger.log(Level.WARNING, "unable to rename metadata file: {0}", localFileMetaTemp);
        }
    }

    /**
     * Check the freshness of the file object, comparing local timestamp to the server timestamp.
     * @param firstMeta a map where metadata should be put for future use
     * @return
     * @throws org.das2.util.filesystem.FileSystem.FileSystemOfflineException
     * @throws IOException 
     */
    private synchronized boolean doCheckFreshness( Map<String,Object> firstMeta ) throws FileSystemOfflineException, IOException {
        boolean download;
        
        Date remoteDate;
        long remoteLength=0;
        
        if ( isLocal() ) { 
            // isLocal has done a careful check of timestamps, and minds the limits on access, put dummy values in for remoteDate and remoteLength.
            remoteDate = new Date(localFile.lastModified());
            remoteLength= localFile.length();
            
        } else if (wfs instanceof HttpFileSystem && !wfs.isOffline() ) {
            URL url = wfs.getURL(this.getNameExt());

            String userInfo= null;

            Map<String,String> meta;
            int responseCode;
            do {

                try {
                    userInfo = KeyChain.getDefault().getUserInfo( url );
                } catch (CancelledOperationException ex) {
                    throw new FileSystemOfflineException("user cancelled credentials");
                }
            
                Map<String,String> requestProperties= new HashMap<>();
            
                if ( userInfo != null) {
                    String encode = Base64.getEncoder().encodeToString( userInfo.getBytes());
                    requestProperties.put( "Authorization", "Basic " + encode );
                }

                String cookie= ((HttpFileSystem)wfs).getCookie();
                if ( cookie!=null ) {
                    requestProperties.put( "Cookie", cookie  );
                }

                meta= HttpUtil.getMetadata( url, requestProperties );
                if ( meta.containsKey(WebProtocol.HTTP_RESPONSE_CODE) ) {
                    responseCode= Integer.parseInt( meta.get(WebProtocol.HTTP_RESPONSE_CODE) );
                    if ( responseCode==401 ) {
                        KeyChain.getDefault().clearUserPassword( url );
                    }
                } else {
                    responseCode= 200;
                }
            } while ( responseCode==401 ); // loop until cancel or correct password is entered.
            
            long lastModified= Long.parseLong( meta.get( WebProtocol.META_LAST_MODIFIED ) );
            remoteDate = new Date(lastModified); // here bug 1393 w/webstart https://sourceforge.net/p/autoplot/bugs/1393/
            logger.log(Level.FINE, "HEAD request reports connection.getLastModified()={0}", remoteDate);
            long contentLength= Long.parseLong( meta.get( WebProtocol.META_CONTENT_LENGTH ) );
            if ( contentLength>-1 ) remoteLength= contentLength;
            if ( "application/x-gzip".equals( meta.get("ContentType") ) ) {
                String contentLocation= meta.get("Content-Location");
                if ( contentLocation!=null && contentLocation.endsWith(".gz") ) {
                    remoteLength=-1;
                }
            }
          
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
            }
            remoteDate = this.lastModified();
            remoteLength= this.getSize();
        }

        if (localFile.exists()) {
            download = false;
            Date localFileLastModified = new Date(localFile.lastModified()); 
            if ( this.lastModified().getTime()==0 ) { // force metadata load. 
                logger.fine("server doesn't provide dates, download unless etag suggests otherwise");
                download = true;
            }
            if ( remoteDate.after(localFileLastModified) || ( remoteLength>-1 && remoteLength!=localFile.length() ) ) {
                logger.log(Level.FINE, "remote file length is different or is newer than local copy of {0}, download.", this.getNameExt());
                download = true;
            }
            
            // check for ETag.
            String remoteETag= metadata==null ? null : metadata.get( WebProtocol.META_ETAG );
            if ( remoteETag!=null ) {
                if ( download ) {
                    String localETag= getLocalETag( getLocalFile() );
                    if ( localETag.length()>0 && localETag.equals(remoteETag ) ) {
                        logger.fine("etag hasn't changed, don't download."); 
                        download= false;
                        //TODO: This isn't really correct.  The client das2 should request the resource, which will send back a 304 (not modified).
                    }
                } else {
                    String localETag= getLocalETag( getLocalFile() );
                    if ( localETag.length()>0 && !localETag.equals(remoteETag ) ) {
                        logger.fine("etag has changed, do download.");
                        download= true;
                    }
                }
            }
            
        } else {
            download = true;
        }

        firstMeta.put( "remoteDate", remoteDate );
        firstMeta.put( "remoteLength", remoteLength );
        logger.log(Level.FINE, "doCheckFreshness says download={0}", download);
        return download;
    }
}
