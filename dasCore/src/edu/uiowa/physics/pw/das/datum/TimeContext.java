/* File: TimeContext.java
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

package edu.uiowa.physics.pw.das.datum;

import edu.uiowa.physics.pw.das.datum.TimeDatum;
import edu.uiowa.physics.pw.das.datum.Units;

/**
 *
 * @author  jbf
 */
public class TimeContext {
    
    public static TimeContext MILLISECONDS= new TimeContext("milliseconds",1/86400000.);
    public static TimeContext SECONDS= new TimeContext("seconds", 1/86400.);
    public static TimeContext MINUTES = new TimeContext("minutes", 1/1440.);
    public static TimeContext HOURS = new TimeContext("hours",1/24.);
    public static TimeContext DAYS = new TimeContext("days",1);
    public static TimeContext WEEKS = new TimeContext("weeks",7);
    public static TimeContext MONTHS = new TimeContext("months",30);
    public static TimeContext YEARS = new TimeContext("years",365);
    public static TimeContext DECADES = new TimeContext("decades",3650);
    
    String s;
    double ordinal;
    
    public TimeContext(String s, double ordinal ) {
        this.s= s;
        this.ordinal= ordinal;
    }
    
    public boolean gt( TimeContext tc ) {
        return ordinal>tc.ordinal;
    }
    
    public boolean le( TimeContext tc ) {
        return ordinal<=tc.ordinal;
    }
    
    public String toString() {
        return s;
    }
    
    public static TimeContext getContext(TimeDatum t1, TimeDatum t2) {
        TimeContext context;
        double seconds= t2.subtract(t1).convertTo(Units.seconds).getValue();
        if (seconds<1) { context=MILLISECONDS; }
        else if (seconds<60) { context=SECONDS; }
        else if (seconds<3600) { context=MINUTES; }
        else if (seconds<86400) { context=HOURS; }
        else if (seconds<=864000) { context=DAYS; }
        else { context=DAYS; }
        return context;
    }
    
}
