/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dataset;

/**
 * Wraps a rank N dataset, slicing on an index of the first dimension to make a rank N-1 dataset.
 * This is currently used to implement DataSetOps.slice0().
 * @author jbf
 */
public class Slice0DataSet extends AbstractDataSet {

    QDataSet ds;
    int index;

    Slice0DataSet(QDataSet ds, int index) {
        if ( ds.rank() > 3 ) {
            throw new IllegalArgumentException("rank limit > 2");
        }
        this.ds = ds;
        this.index = index;
        putProperty( QDataSet.DEPEND_0, ds.property( QDataSet.DEPEND_1 ) );
        putProperty( QDataSet.DEPEND_1, ds.property( QDataSet.DEPEND_2 ) );
        putProperty( QDataSet.DEPEND_2, null );
        QDataSet plane0= (QDataSet) ds.property( QDataSet.PLANE_0 );
        if ( plane0!=null ) {
            putProperty( QDataSet.PLANE_0, new Slice0DataSet( plane0, index ) );
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
}
