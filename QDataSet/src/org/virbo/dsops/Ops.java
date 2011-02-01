/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dsops;

import org.virbo.dataset.BundleDataSet.BundleDescriptor;
import org.virbo.dataset.QubeDataSetIterator;
import org.das2.datum.Datum;
import org.das2.datum.EnumerationUnits;
import org.das2.datum.TimeUtil;
import org.das2.datum.Units;
import org.virbo.math.fft.ComplexArray;
import org.virbo.math.fft.GeneralFFT;
import org.virbo.math.fft.WaveformToSpectrum;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;
import org.das2.datum.UnitsConverter;
import org.das2.datum.UnitsUtil;
import org.das2.util.monitor.NullProgressMonitor;
import org.das2.util.monitor.ProgressMonitor;
import org.virbo.dataset.ArrayDataSet;
import org.virbo.demos.RipplesDataSet;
import org.virbo.dataset.BundleDataSet;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DRank0DataSet;
import org.virbo.dataset.DataSetIterator;
import org.virbo.dataset.FDataSet;
import org.virbo.dataset.IDataSet;
import org.virbo.dataset.JoinDataSet;
import org.virbo.dataset.MutablePropertyDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.RankZeroDataSet;
import org.virbo.dataset.ReverseDataSet;
import org.virbo.dataset.SDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dataset.TransposeRank2DataSet;
import org.virbo.dataset.TrimStrideWrapper;
import org.virbo.dataset.WritableDataSet;
import org.virbo.dsutil.AutoHistogram;
import org.virbo.dsutil.BinAverage;
import org.virbo.dsutil.DataSetBuilder;
import org.virbo.dsutil.FFTUtil;

/**
 * A fairly complete set of operations for QDataSets.  Currently, most operations
 * require that the dataset be a qube.  
 * Most operations check for fill data. 
 * Few operations check units. (TODO: check units)
 * 
 * @author jbf
 */
public class Ops {

    /**
     * UnaryOps are one-argument operations, such as sin, abs, and sqrt
     */
    public interface UnaryOp {
        double op(double d1);
    }

    /**
     * apply the unary operation (such as "cos") to the dataset.
     * DEPEND_[0-3] is propagated.
     * @param ds1
     * @param op
     * @return
     */
    public static final MutablePropertyDataSet applyUnaryOp(QDataSet ds1, UnaryOp op) {
        DDataSet result = DDataSet.create(DataSetUtil.qubeDims(ds1));
        QDataSet wds= DataSetUtil.weightsDataSet(ds1);
        Units u = (Units) ds1.property(QDataSet.UNITS);
        if (u == null) {
            u = Units.dimensionless;
        }
        QubeDataSetIterator it1 = new QubeDataSetIterator(ds1);

        double fill= -1e38;
        
        while (it1.hasNext()) {
            it1.next();
            double d1 = it1.getValue(ds1);
            double w1 = it1.getValue(wds);
            it1.putValue(result, w1==0 ? fill : op.op(d1));
        }
        Map<String,Object> m= new HashMap<String,Object>();
        m.put( QDataSet.DEPEND_0, ds1.property(QDataSet.DEPEND_0) );
        m.put( QDataSet.DEPEND_1, ds1.property(QDataSet.DEPEND_1) );
        m.put( QDataSet.DEPEND_2, ds1.property(QDataSet.DEPEND_2) );
        m.put( QDataSet.DEPEND_3, ds1.property(QDataSet.DEPEND_3) );
        m.remove( QDataSet.VALID_MIN );
        m.remove( QDataSet.VALID_MAX );
        m.remove( QDataSet.TITLE );
        m.remove( QDataSet.LABEL );
        m.remove( QDataSet.MONOTONIC );
        m.remove( QDataSet.METADATA_MODEL );
        m.remove( QDataSet.METADATA );
        DataSetUtil.putProperties( m, result );
        result.putProperty( QDataSet.FILL_VALUE, fill );
        return result;
    }

    /**
     * BinaryOps are operations such as add, pow, atan2
     */
    public interface BinaryOp {
        double op(double d1, double d2);
    }

    /**
     * apply the binary operator element-for-element of the two datasets, minding
     * dataset geometry, fill values, etc.  The two datasets are coerced to
     * compatible geometry, if possible (e.g.Temperature(Time)+2), using CoerceUtil.coerce.
     * @param ds1
     * @param ds2
     * @param op
     * @return
     */
    public static final MutablePropertyDataSet applyBinaryOp( QDataSet ds1, QDataSet ds2, BinaryOp op ) {

        if ( ds1.rank()==ds2.rank() && ds1.rank()>0 ) {
            if ( ds1.length()!=ds2.length() ) {
                throw new IllegalArgumentException("binary option on datasets of different lengths: "+ ds1 + " " + ds2 );
            }
        }

        QDataSet[] operands= new QDataSet[2];

        WritableDataSet result = CoerceUtil.coerce( ds1, ds2, true, operands );

        QubeDataSetIterator it1 = new QubeDataSetIterator( operands[0] );
        QubeDataSetIterator it2 = new QubeDataSetIterator( operands[1] );

        QDataSet w1= DataSetUtil.weightsDataSet(operands[0]);
        QDataSet w2= DataSetUtil.weightsDataSet(operands[1]);

        double fill= -1e38;
        while (it1.hasNext()) {
            it1.next();
            it2.next();
            double d1 = it1.getValue(operands[0]);
            double d2 = it2.getValue(operands[1]);
            double w= it1.getValue(w1) * it2.getValue(w2);
            it1.putValue(result, w==0 ? fill : op.op(d1, d2));
        }

        Map<String, Object> m1 = DataSetUtil.getProperties(ds1);
        Map<String, Object> m2 = DataSetUtil.getProperties(ds2);
        if ( m2.isEmpty() && !m1.isEmpty() && ds2.rank()==0 ) {
            m2.put( QDataSet.DEPEND_0, m1.get(QDataSet.DEPEND_0 ) );
            m2.put( QDataSet.DEPEND_1, m1.get(QDataSet.DEPEND_1 ) );
            m2.put( QDataSet.DEPEND_2, m1.get(QDataSet.DEPEND_2 ) );
            m2.put( QDataSet.DEPEND_3, m1.get(QDataSet.DEPEND_3 ) );
        }
        if ( m1.isEmpty() && !m2.isEmpty() && ds1.rank()==0 ) {
            m1.put( QDataSet.DEPEND_0, m2.get(QDataSet.DEPEND_0 ) );
            m1.put( QDataSet.DEPEND_1, m2.get(QDataSet.DEPEND_1 ) );
            m1.put( QDataSet.DEPEND_2, m2.get(QDataSet.DEPEND_2 ) );
            m1.put( QDataSet.DEPEND_3, m2.get(QDataSet.DEPEND_3 ) );
        }
        Map<String, Object> m3 = equalProperties(m1, m2);
        m3.remove( QDataSet.VALID_MIN );
        m3.remove( QDataSet.VALID_MAX );
        m3.remove( QDataSet.TITLE );
        m3.remove( QDataSet.LABEL );
        m3.remove( QDataSet.MONOTONIC );
        m3.remove( QDataSet.METADATA_MODEL );
        m3.remove( QDataSet.METADATA );

        DataSetUtil.putProperties(m3, result);
        result.putProperty( QDataSet.FILL_VALUE, fill );
        
        return result;
    }

    public static final MutablePropertyDataSet applyBinaryOp(QDataSet ds1, double d2, BinaryOp op) {
        DDataSet result = DDataSet.create(DataSetUtil.qubeDims(ds1));

        QubeDataSetIterator it1 = new QubeDataSetIterator(ds1);
        QDataSet w1= DataSetUtil.weightsDataSet(ds1);

        double fill= -1e38;
        while (it1.hasNext()) {
            it1.next();
            double w= it1.getValue(w1);
            it1.putValue(result, w==0 ? fill : op.op(it1.getValue(ds1), d2));
        }
        Map<String,Object> props= DataSetUtil.getProperties(ds1);
        props.remove( QDataSet.VALID_MIN );
        props.remove( QDataSet.VALID_MAX );
        props.remove( QDataSet.TITLE );
        props.remove( QDataSet.LABEL );
        props.remove( QDataSet.MONOTONIC );
        props.remove( QDataSet.METADATA_MODEL );
        props.remove( QDataSet.METADATA );

        DataSetUtil.putProperties(props, result);
        result.putProperty( QDataSet.FILL_VALUE, fill );
        
        return result;
    }

    /**
     * returns the subset of two groups of properties that are equal, so these
     * may be preserved through operations.
     * @param m1 map of dataset properties, including DEPEND properties.
     * @param m2 map of dataset properties, including DEPEND properties.
     * @return
     */
    private static HashMap<String, Object> equalProperties(Map<String, Object> m1, Map<String, Object> m2) {
        HashMap result = new HashMap();
        for ( String k : m1.keySet()) {
            Object v = m1.get(k);
            if (v != null ) {
                Object v2= m2.get(k);
                if ( v.equals(v2) ) {
                    result.put(k, v);
                } else if ( ( v instanceof QDataSet ) && ( v2 instanceof QDataSet ) ) {
                    if ( equivalent( (QDataSet)v, (QDataSet)v2 ) ) {
                        result.put( k, v );
                    }
                }
            }
        }
        return result;
    }

    /**
     * add the two datasets have the same geometry.
     * @param ds1
     * @param ds2
     * @return
     */
    public static QDataSet add(QDataSet ds1, QDataSet ds2) {
        Units units1 = SemanticOps.getUnits( ds1 );
        Units units2 = SemanticOps.getUnits( ds2 );
        MutablePropertyDataSet result;
        if ( units2.isConvertableTo(units1) && UnitsUtil.isRatioMeasurement(units1) ) {
            final UnitsConverter uc= UnitsConverter.getConverter( units2, units1);
            result= (MutablePropertyDataSet)  applyBinaryOp(ds1, ds2, new BinaryOp() {
                public double op(double d1, double d2) {
                    return d1 + uc.convert(d2);
                }
            } );
            if ( units1!=Units.dimensionless ) result.putProperty( QDataSet.UNITS, units1 );
        } else if ( UnitsUtil.isIntervalMeasurement(units1) ) {
            final UnitsConverter uc= UnitsConverter.getConverter( units2, units1.getOffsetUnits() );
            result= (MutablePropertyDataSet) applyBinaryOp(ds1, ds2, new BinaryOp() {
                public double op(double d1, double d2) {
                    return d1 + uc.convert(d2);
                }
            } );
            result.putProperty( QDataSet.UNITS, units1 );
        } else if ( UnitsUtil.isIntervalMeasurement(units2) ) {
            final UnitsConverter uc= UnitsConverter.getConverter( units1, units2.getOffsetUnits() );
            result= (MutablePropertyDataSet) applyBinaryOp(ds1, ds2, new BinaryOp() {
                public double op(double d1, double d2) {
                    return uc.convert(d1) + d2;
                }
            } );
            result.putProperty( QDataSet.UNITS, units2 );
        } else {
            throw new IllegalArgumentException("units cannot be added: " + units1 + ", "+ units2 );

        }
        result.putProperty(QDataSet.NAME, null );
        result.putProperty(QDataSet.LABEL, maybeLabelInfixOp( ds1, ds2, "+" ) );
        return result;
    }

