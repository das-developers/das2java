/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dataset;

import org.das2.datum.Units;

/**
 * Common expressions that apply semantics to QDataSet.  Introduced
 * to reduce a lot of repeated code, but also to make it clear where semantics
 * are being applied.
 * @author jbf
 */
public class SemanticOps {

    public final static Units getUnits(QDataSet ds) {
        Units u = (Units) ds.property(QDataSet.UNITS);
        return u == null ? Units.dimensionless : u;
    }

    /**
     * return the labels for a dataset where DEPEND_1 is a bundle dimension.
     * @param ds
     * @return
     */
    public final static String[] getComponentLabels(QDataSet ds) {
        int n = ds.length(0);
        QDataSet labels = (QDataSet) ds.property(QDataSet.DEPEND_1);
        if (labels == null) {
            String[] result = new String[n];
            for (int i = 0; i < n; i++) {
                result[i] = "ch_" + i;
            }
            return result;
        } else {
            Units u = getUnits(labels);
            String[] slabels = new String[n];
            for (int i = 0; i < n; i++) {
                if (labels == null) {
                    slabels[i] = String.valueOf(i);
                } else {
                    slabels[i] = String.valueOf(u.createDatum(labels.value(i)));
                }
            }
            return slabels;
        }
    }
}
