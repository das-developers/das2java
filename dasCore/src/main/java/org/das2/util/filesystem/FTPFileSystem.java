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
 * FTPFileSystem.java
 *
 * Created on August 17, 2005, 3:33 PM
 *
 */

package org.das2.util.filesystem;

import org.das2.util.monitor.ProgressMonitor;
import org.das2.util.monitor.NullProgressMonitor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Jeremy
 */
public class FTPFileSystem extends WebFileSystem {
    FTPFileSystem( URL root ) {
        super( root, localRoot(root) );
    }
    
    public boolean isDirectory(String filename) {
        return filename.endsWith("/");
    }
    
    private String[] parseLsl( String dir, File listing ) throws IOException {
        InputStream in= new FileInputStream( listing );
        
        BufferedReader reader= new BufferedReader( new InputStreamReader( in ) );
        
        String aline= reader.readLine();
        
        boolean done= aline==null;
        
        String types="d-";
        
        long bytesRead= 0;
        long totalSize;
        long sumSize=0;
        
        List result= new ArrayList( 20 );
        while ( ! done ) {
            
            bytesRead= bytesRead+ aline.length() + 1;
            
            aline= aline.trim();
            
            if ( aline.length() == 0 ) {
                done=true;
            } else {
                
                char type= aline.charAt(0);
                if ( type == 't' ) {
                    if ( aline.indexOf( "total" ) == 0 ) {
                        //totalSize= Long.parseLong( aline.substring( 5 ).trim() );
                    }
                }
                
                if ( types.indexOf(type)!=-1 ) {
                    int i= aline.lastIndexOf( ' ' );
                    String name= aline.substring( i+1 );
                    //long size= Long.parseLong( aline.substring( 31, 31+12 ) ); // tested on: linux
                    boolean isFolder= type=='d';
                    
                    result.add( name + ( isFolder ? "/" : "" ) );
                    
                    //sumSize= sumSize + size;
                    
                }
                
                aline= reader.readLine();
                done= aline==null;
                
            } // while
            
        }
        return (String[])result.toArray(new String[result.size()]);
    }
    
    public String[] listDirectory(String directory) {
        directory= toCanonicalFolderName( directory );
        
        try {
            new File( localRoot, directory ).mkdirs();
            File listing= new File( localRoot, directory + ".listing" );
            if ( !listing.canRead() ) {
                File partFile= listing;
                downloadFile( directory, listing, partFile, new NullProgressMonitor() );
            }
            listing.deleteOnExit();
            return parseLsl( directory, listing );
        } catch ( IOException e ) {
            throw new RuntimeException(e);
        }
    }
    
    protected void downloadFile(String filename, java.io.File targetFile, File partFile, ProgressMonitor monitor ) throws java.io.IOException {
        FileOutputStream out=null;
        InputStream is= null;
        try {
            filename= toCanonicalFilename( filename );
            URL url= new URL( root + filename.substring(1) );
            
            URLConnection urlc = url.openConnection();
            
            int i= urlc.getContentLength();
            monitor.setTaskSize( i );
            out= new FileOutputStream( partFile );
            is = urlc.getInputStream(); // To download
            monitor.started();
            copyStream(is, out, monitor );
            monitor.finished();
            out.close();
            is.close();
            partFile.renameTo( targetFile );
        } catch ( IOException e ) {
            if ( out!=null ) out.close();
            if ( is!=null ) is.close();
            partFile.delete();
            throw e;
        }
        
    }
    
}