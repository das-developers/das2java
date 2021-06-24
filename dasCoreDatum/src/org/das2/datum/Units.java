/* File: Units.java
 * Copyright (C) 2002-2003 The University of Iowa
 * Created by: Jeremy Faden <jbf@space.physics.uiowa.edu>
 *             Jessica Swanner <jessica@space.physics.uiowa.edu>
 *             Edward E. West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.das2.datum;

import java.text.Normalizer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.das2.datum.format.DatumFormatterFactory;

/**
 * Class for indicating physical units, and other random units.
 * @author  jbf
 */
public abstract class Units {

    private static final Logger logger= Logger.getLogger("datum.units");

    private static Map unitsMap = new HashMap();
    
    public static final Units dimensionless= new NumberUnits("","dimensionless quantities");

    public static final Units radians= new NumberUnits("radian");
    public static final Units degrees= new NumberUnits("degrees");
    public static final Units deg= new NumberUnits("deg");
    public static final Units degrees2= new NumberUnits("Degrees");
    static {
        degrees.registerConverter(radians, new UnitsConverter.ScaleOffset(Math.PI/180.0,0.0) );
        degrees.registerConverter(deg, UnitsConverter.IDENTITY);
        degrees.registerConverter(degrees2, UnitsConverter.IDENTITY);
    }

    /**
     * 
     */
    public static final Units rgbColor= new NumberUnits("rgbColor","256*256*red+256*green+blue");

    /**
     * return the preferred unit to use when there are multiple representations
     * of the same unit (having conversion UnitsConverter.IDENTITY.
     * @param units
     * @return the preferred unit
     */
    public static Units getCanonicalUnit(Units units) {
        String s= units.getId();
        switch ( s ) {
            case "msec": return Units.milliseconds;
            case "Degrees": return Units.degrees;
            case "deg": return Units.degrees;
            case "sec": return Units.seconds;
            case "nanoseconds": return Units.ns;
            case "\u00B5s": return Units.microseconds;
            case "\u03BCs": return Units.microseconds;
            case "ev": return Units.eV;
        }
        return units;
    }
    
    /**
     * this is left in in case legacy code needs to see the conversion from dB to dimensionless offset.
     */
    private static final class dBConverter extends UnitsConverter {
        @Override
        public double convert(double value) {
            return 10 * Math.log10(value);
        }
        @Override
        public UnitsConverter getInverse() {
            if (inverse == null) {
                inverse = new UnitsConverter() {
                    @Override
                    public double convert(double value) {
                        return Math.pow(10.0, value / 10.0);
                    }
                    @Override
                    public UnitsConverter getInverse() {
                        return dBConverter.this;
                    }
                };
            }
            return inverse;
        }
    }
    
    public static final Units celciusDegrees= new NumberUnits("celcius degrees"); // disambiguate from "deg C" which is the temperature scale
    public static final Units fahrenheitDegrees= new NumberUnits("fahrenheit degrees"); // disambiguate from "deg F" which is the temperature scale
    
    public static final Units years= new NumberUnits("years");
    public static final Units days= new NumberUnits("days");
    public static final Units hours= new NumberUnits("hr");
    public static final Units minutes= new NumberUnits("min");
    public static final Units seconds= new NumberUnits("s");
    public static final Units seconds2= new NumberUnits("sec");
    //public static final Units seconds3= new NumberUnits("seconds");  // note s was not convertible to seconds.
    public static final Units milliseconds= new NumberUnits("ms","milliseconds");
    public static final Units milliseconds2= new NumberUnits("msec");
    public static final Units microseconds= new NumberUnits("microseconds");
    public static final Units microseconds2= new NumberUnits("\u00B5s"); // note this is not the normalized micro.
    public static final Units microseconds3= new NumberUnits("\u03BCs");
    
