/*
 * TableDataSetAdapter.java
 *
 * Created on January 29, 2007, 9:27 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.das2.dataset;

import org.das2.dataset.TableDataSet;
import org.das2.dataset.VectorDataSet;
import org.das2.datum.Datum;
import org.das2.datum.DatumVector;
import org.das2.datum.Units;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.das2.datum.UnitsConverter;
import org.das2.datum.UnitsUtil;
import org.das2.qds.ArrayDataSet;
import org.das2.qds.DDataSet;
import org.das2.qds.DataSetOps;
import org.das2.qds.DataSetUtil;
import org.das2.qds.DatumVectorAdapter;
import org.das2.qds.IndexGenDataSet;
import org.das2.qds.JoinDataSet;
import org.das2.qds.MutablePropertyDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.RankZeroDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.TagGenDataSet;
import org.das2.qds.WritableDataSet;

/**
 * Adapts QDataSets to legacy das2 TableDataSet.
 * See also DataSetAdapter.
 * @author jbf
 */
public class TableDataSetAdapter implements TableDataSet {

    Units xunits, yunits, zunits;
    QDataSet x, y, z;
    HashMap properties = new HashMap();
    HashMap<String,QDataSet> planes;

    public static TableDataSet create(QDataSet z) {

        if (z.rank() == 2) {
            QDataSet xds = (QDataSet) z.property(QDataSet.DEPEND_0);
            if (xds == null) {
                xds = new IndexGenDataSet(z.length());
            }
            QDataSet yds = (QDataSet) z.property(QDataSet.DEPEND_1);
            if (yds == null) {
                if ( z.length()>0 && z.property(QDataSet.DEPEND_0,0)!=null ) {
                    Units yunits= null;
                    JoinDataSet jds= new JoinDataSet(2);
                    for ( int i=0; i<z.length(); i++ ) {
                        QDataSet d0ds= (QDataSet) z.property(QDataSet.DEPEND_0,i);
                        if ( yunits==null ) {
                            yunits= (Units) d0ds.property(QDataSet.UNITS);
                            if ( yunits==null ) yunits=Units.dimensionless;
                            jds.putProperty(QDataSet.UNITS, yunits);
                        } else {
                            Units y0units= (Units) d0ds.property(QDataSet.UNITS);
                            if ( y0units==null ) y0units=Units.dimensionless;
                            if ( y0units!=yunits ) throw new IllegalArgumentException("yunits change");
                        }
                        jds.join(d0ds);
                    }
                    yds= jds;
                } else {
                    yds = new IndexGenDataSet(z.length(0));
                }
            }
            if ( xds.length()>1 && !DataSetUtil.isMonotonic(xds)) {
                QDataSet sort = DataSetOps.sort(xds);
                QDataSet cadence= (QDataSet) xds.property(QDataSet.CADENCE);
                z = DataSetOps.applyIndex(z, 0, sort, false);
                xds = DataSetOps.applyIndex(xds, 0, sort, false);
                if ( cadence!=null && cadence.value()<0 ) {
                    cadence= DataSetUtil.asDataSet( DataSetUtil.asDatum(cadence).multiply(-1) );
                    ((WritableDataSet)xds).putProperty(QDataSet.CADENCE, cadence);
                }
            }
            if ( yds.rank()==1 && !DataSetUtil.isMonotonic(yds)) {
                QDataSet sort = DataSetOps.sort(yds);
                QDataSet cadence= (QDataSet) yds.property(QDataSet.CADENCE);
                z = DataSetOps.applyIndex(z, 1, sort, false);
                yds = DataSetOps.applyIndex(yds, 0, sort, false);
                if ( cadence!=null && cadence.value()<0 ) {
                    cadence= DataSetUtil.asDataSet( DataSetUtil.asDatum(cadence).multiply(-1) );
                    ((WritableDataSet)yds).putProperty(QDataSet.CADENCE, cadence);
                }
            }
            
            // convert to us2000 for legacy server
            Units xunits= SemanticOps.getUnits( xds );
            if ( UnitsUtil.isTimeLocation(xunits) ) {
                UnitsConverter uc= UnitsConverter.getConverter( xunits, Units.us2000 );
                ArrayDataSet xx= ArrayDataSet.copy(xds);
                for ( int i=0; i<xds.length(); i++ ) {
                    xx.putValue( i, uc.convert( xx.value(i) ) );
                }
                xx.putProperty( QDataSet.UNITS, Units.us2000 );
                xds= xx;
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
                //if (!DataSetUtil.isMonotonic(xds1)) {
                //    throw new IllegalArgumentException("x table must be monotonic");
                //}
                //if (!DataSetUtil.isMonotonic(yds1)) {
                //    throw new IllegalArgumentException("y table must be monotonic");
                //}
                if ( haveX ) {
                    // convert to us2000 for legacy server
                    if ( UnitsUtil.isTimeLocation(xunits) ) {
                        UnitsConverter uc= UnitsConverter.getConverter( xunits, Units.us2000 );
                        ArrayDataSet xx= ArrayDataSet.copy(xds1);
                        for ( int i=0; i<xds1.length(); i++ ) {
                            xx.putValue( i, uc.convert( xx.value(i) ) );
                        }
                        xx.putProperty( QDataSet.UNITS, Units.us2000 );
                        xds1= xx;
                    }                    
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
                if ( haveXUnits ) xds.putProperty( QDataSet.UNITS, Units.us2000 );
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

        planes= new LinkedHashMap<String,QDataSet>();
        planes.put( "", z );

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
        if (xMono != null && xMono ) {
            properties.put(org.das2.dataset.DataSet.PROPERTY_X_MONOTONIC, Boolean.TRUE);
        }

        QDataSet cadence = (QDataSet) x.property(QDataSet.CADENCE);
        if (cadence != null) {
            Datum dcadence;
            dcadence = DataSetUtil.asDatum(cadence);
            properties.put(org.das2.dataset.DataSet.PROPERTY_X_TAG_WIDTH, dcadence);
        }

        cadence = (QDataSet) y.property(QDataSet.CADENCE);
        if (cadence != null) {
            Datum dcadence;
            dcadence = DataSetUtil.asDatum(cadence);
            properties.put(org.das2.dataset.DataSet.PROPERTY_Y_TAG_WIDTH, dcadence);
        }

        if ( z.property(QDataSet.FILL_VALUE) !=null
                || z.property(QDataSet.VALID_MIN) !=null
                || z.property(QDataSet.VALID_MAX) !=null ) {
            //this.z = DataSetUtil.canonizeFill(z);
            QDataSet wds= DataSetUtil.weightsDataSet(z);
            planes.put( org.das2.dataset.DataSet.PROPERTY_PLANE_WEIGHTS, wds );
            //this.y= DataSetUtil.canonizeFill(y);
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
        if ( y.rank()==1 ) {
            return DatumVectorAdapter.toDatumVector(y);
        } else {  //  if ( y.rank()==2 ) {
            return DatumVectorAdapter.toDatumVector( y.slice(table) );
        }
    }

    public Datum getYTagDatum(int table, int j) {
        if ( y.rank()==1 ) {
            if (table > 0) {
                throw new IllegalArgumentException("table>0");
            }
            return yunits.createDatum(y.value(j));
        } else {
            return yunits.createDatum(y.value(table,j));
        }
    }

    public double getYTagDouble(int table, int j, Units units) {
        if ( y.rank()==1 ) {
            if (table > 0) {
                throw new IllegalArgumentException("table>0");
            }
            return yunits.convertDoubleTo(units, y.value(j));
        } else {
            return yunits.convertDoubleTo(units, y.value(table,j));
        }
    }

    public int getYTagInt(int table, int j, Units units) {
        return (int) getYTagDouble(table, j, units);
    }

    public int getYLength(int table) {
        if ( y.rank()==1 ) {
            return y.length();
        } else {
            return y.length(table);
        }
    }

    public int tableStart(int table) {
        if ( y.rank()==1 ) {
            return 0;
        } else {
            return table;
        }
    }

    public int tableEnd(int table) {
        if ( y.rank()==1 ) {
            return getXLength();
        } else {
            return table+1;
        }
    }

    public int tableCount() {
        if ( y.rank()==1 ) {
            return 1;
        } else {
            return y.length();  // should be the same as x.length() 
        }
    }

    public int tableOfIndex(int i) {
        if ( y.rank()==1 ) {
            return 0;
        } else {
            return i;
        }

    }

    public VectorDataSet getXSlice(int i) {
        if ( y.rank()==1 ) {
            return new VectorDataSetAdapter( z.slice(i), y);
        } else {
            return new VectorDataSetAdapter( z.slice(i), y.slice(i) );
        }
    }

    public VectorDataSet getYSlice(int j, int table) {
        if ( y.rank()==1 ) {
            return new VectorDataSetAdapter(DataSetOps.slice1(z, j), x);
        } else {
            // warning: this assumes qube...
            return new VectorDataSetAdapter(DataSetOps.slice1(z, j), x);
        }
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
        if ( planeID.equals("") ) return this;
        if ( planes.containsKey(planeID) ) {
            return new TableDataSetAdapter( (QDataSet)planes.get(planeID), x, y );
        }
        return null;
    }

    public String[] getPlaneIds() {
        return planes.keySet().toArray(new String[planes.keySet().size()] );
    }

    public String toString() {
        return DataSetUtil.toString(this.z);
    }
}
