/* File: DefaultVectorDataSet.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on October 27, 2003, 11:18 AM
 *      by Edward West <eew@space.physics.uiowa.edu>
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

package org.das2.dataset;

import org.das2.datum.Units;
import org.das2.datum.Datum;

import java.util.*;

/** 
 *
 * @author  Edward West
 */
public final class DefaultVectorDataSet extends AbstractVectorDataSet implements DataSet, VectorDataSet {
    
    private double[][] yValues;
    
    private String[] planeIDs;
    
    private Units[] yUnits;
    
    /** Creates a new instance of DefaultVectorDataSet
     * @param xTags array of double x tag values
     * @param xUnits units for the x tags
     * @param yValues array of double y values
     * @param yUnits untis for the y values
     * @param properties map of String property names to their values
     */
    public DefaultVectorDataSet(double[] xTags, Units xUnits,
                                double[] yValues, Units yUnits,
                                Map properties) {
        this(xTags, xUnits, yValues, yUnits, null, null, properties);
    }
    
    /** Creates a new instance of DefaultVectorDataSet
     * The keys for the properties, yValuesMap, and unitsMap parameter must
     * consist solely of String values.  If any of these key sets contain
     * non-string values an IllegalArgumentException will be thrown.
     * The key set of the yUnitsMap parameter must match exactly the key set
     * of the yValuesMap.  If their key sets do not exactly match, an
     * IllegalArgumentException will be thrown.
     * @param xTags array of double x tag values
     * @param xUnits units for the x tags
     * @param yValues array of double y values
     * @param yUnits untis for the y values
     * @param yValuesMap map of String plane IDs to their y values
     * @param properties map of String property names to their values
     */
    public DefaultVectorDataSet(double[] xTags, Units xUnits,
                         double[] yValues, Units yUnits,
                         Map yValuesMap, Map yUnitsMap,
                         Map properties) {
        super(xTags, xUnits, yUnits, properties);        
        if (yValuesMap == null ^ yUnitsMap == null) {
            throw new IllegalArgumentException("yValuesMap == null ^ yUnitsMap == null");
        }
        if ( yValuesMap!=null ) {
            if (!yValuesMap.keySet().equals(yUnitsMap.keySet())) {
                throw new IllegalArgumentException("mismatched keySets for yValuesMap and yUnitsMap");
            }
            for (Iterator it = yValuesMap.keySet().iterator(); it.hasNext();) {
                if (!(it.next() instanceof String)) {
                    throw new IllegalArgumentException("Non-String key found in yValuesMap");
                }
            }
        }
        int planeCount = 1 + (yValuesMap == null ? 0 : yValuesMap.size());
        this.yValues = new double[planeCount][];
        this.yValues[0] = (double[])yValues.clone();
        this.planeIDs = new String[planeCount];
        this.planeIDs[0] = "";
        this.yUnits = new Units[planeCount];
        this.yUnits[0] = yUnits;
        if (yValuesMap != null) {
            int index = 1;
            for (Iterator it = new TreeMap(yValuesMap).entrySet().iterator(); it.hasNext();) {
                Map.Entry entry = (Map.Entry)it.next();
                String id = (String)entry.getKey();
                double[] values = (double[])entry.getValue();
                planeIDs[index] = id;
                this.yValues[index] = (double[])values.clone();
                this.yUnits[index] = (Units)yUnitsMap.get(id);
            }
        }
    }
    
    /** Creates a DefaultVectorData set without copying or verifying the
     * data and units supplied.
     */
    DefaultVectorDataSet(double[] xTags, Units xUnits,
                         double[][] yValues, Units[] yUnits,
                         String[] planeIDs, Map properties) {
        super(xTags, xUnits, yUnits[0], properties);
        this.yValues = yValues;
        this.yUnits = yUnits;
        this.planeIDs = planeIDs;
    }
    
