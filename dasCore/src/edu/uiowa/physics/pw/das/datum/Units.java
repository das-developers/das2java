/* File: Units.java
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

package edu.uiowa.physics.pw.das.datum;

import edu.uiowa.physics.pw.das.datum.format.*;

import java.util.*;
import java.text.ParseException;
/**
 *
 * @author  jbf
 */
public abstract class Units {
    
    String id;
    String description;
    
    protected Units( String id ) {
        this( id, "" );
    };
    
    protected Units( String id, String description ) {
        this.id= id;
        this.description= description;
    };
    
    private static HashMap conversionTable;
    
    private static HashMap getConversionTable() {
        if ( conversionTable==null ) {
            conversionTable= buildConversionTable();
        }
        return conversionTable;
    }
    
    public static void dumpConversionTable() {
        HashMap conversionTable= getConversionTable();
        
        for ( Iterator i= conversionTable.values().iterator(); i.hasNext(); ) {
            for ( Iterator j= ((HashMap)i.next()).values().iterator(); j.hasNext(); ) {
                edu.uiowa.physics.pw.das.util.DasDie.println(j.next());
            }
        }
    }
    
    private static HashMap buildConversionTable() {
        HashMap conversionTable= new HashMap();
        
        HashMap t2000= new HashMap();
        t2000.put(Units.us2000,new UnitsConverter(1e6,0.0));
        t2000.put(Units.mj1958,new UnitsConverter(1./86400.,15340.));
        conversionTable.put(Units.t2000,t2000);
        
        HashMap us2000= new HashMap();
        us2000.put(Units.mj1958,new UnitsConverter(1./86400000000.,15340.,"us2000->mj1958"));
        us2000.put(Units.t2000,new UnitsConverter(1e-6,0.0,"us2000->t2000"));
        conversionTable.put(Units.us2000,us2000);
        
        HashMap seconds= new HashMap();
        seconds.put(Units.microseconds, new UnitsConverter(1e6,0.0,"seconds->microseconds"));
        seconds.put(Units.days, new UnitsConverter(1./86400,0.0));
        conversionTable.put(Units.seconds,seconds);
        
        HashMap days= new HashMap();
        days.put(Units.microseconds, new UnitsConverter(1e6*86400,0.0,"days->microseconds"));
        days.put(Units.seconds, new UnitsConverter(86400,0.0,"days->seconds"));
        conversionTable.put(Units.days,days);
        
        HashMap celcius= new HashMap();
        celcius.put(Units.fahrenheit, (new UnitsConverter(9./5,32)));
        conversionTable.put(Units.celcius,celcius);
        
        return conversionTable;
    }
    
    public static final Units dimensionless= new NumberUnits("");
    
    public static final Units celcius= new NumberUnits("deg C");
    public static final Units fahrenheit= new NumberUnits("deg F");
    
    public static final Units seconds= new NumberUnits("s");
    public static final Units microseconds= new NumberUnits("microseconds");
    public static final Units days= new NumberUnits("days");
    
    public static final TimeLocationUnits t2000= new TimeLocationUnits("t2000","Seconds since midnight Jan 1, 2000.",Units.seconds);
    public static final TimeLocationUnits us2000= new TimeLocationUnits("us2000", "Microseconds since midnight Jan 1, 2000.",Units.microseconds);
    public static final TimeLocationUnits t1970= new TimeLocationUnits("t1970","Seconds since midnight Jan 1, 1970",Units.seconds);
    public static final TimeLocationUnits mj1958= new TimeLocationUnits("mj1958","Julian - 2436204.5", Units.days);
    
    public static final EnumerationUnits spacecraft= new EnumerationUnits( "spacecraft", "Enumeration of various spacecraft" );
    
    /*
     * Units.percent might be useful for specifying logarithmic spacing.  Define percent space between two numbers a, b where a<b
     * as ( b-a )*100. / a.  So { 1,2,4,8 } has a spacing of 100%.  
     */
    public static final Units percent= new NumberUnits("%","Special dimensionless number, possibly useful for specifying Tag relevance on logarithmic scale");
    
    static { 
      Units.spacecraft.createDatum( "Voyager1" );
      Units.spacecraft.createDatum( "Voyager2" );
      Units.spacecraft.createDatum( "Cluster Rumba" );
      Units.spacecraft.createDatum( "Cluster Salsa" );
      Units.spacecraft.createDatum( "Cluster Samba" );
      Units.spacecraft.createDatum( "Cluster Tango" );
    }
      
