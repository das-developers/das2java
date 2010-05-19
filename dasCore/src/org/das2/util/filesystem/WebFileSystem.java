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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.util.monitor.ProgressMonitor;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import org.das2.system.MutatorLock;

/**
 * Base class for HTTP and FTP-based filesystems.  A local cache is kept of
 * the files.  
 * 
 * @author  Jeremy
 */
public abstract class WebFileSystem extends FileSystem {

    public static File getDownloadDirectory() {
        File local;
        if (System.getProperty("user.name").equals("Web")) {
            local = new File("/tmp");
        } else {
            local = new File(System.getProperty("user.home"));
        }
        local = new File(local, ".das2/fsCache/wfs/");

        return local;
    }
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
    protected boolean offline = true;
    /**
     * if true, then the remote filesystem is not accessible, but local cache
     * copies may be accessed.  See FileSystemSettings.allowOffline
     */
    public static final String PROP_OFFLINE = "offline";

    public boolean isOffline() {
        return offline;
    }

    public void setOffline(boolean offline) {
        boolean oldOffline = offline;
        this.offline = offline;
        propertyChangeSupport.firePropertyChange(PROP_OFFLINE, oldOffline, offline);
    }
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
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
        }
    }

    static protected File localRoot(URI root) {

        File local = FileSystem.settings().getLocalCacheDir();

        String s = root.getScheme() + "/" + root.getHost() + "/" + root.getPath(); //TODO: check getPath

        local = new File(local, s);

        local.mkdirs();
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

            // echo what the download monitor is reporting.
            monitor.setTaskProgress(downloadMonitor.getTaskProgress());

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
     * @return MutatorLock.  The client should call mutatorLock.unlock() when the download is complete
     */
    protected MutatorLock getDownloadLock(final String filename, File f, ProgressMonitor monitor) throws IOException {
        logger.finer("" + Thread.currentThread().getName() + " wants download lock for " + filename + " wfs impl " + this.hashCode());
        synchronized (downloads) {
            ProgressMonitor mon = (ProgressMonitor) downloads.get(filename);
            if (mon != null) { // the webfilesystem is already loading this file, so wait.
                logger.fine("another thread is downloading " + filename + ", waiting...");
                waitForDownload( monitor, filename );
                if (f.exists()) {
                    return null;
                } else {
                    throw new FileNotFoundException("expected to find " + f);
                }
            } else {
                logger.fine("this thread will download " + filename + ".");
                downloads.put(filename, monitor);
                monitor.started();  // this is necessary for the other monitors

                return new MutatorLock() {

                    public void lock() {
                    }

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
        WebFileObject f = new WebFileObject(this, filename, new Date(System.currentTimeMillis()));
        return f;
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
