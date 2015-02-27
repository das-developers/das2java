/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.qdataset.schemes.impl;

import org.qdataset.schemes.UncertaintyProvider;
import org.virbo.dataset.QDataSet;

/**
 *
 * @author jbf
 */
public class UncertaintyProviderImpl implements UncertaintyProvider {
    
    QDataSet deltaPlus;
    QDataSet deltaMinus;
    
    protected UncertaintyProviderImpl( QDataSet ds ) {
        deltaPlus= (QDataSet) ds.property(QDataSet.DELTA_PLUS);
        deltaMinus= (QDataSet) ds.property(QDataSet.DELTA_MINUS);
    }

    @Override
    public double getUncertPlus(int i) {
        return deltaPlus.value(i);
    }

    @Override
    public double getUncertMinus(int i) {
        return deltaMinus.value(i);
    }
}
