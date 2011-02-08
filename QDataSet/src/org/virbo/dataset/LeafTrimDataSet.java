/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dataset;

/**
 * pull out a subset of the dataset by reducing the number of columns in the
 * last dimension.  This does not reduce rank.  This assumes the dataset has no
 * row with length>end.
 *
 * TODO: This probably doesn't handle bundles property.
 * TODO: slice and trim should probably be implemented here for efficiently.
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

        String depNName = "DEPEND_" + (ds.rank() - 1);
        QDataSet depN = (QDataSet) ds.property(depNName);
        if (depN != null) {
            depN = new LeafTrimDataSet(depN, start, end);
        }
        properties.put(depNName, depN);

    }
    
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
            return ds.property(name);
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
    public Object property(String name, int i0, int i1) {
        if (DataSetUtil.isInheritedProperty(name) && properties.containsKey(name)) {
            return properties.get(name);
        } else {
            return ds.rank() == 2 ? ds.property(name, i0, i1 - start) : ds.property(name, i0, i1);
        }
    }

    @Override
    public Object property(String name, int i0, int i1, int i2) {
        if (DataSetUtil.isInheritedProperty(name) && properties.containsKey(name)) {
            return properties.get(name);
        } else {
            return ds.rank() == 3 ? ds.property(name, i0, i1, i2 - start) : ds.property(name, i0, i1, i2);
        }
    }

    @Override
    public Object property(String name, int i0, int i1, int i2, int i3) {
        if (DataSetUtil.isInheritedProperty(name) && properties.containsKey(name)) {
            return properties.get(name);
        } else {
            return ds.rank() == 4 ? ds.property(name, i0, i1, i2, i3 - start) : ds.property(name, i0, i1, i2, i3);
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

    @Override
    public QDataSet trim(int start, int end) {
        return new LeafTrimDataSet( this.ds, this.start+start, this.start+end );
    }


}
