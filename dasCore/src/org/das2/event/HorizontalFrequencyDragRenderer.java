/*
 * HorizontalFrequencyDragRenderer.java
 *
 * Created on February 17, 2004, 6:06 PM
 */

package org.das2.event;

import org.das2.graph.DasCanvasComponent;
import org.das2.graph.DasAxis;
import org.das2.datum.Datum;
import java.awt.*;
import java.awt.event.*;
import java.text.*;

/**
 *
 * @author  Jeremy
 */
public class HorizontalFrequencyDragRenderer implements DragRenderer, KeyListener {
    
    private Rectangle dirtyBounds;
    DasCanvasComponent parent;
    
    DasAxis axis;
    int ncycles;
    
    /** Creates a new instance of HorizontalFrequencyDragRenderer */
    public HorizontalFrequencyDragRenderer( DasCanvasComponent parent, DasAxis axis ) {
        this.parent= parent;
        parent.addKeyListener(this);
        this.axis= axis;
        this.dirtyBounds= new Rectangle();
        ncycles=1;
    }
    
    public void renderLabel( java.awt.Graphics g1, java.awt.Point p1, java.awt.Point p2, String report ) {
        int dxMax= Integer.MIN_VALUE;
        
        Graphics2D g= ( Graphics2D ) g1;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        
        FontMetrics fm= parent.getGraphics().getFontMetrics();
        
        Color color0= g.getColor();
        g.setColor(new Color(255,255,255,200));
        
        Dimension d= parent.getSize();
        
        int dx= fm.stringWidth(report)+6;
        if (dxMax<dx) dxMax=dx;
        int dy= fm.getAscent()+fm.getDescent();
        int xp= p2.x+3;
        int yp= p2.y-3-dy;
        
        if ( (xp+dxMax>d.width-3) && (p2.x-3-dx>0) ) {
            xp= p2.x-3-dx;
        }
        
        if (yp<13) {
            yp= p2.y+3;
        }
        
        Rectangle bg= new Rectangle(xp,yp,dx,dy);        
        g.fill(bg);
        
        g.setColor(new Color(20,20,20));
        g.drawString(report,xp+3,yp+fm.getAscent());
        g.setColor(color0);
        
        dirtyBounds.add(bg);
    }
    
    public Rectangle[] renderDrag(java.awt.Graphics g1, java.awt.Point p1, java.awt.Point p2) {
        
        Graphics2D g= (Graphics2D) g1;
        int x2 = p2.x;
        int x1= p1.x;
        if (x2<x1) { int t=x2; x2= x1; x1= t; }
        int y = p2.y;
        
        Color color0= g.getColor();
        g.setColor(new Color(255,255,255,128));
        g.setStroke(new BasicStroke( 3.0f,BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ));
        
        internalRender(g, dirtyBounds, x1, x2, y);
        
        g.setStroke(new BasicStroke());
        g.setColor(color0);
        
        internalRender(g, dirtyBounds, x1, x2, y);
        
        Datum periodDatum= axis.invTransform( x2 ) . subtract( axis.invTransform( x1 ) );
        double period= periodDatum.doubleValue( periodDatum.getUnits() );
        double freq= ncycles / period;
        
        DecimalFormat df= new DecimalFormat("0.00");
        renderLabel(g1, p1, p2, "T:"+df.format(period)+" f:"+df.format(freq) );
        
        return new Rectangle[] { dirtyBounds };
    }
    
    private void internalRender(Graphics2D g, Rectangle dirtyBounds, int x1, int x2, int y) {
        double width = x2 - x1;
        if ( width > 6 )
            g.drawLine(x1+3, y, x2-3, y);
        g.drawLine(x1, y+2, x1, y-2 ); //serifs
        g.drawLine(x2, y+2, x2, y-2 );
        
        dirtyBounds.setRect(x1-2,y-5,4,10);
        
        double rwidth= width / (double) ncycles;
        if ( width>3*ncycles ) {
            
            for ( double ii= x2 + rwidth; ii<axis.getColumn().getWidth(); ii+= rwidth ) {
                g.drawLine( (int)ii, y+2, (int)ii, y-2 );
                dirtyBounds.add((int)ii+2,y-5);
            }
            for ( double ii= x2 - rwidth; ii>0; ii-= rwidth ) {
                g.drawLine( (int)ii, y+2, (int)ii, y-2 );
                dirtyBounds.add((int)ii-2,y-5);
            }
        }
    }
    
    public boolean isPointSelection() {
        return false;
    }
    
    public void clear(java.awt.Graphics g) {
        parent.paintImmediately(dirtyBounds);
    }
    
    public boolean isUpdatingDragSelection() {
        return false;
    }
    
    public MouseDragEvent getMouseDragEvent(Object source, java.awt.Point p1, java.awt.Point p2, boolean isModified) {
        return null;
    }
    
    public void keyPressed(KeyEvent e) {
        if ( e.getKeyChar()=='1' ) {
            ncycles= 1;
        } else if ( e.getKeyChar()=='2' ) {
            ncycles= 2;
        } else if ( e.getKeyChar()=='3' ) {
            ncycles= 3;
        }
    }
    
    public void keyReleased(KeyEvent e) {
    }
    
    public void keyTyped(KeyEvent e) {
    }
    
}
