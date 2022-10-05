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
import java.awt.geom.Point2D;
import java.text.*;

/**
 * Indicate the length and the slope of the line.
 * @author  Owner
 */
public class LengthDragRenderer extends LabelDragRenderer {
    
    private final DasAxis xaxis;
    private final DasAxis yaxis;
    private final DasPlot plot;
    
    /** Creates a new instance of PointSlopeDragRenderer
     * @param parent
     * @param xaxis
     * @param yaxis 
     */
    public LengthDragRenderer(DasCanvasComponent parent, DasAxis xaxis, DasAxis yaxis) {
        super( parent );
        this.plot= (DasPlot)parent;
        this.xaxis= xaxis;
        this.yaxis= yaxis;
    }

    /**
     * number of cycles or devisor for length
     */
    int ncycles = 20;
    
    @Override
    public Rectangle[] renderDrag(java.awt.Graphics g1, java.awt.Point p1, java.awt.Point p2) {
        Graphics2D g= ( Graphics2D ) g1;
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        double atan= Math.atan2( p2.y-p1.y, p2.x-p1.x );
        
        Line2D line= new Line2D.Double( p1.x + (int)(4.0 * Math.cos(atan)), (int)(p1.y + 4.0*Math.sin(atan)), p2.x, p2.y );
        Line2D line2= new Line2D.Double( p1.x + (int)(6.0 * Math.cos(atan)), (int)(p1.y + 6.0*Math.sin(atan)), p2.x, p2.y );

        Color color0= g.getColor();
        g.setColor(new Color(255,255,255,100));
        g.setStroke(new BasicStroke( 3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ));
        g.draw( line2 );
        g.draw( new Ellipse2D.Double( p1.x-4, p1.y-4, 8, 8 ) );

        g.setStroke(new BasicStroke());
        g.setColor(color0);

        for ( int i=1; i<ncycles; i++ ) {
            Point2D.Double p1x= new Point2D.Double( p1.x + i* ( p2.x - p1.x ) / ncycles,  p1.y + i* ( p2.y - p1.y ) / ncycles );
            double len= Math.sqrt( Math.pow(p2.x - p1.x,2) + Math.pow(p2.y-p1.y,2 ) );
            double unitY= ( p2.y - p1.y ) / len;
            double unitX= ( p2.x - p1.x ) / len;
                    
            Point2D.Double p2up= new Point2D.Double( p1x.x + unitY * 4, p1x.y - unitX * 4 );
            Point2D.Double p2dn= new Point2D.Double( p1x.x - unitY * 4, p1x.y + unitX * 4 );
            g.draw( new Line2D.Double( p2up, p2dn ) );
        }
        
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
            run= DatumUtil.asOrderOneUnits(run.divide(ncycles));
            String runString;
            if ( x1.getUnits()==run.getUnits() ) {
                runString= x1.getFormatter().format(run);
            } else {
                runString= run.toString();
            }
            
            Datum y1= ya.invTransform(p2.y);
            Datum y0= ya.invTransform(p1.y);
            Datum rise= y1.subtract(y0).divide(ncycles);
            String sdiv= ncycles==1 ? "" : "/"+ncycles;

            String riseString;
            riseString= rise.toString();
            
            String radString;
            if ( rise.getUnits().isConvertibleTo(run.getUnits()) ) {
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
                
                radString= String.format( "!cR%s:%s", sdiv, radDatum );
            } else {
                radString= "";
            }
            
            String label1;
            label1= String.format( "\u0394x%s: %s \u0394y%s: %s %s", sdiv, runString, sdiv, riseString, radString );
            
            if ( showReciprocalDeltaX ) {
                label1 += "!c"+ncycles+ "/\u0394x: "+ DatumUtil.asOrderOneUnits( Units.dimensionless.createDatum(1).divide(run) );
            }
            if ( showSlope ) {
                label1 += "!cm: "+ UnitsUtil.divideToString( rise, run );
            }
            
            if ( showFit ) {
                // show y= m * ( x - x0 ) + y0
                
                Datum slope;
                Units runUnits= run.getUnits();
                if ( rise.getUnits().isConvertibleTo(runUnits ) ) {
                    slope= rise.divide(run);
                } else {
                    slope= rise.divide(run.doubleValue(runUnits));
                }
                
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
                    if ( u.isConvertibleTo(Units.seconds) ) {
                        su= UnitsUtil.divideToString( Units.dimensionless.createDatum(drise), run );
                    } else if ( u==Units.dimensionless ) {
                        su= sslope;
                    } else {
                        su= sslope + "/("+u+")";
                    }
                    
                    String sx0= x1.toString();
                    if ( UnitsUtil.isTimeLocation( x1.getUnits() ) ) {
                        fit= "!Cx0="+sx0+"!C";
                        fit+= "!Cy="+ "10!A( x-x0 )*"+su+"!n * " + y1;
                    } else {
                        fit= "!Cy="+ "10!A( x-("+x1+") )*"+su+"!n * " + y1;
                    }

                } else if ( !yaxis.isLog() && xaxis.isLog() ) {
                    NumberFormat nf= new DecimalFormat("0.00");
                    Units u = rise.getUnits();
                    double drise = y1.subtract(y0).doubleValue(u);
                    double drun = Math.log10(x1.divide(x0).doubleValue(Units.dimensionless));
                    String sslope = nf.format(drise / drun);
                    String su;
                    if ( u.isConvertibleTo(Units.seconds) ) {
                        su= UnitsUtil.divideToString( rise, Units.dimensionless.createDatum(drun) );
                    } else if ( u==Units.dimensionless ) {
                        su= sslope;
                    } else {
                        su= sslope + "("+u+")";
                    }
                    fit = "y=" + "Log( x / " + x1 + " )*" + su + " + " + y1; 
                } else {
                    NumberFormat nf= new DecimalFormat("0.00");
                    String dx= "( x - ("+x1+") )";
                    if ( runUnits!=Units.dimensionless ) {
                        if ( UnitsUtil.isTimeLocation( x1.getUnits() ) ) {
                            fit= "!Cx0="+x1+"!C";
                            dx= "( x-x0 )";
                            fit+= "!Cy="+ nf.format(slope.value()) + "/"+runUnits +" * " + dx + " + " + y1;
                        } else {
                            fit= "y="+ nf.format(slope.value()) + "/"+runUnits +" * " + dx + " + " + y1;
                        }
                        
                    } else {
                        fit= "y="+ slope + " * ( x - ("+x1+") ) + "+ y1;
                    }
                }
                label1+= "!c" + fit;
            }
            
            
            setLabel( label1 );
        } else {
            setLabel( "" );
        }
        super.renderDrag( g, p1, p2 );
        return new Rectangle[] { dirtyBounds, myDirtyBounds };
    }
    
    private boolean showReciprocalDeltaX = false;

    public static final String PROP_SHOWRECIPROCAL = "showReciprocal";

    public boolean isShowReciprocalDeltaX () {
        return showReciprocalDeltaX;
    }

    public void setShowReciprocalDeltaX(boolean showReciprocal) {
        this.showReciprocalDeltaX = showReciprocal;
    }

    /**
     * true if the slope should be shown
     */
    private boolean showSlope= false;
    
    /**
     * true if the slope should be shown
     * @return true if the slope should be shown
     */
    public boolean isShowSlope() {
        return this.showSlope;
    }
    
    /**
     * true if the slope should be shown
     * @param showSlope true if the slope should be shown
     */
    public void setShowSlope(boolean showSlope) {
        this.showSlope = showSlope;
    }
    
    protected boolean showFit = true;

    /**
     * true if the fit should be shown
     * @return true if the fit should be shown
     */
    public boolean isShowFit() {
        return showFit;
    }

    /**
     * if true then show the fit for all but xlog,ylin.
     * @param showFit if true then show the fit for all but xlog,ylin.
     */
    public void setShowFit(boolean showFit) {
        this.showFit = showFit;
    }

    /**
     * set the number to normalize the numbers by.
     * @param i 
     */
    public void setNCycles(int i) {
        this.ncycles= i;
    }
    
    /**
     * get the number to normalize the numbers by.
     * @return the number of cycles, typically 1.
     */
    public int getNCycles() {
        return this.ncycles;
    }

}
