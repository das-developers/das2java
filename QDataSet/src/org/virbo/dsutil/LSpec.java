/*
 * LSpec.java
 *
 * Created on April 24, 2007, 11:06 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.virbo.dsutil;

import edu.uiowa.physics.pw.das.datum.Units;
import org.virbo.dataset.AbstractDataSet;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.QDataSet;

/**
 *
 * @author jbf
 */
public class LSpec {
    
    /** Creates a new instance of LSpec */
    private LSpec() {
    }
    
    /**
     * identify monotonically increasing or decreasing segments of the dataset.
     *
     * @return rank 2 data set of sweep indeces, dim0 is sweep number, dim1 is two-element [ start, end(exclusive) ].
     */
    private static QDataSet identifySweeps( QDataSet lds ) {
        DDataSet result= DDataSet.createRank2( lds.length(), 2 );
        double slope0= lds.value(1) - lds.value(0);
        int start=0;
        int index=0;
        int end=0; // index of the right point of slope0.
        
        Units u= (Units) lds.property( QDataSet.UNITS );
        
        for ( int i=1; i<lds.length(); i++ ) {
            if ( u.isFill( lds.value(i) ) || u.isFill( lds.value(i-1) ) ) continue;
            double slope1=  lds.value(i) - lds.value(i-1);
            if ( slope0 * slope1 <= 0. ) {
                if ( slope0!=0. ) {
                    result.putValue( index, 0, start );
                    result.putValue( index, 1, end );
                    index++;
                }
                if ( slope1!=0. ) {
                    start= i-1;
                }
            } else {
                end= i+1; // end is not inclusive
            }
            slope0= slope1;
        }
        
        result.putLength(index);
        return result;
    }
    
    /**
     *
     * @param datax
     * @param start
     * @param end
     * @param x
     * @param guess
     * @param dir 1 if datax is increasing, -1 if decreasing
     * @return index
     */
    public final static int findIndex( QDataSet datax, int start, int end, double x, int guess, int dir ) {
        int index= Math.max( Math.min( guess, end-1 ), start );
        if ( dir > 0 ) {
            while ( index<end && datax.value(index+1) < x ) index++;
            while ( index>start && datax.value(index) > x ) index--;
        } else {
            while ( index<end && datax.value(index+1) > x ) index++;
            while ( index>start && datax.value(index) < x ) index--;
        }
        if ( index==end ) index--; // end is non-inclusive
        return index;
    }
    
    private static void interpolate( QDataSet lds, QDataSet zds, int start, int end, int col, QDataSet lgrid, DDataSet ds ) {
        
        Units u= (Units) ds.property( QDataSet.UNITS );
        double fill= u.getFillDouble();
        
        if ( ! u.equals(  (Units) zds.property( QDataSet.UNITS ) ) ) {
            throw new IllegalArgumentException("zds units must be the same as ds units!");
        }
        
        int index= start;
        
        int dir= (int) Math.signum( lds.value(end-1) - lds.value( start ) );
        if ( dir > 0 ) index= start; else index= end-1;
        
        for ( int i=0; i<lgrid.length(); i++ ) {
            double ll= lgrid.value(i);
            index= findIndex( lds, start, end, ll, index, dir );
            double alpha= ( ll - lds.value(index) ) / ( lds.value(index+1) - lds.value(index) );
            if ( alpha < 0 ) {
                ds.putValue( col, i, fill );
            } else if ( alpha > 1 ) {
                ds.putValue( col, i, fill );
            } else if ( alpha == 0 ) {
                ds.putValue( col, i, zds.value(index) );
            } else {
                if ( u.isFill( zds.value(index) ) || u.isFill( zds.value( index+1 ) ) ) {
                    ds.putValue( col, i, fill );
                } else {
                    ds.putValue( col, i, zds.value(index) * ( 1.0 - alpha ) + zds.value(index+1) * alpha );
                }
            }
        }
        
    }
    
    private static double guessCadence( QDataSet xds, final int skip ) {
        double cadence = Double.MAX_VALUE;
        
        // calculate average cadence for consistent points.  Preload to avoid extra branch.
        double cadenceS= Double.MAX_VALUE;
        int cadenceN= 1;
        
        for ( int i=skip; i < xds.length(); i++) {
            double cadenceAvg;
            cadenceAvg= cadenceS/cadenceN;
            cadence = xds.value(i) - xds.value(i-skip);
            if ( cadence < 0.5 * cadenceAvg) {
                cadenceS= cadence;
                cadenceN= 1;
            } else if ( cadence < 1.5 * cadenceAvg ) {
                cadenceS+= cadence;
                cadenceN+= 1;
            }
        }
        return  cadenceS/cadenceN;
    }
    
    /**
     * rebin the datasets to rank 2 dataset ( time, LShell ), by interpolating along sweeps.  This
     * dataset has the property "sweeps", which is a dataset that indexes the input datasets.
     * @param lds rank 1 dataset of length N
     * @param zds rank 1 dataset of length N, indexed along with <tt>lds</tt>
     * @param lgrid rank 1 dataset indicating the dim 1 tags for the result dataset.
     * @return a rank 2 dataset, with one column per sweep, interpolated to <tt>lgrid</tt>
     */
    public static QDataSet rebin( QDataSet lds, QDataSet zds, QDataSet lgrid ) {
        final QDataSet sweeps= identifySweeps( lds );
        
        DDataSet result= DDataSet.createRank2( sweeps.length(), lgrid.length() );
        result.putProperty( QDataSet.UNITS, lds.property( QDataSet.UNITS ) );
        
        for ( int i=0; i<sweeps.length(); i++ ) {
            interpolate( lds, zds, (int) sweeps.value( i,0 ), (int) sweeps.value( i,1 ), i, lgrid, result );
        }
        
        DDataSet xtags= DDataSet.createRank1( sweeps.length() );
        for ( int i=0; i<sweeps.length(); i++ ) {
            xtags.putValue( i, ( sweeps.value( i,0 ) + sweeps.value( i,1 ) ) / 2 );
        }
        
        QDataSet dep0= (QDataSet) lds.property(QDataSet.DEPEND_0);
        if ( dep0!=null ) {
            
            for ( int i=0; i<sweeps.length(); i++ ) {
                xtags.putValue( i,
                        ( dep0.value( (int)sweeps.value( i,0 ) )
                        + dep0.value( (int)sweeps.value( i,1 ) ) ) / 2 );
                
            }
            xtags.putProperty( QDataSet.UNITS, dep0.property(QDataSet.UNITS) );
            xtags.putProperty( QDataSet.MONOTONIC, org.virbo.dataset.DataSetUtil.isMonotonic(dep0) );
            xtags.putProperty( QDataSet.CADENCE, 1.5 * guessCadence(xtags,2) );
        }
        
        result.putProperty( "sweeps", sweeps );
        
        result.putProperty( QDataSet.DEPEND_1, lgrid );
        result.putProperty( QDataSet.DEPEND_0, xtags );
        
        return result;
    }
}
