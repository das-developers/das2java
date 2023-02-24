
package org.das2.dataset;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.DasException;
import org.das2.datum.Datum;
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.LoggerManager;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.das2.qds.DDataSet;
import org.das2.qds.IndexGenDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.ops.Ops;

/**
 * Revisit Larry's KernalRebinner, where each point is spread out using 
 * a kernel, and then convoluted with each data point.
 * @author jbf
 */
public class KernelRebinner implements DataSetRebinner {
    private static final Logger logger= LoggerManager.getLogger("das2.data.rebinner");

    public enum Type {
        flat,
        cone,
        disk,
    }
    
    Type type;
    
    public KernelRebinner( Type t ) {
        this.type = t;
    }
    
    public static QDataSet makeFlatKernel( RebinDescriptor ddX, RebinDescriptor ddY, int nx, int ny ) {
        
        DDataSet k= DDataSet.createRank2( nx,ny );
        for ( int i=0; i<nx; i++ ) {
            for ( int j=0; j<ny; j++ ) {
                k.putValue( i, j, 1.0 );
            }
        }
        for ( int i=0; i<nx; i++ ) {  // taper the edges
            k.putValue(i,0,0.3);
            k.putValue(i,1,0.6);
            k.putValue(i,ny-1,0.3);
            k.putValue(i,ny-2,0.6);
        }
        for ( int j=1; j<ny-1; j++ ) {  // taper the edges
            k.putValue(0,j,0.3);
            k.putValue(1,j,0.6);
            k.putValue(nx-1,j,0.3);
            k.putValue(nx-2,j,0.6);
        }
        return k;
        
    }
    
    public static QDataSet makeConeKernel( RebinDescriptor ddX, RebinDescriptor ddY, int nx, int ny ) {
        
        double nx2= nx/2;
        double ny2= ny/2;
        
        DDataSet kernel= DDataSet.createRank2( nx,ny );
        for ( int i=0; i<nx; i++ ) {
            for ( int j=0; j<ny; j++ ) {
                // kernel.putValue( i, j, Math.min( nx2 - Math.abs(nx2-i), ny2 - Math.abs(ny2-j) ) );
                double v= Math.max( 1. - Math.sqrt( Math.pow( Math.abs(nx2-i)/nx2, 2 ) + Math.pow( Math.abs(ny2-j)/ny2, 2 ) ), 0.00 );
                kernel.putValue( i, j, v );
            }
        }
        int nx14= nx/4; 
        int ny14= ny/4;
        int nx34= (int)Math.ceil( nx - nx/4. ); // thanks, FindBugs!
        int ny34= (int)Math.ceil( ny - ny/4. );
        DDataSet mask= DDataSet.createRank2( nx,ny );
        for ( int i=nx14; i<nx34; i++ ) {
            for ( int j=ny14; j<ny34; j++ ) {
                //w.putValue( i, j, Math.sin( (float)( nx - nx/2 )/nx ) + Math.sin( (float)( ny - ny/2 )/ny ) );
                mask.putValue( i, j, 1 );
            }
        }
        mask.putProperty( QDataSet.NAME, "mask" );
        
        kernel.putProperty( QDataSet.WEIGHTS,mask );
        kernel.putProperty( QDataSet.NAME, "bilinear" );
        
        return kernel;
    }
    
    public static QDataSet makeCircleKernel( RebinDescriptor ddX, RebinDescriptor ddY, int nx, int ny ) {
        
        double nx2= nx/2;
        double ny2= ny/2;
        double nx4= nx/4;
        
        DDataSet k= DDataSet.createRank2( nx,ny );
        for ( int i=0; i<nx; i++ ) {
            for ( int j=0; j<ny; j++ ) {
                double r= Math.sqrt( Math.pow( Math.abs(nx2-i), 2 ) + Math.pow( Math.abs(ny2-j), 2 ) );
                if ( r>nx4 && r<nx2 ) {
                    k.putValue( i, j, 1 );
                } else {
                    k.putValue( i, j, 0 );
                }
            }
        }
        
        return k;
    }
    
