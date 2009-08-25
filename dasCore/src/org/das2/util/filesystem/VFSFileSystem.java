/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util.filesystem;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.VFS;

/**
 *
 * @author ed
 */
public class VFSFileSystem extends org.das2.util.filesystem.FileSystem {
    private FileSystemManager mgr;
    private org.apache.commons.vfs.FileSystem vfsSystem;
    // We need to be more general than URL here, but superclass needs it.
    // Not sure how we'll handle this yet
    private VFSFileSystem(URL root) throws IOException {
        super(root);
        mgr = VFS.getManager();
    }

    @Override
    public FileObject getFileObject(String filename) {
        org.apache.commons.vfs.FileObject vfsob;
        try {
            vfsob = mgr.resolveFile(filename);
        } catch (FileSystemException e) {
            throw new RuntimeException(e);
        }
        return new VFSFileObject(vfsob);
    }

    @Override
    public boolean isDirectory(String filename) throws IOException {
        org.apache.commons.vfs.FileObject vfsob = mgr.resolveFile(filename);
        return (vfsob.getType() == FileType.FOLDER);
    }

    @Override
    public String[] listDirectory(String directory) throws IOException {
        // We'll let the VFS throw any necessary exceptions
        org.apache.commons.vfs.FileObject vfsob = mgr.resolveFile(directory);
        org.apache.commons.vfs.FileObject children[] = vfsob.getChildren();

        String r[] = new String[children.length];
        for (int i=0; i < children.length; i++) {
            r[i] = children[i].getName().getBaseName();
        }

        return r;
    }

    @Override
    public String[] listDirectory(String directory, String regex) throws IOException {
        String[] listing = listDirectory(directory);

        Pattern pattern = Pattern.compile(regex + "/?");
        ArrayList result = new ArrayList();
        for (int i = 0; i < listing.length; i++) {
            if (pattern.matcher(listing[i]).matches()) {
                result.add(listing[i]);
            }
        }
        return (String[]) result.toArray(new String[result.size()]);

    }

    @Override
    public File getLocalRoot() {
        try {
            org.apache.commons.vfs.FileObject vfsob = vfsSystem.getRoot();
        } catch(FileSystemException e) {
            throw new RuntimeException(e);
        }
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
