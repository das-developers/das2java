/* File: TimeLocationUnits.java
 * Copyright (C) 2002-2003 The University of Iowa
 * Created by: Jeremy Faden <jbf@space.physics.uiowa.edu>
 *             Jessica Swanner <jessica@space.physics.uiowa.edu>
 *             Edward E. West <eew@space.physics.uiowa.edu>
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

package org.das2.datum;

import org.das2.datum.format.DatumFormatterFactory;
import org.das2.datum.format.TimeDatumFormatterFactory;

/**
 *
 * @author  jbf
 */
public class TimeLocationUnits extends LocationUnits {
    
    /* TimeLocationUnits class is introduced because it is often necessary to
     * easily identify a time quantity, for instance when deciding whether to
     * use a timeAxis or not.  (TimeAxis is no longer a class, but we use a 
     * special tickV for time units.)
     */        
    
    public TimeLocationUnits( String id, String description, Units offsetUnits, Basis basis ) {
        super(id,description,offsetUnits,basis);
    }

    public DatumFormatterFactory getDatumFormatterFactory() {
        return TimeDatumFormatterFactory.getInstance();
    }
        
    public Datum parse(String s) throws java.text.ParseException {
        return TimeUtil.toDatum(TimeUtil.parseTime(s));
    }
    
    public String getTimeZone() {
        return "UT";
    }
          
}
