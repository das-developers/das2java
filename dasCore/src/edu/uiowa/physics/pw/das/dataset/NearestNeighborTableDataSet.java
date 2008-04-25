package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.datum.*;
import java.util.*;

public class NearestNeighborTableDataSet implements TableDataSet {
    
    TableDataSet source;
    
    int[] imap;
    
    int[][] jmap;
    
    int[] itableMap;
    
    RebinDescriptor ddX;
    
    RebinDescriptor ddY;
    
    NearestNeighborTableDataSet( TableDataSet source, RebinDescriptor ddX, RebinDescriptor ddY ) {
        imap= new int[ddX.numberOfBins()];
        if ( ddY==null ) {
            if ( source.tableCount()>1 ) {
                throw new IllegalArgumentException();
            }
            jmap= new int[source.tableCount()][source.getYLength(0)];
        } else {
            jmap= new int[source.tableCount()][ddY.numberOfBins()];
        }
        itableMap= new int[ddX.numberOfBins()];
        
        this.ddX= ddX;
        this.ddY= ddY;
        this.source= source;
        
        if ( source.getXLength()==0 ) {
            for ( int i=0; i<imap.length; i++ ) imap[i]= -1;
            
        } else {
            
            Datum xTagWidth= (Datum)source.getProperty("xTagWidth");
            Datum yTagWidth= (Datum)source.getProperty("yTagWidth");
            if ( xTagWidth==null ) xTagWidth= DataSetUtil.guessXTagWidth(source);
            if ( yTagWidth==null ) yTagWidth= TableUtil.guessYTagWidth(source);
            
            DatumVector xx= ddX.binCentersDV();
            double[] yy;
            if ( ddY==null ) {
                yy= TableUtil.getYTagArrayDouble( source, 0, source.getYUnits() );
            } else {
                yy= ddY.binCenters();
            }
            
            int itable0=-1;
            int guess= 0;
            for ( int i=0; i<imap.length; i++ ) {
                imap[i]= DataSetUtil.closestColumn( source, xx.get(i), guess );
                guess= imap[i];
                Datum xclose= source.getXTagDatum(imap[i]);
                Units xunits= xTagWidth.getUnits();
                
                if ( Math.abs(xclose.subtract( xx.get(i) ).doubleValue(xunits) ) > xTagWidth.doubleValue(xunits)/1.90 ) {
                    imap[i]=-1;
                } else {
                    int itable= source.tableOfIndex(imap[i]);
                    itableMap[i]= itable;
                    if ( itable0!=itable ) {
                        if ( ddY==null ) {
                            for ( int j=0; j<jmap[itable].length; j++ ) {
                                jmap[itable][j]= j;
                            }
                        } else {
                            for ( int j=0; j<jmap[itable].length; j++ ) {
                                jmap[itable][j]= TableUtil.closestRow(source,itable,yy[j], ddY.getUnits());
                                Units yunits= yTagWidth.getUnits();
                                if ( UnitsUtil.isRatiometric(yunits) ) {
                                    double yclose= source.getYTagDouble(itable, jmap[itable][j], ddY.getUnits() );
                                    if ( Math.abs( Math.log( yy[j] / yclose ) ) > yTagWidth.doubleValue(Units.logERatio)/1.90  ) jmap[itable][j]=-1;
                                } else {
                                    Datum yclose= source.getYTagDatum( itable, jmap[itable][j] );
                                    if ( Math.abs( yclose.subtract(yy[j],ddY.getUnits()).doubleValue(yunits)) > yTagWidth.doubleValue(yunits)/1.90 ) {
                                        jmap[itable][j]= -1;
                                    }
                                }
                            }
                        }
                        itable0= itable;
                    }
                }
            }
        }
    }
    
    public Datum getDatum(int i, int j) {
        if ( imap[i]!=-1 && jmap[itableMap[i]][j]!=-1 ) {
            return source.getDatum(imap[i], jmap[itableMap[i]][j]);
        } else {
            return source.getZUnits().createDatum(source.getZUnits().getFillDouble());
        }
    }
    
