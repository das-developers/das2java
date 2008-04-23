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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.util.Base64;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.util.filesystem.FileSystem.FileSystemOfflineException;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

/**
 *
 * @author  Jeremy
 */
public class HttpFileSystem extends WebFileSystem {

    private HashMap listings;
    private static HashMap instances = new HashMap();
    /**
     * Keep track of active downloads.  This handles, for example, the case
     * where the same file is requested several times by different threads.
     */
    private HashMap downloads = new HashMap();
    private String userPass;

    /** Creates a new instance of WebFileSystem */
    private HttpFileSystem(URL root, File localRoot) {
        super(root, localRoot);
        listings = new HashMap();
    }

    public static synchronized HttpFileSystem createHttpFileSystem(URL root) throws FileSystemOfflineException {
        if (instances.containsKey(root.toString())) {
            logger.finer("reusing " + root);
            return (HttpFileSystem) instances.get(root.toString());
        } else {
            try {
                // verify URL is valid and accessible
                HttpURLConnection urlc = (HttpURLConnection) root.openConnection();
                urlc.setRequestMethod("HEAD");
                if (root.getUserInfo() != null) {
                    String encode = Base64.encodeBytes(root.getUserInfo().getBytes());
                    // xerces String encode= new String( Base64.encode(root.getUserInfo().getBytes()) );
                    urlc.setRequestProperty("Authorization", "Basic " + encode);
                }
                urlc.connect();
                if (urlc.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new FileSystemOfflineException("" + urlc.getResponseCode() + ": " + urlc.getResponseMessage());
                }
                File local = localRoot(root);
                logger.finer("initializing httpfs " + root + " at " + local);
                HttpFileSystem result = new HttpFileSystem(root, local);
                instances.put(root.toString(), result);
                return result;
            } catch (FileSystemOfflineException e) {
                throw e;
            } catch (IOException e) {
                throw new FileSystemOfflineException(e);
            }
        }
    }

    protected void downloadFile(String filename, File f, File partFile, ProgressMonitor monitor) throws IOException {

        logger.fine("downloadFile(" + filename + ")");

        boolean waitForAnother;
        synchronized (downloads) {
            ProgressMonitor mon = (ProgressMonitor) downloads.get(filename);
            if (mon != null) { // the httpFS is already loading this file, so wait.

                monitor.setProgressMessage("Waiting for file to download");
                while (mon != null) {
                    while (!mon.isStarted()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }
                    }
                    monitor.setTaskSize(mon.getTaskSize());
                    monitor.started();
                    if (monitor.isCancelled()) {
                        mon.cancel();
                    }
                    monitor.setTaskProgress(mon.getTaskProgress());
                    try {
                        downloads.wait(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    mon = (ProgressMonitor) downloads.get(filename);
                    logger.finest("waiting for download");

                }
                monitor.finished();
                if (f.exists()) {
                    return;
                } else {
                    throw new FileNotFoundException("expected to find " + f);
                }
            } else {
                downloads.put(filename, monitor);
                waitForAnother = false;
            }
        }

        try {
            logger.info("downloadFile " + filename);

            URL remoteURL = new URL(root.toString() + filename);

            URLConnection urlc = remoteURL.openConnection();
            if (root.getUserInfo() != null) {
                String encode = new String(Base64.encodeBytes(root.getUserInfo().getBytes()));
                urlc.setRequestProperty("Authorization", "Basic " + encode);
            }

            HttpURLConnection hurlc = (HttpURLConnection) urlc;
            if (hurlc.getResponseCode() != 200) {
                System.err.println("" + hurlc.getResponseCode() + " URL: " + remoteURL);
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
            synchronized (downloads) {
                downloads.remove(filename);
                downloads.notifyAll();
            }
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
    protected Map<String,Object> getHeadMeta( String f ) throws IOException {
        String realName= f;
        boolean exists;
        try {
            URL ur = new URL(this.root, f);
            HttpURLConnection connect = (HttpURLConnection) ur.openConnection();
            connect.setRequestMethod("HEAD");
            HttpURLConnection.setFollowRedirects(false);
            connect.connect();
            HttpURLConnection.setFollowRedirects(true);
            // check for rename, which means we'll do another request
            if ( connect.getResponseCode()==303 ) {
                String surl= connect.getHeaderField("Location");
                if ( surl.startsWith(root.toString()) ) {
                    realName= surl.substring(root.toString().length());
                }
                connect.disconnect();
                ur = new URL(this.root, realName);
                connect = (HttpURLConnection) ur.openConnection();
                connect.setRequestMethod("HEAD");
                connect.connect();
            }
            exists= connect.getResponseCode()!=404; 
            
            Map<String,Object> result= new HashMap<String,Object>();
            result.putAll( connect.getHeaderFields() );
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
