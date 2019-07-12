/* File: HorizontalRangeRenderer.java
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import org.das2.graph.DasCanvasComponent;

import java.awt.Rectangle;

/**
 *
 * @author  eew
 */
public class HorizontalRangeRenderer implements DragRenderer {
    
    private final Rectangle dirtyBounds;
    DasCanvasComponent parent;
    
    public HorizontalRangeRenderer(DasCanvasComponent parent) {
        this.parent= parent;
        dirtyBounds= new Rectangle();
    }
    
    @Override
    public Rectangle[] renderDrag(Graphics g1, Point p1, Point p2) {
        
        Graphics2D g= (Graphics2D) g1;
        int x2 = p2.x;
        int x1= p1.x;
        if (x2<x1) { int t=x2; x2= x1; x1= t; }
        int width= x2-x1;
        int y = p2.y;
        
        Color color0= g.getColor();
        g.setColor(new Color(255,255,255,100));
        g.setStroke(new BasicStroke( 3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ));
        
        if ( width > 6 ) {
            g.drawLine(x1+3, y, x2-3, y);
        }
        g.drawLine(x1, y+2, x1, y-2 ); //serifs
        g.drawLine(x2, y+2, x2, y-2 );
        
        g.setStroke(new BasicStroke());
        g.setColor(color0);
        
        if ( width > 6 ) {
            g.drawLine(x1+3, y, x2-3, y);
        }
        g.drawLine(x1, y+2, x1, y-2 ); //serifs
        g.drawLine(x2, y+2, x2, y-2 );
        
        dirtyBounds.setLocation(x1-2,y+3);
        dirtyBounds.add(x2+2,y-3);
        return new Rectangle[] { dirtyBounds };
    }
    
    @Override
    public MouseDragEvent getMouseDragEvent(Object source, Point p1, Point p2, boolean isModified) {
        return new MouseRangeSelectionEvent(source,p1.x,p2.x,isModified);
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
