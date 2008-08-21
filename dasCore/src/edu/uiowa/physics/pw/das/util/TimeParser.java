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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * TimeParser designed to quickly parse strings with a specified format.  This parser has been
 * shown to perform around 20 times faster than that the generalized parser.
 *
 * @author Jeremy
 */
public class TimeParser {

    final static Logger logger = Logger.getLogger("TimeParser");
    /**
     * %Y-%m-%dT%H:%M:%S.%{milli}Z
     */
    public static final String TIMEFORMAT_Z = "%Y-%m-%dT%H:%M:%S.%{milli}Z";
    
    TimeStruct time;
    TimeStruct timeWidth;
    int ndigits;
    String[] valid_formatCodes = new String[]{"Y", "y", "j", "m", "d", "H", "M", "S", "milli", "micro", "p", "z", "ignore", "b" };
    String[] formatName = new String[]{"Year", "2-digit-year", "day-of-year", "month", "day", "Hour", "Minute", "Second", "millisecond", "microsecond",
        "am/pm", "RFC-822 numeric time zone", "ignore", "3-char-month-name", };
    int[] formatCode_lengths = new int[]{4, 2, 3, 2, 2, 2, 2, 2, 3, 3, 2, 5, -1, 3 };
    int[] precision = new int[]{0, 0, 2, 1, 2, 3, 4, 5, 6, 7, -1, -1, -1, 1 };
    int[] handlers;
    
    /**
     * set of custom handlers to allow for extension
     */
    Map/*<String,FieldHandler>*/ fieldHandlers;
    /**
     * positions of each digit, within the string to be parsed.  If position is -1, then we need to
     * compute it along the way.
     */
    int[] offsets;
    int[] lengths;
    String[] delims;
    String[] fc;
    String regex;
    String formatString;
    
    /**
     * Least significant digit in format.
     *0=year, 1=month, 2=day, 3=hour, 4=min, 5=sec, 6=milli, 7=micro
     */
    int lsd;

    public interface FieldHandler {
        public void handleValue(String fieldContent, TimeStruct startTime, TimeStruct timeWidth);
    }

    /**
     * must contain T or space to delimit date and time.
     * @param exampleTime "1992-353T02:00"
     * @return "%Y-%jT%H%M" etc.
     */
    public static String iso8601String( String exampleTime ) {
        int i= exampleTime.indexOf("T");
        if ( i==-1 ) i= exampleTime.indexOf(" ");
        char dateTimeDelim= exampleTime.charAt(i);
        
        String date=null, time=null;
        if ( i!=-1 ) {
            String datePart= exampleTime.substring(0,i);
            boolean hasDelim= !datePart.matches("\\d+");
            char delim=0;
            if ( hasDelim ) delim= datePart.charAt(4);
            switch ( datePart.length() ) {
                case 10: date= "%Y" + delim + "%m" + delim + "%d"; break;
                case 9: date= "%Y" + delim + "%j"; break;
                case 8: date= hasDelim ? "%Y"+delim+"%j" : "%Y%m%d"; break;
                case 7: date= "%Y%j"; break;
                default: throw new IllegalArgumentException("unable to identify date format for "+exampleTime );
            }
            
            String timePart= exampleTime.substring(i+1);
            hasDelim= !timePart.matches("\\d+");
            delim=0;
            if ( hasDelim ) delim= timePart.charAt(2);
            
            switch ( timePart.length() ) {
                case 4: time= "%H%M"; break;
                case 5: time= "%H"+delim+"%M"; break;                
                case 6: time= "%H%M%S"; break;
                case 8: time= "%H"+delim+"%M"+delim+"%S"; break;
                case 12: time= "%H"+delim+"%M"+delim+"%S.%{milli}"; break;
                case 15: time= "%H"+delim+"%M"+delim+"%S.%{milli}%{micro}"; break;
                default: throw new IllegalArgumentException("unable to identify time format for "+exampleTime );
            }
            
            return date + dateTimeDelim + time;
            
        } else {
            throw new IllegalArgumentException("example time must contain T or space.");
        }
    }
    
