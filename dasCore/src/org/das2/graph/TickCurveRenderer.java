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
import java.awt.image.BufferedImage;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumVector;
import org.das2.datum.DomainDivider;
import org.das2.datum.DomainDividerUtil;
import static org.das2.graph.ContoursRenderer.PROP_LINETHICK;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.JoinDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dsops.Ops;

/**
 * Renderer showing ticks along a curve, useful for orbits.  Note this renderer
 * assumes that someone else is showing the context, so just HH:MM is shown, 
 * assuming YYYY-MM-DD is shown elsewhere.
 * @author  jbf
 */
public final class TickCurveRenderer extends Renderer {
    
    TickVDescriptor tickv;
    DomainDivider ticksDivider;
    private String xplane;
    private String yplane;
    
    private QDataSet xds;
    private QDataSet yds;
    private Units xunits; // xUnits of the axis
    private Units yunits; // yUnits of the axis
    /**
     * data transformed to pixel space
     */
    private double[][] ddata; 
	
	/**
	 * current indeces--careful not thread safe.
	 */
	private final int[] index= new int[3];
        
    TickLabeller tickLabeller;
    
    private TickStyle tickStyle= TickCurveRenderer.TickStyle.outer;
    
    private double lineWidth=  1.0f;
    private Color color= Color.BLACK;
    
    private String tickLength= "0.66em";
    private double tickLen= 0; // updated with each repaint
    
    private GeneralPath path;
    
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
    
    /** 
     * Create a new renderer with the x and y planes of the bundle ds identified.  If the xplane or yplane is 
     * not identified, then use unbundle(1) for the xplane and unbundle(y) for the yplane.
     * @param ds
     * @param xplane
     * @param yplane
     * @param tickv
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
    }

    @Override
    public void setControl(String s) {
        super.setControl(s);
        this.lineWidth= getDoubleControl( PROP_LINETHICK, lineWidth );
        this.color= getColorControl( CONTROL_KEY_COLOR, color );
        this.fontSize= getControl( CONTROL_KEY_FONT_SIZE, fontSize );
        this.tickLength= getControl( "tickLength", tickLength );
        update();
    }
    
    @Override
    public String getControl() {
        Map<String,String> controls= new LinkedHashMap();
        controls.put( PROP_LINETHICK, String.valueOf(lineWidth) );
        controls.put( CONTROL_KEY_COLOR, encodeColorControl(color) );
        controls.put( CONTROL_KEY_FONT_SIZE, fontSize );
        controls.put( "tickLength", tickLength );
        return Renderer.formatControl(controls);
    }
    
    private String fontSize = "";

    public static final String PROP_FONTSIZE = "fontSize";

    public String getFontSize() {
        return fontSize;
    }

    /**
     * relative font size. 
     * @see Renderer#CONTROL_KEY_FONT_SIZE
     * @param fontSize 
     */
    public void setFontSize(String fontSize) {
        String oldFontSize = this.fontSize;
        this.fontSize = fontSize;
        propertyChangeSupport.firePropertyChange(PROP_FONTSIZE, oldFontSize, fontSize);
    }

