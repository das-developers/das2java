/* File: VerticalRangeGesturesRenderer.java
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

/**
 *
 * @author  eew
 */
public class VerticalRangeGesturesRenderer implements DragRenderer {
    
    private final GesturesRenderer gr;
    private final Rectangle dirtyBounds;    
    private final DasCanvasComponent parent;
    
    public VerticalRangeGesturesRenderer(DasCanvasComponent parent) {
       gr= new GesturesRenderer(parent);
       this.parent= parent;
       dirtyBounds= new Rectangle();
    }
    
    @Override
    public Rectangle[] renderDrag(Graphics g1, Point p1, Point p2) {
        
        Graphics2D g= (Graphics2D) g1;
        
        if (  gr.isGesture(p1,p2) ) {
            gr.renderDrag( g, p1, p2 );
            dirtyBounds.setBounds(gr.getDirtyBounds());
        } else {
                        
            int y2 = p2.y;
            int y1= p1.y;
            if (y2<y1) { int t=y2; y2= y1; y1= t; }
            int height= y2-y1;
            int x = p2.x;
            
            Color color0= g.getColor();
            g.setColor(new Color(255,255,255,100));            
            g.setStroke(new BasicStroke( 3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ));            

            if ( height > 6 ) {
                g.drawLine(x, y1+3, x, y2-3);
            }
            g.drawLine(x+2, y1, x-2, y1 ); //serifs
            g.drawLine(x+2, y2, x-2, y2 );
            
            g.setStroke(new BasicStroke());
            g.setColor(color0);
            
            if ( height > 6 ) {
                g.drawLine(x, y1+3, x, y2-3);
            }
            g.drawLine(x+2, y1, x-2, y1 ); //serifs
            g.drawLine(x+2, y2, x-2, y2 );
            dirtyBounds.setLocation(x-4,y1-2);
            dirtyBounds.add(x+4,y2+2);            
        }
        return new Rectangle[] { dirtyBounds };
    }
    
    
    @Override
    public MouseDragEvent getMouseDragEvent(Object source, Point p1, Point p2, boolean isModified) {
        //double dx= p2.x-p1.x;
        //double dy= -1* ( p2.y-p1.y );
        if ( gr.isGesture(p1,p2) ) {
            return gr.getMouseDragEvent(source,p1,p2,isModified);             
        } else {           
            return new MouseRangeSelectionEvent(source,p1.y,p2.y, isModified );
        }                
    }
        
    @Override
    public void clear(Graphics g) {
        parent.paintImmediately(dirtyBounds);
    }
    
    @Override
    public boolean isPointSelection() {
        return false;
    }
    
    @Override
    public boolean isUpdatingDragSelection() {
        return false;
    }    
    
}
