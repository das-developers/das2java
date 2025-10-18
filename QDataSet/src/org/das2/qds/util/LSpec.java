/*
 * LSpec.java
 *
 * Created on April 24, 2007, 11:06 PM
 */

package org.das2.qds.util;

import java.util.LinkedHashMap;
import java.util.Map;
import org.das2.datum.Datum;
import org.das2.datum.Units;
import org.das2.qds.ArrayDataSet;
import org.das2.qds.DDataSet;
import org.das2.qds.DataSetUtil;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.ops.Ops;

/**
 * Form a rank 2 dataset with L and Time for tags by identifying monotonic sweeps
 * in two rank 1 datasets, and interpolating along the sweep.
 * 
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
     * just the inward sweeps
     */
    public static int DIR_INWARD= -1;
    
    /**
     * just the outward sweeps
     */
    public static int DIR_OUTWARD= 1;
    
    /**
     * include both sweeps
     */
    public static int DIR_BOTH= 0;
        
    /**
     * identify monotonically increasing or decreasing segments of the dataset.
     * @param lds the dataset which sweeps back and forth, such as LShell or MagLat (or a sine wave for testing).
     * @param dir 0=both 1=outward 2= inward
     * @return rank 2 data set of sweep indeces, dim0 is sweep number, dim1 is two-element [ start, end(inclusive) ].
     */
    public static QDataSet identifySweeps( QDataSet lds, int dir ) {

        DataSetBuilder builder= new DataSetBuilder( 2, 100, 2 );
        DataSetBuilder dep0builder= new DataSetBuilder( 2, 100, 2 );
        double slope0= lds.value(1) - lds.value(0);
        int start=0;
        int end=0; // index of the right point of slope0.

        QDataSet wds= SemanticOps.weightsDataSet(lds);
        ArrayDataSet tds= ArrayDataSet.copy( SemanticOps.xtagsDataSet(lds) );
        wds= Ops.multiply( wds, SemanticOps.weightsDataSet(tds) );
        
        Datum cadence= SemanticOps.guessXTagWidth( tds, lds );
        cadence= cadence.multiply(10.0); // kludge for data on 2012-10-25
        tds.putProperty( QDataSet.CADENCE, DataSetUtil.asDataSet(cadence) );
        QDataSet nonGap= SemanticOps.cadenceCheck(tds,lds);

        for ( int i=1; i<lds.length(); i++ ) {
            double slope1=  lds.value(i) - lds.value(i-1);
            if ( slope0 * slope1 <= 0. || ( nonGap.value(i-1)==1 && nonGap.value(i)==0 ) ) {
                if ( slope0!=0. && ( dir==0 || ( slope0*dir>0 && end-start>1 ) ) ) {  //TODO: detect jumps in the LShell, e.g. only downward: \\\\\
                    if ( start<end ) {
                        builder.putValue( -1, 0, start );
                        builder.putValue( -1, 1, end-1 );
                        builder.nextRecord();
                        dep0builder.putValue( -1, 0, tds.slice(start) );
                        dep0builder.putValue( -1, 1, tds.slice(end-1) );
                        dep0builder.nextRecord();
                    } else {
                        // all fill.
                    }
                }
                if ( slope1!=0. || nonGap.value(i)>0 ) {
                    start= i;
                }
            } else if ( wds.value(i)>0 ) {
                if ( nonGap.value(i-1)==0 && nonGap.value(i)==1 ) {
                    start= i;
                    end= i+1;
                } else {
                    end= i+1; // end is not inclusive
                }
            }
            slope0= slope1;
        }
        
        dep0builder.putProperty( QDataSet.BINS_1, "min,maxInclusive" );
        dep0builder.putProperty( QDataSet.UNITS, SemanticOps.getUnits(tds) );
        
        DDataSet result= builder.getDataSet();
        result.putProperty( QDataSet.DEPEND_0, dep0builder.getDataSet() );
        result.putProperty( QDataSet.RENDER_TYPE, "eventsBar" );
        
        return result;
        
    }
    
    /**
     *
     * @param datax data series that is aggregation of monotonic series (up and down)
     * @param start start index limiting search
     * @param end end index inclusive limiting search
     * @param x the value to locate.
     * @param guess guess index, because we are calling this repeatedly
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
        return index;
    }
    
    /**
     * this is probably old and shouldn't be used.
     * @param lds
     * @param zds
     * @param start
     * @param end
     * @param col
     * @param lgrid
     * @param ds 
     */
    private static void interpolate( QDataSet lds, QDataSet zds, int start, int end, int col, QDataSet lgrid, DDataSet ds ) {
        
        Units u= SemanticOps.getUnits( ds );

        double fill= u.getFillDouble();
        ds.putProperty( QDataSet.FILL_VALUE, fill );
        
        if ( ! u.equals(  SemanticOps.getUnits( zds ) ) ) {
            throw new IllegalArgumentException("zds units must be the same as ds units!");
        }
        
        QDataSet wds= Ops.valid(zds);
        
        int index;
        
        int dir= (int) Math.signum( lds.value(end) - lds.value( start ) );
        if ( dir > 0 ) index= start; else index= end;
        
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
                if ( ( wds.value(index)==0  ) || wds.value( index+1 )==0 ) {
                    ds.putValue( col, i, fill );
                } else {
                    ds.putValue( col, i, zds.value(index) * ( 1.0 - alpha ) + zds.value(index+1) * alpha );
                }
            }
        }
        
    }
    
