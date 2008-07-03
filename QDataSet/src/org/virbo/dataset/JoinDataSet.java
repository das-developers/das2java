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
public class JoinDataSet implements QDataSet, MutablePropertyDataSet {
    
    ArrayList<QDataSet> datasets;
    int[] offsets= new int[20];
    int[] lengths= new int[20];
    int rank;
    Map<String,Object> properties;
    
    /** Creates a new instance of JoinDataSet */
    public JoinDataSet( int dataSetsRank ) {
        this.rank= dataSetsRank;
        datasets= new ArrayList<QDataSet>();
        properties= new HashMap<String,Object>();
    }
    
    public void join( QDataSet ds ) {
        if ( ds.rank()!=this.rank ) throw new IllegalArgumentException("dataset rank must be "+this.rank);
        datasets.add( ds );
    }

    public int rank() {
        return rank+1;
    }

    public double value(int i) {
        throw new IllegalArgumentException("rank error");
    }

    public double value(int i0, int i1) {
        return datasets.get(i0).value(i1);
    }

    public double value(int i0, int i1, int i2) {
        return datasets.get(i0).value(i1,i2);
    }

    public Object property(String name) {
        return properties.get(name);
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

    public void putProperty(String name, Object value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void putProperty(String name, int index, Object value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void putProperty(String name, int index1, int index2, Object value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
