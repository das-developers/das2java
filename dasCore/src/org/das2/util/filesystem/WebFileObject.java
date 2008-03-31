/*
 * WebFile.java
 *
 * Created on May 14, 2004, 10:06 AM
 */
package org.das2.util.filesystem;

import org.das2.util.monitor.ProgressMonitor;
import org.das2.util.monitor.NullProgressMonitor;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

/**
 *
 * @author  Jeremy
 *
 * This is a refactoring of the HttpFileObject, generalized for use with FTP and HTTP file objects.  Note that
 * the HttpFileObject has not been refactored to use this.
 *
 */
public class WebFileObject extends FileObject {

    WebFileSystem wfs;
    String pathname;
    File localFile;
    Date modifiedDate;
    boolean isRoot;
    boolean isFolder;
    /**
     * true if we know if it's a folder or not.
     */
    boolean isFolderResolved = false;

    public boolean canRead() {
        return true;
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
        if (isFolder) {
            throw new IllegalArgumentException("is a folder");
        }
        if (!localFile.exists()) {
            File partFile = new File(localFile.toString() + ".part");
            wfs.downloadFile(pathname, localFile, partFile, monitor);
        }
        return new FileInputStream(localFile);
    }

    /**
     * 
     * @return
     */
    public FileObject getParent() {
        return new WebFileObject(wfs, wfs.getLocalName(localFile.getParentFile()), new Date(System.currentTimeMillis()));
    }

    public long getSize() {
        if (isFolder) {
            throw new IllegalArgumentException("is a folder");
        }
        return localFile.length();
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
        return modifiedDate;
    }

    /**
     * returns the File that corresponds to the remote file.  This may or may
     * not exist, depending on whether it's been downloaded yet.
     */
    protected File getLocalFile() {
        return this.localFile;
    }

    public boolean exists() {
        if (localFile.exists()) {
            return true;
        } else {
            try {
                // TODO: use HTTP HEAD, etc
                Logger.getLogger("das2.filesystem").info("This implementation of WebFileObject.exists() is not optimal");
                File partFile = new File(localFile.toString() + ".part");
                wfs.downloadFile(pathname, localFile, partFile, new NullProgressMonitor());
                return localFile.exists();
            } catch (FileNotFoundException e) {
                return false;
            } catch (IOException e) {
                // I'm going to assume that it's because the file was not found. 404's from pw's server end up here
                return false;
            }
        }
    }

    protected WebFileObject( WebFileSystem wfs, String pathname, Date modifiedDate ) {
        this.localFile = new File(wfs.getLocalRoot(), pathname);

        this.localFile.deleteOnExit();
        this.modifiedDate = modifiedDate;

        this.wfs = wfs;
        this.pathname = pathname;

        try {
            if (!localFile.canRead()) {
                if (wfs.isDirectory(pathname)) {
                    localFile.mkdirs();
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
            this.isFolderResolved = false;
        }

    }

    public String toString() {
        return "[" + wfs + "]" + getNameExt();
    }

    public String getNameExt() {
        return pathname;
    }

    public java.nio.channels.ReadableByteChannel getChannel(ProgressMonitor monitor) throws FileNotFoundException, IOException {
        return ((FileInputStream) getInputStream(monitor)).getChannel();
    }

    public File getFile(ProgressMonitor monitor) throws FileNotFoundException, IOException {
        boolean download = false;

        Date remoteDate;
        if (wfs instanceof HttpFileSystem) {
            URL url = wfs.getURL(this.getNameExt());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.connect();
            remoteDate = new Date(connection.getLastModified());
        } else {
            // this is the old logic
            remoteDate = new Date(localFile.lastModified());
        }

        if (localFile.exists()) {
            Date localFileLastModified = new Date(localFile.lastModified());
            if (remoteDate.after(localFileLastModified)) {
                FileSystem.logger.info("remote file is newer than local copy of " + this.getNameExt() + ", download.");
                download = true;
            }
        } else {
            download = true;
        }

        if (download) {
            try {
                if (!localFile.getParentFile().exists()) {
                    localFile.getParentFile().mkdirs();
                }
                File partFile = new File(localFile.toString() + ".part");
                wfs.downloadFile(pathname, localFile, partFile, monitor);
            } catch (FileNotFoundException e) {
                // TODO: do something with part file.
                throw e;
            }
        }

        return localFile;

    }

    /**
     * returns true is the file is locally available, meaning clients can 
     * call getFile() and the readble File reference will be available in
     * interactive time.  For FileObjects from HttpFileSystem, a HEAD request
     * is made to ensure that the local file is as new as the website one.
     */
    public boolean isLocal() {
        try {
            boolean download = false;

            if (localFile.exists()) {
                Date remoteDate;
                if (wfs instanceof HttpFileSystem) {
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
                    FileSystem.logger.info("remote file is newer than local copy of " + this.getNameExt() + ", download.");
                    download = true;
                }

            } else {
                download = true;
            }

            return !download;

        } catch (IOException e) {
            return false;
        }
    }
}