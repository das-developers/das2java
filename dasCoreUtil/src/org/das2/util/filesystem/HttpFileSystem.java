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
 * WebFileSystem.java
 *
 * Created on May 13, 2004, 1:22 PM
 *
 * A WebFileSystem allows web files to be opened just as if they were
 * local files, since it manages the transfer of the file to a local
 * file system.
 */
package org.das2.util.filesystem;

import org.das2.util.monitor.CancelledOperationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.das2.util.Base64;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.util.filesystem.FileSystem.FileSystemOfflineException;
import java.util.concurrent.locks.Lock;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import javax.swing.SwingUtilities;
import org.das2.util.FileUtil;
import org.das2.util.OsUtil;
import static org.das2.util.filesystem.FileSystem.toCanonicalFilename;
import org.das2.util.monitor.NullProgressMonitor;

/**
 * Make a web folder accessible.  This assumes listings are provided in html form by requesting links ending in a slash.
 * For example, http://autoplot.org/data/pngwalk/.  Links to resources "outside" of the filesystem are not considered part of
 * the filesystem.  Again, this assumes listings are HTML content, and I suspect this will be changing (xml+client-side-xslt)...
 *
 * @author  Jeremy
 */
public class HttpFileSystem extends WebFileSystem {

    protected static final Logger logger= org.das2.util.LoggerManager.getLogger( "das2.filesystem.http" );

    /**
     * provide some caching for directory entries.
     */
    private final Map<String,DirectoryEntry> listingEntries= new HashMap();
    private final Map<String,Long> listingEntryFreshness= new HashMap();
    
    /** 
     * Create a new HttpFileSystem mirroring the root, a URL pointing to "http" or "https", 
     * in the local folder.
     * @param root the root of the filesystem
     * @param localRoot the local root where files are downloaded.
     */
    protected HttpFileSystem(URI root, File localRoot) {
        super(root, localRoot);
    }
    
    private String cookie=null;
    
    /**
     * return the cookie needed.
     * @return 
     */
    protected String getCookie() {
        return this.cookie;
    }
    
