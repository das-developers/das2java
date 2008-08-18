/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dsops;

import org.virbo.dataset.QubeDataSetIterator;
import edu.uiowa.physics.pw.das.dataset.TableDataSet;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.EnumerationUnits;
import edu.uiowa.physics.pw.das.datum.TimeUtil;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.math.fft.ComplexArray;
import edu.uiowa.physics.pw.das.math.fft.FFTUtil;
import edu.uiowa.physics.pw.das.math.fft.GeneralFFT;
import edu.uiowa.physics.pw.das.math.fft.WaveformToSpectrum;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DataSetAdapter;
import org.virbo.dataset.MutablePropertyDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SDataSet;
import org.virbo.dataset.TransposeRank2DataSet;
import org.virbo.dataset.TrimStrideWrapper;
import org.virbo.dataset.VectorDataSetAdapter;
import org.virbo.dsutil.BinAverage;
import org.virbo.dsutil.DataSetBuilder;

/**
 * A fairly complete set of operations for QDataSets.  Currently, most operations
 * require that the dataset be a qube.  
 * Most operations check for fill data. 
 * Few operations check units. (TODO: check units)
 * 
 * @author jbf
 */
public class Ops {

    public interface UnaryOp {
        double op(double d1);
    }

    public static final QDataSet applyUnaryOp(QDataSet ds1, UnaryOp op) {
        DDataSet result = DDataSet.create(DataSetUtil.qubeDims(ds1));
        Units u= (Units) ds1.property(QDataSet.UNITS);
        if ( u==null ) u= Units.dimensionless;
        QubeDataSetIterator it1 = new QubeDataSetIterator(ds1);
        while (it1.hasNext()) {
            it1.next();
            double d1 = it1.getValue(ds1);
            it1.putValue(result, u.isFill(d1) ? u.getFillDouble() : op.op(d1) );
        }
        DataSetUtil.putProperties(DataSetUtil.getProperties(ds1), result);
        return result;
    }

    public interface BinaryOp {
        double op(double d1, double d2);
    }

    private static HashMap<String,Object> equalProperties(Map<String,Object> m1, Map<String,Object> m2) {
        HashMap result = new HashMap();
        for (Object o : m1.keySet()) {
            Object v = m1.get(o);
            if (v != null && v.equals(m2.get(o))) {
                result.put(o, v);
            }
        }
        return result;
    }

    /**
     * apply the binary operator element-for-element of the two datasets, minding
     * dataset geometry, fill values, etc.
     * @param ds1
     * @param ds2
     * @param op
     * @return
     */
    public static final QDataSet applyBinaryOp(QDataSet ds1, QDataSet ds2, BinaryOp op) {
        DDataSet result = DDataSet.create(DataSetUtil.qubeDims(ds1));

        QubeDataSetIterator it1 = new QubeDataSetIterator(ds1);
        QubeDataSetIterator it2 = new QubeDataSetIterator(ds2);
        Units u1= (Units)ds1.property(QDataSet.UNITS);
        Units u2= (Units)ds2.property(QDataSet.UNITS);
        if ( u1==null ) u1= Units.dimensionless;
        if ( u2==null ) u2= Units.dimensionless;
        while (it1.hasNext()) {
            it1.next();
            double d1 = it1.getValue(ds1);
            it2.next();
            double d2 = it2.getValue(ds2);
            it1.putValue(result, u1.isFill(d1)||u2.isFill(d2) ? u1.getFillDouble() : op.op(d1, d2) );
        }
        Map<String,Object> m1= DataSetUtil.getProperties(ds1);
        Map<String,Object> m2= DataSetUtil.getProperties(ds2);
        Map<String,Object> m3= equalProperties( m1, m2 );
        DataSetUtil.putProperties(m3, result);
        return result;
    }

    public static final QDataSet applyBinaryOp(QDataSet ds1, double d2, BinaryOp op) {
        DDataSet result = DDataSet.create(DataSetUtil.qubeDims(ds1));

        QubeDataSetIterator it1 = new QubeDataSetIterator(ds1);
        while (it1.hasNext()) {
            it1.next();
            it1.putValue(result, op.op(it1.getValue(ds1), d2));
        }
        DataSetUtil.putProperties(DataSetUtil.getProperties(ds1), result);
        return result;
    }

    /**
     * add the two datasets have the same geometry.
     * @param ds1
     * @param ds2
     * @return
     */
    public static QDataSet add(QDataSet ds1, QDataSet ds2) {
        return applyBinaryOp(ds1, ds2, new BinaryOp() {
            public double op(double d1, double d2) {
                return d1 + d2;
            }
        });
    }

    /**
     * subtract one dataset from another.
     * TODO: mind units
     * @param ds1
     * @param ds2
     * @return
     */
    public static QDataSet subtract(QDataSet ds1, QDataSet ds2) {
        MutablePropertyDataSet result= (MutablePropertyDataSet)applyBinaryOp(ds1, ds2, new BinaryOp() {
            public double op(double d1, double d2) {
                return d1 - d2;
            }
        });
        Units units1= (Units) ds1.property(QDataSet.UNITS);
        Units units2= (Units) ds2.property(QDataSet.UNITS);
        if ( units1==units2 ) {
            result.putProperty( QDataSet.UNITS, units1.getOffsetUnits() );
        }
        return result;
    }

    /**
     * return a dataset with each element negated.
     * @param ds1
     * @return
     */
    public static QDataSet negate(QDataSet ds1) {
        return applyUnaryOp(ds1, new UnaryOp() {
            public double op(double d1) {
                return -d1;
            }
        });
    }

