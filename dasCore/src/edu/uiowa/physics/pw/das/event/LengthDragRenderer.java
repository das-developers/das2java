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
public class LengthDragRenderer extends LabelDragRenderer {
    
    DasAxis xaxis, yaxis;
    
    NumberFormat nf;
    
    /** Creates a new instance of PointSlopeDragRenderer */
    public LengthDragRenderer(DasCanvasComponent parent, DasAxis xaxis, DasAxis yaxis) {
        super( parent );
        this.xaxis= xaxis;
        this.yaxis= yaxis;        
    }
    
    
    public void renderDrag(java.awt.Graphics g1, java.awt.Point p1, java.awt.Point p2) {
        Graphics2D g= ( Graphics2D ) g1;
        g1.drawLine( p1.x, p1.y, p2.x, p2.y );
        g1.drawOval(p1.x-1, p1.y-1, 3, 3 ) ;
        
        dirtyBounds= new Rectangle();
        
        dirtyBounds.setRect( p1.x-2, p1.y-2, 5, 5 );
        dirtyBounds.add(p2.x-2,p2.y-2);
        dirtyBounds.add(p2.x+2,p2.y+2);
        
        if ( !p1.equals(p2) ) {
            Datum x0= xaxis.invTransform(p2.x+parent.getX());
            Datum run= x0.subtract(xaxis.invTransform(p1.x+parent.getX()));
            run= DatumUtil.asOrderOneUnits(run);
            String runString;
            if ( x0.getUnits()==run.getUnits() ) {
                runString= x0.getFormatter().format(run); 
            } else {
                runString= run.toString();
            }
            
            Datum y0= yaxis.invTransform(p2.y+parent.getY());
            System.out.println( y0.toString() );
            Datum rise= y0.subtract(yaxis.invTransform(p1.y+parent.getY()));
            String riseString;
            riseString= rise.toString();
            
            String radString;
            if ( rise.getUnits().isConvertableTo(run.getUnits()) ) {
                Units u= run.getUnits();
                double rised= rise.doubleValue(u);
                double rund= run.doubleValue(u);
                double rad= Math.sqrt( rised * rised + rund * rund );
                double srised= rise.getResolution(u);
                double srund= run.getResolution(u);
                double res= rad * Math.sqrt( Math.pow( srised / Math.max(  Math.abs(rised), srised ), 2 )  + 
                        Math.pow( srund / Math.max( Math.abs( rund ), srund ), 2 ) );
                Datum radDatum= Datum.create( rad, u, res );
                
                radString= " R:" + radDatum;
            } else {
                radString= "";
            }
            
            setLabel( "\u0394x: " + runString + " \u0394y: " + riseString + radString );
        } else {
            setLabel( "" );
        }
        super.renderDrag( g, p1, p2 );
        
    }
    
}
