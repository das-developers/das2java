/* File: DasCanvasComponent.java
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

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.components.propertyeditor.Editable;
import edu.uiowa.physics.pw.das.components.propertyeditor.PropertyEditor;
import edu.uiowa.physics.pw.das.event.DasMouseInputAdapter;
import edu.uiowa.physics.pw.das.event.MouseModule;
import edu.uiowa.physics.pw.das.graph.event.DasUpdateListener;

import javax.swing.*;
import java.awt.*;

/**
 *
 * @author  eew
 */
public abstract class DasCanvasComponent extends JPanel implements Editable {
    
    private DasRow row;
    private DasColumn column;
    private ResizeListener rl;
    protected DasMouseInputAdapter mouseAdapter;
    private String dasName;
    
    public DasCanvasComponent() {
        setOpaque(false);
        rl = new ResizeListener();
        mouseAdapter= new DasMouseInputAdapter(this);
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
        try {
            setDasName("c_" + Integer.toString(this.hashCode()));
        }
        catch (edu.uiowa.physics.pw.das.DasNameException dne) {
        }
    }
    
    public DasCanvasComponent(DasRow row, DasColumn column) {
        this();
        setRow(row);
        setColumn(column);
    }
    
    public void addMouseModule(MouseModule module) {
        mouseAdapter.addMouseModule(module);
    }
    
    public void removeMouseModule(MouseModule module) {
        mouseAdapter.removeMouseModule(module);
    }
    
    public DasRow getRow() {
        return row;
    }
    
    public DasColumn getColumn() {
        return column;
    }
    
    public void resize() {
        if ( column==null ) {
            edu.uiowa.physics.pw.das.util.DasDie.println(""+this+" has null column in resize");
        } else if ( row==null ) {
            edu.uiowa.physics.pw.das.util.DasDie.println(""+this+" has null row in resize");
        } else {
            setBounds(column.getDMinimum(),row.getDMinimum(),
            (column.getDMaximum()-column.getDMinimum()),
            (row.getDMaximum()-row.getDMinimum()));
        }
    }
    
    private class ResizeListener implements DasUpdateListener {
        
        public void update(edu.uiowa.physics.pw.das.graph.event.DasUpdateEvent e) {
            markDirty();
            DasCanvasComponent.this.update();
        }
        
    }
    
    public void setRow(DasRow r) {
        if (row == r) {
            return;
        }
	Object oldValue = row;
        if (row != null) {
            row.removepwUpdateListener(rl);
        }
        row = r;
        if (row != null) {
            row.addpwUpdateListener(rl);
        } /*else {
            throw new IllegalArgumentException("null row is not allowed for the meantime");
        }*/
	firePropertyChange("row", oldValue, r);
    }
    
    public void setColumn(DasColumn c) {
        if (column == c) {
            return;
        }
	Object oldValue = column;
        if (column != null) {
            column.removepwUpdateListener(rl);
        }
        column = c;
        if (column != null) {
            column.addpwUpdateListener(rl);
        } /*else {
            throw new IllegalArgumentException("null column is not allowed for the meantime");
        }*/
	firePropertyChange("column", oldValue, c);
    }

    public void showProperties() {
        PropertyEditor editor = new PropertyEditor(this);
        editor.showDialog(this);
    }

    public String toString() {
        return getClass().getName()+"'"+getName()+"'";
    }
    
    /**
     * This method is called when a DasUpdateEvent is processed.
     * The default implementation does nothing.  If a subclass
     * needs to do any expensive operations involved in updating, 
     * they should be done by overriding this method so that
     * the AWT Event Queue can coalesce update events.
     */
    protected void updateImmediately() {
    }
    
    private edu.uiowa.physics.pw.das.event.DasUpdateEvent devt;
    
    public void update() {
        java.awt.EventQueue eventQueue =
        Toolkit.getDefaultToolkit().getSystemEventQueue();
        if (devt == null) devt = new edu.uiowa.physics.pw.das.event.DasUpdateEvent(this);
        eventQueue.postEvent(devt);
    }

