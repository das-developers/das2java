/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.qdataset.schemes;

import org.das2.datum.Units;

/**
 *
 * @author jbf
 */
public interface EnhancedXYData extends XYData {
    boolean isFill(int index);
    Units getXUnits();
    Units getYUnits();
    String getXName();
    String getYName();
    String getXLabel();
    String getYLabel();
}

