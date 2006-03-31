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
    
}
