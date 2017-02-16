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
 * HtmlUtil.java
 *
 * Created on May 14, 2004, 9:06 AM
 */

package org.das2.util.filesystem;

import java.util.logging.Level;
import org.das2.util.monitor.CancelledOperationException;
import org.das2.util.Base64;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.das2.util.LoggerManager;
import org.das2.util.filesystem.WebProtocol;

/**
 *
 * @author  Jeremy
 */
public class HtmlUtil {

    private final static Logger logger= LoggerManager.getLogger( "das2.filesystem" );
    /**
     * this logger is for opening connections to remote sites.
     */
    protected static final Logger loggerUrl= org.das2.util.LoggerManager.getLogger( "das2.url" );
    
    public static boolean isDirectory( URL url ) {
        String file= url.getFile();
        return file.charAt(file.length()-1) != '/';
    }

    /**
     * nice clients consume both the stderr and stdout coming from websites.
     * This reads everything off of the stream and closes it.
     * http://docs.oracle.com/javase/1.5.0/docs/guide/net/http-keepalive.html suggests that you "do not abandon connection"
     * @param err the input stream
     * @throws IOException 
     */
    public static void consumeStream( InputStream err ) throws IOException {
        byte[] buf= new byte[2048];
        try {
            if ( err!=null ) {
                int ret = 0;
                while ((ret = err.read(buf)) > 0) {
                   // empty out the error stream.
                }
            }
        } finally {
            if ( err!=null ) err.close();
        }
    }
    
    /**
     * Get the listing of the web directory, returning links that are "under" the given URL.
     * Note this does not handle off-line modes where we need to log into
     * a website first, as is often the case for a hotel.
     *
     * This was refactored to support caching of listings by simply writing the content to disk.
     *
     * @param url the address.
     * @param urlStream stream containing the URL content, which must be UTF-8 (or US-ASCII)
     * @return list of URIs referred to in the page.
     * @throws IOException
     * @throws CancelledOperationException
     */
    public static URL[] getDirectoryListing( URL url, InputStream urlStream ) throws IOException, CancelledOperationException {    
        return getDirectoryListing( url, urlStream, true );
    }
    
    /**
     * Get the listing of the web directory, returning links that are "under" the given URL.
     * Note this does not handle off-line modes where we need to log into
     * a website first, as is often the case for a hotel.
     *
     * This was refactored to support caching of listings by simply writing the content to disk.
     *
     * @param url the address.
     * @param urlStream stream containing the URL content, which must be UTF-8 (or US-ASCII)
     * @param childCheck only return links to URLs "under" the url.
     * @return list of URIs referred to in the page.
     * @throws IOException
     * @throws CancelledOperationException
     */
    public static URL[] getDirectoryListing( URL url, InputStream urlStream, boolean childCheck ) throws IOException, CancelledOperationException {
        // search the input stream for links
        // first, read in the entire URL

        long t0= System.currentTimeMillis();
        byte b[] = new byte[10000];
        int numRead = urlStream.read(b);  // i18n  also decorator in script to make plot
        StringBuilder contentBuffer = new StringBuilder( 10000 );

        if ( numRead!=-1 ) contentBuffer.append( new String( b, 0, numRead, "UTF-8" ) );
        while (numRead != -1) {
            FileSystem.logger.finest("download listing");
            numRead = urlStream.read(b);
            if (numRead != -1) {
                String newContent = new String(b, 0, numRead, "UTF-8");
                contentBuffer.append( newContent );
            }
        }
        urlStream.close();

        logger.log(Level.FINER, "read listing data in {0} millis", (System.currentTimeMillis() - t0));
        String content= contentBuffer.toString();

        String hrefRegex= "(?i)href\\s*=\\s*([\"'])(.+?)\\1";
        Pattern hrefPattern= Pattern.compile( hrefRegex );

        Matcher matcher= hrefPattern.matcher( content );

        ArrayList urlList= new ArrayList();

        String surl= url.toString();

        while ( matcher.find() ) {
            FileSystem.logger.finest("parse listing");
            String strLink= matcher.group(2);
            URL urlLink;

            try {
                urlLink = new URL(url, URLDecoder.decode(strLink,"UTF-8") );
                strLink = urlLink.toString();
            } catch (MalformedURLException e) {
                logger.log(Level.SEVERE, "bad URL: {0} {1}", new Object[]{url, strLink});
                continue;
            }

            if ( childCheck ) {
                if ( strLink.startsWith(surl) && strLink.length() > surl.length() && null==urlLink.getQuery() ) {
                    String file= strLink.substring( surl.length() );
                    if ( !file.startsWith("../") ) {
                        urlList.add( urlLink );
                    }
                }
            } else {
                urlList.add( urlLink );
            }
        }

        return (URL[]) urlList.toArray( new URL[urlList.size()] );

    }

