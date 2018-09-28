/* File: TableAverageRebinner.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on November 5, 2003, 10:31 AM
 *      by Edward West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.das2.dataset;

import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.datum.Datum;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.UnitsUtil;
import org.das2.DasException;
import org.das2.system.DasLogger;
import java.util.logging.*;
import org.das2.datum.UnitsConverter;
import org.das2.qds.DataSetUtil;
import org.das2.qds.DDataSet;
import org.das2.qds.JoinDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.RankZeroDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.ops.Ops;
//import org.das2.qds.util.AsciiFormatter;

/**
 * DataSetRebinner implementing either bi-linear interpolation in blocks of 4 points, or nearest neighbor interpolation by
 * grabbing close points, or no interpolation at all..  Points the land on the same pixel are averaged together.
 * @author  Edward West
 */
public class AverageTableRebinner implements DataSetRebinner {

    private static final Logger logger = DasLogger.getLogger(DasLogger.DATA_OPERATIONS_LOG);
    /**
     * Holds value of property interpolate.
     */
    private boolean interpolate = true;
    private boolean enlargePixels = true;

    public static enum Interpolate {
        None, Linear, NearestNeighbor, BinXInterpY;
    }

    /** Creates a new instance of TableAverageRebinner */
    public AverageTableRebinner() {
    }

    private static QDataSet getRank1Tags( QDataSet xds ) {
        QDataSet result;
        if ( xds.rank()==2 && SemanticOps.isBins(xds) ) {
            result= Ops.reduceMean( xds, 1 );
        } else {
            result= xds;
        }
        return result;
    }
    
    private boolean cadenceCheck = true;

    public static final String PROP_CADENCECHECK = "cadenceCheck";

    public boolean isCadenceCheck() {
        return cadenceCheck;
    }

    public void setCadenceCheck(boolean cadenceCheck) {
        boolean oldCadenceCheck = this.cadenceCheck;
        this.cadenceCheck = cadenceCheck;
        propertyChangeSupport.firePropertyChange(PROP_CADENCECHECK, oldCadenceCheck, cadenceCheck);
    }

    
    
    /**
     * rebin the data, using the interpolate control to define the interpolation between measurements.  Data that fall into the
     * same pixel are always averaged in the linear space, regardless of interpolation method.
     * @param ds rank 2 table or rank 3 join of tables.  New: rank 2 bundle of (X,Y,Z)
     * @param ddX
     * @param ddY
     * @return rank 2 table with one row/column per screen pixel.
     * @throws IllegalArgumentException
     * @throws DasException
     */
    @Override
    public QDataSet rebin( QDataSet ds, RebinDescriptor ddX, RebinDescriptor ddY ) throws IllegalArgumentException, DasException {
        logger.entering( "org.das2.dataset.AverageTableRebinner", "rebin" );
        if (ds == null) {
            throw new NullPointerException("null data set");
        }
        boolean bundle= false;
        
        if ( ! SemanticOps.isTableDataSet(ds) ) {
            if ( SemanticOps.isBundle(ds) ) {
                bundle=true;
            } else {
                QDataSet zds= (QDataSet) ds.property( QDataSet.PLANE_0 );
                if ( zds==null ) {
                    throw new IllegalArgumentException("Data set must be an instanceof TableDataSet or Bundle: " + ds.getClass().getName());
                } else {
                    ds= Ops.bundle( SemanticOps.xtagsDataSet(ds), ds, zds );
                    bundle=true;
                }
            }
        }

        QDataSet tds = (QDataSet) ds;
        int rank= tds.rank();

        if ( rank==2 ) { // make it into a rank 3 table
            JoinDataSet tdsx= new JoinDataSet(3);
            tdsx.join(tds);
            tds= tdsx;
        }

        QDataSet weights = SemanticOps.weightsDataSet( ds );

        QDataSet tds1= tds.slice(0);

        QDataSet xds= getRank1Tags( SemanticOps.xtagsDataSet(tds1) );
        QDataSet yds= getRank1Tags( SemanticOps.ytagsDataSet(tds1) );
        Units xunits= SemanticOps.getUnits( xds );
        QDataSet zds;
        
        if ( ddX != null && tds.length() > 0 ) {
            UnitsConverter xc= xunits.getConverter(ddX.getUnits());
            QDataSet bounds= SemanticOps.bounds(ds);
            double start = xc.convert( bounds.value(0,0) );
            double end = xc.convert( bounds.value(0,1) );
            DatumRange dr= DatumRangeUtil.union( ddX.binStop(ddX.numberOfBins()-1),ddX.binStart(0));
            if (start > dr.max().doubleValue(ddX.getUnits()) ) {
                throw new NoDataInIntervalException("data starts after range");
            } else if (end < dr.min().doubleValue(ddX.getUnits())) {
                throw new NoDataInIntervalException("data ends before range");
            }
        }

        int nx = (ddX == null ? tds1.length() : ddX.numberOfBins());
        int ny;
        if ( ddY == null ) {
            if ( SemanticOps.isBundle(ds) && !SemanticOps.isSimpleTableDataSet(ds) ) throw new IllegalArgumentException("not supported, must specify ddY bins");
            ny= tds1.length(0);
        } else {
            ny= ddY.numberOfBins();
        }

        if ( ddY==null && rank!=2 ) {
            throw new IllegalArgumentException("ddY was null but there was rank 3 dataset");
        }

        logger.log(Level.FINEST, "Allocating rebinData and rebinWeights: {0} x {1}", new Object[]{nx, ny});

        double[][] rebinData = new double[nx][ny];
        double[][] rebinWeights = new double[nx][ny];

        // average all the measurements that fall onto the same pixel together.
        if ( bundle ) {
            averageBundle( tds, weights, rebinData, rebinWeights, ddX, ddY, interpolateType );
            zds= SemanticOps.getDependentDataSet(tds1);

        } else {
            average( tds, weights, rebinData, rebinWeights, ddX, ddY, interpolateType );
//            try {
//                ArrayDataSet tdsc= ArrayDataSet.copy(tds.slice(0));
//                tdsc.putProperty( QDataSet.DEPEND_0, Ops.collapse1((QDataSet)tdsc.property(QDataSet.DEPEND_0)));
//                TableUtil.dumpToBinaryStream( (TableDataSet) DataSetAdapter.createLegacyDataSet(tdsc), new FileOutputStream("/tmp/foo.d2s") );
//            } catch ( IOException ex ) {
//                ex.printStackTrace();
//                        
//            }
            if (interpolate) { // I think these calculate the interpolated value at the edge.  Note there's a bug in here...
                doBoundaries2RL(tds, weights, rebinData, rebinWeights, ddX, ddY, interpolateType);
                doBoundaries2TB(tds, weights, rebinData, rebinWeights, ddX, ddY, interpolateType);
                doCorners(tds, weights, rebinData, rebinWeights, ddX, ddY, interpolateType);
            }
            zds= tds1;
        }

        if (interpolate) {
            Datum xTagWidth = getXTagWidth(xunits,ds);
            
            if ( xTagWidth.value()<0 ) xTagWidth= xTagWidth.multiply(-1);

            RankZeroDataSet yTagWidth0;
            if ( bundle ) {
                yTagWidth0= DataSetUtil.guessCadenceNew( yds, zds );
            } else {
                if ( tds.rank()<3 ) {
                    QDataSet yds1= yds;
                    if ( yds1.rank()>1 ) yds1= yds1.slice(0); //TODO: rank 2 yds.
                    yTagWidth0= DataSetUtil.guessCadenceNew( yds1, null );
                } else {
                    QDataSet yds1= SemanticOps.ytagsDataSet( tds.slice(0) );
                    if ( yds1.rank()>1 ) yds1= yds1.slice(0);
                    yTagWidth0= DataSetUtil.guessCadenceNew( yds1, null );
                    for ( int i=1;i<tds.length(); i++ ) {
                        yds1= SemanticOps.ytagsDataSet( tds.slice(i) );
                        if ( yds1.rank()>1 ) yds1= yds1.slice(0);
                        yTagWidth0= DataSetUtil.courserCadence( yTagWidth0, DataSetUtil.guessCadenceNew( yds1, null ) );
                    }
                }
            }

            RankZeroDataSet yTagWidthQ= yTagWidth0;
            Datum yTagWidth = yTagWidthQ==null ? null : DataSetUtil.asDatum( yTagWidthQ );
            if ( yTagWidth!=null && yTagWidth.value()<0 ) yTagWidth= yTagWidth.multiply(-1);

            if ( cadenceCheck==false ) yTagWidth= null;

            if (ddY != null) {
                fillInterpolateY(rebinData, rebinWeights, ddY, yTagWidth, interpolateType);
            }
            
            if (ddX != null) {
                fillInterpolateXNew(rebinData, rebinWeights, ddX, xTagWidth, cadenceCheck, interpolateType);
            }

        } else if (enlargePixels) {
            enlargePixels(rebinData, rebinWeights);
        }

        // copy the data into rank 2 dataset.
        DDataSet result= DDataSet.createRank2( nx, ny );
        DDataSet weightResult= DDataSet.createRank2( nx, ny );
        for ( int i=0; i<nx; i++ ) {
            for ( int j=0; j<ny; j++ ) {
                result.putValue( i, j, rebinData[i][j] );
                weightResult.putValue( i, j, rebinWeights[i][j] );
            }
        }

        RebinDescriptor.putDepDataSet( ds, result, ddX, ddY );
        result.putProperty( QDataSet.WEIGHTS, weightResult );

        logger.exiting( "org.das2.dataset.AverageTableRebinner", "rebin" );
        
        return result;
    }

