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
 * return a value based on the scale and offset.  For example, this
 * is used to create timetags:
 *   TagGenDataSet( 1440, 60, 0, Units.t2000 )
 * These are the 1440 minutely timetags in the first day of 2000-01-01.
 * @author jbf
 */
public class TagGenDataSet extends IndexGenDataSet {
    
    double scale, offset;
    Units units;

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
            this.units= units;
        } else {
            this.units= Units.dimensionless;
        }
    }
    
    @Override
    public double value(int i) {
        return i * scale + offset; //TODO: check numerical stability, this is an extrapolation.
    }

    @Override
    public QDataSet trim(int start, int end) {
        return new TagGenDataSet( end-start, scale, offset + start*scale );
    }

    @Override
    public QDataSet slice(int i) {
        return DRank0DataSet.create( i * scale + offset, units );
    }
    
}
