/* File: Datum.java
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

import java.io.Serializable;
import org.das2.datum.format.DatumFormatter;

/**
 * <p>A Datum is a number in the context of a Unit, for example "15 microseconds."
 *   A Datum object has methods for formatting itself as a String, representing 
 *  itself as a double in the context of another Unit, and mathematical
 * operators that allow simple calculations to be made at the physical quantities.
 * Also a Datum's precision can be limited to improve formatting.</p>
 * <p>
 * @author  jbf
 */
public class Datum implements Comparable, Serializable {
    
    protected Units units;
    protected Number value;
    private double resolution;
    private transient DatumFormatter formatter;

    /**
     * class backing Datums with a double.
     */
    public static class Double extends Datum {
        
        Double( Number value, Units units ) {
            super( value, units, 0. );
        }
            
        Double( double value, Units units ) {
            super( new java.lang.Double(value), units, 0. );
        }
        
        Double( double value ) {
            super( new java.lang.Double(value), Units.dimensionless, 0. );
        }        
        
        Double( double value, Units units, double resolution ) {
            super( new java.lang.Double(value), units, units.getDatumFormatterFactory().defaultFormatter(), resolution );
        }
        
    }
    
    /**
     * class backing Datums with a long, such as with CDF_TT2000.
     */
    public static class Long extends Datum {
        public Long( long value, Units units ) {
            super(value, units, 0. );
        }
        public long longValue(Units u) {
            if ( u!=this.units ) throw new IllegalArgumentException("Units must be "+this.units);
            return this.value.longValue();
        }
    }
    
    private Datum(Number value, Units units, double resolution ) {
        this( value, units, units.getDatumFormatterFactory().defaultFormatter(), resolution );
    }
    
    private Datum(Number value, Units units, DatumFormatter formatter, double resolution ) {
        if ( value==null ) throw new IllegalArgumentException("value is null");
        this.value = value;
        this.units = units;
        this.resolution= resolution;
        this.formatter = formatter;
    }
    
    /**
     * returns the datum's double value.  This protected method allows subclasses and classes 
     * within the package to peek at the double value.
     *
     * @return the double value of the datum in the context of its units.
     */
    protected double doubleValue() {
        return this.getValue().doubleValue();
    }
    
    /**
     * returns a double representing the datum in the context of <code>units</code>.
     *
     * @param units the Units in which the double should be returned
     * @return a double in the context of the provided units.
     */
    public double doubleValue(Units units) {
        if ( units!=getUnits() ) {
            return getUnits().getConverter(units).convert(this.getValue()).doubleValue();
        } else {
            return this.getValue().doubleValue();
        }
    }
    
    /**
     * returns the double value without the unit, as long as the Units indicate this is a ratio measurement, and there is a meaningful 0.
     * For example "5 Kg" &rarr; 5, but "2012-02-16T00:00" would throw an IllegalArgumentException.  Note this was introduced because often we just need
     * to check to see if a value is zero.
     * @return
     */
    public double value() {
        if ( UnitsUtil.isRatioMeasurement(units) ) {
            return this.doubleValue( this.getUnits() );
        } else {
            throw new IllegalArgumentException("datum is not ratio measurement: "+this );
        }
    }

    /**
     * return the absolute value (magnitude) of this Datum.  If this
     * datum is fill then the result is fill.  This will have the same
     * units as the datum.  
     * @return 
     * @throws IllegalArgumentException if the datum is not a ratio measurement (like a timetag).
     * @see #value() which returns the double value.
     */
    public Datum abs() {
        if ( UnitsUtil.isRatioMeasurement(units) ) {
            if ( this.getUnits().isFill(value) ) {
                return this;
            } else if ( this.value.doubleValue()>=0 ) {
                return this;
            } else {
                return this.getUnits().createDatum( Math.abs(value.doubleValue()) );
            }
        } else {
            throw new IllegalArgumentException("datum is not a ratio measurement: "+this );
        }
    }
    

