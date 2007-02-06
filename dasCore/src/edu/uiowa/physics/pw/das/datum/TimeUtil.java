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

package edu.uiowa.physics.pw.das.datum;

import java.util.*;

import edu.uiowa.physics.pw.das.util.*;
import edu.uiowa.physics.pw.das.datum.format.*;
import java.text.ParseException;

/**
 *
 * @author  jbf
 */
public final class TimeUtil {
    
    private TimeUtil() {
    }
    
    private final static int[][] daysInMonth = {
        {0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31, 0},
        {0, 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31, 0}
    };
    
    private final static int[][] dayOffset = {
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
    
    
    public final static class TimeStruct {
        /**
         * year containing the time datum
         */
        public int year;
        /**
         * month containing the time datum
         */
        public int month;
        /**
         * day of month containing the time datum
         */
        public int day;
        /**
         * day of year containing the time datum
         */
        public int doy;
        /**
         * hour containing the time datum
         */
        public int hour;
        /**
         * minute containing the time datum
         */
        public int minute;
        /**
         * seconds since the last minute boundary of the time datum
         */
        public double seconds; // remaining number of seconds past minute boundary
        
        /**
         * additional milliseconds since minute boundary
         */
        public int millis;
        
        /**
         * additional microseconds since minute boundary
         */
        public int micros;
        
        public boolean isLocation= false;
        
        public String toString() {
            return year+"/"+month+"/"+day+" "+hour+":"+minute+":"+seconds;
        }
        public boolean[] want;
        
        public TimeStruct copy() {
            TimeStruct result= new TimeStruct();
            result.year= this.year;
            result.month= this.month;
            result.day= this.day;
            result.hour= this.hour;
            result.minute= this.minute;
            result.seconds= this.seconds;
            result.millis= this.millis;
            result.micros= this.micros;
            return result;
        }
        
        public TimeStruct add( TimeStruct offset ) {
            if ( offset.isLocation && this.isLocation ) throw new IllegalArgumentException("can't add two times!");
            TimeStruct result= new TimeStruct();
            
            result.year= this.year + offset.year;
            result.month= this.month + offset.month;
            result.day= this.day + offset.day;
            result.hour= this.hour + offset.hour;
            result.minute= this.minute + offset.minute;
            result.seconds= this.seconds + offset.seconds;
            result.millis= this.millis + offset.millis ;
            result.micros= this.micros + offset.micros ;
            
            result.isLocation= this.isLocation || offset.isLocation;
            
            return result;
        }
    }
    
    public static final int YEAR = 1;
    public static final int MONTH = 2;
    public static final int DAY = 3;
    public static final int HOUR = 4;
    public static final int MINUTE = 5;
    public static final int SECOND = 6;
    public static final int NANO = 7;
    public static final int WEEK = 97;
    public static final int QUARTER = 98;
    
    
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
    
    public static int getJulianDay( Datum datum ) {
        double xx= datum.doubleValue(Units.mj1958);
        return (int)Math.floor( xx ) + 2436205;
    }
    
    public static Datum toDatum( TimeStruct d ) {
        int year = (int)d.year;
        int month = (int)d.month;
        int day = (int)d.day;
        int jd = 367 * year - 7 * (year + (month + 9) / 12) / 4 -
                3 * ((year + (month - 9) / 7) / 100 + 1) / 4 +
                275 * month / 9 + day + 1721029;
        int hour = (int)d.hour;
        int minute = (int)d.minute;
        double seconds = d.seconds + hour*(float)3600.0 + minute*(float)60.0;
        double us2000= UnitsConverter.getConverter(Units.mj1958,Units.us2000).convert(( jd - 2436205 ) + seconds / 86400. );
        return Datum.create( us2000 + d.millis * 1000 + d.micros, Units.us2000 );
    }
    
    /**
     *Break the Julian day apart into month, day year.  This is based on
     *http://en.wikipedia.org/wiki/Julian_day (GNU Public License), and 
     *was introduced when toTimeStruct failed when the year was 1886.
     *@param julian the (integer) number of days that have elapsed since the initial epoch at noon Universal Time (UT) Monday, January 1, 4713 BC
     *@return a TimeStruct with the month, day and year fields set.
     */
    public static TimeStruct julianToGregorian( int julian ) {
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
        TimeStruct result= new TimeStruct();
        result.year= Y;
        result.month= M;
        result.day= D;
        return result;
    }
    
    
    /**
     * splits the time location datum into y,m,d,etc components.  Note that
     * seconds is a double, and micros will be 0.
     */
    public static TimeStruct toTimeStruct(Datum datum) {
        int jalpha, j1, j2, j3, j4, j5;
        int year, month, day, hour, minute;
        double justSeconds;
        
        int jd= getJulianDay(datum);
        double seconds= getSecondsSinceMidnight(datum);
        
        TimeStruct result= julianToGregorian( jd );
        
        hour = (int)(seconds/3600.0);
        minute = (int)((seconds - hour*3600.0)/60.0);
        justSeconds = seconds - hour*(double)3600.0 - minute*(double)60.0;
        
        result.doy = dayOfYear(result.month, result.day, result.year);
        result.hour= hour;
        result.minute= minute;
        result.seconds= justSeconds;
        
        return result;
    }
    
    /**
     * returns int[] { year, month, day, hour, minute, second, millis, micros }
     */
    public static int[] toTimeArray( Datum time ) {
        TimeStruct ts= toTimeStruct( time );
        int seconds= (int)( ts.seconds+0.0000005 );
        int micros= (int)( ( ts.seconds+0.0000005 - seconds ) * 1000000 );
        int millis= micros / 1000;
        micros= micros - millis * 1000 + ts.micros + ts.millis*1000;
        return new int[] { ts.year, ts.month, ts.day, ts.hour, ts.minute, seconds, millis, micros };
    }
    
    public static Datum toDatum( int[] timeArray ) {
        int year = timeArray[0];
        int month = timeArray[1];
        int day = timeArray[2];
        if ( timeArray[MONTH]<1 ) {
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
    
    public static boolean isLeapYear( int year ) {
        return (year % 4)==0;
    }
    
    public static TimeStruct carry(TimeStruct t) {
        TimeStruct result= t;
        
        if (result.seconds>=60) {
            result.seconds-=60;
            result.minute++;
        }
        if (result.minute>=60) {
            result.minute-=60;
            result.hour++;
        }
        if (result.hour>=24) {
            result.hour-=24;
            result.day++;
        }
        
        int daysThisMonth= daysInMonth(result.month,result.year);
        if (result.day>daysThisMonth) {
            result.day-=daysThisMonth;
            result.month++;
        }
        if (result.month>12) {
            result.month-=12;
            result.year++;
        }
        return result;
    }
    
    public static TimeStruct borrow(TimeStruct t) {
        TimeStruct result= t;
        
        if (result.seconds<0.) {
            result.seconds+=60.;
            result.minute--;
        }
        if (result.minute<0) {
            result.minute+=60;
            result.hour--;
        }
        if (result.hour<0) {
            result.hour+=24;
            result.day--;
        }
        
        if (result.day<0 || result.month<1) {
            // we're going to abort here.  The problem is how to decrement the month?
            // What does MONTH=-1, DAY=31 mean?  The "digits" are not independent as
            // they are in HOURS,MINUTES,SECONDS...  I don't think we are going to
            // run into this case anyway. jbf
            DasDie.die("Borrow operation not defined for months<1 or days<0");
        }
        if (result.day==0) {
            int daysLastMonth;
            if (result.month>1) {
                daysLastMonth= daysInMonth(result.year,result.month-1);
            } else {
                daysLastMonth= 31;
            }
            result.day+=daysLastMonth;
            result.month--;
        }
        
        if (result.month==0) {
            result.month+=12;
            result.year--;
        }
        
        return result;
    }
    
    public static TimeStruct normalize( TimeStruct t ) {
        return carry(borrow(t));
    }
    
    public static Datum next( int step, Datum datum ) {
        if ( step >= HOUR && step<WEEK ) throw new IllegalArgumentException("not tested");
        TimeStruct array= toTimeStruct(datum);
        switch (step) {
            case DAY:
                array.day= array.day+1;
                break;
            case MONTH:
                array.month=array.month+1;
                array.day=1;
                break;
            case QUARTER:
                array.month= ((int)(array.month-1)+3)/3*3+1;
                array.day=1;
                break;
            case YEAR:
                array.year=array.year+1;
                array.month=1;
                array.day=1;
                break;
            default:
                break;
        }
        if ( step < HOUR )  {
            array.hour=0;
            array.minute=0;
            array.seconds=0.;
        }
        
        if (array.month>12) {
            array.year++;
            array.month-=12;
        }
        Datum result= toDatum(array);
        return result;
    }
    
    public static Datum nextMonth(Datum datum) {
        return next(MONTH,datum);
    }
    
    public static Datum prev( int step, Datum datum ) {
        
        TimeStruct t= toTimeStruct(datum);
        
        switch(step) {
            case YEAR:
                t.month=1;
            case QUARTER:
                t.month= ((t.month-1)/3*3)+1;
            case MONTH:
                t.day= 1;
            case DAY:
                t.hour= 0;
            case HOUR:
                t.minute= 0;
            case MINUTE:
                t.seconds= 0.;
            case SECOND:
                t.seconds= (int)t.seconds;
        }
        
        Datum result= toDatum(t);
        if ( result.equals(datum) ) {
            return prev(step, datum.subtract( 500, Units.microseconds ));
        } else {
            return result;
        }
        
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
     * @param units the Units to return the result in.
     * @return a double in the context of units.
     */
    public static double convert(int year, int month, int day, int hour, int minute, double second, TimeLocationUnits units) {
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
    
    /**
     * returns 1..12 for the month name
     *
     * @throws ParseException if the name isn't recognized
     */
    public static int monthNumber( String s ) throws ParseException {
        final String[] mons = {
            "jan", "feb", "mar", "apr", "may", "jun",
            "jul", "aug", "sep", "oct", "nov", "dec"
        };
        s= s.substring(0,3).toLowerCase();
        for ( int i=0; i<12; i++ ) {
            if ( s.equals( mons[i] ) ) return i+1;
        }
        throw new ParseException("Unable to parse month", 0 );
    }
    
    public class TimeParser {
        String regex;
        int[] digits;
        
    }
    
    public static final TimeStruct parseTime(String s) throws java.text.ParseException {
        int year, month, day_month, day_year, hour, minute;
        //String s;
        double second;
        int c;
        
        final int DATE = 0;
        final int YEAR = 1;
        final int MONTH = 2;
        final int DAY = 3;
        final int HOUR = 4;
        final int MINUTE = 5;
        final int SECOND = 6;
        
        final String DELIMITERS = " \t/-:,_;";
        final String PDSDELIMITERS = " \t/-T:,_;";
        final String[] months = {
            "january", "febuary", "march", "april", "may", "june",
            "july", "august", "september", "october", "november", "december"
        };
        final String[] mons = {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        };
        
        
        String delimiters;
        int end_of_date;
        //GregorianCalendar curdate;
        int i, j, len, n;
        String[] tok = new String[10];
        boolean[] want = new boolean[7];
        int[] format= new int[7];
        
        java.util.Arrays.fill(want, false);
        int ptr;
        int number;
        double value;
        int hold;
        int leap;
        int tokIndex;
        
        StringTokenizer st;
        
        /* handl PDS time format */
        
        delimiters = DELIMITERS;
        if ((c = s.indexOf((int)'Z')) != -1) s = s.substring(0, c);
        end_of_date = s.indexOf((int)'T');
        if (end_of_date != -1) {
            c = end_of_date - 1;
            if (Character.isDigit(s.charAt(c))) delimiters = PDSDELIMITERS;
            else end_of_date = -1;
        }
        
        /* if not PDS then coiunt out 3 non-space delimiters */
        
        if (end_of_date == -1) {
            n = 0;
            len = s.length();
            for (i = 0; i < len; i++) {
                if ((c = (delimiters.substring(2)).indexOf(s.charAt(i))) != -1) n++;
                if (n == 3) {
                    end_of_date = i;
                    break;
                }
            }
        }
        
        /* default to current year */
        
        //curdate = new GregorianCalendar();
        //curdate.setTime(new Date());
        
        year = 0;
        month = 0;
        day_month = 0;
        day_year = 0;
        hour = 0;
        minute = 0;
        second= 0.0;
        
        /* tokenize the time string */
        st = new StringTokenizer(s, delimiters);
        
        if (!st.hasMoreTokens()) throw new java.text.ParseException( "No tokens in '"+s+"'", 0 );
        
        for (n = 0; n < 10 && st.hasMoreTokens(); n++) tok[n] = st.nextToken();
        
        want[DATE] = want[YEAR] = want[MONTH] = want[DAY] = true;
        hold = 0;
        
        tokIndex = -1;
        
        for (i = 0; i < n; i++) {
            tokIndex = s.indexOf(tok[i], tokIndex+1);
            if ((end_of_date != -1) && want[DATE] && tokIndex > end_of_date) {
                want[DATE] = false;
                want[HOUR] = want[MINUTE] = want[SECOND] = true;
            }
            
            len = tok[i].length();
            
            try {
                value = Double.parseDouble(tok[i]);
            } catch (NumberFormatException e) {
                if (len < 3 || !want[DATE]) throw new java.text.ParseException( "Error at token '"+tok[i]+"' in '"+s+"'", 0 );
                for (j = 0; j < 12; j++) {
                    if (tok[i].equalsIgnoreCase(months[j]) || tok[i].equalsIgnoreCase(mons[j])) {
                        month = j + 1;
                        want[MONTH] = false;
                        if (hold > 0) {
                            if (day_month > 0) throw new java.text.ParseException( "Ambiguous dates in token '"+tok[i]+"' in '"+s+"'", 0 );
                            day_month = hold;
                            hold = 0;
                            want[DAY] = false;
                        }
                        break;
                    }
                }
                if (want[MONTH]) throw new java.text.ParseException( "Error at token '"+tok[i]+"' in '"+s+"'", 0 );
                continue;
            }
            
            if (Math.IEEEremainder(value, 1.0) != 0.0) {
                if (want[SECOND]) {
                    second = value;
                    break;
                } else throw new java.text.ParseException( "Error at token '"+tok[i]+"' in '"+s+"'", 0 );
            }
            
            number = (int)value;
            if (number < 0) throw new java.text.ParseException( "Error at token '"+tok[i]+"' in '"+s+"'", 0 );
            
            if (want[DATE]) {
                
                if (number == 0) throw new java.text.ParseException( "m,d, or y can't be 0 in '"+s+"'", 0 );
                
                if (number > 31) {
                    
                    if (want[YEAR]) {
                        year = number;
                        if (year < 1000) year += 1900;
                        want[YEAR] = false;
                    } else if (want[MONTH]) {
                        want[MONTH] = false;
                        month = 0;
                        day_year = number;
                        want[DAY] = false;
                    } else throw new java.text.ParseException( "Error at token '"+tok[i]+"' in '"+s+"'", 0 );
                    
                } else if (number > 12) {
                    
                    if (want[DAY]) {
                        if (hold > 0) {
                            month = hold;
                            want[MONTH] = false;
                        }
                        if (len == 3) {
                            if (month > 0) throw new java.text.ParseException( "Error at token '"+tok[i]+"' in '"+s+"'", 0 );
                            day_year = number;
                            day_month = 0;
                            want[MONTH] = false;
                        } else day_month = number;
                        want[DAY] = false;
                    } else throw new java.text.ParseException( "Error at token '"+tok[i]+"' in '"+s+"'", 0 );
                    
                } else if (!want[MONTH]) {
                    
                    if (month > 0) {
                        day_month = number;
                        day_year = 0;
                    } else {
                        day_year = number;
                        day_month = 0;
                    }
                    want[DAY] = false;
                    
                } else if (!want[DAY]) {
                    
                    if (day_year > 0) throw new java.text.ParseException( "Error at token '"+tok[i]+"' in '"+s+"'", 0 );
                    month = number;
                    want[MONTH] = false;
                    
                } else if (!want[YEAR]) {
                    
                    if (len == 3) {
                        if (month > 0) throw new java.text.ParseException( "Error at token '"+tok[i]+"' in '"+s+"'", 0 );
                        day_year = number;
                        day_month = 0;
                        want[DAY] = false;
                    } else {
                        if (day_year > 0) throw new java.text.ParseException( "Error at token '"+tok[i]+"' in '"+s+"'", 0 );
                        month = number;
                        if (hold > 0) {
                            day_month = hold;
                            want[DAY] = false;
                        }
                    }
                    want[MONTH] = false;
                    
                } else if (hold > 0) {
                    
                    month = hold;
                    hold = 0;
                    want[MONTH] = false;
                    day_month = number;
                    want[DAY] = false;
                    
                } else hold = number;
                
                if (!(want[YEAR] || want[MONTH] || want[DAY])) {
                    want[DATE] = false;
                    want[HOUR] = want[MINUTE] = want[SECOND] = true;
                }
                
            } else if (want[HOUR]) {
                
                if (len == 4) {
                    hold = number / 100;
                    // TODO: handle times like Jan-1-2001T24:00 --> Jan-2-2001T00:00,  for ease of modifying times
                    if (hold > 23) throw new java.text.ParseException( "Error at token '"+tok[i]+"' in '"+s+"'", 0 );
                    hour = hold;
                    hold = number % 100;
                    if (hold > 59) throw new java.text.ParseException( "Error at token '"+tok[i]+"' in '"+s+"'", 0 );
                    minute = hold;
                    want[MINUTE] = false;
                } else {
                    if (number > 23) throw new java.text.ParseException( "Error at token '"+tok[i]+"' in '"+s+"'", 0 );
                    hour = number;
                }
                want[HOUR] = false;
                
            } else if (want[MINUTE]) {
                // TODO: handle times like 0:90 --> 1:30,  for ease of modifying times
                if (number > 59) throw new java.text.ParseException( "Error at token '"+tok[i]+"' in '"+s+"'", 0 );
                minute = number;
                want[MINUTE] = false;
                
            } else if (want[SECOND]) {
                
                if (number > 61) throw new java.text.ParseException( "Error at token '"+tok[i]+"' in '"+s+"'", 0 );
                second = number;
                want[SECOND] = false;
                
            } else throw new java.text.ParseException( "Error at token '"+tok[i]+"' in '"+s+"'", 0 );
            
        } /* for all tokens */
        
        if (month > 12) throw new java.text.ParseException("Month is greater than 12 in '"+s+"'",0);
        if (month > 0 && day_month <= 0) day_month = 1;
        
        leap = ((year & 3) > 0 ? 0 : ((year % 100) > 0 ? 1 : ((year % 400) > 0 ? 0 : 1)) );
        
        if ((month > 0) && (day_month > 0) && (day_year == 0)) {
            if (day_month > daysInMonth[leap][month]) throw new java.text.ParseException( "day of month too high in '"+s+"'",0 );
            day_year = dayOffset[leap][month] + day_month;
        } else if ((day_year > 0) && (month == 0) && (day_month == 0)) {
            if (day_year > (365 + leap)) throw new java.text.ParseException( "day of year too high in '"+s+"'",0 );
            for (i = 2; i < 14 && day_year > dayOffset[leap][i]; i++);
            i--;
            month = i;
            day_month = day_year - dayOffset[leap][i];
        } else {
            throw new java.text.ParseException( "Need month/day or doy in '"+s+"'",0 );
        }
        
        TimeStruct result= new TimeStruct();
        result.year = year;
        result.month = month;
        result.day = day_month;
        result.doy = day_year;
        result.hour = hour;
        result.minute = minute;
        result.seconds = second;
        result.isLocation= true;
        
        result.want= want;
        
        return result;
    }
    
    /** Creates a datum from a string
     * @param s
     * @throws ParseException
     * @return
     */
    public static Datum create(String s) throws java.text.ParseException {
        TimeStruct ts= parseTime(s);
        return toDatum(ts);
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
    
    public static void main(String[] args) throws Exception {
        System.out.println( TimeUtil.now() );
        System.out.println( Datum.create( TimeUtil.convert(2000,1,2, 0, 0, 0, Units.us2000 ), Units.us2000 ));
        Datum x=create( "2000-1-1 0:00:33.45" );
        System.out.println( x );
        
        TimeStruct ts= TimeUtil.toTimeStruct(x);
        System.out.println( TimeUtil.toDatum(ts) );
        
        TimeDatumFormatter tf = TimeDatumFormatter.DEFAULT;
        
        for ( int i=0; i<44; i++ ) {
            System.out.println(tf.format(x)+"\t"+(long)x.doubleValue(Units.us2000));
            x= TimeUtil.prev(SECOND,x);
        }
    }
    
    public static Datum prevMidnight(Datum datum) {
        return datum.subtract(getSecondsSinceMidnight(datum), Units.seconds);
    }
    
    public static Datum createTimeDatum( int year, int month, int day, int hour, int minute, int second, int nano ) {
        if ( year<1960 ) throw new IllegalArgumentException("year must be > 1960, and no 2 digit years (year="+year+")");
        
        int jd = 367 * year - 7 * (year + (month + 9) / 12) / 4 -
                3 * ((year + (month - 9) / 7) / 100 + 1) / 4 +
                275 * month / 9 + day + 1721029;
        double microseconds = second*1e6 + hour*3600e6 + minute*60e6 + nano/1e3;
        double us2000= UnitsConverter.getConverter(Units.mj1958,Units.us2000).convert(( jd - 2436205 )) + microseconds;
        return Datum.create( us2000, Units.us2000 );
    }
    
}