    /**
     * return the magnitudes of vectors in a rank 2 or greater dataset.  The last
     * index must be a cartesian dimension, so it must have a depend dataset
     * either named "cartesian" or having the property CARTESIAN_FRAME
     * 
     * @param ds of Rank N.
     * @return ds of Rank N-1.
     */
    public static QDataSet magnitude(QDataSet ds) {
        int r = ds.rank();
        QDataSet depn = (QDataSet) ds.property("DEPEND_" + (r - 1));
        boolean isCart = false;
        if (depn != null) {
            if (depn.property(QDataSet.COORDINATE_FRAME) != null) {
                isCart = true;
            } else if ("cartesian".equals(depn.property(QDataSet.NAME))) {
                isCart = true;
            } else {
            }
        }
        if (isCart) {
            ds = pow(ds, 2);
            ds = total(ds, r - 1);
            ds = sqrt(ds);
            return ds;
        } else {
            throw new IllegalArgumentException("last dim must have COORDINATE_FRAME property");
        }

    }

    public static double total(QDataSet ds) {
        double s = 0;
        QubeDataSetIterator it1 = new QubeDataSetIterator(ds);
        while (it1.hasNext()) {
            it1.next();
            s += it1.getValue(ds);
        }
        return s;
    }

    private interface AverageOp {

        /**
         * average in measurement d1 with weight w1 into accum.
         * @param d1
         * @param w1
         * @param store 
         * @return
         */
        void accum(double d1, double w1, double[] accum);

        /**
         * store the initial values.
         * @param store
         */
        void initStore(double[] store);

        /**
         * normalize the accumulator.  accum[0] should contain the value, accum[1] should contain the weight.
         * @param accum
         */
        void normalize(double[] accum);
    }

    /**
     * reduce the dataset's rank by totalling all the elements along a dimension.
     * Only QUBEs are supported presently.
     * 
     * @param ds rank N qube dataset.
     * @param dim zero-based index number.
     * @param normalize return the average instead of the total.
     * @return
     */
    private static QDataSet averageGen(QDataSet ds, int dim, AverageOp op) {
        int[] qube = DataSetUtil.qubeDims(ds);
        int[] newQube = DataSetOps.removeElement(qube, dim);
        QDataSet wds = DataSetUtil.weightsDataSet(ds);
        DDataSet result = DDataSet.create(newQube);
        QubeDataSetIterator it1 = new QubeDataSetIterator(result);
        double fill = (Double) wds.property(QDataSet.FILL_VALUE);
        double[] store = new double[2];
        while (it1.hasNext()) {
            it1.next();
            op.initStore(store);

            QubeDataSetIterator it0 = new QubeDataSetIterator(ds);
            for (int i = 0; i < ds.rank(); i++) {
                int ndim = i < dim ? i : i - 1;
                if (i != dim) {
                    it0.setIndexIteratorFactory(i, new QubeDataSetIterator.SingletonIteratorFactory(it1.index(ndim)));
                }
            }
            while (it0.hasNext()) {
                it0.next();
                op.accum( it0.getValue(ds), it0.getValue(wds), store);
            }
            op.normalize(store);
            it1.putValue(result, store[1] > 0 ? store[0] : fill);
        }

        return result;
    }

    /**
     * reduce the dataset's rank by totalling all the elements along a dimension.
     * Only QUBEs are supported presently.
     * 
     * @param ds rank N qube dataset.
     * @param dim zero-based index number.
     * @param normalize return the average instead of the total.
     * @return
     */
    public static QDataSet total(QDataSet ds, int dim) {
        int[] qube = DataSetUtil.qubeDims(ds);
        int[] newQube = DataSetOps.removeElement(qube, dim);
        QDataSet wds = DataSetUtil.weightsDataSet(ds);
        DDataSet result = DDataSet.create(newQube);
        QubeDataSetIterator it1 = new QubeDataSetIterator(result);
        double fill = (Double) wds.property(QDataSet.FILL_VALUE);
        while (it1.hasNext()) {
            it1.next();
            int n = ds.length(dim);
            double s = 0;
            double w = 0;
            QubeDataSetIterator it0 = new QubeDataSetIterator(ds);
            for (int i = 0; i < ds.rank(); i++) {
                int ndim = i < dim ? i : i - 1;
                if (i != dim) {
                    it0.setIndexIteratorFactory(i, new QubeDataSetIterator.SingletonIteratorFactory(it1.index(ndim)));
                }
            }
            while (it0.hasNext()) {
                it0.next();
                double w1 = it0.getValue(wds);
                s += w1 * it0.getValue(ds);
                w += w1;
            }
            it1.putValue(result, w > 0 ? s : fill);
        }

        return result;
    }

    /**
     * reduce the dataset's rank by reporting the max of all the elements along a dimension.
     * Only QUBEs are supported presently.
     * 
     * @param ds rank N qube dataset.
     * @param dim zero-based index number.
     * @return
     */
    public static QDataSet reduceMax(QDataSet ds, int dim) {
        return averageGen(ds, dim, new AverageOp() {

            public void accum(double d1, double w1, double[] accum) {
                if (w1 > 0.0) {
                    accum[0] = Math.max(d1, accum[0]);
                    accum[1] = w1;
                }
            }

            public void initStore(double[] store) {
                store[0] = Double.NEGATIVE_INFINITY;
                store[1] = 0.;
            }

            public void normalize(double[] accum) {
                // nothing to do
            }
        });
    }

    /**
     * reduce the dataset's rank by reporting the min of all the elements along a dimension.
     * Only QUBEs are supported presently.
     * 
     * @param ds rank N qube dataset.
     * @param dim zero-based index number.
     * @return
     */
    public static QDataSet reduceMin(QDataSet ds, int dim) {
        return averageGen(ds, dim, new AverageOp() {

            public void accum(double d1, double w1, double[] accum) {
                if (w1 > 0.0) {
                    accum[0] = Math.min(d1, accum[0]);
                    accum[1] = w1;
                }
            }

            public void initStore(double[] store) {
                store[0] = Double.POSITIVE_INFINITY;
                store[1] = 0.;
            }

            public void normalize(double[] accum) {
                // nothing to do
            }
        });
    }

