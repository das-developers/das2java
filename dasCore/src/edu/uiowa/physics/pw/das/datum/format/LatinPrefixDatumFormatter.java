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

package edu.uiowa.physics.pw.das.datum.format;

import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.util.*;

import java.text.*;
import java.util.regex.*;

/** Formats Datums using K and M, etc labels and a specified precision.
 *
 * @author  Jeremy Faden
 */
public class LatinPrefixDatumFormatter extends DatumFormatter {
    
    private DecimalFormat format;
    
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
        int exponent= (int) DasMath.log10(1.000001*Math.abs(x)) / 3 * 3;
        
        String expString;
        switch (exponent) {
            case -9: expString="n";break;
            case -6: expString="u";break;
            case -3: expString="m";break;
            case 0: expString="";break;
            case 3: expString="K";break;
            case 6: expString="M";break;
            case 9: expString="G";break;
            default:  expString=""; exponent=0; break;
        }
        
        int sign= x < 0 ? -1 : 1;
        
        double exp= DasMath.exp10(exponent);
        double mant= x / exp;
        
        int mantFracDigits= digits - (int)DasMath.log10(mant);
        
        StringBuffer buff = new StringBuffer(digits+2).append("0");
        if ( digits>1 ) buff.append('.');
        for (int i = 0; i< mantFracDigits; i++) {
            buff.append('0');
        }
        String mantFormatString= buff.toString();
        NumberFormat mantFormat= new DecimalFormat(buff.toString());
        
        return mantFormat.format(mant) + expString;
    }
    
    public String toString() {
        return "EngineeringFormatter("+digits+" sig fig)";
    }
    
}
