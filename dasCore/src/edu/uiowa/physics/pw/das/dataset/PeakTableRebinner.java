/* File: TablePeakRebinner.java
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

package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.datum.*;

/**
 *
 * @author  Edward West
 */
public class PeakTableRebinner implements DataSetRebinner {
    
    /** Creates a new instance of TableAverageRebinner */
    public PeakTableRebinner() {
    }
    
    public DataSet rebin(DataSet ds, RebinDescriptor ddX, RebinDescriptor ddY) throws IllegalArgumentException {
        if (!(ds instanceof TableDataSet)) {
            throw new IllegalArgumentException();
        }
        TableDataSet tds = (TableDataSet)ds;
        long timer= System.currentTimeMillis();
        
        int nx= ddX.numberOfBins();
        int ny= ddY.numberOfBins();
        
        double[] rebinData= new double[nx*ny];
        double[] rebinWeights= new double[nx*ny];

        peaks(tds, rebinData, ddX, ddY);
        
        double[] xTags = ddX.binCenters();
        double[][] yTags = {ddY.binCenters()};
        double[][][] zValues = {{rebinData}};
        int[] tableOffsets = {0};
        Units[] zUnits = {tds.getZUnits()};
        String[] planeIDs = {""};

        return new DefaultTableDataSet(xTags, tds.getXUnits(), yTags, tds.getYUnits(), zValues, zUnits, planeIDs, tableOffsets, java.util.Collections.EMPTY_MAP);
    }
    
    static void peaks(TableDataSet tds, double[] rebinData, RebinDescriptor ddX, RebinDescriptor ddY) {
        double[] ycoordinate= ddY.binCenters();

        int nx= ddX.numberOfBins();
        int ny= ddY.numberOfBins();
        
        java.util.Arrays.fill(rebinData, Double.NaN);
        
        int [] ibiny= new int[tds.getYLength(0)];
        for (int j=0; j < ibiny.length; j++) {
            ibiny[j]= ddY.whichBin(tds.getYTagDouble(0, j, tds.getYUnits()), tds.getYUnits());
        }

        for (int i=0; i < tds.getXLength(); i++) {
            int ibinx= ddX.whichBin(tds.getXTagDouble(i, tds.getXUnits()), tds.getXUnits());
            if (ibinx>=0 && ibinx<nx) {
                for (int j = 0; j < ibiny.length; j++) {
                    if (ibiny[j] >= 0 && ibiny[j] < ny) {
                        int index = ibinx*ny + ibiny[j];
                        double value = tds.getDouble(i, j, tds.getZUnits());
                        if (Double.isNaN(rebinData[index])) {
                            rebinData[index] = value;
                        }
                        else {
                            rebinData[index] = Math.max(value, rebinData[index]);
                        }
                    }
                }
            }
        }
        
    }
    
}