    /**
     * reduce the dataset's rank by reporting the max of all the elements along a dimension.
     * Only QUBEs are supported presently.
     * 
     * @param ds rank N qube dataset.
     * @param dim zero-based index number.
     * @return
     */
    public static QDataSet reduceMean(QDataSet ds, int dim) {
        return averageGen(ds, dim, new AverageOp() {

            public void accum(double d1, double w1, double[] accum) {
                accum[0] += w1 * d1;
                accum[1] += w1;
            }

            public void initStore(double[] store) {
                store[0] = 0.;
                store[1] = 0.;
            }

            public void normalize(double[] accum) {
                if (accum[1] > 0) {
                    accum[0] /= accum[1];
                }
            }
        });
    }

    //public static QDataSet smooth( QDataSet )
    /**
     * element-wise sqrt.
     * @param ds
     * @return
     */
    public static QDataSet sqrt(QDataSet ds) {
        return pow(ds, 0.5);
    }

    /**
     * element-wise abs.  For vectors, this returns the length of each element.
     * @param ds1
     * @return
     */
    public static QDataSet abs(QDataSet ds1) {
        return applyUnaryOp(ds1, new UnaryOp() {

            public double op(double d1) {
                return Math.abs(d1);
            }
        });
    }

    /**
     * element-wise pow.  (** in FORTRAN, ^ in IDL)
     * @param ds1
     * @param pow
     * @return
     */
    public static QDataSet pow(QDataSet ds1, double pow) {
        return applyBinaryOp(ds1, pow, new BinaryOp() {
            public double op(double d1, double d2) {
                return Math.pow(d1, d2);
            }
        });
    }

    /**
     * element-wise pow (** in FORTRAN, ^ in IDL) of two datasets with the same
     * geometry.
     * @param ds1
     * @param pow
     * @return
     */
    public static QDataSet pow(QDataSet ds1, QDataSet pow) {
        return applyBinaryOp(ds1, pow, new BinaryOp() {
            public double op(double d1, double d2) {
                return Math.pow(d1, d2);
            }
        });
    }

    /**
     * element-wise exponentiate e**x.
     * @param ds
     * @return
     */
    public static QDataSet exp(QDataSet ds) {
        return applyUnaryOp(ds, new UnaryOp() {

            public double op(double d1) {
                return Math.pow(Math.E, d1);
            }
        });
    }

    /**
     * element-wise natural logarythm.
     * @param ds
     * @return
     */
    public static QDataSet log(QDataSet ds) {
        return applyUnaryOp(ds, new UnaryOp() {

            public double op(double d1) {
                return Math.log(d1);
            }
        });
    }

    /**
     * element-wise base 10 logarythm.
     * @param ds
     * @return
     */
    public static QDataSet log10(QDataSet ds) {
        return applyUnaryOp(ds, new UnaryOp() {

            public double op(double d1) {
                return Math.log10(d1);
            }
        });
    }

    /**
     * element-wise multiply of two datasets with the same geometry.
     * @param ds
     * @return
     */
    public static QDataSet multiply(QDataSet ds1, QDataSet ds2) {
        return applyBinaryOp(ds1, ds2, new BinaryOp() {

            public double op(double d1, double d2) {
                return d1 * d2;
            }
        });
    }

    /**
     * element-wise divide of two datasets with the same geometry.
     * @param ds
     * @return
     */
    public static QDataSet divide(QDataSet ds1, QDataSet ds2) {
        return applyBinaryOp(ds1, ds2, new BinaryOp() {

            public double op(double d1, double d2) {
                return d1 / d2;
            }
        });
    }

// comparators
    /**
     * element-wise equality test.  1.0 is returned where the two datasets are
     * equal.  invalid measurements are always unequal.
     * @param ds
     * @return
     */
    public static QDataSet eq(QDataSet ds1, QDataSet ds2) {
        return applyBinaryOp(ds1, ds2, new BinaryOp() {

            public double op(double d1, double d2) {
                return d1 == d2 ? 1.0 : 0.0;
            }
        });
    }

    /**
     * element-wise not equal test.  invalid measurements are always unequal.
     * @param ds1
     * @param ds2
     * @return
     */
    public static QDataSet ne(QDataSet ds1, QDataSet ds2) {
        return applyBinaryOp(ds1, ds2, new BinaryOp() {

            public double op(double d1, double d2) {
                return d1 != d2 ? 1.0 : 0.0;
            }
        });
    }

    public static QDataSet gt(QDataSet ds1, QDataSet ds2) {
        return applyBinaryOp(ds1, ds2, new BinaryOp() {

            public double op(double d1, double d2) {
                return d1 > d2 ? 1.0 : 0.0;
            }
        });
    }

    public static QDataSet ge(QDataSet ds1, QDataSet ds2) {
        return applyBinaryOp(ds1, ds2, new BinaryOp() {

            public double op(double d1, double d2) {
                return d1 >= d2 ? 1.0 : 0.0;
            }
        });
    }

    public static QDataSet lt(QDataSet ds1, QDataSet ds2) {
        return applyBinaryOp(ds1, ds2, new BinaryOp() {

            public double op(double d1, double d2) {
                return d1 < d2 ? 1.0 : 0.0;
            }
        });
    }

    public static QDataSet le(QDataSet ds1, QDataSet ds2) {
        return applyBinaryOp(ds1, ds2, new BinaryOp() {

            public double op(double d1, double d2) {
                return d1 <= d2 ? 1.0 : 0.0;
            }
        });
    }

    // logic operators
    public static QDataSet or(QDataSet ds1, QDataSet ds2) {
        return applyBinaryOp(ds1, ds2, new BinaryOp() {

            public double op(double d1, double d2) {
                return d1 != 0 || d2 != 0 ? 1.0 : 0.0;
            }
        });
    }

