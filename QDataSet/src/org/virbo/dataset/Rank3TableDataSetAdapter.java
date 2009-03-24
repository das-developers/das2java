/*
 * TableDataSetAdapter.java
 *
 * Created on January 29, 2007, 9:27 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.virbo.dataset;

import org.das2.dataset.TableDataSet;
import org.das2.dataset.VectorDataSet;
import org.das2.datum.Datum;
import org.das2.datum.DatumVector;
import org.das2.datum.Units;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author jbf
 */
public class Rank3TableDataSetAdapter implements TableDataSet {

    Units xunits, yunits, zunits;
    QDataSet x, y, z;
    HashMap properties = new HashMap();
    int[] tables;
    
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

    /** Creates a new instance of TableDataSetAdapter */
    public Rank3TableDataSetAdapter(QDataSet z, QDataSet x, QDataSet y) {
        
        xunits = (Units) x.property(QDataSet.UNITS);
        yunits = (Units) y.property(QDataSet.UNITS);
        zunits = (Units) z.property(QDataSet.UNITS);
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
        if (xMono != null && xMono.booleanValue()) {
            properties.put(org.das2.dataset.DataSet.PROPERTY_X_MONOTONIC, Boolean.TRUE);
        }
        
        RankZeroDataSet cadence= (RankZeroDataSet) x.property( QDataSet.CADENCE,0 );
        if ( cadence!=null ) {
            Datum dcadence= DataSetUtil.asDatum(cadence);
            properties.put( org.das2.dataset.DataSet.PROPERTY_X_TAG_WIDTH, dcadence );
        }
                
        cadence = (RankZeroDataSet) y.property(QDataSet.CADENCE,0);
        if ( cadence!=null ) {
            Datum dcadence= DataSetUtil.asDatum(cadence);
            properties.put( org.das2.dataset.DataSet.PROPERTY_Y_TAG_WIDTH, dcadence );
        }

        if ( z.property(QDataSet.FILL_VALUE,0) !=null 
                || z.property(QDataSet.VALID_MIN,0) !=null  
                || z.property(QDataSet.VALID_MAX,0) !=null )      {
            z= DataSetUtil.canonizeFill(z);
        } 
        
        tables= new int[ z.length()+1 ];
        for ( int i=1; i<=z.length(); i++ ) {
            tables[i]= tables[i-1] + z.length(i-1);
        }
    }

    public Units getZUnits() {
        return zunits;
    }

    public Datum getDatum(int i, int j) {
        int i0= tableOfIndex(i);
        int i1= i - tableStart(i0);
        return zunits.createDatum(z.value(i0, i1, j));
    }

    public double getDouble(int i, int j, Units units) {
        int i0= tableOfIndex(i);
        int i1= i - tableStart(i0);
        return zunits.convertDoubleTo(units, z.value(i0, i1, j) );
    }


    public double[] getDoubleScan(int i, Units units) {
        int i0= tableOfIndex(i);
        int i1= i - tableStart(i0);
        double[] zz = new double[getYLength(i0)];
        for (int j = 0; j < zz.length; j++) {
            zz[j] = getDouble( i, j, getZUnits());
        }
        return zz;
    }

    public DatumVector getScan(int i) {
        double[] zz = getDoubleScan(i, getZUnits());
        return DatumVector.newDatumVector(zz, getZUnits());
    }

    public int getInt(int i, int j, Units units) {
        return (int) getDouble(i, j, units);
    }

    public DatumVector getYTags(int table) {
        return DatumVectorAdapter.toDatumVector( new Slice0DataSet(y, table) );
    }

    public Datum getYTagDatum(int table, int j) {
        if (table > 0) {
            throw new IllegalArgumentException("table>0");
        }
        return yunits.createDatum(y.value(table,j));
    }

    public double getYTagDouble(int table, int j, Units units) {
        return yunits.convertDoubleTo(units, y.value(table,j));
    }

    public int getYTagInt(int table, int j, Units units) {
        return (int) getYTagDouble(table, j, units);
    }

    public int getYLength(int table) {
        return y.length(table);
    }

    public int tableStart(int table) {
        return tables[table];
    }

    public int tableEnd(int table) {
        return tables[table+1];
    }
    
    public int tableCount() {
        return z.length();
    }

    public int tableOfIndex(int i) {
        /*if ( tables.length>10 ) { // this is going to be slow anyway, and the code is untested
            int result= Arrays.binarySearch( tables, i);
            if ( result<0 ) {
                return -1-result;
            } else {
                return result;
            }
        } else {*/
            for ( int j=0; j<tables.length; j++ ) {
                if ( i>=tables[j] ) return j;
            }
            throw new IndexOutOfBoundsException("index out of bounds: "+i);
        //}
    }

    public VectorDataSet getXSlice(int i) {
        int i0= tableOfIndex(i);
        int i1= i - tableStart(i0);
        return new VectorDataSetAdapter( DataSetOps.slice0( DataSetOps.slice0( z, i0), i1 ), DataSetOps.slice0(y,i0) );
    }

    public VectorDataSet getYSlice(int j, int table) {
        return new VectorDataSetAdapter( DataSetOps.slice1( DataSetOps.slice0(z, table), j), DataSetOps.slice0(x,table) );
    }

    public Object getProperty(String name) {
        Object result = properties.get(name);
        return (result != null) ? result : z.property(name);
    }

    public Object getProperty( int table, String name ) {
        return getProperty(name);
    }

    
    public Map getProperties() {
        Map result = new HashMap();
        result.put(QDataSet.UNITS, null );
        Map m = new HashMap(DataSetUtil.getProperties(z,result));
        m.putAll(properties);
        return m;
    }

    public Units getXUnits() {
        return xunits;
    }

    public Units getYUnits() {
        return yunits;
    }

    public Datum getXTagDatum(int i) {
        int i0= tableOfIndex(i);
        int i1= i - tableStart(i0);        
        return xunits.createDatum( x.value(i0,i1) );
    }

    public double getXTagDouble(int i, Units units) {
        int i0= tableOfIndex(i);
        int i1= i - tableStart(i0);        
        return xunits.convertDoubleTo(units, x.value(i0,i1));
    }

    public int getXTagInt(int i, Units units) {
        int i0= tableOfIndex(i);
        int i1= i - tableStart(i0);
        return (int) xunits.convertDoubleTo(units, x.value(i0,i1));
    }

    public int getXLength() {
        return tables[tables.length-1];
    }

    public org.das2.dataset.DataSet getPlanarView(String planeID) {
        return planeID.equals("") ? this : null;
    }

    public String[] getPlaneIds() {
        return new String[0];
    }

    public String toString() {
        return DataSetUtil.toString(this.z);
    }
    
    
}
