/* File: HorizontalDragRangeSelectorMouseModule.java
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

import org.das2.dataset.TableDataSetConsumer;
import org.das2.graph.DasAxis;
import org.das2.graph.DasPlot;

/**
 *
 * @author  jbf
 */
public class HorizontalDragRangeSelectorMouseModule extends MouseModule {
    
    DasAxis axis;
    
    int start;
    
    /** Utility field used by event firing mechanism. */
    private javax.swing.event.EventListenerList listenerList =  null;
    
    private org.das2.dataset.DataSetConsumer dataSetConsumer;
    
    public HorizontalDragRangeSelectorMouseModule(DasPlot parent, org.das2.dataset.DataSetConsumer dataSetConsumer, DasAxis axis) {
        super( parent, new HorizontalDragRangeRenderer(parent), "Horizontal Drag Range" );
        if (!axis.isHorizontal()) {
            throw new IllegalArgumentException("Axis orientation is not horizontal");
        }
        this.dataSetConsumer= dataSetConsumer;        
        this.axis= axis;
        
    }
    
    public static HorizontalDragRangeSelectorMouseModule create(DasPlot parent) {
        HorizontalDragRangeSelectorMouseModule result=
        new HorizontalDragRangeSelectorMouseModule(parent,null,parent.getXAxis());
        return result;
    }
    
    public void mouseRangeSelected(MouseDragEvent e0) {
        MouseRangeSelectionEvent e= (MouseRangeSelectionEvent)e0;
        org.das2.datum.Datum min;
        org.das2.datum.Datum max;
        
        min= axis.invTransform(e.getMinimum());
        max= axis.invTransform(e.getMaximum());
        DataRangeSelectionEvent te=
        new DataRangeSelectionEvent(parent,min,max);
        fireDataRangeSelectionListenerDataRangeSelected(te);
    }
    
    /** Registers DataRangeSelectionListener to receive events.
     * @param listener The listener to register.
     */
    public synchronized void addDataRangeSelectionListener(org.das2.event.DataRangeSelectionListener listener) {
        if (listenerList == null ) {
            listenerList = new javax.swing.event.EventListenerList();
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
        if ( dataSetConsumer instanceof TableDataSetConsumer ) {
            event.setDataSet(dataSetConsumer.getConsumedDataSet());
        }
        if (listenerList == null) return;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==org.das2.event.DataRangeSelectionListener.class) {
                ((org.das2.event.DataRangeSelectionListener)listeners[i+1]).dataRangeSelected(event);
            }
        }
    }
    
}
