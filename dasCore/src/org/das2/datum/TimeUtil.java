/* File: TimeUtil.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on September 22, 2003, 11:00 AM by Jeremy Faden <jbf@space.physics.uiowa.edu>
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

package org.das2.datum;

import java.text.ParseException;
import java.util.Map;
import java.util.HashMap;
import org.das2.datum.format.TimeDatumFormatter;

/**
 * Various time utilities
 * @author  jbf
 */
public final class TimeUtil {
    
    private TimeUtil() {
    }

	 // One of the few times package private is useful
    final static int[][] daysInMonth = {
        {0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31, 0},
        {0, 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31, 0}
    };

	 // One of the few times package private is useful
    final static int[][] dayOffset = {
        {0, 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334, 365},
        {0, 0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335, 366}
    };
    
    public static int daysInMonth(int month, int year) {
        return daysInMonth[isLeapYear(year)?1:0][month];
    }
    
    public static int julday( int month, int day, int year ) {
        int jd = 367 * year - 7 * (year + (month + 9) / 12) / 4 -
                3 * ((year + (month - 9) / 7) / 100 + 1) / 4 +
                275 * month / 9 + day + 1721029;
        return jd;
    }
    
    public static int dayOfYear( int month, int day, int year ) {
        return day + dayOffset[isLeapYear(year)?1:0][month];
	}

	 // intruduced to aid in debugging
	 public static class TimeDigit{
		CalendarTime.Step ordinal; // YEAR, MONTH, etc.
		String label;
		int divisions; // approximate
		private static Map<CalendarTime.Step,TimeDigit> digits =
			new HashMap<CalendarTime.Step, TimeDigit>();
		@Override
		public String toString(){
			return label;
		}
		private TimeDigit(CalendarTime.Step ordinal, String label, int divisions){
			this.ordinal = ordinal;
			this.label = label;
			this.divisions = divisions;
			digits.put(ordinal, this);
		}
		public CalendarTime.Step getOrdinal(){
			return ordinal;
		}
		public int divisions(){
			return divisions;
		}
		public static TimeDigit fromOrdinal(CalendarTime.Step ordinal){
			return digits.get(ordinal);
		}
	}

    public static final TimeDigit TD_YEAR = new TimeDigit( CalendarTime.Step.YEAR, "YEAR", 12 );
    public static final TimeDigit TD_MONTH = new TimeDigit( CalendarTime.Step.MONTH, "MONTH", 30 );
    public static final TimeDigit TD_DAY = new TimeDigit( CalendarTime.Step.DAY, "DAY", 24 );
    public static final TimeDigit TD_HOUR = new TimeDigit( CalendarTime.Step.HOUR, "HOUR", 60 );
    public static final TimeDigit TD_MINUTE = new TimeDigit( CalendarTime.Step.MINUTE, "MINUTE", 60 );
    public static final TimeDigit TD_SECOND = new TimeDigit( CalendarTime.Step.SECOND, "SECOND", 1000 );
    public static final TimeDigit TD_MILLI= new TimeDigit( CalendarTime.Step.MILLISEC, "MILLISECONDS", 1000 );
    public static final TimeDigit TD_MICRO = new TimeDigit( CalendarTime.Step.MICROSEC, "MICROSECONDS", 1000 );
    public static final TimeDigit TD_NANO = new TimeDigit( CalendarTime.Step.NANOSEC, "NANOSECONDS", 1000 );
    
    public static double getSecondsSinceMidnight(Datum datum) {
        double xx= datum.doubleValue(Units.t2000);
        if (xx<0) {
            xx= xx % 86400;
            if (xx==0) {
                return 0;
            } else {
                return 86400+xx;
            }
        } else {
            return xx % 86400;
        }
    }
    
    public static double getMicroSecondsSinceMidnight(Datum datum) {
        double xx= datum.doubleValue( Units.us2000 );
        if (xx<0) {
            xx= xx % 86400e6;
            if (xx==0) {
                return 0;
            } else {
                return 86400e6+xx;
            }
        } else {
            return xx % 86400e6;
        }
    }
    
    /**
     * return the the integer number of days that have elapsed since roughly Monday, January 1, 4713 BC.  Julian Day
     * is defined as starting at noon UT, here is is defined starting at midnight.
     * @param datum
     * @return
     */
    public static int getJulianDay( Datum datum ) {
        double xx= datum.doubleValue(Units.mj1958);
        return (int)Math.floor( xx ) + 2436205;
    }
    
