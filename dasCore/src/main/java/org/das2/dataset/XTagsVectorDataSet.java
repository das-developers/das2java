/*
 * XTagsVectorDataSet.java
 *
 * Created on December 4, 2004, 11:11 AM
 */

package org.das2.dataset;

import org.das2.datum.Datum;
import org.das2.datum.Units;

/**
 * Create a VectorDataSet that is the X Tags of another DataSet.  This is useful
 * for unifying the CurveRenderer and SymbolLineRenderer codes.
 *
 * @author  Jeremy
 */
public class XTagsVectorDataSet implements VectorDataSet {
    DataSet dataset;
    /** Creates a new instance of XTagsVectorDataSet */
    public XTagsVectorDataSet( DataSet dataset ) {
        this.dataset= dataset;
    }
    
    public Datum getDatum(int i) {
        return dataset.getXTagDatum(i);
    }
    
    public double getDouble(int i, Units units) {
        return dataset.getXTagDouble(i,units);
    }
    
    public int getInt(int i, Units units) {
        return dataset.getXTagInt(i,units);
    }
    
    public DataSet getPlanarView(String planeID) {
        return dataset.getPlanarView(planeID);
    }
    
    public String[] getPlaneIds() {
        return dataset.getPlaneIds();
    }
    
    public Object getProperty(String name) {
        return dataset.getProperty(name);
    }
        
    public java.util.Map getProperties() {
        return dataset.getProperties();
    }
    
    public int getXLength() {
        return dataset.getXLength();
    }
    
    public Datum getXTagDatum(int i) {
        return dataset.getXTagDatum(i);
    }
    
    public double getXTagDouble(int i, Units units) {
        return dataset.getXTagDouble(i,units);
    }
    
    public int getXTagInt(int i, Units units) {
        return dataset.getXTagInt(i,units);
    }
    
    public Units getXUnits() {
        return dataset.getXUnits();
    }
    
    public Units getYUnits() {
        return dataset.getXUnits();
    }
    
}
