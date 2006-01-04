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
    
    private HashMap listings;
    private static HashMap instances= new HashMap();
    
    /** Creates a new instance of WebFileSystem */
    private HttpFileSystem(URL root, File localRoot) {
        super( root, localRoot );
        listings= new HashMap();
    }
    
    public static synchronized HttpFileSystem createHttpFileSystem( URL root ) throws FileSystemOfflineException {
        if ( instances.containsKey( root ) ) {
            logger.finer("reusing "+root);
            return (HttpFileSystem)instances.get(root);
        } else {
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
            logger.finer("initializing httpfs "+root+" at "+local);
            HttpFileSystem result= new HttpFileSystem( root, local );
            instances.put(root,result);
            return result;
        }
    }
    
    protected void transferFile( String filename, File f, DasProgressMonitor monitor ) throws IOException {
        logger.fine("create file "+filename);
        
        URL remoteURL= new URL( root.toString()+filename );
        
        URLConnection urlc = remoteURL.openConnection();
        monitor.setTaskSize( urlc.getContentLength() );
        
        InputStream in= urlc.getInputStream();
        
        if ( !f.getParentFile().exists() ) {
            f.getParentFile().mkdirs();
        }
        if ( f.createNewFile() ) {
            logger.fine("transferring file "+filename);
            FileOutputStream out= new FileOutputStream( f );
            copyStream( in, out, monitor );
            monitor.finished();
            out.close();
        } else {
            handleException( new RuntimeException( "couldn't create local file: "+f ) );
        }
        in.close();
    }
    
    /* dumb method looks for / in parent directory's listing */
    public boolean isDirectory( String filename ) {
        File f= new File( localRoot, filename );
        if ( f.exists() ) {
            return f.isDirectory();
        } else {
            if ( filename.endsWith("/") ) {
                return true;
            } else {
                File parentFile= f.getParentFile();
                String parent= getLocalName( parentFile );
                if ( !parent.endsWith("/") ) parent=parent+"/";
                
                String[] list= listDirectory( parent );
                String lookFor;
                if ( filename.startsWith("/") ) {
                    lookFor= parent + filename.substring(1)+"/";
                } else {
                    lookFor= parent + filename + "/";
                }
                for ( int i=0; i<list.length; i++ ) {
                    if ( list[i].equals( lookFor ) ) {
                        return true;
                    }
                }
                return false;
            }
        }
    }
    
    public String[] listDirectory( String directory ) {
        if ( ! isDirectory( directory ) ) {
            throw new IllegalArgumentException( "is not a directory: "+directory );
        }        
        synchronized (listings) {
            if ( listings.containsKey(directory) ) {
                return ( String[] ) listings.get(directory);
            } else {
                try {
                    URL[] list= HtmlUtil.getDirectoryListing( getURL(directory ) );
                    String[] result= new String[list.length];
                    for ( int i=0; i<list.length; i++ ) {
                        URL url= list[i];
                        result[i]= getLocalName(url);
                    }
                    listings.put( directory, result );
                    return result;
                } catch ( IOException e ) {
                    handleException(e);
                    return new String[0];
                }
            }
        }
    }
    
    public String[] listDirectory( String directory, String regex ) {
        directory= toCanonicalFilename(directory);
        if ( ! isDirectory( directory ) ) {
            throw new IllegalArgumentException( "is not a directory: "+directory );
        }
        
        String[] listing= listDirectory( directory );
        Pattern pattern= Pattern.compile(regex);
        ArrayList result= new ArrayList();
        int n= directory.length();
        for ( int i=0; i<listing.length; i++ ) {
            String r1= listing[i].substring(n);
            if ( pattern.matcher(r1).matches() ) {
                result.add(r1);
            }
        }
        return (String[])result.toArray(new String[result.size()]);
        
    }
    
}
