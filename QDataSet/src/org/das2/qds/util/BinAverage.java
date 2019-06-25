/*
 * BinAverage.java
 *
 * Created on May 30, 2007, 8:56 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.das2.qds.util;

import java.util.Arrays;
import org.das2.datum.UnitsConverter;
import org.das2.qds.ArrayDataSet;
import org.das2.qds.DDataSet;
import org.das2.qds.DataSetOps;
import org.das2.qds.QDataSet;
import org.das2.qds.DataSetUtil;
import org.das2.qds.IDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.WeightsDataSet;
import org.das2.qds.ops.Ops;
import static org.das2.qds.util.BinAverage.rebin;

/**
 * utility class providing methods for bin averaging.
 * @author jbf
 */
public class BinAverage {

    private BinAverage() {
    }

    /**
     * returns a dataset with tags specified by newTags0.  Data from <tt>ds</tt>
     * are averaged together when they fall into the same bin.  Note the result
     * will have the property WEIGHTS.
     *
     * @param ds a rank 1 dataset, no fill
     * @param newTags0 a rank 1 tags dataset, that must be MONOTONIC.
     * @return rank 1 dataset with DEPEND_0 = newTags.
     * @see #rebin(org.das2.qds.QDataSet, org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     * @see #binAverage(QDataSet, QDataSet ) 
     */
    public static DDataSet rebin(QDataSet ds, QDataSet newTags0) {
        return binAverage( ds, newTags0 );
    } 
    
    /**
     * returns a dataset with tags specified by newTags0.  Data from <tt>ds</tt>
     * are averaged together when they fall into the same bin.  Note the result
     * will have the property WEIGHTS.
     *
     * @param ds a rank 1 dataset, no fill
     * @param newTags0 a rank 1 tags dataset, that must be MONOTONIC.
     * @return rank 1 dataset with DEPEND_0 = newTags.
     * @see #rebin(org.das2.qds.QDataSet, org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     * @see #binAverage(QDataSet, QDataSet ) 
     * @see #binAverage(QDataSet, QDataSet, QDataSet ) 
     */
    public static DDataSet binAverage(QDataSet ds, QDataSet newTags0 ) {
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

            if ( w>0 ) {
                double s = result.value(ibin);
                result.putValue(ibin, s + d * w);
                double n = weights.value(ibin);
                weights.putValue(ibin, n + w);
            }

        }

        for (int i = 0; i < result.length(); i++) {
            if (weights.value(i) > 0) {
                result.putValue(i, result.value(i) / weights.value(i));
            } else {
                result.putValue(i, fill);
            }
        }

        weights.putProperty(QDataSet.DEPEND_0,newTags0);
        result.putProperty(QDataSet.DEPEND_0, newTags0);
        result.putProperty(QDataSet.FILL_VALUE,fill);
        result.putProperty(QDataSet.WEIGHTS,weights);

