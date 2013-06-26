/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dsops;

import java.lang.reflect.Array;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import java.util.Map.Entry;
import java.util.Random;
import java.util.regex.Pattern;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.DatumUtil;
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.UnitsConverter;
import org.das2.datum.UnitsUtil;
import org.das2.util.LoggerManager;
import org.das2.util.monitor.NullProgressMonitor;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.util.monitor.SubTaskMonitor;
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
import org.virbo.dataset.LDataSet;
import org.virbo.dataset.MutablePropertyDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.RankZeroDataSet;
import org.virbo.dataset.ReverseDataSet;
import org.virbo.dataset.SDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dataset.TransposeRank2DataSet;
import org.virbo.dataset.TrimStrideWrapper;
import org.virbo.dataset.WeightsDataSet;
import org.virbo.dataset.WritableDataSet;
import org.virbo.dataset.WritableJoinDataSet;
import org.virbo.dsutil.AutoHistogram;
import org.virbo.dsutil.BinAverage;
import org.virbo.dsutil.DataSetBuilder;
import org.virbo.dsutil.FFTUtil;
import org.virbo.dsutil.LinFit;
import org.virbo.math.Contour;

/**
 * A fairly complete set of operations for QDataSets.  Currently, most operations
 * require that the dataset be a qube.  
 * Most operations check for fill data. 
 * Few operations check units. (TODO: check units)
 * 
 * 2013-06-24: add logic so that most routines handle arrays and scalars as well as QDataSets.
 * 
 * @author jbf
 */
public class Ops {

    private static final Logger logger= LoggerManager.getLogger("qdataset");
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
    public static MutablePropertyDataSet applyUnaryOp(QDataSet ds1, UnaryOp op) {
        //TODO: handle JOIN from RPWS group, which is not a QUBE...
        DDataSet result = DDataSet.create(DataSetUtil.qubeDims(ds1));
        QDataSet wds= DataSetUtil.weightsDataSet(ds1);
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
        m.remove( QDataSet.BUNDLE_1 ); // because this contains FILL_VALUE, etc that are no longer correct.
        
        DataSetUtil.putProperties( m, result );
        result.putProperty( QDataSet.FILL_VALUE, fill );
        return result;
    }
    
