
package org.das2.util.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.util.LoggerManager;
import org.das2.util.monitor.ProgressMonitor;

/**
 * This http protocol uses the local cache.
 *
 * @author jbf
 */
public class DefaultHttpProtocol implements WebProtocol {
    
    private static final Logger logger= LoggerManager.getLogger( "das2.filesystem" );
    
    @Override
    public InputStream getInputStream(WebFileObject fo, ProgressMonitor mon) throws IOException {
        if (fo.isFolder) {
            throw new IllegalArgumentException("is a folder");
        }
        File ff= fo.getFile(mon);
        return new FileInputStream(ff);
    }

    /**
     * encode each element of the path.  Slashes are left alone.
     * @param realName
     * @return 
     */
    private static String urlEncodeSansSlash( String realName ) {
        String[] ss= realName.split("/",-2);
        for ( int i=0; i<ss.length; i++ ) {
            try {
                ss[i]= URLEncoder.encode(ss[i],"UTF-8");
            } catch (UnsupportedEncodingException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        StringBuilder builder= new StringBuilder();
        int i=0;
        while ( ss[i].length()==0 && i<ss.length ) i++;
        builder.append(ss[i]);
        for ( i++; i<ss.length; i++ ) {
            if ( ss[i].length()>0 ) {
                builder.append("/").append(ss[i]);
            }
        }
        return builder.toString();
    }
    
    @Override
    public Map<String, String> getMetadata(WebFileObject fo) throws IOException {
        if (!fo.wfs.getRootURL().getProtocol().equals("ftp")) {
            String realName = fo.pathname;
            boolean exists;
            URL ur = new URL(fo.wfs.getRootURL(), urlEncodeSansSlash(realName).replaceAll("\\+", "%20") );
            //URL ur = new URL(fo.wfs.getRootURL(), URLEncoder.encode(realName,"UTF-8").replaceAll("\\+", "%20") );
            HttpURLConnection connect = (HttpURLConnection) ur.openConnection();
            connect.setRequestMethod("HEAD");
            HttpURLConnection.setFollowRedirects(false);
            
            if ( fo.wfs instanceof HttpFileSystem ) {
                HttpFileSystem hfs= ((HttpFileSystem)fo.wfs);
                if ( hfs.getCookie()!=null ) {
                    connect.setRequestProperty("Cookie", hfs.getCookie());
                }
            }
            FileSystem.loggerUrl.log(Level.FINE, "HEAD to get metadata: {0}", new Object[] { ur } );
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
                FileSystem.loggerUrl.log(Level.FINE, "HEAD to get metadata after 303: {0}", new Object[] { ur } );
                connect.connect();
            }
            exists = connect.getResponseCode() != 404;

            Map<String, String> result = new HashMap<>();

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
            
            Map<String, String> result = new HashMap<>();

            URL url= new URL( fo.wfs.getRootURL(), fo.pathname );
            URLConnection urlc = url.openConnection();
            try { 
                FileSystem.loggerUrl.log(Level.FINE, "FTP connection: {0}", new Object[] { url } );
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