    /**
     * return true if the data is ds[:,3] or ds[x[t]]  t&rarr;x&rarr;y
     * @param ds the dataset
     * @return true if the data is ds[:,3] or ds[x[t]]  
     */
    public static boolean acceptsData( QDataSet ds ) {
        if ( ds.rank()==2 && ds.length(0)==3 ) {
            return true;
        }
        QDataSet xx;
        QDataSet yy;
        QDataSet tt;

        if ( ds.rank()==1 ) {
            yy= ds;
            if ( yy.property(QDataSet.DEPEND_0)==null ) {
                return false;
            } else {
                return org.das2.datum.UnitsUtil.isIntervalOrRatioMeasurement( SemanticOps.getUnits(ds) );
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean acceptContext(int x, int y) {
        return selectionArea().contains(x,y);
    }

    public Shape selectionArea() {
        if ( path==null ) {
            return SelectionUtil.NULL;
        }
        Shape s = new BasicStroke( Math.min(14,1.f+8.f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ).createStrokedShape(path);
        return s;
    }
    
    @Override
    public Icon getListIcon() {
        BufferedImage img= new BufferedImage( 16, 16, BufferedImage.TYPE_INT_ARGB );
        Graphics2D g= (Graphics2D) img.getGraphics();
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        g.setColor( color );
        g.setStroke( new BasicStroke((float)lineWidth) );
        g.drawLine(2,12,14,8);
		g.drawLine(8,10,6,6);
		g.drawLine(4,2,4,2);
        return new ImageIcon( img );
    }
    
    /**
     * autorange on the data, returning a rank 2 bounds for the dataset.
     *
     * @param ds1 the dataset
     * @return rank 2 bounding box.  r[0,0] is x min, r[0,1] is x max.
     */
    public static QDataSet doAutorange( QDataSet ds1 ) {

        QDataSet ds= makeCanonical(ds1);
        QDataSet xrange= Ops.rescaleRangeLogLin( Ops.extent( DataSetOps.unbundle(ds,1) ), -0.1, 1.1 );
        QDataSet yrange= Ops.rescaleRangeLogLin( Ops.extent( DataSetOps.unbundle(ds,2) ), -0.1, 1.1 );

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
    
	/**
	 * make line segment length len, starting at line.getP1() and
	 * having the same direction.
	 * @param line the line segment, with non-zero length.
	 * @param len the new length of the line.
	 * @return the new line.
	 */
    private static Line2D normalize(Line2D line, double len) {
        Point2D p1= line.getP1();
        double dx= line.getX2()-line.getX1();
        double dy= line.getY2()-line.getY1();
        double dist= Math.sqrt( dx*dx + dy*dy );
        Line2D result= (Line2D) line.clone();
        result.setLine( p1.getX(), p1.getY(), p1.getX() + dx/dist * len, p1.getY() + dy/dist*len );
        return result;
    }
    
    /**
     * returns a positive double if turning clockwise, negative if ccw, based
     * on the cross product of the two difference vectors.
     * @param findex the floating point index
     * @return returns a positive double if turning clockwise, negative if ccw.
     */    
    private double turnDir( double x1, double y1, double x2, double y2, double x3, double y3 ) {
        double dx1= x2-x1;
        double dx2= x3-x2;
        double dy1= y2-y1;
        double dy2= y3-y2;        
        return dx1*dy2 - dx2*dy1;        
    }
    
	/**
	 * 
	 * @param findex
	 * @param points three-element array containing the points to use.
	 */
	private void id3( double findex, int[] points ) {
        int dd=4;
		int nvert= xds.length();
		int index1= (int)Math.floor(findex);
        int index0= index1-dd;
        if ( index0<0 ) index0= 0;
		while ( index0<index1 && ( ( ddata[0][index0]==-10000 ) || ( ddata[1][index0]==10000 ) ) ) {
			index0++;
		}
        int index2= index1+dd;		
        if ( index2>=nvert ) index2=nvert-1;             
		while ( index2>index1 && ( ( ddata[0][index2]==-10000 ) || ( ddata[1][index2]==10000 ) ) ) {
			index2--;
		}
        if ( index2-index1 > index1-index0 ) {
			index2= index1 + ( index1-index0 );
		}
		while ( index2>index1 && ( ( ddata[0][index2]==-10000 ) || ( ddata[1][index2]==10000 ) ) ) {
			index2--;
		}
		if ( index1-index0 > index2-index1 ) {
			index0= index1 - ( index2-index1 );
		}
		while ( index0<index1 && ( ( ddata[0][index0]==-10000 ) || ( ddata[1][index0]==10000 ) ) ) {
			index0++;
		}
		if ( index2-index0<2 && index0>1 && Math.abs( ddata[0][index2-2])<10000 ) {
			index0= index2-2;
			index1= index2-1;
		}
		if ( index2-index0<2 && index2<(nvert-2) && Math.abs( ddata[0][index2+2])<10000 ) {
			index1= index0+1;
			index2= index0+2;
		}
		points[0]= index0;
		points[1]= index1;
		points[2]= index2;
	}
	
    /**
     * returns a positive double if turning clockwise, negative if ccw.
     * @param findex the floating point index
     * @return returns a positive double if turning clockwise, negative if ccw.
     */
    private double turnDirAt( double findex ) {
        int nvert= xds.length();
        if ( nvert<3 ) {
            return 0;
        }
		id3( findex, index );
        return turnDir( ddata[0][index[0]], ddata[1][index[0]],
                        ddata[0][index[1]], ddata[1][index[1]],
                        ddata[0][index[2]], ddata[1][index[2]] );
    }

    /**
     * return the outsize normal (length 1) line segment.  When the findex
     * is at two points that repeat, then an exception is thrown.
     * @param findex
     * @return return the outsize normal
     * @throws IllegalArgumentException when points repeat.
     */
    private Line2D outsideNormalAt( double findex ) {

		id3( findex, index );
		
		double x0= ddata[0][index[0]];
        double x2= ddata[0][index[2]];
        double y0= ddata[1][index[0]];
        double y2= ddata[1][index[2]];

        double xinterp= DasMath.interpolate( ddata[0], findex );
        double yinterp= DasMath.interpolate( ddata[1], findex );
        
        double dx= x2-x0;
        double dy= y2-y0;
        
        if ( dx==0. && dy==0. ) throw new IllegalArgumentException("findex as at a point that repeats");
        
        double turnDir= turnDirAt(findex);
        // we want the turnDir of the tick to be opposite turnDir of the curve
        
        double dxNorm= dy;
        double dyNorm= -dx;
        
        double turnDirTick= -1*(dx*dyNorm-dxNorm*dy);        
        
        if ( turnDir*turnDirTick < 0 ) {  // this was determined experimentally.
            dxNorm= -dy;
            dyNorm= dx;
        }
                        
        return normalize( new Line2D.Double(xinterp, yinterp, xinterp+dxNorm,yinterp+dyNorm ), 1. ) ;

    }
    
    private void updateTickLength( Graphics2D g ) {
        try {
            double[] pos = DasDevicePosition.parseLayoutStr(this.tickLength );
            Font f= g.getFont();
            if ( pos[0]==0 ) {
                this.tickLen = (int) ( Math.round( pos[1]* f.getSize2D() + pos[2] ) ); // make independent from row layout for initialization.
            } else {
                this.tickLen = (int) ( Math.round( pos[1]* f.getSize2D() + pos[2] ) );
            }
        } catch ( ParseException ex ) {
            ex.printStackTrace();
        }
    }
    
    private void drawTick( Graphics2D g, double findex ) {  
        
        float tl= (float)(tickLen*0.66);
        Line2D tick;
        try {
            tick= normalize( outsideNormalAt( findex ), tl );        
        } catch ( IllegalArgumentException ex ) {
            return;
        }

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
        float tl= (float)tickLen;
        if ( tl<0.001 ) tl= 0.001f;

        Line2D tick;
        
        try {
            tick= normalize( outsideNormalAt( findex ), tl );
        } catch ( IllegalArgumentException ex ) {
            return;
        }

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

		tick= normalize( tick, tl + lineWidth );
				
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
        for ( int i=1; i<ddata[0].length; i++ ) {
            if ( Math.abs( ddata[0][i] )<10000 && Math.abs( ddata[0][i-1] )<10000 &&  Math.abs( ddata[1][i] )<10000 && Math.abs( ddata[1][i-1] )<10000 ) {
                double dx= ddata[0][i] - ddata[0][i-1];
                double dy= ddata[1][i] - ddata[1][i-1];
                plen += Math.sqrt( dx*dx + dy*dy );
                ic++;
            }
        }
        
        if ( ic>0 ) { // compensate for stuff that can't be transformed.
            plen= plen * ddata[0].length / ic;
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
            if ( index0>=0 && index0<ddata[0].length ) {
                if ( x0==Double.MAX_VALUE ) {
                    x0= ddata[0][index0];
                    y0= ddata[1][index0];
                } else {
                    double x1= ddata[0][index0];
                    double y1= ddata[1][index0];
                    double r= (x1-x0)*(x1-x0) + (y1-y0)*(y1-y0);
                    if ( r<rmin ) rmin= Math.sqrt(r);
                    x0= x1;
                    y0= y1;
                }
            }
        }
        return rmin;

    }

    @Override
    public void render(java.awt.Graphics g1, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {
        
        if ( ds==null ) {
            return;
        }

        QDataSet ds2= makeCanonical(ds);

        if ( ds2.length()<2 ) {
            return;
        }

        Graphics2D g= (Graphics2D)g1;
        
        BasicStroke stroke= new BasicStroke( (float)lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND );
        g.setStroke( stroke );

        g.setFont( getParent().getFont() );
        
        g.setColor( color );
        
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
        
        // there are two goals here.  First is to break the line when we cross over modulo spaces.  If
        // we move from 23:59 to 00:01 in local time, we don't want a long line across the plot.  Second,
        // if there's an actual gap, then we want to mark that as well.
        double limit= -1;                   // length limit it pixels
        ddata= new double[2][xds.length()]; // data location in pixel space
        for ( int i=0; i<xds.length(); i++ ) {
            ddata[0][i]= xAxis.transform(xds.value(i),xunits);
            ddata[1][i]= yAxis.transform(yds.value(i),yunits);
            if ( i>0 ) {
                double len1=  Math.sqrt( Math.pow(ddata[0][i]- ddata[0][i-1],2 ) 
                        + Math.pow(ddata[1][i]- ddata[1][i-1],2 ) );
                if ( limit==-1 || ( len1>limit && len1/limit < 10 ) ) { // if each length is within 10 times the previous
                    limit= len1;
                }
                
            }
        }

        if ( limit==0 ) limit= 10000;  // we failed to find two valid adjacent points.
        
        QDataSet wds= Ops.multiply( Ops.valid(xds), Ops.valid(yds) );
        
        GeneralPath p= new GeneralPath();
        p.moveTo( ddata[0][0],ddata[1][0] );
        
        double w0= wds.value(0);
        
        //PrintStream fo=null;
        //try {
        //    fo= new PrintStream( "/tmp/foo.txt" );
        //} catch (Exception ex) {
        //    Logger.getLogger(TickCurveRenderer.class.getName()).log(Level.SEVERE, null, ex);
        //}
        
        for ( int i=1; i<xds.length(); i++ ) {
            double w1= wds.value(i);
            double len1=  Math.sqrt( Math.pow(ddata[0][i]- ddata[0][i-1],2 ) 
                        + Math.pow(ddata[1][i]- ddata[1][i-1],2 ) );
        //    fo.println(""+i+" "+len1+" " + limit);
            if ( len1>limit ) {
                p.moveTo( ddata[0][i],ddata[1][i] );
            } else {
                if ( w0==0 || w1==0 ) {
                    p.moveTo( ddata[0][i],ddata[1][i] );
                } else {
                    p.lineTo( ddata[0][i],ddata[1][i] );
                }
            }
            w0= w1;
        }
        //fo.close();
        GeneralPath rp= new GeneralPath();
        GraphUtil.reducePath( p.getPathIterator(null), rp, 2 );
        g.draw(rp);
        
        path= rp;
        
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

        setUpFont( g, fontSize );
        
        updateTickLength( g );
        
        tickLabeller= new GrannyTickLabeller( ); 
        tickLabeller.init( tickv );
        
        for ( int i=0; i<tickv.minorTickV.getLength(); i++ ) {
            if ( findex.value(i)>=0 && findex.value(i)<xds.length()-1.0 ) {
                drawTick( g, findex.value(i) );
            }
        }

        txds= DDataSet.wrap( tickv.tickV.toDoubleArray( tunits ), tunits );
        findex= Ops.findex( tds, txds );
        for ( int i=0; i<tickv.tickV.getLength(); i++ ) {            
            if ( findex.value(i)>=0 && findex.value(i)<xds.length()-1.0 ) {
                drawLabelTick( g, findex.value(i), i );
            }
        }

        int n= ddata[0].length;
		int index1= n-1;
		int index2= n-3;
		if ( Math.pow(ddata[0][index1]-ddata[0][index2],2)  + Math.pow((ddata[1][index1]-ddata[1][index2]), 2 ) > 2 ) {
			index2= n-2;
		}
        int em= 10;
        Arrow.paintArrow( g,
                new Point2D.Double( ddata[0][index1],ddata[1][index1] ),
                new Point2D.Double( ddata[0][index2],ddata[1][index2] ), em, Arrow.HeadStyle.DRAFTING );
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
        invalidateParentCacheImage();
    }
    
    /**
     * get the color of the orbit
     * @return the color
     */
    public Color getColor() {
        return this.color;
    }
    
    /**
     * set the color of the orbit
     * @param color
     */
    public void setColor( Color color ) {
        this.color= color;
        invalidateParentCacheImage();
    }
    
    
    
    /** Getter for property tickLength.
     * @return Value of property tickLength.
     *
     */
    public String getTickLength() {
        return this.tickLength;
    }
    
    /** Setter for property tickLength.
     * @param tickLength New value of property tickLength.
     *
     */
    public void setTickLength( String tickLength) {
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
