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

package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.datum.*;
import java.util.HashMap;
import java.util.Map;

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
    
    public DataSet rebin(DataSet ds, RebinDescriptor ddX, RebinDescriptor ddY) throws IllegalArgumentException {
        if (!(ds instanceof TableDataSet)) {
            throw new IllegalArgumentException();
        }
        
        TableDataSet tds = (TableDataSet)ds;
        
        if ( ddY==null && tds.tableCount()==0 ) {
            throw new IllegalArgumentException( "empty table and null RebinDescriptor for Y, so result YTags are undefined." );
        }
        
        TableDataSet weights = (TableDataSet)ds.getPlanarView(DataSet.PROPERTY_PLANE_WEIGHTS);
        TableDataSet peaks = (TableDataSet)ds.getPlanarView(DataSet.PROPERTY_PLANE_PEAKS);
        
        long timer= System.currentTimeMillis();
        
        int nx= (ddX == null ? tds.getXLength() : ddX.numberOfBins());
        int ny= (ddY == null ? tds.getYLength(0) : ddY.numberOfBins());
        
        double[][] averageData= new double[nx][ny];
        double[][] averageWeights= new double[nx][ny];
        double[][] peakData = new double[nx][ny];
        
        AverageTableRebinner.average(tds, weights, averageData, averageWeights, ddX, ddY);
        
        double[] xTags;
        double[] xTagMin, xTagMax;
        if (ddX != null) {
            xTags = ddX.binCenters();
            xTagMin = ddX.binStops();
            xTagMax = ddX.binStarts();
            for ( int i=0; i<tds.getXLength(); i++ ) {
                double xt= tds.getXTagDouble(i, tds.getXUnits() );
                int ibin= ddX.whichBin( xt, tds.getXUnits() );
                if ( ibin>-1 && ibin<nx ) {
                    xTagMin[ibin]= Math.min( xTagMin[ibin], xt );
                    xTagMax[ibin]= Math.max( xTagMax[ibin], xt );
                }
            }            
        }
        else {
            xTags = new double[nx];
            for (int i = 0; i < nx; i++) {
                xTags[i] = tds.getXTagDouble(i, tds.getXUnits());
            }
            xTagMin= xTags;
            xTagMax= xTags;      
        }
        
        double[][] yTags;
        if (ddY != null) {
            yTags = new double[][]{ddY.binCenters()};
        }
        else {
            yTags = new double[1][ny];
            for (int j = 0; j < ny; j++) {
                yTags[0][j] = tds.getYTagDouble(0, j, tds.getYUnits());
            }
        }
        
        Datum xTagWidth= DataSetUtil.guessXTagWidth(ds);
        double xTagWidthDouble= xTagWidth.doubleValue(ddX.getUnits().getOffsetUnits());
        AverageTableRebinner.fillInterpolateX(averageData, averageWeights, xTags, xTagMin, xTagMax, xTagWidthDouble, AverageTableRebinner.Interpolate.Linear );
        
        if ( ddY!=null ) {
            Datum yTagWidth= (Datum)ds.getProperty( DataSet.PROPERTY_Y_TAG_WIDTH );
            AverageTableRebinner.fillInterpolateY(averageData, averageWeights, ddY, yTagWidth, AverageTableRebinner.Interpolate.Linear );
        }
        
        if (peaks == null) {
            PeakTableRebinner.peaks(tds, peakData, ddX, ddY);
        }
        else {
            PeakTableRebinner.peaks(peaks, peakData, ddX, ddY);
        }
        
        Map properties= new HashMap( ds.getProperties() );
        
        if ( ddX!=null ) properties.put( DataSet.PROPERTY_X_TAG_WIDTH, ddX.binWidthDatum() );
        if ( ddY!=null ) properties.put( DataSet.PROPERTY_Y_TAG_WIDTH, ddY.binWidthDatum() );
          
        int[] tableOffsets = {0};
        String[] planeIDs =     {"",               DataSet.PROPERTY_PLANE_PEAKS,          DataSet.PROPERTY_PLANE_WEIGHTS};
        double[][][] zValues =  {averageData,      peakData,         averageWeights};
        Units[] zUnits =        {tds.getZUnits(),  tds.getZUnits(),  Units.dimensionless};
        
        return new DefaultTableDataSet(xTags, tds.getXUnits(), yTags, tds.getYUnits(), zValues, zUnits, planeIDs, tableOffsets, properties );
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
