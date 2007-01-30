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

package edu.uiowa.physics.pw.das.datum;

import edu.uiowa.physics.pw.das.datum.format.*;

/**
 * <p>A Datum is a number in the context of a Unit, for example "15 microseconds.".  
 *   A Datum object has methods for formatting itself as a String, representing 
 *  itsself as a double in the context of another Unit, and mathematical
 * operators that allow simple calculations to be made at the physical quantities.
 * Also a Datum's precision can be limited to improve formatting.</p>
 * <p>
 * @author  jbf
 */
public class Datum implements Comparable {
    
    private Units units;
    private Number value;
    private double resolution;
    private DatumFormatter formatter;
    
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
    
    private Datum(Number value, Units units, double resolution ) {
        this( value, units, units.getDatumFormatterFactory().defaultFormatter(), resolution );
    }
    
    private Datum(Number value, Units units, DatumFormatter formatter, double resolution ) {
        this.value = value;
        this.units = units;
        this.resolution= resolution;
        this.formatter = formatter;
    }
    
    /**
     * 
     * @return 
     */
    protected double doubleValue() {
        return this.getValue().doubleValue();
    }
    
    /**
     * 
     * @param units 
     * @return 
     */
    public double doubleValue(Units units) {
        if ( units!=getUnits() ) {
            return getUnits().getConverter(units).convert(this.getValue()).doubleValue();
        } else {
            return this.getValue().doubleValue();
        }
    }
    
    /**
     * 
     * @param units 
     * @return 
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
     * 
     * @return 
     */
    protected int intValue() {
        return this.getValue().intValue();
    }
    
    /**
     * 
     * @param units 
     * @return 
     */
    public int intValue(Units units) {
        if ( units!=getUnits() ) {
            return getUnits().getConverter(units).convert(this.getValue()).intValue();
        } else {
            return this.getValue().intValue();
        }
    }
    
    /**
     * 
     * @return 
     */
    public Units getUnits() {
        return this.units;
    }
    
