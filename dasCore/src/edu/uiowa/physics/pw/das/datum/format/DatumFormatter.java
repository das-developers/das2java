/* File: DatumFormatter.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on September 24, 2003, 4:45 PM
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
public abstract class DatumFormatter {
    
    /** Available for use by subclasses */
    protected DatumFormatter() {}
    
    public abstract String format(Datum datum);
    
    /** Returns the datum formatted as a String with special formatting
     * characters.
     * The default implementation just returns the result of
     * {@link #format(edu.uiowa.physics.pw.das.datum.Datum)}
     */
    public String grannyFormat(Datum datum) {
        return format(datum);
    }
    
}
