/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.datum;

import java.util.ArrayList;
import java.util.List;
import org.das2.datum.format.DatumFormatter;
import org.das2.datum.format.TimeDatumFormatter;

/**
 *
 * @author jbf
 */
public class OrdinalTimeDomainDivider implements DomainDivider {

    /**
     * indicates if the first ordinal value of the field is zero or one.
     */
    private final static int[] ZEROONE = new int[]{0, 1, 1, 0, 0, 0, 0, 0};
    private final static int[] MODULO = new int[]{10000, 12, 30, 24, 60, 60, 1000, 1000};
    private static int N_DIGITS = 8;
    private final static List<Integer>[] FACTORS = new List[N_DIGITS];


    static {
        for (int i = 0; i < N_DIGITS; i++) {
            FACTORS[i] = factors(primeFactors(MODULO[i])); // 10,000 years
        }
    }
    private int significand;
    private int digit;
    private final static int ARR_YEAR = 0;
    private final static int ARR_MONTH = 1;
    private final static int ARR_DAY = 2;
    private final static int ARR_HOUR = 3;
    private final static int ARR_MINUTE = 4;
    private final static int ARR_SECOND = 5;
    private final static int ARR_MILLIS = 6;
    private final static int ARR_MICROS = 7;

    /**
     *
     * @param significand multiplier for digit.
     * @param digit ordinal ARR_YEAR, ARR_MONTH, etc.
     */
    private OrdinalTimeDomainDivider(int significand, int digit) {
        this.significand = significand;
        this.digit = digit;
    }

    protected OrdinalTimeDomainDivider() {
        this(1, TimeUtil.HOUR - 1);
    }

    /**
     * return a list of the prime factors for the number.
     * @param N
     * @return
     */
    private static List<Integer> primeFactors(int N) {
        List<Integer> result = new ArrayList<Integer>();
        while (N % 10 == 0) { // favor tens
            result.add(2);
            result.add(5);
            N = N / 10;
        }
        for (int i = 2; i <= N; i++) {
            while (N % i == 0) {
                result.add(i);
                N = N / i;
            }
        }
        return result;
    }

    /**
     * calculate a list of all factors from the prime factors.  I'm sure there
     * is a more elegant way of doing this.
     * @param primeFactors, the prime factors of the digit, sorted. e.g. base 10->2,5  24->2,2,2,3
     * @return a list of factors for the digit.
     */
    private static List<Integer> factors(List<Integer> primeFactors) {
        List<Integer> result = new ArrayList<Integer>();
        result.add(1);
        int count = 1;
        int c = (int) Math.pow(2, primeFactors.size());

        //I'm sure there is a more elegant way of doing this, probably skipping when the lower digits reset to zero.
        for (int i = 0; i < c; i++) {
            int r1 = 1;
            String sb = Integer.toBinaryString(i);
            int nsb = sb.length() - 1;
            for (int j = 0; j < sb.length(); j++) {
                if (sb.charAt(nsb - j) == '1') {
                    r1 *= primeFactors.get(j);
                }
            }
            if (r1 > result.get(count - 1)) {
                result.add(r1);
                count++;
            }
        }
        result.remove(result.size() - 1); // don't include the number itsself.
        return result;
    }

    public DomainDivider finerDivider(boolean superset) {
        int newSignificand;
        int newDigit = digit;
        List<Integer> factors = FACTORS[digit];
        int i = factors.indexOf(significand);
        do {
            i = i - 1;
            if (i == -1) {
                newDigit = digit + 1;
                factors = FACTORS[newDigit];
                newSignificand = factors.get(factors.size() - 1);
                if ( newDigit==ARR_DAY && superset ) {
                    newSignificand= 1;
                }
                break;
            } else {
                newSignificand = factors.get(i);
            }
        } while (superset && significand % newSignificand > 0);

        return new OrdinalTimeDomainDivider(newSignificand, newDigit);
    }

    public DomainDivider coarserDivider(boolean superset) {
        int newSignificand = 0;
        int newDigit = digit;
        List<Integer> factors = FACTORS[digit];
        int i = factors.indexOf(significand);
        do {
            i = i + 1;
            if (i == factors.size()) {
                newDigit = digit - 1;
                newSignificand = 1;
                if ( digit==ARR_DAY && superset ) {
                    throw new IllegalArgumentException("not supported.");
                } else {
                    newDigit = digit - 1;
                    newSignificand = 1;
                }
                break;
            } else {
                newSignificand = factors.get(i);
            }
        } while (superset && newSignificand % significand > 0);

        return new OrdinalTimeDomainDivider(newSignificand, newDigit);
    }

    /**
     * take the floor of the interval, mutating the input array.  The array is returned for convenience.
     * @param tarr
     * @param significand
     * @param digit
     * @return the input array which has been mutated.
     */
    private static int[] floor(int[] tarr, int significand, int digit) {
        tarr[digit] = (tarr[digit] - ZEROONE[digit]) / significand * significand + ZEROONE[digit];
        for (int i = digit + 1; i < N_DIGITS; i++) {
            tarr[i] = ZEROONE[i];
        }
        return tarr;
    }

    /**
     * take the ceil of the interval, mutating the input array.  The array is returned for convenience.
     * @param tarr
     * @param significand
     * @param digit
     * @return the input array which has been mutated.
     */
    private static int[] ceil(int[] tarr, int significand, int digit) {
        int d0 = tarr[digit] - ZEROONE[digit];
        int ceil = d0 % significand == 0 ? 0 : 1;
        for (int i = digit + 1; i < N_DIGITS; i++) {
            if (tarr[i] > ZEROONE[i]) {
                ceil = 1;
            }
            tarr[i] = ZEROONE[i];
        }
        tarr[digit] = (d0 / significand + ceil) * significand + ZEROONE[digit];
        carry(tarr);
        return tarr;
    }

