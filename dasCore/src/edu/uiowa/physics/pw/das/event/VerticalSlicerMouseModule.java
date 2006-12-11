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

package edu.uiowa.physics.pw.das.event;
import edu.uiowa.physics.pw.das.dataset.DataSetConsumer;
import edu.uiowa.physics.pw.das.graph.DasAxis;
import edu.uiowa.physics.pw.das.graph.DasCanvasComponent;
import edu.uiowa.physics.pw.das.graph.DasPlot;
import edu.uiowa.physics.pw.das.graph.Renderer;
/**
 *
 * @author  jbf
 */
public class VerticalSlicerMouseModule extends MouseModule {
    
    private edu.uiowa.physics.pw.das.dataset.DataSet ds;
    double offset;
    private DasAxis xaxis;
    private DasAxis yaxis;
    
    private edu.uiowa.physics.pw.das.dataset.DataSetConsumer dataSetConsumer;
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
        return new VerticalSlicerMouseModule(parent,parent,xaxis,yaxis);
    }
    
    public static VerticalSlicerMouseModule create( Renderer renderer ) {
        DasPlot parent= renderer.getParent();
        return new VerticalSlicerMouseModule(parent,renderer,parent.getXAxis(),parent.getYAxis());
    }
    
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
    public synchronized void addDataPointSelectionListener(edu.uiowa.physics.pw.das.event.DataPointSelectionListener listener) {
        if (listenerList == null ) {
            listenerList = new javax.swing.event.EventListenerList();
        }
        listenerList.add(edu.uiowa.physics.pw.das.event.DataPointSelectionListener.class, listener);
    }
    
    /** Removes DataPointSelectionListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public synchronized void removeDataPointSelectionListener(edu.uiowa.physics.pw.das.event.DataPointSelectionListener listener) {
        listenerList.remove(edu.uiowa.physics.pw.das.event.DataPointSelectionListener.class, listener);
    }
    
    /** Notifies all registered listeners about the event.
     *
     * @param event The event to be fired
     */
    private void fireDataPointSelectionListenerDataPointSelected(DataPointSelectionEvent event) {
        if (listenerList == null) return;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==edu.uiowa.physics.pw.das.event.DataPointSelectionListener.class) {
                ((edu.uiowa.physics.pw.das.event.DataPointSelectionListener)listeners[i+1]).dataPointSelected(event);
            }
        }
    }
    
}
