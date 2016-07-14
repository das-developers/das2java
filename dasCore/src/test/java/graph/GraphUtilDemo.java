/*
 * GraphUtilDemo.java
 *
 * Created on December 8, 2007, 7:17 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package test.graph;

import org.das2.graph.GraphUtil;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author jbf
 */
public class GraphUtilDemo {
        
    public static void pointsAlongACurveDemo() {
        Ellipse2D e= new Ellipse2D.Double(50,50,100,100);
        final GeneralPath p= new GeneralPath(e);
        
        //final GeneralPath p= new GeneralPath( new Rectangle( 20, 20, 100, 100 ) );
        
        
        double[] points= new double[] { 10, 20, 30, 100, 200 };
        final Point2D.Double[] result= new Point2D.Double[points.length];
        final double[] orientation= new double[points.length];
        
        double len= GraphUtil.pointsAlongCurve( p.getPathIterator(null,0.001), null, null, null, false );
        points[points.length-1]=len;
        GraphUtil.pointsAlongCurve( p.getPathIterator(null,0.001), points, result, orientation, false );
        
        JPanel panel= new JPanel() {
            public void paintComponent( Graphics g1 ) {
                Graphics2D g= (Graphics2D) g1;
                g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
                ((Graphics2D)g).draw(p);
                for ( int i=0; i<result.length; i++ ) {
                    if ( result[i]==null ) continue;
                    AffineTransform at= new AffineTransform();
                    at.translate( result[i].x, result[i].y );
                    at.rotate( orientation[i] );
                    g.setTransform( at );
                    double w= g.getFontMetrics().getStringBounds("A",g).getWidth();
                    g.drawString("A",(int)(-w/2),0);
                }
            }
        };
        
        panel.setMinimumSize( new Dimension(300,300) );
        panel.setPreferredSize( new Dimension(300,300) );
        JFrame frame= new JFrame( );
        frame.setContentPane(panel);
        
        frame.pack();
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        frame.setVisible(true);
        
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        pointsAlongACurveDemo();
    }
    
}
