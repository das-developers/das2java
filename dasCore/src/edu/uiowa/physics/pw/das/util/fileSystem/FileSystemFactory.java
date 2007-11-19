/*
 * FileSystemFactory.java
 *
 * Created on November 15, 2007, 9:26 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.uiowa.physics.pw.das.util.fileSystem;

import edu.uiowa.physics.pw.das.util.fileSystem.FileSystem.FileSystemOfflineException;
import java.net.URL;

/**
 * creates a new instance of a type of filesystem
 * @author jbf
 */
public interface FileSystemFactory {
    FileSystem createFileSystem( URL root ) throws FileSystemOfflineException;
}
