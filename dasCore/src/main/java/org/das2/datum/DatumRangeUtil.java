/*
 * DatumRangeUtil.java
 *
 * Created on September 16, 2004, 2:35 PM
 */

package org.das2.datum;

import org.das2.util.DasMath;
import org.das2.system.DasLogger;
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
    
    public static class DateDescriptor {
        String date;
        String year;
        String month;
        String day;
        String delim;
        int dateformat;
    }
    
    private int stregex( String string, String regex ) {
        Matcher matcher= Pattern.compile(regex).matcher(string);
        if ( matcher.find() ) {
            return matcher.start();
        } else {
            return -1;
        }
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
            throw new ParseException( "failed attempt to parse int in "+s, 0 );
        }
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
                timearr[NANO]= (int)( Integer.parseInt( m.group(4) ) * ( 100000 / DasMath.exp10( m.group(4).length() ) ));
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
            
            Logger logger= DasLogger.getLogger( DasLogger.SYSTEM_LOG );
            
            this.string= stringIn+" ";
            this.ipos= 0;
            
            ArrayList beforeToUnresolved= new ArrayList();
            ArrayList afterToUnresolved= new ArrayList();
            
            String[] formatCodes= new String[] { "%y", "%m", "%d", "%H", "%M", "%S", "" };
            formatCodes[6]= "%N"; // note %_ms, %_us might be used instead
            String[] digitIdentifiers= new String[] {"YEAR", "MONTH", "DAY", "HOUR", "MINUTE", "SECOND", "NANO" };
            
            boolean isThroughNotTo= false;
            
            //final int YEAR=0;
            //final int MONTH=1;
            //final int DAY=2;
            //final int HOUR=3;
            //final int MINUTE=4;
            //final int SECOND=5;
            //final int NANO=6;
            
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
                    Datum start= TimeUtil.toDatum(ts1);
                    Datum width= DatumUtil.parse( newString.substring(ipos) );
                    Datum stop= start.add(width);
                    CalendarTime tt= new CalendarTime(stop);
                    ts2[DAY]= tt.m_nDom;
                    ts2[MONTH]= tt.m_nMonth;
                    ts2[YEAR]= tt.m_nYear;
                    ts2[HOUR]= tt.m_nHour;
                    ts2[MINUTE]= tt.m_nMinute;
                    ts2[SECOND]= (int)tt.m_nSecond;
                    ts2[NANO]= (int)(( tt.m_nSecond - ts2[SECOND] ) * 1e9);
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
        
    }
    
    public static DatumRange parseTimeRange( String string ) throws ParseException {
        return new TimeRangeParser().parse(string);
    }
    
    
    public static DatumRange parseTimeRangeValid( String s ) {
        try {
            return parseTimeRange(s);
        } catch ( ParseException e ) {
            throw new RuntimeException(e);
        }
    }
    
    /* formats time, supressing trailing zeros.  Time2 is another time that will be displayed alongside time,
     * and may be used when deciding how the time should be formatted.  context is used to describe an external
     * time context that can further make the display of the time more efficient.
     */
    private static String efficientTime( Datum time, Datum time2, DatumRange context ) {
        CalendarTime ts= new CalendarTime(time);
        
        String timeString;
        
        int stopRes= 4;
        if ( TimeUtil.getSecondsSinceMidnight(time)==0. && time.equals(context.max()) ) {
            ts.m_nHour=24;
            ts.m_nDom--;
        }
        
        timeString= ""+ts.m_nHour+":";
        
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
    
    public static String formatTimeRange( DatumRange self ) {
        
        String[] monthStr= new String[] { "Jan", "Feb", "Mar", "Apr", "May", "June", "July", "Aug", "Sep", "Oct", "Nov", "Dec" };
        
        double seconds= self.width().doubleValue(Units.seconds);
        
        CalendarTime ts1= new CalendarTime(self.min());
        CalendarTime ts2= new CalendarTime(self.max());
        
        //        ts1= [ year1, month1, dom1, hour1, minute1, second1, nanos1 ]
        //        ts2= [ year2, month2, dom2, hour2, minute2, second2, nanos2 ]
        
        boolean isMidnight1= TimeUtil.getSecondsSinceMidnight( self.min() ) == 0.;
        boolean isMidnight2= TimeUtil.getSecondsSinceMidnight( self.max() ) == 0.;
        
        boolean isMonthBoundry1= isMidnight1 && ts1.m_nDom == 1;
        boolean isMonthBoundry2= isMidnight2 && ts2.m_nDom == 1;
        
        boolean isYearBoundry1= isMonthBoundry1 && ts1.m_nMonth == 1;
        boolean isYearBoundry2= isMonthBoundry2 && ts2.m_nMonth == 1;
        
        //String toDelim= " \u2013 ";
        String toDelim= " through ";
        if ( isYearBoundry1 && isYearBoundry2 ) {  // no need to indicate month
            if (  ts2.m_nYear-ts1.m_nYear == 1 ) {
                return "" + ts1.m_nYear;
            } else {
                return "" + ts1.m_nYear + toDelim + (ts2.m_nYear-1);
            }
        } else if ( isMonthBoundry1 && isMonthBoundry2 ) { // no need to indicate day of month
            if ( ts2.m_nMonth == 1 ) {
                ts2.m_nMonth=13;
                ts2.m_nYear--;
            }
            if ( ts2.m_nYear == ts1.m_nYear ) {
                if ( ts2.m_nMonth-ts1.m_nMonth == 1 ) {
                    return monthStr[ts1.m_nMonth-1] + " " + ts1.m_nYear;
                } else {
                    return monthStr[ts1.m_nMonth-1]+ toDelim  + monthStr[ts2.m_nMonth-1-1] + " " + ts1.m_nYear;
                }
            } else {
                return monthStr[ts1.m_nMonth-1] + " " + ts1.m_nYear + toDelim
                        + monthStr[ts2.m_nMonth-1-1] + " " + ts2.m_nYear;
            }
        }
        
        if ( isMidnight1 && isMidnight2 ) { // no need to indicate HH:MM
            if ( TimeUtil.getJulianDay( self.max() ) - TimeUtil.getJulianDay( self.min() ) == 1 ) {
                return TimeDatumFormatter.DAYS.format( self.min() );
            } else {
                Datum endtime= self.max().subtract( Datum.create( 1, Units.days ) );
                return TimeDatumFormatter.DAYS.format( self.min() ) + toDelim
                        + TimeDatumFormatter.DAYS.format( endtime );
            }
            
        } else {
            DatumFormatter timeOfDayFormatter;
            
            if ( seconds<1. ) timeOfDayFormatter= TimeDatumFormatter.MILLISECONDS;
            else if ( seconds<60. ) timeOfDayFormatter= TimeDatumFormatter.MILLISECONDS;
            else if ( seconds<3600. ) timeOfDayFormatter= TimeDatumFormatter.SECONDS;
            else timeOfDayFormatter= TimeDatumFormatter.MINUTES;
            
            int maxDay= TimeUtil.getJulianDay(self.max());
            if ( TimeUtil.getSecondsSinceMidnight(self.max())==0 ) maxDay--;  //  want to have 24:00, not 00:00
            if ( maxDay== TimeUtil.getJulianDay(self.min()) ) {
                return TimeDatumFormatter.DAYS.format(self.min())
                + " " + efficientTime( self.min(), self.max(), self )
                + " to " +  efficientTime( self.max(), self.min(), self );
            } else {
                String t1str= efficientTime( self.min(), self.max(), self );
                String t2str= efficientTime( self.max(), self.min(), self );
                return TimeDatumFormatter.DAYS.format( self.min() ) + " " + t1str
                        + " to " + TimeDatumFormatter.DAYS.format( self.max() ) + " " + t2str;
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
    public static List generateList( DatumRange bounds, DatumRange element ) {
        
        ArrayList result= new ArrayList();
        DatumRange dr= element;
        while ( dr.max().gt(bounds.min()) ) {
            result.add(0,dr);
            dr= dr.previous();
        }
        dr= element.next();
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
                    throw new IllegalArgumentException("failed to parse: "+str);
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
                return new DatumRange( d1.convertTo(units), d2.convertTo(units) );
            } else {
                throw new ParseException( "Can't convert parsed unit ("+d1.getUnits()+") to "+units, 0 );
            }
        }
    }
	 
 
	 /**  Temporary Hack until apps are moved over to the new dasQCore --cwp
	  * 
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
        // Version in DasQCore can look for times without UTC appended, this version 
		  // can't
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
                    throw ex;
                }
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
        double s1= DasMath.log10( dr.min().doubleValue(u) );
        double s2= DasMath.log10( dr.max().doubleValue(u) );
        double w= s2 - s1;
        if ( w==0. ) {
            // condition that might cause an infinate loop!  For now let's check for this and throw RuntimeException.
            throw new RuntimeException("width is zero!");
        }
        s2= DasMath.exp10( s1 + max * w ); // danger
        s1= DasMath.exp10( s1 + min * w );
        return new DatumRange( s1, s2, u );
    }
    
    /**
     * returns the position within dr, where 0. is the dr.min(), and 1. is dr.max()
     * @param dr a datum range with non-zero width.
     * @param d a datum to normalize with respect to the range.
     * @return a double indicating the normalized datum.
     */
    public static double normalize( DatumRange dr, Datum d ) {
        return d.subtract(dr.min()).divide(dr.width()).doubleValue(Units.dimensionless);
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
     * Like DatumRange.contains, but includes the end point.  Often this allows for simpler code.
     * @see DatumRange.contains.
     */
    public static boolean sloppyContains(DatumRange context, Datum datum) {
        return context.contains(datum) || context.max().equals(datum);
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
