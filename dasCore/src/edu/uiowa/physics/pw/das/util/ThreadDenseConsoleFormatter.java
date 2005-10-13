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
public class ThreadDenseConsoleFormatter extends Formatter {
    
    public String format( LogRecord rec ) {
        Thread t= Thread.currentThread();
        StackTraceElement[] st= new Throwable().getStackTrace();        
//        return rec.getLoggerName()+": "+t+": "+rec.getLevel().getLocalizedName()+": "+rec.getMessage()+"\n"+"\tat "+st[7]+"\n\tat "+st[8]+"\n";
        return rec.getLoggerName()+": "+t+": "+rec.getLevel().getLocalizedName()+": "+rec.getMessage()+"\n";
    }
        
    public ThreadDenseConsoleFormatter() {
    }
    
}
