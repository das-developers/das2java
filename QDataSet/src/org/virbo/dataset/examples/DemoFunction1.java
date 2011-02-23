/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.dataset.examples;

import org.das2.datum.Units;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.MutablePropertyDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.QFunction;
import org.virbo.dsops.Ops;

/**
 * First example function just returns sine of the function.
 * @author jbf
 */
public class DemoFunction1 implements QFunction {

    public QDataSet value(QDataSet parm) {
        return Ops.sin(parm);
    }

    public QDataSet exampleInput() {
       MutablePropertyDataSet bds= DDataSet.createRank2(1,1);
       bds.putProperty( QDataSet.UNITS, 0, Units.radians );
       bds.putProperty( QDataSet.TYPICAL_MIN, 0, -Math.PI );
       bds.putProperty( QDataSet.TYPICAL_MAX, 0, Math.PI );
       bds.putProperty( QDataSet.CADENCE, DataSetUtil.asDataSet( 0.001, Units.radians ) );  // to give a sense of a scale of the structure.

       DDataSet v= DDataSet.createRank1( 1 );
       v.putValue( 0, 0 );

       v.putProperty( QDataSet.BUNDLE_0, bds );

       return v;
    }

}
