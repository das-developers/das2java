/*
 * NBConsoleFormatter.java
 *
 * Created on April 7, 2005, 12:13 PM
 */

package edu.uiowa.physics.pw.das.util;

import java.text.DecimalFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 *
 * @author Jeremy
 */
public class TimerConsoleFormatter extends Formatter {
    
    long t0;
    DecimalFormat nf;
    String lastLoggerName;
    
    public String format( LogRecord rec ) {
        StackTraceElement[] st= new Throwable().getStackTrace();
        if ( lastLoggerName==null || lastLoggerName!=rec.getLoggerName() ) {
            lastLoggerName= rec.getLoggerName();
            t0=System.currentTimeMillis();
        }

        String source= String.valueOf( st[8].getClassName() );
        if ( source.startsWith("edu.uiowa.physics.pw.das" ) ) {
            String spaces= "                                  ";            
            source= source.substring("edu.uiowa.physics.pw.das".length());
            if ( source.length() < spaces.length() ) {
                source= spaces.substring(source.length()) + source;
            }
        }

        long t= System.currentTimeMillis() - t0;
        return nf.format(t) + ":" + rec.getLoggerName()+": "+ source + ": " + rec.getLevel().getLocalizedName()+": "+rec.getMessage()+"\n";
    }
        
    public TimerConsoleFormatter() {
        t0= System.currentTimeMillis();
        nf= new DecimalFormat("00000");
    }
    
}
