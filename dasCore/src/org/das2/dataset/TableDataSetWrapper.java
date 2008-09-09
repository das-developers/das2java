/*
 * SourceDataSetWrapper.java
 *
 * Created on December 7, 2004, 5:21 PM
 */

package org.das2.dataset;

import java.util.*;

/**
 *
 * @author  Jeremy
 *
 * The intended use of this class is to make it easy to change the behavior of a
 * TableDataSet by wrapping it with a subclass of this method.
 */
public class TableDataSetWrapper implements TableDataSet {
    
    protected TableDataSet source;
    
    /** Creates a new instance of SourceDataSetWrapper */
    public TableDataSetWrapper( TableDataSet source ) {
        if ( source==null ) throw new IllegalArgumentException("null source dataset");
        this.source= source;
    }
    
    public Map getProperties() {
        return source.getProperties();
    }
    
    public Object getProperty( String name ) {
        return source.getProperty(name);
    }
    
    public Object getProperty( int table, String name ) {
        return source.getProperty(table, name);
    }
    
    public String[] getPlaneIds() {
        return source.getPlaneIds();
    }

    public org.das2.datum.Datum getDatum(int i, int j) {
        return source.getDatum( i, j );
    }

    public double getDouble(int i, int j, org.das2.datum.Units units) {
        return source.getDouble( i, j, units );
    }

    public double[] getDoubleScan(int i, org.das2.datum.Units units) {
        return source.getDoubleScan( i, units );
    }

    public int getInt(int i, int j, org.das2.datum.Units units) {
        return source.getInt( i, j, units );
    }

    public DataSet getPlanarView(String planeID) {
        return source.getPlanarView( planeID );
    }

    public org.das2.datum.DatumVector getScan(int i) {
        return source.getScan(i);
    }

    public int getXLength() {
        return source.getXLength();
    }

    public VectorDataSet getXSlice(int i) {
        return source.getXSlice(i);
    }

    public org.das2.datum.Datum getXTagDatum(int i) {
        return source.getXTagDatum(i);
    }

    public double getXTagDouble(int i, org.das2.datum.Units units) {
        return source.getXTagDouble( i, units);
    }

    public int getXTagInt(int i, org.das2.datum.Units units) {
        return source.getXTagInt( i, units);
    }

    public org.das2.datum.Units getXUnits() {
        return source.getXUnits();
    }

    public int getYLength(int table) {
        return source.getYLength( table);
    }

    public VectorDataSet getYSlice(int j, int table) {
        return source.getYSlice( j, table);
    }

    public org.das2.datum.Datum getYTagDatum(int table, int j) {
        return source.getYTagDatum( table, j);
    }

    public double getYTagDouble(int table, int j, org.das2.datum.Units units) {
        return source.getYTagDouble( table, j, units);
    }

    public int getYTagInt(int table, int j, org.das2.datum.Units units) {
        return source.getYTagInt( table, j, units);
    }

    public org.das2.datum.DatumVector getYTags(int table) {
        return source.getYTags( table);
    }

    public org.das2.datum.Units getYUnits() {
        return source.getYUnits();
    }

    public org.das2.datum.Units getZUnits() {
        return source.getZUnits();
    }

    public int tableCount() {
        return source.tableCount();
    }

    public int tableEnd(int table) {
        return source.tableEnd( table);
    }

    public int tableOfIndex(int i) {
        return source.tableOfIndex(i);
    }

    public int tableStart(int table) {
        return source.tableStart( table);
    }
        
    
}
