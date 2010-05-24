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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.das2.DasApplication;
import org.das2.util.Base64;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.util.filesystem.FileSystem.FileSystemOfflineException;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import org.das2.system.MutatorLock;

/**
 *
 * @author  Jeremy
 */
public class HttpFileSystem extends WebFileSystem {

    private HashMap listings;

    /** Creates a new instance of WebFileSystem */
    private HttpFileSystem(URI root, File localRoot) {
        super(root, localRoot);
        listings = new HashMap();
    }

    public static synchronized HttpFileSystem createHttpFileSystem(URI rooturi) throws FileSystemOfflineException, UnknownHostException {
        try {
            URL root= rooturi.toURL();

            // verify URL is valid and accessible
            HttpURLConnection urlc = (HttpURLConnection) root.openConnection();
            urlc.setConnectTimeout(3000);

            urlc.setRequestMethod("HEAD");
            String userInfo= KeyChain.getDefault().getUserInfo(root);
            if ( userInfo != null) {
                String encode = Base64.encodeBytes( userInfo.getBytes());
                urlc.setRequestProperty("Authorization", "Basic " + encode);
            }

            boolean offline = true;
            urlc.connect();
            if (urlc.getResponseCode() != HttpURLConnection.HTTP_OK && urlc.getResponseCode() != HttpURLConnection.HTTP_FORBIDDEN) {
                if ( urlc.getResponseCode()==HttpURLConnection.HTTP_UNAUTHORIZED ) {
                    // might be nice to modify URL so that credentials are used.
                    KeyChain.getDefault().clearUserPassword(root);
                }
                if (FileSystem.settings().isAllowOffline()) {
                    logger.info("remote filesystem is offline, allowing access to local cache.");
                } else {
                    throw new FileSystemOfflineException("" + urlc.getResponseCode() + ": " + urlc.getResponseMessage());
                }
            } else {
                offline = false;
            }

            File local;

            if (DasApplication.hasAllPermission()) {
                local = localRoot(rooturi);
                logger.finer("initializing httpfs " + root + " at " + local);
            } else {
                local = null;
                logger.finer("initializing httpfs " + root + " in applet mode");
            }
            HttpFileSystem result = new HttpFileSystem(rooturi, local);
            result.offline = offline;

            return result;

        } catch (FileSystemOfflineException e) {
            throw e;
        } catch (UnknownHostException e) {
            throw e;
        } catch (IOException e) {
            throw new FileSystemOfflineException(e,rooturi);
        }

    }

    protected void downloadFile(String filename, File f, File partFile, ProgressMonitor monitor) throws IOException {

        MutatorLock lock = getDownloadLock(filename, f, monitor);

        if (lock == null) {
            return;
        }

        logger.fine("downloadFile(" + filename + ")");

        try {
            URL remoteURL = new URL(root.toString() + filename);

            URLConnection urlc = remoteURL.openConnection();
            String userInfo= KeyChain.getDefault().getUserInfo(root);
            if ( userInfo != null) {
                String encode = new String(Base64.encodeBytes( userInfo.getBytes()));
                urlc.setRequestProperty("Authorization", "Basic " + encode);
            }

            HttpURLConnection hurlc = (HttpURLConnection) urlc;
            if (hurlc.getResponseCode() == 404) {
                logger.info("" + hurlc.getResponseCode() + " URL: " + remoteURL);
                throw new FileNotFoundException("not found: " + remoteURL);
            } else if (hurlc.getResponseCode() != 200) {
                logger.info("" + hurlc.getResponseCode() + " URL: " + remoteURL);
                throw new IOException(hurlc.getResponseMessage());
            }

            monitor.setTaskSize(urlc.getContentLength());

            if (!f.getParentFile().exists()) {
                logger.fine("make dirs " + f.getParentFile());
                f.getParentFile().mkdirs();
            }
            if (partFile.exists()) {
                logger.fine("clobber file " + f);
                if (!partFile.delete()) {
                    logger.info("Unable to clobber file " + f + ", better use it for now.");
                    return;
                }
            }

            if (partFile.createNewFile()) {
                InputStream in;
                in = urlc.getInputStream();

                logger.fine("transferring bytes of " + filename);
                FileOutputStream out = new FileOutputStream(partFile);
                monitor.setLabel("downloading file");
                monitor.started();
                try {
                    copyStream(in, out, monitor);
                    monitor.finished();
                    out.close();
                    in.close();
                    partFile.renameTo(f);
                } catch (IOException e) {
                    out.close();
                    in.close();
                    partFile.delete();
                    throw e;
                }
            } else {
                throw new IOException("couldn't create local file: " + f);
            }
        } finally {

            lock.unlock();

        }
    }

