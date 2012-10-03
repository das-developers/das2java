/*
 * LoggerId.java
 *
 * Created on June 6, 2005, 12:24 PM
 */

package org.das2.system;

import java.util.logging.*;
import org.das2.util.LoggerManager;

/**
 *
 * @author Jeremy
 */
public class LoggerId {
    private String name;
    private Logger logger;
    public LoggerId( String name ) { // public to change to protected after DasApplication.getLogger factored out
        this.name= name;
        if ( name.length()==0 ) { // don't register the default logger.
            this.logger= Logger.getLogger(name);
        } else {
            this.logger= LoggerManager.getLogger(name);
        }
        this.logger.log( Level.FINE, "{0} logging at {1}", new Object[]{name, this.logger.getLevel()});
    }
    public String toString() {
        return this.name;
    }
    Logger getLogger() {
        return this.logger;
    }
}
