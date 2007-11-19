/*
 * FileSystem.java
 *
 * Created on May 14, 2004, 12:43 PM
 */

package edu.uiowa.physics.pw.das.util.fileSystem;
import edu.uiowa.physics.pw.das.system.DasLogger;
import edu.uiowa.physics.pw.das.util.*;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Filesystems provide an abstraction layer so that clients can access
 * any heirarchy of files in a implementation-independent way.  For example,
 * remote filesystems accessible via http are accessible through the same
 * interface as a local filesystem.
 *
 * @author  Jeremy
 */


public abstract class FileSystem  {
    
    URL root;
    protected static Logger logger= DasLogger.getLogger( DasLogger.FILESYSTEM_LOG );
    
    public static class FileSystemOfflineException extends IOException {
        FileSystemOfflineException() {
            super();
        }
        FileSystemOfflineException( String message ) {
            super( message );
        }
        FileSystemOfflineException( IOException e ) {
            super( e.getMessage() );
            initCause(e);
        }
    }
    
    /**
     * Creates a FileSystem by parsing the URL and creating the correct FS type.
     * Presently, only "file://" and "http://" are supported.
     */
    public static FileSystem create( URL root ) throws FileSystemOfflineException {
        logger.fine("create filesystem "+root);
        FileSystemFactory factory= (FileSystemFactory) registry.get(root.getProtocol());
        if ( factory==null ) {
            throw new IllegalArgumentException( "unsupported protocol: "+root );
        } else {
            return factory.createFileSystem(root);
        }
    }
    
    static HashMap registry;
    static {
        registry= new HashMap();
        registry.put("file",new LocalFileSystemFactory() );
        registry.put("http",new HttpFileSystemFactory() );
        registry.put("ftp",new FtpFileSystemFactory() );
    }
    
    public static void registerFileSystemFactory( String proto, FileSystemFactory factory ) {
        registry.put( proto, factory );
    }
    
    protected FileSystem( URL root ) {
        if ( !root.toString().endsWith("/" ) ) {
            String s= root.toString();
            try {
                root= new URL( s+"/" );
            } catch ( MalformedURLException e ) {
                throw new RuntimeException(e);
            }
        }
        this.root= root;
    }
    
    public URL getRootURL() {
        return root;
    }
    
    private static String getRegexFromGlob( String glob ) {
        final String regex= glob.replaceAll("\\.","\\\\.").replaceAll("\\*","\\.\\*").replaceAll("\\?","\\.");
        return regex;
    }
    
    protected void handleException( Exception e ) {
        DasExceptionHandler.handle(e);
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
    
    abstract public boolean isDirectory( String filename );
    
    /**
     * returns a list of the names of the files in a directory.  Names ending
     * in "/" are themselves directories, and the "/" is not part of the name.
     * This is optional, and a directory may or may not be tagged with the trailing
     * slash.
     */
    abstract public String[] listDirectory( String directory );
    
    /**
     * returns a list of the names of the files in a directory that match regex.
     * Trailing slashes on directory names are not part of the name and need
     * not be part of the regex.
     */
    abstract public String[] listDirectory( String directory, String regex );
    
    /**
     * Boolean.TRUE if the filesystem ignores case, such as Windows local filesystem.
     */
    public static final String PROP_CASE_INSENSITIVE= "caseInsensitive";
    
    protected HashMap properties= new HashMap(5);
    
    public Object getProperty( String name ) {
        return properties.get(name);
    }
    
    /**
     * create a new filesystem that is a part of this filesystem, rooted at
     * directory.
     */
    public FileSystem createFileSystem( String directory ) {
        try {
            return new SubFileSystem( this, toCanonicalFolderName(directory) );
        } catch ( MalformedURLException e ) {
            throw new IllegalArgumentException("invalid directory: "+directory);
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
        String[] result= new String[6];
        result[0]= surl.substring(0,i2);
        result[1]= surl.substring(0,i3);
        result[2]= surlDir+"/";
        result[3]= surl.substring(0,fileEnd);
        result[4]= ext;
        result[5]= params;
        
        return result;
        
    }
}
