/* File: VerticalSliceSelectionRenderer.java
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
public class VerticalSliceSelectionRenderer implements DragRenderer {
    
    DasCanvasComponent parent;
    Rectangle dirtyBounds;
    
    Point crossHairLocation=null;
    
    /** Creates a new instance of VerticalLineSelectionRenderer */
    public VerticalSliceSelectionRenderer(DasCanvasComponent parent) {
        this.parent= parent;
        dirtyBounds= new Rectangle();
    }
    
    private void drawCrossHair(Graphics g0, Point p) {
        
        Graphics g= g0.create();
        
        g.setColor(new Color(0,0,0));
        g.setXORMode(Color.white);
        
        if (crossHairLocation!=null) {
            if (!crossHairLocation.equals(p)) {
                drawCrossHair(g,crossHairLocation);
            }
        }
        
        Dimension d= parent.getSize();
        //g.drawLine((int)0, (int)p.y, (int)d.getWidth(), (int)p.y);
        g.drawLine( p.x, 0, p.x, d.height );
        
        if (crossHairLocation!=null) {
            if (crossHairLocation.equals(p)) {
                crossHairLocation=null;
            } else {
                // this shouldn't happen if things are working properly
                edu.uiowa.physics.pw.das.util.DasDie.println("Sorry about the crosshair mess!");
                crossHairLocation=p;
            }
        } else {
            crossHairLocation= p;
        }
        
        g.dispose();
        
    }
    
    public void renderDrag(Graphics g, Point p1, Point p2) {
        
        if (crossHairLocation!=null) { //make sure the crosshair is erased
            drawCrossHair(g,crossHairLocation);
        }
        drawCrossHair(g,p2);
        //g.drawLine(p2.x, 0, p2.x, parent.getHeight());
        //dirtyBounds.setRect(p2.x,0,1,parent.getHeight());
    }
    
    
    public MouseDragEvent getMouseDragEvent(Object o,Point p1,Point p2,boolean isModified) {
        return null;
    }
    
    public void clear(Graphics g) {
         if (crossHairLocation!=null) {
            drawCrossHair(g,crossHairLocation);
        }
        //parent.paintImmediately(dirtyBounds);
    }
    
    public boolean isXRangeSelection() {
        return false;
    }
    
    public boolean isYRangeSelection() {
        return false;
    }
    
    public boolean isPointSelection() {
        return true;
    }
    
    public boolean isUpdatingDragSelection() {
        return false;
    }
    
}
