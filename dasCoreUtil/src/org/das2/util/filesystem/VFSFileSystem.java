package org.das2.util.filesystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.VFS;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.util.monitor.CancelledOperationException;

//these were supposed to make the URIs consistent with the others: ftp://user@host.gov/foo/ should be "foo" in the user's home directory.
//import org.apache.commons.vfs.FileSystemOptions;
//import org.apache.commons.vfs.provider.ftp.FtpFileSystemConfigBuilder;

/**
 * FileSystem provides additional abstraction to Apache VFS to implement das2 FileSystems, and provide
 * SFTP access.
 * @author Ed Jackson
 */
public class VFSFileSystem extends org.das2.util.filesystem.FileSystem {

    private final FileSystemManager mgr;
    private org.apache.commons.vfs.FileSystem vfsSystem;
    private org.apache.commons.vfs.FileObject fsRoot;
    private final File cacheRoot;
    private final URI fsuri;

    private VFSFileSystem(URI root, boolean createFolder) throws IOException {
        super(root);
        mgr = VFS.getManager();
        downloads= new HashMap();
        String userInfo= root.getUserInfo();
        if ( userInfo!=null && userInfo.contains(":") ) {
            int i= userInfo.indexOf(":");
            userInfo= userInfo.substring(0,i) + "@";
        } else if ( userInfo!=null ) {
            userInfo= userInfo + "@";
        } else {
            if ( root.toASCIIString().contains("sftp:///") ) {
                throw new IllegalArgumentException("sftp must not be followed by ///, should be //.");
            } else {
                throw new IllegalArgumentException("root must contain user name.");
            }
        }
        String subFolderName = "vfsCache/" + root.getScheme() + "/" + userInfo + root.getHost() + root.getPath();
        cacheRoot = new File(settings().getLocalCacheDir(), subFolderName);
        
        fsRoot = mgr.resolveFile(root.toString());


        if (!fsRoot.exists() && createFolder) {
            //Also creates any necessary ancestor folders
            fsRoot.createFolder();
        }

        if (!fsRoot.exists()) {
            throw new FileSystemOfflineException("Specified filesystem root does not exist: "+ KeyChain.getDefault().hideUserInfo(root));
        }
        
        vfsSystem = fsRoot.getFileSystem();

        String roots= root.toString();
        if ( !roots.endsWith("/") ) roots= roots+"/";

        if (fsRoot.getType() == org.apache.commons.vfs.FileType.FOLDER) {
            fsuri = URI.create(roots);
        } else {
            fsuri = URI.create(roots.substring(0, root.toString().lastIndexOf('/')+1 )); // huh--when is this branch taken?
        }
    }

    // SFTP has to be closed to be sure all threads finish, otherwise the VM may hang on app exit
    // This should probably be refactored to the super-class with a default empty implementation.
    public void close() {
        // VfsComponent is the interface that specifies close(), so that's the cast we use:
        ((org.apache.commons.vfs.provider.VfsComponent) vfsSystem).close();
    }

    public static VFSFileSystem createVFSFileSystem(URI root) throws FileSystemOfflineException, UnknownHostException {
        // To preserve legacy behavior, the default is to treat this as a read-only operation
        return createVFSFileSystem(root, false);
    }

