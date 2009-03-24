/*
 * VectorDataSetAdapter.java
 *
 * Created on January 29, 2007, 9:55 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.virbo.dataset;

import org.das2.dataset.VectorDataSet;
import org.das2.datum.Datum;
import org.das2.datum.Units;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author jbf
 */
public class VectorDataSetAdapter implements VectorDataSet {
    
    Units xunits, yunits, zunits;
    QDataSet x, y;
    HashMap<String,QDataSet> planes;
    
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
        
        planes= new LinkedHashMap<String,QDataSet>();
        planes.put( "", y );
        
        for ( int i=0; i<QDataSet.MAX_PLANE_COUNT; i++ ) {
            QDataSet pds= (QDataSet) y.property( "PLANE_"+i);
            if ( pds!=null ) {
                String name= "PLANE_"+i;
                if ( pds.property(QDataSet.NAME )!=null ) name= (String)pds.property(QDataSet.NAME );
                planes.put( name, pds );
            } else {
                break;
            }
        }
        
        Boolean xMono=  (Boolean) x.property( QDataSet.MONOTONIC );
        if ( xMono!=null && xMono.booleanValue() ) {
            properties.put( org.das2.dataset.DataSet.PROPERTY_X_MONOTONIC, Boolean.TRUE );
        }
        
        RankZeroDataSet cadence= (RankZeroDataSet) x.property( QDataSet.CADENCE );
        if ( cadence!=null ) {
            Datum dcadence= DataSetUtil.asDatum(cadence);
            properties.put( org.das2.dataset.DataSet.PROPERTY_X_TAG_WIDTH, dcadence );
        }
                
        if ( y.property(QDataSet.FILL_VALUE) !=null 
                || y.property(QDataSet.VALID_MIN) !=null  
                || y.property(QDataSet.VALID_MAX) !=null ) {
            y= DataSetUtil.canonizeFill(y);
        }
        
        QDataSet dp= (QDataSet) y.property( QDataSet.DELTA_PLUS ) ;
        if ( dp!=null ) {
            if ( dp.property(QDataSet.UNITS)==null ) {
                dp= DDataSet.copy(dp);
                ((MutablePropertyDataSet)dp).putProperty(QDataSet.UNITS,yunits.getOffsetUnits());
            }
            planes.put( "Y_DELTA_PLUS", dp );
        }
        
       dp= (QDataSet) y.property( QDataSet.DELTA_MINUS ) ;
        if ( dp!=null ) {
            if ( dp.property(QDataSet.UNITS)==null ) {
                dp= DDataSet.copy(dp);
                ((MutablePropertyDataSet)dp).putProperty(QDataSet.UNITS,yunits.getOffsetUnits());
            }
            planes.put( "Y_DELTA_MINUS", dp );
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
    
    public org.das2.dataset.DataSet getPlanarView(String planeID) {
        if ( planeID.equals("") ) return this;
        if ( planes.containsKey(planeID) ) {
            return new VectorDataSetAdapter( (QDataSet)planes.get(planeID), x );
        }
        return null;
    }
    
    public String[] getPlaneIds() {
        return planes.keySet().toArray(new String[planes.keySet().size()] );
    }
    
    @Override
    public String toString() {
        return DataSetUtil.toString(y);
    }
}
