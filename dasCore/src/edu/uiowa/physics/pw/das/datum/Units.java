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

package edu.uiowa.physics.pw.das.datum;

import edu.uiowa.physics.pw.das.components.propertyeditor.*;
import edu.uiowa.physics.pw.das.datum.format.*;
import edu.uiowa.physics.pw.das.util.*;

import java.util.*;
import java.text.ParseException;
/**
 *
 * @author  jbf
 */
public abstract class Units implements Displayable {
    
    private static Map unitsMap = new HashMap();
    
    public static final Units dimensionless= new NumberUnits("");
    public static final Units dB = new NumberUnits("dB");
    public static final Units radians= new NumberUnits("radian");
    public static final Units degrees= new NumberUnits("degrees");
    static {
        dimensionless.registerConverter(dB, new dBConverter());
    }
    private static final class dBConverter extends UnitsConverter {
        public double convert(double value) {
            return 10 * DasMath.log10(value);
        }
        public UnitsConverter getInverse() {
            if (inverse == null) {
                inverse = new UnitsConverter() {
                    public double convert(double value) {
                        return Math.pow(10.0, value / 10.0);
                    }
                    public UnitsConverter getInverse() {
                        return dBConverter.this;
                    }
                };
            }
            return inverse;
        }
    }
    
    public static final Units celcius= new NumberUnits("deg C");
    public static final Units fahrenheit= new NumberUnits("deg F");
    static {
        celcius.registerConverter(fahrenheit, new UnitsConverter.ScaleOffset(1.8, 32));
    }
    
    public static final Units hours= new NumberUnits("hr");
    public static final Units minutes= new NumberUnits("min");
    public static final Units seconds= new NumberUnits("s");
    public static final Units microseconds= new NumberUnits("microseconds");
    public static final Units nanoseconds= new NumberUnits("nanoseconds");
    public static final Units days= new NumberUnits("days");
    static {
        seconds.registerConverter(microseconds, UnitsConverter.MICRO);
        seconds.registerConverter(nanoseconds,UnitsConverter.NANO);
        hours.registerConverter(seconds, new UnitsConverter.ScaleOffset( 3600.,0.0));
        minutes.registerConverter(seconds, new UnitsConverter.ScaleOffset( 60.,0.0));
        days.registerConverter(seconds, new UnitsConverter.ScaleOffset(8.64e4, 0.0));
    }
    
    public static final Units bytesPerSecond= new NumberUnits("bytes/s");
    public static final Units kiloBytesPerSecond= new NumberUnits("KBytes/s");
    public static final Units bytes= new NumberUnits( "bytes" );
    public static final Units kiloBytes= new NumberUnits( "KBytes" );
    
    public static final Units hertz= new NumberUnits("Hz");
    public static final Units kiloHertz = new NumberUnits("kHz");
    public static final Units megaHertz = new NumberUnits("MHz");
    static {
        hertz.registerConverter(kiloHertz, UnitsConverter.KILO);
        hertz.registerConverter(megaHertz, UnitsConverter.MEGA);
    }
    
    public static final Units meters = new NumberUnits("m");
    public static final Units kiloMeters = new NumberUnits("km");
    static {
        meters.registerConverter(kiloMeters, UnitsConverter.KILO);
    }
    
    public static final TimeLocationUnits us2000= new TimeLocationUnits("us2000", "Microseconds since midnight Jan 1, 2000.",Units.microseconds);
    public static final TimeLocationUnits t2000= new TimeLocationUnits("t2000","Seconds since midnight Jan 1, 2000.",Units.seconds);
    public static final TimeLocationUnits t1970= new TimeLocationUnits("t1970","Seconds since midnight Jan 1, 1970",Units.seconds);
    public static final TimeLocationUnits mj1958= new TimeLocationUnits("mj1958","Julian - 2436204.5", Units.days);
    static {
        ((Units)t2000).registerConverter(us2000, UnitsConverter.MICRO);
        ((Units)t2000).registerConverter(t1970, new UnitsConverter.ScaleOffset(1.0, 9.466848e8));
        ((Units)t2000).registerConverter(mj1958, new UnitsConverter.ScaleOffset(1.0/8.64e4, 1.534e4));
    }
    
    public static final EnumerationUnits spacecraft= new EnumerationUnits( "spacecraft", "Enumeration of various spacecraft" );
    
