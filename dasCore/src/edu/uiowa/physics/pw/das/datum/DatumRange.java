package edu.uiowa.physics.pw.das.datum;

/**
 * A DatumRange is provided as a means to carry an ordered pair of Datums 
 * representing an interval.  This sort of data structure comes up often in 
 * processing, and it's useful to define once and get the operators 
 * implemented correctly.  Consider using this object whenever you see
 * pairs of Datums in interfaces and codes (e.g. tbegin,tend), they are probably
 * a DatumRange!
 */

public class DatumRange implements Comparable {
    
    Datum s1;    
    Datum s2;
    
    /**
     * Creates valid DatumRange from two Datums.
     * @param s1
     * @param s2
     * @throws IllegalArgumentException if the first Datum is greater than the second, or the units are incompatible.
     */    
    public DatumRange(Datum s1, Datum s2) {
        if ( s2.lt(s1) ) throw new IllegalArgumentException( "s2<s1: "+s2+"<"+s1 ) ;        
        this.s1=s1;
        this.s2=s2;
    } 
        
    /**
     * returns true of the DatumRange overlaps this.  Note that the endpoints are not
     * included in the comparison, so that Tuesday.intersects(Wednesday)==false.
     * @param dr
     * @return
     */    
    public boolean intersects(DatumRange dr) {        
        return this.s2.gt( dr.s1 ) && this.s1.lt( dr.s2 );
    }        
    
    /**
     * returns true if the Datum is in the range, inclusive of the
     * lesser point but exclusive of the greater point.
     * @param d
     * @return
     */    
    public boolean contains( Datum d ) {
        return this.s1.le(d) && d.lt(this.s2);
    }
    
    /**
     * returns the width of the range, which is simply the greater minus the lessor.
     * Note that the units of the result will not necessarily be the same as the
     * endpoints, for example with LocationDatums.
     * @return
     */    
    public Datum width() {
        return this.s2.subtract(this.s1);
    }
    
    /**
     *
     * @return the DatumRange as a String.
     */    
    public String toString() {
        if ( this.s1.getUnits() instanceof TimeLocationUnits ) {
            return DatumRangeUtil.formatTimeRange(this);
        } else {
            return ""+this.s1+" to "+this.s2;
        }
    }
    
    
    /**
     * returns true if the two endpoints are equal.
     * @param o
     * @return
     */    
    public boolean equals( Object o ) {
        return ( this==o ) || 0==compareTo(o);
    }
    
    /**
     * Ordered by the lesser point, then the greater point.  This is mostly provided for .equals, but also so there is a 
     * consistent ordering convention thoughout the system.
     * @param o
     * @return
     */    
    public int compareTo(Object o) {
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
    
    public DatumRange zoomOut( double factor ) {
        double add= (factor-1)/2;        
        return rescale( -add, 1+add );        
    }
    
    /**
     * returns DatumRange relative to this, where 0. is the minimum, and 1. is the maximum.
     * For example rescale(1,2) is scanNext, rescale(0.5,1.5) is zoomOut.
     *
     *@param min
     *@param max
     *@return new Datum
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
    
    public Datum min() {
        return s1;
    }
    
    public Datum max() {
        return s2;
    }
    
    public DatumRange next() {
        return rescale(1,2);
    }
    
    public DatumRange previous() {
        return rescale(-1,0);
    }
    
    public DatumRange include(Datum d) {   
        if ( d.isFill() ) return this;
        if ( this.contains(d) ) return this;
        Datum min= ( this.min().le(d) ? this.min() : d );
        Datum max= ( this.max().ge(d) ? this.max() : d );
        return new DatumRange( min, max );
    }
    
    public Units getUnits() {
        return this.s1.getUnits();
    }
    
    public static DatumRange newDatumRange(double min, double max, Units units) {
        return new DatumRange( Datum.create(min,units), Datum.create(max,units) );
    }
    
}

