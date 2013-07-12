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

package org.das2.datum;

import java.text.ParseException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.das2.datum.format.DatumFormatter;
import org.das2.datum.format.DatumFormatterFactory;
import org.das2.datum.format.DefaultDatumFormatterFactory;
import org.das2.datum.format.EnumerationDatumFormatterFactory;
import org.das2.datum.format.ExponentialDatumFormatter;
import org.das2.datum.format.TimeDatumFormatter;

/**
 *
 * @author  Edward West
 */
public final class DatumUtil {

    private static final Logger logger = LoggerManager.getLogger("datum");

    /** Creates a new instance of DatumUtil */
    private DatumUtil() {
    }
    
    /**
     * copy of DasMath.max, so that this can be independent of org.das2.util.
     * @param A
     * @return
     */
    private static double max( double[] A ) {
        double max= A[0];
        for ( int i=0; i<A.length; i++ ) {
            max= ( max > A[i] ? max : A[i] );
        }
        return max;
    }

    private static double gcd( double a, double d, double error ) {

        if ( error>0 ) {
            a= Math.round( a/error );
            d= Math.round( d/error );
        }

        if ( a<d ) {
            double t= a;
            a= d;
            d= t;
        }

        if ( d==0 ) {
            if ( error>0 ) {
                return a * error;
            } else {
                return a;
            }
        }

        double r= a % d;

        int iterations=0;

        while ( r > 0 && iterations<15 ) {
            d= r;
            r= a % d;
            iterations++;
        }

        if ( error>0 ) {
            return d * error;
        } else {
            return d;
        }
    }


   /*
    * Returns the greatest common divisor of a group of numbers.  This is useful for
    * a number of visualization techniques, for instance when you need to integerize
    * your data, the binsize should be the gcd.  An error parameter is provided to
    * avoid numerical noise, and in case there is a granularity that needn't be
    * surpassed.
    *
    * See org.das2.util.DasMath
    */
    private static double gcd( double[] A, double error ) {
        double guess= A[0];

        double result= guess;

        for ( int i=1; i<A.length; i++ ) {
            result= gcd( result, A[i], error );
        }

        return result;
    }

