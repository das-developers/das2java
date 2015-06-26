/* File: DefaultTableDataSet.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on October 30, 2003, 11:20 AM
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
import org.das2.datum.UnitsConverter;
import org.das2.system.DasLogger;
import java.io.PrintStream;

import java.util.*;
import java.text.MessageFormat;
import java.util.logging.Logger;
import static org.das2.dataset.DataSetAdapter.adaptSubstitutions;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.JoinDataSet;
import org.virbo.dataset.QDataSet;

/**
 *
 * @author  Edward West
 */
public final class DefaultTableDataSet extends AbstractTableDataSet {
    
    /**
     * Description of indices
     * From left to right:
     * 1. Index of the plane.  0 is primary
     * 2. Index of the yscan within the dataset.
     * 3. Index of the zValue within the yscan.
     */
    private double[][][] tableData;
    
    private Units[] zUnits;
    
    private double[][] yTags;
    final int tableCount;
    
    private int[] tableOffsets;
    
    private String[] planeIDs;
    
    private static final Logger logger= DasLogger.getLogger();
    
    /** Creates a new instance of DefaultTableDataSet for tables where the
     * table geometry changes, and the DataSet contains multiple planes.
     */
    public DefaultTableDataSet(double[] xTags, Units xUnits,
            double[][] yTags, Units yUnits,
            double[][][] zValues, Units zUnits,
            Map zValuesMap, Map zUnitsMap,
            Map properties) {
        super(xTags, xUnits, yUnits, zUnits, properties);
        if (zValuesMap == null ^ zUnitsMap == null) {
            throw new IllegalArgumentException("zValuesMap == null ^ zUnitsMap == null");
        }
        if (zValuesMap != null && !zValuesMap.keySet().equals(zUnitsMap.keySet())) {
            throw new IllegalArgumentException("mismatched keySets for zValuesMap and zUnitsMap");
        }
        if (zValuesMap != null) {
            for (Iterator it = zValuesMap.keySet().iterator(); it.hasNext();) {
                if (!(it.next() instanceof String)) {
                    throw new IllegalArgumentException("Non-String key found in zValuesMap");
                }
            }
        }
        int planeCount = 1 + (zValuesMap == null ? 0 : zValuesMap.size());
        this.tableData = new double[planeCount][][];
        this.tableData[0] = flatten(zValues);
        this.yTags = copy(yTags);
        this.tableCount= yTags.length;
        this.zUnits = new Units[planeCount];
        this.zUnits[0] = zUnits;
        this.planeIDs = new String[planeCount];
        this.planeIDs[0] = "";
        tableOffsets = computeTableOffsets(zValues, yTags);
        if (zValuesMap != null) {
            int index = 1;
            for (Iterator it = new TreeMap(zValuesMap).entrySet().iterator(); it.hasNext();) {
                Map.Entry entry = (Map.Entry)it.next();
                String key = (String)entry.getKey();
                double[][][] d = (double[][][])entry.getValue();
                double[][] f = flatten(d);
                this.planeIDs[index] = key;
                this.tableData[index] = f;
                this.zUnits[index] = (Units)zUnitsMap.get(key);
            }
        }
    }
    
    private double[][] flatten(double[][][] d) {
        int sum = 0;
        for (int index = 0; index < d.length; index++) {
            sum += d[index].length;
        }
        double[][] flat = new double[sum][];
        int offset = 0;
        for (int i = 0; i < d.length; i++) {
            for (int j = 0; j < d[i].length; j++) {
                flat[offset] = (double[])d[i][j].clone();
                offset++;
            }
        }
        return flat;
    }
    
    public static DefaultTableDataSet createSimple( double[] xTags, double[] yTags, double[][] zValues ) {
        int nx= zValues.length;
        int ny= zValues[0].length;
        if ( xTags.length!=nx ) {
            throw new IllegalArgumentException("xTags ("+xTags.length+") don't match zValues' first dimension ("+nx+","+ny+")." );
        }
        if ( yTags.length!=ny ) {
            throw new IllegalArgumentException("yTags ("+yTags.length+") don't match zValues' first dimension ("+nx+","+ny+")." );
        }
        return new DefaultTableDataSet( xTags, Units.dimensionless, yTags, Units.dimensionless, zValues, Units.dimensionless, new HashMap() );
    }
    