    private TimeParser( String formatString, Map/*<String,FieldHandler>*/ fieldHandlers ) {
        time = new TimeUtil.TimeStruct();
        this.fieldHandlers = fieldHandlers;
        this.formatString= formatString;
        
        String[] ss = formatString.split("%");
        fc = new String[ss.length];
        String[] delim = new String[ss.length + 1];

        ndigits = ss.length;

        StringBuffer regex = new StringBuffer(100);
        regex.append(ss[0]);
        
        lengths = new int[ndigits];
        for (int i = 0; i < lengths.length; i++) {
            lengths[i] = -1; // -1 indicates not known, but we'll figure out as many as we can.
        }

        delim[0] = ss[0];
        for (int i = 1; i < ndigits; i++) {
            int pp = 0;
            while (Character.isDigit(ss[i].charAt(pp))) {
                pp++;
            }
            if (pp > 0) {
                lengths[i] = Integer.parseInt(ss[i].substring(0, pp));
            }

            if (ss[i].charAt(pp) != '{') {
                fc[i] = ss[i].substring(pp, pp + 1);
                delim[i] = ss[i].substring(pp + 1);
            } else {
                int endIndex = ss[i].indexOf('}', pp);
                fc[i] = ss[i].substring(pp + 1, endIndex);
                delim[i] = ss[i].substring(endIndex + 1);
            }
        }

        handlers = new int[ndigits];
        offsets = new int[ndigits];

        int pos = 0;
        offsets[0] = pos;

        lsd = -1;
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
                    lsd = 100;
                    handler = 100;
                    handlers[i] = 100;
                    offsets[i] = pos;
                    if (lengths[i] < 1 || pos == -1) { // 0->indetermined as well, allows user to force indeterminate

                        pos = -1;
                        lengths[i] = -1;
                    } else {
                        pos += lengths[i];
                    }
                }
            } else {
                handlers[i] = handler;
                if (lengths[i] == -1) {
                    lengths[i] = formatCode_lengths[handler];
                }
                offsets[i] = pos;
                if (lengths[i] < 1 || pos == -1) {
                    pos = -1;
                    lengths[i] = -1;
                } else {
                    pos += lengths[i];
                }
            }

            if (handler < 100) {
                if (precision[handler] > lsd) {
                    lsd = precision[handler];
                }
            }
            String dots = ".........";
            if (lengths[i] == -1) {
                regex.append("(.*)");
            } else {
                regex.append("(" + dots.substring(0, lengths[i]) + ")");
            }
            regex.append(delim[i]);

        }

        timeWidth = new TimeStruct();
        switch (lsd) {
            case 0:
                timeWidth.year = 1;
                break;
            case 1:
                timeWidth.month = 1;
                break;
            case 2:
                timeWidth.day = 1;
                break;
            case 3:
                timeWidth.hour = 1;
                break;
            case 4:
                timeWidth.minute = 1;
                break;
            case 5:
                timeWidth.seconds = 1;
                break;
            case 6:
                timeWidth.millis = 1;
                break;
            case 7:
                timeWidth.micros = 1;
                break;
            case 100: /* do nothing */ break;
        }

        this.delims = delim;
        this.regex = regex.toString();
    }

    /**
     * <pre>
     *  %[fieldLength]<1-char code>  or
     *  %[fieldLength]{<code>}
     *
     *  fieldLength=0 --> makes field length indeterminate, deliminator must follow.
     *
     *  %Y   4-digit year
     *  %y    2-digit year
     *  %j     3-digit day of year
     *  %m   2-digit month
     *  %b   3-char month name
     *  %d    2-digit day
     *  %H    2-digit hour
     *  %M    2-digit minute
     *  %S     2-digit second
     *  %{milli}  3-digit milliseconds
     *  </pre>
     *
     */
    public static TimeParser create(String formatString) {
        return new TimeParser(formatString, null);
    }

    /**
     *  This version allows for extension by specifying an external handler.
     *
     *  %3{fieldName}   2 characters are passed to the handler
     *  %Y   4-digit year
     *  %y    2-digit year
     *  %m   month
     *  %2m 2-digit month
     *  %d    2-digit day
     *  %H    2-digit hour
     *  %M    2-digit minute
     *  %S     2-digit second
     *  %{milli}  3-digit milliseconds
     */
    public static TimeParser create(String formatString, String fieldName, FieldHandler handler) {
        HashMap map = new HashMap();
        map.put(fieldName, handler);
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
        int year =  d.year;
        int month =  d.month;
        int day =  d.day;
        int jd = 367 * year - 7 * (year + (month + 9) / 12) / 4 -
                3 * ((year + (month - 9) / 7) / 100 + 1) / 4 +
                275 * month / 9 + day + 1721029;
        int hour =  d.hour;
        int minute =  d.minute;
        double seconds = d.seconds + hour * (float) 3600.0 + minute * (float) 60.0;
        double us1980 = (jd - 2436205 - 8035) * 86400000000. + seconds * 1e6 + d.millis * 1e3 + d.micros;
        return us1980;
    }

    public TimeParser parse(String timeString) throws ParseException {
        int offs = 0;
        int len = 0;

        time.month = 1;
        time.day = 1;
        time.hour = 0;
        time.minute = 0;
        time.seconds = 0;
        time.micros = 0;

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
                    int i = timeString.indexOf(this.delims[idigit], offs);
                    len = i - offs;
                }
            }
            if (handlers[idigit] < 10) {
                int digit = Integer.parseInt(timeString.substring(offs, offs + len).trim());
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
                handler.handleValue(timeString.substring(offs, offs + len), time, timeWidth);
            } else if (handlers[idigit] == 10) {
                char ch = timeString.charAt(offs);
                if (ch == 'P' || ch == 'p') {
                    time.hour += 12;
                }
            } else if (handlers[idigit] == 11) {
                int offset = Integer.parseInt(timeString.substring(offs, offs + len));
                time.hour -= offset / 100;   // careful!

                time.minute -= offset % 100;
            } else if ( handlers[idigit]== 13 ) {
                time.month= TimeUtil.monthNumber( timeString.substring(offs, offs + len) );
                
            }
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
            case 13:
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
     * Returns the implicit interval as a DatumRange.
     * For example, "Jan 1, 2003" would have a getTimeDatum of "Jan 1, 2003 00:00:00",
     * and getDatumRange() would go from midnight to mignight.
     */
    public DatumRange getTimeRange() {
        TimeStruct time2 = time.add(timeWidth);
        double t1 = toUs2000(time);
        double t2 = toUs2000(time2);
        return new DatumRange(t1, t2, Units.us2000);
    }

    public double getEndTime(Units units) {
        throw new IllegalArgumentException("not implemented for DatumRanges as of yet");
    }

    public String getRegex() {
        return this.regex;
    }

    /**
     * format the range into a string.
     * @param start
     * @param end currently ignored, and may be used in the future
     * @return formatted string.
     */
    public String format(Datum start, Datum end) {

        StringBuffer result = new StringBuffer(100);

        int offs = 0;
        int len = 0;

        TimeUtil.TimeStruct timel= TimeUtil.toTimeStruct(start);

        NumberFormat[] nf = new NumberFormat[5];
        nf[2] = new DecimalFormat("00");
        nf[3] = new DecimalFormat("000");
        nf[4] = new DecimalFormat("0000");


        for (int idigit = 1; idigit < ndigits; idigit++) {
            result.insert( offs,  this.delims[idigit - 1]);
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
                int digit;
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
                    default: throw new RuntimeException("shouldn't get here");
                }
                result.insert( offs, nf[len].format(digit) );
                offs+= len;

            } else if ( handlers[idigit]== 13 ) { // month names
                result.insert( offs, TimeUtil.monthNameAbbrev(timel.month) );
                offs+= len;
                
            } else if (handlers[idigit] == 100) {
                throw new RuntimeException("Handlers not supported");

            } else if (handlers[idigit] == 10) {
               throw new RuntimeException("AM/PM supported");

            } else if (handlers[idigit] == 11) {
                throw new RuntimeException("Time Zones not supported");
            }
        }
        return result.toString().trim();
    }
}
