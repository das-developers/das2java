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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.util.DefaultExceptionHandler;
import org.das2.util.ExceptionHandler;
import org.das2.util.FileUtil;
import org.das2.util.ThrowRuntimeExceptionHandler;
import org.das2.util.monitor.NullProgressMonitor;
import org.das2.util.monitor.ProgressMonitor;

/**
 * Filesystems provide an abstraction layer so that clients can access
 * any hierarchy of files in a implementation-independent way.  For example,
 * remote filesystems accessible via HTTP are accessible through the same
 * interface as a local filesystem.
 *
 * @author  Jeremy
 */
public abstract class FileSystem  {

    URI root;
    protected static final Logger logger= org.das2.util.LoggerManager.getLogger( "das2.filesystem" );
    
    /**
     * this logger is for opening connections to remote sites.
     */
    protected static final Logger loggerUrl= org.das2.util.LoggerManager.getLogger( "das2.url" );
    
    /**
     * Exception indicating the file system is off-line.  For example, if the network is not
     * available, fresh listings of an http site cannot be accessed.
     */
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
    
    private static final Map<URI,FileSystem> instances= Collections.synchronizedMap( new HashMap<URI,FileSystem>() );

    /**
     * non-null means filesystem is bring created and we should wait.
     */
    private static final Map<URI,Object> blocks= Collections.synchronizedMap( new HashMap() );

