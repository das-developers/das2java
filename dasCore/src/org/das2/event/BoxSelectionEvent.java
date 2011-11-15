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
import org.virbo.dataset.QDataSet;

/**
 * This is the range anolog to the DataPointSelectionEvent.  The DPSE is a point,
 * and this is a box.
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
    
    public BoxSelectionEvent( Object source, DatumRange xrange, DatumRange yrange ) {
        this( source, xrange, yrange, null );
    }
    
    public BoxSelectionEvent( Object source, DatumRange xrange, DatumRange yrange, HashMap planes ) {
        super( source );
        this.xrange= xrange;
        this.yrange= yrange;
        this.planes= planes;
    }
     
    public void setFinish( Datum x, Datum y ) {
        this.finishx = x;
        this.finishy = y;
    }
            
    public Datum getFinishX() {
        return this.finishx;
    }
    
    public Datum getFinishY() {
        return this.finishy;
    }
    
    public void setStart( Datum x, Datum y ) {
        this.startx = x;
        this.starty = y;
    }
            
    public Datum getStartX() {
        return this.startx;
    }
    
    public Datum getStartY() {
        return this.starty;
    }
    
    public DatumRange getXRange() {
        return xrange;
    }
    
    public DatumRange getYRange() {
        return yrange;
    }
    
    public Object getPlane( String plane ) {
        return planes==null ? null : planes.get(plane);
    }
    
    public String[] getPlaneIds() {
        if ( planes==null ) {
            return new String[0];
        } else {
            return (String[])planes.keySet().toArray( new String[ planes.keySet().size() ] );
        }
    }
    
    public void setDataSet(QDataSet ds) {
        this.ds = ds;
    }
    
    public QDataSet getDataSet() {
        return ds;
    }
    
    public String toString() {
        return "[BoxSelectionEvent x: "+xrange+", y: "+yrange+"]";
    }
        
}
