/*
 * IndexGenDataSet.java
 *
 * Created on April 1, 2007, 7:55 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.virbo.dataset;

/**
 * This is also used a base class.
 * @author jbf
 */
public class IndexGenDataSet extends AbstractDataSet {
    
    int length;
    
    /** Creates a new instance of IndexGenDataSet */
    public IndexGenDataSet( int length ) {
        super();
        this.length= length;
        properties.put( QDataSet.MONOTONIC, Boolean.TRUE );
    }

    public int rank() {
        return 1;
    }

    public double value(int i) {
        return i;
    }

    public int length() {
        return length;
    }

}
