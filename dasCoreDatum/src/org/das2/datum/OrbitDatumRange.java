
package org.das2.datum;

import java.io.Serializable;
import java.text.ParseException;

/**
 * DatumRange implementation that identifies times by orbit number.  Note orbit numbers are strings, see Cassini for why.
 * next will return the same one at the end of the sequence.
 * <code>
 *   dr= new OrbitDatumRange( "crres", "6" )
 *   dr.toString() &rarr; "orbit:crres:6"
 *   dr= dr.next()
 *   dr.toString() &rarr; "orbit:crres:7"
 * </code>
 * Also, orbit:http://das2.org/wiki/index.php/Orbits/crres:6 is supported for development work and personal lists.
 * @author  Jeremy
 */
public class OrbitDatumRange extends DatumRange implements Serializable {
    
    String sc;
    String orbit;
    Units unitPref;

    public OrbitDatumRange( String sc, String orbit ) throws ParseException {
        super( Orbits.getOrbitsFor(sc).getDatumRange(orbit).min(), Orbits.getOrbitsFor(sc).getDatumRange(orbit).max() );
        this.sc= sc;
        this.orbit= Orbits.trimOrbit(orbit);
        this.unitPref= this.min().getUnits();
    }
    
    public OrbitDatumRange( String sc, String orbit, Units unitPref ) throws ParseException {
        super( Orbits.getOrbitsFor(sc).getDatumRange(orbit).min().convertTo(unitPref), Orbits.getOrbitsFor(sc).getDatumRange(orbit).max().convertTo(unitPref) );
        this.sc= sc;
        this.orbit= Orbits.trimOrbit(orbit);
        this.unitPref= unitPref;
    }
        
    @Override
    public DatumRange next() {
        String next= Orbits.getOrbitsFor(sc).next(this.orbit);
        if ( next==null ) {
            return this;
        } else {
            try {
                return new OrbitDatumRange( sc, next, unitPref );
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
    public DatumRange convertTo( Units u ) {
        if ( u==min().getUnits() ) {
            return this;
        } else {
            try {
                return new OrbitDatumRange( sc, orbit, u );
            } catch (ParseException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public int compareTo(Object o) {
        if ( o instanceof OrbitDatumRange ) {
            OrbitDatumRange odr= ((OrbitDatumRange)o);
            if ( odr.sc.equals(this.sc) ) {
                if ( odr.orbit.equals(this.orbit) ) {
                    return 0;
                } else {
                    return Orbits.getOrbitsFor(sc).compare( this.orbit, odr.orbit );
                }
            } else {
                return super.compareTo(o); // simple time range compare
            }
        } else {
            return super.compareTo(o);
        }
    }

    @Override
    public boolean equals(Object o) {
        if ( o instanceof OrbitDatumRange ) {
            OrbitDatumRange odr= ((OrbitDatumRange)o);
            if ( odr.sc.equals(this.sc) ) {
                if ( odr.orbit.equals(this.orbit) ) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return super.equals(o); // simple time range compare
            }
        } else {
            return super.equals(o);
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + (this.sc != null ? this.sc.hashCode() : 0);
        hash = 71 * hash + (this.orbit != null ? this.orbit.hashCode() : 0);
        return hash;
    }

    /**
     * return the string identifying this orbit, assuming the context is 
     * provided elsewhere.  For example, supposing this toString() is "orbit:rbspa-pp:43"
     * then this would return just "43"
     * @return the string providing the orbit.
     */
    public String getOrbit() {
        return this.orbit;
    }

    @Override
    public String toString() {
        return "orbit:"+sc+":"+orbit;
    }

}