    /**
     * For legacy code reasons, we need to come up with one cadence for all
     * data.  Recently the code would just look at the cadence of one of the modes,
     * (the last it looks like), and would use that.  This will at least return
     * the highest cadence of any of the modes.
     *
     * See  http://sourceforge.net/p/autoplot/bugs/1398/
     * @param xunits the units of the xtags, or the offset units.
     * @param tds the data.
     * @return the cadence for the entire set, and zero if it cannot be established.
     */
    private static Datum getXTagWidth( Units xunits, QDataSet tds ) {
        xunits= xunits.getOffsetUnits();
        QDataSet xds;
        if ( tds.rank()==3 ) {
            Datum cadence= xunits.createDatum( -1 * Double.MAX_VALUE );
            for ( int i=0; i<tds.length(); i++ ) {
                xds= (QDataSet) tds.slice(i).property(QDataSet.DEPEND_0);
                if ( xds==null ) {
                    return xunits.createDatum(1);
                } else if ( xds.length()>2 ) {
                    QDataSet r= DataSetUtil.guessCadenceNew( xds, null );
                    if ( r==null ) return xunits.getOffsetUnits().createDatum(0);
                    Datum rd= DataSetUtil.asDatum(r);
                    cadence= cadence.gt( rd ) ? cadence : rd;
                }
            }
            if ( cadence.value()<0 ) {
                return xunits.createDatum( Double.MAX_VALUE );
            } else {
                return cadence;
            }
        } else {
            xds= SemanticOps.xtagsDataSet(tds);
            if ( xds==null ) {
                return xunits.createDatum(1);
            } else if ( xds.length()>2 ) {
                QDataSet r= DataSetUtil.guessCadenceNew( xds, null );
                if ( r==null ) return xunits.getOffsetUnits().createDatum(0);
                Datum rd= DataSetUtil.asDatum(r);
                return rd;
            } else {
                return xunits.createDatum( Double.MAX_VALUE );
            }
        }
    }
    
    /** return the cadence of the data.
     * @param xds the x tags of the data.
     * @param tds1 the data, where we look for fill values.
     * @return the cadence of the data (never null)
     */
    private static Datum getXTagWidth( QDataSet xds, QDataSet tds1 ) {
        Datum xTagWidth;
        if ( xds.length()>1 ) {
            Datum d= SemanticOps.guessXTagWidth( xds, tds1 );
            if ( d==null ) {
                logger.warning("failed to guessXTagWidth");
                Units xunits= SemanticOps.getUnits(xds).getOffsetUnits();
                xTagWidth= xunits.createDatum(Double.MAX_VALUE);
                return xTagWidth;
                //d= SemanticOps.guessXTagWidth( xds, tds1 );
            } else {
                xTagWidth= d; //d.multiply(0.9);
            }
        } else {
            QDataSet xTagWidthDs= (RankZeroDataSet) xds.property( QDataSet.CADENCE ); // note these were once doubles, but this is not supported here.
            if (xTagWidthDs!=null) {
                xTagWidth= org.das2.qds.DataSetUtil.asDatum(xTagWidthDs);
            } else {
                Units xunits= SemanticOps.getUnits(xds).getOffsetUnits();
                xTagWidth= xunits.createDatum(Double.MAX_VALUE);
            }
        }
        return xTagWidth;
    }


    /**
     * return the next or previous closest index.  For monotonic datasets,
     * this just calls org.virbo.dataset.DataSetUtil.getPreviousIndex or getNextIndex.
     * Otherwise, we scan the dataset.
     * @param xds the rank 1 dataset to search.
     * @param xx the reference value.
     * @param sign 1 for next closest index, -1 for the index of the value just less than xx.
     * @return -1 if there is no valid data, the index otherwise.
     */
    private static int getNextPrevIndex( QDataSet xds, Datum xx, int sign ) {
        if ( SemanticOps.isMonotonic(xds) ) {
            if ( sign<0 ) {
                return org.das2.qds.DataSetUtil.getPreviousIndex(xds, xx);
            } else {
                return org.das2.qds.DataSetUtil.getNextIndex(xds, xx);
            }
        } else {
            double best= Double.MAX_VALUE;
            int ibest= -1;
            double lookFor= xx.doubleValue( SemanticOps.getUnits(xds) );
            QDataSet wds= SemanticOps.weightsDataSet(xds);
            for ( int i=0; i<xds.length(); i++ ) {
                double check= sign*( xds.value(i)-lookFor );
                if ( wds.value(i)>0. && check>0 && check<best ) {
                    ibest= i;
                    best= check;
                }
            }
            return ibest;
        }
    }

