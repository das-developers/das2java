/* File: EmptyDragRenderer.java
 * Copyright (C) 2002-2013 The University of Iowa
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

import java.awt.Graphics;
import java.awt.Point;
import org.das2.graph.DasCanvasComponent;

/**
 * Do-nothing drag renderer and extension point for other DragRenderers.
 * @author  jbf
 */
public abstract class AbstractDragRenderer implements DragRenderer
{
 
    public AbstractDragRenderer(){}
       
    public AbstractDragRenderer(DasCanvasComponent parent) {
        this.parent= parent;
    }
    
    /**
     * return the event for the gesture.  A mouse box event is returned.
     * @param source
     * @param p1
     * @param p2
     * @param isModified
     * @return 
     */
    @Override
    public MouseDragEvent getMouseDragEvent(Object source, Point p1, Point p2, boolean isModified) {
        return new MouseBoxEvent( source, p1, p2, isModified );
    }
    
    /**
     * this is not used, and is left over from an old version of the library.
     * @param g 
     */
    @Override
    public void clear(Graphics g) {
    }
    
    /**
     * indicates that MM.mousePointSelected() should called as new mouse events 
     * come in.  
     */
    @Override
    public boolean isPointSelection() {
        return true;
    }

    /**
     * range selection events should be fired during drag.
     */
    @Override
    public boolean isUpdatingDragSelection() {
        return false;
    }    
    
    private DasCanvasComponent parent;
    
    public DasCanvasComponent getParent() {
        return this.parent;
    }
    
    public void setParent( DasCanvasComponent parent ) {
        this.parent= parent;
    }
}
