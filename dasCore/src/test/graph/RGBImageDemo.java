/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test.graph;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.das2.graph.RGBImageRenderer;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasPlot;
import org.das2.graph.GraphUtil;
import org.das2.graph.Renderer;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.MutablePropertyDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SDataSet;
import org.virbo.dsops.Ops;

/**
 *
 * @author jbf
 */
public class RGBImageDemo extends PlotDemo {
    JPanel contentPane;

    private synchronized JPanel getContentPane() {
        if (contentPane == null) {
            contentPane = new JPanel();
        }
        return contentPane;
    }

    public void showFrame() {
        JFrame frame= new JFrame( "Axis Demo");
        frame.getContentPane().add(getContentPane());
        frame.pack();
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        frame.setVisible(true);
    }

    public RGBImageDemo() {
        int width = 500;
        int height = 500;

        getContentPane().setLayout(new BorderLayout());

        DasCanvas canvas = new DasCanvas(width, height);

        getContentPane().add(canvas, BorderLayout.CENTER );

        DatumRange xrange= DatumRange.newDatumRange(-100,400,Units.dimensionless);
        DatumRange yrange= DatumRange.newDatumRange(-100,400,Units.dimensionless);

        DasPlot plot= GraphUtil.newDasPlot(canvas, xrange, yrange);

        plot.getXAxis().setUseDomainDivider(true);
        plot.getYAxis().setUseDomainDivider(true);

        Renderer r= new RGBImageRenderer();

        QDataSet red= Ops.multiply( DataSetUtil.asDataSet(255), Ops.rand(200,200) );
        QDataSet green= Ops.multiply( DataSetUtil.asDataSet(0), Ops.rand(200,200) );
        QDataSet blue= Ops.multiply( DataSetUtil.asDataSet(0), Ops.rand(200,200) );

        SDataSet rgb= SDataSet.createRank3( 200,200,3 );
        for ( int i=0; i<rgb.length(); i++ ) {
            for ( int j=0; j<rgb.length(0); j++ ) {
                rgb.putValue( i, j, 0, 255 );
                rgb.putValue( i, j, 1, i%3==0 ? 255 : 0 );
                rgb.putValue( i, j, 2, j%5==0 ? 255 : 0 );
            }
        }
        rgb.putProperty( QDataSet.DEPEND_2, Ops.labels( new String[] { "b", "g", "r" } ) );
        r.setDataSet( rgb );
        //r.setDataSet( Ops.multiply( DataSetUtil.asDataSet(255), Ops.rand(300,300) ) );

        plot.addRenderer( r );
        
    }

    public static void main( String[] args ) {
        new RGBImageDemo().showFrame();
    }
}
