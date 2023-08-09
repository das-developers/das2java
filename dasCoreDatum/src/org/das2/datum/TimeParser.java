/*
 * TimeParser.java
 *
 * Created on January 27, 2006, 3:51 PM
 *
 *
 */
package org.das2.datum;

import java.util.logging.Level;
import org.das2.datum.TimeUtil.TimeStruct;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.das2.datum.Orbits.OrbitFieldHandler;

/**
 * TimeParser designed to quickly parse strings with a known format.  This parser has been
 * shown to perform around 20 times faster than the discovery parser.
 * 
 * This class is not thread-safe, so clients must make sure that only one thread accesses the class at a time.
 *
 * @author Jeremy
 */
public class TimeParser {

    final static Logger logger = LoggerManager.getLogger("das2.datum.timeparser");

    /**
     * $Y-$m-$dT$H:$M:$S.$(subsec;places=3)Z
     */
    public static final String TIMEFORMAT_Z = "$Y-$m-$dT$H:$M:$S.$(subsec;places=3)Z";
    private static final int AFTERSTOP_INIT = 999;

    /**
     * the beginning of the interval.
     */
    private TimeStruct startTime;
    
    /**
     * the end of the interval.  Note timeWidth should be consistent with this.
     */
    private TimeStruct stopTime;
    
    /**
     * the width of the interval.  Note stopTime should be consistent with this.
     */
    private TimeStruct timeWidth;
    
    /**
     * the width cached as a Datum, to support phaseStart.
     */
    private Datum timeWidthDatum;
    
    /**
     * the context for parsing times.  For example 2014-09-08 can be the context, and then 
     * "11:00" will result in 2014-09-08T11:00.  This is set in the constructor and will not be 
     * mutated after.
     */
    private TimeStruct context;

    /**
     * non-null means someone is parsing.
     */
    private String lock="";
            
    /**
     * keep track of the orbit DatumRange parsed.
     */
    private OrbitDatumRange orbitDatumRange;

    private int ndigits;
    private String[] valid_formatCodes = new String[]{"Y", "y", "j", "m", "d", "H", "M", "S", "milli", "micro", "p", "z", "ignore", "b", "X", "x" };
    private String[] formatName = new String[]{"Year", "2-digit-year", "day-of-year", "month", "day", "Hour", "Minute", "Second", "millisecond", "microsecond",
        "am/pm", "RFC-822 numeric time zone", "ignore", "3-char-month-name", "ignore", "ignore" };
    private int[] formatCode_lengths = new int[]{4, 2, 3, 2, 2, 2, 2, 2, 3, 3, 2, 5, -1, 3, -1, -1 };
    private int[] precision =          new int[]{0, 0, 2, 1, 2, 3, 4, 5, 6, 7,-1,-1, -1, 1, -1, -1 };
    
    /**
     * set of custom handlers to allow for extension
     */
    private Map<String,FieldHandler> fieldHandlers;
    
    private Map<String,FieldHandler> fieldHandlersById;

    /**
     * code for each handler.
     * @see #fc
     */
    private int[] handlers;
    /**
     * positions of each digit, within the string to be parsed.  If position is -1, then we need to
     * compute it along the way.
     */
    private int[] offsets;
    /**
     * length of each digit, or -1 if the field doesn't have a fixed length.  Note 
     * for two digits to be adjacent, they must have fixed length ($Y$m is okay,
     * but $Y$x is not).
     */
    private int[] lengths;
    private int[] shift;  // any shifts to apply to each digit (used typically to make end time inclusive).
    private final String[] delims;
    
    /**
     * the code for each field.
     * @see #handlers
     */
    private String[] fc;
    private String[] qualifiers;
    private final String regex;
    //private String formatString;
    
    private Datum phasestart=null;
            
    /**
     * position in the template where we switch over to stop time digits.
     */
    private int stopTimeDigit=AFTERSTOP_INIT;  // if after stop, then timeWidth is being set.
    
    /**
     * Least significant digit in format.
     *0=year, 1=month, 2=day, 3=hour, 4=min, 5=sec, 6=milli, 7=micro
     */
    private int lsd;
    
    /**
     * keep track of the least significant digit in the start time of the format, for when "end" modifier is used.
     */
    private int startLsd;