    /**
     *Break the Julian day apart into month, day year.  This is based on
     *http://en.wikipedia.org/wiki/Julian_day (GNU Public License), and 
     *was introduced when toTimeStruct failed when the year was 1886.
     *@param julian the (integer) number of days that have elapsed since the initial epoch at noon Universal Time (UT) Monday, January 1, 4713 BC
     *@return a CalendarTime with the month, day and year fields set.
     */
    public static int[] julianToGregorian( int julian ) {

		 int[] lRet = {0,0,0};

        int j = julian + 32044;
        int g = j / 146097;
        int dg = j % 146097;
        int c = (dg / 36524 + 1) * 3 / 4;
        int dc = dg - c * 36524;
        int b = dc / 1461;
        int db = dc % 1461;
        int a = (db / 365 + 1) * 3 / 4;
        int da = db - a * 365;
        int y = g * 400 + c * 100 + b * 4 + a;
        int m = (da * 5 + 308) / 153 - 2;
        int d = da - (m + 4) * 153 / 5 + 122;
        int Y = y - 4800 + (m + 2) / 12;
        int M = (m + 2) % 12 + 1;
        int D = d + 1;
        
        lRet[0] = Y;
        lRet[1] = M;
        lRet[2] = D;
        return lRet;
    }
    
    /**
     * returns int[] { year, month, day, hour, minute, second, millis, micros }
     */
    public static int[] toTimeArray( Datum time ) {

		 CalendarTime ts= new CalendarTime( time );
		 int millis = (int) (ts.m_nNanoSecond / 1000000);
		 int micros = (int) (ts.m_nNanoSecond / 1000);
		  
       return new int[] { ts.m_nYear, ts.m_nMonth, ts.m_nDom, ts.m_nHour, ts.m_nMinute, ts.m_nSecond,
		                     millis, micros };
    }
    
    public static Datum toDatum( int[] timeArray ) {
        int year = timeArray[0];
        int month = timeArray[1];
        int day = timeArray[2];
        if ( timeArray[1]<1 ) {
            throw new IllegalArgumentException("");
        }
        int jd = 367 * year - 7 * (year + (month + 9) / 12) / 4 -
                3 * ((year + (month - 9) / 7) / 100 + 1) / 4 +
                275 * month / 9 + day + 1721029;
        int hour = (int)timeArray[3];
        int minute = (int)timeArray[4];
        double seconds = timeArray[5] + hour*(float)3600.0 + minute*(float)60.0 + timeArray[6]/1e9;
        double us2000= UnitsConverter.getConverter(Units.mj1958,Units.us2000).convert(( jd - 2436205 ) + seconds / 86400. );
        return Datum.create( us2000, Units.us2000 );
    }

	 /** Is year a leap year.
	  * Warning: Not tested for years prior to 1.
	  */
    public static boolean isLeapYear( int year ) {
		 if(year % 4 != 0)   return false;
		 if(year % 400 == 0) return true;
		 if(year % 100 == 0) return false;
		 else                return true;
    }

    
    public static Datum next( TimeDigit td, int count, Datum datum ) {
        if ( td==TD_NANO ) throw new IllegalArgumentException("not supported nanos");
        CalendarTime ct = new CalendarTime(datum).step(td.getOrdinal(), count);
        Datum result= ct.toDatum();
        return result;
    }
    
    public static Datum next(CalendarTime.Step step, Datum datum ) {
        CalendarTime ct = new CalendarTime(datum).step(step, 1);
		  return ct.toDatum();
    }
    
    
    /** step down the previous ordinal.  If the datum is already at an ordinal
     * boundry, then step down by one ordinal.
     * @param step
     * @param datum
     * @return
     */
    public static Datum prev(CalendarTime.Step step, Datum datum ) {
		CalendarTime ct= new CalendarTime(datum).step(step, -1);
		return ct.toDatum();
    }
    
    public static Datum now() {
        double us2000= ( System.currentTimeMillis() - 946684800e3 ) * 1000;
        return Units.us2000.createDatum(us2000);
    }
    
    /**
     * @param year the year 
     * @param month the month
     * @param day the day of month, unless month==0, then day is day of year.
     * @param hour additional hours
     * @param minute additional minutes
     * @param second additional seconds
     * @param units the Units in which to return the result.
     * @return a double in the context of units.
     */
    public static double convert(int year, int month, int day, int hour, int minute,
		                           double second, TimeLocationUnits units) {
        // if month==0, then day is doy (day of year).
        int jd;
        if ( month>0 ) {
            jd = julday(month,day,year);
        } else {
            // if month==0 then day is doy
            int month1= 1;
            int day1= 1;
            jd = julday(month1,day1,year);
            jd+= ( day - 1 );
        }
        
        second+= hour*3600.0 + minute*60.0;
        
        double us2000 = (jd-2451545)*86400000000. + second * 1000000;
        
        if ( units==Units.us2000 ) {
            return us2000;
        } else {
            return Units.us2000.convertDoubleTo(units, us2000);
        }
    }
    
