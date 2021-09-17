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
    protected LinearDomainDivider(int significand, int exponent) {
        incSignificand = significand;
        incExponent = exponent;
    }

    @Override
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

    @Override
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

    @Override
    public DatumVector boundaries(Datum min, Datum max) {
        if ( !min.isFinite() || !max.isFinite() ) {
            System.err.println("min and max must be finite");
           // throw new IllegalArgumentException("min and max must be finite" );
        }
        
        long nb = boundaryCount(min,max);
        if (nb > MAX_BOUNDARIES )
            throw new IllegalArgumentException("too many divisions requested ("+boundaryCount(min, max)+")");
        double[] values = new double[(int)nb];

        boolean inv= incExponent<0;
        double intervalSize;
        if (inv) {
            intervalSize=  Math.pow(10, -incExponent) / incSignificand;
            double v = Math.ceil(min.doubleValue() * intervalSize) / intervalSize;
            for (int i=0 ; i < nb ; ++i)
                values[i] = v + i / intervalSize;
        } else {
            intervalSize= incSignificand * Math.pow(10, incExponent);
            double v = Math.ceil(min.doubleValue()/intervalSize) * intervalSize;
            for (int i=0 ; i < nb ; ++i)
                values[i] = v + i  * intervalSize;
        }

        return DatumVector.newDatumVector(values, min.getUnits());
    }

    @Override
    public DatumRange rangeContaining(Datum v) {
        double intervalSize = incSignificand * Math.pow(10, incExponent);
        double min = Math.floor(v.doubleValue()/intervalSize) * intervalSize;
        return new DatumRange(min, min + intervalSize, v.getUnits());
    }

    @Override
    public long boundaryCount(Datum min, Datum max) {
        if ( min.gt(max) ) {
            return 0;
        }
        double intervalSize = incSignificand * Math.pow(10, incExponent);
        long mmin = (long)Math.ceil(min.doubleValue()/intervalSize);
        long mmax = (long)Math.floor(max.doubleValue()/intervalSize);

        return mmax - mmin + 1;
    }

    /**
     * protected to provide access to other dividers that delegate to this,
     * and to support formatter.
     * @return
     */
    protected int getSignificand() {
        return incSignificand;
    }

    /**
     * protected to provide access to other dividers that delegate to this,
     * and to support formatter.
     * @return
     */
    protected int getExponent() {
        return incExponent;
    }

    @Override
    public String toString() {
        return "ldd "+incSignificand+"E"+incExponent;
    }

    public static void main(String[] args) {
        DomainDivider div = new LinearDomainDivider();
        DatumRange dr = DatumRangeUtil.newDimensionless(0.1,0.9);
        System.err.println(div.boundaryCount(dr.min(), dr.max())); // logger okay
        System.err.println(div.boundaries(dr.min(), dr.max())); // logger okay
        System.err.println(div.rangeContaining(dr.min())); // logger okay
        DomainDivider div2 = div.coarserDivider(false);
        System.err.println(div2.boundaryCount(dr.min(), dr.max())); // logger okay
        System.err.println(div2.boundaries(dr.min(), dr.max())); // logger okay
        div2 = div.finerDivider(false);
        System.err.println(div2.boundaryCount(dr.min(), dr.max())); // logger okay
        System.err.println(div2.boundaries(dr.min(), dr.max())); // logger okay
    }

}
