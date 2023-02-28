
package org.das2.datum;

import java.text.*;
import java.util.*;
import java.util.logging.*;
import java.util.regex.*;
import org.das2.datum.format.DatumFormatter;
import org.das2.datum.format.TimeDatumFormatter;

/**
 * Utility functions for DatumRanges.
 * @author  Jeremy
 */
public class DatumRangeUtil {

    private static final Logger logger= LoggerManager.getLogger("das2.datum");

    private static final boolean DEBUG=false;


    // this pattern is always a year
    private static boolean isYear( String string ) {
        return string.length()==4 && Pattern.matches("\\d{4}",string);
    }
    
    // this pattern is always a day of year
    private static boolean isDayOfYear( String string ) {
        return string.length()==3 && Pattern.matches("\\d{3}",string);
    }
    
    private static int monthNumber( String string ) throws ParseException {
        if ( Pattern.matches("\\d+", string) ) {
            return parseInt(string);
        } else {
            int month= monthNameNumber(string);
            if ( month==-1 ) throw new ParseException("hoping for month at, got "+string, 0);
            return month;
        }
    }
    
    private static int monthNameNumber( String string ) {
        if ( string.length() < 3 ) return -1;
        String[] monthNames= new String[] { "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec" };
        string= string.substring(0,3).toLowerCase();
        int r=-1;
        for ( int i=0; i<monthNames.length; i++ ) {
            if ( string.equals(monthNames[i]) ) r=i;
        }
        if (r==-1) return -1; else return r+1;
    }
    
    private static int y2k( String syear ) throws ParseException {
        int year= parseInt(syear);
        if ( year > 100 ) {
            return year;
        } else {
            if ( year < 70 ) {
                return 2000+year;
            } else {
                return 1900+year;
            }
        }
    }

    private static final Datum MIN_TIME= TimeUtil.createTimeDatum( 1000, 1, 1, 0, 0, 0, 0 ); // I know these are found elsewhere in the code, but I can't find it.
    private static final Datum MAX_TIME= TimeUtil.createTimeDatum( 3000, 1, 1, 0, 0, 0, 0 );

    /**
     * put limits on the typical use when looking at data:
     *   * both min and max are finite numbers
     *   * time range is between year 1000 and year 3000.
     *   * log ranges span no more than 1e100 cycles.
     * @param dr the DatumRange, which can contain infinite values that would make is inacceptable.
     * @param log if true, then only allow 1e100 cycles.
     * @return true if the range is valid.
     */
    public static boolean isAcceptable(DatumRange dr, boolean log ) {
        if ( Double.isInfinite( dr.min().doubleValue( dr.getUnits() ) ) ||
                Double.isInfinite( dr.max().doubleValue( dr.getUnits() ) ) ) {
            return false;
        } else if ( UnitsUtil.isTimeLocation( dr.getUnits() )
                && ( dr.min().lt( MIN_TIME ) || dr.max().gt( MAX_TIME ) ) ) {
            return false;
        } else if ( log ) {
            if ( !UnitsUtil.isRatioMeasurement( dr.getUnits() ) ) {
                return false;
            } else {
                return !( dr.max().divide(dr.min()).doubleValue(Units.dimensionless) > 1e100 );
            }
        }
        return true;
    }

    /**
     * represent the string to indicate that the calling code will also consider
     * the maximum value part of the datum range.  This once added the text
     * "(including...)," but this isn't terribly necessary and creates clutter on
     * some plots.
     * @param datumRange "5 to 15 Kg"
     * @return "5 to 15 Kg"
     */
    public static String toStringInclusive(DatumRange datumRange) {
        String s= datumRange.toString();
        return s;
    }
    
    public static class DateDescriptor {
        String date;
        String year;
        String month;
        String day;
        String delim;
        String format;
    }
    
    
    private static void caldat( int julday, DateDescriptor dateDescriptor ) {
        int jalpha, j1, j2, j3, j4, j5;
        
        jalpha = (int)(((double)(julday - 1867216) - 0.25)/36524.25);
        j1 = julday + 1 + jalpha - jalpha/4;
        j2 = j1 + 1524;
        j3 = 6680 + (int)(((j2-2439870)-122.1)/365.25);
        j4 = 365*j3 + j3/4;
        j5 = (int)((j2-j4)/30.6001);
        
        int day = j2 - j4 - (int)(30.6001*j5);
        int month = j5-1;
        month = ((month - 1) % 12) + 1;
        int year = j3 - 4715;
        year = year - (month > 2 ? 1 : 0);
        year = year - (year <= 0 ? 1 : 0);
        
        dateDescriptor.day= ""+day;
        dateDescriptor.month= ""+month;
        dateDescriptor.year= ""+year;
        
    }
    
    private static int julday( int month, int day, int year ) {
        int jd = 367 * year - 7 * (year + (month + 9) / 12) / 4 -
                3 * ((year + (month - 9) / 7) / 100 + 1) / 4 +
                275 * month / 9 + day + 1721029;
        return jd;
    }
    
    private static void printGroups( Matcher matcher ) {
        for ( int i=0; i<=matcher.groupCount(); i++ ) {
            System.out.println(" "+i+": "+matcher.group(i) );
        }
        System.out.println(" " );
    }
    
    private static int parseInt( String s ) throws ParseException {
        try {
            return Integer.parseInt(s);
        } catch ( NumberFormatException e ) {
            throw new ParseException( "failed attempt to parse int in \""+s+"\"", 0 );
        }
    }

    private static int getInt( String val, int deft ) {
        if ( val==null ) {
            if ( deft!=-99 ) return deft; else throw new IllegalArgumentException("bad digit");
        }
        int n= val.length()-1;
        if ( Character.isLetter( val.charAt(n) ) ) {
            return Integer.parseInt(val.substring(0,n));
        } else {
            return Integer.parseInt(val);
        }
    }

    private static double getDouble( String val, double deft ) {
        if ( val==null ) {
            if ( deft!=-99 ) return deft; else throw new IllegalArgumentException("bad digit");
        }
        int n= val.length()-1;
        if ( Character.isLetter( val.charAt(n) ) ) {
            return Double.parseDouble(val.substring(0,n));
        } else {
            return Double.parseDouble(val);
        }
    }


    private static Pattern time1, time2, time3, time4, time5, time6;
    static {
        String d= "[-:]"; // delim
        String i4= "(\\d\\d\\d\\d)";
        String i3= "(\\d+)";
        String i2= "(\\d\\d)";
        String tz= "((\\+|\\-)(\\d\\d)(:?(\\d\\d))?)"; // Note UTC allows U+2212 as well as dash.

        String iso8601time= i4 + d + i2 + d + i2 + "T" + i2 + d + i2 + "((" + d + i2 + "(\\." + i3 + ")?)?)Z?" ;  // "2012-03-27T12:22:36.786Z"
        String iso8601time2= i4 + i2 + i2 + "T" + i2 + i2 + "(" + i2 + ")?Z?" ;
        String iso8601time3= i4 + d + i3 + "T" + i2 + d + i2 + "(" + i2 + ")?Z?" ;
        String iso8601time4= i4 + d + i2 + d + i2 + "Z?" ;
        String iso8601time5= i4 + d + i3 + "Z?" ;
        String iso8601time6= i4 + d + i2 + d + i2 + "T" + i2 + d + i2 + "((" + d + i2 + "(\\." + i3 + ")?)?)"+tz+"?" ;  // "2014-09-02T10:55:10-05:00"
        time1= Pattern.compile(iso8601time);
        time2= Pattern.compile(iso8601time2);
        time3= Pattern.compile(iso8601time3);
        time4= Pattern.compile(iso8601time4);
        time5= Pattern.compile(iso8601time5);
        time6= Pattern.compile(iso8601time6);
    }
    
    /**
     * for convenience, this formats the decomposed time.
     * @param result seven-element time [ Y,m,d,H,M,S,nanos ] 
     * @return formatted time
     */
    public static String formatISO8601Datum( int[] result ) {
        return String.format( "%04d-%02d-%02dT%02d:%02d:%02d.%09dZ", result[0], result[1], result[2], result[3], result[4], result[5], result[6] );
    }
    
    /**
     * new attempt to write a clean ISO8601 parser.  This should also parse 02:00
     * in the context of 2010-002T00:00/02:00.  This does not support 2-digit years, which
     * were removed in ISO 8601:2004.
     * 
     * @param str the ISO8601 string
     * @param result the datum, decomposed into [year,month,day,hour,minute,second,nano]
     * @param lsd -1 or the current position ???
     * @return the lsd least significant digit
     */
    public static int parseISO8601Datum( String str, int[] result, int lsd ) {
        StringTokenizer st= new StringTokenizer( str, "-T:.Z+ ", true );
        Object dir= null;
        final Object DIR_FORWARD = "f";
        final Object DIR_REVERSE = "r";
        int want= 0;
        boolean haveDelim= false;
        boolean afterT= false;
        while ( st.hasMoreTokens() ) {
            char delim= ' ';
            if ( haveDelim ) {
                delim= st.nextToken().charAt(0);
                if ( delim=='T' ) afterT= true;
                if ( afterT && ( delim=='-' || delim=='+' || delim==' ' ) ) { // Time offset.  Space is because elsewhere
                    StringBuilder toff= new StringBuilder( String.valueOf(delim) );
                    while ( st.hasMoreElements() ) {
                        toff.append(st.nextToken());
                    }
                    int deltaHours= delim==' ' ? Integer.parseInt(toff.substring(1,3)) : Integer.parseInt(toff.substring(0,3));
                    switch ( toff.length() ) {
                        case 6: 
                            result[3]-= deltaHours;
                            result[4]-= Math.signum(deltaHours) * Integer.parseInt(toff.substring(4) );
                            break;
                        case 5: 
                            result[3]-= deltaHours;
                            result[4]-= Math.signum(deltaHours) * Integer.parseInt(toff.substring(3) );
                            break;
                        case 3:
                            result[3]-= deltaHours;
                            break;
                        default:
                            throw new IllegalArgumentException("malformed time zone designator: "+str);
                    }
                    normalizeTimeComponents(result);
                    break;
                }
                if ( st.hasMoreElements()==false ) { // "Z"
                    break;
                }
            } else {
                haveDelim= true;
            }
            String tok= st.nextToken();
            if ( dir==null ) {
                switch (tok.length()) {
                    case 4:
                        // typical route
                        int iyear= Integer.parseInt( tok );
                        result[0]= iyear;
                        want= 1;
                        dir=DIR_FORWARD;
                        break;
                    case 6:
                        want= lsd;
                        if ( want!=6 ) throw new IllegalArgumentException("lsd must be 6");
                        result[want]= Integer.parseInt( tok.substring(0,2) );
                        want--;
                        result[want]= Integer.parseInt( tok.substring(2,4) );
                        want--;
                        result[want]= Integer.parseInt( tok.substring(4,6) );
                        want--;
                        dir=DIR_REVERSE;
                        break;
                    case 7:
                        result[0]= Integer.parseInt( tok.substring(0,4) );
                        result[1]= 1;
                        result[2]= Integer.parseInt( tok.substring(4,7) );
                        want= 3;
                        dir=DIR_FORWARD;
                        break;
                    case 8:
                        result[0]= Integer.parseInt( tok.substring(0,4) );
                        result[1]= Integer.parseInt( tok.substring(4,6) );
                        result[2]= Integer.parseInt( tok.substring(6,8) );
                        want= 3;
                        dir=DIR_FORWARD;
                        break;
                    default:
                        dir= DIR_REVERSE;
                        want= lsd;  // we are going to have to reverse these when we're done.
                        int i= Integer.parseInt( tok );
                        result[want]= i;
                        want--;
                        break;
                }
            } else if ( dir==DIR_FORWARD) {
                if ( want==1 && tok.length()==3 ) { // $j
                    result[1]= 1;
                    result[2]= Integer.parseInt( tok ); 
                    want= 3;
                } else if ( want==3 && tok.length()==6 ) {
                    result[want]= Integer.parseInt( tok.substring(0,2) );
                    want++;
                    result[want]= Integer.parseInt( tok.substring(2,4) );
                    want++;
                    result[want]= Integer.parseInt( tok.substring(4,6) );
                    want++;
                } else if ( want==3 && tok.length()==4 ) {
                    result[want]= Integer.parseInt( tok.substring(0,2) );
                    want++;
                    result[want]= Integer.parseInt( tok.substring(2,4) );
                    want++;
                } else {
                    int i= Integer.parseInt( tok );
                    if ( delim=='.' && want==6 ) {
                        int n= 9-tok.length();
                        result[want]= i * ((int)Math.pow(10,n));
                    } else {
                        result[want]= i;
                    }
                    want++;
                }
            } else if ( dir==DIR_REVERSE ) { // what about 1200 in reverse?
                int i= Integer.parseInt( tok ); 
                if ( delim=='.' ) {
                    int n= 9-tok.length();
                    result[want]= i * ((int)Math.pow(10,n));
                } else {
                    result[want]= i;
                }
                want--;
            }
        }
        
        if ( dir==DIR_REVERSE ) {
            int iu= want+1;
            int id= lsd;
            while( iu<id ) {
                int t= result[iu];
                result[iu]= result[id];
                result[id]= t;
                iu= iu+1;
                id= id-1;
            }
        } else {
            lsd= want-1;
        }
        
        return lsd;
    }
    
