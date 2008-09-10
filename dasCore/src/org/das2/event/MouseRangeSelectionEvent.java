/* File: MouseRangeSelectionEvent.java
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

package org.das2.event;



/**
 *
 * @author  eew
 */
public class MouseRangeSelectionEvent extends MouseDragEvent
{
    
    private int min, max;
    private boolean isModified;
    
    /** Creates a new instance of DasDevicePositionEvent */
    public MouseRangeSelectionEvent(Object source, int min, int max, boolean isModified) {
        super(source);
        if (min>max) {
            int t= min;
            min= max;
            max= t;
        }            
        
        this.min= min;
        this.max= max;
        this.isModified= isModified;
    }
        
    public int getMinimum() {
        return min;
    }
    
    public int getMaximum() {
        return max;
    }
    public boolean isModified() {
        return isModified;
    }
}
