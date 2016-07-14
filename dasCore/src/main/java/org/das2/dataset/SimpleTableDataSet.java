/*
 * WritableTableDataSet.java
 *
 * Created on September 2, 2004, 11:58 AM
 */

package org.das2.dataset;

import org.das2.datum.Datum;
import org.das2.datum.DatumVector;
import org.das2.datum.Units;
import java.util.*;
import java.util.Map;

/**
 * optimized TableDataSet where only 1-table data set is supported,
 * and is backed by a 1-D array.  This is to house the result of the rebin method 
 * used for spectrograms.
 *
 * @author  Jeremy
 */
public class SimpleTableDataSet implements TableDataSet {
    
    double[] z;
    double[] x;
    double[] y;
    final int nx;
    final int ny;
    Units xunits;
    Units yunits;
    Units zunits;
    Map properties;
    TableDataSet auxPlane;
    String auxPlaneName;
    
    /* z must be stored with the y's adjacent */
    public SimpleTableDataSet( double[] x, double[]y, double[] z, Units xunits, Units yunits, Units zunits ) {
        this.z= z;
        this.x= x;
        this.y= y;
        this.nx= x.length;
        this.ny= y.length;
        this.xunits= xunits;
        this.yunits= yunits;
        this.zunits= zunits;
        auxPlaneName= null;
    }
    
    public SimpleTableDataSet( double[] x, double[]y, double[] z, Units xunits, Units yunits, Units zunits, String planeName, TableDataSet planeData ) {
        this( x, y, z, xunits, yunits, zunits );
        auxPlaneName= planeName;
        auxPlane= planeData;        // TODO: check units and dimensions
    }
    
    
    private final int indexOf( int i, int j ) {
        return i*ny + j;
    }
    
    public Datum getDatum(int i, int j) {
        return Datum.create( z[indexOf(i,j)], zunits );
    }
        
    public double getDouble(int i, int j, Units units) {
        return zunits.convertDoubleTo(units,z[indexOf(i,j)]);
    }
    
    public double[] getDoubleScan(int i, Units units) {
        throw new UnsupportedOperationException();
    }
    
    public int getInt(int i, int j, Units units) {
        throw new UnsupportedOperationException();
    }
    
    public DataSet getPlanarView(String planeID) {
        if ( planeID.equals( auxPlaneName ) ) return auxPlane; else return null;
    }
    
    public String[] getPlaneIds() {
        return new String[0];
    }
    
    public Object getProperty(String name) {
        return properties.get(name);
    }
            
    public Object getProperty( int table, String name) {
        return getProperty(name);
    }
    
    public DatumVector getScan(int i) {
        throw new UnsupportedOperationException();
    }
    
    public int getXLength() {
        return x.length;
    }
    
    public VectorDataSet getXSlice(int i) {
        return new XSliceDataSet( this, i );
    }
    
    public Datum getXTagDatum(int i) {
        return Datum.create( x[i], xunits );
    }
    
    public double getXTagDouble(int i, Units units) {
        return xunits.convertDoubleTo( units, x[i] );
    }
    
    public int getXTagInt(int i, Units units) {
        throw new UnsupportedOperationException();
    }
    
    public Units getXUnits() {
        return xunits;
    }
    
    public int getYLength(int table) {
        return y.length;
    }
    
    public VectorDataSet getYSlice(int j, int table) {
        return new YSliceDataSet( this, j, table );
    }
    
    public Datum getYTagDatum(int table, int j) {
        return Datum.create( y[j], yunits );
    }
    
    public double getYTagDouble(int table, int j, Units units) {
        return yunits.convertDoubleTo(units,y[j]);
    }
    
    public void setYTagDouble( int table, int j, double yvalue, Units units ) {
        y[j]= units.convertDoubleTo( yunits, yvalue );
    }
    
    public int getYTagInt(int table, int j, Units units) {
        throw new UnsupportedOperationException();
    }
    
    public DatumVector getYTags(int table) {
        return DatumVector.newDatumVector(y,yunits);
    }
    
    public Units getYUnits() {
        return yunits;
    }
    
    public Units getZUnits() {
        return zunits;
    }
    
    public int tableCount() {
        return 1;
    }
    
    public int tableEnd(int table) {
        return x.length;
    }
    
    public int tableOfIndex(int i) {
        return 0;
    }
    
    public int tableStart(int table) {
        return 0;
    }
    
    public Map getProperties() {
        return new HashMap(properties);
    }
    
}
