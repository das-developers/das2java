/*
 * ArrowDragRenderer.java
 *
 * Created on February 13, 2007, 3:50 PM
 *
 *
 */

package edu.uiowa.physics.pw.das.event;

import edu.uiowa.physics.pw.das.graph.Arrow;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

/**
 *
 * @author Jeremy
 */
public class ArrowDragRenderer implements DragRenderer {
    public Rectangle[] renderDrag(Graphics g, Point p1, Point p2) {
        Arrow.paintArrow( (Graphics2D)g, p2, p1, 12 );
        Rectangle result= new Rectangle(p1);
        result.add(p2);
        return new Rectangle[] { result };
    }
    
    public void clear(Graphics g) {
        
    }
    
    public MouseDragEvent getMouseDragEvent(Object source, Point p1, Point p2, boolean isModified) {
        return null;
    }
    
    public boolean isPointSelection() {
        return true;
    }
    
    public boolean isUpdatingDragSelection() {
        return true;
    }
    
}
