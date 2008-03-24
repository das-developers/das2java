/*
 * WebFileSystem.java
 *
 * Created on May 13, 2004, 1:22 PM
 *
 * A WebFileSystem allows web files to be opened just as if they were
 * local files, since it manages the transfer of the file to a local
 * file system.
 */

package org.das2.util.filesystem;
import edu.uiowa.physics.pw.das.DasApplication;
import edu.uiowa.physics.pw.das.util.*;
import org.das2.util.filesystem.FileSystem.FileSystemOfflineException;
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
    
    /**
     * Keep track of active downloads.  This handles, for example, the case
     * where the same file is requested several times by different threads.
     */
    private HashMap downloads= new HashMap();
    
    private String userPass;
    
    /** Creates a new instance of WebFileSystem */
    private HttpFileSystem(URL root, File localRoot) {
        super( root, localRoot );
        listings= new HashMap();
    }
    
    public static synchronized HttpFileSystem createHttpFileSystem( URL root ) throws FileSystemOfflineException {
        if ( instances.containsKey( root.toString() ) ) {
            logger.finer("reusing "+root);
            return (HttpFileSystem)instances.get(root.toString());
        } else {
            try {
                // verify URL is valid and accessible
                HttpURLConnection urlc= (HttpURLConnection)root.openConnection();
                urlc.setRequestMethod("HEAD");
                if ( root.getUserInfo()!=null ) {
                    String encode= Base64.encodeBytes(root.getUserInfo().getBytes());
                    // xerces String encode= new String( Base64.encode(root.getUserInfo().getBytes()) );
                    urlc.setRequestProperty("Authorization", "Basic " + encode);
                }
                urlc.connect();
                if ( urlc.getResponseCode()!=HttpURLConnection.HTTP_OK ) {
                    throw new FileSystemOfflineException( "" + urlc.getResponseCode() + ": " + urlc.getResponseMessage() );
                }
                File local= localRoot( root );
                logger.finer("initializing httpfs "+root+" at "+local);
                HttpFileSystem result= new HttpFileSystem( root, local );
                instances.put(root.toString(),result);
                return result;
            } catch ( FileSystemOfflineException e ) {
                throw e;
            } catch ( IOException e ) {
                throw new FileSystemOfflineException(e);
            }
        }
    }
    
    protected void downloadFile( String filename, File f, File partFile, DasProgressMonitor monitor ) throws IOException {
        
        logger.fine("downloadFile("+filename+")");
        
        boolean waitForAnother;
        synchronized ( downloads ) {
            DasProgressMonitor mon= (DasProgressMonitor) downloads.get( filename );
            if ( mon!=null ) { // the httpFS is already loading this file, so wait.
                monitor.setProgressMessage( "Waiting for file to download" );
                while ( mon!=null ) {
                    while ( !mon.isStarted() ) {
                        try { Thread.sleep(100); } catch ( InterruptedException e) {}
                    }
                    monitor.setTaskSize(mon.getTaskSize());
                    monitor.started();
                    if ( monitor.isCancelled() ) mon.cancel();
                    monitor.setTaskProgress( mon.getTaskProgress() );
                    try { downloads.wait(100); } catch ( InterruptedException e ) { throw new RuntimeException(e); }
                    mon= (DasProgressMonitor) downloads.get( filename );
                    logger.finest( "waiting for download" );
                    
                }
                monitor.finished();
                if ( f.exists() ) {
                    return;
                } else {
                    throw new FileNotFoundException("expected to find "+f);
                }
            } else {
                downloads.put( filename, monitor );
                waitForAnother= false;
            }
        }
        
        try {
            logger.info("downloadFile "+filename);
            
            URL remoteURL= new URL( root.toString()+filename );
            
            URLConnection urlc = remoteURL.openConnection();
            if ( root.getUserInfo()!=null ) {
                String encode= new String( Base64.encodeBytes(root.getUserInfo().getBytes()) );
                urlc.setRequestProperty("Authorization", "Basic " + encode);
            }
            
            HttpURLConnection hurlc= (HttpURLConnection)urlc;
            if ( hurlc.getResponseCode()!=200 ) {
                System.err.println(""+hurlc.getResponseCode()+" URL: "+remoteURL);
                throw new IOException( hurlc.getResponseMessage() );
            }
            
            monitor.setTaskSize( urlc.getContentLength() );
            
            if ( !f.getParentFile().exists() ) {
                logger.fine("make dirs "+f.getParentFile());
                f.getParentFile().mkdirs();
            }
            if ( partFile.exists() ) {
                logger.fine("clobber file "+f);
                if ( !partFile.delete() ) {
                    logger.info("Unable to clobber file "+f+", better use it for now." );
                    return;
                }
            }
            
            if ( partFile.createNewFile() ) {
                InputStream in;
                in= urlc.getInputStream();
                
                in= DasApplication.getDefaultApplication().getInputStreamMeter().meterInputStream(in);
                logger.fine("transferring bytes of "+filename);
                FileOutputStream out= new FileOutputStream( partFile );
                monitor.setLabel( "downloading file" );
                monitor.started();
                try {
                    copyStream( in, out, monitor );
                    monitor.finished();
                    out.close();
                    in.close();
                    partFile.renameTo(f);
                } catch ( IOException e ) {
                    out.close();
                    in.close();
                    partFile.delete();
                    throw e;
                }
            } else {
                throw new IOException( "couldn't create local file: "+f );
            }
        } finally {
            synchronized ( downloads ) {
                downloads.remove( filename );
                downloads.notifyAll();
            }
        }
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
