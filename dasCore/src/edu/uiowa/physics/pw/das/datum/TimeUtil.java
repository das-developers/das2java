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

/**
 *
 * @author  jbf
 */
public class TimeUtil {
    
    private TimeUtil() {
    }
    
    public static class TimeStruct {
        int year;
        int month;
        int day;
        int doy;
        int hour;
        int minute;
        double seconds; // remaining number of seconds past minute boundary
    }
    
    public static final int YEAR = 1;
    public static final int MONTH = 2;
    public static final int DAY = 3;
    public static final int HOUR = 4;
    public static final int MINUTE = 5;
    public static final int SECOND = 6;
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
        return Datum.create( ( jd - 2436205 ) + seconds / 86400., Units.mj1958 );
    }
    
    public static TimeStruct toTimeStruct(Datum datum) {
        int jalpha, j1, j2, j3, j4, j5;
        int year, month, day, hour, minute;
        double justSeconds;
        
        int jd= getJulianDay(datum);
        double seconds= getSecondsSinceMidnight(datum);
        
        jalpha = (int)(((double)(jd - 1867216) - 0.25)/36524.25);
        j1 = jd + 1 + jalpha - jalpha/4;
        j2 = j1 + 1524;
        j3 = 6680 + (int)(((j2-2439870)-122.1)/365.25);
        j4 = 365*j3 + j3/4;
        j5 = (int)((j2-j4)/30.6001);
        
        day = j2 - j4 - (int)(30.6001*j5);
        month = j5-1;
        month = ((month - 1) % 12) + 1;
        year = j3 - 4715;
        year = year - (month > 2 ? 1 : 0);
        year = year - (year <= 0 ? 1 : 0);
        
        hour = (int)(seconds/3600.0);
        minute = (int)((seconds - hour*3600.0)/60.0);
        justSeconds = seconds - hour*(double)3600.0 - minute*(double)60.0;
        
        TimeStruct result= new TimeStruct();
        
        result.year= year;
        result.month= month;
        result.day= day;
        result.hour= hour;
        result.minute= minute;
        result.seconds= justSeconds;
        
        return result;
    }
    
    public static Datum next( int step, Datum datum ) {
        TimeStruct array= toTimeStruct(datum);
        switch (step) {
            case DAY:
                array.day= array.day+1;
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
        array.hour=0;
        array.minute=0;
        array.seconds=0.;
        
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
    
    public static Datum now() {
        Calendar cal= new GregorianCalendar();
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        int hour= cal.get(Calendar.HOUR_OF_DAY);
        int minute= cal.get(Calendar.MINUTE);
        int second= cal.get(Calendar.SECOND);
        int year= cal.get(Calendar.YEAR);
        int doy= cal.get(Calendar.DAY_OF_YEAR);
        
        return TimeUtil.create(year+"//"+doy+" "+hour+":"+minute+":"+second);
    }
    
    public static double convert(int year, int month, int day, int hour, int minute, double second, TimeLocationUnits units) {
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
    
    public static final TimeStruct parseTime(String s) {
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
        final int[][] day_offset = {
            {0, 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334, 365},
            {0, 0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335, 366}
        };
        int[][] days_in_month = {
            {0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31, 0},
            {0, 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31, 0}
        };
        
        String delimiters;
        int end_of_date;
        GregorianCalendar curdate;
        int i, j, len, n;
        String[] tok = new String[10];
        boolean[] want = new boolean[7];
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
        
        curdate = new GregorianCalendar();
        curdate.setTime(new Date());
        
        year = curdate.get(Calendar.YEAR);
        month = 0;
        day_month = 0;
        day_year = 0;
        hour = 0;
        minute = 0;
        second= 0.0;
        
        /* tokenize the time string */
        st = new StringTokenizer(s, delimiters);
        
        if (!st.hasMoreTokens()) return null;
        
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
                if (len < 3 || !want[DATE]) return null;
                for (j = 0; j < 12; j++) {
                    if (tok[i].equalsIgnoreCase(months[j]) || tok[i].equalsIgnoreCase(mons[j])) {
                        month = j + 1;
                        want[MONTH] = false;
                        if (hold > 0) {
                            if (day_month > 0) return null;
                            day_month = hold;
                            hold = 0;
                            want[DAY] = false;
                        }
                        break;
                    }
                }
                if (want[MONTH]) return null;
                continue;
            }
            
            if (Math.IEEEremainder(value, 1.0) != 0.0) {
                if (want[SECOND]) {
                    second = value;
                    break;
                } else return null;
            }
            
            number = (int)value;
            if (number < 0) return null;
            
            if (want[DATE]) {
                
                if (number == 0) return null;
                
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
                    } else return null;
                    
                } else if (number > 12) {
                    
                    if (want[DAY]) {
                        if (hold > 0) {
                            month = hold;
                            want[MONTH] = false;
                        }
                        if (len == 3) {
                            if (month > 0) return null;
                            day_year = number;
                            day_month = 0;
                            want[MONTH] = false;
                        } else day_month = number;
                        want[DAY] = false;
                    } else return null;
                    
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
                    
                    if (day_year > 0) return null;
                    month = number;
                    want[MONTH] = false;
                    
                } else if (!want[YEAR]) {
                    
                    if (len == 3) {
                        if (month > 0) return null;
                        day_year = number;
                        day_month = 0;
                        want[DAY] = false;
                    } else {
                        if (day_year > 0) return null;
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
                    if (hold > 23) return null;
                    hour = hold;
                    hold = number % 100;
                    if (hold > 59) return null;
                    minute = hold;
                    want[MINUTE] = false;
                } else {
                    if (number > 23) return null;
                    hour = number;
                }
                want[HOUR] = false;
                
            } else if (want[MINUTE]) {
                
                if (number > 59) return null;
                minute = number;
                want[MINUTE] = false;
                
            } else if (want[SECOND]) {
                
                if (number > 61) return null;
                second = number;
                want[SECOND] = false;
                
            } else return null;
            
        } /* for all tokens */
        
        if (month > 12) return null;
        if (month > 0 && day_month <= 0) day_month = 1;
        
        leap = ((year & 3) > 0 ? 0 : ((year % 100) > 0 ? 1 : ((year % 400) > 0 ? 0 : 1)) );
        
        if ((month > 0) && (day_month > 0) && (day_year == 0)) {
            if (day_month > days_in_month[leap][month]) return null;
            day_year = day_offset[leap][month] + day_month;
        } else if ((day_year > 0) && (month == 0) && (day_month == 0)) {
            if (day_year > (365 + leap)) return null;
            for (i = 2; i < 14 && day_year > day_offset[leap][i]; i++);
            i--;
            month = i;
            day_month = day_year - day_offset[leap][i];
        } else return null;
        
        TimeStruct result= new TimeStruct();
        result.year = year;
        result.month = month;
        result.day = day_month;
        result.doy = day_year;
        result.hour = hour;
        result.minute = minute;
        result.seconds = second;
        
        return result;
    }
    public static Datum create(edu.uiowa.physics.pw.das.util.DasDate date) {        
        edu.uiowa.physics.pw.das.util.DasDate base2000= new edu.uiowa.physics.pw.das.util.DasDate("01/01/2000 00:00");
        return Datum.create(date.subtract(base2000)*1000000,Units.us2000);
    }
    
    public static Datum create(String s) {
        TimeStruct ts= parseTime(s);
        return toDatum(ts);
    }
    
    public static void main(String[] args) {
        System.out.println( TimeUtil.now() );
        System.out.println( Datum.create( TimeUtil.convert(2000,1,2, 0, 0, 0, Units.us2000 ), Units.us2000 ));
        Datum x=create( "1972-1-17 13:26" );
        System.out.println( x );
        
        TimeStruct ts= TimeUtil.toTimeStruct(x);
        System.out.println( TimeUtil.toDatum(ts) );
    }
    
}
