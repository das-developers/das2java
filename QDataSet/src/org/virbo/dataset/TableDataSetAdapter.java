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
public class TableDataSetAdapter implements TableDataSet {

    Units xunits, yunits, zunits;
    QDataSet x, y, z;
    HashMap properties = new HashMap();

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
        return new TableDataSetAdapter(z, xds, yds);
        
    }

    /** Creates a new instance of TableDataSetAdapter */
    public TableDataSetAdapter(QDataSet z, QDataSet x, QDataSet y) {
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

        Boolean xMono = (Boolean) x.property(QDataSet.MONOTONIC);
        if (xMono != null && xMono.booleanValue()) {
            properties.put(org.das2.dataset.DataSet.PROPERTY_X_MONOTONIC, Boolean.TRUE);
        }
        Double cadence = (Double) x.property(QDataSet.CADENCE);
        if (cadence != null) {
            properties.put(org.das2.dataset.DataSet.PROPERTY_X_TAG_WIDTH, xunits.getOffsetUnits().createDatum(cadence.doubleValue()));
        }

        cadence = (Double) y.property(QDataSet.CADENCE);
        if (cadence != null) {
            properties.put(org.das2.dataset.DataSet.PROPERTY_Y_TAG_WIDTH, yunits.getOffsetUnits().createDatum(cadence.doubleValue()));
        }

        if ( z.property(QDataSet.FILL_VALUE) !=null 
                || z.property(QDataSet.VALID_MIN) !=null  
                || z.property(QDataSet.VALID_MAX) !=null )      {
            z= DataSetUtil.canonizeFill(z);
        } 
    }

    public Units getZUnits() {
        return zunits;
    }

    public Datum getDatum(int i, int j) {
        return zunits.createDatum(z.value(i, j));
    }

    public double getDouble(int i, int j, Units units) {
        return zunits.convertDoubleTo(units, z.value(i, j));
    }


    public double[] getDoubleScan(int i, Units units) {
        double[] zz = new double[getYLength(tableOfIndex(i))];
        for (int j = 0; j < zz.length; j++) {
            zz[j] = getDouble(i, j, getZUnits());
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
        return DatumVectorAdapter.toDatumVector(y);
    }

    public Datum getYTagDatum(int table, int j) {
        if (table > 0) {
            throw new IllegalArgumentException("table>0");
        }
        return yunits.createDatum(y.value(j));
    }

    public double getYTagDouble(int table, int j, Units units) {
        if (table > 0) {
            throw new IllegalArgumentException("table>0");
        }
        return yunits.convertDoubleTo(units, y.value(j));
    }

    public int getYTagInt(int table, int j, Units units) {
        return (int) getYTagDouble(table, j, units);
    }

    public int getYLength(int table) {
        return y.length();
    }

    public int tableStart(int table) {
        return 0;
    }

    public int tableEnd(int table) {
        return getXLength();
    }

    public int tableCount() {
        return 1;
    }

    public int tableOfIndex(int i) {
        return 0;
    }

    public VectorDataSet getXSlice(int i) {
        return new VectorDataSetAdapter(DataSetOps.slice0(z, i), y);
    }

    public VectorDataSet getYSlice(int j, int table) {
        return new VectorDataSetAdapter(DataSetOps.slice1(z, j), x);
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
        return xunits.createDatum(x.value(i));
    }

    public double getXTagDouble(int i, Units units) {
        return xunits.convertDoubleTo(units, x.value(i));
    }

    public int getXTagInt(int i, Units units) {
        return (int) xunits.convertDoubleTo(units, x.value(i));
    }

    public int getXLength() {
        return x.length();
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
