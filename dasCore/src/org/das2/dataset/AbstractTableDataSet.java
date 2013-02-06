/* File: AbstractTableDataSet.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on October 27, 2003, 2:44 PM
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
public abstract class AbstractTableDataSet extends AbstractDataSet implements DataSet, TableDataSet {
    
    private Units zUnits;
    
    protected List<Map> tableProperties;
    
    /** Creates a new instance of AbstractTableDataSet */
    public AbstractTableDataSet(double[] xTags, Units xUnits, Units yUnits, Units zUnits, Map properties) {
        super(xTags, xUnits, yUnits, properties);
        this.zUnits = zUnits;
        this.tableProperties= null;
    }
    
    public Units getZUnits() {
        return zUnits;
    }
    
    public VectorDataSet getXSlice(int i) {
        return new XSliceDataSet(this, i);
    }
    
    public VectorDataSet getYSlice(int j, int table) {
        return new YSliceDataSet(this, j, table);
    }
    
    public String toString() {
        return TableUtil.toString(this);
    }
    
    public Object getProperty(int table, String name) {
        if ( tableProperties!=null ) {
            return tableProperties.get(table).get(name);
        } else {
            return null;
        }
    }
    
    protected static class XSliceDataSet extends AbstractDataSet.ViewDataSet implements VectorDataSet{
        
        private int iIndex;
        private TableDataSet ds;
        
        protected XSliceDataSet(AbstractDataSet ds, int i) {
            ds.super();
            this.ds = (TableDataSet)ds;
            this.iIndex = i;
        }
                
        public DataSet getPlanarView(String planeID) {
            return new XSliceDataSet( (AbstractDataSet)ds.getPlanarView(planeID), iIndex );
        }
        
        public String[] getPlaneIds() {
            return ds.getPlaneIds();
        }
        
        public Datum getDatum(int i) {
            return ds.getDatum(iIndex, i);
        }
        
        public double getDouble(int i, Units units) {
            return ds.getDouble(iIndex, i, units);
        }
        
        public int getInt(int i, Units units) {
            return ds.getInt(iIndex, i, units);
        }
        
        public Datum getXTagDatum(int i) {
            int table = ds.tableOfIndex(iIndex);
            return ds.getYTagDatum(table, i);
        }
        
        public int getXLength() {
            int table = ds.tableOfIndex(iIndex);
            return ds.getYLength(table);
        }
        
        public Units getXUnits() {
            return ds.getYUnits();
        }
        
        public double getXTagDouble(int i, Units units) {
            int table = ds.tableOfIndex(iIndex);
            return ds.getYTagDouble(table, i, units);
        }
        
        public Units getYUnits() {
            return ds.getZUnits();
        }
        
        public int getXTagInt(int i, Units units) {
            int table = ds.tableOfIndex(iIndex);
            return ds.getYTagInt(table, i, units);
        }
        
        public Object getProperty(String name) {
            return null;
        }
        
        public String toString() {
            return VectorUtil.toString(this);
        }

        public Map getProperties() {
            return new HashMap();
        }
        
    }
    
    protected static class YSliceDataSet extends AbstractDataSet.ViewDataSet implements VectorDataSet {
        
        private final int table;
        private final int jIndex;
        private final TableDataSet ds;
        
        protected YSliceDataSet(AbstractDataSet ds, int jIndex, int table) {
            ds.super();
            this.jIndex = jIndex;
            this.table = table;
            this.ds = (TableDataSet)ds;
        }
        
        public DataSet getPlanarView(String planeID) {
            return new YSliceDataSet( (AbstractDataSet)ds.getPlanarView(planeID), jIndex, table );
        }
        
        public String[] getPlaneIds() {
            return ds.getPlaneIds();
        }
                
        public Datum getDatum(int i) {
            int offset = ds.tableStart(table);
            return ds.getDatum(i + offset, jIndex);
        }
        
        public double getDouble(int i, Units units) {
            int offset = ds.tableStart(table);
            return ds.getDouble(i + offset, jIndex, units);
        }
        
        public int getInt(int i, Units units) {
            int offset = ds.tableStart(table);
            return ds.getInt(i + offset, jIndex, units);
        }
        
        public Datum getXTagDatum(int i) {
            int offset = ds.tableStart(table);
            return ds.getXTagDatum(i + offset);
        }
        
        public int getXLength() {
            return ds.tableEnd(table) - ds.tableStart(table);
        }
        
        public double getXTagDouble(int i, Units units) {
            int offset = ds.tableStart(table);
            return ds.getXTagDouble(i + offset, units);
        }
        
        public Units getYUnits() {
            return ds.getZUnits();
        }
        
        public int getXTagInt(int i, Units units) {
            int offset = ds.tableStart(table);
            return ds.getXTagInt(i + offset, units);
        }
        
        public Object getProperty(String name) {
            return null;
        }
        
        public Map getProperties() {
            return new HashMap();
        }
        
    }
}
