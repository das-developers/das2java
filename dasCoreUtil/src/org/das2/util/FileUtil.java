/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util;

import java.io.File;

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
     * @throws IllegalArgumentException if it is unable to delete a file
     * @return true if the operation was successful.
     */
    public static boolean deleteFileTree(File root) throws IllegalArgumentException {
        if (!root.exists()) {
            return true;
        }
        File[] children = root.listFiles();
        boolean success = true;
        for (int i = 0; i < children.length; i++) {
            if (children[i].isDirectory()) {
                success = success && deleteFileTree(children[i]);
            } else {
                success = success && children[i].delete();
                if (!success) {
                    throw new IllegalArgumentException("unable to delete file " + children[i]);
                }
            }
        }
        success = success && (!root.exists() || root.delete());
        if (!success) {
            throw new IllegalArgumentException("unable to delete folder " + root);
        }
        return success;
    }

    /**
     * deletes all files with the given name, and root, just as "find , -name name -exec rm {} \;" would.
     * TODO: check links
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
                    success = success && children[i].delete();
                    if (!success) {
                        throw new IllegalArgumentException("unable to delete file " + children[i]);
                    }
                }
            }
        }
        return success;
    }
}
