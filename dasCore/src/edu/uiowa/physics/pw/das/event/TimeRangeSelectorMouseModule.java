/* File: TimeRangeSelectorMouseModule.java
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

import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.graph.DasAxis;
import edu.uiowa.physics.pw.das.graph.DasCanvasComponent;
import edu.uiowa.physics.pw.das.graph.DasPlot;
import edu.uiowa.physics.pw.das.graph.DasAxis;

/**
 *
 * @author  jbf
 */
public class TimeRangeSelectorMouseModule extends MouseModule {
    
    DasAxis timeAxis;
    
    /** Utility field used by event firing mechanism. */
    private javax.swing.event.EventListenerList listenerList =  null;
    
    public String getLabel() { return "X Time Zoom"; }
    
    public TimeRangeSelectorMouseModule(DasCanvasComponent parent, DasAxis timeAxis) {
        this.parent= parent;
        this.dragRenderer= new HorizontalRangeGesturesRenderer(parent);
        this.timeAxis= timeAxis;
    }
    
    public static TimeRangeSelectorMouseModule create(DasPlot parent) {
        DasAxis axis= parent.getXAxis();
        TimeRangeSelectorMouseModule result=null;
        result= new TimeRangeSelectorMouseModule(parent, parent.getXAxis());
        return result;
    }
    
     public void mouseRangeSelected(MouseRangeSelectionEvent e0) {
        MouseRangeSelectionEvent e= (MouseRangeSelectionEvent)e0;
        MouseRangeGestureSelectionEvent e1= (MouseRangeGestureSelectionEvent)e;
        edu.uiowa.physics.pw.das.datum.Datum tmin;
        edu.uiowa.physics.pw.das.datum.Datum tmax;
        if (!e1.isGesture()) {
            edu.uiowa.physics.pw.das.datum.Datum min= timeAxis.invTransform(e.getMinimum());
            edu.uiowa.physics.pw.das.datum.Datum max= timeAxis.invTransform(e.getMaximum());
            edu.uiowa.physics.pw.das.datum.Datum nnMin= timeAxis.findTick(min,0,true); // nearest neighbor
            edu.uiowa.physics.pw.das.datum.Datum nnMax= timeAxis.findTick(max,0,true);
            if (nnMin.equals(nnMax)) {
                min= timeAxis.findTick(min,-1,true);
                max= timeAxis.findTick(max,1,true);
            } else {
                min= nnMin;
                max= nnMax;
            }
            TimeRangeSelectionEvent te=
            new TimeRangeSelectionEvent(parent,(edu.uiowa.physics.pw.das.datum.Datum)min,(edu.uiowa.physics.pw.das.datum.Datum)max);
            fireTimeRangeSelectionListenerTimeRangeSelected(te);
        } else if (e1.isBack()) {
            timeAxis.setDataRangePrev();
        } else if (e1.isZoomOut()) {
            timeAxis.setDataRangeZoomOut();
        } else if (e1.isForward()) {
            timeAxis.setDataRangeForward();
        } else if (e1.getGesture()==Gesture.SCANPREV) { 
            edu.uiowa.physics.pw.das.datum.Datum delta= ( timeAxis.getDataMaximum().subtract(timeAxis.getDataMinimum()) ).multiply(0.9);
            tmin= timeAxis.getDataMinimum().subtract(delta);
            tmax= timeAxis.getDataMaximum().subtract(delta);            
            TimeRangeSelectionEvent te=
            new TimeRangeSelectionEvent(parent,(edu.uiowa.physics.pw.das.datum.Datum)tmin,(edu.uiowa.physics.pw.das.datum.Datum)tmax);
            fireTimeRangeSelectionListenerTimeRangeSelected(te);
        } else if (e1.getGesture()==Gesture.SCANNEXT) {
            edu.uiowa.physics.pw.das.datum.Datum delta= ( timeAxis.getDataMaximum().subtract(timeAxis.getDataMinimum()) ).multiply(0.9);
            tmin= timeAxis.getDataMinimum().add(delta);
            tmax= timeAxis.getDataMaximum().add(delta);            
            TimeRangeSelectionEvent te=
            new TimeRangeSelectionEvent(parent,(edu.uiowa.physics.pw.das.datum.Datum)tmin,(edu.uiowa.physics.pw.das.datum.Datum)tmax);
            fireTimeRangeSelectionListenerTimeRangeSelected(te);
        } else {
            edu.uiowa.physics.pw.das.util.DasDie.println(e1.getGesture());
        }
        
    }
    
    /** Registers TimeRangeSelectionListener to receive events.
     * @param listener The listener to register.
     */
    public synchronized void addTimeRangeSelectionListener(edu.uiowa.physics.pw.das.event.TimeRangeSelectionListener listener) {
        if (listenerList == null ) {
            listenerList = new javax.swing.event.EventListenerList();
        }
        listenerList.add(edu.uiowa.physics.pw.das.event.TimeRangeSelectionListener.class, listener);
    }
    
    /** Removes TimeRangeSelectionListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public synchronized void removeTimeRangeSelectionListener(edu.uiowa.physics.pw.das.event.TimeRangeSelectionListener listener) {
        listenerList.remove(edu.uiowa.physics.pw.das.event.TimeRangeSelectionListener.class, listener);
    }
    
    /** Notifies all registered listeners about the event.
     *
     * @param event The event to be fired
     */
    
    private void fireTimeRangeSelectionListenerTimeRangeSelected(TimeRangeSelectionEvent event) {
        if (listenerList == null) return;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==edu.uiowa.physics.pw.das.event.TimeRangeSelectionListener.class) {
                edu.uiowa.physics.pw.das.util.DasDie.println("fire event: "+this.getClass().getName()+"-->"+listeners[i+1].getClass().getName()+" "+event);
                ((edu.uiowa.physics.pw.das.event.TimeRangeSelectionListener)listeners[i+1]).TimeRangeSelected(event);
            }
        }
    }
    
}
