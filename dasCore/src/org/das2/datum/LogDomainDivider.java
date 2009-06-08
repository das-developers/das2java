package org.das2.datum;

/**
 * A <code>DomainDivider</code> to divide a range into (base 10) logarithmically equal
 * segments. 
 * @author ed
 */
public class LogDomainDivider implements DomainDivider {

    private final DomainDivider expDivider;

    protected LogDomainDivider() {
        this(new LinearDomainDivider());
    }

    // for use by finerDivider and coarserDivider
    private LogDomainDivider(DomainDivider expDivider) {
        this.expDivider = expDivider;
    }

    public DomainDivider coarserDivider(boolean superset) {
        return new LogDomainDivider(expDivider.coarserDivider(superset));
    }

    public DomainDivider finerDivider(boolean superset) {
        // Should update to return a LogLinDomainDivider instead of fractional powers
        return new LogDomainDivider(expDivider.finerDivider(superset));
    }

    public DatumVector boundaries(Datum min, Datum max) {
        long nb = boundaryCount(min, max);
        if (nb > MAX_BOUNDARIES)
            throw new IllegalArgumentException("too many divisions requested (" + nb + ")");

        double logmin = Math.log10(min.doubleValue());
        double logmax = Math.log10(max.doubleValue());

        Datum dmin = Datum.create(logmin);
        Datum dmax = Datum.create(logmax);
        double[] exponents = expDivider.boundaries(dmin, dmax).toDoubleArray(Units.dimensionless);
        // This is for debugging:
        if (exponents.length != nb)
            throw new IllegalArgumentException("fatal error in LogDomainDivider");
        double[] result = new double[exponents.length];

        for (int i = 0; i < nb; ++i) {
            result[i] = Math.pow(10, exponents[i]);
        }

        return DatumVector.newDatumVector(result, min.getUnits());
    }

    public long boundaryCount(Datum min, Datum max) {
        if (min.doubleValue() > max.doubleValue())
            throw new IllegalArgumentException("LogDomainDivider: Illegal range specified (min>max)");
        // log scale cannot span zero
        if (min.doubleValue() < 0 && max.doubleValue() > 0)
            throw new IllegalArgumentException("LogDomainDivider: input range cannot contain zero");

        double logmin = Math.log10(min.doubleValue());
        double logmax = Math.log10(max.doubleValue());

        return expDivider.boundaryCount(Datum.create(logmin), Datum.create(logmax));
    }

    public DatumRange rangeContaining(Datum v) {
        double logv = Math.log10(v.doubleValue());

        DatumRange exponentRange = expDivider.rangeContaining(Datum.create(logv));
        double rangeMin = Math.pow(10, exponentRange.min().doubleValue());
        double rangeMax = Math.pow(10, exponentRange.max().doubleValue());

        return new DatumRange(rangeMin, rangeMax, v.getUnits());
    }

    public static void main(String[] args) {
        DomainDivider div = new LogDomainDivider();
        DatumRange dr = DatumRangeUtil.newDimensionless(0.2, 1000);
        System.err.println(div.boundaryCount(dr.min(), dr.max()));
        System.err.println(div.boundaries(dr.min(), dr.max()));
        System.err.println(div.rangeContaining(dr.min()));
        DomainDivider div2 = div.coarserDivider(false);
        System.err.println(div2.boundaryCount(dr.min(), dr.max()));
        System.err.println(div2.boundaries(dr.min(), dr.max()));
        div2 = div.finerDivider(false);
        System.err.println(div2.boundaryCount(dr.min(), dr.max()));
        System.err.println(div2.boundaries(dr.min(), dr.max()));
    }
}
