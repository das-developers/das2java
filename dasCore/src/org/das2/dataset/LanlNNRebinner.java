
package org.das2.dataset;

//import java.io.File;
//import java.io.IOException;
import java.util.WeakHashMap;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.das2.DasException;
import org.das2.system.DasLogger;
import java.util.logging.*;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.UnitsConverter;
import org.das2.qds.DataSetUtil;
import org.das2.qds.DDataSet;
import org.das2.qds.DataSetOps;
import org.das2.qds.JoinDataSet;
import org.das2.qds.MutablePropertyDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.ops.Ops;
//import org.das2.qds.util.AsciiFormatter;

/**
 * DataSetRebinner for explicitly doing NN rebinning.  The AverageTableRebinner had been used for the purpose, and
 * there were numerous problems.  Also, this looks for BIN_PLUS, BIN_MINUS, BIN_MAX, and BIN_MIN properties in the dataset.
 */
public class LanlNNRebinner implements DataSetRebinner {

    private static final Logger logger = DasLogger.getLogger(DasLogger.DATA_OPERATIONS_LOG);

    public LanlNNRebinner() {
    }

    WeakHashMap<QDataSet,QDataSet> yds0c= new WeakHashMap();
    WeakHashMap<QDataSet,QDataSet> yds1c= new WeakHashMap();
    WeakHashMap<QDataSet,QDataSet> cadence= new WeakHashMap();
    WeakHashMap<QDataSet,QDataSet> xds0c= new WeakHashMap<>();
    
    /**
     * get cadence that checks for null and returns the pixel cadence in this case.
     * @param ds the xtags or ytags 
     * @param res fall-back cadence, that is the axis resolution
     * @return the rank0 cadence, as linear in the units, or ratiometric log10ratio.
     */
    private QDataSet getCadence( QDataSet ds, Datum res ) {
        QDataSet dds= cadence.get(ds);
        if ( dds==null ) { //&& !cadence.containsKey(ds) ) {
            dds= DataSetUtil.guessCadenceNew( ds, null );
            if ( dds==null ) dds= DataSetUtil.asDataSet(res);
            if ( UnitsUtil.isRatiometric( SemanticOps.getUnits(dds) ) ) {
                dds= Ops.convertUnitsTo( dds, Units.log10Ratio );
            }
            cadence.put( ds,dds );
        }
        return dds;
    }
    