    public static DatumFormatter bestFormatter( DatumVector datums ) {
        double[] array;
        Units units;
        
        if ( datums.getUnits() instanceof EnumerationUnits ) {
            return EnumerationDatumFormatterFactory.getInstance().defaultFormatter();
        }
        
        if ( datums.getUnits() instanceof TimeLocationUnits ) {
            Datum t1= datums.get(0);
            int nticks= datums.getLength();
            Datum t2= datums.get(nticks-1);
            return DatumUtil.bestTimeFormatter(t1,t2,nticks-1);
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
        
        double limit= Math.pow( 10, (int)Math.log10( max( array ) ) - 7 );
        double gcd= gcd( array, limit );
        
        int smallestExp=99;
        int ismallestExp=-1;
        for ( int j=0; j<datums.getLength(); j++ ) {
            double d= datums.get(j).doubleValue(units);
            if ( Math.abs(d)>(gcd*0.1) ) { // don't look at fuzzy zero
                int ee= (int)Math.floor(0.05+Math.log10(Math.abs(d)));
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
        int DOUBLE_DIGITS= 10;  // this is <15 because math operations introduce noise in more significant digits
        double d= Math.abs( resolution.doubleValue() );
        int e= (int)Math.floor( Math.log10(d)+0.0001 );
        long i= (long)(d/(Math.pow(10,e-(DOUBLE_DIGITS-1)))+0.5);
        int nzero;
        for ( nzero=1; nzero<16; nzero++ ) {
            if ( i % Math.pow(10,nzero) != 0. ) {
                break;
            }
        }
        nzero= nzero-1;        
        return (DOUBLE_DIGITS-1-nzero)-e;
    }
        
    public static DatumFormatter limitLogResolutionFormatter( Datum minimum, Datum maximum, int nsteps ) {
        Units units = minimum.getUnits();
        
        if ( units instanceof TimeLocationUnits ) {
            return bestTimeFormatter(minimum, maximum, nsteps);
        }
        
        double logmin= Math.log10( minimum.doubleValue( units ) );
        double logmax= Math.log10( maximum.doubleValue( units ) );
        
        double percent= ( Math.pow( 10, ( logmax - logmin ) / nsteps ) - 1. ) * 100;
        
        int nFraction= 2 - (int)Math.floor( 0.05 + Math.log10( percent ) );
        
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
        int nFraction= -1 * (int)Math.floor(0.05+Math.log10(discernable));
        
        nFraction= nFraction<0 ? 0 : nFraction;
        String formatString = zeros(nFraction);
        DatumFormatterFactory factory = units.getDatumFormatterFactory();
        try {
            return factory.newFormatter(formatString);
        } catch ( java.text.ParseException e ) {
            throw new RuntimeException(e);
        }
    }

    /**
     * return a DatumFormatter that efficiently formats evenly-spaced datums
     * from minimum to maximum with nstep intervals.  (Interval size is
     * maximum.subtract(minimum).divide(nsteps))
     * @param minimum
     * @param maximum
     * @param nsteps the number of sub intervals
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
            
            int fracDigits= fractionalDigits( maximum.subtract(minimum).divide(nsteps) );
            
            int smallestExp=99;
            
            double discernable= Math.pow(10,-1*fracDigits);
            
            Datum step= maximum.subtract(minimum).divide( nsteps );
            for ( int j=0; j<nsteps; j++ ) {
                double d= minimum.add(step.multiply(j)).doubleValue(units);
                if ( Math.abs(d)>(discernable*0.1) ) { // don't look at fuzzy zero
                    int ee= (int)Math.floor(0.05+Math.log10(Math.abs(d)));
                    if ( ee<smallestExp ) smallestExp=ee;
                }
            }
            if ( smallestExp < -60 || smallestExp > 60 ) {
                return DefaultDatumFormatterFactory.getInstance().defaultFormatter();
            } else if ( smallestExp < -3 || smallestExp > 3 ) {
                return new ExponentialDatumFormatter( smallestExp - (-1*fracDigits) +1 , smallestExp );
            } else {
                int nFraction= -1 * (int)Math.floor(0.05+Math.log10(discernable));
                nFraction= nFraction<0 ? 0 : nFraction;
                String formatString = zeros(nFraction);
                return factory.newFormatter(formatString);
            }
        }
        catch (java.text.ParseException pe) {
            //Should not happen under normal circumstances, so bail.
            RuntimeException re = new RuntimeException(pe);
            logger.log(Level.SEVERE, pe.getMessage(), re);
            throw re;
        }
    }
    
    private static String exp(int power) {
        StringBuilder buffer = new StringBuilder(power+4);
        for (int i = 0; i < power - 1; i++) {
            buffer.append('#');
        }
        buffer.append("0.#E0");
        return buffer.toString();
    }
    
    private static final String zeros100= "0.00000000000000000000"
            + "0000000000000000000000000000000000000000"
            + "0000000000000000000000000000000000000000";
    public static String zeros(int count) {
        if ( count < 0 ) return "0";
        else if ( count <= 100 ) return zeros100.substring(0,count+2);
        else {
           StringBuffer buff = new StringBuffer(count+2).append("0.");
           for (int index = 0; index < count; index++) {
                buff.append('0');
           }
           return buff.toString();
        }
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
        else if ( secondsPerStep < 31*86400.0 ) {
            return TimeDatumFormatter.DAYS;
        }
        else if ( secondsPerStep < 31557600.0 ) {
            return TimeDatumFormatter.MONTHS;
        }
        else {
            return TimeDatumFormatter.YEARS;
        }
    }

    /**
     * Split the string to separate magnitude component from units component.
     * "5" -> [ "5", "" ]
     * "5 m" -> [ "5", "m" ]
     * "5m" -> [ "5", "m" ]
     * "4.5m^2" -> [ "4.5", "m^2" ] 
     * "4.5e6m^2" -> [ "4.5e6", "m^2" ]
     * "-1" -> [ "-1", "" ]
     * "-1s" -> [ "-1", "s" ]
     * "-1.0e-6s" -> [ "-1e-6", "s" ]
     * "5 Deg N" ->  [ "5", "Deg N" ]
     * " 10 days" -> [ "10", "days" ]
     * See http://jfaden.net:8080/hudson/job/autoplot-test037/ws/splitDatumString.jy
     * @param s the string to break up
     * @return two element array
     */
    public static String[] splitDatumString( String s ) {
        s= s.trim();
        Pattern p= Pattern.compile("([-+]?[0-9]*(\\.[0-9]*)?([eE][-+]?[0-9]+)?)(.*)");
        String[] ss= new String[2];
        Matcher m= p.matcher(s);
        if ( m.find() ) {
            ss[0]= m.group(1);
            ss[1]= m.group(4).trim();
        }
        return ss;
    }

    /**
     * attempt to parse the string as a datum.  Note that if the
     * units aren't specified, then of course the Datum will be
     * assumed to be dimensionless.
     * @throws ParseException when the double can't be parsed or the units aren't recognized.
     */
    public static Datum parse(java.lang.String s) throws ParseException {
        String[] ss= splitDatumString(s);
        Units units;
        if ( ss.length==1 ) {
                units= Units.dimensionless;
        } else {
            try {
                units= Units.getByName(ss[1]);
            } catch ( IllegalArgumentException e ) {
                throw new ParseException( e.getMessage(), 0 );
            }
        }

        return Datum.create( Double.parseDouble(ss[0]), units );
    }
    
    public static Datum parseValid(java.lang.String s) {
        try {
            return parse( s );
        } catch ( ParseException e ) {
            throw new RuntimeException(e);        
        }
    }

    /**
     * create a dimensionless datum by parsing the string.
     * See TimeUtil.createValid( String stime ).
     * @param s
     * @return
     */
    public static Datum createValid( String s ) {
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
        
        if ( dunits==Units.dimensionless ) return d;                
        if ( dunits==Units.dB ) return d;
        if ( dunits instanceof LocationUnits ) return d;
        if ( dunits instanceof EnumerationUnits ) return d;
        
        Units[] conversions = dunits.getConvertableUnits();
        
        double bestScore = 1;
        Datum bestDatum = d;
        for (int i = 0; i < conversions.length; i++) {
            Datum dd = d.convertTo(conversions[i]);
            Number n = dd.getValue();

            if ( n.equals( d.getValue() ) ) continue;

            double nn = Math.abs(n.doubleValue());
            
            double score;
            if (nn > 100)  // favor 72 seconds over 1.2 minutes
                score = 100/nn;
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
