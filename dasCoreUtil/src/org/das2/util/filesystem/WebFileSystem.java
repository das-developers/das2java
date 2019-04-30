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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.das2.util.monitor.ProgressMonitor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.das2.util.FileUtil;
import org.das2.util.monitor.CancelledOperationException;

/**
 * Base class for HTTP and FTP-based filesystems.  A local cache is kept of
 * the files.  
 * 
 * @author  Jeremy
 */
public abstract class WebFileSystem extends FileSystem {

    protected static final Logger logger= org.das2.util.LoggerManager.getLogger( "das2.filesystem.wfs" );

    /**
     * we keep a cached listing in on disk.  This is backed by the website. 
     */
    public static final int LISTING_TIMEOUT_MS = 10000;

    /**
     * we keep a cached listing in memory for performance.  This is backed by the .listing file.
     */
    public static final int MEMORY_LISTING_TIMEOUT_MS= 10000;

    /**
     * timestamp checks will occur no more often than this.
     */
    public static final int HTTP_CHECK_TIMESTAMP_LIMIT_MS = 10000;

    public static File getDownloadDirectory() {
        File local = FileSystem.settings().getLocalCacheDir();
        return local;
    }

    /**
     * get access times via ExpensiveOpCache, which limits the number of HEAD requests to the server.
     */
    ExpensiveOpCache accessCache;

    protected final File localRoot;
    /**
     * if true, then don't download to local cache.  Instead, provide inputStream
     * and getFile throws exception.
     */
    private boolean applet;
    /**
     * plug-in template for implementation.  if non-null, use this.
     */
    protected WebProtocol protocol;
    
    /**
     * true means only local files are used from the cache.
     */
    protected boolean offline = false;
    
    /**
     * the response message explaining why the filesystem is offline.
     */
    protected String offlineMessage= "";
    
    /**
     * if true, then the remote filesystem is not accessible, but local cache
     * copies may be accessed.  See FileSystemSettings.allowOffline
     */
    public static final String PROP_OFFLINE = "offline";

    public boolean isOffline() {
        return offline;
    }

    /**
     * @param offline
     */
    public void setOffline(boolean offline) {
        boolean oldOffline = offline;
        this.offline = offline;
        //FileSystem.settings().setOffline(true);  some may be online, some offline.
        propertyChangeSupport.firePropertyChange(PROP_OFFLINE, oldOffline, offline);
    }

    /**
     * return the reason (if any provided) why the filesystem is offline,
     * @return the message for the response code
     */
    public String getOfflineMessage() {
        return offlineMessage;
    }
    
    protected int offlineResponseCode= 0;
    
    /**
     * if non-zero, the response code (e.g. 403) why the filesystem is offline.
     * @return the response code.
     */
    public int getOfflineResponseCode() {
        return offlineResponseCode;
    }
    
    /**
     * alternate location to check for file before downloading.
     */
    public static final String PROP_READ_ONLY_CACHE= "readOnlyCache";

    private File readOnlyCache= null;

    public final void setReadOnlyCache( File f ) {
        File oldValue= this.readOnlyCache;
        this.readOnlyCache= f;
        propertyChangeSupport.firePropertyChange(PROP_READ_ONLY_CACHE, oldValue, readOnlyCache );
    }

    public final File getReadOnlyCache( ) {
        return this.readOnlyCache;
    }


    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    /**
     * return the name of the folder containing a local copy of the cache.
     * @param localRoot the root which will typically be a subfolder of 
     * FileSystem.settings().getLocalCacheDir();
     * @return null or ...
     */
    
