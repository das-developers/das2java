package org.das2.util.filesystem;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.util.FileUtil;
import org.das2.util.monitor.ProgressMonitor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/**
 * GitLab-backed FileSystem for Das2.
 *
 * Intended URI forms:
 *
 *   https://git.example.edu/group/subgroup/project/-/blob/main/path/to/file.txt
 *   https://git.example.edu/group/subgroup/project/-/tree/main/path/to/dir
 *   https://git.example.edu/group/subgroup/project
 *
 * Internal strategy:
 *   - parse the web URL into host + projectPath + optional ref + optional repoPath
 *   - if ref is missing, query project metadata and use default_branch
 *   - list directories with /api/v4/projects/:id/repository/tree
 *   - read files with /-/raw/{ref}/{path}
 */
public class GitLabFileSystem extends WebFileSystem {

    private final URI rootUri;
    private final String token;
    private final String branch;
    
    protected final ParsedGitLabRoot parsedRoot;
    private final Map<String, String> defaultBranchCache = new HashMap<>();
    private final Map<String, DirectoryCacheEntry> directoryCache = new LinkedHashMap<String, DirectoryCacheEntry>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, DirectoryCacheEntry> eldest) {
            return size() > 128;
        }
    };

    public static GitLabFileSystem createGitLabFileSystem( URI rootUri ) throws IOException {
        File localRoot = WebFileSystem.localRoot(rootUri);
        return new GitLabFileSystem( rootUri, localRoot, "", 0, null );
        
    }
    
    protected GitLabFileSystem(URI rootUri) throws IOException {
        this(rootUri, null, null, 0, null);
    }

    protected GitLabFileSystem( URI rootUri, File localRoot, String branch, int baseOffset, String token) throws IOException {
        super(rootUri,localRoot);
        this.rootUri = rootUri;
        this.token = token;
        this.parsedRoot = parseRoot(rootUri);
        this.branch= branch;
    }

    @Override
    public String[] listDirectory(String directory) throws IOException {
        try {
            String repoPath = normalizeRepoPath(directory);
            List<GitLabNode> nodes = listNodes(repoPath);
            
            String[] result = new String[nodes.size()];
            DirectoryEntry[] wfsCache= new DirectoryEntry[result.length];
            for (int i = 0; i < nodes.size(); i++) {
                GitLabNode n = nodes.get(i);
                result[i] = n.isDirectory ? n.name + "/" : n.name;
                DirectoryEntry de= new DirectoryEntry();
                de.name=result[i];
                de.type= n.isDirectory ? 'd' : 'f';
                de.size= n.size;
                de.modified= Long.MAX_VALUE;
                wfsCache[i]= de;
            }
            cacheListing(directory, wfsCache);
            return result;
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean isDirectory(String filename) throws IOException {
        String p = normalizeRepoPath(filename);
        if (p.length() == 0) {
            return true;
        }

        GitLabNode node = stat(p);
        return node != null && node.isDirectory;
    }


    /**
     * Optional convenience method if Das2 FileSystem has a method like this.
     */
    public long getSize(String filename) throws IOException {
        GitLabNode node = stat(normalizeRepoPath(filename));
        return node == null ? -1L : node.size;
    }

    /**
     * Optional convenience.
     */
    public URI getRootURI() {
        return rootUri;
    }

    // ------------------------------------------------------------------------
    // Core GitLab logic
    // ------------------------------------------------------------------------

    private List<GitLabNode> listNodes(String directory) throws IOException, JSONException {
        String key = getResolvedRef() + "::" + directory;
        DirectoryCacheEntry cached = directoryCache.get(key);
        if (cached != null) {
            return cached.nodes;
        }

        String api = buildRepositoryTreeUrl(directory);
        String txt = readText(api);
        JSONArray arr = new JSONArray(txt);

        List<GitLabNode> out = new ArrayList<GitLabNode>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            GitLabNode n = new GitLabNode();
            n.name = o.getString("name");
            n.path = o.getString("path");
            n.isDirectory = "tree".equals(o.getString("type"));
            n.size = o.has("size") && !o.isNull("size") ? o.getLong("size") : -1L;
            n.sha = o.has("id") ? o.getString("id") : null;
            out.add(n);
        }

        directoryCache.put(key, new DirectoryCacheEntry(Collections.unmodifiableList(out)));
        return out;
    }

    protected GitLabNode stat(String repoPath) throws IOException {
        try {
            String parent = parentPath(repoPath);
            List<GitLabNode> siblings = listNodes(parent);
            String name = leafName(repoPath);
            
            for (GitLabNode n : siblings) {
                if (n.name.equals(name)) {
                    return n;
                }
            }
            return null;
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String buildRepositoryTreeUrl(String directory) throws IOException {
        String projectId = urlEncode(parsedRoot.projectPath);
        String ref = urlEncode(getResolvedRef());

        StringBuilder sb = new StringBuilder();
        sb.append("https://").append(parsedRoot.host)
          .append("/api/v4/projects/")
          .append(projectId)
          .append("/repository/tree?per_page=100")
          .append("&ref=").append(ref);

        if (directory != null && directory.length() > 0) {
            if ( directory.endsWith("/") ) directory= directory.substring(0,directory.length()-1);
            sb.append("&path=").append(urlEncode(directory));
        }

        return sb.toString();
    }

    protected String getResolvedRef() throws IOException {
        try {
            if (parsedRoot.ref != null && parsedRoot.ref.length() > 0) {
                return parsedRoot.ref;
            }
            
            String cached = defaultBranchCache.get(parsedRoot.projectPath);
            if (cached != null) {
                return cached;
            }
            
            String path= parsedRoot.projectPath;
            String api = "https://" + parsedRoot.host + "/api/v4/projects/" + urlEncode(path);
            String txt = readText(api);
            JSONObject obj = new JSONObject(txt);
            String branch = obj.getString("default_branch");
            defaultBranchCache.put(parsedRoot.projectPath, branch);
            return branch;
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String readText(String url) throws IOException {
        HttpURLConnection c = open(url);
        int code = c.getResponseCode();
        if (code == 404) {
            throw new IOException("GitLab API returned 404 for " + url);
        }
        if (code < 200 || code >= 300) {
            throw new IOException("GitLab API failed, HTTP " + code + ": " + url);
        }
        String s = FileUtil.readInputStreamToString(c.getInputStream());
        return s == null ? "" : s;
    }

    protected HttpURLConnection open(String surl) throws IOException {
        URI u = URI.create(surl);
        HttpURLConnection c = (HttpURLConnection) u.toURL().openConnection();
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("User-Agent", "Das2-GitLabFileSystem");
        if (token != null && token.trim().length() > 0) {
            c.setRequestProperty("PRIVATE-TOKEN", token);
        }
        return c;
    }

    // ------------------------------------------------------------------------
    // Parsing
    // ------------------------------------------------------------------------

    private static ParsedGitLabRoot parseRoot(URI uri) throws IOException {
        String host = uri.getHost();
        if (host == null) {
            throw new IOException("URI has no host: " + uri);
        }

        String rawPath = uri.getPath();
        if (rawPath == null) rawPath = "";
        if (rawPath.startsWith("/")) rawPath = rawPath.substring(1);
        if (rawPath.endsWith("/")) rawPath= rawPath.substring(0,rawPath.length()-1);

        String[] parts = rawPath.split("/");
        if (parts.length < 2) {
            throw new IOException("Not enough path components for GitLab project: " + uri);
        }

        ParsedGitLabRoot out = new ParsedGitLabRoot();
        out.host = host;

        int dash = indexOfDashDash(parts);

        if (dash >= 0) {
            // .../<project>/-/(blob|tree|raw)/<ref>/<repoPath...>
            if (dash + 2 >= parts.length) {
                throw new IOException("Malformed GitLab URL: " + uri);
            }

            String mode = parts[dash + 1];
            if (!"blob".equals(mode) && !"tree".equals(mode) && !"raw".equals(mode)) {
                throw new IOException("Unsupported GitLab URL mode '" + mode + "': " + uri);
            }

            out.projectPath = join(parts, 0, dash);
            out.ref = parts[dash + 2];
            out.initialRepoPath = dash + 3 < parts.length ? join(parts, dash + 3, parts.length) : "";
        } else {
            // plain project URL, no ref/path
            out.projectPath = rawPath;
            out.ref = null;
            out.initialRepoPath = "";
        }

        return out;
    }

    private static int indexOfDashDash(String[] parts) {
        for (int i = 0; i < parts.length; i++) {
            if ("-".equals(parts[i])) return i;
        }
        return -1;
    }

    private static String join(String[] parts, int i0, int i1) {
        StringBuilder sb = new StringBuilder();
        for (int i = i0; i < i1; i++) {
            if (i > i0) sb.append('/');
            sb.append(parts[i]);
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    // Path helpers
    // ------------------------------------------------------------------------

    protected String normalizeRepoPath(String p) {
        if (p == null) return parsedRoot.initialRepoPath;
        p = p.trim();
        if (p.startsWith("/")) p = p.substring(1);
        if (p.length() == 0) return parsedRoot.initialRepoPath == null ? "" : parsedRoot.initialRepoPath;
        return p;
    }

    private static String parentPath(String p) {
        int i = p.lastIndexOf('/');
        return i < 0 ? "" : p.substring(0, i);
    }

    private static String leafName(String p) {
        int i = p.lastIndexOf('/');
        return i < 0 ? p : p.substring(i + 1);
    }

    private static String urlEncode(String s) throws IOException {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Encode each path segment but preserve slashes.
     */
    protected static String urlEncodePathSegmentwise(String path) throws IOException {
        if (path == null || path.length() == 0) {
            return "";
        }
        String[] ss = path.split("/");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ss.length; i++) {
            if (i > 0) sb.append('/');
            sb.append(urlEncode(ss[i]));
        }
        return sb.toString();
    }

    @Override
    public FileObject getFileObject(String filename) {
        return new GitLabFileObject(this,filename,new Date(Long.MAX_VALUE));
    }

    private static void copy(InputStream in,OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) >= 0) {
            out.write(buf, 0, n);
        }
    }
 
    @Override
    protected Map<String, String> downloadFile(String filename, File f, File partfile, ProgressMonitor monitor) throws IOException {
        
        FileObject fo= getFileObject(filename);
        
        FileOutputStream fout= new FileOutputStream(partfile);
        copy(fo.getInputStream(monitor),fout);
        
        fout.close();
        partfile.renameTo(f);
        
        return new HashMap<>();
    }

    // ------------------------------------------------------------------------
    // Small data classes
    // ------------------------------------------------------------------------

    protected static class ParsedGitLabRoot {
        String host;
        String projectPath;
        String ref;
        String initialRepoPath;
    }
    

    
    protected static class GitLabNode {
        String name;
        String path;
        boolean isDirectory;
        long size;
        String sha;
    }

    private static class DirectoryCacheEntry {
        final List<GitLabNode> nodes;

        DirectoryCacheEntry(List<GitLabNode> nodes) {
            this.nodes = nodes;
        }
    }
    
}