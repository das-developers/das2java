/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.uiowa.physics.pw.das.event;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

/**
 *
 * @author jbf
 */
public class AngleSelectionDragRenderer implements DragRenderer{

    public Rectangle[] renderDrag(Graphics g, Point p1, Point p2) {
        g.drawLine( p1.x, p1.y, p2.x, p2.y );
        Rectangle r= new Rectangle(p1);
        r.add(p2);
        return new Rectangle[] { r };
    }

    public void clear(Graphics g) {
        
    }

    public MouseDragEvent getMouseDragEvent(Object source, Point p1, Point p2, boolean isModified) {
        return new MouseBoxEvent( source, p1, p2, isModified );
    }

    public boolean isPointSelection() {
        return false;
    }

    public boolean isUpdatingDragSelection() {
        return true;
    }

}
