/*
 * TagGenDataSet.java
 *
 * Created on April 24, 2007, 11:47 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.virbo.dataset;

/**
 *
 * @author jbf
 */
public class TagGenDataSet extends IndexGenDataSet {
    
    double scale, offset;
    
    /** Creates a new instance of TagGenDataSet */
    public TagGenDataSet( int length, double scale, double offset ) {
        super( length );
        this.scale= scale;
        this.offset= offset;
        putProperty( QDataSet.CADENCE, DRank0DataSet.create(scale) );
    }
    
    public double value(int i) {
        return i * scale + offset;
    }

}
