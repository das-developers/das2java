/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.graph;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.graph.DasAnnotation;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasColumn;
import org.das2.graph.DasPlot;
import org.das2.graph.DasRow;
import org.das2.graph.GraphUtil;

/**
 *
 * @author jbf
 */
public class GrannyTextDemo {
        JPanel contentPane;
    protected DasPlot plot;

    private synchronized JPanel getContentPane() {
        if (contentPane == null) {
            contentPane = new JPanel();
        }
        return contentPane;
    }

    public JFrame showFrame() {
        JFrame frame= new JFrame( "Granny Text Demo");
        frame.getContentPane().add(getContentPane());
        frame.pack();
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        frame.setVisible(true);
        return frame;
    }

    public GrannyTextDemo() {
        int width = 500;
        int height = 100;

        getContentPane().setLayout(new BorderLayout());

        DasCanvas canvas = new DasCanvas(width, height);

        getContentPane().add(canvas, BorderLayout.CENTER );

        DasRow row= DasRow.create( canvas, null, "20%", "80%" );
        DasColumn col= DasColumn.create( canvas, null, "20%", "80%" );
        DasAnnotation anno= new DasAnnotation("!c!c!(underline)HELLO!(ununderline) There!");
        anno.setFontSize(20.f);
        canvas.add( anno, row, col );
        canvas.revalidate();
        

    }

    public static void main( String[] args ) {
        new GrannyTextDemo().showFrame();
    }

}
