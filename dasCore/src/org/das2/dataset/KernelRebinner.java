
package org.das2.dataset;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.DasException;
import org.das2.datum.Datum;
import org.das2.datum.LoggerManager;
import org.das2.datum.Units;
import org.das2.qds.DDataSet;
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
    
    public QDataSet makeKernel( RebinDescriptor ddX, RebinDescriptor ddY, Datum xwidth, Datum ywidth ) {
        Datum xx= ddX.binStart(0).add(xwidth);
        int nx = 4 + Math.max( 1, ddX.whichBin( xx.doubleValue(xx.getUnits()), xx.getUnits() ) );
        Datum yy= ddY.binStart(0).add(ywidth);
        int ny = 4 + Math.max( 1, ddY.whichBin( yy.doubleValue(yy.getUnits()), yy.getUnits() ) );
        DDataSet k= DDataSet.createRank2( nx,ny );
        for ( int i=0; i<nx; i++ ) {
            for ( int j=0; j<ny; j++ ) {
                k.putValue( i, j, 1.0 );
            }
        }
        for ( int i=0; i<nx; i++ ) {  // taper the edges
            k.putValue(i,0,0.3);
            k.putValue(i,0,0.6);
            k.putValue(i,ny-1,0.3);
            k.putValue(i,ny-2,0.6);
        }
        for ( int j=0; j<ny; j++ ) {  // taper the edges
            k.putValue(0,j,0.3);
            k.putValue(1,j,0.6);
            k.putValue(nx-1,j,0.3);
            k.putValue(nx-2,j,0.6);
        }
        double nn= Ops.total(k);
        
        return Ops.link( Ops.linspace( xwidth.negative(), xwidth, nx ), Ops.linspace( ywidth.negative(), ywidth, ny ), Ops.divide( k, nn ) );
    }
    
    private void applyKernel(QDataSet kernel, int x, int y, double value, double weight, DDataSet ss, DDataSet ww) {
        
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
                    double w= weight * kernel.value( i-xbase, j-ybase );
                    ss.addValue( i, j, value * w );
                    ww.addValue( i, j, w );
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
        
        QDataSet zds= ds;
        QDataSet wds= org.das2.qds.DataSetUtil.weightsDataSet(ds);
        
        QDataSet xds= SemanticOps.xtagsDataSet(ds);
        QDataSet yds= SemanticOps.ytagsDataSet(ds);
                
        QDataSet xBinWidth= org.das2.qds.DataSetUtil.guessCadenceNew( xds, zds );
        QDataSet yBinWidth= org.das2.qds.DataSetUtil.guessCadenceNew( yds, zds.slice(0) );
        
        if ( xBinWidth==null ) {
            xBinWidth= Ops.dataset( ddX.binWidthDatum() );
        }
        
        if ( yBinWidth==null ) {
            yBinWidth= Ops.dataset( ddY.binWidthDatum() );
        }
        
        long t0= System.currentTimeMillis();
        
        Units xUnits= SemanticOps.getUnits(xds);
        Units zUnits= SemanticOps.getUnits(zds);
        Units yUnits= SemanticOps.getUnits(yds);
        
        int nx= ddX.numberOfBins();
        int ny= ddY.numberOfBins();
        
        logger.log(Level.FINEST, "Allocating rebinData and rebinWeights: {0} x {1}", new Object[]{nx, ny});
        
        DDataSet rebinData= DDataSet.createRank2( nx, ny );
        DDataSet rebinWeights= DDataSet.createRank2( nx, ny );
        
        int nTables = 1; // TODO: Rank 3 support and Rank 2 bundle of x,y,z
        for (int iTable = 0; iTable < nTables; iTable++) {
            
            QDataSet kernel= makeKernel( ddX, ddY, Ops.datum(xBinWidth), Ops.datum(yBinWidth) );
            QDataSet kxds= (QDataSet) kernel.property(QDataSet.DEPEND_0);
            QDataSet kyds= (QDataSet) kernel.property(QDataSet.DEPEND_1);
            
            int [] ibiny= new int[yds.length()];
            for (int j=0; j < ibiny.length; j++) {
                ibiny[j]= ddY.whichBin( yds.value(j), yUnits );
            }
            
            for ( int i=0; i<xds.length(); i++) {
                int ibinx;
                ibinx= ddX.whichBin( xds.value(i), xUnits );
                if ( ibinx==-1 ) {
                    continue;
                    //ibinx= ddX.whichBin( kxds.value(kxds.length()-1), xUnits );
                }
                for (int j = 0; j < yds.length(); j++) {
                    if ( ibiny[j]==-1 ) continue;
                    double z = zds.value(i,j);
                    double w = wds.value(i,j);
                    applyKernel( kernel, ibinx, ibiny[j], z, w, rebinData, rebinWeights );
                }
            }
        }
        
        logger.finest("normalize sums by weights");
        for ( int i=0; i<nx; i++ ) {
            QDataSet w1= rebinWeights.slice(i);
            for ( int j=0; j<ny; j++ ) {
                double w11= w1.value(j);
                if ( w11 > 0. ) {
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
