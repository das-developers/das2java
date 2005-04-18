/*
 * FileSystem.java
 *
 * Created on May 14, 2004, 12:43 PM
 */

package edu.uiowa.physics.pw.das.util.fileSystem;

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.util.*;
import java.io.*;
import java.net.*;

/**
 * Filesystems provide an abstraction layer so that clients can access 
 * any heirarchy of files in a implementation-independent way.  For example,
 * remote filesystems accessible via http are accessible through the same 
 * interface as a local filesystem.
 *
 * @author  Jeremy
 */


public abstract class FileSystem  {   
        
    public class FileSystemOfflineException extends IOException {        
    }
    
    /**
     * Creates a FileSystem by parsing the URL and creating the correct FS type.
     * Presently, only "file://" and "http://" are supported.
     */
    public static FileSystem create( URL root ) throws FileSystemOfflineException {        
        if ( "file".equals(root.getProtocol()) ) {
            return new LocalFileSystem( root );
        } else if ( "http".equals( root.getProtocol() ) ) {
            return HttpFileSystem.createHttpFileSystem( root );
        } else {
            throw new IllegalArgumentException( "unsupported protocol: "+root );
        }
    }        

    public static String getRegexFromGlob( String glob ) {
        final String regex= glob.replaceAll("\\.","\\\\.").replaceAll("\\*","\\.\\*").replaceAll("\\?","\\.");     
        return regex;
    }
    
    protected void handleException( Exception e ) {
        DasExceptionHandler.handle(e);
    }

    /**
     * returns the canonical name /a/b/c.dat of a string that
     * contains backslashes and might not have the leading /
     * and trailing slashes.
     */    
    protected static String toCanonicalFilename( String filename ) {
        filename= filename.replaceAll( "\\\\", "/" );
        if ( filename.length()==0 || filename.charAt(0)!='/' ) {
            filename= "/"+filename;
        }
        return filename;
    }
    
    abstract public FileObject getFile( String filename );
    abstract public boolean isDirectory( String filename );
    abstract public String[] listDirectory( String directory );
    abstract public String[] listDirectory( String directory, String pattern );
}
