
package org.das2.datum;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    
    private static final Logger logger= LoggerManager.getLogger("das2.datum.orbits");
    
    String sc;
    String orbit;
    Units unitPref;
    String pwd;
    String extraFormat;

    public OrbitDatumRange( String sc, String orbit ) throws ParseException {
        super( Orbits.getOrbitsFor(sc).getDatumRange(orbit).min(), Orbits.getOrbitsFor(sc).getDatumRange(orbit).max() );
        this.pwd="";
        this.extraFormat="";
        this.sc= sc;
        this.orbit= Orbits.trimOrbit(orbit);
        this.unitPref= this.min().getUnits();
    }
    
    public OrbitDatumRange( String sc, String orbit, Units unitPref ) throws ParseException {
        super( Orbits.getOrbitsFor(sc).getDatumRange(orbit).min().convertTo(unitPref), Orbits.getOrbitsFor(sc).getDatumRange(orbit).max().convertTo(unitPref) );
        this.pwd="";
        this.extraFormat="";
        this.sc= sc;
        this.orbit= Orbits.trimOrbit(orbit);
        this.unitPref= unitPref;
    }

    /**
     * allow Orbit
     * @param pwd the absolute location (e.g. file:/home/jbf/tmp/)
     * @param sc the relative location of the spacecraft id.
     * @param orbit the orbit number
     * @param unitPref the preferred units for the range, like us2000 vs cdfTT2000.
     * @return an OrbitDatumRange with pwd set
     * @throws ParseException 
     */
    private static OrbitDatumRange createRelative( String pwd, String sc, String orbit, Units unitPref ) throws ParseException {
        OrbitDatumRange result= new OrbitDatumRange( pwd + sc, orbit, unitPref );
        result.pwd=pwd; 
        return result;
    }
    
    /**
     * return a special OrbitDatumRange which contains metadata which will allow it to be represented as %{PWD}orbit.dat,
     * so that .vap files can use resources relative to the .vap.These should not be used outside of this context,
     * but doing so is harmless.
     * @param pwd the absolute position
     * @param odr the OrbitDatumRange to be relativized.
     * @param extraFormat extra directory that will be used when saved.
     * @return either the original OrbitDatumRange, or a new relative one, which will serialize to "orbit:%{PWD}orbit.dat".
     * @throws IOException 
     */
    public static OrbitDatumRange maybeRelativize(String pwd, OrbitDatumRange odr, String extraFormat) throws IOException {
        pwd= new File(pwd).getCanonicalPath()+"/"; //TODO: Sorry Windows.
        String sc= odr.getSpacecraft();
        if ( sc.startsWith("file:") ) {
            File f1= new File( sc.substring(5) ).getCanonicalFile();
            if ( f1.getCanonicalPath().startsWith(pwd) ) {
                try {
                    String scr= f1.getCanonicalPath().substring(pwd.length());
                    OrbitDatumRange rel= createRelative( "file:" + pwd, scr, odr.getOrbit(), odr.getUnits() );
                    rel.setCheat(extraFormat);
                    return rel;
                } catch (ParseException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        } else {
            logger.log(Level.INFO, "fail to relativize: {0} {1}", new Object[]{pwd, sc});
        }
        return odr;   
    }
    
    
    public static OrbitDatumRange maybeRelativize(String pwd, OrbitDatumRange odr) throws IOException {
        return maybeRelativize(pwd,odr,"");
    }
    
    /**
     * add subdirectory to %{PWD} when formatting, so we can embed the orbits file in a zip file under a "cheat" directory.
     * Though this is public, it should not be used unless you thoroughly understand why this was done.
     * @param cheat 
     */
    public void setCheat( String cheat ) {
        if ( cheat.length()>0 ) {
            if ( !( cheat.endsWith("/") || cheat.endsWith("\\") ) ) {
                throw new IllegalArgumentException("extra cheat directory must end with slash or backslash");
            }
        }
        this.extraFormat= cheat;
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

    /**
     * return the spacecraft for the orbits, which can be a reference to a 3-column file.
     * @return the spacecraft id.
     */
    public String getSpacecraft() {
        return this.sc;
    }
    
    @Override
    public String toString() {
        if ( this.pwd.length()>0 ) {
            if ( sc.startsWith(this.pwd) ) {
                return "orbit:%{PWD}"+ extraFormat + sc.substring(this.pwd.length())+":"+orbit;
            }
            return "orbit:"+sc+":"+orbit;
        } else {
            return "orbit:"+sc+":"+orbit;
        }
    }

}
