/* File: EnumerationDatumFormatter.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on September 29, 2003, 4:34 PM
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

/**
 *
 * @author  Edward West
 */
public class EnumerationDatumFormatter extends DatumFormatter {
    
    /** Creates a new instance of EnumerationDatumFormatter */
    public EnumerationDatumFormatter() {
    }
    
    public String toString() {
        return getClass().getName();
    }
    
    public String format(Datum datum) {
        if ( !( datum instanceof EnumerationDatum ) ) {
            throw new IllegalArgumentException("Argument is not an EnumerationDatum! ("+datum.getClass().getName()+")" );
        }
        return datum.toString();
    }
    
}
