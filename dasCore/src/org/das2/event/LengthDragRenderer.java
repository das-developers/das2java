/*
 * PointSlopeDragRenderer.java
 *
 * Created on February 19, 2004, 11:31 PM
 */

package org.das2.event;

import org.das2.graph.DasCanvasComponent;
import org.das2.graph.DasAxis;
import org.das2.graph.DasPlot;
import org.das2.datum.Units;
import org.das2.datum.Datum;
import org.das2.datum.DatumUtil;
import org.das2.datum.UnitsUtil;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.text.*;

/**
 *
 * @author  Owner
 */
public class LengthDragRenderer extends LabelDragRenderer {
    
    private DasAxis xaxis, yaxis;
    private DasPlot plot;
    
    NumberFormat nf;
    
    /** Creates a new instance of PointSlopeDragRenderer */
    public LengthDragRenderer(DasCanvasComponent parent, DasAxis xaxis, DasAxis yaxis) {
        super( parent );
        this.plot= (DasPlot)parent;
        this.xaxis= xaxis;
        this.yaxis= yaxis;
    }
    
    // kludge to handle the fact that datums are not always displayed with their units!!!
    private String datumString( Datum d ) {
        String result= d.toString();
        /*if ( d.getUnits()!= Units.dimensionless && !d.getUnits().isConvertableTo(Units.us2000) ) {
            result+= " "+d.getUnits();
        }*/
        return result;
    }
    
    public Rectangle[] renderDrag(java.awt.Graphics g1, java.awt.Point p1, java.awt.Point p2) {
        Graphics2D g= ( Graphics2D ) g1;
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        double atan= Math.atan2( p2.y-p1.y, p2.x-p1.x );
        
        Line2D line= new Line2D.Double( p1.x + (int)(4.0 * Math.cos(atan)), (int)(p1.y + 4.0*Math.sin(atan)), p2.x, p2.y );
        g.draw( line );
        g.draw( new Ellipse2D.Double( p1.x-4, p1.y-4, 8, 8 ) );
        
        Rectangle myDirtyBounds= new Rectangle();
        
        myDirtyBounds.setRect( p1.x-3, p1.y-3, 7, 7 );
        myDirtyBounds.add(p2.x-2,p2.y-2);
        myDirtyBounds.add(p2.x+2,p2.y+2);
        
        DasAxis xa= xaxis == null ? plot.getXAxis() : xaxis;
        DasAxis ya= yaxis == null ? plot.getYAxis() : yaxis;
        
        if ( !p1.equals(p2) ) {
            Datum x1= xa.invTransform(p2.x);
            Datum x0= xa.invTransform(p1.x);
            Datum run= x1.subtract(x0);
            run= DatumUtil.asOrderOneUnits(run);
            String runString;
            if ( x1.getUnits()==run.getUnits() ) {
                runString= x1.getFormatter().format(run);
            } else {
                runString= datumString(run);
            }
            
            Datum y1= ya.invTransform(p2.y);
            Datum y0= ya.invTransform(p1.y);
            Datum rise= y1.subtract(y0);

            String riseString;
            riseString= datumString(rise);
            
            String radString;
            if ( rise.getUnits().isConvertableTo(run.getUnits()) ) {
                Units u= run.getUnits();
                double rised= rise.doubleValue(u);
                double rund= run.doubleValue(u);
                double rad= Math.sqrt( rised * rised + rund * rund );
                double srised= rise.getResolution(u);
                double srund= run.getResolution(u);
                double res= rad * Math.sqrt(
                        Math.pow( srised / Math.max(  Math.abs(rised), srised ), 2 )  +
                        Math.pow( srund / Math.max( Math.abs( rund ), srund ), 2 ) );
                Datum radDatum= Datum.create( rad, u, res/100. );
                
                radString= "!cR:" + radDatum;
            } else {
                radString= "";
            }
            
            String label= "\u0394x: " + runString + " \u0394y: " + riseString + radString ;
            
            if ( showSlope ) {
                label += "!c m: "+ UnitsUtil.divideToString( rise, run );
            }
            
            if ( showFit ) {
                // show y= m * ( x - x0 ) + y0
                Datum slope= rise.divide(run);
                
                String fit;
                if ( yaxis.isLog() && xaxis.isLog() ) {
                    double ycycles= Math.log10( y1.divide(y0).doubleValue(Units.dimensionless) );
                    double xcycles= Math.log10( x1.divide(x0).doubleValue(Units.dimensionless) );
                    NumberFormat nf= new DecimalFormat("0.00");
                    String sslope= nf.format( ycycles / xcycles );
                    fit = "y= ( x/" +x1 +" )!A"+sslope + "!n * " + y1 ;
                } else if ( yaxis.isLog() && !xaxis.isLog() ) {
                    NumberFormat nf= new DecimalFormat("0.00");
                    Units u= run.getUnits();
                    double drise= Math.log10(y1.divide(y0).doubleValue(Units.dimensionless) );
                    double drun= x1.subtract(x0).doubleValue(u);
                    String sslope= nf.format( drise/drun );
                    String su;
                    if ( u.isConvertableTo(Units.seconds) ) {
                        su= UnitsUtil.divideToString( Units.dimensionless.createDatum(drise), run );
                    } else if ( u==Units.dimensionless ) {
                        su= sslope;
                    } else {
                        su= sslope + "/("+u+")";
                    }
                    
                    fit= "!Cy="+ "10!A( x-("+x1+") )*"+su+"!n * " + y1;

                } else if ( !yaxis.isLog() && xaxis.isLog() ) {
                    fit = "n/a";
                } else {
                    fit= "y="+ slope + " * ( x - ("+x1+") ) + "+ y1;
                }
                label+= "!c" + fit;
            }
            
            
            setLabel( label );
        } else {
            setLabel( "" );
        }
        super.renderDrag( g, p1, p2 );
        return new Rectangle[] { dirtyBounds, myDirtyBounds };
    }
    
    /**
     * Holds value of property showSlope.
     */
    private boolean showSlope= false;
    
    /**
     * Getter for property showSlope.
     * @return Value of property showSlope.
     */
    public boolean isShowSlope() {
        return this.showSlope;
    }
    
    /**
     * Setter for property showSlope.
     * @param showSlope New value of property showSlope.
     */
    public void setShowSlope(boolean showSlope) {
        this.showSlope = showSlope;
    }
    
    protected boolean showFit = false;

    /**
     * Get the value of showFit
     *
     * @return the value of showFit
     */
    public boolean isShowFit() {
        return showFit;
    }

    /**
     * Set the value of showFit
     *
     * @param showFit new value of showFit
     */
    public void setShowFit(boolean showFit) {
        this.showFit = showFit;
    }

}
