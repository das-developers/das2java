/* File: HorizontalSlicerMouseModule.java
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

import org.das2.components.HorizontalSpectrogramSlicer;
import org.das2.dataset.TableDataSetConsumer;
import org.das2.dataset.DataSetConsumer;
import org.das2.graph.DasAxis;
import org.das2.graph.DasPlot;
import org.das2.graph.Renderer;
import org.das2.qds.QDataSet;
/**
 * Slices spectrogram horizontally, e.g. showing one channel vs time.
 * @author  jbf
 */
public class HorizontalSlicerMouseModule extends MouseModule {
    
    private DasAxis xaxis;
    private DasAxis yaxis;
    
    private TableDataSetConsumer dataSetConsumer;
    
    /** Creates a new instance of VerticalSlicerMouseModule */
    
    private DataPointSelectionEvent de;
    
    /** Utility field used by event firing mechanism. */
    private javax.swing.event.EventListenerList listenerList = new javax.swing.event.EventListenerList();
    
    public HorizontalSlicerMouseModule(DasPlot parent, TableDataSetConsumer dataSetConsumer, DasAxis xaxis, DasAxis yaxis) {
        this( parent, (DataSetConsumer)dataSetConsumer, xaxis, yaxis );
    }
    
    protected HorizontalSlicerMouseModule(DasPlot parent, DataSetConsumer dataSetConsumer, DasAxis xaxis, DasAxis yaxis) {
        super( parent, new HorizontalSliceSelectionRenderer(parent), "Horizontal Slice" );
        
        if (!(dataSetConsumer instanceof TableDataSetConsumer)) {
            throw new IllegalArgumentException("dataSetConsumer must be an XTaggedYScanDataSetConsumer");
        }
        this.dataSetConsumer= ( TableDataSetConsumer)dataSetConsumer;
        this.xaxis= xaxis;
        this.yaxis= yaxis;
        this.de= new DataPointSelectionEvent(this,null,null);                
    }
    
    public static HorizontalSlicerMouseModule create(DasPlot parent) {
        DasAxis xaxis= parent.getXAxis();
        DasAxis yaxis= parent.getYAxis();
        return new HorizontalSlicerMouseModule(parent,null,xaxis,yaxis);
    }
    
    public static HorizontalSlicerMouseModule create(Renderer renderer)
    {
        DasPlot parent= renderer.getParent();
        DasAxis xaxis= parent.getXAxis();
        DasAxis yaxis= parent.getYAxis();
        return new HorizontalSlicerMouseModule(parent,renderer,xaxis,yaxis);
    }
    
    @Override
    public void mousePointSelected(MousePointSelectionEvent e) {
        QDataSet ds= dataSetConsumer.getConsumedDataSet();
        de.setDataSet(ds);
        de.set(xaxis.invTransform(e.getX()),yaxis.invTransform(e.getY()));

        fireDataPointSelectionListenerDataPointSelected(de);
    }
    
    /** Registers DataPointSelectionListener to receive events.
     * @param listener The listener to register.
     */
    public void addDataPointSelectionListener( DataPointSelectionListener listener) {
        listenerList.add(org.das2.event.DataPointSelectionListener.class, listener);
    }
    
    /** Removes DataPointSelectionListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public void removeDataPointSelectionListener( DataPointSelectionListener listener) {
        listenerList.remove( DataPointSelectionListener.class, listener);
    }
    
    /** Notifies all registered listeners about the event.
     *
     * @param event The event to be fired
     */
    private void fireDataPointSelectionListenerDataPointSelected(DataPointSelectionEvent event) {
        Object[] listeners;
        listeners = listenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==org.das2.event.DataPointSelectionListener.class) {
                ((org.das2.event.DataPointSelectionListener)listeners[i+1]).dataPointSelected(event);
            }
        }
    }
    
    /**
     * return the slicer listening to the slices.  This returns the 
     * first one found.  
     * @return the slicer
     * @throws IllegalArgumentException if no slicer is found.
     */
    public HorizontalSpectrogramSlicer getSlicer() {
        Object[] listeners;
        synchronized (this) {
            listeners = listenerList.getListenerList();
        }
        for ( int i=0; i<listeners.length; i++ ) {
            if ( listeners[i] instanceof HorizontalSpectrogramSlicer ) {
                return (HorizontalSpectrogramSlicer)listeners[i];
            }
        }
        throw new IllegalArgumentException("slicer not found.");
    }
}
