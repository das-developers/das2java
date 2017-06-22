/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test.graph;

import java.awt.BorderLayout;
import java.text.ParseException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.das2.graph.DasAxis;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasColumn;
import org.das2.graph.DasPlot;
import org.das2.graph.DasRow;
import org.das2.graph.GraphUtil;
import org.das2.graph.Renderer;
import org.das2.graph.SeriesRenderer;
import org.das2.qds.QDataSet;
import org.das2.qds.ops.Ops;

/**
 * relatively simple demo for Larry at APL.
 * @author jbf
 */
public class SimpleDemo2 {
    public static void main( String[] args ) throws ParseException {

        int width = 500;
        int height = 400;

        JPanel panel= new JPanel();

        panel.setLayout(new BorderLayout());

        DasCanvas canvas = new DasCanvas(width, height);
        canvas.setAntiAlias(true);

        panel.add(canvas, BorderLayout.CENTER );

        // read data
        QDataSet yds = Ops.sin( Ops.linspace(0,10,1000) );
        QDataSet tds= Ops.timegen( "2010-01-01T00:00", "1 s", 1000 );

        QDataSet ds= Ops.link( tds, yds );

        // here's some old das2 autoranging, works for this case
        DasAxis xaxis = GraphUtil.guessXAxis(ds);
        DasAxis yaxis = GraphUtil.guessYAxis(ds);

        DasPlot plot = new DasPlot( xaxis, yaxis );

        // here's autoplot as of 2005
        Renderer r= GraphUtil.guessRenderer(ds);
        plot.addRenderer( r );

        // ugh.  I need to make antialiased the default.  Right now it reads the property from $HOME/.dasrc
        if ( r instanceof SeriesRenderer ) {
            ((SeriesRenderer)r).setAntiAliased(true);
        }

        xaxis.setTcaFunction( new QFunctionLarry() );
        xaxis.setDrawTca(true);

        canvas.add( plot, DasRow.create( canvas, null, "0%+2em", "100%-5em" ),
                DasColumn.create( canvas, null, "0%+14em", "100%-4em" ) );

        JFrame frame= new JFrame();
        frame.getContentPane().add( panel );
        frame.pack();

        frame.setVisible(true);
    }

}
