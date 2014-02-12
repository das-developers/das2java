/* File: VerticalRangeSelectorMouseModule.java
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

import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.graph.DasAxis;
import org.das2.graph.DasCanvasComponent;
import org.das2.graph.DasPlot;
import javax.swing.event.EventListenerList;

/**
 *
 * @author  jbf
 */
public class VerticalRangeSelectorMouseModule extends MouseModule {
    
    DasAxis axis;
    
    /** Utility field used by event firing mechanism. */
    private EventListenerList listenerList =  null;
    
    @Override
    public String getLabel() { return "Zoom Y"; };
    
    public VerticalRangeSelectorMouseModule(DasCanvasComponent parent, DasAxis axis) {
        if (axis.isHorizontal()) {
            throw new IllegalArgumentException("Axis orientation is not vertical");
        }
        this.parent= parent;
        this.dragRenderer= new VerticalRangeGesturesRenderer(parent);
        this.axis= axis;
    }
    
    public static VerticalRangeSelectorMouseModule create(DasPlot parent) {
        VerticalRangeSelectorMouseModule result=
        new VerticalRangeSelectorMouseModule(parent,parent.getYAxis());
        return result;
    }
    
    @Override
    public void mouseRangeSelected(MouseDragEvent e0) {                
        if (!e0.isGesture()) {
            Datum min;
            Datum max;
            if ( !( e0 instanceof MouseRangeSelectionEvent ) ) {
                throw new IllegalArgumentException("Event should be MouseRangeSelectionEvent"); // findbugs
            }
            MouseRangeSelectionEvent e= (MouseRangeSelectionEvent)e0;
            min= axis.invTransform(e.getMaximum());
            max= axis.invTransform(e.getMinimum());
            if ( min.gt(max) ) {
                Datum t= min;
                min= max;
                max= t;
            }
            DatumRange dr= new DatumRange( min, max );
            DatumRange nndr= axis.getTickV().enclosingRange(dr, true);
            DataRangeSelectionEvent te=
            new DataRangeSelectionEvent(parent,nndr.min(),nndr.max());
            fireDataRangeSelectionListenerDataRangeSelected(te);
        } else if (e0.getGesture()==Gesture.BACK) {
            axis.setDataRangePrev();
        } else if (e0.getGesture()==Gesture.ZOOMOUT) {
            axis.setDataRangeZoomOut();
        } else if (e0.getGesture()==Gesture.FORWARD) {
            axis.setDataRangeForward();
        } else {
        }
    }
    
    /** Registers DataRangeSelectionListener to receive events.
     * @param listener The listener to register.
     */
    public synchronized void addDataRangeSelectionListener(org.das2.event.DataRangeSelectionListener listener) {
        if (listenerList == null ) {
            listenerList = new EventListenerList();
        }
        listenerList.add(org.das2.event.DataRangeSelectionListener.class, listener);
    }
    
    /** Removes DataRangeSelectionListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public synchronized void removeDataRangeSelectionListener(org.das2.event.DataRangeSelectionListener listener) {
        listenerList.remove(org.das2.event.DataRangeSelectionListener.class, listener);
    }
    
    /** Notifies all registered listeners about the event.
     *
     * @param event The event to be fired
     */
    private void fireDataRangeSelectionListenerDataRangeSelected(DataRangeSelectionEvent event) {
        Object[] listeners;
        synchronized (this) {
            if (listenerList == null) return;
            listeners = listenerList.getListenerList();
        }
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==org.das2.event.DataRangeSelectionListener.class) {
                ((org.das2.event.DataRangeSelectionListener)listeners[i+1]).dataRangeSelected(event);
            }
        }
    }
    
}
