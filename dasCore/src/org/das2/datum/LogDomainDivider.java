package org.das2.datum;

/**
 * A <code>DomainDivider</code> to divide a range into logarithmically equal
 * segments.  By default, it uses base 10, but can use any integer base.  Natural
 * logs are not supported by this class.
 * @author ed
 */
public class LogDomainDivider implements DomainDivider {
    private final int logBase;
    private final DomainDivider expDivider;

    protected LogDomainDivider() {
        this(10);
    }

    protected LogDomainDivider(int logBase) {
        this.logBase = logBase;
        expDivider = new LinearDomainDivider();
    }

    // for use by finerDivider and coarserDivider
    private LogDomainDivider(int logBase, DomainDivider expDivider) {
        this.logBase = logBase;
        this.expDivider = expDivider;
    }
    
    public DomainDivider coarserDivider(boolean superset) {
        return new LogDomainDivider(logBase, expDivider.coarserDivider(superset));
    }

    public DomainDivider finerDivider(boolean superset) {
        return new LogDomainDivider(logBase, expDivider.finerDivider(superset));
    }

    public DatumVector boundaries(Datum min, Datum max) {
        long nb = boundaryCount(min, max);
        if (nb > MAX_BOUNDARIES )
            throw new IllegalArgumentException("LogDomainDivider: too many divisions requested ("+boundaryCount(min, max)+")");

        double logmin = Math.log10(min.doubleValue());
        double logmax = Math.log10(max.doubleValue());
        if (logBase != 10) {
            logmin /= Math.log10(logBase);
            logmax /= Math.log10(logBase);
        }

        Datum dmin = Datum.create(logmin);
        Datum dmax = Datum.create(logmax);
        double[] exponents = expDivider.boundaries(dmin, dmax).toDoubleArray(Units.dimensionless);
        // This is for debugging:
        if (exponents.length != nb)
            throw new IllegalArgumentException("fatal error in LogDomainDivider");
        double[] result = new double[exponents.length];

        for(int i = 0; i < nb; ++i)
            result[i] = Math.pow(logBase, exponents[i]);

        return DatumVector.newDatumVector(result, min.getUnits());
    }

    public long boundaryCount(Datum min, Datum max) {
        if (min.doubleValue() > max.doubleValue())
            throw new IllegalArgumentException("LogDomainDivider: Illegal range specified (min>max)");
        // log scale cannot span zero
        if (min.doubleValue() < 0 && max.doubleValue() > 0)
            throw new IllegalArgumentException("LogDomainDivider: input range cannot contain zero");

        double logmin = Math.log10(min.doubleValue());
        double logmax = Math.log10(min.doubleValue());

        if(logBase != 10) {  //convert to appropriate base
            // remember logX(n) = log10(n) / log10(X)
            logmin /= Math.log10(logBase);
            logmax /= Math.log10(logBase);
        }

        return expDivider.boundaryCount(Datum.create(logmin), Datum.create(logmax));
    }

    public DatumRange rangeContaining(Datum v) {
        double logv = Math.log10(v.doubleValue());
        
        DatumRange exponentRange = expDivider.rangeContaining(Datum.create(logv));
        double rangeMin = Math.pow(logBase, exponentRange.min().doubleValue());
        double rangeMax = Math.pow(logBase, exponentRange.max().doubleValue());

        return new DatumRange(rangeMin, rangeMax, v.getUnits());
    }

}
