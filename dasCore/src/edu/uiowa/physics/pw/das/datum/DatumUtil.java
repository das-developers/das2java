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

import edu.uiowa.physics.pw.das.DasApplication;
import edu.uiowa.physics.pw.das.datum.format.*;
import edu.uiowa.physics.pw.das.util.DasMath;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.regex.*;

/**
 *
 * @author  Edward West
 */
public final class DatumUtil {
    
    /** Creates a new instance of DatumUtil */
    private DatumUtil() {
    }
    
    /**
     *
     * @param minimum
     * @param maximum
     * @param nsteps
     * @return
     */    
    public static DatumFormatter bestFormatter(Datum minimum, Datum maximum, int nsteps) {
        
        Units units = minimum.getUnits();
        
        //Swap them if they are in the wrong order.
        if (maximum.lt(minimum)) {
            Datum tmp = maximum;
            maximum = minimum;
            minimum = tmp;
        }
        
        //Time units we will handle separately
        if ( units instanceof TimeLocationUnits ) {
            return bestTimeFormatter(minimum, maximum, nsteps);
        }
        
        DatumFormatterFactory factory = minimum.getUnits().getDatumFormatterFactory();
        try {
            if (!(factory instanceof DefaultDatumFormatterFactory)) {
                return factory.defaultFormatter();
            }
            double discernable= Math.abs( maximum.subtract(minimum).doubleValue(units) / nsteps );
            if (discernable < 0.001) {
                int power = Math.abs((int)Math.floor(DasMath.log10(discernable)));
                String formatString = exp(power);
                return factory.newFormatter(formatString);
            }
            else {
                int nFraction= -1 * (int)Math.floor(DasMath.log10(discernable));
                nFraction= nFraction<0 ? 0 : nFraction;
                String formatString = zeros(nFraction);
                return factory.newFormatter(formatString);
            }
        }
        catch (java.text.ParseException pe) {
            Logger logger = DasApplication.getDefaultApplication().getLogger();
            //Should not happen under normal circumstances, so bail.
            RuntimeException re = new RuntimeException(pe);
            logger.log(Level.SEVERE, pe.getMessage(), re);
            throw re;
        }
    }
    
    private static String exp(int power) {
        StringBuffer buffer = new StringBuffer(power+4);
        for (int i = 0; i < power - 1; i++) {
            buffer.append('#');
        }
        buffer.append("0.#E0");
        return buffer.toString();
    }
    
    private static String zeros(int count) {
        if (count <= 0) return "0";
        StringBuffer buff = new StringBuffer(count+2).append("0.");
        for (int index = 0; index < count; index++) {
            buff.append('0');
        }
        return buff.toString();
    }
    
    public static DatumFormatter bestTimeFormatter(Datum minimum, Datum maximum, int nsteps) {
        double secondsPerStep = maximum.subtract(minimum).doubleValue(Units.seconds) / ( nsteps );
        
        if (secondsPerStep < 1.) {
            return TimeDatumFormatter.MILLISECONDS;
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
    
    public static Datum createValid(java.lang.String s) {
        return Datum.create( Double.parseDouble(s), Units.dimensionless );
    }    

    public static double[] doubleValues( Datum[] datums, Units units ) {
        double[] result= new double[datums.length];
        for (int j=0; j<datums.length; j++ ) {
            result[j]= datums[j].doubleValue(units);
        }
        return result;
    }
        
    public static double[] doubleValues( Datum[] datums, Units[] unitsArray ) {
        double[] result= new double[datums.length];
        for (int j=0; j<datums.length; j++ ) {
            result[j]= datums[j].doubleValue(unitsArray[j]);
        }
        return result;
    }
    
    /* convert to human-friendly units.  Right now this will just convert us2000 to seconds. */
   /* public static Datum asOrderOneUnits(Datum d) {
        if ( d.getUnits()==Units.microseconds ) {
            return d.convertTo(Units.seconds);
        } else {
            return d;
        }
    }*/
    
    /**
     * This method takes the input datum and gets it as close to order one as
     * possible by trying all possible conversions.
     * @param d A datum that needs to have its units changed to order one units.
     * @return The order-one-ified Datum.
     */    
    public static Datum asOrderOneUnits(Datum d) {
        Units dunits = d.getUnits();
        
        // Don't use location units.
        if (dunits instanceof LocationUnits)
            return d;
        
        Units[] conversions = dunits.getConvertableUnits();
        
        double bestScore = 0;
        Datum bestDatum = d;
        for (int i = 0; i < conversions.length; i++) {
            Datum dd = d.convertTo(conversions[i]);
            Number n = dd.getValue();
            
            double nn = Math.abs(n.doubleValue());
            
            double score;
            if (n.doubleValue() > 1)
                score = 1/nn;
            else
                score = nn;
            
            if (score > bestScore) {
                bestScore = score;
                bestDatum = dd;
            }
        }
        return bestDatum;
    }
    
}
