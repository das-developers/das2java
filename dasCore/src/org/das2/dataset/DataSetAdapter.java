/*
 * DataSetAdapter.java
 *
 * Created on April 2, 2007, 8:49 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.dataset;

import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.virbo.dataset.AbstractDataSet;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DRank0DataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dsops.Ops;

/**
 * Presents legacy das2 datasets as QDataSets.
 * See also TableDataSetAdapter,VectorDataSetAdapter
 * @author jbf
 */
public class DataSetAdapter {
    
    public static final String PROPERTY_SOURCE= "adapterSource";
    
    public static AbstractDataSet create( org.das2.dataset.DataSet ds ) {
        if ( ( ds instanceof VectorDataSet ) && ds.getPlaneIds().length>2 ) {  // TCAs  kludge
            VectorDataSet vds= (VectorDataSet)ds;
            AbstractDataSet bds= (AbstractDataSet) Ops.bundle( null, new Vector(vds) );
            String[] planes= ds.getPlaneIds();
            for ( int i=1; i<planes.length; i++ ) {
                Vector v= new Vector((VectorDataSet)vds.getPlanarView(planes[i]));
                v.putProperty( QDataSet.NAME, planes[i] );
                Ops.bundle( bds, v );
            }
            bds.putProperty( QDataSet.DEPEND_0, new XTagsDataSet(vds) );
            return bds;
        } else if ( ds instanceof VectorDataSet ) {
            return new Vector((VectorDataSet) ds);
        } else if ( ds instanceof TableDataSet ) {
            TableDataSet tds= (TableDataSet) ds;
            if ( tds.tableCount()<=1 ) {
                return new SimpleTable(tds);
            } else {
                if ( tds instanceof DefaultTableDataSet && tds.tableCount()>tds.getXLength()/2 ) {
                    return ((DefaultTableDataSet)tds).toQDataSet();
                } else {                
                    return new MultipleTable(tds);
                }
            }
        } else if ( ds==null ) {
            throw new NullPointerException("dataset is null");
        } else {
            throw new IllegalArgumentException("unsupported dataset type: "+ds.getClass().getName() );
        }
    }
    
    public static org.das2.dataset.DataSet createLegacyDataSet( org.virbo.dataset.QDataSet ds ) {
        if ( ds.rank()==1 ) {
            return VectorDataSetAdapter.create(ds);
        } else if ( SemanticOps.isBundle(ds) ) {
            return VectorDataSetAdapter.createFromBundle(ds);
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
            properties.put( QDataSet.LABEL, source.getProperty( org.das2.dataset.DataSet.PROPERTY_X_LABEL ) );
            Object o= source.getProperty( org.das2.dataset.DataSet.PROPERTY_X_MONOTONIC );
            if ( o!=null ) properties.put( QDataSet.MONOTONIC, o );
        }

        public int rank() {
            return 1;
        }

		  @Override
        public double value(int i) {
            return source.getXTagDouble( i+offset, source.getXUnits() );
        }

		  @Override
        public int length() {
            return length;
        }

    }
    static class XTagsDataSet extends AbstractDataSet {
        org.das2.dataset.DataSet source;
        XTagsDataSet( org.das2.dataset.DataSet source ) {
            this.source= source;
            properties.put( QDataSet.UNITS, source.getXUnits() );
            properties.put( QDataSet.LABEL, source.getProperty( DataSet.PROPERTY_X_LABEL ) );
				
				// QDataSet Cadences are a rank 0 dataset
				Datum d = (Datum) source.getProperty(DataSet.PROPERTY_X_TAG_WIDTH);
				if(d != null)
					properties.put( QDataSet.CADENCE, DRank0DataSet.create(d));
				
            Object o = source.getProperty( org.das2.dataset.DataSet.PROPERTY_X_MONOTONIC );
            if ( o!=null ) properties.put( QDataSet.MONOTONIC, o );
        }
        
        public int rank() {
            return 1;
        }
        
		  @Override
        public double value(int i) {
            return source.getXTagDouble( i, source.getXUnits() );
        }
        
		  @Override
        public int length() {
            return source.getXLength();
        }
        
    }
    
    static class Vector extends AbstractDataSet {
        VectorDataSet source;
        
        Vector( VectorDataSet source ) {
            super();
            this.source= source;
            properties.put( QDataSet.TITLE, source.getProperty( DataSet.PROPERTY_TITLE ) );
            properties.put( QDataSet.UNITS, source.getYUnits() );
            properties.put( QDataSet.LABEL, source.getProperty( DataSet.PROPERTY_Y_LABEL ) );
            properties.put( QDataSet.DEPEND_0, new XTagsDataSet( source ) );
            properties.put( PROPERTY_SOURCE, source );
        }
        
