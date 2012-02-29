/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.datum;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author jbf
 */
public class Orbits {

    private LinkedHashMap<String,DatumRange> orbits;

    private Orbits( String sc ) {
        if ( sc.equals("crres") ) {
            orbits= new LinkedHashMap<String, DatumRange>();
            orbits.put( "591", DatumRangeUtil.parseTimeRangeValid( "1991-03-25T09:49:00.000Z/1991-03-25T19:42:00.000Z" ) );
            orbits.put( "592", DatumRangeUtil.parseTimeRangeValid( "1991-03-25T19:39:00.000Z/1991-03-26T05:31:00.000Z" ) );
            orbits.put( "593", DatumRangeUtil.parseTimeRangeValid( "1991-03-26T05:28:00.000Z/1991-03-26T15:21:00.000Z" ) );
        }
    }

    public DatumRange getDatumRange( String orbit ) {
        return orbits.get(orbit);
    }

    public String next( String orbit ) {//TODO: do this efficiently
        boolean next= false;
        for ( String s: orbits.keySet() ) {
            if ( next ) return s;
            if ( s.equals(orbit) ) {
                next= true;
            }
        }
        return null;
    }
    
    public String prev( String orbit ) { //TODO: do this efficiently
        String prev= null;
        for ( String s: orbits.keySet() ) {
            if ( s.equals(orbit) ) {
                return prev;
            }
            prev= s;
        }
        return null;
    }

    public static Orbits getOrbitsFor( String sc ) {
        return new Orbits(sc);
    }

}
