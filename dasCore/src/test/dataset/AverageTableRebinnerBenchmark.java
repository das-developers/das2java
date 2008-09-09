/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test.dataset;

import org.das2.DasException;
import edu.uiowa.physics.pw.das.dataset.AverageTableRebinner;
import edu.uiowa.physics.pw.das.dataset.RebinDescriptor;
import edu.uiowa.physics.pw.das.dataset.TableDataSet;
import edu.uiowa.physics.pw.das.dataset.test.RipplesDataSet;
import edu.uiowa.physics.pw.das.datum.Units;

/**
 *
 * @author jbf
 */
public class AverageTableRebinnerBenchmark {
    public static void main( String[] args ) throws DasException {
        TableDataSet tds= new RipplesDataSet(  2, 3, 1, 13, 15, 2, 30, 30 );
        Units u= Units.dimensionless;
        RebinDescriptor ddx= new RebinDescriptor( u.createDatum(0), u.createDatum(30), 1000, false );
        RebinDescriptor ddy= new RebinDescriptor( u.createDatum(0), u.createDatum(30), 1000, false );
        
        AverageTableRebinner rebin= new AverageTableRebinner();
        
        long t0= System.currentTimeMillis();
        long totalMillis=0;
        for ( int j=0; j<30.; j++ ) {
            rebin.rebin(tds, ddy, ddy);
            long t1= System.currentTimeMillis() - t0;
            System.err.println( t1 );
            totalMillis+= t1;
            t0= System.currentTimeMillis();
        }
        System.err.println("average: "+totalMillis/30.);
        
    }
}
