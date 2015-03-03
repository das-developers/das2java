/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.util;

import java.util.StringTokenizer;

/**
 * Lightweight converter for LaTeX expressions to Granny strings, for
 * MMS mission.  This handles strings like:<ul>
 * <li> cm^{-3} 
 * <li> nA/m^{2} 
 * <li> \rho^2 + 2\Gamma_{ij}
 * <li> \sqrt{a + b}  (This is not handled by the IDL library either.)
 * </ul>
 * 
 * @author jbf
 */
public class LatexToGranny {
    private static final String STATE_OPEN="open";
    private static final String STATE_CAROT="carot";
    private static final String STATE_UNDERSCORE="underscore";
    private static final String STATE_EXP="exp";
    private static final String STATE_SUBSCRIPT="subscript";
    private static final String STATE_BACKSLASH="backslash";
    
    public static String latexToGranny( String latex ) {
        Object state= STATE_OPEN;
        StringTokenizer st= new StringTokenizer( latex, "^_\\{}+ ", true );
        
        StringBuilder build= new StringBuilder();
        
        String t;
        
        while ( st.hasMoreTokens() ) {
            t= st.nextToken();
            if ( t.equals("^") ) {
                state= STATE_CAROT;
            } else if ( t.equals("_") ) {
                state= STATE_UNDERSCORE;
            } else if ( t.equals("\\") ) {
                state= STATE_BACKSLASH;
            } else if ( t.equals("{") ) {
                if ( state==STATE_CAROT ) {
                    build.append("!U");
                    state= STATE_EXP;
                } else if ( state==STATE_UNDERSCORE ) {
                    build.append("!D");
                    state= STATE_EXP;                    
                } else {
                    build.append("{");
                    state= STATE_OPEN;
                }
            } else if ( t.equals("}") ) {
                if ( state==STATE_EXP ) {
                    build.append("!N");
                    state= STATE_OPEN;
                } else if ( state==STATE_SUBSCRIPT ) {
                    build.append("!N");
                    state= STATE_OPEN;
                } else {
                    build.append("}");
                    state= STATE_OPEN;
                }
            } else {
                if ( state==STATE_BACKSLASH ) {
                    build.append("&").append( lookupEntity(t) ).append(";");
                    state= STATE_OPEN;
                } else if ( state==STATE_CAROT ) {
                    build.append( "!U" );
                    build.append( t );
                    build.append( "!N" );
                    state= STATE_OPEN;
                } else {
                    build.append( t );
                }
            }
        }
        
        return build.toString();
    }
    
    /**
     * detect if the string is LaTeX.
     * @param s
     * @return true if the string appears to be LaTeX.
     */
    public static boolean isLatex( String s ) {
        if ( s.contains("^{") || s.contains("_{") ) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * look up the entity for the given latex symbol.  E.g.:
     * rho->rho
     * @return the entity
     */
    private static String lookupEntity( String ent ) {
        return ent;
        //if ( ent.equals("sqrt") ) {
        //    return "radic";
        //} else {
        //    return ent;
        //}
    }
    
    public static void main( String[] args ) {
        String[] tests= { "\\rho^2 + 2\\Gamma_{ij}        ",
                "cm^{-3}",
            "nA/m^{2}",
            "\\rho^2 + 2\\Gamma_{ij}        ", 
            "A^{B+C}",
            "\\sqrt{a + b}" };
        for ( String t: tests ) {
            System.err.println( t );
            System.err.println( latexToGranny( t ) );
        }
    }
}
