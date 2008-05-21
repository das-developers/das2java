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
 * WebFileSystem.java
 *
 * Created on May 13, 2004, 1:22 PM
 *
 * A WebFileSystem allows web files to be opened just as if they were
 * local files, since it manages the transfer of the file to a local
 * file system.
 */

package org.das2.util.filesystem;

import org.das2.util.monitor.ProgressMonitor;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

/**
 *
 * @author  Jeremy
 */
public abstract class WebFileSystem extends FileSystem {

    public static File getDownloadDirectory() {
        File local;
        if (System.getProperty("user.name").equals("Web")) {
            local = new File("/tmp");
        } else {
            local = new File(System.getProperty("user.home"));
        }
        local = new File(local, ".das2/fsCache/wfs/");

        return local;
    }
    
    protected final File localRoot;
    
    /** Creates a new instance of WebFileSystem */
    protected WebFileSystem(URL root, File localRoot) {
        super( root );        
        this.localRoot= localRoot;
    }
    
    static protected File localRoot( URL root ) {

        File local= getDownloadDirectory();
       
        String s= root.getProtocol() + "/"+ root.getHost() + "/" + root.getFile();
        
        local= new File( local, s );

        local.mkdirs();
        return local;
    }
    
    /**
     * Transfers the file from the remote store to a local copy f.  This should only be
     * used within the class and subclasses, clients should use getFileObject( String ).getFile().
     * Subclasses implementing this should download data to partfile, then rename partfile to
     * f after the download is complete.
     *
     * @param partfile the temporary file during download.
     */
    protected abstract void downloadFile( String filename, File f, File partfile, ProgressMonitor monitor ) throws IOException;
    
	 /** Get the root of the local file cache */
	 public String getLocalRootAbsPath(){  return this.localRoot.getAbsolutePath();	 }
	 
    protected File getLocalRoot() {
        return this.localRoot;
    }
    
    abstract public boolean isDirectory( String filename ) throws IOException;
    
    abstract public String[] listDirectory( String directory ) throws IOException;
    
    public String[] listDirectory( String directory, String regex ) throws IOException {
        String[] names= listDirectory( directory );
        Pattern pattern= Pattern.compile(regex);
        ArrayList result= new ArrayList();
        for ( int i=0; i<names.length; i++ ) {   
            if ( names[i].endsWith("/") ) names[i]= names[i].substring(0,names[i].length()-1);
            if ( pattern.matcher(names[i]).matches() ) {
                result.add(names[i]);
            }
        }
        return (String[])result.toArray(new String[result.size()]);
    }
    
    public URL getURL( String filename ) {
        try {
            filename= FileSystem.toCanonicalFilename(filename);
            return new URL( root+filename.substring(1) );
        } catch ( MalformedURLException e ) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * return the name of the File within the FileSystem, where File is a local
     * file within the local copy of the filesystem.
     */
    public String getLocalName( File file ) {
        if ( !file.toString().startsWith(localRoot.toString() ) ) {
            throw new IllegalArgumentException( "file \""+file+"\"is not of this web file system" );
        }
        String filename= file.toString().substring(localRoot.toString().length() );
        filename= filename.replaceAll( "\\\\", "/" );
        return filename;
    }
    
    public String getLocalName( URL url ) {
        if ( !url.toString().startsWith(root.toString() ) ) {
            throw new IllegalArgumentException( "url \""+url+"\"is not of this web file system" );
        }
        String filename= FileSystem.toCanonicalFilename( url.toString().substring(root.toString().length() ) );
        return filename;
    }
    
    
    public FileObject getFileObject( String filename ) {
        WebFileObject f= new WebFileObject( this, filename, new Date(System.currentTimeMillis()) );
        return f;
    }
    
    /**
     * copies data from in to out, sending the number of bytesTransferred to the monitor.
     */
    protected void copyStream( InputStream is, OutputStream out, ProgressMonitor monitor ) throws IOException {        
        byte[] buffer= new byte[2048];
        int bytesRead= is.read( buffer, 0, 2048 );
        long totalBytesRead= bytesRead;
        while ( bytesRead>-1 ) {
            if ( monitor.isCancelled() ) throw new InterruptedIOException( );
            monitor.setTaskProgress( totalBytesRead );            
            out.write( buffer, 0, bytesRead );            
            bytesRead= is.read( buffer, 0, 2048 );         
            totalBytesRead+= bytesRead;
            logger.finest( "transferring data" );
        }
    }
    
    public String toString() {
        return "wfs "+root;
    }
    
}
