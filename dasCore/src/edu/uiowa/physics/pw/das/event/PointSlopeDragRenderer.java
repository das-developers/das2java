/*
 * PointSlopeDragRenderer.java
 *
 * Created on February 19, 2004, 11:31 PM
 */

package edu.uiowa.physics.pw.das.event;

import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.graph.*;
import edu.uiowa.physics.pw.das.util.*;
import java.awt.*;
import java.text.*;

/**
 *
 * @author  Owner
 */
public class PointSlopeDragRenderer extends LabelDragRenderer {
    
    
    DasAxis xaxis, yaxis;    
    NumberFormat nf;
    
    /** Creates a new instance of PointSlopeDragRenderer */
    public PointSlopeDragRenderer(DasCanvasComponent parent, DasAxis xaxis, DasAxis yaxis ) {
        super( parent );
        this.parent= parent;
        this.xaxis= xaxis;
        this.yaxis= yaxis;    
        dirtyBounds= new Rectangle();
        gtr= new GrannyTextRenderer();
        nf= new DecimalFormat( "0.00E0" );       
    }
    
    public void renderDrag(java.awt.Graphics g1, java.awt.Point p1, java.awt.Point p2) {
        Graphics2D g= ( Graphics2D ) g1;        
        g1.drawLine( p1.x, p1.y, p2.x, p2.y );
        g1.drawOval(p1.x-1, p1.y-1, 3, 3 ) ;
        dirtyBounds.setRect( p1.x-2, p1.y-2, 5, 5 );        
        dirtyBounds.add(p2.x-2,p2.y-2);
        dirtyBounds.add(p2.x+2,p2.y+2);
        
        Datum run= xaxis.invTransform(p2.x+parent.getX()).subtract(xaxis.invTransform(p1.x+parent.getX()));        
        Datum rise= yaxis.invTransform(p2.y+parent.getY()).subtract(yaxis.invTransform(p1.y+parent.getY()));
            
        if ( !p1.equals(p2) ) {
            double slope= rise.doubleValue(rise.getUnits()) / run.doubleValue(run.getUnits());
            setLabel( nf.format(slope) );
        } else {
            setLabel( "" );
        }
        dirtyBounds.add( paintLabel( g, p2 ) ) ;
    }
    
}
