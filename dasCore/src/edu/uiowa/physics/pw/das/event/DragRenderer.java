/* File: DragRenderer.java
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

package edu.uiowa.physics.pw.das.event;

import java.awt.*;

/** A DragRenderer provides the feedback to the human operator
 * of what his mousing is doing.  It applies constraints to the
 * drag as well. It promotes the awt mouse events into events
 * that represent the operation, implementing for example mouse
 * gestures.
 * @author eew
 */
public interface DragRenderer
{ 
    public abstract void renderDrag(Graphics g, Point p1, Point p2);
    public abstract void clear(Graphics g);
    public abstract MouseDragEvent getMouseDragEvent( Object source, Point p1, Point p2, boolean isModified ); 
    
    public boolean isXRangeSelection();
    
    public boolean isYRangeSelection();
    
    public boolean isPointSelection();
    
    public boolean isUpdatingDragSelection();
    
}
