/*
 * LSpec.java
 *
 * Created on April 24, 2007, 11:06 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.virbo.dsutil;

import java.util.LinkedHashMap;
import java.util.Map;
import org.das2.datum.Units;
import org.virbo.dataset.ArrayDataSet;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DRank0DataSet;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;

/**
 * Form a rank 2 dataset with L and Time for tags by identifying monotonic sweeps
 * in two rank 1 datasets.
 * @author jbf
 */
public class LSpec {

    /**
     * identifies the sweep of each record
     */
    public static final String USER_PROP_SWEEPS = "sweeps";
    
    /** Creates a new instance of LSpec */
    private LSpec() {
    }
    
    /**
     * identify monotonically increasing or decreasing segments of the dataset.
     *
     * @return rank 2 data set of sweep indeces, dim0 is sweep number, dim1 is two-element [ start, end(exclusive) ].
     */
    private static QDataSet identifySweeps( QDataSet lds, int dir ) {

        DataSetBuilder builder= new DataSetBuilder( 2, 100, 2 );
        double slope0= lds.value(1) - lds.value(0);
        int start=0;
        int end=0; // index of the right point of slope0.

        QDataSet wds= SemanticOps.weightsDataSet(lds);
        QDataSet tds= SemanticOps.xtagsDataSet(lds);
        QDataSet nonGap= SemanticOps.cadenceCheck(tds,lds);

        for ( int i=1; i<lds.length(); i++ ) {
            if ( wds.value(i)==0 ) continue;
            double slope1=  lds.value(i) - lds.value(i-1);
            if ( slope0 * slope1 <= 0. || ( nonGap.value(i-1)==1 && nonGap.value(i)==0 ) ) {
                if ( slope0!=0. && ( dir==0 || ( slope0*dir>0 && end-start>1 ) ) ) {  //TODO: detect jumps in the LShell, e.g. only downward: \\\\\
                    builder.putValue( -1, 0, start );
                    builder.putValue( -1, 1, end );
                    builder.nextRecord();
                }
                if ( slope1!=0. || nonGap.value(i)>0 ) {
                    start= i-1;
                }
            } else if ( nonGap.value(i-1)==0 && nonGap.value(i)==1 ) {
                start= i;
                end= i+1;
            } else {
                end= i+1; // end is not inclusive
            }
            slope0= slope1;
        }
        
        return builder.getDataSet();
        
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
    private static int findIndex( QDataSet datax, int start, int end, double x, int guess, int dir ) {
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
        double cadence = 0;
        
        // calculate average cadence for consistent points.  Preload to avoid extra branch.
        double cadenceS= 0;
        int cadenceN= 1;
        
        for ( int i=skip; i < xds.length(); i++) {
            double cadenceAvg;
            cadenceAvg= cadenceS/cadenceN;
            cadence = xds.value(i) - xds.value(i-skip);
            if ( cadence > 1.5 * cadenceAvg) {
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
        return rebin( lds, zds, lgrid, 0 );
    }
    
    /**
     * rebin the datasets to rank 2 dataset ( time, LShell ), by interpolating along sweeps.  This
     * dataset has the property "sweeps", which is a dataset that indexes the input datasets.
     * @param lds rank 1 dataset of length N
     * @param zds rank 1 dataset of length N, indexed along with <tt>lds</tt>
     * @param lgrid rank 1 dataset indicating the dim 1 tags for the result dataset.
     * @param dir =1 increasing (outward) only, =-1 decreasing (inward) only, 0 both
     * @return a rank 2 dataset, with one column per sweep, interpolated to <tt>lgrid</tt>
     */
    public static QDataSet rebin( QDataSet lds, QDataSet zds, QDataSet lgrid, int dir ) {

        final QDataSet sweeps= identifySweeps( lds, dir );
        
        DDataSet result= DDataSet.createRank2( sweeps.length(), lgrid.length() );
        result.putProperty( QDataSet.UNITS, zds.property( QDataSet.UNITS ) );
        
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

            DataSetUtil.putProperties( DataSetUtil.getDimensionProperties(dep0,null), xtags );
            Units xunits= SemanticOps.getUnits(dep0);
            xtags.putProperty( QDataSet.UNITS, xunits );
            xtags.putProperty( QDataSet.MONOTONIC, org.virbo.dataset.DataSetUtil.isMonotonic(dep0) );
            xtags.putProperty( QDataSet.CADENCE, DRank0DataSet.create( 1.5 * guessCadence(xtags,2), xunits.getOffsetUnits() ) );
        }

        Map<String,Object> userProps= new LinkedHashMap();
        userProps.put(USER_PROP_SWEEPS, sweeps);

        result.putProperty( QDataSet.USER_PROPERTIES, userProps );

        if ( lgrid.property(QDataSet.UNITS)==null ) {
            ArrayDataSet lgridCopy= ArrayDataSet.copy(lgrid); // often linspace( 4, 10, 30 ) is used to specify locations.  Go ahead and copy over units.
            Units u= (Units) lds.property( QDataSet.UNITS );
            if ( u!=null ) lgridCopy.putProperty( QDataSet.UNITS, u );
            lgrid= lgridCopy;
        }
        
        result.putProperty( QDataSet.DEPEND_1, lgrid );
        result.putProperty( QDataSet.DEPEND_0, xtags );

        DataSetUtil.putProperties( DataSetUtil.getDimensionProperties( zds, null ), result );
        
        return result;
    }
}
