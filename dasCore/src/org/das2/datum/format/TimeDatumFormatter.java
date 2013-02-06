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

import java.text.*;
import java.util.regex.*;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.TimeUtil;

/**
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
    
    private String formatString;
    
    private MessageFormat format;
    
    private int[] scaleSeconds;
    
    /** Creates a new instance of TimeDatumFormatter */
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
     * Context may be null to indicate that the formatted string will be interpretted
     * outside of any context.
     * @param scale the length we wish to represent, such as TimeUtil.HOUR
     * @param context the context for the formatter, or null if the formatted string
     *   will be interpretted outside of any context.
     * @throws IllegalArgumentException if the scale is TimeUtil.NANOS or is not found in TimeUtil.
     */
    public static TimeDatumFormatter formatterForScale( int scale, DatumRange context ) {
        try {
            if ( context!=null ) {
                switch ( scale ) {
                    case TimeUtil.YEAR: return YEARS;
                    case TimeUtil.MONTH: return MONTHS;
                    case TimeUtil.DAY: return DAYS;
                    case TimeUtil.HOUR: return MINUTES;
                    case TimeUtil.MINUTE: return MINUTES;
                    case TimeUtil.SECOND: return SECONDS;
                    case TimeUtil.MILLI: return MILLISECONDS;
                    case TimeUtil.MICRO: return MICROSECONDS;
                    case TimeUtil.NANO: return NANOSECONDS;
                    default: throw new IllegalArgumentException("unsupported scale: "+scale);
                }
            } else {
                switch ( scale ) {
                    case TimeUtil.YEAR: return YEARS;
                    case TimeUtil.MONTH: return MONTHS;
                    case TimeUtil.DAY: return DAYS;
                    case TimeUtil.HOUR: return HOURS;
                    case TimeUtil.MINUTE: return new TimeDatumFormatter("yyyy-MM-dd HH:mm");
                    case TimeUtil.SECOND: return new TimeDatumFormatter("yyyy-MM-dd HH:mm:ss");
                    case TimeUtil.MILLI: return new TimeDatumFormatter("yyyy-MM-dd HH:mm:ss.SSS");
                    case TimeUtil.MICRO: return new TimeDatumFormatter("yyyy-MM-dd HH:mm:ss.SSSSSS");
                    case TimeUtil.NANO: return new TimeDatumFormatter("yyyy-MM-dd HH:mm:ss.SSSSSSSSS");
                    default: throw new IllegalArgumentException("unsupported scale: "+scale);
                }                
            }
        } catch ( ParseException e ) {
            throw new RuntimeException(e);
        }
    }
    
    public String toString() {
        return formatString;
    }
    
    public String format(Datum datum) {
        if ( datum.isFill() ) return "fill";
        TimeUtil.TimeStruct ts = TimeUtil.toTimeStruct(datum);
        Number[] array = timeStructToArray(ts);
        return format.format(array);
    }
    
    protected Format getFormat() {
        return format;
    }
    
    protected String parseTimeFormatString(String input) throws ParseException {
        final String formatPattern = "(([yMDdHmsS])\\2*)";
        final String delimiterPattern = "([-/:.,_ \t]+)";
        final String literalPattern = "('(?:[^']|'')*')";
        Pattern token = Pattern.compile(
                formatPattern + "|" + delimiterPattern + "|" + literalPattern
                );
        int from = 0;
        StringBuffer formatString = new StringBuffer();
        Matcher matcher = token.matcher(input);
        while (matcher.find(from)) {
            int start = matcher.start();
            if (start > from) {
                char[] dots = new char[start + 1];
                java.util.Arrays.fill(dots, from, start, '.');
                dots[from] = '^';
                dots[start] = '^';
                StringBuffer errorString = new StringBuffer("Unrecognized sub-pattern\n");
                errorString.append(input).append("\n");
                errorString.append(dots);
                throw new ParseException(errorString.toString(), from);
            }
            String format = matcher.group(1);
            String delimiter = matcher.group(3);
            String literal = matcher.group(4);
            if (format != null) {
                switch (format.charAt(0)) {
                    case 'y': {
                        appendSubFormat(formatString, YEAR_FIELD_INDEX, format.length());
                    } break;
                    case 'M': {
                        appendSubFormat(formatString, MONTH_FIELD_INDEX, format.length());
                    } break;
                    case 'D': {
                        appendSubFormat(formatString, DOY_FIELD_INDEX, format.length());
                    } break;
                    case 'd': {
                        appendSubFormat(formatString, DAY_FIELD_INDEX, format.length());
                    } break;
                    case 'H': {
                        appendSubFormat(formatString, HOUR_FIELD_INDEX, format.length());
                    } break;
                    case 'm': {
                        appendSubFormat(formatString, MINUTE_FIELD_INDEX, format.length());
                    } break;
                    case 's': {
                        appendSubFormat(formatString, SECONDS_FIELD_INDEX, format.length());
                    }break;
                    case 'S': {
                        int digitCount = format.length();
                        int fieldIndex = addScaleFactor(digitCount);
                        appendSubFormat(formatString, fieldIndex, digitCount);
                    }
                    break;
                    default: break;
                }
            } else if (delimiter != null) {
                formatString.append(delimiter);
            } else if (literal != null) {
                literal = literal.substring(1, literal.length() - 1);
                literal = literal.replaceAll("''", "'");
                formatString.append(literal);
            }
            from = matcher.end();
        }
        return formatString.toString();
    }
    
    /**
     * create the message format, based on %Y, %m, %d format specification.
     * @param input
     * @return
     * @throws java.text.ParseException
     */
    protected String parseTimeFormatStringPercent(String format) throws ParseException {
        StringBuffer formatString= new StringBuffer();
        String[] ss= format.split("%");
        formatString.append(ss[0]);
        int offset= ss[0].length();
        
        for ( int i=1; i<ss.length; i++ ) {
            offset+= 1;
            char c= ss[i].charAt(0);
            switch (c) {
                case 'Y': appendSubFormat(formatString, YEAR_FIELD_INDEX, 4 ); break;
                case 'y': appendSubFormat(formatString, YEAR_FIELD_INDEX, 2 ); break;
                case 'j': appendSubFormat(formatString, DOY_FIELD_INDEX, 3 ); break;
                case 'm': appendSubFormat(formatString, MONTH_FIELD_INDEX, 2 ); break;
                case 'd': appendSubFormat(formatString, DAY_FIELD_INDEX, 2 ); break;
                case 'H': appendSubFormat(formatString, HOUR_FIELD_INDEX, 2 ); break;
                case 'M': appendSubFormat(formatString, MINUTE_FIELD_INDEX, 2 ); break;
                case 'S': appendSubFormat(formatString, SECONDS_FIELD_INDEX, 2 ); break;
                case '{': 
                    int i1= ss[i].indexOf('}');
                    String spec= ss[i].substring(1,i1);
                    if ( spec.equals("milli") ) {
                       int digitCount = 3;
                       int fieldIndex = addScaleFactor(digitCount);
                       appendSubFormat(formatString, fieldIndex, digitCount);
                       ss[i]= ss[i].substring(i1);
                    } else {
                        throw new ParseException("bad format code: {"+spec+"}",offset);
                    }
                    break;
                default: throw new ParseException("bad format code: "+c,offset);
            }
            formatString.append(ss[i].substring(1));
            offset+= ss[i].length();
        }
        return formatString.toString();
    }
    
    private static void appendSubFormat(StringBuffer buffer, int fieldIndex,int count) {
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
     */
    private Number[] timeStructToArray(TimeUtil.TimeStruct ts) {
        int secondsFieldCount = scaleSeconds == null ? 0 : scaleSeconds.length;
        int maxScale = scaleSeconds == null ? 10 : (int)Math.pow(10, max(scaleSeconds));
        int fieldCount = TIMESTAMP_FIELD_COUNT + secondsFieldCount;
        Number[] array = new Number[fieldCount];
        int seconds = (int)Math.round(ts.seconds * maxScale) / maxScale;
        double fracSeconds = ts.seconds - seconds;
        ts.seconds= seconds;
        ts.micros= (int)(fracSeconds * 1e6);
        
        TimeUtil.carry(ts);
        
        array[YEAR_FIELD_INDEX] = new Integer(ts.year);
        array[MONTH_FIELD_INDEX] = new Integer(ts.month);
        array[DAY_FIELD_INDEX] = new Integer(ts.day);
        array[DOY_FIELD_INDEX] = new Integer(ts.doy);
        array[HOUR_FIELD_INDEX] = new Integer(ts.hour);
        array[MINUTE_FIELD_INDEX] = new Integer(ts.minute);
        array[SECONDS_FIELD_INDEX] = new Integer((int)ts.seconds);
        for (int i = TIMESTAMP_FIELD_COUNT; i < array.length; i++) {
            int value = (int)Math.round(fracSeconds * Math.pow(10, scaleSeconds[i - TIMESTAMP_FIELD_COUNT]));
            array[i] = new Integer(value);
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