        public int rank() {
            return 1;
        }
        
		  @Override
        public double value(int i) {
            return source.getDouble( i, source.getYUnits() );
        }
        
		  @Override
        public int length() {
            return source.getXLength();
        }
        
    }
    
    static class YTagsDataSet extends AbstractDataSet {
        TableDataSet source;
        int table;
        YTagsDataSet( TableDataSet source, int table ) {
            this.source= source;
            this.table= table;
            properties.put( QDataSet.UNITS, source.getYUnits() );
            properties.put( QDataSet.LABEL, source.getProperty( DataSet.PROPERTY_Y_LABEL ) );

        }
        public int rank() {
            return 1;
        }
        
		  @Override
        public double value(int i) {
            return source.getYTagDouble( table, i, source.getYUnits() );
        }
        
		  @Override
        public int length() {
            return source.tableCount()>0 ? source.getYLength(table) : 99;
        }
    }
    
    static class SimpleTable extends AbstractDataSet {
        TableDataSet source;
        
        SimpleTable( TableDataSet source ) {
            super();
            if ( source.tableCount() > 1 ) 
					throw new IllegalArgumentException("only simple tables are supported" );
            
            this.source= source;
            properties.put( QDataSet.UNITS, source.getZUnits() );
            properties.put( QDataSet.LABEL, source.getProperty( DataSet.PROPERTY_Z_LABEL ) );
            properties.put( QDataSet.TITLE, source.getProperty( DataSet.PROPERTY_TITLE ) );
            QDataSet xtags= new XTagsDataSet( source );
            properties.put( QDataSet.DEPEND_0, xtags );
            QDataSet ytags= new YTagsDataSet( source, 0 );
            properties.put( QDataSet.DEPEND_1, ytags );
            properties.put( QDataSet.QUBE, Boolean.TRUE );
            properties.put( PROPERTY_SOURCE, source );
				
				//Let Das2 Streams set a Z-Axis range
				DatumRange zRng = (DatumRange) source.getProperty(DataSet.PROPERTY_Z_RANGE);
				if(zRng != null){
					properties.put( QDataSet.TYPICAL_MIN, zRng.min().value());
					properties.put( QDataSet.TYPICAL_MAX, zRng.max().value());
				}
				properties.put( QDataSet.RENDER_TYPE, source.getProperty(DataSet.PROPERTY_RENDERER));
				properties.put( QDataSet.MONOTONIC, source.getProperty(DataSet.PROPERTY_X_MONOTONIC));
				properties.put( QDataSet.FILL_VALUE, source.getProperty(DataSet.PROPERTY_Z_FILL));
				
				properties.put(QDataSet.VALID_MIN, source.getProperty(DataSet.PROPERTY_Z_VALID_MIN));
				properties.put(QDataSet.VALID_MIN, source.getProperty(DataSet.PROPERTY_Z_VALID_MAX));
				
				// Just throw the rest of this jaz into user_properties so that I can at least
				// see it.
				properties.put( QDataSet.USER_PROPERTIES, source.getProperties());
				
        }
        
        public int rank() {
            return 2;
        }
        
		  @Override
        public int length(int i) {
            return source.getYLength( 0 );
        }
        
		  @Override
        public double value( int i, int j ) {
            return source.getDouble( i, j, source.getZUnits() );
        }
        
		  @Override
        public int length() {
            return source.getXLength();
        }
        
    }

    static class MultipleTable extends AbstractDataSet {
        TableDataSet source;

        MultipleTable( TableDataSet source ) {
            super();

            this.source= source;
            properties.put( QDataSet.JOIN_0, DDataSet.create( new int[0] ) );
            properties.put( QDataSet.UNITS, source.getZUnits() );
            properties.put( PROPERTY_SOURCE, source );
            properties.put( QDataSet.TITLE, source.getProperty( DataSet.PROPERTY_TITLE ) );
				//cwp
				//properties.put( QDataSet.FILL_VALUE, source.getProperty( source.p))
        }

        public int rank() {
            return 3;
        }

		  @Override
        public int length() {
            return source.tableCount();
        }

		  @Override
        public int length(int i) {
            return source.tableEnd(i) - source.tableStart(i);
        }

		  @Override
        public int length(int i, int j) {
            try {
                return source.getYLength( i );
            } catch ( IndexOutOfBoundsException ex ) {
                throw ex;
            }
        }

		  @Override
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
