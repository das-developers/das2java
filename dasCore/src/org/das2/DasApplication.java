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

package org.das2;

import org.das2.dataset.LimitSizeBytesDataSetCache;
import org.das2.dataset.NullDataSetCache;
import org.das2.util.DefaultExceptionHandler;
import org.das2.system.DasLogger;
import org.das2.util.ExceptionHandler;
import org.das2.system.LoggerId;
import org.das2.system.NullMonitorFactory;
import org.das2.system.MonitorFactory;
import org.das2.system.DefaultMonitorFactory;
import org.das2.util.ThrowRuntimeExceptionHandler;
import org.das2.util.Splash;
import org.das2.util.DasExceptionHandler;
import org.das2.util.ClassMap;
import org.das2.client.InputStreamMeter;
import org.das2.dataset.DataSetCache;
import org.das2.graph.DasAnnotation;
import org.das2.graph.DasAxis;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasCanvasComponent;
import org.das2.graph.DasColorBar;
import org.das2.graph.DasColumn;
import org.das2.graph.DasPlot;
import org.das2.graph.DasRow;
import java.awt.event.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.*;
import java.util.prefs.*;
import javax.swing.*;
import org.das2.util.filesystem.FileSystemSettings;

/**
 * DasApplication object manages per-application resources, like object name space, 
 * dataset caching, progress monitoring, exception handling and a network speed limiter.
 *
 * @author  Edward West
 */
public class DasApplication {
    
    private static DasApplication DEFAULT;
    
    private JFrame mainFrame;
    
    /**
     * three-state register for keeping track of is applet: null, TRUE, FALSE
     *   null->try to detect.
     *   TRUE|FALSE->explicit setting by application
     */
    private Boolean applet;
    
    /**
     * three-state register for keeping track of headless: null, TRUE, FALSE
     *   null->try to detect.
     *   TRUE|FALSE->explicit setting by application
     * in headless, we assume non-interactive.  progress monitor factory always returns ProgressMonitor.NULL, dataSetCache is trivial.
     *
     */
    private Boolean headless;  // tristate: null, TRUE, FALSE
    
    private NameContext nameContext;
    
    private DataSetCache dataSetCache;
    
    public Logger getLogger() {
        return DasLogger.getLogger();
    }
    
    /**
     * @deprecated use DasLogger.getLogger( LoggerId )
     * @param id the id.
     * @return the logger.
     */
    public Logger getLogger( LoggerId id ) {
        return DasLogger.getLogger(id);
    }
    
    /** Creates a new instance of DasApplication */
    private DasApplication() {
        nameContext = new NameContext();
        applet= null;
    }
    
    public NameContext getNameContext() {
        return nameContext;
    }
    
    static ClassMap classNameMap= new ClassMap();
    static {
        classNameMap.put( DasPlot.class, "plot" );
        classNameMap.put( DasAxis.class, "axis" );
        classNameMap.put( DasColorBar.class, "colorbar" );
        classNameMap.put( DasRow.class, "row");
        classNameMap.put( DasColumn.class, "column");
        classNameMap.put( DasAnnotation.class, "annotation");
        classNameMap.put( Object.class, "object" );
        classNameMap.put( DasCanvasComponent.class, "canvasComponent" );
        classNameMap.put( DasCanvas.class, "canvas" );
    }
    
    private Map hitsMap= new HashMap();
    
    // note that only CanvasComponents have a name.
    public String suggestNameFor( Object c ) {
        String type= (String)classNameMap.get( c.getClass() );
        Integer hits= (Integer)hitsMap.get(type);
        int ihits;
        if ( hits==null ) {
            ihits=0;
        } else {
            ihits= (hits.intValue())+1;
        }
        hitsMap.put( type, ihits);
        return type+"_"+ihits;
    }
    
    public static DasApplication getDefaultApplication() {
         if ( DEFAULT==null ) {
             DEFAULT= new DasApplication();
         }
        return DEFAULT;
    }
    
    /**
     * nasty, evil method for releasing resources on a server.  DO NOT USE THIS!!!!
     */
    public static void resetDefaultApplication() {
        DEFAULT= null;
    }
    
    
    public final boolean isApplet() {
        if ( applet==null ) {
            return Thread.currentThread().getContextClassLoader().getClass().getName().indexOf("plugin") > -1;
        } else {
            return applet.booleanValue();
        }
    }
    
    /**
     * return true if the application appears to have been launched with Java WebStart.
     * @return true if it appears that Java Webstart was used to launch the application.
     */
    public final boolean isJavaWebStart() {        
        return System.getProperty("javawebstart.version", null) != null;
    }
    
