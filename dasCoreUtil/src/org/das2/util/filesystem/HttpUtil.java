
package org.das2.util.filesystem;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.util.LoggerManager;
import org.das2.util.monitor.CancelledOperationException;

/**
 * Utilities for HTTP protocol, such as a cache for the HEAD metadata.
 * @author jbf
 */
public final class HttpUtil {

    
    private final static Logger logger= LoggerManager.getLogger( "das2.filesystem.http.util" );
    
    /**
     * this logger is for opening connections to remote sites.
     */
    protected static final Logger loggerUrl= org.das2.util.LoggerManager.getLogger( "das2.url" );
    
    private static class MetadataRecord {
        Map<String,String> metadata;
        long birthMilli;
    }
    
    
    private static final Map<String, MetadataRecord> cache = 
            Collections.synchronizedMap(new HashMap<String, MetadataRecord>());

    /**
     * nice clients consume both the stderr and stdout coming from websites.
     * This reads everything off of the stream and closes it.
     * http://docs.oracle.com/javase/1.5.0/docs/guide/net/http-keepalive.html 
     * suggests that you "do not abandon connection"
     * @param err the input stream
     * @throws IOException
     */
    public static void consumeStream(InputStream err) throws IOException {
        byte[] buf = new byte[2048];
        try {
            if (err != null) {
                int ret = 0;
                while ((ret = err.read(buf)) > 0) {
                    // empty out the error stream.
                }
            }
        } finally {
            if (err != null) {
                err.close();
            }
        }
    }