    /** Creates a DefaultTableDataSet when the table geometry changes. */
    public DefaultTableDataSet(double[] xTags, Units xUnits,
            double[] yTags, Units yUnits,
            double[][] zValues, Units zUnits,
            Map properties) {
        this(xTags, xUnits, new double[][]{yTags}, yUnits, new double[][][]{zValues}, zUnits, null, null, properties);
    }
    
    DefaultTableDataSet(double[] xTags, Units xUnits,
            double[][] yTags, Units yUnits,
            double[][][] zValues, Units[] zUnits,
            String[] planeIDs, int[] tableOffsets,
            Map properties) {
        super(xTags, xUnits, yUnits, zUnits[0], properties);
        this.yTags = yTags;
        this.tableCount= yTags.length;
        this.tableData = zValues;
        this.zUnits = zUnits;
        this.planeIDs = planeIDs;
        this.tableOffsets = tableOffsets;
    }
    
    DefaultTableDataSet(double[] xTags, Units xUnits,
            double[][] yTags, Units yUnits,
            double[][][] zValues, Units[] zUnits,
            String[] planeIDs, int[] tableOffsets,
            Map properties, List<Map> tableProperties ) {
        super(xTags, xUnits, yUnits, zUnits[0], properties);
        this.yTags = yTags;
        this.tableCount= yTags.length;
        this.tableData = zValues;
        this.zUnits = zUnits;
        this.planeIDs = planeIDs;
        this.tableOffsets = tableOffsets;
        this.tableProperties= tableProperties;
    }
    
    private static double[][] copy(double[][] d) {
        double[][] copy = new double[d.length][];
        for (int index = 0; index < d.length; index++) {
            copy[index] = (double[])d[index].clone();
        }
        return copy;
    }
    
    private static int[] computeTableOffsets(double[][][] tableData, double[][] yTags) {
        int[] tableOffsets = new int[tableData.length];
        int currentOffset = 0;
        for (int index = 0; index < tableOffsets.length; index++) {
            tableOffsets[index] = currentOffset;
            currentOffset += tableData[index].length;;
        }
        return tableOffsets;
    }
    
    public Datum getDatum(int i, int j) {
        int table = tableOfIndex(i);
        if (i < 0 || i >= tableData[0].length) {
            IndexOutOfBoundsException ioobe = new IndexOutOfBoundsException
                    ("x index is out of bounds: " + i + " xLength: " + getXLength());
            logger.throwing(DefaultTableDataSet.class.getName(),
                    "getDatum(int,int)", ioobe);
            throw ioobe;
        }
        if (j < 0 || j >= this.yTags[table].length) {
            IndexOutOfBoundsException ioobe = new IndexOutOfBoundsException
                    ("y index is out of bounds: " + i + " yLength(" + table + "): " + getYLength(table));
            logger.throwing(DefaultTableDataSet.class.getName(),
                    "getDatum(int,int)", ioobe);
            throw ioobe;
        }
        try {
            double value = tableData[0][i][j];
            return Datum.create(value, zUnits[0]);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            throw aioobe; //This is just here so developers can put a breakpoint
            //here if ArrayIndexOutOfBoundsException is thrown.
        }
    }
    
    public DatumVector getScan(int i) {
        int table = tableOfIndex(i);
        if (i < 0 || i >= tableData[0].length) {
            IndexOutOfBoundsException ioobe = new IndexOutOfBoundsException
                    ("x index is out of bounds: " + i + " xLength: " + getXLength());
            logger.throwing(DefaultTableDataSet.class.getName(),
                    "getDatum(int,int)", ioobe);
            throw ioobe;
        }
        try {
            double[] values = tableData[0][i];
            return DatumVector.newDatumVector(values, 0, getYLength(table), zUnits[0]);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            throw aioobe; //This is just here so developers can put a breakpoint
            //here if ArrayIndexOutOfBoundsException is thrown.
        }
    }
    
    public double getDouble(int i, int j, Units units) {
        int table;
        int yLength;
        
        if (i < 0 || i >= getXLength()) {
            throw new IndexOutOfBoundsException("i: " + i + ", xLength: " + getXLength());
        }
        
        table = tableOfIndex(i);
        yLength = yTags[table].length;
        
        if (j < 0 || j >= yLength) {
            throw new IndexOutOfBoundsException("j: " + j + ", yLength: " + yLength);
        }
        double value = tableData[0][i][j];
        if (units == getZUnits()) {
            return value;
        }
        return zUnits[0].getConverter(units).convert(value);
    }
    