    static {
        Units.spacecraft.createDatum( "Voyager1" );
        Units.spacecraft.createDatum( "Voyager2" );
        Units.spacecraft.createDatum( "Cluster Rumba" );
        Units.spacecraft.createDatum( "Cluster Salsa" );
        Units.spacecraft.createDatum( "Cluster Samba" );
        Units.spacecraft.createDatum( "Cluster Tango" );
    }
    
    public static final Units percent= new NumberUnits("%","");

    /**
     * Define a set of units to describe ratiometric (logarythmic) spacing.  Note that Units.percent
     * is no longer the defacto ratiometric spacing, and Units.percentIncrease takes its place.  
     * Note the log10Ratio is the preferred method for expressing spacing, but all are convertable
     * See logERatio, log10Ratio and google for "fold change."
     */

    /* percentIncrease is defined as <code>( b-a )*100. / a</code>.  So { 1,2,4,8 } has a spacing of 100 % diff.  */
    public static final Units percentIncrease= new NumberUnits("% diff","Special dimensionless number, useful for expressing on logarithmic scale.  100% indicates a doubling");    
    public static final Units log10Ratio= new NumberUnits("log10Ratio", "Special dimensionless number, useful for expressing distances on a log10 scale" );
    public static final Units logERatio= new NumberUnits("logERatio", "Special dimensionless number, useful for expressing distances on a logE scale" );
    private static class PercentRatioConverter extends UnitsConverter {
        public double convert(double value) {
            return ( Math.exp(value) - 1.0 ) * 100;
        }
        public UnitsConverter getInverse() {
            if (inverse == null) {
                inverse = new UnitsConverter() {
                    public double convert(double value) {
                        return Math.log( value / 100 + 1. );
                    }
                    public UnitsConverter getInverse() {
                        return PercentRatioConverter.this;
                    }
                };
            }
            return inverse;
        }
    }
    static {
        log10Ratio.registerConverter( logERatio, new UnitsConverter.ScaleOffset( Math.log(10), 0. ) );
        logERatio.registerConverter( percentIncrease, new PercentRatioConverter() );
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
    private Map conversionMap = new IdentityHashMap();
    
    protected Units( String id ) {
        this( id, "" );
    };
    
    protected Units( String id, String description ) {
        this.id= id;
        this.description= description;
        unitsMap.put( id, this );
    };
    
    private void registerConverter(Units toUnits, UnitsConverter converter) {
        conversionMap.put(toUnits, converter);
        UnitsConverter inverse = (UnitsConverter)toUnits.conversionMap.get(this);
        if (inverse == null || inverse.getInverse() != converter) {
            toUnits.registerConverter(this, converter.getInverse());
        }
    }
    
    public Units[] getConvertableUnits() {
        Set result= new HashSet();
        LinkedList queue = new LinkedList();
        queue.add(this);
        while (!queue.isEmpty()) {
            Units current = (Units)queue.removeFirst();
            for (Iterator i = current.conversionMap.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry)i.next();
                Units next = (Units)entry.getKey();
                if (!result.contains(next)) {
                    queue.add(next);
                    result.add(next);
                }
            }
        }
        return (Units[])result.toArray( new Units[result.size()] );
    }
    
    public boolean isConvertableTo( Units toUnits ) {
        try {
            this.getConverter(toUnits);
            return true;
        } catch ( IllegalArgumentException e ) {
            return false;
        }
    }
    
    /**
     *
     * @param fromUnits
     * @param toUnits
     * @return
     * @throws IllegalArgumentException when the conversion is not possible.
     */
    public static UnitsConverter getConverter( final Units fromUnits, final Units toUnits ) {
        if (fromUnits == toUnits) {
            return UnitsConverter.IDENTITY;
        }
        if (fromUnits.conversionMap.get(toUnits) != null) {
            return (UnitsConverter)fromUnits.conversionMap.get(toUnits);
        }
        Map visited = new HashMap();
        visited.put(fromUnits, null);
        LinkedList queue = new LinkedList();
        queue.add(fromUnits);
        while (!queue.isEmpty()) {
            Units current = (Units)queue.removeFirst();
            for (Iterator i = current.conversionMap.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry)i.next();
                Units next = (Units)entry.getKey();
                if (!visited.containsKey(next)) {
                    visited.put(next, current);
                    queue.add(next);
                    if (next == toUnits) {
                        return buildConversion(fromUnits, toUnits, visited);
                    }
                }
            }
        }
        String sfrom= ( fromUnits==Units.dimensionless ) ? "(dimensionless)" : fromUnits.toString();
        String sto= ( toUnits==Units.dimensionless ) ? "(dimensionless)" : toUnits.toString();
        throw new IllegalArgumentException("Inconvertible units: " + sfrom + " -> " + sto );
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
     *
     * @param toUnits
     * @return
     * @throws IllegalArgumentException if conversion between units is not possible
     */
    public UnitsConverter getConverter( Units toUnits ) {
        return getConverter( this, toUnits );
    }
    
