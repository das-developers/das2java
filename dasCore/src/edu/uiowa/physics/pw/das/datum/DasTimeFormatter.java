/* File: DasTimeFormatter.java
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

package edu.uiowa.physics.pw.das.datum;

import edu.uiowa.physics.pw.das.datum.DasFormatter;

/**
 *
 * @author  jbf
 */
public class DasTimeFormatter extends DasFormatter {
    
    private TimeContext timeContext;
    private boolean showDoy;
    private boolean alwaysShowDate;
    
    public DasTimeFormatter(TimeContext timeContext) {
        this.timeContext= timeContext;
        showDoy= false;
        alwaysShowDate= false;
    }
    
    private static int julday( int month, int day, int year ) {
        int jd = 367 * year - 7 * (year + (month + 9) / 12) / 4 -
        3 * ((year + (month - 9) / 7) / 100 + 1) / 4 +
        275 * month / 9 + day + 1721029;
        return jd;
    }
            
    public String format(TimeDatum td) {
        
        double seconds;
        int jd;  // julianDay
        
        if (td.getUnits()==Units.mj1958) {
            double mj1958= td.getValue();
            seconds= mj1958 % 1 * 86400.;
            jd= (int)Math.floor(mj1958) + 2436205;
        } else if (td.getUnits()==Units.us2000) {
            double us2000= td.getValue();
            seconds= edu.uiowa.physics.pw.das.util.DasMath.modp( us2000, 86400000000. ) / 1000000;
            jd= (int)Math.floor( us2000 / 86400000000. ) + 2451545;
        } else {
            double us2000= Units.getConverter(td.getUnits(),Units.us2000).convert(td.getValue());
            seconds= edu.uiowa.physics.pw.das.util.DasMath.modp( us2000, 86400000000. ) / 1000000;
            jd= (int)Math.floor( us2000 / 86400000000. ) + 2451545;
        }
        
        
        if (timeContext==null) timeContext= TimeContext.DAYS;
        
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
    
        if (showDoy) {
            int jd_jan1= julday(1,1,year);            
            sdate+= " ("+(jd-jd_jan1+1)+")";
        }
        
        String result;
        if (timeContext==TimeContext.MILLISECONDS) {
            second= ((int)seconds)%60;
            ssecond= (second<10.)?"0"+second:""+second;
            long milliSeconds = Math.round((seconds%1.0)*1000.0);
            result= shour + ":" + sminute + ":" + ssecond + "."
            + (milliSeconds<100L ? "0" + (milliSeconds<10L ? "0" : "") : "") + milliSeconds;
        } else if (timeContext==TimeContext.SECONDS || timeContext==TimeContext.MINUTES ) {
            result= shour + ":" + sminute + ":" + ssecond;
        } else if (timeContext==TimeContext.HOURS) {
            //if (hour==0 && minute==0 && second==0 ) {
            //result= sdate + " " + shour + ":" + sminute;
            //} else {
            result= shour + ":" + sminute;
            //}
        } else if (timeContext==TimeContext.DAYS) {
            result = sdate;
        } else {
            if (hour==0) {
                result= sdate;
            } else {
                result= sdate;
            }
        }
        
        
        if ( ! timeContext.gt(TimeContext.HOURS) && alwaysShowDate ) {
            result= sdate+" "+result;
        }
        
        return result;
    }
    
    public String format(Object o) {
        // Ed, does my code do anything?
        if ( !( o instanceof TimeDatum ) ) {
            throw new IllegalArgumentException("Argument is not a TimeDatum! ("+o.getClass().getName()+")" );
        }
        TimeDatum d= (TimeDatum)o;
        return format(d);
    }
    
    public void setShowDoy(boolean value) {
        this.showDoy= value;
    }
    
    public void setAlwaysShowDate(boolean value) {
        this.alwaysShowDate= value;
    }
    
    public String format( double d, Units units ) {
        return format( new TimeDatum( d, units ) );
    }
    
    public Datum parse(String s, Datum d) {
        return TimeDatum.create(s);
    }
    
    public TimeContext getContext() {
        return this.timeContext;
    }
    
    public static void main( String[] args ) {
        TimeDatum d= TimeDatum.create("2001-3-1");
        DasTimeFormatter t= new DasTimeFormatter(TimeContext.DAYS);
        DasTimeFormatter t1= new DasTimeFormatter(TimeContext.MILLISECONDS);
        edu.uiowa.physics.pw.das.util.DasDie.println(t.format(d));
        edu.uiowa.physics.pw.das.util.DasDie.println(t.format(d.convertTo(Units.t2000)));
        edu.uiowa.physics.pw.das.util.DasDie.println(t.format(d.convertTo(Units.mj1958)));
        
        edu.uiowa.physics.pw.das.util.DasDie.println("---------------------");
        edu.uiowa.physics.pw.das.util.DasDie.println(t1.format(d));
        edu.uiowa.physics.pw.das.util.DasDie.println(t1.format(TimeDatum.create("2001-3-1 01:14")));
        TimeDatum d2= TimeDatum.create("1996-3-1 01:14");
        edu.uiowa.physics.pw.das.util.DasDie.println(t1.format(d2));
        edu.uiowa.physics.pw.das.util.DasDie.println(t.format(d2));
    }
}
