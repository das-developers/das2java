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

package edu.uiowa.physics.pw.das.datum;

/**
 *
 * @author  eew
 */
public final class DatumVector {
    
    private final Units units;
    private final Object store;
    private final int offset;
    private final int length;
    
    /** T0DO: check offset and length for out of bounds condition */
    private DatumVector(double[] array, int offset, int length, Units units) {
        this(array, offset, length, units, true);
    }
    
    /** T0DO: check offset and length for out of bounds condition */
    private DatumVector(double[] array, int offset, int length, Units units, boolean copy) {
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
        this.length = length;
    }
    
    /** T0DO: check start and end for out of bounds condition */
    public DatumVector getSubVector(int start, int end) {
        if (start == 0 && end == length) {
            return this;
        }
        else return new DatumVector((double[])store, offset + start, end - start, units, false);
    }
    
    public Datum get(int index) {
        return Datum.create(((double[])store)[index + offset], units);
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
        System.arraycopy(store, offset, array, 0, length);
        return array;
    }
    
    public static DatumVector newDatumVector(double[] array, Units units) {
        return newDatumVector(array, 0, array.length, units);
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
    
    public String toString() {
        StringBuffer result= new StringBuffer();
        result.append("[");
        for ( int i=0; i<getLength(); i++ ) {
            if ( i>0 ) result.append(", ");
            result.append(get(i).toString());
        }
        result.append("]");
        return result.toString();
    }
}
