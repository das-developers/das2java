/* File: Units.java
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

import edu.uiowa.physics.pw.das.datum.format.*;

import java.util.*;
import java.text.ParseException;
/**
 *
 * @author  jbf
 */
public class NumberUnits extends Units {    

    private Datum fillDatum= null;
    
    public NumberUnits(String id) {        
        this(id,"");
    }
    
    public NumberUnits(String id, String description) {
        super(id,description);
        this.fillDatum= this.createDatum( -1e31 );
    } 
    
    public Datum createDatum( double value ) {
        return new Datum.Double( new Double(value), this );
    }
    
    public Datum createDatum( int value ) {
        return new Datum.Double( new Long(value), this );
    }
    
    public Datum createDatum( long value ) {
        return new Datum.Double( new Long(value), this );
    }
    
    public Datum createDatum( Number value ) {
        return new Datum.Double( value, this );
    }
    
    
    public DatumFormatterFactory getDatumFormatterFactory() {
        return DefaultDatumFormatterFactory.getInstance();
    }
    
    public Datum parse(String s) throws ParseException {
        try {
            return Datum.create(Double.parseDouble(s), this);
        }
        catch (NumberFormatException nfe) {
            ParseException pe = new ParseException(nfe.getMessage(), 0);
            pe.initCause(nfe);
            throw pe;
        }
    }
    
    
    protected static Number add( Number a, Number value ) {
        if ( ( a instanceof Integer ) && ( value instanceof Integer ) ) {
            return new Integer( a.intValue() + value.intValue() );
        } else {
            return new java.lang.Double( a.doubleValue()+value.doubleValue() );
        }
    }
    
    protected static Number subtract( Number from, Number value ) {
        if ( ( from instanceof Integer ) && ( value instanceof Integer ) ) {
            return new Integer( from.intValue() - value.intValue() );
        } else {
            return new java.lang.Double( from.doubleValue()-value.doubleValue() );
        }
    }
    
    protected static boolean isNumberFill( Number a ) {
        if ( a instanceof Integer ) {
            return a.intValue()==-999999999;
        } else {
            return a.doubleValue()==Double.NaN;
        }
    }
    
    protected static Number divide( Number a, Number value ) {
        if ( ( a instanceof Integer ) && ( value instanceof Integer ) ) {
            return new Integer( a.intValue() / value.intValue() );
        } else {
            return new java.lang.Double( a.doubleValue()/value.doubleValue() );
        }
    }
    
    protected static Number multiply( Number a, Number value ) {
        if ( ( a instanceof Integer ) && ( value instanceof Integer ) ) {
            return new Integer( a.intValue() * value.intValue() );
        } else {
            return new java.lang.Double( a.doubleValue()*value.doubleValue() );
        }
    }
            
    public Datum add( Number a, Number b, Units bUnits ) {
        if ( bUnits!=this ) {
            UnitsConverter uc= Units.getConverter( bUnits, this );
            b= uc.convert(b);
        }
        return createDatum( add( a, b ) );        
    }
    
    public Datum subtract( Number a, Number b, Units bUnits ) {
        if ( bUnits!=this ) {
            UnitsConverter uc= Units.getConverter( bUnits, this );
            b= uc.convert(b);
        }
        return createDatum( subtract( a, b ) );
    }
    
    public Datum multiply( Number a, Number b, Units bUnits ) {
        if ( bUnits==Units.dimensionless ) {
            return createDatum( multiply( a, b ) );
        } else {
            throw new IllegalArgumentException("Only multiplication by dimensionless numbers is supported");
        }
    }
    
    public Datum divide( Number a, Number b, Units bUnits ) {
        if ( bUnits==Units.dimensionless ) {
            return createDatum( divide( a, b ) );
        } else {
            throw new IllegalArgumentException("Only multiplication by dimensionless numbers is supported");
        }        
    }
    
    public Datum getFill() {
        return fillDatum;
    }
    
    public boolean isFill(Number value) {
        if ( value instanceof Double ) {
            return isFill(value.doubleValue());
        } else if ( value instanceof Float ) {
            return isFill(value.floatValue());
        } else if ( value instanceof Integer ) {
            return isFill(value.intValue());
        } else if ( value instanceof Long ) {
            return isFill(value.longValue());
        } else {
            throw new IllegalArgumentException("Unknown Number class: "+value.getClass().toString());
        }
    }
    
    public boolean isFill(double value) {
        return value==Double.NaN;
    }
    
    public boolean isFill(long value) {
        return value==-9999999999L;       
    }
    
    public boolean isFill(int value) {
        return value==-99999999;
    }
    
}
