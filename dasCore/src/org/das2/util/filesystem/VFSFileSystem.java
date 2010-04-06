package org.das2.util.filesystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.VFS;
import org.das2.system.MutatorLock;
import org.das2.util.monitor.ProgressMonitor;

/**
 *
 * @author ed
 */
public class VFSFileSystem extends org.das2.util.filesystem.FileSystem {

    private final FileSystemManager mgr;
    private org.apache.commons.vfs.FileSystem vfsSystem;
    private org.apache.commons.vfs.FileObject fsRoot;
    private final File cacheRoot;
    private final URI fsuri;

    private VFSFileSystem(URI root) throws IOException {
        super(root);
        mgr = VFS.getManager();

        String subFolderName = "vfsCache/" + root.getScheme() + "/" + root.getHost() + root.getPath();
        cacheRoot = new File(settings().getLocalCacheDir(), subFolderName);
        
        fsRoot = mgr.resolveFile(root.toString());
        vfsSystem = fsRoot.getFileSystem();

        if (fsRoot.getType() == org.apache.commons.vfs.FileType.FOLDER) {
            fsuri = URI.create(root.toString());
        } else {
            fsuri = URI.create(root.toString().substring(0, root.toString().lastIndexOf('/')+1 ));
        }
    }

    public static synchronized VFSFileSystem createVFSFileSystem(URI root) throws FileSystemOfflineException {
        //TODO: Handle at least some exceptions; offline detection?
        // yes, this is ugly
        try {
            return new VFSFileSystem(root);
        } catch (IOException e) {
            throw new FileSystemOfflineException(e);  //slightly less ugly
        }
    }

    @Override
    public FileObject getFileObject(String filename) {
        org.apache.commons.vfs.FileObject vfsob;
        try {
            // Have to peel leading slash from absolute path so VFS doesn't resolve to file:///filename
            if(filename.startsWith("/"))
                vfsob = mgr.resolveFile(fsRoot, filename.substring(1));
            else
                vfsob = mgr.resolveFile(fsRoot, filename);
        } catch (FileSystemException e) {
            throw new RuntimeException(e);
        }
        return new VFSFileObject(this, vfsob);
    }

    @Override
    public boolean isDirectory(String filename) throws IOException {
        org.apache.commons.vfs.FileObject vfsob = mgr.resolveFile(filename);
        return (vfsob.getType() == FileType.FOLDER);
    }

