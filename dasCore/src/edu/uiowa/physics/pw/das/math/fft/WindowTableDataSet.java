/*
 * WindowTableDataSet.java
 *
 * Created on March 25, 2004, 9:30 PM
 */

package edu.uiowa.physics.pw.das.math.fft;

import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;

/**
 *
 * @author  jbf
 */
public class WindowTableDataSet implements TableDataSet {
    VectorDataSet source;
    int windowSize;
    double delta;
    Units deltaUnits;
    
    private static boolean checkXTagsGrid( DataSet ds ) {
        if ( ds.getXLength()<1 ) {
            return false;
        } else {
            Units units= ds.getXUnits().getOffsetUnits();
            double base= ds.getXTagDouble( 0, units );
            double delta= ( ds.getXTagDouble( ds.getXLength()-1, units ) - base )
            / ( ds.getXLength()-1 );
            for ( int i=0; i<ds.getXLength(); i++ ) {
                double rr= ( ( ds.getXTagDouble(i, units) - base ) / delta ) % 1.;
                if ( rr > 0.01 && rr < 0.09 ) {
                    return false;
                }
            }
            return true;
        }
    }
    
    /** Creates a new instance of WindowTableDataSet */
    public WindowTableDataSet( VectorDataSet source, int windowSize ) {
        this.source= source;
        this.windowSize= windowSize;
        if ( !checkXTagsGrid(source) ) {
            throw new IllegalArgumentException("xTags don't appear to be gridded");
        }
        if ( source.getXLength()<windowSize ) {
            throw new IllegalArgumentException("windowSize ("+windowSize+") exceeds source xTag length ("+source.getXLength()+")");
        }
        Units units= source.getXUnits().getOffsetUnits();
        this.delta= ( source.getXTagDouble( source.getXLength()-1, units ) -
        source.getXTagDouble( 0, units ) )
        / (source.getXLength()-1);
        this.deltaUnits= units;
    }
    
    public Datum getDatum(int i, int j) {
        return source.getDatum(i*this.windowSize+j);
    }
    
    public double getDouble(int i, int j, Units units) {
        return source.getDouble( i * this.windowSize + j, units );
    }
    
    public int getInt(int i, int j, Units units) {
        return source.getInt( i*this.windowSize+j, units );
    }
    
    public DataSet getPlanarView(String planeID) {
        return new WindowTableDataSet( (VectorDataSet)this.source.getPlanarView(planeID), this.windowSize );
    }
    
    public Object getProperty(String name) {
        return this.source.getProperty(name);
    }
        
    public int getXLength() {
        return this.source.getXLength()/this.windowSize;
    }
    
    public VectorDataSet getXSlice(int i) {
        throw new IllegalArgumentException();
    }
    
    public Datum getXTagDatum(int i) {
        return source.getXTagDatum( i * this.windowSize );
    }
    
    public double getXTagDouble(int i, Units units) {
        return source.getXTagDouble( i *this.windowSize, units );
    }
    
    public int getXTagInt(int i, Units units) {
        return source.getXTagInt( i *this.windowSize, units );
    }
    
    public Units getXUnits() {
        return source.getXUnits();
    }
    
    public int getYLength(int table) {
        return this.windowSize;
    }
    
    public VectorDataSet getYSlice(int j, int table) {
        throw new IllegalArgumentException("not implemented");
    }
    
    public Datum getYTagDatum(int table, int j) {
        return getYUnits().createDatum( getYTagDouble( table,  j, getYUnits() ) );
    }
    
    public DatumVector getYTags(int table) {
        double [] ytags= new double[ getYLength(table) ];
        for ( int j=0; j<getYLength(table); j++ ) {
            ytags[j] = delta*j;
        }
        return DatumVector.newDatumVector(ytags,getYUnits());
    }
    
    public double getYTagDouble(int table, int j, Units units) {
        return this.getYUnits().convertDoubleTo(units, delta * j );
    }
    
    public int getYTagInt(int table, int j, Units units) {
        return (int)this.getYTagDouble( table, j, units );
    }
    
    public Units getYUnits() {
        return deltaUnits;
    }
    
    public Units getZUnits() {
        return source.getYUnits();
    }
    
    public int tableCount() {
        return 1;
    }
    
    public int tableEnd(int table) {
        return source.getXLength()/windowSize;
    }
    
    public int tableOfIndex(int i) {
        return 0;
    }
    
    public int tableStart(int table) {
        return 0;
    }
    
    public double[] getDoubleScan(int i, Units units) {
        DatumVector vv= getScan( i );
        return vv.toDoubleArray(units);
    }
    
    public DatumVector getScan(int i) {
        double [] zz= new double[ getYLength( tableOfIndex(i) ) ];
        for ( int j=0; j<zz.length; j++ ) {
            zz[j] = getDouble( i, j, getYUnits() );
        }
        return DatumVector.newDatumVector(zz,getYUnits());
    }
    
    public String[] getPlaneIds() {
        return source.getPlaneIds();
    }
    
    public java.util.Map getProperties() {
        return source.getProperties();
    }
    
}
