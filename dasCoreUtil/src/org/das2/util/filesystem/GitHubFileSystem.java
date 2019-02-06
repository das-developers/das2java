
package org.das2.util.filesystem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    
    private class GitHubHttpProtocol implements WebProtocol {

        @Override
        public InputStream getInputStream(WebFileObject fo, ProgressMonitor mon) throws IOException {
            URL gitHubURL= gitHubMapFile( root, fo.getNameExt() );
            return gitHubURL.openStream();
        }

        @Override
        public Map<String, String> getMetadata(WebFileObject fo) throws IOException {
        
            if ( fo.wfs.offline ) {
                Map<String,String> result= new HashMap<>();
                result.put(WebProtocol.META_EXIST, String.valueOf( fo.localFile.exists() ) );
                result.put(WebProtocol.META_LAST_MODIFIED, String.valueOf( fo.localFile.lastModified() ) );
                result.put(WebProtocol.META_CONTENT_LENGTH, String.valueOf( fo.localFile.length() ) );
                result.put(WebProtocol.META_CONTENT_TYPE, Files.probeContentType( fo.localFile.toPath() ) );
                return result;
                
            } else {
                URL ur= gitHubMapFile( root, fo.getNameExt() );
                return HttpUtil.getMetadata( ur, null );
            }
    
        }
        
    }
    /** 
     * Create a new GitHubFileSystem mirroring the root, a URL pointing to "http" or "https", 
     * in the local folder.
     * @param root the root of the filesystem
     * @param localRoot the local root where files are downloaded.
     */
    protected GitHubFileSystem(URI root, File localRoot) {
        super(root, localRoot);
        this.protocol= new GitHubHttpProtocol();
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
        if ( directory.equals("/") && root.getRawPath().equals("/") ) { // list from cache.
            File dir= new File( FileSystem.settings().getLocalCacheDir() + "/" + root.getScheme() + "/" + root.getHost() );
            String[] ss= dir.list();
            for ( int i=0; i<ss.length; i++ ) {
                ss[i]= ss[i] + '/';
            }
            return ss;
        }
        InputStream urlStream= null ;
        try {
            URL url= gitHubMapDir( root, directory );
            String surl= url.toString();
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
                } else if ( su.startsWith(surl) ) {
                    String sub= su.substring(surl.length());
                    if ( sub.length()>0 && sub.charAt(0)!='#' && !sub.contains("/") ) {
                        result.add( sub+"/" );
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
    
    /**
     * github puts directories for each project under "raw/master".
     * @param root
     * @param filename
     * @return
     * @throws MalformedURLException 
     */
    public static URL gitHubMapFile( URI root, String filename ) throws MalformedURLException {
        filename= toCanonicalFilename( filename );            
        // png image "https://github.com/autoplot/app/raw/master/Autoplot/src/resources/badge_ok.png"
        String[] path= root.getPath().split("/",-2);
        String spath= path[0] + '/' + path[1] + '/' + path[2] ;
        String n= root.getScheme() + "://" + root.getHost() + '/' + spath + "/raw/master/" + strjoin( path, "/", 3, -1 ) + filename;
        URL url= new URL( n );
        return url;
    }

    /**
     * github puts directories for each project under "tree/master".
     * @param root
     * @param filename
     * @return
     * @throws MalformedURLException 
     */
    public static URL gitHubMapDir( URI root, String filename ) throws MalformedURLException {
        filename= toCanonicalFilename( filename );            
        // png image "https://github.com/autoplot/app/raw/master/Autoplot/src/resources/badge_ok.png"
        String[] path= root.getPath().split("/",-2);
        String spath= path[0] + '/' + path[1] + '/' + path[2] ;
        String n;
        if ( path.length==3 && filename.length()==1 ) {
            return root.toURL();
        } else {
            n= root.getScheme() + "://" + root.getHost() + '/' + spath + "/tree/master/" + strjoin( path, "/", 3, -1 ) + filename;
            URL url= new URL( n );
            return url;
        }
    }

    @Override
    public URI getURI(String filename) {
        try {
            return getURL(filename).toURI();
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * return the URL for the internal filename
     * @param filename internal filename
     * @return 
     */
    @Override
    public URL getURL(String filename) {
        filename = FileSystem.toCanonicalFilename(filename);
        if ( filename.endsWith("/") ) {
            try {
                return gitHubMapDir( root, filename );
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            try {
                return gitHubMapFile( root, filename );
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            }
        }
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
            String etag= urlc.getHeaderField("ETag");
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
            File metadir= new File( targetFile.getParentFile(), ".meta");
            if ( !metadir.exists() ) {
                if ( !metadir.mkdirs() ) {
                    logger.warning("unable to make .meta directory");
                }
            }
            File metaFile= new File( metadir, filename );
            try (PrintStream metaOut = new PrintStream( new FileOutputStream(metaFile) )) {
                metaOut.println("ETag: "+etag+"\n");
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
