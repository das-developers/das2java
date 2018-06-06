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
package org.das2.datum;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.das2.datum.format.DatumFormatterFactory;
import org.das2.datum.format.EnumerationDatumFormatterFactory;

/**
 * Units class for mapping arbitrary objects to Datums.  Nothing about the contract
 * for a Datum requires that they correspond to physical quantities, and we can
 * assign a mapping from numbers to objects using this class.  This allows 
 * information such as "Cluster 1" or "Spin Flip" to be encoded.
 * 
 * This is used to model ordinal or nominal data, as described in
 * http://en.wikipedia.org/wiki/Level_of_measurement
 * 
 * @author  Jeremy
 */
public class EnumerationUnits extends Units {

    private static final Logger logger= LoggerManager.getLogger("das2.datum.enum");
    
    private Map<Integer, Datum> ordinals;  // maps from ordinal to Datum.Integer
    private int highestOrdinal; // highest ordinal for each Units type
    private Map<Object, Datum> objects;    // maps from object to Datum.Integer
    private Map<Datum, Object> invObjects; // maps from Datum.Integer to object
    private Map<Integer,Integer> colors;   // maps from ordinal to the color for the ordinal.
    private static Map<String, EnumerationUnits> unitsInstances;

    public EnumerationUnits(String id) {
        this(id, "");
    }

    public EnumerationUnits(String id, String description) {
        super(id, description);
        synchronized (this) {
            if (unitsInstances == null) {
                unitsInstances = new HashMap<>();
            }
        }
        // Note that there's a class of EnumerationUnits, where the id doesn't have
        // be unique.  This has happened accidentally, and I need to study why it
        // works and what limitations this introduces, but this may happen.  The code
        // briefly checked for this condition and issued a warning that was not helpful.
        highestOrdinal = 0;
        ordinals = new ConcurrentHashMap<>();
        objects = new ConcurrentHashMap<>();
        invObjects = new ConcurrentHashMap<>();
        colors= new ConcurrentHashMap<>();
        unitsInstances.put(id,this);
    }

    public static Datum createDatumAndUnits(Object object) {
        return create(object).createDatum(object);
    }

    /**
     * creates the datum, explicitly setting the ordinal.  Use with caution.
     * @param ival the integer value of the datum
     * @param sval the object to associate.  This can be an object for legacy reasons, but should be a String.
     * @param color RGB color to associate with this value.  
     * @return the Datum, which may have been created already.
     * @throws IllegalArgumentException if this ordinal is already taken by a different value.
     */
    public Datum createDatum(int ival, Object sval, int color ) {
        if ( sval instanceof String ) {
            sval= ((String)sval).trim();
        }
        if (objects.containsKey(sval)) {
            return objects.get(sval);
        } else {
            synchronized (this) {
                if ( objects.containsKey(sval)) { // double check in synchronized block
                    return objects.get(sval);
                } 
                if (highestOrdinal < ival) {
                    highestOrdinal = ival;
                }
                Integer ordinal = ival;
                Datum result = new Datum.Double(ordinal, this);
                if ( ordinals.containsKey(ordinal) ) {
                    Datum d=  ordinals.get(ordinal);
                    if ( ! invObjects.get( d ).equals(sval) ) {
                        throw new IllegalArgumentException("value already exists for this ordinal!");
                    }
                }
                ordinals.put(ordinal, result);
                invObjects.put(result, sval);
                objects.put(sval, result);
                colors.put(ordinal,color);
                return result;
            }
        }
    }
    
    /**
     * return color suggestion for this value.  Note this color is not always used.
     * @param d the datum
     * @return the color suggestions
     */
    public int getColor( Datum d ) {
        if ( colors==null ) {
            return 0xA0A0A0;
        } else {
            Integer c= colors.get( (int)d.doubleValue(this) );
            if ( c==null ) {
                return 0xA0A0A0;
            } else {
                return c;
            }
        }
    }
    
    /**
     * creates the datum, explicitly setting the ordinal.  Use with caution.
     * @param ival the integer value of the datum
     * @param sval the object to associate.  This can be an object for legacy reasons.
     * @return a Datum representing sval.
     * @throws IllegalArgumentException if this ordinal is already taken by a different value.
     */    
    public Datum createDatum( int ival, Object sval ) {
        return createDatum( ival, sval, 0xA0A0A0 );
    }

    public DatumVector createDatumVector(Object[] objects) {
        double[] doubles = new double[objects.length];
        for (int i = 0; i < objects.length; i++) {
            doubles[i] = createDatum(objects[i]).doubleValue(this);
        }
        return DatumVector.newDatumVector(doubles, this);
    }

    /**
     * true if fill has been defined, which is the empty string or all spaces.
     * @return true if fill has been defined.
     */
    public boolean hasFillDatum() {
        return objects.containsKey("");
    }
    