    /**
     * carry digit significands where possible, e.g. 24 hours -> day++
     * @param tarr
     * @return
     */
    private static int[] carry(int[] tarr) {
        for (int i = ARR_SECOND; i > ARR_DAY; i--) {
            if (tarr[i] > MODULO[i]) {
                tarr[i - 1]++;
                tarr[i] -= MODULO[i];
            }
        }

        if (tarr[ARR_MONTH] > 12) {
            tarr[ARR_MONTH] -= 12;
            tarr[ARR_YEAR]++;
        }

        int dim= TimeUtil.daysInMonth(tarr[ARR_MONTH], tarr[ARR_YEAR]);
        if (tarr[ARR_DAY] > dim ) {
            tarr[ARR_MONTH]++;
            tarr[ARR_DAY]-= dim;
        }

        if (tarr[ARR_MONTH] > 12) {
            tarr[ARR_MONTH] -= 12;
            tarr[ARR_YEAR]++;
        }

        return tarr;
    }

    public DatumVector boundaries(Datum min, Datum max) {
        if (digit == ARR_DAY) {
            long nb = boundaryCount(min, max);
            if (nb > MAX_BOUNDARIES) {
                throw new IllegalArgumentException("LinearDomainDivider: too many divisions requested (" + boundaryCount(min, max) + ")");
            }
            double[] values = new double[(int) nb];
            Units units = min.getUnits();

            int[] tmin = ceil( TimeUtil.toTimeArray(min), 1, digit );
            for (int i = 0; i < nb; i++) {
                values[i] = TimeUtil.toDatum(tmin).doubleValue(units);
                tmin[digit]+= significand;
                carry(tmin);
            }
            return DatumVector.newDatumVector(values, units);
        } else {
            long nb = boundaryCount(min, max);
            if (nb > MAX_BOUNDARIES) {
                throw new IllegalArgumentException("LinearDomainDivider: too many divisions requested (" + boundaryCount(min, max) + ")");
            }
            double[] values = new double[(int) nb];
            Units units = min.getUnits();

            int[] tmin = ceil(TimeUtil.toTimeArray(min), significand, digit);
            for (int i = 0; i < nb; i++) {
                values[i] = TimeUtil.toDatum(tmin).doubleValue(units);
                tmin[digit] = tmin[digit] + significand;
                carry(tmin);
            }

            return DatumVector.newDatumVector(values, units);
        }
    }

    public long boundaryCount(Datum min, Datum max) {
        int[] tmin = ceil(TimeUtil.toTimeArray(min), significand, digit);
        int[] tmax = floor(TimeUtil.toTimeArray(max), significand, digit);

        // branch to avoid crossing over days, which have no modulo.
        if (digit < ARR_DAY) {
            long result = tmax[0] - tmin[0];
            for (int i = ARR_MONTH; i <= digit; i++) {
                result = result * MODULO[i] + (tmax[i] - tmin[i]);
            }
            return (result / significand) + 1;
        } else if (digit == ARR_DAY) {
            tmin = ceil(TimeUtil.toTimeArray(min), 1, digit);
            tmax = floor(TimeUtil.toTimeArray(max), 1, digit);
            int jmin = TimeUtil.julianDay(tmin[ARR_YEAR], tmin[ARR_MONTH], tmin[ARR_DAY]);
            int jmax = TimeUtil.julianDay(tmax[ARR_YEAR], tmax[ARR_MONTH], tmax[ARR_DAY]);
            long result = ((jmax - jmin) / significand) + 1;
            return result;
        } else {
            int jmin = TimeUtil.julianDay(tmin[ARR_YEAR], tmin[ARR_MONTH], tmin[ARR_DAY]);
            int jmax = TimeUtil.julianDay(tmax[ARR_YEAR], tmax[ARR_MONTH], tmax[ARR_DAY]);
            long result = jmax - jmin;
            for (int i = ARR_HOUR; i <= digit; i++) {
                result = result * MODULO[i] + (tmax[i] - tmin[i]);
            }
            return (result / significand) + 1;
        }
    }

    public DatumRange rangeContaining(Datum v) {
        int[] tarr = floor(TimeUtil.toTimeArray(v), significand, digit);
        Datum dstart = TimeUtil.toDatum(tarr);
        tarr[digit] = tarr[digit] + significand;
        Datum dstop = TimeUtil.toDatum(tarr);
        return new DatumRange(dstart, dstop);
    }

    public String toString() {
        return "OTDomainDivider by " + significand + " " + TimeUtil.TimeDigit.fromOrdinal(digit + 1);
    }

    public DatumFormatter getFormatter( DatumRange range ) {
        return TimeDatumFormatter.formatterForScale( digit+1, range);
    }

    public static void main(String[] args) throws Exception {

        System.err.println(primeFactors(1000));
        List<Integer> factors = factors(primeFactors(1000));
        for (int i = 0; i < factors.size(); i++) {
            System.err.print(" " + factors.get(i));
        }
        System.err.println("");

        DomainDivider div = new OrdinalTimeDomainDivider();
        DatumRange dr = DatumRangeUtil.parseTimeRange("2009");
        System.err.println(div.boundaryCount(dr.min(), dr.max()));
        System.err.println(div.boundaries(dr.min(), dr.max()));
        System.err.println(div.rangeContaining(dr.min()));
        System.err.println(div.coarserDivider(false).boundaryCount(dr.min(), dr.max()));
        System.err.println(div.finerDivider(false).boundaryCount(dr.min(), dr.max()));
        div = new OrdinalTimeDomainDivider(1000, ARR_YEAR);
        for (int i = 0; i < 100; i++) {
            System.err.println(div);
            div = div.finerDivider(false);
        }
    }
}
