/*
 * UnitsUtil.java
 *
 * Created on December 1, 2004, 10:25 PM
 */

package edu.uiowa.physics.pw.das.datum;

/**
 *
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
        return unit.isConvertableTo(Units.us2000);
    }
    
    /**
     * returns the unit whose product with the parameter unit is
     * unity.
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
     * possible, or returns the division of the magitude parts of the
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
     * convertable units or dimensionless.
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
