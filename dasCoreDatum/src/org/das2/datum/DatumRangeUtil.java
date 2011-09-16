/*
 * DatumRangeUtil.java
 *
 * Created on September 16, 2004, 2:35 PM
 */

package org.das2.datum;

import java.text.*;
import java.util.*;
import java.util.logging.*;
import java.util.regex.*;
import org.das2.datum.format.DatumFormatter;
import org.das2.datum.format.TimeDatumFormatter;

/**
 *
 * @author  Jeremy
 */
public class DatumRangeUtil {
    
    private static final int DATEFORMAT_USA= 1;
    private static final int DATEFORMAT_EUROPE= 0;
    private static final int DATEFORMAT_YYYY_DDD= 2;
    
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

    private static Datum MIN_TIME= TimeUtil.createTimeDatum( 1000, 1, 1, 0, 0, 0, 0 ); // I know these are found elsewhere in the code, but I can't find it.
    private static Datum MAX_TIME= TimeUtil.createTimeDatum( 3000, 1, 1, 0, 0, 0, 0 );

    /**
     * put limits on the typical use when looking at data:
     *   * both min and max are finite numbers
     *   * time range is between year 1000 and year 3000.
     *   * log ranges span no more than 1e100 cycles.
     * @param newDatumRange
     * @return
     */
    public static boolean isAcceptable(DatumRange dr, boolean log ) {
        if ( Double.isInfinite( dr.min().doubleValue( dr.getUnits() ) ) ||
                Double.isInfinite( dr.max().doubleValue( dr.getUnits() ) ) ) {
            return false;
        } else if ( UnitsUtil.isTimeLocation( dr.getUnits() )
                && ( dr.min().lt( MIN_TIME ) || dr.max().gt( MAX_TIME ) ) ) {
            return false;
        } else if ( log && dr.max().divide(dr.min()).doubleValue(Units.dimensionless) > 1e100 ) {
            return false;
        }
        return true;
    }
    
    public static class DateDescriptor {
        String date;
        String year;
        String month;
        String day;
        String delim;
        int dateformat;
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

