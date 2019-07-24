/*
 * NBConsoleFormatter.java
 *
 * Created on April 7, 2005, 12:13 PM
 */
package org.das2.util;

import java.text.DecimalFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 *
 * @author Jeremy
 */
public class TimerConsoleFormatter extends Formatter {

    long t0= System.currentTimeMillis();
    DecimalFormat nf;
    String lastLoggerName;
    String resetMessage;

    private final static String SPACES = "                                  ";
    
    @Override
    public String format(LogRecord rec) {
        
        final String message = rec.getMessage();
        //if ( lastLoggerName==null || lastLoggerName!=rec.getLoggerName() ) {
        if (message != null && resetMessage!=null && message.contains(resetMessage)) {
            //lastLoggerName = rec.getLoggerName();
            t0 = System.currentTimeMillis();
        }

        String source = "???";
        
        StackTraceElement[] st = new Throwable().getStackTrace();
        if (st.length > 8) {
            int idx= 5;
            while ( idx<8 && st[idx].getClassName().contains("java.util.logging.Logger") ) idx++;
            source = String.valueOf(st[idx].getClassName());
            if (source.startsWith("org.das2")) {
                source = source.substring("org.das2".length());
                if (source.length() < SPACES.length()) {
                    source = SPACES.substring(source.length()) + source;
                }
            }
        }

        long t = System.currentTimeMillis() - t0;
        
        String threadId= Thread.currentThread().getName();
        threadId = fixedColumn(threadId, 20);
        
        return nf.format(t) + ":" + fixedColumn(rec.getLoggerName(),20) + ": " + source + ": " + threadId+":" + rec.getLevel().getLocalizedName() + ": " + String.valueOf(message) + "\n";
    }

    public void setResetMessage( String msg ) {
        this.resetMessage= msg;
    }
    
    public TimerConsoleFormatter() {
        t0 = System.currentTimeMillis();
        nf = new DecimalFormat("00000");
    }

    String spaces= "                                                               ";
    private String fixedColumn(String threadId, int sp) {
        try {
        if (threadId.length() > sp) threadId = threadId.substring(threadId.length()-sp, threadId.length());
        if (threadId.length() < sp) threadId = SPACES.substring(0, sp - threadId.length()) + threadId;
        return threadId;
        } catch ( StringIndexOutOfBoundsException ex ) {
            return threadId;
        }
    }
}
