/* File: DataPointSelectionEvent.java
 * Copyright (C) 2002-2003 University of Iowa
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
import edu.uiowa.physics.pw.das.datum.Datum;

/**
 *
 * @author  jbf
 */
public class DataPointSelectionEvent extends DasEvent {
    
    private edu.uiowa.physics.pw.das.datum.Datum x;
    private edu.uiowa.physics.pw.das.datum.Datum y;
    
    public long birthMilli;
    
    private edu.uiowa.physics.pw.das.dataset.DataSet ds=null;
    
    private Object source;
    
    /** Creates a new instance of DataPointSelectionEvent */
    public DataPointSelectionEvent(Object source, edu.uiowa.physics.pw.das.datum.Datum x, edu.uiowa.physics.pw.das.datum.Datum y ) {
        super(source);
        this.birthMilli= System.currentTimeMillis();
        this.x= x;
        this.y= y;
        this.ds= null;        
    }
    
    public edu.uiowa.physics.pw.das.datum.Datum getX() {
        return x;
    }

    public edu.uiowa.physics.pw.das.datum.Datum getY() {
        return y;
    }
    
    public void set(edu.uiowa.physics.pw.das.datum.Datum x, edu.uiowa.physics.pw.das.datum.Datum y) {
        this.x= x;
        this.y= y;
    }
    
    public void setDataSet(edu.uiowa.physics.pw.das.dataset.DataSet ds) {
        this.ds= ds;
    }
    
    public edu.uiowa.physics.pw.das.dataset.DataSet getDataSet() {
        return this.ds;
    }
    
    public String toString() {
        return "[DataPointSelectionEvent x:"+x+" y:"+y+"]";
    }
}
