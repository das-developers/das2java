/*
 * HttpFileSystemFactory.java
 *
 * Created on November 15, 2007, 9:28 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.uiowa.physics.pw.das.util.fileSystem;

import edu.uiowa.physics.pw.das.util.fileSystem.FileSystem.FileSystemOfflineException;
import java.net.URL;

/**
 *
 * @author jbf
 */
public class HttpFileSystemFactory implements FileSystemFactory {
    
    /** Creates a new instance of HttpFileSystemFactory */
    public HttpFileSystemFactory() {
    }

    public FileSystem createFileSystem(URL root) throws FileSystemOfflineException {
        HttpFileSystem hfs= HttpFileSystem.createHttpFileSystem( root );        
        return hfs;
    }
    
}
