/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dsutil;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.Units;
import org.das2.util.DasMath;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DRank0DataSet;
import org.virbo.dataset.DataSetIterator;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.IDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.QubeDataSetIterator;
import org.virbo.dataset.RankZeroDataSet;
import org.virbo.dataset.TagGenDataSet;

/**
 * Self-configuring histogram dynamically adjusts range and bin size as data
 * is added.  Also it tries to identify outlier points, which are available
 * as a Map<Double,Integer> going from value to number observed.  Also for
 * each bin, we keep track of a running mean and variance, which are useful for
 * identifying continuous bins and total moments.  Introduced to support
 * automatic cadence algorithm, should be generally useful in data discovery.
 *
 * @author jbf
 */
public final class AutoHistogram {

    public static final String USER_PROP_BIN_START = "binStart";
    public static final String USER_PROP_BIN_WIDTH = "binWidth";
    public static final String USER_PROP_INVALID_COUNT = "invalidCount";
    public static final String USER_PROP_OUTLIERS = "outliers";
    /**
     * Long, total number of valid points.
     */
    public static final String USER_PROP_TOTAL = "total";
    public final int BIN_COUNT = 100;
    private final int INITIAL_BINW = 1;
    private final double INITIAL_BINW_DENOM = 1E30;
    private final double INITIAL_FIRST_BIN = -1 * Double.MAX_VALUE;
    private final double NEW_INITIAL_FIRST_BIN = Double.MAX_VALUE / INITIAL_BINW_DENOM;
    int nbin;
    double binw;      // numerator of binWidth
    double binwDenom; // denominator of binWidth.  Either this or binw will be 1.
    double firstb;
    double firstBin;  // left edge of the first bin, multiplied by binwDenom.  This is so firstBin is never fractional.
    double[] ss; // accumulator for the mean normalized
    double[] vv; // accumulator for the variance (stddev**2) unnormalized
    double[] nn; // count in each bin
    int zeroesRight;
    int zeroesLeft;
    DataSetBuilder timer;
    long t0;
    long total;
    boolean initialOutliers;
    long invalidCount;
    Units units;
    int rescaleCount;  // number of times we rescaled, useful for debugging.
    /**
     * list of outliers and thier count.  When we rescale, we see if any outliers can be added to the distribution.
     */
    SortedMap<Double, Integer> outliers;

    private final static Logger logger= Logger.getLogger(AutoHistogram.class.getCanonicalName());

    private final static void log( java.util.logging.Level level, String message ) {
        if ( logger.isLoggable(level) ) logger.log( level, message);
    }
    //TODO: check to see if this gets in-lined by the compiler.
    private final static void log( java.util.logging.Level level, String message, Object... args ) {
        if ( logger.isLoggable(level) ) logger.log( level, String.format(message,args) );
    }

    public AutoHistogram() {
        reset();
    }

    public void reset() {
        nbin = BIN_COUNT;
        binw = INITIAL_BINW;
        binwDenom = INITIAL_BINW_DENOM;
        firstb = INITIAL_FIRST_BIN; //firstBin done
        firstBin = NEW_INITIAL_FIRST_BIN;
        ss = new double[nbin];
        vv = new double[nbin];
        nn = new double[nbin];
        zeroesRight = nbin / 2;
        zeroesLeft = nbin / 2;
        total = 0;
        initialOutliers = true;
        outliers = new TreeMap<Double, Integer>();
        invalidCount = 0;
        units = null;
        rescaleCount = 0;
    //timer = new DataSetBuilder(1, 1000000);
    //t0 = System.currentTimeMillis();
    }

