/*
 * ClippedTableDataSet.java
 *
 * Created on February 16, 2004, 12:19 PM
 */

package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.datum.*;

/**
 *
 * @author  Jeremy
 */
public class ClippedTableDataSet implements TableDataSet {
    
    /*
     * clippedTableDataSet
     *
     * TableDataSet that is a view of a section of a TableDataSet including an X and Y range,
     * but not much more.
     */
    
    TableDataSet source;
    
    int xoffset;
    int xlength;
    
    int[] yoffsets;
    int[] ylengths;
    
    int tableOffset;
    int tableCount;
    
    void calculateXOffset( Datum xmin, Datum xmax ) {
        xoffset= DataSetUtil.getPreviousColumn(source, xmin);
        int ix1= DataSetUtil.getNextColumn(source, xmax );
        xlength= ix1- xoffset;
    }
    
    void calculateTableOffset() {  
        tableOffset= -99;
        for ( int itable=0; itable<source.tableCount(); itable++ ) {
            if ( tableOffset==-99 
                && source.tableEnd(itable) > xoffset ) {
                    tableOffset= itable;
            }
            if ( tableOffset!=-99  
                && source.tableEnd(itable) >= xoffset+xlength ) {
                    tableCount= itable - tableOffset + 1;
            }
        }
    }
    
    void calculateYOffsets( Datum ymin, Datum ymax ) {        
        yoffsets= new int[tableCount];
        ylengths= new int[tableCount];
        for ( int itable=tableOffset; itable<tableOffset+tableCount; itable++ ) {
            yoffsets[itable-tableOffset]= TableUtil.getPreviousRow(source, itable, ymin);
            int ix1= TableUtil.getNextRow(source, itable, ymax );
            ylengths[itable-tableOffset]= ix1- yoffsets[itable];
        }
    }
    
    /** Creates a new instance of ClippedTableDataSet */
    public ClippedTableDataSet( TableDataSet source, Datum xmin, Datum xmax, Datum ymin, Datum ymax ) {
        this.source= source;
        calculateXOffset( xmin, xmax );
        calculateTableOffset();
        calculateYOffsets( ymin, ymax );
    }
    
    public ClippedTableDataSet( TableDataSet source, int xoffset, int xlength, 
    int yoffset, int ylength ) {
        if ( source.tableCount() > 1 ) {
            throw new IllegalArgumentException( "this ClippedTableDataSet constructor requires that there be only one table" );
        }
        if ( source.getXLength() < xoffset+xlength ) {
            throw new IllegalArgumentException( "xoffset + xlength greater than the number of XTags in source" );
        }
        if ( source.getYLength(0) < yoffset+ylength ) {
            throw new IllegalArgumentException( "yoffset + ylength greater than the number of YTags in source" );
        }
        if ( yoffset<0 || xoffset<0 ) {
            throw new IllegalArgumentException( "yoffset("+yoffset+") or xoffset("+xoffset+") is negative" );
        }
        this.source= source;
        this.xoffset= xoffset;
        this.xlength= xlength;
        this.tableOffset= 0;
        this.tableCount= 1;
        this.yoffsets= new int[] { yoffset };
        this.ylengths= new int[] { ylength };
    }
    
    private ClippedTableDataSet( TableDataSet source, int xoffset, int xlength, 
    int [] yoffsets, int [] ylengths, int tableOffset, int tableCount ) {
        if ( source==null ) {
            throw new IllegalArgumentException("source is null");
        }
        this.source= source;
        this.xoffset= xoffset;
        this.xlength= xlength;
        this.yoffsets= yoffsets;
        this.ylengths= ylengths;
        this.tableOffset= tableOffset;
        this.tableCount= tableCount;
    }
    
    
    public Datum getDatum(int i, int j) {
        return source.getDatum( i+xoffset, j+yoffsets[tableOfIndex(i)] );
    }
    
    public double getDouble(int i, int j, Units units) {
        return source.getDouble( i+xoffset, j+yoffsets[tableOfIndex(i)], units );
    }
    
    public int getInt(int i, int j, Units units) {
        return source.getInt( i+xoffset, j+yoffsets[tableOfIndex(i)], units );
    }
    
    public DataSet getPlanarView(String planeID) {
        return new ClippedTableDataSet((TableDataSet)source.getPlanarView(planeID),xoffset,xlength,yoffsets,ylengths,tableOffset,tableCount);
    }
    
    public Object getProperty(String name) {
        return source.getProperty(name);
    }
    
    public int getXLength() {
        return xlength;
    }
    
    public VectorDataSet getXSlice(int i) {
        return source.getXSlice( i+xoffset );
    }
    
    public Datum getXTagDatum(int i) {
        return source.getXTagDatum( i+xoffset );
    }    
    
    public double getXTagDouble(int i, Units units) {
        return source.getXTagDouble( i+xoffset, units );
    }
    
    public int getXTagInt(int i, Units units) {
        return source.getXTagInt( i+xoffset, units );
    }
    
    public Units getXUnits() {
        return source.getXUnits();
    }
    
    public int getYLength(int table) {
        return ylengths[table];
    }
    
    public VectorDataSet getYSlice(int j, int table) {
        return source.getYSlice( j+yoffsets[table], table+tableOffset );
    }
    
    public Datum getYTagDatum(int table, int j) {
        return source.getYTagDatum( table+tableOffset, j+yoffsets[table] );
    }
    
    public double getYTagDouble(int table, int j, Units units) {
        return source.getYTagDouble( table+tableOffset, j+yoffsets[table], units );
    }
    
    public int getYTagInt(int table, int j, Units units) {
        return source.getYTagInt( table+tableOffset, j+yoffsets[table], units );
    }
    
    public edu.uiowa.physics.pw.das.datum.Units getYUnits() {
        return source.getYUnits();
    }
    
    public edu.uiowa.physics.pw.das.datum.Units getZUnits() {
        return source.getZUnits();
    }
    
    public int tableCount() {        
        return tableCount;
    }
    
    public int tableEnd(int table) {
        int i= source.tableEnd(table+tableOffset) - xoffset;
        if ( i>getXLength() ) {
            return getXLength();
        } else {
            return i;
        }        
    }
    
    public int tableOfIndex(int i) {
        return source.tableOfIndex( i+xoffset ) - tableOffset;
    }
    
    public int tableStart(int table) {
        int i= source.tableStart(table+tableOffset) - xoffset;
        if ( i<0 ) {
            return 0;
        } else {
            return i;
        }        
    }
    
    public String toString() {
        return "ClippedTableDataSet " + TableUtil.toString(this);
    }
    
}
