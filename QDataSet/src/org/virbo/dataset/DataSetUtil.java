/*
 * DataSetUtil.java
 *
 * Created on April 1, 2007, 4:28 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.virbo.dataset;

import java.util.ArrayList;
import java.util.Calendar;
import org.das2.datum.Units;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.UnitsConverter;
import org.das2.datum.UnitsUtil;
import org.virbo.dsops.Ops;
import org.virbo.dsutil.AutoHistogram;

/**
 *
 * @author jbf
 */
public class DataSetUtil {

    /**
     * creates a dataset of integers 0,1,2,...,n-1.
     */
    public static MutablePropertyDataSet indexGenDataSet(int n) {
        return new IndexGenDataSet(n);
    }

    /**
     * creates a dataset with the given cadence, start and length.
     * This is danger code, because the CADENCE must be reset if the UNITS are reset.
     * use tagGenDataSet( int n, final double start, final double cadence, Units units ) if
     * units are going to be specified.
     */
    public static MutablePropertyDataSet tagGenDataSet(int n, final double start, final double cadence) {
        IndexGenDataSet result = new IndexGenDataSet(n) {
            public double value(int i) {
                return i * cadence + start;
            }
        };
        result.putProperty( QDataSet.CADENCE, DRank0DataSet.create(cadence) );
        if ( cadence<0 ) result.putProperty( QDataSet.MONOTONIC, Boolean.FALSE );
        return result;
    }

    /**
     * creates a dataset with the given cadence, start and length.  QDataSet.CADENCE
     * will be set based on the units specified.  Do not change the units of the
     * result without updating cadence as well.
     */
    public static MutablePropertyDataSet tagGenDataSet(int n, final double start, final double cadence, Units units ) {
        IndexGenDataSet result = new IndexGenDataSet(n) {
            public double value(int i) {
                return i * cadence + start;
            }
        };
        if ( units!=null ) {
            result.putProperty( QDataSet.CADENCE, DRank0DataSet.create(cadence,units.getOffsetUnits()) );
            result.putProperty( QDataSet.UNITS, units );
        } else {
            result.putProperty( QDataSet.CADENCE, DRank0DataSet.create(cadence) );
        }
        
        if ( cadence<0 ) result.putProperty( QDataSet.MONOTONIC, Boolean.FALSE );
        return result;
    }

    /**
     * creates a dataset with the given cadence, start and length.
     */
    public static MutablePropertyDataSet replicateDataSet(int n, final double value) {
        IndexGenDataSet result = new IndexGenDataSet(n) {

            public double value(int i) {
                return value;
            }
        };
        return result;
    }

