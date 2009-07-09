/*
 * BundleDataSet.java
 *
 * Copied from JoinDataSet in June 2009
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.virbo.dataset;

import java.util.ArrayList;
import java.util.List;

/**
 * create a higher rank dataset with dim 1 being a bundle dimension.  Each
 * dataset must have the same length.
 *
 * Note this was created before BUNDLE_1 and bundle descriptor datasets were
 * introduced, so this code is suspect.  TODO: review and ensure compatibility
 * with updates to bundle dataset semantics.
 *
 * @author jbf
 */
public class BundleDataSet extends AbstractDataSet {
    
    List<QDataSet> datasets;
    /**
     * rank of the dataset.
     */
    int rank;

    /**
     * the length of each dataset.
     */
    int len0;
    
    /** Creates a new instance of BundleDataSet
     * 
     */
    public BundleDataSet( ) {
        this.rank= 2;
        datasets= new ArrayList<QDataSet>();
    }

    /**
     * add the dataset to the bundle of datasets.  Currently this implementation only supports rank 1 datasets, but
     * the QDataSet spec allows for qube datasets of any rank>0 to be bundled.  This limitation will be removed
     * in a future version.
     * 
     * @param ds
     */
    public void bundle( QDataSet ds ) {
        if ( ds.rank()!=this.rank-1 ) throw new IllegalArgumentException("only rank 1 supported for now.");
        if ( datasets.size()>0 ) {
            len0= ds.length();
        } else {
            if ( ds.length()!=len0 ) throw new IllegalArgumentException("dataset length is not consistent with the bundle.");
        }
        datasets.add( ds );
    }

    public int rank() {
        return rank;
    }

    public double value(int i0, int i1) {
        return datasets.get(i1).value(i0);
    }

    public Object property(String name, int i0, int i1) {
        return datasets.get(i1).property(name,i0);
    }

    public int length() {
        return len0;
    }

    public int length(int i0) {
        return datasets.size();
    }
    
    public String toString() {
        if ( datasets.size()>4 ) {
            return "BundleDataSet["+datasets.size()+" datasets: "+ datasets.get(0)+", "+datasets.get(1)+", ...]";
        } else {
            return "BundleDataSet["+datasets.size()+" datasets: "+ datasets +" ]";
        }
    }

}
