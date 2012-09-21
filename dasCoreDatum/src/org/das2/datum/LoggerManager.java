/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.datum;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Central place that keeps track of loggers.
 * @author jbf
 */
public final class LoggerManager {

    private static Set loggers= new HashSet();

    /**
     * return the requested logger, but add it to the list of known loggers.
     * @param id
     * @return
     */
    public static Logger getLogger( String id ) {
        loggers.add(id);
        return Logger.getLogger(id);
    }

    /**
     * return the list of known loggers.
     * @return
     */
    public static Set getLoggers() {
        return loggers;
    }
}
