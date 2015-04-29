/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.filters;

import org.virbo.dataset.QDataSet;

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
                    if ( i>0 && dep0.rank()==2 ) {
                        depNames[i] = dname + " ("+dep0.length(0)+" bins)";
                    } else {
                        depNames[i] = dname + " ("+dep0.length()+" bins)";
                    }
                }
            }
        }
        return depNames;
    }  
}
