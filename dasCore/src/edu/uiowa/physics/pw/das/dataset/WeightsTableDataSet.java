/*
 * WeightsTableDataSet.java
 *
 * Created on November 29, 2005, 3:42 PM
 *
 *
 */

package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.datum.Units;
import java.util.HashMap;

/**
 * WeightsTableDataSet wraps a TableDataSet and returns 0.0 if the data point is
 * not valid, and non-zero (generally one) otherwise.
 *
 * This is intended to provide a consistent way to get the weights without having
 * to handle the case where the weights plane doesn't exist.
 *
 * @author Jeremy
 */
public class WeightsTableDataSet implements TableDataSet {
    TableDataSet source;
    Units sourceUnits;
    double fill;
    
    public static TableDataSet create( TableDataSet source ) {
        if ( source.getPlanarView(DataSet.PROPERTY_PLANE_WEIGHTS)!=null ) {
            return (TableDataSet)source.getPlanarView(DataSet.PROPERTY_PLANE_WEIGHTS);
        } else {
            return new WeightsTableDataSet( source );
        }
    }
    
    private WeightsTableDataSet( TableDataSet source ) {
        this.source= source;
        this.sourceUnits= source.getZUnits();
        this.fill= source.getZUnits().getFillDouble();
    }
    
    public edu.uiowa.physics.pw.das.datum.Datum getDatum(int i, int j) {
        return Units.dimensionless.createDatum( getDouble( i, j, Units.dimensionless ) );
    }

    public double getDouble(int i, int j, edu.uiowa.physics.pw.das.datum.Units units) {        
        return ( sourceUnits.isFill(source.getDouble( i, j, sourceUnits )) ) ? 0.0 : 1.0;
    }

    public double[] getDoubleScan(int i, edu.uiowa.physics.pw.das.datum.Units units) {
        throw new IllegalStateException("not implemented");
    }

    public int getInt(int i, int j, edu.uiowa.physics.pw.das.datum.Units units) {
        return ( source.getDouble( i, j, sourceUnits ) != fill ) ? 1 : 0;
    }

    public DataSet getPlanarView(String planeID) {
        return this;
    }

    public String[] getPlaneIds() {
        return new String[] { "" };
    }

    public java.util.Map getProperties() {
        return new HashMap();
    }

    public Object getProperty(String name) {
        return null;
    }

    public edu.uiowa.physics.pw.das.datum.DatumVector getScan(int i) {
        throw new IllegalStateException("not implemented");
    }

    public int getXLength() {
        return source.getXLength();
    }

    public VectorDataSet getXSlice(int i) {
        throw new IllegalStateException("not implemented");
    }

    public edu.uiowa.physics.pw.das.datum.Datum getXTagDatum(int i) {
        return source.getXTagDatum(i);
    }

    public double getXTagDouble(int i, edu.uiowa.physics.pw.das.datum.Units units) {
        return source.getXTagDouble( i, units );
    }

    public int getXTagInt(int i, edu.uiowa.physics.pw.das.datum.Units units) {
        return source.getXTagInt(i, units);
    }

    public edu.uiowa.physics.pw.das.datum.Units getXUnits() {
        return source.getXUnits();
    }

    public int getYLength(int table) {
        return source.getYLength(table);
    }

    public VectorDataSet getYSlice(int j, int table) {
        throw new IllegalStateException("not implemented");
    }

    public edu.uiowa.physics.pw.das.datum.Datum getYTagDatum(int table, int j) {
        return source.getYTagDatum(table, j );
    }

    public double getYTagDouble(int table, int j, edu.uiowa.physics.pw.das.datum.Units units) {
        return source.getYTagDouble(table, j, units);
    }

    public int getYTagInt(int table, int j, edu.uiowa.physics.pw.das.datum.Units units) {
        return source.getYTagInt(table, j, units);
    }

    public edu.uiowa.physics.pw.das.datum.DatumVector getYTags(int table) {
        return source.getYTags(table);
    }

    public edu.uiowa.physics.pw.das.datum.Units getYUnits() {
        return source.getYUnits();
    }

    public edu.uiowa.physics.pw.das.datum.Units getZUnits() {
        return Units.dimensionless;
    }

    public int tableCount() {
        return source.tableCount();
    }

    public int tableEnd(int table) {
        return source.tableEnd(table);
    }

    public int tableOfIndex(int i) {
        return source.tableOfIndex(i);
    }

    public int tableStart(int table) {
        return source.tableStart(table);
    }

    public Object getProperty(int table, String name) {
        return source.getProperty(table,name);
    }
    
}
