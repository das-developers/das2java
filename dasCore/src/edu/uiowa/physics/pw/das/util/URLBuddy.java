/* File: URLBuddy.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on April 7, 2004, 11:34 AM
 *      by Edward E. West <eew@space.physics.uiowa.edu>
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

package edu.uiowa.physics.pw.das.util;

import java.util.*;
import java.util.regex.*;

/**
 *
 * @author  eew
 */
public class URLBuddy {
    
    private static final String ALPHA = "[A-Za-z]";
    private static final String DIGIT = "[0-9]";
    private static final String HEX = "[" + DIGIT + "A-F]";
    
    public static final Pattern VALID_QUERY_NAME = Pattern.compile(
        ALPHA + "[" + ALPHA + DIGIT + "-_:.]*"
    );
    public static final Pattern VALID_QUERY_VALUE = Pattern.compile(
        "(?:[" + ALPHA + DIGIT + "\\.\\-\\*\\_\\+]|\\%(" + HEX + HEX + "))*"
    );
    
    /** Creates a new instance of URLBuddy */
    public URLBuddy() {
    }
    
    public static String encodeUTF8(String str) {
        try {
            return java.net.URLEncoder.encode(str, "UTF-8");
        }
        catch (java.io.UnsupportedEncodingException uee) {
            //All JVM's are required to support UTF-8
            throw new RuntimeException(uee);
        }
    }
    
    public static String decodeUTF8(String str) {
        try {
            return java.net.URLDecoder.decode(str, "UTF-8");
        }
        catch (java.io.UnsupportedEncodingException uee) {
            //All JVM's are required to support UTF-8
            throw new RuntimeException(uee);
        }
    }
    
    /** Returns an unmodifiable map representing the query string passed in.
     * each key is a name from the string and each value is the url encoded
     * value for the key.
     *@param str an URLEncoded query string
     */
    public static Map parseQueryString(String str) {
        HashMap map = new HashMap();
        String[] tokens = str.split("\\&");
        for (int i = 0; i < tokens.length; i++) {
            int eqIndex = tokens[i].indexOf('=');
            if (eqIndex == -1) {
                throwUnexpectedToken(tokens[i], str, "name/value pair");
            }
            String name = tokens[i].substring(0, eqIndex);
            String value = tokens[i].substring(eqIndex + 1);
            if (!validName(name)) {
                throwUnexpectedToken(name, str, "valid name");
            }
            if (!validValue(value)) {
                throwUnexpectedToken(name, str, "url encoded value");
            }
            value = decodeUTF8(value);
            map.put(name, value);
        }
        return Collections.unmodifiableMap(map);
    }
    
    private static final void throwUnexpectedToken(String token, String input, String expecting) {
        int index = input.indexOf(token);
        StringBuffer messageBuffer = new StringBuffer();
        messageBuffer.append("Error parsing query string: Expecting ");
        messageBuffer.append(expecting).append(", found '");
        messageBuffer.append(token).append("'\n");
        messageBuffer.append("Input: ").append(input).append('\n');
        messageBuffer.append("       ");
        for (int i = 0; i < index; i++) {
            messageBuffer.append('.');
        }
        messageBuffer.append('^');
        throw new IllegalArgumentException(messageBuffer.toString());
    }
    
    public static String formatQueryString(Map m) {
        StringBuffer query = new StringBuffer();
        for (Iterator i = m.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            String name = (String)entry.getKey();
            String value = (String)entry.getValue();
            if (!validName(name)) {
                throw new IllegalArgumentException("'" + name + "' is not a valid query name.");
            }
            value = encodeUTF8(value);
            query.append(name).append('=').append(value).append('&');
        }
        if (query.charAt(query.length() - 1) == '&') {
            query.deleteCharAt(query.length() - 1);
        }
        return query.toString();
    }
    
    private static boolean validName(String name) {
        Matcher m = VALID_QUERY_NAME.matcher(name);
        return m.matches();
    }
    
    private static boolean validValue(String value) {
        Matcher m = VALID_QUERY_VALUE.matcher(value);
        return m.matches();
    }

}
