/*
 * DasLogger.java
 *
 * Created on June 6, 2005, 12:12 PM
 */

package org.das2.system;

import org.das2.DasApplication;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.logging.*;

/**
 *
 * @author Jeremy
 */
public class DasLogger {
    public static final boolean DISABLE_RELOAD= true;

    public static void reload() throws IOException {
        try {

            if ( DISABLE_RELOAD ) return;

            java.net.URL logConfigURL;
            
            File local;
            if ( DasApplication.getProperty("user.name","applet").equals("Web") ) {
                local= new File("/tmp");
            } else if ( DasApplication.getProperty("user.name","applet").equals("applet") ) {
                return;
            } else {
                local= new File( DasApplication.getProperty("user.home","applet") );
            }
            
            File userDirectory=new File( local, ".das2" );
            File localLogConfig= new File( userDirectory, "logging.properties" );
            
            if ( localLogConfig.exists() ) {
                Logger.getLogger("").info("using "+localLogConfig);
                logConfigURL= localLogConfig.toURI().toURL();
            } else {
                logConfigURL= DasLogger.class.getResource("logging.properties");
            }
            if ( logConfigURL==null ) {
                System.err.println("unable to locate logging properties file logging.properties, using defaults");
            } else {
                //dumpUrl(logConfigURL);
                InputStream in=  logConfigURL.openStream();
                LogManager.getLogManager().readConfiguration( in );
                in.close();
                //System.err.println( "read log configuration from "+logConfigURL );
                //printStatus();
            }
        } catch ( MalformedURLException e ) {
            throw new RuntimeException(e); // this shouldn't happen
        }
    }
    
    private static void dumpUrl( URL url ) throws IOException {
        BufferedReader reader= new BufferedReader( new InputStreamReader( url.openStream() ) );
        String s= reader.readLine();
        while( s!=null ) {
            System.out.println(s);
            s= reader.readLine();
        }
        reader.close();
        
    }
    
    public static void printStatus() {
        String[] loggers= new String[] { "",  "das2.system", "das2.gui",  "das2.graphics",  "das2.dataOperations", "das2.dataTransfer", };
        for ( int i=0; i<loggers.length; i++ ) {
            Logger logger= Logger.getLogger(loggers[i]);
            Level l= logger.getLevel();
            while ( l==null ) {
                logger= logger.getParent();
                l=logger.getLevel();
            }
            System.err.println( loggers[i]+" logging at "+l );
        }
    }
    
    static {
        try {
            reload();
        } catch ( Exception e ) {
            System.out.println(e);
        }
    }
    
    /**
     * messages having to do with the application-specific Das 2 Application
     */
    public static final LoggerId APPLICATION_LOG= new LoggerId( "" );
    
    /**
     * system messages such as RequestProcessor activity
     */
    public static final LoggerId SYSTEM_LOG= new LoggerId( "das2.system" );
    
    /**
     * events, gestures, user feedback
     */
    public static final LoggerId GUI_LOG= new LoggerId( "das2.gui" );
    
    /**
     * renders, drawing
     */
    public static final LoggerId GRAPHICS_LOG= new LoggerId( "das2.graphics" );
    
    /**
     * renderer's logger
     */
    public static final LoggerId RENDERER_LOG= new LoggerId( "das2.graphics.renderer" );
    
    /**
     * rebinning  and dataset operators
     */
    public static final LoggerId DATA_OPERATIONS_LOG= new LoggerId( "das2.dataOperations" );
    
    /**
     * internet transactions, file I/O
     */
    public static final LoggerId DATA_TRANSFER_LOG= new LoggerId( "das2.dataTransfer" );
    
    /**
     * virtual file system activities
     */
    public static final LoggerId FILESYSTEM_LOG= new LoggerId( "das2.filesystem" );
    
    /**
     * das2 application description files
     */
    public static final LoggerId DASML_LOG= new LoggerId( "das2.dasml" );
    
    /**
     * logger for messages to end users
     */
    public static Logger getLogger() {
        return APPLICATION_LOG.getLogger();
    }
    
    public static Logger getLogger( LoggerId loggerId ) {
        return loggerId.getLogger();
    }
    
    public static Logger getLogger( LoggerId loggerId, String identifier ) {
        String id= loggerId.toString()+"."+identifier;
        return Logger.getLogger(id);
    }

    /**
     * add the one handler to all...
     * @param name 
     */
    public static void setUpHandler(String name) {
        removeAllHandlers();
        Handler h= null;
        if ( name.equals("mini") ) {
            h= new Handler() {
                @Override
                public void publish(LogRecord record) {
                    if ( record.getMessage()!=null ) {
                        System.err.println( MessageFormat.format( record.getMessage(), record.getParameters() ) + " "+record.getLevel() );
                    } else if ( record.getThrown()!=null ) {
                        System.err.println( record.getThrown() );
                    } else {
                        System.err.println( "log record with no message: " + record );
                    }
                }
                public void flush() { }
                public void close() throws SecurityException {  }
            };
        } else {
            throw new IllegalArgumentException("name must be mini, but feel free to add more");
        }
        addHandlerToAll(h);
    }
    
    /**
     * logger for messages to developers
     */
    public synchronized Logger getDebugLogger() {
        return Logger.getLogger("debug");
    }
    
    public static void removeAllHandlers() {
        LoggerId[] loggers= new LoggerId[] { APPLICATION_LOG, SYSTEM_LOG, GUI_LOG, GRAPHICS_LOG, RENDERER_LOG, FILESYSTEM_LOG, DATA_TRANSFER_LOG, DATA_OPERATIONS_LOG, DASML_LOG };
        for ( int i=0; i<loggers.length; i++ ) {
            Logger logger= loggers[i].getLogger();
            Handler[] hh= logger.getHandlers();
            for ( Handler h1: hh ) {
                logger.removeHandler(h1);
            }
        }        
    }
    
    public static void addHandlerToAll( Handler h ) {
        LoggerId[] loggers= new LoggerId[] { APPLICATION_LOG, SYSTEM_LOG, GUI_LOG, GRAPHICS_LOG, RENDERER_LOG, FILESYSTEM_LOG, DATA_TRANSFER_LOG, DATA_OPERATIONS_LOG, DASML_LOG };
        for ( int i=0; i<loggers.length; i++ ) {
            Logger logger= loggers[i].getLogger();
            Handler[] hh= logger.getHandlers();
            for ( Handler h1: hh ) {
                if ( h.getClass().isInstance(h1) ) logger.removeHandler(h1);
            }
            logger.addHandler(h);
        }
    }
}
