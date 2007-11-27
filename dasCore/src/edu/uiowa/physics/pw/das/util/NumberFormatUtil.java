/*
 * NumberFormatUtil.java
 *
 * Created on 2. listopad 2007, 16:27
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.uiowa.physics.pw.das.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 *
 * @author jbf
 */
public class NumberFormatUtil {

    /**
     * handles the localization problem (bug 0000294) by always returning a DecimalFormat
     * for Locale.US. (Sorry, rest of world.)
     *
     * @throws ClassCastException if for some reason, NumberFormat.getInstance doesn't return DecimalFormat.
     */
    public static DecimalFormat getDecimalFormat( ) {
        // see doc for DecimalFormat, which recommends this practice.
        DecimalFormat result= (DecimalFormat) NumberFormat.getInstance( Locale.US );
        return result;
    }
    
    /**
     * handles the localization problem (bug 0000294) by always returning a DecimalFormat
     * for Locale.US. (Sorry, rest of world.)
     * @throws ClassCastException if for some reason, NumberFormat.getInstance doesn't return DecimalFormat.
     */
    public static DecimalFormat getDecimalFormat( String spec ) {
        DecimalFormat result= (DecimalFormat) NumberFormat.getInstance( Locale.US );
        result.applyPattern(spec);   
        return result;        
    }
}
