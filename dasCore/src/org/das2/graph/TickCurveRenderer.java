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

package org.das2.graph;

import org.das2.datum.Units;
import org.das2.util.DasMath;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.components.propertyeditor.Enumeration;
import java.awt.*;
import java.awt.geom.*;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumVector;
import org.das2.datum.DomainDivider;
import org.das2.datum.DomainDividerUtil;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.JoinDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dsops.Ops;

/**
 *
 * @author  jbf
 */
public class TickCurveRenderer extends Renderer {
    
    private Stroke stroke;
    
    TickVDescriptor tickv;
    DomainDivider ticksDivider;
    private String xplane;
    private String yplane;
    
    private QDataSet xds;
    private QDataSet yds;
    private Units xunits; // xUnits of the axis
    private Units yunits; // yUnits of the axis
    private double[][] idata;  // data transformed to pixel space
    
    TickLabeller tickLabeller;
    
    private TickStyle tickStyle= TickCurveRenderer.TickStyle.outer;
    
    private double lineWidth=  1.0f;
    
    private float tickLength= 8.0f;
    
    public static class TickStyle implements Enumeration {
        private String name;
        public static final TickStyle outer= new TickStyle("Outer");
        public static final TickStyle both= new TickStyle("Both");
        private TickStyle(String name) {
            this.name= name;
        }
        @Override
        public String toString() {
            return this.name;
        }
        public javax.swing.Icon getListIcon() {
            return null;
        }
        
    }
    
    /** The dataset be a Vector data set with planes identified
     *  by xplane and yplane.
     */
    public TickCurveRenderer( QDataSet ds, String xplane, String yplane, TickVDescriptor tickv) {
        this();
        this.setDataSet(ds);
        this.xplane= xplane;
        this.yplane= yplane;
        this.tickv= tickv;                
    }

    public TickCurveRenderer() {
        super();
        stroke= new BasicStroke((float)lineWidth);
    }

    /**
     * autorange on the data, returning a rank 2 bounds for the dataset.
     *
     * @param fillDs
     * @return
     */
    public static QDataSet doAutorange( QDataSet ds1 ) {

        QDataSet ds= makeCanonical(ds1);
        QDataSet xrange= Ops.rescaleRange( Ops.extent( DataSetOps.unbundle(ds,1) ), -0.1, 1.1 );
        QDataSet yrange= Ops.rescaleRange( Ops.extent( DataSetOps.unbundle(ds,2) ), -0.1, 1.1 );

        JoinDataSet bds= new JoinDataSet(2);
        bds.join(xrange);
        bds.join(yrange);

        return bds;

    }

    static QDataSet makeCanonical( QDataSet ds ) {
        if ( ds.rank()==2 && ds.length(0)==3 ) {
            return ds;
        }
        QDataSet xx;
        QDataSet yy;
        QDataSet tt;

        if ( ds.rank()==1 ) {
            yy= ds;
            xx= SemanticOps.xtagsDataSet(yy);
            tt= SemanticOps.xtagsDataSet(xx);
            return Ops.bundle( tt, xx, yy );
        } else {
            throw new IllegalArgumentException("dataset must be rank2[3,n] or rank 1 ds[xx[tt]]");
        }
    }

//    private static double length( Line2D line ) {
//        double dx= line.getX2()-line.getX1();
//        double dy= line.getY2()-line.getY1();
//        double dist= Math.sqrt( dx*dx + dy*dy );
//        return dist;
//    }
    
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
        int nvert= xds.length();
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
                    
