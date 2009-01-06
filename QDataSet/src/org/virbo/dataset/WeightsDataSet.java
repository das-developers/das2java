/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dataset;

import org.das2.datum.Units;

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
public abstract class WeightsDataSet implements QDataSet {

    final double fill;
    final double vmin;
    final double vmax;
    /**
     * if false, then check can be skipped.
     */
    final boolean check;
    QDataSet ds;

    protected WeightsDataSet(QDataSet ds) {
        this.ds = ds;
        Double validMin = (Double) ds.property(QDataSet.VALID_MIN);
        if (validMin == null) validMin = Double.NEGATIVE_INFINITY;
        Double validMax = (Double) ds.property(QDataSet.VALID_MAX);
        if (validMax == null) validMax = Double.POSITIVE_INFINITY;
        Units u = (Units) ds.property(QDataSet.UNITS);
        if (u == null) {
            u = Units.dimensionless;
        }
        Double ofill = (Double) ds.property(QDataSet.FILL_VALUE);
        fill = (ofill == null ? u.getFillDouble() : ofill.doubleValue());
        vmin = validMin;
        vmax = validMax;

        check = (vmin > -1 * Double.MAX_VALUE || vmax < Double.MAX_VALUE || !(Double.isNaN(fill)));
    }

    public int rank() {
        return ds.rank();
    }

    public abstract double value(int i);

    public abstract double value(int i0, int i1);

    public abstract double value(int i0, int i1, int i2);

    public Object property(String name) {
        return name.equals(QDataSet.FILL_VALUE) ? fill : null;
    }

    public Object property(String name, int i) {
        return property(name);
    }

    public Object property(String name, int i0, int i1) {
        return property(name);
    }

    public int length() {
        return ds.length();
    }

    public int length(int i) {
        return ds.length(i);
    }

    public int length(int i, int j) {
        return ds.length(i, j);
    }
    
    public static final class ValidRangeFillFinite extends WeightsDataSet {

        public ValidRangeFillFinite( QDataSet ds ) {
            super(ds);
        }
        
        private final double weight(double v) {
            return v == fill || Double.isNaN(v) || v > vmax || v < vmin ? 0.0 : 1.0;
        }

        public double value(int i) {
            return weight(ds.value(i));
        }

        public double value(int i0, int i1) {
            return weight(ds.value(i0, i1));
        }

        public double value(int i0, int i1, int i2) {
            return weight(ds.value(i0, i1, i2));
        }

    }
    
    public static final class FillFinite extends WeightsDataSet {
        public FillFinite( QDataSet ds ) {
            super(ds);
        }
        
        public final double weight(double v) {
            return v == fill || Double.isNaN(v) ? 0.0 : 1.0;
        }

        public double value(int i) {
            return weight(ds.value(i));
        }

        public double value(int i0, int i1) {
            return weight(ds.value(i0, i1));
        }

        public double value(int i0, int i1, int i2) {
            return weight(ds.value(i0, i1, i2));
        }        
    }
    
    public static final class Finite extends WeightsDataSet {
        public Finite( QDataSet ds ) {
            super(ds);
        }
        
        private final double weight(double v) {
            return Double.isNaN(v) ? 0.0 : 1.0;
        }

        public double value(int i) {
            return weight(ds.value(i));
        }

        public double value(int i0, int i1) {
            return weight(ds.value(i0, i1));
        }

        public double value(int i0, int i1, int i2) {
            return weight(ds.value(i0, i1, i2));
        }        
    }    
    
    public final class AllValid extends WeightsDataSet {
        public AllValid( QDataSet ds ) {
            super(ds);
        }
        
        private final double weight(double v) {
            return 1.0;
        }

        public double value(int i) {
            return weight(ds.value(i));
        }

        public double value(int i0, int i1) {
            return weight(ds.value(i0, i1));
        }

        public double value(int i0, int i1, int i2) {
            return weight(ds.value(i0, i1, i2));
        }               
    }
}