    public static VFSFileSystem createVFSFileSystem(URI root, boolean createFolder) throws FileSystemOfflineException, UnknownHostException {
        //TODO: Handle at least some exceptions; offline detection?
        // yes, this is ugly

        if ( root.getScheme().equals("ftp") ) {
            while ( true ) {
            // this branch allows for passwords.  We don't support passwords 
            // over sftp, because of security concerns.
                URI authUri;
                try {
                    authUri = KeyChain.getDefault().resolveUserInfo(root);
                } catch (CancelledOperationException ex) {
                    throw new FileSystemOfflineException("access cancelled");
                }
                try {
                    VFSFileSystem result= new VFSFileSystem(authUri, createFolder);
                    return result;
                } catch (IOException e) {
                    if ( e instanceof org.apache.commons.vfs.FileSystemException ) {
                        org.apache.commons.vfs.FileSystemException vfse=
                            (org.apache.commons.vfs.FileSystemException)e;
                        if ( vfse.getCode().contains("login.error") ) {
                            KeyChain.getDefault().clearUserPassword(authUri);
                            if ( authUri.getUserInfo()==null ) {
                                throw new FileSystemOfflineException(e);
                            }
                        } else if ( vfse.getCode().contains("connect.error") ) {
                            if ( authUri.getUserInfo()==null ) {
                                throw new FileSystemOfflineException(e);
                            }
                            KeyChain.getDefault().clearUserPassword(authUri);
                        } else {
                            throw new FileSystemOfflineException(e);
                        }
                    } else {
                        throw new FileSystemOfflineException(e);
                    }
                }
            }
        }

        try {
            return new VFSFileSystem(root, createFolder);
        } catch (IOException e) {
            if ( e instanceof org.apache.commons.vfs.FileSystemException ) {
                org.apache.commons.vfs.FileSystemException vfse=
                        (org.apache.commons.vfs.FileSystemException)e;
                if ( vfse.getCode().contains("login.error") ) {
                    throw new FileSystemOfflineException(e);
                } else if ( vfse.getCode().equals( "vfs.provider.sftp/connect.error" ) ) {
                    throw new FileSystemOfflineException(e);
                } else if ( vfse.getCode().contains("connect.error") ) {
                    throw new FileSystemOfflineException(e);
                } else if ( e.getMessage().startsWith("Could not connect to ") ) {
                    throw  new UnknownHostException(root.getHost());
                } else if ( vfse.getCode().contains("invalid-absolute-uri") ) {
                    throw new UnknownHostException(vfse.getMessage());
                } else {
                    throw new FileSystemOfflineException(e);
                }
            }
            if ( e.getMessage().startsWith("Could not connect to ") ) {
                throw new UnknownHostException(root.getHost());
            } else {
                throw new FileSystemOfflineException(e);  //slightly less ugly
            }
        }
    }