    public static final Units nanoseconds= new NumberUnits("nanoseconds");
    public static final Units ns= new NumberUnits("ns","nanoseconds");
    public static final Units picoseconds= new NumberUnits("picoseconds");
    static {
        seconds.registerConverter(milliseconds, UnitsConverter.MILLI);
        seconds.registerConverter(microseconds, UnitsConverter.MICRO);
        seconds.registerConverter(nanoseconds,UnitsConverter.NANO);
        seconds.registerConverter(ns,UnitsConverter.NANO);
        nanoseconds.registerConverter( ns, UnitsConverter.IDENTITY );
        seconds.registerConverter(picoseconds,UnitsConverter.PICO);
        seconds.registerConverter(seconds2,UnitsConverter.IDENTITY);
        microseconds.registerConverter(nanoseconds, UnitsConverter.MILLI); // to support time formatting, often from us2000 to microseconds offset.
        microseconds.registerConverter(microseconds2, UnitsConverter.IDENTITY);
        microseconds.registerConverter(microseconds3, UnitsConverter.IDENTITY);
        milliseconds.registerConverter(milliseconds2, UnitsConverter.IDENTITY);
        hours.registerConverter(seconds, new UnitsConverter.ScaleOffset( 3600.,0.0));
        minutes.registerConverter(seconds, new UnitsConverter.ScaleOffset( 60.,0.0));
        days.registerConverter(seconds, new UnitsConverter.ScaleOffset(8.64e4, 0.0));
    }
    
    public static final Units bytesPerSecond= new NumberUnits("bytes/s");
    public static final Units kiloBytesPerSecond= new NumberUnits("KBytes/s");
    public static final Units bytes= new NumberUnits( "bytes" );
    public static final Units kiloBytes= new NumberUnits( "KBytes" );
    static {
        bytesPerSecond.registerConverter( kiloBytesPerSecond, UnitsConverter.KILO );
        bytes.registerConverter( kiloBytes, UnitsConverter.KILO );
    }
    
    public static final Units hertz= new NumberUnits("Hz");
    public static final Units kiloHertz = new NumberUnits("kHz"); // I verified that this should be lower case k.  I wonder why...
    public static final Units megaHertz = new NumberUnits("MHz");
    public static final Units gigaHertz = new NumberUnits("GHz");
    static {
        hertz.registerConverter(kiloHertz, UnitsConverter.KILO);
        hertz.registerConverter(megaHertz, UnitsConverter.MEGA);
        hertz.registerConverter(gigaHertz, UnitsConverter.GIGA);
    }
    
    public static final Units eV= new NumberUnits("eV");
    public static final Units ev= new NumberUnits("ev"); // Mike at LANL had run into these...
    public static final Units keV= new NumberUnits("keV");
    public static final Units MeV= new NumberUnits("MeV");
    static {
        eV.registerConverter(Units.ev, UnitsConverter.IDENTITY);
        eV.registerConverter(Units.keV, UnitsConverter.KILO);
        eV.registerConverter(Units.MeV, UnitsConverter.MEGA);
    }
    
    /**
     * 1 / cm<sup>3</sup>
     */
    public static final Units pcm3= new NumberUnits("cm!a-3!n");    
    
    public static final Units kelvin= new NumberUnits("K");

    public static final Units cm_2s_1keV_1= new NumberUnits( "cm!U-2!N s!U-1!N keV!U-1!N" );
    public static final Units cm_2s_1MeV_1= new NumberUnits( "cm!U-2!N s!U-1!N MeV!U-1!N" );
    static {
        cm_2s_1keV_1.registerConverter( Units.cm_2s_1MeV_1, UnitsConverter.KILO );
    }
    /**
     * Volts <sup>2</sup> m<sup>-2</sup> Hz<sup>-1</sup>
     */
    public static final Units v2pm2Hz= new NumberUnits("V!a2!nm!a-2!nHz!a-1");
    
    /**
     * Watts / m<sup>2</sup>
     */
    public static final Units wpm2= new NumberUnits("W/m!a-2!n");

    
    public static final Units meters = new NumberUnits("m");
    public static final Units millimeters = new NumberUnits("mm");
    public static final Units centimeters = new NumberUnits("cm");
    public static final Units kiloMeters = new NumberUnits("km");
    public static final Units inches = new NumberUnits("inch");
    public static final Units typographicPoints = new NumberUnits("points");
    static {
        meters.registerConverter(kiloMeters, UnitsConverter.KILO);
        meters.registerConverter(centimeters, UnitsConverter.CENTI );
        meters.registerConverter(millimeters, UnitsConverter.MILLI );
        inches.registerConverter( meters, new UnitsConverter.ScaleOffset(0.0254,0.0) );
        inches.registerConverter( typographicPoints, new UnitsConverter.ScaleOffset(72,0.0) );
    }

    public static final Units nT= new NumberUnits("nT","nanoTesla");
    public static final Units cmps= new NumberUnits("cm/s");
    public static final Units mps= new NumberUnits("m s!a-1!n","meters per second");
    static {
        mps.registerConverter( cmps, UnitsConverter.CENTI );
    }
    
    /**** begin of LocationUnits.  These must be defined after the physical units to support Basis. ****/
    
