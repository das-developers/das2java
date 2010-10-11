/*
 * DatumRangeUtil.java
 *
 * Created on September 16, 2004, 2:35 PM
 */

package org.das2.datum;

import java.text.*;
import java.util.regex.*;
import org.das2.datum.format.TimeDatumFormatter;

/**
 *
 * @author  Jeremy
 */
public class DatumRangeUtilOld {
    
    private static Datum parseUnit( double value, String s ) {
        // return Datum, attempting to make value be integer to avoid roundoffs        
        if ( s.equals("d") ) {
            return Units.microseconds.createDatum( value*86400*1e6 );
        } else if ( s.equals( "h" ) ) {
            return Units.microseconds.createDatum( value*3600*1e6 );
        } else if ( s.equals( "m" ) ) {
            return Units.microseconds.createDatum( value*60*1e6 );
        } else if ( s.equals( "s" ) ) {
            return Units.microseconds.createDatum( value*1e6 );
        } else if ( s.equals( "ms" ) ) {
            return Units.nanoseconds.createDatum( value*1e6 );
        } else if ( s.equals( "us" ) ) {
            return Units.nanoseconds.createDatum( value*1e3 );
        } else if ( s.equals( "ns" ) ) {
            return Units.nanoseconds.createDatum( value );
        } else {
            throw new IllegalArgumentException("Unit not recognized: "+s);
        }
    }
    
    private static Datum parseTimeWidth( String ss ) {
        long days=0, seconds=0L, nanos=0L;
        double rseconds= 0.d;
        double rnanos= 0.d;
        
        String[] s= ss.split(" ");
        
        Datum accum= Units.nanoseconds.createDatum(0.);
        int i=0;
        while ( i<s.length ) {
            while ( s[i].equals("") ) i++;
            double value= Double.parseDouble(s[i++]);
            while ( s[i].equals("") ) i++;
            String unit= s[i++];
            Datum datum= parseUnit( value,  unit );
            accum= accum.add(datum);
        }
        return accum;
    }
    
    public static int getIncrement( Datum startDatum, String s1 ) throws ParseException {
        try {
            if ( s1.length()<4 ) {
                s1= "2000-"+s1;
                TimeUtil.TimeStruct ts= TimeUtil.parseTime(s1); // check for ParseException
                return TimeUtil.DAY;
            } else if ( s1.length()<5 ) {
                s1= s1+"-001";
                TimeUtil.TimeStruct ts= TimeUtil.parseTime(s1); // check for ParseException
                return TimeUtil.YEAR;
            } else {
                TimeUtil.TimeStruct ts= TimeUtil.parseTime(s1);
                // when the day is not specified, loop doesn't work.  check for this.
                if ( ts.want[3] ) return 2;
                int i=6;
                while ( i>0 ) {
                    if ( !ts.want[i] ) {
                        break;
                    }
                    i--;
                }
                return i;
            }
        } catch ( ParseException e ) {
            String s= TimeDatumFormatter.DAYS.format(startDatum)+" "+s1;
            TimeUtil.TimeStruct ts= TimeUtil.parseTime(s);
            if ( ts.want[3] ) return 2;
            int i=6;
            while ( i>0 ) {
                if ( !ts.want[i] ) {
                    break;
                }
                i--;
            }
            return i;
        }
    }
        
    private static Datum getStartDatum( String s1 ) throws ParseException {
        if ( s1.length()<5 ) {
            s1= s1+"-001";
            TimeUtil.TimeStruct ts= TimeUtil.parseTime(s1);
            return TimeUtil.toDatum(ts);
        } else {
            TimeUtil.TimeStruct ts= TimeUtil.parseTime(s1);
            return TimeUtil.toDatum(ts);
        }
    }
    
    private static Datum getEndDatum( Datum startDatum, String s1 ) throws ParseException {
        Datum endStart;
        try {
            TimeUtil.TimeStruct ts;
            if ( s1.length()<4 ) {
                TimeUtil.TimeStruct tsStart= TimeUtil.toTimeStruct(startDatum);
                ts= TimeUtil.parseTime(""+tsStart.year+"-"+s1);
            } else if ( s1.length()<5 ) {                
                ts= TimeUtil.parseTime(s1+"-001");
            } else {
                ts= TimeUtil.parseTime(s1);
            }
            endStart= TimeUtil.toDatum(ts);
        } catch ( ParseException e ) {
            String s= TimeDatumFormatter.DAYS.format(startDatum)+" "+s1;
            TimeUtil.TimeStruct ts= TimeUtil.parseTime(s);
            endStart= TimeUtil.toDatum(ts);
        }
        
        int inc= getIncrement( startDatum, s1 );
        if ( inc <= TimeUtil.DAY ) {
            return TimeUtil.next( inc, endStart );
        } else {
            return endStart;
        }
        
    }
    
    public static DatumRange parseTimeRange( String s ) throws ParseException {
        String[] delims= new String[] { "[", "to", "through", "-" };
        int i=0;
        while ( i<delims.length ) {
            if ( s.indexOf(delims[i]) >= 0 ) break;
            i++;
        }
        String s1, s2;
        Datum d1, d2;
        Datum w2;
        
        if ( i == delims.length ) { // no delimeters found
            Datum start= getStartDatum(s);
            Datum end= getEndDatum( start, s );
            return new DatumRange( start, end );
        } else {
            if ( delims[i].equals("-") ) {
                String[] ss= s.split("-");
                if ( ss.length % 2 != 0 ) {
                    throw new IllegalArgumentException("times contain even number of -'s, giving up");
                } else {
                    int n= ss.length;
                    StringBuffer sb1= new StringBuffer(ss[0]);
                    for ( int ii=1; i<n/2; ii++ ) sb1.append( "-" ).append(ss[ii]);
                    StringBuffer sb2= new StringBuffer(ss[n/2]);
                    for ( int ii=n/2+1; i<n; ii++ ) sb2.append( "-" ).append(ss[ii]);
                    // special case YYYY-DDD is misidentified as range.  Handle this
                    if ( sb1.length()==4 && sb2.length()== 3 ) {  
                        String yyyy_ddd= sb1.append("/").append(sb2).toString();
                        Datum start= getStartDatum( yyyy_ddd );
                        Datum end= getEndDatum( start, yyyy_ddd );
                        return new DatumRange( start, end );
                    } 
                    Datum start= getStartDatum(sb1.toString());                     
                    Datum end= getEndDatum(start,sb2.toString());
                    return new DatumRange( start, end );
                }
            } else if ( delims[i].equals("[") ) {
                String[] ss= s.split("\\[");
                Datum start= getStartDatum(ss[0]);
                String widthString= ss[1];
                if ( widthString.indexOf(']') != -1 ) {
                    widthString= widthString.substring(0,widthString.indexOf(']'));
                }
                Datum width= parseTimeWidth(widthString);
                return new DatumRange( start, start.add(width) );
            } else {
                String[] ss= s.split(delims[i]);
                Datum start= getStartDatum(ss[0]);                
                return new DatumRange( start, getEndDatum( start,ss[1] ) );
            }
            
        }
    }
    
    public static DatumRange newDimensionless( double lower, double upper ) {
        return new DatumRange( Datum.create(lower), Datum.create(upper) );
    }
}