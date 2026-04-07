
package org.das2.util.filesystem;

import com.itextpdf.text.io.StreamUtil;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PushbackInputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.das2.util.monitor.NullProgressMonitor;
import org.das2.util.monitor.ProgressMonitor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    
    private String branch= "";
    
    /**
     * the project name, which should not begin or end with /!
     */
    private String project="";
    
    /**
     * GitLabs allows tokens, which allow access to restricted projects.
     */
    private String token="";
    
    /**
     * directory within the Forge.  This should not begin or end with /!
     */
    private String directory="";
    
    private final Forge forge; 
    
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

    @Override
    protected Map<String, String> getRequestProperties() {
        if ( token.length()==0 ) {
            return Collections.emptyMap();
        } else {
            switch (forge) {
                case GITLAB:
                    return Collections.singletonMap( "PRIVATE-TOKEN", token );
                case GITHUB:
                    return Collections.singletonMap( "Authorization", "token "+token );
                default:
                    throw new UnsupportedOperationException("not implemented");
            }
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
                Map<String,String> requestProperties= new HashMap<>();
                
                Map<String,String> rp= getRequestProperties();
                requestProperties.putAll(rp);

                return HttpUtil.getMetadata( ur, requestProperties );
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
     * @param branch the branch, typically "master", and "" if the default branch is not yet known.
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
        this.forge= detectForge(root);
        this.baseOffset= baseOffset;
        this.branch= branch;
        if ( branch.length()>0 ) {
            String sroot= root.toString();
            int i= sroot.indexOf("/"+branch+"/");
            int i0= sroot.indexOf("://");
            i0= sroot.indexOf("/",i0+3);
            this.project= sroot.substring(i0+1,i);
            this.directory= sroot.substring(i+branch.length()+2);
        } else {
            this.project= ""; // empty means not known.
            this.directory="";
        }
        if ( this.project.length()==0 ) {
            int islash= root.getPath().indexOf("/-/"); 
            if ( islash>-1 ) { // new, leave this in 
                String[] ss= root.getPath().substring(0,islash).split("/");
                this.project= String.join("/", Arrays.copyOfRange( ss, 1+baseOffset, ss.length ) );
            }
        }
        this.protocol= new GitHubHttpProtocol();
        
        try {
            listDirectory( "/", new NullProgressMonitor() );
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        
    }
    
    /**
     * create GitLabs instance
     * @param root the root
     * @return the filesystem.
     */
    public static GitHubFileSystem createGitHubFileSystem( URI root ) {
        return createGitHubFileSystem(root,0);
    }
    
    public static enum Forge { GITHUB, GITLAB }
    
    /**
     * detect if the URI is a GitHub or GitLab instance.  This presently looks for github.com or gitlab.umn.edu.
     * @param root
     * @return Forge value.
     */
    public static Forge detectForge( URI root ) {
        String host= root.getHost();
        switch ( host ) {
            case "github.com": 
            case "github.umn.edu":
                return Forge.GITHUB;
            default:
                return Forge.GITLAB;
        }
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
            return "/git"; // Note this git no longer exists, but the code is left here to show where forge can be in directory.
        } else if ( h.equals("git.jfaden.net") ) {
            return "";
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
         * This is the branch we are using.  For URIs with "/tree/" or "/blob/" or "/raw/", the branch can be
         * identified.  Otherwise we will set it to "" and wait for the first read to determine the 
         * default branch.  For GitLab, this may be "master" because we don't use the API.  For GitLab, use
         * an API call to determine the default branch.
         */
        String branch= "";
        
        String suri= root.toString();
        Pattern fsp1= Pattern.compile( "(https?://[a-zA-Z0-9+.\\-]+/)(.*)(tree|blob|raw)/(.*?)/(.*)" );
        Matcher m1= fsp1.matcher( suri );
        if ( m1.matches() ) {
            String project= m1.group(2);
            branch= m1.group(4);
            if ( project.endsWith("/-/") ) { // strange bug where U. Iowa GitLabs server would add extra "-/"
                project= project.substring(0,project.length()-2);
            }
            suri= m1.group(1) + project + branch + "/" + m1.group(5);  // TODO: add slash to make it easier to manage
            try {
                root= new URI(suri);
            } catch (URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
        }
        
        // Note that getLocalRoot(root) contains a copy of this code.  Both the URI and the 
        // local root must be calculated, so we are unable to just call that routine.
        
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
        int ni= Math.min( this.baseOffset+5, ss.length );
        for ( int i=1; i<ni; i++ ) {
            sb.append("/").append(ss[i]);
        }
        return sb.toString();
    }
    
    /**
     * At some point, Gitlab started returning the filename listing in a 
     * separate JSON response.  TODO: It's impossible to distinguish
     * files from folders.
     * @param directory
     * @return
     * @throws IOException 
     */
    public String[] listDirectoryGitLabHowever( String directory ) throws IOException {
        String[] path= root.getPath().split("/",-2);
        // wget -O - 'https://abbith.physics.uiowa.edu/jbf/juno/-/refs/master/logs_tree/team/trajPlot?format=json&offset=0' | json_pp
        // wget -O - 'https://research-git.uiowa.edu/abbith/juno/-/refs/main/logs_tree/team?format=json&offset=0' | json_pp
        // wget -O - 'https://research-git.uiowa.edu/api/v4/projects/abbith%2Fjuno/repository/tree?path=team/wigglePlot&ref=main'| json_pp
        // wget -O - 'https://research-git.uiowa.edu/api/v4/projects/abbith%2Fjuno/repository/tree?path=main/team/wigglePlot/&ref=main'
        if ( root.getHost().equals("research-git.uiowa.edu") || root.getHost().equals("git.jfaden.net") ) {
            String[] maybeListing= listDirectoryGitLab(directory);
            if ( maybeListing!=null ) {
                return maybeListing;
            }
        }
        
        if ( project.length()==0 ) {
            project= path[1] + path[2];
        }
        StringBuilder sb= new StringBuilder();
        sb.append(root.getScheme())
                .append("://")
                .append(root.getHost())
                .append('/')
                .append(project)
                .append("/-/refs/")
                .append(branch)
                .append("/logs_tree");
        if ( path.length>3 && path[3].equals(branch) ) {
            for ( int i=4; i<path.length-1; i++ ) {
                sb.append( "/" );
                sb.append( path[i] );
            }
        } else {
            for ( int i=1; i<path.length-1; i++ ) {
                sb.append( "/" );
                sb.append( path[i] );
            }                
        }
        sb.append("?format=json&offset=0");

        URL url= new URL(sb.toString());
        try {
            
            Map<String,String> requestProperties= new HashMap<>();
            requestProperties.putAll( getRequestProperties() );

            String s= HtmlUtil.readToString( url, requestProperties );
           
            JSONArray ja= new JSONArray(s);
           
            String[] result= new String[ja.length()];
            for ( int i=0; i<result.length; i++ ) {
                result[i]= ja.getJSONObject(i).getString("file_name");
            }
            return result;
            
        } catch (JSONException ex) {
            logger.log(Level.SEVERE, "JSON not returned from Gitlab server, try again soon: {0}", url);
            return new String[0];
        } catch (CancelledOperationException ex) {
            throw new IOException("cancel pressed");
        }
    }
 
    /**
     * Use GitHub's API to list the directory.
     * @param directory within the filesystem
     * @return the list of files, with / suffix for directories.
     * @throws IOException 
     */
    public String[] listDirectoryGithub(String directory) throws IOException {
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
        if ( !root.toString().startsWith("https://github.com/") ) {
            throw new IllegalArgumentException("listDirectoryGithub can't be used here");
        }
        
        if ( branch.length()==0 ) {
            branch= getDefaultBranch(root, project);
        }
        
        String[] path= root.getPath().split("/",-2);

        if ( project.length()==0 ) {
            project= path[1] + '/' + path[2];
        }
        
        
        if ( path.length<4) {
            return null;
        }
        
        if ( path[3].equals(branch) ) {
            String[] npath= new String[path.length-1];
            System.arraycopy( path, 0, npath, 0, 3 );
            System.arraycopy( path, 4, npath, 3, path.length-4 );
            path= npath;
        }
        
        String[] pathsub= Arrays.copyOfRange( path, 3, path.length );
                
        URL url= new URL("https://api.github.com/repos/" + this.project + "/contents/" + String.join( "/", pathsub) + "?ref=" + branch );

        String[] result;
        
        try {

            String jsonListing= HtmlUtil.readToString(url);
        
            JSONArray jo= new JSONArray(jsonListing);
            
            result= new String[jo.length()];
            
            for ( int i=0; i<result.length; i++ ) {
                JSONObject item= jo.getJSONObject(i);
                String surl= item.getString("path");
                int k= surl.lastIndexOf("/");
                String type= item.getString("type");
                if ( type.equals("dir") ) {
                    result[i]= surl.substring(k+1)+"/";
                } else {
                    result[i]= surl.substring(k+1);
                }
            }
            
            return result;
            
        } catch (JSONException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (CancelledOperationException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        
        return null;
        
    }

    /**
     * Use GitLab's API to list the directory.
     * For example, https://research-git.uiowa.edu/space-physics/rbsp/ap-script/-/tree/master/u/ivar/20210416/
     * should list the files in this directory, using calls like 
     * https://research-git.uiowa.edu/api/v4/projects/space-physics%2Frbsp%2Fap-script/repository/tree?path=u/ivar/20210416/&ref=master
     * which will return a JSON listing.
     * @param directory within the filesystem
     * @return the list of files, with / suffix for directories.
     * @throws IOException 
     */
    public String[] listDirectoryGitLab(String directory) throws IOException {
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
        
                
        String[] pathComponents= root.getPath().split("/",-2);
        
        if ( pathComponents.length<4 ) { // just use the old method
            return null;
        }
        
        String project,path;
        
        int idash=-1;
        for ( int i=0; i<pathComponents.length; i++ ) {
            if ( pathComponents[i].equals("-") ) {
                idash=i;
                break;
            }
        }
        
        if ( idash>-1 ) {
            project= String.join( "/", Arrays.copyOfRange( pathComponents, 1, idash ) );
            path= String.join( "/", Arrays.copyOfRange( pathComponents, idash+1, pathComponents.length ) );
        } else {
            // Note ChatGPT says the - in https://research-git.uiowa.edu/space-physics/rbsp/ap-script/-/tree/master/u/ivar/20210416/
            // is a delimiter, so we could probably clean up this logic below.
            if ( pathComponents[1].equals("space-physics") ) {
                project= String.join( "/", Arrays.copyOfRange( pathComponents, 1, 4 ) ); // space-physics/rbsp/ap-script
                path= String.join( "/", Arrays.copyOfRange( pathComponents, 4, pathComponents.length ) ); // team/digitizing
            } else {
                project= String.join( "/", Arrays.copyOfRange( pathComponents, 1, 3 ) ); // abbith/juno
                path= String.join( "/", Arrays.copyOfRange( pathComponents, 3, pathComponents.length ) ); // team/digitizing
            }
        }
        
        if ( this.project.length()==0 ) {
            this.project= project;
        }
        
        if ( branch.length()==0 ) {
            branch= getDefaultBranchGitLab(root,project);
        }
        
        if ( path.startsWith(branch+'/') ) {
            path= path.substring(branch.length()+1);
        }
                
        URL url= new URL( root.getScheme() + "://" + root.getHost() + "/api/v4/projects/" + project.replace("/","%2F")+ "/repository/tree?path=" + path.replace("/","%2F") + "&ref=" + branch );
        
        String[] result;
        
        try {

            Map<String,String> requestProperties= new HashMap<>();
            requestProperties.putAll( getRequestProperties() );

            String jsonListing;
            try {
                jsonListing= HtmlUtil.readToString(url,requestProperties);
            } catch ( IOException ex ) {
                URL tokenURL= new URL( root.getScheme() + "://" + root.getHost() + "/-/" + project + "/");
                String s= KeyChain.getDefault().getToken( tokenURL );
                if ( s!=null ) {
                    token= s;
                    requestProperties.putAll( getRequestProperties() );
                    jsonListing= HtmlUtil.readToString(url,requestProperties);
                    KeyChain.getDefault().storeToken( tokenURL, token );
                    
                    
                } else {
                    throw ex;
                }
            }
                        
            JSONArray jo= new JSONArray(jsonListing);
            
            result= new String[jo.length()];
            
            for ( int i=0; i<result.length; i++ ) {
                JSONObject item= jo.getJSONObject(i);
                String surl= item.getString("path");
                int k= surl.lastIndexOf("/");
                String type= item.getString("type");
                if ( type.equals("tree") ) {
                    result[i]= surl.substring(k+1)+"/";
                } else {
                    result[i]= surl.substring(k+1);
                }
            }
            
            return result;
            
        } catch (JSONException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (CancelledOperationException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        
        return null;
        
    }

    /**
     * Return the default branch (e.g. main or master) for the project.
     * @param root the GitLab root (e.g. https://research-git.uiowa.edu/)
     * @param project the project (e.g. 'space-physics/rbsp/ap-script')
     * @return the default branch (e.g. 'master')
     * @throws IOException 
     */
    public static String getDefaultBranchGitLab(URI root, String project) throws IOException {
        String branch;
        if ( project.length()==0 ) {
            project= root.getPath();
        }
        if ( project.endsWith("/") ) {
            project= project.substring(0,project.length()-1);
        }
        if ( project.startsWith("/") ) {
            project= project.substring(1);
        }
        String api_url = root.getScheme() + "://" + root.getHost() + "/api/v4/projects/" + URLEncoder.encode(project,"US-ASCII");
        try {
            String text= HtmlUtil.readToString( new URL(api_url) );
            JSONObject obj = new JSONObject(text);
            String default_branch = obj.getString("default_branch");
            branch= default_branch;
        } catch (CancelledOperationException | JSONException ex) {
            throw new RuntimeException(ex);
        }
        return branch;
    }
    
    /**
     * Return the default branch (e.g. main or master) for the project.
     * @param root the GitHub root (e.g. https://github.com/autoplot/)
     * @param project the project (e.g. 'autoplot/dev')
     * @return the default branch (e.g. 'master')
     * @throws IOException 
     */
    public static String getDefaultBranchGitHub(URI root, String project) throws IOException {
        String branch;

        if ( project.length()==0 ) {
            project= root.getPath();
            String[] ss= project.split("/");
            if ( ss.length<3 ) {
                throw new IllegalArgumentException("note a project:" + project);
            }
            project= ss[1]+"/"+ss[2];
        }
        String api_url = root.getScheme() + "://api." + root.getHost() + "/repos/" + project;
        if ( api_url.endsWith("/") ) {
            api_url= api_url.substring(0,api_url.length()-1);
        }
        try {
            String text= HtmlUtil.readToString( new URL(api_url) );
            JSONObject obj = new JSONObject(text);
            String default_branch = obj.getString("default_branch");
            branch= default_branch;
        } catch (CancelledOperationException | JSONException ex) {
            throw new RuntimeException(ex);
        }
        return branch;
    }
    
    /**
     * return the default branch (e.g. master or main) for the repo
     * @param root
     * @param project
     * @return
     * @throws IOException 
     */
    public static String getDefaultBranch(URI root, String project) throws IOException {
        Forge forge= detectForge(root);
        switch (forge) {
            case GITHUB:
                return getDefaultBranchGitHub(root, project);
            case GITLAB:
                return getDefaultBranchGitLab(root, project);
            default:
                throw new IllegalArgumentException("unsupported forge");
        }
    }
    
    private String[] listDirectoryProjectsGitLab( URI root, String projectSlash ) {
        String api_url = root.getScheme() + "://" + root.getHost() + "/api/v4/projects/";
        try {
            String text= HtmlUtil.readToString( new URL(api_url) );
            JSONArray arr= new JSONArray(text);
            List<String> result= new ArrayList<>(arr.length());
            for ( int i=0; i<arr.length(); i++ ) {
                JSONObject o = arr.getJSONObject(i);
                String name= o.getString("path_with_namespace");
                if ( name.startsWith(projectSlash) ) {
                    result.add(name.substring(projectSlash.length())+"/-/");
                }
            }
            return result.toArray(new String[0]);
        } catch (CancelledOperationException | JSONException | MalformedURLException ex) {
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private String[] listDirectoryProjectsGitHub( URI root, String projectSlash ) {
        String api_url = root.getScheme() + "://api." + root.getHost() + "/users/"+projectSlash+"repos?per_page=100&page=1";
        try {
            String text= HtmlUtil.readToString( new URL(api_url) );
            JSONArray arr= new JSONArray(text);
            List<String> result= new ArrayList<>(arr.length());
            for ( int i=0; i<arr.length(); i++ ) {
                JSONObject o = arr.getJSONObject(i);
                String name= o.getString("full_name");
                if ( name.startsWith(projectSlash) ) {
                    result.add(name.substring(projectSlash.length())+"/");
                }
            }
            return result.toArray(new String[0]);
        } catch (CancelledOperationException | JSONException | MalformedURLException ex) {
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }    
    
    private String[] listDirectoryProjects(URI root, String project) {
        if ( project.length()==0 ) {
            project= root.getPath();
        }
        if ( project.startsWith("/") ) {
            project= project.substring(1);
        }
        String projectSlash;
        if ( project.endsWith("/") ) {
            projectSlash= project;
        } else {
            int i= project.lastIndexOf("/");
            if ( i==-1 ) {
                projectSlash="/";
            } else {
                projectSlash=project.substring(0,i+1);
            }
        }
        
        switch (forge) {
            case GITLAB:
                return listDirectoryProjectsGitLab(root, projectSlash);
            case GITHUB:
                return listDirectoryProjectsGitHub(root, projectSlash);
            default:
                throw new IllegalStateException("unsupported forge: "+forge);
        }
        
    }
    
    @Override
    public String[] listDirectory(String directory) throws IOException {
        
        // Note Github's API looks easy to use, try:
        // curl https://api.github.com/repos/autoplot/dev/contents/bugs/2025
        // https://github.com/das-developers/das2java/issues/135
        
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
        
        try {
            if ( branch.length()==0 ) {
                branch= getDefaultBranch(root, project);
            }
        } catch ( FileNotFoundException ex ) {
            
        } catch ( IOException | IllegalArgumentException ex ) {
            if ( forge==Forge.GITLAB || forge==Forge.GITHUB ) {
                return listDirectoryProjects(root,project);
            }
        }
        
        if ( forge==Forge.GITLAB ) {
            try {
                return listDirectoryGitLabHowever( directory );
            } catch ( IOException ex ) {
                ex.printStackTrace();
                return new String[0];
            }
        }
        
        String[] path= root.getPath().split("/",-2);
        
        if ( forge==Forge.GITHUB ) {
            String[] resultGithubMaybe= listDirectoryGithub(directory);
            if ( resultGithubMaybe!=null ) {
                return resultGithubMaybe;
            }
        }
        
        // spath is the directory within the server, pointing the the project directory.
        String spath= path[0] + '/' + path[1] + '/' + path[2] ;
        
        if ( baseOffset==1 ) {
            spath = spath + '/' + path[3];
        }
        
        InputStream urlStream= null ;
        try {
            URL url;
            String surl;
            if ( path.length>3 ) {
                url= gitHubMapDir( root, directory );
                surl= url.toString();
                if ( mysteryDash(surl) ) {
                    surl= surl.replace("raw/master", "-/tree/master");
                    url= new URL(surl);
                } else {
                    surl= surl.replace("raw/master", "tree/master");
                    url= new URL(surl);
                }

                logger.log(Level.FINE, "translated list URL to {0}", url);
            } else {
                url= root.toURL();
                surl= url.toString();
            }
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
                    
            if ( branch.length()==0 ) {
                branch= getDefaultBranch(root, projectRoot);
            }

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
                    String k = su.substring( searchChild1.length() ) ;
                    if ( !result.contains(k) ) result.add(k);
                } else if ( su.contains("/tree/"+branch+"/") ) {
                    if ( su.length()>parentLen ) {
                        String ss= su.substring( searchChild1.length() );
                        if ( ss.length()>1 
                                && !ss.contains("#start-of-content") 
                                && !ss.contains("#content-body") 
                                && !su.contains("return_to=") 
                                && !su.endsWith("/..") ) {
                            ss= ss+"/";
                            if ( !result.contains(ss) ) {
                                result.add( ss );
                            }
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
     * TODO: verify this works (I don't think it will) when filename contains slash.
     * @param root the Das2 FileSystem root, which can be a directory within a repo.
     * @param filename the filename to download, not containing any slashes.
     * @return the URL to download the file.
     * @see https://github.com/autoplot/dev/blob/master/bugs/ghdas2/demo173.jy
     * @throws MalformedURLException 
     */
    public URL gitHubMapFile( URI root, String filename ) throws MalformedURLException {
        
        filename= toCanonicalFilename( filename );
        //note filename now starts with a slash.
        
        if ( this.branch.length()==0 ) {
            try {
                this.branch= getDefaultBranch(root, project);
            } catch ( IOException ex ) {
                throw new RuntimeException(ex);
            }
        }
        
        if ( this.forge==Forge.GITLAB && this.branch.length()!=0 && this.project.length()!=0 ) {
            String url = "https://" 
                    + this.root.getHost() 
                    + "/api/v4/projects/" 
                    + this.project.replaceAll("/","%2F") 
                    + "/repository/files/"
                    + this.directory.replaceAll("/","%2F")
                    + filename.substring(1)
                    + "/raw?ref="
                    + this.branch;
            return new URL( url );
            
        }
        
        if ( this.forge==Forge.GITHUB && this.branch.length()!=0 && this.project.length()!=0 ) {
            if ( this.root.getHost().equals("github.com") ) {
                if ( this.directory.length()==0 ) {
                    if ( this.root.getPath().startsWith("/"+this.project) ) {
                        String dir= this.root.getPath().substring(this.project.length()+1);
                        if ( dir.startsWith("/"+branch) ) {
                            dir= dir.substring(branch.length()+1);
                        }
                        if ( dir.startsWith("/") ) {
                            dir= dir.substring(1);
                        }
                        if ( dir.endsWith("/") ) {
                            dir= dir.substring(0,dir.length()-1);
                        }
                        this.directory= dir;
                    } else {
                        throw new IllegalArgumentException("unable to derive path, please submit this so the problem can be resolved.");
                    }
                }
                String url = "https://" 
                    + "raw.githubusercontent.com/" 
                    + this.project + '/'
                    + this.branch +'/'
                    + this.directory +'/'
                    + filename.substring(1);
                return new URL( url );
            }
        }
        
        String sroot= root.toString();
        if ( sroot.startsWith("https://research-git.uiowa.edu/space-physics/") ) {
            // https://github.com/das-developers/das2java/issues/173
            String marker= "/main/";  // Too bad canonical Autoplot GitLab URIs don't have -/blob in them!
            if ( sroot.contains(marker) ) {
                int i = sroot.indexOf(marker);
                String rawUrl = sroot.substring(0, i) + "/-/raw"
                    + sroot.substring(i);
                return new URL(rawUrl + filename.substring(1));
            }
            marker= "/master/";  
            if ( sroot.contains(marker) ) {
                int i = sroot.indexOf(marker);
                String rawUrl = sroot.substring(0, i) + "/-/raw"
                    + sroot.substring(i);
                return new URL(rawUrl + filename.substring(1));
            }
        }
        
        if ( sroot.contains("/-/") ) {
            // https://git.jfaden.net/jbfaden/public/-/raw/main/2022/20220906/albums/img/IMG_20220903_140040358.jpg?ref_type=heads&inline=false
            int i = sroot.indexOf("/-/");
            String rawUrl = sroot.substring(0, i) + "/-/raw/" + branch + "/" + sroot.substring(i+3);
            return new URL(rawUrl + filename.substring(1));
        }
        
        // png image "https://github.com/autoplot/app/raw/master/Autoplot/src/resources/badge_ok.png"
        // another   "https://research-git.uiowa.edu/space-physics/rbsp/doc/README.md"
        String[] path= root.getPath().split("/",-2);
        String spath= path[0] + '/' + path[1] + '/' + path[2] ;
        
        // Note spath should be the same as "project" variable.
        
        if ( this.directory.length()==0 ) {
            if ( this.project.length()>0 ) {
                int i= sroot.indexOf(this.project);
                if ( i>-1 ) { // it really should be
                    i=i+this.project.length();
                    this.directory= sroot.substring(i);
                }
            }
        }
        // number of elements after the host to the base.
        int gitPathElements;
        
        // base is the position of the "blob" in file URLs.  E.g.:
        // https://github.com/autoplot/dev/blob/master/demos/2017/20170518/readme.md  is the same as
        // https://github.com/autoplot/dev/demos/2017/20170518/readme.md 
        
        int idash=-1;
        for ( int i=0; i<path.length; i++ ) {
            if ( path[i].equals("-") ) {
                idash= i;
                break;
            }
        }
        
        int base;
        if ( idash>-1 ) {
            if ( path[idash+1].equals(branch) ) {
                base= idash+1;
                gitPathElements= idash;
            } else if ( path.length>4+baseOffset && path[4+baseOffset].equals(branch) ) { // https://research-git.uiowa.edu/space-physics/juno/ap-script/-/
                base= 5;
                gitPathElements= 4;
                spath= path[0] + '/' + path[1] + '/' + path[2] + '/' + path[3];
            } else {
                base= 3;
                gitPathElements= 3;
            }
            spath= String.join("/",Arrays.copyOfRange(path,1,idash));
        } else {
            if ( path[3+baseOffset].equals(branch) ) {
                base= 4;
                gitPathElements= 3;
            } else if ( path.length>5+baseOffset && path[4+baseOffset].equals(branch) ) { // https://research-git.uiowa.edu/space-physics/juno/ap-script/-/
                base= 5;
                gitPathElements= 4;
                spath= path[0] + '/' + path[1] + '/' + path[2] + '/' + path[3];
            } else {
                base= 3;
                gitPathElements= 3;
            }
        }

        for ( int i=0; i<baseOffset; i++ ) {
            spath= spath + "/" + path[i+gitPathElements];
        }
                
        if ( spath.startsWith("/") ) spath=spath.substring(1);
        
        if ( project.length()==0 ) {
            project= spath; // calculated path for https://research-git.uiowa.edu/space-physics/rbsp/doc/README.md is wrong, but we already have it
        }
        if ( branch.length()==0 ) {
            try {
                if ( forge==Forge.GITLAB ) {
                    branch= getDefaultBranchGitLab(root, project);
                } else {
                    branch= getDefaultBranchGitHub(root, project);
                }
            } catch ( IOException ex ) {
                throw new RuntimeException(ex);
            }
        }
        
        if ( project.length()>0 && branch.length()>0 ) {
            String check= root.getScheme() + "://"+ root.getHost() + '/' + project + '/' + branch + '/';
            if ( sroot.startsWith( check ) ) {
                directory= sroot.substring(check.length());
            }
        }
        
        if ( project.length()>0 && directory.length()>0 ) {
            // https://github.com/autoplot/dev/raw/refs/heads/master/screen/20190704/flag4th.jy
            // https://github.com/autoplot/dev/raw/screen/20190704/flag4th.jy
            // https://github.com/autoplot/dev/blob/master/screen/20190704/flag4th.jy
            // https://github.com/autoplot/dev/raw/refs/heads/master/screen/20190704/flag4th.jy
            // https://raw.githubusercontent.com/autoplot/dev/refs/heads/master/screen/20190704/flag4th.jy
            
            // https://github.umn.edu/FADEN004/test-repo/blob/master/demos/20211005/zipdataset.jyds
            // https://raw.github.umn.edu/FADEN004/test-repo/refs/heads/master/demos/20211005/zipdataset.jyds?token=GHSAT0AAAAAAAAA7V24LM3FVJXJ7GH7ZEL62OZNP2Q
            String n;
            if ( this.forge==Forge.GITLAB ) {
                // https://research-git.uiowa.edu/jbf/testproject/-/blob/master/script/testScript.jy
                // https://research-git.uiowa.edu/jbf/testproject/-/raw/master/script/testScript.jy
                
                n= root.getScheme() + "://" + root.getHost() + '/' + this.project + "/-/raw/" + this.branch + "/" + this.directory + filename.substring(1);
            } else if ( this.forge==Forge.GITHUB ) {
                // https://github.com/autoplot/dev/raw/refs/heads/master/screen/20190704/flag4th.jy
   
                if ( this.directory.equals("/") ) {// https://github.com/autoplot/dev/
                    n= root.getScheme() + "://" + root.getHost() + '/' + this.project + "/raw/" + this.branch + "/" + filename.substring(1);
                } else {
                    //https://raw.githubuser.com/autoplot/dev/9c8610958f790c8a10503478038abb34393cbbfd/demos/2026/20260331/demoUriTemplateX.jy
                    n= root.getScheme() + "://raw." + root.getHost() + '/' + this.project + "/" + this.branch + "/" + this.directory + filename.substring(1);
                    //n= root.getScheme() + "://" + root.getHost() + '/' + this.project + "/raw/refs/heads/" + this.branch + "/" + this.directory + filename.substring(1);
                }
            } else {
                throw new IllegalArgumentException("unsupported forge: "+this.forge);
            }
            URL url= new URL( n );
            return url;
        }
        
        if ( (base+baseOffset)<path.length && path[ base+baseOffset].equals("blob") ) {
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
                    if ( project.length()==0 || branch.length()==0 ) {
                        String n= root.getScheme() + "://" + root.getHost() + '/' + project + "/raw/" + branch + pp + filename;
                        URL url= new URL( n );
                        return url;
                    } else {
                        String pathToDir= root.getScheme() + "://" + root.getHost() + '/' + project;
                        if ( root.toASCIIString().startsWith(pathToDir) ) {
                            String mm= root.toASCIIString().substring(pathToDir.length()+1);
                            if ( mm.startsWith(branch) ) {
                                mm= mm.substring(branch.length()+1);
                            }
                            String n= pathToDir + "/raw/" + branch + "/" + mm + filename.substring(1);
                            URL url= new URL( n );
                            return url; ///HERE
                        } else {
                            if ( pp.length()>0 ) pp= "/" + pp; 
                            String n= root.getScheme() + "://" + root.getHost() + '/' + project + "/raw/" + branch + pp + filename;
                            URL url= new URL( n );
                            return url;
                        }
                    }
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
            if ( forge==Forge.GITLAB && token.length()>0 ) {
                urlc.setRequestProperty("PRIVATE-TOKEN", token);
            }
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
        return "githubfs " + root+ ( isOffline() ? " (offline)" : "" );
    }
    
}
