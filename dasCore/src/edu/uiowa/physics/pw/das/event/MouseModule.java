/* File: MouseModule.java
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

import edu.uiowa.physics.pw.das.graph.DasCanvasComponent;

import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.util.Vector;

/**
 *
 * @author  jbf
 */
public class MouseModule extends MouseInputAdapter {
    
    protected DasCanvasComponent parent;
    protected DragRenderer dragRenderer;
    private String label;
    
    protected MouseModule() {
    }
    
    public MouseModule(DasCanvasComponent parent) {
        this.parent= parent;
        this.dragRenderer= EmptyDragRenderer.renderer;
        this.label= this.getClass().getName();
    }
    
    public String getLabel() {
        return label;
    }
    
    public Vector getHotSpots() {
        return null;
    }
    
    public Cursor getCursor() {
        return new Cursor(Cursor.DEFAULT_CURSOR);
    }
    
    public void hotSpotPressed(Shape s) {
    }
    
    public void mouseRangeSelected(MouseDragEvent e) {
    }
    
    public void mousePointSelected(MousePointSelectionEvent e) {
    }
    
    public void setLabel(java.lang.String label) {
        // note the label must be set before the module is added to the dmia!!!
        this.label= label;
    }
    
}