    /**
     * returns the resolution (or precision) of the datum.  This is metadata for the datum, used
     * primarily to limit the number of decimal places displayed in a string representation,
     * but operators like add and multiply will propagate errors through the calculation.
     *
     * @param units the Units in which the double resolution should be returned.  Note
     *   the units must be convertible to this.getUnits().getOffsetUnits().
     * @return the double resolution of the datum in the context of units.
     */
    public double getResolution( Units units ) {
        Units offsetUnits= getUnits().getOffsetUnits();
        if ( units!=offsetUnits ) {
            return offsetUnits.getConverter(units).convert(this.resolution);
        } else {
            return this.resolution;
        }
    }
    
    /**
     * returns the datum's int value.  This protected method allows subclasses and classes 
     * within the package to peek at the value as an integer.  (The intent was that a
     * Datum might be backed by an integer instead of a double, so that numerical 
     * round-off issues can be avoided.)
     *
     * @return the integer value of the datum in the context of its units.
     */
    protected int intValue() {
        return this.getValue().intValue();
    }
    
    /**
     * returns a int representing the datum in the context of <code>units</code>.
     *
     * @param units the Units in which the int should be returned
     * @return a double in the context of the provided units.
     */
    public int intValue(Units units) {
        if ( units!=getUnits() ) {
            return getUnits().getConverter(units).convert(this.getValue()).intValue();
        } else {
            return this.getValue().intValue();
        }
    }
    
    /**
     * returns the datum's units.  For example, UT times might have the units
     * Units.us2000.
     *
     * @return the datum's units.
     */
    public Units getUnits() {
        return this.units;
    }
    
    /**
     * returns the Number representing the datum's location in the space identified by its units. 
     * This protected method allows subclasses and classes 
     * within the package to peek at the value.  (The intent was that a
     * Datum might be backed by an integer, float, or double, depending on the application.)
     * @return a Number in the context of the provided units.
     */
    protected Number getValue() {
        return this.value;
    }
   
    /**
     * convenience method for checking to see if a datum is a fill datum.
     * @return true if the value is fill as defined by the Datum's units.
     */
    public boolean isFill() {
        return getUnits().isFill(getValue());
    }
    
    /**
     * Groovy scripting language uses this for overloading.
     * @param datum Datum to add, that is convertible to this.getUnits().
     * @return a Datum that is the sum of the two values in this Datum's units.
     */
    public Datum plus( Datum datum ) {
        return add(datum);
    }
    
    /**
     * Groovy scripting language uses this for overloading.
     * @param datum Datum to subtract, that is convertible to this.getUnits().
     * @return a Datum that is the sum of the two values in this Datum's units.
     */
    public Datum minus( Datum datum ) {
        return subtract(datum);
    }
    
    /**
     * Groovy scripting language uses this for overloading.
     * @param datum Datum to divide, that is convertible to this.getUnits().
     * @return a Datum that is the divide of the two values in this Datum's units.
     */
    public Datum div( Datum datum ) {
        return divide(datum);
    }
    
    /**
     * Groovy scripting language uses this for overloading a**b. 
     * @param b double to exponentiate, that is dimensionless.
     * @return a Datum that is the exponentiate of the two values in this Datum's units.
     */
    public Datum power( double b ) {
        if ( UnitsUtil.isRatioMeasurement(units) ) {
            return Datum.create( Math.pow( this.value(),2 ), Units.dimensionless );
        } else {
            throw new IllegalArgumentException("power argument must be dimensionless");
        }
    }

    /**
     * Groovy scripting language uses this for overloading a**b. 
     * @param datum Datum to exponentiate, that is dimensionless.
     * @return a Datum that is the sum of the two values in this Datum's units.
     */
    public Datum power( Datum datum ) {
        if ( datum.getUnits()==Units.dimensionless && UnitsUtil.isRatioMeasurement(units) ) {
            return Datum.create( Math.pow( this.value(),datum.value() ), Units.dimensionless );
        } else {
            throw new IllegalArgumentException("power argument must be dimensionless");
        }
    }
    
    /**
     * return log10 of this datum.
     * @return log10 of this datum.
     */
    public Datum log10() {
        if ( this.getUnits()==Units.dimensionless ) {
            return Units.dimensionless.createDatum( Math.log10( this.value() ) );
        } else {
            throw new IllegalArgumentException("units are not dimensionless");
        }
    }
    
    /**
     * return Math.pow(10,v)
     * @return Math.pow(10,v)
     */
    public Datum exp10() {
        if ( this.getUnits()==Units.dimensionless ) {
            return Units.dimensionless.createDatum( Math.pow(10,this.value() ) );
        } else {
            throw new IllegalArgumentException("units are not dimensionless");
        }
    }
    