    public double[] getDoubleScan(int i, Units units) {
        int table = tableOfIndex(i);
        int yLength = yTags[table].length;
        double[] values = tableData[0][i];
        double[] retValues = new double[yLength];
        if (units == getZUnits()) {
            System.arraycopy(values, 0, retValues, 0, yLength);
        } else {
            UnitsConverter uc = zUnits[0].getConverter(units);
            for (int j = 0; j < yLength; j++) {
                retValues[j] = uc.convert(values[j]);
            }
        }
        return retValues;
    }
    
    public int getInt(int i, int j, Units units) {
        return (int)Math.round(getDouble(i, j, units));
    }
    
    public DataSet getPlanarView(String planeID) {
        int planeIndex = -1;
        for (int index = 0; index < planeIDs.length; index++) {
            if (planeIDs[index].equals(planeID)) {
                planeIndex = index;
            }
        }
        if (planeIndex == -1) {
            return null;
        } else {
            return new PlanarViewDataSet(planeIndex);
        }
    }
    
    public String[] getPlaneIds() {
        String[] result= new String[planeIDs.length];
        System.arraycopy( planeIDs, 0, result, 0, planeIDs.length );
        return result;
    }
    
    /**
     * this is a highly-interleaved table, and if we sort it out and create a 
     * optimal QDataSet, it's better.
     * @return a QDataSet version of the data
     */
    public org.virbo.dataset.AbstractDataSet toQDataSet( ) {
  
        JoinDataSet result= new JoinDataSet(3);
        
        Map<String,Object> dasProps = adaptSubstitutions(getProperties());
				
	// Save properterties with value substitution strings in Autoplot Stlye
	result.putProperty( QDataSet.USER_PROPERTIES, dasProps );

        Set<double[]> doneModes= new HashSet<double[]>();
        int itable=-1;
        for ( int i=0; i<getXLength(); i++ ) {
            if ( itable<(tableOffsets.length-1) && i==tableOffsets[itable+1] ) {
                itable++;
            }
            double[] m= yTags[itable];
            if ( doneModes.contains(m) ) {
                continue;
            } else {
                ArrayList indeces= new ArrayList();
                int itable1= itable;
                int ntable1= yTags.length;
                for ( int i1= i; i1<getXLength(); i1++ ) {
                    boolean sameTable= yTags[itable1]==m;
                    if ( sameTable ) {
                        for ( i1=tableOffsets[itable1]; i1<getXLength() && ( itable1==ntable1-1 || i1<tableOffsets[itable1+1] ); i1++ ) {
                            indeces.add( i1 );
                        }
                        itable1++;
                    } else {
                        itable1++;
                        if ( itable1<tableCount ) {
                            i1= tableOffsets[itable1]-1; //-1 is because we increment in for loop.
                        } else {
                            i1= getXLength()-1;
                        }
                    }
                }
                System.err.println("indeces="+indeces.size());
                double[] xTags= new double[ indeces.size() ];
                int j=0;
                double[] back= new double[ indeces.size()*m.length ];
                itable1= itable;
                for ( int i1= i; i1<getXLength(); i1++ ) {
                    if ( itable1<(tableOffsets.length-1) && i1==tableOffsets[itable1+1] ) {
                        itable1++;
                    }
                    if ( yTags[itable1]==m ) {
                        xTags[j]= getXTagDouble( i1,getXUnits() );
                        System.arraycopy( tableData[0][i1], 0, back, j*m.length, m.length );
                        j++;
                    }
                }
                DDataSet table1= DDataSet.wrap(back,new int[] {indeces.size(),m.length} );
                DDataSet xTagsDs= DDataSet.wrap(xTags);
                xTagsDs.putProperty( QDataSet.UNITS, getXUnits() );
                xTagsDs.putProperty( QDataSet.LABEL, dasProps.get( PROPERTY_X_LABEL ) );
                xTagsDs.putProperty( QDataSet.MONOTONIC, dasProps.get( PROPERTY_X_MONOTONIC ) );
                table1.putProperty( QDataSet.DEPEND_0, xTagsDs );
                DDataSet yTagsDs=  DDataSet.wrap(yTags[itable]);
                yTagsDs.putProperty( QDataSet.UNITS, getYUnits() );
                yTagsDs.putProperty( QDataSet.LABEL, dasProps.get( PROPERTY_Y_LABEL ) );
                table1.putProperty( QDataSet.DEPEND_1,yTagsDs);
                result.join(table1);
                doneModes.add(m);
            }
        }
        result.putProperty( QDataSet.UNITS, getZUnits() );
        result.putProperty( QDataSet.LABEL, dasProps.get( PROPERTY_Z_LABEL ) );
        result.putProperty( QDataSet.TITLE, dasProps.get( PROPERTY_TITLE ) );
        return result;
    }