    static boolean canInterpolate( DatumRange span, Datum xTagWidthLimit  ) {
        double xSampleWidth;
        double fudge= 1.001;
        if (UnitsUtil.isRatiometric(xTagWidthLimit.getUnits())) {
            double p = xTagWidthLimit.doubleValue(Units.logERatio);
            xSampleWidth = p * fudge;
            return Math.log( span.max().divide(span.min()).doubleValue(Units.dimensionless) ) < xSampleWidth;
        } else {
            double d = xTagWidthLimit.doubleValue( xTagWidthLimit.getUnits() );
            xSampleWidth = d * fudge;
            return span.width().doubleValue(xTagWidthLimit.getUnits() ) < xSampleWidth;
        }
    }
    
    static void doBoundaries2RL( QDataSet tds, QDataSet weights, 
        double[][] rebinData, double[][] rebinWeights, 
        RebinDescriptor ddX, RebinDescriptor ddY, Interpolate interpolateType) {

        if ( tds.rank()!=3 ) throw new IllegalArgumentException("rank 3 expected");

        for ( int itable= 0; itable<tds.length(); itable++ ) {

            QDataSet tds1= tds.slice(itable);

            QDataSet xds= getRank1Tags( SemanticOps.xtagsDataSet(tds1) );
            QDataSet yds= SemanticOps.ytagsDataSet(tds1);
            if ( yds.rank()==2 && SemanticOps.isBins(yds) ) {
                yds= getRank1Tags( yds );
            }
            QDataSet wds= SemanticOps.weightsDataSet(tds1);
            
            Units xunits = SemanticOps.getUnits( xds );
            Units yunits = SemanticOps.getUnits( yds );

            Datum xTagWidth= getXTagWidth( xds, tds1 );

            for (int i = 0; i < 2; i++) { // left, right
                int ix = i == 0 ? 0 : ddX.numberOfBins() - 1;
                Datum xx = i == 0 ? ddX.binCenter(0) : ddX.binCenter(ix);

                int i0 = getNextPrevIndex( xds, xx, -1 );
                int i1 = getNextPrevIndex( xds, xx, 1 );
                if ( i0==-1 || i1==-1 ) return;

                if ( i1 != i0 ) {

                    DatumRange dr = DatumRangeUtil.union(
                            xunits.createDatum( xds.value(i0) ),
                            xunits.createDatum( xds.value(i1) ) );
//                    try {
//                        if ( UnitsUtil.isTimeLocation( dr.getUnits() ) ) {
//                            System.err.println( "dr.width().gt( xTagWidth ) -> "+( dr.width().gt( xTagWidth ) ) );
//                            System.err.println( "  dr.width()=="+dr.width() );
//                            System.err.println( "  xtagwidth=="+xTagWidth );
//                        }
//                        dr.width().gt( xTagWidth );
//                    } catch ( InconvertibleUnitsException ex ) {
//                        dr.width().gt( xTagWidth );
//                    }
                    if ( canInterpolate( dr, xTagWidth ) ) {
                        double alpha = DatumRangeUtil.normalize(dr, xx);
                        if ( interpolateType==Interpolate.NearestNeighbor || interpolateType==Interpolate.BinXInterpY ) {
                            alpha= alpha < 0.5 ? 0.0 : 1.0;
                        }
                        int ny = ddY == null ? yds.length() : ddY.numberOfBins();
                        QDataSet yds1;
                        if ( yds.rank()==2 ) {
                            yds1= yds.slice(i0); //TODO: assumes .slice(i0)==.slice(i1)
                        } else {
                            yds1= yds;
                        }
                        for (int j = 0; j < yds1.length(); j++) {
                            int jj = ddY == null ? j : ddY.whichBin( yds1.value( j ), yunits );
                            if (jj >= 0 && jj < ny) {
                                if (rebinWeights[ix][jj] > 0.0) {
                                    continue;
                                }
                                if ( wds.value( i0, j ) * wds.value( i1, j ) == 0.) {
                                    continue;
                                }
                                rebinData[ix][jj] = (1 - alpha) * tds1.value( i0, j ) +
                                        alpha * tds1.value( i1, j );
                                rebinWeights[ix][jj] = 1.0;
                            }
                        }
                    }
                }
            }
        }

    }

    static void doBoundaries2TB( QDataSet tds, QDataSet weights, 
        double[][] rebinData, double[][] rebinWeights, 
        RebinDescriptor ddX, RebinDescriptor ddY, Interpolate interpolateType) {

        if (ddY == null) {
            return;
        }

        for (int itable = 0; itable < tds.length(); itable++) {

            QDataSet tds1= tds.slice(itable);

            QDataSet xds= SemanticOps.xtagsDataSet(tds1);
            QDataSet yds= SemanticOps.ytagsDataSet(tds1);
            if ( yds.rank()==2 ) {
                int islice= yds.length()/2;
                if ( yds.length()>0 && yds.length(islice)>0 && yds.value(islice,0)!=yds.value(islice,0) ) {
                    logger.warning("kludge assumes rank2 yds is repeating values");
                }

                yds= yds.slice(islice); //TODO: kludge assumes that all yds slices are repeated.
            }
            QDataSet wds= SemanticOps.weightsDataSet(tds1);

            Units yunits = SemanticOps.getUnits(yds);
            Units xunits = SemanticOps.getUnits(xds);


            for (int i = 0; i < 2; i++) {
                int iy = i == 0 ? 0 : ddY.numberOfBins() - 1;
                Datum yy = i == 0 ? ddY.binCenter(0) : ddY.binCenter(iy);

                int j0,j1;
                if ( SemanticOps.isMonotonic(yds) ) {
                    j0 = org.das2.qds.DataSetUtil.getPreviousIndex( yds, yy );
                    j1 = org.das2.qds.DataSetUtil.getNextIndex( yds, yy);
                } else {
                    QDataSet myds= Ops.multiply( yds, DataSetUtil.asDataSet(-1) );
                    if ( SemanticOps.isMonotonic(myds) ) {
                        j0 = org.das2.qds.DataSetUtil.getPreviousIndex( myds, yy );
                        j1 = org.das2.qds.DataSetUtil.getNextIndex( myds, yy);
                    } else {
                        //fo_k0_ees_1998011_v01.cdf
                        if ( Ops.total( SemanticOps.weightsDataSet(yds) )==0 ) return;
                        //throw new IllegalArgumentException("dataset tags must be increasing or decreasing");
                        continue; // we just return without interpolating.
                    }

                }

                if (j1 != j0) {

                    DatumRange dr;
                    dr =  DatumRangeUtil.union(
                            yunits.createDatum( yds.value(j0) ),
                            yunits.createDatum( yds.value(j1) ) );

                    if (ddY.isLog()) {
                        Units u = dr.getUnits();
                        double d = dr.min().doubleValue(u);
                        double d0 = Math.log(dr.min().doubleValue(u) / d);
                        double d1 = Math.log(dr.max().doubleValue(u) / d);
                        dr = new DatumRange(d0, d1, Units.logERatio);
                        yy = Units.logERatio.createDatum(Math.log(yy.doubleValue(u) / d));
                    // TODO: infinity
                    }
                    double alpha = DatumRangeUtil.normalize(dr, yy);
                    if ( interpolateType==Interpolate.NearestNeighbor ) {
                        alpha= alpha < 0.5 ? 0.0 : 1.0;
                    }
                    int nx = ddX.numberOfBins();
                    for (int ix = 0; ix < tds1.length(); ix++) {
                        int ii = ddX.whichBin( xds.value(ix), xunits);
                        if (ii >= 0 && ii < nx) {
                            if (rebinWeights[ii][iy] > 0.0) {
                                continue;
                            }
                            if ( wds.value(ix,j0) * wds.value(ix,j1) == 0.) {
                                continue;
                            }
                            rebinData[ii][iy] = (1 - alpha) * tds1.value( ix, j0 ) + alpha * tds1.value( ix, j1 );
                            rebinWeights[ii][iy] = 1.0;
                        }
                    }
                }
            }
        }
    }

