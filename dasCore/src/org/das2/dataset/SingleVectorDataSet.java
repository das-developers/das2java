/*
 * SingleVectorDataSet.java
 *
 * Created on November 3, 2005, 1:29 PM
 *
 *
 */

package org.das2.dataset;

import org.das2.datum.Datum;
import org.das2.datum.Units;
import java.util.HashMap;

/**
 *
 * @author Jeremy
 */
public class SingleVectorDataSet implements VectorDataSet {
    Datum x;
    Datum y;
    HashMap properties;
    
    public SingleVectorDataSet( Datum x, Datum y, HashMap properties ) {
        this.x= x;
        this.y= y;
        this.properties= new HashMap(properties);
    }
    
    public org.das2.datum.Datum getDatum(int i) {
        return y;
    }

    public double getDouble(int i, org.das2.datum.Units units) {
        return y.doubleValue(units);
    }

    public int getInt(int i, org.das2.datum.Units units) {
        return y.intValue(units);
    }

    public DataSet getPlanarView(String planeID) {
        return null;
    }

    public String[] getPlaneIds() {
        return new String[] { "" };
    }
    
    public int getXLength() {
        return 1;
    }
    
    public Datum getXTagDatum(int i) {
        return x;
    }
    
    public double getXTagDouble( int i, Units units ) {
        return x.doubleValue(units);
    }
     
    public int getXTagInt( int i, Units units ) {
        return x.intValue(units);
    }

    public java.util.Map getProperties() {
        return new HashMap( properties );
    }

    public Object getProperty(String name) {
        return properties.get(name);
    }

    public Units getXUnits() {
        return x.getUnits();
    }

    public Units getYUnits() {
        return y.getUnits();
    }
}
