/* Copyright (C) 2003-2015 The University of Iowa 
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
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * present part of a filesystem as a filesystem.
 * @author Jeremy
 */
public class SubFileSystem extends FileSystem {
    FileSystem parent;
    String dir;
    
    private static String trimFront( String dir ) {
        int i=0;
        while ( i<dir.length() && dir.charAt(i)=='/' ) {
            i++;
        }
        return i<dir.length() ? dir.substring(i) : "";
    }
    
    protected SubFileSystem( FileSystem parent, String dir ) throws MalformedURLException, URISyntaxException {
        super( FileSystemUtil.toUri( parent.getRootURI().toString() + trimFront( dir ) ) ); 
        this.parent= parent;
        this.dir= dir;
        
    }
    
    @Override
    public FileObject getFileObject(String filename) {
        return parent.getFileObject( dir + filename );
    }
    
    @Override
    public boolean isDirectory(String filename) throws IOException {
        return parent.isDirectory( dir + filename );
    }
    
    @Override
    public String[] listDirectory(String directory) throws IOException {
        return parent.listDirectory( dir + directory );
    }
    
    @Override
    public String[] listDirectory(String directory, String regex) throws IOException {
        return parent.listDirectory( dir + directory, regex );
    }
    
    @Override
    public File getLocalRoot() {
        return new File( parent.getLocalRoot(), dir );
    }

    @Override
    public String toString() {
        return "subfs "+parent.toString()+" " +dir;
    }
    
    /**
     * return the parent filesystem.
     * @return the parent filesystem.
     */
    protected FileSystem getParent() {
        return parent;
    }

    @Override
    public FileSystem createFileSystem(String directory) throws URISyntaxException {
        if ( directory.startsWith("/") ) directory= directory.substring(1);
        try {
            return new SubFileSystem(parent, dir + directory);
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
    
    
    
}
