/* File: VerticalRangeGesturesRenderer.java
 * Copyright (C) 2002-2003 University of Iowa
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
public class VerticalRangeGesturesRenderer implements DragRenderer {
    
    protected int xInitial;
    protected int yInitial;
    private GesturesRenderer gr;
    private Rectangle dirtyBounds;    
    private DasCanvasComponent parent;
    
    public VerticalRangeGesturesRenderer(DasCanvasComponent parent) {
       gr= new GesturesRenderer(parent);
       this.parent= parent;
       dirtyBounds= new Rectangle();
    }
    
    public void renderDrag(Graphics g1, Point p1, Point p2) {
        
        Graphics2D g= (Graphics2D) g1;
        
        double dx= p2.x-p1.x;
        double dy= -1* ( p2.y-p1.y );
        double angle= Math.atan2(dy, dx) * 180 / Math.PI;
        double radius= Math.sqrt(dy*dy+dx*dx);
        if ( radius<20 ) {
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
            g.setStroke(new BasicStroke( 3.0f,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ));            

            if ( height > 6 )
                g.drawLine(x, y1+3, x, y2-3);
            g.drawLine(x+2, y1, x-2, y1 ); //serifs
            g.drawLine(x+2, y2, x-2, y2 );
            
            g.setStroke(new BasicStroke());
            g.setColor(color0);
            
            if ( height > 6 )
                g.drawLine(x, y1+3, x, y2-3);
            g.drawLine(x+2, y1, x-2, y1 ); //serifs
            g.drawLine(x+2, y2, x-2, y2 );
            dirtyBounds.setLocation(x-4,y1-2);
            dirtyBounds.add(x+4,y2+2);            
        }
    }
    
    
    public MouseDragEvent getMouseDragEvent(Object source, Point p1, Point p2, boolean isModified) {

        double dx= p2.x-p1.x;
        double dy= -1* ( p2.y-p1.y );        
        double radius= Math.sqrt(dy*dy+dx*dx);
        if ( radius<20 ) {
            return gr.getMouseDragEvent(source,p1,p2,isModified); 
            
        } else {           
            return new MouseRangeGestureSelectionEvent(source,p1.y,p2.y, Gesture.NONE);
        }
        
        
    }
        
    public void clear(Graphics g) {
        parent.paintImmediately(dirtyBounds);
    }
    
    public boolean isXRangeSelection() {
        return false;
    }
        
    public boolean isYRangeSelection() {
        return true;
    }
    
    public boolean isPointSelection() {
        return false;
    }
    
    public boolean isUpdatingDragSelection() {
        return false;
    }    
    
}
