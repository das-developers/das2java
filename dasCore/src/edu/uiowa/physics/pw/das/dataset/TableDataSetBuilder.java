/* File: TableDataSetBuilder.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on December 10, 2003, 4:54 PM
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

package edu.uiowa.physics.pw.das.dataset;

import java.util.*;
import edu.uiowa.physics.pw.das.datum.*;

/**
 *
 * @author  Edward West
 */
public class TableDataSetBuilder {
    
    private static final double[] EMPTY = new double[0];
    
    private GapListDouble xTags = new GapListDouble();
    
    private List zValues = new ArrayList();
    
    private List planeIDs = new ArrayList();
    {
        planeIDs.add("");
    }
    
    private Units xUnits = Units.dimensionless;
    
    private Units yUnits = Units.dimensionless;
    
    private Map zUnitsMap = new HashMap();
    {
        zUnitsMap.put("", Units.dimensionless);
    }
    
    private SortedSet yTagSet = new TreeSet(new DoubleArrayComparator());
    
    private Map properties = new HashMap();
    
    private Datum zFill = Units.dimensionless.getFill();
    
    /** Creates a new instance of TableDataSetBuilder */
    public TableDataSetBuilder() {
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

    public void addPlane(String name) {
        if (!planeIDs.contains(name)) {
            planeIDs.add(name);
            zUnitsMap.put(name, Units.dimensionless);
        }
    }
    
    public void insertYScan(double x, double[] y, double[] z) {
        insertYScan(x, y, z, "");
    }
    
    public void insertYScan(double x, double[] y, double[] z, String planeID) {
        int insertionIndex = xTags.indexOf(x);
        if (planeID == null) {
            planeID = "";
        }
        if (yTagSet.contains(y)) {
            y = (double[])yTagSet.tailSet(y).iterator().next();
        }
        else {
            y = (double[])y.clone();
            yTagSet.add(y);
        }
        if (insertionIndex < 0) {
            insertionIndex = ~insertionIndex;
            xTags.add(x);
            MultiYScan scan = new MultiYScan();
            scan.put(planeID, (double[])z.clone());
            scan.setYTags(y);
            zValues.add(insertionIndex, scan);
        }
        else {
            MultiYScan scan = (MultiYScan)zValues.get(insertionIndex);
            if (!Arrays.equals(y, scan.getYTags())) {
                throw new IllegalArgumentException();
            }
            scan.put(planeID, (double[])z.clone());
        }
    }
    
    public void append(TableDataSet tds) {
        Units zUnits = (Units)zUnitsMap.get("");
        for (int table = 0; table < tds.tableCount(); table++) {
            double[] yCoordinates = new double[tds.getYLength(table)];
            for (int j = 0; j < yCoordinates.length; j++) {
                yCoordinates[j] = tds.getYTagDouble(table, j, yUnits);
            }
            double[] scan = new double[tds.getYLength(table)];
            for (int i = tds.tableStart(table); i < tds.tableEnd(table); i++) {
                for (int j = 0; j < scan.length; j++) {
                    scan[j] = tds.getDouble(i, j, zUnits);
                }
                insertYScan(tds.getXTagDouble(i, xUnits), yCoordinates, scan);
            }
        }
    }
    
    public void setXUnits(Units units) {
        if (units == null) {
            throw new NullPointerException();
        }
        xUnits = units;
    }
    
    public void setYUnits(Units units) {
        if (units == null) {
            throw new NullPointerException();
        }
        yUnits = units;
    }
    
    public void setZUnits(Units units) {
        setZUnits(units, "");
    }
    
    public void setZUnits(Units units, String planeID) {
        if (units == null) {
            throw new NullPointerException();
        }
        zUnitsMap.put(planeID, units);
    }
    
    public String toString() {
        int index = 0;
        StringBuffer result = new StringBuffer();
        for (Iterator i = zValues.iterator(); i.hasNext();) {
            MultiYScan scan = (MultiYScan)i.next();
            result.append(toString(xTags.toArray(), index, index+1)).append(toString(scan.get(""))).append('\n');
            index++;
        }
        return result.toString();
    }
    
    public TableDataSet toTableDataSet() {
        int count = getTableCount(zValues);
        int[] tableOffsets = getTableOffsets(zValues, count);
        double[][] collapsedYTags = collapseYTags(zValues, count);
        double[][][] collapsedZValues = collapseZValues(zValues, planeIDs, zUnitsMap);
        Units[] zUnitsArray = getUnitsArray(planeIDs, zUnitsMap);
        return new DefaultTableDataSet(xTags.toArray(), xUnits, collapsedYTags, yUnits, collapsedZValues, zUnitsArray, (String[])planeIDs.toArray(new String[planeIDs.size()]), tableOffsets, properties);
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

    private static int getTableCount(List list) {
        int count = 0;
        double[] previous = null;
        for (Iterator i = list.iterator(); i.hasNext();) {
            MultiYScan scan = (MultiYScan)i.next();
            if (!Arrays.equals(previous, scan.getYTags())) {
                previous = scan.getYTags();
                count++;
            }
        }
        return count;
    }
    
    private static int[] getTableOffsets(List list, int count) {
        double[] previous = null;
        int index = 0;
        int offset = 0;
        int[] tableOffsets = new int[count];
        for (Iterator i = list.iterator(); i.hasNext();) {
            MultiYScan scan = (MultiYScan)i.next();
            if (!Arrays.equals(previous, scan.getYTags())) {
                tableOffsets[index] = offset;
                previous = scan.getYTags();
                index++;
            }
            offset++;
        }
        return tableOffsets;
    }
    
    private static double[][] collapseYTags(List list, int count) {
        double[] previous = null;
        int index = 0;
        double[][] result = new double[count][];
        for (Iterator i = list.iterator(); i.hasNext();) {
            MultiYScan scan = (MultiYScan)i.next();
            if (!Arrays.equals(previous, scan.getYTags())) {
                result[index] = scan.getYTags();
                previous = scan.getYTags();
                index++;
            }
        }
        return result;
    }
    
    private static double[][][] collapseZValues(List list, List planeIDs, Map unitsMap) {
        double[][][] zValues = new double[planeIDs.size()][list.size()][];
        int index = 0;
        for (Iterator i = list.iterator(); i.hasNext();) {
            MultiYScan scan = (MultiYScan)i.next();
            for (int plane = 0; plane < planeIDs.size(); plane++) {
                double[] z = scan.get((String)planeIDs.get(plane));
                if (z == null) {
                    z = new double[scan.getYTags().length];
                    Units units = (Units)unitsMap.get(planeIDs.get(plane));
                    Arrays.fill(z, units.getFill().doubleValue(units));
                }
                zValues[plane][index] = z;
            }
            index++;
        }
        return zValues;
    }
    
    private static Units[] getUnitsArray(List planeIDs, Map unitsMap) {
        Units[] units = new Units[planeIDs.size()];
        for (int i = 0; i < units.length; i++) {
            units[i] = (Units)unitsMap.get(planeIDs.get(i));
        }
        return units;
    }
    
    private class DoubleArrayComparator implements Comparator {
        
        public int compare(Object o1, Object o2) {
            double[] d1 = (double[])o1;
            double[] d2 = (double[])o2;
            if (d1.length != d1.length) {
                return d1.length - d2.length;
            }
            for (int i = 0; i < d1.length; i++) {
                double delta = d1[i] - d2[i];
                if (delta < 0.0) {
                    return -1;
                }
                else if (delta > 0.0) {
                    return 1;
                }
            }
            return 0;
        }
        
    }
    
    private class MultiYScan {
        private HashMap map = new HashMap();
        double[] yTags;
        public void put(String name, double[] scan) {
            map.put(name, scan);
        }
        public double[] get(String name) {
            return (double[])map.get(name);
        }
        public double[] getYTags() {
            return yTags;
        }
        public void setYTags(double[] yTags) {
            this.yTags = yTags;
        }
    }
    
}