    private final static String[] mons= {
             "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        };
    
    /**
     * returns 1..12 for the English month name
     *
     * @throws ParseException if the name isn't recognized
     */
    public static int monthNumber( String s ) throws ParseException {
        s= s.substring(0,3);
        for ( int i=0; i<12; i++ ) {
            if ( s.equalsIgnoreCase( mons[i] ) ) return i+1;
        }
        throw new ParseException("Unable to parse month", 0 );
    }
    
    /**
     * returns "Jan", "Feb", ... for month number (1..12).
     * @param mon integer from 1 to 12.
     * @return three character English month name.
     */
    public static String monthNameAbbrev( int mon ) {
        if ( mon<1 || mon>12 ) throw new IllegalArgumentException("invalid month number: "+mon);
        return mons[mon-1];
    }
    
    /** Creates a datum from a string
     * @param s
     * @throws ParseException
     * @return
     */
    public static Datum create(String s) throws java.text.ParseException {
        CalendarTime ts= new CalendarTime(s);
        return ts.toDatum();
    }
    
    /** creates a Datum from a string which is known to contain
     * a valid time format.  Throws a RuntimeException if the
     * string is not valid.
     * @param validString
     * @return
     */
    public static Datum createValid(String validString ) {
        try {
            return create( validString );
        } catch ( java.text.ParseException ex ) {
            throw new RuntimeException( ex );
        }
    }
    
    public static boolean isValidTime( String string ) {
        try {
            create( string );
            return true;
        } catch ( java.text.ParseException ex ) {
            return false;
        }
    }
    
    public static Datum prevMidnight(Datum datum) {
        //return datum.subtract(getMicroSecondsSinceMidnight(datum), Units.microseconds);
        return datum.subtract(getSecondsSinceMidnight(datum), Units.seconds);
    }
    
    /**
     * returns the next midnight, or this datum if we are already on midnight.
     * @param datum
     * @return
     */
    public static Datum nextMidnight( Datum datum ) {
		 CalendarTime ct = new CalendarTime(datum).step(CalendarTime.Step.DAY, 1);
		 return ct.toDatum();
    }
    /**
     * creates a Datum representing the time given in integer years, months, ..., seconds, nanoseconds.  The year
     * must be at least 1960, and must be a four-digit year.  A double in Units.us2000 is used to represent the 
     * Datum, so resolution will drop as the year drops away from 2000.
     *
     * @param year four digit year >= 1960.
     * @param month integer month, 1..12.
     * @param day integer day of month.
     * @param hour additional hours
     * @param minute additional minutes
     * @param second additional seconds
     * @param nano additional nanoseconds
     * @return a Datum with units Units.us2000.
     */
    public static Datum createTimeDatum( int year, int month, int day, int hour, int minute, int second, int nano ) {
        //if ( year<1960 ) throw new IllegalArgumentException("year must not be < 1960, and no 2 digit years (year="+year+")");
        if ( year<100 ) throw new IllegalArgumentException("year must not be < 100, and no 2 digit years (year="+year+")");
        int jd = 367 * year - 7 * (year + (month + 9) / 12) / 4 -
                3 * ((year + (month - 9) / 7) / 100 + 1) / 4 +
                275 * month / 9 + day + 1721029;
        double microseconds = second*1e6 + hour*3600e6 + minute*60e6 + nano/1e3;
        double us2000= UnitsConverter.getConverter(Units.mj1958,Units.us2000).convert(( jd - 2436205 )) + microseconds;
        return Datum.create( us2000, Units.us2000 );
    }

	 public static void main(String[] args) throws Exception {
        System.out.println( TimeUtil.now() );
        System.out.println( Datum.create( TimeUtil.convert(2000,1,2, 0, 0, 0, Units.us2000 ), Units.us2000 ));
        Datum x=create( "2000-1-1 0:00:33.45" );
        System.out.println( x );

        CalendarTime ts= new CalendarTime(x);
        System.out.println( ts.toDatum() );

        TimeDatumFormatter tf = TimeDatumFormatter.DEFAULT;

        for ( int i=0; i<44; i++ ) {
            System.out.println(tf.format(x)+"\t"+(long)x.doubleValue(Units.us2000));
            x= TimeUtil.prev(CalendarTime.Step.SECOND,x);
        }
    }
    
}
