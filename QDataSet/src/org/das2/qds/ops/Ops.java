
package org.das2.qds.ops;

import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.qds.BundleDataSet.BundleDescriptor;
import org.das2.qds.QubeDataSetIterator;
import org.das2.datum.Datum;
import org.das2.datum.EnumerationUnits;
import org.das2.datum.TimeUtil;
import org.das2.datum.Units;
import org.das2.qds.math.fft.ComplexArray;
import org.das2.qds.math.fft.GeneralFFT;
import org.das2.qds.math.fft.WaveformToSpectrum;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.regex.Pattern;
import org.das2.qds.buffer.BufferDataSet;
import org.das2.datum.CacheTag;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.DatumUtil;
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.UnitsConverter;
import org.das2.datum.UnitsUtil;
import org.das2.math.filter.Butterworth;
import org.das2.util.ClassMap;
import org.das2.util.LoggerManager;
import org.das2.util.monitor.CancelledOperationException;
import org.das2.util.monitor.NullProgressMonitor;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.util.monitor.SubTaskMonitor;
import org.das2.util.monitor.UncheckedCancelledOperationException;
import org.json.JSONException;
import org.json.JSONObject;
import org.das2.qds.AbstractDataSet;
import org.das2.qds.ArrayDataSet;
import org.das2.qds.demos.RipplesDataSet;
import org.das2.qds.BundleDataSet;
import org.das2.qds.CdfSparseDataSet;
import org.das2.qds.ConstantDataSet;
import org.das2.qds.DataSetOps;
import org.das2.qds.DataSetUtil;
import org.das2.qds.DDataSet;
import org.das2.qds.DRank0DataSet;
import org.das2.qds.DataSetIterator;
import org.das2.qds.FDataSet;
import org.das2.qds.IDataSet;
import org.das2.qds.JoinDataSet;
import org.das2.qds.LDataSet;
import org.das2.qds.MutablePropertyDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.RankZeroDataSet;
import org.das2.qds.ReplicateDataSet;
import org.das2.qds.ReverseDataSet;
import org.das2.qds.SDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.TransposeRank2DataSet;
import org.das2.qds.DataSetAnnotations;
import static org.das2.qds.DataSetUtil.propertyNames;
import org.das2.qds.IndexGenDataSet;
import org.das2.qds.IndexListDataSetIterator;
import org.das2.qds.SortDataSet;
import org.das2.qds.SparseDataSet;
import org.das2.qds.SubsetDataSet;
import org.das2.qds.TagGenDataSet;
import org.das2.qds.TailBundleDataSet;
import org.das2.qds.TrimStrideWrapper;
import org.das2.qds.WeightsDataSet;
import org.das2.qds.WritableDataSet;
import org.das2.qds.WritableJoinDataSet;
import org.das2.qds.examples.Schemes;
import org.das2.qds.util.AutoHistogram;
import org.das2.qds.util.BinAverage;
import org.das2.qds.util.DataSetBuilder;
import org.das2.qds.util.FFTUtil;
import org.das2.qds.util.LSpec;
import org.das2.qds.util.LinFit;
import org.das2.qds.math.Contour;
import org.das2.util.ColorUtil;
import org.das2.util.JsonUtil;

/**
 * A fairly complete set of operations for QDataSets, including binary operations
 * like "add" and "subtract", but also more abstract (and complex) operations like
 * smooth and fftPower.  Most operations check data units and validity, but
 * consult the documentation for each function.
 * 
 * These operations are all available in Jython scripts, and some, like add, are
 * connected to operator symbols like +.
 * 
 * @author jbf
 */
public final class Ops {

    private static final Logger logger= LoggerManager.getLogger("qdataset.ops");
    private static final String CLASSNAME = Ops.class.getCanonicalName();

    /**
     * this class cannot be instantiated.
     */
    private Ops() {    
    }
        
    /**
     * UnaryOps are one-argument operations, such as sin, abs, and sqrt
     */
    public interface UnaryOp {
        double op(double d1);
    }

    /**
     * apply the unary operation (such as "cos") to the dataset, propagating
     * DEPEND_0 through DEPEND_3 are propagated.  TODO: This should be reviewed
     * for speed (iterator is known to be slow) and other metadata that can 
     * be preserved.
     * @param ds1 the argument
     * @param op the operation for each element.
     * @return the result the the same geometry.
     */
    public static MutablePropertyDataSet applyUnaryOp(QDataSet ds1, UnaryOp op) {
        WritableDataSet result;
        
        if ( SemanticOps.isJoin(ds1) || DataSetUtil.isQube(ds1)==false ) {
            result = Ops.copy(ds1);
        } else {
            result = DDataSet.create(DataSetUtil.qubeDims(ds1));
        }
        QDataSet wds= DataSetUtil.weightsDataSet(ds1);

        double fill= -1e38;
        
        int n0;
        if ( ds1.rank()==0 ) {
            n0=0;
        } else {
            n0= ds1.length();
        }
        
        switch (ds1.rank()) {
            case 1:
                for ( int i=0; i<n0; i++ ) {
                    double w1 = wds.value(i);
                    result.putValue( i, w1==0 ? fill : op.op(ds1.value(i)) );
                }   
                break;
            case 2:
                for ( int i=0; i<n0; i++ ) {
                    int n1= ds1.length(i);
                    for ( int j=0; j<n1; j++ ) {
                        double w1 = wds.value(i,j);
                        result.putValue( i, j, w1==0 ? fill : op.op(ds1.value(i,j)) );
                    }
                }  
                break;
            case 3:
                for ( int i=0; i<n0; i++ ) {
                    int n1= ds1.length(i);
                    for ( int j=0; j<n1; j++ ) {
                        int n2= ds1.length(i,j);
                        for ( int k=0; k<n2; k++ ) {
                            double w1 = wds.value(i,j,k);
                            result.putValue( i, j, k, w1==0 ? fill : op.op(ds1.value(i,j,k)) );
                        }
                    }
                }
                break;
            default:
                QubeDataSetIterator it1 = new QubeDataSetIterator(ds1);
                while (it1.hasNext()) {
                    it1.next();
                    double d1 = it1.getValue(ds1);
                    double w1 = it1.getValue(wds);
                    it1.putValue(result, w1==0 ? fill : op.op(d1));
                }   
                break;
        }
        
        Map<String,Object> m= new HashMap<>();
        m.put( QDataSet.DEPEND_0, ds1.property(QDataSet.DEPEND_0) );
        m.put( QDataSet.DEPEND_1, ds1.property(QDataSet.DEPEND_1) );
        m.put( QDataSet.DEPEND_2, ds1.property(QDataSet.DEPEND_2) );
        m.put( QDataSet.DEPEND_3, ds1.property(QDataSet.DEPEND_3) );        
        DataSetUtil.putProperties( m, result );
        
        result.putProperty( QDataSet.FILL_VALUE, fill );
        return result;
    }
 
    /**
     * apply the unary operation (such as "cos") to the dataset.
     * DEPEND_[0-3] is propagated.
     * @param ds1 the argument which can be converted to a dataset.
     * @param op the operation for each element.
     * @return the result the the same geometry.
     */    
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
     * these are properties that are propagated through operations.
     */
    private static final String[] _dependProperties= new String[] {
                    QDataSet.DEPEND_0, QDataSet.DEPEND_1, QDataSet.DEPEND_2, QDataSet.DEPEND_3, 
                    QDataSet.BINS_0, QDataSet.BINS_1, 
    };
         
    
    /**
     * apply the binary operator element-for-element of the two datasets, minding
     * dataset geometry, fill values, etc.  The two datasets are coerced to
     * compatible geometry, if possible (e.g.Temperature[Time]+2deg), using 
     * CoerceUtil.coerce.  Structural metadata such as DEPEND_0 are preserved 
     * where this is reasonable, and dimensional metadata such as UNITS are
     * dropped.
     * 
     * @param ds1 the first argument
     * @param ds2 the second argument
     * @param op binary operation for each pair of elements
     * @return the result with the same geometry as the pair.
     */
    public static MutablePropertyDataSet applyBinaryOp( QDataSet ds1, QDataSet ds2, BinaryOp op ) {
        if ( ds1.rank()==ds2.rank() && ds1.rank()>0 ) {
            if ( ds1.length()!=ds2.length() ) {
                throw new IllegalArgumentException("binary operation on datasets of different lengths: "+ ds1 + " " + ds2 );
            }
        }

        QDataSet[] operands= new QDataSet[2];

//        if ( checkComplexArgument(ds1) || checkComplexArgument(ds2) ) {
//            if ( ds1.rank()!=ds2.rank() ) {
//                throw new IllegalArgumentException("complex numbers argument requires that rank must be equal.");
//            }
//        }
//        
        WritableDataSet result = CoerceUtil.coerce( ds1, ds2, true, operands );

        QDataSet op1= operands[0];
        QDataSet op2= operands[1];

        QDataSet w1= DataSetUtil.weightsDataSet(op1);
        QDataSet w2= DataSetUtil.weightsDataSet(op2);

        int n0;
        if ( w1.rank()>0 ) {
            n0= w1.length();
        } else {
            n0= 0;
        }
        
        double fill= -1e38;
        
        switch (w1.rank()) {
            case 1:
                for ( int i=0; i<n0; i++ ) {
                    double d1 = op1.value(i);
                    double d2 = op2.value(i);
                    double w= w1.value(i) * w2.value(i);
                    result.putValue( i, w==0 ? fill : op.op(d1, d2) );
                }       
                break;
            case 2:
                for ( int i=0; i<n0; i++ ) {
                    int n1= w1.length(i);
                    for ( int j=0; j<n1; j++ ) {
                        double d1 = op1.value(i,j);
                        double d2 = op2.value(i,j);
                        double w= w1.value(i,j) * w2.value(i,j);
                        result.putValue( i, j, w==0 ? fill : op.op(d1, d2) );
                    }
                }
                break;
            case 3:
                for ( int i=0; i<n0; i++ ) {
                    int n1= w1.length(i);
                    for ( int j=0; j<n1; j++ ) {
                        int n2= w1.length(i,j);
                        for ( int k=0; k<n2; k++ ) {
                            double d1 = op1.value(i,j,k);
                            double d2 = op2.value(i,j,k);
                            double w= w1.value(i,j,k) * w2.value(i,j,k);
                            result.putValue( i, j, k, w==0 ? fill : op.op(d1, d2) );
                        }
                    }
                }
                break;                
            default:
                QubeDataSetIterator it1 = new QubeDataSetIterator( operands[0] );
                QubeDataSetIterator it2 = new QubeDataSetIterator( operands[1] );
                while (it1.hasNext()) {
                    it1.next();
                    it2.next();
                    double d1 = it1.getValue(operands[0]);
                    double d2 = it2.getValue(operands[1]);
                    double w= it1.getValue(w1) * it2.getValue(w2);
                    it1.putValue(result, w==0 ? fill : op.op(d1, d2));
                }   break;
        }

        Map<String, Object> m1 = DataSetUtil.getProperties( operands[0], _dependProperties, null );
        Map<String, Object> m2 = DataSetUtil.getProperties( operands[1], _dependProperties, null );

        boolean resultIsQube= Boolean.TRUE.equals( m1.get(QDataSet.QUBE) ) || Boolean.TRUE.equals( m2.get(QDataSet.QUBE) );
        if ( m1.size()==1 ) m1.remove( QDataSet.QUBE ); // kludge: CoerceUtil.coerce would copy over a QUBE property, so just remove this.
        if ( m2.size()==1 ) m2.remove( QDataSet.QUBE );
        if ( ( m2.isEmpty() && !m1.isEmpty() ) || ds2.rank()==0 ) {
            m2.put( QDataSet.DEPEND_0, m1.get(QDataSet.DEPEND_0 ) );
            m2.put( QDataSet.DEPEND_1, m1.get(QDataSet.DEPEND_1 ) );
            m2.put( QDataSet.DEPEND_2, m1.get(QDataSet.DEPEND_2 ) );
            m2.put( QDataSet.DEPEND_3, m1.get(QDataSet.DEPEND_3 ) );
            m2.put( QDataSet.CONTEXT_0, m1.get(QDataSet.CONTEXT_0) );
            m2.put( QDataSet.BINS_0,   m1.get(QDataSet.BINS_0 ) );
            m2.put( QDataSet.BINS_1,   m1.get(QDataSet.BINS_1 ) );
        }
        if ( ( m1.isEmpty() && !m2.isEmpty() ) || ds1.rank()==0 ) {
            m1.put( QDataSet.DEPEND_0, m2.get(QDataSet.DEPEND_0 ) );
            m1.put( QDataSet.DEPEND_1, m2.get(QDataSet.DEPEND_1 ) );
            m1.put( QDataSet.DEPEND_2, m2.get(QDataSet.DEPEND_2 ) );
            m1.put( QDataSet.DEPEND_3, m2.get(QDataSet.DEPEND_3 ) );
            m1.put( QDataSet.CONTEXT_0, m2.get(QDataSet.CONTEXT_0) );
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
    
    /**
     * As with applyBinaryOp, but promote compatible objects to QDataSet first.
     * @param ds1 the first operand
     * @param ds2 the second operand
     * @param op  binary operation for each pair of elements
     * @return the result with the same geometry as the pair.
     * @see #dataset(java.lang.Object) 
     */
    public static MutablePropertyDataSet applyBinaryOp( Object ds1, Object ds2, BinaryOp op ) {
        return applyBinaryOp( dataset(ds1), dataset(ds2), op );
    }

    /**
     * returns the subset of two groups of properties that are equal, so these
     * may be preserved through operations.
     * @param m1 map of dataset properties, including DEPEND properties.
     * @param m2 map of dataset properties, including DEPEND properties.
     * @return the subset of two groups of properties that are equal
     */
    public static HashMap<String, Object> equalProperties(Map<String, Object> m1, Map<String, Object> m2) {
        HashMap<String,Object> result = new HashMap<>();
        m1.entrySet().forEach((e) -> {
            String k= e.getKey();
            Object v = e.getValue();
            if (v != null) {
                Object v2= m2.get(k);
                if ( v.equals(v2) ) {
                    result.put(k, v);
                } else if ( ( v instanceof QDataSet ) && ( v2 instanceof QDataSet ) ) {
                    if ( equivalent( (QDataSet)v, (QDataSet)v2 ) ) {
                        result.put( k, v );
                    }
                }
            }
        });
        return result;
    }

    /**
     * returns the BinaryOp that handles the addition operation.  Properties will have the units inserted.
     * @param ds1 the first operand
     * @param ds2 the second operand
     * @param properties the result properties
     * @return the BinaryOp that handles the addition operation.
     */
    private static BinaryOp addGen( QDataSet ds1, QDataSet ds2, Map<String,Object> properties ) {
        Units units1 = SemanticOps.getUnits( ds1 );
        Units units2 = SemanticOps.getUnits( ds2 );
        BinaryOp result;
        if ( units2.isConvertibleTo(units1) && UnitsUtil.isRatioMeasurement(units1) ) {
            final UnitsConverter uc= UnitsConverter.getConverter( units2, units1);
            result= (double d1, double d2) -> d1 + uc.convert(d2);
            if ( units1!=Units.dimensionless ) properties.put( QDataSet.UNITS, units1 );
        } else if ( UnitsUtil.isIntervalMeasurement(units1) ) {
            if ( UnitsUtil.isIntervalMeasurement(units2) ) {
                throw new IllegalArgumentException("two location units cannot be added: " + units1 + ", "+ units2  );
            }
            if ( units2==Units.dimensionless && !UnitsUtil.isTimeLocation(units1) ) {
                units2= units1.getOffsetUnits();
            }
            final UnitsConverter uc= UnitsConverter.getConverter( units2, units1.getOffsetUnits() );
            result= (double d1, double d2) -> d1 + uc.convert(d2);
            properties.put( QDataSet.UNITS, units1 );
        } else if ( UnitsUtil.isIntervalMeasurement(units2) ) {
            final UnitsConverter uc= UnitsConverter.getConverter( units1, units2.getOffsetUnits() );
            result= (double d1, double d2) -> uc.convert(d1) + d2;
            properties.put( QDataSet.UNITS, units2 );
        } else if ( UnitsUtil.isRatioMeasurement(units1) && units2.isConvertibleTo(Units.dimensionless)  ) {
            result= (double d1, double d2) -> d1 + d2;
            properties.put( QDataSet.UNITS, units1 );
        } else {
            throw new IllegalArgumentException("units cannot be added: " + units1 + ", "+ units2 );
        }
        return result;
    }
    /**
     * add the two datasets which have the compatible geometry and units.  For example,
     *<blockquote><pre>{@code
     *ds1=timegen('2014-10-15T07:23','60s',300)
     *ds2=dataset('30s')
     *print add(ds1,ds2)
     *}</pre></blockquote>
     * The units handling is quite simple, and this will soon change.
     * Note that the Jython operator + is overloaded to this method.
     * 
     * @param ds1 a rank N dataset
     * @param ds2 a rank M dataset with compatible geometry
     * @return the element-wise sum of the two datasets.
     * @see #addGen(org.das2.qds.QDataSet, org.das2.qds.QDataSet, java.util.Map) addGen, which shows how units are resolved.
     */
    public static QDataSet add(QDataSet ds1, QDataSet ds2) {
        Map<String,Object> props= new HashMap<>();
        BinaryOp b= addGen( ds1, ds2, props );
        MutablePropertyDataSet result= applyBinaryOp( ds1, ds2, b );
        result.putProperty( QDataSet.UNITS, props.get(QDataSet.UNITS) );
        result.putProperty(QDataSet.NAME, null );
        result.putProperty(QDataSet.LABEL, maybeLabelInfixOp( ds1, ds2, "+" ) );
        return result;
    }

    /**
     * add the two datasets which have the compatible geometry and units. 
     * @param ds1 QDataSet, array, string, scalar argument
     * @param ds2 QDataSet, array, string, scalar argument with compatible geometry.
     * @return the element-wise sum of the two datasets.
     * @see #dataset(java.lang.Object) 
     */ 
    public static QDataSet add( Object ds1, Object ds2 ) {
        return add( dataset(ds1), dataset(ds2) );
    }
    
    /**
     * subtract one dataset from another.
     * @param ds1 a rank N dataset
     * @param ds2 a rank M dataset with compatible geometry
     * @return the element-wise difference of the two datasets.
     */
    public static QDataSet subtract(QDataSet ds1, QDataSet ds2) {
        Units units1 = SemanticOps.getUnits( ds1 );
        Units units2 = SemanticOps.getUnits( ds2 );
        MutablePropertyDataSet result;

        if ( units2.isConvertibleTo(units1) && UnitsUtil.isRatioMeasurement(units1) ) {
            final UnitsConverter uc= UnitsConverter.getConverter( units2, units1);
            result= applyBinaryOp(ds1, ds2, 
                    (BinaryOp) (double d1, double d2) -> d1 - uc.convert(d2));
            if ( units1!=Units.dimensionless ) result.putProperty( QDataSet.UNITS, units1 );
        } else if ( UnitsUtil.isIntervalMeasurement(units1) && UnitsUtil.isIntervalMeasurement(units2) ) {
            final UnitsConverter uc= UnitsConverter.getConverter( units2, units1 );
            result= applyBinaryOp(ds1, ds2, 
                    (BinaryOp) (double d1, double d2) -> d1 - uc.convert(d2));
            result.putProperty( QDataSet.UNITS, units1.getOffsetUnits() );
        } else if ( UnitsUtil.isIntervalMeasurement(units1) && !UnitsUtil.isIntervalMeasurement(units2)) {
            if ( units2==Units.dimensionless && !UnitsUtil.isTimeLocation(units1) ) {
                units2= units1.getOffsetUnits();
            }
            final UnitsConverter uc= UnitsConverter.getConverter( units2, units1.getOffsetUnits() );
            result= applyBinaryOp(ds1, ds2, 
                    (BinaryOp) (double d1, double d2) -> d1 - uc.convert(d2));
            result.putProperty( QDataSet.UNITS, units1 );
        } else if ( UnitsUtil.isIntervalMeasurement(units2) && !UnitsUtil.isIntervalMeasurement(units1)) {
            throw new IllegalArgumentException("cannot subtract interval measurement from ratio measurement: " + units1 + " - "+ units2 );

        } else if ( UnitsUtil.isRatioMeasurement(units1) && units2.isConvertibleTo(Units.dimensionless) ) {
            result= applyBinaryOp(ds1, ds2, 
                    (BinaryOp) (double d1, double d2) -> d1 - d2);
            result.putProperty( QDataSet.UNITS, units1 );

        } else if ( UnitsUtil.isRatioMeasurement(units2) && units1.isConvertibleTo(Units.dimensionless) ) {
            result= applyBinaryOp(ds1, ds2, 
                    (BinaryOp) (double d1, double d2) -> d1 - d2);
            result.putProperty( QDataSet.UNITS, units2 );            
        } else {
            throw new IllegalArgumentException("cannot subtract: " + units1 + " - "+ units2 );
        }
        result.putProperty(QDataSet.NAME, null );
        result.putProperty(QDataSet.MONOTONIC, null );
        result.putProperty(QDataSet.LABEL, maybeLabelInfixOp( ds1, ds2, "-" ) );
        return result;
    }

    /**
     * subtract one dataset from another, using dataset to convert the arguments.
     * @param ds1 QDataSet, array, string, scalar argument
     * @param ds2  QDataSet, array, string, scalar argument with compatible geometry
     * @return the element-wise difference of the two datasets.
     */
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
        return opStr + "("+l1Str + ")";
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
        if ( ! idpat.matcher(l1Str).matches() ) l1Str= "("+l1Str+")";
        String l2Str= label2;
        if ( ! idpat.matcher(l2Str).matches() ) l2Str= "("+l2Str+")";
        if ( l1Str!=null && l2Str!=null ) {
            return l1Str + opStr + l2Str;
        } else {
            return null;
        }
    }
    
    /**
     * only copy the property if it is defined in src.  If it is not in src, then
     * we will insert null into dest if it is found there.
     * @param name the property name
     * @param src the source dataset
     * @param dest the destination dataset
     */
    private static void maybeCopyProperty( String name, QDataSet src, MutablePropertyDataSet dest ) {
        Object o= src.property(name);
        if ( o!=null ) {
            dest.putProperty(name, o);
        } else {
            if ( dest.property(name)!=null ) { // clear the non-null property.
                dest.putProperty(name,null);
            }
        }
    }
    
    /**
     * return a dataset with each element negated.
     * If units are specified, Units must be ratiometric units, like "5 km" 
     * or dimensionless, and not ordinal or time location units.
     * @see #copysign
     * @see #signum
     * @param ds1
     * @return
     */
    public static QDataSet negate(QDataSet ds1) {
        Units u= SemanticOps.getUnits(ds1);
        if ( !UnitsUtil.isRatioMeasurement(u) ) {
            throw new IllegalArgumentException("Units are not ratiometric units");
        }
        MutablePropertyDataSet mpds= 
                applyUnaryOp(ds1, (UnaryOp) (double d1) -> -d1);
        mpds.putProperty(QDataSet.UNITS,u);
        return mpds;
    }
    
    public static QDataSet negate( Object ds1 ) {
        return negate( dataset(ds1) );
    }

    /**
     * return the magnitudes of vectors in a rank 1 or greater dataset (typically
     * rank 2).  The last index should be the cartesian dimension.  For example,
     **<blockquote><pre><small>{@code
     * ds= getDataSet('http://autoplot.org/data/autoplot.cdf?BGSM') # BGSM[Epoch=24,cart=3]
     * m= magnitude(ds)
     *}</small></pre></blockquote>
     * For rank 0, this just returns the absolute value, but with the same units.
     * 
     * @param ds dataset of Rank N.
     * @return dataset of Rank N-1.
     * @see #abs(org.das2.qds.QDataSet) 
     */
    public static QDataSet magnitude(QDataSet ds) {
        
        if ( ds==null ) throw new IllegalArgumentException("input ds is null");
        
        int r = ds.rank();
        if ( r==0 ) {
            return DataSetUtil.asDataSet( Math.abs(ds.value()), (Units) ds.property(QDataSet.UNITS) ); //TODO: invalid.
        }
        QDataSet depn = (QDataSet) ds.property("DEPEND_" + (r - 1));
        boolean isCart;
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
            QDataSet wds= DataSetUtil.weightsDataSet(ds);
            double fill= ((Number)wds.property( "SUGGEST_FILL" )).doubleValue();
            Map<String,Object> props= DataSetUtil.getProperties(ds);
            props.remove("DEPEND_"+(ds.rank()-1) );
            props.remove("BUNDLE_"+(ds.rank()-1) );
            props.put( QDataSet.FILL_VALUE, fill );            
            switch (ds.rank()) {
                case 2:
                    {
                        DDataSet result= DDataSet.create( Arrays.copyOf( DataSetUtil.qubeDims(ds), ds.rank()-1 ) );
                        for ( int i0=0; i0<ds.length(); i0++ ) {
                            int n= ds.length(0);
                            double a=0;
                            boolean valid=true;
                            for ( int i1=0; i1<n; i1++ ) {
                                double w= wds.value(i0,i1);
                                if ( w==0 ) {
                                    valid=false;
                                    break;
                                }
                                double d= ds.value(i0,i1);
                                a= a + d*d;
                            }
                            if ( valid ) {
                                a= Math.sqrt(a);
                                result.putValue( i0, a );
                            } else {
                                result.putValue( i0, fill );
                            }
                        }
                        DataSetUtil.putProperties( props, result);                        
                        ds= result;
                        break;
                    }
                case 3:
                    {
                        DDataSet result= DDataSet.create( Arrays.copyOf( DataSetUtil.qubeDims(ds), ds.rank()-1 ) );
                        for ( int i0=0; i0<ds.length(); i0++ ) {
                            int n= ds.length(0,0);
                            for ( int i1=0; i1<ds.length(0); i1++ ) {
                                double a=0;
                                boolean valid= true;
                                for ( int i2=0; i2<n; i2++ ) {
                                    double w= wds.value(i0,i1,i2);
                                    if ( w==0 ) {
                                        valid= false;
                                        break;
                                    }
                                    double d= ds.value(i0,i1,i2);
                                    a= a + d*d;
                                }
                                if ( valid ) {
                                    a= Math.sqrt(a);
                                    result.putValue( i0, i1, a );
                                } else {
                                    result.putValue( i0, fill );
                                }
                            }
                        }
                        DataSetUtil.putProperties( DataSetUtil.getProperties(ds), result);
                        ds= result;
                        break;
                    }
                default:
                    ds = pow(ds, 2);
                    ds = total(ds, r - 1);
                    ds = sqrt(ds);
                    if ( u!=null ) ((MutablePropertyDataSet)ds).putProperty(QDataSet.UNITS,u);
                    break;
            }
            return ds;
        } else {
            throw new IllegalArgumentException("last dim must have COORDINATE_FRAME property.  See also abs() method. ds="+ds);
        }

    }
    
    public static QDataSet magnitude( Object ds1 ) {
        return magnitude( dataset(ds1) );
    }

    /**
     * return the total of all the elements in the dataset.  If there are 
     * invalid measurements, then fill is returned.
     * Does not support BINS or BUNDLE dimensions.
     *
     * @param ds
     * @return the unweighted total of the dataset, or -1e31 if fill was encountered.
     * @see #total(org.das2.qds.QDataSet, int) total, which should be used instead.
     */
    public static double total(QDataSet ds) {
        return total(ds,new NullProgressMonitor());
    }
    
    /**
     * return the total of all the elements in the dataset.  If there are 
     * invalid measurements, then fill is returned.
     * Does not support BINS or BUNDLE dimensions.
     *
     * @param ds
     * @param mon progress monitor
     * @return the unweighted total of the dataset, or -1e31 if fill was encountered.
     * @see #total(org.das2.qds.QDataSet, int, org.das2.util.monitor.ProgressMonitor) total, which should be used instead.
     */
    public static double total(QDataSet ds,ProgressMonitor mon) {
        double s = 0;
        QubeDataSetIterator it1 = new QubeDataSetIterator(ds);
        QDataSet wds= DataSetUtil.weightsDataSet(ds);
        double fill = ((Number) wds.property(WeightsDataSet.PROP_SUGGEST_FILL)).doubleValue();
        
        it1.setMonitor(mon);
        
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

    /**
     * return the total of all the elements in the object which can be converted
     * to a dataset.
     *
     * @param ds1 the object which can be converted to a dataset.
     * @return 
     * @see #total(org.das2.qds.QDataSet) 
     */
    public static double total( Object ds1 ) {
        return total( dataset(ds1) );
    }    
    
    /**
     * interface for an average operation that combines numbers.  For example, mean or geometric mean, but
     * also "maxOf" and total are AverageOps that can be implemented.
     */
    private interface AverageOp {

        /**
         * average in measurement d1 with weight w1 into accum.
         * @param d1 the data point, possibly NaN.  Note NaN*0=NaN.
         * @param w1 the weight of the data, where 0 indicates the measurement can be ignored, and a positive 
         * number indicates the weight relative others in the average.
         * @param accum storage for the average and the weight.  accum[0] is the average, accum[1] is the weight. 
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
     * Only QUBEs are supported presently, or dim can be the last dimension of a non-qube.
     * It is assumed that when there is just one element, that one element can be returned.
     * 
     * @param ds rank N qube dataset.
     * @param dim zero-based index number.
     * @param AverageOp operation to combine measurements, such as max or mean.
     * @param mon null or a progress monitor
     * @return rank N-1 qube dataset.
     */
    private static QDataSet averageGen(QDataSet ds, int dim, AverageOp op, ProgressMonitor mon ) throws CancelledOperationException {
        if ( ds==null ) throw new NullPointerException("ds reference is null");        
        int[] qube = DataSetUtil.qubeDims(ds);
        if ( qube==null && dim<(ds.rank()-1) ) 
            throw new IllegalArgumentException("dataset is not a qube, operations can only be done on the last index");
        if ( mon==null ) mon= new NullProgressMonitor();
        if ( dim>=ds.rank() ) 
            throw new IllegalArgumentException( String.format( "dimension index (%d) exceeds rank (%d)", dim, ds.rank() ) );
        
        // optimize for Ivar's case, where average is done over 1 element.  
        // This is just a slice!
        if ( qube!=null && qube[dim]==1 ) {
            switch (dim) {
                case 0:
                    return ds.slice(0);
                case 1:
                    return Ops.slice1( ds, 0 );
                case 2:
                    return Ops.slice2( ds, 0 );
                case 3:
                    return Ops.slice3( ds, 0 );
                default:
                    break;
            }
        }
        
        int[] newQube;
        if ( qube!=null ) {
            newQube= DataSetOps.removeElement(qube, dim);
        } else {
            switch ( ds.rank() ) {
                // rank 0 and rank 1 are automatically qubes.
                case 2:
                    newQube= new int[] { ds.length() };
                    break;
                case 3:
                    newQube= new int[] { ds.length(), ds.length(0) };
                    break;
                case 4:
                    newQube= new int[] { ds.length(), ds.length(0), ds.length(0,0) };
                    break;
                default: 
                    throw new IllegalArgumentException("implementation error");
            }
        }
        
        QDataSet wds = DataSetUtil.weightsDataSet(ds);
        DDataSet result = DDataSet.create(newQube);
        DDataSet wresult= DDataSet.create(newQube);
        QubeDataSetIterator it1 = new QubeDataSetIterator(result);
        
        it1.setMonitor(mon.getSubtaskMonitor("reduce"));
        
        double fill = ((Number) wds.property( WeightsDataSet.PROP_SUGGEST_FILL )).doubleValue();
        double[] store = new double[2];
        while (it1.hasNext()) {
            if ( mon.isCancelled() ) {
                throw new CancelledOperationException("User pressed cancel");
            }
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
            it1.putValue(wresult, store[1] );
        }
        Map<String,Object> props= DataSetUtil.getProperties(ds);
        props= DataSetOps.sliceProperties( props, dim );
        DataSetUtil.putProperties( props, result );
        result.putProperty(QDataSet.FILL_VALUE,fill);
        result.putProperty(QDataSet.WEIGHTS,wresult);

        for ( int i=dim+1; i<ds.rank(); i++ ) {
            QDataSet dep= (QDataSet)ds.property( "DEPEND_"+i );
            if ( dep!=null && dep.rank()>2 ) {
                dep= reduceMean( dep, dim, mon.getSubtaskMonitor("average DEPEND_"+dim) );
                result.putProperty( "DEPEND_"+(i-1), dep );
            }   
        }
        
        QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
        if ( dim==0 && dep0!=null && dep0.length()>0 ) {
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
     * @return rank N-1 dataset.
     * @see #total(org.das2.qds.QDataSet) total(ds) total, which is an earlier deprecated routine.
     * @see #reduceSum(org.das2.qds.QDataSet, int) reduceSum, which skips invalid data.
     */
    public static QDataSet total(QDataSet ds, int dim) {
        try {
            return total(ds,dim,new NullProgressMonitor());
        } catch ( CancelledOperationException ex ) {
            throw new IllegalArgumentException("null monitor cannot be cancelled");
        }
    }   
    
    /**
     * reduce the dataset's rank by totaling all the elements along a dimension.
     * Only QUBEs are supported presently.
     * 
     * @param ds rank N qube dataset.  N=1,2,3,4
     * @param dim zero-based index number.
     * @param mon progress monitor.
     * @return the rank N-1 qube dataset.
     * @throws CancelledOperationException
     */
    public static QDataSet total(QDataSet ds, int dim, ProgressMonitor mon) throws CancelledOperationException {
        int[] qube = DataSetUtil.qubeDims(ds);
        if ( qube==null ) throw new IllegalArgumentException("argument does not appear to be qube");
        int[] newQube = DataSetOps.removeElement(qube, dim);
        QDataSet wds = DataSetUtil.weightsDataSet(ds);
        DDataSet result= DDataSet.create(newQube);
        DDataSet weights= DDataSet.create(newQube);
        double fill = ((Number) wds.property(WeightsDataSet.PROP_SUGGEST_FILL)).doubleValue();
        
        if ( ds.rank()==2 && dim==1 ) {
            int jlen= ds.length(0);
            mon.setTaskSize(result.length());
            mon.started();
            for ( int i=0; i<result.length(); i++ ) {
                mon.setTaskProgress(i);
                boolean isfill=false;
                for ( int j=0; j<jlen; j++ ) {
                    if ( wds.value(i,j)==0 ) {
                        isfill= true;
                    } else {
                        result.addValue( i, ds.value(i,j) );
                    }
                }
                if ( isfill ) {
                    result.putValue( i, fill );
                } else {
                    weights.putValue( i, 1 );
                }
            }
        } else {
            QubeDataSetIterator it1 = new QubeDataSetIterator(result);
            it1.setMonitor(mon);
            while (it1.hasNext()) {
                if ( mon.isCancelled() ) throw new CancelledOperationException("total cancelled"); 
                it1.next();
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
                    double w1 = it0.getValue(wds) > 0 ? 1 : 0.;
                    s += w1 * it0.getValue(ds);
                    w += w1;
                }
                it1.putValue(result, w > 0 ? s : fill);
                it1.putValue(weights, w );
            }
        }
        Map<String,Object> props= DataSetUtil.getProperties(ds);
        props= DataSetOps.sliceProperties( props, dim );
        
        for ( int i=dim+1; i<ds.rank(); i++ ) {
            QDataSet dep= (QDataSet) ds.property( "DEPEND_"+i );
            if ( dep!=null && dep.rank()>1 && DataSetUtil.isConstant( dep, dim ) ) {
                switch (dim) {
                    case 0:
                        dep= Ops.slice0(dep,0);
                        break;
                    case 1:
                        dep= Ops.slice1(dep,0);
                        break;
                    case 2:
                        dep= Ops.slice2(dep,0);
                        break;
                    case 3:
                        dep= Ops.slice3(dep,0);
                        break;
                    default:
                        dep= null;
                }
                props.put( "DEPEND_"+(i-1), dep );
            }
        }
        
        DataSetUtil.putProperties( props, result );
        result.putProperty(QDataSet.WEIGHTS,weights);
        result.putProperty(QDataSet.FILL_VALUE,fill);
        return result;
    }
    
    /**
     * reduce the dataset's rank by reporting the max of all the elements along a dimension.
     * Only QUBEs are supported presently.
     * 
     * @param ds rank N qube dataset.
     * @param dim zero-based index number.
     * @return rank N-1 dataset.
     */
    public static QDataSet reduceMax(QDataSet ds, int dim) {
        return reduceMax( ds, dim, null );
    }
    
    /**
     * reduce the dataset's rank by reporting the max of all the elements along a dimension.
     * Only QUBEs are supported presently.
     * 
     * @param ds rank N qube dataset.
     * @param dim zero-based index number.
     * @param mon progress monitor 
     * @return rank N-1 dataset.
     * @see #reduceMin(org.das2.qds.QDataSet, int, org.das2.util.monitor.ProgressMonitor) 
     * @see #reduceMean(org.das2.qds.QDataSet, int, org.das2.util.monitor.ProgressMonitor) 
     * @see #extent(org.das2.qds.QDataSet) 
     * @see #extent(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet reduceMax(QDataSet ds, int dim, ProgressMonitor mon ) {
        try {
            return averageGen(ds, dim, new AverageOp() {
                @Override
                public void accum(double d1, double w1, double[] accum) {
                    if (w1 > 0.0) {
                        accum[0] = Math.max(d1, accum[0]);
                        accum[1] += w1;
                    }
                }
                @Override
                public void initStore(double[] accum) {
                    accum[0] = Double.NEGATIVE_INFINITY;
                    accum[1] = 0.;
                }
                @Override
                public void normalize(double[] accum) {
                    // nothing to do
                }
            }, mon );
        } catch ( CancelledOperationException ex ) {
            throw new RuntimeException(ex);
        }
    }    
    
    /**
     * reduce the dataset's rank by reporting the sum of all the valid elements along a dimension.  The property 
     * "WEIGHTS" will contain the sum of the weights.
     * Only QUBEs are supported presently.  This is like the function "total," but skips invalid values.
     * 
     * @param ds rank N qube dataset.
     * @param dim zero-based index number.
     * @return rank N-1 dataset.
     * @see #total(org.das2.qds.QDataSet, int) 
     */
    public static QDataSet reduceSum(QDataSet ds, int dim) {
        try {
            return averageGen(ds, dim, new AverageOp() {
                @Override
                public void accum(double d1, double w1, double[] accum) {
                    if (w1 > 0.0) {
                        accum[0] = accum[0] + d1;
                        accum[1] += w1;
                    }
                }
                @Override
                public void initStore(double[] accum) {
                    accum[0] = 0.;
                    accum[1] = 0.;
                }
                @Override
                public void normalize(double[] accum) {
                    // nothing to do
                }
            }, new NullProgressMonitor() );
        } catch ( CancelledOperationException ex ) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * reduce the dataset's rank by reporting the min of all the elements along a dimension.
     * Only QUBEs are supported presently.
     * 
     * @param ds rank N qube dataset.
     * @param dim zero-based index number.
     * @return rank N-1 dataset.
     */
    public static QDataSet reduceMin(QDataSet ds, int dim) {
        return reduceMin( ds, dim, null );
    }
    
    /**
     * reduce the dataset's rank by reporting the min of all the elements along a dimension.
     * Only QUBEs are supported presently.
     * 
     * @param ds rank N qube dataset.
     * @param dim zero-based index number.
     * @param mon progress monitor 
     * @return rank N-1 dataset.
     */
    public static QDataSet reduceMin(QDataSet ds, int dim, ProgressMonitor mon ) {
        try {
            return averageGen(ds, dim, new AverageOp() {
                @Override
                public void accum(double d1, double w1, double[] accum) {
                    if (w1 > 0.0) {
                        accum[0] = Math.min(d1, accum[0]);
                        accum[1] += w1;
                    }
                }
                @Override
                public void initStore(double[] accum) {
                    accum[0] = Double.POSITIVE_INFINITY;
                    accum[1] = 0.;
                }
                @Override
                public void normalize(double[] accum) {
                    // nothing to do
                }
            }, mon );
        } catch ( CancelledOperationException ex ) {
            throw new RuntimeException(ex);
        }        
    }

    /**
     * reduce the dataset's rank by reporting the mean of all the elements along a dimension.
     * Only QUBEs are supported presently.  Note this does not contain code that would remove
     * large offsets from zero when making the average, so the number of points is limited.
     * 
     * @param ds rank N qube dataset.
     * @param dim zero-based index number.
     * @return rank N-1 qube dataset.
     */
    public static QDataSet reduceMean(QDataSet ds, int dim) {
        try {
            return reduceMean( ds, dim, new NullProgressMonitor() );
        } catch (CancelledOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * reduce the dataset's rank by reporting the mean of all the elements along a dimension.
     * Only QUBEs are supported presently.  Note this does not contain code that would remove
     * large offsets from zero when making the average, so the number of points is limited.
     * 
     * @param ds rank N qube dataset.
     * @param dim zero-based index number.
     * @param mon progress monitor.
     * @return rank N-1 qube dataset
     * @throws org.das2.util.monitor.CancelledOperationException
     */
    public static QDataSet reduceMean(QDataSet ds, int dim, ProgressMonitor mon ) throws CancelledOperationException {
        if ( dim==1 ) { // special code for bins, since this happens often
            int[] qube= DataSetUtil.qubeDims(ds);
            if ( qube!=null && qube[1]==2 ) {
                QDataSet left= Ops.slice1( ds, 0 );
                QDataSet right= Ops.slice1( ds, 1 );
                return Ops.add( left, Ops.divide( Ops.subtract(right,left), 2 ) );
            }
        }
        return averageGen(ds, dim, new AverageOp() {
            @Override
            public void accum(double d1, double w1, double[] accum) {
                if ( w1 > 0.0 ) { // because 0 * NaN is NaN...
                    accum[0] += w1 * d1;
                    accum[1] += w1;
                }
            }
            @Override
            public void initStore(double[] accum) {
                accum[0] = 0.;
                accum[1] = 0.;
            }
            @Override
            public void normalize(double[] accum) {
                if (accum[1] > 0.) {
                    accum[0] /= accum[1];
                }
            }
        }, mon );
    }

    /**
     * reduce the dataset's rank by reporting the median of all the elements along a dimension.
     * Only QUBEs are supported presently.  Note the weights reported are the totals of the data going in to each
     * median, typically the number of measurements compared (when all weights are 0 or 1).
     * 
     * @param ds rank N qube dataset.
     * @param dim zero-based index number.
     * @param mon progress monitor.
     * @return rank N-1 qube dataset
     * @throws org.das2.util.monitor.CancelledOperationException
     */
    public static QDataSet reduceMedian(QDataSet ds, int dim, ProgressMonitor mon ) throws CancelledOperationException {
        return averageGen(ds, dim, new AverageOp() {
            
            DataSetBuilder b;
            
            @Override
            public void accum(double d1, double w1, double[] accum) {
                if ( w1 > 0.0 ) { // because 0 * NaN is NaN...
                    b.nextRecord(d1);
                    accum[1] += w1;
                }
            }
            @Override
            public void initStore(double[] accum) {
                b= new DataSetBuilder(1,100);
                accum[1]= 0.0;
            }
            @Override
            public void normalize(double[] accum) {
                QDataSet all= b.getDataSet();
                if ( all.length()==0 ) {
                    accum[0]= Double.NaN;
                    accum[1]= 0.0;                    
                } else {
                    accum[0]= median(all).value();
                }
            }
        }, mon );
    }
    
    
    /**
     * reduce each bin to its center.  If the spacing is
     * log, then geometric centers are used.
     * @param dep1 rank 2 [N,2] bins dataset, where bins are min,max boundaries.
     * @return rank 1 N element dataset
     */
    public static QDataSet reduceBins(QDataSet dep1) {
        if ( dep1.property(QDataSet.BINS_1).equals(QDataSet.VALUE_BINS_MIN_MAX) ) {
            DDataSet result= DDataSet.createRank1(dep1.length());
            int n= result.length();
            if ( QDataSet.VALUE_SCALE_TYPE_LOG.equals(dep1.property(QDataSet.SCALE_TYPE)) ) {        
                for ( int i=0; i<n; i++ ) {
                    result.putValue(i,Math.sqrt(dep1.value(i,0)*dep1.value(i,1)));
                }
            } else {
                for ( int i=0; i<n; i++ ) {
                    result.putValue(i,(dep1.value(i,0)+dep1.value(i,1))/2);
                }                
            }
            DataSetUtil.copyDimensionProperties( dep1, result );
            return result;
        } else {
            throw new IllegalArgumentException("dataset must be rank 2 bins dataset, with min,max");
        }
    }
    
    /**
     * increase the rank 1 data into rank 2 data by binning data.  For example,
     * <code>byOrbit= binData( density, datumRange( 'orbit:rbspa-pp:3' ) )</code>
     * 
     * @param ds rank 1 data with (time) tags
     * @param bin datum range which is an example range
     * @return rank 2 bins.
     * 
     */
    public static QDataSet binData( QDataSet ds, DatumRange bin ) {
        JoinDataSet resultx= new JoinDataSet(2);
        JoinDataSet resulty= new JoinDataSet(2);
        DataSetBuilder resultDep= new DataSetBuilder(1,100);
        QDataSet xds= SemanticOps.xtagsDataSet(ds);
        QDataSet yds= SemanticOps.ytagsDataSet(ds);
        DataSetBuilder currentAccumY= new DataSetBuilder(1,100);
        DataSetBuilder currentAccumX= new DataSetBuilder(1,100);
        for ( int i=0; i<ds.length(); i++ ) {
            Datum x= DataSetUtil.asDatum(xds.slice(i));
            while ( bin.min().gt(x) ) {
                DatumRange pbin= bin.previous();
                if ( pbin==bin ) break; // orbit datum ranges get stuck at ends
                if ( currentAccumX.getLength()>0 ) {
                    resultDep.nextRecord(bin.middle());
                    resultx.join(currentAccumX.getDataSet());
                    resulty.join(currentAccumY.getDataSet());
                    currentAccumY= new DataSetBuilder(1,100);
                    currentAccumX= new DataSetBuilder(1,100);
                }
                bin= pbin;                
            }
            while ( bin.max().le(x) ) {
                DatumRange pbin= bin.next();
                if ( pbin==bin ) break; // orbit datum ranges get stuck at ends
                if ( currentAccumX.getLength()>0 ) {
                    resultDep.nextRecord(bin.middle());
                    resultx.join(currentAccumX.getDataSet());
                    resulty.join(currentAccumY.getDataSet());
                    currentAccumY= new DataSetBuilder(1,100);
                    currentAccumX= new DataSetBuilder(1,100);
                }
                bin= pbin;
            }
            if ( bin.contains(x) ) {
                currentAccumX.nextRecord(x);
                currentAccumY.nextRecord(yds.slice(i));
            }
        }
        if ( currentAccumX.getLength()>0 ) {
            resultDep.nextRecord(bin.middle());
            resultx.join(currentAccumX.getDataSet());
            resulty.join(currentAccumY.getDataSet());
        }
        QDataSet result= resulty;
        QDataSet dep1= resultx;
        QDataSet dep0= resultDep.getDataSet();
        
        return Ops.link( dep0, dep1, result );
    }
    
    /**
     * normalize the data so that the max is 1, where we normalize by the biggest
     * value, so that the maximum is one.  
     * @param ds
     * @return dataset with the same geometry as ds.
     */
    public static QDataSet normalize( QDataSet ds ) {
        QDataSet n= abs( extent(ds) );
        if ( n.value(0) > n.value(1) ) {
            n= n.slice(0);
        } else {
            n= n.slice(1);
        }
        MutablePropertyDataSet result= Ops.maybeCopy( Ops.divide( ds, n ) );
        Map<String,Object> props= DataSetUtil.getProperties(ds);
        props.put(QDataSet.UNITS, Units.dimensionless );
        props.put(QDataSet.TITLE, "" + props.get(QDataSet.TITLE)+" (normalized)");
        props.put(QDataSet.LABEL, "" + props.get(QDataSet.LABEL)+" (normalized)");
        DataSetUtil.putProperties( props, result );
        
        return result;
    }

    /**
     * normalize the data so that the max at a row or column is 1, where we 
     * have normalized by the biggest value along a dimension.  For example, 
     * normalize( ds, 1 ) will produce a result
     * where each ds[i,:] will have a max of 1.0.
     * @param ds the dataset
     * @param dim the dimension to normalize
     * @return dataset with the same geometry as ds.
     * @see #reduceMax(org.das2.qds.QDataSet, int) 
     */
    public static QDataSet normalize( QDataSet ds, int dim ) {
        QDataSet n= reduceMax( ds, dim );
        MutablePropertyDataSet result= Ops.maybeCopy( Ops.divide( ds, n ) );
        Map<String,Object> props= DataSetUtil.getProperties(ds);
        props.put(QDataSet.UNITS, Units.dimensionless );
        props.put(QDataSet.TITLE, "" + props.get(QDataSet.TITLE)+" (normalized)");
        props.put(QDataSet.LABEL, "" + props.get(QDataSet.LABEL)+" (normalized)");
        DataSetUtil.putProperties( props, result );
        
        return result;
    }
    
    /**
     * reduce the size of the data by keeping every 10th measurement.  
     * @param ds a qube dataset.
     * @return a decimated qube dataset.
     * @see #decimate(org.das2.qds.QDataSet, int) 
     */
    public static QDataSet decimate( QDataSet ds ) {
        return decimate( ds, 10 );
    }
     
    /**
     * reduce the size of the data by keeping every nth measurement (subsample), starting
     * at the 0th measurement.
     * @param ds rank 1 or more dataset.
     * @param m the decimation factor, e.g. 2 is every other measurement.
     * @return 
     * 
     */
    public static QDataSet decimate( QDataSet ds, int m ) {
        if ( Schemes.isIrregularJoin(ds) ) {
            throw new IllegalArgumentException("not supported");
        }
        int newlen= ds.length()/m;
        int imax= newlen*m - m;
        return DataSetOps.applyIndex( ds, Ops.linspace( 0, imax, newlen) );
    }

    /**
     * BufferDataSets allow us to specify a stride so that subsampling can 
     * be done in constant time.
     * @param ds
     * @param m the decimation factor for the zeroth index, e.g. 2 is every other measurement.
     * @param n the decimation factor for the first index, e.g. 2 is every other measurement.
     * @return 
     */
    private static QDataSet decimateBufferDataSet( BufferDataSet ds, int m, int n ) {
        BufferDataSet result= BufferDataSet.copy(ds);
        
        QDataSet dep0= (QDataSet)ds.property(QDataSet.DEPEND_0);
        if ( dep0!=null && m>1 ) {
            dep0= decimate(dep0,m);
        }        
        QDataSet dep1= (QDataSet)ds.property(QDataSet.DEPEND_1);
        if ( dep1!=null ) {
            if ( dep1.rank()==1 ) {
                dep1= decimate(dep1,n);
            } else {
                dep1= decimate(dep1,m,n);
            }
        }
        result.setRecordStride( result.getRecordStride()*m );
        result.setLength( result.length()/m );
        result.setFieldStride( result.getFieldStride()*n );
        result.setLength1( result.length(0)/n );
        
        if ( dep0!=null ) result.putProperty( QDataSet.DEPEND_0, dep0 );
        if ( dep1!=null ) result.putProperty( QDataSet.DEPEND_1, dep1 );
            
        DataSetUtil.putProperties( DataSetUtil.getDimensionProperties( ds, null ), result );
        return result;
    }
    
    /**
     * reduce the size of the data by keeping every nth measurement (subsample). 
     * @param ds rank 2 or more dataset.
     * @param m the decimation factor for the zeroth index, e.g. 2 is every other measurement.
     * @param n the decimation factor for the first index, e.g. 2 is every other measurement.
     * @return new dataset which is ds.length()/m by ds.length(0)/n.
     * 
     */
    public static QDataSet decimate( QDataSet ds, int m, int n ) {
        if ( Schemes.isIrregularJoin(ds) ) {
            throw new IllegalArgumentException("not supported");
        }
        if ( ds instanceof BufferDataSet && ds.rank()==2 ) {
            return decimateBufferDataSet( (BufferDataSet)ds, m, n );
        }
        int newlen0= ds.length()/m;
        int max0= newlen0*m;
        int newlen1= ds.length(0)/n;
        int max1= newlen1*n;
        QubeDataSetIterator iter= new QubeDataSetIterator(ds);
        iter.setIndexIteratorFactory( 0, 
            new QubeDataSetIterator.StartStopStepIteratorFactory( 0, max0, m ) );
        iter.setIndexIteratorFactory( 1, 
            new QubeDataSetIterator.StartStopStepIteratorFactory( 0, max1, n ) );
        DDataSet result= iter.createEmptyDs();
        QubeDataSetIterator iterout=  new QubeDataSetIterator(result);
        while ( iter.hasNext() ) {
            iter.next();
            iterout.next();
            iterout.putValue( result, iter.getValue(ds) );
        }
        result.putProperty(QDataSet.RENDER_TYPE,ds.property(QDataSet.RENDER_TYPE));
        
        return result;
    }
    
    /**
     * this is introduced to mimic the in-line function which reduces the dimensionality by averaging over the zeroth dimension.
     *   collapse0( ds[30,20] ) &rarr; ds[20]
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
     *   collapse0( ds[30,20] ) &rarr; ds[20]
     * @param fillDs
     * @return the averaged dataset
     */
    public static QDataSet collapse0( QDataSet fillDs ) {
        if ( fillDs.rank()==4 ) {
            return collapse0R4(fillDs,new NullProgressMonitor());
        } else {
            fillDs= Ops.reduceMean(fillDs,0);
            return fillDs;
        }
    }
    
    /**
     * Collapse the rank 4 dataset on the zeroth index.
     * @see org.das2.qds.OperationsProcessor#sprocess(java.lang.String, org.das2.qds.QDataSet, org.das2.util.monitor.ProgressMonitor) 
     * @param ds rank 4 dataset
     * @param mon
     * @return rank 3 dataset
     */
    public static QDataSet collapse0R4( QDataSet ds, ProgressMonitor mon ) {
        if ( ds.rank()==4 ) {
            int[] qube = DataSetUtil.qubeDims(ds);
            if ( qube==null ) throw new IllegalArgumentException("dataset is not a qube");
            int[] newQube = DataSetOps.removeElement(qube, 0);
            //MutablePropertyDataSet mpds= DataSetOps.makePropertiesMutable(ds);
            //mpds.putProperty(QDataSet.BUNDLE_1,null);
            QDataSet mpds= ds;
            QDataSet wds = DataSetUtil.weightsDataSet(mpds);
            DDataSet result = DDataSet.create(newQube);
            DDataSet wresult= DDataSet.create(newQube);
            mon.setTaskSize(qube[1]);
            mon.started();
            for ( int i1= 0; i1<qube[1]; i1++ ) {
                mon.setTaskProgress(i1);
                for ( int i2= 0; i2<qube[2]; i2++ ) {
                    for ( int i3= 0; i3<qube[3]; i3++ ) {
                        for ( int i0=0; i0<qube[0]; i0++ ) {
                            double w= wds.value(i0,i1,i2,i3);
                            if ( w>0 ) {
                                result.addValue(i1, i2, i3, w*ds.value(i0,i1,i2,i3));
                                wresult.addValue(i1, i2, i3, w );
                            }
                        }
                    }
                }
            }
            double fill= Double.NaN;
            for ( int i0= 0; i0<qube[1]; i0++ ) {
                for ( int i1= 0; i1<qube[2]; i1++ ) {
                    for ( int i2= 0; i2<qube[3]; i2++ ) {
                        double w= wresult.value(i0, i1, i2 );
                        if ( w>0 ) {
                            double n= result.value(i0, i1, i2 );
                            result.putValue(i0, i1, i2, n/w);
                        } else {
                            result.putValue(i0, i1, i2, fill );
                        }
                    }
                }
            }
            mon.finished();
            Map<String,Object> props= DataSetUtil.getProperties(ds);
            props= DataSetOps.sliceProperties( props, 0 );
            DataSetUtil.putProperties( props, result );
            result.putProperty(QDataSet.WEIGHTS,wresult);
            return result;
            
        } else {
            throw new IllegalArgumentException("only rank 4");
        }
        
    }

    /**
     * Collapse the rank 4 dataset on the first index.
     * @see org.das2.qds.OperationsProcessor#sprocess(java.lang.String, org.das2.qds.QDataSet, org.das2.util.monitor.ProgressMonitor) 
     * @param ds rank 4 dataset
     * @param mon
     * @return rank 3 dataset
     */
    public static QDataSet collapse1R4( QDataSet ds, ProgressMonitor mon ) {
        if ( ds.rank()==4 ) {
            int[] qube = DataSetUtil.qubeDims(ds);
            if ( qube==null ) throw new IllegalArgumentException("dataset is not a qube");
            int[] newQube = DataSetOps.removeElement(qube, 1);
            //MutablePropertyDataSet mpds= DataSetOps.makePropertiesMutable(ds);
            //mpds.putProperty(QDataSet.BUNDLE_1,null);
            QDataSet mpds= ds;
            QDataSet wds = DataSetUtil.weightsDataSet(mpds);
            DDataSet result = DDataSet.create(newQube);
            DDataSet wresult= DDataSet.create(newQube);
            mon.setTaskSize(qube[0]);
            mon.started();
            for ( int i0= 0; i0<qube[0]; i0++ ) {
                mon.setTaskProgress(i0);
                for ( int i2= 0; i2<qube[2]; i2++ ) {
                    for ( int i3= 0; i3<qube[3]; i3++ ) {
                        for ( int i1=0; i1<qube[1]; i1++ ) {
                            double w= wds.value(i0,i1,i2,i3);
                            if ( w>0 ) {
                                result.addValue(i0, i2, i3, w*ds.value(i0,i1,i2,i3));
                                wresult.addValue(i0, i2, i3, w );
                            }
                        }
                    }
                }
            }
            double fill= Double.NaN;
            for ( int i0= 0; i0<qube[0]; i0++ ) {
                for ( int i2= 0; i2<qube[2]; i2++ ) {
                    for ( int i3= 0; i3<qube[3]; i3++ ) {
                        double w= wresult.value(i0, i2, i3 );
                        if ( w>0 ) {
                            double n= result.value(i0, i2, i3 );
                            result.putValue(i0, i2, i3, n/w);
                        } else {
                            result.putValue(i0, i2, i3, fill );
                        }
                    }
                }
            }
            mon.finished();
            Map<String,Object> props= DataSetUtil.getProperties(ds);
            props= DataSetOps.sliceProperties( props, 1 );
            DataSetUtil.putProperties( props, result );
            result.putProperty(QDataSet.WEIGHTS,wresult);
            return result;
            
        } else {
            throw new IllegalArgumentException("only rank 4");
        }
        
    }
    
    /**
     * this is introduced to mimic the in-line function which reduces the dimensionality by averaging over the first dimension
     *   collapse1( ds[30,20] ) &rarr; ds[30]
     * @param ds
     * @return the averaged dataset
     */
    public static QDataSet collapse1( QDataSet ds ) {
        if ( ds.rank()==4 ) {
            return collapse1R4(ds,new NullProgressMonitor());
        } else {
            ds= Ops.reduceMean(ds,1);
            return ds;
        }
    }

    /**
     * this is introduced to mimic the in-line function which reduces the dimensionality by averaging over the first dimension
     *   collapse2( ds[30,20,10,5] ) &rarr; ds[30,20,5]
     * @param fillDs
     * @return the averaged dataset
     */
    public static QDataSet collapse2( QDataSet fillDs ) {
        if ( fillDs.rank()==4 ) {
            return collapse2R4(fillDs,new NullProgressMonitor());
        } else {
            fillDs= Ops.reduceMean(fillDs,2);
            return fillDs;
        }
    }

    /**
     * Collapse the rank 4 dataset on the second index.
     * @see org.das2.qds.OperationsProcessor#sprocess(java.lang.String, org.das2.qds.QDataSet, org.das2.util.monitor.ProgressMonitor) 
     * @param ds rank 4 dataset
     * @param mon
     * @return rank 3 dataset
     */
    public static QDataSet collapse2R4( QDataSet ds, ProgressMonitor mon ) {
        if ( ds.rank()==4 ) {
            int[] qube = DataSetUtil.qubeDims(ds);
            if ( qube==null ) throw new IllegalArgumentException("dataset is not a qube");
            int[] newQube = DataSetOps.removeElement(qube, 2);
            //MutablePropertyDataSet mpds= DataSetOps.makePropertiesMutable(ds);
            //mpds.putProperty(QDataSet.BUNDLE_1,null);
            QDataSet mpds= ds;
            QDataSet wds = DataSetUtil.weightsDataSet(mpds);
            DDataSet result = DDataSet.create(newQube);
            DDataSet wresult= DDataSet.create(newQube);
            mon.setTaskSize(qube[0]);
            mon.started();
            for ( int i0= 0; i0<qube[0]; i0++ ) {
                mon.setTaskProgress(i0);
                for ( int i1= 0; i1<qube[1]; i1++ ) {
                    for ( int i3= 0; i3<qube[3]; i3++ ) {
                        for ( int i2=0; i2<qube[2]; i2++ ) {
                            double w= wds.value(i0,i1,i2,i3);
                            if ( w>0 ) {
                                result.addValue(i0, i1, i3, w*ds.value(i0,i1,i2,i3));
                                wresult.addValue(i0, i1, i3, w );
                            }
                        }
                    }
                }
            }
            double fill= Double.NaN;
            for ( int i0= 0; i0<qube[0]; i0++ ) {
                for ( int i1= 0; i1<qube[1]; i1++ ) {
                    for ( int i2= 0; i2<qube[3]; i2++ ) {
                        double w= wresult.value(i0, i1, i2 );
                        if ( w>0 ) {
                            double n= result.value(i0, i1, i2 );
                            result.putValue(i0, i1, i2, n/w);
                        } else {
                            result.putValue(i0, i1, i2, fill );
                        }
                    }
                }
            }
            mon.finished();
            Map<String,Object> props= DataSetUtil.getProperties(ds);
            props= DataSetOps.sliceProperties( props, 2 );
            DataSetUtil.putProperties( props, result );
            result.putProperty(QDataSet.WEIGHTS,wresult);
            return result;
            
        } else {
            throw new IllegalArgumentException("only rank 4");
        }
        
    }    
    
    /**
     * this is introduced to mimic the in-line function which reduces the dimensionality by averaging over the first dimension
     *   collapse3( ds[30,20,10,5] ) &rarr; ds[30,20,10]
     * @param fillDs
     * @return the averaged dataset
     */
    public static QDataSet collapse3( QDataSet fillDs ) {
        if ( fillDs.rank()==4 ) {
            return collapse3R4( fillDs, new NullProgressMonitor() );
        } else {
            fillDs= Ops.reduceMean(fillDs,3);
            return fillDs;
        }
    }
    
    /**
     * Collapse the rank 4 dataset on the third index.
     * @see org.das2.qds.OperationsProcessor#sprocess(java.lang.String, org.das2.qds.QDataSet, org.das2.util.monitor.ProgressMonitor) 
     * @param ds rank 4 dataset
     * @param mon
     * @return rank 3 dataset
     */
    public static QDataSet collapse3R4( QDataSet ds, ProgressMonitor mon ) {
        if ( ds.rank()==4 ) {
            int[] qube = DataSetUtil.qubeDims(ds);
            if ( qube==null ) throw new IllegalArgumentException("dataset is not a qube");
            int[] newQube = DataSetOps.removeElement(qube, 3);
            //MutablePropertyDataSet mpds= DataSetOps.makePropertiesMutable(ds);
            //mpds.putProperty(QDataSet.BUNDLE_1,null);
            QDataSet mpds= ds;
            QDataSet wds = DataSetUtil.weightsDataSet(mpds);
            DDataSet result = DDataSet.create(newQube);
            DDataSet wresult= DDataSet.create(newQube);
            mon.setTaskSize(qube[1]);
            mon.started();
            for ( int i0= 0; i0<qube[0]; i0++ ) {
                mon.setTaskProgress(i0);
                for ( int i1= 0; i1<qube[1]; i1++ ) {
                    for ( int i2= 0; i2<qube[2]; i2++ ) {
                        for ( int i3=0; i3<qube[3]; i3++ ) {
                            double w= wds.value(i0,i1,i2,i3);
                            if ( w>0 ) {
                                result.addValue(i0, i1, i2, w*ds.value(i0,i1,i2,i3));
                                wresult.addValue(i0, i1, i2, w );
                            }
                        }
                    }
                }
            }
            double fill= Double.NaN;
            for ( int i0= 0; i0<qube[0]; i0++ ) {
                for ( int i1= 0; i1<qube[1]; i1++ ) {
                    for ( int i2= 0; i2<qube[2]; i2++ ) {
                        double w= wresult.value(i0, i1, i2 );
                        if ( w>0 ) {
                            double n= result.value(i0, i1, i2 );
                            result.putValue(i0, i1, i2, n/w);
                        } else {
                            result.putValue(i0, i1, i2, fill );
                        }
                    }
                }
            }
            mon.finished();
            Map<String,Object> props= DataSetUtil.getProperties(ds);
            props= DataSetOps.sliceProperties( props, 2 );
            DataSetUtil.putProperties( props, result );
            result.putProperty(QDataSet.WEIGHTS,wresult);
            return result;
            
        } else {
            throw new IllegalArgumentException("only rank 4");
        }
        
    }        

    /**
     * trim the dataset to the indices on the zeroth dimension.  Note
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
     * return the trim of the dataset ds where its DEPEND_0 (typically xtags) are
     * within the range dr.
     * @param ds a rank 1 or greater dataset
     * @param dr a range in the same units as ds
     * @return the subset of the data.
     * @see #trim(org.das2.qds.QDataSet, org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet trim( QDataSet ds, DatumRange dr ) {
        return trim( ds, dataset( dr.min() ), dataset( dr.max() ) );
    }
    
    /**
     * return the trim of the dataset ds where its DEPEND_0 (typically xtags) are
     * within the range dr.
     * @param ds a rank 1 or greater dataset
     * @param odr an object which can be interpretted as a range.
     * @return the subset of the data.
     * @see #trim(org.das2.qds.QDataSet, org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet trim( QDataSet ds, Object odr ) {
        DatumRange dr= datumRange(odr);
        return trim( ds, dataset( dr.min() ), dataset( dr.max() ) );
    }

    /**
     * return the trim of the dataset ds where its DEPEND_0 (typically xtags) are
     * within the range dr.  For example,
     * if ds was 7-days from 2014-01-01 through 2014-01-07, and st=2014-01-02
     * and en=2014-01-03 then just the records collected on this one day would
     * be returned.  
     * @param ds the dataset to be trimmed, with a rank 1 monotonic DEPEND_0.
     * @param st rank 0 min value
     * @param en rank 0 max value
     * @return the subset of the data.
     * @see #slice0(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet trim( QDataSet ds, QDataSet st, QDataSet en ) {
        if ( st.rank()!=0 || en.rank()!=0 ) {
            if ( st.rank()==1 && st.length()==2 && en.rank()==1 && en.length()==2 ) {
                return SemanticOps.trim( ds, datumRange(st), datumRange(en) );
            }
            throw new IllegalArgumentException("start and end parameters must be both rank 0 or both rank 1");
        }
        if ( ds==null ) {
            throw new NullPointerException("ds is null");
        }
        if ( ds.length()==0 ) {
            return ds;
        }
        
        QDataSet dep0= SemanticOps.xtagsDataSet(ds);
        
        QDataSet dep0en= dep0;
        if ( dep0.rank()!=1 ) {
            if ( dep0.rank()==2 ) { // join of tags
                WritableDataSet dep0enc= Ops.copy( Ops.slice1( dep0,0 )  );
                dep0enc.putProperty( QDataSet.UNITS, SemanticOps.getUnits(dep0) ); // TODO: this should happen automatically
                WritableDataSet dep0stc= Ops.copy( dep0enc  );
                dep0stc.putProperty( QDataSet.UNITS, SemanticOps.getUnits(dep0) );
                for ( int i=0; i<dep0.length(); i++ ) {
                    QDataSet dep0slice= dep0.slice(i);
                    double d1= dep0slice.slice(dep0slice.length()-1).value();
                    double d2= dep0slice.slice(0).value();
                    if (d1>d2) {
                        double t= d1;
                        d1= d2;
                        d2=t;
                    }
                    dep0enc.putValue(i,d1);
                    dep0stc.putValue(i,d2);
                }
                dep0= dep0stc;
                dep0en= dep0enc;
                Ops.lt( dep0, dep0en );
            }
        }
        
        if ( dep0.length()<1 ) {
            return ds;
        } else if ( dep0.length()<2 ) {
            Datum startDatum= datum(st);
            Datum stopDatum= datum(en);
            Datum t= datum(dep0.slice(0));
            if ( startDatum.le(t) && t.le(stopDatum) ) {
                return ds;
            } else {
                return ds.trim(0, 0);
            }
        }
        
        QDataSet findex= Ops.findex( dep0, st );
        double f1= Math.ceil( findex.value() );
        findex= Ops.findex( dep0en, en );
        double f2= Math.ceil( findex.value() ); // f2 is exclusive.
        
        int n= dep0.length();
        f1= 0>f1 ? 0 : f1;
        f1= n<f1 ? n : f1;
        f2= 0>f2 ? 0 : f2;
        f2= n<f2 ? n : f2;
               
        if ( f1>f2 ) {
            if ( Ops.ge(st,en).value()>0 ) {
                throw new IllegalArgumentException("st must be less than (or earlier than) en");
            } else {
                return ds.trim((int)f1,(int)f1);
            }
        }
        return ds.trim((int)f1,(int)f2);
    }
    
    /**
     * trim the qube dataset on any of its indices, for example ds[:,:,5:10]
     * would use this operation.
     * @param dim the index (0, 1, 2, 3, or 4) on which to trim.
     * @param ds the dataset, which must be a qube.
     * @param st the first index, inclusive
     * @param en the last index, exclusive
     * @return the trimmed dataset with same number of dimensions and fewer indices in one dimension.
     */
    public static QDataSet trim( int dim, QDataSet ds, int st, int en ) {
        if ( dim==0 ) {
            return trim( ds, st, en );
        } else {
            TrimStrideWrapper tsw= new TrimStrideWrapper(ds);
            tsw.setTrim( dim, st, en, 1 );
            return tsw;
        }
    }
    
    /**
     * trim the qube dataset on any of its indices, for example ds[:,:,5:10]
     * would use this operation.
     * @param dim the index (0, 1, 2, 3, or 4) on which to trim.
     * @param ds the dataset, which must be a qube.
     * @param st rank 0 min value
     * @param en rank 0 max value
     * @return the trimmed dataset with same number of dimensions and fewer indices in one dimension.
     */
    public static QDataSet trim( int dim, QDataSet ds, QDataSet st, QDataSet en ) {
        if ( dim==0 ) {
            return trim( ds, st, en );
        } else {
            QDataSet dep= (QDataSet) ds.property( "DEPEND_"+dim );
            if ( dep.rank()==2 ) {
                JoinDataSet result= new JoinDataSet(ds.rank());
                for ( int i=0; i<ds.length(); i++ ) {
                    QDataSet sliceds= trim( dim-1, ds.slice(i), st, en );
                    result.join( sliceds );
                }
                DataSetUtil.copyDimensionProperties( ds, result );
                result.putProperty( QDataSet.DEPEND_0, ds.property(QDataSet.DEPEND_0 ));
                // DEPEND_1 is special, because it ends up attached to each of the joined datasets as DEPEND_0.
                JoinDataSet dep1= new JoinDataSet(2); //TODO: why doesn't this happen automatically?
                for ( int i=0; i<result.length(); i++ ) {
                    QDataSet sliceds= (QDataSet) result.slice(i).property(QDataSet.DEPEND_0);
                    dep1.join( sliceds );
                }
                result.putProperty( QDataSet.DEPEND_1, dep1 );
                result.property( QDataSet.JOIN_0 );
                return result;
            } else if ( dep.rank()!=1 ) {
                throw new IllegalArgumentException("depend rank must be 1 or 2 for trim");
            } else {
                QDataSet findex= Ops.findex( dep, st );
                double f1= Math.ceil( findex.value() );
                findex= Ops.findex( dep, en );
                double f2= Math.ceil( findex.value() ); // f2 is exclusive.
                
                int n= dep.length();
                f1= 0>f1 ? 0 : f1;
                f1= n<f1 ? n : f1;
                f2= 0>f2 ? 0 : f2;
                f2= n<f2 ? n : f2;            
                                
                TrimStrideWrapper tsw= new TrimStrideWrapper(ds);
                tsw.setTrim( dim, (int)f1, (int)f2, 1 );
                return tsw;
            }
        }
    }
    
    
    
    /**
     * return the trim of the dataset ds where its DEPEND_1 (typically ytags) are
     * within the range dr.  For example,
     * if ds was frequencies from 10 Hz to 1e8 Hz, trim1( ds, 100Hz, 1000Hz ) would
	 * return just the data in this range.
     * @param ds the dataset to be trimmed, with a rank 1 monotonic DEPEND_1.
     * @param st rank 0 min value
     * @param en rank 0 max value
     * @return the subset of the data.
     * @see #slice1(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet trim1( QDataSet ds, QDataSet st, QDataSet en ) {
        if ( st.rank()!=0 || en.rank()!=0 ) {
            throw new IllegalArgumentException("bounds must be rank 0");
        }
        if ( ds==null ) {
            throw new NullPointerException("ds is null");
        }
        QDataSet dep1= SemanticOps.ytagsDataSet(ds);
        if ( dep1.rank()!=1 ) {
            return trim( 1, ds, st, en);
        }
        
        QDataSet findex= Ops.findex( dep1, st );
        double f1= Math.ceil( findex.value() );
        findex= Ops.findex( dep1, en );
        double f2= Math.ceil( findex.value() ); // f2 is exclusive.
                
        int n= dep1.length();
        f1= 0>f1 ? 0 : f1;
        f1= n<f1 ? n : f1;
        f2= 0>f2 ? 0 : f2;
        f2= n<f2 ? n : f2;
        
        if ( f1>f2 ) throw new IllegalArgumentException("st must be less than (or earlier than) en");
        
        return Ops.trim1( ds, (int)f1,(int)f2 );
    }
	
    /**
     * trim on the first (not zeroth) dimension.  This is to help with 
     * unbundling the timeranges from an events dataset. 
     * @param ds the dataset, rank 2 or greater
     * @param st the first index
     * @param en the last index, exclusive.
     * @return the trimmed dataset.
     */
    public static QDataSet trim1( QDataSet ds, int st, int en ) {
        if ( ds.rank()==2 ) {
            QDataSet bundle1= (QDataSet) ds.property(QDataSet.BUNDLE_1);
            if ( bundle1!=null ) bundle1= bundle1.trim(st,en);
            ds= DataSetOps.leafTrim( ds, st, en );
            if ( bundle1!=null ) {
                ds= putProperty( ds, QDataSet.BUNDLE_1, bundle1 );
            }
            return ds;
        } else {
            TrimStrideWrapper tsw= new TrimStrideWrapper(ds);
            tsw.setTrim( 1, st, en, 1 );
            return tsw;
        }
    }
    
    /**
     * set the cadence property for the DEPEND_0 data.  This allows scripts to 
     * assert that the data is collected at a given rate, and connecting lines should
     * be drawn.
     * <pre>
     * xx= [ 0,2,4,6,8,10 ]
     * yy= [ 1,1,2,2,3,3 ]
     * ds= dataset(xx,yy)
     * ds= setDepend0Cadence( ds, '2' )
     * </pre>
     * @param ds the data, which has a DEPEND_0 property.
     * @param arg the string representing the cadence.
     * @return the data, which now has a DEPEND_0 with the cadence.
     * @throws ParseException 
     */
    public static QDataSet setDepend0Cadence( QDataSet ds, String arg ) throws ParseException {
        QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
        if ( dep0!=null ) {
            Map<String,Object> props= DataSetUtil.getDimensionProperties( ds,null );
            Units dep0units= SemanticOps.getUnits(dep0);
            QDataSet cadence = DataSetUtil.asDataSet( dep0units.getOffsetUnits().parse(arg) );
            MutablePropertyDataSet mdep0= Ops.putProperty( dep0, QDataSet.CADENCE, cadence );
            ds= Ops.putProperty( ds, QDataSet.DEPEND_0, mdep0 );
            DataSetUtil.putProperties( props,(MutablePropertyDataSet)ds );
        } else if ( SemanticOps.isJoin(ds) ) {
            JoinDataSet n= new JoinDataSet(ds.rank());
            Map<String,Object> props= DataSetUtil.getDimensionProperties( ds,null );
            for ( int ii=0; ii<ds.length(); ii++ ) {
                QDataSet fillDs1= ds.slice(ii);
                Map<String,Object> props1= DataSetUtil.getDimensionProperties( fillDs1,null );
                dep0= (QDataSet) fillDs1.property(QDataSet.DEPEND_0);
                Units dep0units= SemanticOps.getUnits(dep0);
                QDataSet cadence = DataSetUtil.asDataSet( dep0units.getOffsetUnits().parse(arg) );
                MutablePropertyDataSet mdep0= Ops.putProperty( dep0, QDataSet.CADENCE, cadence );
                fillDs1= Ops.putProperty( fillDs1, QDataSet.DEPEND_0, mdep0 );
                DataSetUtil.putProperties( props1,(MutablePropertyDataSet)fillDs1 );
                n.join(fillDs1);
            }
            ds= n;
            DataSetUtil.putProperties( props,(MutablePropertyDataSet)ds );
        }
        return ds;
    }
    
    /**
     * set the cadence property for the DEPEND_1 data.  This allows scripts to 
     * assert that the data is collected at a given rate, and connecting lines should
     * be drawn.
     * <pre>
     * xx= findgen(6)*2
     * yy= findgen(6)*'10Hz'
     * yy[3:]= yy[3:]+'2Hz'
     * zz= randn(6,6)
     * ds= dataset(xx,yy,zz)
     * ds= setDepend1Cadence( ds, '12Hz' )
     * plot( ds )
     * </pre>
     * @param ds the data, which has a DEPEND_1 property.
     * @param arg the string representing the cadence.
     * @return the data, which now has a DEPEND_1 with the cadence.
     * @throws ParseException 
     */    
    public static QDataSet setDepend1Cadence( QDataSet ds, String arg ) throws ParseException {
        Map<String,Object> props= DataSetUtil.getDimensionProperties( ds,null );
        ds= Ops.copy(ds);
        QDataSet dep1= (QDataSet) ds.property(QDataSet.DEPEND_1);
        if ( dep1!=null ) {
            Units dep1units= SemanticOps.getUnits(dep1);
            Datum news;
            try {
                news= dep1units.getOffsetUnits().parse(arg);
            } catch ( ParseException | InconvertibleUnitsException ex ) {
                news= DatumUtil.parse(arg);
            }

            MutablePropertyDataSet mdep0= Ops.putProperty( dep1, QDataSet.CADENCE, DataSetUtil.asDataSet( news ) );
            ds= Ops.putProperty( ds, QDataSet.DEPEND_1, mdep0 );
        } 
        DataSetUtil.putProperties( props,(MutablePropertyDataSet)ds );
        return ds;
    }
    
    /**
     * assert that the valid data is within the given range like "10 to 50000", where
     * the range is parsed using DatumRangeUtil.parseDatumRange.
     * @param ds the data
     * @param arg the formatted range
     * @return the data with the VALID_MIN and VALID_MAX properties set.
     * @throws java.text.ParseException if the string cannot be parsed.
     * @see DatumRangeUtil#parseDatumRange(java.lang.String) 
     */
    public static QDataSet setValidRange( QDataSet ds, String arg ) throws ParseException {
        Units u= SemanticOps.getUnits(ds);
        DatumRange d= DatumRangeUtil.parseDatumRange( arg, u );
        ds= Ops.putProperty( ds, QDataSet.VALID_MIN, d.min().doubleValue(u) );
        ds= Ops.putProperty( ds, QDataSet.VALID_MAX, d.max().doubleValue(u) );
        return ds;
    }
    
    /**
     * element-wise sqrt.  See Ops.pow to square a number.
     * @param ds the dataset
     * @return the square root of the dataset, which will contain NaN where the data is negative.
     * @see #pow(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet sqrt(QDataSet ds) {
        //MutablePropertyDataSet result= (MutablePropertyDataSet) pow(ds, 0.5);
        MutablePropertyDataSet result= applyUnaryOp(ds, (UnaryOp) (double d1) -> Math.sqrt(d1) );
        return result;
    }

    public static double sqrt( double ds1 ) {
        return Math.sqrt( ds1 );
    }

    public static QDataSet sqrt( Object ds1 ) {
        return sqrt( dataset(ds1) );
    }
    
    /**
     * Perform Butterworth filter for high pass or low pass.
     * @param in the rank 1 waveform
     * @param order the order of the filter (2,3,4)
     * @param f the frequency, e.g. Units.hertz.createDatum(10)
     * @param lowp true for low pass, false for high pass.
     * @return the dataset in the same form.
     */    
    public static QDataSet butterworth( QDataSet in, int order, Datum f, boolean lowp ) {
        Butterworth b= new Butterworth( in, order, f, lowp );
        return b.filter();
    }

    /**
     * Perform Butterworth filter for notch or band pass or band reject.
     * @param in the rank 1 waveform
     * @param order the order of the filter (2,3,4)
     * @param flow the lower band limit, e.g. Units.hertz.createDatum(10)
     * @param fhigh the higher band limit, e.g. Units.hertz.createDatum(20)
     * @param pass true for band pass, false for band reject.
     * @return the dataset in the same form.
     */
    public static QDataSet butterworth( QDataSet in, int order, Datum flow, Datum fhigh, boolean pass ) {
        Butterworth b= new Butterworth( in, order, flow, fhigh, pass );
        return b.filter();
    }
    
    /**
     * Perform Butterworth filter for high pass or low pass.
     * @param in the rank 2 waveform
     * @param order the order of the filter (2,3,4)
     * @param f the frequency, e.g. Units.hertz.createDatum(10)
     * @param lowp true for low pass, false for high pass.
     * @return the dataset in the same form.
     */    
    public static QDataSet butterworth1( QDataSet in, int order, Datum f, boolean lowp ) {
        if ( in.rank()==2 ) {
            throw new IllegalArgumentException("data must be rank 2");
        }
        JoinDataSet result= new JoinDataSet(2);
        for ( int i=0; i<in.length(); i++ ) {
            Butterworth b= new Butterworth( in.slice(i), order, f, lowp );
            result.join(b.filter());
        }
        DataSetUtil.putProperties( DataSetUtil.getProperties(in), result);
        return result;
    }

    /**
     * Perform Butterworth filter for notch or band pass or band reject.
     * @param in the rank 2 waveform
     * @param order the order of the filter (2,3,4)
     * @param flow the lower band limit, e.g. Units.hertz.createDatum(10)
     * @param fhigh the higher band limit, e.g. Units.hertz.createDatum(20)
     * @param pass true for band pass, false for band reject.
     * @return the dataset in the same form.
     */
    public static QDataSet butterworth1( QDataSet in, int order, Datum flow, Datum fhigh, boolean pass ) {
        if ( in.rank()==2 ) {
            throw new IllegalArgumentException("data must be rank 2");
        }
        JoinDataSet result= new JoinDataSet(2);
        for ( int i=0; i<in.length(); i++ ) {        
            Butterworth b= new Butterworth( in.slice(i), order, flow, fhigh, pass );
            result.join(b.filter());
        }
        DataSetUtil.putProperties( DataSetUtil.getProperties(in), result);
        return result;
    }
    
        
    
    /**
     * Solves each of a set of cubic equations of the form:
     * a*x^3 + b*x^2 + c*x + d = 0.
     * Takes a rank 2 dataset with each equation across the first dimension and
     * coefficients of each equation across the second.
     * @param coefficients Set of all coefficients.
     * @return Roots of each equation. Double.NaN is returned for complex roots.
     * @author Henry Livingston Wroblewski
     */
    public static QDataSet cubicRoot( QDataSet coefficients )
    {
        //check for correct dimension of input
        if( coefficients.rank() != 2 )
        {
            throw new IllegalArgumentException(
                    "Must pass rank 2 QDataSet to cubicRoot");
        }
        
        // check for correct size input
        if( coefficients.length(0) != 4 || !DataSetUtil.isQube(coefficients) )
        {
            throw new IllegalArgumentException(
                    "Each row must be length 4 for cubicRoot");
        }

        DDataSet result = DDataSet.createRank2(coefficients.length(), 3);
        
        for( int i = 0; i < coefficients.length(); ++i )
        {
            /* alternate method to check for correct size, is slightly faster
            if( coefficients.length(1) != 4 )
            {
                throw new IllegalArgumentException(
                        "Each row must be length 4 for cubicRoot");
            }*/
            
            double[] answers = cubicRoot( coefficients.value(i,0),
                                          coefficients.value(i,1),
                                          coefficients.value(i,2),
                                          coefficients.value(i,3) );
            
            result.putValue(i, 0, answers[0]);
            result.putValue(i, 1, answers[1]);
            result.putValue(i, 2, answers[2]);
        }
        
        return result;
    } //end method cubicRoot()
    
    /**
     * Enter the coefficients for a cubic of the form:
     * a*x^3 + b*x^2 + c*x + d = 0.
     * Based on the method described at http://www.1728.org/cubic2.htm.
     * @param a Coefficient of x^3.
     * @param b Coefficient of x^2.
     * @param c Coefficient of x.
     * @param d Constant.
     * @return Array containing 3 roots. NaN will be returned for imaginary
     * roots.
     * @author Henry Livingston Wroblewski
     */
    public static double[] cubicRoot(double a, double b, double c, double d)
            throws ArithmeticException
    {
        double[] result;
        
        //check for proper degree of polynomial
        if( a == 0.0 )
        {
            throw new IllegalArgumentException("Coefficient of x^3 cannot be " +
                    "0 for cubicRoot.");
        }
        
        double f = (3*c/a - b*b/(a*a))/3;
        double g = (2*b*b*b/(a*a*a) - 9*b*c/(a*a) + 27*d/a)/27;
        double h = g*g/4 + f*f*f/27;
        
        if( h > 0 ) //one real root
        {
            double r = -g/2 + Math.sqrt(h);
            double s = r > 0 ? Math.pow(r, 1.0/3.0) : - Math.pow(-r, 1.0/3.0);
            double t = -g/2 - Math.sqrt(h);
            double u = t > 0 ? Math.pow(t, 1.0/3.0) : -Math.pow(-t, 1.0/3.0);
            
            result = new double[3];
            result[0] = s + u - b/(3*a);
            result[1] = Double.NaN;
            result[2] = Double.NaN;
            //imaginary results
            //result[1] = -(s + u)/2 - b/(3*a) + i*(s-u)*Math.sqrt(3)/2;
            //result[2] = -(s + u)/2 - b/(3*a) - i*(s-u)*Math.sqrt(3)/2;
        }
        else if( f == 0 && g == 0 && h == 0 ) //3 repeated roots
        {
            result = new double[3];
            result[0] = result[1] = result[2] = - Math.pow(d/a, 1.0/3.0);
        }
        else if( h <= 0 ) //all real
        {
            double i = Math.sqrt(g*g/4 - h);
            double j = Math.pow(i, 1.0/3.0);
            double k = Math.acos(-g/(2*i));
            double l = -j;
            double m = Math.cos(k/3);
            double n = Math.sqrt(3)*Math.sin(k/3);
            double P = -b/(3*a);
            
            result = new double[3];
            result[0] = 2*j*Math.cos(k/3) - b/(3*a);
            result[1] = l*(m + n) + P;
            result[2] = l*(m - n) + P;
        }
        else
        {
            // we should never get here...
            throw new ArithmeticException("Undefined case in cubicRoot.");
        }

        return result;
    } //end method cubicRoot()
    

    /**
     * element-wise abs.  For vectors, this returns the length of each element.
     * Note Jython conflict needs to be resolved.  Note the result of this
     * will have dimensionless units, and see magnitude for the more abstract
     * operator.  
     * For ratio-type units (Stevens) like "kms", the unit is preserved.
     * @param ds1 the dataset
     * @return dataset with the same geometry
     * @see Ops#magnitude(org.das2.qds.QDataSet) magnitude(ds), which preserves the sign.
     */
    public static QDataSet abs(QDataSet ds1) {
        Units u= (Units) ds1.property(QDataSet.UNITS);
        if ( u!=null && u instanceof EnumerationUnits ) {
            throw new IllegalArgumentException("data is from an enumeration, and abs cannot be used.");
        }
        MutablePropertyDataSet result= 
                applyUnaryOp(ds1, (UnaryOp) (double d1) -> Math.abs(d1));
        result.putProperty( QDataSet.LABEL, maybeLabelUnaryOp( ds1, "abs") );
        if ( u!=null && UnitsUtil.isRatioMeasurement(u) ) {
            result.putProperty( QDataSet.UNITS, u );
        }
        return result;
    }

    /**
     * return the abs of the long, to support Jython properly.
     * @param x the long
     * @return abs of the long
     */
    public static long abs( long x ) {
        return Math.abs( x );
    }
    
    /**
     * return the abs of the double, to support Jython properly.
     * @param v the valye
     * @return abs of the value
     */
    public static double abs( double v ) {
        return Math.abs( v );
    }

    /**
     * promote the list, double, array etc to QDataSet before taking abs.
     * @param ds1 list, double, array, etc
     * @return the abs in a QDataSet
     */
    public static QDataSet abs( Object ds1 ) {
        return abs( dataset(ds1) );
    }
    

    /**
     * element-wise pow (** in FORTRAN, ^ in IDL) of two datasets with the compatible
     * geometry.
     * @param ds1 the base
     * @param pow the exponent
     * @return the value ds1**pow
     */
    public static QDataSet pow(QDataSet ds1, QDataSet pow) {
        Units units1= SemanticOps.getUnits(ds1);
        Units units2= SemanticOps.getUnits(pow);
        if ( UnitsUtil.isTimeLocation(units1) ) throw new IllegalArgumentException("ds1 is time location");
        if ( UnitsUtil.isTimeLocation(units2) ) throw new IllegalArgumentException("pow is time location");
        MutablePropertyDataSet result=  
                applyBinaryOp(ds1, pow, (BinaryOp) (double d1, double d2) -> Math.pow(d1, d2));
        result.putProperty( QDataSet.LABEL, maybeLabelBinaryOp( ds1, pow, "pow") );
        return result;
    }
    
    /**
     * for Jython, we define this because the doubles aren't coerced.  
     * Note that pow(2,4) returns a long, not an int like Python 2.6.5
     * @param x the base
     * @param y the exponent
     * @return the value x**y
     */
    public static long pow( long x, long y ) {
        return (long)Math.pow( x, y );
    }
    
    /**
     * for Jython, we define this because the doubles aren't coerced.  
     * Note that pow(2,4) returns a long, not an int like Python 2.6.5
     * @param x the base
     * @param y the exponent
     * @return the value x**y
     */
    public static double pow( double x, double y ) {
        return Math.pow( x,y );
    }
    
    /**
     * element-wise pow (** in FORTRAN, ^ in IDL) of two datasets with the same
     * geometry.
     * @param ds1 the base
     * @param pow the exponent
     * @return the value ds1**pow
     */
    public static QDataSet pow( Object ds1, Object pow ) {
        return pow( dataset(ds1), dataset(pow) );
    }
      
    /**
     * element-wise exponentiate e**x.
     * @param ds the dataset
     * @return dataset of the same geometry
     */
    public static QDataSet exp(QDataSet ds) {
        MutablePropertyDataSet result=  
                applyUnaryOp(ds, (UnaryOp) (double d1) -> Math.pow(Math.E, d1));
        result.putProperty( QDataSet.LABEL, maybeLabelUnaryOp( ds, "exp") );
        return result;
    }

    /**
     * Jython requires this be implemented 
     * @param d
     * @return e**d
     */
    public static double exp( double d ) {
        return Math.exp( d );
    }

    /**
     * convert array, list, double, etc to QDataSet and return exp(d)
     * @param ds1 requires this be implemented 
     * @return exp(ds1)
     */
    public static QDataSet exp( Object ds1 ) {
        return exp( dataset(ds1) );
    }
    
    /**
     * element-wise exponentiate 10**x.
     * @param ds
     * @return
     */
    public static QDataSet exp10(QDataSet ds) {
        MutablePropertyDataSet result=   
                applyUnaryOp(ds, (UnaryOp) (double d1) -> Math.pow(10, d1));
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
        MutablePropertyDataSet result=  
                applyUnaryOp(ds, (UnaryOp) (double d1) -> Math.log(d1));
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
        MutablePropertyDataSet result=  
                applyUnaryOp(ds, (UnaryOp) (double d1) -> Math.log10(d1));
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
     * return the unit that is the product of the two units.  Presently
     * this only works when one unit is dimensionless.
     * @param units1 e.g. Units.ergs
     * @param units2 e.g. Units.dimensionless
     * @return the product of the two units, e.g. Units.ergs
     */
    private static Units multiplyUnits( Units units1, Units units2 ) {
        Units resultUnits;
        if ( units1==Units.dimensionless && units2==Units.dimensionless ) {
            resultUnits= Units.dimensionless;
        } else if ( units2==Units.dimensionless && UnitsUtil.isRatioMeasurement(units1) ) {
            resultUnits= units1;
        } else if ( units1==Units.dimensionless && UnitsUtil.isRatioMeasurement(units2) ) {
            resultUnits= units2;
        } else {
            if ( !UnitsUtil.isRatioMeasurement(units1) ) {
                throw new IllegalArgumentException("ds1 units are not ratio scale units: "+units1);
            }
            if ( !UnitsUtil.isRatioMeasurement(units2) ) {
                throw new IllegalArgumentException("ds2 units are not ratio scale units: "+units2);
            }
            logger.fine("throwing out units until we improve the units library, both arguments have physical units");
            resultUnits= null;
        }
        return resultUnits;
    }
    
    /**
     * return true if the argument is complex, having a DEPEND with COORDINATE_FRAME 
     * equal to ComplexNumber.
     * @param ds1 a QDataSet possibly containing complex components.
     * @return true of the dataset is complex.
     * @see Schemes#isComplexNumbers(org.das2.qds.QDataSet) 
     */
    private static boolean checkComplexArgument( QDataSet ds1 ) {
        QDataSet dep= (QDataSet) ds1.property("DEPEND_"+(ds1.rank()-1));
        if ( dep==null ) return false;
        return QDataSet.VALUE_COORDINATE_FRAME_COMPLEX_NUMBER.equals(dep.property(QDataSet.COORDINATE_FRAME));
    }
    
    /**
     * element-wise multiply of two datasets with compatible geometry.
     * Presently, either ds1 or ds2 should be dimensionless.
     * TODO: units improvements.
     * @param ds1
     * @param ds2
     * @see #multiplyUnits
     * @return
     */
    public static QDataSet multiply(QDataSet ds1, QDataSet ds2) {
        Units units1= SemanticOps.getUnits(ds1);
        Units units2= SemanticOps.getUnits(ds2);
        Units resultUnits= multiplyUnits( units1, units2 );

        if ( checkComplexArgument(ds1) && checkComplexArgument(ds2) ) {
            logger.warning("multiply used with two complex arguments, perhaps complexMultiply was intended");
        }
        
        MutablePropertyDataSet result= 
                applyBinaryOp(ds1, ds2, (BinaryOp) (double d1, double d2) -> d1 * d2);
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
     * @param ds1 the numerator
     * @param ds2 the divisor
     * @return the ds1/ds2
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
        } else if ( units2.isConvertibleTo(units1) ) {
            resultUnits= Units.dimensionless;
            uc= units2.getConverter(units1);
        } else {
            if ( !UnitsUtil.isRatioMeasurement(units1) ) throw new IllegalArgumentException("ds1 units are not ratio scale units: "+units1);
            if ( !UnitsUtil.isRatioMeasurement(units2) ) throw new IllegalArgumentException("ds2 units are not ratio scale units: "+units2);

            if ( units1==Units.dimensionless ) {
                try {
                    resultUnits= UnitsUtil.getInverseUnit(units2);
                } catch ( IllegalArgumentException ex ) {
                    logger.info( String.format( "unable to invert ds2 units (%s), arguments have unequal units in divide (/)", units2 ) );
                    resultUnits= null;
                }
            } else {
                logger.info("throwing out units until we improve the units library, arguments have unequal units");
                resultUnits= null;
            }
            uc= UnitsConverter.IDENTITY;
        }

        MutablePropertyDataSet result= 
                applyBinaryOp(ds1, ds2, (BinaryOp) (double d1, double d2) -> d1 / uc.convert(d2));

        if ( resultUnits!=Units.dimensionless ) result.putProperty( QDataSet.UNITS, resultUnits );
        result.putProperty(QDataSet.LABEL, maybeLabelInfixOp( ds1, ds2, "/" ) );
        return result;
    }
    
    public static QDataSet divide( Object ds1, Object ds2 ) {
        return divide( dataset(ds1), dataset(ds2) );
    }


    /**
     * element-wise mod of two datasets with compatible geometry.
     * This should support Units.t2000 mod "24 hours" to get result in hours.
     * @param ds1 the numerator
     * @param ds2 the divisor 
     * @return the remainder after the division
     */
    public static QDataSet mod(QDataSet ds1, QDataSet ds2) {
        Units u1= SemanticOps.getUnits(ds1).getOffsetUnits();
        Units u= SemanticOps.getUnits(ds2);
        final UnitsConverter uc= u1.getConverter(u);
        final double base= 0;
        MutablePropertyDataSet result= 
                applyBinaryOp(ds1, ds2, (BinaryOp) (double d1, double d2) -> uc.convert(d1-base ) % d2);        
        result.putProperty( QDataSet.UNITS, u );
        return result;
    }

    public static QDataSet mod( Object ds1, Object ds2 ) {
        return mod( dataset(ds1), dataset(ds2) );
    }
    
    /**
     * element-wise mod of two datasets with compatible geometry.  This returns
     * a positive number for -18 % 10.  This is Python's behavior.
     * This should support Units.t2000 mod "24 hours" to get result in hours.
     * @param ds1 the numerator
     * @param ds2 the divisor
     * @return the remainder after the division
     */
    public static QDataSet modp(QDataSet ds1, QDataSet ds2) {
        Units u1= SemanticOps.getUnits(ds1).getOffsetUnits();
        Units u= SemanticOps.getUnits(ds2);
        final UnitsConverter uc= u1.getConverter(u);
        final double base= 0;
        MutablePropertyDataSet result= 
                applyBinaryOp(ds1, ds2, (BinaryOp) (double d1, double d2) -> {
            double t= uc.convert(d1-base ) % d2;
            return ( t<0 ) ? t+d2 : t;
        });
        result.putProperty( QDataSet.UNITS, u );
        return result;
    }

    public static QDataSet modp( Object ds1, Object ds2 ) {
        return modp( dataset(ds1), dataset(ds2) );
    }
      
    /**
     * This div goes with modp, where -18 divp 10 = -2 and -18 modp 10 = 8.
     * the div operator always goes towards zero, but divp always goes to
     * the more negative number so the remainder is positive.
     * @param ds1
     * @param ds2
     * @return
     */
    public static QDataSet divp(QDataSet ds1, QDataSet ds2) {
        Units u1= SemanticOps.getUnits(ds1);
        Units u= SemanticOps.getUnits(ds2);
        final UnitsConverter uc;
        UnitsConverter uc1;
        try {
            uc1= u1.getConverter(u);
        } catch ( IllegalArgumentException ex ) {
            uc1= UnitsConverter.IDENTITY;
        }
        uc= uc1;
        MutablePropertyDataSet result= applyBinaryOp(ds1, ds2, 
                (BinaryOp) (double d1, double d2) -> Math.floor( uc.convert(d1) / d2));
        Units resultUnits= uc==UnitsConverter.IDENTITY ? u1 : Units.dimensionless;
        if ( resultUnits!=null ) result.putProperty( QDataSet.UNITS, resultUnits );
        return result;
    }

    public static QDataSet divp( Object ds1, Object ds2 ) {
        return divp( dataset(ds1), dataset(ds2) );
    }

    /**
     * element-wise div of two datasets with compatible geometry.
     * @param ds1
     * @param ds2
     * @return
     */
    public static QDataSet div(QDataSet ds1, QDataSet ds2) {
        return applyBinaryOp(ds1, ds2, 
                (BinaryOp) (double d1, double d2) -> (int) (d1 / d2));
    }
    
    public static QDataSet div( Object ds1, Object ds2 ) {
        return div( dataset(ds1), dataset(ds2) );
    }

    
    // comparators
    /**
     * element-wise equality test.  1.0 is returned where the two datasets are
     * equal.  Fill is returned where either measurement is invalid.
     * @param ds1 rank n dataset
     * @param ds2 rank m dataset with compatible geometry.
     * @return rank n or m dataset.
     */
    public static QDataSet eq(QDataSet ds1, QDataSet ds2) {
        final UnitsConverter uc= SemanticOps.getLooseUnitsConverter( ds1, ds2 );
        return applyBinaryOp(ds1, ds2, 
                (BinaryOp) (double d1, double d2) -> uc.convert(d1) == d2 ? 1.0 : 0.0);
    }

    /**
     * if ds1 is enumeration, then check if o2 could be interpreted as 
     * such, otherwise return the existing interpretation.
     * @param ds1 null, or the context in which we interpret o2.
     * @param o2 the String, QDataSet, array, etc.
     * @param ds2 the fall-back when this is the correct interpretation.
     * @return 
     */
    private static QDataSet enumerationUnitsCheck( QDataSet ds1, Object o2, QDataSet ds2 ) {
        if ( ds1==null ) return ds2;
        Units u= SemanticOps.getUnits(ds1);
        if ( u instanceof EnumerationUnits ) {
            return Ops.dataset( o2, u );
        } else {
            return ds2;
        }
    }
    
    /**
     * element-wise equality test, converting arguments as necessary to
     * like units.  These datasets can be nominal data as well.
     * 
     * @param ds1
     * @param ds2
     * @return 
     */
    public static QDataSet eq( Object ds1, Object ds2 ) {
        QDataSet dds1,dds2;
        try {
            dds1= dataset(ds1);
        } catch ( IllegalArgumentException ex ) {
            dds1= null;
        }
        try {
            dds2= dataset(ds2);
        } catch ( IllegalArgumentException ex ) {
            dds2= null;
        }
        dds2= enumerationUnitsCheck( dds1, ds2, dds2 );
        dds1= enumerationUnitsCheck( dds2, ds1, dds1 );
        return eq( dds1, dds2 );
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
        return applyBinaryOp(ds1, ds2, 
                (BinaryOp) (double d1, double d2) -> uc.convert(d1) != d2 ? 1.0 : 0.0);
    }
    
    /**
     * element-wise equality test, converting arguments as necessary to
     * like units.  These datasets can be nominal data as well.
     * 
     * @param ds1
     * @param ds2
     * @return 
     */
    public static QDataSet ne( Object ds1, Object ds2 ) {
        QDataSet dds1,dds2;
        try {
            dds1= dataset(ds1);
        } catch ( IllegalArgumentException ex ) {
            dds1= null;
        }
        try {
            dds2= dataset(ds2);
        } catch ( IllegalArgumentException ex ) {
            dds2= null;
        }
        dds2= enumerationUnitsCheck( dds1, ds2, dds2 );
        dds1= enumerationUnitsCheck( dds2, ds1, dds1 );
        return ne( dds1, dds2 );
    }
    
    /**
     * element-wise function returns 1 where ds1&gt;ds2.
     * @param ds1
     * @param ds2
     * @return
     */
    public static QDataSet gt(QDataSet ds1, QDataSet ds2) {
        final UnitsConverter uc= SemanticOps.getLooseUnitsConverter( ds1, ds2 );
        return applyBinaryOp(ds1, ds2, 
                (BinaryOp) (double d1, double d2) -> uc.convert(d1) > d2 ? 1.0 : 0.0);
    }

    public static QDataSet gt( Object ds1, Object ds2 ) {
        return gt( dataset(ds1), dataset(ds2) );
    }
    
    /**
     * element-wise function returns the greater of ds1 and ds2.
     * If an element of ds1 or ds2 is fill, then the result is fill.
     * @param ds1
     * @param ds2
     * @return the bigger of the two, in the units of ds1.
     */
    public static QDataSet greaterOf(QDataSet ds1, QDataSet ds2) {
        final UnitsConverter uc= SemanticOps.getLooseUnitsConverter( ds2, ds1 );
        MutablePropertyDataSet mpds=  
                applyBinaryOp(ds1, ds2, (BinaryOp) (double d1, double d2) -> uc.convert(d2) > d1 ? d2 : d1);
        mpds.putProperty( QDataSet.UNITS, ds1.property(QDataSet.UNITS) );
        return mpds;
    }
    
    public static QDataSet greaterOf( Object ds1, Object ds2 ) {
        return greaterOf( dataset(ds1), dataset(ds2) );
    }
    
    /**
     * element-wise function returns the smaller of ds1 and ds2.
     * If an element of ds1 or ds2 is fill, then the result is fill.
     * @param ds1
     * @param ds2
     * @return the smaller of the two, in the units of ds1.
     */
    public static QDataSet lesserOf(QDataSet ds1, QDataSet ds2) {
        final UnitsConverter uc= SemanticOps.getLooseUnitsConverter( ds2, ds1 );
        MutablePropertyDataSet mpds=  
                applyBinaryOp(ds1, ds2, (BinaryOp) (double d1, double d2) -> uc.convert(d2) < d1 ? d2 : d1);
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
        return applyBinaryOp(ds1, ds2, 
                (BinaryOp) (double d1, double d2) -> uc.convert(d1) >= d2 ? 1.0 : 0.0);
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
        return applyBinaryOp(ds1, ds2, 
                (BinaryOp) (double d1, double d2) -> uc.convert(d1) < d2 ? 1.0 : 0.0);
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
        return applyBinaryOp(ds1, ds2, 
                (BinaryOp) (double d1, double d2) -> uc.convert(d1) <= d2 ? 1.0 : 0.0);
    }

    public static QDataSet le( Object ds1, Object ds2 ) {
        return le( dataset(ds1), dataset(ds2) );
    }
    
    // logic operators
    /**
     * element-wise logical or function.  
     * returns 1 where ds1 is non-zero or ds2 is non-zero.  Note
     * that or(fill,1) is currently fill, not 1, as are all binary
     * operators.
     * @param ds1
     * @param ds2
     * @return
     * @see #bitwiseOr(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet or(QDataSet ds1, QDataSet ds2) {
        return applyBinaryOp(ds1, ds2, 
                (BinaryOp) (double d1, double d2) -> d1 != 0 || d2 != 0 ? 1.0 : 0.0);
    }
    
    public static QDataSet or( Object ds1, Object ds2 ) {
        return or( dataset(ds1), dataset(ds2) );
    }


    /**
     * element-wise logical and function.  non-zero is true, zero is false.
     * @param ds1
     * @param ds2
     * @return
     * @see #bitwiseAnd(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet and(QDataSet ds1, QDataSet ds2) {
        return applyBinaryOp(ds1, ds2, 
                (BinaryOp) (double d1, double d2) -> d1 != 0 && d2 != 0 ? 1.0 : 0.0);
    }
    
    public static QDataSet and( Object ds1, Object ds2 ) {
        return and( dataset(ds1), dataset(ds2) );
    }
        

    /**
     * bitwise AND operator treats the data as (32-bit) integers, and 
     * returns the bit-wise AND.
     * @param ds1 
     * @param ds2
     * @return bit-wise AND.
     */
    public static QDataSet bitwiseAnd( QDataSet ds1, QDataSet ds2 ) {
        return applyBinaryOp(ds1, ds2, 
                (BinaryOp) (double d1, double d2) -> (long)d1 & (long)d2);
    }
    
    public static QDataSet bitwiseAnd( Object ds1, Object ds2 ) {
        return bitwiseAnd( dataset(ds1), dataset(ds2) );
    }    
    
    /**
     * bitwise OR operator treats the data as (32-bit) integers, and 
     * returns the bit-wise OR.
     * @param ds1 
     * @param ds2
     * @return bit-wise OR.
     */
    public static QDataSet bitwiseOr( QDataSet ds1, QDataSet ds2 ) {
        return applyBinaryOp(ds1, ds2, 
                (BinaryOp) (double d1, double d2) -> (long)d1 | (long)d2);
    }
    
    public static QDataSet bitwiseOr( Object ds1, Object ds2 ) {
        return bitwiseOr( dataset(ds1), dataset(ds2) );
    }
    
    /**
     * bitwise XOR (exclusive or) operator treats the data as (32-bit) integers, and 
     * returns the bit-wise XOR.  Note there is no bitwise not, and this is because
     * there are no shorts, bytes.  So to implement bitwise not for a 16 bit number
     * you would have bitwiseXor( ds, dataset(2**16-1) ).
     * @param ds1 
     * @param ds2
     * @return bit-wise XOR.
     * @see #not(org.das2.qds.QDataSet) 
     */
    public static QDataSet bitwiseXor( QDataSet ds1, QDataSet ds2 ) {
        return applyBinaryOp(ds1, ds2, 
                (BinaryOp) (double d1, double d2) -> (long)d1 ^ (long)d2);
    }
    
    public static QDataSet bitwiseXor( Object ds1, Object ds2 ) {
        return bitwiseXor( dataset(ds1), dataset(ds2) );
    }
    
    /**
     * element-wise logical not function.  non-zero is true, zero is false.
     * @param ds1
     * @return
     * @see #bitwiseXor(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet not(QDataSet ds1) {
        return applyUnaryOp(ds1, (UnaryOp) (double d1) -> d1 != 0 ? 0.0 : 1.0);
    }

    public static QDataSet not( Object ds1 ) {
        return not( dataset(ds1) );
    }
    
    /**
     * mimic the Jython xrange function for use in loops.  The returns
     * the integers in the sequence, like xrange, but avoids the confusing 
     * "xrange" name and this also takes double
     * values so that it can be used in graphics routines which use double.
     * @param min the first value
     * @param max the last value, plus one (exclusive).
     * @param step the increment between elements.
     * @return an iterator for the numbers in the interval.
     */
    public static Iterator<Integer> irange( double min, double max, int step ) {
        double imin= Math.floor(min);
        double imax= Math.floor(max);
        double istep= step;
        if ( istep % 1. != 0.0 ) throw new IllegalArgumentException("step must be an integer");
        int length= (int)( ( imax - imin ) / istep );
        final TagGenDataSet result= new TagGenDataSet( length, istep, imin );
        result.putProperty( QDataSet.FORMAT, "%d" );
        return new Iterator<Integer>() {
            int p= 0;
            @Override
            public boolean hasNext() {
                return p < result.length();
            }

            @Override
            public Integer next() {
                int i= (int)result.value(p);
                p++;
                return i;
            }
        };
        
    }

    /**
     * mimic the Jython xrange function for use in loops.  This creates
     * an dataset which has no storage.
     * @param min the first value
     * @param max the last value, plus one (exclusive).
     * @return an iterator for the numbers in the interval.
     */
    public static Iterator<Integer> irange( double min, double max ) {
        return irange( min, max, 1 );
    }
    
    /**
     * mimic the Jython xrange function for use in loops.  This creates
     * an dataset which has no storage.
     * @param max the last value, plus one (exclusive).
     * @return an iterator for the numbers in the interval.
     */
    public static Iterator<Integer> irange( double max ) {
        return irange( 0, max, 1 );
    }    

    // IDL,Matlab - inspired routines
    /**
     * returns rank 1 dataset with values [0,1,2,...]
     * This returns an immutable dataset, so that it can
     * be used in Jython like so: for i in indgen(200000).  Note before
     * February 2018, this would return a mutable dataset, and now
     * this returns an IndexGenDataSet, which is immutable.
     *
     * @param len0
     * @return 
     */
    public static QDataSet indgen(int len0) {
        return new IndexGenDataSet(len0);
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
     * return a QDataSet containing empty strings.  This is used in Jython to create
     * an array-like dataset where different strings are assigned after the array
     * is made.
     * @param len0 the number of elements.
     * @return QDataSet with EnumerationUnits and all elements containing the value for "".
     */
    public static QDataSet strarr( int len0 ) {
        String context= "default";
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
        IDataSet result = IDataSet.createRank1(len0);
        Datum d = u.createDatum("");
        double dval= d.doubleValue(u);
        for (int i = 0; i < len0; i++) {            
            result.putValue(i, dval);
        }
        result.putProperty(QDataSet.UNITS, u);
        return result;

    }
    
    /**
     * return a rank 2 QDataSet containing empty strings.  This is used in Jython to create
     * an array-like dataset where different strings are assigned after the array
     * is made.
     * @param len0 the number of elements in the first index.
     * @param len1 the number of elements in the second index.
     * @return QDataSet with EnumerationUnits and all elements containing the value for "".
     * @see #dblarr(int) 
     */
    public static QDataSet strarr( int len0, int len1 ) {
        String context= "default";
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
        IDataSet result = IDataSet.createRank2(len0,len1);
        Datum d = u.createDatum("");
        double dval= d.doubleValue(u);
        for (int i=0; i<len0; i++) {            
            for ( int j=0; j<len1; j++ ) {
                result.putValue(i, j, dval);
            }
        }
        result.putProperty(QDataSet.UNITS, u);
        return result;

    }
    
    /**
     * create a dataset filled with zeros, stored in 4-byte floats.
     * @param len0 the zeroth dimension length
     * @return rank 1 dataset filled with zeros.
     * @see #zeros(int) 
     * @see #dblarr(int) 
     * @see #strarr(int) 
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
     * create a dataset filled with zeros, stored in unsigned bytes.  Each
     * element can contain numbers from 0 to 255.
     * @param len0 the zeroth dimension length
     * @return rank 1 dataset filled with zeros.
     * @see #dblarr(int) 
     */
    public static QDataSet bytarr(int len0) {
        return BufferDataSet.createRank1( BufferDataSet.UBYTE, len0 );
    }

    /**
     * create a rank 2 dataset filled with zeros, stored in unsigned bytes.
     * @param len0 the length of the zeroth dimension.
     * @param len1 the length of the first dimension.
     * @return rank 2 dataset filled with zeros.
     */
    public static QDataSet bytarr(int len0, int len1) {
        return BufferDataSet.createRank2( BufferDataSet.UBYTE, len0, len1 );
    }

    /**
     * create a rank 3 dataset filled with zeros, stored in unsigned bytes.
     * @param len0 the length of the zeroth dimension.
     * @param len1 the length of the first dimension.
     * @param len2 the length of the second dimension.
     * @return rank 3 dataset filled with zeros.
     */
    public static QDataSet bytarr(int len0, int len1, int len2) {
        return BufferDataSet.createRank3( BufferDataSet.UBYTE, len0, len1, len2 );
    }
    
    /**
     * create a dataset filled with zeros, stored in 2-byte signed shorts.
     * Note that shortarr is equivalent to intarr in IDL, containing integer
     * values from -32768 to 32767.
     * @param len0 the zeroth dimension length
     * @return rank 1 dataset filled with zeros.
     * @see #zeros(int) 
     * @see #dblarr(int) 
     */
    public static QDataSet shortarr(int len0) {
        return Ops.replicate((short)0, len0);
    }

    /**
     * create a rank 2 dataset filled with zeros, stored in 2-byte shorts.
     * @param len0 the length of the zeroth dimension.
     * @param len1 the length of the first dimension.
     * @return rank 2 dataset filled with zeros.
     */
    public static QDataSet shortarr(int len0, int len1) {
        return Ops.replicate((short)0, len0, len1);
    }

    /**
     * create a rank 3 dataset filled with zeros, stored in 2-byte shorts.
     * @param len0 the length of the zeroth dimension.
     * @param len1 the length of the first dimension.
     * @param len2 the length of the second dimension.
     * @return rank 3 dataset filled with zeros.
     */
    public static QDataSet shortarr(int len0, int len1, int len2) {
        return Ops.replicate((short)0, len0, len1, len2);
    }
    
    /**
     * create a dataset filled with zeros, stored in 4-byte ints.  
     * @param len0 the zeroth dimension length
     * @return rank 1 dataset filled with zeros.
     * @see #zeros(int) 
     * @see #dblarr(int) 
     */
    public static QDataSet intarr(int len0) {
        return Ops.replicate(0, len0);
    }

    /**
     * create a rank 2 dataset filled with zeros, stored in 4-byte ints.
     * @param len0 the length of the zeroth dimension.
     * @param len1 the length of the first dimension.
     * @return rank 2 dataset filled with zeros.
     */
    public static QDataSet intarr(int len0, int len1) {
        return Ops.replicate(0, len0, len1);
    }

    /**
     * create a rank 3 dataset filled with zeros, stored in 4-byte ints.
     * @param len0 the length of the zeroth dimension.
     * @param len1 the length of the first dimension.
     * @param len2 the length of the second dimension.
     * @return rank 3 dataset filled with zeros.
     */
    public static QDataSet intarr(int len0, int len1, int len2) {
        return Ops.replicate(0, len0, len1, len2);
    }
    
    /**
     * create a dataset filled with zeros, stored in 8-byte longs, suitable for
     * storing cdf_tt2000 times..  
     * @param len0 the zeroth dimension length
     * @return rank 1 dataset filled with zeros.
     * @see #zeros(int) 
     * @see #dblarr(int) 
     */
    public static QDataSet lonarr(int len0) {
        return Ops.replicate(0L, len0);
    }

    /**
     * create a rank 2 dataset filled with zeros, stored in 8-byte longs.
     * @param len0 the length of the zeroth dimension.
     * @param len1 the length of the first dimension.
     * @return rank 2 dataset filled with zeros.
     */
    public static QDataSet lonarr(int len0, int len1) {
        return Ops.replicate(0L, len0, len1);
    }
        
    /**
     * create a rank 1 dataset filled with zeros, stored in 8-byte doubles.
     * @param len0 the length of the zeroth dimension.
     * @return rank 1 dataset filled with zeros.
     * @see #zeros(int) 
     * @see #fltarr(int) 
     * @see #bytarr(int)
     * @see #shortarr(int) 
     * @see #intarr(int) 
     * @see #lonarr(int) 
     */
    public static QDataSet dblarr(int len0) {
        return Ops.replicate(0., len0);
    }

    /**
     * create a rank 2 dataset filled with zeros, stored in 8-byte doubles.
     * @param len0 the length of the zeroth dimension.
     * @param len1 the length of the first dimension.
     * @return rank 2 dataset filled with zeros.
     * @see #zeros(int) 
     * @see #fltarr(int) 
     */   
    public static QDataSet dblarr(int len0, int len1) {     
        return Ops.replicate(0., len0, len1);
    }

    /**
     * create a rank 3 dataset filled with zeros, stored in 8-byte doubles.
     * @param len0 the length of the zeroth dimension.
     * @param len1 the length of the first dimension.
     * @param len2 the length of the second dimension.
     * @return rank 3 dataset filled with zeros.
     * @see #zeros(int) 
     * @see #fltarr(int) 
     */   
    public static QDataSet dblarr(int len0, int len1, int len2) {
        return Ops.replicate(0., len0, len1, len2);
    }

    /**
     * returns rank 1 dataset with values that are times.
     * @param baseTime e.g. "2003-02-04T00:00"
     * @param cadence e.g. "4.3 sec" "1 day"
     * @param len0 the number of elements.
     * @throws ParseException
     * @return
     */
    public static QDataSet timegen(String baseTime, String cadence, int len0) throws ParseException {
        double base = TimeUtil.create(baseTime).doubleValue(Units.us2000);
        String[] ss= cadence.split(" ");
        
        Datum cad= null;
        if ( ss.length==2 ) {
            try {
                Units u= Units.lookupUnits(ss[1]); 
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
     * @param years the years. (2010) Less than 100 is interpreted as 19xx.  
     * @param mons the months (1..12), or null.  If null, then days are day of year.  These are interpreted as integers
     * @param days the day of month (1..28) or day of year.  This may be fractional.
     * @param hour null or the hours of the day.
     * @param minute null or the minutes of the day
     * @param second null or the seconds of the day
     * @param nano null or the nanoseconds (1e-9) of the day
     * @return rank 1 time data set.
     */
    public static QDataSet toTimeDataSet( QDataSet years, QDataSet mons, QDataSet days, QDataSet hour, QDataSet minute, QDataSet second, QDataSet nano ) {
        
        QDataSet[] operands= new QDataSet[2];
        
        for ( int i=0; i<2; i++ ) { // two passes.
            if ( mons!=null ) {
                CoerceUtil.coerce( years, mons, true, operands );
                years= operands[0];
                mons= operands[1];
            }
            CoerceUtil.coerce( years, days, true, operands );
            years= operands[0];
            days= operands[1];
            if ( hour!=null ) {
                CoerceUtil.coerce( years, hour, true, operands );
                years= operands[0];
                hour= operands[1];
            }
            if ( minute!=null ) {
                CoerceUtil.coerce( years, minute, true, operands );
                years= operands[0];
                minute= operands[1];
            }
            if ( second!=null ) {
                CoerceUtil.coerce( years, second, true, operands );
                years= operands[0];
                second= operands[1];
            }            
            if ( nano!=null ) {
                CoerceUtil.coerce( years, nano, true, operands );
                years= operands[0];
                nano= operands[1];
            }     
        }
        WritableDataSet result= CoerceUtil.coerce( years, days, true, operands );
        
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
        QDataSet zeros= ConstantDataSet.create( 0., DataSetUtil.qubeDims(years) );
        if ( hour==null ) hour= zeros;
        if ( minute==null ) minute= zeros;
        if ( second==null ) second= zeros;
        if ( nano==null ) nano= zeros;

        if ( years.rank()!=1 ) throw new IllegalArgumentException("years must be rank 1");
        if ( mons.rank()!=1 ) throw new IllegalArgumentException("months must be rank 1 or null");
        if ( days.rank()!=1 ) throw new IllegalArgumentException("days must be rank 1 or null");
        if ( hour.rank()!=1 ) throw new IllegalArgumentException("hours must be rank 1 or null");
        if ( minute.rank()!=1 ) throw new IllegalArgumentException("minutes must be rank 1 or null");
        if ( second.rank()!=1 ) throw new IllegalArgumentException("seconds must be rank 1 or null");
        if ( nano.rank()!=1 ) throw new IllegalArgumentException("nanos must be rank 1 or null");
        
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
     * @param base
     * @param dcadence
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
     * @param min double
     * @param max double
     * @param len0 number of elements in the result
     * @return rank 1 dataset of linearly spaced data.
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
     * return a rank 1 dataset with <tt>len0</tt> linearly-spaced values, the first
     * is min and the last is max. 
     * @param omin rank 0 dataset
     * @param omax rank 0 dataset
     * @param len0 number of elements in the result
     * @return rank 1 dataset of linearly spaced data.
     */
    public static QDataSet linspace( Object omin, Object omax, int len0) {
        QDataSet dsmin= dataset(omin);
        QDataSet dsmax= dataset(omax);
        Units u= SemanticOps.getUnits(dsmin);
        double min= dsmin.value();
        double max= convertUnitsTo( dsmax, u ).value();
        QDataSet result= linspace( min, max, len0 );
        return putProperty( result, QDataSet.UNITS, u );
    }
    
    /**
     * return a rank 1 dataset with <tt>len0</tt> logarithmically-spaced values, the first
     * is min and the last is max. 
     * @param min double
     * @param max double
     * @param len0 number of elements in the result
     * @return rank 1 dataset of logarithmically spaced data.
     */
    public static QDataSet logspace(double min, double max, int len0) {
        if (len0 < 1) {
            return DDataSet.wrap(new double[]{max});
        } else {
            return pow( 10, linspace( Math.log10(min), Math.log10(max), len0 ) );
        }
    }
    
    /**
     * return a rank 1 dataset with <tt>len0</tt> logarithmically-spaced values, the first
     * is min and the last is max. 
     * @param omin rank 0 dataset
     * @param omax rank 0 dataset
     * @param len0 number of elements in the result
     * @return rank 1 dataset of logarithmically spaced data.
     */
    public static QDataSet logspace( Object omin, Object omax, int len0) {
        QDataSet dsmin= dataset(omin);
        QDataSet dsmax= dataset(omax);
        Units u= SemanticOps.getUnits(dsmin);
        double min= dsmin.value();
        double max= convertUnitsTo( dsmax, u ).value();
        QDataSet result= pow( 10, linspace( Math.log10(min), Math.log10(max), len0 ) );
        return putProperty( result, QDataSet.UNITS, u );
    }
    
    /**
     * returns rank 1 dataset with value
     * @param val fill the dataset with this value.
     * @param len0
     * @return
     */
    public static WritableDataSet replicate(short val, int len0) {
        int size = len0;
        short[] back = new short[size];
        for (int i = 0; i < size; i++) {
            back[i] = val;
        }
        return SDataSet.wrap( back, 1, len0, 1, 1, 1 );
    }

    /**
     * returns rank 2 dataset filled with value
     * @param val fill the dataset with this value.
     * @param len0
     * @param len1
     * @return
     */
    public static WritableDataSet replicate(short val, int len0, int len1) {
        int size = len0 * len1;
        short[] back = new short[size];
        for (int i = 0; i < size; i++) {
            back[i] = val;
        }
        return SDataSet.wrap(back, 2, len0, len1, 1, 1 );
    }
    
    /**
     * returns rank 3 dataset with filled with value.
     * @param val fill the dataset with this value.
     * @param len0
     * @param len1
     * @param len2
     * @return
     */
    public static WritableDataSet replicate(short val, int len0, int len1, int len2) {
        int size = len0 * len1 * len2;
        short[] back = new short[size];
        for (int i = 0; i < size; i++) {
            back[i] = val;
        }
        return SDataSet.wrap(back, 3, len0, len1, len2, 1);
    }
    
    
    /**
     * returns rank 1 dataset with value
     * @param val fill the dataset with this value.
     * @param len0
     * @return
     */
    public static WritableDataSet replicate(int val, int len0) {
        int size = len0;
        int[] back = new int[size];
        for (int i = 0; i < size; i++) {
            back[i] = val;
        }
        return IDataSet.wrap( back, 1, len0, 1, 1, 1 );
    }

    /**
     * returns rank 2 dataset filled with value
     * @param val fill the dataset with this value.
     * @param len0
     * @param len1
     * @return
     */
    public static WritableDataSet replicate(int val, int len0, int len1) {
        int size = len0 * len1;
        int[] back = new int[size];
        for (int i = 0; i < size; i++) {
            back[i] = val;
        }
        return IDataSet.wrap(back, 2, len0, len1, 1, 1 );
    }
    
    /**
     * returns rank 3 dataset with filled with value.
     * @param val fill the dataset with this value.
     * @param len0
     * @param len1
     * @param len2
     * @return
     */
    public static WritableDataSet replicate(int val, int len0, int len1, int len2) {
        int size = len0 * len1 * len2;
        int[] back = new int[size];
        for (int i = 0; i < size; i++) {
            back[i] = val;
        }
        return IDataSet.wrap(back, 3, len0, len1, len2, 1);
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
     * returns rank 4 dataset with filled with value.
     * @param val fill the dataset with this value.
     * @param len0
     * @param len1
     * @param len2
     * @param len3
     * @return 
     */
    public static WritableDataSet replicate(double val, int len0, int len1, int len2, int len3 ) {
        int size = len0 * len1 * len2 * len3;
        double[] back = new double[size];
        for (int i = 0; i < size; i++) {
            back[i] = val;
        }
        return DDataSet.wrap(back, 4, len0, len1, len2, len3 );
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
     * returns a rank N+1 dataset by repeating the rank N dataset, so
     * all records will have the same value. E.g. result.value(i,j)= val.value(j)
     * @param val the rank N dataset
     * @param len0 the number of times to repeat
     * @return rank N+1 dataset.
     * TODO: support rank N
     */
    public static MutablePropertyDataSet replicate( final QDataSet val, final int len0 ) {
        return new ReplicateDataSet( val, len0 );
    }
    
    /**
     * returns a rank N+2 dataset by repeating the rank N dataset, so
     * all records will have the same value. E.g. result.value(i,j)= val.value()
     * @param val the rank N dataset
     * @param len0 the number of times to repeat
     * @param len1 the length of the second index.
     * @return rank N+2 dataset.
     */
    public static MutablePropertyDataSet replicate( final QDataSet val, final int len0, final int len1 ) {
        return new ReplicateDataSet( new ReplicateDataSet( val, len1 ), len0 );
    }
    
    /**
     * return new dataset filled with zeros.
     * @param len0
     * @return
     * @see #fltarr(int) fltarr, which stores the data in 4-byte floats.
     */
    public static WritableDataSet zeros(int len0) {
        return replicate(0.0, len0);
    }

    /**
     * return new dataset filled with zeros.
     * @param len0
     * @param len1
     * @return
     */
    public static WritableDataSet zeros(int len0, int len1) {
        return replicate(0.0, len0, len1);
    }

    /**
     * return new dataset filled with zeros.
     * @param len0
     * @param len1
     * @param len2
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
     * @param len0 the length of the first index.
     * @return dataset filled with ones.
     */
    public static QDataSet ones(int len0) {
        return replicate(1.0, len0);
    }

    /**
     * return a rank two dataset filled with ones.  This is currently mutable, but future versions may not be.
     * @param len0 the length of the first index.
     * @param len1 the length of the second index.
     * @return dataset filled with ones.
     */
    public static QDataSet ones( final int len0, final int len1) {
        boolean returnMutableCopy= true; // for demonstration purposes, see http://sourceforge.net/p/autoplot/bugs/1148/
        if ( returnMutableCopy ) {
            return replicate(1.0, len0, len1);
        } else {
            return new AbstractDataSet() {
                @Override
                public int rank() {
                    return 2;
                }
                @Override
                public int length() {
                    return len0;
                }
                @Override
                public int length(int i0) {
                    return len1;
                }
                @Override
                public double value(int i0,int i1) {
                    return 1.;
                }
            };
        }
    }

    /**
     * return new dataset filled with ones.
     * @param len0 the length of the first index.
     * @param len1 the length of the second index.
     * @param len2 the length of the third index.
     * @return dataset filled with ones.
     */
    public static QDataSet ones(int len0, int len1, int len2) {
        return replicate(1.0, len0, len1, len2);
    }

    /**
     * return new dataset filled with ones.
     * @param len0 the length of the first index.
     * @param len1 the length of the second index.
     * @param len2 the length of the third index.
     * @param len3 the length of the fourth index.
     * @return dataset filled with ones.
     */
    public static QDataSet ones(int len0, int len1, int len2, int len3 ) {
        return replicate(1.0, len0, len1, len2, len3 );
    }
    
    /**
     * concatenates the two datasets together, appending the datasets on the zeroth dimension.
     * The two datasets must be QUBES have similar geometry on the higher dimensions.
     * If one of the datasets is rank 0 and the geometry of the other is rank 1, then
     * the lower rank dataset is promoted before appending.  If the first dataset
     * is null and the second is non-null, then return the second dataset.
     *
     * @param ds1 null or a dataset of length m.
     * @param ds2 dataset of length n to be concatenated.
     * @return a dataset length m+n.
     * @throws IllegalArgumentException if the two datasets don't have the same rank.
     * @see #merge(org.das2.qds.QDataSet, org.das2.qds.QDataSet) merge(ds1,ds2), which will interleave to preserve monotonic.
     * @see #append(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     * @deprecated use append instead.
     */
    public static QDataSet concatenate(QDataSet ds1, QDataSet ds2) {
        return append( ds1, ds2 );
    }
    
    public static QDataSet concatenate( Object ds1, Object ds2 ) {
        return append( dataset(ds1), dataset(ds2) );
    }  
    
    /**
     * return returns a rank N dataset of uniform numbers from [0,1].
     * @param qube the dimensions of the result.
     * @return the result
     */
    private static QDataSet randu(int[] qube, Random rand) {
        DDataSet result = DDataSet.create(qube);
        QubeDataSetIterator it = new QubeDataSetIterator(result);
        while (it.hasNext()) {
            it.next();
            it.putValue(result, rand.nextDouble());
        }
        return result;
    }

    /**
     * return returns a rank N dataset of random numbers of a Gaussian (normal) distribution.
     * @param qube the dimensions of the result.
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
     * returns a rank 0 dataset of random uniform numbers from 0 to 1 but not including 1.
     * @return a rank 0 dataset of random uniform numbers from 0 to 1 but not including 1.
     * @deprecated use randu instead
     */
    public static QDataSet rand() {
        return randu( );
    }
    
    /**
     * returns a rank 1 dataset of random uniform numbers from 0 to 1 but not including 1.
     * @param len0 the number of elements in the result.
     * @return a rank 1 dataset of random uniform numbers from 0 to 1 but not including 1.
     * @deprecated use randu instead.  This is used in many test programs and Jython codes, and will not be removed.
     */
    public static QDataSet rand(int len0) {
        return randu( len0 );
    }

    /**
     * returns a rank 2 dataset of random uniform numbers from 0 to 1 but not including 1.
     * @param len0 the number of elements in the first index.
     * @param len1 the number of elements in the second index.
     * @return a rank 2 dataset of random uniform numbers from 0 to 1 but not including 1.
     * @deprecated use randu instead
     */    
    public static QDataSet rand(int len0, int len1) {
        return randu( len0, len1 );
    }

    /**
     * returns a rank 3 dataset of random uniform numbers from 0 to 1 but not including 1.
     * @param len0 the number of elements in the first index.
     * @param len1 the number of elements in the second index.
     * @param len2 the number of elements in the third index.
     * @return a rank 3 dataset of random uniform numbers from 0 to 1 but not including 1.
     * @deprecated use randu instead
     */
    public static QDataSet rand(int len0, int len1, int len2) {
        return randu( len0, len1, len2 );
    }

    /**
     * returns a rank 0 dataset of random uniform numbers from 0 to 1 but not including 1.
     * @return a rank 0 dataset of random uniform numbers from 0 to 1 but not including 1.
     */
    public static QDataSet randu() {
        return randu(new int[]{}, random );
    }
    
    /**
     * returns a rank 1 dataset of random uniform numbers from 0 to 1 but not including 1.
     * @param len0 the number of elements in the result.
     * @return a rank 1 dataset of random uniform numbers from 0 to 1 but not including 1.
     */
    public static QDataSet randu(int len0) {
        return randu(new int[]{len0}, random );
    }

    /**
     * returns a rank 2 dataset of random uniform numbers from 0 to 1 but not including 1.
     * @param len0 the number of elements in the first index.
     * @param len1 the number of elements in the second index.
     * @return a rank 2 dataset of random uniform numbers from 0 to 1 but not including 1.
     */    
    public static QDataSet randu(int len0, int len1) {
        return randu(new int[]{len0, len1}, random );
    }

    /**
     * returns a rank 3 dataset of random uniform numbers from 0 to 1 but not including 1.
     * @param len0 the number of elements in the first index.
     * @param len1 the number of elements in the second index.
     * @param len2 the number of elements in the third index.
     * @return a rank 3 dataset of random uniform numbers from 0 to 1 but not including 1.
     */
    public static QDataSet randu(int len0, int len1, int len2) {
        return randu(new int[]{len0, len1, len2}, random );
    }
    

    /**
     * return a rank 4 dataset of random uniform numbers from 0 to 1 but not including 1.
     * @param len0 the number of elements in the first index.
     * @param len1 the number of elements in the second index.
     * @param len2 the number of elements in the third index.
     * @param len3 the number of elements in the fourth index.
     * @return a rank 4 dataset of random uniform numbers from 0 to 1 but not including 1.
     */
    public static QDataSet randu(int len0, int len1, int len2, int len3) {
        return randu(new int[]{len0, len1, len2, len3}, random );
    }
    
    /**
     * return a rank 0 dataset of random numbers of a Gaussian (normal) distribution.
     * @return a rank 0 dataset of random numbers of a Gaussian (normal) distribution.
     */
    public static QDataSet randn() {
        return randn(new int[]{}, random );
    }
    
    /**
     * return a rank 1 dataset of random numbers of a Gaussian (normal) distribution.
     * @param len0 the number of elements in the first index.
     * @return a rank 1 dataset of random numbers of a Gaussian (normal) distribution.
     */
    public static QDataSet randn(int len0) {
        return randn(new int[]{len0}, random );
    }

    /**
     * return a rank 2 dataset of random numbers of a Gaussian (normal) distribution.
     * @param len0 the number of elements in the first index.
     * @param len1 the number of elements in the second index.
     * @return a rank 2 dataset of random numbers of a Gaussian (normal) distribution.
     */
    public static QDataSet randn(int len0, int len1) {
        return randn(new int[]{len0, len1}, random );
    }

    /**
     * return a rank 3 dataset of random numbers of a Gaussian (normal) distribution.
     * @param len0 the number of elements in the first index.
     * @param len1 the number of elements in the second index.
     * @param len2 the number of elements in the third index.
     * @return a rank 3 dataset of random numbers of a Gaussian (normal) distribution.
     */
    public static QDataSet randn(int len0, int len1, int len2) {
        return randn(new int[]{len0, len1, len2}, random );
    }

    /**
     * return a rank 4 dataset of random numbers of a Gaussian (normal) distribution.
     * @param len0 the number of elements in the first index.
     * @param len1 the number of elements in the second index.
     * @param len2 the number of elements in the third index.
     * @param len3 the number of elements in the fourth index.
     * @return a rank 4 dataset of random numbers of a Gaussian (normal) distribution.
     */
    public static QDataSet randn(int len0, int len1, int len2, int len3) {
        return randn(new int[]{len0, len1, len2, len3}, random );
    }
    
    private static Random random= new Random();
    
    /**
     * restart the random sequence used by randu and randn.  Note if there
     * if there are multiple threads using random functions, this becomes 
     * unpredictable.
     * @return the seed is returned.
     */
    public static long randomSeed() {
        long seed;
        try {
            seed= java.security.SecureRandom.getInstance("SHA1PRNG").nextLong(); //findbugs  DMI_RANDOM_USED_ONLY_ONCE suggests this.
        } catch ( NoSuchAlgorithmException ex ) {
            seed= 0;
        }
        random= new Random(seed);
        return seed;
    }
    
    /**
     * reset the random sequence used by randu and randn to the given seed.
     * @param seed the new seed for the sequence.
     * @return the seed (which will be the same as the input).
     */
    public static long randomSeed( long seed ) {
        random= new Random(seed);
        return seed;
    }
    
    /**
     * returns a rank 0 dataset of random numbers of a Gaussian (normal) distribution.
     * System.currentTimeMillis() may be used for the seed.  Note this is unlike
     * the IDL randomn function because the seed is not modified.  (Any long parameter in Jython
     * and Java is read-only.)
     * System.currentTimeMillis() may be used for the seed.
     * @param seed basis for the random number (which will not be modified).
     * @return rank 0 dataset
     */
    public static QDataSet randomn(long seed) {
        double[] back = randomnBack(seed,1);
        return DDataSet.wrap( back, 0, 1, 1, 1);
    }
    
    /**
     * returns a rank 1 dataset of random numbers of a Gaussian (normal) distribution.
     * System.currentTimeMillis() may be used for the seed.
     * @param seed basis for the random number (which will not be modified).
     * @param len0 number of elements in the first index
     * @return rank 1 dataset of normal distribution
     */
    public static QDataSet randomn(long seed, int len0) {
        double[] back = randomnBack(seed, len0);
        return DDataSet.wrap(back, 1, len0, 1, 1);
    }

    /**
     * returns a rank 2 dataset of random numbers of a Gaussian (normal) distribution.
     * @param seed basis for the random number (which will not be modified).
     * @param len0 number of elements in the first index
     * @param len1 number of elements in the second index
     * @return rank 2 dataset of normal distribution
     */
    public static QDataSet randomn(long seed, int len0, int len1) {
        double[] back = randomnBack(seed, len0 * len1 );
        return DDataSet.wrap(back, 2, len0, len1, 1);
    }

    /**
     * returns a rank 3 dataset of random numbers of a gaussian (normal) distribution.
     * @param seed basis for the random number (which will not be modified).
     * @param len0 number of elements in the first index
     * @param len1 number of elements in the second index
     * @param len2 number of elements in the third index
     * @return rank 3 dataset of normal distribution
     */
    public static QDataSet randomn(long seed, int len0, int len1, int len2) {
        double[] back = randomnBack(seed, len0 * len1 * len2 );
        return DDataSet.wrap(back, 3, len0, len1, len2);
    }

    /**
     * returns a rank 3 dataset of random numbers of a gaussian (normal) distribution.
     * @param seed basis for the random number (which will not be modified).
     * @param len0 number of elements in the first index
     * @param len1 number of elements in the second index
     * @param len2 number of elements in the third index
     * @param len3 number of elements in the fourth index
     * @return rank 4 dataset of normal distribution
     */
    public static QDataSet randomn(long seed, int len0, int len1, int len2, int len3 ) {
        double[] back = randomnBack(seed, len0 * len1 * len2 );
        return DDataSet.wrap(back, 4, len0, len1, len2, len3 );
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
     * @param seed basis for the random number (which will not be modified).
     * @return a rank 0 dataset of random uniform numbers from 0 to 1 but not including 1.
     */
    public static QDataSet randomu(long seed) {
        double[] back = randomuBack(seed, 1);
        return DDataSet.wrap(back, 0, 1, 1, 1);
    }
    
    /**
     * returns a rank 1 dataset of random numbers of a uniform distribution.
     * System.currentTimeMillis() may be used for the seed.
     * @param seed basis for the random number (which will not be modified).
     * @param len0 number of elements in the first index
     * @return a rank 0 dataset of random uniform numbers from 0 to 1 but not including 1.
     */
    public static QDataSet randomu(long seed, int len0) {
        double[] back = randomuBack(seed, len0);
        return DDataSet.wrap(back, 1, len0, 1, 1);
    }

    /**
     * returns a rank 2 dataset of random numbers of a uniform distribution.
     * @param seed basis for the random number (which will not be modified).
     * @param len0 number of elements in the first index
     * @param len1 number of elements in the second index
     * @return a rank 0 dataset of random uniform numbers from 0 to 1 but not including 1.
     */
    public static QDataSet randomu(long seed, int len0, int len1) {
        double[] back = randomuBack(seed, len0 * len1 );
        return DDataSet.wrap(back, 2, len0, len1, 1);
    }

    /**
     * returns a rank 3 dataset of random numbers of a uniform distribution.
     * @param seed basis for the random number (which will not be modified).
     * @param len0 number of elements in the first index
     * @param len1 number of elements in the second index
     * @param len2 number of elements in the third index
     * @return a rank 0 dataset of random uniform numbers from 0 to 1 but not including 1.
     */
    public static QDataSet randomu(long seed, int len0, int len1, int len2) {
        double[] back = randomuBack(seed, len0 * len1 * len2 );
        return DDataSet.wrap(back, 3, len0, len1, len2);
    }
    
    /**
     * returns a rank 3 dataset of random numbers of a uniform distribution.
     * @param seed basis for the random number (which will not be modified).
     * @param len0 number of elements in the first index
     * @param len1 number of elements in the second index
     * @param len2 number of elements in the third index
     * @param len3 number of elements in the fourth index
     * @return rank 4 dataset of random uniform numbers from 0 to 1 but not including 1.
     */
    public static QDataSet randomu(long seed, int len0, int len1, int len2, int len3 ) {
        double[] back = randomuBack(seed, len0 * len1 * len2 );
        return DDataSet.wrap(back, 4, len0, len1, len2, len3 );
    }

    /**
     * return a table of distances d[len0] to the indices c0; in units of r0.
     * This is motivated by a need for more interesting datasets for testing.
     * @param len0 the length of the dataset
     * @param c0 the center point 0
     * @param r0 the units to normalize in the 0 direction
     * @return rank 2 table 
     */
    public static QDataSet distance( int len0, double c0, double r0 ) {
        DDataSet result= DDataSet.createRank1(len0);
        for ( int i=0; i<len0; i++ ) {
            double r= Math.abs(i-c0)/r0;
            result.putValue( i, r );
        }
        return result;
    }
    
    /**
     * return a table of distances d[len0,len1] to the indices c0,c1; in units of r0, r1.
     * This is motivated by a need for more interesting datasets for testing.
     * @param len0 the length of the dataset
     * @param len1 the length of each row of the dataset
     * @param c0 the center point 0
     * @param c1 the center point 1
     * @param r0 the units to normalize in the 0 direction
     * @param r1 the units to normalize in the 1 direction
     * @return rank 2 table 
     */
    public static QDataSet distance( int len0, int len1, double c0, double c1, double r0, double r1 ) {
        DDataSet result= DDataSet.createRank2(len0, len1);
        for ( int i=0; i<len0; i++ ) {
            for ( int j=0; j<len1; j++ ) {
                double r= Math.sqrt( Math.pow((i-c0)/r0,2) + Math.pow((j-c1)/r1,2) );
                result.putValue( i, j, r );
            }
        }
        return result;
    }
    
    /**
     * rank 1 dataset for demos and testing.
     * @param len0 number of elements in the first index
     * @return rank 1 dataset for demos and testing.
     */
    public static QDataSet ripples( int len0 ) {
        return new RipplesDataSet( len0 );
    }

    /**
     * rank 2 dataset for demos and testing.
     * @param len0 number of elements in the first index
     * @param len1 number of elements in the second index
     * @return rank 2 dataset for demos and testing.
     */
    public static QDataSet ripples( int len0, int len1 ) {
        return new RipplesDataSet( len0, len1 );
    }

    /**
     * rank 3 dataset for demos and testing.
     * @param len0 number of elements in the first index
     * @param len1 number of elements in the second index
     * @param len2 number of elements in the third index
     * @return rank 3 dataset for demos and testing.
     */
    public static QDataSet ripples( int len0, int len1, int len2 ) {
        FDataSet result= org.das2.qds.FDataSet.createRank3( len0, len1, len2 );
        for ( int i=0; i<len0; i++ ) {
            double eps= 1+(i/(float)len0);
            double eps2= 1+(i*5/(float)len0);
            QDataSet d2= new RipplesDataSet( (len1*eps)/10., len2/10., (len2*eps2)/20., (len1*eps)/2., len2/2., (len2*eps)/10., len1, len2 );
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
     * rank 4 dataset for demos and testing.
     * @param len0 number of elements in the first index
     * @param len1 number of elements in the second index
     * @param len2 number of elements in the third index
     * @param len3 number of elements in the fourth index
     * @return rank 4 dataset for demos and testing.
     */
    public static QDataSet ripples( int len0, int len1, int len2, int len3 ) {
        FDataSet result= org.das2.qds.FDataSet.createRank4( len0, len1, len2, len3 );
        Random r= new java.util.Random(0);
        for ( int j=0; j<len3; j++ ) {
            double d= r.nextDouble();
            for ( int i=0; i<len0; i++ ) {
                double eps= 1+(i/(float)len0);
                double eps2= 1+(i*5/(float)len0);
                QDataSet d2= new RipplesDataSet( (len1*eps)/10., len2/10., (len2*eps2)/20., (len1*eps)/2., len2/2., (len2*eps)/10., len1, len2 );
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
     * @param len number of records
     * @return fake rank 1 data timeseries for testing
     */
    public static QDataSet ripplesTimeSeries( int len ) {
        QDataSet rip= ripples( len,100 );
        ArrayDataSet result= ArrayDataSet.copy( DataSetOps.slice1(rip,20) );
        MutablePropertyDataSet t;
        try {
            t = (MutablePropertyDataSet) Ops.timegen("2011-10-24", String.format( Locale.US, "%f sec", 86400. / len), len);
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
     * @return rank 2 waveform
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
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return rip;
    }

    /**
     * return fake position data for testing.
     * result is rank 2 bundle [len,3]
     * @param len number of records
     * @return vector time series.
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
            t = (MutablePropertyDataSet) Ops.timegen("2011-10-24", String.format( Locale.US, "%f sec", 86400. / len), len);
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
     * @param len the number of records
     * @return fake position data for testing
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
            t = (MutablePropertyDataSet) Ops.timegen("2011-10-24", String.format( Locale.US, "%f sec", 86400. / len), len);
            t.putProperty( QDataSet.NAME, "Epoch" );
            result.putProperty(QDataSet.DEPEND_0,t);
            return result;
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }

    }


    /**
     * return fake position data for testing
     * result is rank 3 bundle [3,len/3,27*]
     * @param len the total number of records.
     * @return an example join spectrogram time series.
     * @see Schemes#irregularJoin() 
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
            t = (MutablePropertyDataSet)Ops.timegen("2011-10-24", String.format( Locale.US, "%f sec", 86400. / len3), len3);
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

            t = (MutablePropertyDataSet)Ops.timegen("2011-10-25", String.format( Locale.US, "%f sec", 86400. / len3), len3);
            //t = (MutablePropertyDataSet)Ops.timegen("2011-10-24T23:01", String.format("%f sec", 86400. / len3), len3);
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

            t = (MutablePropertyDataSet)Ops.timegen("2011-10-26", String.format( Locale.US, "%f sec", 86400. / lenr), lenr);
            //t = (MutablePropertyDataSet)Ops.timegen("2011-10-25T20:00", String.format("%f sec", 86400. / lenr), lenr);
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
     * @return an example pitch angle distribution.
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
     * generates a sawtooth from the tags, where a peak occurs with a period 2*PI.
     * All values of T should be ge zero.  TODO: I think there should be a modp 
     * function that is always positive. (-93 % 10 &rarr;7 though...)
     * @param t the independent values
     * @return /|/|/| sawtooth wave with a period of 2 PI.
     */
    public static QDataSet sawtooth( QDataSet t ) {
        QDataSet modt= divide( modp( t, DataSetUtil.asDataSet(TAU) ), TAU );
        return link( t, modt );
    }
    
    /**
     * generates a square from the tags, where a the signal is 1 from 0-PI, 0 from PI-2*PI, etc.
     * @param t the independent values.
     * @return square wave with a period of 2 PI.
     */
    public static QDataSet square( QDataSet t ) {
        QDataSet modt= lt( modp( t, DataSetUtil.asDataSet(TAU) ), PI );
        return link( t, modt );
    }
    
    /**
     * provide explicit method for appending two events scheme datasets.  This will probably be 
     * deprecated, and this was added at 17:30 for a particular need.
     * @param ev1
     * @param ev2
     * @return 
     */
    public static QDataSet appendEvents( QDataSet ev1, QDataSet ev2 ) {
        QDataSet result= ev1;
        for ( int i=0; i<ev2.length(); i++ ) {
            String sval=  ev2.slice(i).slice(3).toString();
            int k= sval.indexOf("=");
            sval= sval.substring(k+1);
            result= createEvent( result, DataSetUtil.asDatumRange( ev2.slice(i).trim(0,2) ),(int) ev2.value(i,2), sval );
        }
        return result;
    }
    
    /**
     * tool for creating ad-hoc events datasets.
     * @param timeRange a timerange like "2010-01-01" or "2010-01-01/2010-01-10" or "2010-01-01 through 2010-01-09"
     * @param rgbcolor and RGB color like 0xFF0000 (red), 0x00FF00 (green), or 0x0000FF (blue),
     * @param annotation label for event, possibly including granny codes.
     * @return a rank 2 QDataSet with [[ startTime, stopTime, rgbColor, annotation  ]]
     * @see #eventsComplement(org.das2.qds.QDataSet, int, java.lang.String) 
     */
    public static QDataSet createEvent( String timeRange, int rgbcolor, String annotation ) {
        return createEvent( null, timeRange, rgbcolor, annotation );
    }

    /**
     * tool for creating ad-hoc events datasets.
     * @param append null or a dataset to append the result.  This events dataset must have [starttime, endtime, RBG color, string] for each record.
     * @param timeRange a timerange like "2010-01-01" or "2010-01-01/2010-01-10" or "2010-01-01 through 2010-01-09"
     * @param rgbcolor an RGB color like 0xFF0000 (red), 0x00FF00 (green), or 0x0000FF (blue).  
     * @param annotation label for event, possibly including granny codes.
     * @return a rank 2 QDataSet with [[ startTime, stopTime, rgbColor, annotation  ]]
     */
    public static QDataSet createEvent( QDataSet append, String timeRange, int rgbcolor, String annotation ) {
        DatumRange dr= DatumRangeUtil.parseTimeRangeValid(timeRange);        
        return createEvent( append, dr, rgbcolor, annotation );
    }
    
    /**
     * tool for creating ad-hoc events datasets.
     * @param dr a datum range like "1496 to 1627" 
     * @param rgbcolor and RGB color like 0xFF0000 (red), 0x00FF00 (green), or 0x0000FF (blue),
     * @param annotation label for event, possibly including granny codes.
     * @return a rank 2 QDataSet with [[ start, stop, rgbColor, annotation  ]]
     * @see #eventsComplement(org.das2.qds.QDataSet, int, java.lang.String) 
     */
    public static QDataSet createEvent( DatumRange dr, int rgbcolor, String annotation ) {
        return createEvent( null, dr, rgbcolor, annotation );
    }
    
    /**
     * tool for creating ad-hoc events datasets.  For example
     * @param append null or a dataset to append the result.  This events dataset must have [starttime, endtime, RBG color, string] for each record.
     * @param dr a datum range
     * @param rgbcolor an RGB color like 0xFF0000 (red), 0x00FF00 (green), or 0x0000FF (blue)
     * @param annotation label for event, possibly including granny codes.
     * @return a rank 2 QDataSet with [[ startTime, stopTime, rgbColor, annotation  ]]
     */
    public static QDataSet createEvent( QDataSet append, DatumRange dr, int rgbcolor, String annotation ) {
        
        Units tu;
        
        EnumerationUnits evu;
        
        MutablePropertyDataSet bds=null;
        if ( append!=null ) {
            bds= (MutablePropertyDataSet) append.property( QDataSet.BUNDLE_1 );
            if ( bds==null ) throw new IllegalArgumentException("append argument must be the output of createEvent");
            evu= (EnumerationUnits) bds.property(QDataSet.UNITS,3);
            tu= (Units)bds.property(QDataSet.UNITS,0);
            if ( bds.property(QDataSet.UNITS,1)!=tu ) {
                throw new IllegalArgumentException("first two columns must be time locations");
            }
        } else {
            evu= EnumerationUnits.create("createEvent");
            tu= dr.getUnits();
        }
        
        DataSetBuilder dsb= new DataSetBuilder(2,100,4);
        
        dsb.putValue( -1, 0, dr.min().doubleValue(tu) );
        dsb.putValue( -1, 1, dr.max().doubleValue(tu) );
        dsb.putValue( -1, 2, rgbcolor );
        dsb.putValue( -1, 3, evu.createDatum(annotation).doubleValue(evu) );
        dsb.nextRecord();
        
        MutablePropertyDataSet ds= dsb.getDataSet();
        
        if ( bds==null ) {
            bds= DDataSet.createRank2( 4, 0 );
            bds.putProperty( "NAME__0", "Time" );
            bds.putProperty( "UNITS__0", tu );
            bds.putProperty( "NAME__1", "StopTime" );
            bds.putProperty( "UNITS__1", tu );
            bds.putProperty( "NAME__2", "Color" );
            bds.putProperty( "FORMAT__2", "0x%06x" ); // format as hex
            bds.putProperty( "NAME__3", "Event" );
            bds.putProperty( "UNITS__3", evu );
        } 
            
        ds.putProperty( QDataSet.BUNDLE_1, bds );
        
        append= append( append, ds );
        
        ((MutablePropertyDataSet)append).putProperty(QDataSet.RENDER_TYPE,QDataSet.VALUE_RENDER_TYPE_EVENTS_BAR);
        return append;

    }
    
    /**
     * make canonical rank 2 bundle dataset of min,max,color,text
     * This was originally part of EventsRenderer, but it became
     * clear that this was generally useful.
     * @param vds dataset in a number of forms that can be converted to an events dataset.
     * @return rank 2 QDataSet [ index; 4( time, stopTime, rgbColor, label ) ]
     */
    public static QDataSet createEvents( QDataSet vds ) {
        return createEvents( vds, Color.GRAY );
    }
    
    /**
     * make canonical rank 2 bundle dataset of min,max,color,text
     * This was originally part of EventsRenderer, but it became
     * clear that this was generally useful.
     * @param vds dataset in a number of forms that can be converted to an events dataset.
     * @param deftColor the color to use as the default color.
     * @return rank 2 QDataSet [ index; time, stopTime, rgbColor, label ]
     */
    public static QDataSet createEvents( QDataSet vds, Color deftColor ) {
    
        QDataSet xmins;
        QDataSet xmaxs;
        QDataSet colors;
        QDataSet msgs;

        if ( vds==null ) {
            return null;
        }
        
        int defaultColor= deftColor.getRGB();
        
        switch (vds.rank()) {
            case 2:
                {
                    QDataSet dep0= (QDataSet) vds.property(QDataSet.DEPEND_0);
                    if ( dep0==null ) {
                        xmins= DataSetOps.unbundle( vds,0 );
                        xmaxs= DataSetOps.unbundle( vds,1 );
                        
                        if ( vds.length(0)>3 ) {
                            colors= DataSetOps.unbundle( vds,2 );
                        } else {
                            colors= Ops.replicate( defaultColor, xmins.length() );
                        }
                        
                    } else if ( dep0.rank()==2 ) {
                        if ( SemanticOps.isBins(dep0) ) {
                            xmins= DataSetOps.slice1( dep0, 0 );
                            xmaxs= DataSetOps.slice1( dep0, 1 );
                            colors= Ops.replicate( 0x808080, xmins.length() );
                            Units u0= SemanticOps.getUnits(xmins );
                            Units u1= SemanticOps.getUnits(xmaxs );
                            if ( !u1.isConvertibleTo(u0) && u1.isConvertibleTo(u0.getOffsetUnits()) ) {
                                xmaxs= Ops.add( xmins, xmaxs );
                            }
                        } else {
                            throw new IllegalArgumentException( "DEPEND_0 is rank 2 but not bins" );
                        }
                        
                    } else  if ( dep0.rank() == 1 ) {
                        Datum width= SemanticOps.guessXTagWidth( dep0, null ).divide(2);
                        xmins= Ops.subtract(dep0, org.das2.qds.DataSetUtil.asDataSet(width) );
                        xmaxs= Ops.add(dep0, org.das2.qds.DataSetUtil.asDataSet(width) );
                        colors= Ops.replicate( defaultColor, xmins.length() );
                        
                    } else {
                        throw new IllegalArgumentException( "rank 2 dataset must have dep0 of rank 1 or rank 2 bins" );
                    }       
                    if ( vds.length(0)==2 ) {
                        msgs= Ops.replicate( dataset( EnumerationUnits.create("default").createDatum("_") ), vds.length() );
                    } else {
                        msgs= DataSetOps.unbundle( vds, vds.length(0)-1 );
                    }
                    
                    break;
                }
            case 1:
                {
                    QDataSet dep0= (QDataSet) vds.property(QDataSet.DEPEND_0);
                    if ( dep0==null ) {
                        if ( UnitsUtil.isTimeLocation(SemanticOps.getUnits(vds)) ) {
                            dep0= vds;
                        } 
                    }
                    if ( dep0==null ) {
                        throw new IllegalArgumentException("cannot make events data set from this rank 1 dataset with no timetags.");
                    } else if ( dep0.rank() == 2  ) {
                        if ( SemanticOps.isBins(dep0) ) {
                            xmins= DataSetOps.slice1( dep0, 0 );
                            xmaxs= DataSetOps.slice1( dep0, 1 );
                            Units u0= SemanticOps.getUnits(xmins );
                            Units u1= SemanticOps.getUnits(xmaxs );
                            if ( !u1.isConvertibleTo(u0) && u1.isConvertibleTo(u0.getOffsetUnits()) ) {
                                xmaxs= Ops.add( xmins, xmaxs );
                            }
                            msgs= vds;
                        } else {
                            throw new IllegalArgumentException("DEPEND_0 is rank 2 but not bins");
                        }
                    } else if ( dep0.rank()==1 && SemanticOps.isBins(dep0) ) {
                        xmins= replicate( DataSetOps.slice0( dep0, 0 ), 1 ); // make into 1-element rank 1 dataset
                        xmaxs= replicate( DataSetOps.slice0( dep0, 1 ), 1 );
                        msgs= Ops.replicate( dataset( EnumerationUnits.create("default").createDatum("_") ), 1 );
                    } else if ( dep0.rank() == 1 ) {
                        Datum width= SemanticOps.guessXTagWidth( dep0, null );
                        if ( width!=null ) {
                            width= width.divide(2);
                        } else {
                            QDataSet sort= Ops.sort(dep0);
                            QDataSet diffs= Ops.diff( DataSetOps.applyIndex(dep0,0,sort,false) );
                            QDataSet w= Ops.reduceMin( diffs,0 );
                            width= DataSetUtil.asDatum(w);
                        }
                        xmins= Ops.subtract(dep0, org.das2.qds.DataSetUtil.asDataSet(width) );
                        xmaxs= Ops.add(dep0, org.das2.qds.DataSetUtil.asDataSet(width) );
                        if ( vds==dep0 ) {
                            msgs= Ops.replicate( dataset( EnumerationUnits.create("default").createDatum("_") ), vds.length() );
                        } else {
                            msgs= vds;
                        }
                    } else {
                        throw new IllegalArgumentException("dataset is not correct form");
                    }       
                    Color c0= new Color( defaultColor );
                    Color c1= new Color( c0.getRed(), c0.getGreen(), c0.getBlue(), c0.getAlpha()==255 ? 128 : c0.getAlpha() );
                    int irgb= c1.getRGB();
                    colors= Ops.replicate( irgb, xmins.length() );
                    break;
                }
            case 0:
                {
                    xmins= Ops.replicate(vds,1); // increase rank from 0 to 1.
                    xmaxs= xmins;
                    Color c0= new Color( defaultColor );
                    Color c1= new Color( c0.getRed(), c0.getGreen(), c0.getBlue(), c0.getAlpha()==255 ? 128 : c0.getAlpha() );
                    int irgb= c1.getRGB();
                    colors= Ops.replicate( irgb, xmins.length() );
                    msgs= Ops.replicate(vds,1);
                    break;
                }
            default:
                throw new IllegalArgumentException("dataset must be rank 0, 1 or 2");
        }

        Units u0= SemanticOps.getUnits( xmins );
        Units u1= SemanticOps.getUnits( xmaxs );

        if ( u1.isConvertibleTo( u0.getOffsetUnits() ) && !u1.isConvertibleTo(u0) ) { // maxes are dt instead of stopt.
            xmaxs= Ops.add( xmins, xmaxs );
            xmaxs= putProperty( xmaxs, QDataSet.DEPEND_0, null );
            xmaxs= putProperty( xmaxs, QDataSet.LABEL, null );
        }

        xmins=  putProperty( xmins, QDataSet.NAME, "startTime" ); 
        xmaxs=  putProperty( xmaxs, QDataSet.NAME, "stopTime" );
        colors= putProperty( colors,QDataSet.NAME, "color" );
        msgs=   putProperty( msgs,  QDataSet.NAME, "messages" );
        
        colors= Ops.putProperty( colors, QDataSet.FORMAT, "0x%08x" );
        
        QDataSet lds= Ops.bundle( xmins, xmaxs, colors, msgs );
        
        return lds;
        
    }
    
    /**
     * return the values which occur in both rank 1 datasets.  Each dataset is sorted.
     * @param itE a bunch of values.
     * @param itB a bunch of values.
     * @return the set of values found in both.
     * @see #eventsConjunction(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static int[] dataIntersection( int[] itE, int[] itB ) {

        QDataSet tE= dataset( itE );
        QDataSet tB= dataset( itB );
        
        QDataSet dsr= dataIntersection( tE, tB );
        
        int[] result= new int[dsr.length()];
        for ( int i=0; i<result.length; i++ ) {
            result[i]= (int)dsr.value(i);
        }
        return result;
    }
    
    /**
     * return the values which occur in both rank 1 datasets.  Each dataset is sorted.
     * @param tE a bunch of values.
     * @param tB a bunch of values.
     * @return the set of values found in both.
     * @see #eventsConjunction(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet dataIntersection( QDataSet tE, QDataSet tB ) {
        QDataSet lE= sort(tE);
        QDataSet lB= sort(tB);
        
        tE= applyIndex( tE, lE );
        tB= applyIndex( tB, lB );
        
        int iE= 0;
        int iB= 0;
        
        DataSetBuilder dsb= new DataSetBuilder(1,100);
        
        while ( iE<tE.length() && iB<tB.length() ) {  
            double e= tE.value(iE);
            double b= tB.value(iB);
            if ( e==b ) {
                dsb.nextRecord( e );
                iE++;
                iB++;
            } else if ( e>b ) {
                iB++;
            } else if ( e<b ) {
                iE++;
            }
        }
        
        dsb.putProperty( QDataSet.UNITS, tE.property(QDataSet.UNITS) );
        
        return dsb.getDataSet();
    }
    
    /**
     * return an events dataset describing differences between the
     * two events lists.  The list will contain labels starting
     * with insert, delete, and update, and the values.
     * 
     * @param tE
     * @param tB
     * @return 
     */
    public static QDataSet eventsDiff( QDataSet tE, QDataSet tB ) {
        int iE= 0;
        int iB= 0;

        QDataSet tE4= createEvents(tE);
        tB= createEvents(tB);
                
        tE= Ops.trim1( tE4, 0, 2 );
        Units tu= (Units)tE.slice(0).slice(0).property( QDataSet.UNITS );
        tB= Ops.trim1( tB, 0, 2 );
        Units bu= (Units)tB.slice(0).slice(0).property( QDataSet.UNITS );
        tB= Ops.putProperty( tB, QDataSet.UNITS, bu );
        tB= convertUnitsTo( tB, tu );
        
        DataSetBuilder dsb= new DataSetBuilder(2,100,4);
        EnumerationUnits eu= (EnumerationUnits)((QDataSet)tE4.property(QDataSet.BUNDLE_1)).property(QDataSet.UNITS,3);
        dsb.setUnits( 3,eu );
            
        while ( iE<tE.length() || iB<tB.length() ) {
            if ( iE==tE.length() ) {
                logger.log(Level.FINE, "eventsDiff xxx {0}", tB.slice(iB).slice(0).svalue());   
            } else if ( iB==tB.length() ) {
                logger.log(Level.FINE, "eventsDiff {0} xxx", tE.slice(iE).slice(0).svalue());   
            } else {
                logger.log(Level.FINE, "eventsDiff {0} {1}", new Object[]{tE.slice(iE).slice(0).svalue(), tB.slice(iB).slice(0).svalue()});   
            }

            if ( iE==tE.length() ) {
                dsb.nextRecord( tB.value(iB,0), tB.value(iB,1), 0xa0f0a0, "insert " + tB.slice(iB).slice(3).svalue() );
                iB++;
            } else if ( iB==tB.length() ) {
                dsb.nextRecord( tE.value(iE,0), tE.value(iE,1), 0xf0a0a0, "delete " + tE.slice(iE).slice(3).svalue() );
                iE++;
            } else if ( tE.value(iE,0) > tB.value(iB,0) ) {
                dsb.nextRecord( tB.value(iB,0), tB.value(iB,1), 0xa0f0a0, "insert " + tB.slice(iB).slice(3).svalue() );
                iB++;
            } else if ( tE.value(iE,0) < tB.value(iB,0) ) {
                dsb.nextRecord( tE.value(iE,0), tE.value(iE,1), 0xf0a0a0, "delete " + tE.slice(iE).slice(3).svalue());
                iE++;
            } else {
                if ( !tE.slice(iE).slice(3).svalue().equals(tB.slice(iB).slice(3).svalue()) ) {
                    String msg= "modify " + tE.slice(iE).slice(3).svalue() + " &rarr; "+ tB.slice(iB).slice(3).svalue();
                    dsb.nextRecord( tB.value(iB,0), 
                                    tB.value(iB,1), 
                                    0xa0a0f0, // ColorUtil.ALICE_BLUE.getRGB(), 
                                    eu.createDatum( msg ) );
                }
                iB++;                
                iE++;
            }
        }
        dsb.putProperty( QDataSet.BUNDLE_1, tE4.property(QDataSet.BUNDLE_1 ) );
        
        return dsb.getDataSet();
        
    }
    
    
    /**
     * return an events list of when events are found in both events lists. 
     * (This might have been better called "eventsIntersection")
     * @param tE rank 2 canonical events list
     * @param tB rank 2 canonical events list
     * @return rank 2 canonical events list
     * @see Schemes#eventsList() 
     * @see #dataIntersection(org.das2.qds.QDataSet, org.das2.qds.QDataSet)    
     * @see #eventsComplement(org.das2.qds.QDataSet, org.das2.datum.DatumRange, int, java.lang.String) 
     */
    public static QDataSet eventsConjunction( QDataSet tE, QDataSet tB ) {

        int iE= 0;
        int iB= 0;

        String state= "open";
        Object start= null;

        QDataSet tE4= createEvents(tE);
        tB= createEvents(tB);
                
        tE= Ops.trim1( tE4, 0, 2 );
        Units tu= (Units)tE.slice(0).slice(0).property( QDataSet.UNITS );
        tB= Ops.trim1( tB, 0, 2 );
        Units bu= (Units)tB.slice(0).slice(0).property( QDataSet.UNITS );
        tB= Ops.putProperty( tB, QDataSet.UNITS, bu );
        tB= convertUnitsTo( tB, tu );
                
        DataSetBuilder dsb= new DataSetBuilder(2,100,4);
        EnumerationUnits eu= EnumerationUnits.create("default");
            
        while ( iE<tE.length() && iB<tB.length() ) {
            //states: 
            // tE begin timetag of E passed. 
            // tB begin timetag of B passed. 
            // tEB within both timeranges.
            // open outside of both timeranges.
            
            switch (state) {
                case "tE":
                    if ( tE.value(iE,1)<=tB.value(iB,0) ) {
                        state= "open";
                        iE= iE+1;
                    } else if ( tE.value(iE,1)>tB.value(iB,0)) {
                        state= "tEB";
                        start= tB.slice(iB).slice(0);
                    }   break;
                case "tB":
                    if ( tB.value(iB,1)<=tE.value(iE,0) ) {
                        state= "open";
                        iB= iB+1;
                    } else if ( tB.value(iB,1)>tE.value(iE,0) ) {
                        state= "tEB";
                        start= tE.slice(iE).slice(0);
                    }   break;
                case "tEB":
                    if ( tE.value(iE,1)<= tB.value(iB,1) ) {
                        state= "tB";
                        dsb.nextRecord( start, tE.slice(iE).slice(1), 0xA0A0A0, eu.createDatum("x") );
                        start= null;
                        iE= iE+1;
                        
                    } else if ( tB.value(iB,1)<=tE.value(iE,1) ) {
                        state= "tE";
                        dsb.nextRecord( start, tB.slice(iB).slice(1), 0xA0A0A0, eu.createDatum("x") );
                        start= null;
                        iB= iB+1;
                    } else {
                        System.err.println("huh");
                    }   break;
                case "open":
                    if ( tE.value(iE,0)<=tB.value(iB,0) ) {
                        state= "tE";
                    } else if ( tE.value(iE,0)>tB.value(iB,0) ) {
                        state= "tB";
                    }   break;
                default:
                    break;
            }
        }
        
        dsb.putProperty( QDataSet.BUNDLE_1, tE4.property(QDataSet.BUNDLE_1 ) );
        //dsb.putProperty( QDataSet.BINS_1, QDataSet.VALUE_BINS_MIN_MAX );
        
        QDataSet result= dsb.getDataSet();
                
        return result;
    }
    
    /**
     * Return an events list of time intervals which are not covered in the events list.
     * A new events list is returned, containing events with the given color and message.
     * This is expected to have a number of uses, one being identifying where data is 
     * missing.  Note this assumes events are not overlapping.
     * 
     * @param events an events list
     * @param range find gaps in events within this range
     * @param color color for the missing events
     * @param msg message to attach to these events
     * @return the events data set.
     * @see #createEvent(java.lang.String, int, java.lang.String) 
     * @see #eventsConjunction(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet eventsComplement( QDataSet events, DatumRange range, int color, String msg ) {
        events= createEvents(events); // make canonical
        QDataSet s= Ops.sort( Ops.slice1(events,0) );
        events= applyIndex( events, s );
        Datum st= range.min();
        QDataSet r= Ops.where( Ops.and( Ops.lt( Ops.slice1( events, 0 ), range.max() ), 
                Ops.gt( Ops.slice1( events, 1 ), range.min() ) ) );
        events= applyIndex( events, r );

        QDataSet bds= (QDataSet) events.property(QDataSet.BUNDLE_1);
        Units eu= (Units) bds.property( QDataSet.UNITS, 3 );
        
        Datum dmsg;
        if ( eu!=null && eu instanceof EnumerationUnits ) {
            dmsg= ((EnumerationUnits)eu).createDatum(msg);
        } else {
            dmsg= Units.nominal().createDatum(msg);
        }
        
        DataSetBuilder dsb= new DataSetBuilder( 2, 1000, 4 );
        dsb.setUnits( 0, (Units) bds.property( QDataSet.UNITS, 0 ) );
        dsb.setUnits( 1, (Units) bds.property( QDataSet.UNITS, 1 ) );
        dsb.setUnits( 2, (Units) bds.property( QDataSet.UNITS, 2 ) );
        dsb.setUnits( 3, (Units) bds.property( QDataSet.UNITS, 3 ) );
        
        for ( int i=0; i<events.length(); i++ ) {
            QDataSet e1= events.slice(i);
            Datum t1= Ops.datum( e1.slice(0) );
            if ( t1.gt(st) ) {
                dsb.nextRecord( st, t1, color, dmsg );
            }
            st= Ops.datum( e1.slice(1) );
        }
        if ( st.lt(range.max()) ) {
            dsb.nextRecord( st, range.max(), color, dmsg );
        }
        
        return dsb.getDataSet();
    }
    
    /**
     * return a dataset with X and Y forming a circle, introduced as a convenient way to indicate planet location.
     * @param x the x coordinate of the circle
     * @param y the y coordinate of the circle
     * @param radius rank 0 dataset
     * @return QDataSet that when plotted is a circle.
     */
    public static QDataSet circle( QDataSet radius, QDataSet x, QDataSet y ) {
        if ( radius==null ) radius= DataSetUtil.asDataSet(1.);
        MutablePropertyDataSet result= (MutablePropertyDataSet) Ops.link( 
                Ops.add( x, Ops.multiply( radius, sin(linspace(0,601*PI/300,601) ) ) ), 
                Ops.add( y, Ops.multiply( radius, cos(linspace(0,601*PI/300,601 ) ) ) ) );
        result.putProperty( QDataSet.RENDER_TYPE, "series" );
        return result;
    }
    
    /**
     * return a dataset with X and Y forming a circle, introduced as a 
     * convenient way to indicate planet location.  Note this is presently
     * returned as Y[X], but should probably return a rank 2 dataset that is a 
     * bundle.
     * @param x the x coordinate of the circle
     * @param y the y coordinate of the circle
     * @param radius rank 0 dataset
     * @return QDataSet that when plotted is a circle.
     */
    public static QDataSet circle( double radius, double x, double y ) {
        return circle( dataset(radius), dataset(x), dataset(y) );
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
     * @param dradius
     * @return QDataSet that when plotted is a circle.
     */
    public static QDataSet circle( double dradius ) {
        QDataSet radius= DataSetUtil.asDataSet(dradius);
        return circle( radius );
    }

    /**
     * return a dataset with X and Y forming a circle, introduced as a convenient way to indicate planet location.
     * @param sradius string parsed into rank 0 dataset
     * @return QDataSet that when plotted is a circle.
     * @throws java.text.ParseException
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
                    Units u= Units.lookupUnits(ss[1]);
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
     * return a dataset with X and Y forming a ellipse, introduced as a convenient way to indicate 
     * planet location of any planet, according to Masafumi.
     * @param xwidth 
     * @param ywidth
     * @return QDataSet that when plotted is an ellipse.
     */
    public static QDataSet ellipse( double xwidth, double ywidth ) {
        MutablePropertyDataSet result= (MutablePropertyDataSet) Ops.link( 
            Ops.multiply( xwidth, sin(linspace(0,601*PI/300,601) ) ), 
            Ops.multiply( ywidth, cos(linspace(0,601*PI/300,601 ) ) ) );
        result.putProperty( QDataSet.RENDER_TYPE, "series" );        
        return result;
    }
    
    /**
     * copies the properties, copying depend datasets as well.  
     * TODO: This is not thorough, and this needs to be reviewed.
     * @param ds the data from which the properties are extracted.
     * @return a map of the properties.
     * @see DataSetUtil#getProperties(org.das2.qds.QDataSet) 
     */
    public static Map<String,Object> copyProperties( QDataSet ds ) {
        Map<String,Object> result = new HashMap<>();
        Map<String,Object> srcProps= DataSetUtil.getProperties(ds);

        // is this a bundle descriptor?
        if ( ds.rank()==2 ) {
            boolean isBundle= false;
            String n= (String) ds.property( QDataSet.NAME );
            if ( n==null && ds.length()>0 ) {
                n= (String) ds.property( QDataSet.NAME, 0 );
            }
            for ( int i=0; i<ds.length(); i++ ) {
                if ( n!=ds.property( QDataSet.NAME, i ) ) {
                    isBundle= true;
                    break;
                }
            }
            if ( isBundle ) {
                String[] nn= new String[] { "NAME", "UNITS", "LABEL" };
                for ( int i=0; i<ds.length(); i++ ) {
                    for (String nn1 : nn) {
                        Object val = ds.property(nn1, i);
                        if (val!=null) {
                            result.put(nn1 + "__" + i, val);
                        }
                    }
                }
            }
        }
        
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
     * copy over all the indexed properties into the mutable property dataset.
     * This was introduced to support DataSetOps.unbundle, but should probably
     * always be used.
     * See https://sourceforge.net/p/autoplot/bugs/1704/
     *
     * @param srcds the source dataset
     * @param mds the destination dataset
     */
    public static void copyIndexedProperties(QDataSet srcds, MutablePropertyDataSet mds) {
        
        String[] names = propertyNames();

        for (String name : names) {
            for ( int i=0; i<srcds.length(); i++ ) {
                Object p= srcds.property(name, i);
                if ( p!=null ) {
                    mds.putProperty( name, i, p );
                }
            }
        }
        String[] moreNames= new String[] { QDataSet.DEPENDNAME_0, QDataSet.DEPENDNAME_1, QDataSet.CONTEXT_0, QDataSet.CONTEXT_1 };
        for (String name : moreNames ) {
            for ( int i=0; i<srcds.length(); i++ ) {
                Object p= srcds.property(name, i);
                if ( p!=null ) {
                    mds.putProperty( name, i, p );
                }
            }
        }

    }
    
    

    /**
     * copy the dataset to make a new one that is writable.  When a join dataset is copied, a WritableJoinDataSet is used
     * to copy each dataset.  This is a deep copy, so for example DEPEND_0 is copied as well.
     * Note that BufferDataSets will be copied to BufferDataSets, and ArrayDataSets 
     * will be copied to ArrayDataSets.
     * @param src
     * @return a copy of src.
     */
    public static WritableDataSet copy( QDataSet src ) {
        logger.log(Level.FINE, "copy({0})", src);
        if ( SemanticOps.isJoin(src) || DataSetUtil.isQube(src)==false ) {
            return WritableJoinDataSet.copy(src);
        } else {
            if ( src instanceof BufferDataSet ) {
                return BufferDataSet.copy(src);                
            } else {
                return ArrayDataSet.copy(src);
            }
        }
    }
    
    /**
     * Copy the dataset to an ArrayDataSet only if the dataset is not already an ArrayDataSet
     * or BufferDataSet.
     * Note this does not consider the mutability of the data.  If the dataset is not mutable, then the
     * original data could be returned (probably).
     * @param ads0 a dataset.
     * @return an ArrayDataSet or BufferDataSet
     */    
    public static WritableDataSet maybeCopy( QDataSet ads0) {
        if ( ads0 instanceof BufferDataSet ) {
            return BufferDataSet.maybeCopy( ads0 );
        } else {
            return ArrayDataSet.maybeCopy( ads0 );
        }
    }    
    
    /**
     * pick out the timetags and sort the data based on these.  This
     * only works when a dataset has DEPEND_0.
     * @param ds
     * @return the data sorted by DEPEND_0.
     */
    public static QDataSet sortInTime( QDataSet ds ) {
        QDataSet t= (QDataSet) ds.property(QDataSet.DEPEND_0);
        if ( t==null ) {
            throw new IllegalArgumentException("data does not contain DEPEND_0");
        }
        QDataSet s= sort( t );
        return applyIndex( ds, s );
        
    }
    
    /**
     * ensure that there are no non-monotonic or repeat records, by removing
     * the first N-1 records of N repeated records.  
     * 
     * Warning: this was extracted from AggregatingDataSource to support BufferDataSets,
     * and is minimally implemented.
     * 
     * When ds has property QDataSet.DEPEND_0, then this is used to identify the
     * monotonic subset.  When ds is a set of timetags, then these are used.
     * 
     * @param ds MutablePropertyDataSet, which must be writable.
     * @return dataset, possibly with records removed.
     */
    public static MutablePropertyDataSet monotonicSubset( QDataSet ds ) {
        
        MutablePropertyDataSet mds;
        if ( ds instanceof BufferDataSet ) {
            mds= (BufferDataSet)ds;
        } else if ( ds instanceof ArrayDataSet ) {
            mds= (ArrayDataSet)ds;
        } else {
            mds= ArrayDataSet.copy(ds);
        }
        
        assert mds instanceof ArrayDataSet || mds instanceof BufferDataSet;
        
        if ( mds.isImmutable() ) {
            if ( mds instanceof ArrayDataSet ) {
                mds= ArrayDataSet.copy(mds);
            } else {
                mds= BufferDataSet.copy(mds);
            } 
            logger.warning("immutabilty forced copy.");
        }
               
        QDataSet sdep0= (QDataSet)mds.property(QDataSet.DEPEND_0);
        if ( sdep0==null && UnitsUtil.isTimeLocation( SemanticOps.getUnits(mds) ) ) {
            sdep0= mds;
        } else if ( sdep0==null ) {
            return mds;
        }
        MutablePropertyDataSet dep0= Ops.maybeCopy( sdep0 ); // I don't think this will copy.

        if ( dep0.length()<2 ) return mds;
        if ( dep0.rank()!=1 ) return mds;
        QDataSet vdep0= Ops.valid(dep0);
        
        int[] rback= new int[dep0.length()];
        rback[dep0.length()/2]= dep0.length()/2;
        int rindex=dep0.length()/2+1;
        double a= dep0.value(rindex-1);
        for ( int i=rindex; i<dep0.length(); i++ ) {
            if ( vdep0.value(i)>0 ) {
                double a1=dep0.value(i);
                if ( a1>a ) {
                    rback[rindex]= i;
                    a= a1;
                    rindex++;
                } else {
                    logger.log(Level.FINER, "data point breaks monotonic rule: {0}", i);
                }
            }
        }
        int lindex=dep0.length()/2;
        a= dep0.value(lindex+1);
        for ( int i=lindex; i>=0; i-- ) {
            if ( vdep0.value(i)>0 ) {
                double a1=dep0.value(i);
                if ( a1<a ) {
                    rback[lindex]= i;
                    a= a1;
                    lindex--;
                } else {
                    logger.log(Level.FINER, "data point breaks monotonic rule: {0}", i);
                }
            }
        }
        lindex+=1;
        
        int nrm= dep0.length() - ( rindex-lindex );
        if ( nrm>0 ) {
            if ( rindex==1 ) {
                logger.log(Level.FINE, "ensureMono removes all points, assume it's monotonic decreasing" );
                return mds;
            }
            logger.log(Level.FINE, "ensureMono removes {0} points", nrm);
            int[] idx= new int[rindex-lindex];
            System.arraycopy( rback, lindex, idx, 0, ( rindex-lindex ) );
            mds.putProperty( QDataSet.DEPEND_0, null );
            MutablePropertyDataSet dep0copy;
            if ( mds instanceof ArrayDataSet ) {
                Class c= ((ArrayDataSet)mds).getComponentType();
                mds= ArrayDataSet.copy( c, new SortDataSet( mds, Ops.dataset(idx) ) );
                Class depclass= ((ArrayDataSet)dep0).getComponentType();
                dep0copy= ArrayDataSet.copy( depclass, new SortDataSet( dep0, Ops.dataset(idx) ) );
            } else if ( mds instanceof BufferDataSet ) {
                Object c= ((BufferDataSet)mds).getType();
                mds= BufferDataSet.copy( c, new SortDataSet( mds, Ops.dataset(idx) ) );
                Object depclass= ((BufferDataSet)dep0).getType();
                dep0copy= BufferDataSet.copy( depclass, new SortDataSet( dep0, Ops.dataset(idx) ) );
            } else {
                throw new IllegalArgumentException("dataset must be ArrayDataSet or BufferDataSet");
            }
            dep0copy.putProperty( QDataSet.MONOTONIC, Boolean.TRUE );
            mds.putProperty( QDataSet.DEPEND_0, dep0copy );
        } else {
            dep0.putProperty(QDataSet.MONOTONIC,Boolean.TRUE);
            mds.putProperty( QDataSet.DEPEND_0, dep0 );
        }
        return mds;
    }
        
    /**
     * element-wise sin.
     * @param ds
     * @return
     */
    public static QDataSet sin(QDataSet ds) {
        MutablePropertyDataSet result= applyUnaryOp(ds, (UnaryOp) (double d1) -> Math.sin(d1) );
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
        MutablePropertyDataSet result= applyUnaryOp(ds, (UnaryOp) (double d1) -> Math.asin(d1) );
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
        MutablePropertyDataSet result= applyUnaryOp(ds, (UnaryOp) (double d1) -> Math.cos(d1) );
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
        MutablePropertyDataSet result= applyUnaryOp(ds, (UnaryOp) (double d1) -> Math.acos(d1) );
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
        MutablePropertyDataSet result= applyUnaryOp(ds, (UnaryOp) (double a) -> Math.tan(a) );
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
        MutablePropertyDataSet result= applyUnaryOp(ds, (UnaryOp) (double a) -> Math.atan(a) );
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
     * element-wise atan2, 4-quadrant atan.  From the Java atan2 documentation:
     * "Returns the angle <i>theta</i> from the conversion of rectangular 
     * coordinates ({@code x},&nbsp;{@code y}) to polar coordinates 
     * (r,&nbsp;<i>theta</i>).  This method computes the phase <i>theta</i> 
     * by computing an arc tangent of {@code y/x} in the range of 
     * -<i>pi</i> to <i>pi</i>."
     * <p>Note different languages have different 
     * argument order.  Microsoft Office uses atan2(x,y); IDL uses atan(y,x);  
     * Matlab uses atan2(y,x); and NumPy uses arctan2(y,x).</p>
     * @param y the y values
     * @param x the x values
     * @return angles between -PI and PI
     * @see java.lang.Math#atan2(double, double) 
     */
    public static QDataSet atan2(QDataSet y, QDataSet x) {
         MutablePropertyDataSet result= 
                 applyBinaryOp(y, x, (BinaryOp) (double y1, double x1) -> Math.atan2(y1, x1));
        result.putProperty(QDataSet.LABEL, maybeLabelBinaryOp(y,x, "atan2" ) );
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
        MutablePropertyDataSet result= 
                applyUnaryOp(ds, (UnaryOp) (double a) -> Math.cosh(a));
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
        MutablePropertyDataSet result= 
                applyUnaryOp(ds, (UnaryOp) (double a) -> Math.sinh(a));
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
        MutablePropertyDataSet result= 
                applyUnaryOp(ds, (UnaryOp) (double a) -> Math.tanh(a));
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
     * Returns <i>e</i><sup>xx</sup>&nbsp;-1.  Note that for values of
     * <i>x</i> near 0, the exact sum of
     * <code>expm1(x)</code>&nbsp;+&nbsp;1 is much closer to the true
     * result of <i>e</i><sup>xx</sup> than <code>exp(x)</code>.
     *
     * @param xx
     * @return
     */
    public static QDataSet expm1(QDataSet xx) {
        MutablePropertyDataSet result= 
                applyUnaryOp(xx, (UnaryOp) (double a) -> Math.expm1(a));
        result.putProperty(QDataSet.LABEL, maybeLabelUnaryOp(result, "expm1" ) );
        return result;
    }
    
    /**
     * Returns <i>e</i><sup>x</sup>&nbsp;-1.  Note that for values of
     * <i>x</i> near 0, the exact sum of
     * <code>expm1(x)</code>&nbsp;+&nbsp;1 is much closer to the true
     * result of <i>e</i><sup>x</sup> than <code>exp(x)</code>.
     *
     * @param x a double
     * @return <i>e</i><sup>x</sup>&nbsp;-1
     */
    public static double expm1( double x ) {
        return Math.expm1( x );
    }
     
    /**
     * Returns <i>e</i><sup>x</sup>&nbsp;-1.  Note that for values of
     * <i>x</i> near 0, the exact sum of
     * <code>expm1(x)</code>&nbsp;+&nbsp;1 is much closer to the true
     * result of <i>e</i><sup>x</sup> than <code>exp(x)</code>.
     *
     * @param x an object which can be interpretted as a double or dataset.
     * @return <i>e</i><sup>x</sup>&nbsp;-1
     */
    public static QDataSet expm1( Object x ) {
        return expm1( dataset(x) );
    }

    /** scalar math functions http://sourceforge.net/p/autoplot/bugs/1052/ */
    
    /**
     * return the length of each index of a n-D array.  In Java these are
     * arrays of arrays, and no test is made to verify that the array is really
     * a qube.  This was introduced when it appeared that Python/jpype was
     * producing arrays without the getClass method.
     * 
     * For example, if we have an array of 3 arrays, each having 5 elements, 
     * then [ 3,5 ] is returned.
     * @param arg0 an array, or array of arrays, or array of array of arrays, etc.
     * @return the n dimensions of each index of the array.
     */
    public static int[] getQubeDimsForArray( Object arg0 ) {
        List<Integer> qqube= new ArrayList<>( );
        qqube.add( Array.getLength(arg0) );
        if ( qqube.get(0)>0 ) {
            Object slice= Array.get(arg0, 0);
            try {
                while ( slice.getClass().isArray() ) {
                    qqube.add( Array.getLength(slice) );
                    slice= Array.get( slice, 0 );
                }
            } catch ( ArrayIndexOutOfBoundsException | IllegalArgumentException ex ) {
                Array.getLength(slice);
            }
        }
        int[] qube= new int[qqube.size()];
        for ( int i=0; i<qube.length; i++ ) qube[i]= qqube.get(i);
        return qube;
    }
    
    /**
     * coerce Java objects like arrays Lists and scalars into a QDataSet.  
     * This is introduced to mirror the useful Jython dataset command.  This is a nasty business that
     * is surely going to cause all sorts of problems, so we should do it all in one place.
     * See http://jfaden.net/jenkins/job/autoplot-test029/
     * This supports:<ul>
     *   <li>int, float, double, etc to Rank 0 datasets
     *   <li>List&lt;Number&gt; to Rank 1 datasets.
     *   <li>Java arrays of Number to Rank 1-4 qubes datasets
     *   <li>Strings to rank 0 datasets with units ("5 s" or "2014-01-01T00:00")
     *   <li>Datums to rank 0 datasets
     *   <li>DatumRanges to rank 1 bins
     * </ul>
     * @param arg0 null,QDataSet,Number,Datum,DatumRange,String,List,or array.
     * @throws IllegalArgumentException if the argument cannot be parsed or converted.
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
        } else if ( arg0 instanceof Boolean ) {
            return DataSetUtil.asDataSet( ((Boolean)arg0) ? 1.0 : 0.0 );
        } else if ( arg0 instanceof DatumRange ) {
            return DataSetUtil.asDataSet( (DatumRange)arg0 );
        } else if ( arg0 instanceof String ) {
            String sarg= (String)arg0;
            try {
               return DataSetUtil.asDataSet( DatumUtil.parse(sarg) ); //TODO: someone is going to want lookupUnits that will allocate new units.
            } catch (ParseException ex) {
               try {
                   DatumRange dr= DatumRangeUtil.parseTimeRange(sarg); 
                   return DataSetUtil.asDataSet(dr);                   
               } catch ( ParseException ex2 ) {
                   try {
                      return DataSetUtil.asDataSet( Units.us2000.parse(sarg) );// rfe 543: ISO8601 support for time zones.  Back off feature.
                   } catch ( ParseException ex1 ) {
                        EnumerationUnits eu= EnumerationUnits.create("default");
                        Datum d= eu.createDatum(sarg);
                        return DataSetUtil.asDataSet(d);                       
                   }
               }
            }
        } else if ( arg0 instanceof List ) {
            List p= (List)arg0;
            double[] j= new double[ p.size() ];
            Units u= null;
            for ( int i=0; i<p.size(); i++ ) {
                Object n= p.get(i);
                if ( n instanceof Number ) {
                    j[i]= ((Number)n).doubleValue();
                } else if ( n instanceof String ) {
                    QDataSet ds1= dataset(n);
                    if ( u==null ) u= SemanticOps.getUnits(ds1);
                    j[i]= ds1.value();
                }
            }
            QDataSet q= DDataSet.wrap( j );
            if ( u!=null ) ((DDataSet)q).putProperty(QDataSet.UNITS,u);
            return q;            
        } else if ( arg0.getClass().isArray() ) { // convert Java array into QDataSet.  Assumes qube.
            //return DataSetUtil.asDataSet(arg0); // I think this is probably a better implementation.
            int[] qube= getQubeDimsForArray(arg0);
            return ArrayDataSet.wrap( arg0, qube, true );
        } else {
            String sarg0= String.valueOf( arg0 );
            if ( sarg0.startsWith("<") && sarg0.endsWith(">") ) {
                throw new IllegalArgumentException("Ops.dataset is unable to coerce \""+ sarg0.substring(1,sarg0.length()-1) + "\" to QDataSet");                
            } else {
                throw new IllegalArgumentException("Ops.dataset is unable to coerce "+arg0+" to QDataSet");
            }
        }
        
    }

    /**
     * coerce Java objects like arrays Lists and scalars into a QDataSet.  
     * This is introduced to mirror the useful Jython dataset command.  This is a nasty business that
     * is surely going to cause all sorts of problems, so we should do it all in one place.
     * See http://jfaden.net/jenkins/job/autoplot-test029/
     * This supports:<ul>
     *   <li>int, float, double, etc to Rank 0 datasets
     *   <li>List&lt;Number&gt; to Rank 1 datasets.
     *   <li>Java arrays of Number to Rank 1-4 qubes datasets
     *   <li>Strings to rank 0 datasets with units ("5 s" or "2014-01-01T00:00")
     *   <li>Datums to rank 0 datasets
     *   <li>DatumRanges to rank 1 bins
     * </ul>
     * @param arg0 null,QDataSet,Number,Datum,DatumRange,String,List,or array.
     * @param u units providing context
     * @throws IllegalArgumentException if the argument cannot be parsed or converted.
     * @return QDataSet
     * @see JythonOps#dataset(PyObject, org.das2.datum.Units) 
     */
    public static QDataSet dataset( Object arg0, Units u ) {
        if ( arg0==null ) {  // there was a similar test in the Python code.
            return null;
        } else if ( arg0 instanceof QDataSet ) {
            return (QDataSet)arg0;
        } else if ( arg0 instanceof Number ) {
            return DataSetUtil.asDataSet( u.createDatum( ((Number)arg0).doubleValue() ) );
        } else if ( arg0 instanceof Datum ) {
            return DataSetUtil.asDataSet( (Datum)arg0 );
        } else if ( arg0 instanceof DatumRange ) {
            return DataSetUtil.asDataSet( (DatumRange)arg0 );
        } else if ( arg0 instanceof String ) {
            String sarg= (String)arg0;
            try {
                if ( u instanceof EnumerationUnits ) {
                    return DataSetUtil.asDataSet( ((EnumerationUnits) u).createDatum(sarg) );
                } else {
                    return DataSetUtil.asDataSet( u.parse(sarg) ); //TODO: someone is going to want lookupUnits that will allocate new units.
                }
            } catch (ParseException ex) {
               try {
                   return DataSetUtil.asDataSet(TimeUtil.create(sarg));
               } catch ( ParseException ex2 ) {
                   try {
                      DatumRange dr= DatumRangeUtil.parseISO8601Range(sarg);
                      if ( dr==null ) {
                         throw new IllegalArgumentException( "unable to parse string: "+sarg ); // legacy.  It should throw ParseException now.
                      } else {
                         return DataSetUtil.asDataSet(dr);
                      }
                   } catch ( ParseException ex1 ) {
                       throw new IllegalArgumentException( "unable to parse string: "+sarg, ex1 );
                   }
               }
            }
        } else if ( arg0 instanceof List ) {
            List p= (List)arg0;
            double[] j= new double[ p.size() ];
            for ( int i=0; i<p.size(); i++ ) {
                Object n= p.get(i);
                if ( n instanceof Number ) {
                    j[i]=((Number)n).doubleValue();
                } else {
                    try {
                        if ( u instanceof EnumerationUnits ) {
                            j[i]= ((EnumerationUnits)u).createDatum((String)n).doubleValue(u);
                        } else {
                            j[i]= u.parse((String)n).doubleValue(u);
                        }
                    } catch (ParseException ex) {
                        throw new IllegalArgumentException(ex);
                    }
                }
            }
            QDataSet q= DDataSet.wrap( j, u );
            return q;            
        } else if ( arg0.getClass().isArray() ) { // convert Java array into QDataSet.  Assumes qube.
            //return DataSetUtil.asDataSet(arg0); // I think this is probably a better implementation.
            List<Integer> qqube= new ArrayList<>( );
            qqube.add( Array.getLength(arg0) );
            if ( qqube.get(0)>0 ) {
                Object slice= Array.get(arg0, 0);
                while ( slice.getClass().isArray() ) {
                    qqube.add( Array.getLength(slice) );
                    slice= Array.get( slice, 0 );
                }
            }
            int[] qube= new int[qqube.size()];
            for ( int i=0; i<qube.length; i++ ) qube[i]= qqube.get(i);
            if ( arg0.getClass().getComponentType()==String.class ) {
                double[] dd= new double[ Array.getLength(arg0) ];
                for ( int i=0; i<dd.length; i++ ) {
                    try {
                        dd[i]= u.parse( (String)Array.get(arg0,i) ).doubleValue(u);
                    } catch (ParseException ex) {
                        throw new IllegalArgumentException(ex);
                    }
                }
                return ArrayDataSet.wrap( dd, qube, true ).setUnits(u);
            } else {
                return ArrayDataSet.wrap( arg0, qube, true ).setUnits(u);
            }
        } else {
            String sarg0= String.valueOf( arg0 );
            if ( sarg0.startsWith("<") && sarg0.endsWith(">") ) {
                throw new IllegalArgumentException("Ops.dataset is unable to coerce \""+ sarg0.substring(1,sarg0.length()-1) + "\" to QDataSet");                
            } else {
                throw new IllegalArgumentException("Ops.dataset is unable to coerce "+arg0+" to QDataSet");
            }
        }
        
    }
    
    /**
     * coerce Java objects like numbers and strings into a Datum.
     * This is introduced to mirror the useful Jython dataset command.  This is a nasty business that
     * is surely going to cause all sorts of problems, so we should do it all in one place.
     * See http://jfaden.net:8080/hudson/job/autoplot-test029/
     * This supports:<ul>
     *   <li>int, float, double, etc to Rank 0 datasets
     *   <li>Strings to rank 0 datasets with units ("5 s" or "2014-01-01T00:00")
     *   <li>rank 0 datasets 
     * </ul>
     * @param arg0 null,QDataSet,Number,Datum, or String.
     * @throws IllegalArgumentException if the argument cannot be parsed or converted.
     * @return Datum
     */
    public static Datum datum( Object arg0 ) {
        if ( arg0==null ) {  // there was a similar test in the Python code.
            return null;
        } else if ( arg0 instanceof QDataSet ) {
            return DataSetUtil.asDatum((QDataSet)arg0);
        } else if ( arg0 instanceof Number ) {
            return Datum.create( ((Number)arg0).doubleValue() );
        } else if ( arg0 instanceof Datum ) {
            return (Datum)arg0;
        } else if ( arg0 instanceof String ) {
            String sarg= (String)arg0;
            try {
               return DatumUtil.parse(sarg);
            } catch (ParseException ex) {
               try {
                   return TimeUtil.create(sarg);
               } catch ( ParseException ex2 ) {
                   throw new IllegalArgumentException( "unable to parse string: "+sarg, ex2 );
               }
            }        
        } else {
            throw new IllegalArgumentException("unable to coerce "+arg0+" to Datum");
        }
    }
    
    /**
     * coerce Java objects like arrays and strings into a DatumRange.
     * This is introduced to mirror the useful Jython dataset command.  This is a nasty business that
     * is surely going to cause all sorts of problems, so we should do it all in one place.
     * See http://jfaden.net:8080/hudson/job/autoplot-test029/
     * This supports:<ul>
     *   <li>2-element rank 1 QDataSet
     *   <li>Strings like ("5 to 15 s" or "2014-01-01")
     *   <li>2-element arrays and lists
     * </ul>
     * @param arg0 null, QDataSet, String, array or List.
     * @throws IllegalArgumentException if the argument cannot be parsed or converted.
     * @return DatumRange
     */
    public static DatumRange datumRange( Object arg0 ) {
        if ( arg0==null ) {  // there was a similar test in the Python code.
            return null;
        } else if ( arg0 instanceof QDataSet ) {
            return DataSetUtil.asDatumRange((QDataSet)arg0);
        } else if ( arg0 instanceof DatumRange ) {
            return (DatumRange)arg0;
        } else if ( arg0 instanceof String ) {
            String sarg= (String)arg0;
            try {
                return DatumRangeUtil.parseDatumRange(sarg);
            } catch ( ParseException ex ) {
                throw new IllegalArgumentException( "unable to parse string as DatumRange: "+sarg );
            }
        } else if ( arg0 instanceof List ) {
            List p= (List)arg0;
            double[] j= new double[ p.size() ];
            if ( j.length!=2 ) throw new IllegalArgumentException("list should have two elements when creating DatumRange: "+arg0 );
            Units u= null;
            for ( int i=0; i<p.size(); i++ ) {
                Object n= p.get(i);
                Datum d= datum(n);
                if ( u==null ) {
                    u= d.getUnits();
                } else {
                    if ( d.getUnits()!=u ) {
                        throw new IllegalArgumentException( "units cannot change when creating DatumRange:"+arg0 );
                    }
                }
                j[i]= d.doubleValue(u);
            }
            return DatumRange.newDatumRange( j[0], j[1], u );

        } else if ( arg0.getClass().isArray() ) { 
            double[] j= new double[ Array.getLength(arg0) ];
            Units u= null;
            if ( j.length!=2 ) throw new IllegalArgumentException("array should have two elements when creating DatumRange: "+arg0 );
            for ( int i=0; i<j.length; i++ ) {
                Object n= Array.get(arg0,i);
                Datum d= datum(n);
                if ( u==null ) {
                    u= d.getUnits();
                } else {
                    if ( d.getUnits()!=u ) {
                        throw new IllegalArgumentException( "units cannot change when creating DatumRange:"+arg0 );
                    }
                }
                j[i]= d.doubleValue(u);
            }
            return DatumRange.newDatumRange( j[0], j[1], u );
            
        } else {
            String sarg0= String.valueOf( arg0 );
            if ( sarg0.startsWith("<") && sarg0.endsWith(">") ) {
                throw new IllegalArgumentException("Ops.dataset is unable to coerce \""+ sarg0.substring(1,sarg0.length()-1) + "\" to QDataSet");                
            } else {
                throw new IllegalArgumentException("Ops.dataset is unable to coerce "+arg0+" to QDataSet");
            }
        }
    }
    
    /**
     * convert the data to radians by multiplying each element by PI/180.
     * This does not check the units of the data, but a future version might.
     * @param ds
     * @return 
     */
    public static QDataSet toRadians(QDataSet ds) {
        MutablePropertyDataSet result= applyUnaryOp(ds, (UnaryOp) (double y) -> y * Math.PI / 180. );
        result.putProperty( QDataSet.UNITS, Units.radians );
        return result;
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
        MutablePropertyDataSet result= applyUnaryOp(ds, (UnaryOp) (double y) -> y * 180 / Math.PI );
        result.putProperty( QDataSet.UNITS, Units.degrees );
        return result;
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
        double v= Double.NEGATIVE_INFINITY;
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
        double v= Double.POSITIVE_INFINITY;
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
     * return the data at the indices given.  This will be a rank 1 QDataSet.
     * Often the indices are created with the "where" function, for example:
     * <code>
     * QDataSet r= Ops.subset( ds, Ops.where( Ops.gt( ds, 1 ) ) )
     * </code>
     * will return the subset of ds where ds is greater than 1.
     * @param ds rank N array, N &gt; 0.
     * @param w rank 1 dataset of length l indexing a rank 1 array, or rank 2 ds[l,N] indexing a rank N array.
     * @return rank 1 indices.
     * @see #applyIndex(org.das2.qds.QDataSet, org.das2.qds.QDataSet) applyIndex, which does the same thing.
     */
    public static QDataSet subset( QDataSet ds, QDataSet w ) {
        return DataSetOps.applyIndex( ds, 0, w, true);
    }
    
    /**
     * returns a dataset containing the indices of where the dataset is non-zero.
     * For a rank 1 dataset, returns a rank 1 dataset with indices for the values.
     * For a higher rank dataset, returns a rank 2 qube dataset with ds.rank()
     * elements in the first dimension.  Note when the dataset is all zeros (false),
     * the result is a zero-length array, as opposed to IDL which would return
     * a -1 scalar.
     * 
     * Note fill values are not included in the list, so it is not necessary that
     * where(A).length + where(not A).length != where( A.or(not(A) ).length
     *
     * Note this is different from the SciPy "where" and similar to Matlab "find."
     * 
     * @param ds of any rank M, M&gt;0.
     * @return a rank 1 or rank 2 dataset with N by M elements, where N is the number
     * of non-zero elements found.
     * @see #putValues(org.das2.qds.QDataSet, org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet where(QDataSet ds) {
        
        if ( ds==null ) {
            throw new NullPointerException("dataset is null");
        }
        if ( ds.rank()<1 ) {
            throw new IllegalArgumentException("dataset is rank 0");
        }
        
        if ( ds.rank()==1 
                && DataSetAnnotations.VALUE_0.equals(DataSetAnnotations.getInstance().getAnnotation(ds,DataSetAnnotations.ANNOTATION_ZERO_COUNT))
                && DataSetAnnotations.VALUE_0.equals(DataSetAnnotations.getInstance().getAnnotation(ds,DataSetAnnotations.ANNOTATION_INVALID_COUNT)) ) {
            return Ops.indgen(ds.length());
        }
        
        DataSetBuilder builder;

        QubeDataSetIterator iter = new QubeDataSetIterator(ds);
        QDataSet wds= DataSetUtil.weightsDataSet(ds);

        int blocksize= Math.max( 100, ds.length() / 4 );
        
        if (ds.rank() == 1) {
            builder = new DataSetBuilder( 1, blocksize );
            while (iter.hasNext()) {
                iter.next();
                if ( iter.getValue(wds)> 0 && iter.getValue(ds) != 0.) {
                    builder.putValue(-1, iter.index(0));
                    builder.nextRecord();
                }
            }
            builder.putProperty(QDataSet.MONOTONIC, Boolean.TRUE);
            if ( builder.getLength()==ds.length() ) {
                DataSetAnnotations.getInstance().putAnnotation(ds,DataSetAnnotations.ANNOTATION_INVALID_COUNT, DataSetAnnotations.VALUE_0 );
                DataSetAnnotations.getInstance().putAnnotation(ds,DataSetAnnotations.ANNOTATION_ZERO_COUNT, DataSetAnnotations.VALUE_0 );
            }
            builder.putProperty( QDataSet.VALID_MAX, ds.length() );
        } else {
            builder = new DataSetBuilder( 2, blocksize, ds.rank() );
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
            switch (ds.rank()) {
                case 2:
                    builder.putProperty(QDataSet.DEPEND_1, labelsDataset(new String[]{"dim0", "dim1"}));
                    break;
                case 3:
                    builder.putProperty(QDataSet.DEPEND_1, labelsDataset(new String[]{"dim0", "dim1", "dim2"}));
                    break;
                case 4:
                    builder.putProperty(QDataSet.DEPEND_1, labelsDataset(new String[]{"dim0", "dim1", "dim2", "dim4"}));
                    break;
                default:
                    break;
            }
        }

        builder.putProperty( QDataSet.CADENCE, DataSetUtil.asDataSet(1.0) );
        builder.putProperty( QDataSet.FORMAT, "%d" );

        return builder.getDataSet();
    }

    public static QDataSet where( Object ds ) {
        return where( dataset(ds) );
    }
    
    /**
     * returns 1 where the dataset contains elements within the set, 0 where elements are not within the set.
     * Note this cannot be used to check for fill data.
     * @param ds a dataset
     * @param set a dataset, typically rank 1, of values.
     * @return 1 where the dataset contains elements within the set, 0 otherwise.
     * @see #not(org.das2.qds.QDataSet)
     * @see #within(org.das2.qds.QDataSet, org.das2.qds.QDataSet)
     */
    public static QDataSet withinSet( QDataSet ds, QDataSet set ) {
        ArrayDataSet ids= ArrayDataSet.create( int.class, DataSetUtil.qubeDims(ds) );
        if ( ds.rank()==1 ) {
            for ( int i=0; i<ds.length(); i++ ) {
                if ( Ops.total( Ops.eq( ds.slice(i), set ) )>0 ) {
                    ids.putValue(i,1);
                } else {
                    ids.putValue(i,0);
                }
            }
        } else {
            QubeDataSetIterator it= new QubeDataSetIterator(ds);
            while ( it.hasNext() ) {
                it.next();
                if ( Ops.total( Ops.eq( it.getRank0Value(ds), set ) ) > 0 ) {
                    it.putValue( ids, 1 );
                } else {
                    it.putValue( ids, 0 );
                }
            }
        }
        return ids;
    }
            
    /**
     * return non-zero where the data in ds are within the bounds.  In Jython,
     *<blockquote><pre>
     *print within( [0,1,2,3,4], '2 to 4' ) --> [ 0,0,1,1,0 ]
     *print within( ttag, 'orbit:rbspa-pp:172' )
     *</pre></blockquote>
     * 
     * @param ds rank N dataset where N &gt; 0
     * @param bounds a rank 1 bounding box, or rank 2 array of bounding boxes, or rank 2 events list
     * @return rank N dataset containing non-zero where the condition is true.
     * @see #without(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     * @see #binsWithin(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     * @see #withinSet(org.das2.qds.QDataSet, org.das2.qds.QDataSet)
     */
    public static QDataSet within( QDataSet ds, QDataSet bounds ) {
        if ( bounds==null ) {
            throw new NullPointerException("bounds is null");
        }
        if ( bounds.rank()==0 ) {
            throw new IllegalArgumentException("bounds must be not be rank 0, but a rank 1 bounding box or rank 2 array of events");
        }
        if ( bounds.rank()==2 ) {
            return withinListOfBounds( ds, bounds );
        }
        final UnitsConverter uc= SemanticOps.getLooseUnitsConverter( bounds.slice(0), ds );
        final double min= uc.convert(bounds.value(0));
        final double max= uc.convert(bounds.value(1));
        return applyUnaryOp(ds, (UnaryOp) (double d1) -> ( d1 >= min && d1 < max ) ? 1.0 : 0.0);
    }
    
    /**
     * 
     * @param ds data, typically timetags.
     * @param bounds rank 2 list of bounds, with bounds[:,0] the mins and bounds[:,1] the maxes
     * @return dataset with the same geometry as ds, with 1 where within the bounds 0, when not within.
     */
    private static QDataSet withinListOfBounds( QDataSet ds, QDataSet bounds ) {
        if ( bounds.rank()!=2 ) throw new IllegalArgumentException("bounds should be rank 2 array of bounds");
        final UnitsConverter uc= SemanticOps.getLooseUnitsConverter( bounds.slice(0).slice(0), ds );
        return applyUnaryOp(ds, (UnaryOp) (double d1) -> {
            for (int i = 0; i<bounds.length(); i++) {
                double min1 = uc.convert(bounds.value(i,0));
                double max1 = uc.convert(bounds.value(i,1));
                if (d1 >= min1 && d1 < max1) {
                    return 1.0;
                }
            }
            return 0.0;
        });
    }
    
    /**
     * return non-zero where the data in ds are within the bounds.  In Jython,
     *<blockquote><pre>
     *print within( [0,1,2,3,4], '2 to 4' ) --> [ 0,0,1,1,0 ]
     *print within( ttag, 'orbit:rbspa-pp:172' )
     *</pre></blockquote>
     * 
     * Note, before March 2, 2015, this would incorrectly return the where of the result.
     * @param ds object which can be converted to rank N dataset where N &gt; 0
     * @param bounds a rank 1 bounding box, DatumRange, or two-element array.  
     * @return rank N dataset containing non-zero where the condition is true.
     * @see #without(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     * @see #where(java.lang.Object)
     * @see #binsWithin(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet within( Object ds, Object bounds ) {
        return within( dataset(ds), dataset( datumRange( bounds ) ) );
    }
    
    /**
     * return non-zero where the bins of ds are within the bounds.  
     * 
     * @param ds rank 2 bins dataset 
     * @param bounds a rank 1 bounding box.  
     * @return rank 1 dataset containing non-zero where the condition is true.
     * @see #within(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet binsWithin( QDataSet ds, QDataSet bounds ) {
        return and( ge( slice1(ds,1), bounds.slice(0) ), lt( slice1(ds,0), bounds.slice(1) ) );
    }
    
    /**
     * 
     * @param ds data, typically timetags.
     * @param bounds rank 2 list of bounds, with bounds[:,0] the mins and bounds[:,1] the maxes
     * @return dataset with the same geometry as ds, with 1 where within the bounds 0, when not within.
     */
    private static QDataSet withoutListOfBounds( QDataSet ds, QDataSet bounds ) {
        if ( bounds.rank()!=2 ) throw new IllegalArgumentException("bounds should be rank 2 array of bounds");
        final UnitsConverter uc= SemanticOps.getLooseUnitsConverter( bounds.slice(0).slice(0), ds );
        return applyUnaryOp(ds, (UnaryOp) (double d1) -> {
            for (int i = 0; i<bounds.length(); i++) {
                double min1 = uc.convert(bounds.value(i,0));
                double max1 = uc.convert(bounds.value(i,1));
                if (d1 >= min1 && d1 < max1) {
                    return 0.0;
                }
            }
            return 1.0;
        });
    }
    
    /**
     * return non-zero where the data in ds are outside of the bounds.  In Jython,
     *<blockquote><pre>
     *print without( [0,1,2,3,4], '2 to 4' ) --> [ 1,1,0,0,1 ]
     *print without( ttag, 'orbit:rbspa-pp:172' )
     *</pre></blockquote>
     * Note if bounds contain fill, then everything is fill.
     * 
     * @param ds rank N dataset where N &gt; 0
     * @param bounds a rank 1 bounding box.  
     * @return rank N dataset containing non-zero where the condition is true.
     * @see #within(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet without( QDataSet ds, QDataSet bounds ) {
        if ( bounds==null ) {
            throw new NullPointerException("bounds is null");
        }
        if ( bounds.rank()==0 ) {
            throw new IllegalArgumentException("bounds must be not be rank 0, but a rank 1 bounding box or rank 2 list of bounding boxes");
        }
        if ( bounds.rank()>2 ) {
            throw new IllegalArgumentException("bounds must be a rank 1 bounding box or rank 2 list of bounding boxes");
        }
        
        if ( bounds.rank()==1 ) {
            return or( lt( ds, bounds.slice(0) ), ge( ds, bounds.slice(1) ) );
        } else {
            return withoutListOfBounds( ds, bounds );
        }
            
    }

    /**
     * return non-zero where the data in ds are outside of the bounds.  In Jython,
     *<blockquote><pre>
     *print without( [0,1,2,3,4], '2 to 4' ) --> [ 1,1,0,0,1 ]
     *print without( ttag, 'orbit:rbspa-pp:172' )
     *</pre></blockquote>
     * Note if bounds contain fill, then everything is fill.
     * @param ds rank N dataset where N &gt; 0
     * @param bounds a rank 1 bounding box.  
     * @return rank N dataset containing non-zero where the condition is true.
     * @see #within(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet without( Object ds, Object bounds ) {
        QDataSet boundsDs= dataset( datumRange( bounds ) );
        return or( lt( ds, boundsDs.slice(0) ), ge( ds, boundsDs.slice(1) ) );
    }
    
    /**
     * return non-zero where the bins of ds are outside of the bounds.
     * @param ds rank 2 bins dataset
     * @param bounds a rank 1 bounding box.  
     * @return rank 1 dataset containing non-zero where the condition is true.
     * @see #binsWithin(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet binsWithout( QDataSet ds, QDataSet bounds ) {
        return or( lt( slice1(ds,0), bounds.slice(0) ), ge( slice1(ds,1), bounds.slice(1) ) );            
    }
        
    /**
     * returns a rank 1 dataset of indices that sort the rank 1 dataset ds.
     * This is not the dataset sorted.  For example:
     *<blockquote><pre>
     *ds= randn(2000)
     *s= sort( ds )
     *dsSorted= ds[s]
     *</pre></blockquote>
     * Note the result will have the property MONOTONIC==Boolean.TRUE if 
     * the data was sorted already.
     * @param ds rank 1 dataset
     * @return rank 1 dataset of indices that sort the input dataset.
     * @see #shuffle(org.das2.qds.QDataSet)  
     */
    public static QDataSet sort(QDataSet ds) {
        return DataSetOps.sort(ds);
    }
    
    public static QDataSet sort(Object ds) {
        return DataSetOps.sort(dataset(ds));
    }
    
    /**
     * Return the unique indices from the rank 1 dataset.  If the 
     * set is not monotonic, then return unique indices from the monotonic
     * portions.
     * 
     * @param ds rank 1 dataset, sorted, or mostly sorted.
     * @return the element indices.
     * @see #sort(java.lang.Object) 
     * @see #uniq(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     * @see #uniqValues(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet uniq( QDataSet ds ) {
        return uniq( ds, null );
    }
    
    /**
     * return a rank 1 hashcodes of each record the dataset, with one hashcodes value for each record.  The 
     * value of hashcodes should repeat if the record repeats.  
     * 
     * NOTE: This is under-implemented and should not be used
     * without understanding the code.
     * 
     * @param ds dataset with rank greater than 0.
     * @return rank 1 dataset.
     */
    public static QDataSet hashcodes( QDataSet ds ) {
        if ( ds.rank()==0 ) throw new IllegalArgumentException("rank 0 not supported");
        if ( ds.rank()==1 ) return ds;
        IDataSet result= IDataSet.createRank1(ds.length());
        for ( int i=0; i<ds.length(); i++ ) {
            QDataSet slice= ds.slice(i);
            QDataSet wds= Ops.valid(slice);
            int hash= 0;
            DataSetIterator it= new QubeDataSetIterator(slice);
            while ( it.hasNext() ) {
                it.next();
                if ( it.getValue(wds)>0. ) {
                    hash= hash * 31 + Double.valueOf(it.getValue(slice)).hashCode();
                } else {
                    hash= hash * 31;
                }
            }
            result.putValue( i,hash );
        }
        return result;
    }
    
    /**
     * Return the unique indices from the rank 1 dataset, using sort to resort the indices.
     * If sort is null, then
     * the dataset is assumed to be monotonic, and only repeating values are
     * coalesced.  If sort is non-null, then it is the result of the function
     * "sort" and should be a rank 1 list of indices that sort the data.
     * @param ds rank 1 dataset, sorted, or mostly sorted.
     * @param sort null, or the rank 1 dataset of indices
     * @return the indices of the unique elements.
     * @see #uniqValues uniqValues, which returns the values.
     */
    public static QDataSet uniq( QDataSet ds, QDataSet sort ) {
        if ( ds.rank()>1 ) throw new IllegalArgumentException("ds.rank()>1" );
        if ( sort!=null && sort.rank()>1 ) throw new IllegalArgumentException("sort.rank()>1" );

        DataSetBuilder builder= new DataSetBuilder(1,100);

        double d;
        int didx;
        if ( sort==null ) {
            DataSetIterator it= new QubeDataSetIterator(ds);
            if ( !it.hasNext() ) {
                return builder.getDataSet();
            }
            it.next();
            d= it.getValue(ds);
            didx= it.index(0);
            while ( it.hasNext() ) {
                it.next();
                double d1= it.getValue(ds);
                if ( d!=d1 ) {
                    builder.putValue(-1, didx );
                    builder.nextRecord();
                    d= d1;
                }
                didx= it.index(0);
            }
        } else {
            DataSetIterator it= new QubeDataSetIterator(sort);
            if ( !it.hasNext() ) {
                return builder.getDataSet();
            }
            it.next();
            int i;
            didx= (int) it.getValue(sort);
            d= ds.value(didx);
            while ( it.hasNext() ) {
                it.next();
                i= (int) it.getValue(sort);
                double d1= ds.value( i );
                if ( d!=d1 ) {
                    builder.putValue(-1, didx );
                    builder.nextRecord();
                    d= d1;
                }
                didx= i;
            }
        }
        builder.putValue(-1, didx );
        builder.nextRecord();

        return builder.getDataSet();

    }

    /**
     * return the unique elements from the dataset.  If sort is null, then
     * the dataset is assumed to be monotonic, and only repeating values are
     * coalesced.  If sort is non-null, then it is the result of the function
     * "sort" and should be a rank 1 list of indices that sort the data.
     *
     * renamed uniqValues from uniq to avoid confusion with the IDL command.
     *
     * This needs example code and should not be used for now.  See VirboAutoplot/src/scripts/test/testUniq.jy
     * @see #uniq uniq, which returns the indices.
     * @param ds rank 1 dataset, sorted, or mostly sorted.
     * @param sort null, or the rank 1 dataset of indices
     * @return the subset of the data which is uniq.
     * @see #uniq(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet uniqValues( QDataSet ds, QDataSet sort  ) {
        QDataSet idx= uniq( ds, sort );
        return DataSetOps.applyIndex( ds, 0, idx, true );
    }
    
    /**
     * retrieve a property from the dataset.  This was introduced for use
     * in the Data Mash Up tool.
     * @param ds the dataset
     * @param name the property name
     * @return the property or null (None) if the dataset doesn't have the property.
     */
    public static Object getProperty( QDataSet ds, String name ) {
        return ds.property(name);
    }
            
    /**
     * converts types often seen in Jython and Java codes to the correct type.  For
     * example,
     * <pre>ds= putProperty( [1400,2800], 'UNITS', 'seconds since 2012-01-01')</pre>
     * will convert the string 'seconds since 2012-01-01' into a Unit before assigning
     * it to the dataset.
     * 
     * @param ds the object which can be interpreted as a dataset, such as a number or array of numbers.
     * @param name the property name
     * @param value the property value, which can converted to the proper type. 
     * @return the dataset, possibly converted to a mutable dataset.
     */    
    public static MutablePropertyDataSet putProperty( Object ds, String name, Object value ) {
        return putProperty( dataset(ds), name, value );
    }
    
    /**
     * converts types often seen in Jython and Java codes to the correct type.  For
     * example, ds= putProperty( ds, 'UNITS', 'seconds since 2012-01-01').  The dataset
     * may be copied to make it mutable.
     * 
     * @param ds the dataset to which the property is to be set.
     * @param name the property name
     * @param value the property value, which can converted to the proper type. 
     * @return the dataset, possibly converted to a mutable dataset.
     */
    public static MutablePropertyDataSet putProperty( QDataSet ds, String name, Object value ) {
        
        MutablePropertyDataSet mds;
        if ( !( ds instanceof MutablePropertyDataSet ) ) {
            mds= ArrayDataSet.maybeCopy(ds);  // https://sourceforge.net/p/autoplot/bugs/1357/ should this be DataSetWrapper.wrap?
        } else {
            if ( ((MutablePropertyDataSet)ds).isImmutable() ) {
                mds= copy(ds);
            } else {
                mds= (MutablePropertyDataSet)ds;            
            }
        }
        
        if ( value!=null && ( value.equals("null") || value.equals("None") || value.equals("Null") ) ) {
            if ( !"String".equals(DataSetUtil.getPropertyType(name)) ) {
                if ( !( name.equals(QDataSet.TITLE) || name.equals(QDataSet.LABEL) ) ) {
                    value= null;
                }
            }
        }
        if ( value==null ) {
            mds.putProperty( name, null );
            return mds;
        }
        
        if ( value==ds && !name.equals("DEPEND_0") ) {
            throw new IllegalArgumentException("a dataset cannot have itself as a property");
        }
        
        String type= DataSetUtil.getPropertyType(name);
        if ( type==null ) {
            logger.log(Level.FINE, "unrecognized property {0}...", name);
            mds.putProperty( name, value ); 
            
        } else {
            switch (type) {
                case DataSetUtil.PROPERTY_TYPE_QDATASET:
                    QDataSet arg= Ops.dataset(value);
                    if ( name.equals("DEPEND_0") ) {
                        if ( arg.rank()>0 && mds.rank()>0 && arg.length()!=mds.length() ) {
                            throw new IllegalArgumentException("DEPEND_0 must be the same length as dataset");
                        }
                    }
                    mds.putProperty(name, arg);
                    break;
                case DataSetUtil.PROPERTY_TYPE_UNITS:
                    if ( value instanceof String ) {
                        String svalue= (String)value;
                        value= Units.lookupUnits(svalue);
                    }   mds.putProperty( name, value);
                    break;
                case DataSetUtil.PROPERTY_TYPE_BOOLEAN:
                    if ( value instanceof String ) {
                        String svalue= (String)value;
                        value= Boolean.valueOf(svalue);
                    } else if ( value instanceof Number ) {
                        value= !((Number)value).equals(0);
                    } else if ( value instanceof QDataSet ) {
                        value= !(((QDataSet)value).value()==0);
                    }
                    mds.putProperty( name, value);
                    break;
                case DataSetUtil.PROPERTY_TYPE_NUMBER:
                    if ( value instanceof String ) {
                        String svalue= (String)value;
                        Units u= (Units)mds.property(QDataSet.UNITS);
                        if ( u!=null ) {
                            try {
                                value= u.parse(svalue).doubleValue(u);
                            } catch (ParseException ex) {
                                try {
                                    value= Integer.valueOf(svalue);
                                } catch ( NumberFormatException ex2 ) {
                                    throw new IllegalArgumentException(ex);
                                }
                            }
                        } else {
                            if ( svalue.contains(".") || svalue.contains("e") || svalue.contains("E") ) {
                                value= Double.valueOf(svalue);
                            } else {
                                value= Integer.valueOf(svalue);
                            }
                        }
                    } else if ( value instanceof QDataSet ) {
                        QDataSet qvalue= (QDataSet)value;
                        if ( qvalue.rank()>1 ) throw new IllegalArgumentException("rank 0 dataset needed for putProperty");
                        value= qvalue.value();
                    }
                    mds.putProperty( name, value);
                    break;
                case DataSetUtil.PROPERTY_TYPE_CACHETAG:
                    if ( value instanceof String ) {
                        String svalue= (String)value;
                        int i= svalue.indexOf("@");
                        try {
                            DatumRange tr= DatumRangeUtil.parseTimeRange( svalue.substring(0,i) );
                            CacheTag r;
                            if ( i==-1 ) {
                                value= new CacheTag( tr, null );
                            } else if ( svalue.substring(i+1).trim().equals("intrinsic") ) {
                                value= new CacheTag( tr, null );
                            } else {
                                Datum res= Units.seconds.parse(svalue.substring(i+1));
                                value= new CacheTag( tr, res );
                            }
                        } catch ( ParseException ex ) {
                            throw new IllegalArgumentException(ex);
                        }
                    }   mds.putProperty( name, value);
                    break;
                case DataSetUtil.PROPERTY_TYPE_MAP:
                    if ( !( value instanceof Map ) ) {
                        try {
                            String json= value.toString(); // Python Dictionary
                            JSONObject obj= new JSONObject(json);
                            Map<String,Object> result= JsonUtil.jsonToMap(obj);
                            mds.putProperty( name, result );
                        } catch (JSONException ex) {
                            logger.log(Level.SEVERE, "type is not supported for PROPERTY TYPE MAP: "+value, ex);
                        }
                    } else {
                        mds.putProperty( name, value);
                    }   break;
                case DataSetUtil.PROPERTY_TYPE_STRING:
                    String svalue= (String)value;
                    if ( svalue.startsWith("'") && svalue.endsWith("'") ) {
                        svalue= svalue.substring(1,svalue.length()-1);
                    }
                    mds.putProperty( name, svalue);
                    break;
                default: // what am I not thinking of?
                    mds.putProperty( name, value);
                    break;
            }
            
        }
        
        return mds;        
        
    }
    
    /**
     * convert the object into the type needed for the property.
     * @param context the dataset to which we are assigning the value.
     * @param name the property name
     * @param value the value
     * @return the correct value.
     * @see org.autoplot.jythonsupport.PyQDataSet#convertPropertyValue
     */
    public static Object convertPropertyValue( QDataSet context, String name, Object value ) {
        
        if ( value==null ) return value;
        
        String type= DataSetUtil.getPropertyType(name);
        if ( type==null ) {
            throw new IllegalArgumentException("unrecognized property: "+name );
            
        } else {
            Units u= context==null ? Units.dimensionless : (Units)context.property(QDataSet.UNITS);
        
            switch (type) {
                case DataSetUtil.PROPERTY_TYPE_QDATASET:
                    return Ops.dataset(value);
                    
                case DataSetUtil.PROPERTY_TYPE_UNITS:
                    if ( value instanceof String ) {
                        String svalue= (String)value;
                        value= Units.lookupUnits(svalue);
                        return value;
                    } else if ( value instanceof Units ) {
                        return value;
                    } else {
                        throw new IllegalArgumentException("cannot convert to value for "+name+": "+value);
                    }
                    
                case DataSetUtil.PROPERTY_TYPE_BOOLEAN:
                    if ( value instanceof String ) {
                        String svalue= (String)value;
                        value= Boolean.valueOf(svalue);
                        return value;
                    } else if ( value instanceof Number ) {
                        value= !((Number)value).equals(0);
                        return value;
                    } else if ( value instanceof QDataSet ) {
                        value= !(((QDataSet)value).value()==0);
                        return value;
                    } else if (value instanceof Boolean ) {
                        return value;
                    } else {
                        throw new IllegalArgumentException("cannot convert to value for "+name+": "+value);
                    }
                case DataSetUtil.PROPERTY_TYPE_NUMBER:
                    if ( value instanceof String ) {
                        String svalue= (String)value;
                        if ( u!=null ) {
                            try {
                                value= u.parse(svalue).doubleValue(u);
                            } catch (ParseException ex) {
                                try {
                                    value= Integer.valueOf(svalue);
                                } catch ( NumberFormatException ex2 ) {
                                    throw new IllegalArgumentException(ex);
                                }
                            }
                        } else {
                            if ( svalue.contains(".") || svalue.contains("e") || svalue.contains("E") ) {
                                value= Double.valueOf(svalue);
                            } else {
                                value= Integer.valueOf(svalue);
                            }
                        }
                        return value;
                    } else if ( value instanceof QDataSet ) {
                        QDataSet qvalue= (QDataSet)value;
                        if ( qvalue.rank()>1 ) throw new IllegalArgumentException("rank 0 dataset needed for property of type Number: "+name);
                        value= datum(qvalue).doubleValue(u);
                        return value;
                    } else if ( value instanceof Datum ) {
                        value= ((Datum)value).doubleValue(u);
                        return value;
                    } else if ( value instanceof Number ) {
                        return value;
                    }
                    
                case DataSetUtil.PROPERTY_TYPE_CACHETAG:
                    if ( value instanceof String ) {
                        String svalue= (String)value;
                        int i= svalue.indexOf("@");
                        try {
                            DatumRange tr= DatumRangeUtil.parseTimeRange( svalue.substring(0,i) );
                            CacheTag r;
                            if ( i==-1 ) {
                                value= new CacheTag( tr, null );
                            } else if ( svalue.substring(i+1).trim().equals("intrinsic") ) {
                                value= new CacheTag( tr, null );
                            } else {
                                Datum res= Units.seconds.parse(svalue.substring(i+1));
                                value= new CacheTag( tr, res );
                            }
                            return value;
                        } catch ( ParseException ex ) {
                            throw new IllegalArgumentException(ex);
                        }
                    } else if ( value instanceof CacheTag ) {
                        return value;
                    } else {
                        throw new IllegalArgumentException("cannot convert to value for "+name+": "+value);
                    }
                    
                case DataSetUtil.PROPERTY_TYPE_MAP:
                    if ( !( value instanceof Map ) ) {
                        try {
                            String json= value.toString();
                            JSONObject obj= new JSONObject(json);
                            Map<String,Object> result= new HashMap<>();
                            Iterator i= obj.keys();
                            while ( i.hasNext() ) {
                                String k= String.valueOf( i.next() );
                                result.put( k, obj.get(k) );
                            }
                            return result;
                        } catch (JSONException ex) {
                            throw new IllegalArgumentException("cannot convert to value for "+name+": "+value);                            
                        }
                    } else {
                        return value;
                    }
                case DataSetUtil.PROPERTY_TYPE_STRING:
                    return value.toString();
                default:
                    return value;
            }
        }
    }
        
    /**
     * Like putProperty, but this inserts the value at the index.  This
     * was introduced to make it easier to work with bundles.  This
     * converts types often seen in Jython and Java codes to the correct type.  For
     * example, {@code bds= putProperty( bds, 'UNITS', 0, 'seconds since 2012-01-01')}.  
     * The dataset may be copied to make it mutable.
     * 
     * @param ds the dataset to which the property is to be set.
     * @param name the property name
     * @param index the property index
     * @param value the property value, which can converted to the proper type. 
     * @return the dataset, possibly converted to a mutable dataset.
     */
    public static MutablePropertyDataSet putIndexedProperty( QDataSet ds, String name, int index, Object value ) {
        
        MutablePropertyDataSet mds;
        if ( !( ds instanceof MutablePropertyDataSet ) ) {
            mds= ArrayDataSet.maybeCopy(ds);
        } else {
            if ( ((MutablePropertyDataSet)ds).isImmutable() ) {
                mds= ArrayDataSet.copy(ds);
            } else {
                mds= (MutablePropertyDataSet)ds;            
            }
        }
        
        if ( value!=null && ( value.equals("Null") || value.equals("None") ) ) {
            if ( !"String".equals(DataSetUtil.getPropertyType(name)) ) {
                if ( !( name.equals(QDataSet.TITLE) || name.equals(QDataSet.LABEL) ) ) {
                    value= null;
                }
            }
        }
        if ( value==null ) {
            mds.putProperty( name, index, null );
            return mds;
        }
        
        String type= DataSetUtil.getPropertyType(name);
        if ( type==null ) {
            logger.log(Level.FINE, "unrecognized property {0}...", name);
            mds.putProperty( name, value ); 
            
        } else {
            switch (type) {
                case DataSetUtil.PROPERTY_TYPE_QDATASET:
                    mds.putProperty(name, Ops.dataset(value));
                    break;
                case DataSetUtil.PROPERTY_TYPE_UNITS:
                    if ( value instanceof String ) {
                        String svalue= (String)value;
                        value= Units.lookupUnits(svalue);
                    }   mds.putProperty( name, value);
                    break;
                case DataSetUtil.PROPERTY_TYPE_BOOLEAN:
                    if ( value instanceof String ) {
                        String svalue= (String)value;
                        value= Boolean.valueOf(svalue);
                    } else if ( value instanceof Number ) {
                        value= !((Number)value).equals(0);
                    }   mds.putProperty( name, value);
                    break;
                case DataSetUtil.PROPERTY_TYPE_NUMBER:
                    if ( value instanceof String ) {
                        String svalue= (String)value;
                        Units u= (Units)mds.property(QDataSet.UNITS);
                        if ( u!=null ) {
                            try {
                                value= u.parse(svalue).doubleValue(u);
                            } catch (ParseException ex) {
                                try {
                                    value= Integer.valueOf(svalue);
                                } catch ( NumberFormatException ex2 ) {
                                    throw new IllegalArgumentException(ex);
                                }
                            }
                        } else {
                            if ( svalue.contains(".") || svalue.contains("e") || svalue.contains("E") ) {
                                value= Double.valueOf(svalue);
                            } else {
                                value= Integer.valueOf(svalue);
                            }
                        }
                    }   mds.putProperty( name, value);
                    break;
                case DataSetUtil.PROPERTY_TYPE_CACHETAG:
                    if ( value instanceof String ) {
                        String svalue= (String)value;
                        int i= svalue.indexOf("@");
                        try {
                            DatumRange tr= DatumRangeUtil.parseTimeRange( svalue.substring(0,i) );
                            CacheTag r;
                            if ( i==-1 ) {
                                value= new CacheTag( tr, null );
                            } else if ( svalue.substring(i+1).trim().equals("intrinsic") ) {
                                value= new CacheTag( tr, null );
                            } else {
                                Datum res= Units.seconds.parse(svalue.substring(i+1));
                                value= new CacheTag( tr, res );
                            }
                        } catch ( ParseException ex ) {
                            throw new IllegalArgumentException(ex);
                        }
                    }   mds.putProperty( name, index, value);
                    break;
                default:
                    mds.putProperty( name, index, value);
                    break;
            }
            
        }
        
        return mds;        
        
    }
        
    /**
     * Like putIndexedProperty, but manages the bundle for the client.  This
     * was introduced to make it easier to work with bundles.  This
     * converts types often seen in Jython and Java codes to the correct type.  For
     * example, {@code ds= putBundleProperty( ds, 'UNITS', 0, 'seconds since 2012-01-01')}.  
     * The dataset may be copied to make it mutable. If the bundle descriptor dataset 
     * is not found, it is added, making the rank 2 dataset a bundle.
     * 
     * @param ds the rank 1 or rank 2 bundle dataset to which the property is to be set. 
     * @param name the property name
     * @param index the property index
     * @param value the property value, which can converted to the proper type. 
     * @return the dataset, possibly converted to a mutable dataset.
     */
    public static MutablePropertyDataSet putBundleProperty( QDataSet ds, String name, int index, Object value ) {
        int dim=ds.rank()-1;
        String bundleProp= "BUNDLE_"+dim;
        QDataSet bds= (QDataSet) ds.property(bundleProp);
        if ( bds==null ) {
            bds= SparseDataSet.createRank(2);
        }
        MutablePropertyDataSet wbds= putIndexedProperty( bds, name, index, value );
        MutablePropertyDataSet mds= putProperty( ds, bundleProp, wbds );
        return mds;
    }
    
    /**
     * like putProperty, but this inserts values into the dataset.  If the dataset
     * is not mutable, then this will make a copy of the data and return the copy.
     * 
     * @param ds the rank 1 or greater dataset
     * @param indices rank 1 indices when ds is rank 1, or rank 2 [:,m] indices for a rank m dataset.
     * @param values null for fill, or the rank 0 value or rank 1 values to assign.
     * @return the dataset with the indices assigned new values.
     * @see #where(org.das2.qds.QDataSet) 
     * @see #removeValues(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static WritableDataSet putValues( Object ds, Object indices, Object values ) {
        QDataSet qvalues= dataset(values);
        QDataSet qindices= dataset(indices);
        QDataSet qds= dataset(ds);
        return putValues( qds, qindices, qvalues );
    }
    
    /**
     * like putProperty, but this inserts values into the dataset.  If the dataset
     * is not mutable, then this will make a copy of the data and return the copy.
     * 
     * @param ds the rank 1 or greater dataset
     * @param indices rank 1 indices when ds is rank 1, or rank 2 [:,m] indices for a rank m dataset.
     * @param value null for fill, or the rank 0 value or rank 1 values to assign.
     * @return the dataset with the indices assigned new values.
     * @see #where(org.das2.qds.QDataSet) 
     * @see #removeValues(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static WritableDataSet putValues( QDataSet ds, QDataSet indices, QDataSet value ) {
        DataSetUtil.checkListOfIndeces(ds,indices);
        WritableDataSet result;
        if ( ds instanceof WritableDataSet ) {
            WritableDataSet wds= (WritableDataSet)ds;
            if ( wds.isImmutable() ) {
                result= copy(wds);
            } else {
                result= wds;
            }
        } else {
            result= copy(ds);
        }
        
        // promote rank to match data, if necessary.  
        if ( result.rank()>1 && indices.rank()==1 ) { // allow rank 1 indices to putValues in rank N>1 dataset (promoting rank).
            DataSetBuilder dsb= new DataSetBuilder(2,indices.length()*result.length(0),result.rank());
            Object[] iii= new Object[result.rank()];
            for ( int i=0; i<indices.length(); i++ ) {
                int i0= (int)indices.value(i);
                iii[0]= i0;
                DataSetIterator it= new QubeDataSetIterator(result.slice(i0));
                while ( it.hasNext() ) {
                    it.next();
                    for ( int j=1; j<result.rank(); j++ ) {
                        iii[j]= it.index(j-1);
                        dsb.nextRecord(iii);
                    }
                }
            }
            indices= dsb.getDataSet();
        }
        
        double dvalue= Double.NaN;
        UnitsConverter uc=null;
        if ( value!=null ) {
            Units vu= SemanticOps.getUnits(value);
            if ( vu!=Units.dimensionless ) {
                uc= SemanticOps.getUnitsConverter(value,ds);
                if ( value.rank()==0) {
                    dvalue= uc.convert( value.value() );
                }
            } else {
                dvalue= value.value();
            }
        }
            
        if ( indices.rank()==1 ) indices= new BundleDataSet(indices);
        IndexListDataSetIterator iter= new IndexListDataSetIterator( indices );
        
        if ( value==null || value.rank()==0 ) {
            while ( iter.hasNext() ) {
                iter.next();
                iter.putValue( result, dvalue );
            }
        } else {
            int i=0; 
            while ( iter.hasNext() ) {
                assert uc!=null;
                iter.next();
                dvalue= uc.convert( value.value(i) );
                iter.putValue( result, dvalue );
                i= i+1;
            }
        }
        return result;
    }
    
    /**
     * put fill data for these indices
     * @param ds the rank 1 or greater dataset
     * @param indices rank 1 indices when ds is rank 1, or rank 2 [:,m] indices for a rank m dataset.
     * @return the dataset with the data at the indices made invalid.
     * @see #putValues(org.das2.qds.QDataSet, org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     * @see #where(org.das2.qds.QDataSet) 
     * @see #removeIndeces(org.das2.qds.QDataSet, org.das2.qds.QDataSet) which copies the data to remove the indices.
     */
    public static WritableDataSet removeValues( QDataSet ds, QDataSet indices ) {
        DataSetUtil.checkListOfIndeces(ds,indices);
        return putValues( ds, indices, null );
    }
    
    public static WritableDataSet removeValues( Object ds, Object indices ) {
        return putValues( dataset(ds), dataset(indices), null );
    }
    
    /**
     * remove values in the dataset which are greater than the value.  
     * This is a convenient method for the common case where we want to
     * filter data by values within the data, introduced to support
     * the data mash up dialog.
     * @param ds rank N dataset
     * @param v the value, a rank 0 scalar or dataset with compatible geometry
     * @return the dataset with these
     */
    public static WritableDataSet removeValuesGreaterThan( QDataSet ds, QDataSet v ) {
        QDataSet r= where( gt(ds,v) );
        return putValues( ds, r, null );
    }
    
    public static WritableDataSet removeValuesGreaterThan( Object ds, Object v ) {
        return removeValuesGreaterThan( dataset(ds), dataset(v) );
    }
        
    /**
     * remove values in the dataset which are less than the value.
     * This is a convenient method for the common case where we want to
     * filter data by values within the data, introduced to support
     * the data mash up dialog.  Note that this inserts fill where data is 
     * to be removed.
     * @param ds rank N dataset
     * @param v the value, a rank 0 scalar or dataset with compatible geometry
     * @return the dataset with these
     */
    public static WritableDataSet removeValuesLessThan( QDataSet ds, QDataSet v ) {
        QDataSet r= where( lt(ds,v) );
        return putValues( ds, r, null );
    }    
    
    public static WritableDataSet removeValuesLessThan( Object ds, Object v ) {
        return removeValuesLessThan( dataset(ds), dataset(v) );
    }
    
    /**
     * remove the fill values from the rank 1 dataset, returning a smaller dataset.
     * This was introduced to support the mash-up dialog.
     * @param ds
     * @return dataset with the values removed.
     */
    public static WritableDataSet removeFill( QDataSet ds ) {
        QDataSet r= where( valid(ds) );
        return applyIndex( ds, r );
    }
    
    /**
     * apply the indices, checking for out-of-bounds values.
     * @param vv values to return, a rank 1, N-element dataset.
     * @param ds the indices.
     * @param fillValue the value to use when the index is out-of-bounds.
     * @return data a dataset with the geometry of ds and the units of values.
     * @see #subset(org.das2.qds.QDataSet, org.das2.qds.QDataSet) subset, which does the same thing.
     * @see #applyIndex(org.das2.qds.QDataSet, int, org.das2.qds.QDataSet) 
     */
    public static WritableDataSet applyIndex( QDataSet vv, QDataSet ds, Number fillValue ) {
        QubeDataSetIterator iter= new QubeDataSetIterator(ds);
        DDataSet result= iter.createEmptyDs();
        while ( iter.hasNext() ) {
            iter.next();
            int idx= (int)( iter.getValue(ds) );
            try {
                iter.putValue( result, vv.value(idx) );
            } catch ( IndexOutOfBoundsException ex ) {
                iter.putValue( result, fillValue.doubleValue() );
            }
        }
        result.putProperty(QDataSet.UNITS,vv.property(QDataSet.UNITS));
        result.putProperty(QDataSet.FILL_VALUE, fillValue );
        return result;
    }
    
    /**
     * apply the indices 
     * @param ds values to return, a rank 1 N-element dataset, or rank 2 N by m element dataset.
     * @param r the indices.
     * @return data a dataset with the geometry of ds and the units of values.
     * @see #subset(org.das2.qds.QDataSet, org.das2.qds.QDataSet) subset, which does the same thing.
     * @see #applyIndex(org.das2.qds.QDataSet, int, org.das2.qds.QDataSet) 
     */
    public static WritableDataSet applyIndex( QDataSet ds, QDataSet r ) {
        QubeDataSetIterator iter= new QubeDataSetIterator(r);
        Number fill= (Number)ds.property( QDataSet.FILL_VALUE );
        if ( fill==null ) fill= -1e38;
        
        WritableDataSet result;
        
        if ( ds.rank()==2 ) {
            DataSetBuilder resultb= new DataSetBuilder(ds.rank(),r.length(),ds.length(0));
            while ( iter.hasNext() ) {
                iter.next();
                int idx= (int)( iter.getValue(r) );
                resultb.nextRecord( ds.slice(idx) );
            }
            result= resultb.getDataSet();
        } else {
            result= iter.createEmptyDs();
            while ( iter.hasNext() ) {
                iter.next();
                int idx= (int)( iter.getValue(r) );
                if ( idx<0 || idx>=ds.length() ) {
                    iter.putValue( result, fill.doubleValue() );
                } else {
                    iter.putValue( result, ds.value(idx) );
                }
            }
        }
        
        result.putProperty(QDataSet.UNITS,ds.property(QDataSet.UNITS));
        result.putProperty(QDataSet.FILL_VALUE,fill);
        
        Map<String,Object> pp= DataSetUtil.getProperties( ds );
        pp.remove( QDataSet.DEPEND_0 );
        pp.remove( QDataSet.BUNDLE_0 );
        DataSetUtil.putProperties( pp, result );
        
        QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
        if ( dep0!=null ) result.putProperty(QDataSet.DEPEND_0,applyIndex( dep0, r ));

        QDataSet bundle0= (QDataSet) ds.property(QDataSet.BUNDLE_0);
        if ( bundle0!=null ) result.putProperty(QDataSet.BUNDLE_0,applyIndex( bundle0, r ));
        
        return result;
    }
    
    /**
     * apply the indices 
     * @param dso values to return, a rank 1, N-element dataset.
     * @param r the indices.
     * @return data a dataset with the geometry of ds and the units of values.
     * @see #applyIndex(org.das2.qds.QDataSet, int, org.das2.qds.QDataSet) 
     */
    public static WritableDataSet applyIndex( Object dso, QDataSet r ) {
        QDataSet vv= dataset(dso);
        QubeDataSetIterator iter= new QubeDataSetIterator(r);
        DDataSet result= iter.createEmptyDs();
        while ( iter.hasNext() ) {
            iter.next();
            int idx= (int)( iter.getValue(r) );
            iter.putValue( result, vv.value(idx) );
        }
        result.putProperty(QDataSet.UNITS,vv.property(QDataSet.UNITS));
        return result;
    }
    
    /**
     * apply the indices to the given dimension.
     * @param ds
     * @param dimension
     * @param indices
     * @return 
     * @see SubsetDataSet
     */
    public static MutablePropertyDataSet applyIndex( QDataSet ds, int dimension, QDataSet indices ) {
        SubsetDataSet sds= new SubsetDataSet(ds);
        sds.applyIndex(dimension,indices);
        return sds;
    }
    
    /**
     * remove the data at the indices from the rank 1 dataset.  This can be
     * used for example like so:
     * <pre>
     * {@code
     * ds= ripples(20)
     * ds= removeIndeces( ds, where( valid(ds).eq(0) ) )
     * print ds.length()
     * }
     * </pre>
     * @param vv a rank 1 dataset
     * @param indices the indices to remove.
     * @see https://github.com/autoplot/dev/blob/master/rfe/20190208/demoRemoveIndeces.jy
     * @see #removeValues(org.das2.qds.QDataSet, org.das2.qds.QDataSet) which inserts fill.
     * @see #where(org.das2.qds.QDataSet) 
     * @return a dataset with the values removed.
     */
    public static QDataSet removeIndeces( QDataSet vv, QDataSet indices ) {
        if ( indices.length()==0 ) return vv;
        if ( !DataSetUtil.isMonotonic(indices) ) {
            QDataSet s= sort(indices);
            indices= applyIndex( indices, s );
        }
        if ( indices.value(0) % 1 > 0 ) {
            throw new IllegalArgumentException("indices must be counting numbers.");
        }
        int currentSkipIndex= 0;
        int nextSkepIndex= (int)indices.value(currentSkipIndex);
        int currentIndex= 0;
        DataSetBuilder build;
        switch (vv.rank()) {
            case 1:
                build= new DataSetBuilder(1,100*(int)Math.ceil((vv.length()-indices.length())/100.) );
                break;
            case 2:
                build= new DataSetBuilder(2,100*(int)Math.ceil((vv.length()-indices.length())/100.),vv.length(0) );
                break;
            default:
                throw new IllegalArgumentException("only rank 1 and rank 2 datasets are supported");
        }
        while ( currentIndex<vv.length() ) {
            if ( currentIndex==nextSkepIndex ) {
                currentSkipIndex++;
                if ( currentSkipIndex==indices.length() ) {
                    nextSkepIndex= -1;
                } else {
                    nextSkepIndex= (int)indices.value(currentSkipIndex);
                }
            } else {
                build.nextRecord(vv.slice(currentIndex));
            }
            currentIndex++;
        }
        DDataSet result= build.getDataSet();
        DataSetUtil.putProperties( DataSetUtil.getProperties(vv), result );
        
        QDataSet dep0= (QDataSet) vv.property(QDataSet.DEPEND_0);
        if ( dep0!=null ) {
            result.putProperty( QDataSet.DEPEND_0, removeIndeces( dep0, indices ) );
        }
        
        return result;
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
     * returns a rank 1 dataset of indices that shuffle the rank 1 dataset ds.
     *<blockquote><pre>
     *s= shuffle( ds )
     *dsShuffled= ds[s]
     *</pre></blockquote>
     * @param ds rank 1 dataset
     * @return rank 1 dataset of integer indices.
     * @see #sort(org.das2.qds.QDataSet) 
     */
    public static QDataSet shuffle(QDataSet ds) {
        int size = ds.length();
        int[] back = new int[size];
        for (int i = 0; i < size; i++) {
            back[i] = i;
        }
        WritableDataSet wds = IDataSet.wrap(back, 1, size, 1, 1);

        Random r = random;

        for (int i = 0; i < size; i++) {
            int i1 = r.nextInt(size - i) + i;
            double t = wds.value(i1);
            wds.putValue(i1, wds.value(i));
            wds.putValue(i, t);
        }

        return wds;
    }
        
    /**
     * returns a rank 1 dataset of indices that shuffle the rank 1 dataset ds.
     *<blockquote><pre>
     *s= shuffle( ds )
     *dsShuffled= ds[s]
     *</pre></blockquote>
     * @param ds an object which can be interpretted as a rank 1 dataset.
     * @return rank 1 dataset of integer indices.
     * @see #sort(org.das2.qds.QDataSet) 
     */
    public static QDataSet shuffle(Object ds) {
        return shuffle(dataset(ds));
    }

    
    /**
     * returns the slice at the given slice location.  
     * @see org.das2.qds.QDataSet#slice(int) 
     * @param ds the rank N (N&gt;0) or more dataset
     * @param idx the index 
     * @return rank N-1 dataset
     */
    public static QDataSet slice0( QDataSet ds, int idx ) {
        return ds.slice(idx);
    }

    /**
     * returns the slice at the given slice location.  The dataset
     * must have monotonic DEPEND_0.
     * @param ds ripples(20,20).  Presently this must be a simple table.
     * @param sliceds dataset("10.3")
     * @return the slice at the given location
     * @see #trim(org.das2.qds.QDataSet, org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet slice0( QDataSet ds, QDataSet sliceds ) {
        if ( sliceds.rank()!=0 ) {
            throw new IllegalArgumentException("sliceds must be rank 0");
        }
        QDataSet dep= SemanticOps.xtagsDataSet(ds);
        if ( dep.rank()!=1 ) {
            throw new IllegalArgumentException("dataset must have rank 1 tags");
        }
        QDataSet findex= Ops.findex( dep, sliceds );
        double f= findex.value();
        //TODO: bug 1234: slice at string appears to mis with FFTPower result
        if ( f>=0. && f<dep.length() ) {
            return slice0( ds, (int)Math.round(f) );
        } else if ( f<0 ) {
            return slice0( ds, 0 );
        } else { 
            return slice0( ds, dep.length()-1 );
        }
    }    
    
    
    /**
     * returns the slice at the given slice location.
     * @see org.das2.qds.DataSetOps#slice1(org.das2.qds.QDataSet, int) 
     * @param ds the rank N where N&ge;2 dataset, which is also a qube.
     * @param idx the index to slice at.
     * @return rank N-1 dataset.
     */
    public static QDataSet slice1( QDataSet ds, int idx ) {
        return DataSetOps.slice1(ds, idx);
    }
    
    /**
     * returns the slice at the given slice location.
     * @param ds the rank N where N&ge;2 dataset, which is also a qube.
     * @param sliceds dataset("10.3")
     * @return rank N-1 dataset.
     */
    public static QDataSet slice1( QDataSet ds, QDataSet sliceds ) {
        if ( sliceds.rank()!=0 ) {
            throw new IllegalArgumentException("sliceds must be rank 0");
        }
        QDataSet dep= SemanticOps.ytagsDataSet(ds);
        if ( dep.rank()!=1 ) {
            throw new IllegalArgumentException("dataset must have rank 1 tags");
        }
        QDataSet findex= Ops.findex( dep, sliceds );
        double f= findex.value();
        if ( f>=0. && f<dep.length() ) {
            return slice1( ds, (int)Math.round(f) );
        } else if ( f<0 ) {
            return slice1( ds, 0 );
        } else { 
            return slice1( ds, dep.length()-1 );
        }
    }        

    /**
     * returns the slice at the given slice location.
     * @see org.das2.qds.DataSetOps#slice2(org.das2.qds.QDataSet, int) 
     * @param ds the rank N where N&ge;3 dataset, which is also a qube.
     * @param idx the index to slice at.
     * @return rank N-1 dataset.
     */
    public static QDataSet slice2( QDataSet ds, int idx ) {
        return DataSetOps.slice2(ds, idx);
    }

    /**
     * returns the slice at the given slice location.
     * @param ds ripples(20,20).  Presently this must be a simple table.
     * @param sliceds dataset("10.3")
     * @return 
     */
    public static QDataSet slice2( QDataSet ds, QDataSet sliceds ) {
        if ( sliceds.rank()!=0 ) {
            throw new IllegalArgumentException("sliceds must be rank 0");
        }
        QDataSet dep= SemanticOps.ytagsDataSet(ds);
        if ( dep.rank()!=1 ) {
            throw new IllegalArgumentException("dataset must have rank 1 tags");
        }
        QDataSet findex= Ops.findex( dep, sliceds );
        double f= findex.value();
        if ( f>=0. && f<dep.length() ) {
            return slice2( ds, (int)Math.round(f) );
        } else if ( f<0 ) {
            return slice2( ds, 0 );
        } else { 
            return slice2( ds, dep.length()-1 );
        }
    }    
    
    /**
     * returns the slice at the given slice location.
     * @see org.das2.qds.DataSetOps#slice3(org.das2.qds.QDataSet, int) 
     * @param ds the rank N where N&ge;4 dataset, which is also a qube.
     * @param idx the index to slice at.
     * @return rank N-1 dataset.
     */
    public static QDataSet slice3( QDataSet ds, int idx ) {
        return DataSetOps.slice3(ds, idx);
    }

    /**
     * returns the slice at the given slice location.
     * @param ds ripples(20,20).  Presently this must be a simple table.
     * @param sliceds dataset("10.3")
     * @return 
     */
    public static QDataSet slice3( QDataSet ds, QDataSet sliceds ) {
        if ( sliceds.rank()!=0 ) {
            throw new IllegalArgumentException("sliceds must be rank 0");
        }
        QDataSet dep= SemanticOps.ytagsDataSet(ds);
        if ( dep.rank()!=1 ) {
            throw new IllegalArgumentException("dataset must have rank 1 tags");
        }
        QDataSet findex= Ops.findex( dep, sliceds );
        double f= findex.value();
        if ( f>=0. && f<dep.length() ) {
            return slice3( ds, (int)Math.round(f) );
        } else if ( f<0 ) {
            return slice3( ds, 0 );
        } else { 
            return slice3( ds, dep.length()-1 );
        }
    }    
    
    /**
     * Enumeration identifying windows applied to data before doing FFTs.
     * @see #fftFilter
     * @see #windowFunction(org.das2.qds.ops.Ops.FFTFilterType, int) 
     */
    public static enum FFTFilterType{ 
        /**
         * Hanning, or Hann window http://en.wikipedia.org/wiki/Hann_function
         */
        Hanning, 
        /**
         * Hanning, or Hann window http://en.wikipedia.org/wiki/Hann_function
         */
        Hann, 
        /**
         * Ones in the middle, and tapers down to zero with a cosine at the ends.
         */
        TenPercentEdgeCosine, 
        /**
         * Unity or boxcar is all ones.
         */
        Unity,
        /**
         * Unity or boxcar is all ones.
         */
        Boxcar
    };

    /**
     * Apply windows to the data to prepare for FFT.  The data is reformed into a rank 2 dataset [N,len].
     * The filter is applied to the data to remove noise caused by the discontinuity.
     * This is deprecated, and windowFunction should be used so that the filter
     * is applied to records just before each fft is performed to save space.
     * @param ds rank 1, 2, or 3 data
     * @param len size of the window.
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
            
            Units cunits= null;
            Units dep0units= null;
            if ( c!=null ) {
                cunits= SemanticOps.getUnits(c);
                if ( dep0ds!=null ) {
                    dep0units= SemanticOps.getUnits(dep0ds);
                    if ( !cunits.getOffsetUnits().isConvertibleTo(dep0units.getOffsetUnits()) ) {
                        c= null;
                    }
                }
            } else {
                dep0units= SemanticOps.getUnits(dep0ds);
            }
            
            if ( c==null && dep0ds!=null ) {
                c= dep0ds.slice(0);
                cunits= dep0units;
            }

            JoinDataSet dep0;
            JoinDataSet jds= new JoinDataSet(ds);
            if ( c!=null && c.rank()==0 ) {
                dep0= new JoinDataSet(c);
                dep0.putProperty( QDataSet.UNITS, cunits );
                jds.putProperty( QDataSet.DEPEND_0, dep0 );
                if ( dep0units!=null && ( dep0units.isConvertibleTo(cunits) || dep0units.getOffsetUnits().isConvertibleTo(cunits) ) ) {
                    jds.putProperty( QDataSet.DEPEND_1, Ops.subtract( dep0ds, c ) );
                } else {
                    jds.putProperty( QDataSet.DEPEND_1, dep0ds );
                }
            }
            jds.putProperty( QDataSet.UNITS, ds.property(QDataSet.UNITS) );

            ds= jds;
        }

        switch (ds.rank()) {
            case 3:
            { // slice it and do the process to each branch.
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
                
            }
            case 2:
            {
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
                switch (filt) {
                    case Hanning:
                        filter= FFTUtil.getWindowHanning(len);
                        break;
                    case Hann:
                        filter= FFTUtil.getWindowHanning(len);
                        break;
                    case TenPercentEdgeCosine:
                        filter= FFTUtil.getWindow10PercentEdgeCosine(len);
                        break;
                    case Unity:
                        filter= FFTUtil.getWindowUnity(len);
                        break;
                    default:
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
                        
                        // Because dep0!=null and dep1!=null.
                        dep0b.putValue(-1, dep0.value(i) + uc.convert( dep1.value( j*len + len/2 )  ) );
                        dep0b.nextRecord();
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
                result.putProperty( QDataSet.UNITS, ds.slice(0).property(QDataSet.UNITS ) );
                System.err.println( "*** Set units to "+ ds.slice(0).property(QDataSet.UNITS ) );
                return result;
                
            }
            default:
                throw new IllegalArgumentException("rank not supported: "+ ds.rank() );
        }

    }

    /**
     * Apply Hanning (Hann) windows to the data to prepare for FFT.  The 
     * data is reformed into a rank 2 dataset [N,len].  Hanning windows taper 
     * the ends of the interval to remove noise caused by the discontinuity.
     * This is deprecated, and windowFunction should be used.
     * @param ds, rank 1, 2, or 3 data
     * @param len
     * @return data[N,len] with the hanning window applied.
     * @see #windowFunction(org.das2.qds.ops.Ops.FFTFilterType, int) 
     */
    public static QDataSet hanning( QDataSet ds, int len ) {
        return fftFilter(  ds, len, FFTFilterType.Hanning );
    }

    /**
     * create a power spectrum on the dataset by breaking it up and
     * doing FFTs on each segment.  A unity (or "boxcar") window is used.
     *
     * data may be rank 1, rank 2, or rank 3.
     *
     * Looks for DEPEND_1.USER_PROPERTIES.FFT_Translation, which should
     * be a rank 0 or rank 1 QDataSet.  If it is rank 1, then it should correspond
     * to the DEPEND_0 dimension.
     *
     * @param ds rank 2 dataset ds(N,M) with M&gt;len
     * @param len the number of elements to have in each fft.
     * @param mon a ProgressMonitor for the process
     * @return rank 2 FFT spectrum
     */
    public static QDataSet fftPower( QDataSet ds, int len, ProgressMonitor mon ) {
        QDataSet unity= ones(len);
        return fftPower( ds, unity, 1, mon );
    }

    /**
     * return a dataset for the given filter type.  The result will be rank 1 and length len.
     * @param filt the type of the window.
     * @param len the length of the window.
     * @return rank 1 QDataSet with length len.
     */
    public static QDataSet windowFunction( FFTFilterType filt, int len ) {
        assert filt!=null;
        switch (filt) {
            case Hanning:
                return FFTUtil.getWindowHanning(len);
            case Hann:
                return FFTUtil.getWindowHanning(len);
            case TenPercentEdgeCosine:
                return FFTUtil.getWindow10PercentEdgeCosine(len);
            case Unity:
                return FFTUtil.getWindowUnity(len);
            case Boxcar:
                return FFTUtil.getWindowUnity(len);
            default:
                throw new UnsupportedOperationException("unsupported op: "+filt );
        }
    }

    /**
     * perform the fft with the window, using no overlap.
     * @param ds rank 1,2 or 3 waveform dataset.
     * @param window the window
     * @param mon a ProgressMonitor for the process
     * @return rank 2 fft spectrum
     * @see #fftPower(org.das2.qds.QDataSet, org.das2.qds.QDataSet, int, org.das2.util.monitor.ProgressMonitor)      
     * @see #windowFunction(org.das2.qds.ops.Ops.FFTFilterType, int) 
     */
    public static QDataSet fftPower( QDataSet ds, QDataSet window, ProgressMonitor mon ) {
        return fftPower( ds, window, 1, mon );
    }
    
    /**
     * fftPower that matches the filter call (|fftPower(ds,len,stepFraction,windowName)).
     * @param ds rank 2 dataset ds(N,M) with M&gt;len
     * @param windowLen the length of the window.
     * @param stepFraction size, expressed as a fraction of the length (1 for no slide, 2 for half steps, 4 for quarters)
     * @param windowName name for the window, including "Hanning" "Hann" "TenPercentEdgeCosine", "Unity", "Boxcar"
     * @param mon a ProgressMonitor for the process
     * @return rank 2 fft spectrum
     * @see #fftPower(org.das2.qds.QDataSet, org.das2.qds.QDataSet, int, org.das2.util.monitor.ProgressMonitor)
     */
    public static QDataSet fftPower( QDataSet ds, int windowLen, int stepFraction, String windowName, ProgressMonitor mon ) {
        QDataSet window= windowFunction( FFTFilterType.valueOf(windowName), windowLen );
        return fftPower( ds, window, stepFraction, mon );
    }
    
    /**
     * return the property, issuing a warning and returning null when the 
     * property value is not of the correct type.
     * @param <T> the property value
     * @param ds the dataset
     * @param propertyName the property name
     * @param clazz the class of the property.
     * @return the property, guaranteed to be of the correct type, or null.
     * @throws IllegalArgumentException if the class given is not of the right type for the property.
     */
    public static <T> T getProperty( QDataSet ds, String propertyName, Class<T> clazz) {
        Class correctClass= DataSetUtil.getPropertyClass(propertyName);
        if ( !clazz.isAssignableFrom(correctClass) ) {
            throw new IllegalArgumentException("requested class is not of correct type: "+clazz+" (should be "+correctClass+")" );
        }
        Object o= ds.property( propertyName );
        if ( o==null ) {
            return null;
        } else {
            if ( clazz.isInstance(o) ) {
                return clazz.cast(o);
            } else {
                logger.log(Level.WARNING, "ds property is not of the correct type: {0} have {1}, want {2}", 
                        new Object[] { propertyName, o.getClass(), clazz } );
                return null;
            }
        }
    } 
    
    /**
     * create a power spectrum on the dataset by breaking it up and
     * doing FFTs on each segment.
     *
     * data may be rank 1, rank 2, or rank 3.
     *
     * Looks for DEPEND_1.USER_PROPERTIES.FFT_Translation, which should
     * be a rank 0 or rank 1 QDataSet.  If it is rank 1, then it should correspond
     * to the DEPEND_0 dimension.  This is used to indicate that the waveform
     * collected with respect to a carrier tone, and the result should be translated.
     *
     * No normalization is done with non-unity windows.  TODO: This probably should be done.  
     * I verified this is not done, see 
     * https://github.com/autoplot/dev/bugs/sf/1317/testWindowFunctionNormalization.jy
     * TODO: This should be rechecked.  I'm pretty sure it's done.
     *
     * @param ds rank 2 dataset ds(N,M) with M&gt;len, rank 3 with the same cadence, or rank 1.
     * @param window window to apply to the data before performing FFT (Hann,Unity,etc.)
     * @param stepFraction size, expressed as a fraction of the length (1 for no slide, 2 for half steps, 4 for quarters)
     * @param mon a ProgressMonitor for the process
     * @return rank 2 FFT spectrum, or rank 3 if the rank 3 input has differing cadences.
     * @SuppressWarnings("unchecked") 
     */
    public static QDataSet fftPower( QDataSet ds, QDataSet window, int stepFraction, ProgressMonitor mon ) {
        if ( mon==null ) {
            mon= new NullProgressMonitor();
        }

        String title= (String) ds.property(QDataSet.TITLE);
        if ( title!=null ) {
            title= "FFTPower of "+title;
        }
        
        Map userProperties= getProperty( ds, QDataSet.USER_PROPERTIES, Map.class );
        
        if ( ds.rank()==1 ) { // wrap to make rank 2
            QDataSet c= (QDataSet) ds.property( QDataSet.CONTEXT_0 );
            if ( c!=null && SemanticOps.getUnits(c).equals(Units.dimensionless) ) {
                c= null;
            }
            JoinDataSet dep0;
            Units dep0u;
            JoinDataSet jds= new JoinDataSet(ds);
            if ( c!=null && c.rank()==0 ) {
                dep0u= (Units) c.property(QDataSet.UNITS);
                dep0= new JoinDataSet(c);
                QDataSet dep1= (QDataSet)ds.property(QDataSet.DEPEND_0);
                if ( dep0u!=null ) {
                    Units dep1u= SemanticOps.getUnits(dep1);
                    dep0.putProperty( QDataSet.UNITS, dep0u );
                    if ( dep1u.getOffsetUnits().isConvertibleTo(dep0u) ) {
                        jds.putProperty( QDataSet.DEPEND_0, dep0 );
                    }
                }
            }

            ds= jds;
        } 

        switch (ds.rank()) {
            case 3:
            { // slice it and do the process to each branch.
                JoinDataSet result= new JoinDataSet(3);
                mon.setTaskSize( ds.length()*10  );
                mon.started();
                Datum lastCadence=null;
                boolean sameCadence= true;
                int recCount= 0;
                for ( int i=0; i<ds.length(); i++ ) {
                    mon.setTaskProgress(i*10);
                    QDataSet pow1= fftPower( ds.slice(i), window, stepFraction, SubTaskMonitor.create( mon, i*10, (i+1)*10 ) );
                    recCount+= pow1.length();
                    if ( lastCadence==null ) {
                        lastCadence= DataSetUtil.asDatum( ((QDataSet)pow1.property(QDataSet.DEPEND_1)).slice(0) );
                    } else {
                        if ( ! DataSetUtil.asDatum( ((QDataSet)pow1.property(QDataSet.DEPEND_1)).slice(0) ).equals(lastCadence) ) {
                            sameCadence= false;
                        }
                    }
                    result.join(pow1);
                }
                if ( sameCadence ) {
                    QDataSet dep1= (QDataSet)result.slice(0).property(QDataSet.DEPEND_1);
                    JoinDataSet newResult= new JoinDataSet(2);
                    DataSetBuilder xdsb= new DataSetBuilder(1,recCount);
                    for ( int i=0; i<result.length(); i++ ) {
                        QDataSet result1= result.slice(i);
                        QDataSet dep0= (QDataSet)result1.property(QDataSet.DEPEND_0);
                        for ( int j=0; j<result1.length(); j++ ) {
                            newResult.join(result1.slice(j));
                            xdsb.nextRecord(dep0.slice(j));
                        }
                    }
                    result= newResult;
                    result.putProperty( QDataSet.DEPEND_0, xdsb.getDataSet() );
                    result.putProperty( QDataSet.DEPEND_1, dep1 );
                }
                mon.finished();
                
                result.putProperty( QDataSet.QUBE, Boolean.TRUE );
                result.putProperty( QDataSet.SCALE_TYPE, QDataSet.VALUE_SCALE_TYPE_LOG );
                if ( title!=null ) result.putProperty( QDataSet.TITLE, title );
                if ( userProperties!=null ) result.putProperty( QDataSet.USER_PROPERTIES, userProperties );
                
                return result;
                
            }
            case 2:
            {
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
                
                double normalization; // the normalization needed because of the window. "Coherent Gain" in Harris paper.
                
                if ( windowNonUnity ) {
                    normalization= total( Ops.pow( window, 2 ) ) / window.length();
                } else {
                    normalization= 1.0;
                }
                
                // double equivalentNoiseBW= 1.0;  // 1.5 for Hanning   Harris 1978 paper in IEEE.
                
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
                if ( dep1==null ) {
                    throw new IllegalArgumentException("fftPower cannot be performed without independent parameter tags");
                }
                
                UnitsConverter uc= UnitsConverter.IDENTITY;
                
                QDataSet translation= null;
                Map user= userProperties;
                if ( user!=null ) {
                    translation= (QDataSet) user.get( "FFT_Translation" ); // kludge for Plasma Wave Group
                    if ( translation!=null && translation.rank()==1 ) {
                        if ( dep0!=null && translation.length()!=dep0.length() ) {
                            throw new IllegalArgumentException("rank 1 FFT_Translation should be the same length as depend_0");
                        }
                    }
                }
                if ( translation!=null ) logger.fine("translation will be applied");
                
                double currentDeltaTime; // ten times the spacing.
                if ( dep1.rank()==1 ) {
                    currentDeltaTime= dep1.value(10) - dep1.value(0);
                } else {
                    currentDeltaTime= dep1.value(0,10) - dep1.value(0,0);
                }
                double lastDeltaTime= currentDeltaTime;
                
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
                int len1D= ds.length(0) / step;
                
                mon.setTaskSize(ds.length()*len1D); // assumes all are the same length.
                mon.started();
                mon.setProgressMessage("performing fftPower");
                
                boolean isMono= dep0==null ? true : DataSetUtil.isMonotonic(dep0);
                
                QDataSet lastTranslation= null; // when translation is applied, keep track of last one
                
                QDataSet nextSlice= null;
                for ( int i=0; i<ds.length(); i++ ) {
                    QDataSet slicei;
                    if ( nextSlice!=null ) {
                        slicei= nextSlice;
                        nextSlice= null;
                    } else {
                        slicei= ds.slice(i);
                    } //TODO: for DDataSet, this copies the backing array.  This shouldn't happen in DDataSet.slice, but it does...
                    QDataSet dep0i= (QDataSet) slicei.property(QDataSet.DEPEND_0);
                    if ( dep0i!=null && dep0==null ) {
                        dep0b.putProperty(QDataSet.UNITS, dep0i.property(QDataSet.UNITS) );
                        if ( !Boolean.TRUE.equals( dep0i.property(QDataSet.MONOTONIC) ) ) {
                            isMono= false;
                        }
                        if ( dep0i.property(QDataSet.VALID_MIN)!=null ) minD= ((Number)dep0i.property(QDataSet.VALID_MIN)).doubleValue(); else minD= Double.NEGATIVE_INFINITY;
                        if ( dep0i.property(QDataSet.VALID_MAX)!=null ) maxD= ((Number)dep0i.property(QDataSet.VALID_MAX)).doubleValue(); else maxD= Double.POSITIVE_INFINITY;
                    }
                    
                    for ( int j=0; j<len1D; j++ ) {
                        
                        mon.setTaskProgress(i*len1D+j);
                        
                        int istart= j*step;
                        GeneralFFT fft = GeneralFFT.newDoubleFFT(len);
                        QDataSet wave;
                        QDataSet offs;
                        
                        if ( istart+len <= slicei.length() ) {
                            wave= slicei.trim( istart,istart+len );
                            if ( dep1.rank()==1 ) {
                                offs= dep1.trim( istart,istart+len );
                            } else {
                                offs= dep1.slice(i).trim( istart,istart+len );
                            }
                        } else {
                            if ( i+1<ds.length() ) { // we can get this record.
                                if ( nextSlice==null ) {
                                    nextSlice= ds.slice(i+1);
                                }
                                //TODO: check timetag
                                QDataSet wave2= nextSlice.trim( 0, istart+len-slicei.length() );
                                wave= append( slicei.trim( istart, slicei.length() ), wave2 );
                                QDataSet offs1, offs2;
                                assert dep0!=null;
                                if ( dep1.rank()==1 ) {
                                    offs1= dep1.trim( istart, slicei.length() );
                                    offs2= Ops.add( Ops.subtract( dep0.slice(i+1), dep0.slice(i) ), 
                                            dep1.trim( 0, istart+len-slicei.length() ) );
                                } else {
                                    offs1= dep1.slice(i).trim( istart, slicei.length() );
                                    offs2= Ops.add( Ops.subtract( dep0.slice(i+1), dep0.slice(i) ), 
                                            dep1.slice(i+1).trim( 0, istart+len-slicei.length() ) );
                                }
                                offs= append( offs1, offs2 );
                            } else {
                                wave= null;
                                offs= null;
                            }
                        }
                        
                        if ( wave==null || offs==null ) continue;
                        
                        QDataSet wds= DataSetUtil.weightsDataSet(wave);
                        
                        double d0=0;
                        if ( dep0!=null ) {
                            d0= dep0.value(i) + uc.convert( offs.value( len/2 )  );
                        } else if ( dep0i!=null ) {
                            d0= dep0i.value(j*step+len/2);
                        } else {
                            dep0b= null;
                        }                        
                        
                        boolean hasFill= false;
                        for ( int k=0; k<wds.length(); k++ ) {
                            if ( wds.value(k)==0 ) {
                                hasFill= true;
                            }
                        }
                        if ( hasFill ) {
                            logger.log(Level.FINER, "fill at {0}", Units.us2000.createDatum(d0));
                            continue;
                        }
                        
                        double switchCadenceCheck; // the cadence at the end of the interval.
                        currentDeltaTime= offs.value(10) - offs.value(0);
                        switchCadenceCheck=  dep1.value(len-1) - dep1.value(len-11);

                        if ( false ) {
                            new java.io.File("/tmp/ap/").mkdirs();
                            try ( PrintWriter write= 
                                new PrintWriter( 
                                    new PrintWriter( String.format( "/tmp/ap/%09d.txt", j ) ) ) ) {
                                write.println( String.format( "# %f %f %f", 
                                    switchCadenceCheck, currentDeltaTime, Math.abs( switchCadenceCheck-currentDeltaTime ) / currentDeltaTime ) );
                                for ( int iz=0; iz<offs.length(); iz++ ) {
                                    write.print( Ops.datum( Ops.subtract( offs.slice(iz), offs.slice(0) ) ).doubleValue( Units.microseconds) );
                                    write.print( " " );
                                    write.print( Math.abs( switchCadenceCheck-currentDeltaTime ) / currentDeltaTime );
                                    write.print( "\n" );
                                } 
                            } catch (FileNotFoundException ex) {
                                Logger.getLogger(Ops.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                            
                        // does the cadence change between packets?
                        boolean cadenceChangeDetect= Math.abs( switchCadenceCheck-currentDeltaTime ) / currentDeltaTime > 0.01;
                        if ( cadenceChangeDetect ) {
                            if ( translation!=null ) {
                                logger.finer("cadence changes but translation allows");
                            } else {
                                logger.finer("cadence changes");
                                continue;
                            }
                        }

                        // is there a gap within the packet?
                        double avgCadence= ( offs.value(len-1) - offs.value(0) ) / ( len-1 );
                        if ( Math.abs( switchCadenceCheck/10-avgCadence ) / currentDeltaTime > 0.01 ) {
                            if ( translation!=null ) {
                                logger.finer("gap detected but translation allows");
                            } else {
                                logger.finer("gap detected");
                                continue;
                            }
                        }
                        currentDeltaTime= offs.value(10) - offs.value(0);
                        
                        boolean newTranslationMode= translation!=null && lastTranslation!=null &&
                            ( !translation.slice(istart).equals(lastTranslation) );
                        if ( newTranslationMode || Math.abs( lastDeltaTime-currentDeltaTime ) / currentDeltaTime > 0.01 ) {
                            QDataSet powxtags1= FFTUtil.getFrequencyDomainTagsForPower(dep1.trim(istart,istart+len));
                            if ( translation!=null ) {
                                switch (translation.rank()) {
                                    case 0:
                                        powxtags1= Ops.add( powxtags1, translation );
                                        lastTranslation= translation;
                                        break;
                                    case 1:
                                        powxtags1= Ops.add( powxtags1, translation.slice(istart) );
                                        lastTranslation= translation.slice(istart);
                                        break;
                                    default:
                                        throw new IllegalArgumentException("bad rank on FFT_Translation, expected rank 0 or rank 1");
                                }
                            }
                            QDataSet ytags= (QDataSet) result.property(QDataSet.DEPEND_1);
                            if ( ytags instanceof CdfSparseDataSet ) {
                                ((CdfSparseDataSet)ytags).putValues( result.length(), powxtags1 );
                            } else {
                                if ( ytags==null ) {
                                    ytags= (QDataSet)result.slice(0).property(QDataSet.DEPEND_0);
                                } 
                                CdfSparseDataSet newYtags= new CdfSparseDataSet(2,ds.length()*len1,ytags);
                                newYtags.putValues(result.length(),powxtags1);
                                newYtags.putProperty(QDataSet.UNITS,powxtags1.property(QDataSet.UNITS));
                                ytags= newYtags;
                                result.putProperty( QDataSet.DEPEND_1, ytags );
                            }
                            powxtags= powxtags1;
                            lastDeltaTime= currentDeltaTime;
                            
                        }
                        
                        //if ( windowNonUnity ) {
                        //    wave= Ops.multiply(wave,window);
                        //}
                        
                        
                        QDataSet vds= FFTUtil.fftPower( fft, wave, window, powxtags );
                        //QDataSet vds= FFTUtil.fftPower( fft, wave );
                        
                        if ( windowNonUnity ) {
                            vds= Ops.multiply( vds, DataSetUtil.asDataSet( 1/normalization ) );
                        }
                        
                        if ( translation!=null ) {
                            QDataSet fftDep1= (QDataSet) vds.property( QDataSet.DEPEND_0 );
                            switch (translation.rank()) {
                                case 0:
                                    fftDep1= Ops.add( fftDep1, translation );
                                    break;
                                case 1:
                                    fftDep1= Ops.add( fftDep1, translation.slice(istart) );
                                    System.err.println("" + istart + ": "+translation.slice(istart) );
                                    break;
                                default:
                                    throw new IllegalArgumentException("bad rank on FFT_Translation, expected rank 0 or rank 1");
                            }
                            ((MutablePropertyDataSet)vds).putProperty( QDataSet.DEPEND_0, fftDep1 );
                        }
                                                                        
                        if ( d0>=minD && d0<=maxD) {
                            if ( translation==null ) {
                                ((MutablePropertyDataSet)vds).putProperty( QDataSet.DEPEND_0, null ); // save space wasted by redundant freq tags.
                            }
                            result.join(vds);
                            if ( dep0b!=null ) {
                                dep0b.putValue(-1, d0 );
                                dep0b.nextRecord();
                            }
                        } else {
                            System.err.println("dropping record with invalid timetag: "+d0 ); //TODO: consider setting VALID_MIN, VALID_MAX instead...
                        }
                        
                        if ( mon.isCancelled() ) throw new UncheckedCancelledOperationException("fftPower was cancelled");
                    }
 
                }
                mon.finished();
                
                QDataSet dep1_= (QDataSet) result.property(QDataSet.DEPEND_1);
                if ( dep1_==null ) {
                    dep1_= (QDataSet)result.slice(0).property(QDataSet.DEPEND_0);
                }
                if ( dep1_!=null && dep1_.rank()==2 && dep1_.length()!=result.length() ) {
                    ((CdfSparseDataSet)dep1_).setLength(result.length()); // seems cheesy but it's true!
                }
                if ( dep0!=null && dep0b!=null ) {
                    dep0b.putProperty(QDataSet.UNITS, dep0.property(QDataSet.UNITS) );
                    if ( isMono ) dep0b.putProperty(QDataSet.MONOTONIC,true);
                    result.putProperty(QDataSet.DEPEND_0, dep0b.getDataSet() );
                } else if ( dep0b!=null ) {
                    if ( isMono ) dep0b.putProperty(QDataSet.MONOTONIC,true);
                    result.putProperty(QDataSet.DEPEND_0, dep0b.getDataSet() );
                }
                
                if ( title!=null ) result.putProperty( QDataSet.TITLE, title );
                if ( userProperties!=null ) result.putProperty( QDataSet.USER_PROPERTIES, userProperties );
                
                if ( translation!=null && result.property(QDataSet.DEPEND_1)==null ) { //TODO: huh?
                    JoinDataSet dep1jds= new JoinDataSet(2);
                    for ( int i=0; i<result.length(); i++ ) {
                        QDataSet sl1= (QDataSet)result.slice(i).property(QDataSet.DEPEND_0);
                        dep1jds.join( sl1 );
                    }
                    dep1_= dep1jds;
                    result.putProperty( QDataSet.DEPEND_1, dep1_ );
                }
                
                if ( dep1_.rank()==1 ) {
                    result.putProperty( QDataSet.QUBE, Boolean.TRUE );
                }
                
                result.putProperty( QDataSet.SCALE_TYPE, QDataSet.VALUE_SCALE_TYPE_LOG );
                return result;
                
            }
            default:
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
     * @return rank 1 dataset, or rank 2 for rank 2 input.
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
            result.putValue(i, (i==0?1:4)*ComplexArray.magnitude2(ca,i) / binsize ); //TODO: why 2 and not 4?  Chris' study suggests 2 is correct.  http://www-pw.physics.uiowa.edu/~jbf/rbsp/script/chris/powerfft/checkPowerFFT.jy
            resultDep0.putValue( i, xtags[i] );
        }
        if ( invUnits!=null ) resultDep0.putProperty( QDataSet.UNITS, invUnits );

        result.putProperty(QDataSet.DEPEND_0, resultDep0);
        return result;

    }
    /**
     * Performs an FFT on the provided rank 1 dataset.  A rank 2 dataset of 
     * complex numbers is returned.  The data must not contain fill and
     * must be uniformly spaced.  DEPEND_0 is used to identify frequencies
     * if available.
     * @param ds a rank 1 dataset.
     * @return a rank 2 dataset of complex numbers.
     * @see Schemes#rank2ComplexNumbers() 
     * @see Ops#ifft(org.das2.qds.QDataSet) 
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
        if ( dep0==null ) dep0= Ops.findgen(ds.length());

        QDataSet tags = FFTUtil.getFrequencyDomainTags(dep0);
        result.putProperty(QDataSet.DEPEND_0, tags );

        QDataSet dep1 = complexCoordinateSystem();

        result.putProperty(QDataSet.DEPEND_1, dep1);
        return result;
    }

    /**
     * Performs an inverse FFT on the provided rank 2 dataset of complex numbers.  
     * A rank 2 dataset of complex numbers is returned.
     * @param ds a rank 2 dataset.
     * @return a rank 2 dataset of complex numbers.
     * @see Ops#fft(org.das2.qds.QDataSet) 
     */
    public static QDataSet ifft(QDataSet ds) {
        GeneralFFT fft = GeneralFFT.newDoubleFFT(ds.length());
        
        ComplexArray.Double cc = FFTUtil.ifft(fft, ds);
        
        DDataSet result = DDataSet.createRank2(ds.length(), 2);
        for (int i = 0; i < ds.length(); i++) {
            result.putValue(i, 0, cc.getReal(i));
            result.putValue(i, 1, cc.getImag(i));
        }

        QDataSet dep0 = (QDataSet) ds.property(QDataSet.DEPEND_0);
        if ( dep0!=null ) {
            QDataSet tags= FFTUtil.getTimeDomainTags(dep0);
            //double[] tags = FFTUtil.getFrequencyDomainTags(1./cadence.value(), ds.length());
            result.putProperty(QDataSet.DEPEND_0, tags );
        }

        QDataSet dep1 = complexCoordinateSystem();
        result.putProperty(QDataSet.DEPEND_1, dep1);
        
        return result;
    }

    /**
     * return the complex conjugate of the rank 1 or rank 2 QDataSet.
     * @param ds ds[2] or ds[n,2]
     * @return ds[2] or ds[n,2]
     * @see #complexMultiply(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static final QDataSet complexConj( QDataSet ds ) {
        QDataSet dep1= complexCoordinateSystem();
        if ( ds.rank()==1 ) {
            ArrayDataSet result= ArrayDataSet.copy(ds);
            result.putValue( 1, -1*ds.value(1) );
            result.putProperty( QDataSet.DEPEND_0, dep1 );
            return result;
        } else {
            ArrayDataSet result= ArrayDataSet.copy(ds);
            for ( int i=0; i<ds.length(); i++ ) {
                result.putValue( i, 1, -1*ds.value( i, 1 ) );
            }
            result.putProperty( QDataSet.DEPEND_1, dep1 );
            return result;
        }
    }
    
    /**
     * perform complex multiplication, where the two datasets must have the same
     * rank and must both end with a complex dimension.
     * @param ds1 ds[2] or ds[n,2] or ds[n,m,2]
     * @param ds2 ds[2] or ds[n,2] or ds[n,m,2]
     * @return ds[2] or ds[n,2] or ds[n,m,2]
     * @see #complexConj(org.das2.qds.QDataSet) 
     */
    public static final QDataSet complexMultiply( QDataSet ds1, QDataSet ds2 ) {
        if ( ds1.rank()>3 ) throw new IllegalArgumentException("ds1 must be ds1[n,2]");
        if ( !Schemes.isComplexNumbers(ds1) ) ds1= complexDataset( ds1, null );
        if ( !Schemes.isComplexNumbers(ds2) ) ds2= complexDataset( ds2, null );
        if ( ds1.rank()==1 && ds2.rank()==2 ) ds1= replicate( ds1, ds2.length() );
        if ( ds1.rank()==2 && ds1.rank()==1 ) ds2= replicate( ds2, ds1.length() );
        if ( ds1.rank()!=ds2.rank() ) throw new IllegalArgumentException("ds1 and ds2 must have the same rank");
        QDataSet dep1= complexCoordinateSystem();
        ArrayDataSet result= ArrayDataSet.copy(ds1);
        switch (ds1.rank()) {
            case 1: {
                result.putValue( 0, ds1.value(0)*ds2.value(0) - ds1.value(1)*ds2.value(1) );
                result.putValue( 1, ds1.value(0)*ds2.value(1) + ds1.value(1)*ds2.value(0) );
                result.putProperty( QDataSet.DEPEND_0, dep1 );
                break;
            }
            case 2: {
                for ( int i=0; i<ds1.length(); i++ ) {
                    result.putValue( i, 0, ds1.value(i,0)*ds2.value(i,0) - ds1.value(i,1)*ds2.value(i,1) );
                    result.putValue( i, 1, ds1.value(i,0)*ds2.value(i,1) + ds1.value(i,1)*ds2.value(i,0) );
                }
                result.putProperty( QDataSet.DEPEND_1, dep1 );
                break;
            }
            case 3: {
                for ( int i=0; i<ds1.length(); i++ ) {
                    for ( int j=0; j<ds1.length(i); j++ ) {
                        result.putValue( i, j, 0, ds1.value(i,j,0)*ds2.value(i,j,0) - ds1.value(i,j,1)*ds2.value(i,j,1) );
                        result.putValue( 1, j, 1, ds1.value(i,j,0)*ds2.value(i,j,1) + ds1.value(i,j,1)*ds2.value(i,j,0) );
                    }
                }
                result.putProperty( QDataSet.DEPEND_2, dep1 );
                break;
            }
            default:
                throw new IllegalArgumentException("rank");
        }
        return result;        
    }
    
    /**
     * apply the cross product of a and b, where a or b may be rank 1, three-element vector,
     * or both can be vector arrays of the same length.  In the case where only X and Y are provided (two-element vectors
     * instead of three), then Z is automatically assumed to be zero.
     * @param a rank 1 ds[3] or rank 2 ds[n,3]
     * @param b rank 1 ds[3] or rank 2 ds[n,3]
     * @return ds[3] or rank 2 ds[n,3]
     */
    public static QDataSet crossProduct( QDataSet a, QDataSet b ) {
        QDataSet ax,bx;
        QDataSet ay,by;
        QDataSet az,bz;

        if ( a.rank()==1 && b.rank()==1 ) {
            ax= a.slice(0);
            ay= a.slice(1);
            az= a.length()==2 ? Ops.dataset(0) : a.slice(2);
            bx= b.slice(0);
            by= b.slice(1);
            bz= b.length()==2 ? Ops.dataset(0) : b.slice(2);

        } else if ( a.rank()==1 ) {
            ax= a.slice(0);
            ay= a.slice(1);
            az= a.length()==2 ? Ops.dataset(0) : a.slice(2);
            bx= Ops.unbundle(b,0);
            by= Ops.unbundle(b,1);
            bz= b.length(0)==2 ? Ops.dataset(0) : Ops.unbundle(b,2);

        } else if ( b.rank()==1 ) {
            ax= Ops.unbundle(a,0);
            ay= Ops.unbundle(a,1);
            az= a.length(0)==2 ? Ops.dataset(0) : Ops.unbundle(a,2);
            bx= b.slice(0);
            by= b.slice(1);
            bz= b.length()==2 ? Ops.dataset(0) : b.slice(2);
            
        } else if ( a.rank()==2 && b.rank()==2 ) {
            ax= Ops.unbundle(a,0);
            ay= Ops.unbundle(a,1);
            az= a.length(0)==2 ? Ops.dataset(0) : Ops.unbundle(a,2);
            bx= Ops.unbundle(b,0);
            by= Ops.unbundle(b,1);
            bz= b.length(0)==2 ? Ops.dataset(0) : Ops.unbundle(b,2);
            
        } else {
            throw new IllegalArgumentException("a and b must be either 3-element rank 1 datasets, or rank 2 n by 3-elements");
        }
        
        QDataSet cx = Ops.subtract( Ops.multiply( ay, bz ), Ops.multiply( az, by ) );
        QDataSet cy = Ops.subtract( Ops.multiply( az, bx ), Ops.multiply( ax, bz ) );
        QDataSet cz = Ops.subtract( Ops.multiply( ax, by ), Ops.multiply( ay, bx ) );

        QDataSet tt= (QDataSet)cx.property( QDataSet.DEPEND_0 );
        
        return Ops.link( tt, Ops.bundle( cx, cy, cz ) );
    }
    
    private static QDataSet complexCoordinateSystem() {
        return Schemes.complexCoordinateSystemDepend();
    }
        
    /**     
     * create a complex dataset.
     * @param realPart the real component.
     * @param imaginaryPart the complex component.
     * @return complex dataset
     * @see org.das2.qds.examples.Schemes#rank2ComplexNumbers() 
     */
    public static QDataSet complexDataset( QDataSet realPart, QDataSet imaginaryPart ) {
        if ( imaginaryPart==null ) {
            ArrayDataSet im= DDataSet.createRank0();
            im.putValue(0);
            imaginaryPart= im;
        }
        QDataSet[] operands= new QDataSet[2];
        CoerceUtil.coerce( realPart, imaginaryPart, false, operands );
        realPart= operands[0];
        imaginaryPart= operands[1];
        QDataSet result= Ops.bundle( realPart, imaginaryPart );
        MutablePropertyDataSet result1= Ops.putProperty( result, "DEPEND_"+(result.rank()-1), complexCoordinateSystem() );
        DataSetUtil.copyDimensionProperties( realPart, result1 );
        return result1;
    }

    /**
     * special function needed by the RPW Group at U. Iowa, which reassigns timetags so the small waveform 
     * packets are visible.
     * @param ds rank 2 waveform
     * @return 
     * @see Schemes#rank2Waveform() 
     * @see #expandToFillGaps(org.das2.qds.QDataSet) 
     */
    public static QDataSet expandWaveform( QDataSet ds ) {
        if ( !Schemes.isRank2Waveform(ds) ) {
            throw new IllegalArgumentException("dataset must be a rank 2 waveform");
        }
        QDataSet deltaT= (QDataSet)ds.property(QDataSet.DEPEND_1);
        QDataSet baseT= (QDataSet)ds.property(QDataSet.DEPEND_0);
        QDataSet cadence= Ops.reduceMin( Ops.diff(baseT), 0 );
        QDataSet extent= Ops.extent(deltaT);
        QDataSet qf= Ops.multiply( Ops.divide( cadence, ( DataSetUtil.asDatumRange(extent).width() ) ), 0.95 );
        deltaT= Ops.multiply( deltaT, qf );
        ds= Ops.link( baseT, deltaT, ds );
        return ds;
    }

    /**
     * Special function by the RPW Group at U. Iowa, which reassigns timetags so the small waveform 
     * packets are visible, or bursty spectrograms are more easily viewed.  This just calls
     * expandToFillGaps with a 90 percent overlap (expandToFillGaps( ds, 0.9 )).
     * @param ds the dataset
     * @return the dataset with new DEPEND_0 values.
     * @see #expandWaveform(org.das2.qds.QDataSet) 
     */
    public static QDataSet expandToFillGaps( QDataSet ds ) {
        return expandToFillGaps( ds, 0.9 );
    }
    
    /**
     * Special function by the RPW Group at U. Iowa, which reassigns timetags so the small waveform 
     * packets are visible, or bursty spectrograms are more easily viewed.  When there are gaps
     * longer than twiceCadenceMin, multiply the timetags preceding by multiplier to fill the gap.
     * @param ds the data, presumably containing burst of continuous packets between long gaps.
     * @param cadenceMin sets threshold to identify continuous bursts of packets.
     * @param multiplier the scale to by which to expand the packet into the gap.
     * @return 
     * @see #expandToFillGaps(org.das2.qds.QDataSet, double) 
     */
    public static QDataSet expandToFillGaps( QDataSet ds, Datum cadenceMin, double multiplier ) {
        QDataSet ttags= (QDataSet) ds.property(QDataSet.DEPEND_0);
        Units toffUnits= ((Units)ttags.property(QDataSet.UNITS)).getOffsetUnits();
        QDataSet dts= Ops.abs( Ops.diff(ttags) );
        int basei= 0;
        Datum baset= Ops.datum( ttags.slice(0) );
                
        DataSetBuilder tb= new DataSetBuilder(1,ds.length());
        tb.setUnits((Units)ttags.property(QDataSet.UNITS));
                
        tb.putValue( 0, baset );
        
        Datum twiceCadenceMin= cadenceMin.multiply(2);
        
        for ( int i=1; i<ttags.length(); i++ ) {
            if ( Ops.datum(dts.slice(i-1)).lt(twiceCadenceMin) ) {
                tb.putValue( i, baset.add( ( ttags.value(i)-ttags.value(basei) ) * multiplier, toffUnits ) );
            } else {
                baset= Ops.datum( ttags.slice(i) );
                basei= i;
                tb.putValue( i, baset );                
            }
        }
        
        QDataSet newTTags= tb.getDataSet();
        return link( newTTags, ds );

    }
    
    /**
     * Special self-configuring function by the RPW Group at U. Iowa, which reassigns timetags so the small waveform 
     * packets are visible, or bursty spectrograms are more easily viewed.
     * @param ds the data, presumably containing burst of continuous packets between long gaps.
     * @param factor duty cycle factor (0.5=50% duty cycle)
     * @return ds with new timetags.
     * @see #expandWaveform(org.das2.qds.QDataSet)
     * @see #expandToFillGaps(org.das2.qds.QDataSet, org.das2.datum.Datum, double) 
     */
    public static QDataSet expandToFillGaps( QDataSet ds, double factor ) {
        if ( Schemes.isRank2Waveform(ds) ) {
            return expandWaveform(ds);
        } else if ( ds.rank()==2 ) {
            QDataSet ttags= (QDataSet)ds.property(QDataSet.DEPEND_0);
            QDataSet dts= Ops.abs( Ops.diff(ttags) );
            Datum cadenceMin= Ops.datum( Ops.reduceMin( dts, 0 ) );
            Datum twiceCadenceMin= cadenceMin.multiply(2);
            QDataSet r= Ops.where( Ops.gt( dts, twiceCadenceMin ) );  // number of gaps
            if ( r.length()<1 ) {
                return ds;
            } else {
                int[] startIndexes= new int[r.length()+1];
                startIndexes[0]= 0;
                Datum cadenceMax= null; // cadenceMax is the smallest of the big jumps.
                int count= 0;  // the number of points in the burst of packets, where we found the smallest of the big gaps.
                for ( int i=0; i<r.length(); i++ ) {
                    startIndexes[i+1]= (int)r.value(i);
                    Datum cadence= datum( subtract( ttags.slice(startIndexes[i+1]+1), ttags.slice(startIndexes[i]) ) ); // big gap size
                    if ( cadenceMax==null || cadence.lt(cadenceMax) ) {
                        cadenceMax= cadence;
                        count= startIndexes[i+1]+1 - startIndexes[i];
                    }
                }
                assert cadenceMax!=null;
                logger.log(Level.FINE, "expandToFillGaps: cadenceMin={0} cadenceMax={1}", new Object[]{cadenceMin, cadenceMax});
                DataSetBuilder tb= new DataSetBuilder(1,ds.length());
                tb.setUnits((Units)ttags.property(QDataSet.UNITS));
                double stepFactor= cadenceMax.divide(cadenceMin).divide(count).value(); // step factor would fill the smallest gap.
                return expandToFillGaps( ds, cadenceMin, stepFactor * factor );
            }
        } else {
            throw new IllegalArgumentException("data must be rank 2 spectrogram or waveform");
        }
    }
    
    /**
     * scipy chirp function, used for testing.
     * @param t Times at which to evaluate the waveform.
     * @param df0 Frequency (e.g. Hz) at time t=0.
     * @param dt1 Time at which `f1` is specified.
     * @param df1 Frequency (e.g. Hz) of the waveform at time `t1`.
     * @return 
     */
    public static QDataSet chirp( QDataSet t, Datum df0, Datum dt1, Datum df1 ) {
        Units tu= SemanticOps.getUnits(t);
        if ( dt1.value()<=0 ) throw new IllegalArgumentException("dt1 must be greater than 0.");
        t= putProperty( copy(t), QDataSet.UNITS, null );
        double f0= df0.value();
        double f1= df1.value();
        double t1= dt1.value();
        double phi= 0;
        QDataSet beta = Ops.divide( Ops.subtract(f1,f0), t1 );
        QDataSet phase = multiply( dataset( 2 * PI ), ( add( multiply( t, f0 ) , multiply( multiply( 0.5, beta ), multiply( t , t ) ) ) ) );
        phi *= PI / 180;
        t= putProperty( t, QDataSet.UNITS, tu ); 
        return link( t, cos( add( phase, phi) ) );
    }
    
    /**
     * Perform the Hilbert function on the rank 1 dataset, similar to
     * the scipy.signal.hilbert function in SciPy.  The result is
     * form differently than hilbert.
     *
     * @param ds rank 1 dataset of length n.
     * @return ds[n,2], complex array
     * @see #hilbert(org.das2.qds.QDataSet) 
     */
    public static QDataSet hilbertSciPy( QDataSet ds ) {
        int N= ds.length();
        WritableDataSet ff= maybeCopy(fft(ds));
        double[] h= new double[N];
        if ( N%2==0 ) {
            h[0]= 1.;
            for ( int i=1; i<N/2; i++ ) h[i]= 2.;
            h[N/2]= 1.;
            for ( int i=N/2+1; i<N; i++ ) h[i]= 0.;  // It looks like the web indicates that local arrays are not initialized.
        } else {
            h[0]= 1.;
            for ( int i=1; i<N/2; i++ ) h[i]= 2.;
            for ( int i=N/2; i<N; i++ ) h[i]= 0.;  // It looks like the web indicates that local arrays are not initialized.
        }
        // swap the real and imaginary components, multiply by h.
        for ( int i=0; i<N; i++ ) {
            double t= ff.value( i,1 );
            ff.putValue( i, 0, h[i] * ff.value( i,0 ) );
            ff.putValue( i, 1, h[i] * t );
        }
        QDataSet result= ifft(ff);
        result= putProperty( result, QDataSet.DEPEND_0, ds.property(QDataSet.DEPEND_0 ) );
        return result;
    }
    
    /**
     * Perform the Hilbert function on the rank 1 dataset, similar to
     * the hilbert function in IDL and Matlab.  
     *
     * @param ds rank 1 dataset of length n.
     * @return ds[n,2], complex array
     * @see #hilbert(org.das2.qds.QDataSet) 
     */
    public static QDataSet hilbert( QDataSet ds ) {
        QDataSet ff= fft( ds );
        WritableDataSet h2= copy(ff);
        int n2= h2.length()/2;

        for ( int i=1; i<n2; i++ ) {
            h2.putValue(i,0,-1*ff.value(i,1));
            h2.putValue(i,1,ff.value(i,0));
        }
        int l= h2.length();
        for ( int i=n2; i<l; i++ ) {
            h2.putValue(i,0,ff.value(i,1));
            h2.putValue(i,1,-1*ff.value(i,0));
        }
        WritableDataSet h= maybeCopy(ifft(h2));
        DataSetUtil.putProperties( DataSetUtil.getProperties(ds), h );
        return h;
    }
    
    /**
     * SciPy's unwrap function, used in demo of hilbertSciPy, which unwraps
     * a function that exists in a modulo space so that the differences are 
     * minimal.
     * @param ds rank 1 dataset, containing values from 0 to discont
     * @param discont the discont, such as PI, TAU, 24, 60, 360, etc.
     * @return 
     */
    public static QDataSet unwrap( QDataSet ds, double discont ) {
        QDataSet d= diff( ds );
        Units u= SemanticOps.getUnits(d).getOffsetUnits();
        QDataSet h= dataset(discont/2,u);
        d= subtract( modp( add( d,h ), dataset(discont,u) ), h);
        WritableDataSet result= maybeCopy( Ops.append( Ops.join( null, ds.slice(0) ), accum( ds.slice(0), d )  ) );
        DataSetUtil.putProperties( DataSetUtil.getProperties(ds), result );
        return result;
    }
    
    /**
     * return true if the dataset can be interpreted as radian degrees from 0 to PI or from 0 to 2*PI.
     * @param ds any QDataSet.
     * @param strict return null if it's not clear that the units are degrees.
     * @return the multiplier to make the dataset into radians, or null.
     */
    public static Double isAngleRange( QDataSet ds, boolean strict ) {
        Units u= SemanticOps.getUnits(ds);
        if ( u==Units.radians ) return 1.;
        if ( u==Units.deg || u==Units.degrees ) return Math.PI/180;
        QDataSet extent= Ops.extent(ds);
        double delta= extent.value(1)-extent.value(0);
        if ( u==Units.dimensionless && ( delta>160 && delta<181 || delta>320 && delta<362 ) ) {
            return Math.PI/180;
        } else if ( u==Units.hours ) {  // untested.
            return Math.PI/12;  // TAU/24.
        } else if ( u==Units.dimensionless && ( delta>Math.PI*160/180 && delta<Math.PI*181/180 || delta>Math.PI*320/180 && delta<Math.PI*362/180 ) ) {
            return 1.;
        } else {
            if ( strict ) {
                return null;
            } else {
                return Math.PI/180;
            }
        }
    }
    
    /**
     * converts a rank 2 bundle of polar data, where ds[:,0] are the radii and ds[:,1] 
     * are the angles.  Any additional bundled datasets are left alone.
     * @param ds
     * @return bundle of X, Y, and remaining data.
     */
    public static QDataSet polarToCartesian( QDataSet ds ) {
        QDataSet rad= unbundle(ds,0);
        Units runits= SemanticOps.getUnits(rad);
        QDataSet angle= unbundle(ds,1);
        Double mult= isAngleRange( angle, true );
        if ( mult==null ) {
            mult= isAngleRange( rad, true );
            if ( mult!=null ) {
                logger.fine("assuming first bundled dataset is angle");
                angle= rad;
                rad= unbundle(ds,1);
            }
        }
        WritableDataSet result= copy(ds);
        for ( int i=0; i<result.length(); i++ ) {
            double r= rad.value(i);
            double x= r * cos(angle.value(i)*mult);
            double y= r * sin(angle.value(i)*mult);
            result.putValue( i, 0, x );
            result.putValue( i, 1, y );
        }
        QDataSet b1= (QDataSet)ds.property(QDataSet.BUNDLE_1);
        if ( b1!=null ) {
            
            WritableDataSet wb1= copy(b1);
            copyIndexedProperties( b1, wb1 );
            
            wb1.putProperty( QDataSet.LABEL, 0, "X" );
            wb1.putProperty( QDataSet.LABEL, 1, "Y" );
            wb1.putProperty( QDataSet.TITLE, 0, "X" );
            wb1.putProperty( QDataSet.TITLE, 1, "Y" );
            wb1.putProperty( QDataSet.UNITS, 0, runits );
            wb1.putProperty( QDataSet.UNITS, 1, runits );
            result.putProperty( QDataSet.BUNDLE_1, wb1 );
        }
        
        return result;
    }
    
    /**
     * create the inverse fft of the real and imaginary spec
     * @param ds rank 3 dataset of N,FFTLength,2
     * @param window
     * @param stepFraction
     * @param mon
     * @return 
     */
    public static QDataSet ifft( QDataSet ds, QDataSet window, int stepFraction, ProgressMonitor mon ) {

        String title= (String) ds.property(QDataSet.TITLE);
        if ( title!=null ) {
            title= "IFFT of "+title;
        }
        
        if ( ds.rank()<3 || ds.rank()>4 ) {
            throw new IllegalArgumentException("rank exception, expected rank 3 or 4: got "+ds );
                        
        } else if ( ds.rank()==4 ) { // slice it and do the process to each branch.
            JoinDataSet result= new JoinDataSet(4);
            mon.setTaskSize( ds.length()*10  );
            mon.started();
            for ( int i=0; i<ds.length(); i++ ) {
                mon.setTaskProgress(i*10);
                QDataSet pow1= ifft( ds.slice(i), window, stepFraction, SubTaskMonitor.create( mon, i*10, (i+1)*10 ) );
                result.join(pow1);
            }
            mon.finished();

            result.putProperty( QDataSet.QUBE, Boolean.TRUE );
            if ( title!=null ) result.putProperty( QDataSet.TITLE, title );
            
            return result;

        }
        
        assert ds.rank()==3;
        
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
        DataSetBuilder dep0Builder= new DataSetBuilder( 1,nsam );

        QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
        if ( dep0!=null ) { // make sure these are really units we can use
            Units u0= SemanticOps.getUnits(dep0);
            if ( UnitsUtil.isNominalMeasurement(u0) ) dep0= null; // nope, we can't use it.
        }

        UnitsConverter uc= UnitsConverter.IDENTITY;

        QDataSet dep1= (QDataSet) ds.property( QDataSet.DEPEND_1 );
        if ( dep1==null ) {
            dep1= (QDataSet)ds.slice(0).property(QDataSet.DEPEND_0);
            if ( dep1==null ) {
                logger.finer("No time offsets found.");
            }
        }
        
        QDataSet ytags=null;
        double minD= Double.NEGATIVE_INFINITY, maxD=Double.POSITIVE_INFINITY;
        if ( dep1!=null && dep1.rank()==1 ) {
            ytags= FFTUtil.getTimeDomainTags( dep1.trim(0,len) );
            //NOTE translation is not implemented for this mode.
            result.putProperty( QDataSet.DEPEND_1, ytags );
            Units dep1Units= (Units) dep1.property(QDataSet.UNITS);
            Units dep0Units= dep0==null ? null : (Units) dep0.property(QDataSet.UNITS);
            if ( dep0Units!=null && dep1Units!=null ) uc= UnitsConverter.IDENTITY;
            if ( dep0!=null && dep0.property(QDataSet.VALID_MIN)!=null ) minD= ((Number)dep0.property(QDataSet.VALID_MIN)).doubleValue();
            if ( dep0!=null && dep0.property(QDataSet.VALID_MAX)!=null ) maxD= ((Number)dep0.property(QDataSet.VALID_MAX)).doubleValue();
        } else {
            throw new IllegalArgumentException("must have ytags...");
        }

        int len1= ( ( ds.length(0)-len ) / step ) + 1;

        mon.setTaskSize(ds.length()*len1); // assumes all are the same length.
        mon.started();
        mon.setProgressMessage("performing ifft");

        boolean isMono= dep0==null ? true : DataSetUtil.isMonotonic(dep0);

        for ( int i=0; i<ds.length(); i++ ) {
            QDataSet slicei= ds.slice(i); //TODO: for DDataSet, this copies the backing array.  This shouldn't happen in DDataSet.slice, but it does...
            QDataSet dep0i= (QDataSet) slicei.property(QDataSet.DEPEND_0);
            if ( dep0i!=null && dep0==null ) {
                dep0Builder.putProperty(QDataSet.UNITS, dep0i.property(QDataSet.UNITS) );
                if ( !Boolean.TRUE.equals( dep0i.property(QDataSet.MONOTONIC) ) ) {
                    isMono= false;
                }
                if ( dep0i.property(QDataSet.VALID_MIN)!=null ) minD= ((Number)dep0i.property(QDataSet.VALID_MIN)).doubleValue(); else minD= Double.NEGATIVE_INFINITY;
                if ( dep0i.property(QDataSet.VALID_MAX)!=null ) maxD= ((Number)dep0i.property(QDataSet.VALID_MAX)).doubleValue(); else maxD= Double.POSITIVE_INFINITY;
            }

            for ( int j=0; j<len1; j++ ) {
                GeneralFFT fft = GeneralFFT.newDoubleFFT(len);
                QDataSet wave= slicei.trim( j*step,j*step+len );
                QDataSet wds= DataSetUtil.weightsDataSet(wave);
                boolean hasFill= false;
                for ( int k=0; k<wds.length(); k++ ) {
                    if ( wds.value(k,0)==0 ) {
                        hasFill= true;
                    }
                }
                if ( hasFill ) continue;

                QDataSet vds= FFTUtil.ifft( fft, wave, null );

                if ( windowNonUnity ) {
                    vds= Ops.multiply( vds, DataSetUtil.asDataSet( 1/normalization ) );
                }

                double d0=0;
                if ( dep0!=null && ytags!=null ) {
                    d0= dep0.value(i) + uc.convert( ytags.value( j*step + len/2 )  );
                } else if ( dep0!=null ) {
                    d0= dep0.value(i);
                } else if ( dep0i!=null ) {
                    d0= dep0i.value(j*step+len/2);
                } else {
                    dep0Builder= null;
                }

                if ( d0>=minD && d0<=maxD) {
                    result.join(vds);
                    if ( dep0Builder!=null ) {
                        dep0Builder.putValue(-1, d0 );
                        dep0Builder.nextRecord();
                    }
                } else {
                    System.err.println("dropping record with invalid timetag: "+d0 ); //TODO: consider setting VALID_MIN, VALID_MAX instead...
                }

                mon.setTaskProgress(i*len1+j);

            }

        }
        mon.finished();
        
        if ( dep0!=null && dep0Builder!=null ) {
            dep0Builder.putProperty(QDataSet.UNITS, dep0.property(QDataSet.UNITS) );
            if ( isMono ) dep0Builder.putProperty(QDataSet.MONOTONIC,true);
            result.putProperty(QDataSet.DEPEND_0, dep0Builder.getDataSet() );
        } else if ( dep0Builder!=null ) {
            if ( isMono ) dep0Builder.putProperty(QDataSet.MONOTONIC,true);
            result.putProperty(QDataSet.DEPEND_0, dep0Builder.getDataSet() );
        }

        if ( title!=null ) result.putProperty( QDataSet.TITLE, title );
        result.putProperty( QDataSet.QUBE, Boolean.TRUE );

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
                QDataSet wds= DataSetUtil.weightsDataSet(wave);
                boolean hasFill= false;
                for ( int k=0; k<wds.length(); k++ ) {
                    if ( wds.value(k)==0 ) {
                        hasFill= true;
                    }
                }
                if ( hasFill ) continue;

                QDataSet vds= FFTUtil.fft( fft, wave, windowNonUnity ? wds : null );

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
     * @see DataSetUtil#rangeOfMonotonic(org.das2.qds.QDataSet) 
     * @see AutoRangeUtil#simpleRange in Autoplot.
     * @see Ops#reduceMax(org.das2.qds.QDataSet, int, org.das2.util.monitor.ProgressMonitor) 
     * @param ds the dataset to measure the extent
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
     * @param range if non-null, return the union of this range and the extent.  This must not contain fill!
     * @return two element, rank 1 "bins" dataset.
     */
    public static QDataSet extent( QDataSet ds, QDataSet range  ) {
        if ( ds==null ) throw new IllegalArgumentException("dataset is null");
        QDataSet wds = DataSetUtil.weightsDataSet(ds);
        return extent( ds, wds, range );
    }

    /**
     * like extent, but does not account for DELTA_PLUS, DELTA_MINUS,
     * BIN_PLUS, BIN_MINUS, BIN_MIN or BIN_MAX properties.  This was introduced to provide
     * a fast way to identify constant datasets and the extent that non-constant 
     * datasets vary.
     * @param ds the dataset to measure the extent rank 1 or rank 2 bins
     * @param wds a weights dataset, containing zero where the data is not valid, positive non-zero otherwise.  If null, then all finite data is treated as valid.
     * @param range if non-null, return the union of this range and the extent.  This must not contain fill!
     * @return two element, rank 1 "bins" dataset.
     * @see #extent(org.das2.qds.QDataSet, org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet extentSimple( QDataSet ds, QDataSet wds, QDataSet range  ) {
        
        logger.entering(CLASSNAME, "extentSimple" );
        if ( ds==null ) throw new IllegalArgumentException("dataset is null");
        int count=0;
        
        if ( wds==null ) {
            wds= DataSetUtil.weightsDataSet(ds);
        }
        
        double [] result;
        Number dfill= ((Number)wds.property(WeightsDataSet.PROP_SUGGEST_FILL));
        double fill= dfill!=null ? dfill.doubleValue() : -1e31;

        if ( range==null ) {
            result= new double[]{ Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        } else {
            result= new double[]{ range.value(0), range.value(1) };
            if ( range.value(0)==fill ) System.err.println("range passed into extent contained fill");
        }

        boolean monoCheck; // true if the data appears to be monotonic decreasing or increasing.
        int ifirst,ilast;

        QDataSet min= ds;
        QDataSet max= ds;
        if ( ds.rank()==2 && SemanticOps.isBins(ds) ) {
            min= Ops.slice1(ds,0);
            max= Ops.slice1(ds,1);
            ds= min;
            wds= Ops.slice1(wds,0);
        }
        
        if ( ds.rank()>0 ) {
            // find the first and last valid points.
            ifirst=0;
            int n= ds.length();
            ilast= n-1;

            monoCheck= Boolean.TRUE.equals( ds.property(QDataSet.MONOTONIC ));
            if ( ds.rank()==1 && monoCheck && n>0 ) {
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
        } else {
            monoCheck= false;
            ifirst=0;
            ilast= 0; // not used.
        }
        
        if ( ds.rank()==1 && monoCheck ) {
            count= Math.max( 0, ilast - ifirst + 1 );
            if ( count>0 ) {
                result[0]= Math.min( result[0], ds.value(ifirst) );
                result[1]= Math.max( result[1], ds.value(ilast) );
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
        
        logger.exiting(CLASSNAME, "extentSimple" );
        
        return qresult;
        
    }
    
    /**
     * returns a two element, rank 1 dataset containing the extent (min to max) of the data, allowing an external
     * evaluation of the weightsDataSet.  If no valid data is found then [fill,fill] is returned.
     * @param ds the dataset to measure the extent rank 1 or rank 2 bins
     * @param wds a weights dataset, containing zero where the data is not valid, positive non-zero otherwise.  If null, then all finite data is treated as valid.
     * @param range if non-null, return the union of this range and the extent.  This must not contain fill!
     * @see #reduceMax(org.das2.qds.QDataSet, int, org.das2.util.monitor.ProgressMonitor) 
     * @return two element, rank 1 "bins" dataset.
     */
    public static QDataSet extent( QDataSet ds, QDataSet wds, QDataSet range ) {

        logger.entering(CLASSNAME, "extent" );
        
        if ( !DataSetUtil.validate(ds,null) ) {
            throw new IllegalArgumentException("data does not validate: "+ds);
        }
        QDataSet max = ds;
        QDataSet min = ds;
        QDataSet deltaplus;
        QDataSet deltaminus;
        
        deltaplus = (QDataSet) ds.property(QDataSet.DELTA_PLUS);
        deltaminus = (QDataSet) ds.property(QDataSet.DELTA_MINUS);
        if ( ds.property(QDataSet.BIN_PLUS )!=null ) deltaplus= (QDataSet)ds.property(QDataSet.BIN_PLUS );
        if ( ds.property(QDataSet.BIN_MINUS )!=null ) deltaminus= (QDataSet)ds.property(QDataSet.BIN_MINUS );

        if ( deltaplus!=null ) {
            Units u= SemanticOps.getUnits(deltaplus);
            deltaplus= Ops.greaterOf(u.createDatum(0),deltaplus);
        }
        if ( deltaminus!=null ) {
            Units u= SemanticOps.getUnits(deltaminus);
            deltaminus= Ops.greaterOf(u.createDatum(0),deltaminus);
        }
        
        if ( ds.rank()==2 && SemanticOps.isBins(ds) ) {
            min= Ops.slice1(ds,0);
            max= Ops.slice1(ds,1);
            ds= min;
            if ( wds!=null ) wds= Ops.slice1(wds,0);
        }
        
        if ( ds.property(QDataSet.BIN_MAX )!=null ) {
            max= (QDataSet)ds.property(QDataSet.BIN_MAX);
        }
        
        if ( ds.property(QDataSet.BIN_MIN )!=null ) {
            min= (QDataSet)ds.property(QDataSet.BIN_MIN);
        }
        
        int count=0;

        if ( wds==null ) {
            wds= DataSetUtil.weightsDataSet(ds);
        }

        double [] result;
        Number dfill= ((Number)wds.property(WeightsDataSet.PROP_SUGGEST_FILL));
        double fill= dfill!=null ? dfill.doubleValue() : -1e31;

        if ( range==null ) {
            result= new double[]{ Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        } else {
            result= new double[]{ range.value(0), range.value(1) };
            if ( range.value(0)==fill ) logger.warning("range passed into extent contained fill");
        }

        boolean monoCheck;
        int ifirst,ilast;

        if ( ds.rank()>0 ) {
            // find the first and last valid points.
            ifirst=0;
            int n= ds.length();
            ilast= n-1;

            monoCheck= Boolean.TRUE.equals( ds.property(QDataSet.MONOTONIC ));
            if ( ds.rank()==1 && monoCheck && n>0 ) {
                monoCheck= DataSetUtil.isMonotonicQuick(ds);
                if ( !monoCheck ) logger.log(Level.WARNING, "this data isn''t really monotonic: {0}", ds);
                if ( monoCheck ) {
                    while ( wds.value(ilast)==0.0 && ilast>0 ) ilast--;
                    if ( ilast<ifirst ) {
                        ilast= ifirst;
                    }
                }
            }
        } else {
            monoCheck= false;
            ifirst=0;
            ilast= 0; // not used.
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

            switch (ds.rank()) {
                case 1:
                    {
                        // optimize for rank 1, see https://sourceforge.net/p/autoplot/bugs/1801/
                        int ni= min.length();
                        for ( int i=0; i<ni; i++ ) {
                            if ( wds.value(i)>0 ) {
                                count++;
                                double a,b;
                                a= result[0];
                                b= min.value(i);
                                result[0]= (a <= b) ? a : b; //Math.min( result[0], min.value(i) );
                                a= result[1];
                                b= max.value(i);
                                result[1]= (a >= b) ? a : b; // Math.max( result[1], max.value(i) );
                            }
                        }       break;
                    }
                case 2:
                    {
                        int ni= min.length();
                        for ( int i=0; i<ni; i++ ) {
                            int nj= min.length(i);
                            for ( int j=0; j<nj; j++ ) {
                                if ( wds.value(i,j)>0 ) {
                                    count++;
                                    result[0]= Math.min( result[0], min.value(i,j) );
                                    result[1]= Math.max( result[1], max.value(i,j) );
                                }
                            }
                        }       
                        break;
                    }
                default:
                    QubeDataSetIterator it = new QubeDataSetIterator(ds);
                    while (it.hasNext()) {
                        it.next();
                        if (it.getValue(wds) > 0.) {
                            count++;
                            result[0] = Math.min(result[0], it.getValue(min));
                            result[1] = Math.max(result[1], it.getValue(max));
                        }
                    }   
                    break;
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
        
        logger.exiting( CLASSNAME, "extent" );
        
        return qresult;
        
    }

    /**
     * fast extent needed at some point, which will be removed.  It simply 
     * calls extentSimple.
     * @param ds a dataset.
     * @return the extent of the data.
     * @deprecated use extentSimple
     * @see #extentSimple(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet extent445( QDataSet ds ) {
        return extentSimple(ds,null);
    }
    
    /**
     * This is introduced to study effect of 
     * https://sourceforge.net/p/autoplot/feature-requests/445/
     * Do not use this in scripts!!!
     * This is very interesting:
     *
     * Ops.extent: 53ms
     * simpleRange: 77ms
     * study445FastRange: 4ms
     * 
     * Ops.extent: 76ms 
     * simpleRange: 114ms 
     * study445FastRange: 12ms
     * 
     * This is likely showing that DataSetIterator is slow...
     * 
     * @param ds the dataset
     * @param range null, or rank 1 bins dataset
     * @return rank 1, two-element range, or when all data is fill result[0] will be Double.POSITIVE_INFINITY.
     * @see #extentSimple(org.das2.qds.QDataSet, org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet extentSimple( QDataSet ds, QDataSet range ) {
        
        logger.entering(CLASSNAME, "extentSimple" );
        
        QDataSet max= ds;
        QDataSet min= ds;
        if ( ds.rank()==2 && SemanticOps.isBins(ds) ) {
            min= Ops.slice1(ds,0);
            max= Ops.slice1(ds,1);
            ds= min;
        }        
        
        Number nfill= (Number)ds.property(QDataSet.FILL_VALUE);
        double fill= ( nfill==null ) ? 1e38 : nfill.doubleValue();
        
        int count=0;
        
        double[] result;
        if ( range==null ) {
            result= new double[]{Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        } else {
            result= new double[]{ range.value(0), range.value(1) };
        }
        
        QDataSet valid= Ops.valid(ds);
            
        switch (ds.rank()) {
            case 1:
                int n= ds.length();
                for ( int i=0; i<n; i++ ) {
                    if ( valid.value(i)>0 ) {
                        double min1= min.value(i); // Math.min requires we do extra redundent checks, and we leave this routine
                        result[0]= result[0] < min1 ? result[0] : min1;
                        double max1= max.value(i);
                        result[1]= result[1] > max1 ? result[1] : max1;
                        count++;
                    }
                }   break;
            case 2:
                {
                    int n0= ds.length();
                    for ( int i0=0; i0<n0; i0++ ) {
                        int n1= ds.length(i0);
                        for ( int i1=0; i1<n1; i1++ ) {
                            if ( valid.value(i0,i1)>0 ) {
                                double min1= min.value(i0,i1); 
                                result[0]= result[0] < min1 ? result[0] : min1 ;
                                double max1= max.value(i0,i1);
                                result[1]= result[1] > max1 ? result[1] : max1;
                                count++;
                            }
                        }
                    }       
                    break;
                }
            case 3:
                {
                    int n0= ds.length();
                    for ( int i0=0; i0<n0; i0++ ) {
                        int n1= ds.length(i0);
                        for ( int i1=0; i1<n1; i1++ ) {
                            int n2= ds.length(i0,i1);
                            for ( int i2=0; i2<n2; i2++ ) {
                                if ( valid.value(i0,i1,i2)>0 ) {
                                    double min1= min.value(i0,i1,i2); 
                                    result[0]= result[0] < min1 ? result[0] : min1;
                                    double max1= max.value(i0,i1,i2);
                                    result[1]= result[1] > max1 ? result[1] : max1;
                                    count++;
                                }
                            }
                        }
                    }
                    break;
                }
            case 4:
                {
                    int n0= ds.length();
                    for ( int i0=0; i0<n0; i0++ ) {
                        int n1= ds.length(i0);
                        for ( int i1=0; i1<n1; i1++ ) {
                            int n2= ds.length(i0,i1);
                            for ( int i2=0; i2<n2; i2++ ) {
                                int n3= ds.length(i0,i1,i2);
                                for ( int i3=0; i3<n3; i3++ ) {
                                    if ( valid.value(i0,i1,i2,i3)>0 ) {
                                        result[0]= Math.min( result[0], min.value(i0,i1,i2,i3) );
                                        result[1]= Math.max( result[1], max.value(i0,i1,i2,i3) );
                                        count++;
                                    }
                                }
                            }
                        }
                    }       
                    break;
                }
            case 0:
                result[0]= ds.value();
                result[1]= ds.value();
                count++;
                break;
            default:
                break;
        }

        DDataSet qresult= DDataSet.wrap(result);
        qresult.putProperty( QDataSet.SCALE_TYPE, ds.property(QDataSet.SCALE_TYPE) );
        qresult.putProperty( QDataSet.USER_PROPERTIES, Collections.singletonMap( "count", count ) );
        qresult.putProperty( QDataSet.BINS_0, "min,maxInclusive" );
        qresult.putProperty( QDataSet.UNITS, ds.property(QDataSet.UNITS ) );
        if ( result[0]==fill ) qresult.putProperty( QDataSet.FILL_VALUE, fill);

        logger.exiting(CLASSNAME, "extentSimple" );
        
        return qresult;
    }
    
    /**
     * calculate the range of data, then rescale it so that the smallest
     * values becomes min and the largest values becomes max.
     * @param data rank N dataset
     * @param min rank 0 min
     * @param max rank 0 max
     * @return rescaled data.
     */
    public static QDataSet rescale( QDataSet data, QDataSet min, QDataSet max ) {
        QDataSet extent= extent(data);
        QDataSet w= Ops.subtract( extent.slice(1), extent.slice(0) );
        if ( w.value()==0 ) {
            switch (data.rank()) {
                case 0:
                    return min;
                case 1:
                    return replicate( min, data.length() );
                case 2:
                    return replicate( min, data.length(), data.length(0) );
                case 3:
                    return Ops.multiply( min, Ops.ones( data.length(), data.length(0), data.length(0,0) ) );
                case 4:
                    return Ops.multiply( min, Ops.ones( data.length(), data.length(0), data.length(0,0), data.length(0,0,0) ) );
                default:
                    break;
            }
        }
        QDataSet xxx= Ops.divide( Ops.subtract( data, extent.slice(0) ), w );
        xxx= Ops.multiply( xxx, Ops.subtract( max, min ) );
        data= Ops.add( min, xxx );
        return data;
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
        if ( dr.length()!=2 ) {
            throw new IllegalArgumentException("length must be 2");
        }
        double w= dr.value(1) - dr.value(0);
        if ( Double.isInfinite(w) || Double.isNaN(w) ) {
            throw new RuntimeException("width is not finite in rescaleRange");
        }
        if ( w==0. ) {
            // condition that might cause an infinate loop!  For now let's check for this and throw RuntimeException.
            throw new RuntimeException("width is zero in rescaleRange!");
        }
        DDataSet result= DDataSet.createRank1(2);
        result.putValue( 0, dr.value(0) + w*min );
        result.putValue( 1, dr.value(0) + w*max );

        DataSetUtil.copyDimensionProperties( dr, result );
        return result;
    }

    /**
     * like rescaleRange, but look at log/lin flag.
     * @param dr
     * @param min
     * @param max
     * @return two-element rank 1 QDataSet
     * @see org.das2.qds.QDataSet#SCALE_TYPE
     */
    public static QDataSet rescaleRangeLogLin( QDataSet dr, double min, double max ) {
        if ( dr.rank()!=1 ) {
            throw new IllegalArgumentException("Rank must be 1");
        }
        if ( dr.length()!=2 ) {
            throw new IllegalArgumentException("length must be 2");
        }        
        DDataSet result= DDataSet.createRank1(2);
        if ( "log".equals( dr.property(QDataSet.SCALE_TYPE) ) ) {
            
            double s1= Math.log10( dr.value(0) );
            double s2= Math.log10( dr.value(1) );
            double w= s2 - s1;
            if ( Double.isInfinite(w) || Double.isNaN(w) ) {
                throw new RuntimeException("width is not finite in rescaleRangeLogLin");
            }
            if ( w==0. ) {
                // condition that might cause an infinate loop!  For now let's check for this and throw RuntimeException.
                throw new RuntimeException("width is zero in rescaleRangeLogLin!");
            }
            s2= Math.pow( 10, s1 + max * w ); // danger
            s1= Math.pow( 10, s1 + min * w );
        
            result.putValue( 0, s1 );
            result.putValue( 1, s2 );
            
        } else {
            double w= dr.value(1) - dr.value(0);
            if ( Double.isInfinite(w) || Double.isNaN(w) ) {
                throw new RuntimeException("width is not finite in rescaleRangeLogLin");
            }
            if ( w==0. ) {
                throw new RuntimeException("width is zero in rescaleRangeLogLin!");
            }
            result.putValue( 0, dr.value(0) + w*min );
            result.putValue( 1, dr.value(0) + w*max );    
        }

        DataSetUtil.copyDimensionProperties( dr, result );
        return result;
    }
    

    /**
     * make a 2-D histogram of the data in x and y.  For example
     *<blockquote><pre>
     *x= randn(10000)+1
     *y= randn(10000)+4
     *zz= histogram2d( x,y, [30,30], dataset([0,8]), dataset([-2,6]) )
     *plot( zz )
     *</pre></blockquote>
     * The result will be a rank 2 dataset with DEPEND_0 and DEPEND_1 indicating
     * the bin locations.  If the xrange or yrange is dimensionless, then 
     * use the units of x or y.
     * @param x the x values
     * @param y the y values
     * @param bins number of bins in x and y
     * @param xrange a rank 1 2-element bounds dataset, so that Units can be specified.
     * @param yrange a rank 1 2-element bounds dataset, so that Units can be specified.
     * @return a rank 2 dataset
     * @see #histogram(org.das2.qds.QDataSet, double, double, double) 
     * @see org.das2.qds.util.Reduction#histogram2D(org.das2.qds.QDataSet, org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet histogram2d( QDataSet x, QDataSet y, int[] bins, QDataSet xrange, QDataSet yrange ) {

        int nx, ny;
        if ( bins==null ) {
            nx=20;
            ny=20;
        } else {
            nx=bins[0];
            ny=bins[1];            
        }
        
        if ( SemanticOps.getUnits(xrange)==Units.dimensionless ) {
            xrange= putProperty( xrange, QDataSet.UNITS, SemanticOps.getUnits(x) );
        }

        if ( SemanticOps.getUnits(yrange)==Units.dimensionless ) {
            yrange= putProperty( yrange, QDataSet.UNITS, SemanticOps.getUnits(y) );
        }
            
        if ( x==null ) throw new NullPointerException("x is null");
        if ( y==null ) throw new NullPointerException("y is null");
        
        if ( xrange.rank()!=1 || xrange.length()!=2 ) {
            throw new IllegalArgumentException("xrange should be rank 1, two-element dataset");
        }
        
        if ( yrange.rank()!=1 || yrange.length()!=2 ) {
            throw new IllegalArgumentException("yrange should be rank 1, two-element dataset");
        }
        double minx= xrange.value(0);
        double miny= yrange.value(0);
        
        double binsizex= ( xrange.value(1)-xrange.value(0) ) / nx;
        double binsizey= ( yrange.value(1)-yrange.value(0) ) / ny;
        MutablePropertyDataSet xtags = DataSetUtil.tagGenDataSet( nx, minx+binsizex/2, binsizex, SemanticOps.getUnits(xrange) );        
        xtags.putProperty( QDataSet.NAME, x.property(QDataSet.NAME) );
        xtags.putProperty( QDataSet.LABEL, x.property(QDataSet.LABEL) );
        xtags.putProperty( QDataSet.TITLE, x.property(QDataSet.TITLE) );
        xtags.putProperty( QDataSet.TYPICAL_MAX, x.property(QDataSet.TYPICAL_MAX) );
        xtags.putProperty( QDataSet.TYPICAL_MIN, x.property(QDataSet.TYPICAL_MIN) );
        
        MutablePropertyDataSet ytags = DataSetUtil.tagGenDataSet( ny, miny+binsizey/2, binsizey, SemanticOps.getUnits(yrange) );
        ytags.putProperty( QDataSet.NAME, y.property(QDataSet.NAME) );
        ytags.putProperty( QDataSet.LABEL, y.property(QDataSet.LABEL) );
        ytags.putProperty( QDataSet.TITLE, y.property(QDataSet.TITLE) );
        ytags.putProperty( QDataSet.TYPICAL_MAX, y.property(QDataSet.TYPICAL_MAX) );
        ytags.putProperty( QDataSet.TYPICAL_MIN, y.property(QDataSet.TYPICAL_MIN) );

        final int[] hits = new int[nx*ny];
        
        QubeDataSetIterator iter = new QubeDataSetIterator(x);
        QDataSet wdsx= DataSetUtil.weightsDataSet(x);
        QDataSet wdsy= DataSetUtil.weightsDataSet(y);
        
        int count=0;
        while ( iter.hasNext() ) { 
            iter.next();
            double x1 = iter.getValue(x);
            double y1 = iter.getValue(y);
            
            double w = iter.getValue(wdsx) * iter.getValue(wdsy);
            
            if ( w>0. ) {
                int ibinx = (int) Math.floor(( x1 - minx ) / binsizex );
                int ibiny = (int) Math.floor(( y1 - miny ) / binsizey );
                if (ibinx >= 0 && ibinx < nx && ibiny>=0 && ibiny<ny ) {
                    hits[ ibinx*ny + ibiny ]++;
                }
                count++;
            }
        }

        IDataSet result = IDataSet.wrap( hits, new int[] { nx,ny } );
        result.putProperty( QDataSet.DEPEND_0, xtags );
        result.putProperty( QDataSet.DEPEND_1, ytags );
        result.putProperty( "count", count );
        result.putProperty( QDataSet.RENDER_TYPE, "nnSpectrogram" );
        
        return result;        

    }
    
    /**
     * returns a rank 1 dataset that is a histogram of the data.  Note there
     * will also be in the properties:
     *   count, the total number of valid values.
     *   nonZeroMin, the smallest non-zero, positive number
     * @param ds rank N dataset
     * @param min the min of the first bin.  If min=-1 and max=-1, then automatically set the min and max.
     * @param max the max of the last bin.
     * @param binSize the size of each bin.
     * @return a rank 1 dataset with each bin's count.  DEPEND_0 indicates the bin locations.
     */
    public static QDataSet histogram(QDataSet ds, double min, double max, double binSize) {
        return DataSetOps.histogram(ds, min, max, binSize);
    }

    /**
     * returns a rank 1 dataset that is a histogram of the data.  Note there
     * will also be in the properties:
     *   count, the total number of valid values.
     *   nonZeroMin, the smallest non-zero, positive number
     * @param ds rank N dataset
     * @param min the center of the first bin.  If min=-1 and max=-1, then automatically set the min and max.
     * @param max the center of the last bin.
     * @param binsize the size of each bin.
     * @return a rank 1 dataset with each bin's count.  DEPEND_0 indicates the bin locations.
     */
    public static QDataSet histogram( QDataSet ds, Datum min, Datum max, Datum binsize ) {
        Units u= SemanticOps.getUnits(ds);
        return histogram( ds, min.doubleValue(u), max.doubleValue(u), binsize.doubleValue(u.getOffsetUnits()) );
    }
    
    /**
     * returns rank 1 dataset that is a histogram of the data.  This will use
     * the units of ds to interpret min, max, and binsize.
     * @param ds rank N dataset
     * @param min the center of the first bin.  If min=-1 and max=-1, then automatically set the min and max.
     * @param max the center of the last bin.
     * @param binsize the size of each bin.
     * @return
     * @throws ParseException 
     */
    public static QDataSet histogram( QDataSet ds, String min, String max, String binsize ) throws ParseException {
        Units u= SemanticOps.getUnits(ds);
        return histogram( ds, u.parse(min), u.parse(max), u.getOffsetUnits().parse(binsize) );
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
     * AutoHistogram is a one-pass self-scaling histogram, useful in autoranging data.  
     * The data is fed into the routine, and bins will grow as more and more data is added,
     * to result in about 100 bins.  For example, if the initial binsize is 1.0 unit, and the data extent
     * is 0-200, then bins are combined so that the new binsize is 2.0 units and 100 bins are used.
     * @param ds rank N dataset (all ranks are supported).
     * @return rank 1 dataset
     */
    public static QDataSet autoHistogram( QDataSet ds ) {
        if ( ds==null ) {
            throw new NullPointerException("ds is null");
        }
        AutoHistogram ah= new AutoHistogram();
        return ah.doit(ds);
    }

    /**
     * returns outerProduct of two rank 1 datasets, a rank 2 dataset with 
     * elements R[i,j]= ds1[i] * ds2[j].
     * 
     * @param ds1 rank 1 dataset length m.
     * @param ds2 rank 1 dataset of length n.
     * @return rank 2 dataset that is m by n.
     */
    public static QDataSet outerProduct(QDataSet ds1, QDataSet ds2) {
        if ( ds1.rank()!=1 )  throw new IllegalArgumentException("rank must be 1: ds1");
        if ( ds2.rank()!=1 )  throw new IllegalArgumentException("rank must be 1: ds2");
        
        QDataSet w1= DataSetUtil.weightsDataSet(ds1);
        QDataSet w2= DataSetUtil.weightsDataSet(ds2);
        double fill= -1e38;
        boolean hasFill= false;
        
        DDataSet result = DDataSet.createRank2(ds1.length(), ds2.length());
        for (int i = 0; i < ds1.length(); i++) {
            for (int j = 0; j < ds2.length(); j++) {
                if ( w1.value(i)*w2.value(j)>0 ) {
                    result.putValue(i, j, ds1.value(i) * ds2.value(j));
                } else {
                    result.putValue(i, j, fill );
                    hasFill= true;
                }
            }
        }
        result.putProperty( QDataSet.DEPEND_0, ds1.property(QDataSet.DEPEND_0 ) );
        result.putProperty( QDataSet.DEPEND_1, ds2.property(QDataSet.DEPEND_0 ) );        
        if ( hasFill ) {
            result.putProperty( QDataSet.FILL_VALUE, fill );
        }
        
        Units units1= SemanticOps.getUnits(ds1);
        Units units2= SemanticOps.getUnits(ds2);        
        Units units;
        if ( units1==Units.dimensionless ) { // allow outerProduct( replicate(1,freq.length()), epoch )
            units= units2;
        } else if ( units2==Units.dimensionless ) {
            units= units1;
        } else {
            units= multiplyUnits( units1, units2 );
        }
        
        if ( units!=Units.dimensionless ) result.putProperty( QDataSet.UNITS, units );
        return result;
    }

    public static QDataSet outerProduct( Object ds1, Object ds2) {
        return outerProduct( dataset(ds1), dataset(ds2) );
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
        QDataSet w1= DataSetUtil.weightsDataSet(ds1);
        QDataSet w2= DataSetUtil.weightsDataSet(ds2);
        double fill= -1e38;
        boolean hasFill= false;
        
        DDataSet result = DDataSet.createRank2(ds1.length(), ds2.length());
        Map<String,Object> props= new HashMap<>();
        BinaryOp b= addGen( ds1, ds2, props );
        for (int i = 0; i < ds1.length(); i++) {
            for (int j = 0; j < ds2.length(); j++) {
                if ( w1.value(i)*w2.value(j)>0 ) {
                    result.putValue(i, j, b.op( ds1.value(i), ds2.value(j) ) );
                } else {
                    result.putValue(i, j, fill );
                    hasFill= true;
                }                
            }
        }
        result.putProperty( QDataSet.DEPEND_0, ds2.property(QDataSet.DEPEND_0 ) );
        result.putProperty( QDataSet.DEPEND_1, ds2.property(QDataSet.DEPEND_0 ) );
        if ( hasFill ) {
            result.putProperty( QDataSet.FILL_VALUE, fill );
        }
        result.putProperty( QDataSet.UNITS, props.get(QDataSet.UNITS ) );
        
        return result;
    }

    public static QDataSet outerSum( Object ds1, Object ds2) {
        return outerSum( dataset(ds1), dataset(ds2) );
    }
    
    /**
     * element-wise floor function.
     * @param ds1
     * @return
     */
    public static QDataSet floor(QDataSet ds1) {
        return applyUnaryOp(ds1, (UnaryOp) (double a) -> Math.floor(a));
    }
    
    public static double floor( double x ) {
        return Math.floor(x);
    }
    
    public static QDataSet floor( Object ds1 ) {
        return floor( dataset(ds1) );
    }

    /**
     * element-wise ceil function.
     * @param ds1
     * @return
     */
    public static QDataSet ceil(QDataSet ds1) {
        return applyUnaryOp(ds1, (UnaryOp) (double a) -> Math.ceil(a) );
    }

    public static double ceil( double x ) {
        return Math.ceil(x);
    }
    
    public static QDataSet ceil( Object x ) {
        return ceil( dataset(x) );
    }

    /**
     * element-wise round function.  0.5 is round up.
     * @param ds1
     * @return
     */
    public static QDataSet round(QDataSet ds1) {
        return applyUnaryOp(ds1, (UnaryOp) (double a) -> Math.round(a) );
    }

    /**
     * for Jython, we handle this because the double isn't coerced.
     * @param x
     * @return
     */
    public static double round( double x ) {
        return Math.round( x );
    }

    public static QDataSet round( Object x ) {
        return round( dataset(x) );
    }
    
    /**
     * element-wise round function, which rounds to i decimal places.
     * 0.5 is round up.
     * @param ds1 the dataset.
     * @param ndigits round to this number of digits after the decimal point (e.g. 1=0.1 2=0.01 -1=10)
     * @return dataset with the same geometry with each value rounded.
     */
    public static QDataSet round(QDataSet ds1, int ndigits ) {
        final double res= Math.pow(10,ndigits);
        return applyUnaryOp(ds1, (UnaryOp) (double a) -> Math.round(a*res) / res );
    }

    /**
     * for Jython, we handle this because the double isn't coerced.
     * @param x the double
     * @param ndigits round to this number of digits after the decimal point (e.g. 1=0.1 2=0.01 -1=10)
     * @return the rounded double.
     */
    public static double round( double x, int ndigits ) {
        final double res= Math.pow(10,ndigits);
        return Math.round( x*res )/res;
    }

    /**
     * element-wise round function, which rounds to i decimal places.
     * 0.5 is round up.
     * @param ds1 the dataset.
     * @param ndigits round to this number of digits after the decimal point (e.g. 1=0.1 2=0.01 -1=10)
     * @return dataset with the same geometry with each value rounded.
     */    
    public static QDataSet round( Object ds1, int ndigits ) {
        return round( dataset(ds1), ndigits );
    }
        
    /**
     * Returns the signum function of the argument; zero if the argument is 
     * zero, 1.0 if the argument is greater than zero, -1.0 if the argument 
     * is less than zero.
     * @param ds1
     * @see #copysign
     * @see #negate
     * @return 
     */
    public static QDataSet signum(QDataSet ds1) {
        return applyUnaryOp(ds1, (UnaryOp) (double a) -> Math.signum(a) );
    }

    public static double signum( double x ) {
        return Math.signum(x);
    }

    public static QDataSet signum( Object x ) {
        return signum( dataset(x) );
    }
    
    /**
     * Returns the first floating-point argument with the sign of the
     * second floating-point argument.
     * @param magnitude
     * @param sign 
     * @see #signum
     * @see #negate
     * @return
     */
    public static QDataSet copysign(QDataSet magnitude, QDataSet sign) {
        return applyBinaryOp(magnitude, sign, 
                (BinaryOp) (double m, double s) -> {
            double s1 = Math.signum(s);
            return Math.abs(m) * (s1 == 0 ? 1. : s1);
        });
    }
    
    public static double copysign( double x, double y ) {
        return Math.abs(x)*Math.signum(y);
    }
    
    public static QDataSet copysign( Object x, Object y ) {
        return multiply( abs(dataset(x)), signum( dataset(y) ) );
    }
    
    /**
     * return a list of indices, similar to the where result.
     * @param ds rank 1 dataset of length N
     * @param seq rank 1 dataset, with length much less than N.
     * @return rank 1 list of indices.
     * @see https://github.com/autoplot/dev/blob/master/demos/2021/20210115/whereSequence.jy
     */
    public static QDataSet whereSequence( QDataSet ds, QDataSet seq ) {
        
        DataSetBuilder builder= new DataSetBuilder(1,100);
        
        int n=ds.length();

        int ifind=0;
        int nfind= seq.length();
        for ( int i=0; i<n; i++ )  {
            if ( ds.value(i)==seq.value(ifind) ) {
                ifind++;
                if ( ifind==nfind ) {
                    builder.nextRecord(i-nfind+1);
                    ifind= 0;
                }
            } else if ( ds.value(i)==seq.value(0) ) {
                ifind= 1;
            } else {
                ifind= 0;
            }
        }

        builder.putProperty( QDataSet.CADENCE, DataSetUtil.asDataSet(1.0) );
        builder.putProperty( QDataSet.FORMAT, "%d" );
        return builder.getDataSet();
        
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
     * @param uu rank 1 monotonically increasing dataset, non-repeating, containing no fill values.
     * @param vv rank N dataset with values in the same physical dimension as uu.  Fill is allowed.
     * @return rank N dataset with the same geometry as vv.  It will have DEPEND_0=vv when vv is rank 1. 
     */
    public static QDataSet findex(QDataSet uu, QDataSet vv) {
        if ( uu==null ) throw new IllegalArgumentException("uu parameter of findex is null");
        if ( vv==null ) throw new IllegalArgumentException("vv parameter of findex is null");
        if ( uu.length()==0 ) throw new IllegalArgumentException("uu has length=0");
        if ( uu.rank()!=1 ) throw new IllegalArgumentException("uu must be rank 1");
        if (!DataSetUtil.isMonotonic(uu)) {
            DataSetUtil.isMonotonic(uu);
            throw new IllegalArgumentException("u must be monotonic");
        }
        if ( !DataSetUtil.isMonotonicAndIncreasingQuick(uu) ) {
            if ( Ops.reduceMin( Ops.diff(uu),0 ).value()==0 ) {
                throw new IllegalArgumentException("u must be non-repeating");
            } else {
                throw new IllegalArgumentException("u must be monotonically increasing and non-repeating");
            }
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

        QDataSet ww= DataSetUtil.weightsDataSet(vv);
        
        UnitsConverter uc= UnitsConverter.getConverter( vvunits, uuunits );

        double fill= -1e31;
        boolean hasFill= false;
        
        int extentWarning= 0; 
        
        while (it.hasNext()) {
            it.next();
            double d = uc.convert( it.getValue(vv) ); 
            double w = it.getValue(ww);
            
            if ( w==0 ) {
                it.putValue(result, fill );
                hasFill= true;
                continue;
            }
            
            // TODO: optimize by only doing binary search below or above ic0&ic1.
            if (uc0 <= d && d <= uc1) { // optimize by seeing if old pair still backets the current point.
                double ff = d==uc0 ? 0 : (d - uc0) / (uc1 - uc0); // may be 1.0
                
                if ( ( ( ic0 + ff ) / n ) < -3 || ( ( ic0 + ff - n ) / n ) > 3 ) {
                    extentWarning++;
                }
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

                if ( ( ( ic0 + ff ) / n ) < -3 || ( ( ic0 + ff - n ) / n ) > 3 ) {
                    extentWarning++;
                }
                it.putValue(result, ic0 + ff);
            }
        }
        if ( extentWarning>0 ) {
            logger.log(Level.WARNING, "alarming extrapolation in findex is suspicious: count:{0} uu:{1} vv:{2}", new Object[]{ extentWarning, extent(uu), extent(vv)});
        }
        if ( result.rank()==1 ) {
            result.putProperty( QDataSet.DEPEND_0, vv );
        }
        if ( hasFill ) {
            result.putProperty( QDataSet.FILL_VALUE, fill );
        }
        return result;
    }
    
    public static QDataSet findex( Object x, Object y ) {
        return findex( dataset(x), dataset(y) );
    }
    
    private static boolean isDimensionless( QDataSet ds ) {
        Units u= (Units)ds.property(QDataSet.UNITS);
        return u==null || u==Units.dimensionless;
    }
            
    /**
     * interpolate values from rank 1 dataset vv using fractional indices 
     * in rank N findex.  For example, findex=1.5 means interpolate
     * the 1st and 2nd indices with equal weight, 1.1 means
     * 90% of the first mixed with 10% of the second.  
     * Only modest extrapolations where findex&gt;=-0.5 and findex&lt;=L-0.5 are allowed, where L is the number of points.
     * The findex parameter must be dimensionless, to ensure that the caller is not passing in physical data.
     *
     * Note there is no check on CADENCE.
     * Note nothing is done with DEPEND_0, presumably because was already calculated and used for findex.
     * 
     * Here is an example use of this in Autoplot's Jython code:
     * <pre>
     * {@code
     *xx= [1,2,3,4]
     *yy= [2,4,5,4]
     *xxx= linspace(0,5,100)
     *yyy= interpolate( yy, findex(xx,xxx) )
     *
     *plot( xx, yy, symbolSize=20 )
     *plot( addPlotElement(0), xxx, yyy )
     * }
     * </pre>
     * 
     * This looks at the AVERAGE_TYPE metadata, and when it is present, linear, geometric, none, mod360, mod24, modpi, and modtau are respected.
     * 
     * @param vv rank 1 dataset having length L that is the data to be interpolated.
     * @param findex rank N dataset of fractional indices.  This must be dimensionless, between -0.5 and L-0.5 and is typically calculated by the findex command.
     * @return the result.  
     * @see #interpolateMod interpolateMod, for data like longitude where 259 deg is 2 degrees away from 1 deg
     */
    public static QDataSet interpolate( QDataSet vv, QDataSet findex ) {
        if ( vv.rank()==2 ) {
            QDataSet result= null;
            for ( int j=0; j<vv.length(0); j++ ) {
                QDataSet vvj= interpolate( Ops.unbundle(vv,j),findex );
                result= Ops.bundle(result,vvj);
            }
            QDataSet dep1= (QDataSet)vv.property(QDataSet.DEPEND_1);
            if ( dep1!=null ) result= putProperty( result, QDataSet.DEPEND_1, dep1 );
            QDataSet bds= (QDataSet)vv.property(QDataSet.BUNDLE_1);
            if ( bds!=null ) result= putProperty( result, QDataSet.BUNDLE_1, bds );
            return result;
        }
        if ( vv.rank()!=1 ) {
            throw new IllegalArgumentException("dataset to be interpolated is not rank1");
        }
        if ( !isDimensionless(findex) ) throw new IllegalArgumentException("findex argument should be dimensionless, expected output from findex command.");
        QDataSet fex0= extent(findex);
        if ( ( fex0.value(1)-vv.length() ) / vv.length() > 100 ) {
            logger.warning("findex looks suspicious, where its max would result in unrealistic extrapolations");
        }
        if ( fex0.value(0) / vv.length() < -100 ) {
            logger.warning("findex looks suspicious, where its min would result in unrealistic extrapolations");
        }
        
        DDataSet result = DDataSet.create(DataSetUtil.qubeDims(findex));
        QubeDataSetIterator it = new QubeDataSetIterator(findex);
        int ic0=0, ic1=0;
        int n = vv.length();

        QDataSet wds= DataSetUtil.weightsDataSet( vv );
        Number fill= (Number)wds.property(QDataSet.FILL_VALUE);
        double dfill;
        if ( fill==null ) dfill= -1e38; else dfill= fill.doubleValue();
        result.putProperty( QDataSet.FILL_VALUE, dfill );
        boolean hasFill= false;

        QDataSet wfindex= DataSetUtil.weightsDataSet(findex);
        wfindex= copy(wfindex); // This was done presumably for performance.
        
        // Starting with v2014a_12, immodest extrapolations beyond 0.5 are no longer allowed.
        boolean noExtrapolate= true;
        
        String averageType= (String)vv.property(QDataSet.AVERAGE_TYPE);
        
        if ( averageType==null ) averageType= QDataSet.VALUE_AVERAGE_TYPE_LINEAR;
        
        double vmin= -Double.MAX_VALUE;
        double vmax= Double.MAX_VALUE;
               
        if ( !averageType.equals( QDataSet.VALUE_AVERAGE_TYPE_LINEAR ) ) {
            Number v= (Number)vv.property(QDataSet.TYPICAL_MIN);
            if ( v==null ) v= (Number)vv.property(QDataSet.VALID_MIN);
            if ( v!=null ) vmin= v.doubleValue();
            v= (Number)vv.property(QDataSet.TYPICAL_MAX);
            if ( v==null ) v= (Number)vv.property(QDataSet.VALID_MAX);
            if ( v!=null ) vmax= v.doubleValue();
        }
        
        double v;  // the value for this iteration
        
        while (it.hasNext()) {
            it.next();

            if ( it.getValue(wfindex)==0 ) {
                it.putValue( result, dfill );
                hasFill= true;
                continue;
            }
            
            double ff = it.getValue(findex);

            if ( ff>=0 && ff<n-1 ) {
                ic0 = (int) Math.floor(ff);
                ic1 = ic0 + 1;                
            } else if ( noExtrapolate && ff < -0.5 ) {  // would extrapolate immodestly
                it.putValue( result, dfill );
                hasFill= true;
                continue;
            } else if ( noExtrapolate && ff > n - 0.5 ) { // would extrapolate immodestly
                it.putValue( result, dfill );
                hasFill= true;
                continue;
            } else if (ff < 0.0 ) {
                ic0 = 0; // extrapolate
                ic1 = 1;
            } else if (ff >= n - 1) {
                ic0 = n - 2; // extrapolate
                ic1 = n - 1;
            } 
            
            double alpha = ff - ic0;
            
            if ( alpha==0.0 || alpha==1.0 || averageType.equals(QDataSet.VALUE_AVERAGE_TYPE_NONE ) ) {
                if ( alpha<0.5 ) {
                    if( wds.value(ic0)>0 ) {
                        v= vv.value(ic0);
                    } else {
                        v= dfill;
                    }
                } else {
                    if ( wds.value(ic1)>0 ) {
                        v= vv.value(ic1);
                    } else {
                        v= dfill;
                    }
                }
                it.putValue(result, v);
                
            } else if ( wds.value(ic0)>0 && wds.value(ic1)>0 ) {
                double vv0 = vv.value(ic0);
                double vv1 = vv.value(ic1);

                switch ( averageType ) {
                    case QDataSet.VALUE_AVERAGE_TYPE_LINEAR:
                        it.putValue(result, vv0 + alpha * (vv1 - vv0));
                        break;
                    case QDataSet.VALUE_AVERAGE_TYPE_GEOMETRIC:
                        it.putValue(result, Math.exp( Math.log(vv0) + alpha * (vv1/vv0) ) );
                        break;
                    case QDataSet.VALUE_AVERAGE_TYPE_MOD24:
                        while ( vv1-vv0 > 12 ) vv1-=24;
                        while ( vv0-vv1 > 12 ) vv0-=24;
                        v= vv0 + alpha * (vv1 - vv0);
                        if ( v<vmin ) v+= 24;
                        if ( v>=vmax ) v-= 24;
                        it.putValue(result, v);
                        break;
                    case QDataSet.VALUE_AVERAGE_TYPE_MOD360:
                        while ( vv1-vv0 > 180 ) vv1-=360;
                        while ( vv0-vv1 > 180 ) vv0-=360;
                        v= vv0 + alpha * (vv1 - vv0);
                        if ( v<vmin ) v+= 360;
                        if ( v>=vmax ) v-= 360;
                        it.putValue(result,v );
                        break;
                    case QDataSet.VALUE_AVERAGE_TYPE_MODPI:
                        while ( vv1-vv0 > PI/2 ) vv1-=PI;
                        while ( vv0-vv1 > PI/2 ) vv0-=PI;
                        v= vv0 + alpha * (vv1 - vv0);
                        if ( v<vmin ) v+= PI;
                        if ( v>=vmax ) v-= PI;
                        it.putValue(result, v);
                        break;                        
                    case QDataSet.VALUE_AVERAGE_TYPE_MODTAU:
                        while ( vv1-vv0 > PI ) vv1-=TAU;
                        while ( vv0-vv1 > PI ) vv0-=TAU;
                        v= vv0 + alpha * (vv1 - vv0);
                        if ( v<vmin ) v+= TAU;
                        if ( v>=vmax ) v-= TAU;
                        it.putValue(result, v);
                        break; 
                    case QDataSet.VALUE_AVERAGE_TYPE_NONE:
                        if ( alpha<0.5 ) {
                            v= vv0;
                        } else {
                            v= vv1;
                        }
                        it.putValue(result, v);
                        break;  
                }
                
                
            } else {
                it.putValue(result, dfill );
                hasFill= true;

            }

        }
        DataSetUtil.copyDimensionProperties( vv, result );
        
        //allow findex0 to provide DEPEND_0 and DEPEND_1.
        for ( int i=0; i<=findex.rank(); i++ ) {
            QDataSet depend= (QDataSet) findex.property( "DEPEND_"+i );
            if ( depend!=null ) {
                result.putProperty( "DEPEND_"+i, depend );
            }
        }

        if ( hasFill ) {
            result.putProperty( QDataSet.FILL_VALUE, dfill );
        }

        return result;
    }
    
    /**
     * 
     * @param vv object that can be converted to rank 1 dataset, such as array. These are the rank 1 dataset that is the data to be interpolated.
     * @param findex object that can be converted to a rank N dataset, such as an array.  These are the rank N dataset of fractional indices. 
     * @return rank N dataset.
     */
    public static QDataSet interpolate( Object vv, Object findex ) {
        return interpolate( dataset(vv), dataset(findex) );
    }    
    
    /**
     * interpolate values from rank 2 dataset vv using fractional indices
     * in rank N findex, using bilinear interpolation.  See also interpolateGrid.
     *
     * @param vv rank 2 dataset.
     * @param findex0 rank N dataset of fractional indices for the zeroth index.  This must be dimensionless, between -0.5 and L-0.5 and is typically calculated by the findex command.
     * @param findex1 rank N dataset of fractional indices for the first index.  This must be dimensionless, between -0.5 and L-0.5 and is typically calculated by the findex command.
     * @return rank N dataset 
     * @see #findex findex, the 1-D findex command
     * @see #interpolateGrid(org.das2.qds.QDataSet, org.das2.qds.QDataSet, org.das2.qds.QDataSet)  
     */
    public static QDataSet interpolate(QDataSet vv, QDataSet findex0, QDataSet findex1) {

        if ( findex0.rank()>0 && findex0.length()!=findex1.length() ) {
            throw new IllegalArgumentException("findex0 and findex1 must have the same geometry.");
        }
        if ( !isDimensionless(findex0) ) throw new IllegalArgumentException("findex0 argument should be dimensionless, expected output from findex command.");
        if ( !isDimensionless(findex1) ) throw new IllegalArgumentException("findex1 argument should be dimensionless, expected output from findex command.");
        
        if ( !DataSetUtil.checkQube(vv) ) {
            logger.warning("vv is not a qube");
        }
        QDataSet fex0= extent(findex0);
        if ( ( fex0.value(1)-vv.length() ) / vv.length() > 100 ) {
            logger.warning("findex0 looks suspicious, where its max would result in unrealistic extrapolations");
        }
        if ( fex0.value(0) / vv.length() < -100 ) {
            logger.warning("findex0 looks suspicious, where its min would result in unrealistic extrapolations");
        }
        QDataSet fex1= extent(findex1);
        if ( ( fex1.value(1)-vv.length(0) ) / vv.length(0) > 100 ) {
            logger.warning("findex1 looks suspicious, where its max would result in unrealistic extrapolations");
        }
        if ( fex1.value(0) / vv.length(0) < -100 ) {
            logger.warning("findex1 looks suspicious, where its min would result in unrealistic extrapolations");
        }
        DDataSet result = DDataSet.create(DataSetUtil.qubeDims(findex0));

        QDataSet wds= DataSetUtil.weightsDataSet(vv);

        QubeDataSetIterator it = new QubeDataSetIterator(findex0);
        int ic00=0, ic01=0, ic10=0, ic11=0;
        int n0 = vv.length();
        int n1 = vv.length(0);

        double fill= -1e38;
        boolean hasFill= false;
        
        // Starting with v2014a_12, immodest extrapolations beyond 0.5 are no longer allowed.
        boolean noExtrapolate= true;
        
        while (it.hasNext()) {
            it.next();

            double ff0 = it.getValue(findex0);
            double ff1 = it.getValue(findex1);

            if ( ff0>=0 && ff0<n0-1 ) {
                ic00 = (int) Math.floor(ff0);
                ic01 = ic00 + 1;                
            } else if ( noExtrapolate && ff0 < -0.5 ) {  // would extrapolate immodestly
                it.putValue( result, fill );
                hasFill= true;
                continue;
            } else if ( noExtrapolate && ff0 >= n0 - 0.5 ) { // would extrapolate immodestly
                it.putValue( result, fill );
                hasFill= true;
                continue;
            } else if (ff0 < 0) {
                ic00 = 0; // extrapolate
                ic01 = 1;
            } else if (ff0 >= n0 - 1) {
                ic00 = n0 - 2; // extrapolate
                ic01 = n0 - 1;
            } 

            if ( ff1>=0 && ff1<n1-1 ) {
                ic10 = (int) Math.floor(ff1);
                ic11 = ic10 + 1;                
            } else if ( noExtrapolate && ff1 < -0.5 ) {  // would extrapolate immodestly
                it.putValue( result, fill );
                hasFill= true;
                continue;
            } else if ( noExtrapolate && ff1 >= n1 - 0.5 ) { // would extrapolate immodestly
                it.putValue( result, fill );
                hasFill= true;
                continue;            
            } else if (ff1 < 0) {
                ic10 = 0; // extrapolate
                ic11 = 1;
            } else if (ff1 >= n1 - 1) {
                ic10 = n1 - 2; // extrapolate
                ic11 = n1 - 1;
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
                hasFill= true;
            }

        }

        DataSetUtil.copyDimensionProperties( vv, result );
        
        //allow findex0 to provide DEPEND_0 and DEPEND_1.
        for ( int i=0; i<=findex0.rank(); i++ ) {
            QDataSet depend= (QDataSet) findex0.property( "DEPEND_"+i );
            if ( depend!=null ) {
                result.putProperty( "DEPEND_"+i, depend );
            }
        }

        if ( hasFill ) {
            result.putProperty( QDataSet.FILL_VALUE, fill );
        }
        
        return result;
    }
     
        
    /**
     * interpolate values from rank 2 dataset vv using fractional indices
     * in rank N findex, using bilinear interpolation.  See also interpolateGrid.
     *
     * @see #findex findex, the 1-D findex command
     * @param vv object convertible to rank 2 dataset.
     * @param findex0 object convertible to rank N dataset of fractional indices for the zeroth index.
     * @param findex1 object convertible to rank N dataset of fractional indices for the first index.
     * @return rank N dataset 
     */
    public static QDataSet interpolate( Object vv, Object findex0, Object findex1 ) {
        return interpolate( dataset(vv), dataset(findex0), dataset(findex1) );
    }    

    /**
     * interpolate values from rank 2 dataset vv using fractional indices
     * in rank N findex, using bilinear interpolation.  See also interpolateGrid.
     *
     * @param vv rank 2 dataset.
     * @param findex0 rank N dataset of fractional indices for the zeroth index.  This must be dimensionless, between -0.5 and L-0.5 and is typically calculated by the findex command.
     * @param findex1 rank N dataset of fractional indices for the first index.  This must be dimensionless, between -0.5 and L-0.5 and is typically calculated by the findex command.
     * @param findex2 rank N dataset of fractional indices for the second index.  This must be dimensionless, between -0.5 and L-0.5 and is typically calculated by the findex command.
     * @return rank N dataset 
     * @see #findex findex, the 1-D findex command
     * @see #interpolateGrid 
     */
    public static QDataSet interpolate( QDataSet vv, QDataSet findex0, QDataSet findex1, QDataSet findex2 ) {

        if ( vv.rank()!=3 ) throw new IllegalArgumentException("vv must be rank 3");
        
        if ( findex0.rank()>0 && findex0.length()!=findex1.length() && findex0.length()!=findex2.length() ) {
            throw new IllegalArgumentException("findex0, findex1, and findex2 must have the same geometry.");
        }
        if ( !isDimensionless(findex0) ) throw new IllegalArgumentException("findex0 argument should be dimensionless, expected output from findex command.");
        if ( !isDimensionless(findex1) ) throw new IllegalArgumentException("findex1 argument should be dimensionless, expected output from findex command.");
        if ( !isDimensionless(findex2) ) throw new IllegalArgumentException("findex2 argument should be dimensionless, expected output from findex command.");
        
        if ( !DataSetUtil.checkQube(vv) ) {
            logger.warning("vv is not a qube");
        }
        QDataSet fex0= extent(findex0);
        if ( ( fex0.value(1)-vv.length() ) / vv.length() > 100 ) {
            logger.warning("findex0 looks suspicious, where its max would result in unrealistic extrapolations");
        }
        if ( fex0.value(0) / vv.length() < -100 ) {
            logger.warning("findex0 looks suspicious, where its min would result in unrealistic extrapolations");
        }
        QDataSet fex1= extent(findex1);
        if ( ( fex1.value(1)-vv.length(0) ) / vv.length(0) > 100 ) {
            logger.warning("findex1 looks suspicious, where its max would result in unrealistic extrapolations");
        }
        if ( fex1.value(0) / vv.length(0) < -100 ) {
            logger.warning("findex1 looks suspicious, where its min would result in unrealistic extrapolations");
        }
        QDataSet fex2= extent(findex2);
        if ( ( fex2.value(1)-vv.length(0,0) ) / vv.length(0,0) > 100 ) {
            logger.warning("findex2 looks suspicious, where its max would result in unrealistic extrapolations");
        }
        if ( fex2.value(0) / vv.length(0,0) < -100 ) {
            logger.warning("findex2 looks suspicious, where its min would result in unrealistic extrapolations");
        }
        DDataSet result = DDataSet.create(DataSetUtil.qubeDims(findex0));

        QDataSet wds= DataSetUtil.weightsDataSet(vv);

        QubeDataSetIterator it = new QubeDataSetIterator(findex0);
        int ic00=0, ic01=0, ic10=0, ic11=0, ic20=0, ic21=0;
        int n0 = vv.length();
        int n1 = vv.length(0);
        int n2 = vv.length(0,0);
        
        double fill= -1e38;
        boolean hasFill= false;
        
        // Starting with v2014a_12, immodest extrapolations beyond 0.5 are no longer allowed.
        boolean noExtrapolate= true;
        
        while (it.hasNext()) {
            it.next();

            double ff0 = it.getValue(findex0);
            double ff1 = it.getValue(findex1);
            double ff2 = it.getValue(findex2);

            if ( ff0>=0 && ff0<n0-1 ) {
                ic00 = (int) Math.floor(ff0);
                ic01 = ic00 + 1;                
            } else if ( noExtrapolate && ff0 < -0.5 ) {  // would extrapolate immodestly
                it.putValue( result, fill );
                hasFill= true;
                continue;
            } else if ( noExtrapolate && ff0 >= n0 - 0.5 ) { // would extrapolate immodestly
                it.putValue( result, fill );
                hasFill= true;
                continue;
            } else if (ff0 < 0) {
                ic00 = 0; // extrapolate
                ic01 = 1;
            } else if (ff0 >= n0 - 1) {
                ic00 = n0 - 2; // extrapolate
                ic01 = n0 - 1;
            } 

            if ( ff1>=0 && ff1<n1-1 ) {
                ic10 = (int) Math.floor(ff1);
                ic11 = ic10 + 1;                
            } else if ( noExtrapolate && ff1 < -0.5 ) {  // would extrapolate immodestly
                it.putValue( result, fill );
                hasFill= true;
                continue;
            } else if ( noExtrapolate && ff1 >= n1 - 0.5 ) { // would extrapolate immodestly
                it.putValue( result, fill );
                hasFill= true;
                continue;            
            } else if (ff1 < 0) {
                ic10 = 0; // extrapolate
                ic11 = 1;
            } else if (ff1 >= n1 - 1) {
                ic10 = n1 - 2; // extrapolate
                ic11 = n1 - 1;
            }

            if ( ff2>=0 && ff2<n2-1 ) {
                ic20 = (int) Math.floor(ff2);
                ic21 = ic20 + 1;                
            } else if ( noExtrapolate && ff2 < -0.5 ) {  // would extrapolate immodestly
                it.putValue( result, fill );
                hasFill= true;
                continue;
            } else if ( noExtrapolate && ff2 >= n2 - 0.5 ) { // would extrapolate immodestly
                it.putValue( result, fill );
                hasFill= true;
                continue;            
            } else if (ff2 < 0) {
                ic20 = 0; // extrapolate
                ic21 = 1;
            } else if (ff2 >= n2 - 1) {
                ic20 = n2 - 2; // extrapolate
                ic21 = n2 - 1;
            }
            
            double alpha0 = ff0 - ic00;
            double alpha1 = ff1 - ic10;
            double alpha2 = ff2 - ic20;

            double vv000 = vv.value( ic20, ic00, ic10);
            double vv001 = vv.value( ic20, ic00, ic11);
            double vv010 = vv.value( ic20, ic01, ic10);
            double vv011 = vv.value( ic20, ic01, ic11);
            double vv100 = vv.value( ic21, ic00, ic10);
            double vv101 = vv.value( ic21, ic00, ic11);
            double vv110 = vv.value( ic21, ic01, ic10);
            double vv111 = vv.value( ic21, ic01, ic11);

            double ww000 = wds.value( ic20, ic00, ic10);
            double ww001 = wds.value( ic20, ic00, ic11);
            double ww010 = wds.value( ic20, ic01, ic10);
            double ww011 = wds.value( ic20, ic01, ic11);
            double ww100 = wds.value( ic21, ic00, ic10);
            double ww101 = wds.value( ic21, ic00, ic11);
            double ww110 = wds.value( ic21, ic01, ic10);
            double ww111 = wds.value( ic21, ic01, ic11);
            
            if ( ww000*ww001*ww010*ww011 *  ww100*ww101*ww110*ww111 > 0 ) {
                double beta0= 1-alpha0;
                double beta1= 1-alpha1;
                double beta2= 1-alpha2;
                double value= vv000 * beta0 * beta1 * beta2 
                        + vv001 * beta0 * alpha1 * beta2 
                        + vv010 * alpha0 * beta1 * beta2
                        + vv011 * alpha0 * alpha1 * beta2
                        + vv100 * beta0 * beta1 * alpha2 
                        + vv101 * beta0 * alpha1 * alpha2 
                        + vv110 * alpha0 * beta1 * alpha2
                        + vv111 * alpha0 * alpha1 * alpha2;
                it.putValue(result, value);
            } else {
                it.putValue(result, fill );
                hasFill= true;
            }

        }

        DataSetUtil.copyDimensionProperties( vv, result );
        
        //allow findex0 to provide DEPEND_0 and DEPEND_1.
        for ( int i=0; i<=findex0.rank(); i++ ) {
            QDataSet depend= (QDataSet) findex0.property( "DEPEND_"+i );
            if ( depend!=null ) {
                result.putProperty( "DEPEND_"+i, depend );
            }
        }

        if ( hasFill ) {
            result.putProperty( QDataSet.FILL_VALUE, fill );
        }
        
        return result;        
    }
    
    /**
     * like interpolate, but the findex is recalculated when the two bracketed points are closer in the 
     * modulo space than they would be in the linear space.
     * @param vv rank 1 dataset that is the data to be interpolated. (e.g. longitude from 0 to 360deg)
     * @param mod rank 0 dataset that is the mod of the space (e.g. 360deg), or rank 1 where the range is specified (e.g. -180 to 180).
     * @param findex rank N dataset of fractional indices.  This must be dimensionless and is typically calculated by the findex command.
     * @return the result, a rank 1 dataset with one element for each findex.
     * @see #interpolate(QDataSet,QDataSet)
     */
    public static QDataSet interpolateMod( QDataSet vv, QDataSet mod, QDataSet findex ) {
        if ( vv.rank()!=1 ) {
            throw new IllegalArgumentException("vv is not rank1");
        }
        if ( !isDimensionless(findex) ) throw new IllegalArgumentException("findex argument should be dimensionless, expected output from findex command.");
        QDataSet fex0= extent(findex);
        if ( ( fex0.value(1)-vv.length() ) / vv.length() > 100 ) {
            logger.warning("findex looks suspicious, where its max will result in unrealistic extrapolations");
        }
        if ( fex0.value(0) / vv.length() < -100 ) {
            logger.warning("findex looks suspicious, where its min will result in unrealistic extrapolations");
        }
        
        DDataSet result = DDataSet.create(DataSetUtil.qubeDims(findex));
        QubeDataSetIterator it = new QubeDataSetIterator(findex);
        int ic0=0, ic1=0;
        int n = vv.length();

        QDataSet wds= DataSetUtil.weightsDataSet( vv );
        Number fill= (Number)wds.property(QDataSet.FILL_VALUE);
        double dfill;
        if ( fill==null ) dfill= -1e38; else dfill= fill.doubleValue();
        result.putProperty( QDataSet.FILL_VALUE, dfill );
        boolean hasFill= false;

        QDataSet wfindex= DataSetUtil.weightsDataSet(findex);
        wfindex= copy(wfindex);
        
        double dmod;
        double dmodLimit;
        double base;
        double top;
        if ( mod.rank()==0 ) {
            dmod= DataSetUtil.asDatum(mod).doubleValue( SemanticOps.getUnits(vv).getOffsetUnits() );
            dmodLimit= dmod/2;
            base= 0;
            top= dmod;
        } else if ( mod.rank()==1 && mod.length()==2 ) {
            dmod= DataSetUtil.asDatum(mod.slice(1)).subtract( DataSetUtil.asDatum(mod.slice(0)) ).doubleValue( SemanticOps.getUnits(vv).getOffsetUnits() );
            dmodLimit= dmod/2;
            vv= Ops.subtract( vv,mod.slice(0) ); 
            base= mod.slice(0).value();
            top= mod.slice(1).value();
        } else {
            throw new IllegalArgumentException("mod must be rank 0 or rank 1 with two elements.");
        }
        
        boolean noExtrapolate= true;
        
        while (it.hasNext()) {
            it.next();

            if ( it.getValue(wfindex)==0 ) {
                it.putValue( result, dfill );
                hasFill= true;
                continue;
            }
            
            double ff = it.getValue(findex);

            if ( ff>=0 && ff<n-1 ) {
                ic0 = (int) Math.floor(ff);
                ic1 = ic0 + 1;                
            } else if ( noExtrapolate && ff < -0.5 ) {  // would extrapolate immodestly
                it.putValue( result, dfill );
                hasFill= true;
                continue;
            } else if ( noExtrapolate && ff >= n - 0.5 ) { // would extrapolate immodestly
                it.putValue( result, dfill );
                hasFill= true;
                continue;
            } else if (ff < 0) {
                ic0 = 0; // extrapolate
                ic1 = 1;
            } else if (ff >= n - 1) {
                ic0 = n - 2; // extrapolate
                ic1 = n - 1;
            }

            double alpha = ff - ic0;

            if ( wds.value(ic0)>0 && wds.value(ic1)>0 ) {
                double vv0 = vv.value(ic0);
                double vv1 = vv.value(ic1);
                while ( (vv1-vv0)> dmodLimit ) {
                    vv0= vv0 + dmod;
                }
                while ( (vv0-vv1)> dmodLimit ) {
                    vv1= vv1 + dmod;
                }
                double v= vv0 + alpha * (vv1 - vv0) + base;
                while ( v > top ) {
                    v= v- dmod;
                }
                while ( v < base ) {
                    v= v+ dmod;
                }
                it.putValue(result, v );
                
            } else {
                it.putValue(result, dfill );
                hasFill= true;
            }

        }
        DataSetUtil.copyDimensionProperties( vv, result );
        
        //allow findex to provide DEPEND_0 and DEPEND_1.
        for ( int i=0; i<=findex.rank(); i++ ) {
            QDataSet depend= (QDataSet) findex.property( "DEPEND_"+i );
            if ( depend!=null ) {
                result.putProperty( "DEPEND_"+i, depend );
            }
        }

        if ( hasFill ) {
            result.putProperty( QDataSet.FILL_VALUE, dfill );
        }

        return result;        
    }
    
    /**
     * interpolate values from rank 2 dataset vv using fractional indices
     * in rank N findex, using bilinear interpolation.  Here the two rank1
     * indexes form a grid and the result is rank 2.
     *
     * @see #findex findex, the 1-D findex command
     * @param vv rank 2 dataset.
     * @param findex0 rank 1 dataset of fractional indices for the zeroth index.
     * @param findex1 rank 1 dataset of fractional indices for the first index.
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

        if ( findex0.rank()!=1 ) {
            throw new IllegalArgumentException("findex0 must be rank 1");
        }
        
        if ( findex1.rank()!=1 ) {
            throw new IllegalArgumentException("findex1 must be rank 1");
        }

        DDataSet result = DDataSet.createRank2(findex0.length(),findex1.length());

        QDataSet wds= DataSetUtil.weightsDataSet(vv);


        int ic00=0, ic01=0, ic10=0, ic11=0;
        int n0 = vv.length();
        int n1 = vv.length(0);

        double fill= -1e38;
        
        QubeDataSetIterator it = new QubeDataSetIterator(findex0);
        
        boolean noExtrapolate= true;
        boolean hasFill= false;
        while (it.hasNext()) {
            it.next();
            QubeDataSetIterator it2= new QubeDataSetIterator(findex1);
            
            while ( it2.hasNext() ) {
                it2.next();
                double ff0 = it.getValue(findex0);
                
                if ( ff0>=0 && ff0<n0-1 ) {
                    ic00 = (int) Math.floor(ff0);
                    ic01 = ic00 + 1;                
                } else if ( noExtrapolate && ff0 < -0.5 ) {  // would extrapolate immodestly
                    result.putValue( it.index(0),it2.index(0),fill );
                    hasFill= true;
                    continue;
                } else if ( noExtrapolate && ff0 >= n0 - 0.5 ) { // would extrapolate immodestly
                    result.putValue( it.index(0),it2.index(0),fill );
                    hasFill= true;
                    continue;
                } else if (ff0 < 0) {
                    ic00 = 0; // extrapolate
                    ic01 = 1;
                } else if (ff0 >= n0 - 1) {
                    ic00 = n0 - 2; // extrapolate
                    ic01 = n0 - 1;
                } 
                
                double ff1 = it2.getValue(findex1);

                if ( ff1>=0 && ff1<n1-1 ) {
                    ic10 = (int) Math.floor(ff1);
                    ic11 = ic10 + 1;                
                } else if ( noExtrapolate && ff1 < -0.5 ) {  // would extrapolate immodestly
                    result.putValue( it.index(0),it2.index(0),fill );
                    hasFill= true;
                    continue;
                } else if ( noExtrapolate && ff1 >= n1 - 0.5 ) { // would extrapolate immodestly
                    result.putValue( it.index(0),it2.index(0),fill );
                    hasFill= true;
                    continue;            
                } else if (ff1 < 0) {
                    ic10 = 0; // extrapolate
                    ic11 = 1;
                } else if (ff1 >= n1 - 1) {
                    ic10 = n1 - 2; // extrapolate
                    ic11 = n1 - 1;
                }                

                double alpha0 = ff0 - ic00;
                double alpha1 = ff1 - ic10;

                double beta0= 1-alpha0;
                double beta1= 1-alpha1;
                
                double ww00=  wds.value(ic00, ic10);
                double ww01 = wds.value(ic00, ic11);
                double ww10 = wds.value(ic01, ic10);
                double ww11 = wds.value(ic01, ic11);

                double vv00 = ( ww00==0. ) ? 0 : vv.value(ic00, ic10);
                double vv01 = ( ww01==0. ) ? 0 : vv.value(ic00, ic11);
                double vv10 = ( ww10==0. ) ? 0 : vv.value(ic01, ic10);
                double vv11 = ( ww11==0. ) ? 0 : vv.value(ic01, ic11);
                             
                double beta0beta1 = beta0 * beta1;
                double beta0alpha1 = beta0 * alpha1;
                double alpha0beta1 = alpha0 * beta1;
                double alpha0alpha1 = alpha0 * alpha1;
                
                ww00 = ( beta0beta1 == 0. ) ? 1 : ww00;  // 
                ww01 = ( beta0alpha1 == 0. ) ? 1 : ww01;
                ww10 = ( alpha0beta1 == 0. ) ? 1 : ww10;
                ww11 = ( alpha0alpha1 == 0. ) ? 1 : ww11;

                if ( ww00*ww01*ww10*ww11 > 0 ) {
                    double value= vv00 * beta0beta1 + vv01 * beta0alpha1 + vv10 * alpha0beta1 + vv11 * alpha0alpha1;
                    result.putValue( it.index(0),it2.index(0),value );
                } else {
                    hasFill= true;
                    result.putValue( it.index(0),it2.index(0),fill );
                }
            } // second index
        }
        
        DataSetUtil.copyDimensionProperties( vv, result );
        QDataSet depend0= (QDataSet) findex0.property(QDataSet.DEPEND_0);
        if ( depend0!=null ) {
            result.putProperty( QDataSet.DEPEND_0, depend0 );
        }
        QDataSet depend1= (QDataSet) findex1.property(QDataSet.DEPEND_1);
        if ( depend1!=null ) {
            result.putProperty( QDataSet.DEPEND_1, depend1 );
        }
        if ( hasFill ) {
            result.putProperty( QDataSet.FILL_VALUE, fill );
        }
                
        QDataSet result1= result;
        if ( slice1 ) {
            result1= DataSetOps.slice1(result1,0);
        }
        if ( slice0 ) {
            result1= result1.slice(0);
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
     *<blockquote><pre>
     *r= where( valid( ds ) )
     *</pre></blockquote> 
     *
     * @param ds a rank N dataset that might have FILL_VALUE, VALID_MIN or VALID_MAX
     *   set.
     * @return a rank N dataset with the same geometry, with zeros where the data
     *   is invalid and &gt;0 where the data is valid.
     */
    public static QDataSet valid( QDataSet ds ) {
        // Note because data can always contain NaNs, there is no optimization for this.
        return DataSetUtil.weightsDataSet(ds);
    }
    
    /**
     * assign zeros to all the values of the dataset.  The 
     * dataset must be mutable.  This was used to verify Jython behavior.
     * @param ds 
     */
    public static void clearWritable( WritableDataSet ds ) {
        if ( ds.isImmutable() ) {
            throw new IllegalArgumentException("ds has been made immutable");
        }
        DataSetIterator it= new QubeDataSetIterator(ds);
        while ( it.hasNext() ) {
            it.next();
            it.putValue(ds,0.0);
        }
    }
    
    /**
     * returns 1 where the data is not NaN, Inf, etc  I needed this when I was working with
     * the RBSP polar scatter script.  Note valid should be used to check for valid data, which
     * also checks for NaN.
     * 
     * @param ds qdataset of any rank.
     * @return 1 where the data is not Nan or Inf, 0 otherwise.
     */
    public static QDataSet finite( QDataSet ds ) {
        ArrayDataSet result= ArrayDataSet.copy(ds);
        DataSetIterator it= new QubeDataSetIterator(ds);
        while ( it.hasNext() ) {
            it.next();
            double d1= it.getValue(ds);
            it.putValue( result, Double.isInfinite(d1) || Double.isNaN(d1) ? 0. : 1. );
        }
        return result;
    };
    
    /**
     * smooth over the first dimension (not the zeroth).  For example,
     * for ds[Time,Energy], this will smooth over energy.
     * @param ds rank 2 dataset.
     * @param size the boxcar size
     * @return smoothed dataset with the same geometry.
     * @see #detrend1(org.das2.qds.QDataSet, int) 
     */
    public static QDataSet smooth1(QDataSet ds, int size) {
        switch (ds.rank()) {
            case 1: {
                throw new IllegalArgumentException("data must be rank 2 or more");
            }
            case 2: {
                ArrayDataSet result= ArrayDataSet.copy(ds);
                for ( int i=0; i<ds.length(); i++ ) {
                    QDataSet result1= BinAverage.boxcar( ds.slice(i), size );
                    for ( int j=0; j<ds.length(0); j++ ) {
                        result.putValue( i,j, result1.value(j) );
                    }
                }
                return result;
            }
            default:
                throw new IllegalArgumentException("only rank 1 and rank 2");
        } 
    }
    
    /**
     * smooth in both the first and second dimensions.  Presently, this just calls smooth and then smooth1.
     * @param ds rank 2 data
     * @param n0 the boxcar size in the first dimension
     * @param n1 the boxcar size in the second dimension
     * @return data with the same geometry
     * @see #smooth(org.das2.qds.QDataSet, int) 
     * @see #smooth1(org.das2.qds.QDataSet, int) 
     */
    public static QDataSet smooth2d( QDataSet ds, int n0, int n1 ) {
        if ( ds.rank()!=2 ) throw new IllegalArgumentException("data must be rank 2");
        ds= smooth(ds,n0);
        ds= smooth1(ds,n1);
        return ds;
    }
    
    /**
     * run boxcar average over the dataset, returning a dataset of same geometry.  Points near the edge are simply copied from the
     * source dataset.  The result dataset contains a property "weights" that is the weights for each point.  
     * 
     * For rank 2 datasets, the smooth is done on the zeroth dimension, typically time.  Note IDL does the smooth 
     * in both X and Y.  
     *
     * @param ds a rank 1 or rank 2 dataset of length N
     * @param size the number of adjacent bins to average
     * @return rank 1 or rank 2 dataset of length N
     * @see #smooth2d(org.das2.qds.QDataSet, int, int) 
     */
    public static QDataSet smooth(QDataSet ds, int size) {
        switch (ds.rank()) {
            case 1: {
                DDataSet result = BinAverage.boxcar(ds, size);
                DataSetUtil.copyDimensionProperties( ds, result );
                return result;
            }
            case 2: {
                ArrayDataSet result= ArrayDataSet.copy(ds);
                if ( SemanticOps.isRank2Waveform(ds) ) {
                    throw new IllegalArgumentException("rank 2 waveform is not supported, use flattenWaveform first.");
                } else {
                    for ( int j=0; j<ds.length(0); j++ ) {
                        QDataSet result1= BinAverage.boxcar( unbundle(ds,j), size );
                        for ( int i=0; i<ds.length(); i++ ) {
                            result.putValue( i,j, result1.value(i) );
                        }
                    }
                }
                return result;
            }
            default:
                throw new IllegalArgumentException("only rank 1 and rank 2");
        } 
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
        
        switch (yy.rank()) {
            case 1:
                fit= new LinFit( xx.trim(0,size), yy.trim(0,size) );
                for ( int i=0; i<size/2; i++ ) {
                    yysmooth.putValue( i, xx.value(i)*fit.getB() + fit.getA() );
                }   fit= new LinFit( xx.trim(n-size,n), yy.trim(n-size,n) );
                for ( int i=n-(size+1)/2; i<n; i++ ){
                    yysmooth.putValue( i, xx.value(i)*fit.getB() + fit.getA() );
                }   
                break;
            case 2:
                for ( int j=0; j<yy.length(0); j++ ) {
                    QDataSet yy1= Ops.unbundle(yy, j);
                    fit= new LinFit( xx.trim(0,size), yy1.trim(0,size) );
                    for ( int i=0; i<size/2; i++ ) {
                        yysmooth.putValue( i, j, xx.value(i)*fit.getB() + fit.getA() );
                    }
                    fit= new LinFit( xx.trim(n-size,n), yy1.trim(n-size,n) );
                    for ( int i=n-(size+1)/2; i<n; i++ ){
                        yysmooth.putValue( i, j, xx.value(i)*fit.getB() + fit.getA() );
                    }
                }   
                break;
            default:
                throw new IllegalArgumentException("yy must be rank 1 or rank 2: "+yy );
        }

        return yysmooth;
    }
    
    public static QDataSet smoothFit( Object xx, Object yy, int size) {
        return smoothFit( dataset(xx), dataset(yy), size );
    }
    
    /**
     * remove D/C and low-frequency components from the data by subtracting
     * out the smoothed data with a boxcar of the given size.  Points on the 
     * end are zero.
     * @param yy rank 1 dataset
     * @param size size of the boxcar
     * @return dataset 
     */
    public static QDataSet detrend( QDataSet yy, int size ) {
        return subtract( yy, smooth( yy, size ) );
    }
    
    public static QDataSet detrend( Object yy, int size ) {
        return detrend( dataset(yy), size );
    }
           
    /**
     * remove D/C and low-frequency components from the data by subtracting
     * out the smoothed data with a boxcar of the given size, along each
     * record. Points on the end are zero.
     * @param yy rank 2 dataset
     * @param size size of the boxcar
     * @return dataset 
     * @see #smooth1(org.das2.qds.QDataSet, int) 
     */
    public static QDataSet detrend1( QDataSet yy, int size ) {
        return subtract( yy, smooth1( yy, size ) );
    }
    
    /**
     * remove the data which is 3 sigmas from the mean of the data.
     * @param ds rank 1 dataset.
     * @return cleaned dataset of the same geometry.
     */
    public static QDataSet cleanData( QDataSet ds ) {
        return cleanData( ds, -1 );
    }
    
    /**
     * remove the data which is N sigmas (stddev) from the mean.  This is 
     * to replicate the "clean_data" function used by CDAWeb at NASA/Goddard.
     * @param ds rank 1 dataset.
     * @param size size for a boxcar, or -1 for the whole dataset.
     * @return cleaned dataset of the same geometry.
     */    
    public static QDataSet cleanData( QDataSet ds, int size ) {
        return cleanData( ds, 3, size );
    }
    
    /**
     * remove the data which is N sigmas (stddev) from the mean.
     * @param ds rank 1 dataset.
     * @param nsigma remove data more than this many stddevs from mean.
     * @param size size for a boxcar, or -1 for the whole dataset.
     * @return cleaned dataset of the same geometry.
     */
    public static QDataSet cleanData( QDataSet ds, double nsigma, int size ) {
        QDataSet mean;
        QDataSet sig3;
        if ( size==-1 ) {
            mean = mean(ds);
            sig3 = multiply( stddev(ds), nsigma );
        } else {
            mean = smooth(ds,size);
            sig3 = multiply( smooth( abs( subtract( ds, mean ) ), size ), nsigma );
        }
        QDataSet w= where( gt( abs( subtract( ds, mean ) ), sig3 ) );
        WritableDataSet wds= copy(ds);
        Number fill= (Number)ds.property(QDataSet.FILL_VALUE);
        QubeDataSetIterator it= new QubeDataSetIterator(ds);
        it.setIndexIteratorFactory( 0, new QubeDataSetIterator.IndexListIteratorFactory( w ) );
        double FILL= ((Number)( fill==null ? Double.NaN : fill )).doubleValue();
        while ( it.hasNext() ) {
            it.next();
            it.putValue( wds, FILL );
        }
        return wds;
    }
    
    /**
     * Mean function that returns the average of the valid elements of a rank N dataset
     * @param ds rank N dataset
     * @return rank 0 dataset
     * @see #mode
     * @see #median
     * @see #variance(org.das2.qds.QDataSet) 
     * @see #meanAverageDeviation(org.das2.qds.QDataSet) 
     * @author mmclouth
     */
    public static QDataSet mean( QDataSet ds ) {
        double avg = 0;
        int n= 0;
        double offs= Double.NaN;
        
        DataSetIterator it= new QubeDataSetIterator(ds);
        QDataSet wds= valid(ds);
        
        while ( it.hasNext() )  {
            it.next();
            if ( it.getValue(wds)==0 ) continue;
            n= n+1;
            if ( n==1 ) {
                offs= it.getValue(ds);
            }
            avg += ( it.getValue(ds) - offs );
        }
        double m = offs + avg / n;
        return DataSetUtil.asDataSet( m,SemanticOps.getUnits(ds) );
    }
    
    public static QDataSet mean( Object o ) {
        return mean( dataset(o) );
    }
    
    /**
     * return the most frequently occurring element of the valid elements of a rank N dataset
     * @param ds rank N dataset.
     * @return the rank 0 dataset
     * @see #mean
     * @see #median
     */
    public static QDataSet mode( QDataSet ds ) {
        
        Map<Double,Integer> m= new HashMap<>();
        DataSetIterator it= new QubeDataSetIterator(ds);
        QDataSet wds= valid(ds);
        
        while ( it.hasNext() ) {
            it.next();
            double w= it.getValue(wds);
            if ( w>0 ) {
                double d= it.getValue(ds);
                Integer i= m.get(d);
                if ( i==null ) {
                    m.put( d, 1 );
                } else {
                    m.put( d, i+1 );
                }
            }
        }
        int max= 0;
        double maxd= Double.NaN;
        for ( Entry<Double,Integer> vv : m.entrySet() ) {
            if ( vv.getValue() > max ) {
                max= vv.getValue();
                maxd= vv.getKey();
            }
        }
        DDataSet result= DDataSet.create( new int[0] );
        result.putValue(maxd);
        DataSetUtil.copyDimensionProperties( ds,result );
        return result;
        
    }
       
    /**
     * Median function that sorts a rank N dataset and returns its median.  
     * If lists are equal in size (even number of elements), always choose 
     * first element of 'more' list
     * @param o object which can be interpreted as a dataset.
     * @return rank 0 dataset
     * @author mmclouth
     * @see #mean
     * @see #mode
     */ 
    public static QDataSet median( Object o ) {
        return median( dataset(o) );
    }
    
    /**
     * Median function that sorts a rank N dataset and returns its median.  
     * If lists are equal in size (even number of elements), always choose 
     * first element of 'more' list
     * @param ds rank N dataset.
     * @return rank 0 dataset
     * @author mmclouth
     * @see #mean
     * @see #mode
     */
    public static QDataSet median( QDataSet ds ) {
        
        LinkedList<Double> less= new LinkedList<>();
        LinkedList<Double> more= new LinkedList<>();
        
        QDataSet wds= valid(ds);
        DataSetIterator iter= new QubeDataSetIterator(ds);
        
        // sort elements into two lists 
        while ( iter.hasNext() ) {
            iter.next();
            
            if ( iter.getValue(wds)==0 ) continue;
            
            double d= iter.getValue(ds);
            
            if ( less.isEmpty() ) {
                less.add(d);
                Collections.sort(less);
            } else if ( less.getLast() >= d) {
                less.add(d);
                Collections.sort(less);
            } else {
                more.add(d);
                Collections.sort(more);
            }
            // balance the two sets, so that they are within one in size.
            if ( less.size()<more.size()-1 ) {
                double mv= more.getFirst();
                less.add( mv );
                more.remove( mv );
                Collections.sort(less);
            } else if ( less.size()-1>more.size() ) {
                double mv= less.getLast();
                less.remove( mv );
                more.add( mv );
                Collections.sort(more);
            }
        }
        
        //assign the median based on which list is bigger
        //if lists are equal in size (even number of elements), always choose first element of 'more' list
        double ans;
        if ( less.size() > more.size() )  {
            ans = less.getLast();
        }
        else if ( less.size()< more.size() )  {
            ans = more.getFirst();
        }
        else  {
            if ( more.isEmpty() ) {
                ans= Double.NaN;
            } else {
                ans = more.getFirst();
            }
        }
    
        return DataSetUtil.asDataSet( ans, SemanticOps.getUnits(ds) );
    }
    
    /**
     * standard deviation function.
     * @param o an object which can be interpreted as a rank N dataset.
     * @return rank 0 dataset with units matching those of the input.
     * @author mmclouth
     */
    public static QDataSet stddev( Object o ) {
        return stddev( dataset(o) );
    }
    
    /**
     * standard deviation function.
     * @param ds rank N dataset.
     * @return rank 0 dataset with units matching those of the input.
     * @author mmclouth
     */
    public static QDataSet stddev( QDataSet ds ) {

        int n = 0;

        DataSetIterator iter= new QubeDataSetIterator(ds);
        
        double sum = 0;
        while ( iter.hasNext() )  {
            iter.next();
            sum += iter.getValue(ds);
            n= n+1;
        }
        double u = sum / n;
        double sub;
        double square = 0;
        
        iter= new QubeDataSetIterator(ds);
        while ( iter.hasNext() )  {
            iter.next();
            sub = ( iter.getValue(ds) - u);
            square += Math.pow(sub, 2);
        }
        double undersqrrt = square / (n-1);
        double result = sqrt(undersqrrt);
        //System.out.println(result);
        return DataSetUtil.asDataSet( result, SemanticOps.getUnits(ds).getOffsetUnits() );
    }

    /**
     * variance function is the square of the stddev.
     * @param o object which can be interpreted as rank 1 dataset.
     * @return Rank 0 QDataSet containing the variance.  The result is currently dimensionless, but this will change.
     * @author mmclouth
     * @see #stddev
     */
    public static QDataSet variance( Object o ) {
        return variance( dataset(o) );
    }
    
    /**
     * variance function is the square of the stddev.
     * @param ds rank 1 dataset.
     * @return Rank 0 QDataSet containing the variance.  The result is currently dimensionless, but this will change.
     * @author mmclouth
     * @see #stddev
     */
    public static QDataSet variance( QDataSet ds ) {
        return Ops.pow(stddev(ds),2);
    }
    
    /**
     * return the Mean Average Deviation (MAD) of the rank N dataset.  
     * The result will contain the USER_PROPERTIES with a map containing 
     * the mean and number of points.
     * @param ds the rank N dataset.
     * @return the rank 0 mean average deviation of the dataset.
     * @see #mean(org.das2.qds.QDataSet) 
     * @see BinAverage#binMeanAverageDeviation(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet meanAverageDeviation( QDataSet ds ) {
        
        QDataSet mean= mean( ds );
        double meanDouble= mean.value();
        
        double sub;
        double sum = 0;
        int n=0;
        
        QDataSet w= DataSetUtil.weightsDataSet(ds);
        
        DataSetIterator iter= new QubeDataSetIterator(ds);
        while ( iter.hasNext() )  {
            iter.next();
            if ( iter.getValue(w)>0 ) {
                sub = ( iter.getValue(ds) - meanDouble );
                sum += sub<0 ? -sub : sub;
                n+= 1;
            }
        }

        double result = sum / n;
        QDataSet results= DataSetUtil.asDataSet( result, SemanticOps.getUnits(ds).getOffsetUnits() );
        
        Map<String,Object> user= new HashMap<>();
        user.put( "mean", mean );
        user.put( "n", n );
        
        return putProperty( results, "USER_PROPERTIES", user );
    }
    
    /**
     * fill in the missing values by copying nearest data points.  All data
     * in the result will be copies of elements found in the result, but no
     * regard is given to how far a point is shifted.  This was
     * motivated by supporting fill in median.
     * @param ds
     * @return dataset that does not contain fill.
     */
    public static QDataSet neighborFill( QDataSet ds ) {
        QDataSet w= Ops.copy( Ops.valid(ds) );
        WritableDataSet wds=null;
        while ( Ops.reduceMin( w, 0 ).value()==0 ) {
            wds= copy(ds);
            for ( int i=1; i<ds.length(); i++ ) {
                if ( w.value(i)==0 && w.value(i-1)>0 ) {
                    wds.putValue(i,w.value(i-1));
                }
            }
            for ( int i=ds.length()-2; i>=0; i-- ) {
                if ( w.value(i)==0 && w.value(i+1)>0 ) {
                    wds.putValue(i,w.value(i+1));
                }                
            }
            w= Ops.valid(wds);
        }
        return wds==null ? ds : wds;
    }
       
//    private static boolean isSorted( List l ) {
//        if ( l.size()==0 ) return true;
//        Number v= (Number)l.get(0);
//        for ( int i=1; i<l.size(); i++ ) {
//            Number d= (Number)l.get(i);
//            if ( v.doubleValue()>d.doubleValue() ) {
//                return false;
//            }
//        }
//        return true;
//    }
//    
    
    /**
     * 1-D median filter with a boxcar of the given size.  The first size/2
     * elements, and the last size/2 elements are copied from the input.
     * @param ds rank 1 or rank 2 dataset.  Future implementations may support higher rank data.
     * @param size the boxcar size
     * @return rank 1 or rank 2 dataset.
     * @see #smooth(org.das2.qds.QDataSet, int) 
     */
    public static QDataSet medianFilter( QDataSet ds, int size ) {
        if ( ds.rank()==2 ) {
            ArrayDataSet res= ArrayDataSet.copy(ds);
            for ( int j=0; j<ds.length(0); j++ ) {
                QDataSet in1= Ops.unbundle(ds,j);
                QDataSet r= where(valid(in1)); // note rank 2 supports fill.
                if ( r.length()<=ds.length()/2 ) {
                    for ( int i=0; i<in1.length(); i++ ) {
                        res.putValue(i,j,in1.value(i));
                    }
                    continue;
                } else if ( r.length()<in1.length() ) {
                    in1= applyIndex(in1,r);
                }
                QDataSet d1= medianFilter( in1, size );
                for ( int i=0; i<in1.length(); i++ ) {
                   res.putValue((int)r.value(i),j,d1.value(i));
                }
            }
            return res;
        }
        if ( Ops.reduceMin( Ops.valid(ds), 0 ).value()==0 ){
            throw new IllegalArgumentException("fill data is not supported");
        }
        if ( size>ds.length()/2 ) {
            throw new IllegalArgumentException("size cannot be greater than ds.length()/2");
        }
        if ( size<3 ) throw new IllegalArgumentException("size cannot be less than 3");
        
        ArrayDataSet res= ArrayDataSet.copy(ds);

        LinkedList<Double> less= new LinkedList<>();
        LinkedList<Double> more= new LinkedList<>();
        LinkedList<Double> vv= new LinkedList<>();
        int hsize= size/2;
        for ( int i=0; i<size-1; i++ ) {
            double d=ds.value(i);
            vv.add(d);
            if ( less.isEmpty() ) {
                less.add(d);
            } else if ( less.getLast()<d ) {
                int index= Collections.binarySearch( more,d );
                if ( index<0 ) index= ~index;
                more.add(index,d);
            } else {
                int index= Collections.binarySearch( less,d );
                if ( index<0 ) index= ~index;
                less.add(index,d);
            }
            // balance the two sets, so that they are within one in size.
            if ( less.size()<more.size()-1 ) {
                double mv= more.getFirst();
                less.add( mv );
                more.remove( 0 );
            } else if ( less.size()-1>more.size() ) {
                double mv= less.getLast();
                more.add( 0, mv );
                less.remove( less.size()-1 );
            }
//            if ( !isSorted(less) ) {
//                throw new IllegalArgumentException("less bad");
//            }
//            if ( !isSorted(more) ) {
//                throw new IllegalArgumentException("more bad");
//            }
            res.putValue(i,d);
        }
        for ( int i=hsize; i<ds.length()-hsize; i++ ) {
            double d=ds.value(i+hsize);
            vv.add(d);
            if ( less.getLast()<d ) {
                int index= Collections.binarySearch( more,d ); // not terribly efficient, but not inefficient... O(n)
                if ( index<0 ) index= ~index;
                more.add(index,d);
            } else {
                int index= Collections.binarySearch( less,d );
                if ( index<0 ) index= ~index;
                less.add(index,d);
            }
            if ( less.size()>more.size() ) {
                res.putValue(i,less.getLast());
            } else {
                res.putValue(i,more.getFirst());
            }
            double rm= vv.remove(0);
            if ( less.getLast()>=rm ) {
                less.remove(rm);
            } else {
                more.remove(rm);
            }
            
            // balance the two sets, so that they are within one in size.
            if ( less.size()<more.size()-1 ) {
                double mv= more.getFirst();
                less.add( mv );
                more.remove( 0 );
            } else if ( less.size()-1>more.size() ) {
                double mv= less.getLast();
                more.add( 0, mv );
                less.remove( less.size()-1 );
            }
//            if ( !isSorted(less) ) {
//                throw new IllegalArgumentException("less bad");
//            }
//            if ( !isSorted(more) ) {
//                throw new IllegalArgumentException("more bad");
//            }            
        }
        for ( int i=ds.length()-hsize; i<ds.length(); i++ ) {
            double d=ds.value(i);
            res.putValue(i,d);
        }
        return res;
    }

    /**
     * 2-D median filter with a boxcar of the given size.  The first size/2
     * elements, and the last size/2 elements are copied from the input.
     * This is just coded and is not working.  It should either be finished in
     * the next couple of months or abandoned.
     * @param ds rank 1 or rank 2 dataset.  Future implementations may support higher rank data.
     * @param size1 the boxcar size in the first dimension
     * @param size2 the boxcar size in the second dimension
     * @return rank 2 dataset.
     * @see #smooth(org.das2.qds.QDataSet, int) 
     */
    private static QDataSet medianFilter2d( QDataSet ds, int size1, int size2 ) {
        
        if ( ds.rank()!=2 ) {
            throw new IllegalArgumentException("dataset must be rank 2");
        }
        
        int n1= ds.length();
        int n2= ds.length(0);

        if ( size1>n1/2 ) {
            throw new IllegalArgumentException("size1 cannot be greater than ds.length()/2");
        }
        if ( size2>n2/2) {
            throw new IllegalArgumentException("size2 cannot be greater than ds.length(0)/2");
        }
        if ( size1<3 ) throw new IllegalArgumentException("size1 cannot be less than 3");
        if ( size2<3 ) throw new IllegalArgumentException("size2 cannot be less than 3");
        
        ArrayDataSet res= ArrayDataSet.copy(ds);

        LinkedList<Double> less;
        LinkedList<Double> more;
        LinkedList<Double> vv= new LinkedList<>();
        int hsize1= size1/2; // half-size
        int hsize2= size2/2;

        // copy "left" edge
        for ( int i=0; i<size1-1; i++ ) {
            for ( int j=0; j<size2-1; j++ ) {
                double d=ds.value(i,j);
                res.putValue(i,j,d);
            }
        }
        // do middle columns
        for ( int i=hsize1; i<n1-hsize1; i++ ) {
            less= new LinkedList<>();
            more= new LinkedList<>();
            for ( int i1=0; i1<size1-1; i1++ ) {
                for ( int j=0; j<size2-1; j++ ) {
                    double d=ds.value(i1,j);
                    vv.add(d);
                    if ( less.isEmpty() ) {
                        less.add(d);
                    } else if ( less.getLast()<d ) {
                        int index= Collections.binarySearch( more,d );
                        if ( index<0 ) index= ~index;
                        more.add(index,d);
                    } else {
                        int index= Collections.binarySearch( less,d );
                        if ( index<0 ) index= ~index;
                        less.add(index,d);
                    }
                    // balance the two sets, so that they are within one in size.
                    if ( less.size()<more.size()-1 ) {
                        double mv= more.getFirst();
                        less.add( mv );
                        more.remove( 0 );
                    } else if ( less.size()-1>more.size() ) {
                        double mv= less.getLast();
                        more.add( 0, mv );
                        less.remove( less.size()-1 );
                    }
                    res.putValue(i1,j,d);
                }
            }
            
            for ( int j=hsize2; j<n2-hsize2; j++ ) {
                
                double d=ds.value(i+hsize1,j+hsize2);
                vv.add(d);
                if ( less.getLast()<d ) {
                    int index= Collections.binarySearch( more,d ); // not terribly efficient, but not inefficient... O(1), presuming hsize<<n1
                    if ( index<0 ) index= ~index;
                    more.add(index,d);
                } else {
                    int index= Collections.binarySearch( less,d );
                    if ( index<0 ) index= ~index;
                    less.add(index,d);
                }
                if ( less.size()>more.size() ) {
                    res.putValue(i,j,less.getLast());
                } else {
                    res.putValue(i,j,more.getFirst());
                }
                double rm= vv.remove(0);
                if ( less.getLast()>=rm ) {
                    less.remove(rm);
                } else {
                    more.remove(rm);
                }

                // balance the two sets, so that they are within one in size.
                if ( less.size()<more.size()-1 ) {
                    double mv= more.getFirst();
                    less.add( mv );
                    more.remove( 0 );
                } else if ( less.size()-1>more.size() ) {
                    double mv= less.getLast();
                    more.add( 0, mv );
                    less.remove( less.size()-1 );
                }
            }
            for ( int j=n2-hsize2; j<n2; j++ ) {
                double d=ds.value(i,j);
                res.putValue(i,j,d);
            }
        }
        for ( int i=n1-hsize1; i<n1; i++ ) {
            for ( int j=n2-hsize2; j<n2; j++ ) {
                double d=ds.value(i,j);
                res.putValue(i,j,d);
            }
        }
        return res;
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
        return contour( dataset(tds), dataset(vv) );
    }
    
    /**
     * return array that is the differences between each successive pair in the dataset.
     * Result[i]= ds[i+1]-ds[i], so that for an array with N elements, an array with
     * N-1 elements is returned.  When the data has a DEPEND_0, the result
     * will have a DEPEND_0 which contains the average of the corresponding points.
     * @param ds a rank 1 dataset with N elements.
     * @return a rank 1 dataset with N-1 elements.
     * @see #accum(org.das2.qds.QDataSet) 
     */
    public static QDataSet diff(QDataSet ds) {
        if (ds.rank() > 1) {
            throw new IllegalArgumentException("only rank 1");
        }
        Units u= (Units) ds.property(QDataSet.UNITS);
        if ( u!=null ) {
            if ( UnitsUtil.isNominalMeasurement(u) ) {
                throw new IllegalArgumentException("cannot take differences of nominal or enumeration data");
            }
        }
        ArrayDataSet result= ArrayDataSet.createRank1( DataSetOps.getComponentType(ds), ds.length()-1 );
        QDataSet w1= DataSetUtil.weightsDataSet(ds);
        QDataSet dep0ds= (QDataSet) ds.property(QDataSet.DEPEND_0);
        DDataSet dep0= null;
        if ( dep0ds!=null ) {
            dep0= DDataSet.createRank1( ds.length()-1 );
            DataSetUtil.putProperties( DataSetUtil.getProperties(dep0ds), dep0 );
        }
        double fill= DataSetOps.suggestFillForComponentType( DataSetOps.getComponentType(ds) );

        for ( int i=0; i<result.length(); i++ ) {
            if ( w1.value(i)>0 && w1.value(i+1)>0 ) {
                result.putValue(i, ds.value(i+1) - ds.value(i) );
            } else {
                result.putValue(i,fill);
            }
            if ( dep0ds!=null ) {
                assert dep0!=null;
                dep0.putValue(i, ( dep0ds.value(i) + ( dep0ds.value(i+1) - dep0ds.value(i) ) / 2 ) );
            }
        }
        result.putProperty(QDataSet.FILL_VALUE, fill );
        if ( u!=null ) result.putProperty(QDataSet.UNITS, u.getOffsetUnits() );
        result.putProperty(QDataSet.NAME, null );
        result.putProperty(QDataSet.MONOTONIC, null );
        if ( dep0ds!=null ) {
            result.putProperty( QDataSet.DEPEND_0, dep0 );
        }
        String label= (String) ds.property(QDataSet.LABEL);
        if ( label!=null ) result.putProperty(QDataSet.LABEL, "diff("+label+")" );

        return result;
    }

    public static QDataSet diff( Object ds ) {
        return diff( dataset(ds) );
    }
    
    
    /**
     * return an array that is the running sum of each element in the array,
     * starting with the value accum.
     * Result[i]= accum + total( ds[0:i+1] )
     * @param accumDs the initial value of the running sum.  Last value of Rank 0 or Rank 1 dataset is used, or may be null.
     * @param ds each element is added to the running sum
     * @return the running of each element in the array.
     * @see #diff(org.das2.qds.QDataSet) 
     */
    public static QDataSet accum( QDataSet accumDs, QDataSet ds ) {
        if (ds.rank() > 1) {
            throw new IllegalArgumentException("only rank 1");
        }
        double accum=0;
        QDataSet accumDep0Ds;
        double accumDep0=0;
        QDataSet dep0ds= (QDataSet) ds.property(QDataSet.DEPEND_0);
        Units units;
        UnitsConverter uc= null;
        if ( accumDs==null ) {
            accumDep0= dep0ds!=null ? dep0ds.value(0) : 0;
            units= Units.dimensionless;
        } else if ( accumDs.rank()==0 ) {
            accum= accumDs.value();
            accumDep0Ds= (QDataSet) accumDs.property( QDataSet.CONTEXT_0 );
            if ( accumDep0Ds!=null ) accumDep0= accumDep0Ds.value(); else accumDep0=0;
            units= SemanticOps.getUnits(accumDs);
            Units ddUnits= SemanticOps.getUnits(ds);
            uc= UnitsConverter.getConverter( ddUnits, units.getOffsetUnits() );
        } else if ( accumDs.rank()==1 ) {
            accum= accumDs.value(accumDs.length()-1);
            accumDep0Ds= (QDataSet)  accumDs.property( QDataSet.DEPEND_0 );
            if ( accumDep0Ds!=null ) accumDep0= accumDep0Ds.value(accumDs.length()); else accumDep0=0;
            units= SemanticOps.getUnits(accumDs);
            Units ddUnits= SemanticOps.getUnits(ds);
            uc= UnitsConverter.getConverter( ddUnits, units.getOffsetUnits() );
        } else {
            throw new IllegalArgumentException("accumDs must be rank 0 or rank 1");
        }
        if ( uc==UnitsConverter.IDENTITY ) uc=null;        
        WritableDataSet result= zeros( ds );
        result.putProperty( QDataSet.UNITS, units );
        DDataSet dep0= null;
        if ( dep0ds!=null ) {
            dep0= DDataSet.createRank1( ds.length() );
            DataSetUtil.putProperties( DataSetUtil.getProperties(dep0ds), dep0 );
        }
        for ( int i=0; i<result.length(); i++ ) {
            double d= ds.value(i);
            accum+= uc==null ?  d: uc.convert(d);
            result.putValue(i,accum);
            if ( dep0ds!=null ) {
                assert dep0!=null;
                dep0.putValue(i, ( accumDep0 + dep0ds.value(i)) / 2 );
                accumDep0= dep0ds.value(i);
            }
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
     * @see #diff(org.das2.qds.QDataSet) 
     */
    public static QDataSet accum( QDataSet ds ) {
        return accum( null, ds );
    }

    public static QDataSet accum( Object ds ) {
        return accum( null, dataset(ds) );
    }
    
    /**
     * for each element i of ds, set the result[i] to the maximum of ds[0:(i+1)]
     * @param ds rank 1 dataset
     * @return the cumulative maximum
     */
    public static QDataSet cumulativeMax( QDataSet ds ) {
        if ( ds.rank()!=1 ) throw new IllegalArgumentException("argument must be rank 1");
        ArrayDataSet result= ArrayDataSet.copy(ds);
        QDataSet w= Ops.valid(ds);
        double max= -1*Double.MAX_VALUE;
        for ( int i=0; i<result.length(); i++ ) {
            if ( w.value(i)>0 ) {
                double t= result.value(i);
                if ( t>max ) max= t;
                result.putValue( i, max );
            } else {
                result.putValue( i, Double.NaN );
            }
        }
        return result;
    }
    
    /**
     * for each element i of ds, set the result[i] to the minimum of ds[0:(i+1)]
     * @param ds rank 1 dataset
     * @return the cumulative minimum
     */
    public static QDataSet cumulativeMin( QDataSet ds ) {
        if ( ds.rank()!=1 ) throw new IllegalArgumentException("argument must be rank 1");
        ArrayDataSet result= ArrayDataSet.copy(ds);
        QDataSet w= Ops.valid(ds);
        double min= Double.MAX_VALUE;
        for ( int i=0; i<result.length(); i++ ) {
            if ( w.value(i)>0 ) {
                double t= result.value(i);
                if ( t<min ) min= t;
                result.putValue( i, min );
            } else {
                result.putValue( i, Double.NaN );
            }
        }
        return result;
    }
    
    /**
     * append two datasets that are QUBEs.  DEPEND_0 and other metadata is
     * handled as well.  So for example: 
     *<blockquote><pre>
     *ds1= findgen(10)
     *ds2= findgen(12)
     *print append(ds1,ds2)  ; dataSet[22] (dimensionless)
     *</pre></blockquote>     
     * If both datasets are ArrayDataSets and of the same component type, then
     * the result will have this type as well.  Otherwise DDataSet is returned.
     * @param ds1 null or rank N dataset
     * @param ds2 rank N dataset with compatible geometry.
     * @see #concatenate(org.das2.qds.QDataSet, org.das2.qds.QDataSet) which is the same thing.
     * @return 
     */
    public static QDataSet append( QDataSet ds1, QDataSet ds2 ) {
        if ( ds1==null ) {
            if ( ds2.rank()>0 ) {
                if ( !DataSetUtil.validate( ds2, null ) ) 
                    throw new RuntimeException("dataset input ds2 to append doesn't validate.");
                return ds2;
            } else {
                if ( !DataSetUtil.validate( ds2, null ) ) 
                    throw new RuntimeException("dataset input ds2 to append doesn't validate.");
                ArrayDataSet result= ArrayDataSet.createRank1(ArrayDataSet.guessBackingStore(ds2),1);
                result.putValue( 0, ds2.value() );
                DataSetUtil.copyDimensionProperties( ds2, result );
                QDataSet c= (QDataSet)ds2.property(QDataSet.CONTEXT_0);
                if ( c!=null && c.rank()==0 ) {
                    result.putProperty( QDataSet.DEPEND_0, append( null, c ) );
                }
                return result;
            }
        } if ( ds1 instanceof BufferDataSet && ds2 instanceof BufferDataSet ) {
            if ( !DataSetUtil.validate( ds1, null ) ) throw new RuntimeException("dataset input ds1 to append doesn't validate.");
            if ( !DataSetUtil.validate( ds2, null ) ) throw new RuntimeException("dataset input ds2 to append doesn't validate.");
            Object c1= ((BufferDataSet)ds1).getType();
            Object c2= ((BufferDataSet)ds2).getType();
            if ( c1==c2 ) {
                QDataSet result= BufferDataSet.append( (BufferDataSet)ds1, (BufferDataSet)ds2 );
                if ( !DataSetUtil.validate( result, null ) ) {
                    throw new RuntimeException("appended result didn't validate, contact support.");
                }
                return result;
            } else {
                Class c= double.class;
                QDataSet result= ArrayDataSet.append( ArrayDataSet.maybeCopy(c,ds1), ArrayDataSet.maybeCopy(c,ds2) );
                if ( !DataSetUtil.validate( result, null ) ) {
                    throw new RuntimeException("appended result didn't validate, contact support.");
                }
                return result;
            }
        } else {
            if ( !DataSetUtil.validate( ds1, null ) ) throw new RuntimeException("dataset input ds1 to append doesn't validate.");
            if ( !DataSetUtil.validate( ds2, null ) ) throw new RuntimeException("dataset input ds2 to append doesn't validate.");
            // use append to combine the two datasets.  Note append copies the data.
            Class c1= double.class;
            Class c2= double.class;
            if ( ds1 instanceof ArrayDataSet ) {
                c1= ((ArrayDataSet)ds1).getComponentType();
            }
            if ( ds2 instanceof ArrayDataSet ) {
                c2= ((ArrayDataSet)ds2).getComponentType();
            }
            Class c= double.class;
            if ( c1==c2 ) {
                c= c1;
            }
            
            QDataSet result= ArrayDataSet.append( ArrayDataSet.maybeCopy(c,ds1), ArrayDataSet.maybeCopy(c,ds2) );
            if ( !DataSetUtil.validate( result, null ) ) {
                throw new RuntimeException("appended result didn't validate, contact support.");
            }
            return result;
        }
    }
    
    /**
     * The first dataset's timetags are used to 
     * synchronize the single dataset to common timetags. Presently,
     * data from dsSource will be interpolated to create data at dsTarget timetag
     * locations, but other methods may be introduced soon.
     * Ordinal units use the nearest neighbor interpolation.    
     * @param dsTarget the dataset providing timetags, or the timetags themselves.
     * @param dsSource the dataset to synch up.  Its timetags must be monotonically increasing and non-repeating.
     * @return the one dataset, synchronized.
     * @see #synchronize(org.das2.qds.QDataSet, org.das2.qds.QDataSet...) 
     */
    public static QDataSet synchronizeOne( QDataSet dsTarget, QDataSet dsSource ) {
        QDataSet ttTarget= (QDataSet) dsTarget.property( QDataSet.DEPEND_0 );
        if ( ttTarget==null && DataSetUtil.isMonotonic(dsTarget) ) ttTarget= dsTarget;
    
        QDataSet ttSource= (QDataSet)dsSource.property( QDataSet.DEPEND_0 );
        
        if ( ttSource==null ) {
            if ( SemanticOps.getUnits(dsSource).isConvertibleTo( SemanticOps.getUnits(ttTarget) ) ) {
                ttSource= dsSource;
            } else {
                throw new IllegalArgumentException("dataset sent to synchronizeOne doesn't have timetags: "+dsSource );
            }
        }
        QDataSet ff;
        try {
            //Ops.extent(ttSource);
            //Ops.extent(ttTarget);
            ff= findex( ttSource, ttTarget ); // TODO: cache ff in case tt1 is the same for each dss.
        } catch ( IllegalArgumentException ex ) {  // data is not monotonic
            logger.log(Level.WARNING, 
                    "when calling synchronize, DEPEND_0 was not monotonic for dsSource, using monotonic subset of points");
            QDataSet dsx=Ops.monotonicSubset(dsSource);
            logger.log(Level.INFO, "monotonicSubset removes {0} records", (dsSource.length()-dsx.length()));
            dsSource= dsx;
            ttSource= (QDataSet)dsSource.property( QDataSet.DEPEND_0 );
            ff= findex( ttSource, ttTarget );
        }
        boolean nn= UnitsUtil.isOrdinalMeasurement( SemanticOps.getUnits(dsSource) );
        if ( nn ) ff= Ops.round(ff);        
        dsSource= interpolate( dsSource, ff );
//        QDataSet tlimit= DataSetUtil.guessCadenceNew( ttSource, null );
//        tlimit=null; // See sftp://jbf@nudnik.physics.uiowa.edu/home/jbf/project/rbsp/u/kris/20180808/demoBugFindexSynchronizeInline.jy
//        if ( tlimit!=null ) {
//            tlimit= Ops.multiply( tlimit, Ops.dataset(1.5) );
//            Number fillValue= (Number) dsSource.property(QDataSet.FILL_VALUE);
//            if ( fillValue==null ) fillValue= Double.NaN;
//            QDataSet iceil= Ops.lesserOf( Ops.ceil(ff), ttSource.length()-1 ); // TODO: rewrite this as stream to save memory.
//            QDataSet tcel= Ops.applyIndex(ttSource,iceil);
//            QDataSet iflr= Ops.greaterOf( Ops.floor(ff), 0 );
//            QDataSet tflr= Ops.applyIndex(ttSource,iflr);
//            QDataSet tdff= Ops.subtract( tcel,tflr );
//            QDataSet r= Ops.where( Ops.gt( tdff, tlimit ) );
//            dsSource= Ops.putValues( dsSource, r, fillValue );
//        }
        return dsSource;
    }
    
    /**
     * The first dataset's timetags are used to 
     * synchronize the second dataset to a set of common timetags. Presently,
     * only interpolation is used, but other methods may be introduced soon.
     * Note that when one of the dataset's DEPEND_0 is not monotonic, a 
     * monotonic subset of its points will be used.
     * Ordinal units use the nearest neighbor interpolation.
     * @param ds1 the dataset providing timetags, or the timetags themselves.
     * @param ds the single datasets to synch up.
     * @return the single dataset evaluated at the other dataset's timetags.
     * @see #synchronizeNN(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     * @see #synchronize(org.das2.qds.QDataSet, org.das2.qds.QDataSet...) 
     * @see #flattenWaveform(org.das2.qds.QDataSet) 
     */
    public static QDataSet synchronize( QDataSet ds1, QDataSet ds ) {
        List<QDataSet> dss= synchronize( ds1, new QDataSet[] { ds } );
        return dss.get(0);
    }
    
    /**
     * The first dataset's timetags are used to 
     * synchronize the list of datasets to a set of common timetags. Presently,
     * only interpolation is used, but other methods may be introduced soon.
     * Note that when one of the dataset's DEPEND_0 is not monotonic, a 
     * monotonic subset of its points will be used.
     * Ordinal units use the nearest neighbor interpolation.
     * Note in the case where dsTarget is the set of timeTags, then it must be monotonic. (TODO: why?)
     * @param dsTarget the dataset providing timetags, or the timetags themselves.  
     * @param dsSources the N datasets to synch up, where each should have monotonic timetags.
     * @return a list of N datasets, synchronized
     * @see #synchronizeNN(org.das2.qds.QDataSet, org.das2.qds.QDataSet...) 
     * @see #synchronize(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     * @see #flattenWaveform(org.das2.qds.QDataSet) 
     */
    public static List<QDataSet> synchronize( QDataSet dsTarget, QDataSet ... dsSources ) {
        QDataSet ttTarget= (QDataSet) dsTarget.property( QDataSet.DEPEND_0 );
        if ( ttTarget==null && DataSetUtil.isMonotonic(dsTarget) ) ttTarget= dsTarget;
        List<QDataSet> result= new ArrayList<>();
        int iarg=0;
        for ( QDataSet dsSource : dsSources ) {
            if ( dsSource==dsTarget ) {
                result.add( dsSource );
                continue;
            }
            QDataSet ttSource= (QDataSet)dsSource.property( QDataSet.DEPEND_0 );
            if ( ttSource==null ) { // there's a funny kludge we must do when timetags are sent.
                if ( SemanticOps.getUnits(dsSource).isConvertibleTo( SemanticOps.getUnits(ttTarget) ) ) {
                    result.add( ttTarget );
                    continue;
                } else {
                    throw new IllegalArgumentException("dataset (number "+(iarg+1) +" of "+dsSources.length + ") sent to synchronize doesn't have timetags: "+dsSource );
                }
            }
            if ( ttSource.length()!=dsSource.length() ) throw new IllegalArgumentException("malformed dataset (number "+(iarg+1) +" of "+dsSources.length + ") DEPEND_0 length not correct: "+ttSource + " " + dsSource );
            QDataSet ff;
            try {
                Ops.extent(ttSource);
                Ops.extent(ttTarget);
                ff= findex( ttSource, ttTarget ); // TODO: cache ff in case tt1 is the same for each dss.
            } catch ( IllegalArgumentException ex ) {  // data is not monotonic
                logger.log(Level.WARNING, "when calling synchronize, DEPEND_0 was not monotonic for dss argument #{0}, using monotonic subset of points", iarg);
                QDataSet dsx=Ops.monotonicSubset(dsSource);
                logger.log(Level.INFO, "monotonicSubset removes {0} records", (dsSource.length()-dsx.length()));
                dsSource= dsx;
                ttSource= (QDataSet)dsSource.property( QDataSet.DEPEND_0 );
                ff= findex( ttSource, ttTarget );
            }
            boolean nn= UnitsUtil.isOrdinalMeasurement( SemanticOps.getUnits(dsSource) ); // https://sourceforge.net/p/autoplot/feature-requests/593/
            if ( nn ) ff= Ops.round(ff);
            dsSource= interpolate( dsSource, ff );
            QDataSet tlimit= DataSetUtil.guessCadenceNew( ttSource, null );
            if ( tlimit!=null ) {
                tlimit= Ops.multiply( tlimit, Ops.dataset(1.5) );
                Number fillValue= (Number) dsSource.property(QDataSet.FILL_VALUE);
                if ( fillValue==null ) fillValue= Double.NaN;
                QDataSet iceil= Ops.lesserOf( Ops.ceil(ff), ttSource.length()-1 ); // TODO: rewrite this as stream to save memory.
                QDataSet tcel= Ops.applyIndex(ttSource,iceil);
                QDataSet iflr= Ops.greaterOf( Ops.floor(ff), 0 );
                QDataSet tflr= Ops.applyIndex(ttSource,iflr);
                QDataSet tdff= Ops.subtract( tcel,tflr );
                QDataSet r= Ops.where( Ops.gt( tdff, tlimit ) );
                dsSource= Ops.putValues( dsSource, r, fillValue );
            }
            result.add( dsSource );
            iarg++;
        }
        return result;        
    }
    
    /**
     * The first dataset's timetags are used to 
     * synchronize the second dataset to a set of common timetags, using
     * nearest neighbor interpolation. 
     * Note that when one of the dataset's DEPEND_0 is not monotonic, a 
     * monotonic subset of its points will be used.
     * Ordinal units use the nearest neighbor interpolation.
     * @param ds1 the dataset providing timetags, or the timetags themselves.
     * @param ds the single datasets to synch up.
     * @return the single dataset evaluated at the other dataset's timetags.
     * @see #synchronize(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     * @see #synchronizeNN(org.das2.qds.QDataSet, org.das2.qds.QDataSet...) 
     * @see #flattenWaveform(org.das2.qds.QDataSet) 
     */
    public static QDataSet synchronizeNN( QDataSet ds1, QDataSet ds ) {
        List<QDataSet> dss= synchronizeNN( ds1, new QDataSet[] { ds } );
        return dss.get(0);
    }
    
    /**
     * The first dataset's timetags are used to 
     * synchronize the list of datasets to a set of common timetags, using
     * nearest neighbor interpolation.
     * Note that when one of the dataset's DEPEND_0 is not monotonic, a 
     * monotonic subset of its points will be used.
    
     * @param ds1 the dataset providing timetags, or the timetags themselves.
     * @param dss the N datasets, each either rank 1 or rank 2, all should have DEPEND_0 which is monotonic.
     * @return a list of N datasets, synchronized
     * @see #synchronize(org.das2.qds.QDataSet, org.das2.qds.QDataSet...)      
     * @see #synchronizeNN(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     * @see #flattenWaveform(org.das2.qds.QDataSet) 
     */
    public static List<QDataSet> synchronizeNN( QDataSet ds1, QDataSet ... dss ) {
        QDataSet tt= (QDataSet) ds1.property( QDataSet.DEPEND_0 );
        if ( tt==null && DataSetUtil.isMonotonic(ds1) ) tt= ds1;
        
        List<QDataSet> result= new ArrayList<>();
    
        int iarg=0;
        for ( QDataSet ds : dss ) {
            if ( ds==ds1 ) {
                result.add( ds );
                continue;
            }
            QDataSet tt1= (QDataSet)ds.property( QDataSet.DEPEND_0 );
            QDataSet ff;
            try {
                ff= findex( tt1, tt );
            } catch ( IllegalArgumentException ex ) {  // data is not monotonic
                logger.log(Level.WARNING, "when calling synchronize, DEPEND_0 was not monotonic for dss argument #{0}, using monotonic subset of points", iarg);
                QDataSet dsx=Ops.monotonicSubset(ds);
                logger.log(Level.INFO, "monotonicSubset removes {0} records", (ds.length()-dsx.length()));
                ds= dsx;
                tt1= (QDataSet)ds.property( QDataSet.DEPEND_0 );
                ff= findex( tt1, tt );
            }
            QDataSet iff= Ops.round(ff);
            ds= interpolate( ds, iff );
            QDataSet tlimit= DataSetUtil.guessCadenceNew( tt1, null );
            if ( tlimit!=null ) {
                tlimit= Ops.multiply( tlimit, Ops.dataset(1.5) );
                Number fillValue= (Number) ds.property(QDataSet.FILL_VALUE);
                if ( fillValue==null ) fillValue= Double.NaN;
                QDataSet tcel= Ops.applyIndex(tt1,Ops.ceil(ff),fillValue);
                QDataSet tflr= Ops.applyIndex(tt1,Ops.floor(ff),fillValue);
                QDataSet tdff= Ops.subtract( tcel,tflr );
                QDataSet r= Ops.where( Ops.gt( tdff, tlimit ) );
                ds= Ops.putValues( ds, r, fillValue );
            }            
            result.add( ds );
            iarg++;
        }
        return result;        
    }  
            
    /**
     * convert the dataset to the target units
     * @param ds the original dataset.
     * @param u units of the new dataset
     * @return a new dataset with all the same properties but with the new units.
     */
    public static QDataSet convertUnitsTo( QDataSet ds, Units u ) {
        UnitsConverter uc= Units.getConverter( SemanticOps.getUnits(ds), u );
        if ( uc==UnitsConverter.IDENTITY ) {
            return ds;
        }
        
        ArrayDataSet ds2;
        Class c= ArrayDataSet.guessBackingStore( ds );
        if ( c==float.class || c==double.class ) {
            ds2= ArrayDataSet.copy(ds);
        } else {
            ds2= DDataSet.copy(ds);
        }
        
        for ( int i=0; i<ds.rank(); i++ ) {
            if ( ds2.property("BUNDLE_"+i) !=null ) {
                ds2.putProperty("BUNDLE_"+i,null);
            }
        }
        
        QDataSet wds= DataSetUtil.weightsDataSet(ds);
        DataSetIterator iter= new QubeDataSetIterator( ds2 );
        while ( iter.hasNext() ) {
            iter.next();
            if ( iter.getValue(wds)>0 ) {
                iter.putValue( ds2, uc.convert( iter.getValue(ds) ) );
            } else {
                iter.putValue( ds2, -1e38 );
            }
        }
        ds2.putProperty( QDataSet.UNITS, u );
        ds2.putProperty( QDataSet.FILL_VALUE, -1e38 );
        ds2.putProperty( QDataSet.VALID_MIN, null );
        ds2.putProperty( QDataSet.VALID_MAX, null );
        
        return ds2;
    }

    /**
     * convert the datumRange to the given units, which must be convertible.
     * @param dr the datum range, e.g. '5 to 50 MHz'
     * @param u the new units. e.g. 'Hz'
     * @return DatumRange in the new units, e.g. '5000000 to 50000000 Hz'
     * @throws InconvertibleUnitsException
     */
    public static DatumRange convertUnitsTo( DatumRange dr, Units u ) {
        return dr.convertTo(u);
    }
    
    /**
     * convert the datum to the given units, which must be convertible.
     * @param d the datum, e.g. '5 MHz'
     * @param u the new units, e.g. 'Hz'
     * @return Datum in the new units, e.g. '5000000 Hz'
     * @throws InconvertibleUnitsException
     */
    public static Datum convertUnitsTo( Datum d, Units u ) {
        return d.convertTo(u);
    }
    
    /**
     * flatten a rank N dataset, though currently rank 4 is not supported.
     * The result for rank 2 is an n,3 dataset of [x,y,z], or if there are no tags, just [z].
     * The last index will be the dependent variable, and the first indices will
     * be the independent variables sorted by dimension.
     * @see org.das2.qds.DataSetOps#flattenRank2(org.das2.qds.QDataSet) 
     * @see #grid(org.das2.qds.QDataSet) 
     * @see #flattenWaveform(org.das2.qds.QDataSet) 
     * @param ds the rank N dataset (note only Rank 2 is supported for now).
     * @return rank 2 dataset bundle
     */
    public static QDataSet flatten( QDataSet ds ) {
        switch (ds.rank()) {
            case 0: {
                DDataSet result= DDataSet.createRank2(1,1);
                result.putValue(0,0,ds.value());
                DataSetUtil.copyDimensionProperties( ds, result );
                return result;
            }
            case 1:
                QDataSet dep0= (QDataSet) ds.property( QDataSet.DEPEND_0 );
                QDataSet plane0= (QDataSet) ds.property( QDataSet.PLANE_0 );
                if ( dep0!=null ) {
                    if ( plane0==null ) {
                        return bundle(dep0,ds); 
                    } else {
                        return bundle(dep0,ds,plane0);
                    }
                } else {
                    return bundle(ds);
                }
            case 2:
                QDataSet result= DataSetOps.flattenRank2(ds);                
                if ( result.rank()==1 ) {
                    return reform( ds, new int [] { result.length(), 1 } );
                } else {
                    return result;
                }
            case 3:
                return DataSetOps.flattenRank3(ds);
            default:
                throw new UnsupportedOperationException("only rank 0,1,and 2 supported");
        }
    }
    
    /**
     * flatten a rank 2 dataset where the y depend variable is just an offset from the xtag. 
     * Note the new DEPEND_0 may have different units from ds.property(DEPEND_0).
     * @param ds rank 2 waveform with tags for DEPEND_0 and offsets for DEPEND_1
     * @return rank 1 waveform
     * @see #flatten(org.das2.qds.QDataSet) 
     * @see DataSetOps#flattenWaveform(org.das2.qds.QDataSet) 
     * @see #synchronizeNN(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet flattenWaveform( QDataSet ds ) {
        return DataSetOps.flattenWaveform(ds);
    }
    
    /**
     * Opposite of the flatten function, takes rank 2 bundle (x,y,z) and 
     * makes a table from it z(x,y). This presumes that the rank 1 X and
     * Y data contain repeating elements for the rows and columns of the grid.
     * @param ds rank 2 bundle of X,Y, and Z data.
     * @return rank 2 table.
     * @see #flatten(org.das2.qds.QDataSet) 
     */
    public static QDataSet grid( QDataSet ds ) {
        return DataSetOps.grid(ds);
    }
    
    /**
     * This finds sweeps of Y and interpolates T->Y->Z to make a regular 
     * spectrogram T,yTags->Z[T,yTags] 
     * This function was once known as "LvT" because it was used to create a spectrogram
     * of Flux(Time,Lshell) by interpolating along sweeps.
     * @param t the rank 1 x values (often time)
     * @param y the rank 1 y values (for example, L)
     * @param z the rank 1 z values at each y.
     * @param ytags the rank 1 y tags for the result.
     * @return the rank 2 spectrogram.
     */
    public static QDataSet gridIrregularY( QDataSet t, QDataSet y, QDataSet z, QDataSet ytags ) {
        QDataSet result= LSpec.rebin( link( t, y ), z, ytags, 0 );
        return result;
    }

    /**
     * create a labels dataset for tagging rows of a dataset.  If the context
     * has been used already, including "default", then the EnumerationUnit
     * for the data will be preserved.  labels(["red","green","blue"],"default")
     * will always return an equivalent (and comparable) result during a session.
     *
     * Example:
     * <tt>dep1= labels( ["X","Y","Z"], "GSM" )</tt>
     * @param labels array of string labels
     * @param context the namespace for the labels, to provide control over String&rarr;int mapping.
     * @return rank 1 QDataSet
     * @deprecated use labelsDataset
     * @see #labelsDataset(java.lang.String[]) 
     */
    public static QDataSet labels(String[] labels, String context) {
        return labelsDataset(labels,context);
    }

    /**
     * create a labels dataset for tagging rows of a dataset.
     * Example:
     * <tt>dep1= labels( ["red","green","blue"] )</tt>
     * @param labels array of string labels
     * @return rank 1 QDataSet
     * @deprecated use labelsDataSet
     * @see #labelsDataset(java.lang.String[]) 
     */
    public static QDataSet labels(String[] labels) {
        return labelsDataset(labels, "default");
    }
    
    /**
     * create a labels dataset for tagging rows of a dataset.  If the context
     * has been used already, including "default", then the EnumerationUnit
     * for the data will be preserved.  labelsDataset(['red','green','blue'],'default')
     * will always return an equivalent (and comparable) result during a session.
     *
     * Example:
     * <tt>dep1= labelsDataset( ['X','Y','Z'], 'GSM' )</tt>
     * @param labels array of string labels
     * @param context the namespace for the labels, to provide control over String&rarr;int mapping.
     * @return rank 1 QDataSet
     */
    public static QDataSet labelsDataset(String[] labels, String context) {
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
     * Example: array of string labels
     * <tt>dep1= labelsDataset( ['red','green','blue'] )</tt>
     * @param labels
     * @return rank 1 QDataSet
     */    
    public static QDataSet labelsDataset(String[] labels) {
        return labelsDataset( labels, "default" );
    }

    /**
     * create a rank 0 label dataset.
     * <tt>dep1= labelsDataset( 'Chicago' )</tt>
     * @param label the string label
     * @return rank 0 QDataSet
     */    
    public static QDataSet labelsDataset(String label) {
        return labelsDataset( label, "default" );
    }    

    /**
     * create a rank 0 label dataset.
     * <tt>dep1= labelsDataset( 'Chicago', 'cities' )</tt>
     * @param label the string label
     * @param context the namespace for the labels, to provide control over String&rarr;int mapping.
     * @return rank 0 QDataSet
     */      
    public static QDataSet labelsDataset(String label, String context) {
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
        IDataSet result = IDataSet.createRank0();
        Datum d = u.createDatum(label);
        result.putValue(d.doubleValue(u));
        result.putProperty(QDataSet.UNITS, u);
        return result;
    }
    
    /**
     * create a dataset of RGB colors.  The output is
     * int(red)*256*256 + int(green)*256 + int(blue)
     * with the units of Units.rgbColor
     * @param red the red component, from 0 to 255
     * @param green the green component, from 0 to 255
     * @param blue the blue component, from 0 to 255
     * @return the rgb encoded colors.
     */
    public static QDataSet rgbColorDataset( QDataSet red, QDataSet green, QDataSet blue ) {
        QDataSet[] operands= new QDataSet[2];
        CoerceUtil.coerce( red, green, false, operands );
        red= operands[0];
        green= operands[1];
        CoerceUtil.coerce( green, blue, false, operands );
        blue= operands[1];

        int[] qube= DataSetUtil.qubeDims(blue);
        IDataSet z= IDataSet.create(qube);
        
        QubeDataSetIterator iter= new QubeDataSetIterator(z);
        while ( iter.hasNext() ) {
            iter.next();
            int r1= (int) iter.getValue(red);
            int g1= (int) iter.getValue(green);
            int b1= (int) iter.getValue(blue);
            iter.putValue( z, r1*256*256 + g1 * 256 + b1 );
        }
        z.putProperty( QDataSet.UNITS, Units.rgbColor );
        return z;
    }

    /**
     * returns the number of elements in each index.  E.g:
     *<blockquote><pre>
     * ds= zeros(3,4)
     * print size(ds) # returns "3,4"
     * </pre></blockquote>
     * Note datasets need not have the same number of elements in each record.
     * This is often the case however, and a "qube" dataset has this property.
     * @param ds a qube dataset.
     * @return the array containing number of elements in each index.
     * @throws IllegalArgumentException if the dataset is not a qube.
     */
    public static int[] size( QDataSet ds ) {
        if ( ds.rank()>1 && ds.length()>1 ) {
            if ( ds.length(0)!=ds.length(ds.length()-1) ) {
                throw new IllegalArgumentException("dataset is not a qube, so the length of each record differs.");
            }
        }
        return DataSetUtil.qubeDims(ds);
    }

    /**
     * return the color encoded as one of:<ul>
     * <li>"red" or "RED" or X11 color names like "LightPink" 
     * <li>#FF0000
     * <li>255,0,0 or 1.0,0,0
     * </ul>
     *
     * @param sval the string representation
     * @return the color
     * @throws IllegalArgumentException if the color cannot be parsed.
     */
    public static Color colorFromString(String sval) {
        Color c;
        try {
            if (sval.contains(",")) {
                String[] ss = sval.split(",", -2);
                if (ss.length == 3) {
                    if (sval.contains(".")) {
                        float rr = Float.parseFloat(ss[0].trim());
                        float gg = Float.parseFloat(ss[1].trim());
                        float bb = Float.parseFloat(ss[2].trim());
                        c = new Color(rr, gg, bb, 1.0f);
                    } else {
                        int rr = Integer.parseInt(ss[0].trim());
                        int gg = Integer.parseInt(ss[1].trim());
                        int bb = Integer.parseInt(ss[2].trim());
                        c = new Color(rr, gg, bb, 255);
                    }

                } else {
                    throw new IllegalArgumentException("color identified in string should be name like 'red' or r,g,b triple like '255,0,0'");
                }
            } else {
                c= ColorUtil.decodeColor(sval);
            }
        } catch (NumberFormatException ex) {
            c = (Color) ClassMap.getEnumElement(Color.class, sval);
            if ( c==null ) {
                throw new IllegalArgumentException("color identified in string should be name like 'red' or r,g,b triple like '255,0,0'");
            }
        }
        return c;
    }

    /**
     * TODO: I suspect this is not up to spec.  See DataSetOps.sliceProperties
     * See reform, the only function that uses this.
     * @param removeDim
     * @param ds
     * @param result
     */
    private static void sliceProperties(int removeDim, QDataSet ds, MutablePropertyDataSet result) {
        size(ds);
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
     * @param ds the dataset
     * @param args varargs list of integers that are slice indices, or "" or ":" to mean don't slice
     * @return the dataset with slices performed.
     */
    public static QDataSet slices( QDataSet ds, Object ... args ) {
        int cdim=0; // to keep track of if we can use native slice
        int sdim=0; // to keep track of number of slices offset.
        QDataSet result= ds;
        StringBuilder docStr= new StringBuilder();
        for ( int i=0; i<args.length; i++ ) {
            if ( args[i] instanceof Integer ) {
                int sliceIdx= ((Integer)args[i]);
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
                docStr.append(sliceIdx);
                sdim++;
            } else {
                if ( args[i] instanceof String ) {
                    String s= (String)args[i];
                    if ( s.contains("=") ) {
                        throw new IllegalArgumentException("argument not supported in this version: "+s );
                    }
                    docStr.append(s);
                }
            }
            if ( i<args.length-1 ) docStr.append(",");
        }

        logger.log(Level.FINER, "slices({0})", docStr.toString());
        //((MutablePropertyDataSet)result).putProperty( QDataSet.CONTEXT_0, "slices("+docStr+")");
        //((MutablePropertyDataSet)result).putProperty( "CONTEXT_1", null ); // not sure who is writing this.

        return result;
    }

    /**
     * Reshape the dataset to remove the first dimension with length 1, reducing
     * its rank by 1.  Dependencies are also preserved.  If no indices are found, then the dataset is returned.
     * 
     * @param ds rank N dataset
     * @return the dataset, or rank N-1 dataset with the first 1-element dimension removed.
     */
    public static QDataSet reform(QDataSet ds) {
        int[] dsqube = DataSetUtil.qubeDims(ds);
        List<Integer> newQube = new ArrayList<>();
        //int[] dimMap = new int[dsqube.length]; // maps from new dataset to old index
        boolean foundDim = false;
        int removeDim = -1;
        for (int i = 0; i < dsqube.length; i++) {
            if (dsqube[i] != 1 || foundDim) {
                newQube.add(dsqube[i]);
                //dimMap[i] = foundDim ? i + 1 : i;
            } else {
                foundDim = true;
                removeDim = i;
            }
        }
        if (foundDim == false) {
            return ds;
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
     * allow reform record-by-record, which comes up often.
     * @param ds rank 2 or greater dataset of length nrec.
     * @param nrec number of records in ds
     * @param qube length nn array with the new geometry of each record.
     * @return ds of rank nn+1.
     */
    public static QDataSet reform(QDataSet ds, int nrec, int[] qube) {
        if ( nrec!=ds.length() ) throw new IllegalArgumentException("rec should be equal to the number of records in ds");
        int[] newArray= new int[qube.length+1];
        newArray[0]= nrec;
        System.arraycopy( qube, 0, newArray, 1, qube.length );
        return reform( ds, newArray );
    }
    
    /**
     * change the dimensionality of the elements of the QUBE dataset.  For example,
     * convert [1,2,3,4,5,6] to [[1,2],[3,4],[5,6]].
     * @param ds dataset
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
     * return the xtags of the dataset. 
     * @param ds the dataset
     * @return the dataset for the xtags.
     * @see SemanticOps#xtagsDataSet(org.das2.qds.QDataSet) 
     */
    public static QDataSet xtags( QDataSet ds ) {
        return SemanticOps.xtagsDataSet(ds);
    }

    /**
     * return the ytags of the dataset. 
     * @param ds the dataset
     * @return the dataset for the xtags.
     * @see SemanticOps#ytagsDataSet(org.das2.qds.QDataSet) 
     */
    public static QDataSet ytags( QDataSet ds ) {
        return SemanticOps.ytagsDataSet(ds);
    }
    
    /**
     * bundle the dataset, making an initial bundle, adding a bundle dimension.  
     * @param ds a rank N dataset
     * @return rank N+1 bundle dataset
     */
    public static QDataSet bundle( QDataSet ds ) {
        return bundle( null, ds );
    }
    
    /**
     * bundle the two datasets, adding if necessary a bundle dimension.  This
     * will try to bundle on the second dimension, unlike join.  This will also
     * isolate the semantics of bundle dimensions as it's introduced.  Note the
     * first argument can be null in order to simplify loops in client code.
     * @param ds1 null, rank N dataset with n records or rank N+1 bundle dataset
     * @param ds2 rank N dataset.
     * @return rank N+1 bundle dataset, presently with mutable properties.
     * @see #join(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     * @see DataSetOps#unbundle(org.das2.qds.QDataSet, java.lang.String) 
     */
    public static QDataSet bundle( QDataSet ds1, QDataSet ds2 ) {
        if ( ds1==null && ds2==null ) {
            throw new NullPointerException("both ds1 and ds2 are null");
        } else if ( ds1==null ) {
            BundleDataSet ds;
            if ( ds2.rank()==0 ) {
                ds= BundleDataSet.createRank0Bundle();
                ds.bundle(ds2);
//            } else if ( ds2.rank()==2 && Ops.isBundle(ds2) ) {
//                ds= new BundleDataSet( ds2.rank() );
//                for ( int i=0; i<ds2.length(0); i++ ) {
//                    ds.bundle(Ops.unbundle(ds2,i));
//                }
            } else {
                ds = new BundleDataSet( ds2.rank()+1 );
                ds.bundle(ds2);
            }
            QDataSet dep0= (QDataSet) ds2.property(QDataSet.DEPEND_0);
            if ( dep0!=null && ds.rank()<3 ) ds.putProperty( QDataSet.DEPEND_0, dep0 );
            return ds;
        } else if ( ds2==null ) {
            throw new NullPointerException("ds2 is null while ds1 ("+ds1+") is not null.");
        } else if ( ds1.rank() == ds2.rank() ) { // findbugs fails to realize that this must be okay.
            if ( ds1.rank()>1 ) {
                TailBundleDataSet ds= new TailBundleDataSet( ds1.rank() + 1 );
                ds.bundle(ds1);
                ds.bundle(ds2);
                for ( int k=0; k<ds1.rank(); k++ ) {
                    if ( ds1.rank()>k ) {
                        String depName= "DEPEND_"+k;
                        QDataSet d1= (QDataSet)ds1.property(depName);
                        QDataSet d2= (QDataSet)ds2.property(depName);
                        if ( d1!=null && d2!=null && Ops.equivalent( d1, d2 ) ) {
                            ds.putProperty( depName, d1 );
                        }
                    }
                }
                return ds;
            } else {
                BundleDataSet ds= new BundleDataSet( ds1.rank() + 1 );
                ds.bundle(ds1);
                ds.bundle(ds2);                
                return ds;
            }
        } else if ( ds1 instanceof BundleDataSet && ds1.rank()-1==ds2.rank() ) {
            ((BundleDataSet)ds1).bundle(ds2);
            return ds1;
        } else if ( ds1 instanceof TailBundleDataSet && ds1.rank()-1==ds2.rank() ) {
            ((TailBundleDataSet)ds1).bundle(ds2);
            return ds1;
        } else if ( ds1.rank()-1==ds2.rank() ) {
            switch (ds1.rank()) {
                case 4:
                {
                    TailBundleDataSet bds= new TailBundleDataSet(ds1.rank());
                    for ( int i=0; i<ds1.length(0,0,0); i++ ) {
                        bds.bundle( DataSetOps.unbundle(ds1,i) );
                    }
                    bds.bundle( ds2 );
                    return bds;
                }
                case 3:
                {
                    TailBundleDataSet bds= new TailBundleDataSet(ds1.rank());
                    for ( int i=0; i<ds1.length(0,0); i++ ) {
                        bds.bundle( DataSetOps.unbundle(ds1,i) );
                    }
                    bds.bundle( ds2 );
                    return bds;
                }
                case 2:
                {
                    BundleDataSet bds= new BundleDataSet(ds1.rank());
                    for ( int i=0; i<ds1.length(0); i++ ) {
                        bds.bundle( DataSetOps.unbundle(ds1,i) );
                    }
                    bds.bundle( ds2 );
                    return bds;
                }
                case 1: // new
                {
                    BundleDataSet bds= new BundleDataSet(ds1.rank());
                    for ( int i=0; i<ds1.length(); i++ ) {
                        bds.bundle( DataSetOps.unbundle(ds1,i) );
                    }
                    bds.bundle( ds2 );
                    return bds;
                }
                // note there was a ds1.rank()==3 which was redundant and impossible to reach
                default:
                {
                    throw new IllegalArgumentException("ds1 rank must be 1,2,3, or 4");
                }
            }
        } else {
            throw new IllegalArgumentException("not supported yet: ds1 rank must be equal to or one more than ds2 rank");
        }

    }

    /**
     * bundle three datasets, giving them a common zeroth index, typically time.
     * unlike join.  This bundles on the second dimension,
     * unlike join.  This is just like bundle(ds1,ds2), in fact this just calls 
     * bundle( bundle( ds1,ds2 ), ds3 )
     * @param ds1 rank 1 (for now) dataset with n records or rank 2 bundle dataset
     * @param ds2 rank 1 (for now) dataset with n records
     * @param ds3 rank 1 (for now) dataset with n records
     * @return rank 2 [n,3] bundle dataset
     * @see #join(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet bundle( QDataSet ds1, QDataSet ds2, QDataSet ds3 ) {
        return bundle( bundle( ds1, ds2 ), ds3 );
    }


    /**
     * bundle four datasets, making them share their zeroth index, typically time,
     * unlike join.  This is just like bundle(ds1,ds2), in fact this just calls
     * bundle( bundle( bundle( ds1,ds2 ), ds3 ), ds4 )
     * @param ds1 rank 1 (for now) dataset with n records or rank 2 bundle dataset
     * @param ds2 rank 1 (for now) dataset with n records
     * @param ds3 rank 1 (for now) dataset with n records
     * @param ds4 rank 1 (for now) dataset with n records
     * @return rank 2 [n,4] bundle dataset
     * @see #join(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet bundle( QDataSet ds1, QDataSet ds2, QDataSet ds3, QDataSet ds4 ) {
        return bundle( bundle( bundle( ds1, ds2 ), ds3 ), ds4 );
    }

    /**
     * bundle five datasets, making them share their zeroth index, typically time,
     * unlike join.  This is just like bundle(ds1,ds2), in fact this just calls
     * bundle( bundle( bundle( ds1,ds2 ), ds3 ), ds4 )
     * @param ds1 rank 1 (for now) dataset with n records or rank 2 bundle dataset
     * @param ds2 rank 1 (for now) dataset with n records
     * @param ds3 rank 1 (for now) dataset with n records
     * @param ds4 rank 1 (for now) dataset with n records
     * @param ds5 rank 1 (for now) dataset with n records
     * @return rank 2 [n,4] bundle dataset
     * @see #join(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet bundle( QDataSet ds1, QDataSet ds2, QDataSet ds3, 
            QDataSet ds4, QDataSet ds5 ) {
        return bundle( bundle( bundle( bundle( ds1, ds2 ), ds3 ), ds4 ), ds5 );
    }
    
    /**
     * bundle six datasets, making them share their zeroth index, typically time,
     * unlike join.  This is just like bundle(ds1,ds2), in fact this just calls
     * bundle( bundle( bundle( ds1,ds2 ), ds3 ), ds4 )
     * @param ds1 rank 1 (for now) dataset with n records or rank 2 bundle dataset
     * @param ds2 rank 1 (for now) dataset with n records
     * @param ds3 rank 1 (for now) dataset with n records
     * @param ds4 rank 1 (for now) dataset with n records
     * @param ds5 rank 1 (for now) dataset with n records
     * @param ds6 rank 1 (for now) dataset with n records
     * @return rank 2 [n,4] bundle dataset
     * @see #join(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet bundle( QDataSet ds1, QDataSet ds2, QDataSet ds3, 
            QDataSet ds4, QDataSet ds5, QDataSet ds6 ) {
        return bundle( bundle( bundle( bundle( bundle( ds1, ds2 ), ds3 )
                , ds4 ), ds5 ), ds6 );
    }

    /**
     * bundle seven datasets, making them share their zeroth index, typically time,
     * unlike join.  This is just like bundle(ds1,ds2), in fact this just calls
     * bundle( bundle( bundle( ds1,ds2 ), ds3 ), ds4 )
     * @param ds1 rank 1 (for now) dataset with n records or rank 2 bundle dataset
     * @param ds2 rank 1 (for now) dataset with n records
     * @param ds3 rank 1 (for now) dataset with n records
     * @param ds4 rank 1 (for now) dataset with n records
     * @param ds5 rank 1 (for now) dataset with n records
     * @param ds6 rank 1 (for now) dataset with n records
     * @param ds7 rank 1 (for now) dataset with n records
     * @return rank 2 [n,4] bundle dataset
     * @see #join(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet bundle( QDataSet ds1, QDataSet ds2, QDataSet ds3, 
            QDataSet ds4, QDataSet ds5, QDataSet ds6, QDataSet ds7 ) {
        return bundle( bundle( bundle( bundle( bundle( bundle( ds1, ds2 ), ds3 )
                , ds4 ), ds5 ), ds6 ), ds7 );
    }
    
    /**
     * Extract the named bundled dataset.  For example, extract B_x from bundle of components.
     * @param ds the bundle of datasets, often rank 2 with BUNDLE_1 property
     * @param name the name of the bundled dataset, or "ch_&lt;i&gt;" where i is the dataset number
     * @see #unbundle(org.das2.qds.QDataSet, int) 
     * @see DataSetOps#bundleNames(org.das2.qds.QDataSet) 
     * @throws IllegalArgumentException if no named dataset is found.
     * @return the named dataset
     */
    public static QDataSet unbundle( QDataSet ds, String name ) {
        return DataSetOps.unbundle(ds, name);
    }
    
    /**
     * Extract a bundled dataset from a bundle of datasets.  The input should
     * be a rank 2 dataset with the property BUNDLE_1 set to a bundle descriptor
     * dataset.  See BundleDataSet for more semantics.  Note we support the case
     * where DEPEND_1 has EnumerationUnits, and this is the same as slice1.
     *
     * @param ds the bundle dataset.
     * @param i index of the dataset to extract. If the index is within a high-rank dataset, then the entire dataset is returned.
     * @throws IndexOutOfBoundsException if the index is invalid.
     * @throws IllegalArgumentException if the dataset is not a bundle dataset, with either BUNDLE_1 or DEPEND_1 set.
     * @return the i-th dataset from the bundle.
     */
    public static QDataSet unbundle( QDataSet ds, int i ) {
        return DataSetOps.unbundle(ds, i);
    }
    
    /**
     * convenient method for getting the times from an events dataset, this
     * unbundles the startTimes at i and the stopTimes at i+1 to a bins dataset.
     * The second column can be durations as well.
     * @param ds the bundle.
     * @param i the index, 0 for a canonical events dataset.
     * @return rank 2 bins dataset.
     */
    public static QDataSet unbundleBins( QDataSet ds, int i ) {
        switch (ds.rank()) {
            case 2:
            {
                MutablePropertyDataSet result= DataSetOps.leafTrim( ds, i, i+2 );
                QDataSet bds= (QDataSet) result.property(QDataSet.BUNDLE_1);
                if ( bds!=null ) {
                    Units u1= (Units) bds.property(QDataSet.UNITS,0);
                    Units u2= (Units) bds.property(QDataSet.UNITS,1);
                    if ( u1==u2 ) {
                        result= copy(result);
                        result.putProperty( QDataSet.BUNDLE_1, null );
                        result.putProperty( QDataSet.BINS_1, QDataSet.VALUE_BINS_MIN_MAX );
                        result.putProperty( QDataSet.UNITS, u1 );
                    } else if ( u2.isConvertibleTo(u1.getOffsetUnits() ) ) {
                        result= copy( result );
                        UnitsConverter uc= u2.getConverter( u1.getOffsetUnits() );
                        for ( int i1=0; i1<result.length(); i1++ ) { 
                            ((WritableDataSet)result).putValue( i1, 1,
                                    result.value(i1,0) + uc.convert(result.value(i1,1)));
                        }
                        result.putProperty( QDataSet.BUNDLE_1, null );
                        result.putProperty( QDataSet.BINS_1, QDataSet.VALUE_BINS_MIN_MAX );
                        result.putProperty( QDataSet.UNITS, u1 );
                    }
                }
                return result;
            }
            case 1:
            {
                MutablePropertyDataSet result= DataSetOps.leafTrim( ds, i, i+2 );
                QDataSet bds= (QDataSet) result.property(QDataSet.BUNDLE_0);
                if ( bds!=null ) {
                    Units u1= (Units) bds.property(QDataSet.UNITS,0);
                    Units u2= (Units) bds.property(QDataSet.UNITS,1);
                    if ( u1==u2 ) {
                        result= copy(result);
                        result.putProperty( QDataSet.BUNDLE_0, null );
                        result.putProperty( QDataSet.BINS_0, QDataSet.VALUE_BINS_MIN_MAX );
                        result.putProperty( QDataSet.UNITS, u1 );
                    } else if ( u2.isConvertibleTo(u1.getOffsetUnits() ) ) {
                        result= copy( result );
                        UnitsConverter uc= u2.getConverter( u1.getOffsetUnits() );
                        for ( int i1=0; i1<result.length(); i1++ ) {
                            ((WritableDataSet)result).putValue( i1, 1,
                                    result.value(i1,0) + uc.convert(result.value(i1,1))); 
                        }
                        result.putProperty( QDataSet.BUNDLE_0, null );
                        result.putProperty( QDataSet.BINS_0, QDataSet.VALUE_BINS_MIN_MAX );
                        result.putProperty( QDataSet.UNITS, u1 );
                    }
                }
                return result;
            }
            default:
                throw new IllegalArgumentException("rank exception, must be rank 1 or rank 2");
        }
    }
    
    /**
     * unbundle the names from the bundle, and rebundle them in the order 
     * specified.
     * @param bundle1 a bundle of datasets
     * @param names the bundled dataset names
     * @return the new bundle
     * @see #unbundle(org.das2.qds.QDataSet, java.lang.String) 
     * @since v2020a_15
     */
    public static QDataSet rebundle( QDataSet bundle1, String[] names ) {
        QDataSet result= null;
        for ( String s: names ) {
            QDataSet b= Ops.unbundle( bundle1, s );
            result= Ops.bundle( result, b );
        }
        return result;
    }
    
    /**
     * unbundle datasets by index from the bundle, and rebundle them in the order 
     * specified.
     * @param bundle1 a bundle of datasets
     * @param ii the index of each dataset
     * @return the new bundle
     * @see #unbundle(org.das2.qds.QDataSet, java.lang.String) 
     * @since v2020a_15
     */
    public static QDataSet rebundle( QDataSet bundle1, int[] ii ) {
        QDataSet result= null;
        for ( int i: ii ) {
            QDataSet b= Ops.unbundle( bundle1, i );
            result= Ops.bundle( result, b );
        }
        return result;
    }
    
    /**
     * return true if DEPEND_1 is set and its units are EnumerationUnits.  This
     * was the pre-bundle way of representing a bundle of datasets.  It might
     * be supported indefinitely, because it has some nice rules about the data.
     * For example, bundled data must be of the same units since there is no place to put
     * the property, and each bundled item must be rank 1.
     * @param zds rank 1 or rank 2 dataset
     * @return return true if DEPEND_1 is set and its units are EnumerationUnits.
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
                logger.info("Somehow a string got into DEPEND_0 property.");
                return false;
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
     * @param zds the dataset
     * @return true if the dataset is a bundle.
     * @see org.das2.qds.examples.Schemes
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
     * possibly sort the data where the DEPEND_0 tags are
     * monotonically increasing.  If the data is already monotonic,
     * then nothing is done to the data.
     * @param ds the dataset
     * @return the dataset, sorted if necessary.
     * @see DataSetUtil#isMonotonic
     */
    public static QDataSet ensureMonotonic( QDataSet ds ) {
        if ( ds.length()==0 ) return ds;
        if ( SemanticOps.isJoin(ds) ) {
            QDataSet ds1= ds.slice(0);
            QDataSet dep0= (QDataSet)ds1.property(QDataSet.DEPEND_0);
            if (Boolean.TRUE.equals(dep0.property(QDataSet.MONOTONIC))) {
                // avoid showing message when data is in fact monotonic.
                return ds;
            } else {
                logger.warning("ensure monotonic does not support joins.");
                return ds;
            }
        }
        QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
        if ( dep0==null ) {
            return ds;
        }

        logger.entering( "org.das2.qds.Ops","ensureMonotonic");
        if ( DataSetUtil.isMonotonic(dep0) ) {
            return ds;
        }
        
        QDataSet sort= Ops.sort(dep0);
        if ( ds instanceof WritableDataSet ) {
            if ( ((WritableDataSet)ds).isImmutable() ) {
                ds= Ops.copy(ds);
            }
            DataSetOps.applyIndexInSitu( ((WritableDataSet)ds), sort );
        } else {
            ds= Ops.copy(ds);
            DataSetOps.applyIndexInSitu( ((WritableDataSet)ds), sort );
        }
        
        dep0= (QDataSet)ds.property(QDataSet.DEPEND_0);
        ((WritableDataSet)dep0).putProperty( QDataSet.MONOTONIC, Boolean.TRUE );
        logger.exiting( "org.das2.qds.Ops","ensureMonotonic" );        
        
        return ds;
        
    }
    
    /**
     * Return data where the DEPEND_0 tags are 
     * monotonically increasing and non repeating. Instead of sorting the data, simply replace repeat records with
     * a fill record.
     * @param ds the dataset
     * @return the dataset, sorted if necessary.
     * TODO: It's surprising that monotonic doesn't imply non-repeating, and this really needs to be revisited.
     * @see DataSetUtil#isMonotonicAndIncreasingQuick
     */
    public static QDataSet ensureMonotonicAndIncreasingWithFill( QDataSet ds ) {
        if ( ds.length()==0 ) return ds;
        if ( SemanticOps.isJoin(ds) ) {
            QDataSet ds1= ds.slice(0);
            QDataSet dep0= (QDataSet)ds1.property(QDataSet.DEPEND_0);
            if (Boolean.TRUE.equals(dep0.property(QDataSet.MONOTONIC))) {
                // avoid showing message when data is in fact monotonic.
                return ds;
            } else {
                logger.warning("ensure monotonic does not support joins.");
                return ds;
            }
        }
        QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
        if ( dep0==null ) {
            return ds;
        }

        logger.entering( "org.das2.qds.Ops","ensureMonotonicWithFill");
        if ( DataSetUtil.isMonotonicAndIncreasing(dep0) ) {
            return ds;
        }
        
        QDataSet wds= DataSetUtil.weightsDataSet(dep0);
        int i;

        for ( i=0; i<dep0.length() && wds.value(i)==0; i++ ) {
            // find first valid point.
        }

        if ( i==ds.length() ) {
            return ds;
        }

        WritableDataSet mdep0= Ops.copy( dep0 );
        double fill= ((Number)(wds.property( WeightsDataSet.PROP_SUGGEST_FILL ))).doubleValue();
                        
        double last = dep0.value(i);

        for ( i = i+1; i < dep0.length(); i++) {
            double d = dep0.value(i);
            double w = wds.value(i);
            if ( w==0 ) continue;
            if ( d <= last  ) {
                mdep0.putValue(i,fill);
            } else {
                last = d; 
            }
        }
        
        logger.exiting( "org.das2.qds.Ops","ensureMonotonicWithFill" );        
        
        MutablePropertyDataSet mpds= DataSetOps.makePropertiesMutable(ds);
        mpds.putProperty( QDataSet.DEPEND_0, mdep0 );
                
        return mpds;
        
    }
    
    /**
     * link is the fundamental operator where we declare that one
     * dataset is dependent on another.  For example link(x,y) creates
     * a new dataset where y is the dependent variable of the independent
     * variable x.  link is like the plot command, but doesn't plot.  For example
     *<blockquote><pre>
     *plot(X,Y) shows a plot of Y(X),
     *link(X,Y) returns the dataset Y(X).
     *</pre></blockquote>
     * @param x rank 1 dataset
     * @param y rank 1 or rank 2 bundle dataset
     * @return rank 1 dataset with DEPEND_0 set to x.
     */
    public static QDataSet link( QDataSet x, QDataSet y ) {
        if (y.rank() == 1) {
            ArrayDataSet yds= ArrayDataSet.copy(y);
            yds.putProperty(QDataSet.DEPEND_0, x);
            List<String> problems= new ArrayList<>();
            if ( !DataSetUtil.validate(yds, problems ) ) {
                throw new IllegalArgumentException( problems.get(0) );
            } else {
                return yds;
            }
        } else {
            ArrayDataSet zds = ArrayDataSet.copy(y);
            if (x != null) {
                if ( zds.rank()==0 ) {
                    zds.putProperty(QDataSet.CONTEXT_0, x);
                } else {
                    zds.putProperty(QDataSet.DEPEND_0, x);
                }
            }
            List<String> problems= new ArrayList<>();
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
     *<blockquote><pre>
     *   plot(x,y,z) shows a plot of Z(X,Y),
     *   link(x,y,z) returns the dataset Z(X,Y).
     *</pre></blockquote>
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
            QDataSet result= bundle( x, y, z );
            BundleDataSet.BundleDescriptor bds= (BundleDescriptor) result.property(QDataSet.BUNDLE_1);
            bds.putProperty( "CONTEXT_0", 2, xname+","+yname ); // note this is a string, not a QDataSet.  This is sloppy, but probably okay for now.
            bds.putProperty( QDataSet.NAME, 0, xname );
            bds.putProperty( QDataSet.NAME, 1, yname );
            bds.putProperty( QDataSet.NAME, 2, zname );

//            DDataSet yds = DDataSet.copy(y);
//            yds.putProperty(QDataSet.DEPEND_0, x);
//            yds.putProperty(QDataSet.PLANE_0, z);
            List<String> problems= new ArrayList<>();
            if ( DataSetUtil.validate(result, problems ) ) {
                return result;
            } else {
                throw new IllegalArgumentException( problems.get(0) );
            }
        } 
        if ( z.rank()==2 && y.rank()==1 && isBundle(z) ) {
            QDataSet z1= DataSetOps.unbundle(z,z.length(0)-1);
            return link( x, y, z1 );
        }
        
        if ( z.rank()==2 && x.rank()==2 && y.rank()==2 ) {
            z= flatten(z);
            z= slice1( z, z.length(0)-1 );
            y= flatten(y);
            y= slice1( y, y.length(0)-1 );
            x= flatten(x);
            x= slice1( x, x.length(0)-1 );
            return link( x, y, z );
                
        }
        
        MutablePropertyDataSet zds = DataSetOps.makePropertiesMutable(z);
        if (x != null || zds.property(QDataSet.DEPEND_0)!=null ) {
            zds.putProperty(QDataSet.DEPEND_0, x);
        }
        if (y != null || zds.property(QDataSet.DEPEND_1)!=null ) {
            zds.putProperty(QDataSet.DEPEND_1, y);
        }
        List<String> problems= new ArrayList<>();
        if ( !DataSetUtil.validate(zds, problems ) ) {
            throw new IllegalArgumentException( problems.get(0) );
        } else {
            return zds;
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

            List<String> problems= new ArrayList<>();
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
            List<String> problems= new ArrayList<>();
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
     * @param d3 rank 1 dataset
     * @param z rank 1 or rank 4 dataset.
     * @return rank 2 bundle when z is rank 1, or a rank 3 dataset when z is rank 3.
     */
    public static QDataSet link( QDataSet d0, QDataSet d1, QDataSet d2, QDataSet d3, QDataSet z ) {
        if (z.rank() == 1) {
            String a1name= (String) d0.property(QDataSet.NAME);
            String a2name= (String) d1.property(QDataSet.NAME);
            String a3name= (String) d2.property(QDataSet.NAME);
            String a4name= (String) d3.property(QDataSet.NAME);
            String a5name= (String) z.property(QDataSet.NAME);
            if ( a1name==null ) a1name="data0";
            if ( a2name==null ) a2name="data1";
            if ( a3name==null ) a3name="data2";
            if ( a4name==null ) a4name="data3";
            if ( a5name==null ) a5name="data4";

            QDataSet result= bundle( d0, d1, d2, d3, z );
            BundleDataSet.BundleDescriptor bds= (BundleDescriptor) result.property(QDataSet.BUNDLE_1);
            bds.putProperty( QDataSet.NAME, 0, a1name );
            bds.putProperty( QDataSet.NAME, 1, a2name );
            bds.putProperty( QDataSet.NAME, 2, a3name );
            bds.putProperty( QDataSet.NAME, 3, a4name );
            bds.putProperty( QDataSet.NAME, 4, a5name );
            
            List<String> problems= new ArrayList<>();
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
            if (d3 != null ) {
                zds.putProperty(QDataSet.DEPEND_3, d3);
            }
            List<String> problems= new ArrayList<>();
            if ( !DataSetUtil.validate(zds, problems ) ) {
                throw new IllegalArgumentException( problems.get(0) );
            } else {
                return zds;
            }
        }

    }
    
    /**
     * @see #link(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     * @param x object which can be converted to a QDataSet
     * @param y object which can be converted to a QDataSet
     * @return the dataset
     * @see #link(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet link( Object x, Object y ) {
        return link( dataset(x), dataset(y) );
    }

    /**
     * @see #link(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     * @param x object which can be converted to a QDataSet
     * @param y object which can be converted to a QDataSet
     * @param z object which can be converted to a QDataSet
     * @return  the dataset
     * @see #link(org.das2.qds.QDataSet, org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet link( Object x, Object y, Object z ) {
        return link( dataset(x), dataset(y), dataset(z) );
    }

    /**
     * @see #link(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     * @param d0 object which can be converted to a QDataSet
     * @param d1 object which can be converted to a QDataSet
     * @param d2 object which can be converted to a QDataSet
     * @param z object which can be converted to a QDataSet
     * @return  the dataset
     * @see #link(org.das2.qds.QDataSet, org.das2.qds.QDataSet, org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */    
    public static QDataSet link( Object d0, Object d1, Object d2, Object z ) {
        return link( dataset(d0), dataset(d1), dataset(d2), dataset(z) );
    }



    /**
     * declare that the dataset is a dependent parameter of an independent parameter.
     * This isolates the QDataSet semantics, and verifies correctness.  See also link(x,y).
     * @param ds the dataset
     * @param dim dimension to declare dependence: 0,1,2.
     * @param dep the independent dataset.
     * @return the dataset, which may be a copy if the data was not mutable.
     */
    public static MutablePropertyDataSet dependsOn( QDataSet ds, int dim, QDataSet dep ) {
        MutablePropertyDataSet mds= DataSetOps.makePropertiesMutable(ds);
        switch (dim) {
            case 0:
                if ( dep!=null && ds.length()!=dep.length() ) {
                    throw new IllegalArgumentException(String.format("ds.length()!=dep.length() (%d!=%d)",ds.length(),dep.length()));
                }   mds.putProperty( QDataSet.DEPEND_0, dep );
                break;
            case 1:
                if ( dep!=null && ds.length(0)!=dep.length() )
                    throw new IllegalArgumentException(String.format("ds.length(0)!=dep.length() (%d!=%d)",ds.length(0),dep.length()));
                mds.putProperty( QDataSet.DEPEND_1, dep );
                break;
            case 2:
                if ( dep!=null && ds.length(0,0)!=dep.length() )
                    throw new IllegalArgumentException(String.format("ds.length(0,0)!=dep.length() (%d!=%d)",ds.length(0,0),dep.length()));
                mds.putProperty( QDataSet.DEPEND_2, dep );
                break;
            case 3:
                if ( dep!=null && ds.length(0,0,0)!=dep.length() )
                    throw new IllegalArgumentException(String.format("ds.length(0,0,0)!=dep.length() (%d!=%d)",ds.length(0,0,0),dep.length()));
                mds.putProperty( QDataSet.DEPEND_3, dep );
                break;
            default:
                break;
        }
        return mds;
    }

    /**
     * This one-argument join was used in a script that George had, so 
     * it must have been a function at some point.
     * @param ds2 rank N dataset
     * @return rank N+1 dataset
     * @deprecated use join(null,ds2) instead.
     */
    public static QDataSet join( QDataSet ds2 ) {
        return join(null,ds2);
    }
    
    /**
     * Join two rank N datasets to make a rank N+1 dataset, with the first dimension
     * having two elements.  This is the anti-slice operator.
     * 
     * If the first dataset is rank N+1 JoinDataset and the other is rank N, then the rank N dataset is
     * added to the rank N+1 dataset.
     * 
     * This is under-implemented right now, and can only join two rank N datasets or if the first dataset is the result of a join.
     * 
     * @param ds1 rank N dataset, or null
     * @param ds2 rank N dataset
     * @see #slices
     * @see #concatenate
     * @return rank N+1 dataset
     */
    public static QDataSet join(QDataSet ds1, QDataSet ds2) {
        if ( ds1==null && ds2==null ) throw new NullPointerException("both ds1 and ds2 are null");
        if ( ds2==null ) throw new NullPointerException("ds2 is null");
        if ( ds1==null ) {
            JoinDataSet ds= new JoinDataSet( ds2.rank()+1 );
            ds.join(ds2);
            if ( ds2.rank()==0 ) {
                QDataSet dep0= (QDataSet) ds2.property(QDataSet.CONTEXT_0);
                if ( dep0!=null && dep0.rank()==0 ) {
                    dep0= join( null, dep0 );
                    ds.putProperty( QDataSet.DEPEND_0, dep0 );
                }
            }
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
     * Merge the two sorted rank N datasets, using their DEPEND_0 datasets, into one rank N dataset.  
     * If neither dataset has DEPEND_0, then this will use the datasets themselves.  When ds1 occurs "before" ds2, then this 
     * is the same as concatenate.
     * When there is a collision where two data points are coincident, use ds1[j].  This is fuzzy, based on the depend_0 cadence of ds1.
     * When ds1 is null (or None), use ds2.
     * Thanks to: http://stackoverflow.com/questions/5958169/how-to-merge-two-sorted-arrays-into-a-sorted-array
     * @param ds1 rank N dataset, or null.
     * @param ds2 rank N dataset
     * @return dataset of rank N with elements interleaved.
     * @see #concatenate(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet merge( QDataSet ds1, QDataSet ds2 ) {
        if ( ds1==null ) return ds2;
        JoinDataSet result= new JoinDataSet(ds1.rank());
        QDataSet dep01= (QDataSet)ds1.property(QDataSet.DEPEND_0);
        QDataSet dep02= (QDataSet)ds2.property(QDataSet.DEPEND_0);
        if ( dep01==null ) {
            if ( dep02==null ) {
                dep01= ds1;
                dep02= ds2;
            } else {
                throw new IllegalArgumentException("ds1 is missing DEPEND_0");
            }
        } else {
            if ( dep02==null ) {
                throw new IllegalArgumentException("ds2 is missing DEPEND_0");
            }
        }
        //there's a bug in result.join where it doesn't look for CONTEXT_0, otherwise this would work without dep0result.
        DataSetBuilder dep0result= new DataSetBuilder(1,ds1.length()+ds2.length());
        int n1= dep01.length();
        int n2= dep02.length();
        int i1= 0; 
        int i2= 0;
        
        QDataSet cadenceDep01= DataSetUtil.guessCadenceNew( dep01, null );
        Units dep0u= SemanticOps.getUnits(dep01);
        if ( cadenceDep01==null ) cadenceDep01= DataSetUtil.asDataSet(0,dep0u);
        cadenceDep01= Ops.divide(cadenceDep01,2);
        
        while (i1 < n1 && i2 < n2 ) {
            if ( Ops.lt( Ops.abs( Ops.subtract( dep01.slice(i1), dep02.slice(i2) ) ), cadenceDep01 ).value()!=0 ) {
                dep0result.putValue( -1, DataSetUtil.asDatum( dep01.slice(i1) ) );
                result.join( ds1.slice(i1++) );
                i2++;
            } else if ( Ops.le( dep01.slice(i1), dep02.slice(i2) ).value()>0 ) {
                dep0result.putValue( -1, DataSetUtil.asDatum( dep01.slice(i1) ) );
                result.join( ds1.slice(i1++) );
            } else {
                dep0result.putValue( -1, DataSetUtil.asDatum( dep02.slice(i2) ) );
                result.join( ds2.slice(i2++) );
            }
            dep0result.nextRecord();
        }
        while ( i1<n1 ) {
            dep0result.putValue( -1, DataSetUtil.asDatum( dep01.slice(i1) ) );
            dep0result.nextRecord();
            result.join( ds1.slice(i1++) );
        }
        while ( i2<n2 ) {
            dep0result.putValue( -1, DataSetUtil.asDatum( dep02.slice(i2) ) );
            dep0result.nextRecord();
            result.join( ds2.slice(i2++) );
        }
        result.putProperty( QDataSet.DEPEND_0, dep0result.getDataSet() );
        return result;
    }
    
    /**
     * get the label, using the NAME when LABEL is not available.
     * @param ds the dataset
     * @return the human-readable label.
     */
    public static String guessLabel( QDataSet ds ) {
        String s= (String) ds.property( QDataSet.LABEL );
        if ( s==null ) s= guessName(ds);
        return s;
    }

    /**
     * get the label, using the NAME when LABEL is not available.
     * @param ds the dataset
     * @param deft the default label to use.
     * @return the human-readable label.
     */
    public static String guessLabel( QDataSet ds, String deft ) {
        String s= (String) ds.property( QDataSet.LABEL );
        if ( s==null ) s= guessName(ds,deft);
        return s;
    }
    
    /**
     * guess a name for the dataset, looking for NAME and then safeName(LABEL).  The
     * result will be a Java-style identifier suitable for the variable.
     * @param ds the dataset
     * @return the name or null if there is no NAME or LABEL
     */
    public static String guessName( QDataSet ds ) {
        return guessName( ds, null );
    }

    /**
     * guess a name for the dataset, looking for NAME and then safeName(LABEL).  The
     * result will be a Java-style identifier suitable for the variable.
     * @param ds the dataset
     * @param deft the default name to use.
     * @return the name, or deft if there is no NAME or LABEL.
     */
    public static String guessName( QDataSet ds, String deft ) {
        String label= (String) ds.property( QDataSet.NAME );
        if ( label!=null && label.length()==0 ) { 
            logger.log(Level.WARNING, "dataset has zero-length name: {0}", ds);
            label=null;
        }
        if ( label==null ) {
            label= (String) ds.property( QDataSet.LABEL );
            if ( label!=null ) label= safeName( label );
        }
        if ( label==null ) {
            return deft==null ? null : safeName(deft);
        } else {
            return safeName(label);
        }
    }

    /**
     * extra spaces and pipes cause problems in the Operations text field.  Provide so that data sources can provide
     * safer names, while we test safe-name requirements on a broader test set.  Use of this method will allow us to see
     * where changes are needed.  TODO: where is this used?
     * @param suggest a name, possibly containing pipes (|)
     * @return suggest, but with pipe converted to underscore.
     */
    public static String saferName( String suggest ) {
        return suggest.trim().replaceAll("\\|","_");
    }

    /**
     * returns true if the name is a Java-style identifier, starting
     * with one of a-z, A-Z, or _; followed by a-z, A-Z, 0-9, or _; and
     * note that only ASCII characters are allowed.
     * 
     * @param name
     * @return  true if the name is a safe identifier name.
     */
    public static boolean isSafeName( String name ) {
        if ( name.length()<1 ) return false;
        if ( Character.isJavaIdentifierStart( name.charAt(0) ) && name.charAt(0)<128 ) {
            for ( int i=1; i<name.length(); i++ ) {
                if ( !( Character.isJavaIdentifierPart( name.charAt(i) ) && name.charAt(0)<128 ) ) return false;
            }
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * made a Java-style identifier from the provided string
     * See Autoplot/src/scripts/safeName.jy which demonstrates this.
     * @param suggest a name, possibly containing spaces and illegal characters
     * @return a Java-style identifier
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
     * return true of the representation of fill is different in the two data sets.
     * TODO: this does not consider WEIGHTS.
     * @param ds1
     * @param ds2
     * @return true of the representation of fill is different in the two data sets.
     */
    public static boolean fillIsDifferent(QDataSet ds1, QDataSet ds2) {
        String[] props = new String[]{QDataSet.FILL_VALUE, QDataSet.VALID_MIN, QDataSet.VALID_MAX};
        for (String p : props) {
            Number fill1 = (Number) ds1.property(p);
            Number fill2 = (Number) ds2.property(p);
            if (fill1 == null) {
                if (fill2 != null) {
                    return true;
                }
            } else {
                if (!fill1.equals(fill2)) {
                    return true;
                }
            }
        }
        return false;
    }    

    /**
     * transpose the rank 2 or rank 1 dataset.  result[i,j]= ds[j,i] for each i,j;
     * or in the case of rank 1, the DEPEND_0 (x) becomes a dependent on the data (y).
     * @param ds rank 2 dataset
     * @return rank 2 dataset
     */
    public static QDataSet transpose(QDataSet ds) {
        if ( ds.rank()==1 ) {
            QDataSet xx= (QDataSet) ds.property(QDataSet.DEPEND_0);
            if ( xx==null ) {
                throw new IllegalArgumentException("rank 1 data must have DEPEND_0");
            } else {
                QDataSet yy= ds;
                return Ops.link( yy, xx );
            }
        } else {
            return new TransposeRank2DataSet(ds);
        }
    }

    public static QDataSet transpose( Object ds ) {
        return transpose( dataset(ds) );
    }
    
    /**
     * returns true iff the dataset values are equivalent.  Note this
     * may promote rank, etc.  If the two datasets have enumerations, then we 
     * create datums and check .equals.  This does not check TITLE, etc,  
     * just that the units and values are equal.
     * @param ds1 the first dataset
     * @param ds2 the second dataset
     * @return true if the dataset values are equivalent.
     */
    public static boolean equivalent( QDataSet ds1, QDataSet ds2 ) {
        if ( ds1!=null && ds1==ds2 ) return true;
        if ( ds1==null ) throw new NullPointerException("ds1 is null");
        if ( ds2==null ) throw new NullPointerException("ds2 is null");
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
            if ( !u1.isConvertibleTo(u2) ) return false;
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
     * <ul>
     * <li>JOIN, BINS   do not increase dataset dimensionality.
     * <li>DEPEND       increases dimensionality by dimensionality of DEPEND ds.
     * <li>BUNDLE       increases dimensionality by N, where N is the number of bundled datasets.
     * </ul>
     * Note this includes implicit dimensions taken by the primary dataset:
     * <ul>
     *   <li>Z(time,freq)&rarr;3
     *   <li>rand(20,20)&rarr;3
     *   <li>B_gsm(20,[X,Y,Z])&rarr;4
     * </ul>
     * @param dss the dataset
     * @return the number of dimensions occupied by the data.
     */
    public static int dimensionCount( QDataSet dss ) {
        return dimensionCount( dss, false );
    }

    /**
     * returns the number of physical dimensions of a dataset.
     * @param dss the dataset
     * @param noImplicit when a dataset is the independent parameter, then there are no implicit dimensions.
     * @return the number of dimensions occupied by the data.
     */
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
                if ( ds.rank()>QDataSet.MAX_RANK ) {
                    ds= ds.slice(0);
                } else {
                    ds= DataSetOps.slice0(ds, 0);
                }
            } else {
                return 1;
            }
        }
        return dim;
    }
    
    /**
     * returns the number of physical dimensions of the object when
     * interpreted as a dataset.
     * @param dss the object that can be coerced into a dataset.
     * @return the number of dimensions occupied by the data.
     * @see #dataset(java.lang.Object) 
     */
    public static int dimensionCount( Object dss ) {
        return dimensionCount( dataset(dss) );
    }

    /**
     * Experiment with multi-threaded FFTPower function.  This breaks up the 
     * task into four independent tasks that can be run in parallel.
     * @param ds rank 2 dataset ds(N,M) with M&gt;len
     * @param len the number of elements to have in each fft.
     * @param mon a ProgressMonitor for the process
     * @return rank 2 FFT spectrum
     */
    public static QDataSet fftPowerMultiThread(final QDataSet ds, final int len, final ProgressMonitor mon ) {
        
        final ArrayList<ProgressMonitor> mons = new ArrayList<>();
        final QDataSet[] out = new QDataSet[4];
        
        final int length = ds.length();
                
        mons.add( new NullProgressMonitor() );
        mons.add( new NullProgressMonitor() );
        mons.add( new NullProgressMonitor() );
        mons.add( new NullProgressMonitor() );
        
        Runnable run1 = () -> {
            try {
                out[0] = Ops.fftPower(ds.trim(0, length/ 4), len, mons.get(0));
                //ScriptContext.plot( 1, out1 );
            } catch (Exception ex) {
                logger.log( Level.WARNING, ex.getMessage(), ex );
            }
        };

        Runnable run2 = () -> {
            try {
                out[1] = Ops.fftPower(ds.trim(length / 4, (length * 2) / 4), len, mons.get(1));
                //ScriptContext.plot( 2, out2 );
            } catch (Exception ex) {
                logger.log( Level.WARNING, ex.getMessage(), ex );
            }
        };

        Runnable run3 = () -> {
            try {
                out[2] = Ops.fftPower(ds.trim((length * 2) / 4, (length * 3) / 4), len, mons.get(2));
                //ScriptContext.plot( 3, out3 );
            } catch (Exception ex) {
                logger.log( Level.WARNING, ex.getMessage(), ex );
            }
        };

        Runnable run4 = () -> {
            try {
                out[3] = Ops.fftPower(ds.trim((length * 3) / 4, length), len, mons.get(3));
                //ScriptContext.plot( 4, out4 );
            } catch (Exception ex) {
                logger.log( Level.WARNING, ex.getMessage(), ex );
            }
        };
        
        new Thread(run1).start();
        new Thread(run2).start();
        new Thread(run3).start();
        new Thread(run4).start();
        
        
        while ( !(mons.get(0)).isFinished() || !(mons.get(1)).isFinished() || !(mons.get(2)).isFinished() || !(mons.get(3)).isFinished()) {
            try {
                Thread.sleep(50);
            } catch ( InterruptedException ex ) {
                
            }
        }
        
        QDataSet concat= null;
        for ( QDataSet out1 : out ) {
            concat= Ops.append( concat, out1 );
        }
        
        return concat;
        
        
    }
       
    /**
     * Point with placeholder for index.
     */
    private static class PointWeightedInt extends ProGAL.geom3d.PointWeighted {
        int idx;
        PointWeightedInt( double x, double y, double z, double w, int idx ) {
            super( x,y,z,w );
            this.idx= idx;
        }
        @Override
        public String toString() {
            return String.valueOf(idx);
        }

        @Override
        public boolean equals(Object o) {
            if ( o instanceof PointWeightedInt ) {
                if (((PointWeightedInt)o).idx==this.idx ) {
                    return true;
                } else {
                    return super.equals(o);
                }
            } else {
                return super.equals(o); 
            }
        }        
        @Override
        public int hashCode() {
            return this.idx;
        }
        
    }
    
//    /**
//     * return the volume of a 4-point tetrahededron. 
//     * @param p the first point.  The tetr is shifted so that this corner is at the origin.
//     * @param a tetr corner
//     * @param b tetr corner
//     * @param c tetr corner
//     * @return the volume
//     */
//    private static double volume( ProGAL.geom3d.Point p, ProGAL.geom3d.Point a, ProGAL.geom3d.Point b, ProGAL.geom3d.Point c ) {
//        a= a.subtract(p);
//        b= b.subtract(p);
//        c= c.subtract(p);
//        return volume(a,b,c);
//    }
    
    /**
     * return the volume of the 4-point tetrahedron with one point
     * at the origin, and points a,b,c are the points not at the origin.
     * See http://www.hpl.hp.com/techreports/2002/HPL-2002-320.pdf, page 4
     * @param a tetrahedron corner
     * @param b tetrahedron corner
     * @param c tetrahedron corner
     * @return the volume
     */
    private static double volume(  ProGAL.geom3d.Point a,  ProGAL.geom3d.Point b,  ProGAL.geom3d.Point c ) {        
        return Math.abs( a.x() * ( b.y() * c.z() - b.z() * c.y() ) -
                a.y() * ( b.x() * c.z() - b.z() * c.x() ) +
                a.z() * ( b.x() * c.y() - b.y() * c.x() ) ) / 6;
    }

    /**
     * return the volume of the 3-point triangle in 2 dimensions.
     * See http://www.mathopenref.com/coordtrianglearea.html
     * @param a 2-d point corner
     * @param b 2-d point corner
     * @param c 2-d point corner
     * @return the volume
     */
    private static double area( ProGAL.geom2d.Point a, ProGAL.geom2d.Point b, ProGAL.geom2d.Point c ) {        
        return Math.abs( a.x() * ( b.y()- c.y() ) +
                b.x() * ( c.y() - a.y() ) +
                c.x() * ( a.y() - b.y() ) ) / 2;
    }
    
    /**
     * Point with placeholder for index.
     */
    private static class VertexInt extends ProGAL.geom2d.delaunay.Vertex {
        int idx;
        VertexInt( double x, double y, int idx ) {
            super( x,y );
            this.idx= idx;
        }
        @Override
        public String toString() {
            return String.valueOf(idx);
        }

        @Override
        public boolean equals(Object o) {
            if ( o instanceof VertexInt ) {
                if ( ((VertexInt)o).idx==this.idx ) {
                    return true;
                } else {
                    return super.equals(o);
                }
            } else {
                return super.equals(o); //To change body of generated methods, choose Tools | Templates.
            }
        }

        @Override
        public int hashCode() {
            return this.idx;
        }
        
    }    
    
    /**
     * 3-D interpolation performed by tesselating the space (with 4-point 
     * tetrahedra) and doing interpolation.
     * NOTE: this does not check units.
     * @param xyz rank 2 bundle of x,y,z data.
     * @param data rank 1 dependent data, a function of x,y,z.
     * @param xinterp the x locations to interpolate, rank 0, 1, or 2.
     * @param yinterp the y locations to interpolate
     * @param zinterp the z locations to interpolate
     * @return the interpolated data.
     */
    public static QDataSet buckshotInterpolate( QDataSet xyz, QDataSet data, QDataSet xinterp, QDataSet yinterp, QDataSet zinterp ) {
        
        if ( xyz.rank()!=2 || xyz.length(0)!=3 ) {
            throw new IllegalArgumentException("xyz must be rank 2 bundle of x,y,z positions");
        } 
        
        if ( xinterp.rank()>2 ) {
            throw new IllegalArgumentException("xinterp rank can be 0,1, or 2");
        } else if ( xinterp.rank()==2 ) {
            int n= DataSetUtil.totalLength(xinterp);
            xinterp= reform(xinterp, new int[] { n } );
            yinterp= reform(yinterp, new int[] { n } );
            zinterp= reform(zinterp, new int[] { n } );
        } else if ( xinterp.rank()==0 ) {
            xinterp= reform( xinterp, new int[] {1} );
            yinterp= reform( yinterp, new int[] {1} );
            zinterp= reform( zinterp, new int[] {1} );
        }
        
        QDataSet xx= Ops.unbundle( xyz, 0 );
        QDataSet yy= Ops.unbundle( xyz, 1 );
        QDataSet zz= Ops.unbundle( xyz, 2 );
              
        Units u= SemanticOps.getUnits(xyz);
        
        // rescale xx,yy,zz so that all are from 0.1 to 0.9.
        QDataSet xx1= Ops.rescale( append(xx,xinterp), dataset(u.createDatum(0.1)), dataset(u.createDatum(0.9)) );
        QDataSet yy1= Ops.rescale( append(yy,yinterp), dataset(u.createDatum(0.1)), dataset(u.createDatum(0.9))  );
        QDataSet zz1= Ops.rescale( append(zz,zinterp), dataset(u.createDatum(0.1)), dataset(u.createDatum(0.9))  );
        
        int inn= xx.length();
        xx= xx1.trim(0,inn);
        yy= yy1.trim(0,inn);
        zz= zz1.trim(0,inn);
        int l= xx1.length();
        xinterp= xx1.trim(inn,l);
        yinterp= yy1.trim(inn,l);
        zinterp= zz1.trim(inn,l);
                
        List<ProGAL.geom3d.PointWeighted> points= new ArrayList<>(xyz.length());
        for ( int i=0; i<xyz.length(); i++ ) {
            points.add( new PointWeightedInt( 
                xx.value(i), yy.value(i), zz.value(i), 1.0, i
            ) );
        }
        ProGAL.geom3d.tessellation.BowyerWatson.RegularTessellation rt= new ProGAL.geom3d.tessellation.BowyerWatson.RegularTessellation(points);
        //for ( ProGAL.geom3d.PointWeighted p: points ) {
        //    rt.walk( p );
        //}
        
        if ( xyz instanceof MutablePropertyDataSet && !((MutablePropertyDataSet)xyz).isImmutable() ) {
            ((MutablePropertyDataSet)xyz).putProperty( "TRIANGULATION", rt );
        }
        
        DDataSet result= DDataSet.create( DataSetUtil.qubeDims(xinterp) );
        
        QDataSet wds= DataSetUtil.weightsDataSet( data );
        Number fill= (Number)wds.property(QDataSet.FILL_VALUE);
        double dfill;
        if ( fill==null ) dfill= -1e38; else dfill= fill.doubleValue();
        result.putProperty( QDataSet.FILL_VALUE, dfill );
        boolean hasFill= false;
        
        DataSetIterator dsi= new QubeDataSetIterator(xinterp);
        
        iloop: while ( dsi.hasNext() ) {
            dsi.next();
            ProGAL.geom3d.Point thePoint= new ProGAL.geom3d.Point( dsi.getValue(xinterp), dsi.getValue(yinterp), dsi.getValue(zinterp) );
            
            //ArrayList a= new ArrayList();
            ProGAL.geom3d.tessellation.BowyerWatson.Tetr t= rt.walk( new ProGAL.geom3d.PointWeighted( thePoint.x(), thePoint.y(), thePoint.z(), 1.0 ) ); // , a );
            
            ProGAL.geom3d.Point[] abcd= Arrays.copyOf( t.getCorners(), t.getCorners().length ); 
            PointWeightedInt[] abcdi= new PointWeightedInt[4];
            for ( int k=0; k<4; k++ ) {
                if ( !( abcd[k] instanceof PointWeightedInt ) ) { // this is outside of the triangulation, an extrapolation.
                    dsi.putValue( result, dfill );
                    hasFill= true;
                    continue iloop;
                }
                abcdi[k]= (PointWeightedInt)abcd[k];
                abcd[k]= abcd[k].subtract(thePoint); // do not modify the mutable Points of the tessellation
            }
            double[] w= new double[4];
            w[0]= volume( abcd[1], abcd[2], abcd[3] );
            w[1]= volume( abcd[0], abcd[2], abcd[3] );
            w[2]= volume( abcd[0], abcd[1], abcd[3] );
            w[3]= volume( abcd[0], abcd[1], abcd[2] );
            double n= w[0] + w[1] + w[2] + w[3];
            for ( int k=0; k<4; k++ ) {
                w[k]/= n;
            }
            double d= data.value( abcdi[0].idx ) * w[0] 
                +   data.value( abcdi[1].idx ) * w[1] 
                +   data.value( abcdi[2].idx ) * w[2] 
                +   data.value( abcdi[3].idx ) * w[3];

            dsi.putValue( result,d );
                    
        }
        
        DataSetUtil.copyDimensionProperties( data, result );
        if ( !hasFill ) {
            result.putProperty( QDataSet.FILL_VALUE, null );
        }
        
        return result;
    };
      
    /**
     * return a triangle tesselation of the space identified by 
     * rank 1 xx and yy.
     * @param xx rank 1 dataset
     * @param yy rank 1 dataset
     * @return rank 2 ds[n,3] dataset.
     */
    public static QDataSet triangulate( QDataSet xx, QDataSet yy ) {
        
        xx= rescale( xx, dataset(0), dataset(1) );
        yy= rescale( yy, dataset(0), dataset(1) );
        
        List<ProGAL.geom2d.Point> points= new ArrayList<>(xx.length());
        for ( int i=0; i<xx.length(); i++ ) {
            points.add( new VertexInt( xx.value(i), yy.value(i), i ) );
        }
        ProGAL.geom2d.delaunay.DTWithBigPoints rt=null;
        try {
            rt= new ProGAL.geom2d.delaunay.DTWithBigPoints( points );  
        } catch ( Exception ex ) {
            throw new IllegalArgumentException("data cannot contain close colinear points");
        }
        
        List<ProGAL.geom2d.delaunay.Triangle> triangles= rt.getTriangles();
        
        DataSetBuilder b= new DataSetBuilder(2,100,3);
        triangles.forEach((t) -> {
            Object o1= t.getCorner(0);
            Object o2= t.getCorner(1);
            Object o3= t.getCorner(2);
            if (o1 instanceof VertexInt 
                    && o2 instanceof VertexInt
                    && o3 instanceof VertexInt) {
                VertexInt c1= (VertexInt)t.getCorner(0);
                VertexInt c2= (VertexInt)t.getCorner(1);
                VertexInt c3= (VertexInt)t.getCorner(2);
                b.nextRecord( new double[] { c1.idx, c2.idx, c3.idx } );
            }
        });
        
        return b.getDataSet();

    }
    
    /** 2-D interpolation performed by tessellating the space (with 3-point 
     * triangles) and doing interpolation.
     * NOTE: this does not check units.
     * @param xy rank 2 bundle of independent data x,y points.
     * @param data rank 1 dependent data, a function of x,y.
     * @param xinterp the x locations to interpolate
     * @param yinterp the y locations to interpolate
     * @return the interpolated data.
     */    
    public static QDataSet buckshotInterpolate( QDataSet xy, QDataSet data, QDataSet xinterp, QDataSet yinterp ) {
        
        if ( xy.rank()!=2 || xy.length(0)!=2 ) {
            throw new IllegalArgumentException("xy must be rank 2 bundle of x,y positions");
        } 
        
        if ( xinterp.rank()>2 ) {
            throw new IllegalArgumentException("xinterp rank can be 0,1, or 2");
        } else if ( xinterp.rank()==2 ) {
            int n= DataSetUtil.totalLength(xinterp);
            xinterp= reform(xinterp, new int[] { n } );
            yinterp= reform(yinterp, new int[] { n } );
        } else if ( xinterp.rank()==0 ) {
            xinterp= reform( xinterp, new int[] {1} );
            yinterp= reform( yinterp, new int[] {1} );
        }
        
        QDataSet xx= Ops.unbundle( xy, 0 );
        QDataSet yy= Ops.unbundle( xy, 1 );
              
        Units u= SemanticOps.getUnits(xy);
        
        boolean rescale= true;
        if ( rescale ) {
            // rescale xx,yy,zz so that all are from 0.1 to 0.9.
            QDataSet xx1= Ops.rescale( append(xx,xinterp), dataset(u.createDatum(0.1)), dataset(u.createDatum(0.9)) );
            QDataSet yy1= Ops.rescale( append(yy,yinterp), dataset(u.createDatum(0.1)), dataset(u.createDatum(0.9))  );
        
            int inn= xx.length();
            xx= xx1.trim(0,inn);
            yy= yy1.trim(0,inn);
            int l= xx1.length();
            xinterp= xx1.trim(inn,l);
            yinterp= yy1.trim(inn,l);
        }
        
        List<ProGAL.geom2d.Point> points= new ArrayList<>(xy.length());
        for ( int i=0; i<xy.length(); i++ ) {
            points.add( new VertexInt( xx.value(i), yy.value(i), i ) );
        }
        ProGAL.geom2d.delaunay.DTWithBigPoints rt= new ProGAL.geom2d.delaunay.DTWithBigPoints( points );
        
        DDataSet result= DDataSet.create( DataSetUtil.qubeDims(xinterp));
        
        QDataSet wds= DataSetUtil.weightsDataSet( data );
        Number fill= (Number)wds.property(QDataSet.FILL_VALUE);
        double dfill;
        if ( fill==null ) dfill= -1e38; else dfill= fill.doubleValue();
        result.putProperty( QDataSet.FILL_VALUE, dfill );
        boolean hasFill= false;
        
        DataSetIterator dsi= new QubeDataSetIterator(xinterp);
        
        iloop: while ( dsi.hasNext() ) {
            dsi.next();
            ProGAL.geom2d.Point thePoint= new ProGAL.geom2d.Point( dsi.getValue(xinterp), dsi.getValue(yinterp) );
            ProGAL.geom2d.Triangle t= rt.walk( thePoint, null );
            ProGAL.geom2d.Point[] abc= new ProGAL.geom2d.Point[] { t.getCorner(0), t.getCorner(1), t.getCorner(2) }; 
            VertexInt[] abci= new VertexInt[3];
            for ( int k=0; k<3; k++ ) {
                if ( !( abc[k] instanceof VertexInt ) ) { // this is outside of the triangulation, an extrapolation.
                    dsi.putValue( result, dfill );
                    hasFill= true;
                    continue iloop;
                }
                abci[k]= (VertexInt)abc[k];
            }
            double[] w= new double[3];
            w[0]= area( thePoint, abc[1], abc[2] );
            w[1]= area( thePoint, abc[0], abc[2] );
            w[2]= area( thePoint, abc[0], abc[1] );
            double n= w[0] + w[1] + w[2];
            for ( int k=0; k<3; k++ ) {
                w[k]/= n;
            }
            double d;
            // nearest neighbor code
//            if ( w[0]>w[1] ) {
//                if ( w[0]>w[2] ) {
//                    d= data.value( abci[0].idx );
//                } else {
//                    d= data.value( abci[2].idx );
//                }
//            } else {
//                if ( w[1]>w[2] ) {
//                    d= data.value( abci[1].idx );
//                } else {
//                    d= data.value( abci[2].idx );
//                }
//            }            
            d= data.value( abci[0].idx ) * w[0] 
                +   data.value( abci[1].idx ) * w[1] 
                +   data.value( abci[2].idx ) * w[2];
            dsi.putValue( result,d );
            
        }
                
        DataSetUtil.copyDimensionProperties( data, result );
        if ( !hasFill ) {
            result.putProperty( QDataSet.FILL_VALUE, null );
        }
        
        result.putProperty( "TRIANGULATION", rt );

        return result;
        
    };
    
    /**
     * return the gamma function for numbers greater than 0.  This will 
     * soon work for any number where gamma has a result (Apache Math v3 is needed for this).
     * @param n
     * @return 
     */
    public static final double gamma( double n ) {
        double logGamma= org.apache.commons.math.special.Gamma.logGamma(n);
        return Math.exp(logGamma);
    }
    
    /**
     * return the gamma function for numbers greater than 0.  This will 
     * soon work for any number where gamma has a result (Apache Math v3 is needed for this).
     * @param n
     * @return 
     */
    public static final QDataSet gamma( Object n ) {
        QDataSet nn= dataset(n);
        WritableDataSet result= zeros(nn.length());
        for ( int i=0; i<nn.length(); i++ ) {
            double logGamma= org.apache.commons.math.special.Gamma.logGamma(nn.value(i));
            result.putValue( i, Math.exp(logGamma) );
        }
        return result;
    }
    
    /**
     * closest double to &pi; or TAU/2
     * @see Math#PI
     */
    public static final double PI = Math.PI;
    
    /**
     * closest double to &tau; or 2*PI
     * @see Math#PI
     */
    public static final double TAU = Math.PI * 2;
    
    /**
     * the closest double to e, the base of natural logarithms.
     * @see Math#E
     */
    public static final double E = Math.E;
}