    private QDataSet makeKernel( RebinDescriptor ddX, RebinDescriptor ddY, Datum xwidth, Datum ywidth ) {
        
        int nx, ny;
        QDataSet k;

        switch (type) {
            case flat:
                try {
                    Datum xx;
                    if ( UnitsUtil.isRatiometric(xwidth.getUnits() ) ) {
                        xx = ddX.binStart(0).add(ddX.binStart(0).multiply(1.+xwidth.convertTo(Units.percentIncrease).value()/100));
                    } else {
                        xx = ddX.binStart(0).add(xwidth);
                    }
                    nx= 2 + Math.max( 1, ddX.whichBin( xx.doubleValue(xx.getUnits()), xx.getUnits() ) );
                } catch ( InconvertibleUnitsException ex ) {
                    nx= 2;
                }   try {
                    Datum yy;
                    if ( UnitsUtil.isRatiometric(ywidth.getUnits() ) ) {
                        ywidth.convertTo(Units.percentIncrease);
                        yy= ddY.binStart(0).add(ddY.binStart(0).multiply(1.+ywidth.convertTo(Units.percentIncrease).value()/100));
                    } else {
                        yy= ddY.binStart(0).add(ywidth);
                    }
                    ny = 2 + Math.max( 1, ddY.whichBin( yy.doubleValue(yy.getUnits()), yy.getUnits() ) );
                } catch ( InconvertibleUnitsException ex ) {
                    ny= 2;
                }   k = makeFlatKernel(ddX, ddY, nx, ny);
                break;
            case cone:
                try {
                    Datum xx;
                    if ( UnitsUtil.isRatiometric(xwidth.getUnits() ) ) {
                        xx = ddX.binStart(0).add(ddX.binStart(0).multiply(1.+xwidth.convertTo(Units.percentIncrease).value()/100));
                    } else {
                        xx = ddX.binStart(0).add(xwidth);
                    }
                    nx= 2 + (int)Math.ceil( 2.0 * Math.max( 1, ddX.whichBin( xx.doubleValue(xx.getUnits()), xx.getUnits() ) ) );
                } catch ( InconvertibleUnitsException ex ) {
                    nx= 1;
                }   try {
                    Datum yy;
                    if ( UnitsUtil.isRatiometric(ywidth.getUnits() ) ) {
                        ywidth.convertTo(Units.percentIncrease);
                        yy= ddY.binStart(0).add(ddY.binStart(0).multiply(1.+ywidth.convertTo(Units.percentIncrease).value()/100));
                    } else {
                        yy= ddY.binStart(0).add(ywidth);
                    }
                    ny = 2 + (int)Math.ceil( 2.0 * Math.max( 1, ddY.whichBin( yy.doubleValue(yy.getUnits()), yy.getUnits() ) ) );
                } catch ( InconvertibleUnitsException ex ) {
                    ny= 2;
                }   k = makeConeKernel(ddX, ddY, nx, ny);
                break;
            case disk:
                try {
                    Datum xx;
                    if ( UnitsUtil.isRatiometric(xwidth.getUnits() ) ) {
                        xx = ddX.binStart(0).add(ddX.binStart(0).multiply(1.+xwidth.convertTo(Units.percentIncrease).value()/100));
                    } else {
                        xx = ddX.binStart(0).add(xwidth);
                    }
                    nx= 2 + (int)( 2.0 * Math.max( 1, ddX.whichBin( xx.doubleValue(xx.getUnits()), xx.getUnits() ) ) );
                } catch ( InconvertibleUnitsException ex ) {
                    nx= 2;
                }   try {
                    Datum yy;
                    if ( UnitsUtil.isRatiometric(ywidth.getUnits() ) ) {
                        ywidth.convertTo(Units.percentIncrease);
                        yy= ddY.binStart(0).add(ddY.binStart(0).multiply(1.+ywidth.convertTo(Units.percentIncrease).value()/100));
                    } else {
                        yy= ddY.binStart(0).add(ywidth);
                    }
                    ny = 2 + (int)( 2.0 * Math.max( 1, ddY.whichBin( yy.doubleValue(yy.getUnits()), yy.getUnits() ) ) );
                } catch ( InconvertibleUnitsException ex ) {
                    ny= 2;
                }   k = makeCircleKernel(ddX, ddY, nx, ny);
                break;
            default:
                throw new IllegalArgumentException("bad type:" + type );
        }
        
        //try {
        //    new AsciiFormatter().formatToFile( "/tmp/ap/kernel.txt", k );
        //} catch (IOException ex) {
        //    Logger.getLogger(KernelRebinner.class.getName()).log(Level.SEVERE, null, ex);
        //}

        return Ops.link( Ops.linspace( xwidth.negative(), xwidth, nx ), Ops.linspace( ywidth.negative(), ywidth, ny ), k );
    }
    
