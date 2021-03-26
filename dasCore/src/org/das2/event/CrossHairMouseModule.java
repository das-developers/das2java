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

package org.das2.event;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import org.das2.graph.DasAxis;
import org.das2.graph.DasPlot;
import org.das2.graph.Renderer;
import org.das2.qds.QDataSet;

/**
 *
 * @author  Owner
 */
public class CrossHairMouseModule extends MouseModule {
    
    private DasAxis xaxis;
    private DasAxis yaxis;
    private DasPlot plot;
    
    protected DataPointSelectionEvent de;
    
    org.das2.dataset.DataSetConsumer dataSetConsumer;
    
    /** Utility field used by event firing mechanism. */
    private javax.swing.event.EventListenerList listenerList = new javax.swing.event.EventListenerList();
    
    public CrossHairMouseModule(DasPlot parent, DasAxis xaxis, DasAxis yaxis) {
        this( parent, null, xaxis, yaxis );
    }
    
    public CrossHairMouseModule( DasPlot parent, org.das2.dataset.DataSetConsumer dataSetConsumer, DasAxis xAxis, DasAxis yAxis ) {
        super(parent,new CrossHairRenderer(parent,dataSetConsumer,xAxis,yAxis),"Crosshair Digitizer");
        this.plot= parent;
        this.dataSetConsumer= dataSetConsumer;
        this.xaxis= xAxis;
        this.yaxis= yAxis;
        this.de= new DataPointSelectionEvent(this,null,null);
    }
    
    public static CrossHairMouseModule create( DasPlot parent ) {
        DasAxis xaxis= null;
        DasAxis yaxis= null;
        return new CrossHairMouseModule(parent,null,xaxis,yaxis);
    }
    
    private QDataSet getContextDataSet() {
        QDataSet ds;
        if ( dataSetConsumer!=null ) {
            ds = dataSetConsumer.getConsumedDataSet();
        } else {
            Renderer[] rends= ((DasPlot)this.parent).getRenderers();
            if ( rends.length>0 ) {
                ds= rends[0].getConsumedDataSet();
            } else {
                ds= null;
            }
        }
        return ds;
    }
    protected DataPointSelectionEvent getDataPointSelectionEvent(MousePointSelectionEvent e) {
        de.setDataSet( getContextDataSet() );
        DasAxis xa, ya;
        xa= ( this.xaxis==null ) ? plot.getXAxis() : xaxis;
        ya= ( this.yaxis==null ) ? plot.getYAxis() : yaxis;
        de.set(xa.invTransform(e.getX()),ya.invTransform(e.getY()));
        return de;
    }
    
    @Override
    public void mousePointSelected(MousePointSelectionEvent e) {
        fireDataPointSelectionListenerDataPointSelected(getDataPointSelectionEvent(e));
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
    protected void fireDataPointSelectionListenerDataPointSelected(DataPointSelectionEvent event) {
        Object[] listeners;
        listeners = listenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==org.das2.event.DataPointSelectionListener.class) {
                ((org.das2.event.DataPointSelectionListener)listeners[i+1]).dataPointSelected(event);
            }
        }
    }

    private static boolean isMac= ( System.getProperty("os.name").toLowerCase().startsWith("mac") );
    
    @Override
    public String getDirections() {
        return "Press p to pin, " + ( isMac ? "command": "control" ) + " to copy to clipboard";
    }

    @Override
    public void keyTyped(KeyEvent keyEvent) {
        if ( ( isMac && keyEvent.isMetaDown() ) || ( keyEvent.isControlDown() ) ) {
            if ( keyEvent.getKeyChar()==KeyEvent.VK_C || keyEvent.getKeyChar()==3 ) { // 3 was observed on Linux/Centos6/Java
                CrossHairRenderer r= (CrossHairRenderer) super.dragRenderer;
                String text= r.label.replaceAll("!c"," ");
                StringSelection stringSelection = new StringSelection( text );
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, (Clipboard clipboard1, Transferable contents) -> {
                });
                logger.log(Level.FINE, "copied to mouse buffer: {0}", text);
            }
        }
    }
    
    
}
