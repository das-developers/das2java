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
        if ( "file".equals(root.getProtocol()) ) {
            return new LocalFileSystem( root );
        } else if ( "http".equals( root.getProtocol() ) ) {
            return HttpFileSystem.createHttpFileSystem( root );
        } else if ( "ftp".equals( root.getProtocol() ) ) {
            return new FTPFileSystem(root);
        } else {
            throw new IllegalArgumentException( "unsupported protocol: "+root );
        }
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
     * 
     */
    abstract public String[] listDirectory( String directory );
    
    /**
     * returns a list of the names of the files in a directory that match regex.
     * Trailing slashes on directory names are not part of the name and need 
     * not be part of the regex.
     */
    abstract public String[] listDirectory( String directory, String regex );
 
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
    
}
