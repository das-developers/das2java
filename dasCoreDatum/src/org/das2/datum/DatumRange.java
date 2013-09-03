package org.das2.datum;

import java.io.Serializable;

/**
 * A DatumRange is provided as a means to carry an ordered pair of Datums
 * representing an interval.  This sort of data structure comes up often in
 * processing, and it's useful to define once and get the operators
 * implemented correctly.  Consider using this object whenever you see
 * pairs of Datums in interfaces and codes (e.g. tbegin,tend), they are probably
 * a DatumRange!
 */

public class DatumRange implements Comparable, Serializable {
    
    Datum s1;
    Datum s2;
    
    /**
     * Creates valid DatumRange from two Datums.
     * @param s1 the start or smaller value of the range.
     * @param s2 the stop or bigger value of the range.
     */
    public DatumRange(Datum s1, Datum s2) {
        if ( s2.lt(s1) ) {
            throw new IllegalArgumentException( "min > max: "+s1+" > "+s2 + " width=" + ( DatumUtil.asOrderOneUnits(s2.subtract(s1)) ) ) ;
        }
        if ( s1.isFinite() && s1.isFill() ) {
            //throw new IllegalArgumentException( "s1 is fill" ) ;
        }
        if ( s2.isFinite() && s2.isFill() ) {
            //throw new IllegalArgumentException( "s2 is fill" ) ;
        }
        this.s1=s1;
        this.s2=s2.convertTo(s1.getUnits());
    }
    
    /**
     * create a datum range from two doubles in the context of units.
     * @param s1 the start or smaller value of the range.
     * @param s2 the stop or bigger value of the range.
     * @param units the units in which to interpret s1 and s2.
     */
    public DatumRange( double s1, double s2, Units units ) {
        this( Datum.create( s1, units ), Datum.create( s2, units ) );
    }
    
    /**
     * returns true of the DatumRange overlaps this.  Note that the endpoints are not
     * included in the comparison, so that Tuesday.intersects(Wednesday)==false.
     * @param dr a valid datum range
     * @return true of the DatumRange overlaps this
     * @throws InconvertibleUnitsException if the units can not be reconciled.
     */
    public boolean intersects(DatumRange dr) {
        return this.s2.gt( dr.s1 ) && this.s1.lt( dr.s2 );
    }
    

    /**
     * returns the intersection of the two intersecting ranges.  This is a range that contains(d) if 
     * and only if this.contains(d) and dr.contains(d).
     * @return the intersection of the two intersecting ranges.
     * @param dr a valid datum range.
     * @throws IllegalArgumentException if the two do not overlap.
     * @see DatumRangeUtil.sloppyIntersection
     */
    public DatumRange intersection( DatumRange dr ) {
        if ( this.intersects(dr) ) {
            Units units= this.getUnits();
            double s11= this.s1.doubleValue(units);
            double s12= dr.s1.doubleValue(units);
            double s1=  Math.max( s11, s12 );
            double s21= this.s2.doubleValue(units);
            double s22= dr.s2.doubleValue(units);
            double s2= Math.min( s21, s22 );
            return new DatumRange( s1, s2, units );
        } else {
            throw new IllegalArgumentException("does not intersect: "+dr);
        }
    }
    
    /**
     * returns true of @param dr is contained by this, so that this.union(dr).equals(this).
     * @param dr
     * @ return true iff this.union(dr).equals(this);
     */
    public boolean contains(DatumRange dr) {
        return this.s1.le( dr.s1 ) && dr.s2.le( this.s2 );
    }
    
    /**
     * returns the union of the two intersecting or adjacent ranges.
     * @throws IllegalArgumentException if the two are disjoint.
     * @param dr
     * @ return DatumRange union of the two DatumRanges
     */
    public DatumRange union( DatumRange dr ) {
        if ( this.intersects(dr) ||
                this.max().equals(dr.min() ) ||
                this.min().equals(dr.max() ) ) {
            Units units= this.getUnits();
            double s11= this.s1.doubleValue(units);
            double s12= dr.s1.doubleValue(units);
            double s21= this.s2.doubleValue(units);
            double s22= dr.s2.doubleValue(units);
            return new DatumRange( Math.min( s11, s12 ), Math.max( s21, s22 ), units );
        } else {
            throw new IllegalArgumentException("does not intersect or touch: "+dr);
        }
    }
    
    /**
     * returns true if the Datum is in the range, inclusive of the
     * lesser point but exclusive of the greater point.
     * @param d
     * @return true if d is in the range, exclusive of max.
     */
    public boolean contains( Datum d ) {
        return this.s1.le(d) && d.lt(this.s2);
    }
    
    /**
     * returns the width of the range, which is simply the greater minus the lessor.
     * Note that the units of the result will not necessarily be the same as the
     * endpoints, for example with LocationDatums.
     * @return Datum that is the width of the range (max.subtract(min)).
     */
    public Datum width() {
        return this.s2.subtract(this.s1);
    }
    
    /**
     * returns a human consumable representation of the string.  This should also be parsable with 
     * DatumRangeUtil.parseDatumRange, but this has not been verified to complete certainty.
     * @return the DatumRange as a String.
     */
    public String toString() {
        if ( this.s1.getUnits() instanceof TimeLocationUnits ) {
            try {
                return DatumRangeUtil.formatTimeRange(this);
            } catch ( IllegalArgumentException ex ) {
                return String.format( "%f %f \"%s\"", this.s1.doubleValue(this.getUnits()), this.s2.doubleValue(this.getUnits()), this.getUnits() );
            }
        } else {
            Units u= getUnits();
            return ""+ this.s1.getFormatter().format(this.s1,u) + " to " + this.s1.getFormatter().format(this.s2,u) + " " + u;
        }
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + (this.s1 != null ? this.s1.hashCode() : 0);
        hash = 67 * hash + (this.s2 != null ? this.s2.hashCode() : 0);
        return hash;
    }
    
