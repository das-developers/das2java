/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dataset;

/**
 * return a rank N-1 dataset from a rank N dataset by slicing on the first
 * dimension.  (Rank 2, 3, and 4 supported.)
 * 
 * plane datasets are sliced as well, when they have rank 2 or greater.
 * bundle_1 handled.
 * 
 * @author jbf
 */
public class Slice1DataSet extends AbstractDataSet {

    QDataSet ds;
    int index;

    Slice1DataSet( QDataSet ds, int index ) {
        if (ds.rank() > 4 ) {
            throw new IllegalArgumentException("rank limit > 4");
        }
        if ( ds.rank()<2 ) {
            throw new IllegalArgumentException("rank limit < 2");
        }
        this.ds = ds;
        this.index = index;

        QDataSet dep1= (QDataSet) ds.property(QDataSet.DEPEND_1);
        if ( dep1.rank()>1 ) {
            System.err.println( "slice on non-qube, dep1 has rank="+dep1.rank() );
        }

        if ( dep1!=null && dep1.rank()==1 ) {
            DataSetUtil.addContext( this, new Slice0DataSet(dep1,index) );
        }
        putProperty( QDataSet.DEPEND_1, ds.property(QDataSet.DEPEND_2) );
        putProperty( QDataSet.DEPEND_2, ds.property(QDataSet.DEPEND_3) );

        putProperty( QDataSet.BUNDLE_1, null );
        putProperty( QDataSet.BINS_1, null );

        for ( int i=0; i<QDataSet.MAX_PLANE_COUNT; i++ ) {
            String prop= "PLANE_"+i;
            QDataSet plane0= (QDataSet) ds.property( prop );
            if ( plane0!=null ) {
                if ( plane0.rank()<2 ) {
                    putProperty( prop, plane0 );
                } else {
                    putProperty( prop, new Slice1DataSet( plane0, index ) );
                }
            } else {
                break;
            }
        }
    }

    public int rank() {
        return ds.rank() - 1;
    }

    public double value(int i) {
        return ds.value(i, index);
    }

    public double value(int i0, int i1) {
        return ds.value(i0, index, i1);
    }

    public double value(int i0, int i1, int i2) {
        return ds.value(i0, index, i1, i2 );
    }

    public Object property(String name) {
        if (properties.containsKey(name)) {
            return properties.get(name);
        } else {
            return ds.property(name);
        }
    }

    //TODO: consider higher-rank properties

    public int length() {
        return ds.length();
    }

    @Override
    public int length(int i) {
        return ds.length( i, index );
    }
    
    public int length(int i0,int i1) {
        return ds.length( i0, index, i1 );
    }
    
    @Override
    public boolean equals(Object obj) {
        if ( obj==null ) return false;
        if ( obj instanceof Slice1DataSet ) {
            Slice1DataSet that= ((Slice1DataSet)obj);
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