    /**
     * Groovy scripting language uses this negation operator.
     * @return a Datum such that it plus this is 0.
     * @throws IllegalArgumentException when the units are location units.
     */
    public Datum negative() {
        if ( UnitsUtil.isRatioMeasurement(units) ) {
            return Datum.create( -1*value(), units );
        } else if ( UnitsUtil.isNominalMeasurement(units) ) {
            throw new IllegalArgumentException("units are Stevens ordinal");
        } else if ( UnitsUtil.isIntervalMeasurement(units) ) {
            throw new IllegalArgumentException("units are Stevens interval measurement");
        } else {
            throw new IllegalArgumentException("units are not Stevens ratio measurement");
        }
    }
    
    /**
     * Groovy scripting language uses this positive operator.
     * @return this datum.
     */
    public Datum positive() {
        return this;
    }
    
    /**
     * returns a Datum whose value is the sum of <tt>this</tt> and <tt>datum</tt>, in <tt>this.getUnits()</tt>.
     * @param datum Datum to add, that is convertible to this.getUnits().
     * @return a Datum that is the sum of the two values in this Datum's units.
     */
    public Datum add( Datum datum ) { 
        Datum result= add( datum.getValue(), datum.getUnits() ); 
        result.resolution= Math.sqrt( datum.resolution * datum.resolution + this.resolution * this.resolution );
        return result;
    }   
    
    /**
     * returns a Datum whose value is the sum of <tt>this</tt> and value in 
     * the context of <tt>units</tt>, in <tt>this.getUnits()</tt>.
     * @param value a Number to add in the context of units.
     * @param units units defining the context of value.  There should be a converter from
     * units to this Datum's units.
     * @return value Datum that is the sum of the two values in this Datum's units.
     */
    public Datum add( Number value, Units units ) {  return getUnits().add( getValue(), value, units ); }
    
    /**
     * returns a Datum whose value is the sum of <tt>this</tt> and value in 
     * the context of <tt>units</tt>, in <tt>this.getUnits()</tt>.
     * @param d a Number to add in the context of units.
     * @param units units defining the context of value.  There should be a converter from
     * units to this Datum's units.
     * @return value Datum that is the sum of the two values in this Datum's units.
     */
    public Datum add( double d, Units units ) {  return add( new java.lang.Double(d), units ); }
    
    /**
     * returns a Datum whose value is the difference of <tt>this</tt> and <tt>value</tt>.
     * The returned Datum will have units according to the type of units subtracted.
     * For example, "1979-01-02T00:00" - "1979-01-01T00:00" = "24 hours" (this datum's unit's offset units),
     * while "1979-01-02T00:00" - "1 hour" = "1979-01-01T23:00" (this datum's units.)
     *
     * Note also the resolution of the result is calculated.
     *
     * @return a Datum that is the sum of the two values in this Datum's units.
     * @param datum Datum to add, that is convertible to this.getUnits() or offset units.
     */
    public Datum subtract( Datum datum ) { 
        Datum result= subtract( datum.getValue(), datum.getUnits() );
        result.resolution= Math.sqrt( datum.resolution * datum.resolution + this.resolution * this.resolution );
        return result;
    }
                
    /**
     * returns a Datum whose value is the difference of <tt>this</tt> and value in 
     * the context of <tt>units</tt>.  
     * The returned Datum will have units according to the type of units subtracted.
     * For example, "1979-01-02T00:00" - "1979-01-01T00:00" = "24 hours" (this datum's unit's offset units),
     * while "1979-01-02T00:00" - "1 hour" = "1979-01-01T23:00" (this datum's units.)
     * 
     * @param a a Number to add in the context of units.
     * @param units units defining the context of value.  There should be a converter from
     * units to this Datum's units or offset units.
     * @return value Datum that is the difference of the two values in this Datum's units.
     */
    public Datum subtract( Number a, Units units ) { 
        Datum result= getUnits().subtract( getValue(), a, units );        
        return result; 
    }
    
