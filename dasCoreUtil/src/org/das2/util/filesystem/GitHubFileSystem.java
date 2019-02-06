
package org.das2.util.filesystem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.util.LoggerManager;
import static org.das2.util.filesystem.FileSystem.loggerUrl;
import static org.das2.util.filesystem.FileSystem.toCanonicalFilename;
import static org.das2.util.filesystem.HtmlUtil.getInputStream;
import static org.das2.util.filesystem.WebFileSystem.localRoot;
import org.das2.util.monitor.CancelledOperationException;
import org.das2.util.monitor.ProgressMonitor;

/**
 *
 * @author jbf
 */
public class GitHubFileSystem extends HttpFileSystem {

    private static final Logger logger= LoggerManager.getLogger("das2.filesystem.wfs.githubfs");
    
    /** 
     * Create a new GitHubFileSystem mirroring the root, a URL pointing to "http" or "https", 
     * in the local folder.
     * @param root the root of the filesystem
     * @param localRoot the local root where files are downloaded.
     */
    protected GitHubFileSystem(URI root, File localRoot) {
        super(root, localRoot);
        this.protocol= null;
    }
    
    public static GitHubFileSystem createGitHubFileSystem( URI root ) {
        File local;

        if (FileSystemSettings.hasAllPermission()) {
            local = localRoot(root);
            logger.log(Level.FINER, "initializing httpfs {0} at {1}", new Object[]{root, local});
        } else {
            local = null;
            logger.log(Level.FINER, "initializing httpfs {0} in applet mode", root);
        }

        return new GitHubFileSystem(root, local);
        
    }
    
    @Override
    public String[] listDirectory(String directory) throws IOException {
        if ( !directory.endsWith("/") ) directory= directory+"/";
        InputStream urlStream= null ;
        try {
            URL url= gitHubMapDir( root, directory );
            urlStream = getInputStream(url);
            URL[] listing= HtmlUtil.getDirectoryListing( url, urlStream, false );
            List<String> result= new ArrayList<>();
            int parentLen= root.toString().length() + ( directory.length() -1 ) + 12;
            for ( URL u: listing ) {
                String su= u.toString();
                if ( su.contains("/blob/master/") ) {
                    result.add( su.substring( parentLen ) );
                } else if ( su.contains("/tree/master/") ) {
                    if ( su.length()>parentLen ) {
                        String ss= u.toString().substring( parentLen ) + "/" ;
                        if ( ss.length()>1 
                                && !ss.contains("#start-of-content") 
                                && !su.contains("return_to=")  ) {
                            result.add( ss );
                        }
                    }
                }
            }
            return result.toArray( new String[result.size()] );
        } catch (CancelledOperationException ex) {
            throw new IOException("cancel pressed");
        } finally {
            try {
                urlStream.close();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        
    }

    @Override
    public FileObject getFileObject(String filename) {
        return new WebFileObject( this, filename, new Date() );        
    }
    
    /**
     * this will be replaced in Java 8.
     * @param c
     * @param delim
     * @param start positive index, or negative from end.
     * @param end positive index, or negative from end.
     * @return 
     */
    public static String strjoin( String[] c, String delim, int start, int end ) {
        StringBuilder result = new StringBuilder();
        if ( start<0 ) start= c.length + start;
        if ( end<0 ) end= c.length + end;
        for ( int i=start; i<end; i++ ) {
            String s= c[i];
            if (result.length() > 0) {
                result.append(delim);
            }
            result.append(s);
        }
        return result.toString();
    }
    
    public static URL gitHubMapFile( URI root, String filename ) throws MalformedURLException {
        filename= toCanonicalFilename( filename );            
        // png image "https://github.com/autoplot/app/raw/master/Autoplot/src/resources/badge_ok.png"
        String[] path= root.getPath().split("/",-2);
        String spath= path[0] + '/' + path[1] + '/' + path[2] ;
        String n= root.getScheme() + "://" + root.getHost() + '/' + spath + "/raw/master/" + strjoin( path, "/", 3, -1 ) + filename;
        URL url= new URL( n );
        return url;
    }

    public static URL gitHubMapDir( URI root, String filename ) throws MalformedURLException {
        filename= toCanonicalFilename( filename );            
        // png image "https://github.com/autoplot/app/raw/master/Autoplot/src/resources/badge_ok.png"
        String[] path= root.getPath().split("/",-2);
        String spath= path[0] + '/' + path[1] + '/' + path[2] ;
        String n= root.getScheme() + "://" + root.getHost() + '/' + spath + "/tree/master/" + strjoin( path, "/", 3, -1 ) + filename;
        URL url= new URL( n );
        return url;
    }
    
    @Override
    protected void downloadFile(String filename, File targetFile, File partFile, ProgressMonitor monitor) throws IOException {
        logger.log(Level.FINE, "downloadFile({0})", filename);
        FileOutputStream out=null;
        InputStream is= null;
        try {
            filename= toCanonicalFilename( filename );
            URL url= gitHubMapFile( root, filename );
            
            URLConnection urlc = url.openConnection();
            
            int i= urlc.getContentLength();
            monitor.setTaskSize( i );
            out= new FileOutputStream( partFile );
            loggerUrl.log(Level.FINE, "getInputStream {0}", new Object[] { urlc.getURL() } );
            is = urlc.getInputStream(); // To download
            monitor.started();
            copyStream(is, out, monitor );
            monitor.finished();
            out.close();
            is.close();
            if ( ! partFile.renameTo( targetFile ) ) {
                throw new IllegalArgumentException("unable to rename "+partFile+" to "+targetFile );
            }
        } catch ( IOException e ) {
            if ( out!=null ) out.close();
            if ( is!=null ) is.close();
            if ( partFile.exists() &&  ! partFile.delete() ) {
                throw new IllegalArgumentException("unable to delete "+partFile );
            }
            throw e;
        }
    }
    
    @Override
    public String toString() {
        return "githubfs " + root;
    }
    
}
