
package org.das2.datum.format;

import java.util.IllegalFormatException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.Datum;
import org.das2.datum.LoggerManager;
import org.das2.datum.TimeParser;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;

/**
 * This is based on the C-style format strings introduced in Java 5 that
 * we can now use.  When used with times, the format should be specified
 * using URI_Templates like $Y$m$dT$H:$M:$S.  
 * TODO: See Autoplot's DataSetUtil.toString, which shows use with Calendar objects.
 * 
 * Here is a table showing some examples:
 * <table summary="examples">
 * <tr><td>%9.2f</td><td>decimal with two fractional places</td></tr>
 * <tr><td>%9.2e</td><td>decimal in scientific notation</td></tr>
 * <tr><td>%.2f</td><td>decimal with two fractional places, and some number of total spaces</td></tr>
 * <tr><td>%5d</td><td>integer in five spaces.</td></tr>
 * <tr><td>$Y$m$dZ</td><td>time specification.</td></tr>
 * </table>
 * @author jbf
 */
public class FormatStringFormatter extends DefaultDatumFormatter {

    private String format;
    private boolean units;
    private boolean integer;

    private static final Logger logger= LoggerManager.getLogger("datum.format");
    
    private TimeParser timeFormat=null;
    
    /**
     * create a new instance based on the Java format string.
     * @param formatStr see http://download.oracle.com/javase/1.5.0/docs/api/java/util/Formatter.html#syntax
     * @param units if true, append the units after the formatted string
     */
    public FormatStringFormatter( String formatStr, boolean units ) {
        if ( TimeParser.isSpec(formatStr) ) {
            timeFormat= TimeParser.create(formatStr);
            this.units= false;
            format= formatStr;
        } else {
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
            return timeFormat.format(datum);
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
