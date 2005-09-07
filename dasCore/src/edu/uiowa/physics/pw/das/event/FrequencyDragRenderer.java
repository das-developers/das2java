/*
 * FrequencyDragRenderer.java
 *
 * Created on September 6, 2005, 4:55 PM
 */

package edu.uiowa.physics.pw.das.event;

import edu.uiowa.physics.pw.das.graph.*;
import java.awt.*;
import java.awt.event.*;
import edu.uiowa.physics.pw.das.datum.*;
import java.text.*;

/**
 *
 * @author  Jeremy
 * @author  eew
 */
public class FrequencyDragRenderer implements DragRenderer, KeyListener {
    
    private Rectangle dirtyBounds;
    DasCanvasComponent parent;
    
    DasAxis axis;
    int ncycles;
    private boolean horizontal;
    
    /** Creates a new instance of HorizontalFrequencyDragRenderer */
    public FrequencyDragRenderer( DasCanvasComponent parent, DasAxis axis ) {
        this.parent= parent;
        parent.addKeyListener(this);
        this.axis= axis;
        this.dirtyBounds= new Rectangle();
        ncycles=1;
    }
    
    public boolean isXRangeSelection() {
        return false;
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
        
        //Probably only need to do this once.
        horizontal = axis.isHorizontal();
        
        Graphics2D g= (Graphics2D) g1;
        int x1= p1.x;
        int x2 = p2.x;
        int y1 = p1.y;
        int y2 = p2.y;
        if (horizontal&&x2<x1) { int t=x2; x2= x1; x1= t; }
        if (!horizontal&&y2<y1) { int t=y2; y2=y2; y1=t; }
        int width= horizontal ? x2-x1 : y2-y1;
        
        Color color0= g.getColor();
        Stroke stroke0 = g.getStroke();
        
        g.setColor(new Color(255,255,255,128));
        g.setStroke(new BasicStroke( 3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ));
        
        internalRender(g, dirtyBounds, x1, x2, y1, y2);
        
        g.setStroke(stroke0);
        g.setColor(color0);
        
        internalRender(g, dirtyBounds, x1, x2, y1, y2);
        
        Datum periodDatum;
        if (horizontal) {
            periodDatum= axis.invTransform( x2 ) . subtract( axis.invTransform( x1 ) );
        }
        else {
            periodDatum= axis.invTransform( y2 ) . subtract( axis.invTransform( y1 ) );
        }
        double period= periodDatum.doubleValue( periodDatum.getUnits() );
        double freq= ncycles / period;
        
        DecimalFormat df= new DecimalFormat("0.00");
        renderLabel(g1, p1, p2, "T:"+df.format(period)+" f:"+df.format(freq) );
        
        return new Rectangle[] { dirtyBounds };
    }
    
    private void internalRender(Graphics2D g, Rectangle dirtyBounds, int x1, int x2, int y1, int y2) {
        double width = horizontal ? x2 - x1 : y2 - y1;
        if (horizontal) {
            if ( width > 6 )
                g.drawLine(x1+3, y2, x2-3, y2);
            g.drawLine(x1, y2+2, x1, y2-2 ); //serifs
            g.drawLine(x2, y2+2, x2, y2-2 );
        
            dirtyBounds.setRect(x1-2,y2-5,4,10);
        }
        else {
            if ( width > 6 )
                g.drawLine(x2, y1+3, x2, y2-3);
            g.drawLine(x2+2, y1, x2-2, y1 ); //serifs
            g.drawLine(x2+2, y2, x2-2, y2 );
        
            dirtyBounds.setRect(x2-5,y1-2,10,4);
        }
        
        double rwidth= width / (double) ncycles;
        if ( width>3*ncycles ) {
            double start = horizontal ? x2 + rwidth : y2 + rwidth;
            int limit = horizontal ? axis.getColumn().getWidth() : axis.getRow().getHeight();
            for ( double ii= start; ii<limit; ii+= rwidth ) {
                if(horizontal) {
                    g.drawLine( (int)ii, y2+2, (int)ii, y2-2 );
                    dirtyBounds.add((int)ii+2,y2-5);
                }
                else {
                    g.drawLine( x2+2, (int)ii, x2, (int)ii );
                    dirtyBounds.add(x2-5,(int)ii+2);
                }
            }
            start = horizontal ? x2 - rwidth : y2 - rwidth;
            for ( double ii= start; ii>0; ii-= rwidth ) {
                if(horizontal) {
                    g.drawLine( (int)ii, y2+2, (int)ii, y2-2 );
                    dirtyBounds.add((int)ii-2,y2-5);
                }
                else {
                    g.drawLine( x2+2, (int)ii, x2, (int)ii );
                    dirtyBounds.add(x2-5,(int)ii-2);
                }
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
    
    public boolean isYRangeSelection() {
        return false;
    }
    
    public MouseDragEvent getMouseDragEvent(Object source, java.awt.Point p1, java.awt.Point p2, boolean isModified) {
        return null;
    }
    
    public void keyPressed(KeyEvent e) {
        int keyCode= e.getKeyCode();
        System.out.println(e);
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
