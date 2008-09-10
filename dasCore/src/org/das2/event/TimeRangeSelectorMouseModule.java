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

package org.das2.event;

import org.das2.datum.DatumRange;
import org.das2.DasApplication;
import org.das2.datum.Datum;
import edu.uiowa.physics.pw.das.graph.DasAxis;
import edu.uiowa.physics.pw.das.graph.DasCanvasComponent;
import edu.uiowa.physics.pw.das.graph.DasPlot;
import org.das2.system.DasLogger;
import javax.swing.event.EventListenerList;


/**
 *
 * @author  jbf
 * @deprecated  Use HorizontalRangeSelectorMouseModule.
 */
public class TimeRangeSelectorMouseModule extends MouseModule {
    
    DasAxis timeAxis;
    
    /** Utility field used by event firing mechanism. */
    private EventListenerList listenerList =  null;
    
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
        Datum tmin;
        Datum tmax;
        
        if (!e0.isGesture()) {
            MouseRangeSelectionEvent e= (MouseRangeSelectionEvent)e0;
            Datum min= timeAxis.invTransform(e.getMinimum());
            Datum max= timeAxis.invTransform(e.getMaximum());
            
            Datum nnMin= timeAxis.findTick(min,0,true); // nearest neighbor
            Datum nnMax= timeAxis.findTick(max,0,true);
            if (nnMin.equals(nnMax)) {
                min= timeAxis.findTick(min,-1,true);
                max= timeAxis.findTick(max,1,true);
            } else {
                min= nnMin;
                max= nnMax;
            }
            TimeRangeSelectionEvent te= new TimeRangeSelectionEvent(parent,new DatumRange( min,max ) );
            fireTimeRangeSelectionListenerTimeRangeSelected(te);
        } else if (e0.getGesture()==Gesture.BACK) {
            timeAxis.setDataRangePrev();
        } else if (e0.getGesture()==Gesture.ZOOMOUT) {
            timeAxis.setDataRangeZoomOut();
        } else if (e0.getGesture()==Gesture.FORWARD) {
            timeAxis.setDataRangeForward();
        } else if (e0.getGesture()==Gesture.SCANPREV) {
            DatumRange range0= timeAxis.getDatumRange();
            TimeRangeSelectionEvent te= new TimeRangeSelectionEvent(parent, range0.previous() );
            fireTimeRangeSelectionListenerTimeRangeSelected(te);
        } else if (e0.getGesture()==Gesture.SCANNEXT) {
            DatumRange range0= timeAxis.getDatumRange();
            TimeRangeSelectionEvent te= new TimeRangeSelectionEvent(parent, range0.next() );
            fireTimeRangeSelectionListenerTimeRangeSelected(te);
        } else {
            throw new RuntimeException("unrecognized gesture: "+e0.getGesture());
        }
        
    }
    
    /** Registers TimeRangeSelectionListener to receive events.
     * @param listener The listener to register.
     */
    public synchronized void addTimeRangeSelectionListener(org.das2.event.TimeRangeSelectionListener listener) {
        if (listenerList == null ) {
            listenerList = new EventListenerList();
        }
        listenerList.add(org.das2.event.TimeRangeSelectionListener.class, listener);
    }
    
    /** Removes TimeRangeSelectionListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public synchronized void removeTimeRangeSelectionListener(org.das2.event.TimeRangeSelectionListener listener) {
        listenerList.remove(org.das2.event.TimeRangeSelectionListener.class, listener);
    }
    
    /** Notifies all registered listeners about the event.
     *
     * @param event The event to be fired
     */
    
    private void fireTimeRangeSelectionListenerTimeRangeSelected(TimeRangeSelectionEvent event) {
        if (listenerList == null) return;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==org.das2.event.TimeRangeSelectionListener.class) {
                String logmsg= "fire event: "+this.getClass().getName()+"-->"+listeners[i+1].getClass().getName()+" "+event;                
                DasLogger.getLogger( DasLogger.GUI_LOG ).fine(logmsg);
                ((org.das2.event.TimeRangeSelectionListener)listeners[i+1]).timeRangeSelected(event);
            }
        }
    }
    
}
