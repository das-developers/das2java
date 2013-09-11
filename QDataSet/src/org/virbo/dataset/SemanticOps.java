/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dataset;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.das2.datum.Basis;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.EnumerationUnits;
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.NumberUnits;
import org.das2.datum.TimeLocationUnits;
import org.das2.datum.TimeUtil;
import org.das2.datum.Units;
import org.das2.datum.UnitsConverter;
import org.das2.datum.UnitsConverter.ScaleOffset;
import org.das2.datum.UnitsUtil;
import org.das2.util.LoggerManager;
import org.virbo.dsops.Ops;

/**
 * Common expressions that apply semantics to QDataSet.  Introduced
 * to reduce a lot of repeated code, but also to make it clear where semantics
 * are being applied.
 * @author jbf
 */
public class SemanticOps {

    private static final Logger logger= LoggerManager.getLogger("qdataset");
    
    /**
     * returns the units found in the UNITS property of the dataset,
     * or Units.dimensionless if it is not found.
     * @param ds
     * @return
     */
    public static Units getUnits(QDataSet ds) {
        if ( ds==null ) {
            throw new NullPointerException("ds is null"); // breakpoint here
        }
        Units u = (Units) ds.property(QDataSet.UNITS);
        if ( u==null && isJoin(ds) ) {
            u= (Units) ds.slice(0).property(QDataSet.UNITS);
        }
        return u == null ? Units.dimensionless : u;
    }

    public static UnitsConverter getUnitsConverter( QDataSet src, QDataSet dst ) {
        Units usrc= getUnits(src);
        Units udst= getUnits(dst);
        return usrc.getConverter(udst);
    }

    /**
     *
     * @param src
     * @param dst
     * @return
     * @throws InconvertableUnitsException when it just can't be done (EnumerationUnits and Ratiometric)
     */
    public static UnitsConverter getLooseUnitsConverter( QDataSet src, QDataSet dst ) {
        Units usrc= getUnits(src);
        Units udst= getUnits(dst);
        try {
            return usrc.getConverter(udst);
        } catch ( InconvertibleUnitsException ex ) {
            if ( UnitsUtil.isRatioMeasurement(usrc) && UnitsUtil.isRatioMeasurement(udst) ) {
                if ( Units.dimensionless==usrc || Units.dimensionless==udst ) {
                    return UnitsConverter.LOOSE_IDENTITY;
                } else {
                    throw ex;
                }
            } else {
                throw ex;
            }
        }
    }

/**
     * return the labels for a dataset where DEPEND_1 is a bundle dimension.
     * Look for the BUNDLE_1.
     * @param ds
     * @return
     */
    public static String[] getComponentNames(QDataSet ds) {
        int n = ds.length(0);
        QDataSet bdesc= (QDataSet) ds.property(QDataSet.BUNDLE_1);
        if ( bdesc!=null && bdesc.rank()==2 ) {
            String[] result= new String[n];
                for ( int i=0; i<n; i++ ) {
                    result[i]= (String) bdesc.property(QDataSet.NAME, i);
                    if ( result[i]==null ) result[i]="ch_"+i;
                }
            return result;
        }
        QDataSet labels;
        if ( bdesc!=null && bdesc.rank()==1 ) {
            labels= bdesc;
        } else {
            labels= (QDataSet) ds.property(QDataSet.DEPEND_1);
        }

        if (labels == null) {
            String[] result = new String[n];
            for (int i = 0; i < n; i++) {
                result[i] = "ch_" + i;
            }
            return result;
        } else {
            Units u = getUnits(labels);
            String[] slabels = new String[n];
            for (int i = 0; i < n; i++) {
                if (labels == null) {
                    slabels[i] = String.valueOf(i);
                } else {
                    slabels[i] = String.valueOf(u.createDatum(labels.value(i)));
                }
            }
            return slabels;
        }
    }


    /**
     * return the labels for a dataset where DEPEND_1 is a bundle dimension.
     * Look for the BUNDLE_1.
     * @param ds
     * @return
     */
    public static String[] getComponentLabels(QDataSet ds) {
        int n = ds.length(0);
        QDataSet bdesc= (QDataSet) ds.property(QDataSet.BUNDLE_1);
        if ( bdesc!=null && bdesc.rank()==2 ) {
            String[] result= new String[n];
                for ( int i=0; i<n; i++ ) {
                    //result[i]= (String) bdesc.property(QDataSet.NAME, i);
                    result[i]= (String) bdesc.property(QDataSet.LABEL, i);
                    if ( result[i]==null ) result[i]= (String) bdesc.property(QDataSet.NAME, i); // whoops!  It was like this for years!
                    if ( result[i]==null ) result[i]="ch_"+i;
                }
            return result;
        }
        QDataSet labels;
        if ( bdesc!=null && bdesc.rank()==1 ) {
            labels= bdesc;
        } else {
            labels= (QDataSet) ds.property(QDataSet.DEPEND_1);
        }
         
        if (labels == null) {
            String[] result = new String[n];
            for (int i = 0; i < n; i++) {
                result[i] = "ch_" + i;
            }
            return result;
        } else {
            Units u = getUnits(labels);
            String[] slabels = new String[n];
            for (int i = 0; i < n; i++) {
                if (labels == null) {
                    slabels[i] = String.valueOf(i);
                } else {
                    slabels[i] = String.valueOf(u.createDatum(labels.value(i)));
                }
            }
            return slabels;
        }
    }

