/*
 * DataSetUtil.java
 *
 * Created on April 1, 2007, 4:28 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.das2.qds;

import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.IllegalFormatConversionException;
import java.util.logging.Level;
import org.das2.datum.Units;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.logging.Logger;
import org.das2.datum.CacheTag;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.DatumUtil;
import org.das2.datum.DatumVector;
import org.das2.datum.EnumerationUnits;
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.LocationUnits;
import org.das2.datum.TimeLocationUnits;
import org.das2.datum.TimeParser;
import org.das2.datum.UnitsConverter;
import org.das2.datum.UnitsUtil;
import org.das2.datum.format.DatumFormatter;
import org.das2.datum.format.DefaultDatumFormatter;
import org.das2.datum.format.EnumerationDatumFormatterFactory;
import org.das2.datum.format.FormatStringFormatter;
import org.das2.datum.format.TimeDatumFormatter;
import org.das2.util.LoggerManager;
import org.das2.qds.examples.Schemes;
import org.das2.qds.ops.Ops;
import org.das2.qds.util.AutoHistogram;
import org.das2.qds.util.LinFit;
import org.das2.util.ColorUtil;
import java.awt.Color;

/**
 * Utilities for QDataSet, such as conversions from various forms
 * to QDataSet, and doing a units conversion.  
 * 
 * TODO: DataSetUtil, DataSetOps, and org.virbo.dsops.Ops have become blurred 
 * over the years.  These should either be combined or new mission statements 
 * need to be created.
 * 
 * @author jbf
 */
public class DataSetUtil {

    private static final Logger logger= LoggerManager.getLogger("qdataset.ops");

    private static final String CLASSNAME= "org.das2.qds.DataSetUtil";
    
    /**
     * creates a dataset of integers 0,1,2,...,n-1.
     * @param n the bound
     * @return the dataset
     */
    public static MutablePropertyDataSet indexGenDataSet(int n) {
        return new IndexGenDataSet(n);
    }

    /**
     * creates a dataset with the given cadence, start and length.
     * This is danger code, because the CADENCE must be reset if the UNITS are reset.
     * use tagGenDataSet( int n, final double start, final double cadence, Units units ) if
     * units are going to be specified.
     * @param n the number of elements
     * @param start the value for the first element.
     * @param cadence the step size between elements
     * @return  the dataset
     */
    public static MutablePropertyDataSet tagGenDataSet(int n, final double start, final double cadence) {
        return new TagGenDataSet( n, cadence, start );
    }

    /**
     * creates a dataset with the given cadence, start and length.  QDataSet.CADENCE
     * will be set based on the units specified.  Do not change the units of the
     * result without updating cadence as well.
     * @param n the number of elements
     * @param start the value for the first element.
     * @param cadence the step size between elements
     * @param units the units of the dataset, for example Units.cm
     * @return  the dataset
     */
    public static MutablePropertyDataSet tagGenDataSet(int n, final double start, final double cadence, Units units ) {
        return new TagGenDataSet( n, cadence, start, units );
    }

    /**
     * creates a dataset with the given cadence, start and length.
     * @param n the number of elements
     * @param value the value for each element
     * @return the dataset
     */
    public static MutablePropertyDataSet replicateDataSet(int n, final double value) {
        return new TagGenDataSet( n, 0., value, Units.dimensionless );
    }

    /**
     * returns true if the dataset is monotonically increasing.
     * If the dataset has the MONOTONIC property set to Boolean.TRUE, believe it.
     * The data can contain repeated values. 
     * An empty dataset is not monotonic.
     * We now use a weights dataset to more thoroughly check for fill.
     * The dataset may contain fill data, only the non-fill portions are considered.
     * @param ds the rank 1 dataset with physical units.
     * @return true when the dataset is monotonically increasing.
     * @see org.das2.qds.QDataSet#MONOTONIC
     * @see org.das2.qds.ArrayDataSet#monotonicSubset(org.das2.qds.ArrayDataSet) 
     * @see #isMonotonicAndIncreasing(org.das2.qds.QDataSet) 
     * @see Ops#ensureMonotonic
     */
    public static boolean isMonotonic(QDataSet ds) {
        Logger llogger= LoggerManager.getLogger("qdataset.ops.monotonic");
        
        llogger.entering( "DataSetUtil", "isMonotonic", ds.toString() );
        
        if (ds.rank() != 1) { // TODO: support bins dataset rank 2 with BINS_1="min,max"
            llogger.exiting( "DataSetUtil", "isMonotonic" );
            return false;
        }

        if (ds.length() == 0) {
            llogger.exiting( "DataSetUtil", "isMonotonic" );
            return false;
        }

        Boolean precalc= (Boolean) ds.property( QDataSet.MONOTONIC );
        if ( precalc!=null ) {
            llogger.exiting( "DataSetUtil", "isMonotonic" );
            return precalc;
        }
        
        QDataSet wds= DataSetUtil.weightsDataSet(ds);
        int i;

        for ( i=0; i<ds.length() && wds.value(i)==0; i++ ) {
            // find first valid point.
        }

        if ( i==ds.length() ) {
            llogger.exiting( "DataSetUtil", "isMonotonic" );
            return false;
        }

        double last = ds.value(i);

        for ( i = i+1; i < ds.length(); i++) {
            double d = ds.value(i);
            double w = wds.value(i);
            if ( w==0 ) continue;
            if ( d < last  ) {
                llogger.exiting( "DataSetUtil", "isMonotonic" );
                return false;
            } 
            last = d;
        }

        llogger.exiting( "DataSetUtil", "isMonotonic" );
        return true;
    }
    
    /**
     * returns true if the dataset is monotonically increasing 
     * and does not contain repeat values.
     * An empty dataset is not monotonic.
     * The dataset may contain fill data, only the non-fill portions are considered.
     * @param ds the rank 1 dataset with physical units.
     * @return true when the dataset is monotonically increasing.
     * @see org.das2.qds.QDataSet#MONOTONIC
     * @see org.das2.qds.ArrayDataSet#monotonicSubset(org.das2.qds.ArrayDataSet) 
     * @see #isMonotonic(org.das2.qds.QDataSet) 
     */
    public static boolean isMonotonicAndIncreasing(QDataSet ds) {
        logger.entering( "DataSetUtil", "isMonotonicAndIncreasing");
        if (ds.rank() != 1) { // TODO: support bins dataset rank 2 with BINS_1="min,max"
            return false;
        }

        if (ds.length() == 0) {
            logger.exiting( "DataSetUtil", "isMonotonicAndIncreasing", false );
            return false;
        }
        if (ds instanceof IndexGenDataSet ) return true;
        
        QDataSet wds= DataSetUtil.weightsDataSet(ds);
        int i;

        for ( i=0; i<ds.length() && wds.value(i)==0; i++ ) {
            // find first valid point.
        }

        if ( i==ds.length() ) {
            logger.exiting( "DataSetUtil", "isMonotonicAndIncreasing", false );
            return false;
        }

        double last = ds.value(i);

        for ( i = i+1; i < ds.length(); i++) {
            double d = ds.value(i);
            double w = wds.value(i);
            if ( w==0 ) continue;
            if ( d <= last  ) {
                logger.log(Level.FINER, "non-monotonic point found at {0}", i);
                logger.exiting( "DataSetUtil", "isMonotonicAndIncreasing", false );
                return false;
            } 
            last = d;
        }
        logger.exiting( "DataSetUtil", "isMonotonicAndIncreasing", true);
        return true;
    }
    
    /**
     * quickly determine, with high accuracy, if data is monotonic (repeated values
     * allowed).  This should
     * be a constant-time operation, and be extremely unlikely to fail.
     * @param ds
     * @return true if the data does pass quick tests for monotonic.
     * @see #isMonotonicAndIncreasing(org.das2.qds.QDataSet) 
     * @see QDataSet#MONOTONIC
     */
    public static boolean isMonotonicQuick(QDataSet ds) {
        logger.entering( "DataSetUtil", "isMonotonicQuick" );
        logger.finest("enter isMonotonicQuick test for "+QDataSet.MONOTONIC);
        if (ds instanceof IndexGenDataSet ) return true;
        Boolean precalc= (Boolean) ds.property( QDataSet.MONOTONIC );
        if ( precalc!=null ) {
            logger.exiting( "DataSetUtil", "isMonotonic" );
            return precalc;
        }
        if (ds.rank() == 1) {
            if (ds.length() < 100) {
                return DataSetUtil.isMonotonic(ds);
            } else {
                QDataSet wds = DataSetUtil.weightsDataSet(ds);
                Random r = new Random(0);
                int i = 0;
                double last = -1.0 * Double.MAX_VALUE;
                int n = ds.length();
                int jump = n / 20;
                for (; i < n; i += (1 + r.nextInt(jump))) {
                    double d = ds.value(i);
                    double w = wds.value(i);
                    while (w == 0 && i < n) {
                        d = ds.value(i);
                        w = wds.value(i);
                        i++;
                    }
                    if (i == n) {
                        break;
                    }
                    if (d < last) {
                        logger.exiting( "DataSetUtil", "isMonotonicQuick", false );
                        return false;
                    }
                    last = d;
                }
                logger.exiting( "DataSetUtil", "isMonotonicQuick", true );
                return true;
            }
        } else {
            logger.exiting( "DataSetUtil", "isMonotonicQuick", false );
            return false;
        }
    }
    
    /**
     * quickly determine, with high accuracy, if data is monotonic.  This should
     * be a constant-time operation, and be extremely unlikely to fail.
     * @param ds
     * @return true if the data does pass quick tests for monotonic increasing.
     * @see #isMonotonicAndIncreasing(org.das2.qds.QDataSet) 
     * @see QDataSet#MONOTONIC
     * @see Ops#ensureMonotonicAndIncreasingWithFill(org.das2.qds.QDataSet) 
     */
    public static boolean isMonotonicAndIncreasingQuick(QDataSet ds) {
        logger.entering( "DataSetUtil", "isMonotonicAndIncreasingQuick" );
        // put in reference to MONOTONIC for usage search.
        logger.finest("enter isMonotonicAndIncreasingQuick test for "+QDataSet.MONOTONIC);  
        if (ds instanceof IndexGenDataSet ) return true;
        if (ds.rank() == 1) {
            if (ds.length() < 100) {
                return DataSetUtil.isMonotonicAndIncreasing(ds);
            } else {
                QDataSet wds = DataSetUtil.weightsDataSet(ds);
                Random r = new Random(0);
                int i = 0;
                double last = -1.0 * Double.MAX_VALUE;
                int n = ds.length();
                int jump = n / 20;
                for (; i < n; i += (1 + r.nextInt(jump))) {
                    double d = ds.value(i);
                    double w = wds.value(i);
                    while (w == 0 && i < n) {
                        d = ds.value(i);
                        w = wds.value(i);
                        i++;
                    }
                    if (i == n) {
                        break;
                    }
                    if (d <= last) {
                        logger.exiting( "DataSetUtil", "isMonotonicAndIncreasingQuick", false );
                        return false;
                    }
                    last = d;
                }
                logger.exiting( "DataSetUtil", "isMonotonicAndIncreasingQuick", true );
                return true;
            }
        } else {
            logger.exiting( "DataSetUtil", "isMonotonicAndIncreasingQuick", false );
            return false;
        }
    }

