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

package org.das2.event;

import org.das2.components.propertyeditor.Displayable;
import org.das2.components.propertyeditor.Editable;
import org.das2.graph.DasCanvasComponent;

import java.awt.*;
import java.awt.event.*;
import java.util.logging.Logger;
import org.das2.datum.LoggerManager;
import org.das2.system.DasLogger;

/** A MouseModule is a pluggable unit that promotes simple
 * mouse events into human events or actions that are useful
 * for science analysis.  Each component has a mouseInputAdapter
 * that manages a set of mouseModules, one is active at any
 * given time.
 *
 * The DasMouseInputAdapter will delegate mouse events, key events, and 
 * mouse wheel events to the active mouse module.
 * @author jbf
 */
public class MouseModule implements Editable, Displayable, KeyListener, MouseListener, MouseMotionListener, MouseWheelListener  {

    protected static final Logger logger= LoggerManager.getLogger(DasLogger.GUI_LOG.toString() );
    //protected DasCanvasComponent parent;
    protected DragRenderer dragRenderer;
    private String label;
    private String directions;
    
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
    
    /**
     * returns a string that identifies the module
     */
    public String getLabel() {
        return label;
    }
    
    /** return a cursor that indicates the selected module. */
    public Cursor getCursor() {
        return new Cursor(Cursor.DEFAULT_CURSOR);
    }

    public DragRenderer getDragRenderer() {
        return dragRenderer;
    }
    
    /**
     * reset the drag renderer. (Made public when the digitizer had different modes.
     * @param d 
     */
    public void setDragRenderer( DragRenderer d ) {
        this.dragRenderer= d;
        parent.repaint();
    }
    
    /** 
     * Action to take when a mouse range (click, drag, release) has been selected. 
     */
    public void mouseRangeSelected(MouseDragEvent e) {
    }
    
    /**
     * Action to take when a point (click or drag) is selected. 
     */
    public void mousePointSelected(MousePointSelectionEvent e) {
    }
    
    public final void setLabel(java.lang.String label) {
        this.label= label;
    }
    
    /**
     * allow one-line directions to be added to the mouse module.
     * This is used in Autoplot for the status bar.  
     * @return the directions, or null.
     */
    public String getDirections() {
        return this.directions;
    }
    
    public void setDirections( String directions ) {
        this.directions= directions;
    }
    
    @Override
    public javax.swing.Icon getListIcon() {
        return null;
    }

    @Override
    public void drawListIcon(Graphics2D g, int x, int y) {
       // do nothing
    }

    @Override
    public String getListLabel() {
        return getLabel();
    }
    
    @Override
    public void keyPressed(KeyEvent keyEvent) {
    }
    
    @Override
    public void keyReleased(KeyEvent keyEvent) {
    }
    
    @Override
    public void keyTyped(KeyEvent keyEvent) {
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {
    }
    
    @Override
    public void mouseExited(MouseEvent e) {
    }
    
    @Override
    public void mouseMoved(MouseEvent e) {
    }
    
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
    }

}
