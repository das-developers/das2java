/*
 * AppendTableDataSet.java
 *
 * Created on September 27, 2005, 2:12 PM
 *
 *
 */

package org.das2.dataset;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Jeremy
 */
public class AppendTableDataSet implements TableDataSet {
    TableDataSet[] tableDataSets;
    int[] firstIndexs;
    int[] firstTables;
    
    public AppendTableDataSet( TableDataSet tds1, TableDataSet tds2 ) {
        List tableDataSetsList= new ArrayList();
        List firstIndexsList= new ArrayList();
        List firstTablesList= new ArrayList();
        
        if ( tds1 instanceof AppendTableDataSet && tds2 instanceof AppendTableDataSet ) {
            throw new IllegalStateException("not implemented");
        } else if ( tds1 instanceof AppendTableDataSet ) {
            AppendTableDataSet atds1= (AppendTableDataSet)tds1;
            tableDataSetsList.addAll( box( atds1.tableDataSets ) );
            firstIndexsList.addAll( box( atds1.firstIndexs ) );
            firstTablesList.addAll( box( atds1.firstTables ) );
            tableDataSetsList.add( tds2 );            
            firstIndexsList.add( new Integer( atds1.firstIndexs[atds1.tableDataSets.length]+tds2.getXLength() ) );            
            firstTablesList.add( new Integer( atds1.firstTables[atds1.tableDataSets.length]+tds2.getXLength() ) );
            
            tableDataSets= (TableDataSet[])tableDataSetsList.toArray( new TableDataSet[ tableDataSetsList.size() ] );
            firstIndexs= unbox( firstIndexsList );
            firstTables= unbox( firstTablesList );
            
        } else if ( tds2 instanceof AppendTableDataSet ) {
            throw new IllegalStateException("not implemented");
            
        } else {
            tableDataSets= new TableDataSet[2];
            tableDataSets[0]= tds1;
            tableDataSets[1]= tds2;
            firstIndexs= new int[3];
            firstIndexs[0]= 0;
            firstIndexs[1]= tds1.getXLength();
            firstIndexs[2]= firstIndexs[1] + tds2.getXLength();
            firstTables= new int[3];
            firstTables[0]= 0;
            firstTables[1]= tds1.tableCount();
            firstTables[2]= firstTables[1] + tds2.tableCount();
        }
    }
    
    private int[] unbox( List intList ) {
        int[] result= new int[intList.size()];
        for ( int i=0; i<intList.size(); i++ ) {
            result[i]= ((Integer)intList.get(i)).intValue();
        }
        return result;
    }
    
    private List box( int[] list ) {
        List result= new ArrayList(list.length);
        for ( int i=0;i<list.length; i++ ) {
            result.add( new Integer( list[i] ) );
        }
        return result;
    }

    private List box( Object[] list ) {
        List result= new ArrayList(list.length);
        for ( int i=0;i<list.length; i++ ) {
            result.add( list[i] );
        }
        return result;
    }

    private AppendTableDataSet( TableDataSet[] tableDataSets, int[] firstIndexs, int[] firstTables ) {
        this.tableDataSets= tableDataSets;
        this.firstIndexs= firstIndexs;
    }
    
    private int tdsIndex( int i ) {
        for ( int itds=0; itds<tableDataSets.length-1; itds++ ) {
            if ( firstIndexs[itds+1]>=i ) return itds;
        }
        return firstIndexs.length-1;
    }
    
    private int tdsTable( int itable ) {
        for ( int itds=0; itds<tableDataSets.length-1; itds++ ) {
            if ( firstTables[itds+1]>=itable ) return itds;
        }
        return firstIndexs.length-1;
    }
    
    public org.das2.datum.Datum getDatum(int i, int j) {
        int itds= tdsIndex(i);
        return tableDataSets[itds].getDatum( i-firstIndexs[itds],  j );
    }
    
    public double getDouble(int i, int j, org.das2.datum.Units units) {
        int itds= tdsIndex(i);
        return tableDataSets[itds].getDouble( i-firstIndexs[itds],  j, units );
    }
    
    public double[] getDoubleScan(int i, org.das2.datum.Units units) {
        int itds= tdsIndex(i);
        return tableDataSets[itds].getDoubleScan( i-firstIndexs[itds],  units );
    }
    