    /**
     * Normalize all the components, so no component is 
     * greater than its expected range or less than zero.
     * 
     * Note that leap seconds are not accounted for.  TODO: account for them.
     * @param components int[7]: [ Y, m, d, H, M, S, nano ]
     * @return the same array
     */
    public static int[] normalizeTimeComponents( int[] components ) {
        while ( components[6]>=1000000000 ) {
            components[5]+=1;
            components[6]-= 1000000000;
        }
        while ( components[5]>=60 ) { // TODO: leap seconds
            components[4]+= 1;
            components[5]-= 60;
        }
        while ( components[5]<0 ) { // TODO: leap seconds
            components[4]-= 1;
            components[5]+= 60;
        }
        while ( components[4]>=60 ) {
            components[3]+= 1;
            components[4]-= 60;
        }
        while ( components[4]<0 ) {
            components[3]-= 1;
            components[4]+= 60;
        }
        while ( components[3]>=23 ) {
            components[2]+= 1;
            components[3]-= 24;
        }
        while ( components[3]<0 ) {
            components[2]-= 1;
            components[3]+= 24;
        }
        // Irregular month lengths make it impossible to do this nicely.  Either 
        // months should be incremented or days should be incremented, but not
        // both.  Note Day-of-Year will be normalized to Year,Month,Day here
        // as well.  e.g. 2000/13/01 because we incremented the month.
        if ( components[2]>28 ) {  
            int daysInMonth= TimeUtil.daysInMonth( components[1], components[0] );
            while ( components[2] > daysInMonth ) {
                components[2]-= daysInMonth;
                components[1]+= 1;
                if ( components[1]>12 ) break;
                daysInMonth= TimeUtil.daysInMonth( components[1], components[0] );
            }
        }
        if ( components[2]==0 ) { // handle borrow when it is no more than one day.
            components[1]=- 1;
            if ( components[1]==0 ) {
                components[1]= 12;
                components[0]-= 1;
            }
            int daysInMonth= TimeUtil.daysInMonth( components[1], components[0] );
            components[2]= daysInMonth;
        }
        while ( components[1]>12 ) {
            components[0]+= 1;
            components[1]-= 12;
        }
        if ( components[1]<0 ) { // handle borrow when it is no more than one year.
            components[0]+= 1;
            components[1]+= 12;
        }
        return components;
        
    }
    