    public int getYLength(int table) {
        return yTags[table].length;
    }
    
    public Datum getYTagDatum(int table, int j) {
        double value = yTags[table][j];
        return Datum.create(value, getYUnits());
    }
    
    public double getYTagDouble(int table, int j, Units units) {
        double value = yTags[table][j];
        if (units == getYUnits()) {
            return value;
        }
        return getYUnits().getConverter(units).convert(value);
    }
    
    public int getYTagInt(int table, int j, Units units) {
        return (int)Math.round(getYTagDouble(table, j, units));
    }
    
    public DatumVector getYTags(int table) {
        return DatumVector.newDatumVector(yTags[table], 0, getYLength(table), getYUnits());
    }
    
    public int tableCount() {
        return tableCount;
    }
    
    public int tableEnd(int table) {
        if (table == tableOffsets.length - 1) {
            return getXLength();
        } else {
            return tableOffsets[table + 1];
        }
    }
    
    public int tableOfIndex(int i) {
        if ( yTags.length>5 ) {
            int table = Arrays.binarySearch(tableOffsets, i);
            if (i >= getXLength()) {
                throw new IndexOutOfBoundsException(i + " > " + getXLength());
            }
            if (table < 0) {
                table = (~table) - 1;
            }
            return table;
        } else {
            int result= tableCount-1;
            while ( result>=0 && tableOffsets[result]>i ) result--;
            return result;
        }
    }
    
    public int tableStart(int table) {
        return tableOffsets[table];
    }
    
    public void dump(java.io.PrintStream out) {
        MessageFormat tableFormat = new MessageFormat(
                "        {0,number,00}. Y Length: {1,number,000}, Start: {2,number,000}, End: {3,number,000}");
        MessageFormat planeFormat = new MessageFormat("        ID: {0}, Z Units: ''{1}''");
        out.println("============================================================");
        out.println(getClass().getName());
        out.println("    X Length:    " + getXLength());
        out.println("    X Units:     '" + getXUnits() + "'");
        out.print("    X Tags:");
        for (int index = 0; index < getXLength(); index ++) {
            out.print(" ");
            out.print(getXTagDouble(index, getXUnits()));
        }
        out.println();
        out.println("    Y Units:     '" + getYUnits() + "'");
        out.println("    Z Units:     '" + getZUnits() + "'");
        out.println("    Table Count: " + tableCount());
        Object[] args = new Object[4];
        for (int table = 0; table < tableCount(); table++) {
            args[0] = table;
            args[1] = yTags[table].length;
            args[2] = tableStart(table);
            args[3] = tableEnd(table);
            String str = tableFormat.format(args);
            out.println(str);
            out.print("        Y Tags:");
            for (int index = 0; index < getYLength(table); index ++) {
                out.print(" ");
                out.print(getYTagDouble(table, index, getXUnits()));
            }
            out.println();
            out.println("        Z Values:");
            for (int j = 0; j < getYLength(table); j++) {
                out.print("        ");
                for (int i = tableStart(table); i < tableEnd(table); i++) {
                    out.print(getDouble(i, j, getZUnits()));
                    out.print("\t");
                }
                out.println();
            }
        }
        out.println("    Plane Count: " + planeIDs.length);
        for (int plane = 1; plane < planeIDs.length; plane++) {
            args[0] = planeIDs[plane];
            args[1] = zUnits[plane];
            String str = planeFormat.format(args);
            out.println(str);
        }
        out.println("============================================================");
    }
    
    public String toString() {
        return "DefaultTableDataSet "+TableUtil.toString(this);
    }
    