    public double convertDoubleTo( Units toUnits, double value ) {
        if ( this==toUnits ) {
            return value;
        } else {
            return getConverter(this,toUnits).convert(value);
        }
    }
    
    public String toString() {
        return id;
    }
    
    public Units getOffsetUnits() {
        return this;
    }
    
    public abstract Datum createDatum( double value );
    public abstract Datum createDatum( int value );
    public abstract Datum createDatum( long value );
    public abstract Datum createDatum( Number value );
    
    public abstract Datum createDatum( double value, double resolution );
    
    private final static double FILL_DOUBLE= -1e31;
    private final static float FILL_FLOAT= -1e31f;
    private final static int FILL_INT= Integer.MAX_VALUE;
    private final static long FILL_LONG= Long.MAX_VALUE;
    
    public double getFillDouble() { return FILL_DOUBLE; }
    public float getFillFloat() { return FILL_FLOAT; }
    public int getFillInt() { return FILL_INT; }
    public long getFillLong() { return FILL_LONG; }
    
    public boolean isFill( double value ) {  return value<FILL_DOUBLE/10; }
    public boolean isFill( float value ) { return value<FILL_FLOAT/10; }
    public boolean isFill( long value ) { return value==FILL_LONG; }
    public boolean isFill( int value ) { return value==FILL_INT; }
    public boolean isFill( Number value ) {
        if ( value instanceof Double ) {
            return isFill(value.doubleValue());
        } else if ( value instanceof Float ) {
            return isFill(value.floatValue());
        } else if ( value instanceof Integer ) {
            return isFill(value.intValue());
        } else if ( value instanceof Long ) {
            return isFill(value.longValue());
        } else {
            throw new IllegalArgumentException("Unknown Number class: "+value.getClass().toString());
        }
    }
    
    public abstract DatumFormatterFactory getDatumFormatterFactory();
    
    public abstract Datum parse(String s) throws ParseException;
    public String format( Datum datum ) {
        return getDatumFormatterFactory().defaultFormatter().format(datum);
    }
    public String grannyFormat( Datum datum ) {
        return getDatumFormatterFactory().defaultFormatter().grannyFormat(datum);
    }
    
    public abstract Datum add( Number a, Number b, Units bUnits );
    public abstract Datum subtract( Number a, Number b, Units bUnits );
    public abstract Datum multiply( Number a, Number b, Units bUnits );
    public abstract Datum divide( Number a, Number b, Units bUnits );
    
    /**
     * returns a Units object with the given string representation that is stored in the unitsMap.
     *
     * @throws IllegalArgumentException if the unit is not recognized.
     */
    public static Units getByName(String s) {
        Units units = (Units)unitsMap.get(s);
        if (units == null) {
            throw new IllegalArgumentException("Unrecognized units: "+s);
        } else return units;
    }
    
    public static void main( String[] args ) throws java.text.ParseException {
        Datum ratio = Datum.create(100);
        Datum db = ratio.convertTo(dB);
        System.out.println("ratio: " + ratio);
        System.out.println("dB: " + db);
        
        Datum Hz = Datum.create(1000000.0, hertz);
        Datum kHz = Hz.convertTo(kiloHertz);
        Datum MHz = kHz.convertTo(megaHertz);
        System.out.println("Hz: " + Hz);
        System.out.println("kHz: " + kHz);
        System.out.println("MHz: " + MHz);
    }
    
    public javax.swing.Icon getListIcon() {
        return null;
    }
    
    public String getListLabel() {
        return this.id;
    }
}
