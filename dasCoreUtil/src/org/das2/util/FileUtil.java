/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.das2.util.filesystem.Glob;

/**
 * static utility methods.
 * 
 * introduced Jul 28, 2008.
 * 
 * @author jbf
 */
public class FileUtil {

    private FileUtil() {
    }
    
    /**
     * return true of the maybeChild parent is a child of possibleParent.  Note either
     * can be null, and this will not throw an exception, but will return false.
     * @param possibleParent parent file.
     * @param maybeChild a file or folder which may exist within possibleParent.
     * @return true if the possibleParent is actually a parent of maybeChild.
     */
    public static boolean isParent(File possibleParent, File maybeChild ) {
        if ( possibleParent==null || maybeChild==null ) return false;
        possibleParent= possibleParent.getAbsoluteFile();
        if ( !possibleParent.exists() || !possibleParent.isDirectory() ) {
            // this cannot possibly be the parent
            return false;
        }
        maybeChild= maybeChild.getAbsoluteFile();
        URI parentURI = possibleParent.toURI(),
        childURI = maybeChild.toURI();
        return !parentURI.relativize(childURI).isAbsolute();
    }
    
    /**
     * deletes all files and folders below root, and root, just as "rm -r" would.
     * TODO: check links     
     * @param root the root where we start deleting.
     * @return true if the operation was successful.
     * @throws IllegalArgumentException if it is unable to delete a file
     */
    public static boolean deleteFileTree(File root) throws IllegalArgumentException {
        return deleteFileTree( root, null );
    }

    /**
     * deletes all files and folders below root, and root, just as "rm -r" would, excluding
     * any files named in exclude.  For example, exclude could contain "readme.txt".
     * @param root the root where we start deleting.
     * @param exclude null or a set containing names to exclude.
     * @return true if the operation was successful.
     * @throws IllegalArgumentException if it is unable to delete a file
     */
    public static boolean deleteFileTree( File root, Set<String> exclude ) throws IllegalArgumentException {
        if (!root.exists()) {
            return true;
        }
        if (!root.canRead()) {
            throw new IllegalArgumentException("cannot read folder " + root );
        }
        File[] children = root.listFiles(); // root is known to exist.
        if ( children==null ) {
            throw new IllegalArgumentException("listFiles returns null, root must be a directory and not a file.");
        }
        boolean success = true;
        boolean noExclude= true;
        for (File children1 : children) {
            if (exclude!=null && exclude.contains(children1.getName())) {
                noExclude= false;
                continue;
            }
            if (children1.isDirectory()) {
                success = success && deleteFileTree(children1, exclude);
            } else {
                success = success && (!children1.exists() || children1.delete()); // in case file is deleted by another process, check exists again.
                if (!success) {
                    throw new IllegalArgumentException("unable to delete file " + children1);
                }
            }
        }
        if ( noExclude ) {
            success = success && (!root.exists() || root.delete());
        }
        if (!success) {
            throw new IllegalArgumentException("unable to delete folder " + root);
        }
        return success;        
    }
    
    /**
     * deletes all files with the given name, and root, just as "find . -name name -exec rm {} \;" would.
     * TODO: check links.  For example deleteWithinFileTree( root, ".listing" )
     * @param root the root directory of the tree.
     * @param name the file name.
     * @throws IllegalArgumentException if it is unable to delete a file
     * @return true if the operation was successful.
     */
    public static boolean deleteWithinFileTree(File root,String name) throws IllegalArgumentException {
        if (!root.exists()) {
            return true;
        }
        if ( !root.canRead() ) {
            System.err.println("unable to read folder: "+root );
            return true;
        }
        File[] children = root.listFiles();
        if ( children==null ) {
            throw new IllegalArgumentException("listFiles returns null, root must be a directory and not a file.");
        }
        boolean success = true;
        for (File children1 : children) {
            if (children1.isDirectory()) {
                success = success && deleteWithinFileTree(children1, name);
            } else {
                if (children1.getName().equals(name)) {
                    success = success && (!children1.exists() || children1.delete());
                    if (!success) {
                        throw new IllegalArgumentException("unable to delete file " + children1);
                    }
                }
            }
        }
        return success;
    }
    
    /**
     * find a files with the given name within the given root, just as "find . -name name -print \;" would.
     * TODO: check links.  For example, find( "/usr/share/fonts/truetype", "FreeMono.ttf" )
     * @param root the root to start
     * @param name name to look for.
     * @throws IllegalArgumentException if the root does not exist.
     * @return the File found, or null if it does not exist.
     */
    public static File find(File root,String name) throws IllegalArgumentException {
        if (!root.exists()) {
            throw new IllegalArgumentException("File does not exist:"+root);
        }
        if ( !root.isDirectory() ) {
            throw new IllegalArgumentException("root should be a directory: "+root);
        }
        if ( !root.canRead() ) {
            throw new IllegalArgumentException("unable to read root: "+root);
        }
        File[] children = root.listFiles(); // root is known to exist
        for (File children1 : children) {
            if (children1.isDirectory()) {
                File f = find(children1, name);
                if ( f!=null ) return f;
            } else {
                if (children1.getName().equals(name)) {
                    return children1;
                }
            }
        }
        return null;
    }    
        