    public static UnitsConverter getConverter( Units fromUnits, Units toUnits ) {
        if ( toUnits==fromUnits ) return UnitsConverter.identity;
        HashMap conversionTable= getConversionTable();
        HashMap conversionsFrom1= (HashMap)conversionTable.get(toUnits);
        HashMap conversionsTo1= (HashMap)conversionTable.get(fromUnits);
        if ( conversionTable.containsKey(fromUnits) &&
        ((HashMap)conversionTable.get(fromUnits)).containsKey(toUnits) ) {
            HashMap conversionsTo= (HashMap)conversionTable.get(fromUnits);
            return (UnitsConverter)conversionsTo.get(toUnits);
        } else if ( conversionTable.containsKey(toUnits) &&
        ((HashMap)conversionTable.get(toUnits)).containsKey(fromUnits) ) {
            HashMap conversionsFrom= (HashMap)conversionTable.get(toUnits);
            if ( !conversionsFrom.containsKey(fromUnits) ) {
                throw new IllegalArgumentException("Can't convert from "+fromUnits.toString()+" to "+toUnits.toString());
            } else {
                return ((UnitsConverter)conversionsFrom.get(fromUnits)).getInversion();
            }
        } else {
            throw new IllegalArgumentException("Can't convert from "+fromUnits.toString()+" to "+toUnits.toString());
        }
    }
    
    public UnitsConverter getConverter( Units toUnits ) {
        return getConverter( this, toUnits );
    }
    
    public double convertDoubleTo( Units toUnits, double value ) {
        if ( this==toUnits ) {
            return value;
        } else {
            return getConverter(this,toUnits).convert(value);
        }
    }
    
    public String toString() {
        return id;
    }
    
    public Units getOffsetUnits() {
        return this;
    }
    
    public abstract Datum createDatum( double value );    
    public abstract Datum createDatum( int value );    
    public abstract Datum createDatum( long value );    
    public abstract Datum createDatum( Number value );
    public abstract Datum getFill();
    public boolean isFill( double value ) {
        return getFill().equals(createDatum(value));
    }
    public boolean isFill( long value ) {
        return getFill().equals(createDatum(value));
    }
    public boolean isFill( int value ) {
        return getFill().equals(createDatum(value));
    }
    public boolean isFill( Number value ) {
        return getFill().equals(createDatum(value));
    }
    
    public abstract DatumFormatterFactory getDatumFormatterFactory();
    
    public abstract Datum parse(String s) throws ParseException;
    public String format( Datum datum ) {
        return getDatumFormatterFactory().defaultFormatter().format(datum);
    }
    public String grannyFormat( Datum datum ) {
        return getDatumFormatterFactory().defaultFormatter().grannyFormat(datum);
    }
    
    public abstract Datum add( Number a, Number b, Units bUnits );
    public abstract Datum subtract( Number a, Number b, Units bUnits );
    public abstract Datum multiply( Number a, Number b, Units bUnits );
    public abstract Datum divide( Number a, Number b, Units bUnits );
    
    public static void main( String[] args ) {
        dumpConversionTable();
        
        System.out.println( Datum.create(1,Units.days).convertTo(Units.microseconds) );
        System.out.println( Datum.create(1,Units.days).convertTo(Units.seconds) );
        System.out.println("Test of uc.convert vs manual convesion:");
        UnitsConverter uc= Units.t2000.getConverter(Units.us2000);
        int nn=500000;
        
        double varient=0.;
        
        long t1= System.currentTimeMillis();
        for (int i=0; i<nn; i++) {
            varient+= uc.convert(i);
        }
        long x= System.currentTimeMillis()-t1;
        System.out.println("Time(ms) of uc.convert: "+x);
        varient=0;
        
        t1= System.currentTimeMillis();
        
        double scale= uc.scale;
        double offset= uc.offset;
        for (int i=0; i<nn; i++) {
            varient+= scale * i + offset;
        }
        x= System.currentTimeMillis()-t1;
        System.out.println("Time(ms) of scale, offset calculation: "+x);
        
        System.out.println( Units.spacecraft.createDatum(1) );
        System.out.println( Units.spacecraft.createDatum(5) );
    
        Datum location= TimeUtil.createValid("2010-10-31");
        Datum number= Datum.create(1,Units.days);
        
        System.out.println( location.add(number) );
        // System.out.println( number.add(location) );  // throws Exception 2003-11-19
                
        System.out.println( Units.days.createDatum(1.) );
        System.out.println( Units.days.createDatum(1.).convertTo(Units.seconds) );

        System.out.println( Units.days.getFill() );
        System.out.println( Units.days.getFill().convertTo(Units.seconds) );
    }
    
    
}
