/*
 * DataSetAdapter.java
 *
 * Created on April 2, 2007, 8:49 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.virbo.dataset;

import org.das2.dataset.TableDataSet;
import org.das2.dataset.VectorDataSet;

/**
 * Presents legacy das2 datasets as QDataSets.
 * @author jbf
 */
public class DataSetAdapter {
    
    public static final String PROPERTY_SOURCE= "adapterSource";
    
    public static AbstractDataSet create( org.das2.dataset.DataSet ds ) {
        if ( ds instanceof VectorDataSet ) {
            return new Vector((VectorDataSet) ds);
        } else if ( ds instanceof TableDataSet ) {
            TableDataSet tds= (TableDataSet) ds;
            if ( tds.tableCount()<=1 ) {
                return new Table(tds);
            } else {
                return new MultipleTable(tds);
            }
        } else {
            throw new IllegalArgumentException("unsupported dataset type: "+ds.getClass().getName() );
        }
    }
    
    public static org.das2.dataset.DataSet createLegacyDataSet( org.virbo.dataset.QDataSet ds ) {
        if ( ds.rank()==1 ) {
            return VectorDataSetAdapter.create(ds);
        } else if ( ds.rank()==2 ) {
            return TableDataSetAdapter.create(ds);
        } else if ( ds.rank()==3 ) {
            return TableDataSetAdapter.create(ds);
        } else {
            throw new IllegalArgumentException("unsupported rank: "+ds.rank() );
        }
    }

    static class MultiTableXTagsDataSet extends AbstractDataSet {
        org.das2.dataset.DataSet source;
        int offset;
        int length;
        MultiTableXTagsDataSet( org.das2.dataset.DataSet source, int offset, int length ) {
            this.source= source;
            this.offset= offset;
            this.length= length;
            properties.put( QDataSet.UNITS, source.getXUnits() );
            Object o= source.getProperty( org.das2.dataset.DataSet.PROPERTY_X_MONOTONIC );
            if ( o!=null ) properties.put( QDataSet.MONOTONIC, o );
        }

        public int rank() {
            return 1;
        }

        public double value(int i) {
            return source.getXTagDouble( i+offset, source.getXUnits() );
        }

        public int length() {
            return length;
        }

    }
    static class XTagsDataSet extends AbstractDataSet {
        org.das2.dataset.DataSet source;
        XTagsDataSet( org.das2.dataset.DataSet source ) {
            this.source= source;
            properties.put( QDataSet.UNITS, source.getXUnits() );
            Object o= source.getProperty( org.das2.dataset.DataSet.PROPERTY_X_MONOTONIC );
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
        int table;
        YTagsDataSet( TableDataSet source, int table ) {
            this.source= source;
            properties.put( QDataSet.UNITS, source.getYUnits() );
            
        }
        public int rank() {
            return 1;
        }
        
        public double value(int i) {
            return source.getYTagDouble( table, i, source.getYUnits() );
        }
        
        public int length() {
            return source.tableCount()>0 ? source.getYLength(0) : 99;
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
            properties.put( QDataSet.DEPEND_1, new YTagsDataSet( source, 0 ) );
            properties.put( QDataSet.QUBE, Boolean.TRUE );
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
        
    }

    static class MultipleTable extends AbstractDataSet {
        TableDataSet source;

        MultipleTable( TableDataSet source ) {
            super();

            this.source= source;
            properties.put( QDataSet.UNITS, source.getZUnits() );
            properties.put( PROPERTY_SOURCE, source );
        }

        public int rank() {
            return 3;
        }

        public int length() {
            return source.tableCount();
        }

        public int length(int i) {
            return source.tableEnd(i) - source.tableStart(i);
        }

        public int length(int i, int j) {
            try {
                return source.getYLength( i );
            } catch ( IndexOutOfBoundsException ex ) {
                throw ex;
            }
        }

        public double value( int i, int j, int k ) {
            int ts= source.tableStart(i);
            try {
                return source.getDouble( ts+j, k, source.getZUnits() );
            } catch ( IndexOutOfBoundsException ex ) {
                throw ex;
            }
        }

        @Override
        public Object property(String name, int i) {
            if ( name.equals( QDataSet.DEPEND_0 ) ) {
                return new MultiTableXTagsDataSet( source, source.tableStart(i), source.tableEnd(i)-source.tableStart(i) );
            } else if ( name.equals( QDataSet.DEPEND_1 ) ) {
                return new YTagsDataSet( source, i );
            } else {
                return super.property(name, i);
            }
        }



    }
}