    private static Pattern time1, time2, time3, time4, time5;
    static {
        String d= "[-:]"; // delim
        String i4= "(\\d\\d\\d\\d)";
        String i3= "(\\d\\d\\d)";
        String i2= "(\\d\\d)";

        String iso8601time= i4 + d + i2 + d + i2 + "T" + i2 + d + i2 + "(" + d + i2 + ")?Z?" ;
        String iso8601time2= i4 + i2 + i2 + "T" + i2 + i2 + "(" + i2 + ")?Z?" ;
        String iso8601time3= i4 + d + i3 + "T" + i2 + d + i2 + "(" + i2 + ")?Z?" ;
        String iso8601time4= i4 + d + i2 + d + i2 + "Z?" ;
        String iso8601time5= i4 + d + i3 + "Z?" ;
        time1= Pattern.compile(iso8601time);
        time2= Pattern.compile(iso8601time2);
        time3= Pattern.compile(iso8601time3);
        time4= Pattern.compile(iso8601time4);
        time5= Pattern.compile(iso8601time5);
    }
    /**
     * returns null or int[7]: [ Y, m, d, H, M, S, nano ]
     *
     * @param stringIn
     * @param periodEnd true means the string refers to the end of a period,
     *   and for ranges (Years,Months,Days) the time should be inclusive.
     *
     *
     * @return
     */
    public static int[] parseISO8601 ( String str ) {

        Matcher m;

        m= time1.matcher(str);
        if ( m.matches() ) {
            return new int[] { Integer.parseInt( m.group(1) ), Integer.parseInt( m.group(2) ), Integer.parseInt( m.group(3) ), getInt( m.group(4), 0 ), getInt( m.group(5), 0 ), getInt( m.group(7), 0), 0 };
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
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * returns the time found in an iso8601 string, or null.  This supports
     * periods (durations) as in: 2007-03-01T13:00:00Z/P1Y2M10DT2H30M
     * Other examples:
     *   2007-03-01T13:00:00Z/2008-05-11T15:30:00Z
     *   2007-03-01T13:00:00Z/P1Y2M10DT2H30M
     *   P1Y2M10DT2H30M/2008-05-11T15:30:00Z
     *   2007-03-01T00:00Z/P1D
     * http://en.wikipedia.org/wiki/ISO_8601#Time_intervals
     * @param stringIn
     * @return null or a DatumRange
     */
    public static DatumRange parseISO8601Range( String stringIn ) {

        String iso8601duration= "P(\\d+Y)?(\\d+M)?(\\d+D)?(T(\\d+H)?(\\d+M)?(\\d+S)?)?";
        
        String[] parts= stringIn.split("/",-2);
        if ( parts.length!=2 ) return null;

        boolean d1= parts[0].charAt(0)=='P';
        boolean d2= parts[1].charAt(0)=='P';

        Pattern p;
        Matcher m;

        int[] digits1= null;
        int[] digits2= null;

        Pattern duration= Pattern.compile(iso8601duration);

        if ( d1 ) {
            m= duration.matcher(parts[0]);
            if ( m.matches() ) {
                digits1= new int[] { getInt( m.group(1), 0 ), getInt( m.group(2), 0 ), getInt( m.group(3), 0 ), getInt( m.group(5), 0 ), getInt( m.group(6), 0 ), getInt( m.group(7), 0 ) };
            }
        } else {
            digits1= parseISO8601( parts[0] );
        }

        if ( d2 ) {
            m= duration.matcher(parts[1]);
            if ( m.matches() ) {
                digits2= new int[] { getInt( m.group(1), 0 ), getInt( m.group(2), 0 ), getInt( m.group(3), 0 ), getInt( m.group(5), 0 ), getInt( m.group(6), 0 ), getInt( m.group(7), 0 ) };
            }
        } else {
            digits2= parseISO8601( parts[1] );
        }

        if ( digits1==null || digits2==null ) return null;
        
        if ( d1 ) {
            for ( int i=0; i<6; i++ ) digits1[i] = digits2[i] - digits1[i];
        }

        if ( d2 ) {
            for ( int i=0; i<6; i++ ) digits2[i] = digits1[i] + digits2[i];
        }

        Datum time1= TimeUtil.toDatum(digits1);
        Datum time2= TimeUtil.toDatum(digits2);

        return new DatumRange( time1, time2 );


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
    //    ;; keeps track of format(e.g. %Y-%j) for debugging, and perhaps to reserialize
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
        
        String delimRegEx= "\\s|-|/|\\.|:|to|through|span|T|Z|\u2013|,";
        Pattern delimPattern= Pattern.compile( delimRegEx );
        int[] ts1= new int[] { -1, -1, -1, -1, -1, -1, -1 };
        int[] ts2= new int[] { -1, -1, -1, -1, -1, -1, -1 };
        int[] ts= null;
        
        boolean beforeTo;
        
        private Pattern yyyymmddPattern= Pattern.compile("((\\d{4})(\\d{2})(\\d{2}))( |to|t|-|$)");
        
        /* groups= group numbers: { year, month, day, delim } (0 is all) */
        private boolean tryPattern( Pattern regex, String string, int[] groups, DateDescriptor dateDescriptor ) throws ParseException {
            Matcher matcher= regex.matcher( string.toLowerCase() );
            if ( matcher.find() && matcher.start()==0 ) {
                // printGroups(matcher);
                int posDate= matcher.start();
                int length= matcher.end()-matcher.start();
                dateDescriptor.delim= matcher.group(groups[3]);
                dateDescriptor.date= string.substring( matcher.start(), matcher.end()-dateDescriptor.delim.length() );
                String month;
                String day;
                String year;
                dateDescriptor.day= matcher.group(groups[2]);
                dateDescriptor.month= matcher.group(groups[1]);
                dateDescriptor.year= matcher.group(groups[0]);
                return true;
            } else {
                return false;
            }
        }
        
        public boolean isTime( String string, int[] timearr ) throws ParseException {
            Matcher m;
            Pattern hhmmssmmPattern= Pattern.compile( "(\\d+):(\\d\\d+):(\\d\\d+).(\\d+) )" );
            Pattern hhmmssPattern= Pattern.compile( "(\\d+):(\\d\\d+):(\\d\\d+)" );
            Pattern hhmmPattern= Pattern.compile( "(\\d+):(\\d\\d+)" );
            Pattern hhPattern= Pattern.compile( "(\\d+):" );
            
            if ( (m=hhmmssmmPattern.matcher(string)).matches() ) {
                timearr[HOUR]= Integer.parseInt( m.group(1) );
                timearr[MINUTE]= Integer.parseInt( m.group(2) );
                timearr[SECOND]= Integer.parseInt( m.group(3) );
                timearr[NANO]= (int)( Integer.parseInt( m.group(4) ) * ( 100000 / Math.pow( 10, m.group(4).length() ) ));
                throw new RuntimeException("working on this");
            } else if (( m=hhmmssPattern.matcher(string)).matches() ) {
            } else if (( m=hhmmPattern.matcher(string)).matches() ) {
            } else if (( m=hhPattern.matcher(string)).matches() ) {
            }
            return false;
        }
        
        public boolean isDate( String string, DateDescriptor dateDescriptor ) throws ParseException {
            //  this is introduced because mm/dd/yy is so ambiguous, the parser
            //  has trouble with these dates.  Check for these as a group.
            
            if ( string.length()<6 ) return false;
            
            int[] groups;
            String dateDelimRegex= "( |to|t|-)";
            String yearRegex= "(\\d{2}(\\d{2})?)"; // t lower case because tryPattern folds case
            
            if ( tryPattern( yyyymmddPattern, string, new int[] { 2,3,4,5 }, dateDescriptor ) ) {
                dateDescriptor.dateformat= DATEFORMAT_USA;
                return true;
            }
            
            String delim;
            
            String delims="(/|\\.|-| )";
            Matcher matcher= Pattern.compile(delims).matcher(string);
            
            if ( matcher.find() ) {
                int posDelim= matcher.start();
                delim= string.substring(matcher.start(),matcher.end());
            } else {
                return false;
            }
            
            if ( delim.equals(".") ) {
                delim="\\.";
            }
            
            String monthNameRegex= "(jan[a-z]*|feb[a-z]*|mar[a-z]*|apr[a-z]*|may|june?|july?|aug[a-z]*|sep[a-z]*|oct[a-z]*|nov[a-z]*|dec[a-z]*)";
            String monthRegex= "((\\d?\\d)|"+monthNameRegex+")";
            String dayRegex= "(\\d?\\d)";
            
            String euroDateRegex;
            
            if ( delim.equals("\\.") ) {
                euroDateRegex= "(" + dayRegex + delim + monthRegex + delim + yearRegex + dateDelimRegex + ")";
                groups= new int [] { 6, 3, 2, 8  };
            } else {
                euroDateRegex= "(" + dayRegex + delim + monthNameRegex + delim + yearRegex + dateDelimRegex + ")";
                groups= new int [] { 4, 3, 2, 6 };
            }
            if ( tryPattern( Pattern.compile( euroDateRegex ), string, groups, dateDescriptor ) ) {
                dateDescriptor.dateformat= DATEFORMAT_EUROPE;
                return true;
            }
            
            String usaDateRegex= monthRegex + delim + dayRegex + delim + yearRegex + dateDelimRegex ;
            if ( tryPattern( Pattern.compile( usaDateRegex ), string, new int[] { 5,1,4,7 }, dateDescriptor ) ) {
                dateDescriptor.dateformat= DATEFORMAT_USA;
                return true;
            }
            
            // only works for four-digit years
            String lastDateRegex= "(\\d{4})" + delim + monthRegex + delim + dayRegex + dateDelimRegex;
            if ( tryPattern( Pattern.compile( lastDateRegex ), string, new int[] { 1,2,5,6 }, dateDescriptor ) ) {
                dateDescriptor.dateformat= DATEFORMAT_USA;
                return true;
            }
            
            String doyRegex= "(\\d{3})";
            String dateRegex= doyRegex+"(-|/)" + yearRegex + dateDelimRegex;
            
            if ( tryPattern( Pattern.compile( dateRegex ), string, new int[] { 3,1,1,5 }, dateDescriptor ) ) {
                int doy= parseInt(dateDescriptor.day);
                if ( doy>366 ) return false;
                int year= parseInt(dateDescriptor.year);
                caldat( julday( 12, 31, year-1 ) + doy, dateDescriptor );
                dateDescriptor.dateformat= DATEFORMAT_YYYY_DDD;
                
                return true;
            }
            
            dateRegex= yearRegex +"(-|/)" + doyRegex + dateDelimRegex;
            if ( tryPattern( Pattern.compile( dateRegex ), string, new int[] { 1,4,4,5 }, dateDescriptor ) ) {
                int doy= parseInt(dateDescriptor.day);
                if ( doy>366 ) return false;
                int year= parseInt(dateDescriptor.year);
                caldat( julday( 12, 31, year-1 ) + doy, dateDescriptor );
                dateDescriptor.dateformat= DATEFORMAT_YYYY_DDD;
                
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
        public String normalizeTo( String s ) throws ParseException {
            
            int minusCount= 0;
            for ( int i=0; i<s.length(); i++ ) if ( s.charAt(i)=='-' ) minusCount++;
            if ( minusCount==0 ) return s;
            
            DateDescriptor dateDescriptor= new DateDescriptor();
            ipos=0;
            
            StringBuffer newString= new StringBuffer();
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
            
            String[] ss= result.split("to");
            if ( ss.length>2 ) {
                result= ss[0];
                for ( int i=1; i<ss.length; i++ ) {
                    result= result + "-" + ss[i];
                }
            } else if ( ss.length==2 ) { // kludgy check for YYYY to YYYY, everything else is error
                String s0= ss[0].trim();
                String s1= ss[1].trim();
                if ( !isYear(s0) || !isYear(s1) ) {
                    result= s0 + " " + s1;
                }
            }
            return result;
        }
        
        public DatumRange parse( String stringIn ) throws ParseException {
            
            Logger logger= Logger.getLogger("das2.datum");

            DatumRange check= parseISO8601Range( stringIn );
            if ( check!=null ) return check;

            this.string= stringIn+" ";
            this.ipos= 0;

            if ( stringIn.length()==0 ) {
                throw new ParseException("empty string",0);
            }
            ArrayList beforeToUnresolved= new ArrayList();
            ArrayList afterToUnresolved= new ArrayList();
            
            String[] formatCodes= new String[] { "%y", "%m", "%d", "%H", "%M", "%S", "" };
            formatCodes[6]= "%N"; // note %_ms, %_us might be used instead
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
            
            int dateFormat= DATEFORMAT_USA;
            
            String newString= normalizeTo(string);
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
                    ts2[NANO]= (int)(( tt.seconds - ts2[SECOND] ) * 1e9);
                    ipos= newString.length();
                }
                
                if ( isDate( string.substring( ipos ), dateDescriptor ) ) {
                    format= format+"%x";
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
                        format= format+"%Y";
                        if ( ts1[YEAR] == -1 && beforeTo ) {
                            ts1[YEAR]= parseInt(token);
                            ts2[YEAR]= ts1[YEAR];
                        } else {
                            setBeforeTo( false );
                            ts2[YEAR]= parseInt(token);
                            if ( ts1[YEAR]==-1 ) ts1[YEAR]=ts2[YEAR];
                        }
                    } else if ( isDayOfYear(token) ) {
                        dateFormat=DATEFORMAT_YYYY_DDD;
                        format= format+"%j";
                        if ( ts1[YEAR] == -1 ) {
                            throw new ParseException( "day of year before year: "+stringIn+" ("+format+")", ipos );
                        }
                        int doy= parseInt(token);
                        caldat( julday( 12, 31, ts1[YEAR]-1 ) + doy, dateDescriptor );
                        dateFormat= DATEFORMAT_YYYY_DDD;
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
                        format= format+"%b";
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
                                format= format+"%H";
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
                                        case 3: format= format+"%_ms"; break;
                                        case 6: format= format+"%_us"; break;
                                        default: format=format+"%N"; break;
                                        
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
                StringBuffer stringBuffer= new StringBuffer("ts1: ");
                for ( int i=0; i<7; i++ ) stringBuffer.append(""+ts1[i]+" ");
                logger.fine( stringBuffer.toString() );
                stringBuffer= new StringBuffer("ts2: ");
                for ( int i=0; i<7; i++ ) stringBuffer.append(""+ts2[i]+" ");
                logger.fine( stringBuffer.toString() );
                logger.fine( format );
            }
            
            if ( beforeTo ) {
                int idx=0;
                for ( int i=0; i<beforeToUnresolved.size(); i++ ) {
                    while( ts1[idx]!=-1 ) idx++;
                    ts1[idx]= parseInt((String)beforeToUnresolved.get(i));
                    String[] s= format.split("UNRSV1"+(i+1));
                    format= s[0]+formatCodes[idx]+s[1];
                }
                beforeToUnresolved.removeAll(beforeToUnresolved);
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
                        idx--;
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
                            String[] s= format.split("UNRSV1"+(i+1));
                            format= s[0]+formatCodes[idx]+s[1];
                        }
                        unload= afterToUnresolved;
                        formatUn= "UNRSV2";
                        ts= ts2;
                    } else {
                        while( idx<7 && ts2[idx]!=-1) idx++;
                        idx--;
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
                StringBuffer stringBuffer= new StringBuffer("ts1: ");
                for ( int i=0; i<7; i++ ) stringBuffer.append(""+ts1[i]+" ");
                logger.fine( stringBuffer.toString() );
                stringBuffer= new StringBuffer("ts2: ");
                for ( int i=0; i<7; i++ ) stringBuffer.append(""+ts2[i]+" ");
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
            
            
            if ( ts1lsd != ts2lsd && ( ts1lsd<HOUR || ts2lsd<HOUR ) ) {
                throw new ParseException( "resolution mismatch: "+digitIdentifiers[ts1lsd]+" specified for start, but "
                        + digitIdentifiers[ts2lsd]+" specified for end, must be same" + " in \""+stringIn+"\""+ " ("+format+")", ipos );
            }
            
            if ( beforeTo ) isThroughNotTo= true;
            
            if ( isThroughNotTo ) {
                ts2[ts2lsd]++;
            }
            
            if ( ts1[0]<1900 ) ts1[0]= y2k(""+ts1[0]);
            if ( ts2[0]<1900 ) ts2[0]= y2k(""+ts2[0]);
            
            if ( ts1lsd < DAY ) {
                try {
                    return new MonthDatumRange( ts1, ts2 );
                } catch ( IllegalArgumentException e ) {
                    ParseException eNew= new ParseException( "fails to parse due to MonthDatumRange: "+stringIn+ " ("+format+")", 0 );
                    eNew.initCause(e);
                    throw eNew;
                }
            } else {
                Datum time1= TimeUtil.createTimeDatum( ts1[0], ts1[1], ts1[2], ts1[3], ts1[4], ts1[5], ts1[6] );
                Datum time2= TimeUtil.createTimeDatum( ts2[0], ts2[1], ts2[2], ts2[3], ts2[4], ts2[5], ts2[6] );
                return new DatumRange( time1, time2 );
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
     * @throws ParseException when the string cannot be parsed.
     * @param s
     * @return
     */
    public static DatumRange parseTimeRange( String string ) throws ParseException {
        return new TimeRangeParser().parse(string);
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
        for ( int i=0;i<times.length;i++ ) {
            int[] arr= TimeUtil.toTimeArray(times[i]);
            int idigit;
            for ( idigit=7; idigit>3; idigit-- ) {
                if ( arr[idigit]>0 ) break;
            }
            stopRes= Math.max( stopRes, idigit );
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
    
    public static boolean useDoy= false;

    public static String formatTimeRange( DatumRange self ) {
        
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

        final DatumFormatter daysFormat= useDoy ? TimeDatumFormatter.DAY_OF_YEAR : TimeDatumFormatter.DAYS;

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
     *
     */
    public static List<DatumRange> generateList( DatumRange bounds, DatumRange element ) {
        
        ArrayList<DatumRange> result= new ArrayList();
        DatumRange dr= element;
        while ( dr.max().le( bounds.min() ) ) {
            dr= dr.next();
        }
        while ( dr.min().ge( bounds.max() ) ) {
            dr= dr.previous();
        }
        while ( dr.max().gt( bounds.min() ) ) {
            dr= dr.previous();
        }
        dr= dr.next();
        while( dr.min().lt(bounds.max() ) ) {
            result.add(dr);
            dr= dr.next();
        }
        return result;
    }
    
    
    public static DatumRange newDimensionless(double lower, double upper) {
        return new DatumRange( Datum.create(lower), Datum.create(upper) );
    }
    
    public static DatumRange parseDatumRange( String str, Units units ) throws ParseException {
        if ( units instanceof TimeLocationUnits ) {
            return parseTimeRange( str );
        } else {
            // consider Patterns -- dash not handled because of negative sign.
            String[] ss= str.split("to");
            if ( ss.length==1 ) {
                ss= str.split("\u2013");
            }
            if ( ss.length != 2 ) {
                if ( ss.length==3 ) {
                    ss[0]= "-"+ss[1];
                    ss[1]= ss[2];
                } else {
                    throw new ParseException("failed to parse: "+str,0);
                }
            }
            
            // TODO: handle "124.0 to 140.0 kHz" when units= Units.hertz
            Datum d2;
            try {
                d2= DatumUtil.parse(ss[1]);
                if ( d2.getUnits()==Units.dimensionless ) d2= units.parse( ss[1] );
            } catch ( ParseException e ) {
                d2= units.parse( ss[1] );
            }
            Datum d1= d2.getUnits().parse( ss[0] );
            
            if ( d1.getUnits().isConvertableTo(units) ) {
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
     * returns DatumRange relative to this, where 0. is the minimum, and 1. is the maximum.
     * For example rescale(1,2) is scanNext, rescale(0.5,1.5) is zoomOut.
     * @param dr a DatumRange with nonzero width.
     * @param min the new min normalized with respect to this range.  0. is this range's min, 1 is this range's max, 0 is
     * min-width.
     * @param max the new max with normalized wrt this range.  0. is this range's min, 1 is this range's max, 0 is
     * min-width.
     * @return new DatumRange.
     */
    public static DatumRange rescale( DatumRange dr, double min, double max ) {
        Datum w= dr.width();
        if ( !w.isFinite() ) {
            throw new RuntimeException("width is not finite");
        }
        if ( w.doubleValue( w.getUnits() )==0. ) {
            // condition that might cause an infinate loop!  For now let's check for this and throw RuntimeException.
            throw new RuntimeException("width is zero!");
        }
        return new DatumRange( dr.min().add( w.multiply(min) ), dr.min().add( w.multiply(max) ) );
    }
    
    /**
     * returns DatumRange relative to this, where 0. is the minimum, and 1. is the maximum, but the
     * scaling is done in the log space.
     * For example, rescaleLog( [0.1,1.0], -1, 2 )-> [ 0.01, 10.0 ]
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
     * returns the position within dr, where 0. is the dr.min(), and 1. is dr.max()
     * @param dr a datum range with non-zero width.
     * @param d a datum to normalize with respect to the range.
     * @return a double indicating the normalized datum.
     */
    public static double normalize( DatumRange dr, Datum d ) {
        Units u= dr.getUnits();
        double d0= dr.min().doubleValue( u );
        double d1= dr.max().doubleValue( u );
        double dd= d.doubleValue( u );
        return (dd-d0) / ( d1-d0 );
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
     * Like DatumRange.intesects, but returns a zero-width range when the two do
     * not intersect.  When they do not intersect, the min or max of the first range
     * is returned, depending on whether or not the second range is above or below
     * the first range.  Often this allows for simpler code.
     * @see DatumRange.intersection.
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
     * @param contains sorted list of non-overlapping ranges.  If remove is true, then
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

        DatumRange bounds1 = bounds.get(is);
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
     * @see DatumRange.contains.
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
     */
    public static DatumRange union( Datum d1, Datum d2 ) {
        Units units= d1.getUnits();
        double s1= d1.doubleValue(units);
        double s2= d2.doubleValue(units);
        return new DatumRange( Math.min(s1,s2), Math.max(s1, s2), units );
    }

    /**
     * return the union of a DatumRange and Datum.  If they do not intersect, the
     * range between the two is included as well.
     * @param range
     * @param include a datum to add this this range.  If its the max, then
     * it will be the end of the datum range, not included.
     * @return
     */
    public static DatumRange union( DatumRange range, Datum include ) {
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
     * @param range
     * @param include
     * @return
     */
    public static DatumRange union( DatumRange range, DatumRange include ) {
        Units units= range.getUnits();
        double s11= range.min().doubleValue(units);
        double s12= include.min().doubleValue(units);
        double s21= range.max().doubleValue(units);
        double s22= include.max().doubleValue(units);
        
        return new DatumRange( Math.min(s11,s12), Math.max(s21, s22), units );
        
    }

}
