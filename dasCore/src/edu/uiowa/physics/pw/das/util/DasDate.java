/* File: DasDate.java
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

package edu.uiowa.physics.pw.das.util;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;
/**
 *
 * @author  eew
 */

public class DasDate implements java.io.Serializable, Comparable, Cloneable {
    
    protected int jd;
    protected double seconds;
    
    /** Creates a new instance of DasDate */
    private DasDate() {
    }
    
    public DasDate(double[] d) {
        int year = (int)d[YEAR];
        int month = (int)d[MONTH];
        int day = (int)d[DAY];
        jd = 367 * year - 7 * (year + (month + 9) / 12) / 4 -
        3 * ((year + (month - 9) / 7) / 100 + 1) / 4 +
        275 * month / 9 + day + 1721029;
        int hour = (int)d[HOUR];
        int minute = (int)d[MINUTE];
        seconds = d[SECOND]
        + hour*(float)3600.0 + minute*(float)60.0;
    }
    
    public DasDate(String s) throws IllegalArgumentException {
        int year, month, day, hour, minute;
        Number[] d = parseTime(s);
        if (d == null)
            throw new IllegalArgumentException("Could not parse date string: \"" + s + "\"");
        year = d[YEAR].intValue();
        month = d[MONTH].intValue();
        day = d[DAY].intValue();
        hour = d[HOUR].intValue();
        jd = 367 * year - 7 * (year + (month + 9) / 12) / 4 -
        3 * ((year + (month - 9) / 7) / 100 + 1) / 4 +
        275 * month / 9 + day + 1721029;
        minute = d[MINUTE].intValue();
        seconds = d[SECOND].floatValue() + hour*(float)3600.0 + minute*(float)60.0;
    }

    public static DasDate valueOf(Date javaDate) {
	SimpleDateFormat df= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");	

	DasDate result= new DasDate(df.format(javaDate));
	return result;
    }

    private static final String DELIMITERS = " \t/-:,_;";
    private static final String PDSDELIMITERS = " \t/-T:,_;";
    
    public static class context {
        public static final int MILLISECONDS= -990;
        public static final int SECONDS= -991;
        public static final int MINUTES= -992;
        public static final int HOURS= -993;
        public static final int DAYS= -994;
        public static final int DATE= -995;
        
        public static int getContextFromSeconds(double seconds) {
            int context;
            if (seconds<1) { context=MILLISECONDS; }
            else if (seconds<60) { context=SECONDS; }
            else if (seconds<3600) { context=MINUTES; }
            else if (seconds<86400) { context=HOURS; }
            else if (seconds<=864000) { context=DAYS; }
            else { context=DATE; }
            return context;
        }
        
    }
    
    public static final int DATE = 0;
    public static final int YEAR = 1;
    public static final int MONTH = 2;
    public static final int DAY = 3;
    public static final int HOUR = 4;
    public static final int MINUTE = 5;
    public static final int SECOND = 6;
    public static final int WEEK = 97;
    public static final int QUARTER = 98;
    