    /**
     * lookupUnits canonical units object, or allocate one.
     * Examples include:
     *   "nT" where it's already allocated,
     *   "apples" where it allocates a new one, and
     *   "seconds since 2011-12-21T00:00" where it uses lookupTimeUnits.
     * @param units string identifier.
     * @return canonical units object.
     */
    public static synchronized Units lookupUnits(String sunits) {
        Units result;
        sunits= sunits.trim();
        try {
            result= Units.getByName(sunits);
            
        } catch ( IllegalArgumentException ex ) {
            if ( sunits.contains(" since ") ) {
                try {
                    result = lookupTimeUnits(sunits);
                } catch (ParseException ex1) {
                    result= new NumberUnits( sunits );
                }
            } else if ( sunits.equals("sec") ) {   // begin, giant table of kludges
                result= Units.seconds;
            } else if ( sunits.equals("msec") ) {  // CDF
                result= Units.milliseconds;
            } else if ( sunits.contains("(All Qs)")) { //themis files have this annotation on the units. Register a converter. TODO: solve this in a nice way.  The problem is I wouldn't want to assume nT(s) doesn't mean nT * sec.
                result= new NumberUnits( sunits );
                Units targetUnits= lookupUnits( sunits.replace("(All Qs)","").trim() );
                result.registerConverter( targetUnits, UnitsConverter.IDENTITY );
            } else {
                Pattern multPattern= Pattern.compile("([.0-9]+)\\s*([a-zA-Z]+)");
                Matcher m= multPattern.matcher(sunits);
                if ( m.matches() ) { // kludge for ge_k0_mgf which has "0.1nT" for units.  We register a converter when we see these.  Note this is going to need more attention
                    try {
                        Units convTo;
                        convTo = SemanticOps.lookupUnits(m.group(2));
                        if ( convTo!=null ) {
                            double fact= Double.parseDouble(m.group(1));
                            result= new NumberUnits( sunits );
                            result.registerConverter( convTo, new ScaleOffset(fact,0.0) );
                        } else {
                            result= SemanticOps.lookupUnits(sunits);
                        }
                    } catch ( NumberFormatException ex2 ) {
                        result= SemanticOps.lookupUnits(sunits);
                    }
                } else {
                    result= new NumberUnits( sunits );
                }
            }
        }

        // look to see if there is a standard unit for this and register a converter if so.  E.g.  [ms]<-->ms
        String stdunits= sunits;
        if ( stdunits.startsWith("[") && stdunits.endsWith("]") ) { // we can't just pop these off.  Hudson has case where this causes problems.  We need to make units in vap files canonical as well.
            stdunits= stdunits.substring(1,stdunits.length()-1);
        }
        if ( stdunits.startsWith("(") && stdunits.endsWith(")") ) { // often units get [] or () put around them.  Pop these off.
            stdunits= stdunits.substring(1,stdunits.length()-1);
        }
        if ( !stdunits.equals(sunits) ) {
            Units stdUnit= lookupUnits(stdunits);  // we need to register "foo" when "[foo]" so that order doesn't matter.
            if ( !stdUnit.isConvertableTo(result) ) {
                System.err.println("registering identity converter "+stdUnit + " -> "+ result );
                stdUnit.registerConverter( result, UnitsConverter.IDENTITY );
                stdUnit.getConverter(result);
            }
        }
        return result;
    }

    /**
     * return canonical das2 unit for colloquial time.
     * @param string
     * @return
     */
    public static Units lookupTimeLengthUnit(String s) throws ParseException {
        s= s.toLowerCase().trim();
        if ( s.startsWith("sec") || s.equals("s") ) {
            return Units.seconds;
        } else if ( s.startsWith("ms") || s.startsWith("millisec") || s.startsWith("milliseconds") ) {
            return Units.milliseconds;
        } else if ( s.equals("hr") || s.startsWith("hour") ) {
            return Units.hours;
        } else if ( s.equals("mn") || s.startsWith("min") ) {
            return Units.minutes;
        } else if ( s.startsWith("us") || s.startsWith("\u00B5s" ) || s.startsWith("micros")) {
            return Units.microseconds;
        } else if ( s.startsWith("ns") || s.startsWith("nanos" ) ) {
            return Units.nanoseconds;
        } else if ( s.startsWith("d") ) { //TODO: yikes...
            return Units.days;
        } else {
            throw new ParseException("failed to identify unit: "+s,0);
        }
    }

