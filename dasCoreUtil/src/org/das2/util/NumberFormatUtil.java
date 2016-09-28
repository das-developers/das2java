/*
 * NumberFormatUtil.java
 *
 * Created on 2. listopad 2007, 16:27
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * provide convenient and consistent access to DecimalFormat objects.
 * @author jbf
 */
public class NumberFormatUtil {

    /**
     * handles the localization problem (bug 0000294) by always returning a DecimalFormat
     * for Locale.US. (Sorry, rest of world.)
     *
     * @return DecimalFormat
     * @throws ClassCastException if for some reason, NumberFormat.getInstance doesn't return DecimalFormat.
     */
    public static DecimalFormat getDecimalFormat( ) {
        // see doc for DecimalFormat, which recommends this practice.
        DecimalFormat result= (DecimalFormat) NumberFormat.getInstance( Locale.US );
        result.setRoundingMode( java.math.RoundingMode.HALF_UP );
        return result;
    }
    
    /**
     * handles the localization problem (bug 0000294) by always returning a DecimalFormat
     * for Locale.US. (Sorry, rest of world.)
     * @param spec specification for DecimalFormat, like 0.000E00
     * @return  DecimalFormat
     * @throws ClassCastException if for some reason, NumberFormat.getInstance doesn't return DecimalFormat.
     */
    public static DecimalFormat getDecimalFormat( String spec ) {
        DecimalFormat result= (DecimalFormat) NumberFormat.getInstance( Locale.US );
        result.applyPattern(spec);   
        result.setRoundingMode( java.math.RoundingMode.HALF_UP );
        return result;        
    }
}
