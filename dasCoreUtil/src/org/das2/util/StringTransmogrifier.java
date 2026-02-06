
package org.das2.util;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Apply an operation to a String to make it a String with similar meaning.  This is needed for
 * new code which indicates the orbit number, but the orbit number context is provided elsewhere 
 * on the plot.
 * 
 * https://calvinandhobbes.fandom.com/wiki/Transmogrifier_Gun?file=CalvinTransmogrifierGunWeb.jpg
 *
 * A regular expression specifies the text to look for in strings, and the result is reformatted
 * using a result template string.  The transmogrify method will convert the input string to 
 * the template, so for example:
 *
 * <pre>%{@code
 * st= StringTransmogrifier('([a-z]+)=([0-9]+)','$1:$2')
 * st.transmogrify('cat=1')   # results in 'cat:1'
 * }</pre>
 * 
 * @author jbf
 */
public class StringTransmogrifier {
    
    Pattern p;
    String result;
    int nf;
    String[] byName;
    int[] byIndex;
    
    /**
     * Create a regex-based transmogrifier. The result is the template which is
     * populated from groups of the regular expression.  The groups can be referenced
     * by index or by name, with 
     * <ul>
     * <li>$&lt;num&gt; by index, where 1 is the first group number
     * <li>$&lt;letter&gt; by 1-letter named group
     * <li>$&lt;{name}&gt; by named group
     * </ul>
     * @param regex the regular expression, like "foo([a-z]+).cdf"
     * @param result the result, like "$1"
     */
    public StringTransmogrifier( String regex, String result ) {
        p= Pattern.compile(regex);
        String[] ss= result.split("\\$");
        nf= ss.length-1;
        byIndex= new int[nf];
        byName= new String[nf];
        StringBuilder resultb= new StringBuilder(ss[0]);
        
        int fieldNum=0;
        
        for ( int i=1; i<ss.length; i++ ) {
            if ( ss[i].length()==0 ) {
                i++;
                resultb.append("$");
                nf= nf-2;
            } else {
                String s= ss[i];
                if ( Character.isDigit(s.charAt(0)) ) {
                    resultb.append("%s");
                    if ( s.length()>1 && Character.isDigit(s.charAt(1)) ) {
                        byIndex[fieldNum]= ( s.charAt(0) - 48 ) * 10 + ( s.charAt(1) - 48 );
                        resultb.append(s.substring(2));
                    } else {
                        byIndex[fieldNum]= ( s.charAt(0) - 48 );
                        resultb.append(s.substring(1));
                    }
                } else if ( s.charAt(0)=='{' ) {
                    int i2= s.indexOf("}");
                    byName[fieldNum]= s.substring(1,i2);
                    resultb.append("%s");
                    resultb.append(s.substring(i2+1));
                } else {
                    byName[fieldNum]= s.substring(0,1);
                    resultb.append("%s");
                    resultb.append(s.substring(1));
                }       
                fieldNum++;
            }
        }
        if ( fieldNum<byName.length ) {
            byName= Arrays.copyOf( byName, fieldNum );
            byIndex= Arrays.copyOf( byIndex, fieldNum );
            nf= fieldNum;
        }
        this.result= resultb.toString();
        
    }
    
    private String[] getFields( Matcher m ) {
        if ( nf==-1 ) throw new IllegalArgumentException("getFields cannot be called until transmogrify is called");
        String[] result= new String[nf];
        for ( int i=0; i<nf; i++ ) {
            if ( byName[i]!=null ) {
                result[i]= m.group(byName[i]);
            } else {
                result[i]= m.group(byIndex[i]);
            }
        }
        return result;
    }
    
    /**
     * Transmogrify the string, and if it does not match the regex, then an IllegalArgumentException is thrown.
     * @param string the string to transmogrify
     * @return the formatted result 
     * @throws IllegalArgumentException if the regex doesn't match the string
     */
    public String transmogrify( String string ) {
        Matcher m= p.matcher(string);
        if ( m.matches() ) {
            String[] args= getFields(m);
            return String.format( result, (Object[])args );
        } else {
            throw new IllegalArgumentException("string does not match");
        }
    }
    
    /**
     * Transmogrify the string, and if it does not match the regex, then return err.
     * @param string the string to transmogrify
     * @param err alternate string (or null) to return
     * @return the formatted result or the <code>err</code> string
     */
    public String transmogrify( String string, String err ) {
        Matcher m= p.matcher(string);
        if ( m.matches() ) {
            String[] args= getFields(m);
            return String.format( result, (Object[])args );
        } else {
            return err;
        }
    }
    
    /**
     * replace all matches of the <code>regex</code> in <code>string</code> with <code>result</code>.
     * @param string the string containing any text to transmogrify
     * @return the string with each match reformatted within the string
     */
    public String find( String string ) {
        Matcher m= p.matcher(string);
        StringBuilder build= new StringBuilder();
        int i0=0;
        while ( m.find() ) {
            build.append( string.substring(i0,m.start()) );
            String[] args= getFields(m);
            build.append( String.format( result, (Object[])args ) );
            i0= m.end();
        }
        build.append(string.substring(i0));
        return build.toString();
    }
}
