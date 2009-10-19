/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dataset;

import java.text.ParseException;
import org.das2.datum.Basis;
import org.das2.datum.Datum;
import org.das2.datum.NumberUnits;
import org.das2.datum.TimeLocationUnits;
import org.das2.datum.TimeUtil;
import org.das2.datum.Units;
import org.das2.datum.UnitsConverter.ScaleOffset;

/**
 * Common expressions that apply semantics to QDataSet.  Introduced
 * to reduce a lot of repeated code, but also to make it clear where semantics
 * are being applied.
 * @author jbf
 */
public class SemanticOps {

    public final static Units getUnits(QDataSet ds) {
        Units u = (Units) ds.property(QDataSet.UNITS);
        return u == null ? Units.dimensionless : u;
    }

    /**
     * return the labels for a dataset where DEPEND_1 is a bundle dimension.
     * @param ds
     * @return
     */
    public final static String[] getComponentLabels(QDataSet ds) {
        int n = ds.length(0);
        QDataSet labels = (QDataSet) ds.property(QDataSet.DEPEND_1);
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
        try {
            result= Units.getByName(sunits);
        } catch ( IllegalArgumentException ex ) {
            if ( sunits.equals("sec") ) {   // begin, giant table of kludges
                result= Units.seconds;
            } else if ( sunits.equals("msec") ) {  // CDF
                result= Units.milliseconds;
            } else {
                result= new NumberUnits( sunits );
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
        try {
            result= Units.getByName(units);
            return result;
        } catch ( IllegalArgumentException ex ) {
            String[] ss= units.split("since");            
            Units offsetUnits= lookupTimeLengthUnit(ss[0]);
            Datum datum;

            if ( ss[1].equals(" 1-1-1 00:00:00" ) ) { // make this into something that won't crash.
                //datum= Units.mj1958.createDatum(-714779);
                ss[1]= "1901-01-01 00:00:00"; // /media/mini/data.backup/examples/netcdf/sst.ltm.1961-1990.nc
            }
            datum= TimeUtil.create(ss[1]);
            String canonicalName = "" + offsetUnits + " since "+ datum;
            Basis basis= new Basis( "since "+ datum, "since "+ datum, Basis.since2000, datum.doubleValue(Units.us2000), Units.us2000.getOffsetUnits() );
            result= new TimeLocationUnits( canonicalName, canonicalName, offsetUnits, basis );
            result.registerConverter( Units.us2000,
                    new ScaleOffset(
                    offsetUnits.convertDoubleTo(Units.microseconds, 1.0),
                    datum.doubleValue(Units.us2000) ) );
            return result;
        }
    }
}
