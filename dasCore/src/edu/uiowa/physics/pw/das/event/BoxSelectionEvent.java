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

package edu.uiowa.physics.pw.das.event;

import edu.uiowa.physics.pw.das.dataset.DataSet;
import edu.uiowa.physics.pw.das.datum.DatumRange;
import java.util.HashMap;

/**
 * This is the range anolog to the DataPointSelectionEvent.  The DPSE is a point,
 * and this is a box.
 *
 * Note that it's acceptible to have null xrange and yrange, so that the same
 * code can support a variety of applications.  It's left to the programmer to
 * see that these are used consistently.
 *
 * @author  jbf
 */
public class BoxSelectionEvent extends DasEvent {
    
    private DatumRange xrange;
    private DatumRange yrange;
    private DataSet ds;
    private HashMap planes;
       
    /**
     * @deprecated  use BoxSelectionEvent( Object, DatumRange, DatumRange );
     */
    public BoxSelectionEvent(Object source, edu.uiowa.physics.pw.das.datum.Datum xMin, edu.uiowa.physics.pw.das.datum.Datum xMax, edu.uiowa.physics.pw.das.datum.Datum yMin, edu.uiowa.physics.pw.das.datum.Datum yMax) {
        this( source, xMin.le(xMax) ? new DatumRange( xMin, xMax ) : new DatumRange( xMax, xMin ),
                yMin.le(yMax) ? new DatumRange( yMin, yMax ) : new DatumRange( yMax, yMin ) );
    }
    
    public BoxSelectionEvent( Object source, DatumRange xrange, DatumRange yrange ) {
        this( source, xrange, yrange, null );
    }
    
    public BoxSelectionEvent( Object source, DatumRange xrange, DatumRange yrange, HashMap planes ) {
        super( source );
        this.xrange= xrange;
        this.yrange= yrange;
        this.planes= planes;
    }
        
    /**
     * @deprecated  use getXRange().min();
     */
    public edu.uiowa.physics.pw.das.datum.Datum getXMinimum() {
        if ( xrange!=null ) return xrange.min(); else return null;
    }
    
    /**
     * @deprecated  use getXRange().max();
     */
    public edu.uiowa.physics.pw.das.datum.Datum getXMaximum() {
        if ( xrange!=null ) return xrange.max(); else return null;
    }
    
    /**
     * @deprecated  use getYRange().min();
     */    
    public edu.uiowa.physics.pw.das.datum.Datum getYMinimum() {
        if ( yrange!=null ) return yrange.min(); else return null;
    }
    
    /**
     * @deprecated  use getYRange().max();
     */
    public edu.uiowa.physics.pw.das.datum.Datum getYMaximum() {
        if ( yrange!=null ) return yrange.max(); else return null;
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
    
    public void setDataSet(DataSet ds) {
        this.ds = ds;
    }
    
    public DataSet getDataSet() {
        return ds;
    }
    
    public String toString() {
        return "[BoxSelectionEvent x: "+xrange+", y: "+yrange+"]";
    }
        
}
