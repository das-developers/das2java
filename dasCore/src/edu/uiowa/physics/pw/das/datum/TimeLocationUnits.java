/* File: TimeLocationUnits.java
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

import edu.uiowa.physics.pw.das.datum.DasTimeFormatter;
import edu.uiowa.physics.pw.das.datum.LocationUnits;


/**
 *
 * @author  jbf
 */
public class TimeLocationUnits extends LocationUnits {
    
    /* TimeLocationUnits class is introduced because it is often necessary to
     * easily identify a time quantity, for instance when deciding whether to
     * use a timeAxis or not.
     */
    
    public TimeLocationUnits( String id, String description, Units offsetUnits ) {
        super(id,description,offsetUnits);
    }
    
    public String format(double d) {
        DasTimeFormatter dtf= new DasTimeFormatter(TimeContext.HOURS);
        dtf.setAlwaysShowDate(true);
        String result= dtf.format(d,this);
        return result;
    }
    
    public double parse(String s) {
        int DATE = 0;
        int YEAR = 1;
        int MONTH = 2;
        int DAY = 3;
        int HOUR = 4;
        int MINUTE = 5;
        int SECOND = 6;
        
        Number[] d = edu.uiowa.physics.pw.das.util.DasDate.parseTime(s);
        
        if (d == null)
            throw new IllegalArgumentException("Could not parse date string: \"" + s + "\"");
        
        int year, month, day, hour, minute;
        int jd;
        double seconds;
        
        year = d[YEAR].intValue();
        month = d[MONTH].intValue();
        day = d[DAY].intValue();
        hour = d[HOUR].intValue();
        jd = 367 * year - 7 * (year + (month + 9) / 12) / 4 -
        3 * ((year + (month - 9) / 7) / 100 + 1) / 4 +
        275 * month / 9 + day + 1721029;
        minute = d[MINUTE].intValue();
        seconds = d[SECOND].floatValue() + hour*(float)3600.0 + minute*(float)60.0;
        
        double result;
        result= ( jd-2451545 ) * 86400000000. + seconds * 1000000.;
        if ( this!=us2000 ) {
            UnitsConverter uc= getConverter( us2000, this );
            result= uc.convert(result);
        }
        return result;
        
    }
    
}
