
package org.das2.datum;

/**
 * converts decimal years to us2000.
 * @author jbf
 */
public final class DecimalYearConverter extends UnitsConverter {

    /**
     * reverse means convert from us2000 to decimal years.
     */
    boolean reverse;
    
    double us2000Min= Double.MAX_VALUE, us2000Max=Double.MAX_VALUE;
    int year;
    
    public DecimalYearConverter( boolean us2000ToDecimalYear ) {
        this.reverse= us2000ToDecimalYear;
        if ( reverse ) {
            inverse= new LeapSecondsConverter( !reverse );
            inverse.inverse= this;
        }
    }
    
    @Override
    public UnitsConverter getInverse() {
        return inverse;
    }

    private void reset( double valueUs2000 ) {
        Datum min= TimeUtil.floor( TimeUtil.YEAR, Units.us2000.createDatum(valueUs2000) );
        Datum max= TimeUtil.next( TimeUtil.YEAR, min );
        year= TimeUtil.toTimeStruct(min).year;
        us2000Min= min.doubleValue( Units.us2000 );
        us2000Max= max.doubleValue( Units.us2000 );        
    }
    
    @Override
    public double convert(double value) {
        if ( reverse ) {
            if ( value<us2000Min || value>=us2000Max ) {
                reset(value);
            }
            double fp= (value-us2000Min)/(us2000Max-us2000Min);
            return year+fp;
        } else {
            if ( ((int)value)!=year ) {
                reset( TimeUtil.createTimeDatum( (int)(value), 1, 1, 0, 0, 0, 0 ).doubleValue(Units.us2000 ) );
            }
            double fp= value-(int)value;
            double extraMicros= fp * (us2000Max-us2000Min);
            return us2000Min + extraMicros;
        }
    }
    
    
}
