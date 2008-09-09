/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test.graph;

import org.das2.dataset.DataSet;
import org.das2.dataset.VectorDataSet;
import org.das2.dataset.VectorDataSetBuilder;
import org.das2.dataset.test.BigVectorDataSet;
import org.das2.datum.Datum;
import org.das2.datum.Units;
import edu.uiowa.physics.pw.das.graph.DasColorBar;
import edu.uiowa.physics.pw.das.graph.DasColumn;
import edu.uiowa.physics.pw.das.graph.DasPlot;
import edu.uiowa.physics.pw.das.graph.DefaultPlotSymbol;
import edu.uiowa.physics.pw.das.graph.GraphUtil;
import edu.uiowa.physics.pw.das.graph.PsymConnector;
import edu.uiowa.physics.pw.das.graph.SeriesRenderer;
import org.das2.util.monitor.NullProgressMonitor;
import java.util.Random;

/**
 *
 * @author jbf
 */
public class PlotSymbolRendererDemo {

    public static void doit() {

        VectorDataSet vds = BigVectorDataSet.getDataSet(100000, new NullProgressMonitor());

        DasPlot p = GraphUtil.visualize(vds);

        p.removeRenderer(p.getRenderer(0));

        //PlotSymbolRenderer r2= new PlotSymbolRenderer();
        SeriesRenderer r2 = new SeriesRenderer();

        r2.setPsym(DefaultPlotSymbol.CIRCLES);
        r2.setSymSize(10.0);
        r2.setPsymConnector( PsymConnector.NONE );

        p.addRenderer(r2);

        r2.setDataSet(vds);

        p.setPreviewEnabled(true);
        p.getXAxis().setAnimated(false);
        p.getYAxis().setAnimated(false);


    }

    public static void doit2() {
        int size= 20000;
        double dsize = (double) size;

        System.err.println("enter getDataSet");
        long t0 = System.currentTimeMillis();

        Random random = new Random(0);

        VectorDataSetBuilder vbd = new VectorDataSetBuilder(Units.dimensionless, Units.dimensionless);
        vbd.addPlane( "color", Units.dimensionless );
        
        double y = 0;
        for (int i = 0; i < size; i += 1) {
            y += random.nextDouble() - 0.5;
            if (i % 100 == 10) {
                vbd.insertY( i / dsize, Units.dimensionless.getFillDouble() );
                vbd.insertY( i/ dsize , 0, "color" );
            } else {
                vbd.insertY( i / dsize, y );
                vbd.insertY( i / dsize, (i-10)/100, "color" );
            }
        }
        
        vbd.setProperty(DataSet.PROPERTY_X_MONOTONIC, Boolean.TRUE);
        VectorDataSet vds = vbd.toVectorDataSet();
        System.err.println("done getDataSet in " + (System.currentTimeMillis() - t0) + " ms");

        DasPlot p = GraphUtil.visualize(vds);
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


    }

    public static void main(String[] args) {
        //doit();
        doit2();
    }
}
