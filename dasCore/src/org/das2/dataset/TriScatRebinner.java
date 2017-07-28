/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.dataset;

import ProGAL.geom2d.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.das2.datum.Datum;
import org.das2.datum.Units;
import org.das2.qds.DataSetOps;
import org.das2.qds.QDataSet;
import org.das2.qds.WritableDataSet;
import org.das2.qds.examples.Schemes;
import org.das2.qds.ops.Ops;
import org.das2.util.LoggerManager;

/**
 *
 * @author jbf
 */
public class TriScatRebinner implements DataSetRebinner {

    private static final Logger logger = LoggerManager.getLogger("das2.data.rebinner");

    /**
     * return the volume of the 3-point triangle in 2 dimensions.
     * See http://www.mathopenref.com/coordtrianglearea.html
     * @param a 2-d point corner
     * @param b 2-d point corner
     * @param c 2-d point corner
     * @return the volume
     */
    private static double area( ProGAL.geom2d.Point a, ProGAL.geom2d.Point b, ProGAL.geom2d.Point c ) {        
        return Math.abs( a.x() * ( b.y()- c.y() ) +
                b.x() * ( c.y() - a.y() ) +
                c.x() * ( a.y() - b.y() ) ) / 2;
    }
        
    /**
     * Point with placeholder for index.
     */
    private static class VertexInt extends ProGAL.geom2d.delaunay.Vertex {
        int idx;
        VertexInt( double x, double y, int idx ) {
            super( x,y );
            this.idx= idx;
        }
        @Override
        public String toString() {
            return String.valueOf(idx);
        }
    }    
    
    private java.awt.geom.Rectangle2D getBounds( ProGAL.geom2d.delaunay.Triangle t ) {
        Rectangle2D.Double r= new Rectangle2D.Double( t.getCorner(0).x(), t.getCorner(0).y(), 0, 0 );
        
        r.add( new java.awt.geom.Point2D.Double( t.getCorner(1).x(), t.getCorner(1).y() ));
        r.add( new java.awt.geom.Point2D.Double( t.getCorner(2).x(), t.getCorner(2).y() ));
        
        return r;
    }
    
