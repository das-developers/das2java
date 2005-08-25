/*
 * WebFileSystem.java
 *
 * Created on May 13, 2004, 1:22 PM
 *
 * A WebFileSystem allows web files to be opened just as if they were
 * local files, since it manages the transfer of the file to a local
 * file system.
 */

package edu.uiowa.physics.pw.das.util.fileSystem;

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.util.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import javax.swing.*;

/**
 *
 * @author  Jeremy
 */
public class HttpFileSystem extends WebFileSystem {
    
    /** Creates a new instance of WebFileSystem */
    private HttpFileSystem(URL root, File localRoot) {
        super( root, localRoot );
    }
    
    public static HttpFileSystem createHttpFileSystem( URL root ) throws FileSystemOfflineException {        
        File local;         
        if ( System.getProperty("user.name").equals("Web") ) {
            local= new File("/tmp");
        } else {
            local= new File( System.getProperty("user.home") );
        }
        local= new File( local, ".das2/fileSystemCache/WebFileSystem/" );
        local= new File( local, root.getHost() );
        local= new File( local, root.getFile() );
        local.mkdirs();
        return new HttpFileSystem( root, local );
    }
    
    protected void transferFile( String filename, File f, DasProgressMonitor monitor ) throws IOException {
        DasApplication.getDefaultApplication().getLogger().fine("create file "+filename);
        URL remoteURL= new URL( root.toString()+filename );
        
        URLConnection urlc = remoteURL.openConnection();
        monitor.setTaskSize( urlc.getContentLength() );
        
        InputStream in= urlc.getInputStream();
        
        if ( !f.getParentFile().exists() ) {
            f.getParentFile().mkdirs();
        }
        if ( f.createNewFile() ) {
            DasApplication.getDefaultApplication().getLogger().fine("transferring file "+filename);
            FileOutputStream out= new FileOutputStream( f );            
            copyStream( in, out, monitor );
            monitor.finished();
            out.close();
        } else {
            handleException( new RuntimeException( "couldn't create local file: "+f ) );
        }
        in.close();
    }
    
    public boolean isDirectory( String filename ) {
        File f= new File( localRoot, filename );
        if ( f.exists() ) {
            return f.isDirectory();
        } else {
            if ( filename.endsWith("/") ) {
                return true;
            } else {
                try {                    
                    File parentFile= f.getParentFile();
                    URL[] urls= HtmlUtil.getDirectoryListing( getURL( getLocalName( parentFile ) ) );
                    URL remoteUrl;
                    if ( filename.startsWith("/") ) {
                        remoteUrl= new URL( root+filename.substring(1)+"/" );
                    } else {
                        remoteUrl= new URL( root+filename+"/" );
                    }
                    for ( int i=0; i<urls.length; i++ ) {
                        if ( urls[i].equals( remoteUrl ) ) {
                            return true;
                        }
                    }
                    return false;
                } catch ( IOException e ) {
                    handleException(e);
                    return false;
                }
            }
        }
    }
    
    public String[] listDirectory( String directory ) {
        if ( ! isDirectory( directory ) ) {
            throw new IllegalArgumentException( "is not a directory: "+directory );
        }
        try {
            URL[] list= HtmlUtil.getDirectoryListing( getURL(directory ) );
            if ( list.length>100 ) {
                throw new IllegalStateException( "URL list is very long, refusing to transfer" );
            }
            String[] result= new String[list.length];
            for ( int i=0; i<list.length; i++ ) {
                URL url= list[i];
                result[i]= getLocalName(url);
            }
            return result;
        } catch ( IOException e ) {
            handleException(e);
            return new String[0];
        }
    }
    
    public String[] listDirectory( String directory, String regex ) {
        directory= toCanonicalFilename(directory);
        if ( ! isDirectory( directory ) ) {
            throw new IllegalArgumentException( "is not a directory: "+directory );
        }
        try {
            Pattern pattern= Pattern.compile(regex);
            URL[] list= HtmlUtil.getDirectoryListing( getURL(directory ) );
            if ( list.length>100 ) {
                throw new IllegalStateException( "URL list is very long, refusing to transfer" );
            }
            ArrayList result= new ArrayList();
            for ( int i=0; i<list.length; i++ ) {
                URL url= list[i];
                String r1= getLocalName(url).substring(directory.length());
                if ( pattern.matcher(r1).matches() ) {
                    result.add(r1);
                }
            }
            return (String[])result.toArray(new String[result.size()]);
            
        } catch ( IOException e ) {
            handleException(e);
            return new String[0];
        }
    }
    
}
