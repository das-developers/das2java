/* File: DatumVector.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on February 18, 2004, 10:40 AM
 *      by Edward E. West <eew@space.physics.uiowa.edu>
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author  eew
 */
public final class DatumVector {
    
    private final Units units;
    private final Object store;
    private final double resolution;
    private final int offset;
    private final int length;
    
    /** T0DO: check offset and length for out of bounds condition */
    private DatumVector(double[] array, int offset, int length, Units units) {
        this(array, offset, length, units, 0.0, true);
    }
    
    /** T0DO: check offset and length for out of bounds condition */
    private DatumVector(double[] array, int offset, int length, Units units, double resolution, boolean copy) {
        //<editor-fold defaultstate="collapsed" desc="parameter checking">
        if (array == null) {
            throw new NullPointerException("array is null");
        }
        if (units == null) {
            throw new NullPointerException("units is null");
        }
        //</editor-fold>
        if (copy) {
            this.store = new double[length];
            for (int i = 0; i < length; i++) {
                ((double[])store)[i] = array[offset + i];
            }
            offset = 0;
        }
        else {
            this.store = array;
        }
        this.offset = offset;
        this.units = units;
        this.resolution= resolution;
        this.length = length;
    }
    
    /** 
     * return a subset of a vector
     * @param start the start index
     * @param end the end index, exclusive.
     * @return the DatumVector
     */
    public DatumVector getSubVector(int start, int end) {
        if (start == 0 && end == length) {
            return this;
        }        
        if ( start<0 ) throw new IllegalArgumentException("start less than 0");
        if ( end>length ) {
            throw new IllegalArgumentException("end greater than length");
        }
        return new DatumVector((double[])store, offset + start, end - start, units, resolution, false);
    }
    
    public Datum get(int index) {
        return Datum.create( ((double[])store)[index + offset], units, resolution );
    }
    
    public Units getUnits() {
        return this.units;
    }
    
    public double doubleValue(int index, Units toUnits) {
        return units.convertDoubleTo(toUnits, ((double[])store)[index + offset]);
    }
    
    public double[] toDoubleArray(Units units) {
        return toDoubleArray(null, units);
    }
    
    public double[] toDoubleArray(double[] array, Units units) {
        if (array == null || array.length < length) {
            array = new double[length];
        }
        if (units == this.units) {
            System.arraycopy(store, offset, array, 0, length);
        }
        else {
            double[] store = (double[])this.store;
            for (int i = 0; i < length; i++) {
                array[i] = this.units.convertDoubleTo(units, store[i]);
            }
        }
        return array;
    }
    
    public static DatumVector newDatumVector(Datum[] array, Units units) {
        double[] dArray = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            dArray[i] = array[i].doubleValue(units);
        }
        return newDatumVector(dArray, units);
    }
    
    public static DatumVector newDatumVector(double[] array, Units units) {
        return newDatumVector(array, 0, array.length, units);
    }

    public static DatumVector newDatumVector(double[] array, double resolution, Units units) {
        return new DatumVector( array, 0, array.length, units, resolution, true );
    }

    public static DatumVector newDatumVector(double[] array, int offset, int length, Units units) {
        return new DatumVector(array, offset, length, units);
    }

    public int getLength() {
        return length;
    }
    
    public DatumVector add( Datum d ) {
        double[] dd= new double[getLength()];
        Units newUnits;
        if ( d.getUnits() instanceof LocationUnits ) {
            newUnits= d.getUnits();
            for ( int i=0; i<dd.length; i++ ) {
                dd[i]= d.add(get(i)).doubleValue(newUnits);
            }        
        } else {
            newUnits= units;            
            for ( int i=0; i<dd.length; i++ ) {
                dd[i]= get(i).add(d).doubleValue(newUnits);
            }
        }
        return new DatumVector( dd, 0, dd.length, newUnits );
    }
    
    /**
     * return a DatumVector which contains the elements of a and then 
     * the elements of b.
     * @param a null or a DatumVector
     * @param b a DatumVector with compatible units.
     * @return the DatumVector which contains the elements of a and then 
     * the elements of b.
     */
    public static DatumVector append( DatumVector a, DatumVector b ) {
        if ( a==null ) return b;
        Units u= a.getUnits();
        double[] ra= new double[ a.length + b.length ];
        for ( int i=0; i<a.length; i++ ) {
            ra[i]= a.doubleValue( i, u );
        }
        for ( int i=0; i<b.length; i++ ) {
            ra[i+a.length]= b.doubleValue( i, u );
        }
        DatumVector result= new DatumVector( ra, 0, ra.length, u );
        return result;
    }
    
    public DatumVector subtract( Datum d ) {
        double[] dd= new double[getLength()];
        Units newUnits;
        if ( units instanceof LocationUnits && d.getUnits() instanceof LocationUnits ) {
            newUnits= units.getOffsetUnits();
        } else {
            newUnits= units;
        }
        for ( int i=0; i<dd.length; i++ ) {
            Datum diff= get(i).subtract(d);
            dd[i]= diff.doubleValue(newUnits);
        }        
        return new DatumVector( dd, 0, dd.length, newUnits );
    }
    
    public DatumVector multiply( double d ) {
        double[] dd= new double[getLength()];        
        if ( units instanceof LocationUnits ) {
            throw new IllegalArgumentException("can't multiply a LocationUnits");            
        } else {            
            for ( int i=0; i<dd.length; i++ ) {
                dd[i]= get(i).multiply(d).doubleValue(units);
            }
        }
        return new DatumVector( dd, 0, dd.length, units );
    }
    
    @Override
    public String toString() {
        Units units= this.getUnits();
        StringBuilder result= new StringBuilder();
        result.append("[");
        for ( int i=0; i< Math.min( 4, getLength() ); i++ ) {
            if ( i>0 ) result.append(", ");
            Datum d= get(i);
            result.append(d.getFormatter().format(d,units));
        }
        if ( getLength()>4 ) result.append(", ... (len=").append(getLength()).append(")");
        result.append(" ").append(units.toString()).append(" ]");
        return result.toString();
    }
    
    /**
     * check that element-for-element the two are equal
     * @param a 
     * @param b
     * @return true if they are equal.
     */
    public static boolean datumVectorsEqual( DatumVector a, DatumVector b ) { 
        if ( a==null || b==null ) return false;
        if ( a.getLength()==b.getLength() ) {
            for ( int i=0; i<a.getLength(); i++ ) {
                if ( !a.get(i).equals(b.get(i)) ) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * return this DatumVector as a list of Datums.
     * @return this DatumVector as a list of Datums.
     */
    public Collection<? extends Datum> asList() {
        List<Datum> result= new ArrayList<>();
        for ( int i=0; i<getLength(); i++ ) {
            result.add( get(i) );
        }
        return result;
    }
        
}
