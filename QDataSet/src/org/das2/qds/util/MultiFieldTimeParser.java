/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qds.util;

import java.text.ParseException;
import org.das2.datum.Units;
import org.das2.datum.TimeParser;
import org.das2.qds.DataSetOps;
import org.das2.qds.MutablePropertyDataSet;
import org.das2.qds.QDataSet;

/**
 * Parse the record by recombining the separated fields, then parsing
 * the combined string.  This is to be used with AsciiParser.
 *
 * 2010/03/11: Indeterminate field length is used when one field is in a record.
 * 2010/03/11: The last field, if just one digit type (%S), can contain fractional part.
 *
 * @author jbf
 */
public class MultiFieldTimeParser implements AsciiParser.FieldParser {

    StringBuilder agg;
    int firstColumn;
    int lastColumn;
    TimeParser parser;
    Units units;
    String lastDigitFormat;
    boolean[] isNumber;

    private boolean multiFieldAdjacent( String spec ) {
        return spec.length()>3 && spec.charAt(2)=='%' && spec.charAt(3)!='%';
    }

    private int fieldCount( String spec ) {
        int count=0;
        for ( int i=0; i<spec.length(); i++ ) {
            if ( spec.charAt(i)=='%' && spec.charAt(i+1)!='%' ) count++;
        }
        return count;
    }

    private boolean isNumber( String spec ) {
        if ( spec.equals("%{ignore}") ) {
            return false;
        } else if ( spec.equals("%b") ) { //TODO: %-1{b}, etc.
            return false;
        } else {
            return fieldCount( spec )==1;
        }
    }
    
    /**
     * configure AsciiParser ap to use this field parser.  The parser is
     * registered with each column and the units are set.
     * @param ap
     * @param firstColumn
     * @param timeFormats 
     */
    public static MultiFieldTimeParser create( AsciiParser ap, int firstColumn, String[] timeFormats ) {
       String format= timeFormats[0];
       for ( int i=1; i<timeFormats.length; i++ ) {
           format= " "+timeFormats[i];
       }
       TimeParser tp= TimeParser.create( format );
       MultiFieldTimeParser mftp= new MultiFieldTimeParser( firstColumn, timeFormats, tp, Units.cdfTT2000 );
       for ( int i=0; i<timeFormats.length; i++ ) {
           ap.setFieldParser( i, mftp );
           ap.setUnits( i, Units.dimensionless );
       }
       return mftp;
    }

    public QDataSet unpack( QDataSet rank2, Units u ) {
        MutablePropertyDataSet ds= DataSetOps.slice1( rank2, lastColumn );
        if ( u!=units ) throw new IllegalArgumentException("unable to convert units, my fault.");
        ds.putProperty( QDataSet.UNITS, u );
        return ds;
    }
    
    public MultiFieldTimeParser( int firstColumn, String[] timeFormats, TimeParser parser, Units units ) {
        this.firstColumn= firstColumn;
        this.lastColumn= firstColumn + timeFormats.length - 1;
        StringBuilder timeFormat;

        isNumber= new boolean[timeFormats.length];
        
        isNumber[0]= isNumber( timeFormats[0] );
        if ( timeFormats[0].length()>1 && ( timeFormats[0].charAt(1)!='(' && timeFormats[0].charAt(1)!='{' )  ) { //Grrr.  TODO: use parens internally
            if ( multiFieldAdjacent(timeFormats[0]) ) {
                timeFormat= new StringBuilder(timeFormats[0]); // to have indeterminate length for first field, we need terminator.
            } else {
                timeFormat= new StringBuilder("%-1").append( timeFormats[0].substring(1) ); //kludge for whitespace
            }
        } else {
            timeFormat= new StringBuilder(timeFormats[0]);
        }

        for ( int i=1; i<timeFormats.length-1; i++ ) {
            isNumber[i]= isNumber( timeFormats[i] );
            if ( multiFieldAdjacent(timeFormats[i]) ) {
                timeFormat.append( " " ).append( timeFormats[i] ); // to have indeterminate length for first field, we need terminator.
            } else {
                timeFormat.append( " " ).append("%-1") .append( timeFormats[i].substring(1) ); //kludge for whitespace
            }
        }

        if ( timeFormats.length>1 && timeFormats[timeFormats.length-1].length()<3 ) {
            lastDigitFormat= timeFormats[timeFormats.length-1];
            isNumber[timeFormats.length-1]= true;
        } else {
            lastDigitFormat=null; // we can't use this feature
            String lastTimeFormat= timeFormats[timeFormats.length-1];
            String[] lastTimeFormats= lastTimeFormat.split("%");
            StringBuilder sb= new StringBuilder();
            for ( int i=1; i<lastTimeFormats.length; i++ ) {
                if ( lastTimeFormats[i].startsWith("{") && ( i==lastTimeFormats.length-1 || !lastTimeFormats[i].endsWith("}") ) ) { // if there is a delimiter
                    sb.append("%").append("-1").append(lastTimeFormats[i]);
                } else if ( lastTimeFormats[i].startsWith("{") ) {
                    sb.append("%").append(lastTimeFormats[i]); 
                } else {
                    if ( lastTimeFormats[i].length()>1 ) { // if there is a delimiter there, then we can have variable length fields.
                        sb.append("%").append("-1").append(lastTimeFormats[i]);
                    } else {
                        sb.append("%").append(lastTimeFormats[i]);
                    }
                }
            }
            timeFormat.append(" ").append( sb.toString() ); // to have indeterminate length for first field, we need terminator.
            isNumber[timeFormats.length-1]= false;
        }

        this.parser= TimeParser.create(timeFormat.toString());
        //this.parser= parser;
        this.units= units;
    }

    /**
     * Either the field is accumulated in a string, and the entire string is parsed for the last field.
     * @param field  the contents of the field.
     * @param columnIndex  the index of the column in the table.
     * @return 0 or the value for the time unit if it's the last field.
     * @throws ParseException
     */
    public double parseField(String field, int columnIndex) throws ParseException {
        double d= -1e31;
        if ( isNumber[columnIndex-firstColumn] ) {
            d= Double.parseDouble(field); // attempt to parse the number
            if ( d-(int)d == 0 ) { //TODO: this needs more thorough testing, to see what happens with time fields, etc.
                field= ""+ (int)d; // http://vho.nasa.gov/mission/helios2/H276_021.dat contains float years "1976.0000" that don't parse.
            }
        }
        if ( columnIndex==firstColumn ) {
            agg= new StringBuilder(field);
            return 0;
        } else if ( columnIndex<lastColumn ) {
            if ( agg==null ) throw new ParseException("another field was not parseable",0);
            agg= agg.append(" ").append( field ); 
            return 0;
        } else {
            if ( agg==null ) throw new ParseException("another field was not parseable",0);
            if ( lastDigitFormat==null ) {
                agg= agg.append(" ").append( field );
                return parser.parse(agg.toString()).getTime(units);
            } else {
                parser.parse(agg.toString());
                parser.setDigit( lastDigitFormat, Double.parseDouble(field) );
                return parser.getTime(units);
            }
        }
    }
    
    /**
     * suggest units for unpacking.
     * @return 
     */
    public Units getUnits() {
        return units;
    }

}
