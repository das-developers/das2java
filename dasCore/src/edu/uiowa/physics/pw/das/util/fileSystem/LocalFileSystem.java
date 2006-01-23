/*
 * FileWebFileSystem.java
 *
 * Created on May 14, 2004, 1:02 PM
 */

package edu.uiowa.physics.pw.das.util.fileSystem;

import edu.uiowa.physics.pw.das.util.DasProgressMonitor;
import edu.uiowa.physics.pw.das.util.fileSystem.FileSystem.FileSystemOfflineException;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

/**
 *
 * @author  Jeremy
 */
public class LocalFileSystem extends FileSystem {
    
    File localRoot;
    
    protected LocalFileSystem(URL root) throws FileSystemOfflineException {
        super( root );
        if ( !("file".equals(root.getProtocol()) ) ) {
            throw new IllegalArgumentException("protocol not file: "+root);
        }
        localRoot= new File( root.toString().substring("file:/".length() ) );
        if ( !localRoot.exists() ) {
            File[] roots= File.listRoots();
            if ( Arrays.asList(roots).contains(localRoot) ) {
                throw new FileSystemOfflineException();
            } else {
                throw new IllegalArgumentException( "root does not exist: "+root );
            }
        }
    }
    
    public boolean isDirectory(String filename) {
        return new File( localRoot, filename ).isDirectory();
    }
    
    String getLocalName( File file ) {
        if ( !file.toString().startsWith(localRoot.toString() ) ) {
            throw new IllegalArgumentException( "file \""+file+"\"is not of this web file system" );
        }
        String filename= file.toString().substring(localRoot.toString().length() );
        filename= filename.replaceAll( "\\\\", "/" );
        return filename;
    }
    
    public String[] listDirectory(String directory) {
        File f= new File( localRoot, directory );
        File[] files= f.listFiles();
        String[] result= new String[files.length];
        for ( int i=0; i<files.length; i++ ) result[i]= files[i].getName() + ( files[i].isDirectory() ? "/" : "" );
        return result;
    }
    
    public String[] listDirectory(String directory, String regex ) {
        File f= new File( localRoot, directory );
        final Pattern pattern= Pattern.compile(regex);
        File[] files= f.listFiles( new FilenameFilter() {
            public boolean accept( File file, String name ) {
                return pattern.matcher(name).matches();
            }
        } );
        String[] result= new String[files.length];
        for ( int i=0; i<files.length; i++ ) result[i]= files[i].getName() + ( files[i].isDirectory() ? "/" : "" );
        return result;
    }
    
    public String toString() {
        return "lfs "+localRoot;
    }
    
    public FileObject getFileObject(String filename) {
        return new LocalFileObject( this, localRoot, filename );
    }
    
}
