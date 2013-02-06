/* File: DataPointReporter.java
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

package org.das2.components;

import org.das2.event.DataPointSelectionEvent;


/**
 *
 * @author  Owner
 */
public class DataPointReporter extends javax.swing.JPanel implements org.das2.event.DataPointSelectionListener {
    
    private javax.swing.JTextField output;    
    
    /** Creates a new instance of DataPointReporter */
    public DataPointReporter() {
        super();
        this.setLayout(new java.awt.FlowLayout()); 
        output= new javax.swing.JTextField(20);         
        this.add(output);
        
    }
    
    public void dataPointSelected(org.das2.event.DataPointSelectionEvent e) {
        output.setText("("+e.getX()+","+e.getY()+")");
        fireDataPointSelectionListenerDataPointSelected(e);       
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
        if (listenerList == null) return;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==org.das2.event.DataPointSelectionListener.class) {
                ((org.das2.event.DataPointSelectionListener)listeners[i+1]).dataPointSelected(event);
            }
        }
    }
}
