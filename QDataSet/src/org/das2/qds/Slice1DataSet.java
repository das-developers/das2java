/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.qds;

import java.util.Map;
import org.das2.datum.EnumerationUnits;
import org.das2.datum.Units;

/**
 * return a rank N-1 dataset from a rank N dataset by slicing on the second
 * dimension.  (Rank 2, 3, and 4 supported.)
 * 
 * plane datasets are sliced as well, when they have rank 2 or greater.
 * bundle_1 handled.
 *
 * Note when DEPEND_1 has EnumerationUnits (like when it comes from labels() method,
 * then this should be the same as the unbundle method.
 * 
 * @author jbf
 */
public final class Slice1DataSet extends AbstractDataSet {

    QDataSet ds;
    int index;

    public Slice1DataSet(QDataSet ds, int index) {
        this( ds, index, true, false );
    }

    public Slice1DataSet(QDataSet ds, int index, boolean unbundle ) {
        this( ds, index, false, unbundle );
    }

    Slice1DataSet( QDataSet ds, int index, boolean addContext, boolean unbundle ) {
        if (ds.rank() > 4 ) {
            throw new IllegalArgumentException("rank limit > 4");
        }
        if ( ds.rank()<2 ) {
            throw new IllegalArgumentException("cannot create a Slice1DataSet from rank "+ds.rank() + " dataset");
        }
        if ( index>= ds.length(0) ) throw new IndexOutOfBoundsException("index is out of bounds");

        this.ds = ds;
        this.index = index;

        QDataSet dep1= (QDataSet) ds.property(QDataSet.DEPEND_1);

        QDataSet bds= (QDataSet)ds.property(QDataSet.BUNDLE_1);
//        if ( bundle1!=null && !unbundle ) {
//            System.err.println("we're not going to do this correctly, use unbundle instead");
//        }
        String label=null;
        Units dep1units= dep1==null ? null : (Units) dep1.property(QDataSet.UNITS);//TODO stupid hurry, need to drive Sarah to work
        if ( dep1!=null && dep1.rank()==1 && ( dep1units instanceof EnumerationUnits ) ) { // check for legacy bundle where DEPEND_1 is labels.
            label= String.valueOf(dep1units.createDatum(dep1.value(index)));
            addContext= false;
        }


        // add the CONTEXT_0 property.
        if ( bds!=null && dep1==null ) {
            QDataSet context=null;
            if ( addContext ) {
                context= DataSetOps.getContextForUnbundle( bds, index );
            }
            if ( label!=null ) {
                if ( addContext ) {
                    DataSetUtil.addContext( this, context );
                }
                putProperty( QDataSet.LABEL, label );
                putProperty( QDataSet.NAME, org.das2.qds.ops.Ops.safeName(label) );
                //putProperty( QDataSet.NAME, bds.property(QDataSet.NAME,index) ); //TODO: slice2 and slice3 are different.
            } else {
                if ( addContext ) {
                    DataSetUtil.addContext( this, context );
                }
            }
            if ( ds.rank()==2 ) { // it should
                Map<String,Object> o= DataSetUtil.sliceProperties( bds, index, null );
                DataSetUtil.putProperties( o, this ); 
            }
        } else if ( dep1!=null ) {
            switch (dep1.rank()) {
                case 1:
                    if ( label!=null ) {
                        putProperty( QDataSet.LABEL, label ); // special code is like unbundle operator
                        putProperty( QDataSet.NAME, org.das2.qds.ops.Ops.safeName(label) );
                        if ( addContext ) DataSetUtil.addContext( this, new Slice0DataSet(dep1,index,false) );
                    } else {
                        if ( addContext ) DataSetUtil.addContext( this, new Slice0DataSet(dep1,index,false) );
                    }   break;
                case 2:
                    if ( dep1.property(QDataSet.BINS_1)==null ) {
                        if ( addContext ) DataSetUtil.addContext( this, new Slice1DataSet(dep1,index,false) );
                        break;
                    }
                    if ( dep1.property(QDataSet.BINS_1).equals(QDataSet.VALUE_BINS_MIN_MAX) ) {
                        if ( addContext ) DataSetUtil.addContext( this, new Slice0DataSet(dep1,index,false) );
                        break;
                    }
                default: 
                    System.err.println( "slice on non-qube, dep1 has rank="+dep1.rank() );
                    break;
            }
        } else {
            DRank0DataSet context= DataSetUtil.asDataSet(index);
            context.putProperty( QDataSet.NAME, "slice1" );
            if ( addContext ) DataSetUtil.addContext( this, context );
            if ( ds.rank()==2 && ds.length()<3 ) {
                Units u1= (Units)ds.slice(0).slice(index).property( QDataSet.UNITS );
                Units u= u1;
                for ( int i=1; i<ds.length(); i++ ) {
                    Units u2= (Units)ds.slice(i).slice(index).property( QDataSet.UNITS );
                    if ( u1!=u2 ) u=null;
                }
                if ( u!=null ) putProperty( QDataSet.UNITS, u );
            }
        }


        putProperty( QDataSet.DEPEND_0, ds.property(QDataSet.DEPEND_0) );
        
        // rfe670: check for high-rank DEPEND_2.
        QDataSet dep2= (QDataSet) ds.property(QDataSet.DEPEND_2);
        if ( dep2!=null && dep2.rank()>2 ) dep2= new Slice1DataSet( dep2, index );
        putProperty( QDataSet.DEPEND_1, dep2 );
        
        QDataSet dep3= (QDataSet) ds.property(QDataSet.DEPEND_3);
        if ( dep3!=null && dep3.rank()>2 ) dep3= new Slice1DataSet( dep3, index );        
        putProperty( QDataSet.DEPEND_2, dep3 );

        putProperty( QDataSet.BUNDLE_0, ds.property(QDataSet.BUNDLE_0) );
        putProperty( QDataSet.BUNDLE_1, ds.property(QDataSet.BUNDLE_2) );
        putProperty( QDataSet.BUNDLE_2, ds.property("BUNDLE_3") );
        
        putProperty( QDataSet.BINS_0, ds.property( QDataSet.BINS_0 ) );
        putProperty( QDataSet.BINS_1, ds.property( "BINS_2") );
        putProperty( "BINS_2", ds.property( "BINS_3") );

        for ( int i=0; i<QDataSet.MAX_PLANE_COUNT; i++ ) {
            String prop= "PLANE_"+i;
            QDataSet plane0= (QDataSet) ds.property( prop );
            if ( plane0!=null ) {
                if ( plane0.rank()<2 ) {
                    putProperty( prop, plane0 );
                } else {
                    putProperty( prop, new Slice1DataSet( plane0, index, addContext ) );
                }
            } else {
                break;
            }
        }

        String[] props= DataSetUtil.correlativeProperties();
        for (String prop : props) {
            QDataSet s = (QDataSet) ds.property( prop );
            if (s != null) {
                if ( s.rank()<2 ) {
                    putProperty( prop, s );
                } else {
                    putProperty( prop, new Slice1DataSet( s, index, addContext ) );
                }
            }
        }
        
        putProperty( QDataSet.WEIGHTS, null );

        DataSetUtil.copyDimensionProperties( ds, this );
        DataSetUtil.maybeCopyRenderType( ds, this );
                
    }

    @Override
    public int rank() {
        return ds.rank() - 1;
    }

    @Override
    public double value(int i) {
        return ds.value(i, index);
    }

    @Override
    public double value(int i0, int i1) {
        return ds.value(i0, index, i1);
    }

    @Override
    public double value(int i0, int i1, int i2) {
        return ds.value(i0, index, i1, i2 );
    }

    /**
     * @param name
     * @return
     */
    @Override
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

    @Override
    public int length() {
        return ds.length();
    }

    @Override
    public int length(int i) {
        return ds.length( i, index );
    }
    
    @Override
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
