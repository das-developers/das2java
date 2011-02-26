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
 *
 * Supports rank 2 depend_1 datasets.  Supports CONTEXT_0, DELTA_PLUS, DELTA_MINUS
 *
 * Supports BINS_1, JOIN_0
 *
 * @author jbf
 */
public class Slice0DataSet extends AbstractDataSet implements RankZeroDataSet {

    QDataSet ds;
    int index;

    public Slice0DataSet(QDataSet ds, int index) {
        this( ds, index, true );
    }

    public Slice0DataSet( QDataSet ds, int index, boolean addContext ) {
        if ( ds.rank() > 4 ) {
            throw new IllegalArgumentException("rank limit > 4");
        }
        this.ds = ds;
        this.index = index;
        QDataSet dep0= (QDataSet) ds.property( QDataSet.DEPEND_0 );
        QDataSet dep1= (QDataSet) ds.property( QDataSet.DEPEND_1 );
        if ( dep0!=null && dep1!=null && dep0.rank()>1 && dep1.rank()>1 ) {
            // special case where we are pulling out a table, this used to be a runtime exception
            putProperty( QDataSet.DEPEND_0, new Slice0DataSet(dep0, index, false ));
            putProperty( QDataSet.DEPEND_1, new Slice0DataSet(dep1, index, false ));
        } else if ( DataSetUtil.isQube(ds) || ds.property(QDataSet.DEPEND_1)!=null ) { //DEPEND_1 rank 1 implies qube
            if ( addContext ) {
                if ( dep0!=null ) {
                    DataSetUtil.addContext( this, new Slice0DataSet( dep0, index, false ) );
                } else {
                    DRank0DataSet context= DataSetUtil.asDataSet(index);
                    context.putProperty( QDataSet.NAME, "slice0" );
                    DataSetUtil.addContext( this, context );
                }
            }
            if ( dep1!=null && dep1.rank()==2 ) {
                putProperty( QDataSet.DEPEND_0, new Slice0DataSet( dep1, index, false ) );
            } else if ( dep1!=null ) {
                putProperty( QDataSet.DEPEND_0, dep1 );
            }
            putProperty( QDataSet.BINS_0, ds.property( QDataSet.BINS_1 ) );
            putProperty( QDataSet.DEPEND_1, ds.property( QDataSet.DEPEND_2 ) );
            putProperty( QDataSet.DEPEND_2, ds.property( QDataSet.DEPEND_3 ) );

        } else {
            if ( dep0!=null && dep0.rank()>1 ) {
                putProperty( QDataSet.DEPEND_0, new Slice0DataSet(dep0, index, false ));  //DEPEND_0 rank>1
            } else if ( dep0!=null ) {
                if ( addContext ) DataSetUtil.addContext( this, new Slice0DataSet( dep0, index, false ) );
            } else {
                if ( ds.property(QDataSet.DEPEND_0,index)==null ) { // bundle dataset  //TODO: this needs more review
                    putProperty( QDataSet.DEPEND_0, null );
                } else {
                    if ( addContext )  {
                        DRank0DataSet context= DataSetUtil.asDataSet(index);
                        context.putProperty( QDataSet.NAME, "slice0" );
                        DataSetUtil.addContext( this, context );
                    }
                }
            }
        }
        putProperty( QDataSet.JOIN_0, null );
        
        for ( int i=0; i<QDataSet.MAX_PLANE_COUNT; i++ ) {
            String prop= "PLANE_"+i;
            QDataSet plane0= (QDataSet) ds.property( prop );
            if ( plane0!=null ) {
                if ( plane0.rank()<1 ) {
                    putProperty( prop, plane0 );
                } else {
                    putProperty( prop, new Slice0DataSet( plane0, index, addContext ) );
                }
            } else {
                break;
            }
        }

        String[] p= new String[] { QDataSet.DELTA_MINUS, QDataSet.DELTA_PLUS };
        for ( int i=0; i<p.length; i++ ) {
            QDataSet delta= (QDataSet) ds.property( p[i] );
            if ( delta!=null && delta.rank()>0 ) {
                putProperty( p[i], new Slice0DataSet(delta,index,addContext) );
            }
        }

        putProperty( QDataSet.WEIGHTS_PLANE, null );

        DataSetUtil.copyDimensionProperties( ds, this );
        
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
            //warning: dataset does not define slice, so property is inherited
            return ds.property(name,index);
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
