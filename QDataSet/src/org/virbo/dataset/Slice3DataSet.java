/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dataset;

/**
 * return a rank N-1 dataset from a rank N dataset by slicing on the fourth
 * dimension.  (Rank 4 supported.)
 * 
 * plane datasets are sliced as well, when they have rank 4 or greater.
 * 
 * @author jbf
 */
public class Slice3DataSet extends AbstractDataSet {

    QDataSet ds;
    int index;

    public Slice3DataSet(QDataSet ds, int index) {
        this( ds, index, true );
    }

    Slice3DataSet( QDataSet ds, int index, boolean addContext ) {
        if (ds.rank() > 4 ) {
            throw new IllegalArgumentException("rank limit > 4");
        }
        if ( ds.rank() < 4 ) {
            throw new IllegalArgumentException("rank limit < 4");
        }
        this.ds = ds;
        this.index = index;

        if ( addContext ) {
            QDataSet bundle= (QDataSet) ds.property( QDataSet.BUNDLE_3 );
            QDataSet dep3= (QDataSet) ds.property(QDataSet.DEPEND_3);
            if ( bundle!=null && dep3==null ) {
                QDataSet context=null;
                if ( addContext ) {
                    context= DataSetOps.getContextForUnbundle( bundle, index );
                    DataSetUtil.addContext( this, context );
                    putProperty( QDataSet.NAME, bundle.property(QDataSet.NAME,index) );
                }
            } else {
                if ( dep3!=null ) {
                    if ( dep3.rank()==1 ) {
                        DataSetUtil.addContext( this, new Slice0DataSet(dep3,index,false) );
                    } else if ( dep3.rank()==2 ) {
                        DataSetUtil.addContext( this, new Slice1DataSet(dep3,index,false) );
                    } else {
                        System.err.println( "slice on non-qube, dep3 has rank="+dep3.rank() );
                    }
                } else {
                    DRank0DataSet context= DataSetUtil.asDataSet(index);
                    context.putProperty( QDataSet.NAME, "slice3" );
                    DataSetUtil.addContext( this, context );
                }
            }
        }

        putProperty( QDataSet.DEPEND_0, ds.property(QDataSet.DEPEND_0) );
        putProperty( QDataSet.DEPEND_1, ds.property(QDataSet.DEPEND_1) );
        putProperty( QDataSet.DEPEND_2, ds.property(QDataSet.DEPEND_2) );
        putProperty( QDataSet.DEPEND_3, null );

        putProperty( QDataSet.BUNDLE_0, ds.property( QDataSet.BUNDLE_0 ) );
        putProperty( QDataSet.BUNDLE_1, ds.property( QDataSet.BUNDLE_1 ) );
        putProperty( QDataSet.BUNDLE_2, ds.property( QDataSet.BUNDLE_2 ) );
        putProperty( QDataSet.BINS_0, ds.property( QDataSet.BINS_0 ) );
        putProperty( QDataSet.BINS_1, ds.property( QDataSet.BINS_1 ) );
        
        for ( int i=0; i<QDataSet.MAX_PLANE_COUNT; i++ ) {
            String prop= "PLANE_"+i;
            QDataSet plane0= (QDataSet) ds.property( prop );
            if ( plane0!=null ) {
                if ( plane0.rank()<4 ) {
                    putProperty( prop, plane0 );
                } else {
                    putProperty( prop, new Slice3DataSet( plane0, index ) );
                }
            } else {
                break;
            }
        }
        
        putProperty( QDataSet.WEIGHTS, null );

        DataSetUtil.copyDimensionProperties( ds, this );
        DataSetUtil.maybeCopyRenderType( ds, this );

    }

    public int rank() {
        return ds.rank() - 1;
    }

    public double value(int i0, int i1, int i2) {
        return ds.value( i0, i1, i2, index );
    }

    public Object property(String name) {
        if (properties.containsKey(name)) {
            return properties.get(name);
        } else {
            if ( DataSetUtil.isInheritedProperty(name) ) {
                return ds.property(name);
            } else {
                return null;
            }
            
        }
    }

    public int length() {
        return ds.length();
    }

    @Override
    public int length(int i) {
        return ds.length( i );
    }
    
    public int length(int i0,int i1) { //TODO: really? interesting...
        return ds.length( i0, i1 );
    }
    
    @Override
    public boolean equals(Object obj) {
        if ( obj==null ) return false;
        if ( obj instanceof Slice3DataSet ) {
            Slice3DataSet that= ((Slice3DataSet)obj);
            return that.ds.equals(this.ds) && that.index==this.index;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return ds.hashCode() + index;
    }
}
