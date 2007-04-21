/*
 * MoveComponentMouseModule.java
 *
 * Created on April 21, 2007, 7:10 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.uiowa.physics.pw.das.event;

import edu.uiowa.physics.pw.das.graph.DasCanvasComponent;
import edu.uiowa.physics.pw.das.graph.DasColumn;
import edu.uiowa.physics.pw.das.graph.DasRow;
import java.awt.Point;
import java.awt.event.MouseEvent;

/**
 *
 * @author jbf
 */
public class MoveComponentMouseModule extends MouseModule {
    
    Point p0;
    
    /** Creates a new instance of MoveComponentMouseModule */
    public MoveComponentMouseModule( DasCanvasComponent parent ) {
        super( parent, EmptyDragRenderer.renderer, "Move Component" );
    }

    public void mouseReleased(MouseEvent e) {
        super.mouseReleased(e);
        Point p= e.getPoint();
        int dx= p.x - p0.x;
        int dy= p.y - p0.y;
        
        DasRow row= this.parent.getRow();
        row.setDPosition( row.getDMinimum() + dy, row.getDMaximum() + dy );
        
        DasColumn col= this.parent.getColumn();
        col.setDPosition( col.getDMinimum() + dx, col.getDMaximum() + dx );
        
        p0=null;
    }

    public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        p0= e.getPoint();
    }

    public void mouseDragged(MouseEvent e) {
        super.mouseDragged(e);
    }
    
}