        return turnDir( xds.value(index0), yds.value(index0),
                        xds.value(index1), yds.value(index1),
                        xds.value(index2), yds.value(index2) );
    }
    
    private Line2D outsideNormalAt( double findex ) {
        int nvert= xds.length();
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
                        
        return normalize( new Line2D.Double(xinterp, yinterp, xinterp+dxNorm,yinterp+dyNorm ), 1. ) ;

    }
    
    private void drawTick( Graphics2D g, double findex ) {  
        float tl= getTickLength()*2/3;
        Line2D tick= normalize( outsideNormalAt( findex ), tl );        

        if ( tick.getP1().getX() < -1000 || tick.getP1().getY()<-1000 || tick.getP1().getX()> 9999 || tick.getP1().getY()> 9999 ) {
            return;
        }

        if ( tickStyle==TickStyle.both ) {
            Line2D flipTick= normalize( tick, -tl );
            Line2D bothTick= new Line2D.Double( flipTick.getP2(), tick.getP2() );
            g.draw( bothTick );            
        } else {
            g.draw( tick );
        }
    }
    
    //private double slope( Line2D line ) {
    //    return ( line.getY2()-line.getY1() ) / ( line.getX2()-line.getX1() );
    //}
    
    private void drawLabelTick( Graphics2D g, double findex, int tickNumber ) {        
        float tl= getTickLength();
        Line2D tick= normalize( outsideNormalAt( findex ), tl );

        if ( tick.getP1().getX() <-1000 || tick.getP1().getY()<-1000 || tick.getP1().getX()> 9999 || tick.getP1().getY()> 9999 ) {
            return;
        }

        if ( tickStyle==TickStyle.both ) {
            Line2D flipTick= normalize( tick, -tl );
            Line2D bothTick= new Line2D.Double( flipTick.getP2(), tick.getP2() );
            g.draw( bothTick );            
        } else {
            g.draw( tick );
        }


        tickLabeller.labelMajorTick( g, tickNumber, tick );
        
    }

    private TickVDescriptor resetTickV( QDataSet tds ) {
        QDataSet trange= Ops.extent(tds);
        DatumRange dr= DataSetUtil.asDatumRange( trange, true );

        if ( ticksDivider==null ) {
             ticksDivider= DomainDividerUtil.getDomainDivider( dr.min(), dr.max(), false );
        }
        
        double plen= 0;
        int ic= 1;
        for ( int i=1; i<idata[0].length; i++ ) {
            if ( Math.abs( idata[0][i] )<10000 && Math.abs( idata[0][i-1] )<10000 &&  Math.abs( idata[1][i] )<10000 && Math.abs( idata[1][i-1] )<10000 ) {
                double dx= idata[0][i] - idata[0][i-1];
                double dy= idata[1][i] - idata[1][i-1];
                plen += Math.sqrt( dx*dx + dy*dy );
                ic++;
            }
        }
        
        if ( ic>0 ) { // compensate for stuff that can't be transformed.
            plen= plen * idata[0].length / ic;
        }
        if ( plen<100 ) plen=100;

        while ( ticksDivider.boundaryCount( dr.min(), dr.max() ) < Math.ceil( plen / 100 ) ) {
            ticksDivider= ticksDivider.finerDivider(false);
        }
        while ( ticksDivider.boundaryCount( dr.min(), dr.max() ) > Math.ceil( plen / 50 ) ) {
            ticksDivider= ticksDivider.coarserDivider(false);
        }

        DatumVector major = ticksDivider.boundaries(dr.min(), dr.max());
        DatumVector minor = ticksDivider.finerDivider(true).boundaries(dr.min(), dr.max());
        tickv = TickVDescriptor.newTickVDescriptor(major, minor);
        tickv.datumFormatter= DomainDividerUtil.getDatumFormatter( ticksDivider, dr );

        return tickv;
    }

    /** 
     * returns the minimal distance between consecutive ticks, or Double.MAX_VALUE if fewer than two ticks are found.
     * @param tds
     * @return
     */
    public double checkTickV( QDataSet tds ) {

        Units tunits= SemanticOps.getUnits(tds);
        
        DDataSet txds= DDataSet.wrap( tickv.tickV.toDoubleArray( tunits ), tunits );
        QDataSet findex= Ops.findex( tds, txds );
        double rmin= Double.MAX_VALUE;
        double x0= Double.MAX_VALUE,y0= Double.MAX_VALUE;

        for ( int i=0; i<tickv.tickV.getLength(); i++ ) {
            int index0= (int)Math.floor(findex.value(i));
            if ( index0>=0 && index0<idata[0].length ) {
                if ( x0==Double.MAX_VALUE ) {
                    x0= idata[0][index0];
                    y0= idata[1][index0];
                } else {
                    double x1= idata[0][index0];
                    double y1= idata[1][index0];
                    double r= (x1-x0)*(x1-x0) + (y1-y0)*(y1-y0);
                    if ( r<rmin ) rmin= Math.sqrt(r);
                    x0= x1;
                    y0= y1;
                }
            }
        }
        return rmin;

    }

    public void render(java.awt.Graphics g1, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {
        
        if ( ds==null ) {
            return;
        }

        QDataSet ds2= makeCanonical(ds);

        if ( ds2.length()<2 ) {
            return;
        }
        
        Graphics2D g= (Graphics2D)g1;
        g.setStroke( stroke );

        g.setFont( parent.getFont() );
        
        g.setColor( Color.black );
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

        QDataSet ds3= ds2;
        
        if ( xplane!=null && !xplane.equals("") ) {
            xds= DataSetOps.unbundle( ds3, xplane );
        } else {
            xds= DataSetOps.unbundle( ds3, 1 );
        }
        if ( yplane!=null && !yplane.equals("") ) {
            yds= DataSetOps.unbundle( ds3, yplane );
        } else {
            yds= DataSetOps.unbundle( ds3, 2 );
        }

        QDataSet tds;
        tds= (QDataSet) xds.property(QDataSet.DEPEND_0);
        if ( tds==null ) {
            tds= DataSetOps.unbundle( ds3, 0 );
        }


        xunits= SemanticOps.getUnits(xds);
        yunits= SemanticOps.getUnits(yds);
        
        idata= new double[2][xds.length()];
        for ( int i=0; i<xds.length(); i++ ) {
            idata[0][i]= xAxis.transform(xds.value(i),xunits);
            idata[1][i]= yAxis.transform(yds.value(i),yunits);
        }


        GeneralPath p= new GeneralPath();
        p.moveTo( (float)idata[0][0],(float)idata[1][0] ); // Java5 requires floats
        for ( int i=1; i<xds.length(); i++ ) {
            p.lineTo( (float)idata[0][i],(float)idata[1][i] );
        }
        GeneralPath rp= new GeneralPath();
        GraphUtil.reducePath( p.getPathIterator(null), rp, 2 );
        g.draw(rp);
        
        
        QDataSet findex;
        Units tunits= SemanticOps.getUnits(tds);

        if ( tickv==null ) {
            tickv= resetTickV( tds );
        } else {
            double check= checkTickV(tds);
            if ( check<30 ) {
                tickv= resetTickV( tds );
            } else if ( check>100 ) {
                tickv= resetTickV( tds );
            }
        }
        
        DDataSet txds= DDataSet.wrap( tickv.minorTickV.toDoubleArray( tunits ), tunits );
        findex= Ops.findex( tds, txds );

        tickLabeller= new GrannyTickLabeller( ); 
        tickLabeller.init( tickv );
        
        for ( int i=0; i<tickv.minorTickV.getLength(); i++ ) {
            if ( findex.value(i)>=0 && findex.value(i)<xds.length() ) {
                drawTick( g, findex.value(i) );
            }
        }

        txds= DDataSet.wrap( tickv.tickV.toDoubleArray( tunits ), tunits );
        findex= Ops.findex( tds, txds );
        for ( int i=0; i<tickv.tickV.getLength(); i++ ) {            
            if ( findex.value(i)>=0 && findex.value(i)<xds.length() ) {
                drawLabelTick( g, findex.value(i), i );
            }
        }

        int n= idata[0].length;
        int em= 10;
        Arrow.paintArrow( g,
                new Point2D.Double( idata[0][n-1],idata[1][n-1] ),
                new Point2D.Double( idata[0][n-3],idata[1][n-3] ), em, Arrow.HeadStyle.FAT_TRIANGLE );
        tickLabeller.finished();
        
    }
    
//    private static String lineToString( Line2D line ) {
//        return GraphUtil.toString( line );
//    }
    
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
        invalidateParentCacheImage();
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
        invalidateParentCacheImage();
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
        invalidateParentCacheImage();
    }

    /**
     * set the ticks for the renderer.
     */
    public void setTickVDescriptor(TickVDescriptor ticks) {
        this.tickv= ticks;
        this.invalidateParentCacheImage();
    }
    
}
