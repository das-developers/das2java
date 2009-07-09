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
        if ( ds.rank() > 4 ) {
            throw new IllegalArgumentException("rank limit > 2");
        }
        this.ds = ds;
        this.index = index;
        if ( DataSetUtil.isQube(ds) ) {
            putProperty( QDataSet.DEPEND_0, ds.property( QDataSet.DEPEND_1 ) );
            putProperty( QDataSet.DEPEND_1, ds.property( QDataSet.DEPEND_2 ) );
            putProperty( QDataSet.DEPEND_2, ds.property( QDataSet.DEPEND_3 ) );
        } else {
            QDataSet dep0= (QDataSet) ds.property( QDataSet.DEPEND_0 );
            if ( dep0!=null && dep0.rank()>1 ) {
                putProperty( QDataSet.DEPEND_0, new Slice0DataSet(dep0, index));  //DEPEND_0 rank>1
            } else {
                if ( ds.property(QDataSet.DEPEND_0,index)==null ) { // bundle dataset
                    putProperty( QDataSet.DEPEND_0, null );
                }
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

    public double value() {
        return ds.value(index);
    }

    public double value(int i) {
        return ds.value(index,i);
    }

    public double value(int i0, int i1) {
        return ds.value(index, i0, i1);
    }

    public double value(int i0, int i1, int i2) {
        return ds.value(index, i0, i1, i2 );
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
    
    public Object property( String name, int i0, int i1 ) {
        if (properties.containsKey(name)) {
            return properties.get(name);
        } else {
            return ds.property(name,index,i0,i1);
        }
    }

    public Object property( String name, int i0, int i1, int i2 ) {
        if (properties.containsKey(name)) {
            return properties.get(name);
        } else {
            return ds.property(name,index,i0,i1,i2);
        }
    }

    public int length() {
        return ds.length(index);
    }
    
    public int length( int i0 ) {
        return ds.length(index,i0);        
    }

    public int length( int i0, int i1 ) {
        return ds.length(index,i0,i1);
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
