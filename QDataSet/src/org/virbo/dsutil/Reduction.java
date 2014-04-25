/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.dsutil;

import java.util.Map;
import org.das2.datum.Datum;
import org.das2.datum.Units;
import org.das2.datum.UnitsConverter;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.IDataSet;
import org.virbo.dataset.MutablePropertyDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dsops.Ops;

/**
 * Reduction is set of static methods for reducing data.
 * @author jbf
 */
public class Reduction {
    /**
     * return a converter for differences.  If dst units are specified,
     * then explicitly this is the target.
     * @param src
     * @param dst
     * @return
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
     * See org.qstream.filter.MinMaxReduceFilter.  
     * This is basically a copy of that code.
     * @param ds a rank1 or rank2 waveform dataset.
     * @param xLimit the target resolution, result will be finer than this, if possible.
     * @return either the original dataset when there is no reduction to be done, or a series data set with bins (deltas for now).
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
     * Because of high-resolution magnetometer data, this is extended to support this data type.
     * 
     * @param ds rank 1 or rank 2 dataset.  Must have DEPEND_0 (presently) and be a qube.  If this is null, then the result is null.
     * @param xLimit the size of the bins or null to indicate no limit.
     * @return the reduced dataset, or null if the input dataset was null.
     */
    public static QDataSet reducex( QDataSet ds, QDataSet xLimit ) {
        long t0= System.currentTimeMillis();

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
        } else {
            throw new IllegalArgumentException("only rank 1 and rank 2 datasets");
        }

        QDataSet x= (QDataSet) ds.property( QDataSet.DEPEND_0 );
        if ( x==null ) x= new org.virbo.dataset.IndexGenDataSet(ds.length());

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
        double ax0 = 0;
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
        int inCount = 0;

        QDataSet wds= DataSetUtil.weightsDataSet(ds);

        int i=0;

        double fill= Double.NaN;

        while ( i<x.length() ) {
            inCount++;

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
                        ax0 = sx0 / nx;
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
                ax0 = sx0 / nx;
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

        Map<String,Object> xprops= DataSetUtil.getProperties(x);
        if ( xprops.containsKey( QDataSet.CADENCE ) ) xprops.put( QDataSet.CADENCE, xLimit );
        if ( xprops.containsKey( QDataSet.CACHE_TAG ) ) xprops.put( QDataSet.CACHE_TAG, null );
        DataSetUtil.putProperties( xprops, xds );

        Map<String,Object> yprops= DataSetUtil.getProperties(ds);
        yprops.put( QDataSet.DEPEND_0, xds );
        DataSetUtil.putProperties( yprops, result );
        yminbuilder.putProperty( QDataSet.UNITS, SemanticOps.getUnits(result) );
        ymaxbuilder.putProperty( QDataSet.UNITS, SemanticOps.getUnits(result) );

        result.putProperty( QDataSet.DEPEND_0, xds );
        result.putProperty( QDataSet.WEIGHTS, wbuilder.getDataSet() );

        result.putProperty( QDataSet.DELTA_MINUS, Ops.subtract( result, yminbuilder.getDataSet() ) );
        result.putProperty( QDataSet.DELTA_PLUS, Ops.subtract( ymaxbuilder.getDataSet(), result ) );

        System.err.println( String.format( "time to reducex(%d records -> %d records) (ms): %d", ds.length(), result.length(), System.currentTimeMillis()-t0) );

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
     * @return
     */
    public static QDataSet reduce2D( QDataSet ds, QDataSet xLimit, QDataSet yLimit ) {

        long t0= System.currentTimeMillis();

        DataSetBuilder xbuilder= new DataSetBuilder( 1, 1000 );
        DataSetBuilder ybuilder= new DataSetBuilder( 1, 1000 );
        DataSetBuilder yminbuilder= new DataSetBuilder( 1, 1000 );
        DataSetBuilder ymaxbuilder= new DataSetBuilder( 1, 1000 );
        DataSetBuilder wbuilder= new DataSetBuilder( 1, 1000 ); // weights to go here

        QDataSet x= (QDataSet) ds.property( QDataSet.DEPEND_0 );
        if ( x==null ) x= new org.virbo.dataset.IndexGenDataSet(ds.length());
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

        System.err.println( String.format( "time to reduce2D(%d points) (ms): %d", ds.length(), System.currentTimeMillis()-t0) );

        return yds;

    }
    
    /**
     * reduce the buckshot scatter data by laying it out on a 2-D grid and
     * accumulating the hits to each cell.  Written originally to support 
     * SeriesRenderer, to replace the "200000 point limit" warning.
     * @param ds rank1 Y(X)
     * @param xxx rank1 dataset describes the bins, which must be uniformly linearly spaced, or log spaced.  Uses SCALE_TYPE property.
     * @param yyy rank1 dataset describes the bins, which must be uniformly linearly spaced, or log spaced.
     * @return rank 2 ds containing frequency of occurrence for each bin, with DEPEND_0=xxx and DEPEND_1=yyy.
     */
    public static QDataSet histogram2D( QDataSet ds, QDataSet xxx, QDataSet yyy ) {
        if ( ds.rank()!=1 ) {
            throw new IllegalArgumentException("ds.rank() must be 1");
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
        
        return result;
    }
}
