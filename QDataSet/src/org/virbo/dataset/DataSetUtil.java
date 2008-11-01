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
import org.das2.datum.Units;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.virbo.dsops.Ops;

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
     */
    public static MutablePropertyDataSet tagGenDataSet(int n, final double start, final double cadence) {
        IndexGenDataSet result = new IndexGenDataSet(n) {

            public double value(int i) {
                return i * cadence + start;
            }
        };
        result.putProperty(QDataSet.CADENCE, Double.valueOf(cadence));
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
     * returns true if the dataset is monotonically increasing, and contains no fill.
     * An empty dataset is monotonic.
     */
    public static boolean isMonotonic(QDataSet ds) {
        if (ds.rank() != 1) {
            return false;
        }
        int i = 0;

        final Units u = (Units) ds.property(QDataSet.UNITS);

        if (ds.length() == 0) {
            return false;
        }

        double last = ds.value(i);

        if (u != null && u.isFill(last)) {
            return false;
        }

        for (i = 1; i < ds.length(); i++) {
            double d = ds.value(i);
            if (d < last || (u != null && u.isFill(d))) {
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

    public static String[] propertyNames() {
        return new String[]{
                    QDataSet.UNITS, QDataSet.CADENCE,
                    QDataSet.MONOTONIC, QDataSet.SCALE_TYPE,
                    QDataSet.TYPICAL_MIN, QDataSet.TYPICAL_MAX,
                    QDataSet.VALID_MIN, QDataSet.VALID_MAX,
                    QDataSet.FILL_VALUE,
                    QDataSet.QUBE,
                    QDataSet.NAME, QDataSet.LABEL, QDataSet.TITLE,
                    QDataSet.CACHE_TAG,
                    QDataSet.COORDINATE_FRAME,
                    QDataSet.DELTA_MINUS, QDataSet.DELTA_PLUS,
                    QDataSet.USER_PROPERTIES,
                };
    }

    /**
     * gets all the properties of the dataset.  This is a shallow
     * copy of properties.
     */
    public static Map<String, Object> getProperties(QDataSet ds, Map def) {
        Map result = def;

        for (int i = 0; i < ds.rank(); i++) {
            Object dep = ds.property("DEPEND_" + i);
            if (dep != null) {
                result.put("DEPEND_" + i, dep);
            }
        }

        for (int i = 0; i < QDataSet.MAX_PLANE_COUNT; i++) {
            QDataSet plane = (QDataSet) ds.property("PLANE_" + i);
            if (plane != null) {
                result.put("PLANE_" + i, plane);
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
     * copy all properties into the dataset by iterating through the map.
     */
    public static void putProperties(Map<String, Object> properties, MutablePropertyDataSet ds) {
        for (Iterator i = properties.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            ds.putProperty((String) e.getKey(), e.getValue());
        }
    }

    /**
     * cleans up code by doing the cast, and handles default value
     */
    /*public static <T> getProperty( QDataSet ds, String propertyName, Class<T> clazz, Object<T> defaultValue ) {
    T p = ds.property( propertyName );
    if ( p==null ) p= defaultValue;
    return p;
    ArrayList o;
    }*/
    public static String toString(QDataSet ds) {

        String name = (String) ds.property(QDataSet.NAME);
        if (name == null) {
            name = "dataSet";
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

        StringBuffer dimStr = new StringBuffer("" + depNames[0] + ds.length());
        if (ds.rank() > 1) {
            dimStr.append("," + depNames[1] + ds.length(0) + qubeStr);
        }
        if (ds.rank() > 2) {
            dimStr.append("," + depNames[2] + ds.length(0, 0) + qubeStr);
        }

        String u = String.valueOf(ds.property(QDataSet.UNITS));
        if (u.equals("null") || u.equals("")) {
            u = "dimensionless";
        }
        return name + "[" + dimStr.toString() + "] (" + u + ")";
    }

    /**
     * calculate cadence by averaging consistent inter-point distances, 
     * taking invalid measurements into account.  This number needs to be interpretted
     * in the context of the dataset using the properties UNITS and
     * SCALE_TYPE.  If SCALE_TYPE is "log", then this number should be interpreted
     * as the ratiometric spacing in natural log space.  
     * Math.log( xds.value(1) ) - Math.log( xds.value(0) ) or
     * Math.log( xds.value(1) / xds.value(0) )
     */
    public static double guessCadence(QDataSet xds, QDataSet yds) {
        if (yds == null) {
            yds = DataSetUtil.replicateDataSet(xds.length(), 1.0);
        }
        assert (xds.length() == yds.length());
        Units u = (Units) yds.property(QDataSet.UNITS);
        if (u == null) {
            u = Units.dimensionless;
        }
        double cadence = Double.MAX_VALUE;

        // calculate average cadence for consistent points.  Preload to avoid extra branch.
        double cadenceS = Double.MAX_VALUE;
        int cadenceN = 1;

        int i = 0;
        double x0 = 0;
        while (i < xds.length() && !u.isValid(yds.value(i))) {
            i++;
        }
        if (i < yds.length()) {
            x0 = xds.value(i);
        }
        final boolean log= "log".equals( xds.property( QDataSet.SCALE_TYPE ) );
        for (i++; i < xds.length(); i++) {
            if (u.isValid(yds.value(i))) {
                double cadenceAvg;
                cadenceAvg = cadenceS / cadenceN;
                if (log) {
                    cadence = Math.abs( Math.log( xds.value(i) / x0 ) );
                } else {
                    cadence = Math.abs( xds.value(i) - x0 );
                }
                if (cadence < 0.5 * cadenceAvg && cadenceN < 10) {
                    cadenceS = cadence;
                    cadenceN = 1;
                } else if (cadence > 0.5 * cadenceAvg && cadence < 1.5 * cadenceAvg) {
                    cadenceS += cadence;
                    cadenceN += 1;
                }
                x0 = xds.value(i);
            }
        }
        
        double avg= cadenceS / cadenceN;
        
        QDataSet hist= Ops.histogram( Ops.diff(xds), 0, avg*10, avg*10/99 );
        
        int maxPeak= -1;
        int minPeak= -1;
        int peakHeight= Math.min( 1, xds.length() / 20 );
        for ( i=0; i<hist.length(); i++ ) {  // expect to see just one peak, otherwise use max peak.
            // TODO: verify that the cadence is in the middle of the 10th bin.  
            // TODO: check for peaks at integer multiples of the cadence.
            if ( hist.value(i)>=peakHeight ) {
                if ( minPeak==-1 ) minPeak= i;
                maxPeak= i;
            }
        }
        if ( maxPeak>minPeak ) {
            return ((QDataSet)hist.property(QDataSet.DEPEND_0)).value(maxPeak);
        } else {
            return avg;
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
     */
    public static double guessCadence(QDataSet xds) {
        return guessCadence(xds, null);
    }

    /**
     * test to see that the dataset is a qube.
     * @param ds QDataSet of any rank.
     * @return true if the dataset is a qube.
     */
    public static boolean isQube(QDataSet ds) {
        if (ds.rank() == 1) return true;
        Boolean q = (Boolean) ds.property(QDataSet.QUBE);
        if (q == null || q.equals(Boolean.FALSE)) {
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
        }
        if (ds.rank() == 1) {
            return new int[]{ds.length()};  // rank 1 datasets are trivially qubes

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
                if (ds.rank() > 3) { // TODO: generalize to rank N

                    qube[3] = ((RankNDataSet) ds).slice(0).length(0, 0);
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
            default:
                throw new IllegalArgumentException("rank not supported");
        }
        if (qube != null) {
            ds.putProperty(QDataSet.QUBE, Boolean.TRUE);
        }
    }

    public static String format(QDataSet ds) {
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
                    buf.append(" " + ds.value(i));
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
        QDataSet stats = DataSetOps.moment(ds);
        return "" + stats.value(0) + "+/-" + stats.property("stddev") + " N=" + stats.property("validCount");
    }

    /**
     * returns true if the dataset is valid, false otherwise.  If problems is
     * non-null, then problems will be indicated here.
     * @param ds rank N dataset.
     * @param problems insert problem descriptions here
     * @return true if the dataset is valid, false otherwise
     */
    public static boolean validate(QDataSet ds, List<String> problems) {
        if (problems==null ) problems= new ArrayList<String>();
        return validate(ds, problems, 0);
    }

    private static boolean validate(QDataSet ds, List<String> problems, int dimOffset) {
        QDataSet dep = (QDataSet) ds.property(QDataSet.DEPEND_0);
        if (dep != null) {
            if (dep.length() != ds.length()) {
                problems.add( String.format("DEPEND_%d length is %d, should be %d.", dimOffset, dep.length(), ds.length() ) );
            }
            if (ds.rank() > 1 && ds.length() > 0) {
                validate(DataSetOps.slice0(ds, 0), problems, dimOffset + 1);
            }
        }
        return problems.size()==0;
    }

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
     */
    public static QDataSet weightsDataSet(final QDataSet ds) {
        QDataSet result = (QDataSet) ds.property(QDataSet.WEIGHTS_PLANE);
        if (result == null) {
            result = new WeightsDataSet(ds);
        }
        return result;
    }

    /** 
     * Iterate through the dataset, changing all points outside of validmin,
     * validmax and with zero weight to fill=-1e31.  VALID_MIN and VALID_MAX 
     * properties are cleared, and FILL_VALUE is set to -1e31.
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
            if (it.getValue(wds) == 0) it.putValue(wrds, fill);
        }
        wrds.putProperty(QDataSet.FILL_VALUE, fill);
        wrds.putProperty(QDataSet.VALID_MIN, null);
        wrds.putProperty(QDataSet.VALID_MAX, null);
        return wrds;
    }
}

