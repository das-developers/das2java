/* File: TimeDatumFormatter.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on September 25, 2003, 1:47 PM
 *      by Edward West <eew@space.physics.uiowa.edu>
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

package org.das2.datum.format;

import java.text.Format;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.TimeUtil;
import org.das2.datum.Units;

/**
 * Formatter is configured with strings of SimpleDateFormat (yyyy-MM-DD)
 * or % percent format (%Y-%m-%d) specifiers.  
 * SimpleDateFormat is discouraged because most other parts of das2 use the
 * shorter form.
 * This does not support formats with seconds but no hours and minutes, like "$Y $j $S"
 * 
 * @author  Edward West
 */
public class TimeDatumFormatter extends DatumFormatter {
        
    /** Private constants for referencing an array of timestamp fields */
    private static final int YEAR_FIELD_INDEX = 0;
    private static final int MONTH_FIELD_INDEX = 1;
    private static final int DAY_FIELD_INDEX = 2;
    private static final int DOY_FIELD_INDEX = 3;
    private static final int HOUR_FIELD_INDEX = 4;
    private static final int MINUTE_FIELD_INDEX = 5;
    private static final int SECONDS_FIELD_INDEX = 6;
    private static final int TIMESTAMP_FIELD_COUNT = 7;
    
    /**
     * yyyy-MM-dd'T'HH:mm:ss.SSS'Z
     */
    public static final TimeDatumFormatter DEFAULT;
    /**
     * yyyy-MM-dd
     */
    public static final TimeDatumFormatter DAYS;

    /**
     * yyyy-JJJ
     */
    public static final TimeDatumFormatter DAY_OF_YEAR;
    
    /**
     * yyyy-MM-dd (JJJ)
     */
    public static final TimeDatumFormatter DOY_DAYS;

    /**
     * yyyy
     */
    public static final TimeDatumFormatter YEARS;
    /**
     * yyyy-MM
     */
    public static final TimeDatumFormatter MONTHS;
    /**
     * yyyy-MM-dd HH:'00'
     */
    public static final TimeDatumFormatter HOURS;
    /**
     * HH:mm
     */
    public static final TimeDatumFormatter MINUTES;
    /**
     * HH:mm:ss
     */
    public static final TimeDatumFormatter SECONDS;
    /**
     * HH:mm:ss.SSS
     */
    public static final TimeDatumFormatter MILLISECONDS;
    /**
     * HH:mm:ss.SSSSSS
     */
    public static final TimeDatumFormatter MICROSECONDS;
    /**
     * HH:mm:ss.SSSSSSSSS
     */
    public static final TimeDatumFormatter NANOSECONDS;
    
