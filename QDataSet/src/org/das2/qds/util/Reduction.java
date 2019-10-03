
package org.das2.qds.util;

import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.Datum;
import org.das2.datum.DatumUtil;
import org.das2.datum.Units;
import org.das2.datum.UnitsConverter;
import org.das2.datum.UnitsUtil;
import org.das2.qds.ConstantDataSet;
import org.das2.util.LoggerManager;
import org.das2.qds.DDataSet;
import org.das2.qds.DataSetOps;
import org.das2.qds.DataSetUtil;
import org.das2.qds.IDataSet;
import org.das2.qds.JoinDataSet;
import org.das2.qds.MutablePropertyDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.WritableDataSet;
import org.das2.qds.ops.Ops;

/**
 * Reduction is set of static methods for reducing data, or 
 * averaging data to make smaller datasets.
 * @author jbf
 */
public class Reduction {
    
    private static final Logger logger= LoggerManager.getLogger("qdataset.ops.reduction");
    
    /**
     * return a converter for differences.  If dstUnits are specified,
     * then explicitly this is the target.
     * @param src source dataset
     * @param dst target dataset
     * @param dstUnits if not null, then explicitly use these units.
     * @return a converter for differences. 
     */
    private static UnitsConverter getDifferencesConverter( QDataSet src, QDataSet dst, Units dstUnits ) {

        Units unitsIn, unitsOut;
        unitsIn= (Units) dst.property( QDataSet.UNITS );
        if ( unitsIn==null ) unitsIn= Units.dimensionless;
        unitsOut= (Units)src.property( QDataSet.UNITS );
        if ( unitsOut==null ) unitsOut= Units.dimensionless;

        UnitsConverter xuc;
        if ( dstUnits!=null ) {
            xuc= unitsOut.getConverter( dstUnits );
        } else {
            xuc= unitsOut.getConverter( unitsIn.getOffsetUnits() );
        }
        return xuc;
    }

    /**
     * @param ds a rank1 or rank2 waveform dataset.
     * @param xLimit the target resolution, result will be finer than this, if possible.
     * @return either the original dataset when there is no reduction to be done, or a series data set with bins (deltas for now).
     * @see org.das2.qstream.filter.MinMaxReduceFilter.  This is basically a copy of that code.
     */
    private static QDataSet reducexWaveform( QDataSet ds, QDataSet xLimit ) {
        DataSetBuilder xbuilder;
        DataSetBuilder ybuilder;
        DataSetBuilder yminbuilder;
        DataSetBuilder ymaxbuilder;
        
        xbuilder= new DataSetBuilder( 1, 1000 );
        ybuilder= new DataSetBuilder( 1, 1000 );
        yminbuilder= new DataSetBuilder( 1, 1000 );
        ymaxbuilder= new DataSetBuilder( 1, 1000 );
        //wbuilder= new DataSetBuilder( 2, 1000, ds.length(0) );
        
        Datum cadence= DataSetUtil.asDatum(xLimit);
        QDataSet _offsets= (QDataSet) ds.property(QDataSet.DEPEND_1);
        MutablePropertyDataSet offsets= DataSetOps.makePropertiesMutable(_offsets);
        offsets.putProperty( QDataSet.VALID_MIN, null ); //TODO:  EMFISIS HFR has incorrect VALID_MAX.
        offsets.putProperty( QDataSet.VALID_MAX, null );
        
        if ( offsets.rank()==2 ) {
            offsets= (MutablePropertyDataSet)offsets.slice(0);
            logger.fine("slice(0) on rank 2 dataset because code doesn't support time-varying DEPEND_1");
        }
        
        int icadence= 4;
        while ( icadence<offsets.length()/2 && cadence.gt( DataSetUtil.asDatum(offsets.slice(icadence)).subtract( DataSetUtil.asDatum( offsets.slice(0)) ) ) ) {
            icadence= icadence*2;
        }
        icadence= icadence/2;                
        
        if ( icadence<4 ) {
            return ds;
        }
        
        QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
        
        xbuilder.putProperty( QDataSet.UNITS, dep0.property(QDataSet.UNITS) );
        if ( icadence<offsets.length() ) {
            xbuilder.putProperty( QDataSet.CADENCE, Ops.subtract(offsets.slice(icadence),offsets.slice(0)) );
        } else {
            xbuilder.putProperty( QDataSet.CADENCE, Ops.multiply( Ops.subtract(offsets.slice(icadence/2),offsets.slice(0)), 2 ) );
        }
        int iout= 0;
        
        for ( int j=0; j<ds.length(); j++ ) {
            int i=0;
            QDataSet ttag= dep0.slice(j);    
            QDataSet ds1= ds.slice(j);
            while ( (i+icadence)<offsets.length() ) {                     
                QDataSet ext= Ops.extent(ds1.trim(i,i+icadence) );
                QDataSet avg= Ops.reduceMean(ds1.trim(i,i+icadence),0);
                xbuilder.putValue(iout, Ops.add( ttag, offsets.slice(i+icadence/2) ).value() );
                yminbuilder.putValue(iout,ext.value(0));
                ymaxbuilder.putValue(iout,ext.value(1));
                ybuilder.putValue(iout,avg.value());                
                iout++;
                i+= icadence;
            }
        }
        
        DDataSet result= ybuilder.getDataSet();
        DataSetUtil.copyDimensionProperties( ds, result );
        yminbuilder.putProperty( QDataSet.UNITS, ds.property(QDataSet.UNITS) );
        ymaxbuilder.putProperty( QDataSet.UNITS, ds.property(QDataSet.UNITS) );
        
        result.putProperty( QDataSet.DELTA_MINUS, Ops.subtract( result, yminbuilder.getDataSet() ) ); // TODO: this should be BIN_PLUS and BIN_MINUS.
        result.putProperty( QDataSet.DELTA_PLUS, Ops.subtract( ymaxbuilder.getDataSet(), result ) );
        result.putProperty( QDataSet.DEPEND_0, xbuilder.getDataSet() );
        
        if ( result.property(QDataSet.CACHE_TAG)!=null ) result.putProperty(QDataSet.CACHE_TAG,null);
        return result;
                
    }
    
