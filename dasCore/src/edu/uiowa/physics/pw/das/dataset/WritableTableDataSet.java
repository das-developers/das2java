/*
 * WritableTableDataSet.java
 *
 * Created on September 2, 2004, 11:58 AM
 */

package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.DatumVector;
import edu.uiowa.physics.pw.das.datum.Units;
import java.util.*;
import java.util.Map;

/**
 *
 * @author  Jeremy
 */
public class WritableTableDataSet implements TableDataSet {
    
    double[][] z;
    double[] x;
    double[] y;
    Units xunits;
    Units yunits;
    Units zunits;
    Map properties;    
    
    public static WritableTableDataSet newSimple( int nx, Units xunits, int ny, Units yunits, Units zunits ) {
        double [][] z= new double[nx][ny];
        double [] x= new double[nx];
        double [] y= new double[ny];
        return new WritableTableDataSet( x, xunits, y, yunits, z, zunits, new HashMap() );
    }
    
    public static WritableTableDataSet newEmpty( TableDataSet tds ) {
        if ( tds.tableCount() > 1 ) throw new IllegalArgumentException("only supported for simple tables");
        int nx= tds.tableEnd(0);
        int ny= tds.getYLength(0);
        WritableTableDataSet result= newSimple( nx, tds.getXUnits(), ny, tds.getYUnits(), tds.getZUnits() );
        for ( int i=0; i<nx; i++ ) {
            result.setXTagDouble( i, tds.getXTagDouble( i, tds.getXUnits() ), tds.getXUnits() );
        }
        for ( int j=0; j<ny; j++ ) {
            result.setYTagDouble( 0, j, tds.getYTagDouble( 0, j, tds.getYUnits() ), tds.getYUnits() );
        }
        if ( tds.getProperty("xTagWidth")!=null ) {
            result.setProperty( "xTagWidth", tds.getProperty("xTagWidth") );
        }
        return result;
    }
    
    public static WritableTableDataSet newCopy( TableDataSet tds ) {
        if ( tds.tableCount() > 1 ) throw new IllegalArgumentException("only supported for simple tables");
        int nx= tds.tableEnd(0);
        int ny= tds.getYLength(0);
        WritableTableDataSet result= newEmpty( tds );
        for ( int i=0; i<nx; i++ ) {
            for ( int j=0; j<ny; j++ ) {
                result.setDouble(i, j, tds.getDouble( i, j, tds.getZUnits() ), tds.getZUnits() );
            }
        }
        return result;
    }
    
    private WritableTableDataSet( double[] x, Units xunits, double [] y, Units yunits, double[][] z, Units zunits, Map properties ) {
        this.z= z;
        this.x= x;
        this.y= y;
        this.zunits= zunits;
        this.yunits= yunits;
        this.xunits= xunits;
        this.properties= properties;
    }
    
    public Datum getDatum(int i, int j) {
        return Datum.create( z[i][j], zunits );
    }
    
    public void setDatum( int i, int j, Datum datum ) {        
        z[i][j]= datum.doubleValue( zunits );
    }
    
    public double getDouble(int i, int j, Units units) {
        return zunits.convertDoubleTo(units,z[i][j]);
    }
    
    public void setDouble( int i, int j, double zvalue, Units units ) {
        z[i][j]= units.convertDoubleTo(zunits,zvalue);
    }
    
    public double[] getDoubleScan(int i, Units units) {
        throw new UnsupportedOperationException();
    }
    
    public int getInt(int i, int j, Units units) {
        throw new UnsupportedOperationException();
    }
    
    public DataSet getPlanarView(String planeID) {
        return null;
    }
    
    public String[] getPlaneIds() {
        return new String[0];
    }
    
    public Object getProperty(String name) {
        return properties.get(name);
    }
        
    public void setProperty( String name, Object value ) {
        properties.put( name, value );
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
    
    public void setXTagDouble( int i, double xvalue, Units units ) {
        x[i]= units.convertDoubleTo(xunits, xvalue );
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
