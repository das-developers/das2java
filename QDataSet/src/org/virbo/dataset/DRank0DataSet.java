/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.dataset;

import org.das2.datum.Datum;
import org.das2.datum.Units;

/**
 * Implementation of Rank 0 dataset backed by a double.
 * @author jbf
 */
public class DRank0DataSet extends AbstractDataSet implements RankZeroDataSet {

    double d;
    Units u;

    private DRank0DataSet( double d, Units u ) {
        if ( u!=null && u!=Units.dimensionless ) putProperty( QDataSet.UNITS, u );
        this.d= d;
    }

    public static DRank0DataSet create( double d ) {
        return new DRank0DataSet( d, null );
    }

    public static DRank0DataSet create( double d, Units u ) {
        return new DRank0DataSet( d, u );
    }

    public static DRank0DataSet create( Datum d ) {
        Units u= d.getUnits();
        return new DRank0DataSet( d.doubleValue(u), u );
    }

    @Override
    public int rank() {
        return 0;
    }

    public double value() {
        return d;
    }

    public String toString() {
        return DataSetUtil.toString(this);
    }
}
