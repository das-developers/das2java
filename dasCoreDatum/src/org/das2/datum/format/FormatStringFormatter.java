/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.datum.format;

import java.util.IllegalFormatException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.Datum;
import org.das2.datum.LoggerManager;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;

/**
 * This is based on the C-style format strings introduced in Java 5 that
 * we can now use.  Note this should not be used for times.  In the future this
 * may be supported.  TODO: See Autoplot's DataSetUtil.toString, which shows
 * use with Calendar objects.
 * @author jbf
 */
public class FormatStringFormatter extends DefaultDatumFormatter {

    private String format;
    private boolean units;
    private boolean integer;

    private static final Logger logger= LoggerManager.getLogger("datum.format");
    
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

        if ( formatStr.equals("%d") ) { // see if we can avoid the exception by checking for this case.
            String s= String.format( format, 0 );
            logger.log( Level.FINEST, "format string results in {0}", s);
            integer= true;
        } else {
            // attempt to use the string
            try {
                String s= String.format( format, 0. );
                logger.log( Level.FINEST, "format string results in {0}", s);
                integer= false;
            }  catch ( IllegalFormatException ex ) {
                integer= true;
            }
        }
    }

    @Override
    public String format(Datum datum) {
        String s= format( datum, datum.getUnits() );
        if ( units && datum.getUnits()!=Units.dimensionless ) {
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
