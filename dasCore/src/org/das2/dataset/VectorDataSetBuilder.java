/* File: VectorDataSetBuilder.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on December 23, 2003, 8:58 AM
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
public class VectorDataSetBuilder {
    
    private GapListDouble xTags = new GapListDouble();
    private List yValues = new ArrayList();
    
    private List planeIDs = new ArrayList();
    {
        planeIDs.add("");
    }
    
    private Units xUnits = Units.dimensionless;
    
    private Map yUnitsMap = new HashMap();
    {
        yUnitsMap.put("", Units.dimensionless);
    }
    
    private Map properties = new HashMap();
    
    /** Creates a new instance of VectorDataSetBuilder */
    public VectorDataSetBuilder( Units xUnits, Units yUnits ) {
        this.xUnits= xUnits;
        setYUnits(yUnits);
    }
    
    public void setProperty(String name, Object value) {
        properties.put(name, value);
    }
    
    public Object getProperty(String name) {
        return properties.get(name);
    }
    
    public void addProperties(Map map) {
        properties.putAll(map);
    }
    
    public void addPlane(String name, Units yUnits ) {
        if (!planeIDs.contains(name)) {
            planeIDs.add(name);
            yUnitsMap.put(name, yUnits );
        }
    }
    
    public void insertY(double x, double y) {
        insertY(x, y, "");
    }
    
    public void insertY(double x, double y, String planeID) {
        if ( !planeIDs.contains(planeID) ) {
            throw new IllegalArgumentException( "invalid planeID: "+planeID+", have "+planeIDs );
        }
        if ( Double.isInfinite(x) || Double.isNaN(x) ) {
            throw new IllegalArgumentException( "x is not finite" );
        }
        int insertionIndex = xTags.indexOf(x);
        if (planeID == null) {
            planeID = "";
        }
        if (insertionIndex < 0) {
            insertionIndex = ~insertionIndex;
            xTags.add(x);
            MultiY scan = new MultiY();
            scan.put(planeID, y);
            yValues.add(insertionIndex, scan);
        } else {
            MultiY scan = (MultiY)yValues.get(insertionIndex);
            scan.put(planeID, y);
        }
    }
    
    /**
     * Insert method favored when there is a default and one additional plane, 
     * because it's less prone to error when the
     * one forgets the planeId.  (And it's slightly more efficient because 
     * the index search need only be done once.)
     */
    public void insertY( double x, double y, String planeId1, double planeValue1 ) {
        if ( Double.isInfinite(x) || Double.isNaN(x) ) {
            throw new IllegalArgumentException( "x is not finite" );
        }
        int insertionIndex = xTags.indexOf(x);
        if (insertionIndex < 0) {
            insertionIndex = ~insertionIndex;
            xTags.add(x);
            MultiY scan = new MultiY();
            scan.put( "", y );
            scan.put( planeId1, planeValue1 );
            yValues.add(insertionIndex, scan);
        } else {
            //throw new IllegalArgumentException("already got value at this index");
            MultiY scan = (MultiY)yValues.get(insertionIndex);
            scan.put( "", y );
            scan.put( planeId1, planeValue1 );
        }
    }
    
    /** 
     * insert a datum for the default plane
     */
    public void insertY(Datum x, Datum y) {
        insertY(x, y, "");
    }
    
    /**
     * insert a datum for the plane.
     */
    public void insertY(Datum x, Datum y, String planeID) {
        if (!planeIDs.contains(planeID)) {
            throw new IllegalArgumentException("invalid planeID: "+planeID+", have "+planeIDs);
        }
        double xd = x.doubleValue(xUnits);
        double yd = y.doubleValue((Units)yUnitsMap.get(planeID));
        insertY(xd, yd, planeID);
    }
    
    public void append(VectorDataSet vds) {
        String[] planeIds= DataSetUtil.getAllPlaneIds(vds);
        for ( int iplane=0; iplane<planeIds.length; iplane++ ) {
            String plane= planeIds[iplane];
            VectorDataSet planeDs= (VectorDataSet)vds.getPlanarView(plane);
            Units yUnits = (Units)yUnitsMap.get(plane);
            if ( yUnits==null ) {
                addPlane( plane, planeDs.getYUnits() );
                yUnits = (Units)yUnitsMap.get(plane);
            }
            for (int i = 0; i < planeDs.getXLength(); i++) {
                insertY( planeDs.getXTagDouble(i, xUnits), planeDs.getDouble(i, yUnits), plane );
            }
        }
        addProperties(vds.getProperties());
    }
    
    public void setXUnits(Units units) {
        if (units == null) {
            throw new NullPointerException();
        }
        xUnits = units;
    }
    
    public void setYUnits(Units units) {
        setYUnits(units, "");
    }
    
    public void setYUnits(Units units, String planeID) {
        if (units == null) {
            throw new NullPointerException();
        }
        yUnitsMap.put(planeID, units);
    }
    
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append("x: ").append(toString(xTags.toArray())).append("\ny: ");
        for (Iterator i = yValues.iterator(); i.hasNext();) {
            MultiY y = (MultiY)i.next();
            result.append(y.get("")).append(',');
        }
        return result.toString();
    }
    
    public VectorDataSet toVectorDataSet() {
        double[][] collapsedYValues = collapseYValues(yValues, planeIDs, yUnitsMap);
        Units[] yUnitsArray = getUnitsArray(planeIDs, yUnitsMap);
        properties.put( "plane-list", Collections.unmodifiableList(planeIDs) );
        return new DefaultVectorDataSet(xTags.toArray(), xUnits, collapsedYValues, yUnitsArray, (String[])planeIDs.toArray(new String[planeIDs.size()]), properties);
    }
    
    private static double[] insert(double[] array, double value, int index) {
        double[] result = new double[array.length + 1];
        System.arraycopy(array, 0, result, 0, index);
        result[index] = value;
        System.arraycopy(array, index, result, index + 1, array.length - index);
        return result;
    }
    
    private static double[][] insert(double[][] array, double[] values, int index) {
        double[][] result = new double[array.length + 1][];
        System.arraycopy(array, 0, result, 0, index);
        result[index] = values;
        System.arraycopy(array, index, result, index + 1, array.length - index);
        return result;
    }
    
    private static String toString(double[] array) {
        return toString(array, 0, array.length);
    }
    
    private static String toString(double[] array, int startIndex, int endIndex) {
        if (array.length == 0) return "[]";
        StringBuffer buffer = new StringBuffer("[");
        for (int i = startIndex; i < endIndex-1; i++) {
            buffer.append(array[i]).append(", ");
        }
        buffer.append(array[endIndex - 1]).append(']');
        return buffer.toString();
    }
    
    private static double[][] collapseYValues(List list, List planeIDs, Map unitsMap) {
        double[][] yValues = new double[planeIDs.size()][list.size()];
        int index = 0;
        for (Iterator i = list.iterator(); i.hasNext();) {
            MultiY my = (MultiY)i.next();
            for (int plane = 0; plane < planeIDs.size(); plane++) {
                double y = my.get((String)planeIDs.get(plane));
                if (Double.isNaN(y)) {
                    Units units = (Units)unitsMap.get(planeIDs.get(plane));
                    y = units.getFillDouble();
                }
                yValues[plane][index] = y;
            }
            index++;
        }
        return yValues;
    }
    
    private static Units[] getUnitsArray(List planeIDs, Map unitsMap) {
        Units[] units = new Units[planeIDs.size()];
        for (int i = 0; i < units.length; i++) {
            units[i] = (Units)unitsMap.get(planeIDs.get(i));
        }
        return units;
    }
    
    private static class MultiY {
        private HashMap yValues = new HashMap();
        private void put(String name, double y) {
            yValues.put(name, new Double(y));
        }
        private double get(String name) {
            Double y = (Double)yValues.get(name);
            return y == null ? Double.NaN : y.doubleValue();
        }
    }
    
}
