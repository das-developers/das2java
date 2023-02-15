
package org.das2.util.filesystem;

import com.itextpdf.text.io.StreamUtil;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.io.PushbackInputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
 * Note, GitHub is slow to update the raw view, so five minutes may pass
 * before a pushed change is visible.  See
 * https://sourceforge.net/p/autoplot/bugs/2203/ .  This can be resolved
 * by maintaining a local clone of the repo.
 * 
 * @author jbf
 */
public class GitHubFileSystem extends HttpFileSystem {

    private static final Logger logger= LoggerManager.getLogger("das2.filesystem.wfs.githubfs");
    
    private String branch= "master";
    
    // mission statement needed.  I believe this is the offset to the first folder/file name.
    private int baseOffset= 0;

    private boolean isNeedLoginPage(File partFile) throws IOException {
        int MAX_LINE_COUNT= 100;
        try ( PushbackInputStream is= new PushbackInputStream( new FileInputStream(partFile) ) ) {
            byte[] cc= new byte[60];
            int bytesRead= is.read(cc);
            int p=bytesRead;
            while ( p<15 && bytesRead>-1 ) {
                bytesRead= is.read(cc,p,15-p);
            }
            if ( bytesRead==-1 ) {
                return false;
            }
            boolean isLogin= false;
            if ( new String( cc, "US-ASCII" ).trim().startsWith("<!DOCTYPE html>") ) {
                try (BufferedReader r = new BufferedReader( new InputStreamReader( is ) )) {
                    String line= r.readLine();
                    int lineCount= 0;
                    while ( line!=null ) {
                        lineCount++;
                        if ( line.contains("Sign in") ) {
                            isLogin= true;
                            break;
                        }
                        if ( lineCount>MAX_LINE_COUNT ) {
                            break;
                        }
                        line= r.readLine();
                    }
                }
            }
            return isLogin;
        }
    }
    
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
        File f= this.getReadOnlyCache( );
        if ( f==null ) {
            File localRoCache= lookForROCacheGH(localRoot,branch);
            if ( localRoCache!=null ) {
                setReadOnlyCache( localRoCache );
            }
        }
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
     * return the location within the file cache of this GitHub filesystem.
     * TODO: There's something with branches that still needs work.  Also the constructor
     * does not use this.
     * @param root
     * @return the directory containing the resource.
     */
    public static File getLocalRoot( URI root ) {
        
        String suri= root.toString();
        Pattern fsp1= Pattern.compile( "(https?://[a-zA-Z0-9+.\\-]+/)(.*)(tree|blob|raw)/(.*?)/(.*)" );
        Matcher m1= fsp1.matcher( suri );
        if ( m1.matches() ) {
            String project= m1.group(2);
            if ( project.endsWith("/-/") ) { // gitlabs server U. Iowa server would add extra "-/"
                project= project.substring(0,project.length()-2);
            }
            suri= m1.group(1)+project+m1.group(4)+"/" + m1.group(5);
            try {
                root= new URI(suri);
            } catch (URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
        }
        
        File local;

        local = localRoot(root);
        logger.log(Level.FINER, "initializing httpfs {0} at {1}", new Object[]{root, local});

        return local;
    }
    
    /**
     * one place that lists the GitHub (GitLab) filesystems.
     * @param h the host
     * @param path path to the top of the GitLabs instance.
     * @return null if it is not a GitHub filesystem, or the initial path otherwise
     */
    public static String isGithubFileSystem( String h, String path ) {
        if ( h.equals("github.com") ) {
            return "";
        } else if ( h.equals("git.uiowa.edu") ) {
            return "";
        } else if ( h.equals("abbith.physics.uiowa.edu") ) {
            return "";
        } else if ( h.equals("research-git.uiowa.edu") ) {
            return "";
        } else if ( h.equals("git.physics.uiowa.edu" ) ) {
            return "";
        } else if ( h.equals("github.umn.edu" ) ) {
            return "";
        } else if ( h.equals("jfaden.net") && path.startsWith("/git") ) {
            return "/git";
        } else if ( h.equals("gitlab.com") ) {
            return "";
        } else {
            return null;
        }
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
        Pattern fsp1= Pattern.compile( "(https?://[a-zA-Z0-9+.\\-]+/)(.*)(tree|blob|raw)/(.*?)/(.*)" );
        Matcher m1= fsp1.matcher( suri );
        if ( m1.matches() ) {
            String project= m1.group(2);
            branch= m1.group(4);
            if ( project.endsWith("/-/") ) { // strange bug where U. Iowa GitLabs server would add extra "-/"
                project= project.substring(0,project.length()-2);
            }
            suri= m1.group(1) + project + branch + "/" + m1.group(5);
            try {
                root= new URI(suri);
            } catch (URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
        }
        
        // Note that getLocalRoot(root) contains a copy of this code.  Both the URI and the 
        // local root must be calculated, so we are unable to just call that routine.
        
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
    
    /**
     * return true if the URLs should contain an extra dash before the tree/master 
     * part, as in https://abbith.physics.uiowa.edu/jbf/juno/-/tree/master/team/ephemeris
     * 
     * @param host the host name, e.g. https://abbith.physics.uiowa.edu
     * @return 
     */
    private static boolean mysteryDash( String host ) {
        return host.contains("https://abbith.physics.uiowa.edu");
    }
    
    /**
     * return the root of the github project.
     * @return 
     */
    private String getGitProjectRoot( ) {
        String[] ss= root.toString().split("/");
        StringBuilder sb= new StringBuilder(ss[0]);
        for ( int i=1; i<this.baseOffset+5; i++ ) {
            sb.append("/").append(ss[i]);
        }
        return sb.toString();
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
        
        String[] path= root.getPath().split("/",-2);
        
        // spath is the directory within the server, pointing the the project directory.
        String spath= path[0] + '/' + path[1] + '/' + path[2] ;
        
        if ( baseOffset==1 ) {
            spath = spath + '/' + path[3];
        }
        
        InputStream urlStream= null ;
        try {
            URL url= gitHubMapDir( root, directory );
            String surl= url.toString();
            if ( mysteryDash(surl) ) {
                surl= surl.replace("raw/master", "-/tree/master");
                url= new URL(surl);
            } else {
                surl= surl.replace("raw/master", "tree/master");
                url= new URL(surl);
            }
            
            logger.log(Level.FINE, "translated list URL to {0}", url);
            
            urlStream = getInputStream(url);
            URL[] listing= HtmlUtil.getDirectoryListing( url, urlStream, false );
            
            //try (PrintWriter fw = new PrintWriter( new FileWriter(new File("/tmp/ap/listingGitHubFS.txt")) )) {
            //    for ( URL u: listing ) {
            //        fw.println(u);
            //    }
            //}
            //System.err.println("Wrote /tmp/ap/listingGitHubFS.txt") ;
            
            String sroot= root.toString();
            List<String> result= new ArrayList<>();
            int parentLen= sroot.length() + ( directory.length() -1 );
            
            String mysteryDash= mysteryDash(surl) ? "/-" : "";
            
            // the root of the project, for example "https://jfaden.net/git/jbfaden/public"
            String projectRoot = getGitProjectRoot();         
                    
            int ii= sroot.indexOf(spath) + spath.length();
            if ( sroot.substring(ii).startsWith("/" + branch+"/") ) {
                ii= ii+ branch.length() + 1;
            }
            String searchChild1= projectRoot + mysteryDash + "/tree/" + branch + sroot.substring(ii);
            String searchChild2= projectRoot + mysteryDash + "/blob/" + branch + sroot.substring(ii);
            //https://jfaden.net/git/jbfaden/public/tree/master/2021
            //int icount=0;
            for ( URL u: listing ) {
                String su= u.toString();
                //if ( su.contains("readme.md") ) {
                //    System.err.println("here for debugging");
                //}
                //if ( icount==76 ) {
                //    System.err.println("here for debugging");
                //}
                //icount++;
                // listing   "https://github.com/autoplot/dev/blob/master/bugs/sf/2507/empty.dat" // this is the problem
                // searchfor "https://github.com/autoplot/dev/master/blob/bugs/sf/2507/"          // this is the problem
                //"https://jfaden.net/git/jbfaden/public/blob/master/2023/20230215/afile.csv"
                //"https://jfaden.net/git/jbfaden/public/blob/master/2023/20230215/"
                if ( !su.startsWith(searchChild1) ) {
                    if ( !su.startsWith(searchChild2) ) {
                        continue;
                    }
                }
                if ( su.contains("/blob/"+branch+"/") // These are files
                        && !su.endsWith(".gitkeep") ) {
                    result.add( su.substring( searchChild1.length() ) );
                } else if ( su.contains("/tree/"+branch+"/") ) {
                    if ( su.length()>parentLen ) {
                        String ss= su.substring( searchChild1.length() );
                        if ( ss.length()>1 
                                && !ss.contains("#start-of-content") 
                                && !ss.contains("#content-body") 
                                && !su.contains("return_to=") 
                                && !su.endsWith("/..") ) {
                            result.add( ss + "/" );
                        }
                    }
                } else if ( su.startsWith(surl) ) {
                    String sub= su.substring(surl.length());
                    if ( sub.length()>0 && sub.charAt(0)!='#' && !sub.contains("/") ) {
                        if ( !result.contains( sub+"/" ) ) { // inefficient
                            result.add( sub+"/" );
                        }
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
     * With the GitHubFileSystem, we have to take into account that the 
     * FileSystem cache will have the branch name while the local copy
     * will not.  <em>Note that this assumes the rocache copy is on the same
     * branch.</em>
     * @param start
     * @param branch
     * @return the rocache location if available.
     */
    private File lookForROCacheGH( File start, String branch ) {
        // This is a copy of WebFileSystem.lookForROCache.  This differs where
        // we inspect tail to look for the branch name: tail.substring(1).startsWith(branch) 
        start= new File( start, branch );
        File _localRoot= start;
        File stopFile= FileSystem.settings().getLocalCacheDir();
        File result= null;

        if ( !_localRoot.toString().startsWith(stopFile.toString()) ) {
            throw new IllegalArgumentException("localRoot filename ("+stopFile+") must be parent of local root: "+start);
        }
        
        while ( !( _localRoot.equals(stopFile) ) ) {
            File f= new File( _localRoot, "ro_cache.txt" );
            if ( f.exists() ) {
                try ( BufferedReader read=new BufferedReader( new InputStreamReader( new FileInputStream(f), "UTF-8" ) ) ) {
                    String s = read.readLine();
                    while (s != null) {
                        int i= s.indexOf("#");
                        if ( i>-1 ) s= s.substring(0,i);
                        if ( s.trim().length()>0 ) {
                            if ( s.startsWith("http:") || s.startsWith("https:") || s.startsWith("ftp:") ) {
                                throw new IllegalArgumentException("ro_cache should contain the name of a local folder");
                            }
                            String sf= s.trim();
                            result= new File(sf);
                            break;
                        }
                        s = read.readLine();
                    }
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, ex.getMessage(), ex);
                }
                break;
            } else {
                _localRoot= _localRoot.getParentFile();
            }
        }
        if ( result==null ) {
            return result;
        } else {
            String tail= start.getAbsolutePath().substring(_localRoot.getAbsolutePath().length());
            if ( tail.length()>0 ) {
                if ( tail.substring(1).startsWith(branch) ) { // 1 is for the initial slash
                    tail= tail.substring( 1+branch.length() );
                }
                return new File( result, tail );
            } else {
                return result;
            }
        }
    }
            
    /**
     * Given the URI, convert this to the link which will download the file.
     * github puts directories for each project under "raw/master".
     * @param root
     * @param filename
     * @return
     * Translate:<pre>%{code
     * https://abbith.physics.uiowa.edu/jbf/myawesomepublicproject/blob/24dff04b9bcb275d8bfd85b38e0e8b039b21d655/sayAwesome.jy to <br>
     * https://abbith.physics.uiowa.edu/jbf/myawesomepublicproject/raw/24dff04b9bcb275d8bfd85b38e0e8b039b21d655/sayAwesome.jy
     * https://github.com/autoplot/app/raw/master/Autoplot/src/resources/badge_ok.png to
     * https://github.com/autoplot/app/master/Autoplot/src/resources/badge_ok.png
     * https://jfaden.net/git/jbfaden/public/blob/master/u/jeremy/2019/20191023/updates.jy
     *
     * https://research-git.uiowa.edu/space-physics/juno/ap-script/master/test/testap.jy to
     * https://research-git.uiowa.edu/space-physics/juno/ap-script/-/raw/master/test/testap.jy 
     *
     * https://research-git.uiowa.edu/jbf/testproject/-/blob/master/script/testScript.jy to
     * https://research-git.uiowa.edu/jbf/testproject/master/script/testScript.jy
     * }
     * </pre>
     * @throws MalformedURLException 
     */
    public URL gitHubMapFile( URI root, String filename ) throws MalformedURLException {
        filename= toCanonicalFilename( filename );       
        // png image "https://github.com/autoplot/app/raw/master/Autoplot/src/resources/badge_ok.png"
        String[] path= root.getPath().split("/",-2);
        String spath= path[0] + '/' + path[1] + '/' + path[2] ;
        
        // number of elements after the host to the base.
        int gitPathElements;
        
        int base;
        if ( path[3+baseOffset].equals(branch) ) {
            base= 4;
            gitPathElements= 3;
        } else if ( path.length>4+baseOffset && path[4+baseOffset].equals(branch) ) { // https://research-git.uiowa.edu/space-physics/juno/ap-script/-/
            base= 5;
            gitPathElements= 4;
            spath= path[0] + '/' + path[1] + '/' + path[2] + '/' + path[3];
        } else {
            base= 3;
            gitPathElements= 3;
        }

        for ( int i=0; i<baseOffset; i++ ) {
            spath= spath + "/" + path[i+gitPathElements];
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
                if ( base==5 ) {
                    String n= root.getScheme() + "://" + root.getHost() + '/' + spath + "/raw/"+branch+"/" + strjoin( path, "/", base+baseOffset, -1 ) + filename;
                    URL url= new URL( n );
                    return url;
                } else {
                    String pp= strjoin( path, "/", base+baseOffset, -1 );
                    if ( pp.length()>0 ) pp= "/" + pp; 
                    String n= root.getScheme() + "://" + root.getHost() + '/' + spath + "/raw/" + branch + pp + filename;
                    URL url= new URL( n );
                    return url;
                }
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
        
        URL ff= gitHubMapFile( root, filename+"/readme.md" );
        String s= ff.toString();
        s= s.substring(0,s.length()-10);
        if ( s.endsWith("raw/"+branch+"/") ) {
            int len= ("raw/"+branch+"/").length();
            return new URL( s.substring(0,s.length()-len) );
        } else {
            return new URL( s );
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
    protected Map<String,String> downloadFile(String filename, File targetFile, File partFile, ProgressMonitor monitor) throws IOException {
        
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
            
            int expectedContentLength= urlc.getContentLength();
            monitor.setTaskSize( expectedContentLength );
            out= new FileOutputStream( partFile );
            loggerUrl.log(Level.FINE, "GET {0}", new Object[] { urlc.getURL() } );
            is = urlc.getInputStream(); // To download
            monitor.started();
            long totalBytesRead= copyStream(is, out, monitor );
            if ( totalBytesRead<urlc.getContentLength() ) {
                logger.log(Level.WARNING, "fewer bytes downloaded than expected: {0} of {1}", new Object[]{totalBytesRead, expectedContentLength});
                throw new IOException("fewer bytes in HTTP response than stated in header.");
            }
            monitor.finished();
            out.close();
            is.close();
            //TODO: there's a problem where if you aren't logged in to a private project, you get a 200 response with HTML.  Detect this!
            if ( isNeedLoginPage( partFile ) ) {
                throw new IOException("GitHub/GitLabs which requires authentication is not supported");
            }
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
    public FileObject getFileObject(String filename) {
        return new GitHubFileObject( this, filename, new Date( Long.MAX_VALUE ) );
    }
    
    
    @Override
    public String toString() {
        return "githubfs " + root;
    }
    
}