    public static final Units centigrade= new LocationUnits( "centigrade", "centigrade", Units.celciusDegrees, Basis.centigrade );
    public static final Units fahrenheitScale= new LocationUnits("deg F", "deg F", Units.fahrenheitDegrees, Basis.fahrenheit );

    static {
        centigrade.registerConverter(fahrenheitScale, new UnitsConverter.ScaleOffset(1.8, 32));
        celciusDegrees.registerConverter(fahrenheitDegrees, new UnitsConverter.ScaleOffset(1.8,0) );
    }

    /**
     * currencies for demonstration purposes.
     */
    public static final Units dollars= new CurrencyUnits("dollars","$","United States Dollars");
    public static final Units euros= new CurrencyUnits("euros","\u20AC", "Euro Dollars");
    public static final Units yen= new CurrencyUnits("yen","\uFFE5","Japanese Yen");
    public static final Units rupee= new CurrencyUnits("rupee","\u20B9", "Indian Rupee");
    
    /**
     * Microseconds since midnight Jan 1, 2000, excluding those within a leap second.  Differences across leap
     * second boundaries do not represent the number of microseconds elapsed.
     */
    public static final TimeLocationUnits us2000= new TimeLocationUnits("us2000", "Microseconds since midnight Jan 1, 2000.",
            Units.microseconds, Basis.since2000);

    /**
     * Microseconds since midnight Jan 1, 1980, excluding those within a leap second.
     */
    public static final TimeLocationUnits us1980= new TimeLocationUnits("us1980", "Microseconds since midnight Jan 1, 1980.",
            Units.microseconds, Basis.since1980 );

    /**
     * Seconds since midnight Jan 1, 2010, excluding leap seconds.
     */
    public static final TimeLocationUnits t2010= new TimeLocationUnits("t2010","Seconds since midnight Jan 1, 2010.",
            Units.seconds, Basis.since2010 );

    /**
     * Seconds since midnight Jan 1, 2000, excluding leap seconds.
     */    
    public static final TimeLocationUnits t2000= new TimeLocationUnits("t2000","Seconds since midnight Jan 1, 2000.",
            Units.seconds, Basis.since2000 );

    /**
     * seconds since midnight Jan 1, 1970, excluding leap seconds.
     */
    public static final TimeLocationUnits t1970= new TimeLocationUnits("t1970","Seconds since midnight Jan 1, 1970",
            Units.seconds, Basis.since1970 );
    
    /**
     * milliseconds since midnight Jan 1, 1970, excluding leap seconds.
     */
    public static final TimeLocationUnits ms1970= new TimeLocationUnits("ms1970","Milliseconds since midnight Jan 1, 1970",
            Units.milliseconds, Basis.since1970 );

    /**
     * milliseconds since midnight Jan 1, 1970, excluding leap seconds.
     */
    public static final TimeLocationUnits us1970= new TimeLocationUnits("us1970","Microseconds since midnight Jan 1, 1970",
            Units.microseconds, Basis.since1970 );

    /**
     * roughly days since on midnight on 1958-01-01, Julian - 2436204.5 to be more precise.
     */
    public static final TimeLocationUnits mj1958= new TimeLocationUnits("mj1958","days since 1958-01-01T00:00Z, or Julian - 2436204.5", 
            Units.days, Basis.since1958 );
    
    /**
     * The Modified Julian Day (MJD) is the number of days (with decimal fraction of the day) that have elapsed since midnight at the beginning of Wednesday November 17, 1858. 
     * Julian - 2400000.5
     */
    public static final TimeLocationUnits mjd= new TimeLocationUnits("mjd", "days since midnight November 17, 1858.", 
            Units.days , Basis.modifiedJulian );
    
    /**
     * The Julian Day (MJD) is the number of days (with decimal fraction of the day) that have elapsed since noon on January 1, 4713 BCE.  
     * Julian - 2400000.5
     */
    public static final TimeLocationUnits julianDay= new TimeLocationUnits("julianDay", "days since noon January 1, 4713 BCE", 
            Units.days , Basis.julian );
    /**
     * cdf epoch milliseconds since midnight, 01-Jan-0000, excluding those with a leap second.  There must be skipped days, because this doesn't yield 01-Jan-0000 for 0.,
     * but works fine at 1-1-2000., excluding those within a leap second
     */
    public static final TimeLocationUnits cdfEpoch= new TimeLocationUnits("cdfEpoch","milliseconds since 01-Jan-0000", 
            Units.milliseconds, Basis.since0000 );

