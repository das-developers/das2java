
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
    
    public String transmogrify( String string ) {
        Matcher m= p.matcher(string);
        if ( m.matches() ) {
            String[] args= getFields(m);
            return String.format( result, (Object[])args );
        } else {
            return result;
        }
    }
}
