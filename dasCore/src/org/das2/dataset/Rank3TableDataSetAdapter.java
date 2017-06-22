/*
 * TableDataSetAdapter.java
 *
 * Created on January 29, 2007, 9:27 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.das2.dataset;

import org.das2.datum.Datum;
import org.das2.datum.DatumVector;
import org.das2.datum.Units;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.das2.datum.DatumRange;
import org.das2.qds.DataSetOps;
import org.das2.qds.DataSetUtil;
import org.das2.qds.DatumVectorAdapter;
import org.das2.qds.IndexGenDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.Slice0DataSet;

/**
 *
 * @author jbf
 */
public class Rank3TableDataSetAdapter implements TableDataSet {

    Units xunits, yunits, zunits;
    QDataSet x, y, z;
    HashMap properties = new HashMap();
    int[] tables;

    HashMap<String,QDataSet> planes;
    
    public static TableDataSet create(QDataSet z) {
        QDataSet xds = (QDataSet) z.property(QDataSet.DEPEND_0);
        if (xds == null) {
            xds = new IndexGenDataSet(z.length());
        }
        QDataSet yds = (QDataSet) z.property(QDataSet.DEPEND_1);
        if (yds == null) {
            yds = new IndexGenDataSet(z.length(0));
        }
        if ( !DataSetUtil.isMonotonic(xds) ) {
            QDataSet sort= DataSetOps.sort(xds);
            z= DataSetOps.applyIndex( z, 0, sort, false );
            xds= DataSetOps.applyIndex( xds, 0, sort, false );
        }
        if ( !DataSetUtil.isMonotonic(yds) ) {
            QDataSet sort= DataSetOps.sort(yds);
            z= DataSetOps.applyIndex( z, 1, sort, false );    
            yds= DataSetOps.applyIndex( yds, 0, sort, false );
        }
        return new Rank3TableDataSetAdapter(z, xds, yds);
        
    }

    /** 
     * Creates a new instance of TableDataSetAdapter
     * @param z
     * @param x
     * @param y 
     */
    public Rank3TableDataSetAdapter(QDataSet z, QDataSet x, QDataSet y) {

        planes= new LinkedHashMap<>();
        planes.put( "", z );
        
        if ( SemanticOps.isJoin(z) ) { // it really must be...
            xunits = (Units) x.slice(0).property(QDataSet.UNITS);
            yunits = (Units) y.slice(0).property(QDataSet.UNITS);
            zunits = (Units) z.slice(0).property(QDataSet.UNITS);                
        } else {
            xunits = (Units) x.property(QDataSet.UNITS);
            yunits = (Units) y.property(QDataSet.UNITS);
            zunits = (Units) z.property(QDataSet.UNITS);    
        }
        if (xunits == null) {
            xunits = Units.dimensionless;
        }
        if (yunits == null) {
            yunits = Units.dimensionless;
        }
        if (zunits == null) {
            zunits = Units.dimensionless;
        }
        this.x = x;
        this.y = y;
        this.z = z;

        Boolean xMono = (Boolean) x.property(QDataSet.MONOTONIC,0);
        if (xMono != null && xMono ) {
            properties.put(org.das2.dataset.DataSet.PROPERTY_X_MONOTONIC, Boolean.TRUE);
        }
        
        QDataSet cadence= (QDataSet) x.property( QDataSet.CADENCE,0 );
        if ( cadence!=null ) {
            Datum dcadence= DataSetUtil.asDatum(cadence);
            properties.put( org.das2.dataset.DataSet.PROPERTY_X_TAG_WIDTH, dcadence );
        }
                
        cadence = (QDataSet) y.property(QDataSet.CADENCE,0);
        if ( cadence!=null ) {
            Datum dcadence= DataSetUtil.asDatum(cadence);
            properties.put( org.das2.dataset.DataSet.PROPERTY_Y_TAG_WIDTH, dcadence );
        }

        if ( z.property(QDataSet.FILL_VALUE,0) !=null 
                || z.property(QDataSet.VALID_MIN,0) !=null  
                || z.property(QDataSet.VALID_MAX,0) !=null )      {
            QDataSet wds= DataSetUtil.weightsDataSet(z);
            planes.put( org.das2.dataset.DataSet.PROPERTY_PLANE_WEIGHTS, wds );
            //z= DataSetUtil.canonizeFill(z);
        } 
        
        tables= new int[ z.length()+1 ];
        for ( int i=1; i<=z.length(); i++ ) {
            tables[i]= tables[i-1] + z.length(i-1);
        }
        
        String s;
        s= (String) z.slice(0).property( QDataSet.LABEL ); 
        if ( s!=null ) properties.put( DataSet.PROPERTY_Z_LABEL, s );

        s= (String) z.slice(0).property( QDataSet.SCALE_TYPE );
        if ( s!=null ) properties.put( DataSet.PROPERTY_Z_SCALETYPE, s );
        
        s= (String) z.slice(0).property( QDataSet.TITLE );
        if ( s!=null ) properties.put( DataSet.PROPERTY_TITLE, s );

        s= (String) z.slice(0).property( QDataSet.NAME );
        if ( s!=null ) properties.put( "name", s );
        
        s= (String) z.slice(0).property( QDataSet.DESCRIPTION );
        if ( s!=null ) properties.put( DataSet.PROPERTY_SUMMARY, s );

        Number m1= (Number) z.slice(0).property( QDataSet.TYPICAL_MIN );
        Number m2= (Number) z.slice(0).property( QDataSet.TYPICAL_MAX );
        if ( m1!=null && m2!=null ) {
            properties.put( DataSet.PROPERTY_Z_RANGE, new DatumRange( m1.doubleValue(), m2.doubleValue(), zunits ) );
        }
        
        s= (String) y.slice(0).property( QDataSet.LABEL );
        if ( s!=null ) properties.put( DataSet.PROPERTY_Y_LABEL, s );

        s= (String) y.slice(0).property( QDataSet.SCALE_TYPE );
        if ( s!=null ) properties.put( DataSet.PROPERTY_Y_SCALETYPE, s );
        
        s= (String) x.slice(0).property( QDataSet.LABEL );
        if ( s!=null ) properties.put( DataSet.PROPERTY_X_LABEL, s );
        
    }

