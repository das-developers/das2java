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

package org.das2.graph;

import org.das2.DasApplication;
import org.das2.event.DasMouseInputAdapter;
import org.das2.event.MouseModule;
import org.das2.graph.event.DasUpdateListener;
import org.das2.system.DasLogger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.*;
import org.das2.components.propertyeditor.Editable;
import org.das2.components.propertyeditor.PropertyEditor;

/**
 * Super class providing base functionality for all canvas components such as 
 * DasAxis, DasPlot, and DasLabel.
 * @author  eew
 */
public abstract class DasCanvasComponent extends JComponent implements Editable {
    
    private Logger logger= DasLogger.getLogger(DasLogger.GUI_LOG);
    
    protected static abstract class CanvasComponentAction extends DasCanvas.CanvasAction {
        private static DasCanvasComponent currentCanvasComponent;
        public CanvasComponentAction(String label) {
            super(label);
        }
        public static DasCanvasComponent getCurrentComponent() {
            return currentCanvasComponent;
        }
    }
    
    private static final MouseListener currentComponentListener = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            DasCanvasComponent dcc;
            if (e.getSource() instanceof DasCanvasComponent) {
                dcc = (DasCanvasComponent)e.getComponent();
            } else {
                dcc = (DasCanvasComponent)SwingUtilities.getAncestorOfClass(DasCanvasComponent.class, e.getComponent());
            }
            CanvasComponentAction.currentCanvasComponent = dcc;
            DasCanvas canvas = dcc.getCanvas();
            canvas.makeCurrent();
        }
    };
    
    /**
     * action for entering the properties editor.
     */
    public static final Action PROPERTIES_ACTION = new CanvasComponentAction("Properties") {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (getCurrentComponent() != null) {
                getCurrentComponent().showProperties();
            }
        }
    };
    
    private DasRow row;
    private DasColumn column;
    private ResizeListener rl;
    
    /**
     * the mouse adapter for handling mouse events.
     */
    protected DasMouseInputAdapter mouseAdapter;
    
    private String dasName;
    
    /**
     * constructs a DasCanvasComponent, creating the
     * DasMouseInputAdapter for it and assigning a
     * default name to it.
     */
    public DasCanvasComponent() {
        setOpaque(false);
        rl = new ResizeListener();
        
        row= DasRow.NULL;
        column= DasColumn.NULL;
        
        setDasMouseInputAdapter( new DasMouseInputAdapter(this) );
        
        try {
            String name= DasApplication.getDefaultApplication().suggestNameFor(this);
            setDasName(name);
        } catch (org.das2.DasNameException dne) {
        }
    }
    
    /**
     * Add the MouseModule to the list of MouseModules
     * attached to the component via the DasMouseInputAdapter.
     * MouseModules will appear the in the order that they
     * are added.
     * @param module the mouse module to add
     * @see org.das2.event.MouseModule
     */
    public void addMouseModule(MouseModule module) {
        mouseAdapter.addMouseModule(module);
    }
    
    /**
     * Remove the MouseModule from the list of MouseModules
     * attached to the component via the DasMouseInputAdapter.
     * @param module the mouse module to remove
     * @see org.das2.event.MouseModule
     */
    public void removeMouseModule(MouseModule module) {
        mouseAdapter.removeMouseModule(module);
    }


    /**
     * accessor for the DasRow used for positioning the component.
     * @return DasRow used for positioning the component.
     */
    public DasRow getRow() {
        return row;
    }
    
    /**
     * accessor for the DasColumn used for positioning the component.
     * @return DasColumn used for positioning the component.
     */
    public DasColumn getColumn() {
        return column;
    }
    
    /** Called by the DasCanvas layout manager to request this component
     * to set its bounds.
     */
    public void resize() {
        if (column == DasColumn.NULL || row == DasRow.NULL ) {
            logger.log(Level.WARNING, "Null row and/or column in resize: row={0} column={1}", new Object[]{row, column});
        } else {
            setBounds(column.getDMinimum(),row.getDMinimum(),
                    (column.getDMaximum()-column.getDMinimum()),
                    (row.getDMaximum()-row.getDMinimum()));
        }
    }

    /**
     * class for handling resize events.
     */
    private class ResizeListener implements DasUpdateListener {
        @Override
        public void update(org.das2.graph.event.DasUpdateEvent e) {
            logger.log(Level.FINE, "component row or column moved: {0}", e.getSource());
            markDirty("resize");
            DasCanvasComponent.this.update();
        }
        
    }
    
    /**
     * set the DasRow for positioning the component vertically.
     * The current row is disconnected, and a propertyChange is
     * fired.
     * @param r the DasRow
     */
    public void setRow(DasRow r) {
        if (row == r) {
            return;
        }
        Object oldValue = row;
        if (row != DasRow.NULL ) {
            row.removeUpdateListener(rl);
        }
        row = r;
        if (row != DasRow.NULL ) {
            row.addUpdateListener(rl);
        } /*else {
            throw new IllegalArgumentException("null row is not allowed for the meantime");
        }*/
        if ( getRow()!=DasRow.NULL && getColumn()!=DasColumn.NULL ) resize();
        firePropertyChange("row", oldValue, r);
    }
    
    /**
     * set the DasColumn for positioning the component horizontally.
     * The current column is disconnected, and a propertyChange is
     * fired.
     * @param c the DasColumn
     */
    public void setColumn(DasColumn c) {
        if (column == c) {
            return;
        }
        Object oldValue = column;
        if (column != DasColumn.NULL ) {
            column.removeUpdateListener(rl);
        }
        column = c;
        if (column != DasColumn.NULL ) {
            column.addUpdateListener(rl);
        } /*else {
            throw new IllegalArgumentException("null column is not allowed for the meantime");
        }*/
        if ( getRow()!=DasRow.NULL && getColumn()!=DasColumn.NULL ) resize();
        firePropertyChange("column", oldValue, c);
    }
    
    /**
     * popup the PropertyEditor for editing the state
     * of this component.
     */
    public void showProperties() {
        PropertyEditor editor = new PropertyEditor(this);
        editor.showDialog(this);
    }
    
    /**
     * @return a concise String representation of the object.
     */
    @Override
    public String toString() {
        return getClass().getName()+"'"+getDasName()+"'";
    }
    
    /**
     * This method is called when a DasUpdateEvent is processed.
     * The default implementation does nothing.  If a subclass
     * needs to do any expensive operations involved in updating,
     * they should be done by overriding this method so that
     * the AWT Event Queue can coalesce update events.
     */
    protected void updateImmediately() {
        logger.log(Level.FINER, "updateImmediately for {0}", this.getClass().getName());
    }
    
    private org.das2.event.DasUpdateEvent devt;
    
    /**
     * posts an update event on the SystemEventQueue, indicating that work needs to be
     * done to get the get the component back into a valid state.
     */
    public void update() {
        logger.log(Level.FINER, "update for {0}", this.getClass().getName());
        java.awt.EventQueue eventQueue =
                Toolkit.getDefaultToolkit().getSystemEventQueue();
        if (devt == null) devt = new org.das2.event.DasUpdateEvent(this);
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
    @Override
    protected void processEvent(AWTEvent e) {
        super.processEvent(e);
        if (e instanceof org.das2.event.DasUpdateEvent) {
            processDasUpdateEvent((org.das2.event.DasUpdateEvent)e);
        }
    }
    
    /**
     * like processEvent, but we also check the dirty status
     * @param e the event  
     */
    protected void processDasUpdateEvent(org.das2.event.DasUpdateEvent e) {
        logger.fine("enter process DasUpdateEvent");
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
    @Override
    protected AWTEvent coalesceEvents(AWTEvent existingEvent, AWTEvent newEvent) {
        if (existingEvent instanceof org.das2.event.DasUpdateEvent && newEvent instanceof org.das2.event.DasUpdateEvent) {
            return existingEvent;
        }
        return super.coalesceEvents(existingEvent, newEvent);
    }
    
    /**
     * currently does nothing.
     */
    protected void installComponent() {}
    
    /**
     * remove the component row and column update listener.
     */
    protected void uninstallComponent() {
        getRow().removeUpdateListener(rl);
        getColumn().removeUpdateListener(rl);
    }
    
    /**
     * return the font used to paint the component.
     * @return the font.
     */
    @Override
    public Font getFont() {
        return (getParent() == null ? super.getFont() : getParent().getFont());
    }
    
    /**
     * convenient method intended to encourage use of em's.  returns the em size for the canvas.  
     * We define the em size as the height of the component's font.
     * @return the height of the component's font.
     */
    public double getEmSize() {
        Font f= getFont();
        return f==null ? 8 : f.getSize2D();
    }
    
    private final Set dirty = new HashSet<String>();
    
    /**
     * set the dirty flag indicating the state has changed and work is to be
     * done to restore a valid state.  For example, a DasAxis' minimum is
     * changed, so we will need to recalculate the ticks.  (But we don't want
     * to recalculate the ticks immediately, since the maximum may change
     * as well.)
     */
    void markDirty() {
        dirty.add("??");
    }
    
    /**
     * mark the component as dirty, providing a reason that is useful 
     * when debugging.
     * @param note a note useful for debugging.
     */
    void markDirty( String note ) {
        boolean loudDebug= false; // ( this instanceof DasAxis ) && ( ((DasAxis)this).isHorizontal() );
        if ( loudDebug ) {
            logger.log(Level.WARNING, "{0}.markDirty({1})", new Object[]{this.dasName, note});
        } else {
            logger.log(Level.FINE, "{0}.markDirty({1})", new Object[]{this.dasName, note});
        }
        dirty.add(note);
    }
    
    /**
     * Returns true if the component has been marked as dirty, meaning
     * work needs to be done to restore it to a valid state.
     * @return true if the component has been marked as dirty.
     */
    boolean isDirty() {
        return !dirty.isEmpty();
    }
    
    /**
     * clear the dirty flag, indicating the component is in a self-consistent
     * state.
     */
    void markClean() {
        boolean loudDebug= false; // ( this instanceof DasAxis ) && ( ((DasAxis)this).isHorizontal() );
        if ( loudDebug ) {
            logger.log(Level.WARNING, "{0}.markClean()", this.dasName);
        } else {
            logger.log(Level.FINE, "{0}.markClean()", this.dasName);
        }
        dirty.clear();
    }
    
    /**
     * get the DasCanvas which contains this DasCanvasComponent.
     * @return the DasCanvas which contains this DasCanvasComponent.
     */
    public DasCanvas getCanvas() {
        return (DasCanvas)getParent();
    }
    
    /**
     * Get the String identifier for the component which identifies
     * the component within the application.  This name should be
     * consistent between sessions of an application, where
     * applicable, for persistent state support.
     *
     * @return the name of the component.
     */
    public String getDasName() {
        return dasName;
    }
    
    /**
     * Set the String identifier for the component which identifies
     * the component within the application.  This name should be
     * consistent between sessions of an application, where
     * applicable, for persistent state support.  For example,
     * "timeAxis1" or "theTimeAxis"
     * @param name unique String identifying the component within
     * the application.
     * @throws org.das2.DasNameException
     */
    public final void setDasName(String name) throws org.das2.DasNameException {
        if (name.equals(dasName)) {
            return;
        }
        setName(name); // might as well set the component name.
        String oldName = dasName;
        dasName = name;
        DasApplication app = DasApplication.getDefaultApplication();
        if (app != null) {
            app.getNameContext().put(name, this);
            if (oldName != null) {
                app.getNameContext().remove(oldName);
            }
        }
        if ( getDasMouseInputAdapter()!=null ) {
            getDasMouseInputAdapter().resetName(name);
        }

        this.firePropertyChange("name", oldName, name);
    }
    
    /**
     * returns the active region of the canvas component, which is not necessarily the bounds.
     * @return the active region of the canvas component
     */
    public Shape getActiveRegion() {
        int x = getColumn().getDMinimum();
        int y = getRow().getDMinimum();
        int width = getColumn().getDMaximum() - x;
        int height = getRow().getDMaximum() - y;
        return new Rectangle(x, y, width, height);
    }
    
    /**
     * returns true if the component is suitable context for the point.  For example,
     * the operator right-clicks at the point, is this point a transparent region of
     * the component, and accepting context would be confusing to the operator?  This
     * was first introduced to support the annotation component, which draws a compact
     * background bubble around a message, which is typically smaller than its bounds,
     * plus an arrow.
     * @param x the x location on the canvas, with (0,0) being the upper-left corner.
     * @param y the y location on the canvas, with (0,0) being the upper-left corner.
     * @return true if the component accepts the context at this point.
     */
    public boolean acceptContext( int x, int y ) {
        return true;
    }
    
    /**
     * return a list of actions.  This is used by the DasMouseInputAdapter.
     * @return the actions this provides.  
     */
    public Action[] getActions() {
        return new Action[] {
            PROPERTIES_ACTION,
        };
    }
    
    /**
     * Get the DasMouseInputAdapter, which handles mouse input for the component.
     * @return the dasMouseInputAdapter.
     */
    public final DasMouseInputAdapter getDasMouseInputAdapter() {
        return this.mouseAdapter;
    }
    
    /**
     * Set the dasMouseInputAdapter, which handles mouse input for the component
     * @param dasMouseInputAdapter the dasMouseInputAdapter.
     */
    private void setDasMouseInputAdapter(DasMouseInputAdapter dasMouseInputAdapter) {
        if ( mouseAdapter!=null ) {
            removeMouseListener(mouseAdapter);
            removeMouseMotionListener(mouseAdapter);
            removeMouseListener(currentComponentListener);
            removeKeyListener(mouseAdapter.getKeyAdapter());
            removeMouseWheelListener(mouseAdapter);
        }
        this.mouseAdapter = dasMouseInputAdapter;
        if ( ! DasApplication.getDefaultApplication().isHeadless() ) {
            addMouseListener(mouseAdapter);
            addMouseMotionListener(mouseAdapter);
            addMouseListener(currentComponentListener);
            addKeyListener(mouseAdapter.getKeyAdapter());
            addMouseWheelListener(mouseAdapter);
        }

    }
    
}
