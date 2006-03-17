/*
 * TimeParser.java
 *
 * Created on January 27, 2006, 3:51 PM
 *
 *
 */

package edu.uiowa.physics.pw.das.util;

import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.DatumRange;
import edu.uiowa.physics.pw.das.datum.TimeUtil;
import edu.uiowa.physics.pw.das.datum.TimeUtil.TimeStruct;
import edu.uiowa.physics.pw.das.datum.Units;

/**
 *
 * @author Jeremy
 */
public class TimeParser {
    
    public static final String TIMEFORMAT_Z= "%Y-%m-%dT%H:%M:%S.%{milli}Z";
    TimeStruct time;
    int ndigits;
    
    String[] valid_formatCodes= new String[] { "Y", "y", "j", "m", "d", "H", "M", "S", "milli", "micro", "p", "z", "ignore" };
    String[] formatName= new String[] { "Year", "2-digit-year", "day-of-year", "month", "day", "Hour", "Minute", "Second", "millisecond", "microsecond",
    "am/pm", "RFC-822 numeric time zone", "ignore" };
    int[] formatCode_lengths= new int[] { 4, 2, 3, -1, -1, 2, 2, 2, 3, 3, 2, 5, -1 };
    int [] precision= new int[] { 0, 0, 2, 1, 2, 3, 4, 5, 6, 7, -1, -1, -1 };
    
    int[] handlers;
    
    /**
     * positions of each digit, within the string to be parsed.  If position is -1, then we need to
     * compute it along the way.
     */
    int[] offsets;
    int[] lengths;
    String[] delims;
    
    String regex;

    /**
     * Least significant digit in format.
     *0=year, 1=month, 2=day, 3=hour, 4=min, 5=sec, 6=milli, 7=micro
     */
    int lsd; 
    
    private TimeParser( String formatString ) {
        time= new TimeUtil.TimeStruct();
        
        String[] ss= formatString.split("%");
        String[] fc= new String[ss.length];
        String[] delim= new String[ss.length+1];
        
        ndigits= ss.length;  

        StringBuffer regex= new StringBuffer(100);
        regex.append( ss[0] );
                
        delim[0]= ss[0];
        for ( int i=1; i<ndigits; i++ ) {
            if ( ss[i].charAt(0)!='{' ) {
                fc[i]= ss[i].substring(0,1);
                delim[i]= ss[i].substring(1);
            } else {
                int endIndex= ss[i].indexOf( '}', 1 );
                fc[i]= ss[i].substring(1,endIndex);
                delim[i]= ss[i].substring(endIndex+1);
            }
        }
        
        handlers= new int[ndigits];
        offsets= new int[ndigits];
        lengths= new int[ndigits];
        
        int pos= 0;
        offsets[0]= pos;
        lengths[0]= -1; // don't use!
        
        lsd= -1;
        for ( int i=1; i<ndigits; i++ ) {
            if ( pos != -1 ) pos+= delim[i-1].length();
            int handler= 9999;
            for ( int j=0; j<valid_formatCodes.length; j++ ) {
                if ( valid_formatCodes[j].equals( fc[i] ) ) {
                    handler= j;
                    break;
                }
            }
            if ( handler==9999 ) {
                throw new IllegalArgumentException( "bad format code: \""+fc[i]+"\"" );
            } else {
                handlers[i]= handler;                
                lengths[i]= formatCode_lengths[handler];
                offsets[i]= pos;
                if ( lengths[i]== -1|| pos== -1 ) {
                    pos= -1;
                } else {
                    pos+= formatCode_lengths[handler];
                }
            }
            
            if ( precision[handler] > lsd ) lsd= precision[ handler ];
            
            String dots=".........";
            if ( lengths[i]==-1 ) {
                regex.append( "(.*)" );
            } else {
                regex.append( "("+dots.substring(0,lengths[i])+")" );
            }
            regex.append( delim[i] );
            
        }
        
        this.delims= delim;
        this.regex= regex.toString();
    }   
    
    public static TimeParser create( String formatString ) {
        return new TimeParser( formatString );
    }
    
    private int parsePositiveInt( String s, int offset, int len ) {
        int result=0;
        int radix=1;
        for ( int j= offset+len-1; j>=offset; j-- ) {
            result+= radix * ( s.charAt(j)-'0' );
            radix= radix*10;
        }
        return result;
    }
    
