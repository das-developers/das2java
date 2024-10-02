
package org.das2.qds;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.das2.datum.CacheTag;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.EnumerationUnits;
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.Units;
import org.das2.datum.UnitsConverter;
import org.das2.datum.UnitsUtil;
import org.das2.qds.examples.Schemes;
import org.das2.util.LoggerManager;
//import static org.das2.qds.DataSetUtil.isConstant;
import org.das2.qds.ops.Ops;

/**
 * Common expressions that apply semantics to QDataSet.  Introduced
 * to reduce a lot of repeated code, but also to make it clear where semantics
 * are being applied.
 * @author jbf
 */
public final class SemanticOps {

    /**
     * this is a utility class which cannot be instantiated.
     */
    private SemanticOps() {   
    }
    
    private static final Logger logger= LoggerManager.getLogger("qdataset.ops");
    private static final String CLASSNAME= SemanticOps.class.getName();
    
    /**
     * returns the units found in the UNITS property of the dataset,
     * or Units.dimensionless if it is not found.
     * @param ds
     * @return the units found in the dataset, or Units.dimensionless.
     */
    public static Units getUnits(QDataSet ds) {
        if ( ds==null ) {
            throw new NullPointerException("ds is null"); // breakpoint here
        }
        Units u = (Units) ds.property(QDataSet.UNITS);
        if ( u==null && ( ds.rank()>1 && ds.property(QDataSet.JOIN_0)!=null ) ) {
            u= (Units) ds.slice(0).property(QDataSet.UNITS);
        }
        return u == null ? Units.dimensionless : u;
    }

    /**
     * return the UnitsConverter that will convert data from src to the units of dst.
     * @param src the dataset from which we get the original units.
     * @param dst the dataset from which we get the destination units.
     * @return the UnitsConverter
     * @throws IllegalArgumentException
     */
    public static UnitsConverter getUnitsConverter( QDataSet src, QDataSet dst ) {
        Units usrc= getUnits(src);
        Units udst= getUnits(dst);
        return usrc.getConverter(udst);
    }

    /**
     * returns the UnitsConverter, or IDENTITY if the converter cannot be found
     * and one of the two units is dimensionless.
     * @param src the dataset from which we get the original units.
     * @param dst the dataset from which we get the destination units.
     * @return the UnitsConverter
     * @throws InconvertibleUnitsException when it just can't be done (EnumerationUnits and Ratiometric)
     */
    public static UnitsConverter getLooseUnitsConverter( QDataSet src, QDataSet dst ) {
        Units usrc= getUnits(src);
        Units udst= getUnits(dst);
        if ( usrc.isConvertibleTo(udst)) {
            return usrc.getConverter(udst);
        } else {
            if ( UnitsUtil.isRatioMeasurement(usrc) && UnitsUtil.isRatioMeasurement(udst) ) {
                if ( Units.dimensionless==usrc || Units.dimensionless==udst ) {
                    return UnitsConverter.LOOSE_IDENTITY;
                } else {
                    return usrc.getConverter(udst); // throws InconvertibleUnitsException
                }
            } else {
                return usrc.getConverter(udst); // throws InconvertibleUnitsException
            }
        }
    }