    public void printDebugInfo(PrintStream out) {
        out.println("xLength: " + getXLength());
        out.println("tableCount: " + tableCount());
        for (int table = 0; table < tableCount(); table++) {
            out.println("tableStart(" + table + "): " + tableStart(table));
            out.println("yLength: " + getYLength(table));
            for (int i = tableStart(table); i < tableEnd(table); i++) {
                out.println(i + ": " + tableData[0][i].length);
            }
            out.println("tableEnd(" + table + "): " + tableEnd(table));
        }
    }
    
    private final class PlanarViewDataSet extends AbstractDataSet.ViewDataSet implements TableDataSet {
        
        private final int index;
        
        private PlanarViewDataSet(int index) {
            DefaultTableDataSet.this.super();
            this.index = index;
        }
        
        public DataSet getPlanarView(String planeID) {
            return planeID.equals("") ? this : null;
        }
        
        public String[] getPlaneIds() {
            return new String[0];
        }
        
        public Datum getDatum(int i, int j) {
            double value = tableData[index][i][j];
            return Datum.create(value, zUnits[index]);
        }
        
        public double getDouble(int i, int j, Units units) {
            double value = tableData[index][i][j];
            return zUnits[index].getConverter(units).convert(value);
        }
        
        public double[] getDoubleScan(int i, Units units) {
            int table = tableOfIndex(i);
            int yLength = yTags[table].length;
            double[] values = tableData[index][i];
            double[] retValues = new double[yLength];
            if (units == getZUnits()) {
                System.arraycopy(values, 0, retValues, 0, yLength);
            } else {
                UnitsConverter uc = zUnits[index].getConverter(units);
                for (int j = 0; j < yLength; j++) {
                    retValues[j] = uc.convert(values[j]);
                }
            }
            return retValues;
        }
        
        public DatumVector getScan(int i) {
            int table = tableOfIndex(i);
            if (i < 0 || i >= tableData[0].length) {
                IndexOutOfBoundsException ioobe = new IndexOutOfBoundsException
                        ("x index is out of bounds: " + i + " xLength: " + getXLength());
                logger.throwing(DefaultTableDataSet.class.getName(),
                        "getDatum(int,int)", ioobe);
                throw ioobe;
            }
            try {
                double[] values = tableData[index][i];
                return DatumVector.newDatumVector(values, 0, getYLength(table), zUnits[index]);
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                throw aioobe; //This is just here so developers can put a breakpoint
                //here if ArrayIndexOutOfBoundsException is thrown.
            }
        }
        
        public int getInt(int i, int j, Units units) {
            return (int)Math.round(getDouble(i, j, units));
        }
        
        public VectorDataSet getXSlice(int i) {
            return new XSliceDataSet(this, i);
        }
        
        public int getYLength(int table) {
            return DefaultTableDataSet.this.getYLength(table);
        }
        
        public VectorDataSet getYSlice(int j, int table) {
            return new YSliceDataSet(this, j, table);
        }
        
        public Datum getYTagDatum(int table, int j) {
            return DefaultTableDataSet.this.getYTagDatum(table, j);
        }
        
        public double getYTagDouble(int table, int j, Units units) {
            return DefaultTableDataSet.this.getYTagDouble(table, j, units);
        }
        
        public int getYTagInt(int table, int j, Units units) {
            return DefaultTableDataSet.this.getYTagInt(table, j, units);
        }
        
        public Units getZUnits() {
            return DefaultTableDataSet.this.zUnits[index];
        }
        
        public int tableCount() {
            return DefaultTableDataSet.this.tableCount();
        }
        
        public int tableEnd(int table) {
            return DefaultTableDataSet.this.tableEnd(table);
        }
        
        public int tableOfIndex(int i) {
            return DefaultTableDataSet.this.tableOfIndex(i);
        }
        
        public int tableStart(int table) {
            return DefaultTableDataSet.this.tableStart(table);
        }
        
        public Object getProperty(String name) {
            Object result= DefaultTableDataSet.this.getProperty(planeIDs[index] + "." + name);
            if ( result==null ) result= DefaultTableDataSet.this.getProperty(name);
            return result;
        }

        public Object getProperty(int table, String name) {
            Object result= DefaultTableDataSet.this.getProperty(table,planeIDs[index] + "." + name);
            if ( result==null ) result= DefaultTableDataSet.this.getProperty(table,name);
            return result;
        }
        
        public DatumVector getYTags(int table) {
            return DefaultTableDataSet.this.getYTags(table);
        }
        
        public String toString() {
            return TableUtil.toString(this);
        }
    }
    
}
