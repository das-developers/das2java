/* File: DnDSupport.java
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

package org.das2.util;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;

/**
 *
 * @author  eew
 */
public abstract class DnDSupport {
    
    private Component component;
    
    private DnDSupport parent;
    
    private int ops;
    
    private GestureRecognizer gestureRecognizer;
    
    protected DnDSupport(Component component, int ops, DnDSupport parent) {
        this.component = component;
        this.parent = parent;
        this.ops = ops;
        component.setDropTarget(new DropTarget(component, ops, new DropHandler()));
    }
    
    public void setParent(DnDSupport parent) {
        this.parent = parent;
    }
    
    protected abstract int canAccept(DataFlavor[] flavors, int x, int y, int action);
    
    protected abstract boolean importData(Transferable t, int x, int y, int action);
    
    protected abstract void done();
    
    protected abstract void exportDone(Transferable t, int action);
    
    protected abstract Transferable getTransferable(int x, int y, int action);
    
    private int canAcceptInternal(DataFlavor[] flavors, int x, int y, int action) {
        int result = canAccept(flavors, x, y, action);
        if (result == -1 && parent != null) {
            return parent.canAcceptInternal(flavors, x + component.getX(), y + component.getY(), action);
        }
        return result;
    }
    
    private boolean importDataInternal(Transferable t, int x, int y, int action) {
        if (canAccept(t.getTransferDataFlavors(), x, y, action) != -1) {
            return importData(t, x, y, action);
        }
        else if (parent != null) {
            return parent.importDataInternal(t, x + component.getX(), y + component.getY(), action);
        }
        else {
            return false;
        }
    }
    
    private void doneInternal() {
        done();
        if (parent != null) {
            parent.doneInternal();
        }
    }
    
    public void startDrag(int x, int y, int action, java.awt.event.MouseEvent evt) {
        if (gestureRecognizer == null) {
            gestureRecognizer = new GestureRecognizer(component, new DragHandler());
        }
        gestureRecognizer.startDrag(x, y, action, evt);
    }
    
    private class DropHandler implements DropTargetListener {
        
        public void dragEnter(DropTargetDragEvent dtde) {
            Point location = dtde.getLocation();
            int action = canAcceptInternal(dtde.getCurrentDataFlavors(),
                                location.x, location.y,
                                dtde.getDropAction());
            if (action == -1) {
                dtde.rejectDrag();
            }
            else {
                dtde.acceptDrag(action);
            }
        }
        
        public void dragExit(DropTargetEvent dte) {
            doneInternal();
        }
        
        public void dragOver(DropTargetDragEvent dtde) {
            Point location = dtde.getLocation();
            int action = canAcceptInternal(dtde.getCurrentDataFlavors(),
                                           location.x, location.y,
                                           dtde.getDropAction());
            if (action == -1) {
                dtde.rejectDrag();
            }
            else {
                dtde.acceptDrag(action);
            }
        }
        
        public void drop(DropTargetDropEvent dtde) {
            Point location = dtde.getLocation();
            int action = canAcceptInternal(dtde.getCurrentDataFlavors(),
                                           location.x, location.y,
                                           dtde.getDropAction());
            if (action == -1) {
                dtde.rejectDrop();
            }
            else {
                dtde.acceptDrop(action);
                boolean success = importDataInternal(dtde.getTransferable(),
                                                     location.x, location.y,
                                                     dtde.getDropAction());
                dtde.dropComplete(success);
            }
            doneInternal();
        }
        
        public void dropActionChanged(DropTargetDragEvent dtde) {
        }
    }
    
    private class GestureRecognizer extends java.awt.dnd.DragGestureRecognizer {
        
        GestureRecognizer(Component component, DragHandler handler) {
            super(DragSource.getDefaultDragSource(), component, ops, handler);
        }
        
        void startDrag(int x, int y, int action, java.awt.event.MouseEvent evt) {
            appendEvent(evt);
            fireDragGestureRecognized(action, new Point(x, y));
        }
        
        protected void registerListeners() {}
        
        protected void unregisterListeners() {}
        
    }
    
    private class DragHandler implements DragGestureListener, DragSourceListener {
        
        public void dragGestureRecognized(DragGestureEvent dge) {
            Point p = dge.getDragOrigin();
            int action = dge.getDragAction();
            Transferable t = getTransferable(p.x, p.y, action);
            if (t != null) {
                dge.startDrag(new Cursor(Cursor.HAND_CURSOR), t, this);
            }
        }
        
        public void dragDropEnd(DragSourceDropEvent dsde) {
            DragSourceContext dsc = dsde.getDragSourceContext();
	    if (dsde.getDropSuccess()) {
                exportDone(dsc.getTransferable(), dsde.getDropAction());
	    } else {
                exportDone(null, DnDConstants.ACTION_NONE);
            }
        }
        
        public void dragEnter(DragSourceDragEvent dsde) {
        }
        
        public void dragExit(DragSourceEvent dse) {
        }
        
        public void dragOver(DragSourceDragEvent dsde) {
        }
        
        public void dropActionChanged(DragSourceDragEvent dsde) {
        }
        
    }
}
