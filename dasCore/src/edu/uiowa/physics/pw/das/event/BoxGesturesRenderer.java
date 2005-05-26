/* File: BoxRenderer.java
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
package edu.uiowa.physics.pw.das.event;

import edu.uiowa.physics.pw.das.graph.DasCanvasComponent;

import java.awt.*;

/**
 *
 * @author  eew
 */
public class BoxGesturesRenderer extends BoxRenderer {
    
    GesturesRenderer gr;
    
    public BoxGesturesRenderer(DasCanvasComponent parent) {
        super(parent);
        gr= new GesturesRenderer(parent);
    }
    
    public void clear(Graphics g) {
        parent.paintImmediately(dirtyBounds);
    }
        
    public Rectangle[] renderDrag(Graphics g1, Point p1, Point p2) {
        Graphics2D g= (Graphics2D) g1;
                
        if ( gr.isGesture( p1, p2 ) ) {
            Rectangle[] rr= gr.renderDrag( g, p1, p2 );
            dirtyBounds= rr[0];
        } else {
            Rectangle r = new Rectangle(p1);
            r.add(p2);
            
            Color color0= g.getColor();
            g.setColor(new Color(255,255,255,100));
            g.setStroke(new BasicStroke( 3.0f,
            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ));
            
            g.drawRect(r.x, r.y, r.width, r.height);
            
            g.setStroke(new BasicStroke());
            g.setColor(color0);
            
            g.drawRect(r.x, r.y, r.width, r.height);
            
            dirtyBounds.setLocation(r.x-2,r.y-3);
            dirtyBounds.add(r.x+r.width+2,r.y+r.height+3);
        }
        return new Rectangle[] { dirtyBounds };
    }
    
    public MouseDragEvent getMouseDragEvent(Object source, Point p1, Point p2, boolean isModified) {
        if ( gr.isGesture(p1,p2) ) {
            return gr.getMouseDragEvent( source, p1, p2, isModified );
        } else {
            return super.getMouseDragEvent( source, p1, p2, isModified );
        }
    }
    
}
