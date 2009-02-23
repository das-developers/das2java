/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dsutil;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import org.das2.util.DasMath;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DataSetIterator;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.QubeDataSetIterator;
import org.virbo.dataset.TagGenDataSet;

/**
 * Self-configuring histogram dynamically adjusts range and bin size as data
 * is added.  Introduced to support automatic cadence algorithm, should be
 * generally useful in data discovery.
 *
 * @author jbf
 */
public final class AutoHistogram {

    public final int BIN_COUNT = 100;
    private final int INITIAL_BINW = 1;
    private final double INITIAL_BINW_DENOM = 1E30;
    private final double INITIAL_FIRST_BIN = -1 * Double.MAX_VALUE;
    int nbin;
    double binw;
    double binwDenom;
    double firstb;
    double[] ss;
    double[] nn;
    int zeroesRight;
    int zeroesLeft;
    DataSetBuilder timer;
    long t0;
    long total;
    /**
     * list of outliers and thier count.  When we rescale, we see if any outliers can be added to the distribution.
     */
    SortedMap<Double, Integer> outliers;

    public AutoHistogram() {
        reset();
    }

    public void reset() {
        nbin = BIN_COUNT;
        binw = INITIAL_BINW;
        binwDenom = INITIAL_BINW_DENOM;
        firstb = INITIAL_FIRST_BIN;
        ss = new double[nbin];
        nn = new double[nbin];
        zeroesRight = nbin / 2;
        zeroesLeft = nbin / 2;
        total = 0;
        outliers = new TreeMap<Double, Integer>();
        //timer = new DataSetBuilder(1, 1000000);
        //t0 = System.currentTimeMillis();
    }

    /**
     * add the value to the distribution, updateing zeroesLeft and zeroesRight.
     * @param ibin
     * @param d
     */
    private final void addToDistribution( int ibin, double d, int count ) {
        if (ibin < zeroesLeft) {
            zeroesLeft = ibin;
        }
        if (ibin >= nbin - zeroesRight) {
            zeroesRight = nbin - ibin - 1;
        }
        nn[ibin]+= count;
        ss[ibin] += d*count;
        total+= count;
    }

    private DDataSet asDataSet() {
        DDataSet result = DDataSet.wrap(nn);
        DDataSet totals = DDataSet.wrap(ss);
        totals.putProperty(QDataSet.NAME, "total");
        result.putProperty(QDataSet.PLANE_0, totals);
        QDataSet dep0 = new TagGenDataSet(nbin, binw / binwDenom, firstb);
        result.putProperty(DDataSet.DEPEND_0, dep0);
        Map<String, Object> user = new HashMap<String, Object>();
        user.put("binStart", firstb);
        user.put("binWidth", binw / binwDenom);
        user.put("total", total);
        user.put("outliers",outliers);
        int outlierCount=0;
        for ( int i : outliers.values() )  outlierCount+= i;
        user.put("outlierCount",outlierCount);
        result.putProperty(QDataSet.USER_PROPERTIES, user);
        return result;
    }

    //public QDataSet getTiming() {
    //    return timer.getDataSet();
    //}

