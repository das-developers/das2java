/*
 * DataSetUtil.java
 *
 * Created on April 1, 2007, 4:28 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.virbo.dataset;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.IllegalFormatConversionException;
import java.util.logging.Level;
import org.das2.datum.Units;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.DatumUtil;
import org.das2.datum.UnitsConverter;
import org.das2.datum.UnitsUtil;
import org.das2.datum.format.DatumFormatter;
import org.das2.datum.format.DefaultDatumFormatter;
import org.das2.datum.format.FormatStringFormatter;
import org.das2.util.LoggerManager;
import org.virbo.dsops.Ops;
import org.virbo.dsutil.AutoHistogram;

/**
 *
 * @author jbf
 */
public class DataSetUtil {

    private static final Logger logger= LoggerManager.getLogger("qdataset");

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
            @Override
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
            @Override
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

            @Override
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

    /**
     * Return the set of non-structural properties of the dataset, like the UNITS and CADENCE.
     * These are the dimensionProperties, plus others specific to the dataset, such as CADENCE and
     * DELTA_PLUS.
     */
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
                    QDataSet.BIN_MINUS, QDataSet.BIN_PLUS,
                    QDataSet.WEIGHTS_PLANE,
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
        String[] names= dimensionProperties();
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
        return new String[]{
            QDataSet.UNITS, QDataSet.FORMAT, QDataSet.SCALE_TYPE,
            QDataSet.TYPICAL_MIN, QDataSet.TYPICAL_MAX,
            QDataSet.VALID_MIN, QDataSet.VALID_MAX, QDataSet.FILL_VALUE,
            QDataSet.NAME, QDataSet.LABEL, QDataSet.TITLE,
            QDataSet.USER_PROPERTIES, QDataSet.NOTES,
        };
    }

    /**
     * properties that describe the dataset itself, rather than those of a dimension
     * or structural properties.
     * @return
     */
    public static String[] globalProperties() {
        return new String[] {
            QDataSet.USER_PROPERTIES, QDataSet.NOTES, QDataSet.VERSION, QDataSet.METADATA, QDataSet.METADATA_MODEL, QDataSet.SOURCE, 
        };
    }

    /**
     * properties that go along with the zeroth index.  These are all QDataSets with dimensions compatible with the datasets.
     * If you trim the dataset, then these must be trimmed as well.
     * @return
     */
    public static String[] correlativeProperties() {
        return new String[] {
            QDataSet.DELTA_MINUS, QDataSet.DELTA_PLUS, QDataSet.BIN_MINUS, QDataSet.BIN_PLUS, QDataSet.WEIGHTS_PLANE,
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
                || prop.startsWith("PLANE_")
                || prop.equals( QDataSet.START_INDEX )
                || prop.equals( QDataSet.RENDER_TYPE );
        // note CONTEXT* is inherited.
        return !indexProp;
    }

    /**
     * return properties attached to the slice at index.  Note the slice
     * implementations use this, and this only returns properties from
     * dimensionProperties().
     * @param ds
     * @param index
     * @return
     */
    public static Map<String, Object> sliceProperties( QDataSet ds, int index, Map<String,Object> result ) {
        if ( result==null ) result= new LinkedHashMap();

        String[] names = dimensionProperties();

        for (int i = 0; i < names.length; i++) {
            Object val= ds.property(names[i],index);
            if ( val != null) {
                result.put( names[i], val );
            }
        }

        return result;

    }

    /**
     * help out implementations of the QDataSet.trim() command.  This does the dimension properties
     * and geometric properties like DEPEND_0  and DELTA_PLUS.
     * @param ds the dataset with properties to trim.
     * @param start start index of trim operation
     * @param stop exclusive stop index of the trim operation.
     * @return
     */
    public static Map<String,Object> trimProperties( QDataSet ds, int start, int stop ) {

        Map<String,Object> result= new LinkedHashMap();
        getDimensionProperties(ds,result);

        QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
        if ( dep0!=null ) result.put( QDataSet.DEPEND_0, dep0.trim(start,stop) );

        QDataSet dsp;
        String [] props= DataSetUtil.correlativeProperties();
        for ( String s: props ) {
            dsp= (QDataSet) ds.property( s );
            if ( dsp!=null ) result.put( s, dsp.trim(start,stop) );
        }

        for ( int i=0; i<QDataSet.MAX_PLANE_COUNT; i++ ) {
            QDataSet p= (QDataSet) ds.property("PLANE_"+i);
            if ( p!=null ) {
                if ( p.rank()>0 ) result.put( "PLANE_"+i, p.trim(start,stop) ); else result.put("PLANE_"+i, p ); // note planes must be at least rank 1 right now.
            } else {
                break;
            }
        }

        return result;
        
    }

    /**
     * return just the properties attached to the dataset, not DEPEND_x, etc.
     * @param ds
     * @param def default values
     * @return
     */
    public static Map<String,Object> getDimensionProperties( QDataSet ds, Map<String,Object> def ) {
        return getProperties( ds, dimensionProperties(), def );
    }

    /**
     * return the properties listed, using the defaults if provided.
     * See dimensionProperties(), globalProperties().
     * @param ds dataset source of the properties.
     * @param names array of names
     * @param def defaults, or null if no defaults are to be used.
     * @return map of the properties.
     */
    public static Map<String,Object> getProperties( QDataSet ds, String[] names, Map def ) {
        if ( def==null ) {
            def= new LinkedHashMap();
        } else {
            def= new LinkedHashMap( def );
        }

        for (int i = 0; i < names.length; i++) {
            Object val= ds.property(names[i]);
            if ( val != null) {
                def.put( names[i], val );
            }
        }

        return def;
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
            try {
                if ( name.equals("dataSet") ) {
                    Datum d= DataSetUtil.asDatum(ds);
                    return String.valueOf( d );
                } else {
                    return name + "=" + DataSetUtil.asDatum(ds) ;
                }
            } catch ( IllegalArgumentException ex ) {
                return "Error: "+ex;
            }
        }

        if ( ds.rank()==1 && QDataSet.VALUE_BINS_MIN_MAX.equals(ds.property(QDataSet.BINS_0)) ) {
            if (  ds.value(0) <= ds.value(1) ) {
                DatumRange dr= new DatumRange( ds.value(0), ds.value(1), u );
                return dr.toString();
            } else {
                return String.format( "%s %s (invalid because BINS_0=min,max)", ds.slice(0), ds.slice(1) );
            }
        }

        if ( ds.rank()==1 && "min,maxInclusive".equals(ds.property(QDataSet.BINS_0)) ) {
            if (  ds.value(0) <= ds.value(1) ) {
                DatumRange dr= new DatumRange( ds.value(0), ds.value(1), u );
                return dr.toString() + "  (inclusive)";
            } else {
                return String.format( "%s %s (invalid because BINS_0=min,maxInclusive)", ds.slice(0), ds.slice(1) );
            }
        }

        if ( ds.rank()==1 && Ops.isLegacyBundle(ds) && ds.length()<8 ) { // introduced to support where or rank 2 dataset.
            QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
            StringBuilder str = new StringBuilder("");
            try {
                str.append( dep0.slice(0) ).append("=").append( ds.slice(0) );
            } catch ( RuntimeException ex ) {
                ex.printStackTrace();
                str.append("Exception");
            }
            for ( int i=1; i<ds.length(); i++ ) {
                str.append(", ").append( dep0.slice(i) ).append("=").append( ds.slice(i) );
            }
            return str.toString();
        }

        if ( ds.rank()==2 && ds.length()==2 && ds.length(0)==2 && "min,maxInclusive".equals(ds.property( QDataSet.BINS_1) ) ) {
            Units u1= (Units) ds.property(QDataSet.UNITS,0);
            Units u2= (Units) ds.property(QDataSet.UNITS,1);

            DatumRange dr1= new DatumRange( ds.value(0,0), ds.value(0,1), u1==null ? Units.dimensionless : u1 );
            DatumRange dr2= new DatumRange( ds.value(1,0), ds.value(1,1), u2==null ? Units.dimensionless : u2 );
            return dr1.toString() + "; "+ dr2.toString() + "  (inclusive)";
        }

        if ( ds.rank()==2 && ds.length()==2 && ds.length(0)==2 && "min,max".equals(ds.property( QDataSet.BINS_1) ) ) {
            Units u1= (Units) ds.property(QDataSet.UNITS,0);
            Units u2= (Units) ds.property(QDataSet.UNITS,1);

            DatumRange dr1= new DatumRange( ds.value(0,0), ds.value(0,1), u1==null ? Units.dimensionless : u1 );
            DatumRange dr2= new DatumRange( ds.value(1,0), ds.value(1,1), u2==null ? Units.dimensionless : u2 );
            return dr1.toString() + "; "+ dr2.toString();
        }

        String qubeStr = DataSetUtil.isQube(ds) ? "" : "*";

        String[] depNames = new String[4];

        for (int i = 0; i < QDataSet.MAX_RANK; i++) {
            depNames[i] = "";
            Object dep0o= ds.property("DEPEND_" + i);
            if ( dep0o!=null ) {
                String dname=null;
                if ( dep0o instanceof QDataSet ) {
                    QDataSet dep0 = (QDataSet) dep0o;
                    if (dep0 != null) {
                        dname = (String) dep0.property(QDataSet.NAME);
                    }
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

        if ( ds.property(QDataSet.BUNDLE_0)!=null && depNames[0].length()==0  ) {
            depNames[0]= "BUNDLE_0=";
        }

        if ( ds.property(QDataSet.BUNDLE_1)!=null && depNames[0].length()==0 ) {
            depNames[1]= "BUNDLE_1=";    // TODO: consider  ds[time=1440,density,b_gsm=5] vs ds[time=1440,BUNDLE_1=5]
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
        
        Logger logger= LoggerManager.getLogger("qdataset.guesscadence");
        
        Object o= xds.property( QDataSet.CADENCE );
        Units u= (Units) xds.property( QDataSet.UNITS );

        if ( UnitsUtil.isNominalMeasurement(u) ) return null;
        
        if ( o!=null ) {
            if ( o instanceof RankZeroDataSet ) {
                return (RankZeroDataSet) o;
            } else if ( o instanceof QDataSet ) {
                QDataSet q= (QDataSet)o;
                while ( q.rank()>0 ) {
                    logger.log( Level.SEVERE, "averaging CADENCE rank 0: {0}", q);
                    q= Ops.reduceMax( q, 0 );
                }
                return DRank0DataSet.create( DataSetUtil.asDatum(q) );
            } else {
                return DataSetUtil.asDataSet( ((Number)o).doubleValue(), u.getOffsetUnits() );
                //TODO: This legacy behavior should be removed.
            }
        }

        if (yds == null) {
            yds = DataSetUtil.replicateDataSet(xds.length(), 1.0);
        } else {
            if ( xds.length()!=yds.length() ) {
               throw new IllegalArgumentException("xds.length()!=yds.length()");
            }
        }
        
        if ( yds.rank()>1 ) { //TODO: check for fill columns.  Note the fill check was to support a flakey dataset.
            yds = DataSetUtil.replicateDataSet(xds.length(), 1.0);
        }

        if ( xds.length()<2 ) return null;

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
        QDataSet diffs;
        if ( yds.rank()==1 && xds.rank()==1 ) { // ftp://virbo.org/tmp/poes_n17_20041228.cdf?P1_90[0:300] has every other value=fill.
            QDataSet r= Ops.where( Ops.valid(yds) );
            
            if ( r.length()<2 ) {
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
        logger.log( Level.FINER, "consider log: isratio={0} allPositive={1} bunch0={2}", new Object[]{ isratiomeas, extent.value(0)>0, bunch0 });            
        if ( isratiomeas && extent.value(0)>0 &&
                ( logScaleType 
                || everIncreasing>everIncreasingLimit
                || bunch0 ) ) {
            ah= new AutoHistogram();
            QDataSet loghist= ah.doit( Ops.diff(Ops.log(xds)),DataSetUtil.weightsDataSet(yds)); //TODO: sloppy!
            // ltotal can be different than total.  TODO: WHY?  maybe because of outliers?
            long ltotal= (Long)( ((Map<String,Object>)loghist.property( QDataSet.USER_PROPERTIES )).get(AutoHistogram.USER_PROP_TOTAL) );
            int logPeak=0;
            int logPeakv=(int) loghist.value(0);
            int logMedian=-1;
            int logHighestPeak=0;
            t=0;

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

        // one last sanity check, for the PlasmaWaveGroup file:///home/jbf/project/autoplot/data/qds/gapBug/gapBug.qds?Frequency
        if ( t<65 && log ) {
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
            if ( bigSkip==0 && skip>0 ) return null;
        }

        if ( log ) {
            MutablePropertyDataSet result= DRank0DataSet.create(ss/nn);
            result.putProperty( QDataSet.UNITS, Units.logERatio );
            result.putProperty( QDataSet.SCALE_TYPE, "log" );
            logger.log(Level.FINE, "guessCadence({0})->{1} (log)", new Object[]{xds, result});
            return (RankZeroDataSet)result;
        } else {
            MutablePropertyDataSet result= DRank0DataSet.create(ss/nn);
            result.putProperty( QDataSet.UNITS, xunits.getOffsetUnits() );
            logger.log(Level.FINE, "guessCadence({0})->{1} (linear)", new Object[]{xds, result});
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
     * distance.  Assumes all points are valid.  This number needs to be interpreted
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
     * return true if each record of DEPEND_0 is the same.  Rank 0 datasets
     * are trivially constant.
     * @param ds any dataset
     * @return true if the dataset doesn't change with DEPEND_0 or is rank 0.
     */
    public static boolean isConstant( QDataSet ds ) {
        if ( ds.rank()==0 || ds.length()==0 ) {
            return true;
        } else {
            QDataSet s1= ds.slice(0);
            for ( int i=1; i<ds.length(); i++ ) {
                if ( !s1.equals(ds.slice(i)) ) return false;
            }
            return true;
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
        if ( ds instanceof MutablePropertyDataSet ) {
            logger.fine("putProperty(QDataSet.QUBE,Boolean.TRUE)");
            ((MutablePropertyDataSet)ds).putProperty( QDataSet.QUBE, Boolean.TRUE );
        }
        return true;
    }
    
    /**
     * check to see if a dataset really is a qube, even if there is a
     * rank 2 dep1.  Note this ignores BUNDLE_1 property if there is a DEPEND_1.
     * This was motivated by the fftPower routine, which returned a rank 2 DEPEND_1,
     * but is typically constant, and RBSP/ECT datasets that often have rank 2 
     * DEPEND_1s that are constant.  This
     * will putProperty(QDataSet.QUBE,Boolean.TRUE) when the dataset really is
     * a qube.
     * @param ds any dataset
     * @return true if the dataset really is a qube.
     */
    public static boolean checkQube( QDataSet ds ) {
        if ( ds.rank()<2 ) {
            return true;
        } else {
            Boolean q = (Boolean) ds.property(QDataSet.QUBE);
            if ( q == null || q.equals(Boolean.FALSE)) {
                if ( SemanticOps.isJoin(ds) ) {
                    return checkQubeJoin(ds);
                }
                for ( int i=1; i<ds.rank(); i++ ) {
                    QDataSet dep= (QDataSet) ds.property( "DEPEND_"+i );
                    if ( dep!=null && dep.rank()>1 ) {
                        if ( !isConstant(dep) ) {
                            return false;
                        }
                    }
                }
                if ( ds instanceof MutablePropertyDataSet ) {
                    logger.fine("putProperty(QDataSet.QUBE,Boolean.TRUE)");
                    ((MutablePropertyDataSet)ds).putProperty( QDataSet.QUBE, Boolean.TRUE );
                }
                return true;
            } else {
                return true;
            }
        }
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
     * returns 1 for zero-length qube, the product otherwise.
     * @param qube
     * @return
     */
    public static int product( int[] qube ) {
        switch ( qube.length ) {
            case 0: return 1;
            case 1: return qube[0];
            case 2: return qube[0]*qube[1];
            case 3: return qube[0]*qube[1]*qube[2];
            case 4: return qube[0]*qube[1]*qube[2]*qube[3];
            default: throw new IllegalArgumentException("qube is too long");
        }
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
        return format( ds, true );
    }

    /**
     * return a human-readable string representing the dataset
     * @param ds the dataset to represent
     * @param showContext show the context property (@slice2=1) if present and ds is rank0.
     * @return
     */
    public static String format(QDataSet ds,boolean showContext) {
        if ( ds.property(QDataSet.BUNDLE_0)!=null ) {
            StringBuilder result= new StringBuilder(); // for documenting context.
            for ( int i=0; i<ds.length(); i++ ) {
                QDataSet cds= DataSetOps.slice0(ds, i);
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
            result.append( new DatumRange( ds.value(0), ds.value(1), u ).toString() );
            result.append( "(inclusive)" );
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
        }
        if ( ds.rank()==0 ) {
            String name= (String) ds.property(QDataSet.NAME);
            Units u= (Units) ds.property(QDataSet.UNITS);
            String format= (String) ds.property( QDataSet.FORMAT );
            StringBuilder result= new StringBuilder();
            if ( name!=null ) {
                result.append(name).append("=");
            }
            if ( format!=null ) {
                if ( u!=null ) {
                    if ( UnitsUtil.isTimeLocation(u) ) {
                        double millis= u.convertDoubleTo(Units.t1970, ds.value() );
                        Calendar cal= Calendar.getInstance();
                        cal.setTimeInMillis( (long)millis ); // TODO: check how to specify to nanos.
                        result.append( String.format(Locale.US,format,cal) );
                    } else {
                        result.append( String.format(Locale.US,format,ds.value()) );
                        if ( u!=Units.dimensionless ) result.append( " " ).append(u.toString());
                    }
                } else {
                    result.append( String.format(Locale.US,format,ds.value()) );
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
     * add method for validating before link is called.
     * @param xds
     * @param yds
     * @param problems insert problem descriptions here, if null then ignore
     * @return true if the datasets can be linked into a valid dataset, false otherwise
     */
    public static boolean validate(QDataSet xds, QDataSet yds, List<String> problems ) {
        if ( xds.length()!=yds.length() ) {
            if (problems == null) problems = new ArrayList<String>();
            problems.add(String.format("DEPEND_%d length is %d, should be %d.", 0, xds.length(), yds.length()));
            return false;
        } else {
            return validate( Ops.link(xds, yds), problems, 0 );
        }
    }

    /**
     * add method for validating before link is called.
     * @param xds
     * @param yds
     * @param zds
     * @param problems insert problem descriptions here, if null then ignore
     * @return true if the datasets can be linked into a valid dataset, false otherwise
     */
    public static boolean validate(QDataSet xds, QDataSet yds, QDataSet zds, List<String> problems ) {
        if ( xds.length()!=yds.length() ) {
            if (problems == null) problems = new ArrayList<String>();
            problems.add(String.format("DEPEND_%d length is %d, should be %d.", 0, xds.length(), yds.length()));
            return false;
        } else {
            return validate( Ops.link(xds, yds, zds ), problems, 0 );
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

    /**
     * internal validate method
     * @param ds a dataset which may not be valid.
     * @param problems null or a list of problems that is appended.
     * @param dimOffset used to check the slices as well for high rank datasets.
     * @return true if the dataset is valid.
     */
    private static boolean validate(QDataSet ds, List<String> problems, int dimOffset) {
        if (problems == null) problems = new ArrayList<String>();
        QDataSet dep = (QDataSet) ds.property(QDataSet.DEPEND_0);
        if (dep != null) {
            if (dep.length() != ds.length()) {
                problems.add(String.format("DEPEND_%d length is %d while data length is %d.", dimOffset, dep.length(), ds.length()));
            }
            if (ds.rank() > 1 && ds.length() > 0) {
                QDataSet dep1= (QDataSet)ds.property(QDataSet.DEPEND_1);
                if ( dep1!=null && dep1.rank()>1 ) {
                    if ( dep1.length()!=ds.length() ) {
                        problems.add(String.format("rank 2 DEPEND_1 length is %d while data length is %d.", dep1.length(), ds.length()));
                    }
                }
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
        QDataSet bds= (QDataSet) ds.property(QDataSet.BUNDLE_1);
        if ( bds!=null ) {
            for ( int i=0; i< Math.min(1,bds.length()); i++ ) {
                QDataSet bds1= DataSetOps.unbundle(ds,i,true); // assumes rank1, so we have excessive work for rank>1
                Object o= bds1.property(QDataSet.DEPEND_1);
                if ( o!=null && !(o instanceof QDataSet) ) {
                    validate( bds1,problems,1) ;
                }
            }
        }
        
        QDataSet plane0 = (QDataSet) ds.property(QDataSet.PLANE_0);
        if ( plane0!=null ) {
            if ( plane0.rank()>0 && plane0.length()!=ds.length() ) { 
                problems.add( String.format( "PLANE_0 length is %d, should be %d", plane0.length(), ds.length() ) );
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
     * @param ds
     * @return
     */
    public static QDataSet bundleWeightsDataSet( final QDataSet ds ) {
        QDataSet bds= (QDataSet)ds.property(QDataSet.BUNDLE_1);
        if ( bds==null ) {
            throw new IllegalArgumentException("dataset must be bundle");
        }
        QDataSet result=null;
        int nb= ds.length(0);
        for ( int i=0; i<nb; i++ ) {
            result= Ops.bundle( result, weightsDataSet( DataSetOps.unbundle(ds,i,false) ) );
        }
        ((MutablePropertyDataSet)result).putProperty( QDataSet.FILL_VALUE, -1e38 ); // codes like total assume this property exists.
        
        return result;
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
     * @param ds the dataset 
     * @return a dataset with the same geometry with zero or positive weights.
     */
    public static QDataSet weightsDataSet(final QDataSet ds) {
        Object o= ds.property(QDataSet.WEIGHTS_PLANE);
        if ( o!=null ) {
            if ( !(o instanceof QDataSet) ) {
                logger.log(Level.WARNING, "WEIGHTS_PLANE contained something that was not a qdataset: {0}", o);
                o=null;
            } else if ( ((QDataSet)o).length()!=ds.length() ) {
                logger.log(Level.WARNING, "WEIGHTS_PLANE was dataset with the wrong length: {0}", o);
                o=null;
            }
        }

        Number ofill= null;
        QDataSet result = (QDataSet) o;
        if ( result!=null ) {
            ofill= (Number)result.property(QDataSet.FILL_VALUE);
            if ( ofill==null ) {
                MutablePropertyDataSet mpds= DataSetOps.makePropertiesMutable( result );
                mpds.putProperty( QDataSet.FILL_VALUE, QDataSet.DEFAULT_FILL_VALUE );
                return mpds;
            } else {
                return result;
            }
        }
        if (result == null) {
            QDataSet bds= (QDataSet)ds.property(QDataSet.BUNDLE_1);
            if ( bds!=null ) {
                return bundleWeightsDataSet(ds);
            }
            Number validMin = (Number) ds.property(QDataSet.VALID_MIN);
            if (validMin == null) validMin = Double.NEGATIVE_INFINITY;
            Number validMax = (Number) ds.property(QDataSet.VALID_MAX);
            if (validMax == null) validMax = Double.POSITIVE_INFINITY;
            Units u = (Units) ds.property(QDataSet.UNITS);
            ofill = (Number) ds.property(QDataSet.FILL_VALUE);
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
                if ( format==null ) {
                    return Datum.create( ds.value(), u );
                } else {
                    return Datum.create( ds.value(), u, new FormatStringFormatter(format, true) );
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
        if ( sloppy==false ) {
            if ( !ds.property( QDataSet.BINS_0 ).equals(QDataSet.VALUE_BINS_MIN_MAX) ) {
                throw new IllegalArgumentException("expected min,max for BINS_0 because we are not allowing sloppy.");
            }
        }
        return new DatumRange( ds.value(0), ds.value(1), u );
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
     * convert the QDataSet to an array.  Units and fill are ignored...
     * @param d
     * @return
     */
    public static double[] asArrayOfDoubles( QDataSet d ) {
        double[] result;
        if ( d.rank()==1 ) {
            DDataSet ds= (DDataSet)ArrayDataSet.maybeCopy( DDataSet.class, d );
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
     * @param d
     * @return
     */
    public static double[][] as2DArrayOfDoubles( QDataSet d ) {
        double[][] result;
        if ( d.rank()==2 ) {
            DDataSet ds= (DDataSet)ArrayDataSet.maybeCopy( DDataSet.class, d );
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
     * @param arr
     * @return
     */
    public static QDataSet asDataSet(Object x, Object y) {
        QDataSet xds= asDataSet(x);
        QDataSet yds= asDataSet(y);
        return Ops.link(xds, yds);
    }

    /**
     * convert java types into QDataSets.
     * @param arr
     * @return
     */
    public static QDataSet asDataSet(Object x, Object y, Object z) {
        QDataSet xds= asDataSet(x);
        QDataSet yds= asDataSet(y);
        QDataSet zds= asDataSet(z);
        return Ops.link(xds, yds, zds);
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

    /**
     * provide the context as a string, for example to label a plot.  The dataset CONTEXT_i properties are inspected,
     * each of which must be one of:
     * <li>rank 0 dataset
     * <li>rank 1 bins dataset
     * <li>rank 1 bundle
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
     * each of which must be one of:
     * <li>rank 0 dataset
     * <li>rank 1 bins dataset
     * <li>rank 1 bundle
     *
     * @param ds the dataset containing context properties which are rank 0 datums or rank 1 datum ranges.
     * @param the delimiter between context elements, such as "," or "!c"
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
                    QDataSet extent= Ops.extent(cds);
                    if ( extent.value(1)==extent.value(0) ) {
                        result.append( DataSetUtil.format(cds.slice(0),false) );  // for CLUSTER/PEACE this happens where rank 1 context is all the same value
                    } else {
                        result.append(DataSetUtil.format(extent, false)).append(" ").append( cds.length() ).append( " different values"); // slice was probably done when we should't have.
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
        Units toUnits= SemanticOps.getUnits( ds );
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
     * This assumes there is no invalid data!
     * @param ds
     * @param datum
     * @return
     */
    public static int closestIndex( QDataSet ds, Datum datum ) {
        if ( ds.rank()!=1 ) {
            throw new IllegalArgumentException("ds rank should be 1");
        }
        if ( ds.length()==0 ) {
            throw new IllegalArgumentException("ds length is zero");
        }
        
        double ddatum= datum.doubleValue( SemanticOps.getUnits(ds) );

        if ( !isMonotonic(ds) ) {
            int closest= 0;
            double v= Math.abs( ds.value(closest)-ddatum );
            for ( int i=1; i<ds.length(); i++ ) {
                double tv= Math.abs( ds.value(i)-ddatum );
                if ( tv < v ) {
                    closest= i;
                    v= tv;
                }
            }
            return closest;

        } else {

            int result= xTagBinarySearch( ds, datum, 0, ds.length()-1 );

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

    public static Object getProperty( QDataSet ds, String name, Object deflt ) {
        Object o= ds.property(name);
        if ( o!=null ) return o; else return deflt;
    }


    /**
     * return the value, which gets units from index i. from rank 1 bundle dataset.
     * @param yds
     * @param value
     * @param i
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
     * return the string value of the double, considering QDataSet.FORMAT, the units and the value.
     * formatting is done assuming someone else will report the units.  If the value is invalid,
     * then "***" is returned.
     * @param yds`
     * @param value
     * @return
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
            if ( form==null ) {
                if ( "log".equals( yds.property(QDataSet.SCALE_TYPE) ) ) {
                    s = String.format( Locale.US, "%9.3e", value ).trim();
                } else {
                    s = String.format( Locale.US, "%9.3f", value ).trim();
                }
            } else {
                try {
                    s = String.format( Locale.US, form, value );
                } catch ( IllegalFormatConversionException ex ) { // '%2X'
                    char c= ex.getConversion();
                    if ( c=='X' || c=='x' || c=='d' || c=='o' || c=='c' || c=='C'  ) {
                        s = String.format( Locale.US, form, (long)value );
                    } else {
                        //warning bad format string
                        s= df.format(d);
                    }
                }
            }
        } else {
            s = df.format(d,u);
        }
        return s;
    }


    /**
     * make a proper bundle ds from a simple bundle containing ordinal units
     * This assumes that labels is a unique set of labels.
     * See http://autoplot.org/QDataSet#DataSet_Properties under BUNDLE_1.
     * See DataSetOps.unbundle
     * @param labelsDs
     * @throws IllegalArgumentException if the input is not rank 1.
     * @return a BundleDescriptor to be set as BUNDLE_i.  See BundleDataSet
     */
    public static MutablePropertyDataSet toBundleDs( QDataSet labels ) {
        if ( labels.rank()!=1 ) throw new IllegalArgumentException("labels must be rank 1");
        IDataSet result=  IDataSet.createRank2( labels.length(), 0 );
        Units u= SemanticOps.getUnits(labels);
        for ( int i=0; i<labels.length(); i++ ) {
            result.putProperty( QDataSet.NAME, i, Ops.safeName( u.createDatum( labels.value(i) ).toString() ) );
            result.putProperty( QDataSet.LABEL, i, u.createDatum( labels.value(i) ).toString() );
        }
        String name= (String) labels.property(QDataSet.NAME);
        if ( name!=null ) result.putProperty( QDataSet.NAME, name );
        String label= (String) labels.property(QDataSet.LABEL);
        if ( label!=null ) result.putProperty( QDataSet.LABEL, label );

        return result;
    }

}


