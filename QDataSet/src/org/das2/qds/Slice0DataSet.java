/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.qds;

import java.util.Map;
import java.util.logging.Logger;
import org.das2.util.LoggerManager;

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

    private static final Logger logger= LoggerManager.getLogger("qdataset");
    
    QDataSet ds;
    int index;

    public Slice0DataSet(QDataSet ds, int index) {
        this( ds, index, true );
    }

    public Slice0DataSet( QDataSet ds, int index, boolean addContext ) {
        if ( ds.rank() > 4 ) {
            throw new IllegalArgumentException("rank limit > 4");
        }
        if ( ds.rank()< 1 ) {
            throw new IllegalArgumentException("slice called on rank 0 dataset");
        }
        if ( ds.length()==0 ) {
            throw new IndexOutOfBoundsException("can't slice empty dataset "+ds);
        } else if ( index>=ds.length() ) {
            throw new IndexOutOfBoundsException("can't slice "+ds+" at index "+index);
        }
        this.ds = ds;
        this.index = index;
        QDataSet dep0= (QDataSet) ds.property( QDataSet.DEPEND_0 );
        QDataSet dep1= (QDataSet) ds.property( QDataSet.DEPEND_1 );
        if ( dep0!=null && dep1!=null && dep0.rank()>1 && dep1.rank()>1 && !SemanticOps.isBins(dep0) && !SemanticOps.isBins(dep1) ) {
            // special case where we are pulling out a table, this used to be a runtime exception
            // TODO: review this.  I can't imagine how this is valid.
            putProperty( QDataSet.DEPEND_0, new Slice0DataSet(dep0, index, false ));
            putProperty( QDataSet.DEPEND_1, new Slice0DataSet(dep1, index, false )); //TODO: really?  We need to think about this...
        } else if ( DataSetUtil.isQube(ds) || dep1!=null ) { //DEPEND_1 rank 1 implies qube
            if ( addContext ) {
                if ( dep0!=null ) {
                    DataSetUtil.addContext( this, new Slice0DataSet( dep0, index, false ) );
                } else {
                    DRank0DataSet context= DataSetUtil.asDataSet(index);
                    context.putProperty( QDataSet.NAME, "slice0" );
                    DataSetUtil.addContext( this, context );
                }
            }
            if ( dep1!=null && dep1.rank()==2 && !SemanticOps.isBins(dep1) ) {
                putProperty( QDataSet.DEPEND_0, new Slice0DataSet( dep1, index, false ) );
            } else if ( dep1!=null && dep1.rank()==2 && SemanticOps.isBins(dep1) ) {
                putProperty( QDataSet.DEPEND_0, dep1 );
            } else if ( ds.rank()>1 && dep1!=null ) {
                putProperty( QDataSet.DEPEND_0, dep1 );
            }
            putProperty( QDataSet.BUNDLE_0, ds.property(QDataSet.BUNDLE_1 ) );
            putProperty( QDataSet.BUNDLE_1, null );
            putProperty( QDataSet.BINS_0, ds.property( QDataSet.BINS_1 ) );
            
            if ( ds.rank()==1 ) {
                QDataSet bds= (QDataSet)ds.property( QDataSet.BUNDLE_0 );
                if ( bds!=null ) {
                    Map<String,Object> o= DataSetUtil.sliceProperties( bds, index, null );
                    DataSetUtil.putProperties( o, this ); 
                }
            } 
            if ( ds.rank()>2 ) {
                QDataSet dep =  (QDataSet) ds.property( QDataSet.DEPEND_2 );
                if ( dep!=null && dep.rank()>1 ) {
                    putProperty( QDataSet.DEPEND_1, new Slice0DataSet( dep, index, false ) ); // TODO: consider dep.slice
                } else {
                    putProperty( QDataSet.DEPEND_1, dep );
                }
            }
            if ( ds.rank()>3 ) {
                QDataSet dep =  (QDataSet) ds.property( QDataSet.DEPEND_3 );
                if ( dep!=null && dep.rank()==2 ) {
                    putProperty( QDataSet.DEPEND_2, new Slice0DataSet( dep, index, false ) );
                } else {
                    putProperty( QDataSet.DEPEND_2, dep );
                }
            }

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
                    putProperty( QDataSet.DEPEND_0, ds.property(QDataSet.DEPEND_0,index) ); //TODO: discovered this problem with autoplot-test013, where DEPEND_0 is a string.  Oops.  We'll try to get the same result for now.
                }
            }
            if ( ds.rank()>2 ) putProperty( QDataSet.DEPEND_1, ds.property(QDataSet.DEPEND_1,index)); //TODO: QDataSet.DEPENDNAME_1,etc
            putProperty( QDataSet.RENDER_TYPE, ds.property(QDataSet.RENDER_TYPE,index)); //kludge for autoplot test030_002, we use this to unbundle.
            putProperty( QDataSet.BINS_0, ds.property(QDataSet.BINS_0,index) );
            if ( ds.rank()>2 ) putProperty( QDataSet.BINS_1, ds.property(QDataSet.BINS_1,index) );
            putProperty( QDataSet.BUNDLE_0, ds.property(QDataSet.BUNDLE_0,index) );
            if ( ds.rank()>2 ) putProperty( QDataSet.BUNDLE_1, ds.property(QDataSet.BUNDLE_1,index) );
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

        String[] p= DataSetUtil.correlativeProperties();
        for ( int i=0; i<p.length; i++ ) {
            Object o=  ds.property( p[i] );
            if ( o!=null && !(o instanceof QDataSet ) ) {
                logger.warning("dropping property "+p[i]+" because it is not a QDataSet");
                continue;
            }
            QDataSet delta= (QDataSet) o;
            if ( delta!=null && delta.rank()>0 ) {
                putProperty( p[i], new Slice0DataSet(delta,index,addContext) );
            }
        }

        putProperty( QDataSet.WEIGHTS, null );

        DataSetUtil.copyDimensionProperties( ds, this );
        DataSetUtil.maybeCopyRenderType( ds, this );
    }

    public int rank() {
        return ds.rank() - 1;
    }

    @Override
    public double value() {
        return ds.value(index);
    }

    @Override
    public double value(int i) {
        return ds.value(index,i);
    }

    @Override
    public double value(int i0, int i1) {
        return ds.value(index, i0, i1);
    }

    @Override
    public double value(int i0, int i1, int i2) {
        return ds.value(index, i0, i1, i2 );
    }

    @Override
    public Object property(String name) {
        if (properties.containsKey(name)) {
            return properties.get(name);
        } else {
            if ( DataSetUtil.isInheritedProperty(name) ) {
                return ds.property(name,index);
            } else {
                return null;
            }
        }
    }

    @Override
    public Object property( String name, int i ) {
        if (properties.containsKey(name)) {
            return properties.get(name);
        } else {
            if ( DataSetUtil.isInheritedProperty(name) ) {
                //warning: dataset does not define slice, so property is inherited
                return ds.property(name,index);
            } else {
                return null;
            }
        }
    }

    @Override
    public int length() {
        return ds.length(index);
    }
    
    @Override
    public int length( int i0 ) {
        return ds.length(index,i0);        
    }

    @Override
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