    private double toUs2000( TimeStruct d ) {
        int year = (int)d.year;
        int month = (int)d.month;
        int day = (int)d.day;
        int jd = 367 * year - 7 * (year + (month + 9) / 12) / 4 -
                3 * ((year + (month - 9) / 7) / 100 + 1) / 4 +
                275 * month / 9 + day + 1721029;
        int hour = (int)d.hour;
        int minute = (int)d.minute;
        double seconds = d.seconds + hour*(float)3600.0 + minute*(float)60.0;
        double mjd1958= ( jd - 2436205 ) + seconds / 86400. ;
        double us2000= ( mjd1958 - 15340 ) * 86400000000. + seconds + d.millis * 1000 + d.micros;
        return us2000;
    }
    public TimeParser parse( String timeString ) {
        int offs= 0;
        int len= 0;
        
        time.month= 1;
        time.day= 1;
        time.hour= 0; 
        time.minute= 0;
        time.seconds= 0;
        time.micros= 0;
        
        for ( int idigit=1; idigit<ndigits; idigit++ ) {
            if ( offsets[idigit] != -1 ) {  // note offsets[0] is always known
                offs= offsets[idigit];
            } else {
                offs+= len + this.delims[idigit-1].length();
            }
            if ( lengths[idigit]!= -1 ) {
                len= lengths[idigit];
            } else {
                int i= timeString.indexOf( this.delims[idigit], offs );
                len= i-offs;
            }
            if ( handlers[idigit] < 10 ) {                
                int digit= parsePositiveInt( timeString, offs, len );
                switch ( handlers[idigit] ) {
                    case 0: time.year= digit; break;
                    case 1: time.year= digit < 58 ? 2000+digit : 1900 + digit; break;
                    case 2: time.month= 1; time.day= digit; break;
                    case 3: time.month= digit; break;
                    case 4: time.day= digit; break;
                    case 5: time.hour= digit; break;
                    case 6: time.minute= digit; break;
                    case 7: time.seconds= digit; break;
                    case 8: time.millis= digit; break;
                    case 9: time.micros= digit; break;
                }
            } else  if ( handlers[idigit]==10 ) {
                    char ch= timeString.charAt(offs );
                    if ( ch=='P' || ch=='p' ) {
                        time.hour+=12;
                    }
            } else if ( handlers[idigit]==11 ) {
                int offset= Integer.parseInt( timeString.substring( offs, offs+len ) );
                DasMath.modp(0,0);
                time.hour-= offset / 100;   // careful!
                time.minute-=  offset % 100;
            }            
        }
        return this;
    }
    
    public double getTime( Units units ) {
        return Units.us2000.convertDoubleTo( units, toUs2000( time ) );
    }
    
    public Datum getTimeDatum() {
        return Units.us2000.createDatum( toUs2000( time ) );
    }
    
    /**
     * Returns the implicit interval as a DatumRange.
     * For example, "Jan 1, 2003" would have a getTimeDatum of "Jan 1, 2003 00:00:00", 
     * and getDatumRange() would go from midnight to mignight. 
     */
    public DatumRange getTimeRange() {
        TimeStruct time2= time.copy();
        switch ( lsd ) {
            case 0: time2.year= time.year + 1; break;
            case 1: time2.month= time.month + 1; break;
            case 2: time2.day= time.day +1; break;
            case 3: time2.hour= time.hour +1; break;
            case 4: time2.minute= time.minute +1; break;
            case 5: time2.seconds= time.seconds +1; break;
            case 6: time2.millis= time.millis + 1; break;
            case 7: time2.micros= time.micros+1; break;
        }
        double t1= toUs2000( time );
        double t2= toUs2000( time2 );
        return new DatumRange( t1, t2, Units.us2000 );
    }
    
    public double getEndTime( Units units ) {
        throw new IllegalArgumentException("not implemented for DatumRanges as of yet" );
    }
    

    public String getRegex() {
        return this.regex;
    }
    
    public String format(Datum start, Datum end) {
        if ( true ) throw new IllegalArgumentException("not implemented");
        return null;
    }
}
