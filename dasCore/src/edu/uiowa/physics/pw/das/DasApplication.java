/* File: DasApplication.java
 * Copyright (C) 2002-2003 The University of Iowa
 * Created by: Jeremy Faden <jbf@space.physics.uiowa.edu>
 *             Jessica Swanner <jessica@space.physics.uiowa.edu>
 *             Edward E. West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.uiowa.physics.pw.das;

import java.awt.event.*;
import java.util.logging.*;
import java.util.prefs.*;
import javax.swing.*;

/**
 *
 * @author  Edward West
 */
public class DasApplication {
    
    private static final DasApplication DEFAULT = new DasApplication();
    
    private static class LoggerId {
        String name;
        Logger logger;
        LoggerId( String name ) {
            this.name= name;
            this.logger= Logger.getLogger(name);
            this.logger.setLevel(Level.WARNING);
            this.logger.log( this.logger.getLevel(), name +" logging at "+this.logger.getLevel() );
        }
        public String toString() {
            return this.name;
        }
        public Logger getLogger() {
            return this.logger;
        }
    }
    
    /* messages having to do with the application-specific Das 2 Application */
    public static final LoggerId APPLICATION_LOG= new LoggerId(""); 
    
    /* events, gestures, user feedback */
    public static final LoggerId GUI_LOG= new LoggerId("gui");
    
    /* renders, drawing */
    public static final LoggerId GRAPHICS_LOG= new LoggerId("graphics");
    
    /* rebinning */
    public static final LoggerId DATA_OPERATIONS_LOG= new LoggerId("data operations");
    
    /* internet transactions, file I/O */
    public static final LoggerId DATA_TRANSFER_LOG= new LoggerId("data transfer");
    
    static {
        String[] beanInfoSearchPath = { "edu.uiowa.physics.pw.das.beans" };
        java.beans.Introspector.setBeanInfoSearchPath(beanInfoSearchPath);
    }
    
    private NameContext nameContext;
    private Logger debugLogger;
    
    /** Creates a new instance of DasApplication */
    private DasApplication() {
        nameContext = new NameContext();        
    }
    
    public NameContext getNameContext() {
        return nameContext;
    }
    
    public static DasApplication getDefaultApplication() {
        return DEFAULT;
    }
        
    private boolean headless= false;
    
    private static boolean isApplet() {
        return false;
    }
        
    private static boolean isX11() {
        String osName= System.getProperty( "os.name" );
        return "SunOS".equals( osName ) 
         || "Linux".equals( osName );
    }
    
    public static boolean isHeadAvailable() {
        return true;
        /*
        return ( System.getProperty( "awt.toolkit" ) != null );
        /*
               //boolean headAvailable= !java.awt.GraphicsEnvironment.isHeadless();       
        boolean result= false;
        if ( isApplet() ) result= true;
        getDefaultApplication().getLogger().fine( System.getProperty( "os.name" ) );
        String osName= System.getProperty( "os.name" );
        if ( "Windows".equals( osName ) ) {
            result= true;
        } else if ( "Windows XP".equals( osName ) ) {
            result= true;
        } else if ( isX11() ) {
            String DISPLAY= System.getProperty( "DISPLAY" );
            getDefaultApplication().getLogger().fine( System.getProperty( "DISPLAY" ) );
            if ( "".equals(DISPLAY) ) {
                result= false;
            } else {
                result= true;
            }                
        }
        return result;
         */
    }
    
    public boolean isHeadless() {        
/*        if ( !headAvailable() && !"true".equals(System.getProperty("headless")) ) {
            getLogger().info("setting headless to true");
            setHeadless( true );
        } */
        return "true".equals(System.getProperty("java.awt.headless"));
    }
    
    public void setHeadless( boolean headless ) {
        if ( headless ) {
            System.setProperty("java.awt.headless","true");
        } else {
            if ( ! isHeadAvailable() ) {
                throw new IllegalArgumentException( "attempt to unset headless when environment is headless." );
            }                 
            System.setProperty("java.awt.headless","false");
        }
    }
    
    public JFrame getMainFrame( java.awt.Container canvas ) {
        JFrame frame= getMainFrame();
        frame.setContentPane(canvas);
        frame.pack();
        frame.setVisible(true);
        return frame;
    }
    
    public JFrame getMainFrame() {
        final JFrame result= new JFrame("Das2");        
        final Preferences prefs= Preferences.userNodeForPackage(DasApplication.class);
               
        int xlocation= prefs.getInt( "xlocation", 20 );
        int ylocation= prefs.getInt( "ylocation", 20 );
        result.setLocation(xlocation, ylocation);
        result.addWindowListener( new WindowAdapter() {
            public void windowClosing( WindowEvent e ) {
                prefs.putInt( "xlocation", result.getLocation().x );
                prefs.putInt( "ylocation", result.getLocation().y );
                System.out.println("bye!"+result.getLocation());
            }
        } );
        result.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        return result;
    }
    
    /**
     * logger for messages to end users
     */    
    public Logger getLogger() {
        return DasProperties.getLogger();
    }
    
    public Logger getLogger( LoggerId loggerId ) {
        return loggerId.getLogger();
    }
    
    /**
     * logger for messages to developers
     */    
    public synchronized Logger getDebugLogger() {
        return Logger.getLogger("debug");
    }
}