    /**
     * return the metadata about a URL.  This will support http, https,
     * and ftp, and will check for redirects.  This will
     * allow caching of head requests.  
     * @param url ftp,https, or http URL
     * @param props if non-null, may be a map containing cookie.
     * @return the metadata
     * @throws java.io.IOException when HEAD requests are made.
     * @see WebProtocol#META_EXIST
     * @see WebProtocol#HTTP_RESPONSE_CODE
     */
    public static Map<String, String> getMetadata(
            URL url, 
            Map<String, String> props) throws IOException {
        long ageMillis = Long.MAX_VALUE;
        String surl = url.toString();
        MetadataRecord mr;
        synchronized ( cache) {
            mr = cache.get(surl);
            if (mr != null) {
                ageMillis = System.currentTimeMillis() - mr.birthMilli;
            }
            if (mr != null && (ageMillis < WebFileSystem.LISTING_TIMEOUT_MS)) {
                if (mr.metadata != null) {
                    logger.log(Level.FINE, "using cached metadata for {0}", url);
                    return mr.metadata;
                }
            }
            if (mr == null) {
                mr = new MetadataRecord();
                mr.birthMilli = System.currentTimeMillis();
                mr.metadata = null;
                cache.put(surl, mr);
            }
        }
        synchronized (mr) {
            if (mr.metadata != null) {
                ageMillis = System.currentTimeMillis() - mr.birthMilli;
                if (ageMillis < WebFileSystem.LISTING_TIMEOUT_MS) {
                    return mr.metadata;
                }
            }
            logger.log(Level.FINE, "reading metadata for {0}", url);
            Map<String, String> theResult;
            if (!url.getProtocol().equals("ftp")) {
                boolean exists;
                
                HttpURLConnection connect = (HttpURLConnection) url.openConnection();
                connect.setConnectTimeout(5000);
                    
                //connect.setDefaultUseCaches(false);
                //connect.setUseCaches(false);
                
                connect.setRequestMethod("HEAD");
                //connect= (HttpURLConnection) HttpUtil.checkRedirect(connect);

                //connect.setDefaultUseCaches(false);
                //connect.setUseCaches(false);
                //connect.setRequestProperty("Cache-Control", "max-age=0"); 
                     // attempts to get github.com to send fresh headers.
                try {
                    String encode = KeyChain.getDefault().getUserInfoBase64Encoded(url);
                    if (encode != null) {
                        connect.setRequestProperty("Authorization", "Basic " + encode);
                    }
                } catch (CancelledOperationException ex) {
                    logger.log(Level.INFO, "user cancelled auth dialog");
                    // this is what we would do before.
                }
                if (props != null) {
                    String cookie = props.get(WebProtocol.META_COOKIE);
                    if (cookie != null) {
                        connect.setRequestProperty(WebProtocol.META_COOKIE, cookie);
                    }
                }
                //HttpURLConnection.setFollowRedirects(true);
                //connect = (HttpURLConnection) HttpUtil.checkRedirect(connect);
                FileSystem.loggerUrl.log(Level.FINE, "HEAD to get metadata: {0}", new Object[]{url});
                connect.connect();
                int responseCode= connect.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM 
                        || responseCode == HttpURLConnection.HTTP_MOVED_TEMP 
                        || responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                    connect= (HttpURLConnection) HttpUtil.checkRedirect(connect);
                }
                exists = connect.getResponseCode() != 404;
                Map<String, String> result = new HashMap<>();
                Map<String, List<String>> fields = connect.getHeaderFields();
                for (Map.Entry<String, List<String>> e : fields.entrySet()) {
                    String key = e.getKey();
                    if ( key!=null ) {
                        List<String> value = e.getValue();
                        result.put(key, value.get(0));
                    }
                }
                result.put(WebProtocol.HTTP_RESPONSE_CODE,String.valueOf(responseCode));
                result.put(WebProtocol.META_EXIST, String.valueOf(exists));
                result.put(WebProtocol.META_LAST_MODIFIED, String.valueOf(connect.getLastModified()));
                result.put(WebProtocol.META_CONTENT_LENGTH, String.valueOf(connect.getContentLength()));
                result.put(WebProtocol.META_CONTENT_TYPE, connect.getContentType());
                result.put(WebProtocol.META_ETAG, connect.getHeaderField("ETag") );
                
                logger.log(Level.FINE, "URL: {0}", url);
                logger.log(Level.FINE, "ETag: {0}", connect.getHeaderField("ETag"));
                //connect.disconnect();
                theResult = result;
            } else {
                Map<String, String> result = new HashMap<>();
                URLConnection urlc = url.openConnection();
                try {
                    FileSystem.loggerUrl.log(Level.FINE, "FTP connection: {0}", new Object[]{url});
                    urlc.connect();
                    urlc.getInputStream().close();
                    result.put(WebProtocol.META_EXIST, "true");
                } catch (IOException ex) {
                    result.put(WebProtocol.META_EXIST, "false");
                }
                theResult = result;
            }
            mr.birthMilli = System.currentTimeMillis();
            mr.metadata = theResult;
            return theResult;
        }
    }

    /**
     * copy over connection properties like Cookie and Accept-Encoding.
     * @param urlc
     * @param newConnection 
     */
    public static void copyConnectProperties(HttpURLConnection urlc, HttpURLConnection newConnection) {
        String s;
        s= urlc.getHeaderField("Referer");
        if ( s!=null ) newConnection.addRequestProperty( "Referer", s );
        s= urlc.getHeaderField("Cookie");
        if ( s!=null ) newConnection.addRequestProperty( "Cookie", s );
        s= urlc.getHeaderField("Accept-Encoding");
        if ( s!=null ) newConnection.addRequestProperty( "Accept-Encoding", s );
        s= urlc.getHeaderField("Authorization");
        if ( s!=null ) newConnection.addRequestProperty( "Authorization", s );
        try {
            String requestMethod = urlc.getRequestMethod();
            if ( requestMethod!=null ) {
                newConnection.setRequestMethod( requestMethod );
            }
        } catch (ProtocolException ex) {
            throw new RuntimeException(ex);
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
    public static URLConnection checkRedirect(URLConnection urlConnection) throws IOException {
        if (urlConnection instanceof HttpURLConnection) {
            HttpURLConnection huc = (HttpURLConnection) urlConnection;
            huc.setInstanceFollowRedirects(true);
            huc.setConnectTimeout( FileSystem.settings().getConnectTimeoutMs() );
            huc.setReadTimeout( FileSystem.settings().getReadTimeoutMs() );
            loggerUrl.log(Level.FINEST, "getResponseCode {0}", urlConnection.getURL());
            int responseCode = huc.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM 
                    || responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                    || responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                String newUrl = huc.getHeaderField("Location");
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
                    if ( "true".equals(System.getProperty("log_redirects"))) {
                        File localCache= FileSystem.settings().getLocalCacheDir();
                        File redirectLog= new File( localCache, "redirect.log" );
                        TimeZone tz = TimeZone.getTimeZone("UTC");
                        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
                        df.setTimeZone(tz);
                        String nowAsISO = df.format(new Date());
                        synchronized(redirectLog) {
                            try ( BufferedWriter write= new BufferedWriter( new FileWriter(redirectLog,true) ) ) {
                                write.append(nowAsISO);
                                write.append(" ");
                                write.append(urlConnection.getURL().toExternalForm());
                                write.append(" ");
                                write.append(newUrl);
                                write.newLine();
                            }
                        }
                    }
                    logger.log(Level.INFO, "URL {0} permanently moved to {1}", 
                            new Object[]{urlConnection.getURL(), newUrl});
                } else {
                    loggerUrl.log(Level.FINE, "{0} redirect to {1}", 
                            new Object[] { responseCode, newUrl } );
                }

                HttpURLConnection newUrlConnection = 
                        (HttpURLConnection) new URL(newUrl).openConnection();
                copyConnectProperties( huc, newUrlConnection );
                newUrlConnection.addRequestProperty("Referer", 
                        urlConnection.getURL().toString());
                ((HttpURLConnection) urlConnection).disconnect();
                urlConnection = newUrlConnection;
            }
            return urlConnection;
        } else {
            return urlConnection;
        }
    }
    
}
