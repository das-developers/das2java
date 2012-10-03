/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.dataset;

/**
 * Join of WritableDataSets where each dataset is writable.  Note type checking is
 * only done when the constructor that accepts a dataset, otherwise type checking
 * is not done until the data is accessed.  This was introduced to properly
 * support Ops.copy.
 * @author jbf
 */
public class WritableJoinDataSet extends JoinDataSet implements WritableDataSet {

    public WritableJoinDataSet( int rank ) {
        super(rank);
    }

    public WritableJoinDataSet( QDataSet ds ) {
        super(ds);
        if ( !( ds instanceof WritableDataSet ) ) {
            throw new IllegalArgumentException("dataset must be writable: "+ds );
        }
    }

    public void putValue(double d) {
        throw new IllegalArgumentException("rank error, expected "+rank());
    }

    public void putValue(int i0, double d) {
        ((WritableDataSet)datasets.get(i0)).putValue(d);
    }

    public void putValue(int i0, int i1, double d) {
        ((WritableDataSet)datasets.get(i0)).putValue(i1,d);
    }

    public void putValue(int i0, int i1, int i2, double d) {
        ((WritableDataSet)datasets.get(i0)).putValue(i1,i2,d);
    }

    public void putValue(int i0, int i1, int i2, int i3, double d) {
        ((WritableDataSet)datasets.get(i0)).putValue(i1,i2,i3,d);
    }

}
