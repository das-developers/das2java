package edu.uiowa.physics.pw.das.datum;

/**
 * A DatumRange is provided as a means to carry an ordered pair of Datums 
 * representing an interval.  This sort of data structure comes up often in 
 * processing, and it's useful to define once and get the operators 
 * implemented correctly.
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
        return ""+this.s1+" - "+this.s2;
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
    
}