    public static QDataSet and(QDataSet ds1, QDataSet ds2) {
        return applyBinaryOp(ds1, ds2, new BinaryOp() {

            public double op(double d1, double d2) {
                return d1 != 0 && d2 != 0 ? 1.0 : 0.0;
            }
        });
    }

    public static QDataSet not(QDataSet ds1) {
        return applyUnaryOp(ds1, new UnaryOp() {

            public double op(double d1) {
                return d1 != 0 ? 0.0 : 1.0;
            }
        });
    }

    // IDL,Matlab - inspired routines
    /**
     * returns rank 1 dataset with values [0,1,2,...]
     * @param size
     * @return
     */
    public static QDataSet dindgen(int len0) {
        int size = len0;
        double[] back = new double[size];
        for (int i = 0; i < size; i++) {
            back[i] = i;
        }
        return DDataSet.wrap(back, 1, len0, 1, 1);
    }

    /**
     * returns rank 2 dataset with values increasing [ [0,1,2], [ 3,4,5] ]
     * @param len0
     * @param len1
     * @return
     */
    public static QDataSet dindgen(int len0, int len1) {
        int size = len0 * len1;
        double[] back = new double[size];
        for (int i = 0; i < size; i++) {
            back[i] = i;
        }
        return DDataSet.wrap(back, 2, len0, len1, 1);
    }

    /**
     * returns rank 3 dataset with values increasing
     * @param len0
     * @param len1
     * @param len2
     * @return
     */
    public static QDataSet dindgen(int len0, int len1, int len2) {
        int size = len0 * len1 * len2;
        double[] back = new double[size];
        for (int i = 0; i < size; i++) {
            back[i] = i;
        }
        return DDataSet.wrap(back, 3, len0, len1, len2);
    }

      
    /**
     * returns rank 1 dataset with values [0,1,2,...]
     * @param baseTime e.g. "2003-02-04T00:00"
     * @param cadence e.g. "4.3 sec" "1 day"
     * @param len0 the number of elements.
     * @return
     */
    public static QDataSet timegen( String baseTime, String cadence, int len0) throws ParseException {
        int size = len0;
        double base= TimeUtil.create(baseTime).doubleValue( Units.us2000 );
        double dcadence= Units.us2000.getOffsetUnits().parse(cadence).doubleValue( Units.us2000.getOffsetUnits() );
        
        double[] back = new double[size];
        for (int i = 0; i < size; i++) {
            back[i] = base + i*dcadence;
        }
        DDataSet result= DDataSet.wrap(back, 1, len0, 1, 1);
        result.putProperty( QDataSet.UNITS, Units.us2000 );
        result.putProperty( QDataSet.MONOTONIC, Boolean.TRUE );
        return result;
    }

    /**
     * return a rank 1 dataset with <tt>len0</tt> linearly-spaced values, the first
     * is min and the last is max. 
     * @param min
     * @param max
     * @param len0
     * @return
     */
    public static QDataSet linspace(double min, double max, int len0) {
        double[] back = new double[len0];
        if (len0 < 1) {
            return DDataSet.wrap(new double[]{max});
        } else {
            double delta = (max - min) / (len0 - 1);
            for (int i = 0; i < len0; i++) {
                back[i] = min + i * delta;
            }
            return DDataSet.wrap(back, 1, len0, 1, 1);
        }
    }

    /**
     * returns rank 1 dataset with value
     * @param val fill the dataset with this value.
     * @param len0
     * @return
     */
    public static QDataSet replicate(double val, int len0) {
        int size = len0;
        double[] back = new double[size];
        for (int i = 0; i < size; i++) {
            back[i] = val;
        }
        return DDataSet.wrap(back, 1, len0, 1, 1);
    }

    /**
     * returns rank 2 dataset filled with value
     * @param val fill the dataset with this value.
     * @param len0
     * @param len1
     * @return
     */
    public static QDataSet replicate(double val, int len0, int len1) {
        int size = len0 * len1;
        double[] back = new double[size];
        for (int i = 0; i < size; i++) {
            back[i] = val;
        }
        return DDataSet.wrap(back, 2, len0, len1, 1);
    }

    /**
     * returns rank 3 dataset with filled with value.
     * @param val fill the dataset with this value.
     * @param len0
     * @param len1
     * @param len2
     * @return
     */
    public static QDataSet replicate(double val, int len0, int len1, int len2) {
        int size = len0 * len1 * len2;
        double[] back = new double[size];
        for (int i = 0; i < size; i++) {
            back[i] = val;
        }
        return DDataSet.wrap(back, 3, len0, len1, len2);
    }

    /**
     * return new dataset filled with zeros.
     * @param len0
     * @return
     */
    public static QDataSet zeros(int len0) {
        return replicate(0.0, len0);
    }

    /**
     * return new dataset filled with zeros.
     * @param len0
     * @return
     */
    public static QDataSet zeros(int len0, int len1) {
        return replicate(0.0, len0, len1);
    }

    /**
     * return new dataset filled with zeros.
     * @param len0
     * @return
     */
    public static QDataSet zeros(int len0, int len1, int len2) {
        return replicate(0.0, len0, len1, len2);
    }

    /**
     * return new dataset filled with ones.
     * @param len0
     * @return
     */
    public static QDataSet ones(int len0) {
        return replicate(1.0, len0);
    }

    /**
     * return new dataset filled with ones.
     * @param len0
     * @return
     */
    public static QDataSet ones(int len0, int len1) {
        return replicate(1.0, len0, len1);
    }

    /**
     * return new dataset filled with ones.
     * @param len0
     * @return
     */
    public static QDataSet ones(int len0, int len1, int len2) {
        return replicate(1.0, len0, len1, len2);
    }

