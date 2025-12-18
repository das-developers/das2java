package org.das2.qds.util;

import org.das2.qds.DDataSet;
import org.das2.qds.MutablePropertyDataSet;
import org.das2.qds.QDataSet;

/**
 * LTTB (Largest Triangle Three Buckets) downsampling for Autoplot QDataSet.
 *
 * Assumes:
 *   - yData is rank-1
 *   - xData is taken from DEPEND_0 if present; otherwise x = 0,1,2,...
 *
 * Returns a rank-1 DDataSet with DEPEND_0 set to a rank-1 x dataset.
 */
public final class LttbDownsampler {

    private LttbDownsampler() {
        // utility class
    }

    /**
     * Downsample a 1D QDataSet with LTTB.
     *
     * @param yData     rank-1 QDataSet of y-values
     * @param threshold desired number of points (including first and last)
     * @return downsampled QDataSet (rank-1) with DEPEND_0 set
     */
    public static QDataSet lttb(QDataSet yData, int threshold) {
        int dataLength = yData.length();
        if (dataLength == 0) {
            return yData;
        }

        // If threshold is invalid or larger than input, just return original
        if (threshold >= dataLength || threshold <= 2) {
            return yData;
        }

        // Get x values from DEPEND_0 if present, otherwise use index
        QDataSet xDep = (QDataSet) yData.property(QDataSet.DEPEND_0);
        double[] x = new double[dataLength];
        double[] y = new double[dataLength];

        if (xDep != null) {
            for (int i = 0; i < dataLength; i++) {
                x[i] = xDep.value(i);
            }
        } else {
            for (int i = 0; i < dataLength; i++) {
                x[i] = i;
            }
        }

        for (int i = 0; i < dataLength; i++) {
            y[i] = yData.value(i);
        }

        // LTTB core
        double[] sampledX = new double[threshold];
        double[] sampledY = new double[threshold];

        // Always include first point
        int sampledIndex = 0;
        sampledX[sampledIndex] = x[0];
        sampledY[sampledIndex] = y[0];
        sampledIndex++;

        double bucketSize = (double) (dataLength - 2) / (threshold - 2);

        int a = 0;  // index of last chosen point

        for (int i = 0; i < threshold - 2; i++) {
            int avgRangeStart = (int) Math.floor((i + 1) * bucketSize) + 1;
            int avgRangeEnd   = (int) Math.floor((i + 2) * bucketSize) + 1;
            if (avgRangeEnd > dataLength) {
                avgRangeEnd = dataLength;
            }

            int avgRangeLength = avgRangeEnd - avgRangeStart;
            if (avgRangeLength < 1) {
                avgRangeLength = 1;
                avgRangeStart = Math.max(avgRangeStart, 1);
                avgRangeEnd   = avgRangeStart + 1;
                if (avgRangeEnd > dataLength) {
                    avgRangeEnd = dataLength;
                    avgRangeStart = avgRangeEnd - 1;
                }
            }

            double avgX = 0.0;
            double avgY = 0.0;
            for (int j = avgRangeStart; j < avgRangeEnd; j++) {
                avgX += x[j];
                avgY += y[j];
            }
            avgX /= avgRangeLength;
            avgY /= avgRangeLength;

            int rangeOffs = (int) Math.floor(i * bucketSize) + 1;
            int rangeTo   = (int) Math.floor((i + 1) * bucketSize) + 1;
            rangeTo = Math.min(rangeTo, dataLength - 1);

            double maxArea = -1.0;
            int maxAreaIndex = rangeOffs;

            double ax = x[a];
            double ay = y[a];

            for (int j = rangeOffs; j < rangeTo; j++) {
                // Triangle area between (ax,ay), (x[j],y[j]), (avgX,avgY)
                double area = Math.abs(
                        (ax - avgX) * (y[j] - ay) -
                        (ax - x[j]) * (avgY - ay)
                );
                if (area > maxArea) {
                    maxArea = area;
                    maxAreaIndex = j;
                }
            }

            sampledX[sampledIndex] = x[maxAreaIndex];
            sampledY[sampledIndex] = y[maxAreaIndex];
            sampledIndex++;

            a = maxAreaIndex;
        }

        // Always include last point
        sampledX[sampledIndex] = x[dataLength - 1];
        sampledY[sampledIndex] = y[dataLength - 1];

        // Build output QDataSet (x and y)
        DDataSet xOut = DDataSet.createRank1(threshold);
        DDataSet yOut = DDataSet.createRank1(threshold);

        for (int i = 0; i < threshold; i++) {
            xOut.putValue(i, sampledX[i]);
            yOut.putValue(i, sampledY[i]);
        }

        // Copy units, if present
        if (xDep != null && xDep.property(QDataSet.UNITS) != null) {
            ((MutablePropertyDataSet) xOut).putProperty(
                    QDataSet.UNITS,
                    xDep.property(QDataSet.UNITS)
            );
        }
        if (yData.property(QDataSet.UNITS) != null) {
            ((MutablePropertyDataSet) yOut).putProperty(
                    QDataSet.UNITS,
                    yData.property(QDataSet.UNITS)
            );
        }

        // Wire x as DEPEND_0 of y
        ((MutablePropertyDataSet) yOut).putProperty(QDataSet.DEPEND_0, xOut);

        return yOut;
    }
}