    /**
     * subtract one dataset from another.
     * @param ds1
     * @param ds2
     * @return
     */
    public static QDataSet subtract(QDataSet ds1, QDataSet ds2) {
        Units units1 = SemanticOps.getUnits( ds1 );
        Units units2 = SemanticOps.getUnits( ds2 );
        MutablePropertyDataSet result;

        if ( units2.isConvertableTo(units1) && UnitsUtil.isRatioMeasurement(units1) ) {
            final UnitsConverter uc= UnitsConverter.getConverter( units2, units1);
            result= (MutablePropertyDataSet)  applyBinaryOp(ds1, ds2, new BinaryOp() {
                public double op(double d1, double d2) {
                    return d1 - uc.convert(d2);
                }
            } );
            if ( units1!=Units.dimensionless ) result.putProperty( QDataSet.UNITS, units1 );
        } else if ( UnitsUtil.isIntervalMeasurement(units1) && UnitsUtil.isIntervalMeasurement(units2) ) {
            final UnitsConverter uc= UnitsConverter.getConverter( units2, units1 );
            result= (MutablePropertyDataSet) applyBinaryOp(ds1, ds2, new BinaryOp() {
                public double op(double d1, double d2) {
                    return d1 - uc.convert(d2);
                }
            } );
            result.putProperty( QDataSet.UNITS, units1.getOffsetUnits() );
        } else if ( UnitsUtil.isIntervalMeasurement(units1) && !UnitsUtil.isIntervalMeasurement(units2)) {
            final UnitsConverter uc= UnitsConverter.getConverter( units2, units1.getOffsetUnits() );
            result= (MutablePropertyDataSet) applyBinaryOp(ds1, ds2, new BinaryOp() {
                public double op(double d1, double d2) {
                    return d1 - uc.convert(d2);
                }
            } );
            result.putProperty( QDataSet.UNITS, units1 );
        } else if ( UnitsUtil.isIntervalMeasurement(units2) && !UnitsUtil.isIntervalMeasurement(units1)) {
            throw new IllegalArgumentException("cannot subtract interval measurement from ratio measurement: " + units1 + " - "+ units2 );
        } else {
            throw new IllegalArgumentException("cannot subtract: " + units1 + " - "+ units2 );
        }
        result.putProperty(QDataSet.NAME, null );
        result.putProperty(QDataSet.MONOTONIC, null );
        result.putProperty(QDataSet.LABEL, maybeLabelInfixOp( ds1, ds2, "-" ) );
        return result;
    }

    /**
     * maybe insert a label indicating the one-argument operation.  The operation
     * is formatted like opStr(ds1).  If a label cannot be created (for example,
     * no LABEL or NAME property), then null is returned.
     * @param ds1
     * @param opStr
     * @return the label or null.
     */
    private static String maybeLabelUnaryOp( QDataSet ds1, String opStr ) {
        String label1= (String) ds1.property(QDataSet.LABEL);
        if ( label1==null ) label1= (String)ds1.property(QDataSet.NAME);
        if ( label1==null ) return null;
        String l1Str= label1;
        if ( l1Str!=null ) {
            return opStr + "("+l1Str + ")";
        } else {
            return null;
        }
    }
    /**
     * maybe insert a label indicating the two-argument operation.  The operation
     * is formatted like opStr(ds1,ds2)
     * @param ds1
     * @param ds2
     * @param opStr
     */
    private static String maybeLabelBinaryOp( QDataSet ds1, QDataSet ds2, String opStr ) {
        String label1= (String) ds1.property(QDataSet.LABEL);
        if ( label1==null ) label1= (String)ds1.property(QDataSet.NAME);
        String label2= (String) ds2.property(QDataSet.LABEL);
        if ( label2==null ) label2= (String)ds2.property(QDataSet.NAME);
        if ( label1==null || label2==null ) return null;
        String l1Str= label1;
        String l2Str= label2;
        if ( l1Str!=null && l2Str!=null ) {
            return opStr + "(" + l1Str + "," + l2Str + ")";
        } else {
            return null;
        }
    }
    