    @Override
    public QDataSet rebin(QDataSet zz, RebinDescriptor rebinDescX, RebinDescriptor rebinDescY) {
        // throws IllegalArgumentException, DasException {

        LoggerManager.resetTimer("triscat rebin");
                
        WritableDataSet result = Ops.zeros(rebinDescX.numberOfBins(), rebinDescY.numberOfBins());

        rebinDescX.setOutOfBoundsAction(RebinDescriptor.MINUSONE);
        rebinDescY.setOutOfBoundsAction(RebinDescriptor.MINUSONE);

        QDataSet xx;
        QDataSet yy;
        if (Schemes.isBundleDataSet(zz)) {
            logger.fine("is bundle");
            xx = DataSetOps.slice1(zz, 0);
            yy = DataSetOps.slice1(zz, 1);
            zz = DataSetOps.slice1(zz, zz.length(0) - 1);
        } else {
            if (!(zz.property(QDataSet.DEPEND_0) == null)) {
                xx = (QDataSet) zz.property(QDataSet.DEPEND_0);
            } else {
                xx = Ops.findgen(zz.length());
            }
            if (!(zz.property(QDataSet.DEPEND_1) == null)) {
                yy = (QDataSet) zz.property(QDataSet.DEPEND_1);
            } else {
                if (Schemes.isLegacyXYZScatter(zz)) {
                    yy = zz;
                    zz = (QDataSet) yy.property(QDataSet.PLANE_0);
                } else {
                    yy = Ops.findgen(zz.length());
                }
            }

        }
        
        LoggerManager.markTime("got X Y Z datasets");
        
        List<ProGAL.geom2d.Point> points= new ArrayList(xx.length());
        for ( int i=0; i<xx.length(); i++ ) {
            points.add( new VertexInt( xx.value(i), yy.value(i), i ) );
        }
        
        LoggerManager.markTime("added points");
        
        ProGAL.geom2d.delaunay.DTWithBigPoints rt= new ProGAL.geom2d.delaunay.DTWithBigPoints( points );
                
        LoggerManager.markTime("triangulation done");
        
        QDataSet wds= org.das2.qds.DataSetUtil.weightsDataSet( zz );
        Number fill= (Number)wds.property(QDataSet.FILL_VALUE);
        double dfill;
        if ( fill==null ) dfill= -1e38; else dfill= fill.doubleValue();
        result.putProperty( QDataSet.FILL_VALUE, dfill );
        boolean hasFill= false;
        
        Units xunits= Units.dimensionless;
        Units yunits= Units.dimensionless;
        
        LoggerManager.markTime("begin interp all pixels");
        
        ProGAL.geom2d.delaunay.Triangle t=null;
        int dir=1;
        boolean triUsable=true; // true if the current triangle can be used.
        
        Datum dxlimit= org.das2.qds.DataSetUtil.asDatum( org.das2.qds.DataSetUtil.guessCadence(xx,null) );
        Datum dylimit= org.das2.qds.DataSetUtil.asDatum( org.das2.qds.DataSetUtil.guessCadence(yy,null) );

        double ylimit= 20;
        double xlimit= 20;
        
        for ( int ix= 0; ix<rebinDescX.numberOfBins(); ix++ ) {
            for ( int iy= dir==1 ? 0 : rebinDescY.numberOfBins()-1;
                    dir==1 ? iy<rebinDescY.numberOfBins() : iy>=0; iy+=dir ) { // Boustrophedon back and forth
                //if ( ix>300 && iy>300 ) {
                //    System.err.println("here");
                //}
                ProGAL.geom2d.Point thePoint= new ProGAL.geom2d.Point( rebinDescX.binCenter(ix,xunits), rebinDescY.binCenter(iy,yunits) );
                ProGAL.geom2d.delaunay.Triangle t1= rt.walk( thePoint, null, t );
                if ( t1!=t ) {
                    Rectangle2D r= getBounds(t1);
                    System.err.println(r);
                    triUsable= r.getWidth() < dxlimit.doubleValue(xunits) && r.getHeight()<dylimit.doubleValue(yunits);
                    t= t1;
                }
                ProGAL.geom2d.Point[] abc= new ProGAL.geom2d.Point[] { t.getCorner(0), t.getCorner(1), t.getCorner(2) }; 
                VertexInt[] abci= new VertexInt[3];
                hasFill= false;
                for ( int k=0; k<3; k++ ) {
                    if ( !( abc[k] instanceof VertexInt ) ) { // this is outside of the triangulation, an extrapolation.
                        result.putValue( ix, iy, dfill );
                        hasFill= true;
                    } else {
                        abci[k]= (VertexInt)abc[k];
                    }
                }
                if ( hasFill || !triUsable ) {
                    result.putValue( ix, iy, dfill );
                } else {
                    double[] w= new double[3];
                    w[0]= area( thePoint, abc[1], abc[2] );
                    w[1]= area( thePoint, abc[0], abc[2] );
                    w[2]= area( thePoint, abc[0], abc[1] );
                    double n= w[0] + w[1] + w[2];
                    for ( int k=0; k<3; k++ ) {
                        w[k]/= n;
                    }
                    double d;
                    // nearest neighbor code
        //            if ( w[0]>w[1] ) {
        //                if ( w[0]>w[2] ) {
        //                    d= data.value( abci[0].idx );
        //                } else {
        //                    d= data.value( abci[2].idx );
        //                }
        //            } else {
        //                if ( w[1]>w[2] ) {
        //                    d= data.value( abci[1].idx );
        //                } else {
        //                    d= data.value( abci[2].idx );
        //                }
        //            }      
                    try { 
                        d= zz.value( abci[0].idx ) * w[0] 
                            +   zz.value( abci[1].idx ) * w[1] 
                            +   zz.value( abci[2].idx ) * w[2];
                        result.putValue( ix, iy, d );
                    } catch ( NullPointerException ex ) {
                        System.err.println("here151");
                        d= zz.value( abci[0].idx ) * w[0] 
                            +   zz.value( abci[1].idx ) * w[1] 
                            +   zz.value( abci[2].idx ) * w[2];
                        result.putValue( ix, iy, d );
                    }
                }
            }
            dir= -dir;
        }
        
        LoggerManager.markTime("done interp all pixels");
                
        org.das2.qds.DataSetUtil.copyDimensionProperties( zz, result );
        if ( !hasFill ) {
            result.putProperty( QDataSet.FILL_VALUE, null );
        }
        
        result.putProperty( "TRIANGULATION", rt );
        return result;
    }
    
    

}
