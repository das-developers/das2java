/*
 * DatumVectorAdapter.java
 *
 * Created on January 29, 2007, 9:39 AM
 *
 */

package org.das2.qds;

import org.das2.datum.DatumVector;
import org.das2.datum.Units;

/**
 * utility routines for adapting legacy das2 DatumVector.
 * @author jbf
 */
public class DatumVectorAdapter {
   
    /**
     * extracts a das2 legacy DatumVector from a rank 1 QDataSet.
     */
    public static DatumVector toDatumVector( QDataSet ds ) {
        double[] dd= new double[ds.length()];
        for ( int i=0; i<ds.length(); i++ ) {
            dd[i]= ds.value(i);
        }
        Units u=  (Units) ds.property(QDataSet.UNITS) ;
        if ( u==null ) u= Units.dimensionless;
        return DatumVector.newDatumVector( dd, u );
    }
}
