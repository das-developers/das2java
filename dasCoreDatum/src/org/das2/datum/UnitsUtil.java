/*
 * UnitsUtil.java
 *
 * Created on December 1, 2004, 10:25 PM
 */

package org.das2.datum;

import java.text.DecimalFormat;

/**
 * Useful operations for units, and tests for Steven's Levels of Measurement.
 * @see http://www.statisticssolutions.com/data-levels-of-measurement/
 * @author  Jeremy
 */
public final class UnitsUtil {
    
    /**
     * this is a utility class which cannot be instantiated.
     */
    private UnitsUtil() {
    }
    
    /**
     * returns true if the unit is used to measure distance in a logarithmic
     * space, such as decibels or percent increase.  Note Units.dimensionless
     * are not considered ratiometric.  (Of course, all ratiometic
     * units are dimensionless...)
     *
     * Do not confuse this with isRatioMeasurement.  "5kg" is ratio measurement.
     * "105%" is ratiometric.
     * @param unit the unit
     * @return true if the unit is used to measure distance in a logarithmic space
     */
    public static boolean isRatiometric( Units unit ) {
        return unit!=Units.dimensionless && unit.isConvertibleTo(Units.logERatio);
    }
    
    /**
     * returns true if the unit describes a location in time, as in us2000 or
     * Units.lookupTimeUnits('seconds since 1989-12-13T00:00Z')
     * @param unit the unit to test
     * @return true if the unit describes a location in time
     */
    public static boolean isTimeLocation( Units unit ) {
        return unit==Units.us2000 || unit.isConvertibleTo(Units.us2000);
    }
    
    /**
     * returns true if the unit is a ratio measurement, meaning there is a physical zero
     * and you can make meaningful ratios between arbitrary numbers.  All operations
     * like add, multiply and divide are allowed.  
     * Examples include "5 km" or "0.2/cc" and "15 counts"
     * @see http://en.wikipedia.org/wiki/Level_of_measurement
     * @see http://www.statisticssolutions.com/data-levels-of-measurement/
     * @see https://www.questionpro.com/blog/nominal-ordinal-interval-ratio/
     * @param unit
     * @return
     */
    public static boolean isRatioMeasurement( Units unit ) {
        return !(unit instanceof EnumerationUnits) && unit.getOffsetUnits()==unit;
    }
    
    /**
     * returns true if the unit is a interval measurement, meaning the choice of
     * zero is arbitrary.  Subtraction and comparison are allowed, but addition, 
     * multiplication and division are invalid operators.  
     * Examples include "2008-04-09T14:27:00Z" and 15 deg W Longitude.
     * @see http://en.wikipedia.org/wiki/Level_of_measurement
     * @see http://www.statisticssolutions.com/data-levels-of-measurement/
     * @param unit
     * @return
     */
    public static boolean isIntervalMeasurement( Units unit ) {
        return !(unit instanceof EnumerationUnits) && unit.getOffsetUnits()!=unit;
    }

    /**
     * returns true if the unit is a interval measurement or is a ratio measurement,
     * and not a nominal or ordinal measurement.  These are things that are plotted
     * by showing a location on an axis.
     * See http://en.wikipedia.org/wiki/Level_of_measurement
     * Examples include "2008-04-09T14:27:00Z" and "5 km"
     * @param unit
     * @return
     */
    public static boolean isIntervalOrRatioMeasurement( Units unit ) {
        return !(unit instanceof EnumerationUnits);
    }

    /**
     * returns true if the unit is nominal, meaning that Datums with this unit
     * can only be equal or not equal.  Currently all nominal data is stored
     * as ordinal data, so this always returns false.  
     * Examples include "Iowa City", and "Voyager 1".
     * See http://en.wikipedia.org/wiki/Level_of_measurement
     * @param unit the unit
     * @return true if the unit is nominal.
     */
    public static boolean isNominalMeasurement( Units unit ) {
        return unit instanceof EnumerationUnits;
    }
    
    /**
     * returns true if the unit is ordinal, meaning that Datums with this unit
     * can only be equal or not equal, or compared.  subtract, add, multiply,
     * divide are invalid.
     * Examples include energy bin labels and quality measures.
     * See http://en.wikipedia.org/wiki/Level_of_measurement
     * @param unit
     * @return true if the unit is ordinal.
     */
    public static boolean isOrdinalMeasurement( Units unit ) {
        return unit instanceof EnumerationUnits;
    }
    
