/* File: EnumerationDatum.java
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

import edu.uiowa.physics.pw.das.datum.DasFormatter;
import edu.uiowa.physics.pw.das.datum.Datum;

import java.util.HashMap;

/**
 *
 * @author  Owner
 */
public class EnumerationDatum extends Datum {
    
    Object object;
    public static DasFormatter nf1;
    public static HashMap instances;
    public static HashMap highestOrdinal;
    public static HashMap objects;
    
    public static EnumerationDatum create( Object object ) {
        if ( getObjects().containsKey(object) ) {
            return (EnumerationDatum)getObjects().get(object);
        } else {
            return new EnumerationDatum( object, getNextOrdinal( object ), EnumerationUnits.create( object ) );
        }
    }
    
    public static EnumerationDatum[] create( Object[] objects ) {
        EnumerationDatum[] result= new EnumerationDatum[objects.length];
        for ( int i=0; i<objects.length; i++ ) {
            result[i]= create(objects[i]);
        }
        return result;
    }
    
    private static int getNextOrdinal( Object o ) {
        EnumerationUnits units= EnumerationUnits.create( o );
        if ( highestOrdinal==null ) highestOrdinal= new HashMap();
        Integer integer= (Integer) highestOrdinal.get(units);
        if (integer==null) {
            return 0;
        } else {
            return integer.intValue()+1;
        }
    }
    
    static HashMap getObjects() {
        if ( objects==null ) {
            objects= new HashMap();
        }
        return objects;
    }
    
    static HashMap getInstances() {
        if ( instances==null ) {
            instances= new HashMap();
        }
        return instances;
    }
    
    static HashMap getInstances(EnumerationUnits units) {
        getInstances();
        if ( instances.containsKey(units) ) {
            HashMap result= (HashMap)instances.get(units);
            return result;
        } else {
            HashMap result= new HashMap();
            instances.put(units,result);
            return result;
        }
    }
    
    public static EnumerationDatum create( Object object, int ordinal ) {
        if ( getObjects().containsKey(object) ) {
            return (EnumerationDatum)getObjects().get(object);
        }
        EnumerationUnits units= EnumerationUnits.create(object);
        return create( object, ordinal, units );
    }
    
    public static EnumerationDatum create( Object object, int ordinal, EnumerationUnits units ) {
        if ( getObjects().containsKey(object) ) {
            return (EnumerationDatum)getObjects().get(object);
        }
        HashMap t= getInstances(units);
        Integer d= new Integer(ordinal);
        if ( t.containsKey(d) ) {
            EnumerationDatum datum= (EnumerationDatum)t.get(d);
            if (datum.getObject().equals(object)) return datum; else
                throw new IllegalArgumentException("ordinal is already taken: "+d);
        } else {
            return new EnumerationDatum( object, ordinal, units );
        }
    }
    
    private EnumerationDatum( Object object, int ordinal, EnumerationUnits units ) {
        super(ordinal,units);
        
        if ( objects==null ) {
            objects= new HashMap();
        }
        if ( objects.containsKey(object) ) {
            throw new IllegalArgumentException( "This object has been enumerated already!" );
        }
        
        if ( highestOrdinal==null ) {
            highestOrdinal= new HashMap();
        }
        
        this.object= object;
        HashMap t= getInstances(units);
        Integer d= new Integer(ordinal);
        if ( t.containsKey(d) ) {
            EnumerationDatum datum= (EnumerationDatum)t.get(d);
            if (!datum.getObject().equals(object)) throw new IllegalArgumentException("ordinal is already taken: "+d);
        }
        t.put(d,this);
        
        if ( highestOrdinal.containsKey(units) ) {
            Integer highest= (Integer)highestOrdinal.get(units);
            if ( highest.compareTo(d)<0 ) {
                highestOrdinal.put(units,d);
            }
        } else {
            highestOrdinal.put(units,d);
        }
        objects.put( object, this );
    }
    
    public String toString() {
        return object.toString();
    }
    
    public static Datum create( double value, Units units ) {
        int ordinal= (int)value;
        Datum result=null;
        
        if ( ! (units instanceof EnumerationUnits )) {
            throw new IllegalArgumentException("units are not EnumerationUnits: "+units);
        }
        HashMap t= getInstances((EnumerationUnits)units);
        Integer d= new Integer(ordinal);
        if ( t.containsKey(d) ) {
            result= (EnumerationDatum)t.get(d);
        } else {
            throw new IllegalArgumentException("Datum for this ordinal has not been instanciated");
        }
        return result;
    }
    
    
    public static EnumerationDatum first(EnumerationDatum[] array) {
        if ( array.length==0 ) throw new IllegalArgumentException("Empty array");
        EnumerationDatum result= array[0];
        for (int i=1; i<array.length; i++) {
            if ( array[i].lt(result) ) result= array[i];
        }
        return result;
    }
    
    public boolean equals(Datum datum) {
        if ( datum==null ) return false;
        if ( !( datum instanceof EnumerationDatum ) ) {
            throw new IllegalArgumentException("argument is not an EnumerationDatum ("+datum+")");
        }
        return this.object.equals(((EnumerationDatum)datum).getObject());
    }
    
    public Object getObject() {
        return this.object;
    }
    
    public static EnumerationDatum lookup( Object object ) {
        if ( getObjects().containsKey(object) ) {
            return (EnumerationDatum)getObjects().get(object);
        } else {
            throw new IllegalArgumentException( "No instance for object: "+object );
        }
    }
    
    public static EnumerationDatum last(EnumerationDatum[] array) {
        if ( array.length==0 ) throw new IllegalArgumentException("Empty array");
        EnumerationDatum result= array[0];
        for (int i=1; i<array.length; i++) {
            if ( array[i].gt(result) ) result= array[i];
        }
        return result;
    }
    
    public static EnumerationDatum lookup( EnumerationDatum[] array, Object object ) {
        if ( array.length==0 ) throw new IllegalArgumentException("Empty array");
        int i=0;
        EnumerationDatum result= null;
        for ( i=1; i<array.length; i++ ) {
            if ( array[i].object.equals(object) ) {
                result= array[i];
            }
        }
        if ( result==null ) {
            throw new IllegalArgumentException("Object not found in EnumerationDatum array");
        }
        return result;
    }
    
    public static EnumerationDatum lookup( EnumerationDatum[] array, double ordinal ) {
        if ( array.length==0 ) throw new IllegalArgumentException("Empty array");
        int i=0;
        EnumerationDatum result= array[i];
        double distance= Math.abs( array[i].getValue() - ordinal );
        for ( i=1; i<array.length; i++ ) {
            double d1=  Math.abs( array[i].getValue() - ordinal );
            if ( d1 < distance ) {
                result= array[i];
                distance= d1;
            }
        }
        return result;
    }
    
    public class EnumerationFormatter extends DasFormatter {
        public String format(Object o) {
            if ( !( o instanceof EnumerationDatum ) ) {
                throw new IllegalArgumentException("Argument is not an EnumerationDatum! ("+o.getClass().getName()+")" );
            }
            EnumerationDatum datum= (EnumerationDatum)o;
            return datum.object.toString();
        }
        public String format( double d, Units units ) {
            return format(create(d,units));
        }
    }
    
    
    public DasFormatter getFormatter( Datum datum2, int nsteps ) {
        return getFormatter();
    }
    
    public DasFormatter getFormatter() {
        if (nf1==null) nf1=new EnumerationFormatter();
        return nf1;
    }
    
}
