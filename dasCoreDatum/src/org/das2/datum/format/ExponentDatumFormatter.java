
package org.das2.datum.format;

import org.das2.datum.Datum;
import org.das2.datum.Units;

/**
 * write with M!aE, always
 * @author jbf
 */
public class ExponentDatumFormatter extends DatumFormatter {

    String mantissaFormat;
    boolean intMantissa;
    
    public ExponentDatumFormatter( String mantissaFormat ) {
        this.mantissaFormat= mantissaFormat;
        intMantissa= mantissaFormat.equals("%d");
    }
    
    @Override
    public String format(Datum datum) {
        int magnitude= (int)Math.floor( Math.log10( datum.value() ) );
        double mant= datum.value() / ( Math.pow( 10, magnitude ) );
        if ( intMantissa ) {
            return (int)mant + "E" + magnitude;
        } else { 
            return String.format( mantissaFormat, mant ) + "E";
        }
    }

    @Override
    public String grannyFormat(Datum datum) {
        return grannyFormat( datum, datum.getUnits() );
    }

    @Override
    public String grannyFormat(Datum datum, Units units) {
        int magnitude= (int)Math.floor( Math.log10( datum.value() ) );
        double mant= datum.value() / ( Math.pow( 10, magnitude ) );
        if ( intMantissa ) {
            if ( mant==1 ) {
                return "10!A" + magnitude + "!n";
            } else {
                return mant + "*10!A" + magnitude + "!n";
            }
        } else { 
            return String.format( mantissaFormat, mant ) + "*10!A" + magnitude + "!n";
        }
    }
    
    
}
