/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.datum.format;

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

    /**
     * create a new instance based on the Java format string.
     * @param formatStr see http://download.oracle.com/javase/1.5.0/docs/api/java/util/Formatter.html#syntax
     * @param units if true, append the units after the formatted string
     */
    public FormatStringFormatter( String formatStr, boolean units ) {
        this.format= formatStr;
        this.units= units;
        String s= String.format( format, 0 );  // try it out to catch errors early.
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
            return String.format( format, datum.doubleValue(units) );
        }
    }

}