    /**
     * joins the two datasets together, appending the on the zeroth dimension.
     * The two datasets must be QUBES have similar geometry on the higher dimensions.
     * @param ds1
     * @param ds2
     * @return 
     * @throws IllegalArgumentException if the two datasets don't have the same rank.
     */
    public static QDataSet join(QDataSet ds1, QDataSet ds2) {
        DDataSet result = DDataSet.copy(ds1);
        result.join(DDataSet.copy(ds2));
        return result;
    }

    /**
     * return returns a rank 1 dataset of uniform numbers from [0,1].
     * @param len0
     * @return
     */
    private static QDataSet rand(int[] qube, Random rand) {
        DDataSet result = DDataSet.create(qube);
        QubeDataSetIterator it = new QubeDataSetIterator(result);
        while (it.hasNext()) {
            it.next();
            it.putValue(result, rand.nextDouble());
        }
        return result;
    }

    /**
     * return returns a rank 1 dataset of random numbers of a guassian (normal) distribution.
     * @param len0
     * @return
     */
    private static QDataSet randn(int[] qube, Random rand) {
        DDataSet result = DDataSet.create(qube);
        QubeDataSetIterator it = new QubeDataSetIterator(result);
        while (it.hasNext()) {
            it.next();
            it.putValue(result, rand.nextGaussian());
        }
        return result;
    }

    /**
     * return returns a rank 1 dataset of random uniform numbers from [0,1].
     */
    public static QDataSet rand(int len0) {
        return rand(new int[]{len0}, new Random());
    }

    /**
     * return returns a rank 2 dataset of random uniform numbers from [0,1].
     */
    public static QDataSet rand(int len0, int len1) {
        return rand(new int[]{len0, len1}, new Random());
    }

    /**
     * return returns a rank 3 dataset of random uniform numbers from [0,1].
     */
    public static QDataSet rand(int len0, int len1, int len2) {
        return rand(new int[]{len0, len1, len2}, new Random());
    }

    /**
     * return returns a rank 1 dataset of random numbers of a guassian (normal) distribution.
     */
    public static QDataSet randn(int len0) {
        return randn(new int[]{len0}, new Random());
    }

    /**
     * return returns a rank 2 dataset of random numbers of a guassian (normal) distribution.
     */
    public static QDataSet randn(int len0, int len1) {
        return randn(new int[]{len0, len1}, new Random());
    }

    /**
     * return returns a rank 3 dataset of random numbers of a guassian (normal) distribution.
     */
    public static QDataSet randn(int len0, int len1, int len2) {
        return randn(new int[]{len0, len1, len2}, new Random());
    }

    /**
     * returns a rank 1 dataset of random numbers of a guassian (normal) distribution.
     * System.currentTimeMillis() may be used for the seed.
     * @param seed
     * @param len0
     * @return
     */
    public static QDataSet randomn(long seed, int len0) {
        int size = len0;
        double[] back = new double[size];
        Random r = new Random(seed);
        for (int i = 0; i < size; i++) {
            back[i] = r.nextGaussian();
        }
        return DDataSet.wrap(back, 1, len0, 1, 1);
    }

    /**
     * returns a rank 2 dataset of random numbers of a guassian (normal) distribution.
     * @param seed
     * @param len0
     * @param len1
     * @return
     */
    public static QDataSet randomn(long seed, int len0, int len1) {
        int size = len0 * len1;
        double[] back = new double[size];
        Random r = new Random(seed);
        for (int i = 0; i < size; i++) {
            back[i] = r.nextGaussian();
        }
        return DDataSet.wrap(back, 2, len0, len1, 1);
    }

    /**
     * returns a rank 3 dataset of random numbers of a guassian (normal) distribution.
     * @param seed
     * @param len0
     * @param len1
     * @param len2
     * @return
     */
    public static QDataSet randomn(long seed, int len0, int len1, int len2) {
        int size = len0 * len1 * len2;
        double[] back = new double[size];
        Random r = new Random(seed);
        for (int i = 0; i < size; i++) {
            back[i] = r.nextGaussian();
        }
        return DDataSet.wrap(back, 3, len0, len1, len2);
    }

    /**
     * element-wise sin.
     * @param ds
     * @return
     */
    public static QDataSet sin(QDataSet ds) {
        return applyUnaryOp(ds, new UnaryOp() {

            public double op(double d1) {
                return Math.sin(d1);
            }
        });
    }

    /**
     * element-wise sin.
     * @param ds
     * @return
     */
    public static QDataSet asin(QDataSet ds) {
        return applyUnaryOp(ds, new UnaryOp() {

            public double op(double d1) {
                return Math.asin(d1);
            }
        });
    }

    /**
     * element-wise cos.
     * @param ds
     * @return
     */
    public static QDataSet cos(QDataSet ds) {
        return applyUnaryOp(ds, new UnaryOp() {

            public double op(double d1) {
                return Math.cos(d1);
            }
        });
    }

    /**
     * element-wise acos.
     * @param ds
     * @return
     */
    public static QDataSet acos(QDataSet ds) {
        return applyUnaryOp(ds, new UnaryOp() {

            public double op(double d1) {
                return Math.acos(d1);
            }
        });
    }

    /**
     * element-wise tan.
     * @param ds
     * @return
     */
    public static QDataSet tan(QDataSet ds) {
        return applyUnaryOp(ds, new UnaryOp() {

            public double op(double a) {
                return Math.tan(a);
            }
        });
    }

    /**
     * element-wise atan.
     * @param ds
     * @return
     */
    public static QDataSet atan(QDataSet ds) {
        return applyUnaryOp(ds, new UnaryOp() {

            public double op(double a) {
                return Math.atan(a);
            }
        });
    }

