/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.dataset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.Units;
import org.das2.qds.QDataSet;
import org.das2.qds.WritableDataSet;
import org.das2.qds.ops.Ops;
import org.das2.datum.Datum;
import org.das2.datum.LoggerManager;
import org.das2.qds.DataSetOps;
import org.das2.qds.SemanticOps;
import org.das2.qds.examples.Schemes;

/**
 *
 * @author leiffert
 */
public class ScatterRebinner implements DataSetRebinner {

    private static final Logger logger = LoggerManager.getLogger("das2.data.rebinner");

    @Override
    public QDataSet rebin(QDataSet zds, RebinDescriptor ddX, RebinDescriptor ddY, RebinDescriptor ddZ) {
        // throws IllegalArgumentException, DasException {

        WritableDataSet result = Ops.zeros(ddX.numberOfBins(), ddY.numberOfBins());

        ddX.setOutOfBoundsAction(RebinDescriptor.MINUSONE);
        ddY.setOutOfBoundsAction(RebinDescriptor.MINUSONE);

        QDataSet xds = null, yds = null;

        if (Schemes.isBundleDataSet(zds)) {
            logger.fine("is bundle");
            xds = DataSetOps.slice1(zds, 0);
            yds = DataSetOps.slice1(zds, 1);
            zds = DataSetOps.slice1(zds, zds.length(0) - 1);
        } else {
            if (!(zds.property(QDataSet.DEPEND_0) == null)) {
                xds = (QDataSet) zds.property(QDataSet.DEPEND_0);
            } else {
                xds = Ops.findgen(zds.length());
            }
            if (!(zds.property(QDataSet.DEPEND_1) == null)) {
                yds = (QDataSet) zds.property(QDataSet.DEPEND_1);
            } else {
                if (Schemes.isLegacyXYZScatter(zds)) {
                    yds = zds;
                    zds = (QDataSet) yds.property(QDataSet.PLANE_0);
                } else {
                    yds = Ops.findgen(zds.length());
                }
            }

        }
        
        if ( xds==null ) throw new IllegalArgumentException("unable to identify x data set");
        if ( yds==null ) throw new IllegalArgumentException("unable to identify y data set");
        
        int nx = ddX.numberOfBins() - 1;
        int ny = ddY.numberOfBins() - 1;

        //Used to keep track when multiple data points fall in the same bin. 
        int[][] nPointsInBin = new int[nx + 1][ny + 1];
        double[][] cumMovingAverage = new double[nx + 1][ny + 1];

        int xB = 0, yB = 0;

        Units xdsUnits;
        Units ydsUnits;

        xdsUnits = SemanticOps.getUnits(xds);
        ydsUnits = SemanticOps.getUnits(yds);

        switch (zds.rank()) {
            case 0:
                break;
            case 1:
                for (int i = 0; i < zds.length(); i++) {
                    switch (xds.rank()) {
                        case 0:
                            xB = ddX.whichBin(xds.value(), xdsUnits);
                            break;
                        case 1:
                            xB = ddX.whichBin(xds.value(i), xdsUnits);
                            break;
                        case 2:
                            logger.fine(" Don't know what to do with rank 2 Depend 0 datasets yet.");
                            break;
                        default:
                            break;
                    }
                    switch (yds.rank()) {
                        case 0:
                            yB = ddY.whichBin(yds.value(), ydsUnits);
                            break;
                        case 1:
                            yB = ddY.whichBin(yds.value(i), ydsUnits);
                            break;
                        case 2:
                            logger.fine(" Don't know what to do with rank 2 Depend 0 datasets yet.");
                            break;
                        default:
                            break;
                    }

                    if (xB == -1 || yB == -1) {
                        continue;
                    }
                    // Store cumulative average incase multiple datapoints are in a single bin.
                    cumMovingAverage[xB][yB] = (zds.value(i) + nPointsInBin[xB][yB] * cumMovingAverage[xB][yB]) / (nPointsInBin[xB][yB] + 1);
                    nPointsInBin[xB][yB]++;
                    result.putValue(xB, yB, cumMovingAverage[xB][yB]);
                }
                break;
            case 2:
                for (int i = 0; i < zds.length(); i++) {
                    for (int j = 0; j < zds.length(i); j++) {
                        switch (xds.rank()) {
                            case 0:
                                xB = ddX.whichBin(xds.value(), xdsUnits);
                                break;
                            case 1:
                                xB = ddX.whichBin(xds.value(i), xdsUnits);
                                break;
                            case 2:
                                logger.fine(" Don't know what to do with rank 2 dataset here.");
                                break;
                            default:
                                break;
                        }
                        switch (yds.rank()) {
                            case 0:
                                yB = ddY.whichBin(yds.value(), ydsUnits);
                                break;
                            case 1:
                                yB = ddY.whichBin(yds.value(j), ydsUnits);
                                break;
                            case 2:
                                logger.fine(" Don't know what to do with rank 2 dataset here.");
                                break;
                            default:
                                break;
                        }
                        if (xB == -1 || yB == -1) {
                            continue;
                        }

                        // Store cumulative average incase multiple datapoints are in a single bin.
                        cumMovingAverage[xB][yB] = (zds.value(i, j) + nPointsInBin[xB][yB] * cumMovingAverage[xB][yB]) / (nPointsInBin[xB][yB] + 1);
                        nPointsInBin[xB][yB]++;
                        result.putValue(xB, yB, cumMovingAverage[xB][yB]);

                    }
                }
                break;
            case 3:
                logger.fine("Does not support rank 3 datasets.");
                break;
            default:
                logger.fine("Rank is not 0, 1, 2 or 3 or not recognized.");
                break;
        }

        // The hardBins are used to define a sharp edge rectngle of bins around a datapoint when an area should
        // have the same value as a datapoint. 
        // The softRad are used from the dataset property cadence and define the boundary to be interpolated to.
        int xHardBinPlus = 0, xHardBinMinus = 0, yHardBinPlus = 0, yHardBinMinus = 0, xSoftRad = 0, ySoftRad = 0;
        Datum xDat = ddX.binWidthDatum();

        // Used to sort all the distances between points when no cadence property is set. 
        List<Integer> xCadencesToSort = new ArrayList<>();

        // Gets an array of widths of all bins 
        double[] xbinWidths = getBinWidths(ddX);

        // Once a cadence value is determined, this array is used to store the amount of bins
        // that cadence value corresponds to.
        int[] xcadencesInBins = new int[xbinWidths.length];

        Units xoffsetUnits = xdsUnits.getOffsetUnits();

        // Checking for binplus/minus
        QDataSet xPlus = (QDataSet) xds.property(QDataSet.BIN_PLUS);
        if (xPlus != null) {
            xHardBinPlus = (int) (xPlus.value() / xDat.doubleValue(xoffsetUnits));
        }
        QDataSet xMinus = (QDataSet) xds.property(QDataSet.BIN_MINUS);
        if (xMinus != null) {
            xHardBinMinus = (int) (xMinus.value() / xDat.doubleValue(xoffsetUnits));
        }

        QDataSet xCad = (QDataSet) xds.property(QDataSet.CADENCE);
        double xCadenceVal = 0;
        if (xCad != null) {
            xCadenceVal = xCad.value();
            if (ddX.isLog) {
                // One cadence value will not work for logrithmic axis so the function 
                // call below gets an array of cadence values in bins to be used. 
                xcadencesInBins = getCadenceValues(xbinWidths, xCadenceVal, -1);
            } else {
                xcadencesInBins = null;
                xSoftRad = (int) Math.round(xCadenceVal / xDat.doubleValue(xoffsetUnits));
            }

        } else {
            // If no cadence is given, the number of bins between each datapoint is calculated and 
            // added to a list. The list is sorted and a position in the list is used given by xPosInList.

            int xPosInList = (int) Math.round(0.95 * xCadencesToSort.size());

            int currentDataWidthx;
            for (int i = 0; i < xds.length() - 1; i++) {
                currentDataWidthx = ddX.whichBin(xds.value(i + 1), xdsUnits)
                        - ddX.whichBin(xds.value(i), xdsUnits);
                if (currentDataWidthx >= 1) {
                    xCadencesToSort.add(currentDataWidthx);
                }
            }
            Collections.sort(xCadencesToSort, Collections.reverseOrder());
            //xCadencesToSort.sort( (x,y) -> y-x );
            if (xCadencesToSort.size() >= 1) {
                xCadenceVal = xCadencesToSort.get(xPosInList);
                logger.log(Level.FINE, " X cadence from sorted list = {0}", xCadenceVal);
            } else {
                logger.fine("No Cadences.");
            }
            if (ddX.isLog) {
                xcadencesInBins = getCadenceValues(xbinWidths, xCadenceVal, xCadencesToSort.get(0));
            } else {
                xcadencesInBins = null;
                xSoftRad = (int) xCadenceVal;
            }
        }

        //Process is the same as X dataset
        Datum yDat = ddY.binWidthDatum();
        List<Integer> yCadencesToSort = new ArrayList<>();
        List<Double> yCadencesToSortValues = new ArrayList<>();
        double[] ybinWidths = getBinWidths(ddY);
        int[] ycadencesInBins = new int[ybinWidths.length];

        Units yoffsetUnits = ydsUnits.getOffsetUnits();
        QDataSet yPlus = (QDataSet) yds.property(QDataSet.BIN_PLUS);
        if (yPlus != null) {
            yHardBinPlus = (int) (yPlus.value() / yDat.doubleValue(yoffsetUnits));
        }
        QDataSet yMinus = (QDataSet) yds.property(QDataSet.BIN_MINUS);
        if (yMinus != null) {
            yHardBinMinus = (int) (yMinus.value() / yDat.doubleValue(yoffsetUnits));
        }
        QDataSet yCad = (QDataSet) yds.property(QDataSet.CADENCE);
        double yCadenceVal = 0;
        int yCadenceValueBin = 0;
        if (yCad != null) {
            yCadenceVal = yCad.value();
            if (ddY.isLog) {
                ycadencesInBins = getCadenceValues(ybinWidths, yCadenceVal, -1);

            } else {
                ycadencesInBins = null;
                ySoftRad = (int) Math.round(yCadenceVal / yDat.doubleValue(yoffsetUnits));
            }

        } else {
            int currentDataWidthy;
            for (int i = 0; i < yds.length() - 1; i++) {
                currentDataWidthy = ddY.whichBin(yds.value(i + 1), yoffsetUnits)
                        - ddY.whichBin(yds.value(i), (Units) yoffsetUnits);
                if (currentDataWidthy >= 1) {
                    yCadencesToSort.add(currentDataWidthy);
                    yCadencesToSortValues.add(yds.value(i + 1) - yds.value(i));
                }
            }

            Collections.sort(yCadencesToSort, Collections.reverseOrder());
            //yCadencesToSort.sort( (x,y) -> y-x );
            Collections.sort(yCadencesToSortValues, Collections.reverseOrder());
            //yCadencesToSortValues.sort( (x,y) -> y.compareTo(x));
            int yCadSortIndex = 0;
            if (yCadencesToSortValues.size() >= 1) {
                yCadSortIndex = (int) Math.round(0.95 * yCadencesToSortValues.size());
                yCadenceVal = yCadencesToSortValues.get(yCadSortIndex);
                logger.log(Level.FINE, " Y cadence from sorted list = {0}", yCadenceVal);
            } else {
                logger.fine("No Cadences.");
            }
            if (ddY.isLog) {
                //QDataSet guessYCad = org.virbo.dataset.DataSetUtil.guessCadence(yds, null);
                //yCadenceVal = guessYCad.value();
                //logger.fine(" The Guess Cadence Dataset has a value of: " + yCadenceVal);
                ycadencesInBins = getCadenceValues(ybinWidths, yCadenceVal, yCadencesToSort.get(yCadSortIndex));
            } else {
                ycadencesInBins = null;
                ySoftRad = (int) yCadencesToSort.get(yCadSortIndex);
            }
        }

        if (xcadencesInBins != null) {
            logger.fine(" Cadences vary in X");
        } else {
            logger.log(Level.FINE, " x Cadence is = {0}", xSoftRad);
        }
        if (ycadencesInBins != null) {
            logger.fine(" Cadences vary in Y");
        } else {
            logger.log(Level.FINE, " y Cadence is = {0}", ySoftRad);
        }

        result = InterpolateHardAndSoftEdge(xHardBinPlus, xHardBinMinus, yHardBinPlus, yHardBinMinus, xSoftRad, ySoftRad,
                nx, ny, result, xcadencesInBins, ycadencesInBins);

        return result;

    }

