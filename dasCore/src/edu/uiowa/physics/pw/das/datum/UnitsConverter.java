/* File: UnitsConverter.java
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

import edu.uiowa.physics.pw.das.datum.Units;

/**
 *
 * @author  jbf
 */
public class UnitsConverter {
    
    public static final UnitsConverter IDENTITY = new UnitsConverter(1.0, 0.0);
    public static final UnitsConverter TERA = new UnitsConverter(1e12, 0.0);
    public static final UnitsConverter GIGA = new UnitsConverter(1e9, 0.0);
    public static final UnitsConverter MEGA = new UnitsConverter(1e6, 0.0);
    public static final UnitsConverter KILO = new UnitsConverter(1e3, 0.0);
    public static final UnitsConverter MILLI = new UnitsConverter(1e-3, 0.0);
    public static final UnitsConverter MICRO = new UnitsConverter(1e-6, 0.0);
    public static final UnitsConverter NANO = new UnitsConverter(1e-9, 0.0);
    public static final UnitsConverter PICO = new UnitsConverter(1e-12, 0.0);
    
    private final double offset;
    private final double scale;
    private final int hashCode;
    private UnitsConverter inverse;

    /** Creates a new instance of UnitsConverter.ScaleOffset */
    public UnitsConverter(double scale, double offset) {
        this(scale, offset, null);
    }

    private UnitsConverter(double scale, double offset, UnitsConverter inverse) {
        this.scale = scale;
        this.offset = offset;
        this.inverse = inverse;
        hashCode = computeHashCode();
    }
    
    private int computeHashCode() {
        long scaleBits = Double.doubleToLongBits(scale);
        long offsetBits = Double.doubleToLongBits(offset);
        long code = (11 * 13 * 13) + (13 * scaleBits) + offsetBits;
        int a = (int)(code >> 32);
        int b = (int)(0xFFFFFFFFL & code);
        return a + b;
    }

    public UnitsConverter getInverse() {
        if (inverse == null) {
            inverse = new UnitsConverter(1.0 / scale, -(offset / scale), this);
        }
        return inverse;
    }

    public double convert( double value ) {
        return scale * value + offset;
    }
    
    public Number convert( Number number ) {
        double value = number.doubleValue();
        value = convert(value);
        if (number instanceof Integer) {
            return new Integer((int)value);
        }
        else if (number instanceof Long) {
            return new Long((long)value);
        }
        else {
            return new Double(value);
        }
    }

    public UnitsConverter append(UnitsConverter that) {
        if (this.equals(IDENTITY)) {
            return that;
        }
        else if (that.equals(IDENTITY)) {
            return this;
        }
        else {
            double aScale = this.scale * that.scale;
            double aOffset = this.offset * that.scale + that.offset;
            return new UnitsConverter(aScale, aOffset);
        }
    }

    public String toString() {
        return getClass().getName() + "[scale=" + scale + ",offset=" + offset + "]";
    }
    
    public int hashCode() {
        return hashCode;
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof UnitsConverter)) {
            return false;
        }
        UnitsConverter that = (UnitsConverter)o;
        return this.scale == that.scale && this.offset == that.offset;
    }
    
    public static UnitsConverter getConverter(Units fromUnits, Units toUnits) {
        return Units.getConverter(fromUnits,toUnits);
    }

}
