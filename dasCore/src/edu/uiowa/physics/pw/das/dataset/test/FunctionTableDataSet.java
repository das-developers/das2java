/* File: RipplesDataSet.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on November 18, 2003, 12:52 PM by __FULLNAME__ <__EMAIL__>
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


package edu.uiowa.physics.pw.das.dataset.test;

import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;
import java.util.*;

/**
 *
 * @author  jbf
 */
public abstract class FunctionTableDataSet implements TableDataSet {
    
    Units zUnits= Units.dimensionless;
    Units yUnits= Units.dimensionless;
    Units xUnits= Units.dimensionless;
    protected int ytags;
    protected int xtags;
    
    double[] data;        
    
    public abstract double getDoubleImpl(int i, int j, Units units);
    
    public FunctionTableDataSet( int nx, int ny ) {
        xtags= nx;
        ytags= ny;
        data= new double[nx*ny];
        for ( int i=0; i<nx; i++ ) {
            for ( int j=0; j<ny; j++ ) {
                int idx= i+j*xtags;            
                data[idx]= getDoubleImpl(i,j,Units.dimensionless);
            }
        }
    }

    protected void fillCache() {
        data= new double[xtags*ytags];
        for ( int i=0; i<xtags; i++ ) {
            for ( int j=0; j<ytags; j++ ) {
                int idx= i+j*xtags;            
                data[idx]= getDoubleImpl(i,j,Units.dimensionless);
            }
        }
    }
    
    public double getDouble( int i, int j, Units units ) {
        int idx= i+j*xtags;
        return data[idx];
    }
    
    public Datum getDatum(int i, int j) {
        return zUnits.createDatum(getDouble(i,j,zUnits));
    }
    
    public double[] getDoubleScan(int i, Units units) {
        int yLength = getYLength(tableOfIndex(i));
        double[] array = new double[yLength];
        for (int j = 0; j < yLength; j++) {
            array[j] = getDouble(i, j, units);
        }
        return array;
    }
    
    public DatumVector getScan(int i) {
        return DatumVector.newDatumVector(getDoubleScan(i, zUnits), zUnits);
    }
    
    public int getInt(int i, int j, Units units) {
        return (int)getDouble(i, j, units);
    }
    
    public DataSet getPlanarView(String planeID) {
        return null;
    }

    public String[] getPlaneIds() {
        return new String[0];
    }
    
    public Object getProperty(String name) {
        return null;
    }
    
    public Map getProperties() {
        return new HashMap();
    }
    
    public int getXLength() {
        return xtags;
    }
    
    public VectorDataSet getXSlice(int i) {
        return new XSliceDataSet(this, i );
    }
    
    public Datum getXTagDatum(int i) {
        return xUnits.createDatum(getXTagDouble(i,xUnits));
    }
    
    public double getXTagDouble(int i, Units units) {
        return xUnits.convertDoubleTo(units, (double)i);
    }
    
    public int getXTagInt(int i, Units units) {
        return (int)getXTagDouble(i,units);
    }
    
    public Units getXUnits() {
        return xUnits;
    }
    
    public int getYLength(int table) {
        return ytags;
    }
    
    public VectorDataSet getYSlice(int j, int table) {
        return new YSliceDataSet(this, j, table);
    }
    
    public Datum getYTagDatum(int table, int j) {
        return yUnits.createDatum(getYTagDouble(table, j, yUnits));
    }
    
    public double getYTagDouble(int table, int j, Units units) {
        if ( table>0 ) throw new IllegalArgumentException("table doesn't exist: "+table);
        return yUnits.convertDoubleTo(units, (double)j);
    }
    
    public int getYTagInt(int table, int j, Units units) {
        return (int)getYTagDouble(table, j, units);
    }
    
    public Units getYUnits() {
        return yUnits;
    }
    
    public Units getZUnits() {
        return zUnits;
    }
    
    public int tableCount() {
        return 1;
    }
    
    public int tableEnd(int table) {
        return xtags;
    }
    
    public int tableOfIndex(int i) {
        return 0;
    }
    
    public int tableStart(int table) {
        return 0;
    }
    
    public DatumVector getYTags(int table) {
        double[] tags = new double[getYLength(table)];
        for (int j = 0; j < tags.length; j++) {
            tags[j] = getYTagDouble(table, j, yUnits);
        }
        return DatumVector.newDatumVector(tags, yUnits);
    }
    
    public String toString() {
        return TableUtil.toString(this);
    }
}
