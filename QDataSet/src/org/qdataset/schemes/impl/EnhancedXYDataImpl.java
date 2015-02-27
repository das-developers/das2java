/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.qdataset.schemes.impl;

import org.das2.datum.SIUnits;
import org.das2.datum.Units;
import org.qdataset.schemes.EnhancedXYData;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dsops.Ops;

/**
 *
 * @author jbf
 */
public class EnhancedXYDataImpl extends XYDataImpl implements EnhancedXYData {

    private QDataSet valid;
    
    protected EnhancedXYDataImpl( QDataSet ds ) {
        super( ds );
        this.valid= Ops.valid(y);
        
    }
    
    @Override
    public boolean isFill(int index) {
        return valid.value(index)==0;
    }

    @Override
    public Units getXUnits() {
        return SemanticOps.getUnits(x);
    }

    @Override
    public Units getYUnits() {
        return SemanticOps.getUnits(y);
    }

    @Override
    public String getXName() {
        return (String) x.property(QDataSet.NAME);
    }

    @Override
    public String getYName() {
        return (String) y.property(QDataSet.NAME);
    }

    @Override
    public String getXLabel() {
        return (String) x.property( QDataSet.LABEL );
    }

    @Override
    public String getYLabel() {
        return (String) y.property( QDataSet.LABEL );
    }
    
}
