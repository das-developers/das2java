/* File: HorizontalSlicerMouseModule.java
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

import edu.uiowa.physics.pw.das.dataset.DataSet;
import edu.uiowa.physics.pw.das.dataset.DataSetConsumer;
import edu.uiowa.physics.pw.das.dataset.XTaggedYScanDataSetConsumer;
import edu.uiowa.physics.pw.das.graph.DasAxis;
import edu.uiowa.physics.pw.das.graph.DasPlot;
import edu.uiowa.physics.pw.das.graph.Renderer;
/**
 *
 * @author  jbf
 */
public class HorizontalSlicerMouseModule extends MouseModule {
    
    private DasAxis xaxis;
    private DasAxis yaxis;
    
    private edu.uiowa.physics.pw.das.dataset.XTaggedYScanDataSetConsumer dataSetConsumer;
    
    /** Creates a new instance of VerticalSlicerMouseModule */
    
    private DataPointSelectionEvent de;
    
    /** Utility field used by event firing mechanism. */
    private javax.swing.event.EventListenerList listenerList =  null;
    
    public HorizontalSlicerMouseModule(DasPlot parent, edu.uiowa.physics.pw.das.dataset.XTaggedYScanDataSetConsumer dataSetConsumer, DasAxis xaxis, DasAxis yaxis) {
        this( parent, (edu.uiowa.physics.pw.das.dataset.DataSetConsumer)dataSetConsumer, xaxis, yaxis );
    }
    
    protected HorizontalSlicerMouseModule(DasPlot parent, edu.uiowa.physics.pw.das.dataset.DataSetConsumer dataSetConsumer, DasAxis xaxis, DasAxis yaxis) {
        this.parent= parent;
        
        if (!(dataSetConsumer instanceof edu.uiowa.physics.pw.das.dataset.XTaggedYScanDataSetConsumer)) {
            throw new IllegalArgumentException("dataSetConsumer must be an XTaggedYScanDataSetConsumer");
        }
        this.dataSetConsumer= (edu.uiowa.physics.pw.das.dataset.XTaggedYScanDataSetConsumer)dataSetConsumer;
        this.xaxis= xaxis;
        this.yaxis= yaxis;
        this.dragRenderer= new HorizontalSliceSelectionRenderer(parent);
        this.de= new DataPointSelectionEvent(this,null,null);        
        setLabel("Horizontal Slice");
    }
    
    public static HorizontalSlicerMouseModule create(DasPlot parent) {
        DasAxis xaxis= parent.getXAxis();
        DasAxis yaxis= parent.getYAxis();
        return new HorizontalSlicerMouseModule(parent,parent,xaxis,yaxis);
    }
    
    public static HorizontalSlicerMouseModule create(Renderer renderer)
    {
        DasPlot parent= renderer.getParent();
        DasAxis xaxis= parent.getXAxis();
        DasAxis yaxis= parent.getYAxis();
        return new HorizontalSlicerMouseModule(parent,renderer,xaxis,yaxis);
    }
    
    public void mousePointSelected(MousePointSelectionEvent e) {
        edu.uiowa.physics.pw.das.dataset.DataSet ds= dataSetConsumer.getDataSet();
        de.setDataSet(ds);
        de.set(xaxis.invTransform(e.getX()),yaxis.invTransform(e.getY()));

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
                ((edu.uiowa.physics.pw.das.event.DataPointSelectionListener)listeners[i+1]).DataPointSelected(event);
            }
        }
    }
    
}
