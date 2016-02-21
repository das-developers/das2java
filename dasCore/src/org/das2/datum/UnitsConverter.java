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

package org.das2.datum;

/**
 *
 * @author  jbf
 */
public abstract class UnitsConverter {
    
    public static final UnitsConverter IDENTITY = new UnitsConverter() {
        public UnitsConverter getInverse() {
            return this;
        }
        public double convert(double value) {
            return value;
        }
        public String toString() {
            return "IDENTITY UnitsConverter";
        }
    };
    
    public static final UnitsConverter TERA = new ScaleOffset(1e-12, 0.0);
    public static final UnitsConverter GIGA = new ScaleOffset(1e-9, 0.0);
    public static final UnitsConverter MEGA = new ScaleOffset(1e-6, 0.0);
    public static final UnitsConverter KILO = new ScaleOffset(1e-3, 0.0);
    public static final UnitsConverter MILLI = new ScaleOffset(1e3, 0.0);
	public static final UnitsConverter CENTI = new ScaleOffset(1e2, 0.0);
    public static final UnitsConverter MICRO = new ScaleOffset(1e6, 0.0);
    public static final UnitsConverter NANO = new ScaleOffset(1e9, 0.0);
    public static final UnitsConverter PICO = new ScaleOffset(1e12, 0.0);
    
    protected UnitsConverter inverse;
    
    protected UnitsConverter() {
    }
    
    protected UnitsConverter(UnitsConverter inverse) {
        this.inverse = inverse;
    }
    
    public abstract UnitsConverter getInverse();

    public abstract double convert(double value);
    
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
        return new Appended(this, that);
    }

    public static class ScaleOffset extends UnitsConverter {
        private final double offset;
        private final double scale;
        private final int hashCode;
    
        /** 
         * Creates a new UnitsConverter.ScaleOffset.  This
         * converter multiplies by scale and adds offset, so
         * offset is in the target Units.  For example,
         * deg C to deg F would be 
         * <pre>new UnitsConverter.ScaleOffset( 9./5, 32 )</pre>.
         * 
         */
        public ScaleOffset(double scale, double offset) {
            this(scale, offset, null);
        }

        private ScaleOffset(double scale, double offset, UnitsConverter inverse) {
            super(inverse);
            this.scale = scale;
            this.offset = offset;
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
            if (((UnitsConverter)this).inverse == null) {
                ((UnitsConverter)this).inverse = new ScaleOffset(1.0 / scale, -(offset / scale), this);
            }
            return ((UnitsConverter)this).inverse;
        }

        public double convert( double value ) {
            return scale * value + offset;
        }

        public UnitsConverter append(UnitsConverter that) {
            if (this.equals(IDENTITY)) {
                return that;
            }
            else if (that.equals(IDENTITY)) {
                return this;
            }
            else if (that instanceof ScaleOffset) {
                ScaleOffset so = (ScaleOffset)that;
                double aScale = this.scale * so.scale;
                double aOffset = this.offset * so.scale + so.offset;
                return new ScaleOffset(aScale, aOffset);
            }
            else {
                return super.append(that);
            }
        }
        
        public boolean equals(Object o) {
            if (!(o instanceof ScaleOffset)) {
                return false;
            }
            ScaleOffset that = (ScaleOffset)o;
            return this.scale == that.scale && this.offset == that.offset;
        }

        public String toString() {
            return getClass().getName() + "[scale=" + scale + ",offset=" + offset + "]";
        }

        public int hashCode() {
            return hashCode;
        }
    
    }
    
    public static class Appended extends UnitsConverter{
        
        UnitsConverter[] converters;
        
        public Appended(UnitsConverter uc1, UnitsConverter uc2) {
            UnitsConverter[] a1 = ucToArray(uc1);
            UnitsConverter[] a2 = ucToArray(uc2);
            converters = new UnitsConverter[a1.length + a2.length];
            for (int i = 0; i < a1.length; i++) {
                converters[i] = a1[i];
            }
            for (int i = 0; i < a2.length; i++) {
                converters[i + a1.length] = a2[i];
            }
        }
        
        private Appended(UnitsConverter[] array, UnitsConverter inverse) {
            super(inverse);
            converters = array;
        }
        
        public double convert(double value) {
            for (int i = 0; i < converters.length; i++) {
                value = converters[i].convert(value);
            }
            return value;
        }
        
        public Number convert(Number value) {
            for (int i = 0; i < converters.length; i++) {
                value = converters[i].convert(value);
            }
            return value;
        }
        
        public UnitsConverter getInverse() {
            if (inverse == null) {
                int length = converters.length;
                UnitsConverter[] inverseArray = new UnitsConverter[length];
                for (int i = 0; i < length; i++) {
                    inverseArray[i] = converters[length - i - 1].getInverse();
                }
                inverse = new Appended(inverseArray, this);
            }
            return inverse;
        }
        
        private static UnitsConverter[] ucToArray(UnitsConverter uc) {
            if (uc instanceof Appended) {
                return ((Appended)uc).converters;
            }
            else {
                return new UnitsConverter[] {uc};
            }
        }
        
    }
    
    public static UnitsConverter getConverter(Units fromUnits, Units toUnits) {
        return Units.getConverter(fromUnits,toUnits);
    }

}
