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
import java.nio.channels.FileChannel;
import java.util.Set;

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
     * deletes all files with the given name, and root, just as "find , -name name -exec rm {} \;" would.
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
}
