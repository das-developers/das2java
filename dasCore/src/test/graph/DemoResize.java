/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test.graph;

import javax.swing.JFrame;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasColumn;
import org.das2.graph.DasPlot;
import org.das2.graph.DasRow;
import org.das2.graph.SeriesRenderer;

/**
 *
 * @author jbf
 */
public class DemoResize {
    public static void main(String[] args ) throws InterruptedException {
        
        DasCanvas c = new DasCanvas();

        DasPlot p = DasPlot.createDummyPlot();
        p.setDrawMinorGrid(true);
        c.add(p, DasRow.create(c), DasColumn.create(c));

        SeriesRenderer rend = new SeriesRenderer();
        p.addRenderer(rend);
        
        JFrame frame= new JFrame();
        frame.getContentPane().add(c);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        
        p.getRow().setEmMinimum(12);
        p.getRow().setEmMinimum(10);
        

        Thread.sleep(1000);
        p.getRow().setEmMinimum(5.5);
    }
}
