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

import java.util.logging.*;

/**
 *
 * @author  Edward West
 */
public class DasApplication {
    
    private static final DasApplication DEFAULT = new DasApplication();
    
    static {
        String[] beanInfoSearchPath = { "edu.uiowa.physics.pw.das.beans" };
        java.beans.Introspector.setBeanInfoSearchPath(beanInfoSearchPath);
    }
    
    private NameContext nameContext;
    
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
        getDefaultApplication().getLogger().info( System.getProperty( "os.name" ) );
        String osName= System.getProperty( "os.name" );
        if ( "Windows".equals( osName ) ) {
            result= true;
        } else if ( "Windows XP".equals( osName ) ) {
            result= true;
        } else if ( isX11() ) {
            String DISPLAY= System.getProperty( "DISPLAY" );
            getDefaultApplication().getLogger().info( System.getProperty( "DISPLAY" ) );
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
    
    public Logger getLogger() {
        return DasProperties.getLogger();
    }
}
