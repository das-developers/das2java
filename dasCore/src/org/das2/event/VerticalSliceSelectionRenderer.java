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

package org.das2.event;

import edu.uiowa.physics.pw.das.graph.DasCanvasComponent;
import edu.uiowa.physics.pw.das.graph.DasRow;

import java.awt.*;
/**
 *
 * @author  eew
 */
public class VerticalSliceSelectionRenderer implements DragRenderer {
    
    DasCanvasComponent parent;
    Rectangle dirtyBounds;
    
    /** Creates a new instance of VerticalLineSelectionRenderer */
    public VerticalSliceSelectionRenderer(DasCanvasComponent parent) {
        this.parent= parent;
        dirtyBounds= new Rectangle();
    }
    
    private void drawCrossHair(Graphics g0, Point p) {
        
        Graphics g= g0.create();
        
        g.setColor(new Color(0,0,0));
        g.setXORMode(Color.white);        
        
        DasRow row= parent.getRow();
        
        g.drawLine( p.x, row.getDMinimum(), p.x, row.getDMaximum() );
        
        g.dispose();
        
    }
    
    public Rectangle[] renderDrag(Graphics g, Point p1, Point p2) {        
        drawCrossHair(g,p2);
        //g.drawLine(p2.x, 0, p2.x, parent.getHeight());
        dirtyBounds.setRect(p2.x,0,1,parent.getHeight());
        return new Rectangle[] { dirtyBounds };
    }
    
    
    public MouseDragEvent getMouseDragEvent(Object o,Point p1,Point p2,boolean isModified) {
        return null;
    }
    
    public void clear(Graphics g) {        
        parent.paintImmediately(dirtyBounds);
    }
    
    public boolean isPointSelection() {
        return true;
    }
    
    public boolean isUpdatingDragSelection() {
        return false;
    }
    
}
