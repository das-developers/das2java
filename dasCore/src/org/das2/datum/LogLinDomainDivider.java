package org.das2.datum;

/**
 * NOT READY FOR USE.
 * A DomainDivider which divides logarithmically (base 10), with linear subdivisions.
 * It provides divisions such as 1,2,3,4,5,6,7,8,9,10,20,30,40,50.....
 * @author ed
 */
public class LogLinDomainDivider implements DomainDivider {
    private DomainDivider decadeDivider;
    //private long divsPerDecade;

    protected LogLinDomainDivider() {
        this(new LinearDomainDivider());
    }

    private LogLinDomainDivider(DomainDivider decadeDivider) {
        this.decadeDivider = decadeDivider;
    }

    public DomainDivider coarserDivider(boolean superset) {
        DomainDivider d = decadeDivider.coarserDivider(superset);
        if  (d.boundaryCount(Datum.create(0.0), Datum.create(1.0)) < 1) {
            return new LogDomainDivider();
        }
        else
            return new LogLinDomainDivider(d);
    }

    public DomainDivider finerDivider(boolean superset) {
        // Make the linear subidivision finer.
        return new LogLinDomainDivider(decadeDivider.finerDivider(superset));
    }

    public DatumVector boundaries(Datum min, Datum max) {
        long nb = boundaryCount(min,max);
        if (nb > MAX_BOUNDARIES )
            throw new IllegalArgumentException("too many divisions requested ("+boundaryCount(min, max)+")");
        throw new UnsupportedOperationException("Not supported yet.");
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
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static void main(String[] args) {
        DomainDivider d = new LogLinDomainDivider();
        DatumRange dr = DatumRangeUtil.newDimensionless(7.9, 201);
        System.err.println(d.boundaryCount(dr.min(), dr.max()));
    }
}
