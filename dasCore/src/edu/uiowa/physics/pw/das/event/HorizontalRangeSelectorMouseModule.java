/* File: HorizontalRangeSelectorMouseModule.java
 * Copyright (C) 2002-2003 University of Iowa
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
import edu.uiowa.physics.pw.das.graph.DasSpectrogramPlot;

import java.awt.*;
import java.util.Hashtable;
import java.util.Vector;

/**
 *
 * @author  jbf
 */
public class HorizontalRangeSelectorMouseModule extends MouseModule {
    
    DasAxis axis;
    private Hashtable hotSpotsMap;
    
    /** Utility field used by event firing mechanism. */
    private javax.swing.event.EventListenerList listenerList =  null;
    
    public HorizontalRangeSelectorMouseModule(DasCanvasComponent parent, DasAxis axis) {
        if (!axis.isHorizontal()) {
            throw new IllegalArgumentException("Axis orientation is not horizontal");
        }
        this.parent= parent;
        this.dragRenderer= new HorizontalRangeGesturesRenderer(parent);
        this.axis= axis;
        setLabel("Zoom X");
        hotSpotsMap= new Hashtable();
        
    }
    
    public Vector getHotSpots() {
        return null;
        /*
        if (!(axis instanceof DasTimeAxis)) {
            return null;
        } else {
            Enumeration e= hotSpotsMap.elements();
            while (e.hasMoreElements()) hotSpotsMap.remove(e.nextElement());
            Vector hotSpots= new Vector();
            int w= parent.getWidth();
            int h= parent.getHeight();
            Point l= parent.getLocation();
            DasColumn col= parent.getColumn();
            DasRow row= parent.getRow();
            Rectangle prev= new Rectangle(0,0,(int)col.getDMinimum()-l.x,h);
            Rectangle next= new Rectangle((int)col.getDMaximum()-l.x,0,w-((int)col.getDMaximum()-l.x),h);
            hotSpotsMap.put(prev,new MouseRangeGestureSelectionEvent(this,0,0,Gesture.SCANPREV));
            hotSpotsMap.put(next,new MouseRangeGestureSelectionEvent(this,0,0,Gesture.SCANNEXT));
            hotSpots.add(prev);
            hotSpots.add(next);
            return hotSpots;
        }
         */
    }
    
    public void hotSpotPressed(Shape hotSpot) {
        MouseRangeGestureSelectionEvent e= (MouseRangeGestureSelectionEvent)hotSpotsMap.get(hotSpot);
        mouseRangeSelected(e);
    }
    
    public static HorizontalRangeSelectorMouseModule create(DasPlot parent) {
        DasAxis axis= parent.getXAxis();
        HorizontalRangeSelectorMouseModule result=
        new HorizontalRangeSelectorMouseModule(parent,parent.getXAxis());
        return result;
    }
    
    public void mouseRangeSelected(MouseDragEvent e0) {
        MouseRangeSelectionEvent e= (MouseRangeSelectionEvent)e0;
        edu.uiowa.physics.pw.das.util.DasDie.println(""+getHotSpots());
        MouseRangeGestureSelectionEvent e1= (MouseRangeGestureSelectionEvent)e;
        edu.uiowa.physics.pw.das.datum.Datum min;
        edu.uiowa.physics.pw.das.datum.Datum max;
        if (!e1.isGesture()) {
            min= axis.invTransform(e.getMinimum());
            max= axis.invTransform(e.getMaximum());
            edu.uiowa.physics.pw.das.datum.Datum nnMin= axis.findTick(min,0,true);
            edu.uiowa.physics.pw.das.datum.Datum nnMax= axis.findTick(max,0,true);
            if (nnMin.equals(nnMax)) {
                min= axis.findTick(min,-1,true);
                max= axis.findTick(max,1,true);
            } else {
                min= nnMin;
                max= nnMax;
            }
            DataRangeSelectionEvent te=
            new DataRangeSelectionEvent(parent,min,max);
            fireDataRangeSelectionListenerDataRangeSelected(te);
        } else if (e1.isBack()) {
            axis.setDataRangePrev();
        } else if (e1.isZoomOut()) {
            axis.setDataRangeZoomOut();
        } else if (e1.isForward()) {
            axis.setDataRangeForward();
        } else if (e1.getGesture()==Gesture.SCANPREV) {
            /*
            Datum delta= ( axis.getDataMaximum().subtract(axis.getDataMinimum()) ).multiply(1.0);
            Datum tmin= axis.getDataMinimum().subtract(delta);
            Datum tmax= axis.getDataMaximum().subtract(delta);
            DataRangeSelectionEvent te=
            new DataRangeSelectionEvent(parent,tmin,tmax);
            fireDataRangeSelectionListenerDataRangeSelected(te);
             */
            axis.scanPrevious();
        } else if (e1.getGesture()==Gesture.SCANNEXT) {
            /*
            Datum delta= ( axis.getDataMaximum().subtract(axis.getDataMinimum()) ).multiply(1.0);
            Datum tmin= axis.getDataMinimum().add(delta);
            Datum tmax= axis.getDataMaximum().add(delta);
            DataRangeSelectionEvent te=
            new DataRangeSelectionEvent(parent,tmin,tmax);
            fireDataRangeSelectionListenerDataRangeSelected(te);
             */
            axis.scanNext();
        }
    }
    
    /** Registers DataRangeSelectionListener to receive events.
     * @param listener The listener to register.
     */
    public synchronized void addDataRangeSelectionListener(edu.uiowa.physics.pw.das.event.DataRangeSelectionListener listener) {
        if (listenerList == null ) {
            listenerList = new javax.swing.event.EventListenerList();
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
        if ( parent instanceof DasSpectrogramPlot ) {
            event.setDataSet(((DasSpectrogramPlot)parent).getData());
        }
        if (listenerList == null) return;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==edu.uiowa.physics.pw.das.event.DataRangeSelectionListener.class) {
                ((edu.uiowa.physics.pw.das.event.DataRangeSelectionListener)listeners[i+1]).DataRangeSelected(event);
            }
        }
    }
    
}