    /**
     * element-wise atan2, 4-quadrant atan.
     * @param dsy
     * @param dsx
     * @return
     */
    public static QDataSet atan2(QDataSet dsy, QDataSet dsx) {
        return applyBinaryOp(dsy, dsx, new BinaryOp() {

            public double op(double y, double x) {
                return Math.atan2(y, x);
            }
        });
    }

    /**
     * element-wise cosh.
     * @param ds
     * @return
     */
    public static QDataSet cosh(QDataSet ds) {
        return applyUnaryOp(ds, new UnaryOp() {

            public double op(double a) {
                return Math.cosh(a);
            }
        });
    }

    /**
     * element-wise sinh.
     * @param ds
     * @return
     */
    public static QDataSet sinh(QDataSet ds) {
        return applyUnaryOp(ds, new UnaryOp() {

            public double op(double a) {
                return Math.sinh(a);
            }
        });
    }

    /**
     * element-wise tanh.
     * @param ds
     * @return
     */
    public static QDataSet tanh(QDataSet ds) {
        return applyUnaryOp(ds, new UnaryOp() {

            public double op(double a) {
                return Math.tanh(a);
            }
        });
    }

    /**
     * Returns <i>e</i><sup>x</sup>&nbsp;-1.  Note that for values of
     * <i>x</i> near 0, the exact sum of
     * <code>expm1(x)</code>&nbsp;+&nbsp;1 is much closer to the true
     * result of <i>e</i><sup>x</sup> than <code>exp(x)</code>.
     *
     * @param ds
     * @return
     */
    public static QDataSet expm1(QDataSet ds) {
        return applyUnaryOp(ds, new UnaryOp() {

            public double op(double a) {
                return Math.expm1(a);
            }
        });
    }

    public static QDataSet toRadians(QDataSet ds) {
        return applyUnaryOp(ds, new UnaryOp() {

            public double op(double y) {
                return y * Math.PI / 180.;
            }
        });
    }

    public static QDataSet toDegrees(QDataSet ds) {
        return applyUnaryOp(ds, new UnaryOp() {

            public double op(double y) {
                return y * 180 / Math.PI;
            }
        });
    }

    /**
     * returns a dataset containing the indeces of where the dataset is non-zero.
     * For a rank 1 dataset, returns a rank 1 dataset with indeces for the values.
     * For a higher rank dataset, returns a rank 2 qube dataset with ds.rank()
     * elements in the first dimension.
     * 
     * @param ds of any rank M
     * @return a rank 1 or rank 2 dataset with N by M elements, where N is the number
     * of non-zero elements found.
     */
    public static QDataSet where(QDataSet ds) {
        DataSetBuilder builder;

        QubeDataSetIterator iter = new QubeDataSetIterator(ds);

        if (ds.rank() == 1) {
            builder = new DataSetBuilder(1, 100, 1, 1);
            int count = 0;
            while (iter.hasNext()) {
                iter.next();
                if ( iter.getValue(ds) != 0.) {
                    builder.putValue( count, iter.index(0) );
                    builder.nextRecord();
                }
            }
            builder.putProperty(QDataSet.MONOTONIC, Boolean.TRUE);
        } else {
            builder = new DataSetBuilder(2, 100, ds.rank(), 1);
            int count = 0;
            while (iter.hasNext()) {
                iter.next();
                if ( iter.getValue(ds) != 0.) {
                    builder.putValue(count, 0, iter.index(0));
                    if (ds.rank() > 1) {
                        builder.putValue(count, 1, iter.index(1));
                    }
                    if (ds.rank() > 2) {
                        builder.putValue(count, 2, iter.index(2));
                    }
                    builder.nextRecord();
                }
            }
            if ( ds.rank()==2 ) {
                builder.putProperty( QDataSet.DEPEND_1, labels( new String[] { "dim0", "dim1" } ) );
            } else if ( ds.rank()==3 ) {
                builder.putProperty( QDataSet.DEPEND_1, labels( new String[] { "dim0", "dim1", "dim2" } ) );
            }
        }

        builder.putProperty(QDataSet.CADENCE, 1.0);

        return builder.getDataSet();
    }

    /**
     * returns a rank 1 dataset of indeces that sort the rank 1 dataset ds.
     * This is not the dataset sorted.  For example:
     * <pre>
     *   ds= randn(2000)
     *   s= sort( ds )
     *   dsSorted= ds[s]
     * </pre>
     * 
     * @param ds rank 1 dataset
     * @return rank 1 dataset of indeces that sort the input dataset.
     */
    public static QDataSet sort(QDataSet ds) {
        return DataSetOps.sort(ds);
    }

    /**
     * Performs an FFT on the provided rank 1 dataset.  A rank 2 dataset of 
     * complex numbers is returned.
     * @param ds a rank 1 dataset.
     * @return a rank 2 dataset of complex numbers.
     */
    public static QDataSet fft(QDataSet ds) {
        GeneralFFT fft = GeneralFFT.newDoubleFFT(ds.length());
        Units u = (Units) ds.property(QDataSet.UNITS);
        if (u == null) {
            u = Units.dimensionless;
        }
        ComplexArray.Double cc = FFTUtil.fft(fft, VectorDataSetAdapter.create(ds), u);
        DDataSet result = DDataSet.createRank2(ds.length(), 2);
        for (int i = 0; i < ds.length(); i++) {
            result.putValue(i, 0, cc.getReal(i));
            result.putValue(i, 1, cc.getImag(i));
        }

        QDataSet dep0 = (QDataSet) ds.property(QDataSet.DEPEND_0);
        double cadence = dep0 == null ? 1.0 : DataSetUtil.guessCadence(dep0);

        double[] tags = FFTUtil.getFrequencyDomainTags(cadence, ds.length());
        result.putProperty(QDataSet.DEPEND_0, DDataSet.wrap(tags));

        EnumerationUnits u1 = EnumerationUnits.create("complexCoordinates");
        DDataSet dep1 = DDataSet.createRank1(2);
        dep1.putValue(0, u1.createDatum("real").doubleValue(u1));
        dep1.putValue(1, u1.createDatum("imag").doubleValue(u1));
        dep1.putProperty(QDataSet.COORDINATE_FRAME, "ComplexNumber");
        dep1.putProperty(QDataSet.UNITS, u1);

        result.putProperty(QDataSet.DEPEND_1, dep1);
        return result;
    }

