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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
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

    /** Creates a new instance of WebFileSystem */
    private HttpFileSystem(URI root, File localRoot) {
        super(root, localRoot);
    }

    public static HttpFileSystem createHttpFileSystem(URI rooturi) throws FileSystemOfflineException, UnknownHostException {
        try {

            String auth= rooturi.getAuthority();
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

            boolean doCheck= true;
            URI parentURI= FileSystemUtil.getParentUri( rooturi );
            if ( parentURI!=null ) {
                HttpFileSystem parent= (HttpFileSystem) peek( parentURI );
                if ( parent!=null && parent.isOffline() ) {
                    logger.fine("parent is offline, don't check...");
                    doCheck= false;
                }
            }


            boolean offline = true;
            if ( doCheck && !FileSystem.settings().isOffline() ) {
                // verify URL is valid and accessible
                HttpURLConnection urlc = (HttpURLConnection) root.openConnection();
                urlc.setConnectTimeout( FileSystem.settings().getConnectTimeoutMs() );

                //urlc.setRequestMethod("HEAD"); // Causes problems with the LANL firewall.

                String userInfo= null;

                try {
                    userInfo = KeyChain.getDefault().getUserInfo(root);
                } catch (CancelledOperationException ex) {
                    logger.log( Level.FINE, "user cancelled credentials for {0}", rooturi);
                    throw new FileSystemOfflineException("user cancelled credentials for "+rooturi );
                }
                if ( userInfo != null) {
                    String encode = Base64.encodeBytes( userInfo.getBytes());
                    urlc.setRequestProperty("Authorization", "Basic " + encode);
                }

                boolean connectFail= true;

                byte[] buf= new byte[2048];
                try {
                    urlc.connect();
                    InputStream is = urlc.getInputStream();
                    int ret = 0;
                    while ((ret = is.read(buf)) > 0) { //http://docs.oracle.com/javase/1.5.0/docs/guide/net/http-keepalive.html suggests that you "do not abandon connection"
                       // empty out the input stream.
                    }
                    is.close();
                    connectFail= false;
                } catch ( IOException ex ) {
                    int code= 0;
                    try {
                        code= urlc.getResponseCode();
                    } catch ( IOException ex2 ) {
                        // do nothing in this case, just try to get a response code.
                        logger.log(Level.SEVERE,null,ex2);
                    }
                    if ( code==401 ) {
                        connectFail= false;
                    } else {
                        logger.log( Level.SEVERE, String.format( "%d: failed to connect to %s", code, root ), ex );
                        if ( FileSystem.settings().isAllowOffline() ) {
                            logger.info("remote filesystem is offline, allowing access to local cache.");
                        } else {
                            throw new FileSystemOfflineException("" + urlc.getResponseCode() + ": " + urlc.getResponseMessage());
                        }
                        InputStream err = urlc.getErrorStream();
                        if ( err!=null ) {
                            int ret = 0;
                            while ((ret = err.read(buf)) > 0) {
                               // empty out the error stream.
                            }
                            err.close();
                        }
                    }
                }

                if ( !connectFail ) {
                    if (urlc.getResponseCode() != HttpURLConnection.HTTP_OK && urlc.getResponseCode() != HttpURLConnection.HTTP_FORBIDDEN) {
                        if ( urlc.getResponseCode()==HttpURLConnection.HTTP_UNAUTHORIZED ) {
                            // might be nice to modify URL so that credentials are used.
                            KeyChain.getDefault().clearUserPassword(root);
                            if ( userInfo==null ) {
                                String port=  root.getPort()==-1 ? "" : ( ":" +root.getPort() );
                                URL rootAuth= new URL( root.getProtocol() + "://" + "user@" + root.getHost() + port + root.getFile() );
                                try {
                                    URI rootAuthUri= rootAuth.toURI();
                                    return createHttpFileSystem( rootAuthUri );
                                } catch ( URISyntaxException ex ) {
                                    throw new RuntimeException(ex);
                                }
                            }
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

        Lock lock = getDownloadLock(filename, f, monitor);

        if (lock == null) {
            return;
        }

        logger.log(Level.FINE, "downloadFile({0})", filename);

        try {
            URL remoteURL = new URL(root.toString() + filename);

            URLConnection urlc = remoteURL.openConnection();
            urlc.setConnectTimeout( FileSystem.settings().getConnectTimeoutMs() );

            //TODO: consider setting the following:
            //urlc.setUseCaches(false);

            String userInfo;
            try {
                userInfo = KeyChain.getDefault().getUserInfo(root);
            } catch (CancelledOperationException ex) {
                throw new IOException("user cancelled at credentials entry");
            }
            if ( userInfo != null) {
                String encode = Base64.encodeBytes(userInfo.getBytes());
                urlc.setRequestProperty("Authorization", "Basic " + encode);
            }

            HttpURLConnection hurlc = (HttpURLConnection) urlc;
            if (hurlc.getResponseCode() == 404) {
                logger.log(Level.INFO, "{0} URL: {1}", new Object[]{hurlc.getResponseCode(), remoteURL});
                throw new FileNotFoundException("not found: " + remoteURL);
            } else if (hurlc.getResponseCode() != 200) {
                logger.log(Level.INFO, "{0} URL: {1}", new Object[]{hurlc.getResponseCode(), remoteURL});
                throw new IOException( hurlc.getResponseCode()+": "+ hurlc.getResponseMessage() + "\n"+remoteURL );
            }

            Date d=null;
            List<String> sd= urlc.getHeaderFields().get("Last-Modified");
            if ( sd!=null && sd.size()>0 ) {
                d= new Date( sd.get(sd.size()-1) );
            }
            

            monitor.setTaskSize(urlc.getContentLength());

            if (!f.getParentFile().exists()) {
                logger.log(Level.FINE, "make dirs {0}", f.getParentFile());
                FileSystemUtil.maybeMkdirs( f.getParentFile() );
            }
            if (partFile.exists()) {
                logger.log(Level.FINE, "clobber file {0}", f);
                if (!partFile.delete()) {
                    logger.log(Level.INFO, "Unable to clobber file {0}, better use it for now.", f); //TODO: review this
                    return;
                }
            }

            if (partFile.createNewFile()) {
                InputStream in;
                in = urlc.getInputStream();

                logger.log(Level.FINE, "transferring bytes of {0}", filename);
                FileOutputStream out = new FileOutputStream(partFile);
                monitor.setLabel("downloading file");
                monitor.started();
                try {
                    copyStream(in, out, monitor);
                    monitor.finished();
                    out.close();
                    in.close();
                    if ( d!=null ) {
                        try {
                            partFile.setLastModified(d.getTime()+10); // add 10 secs because of bad experiences with Windows filesystems.  Also this is probably a good idea in case local clock is not set properly.
                        } catch ( Exception ex ) {
                            logger.log( Level.SEVERE, "unable to setLastModified", ex );
                        }
                    }
                    if ( f.exists() ) {
                        logger.log(Level.FINE, "deleting old file {0}", f);
                        if ( ! f.delete() ) {
                            throw new IllegalArgumentException("unable to delete "+f );
                        }
                    }
                    if ( !partFile.renameTo(f) ) {
                        throw new IllegalArgumentException( "rename failed " + partFile + " to "+f );
                    }
                } catch (IOException e) {
                    out.close();
                    in.close();
                    if ( partFile.exists() && !partFile.delete() ) {
                        throw new IllegalArgumentException("unable to delete "+partFile );
                    }
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
    protected Map<String, Object> getHeadMeta(String f) throws IOException, CancelledOperationException {
        String realName = f;
        boolean exists;
        try {
            URL ur = new URL(this.root.toURL(), f);
            HttpURLConnection connect = (HttpURLConnection) ur.openConnection();
            String userInfo= KeyChain.getDefault().getUserInfo(ur);
            if ( userInfo != null) {
                String encode = Base64.encodeBytes(userInfo.getBytes());
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

    /**
     * Allow the local RO Cache to contain files that are not yet in the remote filesystem, to support the case
     * where a data provider tests locally available products before mirroring them out to the public website.
     * @param directory
     * @param remoteList
     * @return
     */
    private Map<String,DirectoryEntry> addRoCacheEntries( String directory, Map<String,DirectoryEntry> remoteList ) {
        File f= this.getReadOnlyCache();
        if ( f!=null ) {
            String[] ss= new File( f, directory ).list();
            if ( ss==null ) return remoteList;
            List<DirectoryEntry> add= new ArrayList<DirectoryEntry>();
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

    /**
     * list the directory, using the cached entry from listDirectoryFromMemory, or
     * by HtmlUtil.getDirectoryListing.  If there is a ro_cache, then add extra entries from here as well.
     * @param directory
     * @return
     * @throws IOException
     */
    public String[] listDirectory(String directory) throws IOException {

        DirectoryEntry[] cached= listDirectoryFromMemory( directory );
        if ( cached!=null ) {
            return FileSystem.getListing( cached );
        }

        if ( protocol!=null && protocol instanceof AppletHttpProtocol ) { // support applets.  This could check for local write access, but DefaultHttpProtocol shows a problem here too.
            InputStream in=null;
            URL[] list;
            try {
                in= protocol.getInputStream( new WebFileObject(this,directory,new Date() ), new NullProgressMonitor() );
                list= HtmlUtil.getDirectoryListing( getURL(directory), in );
            } catch ( CancelledOperationException ex ) {
                throw new IllegalArgumentException(ex); //TODO: this should probably be IOException(ex).  See use 20 lines below as well.
            } finally {
                if ( in!=null ) in.close();
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
            logger.log(Level.FINE, "using cached listing for {0}", directory);

            File listing= listingFile(directory);
            
            URL[] list=null;
            FileInputStream fin=null;
            try {
                fin= new FileInputStream(listing);
                list = HtmlUtil.getDirectoryListing(getURL(directory), fin );
            } catch (CancelledOperationException ex) {
                throw new IllegalArgumentException(ex); // shouldn't happen since it's local
            } finally {
                if ( fin!=null ) fin.close();
            }
            
            result = new LinkedHashMap(list.length);
            int n = directory.length();
            for (int i = 0; i < list.length; i++) {
                URL url = list[i];
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
            logger.log(Level.FINE, "this filesystem is offline, using local listing: {0}", f);

            if ( !f.exists() ) throw new FileSystemOfflineException("unable to list "+f+" when offline");
            File[] listing = f.listFiles();

            int n= f.toString().length();
            List<String> result1= new ArrayList();
            for ( int i=0; i<listing.length; i++ ) {
                File f1= listing[i];
                if ( f1.getName().endsWith(".listing") ) continue;
                if ( f1.isDirectory() ) {
                    result1.add( f1.getName() + "/" );
                } else {
                    result1.add( f1.getName() );
                }
            }
            result= addRoCacheEntries( directory, new LinkedHashMap() );
            for ( DirectoryEntry f1: result.values() ) {
                if ( f1.type=='d' ) {
                    result1.add( f1.name + "/" );
                } else {
                    result1.add( f1.name );
                }
            }
            return result1.toArray( new String[result1.size()] );
        }


        while ( !successOrCancel ) {
            logger.log(Level.FINE, "list {0}", directory);
            URL[] list;
            try {
                URL listUrl= getURL(directory);

                String file= listUrl.getFile();
                if ( file.charAt(file.length()-1)!='/' ) {
                    listUrl= new URL( listUrl.toString()+'/' );
                }

                File listing= listingFile( directory );

                downloadFile( directory, listing, new File( listing.toString()+".part" ), new NullProgressMonitor() );

                FileInputStream fin=null;
                try {
                    fin= new FileInputStream(listing);
                    list = HtmlUtil.getDirectoryListing( getURL(directory), fin );
                } finally {
                    if ( fin!=null ) fin.close();
                }

                result = new LinkedHashMap();
                int n = directory.length();
                for (int i = 0; i < list.length; i++) {
                    URL url = list[i];
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
