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
import java.util.HashMap;
import java.util.Map;

/**
 * create a higher rank dataset with dim 0 being a join dimension.
 * @author jbf
 */
public class JoinDataSet extends AbstractDataSet {
    
    ArrayList<QDataSet> datasets;
    /**
     * rank of the dataset.  Joined DataSets should have rank rank-1.
     */
    int rank;
    
    /** Creates a new instance of JoinDataSet */
    public JoinDataSet( int rank ) {
        this.rank= rank;
        datasets= new ArrayList<QDataSet>();
    }
    
    public void join( QDataSet ds ) {
        if ( ds.rank()!=this.rank-1 ) throw new IllegalArgumentException("dataset rank must be "+(this.rank-1));
        datasets.add( ds );
    }

    public int rank() {
        return rank;
    }

    public double value(int i0, int i1) {
        return datasets.get(i0).value(i1);
    }

    public double value(int i0, int i1, int i2) {
        return datasets.get(i0).value(i1,i2);
    }

    public Object property(String name, int i0) {
        return datasets.get(i0).property(name);
    }

    public Object property(String name, int i0, int i1) {
        return datasets.get(i0).property(name,i1);
    }

    public int length() {
        return datasets.size();
    }

    public int length(int i0) {
        return datasets.get(i0).length();
    }

    public int length(int i0, int i1) {
        return datasets.get(i0).length(i1);
    }
    
    public String toString() {
        if ( datasets.size()>4 ) {
            return "JoinDataSet["+datasets.size()+" datasets: "+ datasets.get(0)+", "+datasets.get(1)+", ...]";
        } else {
            return "JoinDataSet["+datasets.size()+" datasets: "+ datasets +" ]";
        }
    }
    
}
