/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dataset;

/**
 * return a rank N-1 dataset from a rank N dataset by slicing on the third
 * dimension.  (Rank 3 and 4 supported.)
 * 
 * plane datasets are sliced as well, when they have rank 3 or greater.
 * 
 * @author jbf
 */
public class Slice2DataSet extends AbstractDataSet {

    QDataSet ds;
    int index;

    public Slice2DataSet(QDataSet ds, int index) {
        this( ds, index, true );
    }

    Slice2DataSet( QDataSet ds, int index, boolean addContext ) {
        if (ds.rank() > 4 ) {
            throw new IllegalArgumentException("rank limit > 4");
        }
        if ( ds.rank()<3 ) {
            throw new IllegalArgumentException("rank limit < 3");
        }
        if ( index>= ds.length(0,0) ) throw new IndexOutOfBoundsException("slice2 index is out of bounds");

        this.ds = ds;
        this.index = index;

        if ( addContext ) {
            QDataSet bundle= (QDataSet) ds.property( QDataSet.BUNDLE_2 );
            QDataSet dep2= (QDataSet) ds.property(QDataSet.DEPEND_2);
            if ( bundle!=null && dep2==null ) {
                QDataSet context=null;
                if ( addContext ) {
                    context= DataSetOps.getContextForUnbundle( bundle, index );
                    DataSetUtil.addContext( this, context );
                    putProperty( QDataSet.NAME, bundle.property(QDataSet.NAME,index) );
                }
            } else {
                if ( dep2!=null ) {
                    if ( dep2.rank()==1 ) {
                        DataSetUtil.addContext( this, new Slice0DataSet(dep2,index,false) );
                    } else if ( dep2.rank()==2 ) {
                        DataSetUtil.addContext( this, new Slice1DataSet(dep2,index,false) );
                    } else {
                        System.err.println( "slice on non-qube, dep3 has rank="+dep2.rank() );
                    }
                } else {
                    DRank0DataSet context= DataSetUtil.asDataSet(index);
                    context.putProperty( QDataSet.NAME, "slice2" );
                    DataSetUtil.addContext( this, context );
                }
            }
        }

        putProperty( QDataSet.DEPEND_0, ds.property(QDataSet.DEPEND_0) );
        putProperty( QDataSet.DEPEND_1, ds.property(QDataSet.DEPEND_1) );
        putProperty( QDataSet.DEPEND_2, ds.property(QDataSet.DEPEND_3) );

        putProperty( QDataSet.BUNDLE_0, ds.property( QDataSet.BUNDLE_0 ) );
        putProperty( QDataSet.BUNDLE_1, ds.property( QDataSet.BUNDLE_1 ) );
        putProperty( QDataSet.BINS_0, ds.property( QDataSet.BINS_0 ) );
        putProperty( QDataSet.BINS_1, ds.property( QDataSet.BINS_1 ) );

        for ( int i=0; i<QDataSet.MAX_PLANE_COUNT; i++ ) {
            String prop= "PLANE_"+i;
            QDataSet plane0= (QDataSet) ds.property( prop );
            if ( plane0!=null ) {
                if ( plane0.rank()<3 ) {
                    putProperty( prop, plane0 );
                } else {
                    putProperty( prop, new Slice2DataSet( plane0, index ) );
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

    public double value(int i0, int i1) {
        return ds.value(i0, i1, index );
    }

    public double value(int i0, int i1, int i2) {
        return ds.value(i0, i1, index, i2 );
    }

    public Object property(String name) {
        if (properties.containsKey(name)) {
            return properties.get(name);
        } else {
            if ( DataSetUtil.isInheritedProperty(name)) {
                return ds.property(name);
            } else {
                return null;
            }
        }
    }

    public int length() {
        return ds.length();
    }

    public int length(int i) {
        return ds.length( i );
    }
    
    public int length(int i0,int i1) { // the highest this rank can be is 3.
        return ds.length( i0, i1, index );
    }
    
    @Override
    public boolean equals(Object obj) {
        if ( obj==null ) return false;
        if ( obj instanceof Slice2DataSet ) {
            Slice2DataSet that= ((Slice2DataSet)obj);
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
