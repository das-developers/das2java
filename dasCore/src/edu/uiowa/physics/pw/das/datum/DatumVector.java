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
    private final int length;
    
    private DatumVector(double[] array, Units units) {
        this.store = array.clone();
        this.units = units;
        this.length = array.length;
    }
    
    public Datum get(int index) {
        return Datum.create(((double[])store)[index], units);
    }
    
    public double doubleValue(int index, Units toUnits) {
        return units.convertDoubleTo(toUnits, ((double[])store)[index]);
    }
    
    public double[] toDoubleArray(Units units) {
        return toDoubleArray(null, units);
    }
    
    public double[] toDoubleArray(double[] array, Units units) {
        if (array == null || array.length < length) {
            array = new double[length];
        }
        System.arraycopy(store, 0, array, 0, length);
        return array;
    }
    
    public static DatumVector newDatumVector(double[] array, Units units) {
        return new DatumVector(array, units);
    }
    
    public int getLength() {
        return length;
    }
}
