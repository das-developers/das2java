package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.datum.*;

public class XSliceDataSet extends ViewDataSet implements VectorDataSet {
    
    private int iIndex;
    
    private TableDataSet tdsSource;
    
    public XSliceDataSet(TableDataSet tdsSource, int i) {
        super(tdsSource);
        this.tdsSource = (TableDataSet)tdsSource;
        this.iIndex = i;
    }
    
    public DataSet getPlanarView(String planeID) {
        return null;
    }
    
    public Datum getDatum(int i) {
        return tdsSource.getDatum(iIndex, i);
    }
    
    public double getDouble(int i, Units units) {
        return tdsSource.getDouble(iIndex, i, units);
    }
    
    public int getInt(int i, Units units) {
        return tdsSource.getInt(iIndex, i, units);
    }
    
    public Datum getXTagDatum(int i) {
        int table = tdsSource.tableOfIndex(iIndex);
        return tdsSource.getYTagDatum(table, i);
    }
    
    public int getXLength() {
        int table = tdsSource.tableOfIndex(iIndex);
        return tdsSource.getYLength(table);
    }
    
    public Units getXUnits() {
        return tdsSource.getYUnits();
    }
    
    public double getXTagDouble(int i, Units units) {
        int table = tdsSource.tableOfIndex(iIndex);
        return tdsSource.getYTagDouble(table, i, units);
    }
    
    public Units getYUnits() {
        return tdsSource.getZUnits();
    }
    
    public int getXTagInt(int i, Units units) {
        int table = tdsSource.tableOfIndex(iIndex);
        return tdsSource.getYTagInt(table, i, units);
    }
    
    public Object getProperty(String name) {
        return null;
    }
    
}

