/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test.graph;

import org.das2.dataset.test.BigVectorDataSet;
import org.das2.datum.Datum;
import org.das2.datum.Units;
import org.das2.graph.DasColorBar;
import org.das2.graph.DasColumn;
import org.das2.graph.DasPlot;
import org.das2.graph.DefaultPlotSymbol;
import org.das2.graph.GraphUtil;
import org.das2.graph.PsymConnector;
import org.das2.graph.SeriesRenderer;
import org.das2.util.monitor.NullProgressMonitor;
import java.util.Random;
import org.das2.graph.LegendPosition;
import org.das2.qds.DDataSet;
import org.das2.qds.DataSetOps;
import org.das2.qds.DataSetUtil;
import org.das2.qds.MutablePropertyDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.ops.Ops;
import org.das2.qds.util.DataSetBuilder;

/**
 *
 * @author jbf
 */
public class PlotSymbolRendererDemo {

    public static DasPlot doit() {

        QDataSet vds = BigVectorDataSet.getDataSet(100000, new NullProgressMonitor());

        DasPlot p = GraphUtil.visualize(vds);
        return p;
    }

    public static DasPlot doit1() {

        QDataSet ds = BigVectorDataSet.getDataSet(1000, new NullProgressMonitor());
        QDataSet xds = ds.trim(0,500);
        QDataSet yds = ds.trim(500,1000);

        QDataSet ds2= Ops.link( xds, yds );

        DasPlot p = GraphUtil.visualize(ds2);
        return p;
    }

    /**
     * tests scheme where color( X, Y ) and X and Y are not monotonic.
     * If bundleScheme is true, then ds[ :, "X,Y,color" ] is the scheme.
     * @param bundleScheme
     */
    public static DasPlot doit2( boolean bundleScheme ) {
        int size= 20000;
        double dsize = (double) size;

        System.err.println("enter getDataSet");
        long t0 = System.currentTimeMillis();

        Random random = new Random(0);

        DataSetBuilder vbd = new DataSetBuilder( 2, 100, 3 );
        vbd.putProperty( QDataSet.FILL_VALUE, Units.dimensionless.getFillDouble() );

        double y = 0;
        for (int i = 0; i < size; i += 1) {
            y += random.nextDouble() - 0.5;
            if (i % 100 == 10) {
                vbd.putValue( i, 0, i / dsize );
                vbd.putValue( i, 1, Units.dimensionless.getFillDouble() );
                vbd.putValue( i, 2, 0 );
            } else {
                vbd.putValue( i, 0, i / dsize );
                vbd.putValue( i, 1, y );
                vbd.putValue( i, 2, (i-10)/100 );
            }
        }

        vbd.putProperty( QDataSet.DEPEND_1, Ops.labelsDataset(new String[] { "x", "y", "color" } ) );

        MutablePropertyDataSet vds = vbd.getDataSet();
        
        if ( !bundleScheme ) {
            // test table scheme vds[:,3] with BundleDescriptor
            MutablePropertyDataSet vds1= DataSetOps.slice1( vds,1 );
            vds1.putProperty( QDataSet.DEPEND_0, DataSetOps.slice1(vds,0) );
            vds1.putProperty( QDataSet.PLANE_0, DataSetOps.slice1(vds,2) );
            vds= vds1;
        }

        System.err.println("done getDataSet in " + (System.currentTimeMillis() - t0) + " ms");

        DasPlot p = GraphUtil.visualize(vds); //TODO: this should fail right now.
        p.getColumn().setEmMaximum(-10);

        p.removeRenderer(p.getRenderer(0));

        //PlotSymbolRenderer r2= new PlotSymbolRenderer();
        SeriesRenderer r2 = new SeriesRenderer();

        DasColorBar cb= new DasColorBar( Datum.create(0), Datum.create(100), false );
        p.getCanvas().add( cb, p.getRow(), DasColumn.create( null, p.getColumn(), "100%+2em", "100%+4em" ) );
        
        r2.setColorBar(cb);
        
        r2.setPsym(DefaultPlotSymbol.CIRCLES);
        r2.setSymSize(10.0);
        r2.setPsymConnector( PsymConnector.NONE );
        
        r2.setColorByDataSetId("color");
        
        p.addRenderer(r2);

        r2.setDataSet(vds);

        p.setPreviewEnabled(true);
        p.getXAxis().setAnimated(false);
        p.getYAxis().setAnimated(false);

        return p;
    }

    public static DasPlot doit3() {
        int len0= 100;
        QDataSet dy= Ops.add( DataSetUtil.asDataSet(5), Ops.randn(len0) );
        MutablePropertyDataSet y= (MutablePropertyDataSet)Ops.multiply( DataSetUtil.asDataSet(30), Ops.sin(Ops.linspace(0,5*Math.PI,len0) ) );
        y.putProperty(QDataSet.DELTA_MINUS, dy);
        y.putProperty(QDataSet.DELTA_PLUS, dy);

        DasPlot p = GraphUtil.visualize(y);
        return p;
    }

    public static DasPlot doit4() {

        QDataSet ds = BigVectorDataSet.getDataSet(1500, new NullProgressMonitor());
        QDataSet xds = ds.trim(0,500);
        QDataSet yds = ds.trim(500,1000);
        QDataSet cds= Ops.findgen(500);

        QDataSet ds2= Ops.link( xds, yds, cds );

        DasPlot p = GraphUtil.visualize(ds2);
        return p;
    }


    public static void main(String[] args) {
        DasPlot p;
        //doit();
        //doit1();
        //doit2(true);
        //doit3();
        p=doit4();

        //p.setLegendPosition( LegendPosition.OutsideNE );
        p.setLegendPosition( LegendPosition.SE );
        p.getRenderer(0).setLegendLabel("Test!cTest");
        p.getRenderer(0).setDrawLegendLabel(true);

        System.err.println("done");
    }
}