        return result;
    }
    
    /**
     * returns a dataset with tags specified by newTags.
     * @param ds a rank 2 dataset.  If it's a bundle, then rebinBundle is called.
     * @param newTags0 rank 1 monotonic dataset
     * @param newTags1 rank 1 monotonic dataset
     * @return rank 2 dataset with newTags0 for the DEPEND_0 tags, newTags1 for the DEPEND_1 tags.  WEIGHTS property contains the weights.
     * @see #rebin(org.das2.qds.QDataSet, int, int) 
     * @see #rebinBundle(org.das2.qds.QDataSet, org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     * @see #rebin(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     * @deprecated see binAverage
     * @see #binAverage(QDataSet, QDataSet, QDataSet ) 
     */
    public static DDataSet rebin(QDataSet ds, QDataSet newTags0, QDataSet newTags1) {
        return binAverage( ds, newTags0, newTags1 );
    }
    
    
    /**
     * returns a dataset with tags specified by newTags.
     * @param ds a rank 2 dataset.  If it's a bundle, then rebinBundle is called.
     * @param newTags0 rank 1 monotonic dataset
     * @param newTags1 rank 1 monotonic dataset
     * @return rank 2 dataset with newTags0 for the DEPEND_0 tags, newTags1 for the DEPEND_1 tags.  WEIGHTS property contains the weights.
     * @see #rebin(org.das2.qds.QDataSet, int, int) 
     * @see #rebinBundle(org.das2.qds.QDataSet, org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     * @see #binAverage(org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     * @see #binAverage(org.das2.qds.QDataSet ) 
     */
    public static DDataSet binAverage(QDataSet ds, QDataSet newTags0, QDataSet newTags1) {
    
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
            
            QDataSet ds1= ds.slice(i);
            QDataSet wds1= wds.slice(i);
            
            ibin0 = DataSetUtil.closest(newTags0, dstags0.value(i), ibin0);

            //QDataSet dstags1 = (QDataSet) ds.property(QDataSet.DEPEND_1, i);
            QDataSet dstags1 = (QDataSet) ds1.property(QDataSet.DEPEND_0);
            
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
                double d = ds1.value(j);
                double w = wds1.value(j);
                if ( w>0 ) {
                    double s = result.value(ibin0, ibin1);
                    result.putValue(ibin0, ibin1, s + w * d);
                    double n = weights.value(ibin0, ibin1);
                    weights.putValue(ibin0, ibin1, n + w);
                }
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

        weights.putProperty(QDataSet.DEPEND_0, newTags0);
        weights.putProperty(QDataSet.DEPEND_1, newTags1);
        result.putProperty(QDataSet.DEPEND_0, newTags0);
        result.putProperty(QDataSet.DEPEND_1, newTags1);
        result.putProperty(QDataSet.FILL_VALUE, fill );
        result.putProperty(QDataSet.WEIGHTS, weights);

        return result;
    }

    
    /**
     * return true if the data is linearly spaced with the given base and offset.
     * @param dep0
     * @param xscal
     * @param xbase
     * @return true if the data is linearly spaced with the given base and offset.
     */
    private static boolean isLinearlySpaced( QDataSet dep0, double xscal, double xbase ) {
        int nx= dep0.length(); 
        for ( int i=0; i<nx; i++ ) {
            if ( (int)( ( dep0.value(i)-xbase ) / xscal ) != i ) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 
     * takes rank 2 bundle (x,y,z,f) and averages it into rank 3 qube f(x,y,z).  This is 
     * similar to what happens in the spectrogram routine.
     * @param ds rank 2 bundle(x,y,z,f)
     * @param dep0 the rank 1 depend0 for the result, which must be uniformly spaced.
     * @param dep1 the rank 1 depend1 for the result, which must be uniformly spaced.
     * @param dep2 the rank 1 depend2 for the result, which must be uniformly spaced.
     * @return rank 3 dataset of z averages with depend_0, depend_1, and depend_2.  WEIGHTS contains the total weight for each bin.
     * @see #rebinBundle(org.das2.qds.QDataSet, org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     * @deprecated see binAverageBundle
     * @see #binAverageBundle(org.das2.qds.QDataSet, org.das2.qds.QDataSet, org.das2.qds.QDataSet, org.das2.qds.QDataSet ) 
     */
    public static DDataSet rebinBundle( QDataSet ds, QDataSet dep0, QDataSet dep1, QDataSet dep2 ) {
        return binAverageBundle( ds, dep0, dep1, dep2 );
    }
    
    /**
     * 
     * takes rank 2 bundle (x,y,z,f) and averages it into rank 3 qube f(x,y,z).  This is 
     * similar to what happens in the spectrogram routine.
     * @param ds rank 2 bundle(x,y,z,f)
     * @param dep0 the rank 1 depend0 for the result, which must be uniformly spaced.
     * @param dep1 the rank 1 depend1 for the result, which must be uniformly spaced.
     * @param dep2 the rank 1 depend2 for the result, which must be uniformly spaced.
     * @return rank 3 dataset of z averages with depend_0, depend_1, and depend_2.  WEIGHTS contains the total weight for each bin.
     * @see #rebinBundle(org.das2.qds.QDataSet, org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */    
    public static DDataSet binAverageBundle( QDataSet ds, QDataSet dep0, QDataSet dep1, QDataSet dep2 ) {        
        DDataSet sresult= DDataSet.createRank3( dep0.length(), dep1.length(), dep2.length() );
        IDataSet nresult= IDataSet.createRank3( dep0.length(), dep1.length(), dep2.length() );
        QDataSet wds= DataSetUtil.weightsDataSet( DataSetOps.slice1(ds,3) );

        QDataSet dep0_0 = dep0;
        QDataSet dep1_0 = dep1;
        QDataSet dep2_0 = dep2;
        
        boolean xlog = false;
        double xscal = dep0.value(1) - dep0.value(0);
        double xbase = dep0.value(0) - (xscal / 2);
        int nx = dep0.length();
        if (!isLinearlySpaced(dep0, xscal, xbase)) {
            xscal = Math.log10(dep0.value(1) / dep0.value(0));
            xbase = Math.log10(xbase);
            dep0 = Ops.log10(dep0);
            if (!isLinearlySpaced(dep0, xscal, xbase)) {
                throw new IllegalArgumentException("dep0 must be uniformly spaced.");
            } else {
                xlog = true;
            }
        }        
        
        boolean ylog = false;
        double yscal = dep1.value(1) - dep1.value(0);
        double ybase = dep1.value(0) - (yscal / 2);
        int ny = dep1.length();
        if (!isLinearlySpaced(dep1, yscal, ybase)) {
            yscal = Math.log10(dep1.value(1) / dep1.value(0));
            ybase = Math.log10(ybase);
            dep1 = Ops.log10(dep1);
            if (!isLinearlySpaced(dep1, yscal, ybase)) {
                isLinearlySpaced(dep1, yscal, ybase);
                throw new IllegalArgumentException("dep1 must be uniformly spaced.");
            } else {
                ylog = true;
            }
        }

        boolean zlog = false;
        double zscal = dep2.value(1) - dep2.value(0);
        double zbase = dep2.value(0) - (zscal / 2);
        int nz = dep2.length();
        if (!isLinearlySpaced(dep2, zscal, zbase)) {
            zscal = Math.log10(dep2.value(1) / dep2.value(0));
            zbase = Math.log10(zbase);
            dep2 = Ops.log10(dep2);
            if (!isLinearlySpaced(dep2, zscal, zbase)) {
                isLinearlySpaced(dep2, zscal, zbase);
                throw new IllegalArgumentException("dep2 must be uniformly spaced.");
            } else {
                ylog = true;
            }
        }        
                
        if ( ds.length()>0 ) { // accumulate.
            UnitsConverter xuc= SemanticOps.getLooseUnitsConverter( ds.slice(0).slice(0), dep0 );
            UnitsConverter yuc= SemanticOps.getLooseUnitsConverter( ds.slice(0).slice(1), dep1 );
            UnitsConverter zuc= SemanticOps.getLooseUnitsConverter( ds.slice(0).slice(2), dep2 );
            for ( int ids=0; ids<ds.length(); ids++ ) {
                double w= wds.value(ids);
                if ( w>0 ) {
                    double x= xuc.convert( xlog ? Math.log10( ds.value(ids,0) ) : ds.value(ids,0));
                    double y= yuc.convert( ylog ? Math.log10( ds.value(ids,1) ) : ds.value(ids,1));
                    double z= zuc.convert( zlog ? Math.log10( ds.value(ids,2) ) : ds.value(ids,2));
                    double f= ds.value(ids,3);
                    int i= (int)( ( x-xbase ) / xscal );
                    int j= (int)( ( y-ybase ) / yscal );
                    int k= (int)( ( z-zbase ) / zscal );
                    if ( i<0 || j<0 || k<0 ) continue;
                    if ( i>=nx || j>=ny || k>=nz ) continue;
                    sresult.putValue( i, j, k, f + sresult.value( i, j, k ) );
                    nresult.putValue( i, j, k, w + nresult.value( i, j, k ) );
                }
            }
        }

        double fill= -1e31;  // normalize.  The weights will be in the WEIGHTS property
        for ( int i=0; i<nx; i++ ) {
            for ( int j=0; j<ny; j++ ) {
                for ( int k=0; k<nz; k++ ) {
                    int n= (int)nresult.value( i,j,k );
                    if ( n>0 ) {
                        sresult.putValue( i,j,k, sresult.value(i,j,k)/n );
                    } else {
                        sresult.putValue( i,j,k, fill );
                    }
                }
            }
        }

        DataSetUtil.copyDimensionProperties( ds, sresult );
        nresult.putProperty( QDataSet.DEPEND_0, dep0_0 );
        nresult.putProperty( QDataSet.DEPEND_1, dep1_0 );
        nresult.putProperty( QDataSet.DEPEND_2, dep2_0 );
        sresult.putProperty( QDataSet.DEPEND_0, dep0_0 );
        sresult.putProperty( QDataSet.DEPEND_1, dep1_0 );
        sresult.putProperty( QDataSet.DEPEND_2, dep2_0 );
        sresult.putProperty( QDataSet.FILL_VALUE, fill );
        sresult.putProperty( QDataSet.WEIGHTS, nresult );
        sresult.putProperty( QDataSet.RENDER_TYPE, "nnSpectrogram" );

        return sresult;
        
    }
    
    /**
     * takes rank 2 bundle (x,y,z) and averages it into table z(x,y).  This is 
     * similar to what happens in the spectrogram routine.
     * @param ds rank 2 bundle(x,y,z)
     * @param dep0 the rank 1 depend0 for the result, which must be uniformly spaced.
     * @param dep1 the rank 1 depend1 for the result, which must be uniformly spaced.
     * @return rank 2 dataset of z averages with depend_0 and depend_1.  WEIGHTS contains the total weight for each bin.
     * @see #rebin(org.das2.qds.QDataSet, org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     * @see #rebinBundle(org.das2.qds.QDataSet, org.das2.qds.QDataSet, org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     * @deprecated see binAverageBundle
     * @see #binAverageBundle(org.das2.qds.QDataSet, org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static DDataSet rebinBundle( QDataSet ds, QDataSet dep0, QDataSet dep1 ) {
        return binAverageBundle( ds, dep0, dep1 );
    }
    
    /**
     * takes rank 2 bundle (x,y,z) and averages it into table z(x,y).  This is 
     * similar to what happens in the spectrogram routine.
     * @param ds rank 2 bundle(x,y,z)
     * @param dep0 the rank 1 depend0 for the result, which must be uniformly spaced.
     * @param dep1 the rank 1 depend1 for the result, which must be uniformly spaced.
     * @return rank 2 dataset of z averages with depend_0 and depend_1.  WEIGHTS contains the total weight for each bin.
     * @see #rebin(org.das2.qds.QDataSet, org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     * @see #rebinBundle(org.das2.qds.QDataSet, org.das2.qds.QDataSet, org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static DDataSet binAverageBundle(QDataSet ds, QDataSet dep0, QDataSet dep1) {

        DDataSet sresult = DDataSet.createRank2(dep0.length(), dep1.length());
        IDataSet nresult = IDataSet.createRank2(dep0.length(), dep1.length());
        QDataSet wds = DataSetUtil.weightsDataSet(DataSetOps.slice1(ds, 2));

        QDataSet dep0_0 = dep0;
        QDataSet dep1_0 = dep1;
        
        boolean xlog = false;
        double xscal = dep0.value(1) - dep0.value(0);
        double xbase = dep0.value(0) - (xscal / 2);
        int nx = dep0.length();
        if (!isLinearlySpaced(dep0, xscal, xbase)) {
            xscal = Math.log10(dep0.value(1) / dep0.value(0));
            xbase = Math.log10(xbase);
            dep0 = Ops.log10(dep0);
            if (!isLinearlySpaced(dep0, xscal, xbase)) {
                throw new IllegalArgumentException("dep0 must be uniformly spaced.");
            } else {
                xlog = true;
            }
        }

        boolean ylog = false;
        double yscal = dep1.value(1) - dep1.value(0);
        double ybase = dep1.value(0) - (yscal / 2);
        int ny = dep1.length();
        if (!isLinearlySpaced(dep1, yscal, ybase)) {
            yscal = Math.log10(dep1.value(1) / dep1.value(0));
            ybase = Math.log10(ybase);
            dep1 = Ops.log10(dep1);
            if (!isLinearlySpaced(dep1, yscal, ybase)) {
                isLinearlySpaced(dep1, yscal, ybase);
                throw new IllegalArgumentException("dep1 must be uniformly spaced.");
            } else {
                ylog = true;
            }
        }

        if ( ds.length()>0 ) {
            UnitsConverter xuc= SemanticOps.getLooseUnitsConverter( ds.slice(0).slice(0), dep0 );
            UnitsConverter yuc= SemanticOps.getLooseUnitsConverter( ds.slice(0).slice(1), dep1 );
            for ( int ids=0; ids<ds.length(); ids++ ) {
                double w= wds.value(ids);
                if ( w>0 ) {
                    double x= xuc.convert( xlog ? Math.log10( ds.value(ids,0) ) : ds.value(ids,0) );
                    double y= yuc.convert( ylog ? Math.log10( ds.value(ids,1) ) : ds.value(ids,1) );
                    double z= ds.value(ids,2);
                    int i= (int)( ( x-xbase ) / xscal );
                    int j= (int)( ( y-ybase ) / yscal );
                    if ( i<0 || j<0 ) continue;
                    if ( i>=nx || j>=ny ) continue;
                    sresult.putValue( i, j, z + sresult.value( i, j ) );
                    nresult.putValue( i, j, w + nresult.value( i, j ) );
                }
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
        nresult.putProperty( QDataSet.DEPEND_0, dep0_0 );
        nresult.putProperty( QDataSet.DEPEND_1, dep1_0 );
        sresult.putProperty( QDataSet.DEPEND_0, dep0_0 );
        sresult.putProperty( QDataSet.DEPEND_1, dep1_0 );
        sresult.putProperty( QDataSet.FILL_VALUE, fill );
        sresult.putProperty( QDataSet.WEIGHTS, nresult );
        sresult.putProperty( QDataSet.RENDER_TYPE, "nnSpectrogram" );

        return sresult;
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
            if ( w>0 ) {
                runningSum += d * w;
                //runningSum2 += d*d;
                runningWeight += w;
            }
        }

        for (int i = s2; i < nn - s3; i++) {
            sums.putValue(i, runningSum);
            //sums2.putValue(i, runningSum2);
            weights.putValue(i, runningWeight);

            double d0 = ds.value(i - s2);
            double w0 = wds.value(i - s2);
            if ( w0==0 ) d0= 0; 

            double d = ds.value(i - s2 + size);
            double w = wds.value(i - s2 + size);
            if ( w==0 ) d= 0;
            
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

        Number fill= ((Number) wds.property( WeightsDataSet.PROP_SUGGEST_FILL ) );
        if ( fill==null ) fill= -1e31;

        for (int i = 0; i < nn; i++) {
            double w= weights.value(i);
            if ( w > 0) {
                double s = result.value(i);
                result.putValue(i, s / w);
                //resultVar.putValue( i, ( Math.sqrt( resultVar.value(i) -  s * s ) / weights.value(i)) ); 

            } else {
                result.putValue(i, fill.doubleValue() );
            }
        }

        result.putProperty(QDataSet.WEIGHTS, weights);
        //result.putProperty( QDataSet.DELTA_PLUS, resultVar );
        //result.putProperty( QDataSet.DELTA_MINUS, resultVar );
        result.putProperty(QDataSet.DEPEND_0, ds.property(QDataSet.DEPEND_0));
        result.putProperty(QDataSet.FILL_VALUE, fill);
        return result;

    }

    /**
     * reduce the rank 1 dataset by averaging blocks of bins together
     * @param ds rank 1 dataset with N points
     * @param n0 number of bins in the result.
     * @return rank 1 dataset with n0 points.  Weights plane added.
     * @see #rebin(org.das2.qds.QDataSet, int, int) 
     * @see #rebin(org.das2.qds.QDataSet, int, int, int) 
     */
    public static QDataSet rebin(QDataSet ds, int n0 ) {

        DDataSet result = DDataSet.createRank1( n0);
        DDataSet weights = DDataSet.createRank1( n0 );

        QDataSet wds = DataSetUtil.weightsDataSet(ds);

        int binSize0= ds.length() / n0;
        
        double fill = ((Number) wds.property( WeightsDataSet.PROP_SUGGEST_FILL )).doubleValue();

        for (int i0 = 0; i0 < n0; i0++) {
            int j0 = i0 * binSize0;

            double s = 0, w = 0;
            for (int k0 = 0; k0 < binSize0; k0++) {
                double w1 = wds.value(j0 + k0);
                if ( w1>0 ) {
                    w += w1;
                    s += w1 * ds.value(j0 + k0);
                }
            }
            weights.putValue(i0, w);
            result.putValue(i0, w == 0 ? fill : s / w);
        }

        result.putProperty(QDataSet.WEIGHTS, weights);
        result.putProperty(QDataSet.FILL_VALUE, fill);
        QDataSet dep0 = (QDataSet) ds.property(QDataSet.DEPEND_0);
        if (dep0 != null) {
            result.putProperty(QDataSet.DEPEND_0, rebin(dep0, binSize0));
        }
        DataSetUtil.copyDimensionProperties( ds, result );

        return result;
    }

    /**
     * reduce the rank 2 dataset by averaging blocks of bins together.  depend
     * datasets reduced as well.
     * @param ds rank 2 dataset with M by N points
     * @param n0 the number of bins in the result. Note this changed in v2013a_6 from earlier versions of this routine.
     * @param n1 the number of bins in the result.
     * @return rank 2 dataset with n0 by n1 points, with a weights plane.
     * @see #rebin(org.das2.qds.QDataSet, org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     */
    public static QDataSet rebin(QDataSet ds, int n0, int n1) {
        DDataSet result = DDataSet.createRank2( n0, n1);
        DDataSet weights = DDataSet.createRank2( n0, n1);

        QDataSet wds = DataSetUtil.weightsDataSet(ds);

        double fill = ((Number) wds.property( WeightsDataSet.PROP_SUGGEST_FILL )).doubleValue();

        int binSize0= ds.length() / n0;
        int binSize1= ds.length(0) / n1;
        
        if ( binSize0==0 ) throw new IllegalArgumentException("rebin can only be used to reduce data");
        if ( binSize1==0 ) throw new IllegalArgumentException("rebin can only be used to reduce data");
        
        for (int i0 = 0; i0 < n0; i0++) {
            for (int i1 = 0; i1 < n1; i1++) {
                int j0 = i0 * binSize0;
                int j1 = i1 * binSize1;
                double s = 0, w = 0;

                for (int k0 = 0; k0 < binSize0; k0++) {
                    for (int k1 = 0; k1 < binSize1; k1++) {
                        double w1 = wds.value(j0 + k0, j1 + k1);
                        if ( w1>0 ) {
                            w += w1;
                            s += w1 * ds.value(j0 + k0, j1 + k1);
                        }
                    }
                }
                weights.putValue(i0, i1, w);
                result.putValue(i0, i1, w == 0 ? fill : s / w);
            }
        }

        result.putProperty(QDataSet.WEIGHTS, weights);
        result.putProperty(QDataSet.FILL_VALUE, fill);

        QDataSet dep0 = (QDataSet) ds.property(QDataSet.DEPEND_0);
        if (dep0 != null) {
            result.putProperty(QDataSet.DEPEND_0, rebin(dep0, n0));
        }

        QDataSet dep1 = (QDataSet) ds.property(QDataSet.DEPEND_1);
        if (dep1 != null) {
            if ( dep1.rank()!=1 ) throw new IllegalArgumentException("dep1 must be rank 1");
            result.putProperty(QDataSet.DEPEND_1, rebin(dep1, n1));
        }
        DataSetUtil.copyDimensionProperties( ds, result );
        
        return result;
    }

    /**
     * reduce the rank 3 dataset by averaging blocks of bins together.  depend
     * datasets reduced as well.
     * @param ds rank 3 dataset 
     * @param n0 the number of bins in the result.
     * @param n1 the number of bins in the result.
     * @param n2 the number of bins in the result.
     * @return rank 3 dataset ds[n0,n1,n2]
     * @see #rebin(org.das2.qds.QDataSet, org.das2.qds.QDataSet, org.das2.qds.QDataSet) 
     * @see #rebin(org.das2.qds.QDataSet, int) 
     */
    public static QDataSet rebin(QDataSet ds, int n0, int n1, int n2) {
        DDataSet result = DDataSet.createRank3( n0, n1, n2);
        DDataSet weights = DDataSet.createRank3( n0, n1, n2);

        QDataSet wds = DataSetUtil.weightsDataSet(ds);

        double fill = ((Number) wds.property( WeightsDataSet.PROP_SUGGEST_FILL )).doubleValue();

        int binSize0= ds.length() / n0;
        int binSize1= ds.length(0) / n1;
        int binSize2= ds.length(0,0) / n2;
        
        if ( binSize0==0 ) throw new IllegalArgumentException("rebin can only be used to reduce data");
        if ( binSize1==0 ) throw new IllegalArgumentException("rebin can only be used to reduce data");
        if ( binSize2==0 ) throw new IllegalArgumentException("rebin can only be used to reduce data");
        
        for (int i0 = 0; i0 < n0; i0++) {
            for (int i1 = 0; i1 < n1; i1++) {
                for (int i2 = 0; i2 < n2; i2++) {
                    int j0 = i0 * binSize0;
                    int j1 = i1 * binSize1;
                    int j2 = i2 * binSize2;
                    double s = 0, w = 0;

                    for (int k0 = 0; k0 < binSize0; k0++) {
                        for (int k1 = 0; k1 < binSize1; k1++) {
                            for (int k2 = 0; k2 < binSize2; k2++) {
                                double w1 = wds.value(j0 + k0, j1 + k1, j2 + k2 );
                                if ( w1>0 ) {
                                    w += w1;
                                    s += w1 * ds.value(j0 + k0, j1 + k1, j2 + k2 );
                                }
                            }
                        }
                    }
                    weights.putValue(i0, i1, i2, w);
                    result.putValue(i0, i1, i2, w == 0 ? fill : s / w);
                }
            }
        }

        result.putProperty(QDataSet.WEIGHTS, weights);
        result.putProperty(QDataSet.FILL_VALUE, fill);

        QDataSet dep0 = (QDataSet) ds.property(QDataSet.DEPEND_0);
        if (dep0 != null) {
            result.putProperty(QDataSet.DEPEND_0, rebin(dep0, n0));
        }

        QDataSet dep1 = (QDataSet) ds.property(QDataSet.DEPEND_1);
        if (dep1 != null) {
            if ( dep1.rank()!=1 ) throw new IllegalArgumentException("dep1 must be rank 1");
            result.putProperty(QDataSet.DEPEND_1, rebin(dep1, n1));
        }
        
        QDataSet dep2 = (QDataSet) ds.property(QDataSet.DEPEND_2);
        if (dep2 != null) {
            if ( dep2.rank()!=1 ) throw new IllegalArgumentException("dep2 must be rank 1");
            result.putProperty(QDataSet.DEPEND_2, rebin(dep2, n2));
        }
        
        DataSetUtil.copyDimensionProperties( ds, result );
        
        return result;
    }
}
