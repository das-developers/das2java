/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dataset;

import org.das2.datum.Units;
import org.virbo.dsops.Ops;

/**
 * return a function based on a QDataSet.
 * @author jbf
 */
public class NearestNeighborQFunction extends AbstractQFunction {

    QDataSet dep0;
    QDataSet data;
    QDataSet fill;
    
    public NearestNeighborQFunction( QDataSet ds ) {
        this.dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
        this.data= ds;
        this.fill= Ops.join( null, DataSetUtil.asDataSet( Double.NaN, SemanticOps.getUnits(ds) ) );
    }
    
    /**
     * return the index of the closest data.
     * @param d
     * @return 
     */
    private int closest( QDataSet d ) {
        return (int)Ops.findex( dep0, d ).value();
    }
    
    @Override
    public QDataSet value(QDataSet parm) {
        int i= closest( parm.slice(0) );
        if ( i<0 ) return fill;
        if ( i>=data.length() ) return fill;
        if ( data.rank()==1 ) {
            return Ops.join( null, data.slice( i ) );
        } else {
            return data.slice( i );
        }
    }

    DDataSet inputDescriptor = DDataSet.createRank2(1,0);
    {
        inputDescriptor.putProperty(QDataSet.LABEL, 0, "Time");
        inputDescriptor.putProperty(QDataSet.UNITS, 0, Units.t2000);
    }
        
    @Override
    public QDataSet exampleInput() {
        QDataSet q = DataSetUtil.asDataSet(0,Units.t2000);
        MutablePropertyDataSet ret = (MutablePropertyDataSet) Ops.bundle(null,q);
        ret.putProperty(QDataSet.BUNDLE_0,inputDescriptor);
        return ret;
    }
    
}
