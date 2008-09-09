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

package org.das2.dataset;

import org.das2.datum.Units;
import org.das2.datum.DatumVector;
import org.das2.datum.Datum;
import java.util.*;

/**
 *
 * @author  Edward West
 */
public class TableDataSetBuilder {
    
    private static final double[] EMPTY = new double[0];
    
    private GapListDouble xTags = new GapListDouble();
    
    private List zValues = new ArrayList();
    
    private List planeIDs = new ArrayList(); {
        planeIDs.add("");
    }
    
    private Units xUnits = Units.dimensionless;
    
    private Units yUnits = Units.dimensionless;
    
    private Map zUnitsMap = new HashMap(); {
        zUnitsMap.put("", Units.dimensionless);
    }
    
    private SortedSet yTagSet = new TreeSet(new DoubleArrayComparator());
    
    private Map properties = new HashMap();
    private List<Map> tableProperties= new ArrayList<Map>();
    
    /** Creates a new instance of TableDataSetBuilder */
    public TableDataSetBuilder( Units xUnits, Units yUnits, Units zUnits ) {
        setXUnits(xUnits);
        setYUnits(yUnits);
        setZUnits(zUnits);
    }
    
    public void setProperty(String name, Object value) {
        properties.put(name, value);
    }
    
    public void setProperty( int table, String name, Object value ) {
        if ( tableProperties.size()<table ) {
            tableProperties.add( new HashMap() );
        }
        tableProperties.get(table).put( name, value );
    }
    
    public Object getProperty(String name) {
        return properties.get(name);
    }
    
    public void addProperties(Map map) {
        properties.putAll(map);
    }
    
    public void addPlane(String name, Units zUnits ) {
        if (name != null && !planeIDs.contains(name)) {
            planeIDs.add(name);
            zUnitsMap.put(name, zUnits );
        }
    }
    
    public void insertYScan( Datum x, DatumVector y, DatumVector z) {
        insertYScan(x, y, new DatumVector[]{z}, new String[]{""});
    }
    
    public void insertYScan( Datum x, DatumVector y, DatumVector z, String planeId ) {
        insertYScan( x, y, new DatumVector[]{z}, new String[]{ planeId } );
    }
    
    private int count = 0;
    
    public void insertYScan(Datum xTag, DatumVector yTags, DatumVector[] scans, String[] planeIDs) {
        double x = xTag.doubleValue(xUnits);
        double[] y = yTags.toDoubleArray(yUnits);
        int insertionIndex = xTags.indexOf(x);
        for (int i = 0; i < scans.length; i++) {
            if (yTags.getLength() != scans[i].getLength()) {
                IllegalArgumentException iae = new IllegalArgumentException
                    ("Scan length must equal yTags length");
            }
        }
        if (yTagSet.contains(y)) {
            y = (double[])yTagSet.tailSet(y).iterator().next();
        }
        else {
            yTagSet.add(y);
        }
        if (insertionIndex < 0) {
            insertionIndex = ~insertionIndex;
        }
        xTags.add(x);
        MultiYScan scan = new MultiYScan();
        for (int i = 0; i < planeIDs.length; i++) {
            String planeID = planeIDs[i];
            Units zUnits = (Units)zUnitsMap.get(planeID);
            if (zUnits == null) {
                zUnits = Units.dimensionless;
                addPlane(planeID, zUnits);
            }
            double[] z = scans[i].toDoubleArray(zUnits);
            //assert(y.length == z.length);
            scan.put(planeID, z);
            scan.setYTags(y);
            zValues.add(insertionIndex, scan);
        }
    }
    
    
    private void appendProperties( Map properties ) {
        for ( Iterator i=properties.keySet().iterator(); i.hasNext(); ) {
            Object key= i.next();
            if ( this.properties.containsKey(key) ) {
                if ( key.equals( DataSet.PROPERTY_SIZE_BYTES ) ) {
                    // this is reset anyway
                } else if ( key.equals( DataSet.PROPERTY_CACHE_TAG ) ) {
                    CacheTag tag= (CacheTag)this.properties.get(key);
                    CacheTag appendTag= (CacheTag)properties.get(key);
                    try {
                        this.properties.put( key, CacheTag.append( tag, appendTag ) );
                    } catch ( IllegalArgumentException e ) {
                        System.err.println("ignoring unequal property: append: "+key+"="+properties.get(key)+" to "+this.properties.get(key ) ); 
                    }
                } else if ( !this.properties.get(key).equals(properties.get(key)) ) {
                    System.err.println( "ignoring unequal property: append: "+key+"="+properties.get(key)+" to "+this.properties.get(key ) ); 
                }
            } else {
                this.properties.put( key, properties.get(key) );
            }
        }
    }
    
    public void append(TableDataSet tds) {        
        appendProperties( tds.getProperties() );
        String[] planeIDs = (String[])this.planeIDs.toArray(new String[this.planeIDs.size()]);
        TableDataSet[] planes = new TableDataSet[planeIDs.length];
        planes[0] = tds;
        for (int i = 1; i < planeIDs.length; i++) {
            planes[i] = (TableDataSet)tds.getPlanarView(planeIDs[i]);
        }
        DatumVector[] z = new DatumVector[planes.length];
        for (int table = 0; table < tds.tableCount(); table++) {
            DatumVector y = getYTagsDatumVector(tds, table);
            for (int i = tds.tableStart(table); i < tds.tableEnd(table); i++) {
                for (int p = 0; p < planes.length; p++) {
                    z[p] = getZScanDatumVector(planes[p], table, i);
                }
                insertYScan(tds.getXTagDatum(i), y, z, planeIDs);
            }
        }
    }
    
    private DatumVector getYTagsDatumVector(TableDataSet tds, int table) {
        double[] yTags = new double[tds.getYLength(table)];
        for (int j = 0; j < tds.getYLength(table); j++) {
            yTags[j] = tds.getYTagDouble(table, j, yUnits);
        }
        return DatumVector.newDatumVector(yTags, yUnits);
    }
    
    private DatumVector getZScanDatumVector(TableDataSet tds, int table, int i) {
        if (tds == null) {
            return null;
        }
        double[] scan = new double[tds.getYLength(table)];
        for (int j = 0; j < tds.getYLength(table); j++) {
            scan[j] = tds.getDouble(i, j, yUnits);
        }
        return DatumVector.newDatumVector(scan, yUnits);
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
        return "TableDataSetBuilder ["+xTags.size()+" xtags, "+getTableCount(zValues)+"tables]";
    }
    
    public TableDataSet toTableDataSet() {
        int count = getTableCount(zValues);
        int[] tableOffsets = getTableOffsets(zValues, count);
        double[][] collapsedYTags = collapseYTags(zValues, count);
        double[][][] collapsedZValues = collapseZValues(zValues, planeIDs, zUnitsMap);
        Units[] zUnitsArray = getUnitsArray(planeIDs, zUnitsMap);
        return new DefaultTableDataSet(xTags.toArray(), xUnits, 
                collapsedYTags, yUnits, 
                collapsedZValues, zUnitsArray, 
                (String[])planeIDs.toArray(new String[planeIDs.size()]), tableOffsets, 
                properties, tableProperties );
    }
    
    public int getXLength() {
        return xTags.size();
    }
    
    public double getXTag(int i) {
        return xTags.get(i);
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
                    Arrays.fill(z, units.getFillDouble());
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
            if (d1.length != d2.length) {
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
