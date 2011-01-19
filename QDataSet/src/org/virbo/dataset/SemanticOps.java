/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dataset;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.das2.datum.Basis;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.NumberUnits;
import org.das2.datum.TimeLocationUnits;
import org.das2.datum.TimeUtil;
import org.das2.datum.Units;
import org.das2.datum.UnitsConverter;
import org.das2.datum.UnitsConverter.ScaleOffset;
import org.virbo.dsops.Ops;

/**
 * Common expressions that apply semantics to QDataSet.  Introduced
 * to reduce a lot of repeated code, but also to make it clear where semantics
 * are being applied.
 * @author jbf
 */
public class SemanticOps {

    /**
     * returns the units found in the UNITS property of the dataset,
     * or Units.dimensionless if it is not found.
     * @param ds
     * @return
     */
    public final static Units getUnits(QDataSet ds) {
        Units u = (Units) ds.property(QDataSet.UNITS);
        return u == null ? Units.dimensionless : u;
    }

    public final static UnitsConverter getUnitsConverter( QDataSet src, QDataSet dst ) {
        Units usrc= getUnits(src);
        Units udst= getUnits(dst);
        return usrc.getConverter(udst);
    }

    /**
     * return the labels for a dataset where DEPEND_1 is a bundle dimension.
     * Look for the BUNDLE_1.
     * @param ds
     * @return
     */
    public final static String[] getComponentLabels(QDataSet ds) {
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
     * lookupUnits canonical units object, or allocate one.
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
        return result;
    }

