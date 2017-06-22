/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test.graph;

import javax.swing.JFrame;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.graph.RGBImageRenderer;
import org.das2.graph.Renderer;
import org.das2.qds.QDataSet;
import org.das2.qds.SDataSet;
import org.das2.qds.ops.Ops;

/**
 *
 * @author jbf
 */
public class RGBImageDemo extends PlotDemo {

    public RGBImageDemo() {
        super();

        Renderer r= new RGBImageRenderer();

        SDataSet rgb= SDataSet.createRank3( 200,200,3 );
        for ( int i=0; i<rgb.length(); i++ ) {
            for ( int j=0; j<rgb.length(0); j++ ) {
                rgb.putValue( i, j, 0, 255 );
                rgb.putValue( i, j, 1, i%3==0 ? 255 : 0 );
                rgb.putValue( i, j, 2, j%5==0 ? 255 : 0 );
            }
        }
        rgb.putProperty( QDataSet.DEPEND_2, Ops.labelsDataset(new String[] { "b", "g", "r" } ) );
        r.setDataSet( rgb );
        //r.setDataSet( Ops.multiply( DataSetUtil.asDataSet(255), Ops.rand(300,300) ) );
        plot.getXAxis().setDatumRange( DatumRange.newDatumRange(0,20,Units.dimensionless) );
        plot.addRenderer( r );
        
    }

    public static void main( String[] args ) {
        JFrame frame= new RGBImageDemo().showFrame();
        frame.setSize( 800,800 );
    }
}