    /**
     * Interpolates values within an ellipse of each data point.
     *
     * A box defined by the hard edge parameters is placed around each data
     * point. Box width = xHardBinPlus + xHardBinMinus + 1 Box Length =
     * yHardBinPlus + yHardBinMinus + 1 Bins lying within the box all receive
     * the same value as the data point. Next, an ellipse is defined around the
     * box with semi-major and minor axis defined by the soft edge parameters.
     * Bins lying outside the box and within the ellipse are given a weight
     * between 0 and 1 inclusive as a function of distance from the center of
     * the box, zero being on the edge of the ellipse. Weight = 1.0 -
     * (currentXBin**2/xSoftRad**2) + (currentYBin**2 / ySoftRad**2) where
     * currentXBin and currentYBin are the number of bins away from the data
     * point in the x and y direction, respectively. Each bin keeps track of the
     * all the weights and values associated with it the values are averaged in
     * the end to give a final value to each bin. The final value of a bin = Sum
     * (weight * value) / Sum ( weight)
     *
     * @param xHardBinPlus - Defines the number of bins greater than the bin of
     * the data point that should receive the same value as the data.
     * @param xHardBinMinus - Defines the number of bins less than the bin of
     * the data point that should receive the same value as the data.
     * @param yHardBinPlus - Same as xHardBinPlus but in the y direction
     * @param yHardBinMinus - Same as xHardBinMinus but in the y direction
     * @param xSoftRad - defines a semi-major/minor axis of the interpolation
     * ellipse
     * @param ySoftRad - defines the other semi-major/minor axis of the
     * interpolation ellipse
     * @param xbins - Total number of bins in the x direction. Used just to make
     * sure the interpolation ellipse does not go off the bounds of the plot.
     * @param ybins - same as x bins but in the y direction.
     * @param data - the input dataset.
     * @param yVaryingCadence
     * @return
     */
    private WritableDataSet InterpolateHardAndSoftEdge(int xHardBinPlus, int xHardBinMinus, int yHardBinPlus, int yHardBinMinus,
            int xSoftRad1, int ySoftRad1, int xbins, int ybins, WritableDataSet data, int[] xVaryingCadenceBin, int[] yVaryingCadenceBin) {
        // Create template array of weights to be used for each data point
        double[][] ValTimesWeightSum = new double[xbins + 1][ybins + 1];
        double[][] WeightSum = new double[xbins + 1][ybins + 1];
        int[][] count = new int[xbins + 1][ybins + 1];

        int prevxSoftRad;
        int prevySoftRad;
        int xSoftRad;
        int ySoftRad;
        double[][] templateBox;
        templateBox = CreateTemplateBox(xHardBinPlus, xHardBinMinus, yHardBinPlus, yHardBinMinus, 1, 1);
        xSoftRad = xSoftRad1;
        ySoftRad = ySoftRad1;
        prevxSoftRad = 1;
        prevySoftRad = 1;
        double value = 0.0;
        for (int ix = 0; ix <= xbins; ix++) {
            if (xVaryingCadenceBin != null) {
                xSoftRad = xVaryingCadenceBin[ix];
            }
            for (int iy = 0; iy <= ybins; iy++) {
                if (yVaryingCadenceBin != null) {
                    ySoftRad = yVaryingCadenceBin[iy];
                }

                value = data.value(ix, iy);
                if (value > 0.0) {

                    if ((xSoftRad != prevxSoftRad || ySoftRad != prevySoftRad)) {
                        templateBox = CreateTemplateBox(xHardBinPlus, xHardBinMinus, yHardBinPlus, yHardBinMinus, xSoftRad, ySoftRad);
                    }
                    prevxSoftRad = xSoftRad;
                    prevySoftRad = ySoftRad;
                    if ((xSoftRad == 0 && ySoftRad == 0)) {
                        continue;
                    }
                    for (int i = -(xHardBinMinus + xSoftRad); i <= (xHardBinPlus + xSoftRad); i++) {
                        for (int j = -(yHardBinMinus + ySoftRad); j <= (yHardBinPlus + ySoftRad); j++) {
                            if ((ix + i < 0) || (ix + i > xbins) || (iy + j < 0) || (iy + j > ybins)) {
                                continue;
                            } else {
                                if (templateBox[i + xHardBinMinus + xSoftRad][j + yHardBinMinus + ySoftRad] == 0) {
                                    continue;
                                }
                                ValTimesWeightSum[ix + i][iy + j] += value * templateBox[i + xHardBinMinus + xSoftRad][j + yHardBinMinus + ySoftRad];
                                WeightSum[ix + i][iy + j] += templateBox[i + xHardBinMinus + xSoftRad][j + yHardBinMinus + ySoftRad];
                                count[ix + i][iy + j]++;
                            }
                        }
                    }
                }
            }
        }
        for (int ix2 = 0; ix2 <= xbins; ix2++) {
            for (int iy2 = 0; iy2 <= ybins; iy2++) {

                if (count[ix2][iy2] >= 1) {
                    data.putValue(ix2, iy2, (ValTimesWeightSum[ix2][iy2] / WeightSum[ix2][iy2]));

                } else {
                    data.putValue(ix2, iy2, -1e31);
                    //data.putValue(ix2,iy2, 0.0);
                }
            }
        }
        return data;
    }

