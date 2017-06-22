/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 * 2008-02-14
 */

package org.das2.qds;

/**
 * interface provide access to a rank 0 dataset, which can be thought of as
 * a scalar (and set of correlated scalars) with metadata.
 *
 * This is a marker interface, since it no longer adds any new methods to the
 * interface.  TODO: this really needs to be studied, see 
 * org.virbo.qstream.SimpleStreamFormatter.timeDigits(SimpleStreamFormatter.java:188)
 * which would assume the data was a rank0 dataset, which it once was because
 * it came from slice0. 
 * 
 * @author jbf
 */
public interface RankZeroDataSet extends QDataSet {
    /**
     * rank 0 accessor.
     * @return the scalar value stored in this dataset.
     * @throws IllegalArgumentException if the dataset is not rank 0.
     */
    double value();
    Object property( String name );
}
