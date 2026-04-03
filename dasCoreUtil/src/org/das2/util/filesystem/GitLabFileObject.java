
package org.das2.util.filesystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Date;
import org.das2.util.monitor.ProgressMonitor;

/**
 *
 * @author jbf
 */
public class GitLabFileObject extends WebFileObject {
    
    GitLabFileSystem gitlabFS;
    
    public GitLabFileObject(GitLabFileSystem wfs, String pathname, Date modifiedDate) {
        super(wfs, pathname, modifiedDate);
        this.gitlabFS= wfs;
    }

    @Override
    public boolean exists() {            
        try {
            String p = gitlabFS.normalizeRepoPath(pathname);
            if (p.length() == 0) {
                return true;
            }
            return gitlabFS.stat(p) != null;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    @Override
    public InputStream getInputStream(ProgressMonitor monitor) throws IOException {
        String repoPath = gitlabFS.parsedRoot.initialRepoPath + "/"+ gitlabFS.normalizeRepoPath(this.pathname);
        String ref = gitlabFS.getResolvedRef();

        String rawUrl = "https://" + gitlabFS.parsedRoot.host
                + "/" + gitlabFS.parsedRoot.projectPath
                + "/-/raw/" + gitlabFS.urlEncodePathSegmentwise(ref)
                + "/" + gitlabFS.urlEncodePathSegmentwise(repoPath);

        HttpURLConnection c = gitlabFS.open(rawUrl);
        int code = c.getResponseCode();
        if (code == 404) {
            throw new IOException("GitLab file not found: " + pathname + " [" + rawUrl + "]");
        }
        if (code < 200 || code >= 300) {
            throw new IOException("GitLab raw download failed, HTTP " + code + ": " + rawUrl);
        }
        return c.getInputStream();
    }

    @Override
    public File getFile(ProgressMonitor monitor) throws FileNotFoundException, IOException {
        File partfile= new File( localFile.getAbsolutePath() + ".part." + Thread.currentThread().getName() );
        FileOutputStream fout= new FileOutputStream(partfile);
        copy( getInputStream(monitor),fout );
        
        fout.close();
        partfile.renameTo(localFile);
        
        return localFile;
    }
    
    
    private static void copy(InputStream in,OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) >= 0) {
            out.write(buf, 0, n);
        }
    }
    
}
