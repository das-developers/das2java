/*
 * DasLogger.java
 *
 * Created on June 6, 2005, 12:12 PM
 */

package edu.uiowa.physics.pw.das.system;

import java.io.*;
import java.util.logging.*;

/**
 *
 * @author Jeremy
 */
public class DasLogger {
    
    {
        try {
            java.net.URL logConfigURL;
            
            File local;
            if ( System.getProperty("user.name").equals("Web") ) {
                local= new File("/tmp");
            } else {
                local= new File( System.getProperty("user.home") );
            }
            
            File userDirectory=new File( local, ".das2" );
            File localLogConfig= new File( userDirectory, "logging.properties" );
            
            if ( localLogConfig.exists() ) {
                Logger.getLogger("").info("using "+localLogConfig);
                logConfigURL= localLogConfig.toURL();
            } else {
                logConfigURL= DasLogger.class.getResource("logging.properties");
            }
            System.err.println( "reading log configuration from "+logConfigURL );
            LogManager.getLogManager().readConfiguration( logConfigURL.openStream() );
        } catch ( Exception e ) {
            System.out.println(e);
        }
    }
    
    /* messages having to do with the application-specific Das 2 Application */
    public static final LoggerId APPLICATION_LOG= new LoggerId( "" );
    
    /* system messages such as RequestProcessor activity */
    public static final LoggerId SYSTEM_LOG= new LoggerId( "das2.system" );
    
    /* events, gestures, user feedback */
    public static final LoggerId GUI_LOG= new LoggerId( "das2.gui" );
    
    /* renders, drawing */
    public static final LoggerId GRAPHICS_LOG= new LoggerId( "das2.graphics" );
    
    /* rebinning */
    public static final LoggerId DATA_OPERATIONS_LOG= new LoggerId( "das2.dataOperations" );
    
    /* internet transactions, file I/O */
    public static final LoggerId DATA_TRANSFER_LOG= new LoggerId( "das2.dataTransfer" );
    
    /**
     * logger for messages to end users
     */
    public static Logger getLogger() {
        return APPLICATION_LOG.getLogger();
    }
    
    public static Logger getLogger( LoggerId loggerId ) {
        return loggerId.getLogger();
    }
    
    /**
     * logger for messages to developers
     */
    public synchronized Logger getDebugLogger() {
        return Logger.getLogger("debug");
    }
    
}
