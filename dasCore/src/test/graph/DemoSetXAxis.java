/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.graph;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasPlot;
import org.das2.graph.GraphUtil;

/**
 *
 * @author jbf
 */
public class DemoSetXAxis {
    public static void main( String[] args ) {
        
        JFrame frame= new JFrame();
        
        int width = 500;
        int height = 100;

        frame.getContentPane().setLayout(new BorderLayout());

        DasCanvas canvas = new DasCanvas(width, height);

        frame.getContentPane().add(canvas, BorderLayout.CENTER );

        DatumRange xrange= DatumRange.newDatumRange(0,10,Units.seconds);
        //DatumRange xrange= DatumRangeUtil.parseTimeRangeValid("2009");
        DatumRange yrange= DatumRange.newDatumRange(0.1,100, Units.dimensionless);

        DasPlot plot= GraphUtil.newDasPlot(canvas, xrange, yrange);

        //plot.getXAxis().setMajorTicksDomainDivider(DomainDividerUtil.getDomainDivider( xrange.min(), xrange.max() ) );
        //plot.getXAxis().setMajorTicksDomainDivider( DomainDividerUtil.getDomainDivider( xrange.min(), xrange.max() ) );
        plot.getXAxis().setUseDomainDivider(true);
        plot.getYAxis().setUseDomainDivider(true);

        // Note null results in NullPointerException
        //plot.setXAxis(null);
        
        frame.pack();
        frame.setVisible(true);
    }
}
