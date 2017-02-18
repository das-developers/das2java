/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.util.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.util.LoggerManager;
import org.das2.util.monitor.CancelledOperationException;

/**
 * Utilities for HTTP protocol, such as a cache for the HEAD metadata.
 * @author jbf
 */
public final class HttpUtil {

    
    private final static Logger logger= LoggerManager.getLogger( "das2.filesystem" );
    
    /**
     * this logger is for opening connections to remote sites.
     */
    protected static final Logger loggerUrl= org.das2.util.LoggerManager.getLogger( "das2.url" );
    
    private static class MetadataRecord {
        Map<String,String> metadata;
        long birthMilli;
    }
    
    
    private static final Map<String, MetadataRecord> cache = Collections.synchronizedMap(new HashMap<String, MetadataRecord>());

    /**
     * nice clients consume both the stderr and stdout coming from websites.
     * This reads everything off of the stream and closes it.
     * http://docs.oracle.com/javase/1.5.0/docs/guide/net/http-keepalive.html suggests that you "do not abandon connection"
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
     */
    public static Map<String, String> getMetadata(URL url, Map<String, String> props) throws IOException {
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
                connect.setRequestMethod("HEAD");
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
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                    connect= (HttpURLConnection) HttpUtil.checkRedirect(connect);
                }
                exists = connect.getResponseCode() != 404;
                Map<String, String> result = new HashMap<>();
                Map<String, List<String>> fields = connect.getHeaderFields();
                for (Map.Entry<String, List<String>> e : fields.entrySet()) {
                    String key = e.getKey();
                    List<String> value = e.getValue();
                    result.put(key, value.get(0));
                }
                result.put(WebProtocol.META_EXIST, String.valueOf(exists));
                result.put(WebProtocol.META_LAST_MODIFIED, String.valueOf(connect.getLastModified()));
                result.put(WebProtocol.META_CONTENT_LENGTH, String.valueOf(connect.getContentLength()));
                result.put(WebProtocol.META_CONTENT_TYPE, connect.getContentType());
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
            loggerUrl.log(Level.FINEST, "getResponseCode {0}", urlConnection.getURL());
            int responseCode = huc.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                String newUrl = huc.getHeaderField("Location");
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
                    logger.log(Level.FINE, "URL {0} permanently moved to {1}", new Object[]{urlConnection.getURL(), newUrl});
                }
                String cookie = huc.getHeaderField("Cookie");
                String acceptEncoding = huc.getRequestProperty("Accept-Encoding");
                String authorization = huc.getRequestProperty("Authorization");
                String requestMethod = huc.getRequestMethod();
                HttpURLConnection newUrlConnection = (HttpURLConnection) new URL(newUrl).openConnection();
                newUrlConnection.addRequestProperty("Referer", urlConnection.getURL().toString());
                if (cookie != null) {
                    newUrlConnection.setRequestProperty("Cookie", cookie);
                }
                if (acceptEncoding != null) {
                    newUrlConnection.setRequestProperty("Accept-Encoding", acceptEncoding);
                }
                if (authorization != null) {
                    newUrlConnection.setRequestProperty("Authorization", authorization);
                }
                if (requestMethod != null) {
                    newUrlConnection.setRequestMethod(requestMethod);
                }
                ((HttpURLConnection) urlConnection).disconnect();
                urlConnection = newUrlConnection;
            }
            return urlConnection;
        } else {
            return urlConnection;
        }
    }
    
}