    /**
     * perform ffts on the rank 1 dataset to make a rank2 spectrogram.
     * @param ds rank 1 dataset
     * @param len the window length
     * @return rank 2 dataset.
     */
    public static QDataSet fftWindow(QDataSet ds, int len) {
        TableDataSet result = WaveformToSpectrum.getTableDataSet(VectorDataSetAdapter.create(ds), len);
        return DataSetAdapter.create(result);
    }

    /**
     * returns histogram of dataset, the number of points falling in each bin.
     * 
     * @param ds
     * @param min
     * @param max
     * @param binSize
     * @return
     */
    public static QDataSet histogram(QDataSet ds, double min, double max, double binSize) {
        return DataSetOps.histogram(ds, min, max, binSize);
    }

    /**
     * returns outerProduct of two rank 1 datasets, a rank 2 dataset with 
     * elements R[i,j]= ds1[i] * ds2[j].
     * 
     * @param ds1
     * @param ds2
     * @return
     */
    public static QDataSet outerProduct(QDataSet ds1, QDataSet ds2) {
        DDataSet result = DDataSet.createRank2(ds1.length(), ds2.length());
        for (int i = 0; i < ds1.length(); i++) {
            for (int j = 0; j < ds2.length(); j++) {
                result.putValue(i, j, ds1.value(i) * ds2.value(j));
            }
        }
        return result;
    }

    public static QDataSet floor(QDataSet ds1) {
        return applyUnaryOp(ds1, new UnaryOp() {

            public double op(double a) {
                return Math.floor(a);
            }
        });
    }

    public static QDataSet ceil(QDataSet ds1) {
        return applyUnaryOp(ds1, new UnaryOp() {

            public double op(double a) {
                return Math.ceil(a);
            }
        });
    }

    /**
     * Returns the signum function of the argument; zero if the argument is 
     * zero, 1.0 if the argument is greater than zero, -1.0 if the argument 
     * is less than zero.
     * @param ds1
     * @see copysign
     * @return 
     */
    public static QDataSet signum(QDataSet ds1) {
        return applyUnaryOp(ds1, new UnaryOp() {
            public double op(double a) {
                return Math.signum(a);
            }
        });
    }

    /**
     * Returns the first floating-point argument with the sign of the
     * second floating-point argument.
     * @param magnitude
     * @param sign
     * @see signum
     * @return
     */
    public static QDataSet copysign(QDataSet magnitude, QDataSet sign) {
        return applyBinaryOp(magnitude, sign, new BinaryOp() {

            public double op(double m, double s) {
                double s1 = Math.signum(s);
                return Math.abs(m) * (s1 == 0 ? 1. : s1);
            }
        });
    }

    /**
     * returns the floating point index of each element of vv within the monotonically
     * increasing dataset uu.  The result dataset will have the same geometry
     * as vv.  The result will be negative when the element of vv is below the smallest 
     * element of uu.  The result will be greater than or equal to the length of 
     * uu minus one when it is greater than all elements.
     * 
     * @param uu rank 1 monotonically increasing dataset.
     * @param vv rank N dataset with values in the same physical dimension as uu.
     * @return rank N dataset with the same geometry as vv.
     */
    public static QDataSet findex(QDataSet uu, QDataSet vv) {
        if (!DataSetUtil.isMonotonic(uu)) {
            throw new IllegalArgumentException("u must be monotonic");
        }
        DDataSet result = DDataSet.create(DataSetUtil.qubeDims(vv));
        QubeDataSetIterator it = new QubeDataSetIterator(vv);
        int ic0 = 0;
        int ic1 = 1;
        double uc0 = uu.value(ic0);
        double uc1 = uu.value(ic1);
        int n = uu.length();
        while (it.hasNext()) {
            it.next();
            double d = it.getValue(vv);
            // TODO: optimize by only doing binary search below or above ic0&ic1.
            if (uc0 <= d && d <= uc1) {
                double ff = (d - uc0) / (uc1 - uc0); // may be 1.0

                it.putValue(result, ic0 + ff);
            } else {
                int index = DataSetUtil.binarySearch(uu, d, 0, uu.length() - 1);
                if (index == -1) {
                    index = 0; //insertion point is 0

                    ic0 = 0;
                    ic1 = 1;
                } else if (index < (-n)) {
                    ic0 = n - 2;
                    ic1 = n - 1;
                } else if (index < 0) {
                    ic1 = ~index;  // usually this is the case

                    ic0 = ic1 - 1;
                } else if ( index>=(n-1) ) {
                    ic0 = n-2;
                    ic1 = n-1;
                } else {
                    ic0 = index;
                    ic1 = index + 1;
                }
                uc0 = uu.value(ic0);
                uc1 = uu.value(ic1);
                double ff = (d - uc0) / (uc1 - uc0); // may be 1.0

                it.putValue(result, ic0 + ff);
            }
        }
        return result;
    }

