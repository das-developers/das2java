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
 *
 * @author  jbf
 */
public class Datum {
    
    private Units units;
    private Number value;
    private double resolution;
    private DatumFormatter formatter;
    
    public static class Double extends Datum {
        
        Double( Number value, Units units ) {
            super(value,units);
        }
        Double( double value, Units units ) {
            this(new java.lang.Double(value),units);
        }
        Double( double value ) {
            this( value, Units.dimensionless );
        }        
        Double( double value, Units units, double resolution ) {
            super( new java.lang.Double(value), units, units.getDatumFormatterFactory().defaultFormatter(), resolution );
        }
        
    }
    
    private Datum(Number value, Units units) {
        this( value, units, units.getDatumFormatterFactory().defaultFormatter(), 0. );
    }
    
    private Datum(Number value, Units units, DatumFormatter formatter, double resolution ) {
        this.value = value;
        this.units = units;
        this.resolution= resolution;
        this.formatter = formatter;
    }
    
    protected double doubleValue() {
        return this.getValue().doubleValue();
    }
    
    public double doubleValue(Units units) {
        if ( units!=getUnits() ) {
            return getUnits().getConverter(units).convert(this.getValue()).doubleValue();
        } else {
            return this.getValue().doubleValue();
        }
    }
    
    public double getResolution( Units units ) {
        Units offsetUnits= getUnits().getOffsetUnits();
        if ( units!=offsetUnits ) {
            return offsetUnits.getConverter(units).convert(this.resolution);
        } else {
            return this.resolution;
        }
    }
    
    protected int intValue() {
        return this.getValue().intValue();
    }
    
    public int intValue(Units units) {
        if ( units!=getUnits() ) {
            return getUnits().getConverter(units).convert(this.getValue()).intValue();
        } else {
            return this.getValue().intValue();
        }
    }
    
    public Units getUnits() {
        return this.units;
    }
    
    public Number getValue() {
        return this.value;
    }
   
    public boolean isFill() {
        return getUnits().isFill(getValue());
    }
    
    public Datum add( Datum a ) { 
        Datum result= add( a.getValue(), a.getUnits() ); 
        result.resolution= Math.sqrt( a.resolution * a.resolution + this.resolution * this.resolution );
        return result;
    }    
    
    public Datum add( Number a, Units units ) {  return getUnits().add( getValue(), a, units ); }
    public Datum add( double d, Units units ) {  return add( new java.lang.Double(d), units ); }
    
    public Datum subtract( Datum a ) { 
        Datum result= subtract( a.getValue(), a.getUnits() );
        result.resolution= Math.sqrt( a.resolution * a.resolution + this.resolution * this.resolution );
        return result;
    }
                
    public Datum subtract( Number a, Units units ) { 
        Datum result= getUnits().subtract( getValue(), a, units );        
        return result; 
    }
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
    
    public int compareTo( Object a ) throws IllegalArgumentException {
        if ( ! (a instanceof Datum) ) throw new IllegalArgumentException("comparable type mismatch");
        return compareTo((Datum)a);
    }
    
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
    
    public boolean isValid() {
        return (value.doubleValue()!=java.lang.Double.NaN);
    }
    
    public boolean isFinite() {
        return ( value.doubleValue()!=java.lang.Double.POSITIVE_INFINITY )
        && ( value.doubleValue()!=java.lang.Double.NEGATIVE_INFINITY );
    }
    
    public String toString() {
        if (formatter==null) {
            return units.getDatumFormatterFactory().defaultFormatter().format(this);
        } else {
            return formatter.format(this);
        }
    }
    
    public static Datum create(double value) {
        return Units.dimensionless.createDatum(value);
    }
    
    public static Datum create( double value, Units units ) {
        return units.createDatum( value );
    }
    
    public static Datum create(double value, Units units, DatumFormatter formatter) {
        Datum result= create( value, units);
        result.formatter= formatter;
        return result;
    }
    
    public static Datum create( double value, Units units, double resolution ) {
        Datum result= units.createDatum( value, resolution );
        result.formatter= units.getDatumFormatterFactory().defaultFormatter();
        return result;
    }
    
    public static Datum create( int value ) {
        return Units.dimensionless.createDatum( value );
    }
    
    public static Datum create( int value, Units units ) {
        return units.createDatum( value );
    }
    
    public DatumFormatter getFormatter() {
        return this.formatter;
    }
    
}
