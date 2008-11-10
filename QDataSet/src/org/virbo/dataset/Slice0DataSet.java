/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dataset;

/**
 * Wraps a rank N dataset, slicing on an index of the first dimension to make a rank N-1 dataset.
 * This is currently used to implement DataSetOps.slice0().
 * 
 * Slicing a rank 1 dataset results in a rank 0 dataset.
 * @author jbf
 */
public class Slice0DataSet extends AbstractDataSet implements RankZeroDataSet {

    QDataSet ds;
    int index;

    Slice0DataSet(QDataSet ds, int index) {
        if ( ds.rank() > 3 ) {
            throw new IllegalArgumentException("rank limit > 2");
        }
        this.ds = ds;
        this.index = index;
        if ( DataSetUtil.isQube(ds) ) {
            putProperty( QDataSet.DEPEND_0, ds.property( QDataSet.DEPEND_1 ) );
            putProperty( QDataSet.DEPEND_1, ds.property( QDataSet.DEPEND_2 ) );
            putProperty( QDataSet.DEPEND_2, null );
        } else {
            QDataSet dep0= (QDataSet) ds.property( QDataSet.DEPEND_0 );
            if ( dep0!=null && dep0.rank()>1 ) {
                putProperty( QDataSet.DEPEND_0, new Slice0DataSet(dep0, index));
            } else {
                putProperty( QDataSet.DEPEND_0, null );
            }
        }
        
        for ( int i=0; i<QDataSet.MAX_PLANE_COUNT; i++ ) {
            QDataSet plane0= (QDataSet) ds.property( "PLANE_"+i );
            if ( plane0!=null ) {
                putProperty( "PLANE_"+i, new Slice0DataSet( plane0, index ) );
            } else {
                break;
            }
        }
    }

    public int rank() {
        return ds.rank() - 1;
    }

    public double value(int i) {
        return ds.value(index,i);
    }

    public double value(int i0, int i1) {
        return ds.value(index, i0, i1);
    }

    public Object property(String name) {
        if (properties.containsKey(name)) {
            return properties.get(name);
        } else {
            return ds.property(name,index);
        }
    }

    public Object property( String name, int i ) {
        if (properties.containsKey(name)) {
            return properties.get(name);
        } else {
            return ds.property(name,index,i);
        }
    }
    
    public int length() {
        return ds.length(index);
    }
    
    public int length( int i0 ) {
        return ds.length(index,i0);        
    }

    @Override
    public int length(int i, int j) {
        throw new IllegalArgumentException("rank limit");
    }
    
    
    
    @Override
    public boolean equals(Object obj) {
        if ( obj==null ) return false;
        if ( obj instanceof Slice0DataSet ) {
            Slice0DataSet that= ((Slice0DataSet)obj);
            return that.ds.equals(this.ds) && that.index==this.index;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return ds.hashCode() + index;
    }

    public double value() {
        if ( rank()!=0 ) throw new IllegalArgumentException("rank error");
        return ds.value(index);
    }
}
