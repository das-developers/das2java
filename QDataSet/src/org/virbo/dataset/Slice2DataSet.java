/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dataset;

/**
 * return a rank N-1 dataset from a rank N dataset by slicing on the first
 * dimension.  (Rank 3 and 4 supported.)
 * 
 * plane datasets are sliced as well, when they have rank 3 or greater.
 * 
 * @author jbf
 */
public class Slice2DataSet extends AbstractDataSet {

    QDataSet ds;
    int index;

    Slice2DataSet( QDataSet ds, int index ) {
        if (ds.rank() > 4 ) {
            throw new IllegalArgumentException("rank limit > 4");
        }
        if ( ds.rank()<3 ) {
            throw new IllegalArgumentException("rank limit < 3");
        }
        this.ds = ds;
        this.index = index;

        QDataSet dep2= (QDataSet) ds.property(QDataSet.DEPEND_2);
        if ( dep2.rank()>1 ) {
            System.err.println( "slice on non-qube, dep2 has rank="+dep2.rank() );
        }

        if ( dep2!=null && dep2.rank()==1 ) {
            DataSetUtil.addContext( this, new Slice0DataSet(dep2,index) );
        }
        putProperty( QDataSet.DEPEND_2, ds.property(QDataSet.DEPEND_3) );

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
            return ds.property(name);
        }
    }

    //TODO: consider higher-rank properties

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