    /**
     * find a files with the given pattern within the given root, 
     * just as "find . -name *.dat -print \;" would.
     * TODO: check links.  For example, find( "/usr/share/fonts/truetype", "FreeMono.ttf" )
     * @param root the root to start
     * @param pattern name to look for.
     * @param result results are added to this list, or null if the count that is all that is needed.
     * @throws IllegalArgumentException if the root does not exist.
     * @return the number of files found
     * @see org.das2.util.filesystem.Glob
     */
    public static int find( File root, Pattern pattern, List<String> result ) throws IllegalArgumentException {
        if ( result==null ) result= new ArrayList<>();
        
        if (!root.exists()) {
            throw new IllegalArgumentException("File does not exist:"+root);
        }
        if ( !root.isDirectory() ) {
            throw new IllegalArgumentException("root should be a directory: "+root);
        }
        if ( !root.canRead() ) {
            throw new IllegalArgumentException("unable to read root: "+root);
        }
        int count= 0;
        File[] children = root.listFiles(); // root is known to exist
        for (File children1 : children) {
            if (children1.isDirectory()) {
                count += find(children1, pattern,result);
            } else {
                if ( pattern.matcher( children1.getName() ).matches() ) {
                    result.add( children1.getAbsolutePath() );
                    count= count+1;
                }
            }
        }
        return count;
    }    
    
    /**
     * find a files with the given name within one of the given roots.
     * TODO: check links.  For example, find( "/usr/share/fonts/truetype", "FreeMono.ttf" )
     * This allows root folders that do not exist.
     * @param roots array of roots to start search.
     * @param name name to look for.
     * @return the File found, or null if it does not exist.
     */
    public static File find(File[] roots,String name) {
        for ( File root: roots ) {
            if ( root.exists() ) {
                File r= find( root, name );
                if ( r!=null ) {
                    return r;
                }
            }
        }
        return null;
    }    
    
    /**
     * find all files under the root matching the spec.
     * @param root the root of the search (e.g. /fonts/)
     * @param name the pattern to match
     * @param matches list that will accept the matches, or null if one should be created.
     * @return the list.
     */
    public static List<File> listRecursively( File root, Pattern name, List<File> matches ) {
        if (!root.exists()) {
            throw new IllegalArgumentException("File does not exist:"+root);
        }
        if (!root.isDirectory()) {
            throw new IllegalArgumentException("root is not a folder:"+root);
        }
        if (!root.canRead()) return Collections.emptyList();
        if ( matches==null ) matches= new ArrayList();
        File[] children = root.listFiles(); // root is known to exist.
        for (File children1 : children) {
            if (children1.isDirectory()) {
                listRecursively(children1, name, matches);
            } else {
                if (name.matcher(children1.getName()).matches()) {
                    matches.add(children1);
                }
            }
        }
        return matches;
    }
    
    /**
     * Return an array of files where the regex is found at the end.  A check is performed to see if the root is case-insensitive.
     * @param root the root of the search (e.g. /fonts/)
     * @param glob the glob to match (e.g. *.ttf)
     * @return list of files.
     */
    public static File[] listRecursively( File root, String glob ) {
        String regex= Glob.getRegex( glob );
        boolean b= new File(root,"xxx").equals(new File(root,"XXX"));
        if ( b ) regex= "(?i)"+regex;
        Pattern name= Pattern.compile( ".*" + regex );
        List<File> result= listRecursively( root, name, null );
        return result.toArray( new File[result.size()] );
    }
    
    /**
     * copies the file or folder from src to dst.
     * @param src the source file or folder.
     * @param dst the location for the new file or folder.
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public static void fileCopy( File src, File dst ) throws FileNotFoundException, IOException {
        if ( !src.exists() ) throw new IllegalArgumentException("src does not exist.");
        if ( !src.canRead() ) throw new IllegalArgumentException("src cannot be read.");
        if ( src.equals(dst) ) throw new IllegalArgumentException("src and dst files are the same");
        if ( src.isDirectory() && ( !dst.exists() || dst.isDirectory() ) ) {
            if ( !dst.exists() ) {
                if ( !dst.mkdirs() ) throw new IOException("unable to mkdir " + dst);
            }
            File dst1= new File( dst, src.getName() );
            if ( !dst1.exists() && !dst1.mkdir() ) throw new IOException("unable to mkdir " + dst1);
            dst= dst1;
            File[] files= src.listFiles(); // src is known to exist.
            for ( File f:files ) {
                if ( f.isDirectory() ) {
                    dst1= new File( dst, f.getName() );
                    if ( !dst1.exists() && !dst1.mkdir() ) throw new IOException("unable to mkdir " + dst1);
                } else {
                    dst1= dst;
                }
                fileCopy( f, dst1 );
            }
            return;
        } else if ( dst.isDirectory() ) {
            dst= new File( dst, src.getName() );
        }
        FileChannel ic = new FileInputStream(src).getChannel();
        FileChannel oc = new FileOutputStream(dst).getChannel();
        try {
            ic.transferTo(0, ic.size(), oc);
        } finally {
            ic.close();
            oc.close();
        }
    }
    
    /**
     * return the first four bytes of the file as a string.  Some magic
     * numbers we care about:
     *  <li> '\x89HDF'  HDF (and new NetCDF) files
     * @param src
     * @return a four byte string
     * @throws java.io.FileNotFoundException
     * @throws IllegalArgumentException if the file is less than four bytes.
     */
    public static String getMagic(File src) throws FileNotFoundException, IOException {
        byte[] four= new byte[4];
        FileChannel ic = new FileInputStream(src).getChannel();
        ByteBuffer buf= ByteBuffer.wrap(four);
        int bytesRead=0;
        try {
            while ( bytesRead<4 ) {
                int bytes= ic.read( buf );
                if ( bytes==-1 ) {
                    if ( bytesRead==0 ) {
                        throw new IllegalArgumentException("File is empty: "+src);
                    } else {
                        throw new IllegalArgumentException("File has less than four bytes: "+src);
                    }
                }
                bytesRead+= bytes;
            }
        } finally {
            ic.close();
        }
        return new String( four );
    }

    
}
