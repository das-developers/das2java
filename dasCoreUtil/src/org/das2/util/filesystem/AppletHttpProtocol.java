
package org.das2.util.filesystem;

import org.das2.util.DasProgressMonitorInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

/**
 * uses HTTP, and doesn't download resources to cache
 * @author jbf
 */
public class AppletHttpProtocol implements WebProtocol {

    @Override
    public InputStream getInputStream(WebFileObject fo, org.das2.util.monitor.ProgressMonitor mon) throws IOException {
        HttpURLConnection connect = (HttpURLConnection) fo.wfs.getURL(fo.pathname).openConnection();
        connect.connect();
        int len = connect.getContentLength();
        FileSystem.loggerUrl.log(Level.FINE, "getInputStream {0}", new Object[] { connect.getURL() } );
        DasProgressMonitorInputStream in = new DasProgressMonitorInputStream(connect.getInputStream(), mon);
        if (len != -1)
            in.setStreamLength(len);
        return in;
    }

    @Override
    public Map<String, String> getMetadata(WebFileObject fo) throws IOException {
        String realName = fo.pathname;
        boolean exists;

        URL ur = new URL(fo.wfs.getRootURL(), realName);
        
        FileSystem.loggerUrl.log(Level.FINE, "openConnection {0}", new Object[] { ur } );
        HttpURLConnection connect = (HttpURLConnection) ur.openConnection();
        connect.setRequestMethod("HEAD");
        connect= (HttpURLConnection)HtmlUtil.checkRedirect(connect);
        exists = connect.getResponseCode() != 404;

        Map<String, String> result = new HashMap<>();

        Map<String, List<String>> fields = connect.getHeaderFields();
        for (Entry<String,List<String>> e : fields.entrySet()) {
            String key= e.getKey();
            List<String> value = e.getValue();
            result.put(key, value.get(0));
        }

        result.put( META_EXIST, String.valueOf(exists) );
        
        //connect.disconnect();

        return result;

    }
}
