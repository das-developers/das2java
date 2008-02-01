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

/**
 *
 * @author jbf
 */
public class JoinDataSet implements QDataSet {
    
    ArrayList datasets;
    int[] offsets= new int[20];
    int[] lengths= new int[20];
    int count=0;
    int rank;
    
    /** Creates a new instance of JoinDataSet */
    public JoinDataSet() {
        count=0;
    }
    
    public void join( QDataSet ds ) {
        datasets.add( ds );
        if ( count==0 ) {
            offsets[count]= 0;   
            rank= ds.rank();
        } else {
            offsets[count]= offsets[count-1] + lengths[count-1];
        }
        lengths[count]= ds.length();
        count++;
    }

    public int rank() {
        return rank;
    }

    public double value(int i) {
        throw new RuntimeException("not implemented");
    }

    public double value(int i0, int i1) {
        throw new RuntimeException("not implemented");
    }

    public double value(int i0, int i1, int i2) {
        throw new RuntimeException("not implemented");
    }

    public Object property(String name) {
        throw new RuntimeException("not implemented");
    }

    public Object property(String name, int i) {
        throw new RuntimeException("not implemented");
    }

    public Object property(String name, int i0, int i1) {
        throw new RuntimeException("not implemented");
    }

    public int length() {
        throw new RuntimeException("not implemented");
    }

    public int length(int i) {
        throw new RuntimeException("not implemented");
    }

    public int length(int i, int j) {
        throw new RuntimeException("not implemented");
    }
    
}
