/* File: BoxRangeSelectorMouseModule.java
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

import org.das2.dataset.DataSetConsumer;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.graph.DasAxis;
import org.das2.graph.DasCanvasComponent;
import org.das2.graph.DasPlot;
import javax.swing.event.EventListenerList;

/**
 * //@deprecated  use BoxSelectorMouseModule 
 * @author  jbf
 */
public class BoxRangeSelectorMouseModule extends MouseModule {
    
    protected DasAxis xAxis;
    protected DasAxis yAxis;
    private DataSetConsumer consumer;
    
    /** Utility field used by event firing mechanism. */
    private EventListenerList listenerList =  new javax.swing.event.EventListenerList();
    
    /**
     * @param consumer is the source context of the data set selection
     */
    public BoxRangeSelectorMouseModule(DasCanvasComponent parent, DataSetConsumer consumer, DasAxis xAxis, DasAxis yAxis) {
        super( parent, new BoxGesturesRenderer(parent), "Box Selection" );
        if (!xAxis.isHorizontal()) {
            throw new IllegalArgumentException("X Axis orientation is not horizontal");
        }
        if (yAxis.isHorizontal()) {
            throw new IllegalArgumentException("Y Axis orientation is not vertical");
        }
        this.xAxis= xAxis;
        this.yAxis= yAxis;
        this.consumer = consumer;
    }
    
    public static BoxRangeSelectorMouseModule create(DasPlot parent) {
        BoxRangeSelectorMouseModule result=
        new BoxRangeSelectorMouseModule(parent, null, parent.getXAxis(),parent.getYAxis());
        return result;
    }
    
    public void mouseRangeSelected(MouseDragEvent e0) {
        if (e0 instanceof MouseBoxEvent) {
            MouseBoxEvent e= (MouseBoxEvent)e0;

            Datum xMin = xAxis.invTransform(e.getXMinimum());
            Datum xMax = xAxis.invTransform(e.getXMaximum());
            Datum yMin = yAxis.invTransform(e.getYMinimum());
            Datum yMax = yAxis.invTransform(e.getYMaximum());
            BoxSelectionEvent evt = new BoxSelectionEvent(this, new DatumRange( xMin, xMax ), new DatumRange( yMin, yMax) );
            if (consumer != null) {
                evt.setDataSet(consumer.getConsumedDataSet());
            }
            fireBoxSelected(evt);
        }
    }
    
    /** Registers DataRangeSelectionListener to receive events.
     * @param listener The listener to register.
     */
    public void addBoxSelectionListener(BoxSelectionListener listener) {
        listenerList.add(BoxSelectionListener.class, listener);
    }
    
    /** Removes DataRangeSelectionListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public void removeBoxSelectionListener(BoxSelectionListener listener) {
        listenerList.remove(BoxSelectionListener.class, listener);
    }
    
    
    /** Notifies all registered listeners about the event.
     *
     * @param event The event to be fired
     */
    protected void fireBoxSelected(BoxSelectionEvent event) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i] == BoxSelectionListener.class) {
                ((BoxSelectionListener)listeners[i+1]).boxSelected(event);
            }
        }
    }
    
}
