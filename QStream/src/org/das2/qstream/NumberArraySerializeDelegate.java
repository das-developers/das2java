/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qstream;

import java.lang.reflect.Array;
import java.text.ParseException;

/**
 *
 * @author jbf
 */
public class NumberArraySerializeDelegate implements SerializeDelegate {

    protected static final String TYPE_NUMBER_ARRAY= "numberArray";

    public String format(Object o) {
        StringBuilder result= new StringBuilder();
        for ( int i=0; i<Array.getLength(o); i++ ) {
            if ( i>0 ) result.append(",");
            result.append( Array.get( o,i ) );
        }
        return result.toString();
    }

    public Object parse(String typeId, String s) throws ParseException {
        String[] ss= s.split(",");
        double[] result= new double[ss.length];
        for ( int i=0; i<result.length; i++ ) {
            try {
                result[i]= Double.parseDouble(ss[i]);
            } catch ( NumberFormatException ex ) {
                result[i]= 0;
            }
        }
        return result;
    }

    public String typeId(Class clas) {
        return TYPE_NUMBER_ARRAY;
    }

}
