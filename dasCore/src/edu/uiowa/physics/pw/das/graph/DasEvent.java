/* File: DasEvent.java
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

package edu.uiowa.physics.pw.das.graph;

/**
 *
 * @author  eew
 */
public class DasEvent {
    
    public static class MouseButton
    {
        private String s;
        private MouseButton(String s) { this.s = s; }
        
        public String toString() { return s; }
        
        public static final MouseButton LEFT = new MouseButton("left mouse button");
        public static final MouseButton MIDDLE = new MouseButton("middle mouse button");
        public static final MouseButton RIGHT = new MouseButton("right mouse button");
    }
    
    public static final MouseButton LEFT = MouseButton.LEFT;
    public static final MouseButton MIDDLE = MouseButton.MIDDLE;
    public static final MouseButton RIGHT = MouseButton.RIGHT;
    
    protected MouseButton button;
    protected boolean isShiftDown;
    
    /** Creates a new instance of DasEvent */
    public DasEvent(MouseButton b, boolean i)
    {
        button = b;
        isShiftDown = i;
    }
    
    public MouseButton getButton()
    {
        return button;
    }
    
    public boolean isShiftDown()
    {
        return isShiftDown;
    }
}
