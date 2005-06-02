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
import edu.uiowa.physics.pw.das.util.*;
import java.math.*;

import java.util.*;
import java.text.ParseException;
/**
 *
 * @author  jbf
 */
public class NumberUnits extends Units {
    
    public NumberUnits(String id) {
        this(id,"");
    }
    
    public NumberUnits(String id, String description) {
        super(id,description);
    }
    
    public Datum createDatum( double value ) {
        return new Datum.Double( value, this, 0. );
    }
    
    public Datum createDatum( double value, double resolution ) {
        return new Datum.Double( value, this, resolution );
    }
    
    public Datum createDatum( int value ) {
        return new Datum.Double( value, this );
    }
    
    public Datum createDatum( long value ) {
        return new Datum.Double( value, this );
    }
    
    public Datum createDatum( Number value ) {
        return new Datum.Double( value, this );
    }
    
    
    public DatumFormatterFactory getDatumFormatterFactory() {
        return DefaultDatumFormatterFactory.getInstance();
    }
    
    /*
     * parse the string in the context of this.  If units are not 
     * specified, then assume units are this.  Otherwise, parse the
     * unit and attempt to convert to this before creating the unit.
     */
    public Datum parse(String s) throws ParseException {
        try {
            String[] ss= s.trim().split("\\s");
            if ( ss.length==1 ) {
                BigDecimal dd= new BigDecimal(s);
                double resolution= DasMath.exp10( -1*dd.scale() );
                return Datum.create(Double.parseDouble(s), this, resolution );
            } else {
                String unitsString= ss[1]; 
                for ( int i=2; i<ss.length; i++ ) unitsString+= " "+ss[i];
                Units u;
                try { 
                    u= Units.getByName(unitsString);
                } catch ( IllegalArgumentException e ) {
                    ParseException t= new ParseException(s, ss[0].length()+1 );                    
                    t.initCause(e);
                    throw t;
                }
                return Datum.create( u.convertDoubleTo(this,Double.parseDouble(ss[0])), this );                
            }
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
            UnitsConverter uc= bUnits.getConverter(this);
            if ( uc==null ) throw new IllegalArgumentException("Only division by dimensionless or convertable Datums is supported");
            return Units.dimensionless.createDatum( divide( a, uc.convert(b) ) );
        }
    }   
    
}
