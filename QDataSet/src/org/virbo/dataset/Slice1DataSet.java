/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dataset;

import org.das2.datum.EnumerationUnits;
import org.das2.datum.Units;

/**
 * return a rank N-1 dataset from a rank N dataset by slicing on the first
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

        QDataSet bundle1= (QDataSet)ds.property(QDataSet.BUNDLE_1);
        if ( bundle1!=null ) {
            System.err.println("we're not going to do this correctly, use unbundle instead");
        }
        String label=null;
        Units dep1units= dep1==null ? null : (Units) dep1.property(QDataSet.UNITS);//TODO stupid hurry, need to drive Sarah to work
        if ( dep1!=null && dep1.rank()==1 && ( dep1units instanceof EnumerationUnits ) ) { // check for legacy bundle where DEPEND_1 is labels.
            label= String.valueOf(dep1units.createDatum(dep1.value(index)));
        }


        if ( dep1!=null ) {
            if ( dep1.rank()==1 ) {
                if ( label!=null ) {
                    putProperty( QDataSet.LABEL, label ); // special code is like unbundle operator
                    putProperty( QDataSet.NAME, org.virbo.dsops.Ops.safeName(label) );
                } else {
                    DataSetUtil.addContext( this, new Slice0DataSet(dep1,index) );
                }
            } else if ( dep1.rank()==2 ) {
                DataSetUtil.addContext( this, new Slice1DataSet(dep1,index) );
            } else {
                System.err.println( "slice on non-qube, dep1 has rank="+dep1.rank() );
            }
        } else {
            DRank0DataSet context= DataSetUtil.asDataSet(index);
            context.putProperty( QDataSet.NAME, "slice1" );
            DataSetUtil.addContext( this, context );
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

        putProperty( QDataSet.WEIGHTS_PLANE, null );

        DataSetUtil.copyDimensionProperties( ds, this );
                
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

    /**
     * TODO: this is danger code, because we grab the property from the original dataset.  The bug I ran into is it was getting WEIGHTS
     * from the parent, which had the wrong dimensionality.
     * @param name
     * @return
     */
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
