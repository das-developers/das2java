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

/**
 *
 * @author  Edward West
 */
public class TableAveragePeakRebinner implements DataSetRebinner {
    
    /** Creates a new instance of TableAveragePeakRebinner */
    public TableAveragePeakRebinner() {
    }
    
    public DataSet rebin(DataSet ds, RebinDescriptor ddX, RebinDescriptor ddY) throws IllegalArgumentException {
        if (!(ds instanceof TableDataSet)) {
            throw new IllegalArgumentException();
        }
        TableDataSet tds = (TableDataSet)ds;
        TableDataSet weights = (TableDataSet)ds.getPlanarView("weights");
        
        long timer= System.currentTimeMillis();
        
        int nx= ddX.numberOfBins();
        int ny= ddY.numberOfBins();
        
        double[] averageData= new double[nx*ny];
        double[] averageWeights= new double[nx*ny];
        double[] peakData = new double[nx*ny];

        TableAverageRebinner.average(tds, weights, averageData, averageWeights, ddX, ddY);
        
        TableAverageRebinner.fillInterpolateX(averageData, averageWeights, ddX.binCenters(), Double.POSITIVE_INFINITY);
        TableAverageRebinner.fillInterpolateY(averageData, averageWeights, ddY.binCenters(), Double.POSITIVE_INFINITY, ddY.isLog());
        
        TablePeakRebinner.peaks(tds, peakData, ddX, ddY);

        double[] xTags = ddX.binCenters();
        double[][] yTags = {ddY.binCenters()};
        int[] tableOffsets = {0};
        String[] planeIDs =     {"",                "peaks",            "weights"};
        double[][][] zValues =  {{averageData},     {peakData},         {averageWeights}};
        Units[] zUnits =        {tds.getZUnits(),   tds.getZUnits(),    Units.dimensionless};

        return new DefaultTableDataSet(xTags, tds.getXUnits(), yTags, tds.getYUnits(), zValues, zUnits, planeIDs, tableOffsets, java.util.Collections.EMPTY_MAP);
    }
    
}