    /**
     * returns true if the dataset is monotonically increasing.
     * If the dataset says it's monotonic, believe it.
     * An empty dataset is not monotonic.
     * We now use a weights dataset to more thoroughly check for fill.
     * The dataset may now contain fill data.
     * See QDataSet.MONOTONIC.
     */
    public static boolean isMonotonic(QDataSet ds) {
        if (ds.rank() != 1) { // TODO: support bins dataset rank 2 with BINS_1="min,max"
            return false;
        }

        if (ds.length() == 0) {
            return false;
        }

        if (Boolean.TRUE.equals(ds.property(QDataSet.MONOTONIC))) {
            return true;
        }

        QDataSet wds= DataSetUtil.weightsDataSet(ds);
        int i = 0;

        for ( i=0; i<ds.length() && wds.value(i)==0; i++ ) {
            // find first valid point.
        }

        if ( i==ds.length() ) {
            return false;
        }

        double last = ds.value(i);

        for ( i = i+1; i < ds.length(); i++) {
            double d = ds.value(i);
            double w = wds.value(i);
            if ( w==0 ) continue;
            if ( d < last  ) {
                return false;
            } 
            last = d;
        }
        return true;
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
     * which must either be null or a Map<String,Object>.
     * @param ds  The dataset containing the property.
     * @param name  The name of the user property.
     * @return
     */
    public static Object getUserProperty( QDataSet ds, String name ) {
        Map<String,Object> userProps= (Map<String, Object>) ds.property(QDataSet.USER_PROPERTIES);
        if ( userProps==null ) return null;
        return userProps.get(name);
    }

    public static String[] propertyNames() {
        return new String[]{
                    QDataSet.UNITS, 
                    QDataSet.VALID_MIN, QDataSet.VALID_MAX,
                    QDataSet.FILL_VALUE,
                    QDataSet.FORMAT, QDataSet.CADENCE,
                    QDataSet.MONOTONIC, QDataSet.SCALE_TYPE,
                    QDataSet.TYPICAL_MIN, QDataSet.TYPICAL_MAX, QDataSet.RENDER_TYPE,
                    QDataSet.QUBE,
                    QDataSet.NAME, QDataSet.LABEL, QDataSet.TITLE,
                    QDataSet.CACHE_TAG,
                    QDataSet.COORDINATE_FRAME,
                    QDataSet.DELTA_MINUS, QDataSet.DELTA_PLUS,
                    QDataSet.USER_PROPERTIES,
                    QDataSet.METADATA, QDataSet.METADATA_MODEL,
                };
    }

    /**
     * copy over all the dimension properties.
     * This DOES NOT support join datasets yet.
     * @param source
     * @param dest
     */
    public static void copyDimensionProperties( QDataSet source, MutablePropertyDataSet dest ) {
        String[] names= dimensionProperties();
        for ( String n: names ) {
            Object p= source.property(n);
            if ( p!=null ) dest.putProperty( n, p );
        }
    }

    /**
     * return the list of properties that pertain to the dimension that dataset
     * values exist.  These are the properties that survive through most operations.
     * For example, if you flattened the dataset, what properties 
     * would still exist?  If you shuffled the data?  These are not structural
     * properties like DEPEND_0, BUNDLE_1, etc.
     * @return
     */
    public static String[] dimensionProperties() {
        return new String[]{
            QDataSet.UNITS, QDataSet.FORMAT, QDataSet.SCALE_TYPE,
            QDataSet.TYPICAL_MIN, QDataSet.TYPICAL_MAX,
            QDataSet.VALID_MIN, QDataSet.VALID_MAX, QDataSet.FILL_VALUE,
            QDataSet.RENDER_TYPE,
            QDataSet.NAME, QDataSet.LABEL, QDataSet.TITLE,
            QDataSet.USER_PROPERTIES
        };
    }

    /**
     * true if the property is one that is global and is relevant throughout the
     * dataset, such as a title or the units.
     *    property( "TITLE",0,0 ) often returns property("TITLE"), but
     *    property( "DEPEND_0",0,0 ) should never return property("DEPEND_0").
     * This is false, for example, for DEPEND_1.
     * @param prop the property name.
     * @return
     */
    public static boolean isInheritedProperty( String prop ) {
        boolean indexProp= prop.startsWith("DEPEND_")
                || prop.startsWith("BUNDLE_")
                || prop.startsWith("BINS_")
                || prop.startsWith("JOIN_")
                || prop.startsWith("PLANE_");
        // note CONTEXT* is inherited.
        return !indexProp;
    }

    /**
     * gets all the properties of the dataset.  This is a shallow
     * copy of properties.
     */
    public static Map<String, Object> getProperties(QDataSet ds, Map def) {
        Map result = def;

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

        for (int i = 0; i < names.length; i++) {
            if (ds.property(names[i]) != null) {
                result.put(names[i], ds.property(names[i]));
            }
        }

        return result;

    }

    /**
     * gets all the properties of the dataset.  This is a shallow
     * copy of properties.
     */
    public static Map<String, Object> getProperties(QDataSet ds) {
        return getProperties(ds, new LinkedHashMap());
    }

    /**
     * copy all properties into the dataset by iterating through the map.  Properties
     * that are equal to null are not copied, since null is equivalent to the
     * property not found.
     */
    public static void putProperties(Map<String, Object> properties, MutablePropertyDataSet ds) {
        for (Iterator i = properties.entrySet().iterator(); i.hasNext();) {
            Map.Entry<String,Object> e = (Map.Entry<String,Object>) i.next();
            if ( e.getKey().startsWith("DEPEND_") && e.getValue() instanceof Map ) {
                QDataSet dep= (QDataSet) ds.property(e.getKey());
                if ( dep instanceof MutablePropertyDataSet ) {
                    MutablePropertyDataSet mdep= (MutablePropertyDataSet)dep;
                    putProperties( (Map<String,Object>)e.getValue(), mdep );
                }
            } else if ( e.getKey().startsWith("PLANE_") && e.getValue() instanceof Map ) {
                QDataSet dep= (QDataSet) ds.property(e.getKey());
                if ( dep instanceof MutablePropertyDataSet ) {
                    MutablePropertyDataSet mdep= (MutablePropertyDataSet)dep;
                    putProperties( (Map<String,Object>)e.getValue(), mdep );
                }
            } else if ( e.getKey().startsWith("BUNDLE_") && e.getValue() instanceof Map ) {
                QDataSet dep= (QDataSet) ds.property(e.getKey());
                if ( dep instanceof MutablePropertyDataSet ) {
                    MutablePropertyDataSet mdep= (MutablePropertyDataSet)dep;
                    putProperties( (Map<String,Object>)e.getValue(), mdep );
                }
            } else if ( e.getKey().startsWith("CONTEXT_") && e.getValue() instanceof Map ) {
                QDataSet dep= (QDataSet) ds.property(e.getKey());
                if ( dep instanceof MutablePropertyDataSet ) {
                    MutablePropertyDataSet mdep= (MutablePropertyDataSet)dep;
                    putProperties( (Map<String,Object>)e.getValue(), mdep );
                }
            } else {
                if ( e.getValue()!=null ) ds.putProperty((String) e.getKey(), e.getValue());
            }
        }
    }

    /**
     * cleans up code by doing the cast, and handles default value.  The
     * result of this is for human-consumption!
     *
     */
    /*public static <T> getProperty( QDataSet ds, String propertyName, Class<T> clazz, Object<T> defaultValue ) {
    T p = ds.property( propertyName );
    if ( p==null ) p= defaultValue;
    return p;
    ArrayList o;
    }*/
    public static String toString(QDataSet ds) {

        if ( ds==null ) {
            throw new IllegalArgumentException( "null dataset" );
        }
        Units u= (Units)ds.property(QDataSet.UNITS);
        if ( u==null ) u= Units.dimensionless;

        String name = (String) ds.property(QDataSet.NAME);
        if (name == null) {
            name = "dataSet";
        }

        if ( ds.rank()==0 ) {
            if ( name.equals("dataSet") ) {
                return String.valueOf( DataSetUtil.asDatum(ds) );
            } else {
                return name + "=" + DataSetUtil.asDatum(ds) ;
            }
        }

        if ( ds.rank()==1 && "min,max".equals(ds.property(QDataSet.BINS_0)) ) {
            DatumRange dr= new DatumRange( ds.value(0), ds.value(1), u );
            return dr.toString();
        }

        if ( ds.rank()==1 && "min,maxInclusive".equals(ds.property(QDataSet.BINS_0)) ) {
            DatumRange dr= new DatumRange( ds.value(0), ds.value(1), u );
            return dr.toString() + "  (inclusive)";
        }

        if ( ds.rank()==2 && ds.length()==2 && ds.length(0)==2 && "min,maxInclusive".equals(ds.property( QDataSet.BINS_1) ) ) {
            Units u1= (Units) ds.property(QDataSet.UNITS,0);
            Units u2= (Units) ds.property(QDataSet.UNITS,1);

            DatumRange dr1= new DatumRange( ds.value(0,0), ds.value(0,1), u1==null ? Units.dimensionless : u1 );
            DatumRange dr2= new DatumRange( ds.value(1,0), ds.value(1,1), u2==null ? Units.dimensionless : u2 );
            return dr1.toString() + "; "+ dr2.toString() + "  (inclusive)";
        }

        String qubeStr = DataSetUtil.isQube(ds) ? "" : "*";

        String[] depNames = new String[4];

        for (int i = 0; i < 4; i++) {
            depNames[i] = "";
            QDataSet dep0 = (QDataSet) ds.property("DEPEND_" + i);
            if (dep0 != null) {
                String dname = (String) dep0.property(QDataSet.NAME);
                if (dname != null) {
                    if (dname.length() > 6) {
                        dname = dname.substring(0, 6) + "...";
                    }
                    depNames[i] = dname + "=";
                }
            }
        }

        int[] qubeDims;
        if ( DataSetUtil.isQube(ds) ) {
            qubeDims= DataSetUtil.qubeDims(ds);
        } else {
            qubeDims= new int[ ds.rank() ];
            qubeDims[0]= ds.length();
            if ( ds.rank() > 1) qubeDims[1]= ds.length(0);
            if ( ds.rank() > 2) qubeDims[2]= ds.length(0,0);
            if ( ds.rank() > 3) qubeDims[3]= ds.length(0,0,0);
        }

        StringBuffer dimStr = new StringBuffer("" + depNames[0] + ds.length());
        for ( int i=1; i<ds.rank(); i++ ) {
            dimStr.append("," + depNames[i] + qubeDims[i] + qubeStr);
        }
        
        String su = String.valueOf(u);
        if ( su.equals("")) {
            su = "dimensionless";
        }
        
        return name + "[" + dimStr.toString() + "] (" + su + ")";
       
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
     * @param ds
     * @return
     */
   public static QDataSet validPoints( QDataSet ds ) {
        Units u= (Units) ds.property(QDataSet.UNITS);
        if ( u==null ) {
            u= Units.dimensionless;
        }

        double offset= u.getFillDouble();

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
     * return the unit for which all elements in the dataset are
     * integer multiples of the result.
     * @param ds
     * @param d first factor for the dataset, error is used to detect non-zero significance.
     * @param limit the resolution for which data is considered equal, and this
     * limit should be greater than numerical precision.
     * @throws IllegalArgumentException if there is no valid data.
     * @return
     */
   public static QDataSet gcd( QDataSet ds, QDataSet d, QDataSet limit ) {
        QDataSet r, hist, peaks;

        do {

            r= Ops.mod( ds, d );
            hist= Ops.autoHistogram(r);

            peaks= AutoHistogram.peaks(hist);

            // stop is stopping condition tolerance.
            double stop= ( d.property(QDataSet.DELTA_MINUS)!=null ) ?  ((QDataSet)d.property(QDataSet.DELTA_MINUS)).value() : 0.0;
            stop= Math.max( stop, DataSetUtil.value( (RankZeroDataSet)limit, (Units)peaks.property(QDataSet.UNITS) ));
            double top= d.value() - stop;

            int nonZeroPeakIndex= ( peaks.value(0) - stop < 0.0 ) ? 1 : 0;
            int lastNonZeroPeakIndex= peaks.length()-1;

            
            while ( lastNonZeroPeakIndex>=0 && ( peaks.value(lastNonZeroPeakIndex) > top ) ) {
                lastNonZeroPeakIndex--;
            }

            if ( lastNonZeroPeakIndex < nonZeroPeakIndex ) {
                break;
            } else {
                d= DataSetOps.slice0( peaks, nonZeroPeakIndex );
            }

        } while ( true );

        return d;
   }

    /**
     * return the unit for which all elements in the dataset are 
     * integer multiples plus some offset.
     * @param ds
     * @param limit the resolution for which data is considered equal.  The result
     * will be an integer multiple of this.
     * @throws IllegalArgumentException if there is no valid data.
     * @return
     */
    public static QDataSet gcd( QDataSet ds, QDataSet limit ) {
        QDataSet ds1= validPoints(ds);
        if ( ds1.length()==0 ) throw new IllegalArgumentException("no valid points");
        if ( ds1.length()==1 ) return DataSetOps.slice0( ds, 0 );
        QDataSet guess= DataSetOps.slice0( ds, 1 );
        return gcd( ds, guess, limit );
    }

    /**
     * returns a rank 0 dataset indicating the cadence of the dataset.  Using a
     * dataset as the result allows the result to indicate SCALE_TYPE and UNITS.
     * History:
     *    2011-02-21: keep track of repeat values, allowing zero to be considered either mono increasing or mono decreasing
     *    2011-02-21: deal with interleaved fill values, keeping track of last valid value.
     * @param xds the x tags, which may not contain fill values for non-null result.
     * @param yds the y values, which if non-null is only used for fill values.  This
     *   is only used if it is rank 1.
     * @return null or the cadence in a rank 0 dataset.  The following may be
     *    properties of the result:
     *    SCALE_TYPE  may be "log"
     *    UNITS       will be ratiometric unit when the SCALE_TYPE is log, and
     *       will be the offset unit for interval units like Units.t2000.
     */
    public static RankZeroDataSet guessCadenceNew( QDataSet xds, QDataSet yds) {
        Object o= xds.property( QDataSet.CADENCE );
        Units u= (Units) xds.property( QDataSet.UNITS );

        if ( UnitsUtil.isNominalMeasurement(u) ) return null;
        
        if ( o!=null ) {
            if ( o instanceof RankZeroDataSet ) {
                return (RankZeroDataSet) o;
            } else {
                return DataSetUtil.asDataSet( ((Number)o).doubleValue(), u.getOffsetUnits() );
                //TODO: This legacy behavior should be removed.
            }
        }

        if (yds == null) {
            yds = DataSetUtil.replicateDataSet(xds.length(), 1.0);
        }
        assert (xds.length() == yds.length());  // note we need to turn assertions on as a test.  test012_003 shows where this is ignored.

        if ( yds.rank()>1 ) { //TODO: check for fill columns.  Note the fill check was to support a flakey dataset.
            yds = DataSetUtil.replicateDataSet(xds.length(), 1.0);
        }

        if ( xds.length()<2 ) return null;

        // Do initial scans of the data to check for monotonic decreasing and "ever increasing" spacing.
        double sp; // spacing between two measurements.
        double monoMag; // -1 if mono decreasing, 0 if not monotonic, 1 if mono increasing.

        QDataSet wds= DataSetUtil.weightsDataSet(xds);

        // check to see if spacing is ever-increasing, which is a strong hint that this is log spacing.
        // everIncreasing is a measure of this.  When it is >0, it is the ratio of the last to the first
        // number in a ever increasing sequence.
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
        }
        if ( ( repeatValues + monoIncreasing ) >(9*count/10) ) {
            monoMag= 1;
        } else if ( ( repeatValues + monoDecreasing ) >(9*count/10) ) {
            monoMag= -1;
        } else {
            monoMag= 0;
        }
        
        // don't allow datasets with fill in x to be considered.  
        if ( xHasFill && monoMag==0 ) return null;
        if ( monoMag==0 ) return null;
        
        double everIncreasing= 0.;
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
                if ( wds.value(i)==0 || wds.value(i-2)==0 ) {
                    continue;
                }
                if ( xds.value(i)<=0 || xds.value(i-2)<=0 ) {
                    everIncreasing= 0;
                    continue;
                }
                double sp1= monoMag * ( xds.value(i) - xds.value(i-2) );
                if ( sp1 > sp0*1.00001  ) {
                    everIncreasing= xds.value(i)/xds.value(0);
                    sp0= sp1;
                } else {
                    everIncreasing= 0;
                }
            }
        }
        if ( everIncreasing>0 && monoMag==-1 ) everIncreasing= 1/everIncreasing;

        boolean logScaleType = "log".equals( xds.property(QDataSet.SCALE_TYPE) );

        QDataSet extent= Ops.extent(xds);

        AutoHistogram ah= new AutoHistogram();
        QDataSet diffs=  Ops.diff(xds);
        if ( monoDecreasing>(9*count/10) ) {
            diffs= Ops.multiply( diffs, asDataSet(-1) );
        }
        QDataSet hist= ah.doit( diffs ); 

        long total= (Long)( ((Map<String,Object>)hist.property( QDataSet.USER_PROPERTIES )).get(AutoHistogram.USER_PROP_TOTAL) );

        if ( total==0 ) {
            return null;
        }
        
        // if the ratio of successive numbers is always increasing this is a strong
        // hint that ratiometric spacing is more appropriate.  If non-zero, then
        // this is the ratio of the first to the last number.
        final int everIncreasingLimit = total < 10 ? 25 : 100;

        int ipeak=0;
        int peakv=(int) hist.value(0);
        int linHighestPeak=0; // highest observed non-trivial peak

        int linMedian=-1;
        int t=0;

        double mean= AutoHistogram.mean( hist ).value();
        int imean= AutoHistogram.binOf( hist, mean );

        for ( int i=0; i<hist.length(); i++ ) {
            t+= hist.value(i);
            if ( hist.value(i)>peakv ) {
                ipeak= i;
                peakv= (int) hist.value(i);
            }
            if ( hist.value(i)>peakv/10  ) {
                linHighestPeak= i;
            }
            if ( linMedian==-1 && t>total/2 ) {
                linMedian= i;
            }
        }
        int linLowestPeak=0;
        for ( int i=0; i<hist.length(); i++ ) {
            if ( hist.value(i)>peakv/10 ) {
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

        if ( UnitsUtil.isRatioMeasurement(xunits) && 
                ( logScaleType || everIncreasing>everIncreasingLimit || ( ipeak==0 && extent.value(0)-Math.abs(mean) < 0 && ( total<10 || firstBin<=0. ) ) ) ) {
            ah= new AutoHistogram();
            QDataSet loghist= ah.doit( Ops.diff(Ops.log(xds)),DataSetUtil.weightsDataSet(yds)); //TODO: sloppy!
            // ltotal can be different than total.  TODO: WHY?  maybe because of outliers?
            long ltotal= (Long)( ((Map<String,Object>)loghist.property( QDataSet.USER_PROPERTIES )).get(AutoHistogram.USER_PROP_TOTAL) );
            int logPeak=0;
            int logPeakv=(int) loghist.value(0);
            int logMedian=-1;
            int logHighestPeak=0;
            t=0;

            mean= AutoHistogram.mean(loghist).value();
            int lmean= AutoHistogram.binOf( loghist, mean );
            for ( int i=0; i<loghist.length(); i++ ) {
                t+= loghist.value(i);
                if ( loghist.value(i)>logPeakv ) {
                    logPeak=i;
                    logPeakv= (int) loghist.value(i);
                }
                if ( loghist.value(i)>logPeakv/100  ) { // be loosy-goosey with log.
                   logHighestPeak= i;
                }
                if ( logMedian==-1 && t>ltotal/2 ) {
                    logMedian= i;
                }
            }
            int logLowestPeak=0;
            for ( int i=0; i<hist.length(); i++ ) {
                if ( loghist.value(i)>logPeakv/10 ) {
                    logLowestPeak=i;
                    break;
                }
            }

            int highestPeak= linHighestPeak;

            if ( everIncreasing>everIncreasingLimit || ( logPeak>1 && (1.*logMedian/loghist.length() > 1.*linMedian/hist.length() ) ) ) {
                hist= loghist;
                ipeak= logPeak;
                peakv= logPeakv;
                highestPeak= logHighestPeak;
                log= true;
            }

            if ( peakv<20 ) {
                ipeak= highestPeak;
                peakv= (int) hist.value(ipeak);
            }

        } else if ( peakv<20 ) { // loosen things up when there isn't much data.
            ipeak= linHighestPeak;
            peakv= (int) hist.value(ipeak);
        }

        double ss=0;
        double nn=0;

        QDataSet sss= (QDataSet) hist.property( QDataSet.PLANE_0 ); // DANGER--don't change PLANE_0!

        for ( int i=ipeak; i>=0; i-- ) {
            if ( hist.value(i)>(peakv/4) ) {
                ss+= sss.value(i) * hist.value(i);
                nn+= hist.value(i);
            } else {
                break;
            }
        }

        for ( int i=ipeak+1; i<hist.length(); i++ ) {
            if ( hist.value(i)>(peakv/4) ) {
                ss+= sss.value(i) * hist.value(i);
                nn+= hist.value(i);
            } else {
                break;
            }
        }

        if ( log ) {
            MutablePropertyDataSet result= DRank0DataSet.create(ss/nn);
            result.putProperty( QDataSet.UNITS, Units.logERatio );
            result.putProperty( QDataSet.SCALE_TYPE, "log" );
            return (RankZeroDataSet)result;
        } else {
            MutablePropertyDataSet result= DRank0DataSet.create(ss/nn);
            result.putProperty( QDataSet.UNITS, xunits.getOffsetUnits() );
        
            return (RankZeroDataSet)result;
        }
    }


    /**
     * calculate cadence by averaging consistent inter-point distances, 
     * taking invalid measurements into account.  This number needs to be interpretted
     * in the context of the dataset using the properties UNITS and
     * SCALE_TYPE.  If SCALE_TYPE is "log", then this number should be interpreted
     * as the ratiometric spacing in natural log space.  
     * Math.log( xds.value(1) ) - Math.log( xds.value(0) ) or
     * Math.log( xds.value(1) / xds.value(0) )
     *
     * @deprecated  use guessCadenceNew which is more robust.
     * @return double in the units of xds's units.getOffsetUnits(), or null if
     * no cadence is detected.
     */
    public static Double guessCadence(QDataSet xds, QDataSet yds) {
        RankZeroDataSet d= (RankZeroDataSet) xds.property( QDataSet.CADENCE );
        if ( d!=null ) {
            if ( "log".equals(xds.property(QDataSet.SCALE_TYPE)) ) {
                return DataSetUtil.asDatum(d).doubleValue(Units.logERatio);
            } else {
                return d.value();
            }
        }
        
        if (yds == null) {
            yds = DataSetUtil.replicateDataSet(xds.length(), 1.0);
        }
        assert (xds.length() == yds.length());

        if ( yds.rank()>1 ) { //TODO: check for fill columns.
            yds = DataSetUtil.replicateDataSet(xds.length(), 1.0);
        }
        
        Units u = (Units) yds.property(QDataSet.UNITS);
        if (u == null) {
            u = Units.dimensionless;
        }
        double cadence = Double.MAX_VALUE;

        if ( xds.length()<2 ) return cadence;

        // calculate average cadence for points consistent with max.  Preload to avoid extra branch.
        double cadenceSMax = 0;
        int cadenceNMax = 1;
        
        // calculate average cadence for points consistent with min.  Preload to avoid extra branch.
        double cadenceSMin = Double.MAX_VALUE;
        int cadenceNMin = 1;

        int i = 0;
        double x0 = 0;
        while (i < xds.length() && !u.isValid(yds.value(i))) {
            i++;
        }
        if (i < yds.length()) {
            x0 = xds.value(i);
        } else {
            return Double.MAX_VALUE;
        }
        final boolean log= "log".equals( xds.property( QDataSet.SCALE_TYPE ) );
        for (i++; i < xds.length() && i<DataSetOps.DS_LENGTH_LIMIT; i++) {
            if (u.isValid(yds.value(i))) {
                double cadenceAvgMin;
                cadenceAvgMin = cadenceSMin / cadenceNMin;
                double cadenceAvgMax;
                cadenceAvgMax = cadenceSMax / cadenceNMax;
                if (log) {
                    cadence = Math.abs( Math.log( xds.value(i) / x0 ) );
                } else {
                    cadence = Math.abs( xds.value(i) - x0 );
                }
                
                if ( cadence < 0.5 * cadenceAvgMin && cadenceNMin < 10 ) {  // set the initial value
                    cadenceSMin = cadence;
                    cadenceNMin = 1;
                    cadenceAvgMin = cadence; // set this, since cadenceMax uses it.
                } else if (cadence > 0.5 * cadenceAvgMin && cadence < 1.5 * cadenceAvgMin) {
                    cadenceSMin += cadence;
                    cadenceNMin += 1;
                }

                if ( cadence > 1.5 * cadenceAvgMax && cadenceNMax < 10 && cadence < 100 * cadenceAvgMin ) {  // set the initial value
                    cadenceSMax = cadence;
                    cadenceNMax = 1;
                } else if (cadence > 0.5 * cadenceAvgMax && cadence < 1.5 * cadenceAvgMax) {
                    cadenceSMax += cadence;
                    cadenceNMax += 1;
                }
                
                x0 = xds.value(i);
            }
        }
        
        double avgMin= cadenceSMin / cadenceNMin;
        double avgMax= cadenceSMax / cadenceNMax;
        
        QDataSet hist= Ops.histogram( Ops.diff(xds), 0, avgMin*10, avgMin*10/99 );
        
        int maxPeak= -1;
        int minPeak= -1;
        int peakHeight= Math.max( 1, xds.length() / 100 );
        for ( i=0; i<hist.length(); i++ ) {  // expect to see just one peak, otherwise use max peak.
            // TODO: verify that the cadence is in the middle of the 10th bin.  
            // TODO: check for peaks at integer multiples of the cadence.
            if ( hist.value(i)>=peakHeight ) {
                if ( minPeak==-1 ) minPeak= i;
                maxPeak= i;
                peakHeight= (int)hist.value(i);
            }
        }
        if ( maxPeak>minPeak ) {
            return avgMax*2;
        } else {
            return avgMin;
        }    
    }

    /**
     * calculate cadence by averaging the smallest set of consistent inter-point
     * distance.  Assumes all points are valid.  This number needs to be interpretted
     * in the context of the dataset, for example using the properties UNITS and
     * SCALE_TYPE.  If SCALE_TYPE is "log", then this number should be interpreted
     * as the ratiometric spacing in natural log space.  
     * Math.log( xds.value(1) ) - Math.log( xds.value(0) ) or
     * Math.log( xds.value(1) / xds.value(0) )
     * 
     * result can be null, indicating that no cadence can be established.
     * @deprecated use guessCadenceNew which is more robust.
     */
    public static Double guessCadence(QDataSet xds) {
        return guessCadence(xds, null);
    }

    /**
     * test to see that the dataset is a qube.
     * @param ds QDataSet of any rank.
     * @return true if the dataset is a qube.
     */
    public static boolean isQube(QDataSet ds) {
        if (ds.rank() <= 1) return true;
        Boolean q = (Boolean) ds.property(QDataSet.QUBE);
        if (q == null || q.equals(Boolean.FALSE)) {
            QDataSet dep1= (QDataSet) ds.property(QDataSet.DEPEND_1);
            if ( ds.rank()==2 && dep1!=null && dep1.rank()==1 ) {
                return true;
            }
            return false;
        }
        return true;
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
            throw new IllegalArgumentException("rank limit");
        } else if (ds.rank()==2 ) {  // rank 1 depend_1 implies qube.
            QDataSet dep1= (QDataSet) ds.property(QDataSet.DEPEND_1);
            if ( dep1!=null && dep1.rank()==1 ) return new int[] { ds.length(), dep1.length() };
        } else if (ds.rank() == 1) {
            return new int[]{ds.length()};  // rank 1 datasets are trivially qubes
        } else if ( ds.rank()== 0 ) {
            return new int[]{};  // rank 0 datasets are trivially qubes
        }
        Boolean q = (Boolean) ds.property(QDataSet.QUBE);
        if (q == null || q.equals(Boolean.FALSE)) {
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
     * add QUBE property to dataset, maybe verifying that it is a qube.  This is
     * intended to reduce code that builds datasets, not to verify that a dataset
     * is a qube.
     * @param ds
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

    public static String format(QDataSet ds) {
        if ( ds.property(QDataSet.BUNDLE_0)!=null ) {
            StringBuffer result= new StringBuffer(); // for documenting context.
            for ( int i=0; i<ds.length(); i++ ) {
                QDataSet cds= DataSetOps.slice0(ds, i);
                result.append( DataSetUtil.format(cds) );
                if ( i<ds.length()-1 ) result.append(", ");
            }
            return result.toString();
        } else if ( "min,max".equals( ds.property(QDataSet.BINS_0) ) && ds.rank()==1) {
            StringBuffer result= new StringBuffer();
            Units u= (Units) ds.property(QDataSet.UNITS);
            if ( u==null ) u= Units.dimensionless;
            result.append( new DatumRange( ds.value(0), ds.value(1), u ).toString() );

            String[] ss= ((String)ds.property(QDataSet.BINS_0)).split(",",-2);
            if (ss.length!=ds.length() ) throw new IllegalArgumentException("bins count != length in ds");
            return result.toString();

        } else if ( "min,maxInclusive".equals( ds.property(QDataSet.BINS_0) ) && ds.rank()==1) {
            StringBuffer result= new StringBuffer();
            Units u= (Units) ds.property(QDataSet.UNITS);
            if ( u==null ) u= Units.dimensionless;
            result.append( new DatumRange( ds.value(0), ds.value(1), u ).toString() );
            result.append( "(inclusive)" );
            String[] ss= ((String)ds.property(QDataSet.BINS_0)).split(",",-2);
            if (ss.length!=ds.length() ) throw new IllegalArgumentException("bins count != length in ds");
            return result.toString();

        } else if ( ds.property(QDataSet.BINS_0)!=null && ds.rank()==1) {
            StringBuffer result= new StringBuffer();
            Units u= (Units) ds.property(QDataSet.UNITS);
            if ( u==null ) u= Units.dimensionless;
            String[] ss= ((String)ds.property(QDataSet.BINS_0)).split(",",-2);
            if (ss.length!=ds.length() ) throw new IllegalArgumentException("bins count != length in ds");
            for ( int i=0; i<ds.length(); i++ ) {
                result.append( ss[i]+"="+u.createDatum(ds.value(i)) );
                if ( i<ds.length()-1 ) result.append(", ");
            }
            if ( ds.property(QDataSet.SCALE_TYPE)!=null ) {
                result.append("SCALE_TYPE="+ds.property(QDataSet.SCALE_TYPE) );
            }
            return result.toString();
        }
        if ( ds.rank()==0 ) {
            String name= (String) ds.property(QDataSet.NAME);
            Units u= (Units) ds.property(QDataSet.UNITS);
            String format= (String) ds.property( QDataSet.FORMAT );
            StringBuffer result= new StringBuffer();
            if ( name!=null ) {
                result.append(name).append("=");
            }
            if ( format!=null ) {
                if ( u!=null ) {
                    if ( UnitsUtil.isTimeLocation(u) ) {
                        double millis= u.convertDoubleTo(Units.t1970, ds.value() );
                        Calendar cal= Calendar.getInstance();
                        cal.setTimeInMillis( (long)millis ); // TODO: check how to specify to nanos.
                        result.append( String.format(format,cal) );
                    } else {
                        result.append( String.format(format,ds.value()) );
                        if ( u!=Units.dimensionless ) result.append( " " ).append(u.toString());
                    }
                } else {
                    result.append( String.format(format,ds.value()) );
                }
            } else {
                if ( u!=null ) {
                    result.append( u.createDatum(ds.value()).toString() );
                } else {
                    result.append( ds.value() );
                }
            }
            QDataSet context0= (QDataSet) ds.property("CONTEXT_0");
            if ( context0!=null ) {
                result.append(" @ " + format(context0) );
            }
            return result.toString();
        }
        StringBuffer buf = new StringBuffer(ds.toString() + ":\n");
        if (ds.rank() == 1) {
            for (int i = 0; i < Math.min(40, ds.length()); i++) {
                buf.append(" " + ds.value(i));
            }
            if (ds.length() >= 40) {
                buf.append(" ...");
            }
        }
        if (ds.rank() == 2) {
            for (int i = 0; i < Math.min(10, ds.length()); i++) {
                for (int j = 0; j < Math.min(20, ds.length(i)); j++) {
                    buf.append(" " + ds.value(i,j));
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
     * @param ds
     * @return
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
        if (problems == null) problems = new ArrayList<String>();
        return validate(ds, problems, 0);
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
        int[] qube= DataSetUtil.qubeDims(ds);
        qube= null;
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

    private static boolean validate(QDataSet ds, List<String> problems, int dimOffset) {
        QDataSet dep = (QDataSet) ds.property(QDataSet.DEPEND_0);
        if (dep != null) {
            if (dep.length() != ds.length()) {
                problems.add(String.format("DEPEND_%d length is %d, should be %d.", dimOffset, dep.length(), ds.length()));
            }
            if (ds.rank() > 1 && ds.length() > 0) {
                 validate(DataSetOps.slice0(ds, 0), problems, dimOffset + 1); // don't use native, because it may copy. Note we only check the first assuming QUBE.
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
     * Provide consistent valid logic to operators by providing a QDataSet
     * with >0.0 where the data is valid, and 0.0 where the data is invalid.
     * VALID_MIN, VALID_MAX and FILL_VALUE properties are used.  
     * 
     * Note, when FILL_VALUE is not specified, -1e31 is used.  This is to
     * support legacy logic.
     * 
     * For convenience, the property FILL_VALUE is set to the fill value used.
     * 
     */
    public static QDataSet weightsDataSet(final QDataSet ds) {
        QDataSet result = (QDataSet) ds.property(QDataSet.WEIGHTS_PLANE);
        if (result == null) {
            Number validMin = (Number) ds.property(QDataSet.VALID_MIN);
            if (validMin == null) validMin = Double.NEGATIVE_INFINITY;
            Number validMax = (Number) ds.property(QDataSet.VALID_MAX);
            if (validMax == null) validMax = Double.POSITIVE_INFINITY;
            Units u = (Units) ds.property(QDataSet.UNITS);
            Number ofill = (Number) ds.property(QDataSet.FILL_VALUE);
            double fill = (ofill == null ? Double.NaN : ofill.doubleValue());
            boolean check = (validMin.doubleValue() > -1 * Double.MAX_VALUE || validMax.doubleValue() < Double.MAX_VALUE || !(Double.isNaN(fill)));
            if (check) {
                result = new WeightsDataSet.ValidRangeFillFinite(ds);
            } else {
                if (u != null) {
                    result = new WeightsDataSet.FillFinite(ds); // support legacy Units to specify fill value
                } else {
                    result = new WeightsDataSet.Finite(ds);
                }
            }
        }
        return result;
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
     * @param ds
     * @param u
     * @return
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

    public static Datum asDatum( QDataSet ds ) {
        if ( ds.rank()>0 ) {
            throw new IllegalArgumentException("dataset is not rank 0");
        } else {
            Units u= (Units) ds.property(QDataSet.UNITS);
            if ( u==null ) {
                return Units.dimensionless.createDatum(ds.value());
            } else {
                return u.createDatum(ds.value());
            }
        }
    }

    public static DatumRange asDatumRange( QDataSet ds, boolean sloppy ) {
        Units u= SemanticOps.getUnits(ds);
        if ( sloppy==false ) {
            if ( !ds.property( QDataSet.BINS_0 ).equals("min,max") ) {
                throw new IllegalArgumentException("expected min,max for BINS_0 because we are not allowing sloppy.");
            }
        }
        return new DatumRange( ds.value(0), ds.value(1), u );
    }

    public static DRank0DataSet asDataSet( double d, Units u ) {
        return DRank0DataSet.create(d,u);
    }

    public static DRank0DataSet asDataSet( double d ) {
        return DRank0DataSet.create(d);
    }

    public static DRank0DataSet asDataSet( Datum d ) {
        return DRank0DataSet.create(d);
    }

    /**
     * convert java arrays into QDataSets.
     * @param arr
     * @return
     */
    public static QDataSet asDataSet(Object arr) {
        if ( arr.getClass().isArray() ) {
            Class c=  arr.getClass().getComponentType();
            if ( c==double.class ) {
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
            throw new IllegalArgumentException("unsupported type: "+arr.getClass());
        }
    }

    /**
     * adds the rank 0 dataset (a Datum) to the dataset's properties, after all
     * the other CONTEXT_<i> properties.
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
     * the other CONTEXT_<i> properties.
     * @param ds
     * @param cds
     */
    public static void addContext( Map<String,Object> props, QDataSet cds ) {
        int idx=0;
        while ( props.get("CONTEXT_"+idx)!=null ) idx++;
        props.put( "CONTEXT_"+idx, cds );
    }

    public static String contextAsString( QDataSet ds ) {
        StringBuffer result= new StringBuffer();
        QDataSet cds= (QDataSet) ds.property( QDataSet.CONTEXT_0 );
        int idx=0;
        while ( cds!=null ) {
            if ( cds.rank()>0 ) {
                if ( cds.rank()==1 && cds.property(QDataSet.BINS_0)!=null ) {
                    result.append( DataSetUtil.format(cds) );
                } else {
                    QDataSet extent= Ops.extent(cds);
                    if ( extent.value(1)==extent.value(0) ) {
                        result.append( DataSetUtil.format(cds.slice(0)) );  // for CLUSTER/PEACE this happens where rank 1 context is all the same value
                    } else {
                        result.append( DataSetUtil.format(extent) ).append( " " +cds.length() + " different values" ); // slice was probably done when we should't have.
                    }
                }
            } else {
                result.append( DataSetUtil.format(cds) );
            }
            idx++;
            cds= (QDataSet) ds.property( "CONTEXT_"+idx );
            if ( cds!=null ) result.append(", ");
        }
        return result.toString();
    }
    
    /**
     * returns the indeces of the min and max elements of the monotonic dataset.
     * This uses DataSetUtil.isMonotonic() which would be slow if MONOTONIC is
     * not set.
     * @param ds
     * @return
     * @see Ops.extent which returns the range containing any data.
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
     */
    public static int xTagBinarySearch( QDataSet ds, Datum datum, int low, int high ) {
        Units units= datum.getUnits();
        Units toUnits= SemanticOps.getUnits( ds );
        UnitsConverter uc= units.getConverter(toUnits);
        double key= datum.doubleValue(toUnits);
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
     * returns the index of the closest index in the data. "column" comes
     * from the legacy operator.
     * @param ds
     * @param datum
     * @return
     */
    public static int closestIndex( QDataSet ds, Datum datum ) {
        if ( !isMonotonic(ds) ) {
            System.err.println("dataset is not monotonic");
            isMonotonic(ds);
            throw new IllegalArgumentException("dataset is not monotonic");
        }
        int result= xTagBinarySearch( ds, datum, 0, ds.length()-1 );
        double ddatum= datum.doubleValue( SemanticOps.getUnits(ds) );
        if (result == -1) {
            result = 0; //insertion point is 0
        } else if (result < 0) {
            result= ~result; // usually this is the case
            if ( result >= ds.length()-1 ) {
                result= ds.length()-1;
            } else {
                double x= ddatum;
                double x0= ds.value(result-1 );
                double x1= ds.value(result );
                result= ( ( x-x0 ) / ( x1 - x0 ) < 0.5 ? result-1 : result );
            }
        }
        return result;
    }

    public static int closestIndex( QDataSet table, double x, Units units ) {
        return closestIndex( table, units.createDatum(x) );
    }


    /**
     * returns the first column that is before the given datum.  Note the
     * if the datum identifies (==) an xtag, then the previous column is
     * returned.
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

}


