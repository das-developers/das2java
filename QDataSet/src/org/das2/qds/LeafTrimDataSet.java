/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.qds;

import java.util.Map;

/**
 * pull out a subset of the dataset by reducing the number of columns in the
 * last dimension.  This does not reduce rank.  This assumes the dataset has no
 * row with length&gt;end.
 *
 * @author jbf
 */
public class LeafTrimDataSet extends AbstractDataSet {

    final QDataSet ds;
    final int start;
    final int end;

    /**
     * @param source the rank 1 or greater dataset to back this dataset.
     * @param start first index to include.
     * @param end last index, exclusive
     */
    public LeafTrimDataSet( QDataSet source, int start, int end ) {
        if ( source.rank()==0 ) {
            throw new IllegalArgumentException( "source is rank 0");
        }
        this.ds= source;
        this.start= start;
        this.end= end;

        int len= 0;
        if ( ds.rank()==1 ) {
            len= ds.length();
        } else if ( ds.rank()==2 ) {
            len= ds.length(0);
        } else if ( ds.rank()==3 ) {
            len= ds.length(0,0);
        } else if ( ds.rank()==4 ) {
            len= ds.length(0,0,0);
        } else {
            throw new IllegalArgumentException("rank exception"); // TODO: consider return the dataset for rank 0.
        }
        if ( end>len ) {
            throw new IndexOutOfBoundsException("end is greater than last dimension length");
        }
        if ( start<0 ) {
            throw new IndexOutOfBoundsException("start is less than 0");
        }
        if ( start>end ) {
            throw new IndexOutOfBoundsException("start is greater than end");
        }

        String depNName = "DEPEND_" + (ds.rank() - 1);
        QDataSet depN = (QDataSet) ds.property(depNName);
        if (depN != null) {
            if ( depN.rank()==2 ) {
                depN = new LeafTrimDataSet(depN, start, end);
                properties.put(depNName, depN);
            } else {
                depN= depN.trim(start, end);
                properties.put(depNName, depN);
            }
        }
        String bundleNName = "BUNDLE_" + (ds.rank() - 1);
        QDataSet bds = (QDataSet) ds.property(bundleNName);
        if ( bds != null) {
            bds = bds.trim( start, end );
            properties.put( bundleNName, bds );
        }
        for ( int i=0; i<ds.rank()-1; i++ ) {
            Object o= ds.property("DEPEND_"+i);
            if ( o!=null ) properties.put( "DEPEND_"+i, o );
        }
        for ( int i=0; i<ds.rank()-1; i++ ) {
            Object o= ds.property("BUNDLE_"+i);
            if ( o!=null ) properties.put( "BUNDLE_"+i, o );
        }
        for ( int i=0; i<ds.rank()-1; i++ ) {
            Object o= ds.property("BINS_"+i);
            if ( o!=null ) properties.put( "BINS_"+i, o );
        }
    }
    
    @Override
    public int rank() {
        return ds.rank();
    }

    @Override
    public double value(int i) {
        return ds.value(i + start);
    }

    @Override
    public double value(int i0, int i1) {
        return ds.value(i0, i1 + start);
    }

    @Override
    public double value(int i0, int i1, int i2) {
        return ds.value(i0, i1, i2 + start);
    }

    @Override
    public double value(int i0, int i1, int i2, int i3) {
        return ds.value(i0, i1, i2, i3 + start);
    }

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
    public Object property(String name, int i) {
        if (DataSetUtil.isInheritedProperty(name) && properties.containsKey(name)) {
            return properties.get(name);
        } else {
            return ds.rank() == 1 ? ds.property(name, i - start) : ds.property(name, i);
        }
    }

    @Override
    public int length() {
        return ds.rank() == 1 ? end - start : ds.length();
    }

    @Override
    public int length(int i) {
        return ds.rank() == 2 ? end - start : ds.length(i);
    }

    @Override
    public int length(int i, int j) {
        return ds.rank() == 3 ? end - start : ds.length(i, j);
    }

    @Override
    public int length(int i0, int i1, int i2) {
        return ds.rank() == 4 ? end - start : ds.length(i0, i1, i2);
    }

    /**
     * {@inheritDoc}
     * This must also copy the dimension properties explicitly.
     * @param i
     * @return 
     */
    @Override
    public QDataSet slice(int i) {
        if ( this.rank()==1 ) {
            return ds.slice( i + start );
        } else {
            LeafTrimDataSet result= new LeafTrimDataSet( ds.slice(i), this.start, this.end );
            DataSetUtil.copyDimensionProperties( this, result );
            Map<String,Object> ps= DataSetUtil.sliceProperties( ds, i, null );
            DataSetUtil.putProperties( ps, result );
            return result;
        }
    }

    @Override
    public QDataSet trim(int start, int end) {
        if ( this.rank()==1 ) {
            return ds.trim( this.start+start, this.start+end );
        } else {
            LeafTrimDataSet result= new  LeafTrimDataSet( ds.trim(start, end), this.start, this.end );
            for ( int i=1; i<this.rank(); i++ ) {
                String pname= "DEPEND_"+i;
                QDataSet dds= (QDataSet)this.property(pname);
                if ( dds!=null ) {
                    result.putProperty( pname, dds );
                }
                pname= "BUNDLE_"+i;
                dds= (QDataSet)this.property(pname);
                if ( dds!=null ) {
                    result.putProperty( pname, dds );
                }       
                pname= "BINS_"+i;
                String s= (String)this.property(pname);
                if ( s!=null ) {
                    result.putProperty( pname, s );
                }
            }
            DataSetUtil.copyDimensionProperties( this, result );
            return result;
        }
    }
}
