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
 * FileWebFileSystem.java
 *
 * Created on May 14, 2004, 1:02 PM
 */

package org.das2.util.filesystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.das2.util.filesystem.FileSystem.FileSystemOfflineException;


/**
 *
 * @author  Jeremy
 */
public class LocalFileSystem extends FileSystem {
    
    File localRoot;
    
    /**
     * Note the String used to create the URL should have either one or three slashes:
     *   file:/home/jbf    or   file:///home/jbf   
     *   but not file://home/jbf
     * Also, on Windows, /c:/documents and settings/jbf/  is okay.
     * @param root
     * @throws org.das2.util.filesystem.FileSystem.FileSystemOfflineException
     * @throws java.io.FileNotFoundException if the root does not exist.
     */
    protected LocalFileSystem(URI root) throws FileSystemOfflineException, FileNotFoundException {
        super( root );
        if ( !("file".equals(root.getScheme()) ) ) {
            throw new IllegalArgumentException("protocol not file: "+root);
        }
        String surl= root.getPath();
        if ( surl==null ) {
            throw new URIException("root contains no path: "+root);
        }
        if ( !surl.endsWith("/") ) surl+="/";
        if ( surl.startsWith("file://") && !surl.startsWith("file:///") ) {
            throw new URIException("Local file URLs should start with file:/ or file:///, but not file:// "+surl);
        }
        String[] split= FileSystem.splitUrl( surl );
        localRoot=new File( split[2].substring(split[0].length()) );
//        try { // simulate slow web site
//            Thread.sleep(5000);
//        } catch (InterruptedException ex) {
//            logger.log(Level.SEVERE, null, ex);
//        }
        if ( !localRoot.exists() ) {
            File[] roots= File.listRoots();
            if ( Arrays.asList(roots).contains(localRoot) ) {
                throw new FileSystemOfflineException();
            } else {
                throw new FileNotFoundException( "local root does not exist: "+localRoot );
            }
        }
        boolean b= new File("xxx").equals(new File("XXX"));
        properties.put( PROP_CASE_INSENSITIVE, b );
    }
    
    @Override
    public boolean isDirectory(String filename) {
        return new File( localRoot, filename ).isDirectory();
    }
    
    String getLocalName( File file ) {
        if ( !file.toString().startsWith(localRoot.toString() ) ) {
            throw new IllegalArgumentException( "file \""+file+"\"is not of this web file system" );
        }
        String filename= file.toString().substring(localRoot.toString().length() );
        filename= filename.replaceAll( "\\\\", "/" );
        return filename;
    }
    
    @Override
    public String[] listDirectory(String directory) {
        File f= new File( localRoot, directory );
        if ( !f.canRead() || ( f.getParentFile()!=null && f.isHidden() ) ) {
            throw new IllegalArgumentException("cannot read directory " +f );
        }
        File[] files= f.listFiles();
        if ( files==null ) { // On Windows, I was getting null with c:\Users\sklemuk\Documents.
            return new String[0];
        }
        List<String> result= new ArrayList();
        for (File file : files) {
            if (!file.isHidden()) {
                // Windows 7 hides "c:/Documents and Settings", and we get bugs if this is presented.
                result.add(file.getName() + (file.isDirectory() ? "/" : ""));
            }
        }
        return result.toArray( new String[result.size()] );
    }
    
    @Override
    public String[] listDirectory(String directory, String regex ) {
        File f= new File( localRoot, directory );
        final Pattern pattern= Pattern.compile(regex);
        if ( !f.canRead() || ( f.getParentFile()!=null && f.isHidden() ) ) throw new IllegalArgumentException("cannot read directory " +f );
        File[] files= f.listFiles( (File file, String name) -> pattern.matcher(name).matches() && ! file.isHidden() );
        if ( files==null ) {
            throw new IllegalStateException("unable to list directory: "+f );
        }
        String[] result= new String[files.length];
        for ( int i=0; i<files.length; i++ ) result[i]= files[i].getName() + ( files[i].isDirectory() ? "/" : "" );
        return result;
    }
    
    @Override
    public String toString() {
        String s= String.valueOf(localRoot);
        s= s.replaceAll("\\\\","/");
        if ( !s.endsWith("/" ) ) s= s+"/";
        return "lfs "+s;
    }
    
    @Override
    public FileObject getFileObject(String filename) {
//        try { // simulate slow filesystem
//            Thread.sleep(1000);
//        } catch (InterruptedException ex) {
//            logger.log(Level.SEVERE, null, ex);
//        }
        return new LocalFileObject( this, localRoot, filename );
    }
    
    @Override
    public File getLocalRoot() {
        return localRoot;
    }
    
}