    private static final String[] months = {
        "january", "febuary", "march", "april", "may", "june",
        "july", "august", "september", "october", "november", "december"
    };
    private static final String[] mons = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };
    
    private static final int[][] day_offset = {
        {0, 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334, 365},
        {0, 0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335, 366}
    };
    private static final int[][] days_in_month = {
        {0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31, 0},
        {0, 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31, 0}
    };
    public static final Number[] parseTime(String s) {
        int year, month, day_month, day_year, hour, minute;
        //String s;
        double second;
        int c;
        String delimiters;
        int end_of_date;
        GregorianCalendar curdate;
        //time_t curtime;
        //struct tm *curtm;
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
        Number[] result = new Number[7];
        
        StringTokenizer st;
        
        //s = new String(string);
        
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
        
        result[YEAR] = new Integer(year);
        result[MONTH] = new Integer(month);
        result[DAY] = new Integer(day_month);
        result[DATE] = new Integer(day_year);
        result[HOUR] = new Integer(hour);
        result[MINUTE] = new Integer(minute);
        result[SECOND] = new Double(second);
        
        return result;
    }
    
    /**
     * Returns a string representation of this date object in
     * form yyyy-MM-ddThh:mm:ss.sss
     */
    public String toString() {
        int jalpha, j1, j2, j3, j4, j5;
        int year, month, day, hour, minute;
        double justSeconds;
        
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
        return year + "-" + month + "-" + day + "T" + getTimeAsString();
    }
    
    public DasDate previous(int step) {
        double [] d7= toArray();
        if (step<=DasDate.MONTH) {
            edu.uiowa.physics.pw.das.util.DasDie.die("operation not defined yet...see DasDate.borrow");
        }
        int idx= step;
        if (d7[step+1]==0) {
            d7[step]--;
        }
        for (int i=step+1; i<d7.length; i++) d7[i]=0;
        
        d7= borrow(d7);  // this handles the day decrement
        // what am i thinking...why not just do this with
        // julian day...
        return new DasDate(d7);
        
    }
    
    public DasDate next(int step) {
        double [] array= toArray();
        switch (step) {
            case DAY:
                array[DAY]= array[DAY]+1;
            case MONTH:
                array[MONTH]=array[MONTH]+1;
                array[DAY]=1;
                break;
            case QUARTER:
                array[MONTH]= ((int)(array[MONTH]-1)+3)/3*3+1;
                array[DAY]=1;
                break;
            case YEAR:
                array[YEAR]=array[YEAR]+1;
                array[MONTH]=1;
                array[DAY]=1;
                break;
            default:
                break;
        }
        array[HOUR]=0;
        array[MINUTE]=0;
        array[SECOND]=0.;
        
        if (array[MONTH]>12) {
            array[YEAR]++;
            array[MONTH]-=12;
        }
        DasDate result= new DasDate(array);
        return result;
    }
    
    public DasDate add(double seconds) {
        return add(this, seconds);
    }
    
    public static DasDate add(DasDate d, double seconds) {
        DasDate result = new DasDate();
        result.seconds = d.seconds + (float)seconds;
        int days= (int)Math.floor(result.seconds/86400.);
        result.jd = d.jd + days;
        result.seconds -= (float)(days*86400);
        return result;
    }
    
    public double subtract(DasDate d) {
        return subtract(this, d);
    }
    
    public static double subtract(DasDate d1, DasDate d2) {
        return (double)(d1.jd - d2.jd)*86400d + (double)(d1.seconds - d2.seconds);
    }
    
    /**
     * Compares the specified object to this one for order.
     *
     * The specified object must be a DasDate.
     *
     * @param o the object to be compared to <code>this</code>
     * @return a negative integer, zero, or a positive integer as <code>this</code>
     *    is less than, equal to, or greater than the argument.
     * @throws ClassCastException if (o instanceof DasDate) evaluates to false
     */
    public int compareTo(Object o) {
        DasDate d = (DasDate)o;
        if (this.jd < d.jd)
            return -1;
        if (this.jd > d.jd)
            return 1;
        if (this.seconds < d.seconds)
            return -1;
        if (this.seconds > d.seconds)
            return 1;
        return 0;
    }
    
    public boolean lt(Object o) {
        return (this.compareTo(o)==-1 );
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof DasDate)) return false;
        if (this == o) return true;
        DasDate d = (DasDate)o;
        return (this.jd == d.jd) && (this.seconds == d.seconds);
    }
    
    public static double timeLengthFromString(String stime) {
        double result=0;
        if (stime.length()<2) return result;
        if (stime.substring(stime.length()-2).equals("ms"))
            stime= stime.substring(0,stime.length()-2)+"M";
        StringTokenizer tok= new StringTokenizer(stime,"dhmsM",true);
        while (tok.hasMoreTokens()) {
            String v= tok.nextToken();
            String u= tok.nextToken();
            if (u.equals("d")) result+=86400*Integer.parseInt(v);
            else if (u.equals("h")) result+=3600*Integer.parseInt(v);
            else if (u.equals("m")) result+=60*Integer.parseInt(v);
            else if (u.equals("s")) result+=Integer.parseInt(v);
            else if (u.equals("M")) result+=1/1000.*Integer.parseInt(v);
        }
        return result;
    }
    
    
    public static String timeLengthToString(double seconds) {
        int days= (int) ( seconds / 86400. );
        seconds-= days*86400;
        int hours= (int) (seconds / 3600. );
        seconds-= hours*3600;
        int minutes= (int) (seconds / 60. );
        seconds-= minutes*60;
        int iseconds= (int) (seconds);
        seconds-= iseconds;
        int millis= (int)Math.round( seconds*1000 );
        
        int [] d5= new int[]{days,hours,minutes,iseconds,millis};
        String[] s5= new String[]{"d","h","m","s","ms"};
        
        int istart=-1;
        int iend=-1;
        for (int i=0;i<5;i++) {
            if (d5[i]>0) {
                if (istart==-1) istart=i; else iend=i;
            }
        }
        if (istart==-1) {
            istart=4;
            iend=4;
        }
        if (iend==-1) {
            iend=istart;
        }
        
        String result="";
        for (int i=istart; i<=iend; i++)
            result+=d5[i]+s5[i];
        return result;
        
    }
    
    public String getTimeAsString() {
        return getTimeAsString(context.SECONDS);
    }
    
    public String getTimeAsString(int cntext) {
        
        int iseconds= (int)(seconds+0.5);
        
        int year, month, day, hour, minute, second;
        
        hour = (int)(iseconds/3600);
        String shour=(hour<10.)?"0"+hour:""+hour;
        minute = (int)((iseconds - hour*3600)/60);
        String sminute=(minute<10.)?"0"+minute:""+minute;
        second = (int)(iseconds%60);
        String ssecond=(second<10.)?"0"+second:""+second;
        
        int jalpha, j1, j2, j3, j4, j5;
        
        float justSeconds;
        
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
        String sdate= year + "-" + month + "-" + day;
        
        String result;
        if (cntext==context.MILLISECONDS) {
            ssecond= Integer.toString(((int)seconds)%60);
            long milliSeconds = Math.round((seconds%1.0)*1000.0);
            result= shour + ":" + sminute + ":" + ssecond + "."
            + (milliSeconds<100L ? "0" + (milliSeconds<10L ? "0" : "") : "") + milliSeconds;
        } else if (cntext==context.SECONDS || cntext==context.MINUTES ) {
            result= shour + ":" + sminute + ":" + ssecond;
        } else if (cntext==context.HOURS) {
            //if (hour==0 && minute==0 && second==0 ) {
	    //result= sdate + " " + shour + ":" + sminute;
            //} else {
	    result= shour + ":" + sminute;
	    //}
	} else if (cntext==context.DATE) {
	    result = sdate;
        } else {
            if (hour==0) {
                result= sdate;
            } else {
                result= sdate+'.'+(iseconds*10/86400);
            }
        }
        
        return result;
    }

    public double[] toArray() {
        int jalpha, j1, j2, j3, j4, j5;
        int year, month, day, hour, minute;
        double justSeconds;
        
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
        
        double [] result= new double[7];
        result[YEAR]= year;
        result[MONTH]= month;
        result[DAY]= day;
        result[HOUR]= hour;
        result[MINUTE]= minute;
        result[SECOND]= justSeconds;
        
        return result;
    }
    
    public int getJulianDay() {
        return jd;
    }
    
    public void setJulianDay(int JulianDay){
        this.jd= JulianDay;
    }
    
    public double getSecondsSinceMidnight() {
        return seconds;
    }
    
    public void setSecondsSinceMidnight(double secondsSinceMidnight) {
        this.seconds= secondsSinceMidnight;
    }
    
    public double[] toArray(int context) {
        double[] d7= toArray();
        switch (context) {
            case DasDate.context.MILLISECONDS:
                d7[SECOND]= Math.round( d7[SECOND] * 1000. ) / 1000.;
                break;
            case DasDate.context.SECONDS:
                d7[SECOND]= Math.round( d7[SECOND] );
                break;
            case DasDate.context.MINUTES:
                d7[MINUTE]+= Math.round(d7[SECOND]/60.);
                d7[SECOND]= 0.;
                break;
            case DasDate.context.HOURS:
                d7[HOUR]+= Math.round(d7[MINUTE]/60.+d7[SECOND]/3600.);
                d7[SECOND]= d7[MINUTE]= 0.;
                break;
            case DasDate.context.DAYS:
                d7[DAY]+= Math.round(d7[HOUR]/24.+d7[MINUTE]/1440.+d7[SECOND]/86400.);
                d7[HOUR]= d7[SECOND]= d7[MINUTE]= 0.;
        }
        d7= carry(d7);
        return d7;
    }
    
    public static double [] carry( double [] d7 ) {
        double[] result= new double[d7.length];
        for (int i=0; i<result.length; i++)
            result[i]= d7[i];
        
        if (result[SECOND]>=60) {
            result[SECOND]-=60;
            result[MINUTE]++;
        }
        if (result[MINUTE]>=60) {
            result[MINUTE]-=60;
            result[HOUR]++;
        }
        if (result[HOUR]>=24) {
            result[HOUR]-=24;
            result[DAY]++;
        }
        
        int daysThisMonth= days_in_month[((int)result[YEAR]%4)==0?1:0][(int)result[MONTH]];
        if (result[DAY]>daysThisMonth) {
            result[DAY]-=daysThisMonth;
            result[MONTH]++;
        }
        if (result[MONTH]>12) {
            result[MONTH]-=12;
            result[YEAR]++;
        }
        return result;
    }
    
    public static double[] borrow( double[] d7) {
        double[] result= new double[d7.length];
        for (int i=0; i<result.length; i++)
            result[i]= d7[i];
        
        if (result[SECOND]<0) {
            result[SECOND]+=60;
            result[MINUTE]--;
        }
        if (result[MINUTE]<0) {
            result[MINUTE]+=60;
            result[HOUR]--;
        }
        if (result[HOUR]<0) {
            result[HOUR]+=24;
            result[DAY]--;
        }
        
        if (result[DAY]<0 || result[MONTH]<1) {
            // we're going to abort here.  The problem is how to decrement the month?
            // What does MONTH=-1, DAY=31 mean?  The "digits" are not independent as
            // they are in HOURS,MINUTES,SECONDS...  I don't think we are going to
            // run into this case anyway. jbf
            edu.uiowa.physics.pw.das.util.DasDie.die("Borrow operation not defined for months<1 or days<0");
        }
        if (result[DAY]==0) {
            int daysLastMonth;
            if (result[MONTH]>1) {
                daysLastMonth= days_in_month[((int)result[YEAR]%4)==0?1:0][(int)result[MONTH]-1];
            } else {
                daysLastMonth= 31;
            }
            result[DAY]+=daysLastMonth;
            result[MONTH]--;
        }
        
        if (result[MONTH]==0) {
            result[MONTH]+=12;
            result[YEAR]--;
        }
        
        return result;
    }
    
    public String getTimeAsHersheyString(int cntext) {
        double[] d7= toArray(cntext);
        
        DecimalFormat nf= new DecimalFormat();
        nf.applyPattern("00");
        
        int day   = (int)d7[DAY];
        String month = mons[(int)d7[MONTH]-1];
        int year  = (int)d7[YEAR];
        String sdate= "" + day + " " + month + " " + year;
        
        String result;
        
        int hour= (int)d7[HOUR];
        int minute= (int)d7[MINUTE];
        double second=d7[SECOND];
        
        String shour= nf.format(hour);
        String sminute= nf.format(minute);
        if (cntext==context.MILLISECONDS) {
            DecimalFormat nf3= new DecimalFormat();
            nf3.applyPattern("00.000");
            result= shour + ":" + sminute + ":" +
            "!K" + nf3.format(second) + "!N";
        } else if (cntext==context.SECONDS ) {
            result= shour + ":" + sminute + ":" + nf.format(second);
        } else if ( cntext==context.MINUTES || cntext==context.HOURS) {
            if (hour==0 && minute==0 && second==0 ) {
                result= shour + ":" + sminute + "!C" + sdate ;
            } else {
                result= shour + ":" + sminute;
            }
        } else if (cntext==context.DAYS) {
            result= sdate;
        } else {
            result= sdate;
        }
        
        return result;
    }
    
    public DasDate subtract(double seconds) {
        return add(this,-1*seconds);
    }
    
    public static DasDate create(edu.uiowa.physics.pw.das.datum.TimeDatum datum) {
        DasDate result= new DasDate();
        edu.uiowa.physics.pw.das.datum.Datum datumT2000= datum.convertTo(edu.uiowa.physics.pw.das.datum.Units.t2000);
        result.jd= 2451545 + (int)Math.floor( datumT2000.doubleValue(datumT2000.getUnits()) / 86400. );
        result.seconds= datumT2000.doubleValue(datumT2000.getUnits()) - 
          86400 * Math.floor( datumT2000.doubleValue(datumT2000.getUnits()) / 86400. );
        return result;
    }
    
}