    /**
     * produce a simpler version of the dataset by averaging data adjacent in X.
     * code taken from org.das2.graph.GraphUtil.reducePath.  Adjacent points are
     * averaged together until a point is found that is not in the bin, and then
     * a new bin is started.  The bin's lower bounds are integer multiples
     * of xLimit.
     *
     * xLimit is a rank 0 dataset.
     *
     * 2015-06-18: xcadence and bins are now regular.
     * 
     * Because of high-resolution magnetometer data, this is extended to support this data type.
     * 
     * This will set the DELTA_PLUS and DELTA_MINUS variables to the extremes of 
     * each bin.  To remove these, use putProperty( QDataSet.DELTA_MINUS, None ) 
     * (None in Jython, null for Java) and putProperty( QDataSet.DELTA_PLUS, None ).
     * 
     * @param ds rank 1 or rank 2 dataset.  Must have DEPEND_0 (presently) and be a qube.  If this is null, then the result is null.
     * @param xLimit the size of the bins or null to indicate no limit.
     * @return the reduced dataset, or null if the input dataset was null.
     */
    public static QDataSet reducex( QDataSet ds, QDataSet xLimit ) {
        long t0= System.currentTimeMillis();
        logger.entering( "Reduction", "reducex" );
        if ( ds==null ) return ds; // Craig 2038937185
        
        if ( !DataSetUtil.isQube(ds) ) {
            throw new IllegalArgumentException("rank 2 dataset must be a qube");
        }
        if ( ds.rank()==0 ) {
            return ds;
        }

        DataSetBuilder xbuilder= new DataSetBuilder( 1, 1000 );
        DataSetBuilder ybuilder;
        DataSetBuilder yminbuilder;
        DataSetBuilder ymaxbuilder;
        DataSetBuilder wbuilder;

        if ( ds.rank()==1 ) {
            return reduce2D( ds, xLimit, null );

        } else if ( ds.rank()==2 ) {
            if ( SemanticOps.isRank2Waveform(ds) ) {
                return reducexWaveform( ds, xLimit );            
            } else {
                ybuilder= new DataSetBuilder( 2, 1000, ds.length(0) );
                yminbuilder= new DataSetBuilder( 2, 1000, ds.length(0) );
                ymaxbuilder= new DataSetBuilder( 2, 1000, ds.length(0) );
                wbuilder= new DataSetBuilder( 2, 1000, ds.length(0) );
            }
        } else if ( ds.rank()==3 && DataSetUtil.isQube(ds) ) {
            return reduceRankN(ds, DataSetUtil.asDatum(xLimit));
            
        } else if ( ds.rank()==3 && SemanticOps.isJoin(ds) ) {
            JoinDataSet result= new JoinDataSet(3);
            for ( int i=0; i<ds.length(); i++ ) {
                QDataSet ds1= ds.slice(i);
                result.join( reducex(ds1,xLimit) );
            }
            return result;
            
        } else {
            throw new IllegalArgumentException("only rank 1, rank 2, and rank 3 join datasets");
        }

        QDataSet x= (QDataSet) ds.property( QDataSet.DEPEND_0 );
        if ( x==null ) {
            if ( ds.rank()==2 && SemanticOps.isBundle(ds) ) {
                x= DataSetOps.unbundle(ds, 0); // often the x values.
            } else {
                x= new org.das2.qds.IndexGenDataSet(ds.length());
            }
        }

        int ny= ds.rank()==1 ? 1 : ds.length(0);

        double x0 = Float.MAX_VALUE;
        double sx0 = 0;
        double nx= 0;  // number in X average.
        double[] sy0 = new double[ny];
        double[] nn0 = new double[ny];
        double[] miny0 = new double[ny];
        for ( int j=0; j<ny; j++ ) miny0[j]= Double.POSITIVE_INFINITY;
        double[] maxy0 = new double[ny];
        for ( int j=0; j<ny; j++ ) maxy0[j]= Double.NEGATIVE_INFINITY;
        double ax0;
        double[] ay0 = new double[ny];

        UnitsConverter uc;
        double dxLimit;
        if ( xLimit!=null ) {
            uc= getDifferencesConverter( xLimit, x, null );
            dxLimit = uc.convert( xLimit.value() );
        } else {
            dxLimit= Double.MAX_VALUE;
        }

        int points = 0;
        //int inCount = 0;

        QDataSet wds= DataSetUtil.weightsDataSet(ds);

        int i=0;

        double fill= Double.NaN;

        while ( i<x.length() ) {
            //inCount++;

            double xx= x.value(i);
            QDataSet yy= ds.slice(i);
            QDataSet ww= wds.slice(i);

            double pxx = xx;

            double wx= 1; // weight to use for x.

            sx0 += pxx*wx; //TODO: to avoid numerical noise, remove delta before accumulating
            nx+= 1;

            if ( x0==Float.MAX_VALUE ) x0= Math.floor( xx / dxLimit ) * dxLimit;
            double dx = pxx - x0;

            for ( int j=0; j<ny; j++ ) {
                if ( ww.value(j)==0 ) {
                    continue;
                }

                if ( dx>= 0 && dx < dxLimit ) {
                    double pyy = yy.value(j);

                    sy0[j] += pyy*ww.value(j);
                    nn0[j] += ww.value(j);
                    if ( ww.value(j)>0 ) {
                        miny0[j] = Math.min( miny0[j], pyy );
                        maxy0[j] = Math.max( maxy0[j], pyy );
                    }
                }
            }

            if ( dx<0 || dx>= dxLimit ) { // clear the accumulators

                x0 = Math.floor(pxx/dxLimit) * dxLimit;

                for ( int j=0; j<ny; j++ ) {

                    if ( nx>0 ) {
                        boolean nv= nn0[j]==0;
                        //ax0 = sx0 / nx;
                        ax0 = x0 + dxLimit/2;
                        ay0[j] = nv ? fill : sy0[j] / nn0[j];
                        if (j==0 ) xbuilder.putValue( points, ax0 );
                        ybuilder.putValue( points, j, ay0[j] );
                        yminbuilder.putValue( points, j, nv ? fill : miny0[j] );
                        ymaxbuilder.putValue( points, j, nv ? fill : maxy0[j] );
                        wbuilder.putValue( points, j, nn0[j] );

                    }

                    double pyy = yy.value(j);
                    double wwj= ww.value(j);

                    if ( j==0 ) sx0 = pxx*wx;
                    sy0[j] = pyy*wwj;
                    nn0[j] = wwj;
                    nx= 1;
                    if ( wwj>0 ) {
                        miny0[j] = pyy;
                        maxy0[j] = pyy;
                    } else {
                        miny0[j] = Double.POSITIVE_INFINITY;
                        maxy0[j] = Double.NEGATIVE_INFINITY;
                    }

                }
                if ( nx>0 ) points++;

            }

            i++;

        }

        if ( nx>0 ) {
            for ( int j=0; j<ny; j++ ) {
                boolean nv= nn0[j]==0;
                //ax0 = sx0 / nx;
                ax0 = x0 + dxLimit/2;
                ay0[j] = nv ? fill : sy0[j] / nn0[j];
                if ( j==0 ) xbuilder.putValue( points, ax0 );
                ybuilder.putValue( points, j, ay0[j] );
                yminbuilder.putValue( points, j, nv ? fill : miny0[j] );
                ymaxbuilder.putValue( points, j, nv ? fill : maxy0[j] );
                wbuilder.putValue( points, j, nn0[j] );
            }
            points++;
        }

        MutablePropertyDataSet result= ybuilder.getDataSet();
        MutablePropertyDataSet xds= xbuilder.getDataSet();

        Map<String,Object> xprops= DataSetUtil.getDimensionProperties(x,null);
        if ( xprops.containsKey( QDataSet.CADENCE ) ) xprops.put( QDataSet.CADENCE, xLimit );
        if ( xprops.containsKey( QDataSet.CACHE_TAG ) ) xprops.put( QDataSet.CACHE_TAG, null );
        DataSetUtil.putProperties( xprops, xds );

        Map<String,Object> yprops= DataSetUtil.getProperties(ds);
        yprops.put( QDataSet.DEPEND_0, xds );
        for ( int j=1; j<ds.rank(); j++ ) {
            String DEP= "DEPEND_"+j;
            QDataSet dep1= (QDataSet)yprops.get(DEP);
            if ( dep1!=null && dep1.rank()==2 ) {
                if ( DataSetUtil.isConstant(dep1) ) {
                    yprops.put( DEP, dep1.slice(0) );
                } else {
                    logger.log(Level.INFO, "dropping {0} which is time-varying", DEP);
                    yprops.put( DEP, null );
                }
            }
        }
        DataSetUtil.putProperties( yprops, result );
        yminbuilder.putProperty( QDataSet.UNITS, SemanticOps.getUnits(result) );
        ymaxbuilder.putProperty( QDataSet.UNITS, SemanticOps.getUnits(result) );

        result.putProperty( QDataSet.DEPEND_0, xds );
        result.putProperty( QDataSet.WEIGHTS, wbuilder.getDataSet() );

        QDataSet yminDs= yminbuilder.getDataSet();
        QDataSet ymaxDs= ymaxbuilder.getDataSet();
        result.putProperty( QDataSet.DELTA_MINUS, Ops.subtract( result, yminDs ) ); // TODO: This bad behavior should be deprecated.
        result.putProperty( QDataSet.DELTA_PLUS, Ops.subtract( ymaxDs, result ) );
        result.putProperty( QDataSet.BIN_MIN, yminDs );
        result.putProperty( QDataSet.BIN_MAX, ymaxDs );
        
        logger.log( Level.FINE, "time to reducex({0} records -> {1} records) (ms): {2}", new Object[] { ds.length(), result.length(), System.currentTimeMillis()-t0 } );
        logger.exiting("Reduction", "reducex" );
        
        //System.err.println( String.format( "time to reducex(%d records -> %d records) (ms): %d", ds.length(), result.length(), System.currentTimeMillis()-t0) );

        return result;

    }

