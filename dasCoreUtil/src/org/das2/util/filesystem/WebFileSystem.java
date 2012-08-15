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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.util.monitor.CancelledOperationException;
import org.das2.util.monitor.ProgressMonitor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import org.das2.util.FileUtil;

/**
 * Base class for HTTP and FTP-based filesystems.  A local cache is kept of
 * the files.  
 * 
 * @author  Jeremy
 */
public abstract class WebFileSystem extends FileSystem {

    /**
     * we keep a cached listing in on disk.  This is backed by the the website.
     */
    public static final int LISTING_TIMEOUT_MS =      200000;

    /**
     * we keep a cached listing in memory for performance.  This is backed by the .listing file.
     */
    public static final int MEMORY_LISTING_TIMEOUT_MS= 60000;

    public static File getDownloadDirectory() {
        File local = FileSystem.settings().getLocalCacheDir();
        return local;
    }

    Map<String,Long> lastAccessed= new HashMap();

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
    protected boolean offline = false;
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


    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    /**
     * return the name of the folder containing a local copy of the cache.
     * @param localRoot
     * @return
     */
    private static File lookForROCache( File start ) {

        File localRoot= start;
        File stopFile= FileSystem.settings().getLocalCacheDir();
        File result= null;

        while ( !( localRoot.equals(stopFile) ) ) {
            File f= new File( localRoot, "ro_cache.txt" );
            if ( f.exists() ) {
                BufferedReader read = null;
                try {
                    read = new BufferedReader(new FileReader(f));
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
                    Logger.getLogger(WebFileSystem.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    try {
                        if ( read!=null ) read.close();
                    } catch (IOException ex) {
                        Logger.getLogger(WebFileSystem.class.getName()).log(Level.SEVERE, null, ex);
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


    /** Creates a new instance of WebFileSystem */
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

    }

    static protected File localRoot(URI root) {

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
            if (monitor.isCancelled()) {
                downloadMonitor.cancel();
            }

            if ( !monitor.isCancelled() ) {  //TODO: syncronized block or something
                // echo what the download monitor is reporting.
                monitor.setTaskProgress(downloadMonitor.getTaskProgress());
            }

            try {
                downloads.wait(100); // wait 100ms, then proceed to support progress information
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            downloadMonitor = (ProgressMonitor) downloads.get(filename);
            
        }
        
        monitor.finished();
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
     * @param mon a monitor for the download.  If a MutatorLock is returned, then
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
                logger.log(Level.FINE, "another thread is downloading {0}, waiting...", filename);
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
                logger.log(Level.FINE, "this thread will download {0}.", filename);
                downloads.put(filename, monitor);
                monitor.started();  // this is necessary for the other monitors

                return new ReentrantLock() {
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
                };

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
     */
    protected abstract void downloadFile(String filename, File f, File partfile, ProgressMonitor monitor) throws IOException;

    /** Get the root of the local file cache
     * @deprecated use getLocalRoot().getAbsolutePath()
     */
    public String getLocalRootAbsPath() {
        return this.localRoot.getAbsolutePath();
    }

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

    private Map<String,DirectoryEntry[]> listings= new HashMap();
    private Map<String,Long> listingFreshness= new HashMap();

    public synchronized boolean isListingCached( String directory ) {
        File listing = listingFile( directory );
        if ( listing.exists() && ( System.currentTimeMillis() - listing.lastModified() ) < LISTING_TIMEOUT_MS ) {
            logger.fine(String.format( "listing date is %f5.2 seconds old", (( System.currentTimeMillis() - listing.lastModified() ) /1000.) ));
            return true;
        } else {
            return false;
        }
    }

    public synchronized void cacheListing( String directory, DirectoryEntry[] listing ) {
         listings.put( directory, listing );
         listingFreshness.put( directory, new Long( System.currentTimeMillis() ) );
    }

    protected synchronized DirectoryEntry[] listDirectoryFromMemory( String directory ) {
        Long freshness= listingFreshness.get(directory);
        if ( freshness==null ) return null;
        if ( System.currentTimeMillis()-freshness < MEMORY_LISTING_TIMEOUT_MS ) {
            DirectoryEntry [] result= listings.get(directory);
            return result;
        } else {
            listings.remove(directory);
            listingFreshness.remove(directory);
        }
        return null;
    }

    abstract public boolean isDirectory(String filename) throws IOException;

    abstract public String[] listDirectory(String directory) throws IOException;

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

    public URL getURL(String filename) {
        try {
            filename = FileSystem.toCanonicalFilename(filename);
            return new URL(root + filename.substring(1));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public URI getURI( String filename ) {
        try {
            filename = FileSystem.toCanonicalFilename(filename);
            return new URI(root + filename.substring(1));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * return the root of the filesystem as a URL.
     * @return the root of the filesystem as a URL.
     */
    public URL getRootURL() {
        try {
            return root.toURL();
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }
    /**
     * return the name of the File within the FileSystem, where File is a local
     * file within the local copy of the filesystem.
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

    public FileObject getFileObject(String filename) {
        String path= toCanonicalFilename(filename);
        int i= path.lastIndexOf("/");

        String dir= path.substring(0, i+1);

        try {
            listDirectory(dir); // load the listing into memory to get file object
        } catch (IOException ex) {
            Logger.getLogger(WebFileSystem.class.getName()).log(Level.SEVERE, null, ex);
        }

        // we should be able to get the listing that we just did from memory.
        DirectoryEntry[] des= listDirectoryFromMemory(dir);
        DirectoryEntry result= null;

        if ( des!=null ) {
            String fname= path.substring(i+1);
            for ( i=0; i<des.length; i++ ) {
                if ( fname.equals(des[i].name) ) {
                    result= des[i];
                }
            }
        }

        if ( result==null ) {
            // this won't exist.
            return new WebFileObject( this, filename, new Date( Long.MAX_VALUE ) );
        } else {
            return new WebFileObject( this, filename, new Date( result.modified ) );
        }
    }

    /**
     * reduce the number of hits to a server by caching last access times for local files.
     * Note subclasses of this must call markAccess to indicate the file is accessed.
     *
     * @return
     */
    protected synchronized long getLastAccessed( String filename ) {
        Long lastAccess= lastAccessed.get(filename);
        if ( lastAccess==null ) {
            return 0;
        } else {
            return lastAccess.longValue();
        }
    }

    protected synchronized void markAccess( String filename ) {
        lastAccessed.put( filename, System.currentTimeMillis() );
    }

    /**
     * copies data from in to out, sending the number of bytesTransferred to the monitor.
     */
    protected void copyStream(InputStream is, OutputStream out, ProgressMonitor monitor) throws IOException {
        byte[] buffer = new byte[2048];
        int bytesRead = is.read(buffer, 0, 2048);
        long totalBytesRead = bytesRead;
        while (bytesRead > -1) {
            if (monitor.isCancelled()) {
                throw new InterruptedIOException();
            }
            monitor.setTaskProgress(totalBytesRead);
            out.write(buffer, 0, bytesRead);
            bytesRead = is.read(buffer, 0, 2048);
            totalBytesRead += bytesRead;
            logger.finest("transferring data");
        }
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