    /**
     * check the security manager to see if all permissions are allowed,
     * True indicates is not an applet running in a sandbox.
     * See FileSystemSettings, which has a copy of this code
     * @return true if all permissions are allowed
     */
    public static boolean hasAllPermission() {
        try {
            if ( restrictPermission==true ) return false;
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(new java.security.AllPermission());  
            }
            return true;
        } catch ( SecurityException ex ) {
            return false;
        }
    }

    private static boolean restrictPermission= false;

    /**
     * true means don't attempt to gain access to applet-restricted functions.
     * @param v true means don't attempt to gain access to applet-restricted functions.
     * @see FileSystemSettings#setRestrictPermission(boolean) 
     */
    public static void setRestrictPermission( boolean v ) {
        restrictPermission= v;
        FileSystemSettings.setRestrictPermission(v);
    }

    /**
     * support restricted security environment by checking permissions before 
     * checking property.
     * @param name
     * @param deft
     * @return
     */
    public static String getProperty( String name, String deft ) {
        try {
            return System.getProperty(name, deft);
        } catch ( SecurityException ex ) {
            return deft;
        }
    }
      
     
    // force the application state to be applet or application
    public void setApplet( boolean applet ) {
        this.applet= Boolean.valueOf(applet);
    }
    
    public void setReloadLoggingProperties( boolean v ) {
        if ( v ) {
            try {
                DasLogger.reload();
                DasLogger.printStatus();
            } catch ( IOException e ) {
                getExceptionHandler().handle(e);
            }
        }
    }
    
    public boolean isReloadLoggingProperties() {
        return false;
    }
    
    private static boolean isX11() {
        String osName= System.getProperty( "os.name" ); // applet okay
        return "SunOS".equals( osName )
        || "Linux".equals( osName );
    }
    
    /**
     * returns the location of the local directory sandbox.  For example,
     * The web filesystem object downloads temporary files to here, logging
     * properties file, etc.
     *
     * Assume that this File is local, so I/O is quick, and that the process
     * has write access to this area.
     * For definition, assume that at least 1Gb of storage is available as
     * well.
     */
    public static File getDas2UserDirectory() {
        File local;
        // for applets, if we are running from a disk, then it is okay to write local files, but we can't check permissions
        if ( DasApplication.getProperty("user.name", "Web").equals("Web") ) {
            local= new File("/tmp");
        } else {
            local= new File( System.getProperty("user.home") );
        }
        local= new File( local, ".das2" );
        return local;
    }
    
    /**
     * return true if this doesn't have to be a headless application.
     * This should not be used.
     * @return 
     */
    private static boolean isHeadAvailable() {
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
        if ( headless!=null ) {
            return headless.booleanValue();
        } else {
            return "true".equals(DasApplication.getProperty("java.awt.headless","false"));
        }
    }
    
    public void setHeadless( boolean headless ) {
        this.headless= Boolean.valueOf(headless);
        if ( headless ) {
            System.setProperty("java.awt.headless","true");
        } else {
            if ( ! isHeadAvailable() ) {
                throw new IllegalArgumentException( "attempt to unset headless when environment is headless." );
            }
            System.setProperty("java.awt.headless","false");
        }
    }
    
    
    InputStreamMeter meter;
    public InputStreamMeter getInputStreamMeter() {
        if ( meter==null ) {
            meter= new InputStreamMeter();
        }
        return meter;
    }
    
    /**
     * @deprecated  use createMainFrame( String title, Container container );
     */
    public JFrame createMainFrame( java.awt.Container container ) {
        return createMainFrame( "Das2", container );
    }
    
    public JFrame createMainFrame( String title, java.awt.Container container ) {
        JFrame frame= createMainFrame(title);
        frame.getContentPane().add(container);
        frame.pack();
        frame.setVisible(true);
        return frame;
    }
    
    /**
     * @deprecated use createMainFrame(String title)
     */
    public JFrame createMainFrame() {
        return createMainFrame("Das2");
    }
    
    public JFrame createMainFrame( String title ) {
        mainFrame= new JFrame(title);
        final Preferences prefs= Preferences.userNodeForPackage(DasApplication.class);
        int xlocation= prefs.getInt( "xlocation", 20 );
        int ylocation= prefs.getInt( "ylocation", 20 );
        mainFrame.setLocation(xlocation, ylocation);
        mainFrame.addWindowListener( new WindowAdapter() {
            public void windowClosing( WindowEvent e ) {
                quit();
            }
        } );
        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        System.setProperty( "sun.awt.exception.handler", DasExceptionHandler.class.getName() );
        return mainFrame;
    }
    
    public JFrame getMainFrame() {
        return this.mainFrame;
    }

    public void setMainFrame( JFrame frame ) {
        this.mainFrame= frame;
    }
    
    public void quit() {
        final Preferences prefs= Preferences.userNodeForPackage(DasApplication.class);
        prefs.putInt( "xlocation", mainFrame.getLocation().x );
        prefs.putInt( "ylocation", mainFrame.getLocation().y );
        System.out.println("bye!"+mainFrame.getLocation());
        System.exit(0);
    }
    
    /**
     * Holds value of property interactive.
     */
    private boolean interactive=true;
    
    /**
     * Getter for property interactive.
     * @return Value of property interactive.
     */
    public boolean isInteractive() {
        return this.interactive;
    }
    
    /**
     * Setter for property interactive.
     * @param interactive New value of property interactive.
     */
    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
    }
    
    public String getDas2Version() {
        return Splash.getVersion();
    }
    
    private MonitorFactory monitorFactory;
    
    public MonitorFactory getMonitorFactory() {
        if ( monitorFactory==null ) {
            if ( !isHeadless() ) {
                monitorFactory= new DefaultMonitorFactory();
            } else {
                monitorFactory= new NullMonitorFactory();
            }
        }
        return monitorFactory;
    }
    
    public DataSetCache getDataSetCache() {
        if ( dataSetCache==null ) {
            if ( isHeadless() ) {
                dataSetCache= new NullDataSetCache();
            } else {
                dataSetCache= new LimitSizeBytesDataSetCache(30000000);
            }
        }
        return dataSetCache;
    }
    
    ExceptionHandler exceptionHandler;

    /**
     * warning: this code is repeated in FileSystem to avoid dependence.
     * @return
     */
    public ExceptionHandler getExceptionHandler() {
        if ( exceptionHandler==null ) {
            if ( isHeadless() ) {
                exceptionHandler= new ThrowRuntimeExceptionHandler();
            } else {
                exceptionHandler= new DefaultExceptionHandler();
            }
        }
        return exceptionHandler;
    }

    /**
     * explicitly set the ExceptionHandler that will handle runtime exceptions
     * @param h
     */
    public void setExceptionHandler( ExceptionHandler h ) {
        this.exceptionHandler= h;
    }
}