    /**
     * produce a simpler version of the dataset by averaging adjacent data.
     * code taken from org.das2.graph.GraphUtil.reducePath.  Adjacent points are
     * averaged together until a point is found that is not in the bin, and then
     * a new bin is started.  The bin's lower bounds are integer multiples
     * of xLimit and yLimit.
     *
     * If yLimit is null, then averaging is done for all points in the x bin,
     * regardless of how close they are in Y.  This is similarly true when
     * xLimit is null.
     *
     * xLimit and yLimit are rank 0 datasets, so that they can indicate that binning
     * should be done in log space rather than linear.  In this case, a SCALE_TYPE
     * for the dataset should be "log" and its unit should be convertible to
     * Units.logERatio (for example, Units.log10Ratio or Units.percentIncrease).
     * Note when either is log, then averaging is done in the log space.
     *
     * @param ds rank 1 dataset.  Must have DEPEND_0 (presently) 
     * @param xLimit the size of the bins or null to indicate no limit.
     * @param yLimit the size of the bins or null to indicate no limit.
     * @return the reduced dataset, rank 1 with DEPEND_0.
     */
    public static QDataSet reduce2D( QDataSet ds, QDataSet xLimit, QDataSet yLimit ) {
        logger.entering("Reduction", "reduce2D");
        long t0= System.currentTimeMillis();

        DataSetBuilder xbuilder= new DataSetBuilder( 1, 1000 );
        DataSetBuilder ybuilder= new DataSetBuilder( 1, 1000 );
        DataSetBuilder yminbuilder= new DataSetBuilder( 1, 1000 );
        DataSetBuilder ymaxbuilder= new DataSetBuilder( 1, 1000 );
        DataSetBuilder wbuilder= new DataSetBuilder( 1, 1000 ); // weights to go here

        QDataSet x= (QDataSet) ds.property( QDataSet.DEPEND_0 );
        if ( x==null ) {
            if ( SemanticOps.getUnits(xLimit)!=Units.dimensionless ) {
                throw new IllegalArgumentException("xLimit is not dimensionless, yet there are no timetags in the data set: "+ds );
            } else {
                x= new org.das2.qds.IndexGenDataSet(ds.length());
            }
        }
        QDataSet y= ds;

        double x0 = Float.MAX_VALUE;
        double y0 = Float.MAX_VALUE;
        double sx0 = 0;
        double sy0 = 0;
        double nn0 = 0;
        double miny0 = Double.POSITIVE_INFINITY;
        double maxy0 = Double.NEGATIVE_INFINITY;
        double ax0;
        double ay0;  // last averaged location

        boolean xlog= xLimit!=null && "log".equals( xLimit.property( QDataSet.SCALE_TYPE ) );
        boolean ylog= yLimit!=null && "log".equals( yLimit.property( QDataSet.SCALE_TYPE ) );

        UnitsConverter uc;
        double dxLimit, dyLimit;
        if ( xLimit!=null ) {
            uc= getDifferencesConverter( xLimit, x, xlog ? Units.logERatio : null );
            dxLimit = uc.convert( xLimit.value() );
        } else {
            dxLimit= Double.MAX_VALUE;
        }
        if ( yLimit!=null ) {
            uc= getDifferencesConverter( yLimit, y, ylog ? Units.logERatio : null );
            dyLimit = uc.convert( yLimit.value() );
        } else {
            dyLimit= Double.MAX_VALUE;
        }

        int points = 0;
        //int inCount = 0;

        QDataSet wds= DataSetUtil.weightsDataSet(y);

        int i=0;

        while ( i<x.length() ) {
            //inCount++;

            double xx= x.value(i);
            double yy= y.value(i);
            double ww= wds.value(i);

            if ( ww==0 ) {
                i++;
                continue;
            }

            double pxx = xlog ? Math.log(xx) : xx;
            double pyy = ylog ? Math.log(yy) : yy;

            double dx = pxx - x0;
            double dy = pyy - y0;

            if ( Math.abs(dx) < dxLimit && Math.abs(dy) < dyLimit) {
                sx0 += pxx*ww;
                sy0 += pyy*ww;
                nn0 += ww;
                if ( ww>0 ) {
                    miny0 = Math.min( miny0, yy);
                    maxy0 = Math.max( maxy0, yy);
                }
                i++;
                continue;
            }

            if ( nn0>0 ) {
                ax0 = sx0 / nn0;
                ay0 = sy0 / nn0;
                xbuilder.putValue( points, xlog ? Math.exp(ax0) : ax0 );
                ybuilder.putValue( points, ylog ? Math.exp(ay0) : ay0 );
                yminbuilder.putValue( points, miny0 );
                ymaxbuilder.putValue( points, maxy0 );
                wbuilder.putValue( points, nn0 );
                points++;
            }

            i++;

            x0 = dxLimit * ( 0.5 + (int) Math.floor(pxx/dxLimit) );
            y0 = dyLimit * ( 0.5 + (int) Math.floor(pyy/dyLimit) );
            sx0 = pxx*ww;
            sy0 = pyy*ww;
            nn0 = ww;
            if ( ww>0 ) {
                miny0 = yy;
                maxy0 = yy;
            } else {
                miny0 = Double.POSITIVE_INFINITY;
                maxy0 = Double.NEGATIVE_INFINITY;
            }


        }

        if ( nn0>0 ) {
            ax0 = sx0 / nn0;
            ay0 = sy0 / nn0;
            xbuilder.putValue( points, xlog ? Math.exp(ax0) : ax0 );
            ybuilder.putValue( points, ylog ? Math.exp(ay0) : ay0 );
            yminbuilder.putValue( points, miny0 );
            ymaxbuilder.putValue( points, maxy0 );
            wbuilder.putValue( points, nn0 );
            points++;
        }

        MutablePropertyDataSet yds= ybuilder.getDataSet();
        MutablePropertyDataSet xds= xbuilder.getDataSet();

        Map<String,Object> xprops= DataSetUtil.getProperties(x);
        if ( xprops.containsKey( QDataSet.CADENCE ) ) xprops.put( QDataSet.CADENCE, xLimit );
        if ( xprops.containsKey( QDataSet.CACHE_TAG ) ) xprops.put( QDataSet.CACHE_TAG, null );
        DataSetUtil.putProperties( xprops, xds );

        Map<String,Object> yprops= DataSetUtil.getProperties(y);
        yprops.put( QDataSet.DEPEND_0, xds );
        DataSetUtil.putProperties( yprops, yds );
        yminbuilder.putProperty( QDataSet.UNITS, SemanticOps.getUnits(y) );
        ymaxbuilder.putProperty( QDataSet.UNITS, SemanticOps.getUnits(y) );
        
        yds.putProperty( QDataSet.DEPEND_0, xds );
        yds.putProperty( QDataSet.WEIGHTS, wbuilder.getDataSet() );

        //TODO: this should probably be BIN_PLUS, BIN_MINUS
        yds.putProperty( QDataSet.DELTA_MINUS, Ops.subtract( yds, yminbuilder.getDataSet() ) );
        yds.putProperty( QDataSet.DELTA_PLUS, Ops.subtract( ymaxbuilder.getDataSet(), yds ) );

        logger.log( Level.FINE, "time to reduce2D({0} records -> {1} records) (ms): {2}", new Object[] { ds.length(), yds.length(), System.currentTimeMillis()-t0 } );
        logger.entering("Reduction", "reduce2D");
        
        return yds;

    }

