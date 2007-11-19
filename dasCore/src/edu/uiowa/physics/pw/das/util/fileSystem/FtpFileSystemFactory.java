/*
 * FtpFileSystemFactory.java
 *
 * Created on November 15, 2007, 9:31 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.uiowa.physics.pw.das.util.fileSystem;

import java.net.URL;

/**
 *
 * @author jbf
 */
public class FtpFileSystemFactory implements FileSystemFactory {
    
    /** Creates a new instance of FtpFileSystemFactory */
    public FtpFileSystemFactory() {
    }

    public FileSystem createFileSystem(URL root) throws FileSystem.FileSystemOfflineException {
        return FTPFileSystem.create( root );
    }
    
}
