package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.datum.*;

public class NearestNeighborTableDataSet implements TableDataSet {
    
    TableDataSet source;
    
    int[] imap;
    
    int[][] jmap;
    
    int[] itableMap;
    
    RebinDescriptor ddX;
    
    RebinDescriptor ddY;
    
    NearestNeighborTableDataSet(TableDataSet source, RebinDescriptor ddX, RebinDescriptor ddY) {
        imap= new int[ddX.numberOfBins()];
        jmap= new int[source.tableCount()][ddY.numberOfBins()];
        itableMap= new int[ddX.numberOfBins()];
        
        this.ddX= ddX;
        this.ddY= ddY;
        this.source= source;
        
        double[] xx= ddX.binCenters();
        double[] yy= ddY.binCenters();
        
        int itable0=-1;
        for ( int i=0; i<imap.length; i++ ) {
            imap[i]= TableUtil.closestColumn(source, xx[i], ddX.getUnits() );
            int itable= source.tableOfIndex(imap[i]);
            itableMap[i]= itable;
            if ( itable0!=itable ) {
                for ( int j=0; j<jmap[itable].length; j++ ) {
                    jmap[itable][j]= TableUtil.closestRow(source,itable,yy[j], ddY.getUnits());
                }
                itable0= itable;
            }
        }
    }
    
    public Datum getDatum(int i, int j) {
        return source.getDatum(imap[i], jmap[itableMap[i]][j]);
    }
    
    public double getDouble(int i, int j, Units units) {
        return source.getDouble(imap[i], jmap[itableMap[i]][j], units);
    }
    
    public int getInt(int i, int j, Units units) {
        return source.getInt(imap[i], jmap[itableMap[i]][j],units);
    }
    
    public DataSet getPlanarView(String planeID) {
        return new NearestNeighborTableDataSet((TableDataSet)source.getPlanarView(planeID),ddX,ddY);
    }
    
    public Object getProperty(String name) {
        return source.getProperty(name);
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
        return ddX.binCenter(i);
    }
    
    public int getXTagInt(int i, Units units) {
        return (int)getXTagDouble(i,units);
    }
    
    public Units getXUnits() {
        return ddX.getUnits();
    }
    
    public int getYLength(int table) {
        return ddY.numberOfBins();
    }    
    
    public Datum getYTagDatum(int table, int j) {
        return ddY.getUnits().createDatum(getYTagDouble(table,j,ddY.getUnits()));
    }
    
    public double getYTagDouble(int table, int j, Units units) {
        return ddY.binCenter(j);
    }
    
    public int getYTagInt(int table, int j, Units units) {
        return (int)getYTagDouble( table, j, units);
    }
    
    public Units getYUnits() {
        return ddY.getUnits();
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
    
}