     private static File lookForROCache( File start ) {

        File localRoot= start;
        File stopFile= FileSystem.settings().getLocalCacheDir();
        File result= null;

        if ( !localRoot.toString().startsWith(stopFile.toString()) ) {
            throw new IllegalArgumentException("localRoot filename ("+stopFile+") must be parent of local root: "+start);
        }
        
        while ( !( localRoot.equals(stopFile) ) ) {
            File f= new File( localRoot, "ro_cache.txt" );
            if ( f.exists() ) {
                BufferedReader read = null;
                try {
                    read = new BufferedReader( new InputStreamReader( new FileInputStream(f), "UTF-8" ) );
                    String s = read.readLine();
                    while (s != null) {
                        int i= s.indexOf("#");
                        if ( i>-1 ) s= s.substring(0,i);
                        if ( s.trim().length()>0 ) {
                            if ( s.startsWith("http:") || s.startsWith("https:") || s.startsWith("ftp:") ) {
                                throw new IllegalArgumentException("ro_cache should contain the name of a local folder");
                            }
                            String sf= s.trim();
                            result= new File(sf);
                            break;
                        }
                        s = read.readLine();
                    }
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, ex.getMessage(), ex);
                } finally {
                    try {
                        if ( read!=null ) read.close();
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, ex.getMessage(), ex);
                    }
                }
                break;
            } else {
                localRoot= localRoot.getParentFile();
            }
        }
        if ( result==null ) {
            return result;
        } else {
            String tail= start.getAbsolutePath().substring(localRoot.getAbsolutePath().length());
            if ( tail.length()>0 ) {
                return new File( result, tail );
            } else {
                return result;
            }
        }
    }

    /**
     * Allow the local RO Cache to contain files that are not yet in the remote filesystem, to support the case
     * where a data provider tests locally available products before mirroring them out to the public website.
     * @param directory
     * @param remoteList
     * @return
     */
    protected Map<String,DirectoryEntry> addRoCacheEntries( String directory, Map<String,DirectoryEntry> remoteList ) {
        File f= this.getReadOnlyCache();
        if ( f!=null ) {
            String[] ss= new File( f, directory ).list();
            if ( ss==null ) return remoteList;
            List<DirectoryEntry> add= new ArrayList<>();
            for ( String s: ss ) {
                File f1= new File( f, directory+s );
                if ( f1.isDirectory() ) {
                    s= s+"/"; //TODO: verify windows.
                }
                if ( !remoteList.containsKey(s) ) {
                    
                    DirectoryEntry de1= new DirectoryEntry();
                    de1.modified= f1.lastModified();
                    de1.name= s;
                    de1.type= f1.isDirectory() ? 'd': 'f';
                    de1.size= f1.length();
                    add.add( de1 );
                }
            }
            for ( DirectoryEntry de1: add ) {
                remoteList.put( de1.name, de1 );
            }
        }
        return remoteList;
    }

    /** Creates a new instance of WebFileSystem
     * @param root the remote URI.
     * @param localRoot local directory used to store local copies of the data.
     */
    protected WebFileSystem(URI root, File localRoot) {
        super(root);
        this.localRoot = localRoot;
        if (localRoot == null) {
            if ( root.getScheme().equals("http")
                    || root.getScheme().equals("https" ) ) {
                this.protocol = new AppletHttpProtocol();
            }
        } else {
            if (root.getScheme().equals("http")
                    || root.getScheme().equals("https" ) ) {
                this.protocol = new DefaultHttpProtocol();
            }
            File f= lookForROCache( localRoot );
            if ( f!=null ) {
                setReadOnlyCache( f );
            }
        }

        ExpensiveOpCache.Op accessTime;
        
        if ( root.getScheme().equals("http") || root.getScheme().equals("https") ) {
            accessTime= new LastAccessTime();
        } else {
            accessTime= new ListingsLastAccessTime();
        }
        this.accessCache= new ExpensiveOpCache( accessTime, HTTP_CHECK_TIMESTAMP_LIMIT_MS );

    }

    private class LastAccessTime implements ExpensiveOpCache.Op {

        @Override
        public Object doOp(String key) throws IOException {
            URL url = getURL(key);
            
            Map<String,String> meta= HttpUtil.getMetadata( url, null );

            DirectoryEntry result= new DirectoryEntry();
            result.modified= Long.parseLong( meta.get( WebProtocol.META_LAST_MODIFIED ) );
            result.name= key;
            result.size= Long.parseLong( meta.get( WebProtocol.META_CONTENT_LENGTH ) );
            
            return result;
        }
    }

    /**
     * assume local files contain the last access time. This uses the .listings file and
     * assumes that listDirectory will be keeping things up-to-date.
     */
    private class ListingsLastAccessTime implements ExpensiveOpCache.Op {
        @Override
        public Object doOp(String key) throws IOException {
            File localFile= new File( getLocalRoot(), key );
            String name= localFile.getName(); // remove the path information
            String parent= WebFileSystem.this.getLocalName( localFile.getParentFile() );
            parent= parent + '/';
            String[] ss= listDirectory( parent ); // fill the cache
            logger.log( Level.FINE, "ss.length={0}", ss.length );
            DirectoryEntry[] des= listDirectoryFromMemory(parent);
            if ( des==null ) return new Date(0);
            for (DirectoryEntry de : des) {
                if (de.name.equals(name)) {
                    return de;
                }
            }
            return FileSystem.NULL;
        }
    }
    
    /**
     * return the local root for the URI.
     * @param root the URI such as http://das2.org/data/
     * @return /home/jbf/autoplot_data/fscache/http/das2.org/data/
     */
    public static File localRoot(URI root) {

        File local = FileSystem.settings().getLocalCacheDir();

        logger.log( Level.FINE, "WFS localRoot={0}", local);
        
        String s = root.getScheme() + "/" + root.getHost() + "/" + root.getPath(); //TODO: check getPath

        local = new File(local, s);
        try {
            FileSystemUtil.maybeMkdirs(local);
        } catch (IOException ex) {
            throw new IllegalArgumentException( ex );
        }

        return local;
    }
    
    /**
     * Keep track of active downloads.  This handles, for example, the case
     * where the same file is requested several times by different threads.
     */
    private final Map downloads = new HashMap();

    /**
     * Wait while another thread is downloading the file.
     * @param monitor this thread's monitor.
     * @param mon the monitor of the thread doing the download.
     * @param filename
     * @throws java.lang.RuntimeException
     */
    private void waitForDownload( ProgressMonitor monitor, final String filename ) {

        monitor.setProgressMessage("waiting for file to download");
        
        ProgressMonitor downloadMonitor = (ProgressMonitor) downloads.get(filename);

        monitor.started();

        while (downloadMonitor != null) {

            // in case downloadMonitor switched from indeterminate to determinate
            monitor.setTaskSize( downloadMonitor.getTaskSize() );

            // this monitor can tell the downloading monitor to cancel.
            boolean isCancelled= monitor.isCancelled();
            if ( isCancelled ) {
                downloadMonitor.cancel();
            } else {
                // echo what the download monitor is reporting.
                monitor.setTaskProgress(downloadMonitor.getTaskProgress());
            }

            try {
                downloads.wait(100); // wait 100ms, then proceed to support progress information
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            downloadMonitor = (ProgressMonitor) downloads.get(filename);
            
            if ( downloadMonitor!=null && downloadMonitor.isFinished() ) {
                logger.warning("watched downloadMonitor is finished but is not being unlocked");
                monitor.setProgressMessage("file is downloaded, just a moment");
            }            
        }
        
        monitor.finished();
    }

    /**
     * ID string for the process.
     */
    protected static final String id= String.format( "%014d_%s", System.currentTimeMillis(), ManagementFactory.getRuntimeMXBean().getName() );
            
    /**
     * return a name where the download is to be staged.  This will
     * be unique for any process.  We make the following assumptions:
     * <ul>
     * <li> multiple Java processes will be using and modifying the file database.
     * <li> java processes may be on different machines
     * <li> an ID conflict may occur should two processes have the same UID, hostname, and are started at the same clock time millisecond.
     * </ul>
     * See https://sourceforge.net/p/autoplot/bugs/1301/
     * @param localFile
     * @return the temporary filename to use.
     */
    public final File getPartFile( File localFile ) {
        return new File( localFile.toString()+ "." + id + ".part" );
    }
    
    /**
     * Request lock to download file.  If this thread gets the lock, then it
     * should download the file and call  mutatorLock.unlock() when the
     * download is complete.   If another thread is downloading the file, this
     * will block until the download is complete, and null will be returned to
     * indicate that the file has already been downloaded.  This must start the
     * monitor when it gets the lock.
     * 
     * @param filename the filename with in the filesystem.
     * @param f the File which will be the local copy.
     * @param monitor a monitor for the download.  If a MutatorLock is returned, then
     *    the monitor is not touched, but other threads may use it to keep track
     *    of the download progress.
     * @throws FileNotFoundException if the file wasn't found after another thread loaded the file.
     * @return Lock or null if another thread loaded the resource.  The client should call lock.unlock() when the download is complete
     */
    protected Lock getDownloadLock(final String filename, File f, ProgressMonitor monitor) throws IOException {
        logger.log(Level.FINER, "{0} wants download lock for {1} wfs impl {2}", new Object[]{Thread.currentThread().getName(), filename, this.hashCode()});
        synchronized (downloads) {
            ProgressMonitor mon = (ProgressMonitor) downloads.get(filename);
            if (mon != null) { // the webfilesystem is already loading this file, so wait.
                logger.log(Level.FINER, "another thread is downloading {0}, waiting...", filename);
                waitForDownload( monitor, filename );  //TODO: this seems strange, that we would have this in a synchronized block.
                if (f.exists()) {
                    return null;
                } else {
                    if ( monitor.isCancelled() ) {
                        throw new InterruptedIOException("request was cancelled");
                    } else {
                        throw new FileNotFoundException("expected to find " + f);
                    }
                }
            } else {
                logger.log(Level.FINER, "this thread will download {0}.", filename);
                downloads.put(filename, monitor);
                monitor.started();  // this is necessary for the other monitors
                return new LocalReentrantLock(filename);
            }
        }
    }
    
    private class LocalReentrantLock extends ReentrantLock {
        String filename;
        private LocalReentrantLock( String filename ) {
            this.filename= filename;
        }
        @Override
        public void lock() {
        }

        @Override
        public void unlock() {
            synchronized (downloads) {
                downloads.remove(filename);
                downloads.notifyAll();
            }
        }
    }

    /**
     * Transfers the file from the remote store to a local copy f.  This should only be
     * used within the class and subclasses, clients should use getFileObject( String ).getFile().
     * Subclasses implementing this should download data to partfile, then rename partfile to
     * f after the download is complete.
     *
     * Note, this is non-trivial, since several threads and even several processes may be using
     * the same area at once.  See HttpFileSystem's implementation of this before attempting to
     * implement the function.
     *
     * @param filename the name of the file, relative to the filesystem.
     * @param f the file to where the file is downloaded.
     * @param partfile the temporary file during download.
     * @param monitor progress monitor
     * @return metadata headers from the load, if any are available.  See WebProtocol.
     * @see WebProtocol#getMetadata(org.das2.util.filesystem.WebFileObject) 
     * @throws java.io.IOException
     */
    protected abstract Map<String,String> downloadFile(String filename, File f, File partfile, ProgressMonitor monitor) throws IOException;

    /** Get the root of the local file cache
     * @return the root of the local file cache
     * @deprecated use getLocalRoot().getAbsolutePath()
     */
    public String getLocalRootAbsPath() {
        return this.localRoot.getAbsolutePath();
    }

    @Override
    public File getLocalRoot() {
        return this.localRoot;
    }

    public synchronized void resetListingCache() {
        if ( !FileUtil.deleteWithinFileTree(localRoot,".listing") ) { // yikes.  This should probably not be in a synchronized block...
            throw new IllegalArgumentException("unable to delete all .listing files");
        }
        listings.clear();
        listingFreshness.clear();
    }

    /**
     * From FTPBeanFileSystem.
     * @param directory
     */
    public synchronized void resetListCache( String directory ) {
        directory = toCanonicalFolderName(directory);

        File f= new File(localRoot, directory + ".listing");
        if ( f.exists() && ! f.delete() ) {
            throw new IllegalArgumentException("unable to delete .listing file: "+f);
        }
        listings.remove( directory );
        listingFreshness.remove( directory );
    }

    /**
     * return the File for the cached listing, even if it does not exist.
     * @param directory
     * @return
     */
    protected File listingFile( String directory ) {
        File f= new File(localRoot, directory);
        try {
            FileSystemUtil.maybeMkdirs( f );
        } catch ( IOException ex ) {
            throw new IllegalArgumentException("unable to mkdir "+f,ex);
        }
        File listing = new File(localRoot, directory + ".listing");
        return listing;
    }

    private final Map<String,DirectoryEntry[]> listings= new HashMap();
    private final Map<String,Long> listingFreshness= new HashMap();

    /**
     * return true if the listing file (.listing) is available in the 
     * file system cache, and is still fresh.  LISTING_TIMEOUT_MS controls
     * the freshness, where files older than LISTING_TIMEOUT_MS milliseconds
     * will not be used.  Note the timestamp on the file comes from the server
     * providing the listing, so the age may be negative when clocks are not
     * synchronized.
     * @param directory
     * @return true if the listing is cached.
     */
    public synchronized boolean isListingCached( String directory ) {
        File f= new File(localRoot, directory);
        if ( !f.exists() ) return false;
        File listing = listingFile( directory );
        if ( listing.exists() ) {
            long ageMs= ( System.currentTimeMillis() - listing.lastModified() );
            if ( ageMs<LISTING_TIMEOUT_MS ) {
                logger.log( Level.FINE, "listing date is {0} millisec old", ageMs );
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public synchronized void cacheListing( String directory, DirectoryEntry[] listing ) {
         listings.put( directory, listing );
         listingFreshness.put( directory, System.currentTimeMillis() );
    }

    /**
     * list the directory using the ram memory cache.  MEMORY_LISTING_TIMEOUT_MS=4s limits the lifespan
     * of a cache entry, and this is really to avoid expensive listings of the same resource when searches are
     * done.
     * @param directory the directory name within the filesystem.
     * @return null or the listing.
     */
    protected synchronized DirectoryEntry[] listDirectoryFromMemory( String directory ) {
        directory= toCanonicalFilename(directory);
        Long freshness= listingFreshness.get(directory);
        if ( freshness==null ) return null;
        long ageMillis= System.currentTimeMillis()-freshness;
        if ( System.currentTimeMillis()-freshness < MEMORY_LISTING_TIMEOUT_MS ) {
            logger.log(Level.FINER, "list directory from memory for {0}", directory);
            DirectoryEntry [] result= listings.get(directory);
            return result;
        } else {
            logger.log(Level.FINER, "remove old ({0}ms) directory listing for {1}", new Object[] { ageMillis, directory } );
            listings.remove(directory);
            listingFreshness.remove(directory);
        }
        return null;
    }

    /**
     * trigger an update of the in-memory listing, or check to see if it is in memory.
     * @param filename the particular file for which we need a listing.
     * @param force if true, then list if it isn't available.
     * @return null if the listing is not available, or if the element is not in the folder, the DirectoryEntry otherwise.
     * @throws java.io.IOException when the directory is listed.
     */
    public DirectoryEntry maybeUpdateDirectoryEntry( String filename, boolean force ) throws IOException {
        // we should be able to get the listing that we just did from memory.
        String path= toCanonicalFilename(filename);
        int i= path.lastIndexOf("/");
        DirectoryEntry[] des= listDirectoryFromMemory(path.substring(0,i+1));
        int itry= 10;
        while ( force && des==null && itry-->0 ) {
            listDirectory(path.substring(0,i+1));
            des= listDirectoryFromMemory(path.substring(0,i+1));
        }
        if ( force && des==null ) {
            throw new IOException("unable to get listing: " + this.getRootURL() + path.substring(1,i+1) );
        }
        DirectoryEntry result= null;
        if ( des!=null ) {
            String fname= path.substring(i+1);
            for ( i=0; i<des.length; i++ ) {
                if ( fname.equals(des[i].name) ) {
                    result= des[i];
                }
            }
        }
        return result;
    }
    

    /**
     * return true if the name refers to a directory, not a file.
     * @param filename
     * @return
     * @throws IOException
     */
    @Override
    abstract public boolean isDirectory(String filename) throws IOException;

    /**
     * return the directory listing for the name.  
     * @param directory
     * @return
     * @throws IOException
     */
    @Override
    abstract public String[] listDirectory(String directory) throws IOException;

    @Override
    public String[] listDirectory(String directory, String regex) throws IOException {
        String[] names = listDirectory(directory);
        Pattern pattern = Pattern.compile(regex);
        ArrayList result = new ArrayList();
        for (int i = 0; i < names.length; i++) {
            if (names[i].endsWith("/")) {
                names[i] = names[i].substring(0, names[i].length() - 1);
            }
            if (pattern.matcher(names[i]).matches()) {
                result.add(names[i]);
            }
        }
        return (String[]) result.toArray(new String[result.size()]);
    }
    
    /**
     * return the URL for the internal filename
     * @param filename internal filename
     * @return 
     */
    public URL getURL(String filename) {
        filename = FileSystem.toCanonicalFilename(filename);
        try {
            return new URL( root.toURL(), FileSystemUtil.uriEncode(filename.substring(1)) );
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * return the URI for the internal filename
     * @param filename internal filename
     * @return 
     */
    public URI getURI( String filename ) {
        try {
            filename = FileSystem.toCanonicalFilename(filename);
            return new URI(root + FileSystemUtil.uriEncode(filename.substring(1)));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * return the root of the filesystem as a URL.
     * Since the root is stored as a URI and "ftp://jbf@mysite.com:@ftpproxy.net/temp/" is a legal address, check for this case.
     * @return the root of the filesystem as a URL.
     */
    public URL getRootURL() {
        try {
            return root.toURL();
        } catch (MalformedURLException ex) {

            String auth= root.getAuthority();
            String[] ss= auth.split("@");

            String userInfo= null;
            if ( ss.length>3 ) {
                throw new IllegalArgumentException("user info section can contain at most two at (@) symbols");
            } else if ( ss.length==3 ) {//bugfix 3299977.  UMich server uses email:password@umich.  Java doesn't like this.
                // the user didn't escape the at (@) in the email.  escape it here.
                StringBuilder userInfo_= new StringBuilder( ss[0] );
                for ( int i=1;i<2;i++ ) userInfo_.append("%40").append(ss[i]);
                auth= ss[2];
                try {
                    URI rooturi2= new URI( root.getScheme() + "://" + userInfo_.toString()+"@"+auth + root.getPath() );
                    return rooturi2.toURL();
                } catch ( URISyntaxException | MalformedURLException ex2 ) {
                    throw new RuntimeException(ex2);
                }
            } else {
                throw new RuntimeException(ex);
            }
            
        }
    }
    /**
     * return the name of the File within the FileSystem, where File is a local
     * file within the local copy of the filesystem.
     * @param file
     * @return the name within the filesystem
     */
    public String getLocalName(File file) {
        if (!file.toString().startsWith(localRoot.toString())) {
            throw new IllegalArgumentException("file \"" + file + "\"is not of this web file system");
        }
        String filename = file.toString().substring(localRoot.toString().length());
        filename = filename.replaceAll("\\\\", "/");
        return filename;
    }

    public String getLocalName(URL url) {
        if (!url.toString().startsWith(root.toString())) {
            throw new IllegalArgumentException("url \"" + url + "\"is not of this web file system");
        }
        String filename = FileSystem.toCanonicalFilename(url.toString().substring(root.toString().length()));
        return filename;
    }

    /**
     * Return the handle for this file.  This is not the file itself, and 
     * accessing this object does not necessarily download the resource.  This will be a 
     * container for file metadata, in addition to providing access to the data
     * file itself.
     * @param filename the name of the file within the filesystem.
     * @return a FileObject for the file
     */
    @Override
    public FileObject getFileObject(String filename) {
        return new WebFileObject( this, filename, new Date( Long.MAX_VALUE ) );  // note result.modified may be Long.MAX_VALUE, indicating need to load.
    }

    /**
     * reduce the number of hits to a server by caching last access times for local files.
     * Note subclasses of this must call markAccess to indicate the file is accessed.
     *
     * For example, we will not do a head request to check for an update more than once per minute.
     *
     * @param filename the filename within the filesystem.
     * @return the last time the file was accessed via a HEAD request.
     */
    protected synchronized long getLastAccessed( String filename ) {
        try {
            DirectoryEntry result = (DirectoryEntry) accessCache.doOp( filename );
            return result.modified;
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "returning 1970-01-01", ex);
            return 0;
        }
    }

    /**
     * copies data from in to out, sending the number of bytesTransferred to the monitor.
     * NOTE: monitor.finished is not called, breaking monitor rules.
     * @param is the input stream
     * @param out the output stream
     * @param monitor monitor for the task.  Note this violates the monitor policy, and only calls setTaskProgress.  Use with care!
     * @throws java.io.IOException
     */
    protected void copyStream(InputStream is, OutputStream out, ProgressMonitor monitor) throws IOException {
        long reportIncrementBytes= 1000000;
        byte[] buffer = new byte[2048];
        int bytesRead = is.read(buffer, 0, 2048);
        long totalBytesRead = bytesRead;
        long t0= System.currentTimeMillis();
        long reportSpeedTotalBytesRead= reportIncrementBytes;
        while (bytesRead > -1) {
            if (monitor.isCancelled()) {
                throw new InterruptedIOException();
            }
            monitor.setTaskProgress(totalBytesRead);
            out.write(buffer, 0, bytesRead);
            bytesRead = is.read(buffer, 0, 2048);
            totalBytesRead += bytesRead;
            if ( totalBytesRead>reportSpeedTotalBytesRead ) {
                if ( logger.isLoggable(Level.FINER ) ) {
                    String mbt= String.format( "%.1f MB", totalBytesRead / 1000000. );
                    String mbps= String.format( "%.2f MBytesPerSecond", ( totalBytesRead ) / ( ( System.currentTimeMillis()-t0 ) / 1000. ) / 1000000 );
                    logger.log(Level.FINER, "transferring data transferred={0} speed={1}", new Object[] { mbt, mbps } );
                    reportSpeedTotalBytesRead= (long) ( Math.ceil( ( totalBytesRead + 1 )/ reportIncrementBytes ) ) * reportIncrementBytes;
                }
            } else {
                logger.finest("transferring data");
            }
            
        }
    }

    /**
     * nice clients consume both the stderr and stdout coming from websites.
     * http://docs.oracle.com/javase/1.5.0/docs/guide/net/http-keepalive.html suggests that you "do not abandon connection"
     * @param err
     * @throws IOException 
     * @deprecated see HtmlUtil.consumeStream.
     * @see HtmlUtil#consumeStream(java.io.InputStream) 
     */
    public static void consumeStream( InputStream err ) throws IOException {
        HtmlUtil.consumeStream(err);
    }
    
    @Override
    public String toString() {
        return "wfs " + root;
    }

    public boolean isAppletMode() {
        return applet;
    }

    public void setAppletMode(boolean applet) {
        this.applet = applet;
    }
}
