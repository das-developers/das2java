
package org.das2.datum;

/**
 * introduced so that clients can more precisely catch this exception.
 * @author jbf
 */
public class InconvertibleUnitsException extends IllegalArgumentException {
    
    /** 
     * Creates a new instance of InconvertibleUnitsException 
     * @param fromUnits the original units
     * @param toUnits the target units
     */
    public InconvertibleUnitsException( Units fromUnits, Units toUnits ) {
        super( ( fromUnits==Units.dimensionless ? "(dimensionless)" : fromUnits.toString() ) 
            + " -> " 
            + ( toUnits==Units.dimensionless ? "(dimensionless)" : toUnits.toString() ) ) ;
    }
    
}
