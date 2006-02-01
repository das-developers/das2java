/*
 * TimeParser.java
 *
 * Created on January 27, 2006, 3:51 PM
 *
 *
 */

package edu.uiowa.physics.pw.das.util;

import edu.uiowa.physics.pw.das.datum.Datum;
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
    
    String[] valid_formatCodes= new String[] { "Y", "y", "j", "m", "d", "H", "M", "S", "milli", "micro",  };
    String[] formatName= new String[] { "Year", "2-digit-year", "day-of-year", "month", "day", "Hour", "Minute", "Second", "millisecond", "microsecond" };
    int[] formatCode_lengths= new int[] { 4, 2, 3, 2, 2, 2, 2, 2, 3, 3 };
    int[] handlers;
    
    /**
     * positions of each digit, within the string to be parsed. 
     */
    int[] offsets;
    int[] lengths;
    
    private TimeParser( String formatString ) {
        time= new TimeUtil.TimeStruct();
        
        String[] ss= formatString.split("%");
        String[] fc= new String[ss.length];
        String[] delim= new String[ss.length+1];
        
        ndigits= ss.length;
        
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
        
        for ( int i=1; i<ndigits; i++ ) {
            pos+= delim[i-1].length();
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
                offsets[i]= pos;
                lengths[i]= formatCode_lengths[handler];
                pos+= formatCode_lengths[handler];
            }
        }
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
        for ( int i=1; i<ndigits; i++ ) {
            int digit= parsePositiveInt( timeString, offsets[i], lengths[i] );
            switch ( handlers[i] ) {
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
        }
        return this;
    }
    
    public double getTime( Units units ) {
        return Units.us2000.convertDoubleTo( units, toUs2000( time ) );
    }
    
    public Datum getTimeDatum() {
        return Units.us2000.createDatum( toUs2000( time ) );
    }
    
    public double getEndTime( Units units ) {
        throw new IllegalArgumentException("not implemented for DatumRanges as of yet" );
    }
    
   public static void main( String[] args ) throws Exception {
        String timestr;
        timestr= "2005-01-03T00:19:08.927Z";
        String format= "%Y-%m-%dT%H:%M:%S.%{milli}Z";
        TimeParser parser= TimeParser.create( format );
        parser.parse(timestr);
        System.out.println( " "+timestr+" --> " + Units.us2000.createDatum( parser.getTime(Units.us2000)) );
        
        timestr= "2005-01-03T00:12:00.926Z";
        parser= TimeParser.create( format );
        
        int nrep=100000;
        long t0= System.currentTimeMillis();
        for ( int i=0; i<nrep; i++ ) {
            parser.parse(timestr);   
        }
        System.out.println("fast parse parses in "+(System.currentTimeMillis()-t0));
        t0= System.currentTimeMillis();
        for ( int i=0; i<nrep; i++ ) {
            TimeUtil.parseTime(timestr);
        }
        System.out.println("slow parse parses in "+(System.currentTimeMillis()-t0));
        
    }
}
