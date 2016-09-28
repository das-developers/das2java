/*
 * FixedWidthDecimalFormatter.java
 *
 * Created on January 5, 2004, 2:30 PM
 */

package org.das2.util;

/**
 *
 * @author  Jeremy
 */
public class FixedWidthFormatter {
    private final static String spaces= "                                  ";
    private final static String stars= "**********************************";
    
    public static String format( String s, int nchars ) {
        if ( nchars>stars.length() ) {
            return formatWide( s, nchars );
        }
        int pad= nchars - s.length();
        if ( pad>0 ) {
            s= spaces.substring(0,pad) + s;
            
        } else if ( s.length() > nchars ) {
            s= stars.substring(0,nchars);
            
        }       
        return s;
    }     
    
    public static String formatWide( String s, int nchars ) {
        if ( s.length() < nchars ) {
            StringBuilder sb= new StringBuilder(nchars-s.length());            
            for ( int i=0; i<(nchars-s.length()); i++ ) {
                sb.append(' ');               
            }            
            s= sb.toString()+s;
        } else if ( s.length() > nchars ) {
            StringBuilder sb= new StringBuilder(nchars);            
            for ( int i=0; i<(nchars); i++ ) {
                sb.append('*');               
            }    
            s= sb.toString();
        }       
        return s;
    }     
}
