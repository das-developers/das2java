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
import edu.uiowa.physics.pw.das.util.*;
import edu.uiowa.physics.pw.das.util.fileSystem.FileSystem.FileSystemOfflineException;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;


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
            try {
                // verify URL is valid an accessible
                HttpURLConnection urlc= (HttpURLConnection)root.openConnection();
                urlc.setRequestMethod("HEAD");
                urlc.connect();
                if ( urlc.getResponseCode()!=HttpURLConnection.HTTP_OK ) {
                    throw new FileSystemOfflineException( "" + urlc.getResponseCode() + ": " + urlc.getResponseMessage() );
                }
                File local= localRoot( root );
                logger.finer("initializing httpfs "+root+" at "+local);
                HttpFileSystem result= new HttpFileSystem( root, local );
                instances.put(root,result);
                return result;
            } catch ( FileSystemOfflineException e ) {
                throw e;
            } catch ( IOException e ) {
                throw new FileSystemOfflineException(e);                
            }
        }
    }
    
    protected void downloadFile( String filename, File f, DasProgressMonitor monitor ) throws IOException {
        logger.info("transferFile "+filename);
        
        URL remoteURL= new URL( root.toString()+filename );
        
        URLConnection urlc = remoteURL.openConnection();
        monitor.setTaskSize( urlc.getContentLength() );
        
        InputStream in= urlc.getInputStream();
        
        if ( !f.getParentFile().exists() ) {
            logger.fine("make dirs "+f.getParentFile());
            f.getParentFile().mkdirs();
        }
        if ( f.exists() ) {
            logger.fine("clobber file "+f);
            if ( !f.delete() ) {
                logger.info("Unable to clobber file "+f+", better use it for now." );
                return;
            }
        }
        if ( f.createNewFile() ) {
            logger.fine("transferring file "+filename);
            FileOutputStream out= new FileOutputStream( f );
            copyStream( in, out, monitor );
            monitor.finished();
            out.close();
        } else {
            throw new IOException( "couldn't create local file: "+f );
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
                    lookFor= filename.substring(1)+"/";
                } else {
                    lookFor= filename + "/";
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
        directory= this.toCanonicalFilename( directory );
        if ( ! isDirectory( directory ) ) {
            throw new IllegalArgumentException( "is not a directory: "+directory );
        } 
        
        if ( !directory.endsWith("/") ) directory= directory+"/";
        
        synchronized (listings) {
            if ( listings.containsKey(directory) ) {
                return ( String[] ) listings.get(directory);
            } else {
                try {
                    URL[] list= HtmlUtil.getDirectoryListing( getURL(directory ) );
                    String[] result= new String[list.length];
                    int n= directory.length();
                    for ( int i=0; i<list.length; i++ ) {
                        URL url= list[i];
                        result[i]= getLocalName(url).substring(n);
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
    
    // TODO: handle / or not in regex
    public String[] listDirectory( String directory, String regex ) {
        directory= toCanonicalFilename(directory);
        if ( ! isDirectory( directory ) ) {
            throw new IllegalArgumentException( "is not a directory: "+directory );
        }
        
        String[] listing= listDirectory( directory );
        Pattern pattern= Pattern.compile(regex+"/?");
        ArrayList result= new ArrayList();
        for ( int i=0; i<listing.length; i++ ) {
            if ( pattern.matcher(listing[i]).matches() ) {
                result.add(listing[i]);
            }
        }
        return (String[])result.toArray(new String[result.size()]);
        
    }
    
}
