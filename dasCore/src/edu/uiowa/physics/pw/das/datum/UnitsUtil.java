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

    public static Units getInverseUnit( Units unit ) {
        if ( unit==Units.seconds ) {
            return Units.hertz;
        } else if ( unit==Units.dimensionless ) {
            return Units.dimensionless;
        } else if ( unit==Units.microseconds ) {
            return Units.megaHertz;
        } else {
            throw new IllegalArgumentException( "units not supported: "+unit );
        }
    }

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
        } else {
            UnitsConverter uc= bUnits.getConverter(aUnits);
            if ( uc==null ) {
                if ( bInvUnits!=null ) {
                    UnitsConverter uc2= bInvUnits.getConverter(aUnits);
                    if ( uc2!=null ) {
                        return aUnits.createDatum( a * uc2.convert( 1./b )  );
                    } else {
                        throw new IllegalArgumentException("unable to calculate, b inverse units not convertable to a");
                    }
                } else {
                    throw new IllegalArgumentException("unable to calculate, b inversion not known");
                }
            } else {
                return Units.dimensionless.createDatum( a / uc.convert(b) );
            }
        }
    }
}
