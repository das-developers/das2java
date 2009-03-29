/*
 * TagGenDataSet.java
 *
 * Created on April 24, 2007, 11:47 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.virbo.dataset;

import org.das2.datum.Units;

/**
 *
 * @author jbf
 */
public class TagGenDataSet extends IndexGenDataSet {
    
    double scale, offset;

    public TagGenDataSet( int length, double scale, double offset ) {
        this( length, scale, offset, null );
    }
    
    public TagGenDataSet( int length, double scale, double offset, Units units ) {
        super( length );
        this.scale= scale;
        this.offset= offset;
        if ( units!=null ) {
            putProperty( QDataSet.CADENCE, DRank0DataSet.create(scale, units.getOffsetUnits() ) );
            putProperty( QDataSet.UNITS, units );
        }
    }
    
    public double value(int i) {
        return i * scale + offset;
    }

}
