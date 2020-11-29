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

import java.io.IOException;
import java.util.*;

import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.format.TimeDatumFormatter;

/**
 * Various time utilities
 * @author  jbf
 */
public final class TimeUtil {
    
    private static final Logger logger= LoggerManager.getLogger("das2.datum.timeutil");

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
    
    /**
     * calculation of julianDay based on http://www.imcce.fr/en/grandpublic/temps/jour_julien.php
     * This is slightly slower because of a cusp at 1582, but is accurate
     * before these times.
     * @param YY  Gregorian year
     * @param MM  Gregorian month
     * @param DD Gregorian day
     * @return 
     */
    public static int julianDayIMCCE( int YY, int MM, int DD ) {
        int GGG = 1;
        if( YY < 1582 ) GGG = 0;
        if( YY <= 1582 && MM < 10 ) GGG = 0;
        if( YY <= 1582 && MM == 10 && DD < 5 ) GGG = 0;
        int JD = -1 * (7 * ( ((MM + 9) / 12) + YY) / 4);
        int S = 1;
        if ((MM - 9) < 0) S = -1;
        int A = Math.abs(MM - 9);
        int J1 = (YY + S * (A / 7));
        J1 = -1 * (((J1 / 100) + 1) * 3 / 4);
        JD = JD + (275 * MM / 9) + DD + (GGG * J1);
        JD = JD + 1721027 + 2 * GGG + 367 * YY;
        return JD;
    }
    
    /**
     * return the julianDay for the year month and day.  This was verified
     * against another calculation (julianDayWP, commented out above) from 
     * http://en.wikipedia.org/wiki/Julian_day.  Both calculations have 20 operations.
     * Note this does not match 
     * @see julianToGregorian
     * @param year calendar year greater than 1582.
     * @param month 
     * @param day day of month. For day of year, use month=1 and doy for day.
     * @return the Julian day
     */
    public static int julianDay( int year, int month, int day ) {
        if ( year<=1582 ) {
            throw new IllegalArgumentException("year must be more than 1582");
        }
        int jd = 367 * year - 7 * (year + (month + 9) / 12) / 4 -
                3 * ((year + (month - 9) / 7) / 100 + 1) / 4 +
                275 * month / 9 + day + 1721029;
        return jd;
    }
    
    /**
     * return the day of year for the month and day, for the given year
     * @param month the month, january=1, february=2, etc.
     * @param day day of month
     * @param year four-digit year
     * @return the day of year
     */
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

        /**
         * additional nanoseconds since minute boundary
         */
        public int nanos;

        public boolean isLocation= false;
        
        @Override
        public String toString() {
            if ( isLocation ) {
                int dayOfYear= dayOfYear( month, day, year );
                return String.format( Locale.US, "%4d/%02d/%02d %02d:%02d:%06.3f (doy=%03d)", year,month,day,hour,minute,seconds+millis/1000.+micros/1000000.,dayOfYear );
            } else {
                int intSeconds= (int)seconds;
                int nanos= (int)( 1000000000 * ( seconds - intSeconds ) );
                nanos+=this.nanos;
                nanos+=micros*1000;
                nanos+=millis*1000000;
                return DatumRangeUtil.formatISO8601Duration( new int[] { year,month,day,hour,minute,intSeconds,nanos } ); //TODO: test this.
            }
        }
        
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
            result.nanos= this.nanos;
            result.isLocation= this.isLocation;
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
            result.nanos= this.nanos + offset.nanos ;
            
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
    public static final int MILLI= 7;
    public static final int MICRO = 8;
    public static final int NANO = 9;
    public static final int WEEK = 97;
    public static final int QUARTER = 98;
    public static final int HALF_YEAR= 99;
    
    // introduced to aid in debugging.
    public static class TimeDigit {
        int ordinal; // YEAR, MONTH, etc.
        String label;
        int divisions; // approximate
        private static final TimeDigit[] digits= new TimeDigit[10];
        @Override
        public String toString(){
            return label;
        }
        private TimeDigit( int ordinal, String label, int divisions ) {
            this.ordinal= ordinal;
            this.label= label;
            this.divisions= divisions;
            digits[ordinal]= this;
        }
        public int getOrdinal() {
            return ordinal;
        }
        public int divisions() {
            return divisions;
        }
        public static TimeDigit fromOrdinal( int ordinal ) {
            return digits[ordinal];
        }
    }
    
    public static final TimeDigit TD_YEAR = new TimeDigit( 1, "YEAR", 12 );
    public static final TimeDigit TD_MONTH = new TimeDigit( 2, "MONTH", 30 );
    public static final TimeDigit TD_DAY = new TimeDigit( 3, "DAY", 24 );
    public static final TimeDigit TD_HOUR = new TimeDigit( 4, "HOUR", 60 );
    public static final TimeDigit TD_MINUTE = new TimeDigit( 5, "MINUTE", 60 );
    public static final TimeDigit TD_SECOND = new TimeDigit( 6, "SECOND", 1000 );
    public static final TimeDigit TD_MILLI= new TimeDigit( 7, "MILLISECONDS", 1000 );
    public static final TimeDigit TD_MICRO = new TimeDigit( 8, "MICROSECONDS", 1000 );
    public static final TimeDigit TD_NANO = new TimeDigit( 9, "NANOSECONDS", 1000 );
    
    /**
     * return the number of seconds elapsed since the last midnight.
     * @param datum
     * @return 
     */
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
    
