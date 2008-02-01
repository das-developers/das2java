/*
 * VectorDataSetAdapter.java
 *
 * Created on January 29, 2007, 9:55 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.virbo.dataset;

import edu.uiowa.physics.pw.das.dataset.VectorDataSet;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.Units;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author jbf
 */
public class VectorDataSetAdapter implements VectorDataSet {
    
    Units xunits, yunits, zunits;
    QDataSet x, y;
    QDataSet plane0;
    
    HashMap properties= new HashMap();
    
    public static VectorDataSet create( QDataSet y ) {
        QDataSet xds= (QDataSet)y.property( QDataSet.DEPEND_0 );
        if ( xds==null ) {
            xds= new IndexGenDataSet(y.length());
        }
        return new VectorDataSetAdapter( y, xds );
        
    }
    
    /** Creates a new instance of VectorDataSetAdapter */
    public VectorDataSetAdapter( QDataSet y, QDataSet x  ) {
        if ( y.rank()!=1 ) throw new IllegalArgumentException("y (rank="+y.rank()+") is not rank 1");
        if ( x.rank()!=1 ) throw new IllegalArgumentException("x (rank="+x.rank()+") is not rank 1");
        xunits= (Units) x.property(QDataSet.UNITS);
        if ( xunits==null ) xunits= Units.dimensionless;
        yunits= (Units) y.property(QDataSet.UNITS);
        if ( yunits==null ) yunits= Units.dimensionless;
        this.x= x;
        this.y= y;
        plane0= (QDataSet) y.property( QDataSet.PLANE_0);
        
        Boolean xMono=  (Boolean) x.property( QDataSet.MONOTONIC );
        if ( xMono!=null && xMono.booleanValue() ) {
            properties.put( edu.uiowa.physics.pw.das.dataset.DataSet.PROPERTY_X_MONOTONIC, Boolean.TRUE );
        }
        
        Double cadence= (Double) x.property( QDataSet.CADENCE );
        if ( cadence!=null ) {
            properties.put( edu.uiowa.physics.pw.das.dataset.DataSet.PROPERTY_X_TAG_WIDTH, xunits.getOffsetUnits().createDatum( cadence.doubleValue() ) );
        }
    }
    
    public Datum getDatum(int i) {
        return yunits.createDatum( y.value(i) );
    }
    
    public double getDouble(int i, Units units) {
        return yunits.convertDoubleTo( units, y.value(i) );
    }
    
    public int getInt(int i, Units units) {
        return (int)yunits.convertDoubleTo( units, y.value(i) );
    }
    
    public Object getProperty(String name) {
        Object result= properties.get(name);
        return ( result!=null ) ? result : y.property(name);
    }
    
    public Map getProperties() {
        Map result= new HashMap();
        result.put( QDataSet.VALID_RANGE, null );
        result.put( QDataSet.UNITS, null );
        Map m= new HashMap( DataSetUtil.getProperties(y, result ) );
        m.putAll( properties );
        return m;
    }
    
    public Units getXUnits() {
        return xunits;
    }
    
    public Units getYUnits() {
        return yunits;
    }
    
    public Datum getXTagDatum(int i) {
        return xunits.createDatum( x.value( i ) );
    }
    
    public double getXTagDouble(int i, Units units) {
        return xunits.convertDoubleTo( units, x.value( i ) );
    }
    
    public int getXTagInt(int i, Units units) {
        return (int)xunits.convertDoubleTo( units, x.value( i ) );
    }
    
    public int getXLength() {
        return x.length();
    }
    
    public edu.uiowa.physics.pw.das.dataset.DataSet getPlanarView(String planeID) {
        if ( planeID.equals("") ) return this;
        if ( planeID.equals("plane0") && plane0!=null ) return new VectorDataSetAdapter( plane0, x );
        return null;
    }
    
    public String[] getPlaneIds() {
        if ( plane0!=null ) {
            return new String[] { "plane0" };
        } else {
            return new String[0];
        }
    }
    
    public String toString() {
        return DataSetUtil.toString(y);
    }
}
