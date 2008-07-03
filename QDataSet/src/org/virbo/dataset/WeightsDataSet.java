/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dataset;

import edu.uiowa.physics.pw.das.datum.DatumRange;
import edu.uiowa.physics.pw.das.datum.Units;


/**
 * Provide consistent valid logic to operators by providing a QDataSet
 * with 1.0 where the data is valid, and 0.0 where the data is invalid.
 * VALID_MIN, VALID_MAX and FILL_VALUE properties are used.
 * 
 * Note, when FILL_VALUE is not specified, -1e31 is used.  This is to
 * support legacy logic.
 * 
 * For convenience, the property FILL_VALUE is set to the fill value used.
 * 
 * 
 * @author jbf
 */
public class WeightsDataSet implements QDataSet {

    final double fill;
    final double vmin;
    final double vmax;
    QDataSet ds;

    protected WeightsDataSet(QDataSet ds) {
        this.ds = ds;
        Double validMin = (Double) ds.property(QDataSet.VALID_MIN);
        if ( validMin==null ) validMin= Double.NEGATIVE_INFINITY;
        Double validMax = (Double) ds.property(QDataSet.VALID_MAX);
        if ( validMax==null ) validMax= Double.POSITIVE_INFINITY;
        Units u = (Units) ds.property(QDataSet.UNITS);
        if (u == null) {
            u = Units.dimensionless;
        }
        Double ofill = (Double) ds.property(QDataSet.FILL_VALUE);
        fill = ( ofill == null ? u.getFillDouble() : ofill.doubleValue() );
        vmin = validMin;
        vmax = validMax;
    }

    public int rank() {
        return ds.rank();
    }

    private final double weight( double v ) {
        return v==fill || Double.isNaN(v) || v>vmax || v<vmin ? 0.0 : 1.0;
    }
    
    public double value(int i) {
        return weight(ds.value(i));
    }

    public double value(int i0, int i1) {
        return weight(ds.value(i0,i1));
    }

    public double value(int i0, int i1, int i2) {
        return weight(ds.value(i0,i1,i2));
    }

    public Object property(String name) {
        return name.equals(QDataSet.FILL_VALUE) ? fill : null;
    }

    public Object property(String name, int i) {
        return name.equals(QDataSet.FILL_VALUE) ? fill : null;
    }

    public Object property(String name, int i0, int i1) {
        return name.equals(QDataSet.FILL_VALUE) ? fill : null;
    }

    public int length() {
        return ds.length();
    }

    public int length(int i) {
        return ds.length(i);
    }

    public int length(int i, int j) {
        return ds.length(i,j);
    }

}
