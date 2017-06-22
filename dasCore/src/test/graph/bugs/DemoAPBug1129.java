/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test.graph.bugs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.Units;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasColorBar;
import org.das2.graph.DasPlot;
import org.das2.graph.GraphUtil;
import org.das2.graph.SeriesRenderer;
import org.das2.graph.SpectrogramRenderer;
import org.das2.graph.util.GraphicalLogHandler;
import org.das2.util.FileUtil;
import org.das2.util.LoggerManager;
import org.das2.qds.ArrayDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.ops.Ops;
import test.graph.PlotDemo;

/**
 *
 * @author jbf
 */
public class DemoAPBug1129 {
    JPanel contentPane;
    private DasCanvas canvas;
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

    private class MyHandler extends Handler {

        @Override
        public void publish(LogRecord rec) {
            Object[] parms= rec.getParameters();

            String recMsg;
            if ( parms==null || parms.length==0 ) {
                recMsg = rec.getMessage();
            } else {
                recMsg = MessageFormat.format( rec.getMessage(), parms );
            }
            System.err.println( recMsg );
        }

        @Override
        public void flush() {
            
        }

        @Override
        public void close() throws SecurityException {
            
        }
        
    }
    
    
    public DemoAPBug1129() {
        int width = 400;
        int height = 200;

        LoggerManager.getLogger("das2.graphics").setLevel(Level.FINER);
        Handler h= new MyHandler();
        h.setLevel( Level.ALL );
        LoggerManager.getLogger("das2.graphics").addHandler( h );
        
        getContentPane().setLayout(new BorderLayout());

        canvas = new DasCanvas(width, height);

        getContentPane().add(canvas, BorderLayout.CENTER );

        DatumRange xrange= DatumRange.newDatumRange(-5,35,Units.dimensionless);
        DatumRange yrange= DatumRange.newDatumRange(-5,35, Units.dimensionless);
        DatumRange zrange= DatumRange.newDatumRange(-0.2,1.2,Units.dimensionless);
        
        cb= new DasColorBar( zrange.min(), zrange.max(), false );
        plot= GraphUtil.newDasPlot(canvas, xrange, yrange);
        
        canvas.add( cb, plot.getRow(), plot.getColumn().createAttachedColumn(1.05,1.10) );
        plot.setPreviewEnabled(true);
        
        plot.addRenderer( new SpectrogramRenderer(null,cb) );
        SeriesRenderer r= new SeriesRenderer();
        r.setAntiAliased(true);
        r.setColor( Color.WHITE );
        
        plot.addRenderer( r );
        
        plot.getColumn().setEmMaximum(-10);
        
    }
    
    private void writeImage( int j, char a ) {
        try {
            canvas.waitUntilIdle(true);
            canvas.writeToPng( String.format("/tmp/ap/%05d%s.png",j,a) );
            // canvas.waitUntilIdle(true); When the fault occurs, there are no pending events.
        } catch (IOException ex) {
            Logger.getLogger(DemoAPBug1129.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(DemoAPBug1129.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private ArrayDataSet getDataSet() {
        ArrayDataSet ds= ArrayDataSet.copy( Ops.replicate(0.5,30,30) );
        for ( int i=0;i<30;i++ ) {
            ds.putValue(i,i,0);
        }
        return ds;
    }
    
    private void initalize() {
        //ArrayDataSet ds= ArrayDataSet.copy( Ops.ripples(30,30) );
        ArrayDataSet ds= getDataSet();
        plot.getRenderer(0).setDataSet(ds);
        plot.getRenderer(1).setDataSet(Ops.link(new double[] { 3,15 }, new double[] {3,15} ) );
    }
    
    private void stressIt() {
        ArrayDataSet ds= getDataSet();
        
        DatumRange xr1= DatumRangeUtil.rescale(plot.getXAxis().getDatumRange(), -0.02, 0.98 );
        plot.getColumn().setMinimum(0.1);
        plot.getColumn().setMaximum(0.9);
        DatumRange xr2= DatumRangeUtil.rescale(plot.getXAxis().getDatumRange(), 0.02, 1.02 );
        
        if ( new File("/tmp/ap/").exists() ) {
            if ( !FileUtil.deleteFileTree(new File("/tmp/ap/")) ) {
                throw new RuntimeException("unable to delete old tree");
            }   
        }
        
        if ( !new File("/tmp/ap/").mkdirs() ) {
            throw new RuntimeException("mkdirs");
        }
        
        int j=0;
        while ( true ) {
            plot.getColumn().setMinimum(0.1);
            plot.getColumn().setMaximum(0.9);
            plot.getXAxis().setDatumRange( xr1 );
            writeImage(j,'a');
            if ( canvas.broken!=null ) {
                //break;
            }
            plot.getXAxis().setDatumRange( xr2 );
            //ds.putValue( j % 30, ( j % 900) /30, 0.6+Math.random()/10 );
            plot.getColumn().setMinimum(0.12);
            plot.getColumn().setMaximum(0.92);    
            writeImage(j,'b');
            if ( canvas.broken!=null ) {
                //break;
            }
            //plot.getRenderer(0).setDataSet(ds);
            //writeImage(j,'c');
            j=j+1;
            if ( ( j % 900) ==0  ) {
                ds= getDataSet();
            }
        }
    }

    public static void main( String[] args ) {
        DemoAPBug1129 app= new DemoAPBug1129();
        
        app.showFrame();
        app.initalize();
        app.stressIt();
    }    
}
