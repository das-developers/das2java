/*
 * MonthDatumRange.java
 *
 * Created on November 15, 2004, 4:28 PM
 */

package org.das2.datum;

/**
 * DatumRange implementation that identifies times by orbit number.  Note orbit numbers are strings, see Cassini for why.
 * next will return the same one at the end of the sequence.
 *   dr= new OrbitDatumRange( "crres", "6" )
 *   dr.toString() -> "orbit:crres:6"
 *   dr= dr.next()
 *   dr.toString() -> "orbit:crres:7"
 * @author  Jeremy
 */
public class OrbitDatumRange extends DatumRange {
    
    String sc;
    String orbit;

    public OrbitDatumRange( String sc, String orbit ) {
        super( Orbits.getOrbitsFor(sc).getDatumRange(orbit).min(), Orbits.getOrbitsFor(sc).getDatumRange(orbit).max() );
        this.sc= sc;
        this.orbit= orbit;
    }
    
    public DatumRange next() {
        String next= Orbits.getOrbitsFor(sc).next(this.orbit);
        if ( next==null ) {
            return this;
        } else {
            return new OrbitDatumRange( sc, next );
        }
    }
    
    public DatumRange previous() {
        String prev= Orbits.getOrbitsFor(sc).prev(this.orbit);
        if ( prev==null ) {
            return this;
        } else {
            return new OrbitDatumRange( sc, prev );
        }
    }

    @Override
    public String toString() {
        return "orbit:"+sc+":"+orbit;
    }

}