    /**
     * do a histogram, dynamically shifting the bins and changing the bin size.
     * returns a QDataSet with the planes:
     *   each bin has the number of points in each bin.
     *   total  total of values in the bin
     *   depend_0 is the bin names.
     * The property QDataSet.USER_PROPERTIES contains a map with the following
     * keys:
     *   binStart, Double, the left side of the first bin.
     *   binWidth, Double, the bin width.
     *   total, Integer, the number of points in the distribution.
     *   outliers, Map<Double,Integer>, outliers and the count of each outlier.
     * @param ds
     * @return
     */
    public QDataSet doit(QDataSet ds) {
        DataSetIterator iter = new QubeDataSetIterator(ds);

        if (firstb == INITIAL_FIRST_BIN) {
            // prime the firstb
            if (iter.hasNext()) {
                iter.next();
                double d = iter.getValue(ds);
                firstb = Math.floor(d * binwDenom / binw) * binw / binwDenom;
                ss[0] += d;
                nn[0] += 1;
                total++;
            }
        }

        if (binw == INITIAL_BINW && binwDenom == INITIAL_BINW_DENOM) {
            // prime the binw
            if (iter.hasNext()) {
                iter.next();
                double d = iter.getValue(ds);
                int ibin = binOf(d);
                while (ibin == 0) {
                    ss[0] += d;
                    nn[0] += 1;
                    if (iter.hasNext()) {
                        iter.next();
                        d = iter.getValue(ds);
                        ibin = binOf(d);
                    } else {
                        break;
                    }
                }
                binw = Math.abs(d - ss[0] / nn[0]) / (nbin / 100);
                if (binw < 1.0) {
                    binwDenom = DasMath.exp10(Math.ceil(DasMath.log10(1 / binw)));
                    binw = 1.0;
                } else {
                    binw = DasMath.exp10(Math.floor(DasMath.log10(binw)));
                    binwDenom = 1.0;
                }

                ibin = binOf(d);
                if (ibin < 0) {
                    ss[-ibin] = ss[0];
                    nn[-ibin] = nn[0];
                    ss[0] = d;
                    nn[0] = 1;
                    firstb = firstb - ibin * binw / binwDenom;
                    ibin = -ibin;
                } else {
                    ss[ibin] = d;
                    nn[ibin] = 1;
                }
                total++;
                zeroesRight = nbin - ibin - 1;
                zeroesLeft = 0;
            }

        //System.err.printf("initial zeroesLeft %d  zeroesRight %d  binw=%f   binwDemon=%f\n", zeroesLeft, zeroesRight, binw, binwDenom);
        }

        while (iter.hasNext()) {

            iter.next();

            double d = iter.getValue(ds);

            //if (iter.index(0) % 10 == 0) {
            //    timer.putValue(-1, System.currentTimeMillis() - t0);
            //    timer.nextRecord();
            //}

            int ibin = binOf(d);

            if (ibin < 0) {
                if (ibin + zeroesRight >= 0) { // shift hist to the left
                    int shift = (int) Math.ceil((zeroesRight + (-ibin)) / 2.);
                    ibin = shiftRight(ibin, shift);
                } else if (ibin < nbin * 3) {
                    putOutlier(d);
                    reduceOutliers( Math.max(30,total/100) );
                    continue;
                } else {
                    while (ibin < 0) {
                        ibin = rescaleRight(ibin);
                    }
                }

            } else if (ibin >= nbin) {
                if (ibin - zeroesLeft < nbin) {  // shift hist to the right
                    int shift = (int) Math.ceil((zeroesLeft + (ibin - nbin)) / 2.);
                    ibin = shiftLeft(ibin, shift);
                } else if (ibin > nbin * 4) {
                    putOutlier(d);
                    reduceOutliers( Math.max(30,total/100) );
                    continue;
                } else {
                    while (ibin >= nbin) {
                        ibin = rescaleLeft(ibin);
                    }
                }

            }

            addToDistribution(ibin,d,1);

            //checkTotal();
        }

        DDataSet result = asDataSet();

        return result;
    }

    /**
     * see if any outliers can now be added to the distribution
     */
    private void checkOutliers() {
        List<Double> remove= new ArrayList<Double>();
        for ( Entry<Double,Integer> out: outliers.entrySet() ) {
            int ibin= binOf(out.getKey());
            if ( ibin>=0 && ibin<nbin ) {
                remove.add(out.getKey());
                addToDistribution( ibin, out.getKey(), out.getValue() );
            }
        }
        for ( Double d: remove ) {
            outliers.remove(d);
        }
    }

    /**
     * add the value to the outliers list.
     * @param d
     */
    private void putOutlier(double d) {
        Integer count = outliers.get(d);
        if (count == null) {
            outliers.put(d, 1);
        } else {
            outliers.put(d, count + 1);
        }
    }

    /**
     * reduce the number of outliers by scaling the distribution to include
     * the closest outliers
     */
    private void reduceOutliers( long limit ) {
        while ( outliers.size()>limit ) {
            double d0= firstb + binw/binwDenom * ( zeroesLeft );
            SortedMap<Double,Integer> headmap= outliers.headMap( d0 );
            double d1= firstb + binw/binwDenom * ( nbin - zeroesRight );
            SortedMap<Double,Integer> tailmap= outliers.tailMap( d1 );
            if ( headmap.size()==0 ) {
                rescaleLeft(0);
            } else if ( tailmap.size()==0 ) {
                rescaleRight(0);
            } else if ( d0 - headmap.lastKey()  >  tailmap.firstKey() - d1 ) {
                rescaleLeft(0);
            } else {
                rescaleRight(0);
            }
            checkOutliers();
        }
    }

