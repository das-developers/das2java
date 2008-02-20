/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.physics.pw.das.dataset.test;

import edu.uiowa.physics.pw.das.dataset.DataSet;
import edu.uiowa.physics.pw.das.dataset.VectorDataSet;
import edu.uiowa.physics.pw.das.dataset.VectorDataSetBuilder;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.util.DasProgressMonitor;
import java.util.Random;

/**
 *
 * @author jbf
 */
public class BigVectorDataSet {

    public static VectorDataSet getDataSet( int size, DasProgressMonitor mon ) {
        double dsize= (double)size;
        
        System.err.println("enter getDataSet");
        long t0 = System.currentTimeMillis();

        Random random = new Random(0);

        VectorDataSetBuilder vbd = new VectorDataSetBuilder(Units.dimensionless, Units.dimensionless);
        double y = 0;
        for (int i = 0; i < size; i += 1) {
            y += random.nextDouble() - 0.5;
            if (i % 100 == 10) {
                vbd.insertY( i / dsize, Units.dimensionless.getFillDouble());
            } else {
                vbd.insertY( i / dsize, y);
            }
        }
        vbd.setProperty(DataSet.PROPERTY_X_MONOTONIC, Boolean.TRUE);
        VectorDataSet vds = vbd.toVectorDataSet();
        System.err.println("done getDataSet in " + (System.currentTimeMillis() - t0) + " ms");
        return vds;
    }
}
