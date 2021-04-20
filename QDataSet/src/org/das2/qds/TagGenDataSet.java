/*
 * TagGenDataSet.java
 *
 * Created on April 24, 2007, 11:47 PM
 */

package org.das2.qds;

import org.das2.datum.Units;

/**
 * return a value based on the scale and offset.  For example, this
 * is used to create timetags:
 * <blockquote><pre>
 *   TagGenDataSet( 1440, 60, 0, Units.t2000 )
 * </pre></blockquote>
 * These are the 1440 minutely timetags in the first day of 2000-01-01.
 * @author jbf
 */
public class TagGenDataSet extends AbstractRank1DataSet {
    
    double scale, offset;

    /**
     * create new dimensionless TagGenDataSet
     * @param length number of elements 
     * @param scale the increment between elements
     * @param offset the value for the zeroth element.
     */    
    public TagGenDataSet( int length, double scale, double offset ) {
        this( length, scale, offset, null );
    }
    
    /**
     * create new TagGenDataSet
     * @param length number of elements 
     * @param scale the increment between elements
     * @param offset the value for the zeroth element.
     * @param units the units of the data.
     */
    public TagGenDataSet( int length, double scale, double offset, Units units ) {
        super(length);
        this.scale= scale;
        this.offset= offset;
        if ( units!=null ) {
            putProperty( QDataSet.CADENCE, DRank0DataSet.create( scale, units.getOffsetUnits() ) );
            putProperty( QDataSet.UNITS, units );
        } else {
            putProperty( QDataSet.UNITS, Units.dimensionless );
            putProperty( QDataSet.CADENCE, DRank0DataSet.create( scale, Units.dimensionless ) );
        }
        if ( scale>0 ) putProperty( QDataSet.MONOTONIC, Boolean.TRUE );
    }
    
    @Override
    public double value(int i) {
        return i * scale + offset; //TODO: check numerical stability, this is an extrapolation.
    }

    @Override
    public QDataSet trim(int start, int end) {
        if ( start==0 && end==length() ) {
            return this;
        }
        Units u= (Units)property(QDataSet.UNITS);
        if ( u==null ) u=Units.dimensionless;
        TagGenDataSet result= new TagGenDataSet( end-start, scale, offset + start*scale, u );
        DataSetUtil.copyDimensionProperties( this, result );
        return result;
    }

    @Override
    public QDataSet slice(int i) {
        Units u= (Units) property(QDataSet.UNITS);
        if ( u==null ) u=Units.dimensionless;
        return DRank0DataSet.create( i * scale + offset, u );
    }
    
}
