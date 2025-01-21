package org.das2.util.filesystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * A filesystem to read data from zip files.
 * @author Ed Jackson
 */
public class ZipFileSystem extends FileSystem {

    private ZipFile zipFile;
    private TreeMap<String, ZipFileObject> filemap = new TreeMap<>();

    protected ZipFileSystem(URI root) throws IOException {
        super(root);
        if ( !("file".equals(root.getScheme()) ) ) {
            throw new IllegalArgumentException("Cannot access non-local zip file: "+root);
        }

        File f;
        f = new File(root);

        // This may throw ZipException, IOException, or SecurityException
        try {
            zipFile = new ZipFile( f );
        } catch ( ZipException ex ) {
            throw new IllegalArgumentException("File is not a properly formatted zip file: "+f,ex);
        }

        // First create the root FileObject, which has no corresponding ZipEntry
        filemap.put("/", new ZipFileObject(this, null, null));

        Enumeration<? extends ZipEntry> contents = zipFile.entries();
        while(contents.hasMoreElements()) {
            ZipEntry entry = contents.nextElement();
            String entryName = "/" + entry.getName();
            addZipEntry(entryName, entry); //TODO: do the other FS implementations start entries with /?
        }
    }

    private void addZipEntry(String name, ZipEntry entry) {
        logger.log(Level.FINE, "addZipEntry: {0}", name);
        String parentName = name.substring(0, name.lastIndexOf('/', name.length()-2)+1);
        // recursively back up until we find a path we've already added.
        if (!filemap.containsKey(parentName)) addZipEntry(parentName, null);

        String n = null;
        if (entry == null) {
            n = name;
            if (n.endsWith("/")) n = n.substring(0, n.length()-1);
            n = n.substring(n.lastIndexOf('/'));
        }
        ZipFileObject zfo = new ZipFileObject(this, entry, filemap.get(parentName),n);
        filemap.put(name, zfo);
        filemap.get(parentName).addChildObject(zfo);
        if ( name.endsWith(".gz") ) {
            n= name.substring(0,name.length()-3);
            zfo = new ZipFileObject(this, entry, filemap.get(parentName), n);
            filemap.put(n, zfo);
            filemap.get(parentName).addChildObject(zfo);
        }
    }

    // ZipFileObject will need this for opening streams
    protected ZipFile getZipFile() {
        return zipFile;
    }

    @Override
    public FileObject getFileObject(String filename) {
        String f = FileSystem.toCanonicalFilename(filename);
        if ( !f.startsWith("/") ) f= "/"+f;
        if (filemap.containsKey(f)) {
            return filemap.get(f);
        } else if (filemap.containsKey(f+"/")) {  //maybe it's a folder with out trailing /
            return filemap.get(f+"/");
        } else if ( filemap.containsKey(f+".gz") ) {
            return filemap.get(f+".gz");
        } else {
            return new ZipFileObject( this, null, null, filename );
        }
    }

    @Override
    public boolean isDirectory(String filename) throws IOException {
        // First try canonical version of given filename, then try as folder name
        String f = FileSystem.toCanonicalFilename(filename);
        if (filemap.containsKey(f))
            return filemap.get(f).isFolder();
        f = FileSystem.toCanonicalFolderName(filename);
        if (filemap.containsKey(f)) return filemap.get(f).isFolder();

        // if we make it this far, the given filename doesn't exist
        throw new FileNotFoundException("No such file in zip: " + filename);
    }

    @Override
    public String[] listDirectory(String directory) throws IOException {
        String dname = FileSystem.toCanonicalFolderName(directory);
        if (!isDirectory(dname)) {
            throw new IllegalArgumentException("Not a folder in zip file: " + dname);
        }
        FileObject[] contents = filemap.get(dname).getChildren();
        String[] results = new String[contents.length];
        
        for(int i=0; i<contents.length; ++i) {
            String s = contents[i].getNameExt();
            results[i] = s.substring(s.lastIndexOf('/',s.length()-2)+1);
        }
        return results;
    }

    @Override
    public String[] listDirectory(String directory, String regex) throws IOException {
        directory = toCanonicalFilename(directory);
        String[] listing = listDirectory(directory);  // throws exception if not directory
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
        // For /home/user/file.zip create zip/home/user/file/ in the cache dir
        File localCacheDir =  settings().getLocalCacheDir();
        char sep= File.separatorChar;
        String zname= zipFile.getName().substring(0, zipFile.getName().length()-4) + sep;
        if ( !zname.startsWith("/") && zname.charAt(1)==':' ) {
            zname= String.valueOf( sep ) + String.valueOf(zname.charAt(0)).toLowerCase() + zname.substring(2); // windows
        }
        String zipCacheName = localCacheDir.getAbsolutePath() + sep + "zip" + zname;

        File zipCache = new File(zipCacheName);
        /*if (!zipCache.exists() && !zipCache.mkdirs()) {
            throw new RuntimeException( new IOException( "Error accessing zip cache" ) );
            //TODO: FileSystem.getLocalRoot throws IOException
        }*/
        return zipCache;
    }

    @Override
    public String toString() {
        return "zipfs "+ zipFile.getName();
                
    }
}