    /**
     * lookupUnits canonical units object, or allocate one.  If one is
     * allocated, then parse for "<unit> since <datum>" and add conversion to
     * "microseconds since 2000-001T00:00."  Note leap seconds are ignored!
     * @param timeUnits
     * @return
     */
    public static synchronized Units lookupTimeUnits( String units ) throws ParseException {

        Units result;

        //for speed, see if it's already registered.
        try {
            result= Units.getByName(units);
            return result;
        } catch ( IllegalArgumentException ex ) {
            //do nothing until later
        }


        String[] ss= units.split("since");
        Units offsetUnits= lookupTimeLengthUnit(ss[0]);
        Datum datum;

        if ( ss[1].equals(" 1-1-1 00:00:00" ) ) { // make this into something that won't crash.
            //datum= Units.mj1958.createDatum(-714779);
            ss[1]= "1901-01-01 00:00:00"; // /media/mini/data.backup/examples/netcdf/sst.ltm.1961-1990.nc
        }
        if ( ss[1].contains("1970-01-01 00:00:00.0 0:00") ) {
            ss[1]= "1970-01-01 00:00:00";
        }
        if ( ss[1].endsWith(" UTC") ) { // http://www.ngdc.noaa.gov/stp/satellite/dmsp/f16/ssj/2011/01/f16_20110101_ssj.h5?TIME
            ss[1]= ss[1].substring(0,ss[1].length()-4);
        }
        datum= TimeUtil.create(ss[1]);
        String canonicalName = "" + offsetUnits + " since "+ datum;
        try {
            result= Units.getByName(canonicalName);
            return result;
        } catch ( IllegalArgumentException ex ) {
            Basis basis= new Basis( "since "+ datum, "since "+ datum, Basis.since2000, datum.doubleValue(Units.us2000), Units.us2000.getOffsetUnits() );
            result= new TimeLocationUnits( canonicalName, canonicalName, offsetUnits, basis );
            result.registerConverter( Units.us2000,
                    new ScaleOffset(
                    offsetUnits.convertDoubleTo(Units.microseconds, 1.0),
                    datum.doubleValue(Units.us2000) ) );
            return result;
        }
    }

    public static boolean isRank1Bundle(QDataSet ds) {
        if ( ds.rank()!=1 ) return false;
        if ( ds.property(QDataSet.BUNDLE_0)!=null ) {
            return true;
        } else {
            QDataSet dep= (QDataSet) ds.property(QDataSet.DEPEND_0);
            if ( dep==null ) return false;
            Units depu= getUnits(dep);
            if ( depu instanceof EnumerationUnits ) {
                return true;
            } else {
                return false;
            }
        }
    }
    
    /**
     * Test for bundle scheme.  Returns true if the BUNDLE_1 is set.
     * @param ds
     * @return true if the dataset is a bundle
     */
    public static boolean isBundle(QDataSet ds) {
        return ds.rank()==2 && ds.property(QDataSet.BUNDLE_1)!=null;
    }

    /**
     * Test for rank 2 waveform dataset, where DEPEND_1 is offset from DEPEND_0, and the data is the waveform.
     *  DEPEND_1 must be at least 128 elements long.
     *  DEPEND_1 must not be dimensionless.
     * @param fillDs
     * @return
     */
    public static boolean isRank2Waveform(QDataSet fillDs ) {
        if ( fillDs.rank()==2 ) {
            QDataSet dep0= (QDataSet) fillDs.property(QDataSet.DEPEND_0);
            QDataSet dep1= (QDataSet) fillDs.property(QDataSet.DEPEND_1);
            if ( dep0!=null && dep1!=null && dep1.length()>=QDataSet.MIN_WAVEFORM_LENGTH ) {
                Units dep0units= SemanticOps.getUnits( dep0 );
                Units dep1units= SemanticOps.getUnits( dep1 );
                if ( dep0units!=Units.dimensionless && dep1units.isConvertableTo( dep0units.getOffsetUnits() ) ) {
                    return( true );
                }
            }
        }
        return false;
    }

    /**
     * See Ops.isLegacyBundle
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
        }
        return false;
    }
    /**
     * Test for bins scheme, where BINS_1 (or BINS_0) is set.  
     * This is where a two-element index is min, max.
     * Note the BINS dimension must be the last index of the QDataSet.
     * @param ds
     * @return
     */
    public static boolean isBins(QDataSet ds ) {
        String binsProp= (String) ds.property( "BINS_"+(ds.rank()-1) );
        boolean bins= binsProp!=null && ( QDataSet.VALUE_BINS_MIN_MAX.equals( binsProp ) || "min,maxInclusive".equals( binsProp ) );
        return bins;
    }
    
    /**
     * returns true if the dataset indicates that it is monotonically
     * increasing.  See DataSetUtil.isMonotonic.
     * @param ds
     * @return
     */
    public static boolean isMonotonic( QDataSet ds ) {
        return DataSetUtil.isMonotonic(ds);
    }

