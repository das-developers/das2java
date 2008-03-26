/*
 * DataSetUtil.java
 *
 * Created on April 1, 2007, 4:28 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.virbo.dataset;

import edu.uiowa.physics.pw.das.datum.Units;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
            if (d <= last || (u != null && u.isFill(d))) {
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
    public static String version = "2007-12-19";

    /**
     * gets all the properties of the dataset.  This is a shallow
     * copy of properties.
     */
    public static Map<String, Object> getProperties(QDataSet ds, Map def) {
        Map result = def;

        String[] names = new String[]{QDataSet.UNITS, QDataSet.CADENCE,
            QDataSet.MONOTONIC, QDataSet.SCALE_TYPE,
            QDataSet.TYPICAL_RANGE, QDataSet.VALID_RANGE,
            QDataSet.QUBE, QDataSet.FILL_VALUE,
            QDataSet.NAME, QDataSet.LABEL, QDataSet.TITLE,
            QDataSet.CACHE_TAG
        ,
            };
        
        for ( int i=0; i < names.length; i++) {
            if (ds.property(names[i]) != null) {
                result.put(names[i], ds.property(names[i]));
            }
        }

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

        return result;

    }

    /**
     * gets all the properties of the dataset.  This is a shallow
     * copy of properties.
     */
    public static Map<String, Object> getProperties(QDataSet ds) {
        return getProperties(ds, new HashMap());
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

        String qubeStr = Boolean.TRUE.equals(ds.property(QDataSet.QUBE)) ? "" : "*";

        String[] depNames = new String[4];

        for (int i = 0; i < 4; i++) {
            depNames[i] = "";
            QDataSet dep0 = (QDataSet) ds.property("DEPEND_" + i);
            if (dep0 != null) {
                String dname = (String) dep0.property(QDataSet.NAME);
                if (dname != null) {
                    if (dname.length() > 6) {
                        dname = dname.substring(0, 6) + "*";
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
     * calculate cadence by averaging the smallest set of consistent inter-point
     * distance.  Assumes all points are valid
     */
    public static double guessCadence(QDataSet xds) {

        double cadence = Double.MAX_VALUE;

        // calculate average cadence for consistent points.  Preload to avoid extra branch.
        double cadenceS = Double.MAX_VALUE;
        int cadenceN = 1;

        double x0 = xds.value(0);

        for (int i = 1; i < xds.length(); i++) {
            double cadenceAvg;
            cadenceAvg = cadenceS / cadenceN;
            cadence = xds.value(i) - x0;
            if (cadence < 0.5 * cadenceAvg) {
                cadenceS = cadence;
                cadenceN = 1;
            } else if (cadence < 1.5 * cadenceAvg) {
                cadenceS += cadence;
                cadenceN += 1;
            }
            x0 = xds.value(i);
        }
        return cadenceS / cadenceN;
    }

    /**
     * provides a convenient way of indexing qubes, returning an int[] of 
     * length ds.rank() containing each dimension's length,
     * or null if the dataset is not a qube.
     * @param ds
     * @return int[] of length ds.rank() containing each dimension's length, pr null.
     */
    public static int[] qubeDims(QDataSet ds) {
        if ( ds.rank() > 4 ) throw new IllegalArgumentException("rank limit");
        if (ds.rank() == 1) {
            return new int[] { ds.length() };  // rank 1 datasets are trivially qubes
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
                if ( ds.rank() > 3 ) { // TODO: generalize to rank N
                    qube[3]= ((RankNDataSet)ds).slice(0).length(0,0);
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

    public static String statsString(QDataSet ds) {
        QDataSet stats = DataSetOps.moment(ds);
        return "" + stats.value(0) + "+/-" + stats.property("stddev") + " N=" + stats.property("validCount");
    }
}

