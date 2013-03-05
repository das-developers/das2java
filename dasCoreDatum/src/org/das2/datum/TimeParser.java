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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.das2.datum.Orbits.OrbitFieldHandler;

/**
 * TimeParser designed to quickly parse strings with a specified format.  This parser has been
 * shown to perform around 20 times faster than that the generalized parser.
 *
 * @author Jeremy
 */
public class TimeParser {

    final static Logger logger = LoggerManager.getLogger("datum.timeparser");
    /**
     * %Y-%m-%dT%H:%M:%S.%{milli}Z
     */
    public static final String TIMEFORMAT_Z = "%Y-%m-%dT%H:%M:%S.%{milli}Z";

    TimeStruct time;
    TimeStruct timeWidth;
    TimeStruct context;

    /**
     * keep track of the orbit DatumRange parsed.
     */
    OrbitDatumRange orbitDatumRange;

    int ndigits;
    String[] valid_formatCodes = new String[]{"Y", "y", "j", "m", "d", "H", "M", "S", "milli", "micro", "p", "z", "ignore", "b", "X", };
    String[] formatName = new String[]{"Year", "2-digit-year", "day-of-year", "month", "day", "Hour", "Minute", "Second", "millisecond", "microsecond",
        "am/pm", "RFC-822 numeric time zone", "ignore", "3-char-month-name", "ignore", };
    int[] formatCode_lengths = new int[]{4, 2, 3, 2, 2, 2, 2, 2, 3, 3, 2, 5, -1, 3, -1 };
    int[] precision = new int[]{0, 0, 2, 1, 2, 3, 4, 5, 6, 7, -1, -1, -1, 1, -1, };
    int[] handlers;
    
    /**
     * set of custom handlers to allow for extension
     */
    Map<String,FieldHandler> fieldHandlers;
    
    /**
     * positions of each digit, within the string to be parsed.  If position is -1, then we need to
     * compute it along the way.
     */
    int[] offsets;
    int[] lengths;
    String[] delims;
    String[] fc;
    String[] qualifiers;
    String regex;
    String formatString;
    /**
     * Least significant digit in format.
     *0=year, 1=month, 2=day, 3=hour, 4=min, 5=sec, 6=milli, 7=micro
     */
    int lsd;

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
         * @return a regular expression matching valid entries.
         */
        public String getRegex();

        /**
         * parse the field to interpret as a time range.
         * @param fieldContent
         * @param startTime
         * @param timeWidth
         * @param extra extra data, such as version numbers, are passed out here.
         * @throws ParseException
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
     * tp= TimeParser.create(sagg,"v", TimeParser.IGNORE_FIELD_HANDLER );
     */
    public static FieldHandler IGNORE_FIELD_HANDLER= new TimeParser.FieldHandler() {
        public String configure(Map<String, String> args) {
            return null;
        }
        public String getRegex() {
            return null;
        }
        public void parse(String fieldContent, TimeStruct startTime, TimeStruct timeWidth, Map<String, String> extra) throws ParseException {
        }
        public String format(TimeStruct startTime, TimeStruct timeWidth, int length, Map<String, String> extra) throws IllegalArgumentException {
            return null;
        }
    };

