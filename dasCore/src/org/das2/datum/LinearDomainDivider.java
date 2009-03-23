package org.das2.datum;

/**
 * A <code>DomainDivider</code> to divide a linear data range into equal segments.
 * Within a given order of magnitude, this divider will work in increments of one,
 * two, or five.
 * @author Ed Jackson
 */
public class LinearDomainDivider implements DomainDivider {
    private final int incSignificand, incExponent;

    // protected access for factory method in DomainDividerUtil
    protected LinearDomainDivider() {
        this(1, 0);
    }

    // private version allows specification of increment, for use by
    // finerDivider and coarserDivider
    private LinearDomainDivider(int significand, int exponent) {
        incSignificand = significand;
        incExponent = exponent;
    }

    public DomainDivider coarserDivider(boolean superset) {
        int newSignificand, newExponent;

        if (incSignificand == 1) {
            newSignificand = 2;
            newExponent = incExponent;
        } else if (incSignificand == 2) {
            if (superset) {
                newSignificand = 1;
                newExponent = incExponent + 1;
            } else {
                newSignificand = 5;
                newExponent = incExponent;
            }
        } else if (incSignificand == 5) {
            newSignificand = 1;
            newExponent = incExponent + 1;
        } else {
            throw new IllegalStateException("Illegal state in LinearDomainDivider");
        }
        return new LinearDomainDivider(newSignificand, newExponent);
    }

    public DomainDivider finerDivider(boolean superset) {
        int newSignificand, newExponent;

        if (incSignificand == 1) {
            newSignificand = 5;
            newExponent = incExponent - 1;
        } else if (incSignificand == 2) {
            newSignificand = 1;
            newExponent = incExponent;
        } else if (incSignificand == 5) {
            if (superset) {
                newSignificand = 1;
                newExponent = incExponent;
            } else {
                newSignificand = 2;
                newExponent = incExponent;
            }            
        } else {
            throw new IllegalStateException("Illegal state in LinearDomainDivider");
        }
        return new LinearDomainDivider(newSignificand, newExponent);
    }

    public DatumVector boundaries(Datum min, Datum max) {
        long nb = boundaryCount(min,max);
        //this limit is kind of arbitrary, but we need something
        //should it be a different exception? sublclass?
        if (nb > 1024)
            throw new IndexOutOfBoundsException("LinearDomainDivider: too many divisions requested");
        double[] values = new double[(int)nb];
        double intervalSize = Math.pow(incSignificand, incExponent);

        double v = Math.floor(min.doubleValue()/intervalSize);
        for (int i=0 ; i < nb ; ++i) {
            values[i] = v;
            v += intervalSize;
        }
        return DatumVector.newDatumVector(values, min.getUnits());
    }

    public DatumRange rangeContaining(Datum v) {
        double intervalSize = Math.pow(incSignificand, incExponent);
        double min = Math.floor(v.doubleValue()/intervalSize);
        return new DatumRange(min, min + intervalSize, v.getUnits());
    }

    public long boundaryCount(Datum min, Datum max) {
        double intervalSize = Math.pow(incSignificand, incExponent);
        long mmin = (long)Math.floor(min.doubleValue()/intervalSize);
        long mmax = (long)Math.ceil(max.doubleValue()/intervalSize);

        return mmax - mmin + 1;
    }

}
