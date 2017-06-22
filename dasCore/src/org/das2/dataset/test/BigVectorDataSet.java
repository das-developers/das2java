/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.dataset.test;

import org.das2.dataset.DataSet;
import org.das2.dataset.VectorDataSet;
import org.das2.dataset.VectorDataSetBuilder;
import org.das2.datum.Units;
import org.das2.util.monitor.ProgressMonitor;
import java.util.Random;
import org.das2.qds.QDataSet;
import org.das2.qds.util.DataSetBuilder;

/**
 *
 * @author jbf
 */
public class BigVectorDataSet {

    public static QDataSet getDataSet( int size, ProgressMonitor mon ) {
        double dsize= (double)size;
        
        System.err.println("enter getDataSet");
        long t0 = System.currentTimeMillis();

        Random random = new Random(0);

        DataSetBuilder vbd = new DataSetBuilder(1,100);
        DataSetBuilder xbd = new DataSetBuilder(1,100);

        double y = 0;
        for (int i = 0; i < size; i += 1) {
            y += random.nextDouble() - 0.5;
            if (i % 100 == 10) {
                vbd.putValue( i, Units.dimensionless.getFillDouble());
                xbd.putValue( i, i/dsize );
            } else {
                vbd.putValue( i, y);
                xbd.putValue( i, i/dsize );
            }
        }
        xbd.putProperty( QDataSet.MONOTONIC, Boolean.TRUE );
        vbd.putProperty( QDataSet.DEPEND_0, xbd.getDataSet() );
        vbd.putProperty( QDataSet.FILL_VALUE, Units.dimensionless.getFillDouble() );

        QDataSet vds = vbd.getDataSet();

        System.err.println("done getDataSet in " + (System.currentTimeMillis() - t0) + " ms");
        return vds;
    }
}
