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
    public TimeDatum(double value, Units units) {
        super(value,units);
    }
    
    public String toString() {
        return edu.uiowa.physics.pw.das.util.DasDate.create(this).toString();
    }    
    
    public static TimeDatum create(edu.uiowa.physics.pw.das.util.DasDate date) {
        edu.uiowa.physics.pw.das.util.DasDate base2000= new edu.uiowa.physics.pw.das.util.DasDate("01/01/2000 00:00");
        return new TimeDatum(date.subtract(base2000)*1000000,Units.us2000);
    }
            
    public static TimeDatum create(String s) {
        return TimeDatum.create(new edu.uiowa.physics.pw.das.util.DasDate(s));
    }
    
    public static double convert( int year, int month, int day, int hour, int minute, double second, TimeLocationUnits units ) {
        // if month==0, then day is doy (day of year).
        int jd;
        if ( month>0 ) {
            jd = 367 * year - 7 * (year + (month + 9) / 12) / 4 -
            3 * ((year + (month - 9) / 7) / 100 + 1) / 4 +
            275 * month / 9 + day + 1721029;
        } else {
            int month1= 1;            
            int day1= 1;
            jd = 367 * year - 7 * (year + (month1 + 9) / 12) / 4 -
            3 * ((year + (month1 - 9) / 7) / 100 + 1) / 4 +
            275 * month1 / 9 + day1 + 1721029;
            jd+= ( day - 1 );
        }
            
        second+= hour*3600.0 + minute*60.0;
        
        if ( units==Units.us2000 ) {
            return (jd-2451545)*86400000000. + second * 1000000;
        } else {
            throw new IllegalArgumentException("Not implemented for units: "+units.toString());
        }        
    }
    
    public static TimeDatum now() {
        Calendar cal= new GregorianCalendar();
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        int hour= cal.get(Calendar.HOUR_OF_DAY);
        int minute= cal.get(Calendar.MINUTE);
        int second= cal.get(Calendar.SECOND);
        int year= cal.get(Calendar.YEAR);
        int doy= cal.get(Calendar.DAY_OF_YEAR);
        
        return TimeDatum.create(year+"//"+doy+" "+hour+":"+minute+":"+second);
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
    
    public double getSecondsSinceMidnight() {
        Datum x= this.convertTo(Units.t2000);
        if (x.getValue()<0) {
            double xx= ( x.getValue() % 86400 );
            if (xx==0) {
                return 0;
            } else {
                return 86400-xx;
            }
        } else {
            return x.getValue() % 86400;
        }
    }
   
    public static void main(String[] args) {
        System.out.println(now());
        System.out.println( convert(2000,1,1, 0, 0, 0, Units.us2000 ));
        System.out.println( convert(2000,1,2, 0, 0, 0, Units.us2000 ));
    }
    
    public TimeDatum nextMonth() {
        DasDate pwdate= DasDate.create(this);
        pwdate= pwdate.next(DasDate.MONTH);
        return TimeDatum.create(pwdate);
    }
    
}