    /**
     * Get the listing of the web directory, returning links that are "under" the given URL.
     * Note this does not handle off-line modes where we need to log into
     * a website first, as is often the case for a hotel.
     * @param url
     * @return list of URIs referred to in the page.
     * @throws IOException
     * @throws CancelledOperationException
     */
    public static URL[] getDirectoryListing( URL url ) throws IOException, CancelledOperationException {

        FileSystem.logger.log(Level.FINER, "listing {0}", url);
        
        String file= url.getFile();
        if ( file.charAt(file.length()-1)!='/' ) {
            url= new URL( url.toString()+'/' );
        }
        
        InputStream urlStream= getInputStream(url);
        
        return getDirectoryListing( url, urlStream );
    }
    
    /**
     * get the inputStream, following redirects if a 301 or 302 is encountered.  The scientist may be
     * prompted for a password.
     * @param url
     * @return input stream
     * @throws IOException 
     * @throws org.das2.util.monitor.CancelledOperationException 
     */
    public static InputStream getInputStream( URL url ) throws IOException, CancelledOperationException {

        loggerUrl.log(Level.FINE, "getInputStream {0}", new Object[] { url } );
        
        long t0= System.currentTimeMillis();
        
        String userInfo= KeyChain.getDefault().getUserInfo(url);

        //long t0= System.currentTimeMillis();
        loggerUrl.log(Level.FINE, "openConnect {0}", new Object[] { url } );
        URLConnection urlConnection = url.openConnection();

        urlConnection.setAllowUserInteraction(false);
        urlConnection.setConnectTimeout(FileSystem.settings().getConnectTimeoutMs() );
        
        logger.log(Level.FINER, "connected in {0} millis", (System.currentTimeMillis() - t0));
        if ( userInfo != null) {
            String encode = Base64.encodeBytes( userInfo.getBytes());
            urlConnection.setRequestProperty("Authorization", "Basic " + encode);
        }
                
        urlConnection= checkRedirect( urlConnection );
        return urlConnection.getInputStream();
           
    }

    private static class MetadataRecord {
        Map<String,String> metadata;
        long birthMilli;
    }
    
    private static final Map<URL,MetadataRecord> cache= Collections.synchronizedMap( new HashMap<URL,MetadataRecord>() );
    