    static void doCorners( QDataSet tds, QDataSet weights, 
        double[][] rebinData, double[][] rebinWeights, 
        RebinDescriptor ddX, RebinDescriptor ddY, Interpolate interpolateType) {
        if (ddY == null) {
            return;
        }

        for ( int itable=0; itable<tds.length(); itable++ ) {
            QDataSet tds1= tds.slice(itable);

            QDataSet xds= getRank1Tags( SemanticOps.xtagsDataSet(tds1) );
            QDataSet yds= SemanticOps.ytagsDataSet(tds1);
            if ( yds.length()==1 && yds.rank()==2 && tds1.length()>1 ) {
                yds= yds.slice(0); //TODO: kludge for RBSP.  Bad CDF file?  vap+cdfj:http://emfisis.physics.uiowa.edu/L1/2011/03/03/rbsp-a_HFR-spectra_emfisis-L1_20110303144809_v1.1.1.cdf?HFR_Spectra
            }
            if ( SemanticOps.isBins(yds) ) {
                yds= getRank1Tags( yds );
            }
            QDataSet wds= SemanticOps.weightsDataSet(tds1);

            Units yunits = SemanticOps.getUnits(yds);
            Units xunits = SemanticOps.getUnits(xds);

            Datum xTagWidth= getXTagWidth( xds, tds1);

            for (int i = 0; i < 2; i++) {
                int ix = i == 0 ? 0 : ddX.numberOfBins() - 1;
                Datum xx = ddX.binCenter(ix);

                int i0 = getNextPrevIndex( xds, xx, -1 );
                int i1 = getNextPrevIndex( xds, xx, 1 );
                if ( i0==-1 || i1==-1 ) {
                    continue;
                }
                if (i0 == i1) {
                    continue;
                }

                DatumRange xdr = DatumRangeUtil.union(
                            xunits.createDatum( xds.value(i0) ),
                            xunits.createDatum( xds.value(i1) ) );
                double xalpha = DatumRangeUtil.normalize(xdr, xx);
                if (interpolateType == Interpolate.NearestNeighbor || interpolateType==Interpolate.BinXInterpY ) {
                    xalpha = xalpha < 0.5 ? 0.0 : 1.0;
                }

                QDataSet yds1;
                for (int j = 0; j < 2; j++) {
                    if ( yds.rank()==2 ) {
                        yds1= yds.slice(i0);
                    } else {
                        yds1= yds;
                    }
                    int iy = j == 0 ? 0 : ddY.numberOfBins() - 1;
                    Datum yy = ddY.binCenter(iy);

                    int j0 = getNextPrevIndex( yds1, yy, -1 );
                    int j1 = getNextPrevIndex( yds1, yy, 1 );
                    if ( j0==-1 || j1==-1 ) continue;

                    if (j0 != j1) {
                        DatumRange ydr =  DatumRangeUtil.union(
                            yunits.createDatum( yds1.value(j0) ),
                            yunits.createDatum( yds1.value(j1) ) );

                        if ( canInterpolate( xdr, xTagWidth ) ) {
                            double yalpha = DatumRangeUtil.normalize(ydr, yy);
                            if (interpolateType == Interpolate.NearestNeighbor) {
                                yalpha = yalpha < 0.5 ? 0.0 : 1.0;
                            }
                            if (rebinWeights[ix][iy] > 0.0) {
                                continue;
                            }
                            if ( wds.value(i1, j1 ) *
                                    wds.value(i0, j0 ) *
                                    wds.value(i1, j0 ) *
                                    wds.value(i0, j1 ) == 0.) {
                                continue;
                            }
                            rebinData[ix][iy] =
                                    tds1.value(i1, j1 ) * xalpha * yalpha +
                                    tds1.value(i0, j0 ) * (1 - xalpha) * (1 - yalpha) +
                                    tds1.value(i1, j0 ) * xalpha * (1 - yalpha) +
                                    tds1.value(i0, j1 ) * (1 - xalpha) * yalpha;
                            rebinWeights[ix][iy] = 1.0;
                        }
                    }
                }
            }
        }
    }

