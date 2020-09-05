package org.das2.util;

import java.text.MessageFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Print the elapsed time and logger name with each message.
 * @author jbf
 */
public class TimingConsoleFormatter extends Formatter {
    
    long t0= System.currentTimeMillis();
    boolean haveReportedTime= false;
    
    @Override
    public String format( LogRecord rec ) {
        long dt= System.currentTimeMillis() - t0;
        Object[] parms= rec.getParameters();

        // the following copied from Autoplot's org.virbo.autoplot.scriptconsole.LogConsole
        String recMsg;
        String rm1= rec.getMessage();
        if ( rm1!=null ) {
            if ( rm1.length()>1 ) {
                char c= rm1.charAt(0);
                if ( c=='E' ) {
                    if ( rm1.equals("ENTRY {0}") ) {
                        recMsg= "ENTRY "+ rec.getSourceClassName() + "." +rec.getSourceMethodName() + " {0}";
                    } else if ( rm1.equals("ENTRY") ) {
                        recMsg= "ENTRY "+ rec.getSourceClassName() + "." +rec.getSourceMethodName();
                    } else {
                        recMsg= rm1;
                    }
                } else if ( c=='R' ) {
                    if ( rm1.equals("RETURN {0}") ) {
                        recMsg= "RETURN "+ rec.getSourceClassName() + "." +rec.getSourceMethodName() + " {0}";
                    } else if ( rm1.equals("RETURN") ) {
                        recMsg= "RETURN "+ rec.getSourceClassName() + "." +rec.getSourceMethodName();
                    } else {
                        recMsg= rm1;
                    }
                } else {
                    recMsg = rm1;
                }
            } else {
                recMsg = rec.getMessage();
            }
        } else {
            recMsg= null;
        }

        if ( parms!=null && parms.length>0 ) {
            try {
                recMsg = MessageFormat.format( recMsg, parms );
            } catch ( NullPointerException ex ) {
                recMsg= String.valueOf( rec.getMessage() ); //TODO: fix this log message! bug https://sourceforge.net/p/autoplot/bugs/1194/
            }
        }
        if ( ( recMsg==null || recMsg.length()==0 ) && rec.getThrown()!=null ) {
            recMsg= rec.getThrown().toString();
            //TODO: consider if "debug" property should be set instead.  Also it would be nice to digest this for jython errors.
            rec.getThrown().printStackTrace();
            // This is interesting--I've wondered where the single-line-message items have been coming from...
        } else {
            // no message.  breakpoint here for debugging.
            int i=0;
        }
        String st0= "";
        if ( !haveReportedTime ) {
            st0= "#logging started at "+new Date(t0).toGMTString()+"\n";
            haveReportedTime= true;
        }
        return String.format( Locale.US, "%s%9.3f %s: %s\n", st0, dt/1000., rec.getLoggerName(), recMsg );
    }
        
    public TimingConsoleFormatter() {
    }
    
}
