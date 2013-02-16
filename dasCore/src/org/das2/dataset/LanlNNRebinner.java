/* File: TableAverageRebinner.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on November 5, 2003, 10:31 AM
 *      by Edward West <eew@space.physics.uiowa.edu>
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
package org.das2.dataset;

import java.util.WeakHashMap;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.das2.DasException;
import org.das2.system.DasLogger;
import java.util.logging.*;
import org.das2.datum.UnitsConverter;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.JoinDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dsops.Ops;

/**
 * DataSetRebinner implementing either bi-linear interpolation in blocks of 4 points, or nearest neighbor interpolation by
 * grabbing close points, or no interpolation at all..  Points the land on the same pixel are averaged together.
 * @author  Edward West
 */
public class LanlNNRebinner implements DataSetRebinner {

    private static final Logger logger = DasLogger.getLogger(DasLogger.DATA_OPERATIONS_LOG);

    /** Creates a new instance of TableAverageRebinner */
    public LanlNNRebinner() {
    }

    WeakHashMap<QDataSet,QDataSet> yds0c= new WeakHashMap();
    WeakHashMap<QDataSet,QDataSet> yds1c= new WeakHashMap();

    /**
     * rebin the data, using the interpolate control to define the interpolation between measurements.  Data that fall into the
     * same pixel are always averaged in the linear space, regardless of interpolation method.
     * @param ds rank 2 table or rank 3 join of tables.
     * @param ddX
     * @param ddY
     * @return
     * @throws IllegalArgumentException
     * @throws DasException
     */
    public QDataSet rebin( QDataSet ds, RebinDescriptor ddX, RebinDescriptor ddY ) throws IllegalArgumentException, DasException {
        logger.finest("enter LanlNNRebinner.rebin");

        if (ds == null) {
            throw new NullPointerException("null data set");
        }
        if (! SemanticOps.isTableDataSet(ds) ) {
            throw new IllegalArgumentException("Data set must be an instanceof TableDataSet: " + ds.getClass().getName());
        }

        QDataSet tds = (QDataSet) ds;
        int rank= tds.rank();

        if ( rank==2 ) { // make it into a rank 3 table
            JoinDataSet tdsx= new JoinDataSet(3);
            tdsx.join(tds);
            tds= tdsx;
        }

        QDataSet weights = SemanticOps.weightsDataSet( ds );

        QDataSet tds1= tds.slice(0);

        QDataSet xds= SemanticOps.xtagsDataSet(tds1);
        QDataSet xds0, xds1;
        QDataSet binPlus= (QDataSet) xds.property(QDataSet.BIN_PLUS);
        QDataSet binMinus= (QDataSet) xds.property(QDataSet.BIN_MINUS);
        if ( SemanticOps.isBins(xds) ) {
            xds0= Ops.slice1( xds, 0 );
            xds1= Ops.slice1( xds, 1 );
        } else if ( binPlus!=null && binMinus!=null ) {
            xds0= Ops.subtract( xds, binMinus );
            xds1= Ops.add( xds, binPlus );
        }else {
            QDataSet dx= Ops.divide( DataSetUtil.guessCadenceNew( xds, null ), DataSetUtil.asDataSet(2) );
            xds0= Ops.subtract( xds, dx );
            xds1= Ops.add( xds, dx );
        }
        QDataSet yds= SemanticOps.ytagsDataSet(tds1);
        QDataSet yds0, yds1;
        boolean rank2y;

        yds0= yds0c.get(yds); // let's cache the result of this, since rank 2 yds datasets are slow. (http://www.rbsp-ect.lanl.gov/data_pub/rbspa/mageis/level2/rbspa_pre_ect-mageis-L2_$Y$m$d_v$(v,sep).cdf?FEDO)
        yds1= yds1c.get(yds);

        if ( yds0==null || yds1==null ) {
            binPlus= (QDataSet) yds.property(QDataSet.BIN_PLUS);
            binMinus= (QDataSet) yds.property(QDataSet.BIN_MINUS);
            if ( SemanticOps.isBins(yds) ) {
                yds0= Ops.slice1( yds, 0 );
                yds1= Ops.slice1( yds, 1 );
            } else if ( binPlus!=null && binMinus!=null ) {
                yds0= Ops.subtract( yds, binMinus );
                yds1= Ops.add( yds, binPlus );
            } else {
                QDataSet dy= DataSetUtil.guessCadenceNew( yds.rank()==2 ? yds.slice(0): yds, null );
                if ( UnitsUtil.isRatiometric( SemanticOps.getUnits(dy) ) ) {
                    yds0= Ops.divide( yds, dy );
                    yds1= Ops.multiply( yds, dy );
                } else {
                    yds0= Ops.subtract( yds, dy );
                    yds1= Ops.add( yds, dy );
                }
            }
            yds0c.put( yds, yds0 );
            yds1c.put( yds, yds1 );
        }

        rank2y= yds0.rank()==2;

        Units xunits= SemanticOps.getUnits( xds );
        Units yunits= SemanticOps.getUnits( yds );

        if ( ddX != null && tds.length() > 0 ) {
            UnitsConverter xc= xunits.getConverter(ddX.getUnits());
            QDataSet bounds= SemanticOps.bounds(xds); 
            double start = xc.convert( bounds.value(1,0) );
            double end = xc.convert( bounds.value(1,1) );
            if (start > ddX.binStop(ddX.numberOfBins()-1).doubleValue(ddX.getUnits()) ) {
                throw new NoDataInIntervalException("data starts after range");
            } else if (end < ddX.binStart(0).doubleValue(ddX.getUnits())) {
                throw new NoDataInIntervalException("data ends before range");
            }
        }

        int nx = (ddX == null ? tds1.length() : ddX.numberOfBins());
        int ny = (ddY == null ? tds1.length(0) : ddY.numberOfBins());

        if ( ddY==null && rank!=2 ) {
            throw new IllegalArgumentException("ddY was null but there was rank 3 dataset");
        }

        logger.log(Level.FINEST, "Allocating rebinData and rebinWeights: {0} x {1}", new Object[]{nx, ny});

        DDataSet S= DDataSet.createRank2( nx, ny );
        DDataSet N= DDataSet.createRank2( nx, ny );

        double dxpixel= ddX.getUnits().getOffsetUnits().convertDoubleTo( xunits.getOffsetUnits(), ddX.binWidth() );
        double dypixel= ddY.getUnits().getOffsetUnits().convertDoubleTo( yunits.getOffsetUnits(), ddY.binWidth() );

        double y0,y1;
        int nYData= rank2y ? yds0.length(0) : yds0.length();

        for ( int i=0; i<xds0.length(); i++) {
            double x0= xds0.value(i);
            double x1= xds1.value(i);
            double wx= (x1-x0)/dxpixel;
            int px0= ddX.whichBin( x0, xunits );
            int px1= ddX.whichBin( x1, xunits );
            int sx0= Math.max( 0, px0 );
            int sx1= Math.min( nx-1, px1 );
            for ( int j=0; j<nYData; j++ ) {
                double z= tds1.value( i,j );
                if ( rank2y ) {
                    y0= yds0.value(i,j);
                    y1= yds1.value(i,j);
                } else {
                    y0= yds0.value(j);
                    y1= yds1.value(j);
                }
                double wy= (y1-y0)/dypixel;
                int py0= ddY.whichBin( y0, yunits );
                int py1= ddY.whichBin( y1, yunits );
                double w= wx*wy*weights.value(i,j);
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
        }
        
        return Ops.divide( S, N );

    }

    private java.beans.PropertyChangeSupport propertyChangeSupport = new java.beans.PropertyChangeSupport(this);

    public void addPropertyChangeListener(java.beans.PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(java.beans.PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }
}
