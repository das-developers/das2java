/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qds;

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
        if ( u!=null && u.isFill(d) && !Double.isNaN(d) ) {
            putProperty( QDataSet.FILL_VALUE, d );
        }
        this.d= d;
        this.u= u;
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

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + (int) (Double.doubleToLongBits(this.d) ^ (Double.doubleToLongBits(this.d) >>> 32));
        hash = 97 * hash + (this.u != null ? this.u.hashCode() : 0);
        return hash;
    }

    /**
     * implement the equals method to at least return true for two DRank0DataSets.  This
     * was motivated by cadence parameter being dropped because they were not equal.
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if ( this==obj ) {
            return true;
        } else if ( obj instanceof DRank0DataSet ) {
            DRank0DataSet d0= (DRank0DataSet)obj;
            return ( this.d==d0.d && this.u==d0.u );
        } else {
            return super.equals(obj);
        }
    }


}
