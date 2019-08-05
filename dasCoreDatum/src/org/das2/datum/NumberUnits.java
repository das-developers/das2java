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

import java.math.BigDecimal;
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
    
    @Override
    public Datum createDatum( double value ) {
        return new Datum.Double( value, this, 0. );
    }
    
    @Override
    public Datum createDatum( double value, double resolution ) {
        return new Datum.Double( value, this, resolution );
    }
    
    @Override
    public Datum createDatum( int value ) {
        return new Datum.Double( value, this );
    }
    
    @Override
    public Datum createDatum( long value ) {
        return new Datum.Long( value, this );
    }
    
    @Override
    public Datum createDatum( Number value ) {
        return new Datum.Double( value, this );
    }
    
    @Override
    public DatumFormatterFactory getDatumFormatterFactory() {
        return DefaultDatumFormatterFactory.getInstance();
    }
    
    /*
     * parse the decimal providing an estimate of resolution as well.
     * @returns double[2], [0] is number, [1] is the resolution
     */
    private static double[] parseDecimal( String s ) {
        s= s.trim();
        BigDecimal bd;
        if ( s.startsWith("x") ) {
            return new double[] { Integer.parseInt(s.substring(1),16), 0 };
        } else if ( s.startsWith("0x") ) {
            return new double[] { Integer.parseInt(s.substring(2),16), 0 };
        } else if ( s.equalsIgnoreCase("nan") ) {
            return new double[] { Double.NaN, 0 };
        } else {
            bd= new BigDecimal(s);
        }
        
        if ( bd.scale()>0 ) {
            double resolution= Math.pow( 10, -1*bd.scale() );
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
                    dd[1]= Math.pow( 10, -1*scale);
                }
                return dd;
            } else {
                mant= s.substring(0,ie);
                double[] dd= parseDecimal( mant );
                double exp= Math.pow( 10, Double.parseDouble( s.substring(ie+1) ) );
                dd[0] *= exp;
                dd[1] *= exp;
                return dd;
            }
        }
    }
    
    // note + and - are left out because of ambiguity with sign.
    private static Pattern expressionPattern= Pattern.compile( "(.+)(\\*)(.+)" );
    
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
        switch (operator) {
            case "*":
                result= operand1.multiply(operand2);
                break;
            case "/":
                result= operand1.divide(operand2);
                break;
            default:
                throw new IllegalArgumentException("Bad operator: "+operator+" of expression "+s);
        }
        return result;
    }
    
    /*
     * parse the string in the context of this.  If units are not
     * specified, then assume units are this.  Otherwise, parse the
     * unit and attempt to convert to this before creating the unit.
     * At some point, we introduced support for simple expressions like "3*22"
     * Note strings starting with "x" or "0x" are parsed as hexidecimal.
     */
    @Override
    public Datum parse(String s) throws ParseException {
        if ( false && expressionPattern.matcher(s).matches() ) {
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
                if ( this!=Units.dimensionless && s.endsWith(this.getId())  ) { //TODO: bug Units.seconds.parse("1 days"), Units.seconds.parse("1 microseconds"),
                    char cbefore= 0;
                    if ( s.length()>(this.getId().length()+1) ) {
                        cbefore= s.charAt(s.length()-2);
                    }
                    if ( !Character.isLetter(cbefore) ) {
                        s= s.substring(0,s.length()-this.getId().length());
                    }
                }
                if ( s.length()==0 ) {
                    throw new ParseException("String contains no numeric part to parse into Datum",0);
                }
                String[] ss= s.split("\\s+");
                // "1hr", watch for nan
                if ( ss.length==1 && Character.isLetter(s.charAt(s.length()-1))
                    && !s.startsWith("N") && !s.startsWith("n")    // NaN 
                    && !s.startsWith("x") && !s.startsWith("0x") ) // hexidecimal trigger.
                    {   
                    for ( int i=s.length()-1; i>=0; i-- ) {  // find the last number.  TODO: see DatumUtil.splitDatumString( String s )
                        if ( Character.isDigit(s.charAt(i)) ) {
                            String[] ss2= new String[2];
                            ss2[0]= ss[0].substring(0,i+1);
                            ss2[1]= ss[0].substring(i+1);
                            ss= ss2;
                            break;
                        }
                    }
                }
                double[] dd= parseDecimal(ss[0]);
                if ( ss.length==1 ) {
                    return Datum.create( dd[0], this, 0 );
                } else {
                    StringBuilder unitsString= new StringBuilder( ss[1] );
                    for ( int i=2; i<ss.length; i++ ) unitsString.append(" ").append(ss[i]);
                    Units u;
                    try {
                        u= Units.getByName(unitsString.toString());
                    } catch ( IllegalArgumentException e ) {
                        ParseException t= new ParseException(s, ss[0].length()+1 );
                        t.initCause(e);
                        throw t;
                    }
                    UnitsConverter uc= u.getConverter(this);
                    return Datum.create( uc.convert(dd[0]), this, 0. );
                }
            } catch (NumberFormatException nfe) {
                if ( s.equalsIgnoreCase("fill") ) {
                    return getFillDatum();
                } else {
                    ParseException pe = new ParseException("Unable to parse Datum in string: "+ s, 0);
                    pe.initCause(nfe);
                    throw pe;
                }
            }
        }
    }
    
    
    protected static Number add( Number a, Number value ) {
        if ( ( a instanceof Integer ) && ( value instanceof Integer ) ) {
            return a.intValue() + value.intValue();
        } else {
            return a.doubleValue()+value.doubleValue();
        }
    }
    
    protected static Number subtract( Number from, Number value ) {
        if ( ( from instanceof Integer ) && ( value instanceof Integer ) ) {
            return from.intValue() - value.intValue();
        } else {
            return from.doubleValue()-value.doubleValue();
        }
    }
    
    protected static Number divide( Number a, Number value ) {
        if ( ( a instanceof Integer ) && ( value instanceof Integer ) ) {
            return a.intValue() / value.intValue();
        } else {
            return a.doubleValue()/value.doubleValue();
        }
    }
    
    protected static Number multiply( Number a, Number value ) {
        if ( ( a instanceof Integer ) && ( value instanceof Integer ) ) {
            return a.intValue() * value.intValue();
        } else {
            return a.doubleValue()*value.doubleValue();
        }
    }
    
    @Override
    public Datum add( Number a, Number b, Units bUnits ) {
        if ( bUnits!=this ) {
            UnitsConverter uc= Units.getConverter( bUnits, this );
            b= uc.convert(b);
        }
        return createDatum( add( a, b ) );
    }
    
    @Override
    public Datum subtract( Number a, Number b, Units bUnits ) {
        if ( bUnits!=this ) {
            UnitsConverter uc= Units.getConverter( bUnits, this );
            b= uc.convert(b);
        }
        return createDatum( subtract( a, b ) );
    }
    
    @Override
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
    
    @Override
    public Datum divide( Number a, Number b, Units bUnits ) {
        if ( bUnits==Units.dimensionless ) {
            return createDatum( divide( a, b ) );
        } else if ( this==Units.dimensionless ) {
            try {
                Units inv= UnitsUtil.getInverseUnit(bUnits);
                return inv.createDatum( divide( a, b ) );
            } catch ( IllegalArgumentException ex ) {
                if ( bUnits.isConvertibleTo(Units.seconds) ) {
                    double factor= bUnits.convertDoubleTo( Units.seconds, 1 );
                    return Units.hertz.createDatum( divide( a, b ).doubleValue() / factor );
                } else {                    
                    throw new IllegalArgumentException("No inversion found for "+bUnits );
                }
            }
        } else {
            UnitsConverter uc= bUnits.getConverter(this);
            if ( uc==null ) throw new IllegalArgumentException("Only division by dimensionless or convertable Datums is supported");
            return Units.dimensionless.createDatum( divide( a, uc.convert(b) ) );
        }
    }
    
}
