/* File: DatumUtil.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on September 25, 2003, 2:45 PM
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

package edu.uiowa.physics.pw.das.datum;

import edu.uiowa.physics.pw.das.datum.format.*;
import edu.uiowa.physics.pw.das.util.DasMath;

import java.util.regex.*;

/**
 *
 * @author  Edward West
 */
public final class DatumUtil {
    
    /** Creates a new instance of DatumUtil */
    private DatumUtil() {
    }
    
    public static DatumFormatter bestFormatter(Datum minimum, Datum maximum, int nsteps) {
        if ( minimum.getUnits() != minimum.getUnits() ) {
            throw new IllegalArgumentException( "Units don't match!" );
        }
        //Swap them if they are in the wrong order.
        if (maximum.lt(minimum)) {
            Datum tmp = maximum;
            maximum = minimum;
            minimum = tmp;
        }
        
        //Time units we will handle separately
        if ( minimum.getUnits() instanceof TimeLocationUnits ) {
            return bestTimeFormatter(minimum, maximum, nsteps);
        } else {
            DatumFormatterFactory factory = minimum.getUnits().getDatumFormatterFactory();
            if (!(factory instanceof DefaultDatumFormatterFactory)) {
                return factory.defaultFormatter();
            }
            double discernable= Math.abs( maximum.subtract(minimum).getValue() / nsteps );
            int nFraction= -1 * (int)Math.floor(DasMath.log10(discernable));
            nFraction= nFraction<0 ? 0 : nFraction;
            String formatString = "0." + zeros(nFraction);
            try {
                return factory.newFormatter(formatString);
            }
            catch (java.text.ParseException pe) {
                //Should not happen under normal circumstances, so bail.
                throw new RuntimeException(pe);
            }
        }
    }
    
    private static String zeros(int count) {
        StringBuffer buff = new StringBuffer(count);
        for (int index = 0; index < count; index++) {
            buff.setCharAt(index, '0');
        }
        return buff.toString();
    }
    
    public static DatumFormatter bestTimeFormatter(Datum minimum, Datum maximum, int nsteps) {
        double secondsPerStep = maximum.subtract(minimum).convertTo(Units.seconds).getValue() / nsteps;
        if (secondsPerStep < 1.) {
            return TimeDatumFormatter.MICROSECONDS;
        }
        else if (secondsPerStep < 60.) {
            return TimeDatumFormatter.SECONDS;
        }
        else if (secondsPerStep < 86400.) {
            return TimeDatumFormatter.MINUTES;
        }
        else {
            return TimeDatumFormatter.DAYS;
        }
    }
    
    
}