    protected org.apache.commons.vfs.FileObject getVFSFileObject() throws FileSystemException {
        return fsRoot;
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
        org.apache.commons.vfs.FileObject vfsob = mgr.resolveFile( fsRoot, filename ); //TODO: verify filename can contain slashes.
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
        if ( directory.startsWith("/") ) directory= directory.substring(1);

        directory = fsuri.toString() + directory; // suspect https://sourceforge.net/tracker/?func=detail&aid=3055130&group_id=199733&atid=970682
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

        Pattern pattern = Pattern.compile(regex);
        ArrayList result = new ArrayList();
        for (int i = 0; i < listing.length; i++) {
            String s= listing[i];
            if ( s.charAt(s.length()-1)=='/' ) s= s.substring(0,s.length()-1);
            if (pattern.matcher(s).matches()) {
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
    private final Map downloads;

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
     * @param monitor a monitor for the download.  If a MutatorLock is returned, then
     *    the monitor is not touched, but other threads may use it to keep track
     *    of the download progress.
     * @throws FileNotFoundException if the file wasn't found after another thread loaded the file.
     * @return MutatorLock.  The client should call mutatorLock.unlock() when the download is complete
     */
    protected Lock getDownloadLock(final String filename, File f, ProgressMonitor monitor) throws IOException {
        logger.log(Level.FINER, "{0} wants download lock for {1} wfs impl {2}", new Object[]{Thread.currentThread().getName(), filename, this.hashCode()});
        synchronized (downloads) {
            ProgressMonitor mon = (ProgressMonitor) downloads.get(filename);
            if (mon != null) { // the webfilesystem is already loading this file, so wait.
                logger.log(Level.FINE, "another thread is downloading {0}, waiting...", filename);
                waitForDownload(monitor, filename);
                if (f.exists()) {
                    return null;
                } else {
                    throw new FileNotFoundException("expected to find " + f);
                }
            } else {
                logger.log(Level.FINE, "this thread will download {0}.", filename);
                downloads.put(filename, monitor);
                monitor.started();  // this is necessary for the other monitors
                return new LocalReentrantLock(filename);
            }
        }
    }

    private class LocalReentrantLock extends ReentrantLock {
        String filename;
        private LocalReentrantLock( String filename ) {
            this.filename= filename;
        }
        @Override
        public void lock() {
        }

        @Override
        public void unlock() {
            synchronized (downloads) {
                downloads.remove(filename);
                downloads.notifyAll();
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
     * @param is the input stream source of data
     * @param out the output stream to where the data is copied.
     * @param monitor monitor for the transfer, where only setTaskProgress and isCancelled are called.
     * @throws java.io.IOException if the transfer is interrupted.
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

    /**
     * Transfers the file from the remote store to a local copy f.  This should only be
     * used within the class and subclasses, clients should use getFileObject( String ).getFile().
     *
     * @param filename the name of the file, relative to the filesystem.
     * @param f the file to where the file is downloaded.
     * @param partfile the temporary file during download.
     * @param monitor progress monitor
     */
    protected void downloadFile(String filename, File f, File partfile, ProgressMonitor monitor) throws IOException {
        // This shouldn't be called for local files, but just in case...
        if (isLocal()) {
            return;
        }

        Lock lock = getDownloadLock(filename, f, monitor);

        if (lock == null) {
            return;  //Another thread downloaded the file
        }

        try {
            if ( filename.startsWith(fsuri.getPath()) ) {
                logger.log( Level.INFO, "something is funny, we have the path twice:{0} {1}", new Object[]{filename, fsuri});
            }
            filename = fsuri.getPath() + filename;
            org.apache.commons.vfs.FileObject vfsob = vfsSystem.resolveFile(filename);

            if(!vfsob.exists()) {
                throw new FileNotFoundException("attempt to download non-existent file: "+vfsob);
            }

            long size = vfsob.getContent().getSize();
            monitor.setTaskSize(size);

            // If necessary, create destination folder
            if (!f.getParentFile().exists()) {
                logger.log(Level.FINE, "Creating destination directory {0}", f.getParentFile());
                FileSystemUtil.maybeMkdirs( f.getParentFile() );
            }

            if (partfile.exists()) {
                logger.fine("Deleting existing partfile.");
                if ( ! partfile.delete() ) {
                    throw new IllegalArgumentException("unable to delete "+partfile );
                }
            
            }

            // create partfile
            if (partfile.createNewFile()) {
                InputStream is = vfsob.getContent().getInputStream();
                FileOutputStream os = new FileOutputStream(partfile);

                monitor.setLabel("Downloading file...");
                monitor.started();
                try {
                    copyStream(is, os, monitor);
                    is.close();
                    os.close();
                    partfile.setReadable(false,false);
                    partfile.setReadable(true,true);
                    if ( ! partfile.renameTo(f) ) {
                        throw new IllegalArgumentException("unable to rename file "+partfile + " to "+f );
                    }
                } catch (IOException e) {
                    // clean up and pass the exception on
                    is.close();
                    os.close();
                    if ( partfile.exists() && ! partfile.delete() ) {
                        throw new IOException("unable to delete file "+partfile );
                    }
                    throw (e);
                }
            } else {
                // failed to create partfile
                throw new IOException("Error creating local file " + f);
            }
        } finally {
            // Ensure that the download lock is released no matter what
            lock.unlock();
            monitor.finished();
        }
    }

    @Override
    protected void finalize() throws Throwable {
       // ensure that any open VFS filesystem gets closed so threads terminate
       try {
           close();
       } finally {
           super.finalize();
       }
    }

}
