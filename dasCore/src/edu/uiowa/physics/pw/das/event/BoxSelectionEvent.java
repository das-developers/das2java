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

import edu.uiowa.physics.pw.das.datum.Datum;

/**
 *
 * @author  jbf
 */
public class BoxSelectionEvent extends DasEvent {
    
    edu.uiowa.physics.pw.das.datum.Datum xMin;
    edu.uiowa.physics.pw.das.datum.Datum xMax;
    edu.uiowa.physics.pw.das.datum.Datum yMin;
    edu.uiowa.physics.pw.das.datum.Datum yMax;
       
    public BoxSelectionEvent(Object source, edu.uiowa.physics.pw.das.datum.Datum xMin, edu.uiowa.physics.pw.das.datum.Datum xMax, edu.uiowa.physics.pw.das.datum.Datum yMin, edu.uiowa.physics.pw.das.datum.Datum yMax) {
        super(source);
        if (xMin.gt(xMax)) {
            edu.uiowa.physics.pw.das.datum.Datum t=xMin;
            xMin=xMax;
            xMax=t;
        }        
        if (yMin.gt(yMax)) {
            edu.uiowa.physics.pw.das.datum.Datum t=yMin;
            yMin=yMax;
            yMax=t;
        }        
        this.xMin= xMin;
        this.xMax= xMax;        
        this.yMin= yMin;
        this.yMax= yMax;        
    }
    
    public edu.uiowa.physics.pw.das.datum.Datum getXMinimum() {
        return xMin;
    }
    
    public edu.uiowa.physics.pw.das.datum.Datum getXMaximum() {
        return xMax; 
    }
    
    public edu.uiowa.physics.pw.das.datum.Datum getYMinimum() {
        return yMin;
    }
    
    public edu.uiowa.physics.pw.das.datum.Datum getYMaximum() {
        return yMax; 
    }
    
    public String toString() {
        return "[BoxSelectionEvent x: "+xMin+" - "+xMax+", y: "+yMin+" - "+yMax+"]";
    }
        
}
