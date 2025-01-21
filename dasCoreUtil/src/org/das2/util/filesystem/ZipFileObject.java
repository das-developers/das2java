
package org.das2.util.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import org.das2.util.monitor.ProgressMonitor;

/**
 * A file within a Zip file
 * @author Ed Jackson
 */
public class ZipFileObject extends FileObject {
    private ZipFileSystem zfs;      //The parent zip file system
    private ZipEntry zipEntry;      //The corresponding entry in the zip file
    private ZipFileObject parent;
    private String name;            // Full name of file; only used when zipEntry is null
    private ArrayList<ZipFileObject> children;
    private boolean unzip;          // the file must also be unzipped.

    /** Create a new <code>ZipFileObject</code>.
     * 
     * @param zfs the containing <code>ZipFileSystem</code>
     * @param zipEntry the {@link ZipEntry} associated with this object
     * @param par the parent <code>ZipFileObject</code>.  Set to null if this is the root object.
     */
    protected ZipFileObject(ZipFileSystem zfs, ZipEntry zipEntry, ZipFileObject par) {
        this(zfs, zipEntry, par, null);
    }

    /** Create a new <code>ZipFileObject</code> with the specified name.  The name
     * should normally not be specified.  However, a folder which contains only a single
     * folder will not have a corresponding {@link ZipEntry}.  In that case, <code>zipEntry</code>
     * should be set to <code>null</code> and the <code>name</code> set to the
     * name of this folder, with no path information slashes.
     *
     * @param zfs the containing ZipFileSystem
     * @param zipEntry the <code>ZipEntry</code> associated with this object
     * @param par the parent ZipFileObject. Set to null if this is the root object.
     * @param name the file name.  If <code>zipEntry</code> is not
     *      <code>null</code>, this is ignored.
     */
    protected ZipFileObject(ZipFileSystem zfs, ZipEntry zipEntry, ZipFileObject par, String name) {
        this.zfs = zfs;
        this.zipEntry = zipEntry;
        this.parent = par;
        this.name = name;       // used by getNameExt only when zipEntry is null
        this.unzip= zipEntry!=null && name!=null && zipEntry.getName().endsWith(".gz") && !name.endsWith(".gz") ;
        children = new ArrayList<>();
    }

    protected void addChildObject(ZipFileObject child) {
        children.add(child);
    }

    @Override
    public boolean canRead() {
        // At this point we can read the zip file, so we know we can read its contents
        return exists();
    }

    @Override
    public FileObject[] getChildren() throws IOException {
        return children.toArray(new ZipFileObject[children.size()]);
    }

    @Override
    public InputStream getInputStream(ProgressMonitor monitor) throws IOException {
        if ( !exists() ) {
            throw new FileNotFoundException("file not found in zip: "+name );
        } else {
            if ( unzip ) {
                return new GZIPInputStream( zfs.getZipFile().getInputStream(zipEntry) );
            } else {
                return zfs.getZipFile().getInputStream(zipEntry);
            }
        }
    }

    @Override
    public ReadableByteChannel getChannel(ProgressMonitor monitor) throws FileNotFoundException, IOException {
        return ((FileInputStream)getInputStream( monitor )).getChannel();
    }

    /* For the ZipFileSystem, getFile unpacks the requested file to a temporary
     * location and returns that file. */
    @Override
    public synchronized File getFile(ProgressMonitor monitor) throws FileNotFoundException, IOException {
        // ignoring the monitor for now; possibly we'll need to use it this proves slow
        if ( !exists() ) throw new FileNotFoundException(
                String.format( "file %s does not exist in %s", this.name, this.zfs.toString() ) );
        String tmpFileName;
        if ( unzip ) {
            tmpFileName= zfs.getLocalRoot().getAbsoluteFile() + "/" + name;
        } else {
            tmpFileName= zfs.getLocalRoot().getAbsoluteFile() + "/" + zipEntry.getName();
        }
        File tmpFile = new File(tmpFileName);
        File tmpDir = tmpFile.getParentFile();

        // We're blindly unpacking and not checking age of possibly existing cache file
        FileSystemUtil.maybeMkdirs(tmpDir);
        if ( tmpFile.exists() ) {
            if ( tmpFile.lastModified()< new File(zfs.getZipFile().getName()).lastModified() ) {
                if ( !tmpFile.delete() ) {
                    throw new IOException("unable to delete old unzipped file: "+tmpFile );
                }
            } else {
                return tmpFile; //TODO: we need to more thoroughly check timestamps dates, etc.
            }
        }
        if ( ! tmpFile.createNewFile() ) {
            throw new IllegalArgumentException("unable to create file "+tmpFile );
        }

        try (InputStream zStream = getInputStream(monitor) ) {
            FileSystemUtil.dumpToFile(zStream, tmpFile);
        }

        return tmpFile;
    }

    @Override
    public FileObject getParent() {
        return parent;  //will be null if this is root
    }

    @Override
    public long getSize() {
        if (zipEntry==null) return 0;
        return zipEntry.getSize();
    }

    @Override
    public boolean isData() {
        if (zipEntry==null) return false;
        return !(zipEntry.isDirectory());
    }

    @Override
    public boolean isFolder() {
        if (zipEntry==null) return true;  // Root and some directory nodes have no ZipEntry
        return zipEntry.isDirectory();
    }

    @Override
    public boolean isReadOnly() {
        return true;  // we don't support writing into zip files
    }

    @Override
    public boolean isRoot() {
        return parent==null;
    }

    @Override
    public boolean isLocal() {
        /* The zip has been cached locally, so we return true and extract the file
         * when it's needed.  If extraction turns out to be too slow, we might need
         * to modify this behavior. */
        return true;
    }

    @Override
    public boolean exists() {
        return this.zipEntry!=null && this.parent!=null;
    }

    @Override
    public String getNameExt() {
        if(isRoot()) return "/";
        if (zipEntry != null) {
            return "/" + zipEntry.getName();
        } else {
            return parent.getNameExt() + name + "/";
        }
    }

    @Override
    public Date lastModified() {
        // for root we'll return the mod time of the zip file
        if(isRoot()) {
            File f = new File(zfs.getZipFile().getName());
            return new Date(f.lastModified());
        }
        // if getTime() returns -1 (unspecified) default to 1/1/1970 00:00GMT
        long when = zipEntry.getTime();
        return (when>0 ? new Date(when) : new Date(0));
    }

}
