package org.das2.util.filesystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.vfs.FileSystemException;
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

    private static final Logger logger= org.das2.util.LoggerManager.getLogger("das2.filesystem.vfs");
    
    private org.apache.commons.vfs.FileObject vfsob;
    private VFSFileSystem vfsfs;
    private boolean local = false;
    private File localFile = null;
    private String localName= null;  // name within filesystem

    private static String relativeName( org.apache.commons.vfs.FileObject root, org.apache.commons.vfs.FileObject f ) throws FileSystemException {
        String roots= root.getName().toString();
        String fs= f.getName().toString();
        if ( fs.startsWith( roots ) ) {
            return fs.substring(roots.length());
        } else {
            throw new IllegalArgumentException("not a child:"+f);

        }
    }

    /**
     * Create a das2 FileObject from the given VFS FileObject
     * 
     * @param fs
     * @param f
     */
    protected VFSFileObject(VFSFileSystem fs, org.apache.commons.vfs.FileObject f) {
        vfsfs = fs;
        vfsob = f;

        // If the file system is local, the file is already available locally
        if (vfsfs.isLocal()) {
            local = true;
        }
        try {
            // localFile may not exist yet
            localName= relativeName(fs.getVFSFileObject(), f);
            localFile = new File( vfsfs.getLocalRoot(), localName );
        } catch (FileSystemException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    @Override
    public boolean canRead() {
        // TODO: Modify this to check local cache first when appropriate
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
        for (int i = 0; i < vfsKids.length; i++) {
            kids[i] = new VFSFileObject(vfsfs, vfsKids[i]);
        }
        return kids;
    }

    @Override
    public InputStream getInputStream(ProgressMonitor monitor) throws FileNotFoundException, IOException {
        InputStream r;
        //TODO: get the stream on the cached file, caching first if necessary
        try {
            r = vfsob.getContent().getInputStream();
        } catch (FileSystemException e) {
            // if possible, we should differentiate file not found error
            throw new IOException(e.getMessage()); //TODO Use Java 6 to wrap exception
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
        boolean download;
        if (localFile.exists()) {
            Date localFileLastModified = new Date(localFile.lastModified());
            Date remoteDate = new Date( vfsob.getContent().getLastModifiedTime() );
            if (remoteDate.after(localFileLastModified)) {
                FileSystem.logger.log(Level.INFO, "remote file is newer than local copy of {0}, download.", this.getNameExt());
                download = true;
            } else {
                download= false;
            }
        } else {
            download = true;
        }

        if (download) {
            // If it's a folder, ensure corresponding cache dir exists
            if (vfsob.getType() == org.apache.commons.vfs.FileType.FOLDER) {
                if (!localFile.exists()) {
                    FileSystemUtil.maybeMkdirs(localFile);
                }
            } else {
                // Download and cache remote file (including zip etc)
                if (!localFile.getParentFile().exists()) {
                    FileSystemUtil.maybeMkdirs( localFile.getParentFile() );
                    int i=10;
                    File p= localFile.getParentFile();
                    File stopAt= new File( new File( FileSystem.settings().getLocalCacheDir(), "vfsCache" ), "sftp" );
                    while ( i>0 && !p.equals(stopAt) ) {
                        p.setReadable(false,false);
                        p.setReadable(true,true);
                        p.setExecutable(false,false);
                        p.setExecutable(true,true);
                        p= p.getParentFile();
                        i=i-1;
                    }
                }
                File partfile = new File(localFile.toString() + ".part");
                vfsfs.downloadFile( localName, localFile, partfile, monitor);
            }
            local = true;
        }
        return localFile;
    }

    @Override
    public FileObject getParent() {
        org.apache.commons.vfs.FileObject vfsParent;
        try {
            vfsParent = vfsob.getParent();
        } catch (FileSystemException e) {
            throw new RuntimeException(e);
        }

        return new VFSFileObject(vfsfs, vfsParent);
    }

    @Override
    public long getSize() {
        // This das2 method returns -1 on error
        long size = -1;
        try {
            size = vfsob.getContent().getSize();
        } catch (FileSystemException e) {
            logger.log(Level.SEVERE,"unable to getSize",e);
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
        } catch (FileSystemException e) {
            throw new RuntimeException(e);
        }
        return r;
    }

    @Override
    public boolean isLocal() {
        // This will return true for local files OR if remote file is locally cached
        return local;
    }

    @Override
    public boolean exists() {
        boolean r;
        try {
            r = vfsob.exists();
        } catch (FileSystemException e) {
            throw new RuntimeException(e);
        }
        return r;
    }

    @Override
    public String getNameExt() {
        org.apache.commons.vfs.FileName fname = vfsob.getName();
        return fname.getPath();  //this includes the filename
    }

    @Override
    public Date lastModified() {
        // note that das2 says return new Date(0L) on error
        // TODO: Check both cache and remote file?
        long when = 0;
        try {
            when = vfsob.getContent().getLastModifiedTime();
        } catch (FileSystemException e) {
            logger.log(Level.SEVERE,"unable get last modified",e);
        } finally {
            return new Date(when);
        }
    }

    //------- EXPERIMENTAL support for writable file system

    public boolean canWrite() throws IOException {
        return vfsob.isWriteable();
    }

    /** Create a folder named by this VFSFileObject.  This method will also create
     * any necessary ancestor folders.  Does nothing if the folder already exists.
     *
     * @throws IOException If parent folder is read-only, or other error creating this
     * folder or ancestors, or if folder name exists as file.
     */
    public void createFolder() throws IOException {
        vfsob.createFolder();
    }

    /** Create the file named by this VFSFileObject. Also creates any necessary
     * ancestor folders.  Does nothing if file already exists and is a file (not a folder).
     *
     * @throws IOException If parent folder is read-only, or error creating ancestor
     * folders, or if file exists and is a folder.
     */
    public void createFile() throws IOException {
        vfsob.createFile();
    }

    /** Deletes the file.  Does nothing if the file doesn't exist or is a non-empty folder.
     *
     * @throws IOException If the file is a non-empty folder, or is read-only,
     * or on other error during deletion.
     */
    public void delete() throws IOException {
        vfsob.delete();
    }

    public OutputStream getOutputStream(boolean append) throws IOException {
        return vfsob.getContent().getOutputStream(append);
    }

    public void close() throws IOException {
        vfsob.close();
    }
}
