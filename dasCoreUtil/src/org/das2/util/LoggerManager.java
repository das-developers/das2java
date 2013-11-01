/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central place that keeps track of loggers.  Note that both org.das.datum 
 * and org.das2.datum have this same class, which is there to avoid coupling between the 
 * packages.
 * @author jbf
 */
public final class LoggerManager {

    private static Set<String> loggers= new HashSet();
    private static Map<String,Logger> log= new HashMap();
    private static Set<Handler> extraHandlers= new HashSet();

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
            for ( Handler h: extraHandlers ) {
                result.addHandler(h);
            }
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
    
    /**
     * Add the handler to all loggers created by this manager, and all
     * future.  Only call this once!  I would think that adding a handler to 
     * the root would essentially add the handler to all loggers, but it doesn't 
     * seem to. 
     * 
     * @param handler e.g. GraphicalLogHandler
     */
    public static void addHandlerToAll( Handler handler ) {
        for ( String l: loggers ) {
            log.get(l).addHandler(handler);
        }
        extraHandlers.add(handler);
    }
    
    public static void main( String[] args ) {
        Logger l= LoggerManager.getLogger("test");
        Exception e= new java.lang.Exception("this is the problem") ;
        l.log( Level.WARNING, null, e ); // BUG 1119 DEMONSTRATION PURPOSES
        l.log( Level.WARNING, e.getMessage(), e );
        l.log( Level.WARNING, "Exception: {0}", e );
        l.log( Level.INFO, "hello there..." );
    }
}
