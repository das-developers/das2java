/* File: DasDie.java
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

package edu.uiowa.physics.pw.das.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 *
 * @author  jbf
 */
public class DasDie {
    
    public static int DEBUG=-20;  // info useful only to das developers
    public static int VERBOSE=-10; // info useful to end users in debugging
    public static int INFORM=0; // warm-fuzzy operational messages
    public static int WARN=10; // end user needs to be aware of, no action required
    public static int ALARM=20; // end user needs to take action
    public static int CRITICAL=30;  // abnormal system condition that cannot be supressed.
    
    public static int verbosity;
    
    static {
        String debugLevel= edu.uiowa.physics.pw.das.DasProperties.getInstance().getProperty("debugLevel");
        setDebugVerbosityLevel(debugLevel);
    }
    
    /** Creates a new instance of DasDie */
    private DasDie() {
    }
    
    public static void setDebugVerbosityLevel(String debugLevel) {
        if (debugLevel.equals("endUser")) verbosity= WARN;
        else if (debugLevel.equals("dasDeveloper")) verbosity= DEBUG;
        else verbosity= INFORM; 
    }
    
    private static final String calledBy() {
        StringWriter sw = new StringWriter();
        new Throwable().printStackTrace(
        new PrintWriter( sw )
        );
        String callStack = sw.toString();
        
        int atPos = callStack.indexOf( "at" );
        //edu.uiowa.physics.pw.das.util.DasDie.println(callStack.substring(atPos));
        atPos = callStack.indexOf( "at" , atPos+2 );
        //edu.uiowa.physics.pw.das.util.DasDie.println(callStack.substring(atPos));
        atPos = callStack.indexOf( "at" , atPos+2 );
        //edu.uiowa.physics.pw.das.util.DasDie.println(callStack.substring(atPos));
        atPos = callStack.indexOf( "at" , atPos+2 );
        //edu.uiowa.physics.pw.das.util.DasDie.println(callStack.substring(atPos));
        int nextAtPos= callStack.indexOf( "at" , atPos+2 );
        
        String calledBy= callStack.substring(atPos,nextAtPos-2);
        return calledBy;
    }
    
    public static final void die(String message) {                
              
        System.out.print(calledBy()+": ");
        edu.uiowa.physics.pw.das.util.DasDie.println(CRITICAL,message);
        System.exit(-1);
    }
    
    public static final void println(java.lang.String message) {
        println(DEBUG,message);
    }
    
    public static final void println(Object o) {
        println(o.toString());
    }
    
    public static final void println(int verbosity, java.lang.String message) {
        if (verbosity>=DasDie.verbosity) {
            //System.out.print(calledBy()+": ");
            System.err.println(message);
        }
    }
    
    public static final void print(java.lang.String message) {
        print(DEBUG,message);
    }
    
    public static final void print(Object o) {
        print(o.toString());
    }
    
    public static final void print(int verbosity, java.lang.String message) {
        if (verbosity>=DasDie.verbosity) {
            //System.out.print(calledBy()+": ");
            System.out.print(message);
        }
    }
    
        
}