    /**
     * returns true if the dataset is rank 2 or greater with the first dimension a join dimension.
     * Note this does not return true for implicit joins, where JOIN_0 is not set.
     * @param ds
     * @return
     */
    public static boolean isJoin( QDataSet ds ) {
        return ds.rank()>1 && ds.property(QDataSet.JOIN_0)!=null;
    }

    /**
     * returns the plane requested by name, or null if it does not exist.
     * If the name is PLANE_i, then return PLANE_i, otherwise return
     * the dataset with this name.
     * Note QDataSet has the rule that if PLANE_i is null, then all PLANE_(i+1)
     * must also be null.
     * @param ds
     * @param name
     * @return
     */
    public static QDataSet getPlanarView( QDataSet ds, String name ) {
        if ( ds.property( QDataSet.PLANE_0 )==null ) return null; // typical case, get them out of here quickly
        if ( name.equals("") ) throw new IllegalArgumentException("empty name");
        if ( name.charAt(0)=='P' && Pattern.matches( "PLANE_(\\d|\\d\\d)", name ) ) {
            return (QDataSet)ds.property(name);
        }
        int i=0;
        while ( i<QDataSet.MAX_PLANE_COUNT ) {
            QDataSet plane= (QDataSet) ds.property( "PLANE_"+i );
            if ( plane==null ) {
                return null;
            } else {
                String tname= (String) plane.property( QDataSet.NAME );
                if ( tname==null ) {
                    System.err.println("unnamed plane in "+ds );
                } else {
                    if ( name.equals(tname) ) {
                        return plane;
                    }
                }
            }
            i++;
        }
        return null;
    }

    /**
     * return the weights dataset, possibly creating one based on VALID_MIN
     * VALID_MAX and FILL_VALUE properties.  The weights dataset will have
     * value zero where the data is invalid, and a non-zero weight where it is
     * valid.  DataSets may also contain a weights table with relative weights,
     * but this is not uniformly supported.  
     * Note: this uses QDataSet.WEIGHTS_PLANE
     * Note: calls org.virbo.dataset.DataSetUtil.weightsDataSet.
     * @see Ops.valid, which is equivalent
     * @see cadenceCheck, which detects for gaps in cadence.
     * @param ds
     * @return QDataSet with same geometry containing zeros and non-zeros.
     */
    public static QDataSet weightsDataSet( QDataSet ds ) {
        return DataSetUtil.weightsDataSet(ds);
    }

    /**
     * return a reasonable tag width to use, or null if one cannot be
     * guessed.
     * @param ds the dataset containing data with a cadence.
     * @param yds null or a dataset that may contain fill.
     * @return
     */
    public static Datum guessXTagWidth( QDataSet ds, QDataSet yds ) {
        RankZeroDataSet cadence= DataSetUtil.guessCadenceNew( ds, yds );
        return cadence==null ? null : DataSetUtil.asDatum( cadence );
    }

    /**
     * return the dataset containing the x tags for the dataset.  This
     * is QDataSet.DEPEND_0, or if that's null then IndexGenDataSet(ds.length).
     * For a bundle, this is just the 0th dataset.  
     * For a join, this is a join of the xtags datasets of each dataset.
     * @param ds
     * @return
     */
    public static QDataSet xtagsDataSet( QDataSet ds ) {
        QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
        if ( dep0!=null ) return dep0;
        if ( isBundle(ds) && !isBins(ds) ){
            return DataSetOps.unbundle(ds,0);
        } else if ( isLegacyBundle(ds) ) {
            return DataSetOps.unbundle(ds,0);
        } else if ( isJoin(ds) && ds.rank()>2 ) {  // support only RPWS rank 3 array of spectrograms.
            QDataSet xds= xtagsDataSet( ds.slice(0) );
            JoinDataSet result= new JoinDataSet(xds);
            for ( int i=1; i<ds.length(); i++ ) {
                result.join( xtagsDataSet(ds.slice(i) ) );
            }
            return result;
        } else {
            return new IndexGenDataSet(ds.length());
        }
    }