    @Override
    public Units getZUnits() {
        return zunits;
    }

    @Override
    public Datum getDatum(int i, int j) {
        int i0= tableOfIndex(i);
        int i1= i - tableStart(i0);
        return zunits.createDatum(z.value(i0, i1, j));
    }

    @Override
    public double getDouble(int i, int j, Units units) {
        int i0= tableOfIndex(i);
        int i1= i - tableStart(i0);
        return zunits.convertDoubleTo(units, z.value(i0, i1, j) );
    }


    @Override
    public double[] getDoubleScan(int i, Units units) {
        int i0= tableOfIndex(i);
        double[] zz = new double[getYLength(i0)];
        for (int j = 0; j < zz.length; j++) {
            zz[j] = getDouble( i, j, getZUnits());
        }
        return zz;
    }

    @Override
    public DatumVector getScan(int i) {
        double[] zz = getDoubleScan(i, getZUnits());
        return DatumVector.newDatumVector(zz, getZUnits());
    }

    @Override
    public int getInt(int i, int j, Units units) {
        return (int) getDouble(i, j, units);
    }

    @Override
    public DatumVector getYTags(int table) {
        return DatumVectorAdapter.toDatumVector( new Slice0DataSet(y, table) );
    }

    @Override
    public Datum getYTagDatum(int table, int j) {
        return yunits.createDatum(y.value(table,j));
    }

    @Override
    public double getYTagDouble(int table, int j, Units units) {
        return yunits.convertDoubleTo(units, y.value(table,j));
    }

    @Override
    public int getYTagInt(int table, int j, Units units) {
        return (int) getYTagDouble(table, j, units);
    }

    @Override
    public int getYLength(int table) {
        return y.length(table);
    }

    @Override
    public int tableStart(int table) {
        return tables[table];
    }

    @Override
    public int tableEnd(int table) {
        return tables[table+1];
    }
    
    @Override
    public int tableCount() {
        return z.length();
    }

    @Override
    public int tableOfIndex(int i) {
        /*if ( tables.length>10 ) { // this is going to be slow anyway, and the code is untested
            int result= Arrays.binarySearch( tables, i);
            if ( result<0 ) {
                return -1-result;
            } else {
                return result;
            }
        } else {*/
            for ( int j=0; j<tables.length-1; j++ ) { // tables has one extra element
                if ( i>=tables[j] && i<tables[j+1] ) return j;
            }
            throw new IndexOutOfBoundsException("index out of bounds: "+i);
        //}
    }

    @Override
    public VectorDataSet getXSlice(int i) {
        int i0= tableOfIndex(i);
        int i1= i - tableStart(i0);
        return new VectorDataSetAdapter( DataSetOps.slice0( DataSetOps.slice0( z, i0), i1 ), DataSetOps.slice0(y,i0) );
    }

    @Override
    public VectorDataSet getYSlice(int j, int table) {
        return new VectorDataSetAdapter( DataSetOps.slice1( DataSetOps.slice0(z, table), j), DataSetOps.slice0(x,table) );
    }

    @Override
    public Object getProperty(String name) {
        Object result = properties.get(name);
        return (result != null) ? result : z.property(name);
    }

    @Override
    public Object getProperty( int table, String name ) {
        return getProperty(name);
    }

    
    @Override
    public Map getProperties() {
        Map result = new HashMap();
        result.put(QDataSet.UNITS, null );
        Map m = new HashMap(DataSetUtil.getProperties(z,result));
        if ( SemanticOps.isJoin(z) ) {
            m= DataSetUtil.getDimensionProperties( z.slice(0), m );
        }
        m.putAll(properties);
        return m;
    }

    @Override
    public Units getXUnits() {
        return xunits;
    }

    @Override
    public Units getYUnits() {
        return yunits;
    }

    @Override
    public Datum getXTagDatum(int i) {
        int i0= tableOfIndex(i);
        int i1= i - tableStart(i0);        
        return xunits.createDatum( x.value(i0,i1) );
    }

    @Override
    public double getXTagDouble(int i, Units units) {
        int i0= tableOfIndex(i);
        int i1= i - tableStart(i0);        
        return xunits.convertDoubleTo(units, x.value(i0,i1));
    }

    @Override
    public int getXTagInt(int i, Units units) {
        int i0= tableOfIndex(i);
        int i1= i - tableStart(i0);
        return (int) xunits.convertDoubleTo(units, x.value(i0,i1));
    }

    @Override
    public int getXLength() {
        return tables[tables.length-1];
    }

    @Override
    public org.das2.dataset.DataSet getPlanarView(String planeID) {
        if ( planeID.equals("") ) return this;
        if ( planes.containsKey(planeID) ) {
            return new TableDataSetAdapter( (QDataSet)planes.get(planeID), x, y );
        }
        return null;
    }

    @Override
    public String[] getPlaneIds() {
        return planes.keySet().toArray(new String[planes.keySet().size()] );
    }


    @Override
    public String toString() {
        return DataSetUtil.toString(this.z);
    }
    
    
}
