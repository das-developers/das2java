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

package org.das2.event;

import java.awt.*;

/** A DragRenderer provides the feedback to the human operator
 * of what his or her mousing is doing.  It applies constraints to the
 * drag as well. It promotes the awt mouse events into events
 * that represent the operation, implementing for example mouse
 * gestures.
 *
 * @author eew
 */
public interface DragRenderer
{ 
    /**
     * use this color when drawing ghostly backgrounds for contrast. 
     */
    public Color ghostColor= new Color(255,255,255,100);
    
    /**
     * draws the drag for mousing from p1 to p2, and returns an array of
     * Rectangles covering the rendering.  If nothing is drawn, then an 
     * array of length zero should be returned, and nulls are allowed in the
     * array. p1 and p2, and g are in the canvas frame of reference.
     * @param g the graphics context for rendering
     * @param p1 the click point
     * @param p2 the current mouse position during drag and release.
     * @return 
     */
    public abstract Rectangle[] renderDrag(Graphics g, Point p1, Point p2);
    
    /** clears whatever renderDrag rendered.  This is not used by the DasMouseInputAdapter,
     * but must still be supported for now.  Originally the drag renderer would have
     * to unpaint itself as well, but this is no longer used.
     * @param g the graphics context
     */
    public abstract void clear(Graphics g);
    
    /**
     * promotes the drag begin and end into a mouseDragEvent.
     * @param source
     * @param p1 the click point
     * @param p2 the current mouse position during drag and release.
     * @param isModified
     * @return 
     */
    public abstract MouseDragEvent getMouseDragEvent( Object source, Point p1, Point p2, boolean isModified ); 
    
    /**
     * indicates that the mouse module's mousePointSelected() should be called as new mouse events come in.
     * @return true if the mouse module should receive events during the drag.
     */
    public boolean isPointSelection();
    
    /**
     * range selection events should be fired during drag.
     * @return true if selection events should be fired during drag.
     */
    public boolean isUpdatingDragSelection();
    
}
