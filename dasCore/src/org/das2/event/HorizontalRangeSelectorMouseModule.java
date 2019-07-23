/* File: HorizontalRangeSelectorMouseModule.java
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

import java.awt.Point;
import java.awt.event.MouseWheelEvent;
import java.util.logging.Level;
import javax.swing.SwingUtilities;
import org.das2.datum.DatumRange;
import org.das2.graph.DasAxis;
import org.das2.graph.DasCanvasComponent;
import org.das2.graph.DasPlot;
import javax.swing.event.EventListenerList;
import org.das2.datum.DatumRangeUtil;
import static org.das2.event.MouseModule.logger;


/**
 *
 * @author  jbf
 */
public class HorizontalRangeSelectorMouseModule extends MouseModule {
    
    DasAxis axis;   
    long t0, tbirth;
    
    /** Utility field used by event firing mechanism. */
    private EventListenerList listenerList = new EventListenerList();
    
    public HorizontalRangeSelectorMouseModule(DasCanvasComponent parent, DasAxis axis) {
        super(parent,new HorizontalRangeGesturesRenderer(parent),"Zoom X");
        if (!axis.isHorizontal()) {
            throw new IllegalArgumentException("Axis orientation is not horizontal");
        }        
        this.axis= axis;
    }
   
    public static HorizontalRangeSelectorMouseModule create(DasPlot parent) {
        HorizontalRangeSelectorMouseModule result=
        new HorizontalRangeSelectorMouseModule(parent,parent.getXAxis());
        return result;
    }
    
    @Override
    public void mouseRangeSelected(MouseDragEvent e0) {                        
        if (!e0.isGesture()) {
            if ( !( e0 instanceof MouseRangeSelectionEvent ) ) {
                throw new IllegalArgumentException("Event should be MouseRangeSelectionEvent"); // findbugs
            }
            MouseRangeSelectionEvent e= (MouseRangeSelectionEvent)e0;
            DatumRange dr= axis.invTransform(e.getMinimum(),e.getMaximum());
            DatumRange nndr= axis.getTickV().enclosingRange(dr, true);
            DataRangeSelectionEvent te=
            new DataRangeSelectionEvent(e0.getSource(),nndr.min(),nndr.max());
            fireDataRangeSelectionListenerDataRangeSelected(te);
        } else if ( e0.getGesture()==Gesture.BACK ) {
            axis.setDataRangePrev();
        } else if ( e0.getGesture()==Gesture.ZOOMOUT ) {
            axis.setDataRangeZoomOut();
        } else if ( e0.getGesture()==Gesture.FORWARD ) {
            axis.setDataRangeForward();
        } else if ( e0.getGesture()==Gesture.SCANPREV) {
            axis.scanPrevious();
        } else if ( e0.getGesture()==Gesture.SCANNEXT) {
            axis.scanNext();
        } else {
            logger.log(Level.FINE, "unsupported gesture: {0}", e0.getGesture());
        }
    }

    
    /**
     * mouse wheel events zoom or pan rapidly.  With a physical wheel, I (jbf) found
     * that I get 17ms per click, and this is manageable.  With a touchpad on a mac,
     * these events come much faster, like 10ms per click, which can disorient the
     * operator.  So we limit the speed to 20ms per click, for now by dropping
     * rapid clicks.
     *
     * @param e
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        double nmin, nmax; 
        
        double shift = 0.;
        
        if ( (e.isControlDown() )) {
            if (e.getWheelRotation() < 0) {
                nmin = -0.20; // pan left on xaxis
                nmax = +0.80;
            } else {
                nmin = +0.20; // pan right on xaxis
                nmax = +1.20;
            }
        } else if ( e.isShiftDown() ) { 
            if (e.getWheelRotation() < 0) {
                nmin = -0.005; // pan left on xaxis
                nmax = +0.995;
            } else {
                nmin = +0.005; // pan right on xaxis
                nmax = +1.005;
            }
        } else {
            //mac trackpads coast a while after release, so let's govern the speed a little more
            if (e.getWheelRotation() < 0) {
                nmin = 0.10; // zoom in
                nmax = 0.90;
            } else {
                nmin = -0.125; // zoom out
                nmax = 1.125;
            }
        }
        
        Point ep= SwingUtilities.convertPoint( e.getComponent(), e.getPoint(), parent.getCanvas() );
            
        Pos xpos = axis == null ? Pos._null : position( axis.getColumn(), ep.x, 20 );
        switch (xpos) {
            case min:
                shift = -nmin; // this will cancel out nmin
                break;
            case max:
                shift = nmin;
                break;
            default:
                logger.log(Level.FINE, "xpos: {0}", xpos);
        }
                    
        int clickMag = 1;
        final long t1 = System.nanoTime();
        long limitNanos = (long) 40e6;
        if ((t1 - t0) / clickMag < limitNanos) {
            clickMag = (int) Math.floor( (double)(t1 - t0) / limitNanos );
        }

        if (clickMag == 0) return;
        t0 = System.nanoTime();

        // these will be non-null if they should be used.
        DatumRange xdrnew;

        logger.log(Level.FINEST, ":ns:  {0}  {1}", new Object[]{System.nanoTime() - tbirth, clickMag});
        if ( true ) {
            DatumRange dr = axis.getDatumRange();
            for (int i = 0; i < clickMag; i++) {
                if (axis.isLog()) {
                    dr = DatumRangeUtil.rescaleLog(dr, nmin+shift, nmax+shift);
                } else {
                    dr = DatumRangeUtil.rescale(dr, nmin+shift, nmax+shift);
                }
            }
            dr= maybeRound( axis, dr );
            
            if ( ! DatumRangeUtil.isAcceptable( dr, axis.isLog() ) ) {
                dr= null;
            }
            xdrnew= dr;
        }
        
        if ( axisIsAdjustable(axis) && xdrnew==null ) return;

        if ( axisIsAdjustable(axis) ) axis.setDatumRange(xdrnew);

        super.mouseWheelMoved(e);
    }

    
    /** Registers DataRangeSelectionListener to receive events.
     * @param listener The listener to register.
     */
    public void addDataRangeSelectionListener(org.das2.event.DataRangeSelectionListener listener) {
        listenerList.add(org.das2.event.DataRangeSelectionListener.class, listener);
    }
    
    /** Removes DataRangeSelectionListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public void removeDataRangeSelectionListener(org.das2.event.DataRangeSelectionListener listener) {
        listenerList.remove(org.das2.event.DataRangeSelectionListener.class, listener);
    }
    
    
    /** Notifies all registered listeners about the event.
     *
     * @param event The event to be fired
     */
    private void fireDataRangeSelectionListenerDataRangeSelected(DataRangeSelectionEvent event) {
        Object[] listeners;
        listeners = listenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==org.das2.event.DataRangeSelectionListener.class) {
                ((org.das2.event.DataRangeSelectionListener)listeners[i+1]).dataRangeSelected(event);
            }
        }
    }
    
}
