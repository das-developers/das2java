/* File: TickCurveRenderer.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on November 3, 2003, 11:43 AM by __FULLNAME__ <__EMAIL__>
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

package edu.uiowa.physics.pw.das.graph;

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.components.*;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.util.*;
import java.awt.*;
import java.awt.geom.*;

/**
 *
 * @author  jbf
 */
public class TickCurveRenderer extends Renderer {
    
    private Stroke stroke;
    
    TickVDescriptor tickv;
    private String xplane;
    private String yplane;
    
    private VectorDataSet xds;
    private VectorDataSet yds;
    private Units xunits; // xUnits of the axis
    private Units yunits; // yUnits of the axis
    private double[][] idata;  // data transformed to pixel space
    
    /** Holds value of property tickStyle. */
    private TickStyle tickStyle;
    
    /** Holds value of property lineWidth. */
    private double lineWidth;
    
    /** Holds value of property tickLength. */
    private float tickLength;
    
    public static class TickStyle implements PropertyEditor.Enumeration {
        private String name;
        public static TickStyle outer= new TickStyle("Outer");
        public static TickStyle both= new TickStyle("Both");        
        private TickStyle(String name) {
            this.name= name;
        }
        public String toString() {
            return this.name;
        }
        public javax.swing.Icon getListIcon() {
            return null;
        }
        
    }
    
    /** The dataset descriptor should return a Vector data set with planes identified
     *  by xplane and yplane.
     */
    public TickCurveRenderer(DataSetDescriptor dsd, String xplane, String yplane, TickVDescriptor tickv) {
        super(dsd);        
    
        setTickStyle( TickCurveRenderer.TickStyle.outer );
        setLineWidth( 1.0f );
        setTickLength( 8.0f );
        this.xplane= xplane;
        this.yplane= yplane;
        this.tickv= tickv;                
    }
    
    protected void uninstallRenderer() {
    }
    
    protected void installRenderer() {
    }
    
    private static double length( Line2D line ) {
        double dx= line.getX2()-line.getX1();
        double dy= line.getY2()-line.getY1();
        double dist= Math.sqrt( dx*dx + dy*dy );
        return dist;
    }
    
    private static Line2D normalize(Line2D line, double len) {
        // make line segment length len, starting at line.getP1()
        Point2D p1= line.getP1();
        double dx= line.getX2()-line.getX1();
        double dy= line.getY2()-line.getY1();
        double dist= Math.sqrt( dx*dx + dy*dy );
        Line2D result= (Line2D) line.clone();
        result.setLine( p1.getX(), p1.getY(), p1.getX() + dx/dist * len, p1.getY() + dy/dist*len );
        return result;
    }
    
    private double turnDir( double x1, double y1, double x2, double y2, double x3, double y3 ) {
        // returns positive double if turning clockwise, negative is ccw.  Number is 
        //  based on the cross product of the two difference vectors.
        double dx1= x2-x1;
        double dx2= x3-x2;
        double dy1= y2-y1;
        double dy2= y3-y2;        
        return dx1*dy2 - dx2*dy1;        
    }
    
    private double turnDirAt( double findex ) {
        int nvert= xds.getXLength();
        int index0, index1, index2;
        if ( findex<1 ) {
            index0= 0;
        } else if ( findex>nvert-2 ) {
            index0= nvert-3;
        } else {
            index0= (int)Math.floor(findex)-1;
        }
        index1= index0+1;
        index2= index1+1;            
                    
        return turnDir( xds.getDouble(index0,xunits), yds.getDouble(index0,yunits),
                        xds.getDouble(index1,xunits), yds.getDouble(index1,yunits),
                        xds.getDouble(index2,xunits), yds.getDouble(index2,yunits) ); 
    }
    
    private Line2D outsideNormalAt( double findex ) {
        int nvert= xds.getXLength();
        int index0= (int)Math.floor(findex);
        if ( index0==nvert-1 ) index0--;            
        double x1= idata[0][index0];
        double x2= idata[0][index0+1];
        double y1= idata[1][index0];
        double y2= idata[1][index0+1];

        double xinterp= DasMath.interpolate( idata[0], findex );
        double yinterp= DasMath.interpolate( idata[1], findex );
        
        double dx= x2-x1;
        double dy= y2-y1;
        
        double turnDir= turnDirAt(findex);
        // we want the turnDir of the tick to be opposite turnDir of the curve
        
        double dxNorm= dy;
        double dyNorm= -dx;
        
        double turnDirTick= -1*(dx*dyNorm-dxNorm*dy);        
        
        if ( turnDir*turnDirTick > 0 ) {  // same sign, use the other perp direction.
            dxNorm= -dy;
            dyNorm= dx;
        }
                        
        Line2D normal;

        return normalize( new Line2D.Double(xinterp, yinterp, xinterp+dxNorm,yinterp+dyNorm ), 1. ) ;

    }
    
    private void drawTick( Graphics2D g, double findex ) {  
        float tl= getTickLength()*2/3;
        Line2D tick= normalize( outsideNormalAt( findex ), tl );        
        g.draw( tick );
        if ( tickStyle==TickStyle.both ) {
            g.draw( normalize( tick, -tl ) );
        }
    }
    
    private double slope( Line2D line ) {
        return ( line.getY2()-line.getY1() ) / ( line.getX2()-line.getX1() );
    }
    
