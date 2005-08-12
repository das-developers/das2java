/*
 * NBConsoleFormatter.java
 *
 * Created on April 7, 2005, 12:13 PM
 */

package edu.uiowa.physics.pw.das.util;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 *
 * @author Jeremy
 */
public class DenseConsoleFormatter extends Formatter {
    
    public String format( LogRecord rec ) {
        StackTraceElement[] st= new Throwable().getStackTrace();
        return rec.getLoggerName()+": "+rec.getLevel().getLocalizedName()+": "+rec.getMessage()+"\n";
    }
        
    public DenseConsoleFormatter() {
    }
    
}