    public int getInt(int i, int j, org.das2.datum.Units units) {
        int itds= tdsIndex(i);
        return tableDataSets[itds].getInt( i-firstIndexs[itds],  j, units );
    }
    
    public DataSet getPlanarView(String planeID) {
        TableDataSet[] tdsPlane= new TableDataSet[tableDataSets.length];
        for ( int i=0; i<tableDataSets.length; i++ ) {
            tdsPlane[i]= (TableDataSet)tableDataSets[i].getPlanarView(planeID);
        }
        return new AppendTableDataSet( tdsPlane, firstIndexs, firstTables );
    }
    
    public String[] getPlaneIds() {
        return tableDataSets[0].getPlaneIds();
    }
    
    public java.util.Map getProperties() {
        return tableDataSets[0].getProperties();
    }
    
    public Object getProperty(String name) {
        return getProperties().get(name);
    }
    
    public org.das2.datum.DatumVector getScan(int i) {
        int itds= tdsIndex(i);
        return tableDataSets[itds].getScan( i-firstIndexs[itds] );
    }
    
    public int getXLength() {
        return firstIndexs[tableDataSets.length];
    }
    
    public VectorDataSet getXSlice(int i) {
        int itds= tdsIndex(i);
        return tableDataSets[itds].getXSlice( i-firstIndexs[itds] );
    }
    
    public org.das2.datum.Datum getXTagDatum(int i) {
        int itds= tdsIndex(i);
        return tableDataSets[itds].getXTagDatum( i-firstIndexs[itds] );
    }
    
    public double getXTagDouble(int i, org.das2.datum.Units units) {
        int itds= tdsIndex(i);
        return tableDataSets[itds].getXTagDouble( i-firstIndexs[itds],  units );
    }
    
    public int getXTagInt(int i, org.das2.datum.Units units) {
        int itds= tdsIndex(i);
        return tableDataSets[itds].getXTagInt( i-firstIndexs[itds],  units );
    }
    
    public org.das2.datum.Units getXUnits() {
        return tableDataSets[0].getXUnits();
    }
    
    public int getYLength(int table) {
        int itds= tdsTable(table);
        return tableDataSets[itds].getYLength( table-firstTables[itds] );
    }
    
    public VectorDataSet getYSlice(int j, int table) {
        int itds= tdsTable(table);
        return tableDataSets[itds].getYSlice( j, table-firstTables[itds] );
    }
    
    public org.das2.datum.Datum getYTagDatum(int table, int j) {
        int itds= tdsTable(table);
        return tableDataSets[itds].getYTagDatum( j, table-firstTables[itds] );
    }
    
    public double getYTagDouble(int table, int j, org.das2.datum.Units units) {
        int itds= tdsTable(table);
        return tableDataSets[itds].getYTagDouble( j, table-firstTables[itds], units );
    }
    
    public int getYTagInt(int table, int j, org.das2.datum.Units units) {
        int itds= tdsTable(table);
        return tableDataSets[itds].getYTagInt( j, table-firstTables[itds], units );
    }
    
    public org.das2.datum.DatumVector getYTags(int table) {
        int itds= tdsTable(table);
        return tableDataSets[itds].getYTags( table-firstTables[itds] );
    }
    
    public org.das2.datum.Units getYUnits() {
        return tableDataSets[0].getYUnits();
    }
    
    public org.das2.datum.Units getZUnits() {
        return tableDataSets[0].getZUnits();
    }
    
    public int tableCount() {
        return firstTables[tableDataSets.length];
    }
    
    public int tableEnd(int table) {
        int itds= tdsTable(table);
        return firstIndexs[itds+1];
    }
    
    public int tableOfIndex(int i) {
        int itds= tdsIndex(i);
        return tableDataSets[itds].tableOfIndex(i-firstIndexs[itds])+firstTables[itds];
    }
    
    public int tableStart(int table) {
        int itds= tdsTable(table);
        return firstIndexs[itds+1];
    }

    public Object getProperty(int table, String name) {
        int itds= tdsTable(table);
        return tableDataSets[itds].getProperty( table-firstTables[itds], name );
    }
    
}