    /**
     * return the ytags for the simple table that is ds.
     *   rank 2 spectrogram: Z[X,Y] -> Y
     *   bundle_1: ds[ :, [x,y,z] ] -> y
     *   [x,y,z] -> y
     * consider: These break duck typing goal, and really need a scheme
     *   to tell it how to get the dataset.
     * @param ds
     * @return
     */
    public static QDataSet ytagsDataSet( QDataSet ds ) {
        QDataSet dep1= (QDataSet) ds.property(QDataSet.DEPEND_1);
        if ( dep1!=null ) {
            if ( SemanticOps.getUnits(dep1) instanceof EnumerationUnits ) {
                if ( dep1.length()==1 ) {
                    return DataSetOps.slice1(ds,0);// Juno returned a one-element
                } else {
                    return DataSetOps.slice1(ds,1);
                }
            } else {
                return dep1;
            }
        } else if ( isBundle(ds) ) {
            if ( ds.length(0)==1 ) { // test003_008 would use rank2 dataSet[IsoDat...=1441,1] to indicate Y(T).  http://www.rbsp-ect.lanl.gov/data_pub/autoplot/scripts/rbsp_ephem.jyds
                return DataSetOps.unbundle(ds,0);
            } else {
                return DataSetOps.unbundle( ds, 1 );
            }
        } else if ( isLegacyBundle(ds) ) {
            return DataSetOps.unbundle(ds,1);
        } else if ( isJoin(ds)) {
            QDataSet yds= ytagsDataSet( ds.slice(0) );
            JoinDataSet result= new JoinDataSet(yds);
            for ( int i=1; i<ds.length(); i++ ) {
                result.join( ytagsDataSet(ds.slice(i) ) );
            }
            return result;
        } else if ( ds.length()>0 
                && ds.property(QDataSet.DEPEND_1)==null
                && ds.property(QDataSet.DEPEND_0,0)!=null ) { // For Juno pktid=91
            if ( DataSetUtil.isQube(ds) ) {
                return xtagsDataSet( ds.slice(0) );
            } else {
                QDataSet yds= xtagsDataSet( ds.slice(0) );
                JoinDataSet result= new JoinDataSet(yds);
                for ( int i=1; i<ds.length(); i++ ) {
                    result.join( xtagsDataSet(ds.slice(i) ) );
                }
                result.putProperty( QDataSet.UNITS,  yds.slice(0).property( QDataSet.UNITS ) );
                return result;
            }

        } else {
            if ( ds.rank()==1 ) {
                return ds;
            } else if ( ds.rank()==2 && isBins(ds) ) {
                return ds;
            } else {
                QDataSet result= (QDataSet) ds.property(QDataSet.DEPEND_1);
                if ( result==null ) {
                    return new IndexGenDataSet(ds.length(0));
                } else {
                    return result;
                }
            }
        }
    }

    /**
     * return the first table of the bundle containing x and y
     * @param tds
     * @param x
     * @param y
     * @return
     */
    public static QDataSet getSimpleTableContaining( QDataSet tds, Datum x, Datum y ) {
        if ( tds.rank()==2 ) {
            return tds;
        } else {
            for ( int i=0; i<tds.length(); i++ ) {
                QDataSet tds1= tds.slice(i);
                QDataSet bounds= SemanticOps.bounds(tds1);
                Units xunits= SemanticOps.getUnits( SemanticOps.xtagsDataSet(tds1) );
                Units yunits= SemanticOps.getUnits( SemanticOps.ytagsDataSet(tds1) );
                if (  yunits instanceof EnumerationUnits ) {
                    return DataSetOps.slice2( tds, 0 ); //TODO: code this nicely.  This is some something comes up with Image datasets.  
                }
                if ( bounds.value(0,0)<=x.doubleValue(xunits) && x.doubleValue(xunits)<bounds.value(0,1)
                        && bounds.value(1,0)<=y.doubleValue(yunits) && y.doubleValue(yunits)<bounds.value(1,1) ) {
                    return tds1;
                }
            }
        }
        return null;
    }

    /**
     * return the dataset that is dependent on others.  For a bundle, we
     * use DataSetOps.unbundleDefaultDataSet
     * @param ds
     * @return
     */
    public static QDataSet getDependentDataSet( QDataSet ds ) {
        QDataSet vds;
        if ( !SemanticOps.isTableDataSet(ds) ) {
            if ( ds.rank()==2 && SemanticOps.isBundle(ds) ) {
                vds = DataSetOps.unbundleDefaultDataSet( ds );
            } else {
                vds = (QDataSet) ds;
            }
            return vds;
        } else  {
            return ds;
        }
    }

