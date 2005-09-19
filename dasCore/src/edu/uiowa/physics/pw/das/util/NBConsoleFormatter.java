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
public class NBConsoleFormatter extends Formatter {
    
    public String format( LogRecord rec ) {
        StackTraceElement[] st= new Throwable().getStackTrace();
        String result= rec.getLoggerName()+"\n"+rec.getLevel().getLocalizedName()+": "+rec.getMessage()+"\n\tat "+st[7]+"\n\tat "+st[8]+"\n";
        return result;
    }
        
    public NBConsoleFormatter() {
    }
    
}
