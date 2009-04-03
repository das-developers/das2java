/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test.graph;

import org.das2.dataset.DataSet;
import org.das2.dataset.VectorDataSetBuilder;
import org.das2.datum.Units;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasColumn;
import org.das2.graph.DasPlot;
import org.das2.graph.DasRow;
import org.das2.graph.SeriesRenderer;
import java.util.Random;
import javax.swing.JApplet;
import javax.swing.JFrame;

/**
 *
 * @author jbf
 */
public class GraphLegendDemo extends JApplet {

    /**
     * Initialization method that will be called after the applet is loaded
     * into the browser.
     */
    public void init() {
        DasCanvas c = new DasCanvas();
        add(c);

        DasPlot p = DasPlot.createDummyPlot();
        c.add(p, DasRow.create(c), DasColumn.create(c));

        SeriesRenderer rend = new SeriesRenderer();
        rend.setDataSet(getFunDataSet());

        p.addRenderer(rend);

    }

    private DataSet getFunDataSet() {
        VectorDataSetBuilder build = new VectorDataSetBuilder(Units.dimensionless, Units.dimensionless);
        double dx = 0;
        double dy = 0;
        Random r = new Random(0);
        for (int i = 0; i < 1000; i++) {
            dx += r.nextGaussian();
            dy += r.nextGaussian();
            build.insertY(dx + r.nextGaussian(), dy + r.nextGaussian());
        }
        return build.toVectorDataSet();

    }    // TODO overwrite start(), stop() and destroy() methods
    
    public static void main( String[] args ) {
        GraphLegendDemo demo= new GraphLegendDemo();
        demo.init();
        JFrame frame= new JFrame();
        frame.getContentPane().add(demo);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        
    }
}
