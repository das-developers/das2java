/* File: EnumerationUnits.java
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

import java.util.HashMap;
import edu.uiowa.physics.pw.das.datum.format.*;
import java.util.*;

/**
 *
 * @author  Owner
 */
public class EnumerationUnits extends Units {
    
    private HashMap ordinals;  // maps from ordinal to Datum.Integer
    private int highestOrdinal; // highest ordinal for each Units type
    private HashMap objects;    // maps from object to Datum.Integer
    private HashMap invObjects; // maps from Datum.Integer to object
    
    public static HashMap unitsInstances;
    
    public EnumerationUnits(String id) {
        this(id,"");
    }
    
    public EnumerationUnits(String id, String description ) {        
        super(id,description);
        highestOrdinal=0;
        ordinals= new HashMap();
        objects= new HashMap();
        invObjects= new HashMap();
    }
    
    public static Datum createDatumAndUnits( Object object ) {
        return create(object).createDatum( object );
    }
    
    public Datum createDatum( Object object ) {
        if ( objects.containsKey(object) ) {
            return (Datum)objects.get(object);
        } else {
            highestOrdinal++;
            Integer ordinal= new Integer( highestOrdinal );
            Datum result= new Datum.Double( ordinal, this );
            ordinals.put(ordinal,result);            
            invObjects.put(result,object);
            objects.put(object,result);
            return result;
        }
    }
    
    public Datum[] createDatum( Object[] object ) {
        Datum[] result= new Datum[object.length];
        for ( int i=0; i<object.length; i++ ) {
            result[i]= createDatum(object[i]);
        }
        return result;
    }
    
    public Datum createDatum( int value ) {
        Integer key= new Integer(value);
        if ( ordinals.containsKey(key) ) {
            return (Datum)ordinals.get(key);
        } else {
            throw new IllegalArgumentException("No Datum exists for this ordinal");
        }
    }            
    
    public Datum createDatum(long value) {
        return createDatum( (int)value );
    }
    
    public Datum createDatum(Number value) {
        return createDatum(value.intValue());
    }

    public Object getObject( Datum datum ) {        
        if ( invObjects.containsKey(datum) ) {
            return invObjects.get(datum);
        } else {
            throw new IllegalArgumentException( "This Datum doesn't map back to an object!  This shouldn't happen!" );
        }
    }
    
    public static EnumerationUnits create( Object o ) {
        if (unitsInstances==null) unitsInstances= new HashMap();
        Class c= o.getClass();
        if (unitsInstances.containsKey(c)) {
            return (EnumerationUnits)unitsInstances.get(c);
        } else {
            EnumerationUnits result= new EnumerationUnits(c.toString()+"Unit");
            unitsInstances.put(c,result);
            return result;
        }
    }
    
    public Datum createDatum( double d ) {
        return createDatum((int)d);
    }    
    
    public DatumFormatterFactory getDatumFormatterFactory() {
        return EnumerationDatumFormatterFactory.getInstance();
    }            
    
    public Datum subtract(Number a, Number b, Units bUnits) {
        throw new IllegalArgumentException("subtract on EnumerationUnit");
    }
    
    public Datum add(Number a, Number b, Units bUnits) {
        throw new IllegalArgumentException("add on EnumerationUnit");
    }
    
    public Datum divide(Number a, Number b, Units bUnits) {
        throw new IllegalArgumentException("divide on EnumerationUnit");
    }
    
    public Datum multiply(Number a, Number b, Units bUnits) {
        throw new IllegalArgumentException("multiply on EnumerationUnit");
    }
    
    public Datum parse(String s) throws java.text.ParseException {
        Datum result=null;
        for ( Iterator i= objects.keySet().iterator(); i.hasNext(); ) {
            Object key= objects.get(i.next());
            if ( key.toString().equals(s) ) {
                if ( result==null ) {
                    result= (Datum)objects.get(key);
                } else {
                    throw new IllegalStateException("Multiple Objects' string representations match");
                }
            }
        }
        if ( result==null ) {
            throw new java.text.ParseException("no objects match \""+s+"\"",0);
        }
        return result;
    }    
            
}
