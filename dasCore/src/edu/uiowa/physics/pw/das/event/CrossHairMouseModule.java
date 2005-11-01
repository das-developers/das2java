/* File: CrossHairMouseModule.java
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
import edu.uiowa.physics.pw.das.graph.DasAxis;
import edu.uiowa.physics.pw.das.graph.DasPlot;


/**
 *
 * @author  Owner
 */
public class CrossHairMouseModule extends MouseModule {
    
    DasAxis xaxis;
    DasAxis yaxis;
    
    protected DataPointSelectionEvent de;
    
    edu.uiowa.physics.pw.das.dataset.DataSetConsumer dataSetConsumer;
    
    /** Utility field used by event firing mechanism. */
    private javax.swing.event.EventListenerList listenerList =  null;
    
    public CrossHairMouseModule(DasPlot parent, DasAxis xaxis, DasAxis yaxis) {
        this( parent, parent, xaxis, yaxis );
    }
    
    public CrossHairMouseModule( DasPlot parent, edu.uiowa.physics.pw.das.dataset.DataSetConsumer dataSetConsumer, DasAxis xAxis, DasAxis yAxis ) {
        super(parent,new CrossHairRenderer(parent,dataSetConsumer,xAxis,yAxis),"Crosshair Digitizer");
        this.dataSetConsumer= dataSetConsumer;
        this.xaxis= xAxis;
        this.yaxis= yAxis;
        this.de= new DataPointSelectionEvent(this,null,null);
    }
    
    public static CrossHairMouseModule create( DasPlot parent ) {
        DasAxis xaxis= parent.getXAxis();
        DasAxis yaxis= parent.getYAxis();
        return new CrossHairMouseModule(parent,xaxis,yaxis);
    }
    
    protected DataPointSelectionEvent getDataPointSelectionEvent(MousePointSelectionEvent e) {
        de.setDataSet(dataSetConsumer.getDataSet());
        de.set(xaxis.invTransform(e.getX()),yaxis.invTransform(e.getY()));
        return de;
    }
    
    public void mousePointSelected(MousePointSelectionEvent e) {
        fireDataPointSelectionListenerDataPointSelected(getDataPointSelectionEvent(e));
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
    protected void fireDataPointSelectionListenerDataPointSelected(DataPointSelectionEvent event) {
        if (listenerList == null) return;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==edu.uiowa.physics.pw.das.event.DataPointSelectionListener.class) {
                ((edu.uiowa.physics.pw.das.event.DataPointSelectionListener)listeners[i+1]).DataPointSelected(event);
            }
        }
    }
    
}