    /** Returns the Y value for the given index into the x tags as a
     * <code>Datum</code>.
     * @param i index of the x tag for the requested value.
     * @return the value at index location i as a <code>Datum</code>
     */    
    public Datum getDatum(int i) {
        return Datum.create(yValues[0][i], getYUnits());
    }
    
    /** Returns the Y value for the given index into the x tags as a
     * <code>double</code> with the given units.
     * @param i index of the x tag for the requested value.
     * @param units the units the returned value should be coverted to.
     * @return the value at index location i as a <code>double</code>.
     */    
    public double getDouble(int i, Units units) {
        if (yUnits[0].isFill(yValues[0][i])) {
            return units.getFillDouble();
        }
        return getYUnits().getConverter(units).convert(yValues[0][i]);
    }
    
    /** Returns the Y value for the given index into the x tags as a
     * <code>int</code> with the given units.
     * @param i index of the x tag for the requested value.
     * @param units the units the returned value should be coverted to.
     * @return the value at index location i as a <code>int</code>.
     */    
    public int getInt(int i, Units units) {
        return (int)Math.round(getDouble(i, units));
    }
    
    /** Returns a <code>DataSet</code> with the specified view as the primary
     * view.
     * @param planeID the <code>String</code> id of the requested plane.
     * @return the specified view, as a <code>DataSet</code>
     */
    public DataSet getPlanarView(String planeID) {
        int index = -1;
        for (int i = 0; i < planeIDs.length; i++) {
            if (planeIDs[i].equals(planeID)) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            return null;
        }
        else {
            return new PlanarViewDataSet(index);
        }
    }
    
    public String[] getPlaneIds() {
        String[] result= new String[planeIDs.length];
        System.arraycopy( planeIDs, 0, result, 0, planeIDs.length );
        return result;
    }
    
    private class PlanarViewDataSet extends AbstractDataSet.ViewDataSet implements VectorDataSet {
        
        private final int index;
        
        private PlanarViewDataSet(int index) {
            DefaultVectorDataSet.this.super();
            this.index = index;
        }
        
        public Datum getDatum(int i) {
            return Datum.create(yValues[index][i], yUnits[index]);
        }
        
        public double getDouble(int i, Units units) {
            return yUnits[index].getConverter(units).convert(yValues[index][i]);
        }
        
        public int getInt(int i, Units units) {
            return (int)Math.round(getDouble(i, units));
        }
        
        public Units getYUnits() {
            return yUnits[index];
        }
        
        public DataSet getPlanarView(String planeID) {
            DataSet result= planeID.equals("") ? this : DefaultVectorDataSet.this.getPlanarView(planeID);
            if (result==null ) return DefaultVectorDataSet.this.getPlanarView( planeIDs[index] + "."+planeID );
            return result;
        }
    
        public String[] getPlaneIds() {
            return new String[0] ;
        }
        
        public Object getProperty(String name) {
            Object result= DefaultVectorDataSet.this.getProperty(planeIDs[index] + "." + name);
            if ( result==null ) {
                result= DefaultVectorDataSet.this.getProperty(name);
            }
            return result;
        }
        
        @Override
        public Map getProperties() {
            Map superProps= super.getProperties();
            String[] names= new String[] { PROPERTY_TITLE, PROPERTY_Y_LABEL, PROPERTY_Y_FORMAT, PROPERTY_Y_RANGE, PROPERTY_Y_SCALETYPE };
            
            Map result= new HashMap<>(superProps);
            for ( String name: names ) {
                Object v= superProps.get( planeIDs[index] + "." + name );
                result.put( name, v );
            }
            return result;
        }
        
        // TODO: this appears to have different logic than in ViewDataSet.  This needs to be resolved.
        
       // public Map getProperties() {
       //     throw new IllegalStateException("unimplemented");            
       //     //return DefaultVectorDataSet.this.getProperty(planeIDs[index] + "." + name);
       // }

        public String toString() {
            return "DefaultVectorDataSet("+DefaultVectorDataSet.this.planeIDs[index]+") "+VectorUtil.toString(this);
        }
        
    }
    
}
