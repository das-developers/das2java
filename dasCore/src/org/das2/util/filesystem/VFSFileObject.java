/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.util.filesystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Date;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;
import org.das2.util.monitor.ProgressMonitor;

/**
 * This class is part of a wrapper for the Apache Commons VFS.
 *
 * NOTE: At the moment, many situations where the Commons VFS throws a FileSystemException
 * will result in a RuntimeException from this code.  A better way of handling these
 * exceptions should probably be found.
 * @author ed
 */
public class VFSFileObject extends org.das2.util.filesystem.FileObject {

    private FileSystemManager mgr;
    private org.apache.commons.vfs.FileObject vfsob;

    /**
     * Create a das2 FileObject from the given VFS FileObject
     * @param f
     */
    protected VFSFileObject(org.apache.commons.vfs.FileObject f) {
        vfsob = f;
        try {
            mgr = VFS.getManager();
        } catch (FileSystemException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public boolean canRead() {
        boolean r;
        try {
            r = vfsob.isReadable();
        } catch (FileSystemException e) {
            throw new RuntimeException(e);
        }
        return r;
    }

    @Override
    public FileObject[] getChildren() throws IOException {
        org.apache.commons.vfs.FileObject vfsKids[];
        FileObject kids[];

        vfsKids = vfsob.getChildren();  //throws FileSystemException (extends IOException)

        kids = new FileObject[vfsKids.length];
        for (int i=0; i<vfsKids.length; i++) {
            kids[i] = new VFSFileObject(vfsKids[i]);
        }
        return kids;
    }

    @Override
    public InputStream getInputStream(ProgressMonitor monitor) throws FileNotFoundException, IOException {
        InputStream r;
        try {
            r = vfsob.getContent().getInputStream();
        } catch (FileSystemException e) {
            // if possible, we should differentiate file not found error
            throw new IOException(e);
        }
        return r;
    }

    @Override
    public ReadableByteChannel getChannel(ProgressMonitor monitor) throws FileNotFoundException, IOException {
        InputStream in = getInputStream(monitor);
        return Channels.newChannel(in);
    }

    @Override
    public File getFile(ProgressMonitor monitor) throws FileNotFoundException, IOException {
        // For local files, just return a java.io.File
        // For remote files, it will have to be downloaded and cached first
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FileObject getParent() {
        org.apache.commons.vfs.FileObject vfsParent;
        try {
            vfsParent = vfsob.getParent();
        } catch (FileSystemException e) {
            throw new RuntimeException(e);
        }

        return new VFSFileObject(vfsParent);
    }

    @Override
    public long getSize() {
        // This das2 method returns -1 on error
        long size = -1;
        try {
            size = vfsob.getContent().getSize();
        } catch(FileSystemException e) {
            e.printStackTrace();
        } finally {
            return size;
        }
    }

    @Override
    public boolean isData() {
        // really?
        return !isFolder();
    }

    @Override
    public boolean isFolder() {
        boolean r;
        try {
            r = (vfsob.getType() == org.apache.commons.vfs.FileType.FOLDER);
        } catch (FileSystemException e) {
            throw new RuntimeException(e);
        }
        return r;
    }

    @Override
    public boolean isReadOnly() {
        boolean r;
        try {
            r = !vfsob.isWriteable();
        } catch (FileSystemException e) {
            throw new RuntimeException(e);
        }
        return r;
    }

    @Override
    public boolean isRoot() {
        boolean r;
        try {
            r = (vfsob.getParent() == null);
        } catch(FileSystemException e) {
            throw new RuntimeException(e);
        }
        return r;
    }

    @Override
    public boolean isLocal() {
        // This will return true for local files OR if remote file is locally cached
        // I *think* this information can be obtained from FilesCache
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean exists() {
        boolean r;
        try {
            r = vfsob.exists();
        } catch(FileSystemException e) {
            throw new RuntimeException(e);
        }
        return r;
    }

    @Override
    public String getNameExt() {
        org.apache.commons.vfs.FileName fname = vfsob.getName();
        return fname.getPath() + fname.getBaseName();
    }

    @Override
    public Date lastModified() {
        // note that das2 says return new Date(0L) on error
        long when = 0;
        try {
            when = vfsob.getContent().getLastModifiedTime();
        } catch(FileSystemException e) {
            e.printStackTrace();
        } finally {
            return new Date(when);
        }
    }
}