    /** Processes events occurring on this component. By default this
     * method calls the appropriate
     * <code>process&lt;event&nbsp;type&gt;Event</code>
     * method for the given class of event.
     * <p>Note that if the event parameter is <code>null</code>
     * the behavior is unspecified and may result in an
     * exception.
     *
     * @param     e the event
     * @see       java.awt.Component#processComponentEvent
     * @see       java.awt.Component#processFocusEvent
     * @see       java.awt.Component#processKeyEvent
     * @see       java.awt.Component#processMouseEvent
     * @see       java.awt.Component#processMouseMotionEvent
     * @see       java.awt.Component#processInputMethodEvent
     * @see       java.awt.Component#processHierarchyEvent
     * @see       java.awt.Component#processMouseWheelEvent
     * @see       #processDasUpdateEvent
     */
    protected void processEvent(AWTEvent e) {
        super.processEvent(e);
        if (e instanceof edu.uiowa.physics.pw.das.event.DasUpdateEvent) {
            processDasUpdateEvent((edu.uiowa.physics.pw.das.event.DasUpdateEvent)e);
        }
    }
    
    protected void processDasUpdateEvent(edu.uiowa.physics.pw.das.event.DasUpdateEvent e) {
        if (isDisplayable()) {
            if (isDirty()) {
                markClean();
                updateImmediately();
            }
            resize();
            repaint();
        }
    }
    
    /** Potentially coalesce an event being posted with an existing
     * event.  This method is called by <code>EventQueue.postEvent</code>
     * if an event with the same ID as the event to be posted is found in
     * the queue (both events must have this component as their source).
     * This method either returns a coalesced event which replaces
     * the existing event (and the new event is then discarded), or
     * <code>null</code> to indicate that no combining should be done
     * (add the second event to the end of the queue).  Either event
     * parameter may be modified and returned, as the other one is discarded
     * unless <code>null</code> is returned.
     * <p>
     * This implementation of <code>coalesceEvents</code> coalesces
     * <code>DasUpdateEvent</code>s, returning the existingEvent parameter
     *
     * @param  existingEvent  the event already on the <code>EventQueue</code>
     * @param  newEvent       the event being posted to the
     * 		<code>EventQueue</code>
     * @return a coalesced event, or <code>null</code> indicating that no
     * 		coalescing was done
     */
    protected AWTEvent coalesceEvents(AWTEvent existingEvent, AWTEvent newEvent) {
        if (existingEvent instanceof edu.uiowa.physics.pw.das.event.DasUpdateEvent && newEvent instanceof edu.uiowa.physics.pw.das.event.DasUpdateEvent) {
            return existingEvent;
        }
        return super.coalesceEvents(existingEvent, newEvent);
    }
    
    protected void installComponent() {}
    
    protected void uninstallComponent() {}
    
    public Font getFont() {
        return (getParent() == null ? super.getFont() : getParent().getFont());
    }
    
    boolean dirty = true;
    void markDirty() {
        dirty = true;
    }
    boolean isDirty() {
        return dirty;
    }
    void markClean() {
        dirty = false;
    }
    
    public DasCanvas getCanvas() {
        return (DasCanvas)getParent();
    }
    
    public String getDasName() {
        return dasName;
    }
    
    public void setDasName(String name) throws edu.uiowa.physics.pw.das.DasNameException {
        if (name.equals(dasName)) {
            return;
        }
        String oldName = dasName;
        dasName = name;
        DasApplication app = null;
        if (getCanvas() != null) {
            app = getCanvas().getDasApplication();
        }
        if (app != null) {
            app.getNameContext().put(name, this);
            if (oldName != null) {
                app.getNameContext().remove(oldName);
            }
        }
        this.firePropertyChange("name", oldName, name);
    }
    
    public Shape getActiveRegion() {
        int x = getColumn().getDMinimum();
        int y = getRow().getDMinimum();
        int width = getColumn().getDMaximum() - x;
        int height = getRow().getDMaximum() - y;
        return new Rectangle(x, y, width, height);
    }
    
    public DasMouseInputAdapter getMouseAdapter() {
        return mouseAdapter;
    }
    
}
