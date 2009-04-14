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
    public static final String META_LAST_MODIFIED="Last-Modified";
    
    
    public InputStream getInputStream( WebFileObject fo, org.das2.util.monitor.ProgressMonitor mon ) throws IOException;
    
    /**
     * returns metadata for the object.  For property names, see META_LAST_MODIFIED, META_EXIST, etc.
     * @param fo
     * @return
     * @throws java.io.IOException
     */
    public Map<String,String> getMetadata( WebFileObject fo ) throws IOException;
    
}
