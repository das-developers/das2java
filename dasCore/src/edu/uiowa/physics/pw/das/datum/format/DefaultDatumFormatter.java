/* File: DefaultDatumFormatter.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on October 1, 2003, 4:45 PM
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
import java.util.regex.*;

/** Formats Datum objects for printing and parses strings to Datum objects.
 *
 * @author  Edward West
 */
public class DefaultDatumFormatter extends DatumFormatter {
    
    private String formatString;
    
    private DecimalFormat format;
    
    /** Available for use by subclasses */
    protected DefaultDatumFormatter() {}
    
    /** Creates a new instance of DatumFormatter */
    public DefaultDatumFormatter(String formatString) throws ParseException {
        if (formatString.equals("")) {
            this.formatString = "";
            format = null;
        }
        else {
            this.formatString = formatString;
            format = new DecimalFormat(formatString);
        }
    }
    
    public String format(Datum datum) {
        if (format == null) {
            return Double.toString(datum.doubleValue(datum.getUnits()));
        }
        else {
            return format.format(datum.doubleValue(datum.getUnits()));
        }
    }
    
    public String grannyFormat(Datum datum) {
        String format= format(datum);
        if ( format.indexOf("E")!=-1 ) {
            int iE= format.indexOf("E");
            StringBuffer granny = new StringBuffer(format.length() + 4);
            String mant= format.substring(0,iE);
            if (Double.parseDouble(mant)!=1.0) {
                granny.append(mant).append("\u00d7");
            }
            granny.append("10").append("!A").append(format.substring(iE+1)).append("!N");
            format = granny.toString();
        }
        return format;
    }
    
    public String toString() {
        return formatString;
    }
}
