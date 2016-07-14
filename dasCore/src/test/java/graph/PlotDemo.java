/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test.graph;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasPlot;
import org.das2.graph.GraphUtil;

/**
 *
 * @author jbf
 */
public class PlotDemo {

    JPanel contentPane;
    protected DasPlot plot;

    private synchronized JPanel getContentPane() {
        if (contentPane == null) {
            contentPane = new JPanel();
        }
        return contentPane;
    }

    public JFrame showFrame() {
        JFrame frame= new JFrame( "Axis Demo");
        frame.getContentPane().add(getContentPane());
        frame.pack();
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        frame.setVisible(true);
        return frame;
    }

    public PlotDemo() {
        int width = 500;
        int height = 100;

        getContentPane().setLayout(new BorderLayout());

        DasCanvas canvas = new DasCanvas(width, height);

        getContentPane().add(canvas, BorderLayout.CENTER );

        DatumRange xrange= DatumRange.newDatumRange(0,10,Units.seconds);
        //DatumRange xrange= DatumRangeUtil.parseTimeRangeValid("2009");
        DatumRange yrange= DatumRange.newDatumRange(0.1,100, Units.dimensionless);

        plot= GraphUtil.newDasPlot(canvas, xrange, yrange);

        //plot.getXAxis().setMajorTicksDomainDivider(DomainDividerUtil.getDomainDivider( xrange.min(), xrange.max() ) );
        //plot.getXAxis().setMajorTicksDomainDivider( DomainDividerUtil.getDomainDivider( xrange.min(), xrange.max() ) );
        //plot.getXAxis().setUseDomainDivider(true);
        //plot.getYAxis().setUseDomainDivider(true);
    }

    public static void main( String[] args ) {
        new PlotDemo().showFrame();
    }
}
