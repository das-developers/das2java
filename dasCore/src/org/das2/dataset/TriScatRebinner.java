/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.dataset;

import java.awt.geom.Rectangle2D;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.das2.datum.Datum;
import org.das2.datum.Units;
import org.das2.qds.DataSetOps;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.WritableDataSet;
import org.das2.qds.examples.Schemes;
import org.das2.qds.ops.Ops;
import org.das2.qds.util.DataSetBuilder;
import org.das2.util.LoggerManager;

/**
 *
 * @author jbf
 */
public class TriScatRebinner implements DataSetRebinner {

    private static final Logger logger = LoggerManager.getLogger("das2.data.rebinner");

    ProGAL.geom2d.delaunay.DTWithBigPoints triangulation= null;
    WeakReference<QDataSet> trids= null;
            
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
            return String.format( "(%.1f,%.1f)", x(), y() );
        }
    }    
    
    private java.awt.geom.Rectangle2D getBounds( ProGAL.geom2d.delaunay.Triangle t ) {
        Rectangle2D.Double r= new Rectangle2D.Double( t.getCorner(0).x(), t.getCorner(0).y(), 0, 0 );
        
        r.add( new java.awt.geom.Point2D.Double( t.getCorner(1).x(), t.getCorner(1).y() ));
        r.add( new java.awt.geom.Point2D.Double( t.getCorner(2).x(), t.getCorner(2).y() ));
        
        return r;
    }
    
    @Override
    public QDataSet rebin(QDataSet ds, RebinDescriptor rebinDescX, RebinDescriptor rebinDescY) {
        // throws IllegalArgumentException, DasException {

        LoggerManager.resetTimer("triscat rebin");
                
        WritableDataSet result = Ops.zeros(rebinDescX.numberOfBins(), rebinDescY.numberOfBins());

        rebinDescX.setOutOfBoundsAction(RebinDescriptor.MINUSONE);
        rebinDescY.setOutOfBoundsAction(RebinDescriptor.MINUSONE);

        QDataSet zz;
        
        if ( Schemes.isSimpleSpectrogram(ds) ) {
            zz= Ops.flatten(ds);
        } else {
            zz= ds;
        }
        
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
        
        // scale and fuzz the data so there are no collinear points
        QDataSet extentX= Ops.extent(xx);
        QDataSet extentY= Ops.extent(yy);
        double[] dX= new double[] { extentX.value(0), extentX.value(1)-extentX.value(0) };
        double[] dY= new double[] { extentY.value(0), extentY.value(1)-extentY.value(0) };
    
        ProGAL.geom2d.delaunay.DTWithBigPoints rt;
        
        QDataSet ds1= trids==null ? null : trids.get();
        if ( ds1==null || ds1!=ds ) {
            List<ProGAL.geom2d.Point> points= new ArrayList(xx.length());
            double fuzz= 0.0001;
            for ( int i=0; i<xx.length(); i++ ) {
                points.add( new VertexInt( (xx.value(i)-dX[0])/dX[1] + fuzz, (yy.value(i)-dY[0])/dY[1]+fuzz, i ) );
                fuzz= -1 * fuzz;
            }
        
            LoggerManager.markTime("added points");
        
            rt= new ProGAL.geom2d.delaunay.DTWithBigPoints( points );
            this.triangulation= rt;
            trids= new WeakReference(ds);
            LoggerManager.markTime("triangulation done");
            
        } else {
            rt= this.triangulation;
        }
        
        QDataSet wds= org.das2.qds.DataSetUtil.weightsDataSet( zz );
        Number fill= (Number)wds.property(QDataSet.FILL_VALUE);
        double dfill;
        if ( fill==null ) dfill= -1e38; else dfill= fill.doubleValue();
        result.putProperty( QDataSet.FILL_VALUE, dfill );
        boolean hasFill= false;
        
        Units xunits= SemanticOps.getUnits(xx);
        Units yunits= SemanticOps.getUnits(yy);
        
        LoggerManager.markTime("begin interp all pixels");
        
        ProGAL.geom2d.delaunay.Triangle t=null;
        int dir=1;
        boolean triUsable=true; // true if the current triangle can be used.
        
        DataSetBuilder xs= new DataSetBuilder(1,100);
        DataSetBuilder ys= new DataSetBuilder(1,100);
        
        int i=0;
        for ( ProGAL.geom2d.delaunay.Triangle t1: rt.getTriangles() ) {
            if ( !( t1.getCorner(0) instanceof VertexInt && t1.getCorner(1) instanceof VertexInt && t1.getCorner(2) instanceof VertexInt ) ) {
                
            } else {
                Rectangle2D r= getBounds(t1);
                xs.nextRecord( r.getWidth() );
                ys.nextRecord( r.getHeight() );
            }
        }
        QDataSet xss= xs.getDataSet();
        QDataSet yss= ys.getDataSet();
//        try {
//            new AsciiFormatter().formatToFile( "/home/jbf/tmp/dist.txt", xss, yss );
//        } catch (IOException ex) {
//            Logger.getLogger(TriScatRebinner.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        
        Datum dxlimit= Ops.datum(Ops.mean(xss)).add( Ops.datum( Ops.stddev(xss) ).multiply(3) );
        Datum dylimit= Ops.datum(Ops.mean(yss)).add( Ops.datum( Ops.stddev(yss) ).multiply(3) );
        
        //Datum dxlimit= org.das2.qds.DataSetUtil.asDatum( org.das2.qds.DataSetUtil.guessCadence(xx,null) );
        //Datum dylimit= org.das2.qds.DataSetUtil.asDatum( org.das2.qds.DataSetUtil.guessCadence(yy,null) );

        double xlimit= dxlimit.doubleValue(Units.dimensionless); // triangles have been normalized
        double ylimit= dylimit.doubleValue(Units.dimensionless);
        
        for ( int ix= 0; ix<rebinDescX.numberOfBins(); ix++ ) {
            for ( int iy= dir==1 ? 0 : rebinDescY.numberOfBins()-1;
                    dir==1 ? iy<rebinDescY.numberOfBins() : iy>=0; iy+=dir ) { // Boustrophedon back and forth
                ProGAL.geom2d.Point thePoint= new ProGAL.geom2d.Point( (rebinDescX.binCenter(ix,xunits)-dX[0])/dX[1], (rebinDescY.binCenter(iy,yunits)-dY[0])/dY[1] );
                ProGAL.geom2d.delaunay.Triangle t1= rt.walk( thePoint, null, t );
                if ( t1!=t ) {
                    Rectangle2D r= getBounds(t1);
                    triUsable= r.getWidth() < xlimit && r.getHeight()<ylimit;
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
        RebinDescriptor.putDepDataSet( ds, result, rebinDescX, rebinDescY );
        
        if ( !hasFill ) {
            result.putProperty( QDataSet.FILL_VALUE, null );
        }
        
        result.putProperty( "TRIANGULATION", rt );
        return result;
    }
    
    

}
