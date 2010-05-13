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

        if (z.rank() == 2) {
            QDataSet xds = (QDataSet) z.property(QDataSet.DEPEND_0);
            if (xds == null) {
                xds = new IndexGenDataSet(z.length());
            }
            QDataSet yds = (QDataSet) z.property(QDataSet.DEPEND_1);
            if (yds == null) {
                yds = new IndexGenDataSet(z.length(0));
            }
            if (!DataSetUtil.isMonotonic(xds)) {
                QDataSet sort = DataSetOps.sort(xds);
                RankZeroDataSet cadence= (RankZeroDataSet) xds.property(QDataSet.CADENCE);
                z = DataSetOps.applyIndex(z, 0, sort, false);
                xds = DataSetOps.applyIndex(xds, 0, sort, false);
                if ( cadence!=null && cadence.value()<0 ) {
                    cadence= DataSetUtil.asDataSet( DataSetUtil.asDatum(cadence).multiply(-1) );
                    ((WritableDataSet)xds).putProperty(QDataSet.CADENCE, cadence);
                }
            }
            if (!DataSetUtil.isMonotonic(yds)) {
                QDataSet sort = DataSetOps.sort(yds);
                RankZeroDataSet cadence= (RankZeroDataSet) yds.property(QDataSet.CADENCE);
                z = DataSetOps.applyIndex(z, 1, sort, false);
                yds = DataSetOps.applyIndex(yds, 0, sort, false);
                if ( cadence!=null && cadence.value()<0 ) {
                    cadence= DataSetUtil.asDataSet( DataSetUtil.asDatum(cadence).multiply(-1) );
                    ((WritableDataSet)yds).putProperty(QDataSet.CADENCE, cadence);
                }
            }
            return new TableDataSetAdapter(z, xds, yds);
        } else if (z.rank() == 3) {
            JoinDataSet xds= new JoinDataSet(2);
            JoinDataSet yds= new JoinDataSet(2);
            int ix=0;
            int iy=0;
            boolean haveX= true;
            boolean haveY= true;
            Units xunits=null;
            boolean haveXUnits= true;
            Units yunits=null;
            boolean haveYUnits= true;
            for (int i0 = 0; i0 < z.length(); i0++) {
                QDataSet xds1 = (QDataSet) z.property(QDataSet.DEPEND_0,i0);
                if (xds1 == null) {
                    haveX= true;
                    xds1 = new TagGenDataSet( z.length(i0), 1., ix );
                    ix+= z.length(i0);
                } else {
                    Units u= (Units) xds1.property(QDataSet.UNITS);
                    if ( u!=null && xunits!=null && xunits!=u ) {
                        haveXUnits= false;
                    } else if ( u!=null && xunits==null ) {
                        xunits= u;
                    }
                }
                QDataSet yds1 = (QDataSet) z.property(QDataSet.DEPEND_1,i0);
                if (yds1 == null) {
                    yds1 = new IndexGenDataSet( z.length(i0,0) );
                } else {
                    Units u= (Units) yds1.property(QDataSet.UNITS);
                    if ( u!=null && yunits!=null && yunits!=u ) {
                        haveYUnits= false;
                    } else if ( u!=null && yunits==null ) {
                        yunits= u;
                    }
                }
                if (!DataSetUtil.isMonotonic(xds1)) {
                    throw new IllegalArgumentException("x table must be monotonic");
                }
                if (!DataSetUtil.isMonotonic(yds1)) {
                    throw new IllegalArgumentException("y table must be monotonic");
                }
                if ( haveX ) {
                    xds.join( xds1 );
                } 
                if ( haveY ) {
                    yds.join(yds1);
                }
            }
            if ( ( haveX || haveY ) && !( z instanceof MutablePropertyDataSet ) ) {
                z= DDataSet.maybeCopy(z);
            }
            if ( haveX ) {
                if ( haveXUnits ) xds.putProperty( QDataSet.UNITS, xunits );
            }
            if ( haveY ) {
                if ( haveYUnits ) yds.putProperty( QDataSet.UNITS, yunits );
            }
            return new Rank3TableDataSetAdapter(z, xds, yds);
        } else {
            throw new IllegalArgumentException("rank must be 2 or 3");
        }

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

        RankZeroDataSet cadence = (RankZeroDataSet) x.property(QDataSet.CADENCE);
        if (cadence != null) {
            Datum dcadence;
            dcadence = DataSetUtil.asDatum(cadence);
            properties.put(org.das2.dataset.DataSet.PROPERTY_X_TAG_WIDTH, dcadence);
        }

        cadence = (RankZeroDataSet) y.property(QDataSet.CADENCE);
        if (cadence != null) {
            Datum dcadence;
            dcadence = DataSetUtil.asDatum(cadence);
            properties.put(org.das2.dataset.DataSet.PROPERTY_Y_TAG_WIDTH, dcadence);
        }

        if (z.property(QDataSet.FILL_VALUE) != null || z.property(QDataSet.VALID_MIN) != null || z.property(QDataSet.VALID_MAX) != null) {
            this.z = DataSetUtil.canonizeFill(z);
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

    public Object getProperty(int table, String name) {
        return getProperty(name);
    }

    public Map getProperties() {
        Map result = new HashMap();
        result.put(QDataSet.UNITS, null);
        Map m = new HashMap(DataSetUtil.getProperties(z, result));
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
