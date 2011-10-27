/* Copyright (C) 2003-2008 The University of Iowa
 *
 * This file is part of the Das2 <www.das2.org> utilities library.
 *
 * Das2 utilities are free software: you can redistribute and/or modify them
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Das2 utilities are distributed in the hope that they will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * as well as the GNU General Public License along with Das2 utilities.  If
 * not, see <http://www.gnu.org/licenses/>.
 *
 * FileSystem.java
 *
 * Created on May 14, 2004, 12:43 PM
 */

package org.das2.util.filesystem;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.util.DefaultExceptionHandler;
import org.das2.util.ExceptionHandler;
import org.das2.util.ThrowRuntimeExceptionHandler;
import org.das2.util.monitor.NullProgressMonitor;
import org.das2.util.monitor.ProgressMonitor;

/**
 * Filesystems provide an abstraction layer so that clients can access
 * any heirarchy of files in a implementation-independent way.  For example,
 * remote filesystems accessible via http are accessible through the same
 * interface as a local filesystem.
 *
 * @author  Jeremy
 */


public abstract class FileSystem  {

    URI root;
    protected static final Logger logger= Logger.getLogger(  "das2.filesystem" );
    
    public static class FileSystemOfflineException extends IOException {
        public FileSystemOfflineException() {
            super();
        }
        public FileSystemOfflineException( String message ) {
            super( message );
        }
        public FileSystemOfflineException( IOException e ) {
            super( e.getMessage() );
            initCause(e);
        }
        public FileSystemOfflineException( IOException e, URI root ) {
            super( e.getMessage() + ": "+root );
            initCause(e);
        }
    }
    
    private static final Map<URI,FileSystem> instances= new HashMap<URI,FileSystem>();

