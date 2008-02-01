/*
 * DataSetAdapter.java
 *
 * Created on April 2, 2007, 8:49 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.virbo.dataset;

import edu.uiowa.physics.pw.das.dataset.TableDataSet;
import edu.uiowa.physics.pw.das.dataset.VectorDataSet;

/**
 * Presents legacy das2 datasets as QDataSets.
 * @author jbf
 */
public class DataSetAdapter {
    
    public static final String PROPERTY_SOURCE= "adapterSource";
    
    public static AbstractDataSet create( edu.uiowa.physics.pw.das.dataset.DataSet ds ) {
        if ( ds instanceof VectorDataSet ) {
            return new Vector((VectorDataSet) ds);
        } else if ( ds instanceof TableDataSet ) {
            return new Table((TableDataSet) ds);
        } else {
            throw new IllegalArgumentException("unsupported dataset type: "+ds.getClass().getName() );
        }
    }
    
    static class XTagsDataSet extends AbstractDataSet {
        edu.uiowa.physics.pw.das.dataset.DataSet source;
        XTagsDataSet( edu.uiowa.physics.pw.das.dataset.DataSet source ) {
            this.source= source;
            properties.put( QDataSet.UNITS, source.getXUnits() );
            Object o= source.getProperty( edu.uiowa.physics.pw.das.dataset.DataSet.PROPERTY_X_MONOTONIC );
            if ( o!=null ) properties.put( QDataSet.MONOTONIC, o );
        }
        
        public int rank() {
            return 1;
        }
        
        public double value(int i) {
            return source.getXTagDouble( i, source.getXUnits() );
        }
        
        public int length() {
            return source.getXLength();
        }
        
        public void putProperty(String name, int index1, int index2, Object value) {
            throw new IllegalArgumentException("not supported");
        }
        
        public void putProperty(String name, int index, Object value) {
            throw new IllegalArgumentException("not supported");
        }
        
        public void putProperty(String name, Object value) {
            throw new IllegalArgumentException("not supported");
        }
        
    }
    
    static class Vector extends AbstractDataSet {
        VectorDataSet source;
        
        Vector( VectorDataSet source ) {
            super();
            this.source= source;
            properties.put( QDataSet.UNITS, source.getYUnits() );
            properties.put( QDataSet.DEPEND_0, new XTagsDataSet( source ) );
            properties.put( PROPERTY_SOURCE, source );
        }
        
        public int rank() {
            return 1;
        }
        
        public double value(int i) {
            return source.getDouble( i, source.getYUnits() );
        }
        
        public int length() {
            return source.getXLength();
        }
        
    }
    
    static class YTagsDataSet extends AbstractDataSet {
        TableDataSet source;
        YTagsDataSet( TableDataSet source ) {
            this.source= source;
            properties.put( QDataSet.UNITS, source.getYUnits() );
            
        }
        public int rank() {
            return 1;
        }
        
        public double value(int i) {
            return source.getYTagDouble( 0, i, source.getYUnits() );
        }
        
        public int length() {
            return source.getYLength(0);
        }
    }
    
    static class Table extends AbstractDataSet {
        TableDataSet source;
        
        Table( TableDataSet source ) {
            super();
            if ( source.tableCount() > 1 ) throw new IllegalArgumentException("only simple tables are supported" );
            
            this.source= source;
            properties.put( QDataSet.UNITS, source.getZUnits() );
            properties.put( QDataSet.DEPEND_0, new XTagsDataSet( source ) );
            properties.put( QDataSet.DEPEND_1, new YTagsDataSet( source ) );
            properties.put( PROPERTY_SOURCE, source );
        }
        
        public int rank() {
            return 2;
        }
        
        public int length(int i) {
            return source.getYLength( 0 );
        }
        
        public double value( int i, int j ) {
            return source.getDouble( i, j, source.getZUnits() );
        }
        
        public int length() {
            return source.getXLength();
        }
        
        public void putProperty(String name, Object value) {
            throw new IllegalArgumentException("not supported");
        }
        
        public void putProperty(String name, int index, Object value) {
            throw new IllegalArgumentException("not supported");
        }
        
        public void putProperty(String name, int index1, int index2, Object value) {
            throw new IllegalArgumentException("not supported");
        }
        
    }
    
    
}
