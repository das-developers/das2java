package org.das2.datum;

/**
 * A DomainDivider which divides logarithmically (base 10), with linear subdivisions.
 * It provides divisions such as 1,2,3,4,5,6,7,8,9,10,20,30,40,50.....
 * @author ed
 */
public class LogLinDomainDivider implements DomainDivider {
    private LinearDomainDivider decadeDivider;
    //private long divsPerDecade;

    protected LogLinDomainDivider() {
        this(new LinearDomainDivider());
    }

    private LogLinDomainDivider(LinearDomainDivider decadeDivider) {
        this.decadeDivider = decadeDivider;
    }

    public DomainDivider coarserDivider(boolean superset) {
        LinearDomainDivider d = (LinearDomainDivider) decadeDivider.coarserDivider(superset);
        if  (d.boundaryCount(Datum.create(0.0), Datum.create(1.0)) < 1) {
            return new LogDomainDivider();
        }
        else
            return new LogLinDomainDivider(d);
    }

    public DomainDivider finerDivider(boolean superset) {
        // Make the linear subidivision finer.
        return new LogLinDomainDivider((LinearDomainDivider)decadeDivider.finerDivider(superset));
    }

    public DatumVector boundaries(Datum min, Datum max) {
        long nb = boundaryCount(min,max);
        if (nb > MAX_BOUNDARIES )
            throw new IllegalArgumentException("too many divisions requested ("+boundaryCount(min, max)+")");

        DomainDivider logDivider = new LogDomainDivider();
        DatumVector logBoundaries = logDivider.boundaries(min, max);
        int numLogBoundaries = logBoundaries.getLength();
        double result[] = new double[(int)nb];
        int index = 0;

        /* There's some code repetition between this and boundaryCount below */
        if (numLogBoundaries > 1) {
            // Range spans more than 2 decades, so adjust linear division per decade
            // First get divisions from min to first log boundary
            double decadeOffset = Math.pow(10, Math.floor(Math.log10(min.doubleValue())));
            Datum mmin = min.divide(decadeOffset);
            Datum mmax = logBoundaries.get(0).divide(decadeOffset);
            double[] bounds = decadeDivider.boundaries(mmin, mmax).toDoubleArray(mmin.getUnits());
            // We don't store the last value because it will get included as we step through decades next.
            for (int i = 0; i < bounds.length-1; i++)
                result[index++] = bounds[i] * decadeOffset;

            // Now step through full decades.  We skip the last value in each decade
            // because it would get repeated as the first value of the next
            bounds = decadeDivider.boundaries(Datum.create(1), Datum.create(10)).toDoubleArray(Units.dimensionless);
            for (int dec = 0; dec < numLogBoundaries-1; dec++) {
                decadeOffset = logBoundaries.get(dec).doubleValue();
                for (int i = 0; i < bounds.length-1; i++)
                    result[index++] = bounds[i] * decadeOffset;
            }

            // Lastly get divisions from last log boundary to max
            decadeOffset = logBoundaries.get(numLogBoundaries-1).doubleValue();
            mmin = logBoundaries.get(numLogBoundaries-1).divide(decadeOffset);
            mmax = max.divide(decadeOffset);
            bounds = decadeDivider.boundaries(mmin, mmax).toDoubleArray(mmax.getUnits());
            for (int i = 0; i < bounds.length; i++)
                result[index++] = bounds[i] * decadeOffset;

            return DatumVector.newDatumVector(result, min.getUnits());
        } else {
            // Range only spans 1 or 2 decades so just divide linearly
            double decadeOffset = Math.pow(10, Math.floor(Math.log10(min.doubleValue())));
            Datum mmin = min.divide(decadeOffset);
            Datum mmax = max.divide(decadeOffset);
            return decadeDivider.boundaries(mmin, mmax);
        }
    }

    public long boundaryCount(Datum min, Datum max) {
        DomainDivider logDivider = new LogDomainDivider();
        DatumVector logBoundaries = logDivider.boundaries(min, max);
        long bc=0;
        int numLogBoundaries = logBoundaries.getLength();

        /* If the entire range spans only 1 or 2 decades, we do straight linear
         * subdivision.  For larger ranges, adjust division per decade.
         */
        if (numLogBoundaries > 1) {
            // first count divisions from full decades
            long divsPerDecade = decadeDivider.boundaryCount(Datum.create(0), Datum.create(10)) - 1;
            bc = divsPerDecade * (numLogBoundaries -1);

            // count divisions between min and first boundary
            double decadeOffset = Math.pow(10, Math.floor(Math.log10(min.doubleValue())));
            Datum mmin = min.divide(decadeOffset);
            Datum mmax = logBoundaries.get(0).divide(decadeOffset);
            bc += decadeDivider.boundaryCount(mmin, mmax) - 1;

            // count divisions between last boundary and max
            decadeOffset = Math.pow(10, Math.floor(Math.log10(max.doubleValue())));
            mmin = logBoundaries.get(numLogBoundaries-1).divide(decadeOffset);
            mmax = max.divide(decadeOffset);
            bc += decadeDivider.boundaryCount(mmin, mmax) - 1;
        } else {
            double decadeOffset = Math.pow(10, Math.floor(Math.log10(min.doubleValue())));
            Datum mmin = min.divide(decadeOffset);
            Datum mmax = max.divide(decadeOffset);
            bc = decadeDivider.boundaryCount(mmin, mmax);
        }
        return bc;
    }

    public DatumRange rangeContaining(Datum v) {
        DomainDivider logDivider = new LogDomainDivider();
        DatumRange decade = logDivider.rangeContaining(v);
        double decadeOffset = decade.min().doubleValue();

        Datum mmin = decade.min().divide(decadeOffset);
        Datum mmax = decade.max().divide(decadeOffset);
        DatumRange range = decadeDivider.rangeContaining(v.divide(decadeOffset));

        return new DatumRange(range.min().multiply(decadeOffset), range.max().multiply(decadeOffset));
    }

    /**
     * return the number of decimal places used.  For example,
     *  ..., 0.8e0, 0.9e0, 1.1e1, 1.2e1, 1.3e1, ... yeilds 2.
     * @return
     */
    protected int sigFigs() {
        return 0-decadeDivider.getExponent();
    }

    public static void main(String[] args) {
        DomainDivider d = new LogLinDomainDivider();
        DatumRange dr = DatumRangeUtil.newDimensionless(7.9, 218);
        System.err.println(d.boundaryCount(dr.min(), dr.max()));
        DatumVector dv = d.boundaries(dr.min(), dr.max());
        for (int i = 0; i < dv.getLength(); i++)
            System.err.print(dv.get(i).doubleValue() + ", ");
        System.err.println();
        System.err.println(d.rangeContaining(Datum.create(8)));

        System.err.println(d.coarserDivider(true).coarserDivider(true).boundaries(dr.min(), dr.max()));
    }
}
