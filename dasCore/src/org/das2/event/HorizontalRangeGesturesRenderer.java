/* File: HorizontalRangeGesturesRenderer.java
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
import java.awt.Rectangle;
import org.das2.graph.DasCanvasComponent;

import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import org.das2.graph.DasAxis;
import org.das2.util.GrannyTextRenderer;

/**
 *
 * @author  eew
 */
public class HorizontalRangeGesturesRenderer implements DragRenderer {
    
    private final Rectangle dirtyBounds;
    DasCanvasComponent parent;
    GesturesRenderer gr;
    
    public HorizontalRangeGesturesRenderer(DasCanvasComponent parent) {
        this.parent= parent;
        dirtyBounds= new Rectangle();
        gr= new GesturesRenderer(parent);
    }
    
    @Override
    public Rectangle[] renderDrag(Graphics g1, Point p1, Point p2) {
        
        Graphics2D g= (Graphics2D) g1;
        
        double dx= p2.x-p1.x;
        double dy= -1* ( p2.y-p1.y );
        double radius= Math.sqrt(dy*dy+dx*dx);
        
        if ( radius<20 ) {
            gr.renderDrag( g, p1, p2 );
            dirtyBounds.setBounds(gr.getDirtyBounds());
            
        } else {
            
            int x2 = p2.x;
            int x1= p1.x;
            if (x2<x1) { int t=x2; x2= x1; x1= t; }
            int width= x2-x1;
            int y = p2.y;
            
            Color color0= g.getColor();
            g.setColor(new Color(255,255,255,100));
            g.setStroke(new BasicStroke( 3.0f,
            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ));
            
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
            
            // see if drawing again fixes WebStart bug //
            if ( width > 6 ) {
                g.drawLine(x1+3, y, x2-3, y);
            }
            g.drawLine(x1, y+2, x1, y-2 ); //serifs
            g.drawLine(x2, y+2, x2, y-2 );

            dirtyBounds.setLocation(x1-2,y+3);
            dirtyBounds.add(x2+2,y-3);

            if ( parent instanceof DasAxis && ( x1<parent.getColumn().getDMinimum() || x2>parent.getColumn().getDMaximum() ) ) {
                DasAxis p= (DasAxis)parent;
                DatumRange dr= DatumRangeUtil.union( p.invTransform( x1 ), p.invTransform(x2 ) );
                dr= p.getTickV().enclosingRange( dr, true );
                g.setColor( new Color(255,255,255,200) );
                GrannyTextRenderer gtr= new GrannyTextRenderer();
                gtr.setString( g1, ""+dr );
                Rectangle r= gtr.getBounds();
                int x;
                if ( x2>parent.getColumn().getDMaximum() ) {
                    x= x1+3;
                } else {
                    x= x2-3-(int)gtr.getWidth();
                }
                r.translate(x,y+g.getFontMetrics().getHeight() );
                g.fill( r );
                g.setColor(color0);
                gtr.draw( g, x, y+g.getFontMetrics().getHeight() );
                dirtyBounds.add(x,y+g.getFontMetrics().getHeight() );
            }
            
        }
        return new Rectangle[] { dirtyBounds };
    }
    
    
    @Override
    public MouseDragEvent getMouseDragEvent(Object source, Point p1, Point p2, boolean isModified) {
        
        double dx= p2.x-p1.x;
        double dy= -1* ( p2.y-p1.y );
        double radius= Math.sqrt(dy*dy+dx*dx);
        if ( radius<20 ) {
            return gr.getMouseDragEvent(source,p1,p2,isModified);            
        } else {
            return new MouseRangeSelectionEvent( source,p1.x,p2.x, isModified );
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
