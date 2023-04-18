package org.das2.datum;

/**
 * A DomainDivider which divides logarithmically (base 10), with linear subdivisions.
 * It provides divisions such as <ul>
 * <li>1,2,3,4,5,6,7,8,9,10,20,30,40,50,...
 * <li>2,4,6,8,10,20,40,60,80,100,200,...
 * </ul>
 * @author ed
 */
public class LogLinDomainDivider implements DomainDivider {
    private LinearDomainDivider decadeDivider;

    /**
     * create a way to access this class directly.
     * @return 
     */
    public static LogLinDomainDivider create() {
        return new LogLinDomainDivider();
    }
    
    protected LogLinDomainDivider() {
        this(new LinearDomainDivider());
    }

    private LogLinDomainDivider(LinearDomainDivider decadeDivider) {
        this.decadeDivider = decadeDivider;
    }

    @Override
    public DomainDivider coarserDivider(boolean superset) {
        LinearDomainDivider d = (LinearDomainDivider) decadeDivider.coarserDivider(superset);
        if  (d.boundaryCount(Datum.create(1.0), Datum.create(10.0 )) < 1) {
            // revert to straight log division
            return new LogDomainDivider();
        }
        else
            return new LogLinDomainDivider(d);
    }

    @Override
    public DomainDivider finerDivider(boolean superset) {
        // Make the linear subidivision finer.
        return new LogLinDomainDivider((LinearDomainDivider)decadeDivider.finerDivider(superset));
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

        DomainDivider logDivider = new LogDomainDivider();
        DatumVector logBoundaries = logDivider.boundaries(min, max);
        int numLogBoundaries = logBoundaries.getLength();
        double result[] = new double[(int)nb];
        int index = 0;

        /* There's some code repetition between this and boundaryCount below */
        if (numLogBoundaries > 0) {
            // divide min to first boundary
            double decadeOffset = Math.pow(10, Math.floor(Math.log10(min.doubleValue())));
            Datum mmin = min.divide(decadeOffset);
            //Datum mmax = logBoundaries.get(0).divide(decadeOffset);
            Datum mmax = logBoundaries.get(0).divide(decadeOffset);
            double[] bounds = decadeDivider.boundaries(mmin, mmax).toDoubleArray(mmin.getUnits());
            int n;
            if ( decadeDivider.getSignificand()==1 ) {
                // We don't store the last value because it will get included in the next span.
                n= bounds.length-1;
            } else {
                n= bounds.length;
            }
            for (int i = 0; i < n; i++) {
                result[index++] = bounds[i] * decadeOffset;
            }
        }
        if (numLogBoundaries > 1) {
            // divide complete decades.  Skip the last value in each decade
            // because it gets repeated as the first value of the next.
            double[] bounds = decadeDivider.boundaries(Datum.create(1), Datum.create(10)).toDoubleArray(Units.dimensionless);
            for (int dec = 0; dec < numLogBoundaries-1; dec++) {
                double decadeOffset = logBoundaries.get(dec).doubleValue();
                for (int i = 0; i < bounds.length-1; i++)
                    result[index++] = bounds[i] * decadeOffset;
            }

        }
        if (numLogBoundaries > 0) {
            // divide last boundary to max
            double decadeOffset = logBoundaries.get(numLogBoundaries-1).doubleValue();
            Datum mmin = logBoundaries.get(numLogBoundaries-1).divide(decadeOffset);
            Datum mmax = max.divide(decadeOffset);
            double[] bounds = decadeDivider.boundaries(mmin, mmax).toDoubleArray(mmax.getUnits());
            for (int i = 0; i < bounds.length; i++)
                result[index++] = bounds[i] * decadeOffset;
        } else {
            // There are no log boundaries; divide min to max
            double decadeOffset = Math.pow(10, Math.floor(Math.log10(min.doubleValue())));
            Datum mmin = min.divide(decadeOffset);
            Datum mmax = max.divide(decadeOffset);
            double[] bounds = decadeDivider.boundaries(mmin, mmax).toDoubleArray(mmin.getUnits());
            for (int i = 0; i < bounds.length; i++)
                result[index++] = bounds[i] * decadeOffset;
        }
 
        return DatumVector.newDatumVector(result, min.getUnits());
    }

