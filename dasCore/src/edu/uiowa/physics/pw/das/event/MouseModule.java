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

import edu.uiowa.physics.pw.das.components.propertyeditor.*;
import edu.uiowa.physics.pw.das.graph.DasCanvasComponent;

import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

/** A MouseModule is a pluggable unit that promotes simple
 * mouse events into human events or actions that are useful
 * for science analysis.  Each component has a mouseInputAdapter
 * that manages a set of mouseModules, one is active at any
 * given time.
 * @author jbf
 */
public class MouseModule implements Editable, Displayable, KeyListener {
    
    //protected DasCanvasComponent parent;
    protected DragRenderer dragRenderer;
    private String label;
    protected DasCanvasComponent parent;    
    
    protected MouseModule() {
        label= "unlabelled MM";
        dragRenderer= EmptyDragRenderer.renderer;
    }
    
    public MouseModule(DasCanvasComponent parent) {
       this( parent, EmptyDragRenderer.renderer, "unlabelled MM" );
       setLabel(this.getClass().getName());
    }
    
    public MouseModule(DasCanvasComponent parent, DragRenderer dragRenderer, String label) {
        this.parent= parent;
        this.dragRenderer= dragRenderer;
        this.label= label;
    }
    
    /** returns a string that identifies the module */    
    public String getLabel() {
        return label;
    }
    
    public Vector getHotSpots() {
        return null;
    }
    
    /** return a cursor that indicates the selected module. */    
    public Cursor getCursor() {
        return new Cursor(Cursor.DEFAULT_CURSOR);
    }
    
    public void hotSpotPressed(Shape s) {
    }
    
    public DragRenderer getDragRenderer() {
        return dragRenderer;
    }
    
    /** Action to take when a mouse range has been selected. */    
    public void mouseRangeSelected(MouseDragEvent e) {
    }
    
    /** Action to take when a point is selected. */    
    public void mousePointSelected(MousePointSelectionEvent e) {
    }
    
    public void setLabel(java.lang.String label) {        
        this.label= label;
    }
        
    public void mouseReleased(MouseEvent e) {        
    }
    
    public void mousePressed(MouseEvent e) {
    }
    
    public void mouseDragged(MouseEvent e) {
    }

    public javax.swing.Icon getListIcon() {
        return null;
    }

    public String getListLabel() {
        return getLabel();
    }

    public void keyPressed(KeyEvent keyEvent) {
    }

    public void keyReleased(KeyEvent keyEvent) {
    }

    public void keyTyped(KeyEvent keyEvent) {
    }
}