    /**
     *
     * @param root
     * @throws java.net.UnknownHostException
     * @throws java.io.FileNotFoundException
     * @return the FileSystem
     * @throws org.das2.util.filesystem.FileSystem.FileSystemOfflineException
     * @throws IllegalArgumentException if the url cannot be converted to a URI.
     * @deprecated use create( URI root ) instead.
     */
    public static FileSystem create(URL root) throws FileSystemOfflineException, UnknownHostException, FileNotFoundException {
        try {
            return create( root.toURI(), new NullProgressMonitor() );
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * convenient method that converts string like "http://das2.org/" into a URI.
     * @param s string representation of URI, like "http://das2.org/" or "file:///tmp/"
     * @return FileSystem object.
     * @throws org.das2.util.filesystem.FileSystem.FileSystemOfflineException
     * @throws UnknownHostException
     * @throws FileNotFoundException 
     */
    public static FileSystem create( String s ) throws FileSystemOfflineException, UnknownHostException, FileNotFoundException {
        return create( s, new NullProgressMonitor() );
    }
    
    /**
     * convenient method that converts string like "http://das2.org/" into a URI.
     * See http://stackoverflow.com/questions/573184/java-convert-string-to-valid-uri-object , about halfway down for Feb 21 '09 answer.
     * @param s string representation of URI, like "http://das2.org/" or "file:///tmp/"
     * @param mon monitor progress.  For most FS types this is instantaneous, but for zip this can take sub-interactive time.
     * @return FileSystem object.
     * @throws org.das2.util.filesystem.FileSystem.FileSystemOfflineException
     * @throws UnknownHostException
     * @throws FileNotFoundException 
     */
    public static FileSystem create( String s, ProgressMonitor mon ) throws FileSystemOfflineException, UnknownHostException, FileNotFoundException {
        String[] parts = s.split(":",2);
        if ( parts.length==1 ) {
            throw new IllegalArgumentException( "name must start with scheme like 'file:', no colon found");
        }
        try {
            return create( new URI( FileSystemUtil.uriEncode(s) ), mon );  
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException( ex );
        }
    }

    /**
     *
     * @param root
     * @param mon
     * @return
     * @throws java.net.UnknownHostException
     * @throws java.io.FileNotFoundException
     * @deprecated use create( URI root, ProgressMonitor mon ) instead.
     * @throws org.das2.util.filesystem.FileSystem.FileSystemOfflineException
     * @throws IllegalArgumentException if the url cannot be converted to a URI.
     * @throws IllegalArgumentException if the local root does not exist.
     */
    public static FileSystem create( URL root, ProgressMonitor mon ) throws FileSystemOfflineException, UnknownHostException, FileNotFoundException {
        try {
            return create(root.toURI(), mon);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * creates a FileSystem, removing and recreating it if it was in the cache.
     * @param root
     * @return
     * @throws org.das2.util.filesystem.FileSystem.FileSystemOfflineException
     * @throws UnknownHostException
     * @throws FileNotFoundException if the remote folder is not found.
     */
    public static FileSystem recreate( URI root ) throws FileSystemOfflineException, UnknownHostException, FileNotFoundException {
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
     * @throws java.net.UnknownHostException
     * @throws java.io.FileNotFoundException
     * @throws IllegalArgumentException if the URI must be converted to a URL, but cannot.
     * @throws IllegalArgumentException if the local root does not exist.
     */
    public static FileSystem create(URI root) throws FileSystemOfflineException, UnknownHostException, FileNotFoundException {
        return create(root, new NullProgressMonitor());
    }

    /**
     * creates a FileSystem, removing and recreating it if it was in the cache.
     * @param root
     * @param mon
     * @return
     * @throws org.das2.util.filesystem.FileSystem.FileSystemOfflineException
     * @throws UnknownHostException
     * @throws FileNotFoundException if the file is not found on the remote host.
     */
    public static FileSystem recreate( URI root, ProgressMonitor mon ) throws FileSystemOfflineException, UnknownHostException, FileNotFoundException {
        //TODO: there may be a need to synchronize here
        FileSystem result= instances.get(root);
        if ( result!=null ) {
            return instances.remove(root);
        }
        return create( root, mon );
    }

    /**
     * allow factories to peek, so they can see if there is a parent that is offline.
     * @param root the URI 
     * @return null if not existing, or the filesystem for the URI.
     */
    public static FileSystem peek( URI root ) {
        root = toCanonicalFolderName(root);
        FileSystem result = instances.get(root);
        return result;
    }

    /**
     * remove all the cached FileSystem instances.
     */
    public synchronized static void reset() {
        instances.clear();
        blocks.clear();
        KeyChain.getDefault().clearAll();
    }
    
    /**
     * remove all the cached FileSystem instances.
     * @param fs the filesystem
     */
    public synchronized static void reset( FileSystem fs ) {
        instances.remove(fs.getRootURI());
        blocks.remove(fs.getRootURI());
        KeyChain.getDefault().clearUserPassword(fs.getRootURI());
        if ( !FileUtil.deleteWithinFileTree( fs.getLocalRoot(), ".listing" ) ) {
            logger.log(Level.WARNING, "delete all .listing files within tree {0} failed.", settings().getLocalCacheDir());
        }
    }
    
    /**
     * return true if the URI includes part that is within a Zip filesystem.
     * @param root the path to the root.
     * @return true if the path is within a Zip filesystem.
     */
    private static boolean pathIncludesZipFileSystem( URI root ) {
        String p= root.getPath();
        return ( p.contains(".zip") 
                || p.contains(".ZIP")
                || p.contains(".kmz") );
    }

    /**
     * split the URI into the FileSystem containing the zip, the Zip file, and the path within 
     * the zip file.
     * @param root
     * @return 
     */
    private static String[] pathZipSplit( URI root ) {
        String surl= FileSystemUtil.fromUri(root);
        int i= surl.indexOf(".zip");
        if ( i==-1 ) i= surl.indexOf(".ZIP");
        if ( i==-1 ) i= surl.indexOf(".kmz");
        String subdir = surl.substring(i+4);
        String[] ss= FileSystem.splitUrl( surl.substring(0,i+4) );
        return new String[] { ss[2], ss[3].substring(ss[2].length()), subdir };
    }
    
    /**
     * Creates a FileSystem by parsing the URI and creating the correct FS type.
     * Presently, file, http, and ftp are supported.  If the URI contains a folder
     * ending in .zip and a FileSystemFactory is registered as handling .zip, then
     * The zip file will be transferred and the zip file mounted.
     * Note "user" is a special placeholder in http://user@das2.org.
     *
     * @param root the URI, like URI("http://das2.org/") or URI("file:///tmp/")
     * @param mon monitor progress.  For most FS types this is instantaneous, but for zip this can take sub-interactive time.
     * @return the FileSystem implementation
     * @throws org.das2.util.filesystem.FileSystem.FileSystemOfflineException
     * @throws java.net.UnknownHostException
     * @throws java.io.FileNotFoundException
     * @throws IllegalArgumentException if the URI must be converted to a URL, but cannot.
     * @throws IllegalArgumentException if the local root does not exist.
     */
    public static FileSystem create( URI root, ProgressMonitor mon ) throws FileSystemOfflineException, UnknownHostException, FileNotFoundException {
        logger.log(Level.FINER, "request for filesystem {0}", root);

        FileSystem result;

        if ( !root.toString().endsWith("/") ) {
            try {
                root= new URI( root.toString()+"/" );
            } catch ( URISyntaxException ex ) {
            }
        }

        result= instances.get(root);
        if ( result!=null ) {
            return result;
        }
        
        if ( "user".equals(root.getUserInfo()) ) { // HTTP filesystem will add this automatically, so check this as well.
            try {
                URI test= new URI( root.getScheme(), null, root.getHost(), root.getPort(), root.getPath(), root.getQuery(), root.getFragment() );
                result= instances.get(test);
                if ( result!=null ) {
                    return result;
                }
            } catch (URISyntaxException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        
        Object waitObject;
        boolean ishouldwait= false;
        synchronized (blocks) {
            if ( blocks.containsKey(root) ) {
                waitObject= blocks.get(root);
                ishouldwait= true;
                logger.log(Level.FINE, "this thread should wait for waitObject {0} {1}", new Object[]{waitObject, root});
            } else {
                waitObject= new Object(); 
                blocks.put( root, waitObject );
                logger.log(Level.FINE, "created waitObject {0} {1}", new Object[]{waitObject, root});
            }
        }

        if ( ishouldwait ) { // wait until the other thread is done.  If the other thread doesn't put the result in instances, then there's a problem...
            try {
                synchronized ( waitObject ) {
                    while ( blocks.get(root)!=null ) {                        
                        logger.log(Level.FINE, "waiting for {0} {1}", new Object[]{waitObject, root});
                        waitObject.wait(WAIT_TIMEOUT_MS);  
                    }
                    logger.log(Level.FINE, "done waiting for {0}", root);
                }
            } catch (InterruptedException ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
            }

            result= instances.get(root);

            if ( result!=null ) {
                logger.log( Level.FINE,"using existing filesystem {0}", root );
                return result;
            } else {
                // assume the other thread told them what was going on.
                throw new FileSystemOfflineException("other thread failed to create filesystem.");
            }

        } 

        // Ed suggests using a synchronized block instead of the ishouldwait business.  Just have a synchronized 
        // block on the wait object: synchronized(waitObject) {}, and have a double-check within the block
        // for the guys that enter subsequently...
        
        FileSystemFactory factory;
        if ( root.getPath()!=null && pathIncludesZipFileSystem(root) && registry.containsKey("zip") ) {
            try {
                String[] pzs= pathZipSplit(root);
                URI parent = new URI(pzs[0]); //getparent
                String zipname = pzs[1];
                String subdir = pzs[2];
                FileSystem remote = FileSystem.create(parent);
                mon.setProgressMessage("loading zip file");
                File localZipFile = remote.getFileObject(zipname).getFile(mon);
                factory = (FileSystemFactory) registry.get("zip");
                FileSystem zipfs = factory.createFileSystem(localZipFile.toURI());
                if ( subdir.equals("") || subdir.equals("/") ) {
                    result= zipfs;
                } else {
                    result= new SubFileSystem(zipfs, subdir);
                }
            } catch (UnknownHostException | FileNotFoundException ex ) {
                throw ex;
            } catch (URISyntaxException ex) {
                //this shouldn't happen
                throw new RuntimeException(ex);
            } catch (IOException ex) {
                throw new FileSystemOfflineException(ex);
            } finally {
                logger.log( Level.FINE,"created zip new filesystem {0}", root );
                if ( result!=null ) instances.put( root, result);
                blocks.remove(root);
            }
        } else {
            factory= (FileSystemFactory) registry.get(root.getScheme());
        }

        if ( factory==null ) {
            synchronized( waitObject ) {
                logger.log(Level.FINE, "releasing waitObject after factory=null {0}", waitObject);
                blocks.remove(root);
                logger.log(Level.FINE, "releasing waitObject after factory=null {0} (repeat)", waitObject); // need to do this in the finally block in case there was an Exception.
                waitObject.notifyAll(); //TODO: the other threads are going to think it's offline.
            }                
            logger.log(Level.SEVERE, "unsupported protocol: {0}", root);
            throw new IllegalArgumentException( "unsupported protocol: "+root );
            
        } else {
            try {
                if ( result==null ) { // if we didn't create it in the zip file part

                    result = factory.createFileSystem(root);
                    
                }

            } finally {
                logger.log( Level.FINE,"created new filesystem {0}", root );
                if ( result!=null ) {
                    instances.put(root, result);
                    if ( !result.getRootURI().equals(root) ) {
                        instances.put( result.getRootURI(), result);
                    }
                }
                synchronized( waitObject ) {
                    blocks.remove(root);
                    logger.log(Level.FINE, "releasing waitObject {0}", waitObject); // need to do this in the finally block in case there was an Exception.
                    waitObject.notifyAll();
                }
            }

       }

       if ( settings.isOffline() && result instanceof WebFileSystem ) {
           logger.log( Level.FINE,"filesystem is now offline because of settings {0}", root );
           ((WebFileSystem)result).setOffline(true);
       }

       logger.log(Level.FINE, "create provides filesystem: {0}", result);
       return result;
    }
    
    /**
     * timeouts for waits.
     */
    private static final int WAIT_TIMEOUT_MS = 100;

    /**
     * access the file system settings.
     * @return the single settings object.
     */
    public static FileSystemSettings settings() {
        return settings;
    }
    
    private static FileSystemSettings settings= new FileSystemSettings();
    
    private static final HashMap registry;
    static {
        registry= new HashMap();
        registry.put("file",new LocalFileSystemFactory() );
        registry.put("http",new HttpFileSystemFactory() );
        registry.put("https",new HttpFileSystemFactory() );
        registry.put("ftp",new FtpFileSystemFactory() );
		  
		  // The das2 federated catalog navigation is most smoothly handled by creating a
		  // web file system for it.  Leaving this out until after AGU. -cwp
		  // registry.put("dfc",new DfcFileSystemFactory() ); 
    }
        
    /**
     * register a code for handling a filesystem type.  For example, an
     * FTP implementation can be registered to handle URIs like "ftp://autoplot.org/"
     * @param proto protocol identifier, like "ftp" "http" or "sftp"
     * @param factory the factory which will handle the URI.
     */
    public static void registerFileSystemFactory( String proto, FileSystemFactory factory ) {
        registry.put( proto, factory );
    }
    
    protected FileSystem( URI root ) {
        logger.log(Level.FINE, "create new FileSystem: {0}", root);
        if ( !root.toString().endsWith("/" ) ) {
            String s= root.toString();
            try {
                root = new URI(s + "/");
            } catch (URISyntaxException ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
                throw new RuntimeException(ex); // shouldn't happen
            }
        }
        this.root= root;
    }
    
    /**
     * return the URI identifying the root of the filesystem.
     * @return 
     */
    public URI getRootURI() {
        return root;
    }
    
    private static String getRegexFromGlob( String glob ) {
        final String regex= glob.replaceAll("\\.","\\\\.").replaceAll("\\*","\\.\\*").replaceAll("\\?","\\.");
        return regex;
    }
    
    
    /**
     * returns the canonical name /a/b/c.dat of a string that
     * may contain backslashes and might not have the leading /
     * and trailing slashes.  Also, double slashes (//) are
     * removed.  Even for Windows files, forward slashes are used.
     * @param filename name 
     * @return name with \ converted to /, etc.
     */
    public static String toCanonicalFilename( String filename ) {
        filename= filename.replaceAll( "\\\\", "/" );
        if ( filename.length()==0 || filename.charAt(0)!='/' ) {
            filename= "/"+filename;
        }
        filename= filename.replaceAll( "//", "/" );
        return filename;
    }
    
    /**
     * returns the canonical name (/a/b/) of a string that
     * may contain backslashes and might not have the leading /
     * and trailing slashes.  Also, double slashes (//) are
     * removed.  Note this is the name of the FileObject
     * within the FileSystem.  
     * @param name folder name
     * @return name with \ converted to /, etc.
     */
    public static String toCanonicalFolderName( String name ) {
        name= toCanonicalFilename( name );
        if ( !name.endsWith("/") ) name= name + "/";
        return name;
    }

    protected static URI toCanonicalFolderName( URI name ) {
        try {
            String sname= name.toString();
            if ( !sname.endsWith("/") ) sname= sname + "/";
            return new URI(sname);
        } catch ( URISyntaxException ex ) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * return the FileObject that corresponds to the name.
     * @param filename the file name within the filesystem
     * @return the FileObject
     */
    public abstract FileObject getFileObject( String filename );
    
    /**
     * return true if the name is a directory.
     * @param filename the name
     * @return true if the name is a directory.
     * @throws IOException 
     */
    public abstract boolean isDirectory( String filename ) throws IOException;
    
    /**
     * returns a list of the names of the files in a directory.  Names ending
     * in "/" are themselves directories, and the "/" is not part of the name.
     * This is optional, and a directory may or may not be tagged with the trailing
     * slash.
     * @param directory the directory name within the filesystem.
     * @return list of files and folders within the filesystem.
     * @throws java.io.IOException 
     */
    abstract public String[] listDirectory( String directory ) throws IOException;

    /**
     * returns a list of the names of the files in a directory.  Names ending
     * in "/" are themselves directories, and the "/" is not part of the name.
     * This is optional, and a directory may or may not be tagged with the trailing
     * slash.  (REALLY?  This needs to be fixed...)
     * 
     * @param directory the directory name within the filesystem.
     * @param monitor a progress monitor for the task.
     * @return list of files and folders within the filesystem.
     * @throws java.io.IOException 
     */
    public String[] listDirectory( String directory, ProgressMonitor monitor ) throws IOException {
        monitor.started();
        monitor.setProgressMessage( "listing "+directory );
        try {
            String[] result= listDirectory( directory );        
            return result;
        } finally {
            monitor.finished();
        }
    }
    
    /**
     * returns a list of the names of the files in a directory that match regex.
     * Trailing slashes on directory names are not part of the name and need
     * not be part of the regex.  
     * Note regex is a regular expression (.*\.dat), not a glob (*.dat)
     * @param directory the directory
     * @param regex regular expression
     * @return
     * @throws IOException
     */
    abstract public String[] listDirectory( String directory, String regex ) throws IOException;
    
    /**
     * returns a list of the names of the files in a directory that match regex.
     * Trailing slashes on directory names are not part of the name and need
     * not be part of the regex.  
     * Note regex is a regular expression (.*\.dat), not a glob (*.dat)
     * 
     * @param directory
     * @param regex regular expression that must be matched, or null.
     * @param monitor progress monitor for the task.  
     * @return names of files within the directory which match the regex.
     * @throws IOException 
     */
    public String[] listDirectory( String directory, String regex, ProgressMonitor monitor ) throws IOException {
        monitor.started();
        monitor.setProgressMessage( "listing "+directory );
        try {
            String[] result= listDirectory( directory, regex );
            return result;
        } finally {
            monitor.finished();
        }
    }
    
    /**
     * do a deep listing of directories, resolving wildcards along the way.  Note this
     * can be quite expensive, so be careful when levels are too deep.
     * @param directory location within the filesystem.
     * @param regex regular expression (.*\.dat) (not a glob like *.dat).
     * @return the entire path of each matching name, including the directory within the filesystem.
     * @throws IOException
     */
    public String[] listDirectoryDeep( String directory, String regex ) throws IOException {
        String[] arrayResult= listDirectoryDeep( directory, regex, 1 );
        Arrays.sort(arrayResult);
        return arrayResult;
    }
    
    /**
     * do a deep listing of directories, resolving wildcards along the way.  Note this
     * can be quite expensive, so be careful when levels are too deep.
     * @param directory
     * @param regex regular expression (.*\.dat) (not a glob like *.dat).
     * @return the entire path, including the directory.
     * @throws IOException
     */
    private String[] listDirectoryDeep( String directory, String regex, int level ) throws IOException {    
        List<String> result= new ArrayList();
        int i= regex.indexOf( "/" );
        logger.fine( String.format( "listDirectoryDeep(%s,%s)\n",directory,regex) );
        String[] ss;
        switch (i) {
            case -1:
                ss= listDirectory( directory, regex );
                for ( int j=0; j<ss.length; j++ ) {
                    ss[j]= directory + ss[j];
                }
                return ss;
            case 0:
                ss= listDirectory( directory, regex.substring(1) );
                for ( int j=0; j<ss.length; j++ ) {
                    ss[j]= directory + ss[j];
                }
                return ss;
            default:
                ss= listDirectory( directory, regex.substring(0,i) );
                if ( ss.length==1 && ss[0].length()==(i+1) && ss[0].substring(0,i).equals(regex.substring(0,i) ) ) {
                    String dir= ss[0];
                    String[] ss1= listDirectoryDeep( directory + dir, regex.substring(dir.length()), level+1 );
                    return ss1;
                }   
                for ( String s: ss ) {
                    if ( s.endsWith("/") ) {
                        String[] ss1= listDirectoryDeep( directory+s, regex.substring(i+1), level+1 );
                        for ( String s1: ss1 ) {
                            result.add( s1 );
                        }
                    }
                }   break;
        }
        logger.fine( String.format( "listDirectoryDeep(%s,%s,%d)->%d items\n",directory,regex,level,result.size()) );
        String[] arrayResult= result.toArray( new String[result.size()] );
        return arrayResult;
    }
    /**
     * Boolean.TRUE if the filesystem ignores case, such as Windows local filesystem.
     */
    public static final String PROP_CASE_INSENSITIVE= "caseInsensitive";
    
    protected HashMap properties= new HashMap(5);
    
    /**
     * return a filesystem property, such as PROP_CASE_INSENSITIVE.
     * @param name property name, e.g. PROP_CASE_INSENSITIVE
     * @return the property value, e.g. Boolean.TRUE
     */
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
    public abstract File getLocalRoot();
        
    /**
     * create a new filesystem that is a part of this filesystem, rooted at
     * directory.
     * @param directory subdirectory within the filesystem.
     * @return the new FileSystem
     * @throws java.net.URISyntaxException
     */
    public FileSystem createFileSystem( String directory ) throws URISyntaxException {
        try {
            return new SubFileSystem(this, toCanonicalFolderName(directory));
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    
    /**
     * returns a String[5]:<code><pre>
     *   [0] is proto "http://"
     *   [1] will be the host
     *   [2] is proto + path
     *   [3] is proto + path + file
     *   [4] is file ext
     *   [5] is params, not including ?.
     * </pre></code>
     * The URL must start with one of file:, ftp://, http://, https://, sftp://
     * and "c:" is interpreted as "file:///c:..."
     * @param surl a URL string to parse.
     * @return the parsed URL.
     */
    public static String[] splitUrl( String surl ) {
        
        int icolon= surl.indexOf(":");
        if ( surl.charAt(0)=='/' ) {
            surl= "file://"+surl;
            icolon= 4;
        }
        if ( !registry.keySet().contains( surl.substring(0,icolon).toLowerCase() ) ) {
            if ( icolon==1 ) {
                surl= "file://"+ ( ( surl.charAt(0)=='/' ) ? surl : ( '/' + surl ) ); // Windows c:
            }
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
        if ( i2==2 && surl.startsWith("file:/" ) ) i2=5; // if it didn't contain :// and ...
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

    /**
     * return the exception handler.
     * @return  the exception handler.
     */
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

    /**
     * set the exception handler that is called when exceptions occur.
     * @param eh the exception handler.
     */
    public static synchronized void setExceptionHandler( ExceptionHandler eh ) {
        exceptionHandler= eh;
    }
    
    /**
     * DirectoryEntry defines a structure for containing directory entry data.
     */
    public static class DirectoryEntry {
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

    /**
     * result from failed listing, etc.
     */
    public static final DirectoryEntry NULL= new DirectoryEntry();

    /**
     * part of the refactoring to cache time stamps as well, this convenience method returns the old string.
     * This returns des.name, plus '/' if it's a directory.
     * @param des array of directory entries.
     * @return des.name, and
     */
    public static String[] getListing( DirectoryEntry[] des ) {
        String[] result= new String[des.length];
        for ( int i=0; i<des.length; i++ ) {
            result[i]= des[i].name + ( des[i].type=='d' ? "/" : "" );
        }
        return result;
    }

    /**
     * part of the refactoring to cache time stamps as well, this convenience method returns the old string.
     * This returns des.name, plus '/' if it's a directory.
     * @param des Map of directory entries.
     * @return des.name, and
     */
    public static String[] getListing( Map<String,DirectoryEntry> des ) {
        String[] result= new String[des.size()];
        Collection<DirectoryEntry> ddes= des.values();
        int i=0;
        for ( DirectoryEntry ent : ddes ) {
            result[i]= ent.name + ( ent.type=='d' ? "/" : "" );
            i=i+1;
        }
        return result;
    }
    
    /**
     * return a copy of all cached filesystems
     * @return
     */
    public static FileSystem[] peekInstances() {
        int s= instances.size();
        return instances.values().toArray( new FileSystem[s] );
    }
}
