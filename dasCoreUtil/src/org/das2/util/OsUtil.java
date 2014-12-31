/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;

/**
 * Utility methods for operating system functions.
 * @see org.das2.util.filesystem.FileSystemUtil
 * @author jbf
 */
public class OsUtil {
    
    /**
     * return the processID (pid), or the fallback if the pid cannot be found.
     * @param fallback the string (null is okay) to return when the pid cannot be found.
     * @return the process id or the fallback provided by the caller.
     */
    public static String getProcessId(final String fallback) {
        // Note: may fail in some JVM implementations
        // therefore fallback has to be provided

        // something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
        final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        final int index = jvmName.indexOf('@');

        if (index < 1) {
            // part before '@' empty (index = 0) / '@' not found (index = -1)
            return fallback;
        }

        try {
            return Long.toString(Long.parseLong(jvmName.substring(0, index)));
        } catch (NumberFormatException e) {
            // ignore
        }
        return fallback;
    }    
    
    /**
     * Unconditionally close an <code>Reader</code>.
     * <p>
     * Equivalent to {@link Reader#close()}, except any exceptions will be ignored.
     * This is typically used in finally blocks.
     * 
     * From apache commons. http://grepcode.com/file_/repo1.maven.org/maven2/commons-io/commons-io/1.2/org/apache/commons/io/IOUtils.java/?v=source
     *
     * @param input  the Reader to close, may be null or already closed
     */
    public static void closeQuietly(InputStream input) {
        try {
            if (input != null) {
                input.close();
            }
        } catch (IOException ioe) {
            // ignore
        }
    }    
    /**
     * Compare the contents of two Streams to determine if they are equal or
     * not.
     * <p>
     * This method buffers the input internally using
     * <code>BufferedInputStream</code> if they are not already buffered.
     *
     * @param input1  the first stream
     * @param input2  the second stream
     * @return true if the content of the streams are equal or they both don't
     * exist, false otherwise
     * @throws NullPointerException if either input is null
     * @throws IOException if an I/O error occurs
     * 
     * From Apache Commons http://grepcode.com/file_/repo1.maven.org/maven2/commons-io/commons-io/1.2/org/apache/commons/io/IOUtils.java/?v=source
     */
    public static boolean contentEquals(InputStream input1, InputStream input2)
            throws IOException {
        if (!(input1 instanceof BufferedInputStream)) {
            input1 = new BufferedInputStream(input1);
        }
        if (!(input2 instanceof BufferedInputStream)) {
            input2 = new BufferedInputStream(input2);
        }

        int ch = input1.read();
        while (-1 != ch) {
            int ch2 = input2.read();
            if (ch != ch2) {
                return false;
            }
            ch = input1.read();
        }

        int ch2 = input2.read();
        return (ch2 == -1);
    }    
 
        /**
     * Compare the contents of two files to determine if they are equal or not.
     * <p>
     * This method checks to see if the two files are different lengths
     * or if they point to the same file, before resorting to byte-by-byte
     * comparison of the contents.
     * <p>
     * Code origin: Avalon
     *
     * @param file1  the first file
     * @param file2  the second file
     * @return true if the content of the files are equal or they both don't
     * exist, false otherwise
     * @throws IOException in case of an I/O error
     * 
     * From Apache Commons http://grepcode.com/file/repo1.maven.org/maven2/commons-io/commons-io/1.2/org/apache/commons/io/FileUtils.java
     */
    public static boolean contentEquals(File file1, File file2) throws IOException {
        boolean file1Exists = file1.exists();
        if (file1Exists != file2.exists()) {
            return false;
        }

        if (!file1Exists) {
            // two not existing files are equal
            return true;
        }

        if (file1.isDirectory() || file2.isDirectory()) {
            // don't want to compare directory contents
            throw new IOException("Can't compare directories, only files");
        }

        if (file1.length() != file2.length()) {
            // lengths differ, cannot be equal
            return false;
        }

        if (file1.getCanonicalFile().equals(file2.getCanonicalFile())) {
            // same file
            return true;
        }

        InputStream input1 = null;
        InputStream input2 = null;
        try {
            input1 = new FileInputStream(file1);
            input2 = new FileInputStream(file2);
            return contentEquals(input1, input2);

        } finally {
            closeQuietly(input1);
            closeQuietly(input2);
        }
    }

}
    
