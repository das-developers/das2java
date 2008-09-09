/*
 * FixedWidthDecimalFormatter.java
 *
 * Created on January 5, 2004, 2:30 PM
 */

package org.das2.util;

import java.text.*;

/**
 *
 * @author  Jeremy
 */
public class FixedWidthFormatter {
    
    public static String format( String s, int nchars ) {
        if ( s.length() < nchars ) {
            StringBuffer sb= new StringBuffer(nchars-s.length());            
            for ( int i=0; i<(nchars-s.length()); i++ ) {
                sb.append(' ');               
            }            
            s= sb.toString()+s;
        } else if ( s.length() > nchars ) {
            StringBuffer sb= new StringBuffer(nchars);            
            for ( int i=0; i<(nchars); i++ ) {
                sb.append('*');               
            }    
            s= sb.toString();
        }       
        return s;
    }     
    
}
