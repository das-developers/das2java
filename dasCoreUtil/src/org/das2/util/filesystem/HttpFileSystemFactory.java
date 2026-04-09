/* Copyright (C) 2003-2008 The University of Iowa 
 *
 * This file is part of the Das2 <www.das2.org> utilities library.
 *
 * Das2 utilities are free software: you can redistribute and/or modify them
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Das2 utilities are distributed in the hope that they will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * as well as the GNU General Public License along with Das2 utilities.  If
 * not, see <http://www.gnu.org/licenses/>.
 *
 * HttpFileSystemFactory.java
 *
 * Created on November 15, 2007, 9:28 AM
 *
 */
package org.das2.util.filesystem;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.util.LoggerManager;

import org.das2.util.filesystem.FileSystem.FileSystemOfflineException;

/**
 * Creates a FileSystem for reading files via HTTP and HTTPS.
 * @author jbf
 */
public class HttpFileSystemFactory implements FileSystemFactory {

    private static final Logger logger= LoggerManager.getLogger("das2.filesystem.http");
    
    public HttpFileSystemFactory() {
    }

    /**
     * normalize the URI so that no part of the path contains multiple slashes (//).  Authored by ChatGPT.
     * @param uri
     * @return
     * 
     */
    public static URI normalizePath(URI uri) {
        try {
            String path = uri.getPath();
            
            if (path != null) {
                // Replace multiple slashes with a single slash
                String path1 = path.replaceAll("/{2,}", "/");
                if ( path1.equals(path) ) {
                    return uri;
                } else {
                    path= path1;
                }
            }
            
            // Rebuild the URI with the normalized path
            return new URI(
                    uri.getScheme(),
                    uri.getUserInfo(),
                    uri.getHost(),
                    uri.getPort(),
                    path,
                    uri.getQuery(),
                    uri.getFragment()
            );
        } catch (URISyntaxException ex) {
            Logger.getLogger(HttpFileSystemFactory.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }
    
    @Override
    public FileSystem createFileSystem(URI root) throws FileSystemOfflineException, UnknownHostException, FileNotFoundException {
        String h;
        
        root= normalizePath(root);
        
        try {
            h= root.toURL().getHost(); // shows issues: http://demo@host:demo@www-pw.physics.uiowa.edu/~jbf/data/restrictAt/
        } catch ( MalformedURLException ex ) {
            h= "";  // handle as before github addition.
        }        
        String githubPath=GitHubFileSystem.isGithubFileSystem( h, root.getPath() );
        if ( githubPath!=null ) {
            //kludge in abbith to research-git for UIowa.
            if ( h.equals("abbith.physics.uiowa.edu") ) {
                try {
                    root= new URI( root.getScheme(),
                            root.getUserInfo(),
                            "research-git.uiowa.edu",
                            -1,
                            "/abbith"+root.getPath().substring(4),
                            root.getQuery(),
                            root.getFragment() );
                    logger.fine("rewrote abbith URI to research-git");
                } catch (URISyntaxException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
            int offset = githubPath.split("/").length-1;
            WebFileSystem result= GitHubFileSystem.createGitHubFileSystem(root,offset);
            return result;                          
        } else {
            HttpFileSystem hfs = HttpFileSystem.createHttpFileSystem(root);
            //TODO: In the response, there's: <meta content="GitLab" property="og:site_name">
            // which could be used to detect gitlabs.
            if (!FileSystemSettings.hasAllPermission()) hfs.setAppletMode(true);
            return hfs;
        }
    }
}
