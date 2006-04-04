/* File: VerticalRangeSelectorMouseModule.java
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
import edu.uiowa.physics.pw.das.datum.DatumRange;
import edu.uiowa.physics.pw.das.graph.DasColorBar;
import edu.uiowa.physics.pw.das.graph.DasRow;
import edu.uiowa.physics.pw.das.graph.Renderer;
import java.awt.event.MouseEvent;
import javax.swing.event.EventListenerList;

/**
 *
 * @author  jbf
 */
public class ColorBarRepaletteMouseModule extends MouseModule {
    
    DasColorBar colorBar;
    Renderer parent;
    DatumRange range0;
    boolean animated0;
    
    /** Utility field used by event firing mechanism. */
    private EventListenerList listenerList =  null;
    
    public String getLabel() { return "Repalette"; };
    
    public ColorBarRepaletteMouseModule( Renderer parent, DasColorBar colorBar ) {
        if (colorBar.isHorizontal()) {
            throw new IllegalArgumentException("Axis orientation is not vertical");
        }
        this.parent= parent;
        //  this.dragRenderer= (DragRenderer)HorizontalRangeRenderer.renderer;
        this.dragRenderer= new HorizontalSliceSelectionRenderer(parent.getParent());
        this.colorBar= colorBar;
    }
    
    private void setColorBar( int y ) {
        DatumRange dr;        
        DasRow row= colorBar.getRow();
        double alpha=  ( row.getDMaximum() - y ) / (1.*row.getHeight());
        dr= range0.rescale( 0, alpha );        
        colorBar.setDatumRange(dr);
        parent.update();
    }
    
    public void mouseReleased( MouseEvent e ) {
        colorBar.setAnimated(animated0);
    }
    
    public void mousePointSelected(MousePointSelectionEvent e) {
        setColorBar( e.y );
    }
    
    /** Registers DataRangeSelectionListener to receive events.
     * @param listener The listener to register.
     */
    public synchronized void addDataRangeSelectionListener(edu.uiowa.physics.pw.das.event.DataRangeSelectionListener listener) {
        if (listenerList == null ) {
            listenerList = new EventListenerList();
        }
        listenerList.add(edu.uiowa.physics.pw.das.event.DataRangeSelectionListener.class, listener);
    }
    
    /** Removes DataRangeSelectionListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public synchronized void removeDataRangeSelectionListener(edu.uiowa.physics.pw.das.event.DataRangeSelectionListener listener) {
        listenerList.remove(edu.uiowa.physics.pw.das.event.DataRangeSelectionListener.class, listener);
    }
    
    /** Notifies all registered listeners about the event.
     *
     * @param event The event to be fired
     */
    private void fireDataRangeSelectionListenerDataRangeSelected(DataRangeSelectionEvent event) {
        if (listenerList == null) return;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==edu.uiowa.physics.pw.das.event.DataRangeSelectionListener.class) {
                ((edu.uiowa.physics.pw.das.event.DataRangeSelectionListener)listeners[i+1]).DataRangeSelected(event);
            }
        }
    }

    public void mousePressed(java.awt.event.MouseEvent e) {        
        super.mousePressed(e);
        animated0= colorBar.isAnimated();
        colorBar.setAnimated(false);        
        range0= colorBar.getDatumRange();
    }
    
}