    /**
     * return the number of microseconds elapsed since the last midnight.
     * @param datum
     * @return 
     */
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
     *
     * @param datum
     * @return
     */
    public static int getJulianDay( Datum datum ) {
        double xx= datum.doubleValue(Units.mj1958);
        return (int)Math.floor( xx ) + 2436205;
    }

    /**
     * return the the integer number of days that have elapsed since roughly Monday, January 1, 4713 BC.  Julian Day
     * is defined as starting at noon UT, here is is defined starting at midnight.
     *
     * @param val the magnitude
     * @param units Units object identifying the units of the val, such as Units.us2000.
     * @return
     */
    public static int getJulianDay( long val, Units units ) {
        if ( units==Units.us2000 ) {
            return (int)( val / 86400000000L ) + 15340 + 2436205;
        } else if ( units==Units.t2000 ) {
            return (int)( val / 86400L ) + 15340 + 2436205;
        } else if ( units==Units.mj1958 ) {
            return (int)( val ) + 2436205;
        } else {
            UnitsConverter uc= units.getConverter(Units.mj1958);
            return (int)Math.floor(uc.convert(val)) + 2436205;
        }
    }

    /**
     * convert to Datum without regard to the type of unit used to represent time.
     * This will use the canonical Units.us2000 for time locations, which does 
     * not represent leap seconds.  
     * @param d the decomposed time or time width.
     * @return the Datum.
     */
    public static Datum toDatum( TimeStruct d ) {
        if ( d.isLocation ) {
            int year = (int)d.year;
            int month = (int)d.month;
            int day = (int)d.day;
            int jd = 367 * year - 7 * (year + (month + 9) / 12) / 4 -
                    3 * ((year + (month - 9) / 7) / 100 + 1) / 4 +
                    275 * month / 9 + day + 1721029;
            double us2000= ( jd - 2451545 ) * 86400e6; // TODO: leap seconds 
            return Datum.create( d.hour * 3600.0e6 + d.minute * 60e6 + d.seconds * 1e6 + d.millis * 1000 + d.micros + us2000, Units.us2000  );
        } else {
            Datum result;
            if ( d.year>0 ) {
                result= Units.years.createDatum(d.year);
            } else if ( d.month>0 ) {
                result= Units.days.createDatum(d.month*30);
                logger.warning("approximating months");  // TODO: just add Units.months, which is no worse than Units.days or Units.years
            } else if ( d.day>0 ){
                result= Units.days.createDatum(d.day);
            } else {
                result= Units.seconds.createDatum( d.hour*3600 + d.minute*60 + d.seconds + d.millis/1e3 + d.micros/1e6 + d.nanos/1e9 );
            }
            return result;
        }
    }
    
    /**
     * convert to a Datum with the given units.
     * @param d the decomposed time
     * @param u the target units.
     * @return the Datum in the units specified.
     */
    public static Datum toDatum( TimeStruct d, Units u ) {
        if ( d.isLocation ) {
            int year = (int)d.year;
            int month = (int)d.month;
            int day = (int)d.day;
            int jd = 367 * year - 7 * (year + (month + 9) / 12) / 4 -
                    3 * ((year + (month - 9) / 7) / 100 + 1) / 4 +
                    275 * month / 9 + day + 1721029;
            if ( u==Units.cdfTT2000 ) {
                double us2000= ( jd - 2451545 ) * 86400e6; 
                double tt2000= Units.us2000.convertDoubleTo( Units.cdfTT2000, us2000 );
                Units.cdfTT2000.createDatum(tt2000);
                Datum rtt2000= Datum.create( d.hour * 3600.0e9 + d.minute * 60e9 + d.seconds * 1e9 + d.millis * 1e6 + d.micros*1e3 + tt2000, Units.cdfTT2000  );
                return rtt2000;
            } else if ( u!=Units.us2000 ) { // TODO: sub-optimal implementation...
                double us2000= ( jd - 2451545 ) * 86400e6; 
                Datum rus2000= Datum.create( d.hour * 3600.0e6 + d.minute * 60e6 + d.seconds * 1e6 + d.millis * 1000 + d.micros + us2000, Units.us2000  );
                return rus2000.convertTo(u);
            } else {
                double us2000= ( jd - 2451545 ) * 86400e6; // TODO: leap seconds 
                return Datum.create( d.hour * 3600.0e6 + d.minute * 60e6 + d.seconds * 1e6 + d.millis * 1000 + d.micros + us2000, Units.us2000  );
            }
        } else {
            return toDatum(d).convertTo(u);
        }
    }
    
    
    
