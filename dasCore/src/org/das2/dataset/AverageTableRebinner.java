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
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.UnitsConverter;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.JoinDataSet;
import org.virbo.dataset.MutablePropertyDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.RankZeroDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dsops.Ops;

/**
 *
 * @author  Edward West
 */
public class AverageTableRebinner implements DataSetRebinner {

    private static Logger logger = DasLogger.getLogger(DasLogger.DATA_OPERATIONS_LOG);
    /**
     * Holds value of property interpolate.
     */
    private boolean interpolate = true;
    private boolean enlargePixels = true;

    public static enum Interpolate {

        None, Linear, NearestNeighbor
    }

    /** Creates a new instance of TableAverageRebinner */
    public AverageTableRebinner() {
    }

    public QDataSet rebin( QDataSet ds, RebinDescriptor ddX, RebinDescriptor ddY ) throws IllegalArgumentException, DasException {
        logger.finest("enter AverageTableRebinner.rebin");

        if (ds == null) {
            throw new NullPointerException("null data set");
        }
        if (! SemanticOps.isTableDataSet(ds) ) {
            throw new IllegalArgumentException("Data set must be an instanceof TableDataSet: " + ds.getClass().getName());
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

        QDataSet xds= SemanticOps.xtagsDataSet(tds1);
        QDataSet yds= SemanticOps.ytagsDataSet(tds1);
        Units xunits= SemanticOps.getUnits( xds );
        Units yunits= SemanticOps.getUnits( yds );

        UnitsConverter xc= xunits.getConverter(ddX.getUnits());

        if ( ddX != null && tds.length() > 0 ) {
            QDataSet bounds= SemanticOps.bounds(tds);
            double start = xc.convert( bounds.value(0,0) );
            double end = xc.convert( bounds.value(0,1) );
            if (start > ddX.binStop(ddX.numberOfBins()-1).doubleValue(ddX.getUnits()) ) {
                throw new NoDataInIntervalException("data starts after range");
            } else if (end < ddX.binStart(0).doubleValue(ddX.getUnits())) {
                throw new NoDataInIntervalException("data ends before range");
            }
        }

        int nx = (ddX == null ? tds1.length() : ddX.numberOfBins());
        int ny = (ddY == null ? tds1.length(0) : ddY.numberOfBins());

        if ( ddY==null && rank!=2 ) {
            throw new IllegalArgumentException("ddY was null but there was rank 3 dataset");
        }

        logger.finest("Allocating rebinData and rebinWeights: " + nx + " x " + ny);

        double[][] rebinData = new double[nx][ny];
        double[][] rebinWeights = new double[nx][ny];

        average( tds, weights, rebinData, rebinWeights, ddX, ddY );
        if (interpolate) { // I think these calculate the interpolated value at the edge.  Note there's a bug in here...
            doBoundaries2RL(tds, weights, rebinData, rebinWeights, ddX, ddY, interpolateType);
            doBoundaries2TB(tds, weights, rebinData, rebinWeights, ddX, ddY, interpolateType);
            doCorners(tds, weights, rebinData, rebinWeights, ddX, ddY, interpolateType);
        }

        double[] xTags;

        if (ddX != null) {
            xTags = ddX.binCenters();
        } else {
            xTags = new double[nx];
            for (int i = 0; i < nx; i++) {
                xTags[i] = xds.value( i );
            }
        }


        double[][] yTags;
        if (ddY != null) {
            yTags = new double[][]{ddY.binCenters()};
        } else {
            yTags = new double[1][ny];
            for (int j = 0; j < ny; j++) {
                yTags[0][j] = yds.value( j );  // TODO: rank 2
            }
        }

        if (this.interpolate) {
            Datum xTagWidth = getXTagWidth(xds, tds1);

            QDataSet yds1= yds.rank()==1 ? yds : yds.slice(0); //TODO: rank 2 y

            //double xTagWidthDouble = xTagWidth.doubleValue(ddX.getUnits().getOffsetUnits());
            Datum yTagWidth = DataSetUtil.asDatum( DataSetUtil.guessCadenceNew( yds1, null ) );

            if (ddX != null) {
                fillInterpolateXNew(rebinData, rebinWeights, ddX, xTagWidth, interpolateType);
            }
            if (ddY != null) {
                fillInterpolateY(rebinData, rebinWeights, ddY, yTagWidth, interpolateType);
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

        DDataSet xx= DDataSet.createRank1( ddX.numberOfBins() );
        for ( int i=0; i<xx.length(); i++ ) xx.putValue(i, ddX.binCenter(i,xunits));
        xx.putProperty( QDataSet.UNITS, xunits );

        MutablePropertyDataSet yy;
        if ( ddY!=null ) {
            DDataSet yyy= DDataSet.createRank1( ddY.numberOfBins() );
            for ( int i=0; i<yyy.length(); i++ ) yyy.putValue(i, ddY.binCenter(i,yunits));
            yyy.putProperty( QDataSet.UNITS, yunits );
            yy= yyy;
        } else {
            yy= DataSetOps.makePropertiesMutable( yds );
        }

        for ( String s: DataSetUtil.dimensionProperties() ) {
            if ( ds.property(s)!=null ) result.putProperty(s,ds.property(s));
            if ( xds.property(s)!=null ) xx.putProperty(s,xds.property(s));
            if ( yds.property(s)!=null ) yy.putProperty(s,yds.property(s));
        }
        if (ddX != null) {
            xx.putProperty(QDataSet.CADENCE, DataSetUtil.asDataSet(ddX.binWidthDatum()) );
        }
        if (ddY != null) {
            yy.putProperty(QDataSet.CADENCE, DataSetUtil.asDataSet(ddY.binWidthDatum()) );
        }

        result.putProperty( QDataSet.DEPEND_0, xx );
        result.putProperty( QDataSet.DEPEND_1, yy );
        result.putProperty( QDataSet.WEIGHTS_PLANE, weightResult );

        logger.finest("done, AverageTableRebinner.rebin");
        return result;
    }

    private static Datum getXTagWidth( QDataSet xds, QDataSet tds1 ) {
        Datum xTagWidth;
        if ( xds.length()>1 ) {
            Datum d= SemanticOps.guessXTagWidth( xds, tds1 );
            if ( d==null ) {
                System.err.println("failed to guessXTagWidth");
                Units xunits= SemanticOps.getUnits(xds).getOffsetUnits();
                xTagWidth= xunits.createDatum(Double.MAX_VALUE);
                return xTagWidth;
                //d= SemanticOps.guessXTagWidth( xds, tds1 );
            } else {
                xTagWidth= d.multiply(0.9);
            }
        } else {
            QDataSet xTagWidthDs= (RankZeroDataSet) xds.property( QDataSet.CADENCE ); // note these were once doubles, but this is not supported here.
            if (xTagWidthDs!=null) {
                xTagWidth= org.virbo.dataset.DataSetUtil.asDatum(xTagWidthDs);
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
                return org.virbo.dataset.DataSetUtil.getPreviousIndex(xds, xx);
            } else {
                return org.virbo.dataset.DataSetUtil.getNextIndex(xds, xx);
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


    static void doBoundaries2RL( QDataSet tds, QDataSet weights, double[][] rebinData, double[][] rebinWeights, RebinDescriptor ddX, RebinDescriptor ddY, Interpolate interpolateType) {

        if ( tds.rank()!=3 ) throw new IllegalArgumentException("rank 3 expected");

        for ( int itable= 0; itable<tds.length(); itable++ ) {

            QDataSet tds1= tds.slice(itable);

            QDataSet xds= SemanticOps.xtagsDataSet(tds1);
            QDataSet yds= SemanticOps.ytagsDataSet(tds1);
            QDataSet wds= SemanticOps.weightsDataSet(tds1);
            
            Units xunits = SemanticOps.getUnits( xds );
            Units yunits = SemanticOps.getUnits( yds );

            Datum xTagWidth= getXTagWidth( xds, tds1 ).multiply(0.9);

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
                    try {
                        dr.width().gt( xTagWidth );
                    } catch ( InconvertibleUnitsException ex ) {
                        dr.width().gt( xTagWidth );
                    }
                    if ( dr.width().gt( xTagWidth ) ) {
                        double alpha = DatumRangeUtil.normalize(dr, xx);
                        if ( interpolateType==Interpolate.NearestNeighbor ) {
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

    static void doBoundaries2TB( QDataSet tds, QDataSet weights, double[][] rebinData, double[][] rebinWeights, RebinDescriptor ddX, RebinDescriptor ddY, Interpolate interpolateType) {

        if (ddY == null) {
            return;
        }

        for (int itable = 0; itable < tds.length(); itable++) {

            QDataSet tds1= tds.slice(itable);

            QDataSet xds= SemanticOps.xtagsDataSet(tds1);
            QDataSet yds= SemanticOps.ytagsDataSet(tds1);
            if ( yds.rank()==2 ) {
                yds= yds.slice(0); //TODO: kludge assumes that all yds slices are repeated.
                if ( yds.length()>0 && yds.length(0)>0 && yds.value(0,0)!=yds.value(1,0) ) {
                    System.err.println("kludge assumes rank2 yds is repeating values");
                }
            }
            QDataSet wds= SemanticOps.weightsDataSet(tds1);

            Units yunits = SemanticOps.getUnits(yds);
            Units xunits = SemanticOps.getUnits(xds);


            for (int i = 0; i < 2; i++) {
                int iy = i == 0 ? 0 : ddY.numberOfBins() - 1;
                Datum yy = i == 0 ? ddY.binCenter(0) : ddY.binCenter(iy);

                int j0,j1;
                if ( SemanticOps.isMonotonic(yds) ) {
                    j0 = org.virbo.dataset.DataSetUtil.getPreviousIndex( yds, yy );
                    j1 = org.virbo.dataset.DataSetUtil.getNextIndex( yds, yy);
                } else {
                    QDataSet myds= Ops.multiply( yds, DataSetUtil.asDataSet(-1) );
                    if ( SemanticOps.isMonotonic(myds) ) {
                        j0 = org.virbo.dataset.DataSetUtil.getPreviousIndex( myds, yy );
                        j1 = org.virbo.dataset.DataSetUtil.getNextIndex( myds, yy);
                    } else {
                        throw new IllegalArgumentException("dataset tags must be increasing or decreasing");
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

    static void doCorners( QDataSet tds, QDataSet weights, double[][] rebinData, double[][] rebinWeights, RebinDescriptor ddX, RebinDescriptor ddY, Interpolate interpolateType) {
        if (ddY == null) {
            return;
        }

        for ( int itable=0; itable<tds.length(); itable++ ) {
            QDataSet tds1= tds.slice(itable);

            QDataSet xds= SemanticOps.xtagsDataSet(tds1);
            QDataSet yds= SemanticOps.ytagsDataSet(tds1);
            if ( yds.rank()==2 ) {
                yds= yds.slice(0);
            }
            QDataSet wds= SemanticOps.weightsDataSet(tds1);

            Units yunits = SemanticOps.getUnits(yds);
            Units xunits = SemanticOps.getUnits(xds);

            Datum xTagWidth= getXTagWidth( xds, tds1).multiply(0.9);

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
                if (interpolateType == Interpolate.NearestNeighbor) {
                    xalpha = xalpha < 0.5 ? 0.0 : 1.0;
                }


                for (int j = 0; j < 2; j++) {
                    int iy = j == 0 ? 0 : ddY.numberOfBins() - 1;
                    Datum yy = ddY.binCenter(iy);

                    int j0 = getNextPrevIndex( yds, yy, -1 );
                    int j1 = getNextPrevIndex( yds, yy, 1 );
                    if ( j0==-1 || j1==-1 ) continue;

                    if (j0 != j1) {
                        DatumRange ydr =  DatumRangeUtil.union(
                            yunits.createDatum( yds.value(j0) ),
                            yunits.createDatum( yds.value(j1) ) );

                        if (xdr.width().lt(xTagWidth)) {
                            DatumRange xdr1 = new DatumRange(ddX.binCenter(0), ddX.binCenter(ddX.numberOfBins() - 1));
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

    static void average( QDataSet tds, QDataSet weights, double[][] rebinData, double[][] rebinWeights, RebinDescriptor ddX, RebinDescriptor ddY) {
        double[] ycoordinate;
        int nTables;
        Units zunits;
        int nx, ny;

        if ( tds.rank()!=3 ) throw new IllegalArgumentException("rank 3 expected");

        zunits = SemanticOps.getUnits( tds );

        nx = (ddX == null ? tds.length(0) : ddX.numberOfBins());
        ny = (ddY == null ? tds.length(0,0) : ddY.numberOfBins());


        if (ddY != null) {
            ycoordinate = ddY.binCenters();
        } else {
            QDataSet yds= SemanticOps.ytagsDataSet( tds.slice(0) );
            ycoordinate = new double[yds.length()];
            for (int j = 0; j < ycoordinate.length; j++) {
                ycoordinate[j] = yds.value(0);
            }
        }

        nTables = tds.length();
        for (int iTable = 0; iTable < nTables; iTable++) {
            QDataSet tds1= tds.slice(iTable);
            QDataSet xds= SemanticOps.xtagsDataSet( tds1 );
            QDataSet yds= SemanticOps.ytagsDataSet( tds1 );
            QDataSet wds= SemanticOps.weightsDataSet( tds1 );

            Units yunits = SemanticOps.getUnits(yds);
            Units xunits = SemanticOps.getUnits(xds);

            int[] ibiny = new int[tds1.length(0)];
            for (int j = 0; j < ibiny.length; j++) {
                if (ddY != null) {
                    ibiny[j] = ddY.whichBin( yds.value(j), yunits );
                } else {
                    ibiny[j] = j;
                }
            }
            for (int i = 0; i < tds1.length(); i++) {
                int ibinx;

                if (ddX != null) {
                    ibinx = ddX.whichBin( xds.value(i), xunits );
                } else {
                    ibinx = i;
                }

                int ny1= yds.rank()==1 ? yds.length() : yds.length(i);

                if (ibinx >= 0 && ibinx < nx) {
                    for (int j = 0; j < ny1; j++) {
                        try {
                            double z = tds1.value( i, j );
                            double w = wds.value( i, j );
                            if (ibiny[j] >= 0 && ibiny[j] < ny) {
                                rebinData[ibinx][ibiny[j]] += z * w;
                                rebinWeights[ibinx][ibiny[j]] += w;
                            }
                        } catch ( Exception e ) {
                            System.err.println("here");
                        }
                        double z = tds1.value( i, j );
                        double w = wds.value( i, j );
                        if (ibiny[j] >= 0 && ibiny[j] < ny) {
                            rebinData[ibinx][ibiny[j]] += z * w;
                            rebinWeights[ibinx][ibiny[j]] += w;
                        }
                    }
                }
            }
        }
        multiplyWeights( rebinData, rebinWeights, zunits.getFillDouble() );
    }

    private final static double linearlyInterpolate(int i0, double z0, int i1, double z1, int i) {
        double r = ((double) (i - i0)) / (i1 - i0);
        return z0 + r * (z1 - z0);
    }

    private final static void multiplyWeights(double[][] data, double[][] weights, double fill) {
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
    static void fillInterpolateX(final double[][] data, final double[][] weights, final double[] xTags, double[] xTagMin, double[] xTagMax, final double xSampleWidth, Interpolate interpolateType) {

        final int nx = xTags.length;
        final int ny = data[0].length;
        final int[] i1 = new int[nx];
        final int[] i2 = new int[nx];
        double a1;
        double a2;

        for (int j = 0; j < ny; j++) {
            int ii1 = -1;
            int ii2 = -1;
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
                    if ( i1[i] > -1 && i2[i] > -1 && (xTagMin[i2[i]] - xTagMax[i1[i]]) <= xSampleWidth * 1.5 ) {

                        int idx = -1;
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
                    if (i1[i] > -1 && i2[i] > -1 && (xTagMin[i2[i]] - xTagMax[i1[i]]) <= xSampleWidth * 1.5) {
                        a2 = ((xTags[i] - xTagMax[i1[i]]) / (xTagMin[i2[i]] - xTags[i1[i]]));
                        a1 = 1. - a2;
                        data[i][j] = data[i1[i]][j] * a1 + data[i2[i]][j] * a2;
                        weights[i][j] = weights[i1[i]][j] * a1 + weights[i2[i]][j] * a2; //approximate

                    }
                }
            }
        }
    }

    static void fillInterpolateXNew(final double[][] data, final double[][] weights, RebinDescriptor ddX, Datum xTagWidth, Interpolate interpolateType) {

        final int ny = data[0].length;
        final int nx = ddX.numberOfBins();
        final int[] i1 = new int[nx];
        final int[] i2 = new int[nx];
        final double[] xTagTemp = new double[ddX.numberOfBins()];
        double a1;
        double a2;

        final double[] xTags = ddX.binCenters();
        final Units yTagUnits = ddX.getUnits();
        final boolean log = ddX.isLog();

        if (log) {
            for (int i = 0; i < nx; i++) {
                xTagTemp[i] = Math.log(xTags[i]);
            }
        } else {
            for (int i = 0; i < nx; i++) {
                xTagTemp[i] = xTags[i];
            }
        }

        double xSampleWidth;
        double fudge = 1.5;
        if (interpolateType == Interpolate.NearestNeighbor) {
            fudge = 1.1;
        }
        if (xTagWidth == null) {
            double d = Double.MAX_VALUE / 4;  // avoid roll-over when *1.5
            xSampleWidth = d;
        } else {
            if (UnitsUtil.isRatiometric(xTagWidth.getUnits())) {
                double p = xTagWidth.doubleValue(Units.logERatio);
                xSampleWidth = p * fudge;
            } else {
                double d = xTagWidth.doubleValue(yTagUnits.getOffsetUnits());
                xSampleWidth = d * fudge;
            }
        }

        for (int j = 0; j < ny; j++) {
            int ii1 = -1;
            int ii2 = -1;
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
            if (interpolateType == Interpolate.NearestNeighbor && ii1 > -1) {
                for (int iii = ii1; iii < nx; iii++) {
                    i1[iii] = ii1;
                }
            }
            if (interpolateType == Interpolate.NearestNeighbor) {
                for (int i = 0; i < nx; i++) {
                    boolean doInterp;
                    if ( i1[i]!= -1 && i2[i] != -1) {
                        doInterp= ( xTagTemp[i2[i]]-xTagTemp[i1[i]] ) < xSampleWidth*2;
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
                        //doInterp= ((i1[i] != -1) && ((xTagTemp[i2[i]] - xTagTemp[i1[i]]) < xSampleWidth || i2[i] - i1[i] == 2));
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
                    if ((i1[i] != -1) && ((xTagTemp[i2[i]] - xTagTemp[i1[i]]) < xSampleWidth || i2[i] - i1[i] == 2)) { //kludge for bug 000321

                        a2 = ((xTagTemp[i] - xTagTemp[i1[i]]) / (xTagTemp[i2[i]] - xTagTemp[i1[i]]));
                        a1 = 1. - a2;
                        data[i][j] = data[i1[i]][j] * a1 + data[i2[i]][j] * a2;
                        weights[i][j] = weights[i1[i]][j] * a1 + weights[i2[i]][j] * a2; //approximate

                    }
                }
            }
        }
    }


    static void fillInterpolateY(final double[][] data, final double[][] weights, RebinDescriptor ddY, Datum yTagWidth, Interpolate interpolateType) {

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
            for (int j = 0; j < ny; j++) {
                yTagTemp[j] = yTags[j];
            }
        }

        double ySampleWidth;
        double fudge = 1.5;
        if (interpolateType == Interpolate.NearestNeighbor) {
            fudge = 1.1;
        }
        if (yTagWidth == null) {
            double d = Double.MAX_VALUE / 4;  // avoid roll-over when *1.5
            ySampleWidth = d;
        } else {
            if (UnitsUtil.isRatiometric(yTagWidth.getUnits())) {
                double p = yTagWidth.doubleValue(Units.logERatio);
                ySampleWidth = p * fudge;
            } else {
                double d = yTagWidth.doubleValue(yTagUnits.getOffsetUnits());
                ySampleWidth = d * fudge;
            }
        }

        for (int i = 0; i < nx; i++) {
            int ii1 = -1;
            int ii2 = -1;
            for (int j = 0; j < ny; j++) {
                if (weights[i][j] > 0. && ii1 == (j - 1)) { // ho hum another valid point

                    i1[j] = -1;
                    i2[j] = -1;
                    ii1 = j;
                } else if (weights[i][j] > 0. && ii1 == -1) { // first valid point

                    i1[j] = -1;
                    i2[j] = -1;
                    ii1 = j;
                    if (interpolateType == Interpolate.NearestNeighbor) {
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
            if (interpolateType == Interpolate.NearestNeighbor && ii1 > -1) {
                for (int jjj = ii1; jjj < ny; jjj++) {
                    i1[jjj] = ii1;
                }
            }
            if (interpolateType == Interpolate.NearestNeighbor) {
                for (int j = 0; j < ny; j++) {
                    boolean doInterp;
                    if ( i1[j]!= -1 && i2[j] != -1) {
                        doInterp= ( yTagTemp[i2[j]]-yTagTemp[i1[j]] ) < ySampleWidth*2;
                    } else {
                        //kludge for bug 000321
                        if ( ddY.isLog() && !UnitsUtil.isRatiometric(yTagUnits) ) {
                            doInterp= false;
                        } else {
                            if ( i1[j]==-1 && i2[j]==-1 ) {
                                doInterp= false;
                            } else if ( i1[j]==-1 ) {
                                doInterp= ( yTagTemp[i2[j]] - yTagTemp[j] ) < ySampleWidth/2;
                            } else {
                                doInterp= ( yTagTemp[j] - yTagTemp[i1[j]] ) < ySampleWidth/2;
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
                    if ((i1[j] != -1) && ((yTagTemp[i2[j]] - yTagTemp[i1[j]]) < ySampleWidth || i2[j] - i1[j] == 2)) { //kludge for bug 000321

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
     * Getter for property interpolate.
     * @return Value of property interpolate.
     */
    public boolean isInterpolate() {
        return this.interpolate;
    }

    /**
     * Setter for property interpolate.
     * @param interpolate New value of property interpolate.
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