    /**
     * return true if the parser has a field.
     * @param field e.g. "x"
     * @return true if the parser has a field.
     */
    public boolean hasField(String field) {
        for (String fc1 : fc) {
            if (field.equals(fc1)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Interface to add custom handlers for strings with unique formats.  For example, the RPWS group had files with
     * two-hex digits indicating the ten-minute interval covered by the file name.  This is also used for orbits.
     * TODO: FieldHandler needs to report its affect on the LSD.  (Autoplot gets versioning).
     */
    public interface FieldHandler {

        /**
         * arguments for the parser are passed in.
         * @param args map of arguments.  $(t,a1=v1,a2=v2,a3=v3)
         * @return null if the string is parseable, an error message otherwise.
         */
        public String configure( Map<String,String> args );

        /**
         * return a regular expression that matches valid field entries.  ".*" can be used to match anything, but this limits use.
         * TODO: where is this used?  I added it because it's easy and I saw a TODO to add it.
         * @return null to match anything, or a regular expression matching valid entries.
         */
        public String getRegex();

        /**
         * parse the field to interpret as a time range.
         * @param fieldContent the field to parse, for example "2014" for $Y
         * @param startTime the current startTime
         * @param timeWidth the current timeWidth
         * @param extra extra data, such as version numbers, are passed out here.
         * @throws ParseException when the field is not consistent with the spec.
         */
        public void parse( String fieldContent, TimeStruct startTime, TimeStruct timeWidth, Map<String,String> extra ) throws ParseException;
        
        /**
         * create a string given the times, when this is possible.  An IllegalArgumentException should be thrown when this is 
         * not possible, but be loose so this can be composed with other field handlers.  For example, imagine the $Y field handler.
         * This should not throw an exception when 2012-03-29 is passed in because it's not 2012-01-01, because the $m and $d might
         * be used later.  However if a time is specified for a year before the first orbit of a spacecraft, then an exception
         * should be thrown because there is an error that the developer is going to have to deal with.
         * 
         * @param startTime
         * @param timeWidth
         * @param length, -1 or the length of the field.
         * @param extra extra data, such as version numbers, are passed in here.
         * @return the string representing the time range specified.
         * @throws IllegalArgumentException
         */
        public abstract String format( TimeStruct startTime, TimeStruct timeWidth, int length, Map<String,String> extra ) throws IllegalArgumentException;

    }
    
    /**
     * handy FieldHandler that ignores the contents.  For example,
     * <pre>tp= TimeParser.create(sagg,"v", TimeParser.IGNORE_FIELD_HANDLER );</pre>
     */
    public static final FieldHandler IGNORE_FIELD_HANDLER= new TimeParser.FieldHandler() {
        String regex;
        
        @Override
        public String configure(Map<String, String> args) {
            regex= args.get("regex");
            return null;
        }
        @Override
        public String getRegex() {
            return regex; // which can be null.
        }
        
        @Override
        public void parse(String fieldContent, TimeStruct startTime, TimeStruct timeWidth, Map<String, String> extra) throws ParseException {
        }
        
        @Override
        public String format(TimeStruct startTime, TimeStruct timeWidth, int length, Map<String, String> extra) throws IllegalArgumentException {
            return null;
        }
    };

    /**
     * return true if the string appears to be an ISO8601 time.  This
     * requires that the string contain a "T" or space and the hours and
     * minutes components.
     * @param exampleTime string like "1992-353T02:00"
     * @return true if the string appears to be an ISO8601 time.
     */
    public static boolean isIso8601String( String exampleTime ) {
        try { 
            iso8601String(exampleTime);
            return true;
        } catch ( IllegalArgumentException ex ) {
            return false;
        }
    }
    
    /**
     * must contain T or space to delimit date and time.
     * @param exampleTime "1992-353T02:00"
     * @return "$Y-$jT$H$M" etc.
     * @throws IllegalArgumentException if the time does not appear to be ISO8601.
     */
    public static String iso8601String(String exampleTime) {
        int i = exampleTime.indexOf("T");
        if (i == -1) {
            i = exampleTime.indexOf(" ");
        }

        String date = null, time = null;
        if (i != -1 && i>5 ) {
            char dateTimeDelim = exampleTime.charAt(i);
            String datePart = exampleTime.substring(0, i);
            boolean hasDelim = !datePart.matches("\\d+");
            char delim = 0;
            if (hasDelim) {
                delim = datePart.charAt(4);
            }
            switch (datePart.length()) {
                case 10:
                    date = "$Y" + delim + "$m" + delim + "$d";
                    break;
                case 9:
                    date = "$Y" + delim + "$j";
                    break;
                case 8:
                    date = hasDelim ? "$Y" + delim + "$j" : "$Y$m$d";
                    break;
                case 7:
                    date = "$Y$j";
                    break;
                default:
                    throw new IllegalArgumentException("unable to identify date format for " + exampleTime);
            }

            String timePart = exampleTime.substring(i + 1);
            boolean addZ= false;
            if (timePart.endsWith("Z")) {
                timePart = timePart.substring(0, timePart.length() - 1); // see below
                addZ= true;
            }
            hasDelim = !timePart.matches("\\d+");
            delim = 0;
            if (hasDelim && timePart.length()>2 ) {
                delim = timePart.charAt(2);
            }
            switch (timePart.length()) {
                case 4:
                    time = "$H$M";
                    break;
                case 5:
                    time = "$H" + delim + "$M";
                    break;
                case 6:
                    time = "$H$M$S";
                    break;
                case 8:
                    time = "$H" + delim + "$M" + delim + "$S";
                    break;
                case 12:
                    time = "$H" + delim + "$M" + delim + "$S.$(subsec,places=3)";
                    break;
                case 15:
                    time = "$H" + delim + "$M" + delim + "$S.$(subsec,places=6)";
                    break;
                default:
                    throw new IllegalArgumentException("unable to identify time format for " + exampleTime);
            }
            if ( addZ ) {
                time += "Z";
            }
            return date + dateTimeDelim + time;

        } else {
            throw new IllegalArgumentException("example time must contain T or space.");
        }
    }

    /**
     * return true if each successive field is nested within the previous,
     * e.g.  $Y$m/$d is nested, but $Y$m/$Y$m$d is not because of the second $Y.
     * @return true if the spec is nested.
     */
    public boolean isNested() {
        int resolution= -9999;
        
        for ( int i=1; i<fc.length; i++ ) {
            if ( handlers[i]>=0 && handlers[i]<8 ) {
                if ( handlers[i]>resolution ) {
                    resolution= handlers[i];
                } else {
                    if ( stopTimeDigit!=AFTERSTOP_INIT ) {
                        resolution= handlers[i]; // disable this check when Y;end is used.
                    } else {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private char startTimeOnly= 0;

    /**
     * true if the flag (startTimeOnly) was set in the spec. This is a hint to clients (FileStorageModel) using the time that
     * it shouldn't infer that the time is bounded.
     * @return
     */
    public boolean isStartTimeOnly() {
        return ( startTimeOnly>0 );
    }

    /**
     * $(subsec;places=6)  "36" &rarr; "36 microseconds"
     */
    public static class SubsecFieldHandler implements TimeParser.FieldHandler {

        int places;
        double microsecondsFactor;
        String format;
        
        @Override
        public String configure(Map<String, String> args) {
            places= Integer.parseInt( args.get("places") );
            if ( places>9 ) throw new IllegalArgumentException("only nine places allowed.");
            microsecondsFactor= Math.pow( 10, (6-places) );          // magic number 6 comes from timeWidth.micros
            format= "%0"+places+"d";
            return null;
        }

        @Override
        public String getRegex() {
            StringBuilder b= new StringBuilder();
            for ( int i=0; i<places; i++ ) b.append("[0-9]");
            return b.toString();
        }

        @Override
        public void parse(String fieldContent, TimeStruct startTime, TimeStruct timeWidth, Map<String, String> extra) throws ParseException {
            double value= Double.parseDouble(fieldContent);
            startTime.micros= (int)( value * microsecondsFactor ); 
            value= value - startTime.micros / microsecondsFactor;
            startTime.nanos= (int)( value * microsecondsFactor * 1000 );
            timeWidth.seconds= 1e-9 * ( value - startTime.nanos / ( microsecondsFactor * 1000 )); //legacy TimeStruct supported double seconds.
            if ( microsecondsFactor>=1. ) {
                timeWidth.micros= (int)( 1*microsecondsFactor );
            } else {
                timeWidth.nanos= (int)( 1 * microsecondsFactor * 1000 );
            }
        }

        @Override
        public String format(TimeStruct startTime, TimeStruct timeWidth, int length, Map<String, String> extra) throws IllegalArgumentException {
            double nn= ( ( startTime.seconds-(int)startTime.seconds ) * ( 1000000/microsecondsFactor ) ) //legacy TimeStruct supported double seconds.
                    + ( startTime.millis * 1000 / microsecondsFactor ) 
                    + ( startTime.micros / microsecondsFactor )
                    + startTime.nanos / ( microsecondsFactor * 1000 );
            return String.format( format, (int)Math.round(nn) ); 
        }
        
    }
    
    /**
     * $(hrinterval;names=a,b,c,d)  "b" &rarr; "06:00/12:00"
     */
    public static class HrintervalFieldHandler implements TimeParser.FieldHandler {

        Map<String,Integer> values;
        Map<Integer,String> revvalues;
        int mult; // multiply by this to get the start hour
        
        @Override
        public String configure(Map<String, String> args) {
            String vs= args.get("values");
            if ( vs==null ) vs= args.get("names"); // some legacy thing
            if ( vs==null ) return "values must be specified for hrinterval";
            String[] values1= vs.split(",",-2);
            mult= 24 / values1.length;
            if ( 24 - mult*values1.length != 0 ) {
                throw new IllegalArgumentException("only 1,2,3,4,6,8 or 12 intervals");
            }
            values= new HashMap();
            revvalues= new HashMap();
            for ( int i=0; i<values1.length; i++ ) {
                values.put( values1[i], i );
                revvalues.put( i, values1[i] );
            }
            return null;
        }

        @Override
        public String getRegex() {
            Iterator<String> vv= values.keySet().iterator();            
            StringBuilder r= new StringBuilder(vv.next());
            while ( vv.hasNext() ) {
                r.append("|").append(vv.next());
            }
            return r.toString();
        }

        @Override
        public void parse(String fieldContent, TimeStruct startTime, TimeStruct timeWidth, Map<String, String> extra) throws ParseException {
            Integer ii= values.get(fieldContent);
            if ( ii==null ) throw new ParseException( "expected one of "+getRegex(),0 );
            int hour= mult * ii;
            startTime.hour= hour;
            timeWidth.hour= mult;
            timeWidth.year= 0;
            timeWidth.month= 0;
            timeWidth.day= 0;
        }

        @Override
        public String format(TimeStruct startTime, TimeStruct timeWidth, int length, Map<String, String> extra) throws IllegalArgumentException {
            String v= revvalues.get(startTime.hour/mult);
            if ( v==null ) throw new IllegalArgumentException("unable to identify enum for hour "+startTime.hour);
            return v;
        }
        
    }
    
    /**
     * regular intervals are numbered.
     * $(periodic;offset=0;start=2000-001;period=P1D)", "0",  "2000-001"
     */
    public static class PeriodicFieldHandler implements TimeParser.FieldHandler {

        int offset;
        int[] start;
        int julday;
        int[] period;
        
        @Override
        public String configure( Map<String, String> args ) {
            String s= args.get("start");
            if ( s==null ) return "periodic field needs start";
            start= DatumRangeUtil.parseISO8601(s);
            julday= TimeUtil.julianDay( start[0], start[1], start[2] );
            start[0]= 0;
            start[1]= 0;
            start[2]= 0;
            s= args.get("offset");
            if ( s==null ) return "periodic field needs offset";
            offset= Integer.parseInt( s );
            s= args.get("period");
            if ( s==null ) return "periodic field needs period";
            if ( !s.startsWith("P") ) {
                if ( s.endsWith("D") ) {
                    throw new IllegalArgumentException("periodic unit for day is d, not D");
                } if ( s.endsWith("d")  ) {
                    s= "P"+s.toUpperCase(); // TODO: this only supports d,H,M,S
                } else {
                    s= "PT" + s.toUpperCase(); 
                }
            }
            try {
                period= DatumRangeUtil.parseISO8601Duration( s );
            } catch ( ParseException ex ) {
                return "unable to parse period: "+s+"\n"+ex.getMessage();
            }
            
            return null;
        }

        @Override
        public String getRegex() {
            return "[0-9]+";
        }

        @Override
        public void parse(String fieldContent, TimeStruct startTime, TimeStruct timeWidth, Map<String, String> extra) throws ParseException {
            int i= Integer.parseInt(fieldContent);
            int addOffset= i-offset;
            int[] t= new int[7];
            int [] limits= new int[] { -1,-1,0,24,60,60,1000000 };
            timeWidth.day= period[2];
            for ( i=6; i>2; i-- ) {
                t[i]= start[i]+addOffset*period[i];
                while ( t[i]>limits[i] ) {
                    t[i-1]++;
                    t[i]-= limits[i];
                }
            }
            timeWidth.year= 0;
            timeWidth.month= 0;
            timeWidth.hour= period[3];
            timeWidth.minute= period[4];
            timeWidth.seconds= period[5];
            timeWidth.micros= period[6]/1000;
            TimeStruct ts= TimeUtil.julianToGregorian( julday + timeWidth.day * addOffset + t[2] );
            startTime.year= ts.year;
            startTime.month= ts.month;
            startTime.day= ts.day;
            startTime.hour= t[3];
            startTime.minute= t[4];
            startTime.seconds= t[5];
            startTime.millis= t[6];
        }

        @Override
        public String format(TimeStruct startTime, TimeStruct timeWidth, int length, Map<String, String> extra) throws IllegalArgumentException {
            int jd= TimeUtil.julianDay( startTime.year, startTime.month, startTime.day );
            if ( period[1]!=0 || period[3]!=0 || period[4]!=0 || period[5]!=0 || period[6]!=0) {
                throw new IllegalArgumentException("under implemented, only integer number of days supported for formatting.");
            }
            int deltad= (int)( Math.floor( ( jd - this.julday ) / (float)period[2] ) ) + offset;
            String result= String.format("%d",deltad);
            if ( length>16 ) {
                throw new IllegalArgumentException("length>16 not supported");
            } else if ( length>-1 ) {
                result= "_________________".substring(0,length-result.length()) + result;
            }
            return result;
        }
        
        
    }

    /**
     * "$Y$m$d-$(enum;values=a,b,c,d)", "20130202-a", "2013-02-02/2013-02-03" 
     */
    public static class EnumFieldHandler implements TimeParser.FieldHandler {

        LinkedHashSet<String> values;
        String id;
        
        @Override
        public String configure( Map<String, String> args ) {
            values= new LinkedHashSet();
            String svalues= args.get("values");
            String[] ss= svalues.split(",",-2);
            if ( ss.length==1 ) {
                String[] ss2= svalues.split("|",-2); // support legacy URIs.
                if ( ss2.length>1 ) {
                    logger.fine("supporting legacy value containing pipes for values");
                    ss= ss2;
                }
            }
            values.addAll(Arrays.asList(ss));
            
            String s= args.get("id");
            if ( s!=null ) id= s; else id="unindentifiedEnum";
            
            return null;
        }

        @Override
        public String getRegex() {
            Iterator<String> it= values.iterator();
            StringBuilder b= new StringBuilder("[").append(it.next());
            while ( it.hasNext() ) {
                b.append("|").append(Pattern.quote(it.next()));
            }
            b.append("]");
            return b.toString();
        }

        @Override
        public void parse(String fieldContent, TimeStruct startTime, TimeStruct timeWidth, Map<String, String> extra) throws ParseException {
            if ( !values.contains(fieldContent) ) {
                throw new ParseException("value is not in enum: "+fieldContent,0);
            }
            extra.put( id, fieldContent );
        }

        @Override
        public String format(TimeStruct startTime, TimeStruct timeWidth, int length, Map<String, String> extra) throws IllegalArgumentException {
            String v= extra.get(id);
            if ( v==null ) {
                throw new IllegalArgumentException( "\"" + id + " is undefined in extras." );
            }
            if ( values.contains(v) ) {
                return v;
            } else {
                throw new IllegalArgumentException(  id + " value is not within enum: "+values );
            }
        }
        
        /**
         * return the possible values.
         * @return the possible values.
         */
        public String[] getValues() {
            return this.values.toArray( new String[this.values.size()] );
        }
        
        public String getId() {
            return this.id;
        }
    }
    
    /**
     * Just skip the field.  This is the default for $v.
     */
    public static class IgnoreFieldHandler implements FieldHandler {

        String regex;
        
        @Override
        public String configure(Map<String, String> args) {
            regex= args.get("regex");
            return null;
        }

        @Override
        public String getRegex() {
            return regex;
        }

        @Override
        public void parse(String fieldContent, TimeStruct startTime, TimeStruct timeWidth, Map<String, String> extra) throws ParseException {
        }

        @Override
        public String format(TimeStruct startTime, TimeStruct timeWidth, int length, Map<String, String> extra) throws IllegalArgumentException {
            return "";
        }
        
    }

    
    /**
     * convert %() and ${} to standard $(), and support legacy modes in one
     * compact place.  Asterisk (*) is replaced with $x.
     * Note, commas may still appear in qualifier lists, and 
     * makeQualifiersCanonical will be called to remove them.
     * @param formatString like %{Y,m=02}*.dat or $(Y;m=02)$x.dat
     * @return formatString containing canonical spec, $() and $x instead of *, like $(Y,m=02)$x.dat
     */
    private static String makeCanonical( String formatString ) {
        boolean wildcard= formatString.contains("*");
        boolean oldSpec= formatString.contains("${");
        Pattern p= Pattern.compile("\\$[0-9]+\\{");
        boolean oldSpec2= p.matcher(formatString).find();
        if ( formatString.startsWith("$") && !wildcard && !oldSpec && !oldSpec2 ) return formatString;
        if ( formatString.contains("%") && !formatString.contains("$") ) {
            formatString= formatString.replaceAll("\\%", "\\$");
        }
        oldSpec= formatString.contains("${"); // it might contain this now.
        if ( oldSpec && !formatString.contains("$(") ) {
            formatString= formatString.replaceAll("\\$\\{", "\\$(");
            formatString= formatString.replaceAll("\\}", "\\)");
        }
        if ( oldSpec2 && !formatString.contains("$(") ) {
            formatString= formatString.replaceAll("\\$([0-9]+)\\{", "\\$$1(");
            formatString= formatString.replaceAll("\\}", "\\)");
        }
        if ( wildcard ) {
            formatString= formatString.replaceAll("\\*", "\\$x");
        }
        return formatString;
    }
    
    /**
     * $(subsec,places=4) --> $(subsec;places=4)
     * $(enum,values=01,02,03,id=foo) --> $(enum;values=01,02,03;id=foo)
     * @param qualifiers
     * @return 
     */
    private static String makeQualifiersCanonical( String qualifiers ) {
        boolean noDelimiters= true;
        for ( int i=0; noDelimiters && i<qualifiers.length(); i++ ) {
            if ( qualifiers.charAt(i)==',' || qualifiers.charAt(i)==';' ) {
                noDelimiters= false;
            }
        }
        if ( noDelimiters ) return qualifiers;
        
        char[] result= new char[qualifiers.length()];
        
        int istart;
        // We know that the first delimiter must be a semicolon.  
        // If it is, then assume the qualifiers are properly formatted.
        result[0]= qualifiers.charAt(0); // '('
        for ( istart=1; istart<qualifiers.length(); istart++ ) {
            char ch= qualifiers.charAt(istart);
            if ( ch==';' ) return qualifiers; // assume the qualifiers are properly formatted
            if ( ch==',' ) {
                result[istart]=';';
                break;
            }
            if ( Character.isLetter(ch) ) {
                result[istart]=ch;
            }
        }

        boolean expectSemi=false;
        for ( int i= qualifiers.length()-1; i>istart; i-- ) {
            result[i]= qualifiers.charAt(i);
            char ch= qualifiers.charAt(i);
            if ( ch=='=' ) expectSemi=true;
            else if ( ch==',' && expectSemi ) {
                result[i]= ';' ;
            } else if ( ch==';' ) {
                expectSemi= false;
            }
        }
        return new String(result);
    }
    
    /**
     * make the constant-expression Regex safe, but remove \Q\E.
     * @param s
     * @return 
     */
    private static String quotePattern( String s ) {
        if ( s.length()==0 ) return s;
        Pattern safe= Pattern.compile("^[\\/a-zA-Z0-9.,_]+");
        Matcher m= safe.matcher(s);
        if ( m.find() ) {
            int i= m.end();
            return s.substring(0,i)+quotePattern(s.substring(i));
        } else {
            return Pattern.quote(s);
        }
    }
    
    /**
     * create a new TimeParser.  
     * @param formatString
     * @param fieldHandlers a map of code to special handlers
     */
    private TimeParser(String formatString, Map<String,FieldHandler> fieldHandlers) {

        // note this is outside of the spec at https://github.com/hapi-server/uri-templates/wiki/Specification
        if ( fieldHandlers.get("o")==null ) {
            fieldHandlers.put("o",new OrbitFieldHandler());
        }

        if ( fieldHandlers.get("subsec")==null ) {
            fieldHandlers.put("subsec",new SubsecFieldHandler());
        }
        
        if ( fieldHandlers.get("hrinterval")==null ) {
            fieldHandlers.put("hrinterval",new HrintervalFieldHandler());
        }

        if ( fieldHandlers.get("periodic")==null ) {
            fieldHandlers.put("periodic",new PeriodicFieldHandler());
        }
        
        if ( fieldHandlers.get("enum")==null ) {
            fieldHandlers.put("enum",new EnumFieldHandler());
        }
        
        if ( fieldHandlers.get("ignore")==null ) {
            fieldHandlers.put("ignore",new IgnoreFieldHandler());
        }

        if ( fieldHandlers.get("x")==null ) {
            fieldHandlers.put("x",new IgnoreFieldHandler());
        }
        
        logger.log(Level.FINE, "new TimeParser({0},...)", formatString);
        
        startTime = new TimeUtil.TimeStruct();
        startTime.year= MIN_VALID_YEAR;
        startTime.month= 1;
        startTime.day= 1;
        startTime.doy= 1;
        startTime.isLocation= true;
        
        stopTime = new TimeUtil.TimeStruct();
        stopTime.isLocation= true;
        stopTime.year= MAX_VALID_YEAR;
        stopTime.month= 1;
        stopTime.day= 1;
        stopTime.doy= 1;
        stopTime.isLocation= true;

        this.fieldHandlers = fieldHandlers;
        
        this.fieldHandlersById= new HashMap();

        formatString= makeCanonical(formatString);
        //this.formatString = formatString;
        
        String[] ss = formatString.split("\\$");
        fc = new String[ss.length];
        qualifiers= new String[ss.length];
        
        String[] delim = new String[ss.length + 1];

        ndigits = ss.length;

        StringBuilder regex1 = new StringBuilder(100);
        regex1.append(quotePattern(ss[0]));//TODO: I thought we did this already.

        lengths = new int[ndigits];
        for (int i = 0; i < lengths.length; i++) {
            lengths[i] = -1; // -1 indicates not known, but we'll figure out as many as we can.
        }
        
        shift= new int[ndigits];

        delim[0] = ss[0];
        for (int i = 1; i < ndigits; i++) {
            int pp = 0;
            String ssi= ss[i];
            while ( ssi.length()>pp && ( Character.isDigit(ssi.charAt(pp)) || ssi.charAt(pp) == '-') ) {
                pp++;
            }
            if (pp > 0) { // Note length ($5Y) is not supported in https://github.com/hapi-server/uri-templates/wiki/Specification
                lengths[i] = Integer.parseInt(ssi.substring(0, pp));
            } else {
                lengths[i] = 0; // determine later by field type
            }

            ssi= makeQualifiersCanonical(ssi);
            
            logger.log( Level.FINE, "ssi={0}", ss[i] );
            if ( ssi.charAt(pp)!='(' ) {
                fc[i] = ssi.substring(pp, pp + 1);
                delim[i] = ssi.substring(pp + 1);
            } else if ( ssi.charAt(pp) == '(') {
                int endIndex = ssi.indexOf(')', pp);
                if ( endIndex==-1 ) {
                    throw new IllegalArgumentException("opening paren but no closing paren in \"" + ssi+ "\"");
                }
                int semi= ssi.indexOf(";", pp );
                if ( semi != -1) {
                    fc[i] = ssi.substring(pp + 1, semi );
                    qualifiers[i]= ssi.substring( semi+1,endIndex );
                } else {
                    fc[i] = ssi.substring(pp + 1, endIndex);
                }
                delim[i] = ssi.substring(endIndex + 1);
            }
        }

        handlers = new int[ndigits];
        offsets = new int[ndigits];

        int pos = 0;
        offsets[0] = pos;

        lsd = -1;
        int lsdMult= 1;
//TODO: We want to add $Y_1XX/$j/WAV_$Y$jT$(H,span=5)$M$S_REC_V01.PKT
        context= new TimeStruct();
        copyTime( startTime, context );
        //context.year = 0;
        //context.month = 1;
        //context.day = 1;
        //context.hour = 0;
        //context.minute = 0;
        //context.seconds = 0;
        //context.micros = 0;
        
        boolean haveHour= false;
        
        for (int i = 1; i < ndigits; i++) {
            if (pos != -1) {
                pos += delim[i - 1].length();
            }
            int handler = 9999;

            for (int j = 0; j < valid_formatCodes.length; j++) {
                if (valid_formatCodes[j].equals(fc[i])) {
                    handler = j;
                    break;
                }
            }

            if ( fc[i].equals("H") ) {
                haveHour= true;
            } else if ( fc[i].equals("p") ) {
                if ( !haveHour ) {
                    throw new IllegalArgumentException("$H must preceed $p");
                }
            }
            
            if (handler == 9999) {
                if ( !fieldHandlers.containsKey(fc[i]) ) {
                    throw new IllegalArgumentException("bad format code: \"" + fc[i] + "\" in \""+ formatString + "\"");
                } else {
                    handler = 100;
                    handlers[i] = 100;
                    offsets[i] = pos;
                    if (lengths[i] < 1 || pos == -1) { // 0->indetermined as well, allows user to force indeterminate
                        pos = -1;
                        lengths[i] = -1;
                    } else {
                        pos += lengths[i];
                    }
                    FieldHandler fh= fieldHandlers.get(fc[i]);
                    String args= qualifiers[i];
                    Map<String,String> argv= new HashMap();
                    if ( args!=null ) {
                        String[] ss2= args.split(";",-2);
                        for (String ss21 : ss2) {
                            int i3 = ss21.indexOf("=");
                            if (i3==-1) {
                                argv.put(ss21.trim(), "");
                            } else {
                                argv.put(ss21.substring(0, i3).trim(), ss21.substring(i3+1).trim());
                            }
                        }
                    }
                    String errm= fh.configure(argv);
                    if ( errm!=null ) {
                        throw new IllegalArgumentException(errm);
                    }
                    
                    String id= argv.get("id");
                    if ( id!=null ) {
                        fieldHandlersById.put( id,fh );
                    }

                }
            } else {
                handlers[i] = handler;
                if (lengths[i] == 0) {
                    lengths[i] = formatCode_lengths[handler];
                }
                offsets[i] = pos;
                if (lengths[i] < 1 || pos == -1) {
                    pos = -1;
                    //lengths[i] = -1; // bugfix: I wonder where this was used.  removed to support "$-1Y $-1m $-1d $H$M"
                } else {
                    pos += lengths[i];
                }
            }

            int span=1;

            String  fieldRegex ;
            if (lengths[i] == -1) {
                 fieldRegex ="(.*)";
            } else {
                String dots = ".........";
                 fieldRegex ="(" + dots.substring(0, lengths[i]) + ")";
            }
            
            if ( qualifiers[i]!=null ) {
                String[] ss2= qualifiers[i].split(";");
                for ( String ss21 : ss2 ) {
                    boolean okay=false;
                    String qual = ss21.trim();
                    if ( qual.equals("startTimeOnly") ) {
                        startTimeOnly= fc[i].charAt(0);
                        okay= true;
                    }
                    int idx= qual.indexOf("=");
                    if ( !okay && idx>-1 ) {
                        String name= qual.substring(0,idx).trim();
                        String val= qual.substring(idx+1).trim();
                        //FieldHandler fh= (FieldHandler) fieldHandlers.get(name);
                        //fh.parse( val, context, timeWidth );
                        switch (name) {
                            case "Y":
                                context.year= Integer.parseInt(val);
                                break;
                            case "m":
                                context.month= Integer.parseInt(val);
                                break;
                            case "d":
                                context.day= Integer.parseInt(val);
                                break;
                            case "j":
                                context.doy= Integer.parseInt(val);
                                break;
                            case "H":
                                context.hour= Integer.parseInt(val);
                                break;
                            case "M":
                                context.minute= Integer.parseInt(val);
                                break;
                            case "S":
                                context.seconds= Integer.parseInt(val);
                                break;
                            case "cadence":
                                span= Integer.parseInt(val);
                                break;
                            case "span":
                                span= Integer.parseInt(val);
                                break;
                            case "delta":
                                span= Integer.parseInt(val); // see https://github.com/hapi-server/uri-templates/wiki/Specification
                                break;
                            case "resolution":
                                span= Integer.parseInt(val);
                                break;
                            case "period":
                                if ( val.startsWith("P") ) {
                                    try {
                                        int[] r= DatumRangeUtil.parseISO8601Duration(val);
                                        for ( int j=0; j<6; j++ ) {
                                            if (r[j]>0 ) {
                                                lsd= j;
                                                lsdMult= r[j];
                                                logger.log(Level.FINER, "lsd is now {0}, width={1}", new Object[]{lsd, lsdMult});
                                                break;
                                            }
                                        }
                                    } catch (ParseException ex) {
                                        Logger.getLogger(TimeParser.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                } else {
                                    char code= val.charAt(val.length()-1);
                                    switch (code) {
                                        case 'Y':
                                            lsd=0;
                                            break;
                                        case 'm':
                                            lsd=1;
                                            break;
                                        case 'd':
                                            lsd=2;
                                            break;
                                        case 'j':
                                            lsd=2;
                                            break;
                                        case 'H':
                                            lsd=3;
                                            break;
                                        case 'M':
                                            lsd=4;
                                            break;
                                        case 'S':
                                            lsd=5;
                                            break;
                                        default:
                                            break;
                                    }
                                    lsdMult= Integer.parseInt(val.substring(0,val.length()-1) );
                                    logger.log(Level.FINER, "lsd is now {0}, width={1}", new Object[]{lsd, lsdMult});
                                }   break;
                            case "id":
                                ; //TODO: orbit plug in handler...
                                break;
                            case "places":
                                ; //TODO: this all needs to be redone...
                                break;
                            case "phasestart":
                                try {
                                    phasestart= TimeUtil.create(val);
                                } catch (ParseException ex) {
                                    logger.log(Level.SEVERE, null, ex);
                                }
                                break;
                            case "shift":
                                shift[i]= Integer.parseInt(val);
                                break;
                            case "":
                                ;
                                break;
                            case "end":
                                if ( stopTimeDigit==AFTERSTOP_INIT ) {
                                    startLsd= lsd;
                                    stopTimeDigit= i;
                                }   break;
                            case "regex":
                                //TODO: Evil regex, where you can craft regexs which will cause Denial of Service (ReDoS)
                                fieldRegex = val;
                                break;
                            default:
                                if ( !fieldHandlers.containsKey(fc[i]) ) {
                                    throw new IllegalArgumentException("unrecognized/unsupported field: "+name + " in "+qual );
                                }   break;
                        }
                        okay= true;
                    } else if ( !okay ) {
                        String name= qual.trim();
                        if ( name.equals("end") ) {
                            if ( stopTimeDigit==AFTERSTOP_INIT ) {
                                startLsd= lsd;
                                stopTimeDigit= i;
                            }
                            okay= true;
                        }
                    }
                    if ( !okay && ( qual.equals("Y") || qual.equals("m") || qual.equals("d") || qual.equals("j") ||
                            qual.equals("H") || qual.equals("M") ||  qual.equals("S")) ) {
                        throw new IllegalArgumentException( String.format( "%s must be assigned an integer value (e.g. %s=1) in %s", qual, qual, ss[i] ) );
                    }
                    if ( !okay ) {
                        if ( !fieldHandlers.containsKey(fc[i]) ) {
                            logger.log(Level.WARNING, "unrecognized/unsupported field:{0} in {1}", new Object[]{qual, ss[i]});
                            //TODO: check plug-in handlers like orbit...
                            //throw new IllegalArgumentException("unrecognized/unsupported field:"+qual+ " in " +ss[i] );
                        }
                    }
                }
            } else {
                // http://sourceforge.net/p/autoplot/bugs/1506/
                if ( fc[i].length()==1 ) {
                    char code= fc[i].charAt(0);
                    int thisLsd= -1;
                    switch (code) {
                        case 'Y':
                            thisLsd=0;
                            break;
                        case 'm':
                            thisLsd=1;
                            break;
                        case 'd':
                            thisLsd=2;
                            break;
                        case 'j':
                            thisLsd=2;
                            break;
                        case 'H':
                            thisLsd=3;
                            break;
                        case 'M':
                            thisLsd=4;
                            break;
                        case 'S':
                            thisLsd=5;
                            break;
                        default:
                            break;
                    }
                    if ( thisLsd==lsd ) {  // allow subsequent repeat fields to reset (T$y$(m,delta=4)/$x_T$y$m$d.DAT)
                        lsdMult= 1;
                    }
                }
            }

            if (handler < 100) {
                if ( precision[handler] > lsd && lsdMult==1 ) {  // omni2_h0_mrg1hr_$Y$(m,span=6)$d_v01.cdf.  Essentially we ignore the $d.
                    lsd = precision[handler];
                    lsdMult= span;
                    logger.log(Level.FINER, "lsd is now {0}, width={1}", new Object[]{lsd, lsdMult});
                }
            }
            
            regex1.append( fieldRegex );
            regex1.append( quotePattern(delim[i]) );

        }

        timeWidth = new TimeStruct();
        switch (lsd) { // see https://sourceforge.net/p/autoplot/bugs/1506/
            case 0:
                timeWidth.year = lsdMult;
                break;
            case 1:
                timeWidth.month = lsdMult;
                break;
            case 2:
                timeWidth.day = lsdMult;
                break;
            case 3:
                timeWidth.hour = lsdMult;
                break;
            case 4:
                timeWidth.minute = lsdMult;
                break;
            case 5:
                timeWidth.seconds = lsdMult;
                break;
            case 6:
                timeWidth.millis = lsdMult;
                break;
            case 7:
                timeWidth.micros = lsdMult;
                break;
            case -1:
                timeWidth.year= 8000;
                break;
            case 100: /* do nothing */ break;  //TODO: handler needs to report it's lsd, if it affects.
        }

        if ( logger.isLoggable(Level.FINE) ) {
            StringBuilder canonical= new StringBuilder( delim[0] );
            for (int i = 1; i < ndigits; i++) { 
                canonical.append("$");
                if ( qualifiers[i]==null ) {
                    canonical.append(fc[i]); 
                } else {
                    canonical.append("(").append(fc[i]).append(";").append(qualifiers[i]).append(")");
                }
                canonical.append(delim[i]); 
            }
            logger.log( Level.FINE, "Canonical: {0}", canonical.toString());
        }
        
        if ( this.timeWidth.year!=0 || this.timeWidth.month!=0 ) {
            this.timeWidthDatum= null;
        } else {
            this.timeWidthDatum= TimeUtil.toDatum(this.timeWidth);
        }
        
        this.delims = delim;
        this.regex = regex1.toString();
    }
    
    /**
     * the last date represented is 9000/01/01
     */
    private static final int MAX_VALID_YEAR = 9000;
    
    /**
     * the earliest date represented is 1000/01/01
     */
    private static final int MIN_VALID_YEAR = 1000;

    /**
     * Provide standard means of indicating this appears to be a spec by
     * looking for something that would assert the year.
     * @param spec
     * @return true if the string appears to be a spec.
     */
    public static boolean isSpec(String spec) {
        spec= makeCanonical( spec );
        spec= spec.replaceAll(",",";");
        if ( spec.contains("$Y")||spec.contains("$y")||spec.contains("$(Y;")||spec.contains("$(y;") ) return true;
        if ( spec.contains(";Y=") ) return true;
        if ( spec.contains("$o;")|| spec.contains("$(o;") ) return true;
        if ( spec.contains("$(periodic;")) return true;
        return false;
    }

    /**
     * Create a TimeParser object, which is the fast time parser for use when a known format specification is used to
     * parse many instances of a formatted string.  For example, this would be used to interpret the times in an text file,
     * but not times entered in a time range GUI to control an axis.  This can also be and is used for filenames,
     * for example omni2_h0_mrg1hr_$Y$(m,span=6)01_v01.cdf.
     *
     * Note field lengths are used when formatting the data, but when parsing often fractional components are accepted.  For
     * example, the format might be "%Y %j %H", and "2012 365 12.003" is accepted.
     *
     * Note also that often $(Y) is used where %{Y} is used.  These are equivalent, and useful when $() interferes with parsing
     * elsewhere.
     *
     * URI_Templates is a public project with translations to Java, JavaScript, and Python, and provides an specification for 
     * this.  See https://github.com/hapi-server/uri-templates/wiki/Specification .
     * <pre>{@code
     *  $[fieldLength]<1-char code>  or
     *  $[fieldLength](<code>)
     *  $[fieldLength](<code>;qualifiers)
     *
     *  fieldLength=0 --> makes field length indeterminate, deliminator must follow.
     *
     *  $Y   4-digit year
     *  $y   2-digit year
     *  $j   3-digit day of year
     *  $m   2-digit month
     *  $b   3-char month name (jan,feb,mar,apr,may,jun,jul,aug,sep,oct,nov,dec.  Sorry, rest of world...)
     *  $d   2-digit day
     *  $H   2-digit hour
     *  $M   2-digit minute
     *  $S   2-digit second
     *  $(milli)  3-digit milliseconds
     *  $(ignore) skip this field
     *  $x   skip this field
     *  $(enum)  skip this field.  If id is specified, then id can be retrieved.
     *  $v   skip this field
     *  $(hrinterval;values=0,1,2,3)  enumeration of part of day
     *  $(subsec;places=6)  fractional seconds (6->microseconds)
     *  $(periodic;offset=0;start=2000-001;period=P1D)
     *
     * Qualifiers:
     *    span=<int>
     *    delta=<int>
     *    Y=2004  Also for Y,m,d,H,M,S
     *
     *   For example:
     *      $(j;Y=2004) means the day-of-year, within the year 2004.
     *      $(H;Y=2004;j=117) means the hour of day 2004-117
     *      $(m;span=6) means the 6-month interval starting at the given month.
     *
     *  }</pre>
     *
     * @param formatString the format string.
     * @return the time parser.
     */
    public static TimeParser create(String formatString) {
        if ( formatString.length()==0 ) {
            throw new IllegalArgumentException("formatString length must be at least one character");
        }
            
        HashMap map= new HashMap();
        map.put("o",new OrbitFieldHandler());
        map.put("v",new IgnoreFieldHandler()); // note this is often replaced.
        return new TimeParser(formatString,map);
    }

    /**
     * create the time parser, and add specialized handlers.  Note the
     * typical route create(formatString) adds handlers for orbits ($o) and version 
     * numbers ($v).
     * 
     * @param formatString like $Y$m$dT$H
     * @param fieldName name for the special field, like "o"
     * @param handler handler for the special field, like OrbitFieldHandler
     * @param moreHandler additional name/handler pairs.
     * @return the configured TimeParser, ready to use.
     */
    public static TimeParser create(String formatString, String fieldName, FieldHandler handler, Object ... moreHandler  ) {
        if ( formatString.length()==0 ) {
            throw new IllegalArgumentException("formatString length must be at least one character");
        }
            
        HashMap map = new HashMap();
        map.put(fieldName, handler);
        if ( moreHandler!=null ) {
            for ( int i=0; i<moreHandler.length; i+=2 ) {
                fieldName=  (String) moreHandler[i];
                handler= (FieldHandler)moreHandler[i+1];
                map.put( fieldName, handler );
            }
        }
        return new TimeParser(formatString, map);
    }

    private double toUs2000(TimeStruct d) {
        int year = d.year;
        int month = d.month;
        int day = d.day;
        int jd = 367 * year - 7 * (year + (month + 9) / 12) / 4 -
                3 * ((year + (month - 9) / 7) / 100 + 1) / 4 +
                275 * month / 9 + day + 1721029;
        int hour = d.hour;
        int minute = d.minute;
        double seconds = d.seconds + hour * (float) 3600.0 + minute * (float) 60.0;
        int mjd1958 = (jd - 2436205);
        double us2000 = (mjd1958 - 15340) * 86400000000. + seconds * 1e6 + d.millis * 1000 + d.micros;
        return us2000;
    }

    private double toUs1980(TimeStruct d) {
        int year = d.year;
        int month = d.month;
        int day = d.day;
        int jd = 367 * year - 7 * (year + (month + 9) / 12) / 4 -
                3 * ((year + (month - 9) / 7) / 100 + 1) / 4 +
                275 * month / 9 + day + 1721029;
        int hour = d.hour;
        int minute = d.minute;
        double seconds = d.seconds + hour * (float) 3600.0 + minute * (float) 60.0;
        double us1980 = (jd - 2436205 - 8035) * 86400000000. + seconds * 1e6 + d.millis * 1e3 + d.micros;
        return us1980;
    }

    /**
     * force the parser to look for delimiters.  This should be called immediately after 
     */
    public void sloppyColumns() {
        this.lengths[0] = -1;
        for (int i = 1; i < this.offsets.length; i++) {
            this.offsets[i] = -1;
            this.lengths[i] = -1;
        //TODO: check for delims
        }
    }

    /**
     * parse the string, which presumably contains a time matching the
     * spec.  A reference to the TimeParser is returned so operations can be
     * chained together:<code>
     *   tp.parse("2014-01-06T02").getTime( Units.us2000 )
     * </code>
     * Since this the TimeParser has a state, it is not safe to use simultaneously
     * by multiple threads.   Each thread should create its own parser.
     * 
     * @param timeString string containing a time
     * @return a reference to this TimeParser object, which now contains the time.
     * @throws ParseException if the string cannot be parsed.
     */
    public TimeParser parse(String timeString) throws ParseException {
        return parse( timeString, null );
    }

    private void copyTime( TimeStruct src, TimeStruct dst ) {
        dst.year = src.year;
        dst.month = src.month;
        dst.day = src.day;
        dst.hour = src.hour;
        dst.minute = src.minute;
        dst.seconds = src.seconds;
        dst.micros = src.micros;
        dst.nanos = src.nanos;
        dst.isLocation= src.isLocation;
    }
    
    /**
     * attempt to parse the string.  The parser itself is returned so that
     * so expressions can be chained like so:
     *    parser.parse("2009-jan").getTimeRange()
     * @param timeString
     * @param extra map that is passed into field handlers
     * @return the TimeParser, call getTimeRange or getTime to get result.
     * @throws ParseException
     */
    public synchronized TimeParser parse(String timeString, Map<String,String> extra ) throws ParseException {
        
        logger.log(Level.FINER, "parse {0}", timeString);
        
        lock= Thread.currentThread().getName();
        
        int offs = 0;
        int len = 0;

        if ( extra==null ) extra= new HashMap();
        
        orbitDatumRange=null;

        TimeStruct time;
        
        time= startTime;
        
        copyTime( context, startTime );

        for (int idigit = 1; idigit < ndigits; idigit++) {
            if ( idigit==stopTimeDigit ) {
                copyTime( startTime, stopTime );
                time= stopTime;
            }
            
            if (offsets[idigit] != -1) {  // note offsets[0] is always known

                offs = offsets[idigit];
            } else {
                offs += len + this.delims[idigit - 1].length();
            }
            if (lengths[idigit] != -1) {
                len = lengths[idigit];
            } else {
                if (this.delims[idigit].equals("")) {
                    if (idigit == ndigits - 1) {
                        len = timeString.length() - offs;
                    } else {
                        throw new IllegalArgumentException("No delimer specified after unknown length field, \"" + formatName[handlers[idigit]] + "\", field number=" + (1 + idigit) + "");
                    }
                } else {
                    while ( offs<timeString.length() && Character.isWhitespace( timeString.charAt(offs) ) ) offs++;
                    if ( offs>=timeString.length() ) {
                        throw new ParseException( "expected delimiter \"" + this.delims[idigit] + "\" but reached end of string", offs);
                    }
                    int i = timeString.indexOf(this.delims[idigit], offs);
                    if (i == -1) {
                        throw new ParseException("expected delimiter \"" + this.delims[idigit] + "\"", offs);
                    }
                    len = i - offs;
                }
            }

            if ( timeString.length()<offs+len ) {
                throw new ParseException( "string is too short: "+timeString, timeString.length() );
            }

            String field= timeString.substring(offs, offs + len).trim();
            
            logger.log(Level.FINEST, "handling {0} with {1}", new Object[]{field, handlers[idigit]});
            
            try {

                if (handlers[idigit] < 10) {
                    int digit;
                    digit= Integer.parseInt(field) + shift[idigit];
                    switch (handlers[idigit]) {
                        case 0:
                            time.year = digit;
                            break;
                        case 1:
                            time.year = digit < 58 ? 2000 + digit : 1900 + digit;
                            break;
                        case 2:
                            time.month = 1;
                            time.day = digit;
                            break;
                        case 3:
                            time.month = digit;
                            break;
                        case 4:
                            time.day = digit;
                            break;
                        case 5:
                            time.hour = digit;
                            break;
                        case 6:
                            time.minute = digit;
                            break;
                        case 7:
                            time.seconds = digit;
                            break;
                        case 8:
                            time.millis = digit;
                            break;
                        case 9:
                            time.micros = digit;
                            break;
                        default:
                            throw new IllegalArgumentException("handlers[idigit] was not expected value (which shouldn't happen)");
                    }
                } else if (handlers[idigit] == 100) {
                    FieldHandler handler = (FieldHandler) fieldHandlers.get(fc[idigit]);
                    handler.parse(timeString.substring(offs, offs + len), time, timeWidth, extra );
                    if ( handler instanceof Orbits.OrbitFieldHandler ) {
                        orbitDatumRange= ((Orbits.OrbitFieldHandler)handler).getOrbitRange();
                    }
                } else if (handlers[idigit] == 10) { // AM/PM -- code assumes hour has been read already
                    char ch = timeString.charAt(offs);
                    if (ch == 'P' || ch == 'p') {
                        if ( time.hour==12 ) {
                            // do nothing
                        } else {
                            time.hour += 12;
                        }
                    } else if (ch == 'A' || ch == 'a') {
                        if ( time.hour==12 ) {
                            time.hour -= 12;
                        } else {
                            // do nothing
                        }
                    }
                } else if (handlers[idigit] == 11) { // TimeZone is not supported, see code elsewhere.
                    int offset;
                    offset= Integer.parseInt(timeString.substring(offs, offs + len));
                    time.hour -= offset / 100;   // careful!

                    time.minute -= offset % 100;
                } else if (handlers[idigit] == 12) { // $(ignore)
                    if ( len>=0 ) {
                        extra.put( "ignore", timeString.substring(offs, offs + len) );
                    }
                } else if (handlers[idigit] == 13) { // month name
                    time.month = TimeUtil.monthNumber(timeString.substring(offs, offs + len));

                } else if (handlers[idigit] == 14) { // "X"
                    if ( len>=0 ) {
                        extra.put( "X", timeString.substring(offs, offs + len) );
                    }
                } else if (handlers[idigit] == 15) { // "x"
                    if ( len>=0 ) {
                        extra.put( "x", timeString.substring(offs, offs + len) );
                    }
                }
            } catch ( NumberFormatException ex ) {
                throw new ParseException( String.format( "fail to parse digit number %d: %s", idigit, field ), offs );
            }

        }
        
        if ( this.phasestart!=null ) {
            if ( timeWidthDatum==null ) {
                logger.warning("phasestart cannot be used for month or year resolution");
            } else {
                Datum start;
                if (startTime.year < 1990) {
                   start= Units.us1980.createDatum(toUs1980(startTime));
                } else {
                   start= Units.us2000.createDatum(toUs2000(startTime));
                }
                Datum s1= this.phasestart.add( timeWidthDatum.multiply( DatumUtil.divp( start.subtract(this.phasestart), timeWidthDatum ) ) );
                if ( !s1.equals(start) ) {
                    throw new ParseException("does not obey phasestart: "+timeString,0 );
                }
                this.startTime= TimeUtil.toTimeStruct(start);
                this.stopTime= TimeUtil.add( this.startTime, this.timeWidth );
            }
        }
        this.lock= "";
        
        return this;
    }

    /**
     * return the pad for the spec, like "underscore" "space" "zero" or "none"
     * For "none", space is returned, and clients allowing special behavior should check for this.
     * @param args
     * @return the char, or (char)0.
     */
    public static char getPad(Map<String, String> args) {
        String spad= args.get("pad");
        if ( spad==null || spad.equals("underscore") ) return '_';
        if ( spad.equals("space") ) {
            return ' ';
        } else if ( spad.equals("zero")) {
            return '0';
        } else if ( spad.equals("none")) {
            return ' ';
        } else if ( spad.length()>1 ) {
            throw new IllegalArgumentException("unrecognized pad: "+spad );
        } else {
            return spad.charAt(0);
        }
    }

    
    private static class FieldSpec {
        String spec=null;  // unparsed spec
        String fieldType= null;
        int length= -1;
        String params= null;
        @Override
        public String toString() {
            return String.valueOf(spec)+String.valueOf(params);
        }
    }

    /**
     * parse field specifications like:
     *   %{milli;cadence=100}
     *   %3{skip}
     * @param spec
     * @return
     */
    private FieldSpec parseSpec(String spec) {
        FieldSpec result= new FieldSpec();
        int i0= spec.charAt(0)=='%' ? 1 : 0;
        result.spec= spec.substring(i0);
        int i1= i0;
        while ( Character.isDigit(spec.charAt(i1)) ) i1++;
        if ( i1>i0 ) {
            result.length= Integer.parseInt(spec.substring(i0,i1));
            i0= i1;
        }
        int isemi = spec.indexOf(';',i0);
        int ibrace = spec.indexOf('}',i0);
        i1 = ibrace;
        if (isemi > -1 && isemi < ibrace) {
            i1 = isemi;
            result.params= spec.substring(isemi,ibrace);
        } else {
            result.params= "";
        }
        String fieldType = spec.substring(1, i1);
        
        result.fieldType= fieldType;
        return result;
        
    }

    /**
     * set the digit with the integer part, and move the fractional part to the
     * less significant digits.  Format should contain just one field,
     * see setDigit( String format, int value ) to break up fields.
     * @param format like "Y"
     * @param value like 2014
     */
    public synchronized void setDigit(String format, double value) {

        TimeStruct time;

        time= startTime;
        
        format= makeCanonical(format);
        if (format.equals("$(ignore)") || format.equals("$X") || format.equals("$x")) return;
        
        if (value < 0) {
            throw new IllegalArgumentException("value must not be negative on field:"+format+" value:"+value );
        }
        String[] ss = format.split("\\$", -2);
        if (ss.length > 2) {
            throw new IllegalArgumentException("multiple fields not supported");
        }
        for (int i = ss.length - 1; i > 0; i--) {
            int digit = (int) value;
            double fp = value - digit;

            switch (ss[i].charAt(0)) {
                case 'Y':
                    time.year = digit;
                    if (TimeUtil.isLeapYear(time.year)) {
                        time.seconds += 366 * 24 * 3600 * fp;
                    } else {
                        time.seconds += 365 * 24 * 3600 * fp;
                    }
                    break;
                case 'y':
                    time.year = digit < 58 ? 2000 + digit : 1900 + digit;
                    if (TimeUtil.isLeapYear(time.year)) {
                        time.seconds += 366 * 24 * 3600 * fp;
                    } else {
                        time.seconds += 365 * 24 * 3600 * fp;
                    }
                    break;
                case 'j':
                    time.month = 1;
                    time.day = digit;
                    time.seconds += 24 * 3600 * fp;
                    break;
                case 'm':
                    time.month = digit;
                    time.seconds += TimeUtil.daysInMonth(time.month, time.year) * 24 * 3600 * fp;
                    break;
                case 'b':  // someone else must parse the month name into 1..12.
                    time.month = digit;
                    break;
                case 'd':
                    time.day = digit;
                    time.seconds += 24 * 3600 * fp;
                    break;
                case 'H':
                    time.hour = digit;
                    time.seconds += 3600 * fp;
                    break;
                case 'M':
                    time.minute = digit;
                    time.seconds += 60 * fp;
                    break;
                case 'S':
                    time.seconds = digit + fp;
                    break;
                case '{':
                    FieldSpec fs= parseSpec(ss[i]);
                    
                    if (fs.fieldType.equals("milli")) {
                        time.millis = digit;
                        time.micros += 1000 * fp;
                        time.seconds += ((1000 * fp) - time.micros) * 1e-6;
                    } else if (fs.fieldType.equals("micro")) {
                        time.micros = digit;
                        time.seconds += fp * 1e-6;
                    } else if (fs.fieldType.equals("ignore")) {
                        // do nothing
                    }
                    break;
                case '(':
                    fs= parseSpec(ss[i]);
                    
                    if (fs.fieldType.equals("milli")) {
                        time.millis = digit;
                        time.micros += 1000 * fp;
                        time.seconds += ((1000 * fp) - time.micros) * 1e-6;
                    } else if (fs.fieldType.equals("micro")) {
                        time.micros = digit;
                        time.seconds += fp * 1e-6;
                    } else if (fs.fieldType.equals("ignore")) {
                        // do nothing
                    }
                    break;
                default:
                    throw new IllegalArgumentException("format code not supported");
            }
        }
    }

    /**
     * Set the digit using the format code.  If multiple digits are found, then
     * the integer provided should be the misinterpreted integer.  For example,
     * if the format is "%Y%m%d", the integer 20080830 is split apart into 
     * 2008,08,30.
     * @param format spec like "%Y%m%d"
     * @param value integer like 20080830.
     * @return
     */
    public synchronized TimeParser setDigit(String format, int value) {

        TimeStruct time= startTime;
        
        String[] ss = format.split("%", -2);
        for (int i = ss.length - 1; i > 0; i--) {
            int mod = 0;
            int digit;
            switch (ss[i].charAt(0)) {
                case 'Y':
                    mod = 10000;
                    digit = value % mod;
                    time.year = digit;
                    break;
                case 'y':
                    mod = 100;
                    digit = value % mod;
                    time.year = digit < 58 ? 2000 + digit : 1900 + digit;
                    break;
                case 'j':
                    mod = 1000;
                    digit = value % mod;
                    time.month = 1;
                    time.day = digit;
                    break;
                case 'm':
                    mod = 100;
                    digit = value % mod;
                    time.month = digit;
                    break;
                case 'b':  // someone else must parse the month name into two-digit month.
                    mod = 100;
                    digit = value % mod;
                    time.month= digit;
                    break;
                case 'd':
                    mod = 100;
                    digit = value % mod;
                    time.day = digit;
                    break;
                case 'H':
                    mod = 100;
                    digit = value % mod;
                    time.hour = digit;
                    break;
                case 'M':
                    mod = 100;
                    digit = value % mod;
                    time.minute = digit;
                    break;
                case 'S':
                    mod = 100;
                    digit = value % mod;
                    time.seconds = digit;
                    break;
                case 'X':
                    break;
                case '{':
                    FieldSpec fs= parseSpec(ss[i]);
                    if (fs.fieldType.equals("milli")) {
                        mod = 1000;
                    } else if ( fs.fieldType.equals("micros") ) {
                        mod = 1000;
                    } else {
                        mod= (int)Math.pow( 10, fs.length );
                    }
                    digit = value % mod;
                    if ( fs.fieldType.equals("milli")) {
                        time.millis = digit;
                    } else if ( fs.fieldType.equals("micros")) {
                        time.micros = digit;
                    } else if ( fs.fieldType.equals("ignore")) {
                        // do nothing
                    }
                    break;
                case '(':
                    fs= parseSpec(ss[i]);
                    if (fs.fieldType.equals("milli")) {
                        mod = 1000;
                    } else if ( fs.fieldType.equals("micros") ) {
                        mod = 1000;
                    } else {
                        mod= (int)Math.pow( 10, fs.length );
                    }
                    digit = value % mod;
                    if ( fs.fieldType.equals("milli")) {
                        time.millis = digit;
                    } else if ( fs.fieldType.equals("micros")) {
                        time.micros = digit;
                    } else if ( fs.fieldType.equals("ignore")) {
                        // do nothing
                    }
                    break;
                default:
                    throw new IllegalArgumentException("format code not supported");
            }
            value = value / mod;
        }
        return this;

    }

    /**
     * This allows for string split into elements to be interpreted here.  This
     * is to add flexibility to external parsers that have partially parsed the
     * number already.
     * examples:<code>
     *   TimeParser p= TimeParser.create("%Y %m %d");
     *   p.setDigit(0,2007).setDigit(1,12).setDigit(2,5).getTime( Units.us2000 );
     *   p.format();  // maybe in the future
     * </code>
     * @param digitNumber, the digit to set (starting with 0).
     * @param digit, value to set the digit.
     * @return the time parser with the digit set.
     * @throws IllegalArgumentException if the digit has a custom field handler
     * @throws IllegalArgumentException if the digit does not exist.
     */
    public synchronized TimeParser setDigit(int digitNumber, int digit) {
        TimeStruct time;
        
        time= startTime;
        switch (handlers[digitNumber + 1]) {
            case 0:
                time.year = digit;
                break;
            case 1:
                time.year = digit < 58 ? 2000 + digit : 1900 + digit;
                break;
            case 2:
                time.month = 1;
                time.day = digit;
                break;
            case 3:
                time.month = digit;
                break;
            case 4:
                time.day = digit;
                break;
            case 5:
                time.hour = digit;
                break;
            case 6:
                time.minute = digit;
                break;
            case 7:
                time.seconds = digit;
                break;
            case 8:
                time.millis = digit;
                break;
            case 9:
                time.micros = digit;
                break;
            case 12:
                break;  // ignore
            case 13:
                time.month = digit;
                break;
            case 14:
                break;  // ignore
        }
        return this;
    }

    /**
     * explicitly set the context for time parsing.  For example,
     * filenames are just $H$M$S.dat, and the context is "Jan 17th, 2015"
     * Note that the context is stored internally as just a start time, so
     * spans (e.g. 3-day) are not supported.
     * @param tr the range
     */
    public synchronized void setContext( DatumRange tr ) {
        this.context= TimeUtil.toTimeStruct(tr.min());
    }
    
    /**
     * return the parsed time in the given units. Here Autoplot
     * Jython code shows how this is used:
     * <code>
     *  from org.virbo.dataset import SemanticOps
     *  tp= TimeParser.create("$Y-$m-$dT$H")
     *  u= SemanticOps.lookupTimeUnits("seconds since 2014-01-01T00:00")
     *  print tp.parse("2014-01-06T02").getTime( u )
     * </code>
     * @param units as in Units.us2000
     * @return the value in the given units.
     */
    public synchronized double getTime(Units units) {
        return Units.us2000.convertDoubleTo(units, toUs2000(startTime));
    }

    /**
     * return the parsed time as a Datum.  For years less than 1990, 
     * Units.us1980 is used, otherwise Units.us2000 is used.
     * @return a datum representing the parsed time.
     */
    public synchronized Datum getTimeDatum() {
        if (startTime.year < 1990) {
            return Units.us1980.createDatum(toUs1980(startTime));
        } else {
            return Units.us2000.createDatum(toUs2000(startTime));
        }
    }

    /**
     * return the limits of the range we can parse.  These limits come from
     * orbit files like "$(o,sc=rbspa-pp)"
     * or from explicit fields like "$(M,Y=1999)"
     * @return
     */
    public DatumRange getValidRange() {
        if ( fieldHandlers.size()==1 && fieldHandlers.get("o") instanceof OrbitFieldHandler ) {
            OrbitFieldHandler ofh= (OrbitFieldHandler)fieldHandlers.get("o");
            try {
                DatumRange d1 = new OrbitDatumRange( ofh.o.getSpacecraft(), ofh.o.first() );
                DatumRange d2= new OrbitDatumRange( ofh.o.getSpacecraft(), ofh.o.last() );
                return DatumRangeUtil.union( d1,d2 );
            } catch (ParseException ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
            }

            return DatumRangeUtil.parseTimeRangeValid( "1000-9000" );

        } else {
            return DatumRangeUtil.parseTimeRangeValid( "1000-9000" );
        }

    }

    /**
     * Returns the implicit interval as a DatumRange.
     * For example, "Jan 1, 2003" would have a getTimeDatum of "Jan 1, 2003 00:00:00",
     * and getDatumRange() would go from midnight to midnight.
     * This will try to create MonthDatumRanges when possible, to keep it abstract,
     * so for example, 
     * <blockquote><pre>{@code
     *tr= tp.getTimeRange()  // "Jan 2015"
     *tr= tr.next()          // "Feb 2015", not 31 days starting Feb 1
     *}</pre></blockquote>
     * 
     * This accesses time, timeWidth, orbitDatumRange, startTime.
     * @return the DatumRange
     */
    public synchronized DatumRange getTimeRange() {
        if ( !lock.equals("") ) throw new IllegalArgumentException("someone is messing with the parser on a different thread "+lock+ " this thread is "+Thread.currentThread().getName() );
        if ( stopTimeDigit==AFTERSTOP_INIT && startTime.day==1 && startTime.hour==0 && startTime.minute==0 && startTime.seconds==0 && startTime.millis==0 && startTime.micros==0 &&
            timeWidth.day==0 && timeWidth.hour==0 && timeWidth.minute==0 && timeWidth.seconds==0 && timeWidth.millis==0 && timeWidth.micros==0 ) { // special code for years.
            TimeStruct lstopTime = startTime.add(timeWidth);
            lstopTime= TimeUtil.carry(lstopTime);
            int[] t1= new int[] { startTime.year, startTime.month, startTime.day, startTime.hour, startTime.minute, (int)startTime.seconds, startTime.millis*1000000 + startTime.micros*1000 + startTime.nanos };
            int[] t2= new int[] { lstopTime.year, lstopTime.month, lstopTime.day, lstopTime.hour, lstopTime.minute, (int)lstopTime.seconds, lstopTime.millis*1000000 + lstopTime.micros*1000 + lstopTime.nanos };
            return new MonthDatumRange( t1, t2 );            
        } else if ( orbitDatumRange!=null ) {
            return orbitDatumRange;
        } else {
            if ( stopTimeDigit<AFTERSTOP_INIT ) {
                if ( this.lsd<=this.startLsd ) {
                    double t1 = toUs2000(startTime);
                    double t2 = toUs2000(stopTime);
                    return new DatumRange(t1, t2, Units.us2000);
                } else {
                    TimeStruct lstartTime;
                    lstartTime = TimeUtil.normalize(stopTime); // begin misguided attempt to subtract time...  Note this does not consider leap seconds...
                    int julianDay= TimeUtil.julianDay( lstartTime.year, lstartTime.month, lstartTime.day );
                    long micros= timeWidth.micros + timeWidth.millis*1000 + (int)timeWidth.seconds * 1000000L + timeWidth.minute * 60000000L + timeWidth.hour * 3600000000L;
                    long lstartTimeMicros= lstartTime.micros + lstartTime.millis*1000 + (int)lstartTime.seconds * 1000000L + lstartTime.minute * 60000000L + lstartTime.hour * 3600000000L;
                    lstartTimeMicros -= micros;
                    julianDay-= timeWidth.day;
                    while ( lstartTimeMicros<0 ) {
                        julianDay-=1;
                        lstartTimeMicros+= 86400000000L;
                    }
                    lstartTime= TimeUtil.julianToGregorian(julianDay);
                    lstartTime.hour= (int)( lstartTimeMicros/3600000000L );
                    lstartTimeMicros-= lstartTime.hour*3600000000L;
                    lstartTime.minute= (int)( lstartTimeMicros/60000000L );
                    lstartTimeMicros-= lstartTime.minute*60000000L;
                    lstartTime.seconds= (int)( lstartTimeMicros/1000000L );
                    lstartTimeMicros-= lstartTime.seconds*1000000L;
                    lstartTime.millis= (int)( lstartTimeMicros/1000L );
                    lstartTimeMicros-= lstartTime.millis*1000L;
                    lstartTime.micros= (int)( lstartTimeMicros );
                    lstartTime.month-= timeWidth.month;
                    while ( lstartTime.month<1 ) {
                        lstartTime.year-= 1;
                        lstartTime.month+=12;
                    }
                    lstartTime.year-= timeWidth.year;
                    double t1 = toUs2000(lstartTime);
                    double t2 = toUs2000(stopTime);
                    return new DatumRange(t1, t2, Units.us2000);
                }
            } else {
                TimeStruct lstopTime = startTime.add(timeWidth);
                double t1 = toUs2000(startTime);
                double t2 = toUs2000(lstopTime);
                return new DatumRange(t1, t2, Units.us2000);
            }
        }
    }

    /**
     * return the end time of the range in units, e.g. Units.us2000
     * @param units
     * @return 
     */
    public double getEndTime(Units units) {
        DatumRange dr= getTimeRange();
        return dr.max().doubleValue(units);
    }

    /**
     * peek at the regular expression used.
     * @return 
     */
    public synchronized String getRegex() {
        return this.regex;
    }

    /**
     * return the formatted range.  This actually returns the range that contains the min
     * of the given range.
     * @param range
     * @return
     */
    public String format( DatumRange range ) {
        return format( range.min(), range.max() );
    }

    /**
     * The TimeParser can be used to format times as well.  The resolution
     * or span implicitly defines the end time.
     * @param start
     * @return formatted string.
     */
    public String format(Datum start) {    
        return format( start, null );
    }
    
    /**
     * The TimeParser can be used to format times as well.  
     * @param start beginning of the interval
     * @param stop null if not needed or implicit.
     * @return formatted string.
     */
    public String format(Datum start, Datum stop) {
        return format(start,stop,new HashMap());
    }
    
    /**
     * move fractional part of timel.seconds into timel.millis and timel.micros
     * components.
     * @param timel the decomposed time. 
     */
    private void normalizeSeconds( TimeStruct timel ) {
        double dextraNanos= ( 1000000000 * ( timel.seconds - (int) timel.seconds ) );
        timel.seconds= (int)timel.seconds;
        
        timel.nanos+= Math.round(dextraNanos);
        
        int micros= (int)( timel.nanos / 1000 );
        timel.micros+= micros;
        timel.nanos-= micros*1000;
        
        int millis= (int)( timel.micros / 1000 );
        timel.millis+= millis;
        timel.micros-= millis*1000;
        
    }
    
    /**
     * The TimeParser can be used to format times as well.  
     * @param start beginning of the interval
     * @param stop null if not needed or implicit.
     * @param extra null or a map of additional identifiers, see enum and x.
     * @return formatted string.
     */
    public String format(Datum start, Datum stop, Map<String,String> extra ) {

        StringBuilder result = new StringBuilder(100);

        int offs = 0;
        int len;

        if ( this.phasestart!=null ) {
            if ( timeWidthDatum==null ) {
                logger.warning("phaseStart cannot be used for month or year resolution");
            } else {
                start= this.phasestart.add( timeWidthDatum.multiply( DatumUtil.divp( start.subtract(this.phasestart), timeWidthDatum ) ) );
            }
        }

        TimeUtil.TimeStruct timel = TimeUtil.toTimeStruct(start);
        TimeUtil.TimeStruct timeWidthl= new TimeUtil.TimeStruct();        
        copyTime( timeWidth, timeWidthl ); // make a local copy in case future versions allow variable time widths.
        extra= new HashMap(extra);

        TimeUtil.TimeStruct stopTimel;
        if ( stop==null ) {
            if ( timeWidth.year==MAX_VALID_YEAR-MIN_VALID_YEAR ) { // orbits and other strange times
                stopTimel= timel;
            } else {
                stopTimel= TimeUtil.add( timel, timeWidth );
            }
        } else {
            stopTimel= TimeUtil.toTimeStruct(stop);
        }
        normalizeSeconds(stopTimel);
        normalizeSeconds(timel);

        NumberFormat[] nf = new NumberFormat[5];
        nf[2] = new DecimalFormat("00");
        nf[3] = new DecimalFormat("000");
        nf[4] = new DecimalFormat("0000");

        for (int idigit = 1; idigit < ndigits; idigit++) {
            if ( idigit==stopTimeDigit ) {
                timel= stopTimel;
            }
            
            result.insert(offs, this.delims[idigit - 1]);
            if (offsets[idigit] != -1) {  // note offsets[0] is always known

                offs = offsets[idigit];
            } else {
                offs += this.delims[idigit - 1].length();
            }
            if (lengths[idigit] != -1) {
                len = lengths[idigit];
            } else {
                len = -9999;  // the field handler will tell us.

            }
            if (handlers[idigit] < 10) {
                String qual= qualifiers[idigit];
                int digit;
                int span=1;
                if ( qual!=null ) {
                    Pattern p= Pattern.compile("span=(\\d+)"); // TODO: multiple qualifiers
                    Matcher m= p.matcher(qual);
                    if ( m.matches() ) {
                        span= Integer.parseInt(m.group(1));
                    }
                    p= Pattern.compile("delta=(\\d+)"); // TODO: multiple qualifiers
                    m= p.matcher(qual);
                    if ( m.matches() ) {
                        span= Integer.parseInt(m.group(1));
                    }
                }
                switch (handlers[idigit]) {
                    case 0:
                        digit = timel.year;
                        break;
                    case 1:
                        digit = (timel.year < 2000) ? timel.year - 1900 : timel.year - 2000;
                        break;
                    case 2:
                        digit = TimeUtil.dayOfYear(timel.month, timel.day, timel.year);
                        break;
                    case 3:
                        digit = timel.month;
                        break;
                    case 4:
                        digit = timel.day;
                        break;
                    case 5:
                        digit = timel.hour;
                        break;
                    case 6:
                        digit = timel.minute;
                        break;
                    case 7:
                        digit = (int) timel.seconds;
                        break;
                    case 8:
                        digit = timel.millis;
                        break;
                    case 9:
                        digit = timel.micros;
                        break;
                    default:
                        throw new RuntimeException("shouldn't get here");
                }
                if ( span>1 ) {
                    if ( handlers[idigit]>0 && handlers[idigit]<5 ) {
                        logger.fine("uh-oh, span used on ordinal like month, day.  Just leave it alone.");
                    } else {
                        digit= ( digit / span ) * span;
                    }
                }
                if ( len<0 ) {
                    String ss= String.valueOf(digit);
                    result.insert(offs, ss);
                    offs+= ss.length();
                } else {
                    result.insert(offs, nf[len].format(digit));
                    offs += len;
                }

            } else if (handlers[idigit] == 13) { // month names

                result.insert(offs, TimeUtil.monthNameAbbrev(timel.month));
                offs += len;

            } else if (handlers[idigit] == 12 || handlers[idigit]==14 ) { // ignore
                throw new RuntimeException("cannot format spec containing ignore");

            } else if (handlers[idigit] == 100) {
                if ( fc[idigit].equals("v") ) { // kludge for version.  TODO: This can probably use the code below now.
                    String ins= "00";
                    if ( len>-1 ) {
                        if ( len>20 ) throw new IllegalArgumentException("version lengths>20 not supported");
                        ins= "00000000000000000000".substring(0,len);
                    }
                    result.insert( offs, ins );
                    offs+= ins.length();
                } else {
                    FieldHandler fh1= fieldHandlers.get(fc[idigit]);
                    TimeUtil.TimeStruct timeEnd = stopTimel;
                    String ins= fh1.format( timel, TimeUtil.subtract(timeEnd, timel), len, extra );
                    TimeUtil.TimeStruct startTimeTest= new TimeUtil.TimeStruct();
                    copyTime( timel, startTimeTest );
                    TimeUtil.TimeStruct timeWidthTest= new TimeUtil.TimeStruct();
                    copyTime( timeWidthl, timeWidthTest );
                    try {
                        fh1.parse( ins, startTimeTest, timeWidthTest, extra );
                        copyTime( startTimeTest, timel );
                        copyTime( timeWidthTest, timeWidthl );
                        copyTime( TimeUtil.add( timel, timeWidthl ), stopTimel );
                        
                    } catch (ParseException ex) {
                        Logger.getLogger(TimeParser.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if ( len>-1 && ins.length()!=len ) {
                        throw new IllegalArgumentException("length of fh is incorrect, should be "+len+", got \""+ins+"\"");
                    }
                    result.insert( offs, ins );
                    offs+= ins.length();
                }

            } else if (handlers[idigit] == 10) {
                throw new RuntimeException("AM/PM not supported");

            } else if (handlers[idigit] == 11) {
                throw new RuntimeException("Time Zones not supported");
            } //TODO: $x?
        }
        result.insert(offs, this.delims[ndigits - 1]);
        return result.toString().trim();
    }
    
    /**
     * return the field handler for the id.  For example, enum
     * returns the field handler handling enumerations.  Note there 
     * is currently only one field handler for each type, so for example
     * two enumerations are not allowed.
     * 
     * @param code
     * @return the field handler.
     */    
    public FieldHandler getFieldHandlerByCode( String code ) {
        return fieldHandlers.get(code);
    }
    /**
     * return the field handler for the id.  For example, enum
     * returns the field handler handling enumerations.  Note there 
     * is currently only one field handler for each type, so for example
     * two enumerations are not allowed.
     * 
     * @param id the field handler id
     * @return the field handler.
     */    
    public FieldHandler getFieldHandlerById( String id ) {
        return fieldHandlersById.get(id);
    }
    
    @Override
    public String toString() {
        StringBuilder result= new StringBuilder();
        for ( int i=0;i<this.fc.length; i++ ) {
            if ( this.fc[i]!=null ) result.append("$").append( this.fc[i]);
            result.append( this.delims[i] );
        }
        return result.toString();
    }

    static boolean testTimeParser1( String spec, String test, String norm ) throws Exception {
        TimeParser tp= TimeParser.create(spec);
        DatumRange dr= tp.parse(test).getTimeRange();
        DatumRange drnorm= org.das2.datum.DatumRangeUtil.parseTimeRangeValid(norm);

        if ( !dr.equals(drnorm) ) {
            tp= TimeParser.create(spec);
            dr= tp.parse(test).getTimeRange();
            throw new IllegalStateException("ranges do not match: "+spec + " " +test + "--> " + dr + ", should be "+norm );
        }
        return true;
    }
    
    public static void main( String[] aa ) throws Exception {
        TimeParser tp;
        //tp= TimeParser.create( "$Y-$m-$dT$H:$M:$S.$(subsec,places=9)" );
        //System.err.println( "tpf: " + tp.format( Units.cdfTT2000.parse( "2016-05-05T12:54:54.002232668") ) );
        tp= TimeParser.create( "$Y-$m-$(d,phasestart=2019-05-12,delta=7)" );
        System.err.println( "tpf: " + tp.format( Units.cdfTT2000.parse( "2019-05-17T00:00Z") ) );
        System.err.println( "tpf: " + tp.format( Units.cdfTT2000.parse( "2019-05-04T00:00Z") ) );
        testTimeParser();
    }

    /**
     * test time parsing when the format is known.  This time parser is much faster than the time parser of Test009, which must
     * infer the format as it parses.
     * @throws Exception
     */
    public static void testTimeParser() throws Exception {
        LoggerManager.getLogger("datum.timeparser").setLevel(Level.ALL);
        logger.addHandler( new ConsoleHandler() );
        logger.getHandlers()[0].setLevel(Level.ALL);
        org.das2.datum.DatumRangeUtil.parseTimeRangeValid("2000-022/P1D");
        System.err.println( makeCanonical("$Y-$3{J}") );
        testTimeParser1( "$Y-$m-$d $H:$M $p", "2019-05-03 12:00 PM", "2019-05-03T12:00/PT1M");
        testTimeParser1( "$Y-$m-$d $H:$M $p", "2019-05-03 12:00 AM", "2019-05-03T00:00/PT1M");
        //testTimeParser1( "$Y-$m-$d $p $H", "2019-05-03 AM 12", "2019-05-03T00:00/PT1H");
        testTimeParser1( "$Y$m$d-$(Y,end)$m$d", "20130202-20140303", "2013-02-02/2014-03-03" );
        testTimeParser1( "$Y$m$d-$(d,end)", "20130202-13", "2013-02-02/2013-02-13" );
        testTimeParser1( "$(periodic;offset=0;start=2000-001;period=P1D)", "0",  "2000-001");
        testTimeParser1( "$(periodic;offset=0;start=2000-001;period=P1D)", "20", "2000-021");        
        testTimeParser1( "$(periodic,offset=2285,start=2000-346,period=P27D)", "1", "1832-02-08/P27D");
        testTimeParser1( "$(periodic;offset=2285;start=2000-346;period=P27D)", "2286", "2001-007/P27D");
        testTimeParser1( "$Y-$m-$dT$H:$M:$S.$(subsec,places=6)", "2000-01-01T00:00:00.000001", "2000-001T00:00:00.000001/PT.000001S");
        testTimeParser1( "$Y-$m-$dT$H:$M:$S.$(subsec,places=6)", "2000-01-01T00:00:05.000001", "2000-001T00:00:05.000001/PT.000001S");
        testTimeParser1( "$Y-$m-$dT$H:$M:$S.$(subsec,places=9)", "2000-01-01T00:00:05.000001001", "2000-001T00:00:05.000001001/PT.000000001S");
        testTimeParser1( "$Y-$m-$(d,phasestart=2019-05-12,delta=7)", "2019-05-03", "2019-04-28T00:00Z/P7D");
        TimeParser tp= TimeParser.create("$Y$m$d_v$v.dat");
        System.err.println( tp.parse("20130618_v4.05.dat").getTimeRange() );
        System.err.println( makeCanonical( "%Y-%m-%dT%H:%M:%S.%{milli}Z" ) );
    }
}
