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
    
    public InputStream getInputStream( WebFileObject fo, org.das2.util.monitor.ProgressMonitor mon ) throws IOException;
    public Map<String,String> getMetadata( WebFileObject fo ) throws IOException;
    
}