    /**
     *Break the Julian day apart into month, day year.  This is based on
     *http://en.wikipedia.org/wiki/Julian_day (GNU Public License), and 
     *was introduced when toTimeStruct failed when the year was 1886.
     *@see julianDay( int year, int mon, int day )
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
        result.isLocation= true;
        return result;
    }
    
    /**
     * splits the time location datum into y,m,d,etc components, using the full
     * precision of the Long backing the Datum.  The datum can have the units
     * of ms1970 or cdf_tt2000, and 
     * @param datum with time location units.
     * @return TimeStruct containing the time components.
     */
    public static TimeStruct toTimeStruct( Datum.Long datum ) {
        Units u= datum.getUnits();
        
        if ( u==Units.ms1970 || u==Units.us2000 ) {
            long l= datum.longValue(u);

            long base;
            int nsMult; // multiplier to make units into nanoseconds
            
            if ( u==Units.ms1970 ) {
                base = 946684800000L;
                nsMult = 1000000;        
            } else if ( u==Units.us2000 ) {
                base = 0L;
                nsMult = 1000;        
            } else {
                throw new IllegalArgumentException("units must be cdfTT2000 or ms1970");
            }
            // calculate the number of days passed since 2000-01-01.
            long mn= l - base;
            if ( mn<0 ) {
                mn= mn / (86400000000L/nsMult) - 1;
            } else {
                mn= mn / (86400000000L/nsMult);            
            }
            
            int julianDay= 2451545 + (int)mn;

            long midnightDay= base + mn*(86400000000000L/nsMult); 

            TimeStruct result= julianToGregorian( julianDay );

            long sinceMidnight= l-midnightDay;

            long nanoseconds= sinceMidnight*nsMult;

            int hour = (int)(nanoseconds/(3600000000000L));
            if ( hour>23 ) hour= 23;
            int minute = (int)((nanoseconds - hour*3600000000000L)/60000000000L);
            if ( minute>59 ) minute= 59;
            long justNanoSeconds = nanoseconds - hour*3600000000000L - minute*60000000000L;
            
            result.doy = dayOfYear(result.month, result.day, result.year);
            result.hour= hour;
            result.minute= minute;
            result.seconds= justNanoSeconds / 1e9;

            result.isLocation= true;

            return result;
                        
        } else if ( u==Units.cdfTT2000 ) {
            long l= datum.longValue(u);
            int leapSeconds;
            try {
                leapSeconds= LeapSecondsConverter.getLeapSecondCountForTT2000(l);
            } catch ( IOException ex ) {
                throw new RuntimeException(ex); // this should not happen.
            }
            // convert to a new time format, nanoseconds since 2000-Jan-01 00:00:00.000Z.
            // Units.cdfTT2000's 0 is 2000-Jan-01 11:58:55.816000000,
            // so -43135816000000 is 2000-Jan-01 00:00:00.000000000.

            // calculate the number of days passed since 2000-01-01.
            long mn= ( ( l + 43135816000000L ) - (leapSeconds-32)*1000000000L );
            if ( mn<0 ) {
                mn= mn / 86400000000000L - 1;
            } else {
                mn= mn / 86400000000000L;            
            }

            int julianDay= 2451545 + (int)mn;

            long midnightCdfTT2000= mn * 86400000000000L + ( leapSeconds-32 ) *1000000000L - 43135816000000L; 

            TimeStruct result= julianToGregorian( julianDay );

            long sinceMidnight= l-midnightCdfTT2000;

            long nanoseconds= sinceMidnight;

            int hour = (int)(nanoseconds/3600000000000L);
            if ( hour>23 ) hour= 23;
            int minute = (int)((nanoseconds - hour*3600000000000L)/60000000000L);
            if ( minute>59 ) minute= 59;
            long justNanoSeconds = nanoseconds - hour*3600000000000L - minute*60000000000L;

            result.doy = dayOfYear(result.month, result.day, result.year);
            result.hour= hour;
            result.minute= minute;
            result.seconds= justNanoSeconds / 1e9;

            result.isLocation= true;

            return result;
        } else {
            throw new IllegalArgumentException("units must be cdfTT2000 or ms1970");

        }
    }



    /**
     * splits the time location datum into y,m,d,etc components.  Note that if
     * seconds is a double, and micros will be 0.
     * @param datum with time location units.
     * @return TimeStruct containing the time components.
     */
    public static TimeStruct toTimeStruct( Datum datum ) {
        
        Units u= datum.getUnits();
        
        if ( datum instanceof Datum.Long ) {
            if ( u==Units.cdfTT2000 || u==Units.ms1970 || u==Units.us2000 ) {
                return toTimeStruct((Datum.Long)datum);
            }
        }
        
        if ( u==Units.decimalYear ) {
            datum= datum.convertTo(Units.us2000);
            u= Units.us2000;
        }
        double d= datum.doubleValue(u);
        
        int mjd1958= (int)datum.doubleValue( Units.mj1958 );
        if ( mjd1958 < -714781 ) { // year 0001
            throw new IllegalArgumentException( "invalid time: mjd1958="+mjd1958 );
        }
        if ( mjd1958 > 2937613 ) { // year 9999
            throw new IllegalArgumentException( "invalid time: mjd1958="+mjd1958 );
        }
        double midnight= Units.mj1958.convertDoubleTo( u, mjd1958 );
        double sinceMidnight= d-midnight;
        
        int jd= 2436205 + mjd1958;
        logger.log(Level.FINER, "julian day: {0}", jd );

        if ( u==Units.cdfTT2000 && sinceMidnight<0.0 ) { //TODO: huh?  this needs review  TODO: document when this happens.
            TimeStruct result= julianToGregorian( jd );
            boolean isLeap= ( result.month==1 && result.day==1 ); // TODO: still kludgy
            if ( isLeap ) {
                mjd1958= mjd1958-1;
                jd= 2436205 + mjd1958;
                sinceMidnight= sinceMidnight+86401e9;
            } 
        }

        double nanoseconds= u.getOffsetUnits().convertDoubleTo( Units.nanoseconds, sinceMidnight );
        logger.log(Level.FINER, "nanoseconds since midnight: {0}", nanoseconds);

        if ( jd<0 ) {
            throw new IllegalArgumentException("julian day is negative.");
        }

        if ( nanoseconds<0 ) {
            jd= jd-1;
            nanoseconds += 86400e9; // no leap
        }

        if ( nanoseconds>=86400e9 && u!=Units.cdfTT2000 ) {
            jd= jd+1;
            nanoseconds -= 86400e9; // no leap
            logger.finer("nanoseconds roundoff kludge");
        }

        TimeStruct result= julianToGregorian( jd );

        int hour = (int)(nanoseconds/3600.0e9);
        if ( hour>23 ) hour= 23;
        int minute = (int)((nanoseconds - hour*3600.0e9)/60.0e9);
        if ( minute>59 ) minute= 59;
        double justNanoSeconds = nanoseconds - hour*3600.0e9 - minute*60.0e9;

        result.doy = dayOfYear(result.month, result.day, result.year);
        result.hour= hour;
        result.minute= minute;
        result.seconds= justNanoSeconds / 1e9;

        result.isLocation= true;
        
        return result;
    }

