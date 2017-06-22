
package org.das2.qds;

/**
 * Base class for ad-hoc rank 1 datasets.
 * @author faden@cottagesystems.com
 */
public class AbstractRank1DataSet extends AbstractDataSet {

    private final int length;
    
    public AbstractRank1DataSet( int length ) {
        this.length= length;
    }
    
    @Override
    public int rank() {
        return 1;
    }

    @Override
    public int length() {
        return length;
    }
}