    private void drawLabelTick( Graphics2D g, double findex, String label ) {        
        float tl= getTickLength();
        Line2D tick= normalize( outsideNormalAt( findex ), tl );
        g.draw( tick );
        if ( tickStyle==TickStyle.both ) {
            g.draw( normalize( tick, -tl ) );
        }

        // the goal is to position the label such that the tick would intersect the label's center of mass.
        double tickSlope= slope(tick);
        double labelWidth= g.getFontMetrics().stringWidth(label);
        double labelHeight= g.getFontMetrics().getAscent();        
        double labelSlope= labelHeight / labelWidth;
        
        double labelX;
        double labelY;
        
        tick= normalize( tick, length(tick)+2 ); // add 2 pixels space to end of tick
        
        if ( labelSlope > Math.abs( tickSlope ) ) { // tick intersects the height of the label bounds.
            if ( tick.getX2()>tick.getX1() ) {  // e.g. 3 O'Clock
                double rise= tickSlope * labelWidth / 2;
                labelX= tick.getX2();
                labelY= tick.getY2() + labelHeight/2 + rise;
            } else { // e.g. 9 O'Clock                
                double rise= - tickSlope * labelWidth / 2;
                labelX= tick.getX2() - labelWidth;
                labelY= tick.getY2() + labelHeight/2 + rise;
            }
        } else { // tick intersects the width of the label bounds
            if ( tick.getY2()<tick.getY1() ) {  // e.g. 12 O'Clock
                double run= - labelHeight / tickSlope / 2;
                labelX= tick.getX2() + run - labelWidth/2;
                labelY= tick.getY2();
            } else { // e.g. 6 O'Clock
                double run= labelHeight / tickSlope / 2;
                labelX= tick.getX2() + run - labelWidth/2;
                labelY= tick.getY2() + labelHeight;
            }   
        }
        
        g.drawString( label, (float)labelX, (float)labelY );
        
    }

    public void render(java.awt.Graphics g1, DasAxis xAxis, DasAxis yAxis) {
        Graphics2D g= (Graphics2D)g1;
        g.setStroke( stroke );
        
        DataSet ds= getDataSet();
        xds= (VectorDataSet) ds.getPlanarView(xplane);
        yds= (VectorDataSet) ds.getPlanarView(yplane);
        xunits= xds.getYUnits();
        yunits= yds.getYUnits();
        
        idata= new double[2][xds.getXLength()];
        for ( int i=0; i<xds.getXLength(); i++ ) {
            idata[0][i]= xAxis.transform(xds.getDouble(i,xunits),xunits);
            idata[1][i]= yAxis.transform(yds.getDouble(i,yunits),yunits);
        }
        
        for ( int i=1; i<xds.getXLength(); i++ ) {            
            g.drawLine((int)idata[0][i-1],(int)idata[1][i-1],(int)idata[0][i],(int)idata[1][i]);            
        }
        
        double[] findex;
        findex= DasMath.findex( VectorUtil.getXTagArrayDouble(xds,xds.getXUnits()), tickv.minorTickV );

        for ( int i=0; i<tickv.minorTickV.length; i++ ) {
            drawTick( g, findex[i] );
        }

        findex= DasMath.findex( VectorUtil.getXTagArrayDouble(xds,xds.getXUnits()), tickv.tickV );
        for ( int i=0; i<tickv.tickV.length; i++ ) {
            String label= tickv.units.createDatum(tickv.tickV[i]).toString();
            drawLabelTick( g, findex[i], label );            
        }
        
    }
    
    public void updatePlotImage(DasAxis xAxis, DasAxis yAxis) {
    }
    
    private static String lineToString( Line2D line ) {
        return ""+line.getX1()+" "+line.getY1()+" "+line.getX2()+" "+line.getY2();
    }
    
    /** Getter for property tickStyle.
     * @return Value of property tickStyle.
     *
     */
    public TickStyle getTickStyle() {
        return this.tickStyle;
    }    
    
    /** Setter for property tickStyle.
     * @param tickStyle New value of property tickStyle.
     *
     */
    public void setTickStyle(TickStyle tickStyle) {
        this.tickStyle = tickStyle;
    }
    
    /** Getter for property lineWidth.
     * @return Value of property lineWidth.
     *
     */
    public double getLineWidth() {
        return this.lineWidth;
    }
    
    /** Setter for property lineWidth.
     * @param lineWidth New value of property lineWidth.
     *
     */
    public void setLineWidth(double lineWidth) {
        this.lineWidth = lineWidth;
        stroke= new BasicStroke((float)lineWidth);
    }
    
    /** Getter for property tickLength.
     * @return Value of property tickLength.
     *
     */
    public float getTickLength() {
        return this.tickLength;
    }
    
    /** Setter for property tickLength.
     * @param tickLength New value of property tickLength.
     *
     */
    public void setTickLength(float tickLength) {
        this.tickLength = tickLength;
    }
    
    public DataSet getDataSet() {
        DataSetDescriptor dsd= getDataSetDescriptor();
	if ( ! ( dsd instanceof ConstantDataSetDescriptor )) {
            throw new IllegalStateException( "only ConstantDataSetDescriptors for now!" );
        } else {
            try {
                return dsd.getDataSet(null, null, null, null );
            } catch ( DasException e ) {
                throw new RuntimeException(e);
            }
        }        
    }
    
}
