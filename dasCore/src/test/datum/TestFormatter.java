/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test.datum;

import org.das2.datum.Datum;
import org.das2.datum.DatumUtil;
import org.das2.datum.Units;

/**
 *
 * @author jbf
 */
public class TestFormatter {
    public static void main( String[] args ) {
        Datum dmin= Units.cdfEpoch.createDatum( 6.31145908798525E13 );
        Datum dmax= Units.cdfEpoch.createDatum( 6.31146859218215E13 );
        System.err.println( DatumUtil.asOrderOneUnits( dmax.subtract(dmin) ) );
        System.err.println( dmin );
        System.err.println( dmax );

        dmin= Units.cdfEpoch.createDatum( 6.2996224190755695E13 );
        dmax= Units.cdfEpoch.createDatum( 6.2996291909045414E13 );
        System.err.println( DatumUtil.asOrderOneUnits( dmax.subtract(dmin) ) );
        System.err.println( dmin );
        System.err.println( dmax );
    }
}