    /**
     * return the bounds DS[ (x,y), (min,max) ] of the datasets
     * independent parameters.  This is only implemented for:
     *   rank 2 Tables
     *   rank 3 array of tables
     *   rank 1 Y(X)
     *   not for rank 2 bundle dataset
     *   not for rank 1 bundle dataset
     * @param ds rank 2 dataset with BINS_1="min,maxInclusive"
     * @throws IllegalArgumentException when the dataset scheme is not supported
     * @return
     */
    public static QDataSet bounds( QDataSet ds ) {
        QDataSet xrange;
        QDataSet yrange;

        if ( ds.rank()==2 ) {
            if ( ds.property(QDataSet.BUNDLE_1)!=null && ds.property(QDataSet.BINS_1)==null ) {
                throw new IllegalArgumentException("scheme not supported: "+ds ); 
            } else {
                xrange= Ops.extent( SemanticOps.xtagsDataSet(ds), null );
                yrange= Ops.extent( SemanticOps.ytagsDataSet(ds), null );
            }
        } else if ( ds.rank()==3 ) {
            QDataSet ds1= ds.slice(0);
            xrange= Ops.extent( SemanticOps.xtagsDataSet(ds1), null );
            yrange= Ops.extent( SemanticOps.ytagsDataSet(ds1), null );
            for ( int i=1; i<ds.length(); i++ ) {
                ds1= ds.slice(i);
                xrange= Ops.extent( SemanticOps.xtagsDataSet(ds1), xrange );
                yrange= Ops.extent( SemanticOps.ytagsDataSet(ds1), yrange );
            }
        } else if ( ds.rank()==1 ) {
            if ( ds.property(QDataSet.BUNDLE_0)!=null ) {
                throw new IllegalArgumentException("scheme not supported: "+ds );
            } else {
                xrange= Ops.extent( SemanticOps.xtagsDataSet(ds), null );
                yrange= Ops.extent( SemanticOps.ytagsDataSet(ds), null );
            }
        } else {
            throw new IllegalArgumentException("scheme not supported: "+ds );
        }

        JoinDataSet jds= (JoinDataSet) Ops.join( xrange, yrange );
        jds.putProperty( QDataSet.BINS_1,"min,maxInclusive");
        return jds;

//        DDataSet result= DDataSet.createRank2(2,2);
//        result.putValue(0,0,xrange.value(0));
//        result.putValue(0,1,xrange.value(1));
//        result.putValue(1,0,yrange.value(0));
//        result.putValue(1,1,yrange.value(1));
//        result.putProperty(QDataSet.BINS_1,"min,maxInclusive");
//
        //return result;
    }

    /**
     * returns true if the dataset is the scheme of a legacy TableDataSet
     * @param ds
     * @return
     */
    public static boolean isTableDataSet(QDataSet ds) {
         if ( ds.rank()==3 || isSimpleTableDataSet(ds) ) return true;
         QDataSet dep1= (QDataSet) ds.property( QDataSet.DEPEND_1 );
         if ( ds.rank()==2 && dep1!=null ) {
             return true; // Y tags can be rank2, making it a table dataset but not a simple one.
         }
         return false;
    }

    /**
     * returns true if the dataset is the scheme of a legacy TableDataSet
     * with only one table.  Note "Tables" have just one X unit and one Y unit,
     * no bundles.
     * Consider: rule about "duck typing": rules like !Ops.isBundle break this
     *  because it requires to be a simpleTable, you cannot be a bundle.  LANL
     *  rich ASCII allows datasets to be both bundles and simple tables.
     * @param ds
     * @return
     */
    public static boolean isSimpleTableDataSet(QDataSet ds) {
        QDataSet dep1= (QDataSet) ds.property( QDataSet.DEPEND_1 );
        return ds.rank()==2 && ( ( dep1!=null && dep1.rank()==1 ) || !Ops.isBundle(ds) ) && !Ops.isLegacyBundle(ds);
    }

    /**
     * returns true if the dataset is a bundle of rank 1 datasets.  If no
     * dependence is declared, it is assumed that the first one or two datasets
     * are the independent datasets, and the last is the dependent. 
     *    X,Y   -->  Y(X)
     *    X,Y,Z -->  Z(X,Y)
     *
     * @param ds
     * @return
     */
    public static boolean isSimpleBundleDataSet( QDataSet ds ) {
         return ds.rank()==2 && ( ds.property(QDataSet.BUNDLE_1)!=null );
    }

    /**
     * returns true if the dataset is a time series.  This is either something that has DEPEND_0 as a dataset with time
     * location units, or a join of other datasets that are time series.
     * @param ds
     * @return
     */
    public static boolean isTimeSeries( QDataSet ds ) {
        if ( isJoin(ds) ) {
            return isTimeSeries( ds.slice(0) );
        } else {
            QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
            return dep0!=null && UnitsUtil.isTimeLocation( SemanticOps.getUnits(dep0) );
        }
    }

    /**
     * returns the Double value of the number, preserving null and NaN.
     * @param value
     * @return
     */
    public static Double doubleValue( Number value ) {
        if ( value==null ) return null;
        return Double.valueOf( value.doubleValue() );
    }

    /**
     * returns the value as a datum.  Note this should be used with reservation,
     * this is not very efficient when the operation is done many times.
     * @param ds
     * @param d
     * @return
     */
    public static Datum getDatum( QDataSet ds, double d ) {
        Units u = SemanticOps.getUnits(ds);
        Double vmin=  doubleValue( (Number) ds.property( QDataSet.VALID_MIN ) );
        Double vmax=  doubleValue( (Number) ds.property( QDataSet.VALID_MAX ) );
        Double fill= doubleValue( (Number)ds.property( QDataSet.FILL_VALUE ) );
        if ( vmin!=null ) if ( vmin>d ) return u.getFillDatum();
        if ( vmax!=null ) if ( vmax<d ) return u.getFillDatum();
        if ( fill!=null ) if ( fill==d ) return u.getFillDatum();
        return u.createDatum(d);
    }