    /**
     * must contain T or space to delimit date and time.
     * @param exampleTime "1992-353T02:00"
     * @return "%Y-%jT%H%M" etc.
     */
    public static String iso8601String(String exampleTime) {
        int i = exampleTime.indexOf("T");
        if (i == -1) {
            i = exampleTime.indexOf(" ");
        }
        char dateTimeDelim = exampleTime.charAt(i);

        String date = null, time = null;
        if (i != -1) {
            String datePart = exampleTime.substring(0, i);
            boolean hasDelim = !datePart.matches("\\d+");
            char delim = 0;
            if (hasDelim) {
                delim = datePart.charAt(4);
            }
            switch (datePart.length()) {
                case 10:
                    date = "%Y" + delim + "%m" + delim + "%d";
                    break;
                case 9:
                    date = "%Y" + delim + "%j";
                    break;
                case 8:
                    date = hasDelim ? "%Y" + delim + "%j" : "%Y%m%d";
                    break;
                case 7:
                    date = "%Y%j";
                    break;
                default:
                    throw new IllegalArgumentException("unable to identify date format for " + exampleTime);
            }

            String timePart = exampleTime.substring(i + 1);
            if (timePart.endsWith("Z")) {
                timePart = timePart.substring(0, timePart.length() - 1); // see below
            }
            hasDelim = !timePart.matches("\\d+");
            delim = 0;
            if (hasDelim) {
                delim = timePart.charAt(2);
            }
            switch (timePart.length()) {
                case 4:
                    time = "%H%M";
                    break;
                case 5:
                    time = "%H" + delim + "%M";
                    break;
                case 6:
                    time = "%H%M%S";
                    break;
                case 8:
                    time = "%H" + delim + "%M" + delim + "%S";
                    break;
                case 12:
                    time = "%H" + delim + "%M" + delim + "%S.%{milli}";
                    break;
                case 15:
                    time = "%H" + delim + "%M" + delim + "%S.%{milli}%{micro}";
                    break;
                default:
                    throw new IllegalArgumentException("unable to identify time format for " + exampleTime);
            }
            if (timePart.endsWith("Z")) {
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
                    return false;
                }
            }
        }
        return true;
    }

    char startTimeOnly= 0;

    /**
     * true if the flag (startTimeOnly) was set in the spec. This is a hint to clients (FileStorageModel) using the time that
     * it shouldn't infer that the time is bounded.
     * @return
     */
    public boolean isStartTimeOnly() {
        return ( startTimeOnly>0 );
    }


    /**
     * create a new TimeParser.  
     * @param formatString
     * @param fieldHandlers a map of special handlers
     */
    private TimeParser(String formatString, Map<String,FieldHandler> fieldHandlers) {

        logger.fine("new TimeParser("+formatString+",...)");
        
        time = new TimeUtil.TimeStruct();
        this.fieldHandlers = fieldHandlers;
        this.formatString = formatString;

        if ( formatString.contains("$") && !formatString.contains("%") ) {
            formatString= formatString.replaceAll("\\$", "%");
        }

        if ( formatString.contains(".*") ) {
            formatString= formatString.replaceAll("\\.\\*", "\\%\\{ignore\\}");
        }
        //if ( formatString.contains("%(v,sep)") ) { // it would be nice to clean up this implementation.  The following wasn't needed.
        //    formatString= formatString.replaceAll("\\%\\(v,sep\\)", "%{v,sep}");
        //}
        
        String[] ss = formatString.split("%");
        fc = new String[ss.length];
        qualifiers= new String[ss.length];
        
        String[] delim = new String[ss.length + 1];

        ndigits = ss.length;

        StringBuilder regex1 = new StringBuilder(100);
        regex1.append(ss[0].replaceAll("\\+","\\\\+"));//TODO: I thought we did this already.

        lengths = new int[ndigits];
        for (int i = 0; i < lengths.length; i++) {
            lengths[i] = -1; // -1 indicates not known, but we'll figure out as many as we can.

        }

        delim[0] = ss[0];
        for (int i = 1; i < ndigits; i++) {
            int pp = 0;
            while (Character.isDigit(ss[i].charAt(pp)) || ss[i].charAt(pp) == '-') {
                pp++;
            }
            if (pp > 0) {
                lengths[i] = Integer.parseInt(ss[i].substring(0, pp));
            } else {
                lengths[i] = 0; // determine later by field type
            }

            logger.log( Level.FINE, "ss[i]={0}", ss[i] );
            if (ss[i].charAt(pp) != '{' && ss[i].charAt(pp)!='(' ) {
                fc[i] = ss[i].substring(pp, pp + 1);
                delim[i] = ss[i].substring(pp + 1);
            } else if ( ss[i].charAt(pp) == '{') {
                int endIndex = ss[i].indexOf('}', pp);
                int comma = ss[i].indexOf(",", pp);
                int semi= ss[i].indexOf(";", pp );
                if ( comma==-1 || semi>-1 && semi<comma ) comma= semi;
                if (comma != -1) {
                    fc[i] = ss[i].substring(pp + 1, comma);
                    qualifiers[i]= ss[i].substring(comma+1,endIndex);
                } else {
                    fc[i] = ss[i].substring(pp + 1, endIndex);
                }
                delim[i] = ss[i].substring(endIndex + 1);
            } else if ( ss[i].charAt(pp) == '(') {
                int endIndex = ss[i].indexOf(')', pp);
                int comma = ss[i].indexOf(",", pp);
                int semi= ss[i].indexOf(";", pp );
                if ( comma==-1 || semi>-1 && semi<comma ) comma= semi;
                if (comma != -1) {
                    fc[i] = ss[i].substring(pp + 1, comma);
                    qualifiers[i]= ss[i].substring(comma+1,endIndex);
                } else {
                    fc[i] = ss[i].substring(pp + 1, endIndex);
                }
                delim[i] = ss[i].substring(endIndex + 1);
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
        context.year = 0;
        context.month = 1;
        context.day = 1;
        context.hour = 0;
        context.minute = 0;
        context.seconds = 0;
        context.micros = 0;
        
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

            if (handler == 9999) {
                if (fieldHandlers == null || !fieldHandlers.containsKey(fc[i])) {
                    throw new IllegalArgumentException("bad format code: \"" + fc[i] + "\"");
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
                        String[] ss2= args.split(",",-2);
                        for ( int i2=0; i2<ss2.length; i2++ ) {
                            int i3= ss2[i2].indexOf("=");
                            if ( i3==-1 ) {
                                argv.put(ss2[i2].trim(),"");
                            } else {
                                argv.put(ss2[i2].substring(0,i3).trim(),ss2[i2].substring(i3+1).trim());
                            }
                        }
                    }
                    String errm= fh.configure(argv);
                    if ( errm!=null ) {
                        throw new IllegalArgumentException(errm);
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

            if ( qualifiers[i]!=null ) {
                String[] ss2= qualifiers[i].split(";");
                if ( ss2.length==1 && ss2[0].split(",").length>1 ) {
                    System.err.println( "--------------------------------------------------"); // logger okay
                    System.err.println( "maybe use semicolons instead of commas in template"); // logger okay
                    System.err.println( "--------------------------------------------------"); // logger okay
                    logger.fine("maybe use semicolons instead of commas in template");
                }
                for ( int i2=0; i2<ss2.length; i2++ ) {
                    boolean okay=false;
                    String qual= ss2[i2].trim();
                    
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
                        if ( name.equals("Y") ) context.year= Integer.parseInt(val);
                        else if ( name.equals("m") ) context.month= Integer.parseInt(val);
                        else if ( name.equals("d") ) context.day= Integer.parseInt(val);
                        else if ( name.equals("j") ) context.doy= Integer.parseInt(val);
                        else if ( name.equals("H") ) context.hour= Integer.parseInt(val);
                        else if ( name.equals("M") ) context.minute= Integer.parseInt(val);
                        else if ( name.equals("S") ) context.seconds= Integer.parseInt(val);
                        else if ( name.equals("cadence") ) span= Integer.parseInt(val);
                        else if ( name.equals("span") ) span= Integer.parseInt(val);
                        else if ( name.equals("resolution") ) span= Integer.parseInt(val);
                        else if ( name.equals("id") ) ; //TODO: orbit plug in handler...
                        else {
                            if ( !fieldHandlers.containsKey(fc[i]) ) {
                                throw new IllegalArgumentException("unrecognized/unsupported field: "+name + " in "+qual );
                            }
                        }
                        okay= true;
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
            }

            if (handler < 100) {
                if ( precision[handler] > lsd && lsdMult==1 ) {  // omni2_h0_mrg1hr_$Y$(m,span=6)$d_v01.cdf.  Essentially we ignore the $d.
                    lsd = precision[handler];
                    lsdMult= span;
                }
            }

            String dots = ".........";
            if (lengths[i] == -1) {
                regex1.append("(.*)");
            } else {
                regex1.append("(").append(dots.substring(0, lengths[i])).append(")");
            }
            regex1.append(delim[i].replaceAll("\\+","\\\\+"));

        }

        timeWidth = new TimeStruct();
        switch (lsd) {
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
            case 100: /* do nothing */ break;  //TODO: handler needs to report it's lsd, if it affects.
        }

        this.delims = delim;
        this.regex = regex1.toString();
    }

    /**
     * provide standard means of indicating this appears to be a spec.
     * @param spec
     * @return
     */
    public static boolean isSpec(String spec) {
        if ( spec.contains("%") && !spec.contains("$") ) {
            spec= spec.replaceAll("%","$");
            if ( spec.contains("{") && !spec.contains("(") ) {
                spec= spec.replaceAll("\\{","(");
                spec= spec.replaceAll("\\}",")");
            }
        }
        if ( spec.contains("$Y")||spec.contains("$y") ) return true;
        if ( spec.contains(";Y=") ) return true;
        if ( spec.contains(",Y=") ) return true; //yay, sloppy specs!
        if ( spec.contains("$o")|| spec.contains("$(o,") ) return true;
        return false;
    }

    /**
     * Create a TimeParser object, which is the fast time parser for use when a known format specification is used to
     * parse many instances of a formatted string.  For example, this would be used to interpret the times in an text file,
     * but not times entered in a time range GUI to control an axis.  This can also be and is used for filenames,
     * for example omni2_h0_mrg1hr_%Y%(m,span=6)01_v01.cdf.
     *
     * Note field lengths are used when formatting the data, but when parsing often fractional components are accepted.  For
     * example, the format might be "%Y %j %H", and "2012 365 12.003" is accepted.
     *
     * Note also that often $(Y) is used where %{Y} is used.  These are equivalent, and useful when %{} interferes with parsing
     * elsewhere.
     *
     * <pre>
     *  %[fieldLength]<1-char code>  or
     *  %[fieldLength]{<code>}
     *  %[fieldLength]{<code>;qualifiers}
     *
     *  fieldLength=0 --> makes field length indeterminate, deliminator must follow.
     *
     *  %Y   4-digit year
     *  %y   2-digit year
     *  %j   3-digit day of year
     *  %m   2-digit month
     *  %b   3-char month name (jan,feb,mar,apr,may,jun,jul,aug,sep,oct,nov,dec.  Sorry, rest of world...)
     *  %d   2-digit day
     *  %H   2-digit hour
     *  %M   2-digit minute
     *  %S   2-digit second
     *  %{milli}  3-digit milliseconds
     *  %{ignore} skip this field
     *
     * Qualifiers:
     *    span=<int>
     *    Y=2004  Also for Y,m,d,H,M,S
     *
     *   For example:
     *      %{j,Y=2004} means the day-of-year, within the year 2004.
     *      %{H,Y=2004,j=117} means the hour of day 2004-117
     *      %{m,span=6} means the 6-month interval starting at the given month.
     *
     *  </pre>
     *
     */
    public static TimeParser create(String formatString) {
        HashMap map= new HashMap();
        if ( map.get("o")==null ) {
            map.put("o",new OrbitFieldHandler());
        }
        return new TimeParser(formatString,map);
    }

    public static TimeParser create(String formatString, String fieldName, FieldHandler handler, Object ... moreHandler  ) {
        HashMap map = new HashMap();
        map.put(fieldName, handler);
        if ( moreHandler!=null ) {
            for ( int i=0; i<moreHandler.length; i+=2 ) {
                fieldName=  (String) moreHandler[i];
                handler= (FieldHandler)moreHandler[i+1];
                map.put( fieldName, handler );
            }
        }
        if ( map.get("o")==null ) {
            map.put("o",new OrbitFieldHandler());
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
     * reset the seconds register.  setDigit( String formatCode, double val ) accumulates 
     * fractional part in the seconds.
     */
    public void resetSeconds() {
        time.seconds = 0;
    }

    /**
     * force the parser to look for delimiters
     */
    public void sloppyColumns() {
        this.lengths[0] = -1;
        for (int i = 1; i < this.offsets.length; i++) {
            this.offsets[i] = -1;
            this.lengths[i] = -1;
        //TODO: check for delims
        }
    }

    public TimeParser parse(String timeString) throws ParseException {
        return parse( timeString, null );
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
    public TimeParser parse(String timeString, Map<String,String> extra ) throws ParseException {
        int offs = 0;
        int len = 0;

        if ( extra==null ) extra= new HashMap();
        
        orbitDatumRange=null;
        
        time.year = context.year;
        time.month = context.month;
        time.day = context.day;
        time.hour = context.hour;
        time.minute = context.minute;
        time.seconds = context.seconds;
        time.micros = context.micros;

        for (int idigit = 1; idigit < ndigits; idigit++) {
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
                    while ( Character.isWhitespace( timeString.charAt(offs) ) ) offs++;
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
            try {

                if (handlers[idigit] < 10) {
                    int digit;
                        digit= Integer.parseInt(field);
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
                    }
                } else if (handlers[idigit] == 100) {
                    FieldHandler handler = (FieldHandler) fieldHandlers.get(fc[idigit]);
                    handler.parse(timeString.substring(offs, offs + len), time, timeWidth, extra );
                    if ( handler instanceof Orbits.OrbitFieldHandler ) {
                        orbitDatumRange= ((Orbits.OrbitFieldHandler)handler).getOrbitRange();
                    }
                } else if (handlers[idigit] == 10) {
                    char ch = timeString.charAt(offs);
                    if (ch == 'P' || ch == 'p') {
                        time.hour += 12;
                    }
                } else if (handlers[idigit] == 11) {
                    int offset;
                    offset= Integer.parseInt(timeString.substring(offs, offs + len));
                    time.hour -= offset / 100;   // careful!

                    time.minute -= offset % 100;
                } else if (handlers[idigit] == 12) {
                    //ignore
                } else if (handlers[idigit] == 13) { // month name
                    time.month = TimeUtil.monthNumber(timeString.substring(offs, offs + len));

                } else if (handlers[idigit] == 14) {
                    //ignore
                }
            } catch ( NumberFormatException ex ) {
                throw new ParseException( String.format( "fail to parse digit number %d: %s", idigit, field ), offs );
            }

        }
        return this;
    }

    private static class FieldSpec {
        String spec=null;  // unparsed spec
        String fieldType= null;
        int length= -1;
        String params= null;
    }

    /**
     * parse field specifications like:
     *   %{milli;cadence=100}
     *   %3{skip}
     * @param spec
     * @return
     */
    FieldSpec parseSpec(String spec) {
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
     * @param format
     * @param value
     */
    public void setDigit(String format, double value) {
        if (format.equals("%{ignore}") ) return;
        if (format.equals("%X") ) return;
        if (value < 0) {
            throw new IllegalArgumentException("value must not be negative on field:"+format+" value:"+value );
        }
        String[] ss = format.split("%", -2);
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
    public TimeParser setDigit(String format, int value) {
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
     * This allows for string split into elements to be interpretted here.  This
     * is to add flexibility to external parsers that have partially parsed the
     * number already.
     * examples:
     *   TimeParser p= TimeParser.create("%Y %m %d");
     *   p.setDigit(0,2007).setDigit(1,12).setDigit(2,5).getTime( Units.us2000 );
     *   p.format();  // maybe in the future
     * @throws IllegalArgumentException if the digit has a custom field handler
     * @throws IllegalArgumentException if the digit does not exist.
     * @param digitNumber, the digit to set (starting with 0).
     */
    public TimeParser setDigit(int digitNumber, int digit) {
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

    public double getTime(Units units) {
        return Units.us2000.convertDoubleTo(units, toUs2000(time));
    }

    public Datum getTimeDatum() {
        if (time.year < 1990) {
            return Units.us1980.createDatum(toUs1980(time));
        } else {
            return Units.us2000.createDatum(toUs2000(time));
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
                logger.log(Level.SEVERE, null, ex);
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
     */
    public DatumRange getTimeRange() {
        if ( time.day==1 && time.hour==0 && time.minute==0 && time.seconds==0 &&
                timeWidth.year==0 && timeWidth.month==1 && timeWidth.day==0 && timeWidth.year==0 ) { //TODO: sloppy!
            TimeStruct time2 = time.add(timeWidth);
            int[] t1= new int[] { time.year, time.month, time.day, time.hour, time.minute, (int)time.seconds, time.millis };
            int[] t2= new int[] { time2.year, time2.month, time2.day, time2.hour, time2.minute, (int)time2.seconds, time2.millis };
            return new MonthDatumRange( t1, t2 );
        } else if ( orbitDatumRange!=null ) {
            return orbitDatumRange;
        } else {
            TimeStruct time2 = time.add(timeWidth);
            double t1 = toUs2000(time);
            double t2 = toUs2000(time2);
            return new DatumRange(t1, t2, Units.us2000);
        }
    }

    public double getEndTime(Units units) {
        throw new IllegalArgumentException("not implemented for DatumRanges as of yet");
    }

    public String getRegex() {
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
     * format the range into a string.
     * @param start
     * @param end currently ignored, and may be used in the future.  This may be null.
     * @return formatted string.
     */
    public String format(Datum start, Datum end) {

        if ( end==null ) end= start;

        StringBuilder result = new StringBuilder(100);

        int offs = 0;
        int len = 0;

        TimeUtil.TimeStruct timel = TimeUtil.toTimeStruct(start);

        double dextraMillis= 1000 * ( timel.seconds - (int) timel.seconds );
        int extraMillis= (int)Math.floor( dextraMillis );
        timel.seconds= (int)timel.seconds;

        double extraMicros= 1000 * ( dextraMillis - extraMillis );

        NumberFormat[] nf = new NumberFormat[5];
        nf[2] = new DecimalFormat("00");
        nf[3] = new DecimalFormat("000");
        nf[4] = new DecimalFormat("0000");


        for (int idigit = 1; idigit < ndigits; idigit++) {
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
                        digit = timel.millis + extraMillis;
                        break;
                    case 9:
                        digit = timel.micros + (int) extraMicros;
                        break;
                    default:
                        throw new RuntimeException("shouldn't get here");
                }
                if ( span>1 ) {
                    if ( handlers[idigit]>0 && handlers[idigit]<5 ) logger.warning("uh-oh, span used on ordinal like month, day");
                    digit= ( digit / span ) * span;
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
                    TimeUtil.TimeStruct timeEnd = TimeUtil.toTimeStruct(end);
                    String ins= fh1.format( timel, TimeUtil.subtract(timeEnd, timel), len, null );
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
            }
        }
        result.insert(offs, this.delims[ndigits - 1]);
        return result.toString().trim();
    }

    @Override
    public String toString() {
        StringBuilder result= new StringBuilder();
        for ( int i=0;i<this.fc.length; i++ ) {
            if ( this.fc[i]!=null ) result.append("%").append( this.fc[i]);
            result.append( this.delims[i] );
        }
        return result.toString();
    }


}
