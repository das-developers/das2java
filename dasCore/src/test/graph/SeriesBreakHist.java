/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test.graph;

import org.das2.datum.Datum;
import org.das2.graph.DasPlot;
import org.das2.graph.GraphUtil;
import org.das2.graph.SeriesRenderer;
import org.das2.qds.ArrayDataSet;
import org.das2.qds.DataSetUtil;
import org.das2.qds.QDataSet;
import org.das2.qds.ops.Ops;

/**
 *
 * @author jbf
 */
public class SeriesBreakHist extends PlotDemo {

     public static void main( String[] args ) {

        int len0= 10;

        ArrayDataSet y= ArrayDataSet.maybeCopy( Ops.multiply( DataSetUtil.asDataSet(30), Ops.sin(Ops.linspace(0,5*Math.PI,len0) ) ) );
        y.putProperty(QDataSet.VALID_MIN,-998);
        y.putValue(len0-2,-999);

        DasPlot p = GraphUtil.visualize(y);
        SeriesRenderer r= (SeriesRenderer) p.getRenderer(0);

        r.setHistogram(true);
        //r.setFillToReference(true);
        r.setReference( Datum.create(-100) );
    }

}