    /**
     * returns a Datum whose value is the difference of <tt>this</tt> and value in 
     * the context of <tt>units</tt>.
     * The returned Datum will have units according to the type of units subtracted.
     * For example, "1979-01-02T00:00" - "1979-01-01T00:00" = "24 hours" (this datum's unit's offset units),
     * while "1979-01-02T00:00" - "1 hour" = "1979-01-01T23:00" (this datum's units.)
     * 
     * @param d a Number to add in the context of units.
     * @param units units defining the context of value.  There should be a converter from
     * units to this Datum's units or offset units.
     * @return value Datum that is the difference of the two values in this Datum's units.
     */
    public Datum subtract( double d, Units units ) {  
        return subtract( new java.lang.Double(d), units ); 
    }    
    
    private static double relativeErrorMult( double x, double dx, double y, double dy ) {
        return Math.sqrt( dx/x * dx/x + dy/y * dy/y );
    }
    
    /**
     * divide this by the datum <tt>a</tt>.  Currently, only division is only supported:<pre>
     *   between convertable units, resulting in a Units.dimensionless quantity, or
     *   by a Units.dimensionless quantity, and a datum with this datum's units is returned.</pre>
     * This may change, as a generic SI units class is planned.
     *   
     * @param a the datum divisor.
     * @return the quotient.
     */
    public Datum divide( Datum a ) { 
        Datum result= divide( a.getValue(), a.getUnits() );         
        result.resolution= Math.abs( result.doubleValue() ) * relativeErrorMult( doubleValue(), resolution, a.doubleValue(), a.resolution );
        return result;
    }
    
    /**
     * divide this by the Number provided in the context of units.  Currently, only division is only supported:<pre>
     *   between convertable units, resulting in a Units.dimensionless quantity, or
     *   by a Units.dimensionless quantity, and a datum with this datum's units is returned.</pre>
     * This may change, as a generic SI units class is planned.
     * @param a the magnitude of the divisor.
     * @param units the units of the divisor.
     * @return the quotient.
     */
    public Datum divide( Number a, Units units ) {
        return getUnits().divide( getValue(), a, units ); 
    }
    
    /**
     * divide this by the dimensionless double.
     * @param d the magnitude of the divisor.
     * @return the quotient.
     */
    public Datum divide( double d ) {  
        return divide( d, Units.dimensionless ); 
    }
    
    /**
     * multiply this by the datum <tt>a</tt>.  Currently, only multiplication is only supported
     *   by a dimensionless datum, or when this is dimensionless.
     * This may change, as a generic SI units class is planned.
     *
     * This should also throw an IllegalArgumentException if the units are LocationUnits (e.g. UT time), but doesn't.  This may
     * change.
     *   
     * @param a the datum to multiply
     * @return the product.
     */
    public Datum multiply( Datum a ) { 
        Datum result= multiply( a.getValue(), a.getUnits() );         
        result.resolution= result.doubleValue() * relativeErrorMult( doubleValue(), resolution, a.doubleValue(), a.resolution );
        return result;
    }
    
    /**
     * multiply this by the Number provided in the context of units.  Currently, only multiplication is only supported
     *   by a dimensionless datum, or when this is dimensionless.
     * This may change, as a generic SI units class is planned.
     *
     * This should also throw an IllegalArgumentException if the units are LocationUnits (e.g. UT time), but doesn't.  This may
     * change.
     *
     * @param a the magnitude of the multiplier.
     * @param units the units of the multiplier.
     * @return the product.
     */
    public Datum multiply( Number a, Units units ) {
        return getUnits().multiply( getValue(), a, units );
    }
    
    /**
     * multiply by a dimensionless number.
     *
     * This should also throw an IllegalArgumentException if the units are LocationUnits (e.g. UT time), but doesn't.  This may
     * change.
     *
     * @param d the multiplier.
     * @return the product.
     */
    public Datum multiply( double d ) {  
        return multiply( d, Units.dimensionless );
    }
    
    /**
     * creates an equivalent datum using a different unit.  For example,<code>
     *  x= Datum.create( 5, Units.seconds );
     *  System.err.println( x.convertTo( Units.seconds ) );
     * </code>
     * @param units the new Datum's units
     * @throws java.lang.IllegalArgumentException if the datum cannot be converted to the given units.
     * @return a datum with the new units, that is equal to the original datum.
     */
    public Datum convertTo( Units units ) throws IllegalArgumentException {
        UnitsConverter muc= this.units.getConverter(units);
        Datum result= units.createDatum( muc.convert( this.getValue() ) );
        if ( this.resolution!=0. ) {
            muc= this.units.getOffsetUnits().getConverter(units.getOffsetUnits());
            result.resolution= muc.convert(this.resolution);
        }
        return result;
    }
    
