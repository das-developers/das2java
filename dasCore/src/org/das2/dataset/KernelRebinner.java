
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
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.ops.Ops;
import org.das2.qds.util.AsciiFormatter;

/**
 * Revisit Larry's KernalRebinner, where each point is spread out using 
 * a kernel, and then convoluted with each data point.
 * @author jbf
 */
public class KernelRebinner implements DataSetRebinner {
    private static final Logger logger= LoggerManager.getLogger("das2.data.rebinner");
    
    public QDataSet makeFlatKernel( RebinDescriptor ddX, RebinDescriptor ddY, int nx, int ny ) {
        
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
    
    public QDataSet makeBilinearKernel( RebinDescriptor ddX, RebinDescriptor ddY, int nx, int ny ) {
        
        double nx2= nx/2;
        double ny2= ny/2;
        
        DDataSet k= DDataSet.createRank2( nx,ny );
        for ( int i=0; i<nx; i++ ) {
            for ( int j=0; j<ny; j++ ) {
                k.putValue( i, j, nx2 + ny2 - Math.abs(nx2-i) - Math.abs(ny2-j) );
            }
        }
        
        return k;
    }
    
    private QDataSet makeKernel( RebinDescriptor ddX, RebinDescriptor ddY, Datum xwidth, Datum ywidth ) {
        String type= "flat";
        int nx, ny;
        QDataSet k;
        if ( type.equals("flat") ) {
            try {
                Datum xx;
                if ( UnitsUtil.isRatiometric(xwidth.getUnits() ) ) {
                    xx = ddX.binStart(0).add(ddX.binStart(0).multiply(1.+xwidth.convertTo(Units.percentIncrease).value()/100));
                } else {
                    xx = ddX.binStart(0).add(xwidth);
                }
                nx= 4 + Math.max( 1, ddX.whichBin( xx.doubleValue(xx.getUnits()), xx.getUnits() ) );
            } catch ( InconvertibleUnitsException ex ) {
                nx= 4;
            }
            try {
                Datum yy;
                if ( UnitsUtil.isRatiometric(ywidth.getUnits() ) ) {
                    ywidth.convertTo(Units.percentIncrease);
                    yy= ddY.binStart(0).add(ddY.binStart(0).multiply(1.+ywidth.convertTo(Units.percentIncrease).value()/100));
                } else {
                    yy= ddY.binStart(0).add(ywidth);
                }
                ny = 4 + Math.max( 1, ddY.whichBin( yy.doubleValue(yy.getUnits()), yy.getUnits() ) );
            } catch ( InconvertibleUnitsException ex ) {
                ny= 4;
            }
            k = makeFlatKernel(ddX, ddY, nx, ny);
        } else if ( type.equals("bilinear") ) {
            try {
                Datum xx;
                if ( UnitsUtil.isRatiometric(xwidth.getUnits() ) ) {
                    xx = ddX.binStart(0).add(ddX.binStart(0).multiply(1.+xwidth.convertTo(Units.percentIncrease).value()/100));
                } else {
                    xx = ddX.binStart(0).add(xwidth);
                }
                nx= (int)( 1.5 * Math.max( 1, ddX.whichBin( xx.doubleValue(xx.getUnits()), xx.getUnits() ) ) );
            } catch ( InconvertibleUnitsException ex ) {
                nx= 4;
            }
            try {
                Datum yy;
                if ( UnitsUtil.isRatiometric(ywidth.getUnits() ) ) {
                    ywidth.convertTo(Units.percentIncrease);
                    yy= ddY.binStart(0).add(ddY.binStart(0).multiply(1.+ywidth.convertTo(Units.percentIncrease).value()/100));
                } else {
                    yy= ddY.binStart(0).add(ywidth);
                }
                ny = (int)( 1.5 * Math.max( 1, ddY.whichBin( yy.doubleValue(yy.getUnits()), yy.getUnits() ) ) );
            } catch ( InconvertibleUnitsException ex ) {
                ny= 4;
            }
            k = makeBilinearKernel(ddX, ddY, nx, ny);
        } else {
            throw new IllegalArgumentException("bad type:" + type );
        }
        
        try {
            new AsciiFormatter().formatToFile( "/tmp/ap/kernel.txt", k );
        } catch (IOException ex) {
            Logger.getLogger(KernelRebinner.class.getName()).log(Level.SEVERE, null, ex);
        }

        double nn= Ops.total(k);
        k= Ops.divide( k, nn );

        return Ops.link( Ops.linspace( xwidth.negative(), xwidth, nx ), Ops.linspace( ywidth.negative(), ywidth, ny ), k );
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

        long t0= System.currentTimeMillis();
        
        if ( ds.rank()==2 ) { // make it into a rank 3 dataset
            ds= Ops.join(null,ds); 
        }
        
        int nx= ddX.numberOfBins();
        int ny= ddY.numberOfBins();        
        
        DDataSet rebinData= DDataSet.createRank2( nx, ny );
        DDataSet rebinWeights= DDataSet.createRank2( nx, ny );
            
        int nTables = ds.length(); // TODO: Rank 3 support and Rank 2 bundle of x,y,z
        for (int iTable = 0; iTable < nTables; iTable++) {
        
            QDataSet zds= ds.slice(iTable);
            
            QDataSet wds= org.das2.qds.DataSetUtil.weightsDataSet(zds);

            QDataSet xds= SemanticOps.xtagsDataSet(zds);
            QDataSet yds= SemanticOps.ytagsDataSet(zds);

            QDataSet xBinWidth= org.das2.qds.DataSetUtil.guessCadenceNew( xds, zds );
            QDataSet yBinWidth= org.das2.qds.DataSetUtil.guessCadenceNew( yds, zds.slice(0) );

            if ( xBinWidth==null ) {
                xBinWidth= Ops.dataset( ddX.binWidthDatum() );
            }

            if ( yBinWidth==null ) {
                yBinWidth= Ops.dataset( ddY.binWidthDatum() );
            }

            Units xUnits= SemanticOps.getUnits(xds);
            Units zUnits= SemanticOps.getUnits(zds);
            Units yUnits= SemanticOps.getUnits(yds);

            logger.log(Level.FINEST, "Allocating rebinData and rebinWeights: {0} x {1}", new Object[]{nx, ny});
                    
            QDataSet kernel= makeKernel( ddX, ddY, Ops.datum(xBinWidth), Ops.datum(yBinWidth) );
            //QDataSet kxds= (QDataSet) kernel.property(QDataSet.DEPEND_0);
            //QDataSet kyds= (QDataSet) kernel.property(QDataSet.DEPEND_1);
            
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
