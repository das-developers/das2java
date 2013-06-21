/*
 * BinAverage.java
 *
 * Created on May 30, 2007, 8:56 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.virbo.dsutil;

import org.das2.datum.Units;
import java.util.Arrays;
import org.virbo.dataset.ArrayDataSet;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.IDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dsops.Ops;

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

        double fill = ((Number) wds.property(QDataSet.FILL_VALUE)).doubleValue();

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
     * takes rank 2 bundle (x,y,z) and averages it into table z(x,y).  This is similar to what happens in the
     * spectrogram routine.
     * @param ds rank 2 bundle(x,y,z)
     * @param dep0 the depend0 for the result
     * @param dep1 the depend1 for the result
     * @return rank 2 dataset of z averages with depend_0 and depend_1.
     */
    public static DDataSet rebinBundle( QDataSet ds, QDataSet dep0, QDataSet dep1 ) {
        DDataSet sresult= DDataSet.createRank2( dep0.length(), dep1.length() );
        IDataSet nresult= IDataSet.createRank2( dep0.length(), dep1.length() );
        QDataSet wds= DataSetUtil.weightsDataSet( DataSetOps.slice1(ds,2) );

        double xscal= dep0.value(1) - dep0.value(0);
        double xbase= dep0.value(0) - ( xscal / 2);
        int nx= dep0.length();

        double yscal= dep1.value(1) - dep1.value(0);
        double ybase= dep1.value(0) - ( yscal / 2);
        int ny= dep1.length();

        for ( int ids=0; ids<ds.length(); ids++ ) {
            if ( wds.value(ids)>0 ) {
                double x= ds.value(ids,0);
                double y= ds.value(ids,1);
                double z= ds.value(ids,2);
                int i= (int)( ( x-xbase ) / xscal );
                int j= (int)( ( y-ybase ) / yscal );
                if ( i<0 || j<0 ) continue;
                if ( i>=nx || j>=ny ) continue;
                sresult.putValue( i, j, z + sresult.value( i, j ) );
                nresult.putValue( i, j, 1 + nresult.value( i, j ) );
            }
        }

        double fill= -1e31;
        for ( int i=0; i<nx; i++ ) {
            for ( int j=0; j<ny; j++ ) {
                int n= (int)nresult.value( i,j );
                if ( n>0 ) {
                    sresult.putValue( i,j, sresult.value(i,j)/n );
                } else {
                    sresult.putValue( i,j, fill );
                }
            }
        }

        DataSetUtil.copyDimensionProperties( ds, sresult );
        sresult.putProperty( QDataSet.DEPEND_0, dep0 );
        sresult.putProperty( QDataSet.DEPEND_1, dep1 );
        sresult.putProperty( QDataSet.FILL_VALUE, fill );
        sresult.putProperty( QDataSet.RENDER_TYPE, "nnSpectrogram" );

        return sresult;
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

        if ( SemanticOps.isBundle(ds) ) {
            return rebinBundle( ds, newTags0, newTags1 );
        }

        QDataSet dstags0 = (QDataSet) ds.property(QDataSet.DEPEND_0);
        if ( dstags0==null ) {
            throw new IllegalArgumentException("expected ds to have DEPEND_0");
        }

        QDataSet wds = DataSetUtil.weightsDataSet(ds);

        double fill = ((Number) wds.property(QDataSet.FILL_VALUE)).doubleValue();

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
        result.putProperty(QDataSet.WEIGHTS_PLANE, weights);

        return result;
    }

    /**
     * returns number of stddev from adjacent data.
     * @param ds, rank 1 dataset.
     * @param boxcarSize
     * @return QDataSet 
     */
    public static QDataSet residuals(QDataSet ds, int boxcarSize) {
        if (ds.rank() != 1) {
            throw new IllegalArgumentException("rank must be 1");
        }
        QDataSet mean = BinAverage.boxcar(ds, boxcarSize);
        QDataSet dres = Ops.pow(Ops.subtract(ds, mean), 2);
        QDataSet var = Ops.sqrt(BinAverage.boxcar(dres, boxcarSize));
        QDataSet res = Ops.divide(Ops.abs(Ops.subtract(ds, mean)), var);
        return res;
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
        int s3 = s2 + size % 2;   // one greater than s2 if s2 is odd.

        if (ds.rank() != 1) {
            if ( SemanticOps.isRank2Waveform(ds) ) {
                DDataSet result= (DDataSet) ArrayDataSet.createRank2( double.class, ds.length(), ds.length(0) );
                for ( int i=0; i<ds.length(); i++ ) {
                    DDataSet r1= boxcar( ds.slice(i), size );
                    DDataSet.copyElements( r1, 0, result, i, r1.length() ); // careful
                }
                return result;
            } else {
                throw new IllegalArgumentException("dataset must be rank 1");
            }
        }
        if (ds.length() < size) {
            throw new IllegalArgumentException("dataset length is less than window size");
        }

        QDataSet wds = DataSetUtil.weightsDataSet(ds);

        DDataSet sums = DDataSet.createRank1(nn);
        //DDataSet sums2 = DDataSet.createRank1(nn); // commented code for one-pass variance incorrect
        DataSetUtil.putProperties(DataSetUtil.getProperties(ds), sums);
        DDataSet weights = DDataSet.createRank1(nn);

        double runningSum = 0;
        //double runningSum2 = 0;
        double runningWeight = 0;

        // compute initial boxcar, handle the beginning by copying
        for (int i = 0; i < size; i++) {
            double d = ds.value(i);
            double w = wds.value(i);
            sums.putValue(i, d); //note for i>=s2, these values will be clobbered.
            //sums2.putValue(i, d*d);
            weights.putValue(i, w);
            runningSum += d;
            //runningSum2 += d*d;
            runningWeight += w;
        }

        for (int i = s2; i < nn - s3; i++) {
            sums.putValue(i, runningSum);
            //sums2.putValue(i, runningSum2);
            weights.putValue(i, runningWeight);

            double d0 = ds.value(i - s2);
            double w0 = wds.value(i - s2);

            double d = ds.value(i - s2 + size);
            double w = wds.value(i - s2 + size);

            runningSum += d * w - d0 * w0;
            //runningSum2 += d * d * w - d0 * d0 * w0; //  DANGER-assumes small boxcar
            runningWeight += w - w0;

        }

        // handle the end of the dataset by copying
        for (int i = nn - s3; i < nn; i++) {
            double d = ds.value(i);
            double w = wds.value(i);
            sums.putValue(i, d);
            //sums2.putValue(i, d*d);
            weights.putValue(i, w);
        }

        DDataSet result = sums;
        //DDataSet resultVar= sums2;

        Number fill= ((Number) wds.property(QDataSet.FILL_VALUE));
        if ( fill==null ) fill= -1e31;

        for (int i = 0; i < nn; i++) {
            if (weights.value(i) > 0) {
                double s = result.value(i);
                result.putValue(i, s / weights.value(i));
                //resultVar.putValue( i, ( Math.sqrt( resultVar.value(i) -  s * s ) / weights.value(i)) ); 

            } else {
                result.putValue(i, fill.doubleValue() );
            }
        }

        result.putProperty(QDataSet.WEIGHTS_PLANE, weights);
        //result.putProperty( QDataSet.DELTA_PLUS, resultVar );
        //result.putProperty( QDataSet.DELTA_MINUS, resultVar );
        result.putProperty(QDataSet.DEPEND_0, ds.property(QDataSet.DEPEND_0));
        result.putProperty(QDataSet.FILL_VALUE, fill);
        return result;

    }

    /**
     * reduce the rank 1 dataset by averaging blocks of bins together
     * @param ds rank 1 dataset with N points
     * @param binSize0 number of adjacent bins to reduce.
     * @return rank 1 dataset with N/binSize0 points.  Weights plane added.
     */
    public static QDataSet rebin(QDataSet ds, int binSize0) {
        int l0 = ds.length();

        DDataSet result = DDataSet.createRank1(l0 / binSize0);
        DDataSet weights = DDataSet.createRank1(l0 / binSize0);

        QDataSet wds = DataSetUtil.weightsDataSet(ds);

        int n0 = l0 / binSize0;

        double fill = ((Number) wds.property(QDataSet.FILL_VALUE)).doubleValue();

        for (int i0 = 0; i0 < n0; i0++) {
            int j0 = i0 * binSize0;

            double s = 0, w = 0;
            for (int k0 = 0; k0 < binSize0; k0++) {
                double w1 = wds.value(j0 + k0);
                w += w1;
                s += w1 * ds.value(j0 + k0);
            }
            weights.putValue(i0, w);
            result.putValue(i0, w == 0 ? fill : s / w);
        }

        result.putProperty(QDataSet.WEIGHTS_PLANE, weights);
        result.putProperty(QDataSet.FILL_VALUE, fill);
        QDataSet dep0 = (QDataSet) ds.property(QDataSet.DEPEND_0);
        if (dep0 != null) {
            result.putProperty(QDataSet.DEPEND_0, rebin(dep0, binSize0));
        }

        return result;
    }

    /**
     * reduce the rank 2 dataset by averaging blocks of bins together.  depend
     * datasets reduced as well.
     * @param ds rank 2 dataset with M by N points
     * @param binSize0 the number of bins to combine.  Note this is backwards from IDL!
     * @param binSize1 the number of bins to combine
     * @return rank 2 dataset with M/binSize0 by N/binSize1 points, with a weights plane.
     */
    public static QDataSet rebin(QDataSet ds, int binSize0, int binSize1) {
        int l0 = ds.length();
        int l1 = ds.length(0);
        DDataSet result = DDataSet.createRank2(l0 / binSize0, l1 / binSize1);
        DDataSet weights = DDataSet.createRank2(l0 / binSize0, l1 / binSize1);

        QDataSet wds = DataSetUtil.weightsDataSet(ds);

        int n0 = l0 / binSize0;
        int n1 = l1 / binSize1;

        double fill = ((Number) wds.property(QDataSet.FILL_VALUE)).doubleValue();

        for (int i0 = 0; i0 < n0; i0++) {
            for (int i1 = 0; i1 < n1; i1++) {
                int j0 = i0 * binSize0;
                int j1 = i1 * binSize1;
                double s = 0, w = 0;

                for (int k0 = 0; k0 < binSize0; k0++) {
                    for (int k1 = 0; k1 < binSize1; k1++) {
                        double w1 = wds.value(j0 + k0, j1 + k1);
                        w += w1;
                        s += w1 * ds.value(j0 + k0, j1 + k1);
                    }
                }
                weights.putValue(i0, i1, w);
                result.putValue(i0, i1, w == 0 ? fill : s / w);
            }
        }

        result.putProperty(QDataSet.WEIGHTS_PLANE, weights);
        result.putProperty(QDataSet.FILL_VALUE, fill);

        QDataSet dep0 = (QDataSet) ds.property(QDataSet.DEPEND_0);
        if (dep0 != null) {
            result.putProperty(QDataSet.DEPEND_0, rebin(dep0, binSize0));
        }

        QDataSet dep1 = (QDataSet) ds.property(QDataSet.DEPEND_1);
        if (dep1 != null) {
            result.putProperty(QDataSet.DEPEND_1, rebin(dep1, binSize1));
        }

        return result;
    }
}
