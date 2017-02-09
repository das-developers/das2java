/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.datum;

import java.io.Serializable;
import java.util.IdentityHashMap;

/**
 * Model a basis vector that defines a dimension.  For example, Units.us2000 has the
 * getOffsetUnits() &rarr; Units.microseconds  and getBasis()&rarr; "since 2000-01-01T00:00".
 * 
 * @author jbf
 */
public class Basis implements Serializable {
    
    public static final Basis fahrenheit= new Basis( "fahrenheit", "fahrenheit", Basis.physicalZero, 255.370, Units.celciusDegrees ); // findbugs IC_INIT_CIRCULARITY needs to be dealt with.
    public static final Basis kelvin= new Basis( "kelvin", "kelvin", Basis.physicalZero, 0, Units.celciusDegrees );
    public static final Basis centigrade= new Basis( "centigrade", "centigrade", Basis.physicalZero, 273.15, Units.celciusDegrees );

    public static final Basis since2000= new Basis( "since2000", "since 2000-01-01T00:00Z", null, 0, null );
    public static final Basis since2010= new Basis( "since2010", "since 2010-01-01T00:00Z", since2000,  315619200., Units.seconds );
    public static final Basis since1980= new Basis( "since1980", "since 1980-01-01T00:00Z", since2000, -631152000., Units.seconds );
    public static final Basis since1970= new Basis( "since1970", "since 1970-01-01T00:00Z", since2000, -938044800., Units.seconds );
    public static final Basis since1958= new Basis( "since1958", "since 1958-01-01T00:00Z", since2000, -1325376000., Units.seconds );
    public static final Basis modifiedJulian= new Basis( "modifiedJulian", "since 1858-11-17T00:00Z", since2000, 4453401600., Units.seconds );
    public static final Basis julian= new Basis( "julian", "since noon, January 1, 4713 BCE", since2000, 4453401600. + 86400*2400000.5, Units.seconds );
    public static final Basis since0000= new Basis( "since0000", "since 01-Jan-0000T00:00Z", since2000, 63113904000., Units.seconds );

    /**
     * special basis representing physical zero for all combinations of physical units.
     */
    public static final Basis physicalZero= new Basis( "", "physical zero", null, 0, null );
            
    private IdentityHashMap<Basis,Datum> bases;
    
    private String id;
    private String description;
    private Basis parent;
    
    public Basis( String id, String description, Basis parent, double d, Units offsetUnits  ) {
        this.id= id;
        this.description= description;
        this.parent= parent;
        if ( parent!=null ) {
            parent.bases.put( this, offsetUnits.createDatum(d) );
        } else {
            bases= new IdentityHashMap<Basis,Datum>();
        }
    }

    public String getId() {
        return this.id;
    }

    public String getDescription() {
        return this.description;
    }
    
    /**
     * return the location of this basis in given basis, in the given units.
     * @param basis
     * @param u
     * @return
     */
    double getOffset( Basis basis, Units u ) {
        if ( parent==null ) {
            return bases.get(basis).doubleValue(u);
        } else {
            double d0= parent.bases.get(this).doubleValue(u);
            double d1= parent.bases.get(basis).doubleValue(u);
            return d0 - d1;
        }
    }
    
    /**
     * specify offset to another basis.  Register to 
     * @param toBasis
     * @param d
     * @param u
     */
    public void registerConverter( Basis toBasis, double d, Units u ) {
        bases.put( toBasis, u.createDatum(d) );
    }
    
    
    public static void main( String[] args ) {
        Basis since2010x= new Basis( "since2010", "since 2010-01-01T00:00Z", Basis.since2000, 315619200000000.0, Units.microseconds );
        Basis since2011= new Basis( "since2011", "since 2011-01-01T00:00Z", Basis.since2000, 347155200000000.0, Units.microseconds );
        System.err.println( since2011.getOffset(since2010x, Units.days )); // logger okay
        
        System.err.println( centigrade.getOffset( fahrenheit, Units.fahrenheitDegrees ) ); // logger okay
        
    }
}
