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

/** Formats Datums using K and M, etc labels and a specified precision.
 *
 * @author  Jeremy Faden
 */
public class LatinPrefixDatumFormatter extends DatumFormatter {
    
    int digits;
    int exponent;
    
    /* print with digits in the mantissa */
    /* m.mmK */
    public LatinPrefixDatumFormatter( int digits ) {
        this.digits= digits;
    }
    
    public String format(Datum datum) {
        return format( datum, datum.getUnits() ) + " " + datum.getUnits();
    }

    public String format( Datum datum, Units units ) {
        double x= datum.doubleValue(units); 
        if ( x == 0. ) return "0.";
        int expon= (int) Math.log10(1.000001*Math.abs(x)) / 3 * 3;
        
        String expString;
        switch (expon) {
            case -18: expString="a"; break;
            case -15: expString="f"; break;
            case -12: expString="p"; break;
            case -9: expString="n";break;
            case -6: expString="\u03BC";break;  // micro
            case -3: expString="m";break;
            case 0: expString="";break;
            case 3: expString="k";break;
            case 6: expString="M";break;
            case 9: expString="G";break;
            case 12: expString="T"; break;
            default:  expString=""; expon=0; break;
        }
        
        double exp= Math.pow(10,expon);
        double mant= x / exp;
        
        int mantFracDigits= digits - (int)Math.log10(mant);
        
        StringBuffer buff = new StringBuffer(digits+2).append("0");
        if ( digits>1 ) buff.append('.');
        for (int i = 0; i< mantFracDigits; i++) {
            buff.append('0');
        }
        NumberFormat mantFormat= NumberFormatUtil.getDecimalFormat(buff.toString());
        
        return mantFormat.format(mant) + expString;
    }
    
    public String toString() {
        return "EngineeringFormatter("+digits+" sig fig)";
    }
    
}