    /**
     * return the parts of the dataset within the bounds.
     * @param ds
     * @param xrange
     * @param yrange
     */
    public static QDataSet trim( QDataSet ds, DatumRange xrange, DatumRange yrange ) {
        int rank=ds.rank();
        if ( ds.rank()==0 ) return ds;
        if ( xrange==null && yrange==null ) return ds;
        if ( rank==3 && isJoin(ds) ) {
            JoinDataSet jds= new JoinDataSet(ds.rank());
            for ( int i=0; i<ds.length(); i++ ) {
                jds.join( trim( ds.slice(i), xrange, yrange ) );
            }
            DataSetUtil.putProperties( DataSetUtil.getProperties(ds), jds );
            return jds;
        } else if ( rank==2 ) {
            if ( isSimpleTableDataSet(ds) ) {
                QDataSet xds= SemanticOps.xtagsDataSet(ds);
                QDataSet yds= SemanticOps.xtagsDataSet(ds.slice(0));
                QDataSet xinside= xrange==null ? null :
                    Ops.and( Ops.ge( xds, DataSetUtil.asDataSet(xrange.min()) ), Ops.le(  xds, DataSetUtil.asDataSet(xrange.max()) ) );
                QDataSet yinside= yrange==null ? null :
                    Ops.and( Ops.ge( yds, DataSetUtil.asDataSet(yrange.min()) ), Ops.le(  yds, DataSetUtil.asDataSet(yrange.max()) ) );
                SubsetDataSet sds= new SubsetDataSet(ds);
                if ( xinside!=null ) sds.applyIndex( 0, Ops.where(xinside) );  //TODO: consider the use of trim which would be more efficient.
                if ( yinside!=null ) sds.applyIndex( 1, Ops.where(yinside) );
                return sds;

            } else { // copy over elements where
                QDataSet xds= SemanticOps.xtagsDataSet(ds);
                QDataSet yds= SemanticOps.getDependentDataSet(ds);
                QDataSet xinside= xrange==null ? null :
                    Ops.and( Ops.ge( xds, DataSetUtil.asDataSet(xrange.min()) ), Ops.le(  xds, DataSetUtil.asDataSet(xrange.max()) ) );
                QDataSet yinside= null; //yrange==null ? null :
                    //Ops.and( Ops.ge( yds, DataSetUtil.asDataSet(yrange.min()) ), Ops.le(  yds, DataSetUtil.asDataSet(yrange.max()) ) );
                QDataSet ok;
                SubsetDataSet sds= new SubsetDataSet(ds);
                if ( xrange==null && yrange==null ) {
                    return ds;
                } else if ( xrange==null ) {
                    ok= Ops.where( yinside );
                    sds.applyIndex( 1, ok );
                } else if ( yrange==null ) {
                    ok= Ops.where( xinside );
                    sds.applyIndex( 0, ok );
                } else {
                    logger.fine( "yds is being ignored, not sure why...");
                    //ok= Ops.where( Ops.and( xinside, yinside ) );
                    ok= Ops.where( xinside );
                    sds.applyIndex( 0, ok );
                }
                return sds;
                
            }
        } else if ( rank==1 ) { 
            QDataSet xds= SemanticOps.xtagsDataSet(ds);
            QDataSet yds= SemanticOps.getDependentDataSet(ds);
            QDataSet xinside= null;
            if ( DataSetUtil.isMonotonic(xds) && xds.property(QDataSet.FILL_VALUE)==null ) {  // TODO: validmin validmax...
                if ( xrange!=null ) {
                    int i= DataSetUtil.xTagBinarySearch( xds, xrange.min(), 0, xds.length() );
                    if ( i<0 ) {
                        i= -1 * i + 1 ;
                    }
                    int j= DataSetUtil.xTagBinarySearch( xds, xrange.max(), i, xds.length() );
                    if ( j<0 ) {
                        j= -1 * j + 1 ;
                    }
                    if ( yrange==null ) {
                        return ds.trim(i,j);  // optimization for waveforms...
                        
                    } else {
                        int[] back= new int[ xds.length() ];
                        for ( int ii=i; ii<=j; ii++ ) {
                            back[ii]= 1;
                        }
                        xinside= IDataSet.wrap(back);
                    }
                }
            } else {
                xinside= xrange==null ? null :
                Ops.and( Ops.ge( xds, DataSetUtil.asDataSet(xrange.min()) ), Ops.le(  xds, DataSetUtil.asDataSet(xrange.max()) ) );
            }
            QDataSet yinside= yrange==null ? null :
                Ops.and( Ops.ge( yds, DataSetUtil.asDataSet(yrange.min()) ), Ops.le(  yds, DataSetUtil.asDataSet(yrange.max()) ) );
            
            QDataSet ok;
            if ( xrange==null ) {
                ok= Ops.where( yinside );
            } else if ( yrange==null ) {
                ok= Ops.where( xinside );
            } else {
                ok= Ops.where( Ops.and( xinside, yinside ) );
            }
            SubsetDataSet sds= new SubsetDataSet(ds);
            sds.applyIndex( 0, ok );
            return sds;
        } else {
            throw new IllegalArgumentException("not supported: "+ds);
        }
    }