    private void applyKernel(QDataSet kernel, QDataSet mask, int x, int y, double value, double weight, 
            DDataSet ss, DDataSet ww, DDataSet mm ) {
        
        int x0,x1;
        int y0,y1;

        int nx= ss.length();
        int ny= ss.length(0);
        int dx0= kernel.length()/2;
        int dy0= kernel.length(0)/2;

        x0= x-dx0; // this is a location in pixel space
        x1= x0 + kernel.length();
        y0= y-dy0;
        y1= y0 + kernel.length(0);
        int xbase= x0;
        int ybase= y0;
        
        if (x0<0) x0=0; else if (x0>nx) x0=nx;  // trim to visible portion
        if (x1<0) x1=0; else if (x1>nx) x1=nx+1;
        if (y0<0) y0=0; else if (y0>ny) y0=ny;
        if (y1<0) y1=0; else if (y1>ny) y1=ny+1;
        
        for ( int i=x0; i<x1; i++ ) {
            for ( int j=y0; j<y1; j++ ) {
                try {
                    int ik= i-xbase;
                    int jk= j-ybase;
                    double w= weight * kernel.value( ik, jk);
                    ss.addValue( i, j, value * w );
                    ww.addValue( i, j, w );
                    mm.addValue( i, j, mask.value( ik, jk ) * w );
                } catch ( ArrayIndexOutOfBoundsException e ) {
                    //throw new RuntimeException(e);
                }
            }
        }

    }
    
    
    @Override
    public QDataSet rebin(QDataSet ds, RebinDescriptor ddX, RebinDescriptor ddY, RebinDescriptor ddZ) throws IllegalArgumentException, DasException {
        logger.finest("enter QernalTableRebinner.rebin");
        
        if (ds == null) {
            throw new NullPointerException("null data set");
        }        

        long t0= System.currentTimeMillis();
        
        boolean isBundle= SemanticOps.isBundle(ds) && ds.length(0)<4;
        
        if ( ds.rank()==2 && !isBundle ) { // make it into a rank 3 dataset
            ds= Ops.join(null,ds); 
        }
        
        int nx= ddX.numberOfBins();
        int ny= ddY.numberOfBins();        
        
        /**
         * total contribution for each pixel, which must be normalized at some point
         */
        DDataSet rebinData= DDataSet.createRank2( nx, ny );
        
        /**
         * total contribution weights for each pixel
         */
        DDataSet rebinWeights= DDataSet.createRank2( nx, ny );
        
        /**
         * kernels can contribute to an average, but if it's the only point in the average, then it should not be used.
         */
        DDataSet rebinMask= DDataSet.createRank2( nx, ny );
        
        if ( ds.rank()==1 || ( ds.rank()==2 && isBundle ) ) {
            QDataSet zds, xds, yds, wds;
            if ( ds.rank()==1 ) {
                yds= ds; // TODO: nasty shameful code will break when I clean up this model.
                xds= SemanticOps.xtagsDataSet(yds);
                zds= (QDataSet) ds.property(QDataSet.PLANE_0);
                if ( zds==null ) zds= Ops.zeros( yds.length() );
            } else {
                zds= Ops.unbundle( ds, ds.length(0)-1 );
                xds= SemanticOps.xtagsDataSet(ds);
                yds= SemanticOps.ytagsDataSet(ds);
            }
            wds= Ops.valid( zds );

            QDataSet xBinWidth= org.das2.qds.DataSetUtil.guessCadenceNew( xds, zds );
            QDataSet yBinWidth= org.das2.qds.DataSetUtil.guessCadenceNew( yds, zds );
            
            if ( xBinWidth==null ) {
                if ( xds instanceof IndexGenDataSet ) {
                    xBinWidth= Ops.dataset( 1 );
                } else {
                    xBinWidth= Ops.dataset( ddX.binWidthDatum() );
                }
            }

            if ( yBinWidth==null ) {
                if ( yds instanceof IndexGenDataSet ) {
                    yBinWidth= Ops.dataset( 1 );
                } else {
                    yBinWidth= Ops.dataset( ddY.binWidthDatum() );
                }
            }
            Units xUnits= SemanticOps.getUnits(xds);
            Units yUnits= SemanticOps.getUnits(yds);

            logger.log(Level.FINEST, "Allocating rebinData and rebinWeights: {0} x {1}", new Object[]{nx, ny});
                    
            QDataSet kernel= makeKernel( ddX, ddY, Ops.datum(xBinWidth), Ops.datum(yBinWidth) );
            QDataSet mask= org.das2.qds.DataSetUtil.weightsDataSet(kernel);
                        
            for ( int i=0; i<xds.length(); i++) {
                int ibinx,ibiny;
                ibinx= ddX.whichBin( xds.value(i), xUnits );
                if ( ibinx==-1 ) continue;
                ibiny= ddY.whichBin( yds.value(i), yUnits );
                if ( ibiny==-1 ) continue;
                double z = zds.value(i);
                double w = wds.value(i);
                applyKernel( kernel, mask, ibinx, ibiny, z, w, rebinData, rebinWeights, rebinMask );
            }
            
        } else {
            
            int nTables = ds.length(); // TODO: Rank 2 bundle of x,y,z
            for (int iTable = 0; iTable < nTables; iTable++) {

                QDataSet zds= ds.slice(iTable);

                QDataSet wds= org.das2.qds.DataSetUtil.weightsDataSet(zds);

                QDataSet xds= SemanticOps.xtagsDataSet(zds);
                QDataSet yds= SemanticOps.ytagsDataSet(zds);

                QDataSet xBinWidth= org.das2.qds.DataSetUtil.guessCadenceNew( xds, zds );
                if ( xBinWidth==null ) xBinWidth= org.das2.qds.DataSetUtil.guessCadence( xds, zds );
                QDataSet yBinWidth;
                if ( yds.rank()==1 ) {
                    yBinWidth= org.das2.qds.DataSetUtil.guessCadenceNew( yds, zds.slice(0) );
                    if ( yBinWidth==null ) yBinWidth= org.das2.qds.DataSetUtil.guessCadence( yds, zds.slice(0) );
                } else {
                    if ( yds.rank()==2 && QDataSet.VALUE_BINS_MIN_MAX.equals(yds.property(QDataSet.BINS_1)) ) {
                        yBinWidth= Ops.reduceMax( Ops.subtract( Ops.slice1(yds,1), Ops.slice1(yds,0) ), 0 );
                        yds= Ops.reduceMean( yds,1 );
                    } else {
                        yBinWidth= org.das2.qds.DataSetUtil.guessCadenceNew( yds.slice(0), zds.slice(0) );
                        if ( yBinWidth==null ) yBinWidth= org.das2.qds.DataSetUtil.guessCadence( yds.slice(0), zds.slice(0) );
                    }
                }

                if ( xBinWidth==null ) {
                    if ( xds instanceof IndexGenDataSet ) {
                        xBinWidth= Ops.dataset( 1 );
                    } else {
                        xBinWidth= Ops.dataset( ddX.binWidthDatum() );
                    }
                }

                if ( yBinWidth==null ) {
                    if ( yds instanceof IndexGenDataSet ) {
                        yBinWidth= Ops.dataset( 1 );
                    } else {
                        yBinWidth= Ops.dataset( ddY.binWidthDatum() );
                    }
                }

                Units xUnits= SemanticOps.getUnits(xds);
                Units yUnits= SemanticOps.getUnits(yds);

                logger.log(Level.FINEST, "Allocating rebinData and rebinWeights: {0} x {1}", new Object[]{nx, ny});

                QDataSet kernel= makeKernel( ddX, ddY, Ops.datum(xBinWidth), Ops.datum(yBinWidth) );
                QDataSet mask= org.das2.qds.DataSetUtil.weightsDataSet(kernel);

                int [] ibiny=null; 
                QDataSet ydss=null;
                if ( yds.rank()==1 ) {
                    ibiny = new int[yds.length()];
                    for (int j=0; j < ibiny.length; j++) {
                        ibiny[j]= ddY.whichBin( yds.value(j), yUnits );
                    }
                } else if ( yds.rank()==2 ) {
                    ydss= yds;
                } else {
                    throw new IllegalArgumentException("yds rank must be 1 or 2");
                }

                for ( int i=0; i<xds.length(); i++) {
                    int ibinx;
                    ibinx= ddX.whichBin( xds.value(i), xUnits );
                    if ( ibinx==-1 ) {
                        continue;
                        //ibinx= ddX.whichBin( kxds.value(kxds.length()-1), xUnits );
                    }
                    if ( ydss!=null ) {
                        yds= ydss.slice(i);
                        ibiny = new int[yds.length()];
                        for (int j=0; j < ibiny.length; j++) {
                            ibiny[j]= ddY.whichBin( yds.value(j), yUnits );
                        }
                    }
                    assert ibiny!=null;

                    for (int j = 0; j < ibiny.length; j++) {
                        if ( ibiny[j]==-1 ) continue;
                        double z = zds.value(i,j);
                        double w = wds.value(i,j);
                        applyKernel( kernel, mask, ibinx, ibiny[j], z, w, rebinData, rebinWeights, rebinMask );
                    }
                }
            }
        }
        
        logger.finest("normalize sums by weights");
        for ( int i=0; i<nx; i++ ) {
            QDataSet w1= rebinWeights.slice(i);
            QDataSet m1= rebinMask.slice(i);
            for ( int j=0; j<ny; j++ ) {
                double w11= w1.value(j);
                double m11= m1.value(j);
                if ( w11>0. && m11>0. ) {
                    rebinData.putValue( i, j, rebinData.value(i,j)/w11 );
                } else {
                    rebinData.putValue( i, j, Double.NaN );
                }
            }
        }
        
        rebinData.putProperty( QDataSet.WEIGHTS, rebinWeights );
        
        logger.finest( "create new DataSet" );
        
        QDataSet xtags= Ops.dataset( ddX.binCentersDV() );
        QDataSet ytags= Ops.dataset( ddY.binCentersDV() );
        
        logger.log(Level.FINER, "time to complete (ms): {0}", System.currentTimeMillis()-t0);
        logger.finest("done, QernalTableRebinner.rebin");
        
        return Ops.link( xtags, ytags, rebinData );
        
        
    }
    
}