    /**
     * interpolate values from rank 1 dataset vv using fractional indeces 
     * in rank N findex.
     * 
     * @param vv rank 1 dataset.
     * @param findex rank N dataset of fractional indeces.
     * @return
     */
    public static QDataSet interpolate(QDataSet vv, QDataSet findex) {
        DDataSet result = DDataSet.create(DataSetUtil.qubeDims(findex));
        QubeDataSetIterator it = new QubeDataSetIterator(findex);
        int ic0, ic1;
        int n = vv.length();

        while (it.hasNext()) {
            it.next();

            double ff = it.getValue(findex);

            if (ff < 0) {
                ic0 = 0; // extrapolate

                ic1 = 1;
            } else if (ff >= n - 1) {
                ic0 = n - 2; // extrapolate

                ic1 = n - 1;
            } else {
                ic0 = (int) Math.floor(ff);
                ic1 = ic0 + 1;
            }

            double alpha = ff - ic0;

            double vv0 = vv.value(ic0);
            double vv1 = vv.value(ic1);

            it.putValue(result, vv0 + alpha * (vv1 - vv0));

        }
        return result;
    }

    /**
     * interpolate values from rank 1 dataset vv using fractional indeces 
     * in rank N findex, using bilinear interpolation.
     * 
     * @param vv rank 2 dataset.
     * @param findex0 rank N dataset of fractional indeces for the zeroth index.
     * @param findex1 rank N dataset of fractional indeces for the first index.
     * @return rank N dataset 
     */
    public static QDataSet interpolate(QDataSet vv, QDataSet findex0, QDataSet findex1) {

        DDataSet result = DDataSet.create(DataSetUtil.qubeDims(findex0));
        QubeDataSetIterator it = new QubeDataSetIterator(findex0);
        int ic00, ic01, ic10, ic11;
        int n0 = vv.length();
        int n1 = vv.length(0);

        while (it.hasNext()) {
            it.next();

            double ff0 = it.getValue(findex0);
            double ff1 = it.getValue(findex1);

            if (ff0 < 0) {
                ic00 = 0; // extrapolate

                ic01 = 1;
            } else if (ff0 >= n0 - 1) {
                ic00 = n0 - 2; // extrapolate

                ic01 = n0 - 1;
            } else {
                ic00 = (int) Math.floor(ff0);
                ic01 = ic00 + 1;
            }

            if (ff1 < 0) {
                ic10 = 0; // extrapolate

                ic11 = 1;
            } else if (ff1 >= n1 - 1) {
                ic10 = n1 - 2; // extrapolate

                ic11 = n1 - 1;
            } else {
                ic10 = (int) Math.floor(ff1);
                ic11 = ic10 + 1;
            }

            double alpha0 = ff0 - ic00;
            double alpha1 = ff1 - ic10;

            double vv00 = vv.value(ic00, ic10);
            double vv01 = vv.value(ic00, ic11);

            double vv10 = vv.value(ic01, ic10);
            double vv11 = vv.value(ic01, ic11);

            it.putValue(result, vv00 * (1 - alpha0) * (1 - alpha1) + vv01 * (1 - alpha0) * (alpha1) + vv10 * (alpha0) * (1 - alpha1) + vv11 * (alpha0) * (alpha1));

        }
        return result;
    }

    /**
     * run boxcar average over the dataset, returning a dataset of same geometry.  Points near the edge are simply copied from the
     * source dataset.  The result dataset contains a property "weights" that is the weights for each point.
     *
     * @param ds a rank 1 dataset of size N
     * @param size the number of adjacent bins to average
     * @return rank 1 dataset of size N
     */
    public static QDataSet smooth(QDataSet ds, int size) {
        if (ds.rank() > 1) {
            throw new IllegalArgumentException("only rank 1");
        }
        DDataSet result= BinAverage.boxcar(ds, size);
        DataSetUtil.putProperties( DataSetUtil.getProperties(ds), result );
        return result;
    }

    /**
     * return array that is the differences between each successive pair in the dataset.
     * Result[i]= ds[i+1]-ds[i], so that for an array with N elements, an array with
     * N-1 elements is returned.
     * @param ds a rank 1 dataset with N elements.
     * @return a rank 1 dataset with N-1 elements.
     */
    public static QDataSet diff(QDataSet ds) {
        if (ds.rank() > 1) {
            throw new IllegalArgumentException("only rank 1");
        }
        TrimStrideWrapper d1 = new TrimStrideWrapper(ds);
        d1.setTrim(0, 0, ds.length() - 1, 1);
        TrimStrideWrapper d2 = new TrimStrideWrapper(ds);
        d2.setTrim(0, 1, ds.length(), 1);
        QDataSet result= Ops.subtract(d2, d1);
        return result;
    }
    
    /**
     * create a labels dataset for tagging rows of a dataset.
     * Example:
     * <tt>dep1= labels( ["X","Y","Z"], "GSM" )</tt>
     * @param labels
     * @param context
     * @return rank 1 QDataSet
     */
    public static QDataSet labels( String[] labels, String context ) {
        EnumerationUnits u= new EnumerationUnits( context );
        SDataSet result= SDataSet.createRank1(labels.length);
        for ( int i=0; i<labels.length; i++ ) {
            Datum d= u.createDatum(labels[i]);
            result.putValue(i,d.doubleValue(u));
        }
        result.putProperty(QDataSet.UNITS, u);
        return result;
    }

    /**
     * create a labels dataset for tagging rows of a dataset.
     * Example:
     * <tt>dep1= labels( ["X","Y","Z"], "GSM" )</tt>
     * @param labels
     * @return rank 1 QDataSet
     */
    public static QDataSet labels( String[] labels ) {
        return labels( labels, "default" );
    }
        
    public static QDataSet reform( QDataSet ds, int[] qube ) {
        QubeDataSetIterator it0= new QubeDataSetIterator(ds);
        DDataSet result= DDataSet.create(qube);
        QubeDataSetIterator it1= new QubeDataSetIterator(result);
        while ( it0.hasNext() ) {
            it0.next();
            it1.next();
            double v= it0.getValue(ds);
            it1.putValue(result, v);
        }
        return result;
    }
    
    public static QDataSet transpose( QDataSet ds ) {
        return new TransposeRank2DataSet( ds );
    }
            
    public static double PI = Math.PI;
    public static double E = Math.E;
}