    public static MutablePropertyDataSet applyUnaryOp(Object ds1, UnaryOp op) {
        return applyUnaryOp( dataset(ds1), op );
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
    public static MutablePropertyDataSet applyBinaryOp( QDataSet ds1, QDataSet ds2, BinaryOp op ) {
        //TODO: handle JOIN from RPWS group, which is not a QUBE...
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

        Map<String, Object> m1 = DataSetUtil.getProperties(operands[0]);
        Map<String, Object> m2 = DataSetUtil.getProperties(operands[1]);
        boolean resultIsQube= Boolean.TRUE.equals( m1.get(QDataSet.QUBE) ) || Boolean.TRUE.equals( m2.get(QDataSet.QUBE) );
        if ( m1.size()==1 ) m1.remove( QDataSet.QUBE ); // kludge: CoerceUtil.coerce would copy over a QUBE property, so just remove this.
        if ( m2.size()==1 ) m2.remove( QDataSet.QUBE );
        if ( m2.isEmpty() && !m1.isEmpty() ) {
            m2.put( QDataSet.DEPEND_0, m1.get(QDataSet.DEPEND_0 ) );
            m2.put( QDataSet.DEPEND_1, m1.get(QDataSet.DEPEND_1 ) );
            m2.put( QDataSet.DEPEND_2, m1.get(QDataSet.DEPEND_2 ) );
            m2.put( QDataSet.DEPEND_3, m1.get(QDataSet.DEPEND_3 ) );
            m2.put( QDataSet.BINS_0,   m1.get(QDataSet.BINS_0 ) );
            m2.put( QDataSet.BINS_1,   m1.get(QDataSet.BINS_1 ) );
        }
        if ( m1.isEmpty() && !m2.isEmpty() ) {
            m1.put( QDataSet.DEPEND_0, m2.get(QDataSet.DEPEND_0 ) );
            m1.put( QDataSet.DEPEND_1, m2.get(QDataSet.DEPEND_1 ) );
            m1.put( QDataSet.DEPEND_2, m2.get(QDataSet.DEPEND_2 ) );
            m1.put( QDataSet.DEPEND_3, m2.get(QDataSet.DEPEND_3 ) );
            m1.put( QDataSet.BINS_0,   m2.get(QDataSet.BINS_0 ) );
            m1.put( QDataSet.BINS_1,   m2.get(QDataSet.BINS_1 ) );
        }
        Map<String, Object> m3 = equalProperties(m1, m2);
        if ( resultIsQube ) {
            m3.put( QDataSet.QUBE, Boolean.TRUE );
        }
        String[] ss= DataSetUtil.dimensionProperties();
        for ( String s: ss ) {
            m3.remove( s );
        }
        // because this contains FILL_VALUE, etc that are no longer correct.  
        // My guess is that anything with a BUNDLE_1 property shouldn't have 
        // been passed into this routine anyway.
        m3.remove( QDataSet.BUNDLE_1 ); 

        DataSetUtil.putProperties(m3, result);    
        result.putProperty( QDataSet.FILL_VALUE, fill );
        
        return result;
    }
    
    public static MutablePropertyDataSet applyBinaryOp( Object ds1, Object ds2, BinaryOp op ) {
        return applyBinaryOp( dataset(ds1), dataset(ds2), op );
    }

//    public static MutablePropertyDataSet applyBinaryOp(QDataSet ds1, double d2, BinaryOp op) {
//        //TODO: handle JOIN from RPWS group, which is not a QUBE...
//        DDataSet result = DDataSet.create(DataSetUtil.qubeDims(ds1));
//
//        QubeDataSetIterator it1 = new QubeDataSetIterator(ds1);
//        QDataSet w1= DataSetUtil.weightsDataSet(ds1);
//
//        double fill= -1e38;
//        while (it1.hasNext()) {
//            it1.next();
//            double w= it1.getValue(w1);
//            it1.putValue(result, w==0 ? fill : op.op(it1.getValue(ds1), d2));
//        }
//        Map<String,Object> props= DataSetUtil.getProperties(ds1);
//        props.remove( QDataSet.VALID_MIN );
//        props.remove( QDataSet.VALID_MAX );
//        props.remove( QDataSet.TITLE );
//        props.remove( QDataSet.LABEL );
//        props.remove( QDataSet.MONOTONIC );
//        props.remove( QDataSet.METADATA_MODEL );
//        props.remove( QDataSet.METADATA );
//        props.remove( QDataSet.BUNDLE_1 ); // because this contains FILL_VALUE, etc that are no longer correct.
//
//        DataSetUtil.putProperties(props, result);
//        result.putProperty( QDataSet.FILL_VALUE, fill );
//        
//        return result;
//    }

    /**
     * returns the subset of two groups of properties that are equal, so these
     * may be preserved through operations.
     * @param m1 map of dataset properties, including DEPEND properties.
     * @param m2 map of dataset properties, including DEPEND properties.
     * @return
     */
    public static HashMap<String, Object> equalProperties(Map<String, Object> m1, Map<String, Object> m2) {
        HashMap result = new HashMap();
        for ( Entry<String,Object> e : m1.entrySet()) {
            String k= e.getKey();
            Object v = e.getValue();
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
     * returns the BinaryOp that handles the addition operation.  Properties will have the units inserted.
     * @param ds1
     * @param ds2
     * @param properties
     * @return
     */
    private static BinaryOp addGen( QDataSet ds1, QDataSet ds2, Map properties ) {
        Units units1 = SemanticOps.getUnits( ds1 );
        Units units2 = SemanticOps.getUnits( ds2 );
        BinaryOp result;
        if ( units2.isConvertableTo(units1) && UnitsUtil.isRatioMeasurement(units1) ) {
            final UnitsConverter uc= UnitsConverter.getConverter( units2, units1);
            result= new BinaryOp() {
                public double op(double d1, double d2) {
                    return d1 + uc.convert(d2);
                }
            };
            if ( units1!=Units.dimensionless ) properties.put( QDataSet.UNITS, units1 );
        } else if ( UnitsUtil.isIntervalMeasurement(units1) ) {
            final UnitsConverter uc= UnitsConverter.getConverter( units2, units1.getOffsetUnits() );
            result= new BinaryOp() {
                public double op(double d1, double d2) {
                    return d1 + uc.convert(d2);
                }
            };
            properties.put( QDataSet.UNITS, units1 );
        } else if ( UnitsUtil.isIntervalMeasurement(units2) ) {
            final UnitsConverter uc= UnitsConverter.getConverter( units1, units2.getOffsetUnits() );
            result= new BinaryOp() {
                public double op(double d1, double d2) {
                    return uc.convert(d1) + d2;
                }
            };
            properties.put( QDataSet.UNITS, units2 );
        } else {
            throw new IllegalArgumentException("units cannot be added: " + units1 + ", "+ units2 );
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
        Map<String,Object> props= new HashMap();
        BinaryOp b= addGen( ds1, ds2, props );
        MutablePropertyDataSet result= applyBinaryOp( ds1, ds2, b );
        result.putProperty( QDataSet.UNITS, props.get(QDataSet.UNITS) );
        result.putProperty(QDataSet.NAME, null );
        result.putProperty(QDataSet.LABEL, maybeLabelInfixOp( ds1, ds2, "+" ) );
        return result;
    }

    public static QDataSet add( Object ds1, Object ds2 ) {
        return add( dataset(ds1), dataset(ds2) );
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

    public static QDataSet subtract( Object ds1, Object ds2 ) {
        return subtract( dataset(ds1), dataset(ds2) );
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
        return opStr + "(" + l1Str + "," + l2Str + ")";
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
     * If units are specified, Units must be ratiometric units, like "5 km" 
     * or dimensionless, and not ordinal or time location units.
     * @see copysign
     * @see signum
     * @param ds1
     * @return
     */
    public static QDataSet negate(QDataSet ds1) {
        Units u= SemanticOps.getUnits(ds1);
        if ( !UnitsUtil.isRatioMeasurement(u) ) {
            throw new IllegalArgumentException("Units are not ratiometric units");
        }
        MutablePropertyDataSet mpds= applyUnaryOp(ds1, new UnaryOp() {
            public double op(double d1) {
                return -d1;
            }
        });
        mpds.putProperty(QDataSet.UNITS,u);
        return mpds;
    }
    
    public static QDataSet negate( Object ds1 ) {
        return negate( dataset(ds1) );
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
                isCart = depn.length()<4;  // loosen up restrictions
            }
        } else {
            int[] qubedims= DataSetUtil.qubeDims(ds);
            isCart = qubedims[r-1]<4;  // loosen up restrictions
        }
        if (isCart) {
            Units u= (Units) ds.property(QDataSet.UNITS);
            ds = pow(ds, 2);
            ds = total(ds, r - 1);
            ds = sqrt(ds);
            if ( u!=null ) ((MutablePropertyDataSet)ds).putProperty(QDataSet.UNITS,u);
            return ds;
        } else {
            throw new IllegalArgumentException("last dim must have COORDINATE_FRAME property.  See also abs() method");
        }

    }
    
    public static QDataSet magnitude( Object ds1 ) {
        return magnitude( dataset(ds1) );
    }

    /**
     * return the total of all the elements in the dataset, returning a rank
     * 0 dataset.  If there are invalid measurements, then fill is returned.
     * Does not support BINS or BUNDLE dimensions.
     *
     * @param ds
     * @return the unweighted total of the dataset, or -1e31 if fill was encountered.
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

    public static double total( Object ds1 ) {
        return total( dataset(ds1) );
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
     * reduce the dataset's rank by combining all the elements along a dimension.
     * AverageOp is used to combine measurements.
     * Only QUBEs are supported presently.
     * 
     * @param ds rank N qube dataset.
     * @param dim zero-based index number.
     * @param AverageOp operation to combine measurements, such as max or mean.
     * @return
     */
    private static QDataSet averageGen(QDataSet ds, int dim, AverageOp op) {
        if ( ds==null ) throw new NullPointerException("ds reference is null");
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
        Map<String,Object> props= DataSetUtil.getProperties(ds);
        props= DataSetOps.sliceProperties( props, dim );
        DataSetUtil.putProperties( props, result );
        result.putProperty(QDataSet.FILL_VALUE,fill);

        QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
        if ( dim==0 && dep0!=null ) {
            QDataSet extent= Ops.extent(dep0);
            DataSetUtil.addContext( result, extent );
        }

        return result;
    }

    /**
     * reduce the dataset's rank by totaling all the elements along a dimension.
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
        Map<String,Object> props= DataSetUtil.getProperties(ds);
        props= DataSetOps.sliceProperties( props, dim );
        DataSetUtil.putProperties( props, result );
        result.putProperty(QDataSet.WEIGHTS_PLANE,weights);
        result.putProperty(QDataSet.FILL_VALUE,fill);
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
     * this is introduced to mimic the in-line function which reduces the dimensionality by averaging over the zeroth dimension.
     *   collapse1( ds[30,20] ) -> ds[20]
     * @param fillDs
     * @param st the start index
     * @param en the non-inclusive end index
     * @return the averaged dataset
     */
    public static QDataSet collapse0( QDataSet fillDs, int st, int en ) {
        fillDs= fillDs.trim( st,en );
        fillDs= Ops.reduceMean(fillDs,0);
        return fillDs;
    }

    /**
     * this is introduced to mimic the in-line function which reduces the dimensionality by averaging over the zeroth dimension.
     *   collapse1( ds[30,20] ) -> ds[20]
     * @param fillDs
     * @return the averaged dataset
     */
    public static QDataSet collapse0( QDataSet fillDs ) {
        fillDs= Ops.reduceMean(fillDs,0);
        return fillDs;
    }

    /**
     * this is introduced to mimic the in-line function which reduces the dimensionality by averaging over the first dimension
     *   collapse1( ds[30,20] ) -> ds[30]
     * @param fillDs
     * @return the averaged dataset
     */
    public static QDataSet collapse1( QDataSet fillDs ) {
        fillDs= Ops.reduceMean(fillDs,1);
        return fillDs;
    }

    /**
     * this is introduced to mimic the in-line function which reduces the dimensionality by averaging over the first dimension
     *   collapse2( ds[30,20,10,5] ) -> ds[30,20,5]
     * @param fillDs
     * @return the averaged dataset
     */
    public static QDataSet collapse2( QDataSet fillDs ) {
        fillDs= Ops.reduceMean(fillDs,2);
        return fillDs;
    }

    /**
     * this is introduced to mimic the in-line function which reduces the dimensionality by averaging over the first dimension
     *   collapse3( ds[30,20,10,5] ) -> ds[30,20,10]
     * @param fillDs
     * @return the averaged dataset
     */
    public static QDataSet collapse3( QDataSet fillDs ) {
        fillDs= Ops.reduceMean(fillDs,3);
        return fillDs;
    }

    /**
     * trim the dataset to the indeces on the zeroth dimension.  Note
     * the trim function can also be called directly.
     * @param ds the dataset to be trimmed.
     * @param st the start index
     * @param en the non-inclusive end index
     * @return
     */
    public static QDataSet trim( QDataSet ds, int st, int en ) {
        return ds.trim( st, en );
    }

    /**
     * element-wise sqrt.
     * @param ds
     * @return
     */
    public static QDataSet sqrt(QDataSet ds) {
        //MutablePropertyDataSet result= (MutablePropertyDataSet) pow(ds, 0.5);
        MutablePropertyDataSet result= applyUnaryOp( ds, new UnaryOp() {
            public double op(double d1) {
                return Math.sqrt(d1);
            }
        } );
        return result;
    }

    public static double sqrt( double ds1 ) {
        return Math.sqrt( ds1 );
    }

    public static QDataSet sqrt( Object ds1 ) {
        return sqrt( dataset(ds1) );
    }
    
    /**
     * element-wise abs.  For vectors, this returns the length of each element.
     * Note Jython conflict needs to be resolved.
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

    public static long abs( long x ) {
        return Math.abs( x );
    }
    
    public static double abs( double ds1 ) {
        return Math.abs( ds1 );
    }

    public static QDataSet abs( Object ds1 ) {
        return abs( dataset(ds1) );
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
     * for Jython, we define this because the doubles aren't coerced.  
     * Note that pow(2,4) returns a long, not an int like Python 2.6.5
     * @param x
     * @param y
     * @return
     */
    public static long pow( long x, long y ) {
        return (long)Math.pow( x, y );
    }
    
    public static double pow( double ds1, double ds2 ) {
        return Math.pow( ds1,ds2 );
    }
    
    public static QDataSet pow( Object ds1, Object ds2 ) {
        return pow( dataset(ds1), dataset(ds2) );
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

    public static double exp( double ds1 ) {
        return Math.exp( ds1 );
    }

    public static QDataSet exp( Object ds1 ) {
        return exp( dataset(ds1) );
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

    public static double exp10( double ds1 ) {
        return Math.pow( 10, ds1 );
    }

    public static QDataSet exp10( Object ds1 ) {
        return exp10( dataset(ds1) );
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
    
    public static double log( double ds1 ) {
        return Math.log( ds1 );
    }

    public static QDataSet log( Object ds1 ) {
        return log( dataset(ds1) );
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

    public static double log10( double ds1 ) {
        return Math.log10( ds1 );
    }

    public static QDataSet log10( Object ds1 ) {
        return log10( dataset(ds1) );
    }
    
    /**
     * element-wise multiply of two datasets with compatible geometry.
     * Presently, either ds1 or ds2 should be dimensionless.
     * TODO: units improvements.
     * @param ds1
     * @param ds2
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
            logger.info("throwing out units until we improve the units library, both arguments have physical units");
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

    public static QDataSet multiply( Object ds1, Object ds2 ) {
        return multiply( dataset(ds1), dataset(ds2) );
    }
    
    /**
     * element-wise divide of two datasets with compatible geometry.  Either
     * ds1 or ds2 should be dimensionless, or the units be convertible.
     * TODO: units improvements.
     * @param ds1
     * @param ds2
     * @return a data
     */
    public static QDataSet divide(QDataSet ds1, QDataSet ds2) {
        Units units1= SemanticOps.getUnits(ds1);
        Units units2= SemanticOps.getUnits(ds2);
        Units resultUnits;
        final UnitsConverter uc;

        if ( units1==units2 ) {
            resultUnits= Units.dimensionless;
            uc= UnitsConverter.IDENTITY;
        } else if ( units2==Units.dimensionless && UnitsUtil.isRatioMeasurement(units1) ) {
            resultUnits= units1;
            uc= UnitsConverter.IDENTITY;
        } else if ( units2.isConvertableTo(units1) ) {
            resultUnits= Units.dimensionless;
            uc= units2.getConverter(units1);
        } else {
            if ( !UnitsUtil.isRatioMeasurement(units1) ) throw new IllegalArgumentException("ds1 units are not ratio units: "+units1);
            if ( !UnitsUtil.isRatioMeasurement(units2) ) throw new IllegalArgumentException("ds2 units are not ratio units: "+units2);

            if ( units1==Units.dimensionless ) {
                try {
                    resultUnits= UnitsUtil.getInverseUnit(units2);
                } catch ( IllegalArgumentException ex ) {
                    logger.info( String.format( "unable to invert unit2=%s, arguments have unequal units", units2 ) );
                    resultUnits= null;
                }
            } else {
                logger.info("throwing out units until we improve the units library, arguments have unequal units");
                resultUnits= null;
            }
            uc= UnitsConverter.IDENTITY;
        }

        MutablePropertyDataSet result= applyBinaryOp(ds1, ds2, new BinaryOp() {
            public double op(double d1, double d2) {
                return d1 / uc.convert(d2);
            }
        });

        if ( resultUnits!=Units.dimensionless ) result.putProperty( QDataSet.UNITS, resultUnits );
        result.putProperty(QDataSet.LABEL, maybeLabelInfixOp( ds1, ds2, "/" ) );
        return result;
    }
    
    public static QDataSet divide( Object ds1, Object ds2 ) {
        return divide( dataset(ds1), dataset(ds2) );
    }


    /**
     * element-wise mod of two datasets with compatible geometry.
     * TODO: I think there's a tacit assumption that the units are the same.  This should support Units.t2000 mod "24 hours" to get result in hours.
     * @param ds1
     * @param ds2
     * @return
     */
    public static QDataSet mod(QDataSet ds1, QDataSet ds2) {
        MutablePropertyDataSet result= applyBinaryOp(ds1, ds2, new BinaryOp() {
            public double op(double d1, double d2) {
                return d1 % d2;
            }
        });
        Units u= (Units) ds2.property(QDataSet.UNITS);
        if ( u!=null ) result.putProperty( QDataSet.UNITS, u );
        return result;
    }

    public static QDataSet mod( Object ds1, Object ds2 ) {
        return mod( dataset(ds1), dataset(ds2) );
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
    
    public static QDataSet div( Object ds1, Object ds2 ) {
        return div( dataset(ds1), dataset(ds2) );
    }

    
    // comparators
    /**
     * element-wise equality test.  1.0 is returned where the two datasets are
     * equal.  Fill is returned where either measurement is invalid.
     * @param ds
     * @return
     */
    public static QDataSet eq(QDataSet ds1, QDataSet ds2) {
        final UnitsConverter uc= SemanticOps.getLooseUnitsConverter( ds1, ds2 );
        return applyBinaryOp(ds1, ds2, new BinaryOp() {
           public double op(double d1, double d2) {
                return uc.convert(d1) == d2 ? 1.0 : 0.0;
           }
        });
    }

    public static QDataSet eq( Object ds1, Object ds2 ) {
        return eq( dataset(ds1), dataset(ds2) );
    }
    
    /**
     * element-wise not equal test.  1.0 is returned where elements are not equal.
     * Fill is returned where either measurement is invalid.
     * @param ds1
     * @param ds2
     * @return
     */
    public static QDataSet ne(QDataSet ds1, QDataSet ds2) {
        final UnitsConverter uc= SemanticOps.getLooseUnitsConverter( ds1, ds2 );
        return applyBinaryOp(ds1, ds2, new BinaryOp() {
            public double op(double d1, double d2) {
                return uc.convert(d1) != d2 ? 1.0 : 0.0;
            }
        });
    }

    public static QDataSet ne( Object ds1, Object ds2 ) {
        return ne( dataset(ds1), dataset(ds2) );
    }
    
    /**
     * element-wise function returns 1 where ds1&gt;ds2.
     * @param ds1
     * @param ds2
     * @return
     */
    public static QDataSet gt(QDataSet ds1, QDataSet ds2) {
        final UnitsConverter uc= SemanticOps.getLooseUnitsConverter( ds1, ds2 );
        return applyBinaryOp(ds1, ds2, new BinaryOp() {
            public double op(double d1, double d2) {
                return uc.convert(d1) > d2 ? 1.0 : 0.0;
            }
        });
    }

    public static QDataSet gt( Object ds1, Object ds2 ) {
        return gt( dataset(ds1), dataset(ds2) );
    }
    
    /**
     * element-wise function returns the greater of ds1 and ds2.
     * @param ds1
     * @param ds2
     * @return the bigger of the two, in the units of ds1.
     */
    public static QDataSet greaterOf(QDataSet ds1, QDataSet ds2) {
        final UnitsConverter uc= SemanticOps.getLooseUnitsConverter( ds2, ds1 );
        MutablePropertyDataSet mpds=  applyBinaryOp(ds1, ds2, new BinaryOp() {
            public double op(double d1, double d2) {
                return uc.convert(d2) > d1 ? d2 : d1;
            }
        });
        mpds.putProperty( QDataSet.UNITS, ds1.property(QDataSet.UNITS) );
        return mpds;
    }
    
    public static QDataSet greaterOf( Object ds1, Object ds2 ) {
        return greaterOf( dataset(ds1), dataset(ds2) );
    }
    
    /**
     * element-wise function returns the smaller of ds1 and ds2.
     * @param ds1
     * @param ds2
     * @return the smaller of the two, in the units of ds1.
     */
    public static QDataSet lesserOf(QDataSet ds1, QDataSet ds2) {
        final UnitsConverter uc= SemanticOps.getLooseUnitsConverter( ds2, ds1 );
        MutablePropertyDataSet mpds=  applyBinaryOp(ds1, ds2, new BinaryOp() {
            public double op(double d1, double d2) {
                return uc.convert(d2) < d1 ? d2 : d1;
            }
        });
        mpds.putProperty( QDataSet.UNITS, ds1.property(QDataSet.UNITS) );
        return mpds;
    }
    
    public static QDataSet lesserOf( Object ds1, Object ds2 ) {
        return lesserOf( dataset(ds1), dataset(ds2) );
    }
    
        
    /**
     * element-wise function returns 1 where ds1&gt;=ds2.
     * @param ds1
     * @param ds2
     * @return
     */
    public static QDataSet ge(QDataSet ds1, QDataSet ds2) {
        final UnitsConverter uc= SemanticOps.getLooseUnitsConverter( ds1, ds2 );
        return applyBinaryOp(ds1, ds2, new BinaryOp() {
            public double op(double d1, double d2) {
                return uc.convert(d1) >= d2 ? 1.0 : 0.0;
            }
        });
    }

    public static QDataSet ge( Object ds1, Object ds2 ) {
        return ge( dataset(ds1), dataset(ds2) );
    }
    
    /**
     * element-wise function returns 1 where ds1&lt;ds2.
     * @param ds1
     * @param ds2
     * @return
     */
    public static QDataSet lt(QDataSet ds1, QDataSet ds2) {
        final UnitsConverter uc= SemanticOps.getLooseUnitsConverter( ds1, ds2 );
        return applyBinaryOp(ds1, ds2, new BinaryOp() {
            public double op(double d1, double d2) {
                return uc.convert(d1) < d2 ? 1.0 : 0.0;
            }
        });
    }
    
    public static QDataSet lt( Object ds1, Object ds2 ) {
        return lt( dataset(ds1), dataset(ds2) );
    }
    

    /**
     * element-wise function returns 1 where ds1&lt;=ds2.
     * @param ds1
     * @param ds2
     * @return
     */
    public static QDataSet le(QDataSet ds1, QDataSet ds2) {
        final UnitsConverter uc= SemanticOps.getLooseUnitsConverter( ds1, ds2 );
        return applyBinaryOp(ds1, ds2, new BinaryOp() {
            public double op(double d1, double d2) {
                return uc.convert(d1) <= d2 ? 1.0 : 0.0;
            }
        });
    }

    public static QDataSet le( Object ds1, Object ds2 ) {
        return le( dataset(ds1), dataset(ds2) );
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
    
    public static QDataSet or( Object ds1, Object ds2 ) {
        return or( dataset(ds1), dataset(ds2) );
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
    
    public static QDataSet and( Object ds1, Object ds2 ) {
        return and( dataset(ds1), dataset(ds2) );
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

    public static QDataSet not( Object ds1 ) {
        return not( dataset(ds1) );
    }
    
    // IDL,Matlab - inspired routines
    /**
     * returns rank 1 dataset with values [0,1,2,...]
     * @param len0
     * @return
     */
    public static QDataSet indgen(int len0) {
        int size = len0;
        int[] back = new int[size];
        for (int i = 0; i < size; i++) {
            back[i] = i;
        }
        return IDataSet.wrap(back, 1, len0, 1, 1);
    }

    /**
     * returns rank 2 dataset with values increasing [ [0,1,2], [ 3,4,5] ]
     * @param len0
     * @param len1
     * @return
     */
    public static QDataSet indgen(int len0, int len1) {
        int size = len0 * len1;
        int[] back = new int[size];
        for (int i = 0; i < size; i++) {
            back[i] = i;
        }
        return IDataSet.wrap(back, 2, len0, len1, 1);
    }

    /**
     * returns rank 3 dataset with values increasing
     * @param len0
     * @param len1
     * @param len2
     * @return
     */
    public static QDataSet indgen(int len0, int len1, int len2) {
        int size = len0 * len1 * len2;
        int[] back = new int[size];
        for (int i = 0; i < size; i++) {
            back[i] = i;
        }
        return IDataSet.wrap(back, 3, len0, len1, len2);
    }

    /**
     * returns rank 1 dataset with values [0.,1.,2.,...]
     * @param len0
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
     * returns rank 2 dataset with values increasing [ [0.,1.,2.], [ 3.,4.,5.] ]
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
     * returns rank 4 dataset with values increasing
     * @param len0
     * @param len1
     * @param len2
     * @param len3 
     * @return
     */
    public static QDataSet dindgen(int len0, int len1, int len2, int len3 ) {
        int size = len0 * len1 * len2 * len3;
        double[] back = new double[size];
        for (int i = 0; i < size; i++) {
            back[i] = i;
        }
        return DDataSet.wrap( back, new int[] { len0, len1, len2, len3 } );
    }
    
    /**
     * returns rank 1 dataset with values [0.,1.,2.,...]
     * @param len0
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
     * returns rank 2 dataset with values increasing [ [0.,1.,2.], [ 3.,4.,5.] ]
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
     * returns rank 4 dataset with values increasing
     * @param len0
     * @param len1
     * @param len2
     * @param len3
     * @return
     */
    public static QDataSet findgen(int len0, int len1, int len2, int len3) {
        int size = len0 * len1 * len2 * len3;
        float[] back = new float[size];
        for (int i = 0; i < size; i++) {
            back[i] = i;
        }
        return FDataSet.wrap( back, new int[] { len0, len1, len2, len3 } );
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
        String[] ss= cadence.split(" ");
        
        Datum cad= null;
        if ( ss.length==2 ) {
            try {
                Units u= SemanticOps.lookupUnits(ss[1]); 
                cad= u.parse(ss[0]);
            } catch ( ParseException ex ) {
                // try using old code below.
            }
        }
        if ( cad==null ) {
            cad= Units.us2000.getOffsetUnits().parse(cadence);
        }
        double dcadence = cad.doubleValue(Units.us2000.getOffsetUnits());

        return taggen( base, dcadence, len0, Units.us2000 );
    }

    /**
     * return a rank 1 dataset of times.  All inputs should be rank 1 dataset (for now) or null.
     * @param years the years. (2010) Less than 100 is interpreted as 19xx.  This must be integers.
     * @param mons the months (1..12), or null.  If null, then days are day of year.  This must be integers.
     * @param days the day of month (1..28) or day of year.  This may be fractional.
     * @param hour null or the hours of the day.
     * @param minute null or the minutes of the day
     * @param second null or the seconds of the day
     * @param nano null or the nanoseconds (1e-9) of the day
     * @return
     */
    public static QDataSet toTimeDataSet( QDataSet years, QDataSet mons, QDataSet days, QDataSet hour, QDataSet minute, QDataSet second, QDataSet nano ) {
        DDataSet result= DDataSet.createRank1(years.length());
        result.putProperty( QDataSet.UNITS, Units.us2000 );
        if ( years.length()==0 ) {
            throw new IllegalArgumentException("Empty year array");
        }

        if ( years.value(0)<100 ) {
            years= add( years, DataSetUtil.asDataSet(1900) );
        }

        if ( mons==null ) {
            mons= Ops.ones( years.length() );
        }
        // handle nulls with one array, and avoid condition tests within the loop
        QDataSet zeros= Ops.zeros( years.length() );  //TODO: rewrite with zerosDataSet that doesn't allocate space
        if ( hour==null ) hour= zeros;
        if ( minute==null ) minute= zeros;
        if ( second==null ) second= zeros;
        if ( nano==null ) nano= zeros;

        for ( int i=0; i<result.length(); i++ ) {
            int year= (int)years.value(i);
            double fyear= years.value(i) - year;
            int month= (int)mons.value(i);
            double fmonth= mons.value(i) - month;
            int day= (int)days.value(i);
            double fday= days.value(i) - day;

            if ( fyear>0 ) {
                throw new IllegalArgumentException("fractional year not allowed: "+ years.value(i) );
            }
            if ( fmonth>0 ) {
                throw new IllegalArgumentException("fractional month not allowed: "+ mons.value(i) );
            }
            int jd = 367 * year - 7 * (year + (month + 9) / 12) / 4 -
                    3 * ((year + (month - 9) / 7) / 100 + 1) / 4 +
                    275 * month / 9 + day + 1721029;
            double microseconds = nano.value(i)/1e3 + second.value(i)*1e6 + hour.value(i)*3600e6 + minute.value(i)*60e6 + fday*86400000000.;
            double us2000= UnitsConverter.getConverter(Units.mj1958,Units.us2000).convert(( jd - 2436205 )) + microseconds;
            result.putValue( i, us2000 );

        }

        return result;
    }

    public static QDataSet toTimeDataSet( Object years, Object mons, Object days, Object hour, Object minute, Object second, Object nano ) {
        return toTimeDataSet( dataset(years), dataset( mons), dataset( days), dataset( hour), dataset( minute), dataset( second), dataset( nano ) );
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
    public static WritableDataSet replicate(long val, int len0) {
        int size = len0;
        long[] back = new long[size];
        for (int i = 0; i < size; i++) {
            back[i] = val;
        }
        return LDataSet.wrap( back, 1, len0, 1, 1, 1 );
    }

    /**
     * returns rank 2 dataset filled with value
     * @param val fill the dataset with this value.
     * @param len0
     * @param len1
     * @return
     */
    public static WritableDataSet replicate(long val, int len0, int len1) {
        int size = len0 * len1;
        long[] back = new long[size];
        for (int i = 0; i < size; i++) {
            back[i] = val;
        }
        return LDataSet.wrap(back, 2, len0, len1, 1, 1 );
    }

    /**
     * returns rank 3 dataset with filled with value.
     * @param val fill the dataset with this value.
     * @param len0
     * @param len1
     * @param len2
     * @return
     */
    public static WritableDataSet replicate(long val, int len0, int len1, int len2) {
        int size = len0 * len1 * len2;
        long[] back = new long[size];
        for (int i = 0; i < size; i++) {
            back[i] = val;
        }
        return LDataSet.wrap(back, 3, len0, len1, len2, 1);
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
     * concatenates the two datasets together, appending the datasets on the zeroth dimension.
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
        if ( ds1==null && ds2==null ) throw new NullPointerException("both ds1 and ds2 are null");
        if ( ds1==null && ds2==null ) throw new IllegalArgumentException( "both ds1 and ds2 are null");
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
                DDataSet t= DDataSet.createRank1(1); //TODO: better promote rank is found on add, etc.
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
    
    public static QDataSet concatenate( Object ds1, Object ds2 ) {
        return concatenate( dataset(ds1), dataset(ds2) );
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
     * return returns a rank 0 dataset of random uniform numbers from [0,1].
     */
    public static QDataSet rand() {
        return rand(new int[]{}, new Random());
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
     * return returns a rank 0 dataset of random numbers of a guassian (normal) distribution.
     */
    public static QDataSet randn() {
        return randn(new int[]{}, new Random());
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
     * returns a rank 0 dataset of random numbers of a guassian (normal) distribution.
     * System.currentTimeMillis() may be used for the seed.  Note this is unlike
     * the IDL randomn function because the seed is not modified.  (Any long parameter in Jython
     * and Java is read-only.)
     * @param seed
     * @param len0
     * @return
     */
    public static QDataSet randomn(long seed) {
        double[] back = randomnBack(seed,1);
        return DDataSet.wrap( back, 0, 1, 1, 1);
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
     * returns a rank 0 dataset of random numbers of a uniform distribution.
     * System.currentTimeMillis() may be used for the seed.  Note this is unlike
     * the IDL randomn function because the seed is not modified.  (Any long parameter in Jython
     * and Java is read-only.)
     * @param seed
     * @param len0
     * @return
     */
    public static QDataSet randomu(long seed) {
        double[] back = randomuBack(seed, 1);
        return DDataSet.wrap(back, 0, 1, 1, 1);
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
     * rank 4 dataset for demos.
     * @return
     */
    public static QDataSet ripples( int len, int len0, int len1, int len4 ) {
        FDataSet result= org.virbo.dataset.FDataSet.createRank4( len, len0, len1, len4 );
        Random r= new java.util.Random(0);
        for ( int j=0; j<len4; j++ ) {
            double d= r.nextDouble();
            for ( int i=0; i<len; i++ ) {
                double eps= 1+(i/(float)len);
                double eps2= 1+(i*5/(float)len);
                QDataSet d2= new RipplesDataSet( (len0*eps)/10., len1/10., (len1*eps2)/20., (len0*eps)/2., len1/2., (len1*eps)/10., len0, len1 );
                QubeDataSetIterator it= new QubeDataSetIterator(d2);
                while ( it.hasNext() ) {
                    it.next();
                    result.putValue( i, it.index(0),it.index(1), j, it.getValue(d2)+d );
                }
                if ( i==0 ) result.putProperty(QDataSet.FILL_VALUE,d2.property(QDataSet.FILL_VALUE));
            }
        }

        return result;
    }

    /**
     * return fake rank 1 data timeseries for testing
     * @param len
     * @return
     */
    public static QDataSet ripplesTimeSeries( int len ) {
        QDataSet rip= ripples( len,100 );
        ArrayDataSet result= ArrayDataSet.copy( DataSetOps.slice1(rip,20) );
        MutablePropertyDataSet t;
        try {
            t = (MutablePropertyDataSet) Ops.timegen("2011-10-24", String.format("%f sec", 86400. / len), len);
            t.putProperty( QDataSet.NAME, "Epoch" );
            result.putProperty(QDataSet.DEPEND_0,t);
            return result;
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }


    /**
     * return fake waveform data for testing
     * result is rank 2 bundle [len,512]
     * @param len number of 512-element waveforms.
     * @return
     */
    public static QDataSet ripplesWaveformTimeSeries( int len ) {
        MutablePropertyDataSet rip= (MutablePropertyDataSet) multiply( add( ripples( len,512 ), sin( divide( findgen(len,512), DataSetUtil.asDataSet(10.) ) ) ), DataSetUtil.asDataSet(5000.) );
        QDataSet toffset= Ops.taggen( 0., 0.1/512, 512, Units.seconds );
        ((MutablePropertyDataSet)toffset).putProperty( QDataSet.UNITS, Units.seconds );
        rip.putProperty( QDataSet.DEPEND_1, toffset );
        try {
            QDataSet ttag = timegen("2012-10-02T12:03", "0.1 s", len);
            rip.putProperty( QDataSet.DEPEND_0, ttag );
        } catch (ParseException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        return rip;
    }

    /**
     * return fake position data for testing
     * result is rank 2 bundle [len,3]
     * @param len
     * @return
     */
    public static QDataSet ripplesVectorTimeSeries( int len ) {
        QDataSet rip= ripples( len,100 );
        ArrayDataSet x= ArrayDataSet.copy( DataSetOps.slice1(rip,20) );
        ArrayDataSet y= ArrayDataSet.copy( DataSetOps.slice1(rip,30) );
        ArrayDataSet z= ArrayDataSet.copy( DataSetOps.slice1(rip,40) );
        x.putProperty( QDataSet.NAME, "X" );
        y.putProperty( QDataSet.NAME, "Y" );
        z.putProperty( QDataSet.NAME, "Z" );

        MutablePropertyDataSet result= (MutablePropertyDataSet) Ops.bundle( x, y, z );
        MutablePropertyDataSet t;
        try {
            t = (MutablePropertyDataSet) Ops.timegen("2011-10-24", String.format("%f sec", 86400. / len), len);
            t.putProperty( QDataSet.NAME, "Epoch" );
            result.putProperty(QDataSet.DEPEND_0,t);
            return result;
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
        
    }

    /**
     * return fake position data for testing
     * result is rank 2 bundle [len,27]
     * @param len
     * @return
     */
    public static QDataSet ripplesSpectrogramTimeSeries( int len ) {
        QDataSet rip= ripples( len,100 );
        ArrayDataSet result= ArrayDataSet.copy( DataSetOps.leafTrim( rip,0,27 ) );
        result.putProperty( QDataSet.NAME, "Flux" );
        MutablePropertyDataSet y= DataSetOps.makePropertiesMutable( Ops.pow( DataSetUtil.asDataSet(10), Ops.linspace(1,4,27 ) ) );
        y.putProperty( QDataSet.LABEL, "Energy" );
        y.putProperty( QDataSet.NAME, "Energy" );
        result.putProperty( QDataSet.DEPEND_1, y );

        MutablePropertyDataSet t;
        try {
            t = (MutablePropertyDataSet) Ops.timegen("2011-10-24", String.format("%f sec", 86400. / len), len);
            t.putProperty( QDataSet.NAME, "Epoch" );
            result.putProperty(QDataSet.DEPEND_0,t);
            return result;
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }

    }


    /**
     * return fake position data for testing
     * result is rank 2 bundle [len,27]
     * @param len
     * @return
     */
    public static QDataSet ripplesJoinSpectrogramTimeSeries( int len ) {

        int len3= len/3;

        try {
            QDataSet rip= ripples( len3,100 );
            ArrayDataSet result= ArrayDataSet.copy( DataSetOps.leafTrim( rip,0,27 ) );
            result.putProperty( QDataSet.NAME, "Flux" );
            MutablePropertyDataSet y= DataSetOps.makePropertiesMutable( Ops.pow( DataSetUtil.asDataSet(10), Ops.linspace(1,4,27 ) ) );
            y.putProperty( QDataSet.LABEL, "Energy" );
            y.putProperty( QDataSet.NAME, "Energy" );
            result.putProperty( QDataSet.DEPEND_1, y );

            MutablePropertyDataSet t;
            t = (MutablePropertyDataSet)Ops.timegen("2011-10-24", String.format("%f sec", 86400. / len3), len3);
            t.putProperty( QDataSet.NAME, "Epoch" );
            result.putProperty(QDataSet.DEPEND_0,t);

            JoinDataSet jds= new JoinDataSet(result);

            rip= ripples( len3,20 );
            result= ArrayDataSet.copy( DataSetOps.leafTrim( rip,0,20 ) );
            result.putProperty( QDataSet.NAME, "Flux" );
            y= DataSetOps.makePropertiesMutable( Ops.pow( DataSetUtil.asDataSet(10), Ops.linspace(3.1,8.1,20 ) ) );
            y.putProperty( QDataSet.LABEL, "Energy" );
            y.putProperty( QDataSet.NAME, "Energy" );
            result.putProperty( QDataSet.DEPEND_1, y );

            t = (MutablePropertyDataSet)Ops.timegen("2011-10-25", String.format("%f sec", 86400. / len3), len3);
            t.putProperty( QDataSet.NAME, "Epoch" );
            result.putProperty(QDataSet.DEPEND_0,t);

            jds.join(result);

            int lenr=  len-2*len3;
            rip= ripples( lenr,50 );
            result= ArrayDataSet.copy( DataSetOps.leafTrim( rip,0,24 ) );
            result.putProperty( QDataSet.NAME, "Flux" );
            y= DataSetOps.makePropertiesMutable( Ops.pow( DataSetUtil.asDataSet(10), Ops.linspace(2.1,5.1,24 ) ) );
            y.putProperty( QDataSet.LABEL, "Energy" );
            y.putProperty( QDataSet.NAME, "Energy" );
            result.putProperty( QDataSet.DEPEND_1, y );

            t = (MutablePropertyDataSet)Ops.timegen("2011-10-26", String.format("%f sec", 86400. / lenr), lenr);
            t.putProperty( QDataSet.NAME, "Epoch" );
            result.putProperty(QDataSet.DEPEND_0,t);
            jds.join(result);

            return jds;
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }

    }

    /**
     * return an example of a QDataSet containing a pitch angle distribution.  This is
     * a rank 2 dataset with angle in radians for DEPEND_0 and radius for DEPEND_1.
     * @return
     */
    public static QDataSet ripplesPitchAngleDistribution() {
        ArrayDataSet rip= ArrayDataSet.maybeCopy( ripples( 30, 15 ) );
        QDataSet angle= linspace( PI/30/2, PI-PI/30/2, 30 );
        QDataSet rad= linspace( 1, 5, 15 );
        rip.putProperty( QDataSet.DEPEND_0, angle );
        rip.putProperty( QDataSet.DEPEND_1, rad );
        rip.putProperty( QDataSet.RENDER_TYPE, "pitchAngleDistribution" );
        return rip;
    }

    /**
     * tool for creating ad-hoc events datasets.
     * @param timeRange a timerange like "2010-01-01" or "2010-01-01/2010-01-10" or "2010-01-01 through 2010-01-09"
     * @param rgbcolor and RGB color like 0xFF0000 (red), 0x00FF00 (green), or 0x0000FF (blue),
     * @param annotation label for event, possibly including granny codes.
     * @return a rank 2 QDataSet with [[ startTime, duration, rgbColor, annotation  ]]
     */

    public static QDataSet createEvent( String timeRange, int rgbcolor, String annotation ) {
        return createEvent( null, timeRange, rgbcolor, annotation );
    }
    
    /**
     * tool for creating ad-hoc events datasets.
     * @param append null or a dataset to append the result.
     * @param timeRange a timerange like "2010-01-01" or "2010-01-01/2010-01-10" or "2010-01-01 through 2010-01-09"
     * @param rgbcolor and RGB color like 0xFF0000 (red), 0x00FF00 (green), or 0x0000FF (blue),
     * @param annotation label for event, possibly including granny codes.
     * @return a rank 2 QDataSet with [[ startTime, duration, rgbColor, annotation  ]]
     */
    public static QDataSet createEvent( QDataSet append, String timeRange, int rgbcolor, String annotation ) {
        
        Units tu= Units.t2000;
 	Units dtu= Units.seconds;
        
        MutablePropertyDataSet bds=null;
        if ( append!=null ) {
            bds= (MutablePropertyDataSet) append.property( QDataSet.BUNDLE_1 );
            if ( bds==null ) throw new IllegalArgumentException("append argument must be the output of createEvent");
        }
        
        DatumRange dr= DatumRangeUtil.parseTimeRangeValid(timeRange);
        
        EnumerationUnits evu= EnumerationUnits.create("createEvent");
        
        DataSetBuilder dsb= new DataSetBuilder(2,100,4);
        
        dsb.putValue( -1, 0, dr.min().doubleValue(tu) );
        dsb.putValue( -1, 1, dr.width().doubleValue(dtu) );
        dsb.putValue( -1, 2, rgbcolor );
        dsb.putValue( -1, 3, evu.createDatum(annotation).doubleValue(evu) );
        dsb.nextRecord();
        
        MutablePropertyDataSet ds= dsb.getDataSet();
        
        if ( bds==null ) {
            bds= DDataSet.createRank2( 4, 0 );
            bds.putProperty( "NAME__0", "Time" );
            bds.putProperty( "UNITS__0", tu );
            bds.putProperty( "NAME__1", "Duration" );
            bds.putProperty( "UNITS__1", dtu );
            bds.putProperty( "NAME__2", "Color" );
            bds.putProperty( "FORMAT__2", "0x%08x" ); // format as hex
            bds.putProperty( "NAME__3", "Event" );
            bds.putProperty( "UNITS__3", evu );
        } 
            
        ds.putProperty( QDataSet.BUNDLE_1, bds );
        
        append= concatenate( append, ds );
        return append;
        
    }
    
    /**
     * return a dataset with X and Y forming a circle, introduced as a convenient way to indicate planet location.
     * @param radius rank 0 dataset
     * @return QDataSet that when plotted is a circle.
     */
    public static QDataSet circle( QDataSet radius ) {
        if ( radius==null ) radius= DataSetUtil.asDataSet(1.);
        MutablePropertyDataSet result= (MutablePropertyDataSet) Ops.link( Ops.multiply( radius, sin(linspace(0,601*PI/300,601) ) ), Ops.multiply( radius, cos(linspace(0,601*PI/300,601 ) ) ) );
        result.putProperty( QDataSet.RENDER_TYPE, "series" );
        return result;
    }

    /**
     * return a dataset with X and Y forming a circle, introduced as a convenient way to indicate planet location.
     * @param radius
     * @return QDataSet that when plotted is a circle.
     */
    public static QDataSet circle( double dradius ) {
        QDataSet radius= DataSetUtil.asDataSet(dradius);
        return circle( radius );
    }

    /**
     * return a dataset with X and Y forming a circle, introduced as a convenient way to indicate planet location.
     * @param radius string parsed into rank 0 dataset
     * @return QDataSet that when plotted is a circle.
     */
    public static QDataSet circle( String sradius ) throws ParseException {
        QDataSet radius;
        if ( sradius==null ) {
            radius= DataSetUtil.asDataSet(1.);
        } else {
            Datum d;
            try {
                d= DatumUtil.parse(sradius);
            } catch ( ParseException ex ) {
                String[] ss= sradius.split(" ", 2);
                if ( ss.length==2 ) {
                    Units u= SemanticOps.lookupUnits(ss[1]);
                    d= u.parse(ss[0]);  // double.parseDouble
                } else {
                    throw new IllegalArgumentException("unable to parse: "+sradius );
                }
            }
            radius= DataSetUtil.asDataSet( d );
        }

        return circle( radius );
    }


    /**
     * copies the properties, copying depend datasets as well.  Copied from ArrayDataSet.
     */
    private static Map copyProperties( QDataSet ds ) {
        Map result = new HashMap();
        Map srcProps= DataSetUtil.getProperties(ds);

        result.putAll(srcProps);

        for ( int i=0; i < ds.rank(); i++) {
            QDataSet dep = (QDataSet) ds.property("DEPEND_" + i);
            if (dep == ds) {
                throw new IllegalArgumentException("dataset is dependent on itsself!");
            }
            if (dep != null) {
                result.put("DEPEND_" + i, copy(dep) ); // for timetags
            }
        }

        for (int i = 0; i < QDataSet.MAX_PLANE_COUNT; i++) {
            QDataSet plane0 = (QDataSet) ds.property("PLANE_" + i);
            if (plane0 != null) {
                result.put("PLANE_" + i, copy(plane0));
            } else {
                break;
            }
        }

        return result;
    }

    /**
     * copy the dataset to make a new one that is writable.  When a join dataset is copied, a WritableJoinDataSet is used
     * to copy each dataset.  This is a deep copy, so for example DEPEND_0 is copied as well.
     * @param src
     * @return a copy of src.
     */
    public static WritableDataSet copy( QDataSet src ) {
        logger.log(Level.FINE, "copy({0})", src);
        if ( SemanticOps.isJoin(src) ) {
            WritableJoinDataSet result= new WritableJoinDataSet( src.rank() );
            for ( int i=0; i<src.length(); i++ ) {
                result.join( ArrayDataSet.copy(src.slice(i)) );
            }
            Map<String,Object> props= copyProperties(src);
            for ( Entry<String,Object> en: props.entrySet() ) {
                result.putProperty( en.getKey(), en.getValue() );
            }
            return result;
        } else {
            return ArrayDataSet.copy(src);
        }
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

    public static double sin( double ds ) {
        return Math.sin( ds );
    }
    
    public static QDataSet sin( Object ds ) {
        return sin( dataset(ds) );
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
    
    public static double asin( double ds ) {
        return Math.asin( ds );
    }
    
    public static QDataSet asin( Object ds ) {
        return asin( dataset(ds) );
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
        result.putProperty(QDataSet.LABEL, maybeLabelUnaryOp(result, "cos" ) );
        return result;
    }

    public static double cos( double ds ) {
        return Math.cos( ds );
    }
    
    public static QDataSet cos( Object ds ) {
        return cos( dataset(ds) );
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
    
    public static double acos( double ds ) {
        return Math.acos( ds );
    }
    
    public static QDataSet acos( Object ds ) {
        return acos( dataset(ds) );
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
    
    public static double tan( double ds ) {
        return Math.tan( ds );
    }
    
    public static QDataSet tan( Object ds ) {
        return tan( dataset(ds) );
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

    public static double atan( double ds ) {
        return Math.atan( ds );
    }
    
    public static QDataSet atan( Object ds ) {
        return atan( dataset(ds) );
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
    
    public static double atan2( double y, double x ) {
        return Math.atan2( y, x );
    }

    public static QDataSet atan2( Object dsy, Object dsx ) {
        return atan2( dataset(dsy), dataset(dsx) );
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

    public static double cosh( double ds ) {
        return Math.cosh( ds );
    }
    
    public static QDataSet cosh( Object ds ) {
        return cosh( dataset(ds) );
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
    
    public static double sinh( double ds ) {
        return Math.sinh( ds );
    }
    
    public static QDataSet sinh( Object ds ) {
        return sinh( dataset(ds) );
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

    public static double tanh( double ds ) {
        return Math.tanh( ds );
    }
    
    public static QDataSet tanh( Object ds ) {
        return tanh( dataset(ds) );
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
    
    public static double expm1( double ds ) {
        return Math.expm1( ds );
    }
        
    public static QDataSet expm1( Object ds ) {
        return expm1( dataset(ds) );
    }

    /** scalar math functions http://sourceforge.net/p/autoplot/bugs/1052/ */
    
    /**
     * coerce Java objects like arrays Lists and scalars into a QDataSet.  
     * This is introduced to mirror the useful Jython dataset command.
     * This supports:
     *   int, float, double, etc to Rank 0 datasets
     *   List&lt;Number&gt; to Rank 1 datasets.
     *   Java arrays of Number to Rank 1-4 qubes datasets
     *   Strings to rank 0 datasets with units ("5 s" "2014-01-01T00:00")
     *   Datums to rank 0 datasets
     *   DatumRanges to rank 1 bins
     * 
     * @param arg0 null,QDataSet,Number,Datum,DatumRange,String,List,or array.
     * @return QDataSet
     */
    public static QDataSet dataset( Object arg0 ) {
        if ( arg0==null ) {  // there was a similar test in the Python code.
            return null;
        } else if ( arg0 instanceof QDataSet ) {
            return (QDataSet)arg0;
        } else if ( arg0 instanceof Number ) {
            return DataSetUtil.asDataSet( ((Number)arg0).doubleValue() );
        } else if ( arg0 instanceof Datum ) {
            return DataSetUtil.asDataSet( (Datum)arg0 );
        } else if ( arg0 instanceof DatumRange ) {
            return DataSetUtil.asDataSet( (DatumRange)arg0 );
        } else if ( arg0 instanceof String ) {
            try {
               return DataSetUtil.asDataSet( DatumUtil.parse(arg0.toString()) ); //TODO: someone is going to want lookupUnits that will allocate new units.
            } catch (ParseException ex) {
               throw new IllegalArgumentException( "unable to parse string: "+arg0 );
            }
        } else if ( arg0 instanceof List ) {
            List p= (List)arg0;
            double[] j= new double[ p.size() ];
            for ( int i=0; i<p.size(); i++ ) {
                Object n= p.get(i);
                //TODO: consider enumerations for Strings.
                j[i]= ((Number)n).doubleValue();
            }
            QDataSet q= DDataSet.wrap( j );
            return q;            
        } else if ( arg0.getClass().isArray() ) { // convert Java array into QDataSet.  Assumes qube.
            //return DataSetUtil.asDataSet(arg0); // I think this is probably a better implementation.
            List<Integer> qqube= new ArrayList( );
            qqube.add( Array.getLength(arg0) );
            Object slice= Array.get(arg0, 0);
            while ( slice.getClass().isArray() ) {
                qqube.add( Array.getLength(slice) );
                slice= Array.get( slice, 0 );
            }
            int[] qube= new int[qqube.size()];
            for ( int i=0; i<qube.length; i++ ) qube[i]= qqube.get(i);
            return ArrayDataSet.wrap( arg0, qube, true );
        } else {
            throw new IllegalArgumentException("Ops.dataset is unable to coerce "+arg0+" to QDataSet");
        }
        
    }
    
    /**
     * convert the data to radians by multiplying each element by PI/180.
     * This does not check the units of the data, but a future version might.
     * @param ds
     * @return 
     */
    public static QDataSet toRadians(QDataSet ds) {
        return applyUnaryOp(ds, new UnaryOp() {
            public double op(double y) {
                return y * Math.PI / 180.;
            }
        });
    }

    public static QDataSet toRadians( Object ds ) {
        return toRadians( dataset(ds) );
    }
    
    
    /**
     * convert the data to degrees by multiplying each element by 180/PI.
     * This does not check the units of the data, but a future version might.
     * @param ds
     * @return 
     */
    public static QDataSet toDegrees(QDataSet ds) {
        return applyUnaryOp(ds, new UnaryOp() {
            public double op(double y) {
                return y * 180 / Math.PI;
            }
        });
    }
    
    public static QDataSet toDegrees( Object ds ) {
        return toDegrees( dataset(ds) );
    }
    
    /**
     * return the index of the maximum value.  This is to avoid inefficient 
     * code like "where(slice.eq( max(slice) ))[0]"
     * @param ds rank 1 dataset
     * @return the index of the maximum value, or -1 if the data is all fill.
     */
    public static int imax( QDataSet ds ) {
        if ( ds.rank()!=1 ) throw new IllegalArgumentException("rank 1 only");
        QDataSet wds= DataSetUtil.weightsDataSet(ds);
        int result= -1;
        double v= Double.MIN_VALUE;
        for ( int i=0; i<ds.length(); i++ ) {
            if ( wds.value(i)>0 ) {
                double d= ds.value(i);
                if ( d>v ) {
                    result= i;
                    v= d;
                }
            }
        }
        return result;
    }
    
    public static int imax( Object ds ) {
        return imax( dataset(ds) );
    }
    
    /**
     * return the index of the minimum value.  This is to avoid inefficient 
     * code like "where(slice.eq( min(slice) ))[0]"
     * @param ds rank 1 dataset
     * @return the index of the maximum value, or -1 if the data is all fill.
     */
    public static int imin( QDataSet ds ) {
        if ( ds.rank()!=1 ) throw new IllegalArgumentException("rank 1 only");
        QDataSet wds= DataSetUtil.weightsDataSet(ds);
        int result= -1;
        double v= Double.MAX_VALUE;
        for ( int i=0; i<ds.length(); i++ ) {
            if ( wds.value(i)>0 ) {
                double d= ds.value(i);
                if ( d<v ) {
                    result= i;
                    v= d;
                }
            }
        }
        return result;
    }

    public static int imin( Object ds ) {
        return imin( dataset(ds) );
    }
    
    
    /**
     * returns a dataset containing the indeces of where the dataset is non-zero.
     * For a rank 1 dataset, returns a rank 1 dataset with indeces for the values.
     * For a higher rank dataset, returns a rank 2 qube dataset with ds.rank()
     * elements in the first dimension.  Note when the dataset is all zeros (false),
     * the result is a zero-length array, as opposed to IDL which would return
     * a -1 scalar.
     * 
     * Note fill values are not included in the list, so where(A).length + where(not A).length != where( A.or(not(A) ).length
     *
     * @param ds of any rank M
     * @return a rank 1 or rank 2 dataset with N by M elements, where N is the number
     * of non-zero elements found.
     */
    public static QDataSet where(QDataSet ds) {
        DataSetBuilder builder;

        QubeDataSetIterator iter = new QubeDataSetIterator(ds);
        QDataSet wds= DataSetUtil.weightsDataSet(ds);

        if (ds.rank() == 1) {
            builder = new DataSetBuilder(1, 100, 1, 1);
            while (iter.hasNext()) {
                iter.next();
                if ( iter.getValue(wds)> 0 && iter.getValue(ds) != 0.) {
                    builder.putValue(-1, iter.index(0));
                    builder.nextRecord();
                }
            }
            builder.putProperty(QDataSet.MONOTONIC, Boolean.TRUE);
        } else {
            builder = new DataSetBuilder(2, 100, ds.rank(), 1);
            while (iter.hasNext()) {
                iter.next();
                if ( iter.getValue(wds)> 0 && iter.getValue(ds) != 0.) {
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
        builder.putProperty( QDataSet.FORMAT, "%d" );

        return builder.getDataSet();
    }

    public static QDataSet where( Object ds ) {
        return where( dataset(ds) );
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
    
    public static QDataSet sort(Object ds) {
        return DataSetOps.sort(dataset(ds));
    }

    /**
     * return the unique elements from the dataset.  If sort is null, then
     * the dataset is assumed to be monotonic, and only repeating values are
     * coalesced.  If sort is non-null, then it is the result of the function
     * "sort" and should be a rank 1 list of indeces that sort the data.
     *
     * renamed uniqValues from uniq to avoid confusion with the IDL command.
     *
     * TODO: should this return the values or the indeces?  This needs example
     * code and should not be used for now.  See VirboAutoplot/src/scripts/test/testUniq.jy
     * 
     * @param ds
     * @param sort
     * @return
     */
    public static QDataSet uniqValues( QDataSet ds, QDataSet sort  ) {
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
    
    public static QDataSet reverse( Object ds ) {
        return new ReverseDataSet(dataset(ds));
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
    
    public static QDataSet shuffle(Object ds) {
        return shuffle(dataset(ds));
    }

    
    /**
     * @see ds.slice
     * @param ds
     * @param idx
     * @return
     */
    public static QDataSet slice0( QDataSet ds, int idx ) {
        return ds.slice(idx);
    }
    
    /**
     * @see DataSetOps.slice1
     * @param ds
     * @param idx
     * @return
     */
    public static QDataSet slice1( QDataSet ds, int idx ) {
        return DataSetOps.slice1(ds, idx);
    }

    /**
     * @see DataSetOps.slice2
     * @param ds
     * @param idx
     * @return
     */
    public static QDataSet slice2( QDataSet ds, int idx ) {
        return DataSetOps.slice2(ds, idx);
    }

    /**
     * @see DataSetOps.slice3
     * @param ds
     * @param idx
     * @return
     */
    public static QDataSet slice3( QDataSet ds, int idx ) {
        return DataSetOps.slice3(ds, idx);
    }

    /**
     * Enumeration identifying windows applied to data before doing FFTs.
     *@see #fftFilter
     */
    public static enum FFTFilterType{ Hanning, TenPercentEdgeCosine, Unity };

    /**
     * Apply windows to the data to prepare for FFT.  The data is reformed into a rank 2 dataset [N,len].
     * The filter is applied to the data to remove noise caused by the discontinuity.
     * @param ds rank 1, 2, or 3 data
     * @param len
     * @param filt FFTFilterType.Hanning or FFTFilterType.TenPercentEdgeCosine
     * @return data[N,len] with the window applied.
     */
    public static QDataSet fftFilter( QDataSet ds, int len, FFTFilterType filt ) {
        ProgressMonitor mon=null;

        if ( mon==null ) {
            mon= new NullProgressMonitor();
        }

        if ( ds.rank()==1 ) { // wrap to make rank 2
            QDataSet c= (QDataSet) ds.property( QDataSet.CONTEXT_0 );
            QDataSet dep0ds= (QDataSet) ds.property( QDataSet.DEPEND_0 );

            if ( c!=null ) {
                Units cunits= SemanticOps.getUnits(c);
                if ( dep0ds!=null ) {
                    Units dep0units= SemanticOps.getUnits(dep0ds);
                    if ( !cunits.getOffsetUnits().isConvertableTo(dep0units.getOffsetUnits()) ) {
                        c= null;
                    }
                }
            }
            
            if ( c==null && dep0ds!=null ) {
                c= dep0ds.slice(0);
            }

            JoinDataSet dep0;
            Units dep0u;
            JoinDataSet jds= new JoinDataSet(ds);
            if ( c!=null && c.rank()==0 ) {
                dep0u= (Units) c.property(QDataSet.UNITS);
                dep0= new JoinDataSet(c);
                if ( dep0u!=null ) {
                    dep0.putProperty( QDataSet.UNITS, dep0u );
                    jds.putProperty( QDataSet.DEPEND_0, dep0 );
                    jds.putProperty( QDataSet.DEPEND_1, Ops.subtract( dep0ds, c ) );
                }
            }

            ds= jds;
        }

        if ( ds.rank()==3 ) { // slice it and do the process to each branch.
            JoinDataSet result= new JoinDataSet(3);
            mon.setTaskSize( ds.length()*10  );
            mon.started();
            for ( int i=0; i<ds.length(); i++ ) {
                mon.setTaskProgress(i*10);
                QDataSet pow1= fftFilter( ds.slice(i), len, filt );
                result.join(pow1);
            }
            mon.finished();
            return result;

        } else if ( ds.rank()==2 ) {
            JoinDataSet result= new JoinDataSet(2);
            result.putProperty(QDataSet.JOIN_0, null);

            int nsam= ds.length()*(ds.length(0)/len); // approx
            DataSetBuilder dep0b= new DataSetBuilder(1,nsam );

            QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
            QDataSet dep1= (QDataSet) ds.property( QDataSet.DEPEND_1 );

            if ( dep0==null ) dep0= findgen( ds.length() );
            if ( dep1==null ) dep1= findgen( ds.length(0) );

            UnitsConverter uc;
            Units dep0u= SemanticOps.getUnits(dep0);
            Units dep1u= SemanticOps.getUnits(dep1);
            uc= dep1u.getConverter( dep0u.getOffsetUnits() );

            QDataSet filter;
            if ( filt==FFTFilterType.Hanning ) {
                filter= FFTUtil.getWindowHanning(len);
            } else if ( filt==FFTFilterType.TenPercentEdgeCosine ) {
                filter= FFTUtil.getWindow10PercentEdgeCosine(len);
            } else if ( filt==FFTFilterType.Unity ) {
                filter= FFTUtil.getWindowUnity(len);
            } else {
                throw new UnsupportedOperationException("unsupported op: "+filt );
            }

            boolean convertToFloat= ds instanceof FDataSet;

            mon.setTaskSize(ds.length());
            mon.started();
            mon.setProgressMessage("performing fftFilter");
            for ( int i=0; i<ds.length(); i++ ) {
                for ( int j=0; j<ds.length(i)/len; j++ ) {

                    QDataSet wave= ds.slice(i).trim(j*len,(j+1)*len );

                    QDataSet vds= Ops.multiply( wave, filter );
                    if ( convertToFloat ) {
                        vds= ArrayDataSet.copy( float.class, vds );
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
            if (dep1!=null ) {
                result.putProperty(QDataSet.DEPEND_1, dep1.trim(0,len) );
            }

            return result;

        } else {
            throw new IllegalArgumentException("rank not supported: "+ ds.rank() );
        }

    }

    /**
     * Apply Hanning windows to the data to prepare for FFT.  The data is reformed into a rank 2 dataset [N,len].
     * Hanning windows taper the ends of the interval to remove noise caused by the discontinuity.
     * This is deprecated, and windowFunction should be used.
     * @param ds, rank 1, 2, or 3 data
     * @param len
     * @return data[N,len] with the hanning window applied.
     */
    public static QDataSet hanning( QDataSet ds, int len ) {
        return fftFilter(  ds, len, FFTFilterType.Hanning );
    }

    /**
     * create a power spectrum on the dataset by breaking it up and
     * doing ffts on each segment.
     *
     * data may be rank 1, rank 2, or rank 3.
     *
     * Looks for DEPEND_1.USER_PROPERTIES.FFT_Translation, which should
     * be a rank 0 or rank 1 QDataSet.  If it is rank 1, then it should correspond
     * to the DEPEND_0 dimension.
     *
     * @param ds rank 2 dataset ds(N,M) with M>len
     * @param len the number of elements to have in each fft.
     * @param mon a ProgressMonitor for the process
     * @return rank 2 fft spectrum
     */
    public static QDataSet fftPower( QDataSet ds, int len, ProgressMonitor mon ) {
        QDataSet unity= ones(len);
        return fftPower( ds, unity, 1, mon );
    }

    /**
     * return a dataset for the given filter type.  The result will be rank 1 and length len.
     */
    public static QDataSet windowFunction( FFTFilterType filt, int len ) {
        if ( filt==FFTFilterType.Hanning ) {
            return FFTUtil.getWindowHanning(len);
        } else if ( filt==FFTFilterType.TenPercentEdgeCosine ) {
            return FFTUtil.getWindow10PercentEdgeCosine(len);
        } else if ( filt==FFTFilterType.Unity ) {
            return FFTUtil.getWindowUnity(len);
        } else {
            throw new UnsupportedOperationException("unsupported op: "+filt );
        }
    }

    public static QDataSet fftPower( QDataSet ds, QDataSet window, ProgressMonitor mon ) {
        return fftPower( ds, window, 1, mon );
    }
    /**
     * create a power spectrum on the dataset by breaking it up and
     * doing ffts on each segment.
     *
     * data may be rank 1, rank 2, or rank 3.
     *
     * Looks for DEPEND_1.USER_PROPERTIES.FFT_Translation, which should
     * be a rank 0 or rank 1 QDataSet.  If it is rank 1, then it should correspond
     * to the DEPEND_0 dimension.
     *
     * No normalization is done with non-unity windows.
     *
     * @param ds rank 2 dataset ds(N,M) with M>len
     * @param window window to apply to the data before performing FFT (Hann,Unity,etc.)
     * @param step step size, expressed as a fraction of the length (1 for no slide, 2 for half steps, 4 for quarters);
     * @param mon a ProgressMonitor for the process
     * @return rank 2 fft spectrum
     */
    public static QDataSet fftPower( QDataSet ds, QDataSet window, int stepFraction, ProgressMonitor mon ) {
        if ( mon==null ) {
            mon= new NullProgressMonitor();
        }

        String title= (String) ds.property(QDataSet.TITLE);
        if ( title!=null ) {
            title= "FFTPower of "+title;
        }
        
        if ( ds.rank()==1 ) { // wrap to make rank 2
            QDataSet c= (QDataSet) ds.property( QDataSet.CONTEXT_0 );
            JoinDataSet dep0;
            Units dep0u;
            JoinDataSet jds= new JoinDataSet(ds);
            if ( c!=null && c.rank()==0 ) {
                dep0u= (Units) c.property(QDataSet.UNITS);
                dep0= new JoinDataSet(c);
                if ( dep0u!=null ) {
                    dep0.putProperty( QDataSet.UNITS, dep0u );
                    jds.putProperty( QDataSet.DEPEND_0, dep0 );
                }
            }

            ds= jds;
        } 

        if ( ds.rank()==3 ) { // slice it and do the process to each branch.
            JoinDataSet result= new JoinDataSet(3);
            mon.setTaskSize( ds.length()*10  );
            mon.started();
            for ( int i=0; i<ds.length(); i++ ) {
                mon.setTaskProgress(i*10);
                QDataSet pow1= fftPower( ds.slice(i), window, stepFraction, SubTaskMonitor.create( mon, i*10, (i+1)*10 ) );
                result.join(pow1);
            }
            mon.finished();

            result.putProperty( QDataSet.QUBE, Boolean.TRUE );
            if ( title!=null ) result.putProperty( QDataSet.TITLE, title );
            
            return result;

        } else if ( ds.rank()==2 ) {
            JoinDataSet result= new JoinDataSet(2);
            result.putProperty(QDataSet.JOIN_0, null);

            int len= window.length();
            int step;
            if ( stepFraction < 0 ) {
                throw new IllegalArgumentException( String.format( "fractional step size (%d) is negative.", stepFraction ) );
            } else if ( stepFraction <= 32 ) { 
                step= len/stepFraction;
            } else {
                throw new IllegalArgumentException( String.format( "fractional step size (%d) is bigger than 32, the max allowed.", stepFraction ) );
            }
            boolean windowNonUnity= false; // true if a non-unity window is to be applied.
            for ( int i=0; windowNonUnity==false && i<len; i++ ) {
                if ( window.value(i)!=1.0 ) windowNonUnity=true;
            }

            double normalization; // the normalization needed because of the window.

            if ( windowNonUnity ) {
                normalization= total( Ops.pow( window, 2 ) ) / window.length();
            } else {
                normalization= 1.0;
            }            
            
            int nsam= ds.length()*(ds.length(0)/step); // approx
            DataSetBuilder dep0b= new DataSetBuilder( 1,nsam );

            QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
            if ( dep0!=null ) { // make sure these are really units we can use
                Units u0= SemanticOps.getUnits(dep0);
                if ( UnitsUtil.isNominalMeasurement(u0) ) dep0= null; // nope, we can't use it.
            }

            QDataSet dep1= (QDataSet) ds.property( QDataSet.DEPEND_1 );
            if ( dep1==null ) {
                dep1= (QDataSet)ds.slice(0).property(QDataSet.DEPEND_0);
            }
            
            assert dep1!=null;

            UnitsConverter uc= UnitsConverter.IDENTITY;

            QDataSet translation= null;
            Map<String,Object> user= (Map<String, Object>) dep1.property(QDataSet.USER_PROPERTIES );
            if ( user!=null ) {
                translation= (QDataSet) user.get( "FFT_Translation" ); // kludge for Plasma Wave Group
                if ( translation.rank()==1 ) {
                    if ( translation.length()!=dep0.length() ) {
                        throw new IllegalArgumentException("rank 1 FFT_Translation should be the same length as depend_0");
                    }
                }
            }
            if ( translation!=null ) logger.fine("translation will be applied");

            QDataSet powxtags= FFTUtil.getFrequencyDomainTagsForPower(dep1.trim(0,len));
            
            double minD= Double.NEGATIVE_INFINITY, maxD=Double.POSITIVE_INFINITY;
            if ( dep1.rank()==1 ) {
                QDataSet ytags= powxtags;
                if ( translation==null ) result.putProperty( QDataSet.DEPEND_1, ytags );
                Units dep1Units= (Units) dep1.property(QDataSet.UNITS);
                Units dep0Units= dep0==null ? null : (Units) dep0.property(QDataSet.UNITS);
                if ( dep0Units!=null && dep1Units!=null ) uc= dep1Units.getConverter(dep0Units.getOffsetUnits());
                if ( dep0!=null && dep0.property(QDataSet.VALID_MIN)!=null ) minD= ((Number)dep0.property(QDataSet.VALID_MIN)).doubleValue();
                if ( dep0!=null && dep0.property(QDataSet.VALID_MAX)!=null ) maxD= ((Number)dep0.property(QDataSet.VALID_MAX)).doubleValue();
            }

            int len1= ( ( ds.length(0)-len ) / step ) + 1;

            mon.setTaskSize(ds.length()*len1); // assumes all are the same length.
            mon.started();
            mon.setProgressMessage("performing fftPower");

            boolean isMono= dep0==null ? true : DataSetUtil.isMonotonic(dep0);

            for ( int i=0; i<ds.length(); i++ ) {
                QDataSet slicei= ds.slice(i); //TODO: for DDataSet, this copies the backing array.  This shouldn't happen in DDataSet.slice, but it does...
                QDataSet dep0i= (QDataSet) slicei.property(QDataSet.DEPEND_0);
                if ( dep0i!=null && dep0==null ) {
                    dep0b.putProperty(QDataSet.UNITS, dep0i.property(QDataSet.UNITS) );
                    if ( !Boolean.TRUE.equals( dep0i.property(QDataSet.MONOTONIC) ) ) {
                        isMono= false;
                    }
                    if ( dep0i.property(QDataSet.VALID_MIN)!=null ) minD= ((Number)dep0i.property(QDataSet.VALID_MIN)).doubleValue(); else minD= Double.NEGATIVE_INFINITY;
                    if ( dep0i.property(QDataSet.VALID_MAX)!=null ) maxD= ((Number)dep0i.property(QDataSet.VALID_MAX)).doubleValue(); else maxD= Double.POSITIVE_INFINITY;
                }
                
                for ( int j=0; j<len1; j++ ) {
                    GeneralFFT fft = GeneralFFT.newDoubleFFT(len);
                    QDataSet wave= slicei.trim( j*step,j*step+len );
                    QDataSet weig= DataSetUtil.weightsDataSet(wave);
                    boolean hasFill= false;
                    for ( int k=0; k<weig.length(); k++ ) {
                        if ( weig.value(k)==0 ) {
                            hasFill= true;
                        }
                    }
                    if ( hasFill ) continue;

                    if ( windowNonUnity ) {
                        wave= Ops.multiply(wave,window); 
                    }

                    
                    QDataSet vds= FFTUtil.fftPower( fft, wave, window, powxtags );
                    //QDataSet vds= FFTUtil.fftPower( fft, wave );

                    if ( windowNonUnity ) {
                        vds= Ops.multiply( vds, DataSetUtil.asDataSet( 1/normalization ) );
                    }

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

                    double d0=0;
                    if ( dep0!=null ) {
                        d0= dep0.value(i) + uc.convert( dep1.value( j*step + len/2 )  );
                    } else if ( dep0!=null ) {
                        d0= dep0.value(i);
                    } else if ( dep0i!=null ) {
                        d0= dep0i.value(j*step+len/2);
                    } else {
                        dep0b= null;
                    }


                    if ( d0>=minD && d0<=maxD) {
                        result.join(vds);
                        if ( dep0b!=null ) {
                            dep0b.putValue(-1, d0 );
                            dep0b.nextRecord();
                        }
                    } else {
                        System.err.println("dropping record with invalid timetag: "+d0 ); //TODO: consider setting VALID_MIN, VALID_MAX instead...
                    }

                    mon.setTaskProgress(i*len1+j);

                }
                
            }
            mon.finished();
            if ( dep0!=null && dep0b!=null ) {
                dep0b.putProperty(QDataSet.UNITS, dep0.property(QDataSet.UNITS) );
                if ( isMono ) dep0b.putProperty(QDataSet.MONOTONIC,true);
                result.putProperty(QDataSet.DEPEND_0, dep0b.getDataSet() );
            } else if ( dep0b!=null ) {
                if ( isMono ) dep0b.putProperty(QDataSet.MONOTONIC,true);
                result.putProperty(QDataSet.DEPEND_0, dep0b.getDataSet() );
            }

            if ( title!=null ) result.putProperty( QDataSet.TITLE, title );
            result.putProperty( QDataSet.QUBE, Boolean.TRUE );
            
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
        ComplexArray.Double ca = FFTUtil.fft( fft, ds );

        QDataSet dep0 = (QDataSet) ds.property(QDataSet.DEPEND_0);
        RankZeroDataSet cadence = dep0 == null ? DRank0DataSet.create(1.0) : DataSetUtil.guessCadenceNew(dep0,null);
        if ( cadence==null ) throw new IllegalArgumentException("can't establish data cadence");

        double[] xtags = FFTUtil.getFrequencyDomainTags( 1./cadence.value(), ds.length());
        double binsize=  2 * xtags[ xtags.length/2 ] / ds.length();
        Units invUnits= null;
        try {
            invUnits= UnitsUtil.getInverseUnit( SemanticOps.getUnits(cadence) );
        } catch ( IllegalArgumentException ex ) {
            // do nothing.
        }

        DDataSet result = DDataSet.createRank1(ds.length()/2);
        DDataSet resultDep0 = DDataSet.createRank1(ds.length()/2);
        for (int i = 0; i < ds.length()/2; i++) {
            result.putValue(i, (i==0?1:4)*ComplexArray.magnitude2(ca,i) / binsize );
            resultDep0.putValue( i, xtags[i] );
        }
        if ( invUnits!=null ) resultDep0.putProperty( QDataSet.UNITS, invUnits );

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
     * perform ffts on the waveform as we do with fftPower, but keep real and
     * imaginary components.
     * @param ds the waveform rank 1,2,or 3 dataset.
     * @param window the window function, like ones(1024) or windowFunction( FFTFilterType.Hanning, 1024 ).  This is used to infer window size.
     * @param stepFraction step this fraction of the window size.  1 is no overlap, 2 is 50% overlap, 4 is 75% overlap, etc.
     * @param mon progress monitor.
     * @return result[ntime,nwindow,2]
     */
    public static QDataSet fft( QDataSet ds, QDataSet window, int stepFraction, ProgressMonitor mon ) {

        String title= (String) ds.property(QDataSet.TITLE);
        if ( title!=null ) {
            title= "FFT of "+title;
        }
        
        if ( ds.rank()<1 || ds.rank()>3 ) {
            throw new IllegalArgumentException("rank exception, expected rank 1,2 or 3: got "+ds );
            
        } else if ( ds.rank()==1 ) { // wrap to make rank 2
            QDataSet c= (QDataSet) ds.property( QDataSet.CONTEXT_0 );
            JoinDataSet dep0;
            Units dep0u;
            JoinDataSet jds= new JoinDataSet(ds);
            if ( c!=null && c.rank()==0 ) {
                dep0u= (Units) c.property(QDataSet.UNITS);
                dep0= new JoinDataSet(c);
                if ( dep0u!=null ) {
                    dep0.putProperty( QDataSet.UNITS, dep0u );
                    jds.putProperty( QDataSet.DEPEND_0, dep0 );
                }
            }
            ds= jds;
            
        } else if ( ds.rank()==3 ) { // slice it and do the process to each branch.
            JoinDataSet result= new JoinDataSet(3);
            mon.setTaskSize( ds.length()*10  );
            mon.started();
            for ( int i=0; i<ds.length(); i++ ) {
                mon.setTaskProgress(i*10);
                QDataSet pow1= fft( ds.slice(i), window, stepFraction, SubTaskMonitor.create( mon, i*10, (i+1)*10 ) );
                result.join(pow1);
            }
            mon.finished();

            result.putProperty( QDataSet.QUBE, Boolean.TRUE );
            if ( title!=null ) result.putProperty( QDataSet.TITLE, title );
            
            return result;

        }
        
        int len= window.length();
        int step;
        if ( stepFraction < 0 ) {
            throw new IllegalArgumentException( String.format( "fractional step size (%d) is negative.", stepFraction ) );
        } else if ( stepFraction <= 32 ) { 
            step= len/stepFraction;
        } else {
            throw new IllegalArgumentException( String.format( "fractional step size (%d) is bigger than 32, the max allowed.", stepFraction ) );
        }
        boolean windowNonUnity= false; // true if a non-unity window is to be applied.
        for ( int i=0; windowNonUnity==false && i<len; i++ ) {
            if ( window.value(i)!=1.0 ) windowNonUnity=true;
        }

        double normalization; // the normalization needed because of the window.

        if ( windowNonUnity ) {
            normalization= total( Ops.pow( window, 2 ) ) / window.length();
        } else {
            normalization= 1.0;
        }
        
        JoinDataSet result= new JoinDataSet(3);
        result.putProperty(QDataSet.JOIN_0, null);

        int nsam= ds.length()*(ds.length(0)/step); // approx
        DataSetBuilder dep0b= new DataSetBuilder( 1,nsam );

        QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
        if ( dep0!=null ) { // make sure these are really units we can use
            Units u0= SemanticOps.getUnits(dep0);
            if ( UnitsUtil.isNominalMeasurement(u0) ) dep0= null; // nope, we can't use it.
        }

        UnitsConverter uc= UnitsConverter.IDENTITY;

        QDataSet dep1= (QDataSet) ds.property( QDataSet.DEPEND_1 );
        if ( dep1==null ) {
            dep1= (QDataSet)ds.slice(0).property(QDataSet.DEPEND_0);
        }
        
        double minD= Double.NEGATIVE_INFINITY, maxD=Double.POSITIVE_INFINITY;
        if ( dep1!=null && dep1.rank()==1 ) {
            QDataSet ytags= FFTUtil.getFrequencyDomainTags( dep1.trim(0,len) );
            //NOTE translation is not implemented for this mode.
            result.putProperty( QDataSet.DEPEND_1, ytags );
            Units dep1Units= (Units) dep1.property(QDataSet.UNITS);
            Units dep0Units= dep0==null ? null : (Units) dep0.property(QDataSet.UNITS);
            if ( dep0Units!=null && dep1Units!=null ) uc= dep1Units.getConverter(dep0Units.getOffsetUnits());
            if ( dep0!=null && dep0.property(QDataSet.VALID_MIN)!=null ) minD= ((Number)dep0.property(QDataSet.VALID_MIN)).doubleValue();
            if ( dep0!=null && dep0.property(QDataSet.VALID_MAX)!=null ) maxD= ((Number)dep0.property(QDataSet.VALID_MAX)).doubleValue();
        }

        int len1= ( ( ds.length(0)-len ) / step ) + 1;

        mon.setTaskSize(ds.length()*len1); // assumes all are the same length.
        mon.started();
        mon.setProgressMessage("performing fft");

        boolean isMono= dep0==null ? true : DataSetUtil.isMonotonic(dep0);

        for ( int i=0; i<ds.length(); i++ ) {
            QDataSet slicei= ds.slice(i); //TODO: for DDataSet, this copies the backing array.  This shouldn't happen in DDataSet.slice, but it does...
            QDataSet dep0i= (QDataSet) slicei.property(QDataSet.DEPEND_0);
            if ( dep0i!=null && dep0==null ) {
                dep0b.putProperty(QDataSet.UNITS, dep0i.property(QDataSet.UNITS) );
                if ( !Boolean.TRUE.equals( dep0i.property(QDataSet.MONOTONIC) ) ) {
                    isMono= false;
                }
                if ( dep0i.property(QDataSet.VALID_MIN)!=null ) minD= ((Number)dep0i.property(QDataSet.VALID_MIN)).doubleValue(); else minD= Double.NEGATIVE_INFINITY;
                if ( dep0i.property(QDataSet.VALID_MAX)!=null ) maxD= ((Number)dep0i.property(QDataSet.VALID_MAX)).doubleValue(); else maxD= Double.POSITIVE_INFINITY;
            }

            for ( int j=0; j<len1; j++ ) {
                GeneralFFT fft = GeneralFFT.newDoubleFFT(len);
                QDataSet wave= slicei.trim( j*step,j*step+len );
                QDataSet weig= DataSetUtil.weightsDataSet(wave);
                boolean hasFill= false;
                for ( int k=0; k<weig.length(); k++ ) {
                    if ( weig.value(k)==0 ) {
                        hasFill= true;
                    }
                }
                if ( hasFill ) continue;

                QDataSet vds= FFTUtil.fft( fft, wave, windowNonUnity ? weig : null );

                if ( windowNonUnity ) {
                    vds= Ops.multiply( vds, DataSetUtil.asDataSet( 1/normalization ) );
                }

                double d0=0;
                if ( dep0!=null && dep1!=null ) {
                    d0= dep0.value(i) + uc.convert( dep1.value( j*step + len/2 )  );
                } else if ( dep0!=null ) {
                    d0= dep0.value(i);
                } else if ( dep0i!=null ) {
                    d0= dep0i.value(j*step+len/2);
                } else {
                    dep0b= null;
                }

                if ( d0>=minD && d0<=maxD) {
                    result.join(vds);
                    if ( dep0b!=null ) {
                        dep0b.putValue(-1, d0 );
                        dep0b.nextRecord();
                    }
                } else {
                    System.err.println("dropping record with invalid timetag: "+d0 ); //TODO: consider setting VALID_MIN, VALID_MAX instead...
                }

                mon.setTaskProgress(i*len1+j);

            }

        }
        mon.finished();
        
        if ( dep0!=null && dep0b!=null ) {
            dep0b.putProperty(QDataSet.UNITS, dep0.property(QDataSet.UNITS) );
            if ( isMono ) dep0b.putProperty(QDataSet.MONOTONIC,true);
            result.putProperty(QDataSet.DEPEND_0, dep0b.getDataSet() );
        } else if ( dep0b!=null ) {
            if ( isMono ) dep0b.putProperty(QDataSet.MONOTONIC,true);
            result.putProperty(QDataSet.DEPEND_0, dep0b.getDataSet() );
        }

        if ( title!=null ) result.putProperty( QDataSet.TITLE, title );
        result.putProperty( QDataSet.QUBE, Boolean.TRUE );

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
     * Note this accounts for BIN_PLUS, BIN_MINUS properties.
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
     * returns a two element, rank 1 dataset containing the extent (min to max) of the data.
     * Note this accounts for DELTA_PLUS, DELTA_MINUS properties.  
     * Note this accounts for BIN_PLUS, BIN_MINUS properties.
     * If no valid data is found then [fill,fill] is returned.
     * The property QDataSet.SCALE_TYPE is set to lin or log.
     * The property count is set to the number of valid measurements.
     * 2010-10-14: add branch for monotonic datasets.
     * @param ds the dataset to measure the extent
     * @param range, if non-null, return the union of this range and the extent.  This must not contain fill!
     * @return two element, rank 1 "bins" dataset.
     */
    public static QDataSet extent( QDataSet ds, QDataSet range  ) {
        QDataSet wds = DataSetUtil.weightsDataSet(ds);
        return extent( ds, wds, range );
    }

    /**
     * returns a two element, rank 1 dataset containing the extent (min to max) of the data, allowing an external
     * evaluation of the weightsDataSet.  If no valid data is found then [fill,fill] is returned.
     * @param ds the dataset to measure the extent rank 1 or rank 2 bins
     * @param wds a weights dataset, containing zero where the data is not valid, positive non-zero otherwise.  If null, then all finite data is treated as valid.
     * @param range, if non-null, return the union of this range and the extent.  This must not contain fill!
     * @return two element, rank 1 "bins" dataset.
     */
    public static QDataSet extent( QDataSet ds, QDataSet wds, QDataSet range ) {

        QDataSet max = ds;
        QDataSet min = ds;
        QDataSet deltaplus;
        QDataSet deltaminus;

        deltaplus = (QDataSet) ds.property(QDataSet.DELTA_PLUS);
        deltaminus = (QDataSet) ds.property(QDataSet.DELTA_MINUS);
        if ( ds.property(QDataSet.BIN_PLUS )!=null ) deltaplus= (QDataSet)ds.property(QDataSet.BIN_PLUS );
        if ( ds.property(QDataSet.BIN_MINUS )!=null ) deltaminus= (QDataSet)ds.property(QDataSet.BIN_MINUS );

        if ( ds.rank()==2 && SemanticOps.isBins(ds) ) {
            min= Ops.slice1(ds,0);
            max= Ops.slice1(ds,1);
            ds= min;
            wds= Ops.slice1(wds,0);
        }
        
        int count=0;

        if ( wds==null ) {
            wds= new WeightsDataSet.Finite(ds);
        }

        double [] result;
        Number dfill= ((Number)wds.property(QDataSet.FILL_VALUE));
        double fill= dfill!=null ? dfill.doubleValue() : -1e31;

        if ( range==null ) {
            result= new double[]{ Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        } else {
            result= new double[]{ range.value(0), range.value(1) };
            if ( range.value(0)==fill ) System.err.println("range passed into extent contained fill");
        }

        // find the first and last valid points.
        int ifirst=0;
        int n= ds.length();
        int ilast= n-1;
        
        boolean monoCheck= Boolean.TRUE.equals( ds.property(QDataSet.MONOTONIC ));
        if ( ds.rank()==1 && monoCheck ) {
            while ( ifirst<n && wds.value(ifirst)==0.0 ) ifirst++;
            while ( ilast>=0 && wds.value(ilast)==0.0 ) ilast--;
            int imiddle= ( ifirst + ilast ) / 2;
            if ( wds.value(imiddle)>0 ) {
                double dir= ds.value(ilast) - ds.value(ifirst) ;
                if ( ( ds.value(imiddle) - ds.value(ifirst) ) * dir < 0 ) {
                    logger.fine("this data isn't really monotonic.");
                    monoCheck= false;
                }
            }
        }
        
        if ( ds.rank()==1 && monoCheck && deltaplus==null ) {
            count= Math.max( 0, ilast - ifirst + 1 );
            if ( count>0 ) {
                result[0]= Math.min( result[0], min.value(ifirst) );
                result[1]= Math.max( result[1], max.value(ilast) );
            } else {
                result[0] = range==null ? fill : range.value(0);
                result[1] = range==null ? fill : range.value(1);
            }
            if ( result[0]>result[1] ) { // okay with fill.
                double t= result[1];
                result[1]= result[0];
                result[0]= t;
            }
        } else {

            if (deltaplus != null) {
                max = Ops.add(max, deltaplus );
            }

            if (deltaminus != null) {
                min = Ops.subtract(min, deltaminus);
            }

            QubeDataSetIterator it = new QubeDataSetIterator(ds);

            while (it.hasNext()) {
                it.next();
                if (it.getValue(wds) > 0.) {
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
        qresult.putProperty( QDataSet.USER_PROPERTIES, Collections.singletonMap( "count", count ) );
        qresult.putProperty( QDataSet.BINS_0, "min,maxInclusive" );
        qresult.putProperty( QDataSet.UNITS, ds.property(QDataSet.UNITS ) );
        if ( result[0]==fill ) qresult.putProperty( QDataSet.FILL_VALUE, fill);
        
        return qresult;
        
    }

    /**
     * returns rank 1 QDataSet range relative to range "dr", where 0. is the minimum, and 1. is the maximum.
     * For example rescaleRange(ds,1,2) is scanNext, rescaleRange(ds,0.5,1.5) is zoomOut.  This is similar
     * to the DatumRange rescale functions.
     * @param dr a QDataSet with bins and with nonzero width.
     * @param min the new min normalized with respect to this range.  0. is this range's min, 1 is this range's max, -1 is
     * min-width.
     * @param max the new max normalized with respect to this range.  0. is this range's min, 1 is this range's max, -1 is
     * min-width.
     * @return new rank 1 QDataSet range.
     */
    public static QDataSet rescaleRange( QDataSet dr, double min, double max ) {
        if ( dr.rank()!=1 ) {
            throw new IllegalArgumentException("Rank must be 1");
        }
        double w= dr.value(1) - dr.value(0);
        if ( Double.isInfinite(w) || Double.isNaN(w) ) {
            throw new RuntimeException("width is not finite");
        }
        if ( w==0. ) {
            // condition that might cause an infinate loop!  For now let's check for this and throw RuntimeException.
            throw new RuntimeException("width is zero!");
        }
        DDataSet result= DDataSet.createRank1(2);
        result.putValue( 0, dr.value(0) + w*min );
        result.putValue( 1, dr.value(0) + w*max );

        DataSetUtil.copyDimensionProperties( dr, result );
        return result;
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
     * returns outerSum of two rank 1 datasets, a rank 2 dataset with
     * elements R[i,j]= ds1[i] + ds2[j].
     *
     * @param ds1 a rank 1 dataset of length m
     * @param ds2 a rank 1 dataset of length n
     * @return a rank 2 dataset[m,n]
     */
    public static QDataSet outerSum(QDataSet ds1, QDataSet ds2) {
        DDataSet result = DDataSet.createRank2(ds1.length(), ds2.length());
        Map<String,Object> props= new HashMap();
        BinaryOp b= addGen( ds1, ds2, props );
        for (int i = 0; i < ds1.length(); i++) {
            for (int j = 0; j < ds2.length(); j++) {
                result.putValue(i, j, b.op( ds1.value(i), ds2.value(j) ) );
            }
        }
        result.putProperty( QDataSet.UNITS, props.get(QDataSet.UNITS ) );
        return result;
    }

    /**
     * element-wise floor function.
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

    public static double ceil( double x ) {
        return Math.ceil(x);
    }
    
    public static QDataSet ceil( Object x ) {
        return ceil( dataset(x) );
    }



    /**
     * for Jython, we handle this because the double isn't coerced.
     * @param x
     * @return
     */
    public static double round( double x ) {
        return Math.round( x );
    }

    /**
     * element-wise round function.  0.5 is round up.
     * @param ds1
     * @return
     */
    public static QDataSet round(QDataSet ds1) {
        return applyUnaryOp(ds1, new UnaryOp() {
            public double op(double a) {
                return Math.round(a);
            }
        });
    }

    /**
     * Returns the signum function of the argument; zero if the argument is 
     * zero, 1.0 if the argument is greater than zero, -1.0 if the argument 
     * is less than zero.
     * @param ds1
     * @see copysign
     * @see negate
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
     * @see negate
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
    
    public static double copysign( double x, double y ) {
        return Math.abs(x)*Math.signum(y);
    }
    
    public static QDataSet copysign( Object x, Object y ) {
        return multiply( abs(dataset(x)), signum( dataset(y) ) );
    }
    
    
    
    /**
     * returns the "floating point index" of each element of vv within the monotonically
     * increasing dataset uu.  This handy number is the index of the lower bound plus the
     * fractional position between the two bounds.  For example, findex([100,110,120],111.2) is
     * 1.12 because it is just after the 1st element (110) and is 12% of the way from 110 to 120.
     * The result dataset will have the same geometry as vv.  The result will be negative
     * when the element of vv is below the smallest element of uu.  The result will be greater
     * than or equal to the length of uu minus one when it is greater than all elements.
     * When the monotonic dataset contains repeat values, the index of the first is returned.
     *
     * Paul Ricchiazzi wrote this routine first for IDL as a fast replacement for the interpol routine, but
     * it is useful in other situations as well.
     *
     * @param uu rank 1 monotonically increasing dataset, containing no fill values.
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
        Units vvunits= SemanticOps.getUnits( vv );
        Units uuunits= SemanticOps.getUnits( uu );

        UnitsConverter uc= UnitsConverter.getConverter( vvunits, uuunits );

        while (it.hasNext()) {
            it.next();
            double d = uc.convert( it.getValue(vv) ); //TODO: assumes no fill data.
            // TODO: optimize by only doing binary search below or above ic0&ic1.
            if (uc0 <= d && d <= uc1) { // optimize by seeing if old pair still backets the current point.
                double ff = d==uc0 ? 0 : (d - uc0) / (uc1 - uc0); // may be 1.0
                it.putValue(result, ic0 + ff);
            } else {
                int index = DataSetUtil.binarySearch(uu, d, 0, uu.length() - 1);
                if (index == -1) {
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
                double ff = d==uc0 ? 0 : (d - uc0) / (uc1 - uc0); // may be 1.0

                it.putValue(result, ic0 + ff);
            }
        }
        return result;
    }
    
    public static QDataSet findex( Object x, Object y ) {
        return findex( dataset(x), dataset(y) );
    }
    
    /**
     * interpolate values from rank 1 dataset vv using fractional indeces 
     * in rank N findex.  For example, findex=1.5 means interpolate
     * the 1st and 2nd indeces with equal weight, 1.1 means
     * 90% of the first mixed with 10% of the second.  No extrapolation is
     * done, data with findex&lt;0 or findex&gt;(vv.length()-1) are assigned the
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
        if ( vv.rank()!=1 ) {
            throw new IllegalArgumentException("vv is not rank1");
        }
        
        DDataSet result = DDataSet.create(DataSetUtil.qubeDims(findex));
        QubeDataSetIterator it = new QubeDataSetIterator(findex);
        int ic0, ic1;
        int n = vv.length();

        QDataSet wds= DataSetUtil.weightsDataSet( vv );
        Double fill= (Double)wds.property(QDataSet.FILL_VALUE);
        if ( fill==null ) fill= -1e38;
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
        DataSetUtil.copyDimensionProperties( vv, result );

        return result;
    }

    public static QDataSet interpolate( Object x, Object y ) {
        return interpolate( dataset(x), dataset(y) );
    }    
    
    /**
     * interpolate values from rank 2 dataset vv using fractional indeces
     * in rank N findex, using bilinear interpolation.  See also interpolateGrid.
     *
     * @see findex the 1-D findex command.
     * @param vv rank 2 dataset.
     * @param findex0 rank N dataset of fractional indeces for the zeroth index.
     * @param findex1 rank N dataset of fractional indeces for the first index.
     * @return rank N dataset 
     */
    public static QDataSet interpolate(QDataSet vv, QDataSet findex0, QDataSet findex1) {

        if ( findex0.rank()>0 && findex0.length()!=findex1.length() ) {
            throw new IllegalArgumentException("findex0 and findex1 must have the same geometry.");
        }
        DDataSet result = DDataSet.create(DataSetUtil.qubeDims(findex0));

        QDataSet wds= DataSetUtil.weightsDataSet(vv);

        QubeDataSetIterator it = new QubeDataSetIterator(findex0);
        int ic00, ic01, ic10, ic11;
        int n0 = vv.length();
        int n1 = vv.length(0);

        double fill= -1e38;
        result.putProperty( QDataSet.FILL_VALUE, fill );
        
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

            double ww00=  wds.value(ic00, ic10);
            double ww01 = wds.value(ic00, ic11);
            double ww10 = wds.value(ic01, ic10);
            double ww11 = wds.value(ic01, ic11);

            if ( ww00*ww01*ww10*ww11 > 0 ) {
                double beta0= 1-alpha0;
                double beta1= 1-alpha1;
                double value= vv00 * beta0 * beta1 + vv01 * beta0 * alpha1 + vv10 * alpha0 * beta1 + vv11 * alpha0 * alpha1;
                it.putValue(result, value);
            } else {
                it.putValue(result, fill );
            }

        }

        return result;
    }
        
    public static QDataSet interpolate( Object x, Object y, Object z ) {
        return interpolate( dataset(x), dataset(y), dataset(z) );
    }    

    /**
     * interpolate values from rank 2 dataset vv using fractional indeces
     * in rank N findex, using bilinear interpolation.  Here the two rank1
     * indexes form a grid and the result is rank 2.
     *
     * @see findex the 1-D findex command.
     * @param vv rank 2 dataset.
     * @param findex0 rank 1 dataset of fractional indeces for the zeroth index.
     * @param findex1 rank 1 dataset of fractional indeces for the first index.
     * @return rank 2 dataset 
     */
    public static QDataSet interpolateGrid(QDataSet vv, QDataSet findex0, QDataSet findex1) {

        boolean slice0= false;
        if ( findex0.rank()==0 ) {
            slice0= true;
            findex0= new JoinDataSet(findex0);
        }
        
        boolean slice1= false;
        if ( findex1.rank()==0 ) {
            slice1= true;
            findex1= new JoinDataSet(findex1);
        }

        DDataSet result = DDataSet.createRank2(findex0.length(),findex1.length());

        QDataSet wds= DataSetUtil.weightsDataSet(vv);


        int ic00, ic01, ic10, ic11;
        int n0 = vv.length();
        int n1 = vv.length(0);

        double fill= -1e38;
        result.putProperty( QDataSet.FILL_VALUE, fill );
        
        QubeDataSetIterator it = new QubeDataSetIterator(findex0);
        
        while (it.hasNext()) {
            it.next();
            QubeDataSetIterator it2= new QubeDataSetIterator(findex1);
            
            while ( it2.hasNext() ) {
                it2.next();
                double ff0 = it.getValue(findex0);

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

                double ff1 = it2.getValue(findex1);

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

                double vv00 =  vv.value(ic00, ic10);
                double vv01 =  vv.value(ic00, ic11);
                double vv10 =  vv.value(ic01, ic10);
                double vv11 =  vv.value(ic01, ic11);

                double ww00=  wds.value(ic00, ic10);
                double ww01 = wds.value(ic00, ic11);
                double ww10 = wds.value(ic01, ic10);
                double ww11 = wds.value(ic01, ic11);

                if ( ww00*ww01*ww10*ww11 > 0 ) {
                    double beta0= 1-alpha0;
                    double beta1= 1-alpha1;
                    double value= vv00 * beta0 * beta1 + vv01 * beta0 * alpha1 + vv10 * alpha0 * beta1 + vv11 * alpha0 * alpha1;
                    result.putValue( it.index(0),it2.index(0),value );
                } else {
                    result.putValue( it.index(0),it2.index(0),fill );
                }
            } // second index
        }

        QDataSet result1= result;
        if ( slice0 ) {
            result1= result1.slice(0);
        }
        if ( slice1 ) {
            result1= DataSetOps.slice1(result1,0);
        }
        return result1;
    }

    public static QDataSet interpolateGrid( Object x, Object y, Object z ) {
        return interpolateGrid( dataset(x), dataset(y), dataset(z) );
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
    
    public static QDataSet smooth(Object ds, int size) {
        return smooth( dataset(ds), size );
    }
    
    /**
     * run boxcar average over the dataset, returning a dataset of same geometry.  
     * Points near the edge are fit to a line and replaced.  The result dataset 
     * contains a property "weights" that is the weights for each point.  
     *
     * @param xx a rank 1 dataset of size N
     * @param yy a rank 1 dataset of size N
     * @param size the number of adjacent bins to average.  If size is greater than yy.length, size is reset to yy.length.
     * @return rank 1 dataset of size N
     */
    public static QDataSet smoothFit( QDataSet xx, QDataSet yy, int size) {

        if ( size>yy.length() ) size=yy.length();
        
        if ( xx==null ) {
            xx= findgen(yy.length());
        }
        DDataSet yysmooth= (DDataSet) ArrayDataSet.maybeCopy( double.class, smooth(yy,size));
        int n= xx.length();
        
        yysmooth.putProperty( QDataSet.DEPEND_0, xx );
        LinFit fit;
        
        fit= new LinFit( xx.trim(0,size), yy.trim(0,size) );
        for ( int i=0; i<size/2; i++ ) {
            yysmooth.putValue( i, xx.value(i)*fit.getB() + fit.getA() );
        }

        fit= new LinFit( xx.trim(n-size,n), yy.trim(n-size,n) );
        for ( int i=n-(size+1)/2; i<n; i++ ){
            yysmooth.putValue( i, xx.value(i)*fit.getB() + fit.getA() );
        }

        return yysmooth;
    }
    
    public static QDataSet smoothFit( Object xx, Object yy, int size) {
        return smoothFit( dataset(xx), dataset(yy), size );
    }
    
    /**
     * contour the data in rank 2 table tds at rank 0 vv.  The result
     * is a rank 2 bundle of [:,'x,y,z'] where i is the contour number.
     * The result will have DEPEND_0 be an monotonically increasing sequence with
     * jumps indicating new contours.
     * @param tds rank 2 table
     * @param vv rank 2 bundle
     * @return
     */
    public static QDataSet contour( QDataSet tds, QDataSet vv ) {
        QDataSet vds = Contour.contour(tds, vv );
        return vds;
    }

    public static QDataSet contour( Object tds, Object vv ) {
        QDataSet vds = Contour.contour( dataset(tds), dataset(vv) );
        return vds;
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

    public static QDataSet diff( Object ds ) {
        return diff( dataset(ds) );
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

    public static QDataSet accum( Object accumDs, Object ds ) {
        return accum( dataset(accumDs), dataset(ds) );
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

    public static QDataSet accum( Object ds ) {
        return accum( null, dataset(ds) );
    }
    
    /**
     * append two datasets that are QUBEs.  DEPEND_0 and other metadata is
     * handled as well.  So for example:
     *    ds1= findgen(10)
     *    ds2= findgen(12)
     *    print append(ds1,ds2)  ; dataSet[22] (dimensionless)
     * @param ds1 null or rank N dataset
     * @param ds2 rank N dataset with compatible geometry.
     * @return 
     */
    public static QDataSet append( QDataSet ds1, QDataSet ds2 ) {
        if ( ds1==null ) {
            return ds2;
        } else {
            // use append to combine the two datasets.  Note append copies the data.
            return ArrayDataSet.append( ArrayDataSet.maybeCopy(ds1), ArrayDataSet.maybeCopy(ds2) );
        }
    }
            
    /**
     * convert the dataset to the target units
     * @param ds the original dataset.
     * @param u units of the new dataset
     * @return a new dataset with all the same properties but with the new units.
     * @throws InconvertibleUnitsException
     */
    public static QDataSet convertUnitsTo( QDataSet ds, Units u ) {
        UnitsConverter uc= Units.getConverter( SemanticOps.getUnits(ds), u );
        ArrayDataSet ds2= ArrayDataSet.copy(ds);
        DataSetIterator iter= new QubeDataSetIterator( ds2 );
        while ( iter.hasNext() ) {
            iter.next();
            iter.putValue( ds2, uc.convert( iter.getValue(ds) ) );
        }
        ds2.putProperty( QDataSet.UNITS, u );
        return ds2;
    }

    /**
     * flatten a rank N dataset, though currently only rank 2 is supported.
     * The result for rank 2 is an n,3 dataset of [x,y,z], or if there are no tags, just [z].
     * The last index will be the dependent variable, and the first indeces will
     * be the independent variables sorted by dimension.
     * @see DataSetOps.flattenRank2
     * @see DataSetOps.grid
     * @param ds
     * @return
     */
    public static QDataSet flatten( QDataSet ds ) {
        if ( ds.rank()==0 ) {
            DDataSet result= DDataSet.createRank1(1);
            result.putValue(0,ds.value());
            DataSetUtil.copyDimensionProperties( ds, result );
            return result;
        }  else if(ds.rank() == 1) {
            return ds;
        } else if ( ds.rank()==2 ) {
            return DataSetOps.flattenRank2(ds);
        } else {
            throw new UnsupportedOperationException("only rank 0,1,and 2 supported");
        }
    }
    /**
     * create a labels dataset for tagging rows of a dataset.  If the context
     * has been used already, including "default", then the EnumerationUnit
     * for the data will be preserved.  labels(["red","green","blue"],"default")
     * will always return an equivalent (and comparable) result during a session.
     *
     * Example:
     * <tt>dep1= labels( ["X","Y","Z"], "GSM" )</tt>
     * @param labels
     * @param context the namespace for the labels, to provide control over String->int mapping.
     * @return rank 1 QDataSet
     */
    public static QDataSet labels(String[] labels, String context) {
        EnumerationUnits u;
        try {
            Units uu= Units.getByName(context);
            if ( uu!=null && uu instanceof EnumerationUnits ) {
                u= (EnumerationUnits)uu;
            } else {
                u = new EnumerationUnits(context);
            }
        } catch ( IllegalArgumentException ex ) {
            u = new EnumerationUnits(context);
        }
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
     * <tt>dep1= labels( ["red","greed","blue"] )</tt>
     * @param labels
     * @return rank 1 QDataSet
     */
    public static QDataSet labels(String[] labels) {
        return labels(labels, "default");
    }

    /**
     * TODO: I suspect this is not up to spec.  See DataSetOps.sliceProperties
     * See reform, the only function that uses this.
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
     * slice each dimension in one call, so that chaining isn't required to slice multiple dimensions at once.
     * @param ds
     * @param args varargs list of integers that are slice indeces, or "" or ":" to mean don't slice
     * @return
     */
    public static QDataSet slices( QDataSet ds, Object ... args ) {
        int cdim=0; // to keep track of if we can use native slice
        int sdim=0; // to keep track of number of slices offset.
        QDataSet result= ds;
        String docStr= "";
        for ( int i=0; i<args.length; i++ ) {
            if ( args[i] instanceof Integer ) {
                int sliceIdx= ((Integer)args[i]).intValue();
                if ( cdim==i ) {
                    result= result.slice(sliceIdx);
                    cdim++;
                } else {
                    switch ( i-sdim ) {
                        case 1:
                            result= DataSetOps.slice1( result, sliceIdx );
                            break;
                        case 2:
                            result= DataSetOps.slice2( result, sliceIdx );
                            break;
                        case 3:
                            result= DataSetOps.slice3( result, sliceIdx );
                            break;
                        default:
                            throw new IllegalArgumentException("slice not implemented, too many slices follow non-slice");
                    }
                }
                docStr+= String.valueOf(sliceIdx);
                sdim++;
            } else {
                if ( args[i] instanceof String ) {
                    String s= (String)args[i];
                    if ( s.contains("=") ) {
                        throw new IllegalArgumentException("argument not supported in this version: "+s );
                    }
                    docStr+=s;
                }
            }
            if ( i<args.length-1 ) docStr+=",";
        }

        //((MutablePropertyDataSet)result).putProperty( QDataSet.CONTEXT_0, "slices("+docStr+")");
        //((MutablePropertyDataSet)result).putProperty( "CONTEXT_1", null ); // not sure who is writing this.

        return result;
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

    /**
     * change the dimensionality of the elements of the QUBE dataset.  For example,
     *   convert [1,2,3,4,5,6] to [[1,2],[3,4],[5,6]].
     * @param ds
     * @param qube the dimensions of the result dataset.
     * @return a new dataset with the specified dimensions, and the properties (e.g. UNITS) of the input dataset.
     */
    public static QDataSet reform(QDataSet ds, int[] qube) {
        QubeDataSetIterator it0 = new QubeDataSetIterator(ds);
        DDataSet result = DDataSet.create(qube);
        QubeDataSetIterator it1 = new QubeDataSetIterator(result);
        while (it0.hasNext() && it1.hasNext() ) {
            it0.next();
            it1.next();
            double v = it0.getValue(ds);
            it1.putValue(result, v);
        }
        if ( it0.hasNext() != it1.hasNext()  ) {
            throw new IllegalArgumentException("reform fails because different number of elements: "+it0+ " -> " + it1);
        }
        DataSetUtil.copyDimensionProperties( ds, result );
        return result;
    }
    
    public static QDataSet reform( Object ds, int[] qube) {
        return reform( dataset(ds),qube );
    }

    /**
     * bundle the two datasets, adding if necessary a bundle dimension.  This
     * will try to bundle on the second dimension, unlike join.  This will also
     * isolate the semantics of bundle dimensions as it's introduced.
     * @param ds1
     * @param ds2
     * @return
     */
    public static QDataSet bundle( QDataSet ds1, QDataSet ds2 ) {
        if ( ds1==null && ds2==null ) throw new NullPointerException("both ds1 and ds2 are null");
        if ( ds1==null && ds2!=null ) {
            BundleDataSet ds;
            if ( ds2.rank()==0 ) {
                ds= BundleDataSet.createRank0Bundle();
            } else {
                ds = new BundleDataSet( ds2.rank()+1 );
            }
            ds.bundle(ds2);
            QDataSet dep0= (QDataSet) ds2.property(QDataSet.DEPEND_0);
            if ( dep0!=null ) ds.putProperty( QDataSet.DEPEND_0, dep0 );
            return ds;
        } else if (ds1.rank() == ds2.rank()) {
            BundleDataSet ds= new BundleDataSet( );
            ds.bundle(ds1);
            ds.bundle(ds2);
            return ds;
        } else if ( ds1 instanceof BundleDataSet && ds1.rank()-1==ds2.rank() ) {
            ((BundleDataSet)ds1).bundle(ds2);
            return ds1;
        } else if ( ds1.rank()-1==ds2.rank() ) {
            BundleDataSet bds= new BundleDataSet(ds1.rank());
            for ( int i=0; i<ds1.length(0); i++ ) {
                bds.bundle( DataSetOps.unbundle(ds1,i) );
            }
            bds.bundle( ds2 );
            return bds;
        } else {
            throw new IllegalArgumentException("not supported yet");
        }

    }

    /**
     * shorthand for bundling three datasets.  This bundles on the second dimension,
     * unlike join.  This is just like bundle(ds1,ds2), in fact this just calls 
     * bundle( bundle( ds1,ds2 ), ds3 )
     * @param ds1 rank 1 (for now) dataset or rank 2 bundle dataset
     * @param ds2 rank 1 (for now) dataset
     * @param ds3 rank 1 (for now) dataset
     * @return rank 2 [n,3] bundle dataset
     */
    public static QDataSet bundle( QDataSet ds1, QDataSet ds2, QDataSet ds3 ) {
        return bundle( bundle( ds1, ds2 ), ds3 );
    }


    /**
     * shorthand for bundling four datasets.  This bundles on the second dimension,
     * unlike join.  This is just like bundle(ds1,ds2), in fact this just calls
     * bundle( bundle( bundle( ds1,ds2 ), ds3 ), ds4 )
     * @param ds1 rank 1 (for now) dataset or rank 2 bundle dataset
     * @param ds2 rank 1 (for now) dataset
     * @param ds3 rank 1 (for now) dataset
     * @param ds4 rank 1 (for now) dataset
     * @return rank 2 [n,4] bundle dataset
     */
    public static QDataSet bundle( QDataSet ds1, QDataSet ds2, QDataSet ds3, QDataSet ds4 ) {
        return bundle( bundle( bundle( ds1, ds2 ), ds3 ), ds4 );
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
        if ( zds.rank()==2 ) {
            QDataSet dep1= (QDataSet) zds.property(QDataSet.DEPEND_1);
            if ( dep1!=null ) {
                Units u= (Units) dep1.property(QDataSet.UNITS);
                if ( u instanceof EnumerationUnits ) {
                    return true;
                }
            }
        } else if ( zds.rank()==1 ) {
            Object o= zds.property(QDataSet.DEPEND_0);
            if ( o!=null && !( o instanceof QDataSet ) ) {
                throw new IllegalArgumentException("Somehow a string got into DEPEND_0 property.");
            }
            QDataSet dep0= (QDataSet) zds.property(QDataSet.DEPEND_0);
            if ( dep0!=null ) {
                Units u= (Units) dep0.property(QDataSet.UNITS);
                if ( u instanceof EnumerationUnits ) {
                    return true;
                }
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
        } else if ( zds.rank()>=2 ) {
            return zds.property(QDataSet.BUNDLE_1)!=null;
        } else {
            return false;
        }
        
    }

    /**
     * link is the fundamental operator where we declare that one
     * dataset is dependent on another.  For example link(x,y) creates
     * a new dataset where y is the dependent variable of the independent
     * variable x.  link is like the plot command, but doesn't plot.  For example
     *   plot(X,Y) shows a plot of Y(X),
     *   link(X,Y) returns the dataset Y(X).
     *
     * @param x rank 1 dataset
     * @param y rank 1 or rank 2 bundle dataset
     * @return rank 1 dataset with DEPEND_0 set to x.
     */
    public static QDataSet link( QDataSet x, QDataSet y ) {
        if (y.rank() == 1) {
            ArrayDataSet yds= ArrayDataSet.copy(y);
            yds.putProperty(QDataSet.DEPEND_0, x);
            List<String> problems= new ArrayList();
            if ( !DataSetUtil.validate(yds, problems ) ) {
                throw new IllegalArgumentException( problems.get(0) );
            } else {
                return yds;
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
     * link is the fundamental operator where we declare that one
     * dataset is dependent on another.  For example link(x,y,z) creates
     * a new dataset where z is the dependent variable of the independent
     * variables x and y.  link is like the plot command, but doesn't plot.  For example
     *   plot(x,y,z) shows a plot of Z(X,Y),
     *   link(x,y,z) returns the dataset Z(X,Y).
     * Note if z is a rank 1 dataset, then a bundle dataset Nx3 is returned, and names are assigned to the datasets
     *
     * @param x rank 1 dataset
     * @param y rank 1 dataset
     * @param z rank 1 or 2 dataset.
     * @return rank 1 or 2 dataset with DEPEND_0 and DEPEND_1 set to X and Y.
     */
    public static QDataSet link( QDataSet x, QDataSet y, QDataSet z ) {
        if (z.rank() == 1) {
            String xname= (String) x.property(QDataSet.NAME);
            String yname= (String) y.property(QDataSet.NAME);
            String zname= (String) z.property(QDataSet.NAME);
            if ( xname==null ) xname="data0";
            if ( yname==null ) yname="data1";
            if ( zname==null ) zname="data2";
            QDataSet result= bundle( bundle( x, y ), z );
            BundleDataSet.BundleDescriptor bds= (BundleDescriptor) result.property(QDataSet.BUNDLE_1);
            bds.putProperty( "CONTEXT_0", 2, xname+","+yname ); // note this is a string, not a QDataSet.  This is sloppy, but probably okay for now.
            bds.putProperty( QDataSet.NAME, 0, xname );
            bds.putProperty( QDataSet.NAME, 1, yname );
            bds.putProperty( QDataSet.NAME, 2, zname );

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
     * like bundle, but declare the last dataset is dependent on the first three.
     *
     * @param d0 rank 1 dataset
     * @param d1 rank 1 dataset
     * @param d2 rank 1 dataset
     * @param z rank 1 or rank 3 dataset.
     * @return rank 2 bundle when z is rank 1, or a rank 3 dataset when z is rank 3.
     */
    public static QDataSet link( QDataSet d0, QDataSet d1, QDataSet d2, QDataSet z ) {
        if (z.rank() == 1) {
            String a1name= (String) d0.property(QDataSet.NAME);
            String a2name= (String) d1.property(QDataSet.NAME);
            String a3name= (String) d2.property(QDataSet.NAME);
            String a4name= (String) z.property(QDataSet.NAME);
            if ( a1name==null ) a1name="data0";
            if ( a2name==null ) a2name="data1";
            if ( a3name==null ) a3name="data2";
            if ( a4name==null ) a4name="data3";

            QDataSet result= bundle( bundle( bundle( d0, d1 ), d2), z );
            BundleDataSet.BundleDescriptor bds= (BundleDescriptor) result.property(QDataSet.BUNDLE_1);
            bds.putProperty( QDataSet.NAME, 0, a1name );
            bds.putProperty( QDataSet.NAME, 1, a2name );
            bds.putProperty( QDataSet.NAME, 2, a3name );
            bds.putProperty( QDataSet.NAME, 3, a4name );

            List<String> problems= new ArrayList();
            if ( DataSetUtil.validate(result, problems ) ) {
                return result;
            } else {
                throw new IllegalArgumentException( problems.get(0) );
            }

        } else {
            ArrayDataSet zds = ArrayDataSet.copy(z);
            if (d0 != null) {
                zds.putProperty(QDataSet.DEPEND_0, d0);
            }
            if (d1 != null ) {
                zds.putProperty(QDataSet.DEPEND_1, d1);
            }
            if (d2 != null ) {
                zds.putProperty(QDataSet.DEPEND_2, d2);
            }
            List<String> problems= new ArrayList();
            if ( !DataSetUtil.validate(zds, problems ) ) {
                throw new IllegalArgumentException( problems.get(0) );
            } else {
                return zds;
            }
        }

    }

    public static QDataSet link( Object x, Object y ) {
        return link( dataset(x), dataset(y) );
    }


    public static QDataSet link( Object x, Object y, Object z ) {
        return link( dataset(x), dataset(y) );
    }


    public static QDataSet link( Object d0, Object d1, Object d2, Object z ) {
        return link( dataset(d0), dataset(d1), dataset(d2), dataset(z) );
    }



    /**
     * declare that the dataset is a dependent parameter of an independent parameter.
     * This isolates the QDataSet semantics, and verifies correctness.  See also link(x,y).
     * @param ds
     * @param dim dimension to declare dependence: 0,1,2.
     * @param dep the independent dataset.
     * @return
     */
    public static MutablePropertyDataSet dependsOn( QDataSet ds, int dim, QDataSet dep ) {
        MutablePropertyDataSet mds= DataSetOps.makePropertiesMutable(ds);
        if ( dim==0 ) {
            if ( dep!=null && ds.length()!=dep.length() ) {
                throw new IllegalArgumentException(String.format("ds.length()!=dep.length() (%d!=%d)",ds.length(),dep.length()));
            }
            mds.putProperty( QDataSet.DEPEND_0, dep );
        } else if ( dim==1 ) {
            if ( dep!=null && ds.length(0)!=dep.length() ) 
                throw new IllegalArgumentException(String.format("ds.length(0)!=dep.length() (%d!=%d)",ds.length(0),dep.length()));
            mds.putProperty( QDataSet.DEPEND_1, dep );
        } else if ( dim==2 ) {
            if ( dep!=null && ds.length(0,0)!=dep.length() ) 
                throw new IllegalArgumentException(String.format("ds.length(0,0)!=dep.length() (%d!=%d)",ds.length(0,0),dep.length()));
            mds.putProperty( QDataSet.DEPEND_2, dep );
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
     * @see slices
     * @see concatenate
     * @return rank N+1 dataset
     */
    public static QDataSet join(QDataSet ds1, QDataSet ds2) {
        if ( ds1==null && ds2==null ) throw new NullPointerException("both ds1 and ds2 are null");
        if ( ds2==null ) throw new NullPointerException("ds2 is null");
        if ( ds1==null ) {
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
     * guess a name for the dataset, looking for NAME and then safeName(LABEL).  The
     * result will be a C/Python/Java-style identifier suitable for the variable.
     * @param ds
     * @return the name or null if there is no NAME or LABEL
     */
    public static String guessName( QDataSet ds ) {
        return guessName( ds, null );
    }

    /**
     * guess a name for the dataset, looking for NAME and then safeName(LABEL).  The
     * result will be a C/Python/Java-style identifier suitable for the variable.
     * @param ds
     * @return the name, or deft if there is no NAME or LABEL.
     */
    public static String guessName( QDataSet ds, String deft ) {
        String label= (String) ds.property( QDataSet.NAME );
        if ( label==null ) {
            label= (String) ds.property( QDataSet.LABEL );
            if ( label!=null ) label= safeName( label );
        }
        if ( label==null ) {
            return deft==null ? null : safeName(deft);
        } else {
            return label;
        }
    }

    /**
     * extra spaces and pipes cause problems in the Operations text field.  Provide so that data sources can provide
     * safer names, while we test safe-name requirements on a broader test set.  Use of this method will allow us to see
     * where changes are needed.
     * @param suggest
     * @return
     */
    public static String saferName( String suggest ) {
        return suggest.trim().replaceAll("\\|","_");
    }

    /**
     * made a Java-style identifier from the provided string
     * See VirboAutoplot/src/scripts/safeName.jy which demonstrates this.
     * @param suggest
     * @return
     */
    public static String safeName( String suggest ) {
        if ( suggest.startsWith("|") && suggest.endsWith("|") ) { // taken from rich headers code.
            suggest= suggest.substring(1,suggest.length()-1)+"_mag";
        }
        StringBuilder result= new StringBuilder( suggest.replaceAll(" ", "_" ) );
        if ( result.length()==0 ) {
            return "ds";
        }
        if ( ! Character.isJavaIdentifierStart(result.charAt(0)) ) {
            if ( !Character.isJavaIdentifierPart(result.charAt(0)) ) { // we're going to add an underscore anyway
                result.replace(0,1,"_");
            } else {
                result.insert(0,"_");
            }
        }
        for ( int i=1; i<result.length(); i++ ) {
            if ( result.charAt(i)=='.' ) {
                result.replace( i, i+1, "pt" );
                i+=1;
                continue;
            }
            if ( result.charAt(i)=='*' ) {
                result.replace( i, i+1, "star" );
                i+=3;
                continue;
            }
            if ( result.charAt(i)=='/' ) {
                result.replace( i, i+1, "div" );
                i+=2;
                continue;
            }
            if ( result.charAt(i)=='+' ) {
                result.replace( i, i+1, "plus" );
                i+=3;
                continue;
            }
            if ( result.charAt(i)=='-' ) {
                result.replace( i, i+1, "_" ); // 30.0-45.0eV
                i+=0;
                continue;
            }
            if ( result.length()>(i+1) && result.charAt(i)=='<' && result.charAt(i+1)=='=' ) {
                result.replace( i, i+2, "le" );
                i+=1;
                continue;
            }
            if ( result.length()>(i+1) && result.charAt(i)=='>' && result.charAt(i+1)=='=' ) {
                result.replace( i, i+2, "ge" );
                i+=1;
                continue;
            }
            if ( result.length()>(i+1) && result.charAt(i)=='<' && result.charAt(i+1)=='>' ) {
                result.replace( i, i+2, "ne" );
                i+=1;
                continue;
            }
            if ( result.length()>(i+1) && result.charAt(i)=='!' && result.charAt(i+1)=='=' ) {
                result.replace( i, i+2, "ne" );
                i+=1;
                continue;
            }
            if ( result.charAt(i)=='='  ) {
                result.replace( i, i+1, "eq" );
                i+=1;
                continue;
            }
            if ( result.charAt(i)=='>' ) {
                result.replace( i, i+1, "gt" );
                i+=1;
                continue;
            }
            if ( result.charAt(i)=='<' ) {
                result.replace( i, i+1, "lt" );
                i+=1;
                continue;
            }
            if ( !Character.isJavaIdentifierPart( result.charAt(i) ) ) result.replace( i, i+1, "_" );
        }
        return result.toString();
    }

    /**
     * transpose the rank 2 dataset.
     * @param ds rank 2 dataset
     * @return 
     */
    public static QDataSet transpose(QDataSet ds) {
        return DDataSet.copy(new TransposeRank2DataSet(ds));
    }

    public static QDataSet transpose( Object ds ) {
        return DDataSet.copy(new TransposeRank2DataSet( dataset(ds) ));
    }
    
    /**
     * returns true iff the dataset values are equivalent.  Note this
     * may promote rank, etc.
     * If the two datasets have enumerations, then we create datums and check .equals.
     * @param ds1
     * @param ds2
     * @return
     */
    public static boolean equivalent( QDataSet ds1, QDataSet ds2 ) {
        Units u1= SemanticOps.getUnits(ds1);
        Units u2= SemanticOps.getUnits(ds2);
        if ( u1!=u2 && u1 instanceof EnumerationUnits && u2 instanceof EnumerationUnits ) {
            if ( !CoerceUtil.equalGeom( ds1, ds2 ) ) return false;
            QubeDataSetIterator it= new QubeDataSetIterator(ds1);
            while ( it.hasNext() ) {
                it.next();
                try {
                    if ( !u1.createDatum(it.getValue(ds1)).toString().equals( u2.createDatum(it.getValue(ds2) ).toString() ) ) return false;
                } catch ( IndexOutOfBoundsException ex ) {
                    return false; //
                }
            }
            return true;
        } else {
            if ( !u1.isConvertableTo(u2) ) return false;
            if ( !CoerceUtil.equalGeom( ds1, ds2 ) ) return false;
            QDataSet eq= eq( ds1, ds2 );
            QubeDataSetIterator it= new QubeDataSetIterator(eq);
            while ( it.hasNext() ) {
                it.next();
                if ( it.getValue(eq)==0 ) return false;
            }
            return true;
        }
    }
    
    public static boolean equivalent( Object ds1, Object ds2 ) {
        return equivalent( dataset(ds1), dataset(ds2) );
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
    
    public static int dimensionCount( Object dss ) {
        return dimensionCount( dataset(dss) );
    }

    public static final double PI = Math.PI;
    public static final double E = Math.E;
}