    public double[][] CreateTemplateBox(int xHardBinPlus, int xHardBinMinus, int yHardBinPlus, int yHardBinMinus, int xSoftRad, int ySoftRad) {
        double[][] templateWeights = new double[(xHardBinPlus + xHardBinMinus + 2 * xSoftRad) + 1][(yHardBinPlus + yHardBinMinus + 2 * ySoftRad) + 1];
        for (int i = -(xHardBinMinus + xSoftRad); i <= (xHardBinPlus + xSoftRad); i++) {
            for (int j = -(yHardBinMinus + ySoftRad); j <= (yHardBinPlus + ySoftRad); j++) {
                if (InBinPlusMinuxHardEdgeBox(i, j, xHardBinPlus, xHardBinMinus, yHardBinPlus, yHardBinMinus)) {
                    templateWeights[i + (xHardBinMinus + xSoftRad)][j + (yHardBinMinus + ySoftRad)] = 1.0;
                } else if (InEllipseCutoff(i, j, (double) (xHardBinMinus + xHardBinPlus + 2 * xSoftRad) / 2.0, (double) (yHardBinMinus + yHardBinPlus + 2 * ySoftRad) / 2.0)) {

                    templateWeights[i + (xHardBinMinus + xSoftRad)][j + yHardBinMinus + ySoftRad] = 1.0 - EllipseValue(i, j, (xHardBinMinus + xHardBinPlus + 2 * xSoftRad) / (double) 2.0, (yHardBinMinus + yHardBinPlus + 2 * ySoftRad) / (double) 2.0);
                    if (templateWeights[i + (xHardBinMinus + xSoftRad)][j + yHardBinMinus + ySoftRad] < 0) {
                        templateWeights[i + (xHardBinMinus + xSoftRad)][j + yHardBinMinus + ySoftRad] = 0;
                    }
                } else {

                    templateWeights[i + (xHardBinMinus + xSoftRad)][j + yHardBinMinus + ySoftRad] = 0.0;
                }
            }
        }
        return templateWeights;
    }

