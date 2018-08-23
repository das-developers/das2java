
package org.das2.datum;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Central place that keeps track of loggers.  Note that both org.das2.util 
 * and org.das2.datum have this same class, which is there to avoid coupling between the 
 * packages.
 * @author jbf
 */
public final class LoggerManager {

    private static final Set<String> loggers= new HashSet();
    private static final Map<String,Logger> log= new HashMap();

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
    public static Set<String> getLoggers() {
        return loggers;
    }
}