    /**
     * Parser for ISO8601 formatted times.
     * returns null or int[7]: [ Y, m, d, H, M, S, nano ]
     * The code cannot parse any iso8601 string, but this code should.  Right now it parses:
     * "2012-03-27T12:22:36.786Z"
     * "2012-03-27T12:22:36"
     * (and some others) TODO: enumerate and test.
     * TODO: this should use parseISO8601Datum.
     * @param str iso8601 string.
     * @return null or int[7]: [ Y, m, d, H, M, S, nano ]
     */
    public static int[] parseISO8601 ( String str ) {

        Matcher m;

        m= time1.matcher(str);
        if ( m.matches() ) {
            String sf= m.group(10);
            if ( sf!=null && sf.length()>9 ) throw new IllegalArgumentException("too many digits in nanoseconds part");
            int nanos= sf==null ? 0 : ( Integer.parseInt(sf) * (int)Math.pow( 10, ( 9 - sf.length() ) ) );
            return new int[] { Integer.parseInt( m.group(1) ), Integer.parseInt( m.group(2) ), Integer.parseInt( m.group(3) ), getInt( m.group(4), 0 ), getInt( m.group(5), 0 ), getInt( m.group(8), 0), nanos };
        } else {
            m= time2.matcher(str);
            if ( m.matches() ) {
                return new int[] { Integer.parseInt( m.group(1) ), Integer.parseInt( m.group(2) ), Integer.parseInt( m.group(3) ), getInt( m.group(4), 0 ), getInt( m.group(5), 0 ), getInt( m.group(7), 0), 0 };
            } else {
                m= time3.matcher(str);
                if ( m.matches() ) {
                    return new int[] { Integer.parseInt( m.group(1) ), 1, Integer.parseInt( m.group(2) ), getInt( m.group(3), 0 ), getInt( m.group(4), 0 ), getInt( m.group(5), 0), 0 };
                } else {
                    m= time4.matcher(str);
                    if ( m.matches() ) {
                        return new int[] { Integer.parseInt( m.group(1) ), Integer.parseInt( m.group(2) ), getInt( m.group(3), 0 ), 0, 0, 0, 0 };
                    } else {
                        m= time5.matcher(str);
                        if ( m.matches() ) {
                            return new int[] { Integer.parseInt( m.group(1) ), 1, Integer.parseInt( m.group(2) ), 0, 0, 0, 0 };
                        } else {
                            m= time6.matcher(str);
                            if ( m.matches() ) {
                                String sf= m.group(10);
                                if ( sf!=null && sf.length()>9 ) throw new IllegalArgumentException("too many digits in nanoseconds part");
                                int nanos= sf==null ? 0 : ( Integer.parseInt(sf) * (int)Math.pow( 10, ( 9 - sf.length() ) ) );
                                String plusMinus= m.group(12);
                                String tzHours= m.group(13);
                                String tzMinutes= m.group(15);
                                int[] result;
                                if ( plusMinus.charAt(0)=='+') {
                                    result= new int[] { Integer.parseInt( m.group(1) ), Integer.parseInt( m.group(2) ), Integer.parseInt( m.group(3) ), 
                                        getInt( m.group(4), 0 ) - getInt( tzHours,0 ), getInt( m.group(5), 0 )- getInt( tzMinutes, 0 ),
                                        getInt( m.group(8), 0), nanos };
                                } else {
                                    result= new int[] { Integer.parseInt( m.group(1) ), Integer.parseInt( m.group(2) ), Integer.parseInt( m.group(3) ), 
                                        getInt( m.group(4), 0 ) + getInt( tzHours,0 ), getInt( m.group(5), 0 ) + getInt( tzMinutes, 0 ),
                                        getInt( m.group(8), 0) , nanos };
                                }
                                return normalizeTimeComponents(result);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static final String simpleFloat= "\\d?\\.?\\d+";
    public static final String iso8601duration= "P(\\d+Y)?(\\d+M)?(\\d+D)?(T(\\d+H)?(\\d+M)?("+simpleFloat+"S)?)?";
    public static final Pattern iso8601DurationPattern= Pattern.compile(iso8601duration);

    /**
     * returns a 7 element array with [year,mon,day,hour,min,sec,nanos].
     * @param stringIn
     * @return 7-element array with [year,mon,day,hour,min,sec,nanos]
     * @throws ParseException if the string does not appear to be valid.
     */
    public static int[] parseISO8601Duration( String stringIn ) throws ParseException {
        Matcher m= iso8601DurationPattern.matcher(stringIn);
        if ( m.matches() ) {
            double dsec=getDouble( m.group(7),0 );
            int sec= (int)dsec;
            int nanosec= (int)( ( dsec - sec ) * 1e9 );
            return new int[] { getInt( m.group(1), 0 ), getInt( m.group(2), 0 ), getInt( m.group(3), 0 ), getInt( m.group(5), 0 ), getInt( m.group(6), 0 ), sec, nanosec };
        } else {
            if ( stringIn.contains("P") && stringIn.contains("S") && !stringIn.contains("T") ) {
                throw new ParseException("ISO8601 duration expected but not found.  Was the T missing before S?",0);
            } else {
                throw new ParseException("ISO8601 duration expected but not found.",0);
            }
        }
    }
    
    /**
     * format ISO8601 duration string, for example [1,0,0,1,0,0,0] &rarr; P1YT1H
     * @param t 6 or 7-element array with [year,mon,day,hour,min,sec,nanos]
     * @return the formatted ISO8601 duration
     */
    public static String formatISO8601Duration( int[] t ) {
        StringBuilder result= new StringBuilder(24);
        result.append('P');
        if ( t[0]!=0 ) result.append(t[0]).append('Y');
        if ( t[1]!=0 ) result.append(t[1]).append('M');
        if ( t[2]!=0 ) result.append(t[2]).append('D');
        if ( t[3]!=0 || t[4]!=0 || t[5]!=0 || ( t.length==7 && t[6]!=0 ) ) {
            result.append('T');
            if ( t[3]!=0 ) result.append(t[3]).append('H');
            if ( t[4]!=0 ) result.append(t[4]).append('M');
            if ( t[5]!=0 || ( t.length==7 && t[6]!=0 ) ) {
                if ( t.length<7 || t[6]==0 ) {
                    result.append(t[5]).append('S');
                } else {
                    double sec= t[5] + t[6]/1000000000.;
                    result.append( sec ).append('S');
                }
            }
        }
        return result.toString();
    }
    
    /**
     * return true if the string is an ISO8601 duration, such as:<ul>
     * <li>P1D</li>
     * <li>PT4H</li>
     * <li>PT4H5M</li>
     * <li>PT4H5.2M</li>
     * </ul>
     * @param string
     * @return true if the string is a valid ISO8601 Duration
     */
    private static boolean isISO8601RangeDuration( String string ) {
        if ( string.length()<3 ) return false;
        if ( string.charAt(0)!='P' ) return false;
        try { 
            parseISO8601Duration(string);
            return true;
        }  catch ( ParseException ex ) {
            return false;
        }
    }
    
    /**
     * true true for parseable ISO8601 time ranges.
     * @param stringIn
     * @return 
     */
    public static boolean isISO8601Range( String stringIn ) {
        int i= stringIn.indexOf('/');
        if ( i==-1 ) return false;
        String s1= stringIn.substring(0,i);
        String s2= stringIn.substring(i+1);
        if ( TimeParser.isIso8601String( s1 ) ) {
            return TimeParser.isIso8601String(s2) || isISO8601RangeDuration(s2);
        } else if ( TimeParser.isIso8601String(s2) ) {
            return isISO8601RangeDuration(s1);
        } else {
            return false;
        }
    }
    
    /**
     * returns the time found in an iso8601 string, or null.  This supports
     * periods (durations) as in: 2007-03-01T13:00:00Z/P1Y2M10DT2H30M
     * Other examples:<ul>
     *   <li>2007-03-01T13:00:00Z/2008-05-11T15:30:00Z
     *   <li>2007-03-01T13:00:00Z/P1Y2M10DT2H30M
     *   <li>P1Y2M10DT2H30M/2008-05-11T15:30:00Z
     *   <li>2007-03-01T00:00Z/P1D
     *   <li>2012-100T02:00/03:45
     *   <li>2001-01-01T06:08-0600/P1D Time zones (-0600) are supported though discouraged.
     * </ul>
     * http://en.wikipedia.org/wiki/ISO_8601#Time_intervals
     * @param stringIn
     * @return null or a DatumRange
     * @throws java.text.ParseException
     */
    public static DatumRange parseISO8601Range( String stringIn ) throws ParseException {

        String[] parts= stringIn.split("/",-2);
        if ( parts.length!=2 ) return null;

        boolean d1= parts[0].charAt(0)=='P'; // true if it is a duration
        boolean d2= parts[1].charAt(0)=='P';

        int[] digits0;
        int[] digits1;
        int lsd= -1;

        if ( d1 ) {
            digits0= parseISO8601Duration( parts[0] );
        } else {
            digits0= new int[7];
            lsd= parseISO8601Datum( parts[0], digits0, lsd );
            for ( int j=lsd+1; j<3; j++ ) digits0[j]=1; // month 1 is first month, not 0. day 1 
        }

        if ( d2 ) {
            digits1= parseISO8601Duration(parts[1]);
        } else {
            if ( d1 ) {
                digits1= new int[7];
            } else {
                digits1= Arrays.copyOf( digits0, digits0.length );
            }
            lsd= parseISO8601Datum( parts[1], digits1, lsd );
            for ( int j=lsd+1; j<3; j++ ) digits1[j]=1; // month 1 is first month, not 0. day 1 
        }

        if ( digits0==null || digits1==null ) return null;
        
        if ( d1 ) {
            for ( int i=0; i<7; i++ ) digits0[i] = digits1[i] - digits0[i];
            if ( digits0[1]<1 ) {
                digits0[1]+=12;
                digits0[0]-=1;
            }
        }

        if ( d2 ) {
            for ( int i=0; i<7; i++ ) digits1[i] = digits0[i] + digits1[i];
            if ( digits1[1]>12 ) {
                digits1[1]-=12;
                digits1[0]+=1;
            }
        }

        Datum t1= TimeUtil.toDatum(digits0);
        Datum t2= TimeUtil.toDatum(digits1);

        return new DatumRange( t1, t2 );


    }
    
    /**
     * returns the time found in an iso8601 string, or throws a 
     * runtime exception.
     * @param stringIn
     * @return 
     */
    public static DatumRange parseValidISO8601Range( String stringIn )  {
        try {
            return parseISO8601Range(stringIn);
        } catch (ParseException ex) {
            throw new IllegalArgumentException("string was not ISO8601 range", ex );
        }
    }
    
    public static void main(String[]ss ) throws ParseException {
        System.err.println( parseTimeRange( "now-P1D/now+P1D" ) );
        System.err.println( parseTimeRange( "1972/now-P1D" ) );
        System.err.println( parseTimeRange( "now-P10D/now-P1D" ) );
        System.err.println( parseISO8601Range( "20000101T1300Z/PT1H" ) );
        //System.err.println( parseISO8601Range( "2012-100T02:00/03:45" ) );
    }
    /*;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
    //;;
    //;; papco_parse_timerange, string -> timeRange
    //;;
    //;; parses a timerange from a string.  Valid strings include:
    //    ;;  "2001"
    //    ;;  "2001-2004"
    //    ;;  "2003-004"
    //    ;;  "12/31/2001"
    //    ;;  "Jan 2001"
    //   ;;  "Jan-Feb 2004"
    //    ;;  "2004-004 - 2003-007"
    //    ;;  "JAN to MAR 2004"
    //    ;;  "2004/feb-2004/mar"
    //    ;;  "2004/004-008
    //    ;;  "1979-03-01T20:58:45.000Z	span	17.5 s"
    //    ;; keeps track of format(e.g. $Y-$j) for debugging, and perhaps to reserialize
    //    ;;
    //    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
    // if an element is trivially identifiable, as in "mar", then it is required
    // that the corresponding range element be of the same format or not specified.
     */
    
    static class TimeRangeParser {
        String token;
        String delim="";
        
        String string;
        int ipos;
        
        static final int YEAR=0;
        static final int MONTH=1;
        static final int DAY=2;
        static final int HOUR=3;
        static final int MINUTE=4;
        static final int SECOND=5;
        static final int NANO=6;
        
        String delimRegEx= "\\s|-|/|\\.|:|to|through|span|UTC|T|Z|\u2013|,";
        Pattern delimPattern= Pattern.compile( delimRegEx );
        int[] ts1= new int[] { -1, -1, -1, -1, -1, -1, -1 };
        int[] ts2= new int[] { -1, -1, -1, -1, -1, -1, -1 };
        int[] ts= null; // this is pointing to the one we are working on.
        
        boolean beforeTo;
        
        private final Pattern yyyymmddPattern= Pattern.compile("((\\d{4})(\\d{2})(\\d{2}))( |to|t|-|$)");
        
        /* groups= group numbers: { year, month, day, delim } (0 is all) */
        private boolean tryPattern( Pattern regex, String string, int[] groups, DateDescriptor dateDescriptor ) throws ParseException {
            Matcher matcher= regex.matcher( string.toLowerCase() );
            if ( matcher.find() && matcher.start()==0 ) {
                dateDescriptor.delim= matcher.group(groups[3]);
                dateDescriptor.date= string.substring( matcher.start(), matcher.end()-dateDescriptor.delim.length() );
                dateDescriptor.day= matcher.group(groups[2]);
                dateDescriptor.month= matcher.group(groups[1]);
                dateDescriptor.year= matcher.group(groups[0]);
                return true;
            } else {
                return false;
            }
        }
        
        /**
         * return true if the string can be interpretted as a date.  This takes
         * into account some old conventions, such as allowing dots to indicate
         * European-style dates ($d.$m.$Y).
         *
         * @param string
         * @param dateDescriptor
         * @return
         * @throws ParseException 
         */
        public boolean isDate( String string, DateDescriptor dateDescriptor ) throws ParseException {
            //  this is introduced because mm/dd/yy is so ambiguous, the parser
            //  has trouble with these dates.  Check for these as a group.
            
            if ( string.length()<6 ) return false;
            
            int[] groups;
            String dateDelimRegex= "( |to|t|-)";
            String yearRegex= "(\\d{2}(\\d{2})?)"; // t lower case because tryPattern folds case
            
            if ( tryPattern( yyyymmddPattern, string, new int[] { 2,3,4,5 }, dateDescriptor ) ) {
                dateDescriptor.format= "$Y" + dateDescriptor.delim + "$m" + dateDescriptor.delim + "$d";
                return true;
            }
            
            String delim1;
            
            String delims="(/|\\.|-| )";
            Matcher matcher= Pattern.compile(delims).matcher(string);
            
            if ( matcher.find() ) {
                delim1= string.substring(matcher.start(),matcher.end());
            } else {
                return false;
            }
            
            if ( delim1.equals(".") ) {
                delim1="\\.";
            }
            
            String monthNameRegex= "(jan[a-z]*|feb[a-z]*|mar[a-z]*|apr[a-z]*|may|june?|july?|aug[a-z]*|sep[a-z]*|oct[a-z]*|nov[a-z]*|dec[a-z]*)";
            String monthRegex= "((\\d?\\d)|"+monthNameRegex+")";
            String dayRegex= "(\\d?\\d)";
            
            String euroDateRegex;
            
            if ( delim1.equals("\\.") ) {
                euroDateRegex= "(" + dayRegex + delim1 + monthRegex + delim1 + yearRegex + dateDelimRegex + ")";
                groups= new int [] { 6, 3, 2, 8  };
            } else {
                euroDateRegex= "(" + dayRegex + delim1 + monthNameRegex + delim1 + yearRegex + dateDelimRegex + ")";
                groups= new int [] { 4, 3, 2, 6 };
            }
            if ( tryPattern( Pattern.compile( euroDateRegex ), string, groups, dateDescriptor ) ) {
                dateDescriptor.format= "$d"+delim1+"$m"+delim1+"$Y";
                return true;
            }
            
            String usaDateRegex= monthRegex + delim1 + dayRegex + delim1 + yearRegex + dateDelimRegex ;
            if ( tryPattern( Pattern.compile( usaDateRegex ), string, new int[] { 5,1,4,7 }, dateDescriptor ) ) {
                dateDescriptor.format= "$m"+delim1+"$d"+delim1+"$Y";
                return true;
            }
            
            // only works for four-digit years
            String lastDateRegex= "(\\d{4})" + delim1 + monthRegex + delim1 + dayRegex + dateDelimRegex;
            if ( tryPattern( Pattern.compile( lastDateRegex ), string, new int[] { 1,2,5,6 }, dateDescriptor ) ) {
                dateDescriptor.format= "$Y"+delim1+"$m"+delim1+"$d";
                return true;
            }
            
            String doyRegex= "(\\d{3})";
            String dateRegex= doyRegex+"(-|/)" + yearRegex + dateDelimRegex;
            
            if ( tryPattern( Pattern.compile( dateRegex ), string, new int[] { 3,1,1,5 }, dateDescriptor ) ) {
                int doy= parseInt(dateDescriptor.day);
                if ( doy>366 ) return false;
                int year= parseInt(dateDescriptor.year);
                caldat( julday( 12, 31, year-1 ) + doy, dateDescriptor );
                dateDescriptor.format= "$j"+delim1+"$Y";
                return true;
            }
            
            dateRegex= yearRegex +"(-|/)" + doyRegex + dateDelimRegex;
            if ( tryPattern( Pattern.compile( dateRegex ), string, new int[] { 1,4,4,5 }, dateDescriptor ) ) {
                int doy= parseInt(dateDescriptor.day);
                if ( doy>366 ) return false;
                int year= parseInt(dateDescriptor.year);
                caldat( julday( 12, 31, year-1 ) + doy, dateDescriptor );
                dateDescriptor.format= "$Y"+delim1+"$j";
                return true;
            }
            return false;
        }
        
        
        private void nextToken( ) {
            Matcher matcher= delimPattern.matcher( string.substring(ipos) );
            if ( matcher.find() ) {
                int r= matcher.start();
                int length= matcher.end()-matcher.start();
                token= string.substring( ipos, ipos+r );
                delim= string.substring( ipos+r, ipos+r+length );
                if ( delim.equals("to") && token.length()==2 && Character.toLowerCase(token.charAt(0))=='o' && Character.toLowerCase(token.charAt(1))=='c' ) {
                    token= token + delim;
                    ipos+=r + length;
                    matcher= delimPattern.matcher( string.substring(ipos) );
                    if ( matcher.find() ) {
                        r= matcher.start();
                        length= matcher.end()-matcher.start();
                        token= token+ string.substring(ipos,ipos+r);
                        delim= string.substring( ipos+r, ipos+r+length );
                    }
                }
                ipos= ipos + r + length;
            } else {
                token= string.substring(ipos);
                delim= "";
                ipos= string.length();
            }
        }
        
        private void setBeforeTo( boolean v ) {
            beforeTo= v;
            if ( beforeTo ) {
                ts= ts1;
            } else {
                ts= ts2;
            }
        }
        
        /* identify and make the "to" delimiter unambiguous */
        private String normalizeTo( String s ) throws ParseException {
            
            int minusCount= 0;
            for ( int i=0; i<s.length(); i++ ) if ( s.charAt(i)=='-' ) minusCount++;
            if ( minusCount==0 ) return s;
            
            DateDescriptor dateDescriptor= new DateDescriptor();
            ipos=0;
            
            StringBuilder newString= new StringBuilder();
            while ( ipos<s.length() ) {
                if ( isDate( s.substring( ipos ), dateDescriptor ) ) {
                    ipos= ipos+dateDescriptor.date.length()+dateDescriptor.delim.length();
                    token= dateDescriptor.date;
                    delim= dateDescriptor.delim;
                } else {
                    nextToken();
                }
                newString.append(token);
                if ( delim.equals("-") ) {
                    newString.append("to");
                } else {
                    newString.append(delim);
                }
            }
            String result= newString.toString();
            
            String[] ss= result.split("to"); //findbugs SBSC_USE_STRINGBUFFER_CONCATENATION okay
            
            if ( ss.length>2 ) {
                StringBuilder sb= new StringBuilder(ss[0]);
                for ( int i=1; i<ss.length; i++ ) {
                    sb.append("-").append(ss[i]);
                }
                result= sb.toString();
            } else if ( ss.length==2 ) { // kludgy check for YYYY to YYYY, everything else is error
                String s0= ss[0].trim();
                String s1= ss[1].trim();
                if ( !isYear(s0) || !isYear(s1) ) {
                    result= s0 + " " + s1;
                }
            }
            return result;
        }
        
        /**
         * Read the time range from the string.  This looks for 
         * ISO8601 time ranges first, then tries more colloquial methods
         * for representing the strings.
         * Example inputs:<ul>
         * <li>2014-05-06T00:00/24:00    (an ISO8601 range)
         * <li>2014-05-06 00:00 to 24:00
         * <li>2014-05-06 
         * </ul>
         * For examples, see http://jfaden.net:8080/hudson/job/autoplot-test026/lastSuccessfulBuild/artifact/Test026.java
         * @param stringIn
         * @return the parsed DatumRange 
         * @throws ParseException when a time range cannot be inferred.
         */
        public DatumRange parse( String stringIn ) throws ParseException {
            
            DatumRange check= parseISO8601Range( stringIn );
            if ( check!=null ) return check;
            if ( stringIn.contains("/PT") ) {  // 2013-002/PT1D gave confusing error.
                throw new ParseException( "appears to be malformed ISO8601 string: "+stringIn, 0 );
            }
            
            this.string= stringIn+" ";
            this.ipos= 0;

            if ( stringIn.length()==0 ) {
                throw new ParseException("Unable to parse timerange: empty string",0);
            }
            ArrayList beforeToUnresolved= new ArrayList();
            ArrayList afterToUnresolved= new ArrayList();
            
            String[] formatCodes= new String[] { "$y", "$m", "$d", "$H", "$M", "$S", "$N" };
            String[] digitIdentifiers= new String[] {"YEAR", "MONTH", "DAY", "HOUR", "MINUTE", "SECOND", "NANO" };
            
            boolean isThroughNotTo= false;
            
            final int YEAR=0;
            final int MONTH=1;
            final int DAY=2;
            final int HOUR=3;
            final int MINUTE=4;
            final int SECOND=5;
            final int NANO=6;
            
            final int STATE_OPEN=89;
            final int STATE_TS1TIME=90;
            final int STATE_TS2TIME=91;
            
            int state= STATE_OPEN;
            
            String format="";
            setBeforeTo( true );   // true if before the "to" delineator
            
            DateDescriptor dateDescriptor= new DateDescriptor();
            
            String newString= normalizeTo(string);  // TODO: normalizeTo needs to be studied for "2001-01-01T06:08+to+10:08"
            
            if ( newString.endsWith("UTC") ) { // 2006-01-05T07:16:45 to 2006-01-05T07:17:15 UTC
                newString= newString.substring(0,newString.length()-3);
            }
            
            string= newString;
            
            ipos=0;
            while ( ipos < string.length() ) {
                String lastdelim= delim;
                format= format+lastdelim;
                
                if ( lastdelim.equals("to") ) setBeforeTo( false );
                if ( lastdelim.equals("through") ) {
                    setBeforeTo( false );
                    isThroughNotTo= true;
                }
                if ( lastdelim.equals("\u2013") )  setBeforeTo( false ); // hyphen
                if ( lastdelim.equals("span") ) {
                    setBeforeTo(false);
                    if ( ts1[DAY]>-1 ) {
                        for ( int ii=HOUR; ii<=NANO; ii++ ) if (ts1[ii]==-1) ts1[ii]=0;
                    } else {
                        throw new IllegalArgumentException("span needs start date to have day specified");
                    }
                    Datum start= TimeUtil.toDatum(ts1);
                    String swidth= newString.substring(ipos);
                    swidth= makeCanonicalTimeWidthString(swidth);
                    Datum width= DatumUtil.parse( swidth );
                    Datum stop= start.add(width); //TODO: rounding errors.

                    TimeUtil.TimeStruct tt= TimeUtil.toTimeStruct(stop);
                    ts2[DAY]= tt.day;
                    ts2[MONTH]= tt.month;
                    ts2[YEAR]= tt.year;
                    ts2[HOUR]= tt.hour;
                    ts2[MINUTE]= tt.minute;
                    ts2[SECOND]= (int)tt.seconds;
                    ts2[NANO]= (int)(( tt.seconds - ts2[SECOND] ) * 1e9) + tt.millis*1000000 + tt.micros*1000;
                    ipos= newString.length();
                }
                
                if ( isDate( string.substring( ipos ), dateDescriptor ) ) {
                    format= format+dateDescriptor.format;
                    if ( ts1[DAY] != -1 || ts1[MONTH] != -1 || ts1[YEAR] != -1 ) { setBeforeTo( false ); }
                    
                    int month= monthNumber(dateDescriptor.month);
                    int year= y2k(dateDescriptor.year);
                    int day= parseInt(dateDescriptor.day);
                    ts[DAY]= day;
                    ts[MONTH]= month;
                    ts[YEAR]= year;
                    delim=dateDescriptor.delim;
                    ipos= ipos+dateDescriptor.date.length() + dateDescriptor.delim.length();
                    
                } else {
                    
                    nextToken();
                    
                    if ( token.equals("") ) continue;
                    
                    if ( isYear(token) ) {
                        format= format+"$Y";
                        if ( ts1[YEAR] == -1 && beforeTo ) {
                            ts1[YEAR]= parseInt(token);
                            ts2[YEAR]= ts1[YEAR];
                        } else {
                            setBeforeTo( false );
                            ts2[YEAR]= parseInt(token);
                            if ( ts1[YEAR]==-1 ) ts1[YEAR]=ts2[YEAR];
                        }
                    } else if ( isDayOfYear(token) ) {
                        format= format+"$j";
                        if ( ts1[YEAR] == -1 ) {
                            throw new ParseException( "day of year before year: "+stringIn+" ("+format+")", ipos );
                        }
                        int doy= parseInt(token);
                        caldat( julday( 12, 31, ts1[YEAR]-1 ) + doy, dateDescriptor );
                        int day= parseInt(dateDescriptor.day);
                        int month= parseInt(dateDescriptor.month);
                        if ( ts1[DAY] == -1 && beforeTo) {
                            ts1[DAY]= day;
                            ts1[MONTH]= month;
                            ts2[DAY]= day;
                            ts2[MONTH]= month;
                        } else {
                            setBeforeTo( false );
                            ts2[DAY]= day;
                            ts2[MONTH]= month;
                            if ( ts1[DAY] == -1 ) {
                                ts1[DAY]= day;
                                ts1[MONTH]= month;
                            }
                        }
                    } else if ( monthNameNumber( token )!=-1 ) {
                        format= format+"$b";
                        int month= monthNameNumber( token );
                        if ( ts1[MONTH] == -1 ) {
                            ts1[MONTH]= month;
                            ts2[MONTH]= month;
                        } else {
                            setBeforeTo( false );
                            ts2[MONTH]= month;
                            if ( ts1[MONTH]==-1 ) ts1[MONTH]= month;
                        }
                    } else {
                        boolean isWithinTime= delim.equals(":") || lastdelim.equals(":");
                        if ( isWithinTime ) { // TODO: can we get rid of state?
                            if ( delim.equals(":") && !lastdelim.equals(":") && state == STATE_OPEN ) {
                                format= format+"$H";
                                if ( beforeTo && ts1[HOUR] == -1 ) {
                                    state= STATE_TS1TIME;
                                } else {
                                    setBeforeTo( false );
                                    state= STATE_TS2TIME;
                                }
                                ts[HOUR]= parseInt(token);
                            } else {
                                int i= HOUR;
                                while ( i<=NANO ) {
                                    if ( ts[i] == -1 ) break;
                                    i=i+1;
                                }
                                if ( i == SECOND && delim.equals(".") ) {
                                    ts[i]= parseInt(token);
                                    format= format+formatCodes[i];
                                    nextToken();
                                    i=NANO;
                                }
                                if ( i == NANO ) {
                                    int tokenDigits= token.length();
                                    ts[i]= parseInt(token) * (int)Math.pow( 10, (9-tokenDigits) );
                                    switch (tokenDigits) {
                                        case 3: format= format+"$(subsec,places=3)"; break;
                                        case 6: format= format+"$(subsec,places=6)"; break;
                                        default: format=format+"$N"; break;
                                        
                                    }
                                } else {
                                    ts[i]= parseInt(token);
                                    format= format+formatCodes[i];
                                }
                            }
                            
                            if ( !delim.equals(":") && !delim.equals(".") ) {
                                state= STATE_OPEN;
                            }
                        } else { // parsing date
                            if ( beforeTo ) {
                                beforeToUnresolved.add( token );
                                format= format+"UNRSV1"+beforeToUnresolved.size();
                            } else {
                                afterToUnresolved.add( token );
                                format= format+"UNRSV2"+afterToUnresolved.size();
                            }
                            
                        }
                    }
                }
            }
            
            /* go through the start and end time, resolving all the symbols
             * which are marked as unresolvable during the first pass.
             */
            format= format+" ";
            
            {
                StringBuilder stringBuffer= new StringBuilder("ts1: ");
                for ( int i=0; i<7; i++ ) stringBuffer.append("").append(ts1[i]).append(" ");
                logger.fine( stringBuffer.toString() );
                stringBuffer= new StringBuilder("ts2: ");
                for ( int i=0; i<7; i++ ) stringBuffer.append("").append(ts2[i]).append(" ");
                logger.fine( stringBuffer.toString() );
                logger.fine( format );
            }
            
            if ( beforeTo ) {
                int idx=0;
                for ( int i=0; i<beforeToUnresolved.size(); i++ ) {
                    while( idx<7 && ts1[idx]!=-1 ) idx++;
                    if ( idx==7 ) throw new ParseException( "can't resolve token in \""+stringIn+"\": "+beforeToUnresolved.get(i)+ " ("+format+")", 0 );
                    ts1[idx]= parseInt((String)beforeToUnresolved.get(i));
                    String[] s= format.split("UNRSV1"+(i+1),-2);
                    format= s[0]+formatCodes[idx]+s[1];
                }
                beforeToUnresolved.clear();
            }
            
            if ( beforeToUnresolved.size()+afterToUnresolved.size() > 0 ) {
                ArrayList unload;
                String formatUn;
                int idx=0;
                
                if ( beforeToUnresolved.size() < afterToUnresolved.size() ) {
                    if ( beforeToUnresolved.size()>0 ) {
                        for ( int i=0; i<afterToUnresolved.size(); i++ ) {
                            while( idx<7 && ts2[idx]!=-1 ) idx++;
                            if ( idx==7 ) throw new ParseException( "can't resolve token in \""+stringIn+"\": "+afterToUnresolved.get(i)+ " ("+format+")", 0 );
                            ts2[idx]= parseInt((String)afterToUnresolved.get(i));
                            String[] s= format.split("UNRSV2"+(i+1));
                            format= s[0]+formatCodes[idx]+s[1];
                        }
                        unload= beforeToUnresolved;
                        formatUn= "UNRSV1";
                        ts= ts1;
                    } else {
                        while( idx<7 && ts1[idx]!=-1 ) idx++;
                        if ( idx>0 ) idx--;
                        unload= afterToUnresolved;
                        formatUn= "UNRSV2";
                        ts= ts2;
                    }
                } else {
                    if ( afterToUnresolved.size()>0 ) {
                        for ( int i=0; i<beforeToUnresolved.size(); i++ ) {
                            while( idx<7 && ts1[idx]!=-1 ) idx++;
                            if ( idx==7 ) throw new ParseException( "can't resolve token in \""+stringIn+"\": "+beforeToUnresolved.get(i)+ " ("+format+")", 0 );
                            ts1[idx]= parseInt((String)beforeToUnresolved.get(i));
                            if ( idx==0 && ts[idx]<1000 ) {
                                throw new ParseException("Years must be four digit years: "+(String)beforeToUnresolved.get(i),0);
                            }
                            String[] s= format.split("UNRSV1"+(i+1));
                            format= s[0]+formatCodes[idx]+s[1];
                        }
                        unload= afterToUnresolved;
                        formatUn= "UNRSV2";
                        ts= ts2;
                    } else {
                        while( idx<7 && ts2[idx]!=-1) idx++;
                        if ( idx>0 ) idx--;
                        unload= beforeToUnresolved;
                        formatUn= "UNRSV1";
                        ts= ts1;
                    }
                }
                int lsd=idx;
                for ( int i=unload.size()-1; i>=0; i-- ) {
                    while ( ts[lsd]!=-1 && lsd>0 ) lsd--;
                    if ( ts[lsd]!=-1 ) {
                        throw new ParseException( "can't resolve these tokens in \""+stringIn+"\": "+unload+ " ("+format+")", 0 );
                    }
                    ts[lsd]= parseInt((String)unload.get(i));
                    String[] s= format.split(formatUn+(i+1));
                    format= s[0]+formatCodes[lsd]+s[1];
                }
                
            } // unresolved entities
            
            {
                StringBuilder stringBuffer= new StringBuilder("ts1: ");
                for ( int i=0; i<7; i++ ) stringBuffer.append("").append(ts1[i]).append(" ");
                logger.fine( stringBuffer.toString() );
                stringBuffer= new StringBuilder("ts2: ");
                for ( int i=0; i<7; i++ ) stringBuffer.append("").append(ts2[i]).append(" ");
                logger.fine( stringBuffer.toString() );
                logger.fine( format );
            }
            
            /* contextual fill.  Copy over digits that were specified in one time but
             * not the other.
             */
            for ( int i=YEAR; i<=DAY; i++ ) {
                if ( ts2[i] == -1 && ts1[i] != -1 ) ts2[i]= ts1[i];
                if ( ts1[i] == -1 && ts2[i] != -1 ) ts1[i]= ts2[i];
            }
            
            int i= NANO;
            int[] implicit_timearr= new int[] { -1, 1, 1, 0, 0, 0, 0 };
            int ts1lsd= -1;
            int ts2lsd= -1;
            while (i>=0) {
                if ( ts2[i] != -1 && ts2lsd == -1 ) ts2lsd=i;
                if ( ts2lsd == -1 ) ts2[i]= implicit_timearr[i];
                if ( ts2[i] == -1 && ts2lsd != -1 ) {
                    throw new ParseException("not specified in stop time: "+digitIdentifiers[i]+" in "+stringIn+" ("+format+")",ipos);
                }
                if ( ts1[i] != -1 && ts1lsd == -1 ) ts1lsd=i;
                if ( ts1lsd == -1 ) ts1[i]= implicit_timearr[i];
                if ( ts1[i] == -1 && ts1lsd != -1 ) {
                    throw new ParseException("not specified in start time:"+digitIdentifiers[i]+" in "+stringIn+" ("+format+")",ipos);
                }
                i= i-1;
            }
            
            if ( ts1lsd!=ts2lsd && beforeTo ) {
                throw new ParseException( "expected to find \"to\" when hours are specified", 0 );
            }
            
            if ( ts1lsd != ts2lsd && ( ts1lsd<HOUR || ts2lsd<HOUR ) ) {
                throw new ParseException( "resolution mismatch: "+digitIdentifiers[ts1lsd]+" specified for start, but "
                        + digitIdentifiers[ts2lsd]+" specified for end, must be same" + " in \""+stringIn+"\""+ " ("+format+")", ipos );
            }
            
            if ( beforeTo ) isThroughNotTo= true;
            
            if ( isThroughNotTo ) {
                if ( ts2lsd<0 ) {
                    throw new ParseException("cannot interpret as time",ipos);
                }
                ts2[ts2lsd]++;
            }
            
            if ( ts1[0]<1900 ) ts1[0]= y2k(""+ts1[0]);
            if ( ts2[0]<1900 ) ts2[0]= y2k(""+ts2[0]);
            if ( ts1[0]>9000 ) {
                throw new ParseException("Year cannot be greater than 9000: "+ts1[0], 0 );
            }
            if ( ts2[0]>9001 ) { // little slop needed for http://www.sarahandjeremy.net:8080/hudson/job/autoplot-test026/
                throw new ParseException("Year cannot be greater than 9000: "+ts2[0], 0 );
            }
            
            if ( ts1lsd < DAY ) {
                try {
                    return new MonthDatumRange( ts1, ts2 );
                } catch ( IllegalArgumentException e ) {
                    ParseException eNew= new ParseException( "fails to parse due to MonthDatumRange: "+stringIn+ " ("+format+") " + e.getMessage(), 0 );
                    eNew.initCause(e);
                    throw eNew;
                }
            } else {
                Datum time1= TimeUtil.createTimeDatum( ts1[0], ts1[1], ts1[2], ts1[3], ts1[4], ts1[5], ts1[6] );
                Datum time2= TimeUtil.createTimeDatum( ts2[0], ts2[1], ts2[2], ts2[3], ts2[4], ts2[5], ts2[6] );
                if ( time1.le(time2) ) {
                    return new DatumRange( time1, time2 );
                } else {
                    throw new ParseException( String.format( "First time is after second time: %s after %s", time1, time2 ), 0 );
                }
            }
        }

        private String makeCanonicalTimeWidthString(String swidth) {
            if ( swidth.contains("day") && !swidth.contains("days") ){
                swidth= swidth.replaceAll("day ", "days");
            }
            swidth= swidth.replaceAll("hrs", "hr");
            swidth= swidth.replaceAll("mins", "min");
            swidth= swidth.replaceAll("secs", "s");
            swidth= swidth.replaceAll("sec", "s");
            swidth= swidth.replaceAll("msec", "ms");
            return swidth;
        }
        
    }
    
    /**
     * parse the string into a DatumRange with time location units.
     * This parse allows several different forms of time ranges, such as:
     * <ul>
     * <li> orbit:rbspa-pp:3  S/C orbits
     * <li> orbit:rbspa-pp:3-6  S/C orbits, three orbits.
     * <li> P10D  the last 10 days, immediately resolved into static time range.
     * <li> 1972/now-P10D, immediately resolved into static time range.
     * <li> 1972/2002  ISO8601 time ranges
     * <li> 1972 to 2002  legacy das2 colloquial time ranges.
     * <li> lasthour/lasthour+P1H  the hour we are within. 
     * </ul>
     * @param string
     * @return the range interpreted.
     * @throws ParseException when the string cannot be parsed.
     * @throws IllegalArgumentException when an orbits file cannot be read
     */
    public static DatumRange parseTimeRange( String string ) throws ParseException {
        if ( string.startsWith("orbit:") ) { // experiment with orbits  orbit:crres:591
            String[] ss= string.split(":"); // support orbit:http://das2.org/wiki/index.php/Orbits/crres:5
            if ( ss.length==4 ) {
                ss[1]= ss[1]+":"+ss[2];
                ss[2]= ss[3];
            }
            if ( ss.length<3 ) {
                throw new ParseException("orbit misformatted, should be orbit:<sc>:<num> or orbit:<sc>:<num>-<num>",0);
            }
            Pattern p= Pattern.compile("(\\d+)\\-(\\d+)");
            Matcher m= p.matcher(ss[2]);
            if ( m.matches() ) { // This is a stupid feature which caused confusion with Ivar's "mms-ev1_01"
                DatumRange o0= new OrbitDatumRange( ss[1], m.group(1) );
                DatumRange o1= new OrbitDatumRange( ss[1], m.group(2) );
                return DatumRangeUtil.union( o0,o1 );
            } else {
                return new OrbitDatumRange( ss[1], ss[2] );
            }
            
        } else if ( string.startsWith("P") && iso8601DurationPattern.matcher(string).matches() ) { // just for experiment.  See ISO8601
            Datum now= TimeUtil.now();
            DatumRange result= parseISO8601Range( string + "/" + TimeParser.create(TimeParser.TIMEFORMAT_Z).format(now, null) );
            if ( result==null ) throw new ParseException(string,0);
            return result;
        } else {
            if ( string.contains("now") || 
                    ( string.contains("last") && 
                        ( string.contains("lastminute" ) 
                        || string.contains("lasthour") 
                        || string.contains("lastday") 
                        || string.contains("lastmonth") 
                        || string.contains("lastyear") ) 
                    ) 
                ) {
                String[] ss= string.split("/");
                String delim="/";
                if ( ss.length!=2 ) {
                    logger.fine("expected to find '/' along with now");
                }
                StringBuilder snew= new StringBuilder();
                Datum time;
                Datum now= TimeUtil.now();
                for ( int iss=0; iss<ss.length; iss++ ) {
                    String s= ss[iss];
                    if ( iss>0 ) snew.append( delim );
                    if ( s.startsWith("now-") ) {  // now-PT10D
                        int[] dt= parseISO8601Duration(s.substring(4));
                        int[] tt= TimeUtil.fromDatum( now );
                        for ( int i=0; i<tt.length; i++ ) tt[i]= tt[i] - dt[i];
                        time= TimeUtil.toDatum(tt);
                        snew.append( TimeParser.create(TimeParser.TIMEFORMAT_Z).format(time,null) );
                    } else if ( s.startsWith("now+") || s.startsWith("now ") ) { // The plus now+P1D is often mistaken for a space.
                        int[] dt= parseISO8601Duration(s.substring(4));
                        int[] tt= TimeUtil.fromDatum( now );
                        for ( int i=0; i<tt.length; i++ ) tt[i]= tt[i] + dt[i];
                        time= TimeUtil.toDatum(tt);
                        snew.append( TimeParser.create(TimeParser.TIMEFORMAT_Z).format(time,null) );               
                    } else if ( s.equals("now")) {
                        snew.append( TimeParser.create(TimeParser.TIMEFORMAT_Z).format(now,null) );
                    } else if ( s.contains("now") ){
                        String nowString= TimeParser.create(TimeParser.TIMEFORMAT_Z).format(now, null);
                        snew.append( s.replace("now",nowString) );
                    } else if ( s.contains("lastyear-") ) {
                        int[] tt= TimeUtil.fromDatum(now);
                        tt[1]=1;
                        tt[2]=1;
                        tt[3]=0;
                        tt[4]=0;
                        tt[5]=0;
                        tt[6]=0;
                        int[] dt= parseISO8601Duration(s.substring(8));
                        for ( int i=0; i<tt.length; i++ ) tt[i]= tt[i] - dt[i];
                        time= TimeUtil.toDatum(tt);
                        snew.append( TimeParser.create(TimeParser.TIMEFORMAT_Z).format(time,null) );
                    } else if ( s.contains("lastyear") ) {
                        int[] d= TimeUtil.fromDatum(now);
                        d[1]=1;
                        d[2]=1;
                        d[3]=0;
                        d[4]=0;
                        d[5]=0;
                        d[6]=0;
                        Datum lastDay= TimeUtil.toDatum(d);
                        String lastDayString= TimeParser.create(TimeParser.TIMEFORMAT_Z).format(lastDay, null);
                        snew.append( s.replace("lastyear",lastDayString) );
                    } else if ( s.contains("lastmonth-") ) {
                        int[] tt= TimeUtil.fromDatum(now);
                        tt[2]=1;
                        tt[3]=0;
                        tt[4]=0;
                        tt[5]=0;
                        tt[6]=0;
                        int[] dt= parseISO8601Duration(s.substring(8));
                        for ( int i=0; i<tt.length; i++ ) tt[i]= tt[i] - dt[i];
                        time= TimeUtil.toDatum(tt);
                        snew.append( TimeParser.create(TimeParser.TIMEFORMAT_Z).format(time,null) );
                    } else if ( s.contains("lastmonth+") ) {
                        int[] tt= TimeUtil.fromDatum(now);
                        tt[2]=1;
                        tt[3]=0;
                        tt[4]=0;
                        tt[5]=0;
                        tt[6]=0;
                        int[] dt= parseISO8601Duration(s.substring(8));
                        for ( int i=0; i<tt.length; i++ ) tt[i]= tt[i] + dt[i];
                        time= TimeUtil.toDatum(tt);
                        snew.append( TimeParser.create(TimeParser.TIMEFORMAT_Z).format(time,null) );
                    } else if ( s.contains("lastmonth") ) {
                        int[] d= TimeUtil.fromDatum(now);
                        d[2]=1;
                        d[3]=0;
                        d[4]=0;
                        d[5]=0;
                        d[6]=0;
                        Datum lastDay= TimeUtil.toDatum(d);
                        String lastDayString= TimeParser.create(TimeParser.TIMEFORMAT_Z).format(lastDay, null);
                        snew.append( s.replace("lastmonth",lastDayString) );
                    } else if ( s.contains("lastday-") ) {
                        int[] tt= TimeUtil.fromDatum(now);
                        tt[3]=0;
                        tt[4]=0;
                        tt[5]=0;
                        tt[6]=0;
                        int[] dt= parseISO8601Duration(s.substring(8));
                        for ( int i=0; i<tt.length; i++ ) tt[i]= tt[i] - dt[i];
                        time= TimeUtil.toDatum(tt);
                        snew.append( TimeParser.create(TimeParser.TIMEFORMAT_Z).format(time,null) );
                    } else if ( s.contains("lastday+") ) {
                        int[] tt= TimeUtil.fromDatum(now);
                        tt[3]=0;
                        tt[4]=0;
                        tt[5]=0;
                        tt[6]=0;
                        int[] dt= parseISO8601Duration(s.substring(8));
                        for ( int i=0; i<tt.length; i++ ) tt[i]= tt[i] + dt[i];
                        time= TimeUtil.toDatum(tt);
                        snew.append( TimeParser.create(TimeParser.TIMEFORMAT_Z).format(time,null) );
                    } else if ( s.contains("lastday") ) {
                        int[] d= TimeUtil.fromDatum(now);
                        d[3]=0;
                        d[4]=0;
                        d[5]=0;
                        d[6]=0;
                        Datum lastDay= TimeUtil.toDatum(d);
                        String lastDayString= TimeParser.create(TimeParser.TIMEFORMAT_Z).format(lastDay, null);
                        snew.append( s.replace("lastday",lastDayString) );
                    } else if ( s.contains("lasthour-") ) {
                        int[] tt= TimeUtil.fromDatum(now);
                        tt[4]=0;
                        tt[5]=0;
                        tt[6]=0;
                        int[] dt= parseISO8601Duration(s.substring(8));
                        for ( int i=0; i<tt.length; i++ ) tt[i]= tt[i] - dt[i];
                        time= TimeUtil.toDatum(tt);
                        snew.append( TimeParser.create(TimeParser.TIMEFORMAT_Z).format(time,null) );
                    } else if ( s.contains("lasthour+") ) {
                        int[] tt= TimeUtil.fromDatum(now);
                        tt[4]=0;
                        tt[5]=0;
                        tt[6]=0;
                        int[] dt= parseISO8601Duration(s.substring(8));
                        for ( int i=0; i<tt.length; i++ ) tt[i]= tt[i] + dt[i];
                        time= TimeUtil.toDatum(tt);
                        snew.append( TimeParser.create(TimeParser.TIMEFORMAT_Z).format(time,null) );
                    } else if ( s.contains("lasthour") ) {
                        int[] d= TimeUtil.fromDatum(now);
                        d[4]=0;
                        d[5]=0;
                        d[6]=0;
                        Datum lastDay= TimeUtil.toDatum(d);
                        String lastDayString= TimeParser.create(TimeParser.TIMEFORMAT_Z).format(lastDay, null);
                        snew.append( s.replace("lasthour",lastDayString) );
                    } else if ( s.contains("lastminute-") ) { 
                        int[] tt= TimeUtil.fromDatum(now);
                        tt[5]=0;
                        tt[6]=0;
                        int[] dt= parseISO8601Duration(s.substring(11));
                        for ( int i=0; i<tt.length; i++ ) tt[i]= tt[i] - dt[i];
                        time= TimeUtil.toDatum(tt);
                        snew.append( TimeParser.create(TimeParser.TIMEFORMAT_Z).format(time,null) );
                    } else if ( s.contains("lastminute+") ) {
                        int[] tt= TimeUtil.fromDatum(now);
                        tt[5]=0;
                        tt[6]=0;
                        int[] dt= parseISO8601Duration(s.substring(11));
                        for ( int i=0; i<tt.length; i++ ) tt[i]= tt[i] + dt[i];
                        time= TimeUtil.toDatum(tt);
                        snew.append( TimeParser.create(TimeParser.TIMEFORMAT_Z).format(time,null) );
                    } else if ( s.contains("lastminute") ) {
                        int[] d= TimeUtil.fromDatum(now);
                        d[5]=0;
                        d[6]=0;
                        Datum lastDay= TimeUtil.toDatum(d);
                        String lastDayString= TimeParser.create(TimeParser.TIMEFORMAT_Z).format(lastDay, null);
                        snew.append( s.replace("lastminute",lastDayString) );
                    } else {
                        snew.append(s);
                    }
                }
                string= snew.toString();
            }
            string= string.replaceAll("\\+"," "); // Allow Jan+2014.  Note this may mess up ISO8601 time zone.
            return new TimeRangeParser().parse(string);
        }
    }
    

    /**
     * parse the string into a DatumRange with time location units.
     * @throws IllegalArgumentException when the string cannot be parsed.
     * @param s
     * @return
     */
    public static DatumRange parseTimeRangeValid( String s ) {
        try {
            return parseTimeRange(s);
        } catch ( ParseException e ) {
            throw new IllegalArgumentException(e);
        }
    }
    
    /* formats time, supressing trailing zeros.  Time2 is another time that will be displayed alongside time,
     * and may be used when deciding how the time should be formatted.  context is used to describe an external
     * time context that can further make the display of the time more efficient.
     */
    private static String efficientTime( Datum time, Datum time2, DatumRange context ) {
        TimeUtil.TimeStruct ts= TimeUtil.toTimeStruct(time);
        
        String timeString;
        
        int stopRes= 4;
        if ( TimeUtil.getSecondsSinceMidnight(time)==0. && time.equals(context.max()) ) {
            ts.hour=24;
            ts.day--;
        }
        
        timeString= ""+ts.hour+":";
        
        Datum[] times= new Datum[] { time, time2 };
        for (Datum t : times) {
            int[] arr = TimeUtil.toTimeArray(t);
            int idigit;
            for ( idigit=7; idigit>3; idigit-- ) {
                if ( arr[idigit]>0 ) break;
            }
            stopRes= Math.max( stopRes, idigit );
        }

        if ( Math.abs( time2.subtract(time).doubleValue( Units.seconds ) ) > 3600 ) {
            stopRes= Math.min( stopRes, 4 );
        }
        
        int[] arr= TimeUtil.toTimeArray(time);
        if ( stopRes>3 ) {
            timeString+= ( arr[4] < 10 ? "0" : "" ) + arr[4];
            if ( stopRes>4 ) {
                int second= arr[5];
                timeString+=":"+ ( second < 10 ? "0" : "" ) + second;
                if ( stopRes>5 ) {
                    int millis= arr[6];
                    DecimalFormat nf= new DecimalFormat("000");
                    timeString+="."+nf.format(millis);
                    if ( stopRes>6 ) {
                        int micros= arr[7];
                        timeString+=nf.format(micros);
                    }
                }
            }
        }
        
        return  timeString;
    }
    
    private static boolean useDoy= false;
    
    public static void setUseDoy( boolean v ) {
        useDoy= v;
    }

    public static String formatTimeRange( DatumRange self ) {
        return formatTimeRange( self, false );
    }

    /**
     * format the time into an efficient string.  This should be parseable.
     * @param self
     * @param bothDoyYMD if true, use $Y-$m-$d ($j) for formatting days.
     * @return
     */
    public static String formatTimeRange( DatumRange self, boolean bothDoyYMD ) {
        
        String[] monthStr= new String[] { "Jan", "Feb", "Mar", "Apr", "May", "June", "July", "Aug", "Sep", "Oct", "Nov", "Dec" };
        
        double seconds= self.width().doubleValue(Units.seconds);
        
        TimeUtil.TimeStruct ts1= TimeUtil.toTimeStruct(self.min());
        TimeUtil.TimeStruct ts2= TimeUtil.toTimeStruct(self.max());
        
        //        ts1= [ year1, month1, dom1, hour1, minute1, second1, nanos1 ]
        //        ts2= [ year2, month2, dom2, hour2, minute2, second2, nanos2 ]
        
        boolean isMidnight1= TimeUtil.getSecondsSinceMidnight( self.min() ) == 0.;
        boolean isMidnight2= TimeUtil.getSecondsSinceMidnight( self.max() ) == 0.;
        
        boolean isMonthBoundry1= isMidnight1 && ts1.day == 1;
        boolean isMonthBoundry2= isMidnight2 && ts2.day == 1;
        
        boolean isYearBoundry1= isMonthBoundry1 && ts1.month == 1;
        boolean isYearBoundry2= isMonthBoundry2 && ts2.month == 1;
        
        //String toDelim= " \u2013 ";
        String toDelim= " through ";
        if ( isYearBoundry1 && isYearBoundry2 ) {  // no need to indicate month
            if (  ts2.year-ts1.year == 1 ) {
                return "" + ts1.year;
            } else {
                return "" + ts1.year + toDelim + (ts2.year-1);
            }
        } else if ( isMonthBoundry1 && isMonthBoundry2 ) { // no need to indicate day of month
            if ( ts2.month == 1 ) {
                ts2.month=13;
                ts2.year--;
            }
            if ( ts2.year == ts1.year ) {
                if ( ts2.month-ts1.month == 1 ) {
                    return monthStr[ts1.month-1] + " " + ts1.year;
                } else {
                    return monthStr[ts1.month-1]+ toDelim  + monthStr[ts2.month-1-1] + " " + ts1.year;
                }
            } else {
                return monthStr[ts1.month-1] + " " + ts1.year + toDelim
                        + monthStr[ts2.month-1-1] + " " + ts2.year;
            }
        }

        final DatumFormatter daysFormat;
        if ( bothDoyYMD ) {
            daysFormat= TimeDatumFormatter.DOY_DAYS;
        } else if ( useDoy ) {
            daysFormat= TimeDatumFormatter.DAY_OF_YEAR;
        } else {
            daysFormat= TimeDatumFormatter.DAYS;
        }

        if ( isMidnight1 && isMidnight2 ) { // no need to indicate HH:MM
            if ( TimeUtil.getJulianDay( self.max() ) - TimeUtil.getJulianDay( self.min() ) == 1 ) {
                return daysFormat.format( self.min() );
            } else {
                Datum endtime= self.max().subtract( Datum.create( 1, Units.days ) );
                return daysFormat.format( self.min() ) + toDelim
                        + daysFormat.format( endtime );
            }
            
        } else {
            DatumFormatter timeOfDayFormatter;
            
            if ( seconds<1. ) timeOfDayFormatter= TimeDatumFormatter.MILLISECONDS;
            else if ( seconds<60. ) timeOfDayFormatter= TimeDatumFormatter.MILLISECONDS;
            else if ( seconds<3600. ) timeOfDayFormatter= TimeDatumFormatter.SECONDS;
            else timeOfDayFormatter= TimeDatumFormatter.MINUTES;
            
            int minDay= TimeUtil.getJulianDay(self.min());
            int maxDay= TimeUtil.getJulianDay(self.max());
            if ( TimeUtil.getSecondsSinceMidnight(self.max())==0 ) maxDay--;  //  want to have 24:00, not 00:00
            if ( maxDay==minDay ) {
                return daysFormat.format(self.min())
                + " " + efficientTime( self.min(), self.max(), self )
                + " to " +  efficientTime( self.max(), self.min(), self );
            } else {
                timeOfDayFormatter.format( self.min() );
                String sminDay=  daysFormat.format( TimeUtil.prevMidnight( self.min() ) ); //grrr
                String smaxDay=  daysFormat.format( self.max() );
                if ( sminDay.compareTo(smaxDay)>0 ) { // Oh-oh, something has gone terribly wrong.  This is a rounding error...
                    return self.min() + " to " + self.max();
                } 
                return sminDay + " " + timeOfDayFormatter.format( self.min() )
                        + " to " + smaxDay + " " + timeOfDayFormatter.format( self.max() );
            }
        }
    }
    
    /**
     * return a list of DatumRanges that together cover the space identified
     * by bounds.  The list should contain one DatumRange that is equal to
     * element, which should define the phase and period of the list elements.
     * For example,
     * <pre> DatumRange bounds= DatumRangeUtil.parseTimeRangeValid( '2006' );
     * DatumRange first= DatumRangeUtil.parseTimeRangeValid( 'Jan 2006' );
     * List list= generateList( bounds, first );</pre>
     * Note the procedure calls element.previous until bound.min() is contained,
     * then calls bound.max until bound.max() is contained.
     *
     * @param bounds range to be covered.
     * @param element range defining the width and phase of each list DatumRange.
     * @return the ranges covering the range.
     *
     */
    public static List<DatumRange> generateList( DatumRange bounds, DatumRange element ) {
        
        ArrayList<DatumRange> result= new ArrayList();
        DatumRange dr= element;
        while ( dr.max().le( bounds.min() ) ) {
            DatumRange dr1= dr.next();
            if ( dr1.equals(dr) ) break; // TODO: orbits return self when at the beginning or end, requiring code like this.  Maybe they should just return null.
            dr= dr1;
        }
        while ( dr.min().ge( bounds.max() ) ) {
            DatumRange dr1= dr.previous();
            if ( dr1.equals(dr) ) break;
            dr= dr1;
        }
        boolean stepOutside= true;
        while ( dr.max().gt( bounds.min() ) ) {
            DatumRange dr1= dr.previous();
            if ( dr1.equals(dr) ) {
                stepOutside= false;
                break;
            }
            dr= dr1;
        }
        if ( stepOutside ) dr= dr.next();
        while( dr.min().lt(bounds.max() ) ) {
            result.add(dr);
            DatumRange dr1= dr.next();
            if ( dr1.equals(dr) ) break;
            dr= dr1;
        }
        return result;
    }
    
    /**
     * create a new DatumRange.  In the case where lower is greater than
     * upper, the two will be reversed automatically.
     * @param lower
     * @param upper
     * @return 
     */
    public static DatumRange newDimensionless(double lower, double upper) {
        return new DatumRange( Datum.create(lower), Datum.create(upper) );
    }
    
    /** Parse the datum range in the context of units.
     * @param str input like "5 to 15 cm"
     * @param units unit like Units.km
     * @return the DatumRange
     * @throws ParseException 
     */
    public static DatumRange parseDatumRange( String str, Units units ) throws ParseException {
        if ( units instanceof TimeLocationUnits ) {
            return parseTimeRange( str );
        } else {
            // consider Patterns -- dash not handled because of negative sign.
            // 0to4 apples -> 0 to 4 units=apples
            // 0 to 35 sector -> 0 to 35 units=sector  note "to" in sector.
            String[] ss= str.split("to",2);
            if ( ss.length==1 ) {
                ss= str.split("\u2013");
            }
            if ( ss.length != 2 ) {
                throw new ParseException("failed to parse: "+str,0);
            }
            
            // TODO: handle "124.0 to 140.0 kHz" when units= Units.hertz
            Datum d2;
            try {
                d2= DatumUtil.parse(ss[1]);
                if ( d2.getUnits()==Units.dimensionless && units!=Units.dimensionless ) d2= units.parse( ss[1] );
            } catch ( ParseException e ) {
                d2= units.parse( ss[1] );
            }
            if ( ss[0].endsWith("+") ) {  // support 160+to+169
                ss[0]= ss[0].substring(0,ss[0].length()-1);
            }
            
            Datum d1= d2.getUnits().parse( ss[0] );
            
            if ( d1.getUnits().isConvertibleTo(units) ) {
                return new DatumRange( d1, d2 );
            } else {
                throw new ParseException( "Can't convert parsed unit ("+d1.getUnits()+") to "+units, 0 );
            }
        }
    }
    
    public static DatumRange parseDatumRange( String str, DatumRange orig ) throws ParseException {
        return parseDatumRange( str, orig.getUnits() );
    }
	 
    /**
     * This provides unambiguous rules for parsing all types datum ranges strictly 
     * from strings, with no out of band information.  This was introduced to 
     * support das2stream parsing.
     *
     * Examples include: "2013 to 2015 UTC" "3 to 4 kg" "2015-05-05T00:00/2015-06-02T00:00"
     * @param str the string representing a time.
     * @return the DatumRange interpreted.
     * @throws java.text.ParseException
     */
    public static DatumRange parseDatumRange(String str) throws ParseException {
        str= str.trim();
        if ( str.endsWith("UTC" ) ) {
            return parseTimeRange(str.substring(0,str.length()-3));
        } else if ( isISO8601Range(str) ) {
            return parseISO8601Range(str);
        } else {
            // consider Patterns -- dash not handled because of negative sign.
            // 0to4 apples -> 0 to 4 units=apples
            // 0 to 35 sector -> 0 to 35 units=sector  note "to" in sector.
            String[] ss= str.split("to",2);
            if ( ss.length==1 ) {
                ss= str.split("\u2013");
            }
            if ( ss.length==1 ) {
                return parseTimeRange(ss[0]);
            } else if ( ss.length != 2 ) {
                throw new ParseException("failed to parse: "+str,0);
            }
            
            Datum d2;
            Datum d1;
            try {
                d2= DatumUtil.parse(ss[1]);
                d1= d2.getUnits().parse( ss[0] );
                return new DatumRange( d1, d2 );
            } catch ( ParseException ex ) {
                try { 
                    return parseTimeRange(str);
                } catch ( ParseException ex2 ) {
                    // the question now is was it really not a time range, or was it a misformatted datum?
                    if ( TimeUtil.isValidTime(ss[0]) ) {
                        throw ex2;
                    } else {
                        throw ex;
                    }
                }
            }
        }
    }            

    /**
     * parse position strings like "100%-5hr" into [ npos, datum ].
     * Note px is acceptable, but pt is proper.
     * Ems are rounded to the nearest hundredth.
     * Percents are returned as normal (0-1) and rounded to the nearest thousandth.
     * @param s the string, like "100%-5hr"
     * @param result a two-element Datum array with [npos,datum] result[1] provides the units.
     * @return a two-element Datum array with [npos,datum]
     * @throws java.text.ParseException
     */
    public static Datum[] parseRescaleStr( String s, Datum[] result ) throws ParseException {
        String[] ss= s.split("%",-2);
        if ( ss.length==1 ) {
            result[1]= result[1].getUnits().parse(ss[1]);
        } else {
            result[0]= Units.percent.parse(ss[0]);
            if ( ss[1].trim().length()>0 ) {
                result[1]= result[1].getUnits().parse(ss[1]);
            }
        }
        return result;
    }

    /**
     * rescale the DatumRange with a specification like "50%,150%" or "0%-1hr,100%+1hr".  The string is spit on the comma
     * the each is split on the % sign.  This was originally introduced to support CreatePngWalk in Autoplot.
     * @param dr
     * @param rescale
     * @return
     * @throws ParseException
     */
    public static DatumRange rescale( DatumRange dr, String rescale ) throws ParseException {
        String[] ss= rescale.split(",");
        Datum[] rrmin= new Datum[] { Units.percent.createDatum(0), dr.getUnits().getOffsetUnits().createDatum(0.) };
        Datum[] rrmax= new Datum[] { Units.percent.createDatum(100), dr.getUnits().getOffsetUnits().createDatum(0.) };

        if ( ss[0].trim().length()>0 ) {
            rrmin= parseRescaleStr( ss[0], rrmin );
        }
        if ( ss[1].trim().length()>0 ) {
            rrmax= parseRescaleStr( ss[1], rrmax );
        }
        DatumRange result;
        if ( rrmin[0].doubleValue(Units.percent)==0 && rrmax[0].doubleValue(Units.percent)==100 ) {
            result= dr;
        } else {
            result= rescale( dr, rrmin[0].doubleValue(Units.percent)/100, rrmax[0].doubleValue(Units.percent)/100 );
        }
        result= new DatumRange( result.min().add( rrmin[1] ), result.max().add( rrmax[1] ) );
        return result;
        
    }

    /**
     * rescale the DatumRange with a specification like "50%,150%" or
     * "0%-1hr,100%+1hr".  The string is split on the comma the each is split on
     * the % sign.  This was originally introduced to support CreatePngWalk in 
     * Autoplot.
     * @param dr
     * @param rescale
     * @return
     * @throws ParseException
     */
    public static DatumRange rescaleInverse( DatumRange dr, String rescale ) throws ParseException {
        String[] ss= rescale.split(",");
        Datum[] rrmin= new Datum[] { Units.percent.createDatum(0), dr.getUnits().getOffsetUnits().createDatum(0.) };
        Datum[] rrmax= new Datum[] { Units.percent.createDatum(100), dr.getUnits().getOffsetUnits().createDatum(0.) };

        if ( ss[0].trim().length()>0 ) {
            rrmin= parseRescaleStr( ss[0], rrmin );
        }
        if ( ss[1].trim().length()>0 ) {
            rrmax= parseRescaleStr( ss[1], rrmax );
        }
        DatumRange result;
        result= new DatumRange( dr.min().subtract( rrmin[1] ), dr.max().subtract( rrmax[1] ) );
        
        double min= rrmin[0].doubleValue(Units.percent)/100;
        double max= rrmax[0].doubleValue(Units.percent)/100;
        
        result= rescaleInverse(result, min, max );
        
        return result;
        
    }
    
    /**
     * returns DatumRange relative to dr, where 0 is the minimum, and 1 is the maximum.
     * For example rescale(1,2) is scanNext, rescale(-0.5,1.5) is zoomOut.
     * @param dr a DatumRange with nonzero width.
     * @param min the new min normalized with respect to this range.  0. is this range's min, 1 is this range's max
     * @param max the new max with normalized wrt this range.  0. is this range's min, 1 is this range's max.
     * @return new DatumRange.
     */
    public static DatumRange rescale( DatumRange dr, double min, double max ) {
        Datum w= dr.width();
        if ( !w.isFinite() ) {
            throw new RuntimeException("width is not finite (containing fill) in rescale");
        }
        if ( w.doubleValue( w.getUnits() )==0. ) {
            // condition that might cause an infinate loop!  For now let's check for this and throw RuntimeException.
            throw new RuntimeException("width is zero in rescale!");
        }
        return new DatumRange( dr.min().add( w.multiply(min) ), dr.min().add( w.multiply(max) ) );
    }
    
    /**
     * returns Datum relative to dr, where 0 is the minimum, and 1 is the maximum.
     * @param dr a DatumRange with nonzero width.
     * @param n the location normalized with respect to this range.  0. is the range's min, 1 is the range's max.
     * @return the Datum
     */
    public static Datum rescale( DatumRange dr, double n ) {
        Datum w= dr.width();
        if ( !w.isFinite() ) {
            throw new RuntimeException("width is not finite (containing fill) in rescale");
        }
        if ( w.doubleValue( w.getUnits() )==0. ) {
            // condition that might cause an infinate loop!  For now let's check for this and throw RuntimeException.
            throw new RuntimeException("width is zero in rescale!");
        }
        return dr.min().add( w.multiply(n) );
    }
    
    /**
     * inverse of rescale function
     * @param dr a DatumRange with nonzero width.
     * @param min the new min normalized with respect to this range.  0. is this range's min, 1 is this range's max
     * @param max the new max with normalized wrt this range.  0. is this range's min, 1 is this range's max.
     * @return new DatumRange.
     */
    public static DatumRange rescaleInverse( DatumRange dr, double min, double max ) {
        Datum w= dr.width().divide( max-min );
        return new DatumRange( dr.min().add(w.multiply(-min) ), dr.min().add(w.multiply(max) ) );
    }
    
    /**
     * returns DatumRange relative to this, where 0. is the minimum, and 1. is the maximum, but the
     * scaling is done in the log space.
     * For example, rescaleLog( [0.1,1.0], -1, 2 ) &rarr; [ 0.01, 10.0 ]
     * @param dr a DatumRange with nonzero width.
     * @param min the new min normalized with respect to this range.  0. is this range's min, 1 is this range's max, 0 is
     * min-width.
     * @param max the new max with normalized wrt this range.  0. is this range's min, 1 is this range's max, 0 is
     * min-width.
     * @return new DatumRange.
     */
    public static DatumRange rescaleLog( DatumRange dr, double min, double max ) {
        Units u= dr.getUnits();
        double s1= Math.log10( dr.min().doubleValue(u) );
        double s2= Math.log10( dr.max().doubleValue(u) );
        double w= s2 - s1;
        if ( w==0. ) {
            // condition that might cause an infinate loop!  For now let's check for this and throw RuntimeException.
            throw new RuntimeException("width is zero!");
        }
        s2= Math.pow( 10, s1 + max * w ); // danger
        s1= Math.pow( 10, s1 + min * w );
        return new DatumRange( s1, s2, u );
    }
    
    /**
     * create a DatumRange with the value for the center, and the width.
     * @param middle
     * @param width
     * @return datumRange centered at middle with the given width.
     */
    public static DatumRange createCentered(Datum middle, Datum width) {
        Units u= middle.getUnits();
        double m= middle.doubleValue(u);
        double dm= width.doubleValue( u.getOffsetUnits() );
        return DatumRange.newDatumRange( m-dm/2, m+dm/2, u );
    }

    /**
     * returns the position within dr, where 0. is the dr.min(), and 1. is dr.max()
     * @param dr a datum range with non-zero width.
     * @param d a datum to normalize with respect to the range.
     * @return a double indicating the normalized datum.
     * @see #rescale(org.das2.datum.DatumRange, double, double) 
     */
    public static double normalize( DatumRange dr, Datum d ) {
        Units u= dr.getUnits();
        double d0= dr.min().doubleValue( u );
        double d1= dr.max().doubleValue( u );
        double dd= d.doubleValue( u );
        return (dd-d0) / ( d1-d0 );
    }
    
    public static double normalize( DatumRange dr, Datum d, boolean log ) {
        if ( log ) {
            return normalizeLog( dr, d );
        } else {
            return normalize( dr, d );   
        }
    }

    /**
     * returns the position within dr, where 0. is the dr.min(), and 1. is dr.max()
     * @param dr a datum range with non-zero width.
     * @param d a datum to normalize with respect to the range.
     * @return a double indicating the normalized datum.
     */
    public static double normalizeLog( DatumRange dr, Datum d ) {
        Units u= dr.getUnits();
        double d0= Math.log( dr.min().doubleValue( u ) );
        double d1= Math.log( dr.max().doubleValue( u ) );
        double dd= Math.log( d.doubleValue( u ) );
        return (dd-d0) / ( d1-d0 );
    }
    
    /**
     * Like DatumRange.intersects, but returns a zero-width range when the two do
     * not intersect.  When they do not intersect, the min or max of the first range
     * is returned, depending on whether or not the second range is above or below
     * the first range.  Often this allows for simpler code.
     * @param range the first DatumRange
     * @param include the second DatumRange
     * @return a DatumRange that contains parts of both ranges, or is zero-width.
     * @see DatumRange#intersection(org.das2.datum.DatumRange) 
     */
    public static DatumRange sloppyIntersection( DatumRange range, DatumRange include ) {
        Units units= range.getUnits();
        double s11= range.min().doubleValue(units);
        double s12= include.min().doubleValue(units);
        double s21= range.max().doubleValue(units);
        if ( range.intersects(include) ) {
            double s1=  Math.max( s11, s12 );
            double s22= include.max().doubleValue(units);
            double s2= Math.min( s21, s22 );
            return new DatumRange( s1, s2, units );
        } else {
            if ( s11<s12 ) {
                return new DatumRange(s21,s21,units);
            } else {
                return new DatumRange(s11,s11,units);
            }
        }
    }
    
    /**
     * return the elements of src that intersect with elements of the list contains.
     * Both src and dst should be sorted lists that do not contain overlaps.
     * @param bounds sorted list of non-overlapping ranges.
     * @param elements sorted list of non-overlapping ranges.  If remove is true, then
     *   the elements intersecting are removed and the result will contain non-overlapping elements.
     * @param remove if true, remove intersecting elements from the elements list, leaving the elements that
     *   did not intersect with any of the ranges in bounds.
     * @return list of elements of src that overlap with elements of contains.
     */
    public static List<DatumRange> intersection( List<DatumRange> bounds, List<DatumRange> elements, boolean remove ) {

        int is= 0;
        int ns= bounds.size();
        int cs= elements.size();

        List<DatumRange> result= new ArrayList();

        List<DatumRange> contained= new ArrayList();

        DatumRange lastAdded= null;

        DatumRange bounds1;
        int i=0;
        while ( i<elements.size() ) {
            while ( is<ns && bounds.get(is).max().le( elements.get(i).min() ) ) is++;
            if ( is==ns ) break;
            while ( i<cs && elements.get(i).max().le( bounds.get(is).min() )) i++;
            if ( i==cs ) break;
            bounds1= bounds.get(is);
            if ( i<cs && bounds1.intersects( elements.get(i) ) ) {
                while ( i<cs && bounds1.intersects( elements.get(i) ) ) {
                    if ( remove ) contained.add( elements.get(i) );
                    i++;
                    if ( bounds1!=lastAdded ) {
                        result.add( bounds1);
                        lastAdded= bounds1;
                    }
                }
                if ( lastAdded==bounds.get(is) ) is++;
                if ( i>0 ) i--; // the last one might contain part of the next bound.
            }
        }
        if ( remove ) elements.removeAll(contained);
        return result;

    }

    /**
     * Like DatumRange.contains, but includes the end point.  Often this allows for simpler code.
     * @param context the datum range.
     * @param datum the data point
     * @return true if the range contains the datum.
     * @see DatumRange#contains(org.das2.datum.DatumRange) 
     */
    public static boolean sloppyContains(DatumRange context, Datum datum) {
        return context.contains(datum) || context.max().equals(datum);
    }    
    
    /**
     * return a datum range that sloppily covers the d1 and d2.
     * (The bigger of the two will be the exclusive max.)
     * @param d1
     * @param d2
     * @return
     * @throws InconvertibleUnitsException 
     */
    public static DatumRange union( Datum d1, Datum d2 ) {
        Units units= d1.getUnits();
        if ( d1.isFill() ) return new DatumRange( d2, d2 );
        if ( d2.isFill() ) return new DatumRange( d1, d1 );
        double s1= d1.doubleValue(units);
        double s2= d2.doubleValue(units);
        return new DatumRange( Math.min(s1,s2), Math.max(s1, s2), units );
    }

    /**
     * return the union of a DatumRange and Datum.  If they do not intersect, the
     * range between the two is included as well.
     * @param range the range or null.  If range is null, then new DatumRange( include, include ) is returned.
     * @param include a datum to add this this range.  If its the max, then
     * it will be the end of the datum range, not included.
     * @return DatumRange containing all three boundaries of the range and datum
     * @throws InconvertibleUnitsException 
     */
    public static DatumRange union( DatumRange range, Datum include ) {
        if ( range==null ) {
            return new DatumRange( include, include );
        }
        if ( include.isFill() ) return range;
        Units units= range.getUnits();
        double s11= range.min().doubleValue(units);
        double s12= include.doubleValue(units);
        double s21= range.max().doubleValue(units);
        double s22= include.doubleValue(units);

        return new DatumRange( Math.min(s11,s12), Math.max(s21, s22), units );

    }

    /**
     * return the union of two DatumRanges.  If they do not intersect, the
     * range between the two is included as well.
     * @param range the range, or null.  If range is null then include is returned.
     * @param include the DatumRange to include.
     * @return DatumRange containing all four boundaries of the two datum ranges.
     * @throws InconvertibleUnitsException 
     */
    public static DatumRange union( DatumRange range, DatumRange include ) {
        if ( include==null ) throw new NullPointerException("include argument is null");
        if ( range==null ) return include;
        Units units= range.getUnits();
        double s11= range.min().doubleValue(units);
        double s12= include.min().doubleValue(units);
        double s21= range.max().doubleValue(units);
        double s22= include.max().doubleValue(units);
        
        return new DatumRange( Math.min(s11,s12), Math.max(s21, s22), units );
        
    }

    /**
     * return true if the time ranges are overlapping with bounds within 
     * @param timeRange1
     * @param timeRange2
     * @param percent double from 0 to 100.
     * @return true if the two ranges are sufficiently close.
     */
    public static boolean fuzzyEqual( DatumRange timeRange1, DatumRange timeRange2, double percent ) {
        if ( timeRange1.width().value()==0 ) {
            return timeRange1.equals(timeRange2);
        }
        double n0= DatumRangeUtil.normalize( timeRange1, timeRange2.min() );
        double n1= DatumRangeUtil.normalize( timeRange1, timeRange2.max() );
        double p= percent/100.;
        return Math.abs(n0)<p && Math.abs(n1-1)<p ;
    }
    
    /**
     * round to a nice interval with very roughly n divisions.  For example,
     * <pre>
     *   -0.048094730687625806 to 0.047568, 100  &rarr; -0.048 to 0.048
     *   2012-04-18 0:00:00 to 23:59:40, 24 &rarr; 2012-04-18  
     *   2014-08-10 0:00:00 to 2014-08-11T00:00:59, 24 &rarr; 2014-08-10
     * </pre>
     * @param dr
     * @param n
     * @return dr when its width is zero, or a rounded range.
     */
    public static DatumRange roundSections( DatumRange dr, int n ) {
        if ( dr.width().value()==0 ) {
            return dr;
        }
        DomainDivider dd= DomainDividerUtil.getDomainDivider( dr.min(), dr.max() );
        while ( dd.boundaryCount( dr.min(), dr.max() ) > n ) {
            dd= dd.coarserDivider(false);
        }
        while ( dd.boundaryCount( dr.min(), dr.max() ) < n ) {
            dd= dd.finerDivider(false);
        }
        DatumRange min= dd.rangeContaining( dr.min() );
        DatumRange max= dd.rangeContaining( dr.max() );
        if ( DatumRangeUtil.normalize( min, dr.min() )>0.99 ) {
            min= min.next();
        }
        if ( DatumRangeUtil.normalize( max, dr.max() )<0.01 ) {
            max=max.previous();
        }
        return union( min,max );
    }

}
