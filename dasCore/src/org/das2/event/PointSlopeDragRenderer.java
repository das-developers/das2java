/*
 * PointSlopeDragRenderer.java
 *
 * Created on February 19, 2004, 11:31 PM
 */

package org.das2.event;

import org.das2.graph.DasCanvasComponent;
import org.das2.graph.DasAxis;
import org.das2.datum.Datum;
import org.das2.util.GrannyTextRenderer;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.text.*;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumUtil;
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.Units;
import org.das2.datum.UnitsConverter;

/**
 * Shows the slope from the click point to the drag point.
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
        
        gtr= new GrannyTextRenderer();
        nf= new DecimalFormat( "0.00E0" );       
    }
    
    @Override
    public Rectangle[] renderDrag(java.awt.Graphics g1, java.awt.Point p1, java.awt.Point p2) {
        Graphics2D g= ( Graphics2D ) g1;        
        
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        double atan= Math.atan2( p2.y-p1.y, p2.x-p1.x );
        
        Line2D line2= new Line2D.Double( p1.x + (int)(6.0 * Math.cos(atan)), (int)(p1.y + 6.0*Math.sin(atan)), p2.x, p2.y );
        
        Color color0= g.getColor();
        g.setColor(new Color(255,255,255,100));
        g.setStroke(new BasicStroke( 3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ));
        g.draw( line2 );
        g.draw( new Ellipse2D.Double( p1.x-4, p1.y-4, 8, 8 ) );

        g.setStroke(new BasicStroke());
        g.setColor(color0);
        
        g1.drawLine( p1.x, p1.y, p2.x, p2.y );
        g1.drawOval(p1.x-1, p1.y-1, 3, 3 ) ;
        
        Rectangle myDirtyBounds= new Rectangle( p1.x-2, p1.y-2, 5, 5 );
                
        myDirtyBounds.add(p2.x-2,p2.y-2);
        myDirtyBounds.add(p2.x+2,p2.y+2);
        
        Datum run= xaxis.invTransform(p2.x).subtract(xaxis.invTransform(p1.x));        
        Datum rise= yaxis.invTransform(p2.y).subtract(yaxis.invTransform(p1.y));
            
        Datum xdr= xaxis.getDatumRange().width();
        Datum ydr= yaxis.getDatumRange().width();
        
        Units xunits= DatumUtil.asOrderOneUnits(xdr).getUnits();
        Units yunits= DatumUtil.asOrderOneUnits(ydr).getUnits();
        run= run.convertTo(xunits);
        rise= rise.convertTo(yunits);
        
        if ( xaxis.isLog() ) {
            setLabel("xaxis is log");
        } else if ( yaxis.isLog() ) {
            setLabel("yaxis is log");
        } else {
            if ( !p1.equals(p2) ) {
                try {
                    Datum slope= rise.divide(run);
                    setLabel( "m="+slope );
                } catch ( InconvertibleUnitsException ex ) {
                    double drise= rise.doubleValue(rise.getUnits());
                    double drun= run.doubleValue(run.getUnits());
                    double mag= drise/drun;
                    String units= "" + rise.getUnits() + " / " + run.getUnits();
                    setLabel( "m=" + nf.format(mag) + " " + units );
                } catch ( IllegalArgumentException ex ) {  //  1/deg
                    double drise= rise.doubleValue(rise.getUnits());
                    double drun= run.doubleValue(run.getUnits());
                    double mag= drise/drun;
                    String units= "" + rise.getUnits() + " / " + run.getUnits();
                    setLabel( "m=" + nf.format(mag) + " " + units );
                }
            } else {
                setLabel( "" );
            }
        }
        super.renderDrag( g, p1, p2 );
        return new Rectangle[] { dirtyBounds, myDirtyBounds } ;
                
    }
    
}