    public boolean InBinPlusMinuxHardEdgeBox(int xPlusMinus, int yPlusMinus, int xHardBinPlus, int xHardBinMinus, int yHardBinPlus, int yHardBinMinus) {
        if (xPlusMinus >= -(xHardBinMinus) && xPlusMinus <= xHardBinPlus && yPlusMinus >= -(yHardBinMinus) && yPlusMinus <= yHardBinPlus) {
            return true;
        } else {
            return false;
        }
    }

    public boolean InEllipseCutoff(int xDist, int yDist, double xR, double yR) {
        return (((double) xDist * xDist / (xR * xR)) + ((double) yDist * yDist / (yR * yR))) <= 1.0;
    }

    public double EllipseValue(int xDist, int yDist, double xR, double yR) {
        return (double) xDist * xDist / (xR * xR) + ((double) yDist * yDist / (yR * yR));
    }

    public double[] getBinWidths(RebinDescriptor rebinDesc) {
        double[] binStarts = rebinDesc.binStarts();
        double[] binStops = rebinDesc.binStops();
        if (binStarts.length != binStops.length) {
            logger.fine("Number of start bins is not equal to number of stop bins.");
            return null;
        }
        double[] binWidths = new double[binStarts.length];
        for (int i = 0; i < binStarts.length; i++) {
            binWidths[i] = binStops[i] - binStarts[i];
        }
        return binWidths;
    }

    public int[] getCadenceValues(double[] binWidths, double CadenceValue, int maxSep) {
        int[] cadencesIntBins = new int[binWidths.length];
        double cadenceBuffer;
        int IndexBuf;
        for (int i = 0; i < binWidths.length; i++) {
            cadenceBuffer = 0.0;
            IndexBuf = 0;
            while (cadenceBuffer < CadenceValue) {
                if (IndexBuf + i >= binWidths.length) {
                    if (i == 0) {
                        break;
                    }
                    cadencesIntBins[i] = cadencesIntBins[i - 1];
                    break;
                }
                cadenceBuffer += binWidths[i + IndexBuf++];

            }
            if (IndexBuf > maxSep && maxSep != -1) {
                cadencesIntBins[i] = maxSep;
                continue;
            }
            cadencesIntBins[i] = IndexBuf;

        }
        return cadencesIntBins;
    }

}
