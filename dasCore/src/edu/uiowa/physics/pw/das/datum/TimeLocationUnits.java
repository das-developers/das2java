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

package edu.uiowa.physics.pw.das.datum;

import edu.uiowa.physics.pw.das.datum.format.*;

/**
 *
 * @author  jbf
 */
public class TimeLocationUnits extends LocationUnits {
    
    /* TimeLocationUnits class is introduced because it is often necessary to
     * easily identify a time quantity, for instance when deciding whether to
     * use a timeAxis or not.
     */
    
    public TimeLocationUnits( String id, String description, Units offsetUnits ) {
        super(id,description,offsetUnits);
    }

    public DatumFormatterFactory getDatumFormatterFactory() {
        return TimeDatumFormatterFactory.getInstance();
    }
    
    public Datum parse(String s) throws java.text.ParseException {
        return TimeUtil.toDatum(TimeUtil.parseTime(s));
    }
    
}