    /**
     * return the metadata about a URL.  This will support http, https,
     * and ftp, and will check for redirects.  This will 
     * allow caching of head requests.
     * TODO: locking
     * @param url ftp,https, or http URL
     * @param props, if non-null, may be a map containing cookie.
     * @return the metadata
     * @throws java.io.IOException when HEAD requests are made.
     */
    public static Map<String,String> getMetadata( URL url, Map<String,String> props ) throws IOException {
        
        long ageMillis=Long.MAX_VALUE;
        
        MetadataRecord mr;
        synchronized ( cache ) {
            mr= cache.get(url);
            if ( mr!=null ) {
                ageMillis=  System.currentTimeMillis() - mr.birthMilli;
            }
            if ( mr!=null && ( ageMillis< WebFileSystem.LISTING_TIMEOUT_MS ) ) {
                if ( mr.metadata!=null ) {
                    logger.log(Level.FINE, "using cached metadata for {0}", url);
                    return mr.metadata;
                }
            }
            if ( mr==null ) {
                mr= new MetadataRecord();
                mr.birthMilli= System.currentTimeMillis();
                mr.metadata= null;
                cache.put(url,mr);
            }
        }
        
        synchronized ( mr ) {
            
            if ( mr.metadata!=null ) {
                ageMillis=  System.currentTimeMillis() - mr.birthMilli;
                if ( ageMillis<WebFileSystem.LISTING_TIMEOUT_MS ) {
                    return mr.metadata;
                }
            }
        
            logger.log(Level.FINE, "reading metadata for {0}", url);
            Map<String,String> theResult;

            if (!url.getProtocol().equals("ftp")) {

                boolean exists;

                HttpURLConnection connect = (HttpURLConnection) url.openConnection();
                connect.setRequestMethod("HEAD");
                HttpURLConnection.setFollowRedirects(false);

                try {
                    String encode= KeyChain.getDefault().getUserInfoBase64Encoded(url);
                    if ( encode!=null ) {
                        connect.setRequestProperty("Authorization", "Basic " + encode);
                    }
                } catch (CancelledOperationException ex) {
                    logger.log(Level.INFO,"user cancelled auth dialog");
                    // this is what we would do before.
                }

                if ( props!=null ) {
                    String cookie= props.get(WebProtocol.META_COOKIE);
                    if ( cookie!=null ) {
                        connect.setRequestProperty(WebProtocol.META_COOKIE, cookie );
                    }
                }

                HttpURLConnection.setFollowRedirects(true);
                connect= (HttpURLConnection)HtmlUtil.checkRedirect(connect);

                FileSystem.loggerUrl.log(Level.FINE, "HEAD to get metadata: {0}", new Object[] { url } );
                connect.connect();

                exists = connect.getResponseCode() != 404;

                Map<String, String> result = new HashMap<>();

                Map<String, List<String>> fields = connect.getHeaderFields();
                for (Map.Entry<String,List<String>> e : fields.entrySet()) {
                    String key= e.getKey();
                    List<String> value = e.getValue();
                    result.put(key, value.get(0));
                }

                result.put( WebProtocol.META_EXIST, String.valueOf(exists) );
                result.put( WebProtocol.META_LAST_MODIFIED, String.valueOf( connect.getLastModified() ) );
                result.put( WebProtocol.META_CONTENT_LENGTH, String.valueOf( connect.getContentLength() ) );
                result.put( WebProtocol.META_CONTENT_TYPE,connect.getContentType() );

                theResult= result;

            } else {

                Map<String, String> result = new HashMap<>();

                URLConnection urlc = url.openConnection();
                try { 
                    FileSystem.loggerUrl.log(Level.FINE, "FTP connection: {0}", new Object[] { url } );
                    urlc.connect();
                    urlc.getInputStream().close();
                    result.put( WebProtocol.META_EXIST, "true" );

                } catch ( IOException ex ) {
                    result.put( WebProtocol.META_EXIST, "false" );
                }

                theResult= result;

            }

            mr.birthMilli= System.currentTimeMillis();
            mr.metadata= theResult;

            return theResult;
        }
        
    }
    
    /**
     * check for 301, 302 or 303 redirects, and return a new connection in this case.
     * This should be called immediately before the urlConnection.connect call,
     * as this must connect to get the response code.
     * @param urlConnection if an HttpUrlConnection, check for 301 or 302; return connection otherwise.
     * @return a connection, typically the same one as passed in.
     * @throws IOException 
     */
    public static URLConnection checkRedirect( URLConnection urlConnection ) throws IOException {
        if ( urlConnection instanceof HttpURLConnection ) {
            HttpURLConnection huc= ((HttpURLConnection)urlConnection);
            huc.setInstanceFollowRedirects(true);
             
            loggerUrl.fine("getResponseCode "+urlConnection.getURL());
            int responseCode=  huc.getResponseCode();
            if ( responseCode==HttpURLConnection.HTTP_MOVED_PERM 
                    || responseCode==HttpURLConnection.HTTP_MOVED_TEMP 
                    || responseCode==HttpURLConnection.HTTP_SEE_OTHER ) {
                String newUrl = huc.getHeaderField("Location");
                String cookie= huc.getHeaderField("Cookie");
                String acceptEncoding= huc.getRequestProperty( "Accept-Encoding" );
                String requestMethod= huc.getRequestMethod();
                HttpURLConnection newUrlConnection = (HttpURLConnection) new URL(newUrl).openConnection();
                newUrlConnection.addRequestProperty("Referer", urlConnection.getURL().toString() );
                if ( cookie!=null ) newUrlConnection.setRequestProperty( "Cookie", cookie );
                if ( acceptEncoding!=null ) newUrlConnection.setRequestProperty("Accept-Encoding",acceptEncoding);
                if ( requestMethod!=null ) {
                    newUrlConnection.setRequestMethod(requestMethod);
                }
                urlConnection= newUrlConnection;
            }
        
            return urlConnection;
            
        } else {
            
            return urlConnection;
            
        }
    }
    
}
