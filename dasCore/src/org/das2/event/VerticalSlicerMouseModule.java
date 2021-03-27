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
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import org.das2.components.VerticalSpectrogramSlicer;
import org.das2.dataset.DataSetConsumer;
import org.das2.datum.Datum;
import static org.das2.event.MouseModule.logger;
import org.das2.graph.DasAxis;
import org.das2.graph.DasCanvasComponent;
import org.das2.graph.DasPlot;
import org.das2.graph.Renderer;
import org.das2.qds.QDataSet;
/**
 *
 * @author  jbf
 */
public class VerticalSlicerMouseModule extends MouseModule {
    
    private QDataSet ds;
    private DasAxis xaxis;
    private DasAxis yaxis;
    private Datum xlocation;
    
    private org.das2.dataset.DataSetConsumer dataSetConsumer;
    /** Creates a new instance of VerticalSlicerMouseModule */
    
    private DataPointSelectionEvent de;
    
    /** Utility field used by event firing mechanism. */
    private javax.swing.event.EventListenerList listenerList = new javax.swing.event.EventListenerList();;
    
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
        xlocation= xaxis.invTransform(e.getX());
        de.setDataSet(ds);
        
        fireDataPointSelectionListenerDataPointSelected(de);
    }
    
    @Override
    public String getDirections() {
        return "C to copy slice location to clipboard";
    }

    @Override
    public void keyTyped(KeyEvent keyEvent) {
        logger.log(Level.FINE, "keyTyped {0} {1}", new Object[]{keyEvent.getKeyChar(), keyEvent.isMetaDown()});
        if ( keyEvent.getKeyChar()=='c' ) { 
            VerticalSliceSelectionRenderer r= (VerticalSliceSelectionRenderer) super.dragRenderer;
            String text= xlocation.toString();
            StringSelection stringSelection = new StringSelection( text );
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, (Clipboard clipboard1, Transferable contents) -> {
            });
            logger.log(Level.FINE, "copied to mouse buffer: {0}", text);
        }
    }
    
    /** Registers DataPointSelectionListener to receive events.
     * @param listener The listener to register.
     */
    public void addDataPointSelectionListener(org.das2.event.DataPointSelectionListener listener) {
        listenerList.add(org.das2.event.DataPointSelectionListener.class, listener);
    }
    
    /** Removes DataPointSelectionListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public void removeDataPointSelectionListener(org.das2.event.DataPointSelectionListener listener) {
        listenerList.remove(org.das2.event.DataPointSelectionListener.class, listener);
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
    public VerticalSpectrogramSlicer getSlicer() {
        Object[] listeners;
        synchronized (this) {
            listeners = listenerList.getListenerList();
        }
        for ( int i=0; i<listeners.length; i++ ) {
            if ( listeners[i] instanceof VerticalSpectrogramSlicer ) {
                return (VerticalSpectrogramSlicer)listeners[i];
            }
        }
        throw new IllegalArgumentException("slicer not found.");
    }    
    
}
