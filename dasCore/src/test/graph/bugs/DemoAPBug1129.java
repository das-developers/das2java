/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test.graph.bugs;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.Units;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasColorBar;
import org.das2.graph.DasPlot;
import org.das2.graph.GraphUtil;
import org.das2.graph.SpectrogramRenderer;
import org.virbo.dataset.ArrayDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dsops.Ops;
import test.graph.PlotDemo;

/**
 *
 * @author jbf
 */
public class DemoAPBug1129 {
    JPanel contentPane;
    protected DasPlot plot;
    private DasColorBar cb;

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

    public DemoAPBug1129() {
        int width = 500;
        int height = 500;

        getContentPane().setLayout(new BorderLayout());

        DasCanvas canvas = new DasCanvas(width, height);

        getContentPane().add(canvas, BorderLayout.CENTER );

        DatumRange xrange= DatumRange.newDatumRange(-5,35,Units.dimensionless);
        DatumRange yrange= DatumRange.newDatumRange(-5,35, Units.dimensionless);
        DatumRange zrange= DatumRange.newDatumRange(-0.2,0.2,Units.dimensionless);
        
        cb= new DasColorBar( zrange.min(), zrange.max(), false );
        plot= GraphUtil.newDasPlot(canvas, xrange, yrange);
        
        canvas.add( cb, plot.getRow(), plot.getColumn().createAttachedColumn(1.05,1.10) );
        plot.addRenderer( new SpectrogramRenderer(null,cb) );
        
        plot.getColumn().setEmMaximum(-10);
        
    }
    
    private void stressIt() {
        ArrayDataSet ds= ArrayDataSet.copy( Ops.ripples(30,30) );
        plot.getRenderer(0).setDataSet(ds);
        
        DatumRange xr1= DatumRangeUtil.rescale(plot.getXAxis().getDatumRange(), -0.01, 0.99 );
        DatumRange xr2= DatumRangeUtil.rescale(plot.getXAxis().getDatumRange(), 0.01, 1.01 );
        
        int j=0;
        while ( true ) {
            plot.getXAxis().setDatumRange( xr1 );
            plot.getXAxis().setDatumRange( xr2 );
            ds.putValue( j % 30, ( j % 900) /30 , Math.random() );
            plot.getRenderer(0).setDataSet(ds);
            j=j+1;
            if ( ( j % 900) ==0  ) {
                ds= ArrayDataSet.copy( Ops.ripples(30,30) );
            }
        }
    }

    public static void main( String[] args ) {
        DemoAPBug1129 app= new DemoAPBug1129();
        
        app.showFrame();
        app.stressIt();
    }    
}
