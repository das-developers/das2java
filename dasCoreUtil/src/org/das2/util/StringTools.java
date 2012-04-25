/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author jbf
 */
public class StringTools {

    /**
     * splits the string, guarding the space within protect.
     * @param delim the delimiter we split on.  (similar to s.split(delim,-2))
     * @param protect character that blocks off delimiter, such as quote.
     * @return
     */
    public static String[] guardedSplit( String str, String delim, char protect ) {
        byte[] copy= str.getBytes( Charset.forName("US-ASCII") );
        if ( Pattern.compile(str).matcher( String.valueOf(delim) ).matches() ) {
            throw new IllegalArgumentException("the delimiter cannot match the protect character");
        }
        byte hide='_';
        if ( Pattern.compile(str).matcher( String.valueOf(hide) ).matches() ) {
            throw new IllegalArgumentException("the delimiter cannot match _");
        }

        boolean inside= false;
        boolean escape= false; // \"
        for ( int i=0; i<copy.length; i++ ) {
            if ( copy[i]==protect && !escape ) {
                if ( inside ) {
                    inside= false;
                    copy[i]= hide;
                } else {
                    inside= true;
                }
            }
            escape= copy[i]=='\\';
            if ( inside ) copy[i]= hide;
        }

        String scopy= new String(copy);

        ArrayList<String> result= new ArrayList();

        Pattern spl= Pattern.compile(delim);
        Matcher m= spl.matcher(scopy);
        int i=0;
        while ( m.find() ) {
            int i0= i;
            int i1= m.start();
            result.add( str.substring(i0,i1) );
            i= m.end();
        }
        result.add( str.substring(i) );

        return result.toArray( new String[result.size()] );

    }

    public static void main( String[] args ) {
        String s= "a b c \"d \\\" e\" f";
        System.err.println( guardedSplit( s, " ", '"' ) );
    }
}