    /**
     * return the labels for a bundle dataset.  For a rank 2 bundle, this
     * will be found in BUNDLE_1, or legacy ones may have nominal data for DEPEND_1.
     * For a rank 1 bundle this will be BUNDLE_0.
     * @param ds rank 1 or rank 2 bundle.
     * @return the column names.
     */
    public static String[] getComponentNames(QDataSet ds) {
        int n = ds.length(0);
        QDataSet bdesc;
        switch (ds.rank()) {
            case 2:
                bdesc= (QDataSet) ds.property(QDataSet.BUNDLE_1);
                break;
            case 1:
                bdesc= (QDataSet) ds.property(QDataSet.BUNDLE_0);
                break;
            default:
                bdesc= null;
                break;
        }
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
            //if ( labels.rank()>1 && isConstant(labels) ) {
            //    labels= labels.slice(0);
            //}
            for (int i = 0; i < n; i++) {
                if ( labels.rank()>1 ) {
                    slabels[i] = "ch_" + i;
                } else {
                    slabels[i] = String.valueOf(u.createDatum(labels.value(i)));
                }
            }
            return slabels;
        }
    }


    /**
     * return the labels for a dataset using BUNDLE_1 and DEPEND_1.  If BUNDLE_1 is defined and contains a LABEL,
     * then it is used, otherwise a string value of the data is used, supporting legacy bundles, and if no DEPEND_1 is found
     * then use the NAME properties from the bundle, or "ch_<em>i</em>" for each channel.
     * @param ds the dataset, with a BUNDLE_1 or DEPEND_1 dimension which could be used.
     * @return labels for each bundled dataset.
     */
    public static String[] getComponentLabels(QDataSet ds) {
        int n = ds.length(0);
        QDataSet bdesc= (QDataSet) ds.property(QDataSet.BUNDLE_1);
        String[] bundleLabels= new String[n];
        String[] bundleNames= new String[n];
        if ( bdesc!=null && bdesc.rank()==2 ) {
            for ( int i=0; i<n; i++ ) {
                bundleLabels[i]= (String) bdesc.property(QDataSet.LABEL, i);
                bundleNames[i]= (String)bdesc.property(QDataSet.NAME, i);
            }
        }
        QDataSet labels;
        if ( bdesc!=null && bdesc.rank()==1 ) {
            labels= bdesc;
        } else {
            labels= (QDataSet) ds.property(QDataSet.DEPEND_1);
        }
         
        String[] slabels = new String[n];
        if (labels == null) {
            for (int i = 0; i < n; i++) {
                if ( bundleLabels[i]!=null ) {
                    slabels[i]= bundleLabels[i];
                } else {
                    if ( bundleNames[i]!=null ) {
                        slabels[i]= bundleNames[i];
                    } else {
                        slabels[i]= "ch_"+i;
                    }
                }
            }
        } else {
            Units u = getUnits(labels);
            for (int i = 0; i < n; i++) {
                if ( bundleLabels[i]!=null ) {
                    slabels[i]= bundleLabels[i];
                } else {
                    if ( labels.rank()>1 ) {
                        slabels[i] = "ch_"+i;
                    } else {
                        slabels[i] = String.valueOf(u.createDatum(labels.value(i)));
                    }
                }
            }
        }
        return slabels;
    }

    /**
     * lookupUnits canonical units object, or allocate one.
     * Examples include:<ul>
     * <li>"nT" where it's already allocated,
     * <li>"apples" where it allocates a new one, and
     * <li>"seconds since 2011-12-21T00:00" where it uses lookupTimeUnits.
     * </ul>
     * @param sunits string identifier.
     * @return canonical units object.
     * @deprecated use Units.lookupUnits
     */
    public static synchronized Units lookupUnits(String sunits) {
        return Units.lookupUnits(sunits);
    }

    /**
     * return canonical das2 unit for colloquial time.
     * @param s
     * @throws java.text.ParseException
     * @deprecated use Units.lookupTimeLengthUnit
     * @return
     */
    public static Units lookupTimeLengthUnit(String s) throws ParseException {
        return Units.lookupTimeLengthUnit(s);
    }

    /**
     * lookupUnits canonical units object, or allocate one.  If one is
     * allocated, then parse for "&lt;unit&gt; since &lt;datum&gt;" and add conversion to
     * "microseconds since 2000-001T00:00" (us2000).  Note leap seconds are ignored
     * in the returned units, so each day is 86400 seconds long.
     * @param units string like "microseconds since 2000-001T00:00"
     * @return a units object that implements.
     * @throws java.text.ParseException
     * @deprecated use Units.lookupTimeUnits
     */
    public static synchronized Units lookupTimeUnits( String units ) throws ParseException {
        return Units.lookupTimeUnits(units);
    }
    
    /**
     * lookupUnits canonical units object, or allocate one.  If one is
     * allocated, then parse for "&lt;unit&gt; since &lt;datum&gt;" and add conversion to
     * "microseconds since 2000-001T00:00."  Note leap seconds are ignored!
     * @param base the base time, for example 2000-001T00:00.
     * @param offsetUnits the offset units for example microseconds.  Positive values of the units will be since the base time.
     * @return the unit.
     * @deprecated use Units.lookupTimeUnits
     */
    public static synchronized Units lookupTimeUnits( Datum base, Units offsetUnits ) {
        return Units.lookupTimeUnits(base,offsetUnits);
    }

    public static boolean isRank1Bundle(QDataSet ds) {
        if ( ds.rank()!=1 ) return false;
        if ( ds.property(QDataSet.BUNDLE_0)!=null ) {
            return true;
        } else {
            QDataSet dep= (QDataSet) ds.property(QDataSet.DEPEND_0);
            if ( dep==null ) return false;
            Units depu= getUnits(dep);
            return depu instanceof EnumerationUnits;
        }
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
                if ( isRank1Bundle(ds) ) {
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
     * Test for bundle scheme.  Returns true if the BUNDLE_1 is set.
     * @param ds
     * @return true if the dataset is a bundle
     */
    public static boolean isBundle(QDataSet ds) {
        return ds.rank()==2 && ds.property(QDataSet.BUNDLE_1)!=null && !isRank2Waveform(ds);
    }

    /**
     * Test for rank 2 waveform dataset, where DEPEND_1 is offset from DEPEND_0, 
     * and the data is the waveform.  Other rules include:<ul>
     * <li> DEPEND_1 must be at least 128 elements long.
     * <li> DEPEND_1 must not be dimensionless.
     * <li> if DEPEND_1 is in the same units as DEPEND_0, then DEPEND_0 can be ignored.
     * @param fillDs
     * @return
     */
    public static boolean isRank2Waveform(QDataSet fillDs ) {
        if ( fillDs.rank()==2 ) {
            QDataSet dep0= (QDataSet) fillDs.property(QDataSet.DEPEND_0);
            QDataSet dep1= (QDataSet) fillDs.property(QDataSet.DEPEND_1);
            if ( dep0!=null && dep1!=null &&
                ( ( dep1.rank()==1 && dep1.length()>=QDataSet.MIN_WAVEFORM_LENGTH )
                || ( dep1.rank()==2 && dep1.length(0)>=QDataSet.MIN_WAVEFORM_LENGTH ) ) ) {
                Units dep0units= SemanticOps.getUnits( dep0 );
                Units dep1units= SemanticOps.getUnits( dep1 );
                if ( dep0units!=Units.dimensionless && dep1units.isConvertibleTo( dep0units.getOffsetUnits() ) ) {
                    if ( dep0units!=dep1units ) {
                        return true;
                    } else {  
                        return  Units.seconds.isConvertibleTo(dep0units); // Only with time offsets is this allowed.  (http://cdaweb.gsfc.nasa.gov/data/polar/uvi/uvi_k0/2001/po_k0_uvi_20010106_v01.cdf?IMAGE_DATA, slice0.)
                    }
                } else if ( UnitsUtil.isTimeLocation(dep0units) && dep0units==dep1units ) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Test for rank 3 dataset that is a join of rank 2 waveform datasets.
     * @param ds
     * @return 
     */
    public static boolean isRank3JoinOfRank2Waveform( QDataSet ds ) {
        return ( ds.rank()==3 && isJoin(ds) && isRank2Waveform( ds.slice(0) ) );
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
     * Note: calls org.das2.qds.DataSetUtil.weightsDataSet.
     * @see org.das2.qds.ops.Ops#valid which is equivalent
     * @see #cadenceCheck which detects for gaps in cadence.
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
        QDataSet cadence= DataSetUtil.guessCadenceNew( ds, yds );
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
        Object odep0= ds.property(QDataSet.DEPEND_0);
        if ( odep0!=null ) {
            if ( odep0 instanceof String ) {
                logger.info("Somehow a string got into QDataSet DEPEND_0 property...");
                return null;
            }
        }
        QDataSet dep0= (QDataSet)odep0;
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
     * return the ytags for the simple table that is ds.<ul>
     *   <li>rank 2 spectrogram: Z[X,Y] &rarr; Y
     *   <li>bundle_1: ds[ :, [x,y,z] ] &rarr; y
     *   <li>[x,y,z] &rarr; y
     * </ul>
     * TODO: consider that these break duck typing goal, and really need a scheme
     *   to tell it how to get the dataset.
     * @param ds the dataset
     * @return the ytags
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
            if ( ds.rank()==2 && yds==ds.slice(0) ) {
                return Ops.indgen(yds.length());
            }
            JoinDataSet result= new JoinDataSet(yds);
            for ( int i=1; i<ds.length(); i++ ) {
                QDataSet yds1= ytagsDataSet(ds.slice(i) );
                result.join( yds1 );
            }
            return result;
        } else if ( ds.length()>0 
                && ds.property(QDataSet.DEPEND_1)==null
                && ds.rank()>1 && ds.property(QDataSet.DEPEND_0,0)!=null ) { // For Juno pktid=91
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
                    return new IndexGenDataSet(ds.length(0));  //TODO: this should support non-qubes with IndexGenDataSet.lastIndex
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
     * return the bounds DS[ JOIN_0=x,y; BINS_1=min,maxInclusive ] of the datasets
     * independent parameters.  This is only implemented for:
     * <table summary="">
     *   <tr><td>rank 2 Tables</td><td>extent(X),extent(Y) and Z is not represented</td></tr>
     *   <tr><td>rank 3 array of tables</td><td>extent(X),extent(Y) and Z is not represented</td></tr>
     *   <tr><td>rank 1 Y(X)</td><td>extent(X),extent(Y)</td></tr>
     *   <tr><td>not for rank 2 bundle dataset</td></tr>
     *   <tr><td>not for rank 1 bundle dataset</td></tr>
     * </table>
     * The zeroth dimension will be the physical dimension of the DEPEND_0 values.  Or said another way:
     * <table summary="">
     *   <tr><td>bounds[0,0]= X min</td><td>bounds[0,1] = X max</td><td>bounds.slice(0) is the extent of X<td></tr>
     *   <tr><td>bounds[1,0]= Y min</td><td>bounds[1,1] = Y max</td><td>bounds.slice(1) is the extent of Y<td></tr>
     * </table>
     * 
     * @param ds rank 2 dataset with BINS_1="min,maxInclusive"
     * @throws IllegalArgumentException when the dataset scheme is not supported
     * @return
     */
    public static QDataSet bounds( QDataSet ds ) {
        
        logger.entering( CLASSNAME, "bounds" );
        
        QDataSet result= (QDataSet) DataSetAnnotations.getInstance().getAnnotation( ds, DataSetAnnotations.ANNOTATION_BOUNDS );
        
        if ( result!=null ) {
            logger.exiting( CLASSNAME, "bounds" );
            return result;
        }
        
        QDataSet xrange;
        QDataSet yrange;

        if ( ds.rank()==2 ) {
            if ( ds.property(QDataSet.BUNDLE_1)!=null && ds.property(QDataSet.DEPEND_1)==null && ds.property(QDataSet.BINS_1)==null ) {
                xrange= Ops.extent( SemanticOps.xtagsDataSet(ds), null );
                yrange= Ops.extent( SemanticOps.ytagsDataSet(ds), null );
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
        
        DataSetAnnotations.getInstance().putAnnotation( ds, DataSetAnnotations.ANNOTATION_BOUNDS, jds );
        
        logger.exiting( CLASSNAME, "bounds" );
        
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
         if ( ds.rank()==2 ) {
             if ( dep1!=null ) { // TODO: we should check for EnumerationUnits here...
                 return true; // Y tags can be rank2, making it a table dataset but not a simple one.
             } else {
                 QDataSet bds= (QDataSet) ds.property(QDataSet.BUNDLE_1);
                 return bds == null;
             }
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
        if ( dep1!=null && dep1.rank()!=1 ) return false;
        return ds.rank()==2 && ( ( dep1!=null && dep1.rank()==1 ) || !Ops.isBundle(ds) ) && !Ops.isLegacyBundle(ds);
    }

    /**
     * returns true if the dataset is a bundle of rank 1 datasets.  If no
     * dependence is declared, it is assumed that the first one or two datasets
     * are the independent datasets, and the last is the dependent. 
     *<blockquote><pre>
     *    X,Y   -->  Y(X)
     *    X,Y,Z -->  Z(X,Y)
     *</pre></blockquote>

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
        return value.doubleValue();
    }

    /**
     * returns the value as a datum.  Note this should be used with reservation,
     * this is not very efficient when the operation is done many times.
     * @param ds dataset providing the UNITS and VALID_MIN, VALID_MAX and FILL_VALUE.
     * @param d the double.
     * @return a datum representing the value.
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
     * return the parts of the dataset within the bounds.  This assumes how
     * the data is visualized, so for example see SemanticOps.xtagsDataSet 
     * for which dimensions correspond to x and y.
     * @param ds the rank 1 or more dataset, including joins.
     * @param xrange the range or null if no trimming should be done
     * @param yrange the range or null if no trimming should be done
     * @return the trimmed dataset.
     */
    public static QDataSet trim( QDataSet ds, DatumRange xrange, DatumRange yrange ) {
        Logger llogger= LoggerManager.getLogger("qdataset.ops.trim");
        llogger.entering("SemanticOps", "trim");
        int rank=ds.rank();
        if ( ds.rank()==0 ) {
            llogger.exiting("SemanticOps", "trim");
            return ds;
        }
        if ( xrange==null && yrange==null ) {
            llogger.exiting("SemanticOps", "trim");
            return ds;
        }
        if ( rank==3 ) {
            if ( isJoin(ds) ) {
                JoinDataSet jds= new JoinDataSet(ds.rank());
                for ( int i=0; i<ds.length(); i++ ) {
                    QDataSet t1= trim( ds.slice(i), xrange, yrange );
                    if ( t1.length()>0 ) {
                        jds.join( t1 );
                    }
                }
                DataSetUtil.putProperties( DataSetUtil.getProperties(ds), jds );
                llogger.exiting("SemanticOps", "trim");
                return jds;
            } else if ( Schemes.isCompositeImage(ds) ) {
                int[] size= Ops.size(ds);
                TailBundleDataSet bds= new TailBundleDataSet(3);
                for ( int i=0; i<size[2]; i++ ) {
                    QDataSet ch= Ops.slice2(ds,i);
                    ch= trim( ch, xrange, yrange );
                    bds.bundle(ch);
                }
                llogger.exiting("SemanticOps", "trim");
                return bds;
            } else {
                throw new IllegalArgumentException("not supported: "+ds);
            }
        } else if ( rank==2 ) {
            if ( isRank2Waveform(ds) ) {
                QDataSet xds= SemanticOps.xtagsDataSet(ds);
                QDataSet yds= SemanticOps.xtagsDataSet(ds.slice(0));
                QDataSet ydsMax= DataSetUtil.asDataSet( yds.value( yds.length()-1 ), SemanticOps.getUnits(yds) );
                QDataSet xinside= xrange==null ? null :
                    Ops.and( Ops.ge( Ops.add( xds, ydsMax ), DataSetUtil.asDataSet(xrange.min()) ), Ops.le(  xds, DataSetUtil.asDataSet(xrange.max()) ) );
                SubsetDataSet sds= new SubsetDataSet(ds);
                if ( xinside!=null ) sds.applyIndex( 0, Ops.where(xinside) );  //TODO: consider the use of trim which would be more efficient.
                llogger.exiting("SemanticOps", "trim");
                return sds;
                
            } else if ( isSimpleTableDataSet(ds) ) {
                QDataSet xds= SemanticOps.xtagsDataSet(ds);
                QDataSet yds= SemanticOps.xtagsDataSet(ds.slice(0));
                QDataSet xinside= xrange==null ? null :
                    Ops.and( Ops.ge( xds, DataSetUtil.asDataSet(xrange.min()) ), Ops.le(  xds, DataSetUtil.asDataSet(xrange.max()) ) );
                QDataSet yinside= yrange==null ? null :
                    Ops.and( Ops.ge( yds, DataSetUtil.asDataSet(yrange.min()) ), Ops.le(  yds, DataSetUtil.asDataSet(yrange.max()) ) );
                SubsetDataSet sds= new SubsetDataSet(ds);
                if ( xinside!=null ) sds.applyIndex( 0, Ops.where(xinside) );  //TODO: consider the use of trim which would be more efficient.
                if ( yinside!=null ) sds.applyIndex( 1, Ops.where(yinside) );
                llogger.exiting("SemanticOps", "trim");
                return sds;
                
            } else if ( isBundle(ds) ) { 
                QDataSet xds= SemanticOps.xtagsDataSet(ds);
                QDataSet yds= SemanticOps.ytagsDataSet(ds);
                
                QDataSet xinside= xrange==null ? null :
                    Ops.and( Ops.ge( xds, DataSetUtil.asDataSet(xrange.min()) ), Ops.le(  xds, DataSetUtil.asDataSet(xrange.max()) ) );
                QDataSet yinside= yrange==null ? null :
                    Ops.and( Ops.ge( yds, DataSetUtil.asDataSet(yrange.min()) ), Ops.le(  yds, DataSetUtil.asDataSet(yrange.max()) ) );
                QDataSet ok;
                SubsetDataSet sds= new SubsetDataSet(ds);
                if ( xrange==null && yrange==null ) {
                    return ds;
                } else if ( xrange==null ) {
                    //ok= Ops.where( yinside );
                    //sds.applyIndex( 1, ok );
                    llogger.exiting("SemanticOps", "trim");
                    return ds; // this is because we can't easily search the ytags.
                } else if ( yrange==null ) {
                    ok= Ops.where( xinside );
                    sds.applyIndex( 0, ok );
                } else {
                    llogger.fine( "yds is being ignored, not sure why...");
                    ok= Ops.where( Ops.and( xinside, yinside ) );
                    sds.applyIndex( 0, ok );
                }
                llogger.exiting("SemanticOps", "trim");
                return sds;
                
            } else { // copy over elements where
                QDataSet xds= SemanticOps.xtagsDataSet(ds);
                QDataSet yds= SemanticOps.getDependentDataSet(ds);
                QDataSet xinside= xrange==null ? null :
                    Ops.and( Ops.ge( xds, DataSetUtil.asDataSet(xrange.min()) ), Ops.le(  xds, DataSetUtil.asDataSet(xrange.max()) ) );
                //QDataSet yinside= null; //yrange==null ? null :
                    //Ops.and( Ops.ge( yds, DataSetUtil.asDataSet(yrange.min()) ), Ops.le(  yds, DataSetUtil.asDataSet(yrange.max()) ) );
                QDataSet ok;
                SubsetDataSet sds= new SubsetDataSet(ds);
                if ( xrange==null && yrange==null ) {
                    llogger.exiting("SemanticOps", "trim");
                    return ds;
                } else if ( xrange==null ) {
                    //ok= Ops.where( yinside );
                    //sds.applyIndex( 1, ok );
                    llogger.exiting("SemanticOps", "trim");
                    return ds; // this is because we can't easily search the ytags.
                } else if ( yrange==null ) {
                    ok= Ops.where( xinside );
                    sds.applyIndex( 0, ok );
                } else {
                    llogger.fine( "yds is being ignored, not sure why...");
                    //ok= Ops.where( Ops.and( xinside, yinside ) );
                    ok= Ops.where( xinside );
                    sds.applyIndex( 0, ok );
                }
                llogger.exiting("SemanticOps", "trim");
                return sds;
                
            }
        } else if ( rank==1 ) { 
            QDataSet xds= SemanticOps.xtagsDataSet(ds);
            QDataSet yds= SemanticOps.getDependentDataSet(ds);
            QDataSet xinside= null;
            if ( DataSetUtil.isMonotonic(xds) && xds.property(QDataSet.FILL_VALUE)==null ) {  // TODO: validmin validmax...
                if ( xrange!=null ) {
                    int i= DataSetUtil.xTagBinarySearch( xds, xrange.min(), 0, xds.length()-1 );
                    if ( i<0 ) {
                        i= -1 * ( i + 1 );
                    }
                    int j= DataSetUtil.xTagBinarySearch( xds, xrange.max(), i, xds.length()-1 );
                    if ( j<0 ) {
                        j= -1 * ( j + 1 );
                    }
                    if ( yrange==null ) {
                        llogger.exiting("SemanticOps", "trim");
                        return ds.trim(i,j);  // optimization for waveforms...
                        
                    } else {
                        int[] back= new int[ xds.length() ];
                        if ( j==xds.length() ) j= xds.length()-1; // bugfix: if xrange.max is gt last point.
                        for ( int ii=i; ii<=j; ii++ ) {
                            back[ii]= 1;
                        }
                        xinside= IDataSet.wrap(back);
                    }
                }
            } else {
                if ( xds.rank()==2 && SemanticOps.isBins(xds) ) {
                    QDataSet xmin= Ops.slice1( xds, 0 );
                    QDataSet xmax= Ops.slice1( xds, 1 );
                    xinside= xrange==null ? null :
                        Ops.and( Ops.ge( xmax, DataSetUtil.asDataSet(xrange.min()) ), Ops.le(  xmin, DataSetUtil.asDataSet(xrange.max()) ) );                
                } else {
                    xinside= xrange==null ? null :
                        Ops.and( Ops.ge( xds, DataSetUtil.asDataSet(xrange.min()) ), Ops.le(  xds, DataSetUtil.asDataSet(xrange.max()) ) );
                }
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
            llogger.exiting("SemanticOps", "trim");
            return sds;
        } else {
            throw new IllegalArgumentException("not supported: "+ds);
        }
    }

    /**
     * YXT is a dataset where T &rarr; X &rarr; Y.  This pattern comes up all the time
     * and we need to identify it.  Ideally the data would be represented as T &rarr (X;Y).
     * @param ds
     * @return 
     */
    public static boolean isYXT(QDataSet ds) {
        if ( ds.rank()!=1 ) {
            return false;
        }
        QDataSet x= (QDataSet)ds.property(QDataSet.DEPEND_0);
        if ( x==null || x.rank()!=1 ) {
            return false;
        }
        QDataSet t= (QDataSet)x.property(QDataSet.DEPEND_0);
        if ( t==null || t.rank()!=1 ) {
            return false;
        }
        return true;
    }
    
    /**
     * YXT is a dataset where T &rarr; X &rarr; Y.  This pattern comes up all the time
     * and we need to identify it.  Ideally the data would be represented as T &rarr (X;Y).
     * @return a dataset with this form.
     */
    public static QDataSet yxt() {
        QDataSet t0= Ops.dataset( "2023-10-18T00:00" );
        QDataSet t= Ops.linspace( t0, "2023-10-19T00:00",1441 );
        QDataSet a= Ops.multiply( Ops.divide( Ops.subtract( t, t0 ), "18hr" ), 2*Math.PI );
        QDataSet y= Ops.multiply( Ops.sin( a ), Ops.linspace( 1.0, 1.2, 1441 ) );
        QDataSet x= Ops.multiply( Ops.cos( Ops.multiply(a,1.01) ), Ops.linspace( 1.0, 1.2, 1441 ) );
        return Ops.link( Ops.link( t, x ), y );
    }
    
    /**
     * return a dataset with 1's where the cadence preceeding this measurement is acceptable, and 0's where
     * there should be a break in the data.  For example, here's some pseudocode:
     *<blockquote><pre>
     *   findex= Ops.interpolate( xds, x )
     *   cadenceCheck= cadenceCheck(xds)
     *   r= where( cadenceCheck[floor(findex)] eq 0 )
     *   x[r]= fill
     *</pre></blockquote>
     * Presently this just uses guessXTagWidth to get the cadence, but this may allow a future version to support
     * mode changes.
     *
     * The result is a dataset with the same length, and the zeroth element is always 1.
     *
     * @see Ops#valid which checks for fill and valid_min, valid_max.
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
        result= ArrayDataSet.append( one, aresult );

        result= Ops.link( tds, result );

        return result;
        
    }
    
    private static final Map<String,Class> propertyTypes= new HashMap<>();
    static {
        propertyTypes.put( QDataSet.UNITS, Units.class );
        propertyTypes.put( QDataSet.TYPICAL_MIN, Number.class );
        propertyTypes.put( QDataSet.TYPICAL_MAX, Number.class );
        propertyTypes.put( QDataSet.VALID_MIN, Number.class );
        propertyTypes.put( QDataSet.VALID_MAX, Number.class );
        propertyTypes.put( QDataSet.FILL_VALUE, Number.class );
        propertyTypes.put( QDataSet.ELEMENT_DIMENSIONS, int[].class ); // this will probably cause grief, Integer[] vs int[]
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
        propertyTypes.put( QDataSet.QUBE, Boolean.class );
    }
    
    /**
     * verify property types.  For example, that UNITS is a org.das2.datum.Units, etc.
     * Returns true for unrecognized property names (future expansion) and null.  If 
     * throwException is true, then an IllegalArgumentException is thrown.
     * @param prop the property name, e.g. QDataSet.CADENCE
     * @param value the candidate value for the property.
     * @param throwException if true, throw descriptive exception instead of returning false.
     * @return true if the property type is okay.
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
    
    /**
     * return the property type expected for the property.
     * @param prop
     * @return QDataSet, String, Number, Units, or CacheTag
     */
    public static String getPropertyType( String prop ) {
        Class c= propertyTypes.get(prop);
        if ( c==null ) {
            throw new IllegalArgumentException("Property not recognized: "+prop);
        } else {
            if ( c.isAssignableFrom( CacheTag.class ) ) {
                return "CacheTag";
            } else if ( c.isAssignableFrom( Number.class ) ) {
                return "Number";
            } else if ( c.isAssignableFrom( Boolean.class ) ) {
                return "Boolean";
            } else if ( c.isAssignableFrom( QDataSet.class ) ) {
                return "QDataSet";
            } else if ( c.isAssignableFrom( Units.class ) ) {
                return "Units";
            } else {
                throw new IllegalArgumentException("Property not supported: "+prop);
            }
        }
    }

}