    /**
     * perform a binary search for key within ds, constraining the search to between low and high.
     * @param ds a rank 1 monotonic dataset.
     * @param key the value to find.
     * @param low
     * @param high
     * @return a positive index of the found value or -index-1 the insertion point.
     */
    public static int binarySearch(QDataSet ds, double key, int low, int high) {
        while (low <= high) {
            int mid = (low + high) >> 1;
            double midVal = ds.value(mid);
            int cmp;
            if (midVal < key) {
                cmp = -1;   // Neither val is NaN, thisVal is smaller

            } else if (midVal > key) {
                cmp = 1;    // Neither val is NaN, thisVal is larger

            } else {
                long midBits = Double.doubleToLongBits(midVal);
                long keyBits = Double.doubleToLongBits(key);
                cmp = (midBits == keyBits ? 0 : // Values are equal
                        (midBits < keyBits ? -1 : // (-0.0, 0.0) or (!NaN, NaN)
                        1));                     // (0.0, -0.0) or (NaN, !NaN)

            }

            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return mid;
            } // key found

        }
        return -(low + 1);  // key not found.

    }

    /**
     * return the index of the closest value in ds to d, using guess as a starting point.  This
     * implementation ignores guess, but wise clients will supply it as a parameter.
     * @param ds a rank 1, monotonic dataset.
     * @param d the value to find.
     * @param guess a guess at a close index, or -1 if no guess should be made.  In this case, a binary search is performed.
     * @return index of the closest.
     */
    public static int closest(QDataSet ds, double d, int guess) {
        int result = binarySearch(ds, d, 0, ds.length() - 1);
        if (result == -1) {
            result = 0; //insertion point is 0

        } else if (result < 0) {
            result = ~result; // usually this is the case

            if (result >= ds.length() - 1) {
                result = ds.length() - 1;
            } else {
                double x = d;
                double x0 = ds.value(result - 1);
                double x1 = ds.value(result);
                result = ((x - x0) / (x1 - x0) < 0.5 ? result - 1 : result);
            }
        }
        return result;
    }

    /**
     * return the "User" property, which allow for extensions of the data model that
     * aren't used.  This returns the property "name" under the name USER_PROPERTIES,
     * which must either be null or a Map&lt;String,Object&gt;.
     * @param ds  The dataset containing the property.
     * @param name  The name of the user property.
     * @return
     */
    public static Object getUserProperty( QDataSet ds, String name ) {
        Map<String,Object> userProps= (Map<String, Object>) ds.property(QDataSet.USER_PROPERTIES);
        if ( userProps==null ) return null;
        return userProps.get(name);
    }

    /**
     * Return the names of non-structural properties of the dataset, like the UNITS and CADENCE.
     * These are the dimensionProperties, plus others specific to the dataset, such as CADENCE and
     * DELTA_PLUS.
     * @return the names of non-structural properties of the dataset, like the UNITS and CADENCE.
     */
    public static String[] propertyNames() {
        return new String[]{
                    QDataSet.UNITS, 
                    QDataSet.VALID_MIN, QDataSet.VALID_MAX,
                    QDataSet.FILL_VALUE,
                    QDataSet.FORMAT, QDataSet.CADENCE,
                    QDataSet.MONOTONIC, QDataSet.SCALE_TYPE, QDataSet.AVERAGE_TYPE, 
                    QDataSet.TYPICAL_MIN, QDataSet.TYPICAL_MAX, QDataSet.RENDER_TYPE,
                    QDataSet.QUBE,
                    QDataSet.NAME, QDataSet.LABEL, QDataSet.TITLE, QDataSet.DESCRIPTION,
                    QDataSet.CACHE_TAG,
                    QDataSet.COORDINATE_FRAME,
                    QDataSet.DELTA_MINUS, QDataSet.DELTA_PLUS,
                    QDataSet.BIN_MINUS, QDataSet.BIN_PLUS,
                    QDataSet.BIN_MIN, QDataSet.BIN_MAX,
                    QDataSet.WEIGHTS,
                    QDataSet.USER_PROPERTIES,
                    QDataSet.NOTES,
                    QDataSet.METADATA, QDataSet.METADATA_MODEL,
                };
    }

    /**
     * Copy over all the dimension properties, including:
     *       UNITS, FORMAT, SCALE_TYPE,
     *       TYPICAL_MIN, TYPICAL_MAX,
     *       VALID_MIN, VALID_MAX, FILL_VALUE,
     *       NAME, LABEL, TITLE,
     *       USER_PROPERTIES
     * These are dimension properties, as opposed to structural
     * see dimensionProperties() for a list of dimension properties.
     * TODO: This DOES NOT support join datasets yet.
     * @param source
     * @param dest
     */
    public static void copyDimensionProperties( QDataSet source, MutablePropertyDataSet dest ) {
        String[] names= DIMENSION_PROPERTIES;
        for ( String n: names ) {
            Object p= source.property(n);
            if ( p!=null ) dest.putProperty( n, p );
        }
    }

    /**
     * copy over the render type, if it is still appropriate.  This nasty bit of code was introduced
     * to support LANL data, where high-rank data is preferably plotted as a spectrogram, but can be
     * plotted as a stack of lineplots.
     * @param source
     * @param dest
     */
    public static void maybeCopyRenderType( QDataSet source, MutablePropertyDataSet dest ) {
        String rt= (String) source.property(QDataSet.RENDER_TYPE);
        if ( rt==null ) return;
        if ( rt.equals("spectrogram") || rt.equals("nnSpectrogram") ) {
            if ( dest.rank()>1 ) dest.putProperty( QDataSet.RENDER_TYPE, rt );
        }
    }

    private static final String[] DIMENSION_PROPERTIES = new String[] {
        QDataSet.UNITS, QDataSet.FORMAT, QDataSet.SCALE_TYPE, QDataSet.AVERAGE_TYPE,
        QDataSet.TYPICAL_MIN, QDataSet.TYPICAL_MAX,
        QDataSet.VALID_MIN, QDataSet.VALID_MAX, QDataSet.FILL_VALUE,
        QDataSet.NAME, QDataSet.LABEL, QDataSet.TITLE,
        QDataSet.USER_PROPERTIES, QDataSet.NOTES,
    };
   
    /**
     * return the list of properties that pertain to the dimension that dataset
     * values exist.  These are the properties that survive through most operations.
     * For example, if you flattened the dataset, what properties 
     * would still exist?  If you shuffled the data?  These are not structural
     * properties like DEPEND_0, BUNDLE_1, etc.
     * Note that BUNDLE_1 will carry dimension properties as well.
     * @return
     */
    public static String[] dimensionProperties() {
        return Arrays.copyOf( DIMENSION_PROPERTIES,DIMENSION_PROPERTIES.length );
    }
    
    public static final String PROPERTY_TYPE_STRING="String";
    public static final String PROPERTY_TYPE_NUMBER="Number";
    public static final String PROPERTY_TYPE_BOOLEAN="Boolean";
    public static final String PROPERTY_TYPE_MAP="Map";
    public static final String PROPERTY_TYPE_QDATASET="QDataSet";
    public static final String PROPERTY_TYPE_CACHETAG="CacheTag";
    public static final String PROPERTY_TYPE_UNITS="Units";
    
    /**
     * return the class for the property, to support Jython.
     * @param name the property name, e.g. QDataSet.TITLE
     * @return String.class
     * @see #getPropertyType(java.lang.String) 
     * //TODO: super inefficient, this needs to be rewritten as switch
     */
    public static Class getPropertyClass( String name ) {
        if ( name.equals(QDataSet.TITLE) 
                || name.equals(QDataSet.LABEL) 
                || name.equals(QDataSet.BIN_MAX_NAME)
                || name.equals( QDataSet.BIN_MIN_NAME )
                || name.equals(QDataSet.BIN_PLUS_NAME)
                || name.equals(QDataSet.BIN_MINUS_NAME)
                || name.equals(QDataSet.DELTA_PLUS_NAME )
                || name.equals( QDataSet.DELTA_MINUS_NAME )
                || name.equals( QDataSet.DEPENDNAME_0 )
                || name.equals( QDataSet.DEPENDNAME_1 ) ) {
            return String.class;
        } else if (  name.equals(QDataSet.UNITS) ) {
            return Units.class;
        } else if (  name.equals(QDataSet.NAME) 
                || name.equals(QDataSet.FORMAT) 
                || name.equals(QDataSet.RENDER_TYPE) 
                || name.equals(QDataSet.SCALE_TYPE) ) {
            return String.class;
        } else if ( name.equals(QDataSet.TYPICAL_MIN) 
                || name.equals(QDataSet.TYPICAL_MAX) 
                || name.startsWith(QDataSet.VALID_MIN) 
                || name.startsWith(QDataSet.VALID_MAX) 
                || name.equals(QDataSet.FILL_VALUE) ) {
            return Number.class;
        } else if ( name.equals(QDataSet.MONOTONIC) 
                || name.equals(QDataSet.QUBE) ) {
            return Boolean.class;
        } else if ( name.equals(QDataSet.CACHE_TAG) ) {
            return CacheTag.class;
        } else if ( name.equals(QDataSet.USER_PROPERTIES) 
                || name.equals(QDataSet.METADATA) ) {
            return Map.class;
        } else if ( name.startsWith("JOIN_") 
                || name.startsWith("BINS_") ) {
            return String.class;
        } else if ( name.startsWith(QDataSet.SOURCE) 
                || name.startsWith(QDataSet.VERSION) 
                || name.equals(QDataSet.METADATA_MODEL) ) {
            return String.class;
        } else if ( name.equals(QDataSet.CADENCE) ) {
            return QDataSet.class;
        } else if ( name.startsWith("DEPEND_") 
                || name.startsWith("BUNDLE_") 
                || name.startsWith("DELTA_") 
                || name.startsWith("BIN_") 
                || name.startsWith("CONTEXT_")
                || name.startsWith("PLANE_" ) ) {
            return QDataSet.class;
        } else if ( name.equals(QDataSet.START_INDEX) ) {
            return Integer.class;
        } else {
            return null;
        }
    }
    /**
     * return the type of the property, as a string to support use in Jython:
     * String,Number,Boolean,Map,QDataSet,CacheTag,Units
     * @param name the property name
     * @return the property type or null if the name is not recognized
     * @see #getPropertyClass(java.lang.String) 
     * @see org.das2.qds.QDataSet
     */
    public static String getPropertyType( String name ) {
        switch (name) {
            case QDataSet.LABEL:
            case QDataSet.TITLE:
            case QDataSet.DESCRIPTION:
            case QDataSet.BIN_MAX_NAME:
            case QDataSet.BIN_MIN_NAME:
            case QDataSet.BIN_PLUS_NAME:
            case QDataSet.BIN_MINUS_NAME:
            case QDataSet.DELTA_PLUS_NAME:
            case QDataSet.DELTA_MINUS_NAME:
            case QDataSet.DEPENDNAME_0:
            case QDataSet.DEPENDNAME_1:
                return PROPERTY_TYPE_STRING;
            case QDataSet.UNITS:
                return PROPERTY_TYPE_UNITS;
            case QDataSet.NAME:
            case QDataSet.FORMAT:
            case QDataSet.RENDER_TYPE:
            case QDataSet.SCALE_TYPE:
            case QDataSet.AVERAGE_TYPE:
                return PROPERTY_TYPE_STRING;
            case QDataSet.TYPICAL_MIN:
            case QDataSet.TYPICAL_MAX:
            case QDataSet.VALID_MIN:
            case QDataSet.VALID_MAX:
            case QDataSet.FILL_VALUE:
                return PROPERTY_TYPE_NUMBER;
            case QDataSet.MONOTONIC:
            case QDataSet.QUBE:
                return PROPERTY_TYPE_BOOLEAN;
            case QDataSet.CACHE_TAG:
                return PROPERTY_TYPE_CACHETAG;
            case QDataSet.USER_PROPERTIES:
            case QDataSet.METADATA:
                return PROPERTY_TYPE_MAP;
            case QDataSet.CADENCE:
            case QDataSet.WEIGHTS:
                return PROPERTY_TYPE_QDATASET;
            case QDataSet.SOURCE:
            case QDataSet.VERSION:
            case QDataSet.METADATA_MODEL:
            case QDataSet.COORDINATE_FRAME:
                return PROPERTY_TYPE_STRING;
            default:
                break;
        }
               
        if ( name.startsWith("JOIN_") || name.startsWith("BINS_") ) {
            return PROPERTY_TYPE_STRING;
        } else if ( name.startsWith("DEPEND_") 
                || name.startsWith("BUNDLE_") 
                || name.startsWith("DELTA_") 
                || name.startsWith("BIN_") 
                || name.startsWith("CONTEXT_")
                || name.startsWith("PLANE_") ) {
            return PROPERTY_TYPE_QDATASET;
        } else {
            return null;
        }
    }

    /**
     * return true if the property name is a valid dimension property
     * like "UNITS" or "FORMAT".  See dimensionProperties().
     * @param name property name to test
     * @return true if the property is a dimension property.
     */
    public static boolean isDimensionProperty( String name ) {
        for ( String n: DIMENSION_PROPERTIES ) {
            if ( n.equals(name) ) return true;
        }
        return false;
    }
    
    private static final String[] GLOBAL_PROPERTIES= new String[] {
        QDataSet.USER_PROPERTIES, QDataSet.NOTES, QDataSet.VERSION, QDataSet.METADATA, QDataSet.METADATA_MODEL, QDataSet.SOURCE, 
    };

    /**
     * properties that describe the dataset itself, rather than those of a dimension
     * or structural properties.
     * @return the properties that describe the dataset itself
     */
    public static String[] globalProperties() {
        return Arrays.copyOf(GLOBAL_PROPERTIES,GLOBAL_PROPERTIES.length);        
    }

    
    private static final String[] CORRELATIVE_PROPERTIES= new String[] {
        QDataSet.DELTA_MINUS, QDataSet.DELTA_PLUS, 
        QDataSet.BIN_MINUS, QDataSet.BIN_PLUS, 
        QDataSet.BIN_MIN, QDataSet.BIN_MAX, 
        QDataSet.WEIGHTS,
    };
    
    /**
     * properties that go along with the zeroth index.  These are all QDataSets with dimensions compatible with the datasets.
     * If you trim the dataset, then these must be trimmed as well.
     * @return the properties that go along with the zeroth index
     */
    public static String[] correlativeProperties() {
        return Arrays.copyOf(CORRELATIVE_PROPERTIES,CORRELATIVE_PROPERTIES.length);
    }



    /**
     * true if the property is one that is global and is relevant throughout the
     * dataset, such as a title or the units.
     *    property( "TITLE",0,0 ) often returns property("TITLE"), but
     *    property( "DEPEND_0",0,0 ) should never return property("DEPEND_0").
     * This is false, for example, for DEPEND_1.
     * @param prop the property name.
     * @return true if the property is inherited
     */
    public static boolean isInheritedProperty( String prop ) {
        // QDataSet.MAX_RANK is equal to 4.
        switch (prop) {
            case QDataSet.DEPEND_0:
            case QDataSet.DEPEND_1:
            case QDataSet.DEPEND_2:
            case QDataSet.DEPEND_3:
                return false;
            case QDataSet.BUNDLE_0:
            case QDataSet.BUNDLE_1:
            case QDataSet.BUNDLE_2:
            case QDataSet.BUNDLE_3:
                return false;
            case QDataSet.BINS_0:
            case QDataSet.BINS_1:
                return false;
            case QDataSet.JOIN_0:
            case "JOIN_1":
                return false;
            case QDataSet.START_INDEX:
            case QDataSet.RENDER_TYPE:
                return false;
            default:
                break;
        }
        
        if ( Arrays.asList(CORRELATIVE_PROPERTIES).contains(prop) ) {
            return false;
        }
        
        boolean indexProp= prop.startsWith("PLANE_");
        // note CONTEXT* is inherited.
        //TODO: shouldn't DELTA_PLUS and DELTA_MINUS be on this list?
        return !indexProp;
    }

    /**
     * return properties attached to the slice at index.  Note the slice
     * implementations use this, and this only returns properties from
     * dimensionProperties().
     * 
     * http://autoplot.org//QDataSet#20150514
     * 
     * Note this does not look at BUNDLE_1 properties.  TODO: consider this.
     * 
     * @param ds the dataset to slice.
     * @param index index to slice at.
     * @param result a map to insert the new properties, or null if a new one should be created.
     * @return a map of properties attached to the slice at index
     */
    public static Map<String, Object> sliceProperties( QDataSet ds, int index, Map<String,Object> result ) {
        if ( result==null ) result= new LinkedHashMap<>();

        if ( ds.property(QDataSet.BUNDLE_0 )!=null ) {
            logger.fine("sliceProperties is not allowed when BUNDLE_0 is set");
            return result;
        }
        
        String[] names = DIMENSION_PROPERTIES; // no need to copy when we call dimensionProperties()
        for (String name : names) {
            Object val = ds.property(name, index);
            if (val != null) {
                result.put(name, val);
            }
        }

        return result;

    }

    /**
     * help out implementations of the QDataSet.trim() command.  This does the dimension properties
     * and geometric properties like DEPEND_0  and DELTA_PLUS.  This also
     * checks for indexed properties, which are NAME__i.
     * @param ds the dataset with properties to trim.
     * @param start start index of trim operation
     * @param stop exclusive stop index of the trim operation.
     * @return the properties of ds, trimmed to the indices.  
     */
    public static Map<String,Object> trimProperties( QDataSet ds, int start, int stop ) {

        Map<String,Object> result= new LinkedHashMap<>();
        result= getDimensionProperties(ds,result);
        
        if ( result.containsKey(QDataSet.TYPICAL_MIN) ) {
            result.put( QDataSet.TYPICAL_MIN, null );
            result.put( QDataSet.TYPICAL_MAX, null );
        }

        QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
        if ( dep0!=null ) {
            result.put( QDataSet.DEPEND_0, dep0.trim(start,stop) );
        }
        
        for ( int i=1; i<=QDataSet.MAX_RANK; i++ ) {
            String prop= "DEPEND_"+i;
            QDataSet dep= (QDataSet) ds.property(prop);
            if ( dep!=null ) {
                if ( dep.rank()>1 && !Schemes.isRank2Bins(dep) ) {
                    dep= dep.trim(start,stop);
                }
                result.put( prop, dep );
            }
        }

        QDataSet dsp;
        String [] props= DataSetUtil.correlativeProperties();
        for ( String s: props ) {
            dsp= (QDataSet) ds.property( s );
            if ( dsp!=null ) {
                if ( dsp.rank()>0 ) {
                    result.put( s, dsp.trim(start,stop) );
                } else {
                    result.put( s, dsp );
                }
            }
        }

        for ( int i=0; i<QDataSet.MAX_PLANE_COUNT; i++ ) {
            QDataSet p= (QDataSet) ds.property("PLANE_"+i);
            if ( p!=null ) {
                if ( p.rank()>0 ) result.put( "PLANE_"+i, p.trim(start,stop) ); else result.put("PLANE_"+i, p ); // note planes must be at least rank 1 right now.
            } else {
                break;
            }
        }

        if ( ds.length()<QDataSet.MAX_PLANE_COUNT ) { 
            //kludge for indexed properties.
            for ( int i=0; i<stop-start; i++ ) {
                int ips= i+start;
                Object o= ds.property( QDataSet.NAME, ( ips ) );
                if ( o!=null ) result.put( "NAME__"+i, o );
                o= ds.property( QDataSet.UNITS, ( ips ) );
                if ( o!=null ) result.put( "UNITS__"+i, o );            
                o= ds.property( QDataSet.FORMAT, ( ips ) );
                if ( o!=null ) result.put( "FORMAT__"+i, o );            
            }
        }
        return result;
        
    }

    /**
     * return just the properties attached to the dataset, like 
     * UNITS and SCALE_TYPE, and not like DEPEND_x, etc.
     * @see #dimensionProperties()
     * @param ds the dataset
     * @param def default values or null.
     * @return a map of all the properties.
     */
    public static Map<String,Object> getDimensionProperties( QDataSet ds, Map<String,Object> def ) {
        return getProperties( ds, DIMENSION_PROPERTIES, def );
    }
    
    /**
     * return the properties listed, using the defaults if provided.
     * See dimensionProperties(), globalProperties().
     * @param ds dataset source of the properties.
     * @param names array of names
     * @param def defaults, or null if no defaults are to be used.
     * @return map of the properties.
     */
    public static Map<String,Object> getProperties( QDataSet ds, String[] names, Map<String,Object> def ) {
        if ( def==null ) {
            def= new LinkedHashMap<>();
        } else {
            def= new LinkedHashMap<>( def );
        }

        for (String name : names) {
            Object val = ds.property(name);
            if (val != null) {
                def.put(name, val);
            }
        }

        return def;
    }

    /**
     * gets all the properties of the dataset.  This is a shallow
     * copy of properties.
     * @param ds the dataset
     * @param def an empty map.
     * @return the properties.
     */
    public static Map<String, Object> getProperties(QDataSet ds, Map<String, Object> def) {
        Map<String,Object> result = def;

        for (int i = 0; i <= ds.rank(); i++) {
            Object dep = ds.property("DEPEND_" + i);
            if (dep != null) {
                result.put("DEPEND_" + i, dep);
            }
        }

        for (int i = 0; i <= ds.rank(); i++) {
            Object dep = ds.property("BUNDLE_" + i);
            if (dep != null) {
                result.put("BUNDLE_" + i, dep);
            }
        }

        for (int i = 0; i <= ds.rank(); i++) {
            Object dep = ds.property("BINS_" + i);
            if (dep != null) {
                result.put("BINS_" + i, dep);
            }
        }

        for (int i = 0; i <= ds.rank(); i++) {
            Object dep = ds.property("JOIN_" + i);
            if (dep != null) {
                result.put("JOIN_" + i, dep);
            }
        }

        for (int i = 0; i < QDataSet.MAX_PLANE_COUNT; i++) {
            Object plane = ds.property("PLANE_" + i);
            if (plane != null) {
                result.put("PLANE_" + i, plane);
            } else {
                break;
            }
        }

        for (int i = 0; i < QDataSet.MAX_PLANE_COUNT; i++) {
            Object cds = ds.property("CONTEXT_" + i);
            if (cds != null) {
                result.put("CONTEXT_" + i, cds);
            } else {
                break;
            }
        }

        String[] names = propertyNames();

        for (String name : names) {
            if (ds.property(name) != null) {
                result.put(name, ds.property(name));
            }
        }

        return result;

    }

    /**
     * gets all the properties of the dataset.  This is a shallow
     * copy of properties.
     * @param ds the dataset
     * @return the properties
     * @see #putProperties(java.util.Map, org.das2.qds.MutablePropertyDataSet) 
     */
    public static Map<String, Object> getProperties(QDataSet ds) {
        return getProperties(ds, new LinkedHashMap<>());
    }

    /**
     * copy all properties into the dataset by iterating through the map.  Properties
     * that are equal to null are not copied, since null is equivalent to the
     * property not found.
     * @param properties the properties
     * @param ds the mutable property dataset, which is still mutable.
     * @see #getProperties(org.das2.qds.QDataSet) 
     */
    public static void putProperties(Map<String, Object> properties, MutablePropertyDataSet ds) {
        if ( ds.isImmutable() ) {
            logger.warning( "ds is immutable, an exception will be thrown.");
        }
        properties.entrySet().forEach((e) -> {
            String k= e.getKey();
            Object v= e.getValue();
            boolean m= v instanceof Map;
            if ( m ) {
                if ( k.startsWith("DEPEND_") ) {
                    QDataSet dep= (QDataSet) ds.property(k);
                    if ( dep instanceof MutablePropertyDataSet ) {
                        MutablePropertyDataSet mdep= (MutablePropertyDataSet)dep;
                        putProperties( (Map<String,Object>)v, mdep );
                    }
                } else if ( k.startsWith("PLANE_") ) {
                    QDataSet dep= (QDataSet) ds.property(k);
                    if ( dep instanceof MutablePropertyDataSet ) {
                        MutablePropertyDataSet mdep= (MutablePropertyDataSet)dep;
                        putProperties( (Map<String,Object>)v, mdep );
                    }
                } else if ( k.startsWith("BUNDLE_") ) {
                    QDataSet dep= (QDataSet) ds.property(k);
                    if ( dep instanceof MutablePropertyDataSet ) {
                        MutablePropertyDataSet mdep= (MutablePropertyDataSet)dep;
                        putProperties( (Map<String,Object>)v, mdep );
                    }
                } else if ( k.startsWith("CONTEXT_") ) {
                    QDataSet dep= (QDataSet) ds.property(k);
                    if ( dep instanceof MutablePropertyDataSet ) {
                        MutablePropertyDataSet mdep= (MutablePropertyDataSet)dep;
                        putProperties( (Map<String,Object>)v, mdep );
                    }
                } else {
                    if ( v!=null ) ds.putProperty( k, v );
                }
            } else {
                if ( v!=null ) ds.putProperty( k, v );
            }
        });
    }
    
    /**
     * return true if the data is not a bundle and has uniform units throughout 
     * the dataset.
     * @param ds any dataset.
     * @return true if the data is not a bundle and has uniform units throughout 
     */
    public static boolean isNotBundle( QDataSet ds ) {
        switch ( ds.rank() ) {
            case 0: return true;
            case 1: return ds.property(QDataSet.BUNDLE_0)==null;
            case 2: return ds.property(QDataSet.BUNDLE_1)==null;
        }
        if ( ds.length()==0 ) return true;
        if ( ds.property(QDataSet.JOIN_0)!=null ) {
            Units u= SemanticOps.getUnits(ds.slice(0));
            for ( int i=1; i<ds.length(); i++ ) {
                Units u1= SemanticOps.getUnits(ds.slice(i));
                if ( u1!=u ) return false; // Das2Streams can join data with differing but converible units.
            }
        }
        return true;
    }
    
    /**
     * cleans up code by doing the cast, and handles default value.  The
     * result of this is for human-consumption!
     *
     */
    //public static <T> getProperty( QDataSet ds, String propertyName, Class<T> clazz, Object<T> defaultValue ) {
    //T p = ds.property( propertyName );
    //if ( p==null ) p= defaultValue;
    //return p;
    //ArrayList o;
    //}*/
     
    /**
     * provide a string representation of the dataset.  This is intended for
     * human consumption, but does follow rules outlined in 
     * http://autoplot.org//developer.datasetToString
     * 
     * @param ds any dataset.
     * @return a short, human-readable representation of the dataset.
     * @see #format(org.das2.qds.QDataSet, boolean) 
     */
    public static String toString(QDataSet ds) {

        if ( ds==null ) {
            throw new IllegalArgumentException( "null dataset" );
        }
        Units u= (Units)ds.property(QDataSet.UNITS);
        if ( u==null ) {
            if ( ds.property(QDataSet.JOIN_0)!=null && ds.length()>0 ) {
                u= (Units)ds.property(QDataSet.UNITS,0);
            }
            if ( u==null) u= Units.dimensionless;
        }
        
        String name = (String) ds.property(QDataSet.NAME);
        if (name == null) {
            name = "dataset";
        }

        if ( ds.rank()==0 ) {
            try {
                Datum d= DataSetUtil.asDatum(ds);
                if ( name.equals("dataset") ) {
                    return String.valueOf( d );
                } else {
                    return name + "=" + d ;
                }
            } catch ( IllegalArgumentException ex ) {
                return "Error: "+ex;
            }
        }
        
        if ( Schemes.isPolyMesh(ds) ) {
            return name + "[polyMesh of " + ds.slice(1).length() + " polygons]";
        }
        
        if ( ds.rank()==1 && QDataSet.VALUE_BINS_MIN_MAX.equals(ds.property(QDataSet.BINS_0)) ) {
            if (  ds.value(0) <= ds.value(1) ) {
                if ( u!=Units.dimensionless ) {
                    DatumRange dr= new DatumRange( ds.value(0), ds.value(1), u );
                    return dr.toString();
                } else {
                    DatumRange dr= new DatumRange( Ops.datum(ds.slice(0)), Ops.datum(ds.slice(1)) );
                    return dr.toString();
                }
            } else {
                return String.format( "%s %s (invalid because BINS_0=min,max)", ds.slice(0), ds.slice(1) );
            }
        }

        if ( ds.rank()==1 && "min,maxInclusive".equals(ds.property(QDataSet.BINS_0)) ) {
            if (  ds.value(0) <= ds.value(1) ) {
                String f= (String)ds.property(QDataSet.FORMAT);
                if ( "%d".equals(f) ) {
                    String s= String.format( "%d to %d",(int)ds.value(0),(int)ds.value(1));
                    return s;
                } else {
                    DatumRange dr= new DatumRange( ds.value(0), ds.value(1), u );
                    String s= DatumRangeUtil.toStringInclusive( dr );
                    return s;
                }
            } else {
                return String.format( "%s %s (invalid because BINS_0=min,maxInclusive)", ds.slice(0), ds.slice(1) );
            }
        }
        
        if ( ds.rank()==1 && Schemes.isComplexNumbers(ds) ) {
            DecimalFormat df= new DecimalFormat("0.000E0");
            String rs= String.valueOf(ds.value(0));
            String is= String.valueOf(ds.value(1));
            if ( rs.length()>7 ) rs= df.format(ds.value(0));
            if ( is.length()>7 ) is= df.format(ds.value(1));
            return "(" + rs + "+" + is+"j)"; // Use "j" instead of "i" because Python does this.
        }

        if ( ds.rank()==1 && Ops.isLegacyBundle(ds) && ds.length()<8 ) { // introduced to support where or rank 2 dataset.
            QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
            StringBuilder str = new StringBuilder("");
            try {
                str.append( dep0.slice(0) ).append("=").append( ds.slice(0) );
            } catch ( RuntimeException ex ) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
                str.append("Exception");
            }
            for ( int i=1; i<ds.length(); i++ ) {
                str.append(", ").append( dep0.slice(i) ).append("=").append( ds.slice(i) );
            }
            return str.toString();
        }

        if ( ds.rank()==2 && ds.length()==2 && ds.length(0)==2 ) {
            QDataSet ex1= ds.slice(0);
            if ( "min,maxInclusive".equals(ex1.property( QDataSet.BINS_0) ) ) {
                Units u1= (Units) ds.property(QDataSet.UNITS,0);
                Units u2= (Units) ds.property(QDataSet.UNITS,1);

                DatumRange dr1= new DatumRange( ds.value(0,0), ds.value(0,1), u1==null ? Units.dimensionless : u1 );
                DatumRange dr2= new DatumRange( ds.value(1,0), ds.value(1,1), u2==null ? Units.dimensionless : u2 );
                return DatumRangeUtil.toStringInclusive( dr1 ) + "; "+ DatumRangeUtil.toStringInclusive( dr2 );
            }
        }

        if ( ds.rank()==2 && ds.length()==2 && ds.length(0)==2 ) {
            QDataSet ex1= ds.slice(0);
            if ( "min,max".equals(ex1.property( QDataSet.BINS_0 ) ) ) {
                Units u1= (Units) ds.property(QDataSet.UNITS,0);
                Units u2= (Units) ds.property(QDataSet.UNITS,1);

                DatumRange dr1= new DatumRange( ds.value(0,0), ds.value(0,1), u1==null ? Units.dimensionless : u1 );
                DatumRange dr2= new DatumRange( ds.value(1,0), ds.value(1,1), u2==null ? Units.dimensionless : u2 );
                return dr1.toString() + "; "+ dr2.toString();
            }
        }

        String qubeStr = DataSetUtil.isQube(ds) ? "" : "*";

        String[] depNames = new String[ds.rank()];

        for (int i = 0; i < depNames.length; i++) {
            depNames[i] = "";
            Object dep0o= ds.property("DEPEND_" + i);
            if ( dep0o!=null ) {
                String dname;
                if ( dep0o instanceof QDataSet ) {
                    QDataSet dep0 = (QDataSet) dep0o;
                    dname = (String) dep0.property(QDataSet.NAME);
                } else {
                    dname= String.valueOf(dep0o) + "(Str)";
                }
                if (dname != null) {
                    if (dname.length() > 6) {
                        dname = dname.substring(0, 6) + "...";
                    }
                    depNames[i] = dname + "=";
                } else {
                    depNames[i] = "DEPEND_"+i+"=";
                }
            }
        }

        if ( ds.property(QDataSet.BINS_0)!=null ) {
            depNames[0]= ((String)ds.property(QDataSet.BINS_0)).replaceAll(","," ");
        }

        if ( ds.property(QDataSet.BINS_1)!=null ) {
            depNames[1]= ((String)ds.property(QDataSet.BINS_1)).replaceAll(","," ");
        }

        if ( ds.property(QDataSet.JOIN_0)!=null ) {
            //don't add anything to this.  ds[8,time=50*,freq=20*]
        }

        boolean isBundle= false;
        Units bundleUnits= null; // if all bundled data has the same unit, then indicate the unit (for example Bz,By,Bz in nT)
        
        QDataSet bds= (QDataSet) ds.property(QDataSet.BUNDLE_0);
        
        if ( bds!=null && ( depNames[0].length()==0 || depNames[0].equals("DEPEND_0=") ) ) {
            depNames[0]= "BUNDLE_0=";
            isBundle= true;
        }

        bds= (QDataSet) ds.property(QDataSet.BUNDLE_1);
        if ( bds!=null && ( depNames[1].length()==0 || depNames[1].equals("DEPEND_1=") )  ) {
            depNames[1]= "BUNDLE_1=";    // TODO: consider  ds[time=1440,density,b_gsm=5] vs ds[time=1440,BUNDLE_1=5]
            isBundle= true;
        }

        if ( bds!=null ) {
            Units bu= (Units) bds.property(QDataSet.UNITS,0);
            for ( int i=1; bu!=null && i<bds.length(); i++ ) {
                if ( bu!=(Units) bds.property(QDataSet.UNITS,i) ) {
                    bu= null;
                }
            }
            bundleUnits= bu;
        }
        
        int[] qubeDims;
        if ( DataSetUtil.isQube(ds) ) {
            try {
                qubeDims= DataSetUtil.qubeDims(ds);
            } catch ( RuntimeException ex ) {
                logger.log( Level.SEVERE, null, ex );
                qubeDims= new int[] { 0,0,0,0,0,0,0,0,0 };
            }
        } else {
            qubeDims= new int[ ds.rank() ];
            qubeDims[0]= ds.length();
            if ( ds.rank() > 1) qubeDims[1]= ds.length(0);
            if ( ds.rank() > 2) qubeDims[2]= ds.length(0,0);
            if ( ds.rank() > 3) qubeDims[3]= ds.length(0,0,0);
        }

        StringBuilder dimStr = new StringBuilder("" + depNames[0] + ds.length());
        for ( int i=1; i<ds.rank(); i++ ) {
            if ( depNames[i].length()==0 || depNames[i].endsWith("=") ) {
                dimStr.append(",").append(depNames[i]).append(qubeDims[i]).append(qubeStr);
            } else {
                dimStr.append(",").append(depNames[i]);
            }
        }
        
        String su = String.valueOf(u);
        if ( su.equals("")) {
            su = "dimensionless";
        }
        
        if ( isBundle ) {
            if ( bundleUnits!=null ) {
                return name + "[" + dimStr.toString() + "] (" + bundleUnits + ")";
            } else {
                return name + "[" + dimStr.toString() + "]";
            }
        } else {
            return name + "[" + dimStr.toString() + "] (" + su + ")";
        }
       
    }

    /**
     * returns the first valid point found in a dataset, or null if
     * no such point is found.
     * @param ds non-bundle dataset.
     * @return rank zero dataset containing the first valid point, or null.
     */
    public static QDataSet firstValidPoint( QDataSet ds ) {
        Units u= (Units) ds.property(QDataSet.UNITS);
        if ( u==null ) {
            u= Units.dimensionless;
        }

        double offset= u.getFillDouble();

        QDataSet wds= DataSetUtil.weightsDataSet(ds);
        DataSetIterator iter= new QubeDataSetIterator(ds);

        while( iter.hasNext() ) {
            iter.next();
            double w= iter.getValue(wds);
            if ( w>0 ) {
                offset= iter.getValue(ds);
                break;
            }
        }

        if ( offset==u.getFillDouble() ) {
            return null;
        } else {
            return DataSetUtil.asDataSet(offset, u);
        }

    }

    /**
     * return just the valid points of the dataset.
     * @param ds a dataset rank &gt; 0.
     * @return the valid points of the dataset in a rank 1 dataset.
     */
   public static QDataSet validPoints( QDataSet ds ) {

        int lenmax= DataSetUtil.totalLength(ds);

        DDataSet result= DDataSet.createRank1(lenmax);
        int i=0;

        QDataSet wds= DataSetUtil.weightsDataSet(ds);
        DataSetIterator iter= new QubeDataSetIterator(ds);

        while( iter.hasNext() ) {
            iter.next();
            double w= iter.getValue(wds);
            if ( w>0 ) {
                result.putValue( i, iter.getValue(ds) );
                i=i+1;
            }
        }

        for ( String s: propertyNames() ) {
            result.putProperty( s, ds.property(s) );
        }

        return result;

    }

    /**
     * return the greatest common divisor, which is the unit for which 
     * all elements in the dataset are integer multiples of the result.
     * This works on continuous data, however, so limit is used to determine 
     * the fuzz allowed.  TODO: this needs review and is not for production use.
     * @param ds any dataset
     * @param d rank 0 dataset, first factor for the dataset, error is used to detect non-zero significance.
     * @param limit rank 0 dataset, the resolution for which data is considered equal, and this
     * limit should be greater than numerical precision.
     * @throws IllegalArgumentException if there is no valid data.
     * @return the greatest common divisor.
     * 
     */
   public static QDataSet gcd( QDataSet ds, QDataSet d, QDataSet limit ) {
        QDataSet r, hist, peaks;

        do {

            r= Ops.mod( ds, d );
            hist= Ops.autoHistogram(r);

//            try {
//                new AsciiFormatter().formatToFile( "/tmp/hist.dat", (QDataSet)hist.property( QDataSet.DEPEND_0 ), hist );
//            } catch (IOException ex) {
//                Logger.getLogger(DataSetUtil.class.getName()).log(Level.SEVERE, null, ex);
//            }
            peaks= AutoHistogram.peaks(hist);
            if ( peaks.length()==1 && peaks.slice(0).value()==0. ) { // clearly since we divide everything exactly, this is the GCD.
                return d;
            }

            // stop is stopping condition tolerance.
            double stop= ( d.property(QDataSet.DELTA_MINUS)!=null ) ?  ((QDataSet)d.property(QDataSet.DELTA_MINUS)).value() : 0.0;
            stop= Math.max( stop, DataSetUtil.asDatum(limit).doubleValue( SemanticOps.getUnits( peaks ) ) );
            double top= d.value() - stop;

            int nonZeroPeakIndex= ( peaks.value(0) - stop < 0.0 ) ? 1 : 0;
            int lastNonZeroPeakIndex= peaks.length()-1;

            while ( lastNonZeroPeakIndex>=0 && ( peaks.value(lastNonZeroPeakIndex) > top ) ) {
                lastNonZeroPeakIndex--;
            }

            if ( lastNonZeroPeakIndex < nonZeroPeakIndex ) {
                break;
            } else {
                d= peaks.slice( nonZeroPeakIndex );
            }
            
            if ( d.value()==0.0 ) {
                //throw new IllegalArgumentException("things have gone wrong again, where d becomes zero");
                logger.fine("things have gone wrong again, where d becomes zero");
            }
            

        } while ( true );

        return d;
   }

    /**
     * return the greatest common divisor, which is the unit for which 
     * all elements in the dataset are integer multiples of the result.
     * This works on continuous data, however, so limit is used to determine 
     * the fuzz allowed.  TODO: this needs review and is not for production use.
     * @param ds any dataset
     * @param limit the resolution for which data is considered equal.  The result
     * will be an integer multiple of this.
     * @throws IllegalArgumentException if there is no valid data.
     * @return the greatest common divisor.
     */
    public static QDataSet gcd( QDataSet ds, QDataSet limit ) {
        if ( ds.rank()!=1 ) {
            throw new IllegalArgumentException("dataset must be rank 1");
        }
        if ( limit.rank()!=0 || limit.value()<=0 ) {
            throw new IllegalArgumentException("limit must be rank 0 and positive");
        }
        if ( !SemanticOps.getUnits(ds).isConvertibleTo(SemanticOps.getUnits(limit) ) ) {
            throw new IllegalArgumentException("limit must be in the same units as ds");
        }
        QDataSet ds1= validPoints(ds);
        if ( ds1.length()==0 ) throw new IllegalArgumentException("no valid points");
        //if ( ds1.length()==1 ) return  DataSetOps.slice0( ds, 0 );
        if ( ds1.length()==1 ) return ds.slice( 0 );
        //QDataSet guess= DataSetOps.slice0( ds, 1 );
        int i0= 1;
        QDataSet guess= ds.slice( i0 );
        while ( Ops.lt(guess,limit).value()>0 && i0<(ds.length()-1) ) {
            i0++;
            guess= ds.slice( i0 );
        }
        //try {
            return gcd( ds, guess, limit );
//        } catch ( IndexOutOfBoundsException ex ) {
//            System.err.println("# demo bug in gcd");
//            System.err.println("limit="+limit);
//            System.err.println("ds=["+ds.value(0)+",");
//            for ( int i=0; i<ds.length(); i++ ) {
//                System.err.println("    "+ds.value(i)+",");
//            }
//            System.err.println("   ]");
//            throw ex;
//        }
    }

    /**
     * return the courser cadence of the two cadences.  Das2's AverageTableRebinner needs to get the coursest of all the ytags.
     * @param yTagWidth0 rank 0 dataset that is one cadence (e.g. 84 sec)
     * @param yTagWidth1 rank 0 dataset that is the second cadence (e.g. 70 sec)
     * @return the courser of the two cadences.  (e.g. 84 sec)
     */
    public static RankZeroDataSet courserCadence( RankZeroDataSet yTagWidth0, RankZeroDataSet yTagWidth1 ) {
        if ( yTagWidth0==null || yTagWidth1==null ) return null;
        if ( "log".equals( yTagWidth0.property( QDataSet.SCALE_TYPE ) ) == "log".equals( yTagWidth1.property( QDataSet.SCALE_TYPE ) ) ) {
             if ( DataSetUtil.asDatum(yTagWidth1).gt( DataSetUtil.asDatum(yTagWidth0) ) ) {
                 return yTagWidth1;
             } else {
                 return yTagWidth0;
             }
        } else  {
            if ( "log".equals( yTagWidth0.property( QDataSet.SCALE_TYPE ) ) ) {
                return yTagWidth0;
            } else {
                return yTagWidth1;
            }
        }
    }
    
    /**
     * Make a unicode spark line http://www.ssec.wisc.edu/~tomw/java/unicode.html.
     * This should be for human consumption, because future versions may include data
     * reduction and doubling up characters.
     * See commented code in MetadataPanel.histStr. (I knew I had done this before...)
     * @param ds the rank N (typically 1) dataset
     * @param extent None or the range, see Ops.extent(ds)
     * @param bar true indicates bars should be used instead of scatter
     * @return string that is a sparkline.
     */
    public static String toSparkline( QDataSet ds, QDataSet extent, boolean bar ) {
        if ( ds.length()>1000 ) {
            throw new IllegalArgumentException("dataset is too large (ds.length()>1000)");
        }
        if ( extent==null ) extent= Ops.extent(ds);
        //String charsScatter= "\u2840\u2804\u2802\u2801"; //\u2800  is blank
        String charsScatter= "\u28C0\u2824\u2812\u2809"; //\u2800  is blank
        String charsBar= "\u2581\u2582\u2583\u2584\u2585\u2586\u2587\u2588";
        String bb= bar ? charsBar : charsScatter;
        int maxn= bb.length();
        StringBuilder build= new StringBuilder(DataSetUtil.totalLength(ds));
        QubeDataSetIterator it= new QubeDataSetIterator(ds);
        QDataSet wds= DataSetUtil.weightsDataSet(ds);
        double min= extent.value(0);
        double range= extent.value(1)-min;
        while ( it.hasNext() ) {
            it.next();
            if ( it.getValue(wds)>0 ) {
                int n= (int)( maxn * ( it.getValue(ds) - min ) / range );
                if ( bar ) n= Math.max( 0, Math.min( n, (maxn-1) ) );
                if ( n>=0 && n<maxn ) {
                    char c= bb.charAt( n );
                    build.append( c );
                } else {
                    build.append( "\u2800" );
                }
            }
        }
        return build.toString();
    }
    
    /**
     * true if the two datasets appear to be from the same population.
     * @param ds1 first dataset
     * @param ds2 second dataset
     * @return true if the two datasets appear to be from the same population
     */
    public static boolean samePopulation( QDataSet ds1, QDataSet ds2 ) {
        RankZeroDataSet stats1 = DataSetOps.moment(ds1);
        RankZeroDataSet stats2 = DataSetOps.moment(ds2);
        QDataSet stddev1= (QDataSet) stats1.property("stddev");
        QDataSet stddev2= (QDataSet) stats2.property("stddev");
        Units u1= SemanticOps.getUnits( stats1 );
        Units u2= SemanticOps.getUnits( stats2 );
        DatumRange dr1= DatumRange.newDatumRange( Ops.subtract( stats1,stddev1 ).value(), Ops.add( stats1,stddev1 ).value(), u1 );
        DatumRange dr2= DatumRange.newDatumRange( Ops.subtract( stats2,stddev2 ).value(), Ops.add( stats2,stddev2 ).value(), u2 );
        if ( dr1.width().value()>0 ) dr1= DatumRangeUtil.rescale( dr1, 0.2, 0.8 );
        if ( dr2.width().value()>0 ) dr2= DatumRangeUtil.rescale( dr2, 0.2, 0.8 );
        return dr1.intersects(dr2);
    }
    
    /**
     * return true if the data appears to have log spacing.  The 
     * data is assumed to be monotonically increasing or decreasing.
     * @param ds rank 1 dataset.
     * @return true if the data is roughly log spaced.
     */
    public static boolean isLogSpacing( QDataSet ds ) {
        if ( ds.rank()!=1 ) throw new IllegalArgumentException("rank 1 only");
        QDataSet c= (QDataSet) ds.property(QDataSet.CADENCE);
        if ( c!=null ) {
            return UnitsUtil.isRatiometric( SemanticOps.getUnits(ds) );
        }
        
        QDataSet lindiff= Ops.diff(ds);
        QDataSet logdiff= Ops.diff(Ops.log(ds));
        
        int l= lindiff.length();
        int h= l/2;
        
        if ( samePopulation( lindiff.trim(0,h), lindiff.trim(h,l) ) ) {
            return false;
        } else {
            return samePopulation( logdiff.trim(0,h),logdiff.trim(h,l) );
        }
    }
    
    /**
     * infer the bins for the rank 2 ytags.  This was first used with Juno
     * high-rate data, where the ytags follow the FCE implied by the magnetic 
     * field detector.
     * @param ydss rank 2 dataset[n,m]
     * @return two-element array of rank 2 datasets[n,m], where 0 is the min and 1 is the max.
     */
    public static QDataSet[] inferBinsRank2( QDataSet ydss ) {
        QDataSet lastYds= null;
        QDataSet lastYds0= null;
        QDataSet lastYds1= null;
        
        JoinDataSet yds0= new JoinDataSet(2);
        JoinDataSet yds1= new JoinDataSet(2);
        
        for ( int i=0; i<ydss.length(); i++ ) {
            QDataSet yds= ydss.slice(i);
            if ( lastYds!=null && Ops.equivalent(lastYds, yds) ) {
                yds0.join(lastYds0);
                yds1.join(lastYds1);
            } else {
                QDataSet rr= inferBins( yds );
                lastYds0= Ops.slice1( rr,0 );
                lastYds1= Ops.slice1( rr,1 );
                yds0.join(lastYds0);
                yds1.join(lastYds1);
                lastYds= yds;
            }
        }
        QDataSet[] result= new QDataSet[2];
        result[0]= yds0;
        result[1]= yds1;
        return result;
        
    }
    
    /**
     * This is the one code to infer bin boundaries when only the 
     * centers are available.  This uses centers of adjacent data, and
     * extrapolates to get the edge boundaries to create an acceptable limit.
     * When the data is log-spaced, the centers are done in the ratiometric
     * space.  Small (&lt;1000 element) datasets will be sorted if necessary.
     * @param yds rank 1 dataset.
     * @return rank 2 bins dataset.
     */
    public static QDataSet inferBins( QDataSet yds ) {
        if ( yds.rank()!=1 ) throw new IllegalArgumentException("yds must be rank 1");
        QDataSet yds0,yds1;
        QDataSet dy= DataSetUtil.guessCadenceNew( yds, null );
        if ( yds.length()<1000 && dy==null || dy.rank()>1 ) {
            QDataSet s= Ops.sort(yds);
            QDataSet yds_t= Ops.applyIndex( yds, s );
            yds_t= Ops.monotonicSubset(yds_t);
            dy= DataSetUtil.guessCadenceNew( yds_t, null );
        }
        Units dyu= dy==null ? null : SemanticOps.getUnits(dy);
        
        QDataSet delta;
        // check to see that dy is reasonable.
        if ( dy!=null ) {
            if ( UnitsUtil.isRatiometric( dyu ) ) {
                QDataSet diff1= yds.trim(0,yds.length()-1);    
                QDataSet diff2= yds.trim(1,yds.length());
                delta= Ops.log( Ops.divide(diff2,diff1) );
                delta= Ops.putProperty( delta, QDataSet.UNITS, Units.logERatio );
                delta= Ops.convertUnitsTo( delta, Units.percentIncrease );
                delta= Ops.interpolate( delta, Ops.linspace( -0.5,diff1.length()-0.5, diff1.length()+1 ) );
            } else {
                QDataSet diff1= yds.trim(0,yds.length()-1);
                QDataSet diff2= yds.trim(1,yds.length());
                delta= Ops.abs( 
                        Ops.interpolate( Ops.subtract(diff2,diff1), Ops.linspace( -0.5,diff1.length()-0.5, diff1.length()+1 ) ) );
            }
            QDataSet r= Ops.where( Ops.lt( delta, dy ) );
            if ( r.length()>yds.length()/2 ) {
                dy= null;
            }
        }
 
        if ( dy==null ) {
            if ( isLogSpacing( yds ) ) {
                QDataSet diff1= yds.trim(0,yds.length()-1);    
                QDataSet diff2= yds.trim(1,yds.length());
                delta= Ops.log( Ops.divide(diff2,diff1) );
                delta= Ops.putProperty( delta, QDataSet.UNITS, Units.logERatio );
                delta= Ops.convertUnitsTo( delta, Units.percentIncrease );
                delta= Ops.interpolate( delta, Ops.linspace( -0.5,diff1.length()-0.5, diff1.length()+1 ) );
                QDataSet v= Ops.divide( delta,100. );
                v= Ops.putProperty( v, QDataSet.UNITS, null );
                QDataSet ddy= Ops.sqrt( Ops.add( 1., v ) );
                yds0= Ops.divide( yds, ddy );
                yds1= Ops.multiply( yds, ddy );
            } else {
                QDataSet diff1= yds.trim(0,yds.length()-1);
                QDataSet diff2= yds.trim(1,yds.length());
                delta= Ops.abs( 
                        Ops.interpolate( Ops.subtract(diff2,diff1), Ops.linspace( -0.5,diff1.length()-0.5, diff1.length()+1 ) ) );
                delta= Ops.divide( delta, DataSetUtil.asDataSet(2) );
                yds0= Ops.subtract( yds, delta );
                yds1= Ops.add( yds, delta );
            }
        } else {
            if ( UnitsUtil.isRatiometric( SemanticOps.getUnits(dy) ) ) {
                dy= Ops.convertUnitsTo(dy, Units.percentIncrease );
                double ddy= Math.sqrt( 1. + dy.value()/100. );
                yds0= Ops.divide( yds, DataSetUtil.asDataSet(ddy) );
                yds1= Ops.multiply( yds, DataSetUtil.asDataSet(ddy) );
            } else {
                dy= Ops.divide( dy, DataSetUtil.asDataSet(2) );
                yds0= Ops.subtract( yds, dy );
                yds1= Ops.add( yds, dy );
            }
        }
        MutablePropertyDataSet mpds= (MutablePropertyDataSet)Ops.bundle( yds0, yds1 );
        mpds.putProperty( QDataSet.BINS_1, "min,max" );
        return mpds;
    }
    
    /**
     * return the cadence between measurements of a waveform dataset.  This is
     * different than the cadence typically quoted, which is the cadence between
     * waveform records.
     * @param ds
     * @return the cadence, possibly null.
     */
    public static RankZeroDataSet getCadenceWaveform( QDataSet ds ) {
        RankZeroDataSet xlimit;
        if ( Schemes.isRank2Waveform(ds) ) {
            QDataSet offsets= (QDataSet)ds.property(QDataSet.DEPEND_1);
            if ( offsets.rank()==1 ) {
                xlimit= DataSetUtil.guessCadenceNew( offsets, null );
            } else {
                xlimit= DataSetUtil.guessCadenceNew( offsets.slice(0), null );
            }
        } else if ( Schemes.isRank3Waveform(ds) ) {
            xlimit= getCadenceWaveform(ds.slice(0));
            for ( int i=1; i<ds.length(); i++ ) {
                RankZeroDataSet xlimit1= getCadenceWaveform(ds.slice(i));
                if ( Ops.gt( xlimit1, xlimit ).value()==1. ) {
                    xlimit= xlimit1;
                }
            }
        } else {
            throw new IllegalArgumentException("data is not waveform");
        }
        return xlimit;
    }
    
    /**
     * returns a rank 0 dataset indicating the cadence of the dataset.  Using a
     * dataset as the result allows the result to indicate SCALE_TYPE and UNITS.
     * History:<ul>
     *    <li>2011-02-21: keep track of repeat values, allowing zero to be considered either mono increasing or mono decreasing
     *    <li>2011-02-21: deal with interleaved fill values, keeping track of last valid value.
     * </ul>
     * @param xds the x tags, which may not contain fill values for non-null result.
     * @param yds the y values, which if non-null is only used for fill values.  This
     *   is only used if it is rank 1.
     * @return null or the cadence in a rank 0 dataset.  The following may be
     *    properties of the result:<ul>
     *    <li>SCALE_TYPE  may be "log"
     *    <li>UNITS       will be a ratiometric unit when the SCALE_TYPE is log, and
     *       will be the offset unit for interval units like Units.t2000.
     *    </ul>
     */
    public static RankZeroDataSet guessCadenceNew( QDataSet xds, QDataSet yds) {
        
        Logger logger= LoggerManager.getLogger("qdataset.ops.guesscadence");
        
        logger.entering(CLASSNAME,"guessCadenceNew");
        
        Object o= xds.property( QDataSet.CADENCE );
//        
//        if ( o==null ) {
//            o= DataSetAnnotations.getInstance().getAnnotation( xds, DataSetAnnotations.ANNOTATION_CADENCE );
//        }
//        
        if ( yds!=null && yds.rank()>1 ) {
            if ( Schemes.isRank2Waveform(yds)) {// leverage that we have the timetag offsets, and we can look at the first waveform to guess the cadence.
                RankZeroDataSet r1= guessCadenceNew(xds,null);
                QDataSet dd= (QDataSet)yds.property(QDataSet.DEPEND_1);
                Datum rw= null;
                if ( dd.rank()==1 ) {
                    QDataSet ee= Ops.extent(dd);
                    rw= DataSetUtil.asDatum( Ops.subtract( ee.slice(1), ee.slice(0) ) );
                    
                } else {
                    for ( int i=0; i<dd.length(); i++ ) {
                        QDataSet ee= Ops.extent(dd.slice(0));
                        Datum t1= DataSetUtil.asDatum( Ops.subtract( ee.slice(1), ee.slice(0) ) );
                        rw= rw==null ? t1 : ( rw.lt(t1) ? t1 : rw );
                    }
                }
                if ( rw==null ) { // rank 2 offsets.
                    return r1;
                } else {
                    if ( r1==null ) {
                        return DataSetUtil.asDataSet(rw);
                    } else {
                        Datum rt=  DataSetUtil.asDatum(r1);
                        if ( rw.getUnits().isConvertibleTo(rt.getUnits()) && rw.multiply(2.0).gt( rt ) ) {
                            return r1;
                        } else {
                            return DataSetUtil.asDataSet(rw);
                        }
                    }
                }
            } else if ( Schemes.isRank3Waveform(yds) ) {
                Datum dresult=null;
                for ( int i=0; i<yds.length(); i++ ) {
                    QDataSet yds1= yds.slice(i);
                    QDataSet xds1= (QDataSet)yds1.property(QDataSet.DEPEND_0);
                    QDataSet cadence= guessCadenceNew(xds1,yds1);
                    if ( cadence!=null ) {
                        Datum t1= DataSetUtil.asDatum( cadence );
                        dresult= dresult==null ? t1 : ( dresult.lt(t1) ? t1 : dresult );
                    }
                }
                return DataSetUtil.asDataSet(dresult);
            }
        }

        Units u= SemanticOps.getUnits(xds);

        if ( UnitsUtil.isNominalMeasurement(u) ) return null;
        
        if ( o!=null && o instanceof QDataSet ) {
            QDataSet q= (QDataSet)o;
            Units qu= SemanticOps.getUnits(q);
            if ( UnitsUtil.isRatiometric(qu) || qu.isConvertibleTo( u.getOffsetUnits() ) ) {
                if ( q.rank()==0 ) {    
                    if ( q instanceof RankZeroDataSet ) {
                        return (RankZeroDataSet) q;
                    } else {
                        logger.exiting(CLASSNAME,"guessCadenceNew");                    
                        return DRank0DataSet.create(q.value(),qu);
                    }
                
                } else {
                    while ( q.rank()>0 ) {
                        logger.log( Level.SEVERE, "averaging CADENCE rank 0: {0}", q);
                        q= Ops.reduceMax( q, 0 );
                    }
                    logger.exiting(CLASSNAME,"guessCadenceNew");                    
                    return DRank0DataSet.create( DataSetUtil.asDatum(q) );
                }
            } else {
                logger.log(Level.INFO, "CADENCE units ({0}) are inconvertible to {1}", new Object[]{qu, u.getOffsetUnits() }); // bugfix: offset units should be reported.
            }
        }
        
        if ( xds.length()>=100000000 && xds instanceof IndexGenDataSet  ) {
            logger.fine("removing guessCadenceCheck for huge dataset");
            return DRank0DataSet.create(1.0);
        }

        if (yds == null) {
            yds = DataSetUtil.replicateDataSet(xds.length(), 1.0);
        } else {
            if ( xds.length()!=yds.length() ) {
               throw new IllegalArgumentException("xds.length()!=yds.length()");
            }
            
            if ( yds.rank()==1 ) {
                // if yds is actually found to be another set of tags, then look for cadence in x based on this.
                int i= repeatingSignal( yds );
                if ( i>0 && i<yds.length()/2 ) {
                    xds= Ops.decimate( xds, i );
                    yds= Ops.decimate( yds, i );
                }
            }
        }
        
        if ( yds.rank()>1 ) { //TODO: check for fill columns.  Note the fill check was to support a flakey dataset.
            yds = DataSetUtil.replicateDataSet(xds.length(), 1.0);
        }

        if ( xds.length()<2 ) {
            logger.exiting(CLASSNAME,"guessCadenceNew");                    
            return null;
        }

        if ( xds.rank()==2 && xds.property(QDataSet.BINS_1)!=null ) {
            xds= DataSetOps.slice1( xds, 0 );
        }

        // Do initial scans of the data to check for monotonic decreasing and "ever increasing" spacing.
        double sp; // spacing between two measurements.
        double monoMag; // -1 if mono decreasing, 0 if not monotonic, 1 if mono increasing.

        QDataSet wds= DataSetUtil.weightsDataSet(xds);

        // check to see if spacing is monotonically increasing or decreasing, and has repeats.
        int monoDecreasing= 0;
        int monoIncreasing= 0;
        int count= 0;
        boolean xHasFill= false;
        int repeatValues= 0;
        double last= Double.NaN;
        for ( int i=0; i<xds.length(); i++ ) {
            if ( wds.value(i)==0 ) {
                xHasFill= true;
                continue;
            }
            if ( Double.isNaN(last) ) {
                last= xds.value(i);
                continue;
            }
            count++;
            sp= xds.value(i) - last;
            if ( sp<0. ) {
                monoDecreasing++;
            } else if ( sp>0. ) {
                monoIncreasing++;
            } else {
                repeatValues++;
            }
            last= xds.value(i);
        }
        if ( ( repeatValues + monoIncreasing ) >(90*count/100) ) { // 90% increasing
            monoMag= 1;
        } else if ( ( repeatValues + monoDecreasing ) >(90*count/100) ) { // 90% decreasing
            monoMag= -1;
        } else {
            monoMag= 0;
        }
        
        // don't allow datasets with fill in x to be considered.  
        if ( xHasFill && monoMag==0 ) {
            logger.exiting(CLASSNAME,"guessCadenceNew");
            return null;
        }
        if ( monoMag==0 ) {
            logger.exiting(CLASSNAME,"guessCadenceNew");                    
            return null;
        }
        
        double everIncreasing= 0.; // if non-zero, then the ratio of the last to the first number.
        
        if ( xds.length()<100 && xds.rank()==1 ) {
            LinFit f;
            f= new LinFit( Ops.findgen(xds.length()), xds );
            double chilin= f.getChi2();
            QDataSet r= Ops.where( Ops.gt( Ops.abs(xds), DataSetUtil.asDataSet(0) ) );
            if ( r.length()<2 ) {
                everIncreasing= 0;
            } else {
                QDataSet xdsr= DataSetOps.applyIndex( xds, 0, r, false );          // xdsr= xds[r]
                f= new LinFit( Ops.findgen(xdsr.length()), Ops.log(xdsr) );
                double chilog= f.getChi2();
                if ( chilog < ( chilin/2 ) ) {
                    QDataSet ext= Ops.extent(xdsr);
                    everIncreasing= ext.value(1)/ext.value(0);
                }
            }
            
        } else {
            if ( xds.length()>2 ) {
                // check to see if spacing is ever-increasing, which is a strong hint that this is log spacing.
                // everIncreasing is a measure of this.  When it is >0, it is the ratio of the last to the first
                // number in a ever increasing sequence.  Allow for one repeated length (POLAR/Hydra Energies)
                sp= monoMag * ( xds.value(2) - xds.value(0) );
                everIncreasing= xds.value(2) / xds.value(0);
                double sp0= sp;
                if ( xds.value(2)<=0 || xds.value(0)<=0 || xds.value(1)>(xds.value(0)+xds.value(2)) ) {
                    everIncreasing= 0;
                }
                for ( int i=3; everIncreasing>0 && i<xds.length(); i++ ) {
                    if ( wds.value(i)==0 || wds.value(i-3)==0 ) {
                        continue;
                    }
                    if ( xds.value(i)<=0 || xds.value(i-3)<=0 ) {
                        everIncreasing= 0;
                        continue;
                    }
                    double sp1= monoMag * ( xds.value(i) - xds.value(i-3) );
                    if ( sp1 > sp0*1.00001  ) {
                        everIncreasing= xds.value(i)/xds.value(0);
                        sp0= sp1;
                    } else {
                        everIncreasing= 0;
                    }
                }
            }
            if ( everIncreasing>0 && monoMag==-1 ) everIncreasing= 1/everIncreasing;
        }

        boolean logScaleType = "log".equals( xds.property(QDataSet.SCALE_TYPE) );

        QDataSet extent= Ops.extent(xds);

        AutoHistogram ah= new AutoHistogram();
        QDataSet diffs;
        if ( yds.rank()==1 && xds.rank()==1 ) { // ftp://virbo.org/tmp/poes_n17_20041228.cdf?P1_90[0:300] has every other value=fill.
            QDataSet r= Ops.where( Ops.valid(yds) );
            if ( r.length()<2 ) {
                diffs=  Ops.diff( xds );
            } else if ( r.length()==yds.length() ) {
                diffs=  Ops.diff( xds );
            } else {
                diffs=  Ops.diff( DataSetOps.applyIndex( xds, 0, r, false ) );
            }
        } else {
            diffs=  Ops.diff( xds );
        }
        if ( monoDecreasing>(9*count/10) ) {
            diffs= Ops.multiply( diffs, asDataSet(-1) );
        }
        
        if ( repeatValues>0 ) {
            QDataSet r= Ops.where( Ops.ne( diffs,DataSetUtil.asDataSet(0) ) );
            diffs= DataSetOps.applyIndex( diffs, 0, r, false );
        }

        if ( logger.isLoggable(Level.FINE) ) {
            logger.log(Level.FINE, "everIncreasing: {0}", everIncreasing);
            logger.log(Level.FINE, "xHasFill: {0}", xHasFill);
            logger.log(Level.FINE, "monoMag: {0}", monoMag);
            logger.log(Level.FINE, "logScaleType: {0}", logScaleType);
        }
        
        QDataSet hist= ah.doit( diffs ); 

        long total= (Long)( ((Map<String,Object>)hist.property( QDataSet.USER_PROPERTIES )).get(AutoHistogram.USER_PROP_TOTAL) );

        if ( total==0 ) {
            logger.exiting(CLASSNAME,"guessCadenceNew");
            return null;
        }
        
        // if the ratio of successive numbers is always increasing this is a strong
        // hint that ratiometric spacing is more appropriate.  If non-zero, then
        // this is the ratio of the first to the last number.
        final int everIncreasingLimit = total < 10 ? 5 : 10;

        int ipeak=0;
        int peakv=(int) hist.value(0);
        int linHighestPeak=0; // highest observed non-trivial peak

        int linMedian=-1;
        int t=0;

// TODO: do this some time.  A contour plot only has connections in one direction.
//        // look for negative cadence peak as well as positive
//        QDataSet dep0= (QDataSet) hist.property(QDataSet.DEPEND_0 );
//        double binWidth1= ((Number)((Map) hist.property(QDataSet.USER_PROPERTIES)).get(AutoHistogram.USER_PROP_BIN_WIDTH)).doubleValue();
//
//        int imin= -1;
//        for ( int i=0; i<hist.length(); i++ ) {
//            if ( hist.value(i)>0 ) {
//                imin= i;
//                break;
//            }
//        }
//        if ( dep0.value(imin) < binWidth1 ) {
//            return null;
//        }


        double mean= AutoHistogram.mean( hist ).value();

        int firstPositiveBin= Integer.MAX_VALUE;
        QDataSet dep0= (QDataSet) hist.property(QDataSet.DEPEND_0 );
        for ( int i=0; i<hist.length(); i++ ) {
            t+= hist.value(i);
            if ( hist.value(i)>peakv ) {
                ipeak= i;
                peakv= (int) hist.value(i);
            }
            if ( hist.value(i)>peakv/10.  ) {
                linHighestPeak= i;
            }
            if ( linMedian==-1 && t>total/2 ) {
                linMedian= i;
            }
            if ( dep0.value(i)>0 && firstPositiveBin==Integer.MAX_VALUE ) {
                firstPositiveBin= i;
            }
        }
        int linLowestPeak=0;
        for ( int i=0; i<hist.length(); i++ ) {
            if ( hist.value(i)>peakv/10. ) {
                linLowestPeak=i;
                break;
            }
        }

        Units xunits= (Units) xds.property( QDataSet.UNITS );
        if ( xunits==null ) xunits= Units.dimensionless;

        // we use the range of the bins to exclude log option, such as 800000-800010.
        boolean log= false;
        double firstBin= ((Number)((Map) hist.property(QDataSet.USER_PROPERTIES)).get(AutoHistogram.USER_PROP_BIN_START)).doubleValue();
        double binWidth= ((Number)((Map) hist.property(QDataSet.USER_PROPERTIES)).get(AutoHistogram.USER_PROP_BIN_WIDTH)).doubleValue();
        firstBin= firstBin - binWidth;  // kludge, since the firstBin left side is based on the first point.
  
        boolean bunch0= firstPositiveBin<Integer.MAX_VALUE && ipeak==firstPositiveBin && extent.value(0)-Math.abs(mean) < 0 && ( total<10 || firstBin<=0. );
        boolean isratiomeas= UnitsUtil.isRatioMeasurement(xunits);
        
        QDataSet logDiff= null;
        logger.log( Level.FINER, "consider log: isratio={0} allPositive={1} bunch0={2}", new Object[]{ isratiomeas, extent.value(0)>0, bunch0 });            
        if ( isratiomeas && extent.value(0)>0 &&
                ( logScaleType 
                || everIncreasing>everIncreasingLimit
                || bunch0 ) ) {
            ah= new AutoHistogram();
            QDataSet diffs2= Ops.diff(Ops.log(xds));
            logDiff= diffs2;
            QDataSet yy= DataSetUtil.weightsDataSet(yds);
            if ( repeatValues>0 ) {
                QDataSet r= Ops.where( Ops.ne( diffs2,DataSetUtil.asDataSet(0) ) );
                diffs2= DataSetOps.applyIndex( diffs2, 0, r, false );
                r= Ops.putProperty( r, QDataSet.VALID_MAX, null ); // rfe737: clear out the warning, since we know what we are doing.
                yy= DataSetOps.applyIndex( yy, 0, r, false );
            }
            QDataSet loghist= ah.doit( diffs2,yy ); //TODO: sloppy!
            // ltotal can be different than total.  TODO: WHY?  maybe because of outliers?
            long ltotal= (Long)( ((Map<String,Object>)loghist.property( QDataSet.USER_PROPERTIES )).get(AutoHistogram.USER_PROP_TOTAL) );
            int logPeak=0;
            int logPeakv=(int) loghist.value(0);
            int logMedian=-1;
            int logHighestPeak=0;
            t=0;

//            // check to see if they are all basically the same (one peak).
//            QDataSet logdep0= (QDataSet) loghist.property(QDataSet.DEPEND_0);
//            if ( ( ( logdep0.value(logdep0.length()-1) + logdep0.value(0) ) / 2 ) 
//                / ( logdep0.value(1)-logdep0.value(0) ) > 1e4 ) {
//                if ( )
//            }
//            
            //mean= AutoHistogram.mean(loghist).value();
            for ( int i=0; i<loghist.length(); i++ ) {
                t+= loghist.value(i);
                if ( loghist.value(i)>logPeakv ) {
                    logPeak=i;
                    logPeakv= (int) loghist.value(i);
                }
                if ( loghist.value(i)>logPeakv/100.  ) { // be loosy-goosey with log.
                   logHighestPeak= i;
                }
                if ( logMedian==-1 && t>ltotal/2 ) {
                    logMedian= i;
                }
            }
            //int logLowestPeak=0; // see commented code below
            //for ( int i=0; i<loghist.length(); i++ ) {
            //    if ( loghist.value(i)>logPeakv/10. ) {
            //        logLowestPeak=i;
            //        break;
            //    }
            //}

            int highestPeak= linHighestPeak;

            if ( everIncreasing>everIncreasingLimit || ( logPeak>1 && (1.*logMedian/loghist.length() > 1.*linMedian/hist.length() ) ) ) {
                logger.finer( String.format( "switch to log everIncreasing=%s logPeak=%s logMedianPerc=%5.1f linMedianPerc=%5.1f", everIncreasing, logPeak, 1.*logMedian/loghist.length(), 1.*linMedian/hist.length() ) );
                hist= loghist;
                ipeak= logPeak;
                peakv= logPeakv;
                highestPeak= logHighestPeak;
                log= true;
            } else {
                logger.finer( String.format( "stay linear everIncreasing=%s logPeak=%s logMedianPerc=%5.1f linMedianPerc=%5.1f", everIncreasing, logPeak, 1.*logMedian/loghist.length(), 1.*linMedian/hist.length() ) );            
            }

            if ( peakv<20 ) {
                ipeak= highestPeak;
                peakv= (int) hist.value(ipeak);
            } else if ( ipeak<logHighestPeak ) {
              //  if ( hist.value(logHighestPeak) > Math.max( Math.ceil( hist.value(logLowestPeak) / 10 ), 1 ) ) {
              //      ipeak= logHighestPeak;
              //      peakv= (int)hist.value(ipeak);
              //  }
            }

        } else if ( peakv<20 ) { // loosen things up when there isn't much data.
            ipeak= linHighestPeak;
            peakv= (int) hist.value(ipeak);
        } else if ( ipeak<linHighestPeak ) { // ftp://laspftp.colorado.edu/pub/riesberl/MMS/data/full-mode/tha_l2_esa_20080907_v01.cdf?tha_peif_sc_pot
            if ( hist.value(linHighestPeak) > Math.max( Math.ceil( hist.value(linLowestPeak) / 10. ), 1 ) ) {
                ipeak= linHighestPeak;
                peakv= (int)hist.value(ipeak);
            }
        }

        double ss=0;
        double nn=0;

        RankZeroDataSet theResult=null;
        boolean haveResult= false;
        
        QDataSet sss= (QDataSet) hist.property( QDataSet.PLANE_0 ); // DANGER--don't change PLANE_0!

        for ( int i=ipeak; i>=0; i-- ) {
            if ( hist.value(i)>(peakv/4.) ) {
                ss+= sss.value(i) * hist.value(i);
                nn+= hist.value(i);
            } else {
                break;
            }
        }

        for ( int i=ipeak+1; i<hist.length(); i++ ) {
            if ( hist.value(i)>(peakv/4.) ) {
                ss+= sss.value(i) * hist.value(i);
                nn+= hist.value(i);
            } else {
                break;
            }
        }

//        try {
//            // dump to text data file and figure out what's going on.
//            QDataSet bins= (QDataSet)hist.property(QDataSet.DEPEND_0);
//            new AsciiFormatter().formatToFile( 
//                    new java.io.File( String.format( "/tmp/ap/cadence_0038_%04d.dat", hist.length() ) ),
//                    bins, hist ) ;
//        } catch (IOException ex) {
//            logger.log(Level.SEVERE, null, ex);
//        }
//        
//        if ( haveResult && t<1000 ) {
//            // do something here
//        }
        
        // one last sanity check, for the PlasmaWaveGroup file:///home/jbf/project/autoplot/data/qds/gapBug/gapBug.qds?Frequency
        final int LIMIT_YTAGS_LENGTH_CHECK=160;
        if ( t<LIMIT_YTAGS_LENGTH_CHECK && log ) {
            double s= Math.abs( ss/nn );
            int skip=0;
            int bigSkip=0;
            for ( int i=0; i<t-1; i++ ) {
                double d= Math.abs( Math.log( xds.value(i+1) / xds.value(i) ) );
                if ( d > s*1.5 ) {
                    skip++;
                    if ( d > s*7 ) {
                        bigSkip++;
                    }
                }
            }
            logger.log(Level.FINE, "guessCadence({0})->null because of log,skip,not bigSkip", new Object[]{xds});
            if ( bigSkip==0 && skip>0 ) {
                logger.exiting(CLASSNAME,"guessCadenceNew"); 
                theResult= null;
                haveResult= true;
            }
        }

        if ( !haveResult ) {
            MutablePropertyDataSet result= DRank0DataSet.create(ss/nn);

            // 1582: one last check, because the gaps in the spectrogram come up way too often! 
            if ( t<LIMIT_YTAGS_LENGTH_CHECK ) {
                QDataSet r;
                QDataSet tresult= Ops.multiply(result,1.10);
                if ( log && logDiff!=null ) {
                    r= Ops.where( Ops.gt( logDiff,tresult ) );
                } else {
                    r= Ops.where( Ops.gt( diffs,tresult ) );
                }
                if ( r.length()>t/4 ) {
                    theResult= null;
                    haveResult= true;
                }
                
                try {
                    if ( !log ) {
                        Units du= SemanticOps.getUnits(diffs);
                        QDataSet dd= DataSetUtil.gcd( diffs, DataSetUtil.asDataSet(SemanticOps.getUnits(diffs).createDatum(0.1)) );
                        double ddd= dd.value();
                        boolean isMultiples= true;
                        for ( int i=0; i<diffs.length(); i++ ) {
                            if ( Math.abs( diffs.value(i) % ddd )>0 ) {
                                isMultiples= false;
                            }
                            if ( ( diffs.value(i) / ddd ) > 100 ) { // test014_004--log spacing.
                                isMultiples= false;
                            }
                        }
                        if ( isMultiples ) {
                            result= DRank0DataSet.create(dd.value());
                        }
                    }
                } catch ( Exception e ) {
                    logger.fine("unable to perform gcd test");
                }
                
            }

            if ( !haveResult ) {
                if ( log ) {
                    result.putProperty( QDataSet.UNITS, Units.logERatio );
                    result.putProperty( QDataSet.SCALE_TYPE, "log" );
                    logger.log(Level.FINE, "guessCadence({0})->{1} (log)", new Object[]{xds, result});
                    logger.exiting(CLASSNAME,"guessCadenceNew");
                    theResult= (RankZeroDataSet)result;
                } else {
                    result.putProperty( QDataSet.UNITS, xunits.getOffsetUnits() );
                    logger.log(Level.FINE, "guessCadence({0})->{1} (linear)", new Object[]{xds, result});
                    logger.exiting(CLASSNAME,"guessCadenceNew");                    
                    theResult= (RankZeroDataSet)result;
                }
            }
        }  
        
        //DataSetAnnotations.getInstance().putAnnotation( xds, DataSetAnnotations.ANNOTATION_CADENCE, theResult );
        
        return theResult;
    }

    /**
     * return the value of the nth biggest item.  This keeps n values in memory.
     * @param set rank 1 dataset containing comparable data.
     * @param n the number of items to find
     * @return the n largest elements, sorted.
     */
    public static QDataSet nLargest( final QDataSet set, int n ) {
        
        TreeMap<Double,Integer> ts= new TreeMap<>();
                
        int len= set.length();
        QDataSet wds= weightsDataSet(set);
                
        int i=0;
        while ( ts.size()<n && i<set.length() ) {
            if ( wds.value(i)>0 ) {
                double d= set.value(i);
                Integer count= ts.get( d );
                if ( count==null ) {
                    ts.put( d, 1 );
                } else {
                    ts.put( d, count+1 );
                }
            }
            i=i+1;
        }
        
        if ( i==set.length() && ts.size()<n ) {
            throw new IndexOutOfBoundsException("dataset size ("+set.length()+ ") is smaller than size ("+n+")" );
        }
        
        Double sm= ts.firstKey();
        for ( ; i<len; i++ ) {
            if ( wds.value(i)>0 ) {
                Double dsiv= set.value(i);
                if ( dsiv>sm ) {
                    Integer count= ts.get(sm);
                    if ( count==1 ) {
                        ts.remove(sm);
                    } else {
                        ts.put( sm, count-1 );
                    }
                    count= ts.get(dsiv);
                    if ( count==null ) {
                        ts.put( dsiv, 1 );
                    } else {
                        ts.put( dsiv, count+1 );
                    }
                    sm= ts.firstKey();
                }
            }
        }
        
        DDataSet result= DDataSet.createRank1(n);
        result.putProperty( QDataSet.UNITS, SemanticOps.getUnits(set) );
        Double[] dd= ts.keySet().toArray( new Double[ts.size()] );
        Integer[] ii= ts.values().toArray( new Integer[ts.size()] );
        int index=0;
        for ( i=0; i<dd.length; i++ ) {
            for ( int j=0; j<ii[i]; j++ ) {
                result.putValue( index, dd[i] );
                index++;
            }
        }
        return result;
    }
            
    /**
     * use K-means algorithm to find N means in a rank 1 dataset.  
     * @param xds dataset containing data from N normal distributions
     * @param n number of divisions.
     * @return dataset containing the index for each point.
     */
    public static QDataSet kmeansCadence( QDataSet xds, int n ) {
        boolean done= false;
        double[] boundaries;
        QDataSet ext= Ops.extent(xds);
        double min= ext.value(0);
        double max= ext.value(1);
        boundaries= new double[n-1];
        for ( int i=0; i<n-1; i++ ) {
            boundaries[i]= min + ( i+1 ) * ( max-min ) / n;
        }
        double[] means=null;
        double[] ww;
        int[] bs= new int[xds.length()];
        int b= 0; // the current 1-D triangle
        while ( !done ) {
            means= new double[n];
            ww= new double[n];            
            for ( int i=0; i<xds.length(); i++ ) {
                double d= xds.value(i);
                while ( b < n-1 && d >= boundaries[b]) b++;
                while ( b > 0 &&  d < boundaries[b-1] ) b--;
                means[b] += d;
                ww[b] += 1;
                bs[i]= b;
            }
            done= true;
            for ( int j=0; j<n; j++ ) {
                if ( ww[j]==0 ) {
                    for ( int i=0; i<n-1; i++ ) {
                       boundaries[i]= min + ( i+1 ) * ( max-min ) / n;
                    }
                    done= false;
                    throw new IllegalArgumentException("algorithm fails");
                } else {
                    means[j]= means[j]/ww[j];
                }
            }
            if ( !done ) continue;
            for ( int j=0; j<n-1; j++ ) {
                double bb= ( means[j] + means[j+1] ) / 2;
                if ( bb!=boundaries[j] ) {
                    done= false;
                    boundaries[j]= bb;
                }
            }
            System.err.println("boundaries[0]="+boundaries[0]);
        }
        IDataSet result= IDataSet.wrap(bs);
        result.putProperty( QDataSet.DEPEND_0, xds );
        Map<String,Object> userProps= new HashMap<>();
        userProps.put( "means", means );
        result.putProperty( QDataSet.USER_PROPERTIES, userProps );
        return result;
    }
    
    /**
     * return the cadence for the given x tags.  The goal will be a rank 0
     * dataset that describes the intervals, but this will also return a rank 1
     * dataset when multiple cadences are found.  The yds values for each xds value can 
     * also be set, specifying where the x values can be ignored because of fill.  
     * TODO: this needs review.
     * @param xds the x tags, which may not contain fill values for non-null result.
     * @param yds the y values, which if non-null is only used for fill values.  This is only used if it is rank 1.
     * @return rank 0 or rank 1 dataset.
     */
    public static QDataSet guessCadence( QDataSet xds, QDataSet yds ) {
        
        if ( yds!=null && yds.rank()==1 ) { // There's a silly dataset where every other measurement is to be ignored.
            QDataSet r= Ops.where( Ops.valid(yds) );
            if ( r.length()<=xds.length()/2 ) {
                xds= Ops.applyIndex( yds, r );
            }
        }
        
        if ( xds instanceof IndexGenDataSet ) {
            IDataSet result= IDataSet.createRank0();
            result.putValue( 1 );
            Units u= (Units)xds.property(QDataSet.UNITS);
            if ( u!=null ) {
                result.putProperty( QDataSet.UNITS, u.getOffsetUnits() );
            }
            return result;
        }
        
        if ( xds.rank()==1 && xds.length()<1 ) {
            QDataSet cadenceDs= (QDataSet) xds.property(QDataSet.CADENCE);
            if ( cadenceDs==null ) {
                return null;
            } else {
                return cadenceDs;
            }
        }
        
        QDataSet dephhLinear;
        int idephhLinear;
        int scoreLinear;
        
        QDataSet dephhLog;
        int idephhLog;
        int scoreLog;
             
        QDataSet dxds= Ops.abs( Ops.diff( xds ) );
        
        if ( dxds.length()>4 ) {
            QDataSet limit= Ops.divide( dxds.slice(0), 10000 );
            QDataSet gcd1= null;
            try {
                gcd1= DataSetUtil.gcd( dxds.trim(0,dxds.length()/2), limit );
            } catch ( IllegalArgumentException ex ) {
            }
            QDataSet gcd2= null;
            try {
                gcd2= DataSetUtil.gcd( dxds.trim(dxds.length()/2,dxds.length()), limit );
            } catch ( IllegalArgumentException ex ) {
            }
            
            if ( gcd1==null ) return gcd2;
            if ( gcd2==null ) return gcd1;
            
            if ( Ops.divide( gcd1, limit ).value()>10 && Ops.divide( gcd2, limit ).value()>10 ) {
                return Ops.lesserOf( gcd1, gcd2 );
            }
        }
        
        if ( dxds.length()==0 ) {
            QDataSet cadenceDs= (QDataSet) xds.property(QDataSet.CADENCE);
            if ( cadenceDs==null ) {
                return null;
            } else {
                return cadenceDs;
            }
        }
                
        // linear spacing
        {
                    
            // dxds= ( append( dsds[0], dxds ) + append( dxds, dxds[-1]) ) ) / 2;
            dxds= Ops.divide( Ops.add( Ops.append( dxds.slice(0), dxds ), 
                                       Ops.append( dxds, dxds.slice(dxds.length()-1) ) ),
                              Ops.dataset(2) );
        
            QDataSet hh= Ops.autoHistogram( dxds );
        
            int maxhh= -1;
            int imaxhh= -1;
            for ( int i=0; i<hh.length(); i++ ) {
                if ( hh.value(i)>maxhh) {
                    maxhh= (int)hh.value(i);
                    imaxhh= i;
                }
            }
            dephhLinear= (QDataSet)hh.property(QDataSet.DEPEND_0);
            idephhLinear= imaxhh;
            scoreLinear= 1;
        }
        
        // log spacing
        {
            dxds= Ops.diff( Ops.log10( xds ) );
            // dxds= ( append( dsds[0], dxds ) + append( dxds, dxds[-1]) ) ) / 2;
            dxds= Ops.divide( Ops.add( Ops.append( dxds.slice(0), dxds ), 
                              Ops.append( dxds, dxds.slice(dxds.length()-1) ) ), 
                  Ops.dataset(2) );
        
            QDataSet hh= Ops.autoHistogram( dxds );
        
            int maxhh= -1;
            int imaxhh= -1;
            for ( int i=0; i<hh.length(); i++ ) {
                if ( hh.value(i)>maxhh) {
                    maxhh= (int)hh.value(i);
                    imaxhh= i;
                }
            }
            
            dephhLog= (QDataSet)hh.property(QDataSet.DEPEND_0);
            idephhLog= imaxhh;
            scoreLog= 0;
        }
        
        if ( scoreLinear>scoreLog ) {
            return dephhLinear.slice(idephhLinear);
        } else {
            return dephhLog.slice(idephhLog);
        }
                
    }    
    
    /**
     * return the cadence in X for the given X tags and Y tags.  The Y tags can repeat, and when this is the
     * case, the X cadence will be span between successive X tags with the same Y tag.
     * The goal will be a rank 0
     * dataset that describes the intervals, but this will also return a rank 1
     * dataset when multiple cadences are found.
     * @param xds the x tags, which may not contain fill values for non-null result.
     * @param yds the y values, which may not contain fill values for non-null result.
     * @param zds the z values
     * @return rank 0 or rank 1 dataset.
     */
    public static QDataSet guessCadence( QDataSet xds, QDataSet yds, QDataSet zds ) {
        
        QDataSet dxds= Ops.diff( xds );
        
        dxds= Ops.divide( Ops.add( Ops.append( dxds.slice(0), dxds ), Ops.append( dxds, dxds.slice(dxds.length()-1) ) ), Ops.dataset(2) );
        
        QDataSet hh= Ops.autoHistogram( dxds );
        
        int maxhh= -1;
        int imaxhh= -1;
        for ( int i=0; i<hh.length(); i++ ) {
            if ( hh.value(i)>maxhh) {
                maxhh= (int)hh.value(i);
                imaxhh= i;
            }
        }
        QDataSet dephh= (QDataSet)hh.property(QDataSet.DEPEND_0);
        
        return dephh.slice(imaxhh);
                
    }
    
    /**
     * return true if each record of DEPEND_0 is the same.  Rank 0 datasets
     * are trivially constant.
     * TODO: ds.slice(i) can be slow because equivalent does so much with the metadata.
     * @param ds any dataset
     * @return true if the dataset doesn't change with DEPEND_0 or is rank 0.
     */
    public static boolean isConstant( QDataSet ds ) {
        if ( ds.rank()==0 || ds.length()==0 ) {
            return true;
        } else {
            QDataSet s1= ds.slice(0);
            for ( int i=1; i<ds.length(); i++ ) {
                if ( !Ops.equivalent(s1,ds.slice(i)) ) return false;
            }
            return true;
        }
    }
    
    /**
     * return true if each record of DEPEND_i is the same.  Rank 0 datasets
     * are trivially constant.
     * @param ds any dataset
     * @param dim the index
     * @return true if the dataset doesn't change with DEPEND_0 or is rank 0.
     */
    public static boolean isConstant( QDataSet ds, int dim ) {
        if ( ds.rank()==0 || ds.length()==0 ) {
            return true;
        } else if ( dim==0 ) {
            return isConstant( ds );
        } else if ( dim==1 ) {
            QDataSet s1= Ops.slice1( ds, 0 );
            for ( int i=1; i<ds.length(0); i++ ) {
                if ( !Ops.equivalent(s1,Ops.slice1(ds,i) ) ) return false;
            }
            return true;
        } else if ( dim==2 ) {
            QDataSet s1= Ops.slice2( ds, 0 );
            for ( int i=1; i<ds.length(0,0); i++ ) {
                if ( !Ops.equivalent(s1,Ops.slice2(ds,i) ) ) return false;
            }
            return true;
        } else if ( dim==3 ) {
            QDataSet s1= Ops.slice3( ds, 0 );
            for ( int i=1; i<ds.length(0); i++ ) {
                if ( !Ops.equivalent(s1,Ops.slice3(ds,i) ) ) return false;
            }
            return true;
        } else {
            throw new IllegalArgumentException("rank limit");
        }
    }    
    
    /**
     * special check to see if joined datasets really are a qube.  This
     * will putProperty(QDataSet.QUBE,Boolean.TRUE) when the dataset really is
     * a qube.
     * @param ds
     * @return 
     */
    private static boolean checkQubeJoin( QDataSet ds ) {
        QDataSet d0= ds.slice(0);
        Map<String,Object> p0= DataSetUtil.getProperties(d0);
        int n=p0.size();
        for ( int i=1; i<ds.length(); i++ ) {
            QDataSet d1= ds.slice(i);
            if ( d0.length()!=d1.length() ) {
                return false;
            }
            Map<String,Object> p1= DataSetUtil.getProperties(d1);
            Map<String,Object> eqp= Ops.equalProperties( p0, p1 );
            if ( eqp.size()!=n ) {
                return false;
            }
        }
        if ( ds instanceof MutablePropertyDataSet && !((MutablePropertyDataSet)ds).isImmutable() ) {
            logger.fine("putProperty(QDataSet.QUBE,Boolean.TRUE)");
            ((MutablePropertyDataSet)ds).putProperty( QDataSet.QUBE, Boolean.TRUE );
        }
        return true;
    }
    
    /**
     * have one place where we assert qube.
     * @param ds
     * @param value 
     */
    private static void maybeAssertQube( QDataSet ds, Boolean value ) {
        if ( ds instanceof MutablePropertyDataSet && !((MutablePropertyDataSet)ds).isImmutable() ) {
            logger.log(Level.FINE, "putProperty(QDataSet.QUBE,{0})", value);
            ((MutablePropertyDataSet)ds).putProperty( QDataSet.QUBE, value );
        }
        DataSetAnnotations.getInstance().putAnnotation( ds, DataSetAnnotations.ANNOTATION_QUBE, value );
    }
    
    /**
     * check to see if a dataset really is a qube, even if there is a
     * rank 2 dep1.  Note this ignores BUNDLE_1 property if there is a DEPEND_1.
     * This was motivated by the fftPower routine, which returned a rank 2 DEPEND_1,
     * but is typically constant, and RBSP/ECT datasets that often have rank 2 
     * DEPEND_1s that are constant.  This
     * will putProperty(QDataSet.QUBE,Boolean.TRUE) when the dataset really is
     * a qube, or Boolean.FALSE if is clearly not a qube.
     * @param ds any dataset
     * @return true if the dataset really is a qube.
     */
    public static boolean checkQube( QDataSet ds ) {
        if ( ds.rank()<2 ) {
            return true;
        } else {
            Boolean q = (Boolean) ds.property(QDataSet.QUBE);
            if ( q == null ) {
                Boolean q1= (Boolean)DataSetAnnotations.getInstance().getAnnotation( ds, DataSetAnnotations.ANNOTATION_QUBE );
                if ( q1!=null ) return q1;
                if ( SemanticOps.isJoin(ds) ) {
                    return checkQubeJoin(ds);
                }
                for ( int i=1; i<ds.rank(); i++ ) {
                    QDataSet dep= (QDataSet) ds.property( "DEPEND_"+i );
                    if ( dep!=null && dep.rank()>1 ) {
                        if ( !isConstant(dep) ) {
                            maybeAssertQube( ds, Boolean.FALSE );
                            return false;
                        }
                    }
                    if ( i==1 && dep==null && ds.length()>0 ) {
                        int rec0len= DataSetUtil.totalLength( ds.slice(0) );
                        for ( int j=0; j<ds.length(); j++ ) {
                            if ( DataSetUtil.totalLength( ds.slice(j) ) != rec0len ) {
                                 maybeAssertQube( ds, Boolean.FALSE );
                                 return false;
                            }
                        }
                    }
                }
                maybeAssertQube( ds, Boolean.TRUE );
                return true;
            } else {
                return true;
            }
        }
    }
    
    /**
     * test to see that the dataset is a qube.  
     * TODO: this all needs review.
     * @param ds QDataSet of any rank.
     * @return true if the dataset is a qube.
     */
    public static boolean isQube(QDataSet ds) {
        if (ds.rank() < 2) return true;
        Boolean q = (Boolean) ds.property(QDataSet.QUBE);
        if (q == null || q.equals(Boolean.FALSE)) {
            QDataSet dep1= (QDataSet) ds.property(QDataSet.DEPEND_1);
            return ds.rank()==2 && dep1!=null && dep1.rank()==1;
        }
        return true;
    }
    
    /**
     * return a subset of the dataset which is a qube.
     * @param ds a rank N dataset like dataset[3,1616*,6144*], where asterisks (stars) indicate it is not a qube.
     * @param reclen the record length for each record.
     * @return a rank N-1 dataset.
     */
    public static QDataSet qubeSubset( QDataSet ds, int reclen ) {
        if ( DataSetUtil.isQube(ds) ) {
            throw new IllegalArgumentException("Dataset is already a qube");
        }
        int n= 0;
        for ( int i=0; i<ds.length(); i++ ) {
            QDataSet ds1= ds.slice(i); // this must be a qube.
            if ( ds1.length(0)==reclen ) {
                n+= ds1.length();
            }
        }
        
        ArrayDataSet result;
        ArrayDataSet resultDep0;
        
        if ( ds.rank()==3 ) {
            Class dsType= ArrayDataSet.guessBackingStore(ds.slice(0));
            Class dep0Type= ArrayDataSet.guessBackingStore(((QDataSet)ds.slice(0).property(QDataSet.DEPEND_0)));
            result= ArrayDataSet.create( dsType, new int[] { n, reclen } );
            resultDep0= ArrayDataSet.create( dep0Type, new int[] { n } );
            int iout=0;
            boolean copyProps= true;
            for ( int i=0; i<ds.length(); i++ ) {
                QDataSet ds1= ds.slice(i); // this must be a qube.
                if ( ds1.length(0)==reclen ) {
                    QDataSet dep0= (QDataSet) ds1.property(QDataSet.DEPEND_0);
                    if ( dep0==null ) dep0= Ops.add( iout, Ops.indgen(ds1.length()) );
                    DataSetUtil.copyDimensionProperties( dep0, resultDep0 );
                    for ( int j=0; j<ds1.length(); j++ ) {
                        for ( int k=0; k<reclen; k++ ) {
                            result.putValue( iout, k, ds1.value( j,k ) );
                        }
                        resultDep0.putValue( iout, dep0.value(j) );
                        iout++;
                    }
                    if ( copyProps ) {
                        DataSetUtil.copyDimensionProperties( ds1, result );
                        QDataSet resultDep1= (QDataSet)ds1.property(QDataSet.DEPEND_1);
                        if ( resultDep1!=null && resultDep1.rank()>1 ) {
                            throw new IllegalArgumentException("result depend1 cannot be rank 2");
                        }
                        result.putProperty(QDataSet.DEPEND_1, resultDep1);
                        copyProps= false;
                    }
                }
            }
            result.putProperty(QDataSet.DEPEND_0,resultDep0);
        } else {
            throw new IllegalArgumentException("rank 2 and rank 4 are not yet supported");
        }
        
        return result;
    }

    /**
     * provides a convenient way of indexing qubes, returning an int[] of 
     * length ds.rank() containing each dimension's length,
     * or null if the dataset is not a qube.
     * @param ds
     * @return int[] of length ds.rank() containing each dimension's length, or null if the dataset is not a qube.
     */
    public static int[] qubeDims(QDataSet ds) {
        if (ds.rank() > 4) {
            int [] qube= new int[ds.rank()];
            int rank=ds.rank();
            for ( int i=0; i<rank; i++ ) {
                qube[i]= ds.length();
                ds= ds.slice(0);
            }
            return qube;
        } else if (ds.rank()==2 ) {  // rank 1 depend_1 implies qube.
            QDataSet dep1= (QDataSet) ds.property(QDataSet.DEPEND_1);
            if ( dep1!=null && dep1.rank()==1 ) return new int[] { ds.length(), dep1.length() };
        } else if (ds.rank() == 1) {
            return new int[]{ds.length()};  // rank 1 datasets are trivially qubes
        } else if ( ds.rank()== 0 ) {
            return new int[]{};  // rank 0 datasets are trivially qubes
        }
        boolean q= checkQube( ds );
        if ( !q ) {
            return null;
        }
        int[] qube = new int[ds.rank()];
        qube[0] = ds.length();
        if (ds.rank() > 1) {
            qube[1] = ds.length(0);
            if (ds.rank() > 2) {
                qube[2] = ds.length(0, 0);
                if (ds.rank() > 3) {
                    qube[3] = ds.length(0, 0, 0);
                    if (ds.rank() > 4) { // TODO: generalize to rank N
                        throw new IllegalArgumentException("rank limit");
                    }
                }
            }
        }
        return qube;
    }
    
    /**
     * Test for bundle scheme.  Returns true if the data contains data with just one unit.
     * For example ds[time=100,en=30] might have BUNDLE_1 to specify labels for each of the
     * 30 channels, but each of its measurements have the same units.  However, bundle(time,density)
     * results in a dataset with BUNDLE_1 with the two datasets having different units.  
     * When two bundled dataset's units are convertible but not equal this will return false.
     * This will return true for a zero-length dataset.
     * 
     * @param ds the dataset
     * @return true if the dataset has just one unit.
     */
    public static boolean oneUnit(QDataSet ds) {
        switch (ds.rank()) {
            case 0:
                return true;
            case 1:
                if ( SemanticOps.isRank1Bundle(ds) ) {
                    QDataSet bds= (QDataSet)ds.property(QDataSet.BUNDLE_0);
                    if ( bds.length()==0 ) return true;
                    Units u= (Units)bds.property(QDataSet.UNITS,0);
                    for ( int i=1; i<bds.length(); i++ ) {
                        if ( u!=(Units)bds.property(QDataSet.UNITS,i) ) {
                            return false;
                        }
                    }
                    return true;
                } else {
                    return true;
                }
            default:
                if ( DataSetUtil.isQube(ds) ) {
                    return ds.length()==0 || oneUnit(ds.slice(0));
                } else {
                    if ( ds.length()==0 ) return true;
                    QDataSet ds1= ds.slice(0);
                    Units u= (Units)ds1.property(QDataSet.UNITS);
                    for ( int i=0; i<ds.length(); i++ ) {
                        if ( u!=ds1.property(QDataSet.UNITS) ) return false;
                        if ( !oneUnit(ds.slice(i)) ) return false;
                    }
                    return true;
                }
        }
    }
    

    /**
     * returns 1 for zero-length qube, the product otherwise.
     * @param qube int array
     * @return the product of the elements of the array
     */
    public static int product( int[] qube ) {
        switch ( qube.length ) {
            case 0: return 1;
            case 1: return qube[0];
            case 2: return qube[0]*qube[1];
            case 3: return qube[0]*qube[1]*qube[2];
            case 4: return qube[0]*qube[1]*qube[2]*qube[3];
            default: {
                int result= qube[0];
                for ( int i=1; i<qube.length; i++ ) result*= qube[i];
                return result;
            }
        }
    }
    
    /**
     * returns 0 for zero-length qube, the sum otherwise.
     * @param qube int array
     * @return the sum of the elements of the array
     */
    public static int sum( int[] qube ) {
        switch ( qube.length ) {
            case 0: return 0;
            case 1: return qube[0];
            case 2: return qube[0]+qube[1];
            case 3: return qube[0]+qube[1]+qube[2];
            case 4: return qube[0]+qube[1]+qube[2]+qube[3];
            default: {
                int result= qube[0];
                for ( int i=1; i<qube.length; i++ ) result+= qube[i];
                return result;
            }
        }
    }    
    
    /**
     * return string representation of the dimensions of the qube dimensions.
     * For example [2,4] is represented as "[2,4]"
     * @param qube an array of integers, 4 elements or fewer.
     * @return the formatted string representation
     */
    public static String toString( int[] qube ) {
        switch ( qube.length ) {
            case 0: return "[]";
            case 1: return "["+qube[0]+"]";
            case 2: return "["+qube[0]+","+qube[1]+"]";
            case 3: return "["+qube[0]+","+qube[1]+","+qube[2]+"]";
            case 4: return "["+qube[0]+","+qube[1]+","+qube[2]+","+qube[3]+"]";
            default: throw new IllegalArgumentException("qube is too long");
        }
    }

    /**
     * add QUBE property to dataset, maybe verifying that it is a qube.  This is
     * intended to reduce code that builds datasets, not to verify that a dataset
     * is a qube.
     * @param ds the dataset
     * @throws IllegalArgumentException if the dataset is not a qube
     */
    public static void addQube(MutablePropertyDataSet ds) throws IllegalArgumentException {
        int[] qube = null;
        switch (ds.rank()) {
            case 0:
                break; // don't bother adding this property to rank 0 datasets.

            case 1:
                break; // don't bother adding this property to rank 1 datasets.

            case 2:
                qube = new int[]{ds.length(), ds.length(0)};
                if (ds.length() > 0) {
                    for (int i = 1; i < ds.length(); i++) {
                        if (ds.length(i) != ds.length(0)) {
                            throw new IllegalArgumentException("dataset is not a qube");
                        }
                    }
                }
                break;
            case 3:
                qube = new int[]{ds.length(), ds.length(0), ds.length(0, 0)};
                if (ds.length() > 0 && ds.length(0) > 0) {
                    for (int i = 1; i < ds.length(); i++) {
                        if (ds.length(i) != ds.length(0)) {
                            throw new IllegalArgumentException("dataset is not a qube");
                        }
                        for (int j = 1; j < ds.length(0); j++) {
                            if (ds.length(i, j) != ds.length(0, 0)) {
                                throw new IllegalArgumentException("dataset is not a qube");
                            }
                        }
                    }
                }
                break;
            case 4:
                qube = new int[]{ds.length(), ds.length(0), ds.length(0, 0),  ds.length(0,0,0) };
                if (ds.length() > 0 && ds.length(0) > 0 && ds.length(0,0)>0 ) {
                    for (int i = 1; i < ds.length(); i++) {
                        if (ds.length(i) != ds.length(0)) {
                            throw new IllegalArgumentException("dataset is not a qube");
                        }
                        for (int j = 1; j < ds.length(0); j++) {
                            if (ds.length(i, j) != ds.length(0, 0)) {
                                throw new IllegalArgumentException("dataset is not a qube");
                            }
                            for (int k = 1; k < ds.length(0,0); k++) {
                                if (ds.length(i, j, k) != ds.length(0, 0, 0)) {
                                    throw new IllegalArgumentException("dataset is not a qube");
                                }
                            }
                        }
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("rank not supported");
        }
        if (qube != null) {
            ds.putProperty(QDataSet.QUBE, Boolean.TRUE);
        }
    }

    /**
     * return a human-readable string representing the dataset
     * @param ds the dataset to represent
     * @return a human-readable string 
     */
    public static String format(QDataSet ds) {
        return format( ds, true );
    }

    /**
     * return a human-readable string representing the dataset
     * @param ds the dataset to represent
     * @param showContext show the context property (@slice2=1) if present and ds is rank0.
     * @return a human-readable string 
     * @see #toString(org.das2.qds.QDataSet) 
     */
    public static String format(QDataSet ds,boolean showContext) {
        if ( ds.property(QDataSet.BUNDLE_0)!=null ) {
            StringBuilder result= new StringBuilder(); // for documenting context.
            for ( int i=0; i<ds.length(); i++ ) {
                QDataSet cds= ds.slice(i);
                result.append( DataSetUtil.format(cds) );
                if ( i<ds.length()-1 ) result.append(", ");
            }
            return result.toString();
        } else if ( QDataSet.VALUE_BINS_MIN_MAX.equals( ds.property(QDataSet.BINS_0) ) && ds.rank()==1) {
            StringBuilder result= new StringBuilder();
            Units u= (Units) ds.property(QDataSet.UNITS);
            if ( u==null ) u= Units.dimensionless;
            result.append( new DatumRange( ds.value(0), ds.value(1), u ).toString() );

            String[] ss= ((String)ds.property(QDataSet.BINS_0)).split(",",-2);
            if (ss.length!=ds.length() ) throw new IllegalArgumentException("bins count != length in ds");
            return result.toString();

        } else if ( "min,maxInclusive".equals( ds.property(QDataSet.BINS_0) ) && ds.rank()==1) {
            StringBuilder result= new StringBuilder();
            Units u= (Units) ds.property(QDataSet.UNITS);
            if ( u==null ) u= Units.dimensionless;
            result.append( DatumRangeUtil.toStringInclusive( new DatumRange( ds.value(0), ds.value(1), u ) ) );
            String[] ss= ((String)ds.property(QDataSet.BINS_0)).split(",",-2);
            if (ss.length!=ds.length() ) throw new IllegalArgumentException("bins count != length in ds");
            return result.toString();

        } else if ( ds.property(QDataSet.BINS_0)!=null && ds.rank()==1) {
            StringBuilder result= new StringBuilder();
            Units u= (Units) ds.property(QDataSet.UNITS);
            if ( u==null ) u= Units.dimensionless;
            String[] ss= ((String)ds.property(QDataSet.BINS_0)).split(",",-2);
            if (ss.length!=ds.length() ) throw new IllegalArgumentException("bins count != length in ds");
            for ( int i=0; i<ds.length(); i++ ) {
                result.append(ss[i]).append("=").append( u.createDatum(ds.value(i)));
                if ( i<ds.length()-1 ) result.append(", ");
            }
            if ( ds.property(QDataSet.SCALE_TYPE)!=null ) {
                result.append("SCALE_TYPE=").append(ds.property(QDataSet.SCALE_TYPE));
            }
            return result.toString();
        } else if ( ds.rank()==1 && Schemes.isComplexNumbers(ds) ) {
            DecimalFormat df= new DecimalFormat("0.000E0");
            String rs= String.valueOf(ds.value(0));
            String is= String.valueOf(ds.value(1));
            if ( rs.length()>7 ) rs= df.format(ds.value(0));
            if ( is.length()>7 ) is= df.format(ds.value(1));
            return "(" + rs + "+" + is+"j)"; // Use "j" instead of "i" because Python does this.
        }        
        if ( ds.rank()==0 ) {
            String name= (String) ds.property(QDataSet.NAME);
            Units u= (Units) ds.property(QDataSet.UNITS);
            String format= (String) ds.property( QDataSet.FORMAT );
            StringBuilder result= new StringBuilder();
            if ( name!=null ) {
                result.append(name).append("=");
            }
            if ( format!=null && format.trim().length()>0 ) {
                FormatStringFormatter fsf= new FormatStringFormatter( format, true );
                if ( u!=null ) {
                    if ( UnitsUtil.isTimeLocation(u) ) {
                        double millis= u.convertDoubleTo(Units.t1970, ds.value() );
                        Calendar cal= Calendar.getInstance();
                        cal.setTimeInMillis( (long)millis ); // TODO: check how to specify to nanos.
                        result.append( String.format(Locale.US,format,cal) );
                    } else {
                        result.append( fsf.format( DataSetUtil.asDatum(ds) ) );
                    }
                } else {
                    result.append( fsf.format( DataSetUtil.asDatum(ds) ) );
                }
            } else {
                if ( u!=null ) {
                    result.append( u.createDatum(ds.value()).toString() );
                } else {
                    result.append( ds.value() );
                }
            }
            if ( showContext ) {
                QDataSet context0= (QDataSet) ds.property("CONTEXT_0");
                if ( context0!=null ) {
                    result.append(" @ ").append(format(context0));
                }
            }
            return result.toString();
        }
        StringBuilder buf = new StringBuilder(ds.toString() + ":\n");
        if (ds.rank() == 1) {
            for (int i = 0; i < Math.min(40, ds.length()); i++) {
                buf.append(" ").append(ds.value(i));
            }
            if (ds.length() >= 40) {
                buf.append(" ...");
            }
        }
        if (ds.rank() == 2) {
            for (int i = 0; i < Math.min(10, ds.length()); i++) {
                for (int j = 0; j < Math.min(20, ds.length(i)); j++) {
                    buf.append(" ").append(ds.value(i, j));
                }
                if (ds.length() >= 40) {
                    buf.append(" ...");
                }
                buf.append("\n");
            }
            if (ds.length() >= 10) {
                buf.append(" ... ... ... \n");
            }
        }
        return buf.toString();

    }

    /**
     * return a human readable statistical representation of the dataset.  Currently
     * this is the mean, stddev ad number of points.
     * @param ds the data
     * @return return a human readable statistical representation
     */
    public static String statsString(QDataSet ds) {
        RankZeroDataSet stats = DataSetOps.moment(ds);
        return "" + stats.value() + "+/-" + stats.property("stddev") + " N=" + stats.property("validCount");
    }

    /**
     * returns true if the dataset is valid, false otherwise.  If problems is
     * non-null, then problems will be indicated here.
     * @param ds rank N dataset.
     * @param problems insert problem descriptions here, if null then ignore
     * @return true if the dataset is valid, false otherwise
     */
    public static boolean validate(QDataSet ds, List<String> problems) {
        if (problems == null) problems = new ArrayList<>();
        return validate(ds, problems, 0);
    }

    /**
     * add method for validating before link is called.
     * @param xds
     * @param yds
     * @param problems insert problem descriptions here, if null then ignore
     * @return true if the datasets can be linked into a valid dataset, false otherwise
     */
    public static boolean validate(QDataSet xds, QDataSet yds, List<String> problems ) {
        if ( xds.length()!=yds.length() ) {
            if (problems != null) problems.add(String.format("DEPEND_%d length is %d, should be %d.", 0, xds.length(), yds.length()));
            return false;
        } else {
            return validate( Ops.link(xds, yds), problems, 0 );
        }
    }

    /**
     * add method for validating before link is called.
     * @param xds rank 1 tags
     * @param yds rank 1 or rank 2 tags
     * @param zds the dependent data.
     * @param problems insert problem descriptions here, if null then ignore
     * @return true if the datasets can be linked into a valid dataset, false otherwise
     */
    public static boolean validate(QDataSet xds, QDataSet yds, QDataSet zds, List<String> problems ) {
        if ( xds.length()!=zds.length() ) {
            if (problems != null) problems.add(String.format("DEPEND_%d length is %d, should be %d.", 0, xds.length(), zds.length()));
            return false;
        } else {
            return validate( Ops.link(xds, yds, zds ), problems, 0 );
        }
    }

    /**
     * return the total number of values in the dataset, as a long.  For qubes this is the product
     * of the dimension lengths, for other datasets we create a dataset of lengths
     * and total all the elements.
     * @param ds
     * @return the number of values in the dataset.
     */
    public static long totalLengthAsLong(QDataSet ds) {
        if ( ds.rank()==0 ) return 1;
        if ( ds.rank()==1 ) return ds.length();
        int[] qube= DataSetUtil.qubeDims(ds); 
        if ( qube==null ) {
            LengthsDataSet lds= new LengthsDataSet(ds);
            QubeDataSetIterator it= new QubeDataSetIterator(lds);
            long total= 0;
            while ( it.hasNext() ) {
                it.next();
                total+= it.getValue(lds);
            }
            return total;
        } else {
            long total= qube[0];
            for ( int i=1; i<qube.length; i++ ) {
                total*= qube[i];
            }
            return total;
        }
    }
    
    /**
     * return the total number of values in the dataset.  For qubes this is the product
     * of the dimension lengths, for other datasets we create a dataset of lengths
     * and total all the elements.
     * @param ds
     * @return the number of values in the dataset.
     */
    public static int totalLength(QDataSet ds) {
        if ( ds.rank()==0 ) return 1;
        if ( ds.rank()==1 ) return ds.length();
        int[] qube= DataSetUtil.qubeDims(ds); 
        if ( qube==null ) {
            LengthsDataSet lds= new LengthsDataSet(ds);
            QubeDataSetIterator it= new QubeDataSetIterator(lds);
            int total= 0;
            while ( it.hasNext() ) {
                it.next();
                total+= it.getValue(lds);
            }
            return total;
        } else {
            int total= qube[0];
            for ( int i=1; i<qube.length; i++ ) {
                total*= qube[i];
            }
            return total;
        }
    }

    /**
     * internal validate method
     * @param ds a dataset which may not be valid.
     * @param problems null or a list of problems that is appended.
     * @param dimOffset used to check the slices as well for high rank datasets.
     * @return true if the dataset is valid.
     */
    private static boolean validate(QDataSet ds, List<String> problems, int dimOffset) {
        if (problems == null) problems = new ArrayList<>();
        QDataSet dep = (QDataSet) ds.property(QDataSet.DEPEND_0);
        if (dep != null) {
            if ( dep.rank()==0 ) {
                problems.add("DEPEND_0 is rank 0, where it must be rank 1");
                return false;
            }
            if (dep.length() != ds.length()) {
                if ( Schemes.isPolyMesh(dep) ) {
                    if ( dep.slice(1).length() != ds.length() ) {
                        problems.add(String.format("DEPEND_%d length is %d while poly list length is %d.", dimOffset, dep.length(), ds.length()));
                    }
                } else {
                    problems.add(String.format("DEPEND_%d length is %d while data length is %d.", dimOffset, dep.length(), ds.length()));
                }
            }
            //if ( dep.rank() > ds.rank() ) {  // This happens when we have BINS_1 for DEPEND_0.
            //    problems.add(String.format("DEPEND_%d rank is %d but ds.rank() is less (%d)", dimOffset, dep.rank(), ds.rank()) );
            //} else {
            //    if ( dep.rank()==2 && dep.length(0)!=ds.length(0) ) problems.add(String.format("DEPEND_%d length(0) is %d while data.length(0) is %d.", dimOffset, dep.length(0), ds.length(0)) );
            //}
            if ( dep.rank()>1 ) {
                if ( Schemes.isRank2Bins(dep) ) {
                    // okay
                } else if ( Schemes.isXYScatter(dep) ) {
                    // okay
                } else if ( Schemes.isPolyMesh(dep) ) {
                    // okay
                } else {
                    problems.add( "DEPEND_0 should have only one index or must be a bins ([n,2]) dataset.");
                }
            }
            if (ds.rank() > 1 && ds.length() > 0) {
                QDataSet dep1= (QDataSet)ds.property(QDataSet.DEPEND_1);
                if ( dep1!=null && dep1.rank()>1 ) {
                    if ( dep1.length()!=ds.length() && !SemanticOps.isBins(dep1) ) {
                        problems.add(String.format("rank 2 DEPEND_1 length is %d while data length is %d.", dep1.length(), ds.length()));
                    }
                }
                if ( ds.rank()>QDataSet.MAX_RANK ) {
                    validate( ds.slice(0), problems, dimOffset + 1); // we must use native.
                } else {
                    validate(DataSetOps.slice0(ds, 0), problems, dimOffset + 1); // don't use native, because it may copy. Note we only check the first assuming QUBE.
                }
            }
        }
        if ( ds.property(QDataSet.JOIN_0)!=null ) {
            if ( dimOffset>0 ) {
                problems.add( "JOIN_0 must only be on zeroth dimension: "+dimOffset );
            } else {
                Units u= null;
                boolean onceNotify= false;
                for ( int i=0; i<ds.length(); i++ ) {
                    QDataSet ds1= DataSetOps.slice0(ds,i);
                    if ( !validate( ds1, problems, dimOffset + 1 ) ) {
                        problems.add( "join("+i+") not valid JOINED dataset." );
                    }
                    if ( u==null ) {
                        u= SemanticOps.getUnits(ds1);
                    } else {
                        if ( u!=SemanticOps.getUnits(ds1) && !onceNotify ) {
                            problems.add( "units change in joined datasets");
                            onceNotify= true;
                        }
                    }
                }
            }
        }
        
        { 
            Object obds= ds.property(QDataSet.BUNDLE_0);
            if ( obds!=null ) {
                if ( !QDataSet.class.isInstance(obds) ) {
                    problems.add( "BUNDLE_0 is defined but not a QDataSet");
                } else if ( ((QDataSet)obds).rank()<1 ) { // this happens with CDF slice1, when we don't completely implement slice1.
                    problems.add( "BUNDLE_0 found but dataset is only rank 0");
                } else if ( ds.length()!=((QDataSet)obds).length() ) {
                    //problems.add( "ds.length() doesn't equals BUNDLE_0 length");
                    //TODO: 'http://sarahandjeremy.net/~jbf/autoplot/data/hudson_data/qds/agg/2014-02-25_2day.qds' doesn't validate
                } else {
                    if ( ds.rank()<2 ) {
                        QDataSet bds= (QDataSet)obds;
                        for ( int i=0; i< Math.min(1,bds.length()); i++ ) {
                            QDataSet bds1= DataSetOps.unbundle(ds,i,true); // assumes rank1, so we have excessive work for rank>1
                            Object o= bds1.property(QDataSet.DEPEND_1);
                            if ( o!=null && !(o instanceof QDataSet) ) {
                                validate( bds1,problems,1) ;
                            }
                        }
                    }
                }
            }
        }
        
        
        Object obds= ds.property(QDataSet.BUNDLE_1);
        if ( obds!=null && !(obds instanceof QDataSet) ) {
            throw new IllegalArgumentException("BUNDLE_1 property is not a QDataSet");
        } else {
            if ( obds!=null ) {
                QDataSet bds= (QDataSet)obds;
                if ( ds.rank()<2 ) { // this happens with CDF slice1, when we don't completely implement slice1.
                    problems.add( "BUNDLE_1 found but dataset is only rank 1");
                } else {
                    for ( int i=0; i< Math.min(1,bds.length()); i++ ) {
                        QDataSet bds1= DataSetOps.unbundle(ds,i,true); // assumes rank1, so we have excessive work for rank>1
                        Object o= bds1.property(QDataSet.DEPEND_1);
                        if ( o!=null && !(o instanceof QDataSet) ) {
                            validate( bds1,problems,1) ;
                        }
                    }
                }
            }
        }
        
        Object obins= ds.property( QDataSet.BINS_1 );
        if ( obins!=null && !( obins instanceof String ) ) {
            throw new IllegalArgumentException("BINS_1 property is not a String");
        } else {
            if ( obins!=null ) {
                if ( obins.equals( QDataSet.VALUE_BINS_MIN_MAX ) ) {
                    if ( ds.length(0)!=2 ) {
                        problems.add( "BINS_1 is 'min,max' but length is not 2." );
                    }
                    boolean outOfOrder= false;
                    for ( int i=0; i<ds.length(); i++ ) {
                        if ( ds.value(i,0)>ds.value(i,1) ) {
                            outOfOrder= true;
                        }
                    }
                    if ( outOfOrder ) {
                        logger.info( "validate finds BINS_1 is min,max and min is greater than max, but not marking this as invalid." );
                    }
                }
                if ( obds!=null ) { // 2060: check for constant units in BUNDLE_1
                    Units cu= (Units)ds.property(QDataSet.UNITS);
                    if ( cu==null ) cu= Units.dimensionless;
                    QDataSet bds= (QDataSet)obds;
                    Units inconsistentUnit= null;
                    for ( int j=0; j<bds.length(); j++ ) {
                        Object ou= bds.property( "UNITS", j );
                        if ( ou==null ) ou= Units.dimensionless;
                        if ( cu!=((Units)ou) ) {
                            inconsistentUnit= (Units)ou;
                        }
                    }
                    if ( inconsistentUnit!=null ) {
                        String su= cu.toString();
                        if ( su.length()==0 ) su= "dimensionless";
                        String sinc= inconsistentUnit.toString();
                        if ( sinc.length()==0 ) sinc= "dimensionless";
                        problems.add( "UNITS of bins dataset ("+su+") are inconsistent with those found in BUNDLE_1 ("+sinc+")");
                    }
                }
            }
        }
        
        if ( ds.property(QDataSet.PLANE_0)!=null ) {
            QDataSet plane0 = (QDataSet) ds.property(QDataSet.PLANE_0);
            if ( plane0!=null ) {
                if ( plane0.rank()>0 && plane0.length()!=ds.length() ) { 
                    problems.add( String.format( "PLANE_0 length is %d, should be %d", plane0.length(), ds.length() ) );
                }
            }
        }
        return problems.isEmpty();
    }

    /**
     * throw out DEPEND and PLANE to make dataset valid.
     * @param ds
     */
    public static void makeValid(MutablePropertyDataSet ds) {
        int[] qubeDims = null;
        if (DataSetUtil.isQube(ds)) {
            qubeDims = DataSetUtil.qubeDims(ds);
        }
        int i = 0;
        QDataSet dep = (QDataSet) ds.property("DEPEND_" + i);
        if (dep != null) {
            if (dep.length() != ds.length()) {
                ds.putProperty("DEPEND_" + i, null);
            }
        }
        if (qubeDims != null) {
            for (i = 1; i < qubeDims.length; i++) {
                dep = (QDataSet) ds.property("DEPEND_" + i);
                if (dep != null) {
                    if ( dep.length() != qubeDims[i] ) {
                        ds.putProperty("DEPEND_" + i, null);
                    }
                }
            }
        }

    }

    /**
     * special weightsDataSet for when there is a bundle, and each
     * component could have its own FILL_VALID and VALID_MAX.  Each component
     * gets its own weights dataset in a JoinDataSet.
     * @param ds rank 2 bundle dataset
     * @return dataset with the same geometry but a weightsDataSet of each bundled dataset.
     */
    public static QDataSet bundleWeightsDataSet( final QDataSet ds ) {
        QDataSet bds= (QDataSet)ds.property(QDataSet.BUNDLE_1);
        if ( bds==null ) {
            throw new IllegalArgumentException("dataset must be bundle");
        }
        QDataSet result=null;
        int nb= ds.length(0);
        if ( nb==0 ) {
            throw new IllegalArgumentException("bundle is empty");
        }
        for ( int i=0; i<nb; i++ ) {
            result= Ops.bundle( result, weightsDataSet( DataSetOps.unbundle(ds,i,false) ) );
        }
        assert result!=null;
        ((MutablePropertyDataSet)result).putProperty( QDataSet.FILL_VALUE, -1e38 ); // codes like total assume this property exists.
        ((MutablePropertyDataSet)result).putProperty( WeightsDataSet.PROP_SUGGEST_FILL, -1e38 ); // codes like total assume this property exists.
        return result;
    }
    
    /**
     * Provide consistent valid logic to operators by providing a QDataSet
     * with &gt;0.0 where the data is valid, and 0.0 where the data is invalid.
     * VALID_MIN, VALID_MAX and FILL_VALUE properties are used.  
     * 
     * Note, when FILL_VALUE is not specified, -1e31 is used.  This is to
     * support legacy logic.
     * 
     * For convenience, the property SUGGEST_FILL is set to the fill value used. 
     * 
     * @param ds the dataset 
     * @return a dataset with the same geometry with zero or positive weights.
     */
    public static QDataSet weightsDataSet(final QDataSet ds) {
        Object o= ds.property(QDataSet.WEIGHTS); // See Ivar's script /home/jbf/project/rbsp/users/ivar/20180129/process-chorus3.jyds
        QDataSet w= null;
        if ( o!=null ) {
            if ( !(o instanceof QDataSet) ) {
                logger.log(Level.WARNING, "WEIGHTS contained something that was not a qdataset: {0}", o);
                w= null;
            } else {
                w= (QDataSet)o;
                if ( ds.rank() != w.rank() ) {
                    //See https://sourceforge.net/p/autoplot/bugs/2017/
                    logger.log(Level.WARNING, "WEIGHTS contained qdataset with different rank, ignoring: w={0} ds={1}", new Object[] { w, ds } );
                    w= null;
                    
                } else if ( ((QDataSet)o).length()!=ds.length() ) {
                    //logger.log(Level.WARNING, "WEIGHTS_PLANE was dataset with the wrong length: {0}", o);
                    //TODO: this was coming up in  script:sftp://jbf@klunk:/home/jbf/project/rbsp/study/bill/digitizing/newDigitizer/newdigitizer2.jy
                    //hide it for now...
                    w=null;
                }
            }
        }

        if ( UnitsUtil.isNominalMeasurement( SemanticOps.getUnits(ds)) ) {
            EnumerationUnits eu= (EnumerationUnits) SemanticOps.getUnits(ds);
            if ( eu.hasFillDatum() ) {
                if ( ds.property(QDataSet.FILL_VALUE)==null ) {
                    QDataSet ds1= Ops.putProperty( ds, QDataSet.FILL_VALUE, eu.getFillDatum().doubleValue(eu) );
                    return new WeightsDataSet.FillFinite(ds1);
                } else {
                    return new WeightsDataSet.FillFinite(ds); // DANGER: the enumeration datum fill is now ignored.
                }
            } else {
                return new WeightsDataSet.Finite(ds);
            }
        }
        
        Number ofill;
        QDataSet result = w;
        if ( result!=null ) {
            ofill= (Number)result.property(QDataSet.FILL_VALUE);
            if ( ofill==null ) {
                MutablePropertyDataSet mpds= DataSetOps.makePropertiesMutable( result );
                mpds.putProperty( QDataSet.FILL_VALUE, QDataSet.DEFAULT_FILL_VALUE );
                mpds.putProperty( WeightsDataSet.PROP_SUGGEST_FILL, QDataSet.DEFAULT_FILL_VALUE );
                return mpds;
            } else {
                MutablePropertyDataSet mresult= DataSetOps.makePropertiesMutable(result);
                mresult.putProperty( WeightsDataSet.PROP_SUGGEST_FILL, ofill );
                return mresult;
            }
        } else {
            Number validMin,validMax;
            Units u;
            QDataSet bds= (QDataSet)ds.property(QDataSet.BUNDLE_1);
            if ( bds!=null && ds.rank()==2 && ds.length()>0 && ds.length(0)>0 ) {   
                return bundleWeightsDataSet(ds);
            } else {
                validMin = (Number) ds.property(QDataSet.VALID_MIN);
                validMax = (Number) ds.property(QDataSet.VALID_MAX);
                u = (Units) ds.property(QDataSet.UNITS);
                ofill = (Number) ds.property(QDataSet.FILL_VALUE);
            }
            if (validMin == null) validMin = Double.NEGATIVE_INFINITY;
            if (validMax == null) validMax = Double.POSITIVE_INFINITY;
            
            double fill = (ofill == null ? Double.NaN : ofill.doubleValue());
            boolean check = (validMin.doubleValue() > -1 * Double.MAX_VALUE || validMax.doubleValue() < Double.MAX_VALUE || !(Double.isNaN(fill)));
            if (check) {
                if ( validMin.doubleValue() > -1 * Double.MAX_VALUE || validMax.doubleValue() < Double.MAX_VALUE ) {
                    result= new WeightsDataSet.ValidRangeFillFinite(ds);
                } else {
                    result = new WeightsDataSet.ValidRangeFillFinite(ds);
                }
            } else {
                if (u != null) {
                    result = new WeightsDataSet.FillFinite(ds); // support legacy Units to specify fill value
                } else {
                    result = new WeightsDataSet.Finite(ds);
                }
            }
            return result;
        }
        
    }

    /** 
     * Iterate through the dataset, changing all points outside of validmin,
     * validmax and with zero weight to fill=-1e31.  VALID_MIN and VALID_MAX 
     * properties are cleared, and FILL_VALUE is set to -1e31.
     * If the dataset is writable, then the dataset is modified.
     * 
     * @param ds rank N QUBE dataset.
     * @return ds with same geometry as ds.
     */
    public static WritableDataSet canonizeFill(QDataSet ds) {
        if (!(ds instanceof WritableDataSet)) {
            ds = DDataSet.copy(ds);  // assumes ds is QUBE right now...
        }
        WritableDataSet wrds = (WritableDataSet) ds;
        QubeDataSetIterator it = new QubeDataSetIterator(ds);
        QDataSet wds = weightsDataSet(ds);
        double fill = -1e31;
        while (it.hasNext()) {
            it.next();
            if (it.getValue(wds) == 0) {
                it.putValue(wrds, fill);
            }
        }
        wrds.putProperty(QDataSet.FILL_VALUE, fill);
        return wrds;
    }

    /**
     * convert the dataset to the given units.  
     * @param ds the dataset
     * @param u new Units
     * @return equivalent dataset with the new units.
     */
    public static QDataSet convertTo( QDataSet ds, Units u ) {
        Units su= (Units) ds.property(QDataSet.UNITS);
        if ( su==null ) su= Units.dimensionless;
        UnitsConverter uc= su.getConverter(u);
        DDataSet result = (DDataSet) ArrayDataSet.copy(ds);  // assumes ds is QUBE right now...
        QubeDataSetIterator it= new QubeDataSetIterator(ds);
        while ( it.hasNext() ) {
            it.next();
            it.putValue( result, uc.convert( it.getValue(ds)) );
        }
        Number vmin= (Number) ds.property(QDataSet.VALID_MIN);
        if ( vmin!=null ) result.putProperty( QDataSet.VALID_MIN, uc.convert(vmin));
        Number vmax= (Number) ds.property(QDataSet.VALID_MAX);
        if ( vmax!=null ) result.putProperty( QDataSet.VALID_MAX, uc.convert(vmax));
        Number fill= (Number) ds.property(QDataSet.FILL_VALUE);
        if ( fill!=null ) result.putProperty( QDataSet.FILL_VALUE, uc.convert(fill));

        result.putProperty( QDataSet.UNITS, u );
        return result;
    }


    /**
     * get the value of the rank 0 dataset in the specified units.
     * For example, value( ds, Units.km )
     * @param ds
     * @param tu target units
     * @return the double in target units.
     */
    public static double value( RankZeroDataSet ds, Units tu ) {
        Units u= (Units) ds.property(QDataSet.UNITS);
        if ( tu==null && u==null ) {
            return ds.value();
        } else {
            return u.convertDoubleTo(tu, ds.value() );
        }
    }

    public static Datum asDatum( RankZeroDataSet ds ) {
        return asDatum((QDataSet)ds);
    }

    /**
     * convert the rank 0 QDataSet to a Datum.
     * @param ds rank 0 dataset.
     * @return Datum 
     */
    public static Datum asDatum( QDataSet ds ) {
        if ( ds.rank()>0 ) {
            throw new IllegalArgumentException("dataset is not rank 0");
        } else {
            Units u= SemanticOps.getUnits(ds);
            String format= (String) ds.property(QDataSet.FORMAT);
            QDataSet wds= weightsDataSet(ds);
            if ( wds.value()==0 ) {
                return u.getFillDatum();
            } else {
                LongReadAccess ll= ds.capability( LongReadAccess.class );
                if ( ll!=null ) {
                    if ( format==null || format.trim().length()==0 ) {
                        return Datum.create( ll.lvalue(), u );
                    } else {
                        return Datum.create( ll.lvalue(), u, new FormatStringFormatter(format, true) );
                    }                    
                } else {
                    if ( format==null || format.trim().length()==0 ) {
                        return Datum.create( ds.value(), u );
                    } else {
                        return Datum.create( ds.value(), u, new FormatStringFormatter(format, true) );
                    }
                }
            }
        }
    }

    /**
     * return the DatumRange equivalent of this 2-element, rank 1 bins dataset.
     *
     * @param ds a rank 1, 2-element bins dataset.
     * @param sloppy true indicates we don't check BINS_0 property.
     * @return an equivalent DatumRange 
     */
    public static DatumRange asDatumRange( QDataSet ds, boolean sloppy ) {
        Units u= SemanticOps.getUnits(ds);
        double dmin= ds.value(0);
        double dmax= ds.value(1);
        QDataSet bds= (QDataSet) ds.property( QDataSet.BUNDLE_0 );
        if ( bds!=null ) {
            Units u0= (Units) bds.property(QDataSet.UNITS,0);
            Units u1= (Units) bds.property(QDataSet.UNITS,1);
            if ( u0!=null && u1!=null ) {
                if ( u0==u1 ) {
                    u= u0;
                } else {
                    logger.finest("accommodating bundle of min,delta.");
                    u= u0;
                    dmax= u1.convertDoubleTo( u0.getOffsetUnits(), dmax ) + dmin;
                }
            }
        }
        if ( sloppy==false ) {
            if ( !ds.property( QDataSet.BINS_0 ).equals(QDataSet.VALUE_BINS_MIN_MAX) ) {
                throw new IllegalArgumentException("expected min,max for BINS_0 because we are not allowing sloppy.");
            }
        }
        Datum d1= Datum.create( dmin, u );
        Datum d2= Datum.create( dmax, u );
        if ( UnitsUtil.isNominalMeasurement(u) ) {
            // begin funky code.  Nominal strings are sorted now by value, not by ordinal, so this must be consistent.
            if ( d1.le(d2) ) {
                return new DatumRange( d1, d2 );
            } else {
                return new DatumRange( d2, d1 );
            }
        } else {
            return new DatumRange( d1, d2 );
        }
    }

    /**
     * return the DatumRange equivalent of this 2-element, rank 1 bins dataset.  This uses the
     * sloppy mode, which does not check the BINS_0 property.
     *
     * @param ds a two-element dataset
     * @return a DatumRange.
     */
    public static DatumRange asDatumRange( QDataSet ds ) {
        return asDatumRange( ds, true );
    }

    /**
     * return DatumVector, which is a 1-d array of Datums.
     * @param ds a rank 1 QDataSet
     * @return a DatumVector
     */
    public static DatumVector asDatumVector( QDataSet ds ) {
        if ( ds.rank()!=1 ) throw new IllegalArgumentException("Rank must be 1");
        double[] dd= new double[ds.length()];
        for ( int i=0; i<dd.length; i++ ) {
            dd[i]= ds.value(i);
        }
        Units u= (Units) ds.property(QDataSet.UNITS);
        if ( u==null ) u= Units.dimensionless;
        return DatumVector.newDatumVector( dd, u );
    }
    
    /**
     * return the rank 1 dataset equivalent to the DatumVector
     * @param dv 
     * @return rank 1 QDataSet.
     */
    public static QDataSet asDataSet( DatumVector dv ) {
        DDataSet result= DDataSet.wrap(dv.toDoubleArray(dv.getUnits()));
        result.putProperty( QDataSet.UNITS, dv.getUnits() );
        return result;
    }
        
    /**
     * return a 2-element rank 1 bins dataset with BINS_0=QDataSet.VALUE_BINS_MIN_MAX
     * @param dr a Das2 DatumRange
     * @return a 2-element rank 1 bins dataset.
     */
    public static QDataSet asDataSet( DatumRange dr ) {
        DDataSet result= DDataSet.createRank1(2);
        Units u= dr.getUnits();
        result.putValue( 0, dr.min().doubleValue(u) );
        result.putValue( 1, dr.max().doubleValue(u) );
        result.putProperty( QDataSet.UNITS,u );
        result.putProperty( QDataSet.BINS_0, QDataSet.VALUE_BINS_MIN_MAX );
        return result;
    }

    /**
     * create a rank 0 dataset from the double and units.
     * @param d a double, or Double.NaN
     * @param u null or the units of d.
     * @return rank 0 dataset
     */
    public static DRank0DataSet asDataSet( double d, Units u ) {
        return DRank0DataSet.create(d,u);
    }

    /**
     * create a dimensionless rank 0 dataset from the double.
     * @param d a double, or Double.NaN
     * @return rank 0 dataset
     */
    public static DRank0DataSet asDataSet( double d ) {
        return DRank0DataSet.create(d);
    }

    /**
     * create a rank 0 dataset from the Datum.
     * @param d a datum
     * @return rank 0 dataset
     */
    public static DRank0DataSet asDataSet( Datum d ) {
        return DRank0DataSet.create(d);
    }

    /**
     * convert the QDataSet to an array.  Units and fill are ignored...
     * @param d the rank 1 dataset.
     * @return an array of doubles
     */
    public static double[] asArrayOfDoubles( QDataSet d ) {
        double[] result;
        if ( d.rank()==1 ) {
            DDataSet ds= (DDataSet)ArrayDataSet.maybeCopy( double.class, d );
            double[] back= ds.back;
            result= new double[d.length()];
            System.arraycopy( back, 0, result, 0, d.length() );
            
        } else {
            throw new IllegalArgumentException("only rank 1 supported");
        }
        
        return result;
    }

    /**
     * convert the QDataSet to an array.  Units and fill are ignored...
     * @param d the rank 2 dataset
     * @return 2-D array of doubles
     */
    public static double[][] as2DArrayOfDoubles( QDataSet d ) {
        double[][] result;
        if ( d.rank()==2 ) {
            DDataSet ds= (DDataSet)ArrayDataSet.maybeCopy( double.class, d );
            double[] back= ds.back;
            int l1=d.length(0);
            result= new double[d.length()][l1];
            for ( int i=0; i<ds.length(); i++ ) {
                System.arraycopy( back, i*l1, result[i], 0, l1 );
            }
            
        } else {
            throw new IllegalArgumentException("only rank 2 supported");
        }

        return result;
    }

    private static void flatten(double[][] data, double[] back, int offset, int nx, int ny) {
        for (int i = 0; i < nx; i++) {
            double[] dd = data[i];
            System.arraycopy(dd, 0, back, offset + i * ny, ny);
        }
    }

    private static void flatten(float[][] data, float[] back, int offset, int nx, int ny) {
        for (int i = 0; i < nx; i++) {
            float[] dd = data[i];
            System.arraycopy(dd, 0, back, offset + i * ny, ny);
        }
    }

    private static void flatten(long[][] data, long[] back, int offset, int nx, int ny) {
        for (int i = 0; i < nx; i++) {
            long[] dd = data[i];
            System.arraycopy(dd, 0, back, offset + i * ny, ny);
        }
    }

    private static void flatten(int[][] data, int[] back, int offset, int nx, int ny) {
        for (int i = 0; i < nx; i++) {
            int[] dd = data[i];
            System.arraycopy(dd, 0, back, offset + i * ny, ny);
        }
    }

    private static void flatten(short[][] data, short[] back, int offset, int nx, int ny) {
        for (int i = 0; i < nx; i++) {
            short[] dd = data[i];
            System.arraycopy(dd, 0, back, offset + i * ny, ny);
        }
    }

    private static void flatten(byte[][] data, byte[] back, int offset, int nx, int ny) {
        for (int i = 0; i < nx; i++) {
            byte[] dd = data[i];
            System.arraycopy(dd, 0, back, offset + i * ny, ny);
        }
    }
    /**
     * convert java arrays into QDataSets.
     * @param arr 1-D or 2-D array of java native type.
     * @return Rank 1 or Rank 2 dataset.
     */
    public static QDataSet asDataSet(Object arr) {
        if ( arr.getClass().isArray() ) {
            Class c=  arr.getClass().getComponentType();
            if ( c.isArray() ) {
                c= c.getComponentType();
                if ( c.isArray() ) {
                    throw new IllegalArgumentException("3-D arrays not supported");
                }
                int ny= (Array.getLength( Array.get(arr,0) ) );
                int nx= Array.getLength(arr);
                if ( c==double.class ) {
                    double[] dd= new double[nx*ny];
                    flatten( (double[][])arr, dd, 0, nx, ny );
                    return DDataSet.wrap(dd,nx,ny);
                } else if ( c==float.class ) {
                    float[] dd= new float[nx*ny];
                    flatten( (float[][])arr, dd, 0, nx, ny );
                    return FDataSet.wrap(dd,nx,ny);
                } else if ( c==long.class ) {
                    long[] dd= new long[nx*ny];
                    flatten( (long[][])arr, dd, 0, nx, ny );
                    return LDataSet.wrap(dd,nx,ny);
                } else if ( c==int.class ) {
                    int[] dd= new int[nx*ny];
                    flatten( (int[][])arr, dd, 0, nx, ny );
                    return IDataSet.wrap(dd,nx,ny);
                } else if ( c==short.class ) {
                    short[] dd= new short[nx*ny];
                    flatten( (short[][])arr, dd, 0, nx, ny );
                    return SDataSet.wrap(dd,nx,ny);
                } else if ( c==byte.class ) {
                    byte[] dd= new byte[nx*ny];
                    flatten( (byte[][])arr, dd, 0, nx, ny );
                    return BDataSet.wrap(dd,nx,ny);
                } else {
                    throw new IllegalArgumentException("Array component type not supported: "+c);
                }
            } else if ( c==double.class ) {
                return DDataSet.wrap((double[])arr);
            } else if ( c==float.class ) {
                return FDataSet.wrap((float[])arr);
            } else if ( c==long.class ) {
                return LDataSet.wrap((long[])arr);
            } else if ( c==int.class ) {
                return IDataSet.wrap((int[])arr);
            } else if ( c==short.class ) {
                return SDataSet.wrap((short[])arr);
            } else if ( c==byte.class ) {
                return BDataSet.wrap((byte[])arr);
            } else {
                throw new IllegalArgumentException("unsupported type: "+arr.getClass());
            }
        } else {
            if ( arr instanceof QDataSet ) {
                return (QDataSet)arr;
            } else if ( arr instanceof Datum ) {
                return asDataSet( (Datum)arr );
            } else if ( arr.getClass().isPrimitive() ) {
                return asDataSet( (Double)arr );
            } else if ( arr instanceof String ) {
                try {
                    return asDataSet(DatumUtil.parse((String) arr));
                } catch (ParseException ex) {
                    throw new IllegalArgumentException(ex);
                }
            }
            throw new IllegalArgumentException("unsupported type: "+arr.getClass());
        }
    }

    /**
     * convert java arrays into QDataSets.
     * @param x array, number, or String parsed with DatumUtil.parse
     * @param y array, number, or String parsed with DatumUtil.parse
     * @return dataset
     * @see org.das2.datum.DatumUtil#parse(java.lang.String) 
     */
    public static QDataSet asDataSet(Object x, Object y) {
        QDataSet xds= asDataSet(x);
        QDataSet yds= asDataSet(y);
        return Ops.link(xds, yds);
    }

    /**
     * convert java types into QDataSets.
     * @param x array, number, or String parsed with DatumUtil.parse
     * @param y array, number, or String parsed with DatumUtil.parse
     * @param z array, number, or String parsed with DatumUtil.parse
     * @return dataset
     */
    public static QDataSet asDataSet(Object x, Object y, Object z) {
        QDataSet xds= asDataSet(x);
        QDataSet yds= asDataSet(y);
        QDataSet zds= asDataSet(z);
        return Ops.link(xds, yds, zds);
    }

    /**
     * adds the rank 0 dataset (a Datum) to the dataset's properties, after all
     * the other CONTEXT_&lt;i&gt; properties.
     * @param ds
     * @param cds
     */
    public static void addContext( MutablePropertyDataSet ds, QDataSet cds ) {
        int idx=0;
        while ( ds.property("CONTEXT_"+idx)!=null ) idx++;
        ds.putProperty( "CONTEXT_"+idx, cds );
    }

    /**
     * adds the rank 0 dataset (a Datum) to the properties, after all
     * the other CONTEXT_&lt;i&gt; properties.
     * @param props the properties
     * @param cds the context in a rank 0 dataset
     */
    public static void addContext( Map<String,Object> props, QDataSet cds ) {
        int idx=0;
        while ( props.get("CONTEXT_"+idx)!=null ) idx++;
        props.put( "CONTEXT_"+idx, cds );
    }

    /**
     * provide the context as a string, for example to label a plot.  The dataset CONTEXT_i properties are inspected,
     * each of which must be one of:<ul>
     * <li>rank 0 dataset
     * <li>rank 1 bins dataset
     * <li>rank 1 bundle
     * </ul>
     * Here a comma is used as the delimiter.
     *
     * @param ds the dataset containing context properties which are rank 0 datums or rank 1 datum ranges.
     * @return a string describing the context.
     */
    public static String contextAsString( QDataSet ds ) {
        return contextAsString( ds, ", " );
    }

    /**
     * provide the context as a string, for example to label a plot.  The dataset CONTEXT_i properties are inspected,
     * each of which must be one of:<ul>
     * <li>rank 0 dataset
     * <li>rank 1 bins dataset
     * <li>rank 1 bundle
     * </ul>
     * @param ds the dataset containing context properties which are rank 0 datums or rank 1 datum ranges.
     * @param delim the delimiter between context elements, such as "," or "!c"
     * @return a string describing the context.
     */
    public static String contextAsString( QDataSet ds, String delim ) {
        StringBuilder result= new StringBuilder();
        QDataSet cds= (QDataSet) ds.property( QDataSet.CONTEXT_0 );
        logger.log(Level.FINE, "contextAsString {0} CONTEXT_0={1}", new Object[]{ds, cds});
        int idx=0;
        while ( cds!=null ) {
            if ( cds.rank()>0 ) {
                if ( cds.rank()==1 && cds.property(QDataSet.BINS_0)!=null ) {
                    if ( cds.value(1)-cds.value(0) > 0 ) {
                        QDataSet fcds= DataSetUtil.asDataSet( DatumRangeUtil.roundSections( DataSetUtil.asDatumRange( cds, true ), 1000 ) );
                        result.append( DataSetUtil.format(fcds,false) );
                    } else {
                        result.append( DataSetUtil.format(cds,false) );
                    }
                } else {
                    
                    QDataSet extent= Ops.extentSimple(cds,null,null);
                    if ( extent.value(1)==extent.value(0) ) {
                        result.append( DataSetUtil.format(cds.slice(0),false) );  // for CLUSTER/PEACE this happens where rank 1 context is all the same value
                    } else {
                        String name= (String) cds.property(QDataSet.NAME);
                        if ( name==null ) name="data";
                        String label= name + " varies from " + extent.slice(0) + " to "+ extent.slice(1);
                        result.append(label);
                        //result.append(DataSetUtil.format(extent, false)).append(" ").append( cds.length() ).append( " different values"); // slice was probably done when we should't have.
                    }
                }
            } else {
                result.append( DataSetUtil.format(cds,false) );
            }
            idx++;
            cds= (QDataSet) ds.property( "CONTEXT_"+idx );
            if ( cds!=null ) result.append(delim);
        }
        return result.toString();
    }
    
    /**
     * collect the context for the dataset.  This will be a rank 2 join of
     * scalars or ranges, so CONTEXT_0 will be in the zeroth index of the 
     * result.  Rank 0 CONTEXT values are joined to make them rank 1.
     * 
     * @param ds the dataset
     * @return a rank 2 join of 1- and 2-element datasets.
     */
    public static QDataSet getContext( QDataSet ds ) {
        JoinDataSet jds= new JoinDataSet(2);
        QDataSet cds= (QDataSet) ds.property( QDataSet.CONTEXT_0 );
        logger.log(Level.FINE, "contextAsString {0} CONTEXT_0={1}", new Object[]{ds, cds});
        int idx=0;
        while ( cds!=null ) {
            if ( cds.rank()==0 ) {
                cds= new JoinDataSet(cds);
            }
            jds.join(cds);
            idx++;
            cds= (QDataSet) ds.property( "CONTEXT_"+idx );
        }
        return jds;
    }
    
    /**
     * returns the indeces of the min and max elements of the monotonic dataset.
     * This uses DataSetUtil.isMonotonic() which would be slow if MONOTONIC is
     * not set.
     * @param ds monotonic, rank 1 dataset.
     * @return the indeces [min,max] note max is inclusive.
     * @see org.das2.qds.ops.Ops#extent which returns the range containing any data.
     * @see #isMonotonic(org.das2.qds.QDataSet) which must be true
     * @throws IllegalArgumentException when isMonotonic(ds) is false.
     */
    public static int[] rangeOfMonotonic( QDataSet ds ) {
        if ( ds.rank()!=1 ) throw new IllegalArgumentException("must be rank 1");
        if ( DataSetUtil.isMonotonic(ds) ) {
            QDataSet wds= DataSetUtil.weightsDataSet(ds);
            int firstValid= 0;
            while ( firstValid<wds.length() && wds.value(firstValid)==0 ) firstValid++;
            if ( firstValid==wds.length() ) throw new IllegalArgumentException("data contains no valid measurements");
            int lastValid=wds.length()-1;
            while ( lastValid>=0 && wds.value(lastValid)==0 ) lastValid--;
            if ( ( lastValid-firstValid+1 ) == 0 ) {
                throw new IllegalArgumentException("special case where monotonic dataset contains no valid data");
            }
            return new int[] { firstValid, lastValid };
        } else {
            throw new IllegalArgumentException("expected monotonic dataset");
        }
    }
    
    /**
     * returns the index of a tag, or the  <tt>(-(<i>insertion point</i>) - 1)</tt>.  (See Arrays.binarySearch)
     * @param ds monotonically increasing data.
     * @param datum value we are looking for
     * @param low inclusive lower bound of the search
     * @param high inclusive upper bound of the search
     * @return the index of a tag, or the  <tt>(-(<i>insertion point</i>) - 1)</tt>
     */
    public static int xTagBinarySearch( QDataSet ds, Datum datum, int low, int high ) {
        Units toUnits= SemanticOps.getUnits( ds );
        double key= datum.doubleValue(toUnits);
        if ( ds.rank()!=1 ) throw new IllegalArgumentException("data must be rank 1");
        if ( high>=ds.length() ) throw new IndexOutOfBoundsException("high index must be within the data");
        while (low <= high) {
            int mid = (low + high) >> 1;
            double midVal = ds.value(mid);
            int cmp;
            if (midVal < key) {
                cmp = -1;   // Neither val is NaN, thisVal is smaller
            } else if (midVal > key) {
                cmp = 1;    // Neither val is NaN, thisVal is larger
            } else {
                long midBits = Double.doubleToLongBits(midVal);
                long keyBits = Double.doubleToLongBits(key);
                cmp = (midBits == keyBits ?  0 : // Values are equal
                    (midBits < keyBits ? -1 : // (-0.0, 0.0) or (!NaN, NaN)
                        1));                     // (0.0, -0.0) or (NaN, !NaN)
            }

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found.
    }

    /**
     * returns the index of the closest index in the data. 
     * This supports rank 1 datasets, and rank 2 bins datasets where the bin is min,max.
     * 
     * @param ds tags dataset
     * @param datum the location to find
     * @return the index of the closest point.
     * @throws IllegalArgumentException if the dataset is not rank 1.
     * @throws IllegalArgumentException if the dataset is length 0.
     * @throws IllegalArgumentException if the dataset is all fill.
     */
    public static int closestIndex( QDataSet ds, Datum datum ) {
        logger.entering( CLASSNAME, "closestIndex" );
        if ( ds.rank()!=1 ) {
            if ( ds.rank()==2 && SemanticOps.isBins(ds) ) {
                ds= Ops.reduceMean(ds,1);
            } else {
                throw new IllegalArgumentException("ds rank should be 1");
            }
        }
        if ( ds.length()==0 ) {
            throw new IllegalArgumentException("ds length is zero");
        }
        
        boolean handleFill= false;
        QDataSet wds= Ops.valid(ds);
        QDataSet r;
        
        if ( UnitsUtil.isNominalMeasurement( datum.getUnits() ) ) {
            throw new IllegalArgumentException("datum cannot have ordinal units: "+datum );
        }
            
        if ( UnitsUtil.isNominalMeasurement( SemanticOps.getUnits(ds) ) ) {
            throw new IllegalArgumentException("ds cannot have ordinal units: "+ds );
        }

        boolean mono= isMonotonic(ds);
                
        if ( mono ) { // take a millisecond to check for this oft-occurring case.
            if ( wds.value(0)>0. && datum.le( asDatum(ds.slice(0)) ) ) {
                logger.exiting( CLASSNAME, "closestIndex" );
                return 0;
            }
            int n= ds.length()-1;
            if ( wds.value(n)>0. && datum.ge( asDatum(ds.slice(n)) ) ) {
                logger.exiting( CLASSNAME, "closestIndex" );
                return n;
            }
        }
        
        if ( wds instanceof ConstantDataSet && wds.value(0)==1 ) { // optimize
            r= null;
        } else {
            if ( DataSetAnnotations.VALUE_0.equals(DataSetAnnotations.getInstance().getAnnotation(ds,DataSetAnnotations.ANNOTATION_INVALID_COUNT)) ) { 
                r= null;
            } else {
                if ( ds instanceof IndexGenDataSet && wds instanceof org.das2.qds.WeightsDataSet.Finite ) { // this happens a lot.
                    DataSetAnnotations.getInstance().putAnnotation(ds,DataSetAnnotations.ANNOTATION_INVALID_COUNT, DataSetAnnotations.VALUE_0 );
                    r= null;
                } else {
                    r= Ops.where( wds );
                    if ( r.length()<ds.length() ) {
                        if ( r.length()==0 ) throw new IllegalArgumentException("dataset is all fill");
                        handleFill= true;
                        ds= DataSetOps.applyIndex( ds, 0, r, false );
                    } else {
                        DataSetAnnotations.getInstance().putAnnotation(ds,DataSetAnnotations.ANNOTATION_INVALID_COUNT, DataSetAnnotations.VALUE_0 );
                    }
                }
            }
        }
        
        double ddatum= datum.doubleValue( SemanticOps.getUnits(ds) );

        if ( !mono ) {
            int closest= 0;
            double v= Math.abs( ds.value(closest)-ddatum );
            for ( int i=1; i<ds.length(); i++ ) {
                double tv= Math.abs( ds.value(i)-ddatum );
                if ( tv < v ) {
                    closest= i;
                    v= tv;
                }
            }
            if ( handleFill ) {
                assert r!=null;
                closest= (int)r.value(closest);
            }
            logger.exiting( CLASSNAME, "closestIndex" );
            return closest;

        } else {

            int result= xTagBinarySearch( ds, datum, 0, ds.length()-1 );

            if (result == -1) {
                result = 0; //insertion point is 0
            } else if (result < 0) {
                result= ~result; // usually this is the case
                if ( result > ds.length()-1 ) {
                    result= ds.length()-1;
                } else {
                    double x= ddatum;
                    double x0= ds.value(result-1 );
                    double x1= ds.value(result );
                    result= ( ( x-x0 ) / ( x1 - x0 ) < 0.5 ? result-1 : result );
                }
            }
            if ( handleFill ) {
                assert r!=null;
                result= (int)r.value(result);
            }
            logger.exiting( CLASSNAME, "closestIndex" );
            return result;

        }
    }

    public static int closestIndex( QDataSet table, double x, Units units ) {
        return closestIndex( table, units.createDatum(x) );
    }

    /**
     * Returns the index of the value which is less than the
     * value less of the datum.  Note for rank 2 bins, the first bin which
     * has an end less than the datum.
     * @param ds rank 1 monotonic tags, or rank 2 bins.
     * @param datum a datum of the same or convertible units.
     * @return the index, or null (None).
     */
    public static Integer getPreviousIndexStrict( QDataSet ds, Datum datum ) {
        if ( ds.length()==0 ) return null;
        if ( SemanticOps.isBins(ds) && ds.rank()==2 ) { // BINS SCHEME
            ds= Ops.slice1( ds, 1 );
        }
        int i= getPreviousIndex( ds, datum );
        if ( Ops.gt( ds.slice(i), datum ).value()>0. ) {
            return null;
        }  else {
            return i;
        }
    }

    /**
     * Returns the index of the value which is greater than the
     * value less of the datum.  Note for rank 2 bins, the first bin which
     * has an beginning less than the datum.
     * @param ds rank 1 monotonic tags, or rank 2 bins.
     * @param datum a datum of the same or convertible units.
     * @return the index, or null (None).
     */
    public static Integer getNextIndexStrict( QDataSet ds, Datum datum ) {
        if ( ds.length()==0 ) return null;
        if ( SemanticOps.isBins(ds) && ds.rank()==2 ) { // BINS SCHEME
            ds= Ops.slice1( ds, 0 );
        }
        int i= getNextIndex( ds, datum );
        if ( Ops.le( ds.slice(i), datum ).value()>0. ) {
            return null;
        }  else {
            return i;
        }
    }
    
    /**
     * returns the first index that is before the given datum, or zero
     * if no data is found before the datum.
     * PARV!
     * if the datum identifies (==) an xtag, then the previous column is
     * returned.
     * @param ds the dataset
     * @param datum a datum in the same units of the dataset.
     * @return the index 
     */
    public static int getPreviousIndex( QDataSet ds, Datum datum ) {
        int i= closestIndex( ds, datum );
        Units dsUnits= SemanticOps.getUnits(ds);
        // TODO: consider the virtue of ge
        if ( i>0 && ds.value(i)>=(datum.doubleValue(dsUnits)) ) {
            return i-1;
        } else {
            return i;
        }
    }

    /**
     * returns the first column that is after the given datum.  Note the
     * if the datum identifies (==) an xtag, then the previous column is
     * returned.
     * @param ds the dataset
     * @param datum a datum in the same units of the dataset.
     * @return 
     */
    public static int getNextIndex( QDataSet ds, Datum datum ) {
        int i= closestIndex( ds, datum );
        Units dsUnits= SemanticOps.getUnits(ds);
        // TODO: consider the virtue of le
        if ( i<ds.length()-1 && ds.value(i)<=(datum.doubleValue(dsUnits)) ) {
            return i+1;
        } else {
            return i;
        }
    }
    
    /**
     * 
     * @param bounds rank 1, two-element bounds
     * @return true if the bounds are valid.
     */
    private static boolean validBounds( QDataSet bounds ) {
        QDataSet wds= DataSetUtil.weightsDataSet(bounds);
        return !(wds.value(0)==0 || wds.value(1)==0);
    }
    
    /**
     * return the next interval (typically time) containing data, centered on data, with the
     * roughly the same width.
     * @param ds the dataset
     * @param dr0 the current interval
     * @return the next interval
     */
    public static DatumRange getNextInterval( QDataSet ds, DatumRange dr0 ) {
        DatumRange dr= dr0;
        int count; // limits the number of steps we can take.
        int STEP_LIMIT=10000;
        if ( ds==null ||  ds.rank()==0 || !UnitsUtil.isIntervalOrRatioMeasurement(SemanticOps.getUnits(ds) ) ) {
            dr= dr.next();
        } else {
            try {
                QDataSet bounds;
                QDataSet xtags= SemanticOps.xtagsDataSet(ds);
                if ( xtags!=null ) {
                    bounds= SemanticOps.bounds(xtags).slice(1);
                } else {
                    bounds= SemanticOps.bounds(ds).slice(0);
                }
                if ( !validBounds(bounds) 
                        || !SemanticOps.getUnits(bounds).isConvertibleTo(dr.getUnits() ) 
                        || DataSetUtil.asDatumRange(bounds).max().lt(dr.min()) ) {
                    
                    dr= dr.next();
                } else {
                    DatumRange limit= DataSetUtil.asDatumRange(bounds);
                    if ( !DatumRangeUtil.isAcceptable(limit,false) ) {
                        throw new IllegalArgumentException("limit is not acceptable"); // see 10 lines down
                    }
                    limit= DatumRangeUtil.union( limit, dr0 );
                    dr= dr.next();
                    count= 0;
                    while ( dr.intersects(limit) ) {
                        count++;
                        if ( count>STEP_LIMIT ) {
                            logger.warning("step limit in nextprev https://sourceforge.net/p/autoplot/bugs/1209/");
                            dr= dr0.next();
                            break;
                        }                        
                        QDataSet ds1= SemanticOps.trim( ds, dr, null);
                        if ( ds1==null || ds1.length()==0 ) {
                            logger.log(Level.FINER, "no records found in range ({0} steps): {1}", new Object[] { count, dr } );
                            dr= dr.next();
                        } else {
                            logger.log(Level.FINE, "found next data after {0} steps", count);
                            
                            QDataSet box= SemanticOps.bounds(ds1);
                            
                            DatumRange xdr;
                            if ( SemanticOps.isRank2Waveform(ds1) ) {
                                xdr = DataSetUtil.asDatumRange(box.slice(0));
                                DatumRange ddr = DataSetUtil.asDatumRange(box.slice(1));
                                xdr = DatumRangeUtil.union( xdr.min().subtract(ddr.min()), xdr.max().add(ddr.max()) );
                            } else {
                                xdr= DataSetUtil.asDatumRange(box.slice(0));
                            }
                            if ( xdr.width().lt( dr0.width() ) ) {
                                dr= DatumRangeUtil.createCentered( xdr.middle(), dr.width() );
                            }
//                            
//                            if ( DatumRangeUtil.normalize( dr, xdr.min() ) > 0.2 && DatumRangeUtil.normalize( dr, xdr.max() ) < 0.8 ) {
//                                dr= DatumRangeUtil.createCentered( xdr.middle(), dr.width() );
//                                logger.log(Level.FINE, "recenter the data" );
//                            }
//                            
                            break;
                        }
                    }
                }
            } catch ( InconvertibleUnitsException ex ) {
                logger.log(Level.FINE, ex.getMessage() );
                dr= dr.next();
            } catch ( IllegalArgumentException ex ) {
                logger.log(Level.FINE, ex.getMessage() );
                dr= dr.next();
            }
        }
        
        // check to see if "next" is basically the same thing as "scan"
        DatumRange intersectionTest;
        try {
            DatumRange nextInterval= dr0.next();
            intersectionTest= nextInterval.intersection(dr);
            double percentOverlap= intersectionTest.width().divide( nextInterval.width() ).value();
            if ( percentOverlap>0.9  ) {
                dr= nextInterval;
            } 
        } catch ( IllegalArgumentException ex ) {
            // do nothing, the found range is clearly better.
        }

        return dr;
    }

    /**
     * return the number of times a gap in data occurs.  This considers the cadence
     * and waveform-type data.  If the data cadence cannot be determined, then
     * return 0.
     * @param ds
     * @return the number of data breaks
     */
    private static int countDataBreaks( QDataSet ds ) {
        if ( ds.rank()==2 && SemanticOps.isRank2Waveform(ds) ) {
            int gaps= 0;
            QDataSet lastBounds=null;
            QDataSet xtags= Ops.xtags(ds);
            for ( int i=0; i<ds.length(); i++ ) {
                QDataSet ds1= ds.slice(i);
                QDataSet xtags1= Ops.xtags(ds1);
                QDataSet cadence= DataSetUtil.guessCadence( xtags1, ds1 );
                if ( cadence==null ) return 0;
                QDataSet bounds= Ops.add( Ops.extent(xtags1), xtags.slice(i) );
                gaps+= countDataBreaks( ds1 );
                if ( lastBounds!=null ) {
                    QDataSet lastX= lastBounds.slice(1);
                    QDataSet firstX= bounds.slice(0);
                    if ( Ops.gt( Ops.subtract(firstX, lastX), cadence ).value()>0 ) {
                        gaps+=1;
                    }
                }
                lastBounds= bounds;
            }
            return gaps;            
        } else if ( ds.rank()>1 ) {
            int gaps= 0;
            QDataSet lastBounds=null;
            for ( int i=0; i<ds.length(); i++ ) {
                QDataSet ds1= ds.slice(i);
                QDataSet xtags1= Ops.xtags(ds1);
                QDataSet cadence= DataSetUtil.guessCadence( xtags1, ds1 );
                if ( cadence==null ) return 0;
                QDataSet bounds= Ops.extent(xtags1);
                gaps+= countDataBreaks( ds1 );
                if ( lastBounds!=null ) {
                    QDataSet lastX= lastBounds.slice(1);
                    QDataSet firstX= bounds.slice(0);
                    if ( Ops.gt( Ops.subtract(firstX, lastX), cadence ).value()>0 ) {
                        gaps+=1;
                    }
                }
                lastBounds= bounds;
            }
            return gaps;
        } else {
            QDataSet xtags= Ops.xtags(ds);
            QDataSet cadence= DataSetUtil.guessCadence( xtags, ds );
            if ( cadence==null ) return 0; 
            QDataSet r= Ops.where( Ops.gt( Ops.diff( xtags ), cadence ) );
            int gaps= r.length();
            return gaps;
        }
    }
    
    /**
     * is data found at the time t, or might it be within a gap in the data?
     * @param ds any time-series dataset
     * @param t the time
     * @return true if t is within data.
     */
    public static boolean isDataAt( QDataSet ds, Datum t ) {
        QDataSet tt= Ops.xtags(ds);
        if ( tt==null ) throw new IllegalArgumentException("data must have timetags");
        if ( ds.rank()>2 ) {
            for ( int i=0; i<ds.length(); i++ ) {
                if ( isDataAt(ds.slice(i),t) ) {
                    return true;
                }
            }
            return false;
        } else if ( SemanticOps.isRank2Waveform(ds) ) {
            QDataSet ff= Ops.findex( tt, t );
            int i= (int)ff.value();
            if ( i<0 ) i=0;
            QDataSet ds1= ds.slice(i);
            QDataSet tt1= Ops.xtags(ds1);
            if ( tt1==null ) throw new IllegalArgumentException("data slices must have timetags");
            QDataSet bounds= Ops.extent(tt1);
            DatumRange xbounds= Ops.datumRange( Ops.add( bounds, tt.slice(i) ) );
            if ( t.le( xbounds.min() ) ) {
                return false;
            } else if ( t.ge( xbounds.max() ) ) {
                return false;
            } else {
                return isDataAt( ds1,t.subtract(Ops.datum(tt.slice(i))) );
            }
        } else {
            QDataSet ff= Ops.findex( tt, t );        
            QDataSet cadence= guessCadence( tt, ds );
            if ( cadence==null ) return true;
            Datum dcadence= Ops.datum(cadence).divide(1.95);
            int i= (int)ff.value();
            if ( i>0 ) {
                if ( t.minus( Ops.datum(tt.slice(i)) ).abs().lt(dcadence) ) {
                    return true;
                } else if ( i+1<tt.length() && Ops.datum(tt.slice(i+1)).minus(t).abs().lt(dcadence) ) {
                    return true;
                } else {
                    return false;
                }
            }
            return false;
        }
    }
    
    /**
     * return the previous interval (typically time) containing data, centered on data, with the
     * roughly the same width.
     * @param ds the dataset
     * @param dr0 the current interval
     * @return the previous interval
     */
    public static DatumRange getPreviousInterval( QDataSet ds, DatumRange dr0 ) {
        DatumRange dr= dr0;
        int count; // limits the number of steps we can take.
        int STEP_LIMIT=10000;
        if ( ds==null ||  ds.rank()==0 || !UnitsUtil.isIntervalOrRatioMeasurement(SemanticOps.getUnits(ds) ) ) {
            dr= dr.previous();
        } else {
            try {
                QDataSet bounds;
                QDataSet xtags= SemanticOps.xtagsDataSet(ds);
                if ( xtags!=null ) {
                    bounds= SemanticOps.bounds(xtags).slice(1);
                } else {
                    bounds= SemanticOps.bounds(ds).slice(0);
                }
                if ( !validBounds(bounds) 
                        || !SemanticOps.getUnits(bounds).isConvertibleTo(dr.getUnits() )
                        || DataSetUtil.asDatumRange(bounds).min().gt(dr.max()) ) {
                    dr= dr.previous();
                } else {
                    DatumRange limit= DataSetUtil.asDatumRange(bounds);
                    if ( !DatumRangeUtil.isAcceptable(limit,false) ) {
                        throw new IllegalArgumentException("limit is not acceptable"); // see 10 lines down
                    }
                    limit= DatumRangeUtil.union( limit, dr0 );
                    dr= dr.previous();
                    count= 0;
                    while ( dr.intersects(limit) ) {
                        count++;
                        if ( count>STEP_LIMIT ) {
                            logger.warning("step limit in nextprev https://sourceforge.net/p/autoplot/bugs/1209/");
                            dr= dr0.previous();
                            break;
                        }
                        QDataSet ds1= SemanticOps.trim( ds, dr, null);
                        if ( ds1==null || ds1.length()==0 ) {
                            logger.log(Level.FINER, "no records found in range ({0} steps): {1}", new Object[] { count, dr } );
                            dr= dr.previous();
                        } else {
                            logger.log(Level.FINE, "found previous data after {0} steps", count);
                            
                            QDataSet box= SemanticOps.bounds(ds1);
                            
                            boolean doRecenter= false;
                            DatumRange xdr;
                            if ( SemanticOps.isRank2Waveform(ds1) ) {
                                xdr = DataSetUtil.asDatumRange(box.slice(0));
                                DatumRange ddr = DataSetUtil.asDatumRange(box.slice(1));
                                xdr = DatumRangeUtil.union( xdr.min().subtract(ddr.min()), xdr.max().add(ddr.max()) );
                                int dataBreaks= countDataBreaks(ds1);
                                if ( dataBreaks<4 ) doRecenter= true;
                            } else {
                                xdr= DataSetUtil.asDatumRange(box.slice(0));
                            }
                            if ( xdr.width().lt( dr0.width() ) && doRecenter ) {
                                dr= DatumRangeUtil.createCentered( xdr.middle(), dr.width() );
                            }
                            
                            break;
                        }
                    }
                }
            } catch ( InconvertibleUnitsException ex ) {
                logger.log(Level.FINE, ex.getMessage() );
                dr= dr.previous();
            } catch ( IllegalArgumentException ex ) {
                logger.log(Level.FINE, ex.getMessage() );
                dr= dr.previous();
            }
        }
        
        // check to see if "next" is basically the same thing as "scan"
        DatumRange intersectionTest;
        try {
            DatumRange previousInterval= dr0.previous();
            intersectionTest= previousInterval.intersection(dr);
            double percentOverlap= intersectionTest.width().divide( previousInterval.width() ).value();
            if ( percentOverlap>0.9  ) {
                dr= previousInterval;
            } 
        } catch ( IllegalArgumentException ex ) {
            // do nothing, the found range is clearly better.
        }
        
        return dr;
    }
    
    public static Object getProperty( QDataSet ds, String name, Object deflt ) {
        Object o= ds.property(name);
        if ( o!=null ) return o; else return deflt;
    }


    /**
     * return the value, which gets units from index i. from rank 1 bundle dataset.
     * @param ds the dataset providing units and format information.
     * @param value double value from the dataset
     * @param i the index of the value
     * @return
     */
    public static String getStringValue( QDataSet ds, double value, int i ) {
        QDataSet bds= (QDataSet) ds.property(QDataSet.BUNDLE_0);
        if ( bds!=null ) {
            Units u= (Units) bds.property(QDataSet.UNITS,i);
            if ( u==null ) u=Units.dimensionless;
            Datum d= u.createDatum(value);
            return d.toString();
        } else {
            return DataSetUtil.getStringValue( ds, value );
        }
    }

    /**
     * return a DatumFormatter that can accurately format all of the datums
     * in the dataset.  The goal is to identify a formatter which is also 
     * efficient, and doesn't waste an excess of digits.  This sort of code
     * is often needed, and is also found in: <ul>
     * <li>QStream--where ASCII mode needs efficient representation
     * </ul>
     * TODO: make one code for this.
     * TODO: there also needs to be an optional external context ('2017-03-15') so that 'HH:mm' is a valid response.
     * See sftp://jbf@jfaden.net/home/jbf/ct/autoplot/script/development/bestDataSetFormatter.jy
     * @param datums a rank 1 dataset, or if rank&gt;1, then return the formatter for a slice.
     * @return DatumFormatter for the dataset.
     */
    public static DatumFormatter bestFormatter( QDataSet datums ) {
        if ( datums.rank()==0 ) {
            return bestFormatter( Ops.join(null,datums) );
        } else if ( datums.rank()>1 ) {
            if ( datums.rank()==2 && datums.property(QDataSet.BINS_1)!=null ) {
                
            } else {
                //TODO: find formatter for each, and then reconcile.
                return bestFormatter( datums.slice(0) );
            }
        }
        
        Units units= SemanticOps.getUnits(datums);
        if ( Schemes.isBundleDataSet(datums) && datums.length()>0 ) {
            throw new IllegalArgumentException("dataset is a bundle");
        }
        
        if ( units instanceof EnumerationUnits ) {
            return EnumerationDatumFormatterFactory.getInstance().defaultFormatter();
        } else if ( units instanceof TimeLocationUnits ) {
            Datum gcd= asDatum( gcd( Ops.subtract( datums, datums.slice(0) ), DataSetUtil.asDataSet( Units.microseconds.createDatum(1) ) ) );
            try {
                if ( gcd.lt( Units.nanoseconds.createDatum(1) ) ) {
                    return new TimeDatumFormatter("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSSSS)");
                } else if ( gcd.lt( Units.nanoseconds.createDatum(1000) ) ) {
                    return new TimeDatumFormatter("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS)");
                } else if ( gcd.lt( Units.microseconds.createDatum(1000) ) ) {
                    return new TimeDatumFormatter("yyyy-MM-dd'T'HH:mm:ss.SSSSSS)");
                } else if ( gcd.lt( Units.milliseconds.createDatum(1000) ) ) {
                    return new TimeDatumFormatter("yyyy-MM-dd'T'HH:mm:ss.SSS)");
                } else if ( gcd.lt( Units.seconds.createDatum(60) ) ) {
                    return new TimeDatumFormatter("yyyy-MM-dd'T'HH:mm:ss.SSS)");
                } else if ( gcd.lt( Units.seconds.createDatum(600) ) ) {
                    return new TimeDatumFormatter("yyyy-MM-dd'T'HH:mm:ss)");
                } else {
                    return new TimeDatumFormatter("yyyy-MM-dd'T'HH:mm");
                }
            } catch ( ParseException ex ) {
                throw new RuntimeException(ex);
            }
        } else if ( units instanceof LocationUnits ) {
            units= units.getOffsetUnits();
            datums= Ops.subtract( datums, datums.slice(0) );
        } 
        
        QDataSet limit= Ops.dataset( Math.pow( 10, (int)Math.log10( Ops.reduceMax( datums, 0 ).value() ) - 7 ), units );
        datums= Ops.round( Ops.divide( datums, limit ) );
        QDataSet gcd;
        try {
            gcd= gcd( datums, asDataSet(1.0) );
            datums= Ops.multiply( datums, limit );
            gcd= Ops.multiply( gcd, limit );
        } catch ( IllegalArgumentException ex ) { // java.lang.IllegalArgumentException: histogram has too few bins
            gcd= limit;
            datums= Ops.multiply( datums, limit );
        }
        
        int smallestExp=99;
        int ismallestExp=-1;
        for ( int j=0; j<datums.length(); j++ ) {
            double d= datums.value(j);
            if ( Math.abs(d)>(gcd.value()*0.1) ) { // don't look at fuzzy zero
                int ee= (int)Math.floor(0.05+Math.log10(Math.abs(d)));
                if ( ee<smallestExp ) {
                    smallestExp=ee;
                    ismallestExp= j;
                }
            }
        }
        
        Datum resolution= asDatum(gcd);
        Datum base= asDatum( datums.slice(ismallestExp) );
        if ( base.lt(units.createDatum(0.) ) ) {
            base= base.multiply(-1);
        }
        return DatumUtil.bestFormatter( base, base.add( resolution ), 1 );
    }    
    

    /**
     * return the string value of the double, considering QDataSet.FORMAT, the units and the value.
     * formatting is done assuming someone else will report the units.  If the value is invalid,
     * then "***" is returned.
     * @param yds the dataset, maybe with FORMAT and VALID_MIN, etc.
     * @param value the double from the dataset (presumably).
     * @return human-readable string representation.
     */
    public static String getStringValue( QDataSet yds, double value ) {
        Units u= SemanticOps.getUnits(yds);
        String form= (String)yds.property(QDataSet.FORMAT);
        Datum d;

        double vmin= ((Number) getProperty( yds, QDataSet.VALID_MIN, -1*Double.MAX_VALUE )).doubleValue();
        double vmax= ((Number) getProperty( yds, QDataSet.VALID_MAX, Double.MAX_VALUE )).doubleValue();
        double fill= ((Number) getProperty( yds, QDataSet.FILL_VALUE, -1e31 ) ).doubleValue();
        if ( value>=vmin && value<=vmax && value!=fill ) {
            d= u.createDatum( value );
        } else {
            //TODO: consider using format length and "****" to return value.
            return "****";
        }
        DatumFormatter df= d.getFormatter();
        String s;
        if ( df instanceof DefaultDatumFormatter ) {
            if ( form==null || form.trim().length()==0 ) {
                if ( u==Units.rgbColor ) {
                    s= ColorUtil.encodeColor( new Color((int)value) );
                } else if ( "log".equals( yds.property(QDataSet.SCALE_TYPE) ) ) {
                    s = String.format( Locale.US, "%9.3e", value ).trim();
                } else {
                    QDataSet bounds=null;
                    if ( yds.rank()>0 ) bounds= SemanticOps.bounds(yds);
                    if ( bounds!=null && bounds.rank()==2 ) {
                        if ( Math.abs(bounds.value(1,0))<0.01 || Math.abs(bounds.value(1,1))<0.01 ) {
                            s = String.format( Locale.US, "%9.3e", value ).trim();
                        } else {
                            s = String.format( Locale.US, "%9.3f", value ).trim();
                        }
                    } else {
                        s = String.format( Locale.US, "%9.3f", value ).trim();
                    }
                    
                }
            } else {
                if ( UnitsUtil.isTimeLocation(u) ) {
                    TimeParser tp= TimeParser.create(form);
                    return tp.format(d);
                } else {
                    try {
                        if ( form.equals("%d") ) {
                            s= String.format( Locale.US, "%d", (int)value ) ;
                        } else {
                            s = String.format( Locale.US, form, value );
                        }
                    } catch ( IllegalFormatConversionException ex ) { // '%2X'
                        char c= ex.getConversion();
                        if ( c=='d' || c=='X' || c=='x' || c=='o' || c=='c' || c=='C'  ) {
                            s = String.format( Locale.US, form, (long)value );
                        } else {
                            //warning bad format string
                            logger.log(Level.INFO, "unable to use format: {0}", ex.getMessage());
                            s= df.format(d);
                        }
                    } catch ( java.util.UnknownFormatConversionException ex ) {
                        logger.log(Level.INFO, "unable to use format: {0}", ex.getMessage());
                        s= df.format(d,d.getUnits());
                    } catch ( java.util.IllegalFormatPrecisionException ex ) {
                        logger.log(Level.INFO, "unable to use format: {0}", ex.getMessage());
                        s= df.format(d,d.getUnits());
                    }
                }
            }
        } else {
            if ( UnitsUtil.isTimeLocation(u) ) {
                if ( form==null ) {
                    QDataSet c= (QDataSet) yds.property( QDataSet.CADENCE );
                    double ns;
                    if ( c==null ) {
                        if ( yds.length()>50 && yds.rank()==1 ) {
                            try { 
                                c= DataSetUtil.guessCadence(yds.trim(0,50),null);
                            } catch ( RuntimeException ex ) {
                                if ( yds.length()>2 ) {
                                    int n= yds.length();
                                    c= DataSetUtil.asDataSet(u.getOffsetUnits().createDatum( yds.value(n-1)-yds.value(0) ).divide(n));

                                } else {
                                    c= null;
                                }
                            }
                            if ( c==null ) {
                                return df.format(d,u);
                            } else {
                                ns = Datum.create( c.value(), SemanticOps.getUnits(c) ).doubleValue( Units.nanoseconds );
                            }
                        } else {
                            return df.format(d,u);
                        }
                    } else {
                        ns= Datum.create( c.value(), SemanticOps.getUnits(c) ).doubleValue( Units.nanoseconds );
                    }
                    if ( ns<50000 ) {
                        TimeParser tp= TimeParser.create("$Y-$m-$dT$H:$M:$S.$(subsec,places=9)Z");
                        return tp.format(d);
                    } else if ( ns<50000000 ) {
                        TimeParser tp= TimeParser.create("$Y-$m-$dT$H:$M:$S.$(subsec,places=6)Z");
                        return tp.format(d);
                    }
                } else {
                    TimeParser tp= TimeParser.create(form);
                    return tp.format(d); 
                }
            }
            s = df.format(d,u);
        }
        return s;
    }

    /**
     * Return just the value encoded as richly as possible, for human consumption.
     * Example results of this include "9.5 km" "Chicago" or "fill"
     * see also format(ds), toString(ds), getStringValue(ds,d)
     * @param ds
     * @return just the value, without labels.
     */
    public static String getStringValue( QDataSet ds ) {
        if ( ds.rank()==0 ) {
            return getStringValue( ds, ds.value() );
        } else {
            return format(ds);
        }
    }
    
    
    /**
     * make a proper bundle ds from a simple bundle containing ordinal units
     * This assumes that labels is a unique set of labels.
     * See http://autoplot.org/QDataSet#DataSet_Properties under BUNDLE_1.
     * See DataSetOps.unbundle
     * @param labels
     * @throws IllegalArgumentException if the input is not rank 1.
     * @return a BundleDescriptor to be set as BUNDLE_i.  See BundleDataSet
     */
    public static MutablePropertyDataSet toBundleDs( QDataSet labels ) {
        if ( labels.rank()!=1 ) throw new IllegalArgumentException("labels must be rank 1");
        IDataSet result=  IDataSet.createRank2( labels.length(), 0 );
        Units u= SemanticOps.getUnits(labels);
        for ( int i=0; i<labels.length(); i++ ) {
            String label= u.createDatum( labels.value(i) ).toString();
            result.putProperty( QDataSet.NAME, i, Ops.safeName( label ) );
            result.putProperty( QDataSet.LABEL, i, label );
        }
        String name= (String) labels.property(QDataSet.NAME);
        if ( name!=null ) result.putProperty( QDataSet.NAME, name );
        String label= (String) labels.property(QDataSet.LABEL);
        if ( label!=null ) result.putProperty( QDataSet.LABEL, label );

        return result;
    }

    /**
     * return the name for each column of the bundle.  This simply
     * calls SemanticOps.getComponentNames.
     * @param ds dataset, presumably with BUNDLE_1 property.
     * @return array of names.
     */
    public static String[] bundleNames(QDataSet ds) {
        return SemanticOps.getComponentNames(ds);
    }

    /**
     * verify that the indeces really are indeces, and a warning may be printed
     * if the indeces don't appear to match the array by
     * looking at the MAX_VALUE property of the indeces.
     * 
     * @param ds the dataset. 
     * @param indices the indeces which refer to a subset of dataset.
     */
    public static void checkListOfIndeces(QDataSet ds, QDataSet indices) {
        Units u= (Units) indices.property(QDataSet.UNITS);
        if ( u!=null && !u.isConvertibleTo(Units.dimensionless ) ) {
            throw new IllegalArgumentException("indices must not contain units");
        }
        if ( ds.rank()==1 ) {
            Number len= (Number) indices.property(QDataSet.VALID_MAX);
            if ( len!=null ) {
                if ( len.intValue()!=ds.length() ) {
                    logger.warning("indices appear to be from a dataset with different length");
                }
            }
        }
    }

    /**
     * return 0 or the number of values after which the signal repeats.  For example,
     * <code>
     * ds= dataset([1,2,3,4,1,2,3,4,1,2,3,4,1,2,3,4,1,2])
     * assert repeatingSignal(ds)==4
     * </code>
     * if the data contains fill, then it is also not repeating.
     * @param yds the rank 1 dataset.
     * @return the number of samples before a repeat, or 0 if the signal is not repeating.
     * @see #guessCadenceNew
     */
    public static int repeatingSignal(QDataSet yds) {
        QDataSet valid= Ops.valid(yds);
        if ( valid.value(0)==0 ) {
            return 0; 
        }
        double lookfor= yds.value(0);
        int candidate=0;
        for ( int i=1; i<yds.length(); i++ ) {
            if ( yds.value(i)==lookfor ) {
                candidate= i;
                break;
            }
        }
        for ( int i=candidate; i<yds.length(); i++ ) {
            if ( yds.value(i)!=yds.value(i-candidate) ) {
                return 0;
            }
        }
        return candidate;
    }

}

