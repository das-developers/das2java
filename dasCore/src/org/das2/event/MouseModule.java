/* File: MouseModule.java
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

import org.das2.components.propertyeditor.Displayable;
import org.das2.components.propertyeditor.Editable;
import org.das2.graph.DasCanvasComponent;

import java.awt.*;
import java.awt.event.*;
import java.util.logging.Logger;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.DomainDivider;
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.LoggerManager;
import org.das2.datum.UnitsUtil;
import org.das2.graph.DasAxis;
import org.das2.graph.DasDevicePosition;
import org.das2.system.DasLogger;

/** A MouseModule is a pluggable unit that promotes simple
 * mouse events into human events or actions that are useful
 * for science analysis.  Each component has a mouseInputAdapter
 * that manages a set of mouseModules, one is active at any
 * given time.
 *
 * The DasMouseInputAdapter will delegate mouse events, key events, and 
 * mouse wheel events to the active mouse module.
 * 
 * This base class will be extended by instances of MouseModule, overriding
 * methods they wish to handle.
 * 
 * @author jbf
 */
public class MouseModule implements Editable, Displayable, KeyListener, MouseListener, MouseMotionListener, MouseWheelListener  {

    protected static final Logger logger= LoggerManager.getLogger(DasLogger.GUI_LOG.toString() );
    protected DragRenderer dragRenderer;
    private String label;
    private String directions;
    
    protected DasCanvasComponent parent;
    
    protected MouseModule() {
        label= "unlabelled MM";
        dragRenderer= EmptyDragRenderer.renderer;
    }
    
    /**
     * create the mouse module for the parent component.  This is 
     * without a drag renderer, and the class name is used
     * for the label.
     * paint graphic feedback to the human operator.  
     * @param parent the component, such as a DasPlot for the crosshair digitizer or the DasAxis for zoom.
     */    
    public MouseModule(DasCanvasComponent parent) {
        this( parent, EmptyDragRenderer.renderer, "unlabelled MM" );
        setLabel(this.getClass().getName());
    }
    
    /**
     * create the mouse module for the parent component using the dragRenderer to
     * paint graphic feedback to the human operator.  For example, the 
     * dragRenderer might draw a box during the click-drag-release mouse action,
     * and then on release this mouse module creates the event used elsewhere.
     * @param parent the component, such as a DasPlot for the crosshair digitizer or the DasAxis for zoom.
     * @param dragRenderer the drag renderer to provide feedback 
     * @param label label for the mouse module.
     */
    public MouseModule(DasCanvasComponent parent, DragRenderer dragRenderer, String label) {
        this.parent= parent;
        this.dragRenderer= dragRenderer;
        this.label= label;
        if ( dragRenderer instanceof AbstractDragRenderer ) {
            AbstractDragRenderer adr= ((AbstractDragRenderer)dragRenderer);
            if ( adr.getParent()==null ) {
                adr.setParent(parent);
            }
        }
    }
    
    /**
     * returns a human-readable string that identifies the module
     * @return a human-readable string that identifies the module
     */
    public String getLabel() {
        return label;
    }
    
    /** 
     * return a cursor that indicates the selected module.  Note this is currently
     * not used.
     * @return a cursor that indicates the selected module.
     */
    public Cursor getCursor() {
        return new Cursor(Cursor.DEFAULT_CURSOR);
    }

    /**
     * return the current drag renderer.
     * @return the current drag renderer.
     */
    public DragRenderer getDragRenderer() {
        return dragRenderer;
    }
    
    /**
     * set the drag renderer. (Made public when the digitizer had different modes.)
     * @param d set the drag renderer.
     */
    public void setDragRenderer( DragRenderer d ) {
        this.dragRenderer= d;
        if ( d instanceof AbstractDragRenderer ) {
            AbstractDragRenderer adr= ((AbstractDragRenderer)dragRenderer);
            if ( adr.getParent()==null ) {
                adr.setParent(parent);
            }
        }
        parent.repaint();
    }
    
    /** 
     * Action to take when a mouse range (click, drag, release) has been 
     * selected.  This is intended to be overridden. 
     * @param e the drag event.
     */
    public void mouseRangeSelected(MouseDragEvent e) {
    }
    
    /**
     * Action to take when a point (click or drag) is selected.  This is intended
     * to be overridden.
     * @param e the event.
     */
    public void mousePointSelected(MousePointSelectionEvent e) {
    }
    
