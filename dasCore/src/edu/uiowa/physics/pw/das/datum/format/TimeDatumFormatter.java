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

package edu.uiowa.physics.pw.das.datum.format;

import edu.uiowa.physics.pw.das.datum.*;

import java.text.*;
import java.util.*;
import java.util.regex.*;

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

    public static final TimeDatumFormatter DEFAULT;
    public static final TimeDatumFormatter DAYS;
    public static final TimeDatumFormatter MINUTES;
    public static final TimeDatumFormatter SECONDS;
    public static final TimeDatumFormatter MICROSECONDS;
    
    //Initialize final constants
    static {
        try {
            DEFAULT = new TimeDatumFormatter("yyyy'-'MM'-'dd'T'HH':'mm':'ss.sss'Z'");
            DAYS = new TimeDatumFormatter("yyyy'-'MM'-'dd");
            MINUTES = new TimeDatumFormatter("HH':'mm");
            SECONDS = new TimeDatumFormatter("HH':'mm':'ss");
            MICROSECONDS = new TimeDatumFormatter("HH':'mm':'ss.sss");
        }
        catch (ParseException pe) {
            throw new RuntimeException(pe);
        }
    }
    
    private String formatString;
    
    private MessageFormat format;
    
    /** Creates a new instance of TimeDatumFormatter */
    public TimeDatumFormatter(String formatString) throws ParseException {
        this.formatString = formatString;
        format = new MessageFormat(parseTimeFormatString(formatString));
    }
    
    public String toString() {
        return formatString;
    }
    
    public String format(Datum datum) {
        TimeUtil.TimeStruct ts = TimeUtil.toTimeStruct(datum);
        Number[] array = timeStructToArray(ts);
        return format.format(array);
    }
    
    private static String parseTimeFormatString(String input) throws ParseException {
        final String formatPattern = "(([yMDdHm])\\2*)";
        final String secondsPattern = "(s+(?:\\.s+)?)";
        final String literalPattern = "('(?:[^']|'')*')";
        Pattern token = Pattern.compile(
            formatPattern + "|" + secondsPattern + "|" + literalPattern
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
            String seconds = matcher.group(3);
            String literal = matcher.group(4);
            if (format != null) {
                switch (format.charAt(0)) {
                    case 'y': {
                        formatString.append("{").append(YEAR_FIELD_INDEX)
                            .append(",number,0000}");
                    } break;
                    case 'M': {
                        formatString.append("{").append(MONTH_FIELD_INDEX)
                            .append(",number,").append(format.replace('M', '0'))
                            .append("}");
                    } break;
                    case 'D': {
                        formatString.append("{").append(DOY_FIELD_INDEX)
                            .append(",number,000}");
                    } break;
                    case 'd': {
                        formatString.append("{").append(DAY_FIELD_INDEX)
                            .append(",number,").append(format.replace('d', '0'))
                            .append("}");
                    } break;
                    case 'H': {
                        formatString.append("{").append(HOUR_FIELD_INDEX)
                            .append(",number,").append(format.replace('H', '0'))
                            .append("}");
                    } break;
                    case 'm': {
                        formatString.append("{").append(MINUTE_FIELD_INDEX)
                            .append(",number,").append(format.replace('m', '0'))
                            .append("}");
                    } break;
                    default: break;
                }
            }
            else if (seconds != null) {
                formatString.append("{").append(SECONDS_FIELD_INDEX)
                    .append(",number,").append(seconds.replace('s', '0'))
                    .append("}");
            }
            else if (literal != null) {
                literal = literal.substring(1, literal.length() - 1);
                literal = literal.replaceAll("''", "'");
                formatString.append(literal);
            }
            from = matcher.end();
        }
        return formatString.toString();
    }
    
    private static Number[] timeStructToArray(TimeUtil.TimeStruct ts) {
        Number[] array = new Number[TIMESTAMP_FIELD_COUNT];
        array[YEAR_FIELD_INDEX] = new Integer(ts.year);
        array[MONTH_FIELD_INDEX] = new Integer(ts.month);
        array[DAY_FIELD_INDEX] = new Integer(ts.day);
        array[DOY_FIELD_INDEX] = new Integer(ts.doy);
        array[HOUR_FIELD_INDEX] = new Integer(ts.hour);
        array[MINUTE_FIELD_INDEX] = new Integer(ts.minute);
        array[SECONDS_FIELD_INDEX] = new Double(ts.seconds);
        return array;
    }

}
