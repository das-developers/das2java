/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test.graph;

import javax.swing.JFrame;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.graph.DasColorBar;
import org.das2.graph.SpectrogramRenderer;
import org.das2.qds.ops.Ops;

/**
 *
 * @author jbf
 */
public class SpectrogramRendererDemo extends PlotDemo {
    SpectrogramRendererDemo() {
        super();

        DasColorBar cb= new DasColorBar( Datum.create(0), Datum.create(1), false );
        SpectrogramRenderer rend= new SpectrogramRenderer(null,cb);
        //rend.setRebinner( SpectrogramRenderer.RebinnerEnum.lanlNN );
        rend.setDataSet( Ops.ripples(20,20) );

        plot.addRenderer( rend );
        plot.getColumn().setEmMaximum(-10);
        plot.getXAxis().setDatumRange( DatumRange.newDatumRange(0,20,Units.dimensionless) );

    }

    public static void main( String[] args ) {
        JFrame frame= new SpectrogramRendererDemo().showFrame();
        frame.setSize( 800,800 );
    }
}
