
package org.das2.util.filesystem;

import com.itextpdf.text.io.StreamUtil;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.das2.util.LoggerManager;
import static org.das2.util.filesystem.FileSystem.loggerUrl;
import static org.das2.util.filesystem.FileSystem.toCanonicalFilename;
import static org.das2.util.filesystem.HtmlUtil.getInputStream;
import static org.das2.util.filesystem.WebFileSystem.localRoot;
import org.das2.util.monitor.CancelledOperationException;
import org.das2.util.monitor.ProgressMonitor;

/**
 * GitHubFileSystem allows GitHub directories to be mounted directly, even 
 * though it is not a conventional filesystem with files residing in folders.  
 * For example, the file resource README.md found in 
 * https://github.com/autoplot/scripts/ is downloaded from 
 * https://github.com/autoplot/scripts/blob/master/README.md,
 * with "blob/master/" added to the URL.  Likewise directory "demos" is found
 * under "tree/master/".
 * 
 * GitHub also introduced a new problem, where dates cannot be used for 
 * evaluating file freshness.  ETags are now supported in WebFileSystem to 
 * provide this functionality.
 * 
 * Note, there's a strange interaction with Java and GitHub.com, where Java's
 * caching prevents updates from appearing automatically.  See
 * https://sourceforge.net/p/autoplot/bugs/2203/ .
 * 
 * @author jbf
 */
public class GitHubFileSystem extends HttpFileSystem {

    private static final Logger logger= LoggerManager.getLogger("das2.filesystem.wfs.githubfs");
    
    private String branch= "master";
    
    // mission statement needed.  I believe this is the offset to the first folder/file name.
    private int baseOffset= 0;
    
    private class GitHubHttpProtocol implements WebProtocol {

