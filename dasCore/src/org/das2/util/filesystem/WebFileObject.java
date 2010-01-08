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

import java.text.ParseException;
import java.util.logging.Level;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.util.monitor.NullProgressMonitor;
import java.io.*;
import java.lang.String;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.text.DateFormat;
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
    Map<String,String> metadata;

    /**
     * true if we know if it's a folder or not.
     */
    boolean isFolderResolved = false;

    public boolean canRead() {
        return true;
    }

    synchronized void maybeLoadMetadata() throws IOException {
        if ( metadata==null ) {
            metadata= wfs.protocol.getMetadata( this );
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
        if ( wfs.protocol !=null ) {
            return wfs.protocol.getInputStream(this, monitor);
        }
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
     * @return a WebFileObject referencing the parent directory.
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
        if (modifiedDate == null) {
            try {
                Map<String, String> meta = wfs.protocol.getMetadata(this);
                String stime = meta.get(WebProtocol.META_LAST_MODIFIED);
                if (stime == null) {
                    modifiedDate = new Date();
                } else {
                    try {
                        modifiedDate = DateFormat.getDateInstance().parse(stime);
                    } catch (ParseException ex) {
                        Logger.getLogger(WebFileObject.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    //modifiedDate = new Date(Date.parse(stime));
                    return modifiedDate;
                }
            } catch (IOException ex) {
                Logger.getLogger(WebFileObject.class.getName()).log(Level.SEVERE, null, ex);
                return new Date();
            }

        }
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
                if ( wfs.protocol!=null ) {
                    maybeLoadMetadata();
                    return "true".equals( metadata.get( WebProtocol.META_EXIST ) );
                } else {
                    // TODO: use HTTP HEAD, etc
                    Logger.getLogger("das2.filesystem").fine("This implementation of WebFileObject.exists() is not optimal");
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

        boolean download = false;

        if ( monitor==null ) throw new NullPointerException("monitor may not be null");
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
                FileSystem.logger.fine("remote file is newer than local copy of " + this.getNameExt() + ", download.");
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
        if ( wfs.isAppletMode() ) return false;
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
                    FileSystem.logger.fine("remote file is newer than local copy of " + this.getNameExt() + ", download.");
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