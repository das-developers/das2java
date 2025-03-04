/* File: DefaultDatumFormatter.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on October 1, 2003, 4:45 PM
 *      by Edward West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.das2.datum.format;

import java.text.NumberFormat;
import org.das2.datum.Datum;
import org.das2.datum.Units;

/** Formats Datums forcing a given exponent and number of decimal places.
 * This is useful for axes where each of the labels should have the same
 * exponent.  Zero is treated specially, just "0" is returned.  This helps
 * one to quickly identify zero on the axis.
 *
 * @author  Jeremy Faden
 */
public class ExponentialDatumFormatter extends DatumFormatter {
    
    int digits;
    int exponent;
    NumberFormat mantFormat;
    String mantFormatString;
    
    /* print with digits in the mantissa, use exponent for the exponent */
    /* mEe */
    public ExponentialDatumFormatter(int digits, int exponent) {
        this.digits= digits;
        this.exponent= exponent;
        StringBuffer buff = new StringBuffer(digits+2).append("0");
        if ( digits>1 ) buff.append('.');
        for (int i = 1; i< digits; i++) {
            buff.append('0');
        }
        mantFormatString= buff.toString();
        this.mantFormat= NumberFormatUtil.getDecimalFormat( buff.toString() );
    }
    
    public String format(Datum datum) {
        return format( datum, datum.getUnits() ) + " " + datum.getUnits();
    }
    
    @Override
    public String format( Datum datum, Units units ) {
        double x= datum.doubleValue(datum.getUnits());
        if ( x == 0. ) return "0.";
        double exp= Math.pow(10,exponent);
        double mant= x/exp;
        double tenToN= Math.pow(10,digits);
        mant= Math.round( mant * tenToN ) / tenToN;
        if ( mant==0. ) return "0."; // rounding errors
        return mantFormat.format(mant)+"E"+exponent;
    }
    
    @Override
    public String grannyFormat( Datum datum, Units units ) {
        String format= format(datum,units);
        if ( format.indexOf("E")!=-1 ) {
            int iE= format.indexOf("E");
            StringBuffer granny = new StringBuffer(format.length() + 4);
            String mant= format.substring(0,iE);
            granny.append(mant).append("\u00d7");
            granny.append("10").append("!A").append(format.substring(iE+1)).append("!N");
            format = granny.toString();
        }
        return format;
    }
    
    @Override
    public String grannyFormat( Datum datum ) {
        String format= format(datum,datum.getUnits());
        if ( format.indexOf("E")!=-1 ) {
            int iE= format.indexOf("E");
            StringBuffer granny = new StringBuffer(format.length() + 4);
            String mant= format.substring(0,iE);
            granny.append(mant).append("\u00d7");
            granny.append("10").append("!A").append(format.substring(iE+1)).append("!N");
            format = granny.toString();
        }
        return format + " " +datum.getUnits();
    }
    
    public String toString() {
        return mantFormatString + "E"+exponent;
    }
    
}