    public static TimeStruct add( TimeStruct a, TimeStruct b ) {
        if ( b.year>1000 && a.year>1000 ) {
            throw new IllegalArgumentException("cannot add more than 1000 years at a time.  Did you attempt to add two time locations?");
        }
        TimeStruct result= new TimeStruct();
        result.year= a.year + b.year;
        result.month= a.month + b.month;
        result.day= a.day + b.day;
        result.doy= a.doy + b.doy;
        result.hour= a.hour + b.hour;
        result.minute= a.minute + b.minute;
        result.seconds= a.seconds + b.seconds;
        result.millis= a.millis + b.millis;
        result.micros= a.micros + b.micros;
        result.nanos= a.nanos + b.nanos;
        result.isLocation= a.isLocation || b.isLocation;
        return result;
    }
    
    public static TimeStruct subtract( TimeStruct a, TimeStruct b ) {
        TimeStruct result= new TimeStruct();
        result.year= a.year - b.year;
        result.month= a.month - b.month;
        result.day= a.day - b.day;
        result.doy= a.doy - b.doy;
        result.hour= a.hour - b.hour;
        result.minute= a.minute - b.minute;
        result.seconds= a.seconds - b.seconds;
        result.millis= a.millis - b.millis;
        result.micros= a.micros - b.micros;
        result.nanos= a.nanos - b.nanos;
        return result;
    }
    
    /**
     * returns int[] { year, month, day, hour, minute, second, millis, micros }
     * @param time the time
     * @return the time decomposed into year, month, day, hour, minute, second, millis, micros.
     * @deprecated use 7-element fromDatum instead, which is consistent with toDatum.  Note the array elements are different!
     */
    public static int[] toTimeArray( Datum time ) {
        TimeStruct ts= toTimeStruct( time );
        int seconds= (int)( ts.seconds+0.0000005 );
        int micros= (int)( ( ts.seconds+0.0000005 - seconds ) * 1000000 );
        int millis= micros / 1000;
        micros= micros - millis * 1000 + ts.micros + ts.millis*1000;
        return new int[] { ts.year, ts.month, ts.day, ts.hour, ts.minute, seconds, millis, micros };
    }
    
    
    /**
     * returns the 7-element array of components from the time location datum:
     * 0:year, 1:month, 2:day, 3:hour, 4:minute, 5:second, 6:nanoseconds
     * @param time
     * @return seven-element int array.
     */
    public static int[] fromDatum( Datum time ) {
        TimeStruct ts= toTimeStruct( time );
        int seconds= (int)( ts.seconds+0.0000000005 );
        int nanos= (int)( ( ts.seconds+0.0000000005 - seconds ) * 100000000 ) + ts.nanos;
        return new int[] { ts.year, ts.month, ts.day, ts.hour, ts.minute, seconds, nanos };
    }
    
    /**
     * get the datum from the 6 or 7 element timeArray.  The elements are:
     * 0:year, 1:month, 2:day, 3:hour, 4:minute, 5:second, [ 6:nanoseconds ] 
     * 
     * @param timeArray, an int[6] or int[7].
     * @return a datum representing the time.
     */
    public static Datum toDatum( int[] timeArray ) {
        int year = timeArray[0];
        int month = timeArray[1];
        int day = timeArray[2];
        if ( month<1 ) { // support a little slop, like 2018-00-01 to mean Dec 2017.
            logger.info("month was less than 0");
            month+=12;
            year-=1;
        }
        if ( month>12 ) {
            month-=12;
            year+=1;
        }
        if ( month<1 ) throw new IllegalArgumentException("month is less than 1");
        if ( month>12 ) throw new IllegalArgumentException("month is greater than 12");
        int jd = 367 * year - 7 * (year + (month + 9) / 12) / 4 -
                3 * ((year + (month - 9) / 7) / 100 + 1) / 4 +
                275 * month / 9 + day + 1721029;
        int hour = (int)timeArray[3];
        int minute = (int)timeArray[4];
        double seconds = timeArray[5] + hour*(float)3600.0 + minute*(float)60.0 ;
        if ( timeArray.length>6 ) seconds+= timeArray[6]/1e9;
        double us2000= UnitsConverter.getConverter(Units.mj1958,Units.us2000).convert(( jd - 2436205 ) + seconds / 86400. );
        return Datum.create( us2000, Units.us2000 );
    }

