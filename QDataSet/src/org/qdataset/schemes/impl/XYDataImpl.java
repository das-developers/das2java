/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.qdataset.schemes.impl;

import org.qdataset.schemes.XYData;
import org.virbo.dataset.QDataSet;

/**
 *
 * @author jbf
 */
class XYDataImpl implements XYData {
    
    protected QDataSet x;
    protected QDataSet y;

    protected XYDataImpl( QDataSet ds ) {
        this.y= ds;
        this.x= (QDataSet)ds.property(QDataSet.DEPEND_0);
    }
    
    @Override
    public int size() {
        return y.length();
    }

    @Override
    public double getX(int i) {
        return x.value(i);
    }

    @Override
    public double getY(int i) {
        return y.value(i);
    }
    
}
