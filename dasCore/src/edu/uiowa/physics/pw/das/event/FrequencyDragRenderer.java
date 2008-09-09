/*
 * FrequencyDragRenderer.java
 *
 * Created on September 6, 2005, 4:55 PM
 */

package edu.uiowa.physics.pw.das.event;

import org.das2.datum.Datum;
import edu.uiowa.physics.pw.das.graph.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.*;

/**
 *
 * @author  Jeremy
 * @author  eew
 */
public class FrequencyDragRenderer extends LabelDragRenderer implements DragRenderer, KeyListener {
        
    DasCanvasComponent parent;
    
    DasAxis axis;
    int ncycles;
    private boolean horizontal;
    double period;
    
    private PropertyChangeSupport pcs;
    
    /** Creates a new instance of HorizontalFrequencyDragRenderer */
    public FrequencyDragRenderer( DasCanvasComponent parent, DasAxis axis ) {
        super( parent );
        this.parent= parent;
        parent.addKeyListener(this);
        this.axis= axis;
        this.dirtyBounds= new Rectangle();
        ncycles=1;
    }
    
    public Rectangle[] renderDrag(java.awt.Graphics g1, java.awt.Point p1, java.awt.Point p2) {
        
        Rectangle myDirtyBounds= new Rectangle();
        
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
        
        internalRender(g, myDirtyBounds, x1, x2, y1, y2);
        
        g.setStroke(stroke0);
        g.setColor(color0);
        
        internalRender(g, myDirtyBounds, x1, x2, y1, y2);
        
        Datum periodDatum;
        if (horizontal) {
            periodDatum= axis.invTransform( x2 ) . subtract( axis.invTransform( x1 ) );
        }
        else {
            periodDatum= axis.invTransform( y2 ) . subtract( axis.invTransform( y1 ) );
        }
        double oldPeriod = period;
        period= periodDatum.doubleValue( periodDatum.getUnits() );
        fireChange(oldPeriod, period);
        double freq= ncycles / period;
        
        DecimalFormat df= new DecimalFormat("0.00");
        
        setLabel( "T:"+periodDatum+" f:"+df.format(freq) + "" + periodDatum.getUnits()+"!A-1" );
        
        super.renderDrag( g1, p1, p2 );
        
        return new Rectangle[] { dirtyBounds, myDirtyBounds };
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
    
    private void fireChange(final double oldPeriod, final double newPeriod) {
        if (pcs != null) {
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    pcs.firePropertyChange("period", new Double(oldPeriod), new Double(newPeriod));
                }
            });
        }
    }
    
    public void addPropertyChangeListener(String p, PropertyChangeListener l) {
        if (pcs == null) {
            pcs = new PropertyChangeSupport(this);
        }
        pcs.addPropertyChangeListener(p, l);
    }
    
    public void removePropertyChangeListener(String p, PropertyChangeListener l) {
        if (pcs != null) {
            pcs.removePropertyChangeListener(p, l);
        }
    }
    
}
