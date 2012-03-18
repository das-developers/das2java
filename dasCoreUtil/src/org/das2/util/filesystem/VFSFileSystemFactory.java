package org.das2.util.filesystem;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.UnknownHostException;
import org.das2.util.filesystem.FileSystem.FileSystemOfflineException;

/**
 *
 * @author ed
 */
public class VFSFileSystemFactory implements FileSystemFactory {

    public VFSFileSystemFactory() {
    }

    public FileSystem createFileSystem(URI root) throws FileSystemOfflineException, UnknownHostException {
        VFSFileSystem vfs = VFSFileSystem.createVFSFileSystem(root);
        return vfs;
    }
}
