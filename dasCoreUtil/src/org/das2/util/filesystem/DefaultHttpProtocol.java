
package org.das2.util.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.das2.util.monitor.ProgressMonitor;

/**
 * This http protocol uses the local cache.
 *
 * @author jbf
 */
public class DefaultHttpProtocol implements WebProtocol {

    @Override
    public InputStream getInputStream(WebFileObject fo, ProgressMonitor mon) throws IOException {
        if (fo.isFolder) {
            throw new IllegalArgumentException("is a folder");
        }
        File ff= fo.getFile(mon);
        return new FileInputStream(ff);
    }

    @Override
    public Map<String, String> getMetadata(WebFileObject fo) throws IOException {
        if (!fo.wfs.getRootURL().getProtocol().equals("ftp")) {
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

            result.put(META_EXIST, String.valueOf(exists));

            connect.disconnect();

            return result;

        } else {
            
            Map<String, String> result = new HashMap<String, String>();

            URL url= new URL( fo.wfs.getRootURL(), fo.pathname );
            URLConnection urlc = url.openConnection();
            try { 
                urlc.connect();
                urlc.getInputStream().close();
                result.put( META_EXIST, "true" );
                
            } catch ( IOException ex ) {
                result.put( META_EXIST, "false" );
            }
            return result;
            
        }
    }
}