    /**
     * the number of nanoseconds since 01-Jan-2000T12:00, roughly.  This includes leap seconds, so conversion is more than a scale,offset.
     */
    public static final TimeLocationUnits cdfTT2000= new TimeLocationUnits("cdfTT2000","nanoseconds since 01-Jan-2000, including leap seconds",
            Units.nanoseconds, Basis.since2000 );

    /**
     * the year plus the fraction into the current year, ((doy-1)/365) for non-leap years.
     */
    public static final LocationUnits decimalYear= 
            new TimeLocationUnits("decimalYear","years, plus fractional component when multiplied by year length gives day of year", 
                    Units.years, Basis.since0000 );
    
    static {
        ((Units)t2000).registerConverter(us2000, UnitsConverter.MICRO);
        ((Units)us1980).registerConverter(us2000, new UnitsConverter.ScaleOffset(1.0, -631152000000000L ) );
        ((Units)us2000).registerConverter(cdfEpoch, new UnitsConverter.ScaleOffset( 1/1000.,63113904000000L ));
        ((Units)us2000).registerConverter(cdfTT2000, new LeapSecondsConverter( true ) ); 
        ((Units)us2000).registerConverter(decimalYear, new DecimalYearConverter( true ) );
        ((Units)t2000).registerConverter(t1970, new UnitsConverter.ScaleOffset(1.0, 9.466848e8));
        ((Units)t1970).registerConverter(ms1970, UnitsConverter.MILLI );
        ((Units)t1970).registerConverter(us1970, UnitsConverter.MICRO );
        ((Units)t2000).registerConverter(t2010, new UnitsConverter.ScaleOffset(1.0, -3.1561920e+8 ));
        ((Units)t2000).registerConverter(mj1958, new UnitsConverter.ScaleOffset(1.0/8.64e4, 15340 ));
        ((Units)t2000).registerConverter(mjd, new UnitsConverter.ScaleOffset(1.0/8.64e4, 51544 ));
        ((Units)t2000).registerConverter(julianDay, new UnitsConverter.ScaleOffset(1.0/8.64e4, 51544 + 2400000.5 ) );
    }

    /****  ratiometric units ***********/
    
    public static final Units percent= new NumberUnits("%","");

    /**
     * Define a set of units to describe ratiometric (logarithmic) spacing.  Note that Units.percent
     * is no longer the defacto ratiometric spacing, and Units.percentIncrease takes its place.  
     * Note the log10Ratio is the preferred method for expressing spacing, but all are convertible
     * See logERatio, log10Ratio and google for "fold change."
     */

    /* percentIncrease is defined as <code>( b-a )*100. / a</code>.  So { 1,2,4,8 } has a spacing of 100 % diff.  */
    public static final Units dB = new NumberUnits("dB","decibels");
    public static final Units ampRatio= new NumberUnits("ampratio","amplitude ratio");
    public static final Units percentIncrease= new NumberUnits("% diff","Special dimensionless number, useful for expressing on logarithmic scale.  100% indicates a doubling");    
    public static final Units log10Ratio= new NumberUnits("log10Ratio", "Special dimensionless number, useful for expressing distances on a log10 scale" );
    public static final Units logERatio= new NumberUnits("logERatio", "Special dimensionless number, useful for expressing distances on a logE scale" );
    private static class PercentRatioConverter extends UnitsConverter {
        @Override
        public double convert(double value) {
            return ( Math.exp(value) - 1.0 ) * 100;
        }
        @Override
        public UnitsConverter getInverse() {
            if (inverse == null) {
                inverse = new UnitsConverter() {
                    @Override
                    public double convert(double value) {
                        return Math.log( value / 100 + 1. );
                    }
                    @Override
                    public UnitsConverter getInverse() {
                        return PercentRatioConverter.this;
                    }
                };
            }
            return inverse;
        }
    }
    
    /**
     * see http://en.wikipedia.org/wiki/Decibel
     */
    private static class AmpRatioConverter extends UnitsConverter {
        @Override
        public double convert(double value) {
            return ( Math.pow(10,value/20.) );
        }
        @Override
        public UnitsConverter getInverse() {
            if (inverse == null) {
                inverse = new UnitsConverter() {
                    @Override
                    public double convert(double value) {
                        return 20 * Math.log10( value );
                    }
                    @Override
                    public UnitsConverter getInverse() {
                        return AmpRatioConverter.this;
                    }
                };
            }
            return inverse;
        }
    }
    
