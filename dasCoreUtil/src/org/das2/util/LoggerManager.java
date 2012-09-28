/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Central place that keeps track of loggers.
 * @author jbf
 */
public final class LoggerManager {

    private static Set loggers= new HashSet();
    private static Map<String,Logger> log= new HashMap();

    /**
     * return the requested logger, but add it to the list of known loggers.
     * @param id
     * @return
     */
    public synchronized static Logger getLogger( String id ) {
        Logger result= log.get(id);
        if ( result!=null ) {
            return result;
        } else {
            result= Logger.getLogger(id);
            log.put( id, result );
        }
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
