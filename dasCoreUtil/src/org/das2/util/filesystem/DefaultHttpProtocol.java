
package org.das2.util.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
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
    protected static String urlEncodeSansSlash( String realName ) {
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
        
        URL ur = new URL( fo.wfs.getRootURL(), urlEncodeSansSlash(fo.pathname).replaceAll("\\+", "%20") );
        if ( fo.wfs.offline ) {
            Map<String,String> result= new HashMap<>();
            result.put(WebProtocol.META_EXIST, String.valueOf( fo.localFile.exists() ) );
            result.put(WebProtocol.META_LAST_MODIFIED, String.valueOf( fo.localFile.lastModified() ) );
            result.put(WebProtocol.META_CONTENT_LENGTH, String.valueOf( fo.localFile.length() ) );
            result.put(WebProtocol.META_CONTENT_TYPE, Files.probeContentType( fo.localFile.toPath() ) );
            return result;
            
        } else {
            return HttpUtil.getMetadata( ur, null );
        }
    }
}
