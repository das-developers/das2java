/*
 * IndexGenDataSet.java
 *
 * Created on April 1, 2007, 7:55 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.qds;

/**
 * Dataset that simply returns the index as the value.  
 * @author jbf
 */
public final class IndexGenDataSet extends AbstractDataSet {
    
    int length;
    
    /** 
     * Creates a new instance of IndexGenDataSet
     * @param length 
     */
    public IndexGenDataSet( int length ) {
        super();
        this.length= length;
        properties.put( QDataSet.MONOTONIC, Boolean.TRUE );
    }

    @Override
    public int rank() {
        return 1;
    }

    @Override
    public double value(int i) {
        return i;
    }

    @Override
    public int length() {
        return length;
    }

}
