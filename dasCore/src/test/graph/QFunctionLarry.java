/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test.graph;

import org.das2.datum.Units;
import org.das2.qds.AbstractQFunction;
import org.das2.qds.DDataSet;
import org.das2.qds.DataSetOps;
import org.das2.qds.DataSetUtil;
import org.das2.qds.MutablePropertyDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.ops.Ops;

/**
 * Example of a QFunction used to label the TCAs. 
 * @author jbf
 */
public class QFunctionLarry extends AbstractQFunction {

	DDataSet descriptor = DDataSet.createRank2(2,0);
	{
		descriptor.putProperty(QDataSet.LABEL,0,"Sloppy UTC Seconds");
		descriptor.putProperty(QDataSet.UNITS,0,Units.seconds);
		descriptor.putProperty(QDataSet.FORMAT,0,"%5.2f");

		descriptor.putProperty(QDataSet.LABEL,1,"UTC Seconds");
		descriptor.putProperty(QDataSet.UNITS,1,Units.seconds);
		descriptor.putProperty(QDataSet.FORMAT,1,"%5.1f");
	}

	@Override
	public QDataSet value(QDataSet parm) {

            MutablePropertyDataSet q = (MutablePropertyDataSet) DataSetOps.unbundle(parm, 0);
            //We're going to do this by hand for now since it's broken

            Units u= (Units)  parm.property(QDataSet.UNITS,0);
            if ( u==null ) u= (Units) ((QDataSet)parm.property(QDataSet.BUNDLE_0)).property( QDataSet.UNITS,0 );
            q.putProperty(QDataSet.UNITS,u);

            DDataSet ret = DDataSet.createRank1(2);
            ret.putValue(0,98.12345678);
            ret.putValue(1,99.12345678);
            ret.putProperty(QDataSet.BUNDLE_0,descriptor);

            return ret;

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
