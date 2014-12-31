/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.util;

import java.lang.management.ManagementFactory;

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
}
