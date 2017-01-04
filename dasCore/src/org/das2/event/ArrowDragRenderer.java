/*
 * ArrowDragRenderer.java
 *
 * Created on February 13, 2007, 3:50 PM
 *
 *
 */

package org.das2.event;

import org.das2.graph.Arrow;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

/**
 *
 * @author Jeremy
 */
public class ArrowDragRenderer implements DragRenderer {
    @Override
    public Rectangle[] renderDrag(Graphics g, Point p1, Point p2) {
        g.setClip( null );
        Arrow.paintArrow( (Graphics2D)g, p2, p1, 12 , Arrow.HeadStyle.DRAFTING );
        Rectangle result= new Rectangle(p1);
        result.add(p2);
        result.x-= 6;
        result.y-= 6;
        result.width+= 12;
        result.height+= 12;
        
        return new Rectangle[] { result };
    }
    
    @Override
    public void clear(Graphics g) {
        
    }
    
    @Override
    public MouseDragEvent getMouseDragEvent(Object source, Point p1, Point p2, boolean isModified) {
        return null;
    }
    
    @Override
    public boolean isPointSelection() {
        return true;
    }
    
    @Override
    public boolean isUpdatingDragSelection() {
        return true;
    }
    
}