    /**
     * return canonical das2 unit for colloquial time.
     * @param string
     * @return
     */
    public static Units lookupTimeLengthUnit(String s) throws ParseException {
        s= s.toLowerCase().trim();
        if ( s.startsWith("sec") ) {
            return Units.seconds;
        } else if ( s.startsWith("ms") || s.startsWith("millisec") ) {
            return Units.milliseconds;
        } else if ( s.equals("hr") || s.startsWith("hour") ) {
            return Units.hours;
        } else if ( s.equals("mn") || s.startsWith("min") ) {
            return Units.minutes;
        } else if ( s.startsWith("us") || s.startsWith("\u00B5s" ) || s.startsWith("micros")) {
            return Units.microseconds;
        } else if ( s.startsWith("ns") || s.startsWith("nanos" ) ) {
            return Units.nanoseconds;
        } else if ( s.startsWith("d") ) {
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

    /**
     * Test for bundle scheme.  Returns true if the BUNDLE_1 is set.
     * @param ds
     * @return true if the dataset is a bundle
     */
    public static boolean isBundle(QDataSet ds) {
        return ds.rank()==2 && ds.property(QDataSet.BUNDLE_1)!=null;
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

    public static QDataSet weightsDataSet( QDataSet ds ) {
        return DataSetUtil.weightsDataSet(ds);
    }

    public static Datum guessXTagWidth( QDataSet ds, QDataSet yds ) {
        RankZeroDataSet cadence= DataSetUtil.guessCadenceNew( ds, yds );
        return cadence==null ? null : DataSetUtil.asDatum( cadence );
    }

    /**
     * return the dataset containing the x tags for the dataset.  This
     * is QDataSet.DEPEND_0, or if that's null then IndexGenDataSet(ds.length).
     * For a bundle, this is just the 0th dataset.
     * @param ds
     * @return
     */
    public static QDataSet xtagsDataSet( QDataSet ds ) {
        if ( isBundle(ds) ){
            return DataSetOps.unbundle(ds,0);
        } else {
            QDataSet result= (QDataSet) ds.property(QDataSet.DEPEND_0);
            if ( result==null ) {
                return new IndexGenDataSet(ds.length());
            } else {
                return result;
            }
        }
    }

    /**
     * return the ytags for the simple table that is ds.
     * @param ds
     * @return
     */
    public static QDataSet ytagsDataSet( QDataSet ds ) {
        if ( ds.rank()>2 ) throw new IllegalArgumentException("need to slice to get a table");
        QDataSet result= (QDataSet) ds.property(QDataSet.DEPEND_1);
        if ( result==null ) {
            return new IndexGenDataSet(ds.length(0));
        } else {
            return result;
        }
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
     * independent parameters.  This is only implemented for Tables!
     * @param ds
     * @return
     */
    public static QDataSet bounds( QDataSet ds ) {
        QDataSet xrange;
        QDataSet yrange;

        if ( ds.rank()==2 ) {
            xrange= Ops.extent( SemanticOps.xtagsDataSet(ds), null );
            yrange= Ops.extent( SemanticOps.ytagsDataSet(ds), null );
        } else if ( ds.rank()==3 ) {
            QDataSet ds1= ds.slice(0);
            xrange= Ops.extent( SemanticOps.xtagsDataSet(ds1), null );
            yrange= Ops.extent( SemanticOps.ytagsDataSet(ds1), null );
            for ( int i=1; i<ds.length(); i++ ) {
                ds1= ds.slice(i);
                xrange= Ops.extent( SemanticOps.xtagsDataSet(ds1), xrange );
                yrange= Ops.extent( SemanticOps.ytagsDataSet(ds1), yrange );
            }
        } else {
            throw new IllegalArgumentException("bad rank");
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
         return ds.rank()==3 || isSimpleTableDataSet(ds);
    }

    /**
     * returns true if the dataset is the scheme of a legacy TableDataSet
     * with only one table.  Note "Tables" have just one X unit and one Y unit,
     * no bundles.
     * @param ds
     * @return
     */
    public static boolean isSimpleTableDataSet(QDataSet ds) {
         return ds.rank()==2 && !Ops.isBundle(ds)  && !Ops.isLegacyBundle(ds);
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
     * returns the value as a datum.  Note this should be used with reservation,
     * this is not very efficient when the operation is done many times.
     * @param ds
     * @param d
     * @return
     */
    public static Datum getDatum( QDataSet ds, double d ) {
        Units u = SemanticOps.getUnits(ds);
        Double vmin= (Double)ds.property( QDataSet.VALID_MIN );
        Double vmax= (Double)ds.property( QDataSet.VALID_MAX );
        Double fill= (Double)ds.property( QDataSet.FILL_VALUE );
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
            if ( SemanticOps.isBundle(ds) ) { // copy over elements where
                QDataSet xds= SemanticOps.xtagsDataSet(ds);
                QDataSet yds= SemanticOps.getDependentDataSet(ds);
                QDataSet xinside= xrange==null ? null :
                    Ops.and( Ops.ge( xds, DataSetUtil.asDataSet(xrange.min()) ), Ops.le(  xds, DataSetUtil.asDataSet(xrange.max()) ) );
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
                sds.applyIndex( 1, ok );
                return sds;
                
            } else { // simple table
                QDataSet xds= SemanticOps.xtagsDataSet(ds);
                QDataSet yds= SemanticOps.ytagsDataSet(ds);
                QDataSet xinside= xrange==null ? null :
                    Ops.and( Ops.ge( xds, DataSetUtil.asDataSet(xrange.min()) ), Ops.le(  xds, DataSetUtil.asDataSet(xrange.max()) ) );
                QDataSet yinside= yrange==null ? null :
                    Ops.and( Ops.ge( yds, DataSetUtil.asDataSet(yrange.min()) ), Ops.le(  yds, DataSetUtil.asDataSet(yrange.max()) ) );
                SubsetDataSet sds= new SubsetDataSet(ds);
                sds.applyIndex( 0, Ops.where(xinside) );
                sds.applyIndex( 1, Ops.where(yinside) );
                return sds;
            }
        } else if ( rank==1 ) { 
            QDataSet xds= SemanticOps.xtagsDataSet(ds);
            QDataSet yds= SemanticOps.getDependentDataSet(ds);
            QDataSet xinside= xrange==null ? null :
                Ops.and( Ops.ge( xds, DataSetUtil.asDataSet(xrange.min()) ), Ops.le(  xds, DataSetUtil.asDataSet(xrange.max()) ) );
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

}
