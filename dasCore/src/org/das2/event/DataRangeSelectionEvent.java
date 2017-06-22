/* File: DataRangeSelectionEvent.java
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
import org.das2.qds.QDataSet;

/**
 *
 * @author  jbf
 */
public class DataRangeSelectionEvent extends DasEvent {
    
    private QDataSet ds=null;
       
    Datum min;
    Datum max;
    
    Datum reference;  // this is where the selection was made at (perpendicular axis)
        
    public DataRangeSelectionEvent(Object source, Datum min, Datum max) {
        super(source);
        if (min.gt(max)) {
            Datum t=min;
            min=max;
            max=t;
        }        
        this.min= min;
        this.max= max;        
        reference= null;
    }
    
    public Datum getMinimum() {
        return min;
    }
    
    public Datum getMaximum() {
        return max; 
    }
    
    public DatumRange getDatumRange() {
        return new DatumRange( min, max );
    }
    
    public void setDataSet(QDataSet ds) {
        this.ds= ds;
    }
    
    public QDataSet getDataSet() {
        return this.ds;
    }
    
    public void setReference(Datum reference) {
        this.reference= reference;
    }
        
    public Datum getReference() {
        return this.reference;
    }
    
    public String toString() {
        return "[DataRangeSelectionEvent min:"+min+" max:"+max+"]";
    }
        
}