    /**
     * verify that the distribution is consistent with the number of points fed in.
     * This was used in initial coding for tracking down bugs.
     */
    private void checkTotal() {
        long total1 = 0;
        for (double d1 : nn) {
            total1 += d1;
        }
        if (total != total1) {
            throw new IllegalArgumentException("hello!");
        }
    }
    
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);

    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    private final int binOf(double d) {
        return (int) Math.floor((d - firstb) * binwDenom / binw);
    }

    private final int shiftLeft(int ibin, int shift) {
        System.err.printf("shiftLeft(%d)\n", shift);
        // shift hist to the left
        checkTotal();
        System.arraycopy(ss, shift, ss, 0, nbin - shift - zeroesRight);
        System.arraycopy(nn, shift, nn, 0, nbin - shift - zeroesRight);
        zeroesRight += shift;
        zeroesLeft -= shift;
        ibin -= shift;
        Arrays.fill(ss, nbin - zeroesRight, nbin, 0.);
        Arrays.fill(nn, nbin - zeroesRight, nbin, 0.);
        this.firstb += binw * shift / binwDenom;
        checkTotal();
        return ibin;
    }

    private final int shiftRight(int ibin, int shift) {
        System.err.printf("shiftRight(%d)\n", shift);
        // shift hist to the right
        checkTotal();
        System.arraycopy(ss, zeroesLeft, ss, shift + zeroesLeft, nbin - zeroesLeft - shift);
        System.arraycopy(nn, zeroesLeft, nn, shift + zeroesLeft, nbin - zeroesLeft - shift);
        zeroesLeft += shift;
        zeroesRight -= shift;
        ibin += shift;
        Arrays.fill(ss, 0, zeroesLeft, 0.);
        Arrays.fill(nn, 0, zeroesLeft, 0.);
        this.firstb -= binw * shift / binwDenom;
        checkTotal();
        return ibin;
    }

    private int nextFactor() {
        int factor;
        int exp = (int) Math.floor(DasMath.log10(binw)+0.001);
        int mant = (int) Math.round(binw / DasMath.exp10(exp));
        int expDenom = (int) Math.floor(DasMath.log10(binwDenom));
        int mantDenom = (int) Math.round(binwDenom / DasMath.exp10(expDenom));
        if ( mantDenom>1 ) {
            mant= 10 / mantDenom;
        }
        if (mant == 1) {
            factor = 5;
        } else if (mant == 5) {
            factor = 2;
        } else {
            throw new IllegalArgumentException();
        }
        return factor;
    }

    /**
     * first bin has same start, but is ten times wider
     * @param ibin
     * @return
     */
    private final int rescaleLeft(int ibin) {
        int factor = nextFactor();
        System.err.println("rescaleLeft to " + binw / binwDenom + "*" + factor);
        checkTotal();
        // how many bins must we shift to get a nice initial bin?
        int shift = (int) Math.round(DasMath.modp(firstb, (binw * factor / binwDenom)) / (binw / binwDenom));
        if (shift > 0) {
            shift = factor - shift;
            if (shift < zeroesRight) {
                ibin = shiftRight(ibin, shift);
            }
        }
        for (int i = 0; i < nbin / factor; i++) {
            ss[i] = ss[i * factor];
            nn[i] = nn[i * factor];
            for (int j = 1; j < factor; j++) {
                int idx = i * factor + j;
                ss[i] += ss[idx];
                nn[i] += nn[idx];
            }
        }
        int nnew = (nbin - nbin / factor);
        Arrays.fill(ss, nbin / factor, nbin, 0.);
        Arrays.fill(nn, nbin / factor, nbin, 0.);
        if (binwDenom > 1.0) {
            binwDenom = binwDenom / factor;
        } else {
            binw = binw * factor;
        }
        ibin = ibin / factor;
        zeroesLeft = zeroesLeft / factor;
        zeroesRight = zeroesRight / factor + nnew;
        checkTotal();
        checkOutliers();
        return ibin;
    }

    /**
     * last bin has same end, but is ten times wider
     * @param ibin
     * @return
     */
    private final int rescaleRight(int ibin) {
        int factor = nextFactor();
        System.err.println("rescaleRight to " + binw / binwDenom + "*" + factor);
        checkTotal();
        // how many bins must we shift to get a nice initial bin?
        int shift = (int) Math.round(DasMath.modp(firstb, (binw * factor / binwDenom)) / (binw / binwDenom));
        if (shift > 0) {
            if (shift < zeroesLeft) {
                ibin = shiftLeft(ibin, shift);
            }
        }
        int lbin = nbin - 1; //last bin
        for (int i = 0; i < nbin / factor; i++) {
            ss[lbin - i] = ss[lbin - i * factor];
            nn[lbin - i] = nn[lbin - i * factor];
            for (int j = 1; j < factor; j++) {
                int idx = lbin - i * factor - j;
                if (idx != lbin) {
                    ss[lbin - i] += ss[idx];
                    nn[lbin - i] += nn[idx];
                }
            }
        }
        int nnew = nbin - nbin / factor;
        Arrays.fill(ss, 0, nnew, 0.);
        Arrays.fill(nn, 0, nnew, 0.);
        firstb = firstb - nnew * binw;
        if (binwDenom > 1.0) {
            binwDenom = binwDenom / factor;
        } else {
            binw = binw * factor;
        }
        ibin = nbin - (int) Math.ceil((nbin - ibin) / factor);
        zeroesRight = zeroesRight / factor;
        zeroesLeft = zeroesLeft / factor + nnew;
        checkTotal();
        checkOutliers();
        return ibin;
    }
}