    /**
     * return the Datum that represents this object, or create a Datum for 
     * the object.  The object should be a String, but to support legacy applications
     * it is an object 
     * @param object an object, typically a string.
     * @return Datum representing the object.
     */
    public Datum createDatum(Object object) {
        if ( object instanceof String ) {
            object= ((String)object).trim();
        }
        if (objects.containsKey(object)) {
            return objects.get(object);
        } else {
            Integer ordinal;
            synchronized (this) {
                if ( objects.containsKey(object)) { // double check
                    return objects.get(object);
                }
                highestOrdinal++;
                ordinal = highestOrdinal;  
                Datum result = new Datum.Double(ordinal, this);
                ordinals.put(ordinal, result);
                invObjects.put(result, object);
                objects.put(object, result);
                return result;
            }            
        }
    }

    /**
     * provides access to map of all values.
     * @return all the values
     */
    public synchronized Map<Integer, Datum> getValues() {
        return Collections.unmodifiableMap(ordinals);
    }

    @Override
    public Datum createDatum(int value) {
        Integer key = value;
        Datum d= ordinals.get(key);
        if ( d!=null ) {
            return d;
        } else {
            throw new IllegalArgumentException("No Datum exists for this ordinal: " + value + "  hashcode="+this.hashCode() );
        }
    }

    @Override
    public Datum createDatum(long value) {
        return createDatum((int) value);
    }

    @Override
    public Datum createDatum(Number value) {
        return createDatum(value.intValue());
    }

    /**
     * return the object (typically a string) associated with this Datum
     * @param datum
     * @return the object associated with the Datum.
     */
    public Object getObject(Datum datum) {
        if (invObjects.containsKey(datum)) {
            return invObjects.get(datum);
        } else {
            throw new IllegalArgumentException("This Datum doesn't map back to an object!  This shouldn't happen!");
        }
    }

    /**
     * create the enumeration unit with the given context.
     * @param o
     * @return 
     */
    public static synchronized EnumerationUnits create(Object o) {
        String ss= o.toString();
        ss= ss.trim();
        if (unitsInstances == null) {
            unitsInstances = new HashMap<>();
        }
        if (unitsInstances.containsKey(ss)) {
            return unitsInstances.get(ss);
        } else {
            EnumerationUnits result = new EnumerationUnits(ss);
            unitsInstances.put(ss, result);
            return result;
        }
    }

    @Override
    public Datum createDatum(double d) {
        return createDatum((int) d);
    }

    @Override
    public Datum createDatum(double d, double resolution) {
        return createDatum((int) d);
    }

    @Override
    public DatumFormatterFactory getDatumFormatterFactory() {
        return EnumerationDatumFormatterFactory.getInstance();
    }

    @Override
    public Datum subtract(Number a, Number b, Units bUnits) {
        throw new IllegalArgumentException("subtract on EnumerationUnit");
    }

    @Override
    public Datum add(Number a, Number b, Units bUnits) {
        throw new IllegalArgumentException("add on EnumerationUnit");
    }

    @Override
    public Datum divide(Number a, Number b, Units bUnits) {
        throw new IllegalArgumentException("divide on EnumerationUnit");
    }

    @Override
    public Datum multiply(Number a, Number b, Units bUnits) {
        throw new IllegalArgumentException("multiply on EnumerationUnit");
    }

    @Override
    public Datum parse(String s) throws java.text.ParseException {
        Datum result = null;
        for (Iterator i = objects.keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            //Object value = objects.get(key);
            if (key.toString().equals(s)) { // if the look the same, they are the same
                if (result == null) {
                    result = (Datum) objects.get(key);
                } else {
                    throw new IllegalStateException("Multiple Objects' string representations match");
                }
            }
        }
        if (result == null) {
            throw new java.text.ParseException("no objects match \"" + s + "\"", 0);
        }
        return result;
    }

    public synchronized int getHighestOrdinal() {
        return this.highestOrdinal;
    }

    /**
     * return the double for ""
     * @return 
     */
    @Override
    public double getFillDouble() {
        return createDatum("").doubleValue(this);
    }

    /**
     * return the datum for ""
     * @return 
     */
    @Override
    public Datum getFillDatum() {
        return createDatum("");
    }

    @Override
    public boolean isFill(Number value) {
        return value.doubleValue()==getFillDouble();
    }

    @Override
    public double convertDoubleTo(Units toUnits, double value) {
        if ( !( toUnits instanceof EnumerationUnits ) ) throw new IllegalArgumentException("unable to convert ordinal data");
        if ( this==toUnits ) {
            return value;
        } else {
            return ((EnumerationUnits)toUnits).createDatum( this.createDatum(value).toString() ).doubleValue(toUnits);
        }
    }
    
    @Override
    public String toString() {
        //return this.getId() + "(ordinal "+Integer.toHexString(this.hashCode())+")";
        return this.getId() + "(ordinal)";
    }
}
