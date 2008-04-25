package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.datum.*;
import java.util.*;

public class TagMapTableDataSet implements TableDataSet {
    
    TableDataSet source;

    // maps from TagMapTDS to source TDS
    int[] imap;
    
    // maps from TagMapTDS to source TDS
    int[][] jmap;
        
    int[] itableMap;
    
    int tableCount;
    int[] tableStart;
    int[] tableEnd;
    
    TagMapTableDataSet( TableDataSet source, int[] imap, int[][]jmap, int[] itableMap ) {
        this.imap= imap;
        this.jmap= jmap;
        this.itableMap= itableMap;
        
        int itable=itableMap[0];
        
        ArrayList tableStartList= new ArrayList();
        ArrayList tableEndList= new ArrayList();
        tableStartList.add(new Integer(0));
        
        for ( int i=1; i<itableMap.length; i++ ) {
            if ( itableMap[i]>itable ) {
                tableStartList.add( new Integer(i) );
                tableEndList.add( new Integer(i) );
            }
        }
        tableEndList.add( new Integer(itableMap.length) );
        
        tableCount= tableEndList.size();
        tableStart= new int[tableCount];        
        tableEnd= new int[tableCount];
        for ( int i=0; i<tableCount; i++ ) {
            tableStart[i]= ((Integer)tableStartList.get(i)).intValue();
            tableEnd[i]= ((Integer)tableEndList.get(i)).intValue();
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
            return new TagMapTableDataSet(ds, this.imap, this.jmap, this.itableMap );
        }
        else {
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
        return source.getXTagDatum(imap[i]);
    }
    
    public double getXTagDouble(int i, Units units) {
        return source.getXTagDouble( imap[i], units );
    }
    
    public int getXTagInt(int i, Units units) {
        return (int)getXTagDouble(i,units);
    }
    
    public Units getXUnits() {
        return source.getXUnits();
    }
    
    public int getYLength(int table) {
        return jmap[table].length;
    }
    
    public Datum getYTagDatum(int table, int j) {
        return source.getYTagDatum( table, jmap[table][j] );
    }
    
    public double getYTagDouble(int table, int j, Units units) {
        return source.getYTagDouble( table, jmap[table][j], units );
    }
    
    public int getYTagInt(int table, int j, Units units) {
        return (int)getYTagDouble( table, j, units);
    }
    
    public Units getYUnits() {
        return source.getYUnits();
    }
    
    public Units getZUnits() {
        return source.getZUnits();
    }
    
    public int tableCount() {
        return tableCount;
    }
    
    public int tableEnd(int table) {
        return tableEnd[table];
    }
    
    public int tableOfIndex(int i) {
        return 0;
    }
    
    public int tableStart(int table) {
        return tableStart[table];
    }
 
    public String toString() {
        return "TagMapTableDataSet " + TableUtil.toString(this);
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

