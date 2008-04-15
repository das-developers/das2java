/*
 * BinAverage.java
 *
 * Created on May 30, 2007, 8:56 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.virbo.dsutil;

import edu.uiowa.physics.pw.das.datum.Units;
import java.util.Arrays;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.DataSetUtil;

/**
 *
 * @author jbf
 */
public class BinAverage {

    private BinAverage() {
    }

    /**
     * returns a dataset with tags specified by newTags0.  Data from <tt>ds</tt>
     * are averaged together when they fall into the same bin.  
     *
     * @param ds a rank 1 dataset, no fill
     * @param newTags0 a rank 1 tags dataset, that must be MONOTONIC.
     * @return rank 1 dataset with DEPEND_0 = newTags.
     */
    public static DDataSet rebin(QDataSet ds, QDataSet newTags0) {
        QDataSet dstags = (QDataSet) ds.property(QDataSet.DEPEND_0);

        QDataSet wds = DataSetUtil.weightsDataSet(ds);

        Units u = (Units) ds.property(org.virbo.dataset.QDataSet.UNITS);
        if (u == null) {
            u = Units.dimensionless;
        }
        double fill = (Double) wds.property(QDataSet.FILL_VALUE);

        DDataSet result = DDataSet.createRank1(newTags0.length());
        DDataSet weights = DDataSet.createRank1(newTags0.length());
        int ibin = -1;
        for (int i = 0; i < ds.length(); i++) {
            ibin = DataSetUtil.closest(newTags0, dstags.value(i), ibin);
            double d = ds.value(i);
            double w = wds.value(i);

            double s = result.value(ibin);
            result.putValue(ibin, s + d * w);
            double n = weights.value(ibin);
            weights.putValue(ibin, n + w);

        }

        for (int i = 0; i < result.length(); i++) {
            if (weights.value(i) > 0) {
                result.putValue(i, result.value(i) / weights.value(i));
            } else {
                result.putValue(i, fill);
            }
        }

        result.putProperty(QDataSet.DEPEND_0, newTags0);

        return result;
    }

    /**
     * returns a dataset with tags specified by newTags
     * @param ds a rank 2 dataset.
     * @param newTags0 rank 1 monotonic dataset
     * @param newTags1 rank 1 monotonic dataset
     * @return rank 2 dataset with newTags0 for the DEPEND_0 tags, newTags1 for the DEPEND_1 tags.
     */
    public static DDataSet rebin(QDataSet ds, QDataSet newTags0, QDataSet newTags1) {

        if (ds.rank() != 2) {
            throw new IllegalArgumentException("ds must be rank2");
        }

        QDataSet dstags0 = (QDataSet) ds.property(QDataSet.DEPEND_0);

        QDataSet wds = DataSetUtil.weightsDataSet(ds);

        double fill = (Double) wds.property(QDataSet.FILL_VALUE);

        DDataSet result = DDataSet.createRank2(newTags0.length(), newTags1.length());
        DDataSet weights = DDataSet.createRank2(newTags0.length(), newTags1.length());

        QDataSet ibin1CacheDs = null;
        int[] ibins1 = null;

        int ibin0 = -1;
        for (int i = 0; i < ds.length(); i++) {
            ibin0 = DataSetUtil.closest(newTags0, dstags0.value(i), ibin0);

            QDataSet dstags1 = (QDataSet) ds.property(QDataSet.DEPEND_1, i);

            if (dstags1 != ibin1CacheDs) {
                ibins1 = new int[dstags1.length()];
                Arrays.fill(ibins1, -1);
                for (int j = 0; j < dstags1.length(); j++) {
                    ibins1[j] = DataSetUtil.closest(newTags1, dstags1.value(j), ibins1[j]);
                }
                ibin1CacheDs = dstags1;
            }

            for (int j = 0; j < dstags1.length(); j++) {
                int ibin1 = ibins1[j];
                double d = ds.value(i, j);
                double w = wds.value(i, j);
                double s = result.value(ibin0, ibin1);
                result.putValue(ibin0, ibin1, s + w * d);
                double n = weights.value(ibin0, ibin1);
                weights.putValue(ibin0, ibin1, n + w);

            }
        }

        for (int i = 0; i < result.length(); i++) {
            for (int j = 0; j < result.length(i); j++) {
                if (weights.value(i, j) > 0) {
                    result.putValue(i, j, result.value(i, j) / weights.value(i, j));
                } else {
                    result.putValue(i, j, fill);
                }
            }
        }

        result.putProperty(QDataSet.DEPEND_0, newTags0);
        result.putProperty(QDataSet.DEPEND_1, newTags1);
        result.putProperty( QDataSet.WEIGHTS_PLANE, weights );

        return result;
    }

    /**
     * run boxcar average over the dataset, returning a dataset of same geometry.  Points near the edge are simply copied from the
     * source dataset.  The result dataset contains a property "weights" that is the weights for each point.
     *
     * @param ds a rank 1 dataset of size N
     * @param size the number of adjacent bins to average
     * @return rank 1 dataset of size N
     */
    public static DDataSet boxcar(QDataSet ds, int size) {
        int nn = ds.length();
        int s2 = size / 2;

        QDataSet wds= DataSetUtil.weightsDataSet(ds);

        DDataSet sums = DDataSet.createRank1(nn);
        DataSetUtil.putProperties(DataSetUtil.getProperties(ds), sums);
        DDataSet weights = DDataSet.createRank1(nn);

        double runningSum = 0;
        double runningWeight = 0;

        // compute initial boxcar, handle the beginning by copying
        for (int i = 0; i < size; i++) {
            double d = ds.value(i);
            double w = wds.value(i);
            sums.putValue(i, d); //note for i>=s2, these values will be clobbered.
            weights.putValue(i, w);
            runningSum += d;
            runningWeight += w;
        }

        for (int i = s2; i < nn - s2; i++) {
            sums.putValue(i, runningSum);
            weights.putValue(i, runningWeight);

            double d0 = ds.value(i - s2);
            double w0 = wds.value( i - s2 );

            double d = ds.value(i - s2 + size);
            double w = wds.value( i - s2 + size );

            runningSum += d * w - d0 * w0;
            runningWeight += w - w0;

        }

        // handle the end of the dataset by copying
        for (int i = nn - s2; i < size; i++) {
            double d = ds.value(i);
            double w = wds.value(i);
            sums.putValue(i, d);
            weights.putValue(i, w);
        }

        DDataSet result = sums;

        double fill= (Double)wds.property(QDataSet.FILL_VALUE);
        for (int i = 0; i < nn; i++) {
            if (weights.value(i) > 0) {
                result.putValue(i, result.value(i) / weights.value(i));
            } else {
                result.putValue(i, fill );
            }
        }

        result.putProperty(QDataSet.WEIGHTS_PLANE, weights);

        return result;

    }
}