    /**
     * returns a hashcode that is a function of the value and the units.
     * @return a hashcode for the datum
     */
    @Override
    public int hashCode() {
        long bits = (long) getValue().hashCode();
        int doubleHash= (int)(bits ^ (bits >>> 32));
        int unitsHash= units.hashCode(); //TODO: this should probably be converted to canonical unit to match equals.  ("1 km".equals("1000 m")) is true.
        return doubleHash ^ unitsHash;
    }
    
    /**
     * returns true if the two datums are equal.  That is, their double values are equal when converted to the same units.
     * @param a the Object to compare to.
     * @throws java.lang.IllegalArgumentException if the Object is not a datum or the units are not convertable.
     * @return true if the datums are equal.
     */
    @Override
    public boolean equals( Object a ) throws IllegalArgumentException {
        return ( a!=null && (a instanceof Datum) && this.equals( (Datum)a ) );
    }
    
    /**
     * returns true if the two datums are equal.  That is, their double values are equal when converted to the same units.
     * @param a the datum to compare
     * @throws java.lang.IllegalArgumentException if the units are not convertable.
     * @return true if the datums are equal.
     */
    public boolean equals( Datum a ) throws IllegalArgumentException {
        return ( a!=null && this.getUnits().isConvertibleTo( a.getUnits() ) && this.compareTo(a)==0 );
    }
    
    /**
     * returns true if this is less than <tt>a</tt>.
     * @param a a datum convertible to this Datum's units.
     * @throws java.lang.IllegalArgumentException if the two don't have convertible units.
     * @return true if this is less than <tt>a</tt>.
     */
    public boolean lt( Datum a ) throws IllegalArgumentException {
        return (this.compareTo(a)<0);
    }
    
    /**
     * returns true if this is greater than <tt>a</tt>.
     * @param a a datum convertible to this Datum's units.
     * @throws java.lang.IllegalArgumentException if the two don't have convertible units.
     * @return true if this is greater than <tt>a</tt>.
     */
    public boolean gt( Datum a ) throws IllegalArgumentException {
        return (this.compareTo(a)>0);
    }
    
    /**
     * returns true if this is less than or equal to <tt>a</tt>.
     * @param a a datum convertible to this Datum's units.
     * @throws java.lang.IllegalArgumentException if the two don't have convertible units.
     * @return true if this is less than or equal to <tt>a</tt>.
     */
    public boolean le( Datum a ) throws IllegalArgumentException {
        return (this.compareTo(a)<=0);
    }
    
    /**
     * returns true if this is greater than or equal to <tt>a</tt>.
     * @param a a datum convertible to this Datum's units.
     * @throws java.lang.IllegalArgumentException if the two don't have convertible units.
     * @return true if this is greater than or equal to <tt>a</tt>.
     */
    public boolean ge( Datum a ) throws IllegalArgumentException {
        return (this.compareTo(a)>=0);
    }
    
    /**
     * compare the datum to the object.
     * @return an int &lt; 0 if this comes before Datum a in this Datum's units space, 0 if they are equal, and &gt; 0 otherwise.
     * @param a the Object to compare this datum to.
     * @throws IllegalArgumentException if a is not a Datum or is not convertible to this Datum's units.
     */
    @Override
    public int compareTo( Object a ) throws IllegalArgumentException {
        if ( ! (a instanceof Datum) ) throw new IllegalArgumentException("comparable type mismatch");
        return compareTo((Datum)a);
    }
    
    /**
     * compare this to another datum.
     * @return an int &lt; 0 if this comes before Datum a in this Datum's units space,
     * 0 if they are equal, and &gt; 0 otherwise.
     * @param a the Datum to compare this datum to.
     * @throws IllegalArgumentException if a is not convertible to this Datum's
     * units.
     */
    public int compareTo( Datum a ) throws IllegalArgumentException {
        if ( this.units != a.units ) {
            a= a.convertTo(this.units);
        }
        
        return java.lang.Double.compare( this.getValue().doubleValue(), a.getValue().doubleValue() );
        
    }
    
