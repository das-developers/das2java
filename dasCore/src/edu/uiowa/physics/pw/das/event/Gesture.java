/* File: Gesture.java
 * Copyright (C) 2002-2003 University of Iowa
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
package edu.uiowa.physics.pw.das.event;

public class Gesture {
    

//  here is a modification

// second modification


    public static final Gesture NONE = new Gesture("GestureNONE");    
    public static final Gesture BACK = new Gesture("GestureBACK");    
    public static final Gesture FORWARD = new Gesture("GestureFORWARD");    
    public static final Gesture ZOOMOUT = new Gesture("GestureZOOMOUT");    
    public static final Gesture UNDEFINED = new Gesture("GestureUNDEFINED");
    public static final Gesture SCANNEXT = new Gesture("GestureSCANNEXT");
    public static final Gesture SCANPREV = new Gesture("GestureSCANPREV");
    
    String name;
    
    Gesture(String name) {
        this.name= name;
    }
    
    public String toString() {
        return name;
    }
    
}

