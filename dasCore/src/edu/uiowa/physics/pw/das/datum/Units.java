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

import edu.uiowa.physics.pw.das.datum.TimeLocationUnits;

import java.util.Enumeration;
import java.util.Hashtable;
/**
 *
 * @author  jbf
 */
public class Units {
    
    String id;
    String description;
    
    private static Hashtable conversionTable;
    
    private static Hashtable getConversionTable() {
        if ( conversionTable==null ) {
            conversionTable= buildConversionTable();
        }
        return conversionTable;
    }
    
    public static void dumpConversionTable() {
        Hashtable conversionTable= getConversionTable();
        Enumeration e= conversionTable.elements();
        while (e.hasMoreElements()) {
            Enumeration e2= ((Hashtable)e.nextElement()).elements();
            while (e2.hasMoreElements()) {
                edu.uiowa.physics.pw.das.util.DasDie.println(e2.nextElement());
            }
        }
    }
    
    private static Hashtable buildConversionTable() {
        Hashtable conversionTable= new Hashtable();
        Hashtable t2000= new Hashtable();
        t2000.put(Units.us2000,new UnitsConverter(1e6,0.0));
        t2000.put(Units.mj1958,new UnitsConverter(1./86400.,15340.));
        Hashtable us2000= new Hashtable();
        us2000.put(Units.mj1958,new UnitsConverter(1./86400000000.,15340.,"us2000->mj1958"));
        us2000.put(Units.t2000,new UnitsConverter(1e-6,0.0,"us2000->t2000"));
        Hashtable seconds= new Hashtable();
        seconds.put(Units.microseconds, new UnitsConverter(1e6,0.0,"seconds->microseconds"));
        seconds.put(Units.days, new UnitsConverter(1./86400,0.0));
        Hashtable days= new Hashtable();
        days.put(Units.microseconds, new UnitsConverter(1e6*86400,0.0,"days->microseconds"));
        Hashtable celcius= new Hashtable();
        celcius.put(Units.fahrenheit, (new UnitsConverter(9./5,32)));
        conversionTable.put(Units.t2000,t2000);
        conversionTable.put(Units.us2000,us2000);
        conversionTable.put(Units.celcius,celcius);
        conversionTable.put(Units.seconds,seconds);
        conversionTable.put(Units.days,days);
        return conversionTable;
    }
    
    
    private Units(String id) {
        this.id= id;
        this.description="";
    }
    
    public Units(String id, String description) {
        this.id= id;
        this.description= description;
    }
    
    public static Units dimensionless= new Units("");

    public static Units celcius= new Units("deg C");
    public static Units fahrenheit= new Units("deg F");

    public static Units seconds= new Units("s");
    public static Units microseconds= new Units("microseconds");
    public static Units days= new Units("days");

    public static TimeLocationUnits t2000= new TimeLocationUnits("t2000","Seconds since midnight Jan 1, 2000.",Units.seconds);
    public static TimeLocationUnits us2000= new TimeLocationUnits("us2000", "Microseconds since midnight Jan 1, 2000.",Units.microseconds);
    public static TimeLocationUnits t1970= new TimeLocationUnits("t1970","Seconds since midnight Jan 1, 1970",Units.seconds);
    public static TimeLocationUnits mj1958= new TimeLocationUnits("mj1958","Julian - 2436204.5", Units.days);
    
    public static UnitsConverter getConverter( Units fromUnits, Units toUnits ) {
        if ( toUnits==fromUnits ) return UnitsConverter.identity;
        Hashtable conversionTable= getConversionTable();
        if ( conversionTable.containsKey(fromUnits) ) {
            Hashtable conversionsTo= (Hashtable)conversionTable.get(fromUnits);
            if ( !conversionsTo.containsKey(toUnits) ) {
               throw new IllegalArgumentException("Can't convert from "+fromUnits.toString()+" to "+toUnits.toString());
            } else {
                return (UnitsConverter)conversionsTo.get(toUnits);
            }
        } else if ( conversionTable.containsKey(toUnits) ) {
            Hashtable conversionsFrom= (Hashtable)conversionTable.get(toUnits);
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
    
    public String toString() {
        return id;
    }
    
    public double parse(String s) {
        throw new IllegalArgumentException("not implemented for Units besides TimeLocationUnits.");
    }
    
    public String format(double d) {
        throw new IllegalArgumentException("not implemented for Units besides TimeLocationUnits.");
    }
    
    public static void main( String[] args ) {
        dumpConversionTable();
        
        UnitsConverter uc= Units.t2000.getConverter(Units.us2000);
        int nn=50000000;        
        
        double varient=0.;
               
        long t1= System.currentTimeMillis();
        for (int i=0; i<nn; i++) {            
            varient+= uc.convert(i);
        }
        System.out.println(System.currentTimeMillis()-t1);
        System.out.println(varient);
        varient=0;
        
        t1= System.currentTimeMillis();
        
        double scale= uc.scale;
        double offset= uc.offset;
        for (int i=0; i<nn; i++) {
            varient+= scale * i + offset;
        }
        System.out.println(System.currentTimeMillis()-t1);
        System.out.println(varient);
        
        
    }
    
}