    /**
     * this is introduced to support checking if the symbol foo/bar is a folder by checking
     * for a 303 redirect.
     *   EXIST->Boolean
     *   REAL_NAME->String
     * @param f
     * @throws java.io.IOException
     */
    protected Map<String, Object> getHeadMeta(String f) throws IOException {
        String realName = f;
        boolean exists;
        try {
            URL ur = new URL(this.root.toURL(), f);
            HttpURLConnection connect = (HttpURLConnection) ur.openConnection();
            String userInfo= KeyChain.getDefault().getUserInfo(ur);
            if ( userInfo != null) {
                String encode = new String(Base64.encodeBytes( userInfo.getBytes()));
                connect.setRequestProperty("Authorization", "Basic " + encode);
            }
            connect.setRequestMethod("HEAD");
            HttpURLConnection.setFollowRedirects(false);
            connect.connect();
            HttpURLConnection.setFollowRedirects(true);
            // check for rename, which means we'll do another request
            if (connect.getResponseCode() == 303) {
                String surl = connect.getHeaderField("Location");
                if (surl.startsWith(root.toString())) {
                    realName = surl.substring(root.toString().length());
                }
                connect.disconnect();
                ur = new URL(this.root.toURL(), realName);
                connect = (HttpURLConnection) ur.openConnection();
                connect.setRequestMethod("HEAD");
                connect.connect();
            }
            exists = connect.getResponseCode() != 404;

            Map<String, Object> result = new HashMap<String, Object>();
            result.putAll(connect.getHeaderFields());
            connect.disconnect();

            return result;

        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(ex);
        }

    }

    /** dumb method looks for / in parent directory's listing.  Since we have
     * to list the parent, then IOException can be thrown.
     * 
     */
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
                for (int i = 0; i < list.length; i++) {
                    if (list[i].equals(lookFor)) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    public String[] listDirectory(String directory) throws IOException {
        directory = this.toCanonicalFilename(directory);
        if (!isDirectory(directory)) {
            throw new IllegalArgumentException("is not a directory: " + directory);
        }

        if (!directory.endsWith("/")) {
            directory = directory + "/";
        }
        synchronized (listings) {
            if (listings.containsKey(directory)) {
                return (String[]) listings.get(directory);
            } else {

                URL[] list = HtmlUtil.getDirectoryListing(getURL(directory));
                String[] result = new String[list.length];
                int n = directory.length();
                for (int i = 0; i < list.length; i++) {
                    URL url = list[i];
                    result[i] = getLocalName(url).substring(n);
                }
                listings.put(directory, result);
                return result;
            }
        }
    }

    public String[] listDirectory(String directory, String regex) throws IOException {
        directory = toCanonicalFilename(directory);
        if (!isDirectory(directory)) {
            throw new IllegalArgumentException("is not a directory: " + directory);
        }

        String[] listing = listDirectory(directory);
        Pattern pattern = Pattern.compile(regex + "/?");
        ArrayList result = new ArrayList();
        for (int i = 0; i < listing.length; i++) {
            if (pattern.matcher(listing[i]).matches()) {
                result.add(listing[i]);
            }
        }
        return (String[]) result.toArray(new String[result.size()]);

    }
}
