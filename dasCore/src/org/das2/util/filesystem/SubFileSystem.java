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
 * SubFileSystem.java
 *
 * Created on January 16, 2007, 2:19 PM
 *
 *
 */

package org.das2.util.filesystem;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author Jeremy
 */
public class SubFileSystem extends FileSystem {
    FileSystem parent;
    String dir;
    
    protected SubFileSystem( FileSystem parent, String dir ) throws MalformedURLException {
        super( new URL( parent.getRootURL(), dir ) );
        this.parent= parent;
        this.dir= dir;
        
    }
    
    public FileObject getFileObject(String filename) {
        return parent.getFileObject( dir + filename );
    }
    
    public boolean isDirectory(String filename) throws IOException {
        return parent.isDirectory( dir + filename );
    }
    
    public String[] listDirectory(String directory) throws IOException {
        return parent.listDirectory( dir + directory );
    }
    
    public String[] listDirectory(String directory, String regex) throws IOException {
        return parent.listDirectory( dir + directory, regex );
    }
    
    @Override
    public File getLocalRoot() {
        return new File( parent.getLocalRoot(), dir );
    }
    
    
}
