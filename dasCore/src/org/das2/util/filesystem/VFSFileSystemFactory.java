package org.das2.util.filesystem;

import java.net.MalformedURLException;
import java.net.URI;
import org.das2.util.filesystem.FileSystem.FileSystemOfflineException;

/**
 *
 * @author ed
 */
public class VFSFileSystemFactory implements FileSystemFactory {

    public VFSFileSystemFactory() {
    }

    public FileSystem createFileSystem(URI root) throws FileSystemOfflineException, MalformedURLException {
        VFSFileSystem vfs = VFSFileSystem.createVFSFileSystem(root);
        return vfs;
    }
}
