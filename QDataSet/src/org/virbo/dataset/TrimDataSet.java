/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dataset;

/**
 *
 * @author jbf
 */
public class TrimDataSet extends AbstractDataSet {

    final int offset;
    final int len;
    final QDataSet ds;

    public TrimDataSet(QDataSet ds, int start, int stop) {
        this.ds = ds;
        this.offset = start;
        this.len = stop - start;
    }

    public int rank() {
        return ds.rank();
    }

    @Override
    public Object property(String name, int i0, int i1) {
        return super.property(name, i0, i1);
    }

    @Override
    public double value(int i) {
        return ds.value(offset + i);
    }

    @Override
    public double value(int i0, int i1) {
        return ds.value(i0 + offset, i1);
    }

    @Override
    public double value(int i0, int i1, int i2) {
        return ds.value(i0 + offset, i1, i2);
    }

    public Object property(String name) {
        if (properties.containsKey(name)) {
            return properties.get(name);
        } else {
            return ds.property(name);
        }
    }

    public Object property(String name, int i) {
        Object p = properties.get(name);
        return (p != null) ? p : ds.property(name, i);
    }

    public int length() {
        return len;
    }

    public int length(int i0) {
        return ds.length(offset + i0);
    }

    public int length(int i0, int i1) {
        return ds.length(offset + i0, i1);
    }

    @Override
    public QDataSet slice(int i) {
        return new Slice0DataSet( ds, offset + i );
    }

    @Override
    public QDataSet trim(int start, int end) {
        if ( end>=len ) throw new IllegalArgumentException("end>len");
        if ( start<0 ) throw new IllegalArgumentException("start<0" );
        return new TrimDataSet( ds, start+offset, start+offset+end );
    }


}
