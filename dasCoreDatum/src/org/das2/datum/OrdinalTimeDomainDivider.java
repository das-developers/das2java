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
    private static final int N_DIGITS = 8;
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
     * years and seconds divider, which should be used if non-null.
     */
    private LinearDomainDivider ysDivider;


    /**
     *
     * @param significand multiplier for digit.
     * @param digit ordinal ARR_YEAR, ARR_MONTH, etc.
     */
    private OrdinalTimeDomainDivider(int significand, int digit, LinearDomainDivider secondsDivider ) {
        this.significand = significand;
        this.digit = digit;
        this.ysDivider= secondsDivider;
    }

    protected OrdinalTimeDomainDivider() {
        this(1, TimeUtil.HOUR - 1, null );
    }

    /**
     * return a list of the prime factors for the number.
     * @param N
     * @return
     */
    private static List<Integer> primeFactors(int N) {
        List<Integer> result = new ArrayList<>();
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
        List<Integer> result = new ArrayList<>();
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

    @Override
    public DomainDivider finerDivider(boolean superset) {
        int newSignificand;
        int newDigit = digit;
        if ( ysDivider!=null ) {
            if ( digit==ARR_YEAR && ( ysDivider.getSignificand()>1 || ysDivider.getExponent()>0 ) ) {
                return new OrdinalTimeDomainDivider( significand, digit, (LinearDomainDivider)ysDivider.finerDivider(superset) );
            } else if ( digit==ARR_SECOND ) {
                if (  Math.abs( ysDivider.getExponent() ) > 1000 ) {
                    throw new IllegalArgumentException("something has gone wrong in OrdinalTimeDomainDivider");
                }
                return new OrdinalTimeDomainDivider( significand, digit, (LinearDomainDivider)ysDivider.finerDivider(superset) );
            }
        }

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

        if ( newDigit==ARR_SECOND && newSignificand==1. ) {
            return new OrdinalTimeDomainDivider(newSignificand, newDigit, new LinearDomainDivider() ); //TODO: this assumes 1e0
        } else {
            return new OrdinalTimeDomainDivider(newSignificand, newDigit, null );
        }
    }

    @Override
    public DomainDivider coarserDivider(boolean superset) {
        int newSignificand = 0;
        int newDigit = digit;
        List<Integer> factors = FACTORS[digit];
        int i = factors.indexOf(significand);
        if ( ysDivider!=null ) {
            if ( digit==ARR_SECOND && ysDivider.getExponent()<0 ) {
                return new OrdinalTimeDomainDivider( significand, digit, (LinearDomainDivider)ysDivider.coarserDivider(superset) );
            } else if ( digit==ARR_YEAR ) {
                return new OrdinalTimeDomainDivider( significand, digit, (LinearDomainDivider)ysDivider.coarserDivider(superset) );
            }
        }
        do {
            i = i + 1;
            if (i == factors.size()) {
                //newDigit = digit - 1;
                //newSignificand = 1;
                if ( digit==ARR_DAY && superset ) {
                    newDigit = digit - 1;
                    newSignificand = 1;
                    // This breaks the superset contract.  Where will this haunt us?
                } else {
                    newDigit = digit - 1;
                    newSignificand = 1;
                }
                break;
            } else {
                newSignificand = factors.get(i);
            }
        } while (superset && newSignificand % significand > 0);
        if ( newDigit==ARR_YEAR && newSignificand==1. ) {
            return new OrdinalTimeDomainDivider(newSignificand, newDigit, new LinearDomainDivider() ); //TODO: this assumes 1e0
        } else {
            return new OrdinalTimeDomainDivider(newSignificand, newDigit, null );
        }
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
        System.arraycopy(ZEROONE, digit + 1, tarr, digit + 1, N_DIGITS - (digit + 1));
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
        int ceil = d0 % significand == 0 ? 0 : 1; // ceil==0 means digit is at the boundary, ceil=1 means we'll round up.
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

    @Override
    public DatumVector boundaries(Datum min, Datum max) {
        if ( !min.isFinite() || !max.isFinite() ) {
            System.err.println("min and max must be finite");
           // throw new IllegalArgumentException("min and max must be finite" );
        }
        
        if ( digit==ARR_SECOND && ysDivider!=null ) {
            Datum t= TimeUtil.prevMidnight( min ).convertTo(Units.t2000);
            DatumVector secs= ysDivider.boundaries(min.subtract(t).convertTo(Units.seconds), max.subtract(t).convertTo(Units.seconds) );
            return secs.add(t);
        }
        if ( digit==ARR_YEAR && ysDivider!=null ) {
            int yearMin = TimeUtil.toTimeStruct(min).year;
            int yearMax = TimeUtil.toTimeStruct(max).year;
            Units u= Units.dimensionless;
            DatumVector years= ysDivider.boundaries( u.createDatum(yearMin), u.createDatum(yearMax) );
            Units out= min.getUnits();
            double[] tickV = years.toDoubleArray(Units.dimensionless);
            for (int i = 0; i < tickV.length; i++) {
                int iyear = (int) tickV[i];
                tickV[i] = TimeUtil.convert(iyear, 1, 1, 0, 0, 0, (TimeLocationUnits) out );
            }
            return DatumVector.newDatumVector(tickV,out);
        }
        if (digit == ARR_DAY) {
            long nb = boundaryCount(min, max);
            if (nb > MAX_BOUNDARIES) {
                throw new IllegalArgumentException("LinearDomainDivider: too many divisions requested (" + boundaryCount(min, max) + ")");
            }
            double[] values = new double[(int) nb];
            Units units = min.getUnits();

            int[] tmin = ceil( TimeUtil.toTimeArray(min), 1, digit );
            tmin = ceil( tmin, significand, digit );

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

    @Override
    public long boundaryCount(Datum min, Datum max) {
        if ( digit==ARR_SECOND && ysDivider!=null ) {
            Datum t= TimeUtil.prevMidnight( min ).convertTo(Units.t2000);
            return ysDivider.boundaryCount( min.subtract(t).convertTo(Units.seconds), max.subtract(t).convertTo(Units.seconds) );
        }
        if ( digit==ARR_YEAR && ysDivider!=null ) {
            int yearMin = TimeUtil.toTimeStruct(min).year;
            int yearMax = TimeUtil.toTimeStruct(max).year;
            Units u= Units.dimensionless;
            return ysDivider.boundaryCount( u.createDatum(yearMin), u.createDatum(yearMax) );
        }

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

    @Override
    public DatumRange rangeContaining(Datum v) {
        if ( digit==ARR_SECOND && ysDivider!=null ) {
            Datum t= TimeUtil.prevMidnight( v );
            DatumRange r= ysDivider.rangeContaining( v.subtract(t).convertTo(Units.seconds) );
            return new DatumRange( t.add(r.min()), t.add(r.max()) );
        }
        if ( digit==ARR_YEAR && ysDivider!=null ) {
            int yearMin = TimeUtil.toTimeStruct(v).year;
            DatumRange r= ysDivider.rangeContaining( Units.dimensionless.createDatum(yearMin) );
            double tmin, tmax;
            TimeLocationUnits out= (TimeLocationUnits) v.getUnits();
            tmin = TimeUtil.convert((int)r.min().doubleValue(), 1, 1, 0, 0, 0, out );
            tmax = TimeUtil.convert((int)r.max().doubleValue(), 1, 1, 0, 0, 0, out );
            return new DatumRange( tmin, tmax, out );
        }
        int[] tarr = floor(TimeUtil.toTimeArray(v), significand, digit);
        Datum dstart = TimeUtil.toDatum(tarr);
        tarr[digit] = tarr[digit] + significand;
        Datum dstop = TimeUtil.toDatum(tarr);
        return new DatumRange(dstart, dstop);
    }

    @Override
    public String toString() {
        if ( ysDivider!=null ) {
            return "OTDomainDivider delegate offset to "+ysDivider + " "+ TimeUtil.TimeDigit.fromOrdinal(digit + 1) ;
        } else {
            return "OTDomainDivider by " + significand + " " + TimeUtil.TimeDigit.fromOrdinal(digit + 1);
        }
    }

    public DatumFormatter getFormatter( DatumRange range ) {
        if ( ysDivider!=null ) {
            if ( digit==ARR_SECOND ) {
                if ( ysDivider.getExponent()<-6 ) {
                    return TimeDatumFormatter.NANOSECONDS;
                } else if ( ysDivider.getExponent()<-3 ) {
                    return TimeDatumFormatter.MICROSECONDS;
                } else if ( ysDivider.getExponent()<0 ) {
                    return TimeDatumFormatter.MILLISECONDS;
                } else {
                    return TimeDatumFormatter.SECONDS;
                }
            } else {
                return TimeDatumFormatter.YEARS;
            }
        } else {
            return TimeDatumFormatter.formatterForScale( digit+1, range);
        }
    }

    public static void main(String[] args) throws Exception {

        System.err.println(primeFactors(1000)); // logger okay
        List<Integer> factors = factors(primeFactors(1000));
        for (int i = 0; i < factors.size(); i++) {
            System.err.print(" " + factors.get(i)); // logger okay
        }
        System.err.println(""); // logger okay

        DomainDivider div = new OrdinalTimeDomainDivider();
        DatumRange dr = DatumRangeUtil.parseTimeRange("2009");
        System.err.println(div.boundaryCount(dr.min(), dr.max())); // logger okay
        System.err.println(div.boundaries(dr.min(), dr.max())); // logger okay
        System.err.println(div.rangeContaining(dr.min())); // logger okay
        System.err.println(div.coarserDivider(false).boundaryCount(dr.min(), dr.max())); // logger okay
        System.err.println(div.finerDivider(false).boundaryCount(dr.min(), dr.max())); // logger okay
        div = new OrdinalTimeDomainDivider(1000, ARR_YEAR,null);
        for (int i = 0; i < 100; i++) {
            System.err.println(div); // logger okay
            div = div.finerDivider(false);
        }
        for (int i = 0; i < 100; i++) {
            div = div.coarserDivider(false);
            System.err.println(div); // logger okay
        }
        for (int i = 0; i < 30; i++) {
            System.err.println(div); // logger okay
            div = div.finerDivider(true);
        }
        for (int i = 0; i < 30; i++) {
            div = div.coarserDivider(true);
            System.err.println(div); // logger okay
        }

    }
}