    static void averageBundle( QDataSet tds, QDataSet weights, 
        double[][] rebinData, double[][] rebinWeights, 
        RebinDescriptor ddX, RebinDescriptor ddY,
        Interpolate interpolateType ) {
        
        QDataSet tds1= tds.slice(0);
        QDataSet xds= SemanticOps.xtagsDataSet( tds1 );
        QDataSet yds= SemanticOps.ytagsDataSet( tds1 );
        QDataSet zds= SemanticOps.getDependentDataSet( tds1 );

        QDataSet wds= SemanticOps.weightsDataSet( zds );

        Units yunits = SemanticOps.getUnits(yds);
        Units xunits = SemanticOps.getUnits(xds);

        QDataSet vyds= SemanticOps.weightsDataSet(yds);
        QDataSet vxds= SemanticOps.weightsDataSet(xds);

        double fill= -1e31;

        int nx= ddX.numberOfBins();
        int ny= ddY.numberOfBins();

        for (int i = 0; i < zds.length(); i++) {

            int ibinx,ibiny;

            if ( vxds.value(i)>0 ) {
                ibinx = ddX.whichBin( xds.value(i), xunits );
            } else {
                continue;
            }

            if ( vyds.value(i)>0 ) {
                ibiny = ddY.whichBin( yds.value(i), yunits );
            } else {
                continue;
            }


            double z = zds.value( i );
            double w = wds.value( i );

            if (ibiny >= 0 && ibiny < ny && ibinx >= 0 && ibinx < nx ) {
                if ( interpolateType==Interpolate.NearestNeighbor ) { // propogate fill points through average
                    if ( w==0 ) {
                        if ( rebinWeights[ibinx][ibiny]==0 ) {
                            rebinData[ibinx][ibiny] = fill;
                            rebinWeights[ibinx][ibiny] = 1;
                        }
                    } else {
                        if ( rebinWeights[ibinx][ibiny]==1 && rebinData[ibinx][ibiny]==fill ) {
                            rebinData[ibinx][ibiny] = z;
                            rebinWeights[ibinx][ibiny] = w;
                        } else {
                            rebinData[ibinx][ibiny] += z * w;
                            rebinWeights[ibinx][ibiny] += w;
                        }
                    }
                } else {
                    rebinData[ibinx][ibiny] += z * w;
                    rebinWeights[ibinx][ibiny] += w;
                }
            }
        }

        multiplyWeights( rebinData, rebinWeights, fill );
        
    }
    
    /**
     * average data that falls onto the same pixel location.
     * @param tds the dataset to average, either rank 2 table or rank 3 array of tables.
     * @param weights the weights for each measurement of the dataset.
     * @param rebinData output the averages, normalized by the weights
     * @param rebinWeights output the weights for each bin.
     * @param ddX describes the horizontal bins
     * @param ddY describes the vertical bins
     * @param interpolateType if NearestNeighbor, then we set weight=1.  Why? see http://autoplot.org/developer.spectrogram
     */
    static void average( QDataSet tds, QDataSet weights, 
        double[][] rebinData, double[][] rebinWeights, 
        RebinDescriptor ddX, RebinDescriptor ddY,
        Interpolate interpolateType) {
        int nTables;
        Units zunits;
        int nx, ny;

        if ( tds.rank()!=3 ) throw new IllegalArgumentException("rank 3 expected");

        zunits = SemanticOps.getUnits( tds );
        double fill= zunits.getFillDouble();
        
        nx = (ddX == null ? tds.length(0) : ddX.numberOfBins());
        ny = (ddY == null ? tds.length(0,0) : ddY.numberOfBins());

        nTables = tds.length();
        for (int iTable = 0; iTable < nTables; iTable++) {
            QDataSet tds1= tds.slice(iTable);
            QDataSet xds= getRank1Tags( SemanticOps.xtagsDataSet( tds1 ) );
            
            QDataSet yds= SemanticOps.ytagsDataSet( tds1 );
            if ( yds.length()==1 && yds.rank()==2 && tds1.length()>1 ) {
                yds= yds.slice(0); //TODO: kludge for RBSP.  Bad CDF file?  vap+cdfj:http://emfisis.physics.uiowa.edu/L1/2011/03/03/rbsp-a_HFR-spectra_emfisis-L1_20110303144809_v1.1.1.cdf?HFR_Spectra
            }
            if ( yds.rank()==2 && SemanticOps.isBins(yds) ) {
                yds= getRank1Tags( yds );
            }
            QDataSet wds= SemanticOps.weightsDataSet( tds1 );

            Units yunits = SemanticOps.getUnits(yds);
            Units xunits = SemanticOps.getUnits(xds);

            int[] ibiny = new int[tds1.length(0)];

            QDataSet vyds= SemanticOps.weightsDataSet(yds);
            QDataSet vxds= SemanticOps.weightsDataSet(xds);

            if ( yds.rank()==1 ) {    
                for (int j = 0; j < ibiny.length; j++) {
                    if (ddY != null) {
                        if ( vyds.value(j)>0 ) {
                            ibiny[j] = ddY.whichBin( yds.value(j), yunits );
                        } else {
                            ibiny[j] = -10000;
                        }
                    } else {
                        ibiny[j] = j;
                    }
                }
            }

            for (int i = 0; i < tds1.length(); i++) {
                if ( yds.rank()==2 ) {
                    for (int j = 0; j < ibiny.length; j++) {
                        if (ddY != null) {
                            if ( vyds.value(i,j)>0 ) {
                                ibiny[j] = ddY.whichBin( yds.value(i,j), yunits );
                            } else {
                                ibiny[j] = -10000;
                            }
                        } else {
                            ibiny[j] = j;
                        }
                    }
                }

                int ibinx;

                if (ddX != null) {
                    if ( vxds.value(i)>0 ) {
                        ibinx = ddX.whichBin( xds.value(i), xunits );
                    } else {
                        ibinx = -10000;
                    }
                } else {
                    ibinx = i;
                }

                if (ibinx >= 0 && ibinx < nx) {
                    for (int j = 0; j < tds1.length(i); j++) {
                        double z = tds1.value( i, j );
                        double w = wds.value( i, j );
                        if (ibiny[j] >= 0 && ibiny[j] < ny) {
                            if ( interpolateType==Interpolate.NearestNeighbor ) { // propogate fill points through average
                                if ( w==0 ) {
                                    if ( rebinWeights[ibinx][ibiny[j]]==0 ) {
                                        rebinData[ibinx][ibiny[j]] = fill;
                                        rebinWeights[ibinx][ibiny[j]] = 1;
                                    }
                                } else {
                                    if ( rebinWeights[ibinx][ibiny[j]]==1 && rebinData[ibinx][ibiny[j]]==fill ) {
                                        rebinData[ibinx][ibiny[j]] = z;
                                        rebinWeights[ibinx][ibiny[j]] = w;
                                    } else {
                                        rebinData[ibinx][ibiny[j]] += z * w;
                                        rebinWeights[ibinx][ibiny[j]] += w;
                                    }
                                }
                            } else {
                                if ( w>0 ) {
                                    rebinData[ibinx][ibiny[j]] += z * w;
                                    rebinWeights[ibinx][ibiny[j]] += w;
                                }
                            }
                        }
                    }
                }
            }
        }

        multiplyWeights( rebinData, rebinWeights, zunits.getFillDouble() );

    }

