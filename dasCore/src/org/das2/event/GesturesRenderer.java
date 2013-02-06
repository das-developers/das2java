/* File: GesturesRenderer.java
 * Copyright (C) 2002-2003 The University of Iowa
 * Created by: Jeremy Faden <jbf@space.physics.uiowa.edu>
 *             Jessica Swanner <jessica@space.physics.uiowa.edu>
 *             Edward E. West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.das2.event;
import org.das2.graph.DasCanvasComponent;

import java.awt.*;

/**
 *
 * @author  jbf
 */
public class GesturesRenderer implements DragRenderer {
    
    DasCanvasComponent parent;
    Rectangle dirtyBounds;
    
    /** Creates a new instance of GesturesRenderer */
    public GesturesRenderer(DasCanvasComponent parent) {
        this.parent= parent;
        dirtyBounds= new Rectangle();
    }
    
    public MouseDragEvent getMouseDragEvent(Object source, Point p1, Point p2, boolean isModified) {
        Gesture g=null;
        double dx= p2.x-p1.x;
        double dy= -1* ( p2.y-p1.y );
        double angle= Math.atan2(dy, dx) * 180 / Math.PI;
        double radius= Math.sqrt(dy*dy+dx*dx);
        int width= ((Component)source).getWidth();
        int xOffset= ((Component)source).getLocation().x;
        if ( radius<20 && radius>4) {
            if ( (p1.x-xOffset<10) && (p1.x-xOffset)>=0 && (p2.x-xOffset)<0 ) {
                g= Gesture.SCANPREV;
            } else if ((p1.x-xOffset)>(width-10) && (p1.x-xOffset)<width && (p2.x-xOffset)>=width ) {
                g= Gesture.SCANNEXT;
            } else if (Math.abs(angle)>160) {
                g= Gesture.BACK;
            } else if (-110<angle && angle<-70) {
                g= Gesture.ZOOMOUT;
            } else if (Math.abs(angle)<30) {
                g= Gesture.FORWARD;
            }
            
        } else {
            g= Gesture.UNDEFINED;
        }
        return new MouseDragEvent(source,g);
    }
    
    public Rectangle[] renderDrag(Graphics g1, Point p1, Point p2) {
        Graphics2D g= (Graphics2D) g1;
        double dx= p2.x-p1.x;
        double dy= -1* ( p2.y-p1.y );
        double angle= Math.atan2(dy, dx) * 180 / Math.PI;
        double radius= Math.sqrt(dy*dy+dx*dx);
        
        int width= parent.getWidth();
        int xOffset= 0;  // note the inconsistency with getMouseDragEvent--yuk!!!
        
        if (radius>4) {
            
            Color color0= g.getColor();
            
            for (int i=0; i<2; i++) {
                if (i==0) {
                    g.setColor(new Color(255,255,255,100));
                    g.setStroke(new BasicStroke( 3.0f,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ));
                } else {
                    g.setColor(color0);
                    g.setStroke(new BasicStroke());
                }
                
                if (Math.abs(angle)>160) {
                    g.drawLine(p1.x,p1.y,p1.x-5,p1.y);
                    g.drawLine(p1.x-5,p1.y,p1.x-3,p1.y-2);
                    g.drawLine(p1.x-5,p1.y,p1.x-3,p1.y+2);
                } else if (-110<angle && angle<-70) {
                    g.drawLine(p1.x,   p1.y,   p1.x,   p1.y+5);
                    g.drawLine(p1.x-2, p1.y+3, p1.x, p1.y+5);
                    g.drawLine( p1.x+2, p1.y+3,p1.x, p1.y+5);
                } else if (Math.abs(angle)<30) {
                    g.drawLine(p1.x,p1.y,p1.x+5,p1.y);
                    g.drawLine(p1.x+5,p1.y,p1.x+3,p1.y-2);
                    g.drawLine(p1.x+5,p1.y,p1.x+3,p1.y+2);
                }                
            }
        }
        dirtyBounds.setRect(p1.x-10,p1.y-10,20,20);
        return new Rectangle[] { dirtyBounds };
    }
    
    
    public void clear(Graphics g) {
        parent.paintImmediately(dirtyBounds);
    }
    
    protected Rectangle getDirtyBounds() {
        return this.dirtyBounds;
    }
    
    public boolean isPointSelection() {
        return false;
    }
    
    public boolean isUpdatingDragSelection() {
        return false;
    }
    
    public boolean isGesture( Point p1, Point p2 ) {
        double dx= p2.x-p1.x;
        double dy= -1* ( p2.y-p1.y );
        double radius= Math.sqrt(dy*dy+dx*dx);
        return radius < 20;
    }
    
}
