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
    String dsname;

    protected WeightsDataSet(QDataSet ds) {
        this.ds = ds;
        Number validMin = (Number) ds.property(QDataSet.VALID_MIN);
        if (validMin == null) validMin = Double.NEGATIVE_INFINITY;
        Number validMax = (Number) ds.property(QDataSet.VALID_MAX);
        if (validMax == null) validMax = Double.POSITIVE_INFINITY;
        Number ofill = (Number) ds.property(QDataSet.FILL_VALUE);
        fill = (ofill == null ? Double.NaN : ofill.doubleValue());
        //ffill= (double)(float)fill;  There was old code that checked for floats that had been converted to doubles.
        vmin = validMin.doubleValue();
        vmax = validMax.doubleValue();

        check = (vmin > -1 * Double.MAX_VALUE || vmax < Double.MAX_VALUE || !(Double.isNaN(fill)));
        String name= (String)ds.property(QDataSet.NAME);
        dsname= name==null ? "wds" : "wds_"+name;
    }

    public int rank() {
        return ds.rank();
    }

    @Override
    public String toString() {
        return DataSetUtil.toString(ds);
    }

    public abstract double value();

    public abstract double value(int i);

    public abstract double value(int i0, int i1);

    public abstract double value(int i0, int i1, int i2);

    public abstract double value(int i0, int i1, int i2, int i3);

    public Object property(String name) {
        //if ( name.equals(QDataSet.FILL_VALUE) ) return fill;
        if ( name.equals(QDataSet.NAME ) ) return dsname;
        return null;

    }

    public Object property(String name, int i) {
        return property(name);
    }

    public Object property(String name, int i0, int i1) {
        return property(name);
    }

    public Object property(String name, int i0, int i1, int i2) {
        return property(name);
    }

    public Object property(String name, int i0, int i1, int i2, int i3) {
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

    public int length(int i, int j, int k) {
        return ds.length(i, j, k);
    }

    public <T> T capability(Class<T> clazz) {
        return null;
    }

    public QDataSet slice(int i) {
        return new Slice0DataSet( this, i);
    }

    public QDataSet trim(int start, int end) {
        return new TrimDataSet( this, start, end );
    }


    /**
     *  return 1 for finite (Non-NaN) values that are not equal to fill, or outside (not including) vmin to vmax.
     */
    public static final class ValidRangeFillFinite extends WeightsDataSet {

        public ValidRangeFillFinite( QDataSet ds ) {
            super(ds);
        }
        
        private double weight(double v) {
            return v == fill || Double.isNaN(v) || v > vmax || v < vmin ? 0.0 : 1.0;
        }

        public double value() {
            return weight(ds.value());
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

        public double value(int i0, int i1, int i2, int i3) {
            return weight(ds.value(i0, i1, i2, i3));
        }

    }

    /**
     *  return 1 for finite (Non-NaN) values that are not equal to fill, or (float)fill.
     */
    public static final class FillFinite extends WeightsDataSet {
        public FillFinite( QDataSet ds ) {
            super(ds);
        }
        
        private double weight(double v) {
            return v == fill || Double.isNaN(v) ? 0.0 : 1.0;
        }

        public double value() {
            return weight(ds.value());
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

        public double value(int i0, int i1, int i2, int i3) {
            return weight(ds.value(i0, i1, i2, i3));
        }
    }

    /**
     * return 1 for finite (Non-NaN) values.
     */
    public static final class Finite extends WeightsDataSet {
        public Finite( QDataSet ds ) {
            super(ds);
        }
        
        private double weight(double v) {
            return Double.isNaN(v) ? 0.0 : 1.0;
        }

        public double value() {
            return weight(ds.value());
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

        public double value(int i0, int i1, int i2, int i3) {
            return weight(ds.value(i0, i1, i2, i3));
        }
    }    

    /**
     * return 1 for any value.
     */
    public static final class AllValid extends WeightsDataSet {
        public AllValid( QDataSet ds ) {
            super(ds);
        }
        
        public double value() {
            return 1.0;
        }

        public double value(int i) {
            return 1.0;
        }

        public double value(int i0, int i1) {
            return 1.0;
        }

        public double value(int i0, int i1, int i2) {
            return 1.0;
        }

        public double value(int i0, int i1, int i2, int i3) {
            return 1.0;
        }
    }

    /**
     * applies the rules from src to dataset v, returning an array of 0 or non-zero.
     * @param src
     * @param v
     * @return
     */
    public static QDataSet applyRules( QDataSet src, QDataSet v ) {
        WeightsDataSet wds= new ValidRangeFillFinite(src);
        wds.ds= v;
        return wds;
    }
}
