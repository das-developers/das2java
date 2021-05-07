/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qds.filters;

import org.das2.qds.QDataSet;

/**
 * Utility classes 
 * @author jbf
 */
public class FilterEditorPanelUtil {
      
    /**
     * return the names of each dimension of the rank N dataset.
     * @param ds rank N dataset.
     * @return the name of each dimension.
     */
    protected static String[] getDimensionNames( QDataSet ds ) {
        String[] depNames = new String[ds.rank()];
        for (int i = 0; i < ds.rank(); i++) {
            depNames[i] = "dim" + i;
            QDataSet dep0 = (QDataSet) ds.property("DEPEND_" + i);
            if (dep0 != null) {
                String dname = (String) dep0.property(QDataSet.NAME);
                if (dname != null) {
                    if ( i>1 && dep0.rank()==3 ) {
                        depNames[i] = dname + " ("+dep0.length(0,0)+" bins)";
                    } else if ( i>0 && dep0.rank()==2 ) {
                        depNames[i] = dname + " ("+dep0.length(0)+" bins)";
                    } else {
                        depNames[i] = dname + " ("+dep0.length()+" bins)";
                    }
                }
            }
        }
        return depNames;
    }  
    
    /**
     * sloppy regex to be used when the number doesn't need to be parsed.
     * @return regular expression that would match a decimal, without whitespace.
     */
    public static String decimalRegexSloppy() {
        return "[0-9eE\\+\\-\\.]+";
    }
    
    /**
     * return regular expression for decimal regex, without leading 
     * or trailing whitespace.
     * From http://www.regular-expressions.info/floatingpoint.html
     * modified to allow "1.e4"
     * @return regular expression for a decimal, without whitespace.  Note it contains (groups)!
     */
    public static String decimalRegex() {
        return "[-+]?[0-9]*\\.?[0-9]*([eE][-+]?[0-9]+)?";
    }
}