    public double getDouble(int i, int j, Units units) {
        try {
            if ( imap[i]!=-1 && jmap[itableMap[i]][j]!=-1 ) {
                return source.getDouble(imap[i], jmap[itableMap[i]][j], units);
            } else {
                return source.getZUnits().getFillDouble();
            }
        } catch ( ArrayIndexOutOfBoundsException e ) {
            System.err.println("here: "+e);
            throw new RuntimeException(e);
        }
    }
    
    public int getInt(int i, int j, Units units) {
        if ( imap[i]!=-1 && jmap[itableMap[i]][j]!=-1 ) {
            return source.getInt(imap[i], jmap[itableMap[i]][j],units);
        } else {
            return source.getZUnits().getFillInt();
        }
    }
    
    public DataSet getPlanarView(String planeID) {
        TableDataSet ds = (TableDataSet)source.getPlanarView(planeID);
        if (ds != null) {
            return new NearestNeighborTableDataSet(ds,ddX,ddY);
        } else {
            return null;
        }
    }
    
    public String[] getPlaneIds() {
        return source.getPlaneIds();
    }
    
    public Object getProperty(String name) {
        return source.getProperty(name);
    }
    
    public Map getProperties() {
        return source.getProperties();
    }
    
    public int getXLength() {
        return imap.length;
    }
    
    public VectorDataSet getXSlice(int i) {
        return new XSliceDataSet(this,i);
    }
    
    public VectorDataSet getYSlice(int j, int table) {
        return new YSliceDataSet(this, j, table);
    }
    
    public Datum getXTagDatum(int i) {
        return ddX.getUnits().createDatum(getXTagDouble(i,ddX.getUnits()));
    }
    
    public double getXTagDouble(int i, Units units) {
        return ddX.binCenter(i,units);
    }
    
    public int getXTagInt(int i, Units units) {
        return (int)getXTagDouble(i,units);
    }
    
    public Units getXUnits() {
        return ddX.getUnits();
    }
    
    public int getYLength(int table) {
        if ( ddY==null ) {
            return source.getYLength(table);
        } else {
            return ddY.numberOfBins();
        }
    }
    
    public Datum getYTagDatum(int table, int j) {
        if ( ddY==null ) {
            return source.getYTagDatum( table, j );
        } else {
            return ddY.getUnits().createDatum(getYTagDouble(table,j,ddY.getUnits()));
        }
    }
    
    public double getYTagDouble(int table, int j, Units units) {
        if ( ddY==null ) {
            return source.getYTagDouble( table, j, units );
        } else {
            return ddY.binCenter(j,units);
        }
    }
    
    public int getYTagInt(int table, int j, Units units) {
        return (int)getYTagDouble( table, j, units);
    }
    
    public Units getYUnits() {
        if ( ddY==null ) {
            return source.getYUnits();
        } else {
            return ddY.getUnits();
        }
    }
    
    public Units getZUnits() {
        return source.getZUnits();
    }
    
    public int tableCount() {
        return 1;
    }
    
    public int tableEnd(int table) {
        return ddX.numberOfBins();
    }
    
    public int tableOfIndex(int i) {
        return 0;
    }
    
    public int tableStart(int table) {
        return 0;
    }
    
    public String toString() {
        return "NearestNeighborTableDataSet " + TableUtil.toString(this);
    }
    
    public double[] getDoubleScan(int i, Units units) {
        int yLength = getYLength(tableOfIndex(i));
        double[] array = new double[yLength];
        for (int j = 0; j < yLength; j++) {
            array[j] = getDouble(i, j, units);
        }
        return array;
    }
    
    public DatumVector getScan(int i) {
        Units zUnits = getZUnits();
        return DatumVector.newDatumVector(getDoubleScan(i, zUnits), zUnits);
    }
    
    public DatumVector getYTags(int table) {
        double[] tags = new double[getYLength(table)];
        Units yUnits = getYUnits();
        for (int j = 0; j < tags.length; j++) {
            tags[j] = getYTagDouble(table, j, yUnits);
        }
        return DatumVector.newDatumVector(tags, yUnits);
    }

    public Object getProperty(int table, String name) {
        return getProperty(name);
    }
    
}