    /**
     * returns true if the value is finite, that is not INFINITY or NaN.
     * @return true if the value is finite, that is not INFINITY or NaN.
     */
    public boolean isFinite() {
        return ( value.doubleValue()!=java.lang.Double.POSITIVE_INFINITY )
        && ( value.doubleValue()!=java.lang.Double.NEGATIVE_INFINITY )
        && ( !java.lang.Double.isNaN( value.doubleValue() ) );
    }
    
    /**
     * returns a human readable String representation of the Datum, which should also be parseable with
     * Units.parse()
     * @return a human readable String representation of the Datum, which should also be parseable with
     * Units.parse()
     */
    @Override
    public String toString() {
        Datum d= this;
        if ( this.getUnits().isConvertibleTo(Units.seconds ) ) {
            d= DatumUtil.asOrderOneUnits(d);
        }
        if (formatter==null) {
            return units.getDatumFormatterFactory().defaultFormatter().format(d);
        } else {
            return formatter.format(d);
        }
    }
    
    /**
     * convenient method for creating a dimensionless Datum with the given value.
     * @param value the magnitude of the datum.
     * @return a dimensionless Datum with the given value.
     */
    public static Datum create(double value) {
        return Units.dimensionless.createDatum(value);
    }
    
    /**
     * creates a datum with the given units and value, for example,
     * <tt>Datum.create( 54, Units.milliseconds )</tt>
     * @param value the magnitude of the datum.
     * @param units the units of the datum.
     * @return a Datum with the given units and value.
     */
    public static Datum create( double value, Units units ) {
        if ( units==null ) {
            throw new NullPointerException("Units are null");
        }
        return units.createDatum( value );
    }
    
    /**
     * Returns a Datum with a specific DatumFormatter attached to
     * it.  This was was used to limit resolution before limited resolution
     * Datums were introduced.
     *
     * @param value the magnitude of the datum.
     * @param units the units of the datum.
     * @param formatter the DatumFormatter that should be used to format this datum, which will be
     *   returned by getFormatter().
     * @return a Datum with the given units and value, that should return the given formatter when asked.  
     */
    public static Datum create( double value, Units units, DatumFormatter formatter ) {
        Datum result= create( value, units);
        result.formatter= formatter;
        return result;
    }
    
    /**
     * Returns a Datum with the given value and limited to the given resolution.
     * When formatted, the formatter should use this resolution to limit the 
     * precision displayed.
     * @param value the magnitude of the datum, or value to be interpreted in the context of units.
     * @param units the units of the datum.
     * @param resolution the limit to which the datum's precision is known.
     * @return a Datum with the given units and value.
     */
    public static Datum create( double value, Units units, double resolution ) {
        Datum result= units.createDatum( value, resolution );
        result.formatter= units.getDatumFormatterFactory().defaultFormatter();
        return result;
    }
    
    /**
     * Returns a Datum with the given value and limited to the given resolution.
     * When formatted, the formatter should use this resolution to limit the 
     * precision displayed.
     * @param value the magnitude of the datum, or value to be interpreted in the context of units.
     * @param units the units of the datum.
     * @param resolution the limit to which the datum's precision is known.
     * @param formatter the DatumFormatter that should be used to format this datum, which will be
     *   returned by getFormatter().
     * @return a Datum with the given units and value.
     */
    public static Datum create( double value, Units units, double resolution, DatumFormatter formatter ) {
        Datum result= units.createDatum( value, resolution );
        result.formatter= formatter;
        return result;
    }
    
    /**
     * creates a dimensionless datum backed by an int.
     * @return a dimensionless Datum with the given value.
     * @param value the magnitude of the dimensionless datum.
     */
    public static Datum create( int value ) {
        return Units.dimensionless.createDatum( value );
    }
    
    /**
     * creates a datum backed by an int with the given units.
     * @return a Datum with the given units and value.
     * @param value the magnitude of the datum
     * @param units the units of the datum
     */
    public static Datum create( int value, Units units ) {
        return units.createDatum( value );
    }
    
    /**
     * returns a formatter suitable for formatting this datum as a string.
     * @return a formatter to be used to format this Datum into a String.
     */
    public DatumFormatter getFormatter() {
        return this.formatter;
    }
    
}
