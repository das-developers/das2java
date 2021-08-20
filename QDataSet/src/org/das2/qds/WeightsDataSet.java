
package org.das2.qds;

/**
 * Provide consistent valid logic to operators by providing a QDataSet
 * with 1.0 where the data is valid, and 0.0 where the data is invalid.
 * VALID_MIN, VALID_MAX and FILL_VALUE properties are used.
 * 
 * Note, when FILL_VALUE is not specified, -1e31 is used.  This is to
 * support legacy logic.
 * 
 * The property FILL_VALUE is no longer set to the fill value used.  https://sourceforge.net/p/autoplot/bugs/1458/
 * SUGGEST_FILL will be set in the properties.
 * 
 * @author jbf
 */
public abstract class WeightsDataSet implements QDataSet {

    final double vmin;
    final double vmax;
    double reportFill; // since numbers between zero and one cannot be used as fill this is reported.
    final double fill;    // the fill value
    final double fillMin; // support a little noise in fill, after seeing RPWS data which didn't load properly. (sftp://jfaden.net/home/jbf/ct/autoplot/data/d2s/20160906/testWBR2.d2s)
    final double fillMax;
    
    /**
     * the fill value from the original dataset.  
     * TODO: We really need a justification of this.  I suspect this was because
     * you wouldn't know what values would "fit" into the result, for example,
     * you don't want to suggest 1e38 if IDataSet is the implementation.  However,
     * I think the fill value used really depends on a number of things and
     * it's difficult to preserve the value safely.
     */
    public static final String PROP_SUGGEST_FILL = "SUGGEST_FILL";

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
        if ( fill<0 ) {
            fillMin= fill * 1.000001;
            fillMax= fill * 0.999991;
        } else if ( fill>=0 ) {
            fillMin= fill * 0.999991;
            fillMax= fill * 1.000001;
        } else {
            fillMin= Double.NaN;
            fillMax= Double.NaN;
        }
        
        reportFill= fill;
        if ( reportFill==0.0 || reportFill==1.0 ) { 
            reportFill= 127;
        }
        //ffill= (double)(float)fill;  There was old code that checked for floats that had been converted to doubles.
        vmin = validMin.doubleValue();
        vmax = validMax.doubleValue();

        check = (vmin > -1 * Double.MAX_VALUE || vmax < Double.MAX_VALUE || !(Double.isNaN(fill)));
        String name= (String)ds.property(QDataSet.NAME);
        dsname= name==null ? "wds" : "wds_"+name;
    }

    @Override
    public int rank() {
        return ds.rank();
    }

    @Override
    public String toString() {
        return DataSetUtil.toString(ds);
    }

    @Override
    public abstract double value();

    @Override
    public String svalue() {
        return String.valueOf(value());
    }
    
    @Override
    public abstract double value(int i);

    @Override
    public abstract double value(int i0, int i1);

    @Override
    public abstract double value(int i0, int i1, int i2);

    @Override
    public abstract double value(int i0, int i1, int i2, int i3);

    @Override
    public Object property(String name) {
        if ( name.equals(QDataSet.FILL_VALUE) ) return reportFill; // https://sourceforge.net/p/autoplot/bugs/1458/
        if ( name.equals(QDataSet.NAME ) ) return dsname;
        if ( name.equals(PROP_SUGGEST_FILL) ) return fill;
        return null;

    }

    @Override
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

    @Override
    public int length() {
        return ds.length();
    }

    @Override
    public int length(int i) {
        return ds.length(i);
    }

    @Override
    public int length(int i, int j) {
        return ds.length(i, j);
    }

    @Override
    public int length(int i, int j, int k) {
        return ds.length(i, j, k);
    }

    @Override
    public <T> T capability(Class<T> clazz) {
        return null;
    }

    @Override
    public QDataSet slice(int i) {
        return new Slice0DataSet( this, i);
    }

    @Override
    public QDataSet trim(int start, int end) {
        if ( start==0 && end==ds.length() ) {
            return this;
        }
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
            return ( v == fill || ( v>fillMin && v<fillMax ) || Double.isNaN(v) || v > vmax || v < vmin ) ? 0.0 : 1.0;
        }

        @Override
        public double value() {
            return weight(ds.value());
        }

        @Override
        public double value(int i) {
            return weight(ds.value(i));
        }

        @Override
        public double value(int i0, int i1) {
            return weight(ds.value(i0, i1));
        }

        @Override
        public double value(int i0, int i1, int i2) {
            return weight(ds.value(i0, i1, i2));
        }

        @Override
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
            return ( v == fill || ( v>fillMin && v<fillMax ) || Double.isNaN(v) ) ? 0.0 : 1.0;
        }

        @Override
        public double value() {
            return weight(ds.value());
        }

        @Override
        public double value(int i) {
            return weight(ds.value(i));
        }

        @Override
        public double value(int i0, int i1) {
            return weight(ds.value(i0, i1));
        }

        @Override
        public double value(int i0, int i1, int i2) {
            return weight(ds.value(i0, i1, i2));
        }

        @Override
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

        @Override
        public double value() {
            return weight(ds.value());
        }

        @Override
        public double value(int i) {
            return weight(ds.value(i));
        }

        @Override
        public double value(int i0, int i1) {
            return weight(ds.value(i0, i1));
        }

        @Override
        public double value(int i0, int i1, int i2) {
            return weight(ds.value(i0, i1, i2));
        }

        @Override
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
        
        @Override
        public double value() {
            return 1.0;
        }

        @Override
        public double value(int i) {
            return 1.0;
        }

        @Override
        public double value(int i0, int i1) {
            return 1.0;
        }

        @Override
        public double value(int i0, int i1, int i2) {
            return 1.0;
        }

        @Override
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
