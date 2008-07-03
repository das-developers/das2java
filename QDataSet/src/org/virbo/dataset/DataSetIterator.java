/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.dataset;

import org.virbo.dataset.QDataSet;
import org.virbo.dataset.WritableDataSet;

/**
 *
 * @author jbf
 */
public interface DataSetIterator {

    /**
     * get the value from ds at the current iterator position.
     * @param ds a dataset with capatible geometry as the iterator's geometry.
     * @return the value of ds at the current iterator position.
     */
    double getValue(QDataSet ds);

    boolean hasNext();

    int index(int dim);

    int length(int dim);

    void next();

    /**
     * replace the value in ds at the current iterator position.
     * @param ds a writable dataset with capatible geometry as the iterator's geometry.
     * @param v the value to insert.
     */
    void putValue(WritableDataSet ds, double v);

    /**
     * return the rank of the dataset which the iterator will walk through.
     * Note this needn't be the same rank as the dataset!
     * @return
     */
    int rank();

}
