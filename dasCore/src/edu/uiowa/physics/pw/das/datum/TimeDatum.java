/* File: TimeDatum.java
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
import edu.uiowa.physics.pw.das.util.DasDate;
import edu.uiowa.physics.pw.das.datum.DasFormatter;
import edu.uiowa.physics.pw.das.datum.DasTimeFormatter;
import edu.uiowa.physics.pw.das.datum.Datum;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 *
 * @author  jbf
 */
public class TimeDatum extends Datum {
    
    /** Creates a new instance of TimeDatum */
    private TimeDatum(double value, Units units) {
        super(value,units);
    }
    
    public String toString() {
        return edu.uiowa.physics.pw.das.util.DasDate.create(this).toString();
    }    
    
    public DasFormatter getFormatter( Datum datum2, int nsteps ) {
        TimeDatum datum1= this;
        if ( datum1.getUnits() != datum2.getUnits() ) {
           throw new IllegalArgumentException( "Units don't match!" );
        }        
        datum2= datum1.add(datum2.subtract(datum1).divide(nsteps));
        DasTimeFormatter tf= new DasTimeFormatter(TimeContext.getContext(datum1,(TimeDatum)datum2));
        return tf;
    }
    
    public static void main(String[] args) {
        System.out.println( TimeUtil.now() );
        System.out.println( TimeUtil.convert(2000,1,1, 0, 0, 0, Units.us2000 ));
        System.out.println( TimeUtil.convert(2000,1,2, 0, 0, 0, Units.us2000 ));
    }
    
}