    static {
        log10Ratio.registerConverter( logERatio, new UnitsConverter.ScaleOffset( Math.log(10), 0. ) );
        logERatio.registerConverter( percentIncrease, new PercentRatioConverter() );
        dB.registerConverter( log10Ratio, new UnitsConverter.ScaleOffset( 10, 0 ) );
        dB.registerConverter( ampRatio, new AmpRatioConverter() );
    }
    
   /* static {
        unitsMap.put("mj1958", Units.mj1958);
        unitsMap.put("t1970", Units.t1970);
        unitsMap.put("t2000", Units.t2000);
        unitsMap.put("us2000", Units.us2000);
        unitsMap.put("seconds", Units.seconds);
        unitsMap.put("s", Units.seconds);
        unitsMap.put("days", Units.days);
        unitsMap.put("microseconds", Units.microseconds);
        unitsMap.put("", Units.dimensionless);
        unitsMap.put("dB", Units.dB);
    
        unitsMap.put("Hz", Units.hertz);
        unitsMap.put("kHz", Units.kiloHertz);
        unitsMap.put("MHz", Units.megaHertz);
    }*/
    
    private String id;
    private String description;
    private final Map<Units,UnitsConverter> conversionMap = new ConcurrentHashMap<Units, UnitsConverter>();
    
    protected Units( String id ) {
        this( id, "" );
    };
    
    protected Units( String id, String description ) {
        this.id= id;
        this.description= description;
        unitsMap.put( id, this );
    };
    
    /**
     * get the id uniquely identifying the units.  Note the id may contain
     * special tokens, like "since" for time locations.
     * @return the id.
     */
    public String getId() {
        return this.id;
    }
    
    /**
     * register a converter between the units.  Note these converters can be 
     * changed together to derive conversions. (A to B, B to C defines A to C.)
     * @param toUnits the target units
     * @param converter the converter that goes from this unit to target units.
     */
    public void registerConverter(Units toUnits, UnitsConverter converter) {
        conversionMap.put(toUnits, converter);
        UnitsConverter inverse = (UnitsConverter)toUnits.conversionMap.get(this);
        if (inverse == null || inverse.getInverse() != converter) {
            toUnits.registerConverter(this, converter.getInverse());
        }
    }
    
    /**
     * return the units to which this unit is convertible.
     * @return the units to which this unit is convertible.
     * @deprecated use getConvertibleUnits, which is spelled correctly.
     */
    public Units[] getConvertableUnits() {
        return getConvertibleUnits();
    }
    
