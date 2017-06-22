/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qds.examples;

import org.das2.datum.Units;
import org.das2.qds.AbstractQFunction;
import org.das2.qds.DDataSet;
import org.das2.qds.DataSetUtil;
import org.das2.qds.MutablePropertyDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.ops.Ops;

/**
 * First example function just returns sine of the function.
 * @author jbf
 */
public class DemoFunction1 extends AbstractQFunction {

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