//    private static double guessCadence( QDataSet xds, final int skip ) {
//        double cadence = 0;
//        
//        // calculate average cadence for consistent points.  Preload to avoid extra branch.
//        double cadenceS= 0;
//        int cadenceN= 1;
//        
//        for ( int i=skip; i < xds.length(); i++) {
//            double cadenceAvg;
//            cadenceAvg= cadenceS/cadenceN;
//            cadence = xds.value(i) - xds.value(i-skip);
//            if ( cadence > 1.5 * cadenceAvg) {
//                cadenceS= cadence;
//                cadenceN= 1;
//            } else if ( cadence < 1.5 * cadenceAvg ) {
//                cadenceS+= cadence;
//                cadenceN+= 1;
//            }
//        }
//        return  cadenceS/cadenceN;
//    }
    
    /**
     * rebin the datasets to rank 2 dataset ( time, LShell ), by interpolating along sweeps.  This
     * dataset has the property "sweeps", which is a dataset that indexes the input datasets.
     * @param lds rank 1 dataset of length N
     * @param zds rank 1 dataset of length N, indexed along with {@code lds}
     * @param lgrid rank 1 dataset indicating the dim 1 tags for the result dataset.
     * @return a rank 2 dataset, with one column per sweep, interpolated to {@code lgrid}
     */
    public static QDataSet rebin( QDataSet lds, QDataSet zds, QDataSet lgrid ) {
        return rebin( lds, zds, lgrid, 0 );
    }
    
    /**
     * alternate, convenient interface which where tlz is a bundle of buckshot data (T,L,Z)
     * 
     * @param tlz x,y,z (T,L,Z) bundle of Z values collected along inward and outward sweeps of L in time.
     * @param lgrid desired uniform grid of L values.
     * @param dir =1 increasing (outward) only, =-1 decreasing (inward) only, 0 both.  
     * @return a rank 2 dataset, with one column per sweep, interpolated to {@code lgrid}
     */
    public static QDataSet rebin( QDataSet tlz, QDataSet lgrid, int dir ) {
        QDataSet lds= Ops.link( Ops.slice1(tlz,0), Ops.slice1(tlz,1) );
        QDataSet zds= Ops.slice1(tlz,2);
        return rebin( lds, zds, lgrid, dir );
    }
    
    /**
     * alternate algorithm following Brian Larson's algorithm that rebin the datasets to rank 2 dataset ( time, LShell ), by 
     * interpolating along sweeps.  This dataset has the property "sweeps", which is a dataset that indexes the input datasets.
     * @param lds The L values corresponding to y axis position, which should be a function of time.
     * @param tt The Time values corresponding to x axis position.  If null, then use lds.property(QDataSet.DEPEND_0).
     * @param zds the Z values corresponding to the parameter we wish to organize
     * @param tspace rank 0 cadence, such as dataset('9 hr')
     * @param lgrid rank 1 data is the grid points, such as  linspace( 2.,8.,30 )
     * @param dir =1 increasing (outward) only, =-1 decreasing (inward) only, 0 both
     * @return a rank 2 dataset, with one column per sweep, interpolated to {@code lgrid}
     */
    public static QDataSet rebin( QDataSet tt, QDataSet lds, QDataSet zds, QDataSet tspace, QDataSet lgrid, int dir ) {
        final QDataSet sweeps= identifySweeps( lds, dir );

        if ( tt==null ) {
            tt= (QDataSet) lds.property(QDataSet.DEPEND_0);
        }
        
        Units tu= SemanticOps.getUnits(tt);
        
        Number fill= (Number) zds.property( QDataSet.FILL_VALUE );
        double dfill= fill==null ? -1e31 : fill.doubleValue();

        QDataSet wds= DataSetUtil.weightsDataSet(zds);

        int ny= lgrid.length();
        double[] ss= new double[ny];
        double[] nn= new double[ny];

        DataSetBuilder builder= new DataSetBuilder( 2, 100, ny );
        DataSetBuilder tbuilder= new DataSetBuilder( 1, 100 );
        tbuilder.putProperty( QDataSet.UNITS, tu );
        
        builder.putProperty( QDataSet.FILL_VALUE,dfill );
        
        int ix=-1;
        double nextx= Double.MAX_VALUE;
        double dt= DataSetUtil.asDatum( tspace ).doubleValue( tu.getOffsetUnits() );
        double t0= -1;
        double dg= lgrid.value(1)-lgrid.value(0);
        double g0= lgrid.value(0);
        int n= ny-1;
        for ( int i=2; i<n; i++ ) {
           //if ( (lgrid.value(i)-lgrid.value(i-1)/dg ) throw new IllegalArgumentException( "lgrid must be uniform linear" );
        }
        for ( int i=0; i<sweeps.length(); i++ ) {
            int ist= (int)sweeps.value(i,0);
            int ien= (int)sweeps.value(i,1);
            for ( int j= ist; j<ien; j++ ) {
                if ( ix==-1 || tt.value(j)-nextx >= 0  ) { //reset the accumulators
                    nextx= dt * Math.ceil( tt.value(j) / dt );  // next threshold
                    if ( ix>-1 ) {
                        for ( int k=0; k<ny; k++ ) {
                            if ( nn[k]==0 ) {
                                builder.putValue( -1, k, dfill );
                            } else {
                                builder.putValue( -1, k, ss[k]/nn[k] );
                            }
                        }
                        builder.nextRecord();
                        tbuilder.putValue( -1, t0-dt/2 );
                        tbuilder.nextRecord();
                    }
                    t0= nextx;
                    
                    for ( int k=0; k<ny; k++ ) {
                        ss[k]=0;
                        nn[k]=0;
                    }

                    ix++;
                }
                int iy= (int)Math.floor( ( lds.value(j) - g0 ) / dg );
                if ( iy>=0 && iy<ny ) {
                    double w= wds.value(j);
                    if ( w>0 ) {
                        ss[iy]+= w*zds.value(j);
                        nn[iy]+= w;
                    }
                }
            }
        }

        DDataSet result= builder.getDataSet();
        result.putProperty(QDataSet.DEPEND_0,tbuilder.getDataSet());
        result.putProperty(QDataSet.DEPEND_1,lgrid);
        DataSetUtil.copyDimensionProperties( zds, result );

        String title= (String) result.property( QDataSet.TITLE );
        if ( title==null ) title="";
        if ( dir==-1 ) {
            title+= "!cinward";
        } else if ( dir==1 ) {
            title+= "!coutward";
        }
        result.putProperty( QDataSet.TITLE, title );

        result.putProperty( QDataSet.FILL_VALUE, dfill );
        return result;

    }
    /**
     * rebin the datasets to rank 2 dataset ( time, LShell ), by interpolating along sweeps.  This
     * dataset has the property "sweeps", which is a dataset that indexes the input datasets.
     * @param lds rank 1 dataset of length N
     * @param zds rank 1 dataset of length N, indexed along with {@code lds}
     * @param lgrid rank 1 dataset indicating the dim 1 tags for the result dataset.
     * @param dir =1 increasing (outward) only, =-1 decreasing (inward) only, 0 both
     * @return a rank 2 dataset, with one column per sweep, interpolated to {@code lgrid}
     */
    public static QDataSet rebin( QDataSet lds, QDataSet zds, QDataSet lgrid, int dir ) {

        final QDataSet sweeps= identifySweeps( lds, dir );
        
        DDataSet result= DDataSet.createRank2( sweeps.length(), lgrid.length() );
        result.putProperty( QDataSet.UNITS, zds.property( QDataSet.UNITS ) );
        
        for ( int i=0; i<sweeps.length(); i++ ) {
            interpolate( lds, zds, (int) sweeps.value( i,0 ), (int) sweeps.value( i,1 ), i, lgrid, result );
        }
        
        DDataSet xtags= DDataSet.createRank2( sweeps.length(), 2 );
        
        QDataSet dep0= (QDataSet) lds.property(QDataSet.DEPEND_0);
        if ( dep0!=null ) {
            
            for ( int i=0; i<sweeps.length(); i++ ) {
                xtags.putValue( i,0,dep0.value( (int)sweeps.value( i,0 ) ) );
                xtags.putValue( i,1,dep0.value( (int)sweeps.value( i,1 ) ) );
            }

            DataSetUtil.putProperties( DataSetUtil.getDimensionProperties(dep0,null), xtags );
            Units xunits= SemanticOps.getUnits(dep0);
            xtags.putProperty( QDataSet.UNITS, xunits );
            xtags.putProperty( QDataSet.BINS_1, QDataSet.VALUE_BINS_MIN_MAX );
            xtags.putProperty(QDataSet.MONOTONIC, org.das2.qds.DataSetUtil.isMonotonic(dep0) );

        } else {
            for ( int i=0; i<sweeps.length(); i++ ) {
                xtags.putValue( i,0, sweeps.value( i,0 ) );
                xtags.putValue( i,1, sweeps.value( i,1 ) );
            }
        }

        Map<String,Object> userProps= new LinkedHashMap<>();
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
