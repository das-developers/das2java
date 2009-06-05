/*
 * JoinDataSet.java
 *
 * Created on April 27, 2007, 10:52 AM
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
 * @author jbf
 */
public class BundleDataSet extends AbstractDataSet {
    
    List<QDataSet> datasets;
    /**
     * rank of the dataset.  Joined DataSets should have rank rank-1.
     */
    int rank;

    /**
     * the length of each dataset.
     */
    int len0;
    
    /** Creates a new instance of JoinDataSet 
     * @param rank The rank of the JoinDataSet.  Each dataset joined must have rank <tt>rank</tt>-1.
     * 
     */
    public BundleDataSet( ) {
        this.rank= 2;
        datasets= new ArrayList<QDataSet>();
    }
    
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