    @Override
    public long boundaryCount(Datum min, Datum max) {
        DomainDivider logDivider = new LogDomainDivider();
        DatumVector logBoundaries = logDivider.boundaries(min, max);
        long bc=0;
        int numLogBoundaries = logBoundaries.getLength();

        if (numLogBoundaries > 1) { // count the whole-decade boundaries
            long divsPerDecade = decadeDivider.boundaryCount(Datum.create(0), Datum.create(10)) - 1;
            bc = divsPerDecade * (numLogBoundaries -1);
        }

        if (numLogBoundaries > 0) {
            // count divisions between min and first boundary
            double decadeOffset = Math.pow(10, Math.floor(Math.log10(min.doubleValue())));
            Datum mmin = min.divide(decadeOffset);
            Datum mmax = logBoundaries.get(0).divide(decadeOffset);
            long n;
            if ( decadeDivider.getSignificand()==1 ) {
                // We don't store the last value because it will get included in the next span.
                n= decadeDivider.boundaryCount(mmin, mmax)-1;//subtract 1 to avoid double count
            } else {
                n= decadeDivider.boundaryCount(mmin, mmax);
            }
            bc += n; //subtract 1 to avoid double count

            // count divisions between last boundary and max
            double maxd= Math.floor(Math.log10(max.doubleValue()));
            decadeOffset = Math.pow(10, maxd );
            mmin = logBoundaries.get(numLogBoundaries-1).divide(decadeOffset);
            mmax = max.divide(decadeOffset);
            bc += decadeDivider.boundaryCount(mmin, mmax); 
        } else {
            // There are no log boundaries, so just count from min to max
            double decadeOffset = Math.pow(10, Math.floor(Math.log10(min.doubleValue())));
            Datum mmin = min.divide(decadeOffset);
            Datum mmax = max.divide(decadeOffset);
            bc += decadeDivider.boundaryCount(mmin, mmax);
        }
        return bc;
    }

    @Override
    public DatumRange rangeContaining(Datum v) {
        DomainDivider logDivider = new LogDomainDivider();
        DatumRange decade = logDivider.rangeContaining(v);
        double decadeOffset = decade.min().doubleValue();

        //Datum mmin = decade.min().divide(decadeOffset);
        //Datum mmax = decade.max().divide(decadeOffset);
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

    @Override
    public String toString() {
        return "loglin decadeDivider="+decadeDivider;
    }

    public static void main(String[] args) {
        DomainDivider d = new LogLinDomainDivider();
        DatumRange dr = DatumRangeUtil.newDimensionless(7.9, 218);
        System.err.println(d.boundaryCount(dr.min(), dr.max())); // logger okay
        DatumVector dv = d.boundaries(dr.min(), dr.max());
        for (int i = 0; i < dv.getLength(); i++)
            System.err.print(dv.get(i).doubleValue() + ", "); // logger okay
        System.err.println(); // logger okay
        System.err.println(d.rangeContaining(Datum.create(27.3))); // logger okay

        System.err.println(d.coarserDivider(true).coarserDivider(true).boundaries(dr.min(), dr.max())); // logger okay
        System.err.println(d.finerDivider(true).finerDivider(true).boundaries(dr.min(), dr.max())); // logger okay

        d= d.finerDivider(true);
        d= d.finerDivider(true);
        
        for ( int i=0; i<10; i++ ) {
            d= d.coarserDivider(false);
            System.err.println(d); // logger okay
        }
        for ( int i=0; i<10; i++ ) {
            d= d.finerDivider(false);
            System.err.println(d); // logger okay
        }
    }
}
