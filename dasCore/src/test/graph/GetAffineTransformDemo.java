/*
 * GetAffineTransformDemo.java
 *
 * Created on Jul 12, 2007, 4:19:18 PM
 *
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test.graph;

import edu.uiowa.physics.pw.das.DasApplication;
import edu.uiowa.physics.pw.das.datum.DatumRange;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.graph.DasAxis;
import edu.uiowa.physics.pw.das.graph.DasCanvas;
import edu.uiowa.physics.pw.das.graph.DasCanvasComponent;
import edu.uiowa.physics.pw.das.graph.DasColumn;
import edu.uiowa.physics.pw.das.graph.DasRow;
import edu.uiowa.physics.pw.das.graph.GraphUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 *
 * @author jbf
 */
public class GetAffineTransformDemo {
    
    DasAxis.Memento mem;
    DasAxis xaxis;
    DasCanvasComponent component;
    int x0;
    
    class MyCanvasComponent extends DasCanvasComponent {
        public void paintComponent( Graphics g1 ) {
            Graphics2D g= (Graphics2D) g1.create();
            g.translate( -1 * ( getX() - x0), 0 );
            AffineTransform at= xaxis.getAffineTransform( mem, new AffineTransform() );
//g.translate( x0,0 );
            //g.scale( at.getScaleX(), 1.0 );
            g.transform( at );
            
            //g.translate
            System.err.printf( "%d %d %d\n", x0, getX() ,getX() - x0 );
            g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
            g.setFont( Font.decode("sans-28") );
            g.setColor( new Color( 184, 226, 255 ) );
            g.fillRect( 0, 0, getWidth()-1, getHeight()-1 );
            g.setColor( Color.LIGHT_GRAY );
            g.drawString("Hello", 0, getHeight() );
            
            getDasMouseInputAdapter().paint(g1);
        }
        
    }
    
    void run() {
        PropertyChangeListener listener= new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                System.err.println( "propertyChange: " + evt.getPropertyName() + ": " +GraphUtil.getATScaleTranslateString( xaxis.getAffineTransform( mem, new AffineTransform() ) ) );
                //System.err.println( "         : " +GraphUtil.getATScaleTranslateString( xaxis.getATNew( mem, new AffineTransform() ) ) );
                component.repaint();
            }
        };
        
        component= new MyCanvasComponent();
        
        DasCanvas canvas= new DasCanvas( );
        xaxis= new DasAxis(  DatumRange.newDatumRange( -10, 10, Units.dimensionless ), DasAxis.HORIZONTAL );
        
        int emMin0= 10;
        
        DasRow row= new DasRow( canvas, null, 0, 1, 10, -10, 0, 0 );
        DasColumn col= new DasColumn( canvas, null, 0, 1, emMin0, -10, 0, 0 );
        canvas.add( xaxis, row, col );
        
        canvas.add( component, row, col );
        
        DasApplication.getDefaultApplication().createMainFrame("AffineTransformDemo", canvas );
        
        mem= xaxis.getMemento();
        x0= col.getDMinimum();
        
        col.setEmMinimum( 0 );
        col.setEmMaximum( -20 );
        
        System.err.println( "translate -80: " +GraphUtil.getATScaleTranslateString( xaxis.getAffineTransform( mem, new AffineTransform() ) ) );
        
        col.setEmMinimum( 20 );
        col.setEmMaximum( -0 );
        
        System.err.println( "translate +80: " +GraphUtil.getATScaleTranslateString( xaxis.getAffineTransform( mem, new AffineTransform() ) ) );
        
        col.setEmMinimum( 10 );
        col.setEmMaximum( -20 );
        
        System.err.println( "same min: " +GraphUtil.getATScaleTranslateString( xaxis.getAffineTransform( mem, new AffineTransform() ) ) );
        
        col.setEmMinimum( 20 );
        col.setEmMaximum( -10 );
        
        System.err.println( "same max: " +GraphUtil.getATScaleTranslateString( xaxis.getAffineTransform( mem, new AffineTransform() ) ) );
        
        col.setEmMinimum( 20 );
        col.setEmMaximum( -20 );
        
        System.err.println( "purely scale: " +GraphUtil.getATScaleTranslateString( xaxis.getAffineTransform( mem, new AffineTransform() ) ) );
        
        col.setEmMinimum( emMin0 );
        col.setEmMaximum( -10 );
        
        System.err.println( "identity: " +GraphUtil.getATScaleTranslateString( xaxis.getAffineTransform( mem, new AffineTransform() ) ) );
        
        xaxis.addPropertyChangeListener( listener );
        col.addPropertyChangeListener( listener );
    }
    
    public static void main( String[] args ) {
        new GetAffineTransformDemo().run();
    }
}