    /**
     * return approximate duration in Units.seconds or in Units.days.
     * This is assuming a year is 365 days, a month is 30 days, and a day 
     * is 86400 seconds.
     * @param timeArray 6 or 7 element array [ yrs, months, days, hours, minutes, seconds, nanos ]
     * @return 
     * @see DatumRangeUtil#parseISO8601Duration(java.lang.String) 
     */
    public static Datum toDatumDuration( int[] timeArray ) {
        int year = timeArray[0];
        int month = timeArray[1];
        int day = timeArray[2];
        int days= day + month*30 + year*365;
        if ( timeArray.length==7 ) {
            if ( days==0 ) {
                return Units.seconds.createDatum( timeArray[3]*3600 + timeArray[4]*60 + timeArray[5] + timeArray[6]/1e9 );
            } else {
                return Units.days.createDatum( days + timeArray[3]/24. + timeArray[4]/1440. + timeArray[5]/86400. + timeArray[6]/86400e9 );
            }
        } else {
            if ( days==0 ) {
                return Units.seconds.createDatum( timeArray[3]*3600 + timeArray[4]*60 + timeArray[5] );
            } else {
                return Units.days.createDatum( days + timeArray[3]/24. + timeArray[4]/1440. + timeArray[5]/86400. );
            }            
        }
        
    }
    
    /**
     * get the time datum from the 6 or 7 element decomposed timeArray.  The elements are:
     * 0:year, 1:month, 2:day, 3:hour, 4:minute, 5:second, [ 6:nanoseconds ] 
     * 
     * @param timeArray, an int[6] or int[7].
     * @param u target units.
     * @return a datum representing the time.
     */
    public static Datum toDatum( int[] timeArray, Units u ) {
        int year = timeArray[0];
        int month = timeArray[1];
        int day = timeArray[2];
        int jd = 367 * year - 7 * (year + (month + 9) / 12) / 4 -
                3 * ((year + (month - 9) / 7) / 100 + 1) / 4 +
                275 * month / 9 + day + 1721029;
        int nanos= timeArray.length==6 ? 0 : timeArray[6];
        if ( u==Units.cdfTT2000 ) {
            double us2000= ( jd - 2451545 ) * 86400e6; 
            long tt2000= (long) Units.us2000.convertDoubleTo( Units.cdfTT2000, us2000 ); // TODO verify this.
            Datum rtt2000= new Datum.Long( 
                    timeArray[3] * 3600000000000L 
                    + timeArray[4] * 60000000000L 
                    + timeArray[5] *  1000000000L
                    + nanos + tt2000, Units.cdfTT2000  );
            return rtt2000;
        } else if ( u!=Units.us2000 ) { // TODO: sub-optimal implementation...
            double us2000= ( jd - 2451545 ) * 86400e6; 
            Datum rus2000= Datum.create( timeArray[3] * 3600.0e6 + timeArray[4] * 60e6 + timeArray[5] * 1e6 + nanos / 1e3 + us2000, Units.us2000  );
            return rus2000.convertTo(u);
        } else {
            double us2000= ( jd - 2451545 ) * 86400e6; // TODO: leap seconds 
            Datum rus2000= Datum.create( timeArray[3] * 3600.0e6 + timeArray[4] * 60e6 + timeArray[5] * 1e6 + nanos / 1e3 + us2000, Units.us2000  );
            return rus2000;
        }        
    }
    
    /**
     * return the leap year for years 1901-2099.
     * @param year
     * @return
     */
    public static boolean isLeapYear( int year ) {
        return (year % 4)==0 && ( year%400==0 || year%100!=0 );
    }
    
