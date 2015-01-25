package org.das2.util;

import java.text.MessageFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Print the elapsed time and logger name with each message.
 * @author jbf
 */
public class TimingConsoleFormatter extends Formatter {
    
    long t0= System.currentTimeMillis();
    
    @Override
    public String format( LogRecord rec ) {
        long dt= System.currentTimeMillis() - t0;
        String msg= MessageFormat.format( String.valueOf( rec.getMessage() ), rec.getParameters() );
        return String.format("%9.3f %s: %s\n", dt/1000., rec.getLoggerName(), msg );
    }
        
    public TimingConsoleFormatter() {
    }
    
}