    /**
     * set the human-readable label.
     * @param label the human-readable label.
     */
    public final void setLabel(java.lang.String label) {
        this.label= label;
    }
    
    /**
     * round to the nearest nice interval by looking for a DomainDivider in the axis.
     * 
     * @param xAxis the axis
     * @param dr a datum range.
     * @return the DatumRange, possibly rounded to a nice range.
     */
    protected static DatumRange maybeRound(DasAxis xAxis, DatumRange dr) {
        DomainDivider div= xAxis.getMinorTicksDomainDivider();
        //if ( false && div==null ) { // make true to experiment with maybeRound while DomainDividers are still not being used.
        //    div= DomainDividerUtil.getDomainDivider( dr.min(), dr.max(), xAxis.isLog() );
        //}
        if ( div!=null ) {
            try {
                int px= 999;
                while ( px>1 ) {
                    div= div.finerDivider(false);
                    DatumRange minDr= div.rangeContaining(dr.min());
                    px= (int)Math.ceil( Math.abs( xAxis.transform(minDr.max()) - xAxis.transform(minDr.min()) ) );
                }
                DatumRange minDr= div.rangeContaining(dr.min());
                DatumRange maxDr= div.rangeContaining(dr.max());
                Datum min= DatumRangeUtil.normalize( minDr, dr.min() ) < 0.5 ? minDr.min() : minDr.max();
                Datum max= DatumRangeUtil.normalize( maxDr, dr.max() ) < 0.5 ? maxDr.min() : maxDr.max();
                DatumRange drRound= new DatumRange( min, max );

                dr= drRound;
            } catch ( InconvertibleUnitsException ex ) {
                // it's okay to do nothing, this is a transient state
            }
        }
        return dr;
    }
    
    /**
     * return true if the axis is not an axis with enumeration units.
     * @param axis the axis, which might have enumeration units.
     * @return true if the axis is not an axis with enumeration units.
     */
    protected static boolean axisIsAdjustable(DasAxis axis) {
        return axis != null && (UnitsUtil.isIntervalMeasurement(axis.getUnits()) || UnitsUtil.isRatioMeasurement(axis.getUnits()));
    }

    /**
     * allow one-line directions to be added to the mouse module.
     * This is used in Autoplot for the status bar.  
     * @return the directions, or null.
     */
    public String getDirections() {
        return this.directions;
    }
    
    /**
     * set the human-readable directions string, so clients like Autoplot can display
     * them.
     * @param directions human-readable directions.
     */
    public void setDirections( String directions ) {
        this.directions= directions;
    }
    
    /**
     * return the list icon.  This will be be overridden by MouseModules to
     * give a visual reference.
     * @return the list icon.
     */
    @Override
    public javax.swing.Icon getListIcon() {
        return null;
    }

    @Override
    public void drawListIcon(Graphics2D g, int x, int y) {
       // do nothing
    }

    @Override
    public String getListLabel() {
        return getLabel();
    }
    
    @Override
    public void keyPressed(KeyEvent keyEvent) {
    }
    
    @Override
    public void keyReleased(KeyEvent keyEvent) {
    }
    
    @Override
    public void keyTyped(KeyEvent keyEvent) {
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {
    }
    
    @Override
    public void mouseExited(MouseEvent e) {
    }
    
    @Override
    public void mouseMoved(MouseEvent e) {
    }
    
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
    }
    
    /**
     * used by subclasses to describe positions.
     */
    protected enum Pos {
        _null, beyondMin, min, middle, max, beyondMax
    };
    
    /**
     * indicate if the position (pixels) is near the ends of the DasRow or
     * DasColumn.  Note that the max of a row is its bottom.
     * @param ddp the row or column
     * @param pos the position to describe.
     * @param threshold pixel distance to the boundary, 20 is often used.
     * @return enumeration of the position, for example Pos.beyondMin.
     */
    public Pos position( DasDevicePosition ddp, int pos, int threshold ) {
        int max = ddp.getDMaximum();
        int min = ddp.getDMinimum();
        if (((max - min) / threshold) < 3) threshold = (max - min) / 3;
        if (pos < min) {
            return Pos.beyondMin;
        } else if (pos < min + threshold ) {
            return Pos.min;
        } else if (pos <= max - threshold) {
            return Pos.middle;
        } else if (pos <= max) {
            return Pos.max;
        } else {
            return Pos.beyondMax;
        }
    }    
}
