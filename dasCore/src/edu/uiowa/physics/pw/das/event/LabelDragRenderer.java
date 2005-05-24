/*
 * LabelDragRenderer.java
 *
 * Created on October 5, 2004, 1:25 PM
 */

package edu.uiowa.physics.pw.das.event;

import edu.uiowa.physics.pw.das.graph.*;
import edu.uiowa.physics.pw.das.util.*;
import java.awt.*;

/**
 *
 * @author  Jeremy
 */
public class LabelDragRenderer implements DragRenderer {
        
    String label="Label not set";
    GrannyTextRenderer gtr;
    DasCanvasComponent parent;
    
    /* the implementing class is responsible for setting this */
    Rectangle dirtyBounds;
    
    int maxLabelWidth;    
    
    public void clear(Graphics g) {
        parent.paintImmediately(dirtyBounds);        
    }
        
    LabelDragRenderer( DasCanvasComponent parent ) {
        this.parent= parent;
        this.dirtyBounds= new Rectangle();
        gtr= new GrannyTextRenderer();
    }
    
    public MouseDragEvent getMouseDragEvent(Object source, java.awt.Point p1, java.awt.Point p2, boolean isModified) {
        return null;
    }
    
    public boolean isPointSelection() {
        return true;
    }
    
    public boolean isUpdatingDragSelection() {
        return false;
    }
    
    public boolean isXRangeSelection() {
        return true;
    }
    
    public boolean isYRangeSelection() {
        return true;
    }
    
    void setLabel( String s ) {
        this.label= s;
    }
    
    Rectangle paintLabel( Graphics g1, java.awt.Point p2 ) {
        
        Graphics2D g= (Graphics2D)g1;
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        
        FontMetrics fm= g.getFontMetrics();
        
        Dimension d= parent.getSize();
        
        gtr.setString(parent, label);
        
        int dx= (int)gtr.getWidth()+6;
        
        int dy= (int)gtr.getHeight();
        
        int xp= p2.x+3;
        if (maxLabelWidth<dx) maxLabelWidth=dx;
        
        int yp= p2.y-3-dy;
        
        if ( (xp+maxLabelWidth>d.width-3) && (p2.x-3-dx>0) ) {
            xp= p2.x-3-dx;
        }
        
        if (yp<13) {
            yp= p2.y+3;
        }
        
        Rectangle dirtyBounds= new Rectangle();;
        
        Color color0= g.getColor();
        
        // draw the translucent background
        g.setColor(new Color(255,255,255,200));
        dirtyBounds.setRect(xp,yp,dx,dy);
        g.fill(dirtyBounds);
        
        // draw the label
        g.setColor(new Color(20,20,20));
        gtr.draw( g, xp+3, (float)(yp+gtr.getAscent()) );
        
        g.setColor(color0);
        
        return dirtyBounds;
    }

    public void renderDrag(Graphics g, Point p1, Point p2) {        
        dirtyBounds= paintLabel( g, p2 );
    }
}
