
package org.das2.util.filesystem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
            String surl= root.toString() + "tree/master/"+ directory ;
            URL url= new URL(surl);
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

    @Override
    protected void downloadFile(String filename, File targetFile, File partFile, ProgressMonitor monitor) throws IOException {
        FileOutputStream out=null;
        InputStream is= null;
        try {
            filename= toCanonicalFilename( filename );
            String n= root.toASCIIString().replace("github.com", "raw.githubusercontent.com" ) + "master/" + filename.substring(1);
            URL url= new URL( n );
            
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
    
    
    
}