    //Initialize final constants
    static {
        try {
            DEFAULT = new TimeDatumFormatter("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            YEARS= new TimeDatumFormatter("yyyy");
            MONTHS= new TimeDatumFormatter("yyyy-MM");
            DAYS = new TimeDatumFormatter("yyyy-MM-dd");
            DAY_OF_YEAR = new TimeDatumFormatter("%Y-%j");
            DOY_DAYS = new TimeDatumFormatter("%Y-%m-%d (%j)");
            HOURS = new TimeDatumFormatter("yyyy-MM-dd HH:'00'");
            MINUTES = new TimeDatumFormatter("HH:mm");
            SECONDS = new TimeDatumFormatter("HH:mm:ss");
            MILLISECONDS = new TimeDatumFormatter("HH:mm:ss.SSS");
            MICROSECONDS = new TimeDatumFormatter("HH:mm:ss.SSSSSS");
            NANOSECONDS = new TimeDatumFormatter("HH:mm:ss.SSSSSSSSS");
        } catch (ParseException pe) {
            throw new RuntimeException(pe);
        }
    }
    
    private final String formatString;
    
    private final MessageFormat format;
    
    private int[] scaleSeconds;
    
    /** 
     * Creates a new instance of TimeDatumFormatter
     * @param formatString
     * @throws java.text.ParseException
     */
    public TimeDatumFormatter(String formatString) throws ParseException {
        this.formatString = formatString;
        if ( formatString.contains( "%" ) ) {
            format = new MessageFormat(parseTimeFormatStringPercent(formatString));
        } else {
            format = new MessageFormat(parseTimeFormatString(formatString));
        }
    }
        
    /**
     * returns a TimeDatumFormatter suitable for the specified scale and context.
     * Context may be null to indicate that the formatted string will be interpreted
     * outside of any context.
     * @param scale the length we wish to represent, such as TimeUtil.HOUR
     * @param context the context for the formatter, or null if the formatted string
     *   will be interpreted outside of any context.
     * @return the formatter.
     * @throws IllegalArgumentException if the scale is TimeUtil.NANOS or is not found in TimeUtil.
     */
    public static TimeDatumFormatter formatterForScale( int scale, DatumRange context ) {
        return formatterForScale( scale, context, false );
    }
    
    /**
     * returns a TimeDatumFormatter suitable for the specified scale and context.
     * Context may be null to indicate that the formatted string will be interpreted
     * outside of any context.
     * @param scale the length we wish to represent, such as TimeUtil.HOUR
     * @param context the context for the formatter, or null if the formatted string
     *   will be interpreted outside of any context.
     * @param useDOY if true then use day-of-year rather than separate month and day.
     * @return the formatter.
     * @throws IllegalArgumentException if the scale is TimeUtil.NANOS or is not found in TimeUtil.
     */
    public static TimeDatumFormatter formatterForScale( int scale, DatumRange context, boolean useDOY ) {
        try {
            if ( context!=null ) {
                switch ( scale ) {
                    case TimeUtil.YEAR: return YEARS;
                    case TimeUtil.MONTH: return useDOY ? DAY_OF_YEAR : MONTHS;
                    case TimeUtil.DAY: return useDOY ? DAY_OF_YEAR : DAYS;
                    case TimeUtil.HOUR: return MINUTES;
                    case TimeUtil.MINUTE: return MINUTES;
                    case TimeUtil.SECOND: return SECONDS;
                    case TimeUtil.MILLI: return MILLISECONDS;
                    case TimeUtil.MICRO: return MICROSECONDS;
                    case TimeUtil.NANO: return NANOSECONDS;
                    default: throw new IllegalArgumentException("unsupported scale: "+scale);
                }
            } else {
                if ( useDOY ) {
                    switch ( scale ) {
                        case TimeUtil.YEAR: return YEARS;
                        case TimeUtil.MONTH: return DAY_OF_YEAR;
                        case TimeUtil.DAY: return DAY_OF_YEAR;
                        case TimeUtil.HOUR: return new TimeDatumFormatter("%Y-%j %H:%M");
                        case TimeUtil.MINUTE: return new TimeDatumFormatter("%Y-%j %H:%M");
                        case TimeUtil.SECOND: return new TimeDatumFormatter("%Y-%j %H:%M:%S");
                        case TimeUtil.MILLI: return new TimeDatumFormatter("%Y-%j %H:%M:%S.%(subsec,places=3)");
                        case TimeUtil.MICRO: return new TimeDatumFormatter("%Y-%j %H:%M:%S.%(subsec,places=6)");
                        case TimeUtil.NANO: return new TimeDatumFormatter("%Y-%j %H:%M:%S.%(subsec,places=9)");
                        default: throw new IllegalArgumentException("unsupported scale: "+scale);
                    } 
                } else {
                    switch ( scale ) {
                        case TimeUtil.YEAR: return YEARS;
                        case TimeUtil.MONTH: return MONTHS;
                        case TimeUtil.DAY: return DAYS;
                        case TimeUtil.HOUR: return new TimeDatumFormatter("yyyy-MM-dd HH:mm");
                        case TimeUtil.MINUTE: return new TimeDatumFormatter("yyyy-MM-dd HH:mm");
                        case TimeUtil.SECOND: return new TimeDatumFormatter("yyyy-MM-dd HH:mm:ss");
                        case TimeUtil.MILLI: return new TimeDatumFormatter("yyyy-MM-dd HH:mm:ss.SSS");
                        case TimeUtil.MICRO: return new TimeDatumFormatter("yyyy-MM-dd HH:mm:ss.SSSSSS");
                        case TimeUtil.NANO: return new TimeDatumFormatter("yyyy-MM-dd HH:mm:ss.SSSSSSSSS");
                        default: throw new IllegalArgumentException("unsupported scale: "+scale);
                    }       
                }
            }
        } catch ( ParseException e ) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public String toString() {
        return formatString;
    }
    
    @Override
    public String format(Datum datum) {
        if ( datum.isFill() ) return "fill";
        int mjd1958= (int)datum.doubleValue( Units.mj1958 );
        TimeUtil.TimeStruct ts;
        if ( mjd1958 < -349909 ) { // year 1000
            ts= TimeUtil.toTimeStruct( Units.mj1958.createDatum(-349909) );
        } else if ( mjd1958 > 2937279 ) { // year 9999 // should be 2937613 rounding errors?
            ts= TimeUtil.toTimeStruct( Units.mj1958.createDatum(2937279) );
        } else {
            ts= TimeUtil.toTimeStruct(datum);
        }
        Number[] array = timeStructToArray(ts);
        if ( array.length==8 && array[7].intValue()==-1 ) array[7]=0; // https://github.com/das-developers/das2java/issues/16
        return format.format(array);
    }
    
    protected Format getFormat() {
        return format;
    }
    
    /**
     * parse message format strings like yyyy-MM-DD
     * @param input
     * @return
     * @throws ParseException 
     */
    protected final String parseTimeFormatString(String input) throws ParseException {
        final String formatPattern = "(([yMDdHmsS])\\2*)";
        final String delimiterPattern = "([-/:.,_ \t]+)";
        final String literalPattern = "('(?:[^']|'')*')";
        Pattern token = Pattern.compile(
                formatPattern + "|" + delimiterPattern + "|" + literalPattern
                );
        int from = 0;
        StringBuilder frmtString = new StringBuilder();
        Matcher matcher = token.matcher(input);
        while (matcher.find(from)) {
            int start = matcher.start();
            if (start > from) {
                char[] dots = new char[start + 1];
                java.util.Arrays.fill(dots, from, start, '.');
                dots[from] = '^';
                dots[start] = '^';
                StringBuilder errorString = new StringBuilder("Unrecognized sub-pattern\n");
                errorString.append(input).append("\n");
                errorString.append(dots);
                throw new ParseException(errorString.toString(), from);
            }
            String frmt = matcher.group(1);
            String delimiter = matcher.group(3);
            String literal = matcher.group(4);
            if (frmt != null) {
                switch (frmt.charAt(0)) {
                    case 'y': {
                        appendSubFormat(frmtString, YEAR_FIELD_INDEX, frmt.length());
                    } break;
                    case 'M': {
                        appendSubFormat(frmtString, MONTH_FIELD_INDEX, frmt.length());
                    } break;
                    case 'D': {
                        appendSubFormat(frmtString, DOY_FIELD_INDEX, frmt.length());
                    } break;
                    case 'd': {
                        appendSubFormat(frmtString, DAY_FIELD_INDEX, frmt.length());
                    } break;
                    case 'H': {
                        appendSubFormat(frmtString, HOUR_FIELD_INDEX, frmt.length());
                    } break;
                    case 'm': {
                        appendSubFormat(frmtString, MINUTE_FIELD_INDEX, frmt.length());
                    } break;
                    case 's': {
                        appendSubFormat(frmtString, SECONDS_FIELD_INDEX, frmt.length());
                    }break;
                    case 'S': {
                        int digitCount = frmt.length();
                        int fieldIndex = addScaleFactor(digitCount);
                        appendSubFormat(frmtString, fieldIndex, digitCount);
                    }
                    break;
                    default: break;
                }
            } else if (delimiter != null) {
                frmtString.append(delimiter);
            } else if (literal != null) {
                literal = literal.substring(1, literal.length() - 1);
                literal = literal.replaceAll("''", "'");
                frmtString.append(literal);
            }
            from = matcher.end();
        }
        return frmtString.toString();
    }
    
    /**
     * create the message format, based on %Y, %m, %d format specification
     * of the UNIX date command.
     * @param format the format spec, such as %Y%m%dT%H%M
     * @return a SimpleDateFormat string.
     * @throws java.text.ParseException
     */
    protected final String parseTimeFormatStringPercent(String format) throws ParseException {
        StringBuilder frmtString= new StringBuilder();
        String[] ss= format.split("%");
        frmtString.append(ss[0]);
        int offset= ss[0].length();
        
        int lsb=0;
        for ( int i=1; i<ss.length; i++ ) {
            offset+= 1;
            char c= ss[i].charAt(0);
            int oldLsb= lsb;
            switch (c) {
                case 'Y': appendSubFormat(frmtString, YEAR_FIELD_INDEX, 4 ); lsb=YEAR_FIELD_INDEX; break;
                case 'y': appendSubFormat(frmtString, YEAR_FIELD_INDEX, 2 ); lsb=YEAR_FIELD_INDEX; break;
                case 'j': appendSubFormat(frmtString, DOY_FIELD_INDEX, 3 ); lsb=DOY_FIELD_INDEX; break;
                case 'm': appendSubFormat(frmtString, MONTH_FIELD_INDEX, 2 ); lsb=MONTH_FIELD_INDEX; break;
                case 'd': appendSubFormat(frmtString, DAY_FIELD_INDEX, 2 ); lsb=DAY_FIELD_INDEX; break;
                case 'H': appendSubFormat(frmtString, HOUR_FIELD_INDEX, 2 ); lsb=HOUR_FIELD_INDEX; break;
                case 'M': appendSubFormat(frmtString, MINUTE_FIELD_INDEX, 2 ); lsb=MINUTE_FIELD_INDEX; break;
                case 'S': appendSubFormat(frmtString, SECONDS_FIELD_INDEX, 2 ); lsb=SECONDS_FIELD_INDEX; break;
                case '{': 
                    int i1= ss[i].indexOf('}');
                    String spec= ss[i].substring(1,i1);
                    switch (spec) {
                        case "milli": {
                            int digitCount = 3;
                            int fieldIndex = addScaleFactor(digitCount);
                            appendSubFormat(frmtString, fieldIndex, digitCount);
                            ss[i]= ss[i].substring(i1);
                            break;
                        }
                        case "micro": {
                            throw new IllegalArgumentException("This is no longer supported, use subsec");
                            //int digitCount = 3;
                            //int fieldIndex = addScaleFactor(6);
                            //appendSubFormat(frmtString, fieldIndex, digitCount);
                            //ss[i]= ss[i].substring(i1);
                            //break;
                        }
                        default:
                            throw new ParseException("bad format code: {"+spec+"}",offset);
                    }
                    break;
                default: throw new ParseException("bad format code: "+c,offset);
            }
            if ( oldLsb>2 && lsb-oldLsb > 1 ) {
                logger.log(Level.WARNING, "gap in time digits detected: {0}", format);
            }
            frmtString.append(ss[i].substring(1));
            offset+= ss[i].length();
        }
        return frmtString.toString();
    }
    
    private static void appendSubFormat(StringBuilder buffer, int fieldIndex,int count) {
        buffer.append("{").append(fieldIndex).append(",number,");
        for (int i = 0; i < count; i++) {
            buffer.append('0');
        }
        buffer.append("}");
    }
    
    private int addScaleFactor(int scale) {
        if (scaleSeconds == null) {
            scaleSeconds = new int[1];
        } else {
            int[] temp = new int[scaleSeconds.length + 1];
            System.arraycopy(scaleSeconds, 0, temp, 0, scaleSeconds.length);
            scaleSeconds = temp;
        }
        scaleSeconds[scaleSeconds.length - 1] = scale;
        return TIMESTAMP_FIELD_COUNT + scaleSeconds.length - 1;
    }
    
    /**
     * returns Number[] for formatting, truncating the fractional seconds and putting the
     * remainder in lower sig digits.
     *
     * Note: TimeUtil.TimeStruct ts is modified.
     * @return [ year, month, dayOfMonth, dayOfYear, hour, minute, seconds, ... ]
     */
    private Number[] timeStructToArray(TimeUtil.TimeStruct ts) {
        int secondsFieldCount = scaleSeconds == null ? 0 : scaleSeconds.length; 
        int maxScale = scaleSeconds == null ? 10 : (int)Math.pow(10, max(scaleSeconds));
        int fieldCount = TIMESTAMP_FIELD_COUNT + secondsFieldCount;
        Number[] array = new Number[fieldCount];
        int seconds = (int)( (long)Math.round(ts.seconds * maxScale) / maxScale );
        double fracSeconds = ts.seconds - seconds;
        ts.seconds= seconds;
        ts.micros= (int)(fracSeconds * 1e6);

        if ( ts.micros<0 ) {
            ts.seconds--;
            ts.micros+=1000000;
        }
        TimeUtil.carry(ts);
        if ( scaleSeconds!=null && scaleSeconds[0]<7 ) {
            ts= TimeUtil.roundNDigits(ts,scaleSeconds[0]);
        }

        array[YEAR_FIELD_INDEX] = ts.year;
        array[MONTH_FIELD_INDEX] = ts.month;
        array[DAY_FIELD_INDEX] = ts.day;
        array[DOY_FIELD_INDEX] = ts.doy;
        array[HOUR_FIELD_INDEX] = ts.hour;
        array[MINUTE_FIELD_INDEX] = ts.minute;
        array[SECONDS_FIELD_INDEX] = (int)ts.seconds;
        for (int i = TIMESTAMP_FIELD_COUNT; i < array.length; i++) {
            int value = (int)Math.round(fracSeconds * Math.pow(10, scaleSeconds[i - TIMESTAMP_FIELD_COUNT]));
            array[i] = value;
        }
        return array;
    }
    
    private static int max(int[] list) {
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < list.length; i++) {
            max = Math.max(max, list[i]);
        }
        return max;
    }
    
}
