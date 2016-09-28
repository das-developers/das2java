/*
 * Main.java
 *
 * Created on June 28, 2007, 9:34 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package graph;

import org.das2.dataset.DataSet;
import org.das2.dataset.VectorDataSet;
import org.das2.dataset.VectorDataSetBuilder;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasPlot;
import org.das2.graph.GraphUtil;
import org.das2.graph.SeriesRenderer;
import org.das2.util.monitor.ProgressMonitor;
import java.awt.Graphics;
import javax.swing.JFrame;

/**
 *
 * @author jbf
 */
public class SymbolLineSpeed {
    
    /** Creates a new instance of Main */
    public SymbolLineSpeed() {
    }
    
    private VectorDataSet getDataSet( ProgressMonitor mon ) {
        System.err.println("enter getDataSet");
        long t0= System.currentTimeMillis();
        
        VectorDataSetBuilder vbd= new VectorDataSetBuilder( Units.dimensionless, Units.dimensionless );
        double y= 0;
        for ( int i=0; i<1000000; i+=4 ) {
            y+= Math.random() - 0.5;
            if ( i%100==10 ) y= Units.dimensionless.getFillDouble();
            vbd.insertY( i, y );
        }
        vbd.setProperty( DataSet.PROPERTY_X_MONOTONIC, Boolean.TRUE );
        VectorDataSet vds= vbd.toVectorDataSet();
        System.err.println("done getDataSet in "+(System.currentTimeMillis() - t0 ) + " ms" );
        return vds;
    }
    
    private DasPlot getPlot() {
        JFrame frame= new JFrame() ;
        DasCanvas canvas= new DasCanvas( 400, 400 );
        DasPlot p= GraphUtil.newDasPlot( canvas, DatumRange.newDatumRange( 0, 100000, Units.dimensionless ), DatumRange.newDatumRange( -250, 250, Units.dimensionless ) );
        frame.getContentPane().add( canvas );
        frame.pack();
        frame.setVisible( true );
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        
        return p;
    }
    
    public void run() {
        
        DasPlot plot= getPlot();
        plot.getXAxis().setPlot(plot);
        plot.getYAxis().setPlot(plot);
        
        //SymbolLineRenderer rend1= new SymbolLineRenderer();
        SeriesRenderer rend1= new SeriesRenderer();
        
        plot.getXAxis().setDataMaximum( Units.dimensionless.createDatum( 1000010 ) );
        plot.getYAxis().setDatumRange( DatumRange.newDatumRange( -250, 250, Units.dimensionless ) );
        plot.addRenderer( rend1 );
        
        VectorDataSet ds= getDataSet( ProgressMonitor.NULL);
        
        rend1.setDataSet( ds );
        
        for ( int i=0; i<40; i++ ) {
            long t0= System.currentTimeMillis();
            Graphics g= plot.getGraphics();
            g.translate( -plot.getX(), -plot.getY() );
            rend1.updatePlotImage( plot.getXAxis(), plot.getYAxis(), ProgressMonitor.NULL );
            plot.repaint();
            System.err.println( ""+i+"time to update\t"+(System.currentTimeMillis()-t0) );
        }
        
        for ( int i=0; i<40; i++ ) {
            long t0= System.currentTimeMillis();
            Graphics g= plot.getGraphics();
            g.translate( -plot.getX(), -plot.getY() );
            rend1.render( g, plot.getXAxis(), plot.getYAxis(), ProgressMonitor.NULL );
            plot.repaint();
            System.err.println( ""+i+"time to render\t"+(System.currentTimeMillis()-t0) );
        }
        
    }
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new SymbolLineSpeed().run();
    }
    
}