    /**
     * reduce the buckshot scatter data by laying it out on a 2-D hexgrid and
     * accumulating the hits to each cell.  This has not been thoroughly verified.
     * @param ds rank1 Y(X)
     * @param z null or data to average
     * @return rank 2 ds containing frequency of occurrence for each bin, with DEPEND_0=xxx and DEPEND_1=yyy.
     * @see org.das2.qds.ops.Ops#histogram2d(org.das2.qds.QDataSet, org.das2.qds.QDataSet, int[], org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     * @throws IllegalArgumentException when the units cannot be converted
     * @see https://cran.r-project.org/web/packages/hexbin/vignettes/hexagon_binning.pdf
     * 
     */
    public static QDataSet hexbin( QDataSet ds, QDataSet z ) {
        logger.entering("Reduction", "hexbin");
        if ( ds.rank()!=1 && !Ops.isBundle(ds) ) {
            throw new IllegalArgumentException("ds.rank() must be 1");
        }
        
        QDataSet xx= SemanticOps.xtagsDataSet(ds);
        QDataSet yy= SemanticOps.ytagsDataSet(ds);
        
        QDataSet xr= Ops.extent(xx);
        QDataSet yr= Ops.multiply( Ops.extent(yy), (3/Math.sqrt(3)) );
        
        QDataSet xxx= Ops.linspace( xr.value(0), xr.value(1), 100 );
        QDataSet yyy1= Ops.linspace( yr.value(0), yr.value(1), 100 );
        double dy= yyy1.value(1)-yyy1.value(0);
        yyy1= Ops.linspace( yr.value(0)-dy/4, yr.value(1)-dy/4, 100 );
        QDataSet yyy2= Ops.linspace( yr.value(0)+dy/4, yr.value(1)+dy/4, 100 );
        
        double ymin1= yyy1.value(0);
        double ymin2= yyy2.value(0);
        double xmin= xxx.value(0);
        double xspace= xxx.value(1) - xxx.value(0);
        double yspace= yyy1.value(1) - yyy1.value(0);
        
        int nx= xxx.length();
        int ny= yyy1.length();
                
        IDataSet result= IDataSet.createRank2(nx*2,ny);
        QDataSet ww= SemanticOps.weightsDataSet(yy);
        
        UnitsConverter ucx= SemanticOps.getUnitsConverter( xx,xxx );
        UnitsConverter ucy= SemanticOps.getUnitsConverter( yy,yyy1 );
        
        boolean xlog= false;
        boolean ylog= false;
        
        DDataSet S;
        if ( z==null ) {
            z= Ops.ones(xx.length());
            S= null;
        } else {
            S= DDataSet.createRank2(nx*2,ny);
        }
        
        for ( int i=0; i<ds.length(); i++ ) {
            if ( ww.value(i)>0 ) {
                double x= ucx.convert( xx.value(i) );
                double y= ucy.convert( yy.value(i) );
                int ix= (int)( xlog ? (Math.log10(x)-xmin)/xspace : (x-xmin)/xspace );
                int iy1= (int)( ylog ? (Math.log10(y)-ymin1)/yspace : (y-ymin1)/yspace );
                int iy2= (int)( ylog ? (Math.log10(y)-ymin2)/yspace : (y-ymin2)/yspace );
                if ( ix>=0 && ix<nx ) {
                    if ( iy1>=0 && iy1<ny ) {
                        if ( iy2>=0 && iy2<ny ) {
                            double d1= Math.pow(x-xxx.value(ix),2) + Math.pow( y-yyy1.value(iy1), 2 );
                            double d2= Math.pow(x-(xxx.value(ix)+xspace/2),2) + Math.pow( y-yyy2.value(iy2), 2 );
                            if ( d1<d2 ) {
                                result.addValue( ix*2, iy1, 1 );
                                if ( S!=null ) S.addValue( ix*2, iy1, z.value(i) );
                            } else {
                                result.addValue( ix*2+1, iy2, 1 );
                                if ( S!=null ) S.addValue( ix*2+1, iy2, z.value(i) );
                            }
                        } else {
                            result.addValue( ix*2, iy1, 1 );
                            if ( S!=null ) S.addValue( ix*2, iy1, z.value(i) );
                        }
                    } else if ( iy2>=0 && iy2<ny ) {
                        result.addValue( ix*2+1, iy2, 1 );
                        if ( S!=null ) S.addValue( ix*2+1, iy2, z.value(i) );
                    }
                }
            }
        }
        
        WritableDataSet xxxx= Ops.zeros(xxx.length()*2);
        WritableDataSet yyyy= Ops.zeros(xxx.length()*2,yyy1.length());
        
        for ( int i=0; i<xxx.length(); i++ ) {
            xxxx.putValue( i*2, xxx.value(i) );
            xxxx.putValue( i*2+1, xxx.value(i)+xspace/2);
            for ( int j=0; j<yyy1.length(); j++ ) {
                yyyy.putValue( i*2, j, yyy1.value(j) );
                yyyy.putValue( i*2+1, j, yyy2.value(j) );
            }
        }
        
        if ( S!=null ) {
            MutablePropertyDataSet r= (MutablePropertyDataSet)Ops.divide( S, result );
            r.putProperty( QDataSet.DEPEND_0, xxxx );
            r.putProperty( QDataSet.DEPEND_1, yyyy );
            r.putProperty( QDataSet.WEIGHTS, result );
            logger.exiting("Reduction", "hexbin");
            return r;
        } else {
            result.putProperty( QDataSet.DEPEND_0, xxxx );
            result.putProperty( QDataSet.DEPEND_1, yyyy );
            logger.exiting("Reduction", "hexbin");
            return result;
        }
        
    }
    
    
    /**
     * reduce the buckshot scatter data by laying it out on a 2-D grid and
     * accumulating the hits to each cell.  Written originally to support 
     * SeriesRenderer, to replace the "200000 point limit" warning.
     * @param ds rank1 Y(X)
     * @param xxx rank1 dataset describes the bins, which must be uniformly linearly spaced, or log spaced.  Uses SCALE_TYPE property.
     * @param yyy rank1 dataset describes the bins, which must be uniformly linearly spaced, or log spaced.
     * @return rank 2 ds containing frequency of occurrence for each bin, with DEPEND_0=xxx and DEPEND_1=yyy.
     * @see org.das2.qds.ops.Ops#histogram2d(org.das2.qds.QDataSet, org.das2.qds.QDataSet, int[], org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     * @throws IllegalArgumentException when the units cannot be converted
     */
    public static QDataSet histogram2D( QDataSet ds, QDataSet xxx, QDataSet yyy ) {
        logger.entering("Reduction", "histogram2D");
        if ( ds.rank()!=1 ) {
            throw new IllegalArgumentException("ds.rank() must be 1");
        }
        if ( xxx.length()<2 ) {
            throw new IllegalArgumentException("xxx.length() must be at least 2");
        }
        if ( yyy.length()<2 ) {
            throw new IllegalArgumentException("yyy.length() must be at least 2");
        }
        
        boolean xlog= QDataSet.VALUE_SCALE_TYPE_LOG.equals( xxx.property(QDataSet.SCALE_TYPE) );
        boolean ylog= QDataSet.VALUE_SCALE_TYPE_LOG.equals( yyy.property(QDataSet.SCALE_TYPE) );
        double xspace= xlog ? Math.log10(xxx.value(1))-Math.log10(xxx.value(0)) : xxx.value(1)-xxx.value(0);
        double yspace= ylog ? Math.log10(yyy.value(1))-Math.log10(yyy.value(0)) : yyy.value(1)-yyy.value(0);
        double xmin= xlog ? Math.log10(xxx.value(0)) - xspace/2 : xxx.value(0) - xspace/2;
        double ymin= ylog ? Math.log10(yyy.value(0)) - yspace/2 : yyy.value(0) - yspace/2;
        int nx= xxx.length();
        int ny= yyy.length();
        
        IDataSet result= IDataSet.createRank2(nx,ny);
        QDataSet xx= SemanticOps.xtagsDataSet(ds);
        QDataSet yy= SemanticOps.ytagsDataSet(ds);
        QDataSet ww= SemanticOps.weightsDataSet(ds);
        
        UnitsConverter ucx= SemanticOps.getUnitsConverter( xx,xxx );
        UnitsConverter ucy= SemanticOps.getUnitsConverter( yy,yyy );
        
        for ( int i=0; i<ds.length(); i++ ) {
            if ( ww.value(i)>0 ) {
                double x= ucx.convert( xx.value(i) );
                double y= ucy.convert( yy.value(i) );
                int ix= (int)( xlog ? (Math.log10(x)-xmin)/xspace : (x-xmin)/xspace );
                int iy= (int)( ylog ? (Math.log10(y)-ymin)/yspace : (y-ymin)/yspace );
                if ( ix>=0 && ix<nx && iy>=0 && iy<ny ) {
                    result.addValue( ix, iy, 1 );
                }
            }
        }
        result.putProperty( QDataSet.DEPEND_0, xxx );
        result.putProperty( QDataSet.DEPEND_1, yyy );
        logger.exiting("Reduction", "histogram2D");
        return result;
    }
    
