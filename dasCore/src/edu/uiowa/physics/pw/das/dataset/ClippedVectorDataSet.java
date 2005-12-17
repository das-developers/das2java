/*
 * ClippedVectorDataSet.java
 *
 * Created on April 18, 2005, 5:38 PM
 */

package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.datum.*;

/**
 *
 * @author Jeremy
 */
public class ClippedVectorDataSet implements VectorDataSet {
  
    int xoffset;
    int xlength;
    VectorDataSet source;
  
    /** Creates a new instance of ClippedVectorDataSet */
    public ClippedVectorDataSet( VectorDataSet source, DatumRange xclip ) {
        xoffset= DataSetUtil.getPreviousColumn( source, xclip.min() );
        xlength= DataSetUtil.getNextColumn( source, xclip.max() ) - xoffset;
        this.source= source;
    }

    public ClippedVectorDataSet( VectorDataSet source, int xoffset, int xlength ) {
        this.xoffset= xoffset;
        this.xlength= xlength;
        this.source= source;
    }
    
    public Datum getDatum(int i) {
        return source.getDatum(i+xoffset);
    }

    public double getDouble(int i, Units units) {
        return source.getDouble(i+xoffset,units);
    }

    public int getInt(int i, Units units) {
        return source.getInt(i+xoffset,units);
    }

    public DataSet getPlanarView(String planeID) {
        return new ClippedVectorDataSet( (VectorDataSet)source.getPlanarView(planeID), xoffset, xlength );
    }

    public String[] getPlaneIds() {
        return source.getPlaneIds();
    }

    public java.util.Map getProperties() {
        return source.getProperties();
    }

    public Object getProperty(String name) {
        return source.getProperty(name);
    }

    public int getXLength() {
        return xlength;
    }

    public Datum getXTagDatum(int i) {
        return source.getXTagDatum( i+xoffset );
    }

    public double getXTagDouble(int i, Units units) {
        return source.getXTagDouble( i+xoffset, units );
    }

    public int getXTagInt(int i, Units units) {
        return source.getXTagInt( i+xoffset, units );
    }

    public Units getXUnits() {
        return source.getXUnits();
    }

    public Units getYUnits() {
        return source.getYUnits();
    }        
    
}
