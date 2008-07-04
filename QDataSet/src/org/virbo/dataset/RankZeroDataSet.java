/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 * 2008-02-14
 */

package org.virbo.dataset;

/**
 * interface provide access to a rank 0 dataset, which can be thought of as
 * a scalar (and set of correlated scalars) with metadata.
 * 
 * @author jbf
 */
public interface RankZeroDataSet {
    /**
     * rank 0 accessor.
     * @return the scalar value stored in this dataset.
     * @throws IllegalArgumentException if the dataset is not rank 0.
     */
    double value();
    Object property( String name );
}
