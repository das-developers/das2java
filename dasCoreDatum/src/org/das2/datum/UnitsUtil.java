/*
 * UnitsUtil.java
 *
 * Created on December 1, 2004, 10:25 PM
 */

package org.das2.datum;

/**
 * Useful operations for units, and tests for Steven's Levels of Measurement.
 * @author  Jeremy
 */
public class UnitsUtil {
    
    /**
     * returns true if the unit is used to measure distance in a logarithmic
     * space, such as decibels or percent increase.  Note Units.dimensionless
     * are not considered ratiometric.  (Of course, all ratiometic
     * units are dimensionless...)
     */
    public static final boolean isRatiometric( Units unit ) {
        return unit!=Units.dimensionless && unit.isConvertableTo(Units.logERatio);
    }
    
    /**
     * returns true if the unit describes a location in time, as in us2000.
     */
    public static final boolean isTimeLocation( Units unit ) {
        return unit==Units.us2000 || unit.isConvertableTo(Units.us2000);
    }
    
    /**
     * returns true if the unit is a ratio measurement, meaning there is a physical zero
     * and you can make meaningful ratios between arbitrary numbers.  All operations
     * like add, multiply and divide are allowed.  (What about negative numbers?  We
     * need a statistician!)
     * Examples include "5 km" or "0.2/cc" and "15 counts"
     * See http://en.wikipedia.org/wiki/Level_of_measurement
     * @param unit
     * @return
     */
    public static final boolean isRatioMeasurement( Units unit ) {
        return !(unit instanceof EnumerationUnits) && unit.getOffsetUnits()==unit;
    }
    
    /**
     * returns true if the unit is a interval measurement, meaning the choice of
     * zero is arbitrary.  Subtraction and comparison are allowed, but addition, 
     * multiplication and division are invalid operators.  
     * Examples include "2008-04-09T14:27:00Z" and 15 deg W Longitude.
     * See http://en.wikipedia.org/wiki/Level_of_measurement
     * @param unit
     * @return
     */
    public static final boolean isIntervalMeasurement( Units unit ) {
        return !(unit instanceof EnumerationUnits) && unit.getOffsetUnits()!=unit;
    }
    /**
     * returns true if the unit is nominal, meaning that Datums with this unit
     * can only be equal or not equal.  Currently all nominal data is stored
     * as ordinal data, so this always returns false.  
     * Examples include "Iowa City", and "Voyager 1".
     * See http://en.wikipedia.org/wiki/Level_of_measurement
     * @param unit
     * @return true if the unit is nominal.
     */
    public static final boolean isNominalMeasurement( Units unit ) {
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
    public static final boolean isOrdinalMeasurement( Units unit ) {
        return unit instanceof EnumerationUnits;
    }
    
    /**
     * returns the unit whose product with the parameter unit is unity.
     * @throws IllegalArgumentException if the units inversion is not known.
     *   (Presently this is only time units).
     *
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
        } else {
            throw new IllegalArgumentException( "units not supported: "+unit );
        }
    }
    
    /**
     * Special division operation that either does the Datum division if
     * possible, or returns the division of the magnitude parts of the
     * Datums plus the unit names "A/B", suitable for human consumption.
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
            return ""+(a/b)+" "+aUnits+" / " +bUnits;
        }
    }
    
    /**
     * attempt to perform the division of two Datums by looking for
     * convertible units or dimensionless.
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
            return bInvUnits.createDatum(a/b);
        } else {
            if ( !bUnits.isConvertableTo(aUnits) ) {
                throw new IllegalArgumentException("unable to calculate, b units not convertable to a");
            } else {
                UnitsConverter uc= bUnits.getConverter(aUnits);
                return Units.dimensionless.createDatum( a / uc.convert(b) );
            }
        }
    }
}
