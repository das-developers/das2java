/*
 * Arrow.java
 *
 * Created on September 28, 2004, 2:13 PM
 */

package org.das2.graph;

import java.awt.*;
import java.awt.geom.*;

/**
 *
 * @author  Jeremy
 */
public class Arrow extends DasCanvasComponent {
    
    Point head, tail;
    Stroke stroke;
    double em=24; //pixels;
    
    public enum HeadStyle {
        DRAFTING, FAT_TRIANGLE, THIN_TRIANGLE,
    }
    
    public Arrow( DasCanvas c, Point head, Point tail ) {
        this.head= head;
        this.tail= tail;
        setRow( new DasRow( c, head.getX(), tail.getX() ) );
        setColumn( new DasColumn( c, head.getY(), tail.getY() ) );
    }
    
    public void resize() {
        Rectangle bounds= new Rectangle();
        bounds.add( head.x - em, head.y-em ); // account for stroke width
        bounds.add( tail.x + em, tail.y+em );
        setBounds(bounds);
    }
    
    public static void paintArrow( Graphics2D g, Point head, Point tail, double em, HeadStyle style ) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        
        Line2D line= new Line2D.Double( head, tail );
        double dx= - ( head.getX() - tail.getX() );
        double dy= - ( head.getY() - tail.getY() );
        double dd= Math.sqrt( dx*dx + dy*dy );
        dx= dx * em / 4 / dd;
        dy= dy * em / 4 / dd;
        
        double hx= head.getX();
        double hy= head.getY();
        
        g.setStroke( new BasicStroke( (float)(em/8), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ) );
        
        g.draw( line );
        
        GeneralPath p= new GeneralPath();
        p.moveTo( (float)hx, (float)hy );
        
        if ( style==HeadStyle.DRAFTING ) {
            p.lineTo( (float)(hx+2*dx+0.5*dy), (float)(hy+2*dy-0.5*dx) );
            p.lineTo( (float)(hx+3*dx+dy), (float)(hy+3*dy-dx) );
            p.lineTo( (float)(hx+3*dx-dy), (float)(hy+3*dy+dx) );
            p.lineTo( (float)(hx+2*dx-0.5*dy), (float)(hy+2*dy+0.5*dx) );
            p.lineTo( (float)hx, (float)hy );
        } else if ( style==HeadStyle.FAT_TRIANGLE ) {
            p.lineTo( (float)(hx+3*dx+1.5*dy), (float)(hy+3*dy-1.5*dx) );
            p.lineTo( (float)(hx+3*dx-1.5*dy), (float)(hy+3*dy+1.5*dx) );
            p.lineTo( (float)hx, (float)hy );            
        } else if ( style==HeadStyle.THIN_TRIANGLE ) {
            p.lineTo( (float)(hx+3*dx+dy), (float)(hy+3*dy-dx) );
            p.lineTo( (float)(hx+3*dx-dy), (float)(hy+3*dy+dx) );
            p.lineTo( (float)hx, (float)hy );            
        }
        
        g.fill( p );
        
        g.draw( p );
        
    }
    
    protected void paintComponent(Graphics g1) {
        Graphics2D g= (Graphics2D) g1.create();
        g.translate(-getX(),-getY());
        paintArrow( g, head, tail, em , HeadStyle.DRAFTING );
        getDasMouseInputAdapter().paint(g1);
        g.dispose();
    }
    
    
}
