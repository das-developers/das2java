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
    
    public static DatumFormatter bestFormatter( DatumVector datums ) {
        double[] array;
        Units units;
        
        if ( datums.getUnits() instanceof EnumerationUnits ) {
            return EnumerationDatumFormatterFactory.getInstance().defaultFormatter();
        }
        
        if ( datums.getUnits() instanceof LocationUnits ) {
            array= new double[ datums.getLength() ];
            units= ((LocationUnits)datums.get(0).getUnits()).getOffsetUnits();
            array[0]= 0.;
            for ( int i=1; i<datums.getLength(); i++ ) {
                array[i]= datums.get(i).subtract(datums.get(0)).doubleValue(units);
            }
        } else {
            units= datums.getUnits();            
            array= datums.toDoubleArray(units);            
        }
        
        double limit= DasMath.exp10( (int)DasMath.log10( DasMath.max( array ) ) - 7 );
        double gcd= DasMath.gcd( array, limit );
        
        int smallestExp=99;
        int ismallestExp=-1;
        for ( int j=0; j<datums.getLength(); j++ ) {
            double d= datums.get(j).doubleValue(units);
            if ( Math.abs(d)>(gcd*0.1) ) { // don't look at fuzzy zero
                int ee= (int)Math.floor(0.05+DasMath.log10(Math.abs(d)));
                if ( ee<smallestExp ) {
                    smallestExp=ee;
                    ismallestExp= j;
                }
            }
        }
                
        Datum resolution= units.createDatum(gcd);
        Datum base= datums.get(ismallestExp);
        if ( base.lt(units.createDatum(0.) ) ) {
            base= base.multiply(-1);
        }
        return bestFormatter( base, base.add( resolution ), 1 );
    }
    
    public static int fractionalDigits( Datum resolution ) {
        /* returns the number of digits after the decimal place.  This is negative when 
         * the resolution is a integer multiple of 10,100, etc...
         */ 
        double d= Math.abs( resolution.doubleValue() );
        double frac= d-Math.floor(d);
        if ( frac==0. ) {
            int e=0;
            int emax= (int)DasMath.log10(d+0.1);
            while ( e<emax && ( d/DasMath.exp10(e) % 1 == 0. ) ) e++;
            return -1*(e);
        } else {
            int e= (int)Math.floor( DasMath.log10(frac)+0.0001 );
            int emin= (int)DasMath.log10(d) - 18;            
            boolean notDone= true;
            while ( e>emin && notDone  ) {
                double remain= frac/DasMath.exp10(e) % 1;
                if ( remain>0.5 ) remain= 1.0 - remain;
                notDone= remain > (1/10000.);
                e--;
            }
            return -1*(e+1);
        }
    }
 
    public static DatumFormatter limitLogResolutionFormatter( Datum minimum, Datum maximum, int nsteps ) {        
        Units units = minimum.getUnits();
        
        if ( units instanceof TimeLocationUnits ) {
            return bestTimeFormatter(minimum, maximum, nsteps);
        }

        double logmin= DasMath.log10( minimum.doubleValue( units ) );
        double logmax= DasMath.log10( maximum.doubleValue( units ) );        
                
        double percent= ( DasMath.exp10( ( logmax - logmin ) / nsteps ) - 1. ) * 100;
                       
        int nFraction= 2 - (int)Math.floor( 0.05 + DasMath.log10( percent ) );
        
        nFraction= nFraction<0 ? 0 : nFraction;
        String formatString = exp(nFraction);
        DatumFormatterFactory factory = units.getDatumFormatterFactory();
        try {
            return factory.newFormatter(formatString);
        } catch ( java.text.ParseException e ) {
            throw new RuntimeException(e);
        }
    }
    
    public static DatumFormatter limitResolutionFormatter( Datum minimum, Datum maximum, int nsteps ) {        
        Units units = minimum.getUnits();
        
        if ( units instanceof TimeLocationUnits ) {
            return bestTimeFormatter(minimum, maximum, nsteps);
        }

        Datum resolution= maximum.subtract(minimum).divide(nsteps);
        double discernable= resolution.doubleValue(units);        
        int nFraction= -1 * (int)Math.floor(0.05+DasMath.log10(discernable));
        
        nFraction= nFraction<0 ? 0 : nFraction;
        String formatString = zeros(nFraction);
        DatumFormatterFactory factory = units.getDatumFormatterFactory();
        try {
            return factory.newFormatter(formatString);
        } catch ( java.text.ParseException e ) {
            throw new RuntimeException(e);
        }
    }
    
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
            
            int fracDigits= fractionalDigits( maximum.subtract(minimum).divide(nsteps) );
            
            int smallestExp=99;
            
            double discernable= DasMath.exp10(-1*fracDigits);
            
            Datum step= maximum.subtract(minimum).divide( nsteps );
            double dstep= step.doubleValue(units);
            for ( int j=0; j<nsteps; j++ ) {
                double d= minimum.add(step.multiply(j)).doubleValue(units);
                if ( Math.abs(d)>(discernable*0.1) ) { // don't look at fuzzy zero
                    int ee= (int)Math.floor(0.05+DasMath.log10(Math.abs(d)));
                    if ( ee<smallestExp ) smallestExp=ee;
                }
            }
                        
            if ( smallestExp < -3 || smallestExp > 3 ) {               
                return new ExponentialDatumFormatter( smallestExp - (-1*fracDigits) +1 , smallestExp );
            } else {
                int nFraction= -1 * (int)Math.floor(0.05+DasMath.log10(discernable));
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
        else if (secondsPerStep < 3600.) {
            return TimeDatumFormatter.MINUTES;
        }
        else if (secondsPerStep < 86400. ) {
            return TimeDatumFormatter.HOURS;
        }
        else if ( secondsPerStep < 28*86400.0 ) {
            return TimeDatumFormatter.DAYS;
        }
        else if ( secondsPerStep < 31557600.0 ) {
            return TimeDatumFormatter.MONTHS;
        }
        else {
            return TimeDatumFormatter.YEARS;
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
