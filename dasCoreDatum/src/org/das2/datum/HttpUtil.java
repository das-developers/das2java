
package org.das2.datum;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilities for HTTP protocol, such handling redirects.
 * This was needed to support Orbits.
 * @see org.das2.util.filesystem#HttpUtil
 * @author jbf
 */
public final class HttpUtil {

    
    private final static Logger logger= LoggerManager.getLogger( "das2.filesystem" );
    
    /**
     * this logger is for opening connections to remote sites.
     */
    protected static final Logger loggerUrl= org.das2.datum.LoggerManager.getLogger( "das2.url" );
    
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
            huc.setConnectTimeout( 5000 );
            huc.setReadTimeout( 5000 );
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
                loggerUrl.log(Level.FINE, "redirect to {0}", newUrl);
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
