/* File: UnitsConverter.java
 * Copyright (C) 2002-2003 The University of Iowa
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

import edu.uiowa.physics.pw.das.datum.Units;

/**
 *
 * @author  jbf
 */
public class UnitsConverter {
    
    public double offset;
    
    public double scale;
    
    private int numServed;
    
    private UnitsConverter inverse;
    
    private String id;
    
    public static UnitsConverter identity= new UnitsConverter( 1.0, 0.0 );
    
    /** Creates a new instance of MeaureUnitConverter */
    public UnitsConverter(double scale, double offset) {
        this( scale, offset, "" );
    }
    
    public UnitsConverter(double scale, double offset, String id) {
        this.scale= scale;
        this.offset= offset;
        this.id= id;
        numServed= 0;
        inverse= null;
    }
    
    public UnitsConverter getInversion() {
        if ( inverse==null ) {
            inverse= new UnitsConverter( 1/scale, (-1*offset)/scale );
        }
        return inverse;
    }
    
    public double convert( double value ) {
        return scale * value + offset;
    }
    
    public Number convert( Number value ) {
        double doubleValue= convert(value.doubleValue());
        if ( value instanceof Integer ) {
            return new Integer((int)doubleValue);
        } else if ( value instanceof Float ) {
            return new Float(doubleValue);
        } else {
            return new Double(doubleValue);
        }
    }
    
    public String toString() {
        if ( id.equals("") ) {
            return "" + scale + " * old + " + offset;
        } else {
            return id;
        }
    }
    
    public static UnitsConverter getConverter(Units fromUnits, Units toUnits) {
        return Units.getConverter(fromUnits,toUnits);
    }

    public static void main( String[] args ) {
        UnitsConverter x= new UnitsConverter(9./5,32);
        UnitsConverter invx= x.getInversion();
        edu.uiowa.physics.pw.das.util.DasDie.println(""+9.7);
        edu.uiowa.physics.pw.das.util.DasDie.println(""+x.convert(9.7));
        edu.uiowa.physics.pw.das.util.DasDie.println(""+x.convert(invx.convert(9.7)));
        edu.uiowa.physics.pw.das.util.DasDie.println(""+invx.convert(9.7));
        edu.uiowa.physics.pw.das.util.DasDie.println(""+invx.convert(x.convert(9.7)));
    }
}
