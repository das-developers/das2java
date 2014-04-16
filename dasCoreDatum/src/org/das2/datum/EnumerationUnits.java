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
import java.util.logging.Level;
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

    private static final Logger logger= LoggerManager.getLogger("das2.units.enum");
    
    private HashMap<Integer, Datum> ordinals;  // maps from ordinal to Datum.Integer
    private int highestOrdinal; // highest ordinal for each Units type
    private HashMap<Object, Datum> objects;    // maps from object to Datum.Integer
    private HashMap<Datum, Object> invObjects; // maps from Datum.Integer to object
    private HashMap<Integer,Integer> colors;   // maps from ordinal to the color for the ordinal.
    private static HashMap<String, EnumerationUnits> unitsInstances;

    public EnumerationUnits(String id) {
        this(id, "");
    }

    public EnumerationUnits(String id, String description) {
        super(id, description);
        synchronized (this) {
            if (unitsInstances == null) {
                unitsInstances = new HashMap<String, EnumerationUnits>();
            }
        }
        if ( unitsInstances.containsKey(id) ) {
            logger.log(Level.WARNING, "enumeration init for {0} already defined.", id);
        }
        highestOrdinal = 0;
        ordinals = new HashMap<Integer, Datum>();
        objects = new HashMap<Object, Datum>();
        invObjects = new HashMap<Datum, Object>();
        colors= new HashMap<Integer,Integer>();
        unitsInstances.put(id,this);
    }

    public static Datum createDatumAndUnits(Object object) {
        return create(object).createDatum(object);
    }

    /**
     * creates the datum, explicitly setting the ordinal.  Use with caution.
     * @param ival the integer value of the datum
     * @param sval the object to associate.  This can be an object for legacy reasons.
     * @param color RGB color to associate with this value.  
     * @throws IllegalArgumentException if this ordinal is already taken by a different value.
     */
    public Datum createDatum(int ival, Object sval, int color ) {
        if (objects.containsKey(sval)) {
            return objects.get(sval);
        } else {
            synchronized (this) {
                if (highestOrdinal < ival) {
                    highestOrdinal = ival;
                }
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
    
    public int getColor( Datum d ) {
        return colors.get( (int)d.doubleValue(this) );
    }
    
    /**
     * creates the datum, explicitly setting the ordinal.  Use with caution.
     * @param ival the integer value of the datum
     * @param sval the object to associate.  This can be an object for legacy reasons.
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

    public Datum createDatum(Object object) {
        if (objects.containsKey(object)) {
            return objects.get(object);
        } else {
            Integer ordinal;
            synchronized (this) {
                highestOrdinal++;
                ordinal = highestOrdinal;  
            }
            Datum result = new Datum.Double(ordinal, this);
            ordinals.put(ordinal, result);
            invObjects.put(result, object);
            objects.put(object, result);
            return result;
        }
    }

    /**
     * provides access to map of all values.
     * @return
     */
    public Map<Integer, Datum> getValues() {
        return Collections.unmodifiableMap(ordinals);
    }

    public Datum createDatum(int value) {
        Integer key = value;
        if (ordinals.containsKey(key)) {
            return ordinals.get(key);
        } else {
            throw new IllegalArgumentException("No Datum exists for this ordinal: " + value);
        }
    }

    public Datum createDatum(long value) {
        return createDatum((int) value);
    }

    public Datum createDatum(Number value) {
        return createDatum(value.intValue());
    }

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
        if (unitsInstances == null) {
            unitsInstances = new HashMap<String, EnumerationUnits>();
        }
        if (unitsInstances.containsKey(ss)) {
            return unitsInstances.get(ss);
        } else {
            EnumerationUnits result = new EnumerationUnits(ss);
            unitsInstances.put(ss, result);
            return result;
        }
    }

    public Datum createDatum(double d) {
        return createDatum((int) d);
    }

    public Datum createDatum(double d, double resolution) {
        return createDatum((int) d);
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
        Datum result = null;
        for (Iterator i = objects.keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            Object value = objects.get(key);
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

    @Override
    public String toString() {
        return this.getId() + "(ordinal)";
    }
}
