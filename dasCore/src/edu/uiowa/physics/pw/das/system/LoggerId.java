/*
 * LoggerId.java
 *
 * Created on June 6, 2005, 12:24 PM
 */

package edu.uiowa.physics.pw.das.system;

import java.util.logging.*;

/**
 *
 * @author Jeremy
 */
public class LoggerId {
    private String name;
    private Logger logger;
    public LoggerId( String name ) { // public to change to protected after DasApplication.getLogger factored out
        this.name= name;
        this.logger= Logger.getLogger(name);
        this.logger.fine( name +" logging at "+this.logger.getLevel() );
    }
    public String toString() {
        return this.name;
    }
    Logger getLogger() {
        return this.logger;
    }
}