    /**
     * maybe insert a label indicating the operation.
     * @param ds1
     * @param ds2
     * @param opStr
     */
    private static String maybeLabelInfixOp( QDataSet ds1, QDataSet ds2, String opStr ) {
        String label1= (String) ds1.property(QDataSet.LABEL);
        if ( label1==null ) label1= (String)ds1.property(QDataSet.NAME);
        String label2= (String) ds2.property(QDataSet.LABEL);
        if ( label2==null ) label2= (String)ds2.property(QDataSet.NAME);
        if ( label1==null || label2==null ) return null;
        if ( label1.equals(label2) ) { // this happens for example when LABEL=B_GSM and we do B_GSM[:,0] / B_GSM[:,1]  See autoplot-test025: test025_000
            return null;
        }
        Pattern idpat= Pattern.compile("[a-zA-Z_][a-zA-Z_0-9]*");
        String l1Str= label1;
        if ( l1Str!=null && ! idpat.matcher(l1Str).matches() ) l1Str= "("+l1Str+")";
        String l2Str= label2;
        if ( l2Str!=null && ! idpat.matcher(l2Str).matches() ) l2Str= "("+l2Str+")";
        if ( l1Str!=null && l2Str!=null ) {
            return l1Str + opStr + l2Str;
        } else {
            return null;
        }
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
                isCart = ds.length(0)<4;  // loosen up restrictions
            }
        }
        if (isCart) {
            Units u= (Units) ds.property(QDataSet.UNITS);
            ds = pow(ds, 2);
            ds = total(ds, r - 1);
            ds = sqrt(ds);
            if ( u!=null ) ((MutablePropertyDataSet)ds).putProperty(QDataSet.UNITS,u);
            return ds;
        } else {
            throw new IllegalArgumentException("last dim must have COORDINATE_FRAME property");
        }

    }

    /**
     * return the total of all the elements in the dataset, returning a rank
     * 0 dataset.  If there are invalid measurements, then fill is returned.
     * Does not support BINS or BUNDLE dimensions.
     *
     * @param ds
     * @return the unweighted total of the dataset, or -1e31 if fill was encounted.
     */
    public static double total(QDataSet ds) {
        double s = 0;
        QubeDataSetIterator it1 = new QubeDataSetIterator(ds);
        QDataSet wds= DataSetUtil.weightsDataSet(ds);
        double fill = ((Number) wds.property(QDataSet.FILL_VALUE)).doubleValue();
        while (it1.hasNext()) {
            it1.next();
            double w= it1.getValue(wds);
            if ( w==0 ) {
                return fill;
            }
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
        if ( qube==null ) throw new IllegalArgumentException("dataset is not a qube");
        if ( dim>=ds.rank() )
            throw new IllegalArgumentException( String.format( "dimension index (%d) exceeds rank (%d)",
                    dim, ds.rank() ) );
        int[] newQube = DataSetOps.removeElement(qube, dim);
        QDataSet wds = DataSetUtil.weightsDataSet(ds);
        DDataSet result = DDataSet.create(newQube);
        QubeDataSetIterator it1 = new QubeDataSetIterator(result);
        double fill = ((Number) wds.property(QDataSet.FILL_VALUE)).doubleValue();
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
                op.accum(it0.getValue(ds), it0.getValue(wds), store);
            }
            op.normalize(store);
            it1.putValue(result, store[1] > 0 ? store[0] : fill);
        }
        DataSetUtil.putProperties(DataSetUtil.getProperties(ds), result);
        sliceProperties(dim, ds, result);
        return result;
    }

    /**
     * reduce the dataset's rank by totalling all the elements along a dimension.
     * Only QUBEs are supported presently.
     * 
     * @param ds rank N qube dataset.  N=1,2,3,4
     * @param dim zero-based index number.
     * @return
     */
    public static QDataSet total(QDataSet ds, int dim) {
        int[] qube = DataSetUtil.qubeDims(ds);
        if ( qube==null ) throw new IllegalArgumentException("argument does not appear to be qube");
        int[] newQube = DataSetOps.removeElement(qube, dim);
        QDataSet wds = DataSetUtil.weightsDataSet(ds);
        DDataSet result= DDataSet.create(newQube);
        DDataSet weights= DDataSet.create(newQube);
        QubeDataSetIterator it1 = new QubeDataSetIterator(result);
        double fill = ((Number) wds.property(QDataSet.FILL_VALUE)).doubleValue();
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
            it1.putValue(weights, w );
        }
        sliceProperties( dim, ds, result );
        result.putProperty(QDataSet.WEIGHTS_PLANE,weights);
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

    /**
     * element-wise sqrt.
     * @param ds
     * @return
     */
    public static QDataSet sqrt(QDataSet ds) {
        MutablePropertyDataSet result= (MutablePropertyDataSet) pow(ds, 0.5);
        result.putProperty( QDataSet.LABEL, maybeLabelUnaryOp( ds, "sqrt" ) );
        return result;
    }

    /**
     * for jython, we handle this because the double isn't coerced.
     * @param x
     * @return
     */
    public static int abs( int x ) {
        return Math.abs( x );
    }

    /**
     * for jython, we handle this because the double isn't coerced.
     * @param x
     * @return
     */
    public static long abs( long x ) {
        return Math.abs( x );
    }

    /**
     * for jython, we handle this because the double isn't coerced.
     * @param x
     * @return
     */
    public static double abs( double x ) {
        return Math.abs( x );
    }

    /**
     * element-wise abs.  For vectors, this returns the length of each element.
     * Note jython conflict needs to be resolved.
     * @param ds1
     * @return
     */
    public static QDataSet abs(QDataSet ds1) {
        MutablePropertyDataSet result= applyUnaryOp(ds1, new UnaryOp() {

            public double op(double d1) {
                return Math.abs(d1);
            }
        });
        result.putProperty( QDataSet.LABEL, maybeLabelUnaryOp( ds1, "abs") );
        return result;
    }

    /**
     * for jython, we define this because the doubles aren't coerced.
     * @param x
     * @param y
     * @return
     */
    public static long pow( long x, long y ) {
        return (long)Math.pow( x, y );
    }


    /**
     * for jython, we define this because the doubles aren't coerced.
     * @param x
     * @param y
     * @return
     */
    public static double pow( double x, double y ) {
        return Math.pow( x, y );
    }

    /**
     * element-wise pow.  (** in FORTRAN, ^ in IDL)
     * Note jython conflict needs to be resolved.
     * @param ds1
     * @param pow
     * @return
     */
    public static QDataSet pow(QDataSet ds1, double pow) {
        MutablePropertyDataSet result= applyBinaryOp(ds1, pow, new BinaryOp() {

            public double op(double d1, double d2) {
                return Math.pow(d1, d2);
            }
        });
        result.putProperty( QDataSet.LABEL, maybeLabelBinaryOp( ds1, DataSetUtil.asDataSet(pow), "pow") );
        return result;

    }

    /**
     * element-wise pow (** in FORTRAN, ^ in IDL) of two datasets with the same
     * geometry.
     * @param ds1
     * @param pow
     * @return
     */
    public static QDataSet pow(QDataSet ds1, QDataSet pow) {
        MutablePropertyDataSet result=  applyBinaryOp(ds1, pow, new BinaryOp() {

            public double op(double d1, double d2) {
                return Math.pow(d1, d2);
            }
        });
        result.putProperty( QDataSet.LABEL, maybeLabelBinaryOp( ds1, pow, "pow") );
        return result;
    }

    /**
     * element-wise exponentiate e**x.
     * @param ds
     * @return
     */
    public static QDataSet exp(QDataSet ds) {
        MutablePropertyDataSet result=  applyUnaryOp(ds, new UnaryOp() {
            public double op(double d1) {
                return Math.pow(Math.E, d1);
            }
        });
        result.putProperty( QDataSet.LABEL, maybeLabelUnaryOp( ds, "exp") );
        return result;
    }

    /**
     * element-wise exponentiate 10**x.
     * @param ds
     * @return
     */
    public static QDataSet exp10(QDataSet ds) {
        MutablePropertyDataSet result=   applyUnaryOp(ds, new UnaryOp() {

            public double op(double d1) {
                return Math.pow(10, d1);
            }
        });
        result.putProperty( QDataSet.LABEL, maybeLabelUnaryOp( ds, "exp10") );
        return result;
    }

    /**
     * element-wise natural logarithm.
     * @param ds
     * @return
     */
    public static QDataSet log(QDataSet ds) {
        MutablePropertyDataSet result=  applyUnaryOp(ds, new UnaryOp() {

            public double op(double d1) {
                return Math.log(d1);
            }
        });
        result.putProperty( QDataSet.LABEL, maybeLabelUnaryOp( ds, "log") );
        return result;
    }

    /**
     * element-wise base 10 logarithm.
     * @param ds
     * @return
     */
    public static QDataSet log10(QDataSet ds) {
        MutablePropertyDataSet result=  applyUnaryOp(ds, new UnaryOp() {

            public double op(double d1) {
                return Math.log10(d1);
            }
        });
        result.putProperty( QDataSet.LABEL, maybeLabelUnaryOp( ds, "log10") );
        return result;
    }

    /**
     * element-wise multiply of two datasets with compatible geometry.
     * @param ds
     * @return
     */
    public static QDataSet multiply(QDataSet ds1, QDataSet ds2) {
        Units units1= SemanticOps.getUnits(ds1);
        Units units2= SemanticOps.getUnits(ds2);
        Units resultUnits;

        if ( units1==Units.dimensionless && units2==Units.dimensionless ) {
            resultUnits= Units.dimensionless;
        } else if ( units2==Units.dimensionless && UnitsUtil.isRatioMeasurement(units1) ) {
            resultUnits= units1;
        } else if ( units1==Units.dimensionless && UnitsUtil.isRatioMeasurement(units2) ) {
            resultUnits= units2;
        } else {
            if ( !UnitsUtil.isRatioMeasurement(units1) ) throw new IllegalArgumentException("ds1 units are not ratio units: "+units1);
            if ( !UnitsUtil.isRatioMeasurement(units2) ) throw new IllegalArgumentException("ds2 units are not ratio units: "+units2);
            System.err.println("throwing out units until we improve the units library, arguments have unequal units");
            resultUnits= null;
        }

        MutablePropertyDataSet result= applyBinaryOp(ds1, ds2, new BinaryOp() {
            public double op(double d1, double d2) {
                return d1 * d2;
            }
        });
        if ( resultUnits!=Units.dimensionless ) result.putProperty( QDataSet.UNITS, resultUnits );
        result.putProperty(QDataSet.LABEL, maybeLabelInfixOp( ds1, ds2, "*" ) );
        return result;
    }

    /**
     * element-wise divide of two datasets with compatible geometry.
     * @param ds
     * @return
     */
    public static QDataSet divide(QDataSet ds1, QDataSet ds2) {
        Units units1= SemanticOps.getUnits(ds1);
        Units units2= SemanticOps.getUnits(ds2);
        Units resultUnits;

        if ( units1==units2 ) {
            resultUnits= Units.dimensionless;
        } else if ( units2==Units.dimensionless && UnitsUtil.isRatioMeasurement(units1) ) {
            resultUnits= units1;
        } else {
            if ( !UnitsUtil.isRatioMeasurement(units1) ) throw new IllegalArgumentException("ds1 units are not ratio units: "+units1);
            if ( !UnitsUtil.isRatioMeasurement(units2) ) throw new IllegalArgumentException("ds2 units are not ratio units: "+units2);
            System.err.println("throwing out units until we improve the units library, arguments have unequal units");
            resultUnits= null;
        }

        MutablePropertyDataSet result= applyBinaryOp(ds1, ds2, new BinaryOp() {
            public double op(double d1, double d2) {
                return d1 / d2;
            }
        });

        if ( resultUnits!=Units.dimensionless ) result.putProperty( QDataSet.UNITS, resultUnits );
        result.putProperty(QDataSet.LABEL, maybeLabelInfixOp( ds1, ds2, "/" ) );
        return result;
    }

    /**
     * element-wise mod of two datasets with compatible geometry.
     * @param ds
     * @return
     */
    public static QDataSet mod(QDataSet ds1, QDataSet ds2) {
        return applyBinaryOp(ds1, ds2, new BinaryOp() {
            public double op(double d1, double d2) {
                return d1 % d2;
            }
        });
    }

    /**
     * element-wise div of two datasets with compatible geometry.
     * @param ds
     * @return
     */
    public static QDataSet div(QDataSet ds1, QDataSet ds2) {
        return applyBinaryOp(ds1, ds2, new BinaryOp() {
            public double op(double d1, double d2) {
                return (int) (d1 / d2);
            }
        });
    }
    // comparators
    /**
     * element-wise equality test.  1.0 is returned where the two datasets are
     * equal.  Fill is returned where either measurement is invalid.
     * @param ds
     * @return
     */
    public static QDataSet eq(QDataSet ds1, QDataSet ds2) {
        final UnitsConverter uc= SemanticOps.getUnitsConverter( ds1, ds2 );
        return applyBinaryOp(ds1, ds2, new BinaryOp() {
            public double op(double d1, double d2) {
                return uc.convert(d1) == d2 ? 1.0 : 0.0;
            }
        });
    }

    /**
     * element-wise not equal test.  1.0 is returned where elements are not equal.
     * Fill is returned where either measurement is invalid.
     * @param ds1
     * @param ds2
     * @return
     */
    public static QDataSet ne(QDataSet ds1, QDataSet ds2) {
        final UnitsConverter uc= SemanticOps.getUnitsConverter( ds1, ds2 );
        return applyBinaryOp(ds1, ds2, new BinaryOp() {
            public double op(double d1, double d2) {
                return uc.convert(d1) != d2 ? 1.0 : 0.0;
            }
        });
    }

    /**
     * element-wise function returns 1 where ds1&gt;ds2.
     * @param ds1
     * @param ds2
     * @return
     */
    public static QDataSet gt(QDataSet ds1, QDataSet ds2) {
        final UnitsConverter uc= SemanticOps.getUnitsConverter( ds1, ds2 );
        return applyBinaryOp(ds1, ds2, new BinaryOp() {
            public double op(double d1, double d2) {
                return uc.convert(d1) > d2 ? 1.0 : 0.0;
            }
        });
    }

    /**
     * element-wise function returns 1 where ds1&gt;=ds2.
     * @param ds1
     * @param ds2
     * @return
     */
    public static QDataSet ge(QDataSet ds1, QDataSet ds2) {
        final UnitsConverter uc= SemanticOps.getUnitsConverter( ds1, ds2 );
        return applyBinaryOp(ds1, ds2, new BinaryOp() {
            public double op(double d1, double d2) {
                return uc.convert(d1) >= d2 ? 1.0 : 0.0;
            }
        });
    }

    /**
     * element-wise function returns 1 where ds1&lt;ds2.
     * @param ds1
     * @param ds2
     * @return
     */
    public static QDataSet lt(QDataSet ds1, QDataSet ds2) {
        final UnitsConverter uc= SemanticOps.getUnitsConverter( ds1, ds2 );
        return applyBinaryOp(ds1, ds2, new BinaryOp() {
            public double op(double d1, double d2) {
                return uc.convert(d1) < d2 ? 1.0 : 0.0;
            }
        });
    }

    /**
     * element-wise function returns 1 where ds1&lt;=ds2.
     * @param ds1
     * @param ds2
     * @return
     */
    public static QDataSet le(QDataSet ds1, QDataSet ds2) {
        final UnitsConverter uc= SemanticOps.getUnitsConverter( ds1, ds2 );
        return applyBinaryOp(ds1, ds2, new BinaryOp() {
            public double op(double d1, double d2) {
                return uc.convert(d1) <= d2 ? 1.0 : 0.0;
            }
        });
    }

    // logic operators
    /**
     * element-wise logical or function.  
     * returns 1 where ds1 is non-zero or ds2 is non-zero.
     * @param ds1
     * @param ds2
     * @return
     */
    public static QDataSet or(QDataSet ds1, QDataSet ds2) {
        return applyBinaryOp(ds1, ds2, new BinaryOp() {
            public double op(double d1, double d2) {
                return d1 != 0 || d2 != 0 ? 1.0 : 0.0;
            }
        });
    }

    /**
     * element-wise logical and function.  non-zero is true, zero is false.
     * @param ds1
     * @param ds2
     * @return
     */
    public static QDataSet and(QDataSet ds1, QDataSet ds2) {
        return applyBinaryOp(ds1, ds2, new BinaryOp() {
            public double op(double d1, double d2) {
                return d1 != 0 && d2 != 0 ? 1.0 : 0.0;
            }
        });
    }

    /**
     * element-wise logical not function.  non-zero is true, zero is false.
     * @param ds1
     * @param ds2
     * @return
     */
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
     * @param size
     * @return
     */
    public static QDataSet findgen(int len0) {
        int size = len0;
        float[] back = new float[size];
        for (int i = 0; i < size; i++) {
            back[i] = i;
        }
        return FDataSet.wrap(back, 1, len0, 1, 1);
    }

    /**
     * returns rank 2 dataset with values increasing [ [0,1,2], [ 3,4,5] ]
     * @param len0
     * @param len1
     * @return
     */
    public static QDataSet findgen(int len0, int len1) {
        int size = len0 * len1;
        float[] back = new float[size];
        for (int i = 0; i < size; i++) {
            back[i] = i;
        }
        return FDataSet.wrap(back, 2, len0, len1, 1);
    }

    /**
     * returns rank 3 dataset with values increasing
     * @param len0
     * @param len1
     * @param len2
     * @return
     */
    public static QDataSet findgen(int len0, int len1, int len2) {
        int size = len0 * len1 * len2;
        float[] back = new float[size];
        for (int i = 0; i < size; i++) {
            back[i] = i;
        }
        return FDataSet.wrap(back, 3, len0, len1, len2);
    }

    /**
     * create a dataset filled with zeros.
     * @param len0
     * @return
     */
    public static QDataSet fltarr(int len0) {
        return Ops.replicate(0.f, len0);
    }

    public static QDataSet fltarr(int len0, int len1) {
        return Ops.replicate(0.f, len0, len1);
    }

    public static QDataSet fltarr(int len0, int len1, int len2) {
        return Ops.replicate(0.f, len0, len1, len2);
    }

    /**
     * create a dataset filled with zeros.
     * @param len0
     * @return
     */
    public static QDataSet dblarr(int len0) {
        return Ops.replicate(0., len0);
    }

    public static QDataSet dblarr(int len0, int len1) {
        return Ops.replicate(0., len0, len1);
    }

    public static QDataSet dblarr(int len0, int len1, int len2) {
        return Ops.replicate(0., len0, len1, len2);
    }

    /**
     * returns rank 1 dataset with values [0,1,2,...]
     * @param baseTime e.g. "2003-02-04T00:00"
     * @param cadence e.g. "4.3 sec" "1 day"
     * @param len0 the number of elements.
     * @return
     */
    public static QDataSet timegen(String baseTime, String cadence, int len0) throws ParseException {
        double base = TimeUtil.create(baseTime).doubleValue(Units.us2000);
        double dcadence = Units.us2000.getOffsetUnits().parse(cadence).doubleValue(Units.us2000.getOffsetUnits());

        return taggen( base, dcadence, len0, Units.us2000 );
    }

    /**
     * creates tags.  First tag will be start and they will increase by cadence.  Units specifies
     * the units of each tag.
     * @param start
     * @param cadence
     * @param len0
     * @param units
     * @return
     */
    public static MutablePropertyDataSet taggen( double base, double dcadence, int len0, Units units ) {
        double[] back = new double[len0];
        for (int i = 0; i < len0; i++) {
            back[i] = base + i * dcadence;
        }
        DDataSet result = DDataSet.wrap(back, 1, len0, 1, 1);
        result.putProperty(QDataSet.UNITS, units );
        result.putProperty(QDataSet.MONOTONIC, Boolean.TRUE);
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
    public static WritableDataSet replicate(double val, int len0) {
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
    public static WritableDataSet replicate(double val, int len0, int len1) {
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
    public static WritableDataSet replicate(double val, int len0, int len1, int len2) {
        int size = len0 * len1 * len2;
        double[] back = new double[size];
        for (int i = 0; i < size; i++) {
            back[i] = val;
        }
        return DDataSet.wrap(back, 3, len0, len1, len2);
    }

    /**
     * returns rank 1 dataset with value
     * @param val fill the dataset with this value.
     * @param len0
     * @return
     */
    public static WritableDataSet replicate(float val, int len0) {
        int size = len0;
        float[] back = new float[size];
        for (int i = 0; i < size; i++) {
            back[i] = val;
        }
        return FDataSet.wrap(back, 1, len0, 1, 1);
    }

    /**
     * returns rank 2 dataset filled with value
     * @param val fill the dataset with this value.
     * @param len0
     * @param len1
     * @return
     */
    public static WritableDataSet replicate(float val, int len0, int len1) {
        int size = len0 * len1;
        float[] back = new float[size];
        for (int i = 0; i < size; i++) {
            back[i] = val;
        }
        return FDataSet.wrap(back, 2, len0, len1, 1);
    }

    /**
     * returns rank 3 dataset with filled with value.
     * @param val fill the dataset with this value.
     * @param len0
     * @param len1
     * @param len2
     * @return
     */
    public static WritableDataSet replicate(float val, int len0, int len1, int len2) {
        int size = len0 * len1 * len2;
        float[] back = new float[size];
        for (int i = 0; i < size; i++) {
            back[i] = val;
        }
        return FDataSet.wrap(back, 3, len0, len1, len2);
    }

    /**
     * return new dataset filled with zeros.
     * @param len0
     * @return
     */
    public static WritableDataSet zeros(int len0) {
        return replicate(0.0, len0);
    }

    /**
     * return new dataset filled with zeros.
     * @param len0
     * @return
     */
    public static WritableDataSet zeros(int len0, int len1) {
        return replicate(0.0, len0, len1);
    }

    /**
     * return new dataset filled with zeros.
     * @param len0
     * @return
     */
    public static WritableDataSet zeros(int len0, int len1, int len2) {
        return replicate(0.0, len0, len1, len2);
    }

    /**
     * return a new dataset filled with zeroes that has the same geometry as
     * the given dataset.
     * Only supports QUBE datasets.
     * @param ds
     * @return a new dataset with filled with zeroes with the same geometry.
     */
    public static WritableDataSet zeros( QDataSet ds ) {
        return DDataSet.create( DataSetUtil.qubeDims(ds) );
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
     * concatenates the two datasets together, appending the on the zeroth dimension.
     * The two datasets must be QUBES have similar geometry on the higher dimensions.
     * If one of the datasets is rank 0 and the geometry of the other is rank 1, then
     * the lower rank dataset is promoted before appending.  If the first dataset
     * is null and the second is non-null, then return the second dataset.
     *
     * This was briefly known as "join."
     * @param ds1
     * @param ds2
     * @return 
     * @throws IllegalArgumentException if the two datasets don't have the same rank.
     */
    public static QDataSet concatenate(QDataSet ds1, QDataSet ds2) {
        if ( ds1==null && ds2!=null ) return ds2;
        if ( ds1 instanceof FDataSet && ds2 instanceof FDataSet ) {
            FDataSet result = (FDataSet) ArrayDataSet.copy(ds1);
            if ( ds2.rank()==0 && ds1.rank()==1 ) {
                FDataSet t= FDataSet.createRank1(1);
                t.putValue(ds2.value());
                DataSetUtil.putProperties( DataSetUtil.getProperties(ds2), t );
                ds2= t;
            } else if ( ds1.rank()==0 && ds2.rank()==1 ) {
                FDataSet t= FDataSet.createRank1(1);
                t.putValue(ds1.value());
                DataSetUtil.putProperties( DataSetUtil.getProperties(ds1), t );
                result= t;
            }
            return ArrayDataSet.append(result,FDataSet.maybeCopy(ds2));
        } else {
            DDataSet result = (DDataSet)DDataSet.copy(ds1);
            if ( ds2.rank()==0 && ds1.rank()==1 ) {
                DDataSet t= DDataSet.createRank1(1);
                t.putValue(ds2.value());
                DataSetUtil.putProperties( DataSetUtil.getProperties(ds2), t );
                ds2= t;
            } else if ( ds1.rank()==0 && ds2.rank()==1 ) {
                DDataSet t= DDataSet.createRank1(1);
                t.putValue(ds1.value());
                DataSetUtil.putProperties( DataSetUtil.getProperties(ds1), t );
                result= t;
            }
            return ArrayDataSet.append(result,DDataSet.maybeCopy(ds2));
        }
        
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
        double[] back = randomnBack(seed, len0);
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
        double[] back = randomnBack(seed, len0 * len1 );
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
        double[] back = randomnBack(seed, len0 * len1 * len2 );
        return DDataSet.wrap(back, 3, len0, len1, len2);
    }

    private static double[] randomnBack( long seed, int size ) {
        double[] back = new double[size];
        Random r = new Random(seed);
        for (int i = 0; i < size; i++) {
            back[i] = r.nextGaussian();
        }
        return back;
    }

    private static double[] randomuBack( long seed, int size ) {
        double[] back = new double[size];
        Random r = new Random(seed);
        for (int i = 0; i < size; i++) {
            back[i] = r.nextDouble();
        }
        return back;
    }

    /**
     * returns a rank 1 dataset of random numbers of a uniform distribution.
     * System.currentTimeMillis() may be used for the seed.
     * @param seed
     * @param len0
     * @return
     */
    public static QDataSet randomu(long seed, int len0) {
        double[] back = randomuBack(seed, len0);
        return DDataSet.wrap(back, 1, len0, 1, 1);
    }

    /**
     * returns a rank 2 dataset of random numbers of a uniform distribution.
     * @param seed
     * @param len0
     * @param len1
     * @return
     */
    public static QDataSet randomu(long seed, int len0, int len1) {
        double[] back = randomuBack(seed, len0 * len1 );
        return DDataSet.wrap(back, 2, len0, len1, 1);
    }

    /**
     * returns a rank 3 dataset of random numbers of a uniform distribution.
     * @param seed
     * @param len0
     * @param len1
     * @param len2
     * @return
     */
    public static QDataSet randomu(long seed, int len0, int len1, int len2) {
        double[] back = randomuBack(seed, len0 * len1 * len2 );
        return DDataSet.wrap(back, 3, len0, len1, len2);
    }


    /**
     * rank 1 dataset for demos.
     * @param len0
     * @param len1
     * @return
     */
    public static QDataSet ripples( int len0 ) {
        //TableDataSet tds= new RipplesDataSet( len0/10., len1/10., len1/20., len0/2., len1/2., len1/10., len0, len1 );
        //return DataSetAdapter.create(tds);
        return new RipplesDataSet( len0 );
    }

    /**
     * rank 2 dataset for demos.
     * @param len0
     * @param len1
     * @return
     */
    public static QDataSet ripples( int len0, int len1 ) {
        //TableDataSet tds= new RipplesDataSet( len0/10., len1/10., len1/20., len0/2., len1/2., len1/10., len0, len1 );
        //return DataSetAdapter.create(tds);
        return new RipplesDataSet( len0, len1 );
    }

    /**
     * rank 3 dataset for demos.
     * @return
     */
    public static QDataSet ripples( int len, int len0, int len1 ) {
        FDataSet result= org.virbo.dataset.FDataSet.createRank3( len, len0, len1 );
        for ( int i=0; i<len; i++ ) {
            double eps= 1+(i/(float)len);
            double eps2= 1+(i*5/(float)len);
            QDataSet d2= new RipplesDataSet( (len0*eps)/10., len1/10., (len1*eps2)/20., (len0*eps)/2., len1/2., (len1*eps)/10., len0, len1 );
            QubeDataSetIterator it= new QubeDataSetIterator(d2);
            while ( it.hasNext() ) {
                it.next();
                result.putValue(i,it.index(0),it.index(1), it.getValue(d2) );
            }
            if ( i==0 ) result.putProperty(QDataSet.FILL_VALUE,d2.property(QDataSet.FILL_VALUE));
        }
        
        return result;
    }

    /**
     * element-wise sin.
     * @param ds
     * @return
     */
    public static QDataSet sin(QDataSet ds) {
        MutablePropertyDataSet result= applyUnaryOp(ds, new UnaryOp() {

            public double op(double d1) {
                return Math.sin(d1);
            }
        });
        result.putProperty(QDataSet.LABEL, maybeLabelUnaryOp(result, "sin" ) );
        return result;
    }

    /**
     * element-wise arcsin.
     * @param ds
     * @return
     */
    public static QDataSet asin(QDataSet ds) {
        MutablePropertyDataSet result= applyUnaryOp(ds, new UnaryOp() {

            public double op(double d1) {
                return Math.asin(d1);
            }
        });
        result.putProperty(QDataSet.LABEL, maybeLabelUnaryOp(result, "asin" ) );
        return result;
    }

    /**
     * element-wise cos.
     * @param ds
     * @return
     */
    public static QDataSet cos(QDataSet ds) {
        MutablePropertyDataSet result= applyUnaryOp(ds, new UnaryOp() {

            public double op(double d1) {
                return Math.cos(d1);
            }
        });
        result.putProperty(QDataSet.LABEL, maybeLabelUnaryOp(result, "asin" ) );
        return result;
    }

    /**
     * element-wise arccos.
     * @param ds
     * @return
     */
    public static QDataSet acos(QDataSet ds) {
        MutablePropertyDataSet result= applyUnaryOp(ds, new UnaryOp() {

            public double op(double d1) {
                return Math.acos(d1);
            }
        });
        result.putProperty(QDataSet.LABEL, maybeLabelUnaryOp(result, "acos" ) );
        return result;

    }

    /**
     * element-wise tan.
     * @param ds
     * @return
     */
    public static QDataSet tan(QDataSet ds) {
        MutablePropertyDataSet result= applyUnaryOp(ds, new UnaryOp() {

            public double op(double a) {
                return Math.tan(a);
            }
        });
        result.putProperty(QDataSet.LABEL, maybeLabelUnaryOp(result, "tan" ) );
        return result;
    }

    /**
     * element-wise atan.
     * @param ds
     * @return
     */
    public static QDataSet atan(QDataSet ds) {
        MutablePropertyDataSet result= applyUnaryOp(ds, new UnaryOp() {

            public double op(double a) {
                return Math.atan(a);
            }
        });
        result.putProperty(QDataSet.LABEL, maybeLabelUnaryOp(result, "atan" ) );
        return result;
    }

    /**
     * element-wise atan2, 4-quadrant atan.
     * @param dsy
     * @param dsx
     * @return
     */
    public static QDataSet atan2(QDataSet dsy, QDataSet dsx) {
         MutablePropertyDataSet result= applyBinaryOp(dsy, dsx, new BinaryOp() {

            public double op(double y, double x) {
                return Math.atan2(y, x);
            }
        });
        result.putProperty(QDataSet.LABEL, maybeLabelBinaryOp(dsy,dsx, "cosh" ) );
        return result;
    }

    /**
     * element-wise cosh.
     * @param ds
     * @return
     */
    public static QDataSet cosh(QDataSet ds) {
        MutablePropertyDataSet result= applyUnaryOp(ds, new UnaryOp() {

            public double op(double a) {
                return Math.cosh(a);
            }
        });
        result.putProperty(QDataSet.LABEL, maybeLabelUnaryOp(result, "cosh" ) );
        return result;
    }

    /**
     * element-wise sinh.
     * @param ds
     * @return
     */
    public static QDataSet sinh(QDataSet ds) {
        MutablePropertyDataSet result= applyUnaryOp(ds, new UnaryOp() {

            public double op(double a) {
                return Math.sinh(a);
            }
        });
        result.putProperty(QDataSet.LABEL, maybeLabelUnaryOp(result, "sinh" ) );
        return result;
    }

    /**
     * element-wise tanh.
     * @param ds
     * @return
     */
    public static QDataSet tanh(QDataSet ds) {
        MutablePropertyDataSet result= applyUnaryOp(ds, new UnaryOp() {

            public double op(double a) {
                return Math.tanh(a);
            }
        });
        result.putProperty(QDataSet.LABEL, maybeLabelUnaryOp(result, "tanh" ) );
        return result;
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
        MutablePropertyDataSet result= applyUnaryOp(ds, new UnaryOp() {

            public double op(double a) {
                return Math.expm1(a);
            }
        });
        result.putProperty(QDataSet.LABEL, maybeLabelUnaryOp(result, "expm1" ) );
        return result;
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
     * elements in the first dimension.  Note when the dataset is all zeros (false),
     * the result is a zero-length array, as opposed to IDL which would return
     * a -1 scalar.
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
            while (iter.hasNext()) {
                iter.next();
                if (iter.getValue(ds) != 0.) {
                    builder.putValue(-1, iter.index(0));
                    builder.nextRecord();
                }
            }
            builder.putProperty(QDataSet.MONOTONIC, Boolean.TRUE);
        } else {
            builder = new DataSetBuilder(2, 100, ds.rank(), 1);
            while (iter.hasNext()) {
                iter.next();
                if (iter.getValue(ds) != 0.) {
                    builder.putValue(-1, 0, iter.index(0));
                    if (ds.rank() > 1) {
                        builder.putValue(-1, 1, iter.index(1));
                    }
                    if (ds.rank() > 2) {
                        builder.putValue(-1, 2, iter.index(2));
                    }
                    if (ds.rank() > 3) {
                        builder.putValue(-1, 3, iter.index(3));
                    }
                    builder.nextRecord();
                }
            }
            if (ds.rank() == 2) {
                builder.putProperty(QDataSet.DEPEND_1, labels(new String[]{"dim0", "dim1"}));
            } else if (ds.rank() == 3) {
                builder.putProperty(QDataSet.DEPEND_1, labels(new String[]{"dim0", "dim1", "dim2"}));
            } else if (ds.rank() == 4) {
                builder.putProperty(QDataSet.DEPEND_1, labels(new String[]{"dim0", "dim1", "dim2", "dim4"}));
            }
        }

        builder.putProperty(QDataSet.CADENCE, DataSetUtil.asDataSet(1.0) );

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
     * return the unique elements from the dataset.  If sort is null, then
     * the dataset is assumed to be monotonic, and only repeating values are
     * coalesced.  If sort is non-null, then it is the result of the function
     * "sort" and should be a rank 1 list of indeces that sort the data.
     *
     * @param ds
     * @param sort
     * @return
     */
    public static QDataSet uniq( QDataSet ds, QDataSet sort  ) {
        if ( ds.rank()>1 ) throw new IllegalArgumentException("ds.rank()>1" );
        if ( sort!=null && sort.rank()>1 ) throw new IllegalArgumentException("sort.rank()>1" );

        DataSetBuilder builder= new DataSetBuilder(1,100);

        builder.putProperty( QDataSet.UNITS, ds.property( QDataSet.UNITS ) );
        double d;
        if ( sort==null ) {
            DataSetIterator it= new QubeDataSetIterator(ds);
            if ( !it.hasNext() ) {
                return builder.getDataSet();
            }
            it.next();
            d= it.getValue(ds);
            while ( it.hasNext() ) {
                it.next();
                double d1= it.getValue(ds);
                if ( d!=d1 ) {
                    builder.putValue(-1, d);
                    builder.nextRecord();
                    d= d1;
                }
            }
        } else {
            DataSetIterator it= new QubeDataSetIterator(sort);
            if ( !it.hasNext() ) {
                return builder.getDataSet();
            }
            it.next();
            int i= (int) it.getValue(sort);
            d= ds.value(i);
            while ( it.hasNext() ) {
                it.next();
                i= (int) it.getValue(sort);
                double d1= ds.value(i);
                if ( d!=d1 ) {
                    builder.putValue(-1, d);
                    builder.nextRecord();
                    d= d1;
                }
            }
        }
        builder.putValue(-1, d);
        builder.nextRecord();
        return builder.getDataSet();

    }

    /**
     * returns the reverse of the rank 1 dataset.
     * @param ds
     * @return
     */
    public static QDataSet reverse( QDataSet ds ) {
        return new ReverseDataSet(ds);
    }

    /**
     * returns a rank 1 dataset of indeces that shuffle the rank 1 dataset ds
     * <pre>
     *   s= shuffle( ds )
     *   dsShuffled= ds[s]
     * </pre>
     * @param ds rank 1 dataset
     * @return rank 1 dataset of integer indeces.
     */
    public static QDataSet shuffle(QDataSet ds) {
        int size = ds.length();
        int[] back = new int[size];
        for (int i = 0; i < size; i++) {
            back[i] = i;
        }
        WritableDataSet wds = IDataSet.wrap(back, 1, size, 1, 1);

        Random r = new Random();

        for (int i = 0; i < size; i++) {
            int i1 = r.nextInt(size - i) + i;
            double t = wds.value(i1);
            wds.putValue(i1, wds.value(i));
            wds.putValue(i, t);
        }

        return wds;
    }

    /**
     * create a power spectrum on the dataset by breaking it up and
     * doing ffts on each segment.
     * 
     * right now only rank 2 data is supported, but there is no reason that
     * rank 1 shouldn't be supported.
     *
     * Looks for PLANE_0.USER_PROPERTIES.FFT_Translation, which should
     * be a rank 0 or rank 1 QDataSet.  If it is rank 1, then it should correspond
     * to the DEPEND_0 dimension.
     * 
     * @param ds rank 2 dataset ds(N,M) with M>len
     * @param len the number of elements to have in each fft.
     * @param mon a ProgressMonitor for the process
     * @return rank 2 fft spectrum
     */
    public static QDataSet fftPower( QDataSet ds, int len, ProgressMonitor mon ) {
        if ( mon==null ) {
            mon= new NullProgressMonitor();
        }
        if ( ds.rank()==2 ) {
            JoinDataSet result= new JoinDataSet(2);
            result.putProperty(QDataSet.JOIN_0, null);

            int nsam= ds.length()*(ds.length(0)/len); // approx
            DataSetBuilder dep0b= new DataSetBuilder(1,nsam );

            QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
            QDataSet dep1= (QDataSet) ds.property( QDataSet.DEPEND_1 );

            UnitsConverter uc= UnitsConverter.IDENTITY;

            QDataSet translation= null;
            if ( dep1!=null ) {
                Map<String,Object> user= (Map<String, Object>) dep1.property(QDataSet.USER_PROPERTIES );
                if ( user!=null ) {
                    translation= (QDataSet) user.get( "FFT_Translation" );
                    if ( translation.rank()==1 ) {
                        if ( translation.length()!=dep0.length() ) {
                            throw new IllegalArgumentException("rank 1 FFT_Translation should be the same length as depend_0");
                        }
                    }
                }
            }

            if ( dep1!=null && dep1.rank()==1 ) {
                QDataSet ytags= FFTUtil.getFrequencyDomainTagsForPower( dep1.trim(0,len) );
                if ( translation==null ) result.putProperty( QDataSet.DEPEND_1, ytags );
                Units dep1Units= (Units) dep1.property(QDataSet.UNITS);
                Units dep0Units= (Units) dep0.property(QDataSet.UNITS);
                if ( dep0Units!=null && dep1Units!=null ) uc= dep1Units.getConverter(dep0Units.getOffsetUnits());
            }

            mon.setTaskSize(ds.length());
            mon.started();
            mon.setProgressMessage("performing fftPower");
            for ( int i=0; i<ds.length(); i++ ) {
                for ( int j=0; j<ds.length(i)/len; j++ ) {
                    GeneralFFT fft = GeneralFFT.newDoubleFFT(len);
                    QDataSet wave= ds.slice(i).trim(j*len,(j+1)*len );
                    QDataSet weig= DataSetUtil.weightsDataSet(wave);
                    boolean hasFill= false;
                    for ( int k=0; k<weig.length(); k++ ) {
                        if ( weig.value(k)==0 ) {
                            hasFill= true;
                        }
                    }
                    if ( hasFill ) continue;

                    QDataSet vds= FFTUtil.fftPower( fft, wave );
                    if ( translation!=null ) {
                        QDataSet fftDep1= (QDataSet) vds.property( QDataSet.DEPEND_0 );
                        if (translation.rank()==0 ) {
                            fftDep1= Ops.add( fftDep1, translation );
                        } else if ( translation.rank()==1 ) {
                            fftDep1= Ops.add( fftDep1, translation.slice(i) );
                        } else {
                            throw new IllegalArgumentException("bad rank on FFT_Translation, expected rank 0 or rank 1");
                        }
                        ((MutablePropertyDataSet)vds).putProperty( QDataSet.DEPEND_0, fftDep1 );
                    }
                    result.join(vds);
                    if ( dep0!=null && dep1!=null ) {
                        dep0b.putValue(-1, dep0.value(i) + uc.convert( dep1.value( j*len + len/2 )  ) );
                        dep0b.nextRecord();
                    } else if ( dep0!=null ) {
                        dep0b.putValue(-1, dep0.value(i) );
                        dep0b.nextRecord();
                    } else {
                        dep0b= null;
                    }
                }
                mon.setTaskProgress(i);
            }
            mon.finished();
            if (dep0!=null ) {
                dep0b.putProperty(QDataSet.UNITS, dep0.property(QDataSet.UNITS) );
                result.putProperty(QDataSet.DEPEND_0, dep0b.getDataSet() );
            }
            
            return result;

        } else {
            throw new IllegalArgumentException("rank not supported: "+ ds.rank() );
        }
    }


    private static QDataSet fftPowerRank2( QDataSet ds ) {
        JoinDataSet result= new JoinDataSet(2);

        for ( int i=0; i<ds.length(); i++ ) {
            GeneralFFT fft = GeneralFFT.newDoubleFFT(ds.length(i));
            QDataSet vds= FFTUtil.fftPower( fft, DataSetOps.slice0(ds, i) );

            result.join(vds);
        }

        QDataSet dep1= (QDataSet) ds.property( QDataSet.DEPEND_1 );
        if ( dep1!=null && dep1.rank()==1 ) {
            QDataSet ytags= FFTUtil.getFrequencyDomainTagsForPower( dep1 );
            result.putProperty( QDataSet.DEPEND_1, ytags );
        }

        result.putProperty(QDataSet.DEPEND_0, ds.property(QDataSet.DEPEND_0));
        return result;

    }

    private static QDataSet fftPowerRank3( QDataSet ds ) {
        JoinDataSet result= new JoinDataSet(3);

        for ( int i=0; i<ds.length(); i++ ) {
            QDataSet vds= fftPowerRank2( DataSetOps.slice0(ds, i) );
            result.join(vds);
        }
        result.putProperty(QDataSet.DEPEND_0, ds.property(QDataSet.DEPEND_0));
        return result;

    }

    /**
     * returns the power spectrum of the waveform.  Positive frequencies
     * are returned for DEPEND_0, and square of the magnitude is returned for
     * the values.
     * 
     * @param ds rank 1 waveform or rank 2 array of waveforms
     * @return 
     */
    public static QDataSet fftPower( QDataSet ds ) {
        if ( ds.rank()==2 ) {
            return fftPowerRank2(ds);
        }
        if ( ds.rank()==3 ) {
            return fftPowerRank3(ds);
        }

        GeneralFFT fft = GeneralFFT.newDoubleFFT(ds.length());
        Units u = (Units) ds.property(QDataSet.UNITS);
        if (u == null) {
            u = Units.dimensionless;
        }
        ComplexArray.Double ca = FFTUtil.fft( fft, ds );

        QDataSet dep0 = (QDataSet) ds.property(QDataSet.DEPEND_0);
        RankZeroDataSet cadence = dep0 == null ? DRank0DataSet.create(1.0) : DataSetUtil.guessCadenceNew(dep0,null);
        if ( cadence==null ) throw new IllegalArgumentException("can't establish data cadence");

        double[] xtags = FFTUtil.getFrequencyDomainTags( 1./cadence.value(), ds.length());
        double binsize=  2 * xtags[ xtags.length/2 ] / ds.length();

        DDataSet result = DDataSet.createRank1(ds.length()/2);
        DDataSet resultDep0 = DDataSet.createRank1(ds.length()/2);
        for (int i = 0; i < ds.length()/2; i++) {
            result.putValue(i, (i==0?1:4)*ComplexArray.magnitude2(ca,i) / binsize );
            resultDep0.putValue( i, xtags[i] );
        }

        result.putProperty(QDataSet.DEPEND_0, resultDep0);
        return result;

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
        ComplexArray.Double cc = FFTUtil.fft(fft, ds);
        DDataSet result = DDataSet.createRank2(ds.length(), 2);
        for (int i = 0; i < ds.length(); i++) {
            result.putValue(i, 0, cc.getReal(i));
            result.putValue(i, 1, cc.getImag(i));
        }

        QDataSet dep0 = (QDataSet) ds.property(QDataSet.DEPEND_0);
        RankZeroDataSet cadence = dep0 == null ? DRank0DataSet.create(1.0) : DataSetUtil.guessCadenceNew(dep0,null);
        if ( cadence==null ) throw new IllegalArgumentException("can't establish data cadence");

        double[] tags = FFTUtil.getFrequencyDomainTags(1./cadence.value(), ds.length());
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
        QDataSet result = WaveformToSpectrum.getTableDataSet( ds, len);
        return result;
    }

    /**
     * returns a two element, rank 1 dataset containing the extent of the data.
     * Note this accounts for DELTA_PLUS, DELTA_MINUS properties.
     * The property QDataSet.SCALE_TYPE is set to lin or log.
     * The property count is set to the number of valid measurements.
     * TODO: this could use MONOTONIC, but it doesn't.  DELTA_PLUS, DELTA_MINUS make that more difficult.
     * @see DataSetUtil.rangeOfMonotonic( QDataSet ds ).
     * @param ds
     * @return two element, rank 1 "bins" dataset.
     */
    public static QDataSet extent( QDataSet ds ) {
        return extent( ds, null );
    }

    /**
     * returns a two element, rank 1 dataset containing the extent of the data.
     * Note this accounts for DELTA_PLUS, DELTA_MINUS properties.
     * The property QDataSet.SCALE_TYPE is set to lin or log.
     * The property count is set to the number of valid measurements.
     * 2010-10-14: add branch for monotonic datasets.
     * @param ds
     * @param range, if non-null, return the union of this range and the extent.  This must not contain fill!
     * @return two element, rank 1 "bins" dataset.
     */
    public static QDataSet extent( QDataSet ds, QDataSet range ) {

        QDataSet max = ds;
        QDataSet min = ds;
        QDataSet deltaplus;
        QDataSet deltaminus;

        deltaplus = (QDataSet) ds.property(QDataSet.DELTA_PLUS);
        deltaminus = (QDataSet) ds.property(QDataSet.DELTA_MINUS);

        QDataSet w = DataSetUtil.weightsDataSet(ds);
        int count=0;

        double [] result;
        double fill= ((Number)w.property(QDataSet.FILL_VALUE)).doubleValue();

        if ( range==null ) {
            result= new double[]{ Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        } else {
            result= new double[]{ range.value(0), range.value(1) };
            if ( range.value(0)==fill ) System.err.println("range passed into extent contained fill");
        }

        if ( ds.rank()==1 && Boolean.TRUE.equals( ds.property(QDataSet.MONOTONIC )) && deltaplus==null ) {
            int ifirst=0;
            int n= ds.length();
            int ilast= n-1;
            while ( ifirst<n && w.value(ifirst)==0.0 ) ifirst++;

            while ( ilast>=0 && w.value(ilast)==0.0 ) ilast--;
            count= Math.max( 0, ilast - ifirst + 1 );
            if ( count>0 ) {
                result[0]= Math.min( result[0], ds.value(ifirst) );
                result[1]= Math.max( result[1], ds.value(ilast) );
            } else {
                result[0] = range==null ? fill : range.value(0);
                result[1] = range==null ? fill : range.value(1);
            }

        } else {

            if (deltaplus != null) {
                max = Ops.add(ds, deltaplus );
            }

            if (deltaminus != null) {
                min = Ops.subtract(ds, deltaminus);
            }

            QubeDataSetIterator it = new QubeDataSetIterator(ds);

            while (it.hasNext()) {
                it.next();
                if (it.getValue(w) > 0.) {
                    count++;
                    result[0] = Math.min(result[0], it.getValue(min));
                    result[1] = Math.max(result[1], it.getValue(max));
                } else {

                }
            }
            if ( count==0 ) {  // no valid data!
                result[0] = fill;
                result[1] = fill;
            }
        }

        DDataSet qresult= DDataSet.wrap(result);
        qresult.putProperty( QDataSet.SCALE_TYPE, ds.property(QDataSet.SCALE_TYPE) );
        qresult.putProperty( QDataSet.USER_PROPERTIES, Collections.singletonMap( "count", new Integer(count) ) );
        qresult.putProperty( QDataSet.BINS_0, "min,maxInclusive" );
        qresult.putProperty( QDataSet.UNITS, ds.property(QDataSet.UNITS ) );
        if ( result[0]==fill ) qresult.putProperty( QDataSet.FILL_VALUE, fill);
        
        return qresult;
        
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
     * returns a histogram of the dataset, based on the extent and scaletype of the data.
     * @param ds
     * @param binCount number of bins
     * @return
     */
    public static QDataSet histogram( QDataSet ds, int binCount ) {
        
        if ( "log".equals(ds.property(QDataSet.SCALE_TYPE)) ) {
            QDataSet linds= Ops.log10(ds);
            QDataSet range= Ops.extent( linds );
            double width= range.value(1)-range.value(0);
            MutablePropertyDataSet h= (MutablePropertyDataSet) histogram( linds, range.value(0), range.value(1), width/binCount );
            MutablePropertyDataSet bins=  (MutablePropertyDataSet) h.property(QDataSet.DEPEND_0);
            
            bins= (MutablePropertyDataSet) Ops.exp10(bins);
            bins.putProperty( QDataSet.SCALE_TYPE, "log" );
            bins.putProperty( QDataSet.LABEL, ds.property(QDataSet.LABEL) );
            bins.putProperty( QDataSet.TITLE, ds.property(QDataSet.TITLE) );
            
            h.putProperty(QDataSet.DEPEND_0, bins);
            return h;

        } else {
            QDataSet range= Ops.extent( ds );
            double width= range.value(1)-range.value(0);
            return histogram( ds, range.value(0), range.value(1), width/binCount );
        }
        
    }

    /**
     * use one pass auto-scaling histogram
     * @param ds
     * @return
     */
    public static QDataSet autoHistogram( QDataSet ds ) {
        AutoHistogram ah= new AutoHistogram();
        return ah.doit(ds);
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

    /**
     * element-wise ceil function.
     * @param ds1
     * @return
     */
    public static QDataSet floor(QDataSet ds1) {
        return applyUnaryOp(ds1, new UnaryOp() {

            public double op(double a) {
                return Math.floor(a);
            }
        });
    }

    /**
     * element-wise ceil function.
     * @param ds1
     * @return
     */
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
        Units vvunits= (Units) vv.property(QDataSet.UNITS);
        if ( vvunits==null ) vvunits= Units.dimensionless;
        Units uuunits= (Units) uu.property(QDataSet.UNITS);
        if ( uuunits==null ) uuunits= Units.dimensionless;

        UnitsConverter uc= UnitsConverter.getConverter( vvunits, uuunits );

        while (it.hasNext()) {
            it.next();
            double d = uc.convert( it.getValue(vv) ); //TODO: assumes no fill data.
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
                } else if (index >= (n - 1)) {
                    ic0 = n - 2;
                    ic1 = n - 1;
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
     * in rank N findex.  For example, findex=1.5 means interpolate
     * the 1st and 2nd indeces with equal weight, 1.1 means
     * 90% of the first mixed with 10% of the second.  No extrapolation is
     * done, data with findex<0 or findex>(vv.length()-1) are assigned the
     * first or last value.
     *
     * Note there is no check on CADENCE.
     * Note nothing is done with DEPEND_0, presumably because was already
     * calculated and used for findex.
     * 
     * @param vv rank 1 dataset that is the data to be interpolated.
     * @param findex rank N dataset of fractional indeces.
     * @return the result.  
     */
    public static QDataSet interpolate(QDataSet vv, QDataSet findex) {
        DDataSet result = DDataSet.create(DataSetUtil.qubeDims(findex));
        QubeDataSetIterator it = new QubeDataSetIterator(findex);
        int ic0, ic1;
        int n = vv.length();

        QDataSet wds= DataSetUtil.weightsDataSet( vv );
        double fill= (Double)wds.property(QDataSet.FILL_VALUE);
        result.putProperty( QDataSet.FILL_VALUE, fill );

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

            if ( wds.value(ic0)>0 && wds.value(ic1)>0 ) {
                double vv0 = vv.value(ic0);
                double vv1 = vv.value(ic1);

                it.putValue(result, vv0 + alpha * (vv1 - vv0));
                
            } else {
                it.putValue(result, fill );
            }

        }

        return result;
    }

    /**
     * interpolate values from rank 2 dataset vv using fractional indeces
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
     * returns a dataset with zero where the data is invalid, and positive 
     * non-zero where the data is valid.  (This just returns the weights
     * plane of the dataset.)
     * 
     *   r= where( valid( ds ) )
     * 
     * @param ds a rank N dataset that might have FILL_VALUE, VALID_MIN or VALID_MAX
     *   set.
     * @return a rank N dataset with the same geometry, with zeros where the data
     *   is invalid and >0 where the data is valid.
     */
    public static QDataSet valid( QDataSet ds ) {
        return DataSetUtil.weightsDataSet(ds);
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
        DDataSet result = BinAverage.boxcar(ds, size);
        DataSetUtil.copyDimensionProperties( ds, result );
        
        return result;
    }

    /**
     * return array that is the differences between each successive pair in the dataset.
     * Result[i]= ds[i+1]-ds[i], so that for an array with N elements, an array with
     * N-1 elements is returned.  DEPEND_0 will contain the average of the two points.
     * @param ds a rank 1 dataset with N elements.
     * @return a rank 1 dataset with N-1 elements.
     * @see accum
     */
    public static QDataSet diff(QDataSet ds) {
        if (ds.rank() > 1) {
            throw new IllegalArgumentException("only rank 1");
        }
        if ( true ) {
            DDataSet result= DDataSet.createRank1( ds.length()-1 );
            QDataSet w1= DataSetUtil.weightsDataSet(ds);
            QDataSet dep0ds= (QDataSet) ds.property(QDataSet.DEPEND_0);
            DDataSet dep0= null;
            if ( dep0ds!=null ) {
                dep0= DDataSet.createRank1( ds.length()-1 );
                DataSetUtil.putProperties( DataSetUtil.getProperties(dep0ds), dep0 );
            }
            double fill= ((Number)w1.property( QDataSet.FILL_VALUE )).doubleValue();
            for ( int i=0; i<result.length(); i++ ) {
                if ( w1.value(i)>0 && w1.value(i+1)>0 ) {
                    result.putValue(i, ds.value(i+1) - ds.value(i) );
                } else {
                    result.putValue(i,fill);
                }
                if ( dep0ds!=null ) dep0.putValue(i, ( dep0ds.value(i+1) + dep0ds.value(i)) / 2 );
            }
            result.putProperty(QDataSet.FILL_VALUE, new Double(fill) );
            Units u= (Units) ds.property(QDataSet.UNITS);
            if ( u!=null ) result.putProperty(QDataSet.UNITS, u.getOffsetUnits() );
            result.putProperty(QDataSet.NAME, null );
            result.putProperty(QDataSet.MONOTONIC, null );
            if ( dep0ds!=null ) {
                result.putProperty( QDataSet.DEPEND_0, dep0 );
            }
            String label= (String) ds.property(QDataSet.LABEL);
            if ( label!=null ) result.putProperty(QDataSet.LABEL, "diff("+label+")" );
            
            return result;
        } else {
            TrimStrideWrapper d1 = new TrimStrideWrapper(ds); //TODO: use .trim() operator here if we use this again.
            d1.setTrim(0, 0, ds.length() - 1, 1);
            TrimStrideWrapper d2 = new TrimStrideWrapper(ds);
            d2.setTrim(0, 1, ds.length(), 1);
            QDataSet result = Ops.subtract(d2, d1);
            return result;
        }
    }

    /**
     * return an array that is the running sum of each element in the array,
     * starting with the value accum.
     * Result[i]= accum + total( ds[0:i+1] )
     * @param accum the initial value of the running sum.  Last value of Rank 0 or Rank 1 dataset is used, or may be null.
     * @param ds each element is added to the running sum
     * @return the running of each element in the array.
     * @see diff
     */
    public static QDataSet accum( QDataSet accumDs, QDataSet ds ) {
        if (ds.rank() > 1) {
            throw new IllegalArgumentException("only rank 1");
        }
        double accum=0;
        QDataSet accumDep0Ds=null;
        double accumDep0=0;
        QDataSet dep0ds= (QDataSet) ds.property(QDataSet.DEPEND_0);
        if ( accumDs==null ) {
            accumDep0= dep0ds!=null ? dep0ds.value(0) : 0;
        } else if ( accumDs.rank()==0 ) {
            accum= accumDs.value();
            accumDep0Ds= (QDataSet) accumDs.property( QDataSet.CONTEXT_0 );
            if ( accumDep0Ds!=null ) accumDep0= accumDep0Ds.value(); else accumDep0=0;
        } else if ( accumDs.rank()==1 ) {
            accum= accumDs.value(accumDs.length()-1);
            accumDep0Ds= (QDataSet)  accumDs.property( QDataSet.DEPEND_0 );
            if ( accumDep0Ds!=null ) accumDep0= accumDep0Ds.value(accumDs.length()); else accumDep0=0;
        }
        WritableDataSet result= zeros( ds );
        DDataSet dep0= null;
        if ( dep0ds!=null ) {
            dep0= DDataSet.createRank1( ds.length() );
            DataSetUtil.putProperties( DataSetUtil.getProperties(dep0ds), dep0 );
        }
        for ( int i=0; i<result.length(); i++ ) {
            accum+= ds.value(i);
            result.putValue(i,accum);
            if ( dep0ds!=null ) {
                dep0.putValue(i, ( accumDep0 + dep0ds.value(i)) / 2 );
                accumDep0= dep0ds.value(i);
            }
        }
        if ( dep0!=null ) {
            //result.putProperty( QDataSet.DEPEND_0, dep0 );
        }

        return result;
    }


    /**
     * return an array that is the running sum of each element in the array,
     * starting with the value accum.
     * Result[i]= total( ds[0:i+1] )
     * @param ds each element is added to the running sum
     * @return the running of each element in the array.
     * @see diff
     */
    public static QDataSet accum( QDataSet ds ) {
        return accum( null, ds );
    }

    /**
     * create a labels dataset for tagging rows of a dataset.
     * Example:
     * <tt>dep1= labels( ["X","Y","Z"], "GSM" )</tt>
     * @param labels
     * @param context
     * @return rank 1 QDataSet
     */
    public static QDataSet labels(String[] labels, String context) {
        EnumerationUnits u = new EnumerationUnits(context);
        SDataSet result = SDataSet.createRank1(labels.length);
        for (int i = 0; i < labels.length; i++) {
            Datum d = u.createDatum(labels[i]);
            result.putValue(i, d.doubleValue(u));
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
    public static QDataSet labels(String[] labels) {
        return labels(labels, "default");
    }

    /**
     * TODO: I suspect this is not up to spec.  See DataSetOps.sliceProperties
     * @param removeDim
     * @param ds
     * @param result
     */
    private static void sliceProperties(int removeDim, QDataSet ds, MutablePropertyDataSet result) {
        for (int i = 0; i < result.rank(); i++) {
            if (i >= removeDim) {
                result.putProperty("DEPEND_" + i, ds.property("DEPEND_" + (i + 1)));
            } else {
                result.putProperty("DEPEND_" + i, ds.property("DEPEND_" + i));
            }
        }
    }

    /**
     * Reshape the dataset to remove the first dimension with length 1, reducing
     * its rank by 1.  Dependencies are also preserved.
     * @param ds
     * @return
     */
    public static QDataSet reform(QDataSet ds) {
        int[] dsqube = DataSetUtil.qubeDims(ds);
        List<Integer> newQube = new ArrayList<Integer>();
        int[] dimMap = new int[dsqube.length]; // maps from new dataset to old index
        boolean foundDim = false;
        int removeDim = -1;
        for (int i = 0; i < dsqube.length; i++) {
            if (dsqube[i] != 1 || foundDim) {
                newQube.add(dsqube[i]);
                dimMap[i] = foundDim ? i + 1 : i;
            } else {
                foundDim = true;
                removeDim = i;
            }
        }
        if (foundDim == false) {
            throw new IllegalArgumentException("there were no dimensions with length 1");
        }
        int[] qube = new int[newQube.size()];
        for (int i = 0; i < newQube.size(); i++) {
            qube[i] = newQube.get(i);
        }
        MutablePropertyDataSet result = (MutablePropertyDataSet) reform(ds, qube); //DANGER
        sliceProperties(removeDim, ds, result);
        return result;
    }

    public static QDataSet reform(QDataSet ds, int[] qube) {
        QubeDataSetIterator it0 = new QubeDataSetIterator(ds);
        DDataSet result = DDataSet.create(qube);
        QubeDataSetIterator it1 = new QubeDataSetIterator(result);
        while (it0.hasNext()) {
            it0.next();
            it1.next();
            double v = it0.getValue(ds);
            it1.putValue(result, v);
        }
        return result;
    }

    /**
     * bundle the two datasets, adding if necessary a bundle dimension.  This
     * will try to bundle on the second dimension, unlike join.  This will also
     * isolate the semmantics of bundle dimensions as it's introduced.
     * @param ds1
     * @param ds2
     * @return
     */
    public static QDataSet bundle( QDataSet ds1, QDataSet ds2 ) {
        if ( ds1==null && ds2!=null ) {
            BundleDataSet ds= new BundleDataSet( );
            ds.bundle(ds2);
            return ds;
        } else if (ds1.rank() == ds2.rank()) {
            BundleDataSet ds= new BundleDataSet( );
            ds.bundle(ds1);
            ds.bundle(ds2);
            return ds;
        } else if ( ds1 instanceof BundleDataSet && ds1.rank()-1==ds2.rank() ) {
            ((BundleDataSet)ds1).bundle(ds2);
            return ds1;
        } else {
            throw new IllegalArgumentException("not supported yet");
        }

    }

    /**
     * return true if DEPEND_1 is set and its units are EnumerationUnits.  This
     * was the pre-bundle way of representing a bundle of datasets.  It might
     * be supported indefinitely, because it has some nice rules about the data.
     * For example, data must be of the same units since there is no place to put
     * the property.
     * @param zds
     * @return
     */
    public static boolean isLegacyBundle( QDataSet zds ) {
        QDataSet dep1= (QDataSet) zds.property(QDataSet.DEPEND_1);
        if ( dep1!=null ) {
            Units u= (Units) dep1.property(QDataSet.UNITS);
            if ( u instanceof EnumerationUnits ) {
                return true;
            }
        }
        return false;
    }

    /**
     * return true if the dataset is a bundle.  It is rank 2 or rank 1, and
     * has the last dimension a bundle dimension.
     * @param zds
     * @return
     */
    public static boolean isBundle( QDataSet zds ) {
        if ( zds.rank()==1 ) {
            return zds.property(QDataSet.BUNDLE_0)!=null;
        } else if ( zds.rank()==2 ) {
            return zds.property(QDataSet.BUNDLE_1)!=null;
        } else {
            return false;
        }
        
    }

    /**
     * like bundle, but declare the last dataset is dependent on the first one.
     *
     * @param x rank 1 dataset
     * @param y rank 1 or rank 2 dataset
     * @return
     */
    public static QDataSet link( QDataSet x, QDataSet y ) {
        if (y.rank() == 1) {
            String xname= (String) x.property(QDataSet.NAME);
            String yname= (String) y.property(QDataSet.NAME);
            if ( xname==null ) xname="data0";
            if ( yname==null ) yname="data1";
            QDataSet result= bundle( x, y );
            BundleDataSet.BundleDescriptor bds= (BundleDescriptor) result.property(QDataSet.BUNDLE_1);
            bds.putProperty( "CONTEXT_0", 1, xname );
            bds.putProperty( QDataSet.NAME, 0, xname );
            bds.putProperty( QDataSet.NAME, 1, yname );
            bds.putProperty( QDataSet.DEPEND_0, 1, xname );
            List<String> problems= new ArrayList();
            if ( DataSetUtil.validate(result, problems) ) {
                return result;
            } else {
                throw new IllegalArgumentException( problems.get(0) );
            }
        } else {
            ArrayDataSet zds = ArrayDataSet.copy(y);
            if (x != null) {
                zds.putProperty(QDataSet.DEPEND_0, x);
            }
            List<String> problems= new ArrayList();
            if ( !DataSetUtil.validate(zds, problems ) ) {
                throw new IllegalArgumentException( problems.get(0) );
            } else {
                return zds;
            }
        }

    }

    /**
     * like bundle, but declare the last dataset is dependent on the first two.
     *
     * @param x rank 1 dataset
     * @param y rank 1 dataset
     * @param z rank 1 or 2 dataset.
     * @return
     */
    public static QDataSet link( QDataSet x, QDataSet y, QDataSet z ) {
        if (z.rank() == 1) {
            String xname= (String) x.property(QDataSet.NAME);
            String yname= (String) y.property(QDataSet.NAME);
            if ( xname==null ) xname="data0";
            if ( yname==null ) yname="data1";
            QDataSet result= bundle( bundle( x, y ), z );
            BundleDataSet.BundleDescriptor bds= (BundleDescriptor) result.property(QDataSet.BUNDLE_1);
            bds.putProperty( "CONTEXT_0", 2, xname+","+yname );
            bds.putProperty( QDataSet.NAME, 0, xname );
            bds.putProperty( QDataSet.NAME, 1, yname );

//            DDataSet yds = DDataSet.copy(y);
//            yds.putProperty(QDataSet.DEPEND_0, x);
//            yds.putProperty(QDataSet.PLANE_0, z);
            List<String> problems= new ArrayList();
            if ( DataSetUtil.validate(result, problems ) ) {
                return result;
            } else {
                throw new IllegalArgumentException( problems.get(0) );
            }
        } if ( z.rank()==2 && isBundle(z) ) {
            QDataSet z1= DataSetOps.slice1(z,z.length(0)-1);
            return link( x, y, z1 );

        } else {
            ArrayDataSet zds = ArrayDataSet.copy(z);
            if (x != null) {
                zds.putProperty(QDataSet.DEPEND_0, x);
            }
            if (y != null ) {
                zds.putProperty(QDataSet.DEPEND_1, y);
            }
            List<String> problems= new ArrayList();
            if ( !DataSetUtil.validate(zds, problems ) ) {
                throw new IllegalArgumentException( problems.get(0) );
            } else {
                return zds;
            }
        }

    }

    /**
     * declare that the dataset is a dependent parameter of an independent parameter.
     * This isolates the QDataSet semantics, and verifies correctness.
     * @param ds
     * @param dim dimension to declare dependence: 0,1,2.
     * @param dep0
     * @return
     */
    public static MutablePropertyDataSet dependsOn( QDataSet ds, int dim, QDataSet dep0 ) {
        MutablePropertyDataSet mds= DataSetOps.makePropertiesMutable(ds);
        if ( dim==0 ) {
            if ( dep0!=null && ds.length()!=dep0.length() ) {
                throw new IllegalArgumentException(String.format("ds.length()!=dep.length() (%d!=%d)",ds.length(),dep0.length()));
            }
            mds.putProperty( QDataSet.DEPEND_0, dep0 );
        } else if ( dim==1 ) {
            if ( dep0!=null && ds.length(0)!=dep0.length() ) 
                throw new IllegalArgumentException(String.format("ds.length(0)!=dep.length() (%d!=%d)",ds.length(0),dep0.length()));
            mds.putProperty( QDataSet.DEPEND_1, dep0 );
        } else if ( dim==2 ) {
            if ( dep0!=null && ds.length(0,0)!=dep0.length() ) 
                throw new IllegalArgumentException(String.format("ds.length(0,0)!=dep.length() (%d!=%d)",ds.length(0,0),dep0.length()));
            mds.putProperty( QDataSet.DEPEND_2, dep0 );
        }
        return mds;
    }

    /**
     * Join two rank N datasets to make a rank N+1 dataset, with the first dimension
     * having two elements.  This is the anti-slice operator.  
     * 
     * If the first dataset is rank N+1 JoinDataset and the other is rank N, then the rank N dataset is
     * added to the rank N+1 dataset.
     * 
     * This is underimplemented right now, and can only join two rank N datasets
     * or if the first dataset is the result of a join.
     * 
     * @param ds1 rank N dataset, or null
     * @param ds2 rank N dataset
     * @see slice
     * @see concatenate
     * @return rank N+1 dataset
     */
    public static QDataSet join(QDataSet ds1, QDataSet ds2) {
        if ( ds1==null && ds2!=null ) {
            JoinDataSet ds= new JoinDataSet( ds2.rank()+1 );
            ds.join(ds2);
            return ds;
        } else if (ds1.rank() == ds2.rank()) {
            JoinDataSet ds= new JoinDataSet( ds1.rank()+1 );
            ds.join(ds1);
            ds.join(ds2);
            return ds;
        } else if ( ds1 instanceof JoinDataSet && ds1.rank()-1==ds2.rank() ) {
            ((JoinDataSet)ds1).join(ds2);
            return ds1;
        } else {
            throw new IllegalArgumentException("not supported yet");
        }
    }

    /**
     * made a Java-style identifier from the provided string
     * @param suggest
     * @return
     */
    public static String safeName( String suggest ) {
        StringBuilder result= new StringBuilder( suggest.replaceAll(" ", "" ) );
        if ( result.charAt(0)<'A' ) result.replace( 0, 1, "_" );
        if ( result.charAt(0)>'z' ) result.replace( 0, 1, "_" );
        if ( result.charAt(0)!='_' && result.charAt(0)>90 && result.charAt(0)<97 ) {
            result.replace( 0, 1, "_" );
        }
        for ( int i=1; i<result.length(); i++ ) {
            if ( result.charAt(i)<'0' ) result.replace( i, i+1, "_" );
            if ( result.charAt(i)>'z' ) result.replace( i, i+1, "_" );
            if ( result.charAt(i)!='_' && result.charAt(i)>57 && result.charAt(i)<65 ) {
                result.replace( i, i+1, "_" );
            }
            if ( result.charAt(i)!='_' && result.charAt(i)>90 && result.charAt(i)<97 ) {
                result.replace( i, i+1, "_" );
            }
        }
        return result.toString();
    }

    public static QDataSet transpose(QDataSet ds) {
        return DDataSet.copy(new TransposeRank2DataSet(ds));
    }

    /**
     * returns true iff the dataset values are equivalent.  Note this
     * may promote rank, etc.
     * @param ds1
     * @param ds2
     * @return
     */
    public static boolean equivalent( QDataSet ds1, QDataSet ds2 ) {
        QDataSet eq= eq( ds1, ds2 );
        QubeDataSetIterator it= new QubeDataSetIterator(eq);
        while ( it.hasNext() ) {
            it.next();
            if ( it.getValue(eq)==0 ) return false;
        }
        return true;
    }

    /**
     * returns the number of physical dimensions of a dataset.
     *   JOIN, BINS   do not increase dataset dimensionality.
     *   DEPEND       increases dimensionality by dimensionality of DEPEND ds.
     *   BUNDLE       increases dimensionality by N where N is the number of bundled datasets.
     * Note this includes implicit dimensions taken by the primary dataset.
     *   Z(time,freq)->3
     *   rand(20,20)->3
     *   B_gsm(20,[X,Y,Z])->4
     * @param ds
     *
     * @return the number of dimensions occupied by the data.
     */
    public static int dimensionCount( QDataSet dss ) {
        return dimensionCount( dss, false );
    }

    private static int dimensionCount( QDataSet dss, boolean noImplicit ) {
        int dim=1;
        QDataSet ds= dss;
        while ( ds.rank()>0 ) {
            if ( ds.property("JOIN_0")!=null ) {

            } else if ( ds.property("BINS_0")!=null ) {
                
            } else if ( ds.property("DEPEND_0")!=null ) {
                dim+= dimensionCount( (QDataSet) ds.property("DEPEND_0"), true );
            } else if ( ds.property("BUNDLE_0")!=null ) {
                dim+= ((QDataSet)ds.property("BUNDLE_0")).length();
            } else {
                if ( !noImplicit ) dim+= 1; // implicity undeclared dimensions add one dimension
            }
            if ( ds.length()>0 ) {
                ds= DataSetOps.slice0(ds, 0);
            } else {
                throw new IllegalArgumentException("dataset is empty");
            }
        }
        return dim;
    }

    public static double PI = Math.PI;
    public static double E = Math.E;
}
