/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package test.graph;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Do a simple approximation of what should be occurring when 
 * SeriesRenderer is rendering, to get a baseline for expected 
 * performance.
 * 
 * I believe this shows that the SeriesRender should be much much faster than it is.
 * 
 * @author jbf
 */
public class GeneralPathSpeedTest extends JPanel {

    int n= 1200000;
    int jj= 100;
    GeneralPath  gp;
    double[] data;
    double dmin, dmax;
    int resetCount= 10;
    int rc= resetCount;
    
    private void updateData() {
        data= new double[n];
        dmin= Double.POSITIVE_INFINITY;
        dmax= Double.NEGATIVE_INFINITY;
        double d=0;
        for ( int i=0; i<n; i++ ) {
            d= ( (Math.random()-0.5)*5 ) + d;
            if ( d>dmax ) dmax= d;
            if ( d<dmin ) dmin= d;
            data[i]= d;
        }
    }
    
    private void updatePath() {
        long t0 = System.currentTimeMillis();
        updateData();
        System.err.println( "Time to updateData: " + ( System.currentTimeMillis()-t0) );
        
        t0 = System.currentTimeMillis();
        gp= new GeneralPath( GeneralPath.WIND_EVEN_ODD, n*110/100 );
        gp.moveTo(0, 480 * ( data[0]-dmin ) / ( dmax-dmin ));
        
        for ( int i=0; i<n; i++ ) {
            gp.lineTo( i*640./n, 480 * ( data[i]-dmin ) / ( dmax-dmin ) );
        }
        
         System.err.println( "Time to updatePath: " + ( System.currentTimeMillis()-t0) );
          
    }
    
    

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(640,480);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(640,480);
    }
    
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2= (Graphics2D)g;
        g.clearRect( 0, 0, 640, 480 );
        long t0= System.currentTimeMillis();
        g2.draw( gp );
        System.err.println( System.currentTimeMillis()-t0);
        Timer t= new Timer(1000,new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                repaint();
            }
        });
        t.setRepeats(false);
        t.restart();
        if ( --rc==0 ) {
            updatePath();
            rc= resetCount;
        }
    }
    
    public static void main( String[] ss ) {
        GeneralPathSpeedTest gpst=  new GeneralPathSpeedTest() ;
        long t0 = System.currentTimeMillis();
        gpst.updatePath();
        System.err.println( "Time to updatePath: " + ( System.currentTimeMillis()-t0) );
        JOptionPane.showMessageDialog( null, gpst );
        
    }
    
}