    /**
     * returns true if the two endpoints are equal.
     * @param o
     * @return
     */
    public boolean equals( Object o ) {
        if (!(o instanceof DatumRange)) {
            return false;
        }
        if ( !this.getUnits().isConvertableTo( ((DatumRange)o).getUnits() ) ) {
            return false;
        } else {
            return ( this==o ) || 0==compareTo(o);
        }
    }
    
    /**
     * Ordered by the lesser point, then the greater point.  This is mostly provided for .equals, but also so there is a
     * consistent ordering convention thoughout the system.
     * @param o
     * @return
     */
    public int compareTo(Object o) {
        if ( o == null) {
            throw new NullPointerException("Cannot compare to null");
        }
        if ( !(o instanceof DatumRange) ) {
            throw new IllegalArgumentException("argument is not a DatumRange");
        }
        DatumRange dr= (DatumRange)o;
        int comp= s1.compareTo(dr.s1);
        if ( comp!=0 ) {
            return comp;
        } else {
            return s2.compareTo(dr.s2);
        }
    }
    
    /**
     * returns a scaled DatumRange, with a new width that is the this
     * datumRange's width multiplied by factor, and the same center.
     * 1.0 is the same range, 2.0 has twice the width, etc.
     * @param factor double representing the new range's width divided by this range's width.
     * @return a scaled DatumRange, with a new width that is the this
     * datumRange's width multiplied by factor, and the same center.
     */
    public DatumRange zoomOut( double factor ) {
        double add= (factor-1)/2;
        return rescale( -add, 1+add );
    }
    
    /**
     * returns DatumRange relative to this, where 0.0 is the minimum, and 1.0 is the maximum.
     * For example rescale(1,2) is scanNext, rescale(0.5,1.5) is zoomOut.
     * @param min the new min normalized with respect to this range.  0.0 is this range's min, 1.0 is this range's max
     * min-width.
     * @param max the new max with normalized with respect to this range.  0.0 is this range's min, 1.0 is this range's max
     * min-width.
     * @return new DatumRange.
     * @deprecated.  Use DatumRangeUtil.rescale
     */
    public DatumRange rescale( double min, double max ) {
        Datum w= width();
        if ( !w.isFinite() ) {
            throw new RuntimeException("width is not finite");
        }
        if ( w.doubleValue( w.getUnits() )==0. ) {
            // condition that might cause an infinate loop!  For now let's check for this and throw RuntimeException.
            throw new RuntimeException("width is zero!");
        }
        return new DatumRange( s1.add( w.multiply(min) ), s1.add( w.multiply(max) ) );
    }
    
    /**
     * returns the position within this, where 0. is the min(), and 1. is the max()
     * @param d a datum to normalize with respect to the range.
     * @return a double indicating the normalized datum.
     * @deprecated Use DatumRangeUtil.normalize
     */
    public double normalize( Datum d ) {
        return d.subtract(s1).divide(width()).doubleValue(Units.dimensionless);
    }
    
    /**
     * returns the smaller value or start of the range.
     * @return the smaller value or start of the range.
     */
    public Datum min() {
        return s1;
    }
    
    /**
     * returns the bigger value or stop of the range.
     * @return the bigger value or stop of the range.
     */
    public Datum max() {
        return s2;
    }
    
    /**
     * returns the next DatumRange covering the space defined by Units.  Typically,
     * this will be a range with a min equal to this datum's max, and the same width.
     * Some implementations of DatumRange may return a range with a different width
     * than this DatumRange's width, for example, when advancing month-by-month
     * with a MonthDatumRange.
     * @return the next DatumRange covering the space defined by Units. 
     */
    public DatumRange next() {
        return rescale(1,2);
    }
    
    /**
     * returns the previous DatumRange covering the space defined by Units.  See
     * next().
     * @return the previous DatumRange covering the space defined by Units
     */
    public DatumRange previous() {
        return rescale(-1,0);
    }
    
    /**
     * return a new DatumRange that includes the given Datum, extending the
     * range if necessary.  For example, 
     * <pre> [0,1).include(2)->[0,2)  (note this is exclusive of 2 since it's the end).
     * [0,1).include(-1)->[-1,1).
     * [0,1).include(0.5)->[0,1]  (and returns the same DatumRange object)
     * </pre>
     * Also, including a fill Datum returns the same DatumRange as well.
     */
    public DatumRange include(Datum d) {
        if ( d.isFill() ) return this;
        if ( this.contains(d) || this.max().equals(d) ) return this;
        Datum min= ( this.min().le(d) ? this.min() : d );
        Datum max= ( this.max().ge(d) ? this.max() : d );
        return new DatumRange( min, max );
    }
    
    /**
     * return the units of the DatumRange.
     */
    public Units getUnits() {
        return this.s1.getUnits();
    }
    
    /**
     * creates a new DatumRange object with the range specified in the space
     * identified by units.  Note that min must be <= max.
     */
    public static DatumRange newDatumRange(double min, double max, Units units) {
        return new DatumRange( Datum.create(min,units), Datum.create(max,units) );
    }
    
}

