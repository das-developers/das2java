/* File: DataPointSelectionEvent.java
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
import java.util.Map;
import org.das2.qds.QDataSet;

/**
 * This is the general-purpose "a data point was selected" event.  Note that
 * auxiliary data is supported, such as a keystroke that triggered the event.
 * 
 * The X and Y Datums may be null, so that code may be reused.  
 *
 * @author  jbf
 */
public class DataPointSelectionEvent extends DasEvent {
    
    private Datum x;
    private Datum y;
    private Map planes;
    
    public long birthMilli;
    
    private QDataSet ds=null;
    
    /** Creates a new instance of DataPointSelectionEvent */
    public DataPointSelectionEvent(Object source, 
            Datum x, 
            Datum y,
            Map planes ) {
        super(source);
        this.birthMilli= System.currentTimeMillis();
        this.x= x;
        this.y= y;
        this.ds= null;
        this.planes= planes;
    }
    
    public DataPointSelectionEvent(Object source, 
            Datum x, 
            Datum y ) {
        this( source, x, y, null );        
    }    
    
    public org.das2.datum.Datum getX() {
        return x;
    }

    public org.das2.datum.Datum getY() {
        return y;
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
    
    public void set( Datum x,  Datum y) {
        this.x= x;
        this.y= y;
    }
    
    public void setDataSet( QDataSet ds) {
        this.ds= ds;
    }
    
    /**
     * return the context dataset, from which the selection is made.
     * @return 
     */
    public QDataSet getDataSet() {
        return this.ds;
    }
    
    @Override
    public String toString() {
        return "[DataPointSelectionEvent x:"+x+" y:"+y+"]";
    }
}
