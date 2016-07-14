/*
 * EZVectorDataSet.java
 *
 * Created on April 15, 2005, 2:59 PM
 */

package org.das2.dataset;

import org.das2.datum.Datum;
import org.das2.datum.Units;

/**
 * Abstract VectorDataSet that allows for defining a vector dataset by
 * implementing a minimal portion of the api.
 * @author Jeremy
 */
public abstract class QuickVectorDataSet implements VectorDataSet {
    java.util.Map properties= new java.util.HashMap();
    String[] planeIds= new String[0];
    
    public Datum getDatum(int i) {
        Units yUnits= getYUnits();
        return yUnits.createDatum( getDouble(i,yUnits) );
    }
    
    abstract public double getDouble(int i, Units units );
    
    public int getInt(int i, Units units) {
        return (int) getDouble( i,units );
    }
    
    public DataSet getPlanarView(String planeID) {
        return null;
    }
    
    public String[] getPlaneIds() {
        return planeIds;
    }
    
    public java.util.Map getProperties() {
        return null;
    }
    
    public Object getProperty(String name) {
        return null;
    }
    
    abstract public int getXLength();
    
    public Datum getXTagDatum(int i) {
        Units xUnits= getXUnits();
        return xUnits.createDatum( getXTagDouble(i,xUnits) );
    }
    
    abstract public double getXTagDouble(int i, Units units);
    
    public int getXTagInt(int i, Units units) {
        return (int)getXTagDouble( i, units );
    }
    
    abstract public Units getXUnits();
    
    abstract public Units getYUnits();
}