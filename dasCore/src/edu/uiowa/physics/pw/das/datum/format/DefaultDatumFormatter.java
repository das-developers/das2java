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
import java.math.*;

import java.text.*;
import java.util.regex.*;

/** Formats Datum objects for printing and parses strings to Datum objects.
 *
 * @author  Edward West
 */
public class DefaultDatumFormatter extends DatumFormatter {
    
    private String formatString;
    
    private DecimalFormat format;
    
    /** Available for use by subclasses */
    protected DefaultDatumFormatter() {}
    
    /** Creates a new instance of DatumFormatter */
    public DefaultDatumFormatter(String formatString) throws ParseException {
        if (formatString.equals("")) {
            this.formatString = "";
            format = null;
        } else {
            this.formatString = formatString;
            format = new DecimalFormat(formatString);
        }
    }
    
    public String format(Datum datum) {
        return format( datum, datum.getUnits() ) + " " + datum.getUnits();
    }
    
    public String format(Datum datum, Units units ) {
        double d= datum.doubleValue(units);
        if ( Double.isInfinite(d) ) return ""+d;
        String result;
        if (format == null) {
            double resolution= datum.getResolution( units.getOffsetUnits() );
            if ( resolution>0 ) {
                // 28 -->   scale = -1
                // 2.8 -->  scale = 0 
                
                int scale= (int)Math.ceil( -1 * DasMath.log10(resolution) - 0.00001 );
                int exp;
                if ( d != 0. ) {
                    exp= (int)DasMath.log10( Math.abs(d) );
                } else {
                    exp= 0;
                }
                if ( scale>=0 ) {
                    DecimalFormat f;
                    if ( exp<=-5 || exp >=5 ) {
                        f= new DecimalFormat( "0E0" );
                        f.setMinimumFractionDigits(scale+exp-1);
                        f.setMaximumFractionDigits(scale+exp-1);
                    } else {
                        f = new DecimalFormat("0");
                        f.setMinimumFractionDigits(scale);
                        f.setMaximumFractionDigits(scale);
                    }
                    result= f.format(d);
                } else {
                    double round= DasMath.exp10( -1*scale );
                    d= Math.round( d / round ) * round;
                    DecimalFormat f;
                    if ( exp<=-5 || exp >=5 ) {
                        f= new DecimalFormat( "0E0" );
                        f.setMinimumFractionDigits(scale+exp+1);
                        f.setMaximumFractionDigits(scale+exp+1);
                    } else {
                        f = new DecimalFormat("0");
                    }
                    result= f.format(d);
                }
            } else {
                result=  Double.toString(d);
            }
        } else {
            result= format.format(datum.doubleValue(units));
        }
        return result;
    }
    
    public String grannyFormat( Datum datum, Units units ) {
        String format= format( datum, units );
        if ( format.indexOf("E")!=-1 ) {
            int iE= format.indexOf("E");
            StringBuffer granny = new StringBuffer(format.length() + 4);
            String mant= format.substring(0,iE);
            if (Double.parseDouble(mant)!=1.0) {
                granny.append(mant).append("\u00d7");
            }
            granny.append("10").append("!A").append(format.substring(iE+1)).append("!N");
            format = granny.toString();
        }
        return format;
    }
    
    public String grannyFormat(Datum datum) {
        return grannyFormat(datum,datum.getUnits()) + " " + datum.getUnits();
    }
    
    public String toString() {
        return formatString;
    }
}
