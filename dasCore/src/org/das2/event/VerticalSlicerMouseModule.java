/* File: VerticalSlicerMouseModule.java
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
import org.das2.graph.DasAxis;
import org.das2.graph.DasCanvasComponent;
import org.das2.graph.DasPlot;
import org.das2.graph.Renderer;
import org.virbo.dataset.QDataSet;
/**
 *
 * @author  jbf
 */
public class VerticalSlicerMouseModule extends MouseModule {
    
    private QDataSet ds;
    private DasAxis xaxis;
    private DasAxis yaxis;
    
    private org.das2.dataset.DataSetConsumer dataSetConsumer;
    /** Creates a new instance of VerticalSlicerMouseModule */
    
    private DataPointSelectionEvent de;
    
    /** Utility field used by event firing mechanism. */
    private javax.swing.event.EventListenerList listenerList =  null;
    
    public VerticalSlicerMouseModule( DasCanvasComponent parent,
    DataSetConsumer dataSetConsumer, DasAxis xaxis, DasAxis yaxis ) {
        super( parent, new VerticalSliceSelectionRenderer(parent), "Vertical Slice" );
        this.dataSetConsumer= dataSetConsumer;
        this.xaxis= xaxis;
        this.yaxis= yaxis;
        //TODO: this is silly, just create a new one each time...
        this.de= new DataPointSelectionEvent(this,null,null);
    }
    
    public static VerticalSlicerMouseModule create(DasPlot parent) {
        DasAxis xaxis= parent.getXAxis();
        DasAxis yaxis= parent.getYAxis();
        return new VerticalSlicerMouseModule(parent,null,xaxis,yaxis);
    }
    
    public static VerticalSlicerMouseModule create( Renderer renderer ) {
        DasPlot parent= renderer.getParent();
        return new VerticalSlicerMouseModule(parent,renderer,parent.getXAxis(),parent.getYAxis());
    }
    
    @Override
    public void mousePointSelected(MousePointSelectionEvent e) {
        de.birthMilli= System.currentTimeMillis();
        ds= dataSetConsumer.getConsumedDataSet();
        de.set(xaxis.invTransform(e.getX()),yaxis.invTransform(e.getY()));
        
        de.setDataSet(ds);
        
        fireDataPointSelectionListenerDataPointSelected(de);
    }
    
    /** Registers DataPointSelectionListener to receive events.
     * @param listener The listener to register.
     */
    public synchronized void addDataPointSelectionListener(org.das2.event.DataPointSelectionListener listener) {
        if (listenerList == null ) {
            listenerList = new javax.swing.event.EventListenerList();
        }
        listenerList.add(org.das2.event.DataPointSelectionListener.class, listener);
    }
    
    /** Removes DataPointSelectionListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public synchronized void removeDataPointSelectionListener(org.das2.event.DataPointSelectionListener listener) {
        listenerList.remove(org.das2.event.DataPointSelectionListener.class, listener);
    }
    
    /** Notifies all registered listeners about the event.
     *
     * @param event The event to be fired
     */
    private void fireDataPointSelectionListenerDataPointSelected(DataPointSelectionEvent event) {
        
        Object[] listeners;
        synchronized ( this ) {
            if (listenerList == null) return;
            listeners = listenerList.getListenerList();
        }
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==org.das2.event.DataPointSelectionListener.class) {
                ((org.das2.event.DataPointSelectionListener)listeners[i+1]).dataPointSelected(event);
            }
        }
    }
    
}