    /**
     * rebin the data, using the interpolate control to define the interpolation between measurements.  Data that fall into the
     * same pixel are always averaged in the linear space, regardless of interpolation method.
     * @param ds rank 2 table or rank 3 join of tables.
     * @param ddX
     * @param ddY
     * @param ddZ
     * @return
     * @throws IllegalArgumentException
     * @throws DasException
     */
    @Override
    public QDataSet rebin( QDataSet ds, RebinDescriptor ddX, RebinDescriptor ddY, RebinDescriptor ddZ ) 
            throws IllegalArgumentException, DasException {
        logger.entering("org.das2.dataset.LanlNNRebinner", "rebin");

        ddX.setOutOfBoundsAction( RebinDescriptor.EXTRAPOLATE );
        ddY.setOutOfBoundsAction( RebinDescriptor.EXTRAPOLATE );
        
        if (ds == null) {
            throw new NullPointerException("null data set");
        }
        if (!( SemanticOps.isTableDataSet(ds) || SemanticOps.isBundle(ds) ) ) {
            throw new IllegalArgumentException("Data set must be an instanceof TableDataSet: " + ds.getClass().getName());
        }

        QDataSet tds = (QDataSet) ds;
        int rank= tds.rank();

        if ( rank==2 ) { // make it into a rank 3 table so we are always working with the same scheme.
            JoinDataSet tdsx= new JoinDataSet(3);
            tdsx.join(tds);
            tds= tdsx;
        }

        int nx = ddX.numberOfBins();
        int ny = ddY.numberOfBins();

        DDataSet S= DDataSet.createRank2( nx, ny );
        DDataSet N= DDataSet.createRank2( nx, ny );

        boolean rs= false;
        boolean re= false;

        for ( int itable=0; itable<tds.length(); itable++ ) {
            QDataSet tds1= tds.slice(itable);
            QDataSet weights = SemanticOps.weightsDataSet( tds1 );

            // We don't cache the x tags, because often they are timetags and this could cause problems with memory.

            QDataSet xds= SemanticOps.xtagsDataSet(tds1);
            QDataSet xds0, xds1;
            xds1= (QDataSet) xds.property(QDataSet.BIN_MAX);
            xds0= (QDataSet) xds.property(QDataSet.BIN_MIN);
            if ( xds0==null ) {
                QDataSet binPlus= (QDataSet) xds.property(QDataSet.BIN_PLUS);
                QDataSet binMinus= (QDataSet) xds.property(QDataSet.BIN_MINUS);
                if ( SemanticOps.isBins(xds) ) {
                    xds0= Ops.slice1( xds, 0 );
                    xds1= Ops.slice1( xds, 1 );
                } else if ( binPlus!=null && binMinus!=null ) {
                    xds0= Ops.subtract( xds, binMinus );
                    xds1= Ops.add( xds, binPlus );
                } else {
                    QDataSet xds2= xds0c.get(xds);
                    if ( xds2==null ) {
                        xds2= DataSetUtil.inferBins(xds);
                        xds0c.put( xds, xds2 );
                    }
                    xds0= Ops.slice1( xds2, 0 );
                    xds1= Ops.slice1( xds2, 1 );
                }
            }
            
            QDataSet yds= SemanticOps.ytagsDataSet(tds1);
            QDataSet yds0, yds1;
            boolean rank2y;
            
            yds0= yds0c.get(yds); // let's cache the result of this, since rank 2 yds datasets are slow. 
            yds1= yds1c.get(yds); // (http://www.rbsp-ect.lanl.gov/data_pub/rbspa/mageis/level2/rbspa_pre_ect-mageis-L2_$Y$m$d_v$(v,sep).cdf?FEDO)

            if ( false ) { // set to true for debugging.
                yds0= null;
                yds1= null;
            }

            if ( yds0==null || yds1==null ) {
                yds0= (QDataSet) yds.property(QDataSet.BIN_MIN);
                yds1= (QDataSet) yds.property(QDataSet.BIN_MAX);
                if ( yds0==null ) {
                    QDataSet binPlus= (QDataSet) yds.property(QDataSet.BIN_PLUS);
                    QDataSet binMinus= (QDataSet) yds.property(QDataSet.BIN_MINUS);
                    if ( SemanticOps.isBins(yds) ) {
                        yds0= Ops.slice1( yds, 0 );
                        yds1= Ops.slice1( yds, 1 );
                    } else if ( binPlus!=null && binMinus!=null ) {
                        yds0= Ops.subtract( yds, binMinus );
                        yds1= Ops.add( yds, binPlus );
                    } else {
                        switch (yds.rank()) {
                            case 2:
                                {
                                    logger.info("inferring bounds rank 2 ytags, this can be slow.");
                                    // test code: vap+das2server:http://planet.physics.uiowa.edu/das/das2Server?dataset=juno/waves/flight/burst_hfwbr_hi.dsdf&start_time=2014-04-04T17:00:00.000Z&end_time=2014-04-04T21:00:00.000Z
                                    QDataSet[] bins= DataSetUtil.inferBinsRank2( yds );
                                    yds0= bins[0];
                                    yds1= bins[1];
                                    break;
                                }
                            case 1:
                                {
                                    QDataSet bins= DataSetUtil.inferBins( yds.rank()==2 ? yds.slice(0): yds );
                                    yds0= Ops.copy( Ops.slice1( bins, 0 ) );
                                    yds1= Ops.copy( Ops.slice1( bins, 1 ) );
                                    break;
                                }
                            default:
                                throw new UnsupportedOperationException("bad rank on ytags: "+yds.rank());
                        }
                    }
                }
                yds0c.put( yds, yds0 );
                yds1c.put( yds, yds1 );
            }
            assert yds0!=null;
            rank2y= yds0.rank()==2;

            Units xunits= SemanticOps.getUnits( xds );
            Units yunits= SemanticOps.getUnits( yds );

            if ( tds.length() > 0 ) {
                UnitsConverter xc= xunits.getConverter(ddX.getUnits());
                QDataSet bounds= SemanticOps.bounds(xds);
                double start = xc.convert( bounds.value(1,0) );
                double end = xc.convert( bounds.value(1,1) );
                DatumRange dr= DatumRangeUtil.union( ddX.binStop(ddX.numberOfBins()-1),ddX.binStart(0));
                if (start <= dr.max().doubleValue(ddX.getUnits()) ) {
                    rs= true;
                }
                if (end >= dr.min().doubleValue(ddX.getUnits())) {
                    re= true;
                }
            }

            logger.log(Level.FINEST, "Allocating rebinData and rebinWeights: {0} x {1}", new Object[]{nx, ny});

            double y0,y1;
            int nYData= rank2y ? yds0.length(0) : yds0.length();

            if ( SemanticOps.isBundle(tds1) 
                    && tds1.length(0)==3 
                    && !rank2y 
                    && yds0.length()==tds1.length() 
                    && xds0.length()==tds1.length() ) { // bug 1160: I think some data could still be mistaken here.
                tds1= DataSetOps.unbundle(tds1,tds1.length(0)-1);
                weights= DataSetOps.unbundle(weights,weights.length(0)-1);
                for ( int i=0; i<xds0.length(); i++) {
                    double x0= xds0.value(i);
                    double x1= xds1.value(i);
                    int px0,px1;
                    if ( ddX.start>ddX.end ) { // flipped
                        px0= ddX.whichBin( x1, xunits );
                        px1= ddX.whichBin( x0, xunits );
                    } else {
                        px0= ddX.whichBin( x0, xunits );
                        px1= ddX.whichBin( x1, xunits );
                    }
                    double wx= 1./((px1-px0+1));

                    int sx0= Math.max( 0, px0 );
                    int sx1= Math.min( nx-1, px1 );
                    double z= tds1.value( i );
                    y0= yds0.value(i);
                    y1= yds1.value(i);
                    int py0,py1;
                    if ( ddY.start>ddY.end ) { // flipped
                        py0= ddY.whichBin( y1, yunits );
                        py1= ddY.whichBin( y0, yunits );
                    } else {
                        py0= ddY.whichBin( y0, yunits );
                        py1= ddY.whichBin( y1, yunits );
                    }
                    double wy= 1./((py1-py0+1)); // favor short bins
                    double w= wx*wy*weights.value(i);
                    int sy0= Math.max( 0, py0 );
                    int sy1= Math.min( ny-1, py1 );
                    for ( int k=sx0; k<=sx1; k++ ) {
                        for ( int l=sy0; l<=sy1; l++ ) {
                            if ( w>N.value(k,l) ) {
                                S.putValue(k,l,z*w);
                                N.putValue(k,l,w);
                            }
                        }
                    }
                }
            } else {
                QDataSet yds0_1=null;
                QDataSet yds1_1=null;
                if ( rank2y==false ) {
                    yds0_1= yds0;
                    yds1_1= yds1;
                }
                int[] py0s= new int[nYData];
                int[] py1s= new int[nYData];
                double[] wys= new double[nYData];
                for ( int i=0; i<xds0.length(); i++) {
                    double x0= xds0.value(i);
                    double x1= xds1.value(i);
                    int px0,px1;
                    if ( ddX.start>ddX.end ) { // flipped
                        px0= ddX.whichBin( x1, xunits );
                        px1= ddX.whichBin( x0, xunits );
                    } else {
                        px0= ddX.whichBin( x0, xunits );
                        px1= ddX.whichBin( x1, xunits );
                    }
                    double wx= 1./((px1-px0+1));

                    int sx0= Math.max( 0, px0 );
                    int sx1= Math.min( nx-1, px1 );
                    if ( rank2y ) {
                        yds0_1= yds0.slice(i);
                        yds1_1= yds1.slice(i);
                    }
                    assert yds0_1!=null;
                    assert yds1_1!=null;
                    for ( int j=0; j<nYData; j++ ) {
                        if ( i==0 || rank2y ) {
                            y0= yds0_1.value(j);
                            y1= yds1_1.value(j);
                            int py0,py1;
                            if ( ddY.start>ddY.end ) { // flipped
                                py0= ddY.whichBin( y1, yunits );
                                py1= ddY.whichBin( y0, yunits );
                            } else {
                                py0= ddY.whichBin( y0, yunits );
                                py1= ddY.whichBin( y1, yunits );
                            }
                            py0s[j]= py0;
                            py1s[j]= py1;
                            double wy= 1./((py1-py0+1)); // favor short bins                        
                            wys[j]= wy;
                        }
                    }
                    for ( int j=0; j<nYData; j++ ) {
                        double z= tds1.value( i,j );
                        double w= wx*wys[j]*weights.value(i,j);
                        int sy0= Math.max( 0, py0s[j] );
                        int sy1= Math.min( ny-1, py1s[j] );
                        for ( int k=sx0; k<=sx1; k++ ) {
                            for ( int l=sy0; l<=sy1; l++ ) {
                                if ( w>N.value(k,l) ) {
                                    S.putValue(k,l,z*w*1.1);
                                    N.putValue(k,l,w*1.1);
                                }
                            }
                        }
                    }
                }
            }
        }

        if ( !rs ) throw new NoDataInIntervalException("data starts after range");
        if ( !re ) throw new NoDataInIntervalException("data ends before range");

        MutablePropertyDataSet mds= (MutablePropertyDataSet) Ops.divide( S, N );
        RebinDescriptor.putDepDataSet( ds, mds, ddX, ddY );
        
//        try {
//            //System.err.println( String.format( "%d,%d,%f", 40,48, mds.value(40,48) )  );
//            new AsciiFormatter().formatToFile( new File( "/tmp/ap.txt"), N );
//        } catch (IOException ex) {
//            Logger.getLogger(LanlNNRebinner.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        
        logger.exiting("org.das2.dataset.LanlNNRebinner", "rebin");
        
        return mds;

    }

    private final java.beans.PropertyChangeSupport propertyChangeSupport = new java.beans.PropertyChangeSupport(this);

    public void addPropertyChangeListener(java.beans.PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(java.beans.PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }
}
