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
    
    private double value;
    private Units units;
    private DatumFormatter formatter;
    
    /** Creates a new instance of Datum */
    protected Datum( double value ) {
        this( value, Units.dimensionless );
    }
    
    protected Datum( double value, Units units ) {
        this(value, units, units.getDatumFormatterFactory().defaultFormatter());
    }
    
    protected Datum (double value, Units units, DatumFormatter formatter) {
        this.value = value;
        this.units = units;
        this.formatter = formatter;
    }
    
    public double doubleValue(Units units) {
        if ( units!=this.units ) { 
            return getUnits().getConverter(units).convert(this.value);
        } else {
            return this.value;
        }
    }
    
    public Units getUnits() {
        return this.units;
    }
    
    protected double getValue() {
        return this.value;
    }
    
    public Datum add( Datum a ) throws IllegalArgumentException {
        return add( a.getValue(), a.getUnits() );
    }
    
    public Datum add( double a, Units aUnits ) throws IllegalArgumentException {
        if ( units instanceof LocationUnits ) {
            if ( aUnits instanceof LocationUnits ) {
                throw new IllegalArgumentException("You can't add "+units+" to "+aUnits);
            } else {
                Units offsetUnits= ((LocationUnits)units).getOffsetUnits();
                if ( aUnits!=offsetUnits) {
                    UnitsConverter uc= Units.getConverter( aUnits, offsetUnits );
                    a= uc.convert(a);
                }
                return create( this.getValue() + a, units );
            }
        } else {
            if ( units != aUnits ) {
                UnitsConverter uc= Units.getConverter( aUnits, units );
                a= uc.convert(a);
            }
            Datum result= create( this.value + a, units );
            return result;
        }
    }
    
    public Datum subtract( double a, Units units ) throws IllegalArgumentException {
        return this.add(-1*a, units);
    }
    
    public Datum subtract( Datum a ) throws IllegalArgumentException {
        if ( units instanceof LocationUnits ) {
            LocationUnits units= (LocationUnits)getUnits();
            if ( a.getUnits() instanceof LocationUnits ) {
                if ( units != a.getUnits() ) {
                    a= a.convertTo(units);
                }
                double value= this.getValue()-a.getValue();
                return new Datum( value, units.getOffsetUnits() );
            } else {
                if ( a.getUnits()!=units.getOffsetUnits()) {
                    a= a.convertTo(units.getOffsetUnits());
                }
                return create( this.getValue()-a.getValue(), units );
            }
        } else {
            if ( this.units != a.units ) {
                a= a.convertTo(this.units);
            }
            Datum result= create( this.value - a.value, this.units );
            return result;
        }
    }
    
    public Datum divide( double a ) {
        if ( units instanceof LocationUnits ) {
            throw new IllegalArgumentException( "It doesn't make sense to divide LocationUnits, since they indicate a point in space/time" );
        } else {
            return create( this.value/a, this.units );
        }
    }
    
    public Datum multiply( double a ) {
        if ( units instanceof LocationUnits ) {
            throw new IllegalArgumentException( "It doesn't make sense to multiply LocationUnits, since they indicate a point in space/time" );
        } else {
            return create( this.value * a, this.units );
        }
    }
    
    public int hashCode() {
        long bits = Double.doubleToLongBits(value);
	int doubleHash= (int)(bits ^ (bits >>> 32));
        int unitsHash= units.hashCode();
        return doubleHash ^ unitsHash;        
    }
    
    public boolean equals( Object a ) throws IllegalArgumentException {
        return ((a instanceof Datum) && this.equals( (Datum)a ) );
    }
    
    public boolean equals( Datum a ) throws IllegalArgumentException {
        return ( a.units==this.units && a.value==this.value );
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
        double d= this.value - a.value;
        
        if (d==0.) {
            return 0;
        } else if ( d<0. ) {
            return -1;
        } else {
            return 1;
        }
    }
    
    public boolean isValid() {
        return (value!=Double.NaN);
    }
    
    public Datum convertTo( Units units ) throws IllegalArgumentException {
        UnitsConverter muc= this.units.getConverter(units);
        Datum result= create( muc.convert( this.value ), units );
        return result;
    }
    
    public String toString() {
        if (formatter==null) {
            return ""+value+" "+units;
        } else {
            return formatter.format(this);
        }
    }
    
    public static Datum create(double value) {                
        return create( value, Units.dimensionless );
    }
    
    public static Datum create( double value, Units units ) {
            return new Datum( value, units );
    }
    
    public static Datum create(double value, Units units, DatumFormatter formatter) {
        return new Datum(value, units, formatter);
    }
    
    public static void main( String[] args ) throws Exception {
        Datum temp1= new Datum( 32, Units.fahrenheit );
        Datum temp2= new Datum( 212, Units.fahrenheit );
        Datum temp3= new Datum( 100, Units.celcius );
        
        System.out.println( new Datum( 1 ).hashCode() );
        System.out.println( new Datum( 1 ).hashCode() );
        
        Units.dumpConversionTable();
        
        edu.uiowa.physics.pw.das.util.DasDie.println(""+temp1);
        edu.uiowa.physics.pw.das.util.DasDie.println(""+temp2);
        edu.uiowa.physics.pw.das.util.DasDie.println(""+temp3);
        
        edu.uiowa.physics.pw.das.util.DasDie.println(""+temp2.convertTo(Units.celcius));
        edu.uiowa.physics.pw.das.util.DasDie.println(""+temp1.convertTo(Units.celcius));
        
        edu.uiowa.physics.pw.das.util.DasDie.println("=======");
        edu.uiowa.physics.pw.das.util.DasDie.println(""+temp2.subtract(temp1));
        edu.uiowa.physics.pw.das.util.DasDie.println(""+temp3.subtract(temp1));
                
        Datum y= TimeUtil.create("09/15/1997 17:27:32");
        edu.uiowa.physics.pw.das.util.DasDie.println(y.toString());
    }
    
    public DatumFormatter getFormatter() {
        return this.formatter;
    }
    
}
