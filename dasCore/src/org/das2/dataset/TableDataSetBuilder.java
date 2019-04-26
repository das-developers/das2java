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

import org.das2.datum.CacheTag;
import org.das2.datum.Units;
import org.das2.datum.DatumVector;
import org.das2.datum.Datum;
import java.util.*;
import java.util.Map.Entry;

/** Handles 1-N planes of table data.
 *
 * Update Note:  Historically by default there was an un-named plane, that was always
 *               present in the builder.  I removed that so that all planes could have a
 *               name, it probably breaks lots of stuff so extensive tests are needed
 *               before this change is committed. -cwp 2014-09-22
 * 
 * @author  Edward West
 */
public class TableDataSetBuilder {
    
    private static final double[] EMPTY = new double[0];
    
    private List xTags = new ArrayList();
    boolean monotonic= true;
    
    private List zValues = new ArrayList();
    
	// No default plane
   // private List planeIDs = new ArrayList(); {
   //     planeIDs.add("");
   // }
	 
	private List planeIDs = new ArrayList();
    
    private Units xUnits = Units.dimensionless;
    
    private Units yUnits = Units.dimensionless;
    
    private Map zUnitsMap = new HashMap();
    
    private SortedSet yTagSet = new TreeSet(new DoubleArrayComparator());
    
    private Map properties = new HashMap();
	 
	 // One property list for each sub-table
    private List<Map> tableProperties= new ArrayList<Map>();
    
    /** Creates a new instance of TableDataSetBuilder with a default plane
	  * A single plane with the empty string as it's name is defined.
	  * 
	  * @param xUnits Units for the X-axis data
	  * @param yUnits Units for the Y-axis data
	  * @param zUnits Units for the Z-axis data
	  */ 
    public TableDataSetBuilder( Units xUnits, Units yUnits, Units zUnits ) {
        this(xUnits, yUnits, zUnits, "");
    }
	 
	 /** Creates a new instance of TableDataSetBuilder with a named default plane
	  * 
	  * @param xUnits Units for the X-axis data
	  * @param yUnits Units for the Y-axis data
	  * @param zUnits Units for the Z-axis data
	  * @param name The name for data plane at index 0, may be the empty string "".
	  */
	 public TableDataSetBuilder( Units xUnits, Units yUnits, Units zUnits, String name ) {
      
		setXUnits(xUnits);
		setYUnits(yUnits);
		setZUnits(zUnits, name);
		planeIDs.add(name);
		tableProperties.add(null);
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
	 
	 public void setPlaneProperties(int table, Map properties){
		 tableProperties.set(table, properties);
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
	 
	 public void addPlane(String name, Units zUnits, Map properties){
		 if (name != null && !planeIDs.contains(name)) {
            planeIDs.add(name);
            zUnitsMap.put(name, zUnits );
				tableProperties.add(properties);
        }
	 }
    
    public void insertYScan( Datum x, DatumVector y, DatumVector z) {
        insertYScan(x, y, new DatumVector[]{z}, new String[]{""});
    }
    
    public void insertYScan( Datum x, DatumVector y, DatumVector z, String planeId ) {
        insertYScan( x, y, new DatumVector[]{z}, new String[]{ planeId } );
    }
    
    public void insertYScan(Datum xTag, DatumVector yTags, DatumVector[] scans, String[] planeIDs) {
        double x = xTag.doubleValue(xUnits);
        double[] y = yTags.toDoubleArray(yUnits);
        int insertionIndex = xTags.size();
        if ( xTags.size()>0 && ((Double)xTags.get(xTags.size()-1))>x ) monotonic=false;
        if (yTagSet.contains(y)) {
            y = (double[])yTagSet.tailSet(y).iterator().next();
        }
        else {
            yTagSet.add(y);
        }
        xTags.add(x);
        MultiYScan scan = new MultiYScan();
        for (int i = 0; i < planeIDs.length; i++) {
            String planeID = planeIDs[i];
            Units zUnits = (Units)zUnitsMap.get(planeID);
            if ( zUnits==null && i==0 ) {
                zUnits = (Units)zUnitsMap.get("");
            }
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
    
    
    private void appendProperties( Map<String,Object> properties ) {
        //TODO: this needs to be verified after findbugs refactoring
        Map<String,Object> sproperties= properties;
        for ( Entry<String,Object> e : sproperties.entrySet() ) {
            Object key= e.getKey();
            if ( this.properties.containsKey(key) ) {
                if ( key.equals( DataSet.PROPERTY_SIZE_BYTES ) ) {
                    // this is reset anyway
                } else if ( key.equals( DataSet.PROPERTY_CACHE_TAG ) ) {
                    CacheTag tag= (CacheTag)this.properties.get(key);
                    CacheTag appendTag= (CacheTag)e.getValue();
                    try {
                        this.properties.put( key, CacheTag.append( tag, appendTag ) );
                    } catch ( IllegalArgumentException ex ) {
                        System.err.println("ignoring unequal property: append: "+key+"="+e.getValue()+" to "+tag ); 
                    }
                } else if ( !this.properties.get(key).equals(e.getValue()) ) {
                    System.err.println( "ignoring unequal property: append: "+key+"="+e.getValue()+" to "+this.properties.get(key ) ); 
                }
            } else {
                this.properties.put( key, e.getValue() );
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
        return "TableDataSetBuilder ["+xTags.size()+" xtags, "+getTableCount(zValues)+"tables]";
    }
    
    public TableDataSet toTableDataSet() {
        int count = getTableCount(zValues);
        int[] tableOffsets = getTableOffsets(zValues, count);
        double[][] collapsedYTags = collapseYTags(zValues, count);
        double[][][] collapsedZValues = collapseZValues(zValues, planeIDs, zUnitsMap);
        double[] collapsedXTags=  collapseXTags(xTags,xTags.size());
        Units[] zUnitsArray = getUnitsArray(planeIDs, zUnitsMap);
        if ( monotonic ) {
            properties.put( DataSet.PROPERTY_X_MONOTONIC, Boolean.TRUE );
        }
        return new DefaultTableDataSet(collapsedXTags, xUnits,
                collapsedYTags, yUnits, 
                collapsedZValues, zUnitsArray, 
                (String[])planeIDs.toArray(new String[planeIDs.size()]), tableOffsets, 
                properties, tableProperties );
    }
    
    public int getXLength() {
        return xTags.size();
    }
    
    public double getXTag(int i) {
        return (Double)xTags.get(i);
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

    private static double[] collapseXTags( List list, int count ) {
        int index = 0;
        double[] result = new double[count];
        for ( int i=0; i<list.size(); i++ ) {
            result[index] = (Double)list.get(index);
            index++;
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
    
    private static class DoubleArrayComparator implements Comparator {
        /**
         * returns the negative when o1 is shorter of the two, or the o1 has the first element with a smaller value.
         * @param o1
         * @param o2
         * @return 
         */
        public int compare(Object o1, Object o2) {
            double[] d1 = (double[])o1;
            double[] d2 = (double[])o2;
            if (d1.length != d2.length) {
                return d1.length - d2.length;
            }
            
            for (int i = 0; i < d1.length; i++) {
                int c= Double.compare(d1[i],d2[i]);
                if ( c!=0 ) return c;
            }
            return 0;
        }
        
    }
    
    private static class MultiYScan {
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