    /**
     * return a dataset with 1's where the cadence following this measurement is acceptable, and 0's where
     * there should be a break in the data.  For example, here's some pseudocode:
     * <pre>
     *   findex= Ops.interpolate( xds, x )
     *   cadenceCheck= cadenceCheck(xds)
     *   r= where( cadenceCheck[floor(findex)] eq 0 )
     *   x[r]= fill
     * </pre>
     * Presently this just uses guessXTagWidth to get the cadence, but this may allow a future version to support
     * mode changes.
     *
     * The result is a dataset with the same length, and the last element is always 1.
     *
     * @see Ops.valid which checks for fill and valid_min, valid_max.
     * @param tds rank 1 dataset of length N.
     * @param ds dataset dependent on tds and used to detect valid measurements, or null.
     * @return dataset with length N
     */
    public static QDataSet cadenceCheck( QDataSet tds, QDataSet ds ) {
        Datum cadence= guessXTagWidth( tds, ds );
        cadence= cadence.multiply(1.1);

        QDataSet diffs= Ops.diff(tds);
        QDataSet result= (MutablePropertyDataSet) Ops.lt( diffs, DataSetUtil.asDataSet(cadence) ); // cheat cast
        if ( !( result instanceof ArrayDataSet ) ) {
            result= ArrayDataSet.copy(result);
        }
        ArrayDataSet aresult= ((ArrayDataSet)result);
        ArrayDataSet one= ArrayDataSet.createRank1( aresult.getComponentType(), 1 );
        one.putValue(0,1.0);
        DataSetUtil.copyDimensionProperties( aresult,one );
        result= ArrayDataSet.append( aresult, one );

        result= Ops.link( tds, result );

        return result;
        
    }
    
    private static final Map<String,Class> propertyTypes= new HashMap();
    static {
        propertyTypes.put( QDataSet.UNITS, Units.class );
        propertyTypes.put( QDataSet.TYPICAL_MIN, Number.class );
        propertyTypes.put( QDataSet.TYPICAL_MAX, Number.class );
        propertyTypes.put( QDataSet.VALID_MIN, Number.class );
        propertyTypes.put( QDataSet.VALID_MAX, Number.class );
        propertyTypes.put( QDataSet.FILL_VALUE, Number.class );
        propertyTypes.put( QDataSet.CACHE_TAG, org.das2.datum.CacheTag.class );
        propertyTypes.put( QDataSet.CADENCE, QDataSet.class );
        propertyTypes.put( QDataSet.DEPEND_0, QDataSet.class );
        propertyTypes.put( QDataSet.DEPEND_1, QDataSet.class );
        propertyTypes.put( QDataSet.DEPEND_2, QDataSet.class );
        propertyTypes.put( QDataSet.DEPEND_3, QDataSet.class );
        propertyTypes.put( QDataSet.BUNDLE_0, QDataSet.class );
        propertyTypes.put( QDataSet.BUNDLE_1, QDataSet.class );
        propertyTypes.put( QDataSet.DELTA_PLUS, QDataSet.class );
        propertyTypes.put( QDataSet.DELTA_MINUS, QDataSet.class );
        propertyTypes.put( QDataSet.BIN_PLUS, QDataSet.class );
        propertyTypes.put( QDataSet.BIN_MINUS, QDataSet.class );        
    }
    
    /**
     * verify property types.  For example, that UNITS is a org.das2.datum.Units, etc.
     * Returns true for unrecognized property names (future expansion) and null.  If 
     * throwException is true, then an IllegalArgumentException is thrown.
     * @param prop the property name, e.g. QDataSet.CADENCE
     * @param value the candidate value for the property.
     * @param throwException if true, throw descriptive exception instead of returning false.
     * @return 
     */
    public static boolean checkPropertyType( String prop, Object value, boolean throwException ) {
        Class typ= propertyTypes.get(prop);
        if ( typ==null || value==null || typ.isAssignableFrom( value.getClass() ) ) {
            return true;
        } else {
            if ( throwException ) {
                String styp= typ.toString();
                if ( typ==Number.class ) {
                    styp="Number";
                } else if ( typ==QDataSet.class ) {
                    styp="QDataSet";
                }
                if ( value instanceof String ) {
                    throw new IllegalArgumentException("bad value for property "+prop+": \""+value+"\", expected "+styp );
                } else {
                    throw new IllegalArgumentException("bad value for property "+prop+": "+value+", expected "+styp );
                }
            } else {
                return false;
            }
        }
    }

}
