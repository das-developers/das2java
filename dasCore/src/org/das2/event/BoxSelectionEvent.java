/* File: BoxSelectionEvent.java
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


import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import java.util.HashMap;
import org.das2.qds.QDataSet;

/**
 * This is the range analog to the DataPointSelectionEvent.  The 
 * DataPointSelectionEvent is a point, and this is a box.
 *
 * Note that it's acceptable to have null xrange and yrange, so that the same
 * code can support a variety of applications.  It's left to the programmer to
 * see that these are used consistently.
 *
 * @author  jbf
 */
public class BoxSelectionEvent extends DasEvent {
    
    private DatumRange xrange;
    private DatumRange yrange;
    private Datum finishx, finishy;
    private Datum startx, starty;
    private QDataSet ds;
    private HashMap planes;
    
    /**
     * create the BoxSelectionEvent with additional planes of data.
     * @param source the object creating this event, for example the BoxSelectorMouseModule.
     * @param xrange the horizontal range
     * @param yrange the vertical range
     */
    public BoxSelectionEvent( Object source, DatumRange xrange, DatumRange yrange ) {
        this( source, xrange, yrange, null );
    }
    
    /**
     * create the BoxSelectionEvent with additional planes of data.
     * @param source the object creating this event, for example the BoxSelectorMouseModule.
     * @param xrange the horizontal range
     * @param yrange the vertical range
     * @param planes a map from String to Object containing arbitrary metadata.
     */
    public BoxSelectionEvent( Object source, DatumRange xrange, DatumRange yrange, HashMap planes ) {
        super( source );
        this.xrange= xrange;
        this.yrange= yrange;
        this.planes= planes;
    }
     
    /**
     * set the end coordinates of the mouse release.
     * @param x the release coordinate X
     * @param y the release coordinate Y
     */
    public void setFinish( Datum x, Datum y ) {
        this.finishx = x;
        this.finishy = y;
    }
            
    /**
     * get the X coordinate of the mouse button release
     * @return the release coordinate X
     */
    public Datum getFinishX() {
        return this.finishx;
    }
    
    /**
     * get the Y coordinate of the mouse button release
     * @return the release coordinate Y
     */
    public Datum getFinishY() {
        return this.finishy;
    }
    
    /**
     * set the coordinates of the mouse button press
     * @param x the x coordinate
     * @param y the y coordinate
     */    
    public void setStart( Datum x, Datum y ) {
        this.startx = x;
        this.starty = y;
    }
     
    /**
     * get the X coordinate or the mouse button press
     * @return the X coordinate or the mouse button press
     */
    public Datum getStartX() {
        return this.startx;
    }
    
    /**
     * get the Y coordinate or the mouse button press
     * @return the Y coordinate or the mouse button press
     */
    public Datum getStartY() {
        return this.starty;
    }
    
    /**
     * get the X data range of the gesture
     * @return the X data range of the gesture
     */
    public DatumRange getXRange() {
        return xrange;
    }
    
    /**
     * get the Y data range of the gesture
     * @return the Y data range of the gesture
     */
    public DatumRange getYRange() {
        return yrange;
    }
    
    /**
     * get the data attached to the plane name.  This allows applications
     * to attach additional data to the event.  For example, the BoxSelectorMouseModule
     * attaches the key pressed.
     * 
     * @param plane, e.g. 'keyChar'
     * @return the value associated with the plane, or null.
     */
    public Object getPlane( String plane ) {
        return planes==null ? null : planes.get(plane);
    }
    
    /**
     * return the list of additional data planes attached to this event.
     * @return 
     */
    public String[] getPlaneIds() {
        if ( planes==null ) {
            return new String[0];
        } else {
            return (String[])planes.keySet().toArray( new String[ planes.keySet().size() ] );
        }
    }
    
    /**
     * attach a dataset to this event.
     * @param ds a dataset for this event.
     */
    public void setDataSet(QDataSet ds) {
        this.ds = ds;
    }
    
    /**
     * get the dataset attached to this event.
     * @return the dataset attached to this event.
     */
    public QDataSet getDataSet() {
        return ds;
    }
    
    @Override
    public String toString() {
        return "[BoxSelectionEvent x: "+xrange+", y: "+yrange+"]";
    }
        
}
