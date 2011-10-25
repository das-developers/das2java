/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
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

/**
 * uses HTTP, and doesn't download resources to cache
 * @author jbf
 */
public class AppletHttpProtocol implements WebProtocol {

    public InputStream getInputStream(WebFileObject fo, org.das2.util.monitor.ProgressMonitor mon) throws IOException {
        HttpURLConnection connect = (HttpURLConnection) fo.wfs.getURL(fo.pathname).openConnection();
        connect.connect();
        int len = connect.getContentLength();
        DasProgressMonitorInputStream in = new DasProgressMonitorInputStream(connect.getInputStream(), mon);
        if (len != -1)
            in.setStreamLength(len);
        return in;
    }

    public Map<String, String> getMetadata(WebFileObject fo) throws IOException {
        String realName = fo.pathname;
        boolean exists;

        URL ur = new URL(fo.wfs.getRootURL(), realName);
        HttpURLConnection connect = (HttpURLConnection) ur.openConnection();
        connect.setRequestMethod("HEAD");
        HttpURLConnection.setFollowRedirects(false);
        connect.connect();
        HttpURLConnection.setFollowRedirects(true);
        // check for rename, which means we'll do another request
        if (connect.getResponseCode() == 303) {
            String surl = connect.getHeaderField("Location");
            if (surl.startsWith(fo.wfs.root.toString())) {
                realName = surl.substring(fo.wfs.root.toString().length());
            }
            connect.disconnect();
            ur = new URL(fo.wfs.getRootURL(), realName);
            connect = (HttpURLConnection) ur.openConnection();
            connect.setRequestMethod("HEAD");
            connect.connect();
        }
        exists = connect.getResponseCode() != 404;

        Map<String, String> result = new HashMap<String, String>();

        Map<String, List<String>> fields = connect.getHeaderFields();
        for (Entry<String,List<String>> e : fields.entrySet()) {
            String key= e.getKey();
            List<String> value = e.getValue();
            result.put(key, value.get(0));
        }

        result.put( META_EXIST, String.valueOf(exists) );
        
        connect.disconnect();

        return result;

    }
}
