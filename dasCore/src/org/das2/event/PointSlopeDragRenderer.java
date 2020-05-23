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
import org.das2.datum.DatumUtil;
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.das2.graph.DasPlot;

/**
 * Shows the slope from the click point to the drag point.
 * @author  Jeremy
 */
public class PointSlopeDragRenderer extends LabelDragRenderer {
    
    
    DasAxis xaxis, yaxis;    
    NumberFormat nf;
    
    /** Creates a new instance of PointSlopeDragRenderer
     * @param parent
     * @param xaxis
     * @param yaxis 
     */
    public PointSlopeDragRenderer(DasCanvasComponent parent, DasAxis xaxis, DasAxis yaxis ) {
        super( parent );
        
        if ( parent instanceof DasPlot ) {
            DasPlot plot= (DasPlot)parent;
            this.xaxis = xaxis==null ? plot.getXAxis() : xaxis;
            this.yaxis = yaxis==null ? plot.getYAxis() : yaxis;
        } else {
            this.xaxis= xaxis;
            this.yaxis= yaxis;    
        }
        
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
        g1.setColor(g.getBackground());
        g1.fillOval(p1.x-2, p1.y-2, 4, 4 ) ;
        g1.setColor(color0);
        g1.drawOval(p1.x-2, p1.y-2, 4, 4 ) ;
        
        Rectangle myDirtyBounds= new Rectangle( p1.x-2, p1.y-2, 5, 5 );
                
        myDirtyBounds.add(p2.x-2,p2.y-2);
        myDirtyBounds.add(p2.x+2,p2.y+2);
        
        Datum x1= xaxis.invTransform(p2.x);
        Datum x0= xaxis.invTransform(p1.x);
        Datum run= x1.subtract(x0);

        Datum y1= yaxis.invTransform(p2.y);
        Datum y0= yaxis.invTransform(p1.y);
        Datum rise= y1.subtract(y0);
            
        Datum xdr= xaxis.getDatumRange().width();
        Datum ydr= yaxis.getDatumRange().width();
        
        Units xunits= DatumUtil.asOrderOneUnits(xdr).getUnits();
        Units yunits= DatumUtil.asOrderOneUnits(ydr).getUnits();
        run= run.convertTo(xunits);
        rise= rise.convertTo(yunits);
        
        run= DatumUtil.asOrderOneUnits(run);
        
        Units runUnits= run.getUnits();
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
            NumberFormat nf1= new DecimalFormat("0.00");
            Units u = rise.getUnits();
            double drise = y1.subtract(y0).doubleValue(u);
            double drun = Math.log10(x1.divide(x0).doubleValue(Units.dimensionless));
            String sslope = nf1.format(drise / drun);
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
            Datum slope;
            
            if ( rise.getUnits().isConvertibleTo(runUnits ) ) {
                slope= rise.divide(run);
            } else {
                slope= rise.divide(run.doubleValue(runUnits));
            }
            
            NumberFormat nf1= new DecimalFormat("0.00");
            String dx= "( x - ("+x1+") )";
            if ( runUnits!=Units.dimensionless ) {
                if ( UnitsUtil.isTimeLocation( x1.getUnits() ) ) {
                    fit= "!Cx0="+x1+"!C";
                    dx= "( x-x0 )";
                    fit+= "!Cy="+ nf1.format(slope.value()) + "/"+runUnits +" * " + dx + " + " + y1;
                } else {
                    fit= "y="+ nf1.format(slope.value()) + "/"+runUnits +" * " + dx + " + " + y1;
                }

            } else {
                
                if ( rise.getUnits().isConvertibleTo(runUnits ) ) {
                    slope= rise.divide(run);
                } else {
                    slope= rise.divide(run.doubleValue(runUnits));
                }
                fit= "y="+ slope + " * ( x - ("+x1+") ) + "+ y1;
            }
        }


        if ( xaxis.isLog() ) {
            setLabel( fit );
        } else if ( yaxis.isLog() ) {
            setLabel( fit );
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
