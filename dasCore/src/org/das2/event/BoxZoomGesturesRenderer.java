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
package org.das2.event;

import org.das2.graph.DasCanvasComponent;

import java.awt.*;

/**
 * BoxZoom renderer that shows delegation to X and Y axis zooms.
 * @author  eew
 */
public class BoxZoomGesturesRenderer extends BoxRenderer {
    
    GesturesRenderer gr;
    DragRenderer hr,vr;

    protected enum Type { BOX, XAXIS, YAXIS };

    public BoxZoomGesturesRenderer(DasCanvasComponent parent) {
        super(parent);
        gr= new GesturesRenderer(parent);
        hr= new HorizontalRangeGesturesRenderer(parent);
        vr= new VerticalRangeGesturesRenderer(parent);
    }
    
    public void clear(Graphics g) {
        parent.paintImmediately(dirtyBounds);
    }

    protected static Type idType( DasCanvasComponent parent, Point p1, Point p2 ) {
        int ymax= Math.max( p1.y, p2.y );
        int ymin= Math.min( p1.y, p2.y );
        int xmax= Math.max( p1.x, p2.x );
        int xmin= Math.min( p1.x, p2.x );

        double boxAspect= ( ymax - ymin ) / (float)(  xmax - xmin );
        boolean edgeY= ymax>parent.getRow().getDMaximum() || ymin<parent.getRow().getDMinimum();
        boolean edgeX= xmax>parent.getColumn().getDMaximum() || xmin<parent.getColumn().getDMinimum();

        //System.err.println("boxAspect="+boxAspect);
        //check for narrow (<5px) boxes which we will treat as accidental in the narrow dimension
        if ( ( ymax-ymin )<5 || ( boxAspect<0.2 ) ) {
            return Type.XAXIS;
        }
        if ( ( xmax-xmin )<5 || ( boxAspect>5 ) ) {
            return Type.YAXIS;
        }
        //boxes along axes must only zoom along that axes.  The intent might have been to start the box on the axis instead of the plot.
        if ( edgeY && boxAspect<0.2 ) {
            return Type.XAXIS;
        }
        if ( edgeX && boxAspect>5 ) {
            return Type.YAXIS;
        }
        return Type.BOX;
    }

    //java.util.List<Point> pts= new ArrayList();
        
    @Override
    public Rectangle[] renderDrag(Graphics g1, Point p1, Point p2) {
        Graphics2D g= (Graphics2D) g1;
//
//        if ( Point.distance( p1.x, p1.y, p2.x, p2.y ) < 5 ) {
//            pts.clear();
//        } else {
//            pts.add(p2);
//        }
//        for ( Point p: pts ) {
//            g1.drawRect( p.x, p.y, 1, 1 );
//        }        
        
        if ( gr.isGesture( p1, p2 ) ) {
            Rectangle[] rr= gr.renderDrag( g, p1, p2 );
            dirtyBounds= rr[0];
        } else {

            Type t= idType( parent, p1, p2 );

            if ( t==Type.XAXIS ) {
                Rectangle[] rr= hr.renderDrag(g1, p1, p2);
                dirtyBounds= rr[0];

            } else if ( t==Type.YAXIS ) {
                Rectangle[] rr= vr.renderDrag(g1, p1, p2);
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
