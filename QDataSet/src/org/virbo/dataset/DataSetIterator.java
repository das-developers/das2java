/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.dataset;

/**
 *
 * @author jbf
 */
public interface DataSetIterator {

    boolean hasNext();

    int index(int dim);

    int length(int dim);

    void next();

    /**
     * get the value from ds at the current iterator position.
     * @param ds a dataset with capatible geometry as the iterator's geometry.
     * @return the value of ds at the current iterator position.
     */
    double getValue(QDataSet ds);

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

    /**
     * return a dataset that will have the same geometry at the
     * dataset implied by each dimension iterator.  This is
     * introduced to encapsulate this dangerous code to here where it could
     * be done correctly.  Right now this assumes QUBES.
     *
     * Do not pass the result of this into the putValue of this iterator,
     * the result should have its own iterator.
     * 
     * @return
     */
    public DDataSet createEmptyDs();

}
