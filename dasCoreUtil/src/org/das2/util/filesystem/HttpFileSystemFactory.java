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
import java.net.UnknownHostException;
import org.das2.util.filesystem.FileSystem.FileSystemOfflineException;

/**
 * Creates a FileSystem for reading files via HTTP and HTTPS.
 * @author jbf
 */
public class HttpFileSystemFactory implements FileSystemFactory {

    public HttpFileSystemFactory() {
    }

    @Override
    public FileSystem createFileSystem(URI root) throws FileSystemOfflineException, UnknownHostException, FileNotFoundException {
        String h;
        try {
            h= root.toURL().getHost(); // shows issues: http://demo@host:demo@www-pw.physics.uiowa.edu/~jbf/data/restrictAt/
        } catch ( MalformedURLException ex ) {
            h= "";  // handle as before github addition.
        }
        if ( h.equals("github.com") ) {
            WebFileSystem result= GitHubFileSystem.createGitHubFileSystem(root);
            return result;
        } else if ( h.equals("git.uiowa.edu") ) {
            WebFileSystem result= GitHubFileSystem.createGitHubFileSystem(root);
            return result;
        } else {
            HttpFileSystem hfs = HttpFileSystem.createHttpFileSystem(root);
            if (!FileSystemSettings.hasAllPermission()) hfs.setAppletMode(true);
            return hfs;
        }
    }
}