    private static Datum calculateNextX( Datum x, Datum xLimit ) {
        Datum nx;
        if ( UnitsUtil.isTimeLocation( x.getUnits() ) ) {
            Datum t0= x.subtract( Units.us2000.createDatum(0) );
            nx= Units.us2000.createDatum(0).add(t0).add( DatumUtil.modp( t0, xLimit ) );
        } else {
            nx= x.add( DatumUtil.modp( x, xLimit ) );
        }
        if ( nx.equals(x) ) nx= nx.add(xLimit);
        return nx;
    }
        

    /**
     * reduce the data.  This is needed to implement reducex so that high-rank 
     * datasets can be reduced as they are read in.  TODO: make streaming version of this.
     * @param ds
     * @param xLimit
     * @param object
     * @return 
     */
    private static QDataSet reduceRankN(QDataSet ds, Datum xLimit) {
        
        QDataSet oneRecord= ds.slice(0);
        int[] oneRecordQube= DataSetUtil.qubeDims(oneRecord);
        DDataSet sss= DDataSet.create( oneRecordQube );
        DDataSet nnn= DDataSet.create( oneRecordQube );
        Datum xNext= null;
        
        QDataSet xds= SemanticOps.xtagsDataSet(ds);
        
        DataSetBuilder resultSBuilder;
        DataSetBuilder resultNBuilder;
        DataSetBuilder resultxBuilder;
        
        switch ( ds.rank() ) {
            case 3: 
                resultSBuilder= new DataSetBuilder( ds.rank(), ds.length()/10, oneRecordQube[0], oneRecordQube[1] );
                resultNBuilder= new DataSetBuilder( ds.rank(), ds.length()/10, oneRecordQube[0], oneRecordQube[1] );
                break;
            case 4:
                resultSBuilder= new DataSetBuilder( ds.rank(), ds.length()/10, oneRecordQube[0], oneRecordQube[1], oneRecordQube[2] );
                resultNBuilder= new DataSetBuilder( ds.rank(), ds.length()/10, oneRecordQube[0], oneRecordQube[1], oneRecordQube[2] );
                break;
            default:
                throw new IllegalArgumentException("rank not supported: "+ds.rank() );
        }
        resultxBuilder= new DataSetBuilder( 1, ds.length()/10 );
        
        for ( int icurrent= 0; icurrent<ds.length(); icurrent++ ) {
            Datum x= DataSetUtil.asDatum(xds.slice(icurrent));
            QDataSet rec= ds.slice(icurrent);
            if ( xNext==null ) {
                xNext= calculateNextX( x, xLimit );
            }
            if ( x.ge( xNext ) ) {
                resultSBuilder.nextRecord( Ops.divide( sss, nnn ) );
                resultNBuilder.nextRecord( nnn );
                resultxBuilder.nextRecord( xNext.subtract(xLimit.divide(2) ) );
                xNext= xNext.add( xLimit );
                sss= DDataSet.create( oneRecordQube );
                nnn= DDataSet.create( oneRecordQube );
            }             
            QDataSet n= Ops.valid( rec );
            sss.addValues( rec, n );
            nnn.addValues( n, n );
        }
     
        if ( xNext==null ) {
            throw new IllegalArgumentException("this should not happen");
        }
        
        resultSBuilder.nextRecord( Ops.divide( sss, nnn ) );
        resultNBuilder.nextRecord( nnn );
        resultxBuilder.nextRecord( xNext.subtract(xLimit.divide(2) ) );        

        Map<String,Object> props= DataSetUtil.getDimensionProperties( ds, null );
        for ( Map.Entry<String,Object> en: props.entrySet() ) {
            resultSBuilder.putProperty( en.getKey(), en.getValue() );
        }
        
        Map<String,Object> xprops= DataSetUtil.getDimensionProperties( xds, null );
        for ( Map.Entry<String,Object> en: xprops.entrySet() ) {
            resultxBuilder.putProperty( en.getKey(), en.getValue() );
        }
        resultxBuilder.putProperty( QDataSet.CADENCE, DataSetUtil.asDataSet(xLimit) );
        
        resultSBuilder.putProperty( QDataSet.DEPEND_0, resultxBuilder.getDataSet() );
        resultSBuilder.putProperty( QDataSet.WEIGHTS, resultNBuilder.getDataSet() );
        
        DDataSet resultDs= resultSBuilder.getDataSet();
        
        return resultDs;
    }
}
