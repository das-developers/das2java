/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * template for web-based protocols to implement FileSystems
 * @author jbf
 */
public interface WebProtocol {
    public static final String META_EXIST="exist";
    public static final String META_COOKIE="Cookie";
    public static final String META_LAST_MODIFIED= "LastModified";
    public static final String META_CONTENT_LENGTH= "ContentLength";
    public static final String META_CONTENT_TYPE= "ContentType";
    public static final String META_ETAG= "ETag";
    
    /**
     * kludge in place where the response code (200,302,401,etc) can be stored.
     */
    public static final String HTTP_RESPONSE_CODE= "_ResponseCode";
    
    /**
     * get an inputStream for the resource.  The client using the stream must make sure the stream is closed.
     * @param fo the resource
     * @param mon monitor for the stream.
     * @return the stream
     * @throws IOException 
     */
    public InputStream getInputStream( WebFileObject fo, org.das2.util.monitor.ProgressMonitor mon ) throws IOException;
    
    /**
     * return metadata for the resource.  This should include:
     * <ul>
     * <li>WebProtocol.META_EXIST, "true" or "false"
     * <li>WebProtocol.META_LAST_MODIFIED, String.valueOf( fo.localFile.lastModified() )
     * <li>WebProtocol.META_CONTENT_LENGTH, String.valueOf( fo.localFile.length() )
     * <li>WebProtocol.META_CONTENT_TYPE, Files.probeContentType( fo.localFile.toPath() ) )
     * </ul>
     * @param fo the resource
     * @return
     * @throws IOException 
     */
    public Map<String,String> getMetadata( WebFileObject fo ) throws IOException;
    
}