    /**
     * returns the unit whose product with the parameter unit is unity.
     * (Presently this is only supports time units like Hz&rarr;seconds).
     * @param unit the unit
     * @return the inverse unit, or throws exception if one is not known.
     * @throws IllegalArgumentException if the units inversion is not known.
     */
    public static Units getInverseUnit( Units unit ) {
        if ( unit==Units.seconds ) {
            return Units.hertz;
        } else if ( unit==Units.hertz ) {
            return Units.seconds;
        } else if ( unit==Units.dimensionless ) {
            return Units.dimensionless;
        } else if ( unit==Units.milliseconds ) {
            return Units.kiloHertz;
        } else if ( unit==Units.microseconds ) {
            return Units.megaHertz;
        } else if ( unit==Units.nanoseconds ) {
            return Units.gigaHertz;
        } else {
            if ( unit.isConvertibleTo(Units.seconds ) ) {
                UnitsConverter uc= unit.getConverter(Units.seconds);
                if ( uc==UnitsConverter.IDENTITY ) {
                    return Units.hertz;
                } 
                uc= unit.getConverter(Units.milliseconds);  // there's no way to check for scale=1000...
                if ( uc==UnitsConverter.IDENTITY ) {
                    return Units.kiloHertz;
                }
                uc= unit.getConverter(Units.microseconds);  // there's no way to check for scale=1000...
                if ( uc==UnitsConverter.IDENTITY ) {
                    return Units.megaHertz;
                }
                uc= unit.getConverter(Units.nanoseconds);  // there's no way to check for scale=1000...
                if ( uc==UnitsConverter.IDENTITY ) {
                    return Units.gigaHertz;
                } else {
                    throw new IllegalArgumentException( "units not supported: "+unit );
                }
            } else if ( unit.isConvertibleTo(Units.hertz ) ) {
                UnitsConverter uc= unit.getConverter(Units.hertz);
                if ( uc==UnitsConverter.IDENTITY ) {
                    return Units.seconds;
                }
                uc= unit.getConverter(Units.kiloHertz);
                if ( uc==UnitsConverter.IDENTITY ) {
                    return Units.milliseconds ;
                }
                uc= unit.getConverter(Units.megaHertz);
                if ( uc==UnitsConverter.IDENTITY ) {
                    return Units.microseconds;
                }
                uc= unit.getConverter(Units.gigaHertz);
                if ( uc==UnitsConverter.IDENTITY ) {
                    return Units.nanoseconds;
                } else {
                    throw new IllegalArgumentException( "units not supported: "+unit );
                }
            } else {
                throw new IllegalArgumentException( "units not supported: "+unit );
            }
        }
    }
    
    /**
     * Special division operation that either does the Datum division if
     * possible, or returns the division of the magnitude parts of the
     * Datums plus the unit names "A/B", suitable for human consumption.
     * @param aDatum the numerator
     * @param bDatum the denominator
     * @return String for the result, or "A/B"
     */
    public static String divideToString( Datum aDatum, Datum bDatum ) {
        try {
            Datum result= divide( aDatum, bDatum );
            return String.valueOf(result);
        } catch ( IllegalArgumentException e ) {
            Units aUnits= aDatum.getUnits();
            Units bUnits= bDatum.getUnits();
            double a= aDatum.doubleValue(aUnits);
            double b= bDatum.doubleValue(bUnits);
            DecimalFormat df= new DecimalFormat("0.000E0");
            return ""+df.format(a/b)+" "+aUnits+" / " +bUnits;
        }
    }
    
    /**
     * attempt to perform the division of two Datums by looking for
     * convertible units or dimensionless.
     * @param aDatum the numerator
     * @param bDatum the denominator
     * @return the result
     */
    public static Datum divide( Datum aDatum, Datum bDatum ) {
        Units bUnits= bDatum.getUnits();
        Units aUnits= aDatum.getUnits();
        
        Units bInvUnits;
        try{
            bInvUnits= getInverseUnit(bUnits);
        } catch ( IllegalArgumentException e ) {
            bInvUnits= null;
        }
        
        double a= aDatum.doubleValue(aUnits);
        double b= bDatum.doubleValue(bUnits);
        
        if ( bUnits==Units.dimensionless ) {
            return aUnits.createDatum( a/b );
        } else if ( aUnits==Units.dimensionless ) {
            if ( bInvUnits==null ) {
                throw new IllegalArgumentException("unable to calculate, b units not convertable to a");
            } else {
                return bInvUnits.createDatum(a/b);
            }
        } else {
            if ( !bUnits.isConvertibleTo(aUnits) ) {
                throw new IllegalArgumentException("unable to calculate, b units not convertable to a");
            } else {
                UnitsConverter uc= bUnits.getConverter(aUnits);
                return Units.dimensionless.createDatum( a / uc.convert(b) );
            }
        }
    }
}
