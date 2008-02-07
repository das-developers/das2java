/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dataset;

/**
 *
 * @author jbf
 */
public class Slice1DataSet extends AbstractDataSet {

    QDataSet ds;
    int index;

    Slice1DataSet(QDataSet ds, int index) {
        if (ds.rank() > 2) {
            throw new IllegalArgumentException("rank limit > 2");
        }
        this.ds = ds;
        this.index = index;
        putProperty( QDataSet.QUBE, null );
        putProperty( QDataSet.DEPEND_1, null );
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

    public Object property(String name) {
        if (properties.containsKey(name)) {
            return properties.get(name);
        } else {
            return ds.property(name);
        }
    }

    public int length() {
        return ds.length();
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