    /**
     * Create a filesystem from the URI.  Note "user@" will be added to the 
     * URI if credentials are needed and added automatically.
     * @param rooturi
     * @return the filesystem.
     * @throws org.das2.util.filesystem.FileSystem.FileSystemOfflineException
     * @throws UnknownHostException
     * @throws FileNotFoundException 
     */
    public static HttpFileSystem createHttpFileSystem(URI rooturi) throws FileSystemOfflineException, UnknownHostException, FileNotFoundException {
        try {
            
            String auth= rooturi.getAuthority();
            if ( auth==null ) {
                throw new MalformedURLException("URL does not contain authority, check for ///");
            }
            
            if ( rooturi.toString().contains("$Y") ) {
                logger.fine( "somehow template leaked into FileSystem code.");
            }
            
            String[] ss= auth.split("@");

            URL root;

            if ( ss.length>3 ) {
                throw new IllegalArgumentException("user info section can contain at most two at (@) symbols");
            } else if ( ss.length==3 ) {//bugfix 3299977.  UMich server uses email:password@umich.  Java doesn't like this.
                // the user didn't escape the at (@) in the email.  escape it here.
                StringBuilder userInfo= new StringBuilder( ss[0] );
                for ( int i=1;i<2;i++ ) userInfo.append("%40").append(ss[i]);
                auth= ss[2];
                try {
                    URI rooturi2= new URI( rooturi.getScheme() + "://" + userInfo.toString()+"@"+auth + rooturi.getPath() );
                    rooturi= rooturi2;
                } catch ( URISyntaxException ex ) {
                    throw new IllegalArgumentException("unable to handle: "+rooturi);
                }
            }

            root= rooturi.toURL();
            
            logger.log(Level.FINER, "See https://www.draw.io/#G0B1Ywc5_Vexx1d3ctdGZxZDNkM3M" );
            logger.log(Level.FINER, "URL Reference: {0}", root);

            boolean doCheck= true;
            URI parentURI= FileSystemUtil.getParentUri( rooturi );
            if ( parentURI!=null ) {
                HttpFileSystem parent= (HttpFileSystem) peek( parentURI );
                if ( parent!=null && parent.isOffline() ) {
                    logger.finer("parent is offline, do not check...");
                    doCheck= false;
                }
            }


            boolean offline = true;
            String offlineMessage= "";
            int offlineResponseCode= 0;
            
            String cookie= null;
            
            while ( doCheck && !FileSystem.settings().isOffline() ) {
                
                // verify URL is valid and accessible
                HttpURLConnection urlc = (HttpURLConnection) root.openConnection();
                
                urlc.setConnectTimeout( FileSystem.settings().getConnectTimeoutMs() );
                urlc.setReadTimeout( FileSystem.settings().getReadTimeoutMs() );
                //urlc.setRequestMethod("HEAD"); // Causes problems with the LANL firewall.

                String userInfo;  // null means that userInfo has not been attempted.

                try {
                    logger.log(Level.FINER, "Check keychain: ", root);
                    userInfo = KeyChain.getDefault().getUserInfo(root);
                } catch (CancelledOperationException ex) {
                    logger.log( Level.FINER, "user cancelled credentials for {0}", rooturi);
                    break;
                }
                
                if ( userInfo != null) {
                    String encode = Base64.getEncoder().encodeToString( userInfo.getBytes());
                    urlc.setRequestProperty("Authorization", "Basic " + encode);
                }

                cookie= KeyChain.getDefault().getCookie(root);
                if ( cookie!=null ) {
                    urlc.setRequestProperty("Cookie",cookie);
                }
                
                int responseCode= -1;
                try {
                    logger.log( Level.FINER, "Verify Credentials {0}", urlc );
                    if ( userInfo!=null && !userInfo.contains(":") ) {
                        logger.log( Level.INFO, "urlc={0}", urlc );
                        logger.log( Level.INFO, "userInfo does not appear to contain password: {0}", userInfo );
                    } else {
                        logger.log( Level.FINER, "userInfo.length={0}", ( userInfo==null ? -1 : userInfo.length() ));
                    }
                    urlc.connect();
                    responseCode= urlc.getResponseCode();
                    
                    logger.log( Level.FINER, "made connection, now consume rest of stream: {0}", urlc );
                    try {
                        HttpUtil.consumeStream( urlc.getInputStream() );
                    } catch ( IOException ex ) {
                        logger.fine("exception when politely consuming stream after initial check");
                    }
                    logger.log( Level.FINER, "done consuming and initial connection is complete: {0}" );
                    urlc.disconnect();
                    offline= false;
                    doCheck= false;
                    logger.finer( "Verify Credentials exits with okay");
                    
                } catch ( SocketTimeoutException ex ) {
                    logger.finer("Socket timeout");
                    HttpUtil.consumeStream( urlc.getErrorStream() );
                    responseCode= HttpURLConnection.HTTP_GATEWAY_TIMEOUT;
                    offlineMessage= "socket timeout";
                    offlineResponseCode= HttpURLConnection.HTTP_GATEWAY_TIMEOUT;
                    
                    doCheck= false;
                        
                } catch ( IOException ex ) {
                    
                    logger.finer("Error with credentials");
                    int code= 0;
                    String msg;
                    try {
                        code= urlc.getResponseCode();
                        msg= urlc.getResponseMessage();
                    } catch ( IOException ex2 ) {
                        // do nothing in this case, just try to get a response code.
                        logger.log(Level.SEVERE,ex2.getMessage(),ex2);
                        msg= ex2.getMessage();
                    }
                    
                    HttpUtil.consumeStream( urlc.getErrorStream() );
                    
                    if ( code==HttpURLConnection.HTTP_NOT_FOUND ) {
                        logger.log( Level.SEVERE, String.format( "%d: folder not found: %s\n%s", code, root, msg ), ex );
                        throw (FileNotFoundException)ex;
                    } else if ( code==HttpURLConnection.HTTP_BAD_REQUEST ) { // bad request--used by raw.githubusercontent.com/
                        // we will use this as a flag to indicate a file could be downloaded.
                        offlineMessage= "listing results in bad request";
                        logger.log( Level.SEVERE, String.format( "%d: folder cannot be listed: %s\n%s", code, root, msg ), ex );
                        doCheck= false;
                        
                    } else if ( code!=HttpURLConnection.HTTP_UNAUTHORIZED ) {
                        // Note this may still be code 403.  We still enter the same branch for now, because the user might be on a network that isn't permitted now.
                        logger.log( Level.SEVERE, String.format( "%d: failed to connect to %s\n%s", code, root, msg ), ex );
                        if ( FileSystem.settings().isAllowOffline() ) {
                            logger.info("remote filesystem is offline, allowing access to local cache.");
                            break;
                        } else {
                            throw new FileSystemOfflineException("" + code + ": " + msg );
                        }
                    }
                    
                    if ( "true".equals( System.getProperty("java.awt.headless") ) && userInfo!=null ) { 
                        logger.finer( "Headless mode means we have to give up");
                        if ( FileSystem.settings().isAllowOffline() ) {
                            logger.info("remote filesystem is offline, allowing access to local cache.");
                            break;
                        } else {
                            throw new FileSystemOfflineException("" + code + ": " + msg );
                        }
                    }
                    
                    if ( offlineMessage.length()==0 ) offlineMessage= msg;
                    offlineResponseCode= code;
                }

                if ( responseCode!=HttpURLConnection.HTTP_GATEWAY_TIMEOUT ) {
                    if ( responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_FORBIDDEN) {
                        if ( responseCode==HttpURLConnection.HTTP_UNAUTHORIZED ) {
                            // might be nice to modify URL so that credentials are used.
                            KeyChain.getDefault().clearUserPassword(root);
                            if ( userInfo==null ) {
                                String port=  root.getPort()==-1 ? "" : ( ":" +root.getPort() );
                                URL rootAuth= new URL( root.getProtocol() + "://" + "user@" + root.getHost() + port + root.getFile() );
                                try {
                                    URI rootAuthUri= rootAuth.toURI();
                                    rooturi= rootAuthUri;
                                    root= rooturi.toURL();

                                } catch ( URISyntaxException ex ) {
                                    throw new RuntimeException(ex);
                                }
                            }
                        } else if ( responseCode==HttpURLConnection.HTTP_BAD_REQUEST ) {
                            offline= true; // we can get files we know about already, but listings cannot be done.
                        } else {
                            offline= false;
                        }
                    } else {
                        offline= false;
                    }
                }

            }
            
            File local;

            if (FileSystemSettings.hasAllPermission()) {
                local = localRoot(rooturi);
                logger.log(Level.FINER, "initializing httpfs {0} at {1}", new Object[]{root, local});
            } else {
                local = null;
                logger.log(Level.FINER, "initializing httpfs {0} in applet mode", root);
            }
            HttpFileSystem result = new HttpFileSystem(rooturi, local);

            if ( offline ) {
                logger.log( Level.WARNING, "filesystem is offline: {0}", rooturi );
            }
            
            result.offline = offline;
            result.offlineMessage= offlineMessage;
            result.offlineResponseCode= offlineResponseCode;
            result.cookie= cookie;

            return result;

        } catch (FileSystemOfflineException | FileNotFoundException | UnknownHostException e) {
            throw e;
        } catch (IOException e) {
            throw new FileSystemOfflineException(e,rooturi);
        }

    }

