/* File: TableAveragePeakRebinner.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on November 12, 2003, 4:25 PM
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

import java.util.Arrays;
import org.das2.datum.Units;
import org.das2.qds.DDataSet;
import org.das2.qds.JoinDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.ops.Ops;

/**
 *
 * @author  Edward West
 */
public class AveragePeakTableRebinner implements DataSetRebinner {

    /* adds additional planes for debugging */
    private boolean debug= false;
    
    /** Creates a new instance of TableAveragePeakRebinner */
    public AveragePeakTableRebinner() {
    }        
    
    @Override
    public String toString() {
        return "averagePeakRebinner";
    }
    
    @Override
    public QDataSet rebin( QDataSet ds, RebinDescriptor ddX, RebinDescriptor ddY, RebinDescriptor z) throws IllegalArgumentException {
        
        QDataSet tds;
        
        JoinDataSet tds3= new JoinDataSet(3); // for AverageTableRebinner.
        
        if ( ds.rank()==2 ) {
            tds3.join(ds);
            tds= ds;
        } else if ( ds.rank()==3 ) {
            throw new IllegalArgumentException("dataset must be rank 2");
        } else {
            throw new IllegalArgumentException("dataset must be rank 2");
        }
        
                
        QDataSet weights = org.das2.qds.DataSetUtil.weightsDataSet(ds);
        QDataSet peaks = (QDataSet) ds.property( QDataSet.BIN_MAX );
        if ( peaks==null ) {
            QDataSet binPlus= (QDataSet) ds.property( QDataSet.BIN_PLUS );
            if ( binPlus!=null ) peaks= Ops.add( ds, binPlus );
        }
        
        //long timer= System.currentTimeMillis();
        
        int nx= (ddX == null ? tds.length() : ddX.numberOfBins());
        int ny= (ddY == null ? tds.length(0) : ddY.numberOfBins());
        
        double[][] averageData= new double[nx][ny];
        double[][] averageWeights= new double[nx][ny];
        double[][] peakData = new double[nx][ny];

        AverageTableRebinner.average(tds3, weights, averageData, averageWeights, ddX, ddY, AverageTableRebinner.Interpolate.NearestNeighbor );
        
        QDataSet xtds= SemanticOps.xtagsDataSet(tds);
        QDataSet ytds= SemanticOps.ytagsDataSet(tds);
        
        Units xunits= SemanticOps.getUnits(xtds);
        
        double[] xTags;
        double[] xTagMin, xTagMax;
        if (ddX != null) {
            xTags = ddX.binCenters();
            xTagMin = ddX.binStops();
            xTagMax = ddX.binStarts();
            for ( int i=0; i<tds.length(); i++ ) {
                double xt= xtds.value(i);
                int ibin= ddX.whichBin( xt, xunits );
                if ( ibin>-1 && ibin<nx ) {
                    xTagMin[ibin]= Math.min( xTagMin[ibin], xt );
                    xTagMax[ibin]= Math.max( xTagMax[ibin], xt );
                }
            }            
        } else {
            xTags = new double[nx];
            for (int i = 0; i < nx; i++) {
                xTags[i] = xtds.value(i);
            }
            xTagMin= xTags;
            xTagMax= xTags;      
        }
        
//        double[][] yTags;
//        if (ddY != null) {
//            yTags = new double[][]{ddY.binCenters()};
//        }
//        else {
//            yTags = new double[1][ny];
//            for (int j = 0; j < ny; j++) {
//                yTags[0][j] = ytds.value(j);
//            }
//        }
        
        QDataSet xTagWidth= org.das2.qds.DataSetUtil.guessCadenceNew(xtds,null);
        
        double xTagWidthDouble;
        if ( xTagWidth==null ) {
            if ( ddX!=null ) {
                xTagWidthDouble= ddX.binWidth();
            } else {
                xTagWidthDouble= 0.;
            }
        } else {
            Units u= SemanticOps.getUnits(xTagWidth);
            xTagWidthDouble= u.convertDoubleTo( ddX.getUnits().getOffsetUnits(), xTagWidth.value() );
        }
        
        double[][] averageWeightsCopy= new double[averageWeights.length][];
        for ( int i=0; i<averageData.length; i++ ) {
            averageWeightsCopy[i]= Arrays.copyOf( averageWeights[i], averageWeights[i].length );
            
        }
        AverageTableRebinner.fillInterpolateX(averageData, averageWeights, xTags, xTagMin, xTagMax, xTagWidthDouble, AverageTableRebinner.Interpolate.NearestNeighbor );
        
        if ( ddY!=null ) {
            QDataSet yTagWidth= org.das2.qds.DataSetUtil.guessCadenceNew(ytds,null);
            if ( yTagWidth==null ) {
                AverageTableRebinner.fillInterpolateY(averageData, averageWeights, ddY, null, AverageTableRebinner.Interpolate.NearestNeighbor );
            } else {
                AverageTableRebinner.fillInterpolateY(averageData, averageWeights, ddY, org.das2.qds.DataSetUtil.asDatum(yTagWidth), AverageTableRebinner.Interpolate.NearestNeighbor );
            }            
        }
        
        if (peaks == null) {
            PeakTableRebinner.peaks(tds, peakData, ddX, ddY);
            AverageTableRebinner.fillInterpolateX(peakData, averageWeightsCopy, xTags, xTagMin, xTagMax, xTagWidthDouble, AverageTableRebinner.Interpolate.NearestNeighbor );
        }
        else {
            PeakTableRebinner.peaks(peaks, peakData, ddX, ddY);
            AverageTableRebinner.fillInterpolateX(peakData, averageWeightsCopy, xTags, xTagMin, xTagMax, xTagWidthDouble, AverageTableRebinner.Interpolate.NearestNeighbor );
        }
                          
        double[] dd= new double[nx*ny];
        flatten( averageData, dd, 0, nx, ny );
        DDataSet result= DDataSet.wrap( dd, nx, ny );
        org.das2.qds.DataSetUtil.copyDimensionProperties( tds, result );

        double[] ww= new double[nx*ny];
        flatten( averageWeights, ww, 0, nx, ny );
        DDataSet wds= DDataSet.wrap( ww, nx, ny );

        double[] pp= new double[nx*ny];
        flatten( peakData, pp, 0, nx, ny );
        DDataSet pds= DDataSet.wrap( pp, nx, ny );
        
        result.putProperty( QDataSet.DEPEND_1, ytds );
        result.putProperty( QDataSet.WEIGHTS, wds );
        result.putProperty( QDataSet.BIN_MAX, pds );
        
        return result;
        
    }

    /**
     * from org.virbo.cdfdatasource.CdfUtil
     * @param data
     * @param back
     * @param offset
     * @param nx
     * @param ny 
     */
    public static void flatten(double[][] data, double[] back, int offset, int nx, int ny) {
        for (int i = 0; i < nx; i++) {
            double[] dd = data[i];
            System.arraycopy(dd, 0, back, offset + i * ny, ny);
        }
    }
    
    /**
     * Getter for property debug.
     * @return Value of property debug.
     */
    public boolean isDebug() {
        return this.debug;
    }
    
    /**
     * Setter for property debug.
     * @param debug New value of property debug.
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    
}