    /**
     * 
     * @return 
     */
    public Number getValue() {
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
     * returns a Datum whose value is the sum of the <tt>this</tt> and <tt>datum</tt>, in <tt>this.getUnits()</tt>.
     * 
     * @param datum Datum to add, that is convertable to this.getUnits().
     * @return a Datum that is the sum of the two values in this Datum's units.
     * @throws IllegalArgumentException if the units are not convertable or addition operator 
     * is not allowed.  For example, "1970-001 00:00" + "1970-001 00:00".
     */
    public Datum add( Datum datum ) { 
        Datum result= add( datum.getValue(), datum.getUnits() ); 
        result.resolution= Math.sqrt( datum.resolution * datum.resolution + this.resolution * this.resolution );
        return result;
    }    
    
    /**
     * returns a Datum whose value is the sum of the <tt>this</tt> and value in 
     * the context of <tt>units</tt>, in <tt>this.getUnits()</tt>.
     * 
     * @param value a Number to add in the context of units.
     * @param units units defining the context of value.  There should be a converter from
     * units to this Datum's units.
     * @return value Datum that is the sum of the two values in this Datum's units.
     */
    public Datum add( Number value, Units units ) {  return getUnits().add( getValue(), value, units ); }
    
    /**
     * returns a Datum whose value is the sum of the <tt>this</tt> and value in 
     * the context of <tt>units</tt>, in <tt>this.getUnits()</tt>.
     * 
     * @param value a Number to add in the context of units.
     * @param units units defining the context of value.  There should be a converter from
     * units to this Datum's units.
     * @return value Datum that is the sum of the two values in this Datum's units.
     */
    public Datum add( double d, Units units ) {  return add( new java.lang.Double(d), units ); }
    
    /**
     * returns a Datum whose value is the difference of <tt>this</tt> and <tt>value</tt>.
     * The returned Datum's has units 
     *
     * 
     * @param datum Datum to add, that is convertable to this.getUnits().
     * @return a Datum that is the sum of the two values in this Datum's units.
     * @throws IllegalArgumentException if the units are not convertable or addition operator 
     * is not allowed.  For example, "1970-001 00:00" + "1970-001 00:00".
     */
    public Datum subtract( Datum datum ) { 
        Datum result= subtract( datum.getValue(), datum.getUnits() );
        result.resolution= Math.sqrt( datum.resolution * datum.resolution + this.resolution * this.resolution );
        return result;
    }
                
    /**
     * 
     * @param a 
     * @param units 
     * @return 
     */
    public Datum subtract( Number a, Units units ) { 
        Datum result= getUnits().subtract( getValue(), a, units );        
        return result; 
    }
    /**
     * 
     * @param d 
     * @param units 
     * @return 
     */
    public Datum subtract( double d, Units units ) {  return subtract( new java.lang.Double(d), units ); }    
    
    public Datum divide( Datum a ) { return getUnits().divide( getValue(), a.getValue(), a.getUnits() ); }
    public Datum divide( Number a, Units units ) { return getUnits().divide( getValue(), a, units ); }
    public Datum divide( double d ) {  return divide( new java.lang.Double(d), Units.dimensionless ); }
    
    public Datum multiply( Datum a ) { return getUnits().multiply( getValue(), a.getValue(), a.getUnits() ); }
    public Datum multiply( Number a, Units units ) { return getUnits().multiply( getValue(), a, units ); }
    public Datum multiply( double d ) {  return multiply( new java.lang.Double(d), Units.dimensionless ); }
    
    public Datum convertTo( Units units ) throws IllegalArgumentException {
        UnitsConverter muc= this.units.getConverter(units);
        Datum result= units.createDatum( muc.convert( this.getValue() ) );
        if ( this.resolution!=0. ) {
            muc= this.units.getOffsetUnits().getConverter(units.getOffsetUnits());
            result.resolution= muc.convert(this.resolution);
        }
        return result;
    }
    
    public int hashCode() {
        long bits = (long) getValue().hashCode();
        int doubleHash= (int)(bits ^ (bits >>> 32));
        int unitsHash= units.hashCode();
        return doubleHash ^ unitsHash;
    }
    
    public boolean equals( Object a ) throws IllegalArgumentException {
        return ((a instanceof Datum) && this.equals( (Datum)a ) );
    }
    
    public boolean equals( Datum a ) throws IllegalArgumentException {
        return ( a.units==this.units && a.value.equals(this.value) );
    }
    
    public boolean lt( Datum a ) throws IllegalArgumentException {
        return (this.compareTo(a)<0);
    }
    
    public boolean gt( Datum a ) throws IllegalArgumentException {
        return (this.compareTo(a)>0);
    }
    
    public boolean le( Datum a ) throws IllegalArgumentException {
        return (this.compareTo(a)<=0);
    }
    
    public boolean ge( Datum a ) throws IllegalArgumentException {
        return (this.compareTo(a)>=0);
    }
    
    /**
     * @return an int <0 if this comes before Datum a in this Datum's units space,
     * 0 if they are equal, and >0 otherwise.
     * @throws IllegalArgumentException if a is not convertable to this Datum's
     * units.
     */
    public int compareTo( Object a ) throws IllegalArgumentException {
        if ( ! (a instanceof Datum) ) throw new IllegalArgumentException("comparable type mismatch");
        return compareTo((Datum)a);
    }
    
    /**
     * @return an int <0 if this comes before Datum a in this Datum's units space,
     * 0 if they are equal, and >0 otherwise.
     * @throws IllegalArgumentException if a is not convertable to this Datum's
     * units.
     */
    public int compareTo( Datum a ) throws IllegalArgumentException {
        if ( this.units != a.units ) {
            a= a.convertTo(this.units);
        }
        
        double d= this.getValue().doubleValue() - a.getValue().doubleValue();
        
        if (d==0.) {
            return 0;
        } else if ( d<0. ) {
            return -1;
        } else {
            return 1;
        }
    }
    
    /**
     * @return true if the value is non NaN.
     * @deprecated Use isFinite instead, or getValue.
     */
    public boolean isValid() {
        return (value.doubleValue()!=java.lang.Double.NaN);
    }
    
    /**
     * @return true if the value is finite, that is not INFINITY or NaN.
     */
    public boolean isFinite() {
        return ( value.doubleValue()!=java.lang.Double.POSITIVE_INFINITY )
        && ( value.doubleValue()!=java.lang.Double.NEGATIVE_INFINITY )
        && ( value.doubleValue()!=java.lang.Double.NaN );
    }
    
    public String toString() {
        if (formatter==null) {
            return units.getDatumFormatterFactory().defaultFormatter().format(this);
        } else {
            return formatter.format(this);
        }
    }
    
    /**
     * @return a dimensionless Datum with the given value.
     */
    public static Datum create(double value) {
        return Units.dimensionless.createDatum(value);
    }
    
    /**
     * @return a Datum with the given units and value.
     */
    public static Datum create( double value, Units units ) {
        return units.createDatum( value );
    }
    
    /**
     * Returns a Datum with a specific DatumFormatter attached to
     * it.  This was was used to limit resolution before limited resolution
     * Datums were introduced.
     *
     * @return a Datum with the given units and value, that should
     * return the given formatter when asked.  
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
     */
    public static Datum create( double value, Units units, double resolution ) {
        Datum result= units.createDatum( value, resolution );
        result.formatter= units.getDatumFormatterFactory().defaultFormatter();
        return result;
    }
    
    /**
     * @return a dimensionless Datum with the given value.
     */
    public static Datum create( int value ) {
        return Units.dimensionless.createDatum( value );
    }
    
    /**
     * @return a Datum with the given units and value.
     */
    public static Datum create( int value, Units units ) {
        return units.createDatum( value );
    }
    
    /**
     * @return a formatter to be used to format this Datum into a String.
     */
    public DatumFormatter getFormatter() {
        return this.formatter;
    }
    
}
