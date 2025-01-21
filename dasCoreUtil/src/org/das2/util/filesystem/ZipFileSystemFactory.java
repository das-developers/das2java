package org.das2.util.filesystem;

import java.io.IOException;
import java.net.URI;

/**
 * creates a ZipFileSystem
 * @author Ed Jackson
 */
public class ZipFileSystemFactory implements FileSystemFactory {

    public ZipFileSystemFactory() {
    }
 
    @Override
    public FileSystem createFileSystem(URI root) throws FileSystem.FileSystemOfflineException {
        FileSystem zfs = null;
        try {
            zfs = new ZipFileSystem(root);
        } catch (IOException ex) {
            throw new FileSystem.FileSystemOfflineException(ex);
        }
        return zfs;
    }

}