    /**
     * add the value to the distribution, updateing zeroesLeft and zeroesRight.
     * See http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Weighted_incremental_algorithm
     * vv is "S" in wikipedia  Variance = S * n / ((n-1) * sumweight). Std Dev= sqrt(Variance)
     * @param ibin
     * @param d
     */
    private final void addToDistribution(int ibin, double d, int count) {
        try {
            if (ibin < zeroesLeft) {
                zeroesLeft = ibin;
            }
            if (ibin >= nbin - zeroesRight) {
                zeroesRight = nbin - ibin - 1;
            }
            for (int i = 0; i < count; i++) {
                nn[ibin]++;
                double muj = ss[ibin];
                double delta = d - ss[ibin];
                ss[ibin] = ss[ibin] + delta / nn[ibin];
                double j = nn[ibin] - 1;
                if (j > 0)
                    vv[ibin] = (1 - 1. / j) * vv[ibin] + (j + 1) * Math.pow(ss[ibin] - muj, 2);
                total++;
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw ex;
        }
    }

    /**
     * get the histogram of the data accumulated thus far.
     * @return
     */
    public DDataSet getHistogram() {
        int nonZeroCount = nbin - zeroesLeft - zeroesRight + 2;
        int ifirstBin = zeroesLeft - 1;
        if (ifirstBin < 0) ifirstBin = 0;
        if (nonZeroCount + ifirstBin > nbin) nonZeroCount = nbin - ifirstBin;


        double[] nn1 = new double[nonZeroCount];
        System.arraycopy(nn, ifirstBin, nn1, 0, nonZeroCount);
        double[] ss1 = new double[nonZeroCount];
        System.arraycopy(ss, ifirstBin, ss1, 0, nonZeroCount);
        double[] vv1 = new double[nonZeroCount];
        System.arraycopy(vv, ifirstBin, vv1, 0, nonZeroCount);
        for (int i = 0; i < vv1.length; i++) {
            if (nn1[i] > 0) vv1[i] = Math.sqrt(vv1[i]);
        }
        DDataSet result = DDataSet.wrap(nn1);
        DDataSet means = DDataSet.wrap(ss1);
        if (units != null) means.putProperty(QDataSet.UNITS, units);
        DDataSet stddevs = DDataSet.wrap(vv1);
        if (units != null)
            stddevs.putProperty(QDataSet.UNITS, units.getOffsetUnits());
        means.putProperty(QDataSet.NAME, "means");
        stddevs.putProperty(QDataSet.NAME, "stddevs");
        result.putProperty(QDataSet.PLANE_0, means);
        result.putProperty("means", means);
        result.putProperty("PLANE_1", stddevs);
        result.putProperty("stddevs", stddevs);
        result.putProperty( QDataSet.RENDER_TYPE, "stairSteps" );  //TODO: consider schema id

        //TagGenDataSet dep0 = new TagGenDataSet( nonZeroCount, binw / binwDenom, firstb + binw *firstBin / binwDenom, units ); // firstBin done
        double binWidth= binw / binwDenom;
        //TagGenDataSet dep0 = new TagGenDataSet(nonZeroCount, binWidth, ( firstBin + binw * ( ifirstBin + 0.5 ) ) / binwDenom, units); // bin centers
TagGenDataSet dep0 = new TagGenDataSet(nonZeroCount, binWidth, ( firstBin + binw * ( ifirstBin  ) ) / binwDenom, units); // bin centers

        result.putProperty(DDataSet.DEPEND_0, dep0);
        Map<String, Object> user = new HashMap<String, Object>();
        //user.put(USER_PROP_BIN_START, firstb ); // firstBin done
        user.put(USER_PROP_BIN_START, firstBin / binwDenom);
        user.put(USER_PROP_BIN_WIDTH, binw / binwDenom);
        user.put(USER_PROP_TOTAL, total);
        user.put(USER_PROP_OUTLIERS, outliers);
        user.put(USER_PROP_INVALID_COUNT, invalidCount);
        int outlierCount = 0;
        for (int i : outliers.values()) {
            outlierCount += i;
        }
        user.put("outlierCount", outlierCount);
        result.putProperty(QDataSet.USER_PROPERTIES, user);
        return result;
    }

    /**
     * convenient method for getting the bin location of a value from a completed
     * histogram's metadata.  Note this is inefficient since it must do HashMap
     * lookups to get the bin width and bin start, so use this carefully.
     * @param hist
     * @param d
     * @return the index of the bin for the point.
     */
    public static int binOf( QDataSet hist, double d ) {
        Map<String, Object> user = (Map<String, Object>) hist.property( QDataSet.USER_PROPERTIES );
        double binw= (Double)user.get( USER_PROP_BIN_WIDTH );
        double firstBin= (Double)user.get( USER_PROP_BIN_START );
        return (int) Math.floor( ( d -firstBin ) / binw );
    }

    public QDataSet doit(QDataSet ds) {
        return doit(ds, null);
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
     * @param wds WeightsDataSet or null.
     * @return
     */
    public QDataSet doit(QDataSet ds, QDataSet wds) {

        if (wds == null) {
            wds = DataSetUtil.weightsDataSet(ds);
        }

        Units d1 = (Units) ds.property(QDataSet.UNITS);
        if (d1 != null) units = d1;

        DataSetIterator iter = new QubeDataSetIterator(ds);

        while (iter.hasNext()) {

            iter.next();

            try {
                if (iter.getValue(wds) == 0) {
                    invalidCount++;
                    continue;
                }
            } catch ( IndexOutOfBoundsException ex ) {
                //TODO: it would be nice if we could promote the exception to show iterator and also show cause.
                System.err.println( "Index out of bounds: "+iter );
                throw ex;
            }

            double d = iter.getValue(ds);

            if (initialOutliers) {
                if (outliers.size() < 5) { // collect points as outliers until we have enough points to prime the bins.
                    putOutlier(d);
                    continue;
                } else {
                    initialDist();
                    initialOutliers = false;
                }
            }

            int ibin = binOf(d);

            if (ibin < 0) {
                if (ibin + zeroesRight >= 0) { // shift hist to the left
                    int shift = (int) Math.ceil((zeroesRight + (-ibin)) / 2.);
                    ibin = shiftRight(ibin, shift);
                } else if (ibin < (nbin * -3)) {
                    putOutlier(d);
                    reduceOutliers(Math.max(30, total / 100));
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
                    reduceOutliers(Math.max(30, total / 100));
                    continue;
                } else {
                    while (ibin >= nbin) {
                        ibin = rescaleLeft(ibin, true);
                    }
                }

            }

            addToDistribution(ibin, d, 1);

        //checkTotal();
        }

        if (initialOutliers && outliers.size() > 0) {
            initialDist();
        }

        int limit=10;
        while ( limit>=0 && ( outliers.size()>total ) ) {
            initialRedist();
            limit--;
        }

        while ( limit>=0 && ( outliers.size()>total/10 ) ) {
            initialRedist();
            limit--;
        }

        DDataSet result = getHistogram();

        return result;
    }

    /**
     * see if any outliers can now be added to the distribution
     */
    private void checkOutliers() {
        List<Double> remove = new ArrayList<Double>();
        for (Entry<Double, Integer> out : outliers.entrySet()) {
            int ibin = binOf(out.getKey());
            if (ibin >= 0 && ibin < nbin) {
                remove.add(out.getKey());
                addToDistribution(ibin, out.getKey(), out.getValue());
            } else if (ibin < 0 && ibin + zeroesRight >= 0) { // shift hist to the left
                int shift = (int) Math.ceil((zeroesRight + (-ibin)) / 2.);
                ibin = shiftRight(ibin, shift);
                remove.add(out.getKey());
                addToDistribution(ibin, out.getKey(), out.getValue());
            } else if (ibin >= nbin && ibin - zeroesLeft < nbin) {  // shift hist to the right
                int shift = (int) Math.ceil((zeroesLeft + (ibin - nbin)) / 2.);
                ibin = shiftLeft(ibin, shift);
                remove.add(out.getKey());
                addToDistribution(ibin, out.getKey(), out.getValue());
            }
        }
        for (Double d : remove) {
            outliers.remove(d);
        }
    }

    /**
     * come up with initial distribution by picking the closest two points.
     */
    private void initialDist() {

        double closestA = outliers.firstKey();
        double lastD = closestA;
        double closestB = Double.NaN;
        double closestDist = Double.MAX_VALUE;
        for (Double d : outliers.keySet()) {
            double dist = Math.abs( d - lastD ); // note the outliers are sorted, but this is to be explicit.
            if (dist > 0 && dist < closestDist) {
                closestA = lastD;
                closestB = d;
                closestDist = dist;
            }
            lastD = d;
        }

        binw = Math.abs(closestB - closestA) / (nbin / 100);
        if (binw < 1.0) {
            binwDenom = DasMath.exp10(Math.ceil(DasMath.log10(1 / binw)));
            binw = 1.0;
        } else {
            binw = DasMath.exp10(Math.floor(DasMath.log10(binw)));
            binwDenom = 1.0;
        }

        firstb = Math.floor(closestA * binwDenom / binw) * binw / binwDenom; //firstBin done
        firstBin = Math.floor(closestA * binwDenom / binw) * binw;  // TODO: this probably wrong.

        int count = outliers.remove(closestA);
        int ibin = binOf(closestA);
        zeroesLeft = ibin;
        zeroesRight = nbin - ibin - 1; // allow invalid state because checkOutliers will fix it.
        addToDistribution(ibin, closestA, count);

        checkOutliers();
    }

    /**
     * we have a distribution that results in more outliers than data.  Recalculate
     * the bin width until there are fewer outliers than data.
     * This asserts that the closest outlier is actually part of the distribution.
     */
    private void initialRedist() {

        double distCenter= firstb + binw / binwDenom * ( zeroesLeft + ( nbin-zeroesRight-zeroesLeft ) / 2 );
        double closestA = outliers.firstKey();
        double lastD = closestA;
        double closestDist = Double.MAX_VALUE;

        for (Double d : outliers.keySet()) {
            double dist = Math.abs( d - distCenter );
            if (dist > 0 && dist < closestDist) {
                closestA = lastD;
                closestDist = dist;
            }
            lastD = d;
        }

        // grow the distribution bins until we include this point.
        int ibin = binOf(closestA);
        while ( ibin<0 ) {
            ibin= rescaleRight(ibin);
        }
        while ( ibin>nbin ) {
            ibin= rescaleLeft(ibin,false);
        }
        checkOutliers();

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
    private void reduceOutliers(long limit) {
        //int outlierCount=0;
        //for ( Entry<Double,Integer> e: outliers.entrySet() ) {
        //    outlierCount+= e.getValue();
        //}
        //while (outlierCount > limit) {
        while (outliers.size() > limit) {
            //double d0 = firstb + binw / binwDenom * (zeroesLeft); // firstBin done
            double d0 = firstBin / binwDenom + binw / binwDenom * (zeroesLeft);
            SortedMap<Double, Integer> headmap = outliers.headMap(d0);
            //double d1 = firstb + binw / binwDenom * (nbin - zeroesRight); // firstBin done
            double d1 = firstBin / binwDenom + binw / binwDenom * (nbin - zeroesRight);
            SortedMap<Double, Integer> tailmap = outliers.tailMap(d1);
            if (headmap.size() == 0) {
                rescaleLeft(0, true);
            } else if (tailmap.size() == 0) {
                rescaleRight(0);
            } else if (d0 - headmap.lastKey() > tailmap.firstKey() - d1) {
                rescaleLeft(0, true);
            } else {
                rescaleRight(0);
            }
            checkOutliers();
         //   outlierCount=0;
         //   for ( Entry<Double,Integer> e: outliers.entrySet() ) {
         //      outlierCount+= e.getValue();
         //   }
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
            throw new IllegalArgumentException("total check fails");
        }
        for (int i = 0; i < vv.length; i++) {
            double d1 = vv[i];
            if (Double.isNaN(d1)) {
                throw new IllegalArgumentException("nan in variance");
            }
            if (nn[i] < 2 && vv[i] > 0) {
                throw new IllegalArgumentException("non-zero variance in less than two bins in bin #" + i);
            }
        }

        if (Math.abs(firstb - (firstBin / binwDenom)) > binwDenom / 1000) {
            throw new IllegalArgumentException("binw denom");
        }
    }

    private void debugDump() {


        DecimalFormat df = new DecimalFormat(" 00000");
        DecimalFormat nf = new DecimalFormat("00.000");

        System.err.println("-----------------------------");

        long total1 = 0;
        for (int i = 0; i < 20; i++) {
            double d1 = nn[i];
            System.err.print(" " + df.format(d1));
        }
        System.err.println();

        for (int i = 0; i < 20; i++) {
            double d1 = ss[i];
            System.err.print(" " + nf.format(d1));
        }
        System.err.println();

        for (int i = 0; i < 20; i++) {
            double d1 = vv[i];
            System.err.print(" " + nf.format(d1));
        }
        System.err.println();

    }
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);

    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    private final int binOf(double d) {
        //int ibin= (int) Math.floor((d - firstb) * binwDenom / binw); // firstBin done
        return (int) Math.floor((d * binwDenom - firstBin) / binw);
    }

    private final int shiftLeft(int ibin, int shift) {
        if ( logger.isLoggable(java.util.logging.Level.FINEST) ) logger.finest( String.format("shiftLeft(%d)\n", shift) ) ;
        // shift hist to the left
        checkTotal();
        System.arraycopy(ss, shift, ss, 0, nbin - shift - zeroesRight);
        System.arraycopy(nn, shift, nn, 0, nbin - shift - zeroesRight);
        System.arraycopy(vv, shift, vv, 0, nbin - shift - zeroesRight);
        zeroesRight += shift;
        zeroesLeft -= shift;
        ibin -= shift;
        Arrays.fill(ss, nbin - zeroesRight, nbin, 0.);
        Arrays.fill(nn, nbin - zeroesRight, nbin, 0.);
        Arrays.fill(vv, nbin - zeroesRight, nbin, 0.);
        this.firstb += binw * shift / binwDenom; // firstBin done
        this.firstBin += binw * shift;
        checkTotal();
        return ibin;
    }

    private final int expandAndShiftRight(int ibin, int shift, int factor) {
        log( Level.FINEST, "expandAndShiftRight(%d,%d,%d)\n", ibin, shift, factor );
        factor = factor * 2; // TODO: kludge
        checkTotal();
        int nbin1 = (int) Math.ceil((nbin + factor) / (1. * factor)) * factor;
        double[] nn1 = new double[nbin1];
        double[] ss1 = new double[nbin1];
        double[] vv1 = new double[nbin1];
        System.arraycopy(ss, zeroesLeft, ss1, shift + zeroesLeft, nbin - zeroesLeft);
        System.arraycopy(nn, zeroesLeft, nn1, shift + zeroesLeft, nbin - zeroesLeft);
        System.arraycopy(vv, zeroesLeft, vv1, shift + zeroesLeft, nbin - zeroesLeft);

        zeroesLeft += shift;
        zeroesRight -= shift;
        zeroesRight += nbin1 - nbin;
        ibin += shift;
        nbin = nbin1;

        ss = ss1;
        nn = nn1;
        vv = vv1;

        Arrays.fill(ss, 0, zeroesLeft, 0.);
        Arrays.fill(nn, 0, zeroesLeft, 0.);
        Arrays.fill(vv, 0, zeroesLeft, 0.);
        Arrays.fill(ss, nbin - zeroesRight, nbin, 0.);
        Arrays.fill(nn, nbin - zeroesRight, nbin, 0.);
        Arrays.fill(vv, nbin - zeroesRight, nbin, 0.);

        this.firstb -= binw * shift / binwDenom; // firstBin done
        this.firstBin -= binw * shift;
        checkTotal();
        return ibin;
    }

    private final int shiftRight(int ibin, int shift) {
        log( Level.FINEST, "shiftRight(%d)\n", shift);
        // shift hist to the right
        checkTotal();
        System.arraycopy(ss, zeroesLeft, ss, shift + zeroesLeft, nbin - zeroesLeft - shift);
        System.arraycopy(nn, zeroesLeft, nn, shift + zeroesLeft, nbin - zeroesLeft - shift);
        System.arraycopy(vv, zeroesLeft, vv, shift + zeroesLeft, nbin - zeroesLeft - shift);
        zeroesLeft += shift;
        zeroesRight -= shift;
        ibin += shift;
        Arrays.fill(ss, 0, zeroesLeft, 0.);
        Arrays.fill(nn, 0, zeroesLeft, 0.);
        Arrays.fill(vv, 0, zeroesLeft, 0);
        this.firstb -= binw * shift / binwDenom; // firstBin done
        this.firstBin -= binw * shift;
        checkTotal();
        return ibin;
    }

    private int nextFactor() {
        int factor;
        int exp = (int) Math.floor(DasMath.log10(binw) + 0.001);
        int mant = (int) Math.round(binw / DasMath.exp10(exp));
        int expDenom = (int) Math.floor(DasMath.log10(binwDenom));
        int mantDenom = (int) Math.round(binwDenom / DasMath.exp10(expDenom));
        if (mantDenom > 1) {
            mant = 10 / mantDenom;
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
     * last bin has same end, but is ten times wider, and then shift.
     * @param ibin
     * @return
     */
    private final int rescaleRight(int ibin) {
        int factor = nextFactor();
        ibin = rescaleLeft(ibin, false);
        try {
            ibin = shiftRight(ibin, nbin * (factor - 1) / factor);
        } catch (ArrayIndexOutOfBoundsException ex) {
            ibin = shiftRight(ibin, nbin * (factor - 1) / factor);
        }
        checkOutliers();
        return ibin;
    }

    /**
     * first bin has same start, but bin width is nextFactor() wider.
     * @param ibin
     * @return
     */
    private final int rescaleLeft(int ibin, boolean checkOutliers) {
        rescaleCount++;
        int factor = nextFactor();
        log( Level.FINEST, "rescaleLeft to " + binw / binwDenom + "*" + factor);
        checkTotal();
        // how many bins must we shift to get a nice initial bin?
        int shift = (int) Math.round(DasMath.modp(firstBin, (binw * factor)) / (binw));
        if (nbin % factor > 0) {
            ibin = expandAndShiftRight(ibin, shift, factor);
        } else {
            if (shift > 0) {
                //shift = shift;
                if (shift < zeroesRight) {
                    ibin = shiftRight(ibin, shift);
                } else {
                    ibin = expandAndShiftRight(ibin, shift, factor);
                }
            }
        }
        for (int i = 0; i < nbin / factor; i++) {
            nn[i] = nn[i * factor];
            double[] oldMeans = new double[factor];
            double[] oldWeights = new double[factor];
            double[] oldVariances = new double[factor];
            oldMeans[0] = ss[i * factor];
            oldWeights[0] = nn[i * factor];
            oldVariances[0] = vv[i * factor];
            ss[i] = ss[i * factor] * nn[i * factor];
            for (int j = 1; j < factor; j++) {
                int idx = i * factor + j;
                oldMeans[j] = ss[idx];
                oldWeights[j] = nn[idx];
                oldVariances[j] = vv[idx];
                ss[i] += ss[idx] * nn[idx];
                nn[i] += nn[idx];
            }
            if (nn[i] > 0) {
                ss[i] /= nn[i];
            }
            // move the correct mean of the bin to the incorrect mean of the factor bins
            for (int j = 0; j < factor; j++) {
                if (oldWeights[j] > 0) {
                    oldVariances[j] = ((oldWeights[j] - 1) * oldVariances[j] + oldWeights[j] * Math.pow(oldMeans[j] - ss[i], 2));
                } else {
                    oldVariances[j] = 0.0;
                }
            }

            // combine the variances with a weighted average
            vv[i] = oldVariances[0];

            for (int j = 1; j < factor; j++) {
                vv[i] += oldVariances[j];
            }
            if (nn[i] > 1) {
                vv[i] /= (nn[i] - 1);
            }

        }

        int nnew = (nbin - nbin / factor);
        Arrays.fill(ss, nbin / factor, nbin, 0.);
        Arrays.fill(nn, nbin / factor, nbin, 0.);
        Arrays.fill(vv, nbin / factor, nbin, 0.);

        if (binwDenom > 1.0) {
            binwDenom = binwDenom / factor;
            firstBin = firstBin / factor;
        } else {
            binw = binw * factor;
        }
        ibin = ibin / factor;
        zeroesLeft = zeroesLeft / factor;
        zeroesRight = zeroesRight / factor + nnew;
        checkTotal();
        if (checkOutliers) checkOutliers();
        return ibin;
    }

    /**
     * returns the mean of the dataset that has been histogrammed.
     * @param hist
     * @return
     */
    public static RankZeroDataSet mean(QDataSet hist) {
        double SS = 0;
        double NN = 0;
        QDataSet means = (QDataSet) hist.property("means");

        for (int i = 0; i < hist.length(); i++) {
            SS += means.value(i) * hist.value(i);
            NN += hist.value(i);
        }
        DRank0DataSet ds = DataSetUtil.asDataSet(SS / NN);
        ds.putProperty(QDataSet.UNITS, ((QDataSet) hist.property(QDataSet.DEPEND_0)).property(QDataSet.UNITS));
        return ds;
    }


    /**
     * returns the mean of the dataset that has been histogrammed.
     * @param hist a rank 1 dataset with each bin containing the count in each bin.  DEPEND_0 are the labels for
     *     each bin.  The property "means" returns a rank 1 dataset containing the means for each bin.  The
     *     property "stddevs" contains the standard deviation within each bin.
     * @return rank 0 dataset (a Datum) whose value is the mean, and the property("stddev") contains the standard deviation
     */
    public static RankZeroDataSet moments(QDataSet hist) {
        double[] vvs = new double[hist.length()];
        double mean = mean(hist).value();

        QDataSet stddevs = (QDataSet) hist.property("stddevs");
        QDataSet means = (QDataSet) hist.property("means");

        for (int i = 0; i < stddevs.length(); i++) {
            double var = Math.pow(stddevs.value(i), 2);
            vvs[i] = (hist.value(i) - 1) * var + hist.value(i) * Math.pow(means.value(i) - mean, 2);
        }

        double VV = 0;
        long total = (Long) (((Map) hist.property(QDataSet.USER_PROPERTIES)).get(USER_PROP_TOTAL));

        for (int i = 0; i < hist.length(); i++) {
            VV += vvs[i];
        }

        double stddev = Math.sqrt(VV / (total - 1));

        Units u = (Units) ((QDataSet) hist.property(QDataSet.DEPEND_0)).property(QDataSet.UNITS);

        DRank0DataSet result = DataSetUtil.asDataSet(mean);
        if (u != null) result.putProperty(QDataSet.UNITS, u);
        DRank0DataSet stddevds = DataSetUtil.asDataSet(stddev);
        if (u != null) stddevds.putProperty(QDataSet.UNITS, u.getOffsetUnits());
        result.putProperty("stddev", stddevds);
        result.putProperty("validCount", total);
        result.putProperty("invalidCount", ((Map) hist.property(QDataSet.USER_PROPERTIES)).get(USER_PROP_INVALID_COUNT));

        return result;
    }

    /**
     * return a list of all the peaks in the histogram.  A peak is defined as a
     * local maximum, then including the adjacent bins consistent with the peak
     * population, and not belonging to another peak.  
     * @param hist
     * @return QDataSet covarient with hist.
     */
    public static QDataSet peakIds( QDataSet hist ) {
        IDataSet peakId= IDataSet.createRank1(hist.length());
        peakId.putProperty( QDataSet.DEPEND_0, hist.property(QDataSet.DEPEND_0 ) );
        int ipeak=1;
        int n= hist.length();

        if ( n<3 ) {
            throw new IllegalArgumentException("histogram has too few bins");
        }

        QDataSet means= (QDataSet) ( hist.property("means") );
        QDataSet stddevs= (QDataSet) ( hist.property("stddevs") );
        QDataSet bins= (QDataSet) hist.property( QDataSet.DEPEND_0 );
        double binw= bins.value(1)- bins.value(0);

        // seed the peaks by finding local maximums of 5-point groups.  Also,
        // identify valley bottoms.
        // 0= undetermined, >0 peak, -1= valley.
        for ( int i=0; i<n-2; i++ ) { // a peak is a local max, or the tail end of a plateau.
            if ( (i<2 || hist.value(i-2)<=hist.value(i) )
                   && ( i<1 || hist.value(i-1)<=hist.value(i) )
                   && ( i>n-2 || hist.value(i)>hist.value(i+1) )
                   && ( i>n-1 || hist.value(i)>hist.value(i+2) ) ) {
                peakId.putValue(i,ipeak);
                ipeak++;
            } else if ( (i<2 || hist.value(i-2)>hist.value(i) )
                   && ( i<1 || hist.value(i-1)>hist.value(i) )
                   && ( i>n-2 || hist.value(i)<=hist.value(i+1) )
                   && ( i>n-1 || hist.value(i)<=hist.value(i+2) ) ) {
                peakId.putValue(i,-1);
            }
        }

        // move the peakId down along plateau
        for ( int i=n-1; i>=1; i-- ) {
            if ( hist.value(i-1)==hist.value(i) && peakId.value(i)!=0 ) {
                if ( hist.value(i-1)>5 ) {
                    if ( means.value(i-1) + 2 * stddevs.value(i-1) > ( bins.value(i)-binw/2) ) {
                        peakId.putValue(i-1,peakId.value(i));
                    } else {
                        peakId.putValue( i-1,ipeak );
                        ipeak++;
                    }
                } else {
                    peakId.putValue(i-1,peakId.value(i));
                }
            }
        }

        // expand peaks to the left.
        for ( int i=1; i<n; i++ ) {
            if ( peakId.value(i)>0 ) {
                int j=i-1;
                double peakHeight= hist.value(i);
                while ( j>=0 && peakId.value(j)==0 && hist.value(j)>peakHeight/10 && ( means.value(j) + 2 * stddevs.value(j) > ( bins.value(j+1)-binw/2) ) ) {
                    peakId.putValue( j, peakId.value(i) );
                    j--;
                }
            }
        }

        // expand peaks to the right.
        for ( int i=n-2; i>=0; i-- ) {
            if ( peakId.value(i)>0 ) {
                int j=i+1;
                double peakHeight= hist.value(i);
                while ( j<n && peakId.value(j)==0 && hist.value(j)>peakHeight/10 && ( means.value(j) - 2 * stddevs.value(j) < ( bins.value(j-1)+binw/2) ) ) {
                    peakId.putValue( j, peakId.value(i) );
                    j++;
                }
            }
        }

        return peakId; //TODO

    }

    /**
     * return a list of all the peaks in the histogram.  See peakIds to see
     * how peaks are identified.  Once the bins of a peak
     * have been identified, then the mean and stddev of each peak is returned.
     * Note the stddev typically reads low, probably because the tails have been removed.
     * @return QDataSet rank 1 dataset with length equal to the number of identified peaks
     */
    public static QDataSet peaks( QDataSet hist ) {
        QDataSet peakIds= peakIds(hist);
        QDataSet stddevs = (QDataSet) hist.property("stddevs");
        QDataSet means = (QDataSet) hist.property("means");

        int maxPeak=0;

        for ( int i=0; i<peakIds.length(); i++ ) {
            if ( peakIds.value(i)> maxPeak ) {
                maxPeak= (int) peakIds.value(i);
            }
        }

        int npeak= maxPeak;

        double[] vvs = new double[hist.length()];
        double[] SS= new double[hist.length()];
        double[] NN= new double[hist.length()];

        for (int i = 0; i < hist.length(); i++) {
            int ipeak= (int) (peakIds.value(i) - 1);
            if ( ipeak>=0 ) {
                SS[ipeak] += means.value(i) * hist.value(i);
                NN[ipeak] += hist.value(i);
            }
        }

        double[] mean= new double[npeak];

        for ( int ipeak=0; ipeak<npeak; ipeak++ ) {
            mean[ipeak]= SS[ipeak]/NN[ipeak];
        }

        for (int i = 0; i < stddevs.length(); i++) {
            double var = Math.pow(stddevs.value(i), 2);
            int ipeak= (int) (peakIds.value(i) - 1);
            if ( ipeak>=0 ) {
                vvs[i] = (hist.value(i) - 1) * var + hist.value(i) * Math.pow(means.value(i) - mean[ipeak], 2);
            }
        }

        double[] VV = new double[npeak];
        int[] nbin= new int[npeak];

        for (int i = 0; i < hist.length(); i++) {
            int ipeak= (int) (peakIds.value(i) - 1);
            if ( ipeak>=0 ) {
                VV[ipeak] += vvs[i];
                nbin[ipeak] += 1;
            }
        }

        double[] stddev= new double[npeak];

        for ( int ipeak=0; ipeak<npeak; ipeak++ ) {
            stddev[ipeak]= Math.sqrt(VV[ipeak] / (NN[ipeak] - 1));
        }

        Units u = (Units) ((QDataSet) hist.property(QDataSet.DEPEND_0)).property(QDataSet.UNITS);

        DDataSet result= DDataSet.wrap( mean );
        if (u != null) result.putProperty(QDataSet.UNITS, u);
        DDataSet stddevds= DDataSet.wrap( stddev );
        if (u != null) stddevds.putProperty( QDataSet.UNITS, u.getOffsetUnits() );
        result.putProperty( QDataSet.DELTA_PLUS, stddevds );
        result.putProperty( QDataSet.DELTA_MINUS, stddevds );

        return result;

    }
}
