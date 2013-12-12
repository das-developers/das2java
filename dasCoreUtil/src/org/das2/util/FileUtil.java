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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.das2.util.filesystem.FileSystem.PROP_CASE_INSENSITIVE;
import org.das2.util.filesystem.FileSystemSettings;
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
        File[] children = root.listFiles();
        boolean success = true;
        boolean noExclude= true;
        for (int i = 0; i < children.length; i++) {
            if ( exclude!=null && exclude.contains(children[i].getName()) ) {
                noExclude= false;
                continue;
            }
            if (children[i].isDirectory()) {
                success = success && deleteFileTree(children[i],exclude);
            } else {
                success = success && ( !children[i].exists() || children[i].delete() ); // in case file is deleted by another process, check exists again.
                if (!success) {
                    throw new IllegalArgumentException("unable to delete file " + children[i]);
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
     * @throws IllegalArgumentException if it is unable to delete a file
     * @return true if the operation was successful.
     */
    public static boolean deleteWithinFileTree(File root,String name) throws IllegalArgumentException {
        if (!root.exists()) {
            return true;
        }
        File[] children = root.listFiles();
        boolean success = true;
        for (int i = 0; i < children.length; i++) {
            if (children[i].isDirectory()) {
                success = success && deleteWithinFileTree(children[i],name);
            } else {
                if ( children[i].getName().equals(name) ) {
                    success = success && ( !children[i].exists() || children[i].delete() );
                    if (!success) {
                        throw new IllegalArgumentException("unable to delete file " + children[i]);
                    }
                }
            }
        }
        return success;
    }
    
    /**
     * find a files with the given name within the given root, just as "find . -name name -print \;" would.
     * TODO: check links.  For example, find( "/usr/share/fonts/truetype", "FreeMono.ttf" )
     * @throws IllegalArgumentException if the root does not exist.
     * @return the File found, or null if it does not exist.
     */
    public static File find(File root,String name) throws IllegalArgumentException {
        if (!root.exists()) {
            throw new IllegalArgumentException("File does not exist:"+root);
        }
        File[] children = root.listFiles();
        for (int i = 0; i < children.length; i++) {
            if (children[i].isDirectory()) {
                File f= find(children[i],name);
                if ( f!=null ) return f;
            } else {
                if ( children[i].getName().equals(name) ) {
                    return children[i];
                }
            }
        }
        return null;
    }    
    
    /**
     * find a files with the given name within one of the given roots.
     * TODO: check links.  For example, find( "/usr/share/fonts/truetype", "FreeMono.ttf" )
     * This allows root folders that do not exist.
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
        if ( matches==null ) matches= new ArrayList();
        File[] children = root.listFiles();
        for (int i = 0; i < children.length; i++) {
            if (children[i].isDirectory()) {
                listRecursively(children[i],name,matches);
            } else {
                if ( name.matcher( children[i].getName() ).matches() ) {
                    matches.add(children[i]);
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
        if ( src.isDirectory() && ( !dst.exists() || dst.isDirectory() ) ) {
            if ( !dst.exists() ) {
                if ( !dst.mkdirs() ) throw new IOException("unable to mkdir " + dst);
            }
            File dst1= new File( dst, src.getName() );
            if ( !dst1.exists() && !dst1.mkdir() ) throw new IOException("unable to mkdir " + dst1);
            dst= dst1;
            File[] files= src.listFiles();
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
        ic.transferTo(0, ic.size(), oc);
        ic.close();
        oc.close();
    }
    
    /**
     * return the first four bytes of the file as a string.
     * @param file
     * @return a four byte string
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
