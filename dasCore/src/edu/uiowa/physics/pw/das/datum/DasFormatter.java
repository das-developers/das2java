/* File: DasFormatter.java
 * Copyright (C) 2002-2003 University of Iowa
 * Created by: Jeremy Faden <jbf@space.physics.uiowa.edu>
 *             Jessica Swanner <jessica@space.physics.uiowa.edu>
 *             Edward E. West <eew@space.physics.uiowa.edu>
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

package edu.uiowa.physics.pw.das.datum;

import java.text.DecimalFormat;
import java.text.ParsePosition;

/**
 *
 * @author  jbf
 */
public class DasFormatter implements Cloneable {
        
    DecimalFormat nf;
    
    /** Creates a new instance of DasFormatter */
    public DasFormatter() {
        nf= new DecimalFormat();
    }
    
    public DasFormatter(DecimalFormat nf) {
        this.nf= nf;
    }
    
    public void setMaximumFractionDigits(int digits) {
        nf.setMaximumFractionDigits(digits);
    }
    
    public int getMaximumFractionDigits() {
        return nf.getMaximumFractionDigits();
    }
    
    public int getMinimumFractionDigits() {
        return nf.getMinimumFractionDigits();
    }
    
    public void setMinimumFractionDigits(int digits) {
        nf.setMinimumFractionDigits(digits);
    }
    
    public Object clone() {
        try {
            DasFormatter result= (DasFormatter)super.clone();
            result.nf= (DecimalFormat) this.nf.clone();
            return result;
        } catch ( java.lang.CloneNotSupportedException e ) {
            return null;
        }
    }
    
    public String format(Object o) {
        if ( !( o instanceof Datum ) ) {
            throw new IllegalArgumentException("Argument is not a Datum! ("+o.getClass().getName()+")" );
        }
        Datum d= (Datum)o;
        return format(d.getValue(),d.getUnits());
    }
    
    public String format( double d, Units units ) {
        return nf.format(d)+units;      
    }
    
    public String grannyFormat( double d, Units units ) {
        String format= format(d,units);
        if ( format.indexOf("E")!=-1 ) {
            int iE= format.indexOf("E");
            String mant= format.substring(0,iE);
            if (Double.parseDouble(mant)==1.0) {
                mant="10";
            } else {
                mant= mant+"*10";
            }
            String exp= format.substring(iE+1);
            format= mant+"!A"+exp;
        } 
        return format;
    }
    
//    public String format( double d ) {
//        return nf.format(d);
//    }
    
    public Datum parse(String s, Datum d) {
        return d.create(nf.parse(s,new ParsePosition(0)).doubleValue());
    }
        
}