    /**
     *
     * @param root
     * @deprecated use create( URI root ) instead.
     * @return
     * @throws org.das2.util.filesystem.FileSystem.FileSystemOfflineException
     * @throws IllegalArgumentException if the url cannot be converted to a URI.
     */
    public static FileSystem create(URL root) throws FileSystemOfflineException, UnknownHostException {
        try {
            return create( root.toURI(), new NullProgressMonitor() );
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public static FileSystem create( String s ) throws FileSystemOfflineException, UnknownHostException {
        try {
            return create( new URI(s), new NullProgressMonitor() );
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    
    /**
     *
     * @param root
     * @param mon
     * @return
     * @deprecated use create( URI root, ProgressMonitor mon ) instead.
     * @throws org.das2.util.filesystem.FileSystem.FileSystemOfflineException
     * @throws IllegalArgumentException if the url cannot be converted to a URI.
     * @throws IllegalArgumentException if the local root does not exist.
     */
    public synchronized static FileSystem create( URL root, ProgressMonitor mon ) throws FileSystemOfflineException, UnknownHostException {
        try {
            return create(root.toURI(), mon);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * creates a FileSystem, removing and recreating it if it was in the cache.
     * @param root
     * @param mon
     * @return
     * @throws org.das2.util.filesystem.FileSystem.FileSystemOfflineException
     * @throws UnknownHostException
     */
    public synchronized static FileSystem recreate( URI root ) throws FileSystemOfflineException, UnknownHostException {
        return recreate( root, new NullProgressMonitor() );
    }

    /**
     * Creates a FileSystem by parsing the URI and creating the correct FS type.
     * Presently, file, http, and ftp are supported.  If the URI contains a folder
     * ending in .zip and a FileSystemFactory is registered as handling .zip, then
     * The zip file will be transferred and the zip file mounted.
     * 
     * @param root
     * @return
     * @throws org.das2.util.filesystem.FileSystem.FileSystemOfflineException
     * @throws IllegalArgumentException if the URI must be converted to a URL, but cannot.
     * @throws IllegalArgumentException if the local root does not exist.
     */
    public static FileSystem create(URI root) throws FileSystemOfflineException, UnknownHostException {
        return create(root, new NullProgressMonitor());
    }

    /**
     * creates a FileSystem, removing and recreating it if it was in the cache.
     * @param root
     * @param mon
     * @return
     * @throws org.das2.util.filesystem.FileSystem.FileSystemOfflineException
     * @throws UnknownHostException
     */
    public synchronized static FileSystem recreate( URI root, ProgressMonitor mon ) throws FileSystemOfflineException, UnknownHostException {
        FileSystem result= instances.get(root);
        if ( result!=null ) {
            return instances.remove(root);
        }
        return create( root, mon );
    }

    /**
     * Creates a FileSystem by parsing the URI and creating the correct FS type.
     * Presently, file, http, and ftp are supported.  If the URI contains a folder
     * ending in .zip and a FileSystemFactory is registered as handling .zip, then
     * The zip file will be transferred and the zip file mounted.
     *
     * @throws IllegalArgumentException if the URI must be converted to a URL, but cannot.
     * @throws IllegalArgumentException if the local root does not exist.
     */
    public synchronized static FileSystem create( URI root, ProgressMonitor mon ) throws FileSystemOfflineException, UnknownHostException {
        logger.fine("create filesystem "+root);

        FileSystem result= instances.get(root);
        if ( result!=null ) {
            return result;
        }

        FileSystemFactory factory;
        if ( root.getPath()!=null && ( root.getPath().contains(".zip") ||   root.getPath().contains(".ZIP") ) && registry.containsKey("zip") ) {
            try {
                String surl= root.toString();
                int i= surl.indexOf(".zip");
                if ( i==-1 ) i= surl.indexOf(".ZIP");
                String[] ss= FileSystem.splitUrl( surl.substring(0,i+4) );
                URI parent = new URI(ss[2]); //getparent
                String zipname = ss[3].substring(ss[2].length());
                String subdir = surl.substring(i+4);
                FileSystem remote = FileSystem.create(parent);
                File localZipFile = remote.getFileObject(zipname).getFile(mon);
                factory = (FileSystemFactory) registry.get("zip");
                FileSystem zipfs = factory.createFileSystem(localZipFile.toURI());
                if ( subdir.equals("") || subdir.equals("/") ) {
                    result= zipfs;
                } else {
                    result= new SubFileSystem(zipfs, subdir);
                }
            } catch (UnknownHostException ex ) {
                throw ex;
            } catch (URISyntaxException ex) {
                //this shouldn't happen
                throw new RuntimeException(ex);
            } catch (IOException ex) {
                throw new FileSystemOfflineException(ex);
            }
        } else {
            factory= (FileSystemFactory) registry.get(root.getScheme());
        }
        if ( factory==null ) {
            throw new IllegalArgumentException( "unsupported protocol: "+root );
        } else {
            if ( result==null ) { // if we didn't create it in the zip file part
                try {
                    // if we didn't create it in the zip file part
                    result = factory.createFileSystem(root);
                } catch (MalformedURLException ex) {
                    throw new IllegalArgumentException(ex);
                }
            }
        }
        
        instances.put(root, result);
        
        return result;
    }
    
    public static FileSystemSettings settings() {
        return settings;
    }
    
    private static FileSystemSettings settings= new FileSystemSettings();
    
    static HashMap registry;
    static {
        registry= new HashMap();
        registry.put("file",new LocalFileSystemFactory() );
        registry.put("http",new HttpFileSystemFactory() );
        registry.put("https",new HttpFileSystemFactory() );
        registry.put("ftp",new FtpFileSystemFactory() );
    }
        
    public static void registerFileSystemFactory( String proto, FileSystemFactory factory ) {
        registry.put( proto, factory );
    }
    
    protected FileSystem( URI root ) {
        if ( !root.toString().endsWith("/" ) ) {
            String s= root.toString();
            try {
                root = new URI(s + "/");
            } catch (URISyntaxException ex) {
                Logger.getLogger(FileSystem.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException(ex); // shouldn't happen
            }
        }
        this.root= root;
    }
    
    public URI getRootURI() {
        return root;
    }
    
    private static String getRegexFromGlob( String glob ) {
        final String regex= glob.replaceAll("\\.","\\\\.").replaceAll("\\*","\\.\\*").replaceAll("\\?","\\.");
        return regex;
    }
    
    
    /**
     * returns the canonical name /a/b/c.dat of a string that
     * contains backslashes and might not have the leading /
     * and trailing slashes.  Also, double slashes (//) are
     * removed.  Note this is the name of the FileObject
     * within the FileSystem.
     */
    protected static String toCanonicalFilename( String filename ) {
        filename= filename.replaceAll( "\\\\", "/" );
        if ( filename.length()==0 || filename.charAt(0)!='/' ) {
            filename= "/"+filename;
        }
        filename= filename.replaceAll( "//", "/" );
        return filename;
    }
    
    protected static String toCanonicalFolderName( String name ) {
        name= toCanonicalFilename( name );
        if ( !name.endsWith("/") ) name= name + "/";
        return name;
    }
    
    /**
     * return the FileObject that corresponds to the name.
     */
    abstract public FileObject getFileObject( String filename );
    
    abstract public boolean isDirectory( String filename ) throws IOException;
    
    /**
     * returns a list of the names of the files in a directory.  Names ending
     * in "/" are themselves directories, and the "/" is not part of the name.
     * This is optional, and a directory may or may not be tagged with the trailing
     * slash.
     */
    abstract public String[] listDirectory( String directory ) throws IOException;
    
    /**
     * returns a list of the names of the files in a directory that match regex.
     * Trailing slashes on directory names are not part of the name and need
     * not be part of the regex.
     */
    abstract public String[] listDirectory( String directory, String regex ) throws IOException;
    
    /**
     * Boolean.TRUE if the filesystem ignores case, such as Windows local filesystem.
     */
    public static final String PROP_CASE_INSENSITIVE= "caseInsensitive";
    
    protected HashMap properties= new HashMap(5);
    
    public Object getProperty( String name ) {
        return properties.get(name);
    }
    
    /** 
     * return the folder that is a local copy of the filesystem. 
     * For LocalFilesystem, this is the same as the filesystem.  For remote
     * filesystems, this is a folder within their home directory.  
     * Note File.getAbsolutePath() returns the string representation of this root.
     * @return the folder that is a local copy of the filesystem. 
     */
    abstract public File getLocalRoot();
        
    /**
     * create a new filesystem that is a part of this filesystem, rooted at
     * directory.
     */
    public FileSystem createFileSystem( String directory ) throws URISyntaxException {
        try {
            return new SubFileSystem(this, toCanonicalFolderName(directory));
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    
    /**
     * returns a String[5]:
     *   [0] is proto "http://"
     *   [1] will be the host
     *   [2] is proto + path
     *   [3] is proto + path + file
     *   [4] is file ext
     *   [5] is params, not including ?.
     * @param surl an url string to parse.
     */
    public static String[] splitUrl( String surl ) {
        
        if ( !( surl.startsWith("file:/") || surl.startsWith("ftp://") || surl.startsWith("http://") || surl.startsWith("https://") ) ) {
            surl= "file://"+ ( ( surl.charAt(0)=='/' ) ? surl : ( '/' + surl ) ); // Windows c:
        }
        
        int i;
        
        String params=null;
        
        int fileEnd;
        // check for just one ?
        i= surl.indexOf( "?" );
        if ( i != -1 ) {
            fileEnd= i;
            params= surl.substring(i+1);
            i= surl.indexOf("?",i+1);
            if ( i!=-1 ) {
                throw new IllegalArgumentException("too many ??'s!");
            }
        } else {
            fileEnd= surl.length();
        }
        
        i= surl.lastIndexOf("/");
        String surlDir= surl.substring(0,i);
        
        String file= surl.substring(i,fileEnd);
        i= file.lastIndexOf('.');
        String ext;
        if ( i!=-1 ) {
            ext= file.substring(i+1);
        } else {
            ext= "";
        }
        
        // let i2 be the end if the protocol and the beginning of the file.
        int i2= surl.indexOf("://")+3;
        if ( surl.indexOf("://")==-1 && surl.startsWith("file:/" ) ) i2=5;
        int i3= surl.indexOf("/",i2+1);
        if ( i3==-1 ) i3= i2;
        String[] result= new String[6];
        result[0]= surl.substring(0,i2);
        result[1]= surl.substring(0,i3);
        result[2]= surlDir+"/";
        result[3]= surl.substring(0,fileEnd);
        result[4]= ext;
        result[5]= params;
        
        return result;
        
    }

    /**
     * allow applications to specify their own exception handler.
     */
    
    private static ExceptionHandler exceptionHandler= null;

    public static synchronized ExceptionHandler getExceptionHandler() {
        if ( exceptionHandler==null ) {
            String name= "java.awt.headless";
            String deft= "false";
            String val;
            try {
                val= System.getProperty(name, deft);
            } catch ( SecurityException ex ) {
                val= deft;
            }
            boolean headless= "true".equals(val);
            if ( headless ) {
                exceptionHandler= new ThrowRuntimeExceptionHandler();
            } else {
                exceptionHandler= new DefaultExceptionHandler();
            }
        }
        return exceptionHandler;
    }

    public static synchronized void setExceptionHandler( ExceptionHandler eh ) {
        exceptionHandler= eh;
    }
    
    /**
     * DirectoryEntry defines a structure for containing directory entry data.
     */
    public class DirectoryEntry {
        /**
         * the name within the context of the directory.
         */
        public String name; 
        /**
         * the type of entry.  d=directory, f=file
         */
        public char type; 
        /**
         * the length in bytes of the entry
         */
        public long size; 
        /**
         * modified date, in seconds since 1970.
         */
        public long modified;
    }
}
