package org.das2.dataset;

import org.das2.datum.Units;
import org.das2.datum.Datum;

public class YSliceDataSet extends ViewDataSet implements VectorDataSet {
    
    private final int table;    
    private final int jIndex;
    private final int offset;
    private final TableDataSet tdsSource;
    
    public YSliceDataSet(TableDataSet source, int jIndex, int table) {
        super(source);
        this.tdsSource= source;
        this.jIndex = jIndex;
        this.table = table;  
        this.offset= source.tableStart(table);
    }
    
    public DataSet getPlanarView(String planeID) {
        return null;
    }
    
    public String[] getPlaneIds() {
        return new String[0];
    }
    
    public Datum getDatum(int i) {
        return tdsSource.getDatum(i + offset, jIndex);
    }
    
    public double getDouble(int i, Units units) {
        return tdsSource.getDouble(i + offset, jIndex, units);
    }
    
    public int getInt(int i, Units units) {
        return tdsSource.getInt(i + offset, jIndex, units);
    }
    
    public Datum getXTagDatum(int i) {
        return tdsSource.getXTagDatum(i + offset);
    }
    
    public int getXLength() {
        return tdsSource.tableEnd(table) - tdsSource.tableStart(table);
    }
    
    public double getXTagDouble(int i, Units units) {
        return tdsSource.getXTagDouble(i + offset, units);
    }
    
    public Units getYUnits() {
        return tdsSource.getZUnits();
    }
    
    public int getXTagInt(int i, Units units) {
        return tdsSource.getXTagInt(i + offset, units);
    }
    
}

