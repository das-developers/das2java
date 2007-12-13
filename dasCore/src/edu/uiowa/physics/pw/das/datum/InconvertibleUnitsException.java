/*
 * InconvertibleUnitsException.java
 *
 * Created on December 1, 2007, 6:31 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.uiowa.physics.pw.das.datum;

/**
 * introduced so that clients can more precisely catch this exception.
 * @author jbf
 */
public class InconvertibleUnitsException extends IllegalArgumentException {
    
    /** Creates a new instance of InconvertibleUnitsException */
    public InconvertibleUnitsException( Units fromUnits, Units toUnits ) {
        super( ( fromUnits==Units.dimensionless ? "(dimensionless)" : fromUnits.toString() ) 
            + " -> " 
            + ( toUnits==Units.dimensionless ? "(dimensionless)" : toUnits.toString() ) ) ;
    }
    
}