    /**
     * return a list of files and folders in the directory.
     * Conventionally, folders are identified with a trailing slash.
     * @param directory
     * @return
     * @throws IOException
     */
    @Override
    public String[] listDirectory(String directory) throws IOException {
        // We'll let the VFS throw any necessary exceptions
        directory = fsuri.toString() + directory;
        org.apache.commons.vfs.FileObject vfsob = mgr.resolveFile(directory);
        org.apache.commons.vfs.FileObject children[] = vfsob.getChildren();

        String r[] = new String[children.length];
        for (int i = 0; i < children.length; i++) {
            if ( children[i].getType()==FileType.FOLDER ) {
                r[i]= children[i].getName().getBaseName() + "/";
            } else {
                r[i] = children[i].getName().getBaseName();
            }
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
        // file system root for local; otherwise cache root folder
        org.apache.commons.vfs.FileObject vfsob;

        if (isLocal()) {
            // For local files, just return the local filesystem root
            try {
                vfsob = vfsSystem.getRoot();
            } catch (FileSystemException e) {
                throw new RuntimeException(e);
            }
            // Replace getPath with getPathDecoded to replace % escapes with literals
            return new File(vfsob.getName().getPath());
        } else {
            // For non-local files (this includes .zip .tgz etc) we use cache dir
            return cacheRoot;
        }
    }

    public boolean isLocal() {
        // note that this will return false for zip, tgz, etc even if the archive is local
        if (vfsSystem instanceof org.apache.commons.vfs.provider.local.LocalFileSystem) {
            return true;
        } else {
            return false;
        }
    }
    /**
     * Keep track of active downloads.  This handles, for example, the case
     * where the same file is requested several times by different threads.
     */
    private final Map downloads = new HashMap();

    /**
     * Request lock to download file.  If this thread gets the lock, then it
     * should download the file and call  mutatorLock.unlock() when the
     * download is complete.   If another thread is downloading the file, this
     * will block until the download is complete, and null will be returned to
     * indicate that the file has already been downloaded.  This must start the
     * monitor when it gets the lock.
     *
     * @param filename the filename with in the filesystem.
     * @param f the File which will be the local copy.
     * @param mon a monitor for the download.  If a MutatorLock is returned, then
     *    the monitor is not touched, but other threads may use it to keep track
     *    of the download progress.
     * @throws FileNotFoundException if the file wasn't found after another thread loaded the file.
     * @return MutatorLock.  The client should call mutatorLock.unlock() when the download is complete
     */
    protected MutatorLock getDownloadLock(final String filename, File f, ProgressMonitor monitor) throws IOException {
        logger.finer("" + Thread.currentThread().getName() + " wants download lock for " + filename + " wfs impl " + this.hashCode());
        synchronized (downloads) {
            ProgressMonitor mon = (ProgressMonitor) downloads.get(filename);
            if (mon != null) { // the webfilesystem is already loading this file, so wait.
                logger.fine("another thread is downloading " + filename + ", waiting...");
                waitForDownload(monitor, filename);
                if (f.exists()) {
                    return null;
                } else {
                    throw new FileNotFoundException("expected to find " + f);
                }
            } else {
                logger.fine("this thread will download " + filename + ".");
                downloads.put(filename, monitor);
                monitor.started();  // this is necessary for the other monitors

                return new MutatorLock() {

                    public void lock() {
                    }

                    public void unlock() {
                        synchronized (downloads) {
                            downloads.remove(filename);
                            downloads.notifyAll();
                        }
                    }
                };

            }
        }
    }

    /**
     * Wait while another thread is downloading the file.
     * @param monitor this thread's monitor.
     * @param filename
     */
    private void waitForDownload(ProgressMonitor monitor, final String filename) {

        monitor.setProgressMessage("waiting for file to download");

        ProgressMonitor downloadMonitor = (ProgressMonitor) downloads.get(filename);

        monitor.started();

        while (downloadMonitor != null) {

            // in case downloadMonitor switched from indeterminate to determinate
            monitor.setTaskSize(downloadMonitor.getTaskSize());

            // this monitor can tell the downloading monitor to cancel.
            if (monitor.isCancelled()) {
                downloadMonitor.cancel();
            }

            // echo what the download monitor is reporting.
            monitor.setTaskProgress(downloadMonitor.getTaskProgress());

            try {
                downloads.wait(100); // wait 100ms, then proceed to support progress information
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            downloadMonitor = (ProgressMonitor) downloads.get(filename);

        }

        monitor.finished();
    }

    /**
     * copies data from in to out, sending the number of bytesTransferred to the monitor.
     */
    protected void copyStream(InputStream is, OutputStream out, ProgressMonitor monitor) throws IOException {
        byte[] buffer = new byte[2048];
        int bytesRead = is.read(buffer, 0, 2048);
        long totalBytesRead = bytesRead;
        while (bytesRead > -1) {
            if (monitor.isCancelled()) {
                throw new InterruptedIOException();
            }
            monitor.setTaskProgress(totalBytesRead);
            out.write(buffer, 0, bytesRead);
            bytesRead = is.read(buffer, 0, 2048);
            totalBytesRead += bytesRead;
            logger.finest("transferring data");
        }
    }

    protected void downloadFile(String filename, File f, File partfile, ProgressMonitor monitor) throws IOException {
        // This shouldn't be called for local files, but just in case...
        if (isLocal()) {
            return;
        }

        MutatorLock lock = getDownloadLock(filename, f, monitor);

        if (lock == null) {
            return;  //Another thread downloaded the file
        }

        try {
            filename = fsuri.getPath() + filename;
            org.apache.commons.vfs.FileObject vfsob = vfsSystem.resolveFile(filename);

            if(!vfsob.exists()) {
                //System.err.println("Uh oh! Attempt to download non-existent file via VFS.");
                throw new FileNotFoundException("attempt to download non-existent file");
            }

            long size = vfsob.getContent().getSize();
            monitor.setTaskSize(size);

            // If necessary, create destination folder
            if (!f.getParentFile().exists()) {
                logger.fine("Creating destination directory " + f.getParentFile());
                f.getParentFile().mkdirs();
            }

            if (partfile.exists()) {
                logger.fine("Deleting existing partfile.");
                partfile.delete();
            //TODO: check for failed deletion
            }

            // create partfile
            if (partfile.createNewFile()) {
                InputStream is = vfsob.getContent().getInputStream();
                FileOutputStream os = new FileOutputStream(partfile);

                monitor.setLabel("Downloading file...");
                monitor.started();
                try {
                    copyStream(is, os, monitor);
                    monitor.finished();
                    is.close();
                    os.close();
                    partfile.renameTo(f);
                } catch (IOException e) {
                    // clean up and pass the exception on
                    is.close();
                    os.close();
                    partfile.delete();
                    throw (e);
                }
            } else {
                // failed to create partfile
                throw new IOException("Error creating local file " + f);
            }
        } finally {
            // Ensure that the download lock is released no matter what
            lock.unlock();
        }
    }
}
