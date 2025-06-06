package org.das2.datum;

import java.util.ArrayList;

/**
 * A DomainDivider which divides logarithmically (base 10), with linear subdivisions.
 * It provides divisions such as <ul>
 * <li>1,2,3,4,5,6,7,8,9,10,20,30,40,50,...
 * <li>2,4,6,8,10,20,40,60,80,100,200,...
 * </ul>
 * @author ed
 */
public class LogLinDomainDivider implements DomainDivider {
    
    /**
     * linear divisions within the decade 1.0 to 10.0.
     */
    private LinearDomainDivider linearDivider;

    /**
     * create a way to access this class directly.
     * @return 
     */
    public static LogLinDomainDivider create() {
        return new LogLinDomainDivider();
    }
    
    /**
     * create one directly, useful for debugging.
     * @param significand 1, 2, or 5.
     * @param exponent 0 or -1 for many per cycle.
     * @return new LoglinDomainDivider
     */
    public static LogLinDomainDivider create( int significand, int exponent ) {
        LinearDomainDivider lin= new LinearDomainDivider(significand,exponent);
        return new LogLinDomainDivider(lin);
    }
    
    protected LogLinDomainDivider() {
        this(new LinearDomainDivider());
    }

    private LogLinDomainDivider(LinearDomainDivider linearDivider) {
        this.linearDivider = linearDivider;
        int significand= linearDivider.getSignificand();
        if ( significand!=1 && significand!=2 && significand!=5 ) {
            throw new IllegalArgumentException("significand must be 1, 2, or 5.");
        }
    }

    @Override
    public DomainDivider coarserDivider(boolean superset) {
        LinearDomainDivider d = (LinearDomainDivider) linearDivider.coarserDivider(superset);
        if  (d.boundaryCount(Datum.create(1.0), Datum.create(10.0 )) < 1) {
            // revert to straight log division
            return new LogDomainDivider();
        }
        else
            return new LogLinDomainDivider(d);
    }

    @Override
    public DomainDivider finerDivider(boolean superset) {
        // Make the linear subdivision finer.
        LinearDomainDivider lin = (LinearDomainDivider)linearDivider.finerDivider(superset);
        return new LogLinDomainDivider(lin);
    }

    @Override
    public DatumVector boundaries(Datum min, Datum max) {
        if ( !min.isFinite() || !max.isFinite() ) {
            System.err.println("min and max must be finite");
           // throw new IllegalArgumentException("min and max must be finite" );
        }
        
        double multiplier= Math.pow( 10, -1* Math.floor( Math.log10(min.value()) ) );
        if ( multiplier<1 ) multiplier=1;
        
        min= min.multiply(multiplier);
        max= max.multiply(multiplier);
        
        ArrayList<Datum> bb= new ArrayList<>();
        
        double decade = Math.pow(10, Math.floor(Math.log10(min.doubleValue())));
        double nextDecade= decade*10;
        DatumRange current= linearDivider.rangeContaining(min.divide(decade));
        current= DatumRange.newRange( current.min().multiply(decade), current.max().multiply(decade) );
        Datum m=current.min();
        Units u= m.getUnits();

        while ( m.le(max) ) {
            while ( m.value()<nextDecade*1.00001 ) { // kludge for real number fuzz...
                if ( m.ge(min) && m.le(max) ) {
                    bb.add( m.divide(multiplier) );
                }
                if ( m.gt(max) ) break;
                current=current.next();
                m= current.min();
            }
            Datum w= current.width().multiply(10);
            decade= decade*10;
            int nstep= (int) ( Math.floor( (decade*10) - decade ) / w.value() );
            
            Datum newMin= u.createDatum( (decade*10)  - nstep * w.value() );
            Datum newMax= newMin.add(w);
            current= new DatumRange(newMin,newMax);
            if ( newMin.value()==decade ) {
                current= current.next();
            }
            m= current.min();
            nextDecade= nextDecade*10;
        }
        
        DatumVector result= DatumVector.newDatumVector( bb.toArray(new Datum[bb.size()]), current.getUnits() );
        return result;
    }

    @Override
    public long boundaryCount(Datum min, Datum max) {
        return boundaries(min,max).getLength();
    }

    @Override
    public DatumRange rangeContaining(Datum v) {
        DomainDivider logDivider = new LogDomainDivider();
        DatumRange decade = logDivider.rangeContaining(v);
        double decadeOffset = decade.min().doubleValue();

        //Datum mmin = decade.min().divide(decadeOffset);
        //Datum mmax = decade.max().divide(decadeOffset);
        DatumRange range = linearDivider.rangeContaining(v.divide(decadeOffset));

        return new DatumRange(range.min().multiply(decadeOffset), range.max().multiply(decadeOffset));
    }

    /**
     * return the number of decimal places used.  For example,
     *  ..., 0.8e0, 0.9e0, 1.1e1, 1.2e1, 1.3e1, ... yields 2.
     * @return
     */
    protected int sigFigs() {
        return 0-linearDivider.getExponent();
    }

    @Override
    public String toString() {
        return "loglin linearDivider="+linearDivider;
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
