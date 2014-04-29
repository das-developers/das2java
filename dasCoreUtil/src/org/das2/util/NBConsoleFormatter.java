/*
 * NBConsoleFormatter.java
 *
 * Created on April 7, 2005, 12:13 PM
 */

package org.das2.util;

import java.text.MessageFormat;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Formats log messages to the stdout, so that Netbeans will automatically
 * make links from the stack traces printed to stderr.
 * @author Jeremy
 */
public class NBConsoleFormatter extends Formatter {
    boolean coalesce=true;
    String lastMessage=null;
    int coalesceHits=0;
    
    public String format( LogRecord rec ) {
        //if ( rec.getLevel()==Level.WARNING &&rec.getMessage()==null ) {
            //System.err.println("Here");
        //}
        if ( coalesce && lastMessage!=null && lastMessage.equals(rec.getMessage()) ) {
            coalesceHits++;
            return "";
        } else {
            StackTraceElement[] st= new Throwable().getStackTrace();
            //String msg= Message.fo
            int len= Math.min( 9, st.length );
            String msg= MessageFormat.format( String.valueOf( rec.getMessage() ), rec.getParameters() );
            String result= rec.getLoggerName()+"  ["+Thread.currentThread().getName()+"]\n"+rec.getLevel().getLocalizedName()+": "+msg;
            if ( len>2 ) {
                result= result
                    +( "\n\tat "+st[len-2] )
                    +( "\n\tat "+st[len-1]+"\n" );
            }
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
