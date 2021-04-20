/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qds;

/**
 * DataSet that is the lengths of another dataset.  The total of all the leaves
 * is the total number of elements in the dataset.  The rank is one less than
 * that of the wrapped dataset, and the values are the lengths of the last dimension
 * of the wrapped dataset.
 *
 * It's expected that this will be generally useful and a handy way to make
 * a QUBE dataset implementation such as DDataSet support non-qube datasets.
 *
 * @author jbf
 */
public class LengthsDataSet extends AbstractDataSet {

    private QDataSet ds;

    public LengthsDataSet( QDataSet ds ) {
        this.ds= ds;
    }

    @Override
    public int rank() {
        return ds.rank()-1;
    }

    @Override
    public int length() {
        return ds.length();
    }

    @Override
    public int length(int i) {
        return ds.length(i);
    }

    @Override
    public int length(int i, int j) {
        return ds.length(i,j);
    }

    @Override
    public int length(int i, int j, int k ) {
        return ds.length(i,j,k);
    }

    @Override
    public double value() {
        return ds.length();
    }

    @Override
    public double value(int i) {
        return ds.length(i);
    }

    @Override
    public double value(int i0, int i1) {
        return ds.length(i0, i1);
    }

    @Override
    public double value(int i0, int i1, int i2) {
        return ds.length(i0, i1, i2 );
    }
}
