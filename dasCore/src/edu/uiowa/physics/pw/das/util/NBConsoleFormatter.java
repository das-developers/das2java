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
    boolean coalesce=true;
    String lastMessage=null;
    int coalesceHits=0;
    
    public String format( LogRecord rec ) {
        if ( coalesce && lastMessage!=null && lastMessage.equals(rec.getMessage()) ) {
            coalesceHits++;
            return "";
        } else {
            StackTraceElement[] st= new Throwable().getStackTrace();
            String result= rec.getLoggerName()+"\n"+rec.getLevel().getLocalizedName()+": "+rec.getMessage()+"\n\tat "+st[7]+"\n\tat "+st[8]+"\n";
            if ( coalesceHits>0 ) {
                result= "(Last message repeats "+(coalesceHits+1)+" times)\n"+result;
            }
            coalesceHits= 0;
            lastMessage= rec.getMessage();
            return result;
        }
    }
        
    public NBConsoleFormatter() {
    }
    
}