        @Override
        public InputStream getInputStream(WebFileObject fo, ProgressMonitor mon) throws IOException {
            URL gitHubURL= gitHubMapFile( root, fo.getNameExt() );
            logger.log(Level.FINE, "get InputStream from {0}", gitHubURL);
            try {
                InputStream result= HtmlUtil.getInputStream(gitHubURL); // handles redirects.
                if ( gitHubURL.toString().endsWith(".vap") ) {
                    byte[] bb= StreamUtil.inputStreamToArray(result);
                    logger.log(Level.FINE, "downloaded {0} got {1} bytes.", new Object[]{result, bb.length});
                    return new ByteArrayInputStream(bb);
                } else {
                    return result;
                }
            } catch ( CancelledOperationException ex ) {
                throw new InterruptedIOException(ex.getMessage());
            }
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
     * Create a new GitHubFileSystem mirroring the root, a URL pointing to 
     * "http" or "https", in the local folder.  The baseOffset is 0 for 
     * https://github.com/autoplot/dev, because github.com is the GitLabs 
     * instance.  The baseOffset is 1 for https://jfaden.net/git.
     * @param root the root of the filesystem
     * @param localRoot the local root where files are downloaded.
     * @param branch the branch, typically "master".
     * @param baseOffset index of the first folder of the GitLabs server.
     */
    protected GitHubFileSystem(URI root, File localRoot, String branch, int baseOffset) {
        super(root, localRoot);
        this.baseOffset= baseOffset;
        this.branch= branch;
        this.protocol= new GitHubHttpProtocol();
    }
    
    /**
     * create GitLabs instance
     * @param root the root
     * @return the filesystem.
     */
    public static GitHubFileSystem createGitHubFileSystem( URI root ) {
        return createGitHubFileSystem(root,0);
    }
    
    /**
     * @param root the root
     * @param baseOffset the number of folders after the host in the root, for
     * this GitLabs instance.
     * @return the filesystem.
     */
    public static GitHubFileSystem createGitHubFileSystem( URI root, int baseOffset ) {
        File local;
        
        /**
         * code this as if well support branches, but this won't be done until 
         * after the next production release.
         */
        String branch= "master";
        
        String suri= root.toString();
        Pattern fsp1= Pattern.compile( "(https?://[a-z.]*/)(.*)(tree|blob|raw)/(.*?)/(.*)" );
        Matcher m1= fsp1.matcher( suri );
        if ( m1.matches() ) {
            String project= m1.group(2);
            if ( project.endsWith("/-/") ) { // strange bug where U. Iowa server would add extra "-/"
                project= project.substring(0,project.length()-2);
            }
            suri= m1.group(1)+project+m1.group(4)+"/" + m1.group(5);
            try {
                root= new URI(suri);
            } catch (URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
            branch= m1.group(4);
        }
        
        //if ( !branch.equals("master") ) {
        //    throw new IllegalArgumentException("branch must be master (for now)");
        //}
        
        if (FileSystemSettings.hasAllPermission()) {
            local = localRoot(root);
            logger.log(Level.FINER, "initializing httpfs {0} at {1}", new Object[]{root, local});
        } else {
            local = null;
            logger.log(Level.FINER, "initializing httpfs {0} in applet mode", root);
        }

        return new GitHubFileSystem(root, local, branch, baseOffset );
        
    }
    
    @Override
    public String[] listDirectory(String directory) throws IOException {
        if ( !directory.endsWith("/") ) directory= directory+"/";
        if ( directory.equals("/") && root.getRawPath().equals("/") ) { // list from cache.
            File dir= new File( FileSystem.settings().getLocalCacheDir() + "/" + root.getScheme() + "/" + root.getHost() );
            String[] ss= dir.list();
            if ( ss==null ) throw new IllegalArgumentException("dir was not a directory");
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
                if ( su.contains("/blob/"+branch+"/")
                        && !su.endsWith(".gitkeep") ) {
                    result.add( su.substring( parentLen ) );
                } else if ( su.contains("/tree/"+branch+"/") ) {
                    if ( su.length()>parentLen ) {
                        String ss= u.toString().substring( parentLen ) + "/" ;
                        if ( ss.length()>1 
                                && !ss.contains("#start-of-content") 
                                && !ss.contains("#content-body") 
                                && !su.contains("return_to=") 
                                && !su.endsWith("/..") ) {
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
                if ( urlStream!=null ) urlStream.close();
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
     * Translate:<pre>%{code
     * https://abbith.physics.uiowa.edu/jbf/myawesomepublicproject/blob/24dff04b9bcb275d8bfd85b38e0e8b039b21d655/sayAwesome.jy to <br>
     * https://abbith.physics.uiowa.edu/jbf/myawesomepublicproject/raw/24dff04b9bcb275d8bfd85b38e0e8b039b21d655/sayAwesome.jy
     * https://github.com/autoplot/app/raw/master/Autoplot/src/resources/badge_ok.png
     * https://github.com/autoplot/app/raw/master/Autoplot/src/resources/badge_ok.png
     * https://jfaden.net/git/jbfaden/public/blob/master/u/jeremy/2019/20191023/updates.jy
     * }
     * </pre>
     * @throws MalformedURLException 
     */
    public URL gitHubMapFile( URI root, String filename ) throws MalformedURLException {
        filename= toCanonicalFilename( filename );       
        // png image "https://github.com/autoplot/app/raw/master/Autoplot/src/resources/badge_ok.png"
        String[] path= root.getPath().split("/",-2);
        String spath= path[0] + '/' + path[1] + '/' + path[2] ;
        
        for ( int i=0; i<baseOffset; i++ ) {
            spath= spath + "/" + path[i+3];
        }
        
        int base;
        if ( path[3+baseOffset].equals(branch) ) {
            base= 4;
        } else {
            base= 3;
        }
        
        if ( spath.startsWith("/") ) spath=spath.substring(1);
        
        if ( path[ base+baseOffset].equals("blob") ) {
            String n= root.getScheme() + "://" + root.getHost() + '/' + spath + "/raw/" + strjoin( path, "/", base + 1 + baseOffset, -1 ) + filename;
            URL url= new URL( n );
            return url;
        } else {
            if ( root.getHost().equals("github.com") && filename.endsWith(".vap" ) ) { // This is an experiment
                String n= root.getScheme() + "://raw.githubusercontent.com" + '/' + spath + "/"+branch+"/" + strjoin( path, "/", base+baseOffset, -1 ) + filename;
                if ( n.indexOf("//",8)>-1 ) {
                    n= n.substring(0,8) + n.substring(8).replaceAll("//", "/");
                }
                URL url= new URL( n );
                return url;                
            } else {
                String n= root.getScheme() + "://" + root.getHost() + '/' + spath + "/raw/"+branch+"/" + strjoin( path, "/", base+baseOffset, -1 ) + filename;
                URL url= new URL( n );
                return url;
            }
        }
    }

    /**
     * github puts directories for each project under "tree/master".
     * @param root
     * @param filename
     * @return
     * @throws MalformedURLException 
     */
    public URL gitHubMapDir( URI root, String filename ) throws MalformedURLException {
        filename= toCanonicalFilename( filename );            
        // png image "https://github.com/autoplot/app/raw/master/Autoplot/src/resources/badge_ok.png"
        String[] path= root.getPath().split("/",-2);
        String spath= path[0] + '/' + path[1] + '/' + path[2] ;
        for ( int i=3; i<3+baseOffset; i++ ) {
            spath+= "/" + path[i];
        }
        String n;
        if ( path.length==(3+baseOffset) && filename.length()==1 ) {
            return root.toURL();
        } else {
            n= root.getScheme() + "://" + root.getHost() + '/' + spath + "/tree/"+branch+"/" + strjoin( path, "/", 3 + baseOffset, -1 ) + filename;
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
    protected synchronized Map<String,String> downloadFile(String filename, File targetFile, File partFile, ProgressMonitor monitor) throws IOException {
        
        Lock lock = getDownloadLock(filename, targetFile, monitor);
        
        if (lock == null) {
            return Collections.EMPTY_MAP;
        }
        
        logger.log(Level.WARNING, "Thread {0} downloading {1}", new Object[]{Thread.currentThread(), filename});
        
        logger.log(Level.FINE, "downloadFile({0})", filename);
        Map<String,String> result;
        
        FileOutputStream out=null;
        InputStream is= null;
        try {
            filename= toCanonicalFilename( filename );
            URL url= gitHubMapFile( root, filename );
            logger.log(Level.FINE, "downloading {0}", url);
            URLConnection urlc = url.openConnection();
            result= reduceMeta(urlc);
            
            int i= urlc.getContentLength();
            monitor.setTaskSize( i );
            out= new FileOutputStream( partFile );
            loggerUrl.log(Level.FINE, "GET {0}", new Object[] { urlc.getURL() } );
            is = urlc.getInputStream(); // To download
            monitor.started();
            copyStream(is, out, monitor );
            monitor.finished();
            out.close();
            is.close();
            //TODO: there's a problem where if you aren't logged in to a private project, you get a 200 response with HTML.  Detect this!
            if ( targetFile.exists() ) {
                if ( !targetFile.delete() ) {
                    throw new IllegalArgumentException("unable to delete existing file "+targetFile );
                }
            }
            if ( ! partFile.renameTo( targetFile ) ) {
                if ( ! partFile.renameTo( targetFile ) ) {
                    throw new IllegalArgumentException("unable to rename "+partFile+" to "+targetFile );
                }
                throw new IllegalArgumentException("unable to rename "+partFile+" to "+targetFile );
            }
            
        } catch ( IOException e ) {
            if ( out!=null ) out.close();
            if ( is!=null ) is.close();
            if ( partFile.exists() &&  ! partFile.delete() ) {
                throw new IllegalArgumentException("unable to delete "+partFile );
            }
            throw e;
        } finally {
            lock.unlock();
        }
        
        return result;
    }
    
    @Override
    public String toString() {
        return "githubfs " + root;
    }
    
}