    /**
     * return the units to which this unit is convertible.
     * @return the units to which this unit is convertible.
     */
    public Units[] getConvertibleUnits() {
        Set result= new HashSet();
        LinkedList queue = new LinkedList();
        queue.add(this);
        while (!queue.isEmpty()) {
            Units current = (Units)queue.removeFirst();
            for (Map.Entry entry : current.conversionMap.entrySet()) {
                Units next = (Units)entry.getKey();
                if (!result.contains(next)) {
                    queue.add(next);
                    result.add(next);
                }
            }
        }
        // sort the list
        Comparator c= new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                Units u1= (Units)o1;
                Units u2= (Units)o2;
                if ( UnitsUtil.isTimeLocation( u1 ) ) {
                    return u1.toString().compareTo(u2.toString());
                } else {
                    try {
                        return u1.convertDoubleTo( u2, 1.0 ) < 1.0 ? -1 : 1;
                    } catch ( RuntimeException ex ) {
                        return u1.toString().compareTo(u2.toString());
                    }
                }
            }
        };
        Units[] resultArray= (Units[])result.toArray( new Units[result.size()] );
        Arrays.sort(resultArray, c);
        return resultArray;
    }
    
    /**
     * return true if the unit can be converted to toUnits.
     * @deprecated use isConvertibleTo (which does not contain spelling error)
     * @param toUnits Units object.
     * @return true if the unit can be converted to toUnits.
     */
    public boolean isConvertableTo( Units toUnits ) {
        UnitsConverter result= getConverterInternal(this, toUnits);
        return result!=null;
    }

    /**
     * return true if the unit can be converted to toUnits.
     * @param toUnits Units object.
     * @return true if the unit can be converted to toUnits.
     */
    public boolean isConvertibleTo( Units toUnits ) {
        UnitsConverter result= getConverterInternal(this, toUnits);
        return result!=null;
    }
    
    /**
     * lookup the UnitsConverter object that takes numbers from fromUnits to toUnits.  
     * This will chain together UnitsConverters registered via units.registerConverter.
     * @param fromUnits units instance that is the source units.
     * @param toUnits units instance that is the target units.
     * @return UnitsConverter object
     * @throws InconvertibleUnitsException when the conversion is not possible.
     */
    public static UnitsConverter getConverter( final Units fromUnits, final Units toUnits ) {
        logger.log(Level.FINER, "getConverter( {0} to {1} )", new Object[]{fromUnits, toUnits}); //TODO: THIS IS CALLED WITH EVERY REPAINT!!!
        UnitsConverter result= getConverterInternal(fromUnits, toUnits);
        if ( result==null ) {
            throw new InconvertibleUnitsException( fromUnits, toUnits );
        }
        return result;
    }
    
    /**
     * lookup the UnitsConverter object that takes numbers from fromUnits to toUnits.  
     * This will chain together UnitsConverters registered via units.registerConverter.
     * @param fromUnits
     * @param toUnits
     * @return UnitsConverter object
     * @throws InconvertibleUnitsException when the conversion is not possible.
     */
    private static UnitsConverter getConverterInternal( final Units fromUnits, final Units toUnits ) {
        logger.log(Level.FINE, "fromUnits={0} {1} toUnits={2} {3}", new Object[]{fromUnits,fromUnits.hashCode(), toUnits,toUnits.hashCode()});
        if (fromUnits == toUnits) {
            return UnitsConverter.IDENTITY;
        }
        
        UnitsConverter o = fromUnits.conversionMap.get(toUnits);
        if ( o != null) {
            return o;
        }
        
        Map visited = new HashMap();
        visited.put(fromUnits, null);
        LinkedList queue = new LinkedList();
        queue.add(fromUnits);
        while (!queue.isEmpty()) {
            Units current = (Units)queue.removeFirst();
            for ( Map.Entry entry : current.conversionMap.entrySet() ) {
                Units next = (Units)entry.getKey();
                if (!visited.containsKey(next)) {
                    visited.put(next, current);
                    queue.add(next);
                    if (next == toUnits) {
                        logger.log(Level.FINE, "build conversion from {0} to {1}", new Object[]{fromUnits, toUnits});
                        return buildConversion(fromUnits, toUnits, visited);
                    }
                }
            }
        }
        return null;
    }
    
    private static UnitsConverter buildConversion(Units fromUnits, Units toUnits, Map parentMap) {
        ArrayList list = new ArrayList();
        Units current = toUnits;
        while (current != null) {
            list.add(current);
            current = (Units)parentMap.get(current);
        }
        UnitsConverter converter = UnitsConverter.IDENTITY;
        for (int i = list.size() - 1; i > 0; i--) {
            Units a = (Units)list.get(i);
            Units b = (Units)list.get(i - 1);
            UnitsConverter c = (UnitsConverter)a.conversionMap.get(b);
            converter = converter.append(c);
        }
        fromUnits.registerConverter(toUnits, converter);
        return converter;
    }
    
    /**
     * Get the converter that goes from this Unit to toUnits.  E.g. 
     * Units.meters.getConverter(Units.centimeters) yields a converter that
     * multiplies by 100.
     * @param toUnits
     * @return a converter from this unit to toUnits.
     * @throws IllegalArgumentException if conversion between units is not possible
     */
    public UnitsConverter getConverter( Units toUnits ) {
        return getConverter( this, toUnits );
    }
    
    /**
     * convert the double in this units' space to toUnits' space.
     * @param toUnits the units.
     * @param value the value in toUnits.
     * @return the double in the new units system.
     * @throws InconvertibleUnitsException when the conversion is not possible.
     */
    public double convertDoubleTo( Units toUnits, double value ) {
        if ( this==toUnits ) {
            return value;
        } else {
            return getConverter(this,toUnits).convert(value);
        }
    }
    
    @Override
    public String toString() {
        return id;
    }
    
    /**
     * return the units from the Basis for the unit, such as "seconds" in
     * "seconds since midnight, Jan 1, 1970"
     * @return this units offsets.
     */
    public Units getOffsetUnits() {
        return this;
    }
    
    /**
     * return the Basis which defines the meaning of zero and the direction of positive values, such as 
     * "since midnight, Jan 1, 1970"
     * @return the Basis object, which simply identifies a basis.
     */
    public Basis getBasis() {
        return Basis.physicalZero;
    }
    
    public abstract Datum createDatum( double value );
    public abstract Datum createDatum( int value );
    public abstract Datum createDatum( long value );
    public abstract Datum createDatum( Number value );
    
    /**
     * create a Datum with the units.  For example, 
     * <ul>
     * <li>Units.cm.createDatum('5m') &rarr; '500cm'
     * <li>eu.createDatum(datum('voyager1')) will convert to the new ordinal units.
     * </ul>
     * @param value
     * @return 
     */
    public abstract Datum createDatum( Datum value );
    public abstract Datum createDatum( double value, double resolution );
    
    private final static double FILL_DOUBLE= -1e31;
    
    public double getFillDouble() { return FILL_DOUBLE; }
    public Datum getFillDatum() { return this.createDatum(FILL_DOUBLE); }
    
    public boolean isFill( double value ) {  return value<FILL_DOUBLE/10 || Double.isNaN(value); }
    public boolean isFill( Number value ) {
        return isFill(value.doubleValue());
    }
    
    /**
     * test if the double represents a valid datum in the context of this unit.
     * Note slight differences in implementation may cause isFill and isValid 
     * to produce inconsistent results.  For example, this code checks for NaNs
     * whereas isFill does not.
     *
     * @param value the value to check.
     * @return true if the data is not fill and not NaN.  
     */
    public boolean isValid( double value ) {
        return !Double.isNaN(value) && value>FILL_DOUBLE/10 ;
    }
    
    /**
     * return the formatter factor for this Datum.
     * @return 
     */
    public abstract DatumFormatterFactory getDatumFormatterFactory();
    
    /**
     * parse the string in the context of these units.  The unit may
     * throw a parse exception if it cannot be parsed, or may return
     * a 
     * @param s
     * @return
     * @throws ParseException 
     */
    public abstract Datum parse(String s) throws ParseException;
    
    /**
     * format the Datum.
     * @param datum the Datum
     * @return the Datum formatted as a string.
     */
    public String format( Datum datum ) {
        return getDatumFormatterFactory().defaultFormatter().format(datum);
    }
    
    /**
     * format the Datum, allowing use of subscripts and superscripts interpretted by GrannyTextRenderer.
     * @param datum the Datum
     * @return the Datum formatted as a string, possibly containing control sequences like !A and !n, etc.
     */
    public String grannyFormat( Datum datum ) {
        return getDatumFormatterFactory().defaultFormatter().grannyFormat(datum);
    }
    
    public abstract Datum add( Number a, Number b, Units bUnits );
    public abstract Datum subtract( Number a, Number b, Units bUnits );
    public abstract Datum multiply( Number a, Number b, Units bUnits );
    public abstract Datum divide( Number a, Number b, Units bUnits );
    
    /**
     * return all the known units.
     * @return list of all the known units.
     */
    public static List<Units> getAllUnits() {
        return new ArrayList<>(unitsMap.values());
    }
    
    /**
     * returns a Units object with the given string representation that is stored in the unitsMap.
     * Unlike lookupUnits, this will not allocate new units but will throw an IllegalArgumentException
     * if the string is not recognized.
     *
     * @param s units identifier
     * @return units object 
     * @throws IllegalArgumentException if the unit is not recognized.
     * @see #lookupUnits(java.lang.String) 
     */
    public static Units getByName(String s) {
        Units units = (Units)unitsMap.get(s);
        if (units == null) {
            throw new IllegalArgumentException("Unrecognized units: "+s);
        } else return units;
    }
    
    /**
     * return canonical das2 unit for colloquial time.
     * @param s string containing time unit like s, sec, millisec, etc.
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
     * allocated, then parse for "&lt;unit&gt; since &lt;datum&gt;" and add conversion to
     * "microseconds since 2000-001T00:00."  Note leap seconds are ignored!
     * @param base the base time, for example 2000-001T00:00.
     * @param offsetUnits the offset units for example microseconds.  Positive values of the units will be since the base time.
     * @return the unit.
     */
    public static synchronized Units lookupTimeUnits( Datum base, Units offsetUnits ) {
        Units result;
        String canonicalName = "" + offsetUnits + " since "+ base;
        try {
            result= Units.getByName(canonicalName);
            return result;
        } catch ( IllegalArgumentException ex ) {
            Basis basis= new Basis( "since "+ base, "since "+ base, Basis.since2000, base.doubleValue(Units.us2000), Units.us2000.getOffsetUnits().id );
            result= new TimeLocationUnits( canonicalName, canonicalName, offsetUnits, basis );
            result.registerConverter( Units.us2000,
                    new UnitsConverter.ScaleOffset(
                    offsetUnits.convertDoubleTo(Units.microseconds, 1.0),
                    base.doubleValue(Units.us2000) ) );
            return result;
        }        
    }
    
    /**
     * lookupUnits canonical units object, or allocate one.  If one is
     * allocated, then parse for "&lt;unit&gt; since &lt;datum&gt;" and add conversion to
     * "microseconds since 2000-001T00:00" (us2000).  Note leap seconds are ignored
     * in the returned units, so each day is 86400 seconds long, and differences in
     * times should not include leap seconds.  Note this contains a few kludges 
     * as this for datasets encountered by Autoplot.
     * @param units string like "microseconds since 2000-001T00:00" which will be the id.
     * @return a units object that implements.
     * @throws java.text.ParseException if the time cannot be parsed, etc.
     */
    public static synchronized Units lookupTimeUnits( String units ) throws ParseException {

        Units result;

        //see if it's already registered.
        try {
            result= Units.getByName(units);
            return result;
        } catch ( IllegalArgumentException ex ) {
            //do nothing until later
        }
		  
		  if(units.trim().equalsIgnoreCase("UTC")) return us2000;

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
        return lookupTimeUnits( datum, offsetUnits );
    }
    
    /**
     * lookupUnits canonical units object, or allocate one if the 
     * unit has not been used already.
     * Examples include:
     *   "nT" where it's already allocated,
     *   "apples" where it allocates a new one, and
     *   "seconds since 2011-12-21T00:00" where it uses lookupTimeUnits.
     * @param sunits string identifier.
     * @return canonical units object.
     * @see #getByName(java.lang.String) 
     */
    public static synchronized Units lookupUnits(String sunits) {
        Units result;
        sunits= sunits.trim();
        try {
            result= Units.getByName(sunits);
            
        } catch ( IllegalArgumentException ex ) {
            
            sunits= java.text.Normalizer.normalize( sunits, Normalizer.Form.NFC );
            
            try {
                result= Units.getByName(sunits);
                return result;
                
            } catch ( IllegalArgumentException ex2 ) {
                logger.fine("normalized version did not fix");
            }
            
            if ( sunits.contains(" since ") || sunits.equalsIgnoreCase("UTC") ) {
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
                        convTo = lookupUnits(m.group(2));
                        if ( convTo!=null ) {
                            double fact= Double.parseDouble(m.group(1));
                            result= new NumberUnits( sunits );
                            result.registerConverter( convTo, new UnitsConverter.ScaleOffset(fact,0.0) );
                        } else {
                            result= lookupUnits(sunits);
                        }
                    } catch ( NumberFormatException ex2 ) {
                        result= lookupUnits(sunits);
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
            if ( !stdUnit.isConvertibleTo(result) ) {
                logger.log(Level.FINE, "registering identity converter {0} -> {1}", new Object[]{stdUnit, result});
                stdUnit.registerConverter( result, UnitsConverter.IDENTITY );
                stdUnit.getConverter(result);
            }
        }
        return result;
    }

    /**
     * return unit for identifying nominal data.  Strings are enumerated using
     * this unit using the result's create(string) method, which returns a datum
     * representing the string.  This method allocates
     * a number for the string if one hasn't already been allocated, and the 
     * name is unique within the namespace "default".
     * @return an EnumerationUnit with the namespace "default"
     */
    public static EnumerationUnits nominal() {
        return nominal("default");
    }
    
    /**
     * return unit for identifying nominal data.  Strings are enumerated using
     * this unit using the result's create(string) method, which returns a datum
     * representing the string.  This method allocates
     * a number for the string if one hasn't already been allocated, and the 
     * name is unique within the namespace named.
     * @param nameSpace
     * @return an EnumerationUnit.
     */
    public static EnumerationUnits nominal( String nameSpace ) {
        return EnumerationUnits.create(nameSpace);
    }
    
    
    public static void main( String[] args ) throws java.text.ParseException {
        //Datum ratio = Datum.create(100);
        Datum ratio = Units.ampRatio.createDatum(100);
        Datum db = ratio.convertTo(dB);
        System.out.println("ratio: " + ratio);
        System.out.println("dB: " + db);
        
        Datum Hz = Datum.create(1000000.0, hertz);
        Datum kHz = Hz.convertTo(kiloHertz);
        Datum MHz = kHz.convertTo(megaHertz);
        System.out.println("Hz: " + Hz);
        System.out.println("kHz: " + kHz);
        System.out.println("MHz: " + MHz);
        
        System.err.println( Units.ms1970.createDatum(1000) );
    }
}
