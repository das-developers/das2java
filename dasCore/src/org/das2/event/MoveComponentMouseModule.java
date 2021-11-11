/*
 * MoveComponentMouseModule.java
 *
 * Created on April 21, 2007, 7:10 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.das2.event;

import org.das2.graph.DasCanvasComponent;
import org.das2.graph.DasColumn;
import org.das2.graph.DasRow;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;

/**
 * Resets the component row and column to implement the move requested with a 
 * mouse drag.
 * @author jbf
 */
public class MoveComponentMouseModule extends MouseModule {

    Point p0;

    static class MoveRenderer implements DragRenderer {

        DasCanvasComponent c;
        BufferedImage i;

        MoveRenderer(DasCanvasComponent c) {
            this.c = c;
        }

        private void refreshImage() { // this doesn't work.
            Rectangle bounds = c.getActiveRegion().getBounds();
            i = new BufferedImage( bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB );
            Graphics g= i.getGraphics();
            g.translate( c.getX(), c.getY() );
            c.paint( g );

        }

        private Point center(Shape s) {
            long avgX = 0,  avgY = 0;
            double[] coords = new double[6];
            long count = 0;
            for (PathIterator i = s.getPathIterator(null); !i.isDone(); i.next()) {
                int type = i.currentSegment(coords);
                if (type == PathIterator.SEG_LINETO) {
                    avgX += coords[0];
                    avgY += coords[1];
                    count++;
                }
            }

            avgX /= count;
            avgY /= count;

            return new Point((int) avgX, (int) avgY);
        }

        private Shape enlarge(Shape s, double scale) {
            Point center = center(s);
            AffineTransform at = new AffineTransform();
            
            at.translate( +center.x, +center.y);
            at.scale(scale, scale);
            GeneralPath gp = new GeneralPath(s);
            gp.transform(at);
            at = new AffineTransform();
            at.translate(-center.x*scale, -center.y*scale);
            gp.transform(at);

            return gp;
        }

        @Override
        public Rectangle[] renderDrag(Graphics g1, Point p1, Point p2) {
            Rectangle bounds = c.getActiveRegion().getBounds();
            bounds.translate(p2.x - p1.x, p2.y - p1.y);
            Graphics2D g = (Graphics2D) g1;
            
            g.setClip(null);
            g.setColor(c.getForeground());
            g.draw(bounds);
            return new Rectangle[] { enlarge(bounds.getBounds(), 1.2 ).getBounds()};
        }

        @Override
        public void clear(Graphics g) {
        }

        @Override
        public MouseDragEvent getMouseDragEvent(Object source, Point p1, Point p2, boolean isModified) {
            return new MouseDragEvent(source);
        }

        @Override
        public boolean isPointSelection() {
            return false;
        }

        @Override
        public boolean isUpdatingDragSelection() {
            return true;
        }
    }

    /** 
     * Creates a new instance of MoveComponentMouseModule
     * @param parent
     */
    public MoveComponentMouseModule(DasCanvasComponent parent) {
        super(parent, new MoveRenderer(parent), "Move Component");
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        super.mouseReleased(e);
        Point p = e.getPoint();
        int dx = p.x - p0.x;
        int dy = p.y - p0.y;

        DasRow row = this.parent.getRow();
        row.setDPosition(row.getDMinimum() + dy, row.getDMaximum() + dy);

        DasColumn col = this.parent.getColumn();
        col.setDPosition(col.getDMinimum() + dx, col.getDMaximum() + dx);

        p0 = null;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        ((MoveRenderer)this.dragRenderer).refreshImage();
        p0 = e.getPoint();
    }

}
