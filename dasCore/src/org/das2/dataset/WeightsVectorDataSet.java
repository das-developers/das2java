/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.dataset;

import java.util.HashMap;
import org.das2.datum.Datum;
import org.das2.datum.Units;

/**
 * DataSet with non-zero values when the source data is valid.
 * @author jbf
 */
public class WeightsVectorDataSet implements VectorDataSet {

    VectorDataSet source;
    Units sourceUnits;

    public static VectorDataSet create( VectorDataSet source ) {
        if ( source.getPlanarView(DataSet.PROPERTY_PLANE_WEIGHTS)!=null ) {
            return (VectorDataSet)source.getPlanarView(DataSet.PROPERTY_PLANE_WEIGHTS);
        } else {
            return new WeightsVectorDataSet( source );
        }
    }

    private WeightsVectorDataSet( VectorDataSet source ) {
        this.source= source;
        this.sourceUnits= source.getYUnits();
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

    public int getXLength() {
        return source.getXLength();
    }

    public org.das2.datum.Datum getXTagDatum(int i) {
        return source.getXTagDatum(i);
    }

    public double getXTagDouble(int i, org.das2.datum.Units units) {
        return source.getXTagDouble( i, units );
    }

    public int getXTagInt(int i, org.das2.datum.Units units) {
        return source.getXTagInt(i, units);
    }

    public org.das2.datum.Units getXUnits() {
        return source.getXUnits();
    }

    public org.das2.datum.Units getYUnits() {
        return Units.dimensionless;
    }

    public Datum getDatum(int i) {
        return Units.dimensionless.createDatum( getDouble(i,Units.dimensionless ) );
    }

    public double getDouble(int i, Units units) {
        return ( sourceUnits.isFill(source.getDouble( i, sourceUnits ) ) ) ? 0.0 : 1.0;
    }

    public int getInt(int i, Units units) {
        return (int)getDouble(i, units);
    }


}
