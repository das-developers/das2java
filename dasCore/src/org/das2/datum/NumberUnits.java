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

package org.das2.datum;

import org.das2.util.DasMath;
import java.math.*;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.das2.datum.format.DefaultDatumFormatterFactory;
import org.das2.datum.format.DatumFormatterFactory;

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
     * @returns double[2], [0] is number, [1] is the resolution
     */
    private double[] parseDecimal( String s ) {
        s= s.trim();
        BigDecimal bd= new BigDecimal(s);
        
        if ( bd.scale()>0 ) {
            double resolution= DasMath.exp10( -1*bd.scale() );
            return new double[] { Double.parseDouble(s), resolution };
        } else {
            int ie= s.indexOf( 'E' );
            if ( ie==-1 ) ie= s.indexOf('e');
            String mant;
            if ( ie==-1 ) {
                int id= s.indexOf('.');
                double[] dd= new double[2];
                dd[0]= Double.parseDouble(s);
                if ( id==-1 ) {
                    dd[1]= 1.;
                } else {
                    int scale= s.length()-id-1;
                    dd[1]= DasMath.exp10(-1*scale);
                }
                return dd;
            } else {
                mant= s.substring(0,ie);
                double[] dd= parseDecimal( mant );
                double exp= DasMath.exp10( Double.parseDouble( s.substring(ie+1) ) );
                dd[0]= dd[0] * exp;
                dd[1]= dd[1] * exp;
                return dd;
            }
        }
    }
    
    // note + and - are left out because of ambiguity with sign.
    private static Pattern expressionPattern= Pattern.compile( "(.+)([\\*/])(.+)" );
    
    private Datum parseExpression( String s ) throws ParseException {
        Matcher m= expressionPattern.matcher(s);
        if ( !m.matches() ) throw new IllegalArgumentException("not an expression");
        String operator= m.group(2);
        Datum operand1;
        try {
            operand1= Units.dimensionless.parse( m.group(1) );
        } catch ( IllegalArgumentException e ) {
            operand1= this.parse( m.group(1) );
        }
        Datum operand2;
        try {
            operand2= Units.dimensionless.parse( m.group(3) );
        } catch ( IllegalArgumentException e ) {
            operand2= this.parse( m.group(3) );
        }
        Datum result;
        if ( operator.equals("*") ) {
            result= operand1.multiply(operand2);
        } else if ( operator.equals("/") ) {
            result= operand1.divide(operand2);
        } else {
            throw new IllegalArgumentException("Bad operator: "+operator+" of expression "+s);
        }
        return result;
    }
    
    /*
     * parse the string in the context of this.  If units are not
     * specified, then assume units are this.  Otherwise, parse the
     * unit and attempt to convert to this before creating the unit.
     */
    public Datum parse(String s) throws ParseException {
        expressionPattern= Pattern.compile( "(.+)([\\*/])(.+)" );
        if ( expressionPattern.matcher(s).matches() ) {
            Datum result= parseExpression( s );
            if ( result.getUnits()==Units.dimensionless ) {
                result= this.createDatum( result.doubleValue() );
            } else {
                // throw exception if it's not convertable
                result= result.convertTo(this);
            }
            return result;
        } else {
            try {
                s= s.trim();
                if ( s.endsWith(this.getId()) ) {
                    s= s.substring(0,s.length()-this.getId().length());
                }
                String[] ss= s.split("\\s+");
                double[] dd= parseDecimal(ss[0]);
                if ( ss.length==1 ) {
                    return Datum.create( dd[0], this, dd[1] );
                } else {
                    String unitsString= ss[1];
                    for ( int i=2; i<ss.length; i++ ) unitsString+= " "+ss[i];
                    Units u;
                    try {
                        u= Units.lookupUnits(unitsString);
                    } catch ( IllegalArgumentException e ) {
                        ParseException t= new ParseException(s, ss[0].length()+1 );
                        t.initCause(e);
                        throw t;
                    }
                    UnitsConverter uc= u.getConverter(this);
                    return Datum.create( uc.convert(dd[0]), this, uc.convert(dd[1]) );
                }
            } catch (NumberFormatException nfe) {
                if ( s.equals("fill") ) {
                    return getFillDatum();
                } else {
                    ParseException pe = new ParseException(nfe.getMessage(), 0);
                    pe.initCause(nfe);
                    throw pe;
                }
            }
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
            if ( this==Units.dimensionless ) {
                return bUnits.createDatum( multiply( a, b ) );
            } else {
                Units inv= UnitsUtil.getInverseUnit(bUnits);
                UnitsConverter uc= this.getConverter(inv);
                if ( uc==null ) {
                    throw new IllegalArgumentException("Multiplication of two non-dimensionless numbers is not supported");
                } else {
                    return Units.dimensionless.createDatum( multiply( uc.convert(a), b ) );
                }
                
            }
        }
    }
    
    public Datum divide( Number a, Number b, Units bUnits ) {
        if ( bUnits==Units.dimensionless ) {
            return createDatum( divide( a, b ) );
        } else if ( this==Units.dimensionless ) {
            Units inv= UnitsUtil.getInverseUnit(bUnits);
            if ( inv!=null ) {
                return inv.createDatum( divide( a, b ) );
            } else {
                throw new IllegalArgumentException("No inversion found for "+bUnits );
            }
        } else {
            UnitsConverter uc= bUnits.getConverter(this);
            if ( uc==null ) throw new IllegalArgumentException("Only division by dimensionless or convertable Datums is supported");
            return Units.dimensionless.createDatum( divide( a, uc.convert(b) ) );
        }
    }
    
}