    /**
     * Normalize the TimeStruct by incrementing higher digits.  For
     * example, 2002-01-01T24:00 &rarr;  2002-01-02T00:00.
     * This will only carry one to the next higher place, so 70 seconds is handled but not 130.
     * 2015-09-08: this now supports leap seconds.
     * @param t a time structure
     * @return a time structure where extra minutes are moved into hours, etc.
     */
    public static TimeStruct carry(TimeStruct t) {
        TimeStruct result= t;
        
        boolean isLeap= false;
        if ( result.seconds>=60 ) {
            if ( ( result.month==6 && result.day==30 ) || ( result.month==12 && result.day==31 ) ) {
                isLeap= true; //TODO: note this incorrect for non-leap-seconds.
            }
            if ( result.hour<23 || result.minute<59 || !isLeap ) {
                result.seconds-=60;
                result.minute++;
            }
        }
        if (result.minute>=60) {
            result.minute-=60;
            result.hour++;
        }
        if (result.hour>=24) {
            result.hour-=24;
            result.day++;
        }
        while (result.month>12) {
            result.month-=12;
            result.year++;
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
    
    /**
     * Normalize the TimeStruct by decrementing higher digits.
     * @throws IllegalArgumentException if t.day&lt;0 or t.month&lt;1
     * @param t the time.
     * @return the normalized TimeStruct
     * @see #normalize(org.das2.datum.TimeUtil.TimeStruct) 
     */
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
            throw new IllegalArgumentException("Borrow operation not defined for months<1 or days<0");
        }
        if (result.day==0) {
            int daysLastMonth;
            if (result.month>1) {
                daysLastMonth= daysInMonth(result.month-1,result.year);
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

    /**
     * convert times like "2000-01-01T24:00" to "2000-01-02T00:00" and
     * "2000-002T00:00" to "2000-01-02T00:00".
     * This will only carry one to the next higher place, so 70 seconds is handled but not 130.
     * @param t
     * @return
     * @see #borrow(org.das2.datum.TimeUtil.TimeStruct) 
     */
    public static TimeStruct normalize( TimeStruct t ) {
        if ( t.doy>0 && t.day==0 ) {
             int leap= isLeapYear(t.year) ? 1: 0;
             if ( t.doy>dayOffset[leap][13] ) throw new IllegalArgumentException("doy>"+dayOffset[leap][13]+")");
             int month= 12;
             while ( dayOffset[leap][month] > t.doy ) {
                 month--;
             }
             t.day= t.doy - dayOffset[leap][month];
             t.month= month;
        }
        return carry(borrow(t));
    }

    /**
     * round seconds to N decimal places.  For example, n=3 means round to the 
     * millisecond.
     * @param ts a time structure
     * @param n number of digits, 3 is millis, 6 is micros.
     * @return rounded and normalized TimeStruct.
     */
    public static TimeStruct roundNDigits(TimeStruct ts, int n) {
        if ( n>6 ) {
            throw new IllegalArgumentException("only 0 to 6 digits supported");
        }
        double fracSeconds= ts.seconds - (int)ts.seconds;
        ts.seconds= (int)ts.seconds;
        ts.micros+= ts.millis*1000;
        ts.millis= 0;
        double pow= Math.pow( 10, 6-n );
        int roundMicros= (int)( Math.round( (double)( ts.micros + 1000000 * fracSeconds ) / pow ) * pow );
        ts.micros= roundMicros;
        if ( ts.micros>=1000000 ) {
            ts.micros-= 1000000;
            ts.seconds++;
        }
        return normalize(ts);
        
    }
    
    
    /**
     * return the next boundary
     * @param td the boundary, e.g. TimeUtil.TD_HALF_YEAR
     * @param count the number of boundaries 
     * @param datum the starting point.
     * @return the boundary
     */
    public static Datum next( TimeDigit td, int count, Datum datum ) {
        if ( td==TD_NANO ) throw new IllegalArgumentException("not supported nanos");
        TimeStruct array= toTimeStruct(datum);
        int step= td.getOrdinal();
        switch (td.getOrdinal()) {
            case MILLI:
                array.millis+= count;
                break;
            case SECOND: 
                array.seconds+= count;
                break;
            case MINUTE: 
                array.minute+= count;
                break;
            case HOUR:
                array.hour+= count;
                break;
            case DAY:
                array.day+= count;
                break;
            case MONTH:
                array.month+=count;
                array.day=1;
                break;
            case YEAR:
                array.year+= count;
                array.month= 1;
                array.day= 1;
                break;
            default:
                break;
        }
        
        if ( step < MILLI ) array.millis= 0;
        if ( step < SECOND ) array.seconds=0;
        if ( step < MINUTE ) array.minute=0;
        if ( step < HOUR ) array.hour=0;
            
        if (array.month>12) {
            array.year++;
            array.month-=12;
        }
        Datum result= toDatum(array);
        
        return result;
    }

    /**
     * introduced as a way to increase the efficiency of the time axis tick calculation, this wasn't
     * used because datums need to be created anyway.  So this is private for now.
     * @param step, e.g. SECOND, MONTH, QUARTER, YEAR
     * @param array the decomposed time.
     * @return the next boundary.
     */
    private static TimeStruct next( int step, TimeStruct array ) {
        switch (step) {
            case SECOND: 
                array.seconds= array.seconds+1;
                break;
            case MINUTE: 
                array.minute= array.minute+1;
                break;
            case HOUR:
                array.hour= array.hour+1;
                break;
            case DAY:
                array.day= array.day+1;
                break;
            case MONTH:
                array.month=array.month+1;
                array.day=1;
                break;
            case QUARTER:
                array.month= ((array.month-1)+3)/3*3+1;
                array.day=1;
                break;
            case HALF_YEAR:
                array.month= ((array.month-1)+6)/6*6+1;
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
        return array;
    }
    
    /**
     * step to the next ordinal.  If the datum is already at an ordinal
     * boundary, then step to the next by one ordinal.
     * @param step the ordinal location, such as TimeUtil.DAY or TimeUtil.HALF_YEAR
     * @param datum the location.
     * @return the next boundary location.
     */
    public static Datum next( int step, Datum datum ) {
        if ( step==NANO ) throw new IllegalArgumentException("not supported nanos");
        return toDatum( next( step, toTimeStruct(datum) ) );
    }
    
    /**
     * return the next ordinal boundary if we aren't at one already.
     * @param step the ordinal location, such as TimeUtil.DAY or TimeUtil.HALF_YEAR
     * @param datum the location.
     * @return the next ordinal boundary if we aren't at one already.
     */
    public static Datum ceil( int step, Datum datum ) {
        Datum next= next( step, datum );
        Datum t1= prev( step, next );
        if ( t1.equals(datum) ) {
            return datum;
        } else {
            return next;
        }
    }

    /**
     * return the previous ordinal boundary if we aren't at one already.
     * @param step the ordinal location, such as TimeUtil.DAY or TimeUtil.HALF_YEAR
     * @param datum the location.
     * @return the previous ordinal boundary if we aren't at one already.
     */
    public static Datum floor( int step, Datum datum ) {
        Datum prev= prev( step, datum );
        Datum t1= next( step, prev );
        if ( t1.equals(datum) ) {
            return datum;
        } else {
            return prev;
        }
    }
    
    /**
     * step to the next month.
     * @param datum
     * @return the next month.
     * @deprecated Use next(MONTH,datum) instead
     */
    public static Datum nextMonth(Datum datum) {
        return next(MONTH,datum);
    }

    /**
     * decrement by 7 days.
     * @param datum 
     * @return the datum
     */
    public static Datum prevWeek( Datum datum ) {
        TimeStruct t= toTimeStruct(datum);
        t.day= t.day-7;
        if ( t.day<1 ) {
            t.month--;
            t.day+= daysInMonth( t.month, t.year );
        }
        return toDatum(t);
    }

    /**
     * return a DatumRange for the day containing the given time.  Midnight
     * is contained within the following day.
     * @param t a time.
     * @return the day containing the time.
     */
    public static DatumRange dayContaining( Datum t ) {
        Datum midnight= prevMidnight(t);
        return new DatumRange( midnight, next( DAY, midnight ) );
    }
    
    
    /**
     * step down the previous ordinal.  If the datum is already at an ordinal
     * boundary, then step down by one ordinal.
     * @param step the ordinal location, such as TimeUtil.DAY or TimeUtil.HALF_YEAR
     * @param datum the location.
     * @return the prev boundary location.
     */
    public static Datum prev( int step, Datum datum ) {
        
        TimeStruct t= toTimeStruct(datum);
        
        switch(step) {
            case WEEK:
                throw new IllegalArgumentException("not supported, use prevWeek");
            default:
                throw new IllegalArgumentException("unsupported step, implementation error");
            case YEAR:
                t.month=1;
            case HALF_YEAR:
                t.month= ((t.month-1)/6*6)+1;
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
            Datum d= datum.subtract( 500, Units.milliseconds );
            if ( d.equals(result) ) {
                throw new IllegalStateException("aborting to avoid stack overflow!");
            }
            return prev(step,d ); //TODO: yuck!
        } else {
            return result;
        }
        
    }
    
    /**
     * return the current time as a Datum.
     * @return the current time as a Datum.
     */
    public static Datum now() {
        double us2000= ( System.currentTimeMillis() - 946684800e3 ) * 1000;
        return Units.us2000.createDatum(us2000);
    }
    
    /**
     * convert the month components to a double in the given units.
     * @param year the year, which must be greater than 1582
     * @param month the month
     * @param day the day of month, unless month==0, then day is day of year.
     * @param hour additional hours
     * @param minute additional minutes
     * @param second additional seconds
     * @param units the Units in which to return the result.
     * @return a double in the given units.
     */
    public static double convert(int year, int month, int day, int hour, int minute, double second, TimeLocationUnits units) {
        // if month==0, then day is doy (day of year).
        int jd;
        if ( month>0 ) {
            jd = julianDay(year,month,day);
        } else {
            // if month==0 then day is doy  (TODO: why? Clients should use 1 for doy.)
            int month1= 1;
            int day1= 1;
            jd = julianDay(year,month1,day1);
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
     * returns 1..12 for the English month name.  (Sorry, rest of world...)
     *
     * @param s the three-letter month name, jan,feb,...,nov,dec
     * @return 1,2,...,11,12 for the English month name
     * @throws ParseException if the name isn't recognized
     */
    public static int monthNumber( String s ) throws ParseException {
        if ( s.length()<3 ) throw new ParseException("need at least three letters",0);
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
    
    /**
     * parse the time into a timestruct.
     * @param s
     * @see #createValid(java.lang.String) createValid which creates a Datum.
     * @return
     * @throws java.text.ParseException
     */
    public static TimeStruct parseTime(String s) throws java.text.ParseException {
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
                
        String delimiters;
        int end_of_date;
        //GregorianCalendar curdate;
        int i, j, len, n;
        String[] tok = new String[10];
        boolean[] want = new boolean[7];
        //int[] format= new int[7];
        
        java.util.Arrays.fill(want, false);
        int ptr;
        int number;
        double value;
        int hold;
        int leap;
        int tokIndex;
        
        StringTokenizer st;
        
        s= s.trim();
        if ( s.length()==0 ) throw new java.text.ParseException("string is empty",0);
        if ( s.charAt(0)=='-') throw new ParseException("string starts with minus sign",0);
        
        if ( s.equals("now") ) {
            return toTimeStruct( TimeUtil.now() );
        }
        
        /* handl PDS time format */
        
        delimiters = DELIMITERS;
        if ((c = s.indexOf((int)'Z')) != -1) s = s.substring(0, c);
        end_of_date = s.indexOf((int)'T');
        if (end_of_date > 1) { // don't mistake "T" in "TIME"
            c = end_of_date - 1;
            if (Character.isDigit(s.charAt(c))) delimiters = PDSDELIMITERS;
            else end_of_date = -1;
        }
        
        /* if not PDS then count out 3 non-space delimiters */
        
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
                if ( tok[i].length()>0 && Character.isLetter(tok[i].charAt(0) ) ) {
                    throw new NumberFormatException("must start with a number: "+tok[i]); // caught immediately.
                }
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
                if (want[MONTH]) {
                    throw new java.text.ParseException( "Error at token '"+tok[i]+"' in '"+s+"'", 0 );
                }
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

                if ( number >= 10000000 && want[YEAR] ) { // %Y%m%d
                    year= number / 10000;
                    want[YEAR]= false;
                    month= number / 100 % 100;
                    want[MONTH]= false;
                    day_month= number % 100;
                    day_year= 0;
                    want[DAY]= false;
                } else if (number >= 1000000 && want[YEAR] ) { //%Y%j
                    year= number / 1000;
                    want[YEAR]= false;
                    day_year= number % 1000;
                    month= 0;
                    want[MONTH]= false;
                    want[DAY]= false;

                } else if (number > 31) {
                    
                    if (want[YEAR]) {
                        if ( hold!=0 && year<100 && year>50 ) {
                            throw new ParseException("Held digit ("+hold+") before two-digit year ("+year+"): "+s,0);
                        }
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
                    if (hold > 24) throw new java.text.ParseException( "Error at token '"+tok[i]+"' in '"+s+"'", 0 );
                    hour = hold;
                    hold = number % 100;
                    if (hold > 60) throw new java.text.ParseException( "Error at token '"+tok[i]+"' in '"+s+"'", 0 );
                    minute = hold;
                    want[MINUTE] = false;
                } else {
                    if (number > 24) throw new java.text.ParseException( "Error at token '"+tok[i]+"' in '"+s+"'", 0 );
                    hour = number;
                }
                want[HOUR] = false;
                
            } else if (want[MINUTE]) {
                // TODO: handle times like 0:90 --> 1:30,  for ease of modifying times
                if (number > 60) throw new java.text.ParseException( "Error at token '"+tok[i]+"' in '"+s+"'", 0 );
                minute = number;
                want[MINUTE] = false;
                
            } else if (want[SECOND]) {
                
                if (number > 61) throw new java.text.ParseException( "Error at token '"+tok[i]+"' in '"+s+"'", 0 );
                second = number;
                want[SECOND] = false;
                
            } else throw new java.text.ParseException( "Error at token '"+tok[i]+"' in '"+s+"'", 0 );
            
        } /* for all tokens */


        if ( want[YEAR] ) {
            throw new java.text.ParseException("This doesn't appear to contain a year: '"+s+"'",0);
        }

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
            if ( month==0 ) month= 1;
            day_month= 1;
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
        //System.out.println( "TimeUtil.parse="+TimeUtil.parseTime("1"));
        System.out.println(""+isLeapYear(1900) ); //logger okay
        System.out.println(""+isLeapYear(2000) ); //logger okay
        System.out.println(""+isLeapYear(1996) ); //logger okay
        System.out.println(""+isLeapYear(1999) ); //logger okay
        System.out.println(""+isLeapYear(2100) ); //logger okay

        System.out.println( "TimeUtil.parse="+TimeUtil.parseTime("2010")); //logger okay
        System.out.println( TimeUtil.now() ); //logger okay
        System.out.println( Datum.create( TimeUtil.convert(2000,1,2, 0, 0, 0, Units.us2000 ), Units.us2000 )); //logger okay
        Datum x=create( "2000-1-1 0:00:33.45" );
        System.out.println( x ); //logger okay
        
        TimeStruct ts= TimeUtil.toTimeStruct(x);
        System.out.println( TimeUtil.toDatum(ts) ); //logger okay
        
        TimeDatumFormatter tf = TimeDatumFormatter.DEFAULT;
        
        for ( int i=0; i<44; i++ ) {
            System.out.println(tf.format(x)+"\t"+(long)x.doubleValue(Units.us2000)); //logger okay
            x= TimeUtil.prev(SECOND,x);
        }

        Units[] uu= new Units[] { Units.cdfEpoch, Units.us1980, Units.us2000, Units.mj1958 };

        for ( int i=0; i<uu.length; i++ ) {
            Units u= uu[i];
            for ( int j=0; j<10000; j++ ) {
                Datum d= u.createDatum(j);
                String s= d.toString();
                ts= toTimeStruct(d);
                Datum d1= TimeUtil.toDatum(ts);
                if ( !d1.equals(d) ) {
                    if ( d1.subtract(d).doubleValue(Units.microseconds)< 0.000001 ) continue;
                    System.err.println( d1.subtract(d) ); //logger okay
                    System.err.println( ""+i+" "+j+": " +d + " "+d1 +" "+ ts ); //logger okay
                }
            }
        }
    }
    
    /**
     * provide the previous midnight, similar to the floor function, noting that
     * if the datum provided is midnight exactly then it is simply returned.
     * @param datum
     * @return the Datum for the next day boundary.
     */
    public static Datum prevMidnight(Datum datum) {
        //return datum.subtract(getMicroSecondsSinceMidnight(datum), Units.microseconds);
        return datum.subtract(getSecondsSinceMidnight(datum), Units.seconds);
    }
    
    /**
     * provide the next midnight, similar to the ceil function, noting that
     * if the datum provided is midnight already then it is simply returned.
     * @param datum
     * @return the Datum for the next day boundary.
     */
    public static Datum nextMidnight( Datum datum ) {
        double d= getMicroSecondsSinceMidnight(datum);
        if ( d==0 ) {
            return datum;
        } else {
            return next( DAY, datum);
        }
    }
    /**
     * creates a Datum representing the time given in integer years, months, ..., seconds, nanoseconds.  The year
     * must be at least 1000, and must be a four-digit year.  A double in Units.us2000 is used to represent the
     * Datum, so resolution will drop as the year drops away from 2000.
     *
     * @param year four digit year &gt;= 1060.
     * @param month integer month, 1..12.
     * @param day integer day of month.
     * @param hour additional hours
     * @param minute additional minutes
     * @param second additional seconds
     * @param nano additional nanoseconds
     * @return a Datum with units Units.us2000.
     */
    public static Datum createTimeDatum( int year, int month, int day, int hour, int minute, int second, int nano ) {
        if ( year<1000 ) throw new IllegalArgumentException("year must not be < 1000, and 2 digit years are not allowed(year="+year+")");
        if ( year>9001 ) throw new IllegalArgumentException("year must be smaller than 9000");
        int jd = 367 * year - 7 * (year + (month + 9) / 12) / 4 -
                3 * ((year + (month - 9) / 7) / 100 + 1) / 4 +
                275 * month / 9 + day + 1721029;
        double microseconds = second*1e6 + hour*3600e6 + minute*60e6 + nano/1e3;
        double us2000= UnitsConverter.getConverter(Units.mj1958,Units.us2000).convert(( jd - 2436205 )) + microseconds;
        return Datum.create( us2000, Units.us2000 );
    }
    
}
