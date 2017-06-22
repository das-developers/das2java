package org.das2.qds;

/**
 * Iterator for accessing each value of a dataset.
 * See http://jfaden.net:8080/hudson/job/autoplot-test037/ws/dataSetIterator.jy
 * @author jbf
 */
public interface DataSetIterator {

    /**
     * return true while the iterator has a next element.
     * @return true while the iterator has a next element.
     */
    boolean hasNext();

    /**
     * return the current index for the dimension.
     * @param dim the dimension number (0&lt;=dim&lt;inputRank) 
     * @return the current index.
     */
    int index(int dim);

    /**
     * return the length of the dimension, or the length reported by the 
     * iterator.  Use caution, because this does not imply that the result 
     * of the iteration is a qube and does not account for slices.  (TODO:
     * does this mean that as we iterate through, the length depends on the
     * current index?)
     * @param dim the dimension number (0&lt;=dim&lt;inputRank) 
     * @return the length of the dimension
     */
    int length(int dim);

    /**
     * iterate to the next position.
     */
    void next();

    /**
     * get the value from ds at the current iterator position.
     * @param ds a dataset with compatible geometry as the iterator's geometry.
     * @return the value of ds at the current iterator position.
     */
    double getValue(QDataSet ds);

    /**
     * replace the value in ds at the current iterator position.
     * @param ds a writable dataset with compatible geometry as the iterator's geometry.
     * @param v the value to insert.
     */
    void putValue(WritableDataSet ds, double v);

    /**
     * return the rank of the dataset which the iterator will walk through.
     * Note this needn't be the same rank as the dataset!  For example,
     * when QubeDataSetIterator walks through ds[:,0,:], the rank is 2 even 
     * though ds is rank 3.
     * 
     * @return the rank of the dataset which the iterator will walk through.
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
     * @return a dataset that will have the same geometry at the
     * dataset implied by each dimension iterator. 
     * @see QubeDataSetIterator#createEmptyDs() 
     */
    public DDataSet createEmptyDs();

}