    private static void multiplyWeights(double[][] data, double[][] weights, double fill) {
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                if (weights[i][j] > 0.0) {
                    data[i][j] = data[i][j] / weights[i][j];
                } else {
                    data[i][j] = fill;
                }
            }
        }
    }

    //still used by AveragePeakTableRebinner
    //    final double[][] data, final double[][] weights, RebinDescriptor ddY, Datum yTagWidth, Interpolate interpolateType
    static void fillInterpolateX(final double[][] data, final double[][] weights, 
        final double[] xTags, double[] xTagMin, double[] xTagMax, 
        final double xSampleWidth, Interpolate interpolateType) {

        final int nx = xTags.length;
        final int ny = data[0].length;
        final int[] i1 = new int[nx];
        final int[] i2 = new int[nx];
        double a1;
        double a2;

        for (int j = 0; j < ny; j++) {
            int ii1 = -1;
            int ii2;
            for (int i = 0; i < nx; i++) {
                if (weights[i][j] > 0. && ii1 == (i - 1)) { // ho hum another valid point
                    i1[i] = -1;
                    i2[i] = -1;
                    ii1 = i;
                } else if (weights[i][j] > 0. && ii1 == -1) { // first valid point
                    i1[i] = -1;
                    i2[i] = -1;
                    ii1 = i;
                    if (interpolateType == Interpolate.NearestNeighbor) {
                        for (int jjj = 0; jjj < i; jjj++) {
                            i2[jjj] = ii1;
                        }
                    }
                } else if (weights[i][j] > 0. && ii1 < (i - 1)) { // bracketed a gap, interpolate
                    if (ii1 > -1) {
                        i1[i] = -1;
                        i2[i] = -1;
                        ii2 = i;
                        for (int ii = ii1 + 1; ii < i; ii++) {
                            i1[ii] = ii1;
                            i2[ii] = ii2;
                        }
                        ii1 = i;
                    }
                } else {
                    i1[i] = -1;
                    i2[i] = -1;
                }
            }
            if (interpolateType == Interpolate.NearestNeighbor && ii1 > -1) {
                for (int jjj = ii1; jjj < nx; jjj++) {
                    i1[jjj] = ii1;
                }
            }
            if (interpolateType == Interpolate.NearestNeighbor) {

                for (int i = 0; i < nx; i++) {
                    if ( i1[i] > -1 && i2[i] > -1 && ( xSampleWidth==0 || ( Math.abs(xTagMin[i2[i]] - xTagMax[i1[i]]) <= xSampleWidth * 1.5 ) ) ) {
                        int idx;
                        if (i1[i] == -1) {
                            if (i2[i] == -1) {
                                continue;
                            }
                            idx = i2[i];
                        } else if (i2[i] == -1) {
                            idx = i1[i];
                        } else {
                            a2 = ((xTags[i] - xTagMax[i1[i]]) / (xTagMin[i2[i]] - xTags[i1[i]]));
                            if (a2 < 0.5) {
                                idx = i1[i];
                            } else {
                                idx = i2[i];
                            }
                        }
                        data[i][j] = data[idx][j];
                        weights[i][j] = weights[idx][j];
                    }
                }
            } else {
                for (int i = 0; i < nx; i++) {
                    if (i1[i] > -1 && i2[i] > -1 && Math.abs(xTagMin[i2[i]] - xTagMax[i1[i]]) <= xSampleWidth * 1.5) {
                        a2 = ((xTags[i] - xTagMax[i1[i]]) / (xTagMin[i2[i]] - xTags[i1[i]]));
                        a1 = 1. - a2;
                        data[i][j] = data[i1[i]][j] * a1 + data[i2[i]][j] * a2;
                        weights[i][j] = weights[i1[i]][j] * a1 + weights[i2[i]][j] * a2; //approximate

                    }
                }
            }
        }
    }

    /**
     * interpolate the missing pixels by scanning rows, looking for point pairs where weights &gt; 0 and that are acceptably close.
     * @param data the data, not scaled by weight.
     * @param weights 0 means ignore, positive means valid
     * @param ddX
     * @param xTagWidth the nominal cadence between measurements.  This defines acceptably close, and we apply a fudge factor.
     * @param cadenceCheck check for gaps based on cadence
     * @param interpolateType if NearestNeighbor, then special actions can occur.
     */
    static void fillInterpolateXNew(final double[][] data, final double[][] weights, RebinDescriptor ddX, Datum xTagWidth, boolean cadenceCheck, Interpolate interpolateType) {

        final boolean noCadenceCheck= !cadenceCheck;
        
        final int ny = data[0].length;
        final int nx = ddX.numberOfBins();
        final int[] i1 = new int[nx];
        final int[] i2 = new int[nx];
        final double[] xTagTemp = new double[ddX.numberOfBins()];
        double a1;
        double a2;

        final double[] xTags = ddX.binCenters();
        final Units xTagUnits = ddX.getUnits();
        final boolean log = ddX.isLog();

        if (log) {
            for (int i = 0; i < nx; i++) {
                xTagTemp[i] = Math.log(xTags[i]);
            }
        } else {
            System.arraycopy(xTags, 0, xTagTemp, 0, nx);
        }

        double xSampleWidth;
        double fudge = 1.5 * 0.9; // 0.9 was removed from another code.
        
        boolean isNN= interpolateType == Interpolate.NearestNeighbor || interpolateType==Interpolate.BinXInterpY;
        
        if ( isNN ) {
            fudge = 1.01; 
        }
        if (xTagWidth == null) {
            double d = Double.MAX_VALUE / 4;  // avoid roll-over when *1.5
            xSampleWidth = d;
        } else {
            if (UnitsUtil.isRatiometric(xTagWidth.getUnits())) {
                double p = xTagWidth.doubleValue(Units.logERatio);
                xSampleWidth = p * fudge;
            } else {
                double d = xTagWidth.doubleValue(xTagUnits.getOffsetUnits());
                xSampleWidth = d * fudge;
            }
        }

        double pixelSize= ddX.binWidth();
        xSampleWidth= xSampleWidth+ pixelSize; // there's a bug where two close measurements can fall into bins where the centers are more than xSampleWidth apart, so add a pixel width fuzz here.
        
//        try {
//            new AsciiFormatter().formatToFile( "/home/jbf/tmp/weights.txt", weights );
//        } catch (IOException ex) {
//            Logger.getLogger(AverageTableRebinner.class.getName()).log(Level.SEVERE, null, ex);
//        }
        
        for (int j = 0; j < ny; j++) {
            int ii1 = -1;
            int ii2;
            for (int i = 0; i < nx; i++) {
                if (weights[i][j] > 0. && ii1 == (i - 1)) { // ho hum another valid point
                    i1[i] = -1;
                    i2[i] = -1;
                    ii1 = i;
                } else if (weights[i][j] > 0. && ii1 == -1) { // first valid point
                    i1[i] = -1;
                    i2[i] = -1;
                    ii1 = i;
                    if ( isNN ) {
                        for (int iii = 0; iii < i; iii++) {
                            i2[iii] = ii1;
                        }
                    }
                } else if (weights[i][j] > 0. && ii1 < (i - 1)) { // bracketed a gap, interpolate
                    if ((ii1 > -1)) {   // need restriction on Y gap size
                        i1[i] = -1;
                        i2[i] = -1;
                        ii2 = i;
                        for (int iii = i - 1; iii >= ii1; iii--) {
                            i1[iii] = ii1;
                            i2[iii] = ii2;
                        }
                        ii1 = i;
                    }
                } else {
                    i1[i] = -1;
                    i2[i] = -1;
                }
            }
            if ( isNN && ii1 > -1) {
                for (int iii = ii1; iii < nx; iii++) {
                    i1[iii] = ii1;
                }
            }
            if ( isNN ) {
                for (int i = 0; i < nx; i++) {
                    boolean doInterp; //TODO? Really, this is the name?  I think doGrow is better
                    if ( i1[i]!= -1 && i2[i] != -1) {
                        boolean doInterpR= noCadenceCheck || ( ( xTagTemp[i2[i]] - xTagTemp[i] ) < xSampleWidth/2 );
                        doInterp= doInterpR || ( xTagTemp[i] - xTagTemp[i1[i]] ) < xSampleWidth/2;
                        //doInterp= doInterp || ( xTagTemp[i2[i]]-xTagTemp[i1[i]] ) < xSampleWidth*2; // strange bit of code that is probably wrong.
                    } else {
                        //kludge for bug 000321
                        //doInterp= Math.min(i1[i] == -1 ? Double.MAX_VALUE : (xTagTemp[i] - xTagTemp[i1[i]]), i2[i] == -1 ? Double.MAX_VALUE : (xTagTemp[i2[i]] - xTagTemp[i])) < xSampleWidth / 2;
                        //kludge for bug 000321
                        if ( i1[i]==-1 && i2[i]==-1 ) {
                            doInterp= false;
                        } else {
                            if ( ddX.isLog() && !UnitsUtil.isRatiometric(xTagWidth.getUnits()) ) {
                                doInterp= false;
                            } else {
                                if ( i1[i]==-1 ) {
                                    doInterp= ( xTagTemp[i2[i]] - xTagTemp[i] ) < xSampleWidth/2;
                                } else {
                                    doInterp= ( xTagTemp[i] - xTagTemp[i1[i]] ) < xSampleWidth/2;
                                }
                            }
                        }
                    }
                    if ( doInterp ) {
                        int idx;
                        if (i1[i] == -1) {
                            idx = i2[i];
                        } else if (i2[i] == -1) {
                            idx = i1[i];
                        } else {
                            a2 = ((xTagTemp[i] - xTagTemp[i1[i]]) / (xTagTemp[i2[i]] - xTagTemp[i1[i]]));
                            if (a2 < 0.5) {
                                idx = i1[i];
                            } else {
                                idx = i2[i];
                            }
                        }
                        data[i][j] = data[idx][j];
                        weights[i][j] = weights[idx][j];
                    } 
                }
            } else {
                for (int i = 0; i < nx; i++) {
                    if ((i1[i] != -1) && ( noCadenceCheck || (xTagTemp[i2[i]] - xTagTemp[i1[i]]) < xSampleWidth || i2[i] - i1[i] == 2)) { //kludge for bug 000321
                        a2 = ((xTagTemp[i] - xTagTemp[i1[i]]) / (xTagTemp[i2[i]] - xTagTemp[i1[i]]));
                        a1 = 1. - a2;
                        data[i][j] = data[i1[i]][j] * a1 + data[i2[i]][j] * a2;
                        weights[i][j] = weights[i1[i]][j] * a1 + weights[i2[i]][j] * a2; //approximate
                    } 
                }
            }
        }
    }

    /**
     * interpolate the missing pixels by scanning columns, looking for point pairs where weights &gt; 0 and that are acceptably close.
     * @param data the data, not scaled by weight.
     * @param weights 0 means ignore, positive means valid
     * @param ddX
     * @param xTagWidth the nominal cadence between measurements.  This defines acceptably close, and we apply a fudge factor.
     * @param interpolateType if NearestNeighbor, then special actions can occur.
     */
    static void fillInterpolateY(final double[][] data, final double[][] weights, 
        RebinDescriptor ddY, Datum yTagWidth, Interpolate interpolateType) {

        final int nx = data.length;
        final int ny = ddY.numberOfBins();
        final int[] i1 = new int[ny];
        final int[] i2 = new int[ny];
        final double[] yTagTemp = new double[ddY.numberOfBins()];
        double a1;
        double a2;

        final double[] yTags = ddY.binCenters();
        final Units yTagUnits = ddY.getUnits();
        final boolean log = ddY.isLog();

        if (log) {
            for (int j = 0; j < ny; j++) {
                yTagTemp[j] = Math.log(yTags[j]);
            }
        } else {
            System.arraycopy(yTags, 0, yTagTemp, 0, ny);
        }

        boolean isNN= interpolateType == Interpolate.NearestNeighbor;
                
        double ySampleWidth;
        double fudge = 1.5 * 0.9; // 0.9 was removed from another code.
        if ( isNN ) {
            fudge = 1.01;
        }
        boolean ySampleWidthRatiometric= false;
        if (yTagWidth == null) {
            double d = Double.MAX_VALUE / 4;  // avoid roll-over when *1.5
            ySampleWidth = d;
        } else {
            if (UnitsUtil.isRatiometric(yTagWidth.getUnits())) {
                double p = yTagWidth.doubleValue(Units.logERatio);
                ySampleWidth = p * fudge;
                ySampleWidthRatiometric= true;
            } else {
                double d = yTagWidth.doubleValue(yTagUnits.getOffsetUnits());
                ySampleWidth = d * fudge;
            }
        }

        double pixelSize= ddY.binWidth();

        final double[] ySampleWidths= new double[ddY.numberOfBins()];
        for ( int j=0; j<ny; j++ ) {
            if ( ddY.isLog ) {
                if ( ySampleWidthRatiometric ) {
                    ySampleWidths[j]= ySampleWidth + pixelSize; 
                } else {
                    double l0= ddY.binCenter(j,yTagUnits)-ySampleWidth/2;
                    double l1= ddY.binCenter(j,yTagUnits)+ySampleWidth/2;
                    int il1= Math.min( ddY.whichBin( l1, yTagUnits ), ddY.numberOfBins()-1 );
                    int il0= Math.max( ddY.whichBin( l0, yTagUnits ), 0 );
                    ySampleWidths[j]= ( yTagTemp[ il1 ] - yTagTemp[ il0 ] ) + pixelSize;
                }
            } else {
                if ( yTagWidth==null ) {
                    ySampleWidths[j]= -1;
                } else {
                    if ( ySampleWidthRatiometric ) {
                        ySampleWidths[j]= ySampleWidth * ddY.binCenter( j, yTagUnits ) + pixelSize; // THIS IS A GUESS!                    
                    } else {
                        ySampleWidths[j]= ySampleWidth+ pixelSize; // there's a bug where two close measurements can fall into bins where the centers are more than xSampleWidth apart, so add a pixel width fuzz here.
                    }
                }
            }
        }
        
        for (int i = 0; i < nx; i++) {
            int ii1 = -1;
            int ii2;
            for (int j = 0; j < ny; j++) {
                if (weights[i][j] > 0. && ii1 == (j - 1)) { // ho hum another valid point

                    i1[j] = -1;
                    i2[j] = -1;
                    ii1 = j;
                } else if (weights[i][j] > 0. && ii1 == -1) { // first valid point

                    i1[j] = -1;
                    i2[j] = -1;
                    ii1 = j;
                    if ( isNN ) {
                        for (int jjj = 0; jjj < j; jjj++) {
                            i2[jjj] = ii1;
                        }
                    }
                } else if (weights[i][j] > 0. && ii1 < (j - 1)) { // bracketed a gap, interpolate

                    if ((ii1 > -1)) {   // need restriction on Y gap size

                        i1[j] = -1;
                        i2[j] = -1;
                        ii2 = j;
                        for (int jj = j - 1; jj >= ii1; jj--) {
                            i1[jj] = ii1;
                            i2[jj] = ii2;
                        }
                        ii1 = j;
                    }
                } else {
                    i1[j] = -1;
                    i2[j] = -1;
                }
            }
            if ( isNN && ii1 > -1) {
                for (int jjj = ii1; jjj < ny; jjj++) {
                    i1[jjj] = ii1;
                }
            }
            if ( isNN ) {
                for (int j = 0; j < ny; j++) {
                    boolean doInterp;
                    if ( i1[j]!= -1 && i2[j] != -1) {
                        boolean doInterpR= yTagWidth==null || ( (yTagTemp[i2[j]] - yTagTemp[j] ) < ySampleWidths[j] );
                        doInterp= doInterpR || ( yTagTemp[j] - yTagTemp[i1[j]] ) < ySampleWidths[j];
                        doInterp= doInterp || ( yTagTemp[i2[j]]-yTagTemp[i1[j]] ) < ySampleWidths[j];
                    } else {
                        //kludge for bug 000321
                        if ( ddY.isLog() && !UnitsUtil.isRatiometric(yTagUnits) ) {
                            doInterp= false;
                        } else {
                            if ( i1[j]==-1 && i2[j]==-1 ) {
                                doInterp= false;
                            } else if ( i1[j]==-1 ) {
                                doInterp= ( yTagTemp[i2[j]] - yTagTemp[j] ) < ySampleWidths[j]/2;
                            } else {
                                doInterp= ( yTagTemp[j] - yTagTemp[i1[j]] ) < ySampleWidths[j]/2;
                            }
                        }
                    }
                    if ( doInterp ) { 
                        int idx;
                        if (i1[j] == -1) {
                            idx = i2[j];
                        } else if (i2[j] == -1) {
                            idx = i1[j];
                        } else {
                            a2 = ((yTagTemp[j] - yTagTemp[i1[j]]) / (yTagTemp[i2[j]] - yTagTemp[i1[j]]));
                            if (a2 < 0.5) {
                                idx = i1[j];
                            } else {
                                idx = i2[j];
                            }
                        }
                        data[i][j] = data[i][idx];
                        weights[i][j] = weights[i][idx];

                    }
                    
                }
            } else {
                for (int j = 0; j < ny; j++) { //yunits on sample width
                    if ((i1[j] != -1) && ( yTagWidth==null || ( (yTagTemp[i2[j]] - yTagTemp[i1[j]]) < ySampleWidths[j] || i2[j] - i1[j] == 2) ) ) { //kludge for bug 000321
                        a2 = ((yTagTemp[j] - yTagTemp[i1[j]]) / (yTagTemp[i2[j]] - yTagTemp[i1[j]]));
                        a1 = 1. - a2;
                        data[i][j] = data[i][i1[j]] * a1 + data[i][i2[j]] * a2;
                        weights[i][j] = weights[i][i1[j]] * a1 + weights[i][i2[j]] * a2; //approximate

                    }
                }
            }
        }
    }

    private void enlargePixels(double[][] rebinData, double[][] rebinWeights) {
        int enlargeSize = 5;
        for (int aa = 0; aa < enlargeSize; aa++) {
            for (int ii = 0; ii < rebinData.length - 1; ii++) {
                for (int jj = 0; jj < rebinData[0].length; jj++) {
                    if (rebinWeights[ii][jj] == 0) {
                        rebinData[ii][jj] = rebinData[ii + 1][jj];
                        rebinWeights[ii][jj] = rebinWeights[ii + 1][jj];
                    }
                }
            }
            for (int ii = rebinData.length - 1; ii > 0; ii--) {
                for (int jj = 0; jj < rebinData[0].length; jj++) {
                    if (rebinWeights[ii][jj] == 0) {
                        rebinData[ii][jj] = rebinData[ii - 1][jj];
                        rebinWeights[ii][jj] = rebinWeights[ii - 1][jj];
                    }
                }
            }
            for (int jj = 0; jj < rebinData[0].length - 1; jj++) {
                for (int ii = 0; ii < rebinData.length; ii++) {
                    if (rebinWeights[ii][jj] == 0) {
                        rebinData[ii][jj] = rebinData[ii][jj + 1];
                        rebinWeights[ii][jj] = rebinWeights[ii][jj + 1];
                    }
                }
            }
            for (int jj = rebinData[0].length - 1; jj > 0; jj--) {
                for (int ii = 0; ii < rebinData.length; ii++) {
                    if (rebinWeights[ii][jj] == 0) {
                        rebinData[ii][jj] = rebinData[ii][jj - 1];
                        rebinWeights[ii][jj] = rebinWeights[ii][jj - 1];
                    }
                }
            }
        }
    }

    /**
     * "Interpolate" here simply means connecting the data points.
     * @return true indicates we should connect the data points.
     */
    public boolean isInterpolate() {
        return this.interpolate;
    }

    /**
     * "Interpolate" here simply means connecting the data points.
     * @param interpolate true indicates we should connect the data points.
     */
    public void setInterpolate(boolean interpolate) {
        this.interpolate = interpolate;
    }

    public void setEnlargePixels(boolean enlargePixels) {
        this.enlargePixels = enlargePixels;
    }

    public boolean isEnlargePixels() {
        return enlargePixels;
    }
    protected Interpolate interpolateType = Interpolate.Linear;
    public static final String PROP_INTERPOLATETYPE = "interpolateType";

    public Interpolate getInterpolateType() {
        return interpolateType;
    }

    public void setInterpolateType(Interpolate interpolateType) {
        Interpolate oldInterpolateType = this.interpolateType;
        this.interpolateType = interpolateType;
        propertyChangeSupport.firePropertyChange(PROP_INTERPOLATETYPE, oldInterpolateType, interpolateType);
    }
    private java.beans.PropertyChangeSupport propertyChangeSupport = new java.beans.PropertyChangeSupport(this);

    public void addPropertyChangeListener(java.beans.PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(java.beans.PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }
}
