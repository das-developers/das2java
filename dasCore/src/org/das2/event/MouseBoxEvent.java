/* File: MouseBoxEvent.java
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

import java.awt.*;

public class MouseBoxEvent extends MouseDragEvent {
    
    private final Point pressPoint;
    private final Point releasePoint;
    
    /** 
     * Creates a new instance of MouseBoxEvent
     * @param source the source, typically a GUI component.
     * @param pressPoint the mouse press point
     * @param releasePoint the mouse release point
     * @param isModified this is ignored.
     */
    public MouseBoxEvent(Object source, Point pressPoint, Point releasePoint, boolean isModified) {
        super(source);
        this.pressPoint= pressPoint;
        this.releasePoint= releasePoint;
    }
    
    /**
     * return the point where the event started
     * @return the point where the event started
     */
    public Point getPressPoint() {
        return pressPoint;
    }
    
    /**
     * return the point locating the mouse position, which is also the release point
     * @return the point locating the mouse position, which is also the release point
     */
    public Point getPoint() {
        return releasePoint;
    }
    
    /**
     * return the leftmost, or minimum, point of the two
     * @return the leftmost, or minimum, point of the two
     */
    public int getXMinimum() {
        return ( pressPoint.x < releasePoint.x ) ?  pressPoint.x : releasePoint.x ;
    }
        
    /**
     * return the rightmost, or maximum, point of the two
     * @return the rightmost, or maximum, point of the two
     */
    public int getXMaximum() {
        return ( pressPoint.x > releasePoint.x ) ?  pressPoint.x : releasePoint.x ;
    }
    
    /**
     * return the topmost, or maximum, point of the two
     * @return the topmost, or maximum, point of the two
     */
    public int getYMinimum() {
        return ( pressPoint.y < releasePoint.y ) ?  pressPoint.y : releasePoint.y ;
    }
        
    /**
     * return the bottommost, or maximum, point of the two
     * @return the bottommost, or maximum, point of the two
     */
    public int getYMaximum() {
        return ( pressPoint.y > releasePoint.y ) ?  pressPoint.y : releasePoint.y ;
    }
    
}
