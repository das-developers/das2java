/*
 * WebFileSystem.java
 *
 * Created on May 14, 2004, 12:43 PM
 */

package edu.uiowa.physics.pw.das.util.fileSystem;

import edu.uiowa.physics.pw.das.util.*;
import java.io.*;
import java.net.*;

/**
 *
 * @author  Jeremy
 */
public abstract class FileSystem  {   
    
    public static FileSystem create( URL root ) throws IOException {        
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