    /**
     * It looks like an external process (another das2 app) is downloading the resource.  Wait for the other
     * process to download the file.  If the file is idle for more than the allowable external idle millisecond limit
     * (FileSystemSettings.allowableExternalIdleMs), then return false.
     * @param f the file
     * @param partFile the part file we're watching
     * @param monitor 
     * @return true if the other app appears to have loaded the resource, false otherwise.
     */
    private boolean waitDownloadExternal( File f, File partFile, ProgressMonitor monitor ) {
        monitor.setProgressMessage("waiting for other process load");
        while ( partFile.exists() && ( System.currentTimeMillis() - partFile.lastModified() ) < FileSystemSettings.allowableExternalIdleMs ) {
            try {
                Thread.sleep(300);
                logger.log(Level.FINEST, "waiting for external process to download {0}", partFile);
                monitor.setTaskProgress(partFile.length());
                if ( monitor.isCancelled() ) {
                    return false;
                }
            } catch (InterruptedException ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        if ( partFile.exists() ) {
            logger.finer("timeout waiting for partFile to be deleted");
            return false;
        } else {
            if ( f.exists() ) {
                logger.finer("successfully waited for external download to complete");
                return true;
            } else {
                logger.finer("part file removed but complete file is not found");
                return false;
            }
        }
    }
    
    /**
     * pull out some of the headers to record ETag and Content_Type.
     * @param connect
     * @return 
     */
    protected Map<String,String> reduceMeta( URLConnection connect ) {
        Map<String,String> result= new HashMap<>();
        result.put( WebProtocol.META_ETAG, connect.getHeaderField( WebProtocol.META_ETAG ) );
        result.put( WebProtocol.META_CONTENT_TYPE, connect.getHeaderField( WebProtocol.META_CONTENT_TYPE ) );
        return result;
    }
    
    /**
     * 
     * @param filename identifier for the resource, and should this end in gz, then the stream will probably be unzipped.
     * @param remoteURL the remote URL from which a connection is opened.
     * @param f the local file where the content will be stored.
     * @param partFile a temporary local file.
     * @param monitor a monitor for the download.
     * @return ETag if available
     * @throws IOException
     * @throws FileNotFoundException 
     */
    private Map<String,String> doDownload( String filename, URL remoteURL, File f, File partFile, ProgressMonitor monitor ) throws IOException, FileNotFoundException {
        
        Map<String,String> result;
        
        URLConnection urlc = remoteURL.openConnection();
        urlc.setConnectTimeout( FileSystem.settings().getConnectTimeoutMs() );
        urlc.setReadTimeout( FileSystem.settings().getReadTimeoutMs() );

        //bug http://sourceforge.net/p/autoplot/bugs/1393/ shows where this is necessary.
        urlc.setUseCaches(false);

        String userInfo;
        try {
            userInfo = KeyChain.getDefault().getUserInfo(root);
        } catch (CancelledOperationException ex) {
            throw new IOException("user cancelled at credentials entry");
        }
        if ( userInfo != null) {
            String encode = Base64.getEncoder().encodeToString(userInfo.getBytes());
            urlc.setRequestProperty("Authorization", "Basic " + encode);
        }

        if ( cookie!=null ) {
            urlc.addRequestProperty("Cookie", cookie );
        }
        try {
            urlc= HttpUtil.checkRedirect(urlc);

            InputStream in;

            if ( loggerUrl.isLoggable(Level.FINE) && urlc.getURL().getPath().endsWith("/") ) {
                loggerUrl.log(Level.FINE, "GET to get listing {0}", new Object[] { urlc.getURL() } );
            } else {
                loggerUrl.log(Level.FINE, "GET to get data {0}", new Object[] { urlc.getURL() } );
            }
            
            in= urlc.getInputStream();

            HttpURLConnection hurlc = (HttpURLConnection) urlc;
            if (hurlc.getResponseCode() == 404) {
                logger.log(Level.INFO, "{0} URL: {1}", new Object[]{hurlc.getResponseCode(), remoteURL});
                throw new FileNotFoundException("not found: " + remoteURL);
            } else if (hurlc.getResponseCode() != 200) {
                logger.log(Level.INFO, "{0} URL: {1}", new Object[]{hurlc.getResponseCode(), remoteURL});
                throw new IOException( hurlc.getResponseCode()+": "+ hurlc.getResponseMessage() + "\n"+remoteURL );
            }

            Date d;
            List<String> sd= urlc.getHeaderFields().get("Last-Modified");
            if ( sd!=null && sd.size()>0 ) {
                d= new Date( sd.get(sd.size()-1) );
            } else {
                d= new Date();
            }

            monitor.setTaskSize(urlc.getContentLength());

            if (!f.getParentFile().exists()) {
                logger.log(Level.FINER, "make dirs {0}", f.getParentFile());
                FileSystemUtil.maybeMkdirs( f.getParentFile() );
            }
            if (partFile.exists()) {
                logger.log(Level.FINER, "partFile exists {0}", partFile);
                long ageMillis= System.currentTimeMillis() - partFile.lastModified(); // TODO: this is where OS-level locking would be nice...
                if ( ageMillis<FileSystemSettings.allowableExternalIdleMs ) { // if it's been modified less than sixty seconds ago, then wait to see if it goes away, then check again.
                    if ( waitDownloadExternal( f, partFile, monitor ) ) {
                        return Collections.EMPTY_MAP; // success
                    } else {
                        if ( monitor.isCancelled() ) {
                            throw new InterruptedIOException("interrupt while waiting for external process to download "+partFile);
                        } else {
                            throw new IOException( "timeout waiting for external process to download "+partFile );
                        }
                    }
                } else {
                    if (!partFile.delete()) {
                        logger.log(Level.INFO, "Unable to delete part file {0}, using new name for part file.", partFile ); //TODO: review this
                        partFile= new File( f.toString()+".part."+System.currentTimeMillis() );
                    }
                }
            }

            if (partFile.createNewFile()) {
                //InputStream in;
                //in = urlc.getInputStream();

                logger.log(Level.FINER, "transferring bytes of {0}", filename);
                FileOutputStream out = new FileOutputStream(partFile);
                monitor.setLabel("downloading file");
                monitor.started();
                try {
                    // https://sourceforge.net/p/autoplot/bugs/1229/
                    String contentLocation= urlc.getHeaderField("Content-Location");
                    String contentType= urlc.getHeaderField("Content-Type");
                    result= reduceMeta( urlc );

                    boolean doUnzip= !filename.endsWith(".gz" ) && "application/x-gzip".equals( contentType ) && ( contentLocation==null || contentLocation.endsWith(".gz") );
                    if ( doUnzip ) {
                        in= new GZIPInputStream( in );
                    }

                    copyStream(in, out, monitor);
                    monitor.finished();
                    out.close();
                    in.close();                    

                    try {
                        partFile.setLastModified(d.getTime()+HTTP_CHECK_TIMESTAMP_LIMIT_MS);
                    } catch ( Exception ex ) {
                        logger.log( Level.SEVERE, "unable to setLastModified", ex );
                    }

                    if ( f.exists() ) {
                        if ( f.isDirectory() ) {
                            logger.finer("file was once a directory.");
                            if ( !FileUtil.deleteFileTree(f) ) {
                                throw new IllegalArgumentException("unable to folder to make way for file: "+f );
                            }
                        } else {
                            if ( f.length()==partFile.length() ) {
                                if ( OsUtil.contentEquals(f, partFile ) ) {
                                    logger.finer("another thread must have downloaded file.");
                                    if ( !partFile.delete() ) {
                                        throw new IllegalArgumentException("unable to delete "+partFile );
                                    }
                                    return result;
                                } else {
                                    logger.finer("another thread must have downloaded different file.");
                                }
                            }
                            logger.log(Level.FINER, "deleting old file {0}", f);
                            if ( !f.delete() ) {
                                throw new IllegalArgumentException("unable to delete "+f );
                            }
                        }
                    }
                    if ( !partFile.renameTo(f) ) {
                        logger.log(Level.WARNING, "rename failed {0} to {1}", new Object[]{partFile, f});
                        throw new IllegalArgumentException( "rename failed " + partFile + " to "+f );
                    }
                } catch (IOException e) {
                    out.close();
                    in.close();
                    logger.log( Level.FINER, "deleting partial download file {0}", partFile);
                    if ( partFile.exists() && !partFile.delete() ) {
                        throw new IllegalArgumentException("unable to delete "+partFile );
                    }
                    throw e;
                }
            } else {
                throw new IOException("could not create local file: " + f);
            }
        } finally {
            if ( urlc instanceof HttpURLConnection ) {
                if ( remoteURL.getPath().endsWith("/") ) {
                    logger.fine("not closing, because it was a listing file.");
                } else {
                    ((HttpURLConnection)urlc).disconnect();
                }
            }
        }
        return Collections.EMPTY_MAP;
    }
    /**
     * 
     * @param filename filename within the filesystem.
     * @param f the target filename where the file is to be download.
     * @param partFile  use this file to stage the download
     * @param monitor  monitor the progress.
     * @return metadata containing ETag if available.
     * @throws IOException 
     */
    @Override
    protected Map<String,String> downloadFile(String filename, File f, File partFile, ProgressMonitor monitor) throws IOException {

        Lock lock = getDownloadLock(filename, f, monitor);
        
        if (lock == null) {
            return Collections.EMPTY_MAP;
        }
            
        Map<String,String> meta= Collections.EMPTY_MAP;
        
        filename = toCanonicalFilename(filename);
                
        logger.log(Level.FINER, "downloadFile {0}, using temporary file {1}", new Object[] { filename, partFile } );

        try {
            URL remoteURL = getURL( filename );
            
            try {
                meta= doDownload( filename, remoteURL, f, partFile, monitor );
            } catch ( FileNotFoundException ex ) {
                if ( !filename.endsWith("/") ) {
                    remoteURL= new URL(root.toString() + filename.substring(1) + ".gz" );
                    try {
                        doDownload( filename, remoteURL, f, partFile, monitor );
                    } catch ( FileNotFoundException exgz ) {
                        throw ex;
                    }
                }
            }

        } finally {

            lock.unlock();

        }
        return meta;
    }

    /**
     * this is introduced to support checking if the symbol foo/bar is a folder by checking
     * for a 303 redirect.
     *<blockquote><pre><small>{@code
     *   EXIST->Boolean
     *   REAL_NAME->String
     *}</small></pre></blockquote>    
     * others are just HTTP header fields like (see wget --server-response https://raw.githubusercontent.com/autoplot/jyds/master/dd.jyds):
     *<blockquote><pre><small>{@code
     *   Content-Length   the length in bytes of the resource.
     *   Cache-Control    max-age=300
     *   Date: Fri, 18 Jul 2014 12:07:18 GMT  time stamp.
     *   ETag: "750d4f66c58a0ac7fef2784253bf6954d4d38a85"
     *   Accept-Ranges    accepts requests for part of a file.
     *}</small></pre></blockquote>     
     * @param f the name within the filesystem
     * @return the metadata, such as the Date and ETag.
     * @throws java.io.IOException
     * @throws org.das2.util.monitor.CancelledOperationException
     */
    protected Map<String, Object> getHeadMeta(String f) throws IOException, CancelledOperationException {

        try {
            URL ur = new URL(this.root.toURL(), f);
            Map<String,String> meta= HttpUtil.getMetadata( ur, null );
            
            Map<String,Object> result= new HashMap<>();
            result.putAll(meta);
            
            result.put( "EXIST", Boolean.parseBoolean(meta.get( WebProtocol.META_EXIST ) ) );
            result.put( WebProtocol.META_CONTENT_LENGTH, Long.parseLong(meta.get(WebProtocol.META_CONTENT_LENGTH) ) );
            result.put( WebProtocol.META_LAST_MODIFIED, Long.parseLong(meta.get(WebProtocol.META_LAST_MODIFIED ) ) );

            return result;

        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(ex);
        }

    }

    /** dumb method looks for / in parent directory's listing.  Since we have
     * to list the parent, then IOException can be thrown.
     * 
     * @return true if the name appears to be a directory (folder).
     * @throws java.io.IOException
     */
    @Override
    public boolean isDirectory(String filename) throws IOException {

        if (localRoot == null) {
            return filename.endsWith("/");
        }

        File f = new File(localRoot, filename);
        if (f.exists()) {
            return f.isDirectory();
        } else {
            if (filename.endsWith("/")) {
                return true;
            } else {
                File parentFile = f.getParentFile();
                String parent = getLocalName(parentFile);
                if (!parent.endsWith("/")) {
                    parent = parent + "/";
                }
                String[] list = listDirectory(parent);
                String lookFor;
                if (filename.startsWith("/")) {
                    lookFor = filename.substring(1) + "/";
                } else {
                    lookFor = filename + "/";
                }
                for (String list1 : list) {
                    if (list1.equals(lookFor)) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    /**
     * always hide these file types.
     * @return Arrays.asList( new String[] { ".css", ... } ).
     */
    private List<String> hideExtensions() {
        return Arrays.asList( new String[] { ".css", ".php", ".jnlp", ".part" } );
    }
    
    /**
     * list the directory, using the cached entry from listDirectoryFromMemory, or
     * by HtmlUtil.getDirectoryListing.  If there is a ro_cache, then add extra entries from here as well.
     * Note the following extentions are hidden: .css, .php, .jnlp, .part.
     * @param directory name within the filesystem
     * @return names within the directory
     * @throws IOException
     */
    @Override
    public String[] listDirectory(String directory) throws IOException {
        logger.log(Level.FINE, "** listDirectory({0}{1})", new Object[]{root, directory});

        DirectoryEntry[] cached= listDirectoryFromMemory( directory );
        if ( cached!=null ) {
            return FileSystem.getListing( cached );
        }

        if ( this.protocol!=null && this.protocol instanceof AppletHttpProtocol ) { // support applets.  This could check for local write access, but DefaultHttpProtocol shows a problem here too.
            URL[] list;
            try ( InputStream in = this.protocol.getInputStream( new WebFileObject(this,directory,new Date() ), new NullProgressMonitor() ) ) {
                list= HtmlUtil.getDirectoryListing( getURL(directory), in );
            } catch ( CancelledOperationException ex ) {
                throw new IllegalArgumentException(ex); //TODO: this should probably be IOException(ex).  See use 20 lines below as well.
            } 
            
            String[] result;
            result = new String[list.length];
            int n = directory.length();
            for (int i = 0; i < list.length; i++) {
                URL url = list[i];
                result[i] = getLocalName(url).substring(n);
            }
            return result;
        }

        directory = toCanonicalFolderName(directory);

        Map<String,DirectoryEntry> result;
        if ( isListingCached(directory) ) {
            logger.log(Level.FINER, "using cached listing for {0}", directory);

            File listing= listingFile(directory);
            
            URL[] list;
            try (FileInputStream fin = new FileInputStream(listing)) {
                list = HtmlUtil.getDirectoryListing(getURL(directory), fin );
            } catch (CancelledOperationException ex) {
                throw new IllegalArgumentException(ex); // shouldn't happen since it's local
            }
            
            result = new LinkedHashMap(list.length);
            int n = directory.length();
            for (URL url : list) {
                DirectoryEntry de1= new DirectoryEntry();
                de1.modified= Long.MAX_VALUE; // HTTP is somewhat expensive to get dates and sizes, so put in Long.MAX_VALUE to indicate need to load.
                de1.name= getLocalName(url).substring(n);
                de1.type= 'f'; //TODO: directories mis-marked?
                de1.size= Long.MAX_VALUE;
                result.put(de1.name,de1);
            }

            result= addRoCacheEntries( directory, result );

            cacheListing( directory, result.values().toArray( new DirectoryEntry[result.size()] ) );

            return FileSystem.getListing( result );
        }

        boolean successOrCancel= false;

        if ( this.isOffline() ) {
            File f= new File(localRoot, directory).getCanonicalFile();
            logger.log(Level.FINER, "this filesystem is offline, using local listing: {0}", f);

            result= addRoCacheEntries( directory, new LinkedHashMap() );
            
            if ( !f.exists() && result.isEmpty() ) throw new FileSystemOfflineException("unable to list "+f+" when offline");

            List<String> result1= new ArrayList();
            
            if ( f.exists() ) {
                File[] listing = f.listFiles();
                if ( listing==null ) {
                    throw new IllegalArgumentException("expected resource to be a directory: "+f);  
                } 

                for (File f1 : listing) {
                    if ( f1.getName().endsWith(".listing") ) continue;
                    if ( f1.isDirectory() ) {
                        result1.add( f1.getName() + '/' );
                    } else {
                        result1.add( f1.getName() );
                    }
                }
            }
            
            for ( DirectoryEntry f1: result.values() ) {
                if ( f1.type=='d' ) {
                    int n= f1.name.length();
                    if ( n>0 && f1.name.charAt(n-1)=='/' ) {
                        result1.add( f1.name );
                    } else {
                        result1.add( f1.name + '/' );
                    }
                } else {
                    result1.add( f1.name );
                }
            }
            return result1.toArray( new String[result1.size()] );
        }


        while ( !successOrCancel ) {
            logger.log(Level.FINER, "list {0}", directory);
            URL[] list;
            try {
                File listing= listingFile( directory );

                downloadFile( directory, listing, getPartFile(listing), new NullProgressMonitor() );

                if ( !listing.setLastModified( System.currentTimeMillis() ) ) {
                    logger.log(Level.WARNING, "failed to setLastModified: {0}", listing);
                }
                
                try (FileInputStream fin = new FileInputStream(listing)) {
                    list = HtmlUtil.getDirectoryListing( getURL(directory), fin );
                }
                
                int n = FileSystemUtil.uriEncode(directory).length(); // note 20 lines above with getURL uriEncode is used in getURL
                
                //remove .css stuff
                ArrayList newlist= new ArrayList();
                List<String> hideExtensions= hideExtensions();
                for ( URL s: list ) {
                    boolean hide= false;
                    if ( !s.getFile().endsWith("/") ) {
                        for ( String e : hideExtensions ) {
                            if ( s.getFile().endsWith(e) ) hide= true;
                        }
                    }
                    try {
                        String ss= getLocalName(s).substring(n);
                        if ( ss.split("/").length>1 ) {
                            hide= true;
                        }
                    } catch ( IllegalArgumentException ex ) {
                        hide= true;
                    }
                    if ( !hide ) newlist.add(s);
                }
                list= (URL[]) newlist.toArray( new URL[newlist.size()] );

                result = new LinkedHashMap();
                for (URL url : list) {
                    DirectoryEntry de1= new DirectoryEntry();
                    de1.modified= Long.MAX_VALUE;
                    de1.name= getLocalName(url).substring(n);
                    de1.type= 'f';
                    de1.size= Long.MAX_VALUE;
                    result.put(de1.name,de1);
                }

                result= addRoCacheEntries( directory, result );
                cacheListing( directory, result.values().toArray( new DirectoryEntry[result.size()] ) );

                return FileSystem.getListing(result);
                
            } catch (CancelledOperationException ex) {
                throw new IOException( "user cancelled at credentials" ); // JAVA6
            } catch ( IOException ex ) {
                if ( isOffline() ) {
                    logger.info("** using local listing because remote is not available");
                    logger.info("or some other error occurred. **");
                    File localFile= new File( localRoot, directory );
                    return localFile.list();
                } else {
                    throw ex;
                }
            }

        }
        return( new String[] { "should not get here" } ); // we should not be able to reach this point
        
    }

//    public String[] listDirectoryOld(String directory) throws IOException {
//        directory = HttpFileSystem.toCanonicalFilename(directory);
//        if (!isDirectory(directory)) {
//            throw new IllegalArgumentException("is not a directory: " + directory);
//        }
//
//        if (!directory.endsWith("/")) {
//            directory = directory + "/";
//        }
//        synchronized (listings) {
//            if ( isListingCached(directory) ) { //TODO: there are no timestamps to invalidate listings!!!  How is it I haven't run across this before...https://sourceforge.net/tracker/index.php?func=detail&aid=3395693&group_id=199733&atid=970682
//                logger.log( Level.FINE, "use cached listing for {0}", directory );
//                String[] result= (String[]) listings.get(directory);
//                String[] resultc= new String[result.length];
//                System.arraycopy( result, 0, resultc, 0, result.length );
//                return resultc;
//
//            } else {
//                logger.log(Level.FINE, "list {0}", directory);
//                URL[] list;
//                try {
//                    list = HtmlUtil.getDirectoryListing(getURL(directory));
//                } catch (CancelledOperationException ex) {
//                    throw new IOException( "user cancelled at credentials" ); // JAVA6
//                } catch ( IOException ex ) {
//                    if ( isOffline() ) {
//                        System.err.println("** using local listing because remote is not available");
//                        System.err.println("or some other error occurred. **");
//                        File localFile= new File( localRoot, directory );
//                        return localFile.list();
//                    } else {
//                        throw ex;
//                    }
//                }
//                String[] result = new String[list.length];
//                int n = directory.length();
//                for (int i = 0; i < list.length; i++) {
//                    URL url = list[i];
//                    result[i] = getLocalName(url).substring(n);
//                }
//                listings.put(directory, result);
//                listingFreshness.put( directory, System.currentTimeMillis()+LISTING_TIMEOUT_MS );
//                return result;
//            }
//        }
//    }

    @Override
    public String[] listDirectory(String directory, String regex) throws IOException {
        
        if ( SwingUtilities.isEventDispatchThread() ) {
            //logger.warning("listDirectory called on event thread!");
        }
        
        logger.log(Level.FINE, "listDirectory({0},{1})", new Object[]{directory, regex});
        
        if ( regex.endsWith("/") ) regex= regex.substring(0,regex.length()-1);

        directory = toCanonicalFilename(directory);
        if (!isDirectory(directory)) {
            throw new IllegalArgumentException("is not a directory: " + directory);
        }

        String[] listing = listDirectory(directory);
        Pattern pattern = Pattern.compile(regex);
        ArrayList result = new ArrayList();
        for (String s : listing) {
            String c= s;
            if ( s.charAt(s.length()-1)=='/' ) c= s.substring(0,s.length()-1);
            if (pattern.matcher(c).matches()) {
                result.add(s);
            }
        }
        return (String[]) result.toArray(new String[result.size()]);

    }

    
    /**
     * HTTP listings are really done by querying the single file, so support this by issuing a head request
     * @param filename filename within the system
     * @param force if true, then guarantee a listing and throw an IOException if it cannot be done.
     * @return the DirectoryEntry showing size and date.
     * @throws IOException 
     */
    @Override
    public DirectoryEntry maybeUpdateDirectoryEntry(String filename, boolean force) throws IOException {
        Long fresh= listingEntryFreshness.get(filename);
        if ( fresh!=null ) {
            if ( new Date().getTime()-fresh < HttpFileSystem.HTTP_CHECK_TIMESTAMP_LIMIT_MS ) {
                return listingEntries.get(filename);
            } else {
                synchronized ( this ) {
                    listingEntryFreshness.remove(filename);
                    listingEntries.remove(filename);
                }
            }
        }
        try {
            Map<String,Object> meta= getHeadMeta(filename);
            DirectoryEntry de= new DirectoryEntry();
            String odate= (String)meta.get("Date");
            String osize= (String)meta.get("Content-Length");
            de.type= filename.endsWith("/") ? 'd' : 'f';
            if ( odate!=null && osize!=null ) {
                de.modified= new Date(odate).getTime();
                de.size= Long.parseLong(osize);
                synchronized ( this ) {
                    listingEntries.put(filename,de);
                    listingEntryFreshness.put(filename,new Date().getTime());
                }
                return de;
            } else {
                return super.maybeUpdateDirectoryEntry(filename, force);
            }
        } catch (CancelledOperationException ex) {
            return super.maybeUpdateDirectoryEntry(filename, force);
        }
    }
    
    
}
