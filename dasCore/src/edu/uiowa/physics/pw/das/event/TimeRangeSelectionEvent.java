/* File: TimeRangeSelectionEvent.java
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

import edu.uiowa.physics.pw.das.datum.TimeDatum;

/**
 *
 * @author  jbf
 */
public class TimeRangeSelectionEvent extends DasEvent {
    
    private edu.uiowa.physics.pw.das.datum.TimeDatum startTime = null;
    
    private edu.uiowa.physics.pw.das.datum.TimeDatum endTime = null;
    
    /** Creates a new instance of TimeRangeSelectionEvent */
    public TimeRangeSelectionEvent( Object source, edu.uiowa.physics.pw.das.datum.TimeDatum startTime, edu.uiowa.physics.pw.das.datum.TimeDatum endTime ) {
        super(source);
        this.startTime= startTime;
        this.endTime= endTime;        
    }
    
    public edu.uiowa.physics.pw.das.datum.TimeDatum getStartTime() {
        return startTime;
    }
    
    public edu.uiowa.physics.pw.das.datum.TimeDatum getEndTime() {
        return endTime; 
    }
    
    public boolean equals(TimeRangeSelectionEvent e) {
        if (e==null) { 
            return false; 
        } else {
            return e.startTime.equals(startTime) && e.endTime.equals(endTime);
        }
    }
    
    public String toString() {
        return "["+getStartTime()+" - "+getEndTime()+"]";
    }
    
}
