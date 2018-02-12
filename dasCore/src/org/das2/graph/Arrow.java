/*
 * Arrow.java
 *
 * Created on September 28, 2004, 2:13 PM
 */

package org.das2.graph;

import java.awt.*;
import java.awt.geom.*;

/**
 * An arrow component that lives on the canvas, and utility methods
 * for drawing arrows.
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
        Rectangle bounds= new Rectangle( (int)(head.x-em), (int)(head.y-em), 0, 0 );
        bounds.add( head.x - em, head.y-em ); // account for stroke width
        bounds.add( tail.x + em, tail.y+em );
        setBounds(bounds);
    }

    /**
     * paint the arrow with the given thickness and head style.
     * @param g the graphics context.
     * @param head the point locating the head of the arrow
     * @param tail the point locating the tail of the arrow
     * @param headSize the size of the arrow head.
     * @param style HeadStyle.DRAFTING, etc.
     */    
    public static void paintArrow( Graphics2D g, Point head, Point tail, double headSize, HeadStyle style ) {
        paintArrow( g, new Point2D.Double( head.getX(), head.getY() ), new Point2D.Double( tail.getX(), tail.getY() ), headSize, style );
    }

    /**
     * paint the arrow with the given thickness and head style.
     * @param g the graphics context.
     * @param head the point locating the head of the arrow
     * @param tail the point locating the tail of the arrow
     * @param headSize the size of the arrow head in pixels
     * @param style HeadStyle.DRAFTING, etc.
     */
    public static void paintArrow( Graphics2D g, Point2D head, Point2D tail, double headSize, HeadStyle style ) {
        
        double pt= headSize;
                
        g= (Graphics2D) g.create();
        
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        
        float s0= 1.0f;  // the initial stroke width.
        Stroke s= g.getStroke();
        if ( s instanceof BasicStroke ) {
            BasicStroke bs= (BasicStroke)s;
            s0= bs.getLineWidth();
        }
        
        Line2D line= new Line2D.Double( head, tail );
        double dx= - ( head.getX() - tail.getX() );
        double dy= - ( head.getY() - tail.getY() );
        double dd= Math.sqrt( dx*dx + dy*dy );
        
        if ( dd==0 ) return;
        
        dx= dx * pt / 4 / dd;
        dy= dy * pt / 4 / dd;
        
        double hx= head.getX();
        double hy= head.getY();
        
        g.setStroke( new BasicStroke( (float)(pt/8)*s0, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ) );
        
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
    
    @Override
    protected void paintComponent(Graphics g1) {
        Graphics2D g= (Graphics2D) g1.create();
        g.translate(-getX(),-getY());
        paintArrow( g, head, tail, em , HeadStyle.DRAFTING );
        getDasMouseInputAdapter().paint(g1);
        g.dispose();
    }
    
    
}
