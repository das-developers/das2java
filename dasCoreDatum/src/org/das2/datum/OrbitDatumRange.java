
package org.das2.datum;

import java.text.ParseException;

/**
 * DatumRange implementation that identifies times by orbit number.  Note orbit numbers are strings, see Cassini for why.
 * next will return the same one at the end of the sequence.
 *   dr= new OrbitDatumRange( "crres", "6" )
 *   dr.toString() -> "orbit:crres:6"
 *   dr= dr.next()
 *   dr.toString() -> "orbit:crres:7"
 *
 * Also, orbit:http://das2.org/wiki/index.php/Orbits/crres:6 is supported for development work and personal lists.
 * @author  Jeremy
 */
public class OrbitDatumRange extends DatumRange {
    
    String sc;
    String orbit;

    public OrbitDatumRange( String sc, String orbit ) throws ParseException {
        super( Orbits.getOrbitsFor(sc).getDatumRange(orbit).min(), Orbits.getOrbitsFor(sc).getDatumRange(orbit).max() );
        this.sc= sc;
        this.orbit= orbit;
    }
    
    @Override
    public DatumRange next() {
        String next= Orbits.getOrbitsFor(sc).next(this.orbit);
        if ( next==null ) {
            return this;
        } else {
            try {
                return new OrbitDatumRange( sc, next );
            } catch ( ParseException ex ) {
                throw new RuntimeException(ex);
            }
        }
    }
    
    @Override
    public DatumRange previous() {
        String prev= Orbits.getOrbitsFor(sc).prev(this.orbit);
        if ( prev==null ) {
            return this;
        } else {
            try {
                return new OrbitDatumRange( sc, prev );
            } catch ( ParseException ex ) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public String toString() {
        return "orbit:"+sc+":"+orbit;
    }

}
