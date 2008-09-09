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

package edu.uiowa.physics.pw.das.event;

import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import edu.uiowa.physics.pw.das.graph.DasAxis;
import edu.uiowa.physics.pw.das.graph.DasCanvasComponent;
import edu.uiowa.physics.pw.das.graph.DasPlot;
import javax.swing.event.EventListenerList;

/**
 *
 * @author  jbf
 */
public class VerticalRangeSelectorMouseModule extends MouseModule {
    
    DasAxis axis;
    
    /** Utility field used by event firing mechanism. */
    private EventListenerList listenerList =  null;
    
    public String getLabel() { return "Zoom Y"; };
    
    public VerticalRangeSelectorMouseModule(DasCanvasComponent parent, DasAxis axis) {
        if (axis.isHorizontal()) {
            throw new IllegalArgumentException("Axis orientation is not vertical");
        }
        this.parent= parent;
        //  this.dragRenderer= (DragRenderer)HorizontalRangeRenderer.renderer;
        this.dragRenderer= new VerticalRangeGesturesRenderer(parent);
        this.axis= axis;
    }
    
    public static VerticalRangeSelectorMouseModule create(DasPlot parent) {
        DasAxis axis= parent.getYAxis();
        VerticalRangeSelectorMouseModule result=
        new VerticalRangeSelectorMouseModule(parent,parent.getYAxis());
        return result;
    }
    
    public void mouseRangeSelected(MouseDragEvent e0) {                
        if (!e0.isGesture()) {
            Datum min;
            Datum max;
            Datum nnMin;
            Datum nnMax;
            MouseRangeSelectionEvent e= (MouseRangeSelectionEvent)e0;
            min= axis.invTransform(e.getMaximum());
            max= axis.invTransform(e.getMinimum());
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
    public synchronized void addDataRangeSelectionListener(edu.uiowa.physics.pw.das.event.DataRangeSelectionListener listener) {
        if (listenerList == null ) {
            listenerList = new EventListenerList();
        }
        listenerList.add(edu.uiowa.physics.pw.das.event.DataRangeSelectionListener.class, listener);
    }
    
    /** Removes DataRangeSelectionListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public synchronized void removeDataRangeSelectionListener(edu.uiowa.physics.pw.das.event.DataRangeSelectionListener listener) {
        listenerList.remove(edu.uiowa.physics.pw.das.event.DataRangeSelectionListener.class, listener);
    }
    
    /** Notifies all registered listeners about the event.
     *
     * @param event The event to be fired
     */
    private void fireDataRangeSelectionListenerDataRangeSelected(DataRangeSelectionEvent event) {
        if (listenerList == null) return;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==edu.uiowa.physics.pw.das.event.DataRangeSelectionListener.class) {
                ((edu.uiowa.physics.pw.das.event.DataRangeSelectionListener)listeners[i+1]).dataRangeSelected(event);
            }
        }
    }
    
}
