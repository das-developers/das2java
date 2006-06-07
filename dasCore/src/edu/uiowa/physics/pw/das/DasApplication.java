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

import edu.uiowa.physics.pw.das.client.InputStreamMeter;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.dataset.DataSetCache;
import edu.uiowa.physics.pw.das.graph.DasAnnotation;
import edu.uiowa.physics.pw.das.graph.DasAxis;
import edu.uiowa.physics.pw.das.graph.DasCanvas;
import edu.uiowa.physics.pw.das.graph.DasCanvasComponent;
import edu.uiowa.physics.pw.das.graph.DasColorBar;
import edu.uiowa.physics.pw.das.graph.DasColumn;
import edu.uiowa.physics.pw.das.graph.DasPlot;
import edu.uiowa.physics.pw.das.graph.DasRow;
import edu.uiowa.physics.pw.das.system.*;
import edu.uiowa.physics.pw.das.util.*;
import java.awt.event.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.*;
import java.util.prefs.*;
import javax.swing.*;

/**
 *
 * @author  Edward West
 */
public class DasApplication {
    
    private static final DasApplication DEFAULT = new DasApplication();
    
    private JFrame mainFrame;
    
    private Boolean applet;    // tristate: null, TRUE, FALSE  null->try to detect.  TRUE|FALSE->explicit setting by application
    private Boolean headless;  // tristate: null, TRUE, FALSE
    
    private NameContext nameContext;
    
    private DataSetCache dataSetCache;
    
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
    
    public Logger getLogger() {
        return DasLogger.getLogger();
    }
    
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
        
    Map hitsMap= new HashMap();
    
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
        hitsMap.put( type, new Integer(ihits));
        return type+"_"+ihits;
    }
    
    public static DasApplication getDefaultApplication() {
        return DEFAULT;
    }
    
    public final boolean isApplet() {
        if ( applet==null ) {
            return Thread.currentThread().getContextClassLoader().getClass().getName().indexOf("plugin") > -1;
        } else {
            return applet.booleanValue();
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
                DasExceptionHandler.handle(e);
            }
        }
    }
    
    public boolean isReloadLoggingProperties() {
        return false;
    }
    
    private static boolean isX11() {
        String osName= System.getProperty( "os.name" );
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
         if ( System.getProperty("user.name").equals("Web") ) {
            local= new File("/tmp");
        } else {
            local= new File( System.getProperty("user.home") );
        }
        local= new File( local, ".das2" );
        return local;
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
        if ( headless!=null ) {
            return headless.booleanValue();
        } else {
            return "true".equals(System.getProperty("java.awt.headless"));
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
    

    public DataSetCache getDataSetCache() {
        if ( dataSetCache==null ) {
            //dataSetCache= new SimpleDataSetCache();
            //dataSetCache= new LimitCountDataSetCache(10);
            dataSetCache= new LimitSizeBytesDataSetCache(30000000);
        }
        return dataSetCache;
    }
    
    InputStreamMeter meter;
    public InputStreamMeter getInputStreamMeter() {
        if ( meter==null ) {
            meter= new InputStreamMeter();
        }
        return meter;
    }
        
    /**
     * @deprecated.  Use createMainFrame( String title, Container container );
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
     * @deprecated.  Use createMainFrame(String title) 
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
            monitorFactory= new MonitorFactory();
        }
        return monitorFactory;
    }
    

}
