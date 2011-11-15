/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.datum.format;

import java.util.IllegalFormatException;
import org.das2.datum.Datum;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;

/**
 * This is based on the C-style format strings introduced in Java 5 that
 * we can now use.  Note this should not be used for times.  In the future this
 * may be supported.  
 * @author jbf
 */
public class FormatStringFormatter extends DefaultDatumFormatter {

    private String format;
    private boolean units;
    private boolean integer;

    /**
     * create a new instance based on the Java format string.
     * @param formatStr see http://download.oracle.com/javase/1.5.0/docs/api/java/util/Formatter.html#syntax
     * @param units if true, append the units after the formatted string
     */
    public FormatStringFormatter( String formatStr, boolean units ) {
        if ( !formatStr.contains("%") ) {
            throw new IllegalArgumentException("formatStr doesn't contain percent (%)");
        }
        this.format= formatStr;
        this.units= units;

        // attempt to use the string
        try {
            String.format( format, 0. );
            integer= false;
        }  catch ( IllegalFormatException ex ) {
            integer= true;
        }
    }

    @Override
    public String format(Datum datum) {
        String s= format( datum, datum.getUnits() );
        if ( units ) {
            s+= " " + datum.getUnits().toString();
        }
        return s;
    }


    @Override
    public String format(Datum datum, Units units) {
        if ( UnitsUtil.isTimeLocation( datum.getUnits() ) ) {
            throw new IllegalArgumentException("times not formatted");
        } else {
            if ( integer ) {
                return String.format( format, (int)datum.doubleValue(units) );
            } else {
                return String.format( format, datum.doubleValue(units) );
            }
        }
    }

    @Override
    public String toString() {
        return String.format( "FormatStringFormatter(%s)", format );
    }
}
